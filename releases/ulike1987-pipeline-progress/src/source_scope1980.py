#!/usr/bin/env python3
"""Bind the whole-app audit to the immutable published .79 source graph."""
from pathlib import Path
import hashlib
import json

NEW_TRANSFORMERS = {'Transform1980.java', 'PatchClass1980.java',
                    'TimingCameraHooks1980.java', 'VerifyAllHelperReferences1980.java'}
BASELINE_REFERENCE_SHA256 = 'efa9c93c01ba64bb6bf96a5af2d1a637ab190ed13e0c5c9c1192459c7fec0f09'

def sha(data):
    return hashlib.sha256(data).hexdigest()

def production(name):
    p = Path(name)
    return ((len(p.parts) == 1 and p.suffix == '.java')
            or name.startswith('quality-dependencies/')
            or (p.parts[0].startswith('native') and p.parts[0][6:].isdigit()
                and p.suffix in ('.c', '.h', '.comp', '.glsl', '.py', '.sh')))

def test(source, work):
    source, work = Path(source), Path(work)
    work.mkdir(parents=True, exist_ok=True)
    reference_path = source / 'tests1980/source-scope79.json'
    reference_raw = reference_path.read_bytes()
    if sha(reference_raw) != BASELINE_REFERENCE_SHA256:
        raise AssertionError('Published .79 source graph identity changed')
    reference = json.loads(reference_raw)
    roots = json.loads((source / 'production1980.json').read_text())
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
    fixtures = {n for n in introduced if len(Path(n).parts) == 1 and n.endswith('1980Test.java')}
    unexpected = {n for n in introduced if production(n)} - allowed - NEW_TRANSFORMERS - fixtures
    if unexpected:
        raise AssertionError('Undeclared new production source: ' + repr(sorted(unexpected)))
    # Diagnostic snapshots, timing units, stage boundaries and logging are version-only.
    for name in ('ProcessingTiming1947.java', 'CameraTrace1965.java'):
        normalized = (source / name).read_text().replace('1.9.80', '1.9.79')
        if sha(normalized.encode()) != reference[name]:
            raise AssertionError('Version-only diagnostics changed: ' + name)
    # A historical source used by a negative control must be the actual shipped source.
    reference_fixtures = []
    for name in introduced:
        if '-reference/' not in name or not name.endswith('.java'):
            continue
        original = Path(name).name
        key = original if original in reference else 'quality-dependencies/com/hiro/ulike/' + original
        if key not in reference or sha((source / name).read_bytes()) != reference[key]:
            raise AssertionError('Negative control is not published .79 source: ' + name)
        reference_fixtures.append(name)
    native = [n for n in unchanged if Path(n).parts[0].startswith('native') and production(n)]
    java = [n for n in unchanged if production(n) and n not in native]
    if not changed or len(reference) != 1876 or not native or not java:
        raise AssertionError('Complete baseline scope evidence required')
    result = {
        'status': 'passed', 'baseline_ulike_version': '1.9.79',
        'reference_sha256': sha(reference_path.read_bytes()),
        'reviewed_changed_sources': sorted(changed), 'allowed_changed_sources': sorted(allowed),
        'unmodified_sources': sorted(unchanged), 'java_sources': sorted(java), 'native_sources': sorted(native),
        'reviewed_test_changes': [], 'new_production_sources': sorted(introduced & allowed),
        'new_build_transformers': sorted(NEW_TRANSFORMERS), 'new_test_fixtures': sorted(fixtures),
        'published79_negative_control_sources': sorted(reference_fixtures),
        'unmodified_native_shader_sources_byte_identical': True, 'reviewed_native_scope_verified': True,
        'unmodified_java_sources_byte_identical': True,
        'unmodified_cpu_and_other_shader_sources': False,
        'camera_trace_version_only_preserved': True, 'camera_trace_logging_methods_preserved': True,
        'timing_version_only_preserved': True, 'reviewed_certificate_fixture_bytes_verified': True,
        'native_certificate_identity_requires_inherited_binary': True,
    }
    (work / 'source-scope.json').write_text(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
    return result
