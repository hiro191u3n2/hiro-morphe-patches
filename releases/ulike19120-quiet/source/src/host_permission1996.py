#!/usr/bin/env python3
"""Retain the complete .95 queue suite and exercise non-reclaiming decisions."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, subprocess

def test(source, work, jdk=None, ndk=None):
    source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    files=[source/'GpuQualification1961.java',source/'tests1996/PermissionSnapshot1996Test.java',Path(__file__).resolve()]
    pins={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
    spec=importlib.util.spec_from_file_location('permission96_retained',source/'host_queue1995.py')
    retained=importlib.util.module_from_spec(spec);spec.loader.exec_module(retained)
    previous=retained.test(source,work/'retained95',jdk,ndk)
    classes=work/'retained95/retained94/retained93/classes'
    def run(label,args):
        result=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=90)
        (work/(label+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise AssertionError(label+'\n'+(result.stdout+result.stderr)[-14000:])
        return result.stdout
    run('permission-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',classes,'-d',classes,source/'tests1996/PermissionSnapshot1996Test.java'])
    current=json.loads(run('permission-run',[jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.PermissionSnapshot1996Test']).strip().splitlines()[-1])
    if pins!={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in files}:raise AssertionError('Permission source changed during verification')
    result=dict(status='passed',assertions=previous['assertions']+current['assertions'],tests=[previous,current],source_sha256=pins,physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source',required=True);parser.add_argument('--work',required=True);parser.add_argument('--jdk');parser.add_argument('--ndk')
    args=parser.parse_args();print(json.dumps(test(args.source,args.work,args.jdk,args.ndk)))
