#!/usr/bin/env python3
"""Execute unchanged C compiler/topology bodies and compare frozen .77 GLSL.

The GL transport is controlled: this is a mapping, source and preprocessing
regression, not GPU execution or a physical-device performance measurement.
Real shaders/JNI are independently exercised by host_gpu_programs1978.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import re
import shutil
import subprocess

PINS_SHA256 = 'f649194d3438ac3f915bf7ea4c15e996fe7a115bb5caffa5356b5c6f3fdfc980'
NAMES = ('strong1960', 'single1960', 'analysis1960', 'geometry1960',
         'analysis1961', 'residual1961', 'protection1961', 'compare1961', 'finish1961')
TOKENS = re.compile(r'0[xX][0-9a-fA-F]+[uUlL]*|(?:\d+\.\d*|\.\d+|\d+)(?:[eE][+-]?\d+)?[fFuUlL]*|'
                    r'[A-Za-z_]\w*|<<=|>>=|\+\+|--|&&|\|\||==|!=|<=|>=|<<|>>|[+\-*/%&|^]=|[^\s]')


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def between(text, start, end):
    if text.count(start) != 1 or text.count(end) != 1:
        raise AssertionError('Production inspection boundary changed: ' + start)
    return text.split(start, 1)[1].split(end, 1)[0]


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    fixtures = source / 'gpu78-fixtures/program-preservation1978'
    reference = fixtures / 'published1977-reference'
    native = source / 'native1960'
    compiler = shutil.which('cc')
    if not compiler:
        raise RuntimeError('Host C compiler is required for real engine mapping execution')
    assertions = 0

    def check(condition, message):
        nonlocal assertions
        if not condition:
            raise AssertionError(message)
        assertions += 1

    check(sha(fixtures / 'baseline-pins.json') == PINS_SHA256, 'Frozen .77 baseline pin manifest changed')
    baseline = json.loads((fixtures / 'baseline-pins.json').read_text())
    for name, pin in baseline['files'].items():
        check(sha(reference / name) == pin['sha256'] and (reference / name).stat().st_size == pin['bytes'],
              'Frozen published .77 source changed: ' + name)
    tracked = [native / 'engine1960.c', native / 'build_native1960.py']
    tracked += [native / (name + '.comp') for name in NAMES]
    tracked += sorted(p for p in fixtures.rglob('*') if p.is_file() and '__pycache__' not in p.parts)
    tracked += [source / 'host_program_preservation1978.py']
    pins = {str(p.relative_to(source)): sha(p) for p in tracked}
    commands = []

    def run(command, label, stdin=None):
        command = list(map(str, command))
        commands.append({'label': label, 'argv': command})
        result = subprocess.run(command, input=stdin, text=True, capture_output=True, timeout=60)
        (work / (label + '.log')).write_text(result.stdout + result.stderr)
        if result.returncode:
            raise RuntimeError(label + '\n' + result.stdout + result.stderr)
        return result.stdout

    def capture(tree, label):
        out = work / label
        out.mkdir(exist_ok=True)
        engine = (tree / 'engine1960.c').read_text()
        declarations = '#define BASE_PROGRAMS' + between(engine, '#define BASE_PROGRAMS', '\ntypedef struct')
        body = 'static int program(int id){' + between(engine, 'static int program(int id){', '\nstatic int active(')
        dispatch = between(engine, 'JNIEXPORT jboolean JNICALL JNI1960(dispatchNative)(', '\nstatic jintArray read_one(')
        guards = ' if(id>=STRONG_PROGRAM_BASE' + between(dispatch, ' if(id>=STRONG_PROGRAM_BASE', '\n for(int b=0;b<nb;b++)')
        topology = ' uint64_t local=' + between(dispatch, ' uint64_t local=', '\n}')
        template = (fixtures / 'compiler_harness1978.c').read_text()
        for marker, replacement in (('@DECLARATIONS@', declarations), ('@PROGRAM_BODY@', body),
                                    ('@MODE_GUARDS@', guards), ('@DISPATCH_BODY@', topology)):
            check(template.count(marker) == 1, 'Harness insertion boundary missing')
            template = template.replace(marker, replacement)
        cfile = out / 'compiler_harness.c'
        cfile.write_text(template)
        spec = importlib.util.spec_from_file_location('preservation_header_' + label, tree / 'build_native1960.py')
        builder = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(builder)
        check(tuple(builder.NAMES) == NAMES, 'Builder source order changed')
        builder.shader_header(tree, out)
        run([compiler, '-std=c11', '-O0', '-Wall', '-Wextra', '-Werror', '-Wno-misleading-indentation',
             '-I' + str(out), cfile, '-o', out / 'compiler_harness'], label + '-compile')
        rows = []
        for line in run([out / 'compiler_harness', out], label + '-mapping').splitlines():
            fields = line.split('\t')
            check(len(fields) == 9, 'Malformed executed engine mapping')
            rows.append({'id': int(fields[0]), 'source': fields[1], **dict(zip(
                ('base', 'local', 'mode', 'tile', 'ieee', 'pow2', 'workgroup'), map(int, fields[2:])))} )
        launches = {}
        for line in (out / 'dispatch.tsv').read_text().splitlines():
            fields = line.split('\t')
            identity = tuple(map(int, fields[:5]))
            check(identity not in launches, 'Duplicate dispatch observation')
            launches[identity] = [tuple(map(int, item.split(':'))) for item in fields[5:]]
        return rows, launches, out

    old, old_launches, old_out = capture(reference, 'reference77')
    new, new_launches, new_out = capture(native, 'current78')
    check([r['id'] for r in old] == list(range(45)), 'Published baseline does not expose exactly programs 0..44')
    check([r['id'] for r in new] == list(range(51)), 'Current engine does not expose exactly programs 0..50')
    for program in range(45):
        check(old[program] == new[program], 'Legacy program mapping/defines/workgroup changed: ' + str(program))
    for identity, value in old_launches.items():
        check(new_launches.get(identity) == value, 'Legacy dispatch groups or lane offsets changed: ' + str(identity))
    for row in new[45:]:
        program = row['id']
        expected = dict(id=program, source='strong1960', base=0, local=(64, 32, 128)[(program - 45) % 3],
                        mode=3, tile=8 if program >= 48 else 0, ieee=1, pow2=1,
                        workgroup=(64, 32, 128)[(program - 45) % 3])
        check(row == expected, 'New power-of-two program mapping changed: ' + str(program))
        for identity, launches in new_launches.items():
            if identity[0] != program:
                continue
            _, width, rows, begin, mode = identity
            local = row['local']
            groups = ((width + 7) // 8) * ((rows + local // 8 - 1) // (local // 8)) if row['tile'] else (width * rows + local - 1) // local
            expected_launches = [(min(3, groups - first), first * local) for first in range(0, groups, 3)]
            check(launches == expected_launches and mode == 3, 'New program launch topology changed: ' + str(identity))

    for name in NAMES[1:]:
        check((native / (name + '.comp')).read_bytes() == (reference / (name + '.comp')).read_bytes(),
              'Legacy non-Strong shader changed: ' + name)

    expanded = {}
    for program in range(45):
        records = []
        for label, out in (('reference77', old_out), ('current78', new_out)):
            glsl = (out / ('program-%02d.glsl' % program)).read_text()
            directives = re.findall(r'^\s*#(?:version|extension)\b[^\n]*', glsl, re.M)
            # Only GLSL-specific directives are removed from C preprocessing;
            # their exact strings are compared independently below.
            preprocessing = re.sub(r'^\s*#(?:version|extension)\b[^\n]*', '', glsl, flags=re.M)
            text = run([compiler, '-E', '-P', '-undef', '-nostdinc', '-x', 'c', '-'],
                       label + '-preprocess-%02d' % program, preprocessing)
            tokens = TOKENS.findall(text)
            check(bool(tokens), 'Empty preprocessed shader')
            records.append((directives, tokens))
            expanded[label + '/%02d' % program] = hashlib.sha256('\n'.join(tokens).encode()).hexdigest()
        check(records[0] == records[1], 'Old GLSL token stream or required extensions changed: ' + str(program))
    check(pins == {str(p.relative_to(source)): sha(p) for p in tracked}, 'Inputs changed during program-preservation check')
    report = dict(status='passed', assertions=assertions, physical_android_tested=False,
                  device_speedup_verified=False, actual_jni=False, controlled_gl_compiler_transport=True,
                  original_engine_compiler_and_dispatch_executed=True,
                  programs0_44_preserved1978_verified=True, programs45_50_mapping1978_verified=True,
                  strong_macro0_preprocessed_tokens_identical=True, other_eight_shader_bytes_identical=True,
                  legacy_program_count=45, new_program_count=6, legacy_dispatch_cases=len(old_launches),
                  baseline=baseline, source_sha256=pins, mapping_reference=old, mapping_current=new,
                  preprocessed_token_sha256=expanded)
    (work / 'commands.json').write_text(json.dumps(commands, indent=2) + '\n')
    (work / 'result.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), indent=2))
