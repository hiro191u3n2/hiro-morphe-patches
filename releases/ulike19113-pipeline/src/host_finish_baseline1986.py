#!/usr/bin/env python3
"""Deterministic finish admission regression against the published .85 source.

Production qualification, the bounded CPU stability check and ResidentProof
execute. Four complete renderer entry points and the qualification clock are
explicit generated test adapters. They do not assert native-pixel correctness
or Android speed; the retained native Chain/Resident suites cover the unchanged
renderers. No renderer, shader or historical test is edited by this runner.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import re
import subprocess
import sys

BASELINE85_SHA256 = '8996fd645a3cc7ec6e75b34124cc30f7ce74c9fe1769bae70cb6d66fac81f9ac'
RENDER_METHODS = ('cpuFinishOwned1962', 'compareFinish1978', 'runFinish1976', 'runLegacy1976')
REQUIRED_FLAGS = (
    'finish_published85_missing_legacy_negative1986',
    'finish_cpu_fallback_two_complete_proofs1986',
    'finish_actual_output_timing_boundaries1986',
    'finish_existing_legacy_gate1986_preserved',
    'finish_independent_candidate_failures1986',
    'finish_cpu_reference_stability1986',
    'finish_cancellation_and_ownership1986',
    'finish_exact_history1986_preserved',
    'finish_optional_diagnostics1986_noninterference',
    'finish_resident_baseline_handoff1986',
    'finish_pixel_render_methods1986_preserved',
    'finish_reviewed_source_inverse1986',
)


def sha(value):
    return hashlib.sha256(value if isinstance(value, bytes) else Path(value).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def method_span(body, name):
    pattern = re.compile(r'(?m)^    (?:[\w.<>\[\]]+ +)+' + re.escape(name) + r'\([^;]*?\)\s*\{')
    matches = list(pattern.finditer(body))
    if len(matches) != 1:
        raise AssertionError('A single reviewed method is required: ' + name)
    start, pos = matches[0].start(), matches[0].end() - 1
    depth, state = 0, 'code'
    i = pos
    while i < len(body):
        ch, two = body[i], body[i:i + 2]
        if state == 'line':
            if ch == '\n': state = 'code'
        elif state == 'block':
            if two == '*/': state = 'code'; i += 1
        elif state in ('string', 'char'):
            if ch == '\\': i += 1
            elif ch == ('"' if state == 'string' else "'"): state = 'code'
        elif two == '//': state = 'line'; i += 1
        elif two == '/*': state = 'block'; i += 1
        elif ch == '"': state = 'string'
        elif ch == "'": state = 'char'
        elif ch == '{': depth += 1
        elif ch == '}':
            depth -= 1
            if depth == 0: return start, pos, i + 1
        i += 1
    raise AssertionError('Unterminated reviewed method: ' + name)


def method_body(body, name):
    start, _, end = method_span(body, name)
    return body[start:end]


def adapt_controlled_renderers(body):
    replacements = {
        'cpuFinishOwned1962': 'return FinishControl1986.cpu(corrected,rotation,width,height,outputPlan,refreshNoise);',
        'compareFinish1978': 'return FinishControl1986.compare(source,rotation,width,height,plan,refresh,expected,tileRows,pipeline,carried,identityNoiseBefore,cancellation);',
        'runFinish1976': 'return FinishControl1986.output(source,rotation,width,height,outputPlan,refreshNoise,true,tileRows);',
        'runLegacy1976': 'return FinishControl1986.output(source,rotation,width,height,plan,refresh,false,32);',
    }
    original_qualification = method_body(body, 'qualifyFinish1978')
    spans = {}
    for name, replacement in replacements.items():
        start, brace, end = method_span(body, name)
        spans[name] = sha(body[start:end].encode())
        body = body[:brace + 1] + '\n        ' + replacement + '\n    }' + body[end:]
    start, brace, end = method_span(body, 'qualifyFinish1978')
    qualify = body[start:end]
    if qualify != original_qualification:
        raise AssertionError('Renderer adapters changed qualification control flow')
    calls = qualify.count('System.nanoTime()')
    if calls not in (4, 6):
        raise AssertionError('Unexpected measured-timer boundary count: ' + str(calls))
    adapted = qualify.replace('System.nanoTime()', 'FinishControl1986.nanoTime()')
    if adapted.replace('FinishControl1986.nanoTime()', 'System.nanoTime()') != original_qualification:
        raise AssertionError('The qualification adapter may change only the elapsed clock owner')
    body = body[:start] + adapted + body[end:]
    return body, dict(renderer_methods=spans, clock_calls=calls,
                      qualification_control_sha256=sha(original_qualification.encode()))


def inverse_chain1986(current, baseline):
    """Revert only reviewed .86 admission spans, then require the whole .85 file."""
    if sha(baseline) != BASELINE85_SHA256:
        raise AssertionError('Published .85 whole-chain baseline changed')
    current, baseline = current.decode(), baseline.decode()
    pins_path = Path(__file__).resolve().parent / 'tests1986/finish_baseline/reviewed-spans.json'
    pins = json.loads(pins_path.read_text())
    start, _, end = method_span(current, 'qualifyFinish1978')
    old_start, _, old_end = method_span(baseline, 'qualifyFinish1978')
    legacy_start, _, _ = method_span(current, 'legacyKey1976')
    original_between = baseline[old_end:method_span(baseline, 'legacyKey1976')[0]]
    if sha(current[start:end].encode()) != pins['qualifyFinish1978_sha256']:
        raise AssertionError('Reviewed .86 qualification method changed')
    if sha(current[end:legacy_start].encode()) != pins['added_helpers_span_sha256']:
        raise AssertionError('Reviewed .86 helper span changed')
    restored = (current[:start] + baseline[old_start:old_end] + original_between + current[legacy_start:]).encode()
    if restored != baseline.encode() or sha(restored) != BASELINE85_SHA256:
        raise AssertionError('Exact .86 inverse leaves an unreviewed change in GpuChain')
    return restored


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    jar = Path(os.environ.get('ULIKE_ANDROID_JAR', str(jdk.parent / 'android.jar'))).resolve()
    local = source / 'tests1986/finish_baseline'
    baseline = local / 'baseline85/GpuChain1961.java'
    if sha(baseline) != BASELINE85_SHA256:
        raise AssertionError('Frozen published .85 chain changed')
    restored = inverse_chain1986((source / 'GpuChain1961.java').read_bytes(), baseline.read_bytes())
    sys.path.insert(0, str(source))
    builder = module('finish86_compile_closure', source / 'build1985.py')
    inputs = list(dict.fromkeys(list(builder.compile_inputs().values()) + [builder.production_path(n) for n in builder.PRODUCTION]))
    fixtures = [source / name for name in (
        'tests/pipeline1942-fixtures/android/graphics/Bitmap.java',
        'tests/pipeline1942-fixtures/android/graphics/ColorSpace.java',
        'tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java')]
    fixtures += [local / 'FinishControl1986.java', local / 'FinishBaseline1986Test.java']
    tracked = list(dict.fromkeys(inputs + fixtures + [local / 'FinishPeers1986.java', baseline,
        local / 'reviewed-spans.json', Path(__file__).resolve()]))
    before = {str(path): sha(path) for path in tracked}
    commands = []

    def run(label, command):
        command = list(map(str, command)); commands.append(dict(label=label, argv=command))
        result = subprocess.run(command, text=True, capture_output=True, timeout=240)
        (work / (label + '.log')).write_text(result.stdout + result.stderr)
        if result.returncode:
            raise AssertionError(label + '\n' + (result.stdout + result.stderr)[-15000:])
        return result.stdout

    production, android, peers = work / 'production', work / 'fixtures', work / 'peers'
    for path in (production, android, peers): path.mkdir(exist_ok=True)
    run('production-compile', [jdk / 'bin/javac', '-source', '8', '-target', '8', '-Xlint:-options',
        '-encoding', 'UTF-8', '-bootclasspath', jar, '-d', production, *inputs])
    cp = os.pathsep.join(map(str, (production, jar)))
    run('fixture-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', cp, '-d', android, *fixtures])
    run('peer-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', str(android) + os.pathsep + cp,
        '-d', peers, local / 'FinishPeers1986.java'])
    reports, adapters = {}, {}
    current_text, baseline_text = (source / 'GpuChain1961.java').read_text(), baseline.read_text()
    preserved = RENDER_METHODS + ('chooseFinish1976', 'win', 'finishKey1976', 'residentBaseline1978',
        'benchmarkFinish1978', 'compareBenchmark1978')
    for name in preserved:
        if method_body(current_text, name) != method_body(baseline_text, name):
            raise AssertionError('Preserved renderer, reference or key method changed: ' + name)
    for mode, body in (('published85', baseline_text), ('current86', current_text)):
        adapted, evidence = adapt_controlled_renderers(body)
        directory = work / mode; directory.mkdir(exist_ok=True)
        path = directory / 'GpuChain1961.java'; path.write_text(adapted)
        run(mode + '-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8',
            '-cp', os.pathsep.join(map(str, (production, android, jar))), '-d', directory, path])
        runtime = os.pathsep.join(map(str, (peers, directory, android, production, jar)))
        output = run(mode + '-run', [jdk / 'bin/java', '-ea', '-Xmx768m', '-cp', runtime,
            'com.hiro.ulike.FinishBaseline1986Test', mode])
        report = json.loads(output.strip().splitlines()[-1])
        if report.get('status') != 'passed' or report.get('assertions', 0) <= 0:
            raise AssertionError('Executed passing finish assertions absent')
        reports[mode] = report; adapters[mode] = dict(**evidence, adapted_source_sha256=sha(path))
    if before != {str(path): sha(path) for path in tracked}:
        raise AssertionError('Source closure changed during the finish regression')
    result = dict(status='passed', assertions=sum(r['assertions'] for r in reports.values()), cases=reports,
        **{flag: True for flag in REQUIRED_FLAGS}, source_hashes=before, baseline85_source_sha256=BASELINE85_SHA256,
        controlled_adapter=adapters, preserved_methods=list(preserved),
        reviewed_chain_inverse_sha256=sha(restored), reviewed_spans_sha256=sha(local / 'reviewed-spans.json'),
        physical_android_tested=False, device_speedup_verified=False, actual_gpu_transport_executed=False,
        actual_production_resident_proof_executed=True, actual_production_qualification_control_executed=True,
        actual_cpu_renderer_executed=False,
        fixture_scope='Four rendering entry bodies and qualification elapsed-clock owner are controlled only in generated test copies. All qualified outputs traverse production ResidentProof rows; actual renderer bytecode/native pixels remain required by the retained full build gates.')
    (work / 'report.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    (work / 'commands.json').write_text(json.dumps(commands, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True); parser.add_argument('--work', required=True)
    parser.add_argument('--jdk'); parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
