#!/usr/bin/env python3
"""Compile the real runtime against controlled Android doubles and execute branch tests.

Usage: python src/test-runtime/run-tests.py [OUTPUT_DIRECTORY]
JDK 17 is sufficient. No Android device, emulator, original APK, or Gradle is required.
"""
from pathlib import Path
import subprocess
import sys
import tempfile

project = Path(__file__).resolve().parents[2]
output = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path(tempfile.mkdtemp(prefix="oneback-host-"))
output.mkdir(parents=True, exist_ok=True)
sources = sorted((project / "src/test-stubs").rglob("*.java"))
sources += sorted((project / "src/runtime").rglob("*.java"))
sources += sorted((project / "src/test-runtime").glob("*.java"))
subprocess.run(["java", "com.sun.tools.javac.Main", "--release", "8", "-d", str(output),
                *map(str, sources)], check=True)
subprocess.run(["java", "-cp", str(output), "app.hiro.oneback.runtime.OneBackExitHostTest"], check=True)
