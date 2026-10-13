# Super Artemis project rules

Super Artemis is a fork of Artemis (ClassicOldSong/moonlight-android, branch `moonlight-noir`). Start with CONTEXT.md for the current state, then ARCHITECTURE.md, SETUP.md and BUGS.md.

## Always

- Credit every person, PR, issue and commit used: `credits.json`, `Co-authored-by:` trailers with GitHub noreply addresses and `Refs:` lines (CONTRIBUTING.md).
- Follow UPSTREAM.md: before every release and whenever the weekly "Upstream watch" issue lists new items, review upstream and fork changes for security fixes, bug fixes and features, port what fits with credit, then run `python scripts/upstream_watch.py --mark-reviewed`.
- Keep `Game.java` and `StreamSettings.java` as wiring only. New behaviour goes in its own package (`actions/`, `menu/`, `customkeys/`, `gestures/`, `session/`, `backup/`, `about/`, `theme/`).
- Keep each upstream file's line ending style (BUGS.md, BUG-008) and change only what a feature needs.
- Update CONTEXT.md, CHANGELOG.md (Unreleased) and, when structure or a workaround changes, ARCHITECTURE.md and BUGS.md in the same change.
- Docs use no em dashes, en dashes or double hyphens. Run `python scripts/clean_dashes.py --check` on changed docs.
- The unit suite must show no failures beyond the five known upstream ones (BUGS.md, BUG-009).
- Never commit or push without showing the diff, message and branch and getting an explicit yes.
