#!/usr/bin/env python3
"""Finish failure classification and bounded, optional stage observation.

The production queue and Finish admission bodies execute against controlled
renderer outcomes and elapsed times. A separate stage suite retains the actual
command graph with explicit transport fault peers. No physical GPU speed or
Android pixel claim is made by these host fixtures.
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

BASELINE87 = {
    'GpuChain1961.java': '2ffb97882530d0fae926994c4b1fc9f4f90797bc3d32290cc7f42eabfb0478c6',
    'GpuQualification1961.java': '8babb1e44a87da6181582bb768c4d60623792b52e637f40a5dc5248ea13af390',
}
FLAGS = (
    'finish_published87_unmeasured_speed_negative1988_verified',
    'finish_unavailable_is_non_speed1988_verified',
    'finish_mixed_failures1988_verified',
    'finish_memory_and_reference_failures1988_verified',
    'finish_two_exact_two_output_speed_gate1988_verified',
    'finish_legacy_fallback_and_keys1988_preserved',
    'finish_cancellation_and_ownership1988_verified',
    'finish_non_speed_cooldown_retry_bound1988_verified',
    'finish_exact_journal_and_certificate_guard1988_verified',
    'finish_stage_fault_location1988_verified',
    'finish_optional_diagnostics1988_noninterference',
    'finish_terminal_stage_snapshot1988_immutable',
    'finish_scalar_stage_event_bound1988_verified',
    'finish_reviewed_source_inverse1988_verified',
)


def sha(value):
    return hashlib.sha256(value if isinstance(value, bytes) else Path(value).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


def reviewed_source87_1988(raw, name):
    """Strict reviewed-hunk inverse, including whole current and old SHA pins."""
    root = Path(__file__).resolve().parent / 'tests1988/finish_failures'
    baseline = (root / 'baseline87' / name).read_bytes()
    if sha(baseline) != BASELINE87[name]:
        raise AssertionError('Published .87 baseline changed: ' + name)
    pins = json.loads((root / 'reviewed-spans.json').read_text())[name]
    if sha(raw) != pins['current_sha256']:
        raise AssertionError('Reviewed .88 source changed: ' + name)
    lines = raw.decode().splitlines(keepends=True)
    for hunk in reversed(pins['hunks']):
        first, last = hunk['new_first'], hunk['new_last']
        if ''.join(lines[first:last]) != hunk['new']:
            raise AssertionError('Reviewed .88 inverse hunk changed: ' + name)
        lines[first:last] = hunk['old'].splitlines(keepends=True)
    restored = ''.join(lines).encode()
    if restored != baseline or sha(restored) != BASELINE87[name]:
        raise AssertionError('Unreviewed changes remain after exact .88 inverse: ' + name)
    return restored


def reviewed_chain87_source1988(raw):
    return reviewed_source87_1988(raw, 'GpuChain1961.java')


def restore87(source, work):
    source, work = Path(source), Path(work)
    work.mkdir(parents=True, exist_ok=True)
    for name in BASELINE87:
        (work / name).write_bytes(reviewed_source87_1988((source / name).read_bytes(), name))
    return work


def install_legacy88(retained, source, work, observed):
    """Adapt only frozen preservation reads and explicit renderer test seams.

    Historical qualification code always executes current .88 control. Only
    its elapsed clock and the already controlled four renderer entry bodies
    are substituted in generated classes; the typed .88 comparison entry is
    the same fourth renderer seam with an additional memory result.
    """
    source = Path(source).resolve()
    current = (source / 'GpuChain1961.java').read_bytes()
    restored = reviewed_chain87_source1988(current)
    original_body, original_adapt = retained.method_body, retained.adapt_controlled_renderers

    def preserved_body(body, name):
        if name == 'compareFinish1978' and body.encode() == current:
            observed.append(dict(adapter='finish1988_preservation_compare_only', source_sha256=sha(current), restored_sha256=sha(restored)))
            return original_body(restored.decode(), name)
        return original_body(body, name)

    def controlled(body):
        adapted, evidence = original_adapt(body)
        if body.encode() == current:
            start, brace, end = retained.method_span(adapted, 'compareFinish1988')
            original = retained.method_body(body, 'compareFinish1988')
            replacement = '\n        return FinishControl1986.compare(source,rotation,width,height,plan,refresh,expected,tileRows,pipeline,carried,identityNoiseBefore,cancellation);\n    }'
            adapted = adapted[:brace + 1] + replacement + adapted[end:]
            if original_body(adapted, 'qualifyFinish1978').replace('FinishControl1986.nanoTime()', 'System.nanoTime()') != original_body(body, 'qualifyFinish1978'):
                raise AssertionError('The .88 renderer adapter changed actual qualification control')
            evidence['renderer_methods']['compareFinish1988'] = sha(original.encode())
            observed.append(dict(adapter='finish1988_controlled_typed_comparison', current_control_sha256=sha(original_body(body, 'qualifyFinish1978').encode())))
        return adapted, evidence

    retained.method_body = preserved_body
    retained.adapt_controlled_renderers = controlled
    return retained


def adapt_renderers1988(body, source):
    renderer = module('finish88_controlled_renderer_seams', Path(source) / 'host_finish_baseline1986.py')
    original = renderer.method_body(body, 'qualifyFinish1978')
    adapted, evidence = renderer.adapt_controlled_renderers(body)
    if 'static int compareFinish1988(' in body:
        start, brace, end = renderer.method_span(adapted, 'compareFinish1988')
        evidence['renderer_methods']['compareFinish1988'] = sha(renderer.method_body(body, 'compareFinish1988').encode())
        adapted = adapted[:brace + 1] + '\n        return FinishControl1986.compare(source,rotation,width,height,plan,refresh,expected,tileRows,pipeline,carried,identityNoiseBefore,cancellation);\n    }' + adapted[end:]
    if renderer.method_body(adapted, 'qualifyFinish1978').replace('FinishControl1986.nanoTime()', 'System.nanoTime()') != original:
        raise AssertionError('Controlled renderers must preserve current admission control')
    return adapted, evidence


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    jar = Path(os.environ.get('ULIKE_ANDROID_JAR', str(jdk.parent / 'android.jar'))).resolve()
    local = source / 'tests1988/finish_failures'
    restored = restore87(source, work / 'restored87')
    sys.path.insert(0, str(source))
    builder = module('finish88_production_compile_inputs', source / 'build1985.py')
    inputs = list(dict.fromkeys(list(builder.compile_inputs().values()) + [builder.production_path(n) for n in builder.PRODUCTION] + [source / 'PipelineDetail1988.java']))
    tracked = [source / name for name in BASELINE87]
    tracked += list(local.rglob('*'))
    tracked = [p for p in tracked if p.is_file()] + [Path(__file__).resolve(), source / 'tests1986/finish_baseline/FinishControl1986.java',
        source / 'tests1984/qualification/QualificationProgress1984Test.java', source / 'tests1984/qualification/CameraTrace1965.java']
    before = {str(p.relative_to(source)) if p.is_relative_to(source) else str(p): sha(p) for p in tracked}
    commands = []

    def command(label, argv):
        argv = list(map(str, argv));commands.append(dict(label=label, argv=argv))
        proc = subprocess.run(argv, capture_output=True, text=True, timeout=240)
        (work / (label + '.log')).write_text(proc.stdout + proc.stderr)
        if proc.returncode:
            raise AssertionError(label + '\n' + (proc.stdout + proc.stderr)[-18000:])
        return proc.stdout

    production, fixtures = work / 'production', work / 'fixture-classes'
    production.mkdir(exist_ok=True);fixtures.mkdir(exist_ok=True)
    command('production-compile', [jdk / 'bin/javac', '-source', '8', '-target', '8', '-Xlint:-options', '-encoding', 'UTF-8',
        '-bootclasspath', jar, '-d', production, *inputs])
    generated = work / 'fixture-src'
    queue = module('finish88_queue_fixtures', source / 'host_qualification1981.py')
    fixture_inputs = []
    for name, body in queue.FIXTURES.items():
        if name.endswith('/QueueHost1967.java'):
            continue
        if name.endswith('/QueueFixtures1967.java'):
            anchor = 'static boolean sessionBusy(){return busy;}'
            if body.count(anchor) != 1:raise AssertionError('Frozen queue peer seam changed')
            body = body.replace(anchor, anchor + 'static final class Session {} static boolean workspaceFits(long bytes){return FinishControl1986.workspace&&bytes>=0&&bytes<=512L*1024*1024;}')
        path = generated / name;path.parent.mkdir(parents=True, exist_ok=True);path.write_text(body);fixture_inputs.append(path)
    fixture_inputs += [source / name for name in (
        'tests/pipeline1942-fixtures/android/graphics/Bitmap.java',
        'tests/pipeline1942-fixtures/android/graphics/ColorSpace.java',
        'tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java',
        'tests1986/finish_baseline/FinishControl1986.java',
        'tests1984/qualification/QualificationProgress1984Test.java',
        'tests1984/qualification/CameraTrace1965.java')]
    fixture_inputs += [local / 'FinishFailures1988Test.java']
    cp = os.pathsep.join(map(str, (production, jar)))
    command('fixtures-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', cp, '-d', fixtures, *fixture_inputs])
    reports, adapters = {}, {}
    parser = module('finish88_reviewed_method_parser', source / 'host_finish_baseline1986.py')
    for label in ('published87', 'current88', 'fault-runtime', 'fault-linkage', 'fault-oom'):
        directory = work / label;directory.mkdir(exist_ok=True)
        base = restored if label == 'published87' else source
        chain, evidence = adapt_renderers1988((base / 'GpuChain1961.java').read_text(), source)
        (directory / 'GpuChain1961.java').write_text(chain)
        qualification = (base / 'GpuQualification1961.java').read_text()
        if label.startswith('fault-'):
            failure = {'fault-runtime': 'IllegalStateException', 'fault-linkage': 'LinkageError', 'fault-oom': 'OutOfMemoryError'}[label]
            for name in ('finishCpu1988', 'finishBegin1988', 'finishStage1988', 'finishFault1988', 'finishEnd1988'):
                start, brace, end = parser.method_span(qualification, name)
                qualification = qualification[:brace + 1] + '\n        throw new ' + failure + '("controlled optional stage failure");\n    }' + qualification[end:]
        (directory / 'GpuQualification1961.java').write_text(qualification)
        command(label + '-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', os.pathsep.join(map(str, (production, fixtures, jar))),
            '-d', directory, directory / 'GpuChain1961.java', directory / 'GpuQualification1961.java'])
        output = command(label + '-run', [jdk / 'bin/java', '-ea', '-Xmx768m', '-cp', os.pathsep.join(map(str, (fixtures, directory, production, jar))),
            'com.hiro.ulike.FinishFailures1988Test', label])
        report = json.loads(output.strip().splitlines()[-1])
        if report.get('status') != 'passed' or report.get('assertions', 0) <= 0:
            raise AssertionError('Passing executed Finish assertions missing: ' + label)
        reports[label] = report;adapters[label] = evidence
    # This additional generated class keeps the complete Finish GPU command
    # graph, including every bank, readback and release. Only the independent
    # CPU oracle and elapsed clock are controlled; peers inject named API
    # failures and deterministic rows without pretending to be native pixels.
    stage_graph, stage_peers = work / 'stage-graph', work / 'stage-peers'
    stage_graph.mkdir(exist_ok=True);stage_peers.mkdir(exist_ok=True)
    body = (source / 'GpuChain1961.java').read_text()
    original_control = parser.method_body(body, 'qualifyFinish1978')
    start, brace, end = parser.method_span(body, 'cpuFinishOwned1962')
    body = body[:brace + 1] + '\n        return FinishControl1986.cpu(corrected,rotation,width,height,outputPlan,refreshNoise);\n    }' + body[end:]
    start, brace, end = parser.method_span(body, 'qualifyFinish1978')
    control = body[start:end]
    if control != original_control:raise AssertionError('Stage graph adapter changed admission control')
    body = body[:start] + control.replace('System.nanoTime()', 'FinishControl1986.nanoTime()') + body[end:]
    (stage_graph / 'GpuChain1961.java').write_text(body)
    command('stage-graph-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', os.pathsep.join(map(str, (production, fixtures, jar))),
        '-d', stage_graph, stage_graph / 'GpuChain1961.java', source / 'GpuQualification1961.java'])
    command('stage-peers-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', os.pathsep.join(map(str, (production, fixtures, jar))),
        '-d', stage_peers, local / 'FinishStagePeers1988.java', local / 'FinishStages1988Test.java'])
    output = command('stage-graph-run', [jdk / 'bin/java', '-ea', '-Xmx768m', '-cp', os.pathsep.join(map(str, (stage_peers, fixtures, stage_graph, production, jar))),
        'com.hiro.ulike.FinishStages1988Test'])
    graph_report = json.loads(output.strip().splitlines()[-1])
    if graph_report.get('status') != 'passed' or graph_report.get('production_finish_stage_graph_faults1988_verified') is not True:
        raise AssertionError('Actual Finish stage graph faults were not exercised')
    reports['actual_finish_graph'] = graph_report
    after = {str(p.relative_to(source)) if p.is_relative_to(source) else str(p): sha(p) for p in tracked}
    if before != after:raise AssertionError('Reviewed sources or fixtures changed during Finish regression')
    result = dict(status='passed', assertions=sum(r['assertions'] for r in reports.values()), cases=reports,
        **{flag: True for flag in FLAGS},
        source_hashes=before, baseline87_source_sha256=BASELINE87, controlled_renderers=adapters,
        physical_android_tested=False, device_speedup_verified=False, actual_gpu_transport_executed=False,
        actual_production_qualification_control_executed=True, actual_production_queue_executed=True,
        actual_production_resident_proof_executed=True,
        fixture_scope='Renderer outcomes and elapsed clock are controlled in generated test copies. Real current admission, source digest, full ResidentProof rows, queue, retry, persistence and scalar terminal snapshots execute. Separate retained native gates verify pixel math; host fixtures are not Android speed evidence.')
    (work / 'report.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    (work / 'commands.json').write_text(json.dumps(commands, ensure_ascii=False, indent=2) + '\n')
    return result


run = test


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True);parser.add_argument('--work', required=True)
    parser.add_argument('--jdk');parser.add_argument('--ndk');args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
