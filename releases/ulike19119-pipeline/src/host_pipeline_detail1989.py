#!/usr/bin/env python3
"""Execute current .89 timing/detail storage with exact frozen fixture adapters.

Only the expanded reason upper bound/stride and display versions are adapted in the old detail test.
The actual .89 facade and timing class execute in both the retained and new
tests. Exact source inverse data is used solely for preservation assertions.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import re
import shutil

REFERENCE_SHA='0402266011cc267120ae45ca3c468b8d8efc5eeb060ce2b9dfbb9fb79f200eff'
INVERSE_SHA='ada3cfa699e1335ed248708f62404b8d82c3c6ab192e3a505958f05cb4b86091'
REQUIRED_FLAGS=(
    'pipeline89_actual_reason_storage_and_prior_adoption_separation',
    'pipeline89_copy_request_acquired_and_actual_queue_decision',
    'pipeline89_new_owner_background_terminal_fault_isolation',
    'pipeline89_capture_identity_epoch_and_lock_order',
    'pipeline89_scalar_bounds_and_no_hot_publication',
    'pipeline89_frozen_detail_assertions_and_source_inverse',
)
EXPECTED_CASES={
    'expanded_actual_cpu_reasons_and_prior_handoff',
    'requested_closure_and_acquired_pixels_are_distinct',
    'queue_decisions_are_snapshots_not_requeries',
    'new_scalar_bounds_saturation_and_unknowns',
    'new_observers_background_terminal_and_failure_isolation',
    'capture_identity_epoch_overlap_and_no_reverse_lock',
    'parallel_owner_counts_and_existing_publication_only',
}

def sha(value):
    return hashlib.sha256(value if isinstance(value,bytes) else Path(value).read_bytes()).hexdigest()

def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path)
    value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

def reviewed_timing88_source1989(text):
    source=Path(__file__).resolve().parent
    inverse=source/'tests1989/timing-source-inverse.json'
    if sha(inverse)!=INVERSE_SHA:raise AssertionError('Reviewed .89 timing inverse changed')
    original=text
    for row in reversed(json.loads(inverse.read_text())):
        if text.count(row['new'])!=1:raise AssertionError('Current .89 timing addition differs from review')
        text=text.replace(row['new'],row['old'])
    reference=source/'tests1989/source-scope88.json'
    if sha(reference)!=REFERENCE_SHA:raise AssertionError('Published .88 graph changed')
    if sha(text.encode())!=json.loads(reference.read_text())['ProcessingTiming1947.java'] or text==original:
        raise AssertionError('Reviewed .89 inverse did not restore complete published .88 timing')
    return text

def adapt_harness1989(text):
    edits=(
        ('new int[]{-1,12,Integer.MAX_VALUE}','new int[]{-1,20,Integer.MAX_VALUE}'),
        ('pipelineReasons1988[12]','pipelineReasons1988[20]'),
        ('pipelineReasons1988.length==132','pipelineReasons1988.length==220'),
        ('pipelineReasons1988[3*12+2]','pipelineReasons1988[3*20+2]'),
        ('old.values.put("version","1.9.87")','old.values.put("version","1.9.88")'),
        ('"ULike v1.9.88 / 新しい撮影の計測待ち"','"ULike v1.9.89 / 新しい撮影の計測待ち"'),
    )
    original=text
    for old,new in edits:
        if text.count(old)!=(2 if old=='pipelineReasons1988[12]' else 1):
            raise AssertionError('Unexpected frozen reason-bound fixture anchor: '+old)
        text=text.replace(old,new)
    restored=text
    for old,new in reversed(edits):restored=restored.replace(new,old)
    if restored!=original:raise AssertionError('Historical assertion changed beyond reason bound/stride and two display versions')
    return text

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    reference_path=source/'tests1989/source-scope88.json'
    if sha(reference_path)!=REFERENCE_SHA:raise AssertionError('Frozen .88 source declaration changed')
    reference=json.loads(reference_path.read_text())
    frozen=('host_pipeline_detail1988.py','host_timing1982.py','host_timing1984.py','host_timing1985.py',
            'host_timing1986.py','Timing1982Test.java','tests1988/pipeline_detail/PipelineDetail1988Test.java',
            'tests1988/pipeline_detail/PipelineProviders1988.java','tests1988/pipeline_detail/compile_only/PipelineDetail1988.java')
    for name in frozen:
        if sha(source/name)!=reference[name]:raise AssertionError('Frozen detail fixture changed: '+name)
    old=module('detail89_frozen88_helpers',source/'host_pipeline_detail1988.py')
    timing88=reviewed_timing88_source1989((source/'ProcessingTiming1947.java').read_text())
    old.reviewed_timing87_source1988(timing88)
    def constants(body):
        values={}
        for line in re.findall(r'    static final int ([^;]+);',body):
            for token in line.split(','):
                name,value=token.strip().split('=');values[name.strip()]=int(value.strip())
        return values
    actual=constants((source/'PipelineDetail1988.java').read_text())
    inherited=constants((source/'tests1988/pipeline_detail/compile_only/PipelineDetail1988.java').read_text())
    if any(actual.get(name)!=value for name,value in inherited.items()):
        raise AssertionError('An inherited diagnostic ID changed')
    if set(actual.values())-set(range(-1,20)):raise AssertionError('Unbounded new scalar ID')
    jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent).resolve()
    retained=module('detail89_timing82',source/'host_timing1982.py')
    peers84=module('detail89_peers84',source/'host_timing1984.py')
    peers85=module('detail89_peers85',source/'host_timing1985.py')
    peers86=module('detail89_peers86',source/'host_timing1986.py')
    paths=[]
    for name,body in retained.STUBS.items():
        body=peers86.adapt_timing_peers1986(peers85.adapt_timing_peers1985(peers84.adapt_timing_peers1984(body)))
        anchor='final class GpuQualification1961 {'
        if anchor in body:body=old.once(body,anchor,anchor+'\n static boolean background(){return PipelineProviders1988.read(0);}\n')
        path=work/'fixtures'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(body);paths.append(path)
    original=(source/'Timing1982Test.java').read_text()
    if original.count('1.9.82')!=2:raise AssertionError('Frozen timing version literals changed')
    fixture=work/'fixtures/com/hiro/ulike/Timing1982Test.java';fixture.write_text(original.replace('1.9.82','1.9.89'));paths.append(fixture)
    original_harness=(source/'tests1988/pipeline_detail/PipelineDetail1988Test.java').read_text()
    harness=work/'fixtures/com/hiro/ulike/PipelineDetail1988Test.java'
    harness.write_text(adapt_harness1989(original_harness));paths.append(harness)
    providers=source/'tests1988/pipeline_detail/PipelineProviders1988.java'
    new_harness=source/'tests1989/timing/PipelineDetail1989Test.java'
    production=[source/'ProcessingTiming1947.java',source/'PipelineDetail1988.java']
    tracked=production+[new_harness,Path(__file__).resolve(),source/'tests1989/timing-source-inverse.json']+[source/name for name in frozen]
    before={str(path):sha(path) for path in tracked}
    classes=work/'classes';classes.mkdir(exist_ok=True)
    old.run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*production,providers,new_harness,*paths],work/'compile.log')
    results={}
    for label,class_name in [('retained88','PipelineDetail1988Test'),('current89','PipelineDetail1989Test')]:
        output=old.run([jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-cp',classes,'com.hiro.ulike.'+class_name],work/(label+'.log'))
        results[label]=peers85.parsed(output)
    if set(results['retained88']['tests'])!=old.EXPECTED_CASES or set(results['current89']['tests'])!=EXPECTED_CASES:
        raise AssertionError('A retained or new .89 detail scenario did not execute')
    bytecode=old.run([jdk/'bin/javap','-classpath',classes,'-c','-p','com.hiro.ulike.ProcessingTiming1947','com.hiro.ulike.PipelineDetail1988'],work/'bytecode.log')
    marker='final class com.hiro.ulike.PipelineDetail1988 {';boundary=bytecode.index(marker)
    timing_code,facade_code=bytecode[:boundary],bytecode[boundary:]
    verified=[]
    for name in ('pipelineDetail1988','pipelineCopy1988','pipelinePriorReason1989','pipelineCopyState1989'):
        body=old.method_code(timing_code,name)
        if 'monitorenter' not in body:raise AssertionError('Missing trace-owner monitor: '+name)
        if re.search(r'^\s*\d+:\s+(?:new|anewarray|newarray|multianewarray|invokedynamic)\b',body,re.M):
            raise AssertionError('Allocation in scalar sink: '+name)
        if any(not call.startswith('addWork1973:') for call in re.findall(r'// (?:InterfaceMethod|Method) ([^\n]+)',body)):
            raise AssertionError('Scalar sink queried a provider or published: '+name)
        verified.append(name)
    for name in ('foreground','current','owner','record','copy','priorReason1989','copyState1989'):
        body=old.method_code(facade_code,name)
        if re.search(r'^\s*\d+:\s+(?:new|anewarray|newarray|multianewarray|invokedynamic)\b',body,re.M):
            raise AssertionError('Allocation in facade: '+name)
        if any(token in body for token in ('StringBuilder','publish:','refreshViews:','status1967:','qualified:','restore:','schedule:')):
            raise AssertionError('Facade queried admission or published: '+name)
        verified.append(name)
    identity_code=old.method_code(timing_code,'captureId1989')
    if any(token in identity_code for token in ('monitorenter','invoke','new','latest')):
        raise AssertionError('Capture identity adds a lock/provider/allocation/latest fallback')
    verified.append('captureId1989')
    if before!={str(path):sha(path) for path in tracked}:raise AssertionError('Detail source changed during execution')
    result={'status':'passed','assertions':sum(r['assertions'] for r in results.values())+len(verified)+2,
            'tests':results,'physical_android_tested':False,'device_speedup_verified':False,
            'production_source_sha256':{p.name:sha(p) for p in production},'source_sha256':before,
            'timing88_preservation_sha256':sha(timing88.encode()),'bytecode_methods_verified':verified,
            'fixture_reason_bound_adapter':{'before_sha256':sha(original_harness.encode()),'after_sha256':sha(harness)},
            'scope':'Actual current timing and optional facade; controlled Android/background peers. Frozen prior assertions are retained with only reason upper bound/stride and two display versions adapted. New tests exercise identity, decision snapshots, amounts, scalar limits, failures, background/terminal exclusion and concurrent ownership. No Android GPU or speedup claim.',
            **{flag:True for flag in old.REQUIRED_FLAGS+REQUIRED_FLAGS}}
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--source',required=True);parser.add_argument('--work',required=True)
    parser.add_argument('--jdk');parser.add_argument('--ndk');args=parser.parse_args()
    print(json.dumps(test(args.source,args.work,args.jdk,args.ndk),ensure_ascii=False,indent=2))
