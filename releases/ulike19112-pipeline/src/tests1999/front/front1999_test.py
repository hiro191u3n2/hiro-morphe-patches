#!/usr/bin/env python3
"""Current frontend admission, bounded snapshot ownership and retained .91 cases.
GPU transport is explicit and controlled; arithmetic/production admission owners
are current source. The native residual coefficient preparation is unchanged.
"""
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    candidates=[Path(os.environ.get('ULIKE1990_CHAIN_CLASSES','/missing')),work.parent/'chain/host_chain1978/classes',work.parent/'host1990-chain/host_chain1978/classes']
    base=next((p for p in candidates if (p/'com/hiro/ulike/SingleNoise1955.class').is_file()),None)
    if base is None:raise AssertionError('Current chain classes required; set ULIKE1990_CHAIN_CLASSES')
    transport=source/'tests1989/reasons/ReasonsTransport1989.java'
    body=transport.read_text().replace('static int variant(int p,int v)', '''static boolean knownFalse1999;
    static boolean unsupported1999(int p){return knownFalse1999&&enabled&&(p==SINGLE&&!single||p==RESIDUAL1961&&!residual);}
    static int strongExactPrepare1990(int mode,int variant){return 51+mode+variant*3;}
    static boolean warmEnvironment1973(){return enabled;}
    static int variant(int p,int v)''')
    peer=work/'ReasonsTransport1999.java';peer.write_text(body)
    legacy=source/'tests1991/frontmodel/FrontModel1991Test.java'
    body=legacy.read_text().replace('class GpuStrongDefault1990 {static boolean value;static boolean ready(){return value;}}','class GpuStrongDefault1990 {static final String SUITE="strong-test";static boolean value;static boolean ready(){return value;}static void request1994(){}static boolean qualify(GpuQualification1961.Cancellation c){return value;}}').replace('class GpuFinishDefault1990 {static boolean value;static boolean ready(){return value;}}','class GpuFinishDefault1990 {static final String SUITE="finish-test";static boolean value;static boolean ready(){return value;}static boolean qualify(GpuQualification1961.Cancellation c){return value;}static void requestWide1995(){}static String summaryWide1995(){return "controlled finish readiness";}static boolean wideReady1993(){return value;}static String wideSuite1993(){throw new AssertionError("unexpected finish certificate");}static boolean qualifyWide1995(GpuQualification1961.Cancellation c){throw new AssertionError("unexpected finish pixels");}}')
    legacy_copy=work/legacy.name;legacy_copy.write_text(body)
    names=['GpuSingle1960','GpuResidual1961','SingleNoise1955','SingleStage1981','SingleCpu1981','GpuSnapshotBudget1981','CpuExact1978','PipelineDetail1988','GpuQualification1961']
    current=[source/(name+'.java') for name in names]
    harness=source/'tests1999/front/FrontAdmission1999Test.java'
    native=source/'native1960/residual_blocks1961.c'
    support=source/'tests1999/front/FrontSupport1999Test.java'
    tracked=current+[source/'GpuNoise1960.java',support,transport,legacy,harness,native,source/'native1955/single_noise1955.c',Path(__file__).resolve()]
    before={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    classes=work/'classes';classes.mkdir(exist_ok=True);reports=[]
    def run(name,cmd):
        done=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,timeout=180)
        (work/(name+'.log')).write_text(done.stdout+done.stderr)
        if done.returncode:raise AssertionError(name+'\n'+done.stdout+done.stderr)
        match=re.search(r'^RESULT (.*)$',done.stdout,re.M)
        if match:reports.append(json.loads(match.group(1)))
    run('compile',[jdk/'bin/javac','--release','8','-cp',base,'-d',classes,*current,peer,legacy_copy,harness])
    library=work/'libresidual1999_test.so'
    run('native-compile',['cc','-std=c11','-O2','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),native,'-lm','-o',library])
    cp=str(classes)+os.pathsep+str(base)
    for mode in ['single_cold','single_standard','residual_cold','residual_standard']:
        run('retained-'+mode,[jdk/'bin/java','-ea','-Xmx512m','-cp',cp,'com.hiro.ulike.FrontModel1991Test',mode])
    for mode in ['single','residual','reasons','proof','proof-cancel','proof-qualified','proof-exception','proof-interrupted','proof-nullmodel','proof-budget']:
        run(mode,[jdk/'bin/java','-ea','-Xmx512m','-cp',cp,'com.hiro.ulike.FrontAdmission1999Test',mode,library])
    support_classes=work/'support-classes';support_classes.mkdir(exist_ok=True)
    run('support-compile',[jdk/'bin/javac','--release','8','-cp',base,'-d',support_classes,source/'GpuNoise1960.java',support])
    run('real-support',[jdk/'bin/java','-ea','-cp',str(support_classes)+os.pathsep+str(base),'com.hiro.ulike.FrontSupport1999Test'])
    if before!={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Production sources changed during frontend tests')
    result=dict(status='passed',assertions=sum(r['assertions'] for r in reports),tests=reports,source_sha256=before,
        front1999_expired_admission_rejected=True,front1999_requalified_gpu_admitted=True,
        front1999_stage_reserved_before_copy=True,front1999_cached_support_reason=True,
        retained_frontmodel1991_cases=True,physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args()
    print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
