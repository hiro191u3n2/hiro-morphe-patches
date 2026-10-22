#!/usr/bin/env python3
"""Compile actual runtime against host fixtures, then exercise lifecycle/privacy behavior."""
import argparse
import pathlib
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--javac", default="javac")
    parser.add_argument("--java", default="java")
    args = parser.parse_args()
    tests = pathlib.Path(__file__).resolve().parent
    runtime = tests.parent / "runtime"
    sources = sorted(runtime.rglob("*.java")) + sorted(tests.rglob("*.java"))
    with tempfile.TemporaryDirectory(prefix="berry-queue-host-") as classes:
        subprocess.run([args.javac, "--release", "8", "-Xlint:-options", "-d", classes]
                       + [str(source) for source in sources], check=True)
        subprocess.run([args.java, "-cp", classes,
                        "app.hiro.berry.runtime.BerryQueueRuntimeTest"], check=True)


if __name__ == "__main__":
    main()
