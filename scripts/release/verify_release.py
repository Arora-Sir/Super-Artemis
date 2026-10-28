"""Verify a release APK before it is published.

Runs the same checks locally and in CI.
1. apksigner verify passes and the signer is not the Android debug certificate.
2. aapt2 dump badging reports the expected package, version, one launcher activity and a non-debuggable build.
3. Every requested permission is in scripts/release/allowed_permissions.txt. A new permission fails the check.

Usage:
    python scripts/release/verify_release.py path/to/SuperArtemis-v1.0.0-arm64-v8a.apk
    python scripts/release/verify_release.py app.apk --expect-version 1.0.0 --expect-cert-sha256 <hex> --cert-out cert.txt

Tools are found through ANDROID_HOME or ANDROID_SDK_ROOT (newest build-tools) and then PATH.
"""
from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

PACKAGE = "com.mohitarora.superartemis"
ALLOWLIST_FILE = Path(__file__).resolve().parent / "allowed_permissions.txt"


def load_allowed() -> set[str]:
    """Reads the permission allow-list. A leading dot means the permission is prefixed with the package id."""
    allowed = set()
    for line in ALLOWLIST_FILE.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        allowed.add(PACKAGE + line if line.startswith(".") else line)
    return allowed


def find_tool(name: str) -> str:
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if sdk:
        build_tools = Path(sdk) / "build-tools"
        if build_tools.is_dir():
            versions = sorted(
                build_tools.iterdir(),
                key=lambda p: [int(x) if x.isdigit() else 0 for x in re.split(r"[.\-]", p.name)],
                reverse=True,
            )
            for version in versions:
                for suffix in ("", ".bat", ".exe"):
                    candidate = version / f"{name}{suffix}"
                    if candidate.is_file():
                        return str(candidate)
    found = shutil.which(name)
    if found:
        return found
    raise SystemExit(f"{name} not found. Set ANDROID_HOME or add build-tools to PATH.")


def run(cmd: list[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(cmd, capture_output=True, text=True)


def check_signature(apk: Path, problems: list[str]) -> str | None:
    result = run([find_tool("apksigner"), "verify", "--verbose", "--print-certs", str(apk)])
    output = result.stdout + result.stderr
    if result.returncode != 0:
        problems.append(f"apksigner verify failed: {output.strip()[:400]}")
        return None
    if "Verified using v2 scheme (APK Signature Scheme v2): true" not in output and "v3 scheme" not in output:
        problems.append("APK is not signed with signature scheme v2 or v3")
    digest = re.search(r"certificate SHA-256 digest: ([0-9a-fA-F]+)", output)
    if not digest:
        problems.append("Could not read the signer certificate digest")
        return None
    if re.search(r"Signer #1 certificate DN:.*CN=Android Debug", output):
        problems.append("APK is signed with the Android debug certificate")
    return digest.group(1).lower()


def check_badging(apk: Path, expect_version: str | None, problems: list[str]) -> None:
    result = run([find_tool("aapt2"), "dump", "badging", str(apk)])
    if result.returncode != 0:
        problems.append(f"aapt2 dump badging failed: {result.stderr.strip()[:400]}")
        return
    text = result.stdout
    package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", text)
    if not package:
        problems.append("Could not read the package line from badging")
        return
    name, _code, version = package.groups()
    if name != PACKAGE:
        problems.append(f"Unexpected package id {name}, expected {PACKAGE}")
    if expect_version and version != expect_version:
        problems.append(f"versionName is {version}, expected {expect_version}")
    permissions = re.findall(r"uses-permission(?:-sdk-23)?: name='([^']+)'", text)
    allowed = load_allowed()
    for permission in sorted(set(permissions) - allowed):
        problems.append(f"Permission not on the allow-list: {permission}. Review it, then add it to allowed_permissions.txt on purpose")
    for debug_package_permission in (p for p in permissions if p.startswith(PACKAGE + ".debug")):
        problems.append(f"Debug build permission in a release APK: {debug_package_permission}")
    # The app is also a leanback launcher, which aapt2 prints on its own line, so only the plain launcher line is counted
    launchers = re.findall(r"^launchable-activity: name='([^']+)'", text, re.MULTILINE)
    if len(launchers) != 1:
        problems.append(f"Expected exactly one launcher activity, found {len(launchers)}: {launchers}")
    if "application-debuggable" in text:
        problems.append("APK is debuggable")
    print(f"package={name} version={version} permissions={len(permissions)} launchers={len(launchers)}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--expect-version", help="Required versionName, for example 1.0.0")
    parser.add_argument("--expect-cert-sha256", help="Required signer certificate SHA-256, hex without colons")
    parser.add_argument("--cert-out", type=Path, help="Write the signer certificate SHA-256 to this file")
    args = parser.parse_args()

    if not args.apk.is_file():
        print(f"APK not found: {args.apk}")
        return 1
    problems: list[str] = []
    digest = check_signature(args.apk, problems)
    check_badging(args.apk, args.expect_version, problems)

    if digest:
        print(f"signer certificate SHA-256: {digest}")
        if args.expect_cert_sha256 and digest != args.expect_cert_sha256.replace(":", "").lower():
            problems.append("Signer certificate does not match the expected fingerprint")
        if args.cert_out:
            args.cert_out.write_text(digest + "\n", encoding="utf-8")

    for item in problems:
        print(f"FAIL: {item}")
    print(f"verify_release: {len(problems)} problem(s)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
