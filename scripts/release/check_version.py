"""Check that the version is consistent across Gradle, the release notes and the changelog.

Usage:
    python scripts/release/check_version.py
    python scripts/release/check_version.py --tag v1.0.0

With --tag the tag must equal v plus versionName and the release needs notes: a file
.github/release-notes/<tag>.md or a dated changelog entry. Without --tag the changelog may still be Unreleased.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def gradle_version() -> tuple[str, int]:
    text = (ROOT / "app" / "build.gradle").read_text(encoding="utf-8")
    name = re.search(r'versionName\s+"([^"]+)"', text)
    code = re.search(r"versionCode\s*=?\s*(\d+)", text)
    if not name or not code:
        raise SystemExit("versionName or versionCode not found in app/build.gradle")
    return name.group(1), int(code.group(1))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--tag", help="Git tag being released, for example v1.0.0")
    args = parser.parse_args()

    version, _code = gradle_version()
    problems: list[str] = []

    if args.tag and args.tag != f"v{version}":
        problems.append(f"tag {args.tag} does not equal v{version} (versionName in app/build.gradle)")

    changelog = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
    entry = re.search(rf"^## \[{re.escape(version)}\] - \d{{4}}-\d{{2}}-\d{{2}}\s*$", changelog, re.MULTILINE)
    notes = ROOT / ".github" / "release-notes" / f"v{version}.md"
    if args.tag and not entry and not notes.is_file():
        problems.append(f"No release notes: add .github/release-notes/v{version}.md or a dated CHANGELOG.md entry '## [{version}] - YYYY-MM-DD'")
    if not args.tag and not entry and not notes.is_file() and "## [Unreleased]" not in changelog:
        problems.append("CHANGELOG.md has neither an Unreleased section nor an entry for this version")

    for item in problems:
        print(item)
    print(f"check_version: {len(problems)} problem(s), versionName {version}")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
