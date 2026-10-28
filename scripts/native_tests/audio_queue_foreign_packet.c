// Regression test for the debug-build abort in RtpAudioQueue.c (BUG-043).
// NOTE: Datagrams from other senders reach the unconnected audio socket, so the queue must drop them instead of aborting.
// NOTE: Build and run it with scripts/native_tests/run_audio_queue_test.sh, which uses the Android NDK and adb on a private device or emulator.
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include "Limelight-internal.h"
#include "RtpAudioQueue.h"

int AppVersionQuad[4] = { 7, 1, 431, -1 };
int AudioPacketDuration = 5;
CONNECTION_LISTENER_CALLBACKS ListenerCallbacks;

uint64_t PltGetMillis(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint64_t)ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}

static int logLines;

static void logMessage(const char* format, ...) {
    logLines++;
}

static int failures;

static void expectDropped(PRTP_AUDIO_QUEUE queue, const char* name, uint8_t type, uint16_t length) {
    uint8_t buffer[512];
    PRTP_PACKET packet = (PRTP_PACKET)buffer;

    memset(buffer, 0x5a, sizeof(buffer));
    packet->header = 0x80;
    packet->packetType = type;
    packet->sequenceNumber = 100;
    packet->timestamp = 1000;
    packet->ssrc = 0;

    int status = RtpaAddPacket(queue, packet, length);
    if (RTPQ_PACKET_CONSUMED(status) || RTPQ_PACKET_READY(status) || RTPQ_HANDLE_NOW(status)) {
        printf("FAIL %s: status %d\n", name, status);
        failures++;
    }
    else {
        printf("ok   %s\n", name);
    }
}

int main(void) {
    RTP_AUDIO_QUEUE queue;

    ListenerCallbacks.logMessage = logMessage;
    RtpaInitializeQueue(&queue);

    expectDropped(&queue, "type 0", 0, 200);
    expectDropped(&queue, "type 0x55", 0x55, 200);
    expectDropped(&queue, "type 96", 96, 200);
    expectDropped(&queue, "type 98", 98, 200);
    expectDropped(&queue, "type 255", 255, 200);
    expectDropped(&queue, "truncated audio packet", 97, 4);
    expectDropped(&queue, "truncated FEC packet", 127, (uint16_t)sizeof(RTP_PACKET) + 2);

    if (logLines == 0) {
        printf("FAIL no log line was written\n");
        failures++;
    }

    RtpaCleanupQueue(&queue);
    printf(failures == 0 ? "PASS\n" : "FAILED\n");
    return failures == 0 ? 0 : 1;
}
