#!/usr/bin/env python3
"""Build a resource/scheduling ULike .122 overlay on the pinned public .121 MPP.

No native rebuild is required: all native assets and their installer metadata
are copied byte-for-byte. This script records build/structural facts, not an
Android execution or device-performance result.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import struct
import zipfile
import zlib

import build1945 as common
import build19101 as previous

ROOT = Path(__file__).resolve().parent
VERSION = "1.9.122"
SINGLE = "ULike_HQ_Texture_Online_v1.9.122.mpp"
BASE_SHA256 = "cfa0963fb2ec3e5207cff34b3a0d953a300ad5f69a64413e6e3b16d7e6b31fca"
BASE_BYTES = 1618461
LOADER = "app/hiro/ulike/patches/UlikeHqMaxPatch.class"
INSTALLER = "app/hiro/ulike/patches/IntegrationPayload186.class"
CHANGED = {"META-INF/MANIFEST.MF", "classes.dex", "ulike/runtime.dex", LOADER}
DESCRIPTION = ("ULike .122: bounded capture/save resource scheduling, idle post-save pool cleanup "
               "and measured UI recovery diagnostics. Diagnostic comparisons are explicitly "
               "separate from normal quality-preserving operation. Pixel algorithms, native "
               "assets, output resolution and save formats remain unchanged in normal mode. "
               "Physical device performance unverified.")
require, sha, run = common.require, common.sha, common.run


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode()


def read_families(path):
    text = path.read_text()
    value = json.loads(text) if text.lstrip().startswith(("[", "{")) else text.splitlines()
    if isinstance(value, dict):
        value = value["production_helper_roots"]
    value = [name.strip() for name in value if name.strip() and not name.strip().startswith("#")]
    require(value and len(value) == len(set(value)), "Distinct nonempty changed-family list required")
    require(all(re.fullmatch(r"[A-Za-z][A-Za-z0-9_]*", name) for name in value), "Invalid Java family")
    require({"ProcessingTiming1947", "CameraTrace1965", "VerificationJournal19101"} <= set(value),
            "All diagnostic producers must be rebuilt with the new release version")
    return sorted(value)


def dex_integrity(data, name):
    require(len(data) >= 112 and data[:4] == b"dex\n", "Invalid DEX " + name)
    require(struct.unpack_from("<I", data, 32)[0] == len(data), "DEX size " + name)
    require(data[12:32] == hashlib.sha1(data[32:]).digest(), "DEX SHA1 " + name)
    require(struct.unpack_from("<I", data, 8)[0] == zlib.adler32(data[12:]) & 0xffffffff,
            "DEX checksum " + name)


def build(args):
    os.environ["PATH"] = str(args.jdk / "bin") + os.pathsep + os.environ.get("PATH", "")
    identity = common.verify_jdk()
    families = read_families(args.families)
    require(args.baseline.stat().st_size == BASE_BYTES and sha(args.baseline.read_bytes()) == BASE_SHA256,
            "Only the exact public ULike .121 standalone is an accepted baseline")
    for name, digest in common.TOOL_PINS.items():
        require(sha((args.tools / name).read_bytes()) == digest, "Pinned tool differs: " + name)
    base = common.archive(args.baseline)
    require(common.headers(base["META-INF/MANIFEST.MF"])["Version"] == "1.9.121", "Baseline version")
    require(not args.work.exists(), "Use a fresh build-work directory")
    args.work.mkdir(parents=True)
    args.output.mkdir(parents=True, exist_ok=True)
    baseline = args.work / "baseline"
    for name, data in base.items():
        path = baseline / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)

    sources = [previous.production_path(name) for name in families]
    stubmap = previous.compile_inputs(families)
    all_sources = list(dict.fromkeys([*stubmap.values(), *sources]))
    input_pins = {str(path.relative_to(ROOT)): sha(path.read_bytes()) for path in all_sources}
    for name in ("ProcessingTiming1947", "CameraTrace1965", "VerificationJournal19101"):
        content = (ROOT / (name + ".java")).read_text()
        require(re.search(r'\bVERSION\s*=\s*(?:"1\.9\.122"|ProcessingTiming1947\.VERSION)', content) is not None,
                "Diagnostic source version: " + name)
    classes, helpers, dex = [args.work / name for name in ("classes", "helper-classes", "helper-dex")]
    for path in (classes, helpers, dex):
        path.mkdir()
    run(["javac", "-source", "8", "-target", "8", "-encoding", "UTF-8", "-bootclasspath",
         args.tools / "android.jar", "-d", helpers, *all_sources], args.work / "javac.log")
    selected = sorted(path for path in (helpers / "com/hiro/ulike").glob("*.class")
                      if any(path.name == name + ".class" or path.name.startswith(name + "$")
                             for name in families))
    require(all(helpers / "com/hiro/ulike" / (name + ".class") in selected for name in families),
            "Every changed root must compile")
    run(["java", "-cp", args.tools / "d8.jar", "com.android.tools.r8.D8", "--release", "--min-api", "26",
         "--lib", args.tools / "android.jar", "--classpath", helpers, "--output", dex, *selected],
        args.work / "d8.log")
    cp = os.pathsep.join(map(str, (args.tools / "morphe.jar", baseline, classes)))
    transform_sources = [ROOT / (name + ".java") for name in
                         ("MergePayloads", "Transform19122", "TimingCameraHooks1980",
                          "VerifyRuntimeVersion19101", "VerifyAllHelperReferences1980")]
    run(["javac", "-encoding", "UTF-8", "-cp", cp, "-d", classes, *transform_sources],
        args.work / "transform-javac.log")
    asm = ["--add-exports", "java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED",
           "--add-exports", "java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED"]
    run(["javac", "-encoding", "UTF-8", *asm, "-cp", cp, "-d", classes, ROOT / "PatchClass19122.java"],
        args.work / "metadata-javac.log")
    for name in ("emitted", "repeat"):
        run(["java", "-Dulike.production=" + ",".join(families), "-cp", cp, "Transform19122",
             baseline, dex / "classes.dex", args.work / name], args.work / (name + ".log"))
    emitted = args.work / "emitted"
    require({p.name: p.read_bytes() for p in emitted.iterdir()} ==
            {p.name: p.read_bytes() for p in (args.work / "repeat").iterdir()}, "Nondeterministic DEX overlay")
    for name in ("UlikeHqMaxPatch.class", "UlikeHqMaxPatch-repeat.class"):
        run(["java", *asm, "-cp", cp, "PatchClass19122", baseline / LOADER, args.work / name],
            args.work / (name + ".log"))
    require((args.work / "UlikeHqMaxPatch.class").read_bytes() ==
            (args.work / "UlikeHqMaxPatch-repeat.class").read_bytes(), "Nondeterministic JVM metadata")
    reference_result = run(["java", "-cp", cp, "VerifyAllHelperReferences1980", baseline / "ulike/methods.dex",
                            baseline / "ulike/runtime.dex", baseline / "ulike/methods.dex", emitted / "runtime.dex"],
                           args.work / "helper-references.txt")
    require(reference_result.startswith("PASS helper references"), "Unresolved helper references")
    final = dict(base)
    final.update({"ulike/runtime.dex": (emitted / "runtime.dex").read_bytes(),
                  "classes.dex": (emitted / "loader.dex").read_bytes(),
                  LOADER: (args.work / "UlikeHqMaxPatch.class").read_bytes()})
    manifest = common.headers(final["META-INF/MANIFEST.MF"])
    manifest.update(Version=VERSION, Description=DESCRIPTION)
    final["META-INF/MANIFEST.MF"] = common.manifest(manifest)
    changed = {name for name in base if base[name] != final.get(name)}
    require(set(base) == set(final) and changed == CHANGED, "Unexpected archive delta: " + repr(changed))
    native = {name: sha(data) for name, data in base.items()
              if name.endswith(".so") or name == "ulike186/runtime/0000.bin"}
    require(len(native) == 14 and all(final[name] == base[name] for name in native),
            "All fourteen native payloads must remain byte-identical")
    require(final[INSTALLER] == base[INSTALLER], "JVM installer changed")
    for name in ("ulike/methods.dex", "ulike/methods.tsv"):
        require(final[name] == base[name], "Stock method payload changed")
    for name in ("classes.dex", "ulike/runtime.dex", "ulike/methods.dex"):
        dex_integrity(final[name], name)
    common.write_zip(args.output / SINGLE, final)
    common.write_zip(args.work / "repeat.mpp", final)
    require((args.output / SINGLE).read_bytes() == (args.work / "repeat.mpp").read_bytes(), "MPP nondeterminism")
    listing = run(["java", "-jar", args.tools / "morphe.jar", "list-patches", "--patches", args.output / SINGLE],
                  args.work / "patch-list.log")
    require("高画質撮影・質感美肌・素材通信を復旧" in listing and "v1.9.122" in listing,
            "Morphe could not load the .122 ULike patch")
    require(input_pins == {str(p.relative_to(ROOT)): sha(p.read_bytes()) for p in all_sources},
            "Compiler inputs changed while building")
    artifact = args.output / SINGLE
    report = {
        "schema": "ulike19122-build-evidence-v1", "version": VERSION,
        "status": "BUILD_AND_STRUCTURE_VERIFIED_HOST_AND_DEVICE_PENDING",
        "baseline": {"version": "1.9.121", "sha256": BASE_SHA256, "bytes": BASE_BYTES},
        "artifact": {"name": SINGLE, "sha256": sha(artifact.read_bytes()), "bytes": artifact.stat().st_size},
        "tool_sha256": common.TOOL_PINS, "jdk_identity": identity,
        "compiled_input_sha256": input_pins,
        "production_helper_roots": families,
        "resource_delta": sorted(changed), "native_payloads_preserved": native,
        "native_payload_count": len(native), "native_assets_byte_identical": True,
        "stock_methods_and_contracts_byte_identical": True,
        "installer_byte_identical": True, "native_installer_dex_semantics_identical": True,
        "helper_references_verified": True, "deterministic_dex_verified": True,
        "deterministic_archive_verified": True, "morphe_loader_verified": True,
        "inventory": json.loads((emitted / "inventory19122.json").read_text()),
        "host_tests_passed": False, "original_apk_apply_tested": False,
        "device_tested": False, "device_performance_measured": False,
    }
    (args.output / "build19122.json").write_bytes(json_bytes(report))
    print(json.dumps({"status": report["status"], "artifact": report["artifact"]}, ensure_ascii=False, indent=2))
    return report


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("baseline", "tools", "jdk", "families", "work", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    arguments = parser.parse_args()
    for name, value in vars(arguments).items():
        setattr(arguments, name, value.resolve())
    build(arguments)
