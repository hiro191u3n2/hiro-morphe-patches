#!/usr/bin/env python3
"""Run unchanged production GX JNI and compute shaders on Mesa GLES.

The oracle is independently compiled published 1.9.60 Java, frozen before GX
edits. Software Mesa is real shader execution, not a physical Android benchmark.
Unsupported FP64 is reported explicitly and must not be treated as GPU coverage.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import re
import shutil
import subprocess

PIN_SHA = '67635ffa721ce9e3df893ef6bbe7b7441e62aac16fad49e5447e0206aa9e3dd9'

def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def run(args, log, env=None, timeout=360):
    result = subprocess.run(list(map(str, args)), capture_output=True, text=True,
                            env=env, timeout=timeout)
    Path(log).write_text(result.stdout + result.stderr)
    if result.returncode:
        raise RuntimeError('GX1961 executed test failed: ' + str(log) + '\n' +
                           result.stdout[-8000:] + result.stderr[-8000:])
    return result.stdout

def all_sources(root,baseline=False):
    from host_speed1959 import _sources
    root=Path(root)
    names=['NativeSpeed1935','FastPixels1933','GpuNoise1960','GpuPolicy1960','GpuStrong1960','GpuSingle1960','GpuGeometry1960']
    rows=_sources(root)+[root/(n+'.java') for n in names]
    if not baseline:
        import json
        for name in json.loads((root/'production1962.json').read_text()):
            path=root/(name+'.java')
            if not path.is_file():path=root/'quality-dependencies/com/hiro/ulike'/(name+'.java')
            rows=[p for p in rows if p.name!=path.name];rows.append(path)
    return list(dict.fromkeys(rows))

def test_inherited(root, work, jdk=None, ndk=None):
    root, work = Path(root).resolve(), Path(work).resolve() / 'host-gpu1961'
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    ndk = Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
    reference = root / 'tests1961/published1960-reference'
    if sha(reference / 'pins.json') != PIN_SHA:
        raise AssertionError('Frozen published .60 pin manifest changed')
    pins = json.loads((reference / 'pins.json').read_text())
    for name, expected in pins['files'].items():
        if sha(reference / name) != expected['sha256']:
            raise AssertionError('Frozen .60 CPU source changed: ' + name)
    from host_speed1959 import _sources
    baseline_sources = all_sources(reference, baseline=True) + [root / 'tests1961/Oracle1961.java',root/'tests1961/Analysis1961Oracle.java']
    classes = work / 'reference-classes'
    classes.mkdir(exist_ok=True)
    run([jdk / 'bin/javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8',
         '-Xlint:-options', '-d', classes, *baseline_sources], work / 'oracle-compile.log')
    oracle_log = run([jdk / 'bin/java', '-XX:ActiveProcessorCount=4',
                     '-Djava.library.path=' + str(work / 'unavailable'),
                     '-cp', classes, 'com.hiro.ulike.Oracle1961', work / 'published1960.bin'],
                    work / 'oracle-run.log')
    oracle = json.loads(oracle_log.strip())
    if oracle['referenceNativeEnabled'] or oracle['records'] < 80:
        raise AssertionError('Independent CPU reference generation missing')

    run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-Djava.library.path='+str(work/'unavailable'),'-cp',classes,'com.hiro.ulike.Analysis1961Oracle',work/'analysis1961-expected.bin'],work/'analysis1961-oracle-run.log')

    graphics = ndk / 'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
    headers = work / 'headers'
    headers.mkdir(exist_ok=True)
    for name in ('EGL', 'GLES3', 'KHR'):
        shutil.copytree(graphics / name, headers / name, dirs_exist_ok=True)
    spec = importlib.util.spec_from_file_location('gx1961_builder', root / 'native1960/build_native1960.py')
    builder = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(builder)
    builder.shader_header(root / 'native1960', work)
    tracked = [root / 'GpuNoise1960.java', root / 'native1960/engine1960.c',
               root / 'native1960/build_native1960.py', root/'native1960/residual_blocks1961.c', *sorted((root / 'native1960').glob('*.comp'))]
    before = {str(p.relative_to(root)): sha(p) for p in tracked}
    library = work / 'libulike_gpu1960.so'
    run(['cc', '-std=c11', '-O3', '-shared', '-fPIC', '-fno-fast-math', '-ffp-contract=off',
         '-Wall', '-Wextra', '-Werror', '-Wno-misleading-indentation', '-DANDROID',
         '-I' + str(headers), '-I' + str(jdk / 'include'), '-I' + str(jdk / 'include/linux'),
         '-I' + str(work), root / 'native1960/engine1960.c', *sorted((root/'native1960').glob('residual_blocks1961.c')), root / 'tests1960/fault1960.c',root/'tests1961/engine_observe1961.c',
         '-Wl,--wrap=glClientWaitSync', '-Wl,--wrap=glUnmapBuffer', '-Wl,--wrap=glDispatchCompute', '-Wl,--no-undefined',
         '-l:libEGL.so.1', '-l:libGL.so.1','-lm', '-o', library], work / 'native-compile.log')
    current = work / 'current-classes'
    current.mkdir(exist_ok=True)
    run([jdk / 'bin/javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8',
         '-Xlint:-options', '-cp', classes, '-d', current, root / 'GpuNoise1960.java',
         root / 'tests1960/Native1960Test.java'], work / 'current-compile.log')
    env = dict(os.environ, EGL_PLATFORM='surfaceless', LIBGL_ALWAYS_SOFTWARE='1')
    command = [jdk / 'bin/java', '-Xcheck:jni', '-Djava.library.path=' + str(work),
               '-cp', str(current) + os.pathsep + str(classes), 'com.hiro.ulike.Native1960Test']
    reports = {}
    for name, arguments in [('pixels', [work / 'published1960.bin']),
                            ('foreign_context', ['fault', 'context']),
                            ('late_unmap_failure', ['fault', 'unmap']),
                            ('unknown_fence_completion', ['fault', 'timeout'])]:
        output = run(command + arguments, work / (name + '.log'), env=env)
        if 'WARNING in native method' in output or 'FATAL ERROR' in output:
            raise AssertionError('JNI checker failed: ' + name)
        found = re.search(r'^RESULT (\{[^\n]+\})$', output, re.M)
        if not found:
            raise AssertionError('No executed result: ' + name)
        reports[name] = json.loads(found.group(1))
        if reports[name].get('status') != 'passed' or reports[name].get('assertions', 0) < 1:
            raise AssertionError('Executed assertions failed: ' + name)
    if before != {str(p.relative_to(root)): sha(p) for p in tracked}:
        raise AssertionError('Production source changed during test; rerun required')
    spec = importlib.util.spec_from_file_location('single_fp64_1960_host', root / 'tests1960/host_single_fp64_1960.py')
    desktop_single = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(desktop_single)
    reports['desktop_single_fp64'] = desktop_single.test(root, work, work / 'published1960.bin')
    facade_classes = work / 'facade-classes'
    facade_classes.mkdir(exist_ok=True)
    facade_sources = all_sources(root) + [root/'tests1960/Native1960Test.java',root/'tests1961/Facade1961Test.java']
    facade_sources = list(dict.fromkeys(facade_sources))
    facade_pins = {str(p.relative_to(root)): sha(p) for p in facade_sources}
    run([jdk / 'bin/javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8',
         '-Xlint:-options', '-d', facade_classes, *facade_sources], work / 'facade-compile.log')
    for name, native_path, use_gpu, extra in (
            ('production_facades_gpu', work, 'true', []),
            ('production_facades_unavailable', work / 'unavailable', 'false', []),
            ('production_facade_late_failure', work, 'true', ['fault'])):
        output = run([jdk / 'bin/java', '-XX:ActiveProcessorCount=4', '-Xcheck:jni',
                      '-Djava.library.path=' + str(native_path), '-cp', facade_classes,
                      'com.hiro.ulike.Facade1961Test', work / 'published1960.bin', use_gpu, *extra],
                     work / (name + '.log'), env=env)
        found = re.search(r'^RESULT (\{[^\n]+\})$', output, re.M)
        if not found or 'WARNING in native method' in output or 'FATAL ERROR' in output:
            raise AssertionError('Facade execution/JNI report absent: ' + name)
        reports[name] = json.loads(found.group(1))
        if reports[name].get('status') != 'passed' or reports[name].get('assertions', 0) < 1:
            raise AssertionError('Facade regression failed: ' + name)
    if facade_pins != {str(p.relative_to(root)): sha(p) for p in facade_sources}:
        raise AssertionError('Production facade sources changed during execution; rerun')
    before.update(facade_pins)
    new_sources=all_sources(root)+[root/'tests1960/Native1960Test.java']+[p for p in sorted((root/'tests1961').glob('*Test.java')) if p.name!='Qualification1961Test.java']+[root/'tests1961/ReferenceFacePixels1960.java',root/'tests1961/Analysis1961Oracle.java']
    new_classes=work/'new61-classes';new_classes.mkdir(exist_ok=True)
    new_sources=list(dict.fromkeys(new_sources))
    run([jdk/'bin/javac','-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-d',new_classes,*new_sources],work/'new61-compile.log')
    for label,klass in [('protection1961','Protection1961Test'),('analysis1961','Analysis1961Test'),('batch_engine1961','Batch1961Test'),('geometry_tiles1961','GeometryTiles1961Test'),('single_residual_jni1961','SingleResidual1961Test'),('analysis_background1961','AnalysisBackground1961Test'),('strong_facade1961','StrongFacade1961Test')]:
        output=run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-Xcheck:jni','-Djava.library.path='+str(work),'-cp',new_classes,'com.hiro.ulike.'+klass,*(['large'] if label=='geometry_tiles1961' else [work/'analysis1961-expected.bin'] if label=='analysis1961' else [work/'published1960.bin'] if label in ('single_residual_jni1961','strong_facade1961') else [])],work/(label+'.log'),env=env)
        found=re.search(r'^RESULT (\{[^\n]+\})$',output,re.M)
        if not found or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError('New .61 suite result missing: '+label)
        reports[label]=json.loads(found.group(1))
        if reports[label].get('status')!='passed' or reports[label].get('assertions',0)<1:raise AssertionError('New .61 suite failed: '+label)
    for label,mode in [('batch_second_unmap_failure','unmap'),('batch_two_bank_unknown_completion','timeout')]:
        output=run([jdk/'bin/java','-Xcheck:jni','-Djava.library.path='+str(work),'-cp',new_classes,'com.hiro.ulike.Batch1961Test','fault',mode],work/(label+'.log'),env=env)
        found=re.search(r'^RESULT (\{[^\n]+\})$',output,re.M)
        if not found or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError('Batch fault result missing: '+label)
        reports[label]=json.loads(found.group(1))
        if reports[label].get('status')!='passed' or reports[label].get('assertions',0)<1:raise AssertionError('Batch fault coverage failed: '+label)
    before.update({str(p.relative_to(root)):sha(p) for p in new_sources})

    for label,location in [('residual1961',root/'tests1961/single_gpu1961.py'),('qualification1961',root/'host_qualification1961.py')]:
        spec=importlib.util.spec_from_file_location(label+'_host',location)
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        row=module.test(root,work,jdk)
        if label=='residual1961':
            if row.get('status')!='passed' or row.get('pixels',0)<1 or row.get('errors')!=[]:raise AssertionError('Residual actual shader comparison missing')
            row=dict(row,assertions=row['pixels']+row.get('rounding_boundary_cases',0))
        if row.get('status')!='passed' or row.get('assertions',0)<1:raise AssertionError('Independent .61 module failed: '+label)
        reports[label]=row
    for p in sorted((root/'tests1961').rglob('*')):
        if p.is_file() and '__pycache__' not in p.parts and p.suffix in ('.java','.py','.c','.json'):before[str(p.relative_to(root))]=sha(p)

    spec=importlib.util.spec_from_file_location('strong_shared1961_host',root/'tests1961/strong_shared_cache_compare.py')
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    row=module.test(root,work)
    if row.get('status')!='passed' or row.get('actual_mesa_gles_execution') is not True:raise AssertionError('Shared NLM actual shader execution missing')
    reports['strong_shared_cache1961']=dict(row,assertions=row['pixels_compared']+row['confidence_values_compared'])

    pipeline_dumps = []
    for label, source_root, sources, native_path, use_gpu in (
            ('published1960_pipeline', reference, all_sources(reference,baseline=True), work / 'unavailable', 'false'),
            ('candidate1961_pipeline', root, facade_sources, work, 'true')):
        pipeline_classes = work / (label + '-classes')
        pipeline_classes.mkdir(exist_ok=True)
        selected = [p for p in sources if p.name != 'FastResize1933.java']
        selected += [source_root / 'FastResize1933.java', source_root / 'NativeSpeed1935.java',
                     source_root / 'FastPixels1933.java', root / 'tests1961/Pipeline1961Test.java']
        selected = list(dict.fromkeys(selected))
        run([jdk / 'bin/javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8',
             '-Xlint:-options', '-d', pipeline_classes, *selected], work / (label + '-compile.log'))
        dump = work / (label + '.bin')
        output = run([jdk / 'bin/java', '-XX:ActiveProcessorCount=4', '-Xcheck:jni',
                      '-Djava.library.path=' + str(native_path), '-cp', pipeline_classes,
                      'com.hiro.ulike.Pipeline1961Test', dump, use_gpu], work / (label + '.log'), env=env)
        found = re.search(r'^RESULT (\{[^\n]+\})$', output, re.M)
        if not found:
            raise AssertionError('Complete save pipeline execution report missing')
        reports[label] = json.loads(found.group(1))
        pipeline_dumps.append(dump.read_bytes())
    if pipeline_dumps[0] != pipeline_dumps[1]:
        raise AssertionError('Saved-image pipeline differs from frozen published 1960')
    reports['candidate1961_pipeline']['exact_published1960_dump_sha256'] = hashlib.sha256(pipeline_dumps[0]).hexdigest()
    before['FastResize1933.java'] = sha(root / 'FastResize1933.java')
    report = {
        'schema': 'ulike-gx1961-executed-production-jni-v1', 'status': 'passed',
        'assertions': sum(r.get('assertions', 0) for r in reports.values()),
        'oracle': oracle, 'reports': reports,
        'reference': 'byte-frozen published 1.9.60 Java CPU in independent JVM',
        'oracle_binary_sha256': sha(work / 'published1960.bin'),
        'unchanged_production_engine_and_shader_sources': True,
        'gpu_shader_execution_on_host': True,
        'host_backend': 'Mesa software surfaceless EGL GLES',
        'host_pixel_equivalence_to_baseline': True,
        'single_fp64_shader_executed': reports['pixels']['singleFp64Supported'],
        'physical_android_tested': False, 'device_speedup_verified': False,
        'device_quality_improvement_verified': False,
        'cpu_fallback_capability_required': not reports['pixels']['singleFp64Supported'],
        'gx11_gx23_actual_host_coverage': True,
        'sources': before,
        'tests': {str(p.relative_to(root)): sha(p) for p in sorted((root / 'tests1960').glob('*')) if p.is_file()},
    }
    (work / 'result.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


FROZEN61_PIN_SHA256 = '84e54fc1402fd79b0d14df461c88041b9720402017ed6d97138003fb5823184b'
FROZEN61_MPP_SHA256 = 'cbdb34bd082a114b3644f9a95c8b7ceccafcc7a49f3e198e41a48495f14e50d9'
NEW_SUITES = {
    'post_noise_resident_chain1962': 'tests1962/chain1962_test.py',
    'residual_overlap1962': 'tests1962/residual_overlap1962.py',
    'transfer1962': 'host_transfer1962.py',
    'strong_modes1962': 'tests1962/strong_modes1962.py',
    'qualification_retry1962': 'host_qualification1962.py',
}

def frozen61(root):
    reference = Path(root)/'tests1962/published1961-reference'
    if sha(reference/'pins.json') != FROZEN61_PIN_SHA256:
        raise AssertionError('Frozen published .61 source manifest changed')
    pins = json.loads((reference/'pins.json').read_text())
    if pins.get('baseline_version') != '1.9.61' or pins.get('baseline_mpp_sha256') != FROZEN61_MPP_SHA256:
        raise AssertionError('Frozen .61 oracle identity differs')
    for name,expected in pins['files'].items():
        data=(reference/name).read_bytes()
        if len(data)!=expected['bytes'] or hashlib.sha256(data).hexdigest()!=expected['sha256'] or hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()!=expected['git_blob_sha1']:
            raise AssertionError('Frozen .61 CPU source changed: '+name)
    return reference,pins

def test(root, work, jdk=None, ndk=None):
    root,work=Path(root).resolve(),Path(work).resolve()
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
    result=test_inherited(root,work,jdk,ndk)
    actual_work=work/'host-gpu1961'
    reference,pins=frozen61(root)
    env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
    reports=result['reports']
    dumps=[]
    for label,source_root,sources,native_path,gpu in (
        ('published1961_pipeline',reference,[reference/name for name in pins['files'] if name.endswith('.java')],actual_work/'unavailable','false'),
        ('candidate1962_pipeline',root,all_sources(root),actual_work,'true')):
        classes=actual_work/(label+'-classes');classes.mkdir(exist_ok=True)
        selected=list(dict.fromkeys(sources+[root/'tests1961/Pipeline1961Test.java',root/'tests1960/Native1960Test.java']))
        run([jdk/'bin/javac','-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-d',classes,*selected],actual_work/(label+'-compile.log'))
        dump=actual_work/(label+'.bin')
        output=run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-Xcheck:jni','-Djava.library.path='+str(native_path),'-cp',classes,'com.hiro.ulike.Pipeline1961Test',dump,gpu],actual_work/(label+'.log'),env=env)
        found=re.search(r'^RESULT (\{[^\n]+\})$',output,re.M)
        if not found or 'WARNING in native method' in output or 'FATAL ERROR' in output:
            raise AssertionError('Frozen .61/full candidate .62 pipeline report absent: '+label)
        reports[label]=json.loads(found.group(1));dumps.append(dump.read_bytes())
        result['sources'].update({p.relative_to(root).as_posix():sha(p) for p in selected})
    if dumps[0]!=dumps[1]:
        raise AssertionError('Saved-image output differs from exact published .61 CPU source oracle')
    reports['candidate1962_pipeline']['exact_published1961_dump_sha256']=hashlib.sha256(dumps[0]).hexdigest()
    result['sources']['tests1962/published1961-reference/pins.json']=sha(reference/'pins.json')
    for label,relative in NEW_SUITES.items():
        path=root/relative
        spec=importlib.util.spec_from_file_location(label+'_host',path)
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        row=module.test(root,work,jdk,ndk)
        if label=='strong_modes1962':
            row=dict(row,assertions=row['pixels_compared']+row['confidence_values_compared']+row['exact_early_rejection_cutoffs']['cases'])
        if not isinstance(row,dict) or row.get('status')!='passed' or type(row.get('assertions')) is not int or row['assertions']<=0:
            raise AssertionError('New .62 executed suite missing positive assertions: '+label)
        reports[label]=row;result['sources'][relative]=sha(path)
    for p in sorted((root/'tests1962').rglob('*')):
        if p.is_file() and '__pycache__' not in p.parts and p.suffix in ('.java','.py','.c','.json','.comp'):
            result['sources'][p.relative_to(root).as_posix()]=sha(p)
    result.update(schema='ulike-gx1962-executed-production-jni-v1',
        assertions=sum(row.get('assertions',0) for row in reports.values()),
        gx24_gx29_actual_host_coverage=True,
        host_pixel_equivalence_to_published1961=True,
        published1961_oracle={'status':'passed','source_manifest_sha256':FROZEN61_PIN_SHA256,'baseline_mpp_sha256':FROZEN61_MPP_SHA256,'referenceNativeEnabled':False,'cases':reports['published1961_pipeline']['cases'],'exact_output_sha256':hashlib.sha256(dumps[0]).hexdigest()},
        reference='byte-frozen published 1.9.60/1.9.61 Java CPU in independent JVMs')
    (actual_work/'result1962.json').write_text(json.dumps(result,indent=2)+'\n')
    return result

if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--root', default=str(Path(__file__).resolve().parent))
    p.add_argument('--work', required=True)
    p.add_argument('--jdk')
    p.add_argument('--ndk')
    a = p.parse_args()
    print(json.dumps(test(a.root, a.work, a.jdk, a.ndk), indent=2))
