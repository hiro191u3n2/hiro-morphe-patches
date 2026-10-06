#!/usr/bin/env python3
"""Run the shipped Java selection helper against host-only native model fixtures.

This verifies selection policy and fresh reads. It does not execute the patched
DEX visibility hooks, Android View drawing, native camera switching, or hardware.
"""
from pathlib import Path
import argparse
import re
import subprocess


def test(root, work):
    root = Path(root).resolve()
    work = Path(work).resolve()
    folder = work / "ui1930-host"
    classes = folder / "classes"
    classes.mkdir(parents=True)
    sources = sorted((root / "ui-host").rglob("*.java"))
    if not sources:
        raise RuntimeError("UI selection host fixtures are missing")
    compile_run = subprocess.run(
        ["javac", "--release", "8", "-encoding", "UTF-8", "-d", str(classes),
         str(root / "RearLensUi1930.java"), *map(str, sources)],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
    )
    (folder / "compile.log").write_text(compile_run.stdout, encoding="utf-8")
    compile_run.check_returncode()
    host_run = subprocess.run(
        ["java", "-cp", str(classes), "com.hiro.ulike.RearLensUiHost1930"],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
    )
    (work / "host-ui1930.txt").write_text(host_run.stdout, encoding="utf-8")
    host_run.check_returncode()
    match = re.search(r"^HOST_UI1930_ASSERTIONS=(\d+)$", host_run.stdout, re.MULTILINE)
    if match is None:
        raise RuntimeError("Host helper assertions did not report completion")
    print(host_run.stdout, end="")
    return int(match.group(1))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument("--work", type=Path, required=True)
    args = parser.parse_args()
    test(args.root, args.work)
