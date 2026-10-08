#!/usr/bin/env python3
"""Build the NR-only arm64-v8a JNI backend with the Android NDK."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--ndk", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    ndk = args.ndk.resolve()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    root = Path(__file__).resolve().parent
    source = root / "single_noise1955.c"
    bin_dir = ndk / "toolchains/llvm/prebuilt/linux-x86_64/bin"
    compiler = bin_dir / "aarch64-linux-android26-clang"
    library = output / "libulike_nr1955.so"
    flags = ["-O3", "-std=c11", "-Wall", "-Wextra", "-Werror", "-ffp-contract=off",
             "-fno-fast-math", "-fPIC", "-fvisibility=hidden", "-shared",
             "-Wl,--no-undefined", "-Wl,-z,max-page-size=16384"]
    subprocess.run([str(compiler), *flags, str(source), "-lm", "-o", str(library)], check=True)
    subprocess.run([str(bin_dir / "llvm-strip"), "--strip-unneeded", str(library)], check=True)
    revision = None
    for line in (ndk / "source.properties").read_text().splitlines():
        if line.startswith("Pkg.Revision"):
            revision = line.split("=", 1)[1].strip()
    sources = {p.name: digest(p) for p in (source, Path(__file__).resolve())}
    report = {
        "library": library.name, "abi": "arm64-v8a", "native_abi": 1955,
        "min_sdk": 26, "ndk_revision": revision,
        "sha256": digest(library), "bytes": library.stat().st_size,
        "flags": flags, "sourcesmap": sources, "sources": sources,
        "physical_android_tested": False,
        "numerical_policy": "No fast-math; no fused multiply-add; retained DCT DC",
        "transactional_output": True, "workspace_batch_rows": 64,
        "single_frame_only": True,
    }
    report_path = output / "native1955-build.json"
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(report, ensure_ascii=False))


if __name__ == "__main__":
    main()
