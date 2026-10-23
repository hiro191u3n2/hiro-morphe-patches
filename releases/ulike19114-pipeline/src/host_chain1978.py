#!/usr/bin/env python3
"""Run every retained .76 chain fixture with current .78 production dependencies.

The frozen .60 oracle and original .76 harness are executed unchanged. This
wrapper only supplies new production classes to the current javac graph.
"""
from pathlib import Path
import argparse, importlib.util, json

def additions(source):
    source=Path(source)
    names=('ResidentProof1978','GpuStrongRouting1978','CpuExact1978','CpuSingle1978','CpuModel1978','CpuFinishPolicy1978')
    paths=[source/(name+'.java') for name in names]
    for path in paths:
        if not path.is_file():raise AssertionError('Current production dependency absent: '+str(path))
    return paths

def test(source,work,jdk=None,ndk=None):
    source=Path(source).resolve();work=Path(work).resolve()
    spec=importlib.util.spec_from_file_location('chain1976_preserved_by1978',source/'host_chain1976.py')
    previous=importlib.util.module_from_spec(spec);spec.loader.exec_module(previous)
    original=previous.current_sources1976
    def current(root,fixtures):return list(dict.fromkeys(original(root,fixtures)+additions(root)))
    previous.current_sources1976=current
    result=previous.test(source,work,jdk,ndk)
    result['original_chain1976_cases_executed_unchanged']=True
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source',required=True);parser.add_argument('--work',required=True);parser.add_argument('--jdk');parser.add_argument('--ndk');args=parser.parse_args();print(json.dumps(test(args.source,args.work,args.jdk,args.ndk),indent=2))
