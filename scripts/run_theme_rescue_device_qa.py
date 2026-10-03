"""Run the opt-in theme screenshot test on one already onboarded device.

Install matching Debug app and androidTest APKs with adb install -r first.
No clearing data, fixtures, AI provider requests or desktop layout seeding.
"""
import argparse
import pathlib
import subprocess
import threading
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    adb = [args.adb, "-s", args.serial]
    package = "com.aistudio.ailua.osnv"
    state = subprocess.check_output(adb + ["get-state"], text=True).strip()
    if state != "device":
        raise SystemExit("Device must be connected and authorized")
    log = args.output / "instrumentation.log"
    process = subprocess.Popen(adb + [
        "shell", "am", "instrument", "-w", "-r", "-e", "class",
        "com.example.P5VThemeRescueSmokeTest",
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
    deadline = time.monotonic() + 180
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
        raise SystemExit("Instrumentation exceeded 180 seconds; inspect " + str(log))
    reader.join(timeout=3)
    result = log.read_text(encoding="utf-8", errors="replace")
    print(result)
    if process.returncode or "OK (1 test)" not in result:
        raise SystemExit("Device smoke did not pass; inspect " + str(log))
    for name in (
        "01-default-home.png", "02-soft-home.png", "05-theme-center.png",
        "09-midnight-glass.png", "10-glass-border-fix.png", "11-default-dark.png",
        "12-life-bento.png", "13-folder-open.png", "14-widget-compact.png",
        "15-widget-resize-restored.png",
    ):
        subprocess.run(adb + ["pull", "/sdcard/Android/data/" + package
            + "/files/qa/P5.V-UI-Theme/" + name, str(args.output / name)], check=True)


if __name__ == "__main__":
    main()
