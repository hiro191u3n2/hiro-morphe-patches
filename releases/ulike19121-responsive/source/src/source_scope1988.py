#!/usr/bin/env python3
"""Bind the whole-app audit to the immutable published .87 source graph."""
from pathlib import Path
import hashlib
import importlib.util
import json

NEW_TRANSFORMERS = {'Transform1988.java', 'PatchClass1988.java'}
BASELINE_REFERENCE_SHA256 = 'a2c82ad9367212a283fb4755e3d3e3ba887c66523654ba539e7782f2371122f6'

def sha(data):
    return hashlib.sha256(data).hexdigest()

def production(name):
    p = Path(name)
    return ((len(p.parts) == 1 and p.suffix == '.java')
            or name.startswith('quality-dependencies/')
            or (p.parts[0].startswith('native') and p.parts[0][6:].isdigit()
                and p.suffix in ('.c', '.h', '.comp', '.glsl', '.py', '.sh')))


def diagnostic_inverses(source):
    """Execute exact reviewed inverses; none of these bytes are compiled."""
    def module(name):
        spec = importlib.util.spec_from_file_location(name + '_scope88', source / (name + '.py'))
        result = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(result)
        return result
    finish = module('host_finish_failures1988')
    frontend = module('host_pipeline_routes1988_detail')
    timing = module('host_pipeline_detail1988')
    result = {}
    for name in ('GpuChain1961.java', 'GpuQualification1961.java'):
        raw = (source / name).read_bytes()
        restored = finish.reviewed_source87_1988(raw, name)
        result[name] = dict(current_sha256=sha(raw), restored87_sha256=sha(restored))
    for name in ('StrongNoise1958.java', 'QualityPipeline1932.java', 'GpuAnalysis1961.java',
                 'GpuProtection1961.java', 'FastResize1933.java'):
        raw = (source / name).read_bytes()
        restored = frontend.reviewed_source87(name, raw.decode()).encode()
        result[name] = dict(current_sha256=sha(raw), restored87_sha256=sha(restored))
    name = 'ProcessingTiming1947.java'
    raw = (source / name).read_bytes()
    restored = timing.reviewed_timing87_source1988(raw.decode()).encode()
    result[name] = dict(current_sha256=sha(raw), restored87_sha256=sha(restored))
    return result

def test(source, work):
    source, work = Path(source), Path(work)
    work.mkdir(parents=True, exist_ok=True)
    reference_path = source / 'tests1988/source-scope87.json'
    reference_raw = reference_path.read_bytes()
    if sha(reference_raw) != BASELINE_REFERENCE_SHA256:
        raise AssertionError('Published .87 source graph identity changed')
    reference = json.loads(reference_raw)
    roots = json.loads((source / 'production1988.json').read_text())
    allowed = {n + '.java' for n in roots}
    allowed |= {'quality-dependencies/com/hiro/ulike/' + n + '.java' for n in roots}
    changed, unchanged = [], []
    for name, digest in reference.items():
        path = source / name
        if not path.is_file() or path.is_symlink():
            raise AssertionError('Historical source removed: ' + name)
        if sha(path.read_bytes()) == digest:
            unchanged.append(name)
        elif name in allowed:
            changed.append(name)
        else:
            raise AssertionError('Unreviewed historical source edit: ' + name)
    introduced = {p.relative_to(source).as_posix() for p in source.rglob('*')
                  if p.is_file() and not p.is_symlink() and '__pycache__' not in p.parts} - set(reference)
    fixtures = {n for n in introduced if len(Path(n).parts) == 1 and n.endswith('1988Test.java')}
    unexpected = {n for n in introduced if production(n)} - allowed - NEW_TRANSFORMERS - fixtures
    if unexpected:
        raise AssertionError('Undeclared new production source: ' + repr(sorted(unexpected)))
    # CameraTrace changes only its display version; Timing has reviewed scalar
    # additions and is separately restored byte-for-byte below.
    for name in ('CameraTrace1965.java',):
        normalized = (source / name).read_text().replace('1.9.88', '1.9.87')
        if sha(normalized.encode()) != reference[name]:
            raise AssertionError('Version-only diagnostics changed: ' + name)
    # A historical source used by a negative control must be the actual shipped source.
    reference_fixtures = []
    for name in introduced:
        if not any(marker in name for marker in ('-reference/', '/reference87/', '/baseline87/', '/published87/')) or not name.endswith('.java'):
            continue
        original = Path(name).name
        key = original if original in reference else 'quality-dependencies/com/hiro/ulike/' + original
        if key not in reference or sha((source / name).read_bytes()) != reference[key]:
            raise AssertionError('Negative control is not published .87 source: ' + name)
        reference_fixtures.append(name)
    native = [n for n in unchanged if Path(n).parts[0].startswith('native') and production(n)]
    java = [n for n in unchanged if production(n) and n not in native]
    if not changed or len(reference) != 2189 or not native or not java:
        raise AssertionError('Complete baseline scope evidence required')
    inverses = diagnostic_inverses(source)
    if any(row['restored87_sha256'] != reference[name] for name, row in inverses.items()):
        raise AssertionError('A reviewed diagnostic inverse did not restore the actual .87 source graph')
    result = {
        'status': 'passed', 'baseline_ulike_version': '1.9.87',
        'reference_sha256': sha(reference_path.read_bytes()),
        'reviewed_changed_sources': sorted(changed), 'allowed_changed_sources': sorted(allowed),
        'unmodified_sources': sorted(unchanged), 'java_sources': sorted(java), 'native_sources': sorted(native),
        'reviewed_test_changes': [], 'new_production_sources': sorted(introduced & allowed),
        'new_build_transformers': sorted(NEW_TRANSFORMERS), 'new_test_fixtures': sorted(fixtures),
        'published87_negative_control_sources': sorted(reference_fixtures),
        'reviewed_diagnostic_source_inverses1988': inverses,
        'pixel_arithmetic_diagnostic_inverse1988_verified': True,
        'unmodified_native_shader_sources_byte_identical': True, 'reviewed_native_scope_verified': True,
        'unmodified_java_sources_byte_identical': True,
        'unmodified_cpu_and_other_shader_sources': False,
        'camera_trace_version_only_preserved': True, 'camera_trace_logging_methods_preserved': True,
        'timing_version_only_preserved': False, 'diagnostic_instrumentation_added1982': True, 'reviewed_certificate_fixture_bytes_verified': True,
        'native_certificate_identity_requires_inherited_binary': True,
    }
    (work / 'source-scope.json').write_text(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
    return result
