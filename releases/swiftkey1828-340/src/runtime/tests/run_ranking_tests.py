#!/usr/bin/env python3
"""Run the same candidate-priority scenario against baseline and fixed Java."""
import argparse
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parent.parent


def run(command, logfile):
    completed = subprocess.run([str(v) for v in command], stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, text=True)
    logfile.write_text(completed.stdout)
    if completed.returncode != 0:
        raise RuntimeError("Command failed: " + str(logfile) + "\n" + completed.stdout)
    return completed.stdout


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--work", type=Path, required=True)
    args = parser.parse_args()
    args.work.mkdir(parents=True, exist_ok=False)
    report = {"status": "PASSED", "device_tested": False}
    for mode, source in [("baseline", "baseline"), ("fixed", "src")]:
        classes = args.work / mode
        classes.mkdir()
        run(["java", "com.sun.tools.javac.Main", "--release", "8", "-encoding", "UTF-8",
             "-d", classes, *sorted((ROOT / "tests/ranking_stubs").rglob("*.java")),
             ROOT / source / "hiro/swiftkey/WordCandidates.java",
             ROOT / "tests/WordPriorityTest.java"], args.work / (mode + "-compile.log"))
        result = json.loads(run(["java", "-cp", classes, "hiro.swiftkey.WordPriorityTest", mode],
                                args.work / (mode + ".log")))
        if result["status"] != "PASSED":
            raise RuntimeError("Candidate priority test failed")
        report[mode] = result
    (args.work / "report.json").write_text(json.dumps(report, indent=2, sort_keys=True) + "\n")
    print(json.dumps(report, sort_keys=True))


if __name__ == "__main__":
    main()
