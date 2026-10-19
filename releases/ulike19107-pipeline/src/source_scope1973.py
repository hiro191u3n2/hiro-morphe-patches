#!/usr/bin/env python3
"""Verify reviewed source inverses and the untouched legacy shader branch.

The inverse inventory is independently reviewed before it is pinned. Its exact
old/new fragments reconstruct immutable .72 source bytes, including line counts
used for a second compile of the inherited GpuNoise/Qualification families.
"""
from pathlib import Path
import hashlib, json, re

ROOT = Path(__file__).resolve().parent

def sha(raw):
    return hashlib.sha256(raw).hexdigest()

def need(value, message):
    if not value:
        raise AssertionError(message)

def test(source, work):
    source, work = Path(source), Path(work)
    contract_path = source / 'tests1973/source-scope72.json'
    contract = json.loads(contract_path.read_text())
    need(contract['baseline_version'] == '1.9.72' and contract['baseline_mpp_sha256'] ==
         '9fc266478d38864f62edbe21490035e8411a2dc500375494adc732d4d50c6e6c', 'Source scope baseline differs')
    assertions = 2
    for name, digest in contract['unchanged_files'].items():
        need(sha((source / name).read_bytes()) == digest, 'Unchanged CPU/shader/builder source differs: ' + name)
        assertions += 1
    inverse_sources = work / 'inverse-src'
    inverse_sources.mkdir(parents=True, exist_ok=True)
    for name, row in contract['inverse_sources'].items():
        reference = (source / 'tests1973/reference72' / name).read_bytes()
        actual = (source / name).read_bytes()
        need(sha(reference) == row['baseline_sha256'] and sha(actual) == row['candidate_sha256'],
             'Reviewed source scope digest differs: ' + name)
        restored = actual.decode('utf-8')
        for hunk in reversed(row['hunks']):
            new, old = hunk['new'], hunk['old']
            need(new and restored.count(new) == 1, 'Exact unique inverse fragment absent: ' + name)
            restored = restored.replace(new, old, 1)
        need(restored.encode('utf-8') == reference, 'Source inverse failed: ' + name)
        if name in ('GpuNoise1960.java', 'GpuQualification1961.java'):
            (inverse_sources / name).write_bytes(reference)
        assertions += 2 + len(row['hunks'])
    camera = (source / 'CameraTrace1965.java').read_bytes()
    camera_ref = (source / 'tests1973/reference72/CameraTrace1965.java').read_bytes()
    need(camera.replace(b'1.9.73', b'1.9.72') == camera_ref, 'Camera trace differs beyond exact VERSION')
    assertions += 1
    baseline_shader = (source / 'tests1973/reference72/native1960/strong1960.comp').read_text()
    shader = (source / 'native1960/strong1960.comp').read_text()
    start = shader.index('#ifndef GX_IEEE_DIV73\n')
    end = shader.index('/* GX16/GX27:', start)
    legacy = shader[:start] + shader[end:]
    legacy, branches = re.subn(r'^#if GX_IEEE_DIV73\n.*?^#else\n(.*?)^#endif\n',
                               lambda match: match.group(1), legacy, flags=re.M | re.S)
    need(branches > 0 and legacy == baseline_shader, 'Existing program0–38 Strong shader source changed')
    need(sha(shader.encode()) != sha(baseline_shader.encode()), 'Exact division shader addition absent')
    assertions += 2
    timing = (source / 'ProcessingTiming1947.java').read_text()
    timing_ref = (source / 'tests1973/reference72/ProcessingTiming1947.java').read_text()
    def trace_body(text):
        return text.split('    public static final class Trace {\n', 1)[1].split('    public static final class Scope {\n', 1)[0]
    trace = trace_body(timing)
    for fragment in contract['timing_added_trace_declarations']:
        need(trace.count(fragment) == 1, 'Exact additive Timing trace declaration absent')
        trace = trace.replace(fragment, '', 1)
    need(trace == trace_body(timing_ref), 'Inherited Timing trace initialization/source changed')
    assertions += len(contract['timing_added_trace_declarations']) + 1
    result = {'status': 'passed', 'assertions': assertions,
        'source_scope_sha256': sha(contract_path.read_bytes()),
        'camera_trace_version_only_preserved': True,
        'legacy_shader_branch_source_preserved': True,
        'unchanged_cpu_and_other_shader_sources': True,
        'gpu_noise_reviewed_source_inverse_verified': True,
        'qualification_reviewed_source_inverse_verified': True,
        'timing_old_trace_initialization_source_preserved': True,
        'gpu_arithmetic_implementation_changed': True,
        'physical_android_tested': False}
    (work / 'source-scope1973-result.json').write_text(json.dumps(result, indent=2) + '\n')
    return result
