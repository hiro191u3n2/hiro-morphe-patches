#!/usr/bin/env python3
"""Execute the actual .88 detail facade and photo storage with controlled peers.

Android UI/storage and background-state providers are fixtures. The production
ProcessingTiming1947 and PipelineDetail1988 classes are compiled unchanged.
No native algorithm, GPU speed or physical Android execution is claimed.
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

PINS = {
    'host_timing1982.py': '2e8653812cfefd2c64cf0954c9b728bd772101d131b7727aa730f14872c963d8',
    'host_timing1984.py': '94a2d39ae4d6c09ac036179f5d9cfb0f005438f7930dbe1311b4c00b11e512ce',
    'host_timing1985.py': '42e05ea41392b2e4f7d1ed31809668c1442b4b61ce84e5a7316956114d8d657e',
    'host_timing1986.py': '2562a0f0f25a056439c13158e48e1c2169c1f33183c2c1bbc9d4975ec81334ab',
    'Timing1982Test.java': '2f52053b21fc738a5bd522ce91a60ecc314ca212326b162c37b76c7d3facf9a7',
    'tests1988/baseline87/ProcessingTiming1947.java': 'bccdf159231116f912228ddeb8f4560e2848b9da0264e7a377daff9a23fdb46b',
    'tests1988/pipeline_detail/compile_only/PipelineDetail1988.java': '7aa6245751c4dd7b51f44c4cfc2c992b6f712c9145f95c43224199b6baa836fd',
}
REVIEWED_ADDITIONS1988 = (
    '5a34403431a6858d4e866cf2571dce8c2c39a189827e030ba64de68bf1da01f3',
    '04da0e26ed3aa01fded14d46049f042036e74cb7622ba8e1dff31f3d7b6ffe21',
    '272c329f437814a002a91a9f03100b2c2760b8f366502ef30c80fc47b2bc9e48',
    'eb343dabd85914911a7c71fed0c8c85ec134c1b09a156a1ac7459f0672bb5684',
)
REQUIRED_FLAGS = (
    'pipeline_detail1988_actual_trace_storage_verified',
    'pipeline_detail1988_owner_and_background_isolation_verified',
    'pipeline_detail1988_terminal_persistence_verified',
    'pipeline_detail1988_parallel_work_and_wall_verified',
    'pipeline_detail1988_optional_failure_noninterference_verified',
    'pipeline_detail1988_scalar_bounds_and_unknowns_verified',
    'pipeline_detail1988_snapshot_not_adoption_verified',
    'pipeline_detail1988_no_hot_allocation_or_publication_verified',
    'pipeline_detail1988_legacy_timing_scope_preserved',
)
EXPECTED_CASES = {
    'explicit_trace_identity_and_current_scope',
    'selected_backends_and_unknown_semantics',
    'bounded_reasons_and_invalid_inputs',
    'snapshot_outcomes_are_not_gpu_adoption',
    'unmeasured_zero_and_scalar_saturation',
    'all_background_providers_and_optional_failures',
    'damaged_optional_storage_preserves_legacy_summary',
    'parallel_multiple_owner_exact_totals',
    'success_failure_immutable_persistence_and_restore',
    'late_worker_terminal_race',
    'no_operation_ui_storage_or_summary_provider',
    'parallel_work_and_wall_are_distinct',
}


def sha(value):
    return hashlib.sha256(value if isinstance(value, bytes) else Path(value).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec);spec.loader.exec_module(value)
    return value


def once(text, old, new):
    if text.count(old) != 1:
        raise AssertionError('Expected exactly one reviewed anchor: ' + old[:100])
    return text.replace(old, new)


def reviewed_timing87_source1988(text):
    """Preservation-only inverse; executable tests always use the .88 source."""
    original = text
    bounds = (
        ('    // .88 fixed IDs mirror PipelineDetail1988 without adding a provider to rendering.\n',
         '    private static final String[] STRONG_TRIALS_1971='),
        ('        // Fixed primitive storage is allocated only with its capture owner.\n',
         '        Trace(long value,long wall,long monotonic)'),
        ('    /** Scalar-only sink. Foreground exclusion belongs to PipelineDetail1988;\n',
         '    /** Main noise-stage wall durations, separately from summed GPU worker work.'),
        ('    /** Rendering runs only at existing publication/read points and queries no\n',
         '    private static String backendDuration1982('),
    )
    for (begin, end), expected in zip(bounds, REVIEWED_ADDITIONS1988):
        if text.count(begin) != 1 or text.count(end) != 1:
            raise AssertionError('A reviewed .88 timing addition boundary changed')
        start = text.index(begin);finish = text.index(end, start)
        if sha(text[start:finish].encode()) != expected:
            raise AssertionError('A reviewed .88 timing addition changed after review')
        text = text[:start] + text[finish:]
    call = ('            int pipelineStart1988=text.length();\n'
            '            try{appendPipelineDetail1988(text,trace);}catch(Throwable optional){text.setLength(pipelineStart1988);}\n')
    text = once(text, call, '')
    text = once(text, '    public static final String VERSION = "1.9.88";',
                '    public static final String VERSION = "1.9.87";')
    if sha(text.encode()) != PINS['tests1988/baseline87/ProcessingTiming1947.java']:
        raise AssertionError('An inherited timing method changed outside the reviewed .88 additions')
    if original == text:
        raise AssertionError('The current .88 timing additions were not present')
    return text


def run(command, log):
    value = subprocess.run(list(map(str, command)), capture_output=True, text=True, timeout=90)
    Path(log).write_text(value.stdout + value.stderr)
    if value.returncode:
        raise AssertionError(str(log) + '\n' + value.stdout + value.stderr)
    return value.stdout


def method_code(bytecode, name):
    found = re.search(r'^  (?:public |private |protected |static |final )*[^\n]*\b' + re.escape(name)
                      + r'\([^\n]*\);\n(.*?)(?=\n  (?:public |private |protected |static |final )|\n\})',
                      bytecode, re.M | re.S)
    if not found:
        raise AssertionError('Compiled method absent: ' + name)
    return found.group(1)


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    if source.is_file():source = source.parent
    work.mkdir(parents=True, exist_ok=True)
    for name, expected in PINS.items():
        if sha(source / name) != expected:raise AssertionError('Frozen input changed: ' + name)
    inverse = reviewed_timing87_source1988((source / 'ProcessingTiming1947.java').read_text())
    facade_source = (source / 'PipelineDetail1988.java').read_text()
    shim_source = (source / 'tests1988/pipeline_detail/compile_only/PipelineDetail1988.java').read_text()
    constants = lambda body: re.findall(r'    static final int [^;]+;', body)
    if constants(facade_source) != constants(shim_source):
        raise AssertionError('Compiler-only historical bridge no longer matches actual phase/reason/event IDs')
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent).resolve()
    retained = module('detail88_timing82_fixtures', source / 'host_timing1982.py')
    peers84 = module('detail88_peers84', source / 'host_timing1984.py')
    peers85 = module('detail88_peers85', source / 'host_timing1985.py')
    peers86 = module('detail88_peers86', source / 'host_timing1986.py')
    paths = []
    for name, body in retained.STUBS.items():
        body = peers86.adapt_timing_peers1986(peers85.adapt_timing_peers1985(peers84.adapt_timing_peers1984(body)))
        anchor = 'final class GpuQualification1961 {'
        if anchor in body:
            body = once(body, anchor, anchor + '\n static boolean background(){return PipelineProviders1988.read(0);}\n')
        path = work / 'fixtures' / name;path.parent.mkdir(parents=True, exist_ok=True);path.write_text(body);paths.append(path)
    original = (source / 'Timing1982Test.java').read_text()
    if original.count('1.9.82') != 2:raise AssertionError('Historical helper version expectations changed')
    fixture = work / 'fixtures/com/hiro/ulike/Timing1982Test.java'
    fixture.write_text(original.replace('1.9.82', '1.9.88'));paths.append(fixture)
    harness = source / 'tests1988/pipeline_detail/PipelineDetail1988Test.java'
    providers = source / 'tests1988/pipeline_detail/PipelineProviders1988.java'
    production = [source / 'ProcessingTiming1947.java', source / 'PipelineDetail1988.java']
    tracked = production + [harness, providers, Path(__file__).resolve()] + [source / name for name in PINS]
    before = {str(path): sha(path) for path in tracked}
    classes = work / 'classes';classes.mkdir(exist_ok=True)
    run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-d', classes,
         *production, harness, providers, *paths], work / 'compile.log')
    output = run([jdk / 'bin/java', '-ea', '-XX:ActiveProcessorCount=4', '-cp', classes,
                  'com.hiro.ulike.PipelineDetail1988Test'], work / 'run.log')
    result = peers85.parsed(output)
    if set(result.get('tests', [])) != EXPECTED_CASES:raise AssertionError('A required executed .88 detail case is missing')
    bytecode = run([jdk / 'bin/javap', '-classpath', classes, '-c', '-p',
                    'com.hiro.ulike.ProcessingTiming1947', 'com.hiro.ulike.PipelineDetail1988'], work / 'bytecode.log')
    facade_marker = 'final class com.hiro.ulike.PipelineDetail1988 {'
    if bytecode.count(facade_marker) != 1:raise AssertionError('Actual facade bytecode boundary is missing')
    boundary = bytecode.index(facade_marker)
    timing_bytecode, facade_bytecode = bytecode[:boundary], bytecode[boundary:]
    verified = []
    for name in ('pipelineDetail1988', 'pipelineCopy1988'):
        body = method_code(timing_bytecode, name)
        if 'monitorenter' not in body:raise AssertionError('Owner monitor missing from scalar sink: ' + name)
        if re.search(r'^\s*\d+:\s+(?:new|anewarray|newarray|multianewarray|invokedynamic)\b', body, re.M):
            raise AssertionError('Per-operation allocation in actual scalar sink: ' + name)
        calls = re.findall(r'// (?:InterfaceMethod|Method) ([^\n]+)', body)
        if any(not call.startswith('addWork1973:') for call in calls):
            raise AssertionError('Scalar sink invokes a provider/publication/string helper: ' + repr(calls))
        verified.append(name)
    for name in ('foreground', 'current', 'owner', 'record', 'copy'):
        body = method_code(facade_bytecode, name)
        if re.search(r'^\s*\d+:\s+(?:new|anewarray|newarray|multianewarray|invokedynamic)\b', body, re.M):
            raise AssertionError('Per-operation allocation in actual optional facade: ' + name)
        if any(forbidden in body for forbidden in ('StringBuilder', 'publish:', 'refreshViews:', 'status1967:', 'qualified:', 'restore:', 'schedule:')):
            raise AssertionError('Optional facade performs publication or qualification work: ' + name)
        verified.append(name)
    if before != {str(path): sha(path) for path in tracked}:raise AssertionError('Input changed during executed detail suite')
    result.update({flag: True for flag in REQUIRED_FLAGS})
    result.update(assertions=result['assertions'] + len(verified) + 1,
                  java_assertions=result['assertions'], bytecode_methods_verified=verified,
                  production_source_sha256={path.name: sha(path) for path in production},
                  source_sha256=before, timing87_preservation_sha256=sha(inverse.encode()),
                  runner_source_sha256=sha(__file__), fixture_classes_in_runtime=False,
                  actual_jni=False, physical_android_tested=False, device_speedup_verified=False,
                  scope='Actual current photo timing and detail facade with controlled Android and background providers. '
                        'Historical Android helpers are copied with only two version literals adapted; their old test cases '
                        'are separately retained by host_timing1988. New cases exercise real trace persistence, failure, '
                        'identity/current scopes, concurrent work, terminal races, all optional provider failures, bounded '
                        'storage and unknown/zero semantics. Bytecode inspection verifies operation methods allocate '
                        'nothing and scalar sinks call no provider or UI/storage operation. No device or shader performance claim.')
    (work / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True);parser.add_argument('--work', required=True)
    parser.add_argument('--jdk');parser.add_argument('--ndk');args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
