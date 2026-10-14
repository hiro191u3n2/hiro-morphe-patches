#!/usr/bin/env python3
"""Real save/queue/codec helpers with controllable Android/native ABI boundaries.

The historical Android boundary fixtures are reused. The production queue,
AsyncSave Job lifecycle and codec ownership/barriers are never substituted.
The final DEX publication hooks are separately audited by Transform1956.
"""
from pathlib import Path
import ast
import importlib.util
import json
import subprocess


def test(root, work):
    root, work = Path(root), Path(work) / 'host-h33-1956'
    work.mkdir(parents=True, exist_ok=True)
    spec = importlib.util.spec_from_file_location('save_boundary1935', root / 'host_save_baseline1935.py')
    base = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(base)
    fixtures = dict(base.FIXTURES)
    # These are literal Android/stock-application fixture definitions only.
    # The historical test implementation is replaced by the focused H33 test.
    tree = ast.parse((root / 'host_save1945.py').read_text())
    function = next(n for n in tree.body if isinstance(n, ast.FunctionDef) and n.name == 'test')
    for statement in function.body:
        if isinstance(statement, ast.Assign) and len(statement.targets) == 1:
            target = statement.targets[0]
            if isinstance(target, ast.Subscript) and isinstance(target.value, ast.Name) and target.value.id == 'fixtures':
                fixtures[ast.literal_eval(target.slice)] = ast.literal_eval(statement.value)
    fixtures['com/hiro/ulike/SaveHarness1935.java'] = (root / 'tests/h33/SaveHarness1935.java').read_text()
    fixtures['com/hiro/ulike/WholeRoute1953.java'] = '''package com.hiro.ulike; public class WholeRoute1953 {
      public static long retainedBytes(){return 0;} public static void foregroundStarted(){} public static void wake(){}
    }'''
    fixtures['com/hiro/ulike/GpuFinish1953.java'] = '''package com.hiro.ulike; public class GpuFinish1953 {public static long retainedBytes(){return 0;}}'''
    fixtures['com/hiro/ulike/SpeedWorkers1935.java'] = '''package com.hiro.ulike; public class SpeedWorkers1935 {
      public static volatile long scratch; public static long nativeRetainedBytes1956(){return scratch;}
    }'''
    fixtures['com/hiro/ulike/QualityPipeline1932.java'] = '''package com.hiro.ulike; import android.graphics.Bitmap;
      public class QualityPipeline1932 {public static void recycle1954(Bitmap b){if(b!=null&&!b.isRecycled())b.recycle();}}
    '''
    fixtures['com/hiro/ulike/ProcessingTiming1947.java'] = '''package com.hiro.ulike;
      public class ProcessingTiming1947 {
        public static class Trace{} public static class Scope{} public static class Token{} public static final int CORRECTION=2;
        public static Token beginStage(Trace t,int stage){return new Token();} public static void end(Token t){}
        public static Trace forKey(Object o){return null;} public static Trace begin(Object o){return new Trace();}
        public static Scope enter(Trace t){return new Scope();} public static void restore(Scope s){}
        public static Trace traceFor(Object o){return null;} public static void finish(Trace t,boolean success){}
        public static void bind(Object o,Trace t){}
      }
    '''
    paths = []
    for name, text in fixtures.items():
        path = work / 'fixtures' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)
        paths.append(path)
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    sources = [root / f for f in ('AsyncSave1935.java', 'SaveQueue1935.java', 'CodecDrain1945.java', 'ReflectionCache1945.java')]
    command = ['javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8', '-d', str(classes)] + list(map(str, sources + paths))
    compiled = subprocess.run(command, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    (work / 'compile.log').write_text(compiled.stdout)
    if compiled.returncode:
        raise RuntimeError(compiled.stdout)
    assertions = 0
    for fault in (None, 1, 2, 3):
        command = ['java', '-Xmx256m', '-cp', str(classes), 'com.hiro.ulike.SaveHarness1935'] + ([] if fault is None else [str(fault)])
        result = subprocess.run(command, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=55)
        (work / ('result.txt' if fault is None else f'fault-{fault}.txt')).write_text(result.stdout)
        if result.returncode:
            raise RuntimeError(result.stdout)
        assertions += int(result.stdout.split('assertions=')[1].split()[0])
    data = {
        'suite': 'h33_verified_close_save_overlap', 'status': 'passed', 'assertions': assertions,
        'actual_asyncsave_queue_codec_helpers_executed': True,
        'next_fifo_encoder_before_prior_validation_tail_completes': True,
        'hardware_release_requires_actual_close_barrier': True,
        'validation_and_publication_remain_original': True,
        'actual_visibility_and_receipts_submission_fifo': True,
        'blocked_actual_commit_has_no_early_success': True,
        'failed_correction_encoder_validation_publication_fifo_cleanup': True,
        'unconfirmed_codec_release_poison_preserved': True,
        'interrupt_cleanup_preserved': True,
        'three_photo_memory_and_input_ownership_preserved': True,
        'native_scratch_admission_overflow_checked': True,
        'fixture_scope': 'Real Java save/queue/codec helpers; scripted Android callbacks, encoder/file operations and stock application ABI. Physical codec/file verification is not claimed.',
        'physical_android_tested': False, 'device_speedup_verified': False,
    }
    (work / 'result.json').write_text(json.dumps(data, indent=2) + '\n')
    return data


if __name__ == '__main__':
    import sys
    print(json.dumps(test(Path(__file__).parent, Path(sys.argv[1])), indent=2))
