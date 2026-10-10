#!/usr/bin/env python3
"""Production idle tuner and certificate service with explicit controlled transport.

The original75/76 fixtures are retained unchanged. This replaces only their
candidate-count and foreground-proof assumptions; actual shaders are tested by
host_gpu_programs1978 and production foreground code by host_native_facade1978.
"""
from pathlib import Path
import hashlib, importlib.util, json, shutil, subprocess
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    spec=importlib.util.spec_from_file_location('queue78',source/'host_qualification1967.py');q=importlib.util.module_from_spec(spec);spec.loader.exec_module(q)
    fixtures=work/'fixtures';classes=work/'classes';classes.mkdir(exist_ok=True)
    for name,body in q.FIXTURES.items():
        if name.startswith('android/'):
            p=fixtures/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body)
    for name in ('TuningFixtures1978.java','Tuning1978Test.java'):
        p=fixtures/'com/hiro/ulike'/name;p.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source/'gpu78-fixtures'/name,p)
    java=str(Path(jdk)/'bin/java') if jdk else shutil.which('java');javac=str(Path(jdk)/'bin/javac') if jdk else shutil.which('javac')
    production=[source/'GpuQualification1961.java',source/'GpuStrongTuning1975.java'];pins={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in production}
    for label,cmd in [('compile',[javac,'--release','8','-encoding','UTF-8','-d',str(classes),*map(str,production),*map(str,fixtures.rglob('*.java'))]),('run',[java,'-ea','-cp',str(classes),'com.hiro.ulike.Tuning1978Test'])]:
        r=subprocess.run(cmd,text=True,capture_output=True,timeout=180);(work/(label+'.log')).write_text(r.stdout+r.stderr)
        if r.returncode:raise RuntimeError(label+'\n'+r.stdout+r.stderr)
    if pins!={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in production}:raise AssertionError('Production changed during tuning test')
    result=json.loads(r.stdout.strip().splitlines()[-1]);result.update(physical_android_tested=False,device_speedup_verified=False,twentyfour_candidate_tuning1978_verified=True,idle_two_full_argb_confidence_policy1978_verified=True,idle_snapshot_cancel_and_memory1978_verified=True,idle_upload_reuse1978_verified=True,new_gpu_balanced_five_percent_gate1978_verified=True,legacy_exact_rejections1978_preserved=True,worker_bound_speed_retry1978_verified=True,legacy_gpu_failure_direct_recovery1978_verified=True,production_source_sha256=pins)
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
