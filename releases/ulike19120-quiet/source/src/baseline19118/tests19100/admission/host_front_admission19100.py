#!/usr/bin/env python3
"""Execute real current stage/queue/worker/timing owners with controlled GPU
transport, memory availability and oracle timing. No host speed is device proof.
The inherited CPU pixel cases still run; all generated peers remain in work.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, subprocess, sys

def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();jar=Path(os.environ['ULIKE_ANDROID_JAR'])
    sys.path.insert(0,str(source));builder=module('front19100_production',source/'build1990.py')
    production=work/'production';production.mkdir(exist_ok=True)
    owned=[source/n for n in ['SingleStage1981.java','GpuSingle1960.java','GpuResidual1961.java','SingleNoise1955.java','QualityPipeline1932.java','ProcessingTiming1947.java','GpuSnapshotBudget1981.java','GpuQualification1961.java','SingleCpu1981.java','SpeedWorkers1935.java']]
    harness=source/'tests19100/admission/FrontProof19100Test.java'
    tracked=owned+[harness,Path(__file__).resolve()]
    pins={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    def run(name,args):
        r=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=180)
        (work/(name+'.log')).write_text(r.stdout+r.stderr)
        if r.returncode:raise AssertionError(name+'\n'+(r.stdout+r.stderr)[-20000:])
        match=re.search(r'^RESULT (.*)$',r.stdout,re.M)
        return json.loads(match.group(1)) if match else None
    run('production-compile',[jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-bootclasspath',jar,'-d',production,*builder.compile_inputs([]).values()])
    fixtures=module('front19100_android_peers',source/'host_qualification1981.py');files=[]
    for name,body in fixtures.FIXTURES.items():
        if not name.startswith('android/'):continue
        if name.endswith('SharedPreferences.java'):
            body=body.replace('String getString(String key,String fallback);','String getString(String key,String fallback);default long getLong(String key,long fallback){return fallback;}')
            body=body.replace('interface Editor {','interface Editor {default Editor putLong(String key,long value){return putString(key,Long.toString(value));}')
        p=work/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);files.append(p)
    transport=source/'tests1989/reasons/ReasonsTransport1989.java';body=transport.read_text()
    old='static int variant(int p,int v)';assert body.count(old)==1
    body=body.replace(old,'''static long ceiling19100=MAX_BYTES;
    static boolean unsupported1999(int p){return enabled&&(p==SINGLE&&!single||p==RESIDUAL1961&&!residual);}
    static int strongExactPrepare1990(int mode,int variant){return 51+mode+variant*3;}
    static boolean warmEnvironment1973(){return enabled;}
    static int variant(int p,int v)''')
    old='return budget&&bytes>=0&&bytes<=MAX_BYTES;';assert body.count(old)==1
    body=body.replace(old,'return budget&&bytes>=0&&bytes<=MAX_BYTES&&bytes<=ceiling19100;')
    peer=work/'ReasonsTransport19100.java';peer.write_text(body);files.append(peer)
    helpers=work/'FrontHelpers19100.java'
    helpers.write_text('''package com.hiro.ulike;
final class GpuStrongDefault1990 {static final String SUITE="strong-test";static boolean value=true;static boolean ready(){return value;}static void request1994(){}static boolean qualify(GpuQualification1961.Cancellation c){return value;}}
final class GpuFinishDefault1990 {static final String SUITE="finish-test";static boolean value=true;static boolean ready(){return value;}static boolean qualify(GpuQualification1961.Cancellation c){return value;}static void requestWide1995(){}static String summaryWide1995(){return "controlled finish readiness";}static boolean wideReady1993(){return value;}static String wideSuite1993(){return "controlled-wide";}}
''');files.append(helpers)
    legacy=source/'tests1991/frontmodel/FrontModel1991Test.java';body=legacy.read_text();body=body[:body.index('\nclass GpuStrongDefault1990')]
    legacycopy=work/legacy.name;legacycopy.write_text(body);files.append(legacycopy)
    files += [source/n for n in ['tests/pipeline1942-fixtures/android/graphics/Bitmap.java','tests/pipeline1942-fixtures/android/graphics/ColorSpace.java','tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java']]
    classes=work/'classes';classes.mkdir(exist_ok=True);cp=os.pathsep.join(map(str,[classes,production,jar]))
    # Recompile all modified execution owners against the declared peer API.
    run('focused-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',str(production)+os.pathsep+str(jar),'-d',classes,*owned,source/'PipelineDetail1988.java',source/'PipelineDiagnostics1982.java',source/'CpuExact1978.java',harness,*files])
    cases={}
    for mode in ['memory','copy-low','execute-low','exception','linkage','allocation','none','cancel','capture-change','reject-after','reference-change','commit-qualified','original-reject','qualified','interrupted','retained-limit','snapshot-limit','execution-limit','copy-active','copy-reserved','copy-budget','copy-turn','mismatch','slow','worker-trace']:
        cases[mode]=run(mode,[jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-Xmx512m','-cp',cp,'com.hiro.ulike.FrontProof19100Test',mode])
    for mode in ['single_cold','single_standard','residual_cold','residual_standard']:
        cases['retained-'+mode]=run('retained-'+mode,[jdk/'bin/java','-ea','-Xmx512m','-cp',cp,'com.hiro.ulike.FrontModel1991Test',mode])
    if pins!={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Frontend execution sources changed during focused test')
    report=dict(status='passed',assertions=sum(x['assertions'] for x in cases.values()),tests=cases,source_sha256=pins,
        foreground_snapshot_and_deferred_peak_separated=True,deferred_execution_memory_rechecked=True,
        snapshot_cancel_allocation_ownership_verified=True,copy_decision_reasons_preserved=True,
        actual_worker_owner_trace_isolated=True,unqualified_and_other_strip_gpu_rejected=True,
        retained_exact_and_speed_gates=True,retained_frontmodel_cases=True,
        transport_is_controlled=True,physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');return report

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args()
    print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),ensure_ascii=False,indent=2))
