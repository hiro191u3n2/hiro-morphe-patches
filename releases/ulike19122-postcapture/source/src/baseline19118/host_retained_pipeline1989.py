#!/usr/bin/env python3
"""Exact compiler-only compatibility for the immutable .88 route-call suite.

Call prepare_command1989 before the inherited .88 compiler adapter. An already
adapted package-private reservation peer receives a distinct source filename so
that only generic optional declarations are appended by the inherited adapter.
The peer contents must equal the pinned old runner's exact transformation.

Two formerly UNKNOWN expectations now check the observed current branches; all
pixel, ownership, count, timing and published .87 negative assertions remain.
No production source or executable candidate is replaced by a preservation view.
"""
from pathlib import Path
import hashlib

RAW_PEER = 'f8b8793f9f2e7802d50b05745b4b3b183c48d7e82d4ffd96164d3f01b2bf9485'
ADAPTED_PEER = 'b83c9d67751dc33740cf158310d3c0fa793b9416aacb6f466a49ac7a5fc5e164'
RUNNER = '7364be8fa3e538bf6f392cb4f30a8563ec3d3718edbbb12ef53048ede557a868'
PINS = {
    'DetailCalls1988.java': '04e07dfc2f038a8f10e5a1b27299f266082e6a1fa398ab7f468ac83e41837ab9',
    'PipelineCalls1988Test.java': '5b8da7575c05a28a07dbbce14039800c96900d4ea8e912e33da3ad0ddabb93fb',
}
EDITS = {
    'DetailCalls1988.java': ((
        'trace.pipelineReasons1988[phase*12+reason]',
        'trace.pipelineReasons1988[phase*20+reason]'),),
    'PipelineCalls1988Test.java': ((
        'DetailCalls1988.reason(owner,PipelineDetail1988.FRONT_STRIP,PipelineDetail1988.UNKNOWN)==committed,"front CPU reason remains honestly unobserved"',
        'DetailCalls1988.reason(owner,PipelineDetail1988.FRONT_STRIP,PipelineDetail1988.UNAVAILABLE)==committed,"front CPU reason is the observed unavailable GPU branch"'), (
        'DetailCalls1988.selected(owner,PipelineDetail1988.CORRECTION_GEOMETRY,gpu?1:0,1,PipelineDetail1988.UNKNOWN);',
        'DetailCalls1988.selected(owner,PipelineDetail1988.CORRECTION_GEOMETRY,gpu?1:0,1,gpu?PipelineDetail1988.UNKNOWN:fallback?PipelineDetail1988.CANDIDATE_UNAVAILABLE:PipelineDetail1988.UNAVAILABLE);')),
}


def sha(body):
    return hashlib.sha256(body.encode() if isinstance(body, str) else body).hexdigest()


def _write(adapter, path, body, output_name, kind, changes):
    destination = adapter.work / 'pipeline-calls89' / sha(path.read_bytes()) / output_name
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.exists() and destination.read_text() != body:
        raise AssertionError('Inconsistent pinned pipeline compatibility source')
    destination.write_text(body)
    adapter.events.append(dict(kind=kind, original=str(path), before_sha256=sha(path.read_bytes()),
        after_sha256=sha(body), changes=changes, production_substitution=False))
    return destination


def prepare_command1989(adapter, command):
    values = list(map(str, command))
    if not values or not (Path(values[0]).name == 'javac' or 'com.sun.tools.javac.Main' in values):
        return values
    paths = [Path(v).resolve() for v in values if v.endswith('.java') and Path(v).is_file()]
    for path in paths:
        name = path.name
        if name not in PINS and name != 'AnalysisPeers1987.java':
            continue
        body = path.read_text()
        if name == 'AnalysisPeers1987.java':
            if sha(body) == RAW_PEER:
                continue  # The inherited adapter validates and adapts this exact raw peer.
            if sha(body) != ADAPTED_PEER:
                raise AssertionError('Unreviewed analysis reservation peer for pipeline calls')
            frozen = adapter.source / 'tests1987/analysis/AnalysisPeers1987.java'
            runner = adapter.source / 'host_pipeline_routes1988_detail.py'
            if sha(frozen.read_bytes()) != RAW_PEER or sha(runner.read_bytes()) != RUNNER:
                raise AssertionError('Frozen analysis peer or exact generating runner changed')
            generator = adapter.helper('host_pipeline_routes1988_detail')
            if generator._analysis_fixture(frozen.read_text()) != body:
                raise AssertionError('Peer is not the exact old runner metadata transformation')
            destination = _write(adapter, path, body, 'AnalysisMetadataPeers1988.java',
                'already_adapted_analysis_peer1989', ['rename_package_private_source_only_avoid_duplicate_metadata'])
        else:
            frozen = adapter.source / 'tests1988/pipeline_calls' / name
            baseline = frozen.read_text()
            if sha(baseline) != PINS[name]:
                raise AssertionError('Frozen pipeline assertion fixture changed: ' + name)
            updated = baseline
            for old, new in EDITS[name]:
                if updated.count(old) != 1:
                    raise AssertionError('Exact pipeline assertion compatibility anchor changed')
                updated = updated.replace(old, new, 1)
            inverse = updated
            for old, new in reversed(EDITS[name]):
                if inverse.count(new) != 1:
                    raise AssertionError('Pipeline assertion inverse anchor is not unique')
                inverse = inverse.replace(new, old, 1)
            if inverse != baseline:
                raise AssertionError('Unreviewed assertion changes remain')
            if body == updated:
                continue  # Exact idempotent already generated input only.
            if sha(body) != PINS[name] or body != baseline:
                raise AssertionError('Unreviewed pipeline assertion input: ' + name)
            destination = _write(adapter, path, updated, name, 'observed_pipeline_reason_expectation1989',
                ['reason_stride_12_to_20'] if name == 'DetailCalls1988.java' else
                ['front_unavailable_reason_from_disabled_transport', 'geometry_unavailable_or_attempted_candidate_reason'])
        values = [str(destination) if v.endswith('.java') and Path(v).resolve() == path else v for v in values]
    return values
