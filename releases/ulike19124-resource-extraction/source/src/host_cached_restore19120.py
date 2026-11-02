#!/usr/bin/env python3
"""Exercise real cache-only restore classes against hostile no-oracle peers."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import subprocess

ROOT = Path(__file__).resolve().parent
CLASSES = ("GpuStrongDefault1990", "GpuStrongOptimized19119", "GpuModelDefault1990", "GpuFinishDefault1990")
SCENARIOS = ("none", "exact", "full", "rejected", "optimized-none", "optimized-exact", "optimized-full", "optimized-rejected", "optimized-mismatch")

def run(command, *, failure=False):
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True)
    if (result.returncode == 0) == failure:
        raise AssertionError("unexpected command result: " + " ".join(map(str, command)) + "\n" + result.stdout + result.stderr)
    return result.stdout + result.stderr

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--classes", type=Path, required=True)
    p.add_argument("--android", type=Path, required=True)
    p.add_argument("--work", type=Path, required=True)
    p.add_argument("--javac", default="javac")
    p.add_argument("--java", default="java")
    a = p.parse_args()
    a.classes, a.android, a.work = a.classes.resolve(), a.android.resolve(), a.work.resolve()
    actual, fixtures = a.work / "actual", a.work / "fixtures"
    actual.mkdir(parents=True, exist_ok=True); fixtures.mkdir(parents=True, exist_ok=True)
    fixture_sources = sorted((ROOT / "tests19120/cached-fixtures").rglob("*.java"))
    proof_sources = [ROOT / (n + ".java") for n in CLASSES] + fixture_sources + [
        ROOT / "tests19120/CachedRestore19120Test.java", Path(__file__).resolve()]
    source_sha256 = {path.relative_to(ROOT).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()
                     for path in proof_sources}
    run([a.javac, "-source", "8", "-target", "8", "-encoding", "UTF-8", "-bootclasspath", a.android,
         "-classpath", a.classes, "-d", actual, *[ROOT / (n + ".java") for n in CLASSES]])
    compile_cp = os.pathsep.join(map(str, (actual, a.classes, a.android)))
    run([a.javac, "-encoding", "UTF-8", "-classpath", compile_cp, "-d", fixtures,
         *fixture_sources, ROOT / "tests19120/CachedRestore19120Test.java"])
    cp = os.pathsep.join(map(str, (fixtures, actual, a.classes, a.android)))
    evidence = [run([a.java, "-cp", cp, "com.hiro.ulike.CachedRestore19120Test", s]).strip() for s in SCENARIOS]
    mutations = [
        ("GpuStrongOptimized19119", "optimized-full", "        if(!GpuQualification1961.automaticAllowed19120())return;\n", ""),
        ("GpuModelDefault1990", "exact",
         "            if(GpuCertificates1990.restore(layoutSuite1994(variant))==null||\n                    GpuCertificates1990.restore(speedSuite1994(variant))==null)continue;",
         "            if(false)continue;"),
    ]
    negatives = []
    for i, (name, scenario, before, after) in enumerate(mutations):
        source = (ROOT / (name + ".java")).read_text()
        if source.count(before) != 1: raise AssertionError("negative-control anchor changed: " + name)
        directory = a.work / ("negative" + str(i)); directory.mkdir(exist_ok=True)
        path = directory / (name + ".java"); path.write_text(source.replace(before, after))
        run([a.javac, "-source", "8", "-target", "8", "-encoding", "UTF-8", "-bootclasspath", a.android,
             "-classpath", compile_cp, "-d", directory, path])
        bad_cp = os.pathsep.join(map(str, (fixtures, directory, actual, a.classes, a.android)))
        failure = run([a.java, "-cp", bad_cp, "com.hiro.ulike.CachedRestore19120Test", scenario], failure=True)
        if "AssertionError" not in failure: raise AssertionError("negative control failed for unrelated reason")
        negatives.append({"mutation": name, "scenario": scenario, "detected": True})
    for path in proof_sources:
        if hashlib.sha256(path.read_bytes()).hexdigest() != source_sha256[path.relative_to(ROOT).as_posix()]:
            raise AssertionError("test input changed during execution: " + str(path))
    result = {"status": "PASS", "scenarios": 9, "assertions": 87, "results": evidence,
              "negative_controls": negatives, "physical_android_tested": False,
              "source_sha256": source_sha256,
              "scope": "actual restore classes; saved proof admission and preparation; no device performance claim"}
    (a.work / "cached-restore19120.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(result, ensure_ascii=False))

if __name__ == "__main__": main()
