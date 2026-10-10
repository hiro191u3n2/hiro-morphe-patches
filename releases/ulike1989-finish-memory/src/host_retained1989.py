#!/usr/bin/env python3
"""Layer exact .89 compatibility on frozen .88 and earlier host regressions.

No shared Python module is monkey-patched. Generated tiny peers gain optional
diagnostic members only. Current production stays executable; strict reviewed
inverses are restricted to historical preservation reads. The one superseded
CPU-oracle count is adapted explicitly while the old negative is kept at two.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import inspect
import json
import os
import re

ROOT=Path(__file__).resolve().parent
REFERENCE_SHA='0402266011cc267120ae45ca3c468b8d8efc5eeb060ce2b9dfbb9fb79f200eff'

def sha(value):return hashlib.sha256(value if isinstance(value,bytes) else Path(value).read_bytes()).hexdigest()

def load_file(name,path):
    spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

legacy=load_file('retained89_immutable88_adapter',ROOT/'host_retained1988.py')

class Adapter(legacy.Adapter):
    def __init__(self,source,work):
        super().__init__(source,work)
        reference=self.source/'tests1989/source-scope88.json'
        if sha(reference)!=REFERENCE_SHA:raise AssertionError('Published .88 complete source graph changed')
        self.reference88=json.loads(reference.read_text());self.current_helpers={}
        if sha(self.source/'host_retained1988.py')!=self.reference88['host_retained1988.py']:
            raise AssertionError('Frozen .88 compatibility adapter changed')

    def current_helper(self,name):
        if name not in self.current_helpers:self.current_helpers[name]=load_file(name+'_inverse89',self.source/(name+'.py'))
        return self.current_helpers[name]

    def inverse89(self,name,text):
        is_bytes=isinstance(text,bytes);raw=text if is_bytes else text.encode()
        if sha(raw)==self.reference88[name]:return text
        if name in ('GpuChain1961.java','GpuQualification1961.java'):
            restored=self.current_helper('host_finish1989').reviewed_source88_1989(raw,name)
        elif name in ('GpuNoise1960.java','SpeedWorkers1935.java'):
            restored=self.current_helper('host_memory1989').reviewed_source88_1989(raw,name)
        elif name=='ProcessingTiming1947.java':
            restored=self.current_helper('host_pipeline_detail1989').reviewed_timing88_source1989(raw.decode()).encode()
        elif name=='CameraTrace1965.java':restored=raw.replace(b'1.9.89',b'1.9.88')
        else:
            restored=self.current_helper('host_pipeline_reasons1989').reviewed_source88(name,raw.decode()).encode()
        if sha(restored)!=self.reference88[name]:raise AssertionError('Reviewed .89 inverse did not restore .88 '+name)
        self.events.append({'kind':'preservation_only_inverse1989','source':name,'current_sha256':sha(raw),'restored88_sha256':sha(restored)})
        return restored if is_bytes else restored.decode()

    def preservation(self,original,tree):
        # Three additional .89 instrumented sources are part of the frozen .81
        # method contract. Restore only those reviewed spans for preservation;
        # the inherited .88 layer still restores its four frontend sources.
        # No executable current-production compile input uses this tree.
        tree=Path(tree).resolve()
        declaration_name='tests1982/pipeline/preserved81.json'
        declaration=json.loads((tree/declaration_name).read_text())
        destination=self.work/('preservation89-only-'+str(len(self.events)))
        for name in set(declaration['complete_files'])|set(declaration['methods'])|{declaration_name}:
            target=destination/name
            target.parent.mkdir(parents=True,exist_ok=True)
            if target.exists() or target.is_symlink():
                raise AssertionError('Preservation-only destination already exists')
            if name in ('SingleNoise1955.java','GpuSingle1960.java','GpuResidual1961.java'):
                target.write_text(self.inverse89(name,(tree/name).read_text()))
            else:
                target.symlink_to(tree/name)
        return super().preservation(original,destination)

    def helper(self,name):
        result=super().helper(name)
        if name+'.py' in self.reference88:self.install(result,self.source/(name+'.py'))
        return result

    def spec(self,name,location,*args,**kwargs):
        spec=importlib.util.spec_from_file_location(name,location,*args,**kwargs);path=Path(location).resolve()
        if path.parent==self.source and path.name in self.reference88 and path.suffix=='.py':
            if sha(path)!=self.reference88[path.name]:raise AssertionError('Frozen .88 runner changed: '+path.name)
            original=spec.loader;adapter=self
            class Loader:
                def create_module(self,requested):return original.create_module(requested)
                def exec_module(self,module):original.exec_module(module);adapter.install(module,path)
            spec.loader=Loader()
        return spec

    def install(self,module,path):
        if getattr(module,'_retained1989_installed',False):return
        module._retained1989_installed=True
        super().install(module,path)
        if path.name.startswith('build') and hasattr(module,'compile_inputs'):
            previous=module.compile_inputs
            def compile_inputs():
                values=dict(previous())
                values['com/hiro/ulike/FinishMemory1989.java']=self.source/'FinishMemory1989.java'
                return values
            module.compile_inputs=compile_inputs
        if path.name=='host_pipeline_routes1988_detail.py':
            previous=module.reviewed_source87
            module.reviewed_source87=lambda name,text:previous(name,self.inverse89(name,text))
        if path.name=='host_finish_failures1988.py':
            previous=module.reviewed_source87_1988
            module.reviewed_source87_1988=lambda raw,name:previous(self.inverse89(name,raw),name)
        if path.name=='host_pipeline_detail1988.py':
            previous=module.reviewed_timing87_source1988
            module.reviewed_timing87_source1988=lambda text:previous(self.inverse89('ProcessingTiming1947.java',text))

    def compile_command(self,command):
        command=self.current_helper('host_retained_pipeline1989').prepare_command1989(self,command)
        values=list(map(str,super().compile_command(command)))
        if not values or not (Path(values[0]).name=='javac' or 'com.sun.tools.javac.Main' in values):return values
        paths=[Path(v).resolve() for v in values if v.endswith('.java') and Path(v).is_file()]
        for path in paths:
            body=path.read_text();original=body;changes=[]
            if path.name=='PipelineDetail1988.java' and 'Compiler-only bridge for frozen historical math fixtures.' in body:
                actual=(self.source/'PipelineDetail1988.java').read_text()
                constants=re.findall(r'    static final int [^;]+;',actual)
                inherited=re.findall(r'    static final int [^;]+;',body)
                for declaration in constants:
                    if declaration not in inherited:
                        body=body.replace('    private PipelineDetail1988() {}','    private PipelineDetail1988() {}\n'+declaration,1)
                body=body.rsplit('}',1)[0]+'''    static void priorReason1989(ProcessingTiming1947.Trace trace,int phase,int reason,int units){}
    static void copyState1989(ProcessingTiming1947.Trace trace,int phase,int outcome,long requestedBytes,
        long acquiredPixelBytes,long nanos,int queueReason,long retryRemainingNanos,int retries,
        int queuedJobs,int runningJobs,long retainedBytes){}
}
'''
                changes.append('optional_facade_constants_and_sinks')
            def add_class(name,marker,addition):
                nonlocal body
                match=re.search(r'\bclass '+re.escape(name)+r'\s*\{',body)
                if match and marker not in body:
                    body=body[:match.end()]+'\n'+addition+body[match.end():];changes.append('optional_peer_'+name+'_'+marker)
            if 'PERSIST_LOCK' not in body:
                add_class('GpuQualification1961','finishShape1989(',
                    '    static void finishShape1989(int sw,int sh,int ow,int oh,int rotation){}\n'
                    '    static void finishOracle1989(boolean executed){}\n'
                    '    static void finishMemory1989(long[] values){}\n')
            if 'private static Trace latest;' not in body:
                add_class('ProcessingTiming1947','captureId1989(',
                    '    static long captureId1989(long expectedEpoch){return 0;}\n')
                if not re.search(r'\bclass\s+Trace\b',body):
                    add_class('ProcessingTiming1947','class Trace',
                        '    static final class Trace {}\n')
            if 'private static final ArrayDeque<Object> ARRAYS' not in body:
                add_class('SpeedWorkers1935','nativeRetainedBytes1989(',
                    '    static long nativeRetainedBytes1989(long[] values,int offset){return -1;}\n')
                add_class('SpeedWorkers1935','trimNativeIdle1989(',
                    '    static int trimNativeIdle1989(){return 0;}\n')
            # Old result-only analysis peers had no queue metadata to observe.
            # Unknown scalar fields preserve that fact and every existing body.
            match=re.search(r'\bclass QueueDecision1983\s*\{',body)
            if match and 'PERSIST_LOCK' not in body:
                depth=1;end=match.end()
                while depth:
                    if body[end]=='{':depth+=1
                    elif body[end]=='}':depth-=1
                    end+=1
                segment=body[match.end():end-1]
                addition=''
                for field,kind in [('reason','int'),('retries','int'),('queuedJobs','int'),('runningJobs','int'),('retryRemainingNanos','long'),('retainedBytes','long')]:
                    if not re.search(r'\b'+field+r'\b',segment):addition+='final '+kind+' '+field+'=-1;'
                if addition:
                    body=body[:match.end()]+addition+body[match.end():];changes.append('queue_decision_unknown_metadata')
            if path.name=='FinishFailures1988Test.java':
                old='FinishControl1986.cpuCalls==2'
                if old in body:
                    if body.count(old)!=1:raise AssertionError('Unexpected superseded Finish CPU count')
                    body=body.replace(old,'FinishControl1986.cpuCalls==(baseline?2:1)')
                    changes.append('all_terminal_cpu_oracle_1_current_2_published_negative')
            if body!=original:
                destination=self.work/'additive-peers89'/sha(original.encode())/path.name
                destination.parent.mkdir(parents=True,exist_ok=True)
                if destination.exists() and destination.read_text()!=body:raise AssertionError('Inconsistent .89 peer adapter')
                destination.write_text(body)
                values=[str(destination) if v.endswith('.java') and Path(v).resolve()==path else v for v in values]
                self.events.append({'kind':'additive_compatibility_peer1989','original':str(path),'before_sha256':sha(original.encode()),'after_sha256':sha(body.encode()),'changes':changes})
        paths=[Path(v).resolve() for v in values if v.endswith('.java') and Path(v).is_file()]
        cp=[]
        for option in ('-cp','-classpath','--class-path'):
            if option in values:cp += [Path(p) for p in values[values.index(option)+1].split(os.pathsep)]
        route_fixture=self.source/'tests/WholeRoute1953Test.java'
        if route_fixture in paths:
            # This fixed engine-only suite uses the optional facade solely for
            # inlined reason constants. Its historical closure has no timing
            # class; supply only the type required by the inert facade peer.
            if sha(route_fixture)!=self.reference88['tests/WholeRoute1953Test.java']:
                raise AssertionError('Frozen route fixture changed')
            if any(re.search(r'\bclass ProcessingTiming1947\b',p.read_text()) for p in paths) or any((p/'com/hiro/ulike/ProcessingTiming1947.class').is_file() for p in cp):
                raise AssertionError('Route type peer must not shadow an existing timing class')
            destination=self.work/'additive-peers89/route-type/ProcessingTiming1947.java'
            destination.parent.mkdir(parents=True,exist_ok=True)
            destination.write_text('package com.hiro.ulike; final class ProcessingTiming1947 {static final class Trace {}}\n')
            values.append(str(destination));self.events.append({'kind':'compiler_only_route_trace_type1989','sha256':sha(destination),'production_substitution':False})
        if any('FinishMemory1989.' in p.read_text() for p in paths) and not any(p.name=='FinishMemory1989.java' for p in paths) and not any((p/'com/hiro/ulike/FinishMemory1989.class').is_file() for p in cp):
            # Controlled renderer fixtures have no native scratch owner. This
            # declared peer retains their old injected workspace verdict. The
            # dedicated .89 stage and native tests execute the actual helper.
            destination=self.work/'additive-peers89/memory-control/FinishMemory1989.java'
            destination.parent.mkdir(parents=True,exist_ok=True)
            destination.write_text('package com.hiro.ulike; final class FinishMemory1989 {static boolean fits(long extra){return GpuNoise1960.workspaceFits(extra);}}\n')
            values.append(str(destination));self.events.append({'kind':'controlled_workspace_peer1989','sha256':sha(destination)})
        return values

def execute(name,source,work,jdk=None,ndk=None):
    adapter=Adapter(source,Path(work)/'compatibility89');runner=adapter.load(name)
    parameters=inspect.signature(runner.test).parameters
    arguments={key:value for key,value in (('jdk',jdk),('ndk',ndk)) if key in parameters}
    result=runner.test(source,Path(work)/'executed',**arguments)
    if not isinstance(result,dict) or result.get('status')!='passed' or result.get('assertions',0)<=0:
        raise AssertionError('Current production did not execute retained assertions: '+name)
    for filename,digest in adapter.loaded.items():
        if sha(Path(source)/filename)!=digest:raise AssertionError('Frozen runner changed during execution: '+filename)
    result['retained_compatibility1989']={'scope':'Current production with exact source-pinned historical fixture/observer seams. Strict inverse data is for preservation only. All-terminal oracle count changes to1 for current code and stays2 in the old negative. Dedicated .89 suites execute new reasoning/memory/owner paths directly.',
        'loaded_frozen_runner_sha256':adapter.loaded,'events':adapter.events,'adapter_sha256':sha(__file__)}
    Path(work).mkdir(parents=True,exist_ok=True);(Path(work)/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--runner',required=True);parser.add_argument('--source',required=True);parser.add_argument('--work',required=True)
    parser.add_argument('--jdk');parser.add_argument('--ndk');args=parser.parse_args()
    result=execute(args.runner,args.source,args.work,args.jdk,args.ndk)
    print(json.dumps({'status':result['status'],'assertions':result['assertions'],'events':len(result['retained_compatibility1989']['events'])}))
