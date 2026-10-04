"""Observe six actual ADB page gestures outside instrumentation.

Run the theme persistence prepare probe first. This script restarts and unlocks
the current theme, records gfxinfo and a real screenshot, then leaves theme
restoration to the persistence verify probe. No data or theme settings are written.
"""
import argparse
import pathlib
import re
import subprocess
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--theme", required=True, choices=("soft_home", "midnight_glass"))
    parser.add_argument("--output", type=pathlib.Path, required=True)
    args = parser.parse_args()
    adb = [args.adb, "-s", args.serial]
    package = "com.aistudio.ailua.osnv"

    def call(*parts):
        return subprocess.check_output(adb + list(parts))

    size = re.search(rb"(\d+)x(\d+)", call("shell", "wm", "size"))
    width, height = map(int, size.groups())
    call("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    call("shell", "am", "force-stop", package)
    call("shell", "cmd", "statusbar", "collapse")
    call("shell", "am", "start", "-a", "android.intent.action.MAIN", "-c",
         "android.intent.category.LAUNCHER", "-f", "0x10008000", "-n", package + "/com.example.MainActivity")
    focus = ""
    for attempt in range(40):
        window = call("shell", "dumpsys", "window").decode(errors="replace")
        focus = next((line for line in window.splitlines() if "mCurrentFocus=" in line), "")
        if package + "/" in focus:
            break
        if "com.miui.home/" in focus and attempt % 4 == 0:
            call("shell", "am", "start", "-a", "android.intent.action.MAIN", "-c",
                 "android.intent.category.LAUNCHER", "-f", "0x10008000", "-n", package + "/com.example.MainActivity")
        time.sleep(.5)
    if package + "/" not in focus:
        raise SystemExit("AILUA must be in the foreground: " + focus)
    call("shell", "input", "swipe", str(width // 2), str(int(height * .86)),
         str(width // 2), str(int(height * .45)), "550")
    time.sleep(.8)

    def pair():
        for start, end in ((.83, .18), (.18, .83)):
            call("shell", "input", "swipe", str(int(width * start)), str(int(height * .55)),
                 str(int(width * end)), str(int(height * .55)), "350")
            time.sleep(.4)

    pair()
    call("shell", "dumpsys", "gfxinfo", package, "reset")
    started = time.monotonic()
    for _ in range(3):
        pair()
    metrics = call("shell", "dumpsys", "gfxinfo", package).decode(errors="replace")
    rendered = re.search(r"Total frames rendered:\s*(\d+)", metrics)
    if rendered is None or int(rendered[1]) < 30:
        raise SystemExit("Too few rendered frames; do not report a locked or inactive screen as page performance")
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output / ("performance-" + args.theme + "-live-adb.txt")).write_text(
        args.theme + "; actual restart/unlock and six physical page gestures outside instrumentation.\n"
        + "Elapsed seconds: " + str(round(time.monotonic() - started, 3))
        + "\nDebug APK on API29; observation, not a release benchmark.\n" + metrics, encoding="utf-8")
    (args.output / ("live-home-" + args.theme + ".png")).write_bytes(call("exec-out", "screencap", "-p"))
    print("Recorded actual foreground Home gestures:", args.theme)


if __name__ == "__main__":
    main()
