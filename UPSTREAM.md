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
| Forks | qiin2333/moonlight-vplus, derflacco/moonlight-android (`moonlight-noir` and `Artemide_Experimental_Async`), MikalaiKryvusha/KAST, Rafael-CN/VibeArtemis (`dev`), coroiu/twilight-android, logabell/moonlight-android |

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
