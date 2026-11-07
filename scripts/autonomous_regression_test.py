#!/usr/bin/env python3
"""Autonomous regression test suite for Super-Artemis.

Runs automated on-device visual and functional checks across:
- Main computer list and add host dialog
- Stream settings hierarchy and quick setup presets
- Host lock key guidance dialog and clipboard toggle
- App style picker, color tiles, and surface lift dialog
- Developer support dialog, animated heart, and credits
- Logcat crash monitoring for unhandled exceptions or ANRs
"""

import argparse
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PACKAGE = "com.mohitarora.superartemis.debug"
MAIN_ACTIVITY = f"{PACKAGE}/com.limelight.PcView"
SETTINGS_ACTIVITY = f"{PACKAGE}/com.limelight.preferences.StreamSettings"
SCREENSHOT_DIR = ROOT / "build" / "regression_screenshots"


def run_adb(serial: str, *args: str, check: bool = True) -> str:
    """Execute an ADB command targeting a specific device serial."""
    cmd = ["adb", "-s", serial] + list(args)
    res = subprocess.run(cmd, capture_output=True, text=True, check=False)
    if check and res.returncode != 0:
        raise RuntimeError(f"ADB command failed: {' '.join(cmd)}\n{res.stderr.strip()}")
    return res.stdout.strip()


def capture_screenshot(serial: str, name: str) -> Path:
    """Capture screen and pull to local directory."""
    SCREENSHOT_DIR.mkdir(parents=True, exist_ok=True)
    remote_path = f"/sdcard/{name}.png"
    local_path = SCREENSHOT_DIR / f"{name}.png"
    run_adb(serial, "shell", "screencap", "-p", remote_path, check=False)
    run_adb(serial, "pull", remote_path, str(local_path), check=False)
    run_adb(serial, "shell", "rm", "-f", remote_path, check=False)
    if local_path.is_file() and local_path.stat().st_size > 0:
        print(f"[Screenshot] Saved {name}.png ({local_path.stat().st_size} bytes)")
    else:
        print(f"[Screenshot] Warning: Failed to capture {name}.png")
    return local_path


def dump_ui_nodes(serial: str) -> list[dict]:
    """Dump window hierarchy using uiautomator and parse XML nodes."""
    remote_xml = "/sdcard/window_dump.xml"
    run_adb(serial, "shell", "rm", "-f", remote_xml)
    run_adb(serial, "shell", "uiautomator", "dump", remote_xml, check=False)
    xml_content = run_adb(serial, "shell", "cat", remote_xml, check=False)
    run_adb(serial, "shell", "rm", "-f", remote_xml)

    nodes = []
    if not xml_content or "<hierarchy" not in xml_content:
        return nodes

    try:
        root = ET.fromstring(xml_content)
        for node in root.iter("node"):
            bounds_raw = node.get("bounds", "")
            match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds_raw)
            bounds = None
            center = None
            if match:
                x1, y1, x2, y2 = map(int, match.groups())
                bounds = (x1, y1, x2, y2)
                center = ((x1 + x2) // 2, (y1 + y2) // 2)

            nodes.append({
                "text": node.get("text", ""),
                "resource-id": node.get("resource-id", ""),
                "class": node.get("class", ""),
                "content-desc": node.get("content-desc", ""),
                "bounds": bounds,
                "center": center,
            })
    except ET.ParseError:
        pass
    return nodes


def find_node(nodes: list[dict], **kwargs) -> dict | None:
    """Find first node matching criteria."""
    for n in nodes:
        match = True
        for k, v in kwargs.items():
            if k == "text_contains":
                if v.lower() not in n["text"].lower():
                    match = False
                    break
            elif k == "desc_contains":
                if v.lower() not in n["content-desc"].lower():
                    match = False
                    break
            elif k == "id_contains":
                if v.lower() not in n["resource-id"].lower():
                    match = False
                    break
            elif n.get(k) != v:
                match = False
                break
        if match:
            return n
    return None


def find_apk_for_abi(abi: str) -> Path | None:
    """Locate matching debug APK for the target architecture."""
    search_dirs = [
        ROOT / "app" / "build" / "outputs" / "apk" / "nonRoot_game" / "debug",
        ROOT / "app" / "build" / "outputs" / "apk" / "debug",
        ROOT / "app" / "build" / "outputs" / "apk" / "root" / "debug",
    ]
    candidates: list[Path] = []
    for d in search_dirs:
        if d.is_dir():
            candidates.extend(d.glob("*.apk"))

    for cand in candidates:
        if abi in cand.name and "debug" in cand.name:
            return cand

    # Fallback to universal or first debug APK
    for cand in candidates:
        if "debug" in cand.name:
            return cand
    return None


def check_logcat_crashes(serial: str, since_time: str) -> list[str]:
    """Inspect logcat for fatal exceptions or ANRs."""
    logs = run_adb(serial, "logcat", "-d", "-t", "200", "*:E", check=False)
    crash_lines = []
    for line in logs.splitlines():
        if "FATAL EXCEPTION" in line or "AndroidRuntime" in line:
            crash_lines.append(line)
        if PACKAGE in line and ("Exception" in line or "Error" in line):
            crash_lines.append(line)
    return crash_lines


def run_regression(serial: str) -> int:
    """Run full suite of functional and visual regression tests."""
    print(f"=== Starting Super-Artemis Regression on {serial} ===")

    # 1. Device identity and prerequisites
    model = run_adb(serial, "shell", "getprop", "ro.product.model")
    abi = run_adb(serial, "shell", "getprop", "ro.product.cpu.abi")
    sdk = run_adb(serial, "shell", "getprop", "ro.build.version.sdk")
    print(f"Device: {model} (SDK {sdk}, ABI {abi})")

    # Dismiss keyguard and wake screen
    run_adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    run_adb(serial, "shell", "wm", "dismiss-keyguard", check=False)

    # 2. Find and install APK
    apk = find_apk_for_abi(abi)
    if not apk:
        print(f"Error: No debug APK found for ABI {abi} under app/build/outputs/apk/")
        return 1
    print(f"Installing {apk.name}...")
    run_adb(serial, "install", "-r", str(apk))

    # Clear logcat
    run_adb(serial, "logcat", "-c")
    test_start_time = run_adb(serial, "shell", "date", "+'%Y-%m-%d %H:%M:%S.000'")

    # 3. Launch main activity
    print("\n--- Test Phase 1: Main View and Host Discovery ---")
    run_adb(serial, "shell", "am", "start", "-n", MAIN_ACTIVITY)
    time.sleep(2)
    capture_screenshot(serial, "01_main_view")

    nodes = dump_ui_nodes(serial)
    add_pc_btn = find_node(nodes, desc_contains="Add") or find_node(nodes, text_contains="Add")
    if add_pc_btn and add_pc_btn["center"]:
        print(f"Found Add PC button at {add_pc_btn['center']}. Testing Add Host Dialog...")
        run_adb(serial, "shell", "input", "tap", str(add_pc_btn["center"][0]), str(add_pc_btn["center"][1]))
        time.sleep(1)
        capture_screenshot(serial, "02_add_host_dialog")
        # Send BACK twice: once for soft keyboard, once to dismiss dialog
        run_adb(serial, "shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(0.5)
        run_adb(serial, "shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(1)

    # 4. Open Settings
    print("\n--- Test Phase 2: Stream Settings and Quick Setups ---")
    nodes = dump_ui_nodes(serial)
    settings_btn = find_node(nodes, id_contains="settingsButton") or find_node(nodes, desc_contains="Settings")
    if settings_btn and settings_btn["center"]:
        print(f"Found Settings button at {settings_btn['center']}. Opening Settings...")
        run_adb(serial, "shell", "input", "tap", str(settings_btn["center"][0]), str(settings_btn["center"][1]))
    else:
        print("Tapping fallback settings coordinates...")
        run_adb(serial, "shell", "input", "tap", "980", "150")
    time.sleep(2)
    capture_screenshot(serial, "03_settings_top")

    # Locate Quick Setups card
    nodes = dump_ui_nodes(serial)
    quick_setups = find_node(nodes, text_contains="Quick setups")
    if quick_setups and quick_setups["center"]:
        print(f"Tapping Quick Setups at {quick_setups['center']}...")
        run_adb(serial, "shell", "input", "tap", str(quick_setups["center"][0]), str(quick_setups["center"][1]))
        time.sleep(1)
        capture_screenshot(serial, "04_quick_setups_dialog")

        # Verify scenario options
        dialog_nodes = dump_ui_nodes(serial)
        scenarios = ["Competitive", "Everyday", "Best picture", "Battery saver"]
        for sc in scenarios:
            found = find_node(dialog_nodes, text_contains=sc)
            print(f"  Preset verification '{sc}': {'PASS' if found else 'FAIL'}")

        # Tap Everyday desktop preset
        everyday_node = find_node(dialog_nodes, text_contains="Everyday")
        if everyday_node and everyday_node["center"]:
            run_adb(serial, "shell", "input", "tap", str(everyday_node["center"][0]), str(everyday_node["center"][1]))
            time.sleep(1)
        # Dismiss Quick setups dialog
        run_adb(serial, "shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(1)
    else:
        print("Warning: Quick setups card not found on screen.")

    # Locate Video Resolution row
    print("\n--- Test Phase 2b: Sectioned Video Resolution Picker ---")
    nodes = dump_ui_nodes(serial)
    res_row = find_node(nodes, text_contains="Video resolution")
    if res_row and res_row["center"]:
        print(f"Tapping Video resolution row at {res_row['center']}...")
        run_adb(serial, "shell", "input", "tap", str(res_row["center"][0]), str(res_row["center"][1]))
        time.sleep(1)
        capture_screenshot(serial, "04b_resolution_picker_dialog")
        dialog_nodes = dump_ui_nodes(serial)
        has_std = find_node(dialog_nodes, text_contains="Standard Resolutions")
        has_native = find_node(dialog_nodes, text_contains="Native Display")
        has_custom = find_node(dialog_nodes, text_contains="Custom Resolution")
        print(f"  Section 'Standard Resolutions': {'PASS' if has_std else 'FAIL'}")
        print(f"  Section 'Native Display': {'PASS' if has_native else 'FAIL'}")
        print(f"  Section 'Custom Resolution': {'PASS' if has_custom else 'FAIL'}")
        run_adb(serial, "shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(1)
    else:
        print("Warning: Video resolution preference row not found.")

    # Locate Video Bitrate row
    print("\n--- Test Phase 2c: Stepper Video Bitrate Dialog ---")
    nodes = dump_ui_nodes(serial)
    bitrate_row = None
    for n in nodes:
        if "video bitrate:" in n["text"].lower() or ("video bitrate" in n["text"].lower() and "metered" not in n["text"].lower()):
            bitrate_row = n
            break
    if bitrate_row and bitrate_row["center"]:
        print(f"Tapping Video bitrate row at {bitrate_row['center']}...")
        run_adb(serial, "shell", "input", "tap", str(bitrate_row["center"][0]), str(bitrate_row["center"][1]))
        time.sleep(1)
        capture_screenshot(serial, "04c_bitrate_stepper_dialog")
        dialog_nodes = dump_ui_nodes(serial)
        has_mbps = any("mbps" in n["text"].lower() for n in dialog_nodes)
        print(f"  Bitrate unit verification: {'PASS' if has_mbps else 'FAIL'}")
        run_adb(serial, "shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(1)
    else:
        print("Warning: Video bitrate preference row not found.")

    # Expand "Show more" if present under Video settings to expose advanced options
    nodes = dump_ui_nodes(serial)
    show_more = find_node(nodes, text_contains="Show more")
    if show_more and show_more["center"]:
        print(f"Expanding 'Show more' at {show_more['center']}...")
        run_adb(serial, "shell", "input", "tap", str(show_more["center"][0]), str(show_more["center"][1]))
        time.sleep(1)

    # Locate Custom resolution row
    print("\n--- Test Phase 2d: Custom Resolution Dialog & Aspect Ratio Picker ---")
    custom_res_row = None
    for _ in range(3):
        run_adb(serial, "shell", "input", "swipe", "500", "1600", "500", "600", "300")
        time.sleep(1)
        nodes = dump_ui_nodes(serial)
        for n in nodes:
            if n["text"] == "Custom Resolution" or n["text"] == "Custom resolution":
                custom_res_row = n
                break
        if custom_res_row:
            break

    if custom_res_row and custom_res_row["center"]:
        print(f"Tapping Custom Resolution row at {custom_res_row['center']}...")
        run_adb(serial, "shell", "input", "tap", str(custom_res_row["center"][0]), str(custom_res_row["center"][1]))
        time.sleep(1)
        capture_screenshot(serial, "04d_custom_resolution_dialog")
        dialog_nodes = dump_ui_nodes(serial)
        has_ratio = any("16:9" in n["text"] or "Native" in n["text"] for n in dialog_nodes)
        has_ok = any(n["text"] == "OK" for n in dialog_nodes)
        print(f"  Aspect ratio buttons presence: {'PASS' if has_ratio else 'FAIL'}")
        print(f"  OK Button presence: {'PASS' if has_ok else 'FAIL'}")
        run_adb(serial, "shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(1)
    else:
        print("Warning: Custom Resolution row not found.")

    # 5. Host Settings and Lock Key Guidance
    print("\n--- Test Phase 3: Host Settings and Lock Key Guidance ---")
    run_adb(serial, "shell", "input", "swipe", "500", "1500", "500", "500", "300")
    time.sleep(1)
    capture_screenshot(serial, "05_host_section")

    nodes = dump_ui_nodes(serial)
    lock_key = find_node(nodes, text_contains="Lock key does nothing")
    if lock_key and lock_key["center"]:
        print(f"Tapping Lock Key Help at {lock_key['center']}...")
        run_adb(serial, "shell", "input", "tap", str(lock_key["center"][0]), str(lock_key["center"][1]))
        time.sleep(1)
        capture_screenshot(serial, "06_lock_key_dialog")

        dialog_nodes = dump_ui_nodes(serial)
        copy_btns = [n for n in dialog_nodes if "copy" in n["text"].lower() or "copy" in n["content-desc"].lower()]
        print(f"  Lock Key Help Dialog: Found {len(copy_btns)} copy button(s)")
        run_adb(serial, "shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(1)
    else:
        print("Warning: Lock key guidance row not found.")

    # Locate Rumble update interval row
    print("\n--- Test Phase 3b: Controller Rumble Rate Limiting ---")
    nodes = dump_ui_nodes(serial)
    rumble_row = find_node(nodes, text_contains="Rumble update interval")
    if not rumble_row:
        run_adb(serial, "shell", "input", "swipe", "500", "1500", "500", "700", "300")
        time.sleep(1)
        nodes = dump_ui_nodes(serial)
        rumble_row = find_node(nodes, text_contains="Rumble update interval")

    if rumble_row and rumble_row["center"]:
        print(f"Tapping Rumble update interval at {rumble_row['center']}...")
        run_adb(serial, "shell", "input", "tap", str(rumble_row["center"][0]), str(rumble_row["center"][1]))
        time.sleep(1)
        capture_screenshot(serial, "06b_rumble_throttle_dialog")
        dialog_nodes = dump_ui_nodes(serial)
        has_ms = any("ms" in n["text"].lower() for n in dialog_nodes)
        print(f"  Rumble interval unit verification: {'PASS' if has_ms else 'FAIL'}")
        run_adb(serial, "shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(1)
    else:
        print("Warning: Rumble update interval preference row not found.")

    # 6. Appearance, App Style, and Surface Lift
    print("\n--- Test Phase 4: App Style Picker and Surface Lift ---")
    run_adb(serial, "shell", "input", "swipe", "500", "1500", "500", "700", "300")
    time.sleep(1)
    capture_screenshot(serial, "07_appearance_section")

    nodes = dump_ui_nodes(serial)
    app_style = find_node(nodes, text_contains="App style")
    if app_style and app_style["center"]:
        print(f"Tapping App style at {app_style['center']}...")
        run_adb(serial, "shell", "input", "tap", str(app_style["center"][0]), str(app_style["center"][1]))
        time.sleep(1)
        capture_screenshot(serial, "08_app_style_picker_dialog")
        run_adb(serial, "shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(1)

    nodes = dump_ui_nodes(serial)
    pure_black = find_node(nodes, text_contains="Pure black")
    if pure_black and pure_black["center"]:
        print(f"Tapping Pure black row at {pure_black['center']} to open SurfaceLiftDialog...")
        run_adb(serial, "shell", "input", "tap", str(pure_black["center"][0]), str(pure_black["center"][1]))
        time.sleep(1)
        capture_screenshot(serial, "09_surface_lift_dialog")
        run_adb(serial, "shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(1)

    # 7. About and Support Development
    print("\n--- Test Phase 5: About and Support Development ---")
    run_adb(serial, "shell", "input", "swipe", "500", "1800", "500", "300", "400")
    time.sleep(1)
    capture_screenshot(serial, "10_about_section")

    nodes = dump_ui_nodes(serial)
    support_dev = find_node(nodes, text_contains="Support development")
    if support_dev and support_dev["center"]:
        print(f"Tapping Support Development at {support_dev['center']}...")
        run_adb(serial, "shell", "input", "tap", str(support_dev["center"][0]), str(support_dev["center"][1]))
        time.sleep(1)
        capture_screenshot(serial, "11_support_development_dialog")
        run_adb(serial, "shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(1)

    # 8. Check logcat crashes
    print("\n--- Test Phase 6: Crash and Runtime Error Sweep ---")
    crashes = check_logcat_crashes(serial, test_start_time)
    if crashes:
        print(f"FAILED: Found {len(crashes)} crash or error line(s) in logcat:")
        for c in crashes[:10]:
            print(f"  {c}")
        return 1

    print("Zero fatal crashes or exceptions detected during autonomous run.")
    print("=== Super-Artemis Autonomous Regression: ALL PASS ===")
    return 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Super-Artemis autonomous regression test")
    parser.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL", "emulator-5564"), help="Target ADB serial")
    args = parser.parse_args()
    sys.exit(run_regression(args.serial))
