#!/usr/bin/env python3
"""Execute inherited pixel/GPU gates and whole-audit fault regressions.

Actual JNI/Mesa execution is host coverage. Physical Galaxy testing is separate.
The published .62 pipeline oracle executes in its own JVM with frozen sources.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re
import host_gpu1962 as inherited

ROOT = Path(__file__).resolve().parent
FROZEN_SHA = '19647f9a4e59aac9d7dbc69e0f97a7f2b24a1da6659658dc7518f1766c57b33c'
BASE_SHA = 'e3db4401688863d56de30d6e6afdfc6dc8a941d42f7cc1cebec5700bd0f775cd'
AUDIT_SUITES = {
    'audit_gpu': 'tests1963/gpu_engine1963_test.py',
    'audit_save': 'tests1963/save_audit1963.py',
    'audit_pipeline': 'tests1963/pipeline_audit1963.py',
    'audit_ui': 'tests1963/ui_audit1963.py',
    'audit_blacktap': 'tests1963/blacktap1963.py',
    'audit_exitbusy': 'tests1963/exitbusy1963.py',
}

def test(root, work, jdk=None, ndk=None):
    root, work = Path(root).resolve(), Path(work).resolve()
    jdk = Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    ndk = Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
    result = inherited.test(root, work, jdk, ndk)
    reports = result['reports']
    reference = root/'tests1963/published1962-reference'
    raw = (reference/'pins.json').read_bytes()
    if hashlib.sha256(raw).hexdigest() != FROZEN_SHA:
        raise AssertionError('Published .62 oracle manifest changed')
    pins = json.loads(raw)
    if pins['baseline_version'] != '1.9.62' or pins['baseline_mpp_sha256'] != BASE_SHA:
        raise AssertionError('Wrong latest-published oracle')
    for name, expected in pins['files'].items():
        data = (reference/name).read_bytes()
        if len(data) != expected['bytes'] or hashlib.sha256(data).hexdigest() != expected['sha256']:
            raise AssertionError('Frozen .62 source changed: '+name)
    classes = work/'published1962-pipeline-classes'
    classes.mkdir()
    sources = [reference/name for name in pins['pipeline_sources']]
    sources += [root/'tests1961/Pipeline1961Test.java',root/'tests1960/Native1960Test.java']
    inherited.run([jdk/'bin/javac','-encoding','UTF-8','-source','8','-target','8','-Xlint:-options',
        '-d',classes,*sources],work/'published1962-compile.log')
    dump = work/'published1962-pipeline.bin'
    output = inherited.run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-cp',classes,
        '-Djava.library.path='+str(work/'unavailable'),
        'com.hiro.ulike.Pipeline1961Test',dump,'false'],work/'published1962-pipeline.log')
    found = re.search(r'^RESULT (\{[^\n]+\})$',output,re.M)
    if not found:
        raise AssertionError('Published .62 oracle report absent')
    row = json.loads(found.group(1))
    candidate = work/'host-gpu1961/candidate1962_pipeline.bin'
    if dump.read_bytes() != candidate.read_bytes():
        raise AssertionError('Saved pixels differ from latest published .62')
    reports['published1962_pipeline'] = row
    digest = hashlib.sha256(dump.read_bytes()).hexdigest()
    result['published1962_oracle'] = {
        'status':'passed','baseline_mpp_sha256':BASE_SHA,'source_manifest_sha256':FROZEN_SHA,
        'referenceNativeEnabled':False,'cases':row['cases'],'exact_output_sha256':digest,
    }
    result['host_pixel_equivalence_to_published1962'] = True
    for label, relative in AUDIT_SUITES.items():
        spec = importlib.util.spec_from_file_location(label+'_host',root/relative)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        row = module.test(root,work,jdk,ndk)
        if not isinstance(row,dict) or row.get('status') != 'passed' or type(row.get('assertions')) is not int or row['assertions'] <= 0:
            raise AssertionError('Missing executed whole-audit assertions: '+label)
        reports[label] = row
    for p in sorted((root/'tests1963').rglob('*')):
        if p.is_file() and '__pycache__' not in p.parts:
            result['sources'][p.relative_to(root).as_posix()] = inherited.sha(p)
    result.update(schema='ulike-audit1963-executed-production-jni-v1',
        assertions=sum(row.get('assertions',0) for row in reports.values()),
        whole_audit_regressions_passed=True,physical_android_tested=False,
        device_speedup_verified=False,device_quality_improvement_verified=False)
    (work/'result1963.json').write_text(json.dumps(result,sort_keys=True,indent=2)+'\n')
    return result

if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--root',default=str(ROOT));p.add_argument('--work',required=True)
    p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args()
    print(json.dumps(test(a.root,a.work,a.jdk,a.ndk),indent=2))
