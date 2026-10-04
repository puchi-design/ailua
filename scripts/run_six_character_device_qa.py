"""Run opt-in six-character QA on one connected, already onboarded Android phone.

Install matching Debug and androidTest APKs with install -r beforehand. This
runner never installs/clears/seeds data, contacts providers, starts a real call,
or deletes any session to force a preservation result. Private row snapshots
stay under output/private; the public result contains checks/counts only.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import re
import shlex
import struct
import subprocess
import threading
import time
import uuid
import xml.etree.ElementTree as ET
from datetime import datetime, timezone

import capture_ui_product_state as state_capture


PACKAGE = "com.aistudio.ailua.osnv"
TEST_CLASS = "com.example.P5VSixCharacterSmokeTest"
SMOKE_METHOD = "avatarsRosterRealScreensAndPreservedUserState"
RECOVERY_METHOD = "recoverInterruptedQaSelection"
DEVICE_DIRECTORY = f"/sdcard/Android/data/{PACKAGE}/files/qa/P5.V-SixCharacters"
OFFICIAL_IDS = {"hewenchuan", "zhoujianye", "peixubai", "mira", "yuna", "noa"}
GROUP_IDENTITY = "__ailua_group__:rain_tea:mira,yuna,noa"
OS_KEYS = {
    "custom_character_cards", "call_history_json", "bookmarked_message_ids",
    "home_app_order", "relationships_json", "romance_evidence_v1_json", "theater_bookmark",
}
CREATE_NO_WINDOW = getattr(subprocess, "CREATE_NO_WINDOW", 0)


def write_json(path: pathlib.Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def command(adb: list[str], *args: str, timeout: int = 45) -> bytes:
    return subprocess.check_output(adb + list(args), stderr=subprocess.STDOUT, timeout=timeout,
                                   creationflags=CREATE_NO_WINDOW)


def shell(adb: list[str], *args: str, timeout: int = 45) -> bytes:
    # adb shell eventually invokes the Android shell; protect even arbitrary
    # imported-character IDs passed as instrumentation arguments.
    return command(adb, "shell", shlex.join(args), timeout=timeout)


def foreground(adb: list[str]) -> None:
    shell(adb, "input", "keyevent", "KEYCODE_WAKEUP")
    shell(adb, "cmd", "statusbar", "collapse")
    shell(adb, "am", "start", "-a", "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER",
          "-f", "0x10008000", "-n", PACKAGE + "/com.example.MainActivity")


def instrumentation(adb: list[str], method: str, run_id: str, log: pathlib.Path, timeout: int,
                    expected_selection: str | None = None) -> dict:
    foreground(adb)
    trust = shell(adb, "dumpsys", "trust").decode("utf-8", errors="replace")
    current_user = next((line for line in trust.splitlines() if "(current):" in line), "")
    if "deviceLocked=1" in current_user:
        raise ValueError("Android credential lock is active; unlock the phone before UI instrumentation")
    parts = ["am", "instrument", "-w", "-r", "-e", "class", TEST_CLASS + "#" + method,
             "-e", "qa_run_id", run_id]
    if expected_selection:
        parts += ["-e", "expectedCharacterId", expected_selection]
    parts.append(PACKAGE + ".test/androidx.test.runner.AndroidJUnitRunner")
    process = subprocess.Popen(adb + ["shell", shlex.join(parts)], stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, creationflags=CREATE_NO_WINDOW)
    starts: list[bool] = []
    log.parent.mkdir(parents=True, exist_ok=True)

    def collect() -> None:
        with log.open("wb") as output:
            assert process.stdout is not None
            for line in iter(process.stdout.readline, b""):
                output.write(line)
                output.flush()
                if b"INSTRUMENTATION_STATUS_CODE: 1" in line:
                    starts.append(True)

    reader = threading.Thread(target=collect, daemon=True)
    reader.start()
    assisted: set[int] = set()
    deadline = time.monotonic() + timeout
    timed_out = False
    try:
        while process.poll() is None:
            if time.monotonic() >= deadline:
                timed_out = True
                # Killing only the local adb client can leave the test running.
                # Stop both processes before this run's separate recovery probe.
                for package in (PACKAGE + ".test", PACKAGE):
                    try:
                        shell(adb, "am", "force-stop", package)
                    except (subprocess.SubprocessError, OSError):
                        pass
                process.terminate()
                break
            index = len(starts)
            if index and index not in assisted:
                windows = shell(adb, "dumpsys", "window").decode("utf-8", errors="replace")
                focus = next((line for line in windows.splitlines() if "mCurrentFocus=" in line), "")
                if "com.miui.home/" in focus:
                    foreground(adb)
                    assisted.add(index)
                elif PACKAGE + "/" in focus:
                    assisted.add(index)
            time.sleep(1)  # Host foreground monitoring; image readiness is asserted by Coil success semantics.
    finally:
        if process.poll() is None:
            process.terminate()
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)
        reader.join(timeout=5)
    result = log.read_text(encoding="utf-8", errors="replace")
    return {"passed": not timed_out and process.returncode == 0 and bool(re.search(r"OK \(1 test\)", result)),
            "timedOut": timed_out, "returnCode": process.returncode, "log": str(log),
            "assumptionSkipped": "INSTRUMENTATION_STATUS_CODE: -3" in result or "INSTRUMENTATION_STATUS_CODE: -4" in result,
            "miuiForegroundAssists": sorted(assisted)}


def preference_snapshot(adb: list[str], name: str, keys: set[str] | None = None) -> dict | None:
    names = shell(adb, "run-as", PACKAGE, "ls", "shared_prefs").decode().split()
    if name not in names:
        return None
    root = ET.fromstring(command(adb, "exec-out", "run-as", PACKAGE, "cat", "shared_prefs/" + name))
    result = {}
    for node in root:
        key = node.get("name")
        if keys is not None and key not in keys:
            continue
        result[key] = sorted(child.text or "" for child in node) if node.tag == "set" else (
            node.get("value") if node.get("value") is not None else node.text)
    return result


def snapshot(adb: list[str], private: pathlib.Path, label: str) -> dict:
    state_capture.dump_state(adb, private, label)
    path = private / (label + "-state.json")
    data = json.loads(path.read_text(encoding="utf-8"))
    data["preferences"]["ailua_first_session.xml"] = preference_snapshot(adb, "ailua_first_session.xml")
    data["preferences"]["ailua_os_store.protected"] = preference_snapshot(adb, "ailua_os_store.xml", OS_KEYS)
    write_json(path, data)
    return data


def compare_preserved(before: dict, after: dict) -> dict:
    exact = {"schema": before["schema"] == after["schema"]}
    for table in state_capture.TABLES:
        exact[table] = before["tables"].get(table) == after["tables"].get(table)
    for name in (*state_capture.PREFERENCES, "ailua_first_session.xml", "ailua_os_store.protected"):
        exact[name] = before["preferences"].get(name) == after["preferences"].get(name)
    old_sessions = {row["id"]: row for row in before["tables"].get("chat_session", [])}
    new_sessions = {row["id"]: row for row in after["tables"].get("chat_session", [])}
    preserved = all(new_sessions.get(key) == value for key, value in old_sessions.items())
    added = [value for key, value in new_sessions.items() if key not in old_sessions]
    turn_sessions = {row["session_id"] for row in after["tables"].get("chat_turn", [])}
    acceptable = all(row["id"] not in turn_sessions and
                     (row["character_id"] in OFFICIAL_IDS or row["character_id"] == GROUP_IDENTITY) for row in added)
    remaining_checks = all(value for key, value in exact.items() if key != "chat_session")
    return {"passed": remaining_checks and preserved and acceptable, "exactChecks": exact,
            "allExact": all(exact.values()), "allOriginalSessionRowsPreserved": preserved,
            "newSessionsAreExpectedAndEmpty": acceptable,
            "newEmptySessions": [{"characterId": row["character_id"], "hasTurns": row["id"] in turn_sessions} for row in added],
            "beforeCounts": {table: len(rows) for table, rows in before["tables"].items()},
            "afterCounts": {table: len(rows) for table, rows in after["tables"].items()},
            "policy": "Original rows and history stay exact. Existing UI may create empty official/group sessions; additions are disclosed and never deleted by QA."}


def pull(adb: list[str], name: str, output: pathlib.Path, required: bool = True) -> pathlib.Path | None:
    if pathlib.Path(name).name != name or not re.fullmatch(r"[A-Za-z0-9_.-]+", name):
        raise ValueError("Unsafe QA artifact filename")
    destination = output / name
    try:
        command(adb, "pull", DEVICE_DIRECTORY + "/" + name, str(destination))
    except subprocess.SubprocessError:
        if required:
            raise
        return None
    return destination


def pending_backup(adb: list[str]) -> dict | None:
    available = shell(adb, "sh", "-c", f"if [ -f {shlex.quote(DEVICE_DIRECTORY + '/qa-restore.json')} ]; then echo yes; fi").strip()
    return json.loads(shell(adb, "cat", DEVICE_DIRECTORY + "/qa-restore.json")) if available == b"yes" else None


def recovery(adb: list[str], output: pathlib.Path, run_id: str) -> dict:
    backup = pending_backup(adb)
    if backup is None:
        return {"needed": False, "passed": True}
    if backup.get("qaRunId") != run_id:
        return {"needed": True, "passed": False, "reason": "Backup belongs to another run; no selection was overwritten",
                "recordedRunId": backup.get("qaRunId")}
    result = instrumentation(adb, RECOVERY_METHOD, run_id, output / "recovery-instrumentation.log", 120)
    artifact = pull(adb, "qa-recovery-result.json", output, required=False)
    data = json.loads(artifact.read_text(encoding="utf-8")) if artifact else {}
    return {"needed": True, "passed": result["passed"] and data.get("qaRunId") == run_id and data.get("restored") is True,
            "instrumentation": result, "result": data}


def screenshot_metadata(path: pathlib.Path) -> dict:
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        raise ValueError(f"Invalid PNG screenshot: {path.name}")
    width, height = struct.unpack(">II", data[16:24])
    return {"file": path.name, "width": width, "height": height, "sha256": hashlib.sha256(data).hexdigest(), "bytes": len(data)}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--output", required=True, type=pathlib.Path)
    parser.add_argument("--timeout", type=int, default=420)
    parser.add_argument("--expected-selection")
    parser.add_argument("--recover-run-id", help="Recover only this exact interrupted QA run, without running smoke")
    args = parser.parse_args()
    if not pathlib.Path(args.adb).is_file():
        parser.error("Use the known absolute adb.exe path; no installation search is performed")
    args.output.mkdir(parents=True, exist_ok=True)
    adb = [args.adb, "-s", args.serial]
    if command(adb, "get-state").strip() != b"device":
        raise SystemExit("The selected device is unavailable or unauthorized")
    if args.recover_run_id:
        result = recovery(adb, args.output, args.recover_run_id)
        write_json(args.output / "recovery-summary.json", result)
        raise SystemExit(0 if result["passed"] else 1)
    leftover = pending_backup(adb)
    if leftover:
        raise SystemExit("Previous QA recovery backup exists for run " + str(leftover.get("qaRunId")) +
                         "; use --recover-run-id with that exact run before another smoke")
    run_id = uuid.uuid4().hex
    private = args.output / "private" / run_id
    before = snapshot(adb, private, "before")
    expected_selection = args.expected_selection or (before["preferences"].get("ailua_character_context.xml") or {}).get("selected_id")
    device = {"serial": args.serial, "model": shell(adb, "getprop", "ro.product.model").decode().strip(),
              "manufacturer": shell(adb, "getprop", "ro.product.manufacturer").decode().strip(),
              "android": shell(adb, "getprop", "ro.build.version.release").decode().strip(),
              "api": shell(adb, "getprop", "ro.build.version.sdk").decode().strip()}
    result = {"qaRunId": run_id, "startedAt": datetime.now(timezone.utc).isoformat(timespec="seconds"), "device": device}
    write_json(args.output / "six-character-host-result.json", result)
    smoke = {"passed": False}
    restoration = {"passed": False}
    test_result = {}
    errors = []
    try:
        smoke = instrumentation(adb, SMOKE_METHOD, run_id, args.output / "six-character-instrumentation.log", args.timeout, expected_selection)
        artifact = pull(adb, "six-character-result.json", args.output, required=False)
        if artifact:
            test_result = json.loads(artifact.read_text(encoding="utf-8"))
            if test_result.get("qaRunId") != run_id:
                errors.append("Device result is from a previous run; it cannot prove this test")
                test_result = {}
        if not test_result:
            errors.append("No current-run device result; a skipped/aborted test is not a pass")
        screenshots = []
        for entry in test_result.get("captures", []):
            image = pull(adb, entry["file"], args.output)
            metadata = screenshot_metadata(image)
            if (metadata["width"], metadata["height"]) != (entry["width"], entry["height"]):
                raise ValueError("Screenshot dimensions do not match the device capture record")
            screenshots.append({**metadata, "routeKind": entry["routeKind"], "stableId": entry.get("stableId")})
        result["screenshots"] = screenshots
        for filename in ("asset-main-circle-native.png", "asset-alt-circle-native.png", "asset-device-decode.json", "six-character-events.txt"):
            artifact = pull(adb, filename, args.output, required=bool(test_result))
            if artifact and filename.endswith(".png"):
                result.setdefault("nativeCircleBoards", []).append(screenshot_metadata(artifact))
    except (OSError, ValueError, subprocess.SubprocessError) as error:
        errors.append(f"{type(error).__name__}: {error}")
    finally:
        try:
            restoration = recovery(adb, args.output, run_id)
        except (OSError, ValueError, subprocess.SubprocessError) as error:
            errors.append(f"QA recovery unavailable: {type(error).__name__}: {error}")
        try:
            after = snapshot(adb, private, "after")
            preservation = compare_preserved(before, after)
            write_json(args.output / "six-character-preservation.json", preservation)
            result["preservation"] = preservation
        except (OSError, ValueError, subprocess.SubprocessError) as error:
            errors.append(f"After-state capture unavailable: {type(error).__name__}: {error}")
            result["preservation"] = {"passed": False}
        result.update({"instrumentation": smoke, "recovery": restoration,
                       "deviceResult": test_result, "errors": errors,
                       "passed": smoke.get("passed") is True and not smoke.get("assumptionSkipped") and
                       test_result.get("instrumentationReachedEnd") is True and
                       all(test_result.get("protectedStateChecks", {"missing": False}).values()) and
                       restoration.get("passed") is True and result["preservation"]["passed"] is True and not errors,
                       "fixtures": "Onboarding/Call screenshots are isolated presentation fixtures, not a new-user persistence or live call-engine run",
                       "privacy": "Full private rows/preferences are local under output/private and must not be committed or copied into public report"})
        write_json(args.output / "six-character-host-result.json", result)
    print(json.dumps({"passed": result["passed"], "device": device, "runId": run_id,
                      "screenshots": len(result.get("screenshots", [])), "originalDataPreserved": result["preservation"]["passed"],
                      "newEmptySessions": result["preservation"].get("newEmptySessions", []), "errors": errors}, ensure_ascii=False))
    raise SystemExit(0 if result["passed"] else 1)


if __name__ == "__main__":
    main()
