#!/usr/bin/env python3
"""Preserve the complete .90 CPU/JNI matrix and prove the .96 hint-only delta.

The performance data are software-GLES observations, never a Galaxy speed
admission.  Only an existing exact predicate can reject a partial NLM patch.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, subprocess

ROOT = Path(__file__).resolve().parent
REFERENCE_SHA = '6678a1bec6fb41ed757f9f8ad94ad1f5b478bcdfd8ad075bdd7d3cb6dbba18d1'


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def expected_shader(reference):
    candidate = reference
    for samples, factor in ((5, '3.2'), (9, '2.6')):
        before = f'    precise float variance=sy*sy+.22*sc*sc,h2=max(1.5,variance*{factor});\n'
        after = before + f'''#if GX_IEEE_DIV73
    // .96: this rounded threshold is only a cheap hint. It never rejects a
    // candidate: the unchanged exact predicate below must still confirm it.
    // A missed hint only defers rejection to the unchanged final predicate.
    precise float rejectionHint1996={samples}.0*(2.0*variance+5.0*h2);
#endif
'''
        if candidate.count(before) != 1:
            raise AssertionError('Published variance anchor changed')
        candidate = candidate.replace(before, after)
        before = f'            if(impossibleDistance(distance,{samples}.0,variance,h2)){{valid=false;break;}}\n'
        after = f'''#if GX_IEEE_DIV73
            if(distance>rejectionHint1996&&impossibleDistance(distance,{samples}.0,variance,h2)){{valid=false;break;}}
#else
''' + before + '#endif\n'
        if candidate.count(before) != 1:
            raise AssertionError('Published partial predicate anchor changed')
        candidate = candidate.replace(before, after)
    return candidate


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    candidate_path = source / 'native1960/strong1960.comp'
    reference_path = source / 'tests1996/shader/reference95/strong1960.comp'
    fixture = source / 'tests1996/shader/guard1996.py'
    tracked = [candidate_path, reference_path, fixture, Path(__file__)]
    pins = {str(p.relative_to(source)): sha(p) for p in tracked}
    reference, candidate = reference_path.read_text(), candidate_path.read_text()
    if sha(reference_path) != REFERENCE_SHA:
        raise AssertionError('Frozen published .95 Strong shader changed')
    expected = expected_shader(reference)
    if candidate != expected:
        raise AssertionError('Shader delta exceeds the two guarded partial predicates')
    # These controls must fail the same structural contract: dropping exact
    # confirmation, changing final cutoff, or modifying legacy arithmetic.
    negative_controls = [
        candidate.replace('&&impossibleDistance(distance,5.0,variance,h2)', '', 1),
        candidate.replace('if(z>5.0)continue;', 'if(z>5.1)continue;', 1),
        candidate.replace('distance/5.0-2.0*variance', 'distance/5.0-1.9*variance', 1),
    ]
    if len(negative_controls) != 3 or any(item == expected for item in negative_controls):
        raise AssertionError('Arithmetic change negative control did not fail')
    environment = dict(os.environ, EGL_PLATFORM='surfaceless', LIBGL_ALWAYS_SOFTWARE='1')
    commands = []

    def run(args, label, stdin=None):
        args = list(map(str, args))
        commands.append(dict(name=label, argv=args))
        process = subprocess.run(args, input=stdin, capture_output=True, text=True,
                                 env=environment, timeout=600)
        output = process.stdout + process.stderr
        (work / (label + '.log')).write_text(output)
        if process.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:
            raise RuntimeError(label + ' failed:\n' + output[-12000:])
        return output

    def preprocess(text, mode, width, tile, pow2):
        text = re.sub(r'^#(?:version|extension)[^\n]*\n', '', text, flags=re.M)
        command = ['cc', '-E', '-P', '-x', 'c', '-DGX_IEEE_DIV73=0',
                   '-DGX_STRONG_MODE=' + str(mode), '-DGX_LOCAL_SIZE=' + str(width),
                   '-DGX_TILE_WIDTH=' + str(tile), '-DGX_POW2_EXACT78=' + str(pow2), '-']
        result = subprocess.run(command, input=text, text=True, capture_output=True, timeout=30)
        if result.returncode:
            raise RuntimeError(result.stderr)
        return result.stdout

    legacy_programs = 0
    for mode in (-1, 0, 1, 2, 3):
        for width in (32, 64, 128):
            for tile in (0, 8):
                for pow2 in (0, 1):
                    if preprocess(reference, mode, width, tile, pow2) != preprocess(candidate, mode, width, tile, pow2):
                        raise AssertionError('Preprocessed legacy program changed')
                    legacy_programs += 1
    # This includes all existing CPU oracle cases, all exact native programs,
    # corrupted-oracle control, and published-.89 confidence-tail control.
    spec = importlib.util.spec_from_file_location('shader96_math90', source / 'host_gpu_math1990.py')
    base = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(base)
    math = base.test(source, work / 'math', jdk, ndk)
    jdk = Path(jdk or source.parent / 'tools/jdk-21.0.8+9').resolve()
    baseline = work / 'published95'
    baseline.mkdir(exist_ok=True)
    header = (work / 'math/shader_sources1960.h').read_text()

    def literal(text):
        return 'static const char strong1960_source[]=\n' + ''.join(json.dumps(line + '\n') + '\n' for line in text.splitlines()) + ';\n'

    if header.count(literal(candidate)) != 1:
        raise AssertionError('Actual compiled candidate header differs')
    header = header.replace(literal(candidate), literal(reference))
    header = re.sub(r'#define GPU1960_SOURCE_SHA256 "[0-9a-f]+"',
                    '#define GPU1960_SOURCE_SHA256 "' + hashlib.sha256(header.encode()).hexdigest() + '"', header)
    (baseline / 'shader_sources1960.h').write_text(header)
    run(['cc', '-std=c11', '-O3', '-shared', '-fPIC', '-DANDROID', '-fno-fast-math', '-ffp-contract=off',
         '-Wall', '-Wextra', '-Werror', '-Wno-misleading-indentation', '-I' + str(work / 'math/headers'),
         '-I' + str(jdk / 'include'), '-I' + str(jdk / 'include/linux'), '-I' + str(baseline),
         source / 'native1960/engine1960.c', '-l:libEGL.so.1', '-l:libGL.so.1', '-o',
         baseline / 'libulike_gpu1960.so'], 'published95-native-compile')
    output = run([jdk / 'bin/java', '-Xcheck:jni', '-Djava.library.path=' + str(baseline),
                  '-cp', work / 'math/classes', 'com.hiro.ulike.FuzzReplay1990', work / 'math/oracle.bin'],
                 'published95-replay')
    match = re.search(r'RESULT records=(\d+) reads=(\d+) pixelsDiff=(\d+) confidenceDiff=(\d+)', output)
    if not match or tuple(map(int, match.groups())) != (1016, 18240, 0, 0) or 'UNSUPPORTED ' in output:
        raise AssertionError('Published .95 baseline did not match the shared CPU oracle')
    import sys
    run([sys.executable, fixture, '--source', source, '--work', work / 'gles'], 'actual-gles-hint-and-cost')
    gles = json.loads((work / 'gles/result.json').read_text())
    if pins != {str(p.relative_to(source)): sha(p) for p in tracked}:
        raise AssertionError('Shader proof inputs changed during execution')
    # Keep the inherited fields at the top level for release evidence readers.
    result = dict(math)
    result.update(status='passed', assertions=math['assertions'] + 18240 + legacy_programs + 3 + gles['assertions'],
                  inherited_gpu_math90=math, shader96=True, actual_jni=True,
                  published95_records=1016, published95_reads=18240, published95_argb_differences=0,
                  published95_confidence_differences=0, legacy_preprocessor_programs=legacy_programs,
                  only_guarded_partial_predicates_changed=True, arithmetic_negative_controls=3,
                  focused_gles=gles, shader96_source_sha256=pins,
                  published95_shader_sha256=REFERENCE_SHA, candidate_shader_sha256=sha(candidate_path),
                  physical_android_tested=False, device_speedup_verified=False)
    (work / 'commands.json').write_text(json.dumps(commands, indent=2) + '\n')
    (work / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    return result


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--source', type=Path, default=ROOT)
    p.add_argument('--work', type=Path, required=True)
    p.add_argument('--jdk', type=Path)
    p.add_argument('--ndk', type=Path)
    a = p.parse_args()
    print(json.dumps(test(a.source, a.work, a.jdk, a.ndk), indent=2))
