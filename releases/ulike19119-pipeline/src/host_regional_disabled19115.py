#!/usr/bin/env python3
"""Shipped missing-helper behavior; actual GSD and unchanged extracted Stage hook.

Controlled peers prove selection, namespace and queue behavior. No JNI/GLES or
physical Android timing is executed. Distributed DEX omission is a separate gate.
"""
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess,time
ROOT=Path(__file__).resolve().parent
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def block(text,start):
 begin=text.index(start);opening=text.index('{',begin);depth=1;end=opening+1
 while depth:
  depth+=(text[end]=='{')-(text[end]=='}');end+=1
 return text[begin:end]
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve()
 if work.exists():raise AssertionError('Fresh missing-helper proof work directory required')
 work.mkdir(parents=True);jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or os.environ.get('JAVA_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
 local=source/'tests19115/disabled';gsd=source/'GpuStrongDefault1990.java';strong=source/'GpuStrong1960.java'
 fixture=[local/'DisabledPeers19115.java',local/'RegionalDisabled19115Test.java'];hook_fixture=[local/'DisabledHookPeers19115.java',local/'DisabledStageHook19115Test.java']
 tracked=[gsd,strong,source/'host_regional_disabled19115.py',*fixture,*hook_fixture];pins={str(p.relative_to(source)):sha(p) for p in tracked};commands=[];started=time.monotonic()
 def run(argv,name,expected=None):
  argv=list(map(str,argv));commands.append({'name':name,'argv':argv});p=subprocess.run(argv,text=True,capture_output=True,timeout=120);out=p.stdout+p.stderr;(work/(name+'.log')).write_text(out)
  if expected:
   if not p.returncode or 'java.lang.AssertionError' not in out or expected not in out:raise AssertionError(name+' did not reject intended contract:\n'+out[-6000:])
  elif p.returncode:raise AssertionError(name+' failed:\n'+out[-6000:])
  return out
 def compile_execute(prod,fixtures,name,main,expected=None):
  classes=work/name/'classes';classes.mkdir(parents=True)
  run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,prod,*fixtures],name+'-compile')
  if list(classes.rglob('GpuRegionalDefault19115*.class')):raise AssertionError('Helper leaked into missing-helper classpath')
  return run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-Xmx256m','-cp',classes,main],name,expected)
 main='com.hiro.ulike.RegionalDisabled19115Test';result=json.loads(compile_execute(gsd,fixture,'actual-gsd',main))
 original=gsd.read_text();state=block(original,'private static boolean regionalState19115(');request=block(original,'private static void requestRegional19115(')
 guard='if(u.length>23&&u[23]!=0&&(u[23]!=1||profile!=PROFILE||!regionalAccepts19115(u)))return false;'
 reservation='GpuQualification1961.schedule("strong-regional19115-v1",512,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){}public void close(){}});'
 mutations=[
  ('missing_state_fail_open',state,state.replace('catch(Throwable unavailable){return false;}','catch(Throwable unavailable){return true;}'),'missing helper readiness is false'),
  ('missing_request_prepares',request,request.replace('catch(Throwable unavailable){}','catch(Throwable unavailable){GpuNoise1960.prepareRegional19115();}'),'absent helper never prepares optional GPU programs'),
  ('missing_request_reserves',request,request.replace('catch(Throwable unavailable){}','catch(Throwable unavailable){'+reservation+'}'),'missing request hook is a no-op'),
  ('namespace_changed','return regionalFaster19115()?key+":regional115":key;','return key+":regional115";','missing helper preserves original FAST key exactly'),
  ('unproved_regional_marker',guard,'','regional output marker cannot commit without helper'),
  ('profile7_first','else Adaptive1998.request();','else Adaptive1994.request();','missing helper queues one legacy profile6 first'),
  ('same_capture_profile7','ProcessingTiming1947.captureEpoch1953()>completedEpoch','ProcessingTiming1947.captureEpoch1953()>=completedEpoch','same capture cannot start profile7 after profile6'),
  ('missing_timing_bridge','if(profile==PROFILE&&regionalFaster19115())return regionalBaseline19115(f,cancellation);','if(profile==PROFILE)return regionalBaseline19115(f,cancellation);','missing helper retains old complete-API timing reference'),
 ]
 controls=[]
 for name,before,after,expected in mutations:
  if before not in original or before==after:raise AssertionError('Missing mutation anchor '+name)
  folder=work/'mutations'/name;folder.mkdir(parents=True);mutant=folder/gsd.name;mutant.write_text(original.replace(before,after))
  compile_execute(mutant,fixture,'mutation-'+name,main,expected);controls.append({'name':name,'rejected':True,'expected_assertion':expected})
 hook=block(strong.read_text(),'private static final class RegionalHooks19115 {')
 wrapper='package com.hiro.ulike;\nfinal class GpuStrong1960 {\n'+hook+'\n}\n'
 hook_source=work/'hook-source/GpuStrong1960.java';hook_source.parent.mkdir();hook_source.write_text(wrapper)
 hook_main='com.hiro.ulike.DisabledStageHook19115Test';hook_result=json.loads(compile_execute(hook_source,hook_fixture,'actual-stage-hook',hook_main))
 ready=block(hook,'static boolean ready(');bad_ready=ready.replace('catch(Throwable unavailable){return false;}','catch(Throwable unavailable){return true;}')
 if ready==bad_ready:raise AssertionError('Missing Stage hook fail-open mutation anchor')
 mutant=work/'hook-mutation/GpuStrong1960.java';mutant.parent.mkdir();mutant.write_text(wrapper.replace(ready,bad_ready))
 expected='missing helper disables automatic Stage adoption';compile_execute(mutant,hook_fixture,'mutation-stage-fail-open',hook_main,expected);controls.append({'name':'stage_missing_fail_open','rejected':True,'expected_assertion':expected})
 if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Missing-helper proof input changed during execution')
 if result.get('assertions',0)<150 or result.get('scenarios',0)<15 or hook_result.get('stage_auto_adoption_count')!=0:raise AssertionError('Incomplete missing-helper coverage')
 result.update(assertions=result['assertions']+hook_result['assertions']+len(controls),stage_hook=hook_result,stage_auto_adoption_count=hook_result['stage_auto_adoption_count'],mutation_count=len(controls),mutation_controls=controls,source_sha256=pins,extracted_hook_sha256=hashlib.sha256(hook.encode()).hexdigest(),generated_wrapper_sha256=sha(hook_source),host_elapsed_seconds=round(time.monotonic()-started,3),controlled_gpu_peers=True,scope='Actual GSD compiled without helper; exact unchanged production Stage hook block in separate controlled wrapper. No full Stage/GPU execution. Distributed DEX exclusion is independently checked.')
 (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),indent=2))
