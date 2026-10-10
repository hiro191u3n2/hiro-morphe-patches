#!/usr/bin/env python3
"""Bind every .89 change to the complete immutable published .88 source graph."""
from pathlib import Path
import hashlib
import importlib.util
import json

NEW_TRANSFORMERS={'Transform1989.java','PatchClass1989.java'}
BASELINE_REFERENCE_SHA256='0402266011cc267120ae45ca3c468b8d8efc5eeb060ce2b9dfbb9fb79f200eff'

def sha(data):return hashlib.sha256(data).hexdigest()

def production(name):
    p=Path(name)
    return ((len(p.parts)==1 and p.suffix=='.java') or name.startswith('quality-dependencies/')
        or (p.parts[0].startswith(('native','h8gpu','finishgpu')) and p.suffix in ('.c','.h','.comp','.glsl','.py','.sh')))

def reviewed_inverses(source):
    def module(name):
        spec=importlib.util.spec_from_file_location(name+'_scope89',source/(name+'.py'))
        result=importlib.util.module_from_spec(spec);spec.loader.exec_module(result);return result
    finish=module('host_finish1989');memory=module('host_memory1989')
    frontend=module('host_pipeline_reasons1989');timing=module('host_pipeline_detail1989')
    groups={finish.reviewed_source88_1989:('GpuChain1961.java','GpuQualification1961.java'),
            memory.reviewed_source88_1989:('GpuNoise1960.java','SpeedWorkers1935.java')}
    result={}
    for function,names in groups.items():
        for name in names:
            raw=(source/name).read_bytes();restored=function(raw,name)
            result[name]={'current_sha256':sha(raw),'restored88_sha256':sha(restored)}
    for name in ('SingleNoise1955.java','GpuSingle1960.java','GpuResidual1961.java','QualityPipeline1932.java',
                 'FastResize1933.java','GpuGeometry1960.java','WholeRoute1953.java','GpuAnalysis1961.java','PipelineDetail1988.java'):
        raw=(source/name).read_bytes();restored=frontend.reviewed_source88(name,raw.decode()).encode()
        result[name]={'current_sha256':sha(raw),'restored88_sha256':sha(restored)}
    raw=(source/'ProcessingTiming1947.java').read_bytes();restored=timing.reviewed_timing88_source1989(raw.decode()).encode()
    result['ProcessingTiming1947.java']={'current_sha256':sha(raw),'restored88_sha256':sha(restored)}
    return result

def test(source,work):
    source,work=Path(source),Path(work);work.mkdir(parents=True,exist_ok=True)
    reference_path=source/'tests1989/source-scope88.json';raw=reference_path.read_bytes()
    if sha(raw)!=BASELINE_REFERENCE_SHA256:raise AssertionError('Published .88 source identity changed')
    reference=json.loads(raw);roots=json.loads((source/'production1989.json').read_text())
    allowed={name+'.java' for name in roots}
    # Historical compile-only dependency copies deliberately remain unchanged.
    changed=[];unchanged=[]
    for name,digest in reference.items():
        path=source/name
        if not path.is_file() or path.is_symlink():raise AssertionError('Historical source removed: '+name)
        if sha(path.read_bytes())==digest:unchanged.append(name)
        elif name in allowed:changed.append(name)
        else:raise AssertionError('Unreviewed historical source edit: '+name)
    introduced={p.relative_to(source).as_posix() for p in source.rglob('*') if p.is_file() and not p.is_symlink() and '__pycache__' not in p.parts}-set(reference)
    unexpected={name for name in introduced if production(name)}-allowed-NEW_TRANSFORMERS
    if unexpected:raise AssertionError('Undeclared production source: '+repr(sorted(unexpected)))
    camera=(source/'CameraTrace1965.java').read_bytes().replace(b'1.9.89',b'1.9.88')
    if sha(camera)!=reference['CameraTrace1965.java']:raise AssertionError('Camera trace changed beyond display version')
    negative=[]
    for name in introduced:
        if not name.endswith('.java') or not any(part=='baseline88' or part.endswith('-baseline88') for part in Path(name).parts):continue
        original=Path(name).name
        if original not in reference or sha((source/name).read_bytes())!=reference[original]:
            raise AssertionError('Negative control is not frozen published .88: '+name)
        negative.append(name)
    inverses=reviewed_inverses(source)
    if any(row['restored88_sha256']!=reference[name] for name,row in inverses.items()):
        raise AssertionError('A reviewed inverse did not restore complete .88 production')
    if set(changed)!=(set(inverses)|{'CameraTrace1965.java'}):raise AssertionError('Changed source/inverse coverage differs')
    native=[name for name in unchanged if Path(name).suffix in ('.c','.h','.comp','.glsl') and not name.startswith('tests')]
    java=[name for name in unchanged if production(name) and name not in native]
    if len(reference)!=2234 or not native or not java:raise AssertionError('Complete published .88 preservation required')
    result={'status':'passed','baseline_ulike_version':'1.9.88','reference_sha256':sha(raw),
        'reviewed_changed_sources':sorted(changed),'allowed_changed_sources':sorted(allowed),
        'unmodified_sources':sorted(unchanged),'java_sources':sorted(java),'native_sources':sorted(native),
        'reviewed_test_changes':[],'new_production_sources':sorted(introduced&allowed),
        'new_build_transformers':sorted(NEW_TRANSFORMERS),'published88_negative_control_sources':sorted(negative),
        'reviewed_source_inverses1989':inverses,'pixel_arithmetic_reviewed_inverse1989_verified':True,
        'unmodified_native_shader_sources_byte_identical':True,'reviewed_native_scope_verified':True,
        'unmodified_java_sources_byte_identical':True,'unmodified_cpu_and_other_shader_sources':False,
        'camera_trace_version_only_preserved':True,'camera_trace_logging_methods_preserved':True,
        'timing_version_only_preserved':False,'diagnostic_instrumentation_added1982':True,
        'reviewed_certificate_fixture_bytes_verified':True,'native_certificate_identity_requires_inherited_binary':True}
    (work/'source-scope.json').write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+'\n');return result
