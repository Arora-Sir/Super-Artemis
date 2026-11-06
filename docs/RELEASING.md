# Releasing Super Artemis

This is the step-by-step guide for signing and publishing a release. It follows the process that DualClone uses, with the same scripts and the same protected GitHub environment. It holds no secret values, and the agent never sees the signing password.

## Overview

- Releases are APKs signed with one long-lived key and published through GitHub Releases.
- Signing happens only in the `Release` workflow (`.github/workflows/build-unified.yml`), on a pushed `v*` tag, after a reviewer approves the run in the `release` environment.
- The workflow creates a draft release. The owner publishes it by hand.
- Android, Obtainium and the in-app update check only update in place when the signing key never changes. The key is the most valuable thing in this project.

## Status (2026-10-29)

Steps 1 and 2 are done. The key lives in the shared toolkit at `%USERPROFILE%\.android-keystore\SuperArtemis` (alias `super-artemis`), with a Bitwarden backup. The environment `release`, its four secrets and the variable `RELEASE_CERT_SHA256` exist. Check them any time with `%USERPROFILE%\.android-keystore\0-verify-all.ps1` and `4-verify-key.ps1` (they print status only). The tag ruleset for `v*` is still optional. Sections 1 and 2 stay below as the recovery guide if the key or the secrets ever have to be recreated.

## 1. Generate the release keystore (owner, once, already done)

Use a trusted machine and never create the keystore inside the repository. The numbered scripts in `%USERPROFILE%\.android-keystore` (5 creates the key, 7 sets the GitHub secrets) do the same as the commands below for every app.

```bash
python scripts/release/generate_keystore.py --out-dir "$HOME/.android-keystore/SuperArtemis"
```

The helper prompts for a password of at least 12 characters, runs `keytool` with RSA 4096 and 10000 days of validity, writes `release.jks` and `release.jks.base64` into the folder, and prints only the public certificate SHA-256. The alias is `super-artemis`.

### Key management rules

- Pick a new password. The earlier one leaked into a backup repository history and counts as burned.
- Store the password in a password manager, not next to the file.
- Keep two copies of `release.jks` in separate places you control, such as an encrypted drive and an encrypted vault, and test a restore once with `keytool -list -keystore <copy>`.
- Never commit the keystore, the base64 file or the password. The pre-commit hook blocks keystores and literal passwords.
- If the key is lost, nobody can update in place. A new app with a new key means every user has to uninstall and reinstall.

## 2. Create the protected environment and the secrets (owner, once)

1. In the repository open Settings, Environments, New environment, and name it `release`.
2. Turn on Required reviewers and add yourself. A sole maintainer leaves "Prevent self-review" off, otherwise nobody can approve.
3. Under Deployment branches and tags choose Selected branches and tags and add the tag rule `v*`.
4. Under Settings, Rules, Rulesets add a tag ruleset for `v*` that only maintainers can create, update or delete.
5. Under Settings, Actions, General set workflow permissions to read only, and require approval for all outside collaborators.

Send the keystore straight into its secret so the text is never shown or saved.

PowerShell on Windows:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\.android-keystore\SuperArtemis\release.jks")) | gh secret set KEYSTORE_BASE64 --env release --repo Arora-Sir/Super-Artemis
```

The other three secrets prompt for the value, so the password stays out of the shell history:

```bash
gh secret set KEYSTORE_PASSWORD --env release --repo Arora-Sir/Super-Artemis
gh secret set KEY_ALIAS --env release --repo Arora-Sir/Super-Artemis
gh secret set KEY_PASSWORD --env release --repo Arora-Sir/Super-Artemis
```

Type `super-artemis` for the alias, and the same password for both password secrets. Then delete `release.jks.base64` and check the names:

```bash
gh secret list --env release --repo Arora-Sir/Super-Artemis
```

Read the certificate fingerprint and store it as a repository variable. It is public, and the workflow compares every signed APK against it:

```bash
keytool -list -v -keystore "$HOME/.android-keystore/SuperArtemis/release.jks" -alias super-artemis
gh variable set RELEASE_CERT_SHA256 --repo Arora-Sir/Super-Artemis --body "<the SHA256 line without colons>"
```

Put the same fingerprint in the README, so users can check an APK before installing it.

## 3. How the workflow handles the key

`build-unified.yml` does this, in order.

1. Runs only when the repository is `Arora-Sir/Super-Artemis` and the ref is a tag, so forks and pull requests never reach it.
2. Targets the `release` environment, so the job waits for a reviewer and only then receives the secrets.
3. Checks out the tag and the native submodule without storing the token, and fails when the tagged commit is not on `unified-main`.
4. Checks that the tag equals `v` plus `versionName` and that release notes exist (`scripts/release/check_version.py`).
5. Fails with the secret name, never its value, when any of the four secrets is empty.
6. Decodes `KEYSTORE_BASE64` into `release.jks` under `RUNNER_TEMP` with owner-only permissions and builds `assembleNonRoot_gameRelease`. Gradle reads `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` (see `app/build.gradle`).
7. Runs `scripts/release/verify_release.py`: the signature verifies, the signer is not the debug certificate and matches `RELEASE_CERT_SHA256`, the package and version are right, there is one launcher activity, the build is not debuggable, and every permission is on `scripts/release/allowed_permissions.txt`.
8. Writes `SHA256SUMS.txt`, builds the release notes with the certificate fingerprint, deletes the keystore in a step that always runs, and creates a draft release.

The release build never falls back to the debug key. Third-party actions are pinned by commit SHA and Dependabot proposes updates. Never add `set -x`, an `echo` of a secret or a `cat` of the decoded keystore to the workflow, and review every change to it as security-sensitive.

## 4. Pipeline stages

| Stage | Workflow | Trigger | Secrets |
| :--- | :--- | :--- | :--- |
| Docs and credits | `docs-check.yml` | Every push and pull request | None |
| Build and test | `ci.yml` | Pull request and push to `unified-main` | None |
| Upstream watch | `upstream-watch.yml` | Weekly | None |
| Release | `build-unified.yml` | Push of a `v*` tag | Environment `release` |

## 5. Each release (owner)

1. Move the `[Unreleased]` entries of `CHANGELOG.md` under the new version, bump `versionName` and `versionCode` in `app/build.gradle`, and add `.github/release-notes/vX.Y.Z.md`.
2. Run `python scripts/release/check_version.py --tag vX.Y.Z`, the unit suite, the dash and credits checks, and the upstream review (`python scripts/upstream_watch.py --mark-reviewed`).
3. Merge to `unified-main` and wait for `ci.yml` to pass.
4. Create the tag on `unified-main`: `git tag vX.Y.Z` and `git push origin vX.Y.Z`.
5. Approve the run in the `release` environment.
6. Download the draft APK. Run `python scripts/release/verify_release.py <apk> --expect-version X.Y.Z`, and check that the fingerprint equals the one in the README.
7. Install it next to the debug build on the Tab and the S24, open it, connect once.
8. Publish the draft. Obtainium and the in-app update check only see a published release.

## 6. Local signed build

Set the environment variables `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`, then run `./gradlew assembleNonRoot_gameRelease`. With no key set, the build leaves an unsigned APK and `verify_release.py` rejects it.

## 7. If something goes wrong

- A secret leaked: delete it in GitHub, then change the keystore passwords with `keytool -storepasswd` and `-keypasswd` and set the secrets again.
- The keystore leaked: the key is compromised. Generate a new key, publish the new fingerprint in the release notes, and plan an APK Signature Scheme v3 key rotation so updates can continue. Very old Android versions need a reinstall.
- A bad draft: delete the draft release and the tag (the tag ruleset needs a temporary owner override), fix the cause, and tag again.
- A bad published release: never reuse a version. Ship the next patch version and point the old notes to it.

## 8. What the agent does and does not do

The agent writes the scripts, the workflows and this guide, and runs the checks. It never reads, copies or uploads a keystore, never types or sees a signing password, and never creates the tag or pushes without an explicit yes.
