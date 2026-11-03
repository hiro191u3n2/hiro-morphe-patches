#!/usr/bin/env python3
"""Verify the English-candidate priority data flow in the actual final DEX."""
import argparse
import json
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parent


def run(command, logfile):
    completed = subprocess.run([str(v) for v in command], stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, text=True)
    logfile.write_text(completed.stdout)
    if completed.returncode != 0:
        raise RuntimeError("Command failed: " + str(logfile) + "\n" + completed.stdout)
    return completed.stdout


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--baseline", type=Path, required=True)
    parser.add_argument("--runtime", type=Path, required=True)
    parser.add_argument("--tools", type=Path, required=True)
    parser.add_argument("--work", type=Path, required=True)
    args = parser.parse_args()
    args.work.mkdir(parents=True, exist_ok=False)
    classes = args.work / "classes"
    classes.mkdir()
    run(["java", "com.sun.tools.javac.Main", "-cp", args.tools / "morphe.jar", "-d", classes,
         ROOT / "EnglishPriorityDexTest.java"], args.work / "compile.log")
    report = json.loads(run([os.environ.get("MORPHE_JAVA", "java"), "-cp",
                             str(classes) + os.pathsep + str(args.tools / "morphe.jar"),
                             "EnglishPriorityDexTest", args.baseline, args.runtime], args.work / "test.log"))
    if report["status"] != "PASSED":
        raise RuntimeError("Final English candidate DEX check failed")
    (args.work / "report.json").write_text(json.dumps(report, indent=2, sort_keys=True) + "\n")
    print(json.dumps(report, sort_keys=True))


if __name__ == "__main__":
    main()
