#!/usr/bin/env python3
"""Bound all seven exact-output optimizations against the published .77 graph.

Changed implementations require fresh equivalence tests; this check only proves
that the change stays within the reviewed source and fixture inventory.
"""
from pathlib import Path
import hashlib
import json

NATIVE_EDITS = {
    'native1960/strong1960.comp', 'native1960/engine1960.c',
    'native1960/build_native1960.py', 'native1955/single_noise1955.c',
    'native1955/build_native1955.py',
}
NEW_TRANSFORMERS = {
    'Transform1978.java', 'PatchClass1978.java', 'PatchLoader1978.java',
    'TimingCameraHooks1978.java',
}


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def native(name):
    path = Path(name)
    return (path.parts[0].startswith('native') and path.parts[0][6:].isdigit()
            and path.suffix in ('.c', '.h', '.comp', '.glsl', '.py', '.sh'))


def production(name):
    path = Path(name)
    return (len(path.parts) == 1 and path.suffix == '.java'
            or name.startswith('quality-dependencies/') or native(name))


def test(source, work):
    source, work = Path(source), Path(work)
    work.mkdir(parents=True, exist_ok=True)
    reference_path = source / 'tests1978/source-scope77.json'
    reference = json.loads(reference_path.read_text())
    review_path = source / 'tests1978/reviewed-test-edits.json'
    review = json.loads(review_path.read_text())
    roots = json.loads((source / 'production1978.json').read_text())
    allowed = {name + '.java' for name in roots} | NATIVE_EDITS
    changed, unchanged, fixtures = [], [], []
    for name, digest in reference.items():
        path = source / name
        if not path.is_file() or path.is_symlink():
            raise AssertionError('Published77 source missing: ' + name)
        current = sha(path.read_bytes())
        if current == digest:
            unchanged.append(name)
            continue
        if name in review:
            row = review[name]
            if (row.get('baseline_sha256') != digest
                    or row.get('sha256') != current
                    or not row.get('reason')):
                raise AssertionError('Reviewed test edit differs: ' + name)
            if production(name) and not Path(name).name.endswith('Test.java'):
                raise AssertionError('Production edit cannot be classified as a fixture: ' + name)
            fixtures.append(name)
        elif name in allowed:
            changed.append(name)
        else:
            raise AssertionError('Unreviewed published77 source changed: ' + name)
    if set(review) != set(fixtures):
        raise AssertionError('Fixture review must describe exactly the changed fixtures')
    current = {p.relative_to(source).as_posix() for p in source.rglob('*')
               if p.is_file() and not p.is_symlink() and '__pycache__' not in p.parts}
    introduced = current - set(reference)
    new_fixtures = {name for name in introduced
                    if len(Path(name).parts) == 1 and name.endswith('1978Test.java')}
    unexpected = {name for name in introduced if production(name)} - allowed - NEW_TRANSFORMERS - new_fixtures
    if unexpected:
        raise AssertionError('Undeclared new production source: ' + repr(sorted(unexpected)))
    baseline_logger = (source / 'tests1978/CameraTrace1965.java').read_text()
    if (source / 'CameraTrace1965.java').read_text().replace('1.9.78', '1.9.77') != baseline_logger:
        raise AssertionError('Camera logger changed beyond release version')
    unmodified_java = [name for name in unchanged if production(name) and not native(name)]
    unmodified_native = [name for name in unchanged if native(name)]
    if not changed or not unmodified_java or not unmodified_native:
        raise AssertionError('Actual bounded optimization and preservation inventory required')
    result = {
        'status': 'passed', 'baseline_ulike_version': '1.9.77',
        'reference_sha256': sha(reference_path.read_bytes()),
        'reviewed_changed_sources': sorted(changed),
        'allowed_changed_sources': sorted(allowed),
        'unmodified_sources': sorted(unchanged),
        'java_sources': sorted(unmodified_java),
        'native_sources': sorted(unmodified_native),
        'reviewed_test_changes': sorted(fixtures),
        'reviewed_test_edits_sha256': sha(review_path.read_bytes()),
        'new_build_transformers': sorted(NEW_TRANSFORMERS),
        'new_test_fixtures': sorted(new_fixtures),
        'new_production_sources': sorted(name for name in introduced if name in allowed),
        'unmodified_native_shader_sources_byte_identical': True,
        'reviewed_native_scope_verified': True,
        'unmodified_java_sources_byte_identical': True,
        'unmodified_cpu_and_other_shader_sources': True,
        'camera_trace_version_only_preserved': True,
        'timing_version_only_preserved': False,
        'timing_diagnostic_additions_require_fresh_host_tests': True,
        'reviewed_certificate_fixture_bytes_verified': True,
        'optimized_file_equivalence_requires_fresh_host_tests': True,
    }
    (work / 'source-scope.json').write_text(json.dumps(result, sort_keys=True, indent=2) + '\n')
    return result
