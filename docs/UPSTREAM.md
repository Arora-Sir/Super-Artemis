# Staying current with upstream

Super Artemis builds on Artemis, Moonlight, moonlight-common-c and Apollo, and borrows fixes from several forks. This file is the rule for keeping up with them, for both features and security.

## What is tracked

`scripts/upstream_sources.json` lists every repository and branch, with the last commit someone reviewed:

| Role | Repositories |
| :--- | :--- |
| Base | ClassicOldSong/moonlight-android (`moonlight-noir`) |
| Upstream | moonlight-stream/moonlight-android |
| Native core | ClassicOldSong/moonlight-common-c, moonlight-stream/moonlight-common-c (our fork: Arora-Sir/moonlight-common-c, branch `super-artemis`) |
| Host | ClassicOldSong/Apollo |
| Forks | qiin2333/moonlight-vplus, derflacco/moonlight-android (`moonlight-noir` and `Artemide_Experimental_Async`), MikalaiKryvusha/KAST (`moonlight-noir`), Rafael-CN/VibeArtemis (`dev`), coroiu/twilight-android (`moonlight-noir`), logabell/moonlight-android (`moonlight-noir`), 4had45/moonlit-artemis (`moonlit`), MrOz59/Hestia-Android (`moonlight-noir`), kefrens/ariane-android (`ariane-main`), joemossjr16/artemis-android-pyrowave (`pyrowave`), Taveszfito/Artemis-Extended-Native-Wireless-DualSense-Features (`agent/controller-emulation-settings`) |

Add a repository there when code is taken from a new source, so it is watched from then on.

## How changes are found

- Every Monday the "Upstream watch" workflow runs `scripts/upstream_watch.py` and updates one open issue labelled `upstream-watch` with new commits (likely fixes are flagged), releases and published security advisories since each `reviewed` commit.
- Run it locally at any time with `python scripts/upstream_watch.py` (set `GH_TOKEN` for a higher API limit).
- Dependabot (`.github/dependabot.yml`) opens pull requests for Gradle libraries, GitHub Actions and the native submodule, and GitHub security alerts cover known vulnerable dependencies.

## When to review

- Security advisories and fixes for crashes, hangs or data exposure are reviewed as soon as they appear.
- Everything else is reviewed before every release and whenever the weekly issue lists new items.
- Before tagging a release, the report must show nothing unreviewed.

## How to take a change

1. Read the actual diff and the reasoning behind it. Prefer small, proven fixes. Large rewrites need a device test plan before they are taken.
2. Port with authorship: `git am` or `git cherry-pick` keeps the original author for clean picks. A hand port carries a `Co-authored-by:` trailer with the author's GitHub noreply address. Every commit lists its sources in a `Refs:` line (see CONTRIBUTING.md).
3. Keep upstream files close to upstream: change only what the fix needs and keep each file's line ending style (BUGS.md, BUG-008).
4. Add the people to `app/src/main/res/raw/credits.json`, run `python scripts/render_credits.py --write`, and add a CHANGELOG entry.
5. Run the unit suite and the device checks the change touches.

## Recording the review

After a review, mark every source as reviewed:

```
python scripts/upstream_watch.py --mark-reviewed
```

Commit the updated `scripts/upstream_sources.json` with the ports, so the next report starts where this review ended. Close the weekly issue when it lists nothing new.

## Deferred upstream changes

Marking a source as reviewed moves its reviewed commit forward, so anything reviewed and not taken is listed here to stay visible. Each entry needs its own branch and a long stream test before it is taken.

- moonlight-stream/moonlight-common-c, reviewed 2026-11-18. Taken on 2026-11-20 for 1.3.0 and credited to Cameron Gutman: the `isInSubnetV6` mask fix (`dc09c9f2e`), the `motionType` check (`f0df33caf`), the ENet batching fix for mouse and UTF-8 input (`d5b38a3ac`), the multi-waiter fix for Lbq and PLT_EVENT (`eac4c469c`) and the `LiGetEstimatedRttInfo` race fix (`a0262e78e`, merged by hand, with `stdatomic.h` included explicitly). Still deferred: the VideoDepacketizer lock (`e64327c86`), which needs newer upstream interfaces that this fork does not have (frame type flags and microsecond timestamps) and conflicts in 5 places, and the RTP stats redesign (`541e39683`), which the app does not need because it never calls the RTP stats functions and only uses `LiGetPendingVideoFrames`. Revisit both when the fork is rebased onto a newer upstream core.
- qiin2333/moonlight-vplus, reviewed 2026-11-18 and not taken for 1.2.0: the DualSense Bluetooth host state check (`37a3b3b80`, a test only change in a haptics path this fork does not have) and the bounded long vibration stop (`2318af027`, built on vplus's authored haptics pipeline, which this fork does not have). The ARM64 AES detection (`6c58f7a76`) was ported on 2026-11-19 and credited.
- The review for release 1.2.0 found no security advisory for any readable source. Four sources answered 403 and could not be read.
- qiin2333/moonlight-vplus, reviewed 2026-11-21 (21 commits since `c8619aaaf6f3`, releases v12.15.28 to v12.15.31) and not taken. No security advisory was published for any source. The input fixes (`6740261d7` accessibility key sequence ownership, `4c217ec53` keyboard state while the stream is paused, `e7272f407` external mouse sending absolute coordinates after remote desktop mode is turned off, `7b24f56f0` stylus input after pan and zoom) are written against vplus's Kotlin `Game.kt` and `KeyboardInputHandler.kt` and its own remote desktop mode and accessibility service, which this fork does not have, so they cannot be cherry-picked. Revisit them if the same symptoms show up here. The rest is vplus's own UI (crown profile editor, performance monitor menu, switch styling), Chinese string work, native keyboard capture for Android 16.1 and external stream windows (needs a device this fork's owner does not have), and release and CI bookkeeping.
- Rafael-CN/VibeArtemis, reviewed 2026-11-21: two repository hygiene commits (planning docs and agent config moved out of the public repo), nothing to take.
