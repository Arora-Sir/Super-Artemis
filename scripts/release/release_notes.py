"""Print GitHub release notes for one version.

Usage:
    python scripts/release/release_notes.py v1.0.0 --cert-file cert.txt > RELEASE_NOTES.md

Uses .github/release-notes/<tag>.md when it exists, otherwise the dated CHANGELOG.md section, and otherwise the
Thanks section that scripts/render_credits.py builds from credits.json. The signing certificate SHA-256 and a short
verification note are appended when a certificate file is given.
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def changelog_section(version: str) -> str | None:
    text = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
    match = re.search(rf"^## \[{re.escape(version)}\] - \d{{4}}-\d{{2}}-\d{{2}}\s*$", text, re.MULTILINE)
    if not match:
        return None
    rest = text[match.end():]
    nxt = re.search(r"^## \[", rest, re.MULTILINE)
    body = rest[: nxt.start()] if nxt else rest
    return re.sub(r"^---\s*$", "", body, flags=re.MULTILINE).strip()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("tag", help="Release tag, for example v1.0.0")
    parser.add_argument("--cert-file", type=Path, help="File holding the signer certificate SHA-256")
    args = parser.parse_args()

    notes_file = ROOT / ".github" / "release-notes" / f"{args.tag}.md"
    if not notes_file.is_file():
        base_tag = args.tag.split("-")[0]
        notes_file = ROOT / ".github" / "release-notes" / f"{base_tag}.md"
    if notes_file.is_file():
        body = notes_file.read_text(encoding="utf-8").strip()
    else:
        raw_version = args.tag.lstrip("v").split("-")[0]
        body = changelog_section(raw_version) or subprocess.run(
            [sys.executable, str(ROOT / "scripts" / "render_credits.py"), "--release-notes"],
            capture_output=True, text=True, check=True).stdout.strip()

    parts = [body]
    if args.cert_file and args.cert_file.is_file():
        digest = args.cert_file.read_text(encoding="utf-8").strip()
        parts.append(
            "### Verify this release\n\n"
            f"Signing certificate SHA-256: `{digest}`\n\n"
            "Compare it with the fingerprint in the README before installing. "
            "`SHA256SUMS.txt` holds the SHA-256 checksum of the APK."
        )
    sys.stdout.write("\n\n".join(parts) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
