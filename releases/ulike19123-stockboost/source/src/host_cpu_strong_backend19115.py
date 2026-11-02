#!/usr/bin/env python3
"""CPU backend observation: real work branches, controlled JNI, actual Stage owner.

The JNI fixture controls completion/failure; it is not the production numerical
kernel. Stage ownership is exercised separately with controlled scalar reports.
Neither scope estimates physical Android performance.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re,shutil,subprocess,time
ROOT=Path(__file__).resolve().parent
BASELINE_SHA='0adad39df071e47c58bc47d99e39ff1725ddb11c923ed3144c9c7aaaf74e33b4'
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def module(name,path):
 s=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
def block(text,start):
 at=text.index(start);opening=text.index('{',at);end=opening+1;depth=1
 while depth:depth+=(text[end]=='{')-(text[end]=='}');end+=1
 return text[at:end]
def normalized(body):
 body=body.replace('processRangeWork19115(', 'processRangeInternal(')
 body=body.replace('            if(!complete&&workspace.cpuOwner19115!=null)workspace.cpuFalse19115++;\n','')
 body=body.replace('if(complete){if(workspace.cpuOwner19115!=null)workspace.cpuResult19115=1;return;}','if(complete)return;')
 body=body.replace('if(complete){if(workspace.cpuOwner19115!=null)workspace.cpuResult19115=1;return true;}','if(complete)return true;')
 body=body.replace(';if(workspace.cpuOwner19115!=null)workspace.cpuLink19115++','')
 body=body.replace('        if(workspace.cpuOwner19115!=null)workspace.cpuResult19115=noise==0||!model.active()||end<=begin?3:2;\n','')
 body=body.replace('        if(workspace.cpuOwner19115!=null)workspace.cpuResult19115=noise==0||!model.active()||end<=begin?3:2;return true;','        return true;')
 body=body.replace("confidence);\n        return true;", "confidence);return true;")
 return body
def prove_bodies(current,baseline):
 for old,new in [('private static void processRangeInternal(', 'private static void processRangeWork19115('),('private static boolean gpuOracle1960(','private static boolean gpuOracle1960(')]:
  if normalized(block(current,new))!=block(baseline,old):raise AssertionError('CPU work body changed beyond scalar markers: '+new)
 wrapper=block(current,'private static void processRangeInternal(')
 if any(s in wrapper for s in ('clone(', 'arraycopy(', 'new int[','new float[','processNative(','processJavaRangeInternal(')):raise AssertionError('Observation wrapper added image work or a backend attempt')
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve()
 if work.exists():raise AssertionError('Fresh CPU backend proof directory required')
 work.mkdir(parents=True);jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or os.environ.get('JAVA_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
 local=source/'tests19115/cpu_backend';noise=source/'StrongNoise1958.java';strong=source/'GpuStrong1960.java';baseline=source/'tests19115/provenance/reference114/StrongNoise1958.java'
 legacy=['host_strong_timing1995.py','host_strong_peers19103.py','host_strong_diagnostics1986.py','host_qualification1981.py','tests1982/strong/StrongDiagnosticsFixtures1982.java','tests1982/strong/StrongDiagnostics1982Test.java','tests1990/strongroute/DefaultPeers1990.java','tests1990/strongroute/StrongDefaultRouting1990Test.java','GpuQualification1961.java','GpuStrongDefault1990.java']
 tracked=[noise,strong,baseline,source/'host_cpu_strong_backend19115.py',*sorted(local.glob('*')),*[source/n for n in legacy]];pins={str(p.relative_to(source)):sha(p) for p in tracked if p.is_file()};commands=[];started=time.monotonic()
 def run(argv,name,expected=None):
  argv=list(map(str,argv));commands.append({'name':name,'argv':argv});r=subprocess.run(argv,text=True,capture_output=True,timeout=180);out=r.stdout+r.stderr;(work/(name+'.log')).write_text(out)
  if 'WARNING in native method' in out or 'FATAL ERROR' in out:raise AssertionError(name+' JNI checker failed\n'+out)
  if expected:
   if not r.returncode or 'java.lang.AssertionError' not in out or expected not in out:raise AssertionError(name+' did not reject intended contract\n'+out[-8000:])
  elif r.returncode:raise AssertionError(name+' failed\n'+out[-10000:])
  return out
 if sha(baseline)!=BASELINE_SHA:raise AssertionError('Immutable CPU baseline changed')
 current=noise.read_text();old=baseline.read_text();prove_bodies(current,old)
 source_controls=[]
 native_statement=next(line for line in block(current,'private static void processRangeWork19115(').splitlines() if 'boolean complete=confidence==null?processNative(' in line)
 for name,before,after in [('extra_native_attempt',native_statement,native_statement.replace('boolean complete=', 'boolean extraComplete=')+'\n'+native_statement),('extra_copy','Object owner=workspace==null?', 'int[] copied=input.clone();Object owner=workspace==null?')]:
  if before not in current:raise AssertionError('Missing body mutation anchor')
  try:prove_bodies(current.replace(before,after,1),old)
  except AssertionError:source_controls.append({'name':name,'rejected':True})
  else:raise AssertionError('Body normalization accepted '+name)
 native=work/'native';native.mkdir();run(['cc','-std=c11','-O2','-shared','-fPIC','-Wall','-Wextra','-Werror','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),local/'CpuBackendControl19115.c','-o',native/'libulike_smooth1958.so'],'compile-controlled-jni')
 peer=local/'CpuBackendPeers19115.java';fixture=local/'CpuBackend19115Test.java'
 def compile_noise(prod,peers,folder,include_test=True):
  folder.mkdir(parents=True);run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',folder,prod,peers,*([fixture] if include_test else [])],folder.name+'-compile');return folder
 def noise_run(classes,label,expected=None,args=()):return run([jdk/'bin/java','-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx256m','-Djava.library.path='+str(native),'-cp',classes,'com.hiro.ulike.CpuBackend19115Test',*args],label,expected)
 classes=compile_noise(noise,peer,work/'cpu-classes');backend=json.loads(noise_run(classes,'cpu-backends'))
 absent=work/'absent/CpuBackendPeers19115.java';absent.parent.mkdir();peertext=peer.read_text()
 for name in ('public static Object beginCpuStrip19115(', 'public static void endCpuStrip19115('):peertext=peertext.replace(block(peertext,name),'')
 absent.write_text(peertext);missing=json.loads(noise_run(compile_noise(noise,absent,work/'absent-classes'),'missing-observer',args=('absent',)))
 baseline_classes=compile_noise(baseline,peer,work/'baseline-classes',False)
 def descriptors(cp,name):
  output=run([jdk/'bin/javap','-p','-s','-classpath',cp,name],'javap-'+cp.name+'-'+name.split('.')[-1]);lines=output.splitlines();found={}
  for i,line in enumerate(lines[:-1]):
   if '(' in line and name.split('.')[-1]+'(' in line:found[line.strip().split('(',1)[0].split()[-1]]=lines[i+1].strip()
   if 'processRangeInternal(' in line:found['processRangeInternal']=lines[i+1].strip()
  return found
 abi={};anon=lambda cp:sorted(p.stem for p in (cp/'com/hiro/ulike').glob('StrongNoise1958$*.class') if re.fullmatch(r'StrongNoise1958\$\d+',p.stem))
 if anon(classes)!=anon(baseline_classes):raise AssertionError('StrongNoise anonymous-class identity changed')
 for name in ['StrongNoise1958',*anon(classes)]:
  a=descriptors(classes,'com.hiro.ulike.'+name);b=descriptors(baseline_classes,'com.hiro.ulike.'+name)
  if a!=b:raise AssertionError('Published StrongNoise constructor/wrapper ABI changed '+name)
  abi[name]=a
 original=module('cpu_backend_legacy_timing',source/'host_strong_timing1995.py').test(source,work/'stage-original',jdk,ndk)
 stage_classes=work/'stage-original/classes';stage_fixtures=[local/'ResidentTiming19111.java',local/'CpuStageContext19115.java',local/'CpuStageOwnership19115Test.java']
 run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',stage_classes,'-d',stage_classes,*stage_fixtures],'compile-stage-ownership')
 stage_main='com.hiro.ulike.CpuStageOwnership19115Test'
 stage=json.loads(run([jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-cp',stage_classes,stage_main],'stage-ownership'))
 controls=[]
 noise_mutations=[
  ('native_completion','workspace.cpuResult19115=1;return;', 'workspace.cpuResult19115=2;return;','observed backend classification 1/0/0'),
  ('native_false','workspace.cpuFalse19115++;','workspace.cpuFalse19115+=0;','observed backend classification 2/1/0'),
  ('native_link','workspace.cpuLink19115++;','workspace.cpuLink19115+=0;','observed backend classification 2/0/1'),
  ('java_completion','noise==0||!model.active()||end<=begin?3:2','noise==0||!model.active()||end<=begin?3:1','observed backend classification 2/1/0'),
  ('incomplete','workspace.cpuResult19115):5','workspace.cpuResult19115):4','observed backend classification 5/1/0'),
  ('workspace_release','workspace.cpuOwner19115=null;','workspace.cpuOwner19115=owner;','workspace token cleared after completed call'),
 ]
 for name,before,after,expected in noise_mutations:
  if before not in current:raise AssertionError('Missing CPU mutation anchor '+name)
  folder=work/'mutations'/name;folder.mkdir(parents=True);p=folder/noise.name;p.write_text(current.replace(before,after))
  noise_run(compile_noise(p,peer,folder/'classes'),name,expected);controls.append({'name':name,'rejected':True,'expected_assertion':expected})
 stage_text=strong.read_text();stage_mutations=[
  ('duplicate_token','if(token.reported){stage.cpuCountsInvalid19115=true;return;}','', 'partial or invalid CPU coverage remains unobserved'),
  ('missing_finish','stage.cpuEnded19115!=stage.cpuStarted19115||','', 'partial or invalid CPU coverage remains unobserved'),
  ('missing_strip_coverage','stage.cpuStarted19115<stage.cpuStrips||','', 'partial or invalid CPU coverage remains unobserved'),
  ('wrong_model','stage!=active||stage.model!=model||!stage.cpuOracle1976||stage.closed','stage!=active||!stage.cpuOracle1976||stage.closed','wrong owner and model cannot contaminate valid counts'),
  ('foreground_token','stage.model!=model||!stage.cpuOracle1976)return null;','stage.model!=model)return null;','ordinary foreground and GPU benchmark are unobserved'),
 ]
 for name,before,after,expected in stage_mutations:
  if before not in stage_text:raise AssertionError('Missing Stage mutation anchor '+name)
  folder=work/'mutations'/name;folder.mkdir(parents=True);p=folder/strong.name;p.write_text(stage_text.replace(before,after));out=folder/'classes';out.mkdir()
  run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',stage_classes,'-d',out,p],name+'-compile')
  run([jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-cp',str(out)+os.pathsep+str(stage_classes),stage_main],name,expected);controls.append({'name':name,'rejected':True,'expected_assertion':expected})
 if pins!={str(p.relative_to(source)):sha(p) for p in tracked if p.is_file()}:raise AssertionError('CPU diagnostic proof source changed during execution')
 result=dict(status='passed',assertions=backend['assertions']+missing['assertions']+stage['assertions']+len(controls)+len(source_controls)+len(abi),scenarios=backend['scenarios']+missing['scenarios']+stage['scenarios'],backend=backend,observer_absent=missing,stage=stage,original_regression_assertions=original['assertions'],actual_native_return_executed=True,native_false_and_link_fallback_observed=True,stage_token_ownership_preserved=True,foreground_unobserved=True,wrapper_body_and_abi_preserved=True,controlled_jni_not_production_kernel=True,physical_android_tested=False,device_speedup_verified=False,mutation_count=len(controls),mutation_controls=controls,body_mutation_count=len(source_controls),body_mutation_controls=source_controls,anonymous_and_wrapper_abi=abi,source_sha256=pins,host_elapsed_seconds=round(time.monotonic()-started,3),scope='Actual StrongNoise and controlled JNI completion tested separately from actual GpuStrong ownership with controlled scalar reports; no combined pipeline/device timing claim.')
 (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),indent=2))
