#!/usr/bin/env python3
"""Reproduce the Piko Hide feed posts label fix from pinned upstream binaries.

Requires Python 3 and JDK 21 (JAVA_HOME or --java-home). No Gradle, Kotlin,
Android SDK, GitHub token, or unpublished artifacts are required.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parent
UPSTREAM = (
    "https://github.com/crimera/piko/releases/download/v3.11.0-dev.2/patches-3.11.0-dev.2.mpp",
    "c60977417f11a3e9ca71b6afe3236abc8535fe6bf771e5e153fc8a97892171fc",
)
TOOLCHAIN = (
    "https://github.com/MorpheApp/morphe-desktop/releases/download/v1.19.1-dev.2/morphe-desktop-1.19.1-dev.2-all.jar",
    "dde37642d5501e6c188bb0d173c3e142155912318d0bde60fceb496981074c68",
)
ENTRY = "app/crimera/patches/instagram/misc/distractionFree/HideFeedPostsPatchKt.class"
EXPORTS = [
    "--add-exports", "java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED",
    "--add-exports", "java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED",
]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def acquire(path, source):
    url, expected = source
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists():
        print(f"Downloading {path.name}", flush=True)
        req = urllib.request.Request(url, headers={"User-Agent": "Piko-Hiro-Reproducible-Label-Fix/1"})
        temporary = path.with_suffix(path.suffix + ".part")
        with urllib.request.urlopen(req, timeout=120) as response, temporary.open("wb") as target:
            shutil.copyfileobj(response, target)
        temporary.rename(path)
    actual = sha(path)
    if actual != expected:
        raise SystemExit(f"SHA-256 mismatch for {path}: {actual} != {expected}")


def run(argv):
    subprocess.run([str(x) for x in argv], cwd=ROOT, check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java-home", type=Path, default=os.environ.get("JAVA_HOME"))
    parser.add_argument("--output", type=Path, default=ROOT / "out/Piko_3.11.0-dev.2.hiro.1.mpp")
    args = parser.parse_args()
    java = args.java_home / "bin/java" if args.java_home else shutil.which("java")
    javac = args.java_home / "bin/javac" if args.java_home else shutil.which("javac")
    if not java or not javac:
        raise SystemExit("JDK 21 required; set JAVA_HOME or pass --java-home.")
    original = ROOT / "input/piko-original.mpp"
    tools = ROOT / "tools/morphe-desktop.jar"
    out = ROOT / "out"
    out.mkdir(exist_ok=True)
    acquire(original, UPSTREAM)
    acquire(tools, TOOLCHAIN)
    with zipfile.ZipFile(original) as source:
        (ROOT / "input/HideFeedPostsPatchKt.class").write_bytes(source.read(ENTRY))
        (ROOT / "input/classes.dex").write_bytes(source.read("classes.dex"))
    run([javac, *EXPORTS, "-cp", tools, "-d", out,
         ROOT / "src/FixBundle.java", ROOT / "src/AuditBundle.java"])
    run([java, *EXPORTS, "-cp", os.pathsep.join([str(tools), str(out)]), "FixBundle",
         ROOT / "input/HideFeedPostsPatchKt.class", out / "HideFeedPostsPatchKt.class",
         ROOT / "input/classes.dex", out / "classes.dex"])
    run([java, "-Xmx512m", *EXPORTS, "-cp", os.pathsep.join([str(tools), str(out)]), "AuditBundle",
         ROOT / "input/classes.dex", out / "classes.dex",
         ROOT / "input/HideFeedPostsPatchKt.class", out / "HideFeedPostsPatchKt.class"])
    replacements = {
        ENTRY: (out / "HideFeedPostsPatchKt.class").read_bytes(),
        "classes.dex": (out / "classes.dex").read_bytes(),
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(original) as source:
        manifest = source.read("META-INF/MANIFEST.MF")
        if manifest.count(b"Version: 3.11.0-dev.2") != 1:
            raise SystemExit("Unexpected manifest version")
        replacements["META-INF/MANIFEST.MF"] = manifest.replace(
            b"Version: 3.11.0-dev.2", b"Version: 3.11.0-dev.2.hiro.1")
        with zipfile.ZipFile(args.output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as target:
            for entry in source.infolist():
                target.writestr(entry, replacements.get(entry.filename, source.read(entry.filename)))
            for filename in ["NOTICE", "LICENSE"]:
                if filename not in source.namelist():
                    entry = zipfile.ZipInfo(filename, (1980, 2, 1, 0, 0, 0))
                    entry.compress_type = zipfile.ZIP_DEFLATED
                    entry.external_attr = 0o100644 << 16
                    target.writestr(entry, (ROOT / filename).read_bytes())
    with zipfile.ZipFile(original) as before, zipfile.ZipFile(args.output) as after:
        changed = [name for name in before.namelist() if before.read(name) != after.read(name)]
        if set(changed) != set(replacements):
            raise SystemExit(f"Unexpected modified entries: {changed}")
        added = sorted(set(after.namelist()) - set(before.namelist()))
        if added != ["LICENSE", "NOTICE"]:
            raise SystemExit(f"Unexpected added entries: {added}")
    report = {
        "version": "3.11.0-dev.2.hiro.1",
        "upstream_url": UPSTREAM[0], "upstream_sha256": UPSTREAM[1],
        "toolchain_url": TOOLCHAIN[0], "toolchain_sha256": TOOLCHAIN[1],
        "output": args.output.name, "output_sha256": sha(args.output),
        "bytes": args.output.stat().st_size,
        "modified_original_entries": changed, "added_attribution_entries": added,
        "jvm_label_fixes": 1, "dex_label_fixes": 1,
    }
    (out / "build-report.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
