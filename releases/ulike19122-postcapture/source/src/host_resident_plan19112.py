#!/usr/bin/env python3
"""Current full-source resident planner and unchanged .111 scheduling oracle.

Controlled resource observations cover concurrency and capacity edges. The
actual SpeedWorkers owner is exercised; no physical Android speed is inferred.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess,sys

def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);sys.path.insert(0,str(source))
 build=module('resident_plan112_build',source/'build19101.py')
 android=Path(os.environ['ULIKE_ANDROID_JAR']).resolve()
 if hashlib.sha256(android.read_bytes()).hexdigest()!=build.TOOL_PINS['android.jar']:raise AssertionError('Pinned Android compile API required')
 fixture=source/'tests19112/plan/ResidentPlan19112Test.java'
 baseline=source/'tests19112/plan/baseline111/QualityPipeline1932.java'
 raw=baseline.read_bytes()
 if hashlib.sha256(raw).hexdigest()!=BASELINE_SHA256:raise AssertionError('Published .111 planner source changed')
 original=raw.decode();start=original.index('            int selected = Math.min(256,Scheduling1944.coreRows(width,height,halo,upper,budget));',original.index('private static SmoothRegions1958 smoothNoiseInPlace1976'))
 end=original.index('            residentTiming19111("strongPlan"',start)
 block=original[start:end]
 block=block.replace('int selected = Math.min(256,Scheduling1944.coreRows(width,height,halo,upper,budget));','int selected = initialRows;')
 block=block.replace('            selected=GpuStrongLayout1976.selectRows(width,height,level,shadows,captureSmooth,plan==null?0:plan.beautyQ8,plan==null?0:plan.shadowBudgetQ8,selected);\n','')
 block=block.replace('SpeedWorkers1935.availableWorkers1944()','available')
 oracle=work/'PublishedStrongPlan111.java'
 oracle.write_text('package com.hiro.ulike; final class PublishedStrongPlan111 {static long[] choose(int width,int height,int halo,int initialRows,int upper,int available,long budget){final long shared=(long)width*halo*4;\n'+block+'return new long[]{core,count,perWorker};}}\n')
 # Compile the same complete production dependency graph used by the MPP
 # build. This planner invokes no Android implementation; framework classes
 # supply signatures only, and the resource seam is explicitly controlled.
 sources=list(dict.fromkeys(list(build.compile_inputs([]).values())+[fixture,oracle]))
 preserved=[baseline,Path(__file__).resolve(),source/'resident_plan_inverse19112.py',source/'tests19112/plan/plan-inverse.json']
 pins={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources+preserved}
 commands=[]
 def run(label,args):
  args=list(map(str,args));commands.append({'label':label,'argv':args});r=subprocess.run(args,capture_output=True,text=True,timeout=180);output=r.stdout+r.stderr;(work/(label+'.log')).write_text(output)
  if r.returncode:raise AssertionError(label+'\n'+output[-16000:])
  return output
 classes=work/'classes';classes.mkdir(exist_ok=True)
 run('compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',android,'-d',classes,*sources])
 output=run('planner',[jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-Xmx256m','-cp',str(classes)+os.pathsep+str(android),'com.hiro.ulike.ResidentPlan19112Test'])
 result=json.loads(next(line[7:] for line in output.splitlines() if line.startswith('RESULT ')))
 if result.get('planner_cases')!=4000:raise AssertionError('Planner coverage incomplete')
 inverse=module('resident_plan112_inverse',source/'resident_plan_inverse19112.py').test(source)
 result['assertions']+=inverse['assertions'];result['source_inverse']=inverse
 result['resident_plan_source_inverse19112']=inverse['resident_plan_source_inverse19112']
 if pins!={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources+preserved}:raise AssertionError('Production source changed during resident planner test')
 result.update(source_sha256=pins,commands=commands,published111_sha256=BASELINE_SHA256)
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

BASELINE_SHA256='a10406bc6f19255decf3426aafb46254229d2d73e309502a44348898a7744368'
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
