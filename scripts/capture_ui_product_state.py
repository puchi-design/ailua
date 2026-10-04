"""Capture or compare the existing phone data without seeding or clearing it.

Snapshots are local QA evidence, never source assets. The app is stopped while
copying its SQLite database and WAL, then reopened. No provider settings are read.
"""
import argparse
import json
import pathlib
import sqlite3
import subprocess
import xml.etree.ElementTree as ET

PACKAGE = "com.aistudio.ailua.osnv"
TABLES = (
    "desktop_page", "desktop_item", "desktop_folder", "workspace_widget_state",
    "chat_session", "chat_turn", "chat_variant", "memory_entry", "memory_extract_cursor",
)
PREFERENCES = (
    "ailua_character_context.xml", "ailua_home_display.xml", "ailua_settings.xml",
    "ailua_control_center.xml", "ailua_virtual_lock.xml",
)


def dump_state(adb, output, label):
    def call(*args):
        return subprocess.check_output(adb + list(args), stderr=subprocess.STDOUT)

    if call("get-state").strip() != b"device":
        raise SystemExit("The selected device is unavailable or unauthorized")
    output.mkdir(parents=True, exist_ok=True)
    call("shell", "am", "force-stop", PACKAGE)
    try:
        db = output / (label + ".db")
        db.write_bytes(call("exec-out", "run-as", PACKAGE, "cat", "databases/ailua_chat.db"))
        files = call("shell", "run-as", PACKAGE, "ls", "databases").decode().split()
        if "ailua_chat.db-wal" in files:
            pathlib.Path(str(db) + "-wal").write_bytes(
                call("exec-out", "run-as", PACKAGE, "cat", "databases/ailua_chat.db-wal"))
        with sqlite3.connect(db) as connection:
            connection.row_factory = sqlite3.Row
            tables = {row[0] for row in connection.execute("SELECT name FROM sqlite_master WHERE type='table'")}
            state = {"schema": connection.execute("PRAGMA user_version").fetchone()[0],
                     "tables": {}, "preferences": {}}
            for table in TABLES:
                if table in tables:
                    state["tables"][table] = [dict(row) for row in connection.execute(
                        'SELECT * FROM "' + table + '" ORDER BY rowid')]
        prefs = call("shell", "run-as", PACKAGE, "ls", "shared_prefs").decode().split()
        for name in PREFERENCES:
            if name not in prefs:
                state["preferences"][name] = None
                continue
            root = ET.fromstring(call("exec-out", "run-as", PACKAGE, "cat", "shared_prefs/" + name))
            state["preferences"][name] = {
                node.get("name"): node.get("value") if node.get("value") is not None else node.text
                for node in root if name != "ailua_settings.xml"
                or node.get("name", "").startswith("theme_engine_")
                or node.get("name") in ("dark_theme", "developer")
            }
        path = output / (label + "-state.json")
        path.write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps({"snapshot": str(path), "schema": state["schema"],
                          "rows": {key: len(rows) for key, rows in state["tables"].items()}}))
    finally:
        call("shell", "am", "start", "-n", PACKAGE + "/com.example.MainActivity")


def compare(output, before, after):
    old = json.loads((output / (before + "-state.json")).read_text(encoding="utf-8"))
    new = json.loads((output / (after + "-state.json")).read_text(encoding="utf-8"))
    checks = {"schema": old["schema"] == new["schema"]}
    for table in TABLES:
        checks[table] = old["tables"].get(table) == new["tables"].get(table)
    for name in PREFERENCES:
        checks[name] = old["preferences"].get(name) == new["preferences"].get(name)
    report = {"before": before, "after": after, "exactChecks": checks,
              "passed": all(checks.values())}
    (output / (after + "-preservation.json")).write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(json.dumps(report, indent=2))
    if not report["passed"]:
        raise SystemExit(1)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True, type=pathlib.Path)
    sub = parser.add_subparsers(dest="action", required=True)
    snapshot = sub.add_parser("capture")
    snapshot.add_argument("--adb", required=True)
    snapshot.add_argument("--serial", required=True)
    snapshot.add_argument("--label", required=True)
    comparison = sub.add_parser("compare")
    comparison.add_argument("--before", default="before")
    comparison.add_argument("--after", default="after")
    args = parser.parse_args()
    if args.action == "capture":
        dump_state([args.adb, "-s", args.serial], args.output, args.label)
    else:
        compare(args.output, args.before, args.after)


if __name__ == "__main__":
    main()
