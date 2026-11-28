# Network paths and slow stream starts

How Super Artemis picks the address of a PC, what happens when the first path is not ready, and what was measured. This file is the research record for the 2026-11-21 Tailscale start that took about 20 seconds and failed.

## How an address is chosen

A PC keeps up to four saved addresses: local (from mDNS), manual (typed by the user, also where a Tailscale address goes), remote (found with STUN) and IPv6. `ComputerManagerService.parallelPollPc` polls all four at once but reads the results in strict order: local, manual, remote, IPv6. The first one that answers becomes the active address and is passed to `Game`.

Consequences:

- A stale local address (the router moved from 192.168.0.x to 192.168.1.x) holds up the result for its full connect timeout, 3 s, or 5 s when the PC was online before. Only then is the manual Tailscale address considered.
- Nothing saves a Tailscale address by itself. It works because it was added by hand as the manual address.
- Wake-on-LAN cannot reach a sleeping PC from outside the LAN. A shortcut launch of an offline PC repeats the attempt up to 10 times before it shows the offline dialog.
- A hostname or MagicDNS name typed as the manual address only resolves while the VPN provides DNS.

## Measured case: 2026-11-21 22:45, Tab S10 Ultra to Predator-Helios-300

Timeline from the tablet log, the Apollo log and the PC's Tailscale status:

| Time | What happened |
| :--- | :--- |
| 22:45:09 | The tablet's Wi-Fi network changed. |
| 22:45:21 | Shortcut launch. A Wake-on-LAN send failed with `ENETUNREACH`. The stored local address was stale. |
| 22:45:24 | Poll finished. The PC was reached over Tailscale and `Game` started. |
| 22:45:25 | The host accepted the launch, then spent about 2 s testing encoders. |
| 22:45:27.5 | Host session started. The host named the client by its Tailscale address. |
| 22:45:27 to 22:45:38 | The client's RTSP handshake waited about 10 s. |
| 22:45:37.6 | The host logged `Initial Ping Timeout`. It never logged `CLIENT CONNECTED`. |
| 22:45:38 to 22:45:46 | The client's ENet connect to UDP 47999 failed (error 11). |
| 22:45:52 | The user opened the PC again. Discovery found the LAN address in 40 ms. |
| 22:45:56.6 | LAN session connected. First frame about 4.5 s after the launch. |

What this shows:

- TCP over Tailscale worked: the HTTP launch and the RTSP handshake both reached the host.
- UDP media did not arrive during the first attempt. The host never accepted the client's ping.
- The 20 seconds is two waits inside moonlight-common-c: the RTSP stage (10 s connect timeout, `RtspConnection.c`) and the ENet connect (8 s, the library's own retry schedule, `ControlStream.c`). The client cannot tell earlier that the UDP path is dead, because the audio and video ping threads only send and never read a reply. No safe client-side shortening exists.
- Why UDP did not arrive is not proven. Open candidates:
  - H1: Tailscale was still moving its path after the Wi-Fi change.
  - H2: the tablet's native UDP sockets were not on the VPN network yet.
  - H3: the host received pings but mishandled the Tailscale interface.
  - H4: a one-off.

## Ruled out

- Windows firewall: the Apollo rules apply to every profile and the Tailscale network is Private.
- Tailscale now: the tablet and the PC are on a direct path (22 ms) and `tailscale netcheck` reports UDP, IPv4 and IPv6 working.
- The host's `WSASendMsg() failed: 10022` lines as the cause. Of 49 Apollo sessions since 2026-11-19, 48 connected, and many of them logged these lines, including the successful LAN retry (510 lines) right after the failed one (516 lines). They do not separate success from failure. They are still not proven harmless: Sunshine issue 5443 reports them as fatal on port-forwarded WAN paths, and benign during setup over Tailscale.

## External findings

- Tailscale starts every connection on a DERP relay and tries a direct path in parallel, usually upgrading within a few seconds. No document gives a recovery time after a Wi-Fi change on Android. https://tailscale.com/kb/1257/connection-types and https://tailscale.com/blog/how-nat-traversal-works
- Apollo issue 1232 and Sunshine issue 5443 describe video and audio UDP failing over Tailscale while TCP works, with no confirmed fix. https://github.com/ClassicOldSong/Apollo/issues/1232 and https://github.com/LizardByte/Sunshine/issues/5443
- In Sunshine, `Initial Ping Timeout` means no accepted ping arrived within `ping_timeout` (default 10 s). Pings that do arrive are logged with the sender address, so a log with none means no packet reached the receiver. This comes from a summarized read of `src/stream.cpp` and is not fully verified.
- Tailscale's default MTU is 1280. An MTU fix for Moonlight over Tailscale is untested here.

## Open questions and planned tests

Tests are run one at a time on the tablet with its serial pinned, and only when the tablet and the PC desktop are free.

- E2: tablet Wi-Fi off, 2 s, on, then launch over the Tailscale address at once and again 60 s later. Compare first-frame times and whether the host logs a ping.
- E1: tablet on the S24's mobile hotspot, PC on home Wi-Fi, both on Tailscale, five launches.
- E3: during E1 and E2, watch `tailscale status` and `tailscale ping` on the PC for relay versus direct.
- E5: only if a failure reproduces, capture UDP 47998 to 48000 on the PC to see whether any client packet arrives.

## E2 result (2026-11-21, Tab S10 Ultra, debug build 1.2.0)

The debug app's saved PC record held a stale local address and the Tailscale manual address, the same setup as the failed start.

- Two launches with no network change connected in 4.4 s and 7.4 s (the host saw the tablet's LAN address, so Tailscale was not the path).
- Three launches made right after turning the tablet's Wi-Fi off and on with `svc wifi` never started a stream within 50 s. The PC's `tailscale status` showed the tablet as `relay`, `offline, last seen` 4 min, then 8 min, so the tablet's Tailscale did not come back after the Wi-Fi change during the whole test. The tablet's own Wi-Fi did get its address back (it answered a LAN ping).
- Turning Wi-Fi off and on also dropped wireless adb (both the LAN and the Tailscale address went offline and `adb connect` timed out), so the test stopped there and the tablet's state could not be read further.

What it suggests, not proven: after a Wi-Fi change the tablet's Tailscale can stay disconnected for minutes, which fits a Tailscale path that is not ready when the stream starts (H1 or H2). It may also be specific to `svc wifi` or to the tablet dozing. Needs a repeat with the tablet awake and a real network switch.

## E1 attempt (2026-11-22 00:01 to 00:05, user-driven hotspot switch)

The user moved the tablet between home Wi-Fi and the S24's hotspot and launched the Desktop shortcut each time. Tablet log and Apollo log together:

| Time | Tablet network | Host saw client | To CLIENT CONNECTED |
| :--- | :--- | :--- | :--- |
| 00:01:15 to 00:01:42 | no network (polls failed every 3 s) | none | none |
| 00:01:45 joined home Wi-Fi, 00:01:47 launch | home Wi-Fi | LAN address of the tablet | about 4 s |
| 00:04:30 joined the hotspot, 00:04:35 launch | S24 hotspot | LAN address of the S24 | about 2 s |
| 00:04:53 back on home Wi-Fi, 00:04:57 launch | home Wi-Fi | LAN address of the tablet | about 4 s |

What this shows:

- A launch 2 s after joining a different network connected in 2 to 4 s, so a network change alone does not cause the slow start.
- The hotspot run was not an away-from-home test. The host saw the S24's own LAN address, which means the S24 was on home Wi-Fi and shared it, so the tablet reached the PC's LAN address through the S24. Tailscale was never the path.
- The 22:45 failure is still not reproduced. A real away test needs the S24 on mobile data (S24 Wi-Fi off) with the hotspot on, so the PC's LAN address is unreachable from the tablet.

## Emulator replication (2026-11-22, Android 13 x86_64 emulator, root debug build)

The emulator reaches the PC's Apollo through its host alias `10.0.2.2`, so the saved record was seeded with a manual address of `10.0.2.2` and a local address that drops packets (`203.0.113.5`, a documentation range). This reproduces only the address-selection wait, not the Tailscale UDP failure, because the emulator has no Tailscale.

Corrections to earlier notes:

- The first poll in a fresh app process took 9 to 22 s on the emulator (the answer from `10.0.2.2` itself took that long), with the stale address or without it. That is cold-start work on a slow emulator and not an address wait, so warm polls are used for comparison.
- A local address that does not exist on the emulator network (`10.0.2.99`) fails at once and cannot reproduce the wait. A packet-dropping address does.
- The root flavor installs as `com.limelight.root.superartemis.debug`. The nonRoot flavor on the tablet and the old emulator install is `com.mohitarora.superartemis.debug`. Building `assembleRootDebug` and installing it does not update the other package.

Measured warm poll durations with a dead local address and a working manual address:

| Build | Poll duration |
| :--- | :--- |
| Before (waits for local): local fails after 3.0 s | about 3.0 s |
| After, grace 1500 ms | about 2.3 s |
| After, grace 600 ms | about 1.2 s (0.7 s when the manual answer came back fast) |

With the roles swapped (fast local, dead manual) the poll took 0.05 to 0.8 s and kept the local address, so the LAN is still preferred when it answers.

The first Tailscale UDP failure of 2026-11-21 is not addressed by this. It only removes the 3 s the tablet spent waiting on the stale local address at the start of that launch.

## Retry change, tried and removed

An uncommitted retry (`session/PathRetry`) relaunched once through `ShortcutTrampoline` when the control stream failed over a non-LAN address. It was removed on 2026-11-27 without being committed: it never ran in any test, and a fresh poll away from home would have chosen the same Tailscale address again. Reconsider a retry only after a connect failure is reproduced and a fresh poll is shown to find a better address.

## Away-from-home start on the S24 (2026-11-22 13:55, debug build with the poll and surface fixes plus the retry that was later removed)

The S24 Ultra was on LTE with Wi-Fi off, and the PC was on home Wi-Fi. Tailscale reached the phone directly over IPv6 at 147 ms. A cold start of the app, a tap on the PC and a tap on the Desktop tile gave:

- The poll chose the Tailscale address in 0.25 s. The IPv6 address kept failing after 3 s in the background and no longer held the result up.
- The Desktop tap to the first decoded frame took about 1.5 s: resume request at +0.5 s, first video packet at +1.4 s, frame at +1.5 s.
- This is one run. The 2026-11-21 failure was after a Wi-Fi network change and this run had none, so that case is still not reproduced.

## Network switch tests on the S24 (2026-11-26, same debug build)

The user switched the S24 by hand. Runs 1 and 2 were on mobile data with Wi-Fi off. Run 3 was started by a script on the phone 5 s after it joined home Wi-Fi. All three used the Tailscale address. The host reported a second active session during each run. Times exclude app startup, because the debug build starts slowly.

| Step | Run 1 | Run 2 | Run 3 (just joined Wi-Fi) |
| :--- | :--- | :--- | :--- |
| Poll answer over Tailscale | about 0.8 s | about 0.8 s | 12.4 s |
| Surface ready to resume answer | 0.9 s | 0.9 s | 3.2 s |
| RTSP handshake | 1.2 s | 1.7 s | 6.9 s |
| Resume answer to first frame | 1.6 s | 2.3 s | 7.9 s |
| Poll start to first frame | 3.6 s | 4.3 s | 23.9 s |
| First audio packet | logged at once | logged at once | none logged in the first 15 s |

What this shows:

- A slow start after a network change reproduces. Run 3 reached a picture, about 31 s after Wi-Fi joined. The 2026-11-21 failure to connect did not reproduce.
- The time went into TCP requests over Tailscale. The host received the bitrate request 6.9 s after the display mode request, against 1.0 s and 1.3 s in runs 1 and 2, which matches the client's handshake time. This fits a Tailscale path that was still settling (H1). The path itself was not measured during the run.
- Run 3 had video without a logged audio packet, the same symptom as the failed start of 2026-11-22. The cause is unknown.
- The home network was reachable minutes later: the phone pinged the PC's LAN address at 12 ms average and got HTTP 200 from `/serverinfo` there. LAN reach during the first seconds after joining was not measured.
- Local discovery ran for 12.5 s in run 3 and reported no PC. The cause is unknown.
- The retry described above never ran, because no run failed. A fresh poll would have chosen the same address.

On the 3.8 s host setup of 2026-11-22: the host finished audio setup 1.1 s after the session started and then waited 2.7 s for the client's control connection. It also logged six `Ping Timeout` lines for the same phone in the first second, probably leftover sessions. Neither is proven to be the cause.

## The saved home address was overwritten

The host reports as `LocalIP` the address each request arrived on. Measured on 2026-11-26: `/serverinfo` asked over the LAN returns the LAN address, and asked over Tailscale returns the Tailscale address. `NvHTTP` stores that value as the local address and `ComputerDetails.update` saved it, so one poll over Tailscale replaced the home address. The S24's record held the Tailscale address as both local and manual, which left local discovery as the only way back to the LAN.

`ComputerDetails.update` now keeps a saved home address when the reported one is in the Tailscale range (`100.64.0.0/10` or `fd7a:115c:a1e0::/48`). A record that already lost its home address gets it back when discovery or a poll over the LAN reports it again. Whether the kept address makes a start right after joining Wi-Fi faster is not yet measured.

Checked on the S24 on 2026-11-26 with the fixed debug build:

1. At home, discovery stored the LAN address as the local address and left the Tailscale address as the manual one. Polls then picked the LAN address in about 0.2 s.
2. With Wi-Fi off, the polls picked the Tailscale address (at least six in a row) and the saved record still held the LAN address as local.

How long discovery gets matters. A start that resumes straight into the stream stops it after about 0.6 s, and a PC list visit of 1.8 s found nothing. Two further windows of 2.5 s and 1.2 s found nothing either, and a later one resolved the PC in 30 ms. Why discovery succeeds only sometimes is unknown.
