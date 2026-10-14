#!/usr/bin/env python3
"""Verify the Java-only GPU bootstrap repair against all pinned .78 sources."""
from pathlib import Path
import hashlib
import json

NEW_TRANSFORMERS={'Transform1979.java','PatchClass1979.java','TimingCameraHooks1979.java'}

def sha(data):
    return hashlib.sha256(data).hexdigest()

def production(name):
    p=Path(name)
    return (len(p.parts)==1 and p.suffix=='.java' or name.startswith('quality-dependencies/')
            or p.parts[0].startswith('native') and p.parts[0][6:].isdigit()
            and p.suffix in ('.c','.h','.comp','.glsl','.py','.sh'))

def test(source,work):
    source,work=Path(source),Path(work)
    work.mkdir(parents=True,exist_ok=True)
    reference_path=source/'tests1979/source-scope78.json'
    reference=json.loads(reference_path.read_text())
    roots=json.loads((source/'production1979.json').read_text())
    allowed={name+'.java' for name in roots}
    changed,unchanged=[],[]
    for name,digest in reference.items():
        path=source/name
        if not path.is_file() or path.is_symlink():
            raise AssertionError('Published .78 source missing: '+name)
        if sha(path.read_bytes())==digest:
            unchanged.append(name)
        elif name in allowed:
            changed.append(name)
        else:
            raise AssertionError('Unreviewed .78 source or historical fixture edit: '+name)
    introduced={p.relative_to(source).as_posix() for p in source.rglob('*')
                if p.is_file() and not p.is_symlink() and '__pycache__' not in p.parts}-set(reference)
    new_fixtures={n for n in introduced if len(Path(n).parts)==1 and n.endswith('1979Test.java')}
    unexpected={n for n in introduced if production(n)}-allowed-NEW_TRANSFORMERS-new_fixtures
    if unexpected:
        raise AssertionError('Unreviewed new production source: '+repr(sorted(unexpected)))
    camera=(source/'CameraTrace1965.java').read_text()
    if camera.count('.getMethod("liveSummary1979")')!=1:
        raise AssertionError('Exactly one reviewed live-dialog retrieval required')
    restored=camera.replace('1.9.79','1.9.78').replace('.getMethod("liveSummary1979")','.getMethod("summary")')
    if sha(restored.encode())!=reference['CameraTrace1965.java']:
        raise AssertionError('Camera trace changed beyond version and one live-dialog method name')
    native=[n for n in unchanged if Path(n).parts[0].startswith('native') and production(n)]
    java=[n for n in unchanged if production(n) and n not in native]
    if not changed or len(reference)<1843 or not native or not java:
        raise AssertionError('Complete baseline scope evidence required')
    result={'status':'passed','baseline_ulike_version':'1.9.78','reference_sha256':sha(reference_path.read_bytes()),
            'reviewed_changed_sources':sorted(changed),'allowed_changed_sources':sorted(allowed),
            'unmodified_sources':sorted(unchanged),'java_sources':sorted(java),'native_sources':sorted(native),
            'reviewed_test_changes':[],'new_production_sources':sorted(n for n in introduced if n in allowed),
            'new_build_transformers':sorted(NEW_TRANSFORMERS),'new_test_fixtures':sorted(new_fixtures),
            'unmodified_native_shader_sources_byte_identical':True,'reviewed_native_scope_verified':True,
            'unmodified_java_sources_byte_identical':True,'unmodified_cpu_and_other_shader_sources':True,
            'camera_trace_version_only_preserved':False,'camera_trace_logging_methods_preserved':True,
            'timing_version_only_preserved':False,'reviewed_certificate_fixture_bytes_verified':True,
            'native_certificate_identity_requires_inherited_binary':True}
    (work/'source-scope.json').write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+'\n')
    return result
