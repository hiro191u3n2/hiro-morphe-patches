#!/usr/bin/env python3
"""Execute current frontend and model admission with controlled GPU transport.
Real JNI pixel math is tested separately by host_model_math1990.py. Historical
frontcopy tests required permanent proof suppression; .91 explicitly replaces
that policy with bounded stage proof progress, retaining all CPU-pixel checks.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re,subprocess

def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
 candidates=[Path(os.environ.get('ULIKE1990_CHAIN_CLASSES','/missing')),work.parent/'chain/host_chain1978/classes',source.parent/'work/host1990-chain/host_chain1978/classes']
 base=next((p for p in candidates if (p/'com/hiro/ulike/SingleNoise1955.class').is_file()),None)
 if base is None:raise AssertionError('Run current chain test first or set ULIKE1990_CHAIN_CLASSES')
 transport=source/'tests1989/reasons/ReasonsTransport1989.java';body=transport.read_text()
 body=body.replace('static int variant(int p,int v)', 'static int strongExactPrepare1990(int mode,int variant){return 51+mode+variant*3;}\n    static boolean warmEnvironment1973(){return enabled;}\n    static int variant(int p,int v)')
 peer=work/'ReasonsTransport1991.java';peer.write_text(body)
 current=['GpuSingle1960.java','GpuResidual1961.java','SingleStage1981.java','SingleCpu1981.java','GpuSnapshotBudget1981.java','CpuExact1978.java','GpuAnalysis1961.java','StrongNoise1958.java','GpuModelDefault1990.java','GpuCertificates1990.java','GpuStartup1990.java','GpuQualification1961.java']
 inputs=[source/p for p in current]+[source/'tests1991/frontmodel/FrontModel1991Test.java',source/'tests1991/frontmodel/ModelRecovery1991Test.java']
 extra=[source/'tests1991/frontmodel/StartupModel1991Test.java',source/'host_startup1990.py']
 pins={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs+[transport]+extra}
 # Frontend readiness peers also satisfy the startup transport contract. They
 # never qualify real pixels and are absent from shipped production classes.
 harness=inputs[-2];adapted=work/harness.name
 body=harness.read_text().replace('class GpuStrongDefault1990 {static boolean value;static boolean ready(){return value;}}','class GpuStrongDefault1990 {static final String SUITE="strong-test";static boolean value;static boolean ready(){return value;}static void request1994(){}static boolean qualify(GpuQualification1961.Cancellation c){return value;}}').replace('class GpuFinishDefault1990 {static boolean value;static boolean ready(){return value;}}','class GpuFinishDefault1990 {static final String SUITE="finish-test";static boolean value;static boolean ready(){return value;}static boolean qualify(GpuQualification1961.Cancellation c){return value;}static void requestWide1995(){}static String summaryWide1995(){return "controlled finish readiness";}static boolean wideReady1993(){return value;}static String wideSuite1993(){throw new AssertionError("frontend readiness peer must not request finish certificate");}static boolean qualifyWide1995(GpuQualification1961.Cancellation c){throw new AssertionError("frontend readiness peer must not run finish pixels");}}')
 adapted.write_text(body);compile_inputs=[adapted if p==harness else p for p in inputs]+[peer]
 classes=work/'classes';classes.mkdir(exist_ok=True);results=[]
 commands=[('compile',[jdk/'bin/javac','--release','8','-cp',base,'-d',classes,*compile_inputs])]
 for scenario in ['single_cold','single_standard','residual_cold','residual_standard']:
  commands.append((scenario,[jdk/'bin/java','-ea','-Xmx512m','-cp',str(classes)+os.pathsep+str(base),'com.hiro.ulike.FrontModel1991Test',scenario]))
 commands.append(('model_recovery',[jdk/'bin/java','-ea','-Xmx512m','-cp',str(classes)+os.pathsep+str(base),'com.hiro.ulike.ModelRecovery1991Test']))
 for name,cmd in commands:
  done=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,timeout=120);(work/(name+'.log')).write_text(done.stdout+done.stderr)
  if done.returncode:raise AssertionError(name+'\n'+done.stdout+done.stderr)
  if name!='compile':results.append(json.loads(re.search(r'^RESULT (.*)$',done.stdout,re.M).group(1)))
 # Independently compile the real startup/certificate owners against the
 # declared .90 initialization peers, with .91 model/cache assertions.
 spec=importlib.util.spec_from_file_location('frontmodel_startup_fixtures',source/'host_startup1990.py');fixtures=importlib.util.module_from_spec(spec);spec.loader.exec_module(fixtures)
 startup_inputs=[]
 for name,body in fixtures.SOURCES.items():
  path=work/'startup-peers'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(body);startup_inputs.append(path)
 startup_classes=work/'startup-classes';startup_classes.mkdir(exist_ok=True)
 startup_inputs += [source/'GpuStartup1990.java',source/'GpuCertificates1990.java',extra[0]]
 commands=[('startup_compile',[jdk/'bin/javac','--release','8','-d',startup_classes,*startup_inputs])]
 for scenario in ['independent','busy','cache']:
  commands.append(('startup_'+scenario,[jdk/'bin/java','-ea','-cp',startup_classes,'com.hiro.ulike.StartupModel1991Test',scenario]))
 for name,cmd in commands:
  done=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,timeout=60);(work/(name+'.log')).write_text(done.stdout+done.stderr)
  if done.returncode:raise AssertionError(name+'\n'+done.stdout+done.stderr)
  if name!='startup_compile':results.append(json.loads(re.search(r'^RESULT (.*)$',done.stdout,re.M).group(1)))
 if pins!={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs+[transport]+extra}:raise AssertionError('Sources changed during front/model tests')
 result=dict(status='passed',assertions=sum(x['assertions'] for x in results),tests=results,source_sha256=pins,transport_is_controlled=True,physical_android_tested=False,device_speedup_verified=False)
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
