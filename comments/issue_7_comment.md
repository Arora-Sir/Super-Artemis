Pull request #11 has been received and reviewed for part 1 of this request.

The JNI export `MoonBridge.getControlStreamSilenceMs()` and the 500 ms watchdog in `Game.java` correctly present the connection status overlay over the last decoded frame without tearing down the surface.

All checks passed. We will proceed with testing and merging PR #11 before reviewing the automatic resumer in part 2.
