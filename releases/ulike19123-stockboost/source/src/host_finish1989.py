#!/usr/bin/env python3
"""Execute Finish .89 skip, captured diagnostics and scoped memory integration.

The .88 source is SHA-pinned and runs independently as a negative control.
Current production admission/queue/digest/proofs execute against explicit host
renderer/clock seams. Separate actual stage-graph tests retain real Finish
orchestration while replacing native transport with finite failure peers.
This is not physical Android, GPU pixel, or device-speed evidence.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import subprocess
import sys

BASELINE88 = {
    'GpuChain1961.java': 'ee4c2da3ff453e50f23fc256002ea0d9c5644907aa9cd8e8378fd08bf26541d4',
    'GpuQualification1961.java': 'ae51bce549f4efca7f75e8d72e380e5a42e3fc1ede5646a16c23994cdb6ad001',
}
FLAGS = (
    'finish_published88_redundant_second_oracle1989_negative_verified',
    'finish_no_viable_candidate_skips_second_oracle1989_verified',
    'finish_legacy_or_new_survivor_two_trials1989_verified',
    'finish_two_exact_two_output_five_percent1989_preserved',
    'finish_cancel_before_break_and_resources1989_verified',
    'finish_typed_failure_retry_exact_history1989_preserved',
    'finish_capture_owner_and_shape1989_verified',
    'finish_memory_diagnostics_copied_scalars1989_verified',
    'finish_active_terminal_snapshot1989_immutable',
    'finish_optional_diagnostic_failures1989_noninterference',
    'finish_background_stage8_memory_relief1989_verified',
    'finish_relieved_output_includes_trim_time1989_verified',
    'finish_reviewed_source_inverse1989_verified',
)


def sha(value):
    return hashlib.sha256(value if isinstance(value, bytes) else Path(value).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


def reviewed_source88_1989(raw, name):
    """Preservation-only exact hunk inverse: current and baseline SHA required."""
    local = Path(__file__).resolve().parent / 'tests1989/finish'
    baseline = (local / 'baseline88' / name).read_bytes()
    if sha(baseline) != BASELINE88[name]:
        raise AssertionError('Frozen published .88 source changed: ' + name)
    review = json.loads((local / 'reviewed-spans.json').read_text())[name]
    if sha(raw) != review['current_sha256'] or review['baseline_sha256'] != BASELINE88[name]:
        raise AssertionError('Unreviewed current Finish source: ' + name)
    lines = raw.decode().splitlines(keepends=True)
    for hunk in reversed(review['hunks']):
        first, last = hunk['new_first'], hunk['new_last']
        if ''.join(lines[first:last]) != hunk['new']:
            raise AssertionError('Reviewed .89 inverse span differs: ' + name)
        lines[first:last] = hunk['old'].splitlines(keepends=True)
    restored = ''.join(lines).encode()
    if restored != baseline or sha(restored) != BASELINE88[name]:
        raise AssertionError('Unreviewed changes remain after exact .89 inverse: ' + name)
    return restored


def reviewed_chain88_source1989(raw):
    return reviewed_source88_1989(raw, 'GpuChain1961.java')


def restore88(source, work):
    source, work = Path(source), Path(work)
    work.mkdir(parents=True, exist_ok=True)
    for name in BASELINE88:
        (work / name).write_bytes(reviewed_source88_1989((source / name).read_bytes(), name))
    return work


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    jar = Path(os.environ.get('ULIKE_ANDROID_JAR', str(jdk.parent / 'android.jar'))).resolve()
    local = source / 'tests1989/finish'
    restored = restore88(source, work / 'restored88')
    # Inverse rejection is executable, not a claim from a source-presence test.
    for name in BASELINE88:
        raw = (source / name).read_bytes()
        try:
            reviewed_source88_1989(raw + b'\n// unreviewed drift\n', name)
        except AssertionError:
            pass
        else:
            raise AssertionError('Inverse accepted unreviewed source drift')
    sys.path.insert(0, str(source))
    builder = module('finish89_compile_inputs', source / 'build1985.py')
    inputs = list(dict.fromkeys(list(builder.compile_inputs().values()) + [builder.production_path(n) for n in builder.PRODUCTION] +
        [source / 'PipelineDetail1988.java', source / 'FinishMemory1989.java']))
    tracked = [source / name for name in BASELINE88] + [source / 'FinishMemory1989.java', source / 'GpuNoise1960.java', source / 'SpeedWorkers1935.java', source / 'ProcessingTiming1947.java']
    tracked += [p for p in local.rglob('*') if p.is_file()] + [Path(__file__).resolve()]
    tracked += [source / name for name in ('host_finish_failures1988.py', 'host_finish_baseline1986.py',
        'tests1988/finish_failures/FinishFailures1988Test.java', 'tests1988/finish_failures/FinishStagePeers1988.java',
        'tests1986/finish_baseline/FinishControl1986.java', 'tests1984/qualification/QualificationProgress1984Test.java')]
    before = {str(p.relative_to(source)): sha(p) for p in tracked}
    commands = []

    def command(label, argv):
        argv = list(map(str, argv));commands.append(dict(label=label, argv=argv))
        proc = subprocess.run(argv, capture_output=True, text=True, timeout=240)
        (work / (label + '.log')).write_text(proc.stdout + proc.stderr)
        if proc.returncode:
            raise AssertionError(label + '\n' + (proc.stdout + proc.stderr)[-16000:])
        return proc.stdout

    production, fixtures, generated = work / 'production', work / 'fixture-classes', work / 'fixture-src'
    production.mkdir(exist_ok=True);fixtures.mkdir(exist_ok=True)
    command('production-compile', [jdk / 'bin/javac', '-source', '8', '-target', '8', '-Xlint:-options', '-encoding', 'UTF-8',
        '-bootclasspath', jar, '-d', production, *inputs])
    queue = module('finish89_queue_peers', source / 'host_qualification1981.py')
    fixture_inputs = []
    for name, body in queue.FIXTURES.items():
        if name.endswith('/QueueHost1967.java'):
            continue
        if name.endswith('/QueueFixtures1967.java'):
            anchor = 'static boolean sessionBusy(){return busy;}'
            if body.count(anchor) != 1:raise AssertionError('Frozen queue peer seam changed')
            body = body.replace(anchor, anchor + 'static final class Session {} static boolean workspaceFits(long bytes){return FinishControl1986.workspace&&bytes>=0&&bytes<=512L*1024*1024;}')
            anchor = 'static long captureEpoch1953(){return epoch;}'
            if body.count(anchor) != 1:raise AssertionError('Frozen capture peer seam changed')
            body = body.replace(anchor, anchor + 'static long capture1989=1791670635624L;static long captureId1989(long expectedEpoch){return expectedEpoch==epoch?capture1989:0;}')
        path = generated / name;path.parent.mkdir(parents=True, exist_ok=True);path.write_text(body);fixture_inputs.append(path)
    fixture_inputs += [source / name for name in (
        'tests/pipeline1942-fixtures/android/graphics/Bitmap.java', 'tests/pipeline1942-fixtures/android/graphics/ColorSpace.java',
        'tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java', 'tests1986/finish_baseline/FinishControl1986.java',
        'tests1984/qualification/QualificationProgress1984Test.java', 'tests1984/qualification/CameraTrace1965.java',
        'tests1988/finish_failures/FinishFailures1988Test.java')]
    fixture_inputs += [local / 'Finish1989Test.java']
    cp = os.pathsep.join(map(str, (production, jar)))
    command('fixtures-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', cp, '-d', fixtures, *fixture_inputs])
    renderers = module('finish89_explicit_frozen_renderer_seams', source / 'host_finish_failures1988.py')
    parser = module('finish89_method_parser', source / 'host_finish_baseline1986.py')
    reports, adapters = {}, {}
    for label in ('published88', 'current89', 'fault-runtime', 'fault-linkage', 'fault-oom'):
        directory = work / label;directory.mkdir(exist_ok=True)
        base = restored if label == 'published88' else source
        raw_chain = (base / 'GpuChain1961.java').read_text()
        chain, evidence = renderers.adapt_renderers1988(raw_chain, source)
        if parser.method_body(chain, 'qualifyFinish1978').replace('FinishControl1986.nanoTime()', 'System.nanoTime()') != parser.method_body(raw_chain, 'qualifyFinish1978'):
            raise AssertionError('Current .89 admission changed by renderer seam')
        (directory / 'GpuChain1961.java').write_text(chain)
        qualification = (base / 'GpuQualification1961.java').read_text()
        if label.startswith('fault-'):
            failure = {'fault-runtime': 'IllegalStateException', 'fault-linkage': 'LinkageError', 'fault-oom': 'OutOfMemoryError'}[label]
            for name in ('finishShape1989', 'finishOracle1989', 'finishMemory1989'):
                start, brace, end = parser.method_span(qualification, name)
                qualification = qualification[:brace + 1] + '\n        throw new ' + failure + '("controlled optional metadata failure");\n    }' + qualification[end:]
        (directory / 'GpuQualification1961.java').write_text(qualification)
        command(label + '-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', os.pathsep.join(map(str, (production, fixtures, jar))),
            '-d', directory, directory / 'GpuChain1961.java', directory / 'GpuQualification1961.java'])
        output = command(label + '-run', [jdk / 'bin/java', '-ea', '-Xmx768m', '-cp', os.pathsep.join(map(str, (fixtures, directory, production, jar))),
            'com.hiro.ulike.Finish1989Test', label])
        report = json.loads(output.strip().splitlines()[-1])
        if report.get('status') != 'passed' or report.get('assertions', 0) <= 0:
            raise AssertionError('Executed passing .89 assertions missing: ' + label)
        reports[label] = report;adapters[label] = evidence
    stage_reports = run_stage_graph(source, work, jdk, jar, production, fixtures, restored, command, parser)
    reports.update(stage_reports)
    after = {str(p.relative_to(source)): sha(p) for p in tracked}
    if before != after:raise AssertionError('Production or fixtures changed during .89 regression')
    result = dict(status='passed', assertions=2+sum(report['assertions'] for report in reports.values()), cases=reports,
        **{flag: True for flag in FLAGS}, source_hashes=before, baseline88_source_sha256=BASELINE88, controlled_renderers=adapters,
        physical_android_tested=False, device_speedup_verified=False, actual_gpu_transport_executed=False,
        actual_production_qualification_control_executed=True, actual_production_queue_executed=True,
        actual_production_resident_proof_executed=True, actual_production_finish_stage_graph_executed=True,
        fixture_scope='Current qualification, skip, CPU digest, typed outcomes, exact/full-output gates, retry, queue ownership and immutable snapshots execute. Renderer outcomes and elapsed clock are controlled; stage tests retain actual Finish command graph plus actual memory helper with explicit GPU/native peers. Native pixel and physical-device speed claims are excluded.')
    (work / 'report.json').write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n')
    (work / 'commands.json').write_text(json.dumps(commands, ensure_ascii=False, indent=2)+'\n')
    return result


def run_stage_graph(source, work, jdk, jar, production, fixtures, restored, command, parser):
    local = source / 'tests1989/finish'
    peers = work / 'stage-peers';peers.mkdir(exist_ok=True)
    command('stage-peers-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8',
        '-cp', os.pathsep.join(map(str, (production, fixtures, jar))), '-d', peers,
        local / 'FinishStagePeers1989.java', local / 'FinishMemoryGraph1989Test.java'])
    reports = {}
    for label in ('published88', 'current89'):
        stage = work / ('stage-' + label);stage.mkdir(exist_ok=True)
        base = restored if label == 'published88' else source
        original = (base / 'GpuChain1961.java').read_text()
        body = original
        start, brace, end = parser.method_span(body, 'cpuFinishOwned1962')
        body = body[:brace + 1] + '\n        return FinishControl1986.cpu(corrected,rotation,width,height,outputPlan,refreshNoise);\n    }' + body[end:]
        start, brace, end = parser.method_span(body, 'qualifyFinish1978')
        control = body[start:end]
        if control != parser.method_body(original, 'qualifyFinish1978'):
            raise AssertionError('Actual stage graph changed production qualification')
        body = body[:start] + control.replace('System.nanoTime()', 'FinishControl1986.nanoTime()') + body[end:]
        # Only the CPU renderer entry and clock changed. The .89 stage8 gate,
        # comparator, bands, readback, leases and all cleanup remain executable.
        if parser.method_body(body, 'compareFinish1988') != parser.method_body(original, 'compareFinish1988'):
            raise AssertionError('Actual graph adapter altered production comparator')
        (stage / 'GpuChain1961.java').write_text(body)
        command('stage-' + label + '-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8',
            '-cp', os.pathsep.join(map(str, (production, fixtures, jar))), '-d', stage,
            stage / 'GpuChain1961.java', base / 'GpuQualification1961.java'])
        output = command('stage-' + label + '-run', [jdk / 'bin/java', '-ea', '-Xmx768m',
            '-cp', os.pathsep.join(map(str, (peers, fixtures, stage, production, jar))),
            'com.hiro.ulike.FinishMemoryGraph1989Test', label])
        report = json.loads(output.strip().splitlines()[-1])
        if report.get('status') != 'passed' or report.get('assertions', 0) <= 0 or report.get('actual_finish_stage_graph') is not True:
            raise AssertionError('Actual Finish graph assertions missing')
        if label == 'current89' and report.get('actual_memory_helper') is not True:
            raise AssertionError('Current stage integration did not execute the real helper')
        reports['actual_graph_' + label] = report
    return reports


run = test


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True);parser.add_argument('--work', required=True);parser.add_argument('--jdk');parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source,args.work,args.jdk,args.ndk),ensure_ascii=False,indent=2))
