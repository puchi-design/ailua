"""Run the opt-in theme screenshot test on one already onboarded device.

Install matching Debug app and androidTest APKs with adb install -r first.
No clearing data, fixtures, AI provider requests or desktop layout seeding.
"""
import argparse
import pathlib
import re
import subprocess
import threading
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    parser.add_argument("--suite", choices=("icons", "themes", "product", "persistence"), default="icons")
    parser.add_argument("--method", choices=("workspaceGestureAndRestore", "captureLoadedThemePreview", "prepareForColdStart", "verifyAfterColdStart"))
    parser.add_argument("--theme", choices=("soft_home", "midnight_glass"), default="soft_home")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    adb = [args.adb, "-s", args.serial]
    package = "com.aistudio.ailua.osnv"
    suites = {
        "icons": ("P5VProductionIconsSmokeTest", "P5.V-UIR3", (
            "01-default-icons.png", "02-soft-icons.png", "03-midnight-icons.png",
            "04-y2k-prototype.png", "05-y2k-icon-picker.png",
            "01-default-icons-folder.png", "02-soft-icons-folder.png",
            "03-midnight-icons-folder.png", "04-y2k-prototype-folder.png",
        )),
        "themes": ("P5VProductionThemesSmokeTest", "P5.V-UIProduct", tuple(
            name + suffix + ".png"
            for name in ("01-default", "02-soft", "03-rainy", "04-sakura", "05-y2k", "06-midnight")
            for suffix in ("", "-folder")
        )),
        "product": ("P5VUiProductSmokeTest", "P5.V-UIProduct", tuple(
            name + ".png" for name in (
                "07-theme-center", "08-theme-detail", "09-wallpaper-center", "10-icon-center",
                "11-my-theme", "12-theme-lab", "13-app-library", "14-lockscreen",
                "15-notification", "16-control-center", "17-chat", "18-living",
            )
        )),
        "persistence": ("P5VThemePersistenceSmokeTest", "P5.V-UIProduct", ()),
    }
    class_name, device_directory, screenshots = suites[args.suite]
    if args.suite == "persistence" and args.method not in ("prepareForColdStart", "verifyAfterColdStart"):
        parser.error("Persistence requires one method, with host force-stop between prepare and verify")
    if args.method and args.suite not in ("product", "persistence"):
        parser.error("A method is only supported for product or persistence probes")
    if args.method:
        class_name += "#" + args.method
        screenshots = ("07-theme-center.png", "08-theme-detail.png") if args.method == "captureLoadedThemePreview" else ()
    state = subprocess.check_output(adb + ["get-state"], text=True).strip()
    if state != "device":
        raise SystemExit("Device must be connected and authorized")
    # Start the authorized target explicitly before ActivityScenario. A previous browser or
    # system notification panel otherwise leaves MIUI automation waiting in the background.
    subprocess.run(adb + ["shell", "cmd", "statusbar", "collapse"],
                   check=True, stdout=subprocess.DEVNULL)
    subprocess.run(adb + ["shell", "am", "start", "-a", "android.intent.action.MAIN",
        "-c", "android.intent.category.LAUNCHER", "-f", "0x10008000", "-n",
        package + "/com.example.MainActivity"], check=True, stdout=subprocess.DEVNULL)
    log = args.output / (args.method + ".log" if args.method else args.suite + "-instrumentation.log")
    process = subprocess.Popen(adb + [
        "shell", "am", "instrument", "-w", "-r", "-e", "class",
        "com.example." + class_name,
        "-e", "qa_theme", args.theme,
        package + ".test/androidx.test.runner.AndroidJUnitRunner",
    ], stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
       creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
    starts = []

    def collect():
        with log.open("wb") as output:
            for line in iter(process.stdout.readline, b""):
                output.write(line)
                output.flush()
                if b"INSTRUMENTATION_STATUS_CODE: 1" in line:
                    starts.append(True)

    reader = threading.Thread(target=collect, daemon=True)
    reader.start()
    assisted = set()
    deadline = time.monotonic() + 300
    while process.poll() is None and time.monotonic() < deadline:
        index = len(starts)
        if index and index not in assisted:
            windows = subprocess.check_output(adb + ["shell", "dumpsys", "window"], text=True)
            focus = next((line for line in windows.splitlines() if "mCurrentFocus=" in line), "")
            # Some MIUI ActivityScenario launches return to the system launcher.
            if "com.miui.home/" in focus:
                subprocess.run(adb + ["shell", "am", "start", "-a", "android.intent.action.MAIN",
                    "-c", "android.intent.category.LAUNCHER", "-f", "0x10008000", "-n",
                    package + "/com.example.MainActivity"], check=True, stdout=subprocess.DEVNULL)
                assisted.add(index)
            elif package + "/" in focus:
                assisted.add(index)
        time.sleep(1)
    if process.poll() is None:
        process.terminate()
        reader.join(timeout=3)
        raise SystemExit("Instrumentation exceeded 300 seconds; inspect " + str(log))
    reader.join(timeout=3)
    result = log.read_text(encoding="utf-8", errors="replace")
    print(result)
    if process.returncode or not re.search(r"OK \(\d+ tests?\)", result):
        raise SystemExit("Device smoke did not pass; inspect " + str(log))
    for name in screenshots:
        subprocess.run(adb + ["pull", "/sdcard/Android/data/" + package
            + "/files/qa/" + device_directory + "/" + name, str(args.output / name)], check=True)
    if args.suite == "product" and not args.method:
        extra = ("product-smoke-events.txt", "theme-asset-inspector.png", "drag-icon-moved.png") + tuple(
            "wallpaper-oil_" + stem + ".png" for stem in (
                "rose_garden", "lake_wildflowers", "pink_bloom", "woodland_path", "waterlilies", "garden_still_life"))
        for name in extra:
            subprocess.run(adb + ["pull", "/sdcard/Android/data/" + package
                + "/files/qa/" + device_directory + "/" + name, str(args.output / name)], check=True)
    if args.suite == "themes":
        for name in ("performance-soft_home.txt", "performance-midnight_glass.txt"):
            subprocess.run(adb + ["pull", "/sdcard/Android/data/" + package
                + "/files/qa/" + device_directory + "/" + name, str(args.output / name)], check=True)
    if args.method == "verifyAfterColdStart":
        subprocess.run(adb + ["pull", "/sdcard/Android/data/" + package
                + "/files/qa/" + device_directory + "/cold-start-" + args.theme + "-persistence.txt",
            str(args.output / ("cold-start-" + args.theme + "-persistence.txt"))], check=True)


if __name__ == "__main__":
    main()
