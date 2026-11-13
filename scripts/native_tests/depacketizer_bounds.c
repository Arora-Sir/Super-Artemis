// Regression test for the bounded frame reassembly in VideoDepacketizer.c (moonlight-common-c#169).
// NOTE: A flood of continuation packets without a start of frame used to grow one NAL chain without limit, and the chain is now capped at 32 MB and 65536 fragments.
// NOTE: Build and run it with scripts/native_tests/run_depacketizer_test.sh on a private emulator, never on a device that is streaming.
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/resource.h>
#include <time.h>

#include "Limelight-internal.h"
#include "Video.h"
#include "RtpVideoQueue.h"

// The depacketizer entry points that Limelight-internal.h does not declare
void initializeVideoDepacketizer(int pktSize);
void stopVideoDepacketizer(void);
void destroyVideoDepacketizer(void);
void queueRtpPacket(PRTPV_QUEUE_ENTRY queueEntryPtr);

// Globals and callbacks that the rest of the library normally provides
int AppVersionQuad[4] = { 7, 1, 431, -1 };
STREAM_CONFIGURATION StreamConfig;
CONNECTION_LISTENER_CALLBACKS ListenerCallbacks;
DECODER_RENDERER_CALLBACKS VideoCallbacks;
int NegotiatedVideoFormat = VIDEO_FORMAT_H264;

static int idrRequests;
static int lossReports;

void LiRequestIdrFrame(void) {
    idrRequests++;
}

void connectionDetectedFrameLoss(uint32_t startFrame, uint32_t endFrame) {
    lossReports++;
}

void notifyKeyFrameReceived(void) {
}

void connectionReceivedCompleteFrame(uint32_t frameIndex) {
}

bool isReferenceFrameInvalidationEnabled(void) {
    return false;
}

bool LiGetCurrentHostDisplayHdrMode(void) {
    return false;
}

uint64_t LiGetMillis(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint64_t)ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}

static void logMessage(const char* format, ...) {
}

int PltCreateMutex(PLT_MUTEX* mutex) {
    return pthread_mutex_init(mutex, NULL);
}

void PltDeleteMutex(PLT_MUTEX* mutex) {
    pthread_mutex_destroy(mutex);
}

void PltLockMutex(PLT_MUTEX* mutex) {
    pthread_mutex_lock(mutex);
}

void PltUnlockMutex(PLT_MUTEX* mutex) {
    pthread_mutex_unlock(mutex);
}

int PltCreateConditionVariable(PLT_COND* cond, PLT_MUTEX* mutex) {
    return pthread_cond_init(cond, NULL);
}

void PltDeleteConditionVariable(PLT_COND* cond) {
    pthread_cond_destroy(cond);
}

void PltSignalConditionVariable(PLT_COND* cond) {
    pthread_cond_signal(cond);
}

void PltWaitForConditionVariable(PLT_COND* cond, PLT_MUTEX* mutex) {
    pthread_cond_wait(cond, mutex);
}

static int failures;
static uint32_t streamPacketIndex;

static void check(int ok, const char* name) {
    printf("%s %s\n", ok ? "ok  " : "FAIL", name);
    if (!ok) {
        failures++;
    }
}

static long maxRssKb(void) {
    struct rusage usage;
    getrusage(RUSAGE_SELF, &usage);
    return usage.ru_maxrss;
}

// Sends one video packet through the depacketizer the way the FEC queue does:
// the queue entry lives at the end of the packet allocation and the depacketizer takes over both.
static void sendPacket(uint32_t frameIndex, uint8_t flags, const uint8_t* payload, int payloadLength) {
    int headerLength = sizeof(RTP_PACKET) + sizeof(NV_VIDEO_PACKET);
    int receiveSize = headerLength + payloadLength;
    uint8_t* block = calloc(1, receiveSize + sizeof(RTPV_QUEUE_ENTRY));
    PRTP_PACKET rtp = (PRTP_PACKET)block;
    PNV_VIDEO_PACKET video = (PNV_VIDEO_PACKET)(block + sizeof(RTP_PACKET));
    PRTPV_QUEUE_ENTRY entry = (PRTPV_QUEUE_ENTRY)(block + receiveSize);

    rtp->header = 0x80;
    video->streamPacketIndex = (streamPacketIndex++) << 8;
    video->frameIndex = frameIndex;
    video->flags = flags | FLAG_CONTAINS_PIC_DATA;
    video->multiFecBlocks = 0;
    memcpy(block + headerLength, payload, payloadLength);

    entry->packet = rtp;
    entry->length = receiveSize;
    entry->receiveTimeMs = 1000;
    entry->presentationTimeMs = 1;
    entry->isParity = false;
    queueRtpPacket(entry);
}

static const uint8_t FRAME_HEADER[8] = { 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00 };

// A first packet payload is the 8 byte frame header followed by the NAL units
static int buildFirstPayload(uint8_t* out, const uint8_t* nals, int nalLength) {
    memcpy(out, FRAME_HEADER, sizeof(FRAME_HEADER));
    memcpy(out + sizeof(FRAME_HEADER), nals, nalLength);
    return (int)sizeof(FRAME_HEADER) + nalLength;
}

static const uint8_t IDR_NALS[] = {
    0, 0, 0, 1, 0x67, 0x42, 0x00, 0x1f, 0xe9,       // SPS
    0, 0, 0, 1, 0x68, 0xce, 0x3c, 0x80,             // PPS
    0, 0, 0, 1, 0x65, 0x88, 0x84, 0x00, 0x10, 0xff  // IDR slice
};
static const uint8_t P_NALS[] = { 0, 0, 0, 1, 0x41, 0x9a, 0x24, 0x6c, 0x41, 0xff };

static void sendSingleFrame(uint32_t frameIndex, const uint8_t* nals, int nalLength) {
    uint8_t payload[256];
    int length = buildFirstPayload(payload, nals, nalLength);
    sendPacket(frameIndex, FLAG_SOF | FLAG_EOF, payload, length);
}

static int drainFrames(int* idrFrames, int* pFrames) {
    VIDEO_FRAME_HANDLE handle;
    PDECODE_UNIT unit;
    int count = 0;
    while (LiPollNextVideoFrame(&handle, &unit)) {
        if (unit->frameType == FRAME_TYPE_IDR) {
            (*idrFrames)++;
        }
        else {
            (*pFrames)++;
        }
        LiCompleteVideoFrame(handle, DR_OK);
        count++;
    }
    return count;
}

static bool streamInitialized;

static void resetStream(void) {
    // Stop and destroy the previous stream the way the library does, then start a new one in the same process
    if (streamInitialized) {
        stopVideoDepacketizer();
        destroyVideoDepacketizer();
    }
    initializeVideoDepacketizer(1392);
    streamInitialized = true;
    streamPacketIndex = 0;
    idrRequests = 0;
    lossReports = 0;
}

static void testNormalFrames(void) {
    resetStream();
    int idr = 0, p = 0;

    // A multi packet IDR frame, then three single packet P frames
    uint8_t first[256], middle[64], last[64];
    int firstLength = buildFirstPayload(first, IDR_NALS, sizeof(IDR_NALS));
    memset(middle, 0x55, sizeof(middle));
    memset(last, 0x66, sizeof(last));
    sendPacket(1, FLAG_SOF, first, firstLength);
    sendPacket(1, 0, middle, sizeof(middle));
    sendPacket(1, FLAG_EOF, last, sizeof(last));
    for (uint32_t frame = 2; frame <= 4; frame++) {
        sendSingleFrame(frame, P_NALS, sizeof(P_NALS));
    }
    int total = drainFrames(&idr, &p);
    check(total == 4, "normal frames: four frames reach the decoder");
    check(idr == 1 && p == 3, "normal frames: one IDR frame and three P frames");
}

static void testContinuationFloodIsRejected(void) {
    resetStream();
    int idr = 0, p = 0;
    sendSingleFrame(1, IDR_NALS, sizeof(IDR_NALS));
    drainFrames(&idr, &p);

    long before = maxRssKb();
    uint8_t payload[1000];
    memset(payload, 0x77, sizeof(payload));
    // 100000 in-sequence continuation packets of frame 2 without a start of frame, about 100 MB when kept
    for (int i = 0; i < 100000; i++) {
        sendPacket(2, 0, payload, sizeof(payload));
    }
    long growthMb = (maxRssKb() - before) / 1024;
    printf("     memory growth during the flood: %ld MB\n", growthMb);
    check(growthMb < 20, "flood without a start of frame: memory stays bounded");
    check(idrRequests > 0 || lossReports > 0, "flood without a start of frame: recovery is requested");

    // The stream recovers with the next start of frame
    idr = 0;
    p = 0;
    sendSingleFrame(3, IDR_NALS, sizeof(IDR_NALS));
    sendSingleFrame(4, P_NALS, sizeof(P_NALS));
    int total = drainFrames(&idr, &p);
    check(total >= 1, "flood without a start of frame: the next frames still decode");
}

static void testFragmentCap(void) {
    resetStream();
    int idr = 0, p = 0;
    uint8_t payload[256];
    int length = buildFirstPayload(payload, IDR_NALS, sizeof(IDR_NALS));
    sendPacket(1, FLAG_SOF, payload, length);
    uint8_t tiny[1] = { 0x11 };
    // 70000 one byte continuation packets of one frame go past the 65536 fragment cap
    for (int i = 0; i < 70000; i++) {
        sendPacket(1, 0, tiny, sizeof(tiny));
    }
    sendPacket(1, FLAG_EOF, tiny, sizeof(tiny));
    int total = drainFrames(&idr, &p);
    check(total == 0, "fragment cap: a frame of 70000 fragments is dropped");
    check(idrRequests > 0 || lossReports > 0, "fragment cap: recovery is requested");

    idr = 0;
    p = 0;
    sendSingleFrame(2, IDR_NALS, sizeof(IDR_NALS));
    check(drainFrames(&idr, &p) >= 1, "fragment cap: the next frame decodes");
}

static void testFrameSizeCap(void) {
    resetStream();
    int idr = 0, p = 0;
    uint8_t payload[256];
    int length = buildFirstPayload(payload, IDR_NALS, sizeof(IDR_NALS));
    sendPacket(1, FLAG_SOF, payload, length);
    long before = maxRssKb();
    static uint8_t big[1400];
    memset(big, 0x22, sizeof(big));
    // About 42 MB of continuation data in 30000 packets goes past the 32 MB frame cap
    for (int i = 0; i < 30000; i++) {
        sendPacket(1, 0, big, sizeof(big));
    }
    sendPacket(1, FLAG_EOF, big, sizeof(big));
    long growthMb = (maxRssKb() - before) / 1024;
    printf("     memory growth for a 42 MB frame: %ld MB\n", growthMb);
    int total = drainFrames(&idr, &p);
    check(total == 0, "size cap: a frame over 32 MB is dropped");
    check(growthMb < 40, "size cap: the chain stops near 32 MB");
}

static void testRestartResetsState(void) {
    // Starting a new stream in the same process must start clean
    for (int round = 0; round < 3; round++) {
        resetStream();
        int idr = 0, p = 0;
        sendSingleFrame(1, IDR_NALS, sizeof(IDR_NALS));
        sendSingleFrame(2, P_NALS, sizeof(P_NALS));
        char name[80];
        snprintf(name, sizeof(name), "restart round %d: a fresh stream decodes", round + 1);
        check(drainFrames(&idr, &p) == 2, name);
    }
}

int main(void) {
    ListenerCallbacks.logMessage = logMessage;
    memset(&VideoCallbacks, 0, sizeof(VideoCallbacks));
    StreamConfig.supportedVideoFormats = VIDEO_FORMAT_H264;

    testNormalFrames();
    testContinuationFloodIsRejected();
    testFragmentCap();
    testFrameSizeCap();
    testRestartResetsState();

    printf(failures == 0 ? "ALL PASSED\n" : "%d FAILED\n", failures);
    return failures == 0 ? 0 : 1;
}
