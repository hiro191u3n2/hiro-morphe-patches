#!/usr/bin/env python3
"""Failure injection at the actual synchronous/async capture reservation boundary.

The original .79 AsyncSave source must fail the pinned link-failure scenarios; the
candidate must release only the reservation it still owns and accept a new shot.
Android rendering and storage are scripted ABI boundaries, not device tests.
"""
from pathlib import Path
import hashlib
import importlib.util
import json
import os
import re
import shutil
import subprocess

REFERENCE_SHA = '7d0ea0908596ddec641266261f61485aedb1d699758cfa9489eea84a88fd17ae'


def module(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


def test(source, work, jdk=None, ndk=None):
    source = Path(source).resolve()
    work = Path(work).resolve() / 'host-audit-save1980'
    work.mkdir(parents=True, exist_ok=True)
    java = Path(jdk) / 'bin/java' if jdk else Path(shutil.which('java') or '/usr/bin/java')
    javac = Path(jdk) / 'bin/javac' if jdk else None
    compiler = [str(javac)] if javac and javac.exists() else [str(java), 'com.sun.tools.javac.Main']
    reference = source / 'tests1980/pipeline79-reference/AsyncSave1935.java'
    if hashlib.sha256(reference.read_bytes()).hexdigest() != REFERENCE_SHA:
        raise AssertionError('Published .79 reservation source changed')

    fixtures = dict(module(source / 'host_save_baseline1935.py', 'save_base1980').FIXTURES)
    fixtures['com/hiro/ulike/SaveHarness1935.java'] = (source / 'pipeline80-fixtures/SaveHarness1935.java').read_text()
    fixtures['android/graphics/Bitmap.java'] = fixtures['android/graphics/Bitmap.java'].replace(
        'public Config getConfig(){', 'public boolean isPremultiplied(){return true;}public Config getConfig(){')
    fixtures['com/hiro/ulike/ProcessingTiming1947.java'] = module(source / 'host_capture_save1947.py', 'save_timing1980').TIMING.replace(
        'public static Trace forKey(Object key){return MAP.get(key);}',
        'public static Trace forKey(Object key){if(SaveHarness1935.failTimingLookup){SaveHarness1935.failTimingLookup=false;throw new NoSuchMethodError("scripted timing lookup resolution");}return MAP.get(key);}'
    ).replace(
        'public static void finish(Trace t,boolean ok){',
        'public static void finish(Trace t,boolean ok){if(SaveHarness1935.failTimingFinish){SaveHarness1935.failTimingFinish=false;SaveHarness1935.failedTimingFinishes++;throw new NoSuchMethodError("scripted timing finish resolution");}')
    fixtures['com/hiro/ulike/WholeRoute1953.java'] = '''package com.hiro.ulike;public class WholeRoute1953{public static long retainedBytes(){return 0;}public static void foregroundStarted(){}public static void wake(){}}'''
    fixtures['com/hiro/ulike/GpuFinish1953.java'] = '''package com.hiro.ulike;public class GpuFinish1953{public static long retainedBytes(){if(SaveHarness1935.failAfterSubmit&&SaveQueue1935.nativeBytes()>0){SaveHarness1935.failAfterSubmit=false;throw new NoSuchMethodError("scripted queued optional helper");}return 0;}}'''
    fixtures['com/hiro/ulike/SpeedWorkers1935.java'] = '''package com.hiro.ulike;public class SpeedWorkers1935{public static long nativeRetainedBytes1956(){return 0;}}'''
    fixtures['com/hiro/ulike/QualityPipeline1932.java'] = '''package com.hiro.ulike;import android.graphics.Bitmap;public class QualityPipeline1932{public static void recycle1954(Bitmap b){if(b!=null&&!b.isRecycled())b.recycle();}}'''
    fixtures['com/hiro/ulike/CodecDrain1945.java'] = '''package com.hiro.ulike;public class CodecDrain1945{public static void awaitClosed(){}}'''
    fixtures['com/hiro/ulike/PhotoDetail.java'] = fixtures['com/hiro/ulike/PhotoDetail.java'].replace(
        'public static Settings snapshot1932(){return live;}',
        'public static Settings snapshot1932(){if(SaveHarness1935.failSnapshot){SaveHarness1935.failSnapshot=false;throw SaveHarness1935.failure();}return live;}')
    fixtures['com/hiro/ulike/ShotContext1932.java'] = fixtures['com/hiro/ulike/ShotContext1932.java'].replace(
        'map.put(b,forBitmap(a));',
        'SaveHarness1935.lastOwned=b;if(SaveHarness1935.failTransfer){SaveHarness1935.failTransfer=false;throw SaveHarness1935.failure();}map.put(b,forBitmap(a));ProcessingTiming1947.transfer(a,b);').replace(
        'map.put(b,new Snapshot(id));',
        'map.put(b,new Snapshot(id));ProcessingTiming1947.begin(b);ProcessingTiming1947.associate(b,id);')
    paths = []
    for name, body in fixtures.items():
        path = work / 'fixtures' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(body)
        paths.append(path)

    runs = []
    assertions = 0
    for label, async_source, modes in (
            ('published79', reference, ('snapshot-link', 'transfer-link', 'timing-link', 'transfer-finish-link')),
            ('candidate80', source / 'AsyncSave1935.java', ('snapshot-link', 'transfer-link', 'snapshot-assertion', 'transfer-assertion', 'snapshot-thread-death', 'transfer-thread-death', 'after-submit', 'timing-link', 'transfer-finish-link', 'transfer-finish-assertion'))):
        classes = work / label / 'classes'
        classes.mkdir(parents=True, exist_ok=True)
        command = compiler + ['-encoding', 'UTF-8', '-source', '8', '-target', '8', '-d', str(classes),
            str(async_source), str(source / 'SaveQueue1935.java'), str(source / 'ReflectionCache1945.java')] + list(map(str, paths))
        compiled = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=60)
        (work / label / 'compile.log').write_text(compiled.stdout)
        if compiled.returncode:
            raise AssertionError(compiled.stdout)
        for mode in modes:
            result = subprocess.run([str(java), '-Xmx256m', '-cp', str(classes), 'com.hiro.ulike.SaveHarness1935', mode],
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=15)
            (work / label / (mode + '.log')).write_text(result.stdout)
            if label == 'published79':
                if result.returncode == 0 or 'capture and exit reservation leaked' not in result.stdout:
                    raise AssertionError('Published .79 must expose the reservation leak: ' + result.stdout)
            else:
                if result.returncode:
                    raise AssertionError(result.stdout)
                assertions += int(re.search(r'assertions=(\d+)', result.stdout).group(1))
            runs.append({'source': label, 'scenario': mode, 'exit_code': result.returncode,
                'expected_regression': label == 'published79', 'log': result.stdout})
    output = {'suite': 'audit-save1980', 'status': 'passed', 'assertions': assertions,
        'physical_android_tested': False, 'baseline_source_sha256': REFERENCE_SHA,
        'published79_linkage_reservation_leaks_reproduced': True,
        'candidate_error_paths_release_owned_bitmap_and_reservation': True,
        'fatal_errors_rethrown_after_cleanup': True,
        'next_capture_after_failure_succeeds': True, 'submitted_job_not_released_twice': True,
        'diagnostic_linkage_failure_does_not_block_terminal_cleanup': True,
        'runs': runs}
    (work / 'result.json').write_text(json.dumps(output, ensure_ascii=False, indent=2) + '\n')
    return output


if __name__ == '__main__':
    import sys
    print(json.dumps(test(Path(__file__).parent, sys.argv[1], sys.argv[2] if len(sys.argv)>2 else None), ensure_ascii=False, indent=2))
