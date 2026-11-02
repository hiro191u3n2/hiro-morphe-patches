#!/usr/bin/env python3
"""Executable timing tests using Android surface doubles; no device speed claims."""
from pathlib import Path
import os, subprocess, tempfile, json, shutil
ROOT=Path(__file__).resolve().parent

def _java_tool(name):
    for variable in ('ULIKE_JDK_HOME', 'JAVA_HOME'):
        value=os.environ.get(variable)
        if value:
            candidate=Path(value)/'bin'/name
            if candidate.is_file():return str(candidate)
    found=shutil.which(name)
    if found:return found
    raise RuntimeError(f'{name} not available; set ULIKE_JDK_HOME or JAVA_HOME, or add a JDK to PATH')

def test(root=None, work=None):
    root=Path(root or ROOT)
    def execute(out):
        out=Path(out);out.mkdir(parents=True,exist_ok=True)
        sources=[root/'ProcessingTiming1947.java', root/'tests/ProcessingTiming1947Test.java']
        sources+=sorted((root/'tests/timing1947-fixtures').rglob('*.java'))
        from host_common1952 import sources1952
        sources=sources1952(root,sources)
        subprocess.run([_java_tool('javac'),'-encoding','UTF-8','-source','8','-target','8','-d',str(out)]+list(map(str,sources)),check=True,capture_output=True,text=True)
        result=subprocess.run([_java_tool('java'),'-cp',str(out),'com.hiro.ulike.ProcessingTiming1947Test'],check=True,capture_output=True,text=True)
        return {'ok':True,'suite':'timing1947','result':result.stdout.strip()}
    if work is not None:return execute(Path(work)/'timing1947-classes')
    with tempfile.TemporaryDirectory(prefix='ulike-timing1947-') as out:return execute(out)

if __name__=='__main__':print(json.dumps(test(),ensure_ascii=False))
