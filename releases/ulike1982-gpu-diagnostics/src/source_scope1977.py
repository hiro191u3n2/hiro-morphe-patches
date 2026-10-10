#!/usr/bin/env python3
"""Allow only the .77 qualification repair and version-only diagnostics over .76."""
from pathlib import Path
import hashlib, json

def sha(raw):
    return hashlib.sha256(raw).hexdigest()

JAVA_EDITS = {'GpuQualification1961.java', 'ProcessingTiming1947.java', 'CameraTrace1965.java'}
NATIVE_EDITS = set()
NEW_TRANSFORMERS = {'Transform1977.java', 'PatchClass1977.java', 'PatchLoader1977.java', 'TimingCameraHooks1977.java'}
NEW_TEST_FIXTURES = {'Timing1977Test.java'}

def production(name):
    path = Path(name)
    return ((len(path.parts) == 1 and path.suffix == '.java')
        or name.startswith('quality-dependencies/')
        or (path.parts[0].startswith('native') and path.parts[0][6:].isdigit()
            and path.suffix in ('.c', '.h', '.comp', '.glsl', '.py', '.sh')))

def test(source, work):
    source, work = Path(source), Path(work)
    work.mkdir(parents=True, exist_ok=True)
    reference_path = source / 'tests1977/source-scope76.json'
    reference = json.loads(reference_path.read_text())
    test_review_path = source / 'tests1977/reviewed-test-edits.json'
    test_review = json.loads(test_review_path.read_text())
    if set(test_review) != {'PreferredCertificate1973Test.java'}:
        raise AssertionError('Only the explicitly reviewed certificate fixture may differ')
    for name, row in test_review.items():
        if row.get('baseline_sha256') != reference.get(name) or row.get('sha256') != sha((source / name).read_bytes()):
            raise AssertionError('Reviewed certificate fixture bytes differ: ' + name)
    changed, unchanged_java, unchanged_native, changed_tests = [], [], [], []
    for name, digest in reference.items():
        if not production(name):
            continue
        path = source / name
        if not path.is_file() or path.is_symlink():
            raise AssertionError('Published76 production source missing: ' + name)
        if sha(path.read_bytes()) != digest:
            if name in test_review:
                changed_tests.append(name)
                continue
            if name not in JAVA_EDITS:
                raise AssertionError('Unreviewed published76 source changed: ' + name)
            changed.append(name)
        else:
            (unchanged_native if name.startswith('native') else unchanged_java).append(name)
    current = {p.relative_to(source).as_posix() for p in source.rglob('*')
        if p.is_file() and not p.is_symlink() and production(p.relative_to(source).as_posix())}
    unexpected = current - set(reference) - NEW_TRANSFORMERS - NEW_TEST_FIXTURES
    if unexpected:
        raise AssertionError('Unexpected new production source: ' + repr(sorted(unexpected)))
    for name in ('CameraTrace1965.java', 'ProcessingTiming1947.java'):
        original = (source / 'tests1977' / name).read_text()
        if (source / name).read_text().replace('1.9.77', '1.9.76') != original:
            raise AssertionError(name + ' changed beyond version')
    if set(changed) != JAVA_EDITS or not unchanged_java or not unchanged_native:
        raise AssertionError('Exact three-source recovery scope required: ' + repr(sorted(changed)))
    result = {'status': 'passed', 'baseline_ulike_version': '1.9.76',
        'unmodified_native_shader_sources_byte_identical': True,
        'reviewed_native_scope_verified': True, 'unmodified_java_sources_byte_identical': True,
        'unmodified_cpu_and_other_shader_sources': True, 'camera_trace_version_only_preserved': True, 'timing_version_only_preserved': True,
        'reference_sha256': sha(reference_path.read_bytes()), 'native_sources': unchanged_native,
        'java_sources': unchanged_java, 'reviewed_changed_sources': sorted(changed),
        'allowed_changed_sources': sorted(JAVA_EDITS),
        'reviewed_test_changes': sorted(changed_tests),
        'reviewed_test_edits_sha256': sha(test_review_path.read_bytes()),
        'reviewed_certificate_fixture_bytes_verified': True, 'new_build_transformers': sorted(NEW_TRANSFORMERS), 'new_test_fixtures': sorted(NEW_TEST_FIXTURES),
        'optimized_file_equivalence_requires_fresh_host_tests': True}
    (work / 'source-scope.json').write_text(json.dumps(result, sort_keys=True, indent=2) + '\n')
    return result
