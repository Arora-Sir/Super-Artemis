#!/bin/sh
# Builds audio_queue_foreign_packet.c with the Android NDK and runs it on one adb device or emulator.
# Usage: run_audio_queue_test.sh <adb serial>. Use a private emulator, never a device that is streaming.
# NOTE: The binary lives in /data/local/tmp and is removed afterwards, no app data is touched.
set -e
SERIAL="$1"
[ -n "$SERIAL" ] || { echo "usage: $0 <adb serial>"; exit 2; }
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CORE="$ROOT/app/src/main/jni/moonlight-core/moonlight-common-c"
NDK_BIN="${NDK_BIN:-$LOCALAPPDATA/Android/Sdk/ndk/29.0.13113456/toolchains/llvm/prebuilt/windows-x86_64/bin}"
OUT="${TMPDIR:-/tmp}/audio_queue_test"
"$NDK_BIN/aarch64-linux-android26-clang" -DLC_ANDROID -DLC_DEBUG -DHAS_SOCKLEN_T=1 \
  -I"$CORE/src" -I"$CORE/reedsolomon" -I"$CORE/enet/include" \
  "$ROOT/scripts/native_tests/audio_queue_foreign_packet.c" "$CORE/src/RtpAudioQueue.c" "$CORE/reedsolomon/rs.c" -o "$OUT"
MSYS_NO_PATHCONV=1 adb -s "$SERIAL" push "$OUT" /data/local/tmp/audio_queue_test >/dev/null
MSYS_NO_PATHCONV=1 adb -s "$SERIAL" shell "chmod 755 /data/local/tmp/audio_queue_test && /data/local/tmp/audio_queue_test; echo exit=\$?; rm -f /data/local/tmp/audio_queue_test"
