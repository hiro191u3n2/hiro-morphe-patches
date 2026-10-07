#!/usr/bin/env python3
"""Run production lens event helper with deterministic host View/native fixtures."""
from pathlib import Path
import argparse
import re
import subprocess

def test(root, work, androidjar=None):
    root, work = Path(root).resolve(), Path(work).resolve()
    folder = work / "lens1936-host"
    classes = folder / "classes"
    classes.mkdir(parents=True, exist_ok=True)
    sources = sorted((root / "lens1936-host").rglob("*.java"))
    compilation = subprocess.run(["javac", "-source", "8", "-target", "8", "-encoding", "UTF-8", "-d", str(classes),
        str(root / "LensVisibility1936.java"), *map(str,sources)], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (folder/"compile.log").write_text(compilation.stdout)
    compilation.check_returncode()
    run = subprocess.run(["java", "-cp", str(classes), "com.hiro.ulike.LensVisibilityHost1936"],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (work/"host-lens1936.txt").write_text(run.stdout)
    run.check_returncode()
    match = re.search(r"^HOST_LENS1936_ASSERTIONS=(\d+)$",run.stdout,re.M)
    if not match: raise RuntimeError("Lens host regression did not complete")
    print(run.stdout,end="")
    return {"status":"passed","assertions":int(match.group(1)),"scope":"production Java helper with host View and native state fixtures; no hardware"}

if __name__=="__main__":
    p=argparse.ArgumentParser();p.add_argument("--root",type=Path,default=Path(__file__).resolve().parent);p.add_argument("--work",type=Path,required=True)
    a=p.parse_args();test(a.root,a.work)
