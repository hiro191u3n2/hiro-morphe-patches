#!/usr/bin/env python3
"""Current production Finish lifetimes/queue; explicit native mock, no device speed claim."""
from pathlib import Path
import importlib.util,json,os,subprocess,sys

def mod(name,path):
 s=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m

def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);jar=Path(os.environ.get('ULIKE_ANDROID_JAR',str(jdk.parent/'android.jar')))
 def run(label,args):
  p=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=180);(work/(label+'.log')).write_text(p.stdout+p.stderr)
  if p.returncode:raise AssertionError(label+'\n'+(p.stdout+p.stderr)[-14000:])
  return p.stdout
 sys.path.insert(0,str(source));builder=mod('finish91_builder',source/'build1990.py');fixtures=mod('finish91_queue_peers',source/'host_qualification1981.py')
 production=work/'production';production.mkdir(exist_ok=True)
 run('production_compile',[jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-bootclasspath',jar,'-d',production,*builder.compile_inputs([]).values()])
 queuefiles=[];graphfiles=[]
 for name,body in fixtures.FIXTURES.items():
  if name.endswith('QueueHost1967.java'):continue
  if name.endswith('QueueFixtures1967.java'):
   body=body.replace('static long captureEpoch1953(){return epoch;}','static long captureEpoch1953(){return epoch;}static long captureId1989(long e){return e==epoch?1:0;}')
   graphbody='package com.hiro.ulike;\nfinal class SaveQueue1935 {static volatile boolean idle;static boolean idle1953(){return idle;}}\nfinal class WholeRoute1953 {static long retainedBytes(){return 0;}}'
  else:graphbody=body
  for family,text,files in [('queue',body,queuefiles),('graph',graphbody,graphfiles)]:
   p=work/family/'fixture-src'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);files.append(p)
 queue=work/'queue/classes';queue.mkdir(parents=True,exist_ok=True)
 run('queue_compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',queue,source/'GpuQualification1961.java',source/'tests1992/failure/Qualification1992Test.java',*queuefiles])
 q=json.loads(run('queue_run',[jdk/'bin/java','-ea','-cp',queue,'com.hiro.ulike.Qualification1992Test']).strip().splitlines()[-1])
 graph=work/'graph/classes';graph.mkdir(parents=True,exist_ok=True)
 graphfiles += [source/p for p in ['tests/pipeline1942-fixtures/android/graphics/Bitmap.java','tests/pipeline1942-fixtures/android/graphics/ColorSpace.java','tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java','tests1986/finish_baseline/FinishControl1986.java','tests1992/finish_memory/FinishStagePeers1992.java','tests1992/finish_memory/FinishMemory1992Test.java','tests1992/finish_memory/FinishRoute1992Test.java']]
 run('graph_compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',str(production)+os.pathsep+str(jar),'-d',graph,*graphfiles])
 g=json.loads(run('graph_run',[jdk/'bin/java','-ea','-Xmx768m','-cp',os.pathsep.join(map(str,[graph,production,jar])),'com.hiro.ulike.FinishMemory1992Test']).strip().splitlines()[-1])
 route=json.loads(run('route_run',[jdk/'bin/java','-ea','-Xmx768m','-cp',os.pathsep.join(map(str,[graph,production,jar])),'com.hiro.ulike.FinishRoute1992Test']).strip().splitlines()[-1])
 result={'status':'passed','assertions':q['assertions']+g['assertions']+route['assertions'],'queue':q,'finish':g,'route':route,'physical_android_tested':False,'device_speedup_verified':False}
 (work/'report.json').write_text(json.dumps(result,indent=2)+'\n');return result
run=test
if __name__=='__main__':
 import argparse
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),indent=2))
