#!/bin/sh
# Builds depacketizer_bounds.c with the Android NDK and runs it on one adb device or emulator.
# Usage: run_depacketizer_test.sh <adb serial> [path to another VideoDepacketizer.c to test instead of the current one]
# Use a private emulator, never a device that is streaming.
# NOTE: The binary lives in /data/local/tmp and is removed afterwards, no app data is touched.
set -e
SERIAL="$1"
DEPACKETIZER="$2"
[ -n "$SERIAL" ] || { echo "usage: $0 <adb serial> [VideoDepacketizer.c]"; exit 2; }
ROOT="$(cd "$(dirname "$0")/../.." && (pwd -W 2>/dev/null || pwd))"
CORE="$ROOT/app/src/main/jni/moonlight-core/moonlight-common-c"
[ -n "$DEPACKETIZER" ] || DEPACKETIZER="$CORE/src/VideoDepacketizer.c"
NDK_BIN="${NDK_BIN:-$LOCALAPPDATA/Android/Sdk/ndk/29.0.13113456/toolchains/llvm/prebuilt/windows-x86_64/bin}"
ABI="$(MSYS_NO_PATHCONV=1 adb -s "$SERIAL" shell getprop ro.product.cpu.abi | tr -d '\r')"
case "$ABI" in
  x86_64) CC="$NDK_BIN/x86_64-linux-android26-clang" ;;
  arm64-v8a) CC="$NDK_BIN/aarch64-linux-android26-clang" ;;
  *) echo "unsupported abi $ABI"; exit 2 ;;
esac
OUT="${TMPDIR:-/tmp}/depacketizer_test"
"$CC" -DLC_ANDROID -DHAS_SOCKLEN_T=1 $EXTRA_CFLAGS \
  -I"$CORE/src" -I"$CORE/reedsolomon" -I"$CORE/enet/include" \
  "$ROOT/scripts/native_tests/depacketizer_bounds.c" "$DEPACKETIZER" "$CORE/src/LinkedBlockingQueue.c" "$CORE/src/ByteBuffer.c" -o "$OUT"
MSYS_NO_PATHCONV=1 adb -s "$SERIAL" push "$OUT" /data/local/tmp/depacketizer_test >/dev/null
MSYS_NO_PATHCONV=1 adb -s "$SERIAL" shell "chmod 755 /data/local/tmp/depacketizer_test && /data/local/tmp/depacketizer_test; echo exit=\$?; rm -f /data/local/tmp/depacketizer_test"
