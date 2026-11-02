#!/usr/bin/env python3
"""Execute current .89 branch-reason diagnostics and pinned .88 negative controls.

The exact reviewed_source88 inverse is solely a preservation/compiler-contract
view. Runtime tests execute the actual current production classes, actual CPU
arithmetic and actual capture-owned scalar sinks; no inverse is used at runtime.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parent
INVERSE_MANIFEST_SHA256 = 'd0a471e248c57d93973ddad6c6feeb3ac5d50e12fdc12c03f41031a545ecddfc'
BASELINE_SHA256 = {
    'SingleNoise1955.java': 'ffccfb3c81782e250fb9501ab9619940128ed54cc74a95c289e50613753655c6',
    'GpuSingle1960.java': '2ed0c0ee874dcc71985a928114995c7a35127a7dc3e2001cd9521f01ff1b39eb',
    'GpuResidual1961.java': '92aace4509d84a09e4a334051124ce2f24e44d77e13b566c8b5e205bf7bd9315',
    'QualityPipeline1932.java': '6aece3e1e44d5aa7ad79f3606daa712688a269c3203801ac51b1bb6df3ad0372',
    'FastResize1933.java': '92dc85522fd06ef8a543642e884f7a9cb4ca4cd85a1cb894ab81ee727c418e17',
    'GpuGeometry1960.java': '56facfe246b630d1c578e8246f18a15923d63090b18850dbc66a3d173c75be89',
    'WholeRoute1953.java': '0fdbcd12f48038f5663ca8083e7f4ff2bb17b513ab31b7d9c5d1fabd3f22b5e3',
    'GpuAnalysis1961.java': 'ce3d53c20b2a804f458b400e4080f7d4b52a2d57f0a775302e1b0e71c84d6537',
    'PipelineDetail1988.java': 'e003a261544a6280ddd3d4fdee90ef3a7ddc0ad208645f447ee06620b7962b7c',
}
REQUIRED_FLAGS = (
    'pipeline89_cpu_reasons_from_actual_branches',
    'pipeline89_single_residual_handoff_reset_verified',
    'pipeline89_correction_reason_result_owner_verified',
    'pipeline89_queue_decision_metadata_no_requery',
    'pipeline89_requested_acquired_pixel_bytes_distinct',
    'pipeline89_optional_owner_terminal_background_isolation',
    'pipeline89_published88_missing_reason_reproduced',
    'pipeline89_reviewed_source_inverse_verified',
)


def _sha(text):
    return hashlib.sha256(text.encode('utf-8')).hexdigest()


def inverse_manifest1989():
    raw = (ROOT / 'tests1989/reasons/source_inverse1989.json').read_text()
    if _sha(raw) != INVERSE_MANIFEST_SHA256:
        raise AssertionError('Reviewed .89 reason inverse manifest changed')
    value = json.loads(raw)
    if value.get('schema') != 'ulike1989-pipeline-reasons-exact-inverse-v1':
        raise AssertionError('Unexpected reason preservation schema')
    if set(value['files']) != set(BASELINE_SHA256):
        raise AssertionError('Reason preservation source scope differs')
    return value


def reviewed_source88(name, text):
    """Restore exact pinned .88 after validating every reviewed .89 source hunk."""
    name = str(name)
    if not name.endswith('.java'):
        name += '.java'
    if name not in BASELINE_SHA256 or not isinstance(text, str):
        raise AssertionError('Unreviewed reason inverse source: ' + name)
    item = inverse_manifest1989()['files'][name]
    baseline = (ROOT / 'tests1989/reasons-baseline88' / name).read_text()
    expected = BASELINE_SHA256[name]
    if _sha(baseline) != expected or item['baseline_sha256'] != expected:
        raise AssertionError('Pinned published .88 source changed: ' + name)
    if _sha(text) != item['candidate_sha256']:
        raise AssertionError('Current source differs from reviewed .89 reasons: ' + name)
    edits = item['edits']
    if len(edits) != item['edit_count'] or not edits:
        raise AssertionError('Reason edit count differs: ' + name)
    previous_candidate = previous_baseline = 0
    for edit in edits:
        start, end = edit['candidate_char_start'], edit['candidate_char_end']
        old_start, old_end = edit['baseline_char_start'], edit['baseline_char_end']
        if not (previous_candidate <= start <= end <= len(text) and
                previous_baseline <= old_start <= old_end <= len(baseline)):
            raise AssertionError('Reason hunk bounds/order differ: ' + name)
        if (text[start:end] != edit['after'] or
                baseline[old_start:old_end] != edit['before'] or
                _sha(edit['after']) != edit['after_sha256'] or
                _sha(edit['before']) != edit['before_sha256'] or
                text[:start].count('\n') + 1 != edit['candidate_line'] or
                baseline[:old_start].count('\n') + 1 != edit['baseline_line']):
            raise AssertionError('Exact positioned reason hunk differs: ' + name)
        previous_candidate, previous_baseline = end, old_end
    for edit in reversed(edits):
        text = text[:edit['candidate_char_start']] + edit['before'] + text[edit['candidate_char_end']:]
    if _sha(text) != expected or text != baseline:
        raise AssertionError('Unreviewed nontelemetry source changes remain: ' + name)
    return text


def _module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    if source.is_file():
        source = source.parent
    work.mkdir(parents=True, exist_ok=True)
    executable = shutil.which('javac')
    configured = jdk or os.environ.get('ULIKE_JDK_HOME')
    if not configured and not executable:
        raise AssertionError('JDK required for actual production regression')
    jdk = Path(configured or Path(executable).resolve().parent.parent).resolve()
    jar = Path(os.environ.get('ULIKE_ANDROID_JAR', jdk.parent / 'android.jar')).resolve()
    if not jar.is_file():
        raise AssertionError('Pinned Android compiler declarations required')
    inverse_checks = {}
    for name, expected in BASELINE_SHA256.items():
        current = (source / name).read_text()
        if _sha(reviewed_source88(name, current)) != expected:
            raise AssertionError('Incomplete source preservation inverse')
        try:
            reviewed_source88(name, current + '\n// unreviewed arithmetic mutation\n')
        except AssertionError:
            pass
        else:
            raise AssertionError('Unreviewed source mutation passed reason inverse')
        inverse_checks[name] = dict(baseline_sha256=expected, candidate_sha256=_sha(current),
            hunks=inverse_manifest1989()['files'][name]['edit_count'], mutation_rejected=True)
    sys.path.insert(0, str(source))
    builder = _module('pipeline89_reason_compile_closure', source / 'build1989.py')
    inputs = list(dict.fromkeys(list(builder.compile_inputs().values()) +
                              [builder.production_path(name) for name in builder.PRODUCTION]))
    local = source / 'tests1989/reasons'
    frozen = [source / p for p in (
        'tests1987/analysis/AnalysisCapture1987Test.java',
        'tests1982/pipeline/BackendDiagnostics1982Test.java',
        'tests/pipeline1942-fixtures/android/graphics/Bitmap.java',
        'tests/pipeline1942-fixtures/android/graphics/ColorSpace.java',
        'tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java')]
    native = [source / 'native1960/residual_blocks1961.c', source / 'native1955/single_noise1955.c']
    tracked = list(dict.fromkeys(inputs + frozen + native + list(local.glob('*.java')) +
        [source / 'tests1989/reasons-baseline88' / n for n in BASELINE_SHA256] +
        [local / 'source_inverse1989.json', Path(__file__).resolve()]))
    def pins():
        return {str(p.relative_to(source)) if p.is_relative_to(source) else str(p):
                hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    before = pins()
    commands = []
    def run(label, command, negative=False):
        command = list(map(str, command))
        commands.append(dict(label=label, argv=command))
        result = subprocess.run(command, text=True, capture_output=True, timeout=180)
        output = result.stdout + result.stderr
        (work / (label + '.log')).write_text(output)
        if negative:
            if result.returncode == 0 or 'REASON1989_MISSING' not in output:
                raise AssertionError('Pinned .88 did not reproduce absent reason at ' + label + '\n' + output[-12000:])
        elif result.returncode:
            raise AssertionError(label + '\n' + output[-14000:])
        return result.stdout
    def fresh(name):
        path = work / name
        if path.exists():
            shutil.rmtree(path)
        path.mkdir(parents=True)
        return path
    production, old, generated, failure = [fresh(n) for n in ('production', 'published88', 'generated', 'optional-failure')]
    run('actual89-production-compile', [jdk / 'bin/javac', '-source', '8', '-target', '8', '-Xlint:-options',
        '-encoding', 'UTF-8', '-bootclasspath', jar, '-d', production, *inputs])
    cp = os.pathsep.join(map(str, (production, jar)))
    queue = _module('pipeline89_reason_android_fixtures', source / 'host_qualification1967.py')
    android = []
    for name, text in queue.FIXTURES.items():
        if name.startswith('android/'):
            path = generated / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text)
            android.append(path)
    android += frozen[-3:]
    peers = {}
    groups = {
        'routes': [local / 'ReasonsTransport1989.java', frozen[1],
                   local / 'ReasonsRoutes1989Test.java', local / 'ReasonsWhole1989Test.java'],
        'analysis': [local / 'ReasonsAnalysisPeers1989.java', frozen[0], local / 'ReasonsAnalysis1989Test.java'],
    }
    for name, group in groups.items():
        peers[name] = fresh(name + '-peers')
        run(name + '-peers-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8',
            '-cp', cp, '-d', peers[name], *android, local / 'ReasonsAssertions1989.java', *group])
    fault_path = generated / 'failure/PipelineDetail1988.java'
    fault_path.parent.mkdir(parents=True, exist_ok=True)
    fault_path.write_text('''package com.hiro.ulike;
final class PipelineDetail1988 {
    static long faults1989;
    static AssertionError failure(){faults1989++;return new AssertionError("optional diagnostic facade failure");}
    static ProcessingTiming1947.Trace current(){throw failure();}
    static ProcessingTiming1947.Trace foreground(ProcessingTiming1947.Trace trace){throw failure();}
    static ProcessingTiming1947.Trace owner(Object image){throw failure();}
    static void record(ProcessingTiming1947.Trace trace,int phase,int backend,int reason,int units,long nanos){throw failure();}
    static void copy(ProcessingTiming1947.Trace trace,int phase,int outcome,long bytes,long nanos){throw failure();}
    static void priorReason1989(ProcessingTiming1947.Trace trace,int phase,int reason,int units){throw failure();}
    static void copyState1989(ProcessingTiming1947.Trace trace,int phase,int outcome,long requestedBytes,
        long acquiredPixelBytes,long nanos,int queueReason,long retryRemainingNanos,int retries,
        int queuedJobs,int runningJobs,long retainedBytes){throw failure();}
}
''')
    run('optional-failure-compile', [jdk / 'bin/javac', '--release', '8', '-cp', cp, '-d', failure, fault_path])
    run('published88-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', cp,
        '-d', old, *[source / 'tests1989/reasons-baseline88' / n for n in BASELINE_SHA256]])
    # The selected Residual driver executes the original binary64 preparation.
    # Only GPU transport/readback is controlled; no arithmetic is replaced.
    library = work / 'libresidual_reasons1989.so'
    run('actual-residual-native-compile', ['cc', '-O3', '-std=c11', '-Wall', '-Wextra', '-Werror',
        '-ffp-contract=off', '-fno-fast-math', '-fPIC', '-shared', '-Wl,--no-undefined',
        '-I' + str(jdk / 'include'), '-I' + str(jdk / 'include/linux'), native[0],
        '-lm', '-pthread', '-o', library])
    driver_modes = ('unavailable', 'memory', 'cold', 'exact', 'busy', 'policy', 'session', 'stage',
                    'noresult', 'link', 'memory-failure', 'open', 'gpu')
    route_modes = [prefix + mode for prefix in ('single-', 'residual-') for mode in driver_modes]
    route_modes += ['front-' + mode for mode in ('normal', 'background', 'terminal', 'write-failure')]
    route_modes += ['geometry-' + mode for mode in ('cold', 'unavailable', 'session', 'environment', 'exact',
                    'noresult', 'link', 'memory', 'gpu', 'open', 'background', 'terminal')]
    route_modes += ['correction-' + mode for mode in ('moire', 'sharp', 'joined')]
    whole_modes = ('missing', 'environment', 'dispatch', 'gpu-null', 'gpu-runtime', 'gpu-link', 'gpu-memory',
                   'publish', 'unavailable', 'busy', 'optional', 'clock', 'gpu', 'cpu-throws')
    analysis_modes = ['cold', 'commit-declined', 'begin-declined', 'allocation', 'partial', 'background',
        'terminal', 'null-metadata', 'null-commit', 'source-failure', 'invalid-traversal', 'valid-partial']
    analysis_modes += ['denied-' + str(reason) for reason in range(2, 22)]
    targets = [(m, 'routes', 'ReasonsRoutes1989Test', m) for m in route_modes]
    targets += [('whole-' + m, 'routes', 'ReasonsWhole1989Test', m) for m in whole_modes]
    targets += [('analysis-' + m, 'analysis', 'ReasonsAnalysis1989Test', m) for m in analysis_modes]
    fault_modes = {'front-normal', 'front-write-failure', 'geometry-cold', 'geometry-gpu', 'correction-moire',
                   'correction-joined', 'analysis-cold', 'analysis-denied-14', 'analysis-invalid-traversal'}
    reports = {}
    for fault in (False, True):
        for label, group, main, mode in targets:
            if fault and label not in fault_modes:
                continue
            paths = ([failure] if fault else []) + [peers[group], production, jar]
            report_label = ('fault-' if fault else 'actual89-') + label
            output = run(report_label, [jdk / 'bin/java', '-ea', '-Xmx768m', '-XX:ActiveProcessorCount=4',
                '-Dulike.reasons.fault1989=' + str(fault).lower(), '-cp', os.pathsep.join(map(str, paths)),
                'com.hiro.ulike.' + main, mode, *([library] if main == 'ReasonsRoutes1989Test' else [])])
            result = json.loads(output.strip().splitlines()[-1])
            if result.get('status') != 'passed' or result.get('assertions', 0) <= 0 or result.get('optional_failure') is not fault:
                raise AssertionError('Empty/unexecuted actual branch report: ' + report_label)
            reports[report_label] = result
    negatives = ('single-cold', 'residual-cold', 'front-normal', 'geometry-cold', 'correction-moire',
                 'whole-missing', 'analysis-cold', 'analysis-denied-14')
    for label, group, main, mode in targets:
        if label in negatives:
            run('published88-missing-' + label, [jdk / 'bin/java', '-ea', '-Xmx768m', '-XX:ActiveProcessorCount=4',
                '-cp', os.pathsep.join(map(str, (old, peers[group], production, jar))),
                'com.hiro.ulike.' + main, mode, *([library] if main == 'ReasonsRoutes1989Test' else [])], negative=True)
    if pins() != before:
        raise AssertionError('Production or frozen fixtures changed during actual reason tests')
    report = dict(status='passed', assertions=sum(item['assertions'] for item in reports.values()) + 2 * len(inverse_checks),
        pixels=sum(item['pixels'] for item in reports.values()), cases=reports, inverse=inverse_checks,
        published88_missing_reason_negative_controls=list(negatives), source_sha256=before, commands=commands,
        physical_android_tested=False, device_speedup_verified=False, actual_gpu_backend_executed=False,
        actual_cpu_pixel_arithmetic_executed=True, actual_native_residual_preparation_executed=True,
        runtime_preservation_inverse_substitution=False,
        controlled_peers=['Android bitmap ownership', 'GPU transport outcomes and buffers',
            'analysis reservation and certificate decisions', 'WholeRoute work, history and monotonic clock',
            'optional facade Throwable'],
        scope='Nine actual .89 production roots with actual PipelineDetail1988/ProcessingTiming1947; '
              'explicit fault injection and immutable .88 missing-reason controls are separate executions.')
    report.update({flag: True for flag in REQUIRED_FLAGS})
    (work / 'result.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=ROOT)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    parser.add_argument('--ndk', type=Path)
    args = parser.parse_args()
    result = test(args.source, args.work, args.jdk, args.ndk)
    print(json.dumps({key: result[key] for key in ('status', 'assertions', 'pixels', *REQUIRED_FLAGS)}))
