#!/usr/bin/env python3
"""Current .88 route telemetry regression and exact .87 preservation view.

reviewed_source87 is solely a preservation/compiler-contract view. Runtime
regressions compile and execute the real .88 source. It accepts only the five
fully reviewed candidate hashes and inverts every exact, positioned edit; it is
not a general diagnostic stripper or permission to alter image arithmetic.
"""
from pathlib import Path
import hashlib
import json
import argparse
import importlib.util
import os
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parent
INVERSE_MANIFEST_SHA256 = 'dc3a98fe0e7cd245dfd83104de0481c94b992e99fcbd899b82116857a3604783'
BASELINE_SHA256 = {
    'StrongNoise1958.java': '92b89c40f61ce438084deb0bdeeed935b63bfc4ea7b83afb71f914dcd2082806',
    'QualityPipeline1932.java': 'f583cc83d21cc07b54e23fd99a85b27ed6aa8ba09613008ced71b989585bc6e0',
    'GpuAnalysis1961.java': 'b6b19b63264c859e1d0ed884ef9ac84768eaf39f696366e8c086b3d26dd41c55',
    'GpuProtection1961.java': 'b015686c3db870c12171db5e062037bcf948a59f55a642f9166f14b7f35f4953',
    'FastResize1933.java': 'cb2767c509ac609dc5044599e2dab69dbc86a8fdfb2d201ef4475c82902bf808',
}
REQUIRED_FLAGS = (
    'pipeline88_actual_front_units_and_commit_verified',
    'pipeline88_actual_model_three_units_verified',
    'pipeline88_actual_correction_units_backend_verified',
    'pipeline88_actual_snapshot_outcomes_verified',
    'pipeline88_optional_failure_preserves_pixels_ownership',
    'pipeline88_published87_missing_detail_reproduced',
    'pipeline88_reviewed_source_inverse_verified',
    'pipeline88_foreground_owner_background_exclusion',
)


def _sha(text):
    return hashlib.sha256(text.encode('utf-8')).hexdigest()


def inverse_manifest1988():
    path = ROOT / 'tests1988/pipeline_calls/source_inverse1988.json'
    raw = path.read_text()
    if _sha(raw) != INVERSE_MANIFEST_SHA256:
        raise AssertionError('Reviewed .88 telemetry inverse manifest changed')
    value = json.loads(raw)
    if value.get('schema') != 'ulike1988-pipeline-calls-exact-inverse-v1':
        raise AssertionError('Unexpected telemetry preservation schema')
    if set(value['files']) != set(BASELINE_SHA256):
        raise AssertionError('Telemetry preservation source scope differs')
    return value


def reviewed_source87(name, text):
    """Return exact pinned .87 only after all .88 hashes/hunks are validated."""
    name = str(name)
    if not name.endswith('.java'):
        name += '.java'
    if name not in BASELINE_SHA256 or not isinstance(text, str):
        raise AssertionError('Unreviewed telemetry inverse source: ' + name)
    item = inverse_manifest1988()['files'][name]
    baseline = (ROOT / 'tests1988/baseline87' / name).read_text()
    expected = BASELINE_SHA256[name]
    if _sha(baseline) != expected or item['baseline_sha256'] != expected:
        raise AssertionError('Pinned published .87 source changed: ' + name)
    if _sha(text) != item['candidate_sha256']:
        raise AssertionError('Current source differs from reviewed .88 telemetry: ' + name)
    edits = item['edits']
    if len(edits) != item['edit_count'] or not edits:
        raise AssertionError('Telemetry edit count differs: ' + name)
    previous_candidate = previous_baseline = 0
    for edit in edits:
        start, end = edit['candidate_char_start'], edit['candidate_char_end']
        old_start, old_end = edit['baseline_char_start'], edit['baseline_char_end']
        if not (previous_candidate <= start <= end <= len(text) and
                previous_baseline <= old_start <= old_end <= len(baseline)):
            raise AssertionError('Telemetry hunk bounds/order differ: ' + name)
        if (text[start:end] != edit['after'] or
                baseline[old_start:old_end] != edit['before'] or
                _sha(edit['after']) != edit['after_sha256'] or
                _sha(edit['before']) != edit['before_sha256'] or
                text[:start].count('\n') + 1 != edit['candidate_line'] or
                baseline[:old_start].count('\n') + 1 != edit['baseline_line']):
            raise AssertionError('Exact positioned telemetry hunk differs: ' + name)
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


def _analysis_fixture(text):
    """Expose only metadata already implied by the frozen reservation peer."""
    changes = (
        ('    static final class Reservation1984 implements AutoCloseable {',
         '    static final class QueueDecision1983 {\n'
         '        final boolean accepted;final long requestedBytes;\n'
         '        QueueDecision1983(boolean accepted,long bytes){this.accepted=accepted;requestedBytes=bytes;}\n'
         '    }\n'
         '    static final class Reservation1984 implements AutoCloseable {\n'
         '        final QueueDecision1983 decision;'),
        ('Reservation1984(boolean accepted,long bytes){this.accepted=accepted;',
         'Reservation1984(boolean accepted,long bytes){decision=new QueueDecision1983(accepted,bytes);this.accepted=accepted;'),
        ('Object commit(Probe probe){commits++;if(!begun||!current()){probe.close();close();return null;}pending=probe;held=false;return null;}',
         'QueueDecision1983 commit(Probe probe){commits++;if(!begun||!current()){probe.close();close();return new QueueDecision1983(false,bytes);}pending=probe;held=false;return new QueueDecision1983(true,bytes);}'),
    )
    for before, after in changes:
        if text.count(before) != 1:
            raise AssertionError('Frozen analysis control peer interface changed')
        text = text.replace(before, after, 1)
    return text


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    if source.is_file():
        source = source.parent
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
    jar = Path(os.environ.get('ULIKE_ANDROID_JAR', jdk.parent / 'android.jar')).resolve()
    if not jar.is_file():
        raise AssertionError('Pinned Android compiler declarations required')
    inverse_checks = {}
    for name, expected in BASELINE_SHA256.items():
        current = (source / name).read_text()
        restored = reviewed_source87(name, current)
        if _sha(restored) != expected:
            raise AssertionError('Incomplete source preservation inverse')
        try:
            reviewed_source87(name, current + '\n// unreviewed arithmetic mutation\n')
        except AssertionError:
            pass
        else:
            raise AssertionError('Unreviewed source mutation passed telemetry inverse')
        inverse_checks[name] = dict(baseline_sha256=expected, candidate_sha256=_sha(current),
            hunks=inverse_manifest1988()['files'][name]['edit_count'], mutation_rejected=True)
    sys.path.insert(0, str(source))
    builder = _module('pipeline88_calls_compile_closure', source / 'build1988.py')
    inputs = list(dict.fromkeys(list(builder.compile_inputs().values()) +
                              [builder.production_path(name) for name in builder.PRODUCTION]))
    local = source / 'tests1988/pipeline_calls'
    frozen = [source / path for path in (
        'tests1987/analysis/AnalysisPeers1987.java', 'tests1987/analysis/AnalysisCapture1987Test.java',
        'tests1987/protection/ProtectionPeers1987.java', 'tests1987/protection/ProtectionOpportunity1987Test.java',
        'tests1982/pipeline/BackendTransport1982.java', 'tests1982/pipeline/BackendDiagnostics1982Test.java',
        'tests/pipeline1942-fixtures/android/graphics/Bitmap.java',
        'tests/pipeline1942-fixtures/android/graphics/ColorSpace.java',
        'tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java')]
    tracked = list(dict.fromkeys(inputs + frozen + list(local.glob('*.java')) +
        [source / 'tests1988/baseline87' / name for name in BASELINE_SHA256] +
        [local / 'source_inverse1988.json', Path(__file__).resolve()]))
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
            if result.returncode == 0 or 'PIPELINE_DETAIL_MISSING' not in output:
                raise AssertionError('Pinned .87 did not reproduce absent detail at ' + label + '\n' + output[-12000:])
        elif result.returncode:
            raise AssertionError(label + '\n' + output[-14000:])
        return result.stdout
    def fresh(name):
        path = work / name
        if path.exists():
            shutil.rmtree(path)
        path.mkdir(parents=True)
        return path
    production, old, generated, failure = [fresh(n) for n in ('production', 'published87', 'generated', 'optional-failure')]
    run('actual88-production-compile', [jdk / 'bin/javac', '-source', '8', '-target', '8', '-Xlint:-options',
        '-encoding', 'UTF-8', '-bootclasspath', jar, '-d', production, *inputs])
    cp = os.pathsep.join(map(str, (production, jar)))
    queue = _module('pipeline88_android_fixtures', source / 'host_qualification1967.py')
    android = []
    for name, text in queue.FIXTURES.items():
        if name.startswith('android/'):
            path = generated / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text)
            android.append(path)
    android += frozen[-3:]
    adapted_analysis = generated / 'AnalysisPeers1987.java'
    adapted_analysis.write_text(_analysis_fixture(frozen[0].read_text()))
    peers = {}
    groups = {
        'pipeline': [frozen[4], frozen[5], local / 'PipelineCalls1988Test.java'],
        'analysis': [adapted_analysis, frozen[1], local / 'AnalysisDetail1988Test.java'],
        'protection': [frozen[2], frozen[3], local / 'ProtectionDetail1988Test.java'],
    }
    for name, group in groups.items():
        peers[name] = fresh(name + '-peers')
        run(name + '-peers-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8',
            '-cp', cp, '-d', peers[name], *android, local / 'DetailCalls1988.java', *group])
    # Error-producing facade is a fault injector only; actual .88 production
    # and all original numeric methods remain the compiled runtime inputs.
    fault_path = generated / 'failure/PipelineDetail1988.java'
    fault_path.parent.mkdir(parents=True)
    fault_path.write_text('''package com.hiro.ulike;
final class PipelineDetail1988 {
    static long faults1988;
    static AssertionError failure(){faults1988++;return new AssertionError("optional diagnostic facade failure");}
    static ProcessingTiming1947.Trace current(){throw failure();}
    static ProcessingTiming1947.Trace foreground(ProcessingTiming1947.Trace trace){throw failure();}
    static ProcessingTiming1947.Trace owner(Object image){throw failure();}
    static void record(ProcessingTiming1947.Trace trace,int phase,int backend,int reason,int units,long nanos){throw failure();}
    static void copy(ProcessingTiming1947.Trace trace,int phase,int outcome,long bytes,long nanos){throw failure();}
}
''')
    run('optional-failure-compile', [jdk / 'bin/javac', '--release', '8', '-cp', cp, '-d', failure, fault_path])
    run('published87-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', cp,
        '-d', old, *[source / 'tests1988/baseline87' / n for n in BASELINE_SHA256]])
    modes = ('model', 'model-fixed', 'front', 'front-background', 'front-failure',
             'geometry-cpu', 'geometry-gpu', 'geometry-fallback', 'geometry-background',
             'correction-moire', 'correction-sharp', 'correction-joined')
    reports = {}
    targets = [(mode, 'pipeline', 'PipelineCalls1988Test', [mode]) for mode in modes] + [
        ('analysis', 'analysis', 'AnalysisDetail1988Test', []),
        ('protection', 'protection', 'ProtectionDetail1988Test', [])]
    for fault in (False, True):
        for label, group, main, args in targets:
            paths = ([failure] if fault else []) + [peers[group], production, jar]
            label = ('fault-' if fault else 'actual88-') + label
            output = run(label, [jdk / 'bin/java', '-ea', '-Xmx768m', '-XX:ActiveProcessorCount=4',
                '-Dulike.detail.failure1988=' + str(fault).lower(), '-cp', os.pathsep.join(map(str, paths)),
                'com.hiro.ulike.' + main, *args])
            result = json.loads(output.strip().splitlines()[-1])
            if result.get('status') != 'passed' or result.get('assertions', 0) <= 0 or result.get('optional_failure') is not fault:
                raise AssertionError('Empty/unexecuted actual route telemetry report: ' + label)
            reports[label] = result
    negatives = ('model', 'geometry-cpu', 'correction-moire', 'analysis', 'protection')
    for label, group, main, args in targets:
        if label in negatives:
            run('published87-missing-' + label, [jdk / 'bin/java', '-ea', '-Xmx768m', '-XX:ActiveProcessorCount=4',
                '-cp', os.pathsep.join(map(str, (old, peers[group], production, jar))),
                'com.hiro.ulike.' + main, *args], negative=True)
    if pins() != before:
        raise AssertionError('Production or frozen fixtures changed during actual route tests')
    report = dict(status='passed', assertions=sum(item['assertions'] for item in reports.values()) + 10,
        cases=reports, inverse=inverse_checks, published87_missing_detail_negative_controls=list(negatives),
        source_sha256=before, commands=commands, physical_android_tested=False, device_speedup_verified=False,
        actual_gpu_backend_executed=False, actual_cpu_pixel_arithmetic_executed=True,
        controlled_peers=['Android bitmap ownership', 'GPU transport result buffers',
                          'analysis/protection reservation and certificate state', 'optional facade Throwable'],
        runtime_preservation_inverse_substitution=False,
        scope='All five actual .88 production roots run with current actual PipelineDetail1988/ProcessingTiming1947; separate explicit facade fault injection and immutable .87 missing-detail controls.')
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
    print(json.dumps({key: result[key] for key in ('status', 'assertions', *REQUIRED_FLAGS)}))
