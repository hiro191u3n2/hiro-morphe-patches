#!/usr/bin/env python3
"""Fresh .58/.59 binary-output differential plus inherited and actual ARM gates.

The immutable oracle consists of the exact published .58 production compilation
closure. Baseline and candidate execute separately, with independently rebuilt
JNI libraries. No baseline calls candidate production code. Every output pixel,
map pixel, evidence float bit and NR13 mask byte is compared without tolerance.
"""
from pathlib import Path
import argparse
import hashlib
import importlib
import json
import shutil
import subprocess

from host_common1954 import sources1954
from host_nr1955 import _java_tools

ROOT = Path(__file__).resolve().parent
REFERENCE = ROOT / 'tests1959/published1958-reference'
REFERENCE_PINS_SHA256 = '3ee53918cb7126a6c30377a1a890a944edfe341092c76d9d866c2463bdd52595'
BASELINE_MPP_SHA256 = '44ae9de43be78eb0bfe9121f4920469eeac9f62d84520f12904e4cbf9f7b39da'


def _sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def _execute(command, log, timeout=360):
    result = subprocess.run(list(map(str, command)), capture_output=True,
                            text=True, timeout=timeout)
    Path(log).write_text(result.stdout + result.stderr)
    if result.returncode:
        raise RuntimeError('H40-H45 executed host command failed: ' +
                           result.stdout[-3000:] + result.stderr[-12000:])
    return result.stdout


def _checked(report, name):
    if (not isinstance(report, dict) or report.get('status') != 'passed'
            or type(report.get('assertions')) is not int
            or report['assertions'] <= 0):
        raise AssertionError('Executed H40-H45 assertions missing: ' + name)
    return report


def baseline_pins():
    raw = (REFERENCE / 'pins.json').read_bytes()
    if hashlib.sha256(raw).hexdigest() != REFERENCE_PINS_SHA256:
        raise AssertionError('Frozen .58 source pin manifest changed')
    pins = json.loads(raw)
    if (pins.get('schema') != 'ulike-published1958-exact-source-oracle-v1'
            or pins.get('baseline_version') != '1.9.58'
            or pins.get('baseline_mpp_sha256') != BASELINE_MPP_SHA256):
        raise AssertionError('Incorrect .58 source oracle identity')
    for name, expected in pins['files'].items():
        data = (REFERENCE / name).read_bytes()
        git_blob = hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()
        if (len(data) != expected['bytes'] or hashlib.sha256(data).hexdigest() != expected['sha256']
                or git_blob != expected['git_blob_sha1']):
            raise AssertionError('Frozen published .58 oracle changed: ' + name)
    return pins


def _sources(root):
    root = Path(root)
    common = list((root / 'tests/pipeline1942-fixtures').rglob('*.java'))
    common += list((root / 'tests/timing1947-fixtures').rglob('*.java'))
    common += [root / name for name in (
        'SpeedWorkers1935.java', 'Scheduling1944.java', 'NoiseCache1944.java',
        'NativeSpeed1944.java', 'GpuInteger1949.java', 'QualityPixels1932.java',
        'NativeMoire1951.java', 'PolicyCache1945.java', 'QualityShadow1932.java',
        'SpatialNoise1934.java', 'LongMoire1934.java', 'ProcessingTiming1947.java',
        'QualityPipeline1932.java', 'SingleNoise1955.java', 'StrongNoise1957.java',
        'StrongNoise1958.java',
    )]
    return sources1954(root, common)


def _commit_fixture(source, target):
    """Model commit/readback precision at the Bitmap boundary in both JVMs.

    This is an explicit integer premultiplication/RGB565 host surrogate, not
    a claim to reproduce every physical Android Bitmap implementation.
    Production algorithms and pinned historical fixtures remain untouched.
    """
    source_text = Path(source).read_text()
    old = '  for(int row=0;row<h;row++)System.arraycopy(in,offset+row*stride,pixels,(y+row)*width+x,w);'
    new = '''  for(int row=0;row<h;row++)for(int column=0;column<w;column++) {
   int value=in[offset+row*stride+column],a=value>>>24;
   int r=value>>>16&255,g=value>>>8&255,b=value&255;
   if(config==Config.RGB_565) {
    int r5=r>>>3,g6=g>>>2,b5=b>>>3;
    r=(r5<<3)|(r5>>>2);g=(g6<<2)|(g6>>>4);b=(b5<<3)|(b5>>>2);a=255;
   } else if(config==Config.ARGB_8888&&premultiplied&&a!=255) {
    if(a==0)r=g=b=0;
    else {r=Math.min(255,(((r*a+127)/255)*255+a/2)/a);
     g=Math.min(255,(((g*a+127)/255)*255+a/2)/a);
     b=Math.min(255,(((b*a+127)/255)*255+a/2)/a);}
   }
   pixels[(y+row)*width+x+column]=(a<<24)|(r<<16)|(g<<8)|b;
  }'''
    if source_text.count(old) != 1:
        raise AssertionError('Commit-precision Bitmap fixture anchor changed')
    target = Path(target)
    target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(source_text.replace(old,new))
    return target


def differential(work, input=None, tools=None):
    """Focused exact proof; intentionally separate from mandatory formal run."""
    pins = baseline_pins()
    out = Path(work).resolve() / 'host-speed1959'
    out.mkdir(parents=True, exist_ok=True)
    compiler, java = _java_tools(tools)
    include = Path(java).resolve().parent.parent / 'include'
    cc = shutil.which('cc') or shutil.which('gcc')
    if not cc or not (include/'jni.h').is_file():
        raise AssertionError('Executing JDK JNI headers and C compiler required')
    flags = ['-O3', '-std=c11', '-Wall', '-Wextra', '-Werror', '-ffp-contract=off',
             '-fno-fast-math', '-fPIC', '-fvisibility=hidden', '-shared', '-Wl,--no-undefined']
    reports, builds, dumps, all_inputs, generated = {}, {}, {}, {}, {}
    compiled_java, compiled_native = {}, {}
    driver = ROOT / 'tests1959/ExactBaseline1959.java'
    owner_driver = ROOT / 'tests1959/ScratchOwners1959Test.java'
    for label, root in (('published1958', REFERENCE), ('candidate1959', ROOT)):
        classes = out / (label+'-classes')
        native = out / (label+'-native')
        for directory in (classes, native):
            if directory.exists():
                shutil.rmtree(directory)
            directory.mkdir()
        sources = _sources(root) + [driver]
        if label == 'candidate1959':
            sources += [owner_driver, ROOT/'tests/PipelineCarry1959Test.java',
                        ROOT/'tests/StrongWorkspace1959Test.java']
        original_bitmap = root/'tests/pipeline1942-fixtures/android/graphics/Bitmap.java'
        generated_bitmap = _commit_fixture(original_bitmap,
            out/(label+'-commit-fixture/android/graphics/Bitmap.java'))
        compile_sources = [generated_bitmap if p == original_bitmap else p for p in sources]
        generated[label] = {'original_source_sha256': _sha(original_bitmap),
                            'generated_source_sha256': _sha(generated_bitmap),
                            'scope': 'Identical explicit RGB565 and integer premultiplication commit/readback host surrogate'}
        inputs = {str(p.relative_to(root)) if p.is_relative_to(root) else str(p.relative_to(ROOT)): _sha(p)
                  for p in sources}
        native_inputs = {name: _sha(root/name) for name in
                         ('native1958/smooth_noise1958.c', 'native1951/moire1951.c')}
        # New production native includes are compiler inputs too.
        native_inputs.update({p.relative_to(root).as_posix(): _sha(p)
                              for p in (root/'native1958').glob('*.h')})
        all_inputs[label] = {'java': inputs, 'native': native_inputs}
        compiled_java.update({p.relative_to(ROOT).as_posix(): _sha(p) for p in sources})
        compiled_native.update({(root/name).relative_to(ROOT).as_posix(): value
                                for name,value in native_inputs.items()})
        _execute(compiler + ['-encoding', 'UTF-8', '-source', '8', '-target', '8',
                            '-Xlint:-options', '-d', classes, *compile_sources],
                 out/(label+'-compile.log'))
        for source, lib in (('native1958/smooth_noise1958.c', 'libulike_smooth1958.so'),
                            ('native1951/moire1951.c', 'libulike_moire1951.so')):
            _execute([cc, *flags, '-I'+str(include), '-I'+str(include/'linux'),
                      root/source, '-lm', '-pthread', '-o', native/lib],
                     out/(label+'-'+lib+'-compile.log'))
            if (native/lib).read_bytes()[:6] != b'\x7fELF\x02\x01':
                raise AssertionError('Fresh differential JNI ELF64 absent')
        builds[label] = {p.name: {'sha256': _sha(p), 'bytes': p.stat().st_size}
                         for p in native.glob('*.so')}
        for backend, library_path in (('java', out/'unavailable'), ('jni', native)):
            name = label+'-'+backend
            dump = out/(name+'.bin')
            stdout = _execute([java, '-XX:ActiveProcessorCount=4',
                               '-Djava.library.path='+str(library_path), '-cp', classes,
                               'com.hiro.ulike.ExactBaseline1959', dump,
                               str(label == 'candidate1959').lower()], out/(name+'.log'), 600)
            report = _checked(json.loads(stdout), name)
            if report.get('nativeAvailable') is not (backend == 'jni'):
                raise AssertionError('Differential backend did not execute requested native state: '+name)
            if (report.get('strongCases', 0) < 20 or report.get('pipelineCases', 0) < 16
                    or report.get('recordedFloatBits', 0) <= 1000
                    or report.get('recordedMaskBytes', 0) <= 1000):
                raise AssertionError('Exact output/map/evidence/mask coverage missing: '+name)
            if label == 'candidate1959' and report.get('committedHalfHandoffs', 0) < 1:
                raise AssertionError('Actual H45 committed half path not executed')
            reports[name] = report
            dumps[name] = dump
            if label == 'candidate1959':
                carry_name = 'pipeline-committed-half-'+backend
                carry = json.loads(_execute([java, '-XX:ActiveProcessorCount=4',
                    '-Djava.library.path='+str(library_path), '-cp', classes,
                    'com.hiro.ulike.PipelineCarry1959Test'], out/(carry_name+'.log')))
                carry = _checked(carry, carry_name)
                if (carry.get('h44_shared_nr13_confidence_exact') is not True
                        or carry.get('h45_committed_half_exact') is not True
                        or carry.get('h45_full_image_row_reads_removed',0) <= 0
                        or carry.get('h45_rgb565_committed_fallback') is not True
                        or carry.get('nativeAvailable') is not (backend == 'jni')):
                    raise AssertionError('Executed H44/H45 carry/policy exactness missing')
                reports[carry_name] = carry
                workspace_name = 'strong-preparation-workspace-'+backend
                workspace = _checked(json.loads(_execute([java, '-XX:ActiveProcessorCount=4',
                    '-Djava.library.path='+str(library_path), '-cp', classes,
                    'com.hiro.ulike.StrongWorkspace1959Test'], out/(workspace_name+'.log'))),workspace_name)
                if (workspace.get('parallelPreparationPeak',0) < 2
                        or any(workspace.get(flag) is not True for flag in
                            ('exclusiveWorkspaceChecked','drainedCancellationChecked',
                             'carriedHalfExactChecked','sharedConfidenceExactChecked'))):
                    raise AssertionError('Actual H40 parallel/H43 exclusive-workspace evidence missing')
                reports[workspace_name] = workspace
        if label == 'candidate1959':
            for order in ('forward', 'reverse'):
                name = 'native-memory-owners-'+order
                report = json.loads(_execute([java, '-XX:ActiveProcessorCount=4',
                    '-Djava.library.path='+str(out/'unavailable'), '-cp', classes,
                    'com.hiro.ulike.ScratchOwners1959Test', order], out/(name+'.log')))
                reports[name] = _checked(report, name)
        now = {str(p.relative_to(root)) if p.is_relative_to(root) else str(p.relative_to(ROOT)): _sha(p)
               for p in sources}
        if now != inputs or any(_sha(root/name) != value for name,value in native_inputs.items()):
            raise AssertionError('Differential production/compiler inputs changed during execution: '+label)
        if _sha(generated_bitmap) != generated[label]['generated_source_sha256']:
            raise AssertionError('Commit Bitmap fixture changed during differential execution')
    comparisons = []
    reference_data = dumps['published1958-java'].read_bytes()
    for name, dump in dumps.items():
        data = dump.read_bytes()
        if data != reference_data:
            limit = min(len(data),len(reference_data))
            first = next((i for i in range(limit) if data[i] != reference_data[i]), limit)
            raise AssertionError('Exact .58 binary output differs: '+name+' firstByte='+str(first)
                                 +' baselineBytes='+str(len(reference_data))+' actualBytes='+str(len(data)))
        comparisons.append({'name': name, 'sha256': _sha(dump), 'bytes': len(data),
                            'differing_bytes': 0})
    baseline_pins()
    result = {
        'status': 'passed', 'assertions': sum(r['assertions'] for r in reports.values()) + len(comparisons),
        'host_pixel_equivalence_to_baseline': True,
        'native_java_pixel_equivalence': True,
        'baseline_version': '1.9.58', 'baseline_mpp_sha256': BASELINE_MPP_SHA256,
        'baseline_source_commit': pins['baseline_commit'],
        'baseline_source_tree': pins['baseline_source_tree'],
        'baseline_frozen_pin_manifest_sha256': REFERENCE_PINS_SHA256,
        'compiled_inputs_sha256': all_inputs, 'fresh_native_libraries': builds,
        'compiled_sources_sha256': compiled_java,
        'compiled_native_sources_sha256': compiled_native,
        'generated_host_fixture_source_sha256': generated,
        'reports': reports, 'exact_binary_comparisons': comparisons,
        'coverage': ['half/quarter/eighth exact map pixels', 'noise/regional evidence float bits',
            'full/streamed/concurrent strong output', 'row stripe sizes 1/7/32/128/256',
            'odd/even dimensions and partial pyramids', 'NR1-NR13 final-save pixels',
            'NR13 cells and rotated/resized mask sampling', 'captured committed 2x2 half',
            'noise levels OFF/1/3/4 and shadow/chroma choices', 'alpha/RGB565/F16/wide-colour/gainmap gates',
            'copy/create/partial write rollback', 'cancellation transaction',
            'independent native memory owner registration, overflow and failure'],
        'physical_android_tested': False, 'device_speedup_verified': False,
    }
    (out/'differential-result.json').write_text(json.dumps(result,sort_keys=True,indent=2)+'\n')
    return result


def run(work, input=None, tools=None):
    """Formal build gate. All suites are executed freshly; ARM cannot be omitted."""
    import host_nr1958
    native_headers = {p.relative_to(ROOT).as_posix(): _sha(p)
                      for p in (ROOT/'native1958').glob('*.h')}
    inherited = _checked(host_nr1958.run(work, input, tools), 'inherited-nr1958')
    if any(_sha(ROOT/name) != value for name,value in native_headers.items()):
        raise AssertionError('Native headers changed during inherited fresh JNI execution')
    inherited['compiled_native_sources_sha256'].update(native_headers)
    exact = _checked(differential(work, input, tools), 'published1958-exact-differential')
    arm_module = importlib.import_module('native_arm1959')
    arm = _checked(arm_module.run(work, input, tools), 'actual-arm-neon')
    if (arm.get('arm_neon_pixel_equivalence') is not True
            or arm.get('arm_neon_actual_execution') is not True):
        raise AssertionError('Actual ARM NEON exact execution evidence absent')
    result = {'status': 'passed', 'selected': ['H40','H41','H42','H43','H44','H45'],
        'assertions': inherited['assertions']+exact['assertions']+arm['assertions'],
        'host_pixel_equivalence_to_baseline': True, 'pixel_equivalence_to_baseline': True,
        'native_java_pixel_equivalence': True, 'arm_neon_pixel_equivalence': True,
        'arm_neon_actual_execution': True, 'production_host_quality_changed': False,
        'physical_android_tested': False, 'device_speedup_verified': False,
        'device_quality_improvement_verified': False,
        'baseline_nr1958_result': inherited,
        'compiled_sources_sha256': {**inherited['compiled_sources_sha256'],
                                    **exact['compiled_sources_sha256']},
        'compiled_native_sources_sha256': {**inherited['compiled_native_sources_sha256'],
                                           **exact['compiled_native_sources_sha256'],
                                           **arm['source_pins']},
        'reports': {'inherited_nr1958': inherited,
                    'exact_published1958_differential': exact, 'actual_arm_neon': arm}}
    target = Path(work).resolve()/'host-speed1959/result.json'
    target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(json.dumps(result,sort_keys=True,indent=2)+'\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--work', '--out', required=True)
    parser.add_argument('--input')
    parser.add_argument('--tools')
    parser.add_argument('--output')
    parser.add_argument('--suite', choices=('formal','differential'), default='formal')
    args = parser.parse_args()
    result = (run if args.suite == 'formal' else differential)(args.work,args.input,args.tools)
    data = json.dumps(result,sort_keys=True,indent=2)+'\n'
    if args.output:
        Path(args.output).write_text(data)
    print(data)
