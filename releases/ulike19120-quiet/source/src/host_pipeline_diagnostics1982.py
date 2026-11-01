#!/usr/bin/env python3
"""Selected-backend and resident terminal diagnostics, with explicit host transport.

The frozen .81 resident source reproduces the missing terminal notes. Actual
Single/model/geometry arithmetic is run in the current production closure;
controlled GPU readback is never represented as a device or shader benchmark.
"""
from collections import Counter
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

REQUIRED_FLAGS = (
    'resident_all_terminal_reasons1982',
    'pipeline_accepted_backend_counts1982',
    'pipeline_failure_not_gpu_accepted1982',
    'pipeline_background_trace_excluded1982',
    'pipeline_default_route_explicit1982',
    'pipeline_math_and_gates_preserved1982',
)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def run(command, log, timeout=180):
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True, timeout=timeout)
    Path(log).write_text(result.stdout + result.stderr)
    if result.returncode:
        raise AssertionError(str(log) + '\n' + (result.stdout + result.stderr)[-9000:])
    return result.stdout


def result(text):
    for line in reversed(text.splitlines()):
        if line.startswith('{'):
            value = json.loads(line)
            if value.get('status') != 'passed' or value.get('assertions', 0) <= 0:
                raise AssertionError('Failed or empty host result')
            return value
    raise AssertionError('No structured host result')


TOKEN = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[A-Za-z_$][\w$]*|\d+|[^\s]', re.S)


def methods(text):
    """Whitespace/comment independent method body contracts, including callbacks."""
    tokens = [m.group() for m in TOKEN.finditer(text) if not m.group().startswith(('//', '/*'))]
    braces, parens, close_brace, open_paren = [], [], {}, {}
    for index, token in enumerate(tokens):
        if token == '{':
            braces.append(index)
        elif token == '}':
            close_brace[braces.pop()] = index
        elif token == '(':
            parens.append(index)
        elif token == ')':
            open_paren[index] = parens.pop()
    found = []
    controls = {'if', 'for', 'while', 'switch', 'catch', 'synchronized', 'try'}
    for start, end in close_brace.items():
        if start == 0 or tokens[start - 1] != ')':
            continue
        opening = open_paren[start - 1]
        if opening == 0:
            continue
        name = tokens[opening - 1]
        if name in controls or not re.fullmatch(r'[A-Za-z_$][\w$]*', name):
            continue
        if opening > 1 and tokens[opening - 2] in {'.', 'new'}:
            continue
        body = '\0'.join(tokens[start:end + 1])
        found.append({'name': name, 'sha256': hashlib.sha256(body.encode()).hexdigest()})
    return found


def erase_markers(text):
    # The primitive workspace marker has no image or gate semantics.
    text = re.sub(r'if\(workspace!=null\)workspace\.selectedBackend1982=[01];', '', text)
    text = re.sub(r'workspace\.selectedBackend1982=-?[01];', '', text)
    text = re.sub(r'if\(selected1982!=null\)selected1982\[0\]=[01];', '', text)
    return text


def preservation(source):
    declaration = json.loads((source / 'tests1982/pipeline/preserved81.json').read_text())
    if declaration['baseline_version'] != '1.9.81':
        raise AssertionError('Wrong preservation baseline')
    checked = 0
    for name, expected in declaration['complete_files'].items():
        if sha(source / name) != expected:
            raise AssertionError('Unchanged numeric file changed: ' + name)
        checked += 1
    for name, expected in declaration['methods'].items():
        observed = Counter((m['name'], m['sha256']) for m in methods(erase_markers((source / name).read_text())))
        wanted = Counter((m['name'], m['sha256']) for m in expected)
        missing = wanted - observed
        if missing:
            raise AssertionError('Preserved math/gate method changed: ' + name + ' ' + str(list(missing)[:8]))
        checked += len(expected)
    if checked < 200:
        raise AssertionError('Incomplete mathematical/source contract')
    return checked


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
    jar = Path(os.environ.get('ULIKE_ANDROID_JAR', jdk.parent / 'android.jar')).resolve()
    tests = source / 'tests1982/pipeline'
    proof = json.loads((tests / 'baseline81/pins.json').read_text())
    if proof['baseline_version'] != '1.9.81':
        raise AssertionError('Wrong resident baseline')
    for name, pin in proof['files'].items():
        path = tests / 'baseline81' / name
        if sha(path) != pin['sha256'] or path.stat().st_size != pin['bytes']:
            raise AssertionError('Frozen .81 resident source changed')
    before = {str(p.relative_to(source)): sha(p) for p in source.glob('*.java')}
    preserved = preservation(source)
    reports = {}
    for mode in ('current', 'baseline'):
        classes = work / (mode + '-resident')
        classes.mkdir(exist_ok=True)
        resident = source / 'GpuResident1976.java' if mode == 'current' else tests / 'baseline81/GpuResident1976.java'
        files = [resident, source / 'ResidentProof1978.java', source / 'GpuSnapshotBudget1981.java',
                 source / 'PipelineDiagnostics1982.java', tests / 'android/graphics/Bitmap.java',
                 tests / 'ResidentPeers1982.java', tests / 'ResidentDiagnostics1982Test.java']
        run([jdk / 'bin/javac', '--release', '8', '-Xlint:-options', '-d', classes, *files], work / (mode + '-resident-compile.log'))
        reports[mode + '_resident'] = result(run([jdk / 'bin/java', '-cp', classes,
            'com.hiro.ulike.ResidentDiagnostics1982Test', mode], work / (mode + '-resident.log')))
    sys.path.insert(0, str(source))
    builder = module('pipeline82_builder', source / 'build1982.py')
    inputs = list(builder.compile_inputs().values()) + [builder.production_path(n) for n in builder.PRODUCTION]
    classes, fixtures = work / 'production-classes', work / 'fixture-classes'
    classes.mkdir(exist_ok=True)
    fixtures.mkdir(exist_ok=True)
    run([jdk / 'bin/javac', '-source', '8', '-target', '8', '-Xlint:-options', '-encoding', 'UTF-8',
         '-bootclasspath', jar, '-d', classes, *inputs], work / 'production-compile.log')
    queue = module('pipeline82_android_fixtures', source / 'host_qualification1967.py')
    generated = []
    for name, body in queue.FIXTURES.items():
        if name.startswith('android/'):
            path = work / 'generated' / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(body)
            generated.append(path)
    generated += [source / 'tests/pipeline1942-fixtures/android/graphics/Bitmap.java',
                  source / 'tests/pipeline1942-fixtures/android/graphics/ColorSpace.java',
                  source / 'tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java',
                  tests / 'BackendTransport1982.java', tests / 'BackendDiagnostics1982Test.java']
    cp = str(classes) + os.pathsep + str(jar)
    run([jdk / 'bin/javac', '--release', '8', '-Xlint:-options', '-encoding', 'UTF-8', '-cp', cp,
         '-d', fixtures, *generated], work / 'backend-fixture-compile.log')
    library = work / 'libresidual1982_test.so'
    run(['cc', '-O3', '-std=c11', '-Wall', '-Wextra', '-Werror', '-ffp-contract=off', '-fno-fast-math',
         '-fPIC', '-shared', '-Wl,--no-undefined', '-I' + str(jdk / 'include'), '-I' + str(jdk / 'include/linux'),
         source / 'native1960/residual_blocks1961.c', '-lm', '-pthread', '-o', library], work / 'residual-native-compile.log')
    modes = ('single-gpu', 'single-null', 'single-link', 'residual-gpu', 'residual-null', 'residual-link',
             'worker', 'worker-background', 'worker-oracle', 'worker-write-failure',
             'geometry-cpu', 'geometry-gpu', 'geometry-failure',
             'regions-cpu', 'regions-gpu', 'regions-failure', 'model', 'defaults')
    for mode in modes:
        reports[mode] = result(run([jdk / 'bin/java', '-Xmx768m', '-cp', str(fixtures) + os.pathsep + cp,
            'com.hiro.ulike.BackendDiagnostics1982Test', mode, library], work / (mode + '.log')))
    after = {str(p.relative_to(source)): sha(p) for p in source.glob('*.java')}
    if before != after:
        raise AssertionError('Production sources changed while diagnostics tests ran')
    report = dict(status='passed', assertions=preserved + sum(v['assertions'] for v in reports.values()),
                  cases=reports, preserved_math_gate_methods_and_files=preserved,
                  source_hashes=before, physical_android_tested=False, device_tested=False,
                  gpu_transport_controlled=True, device_speedup_verified=False)
    report.update({flag: True for flag in REQUIRED_FLAGS})
    (work / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), indent=2))
