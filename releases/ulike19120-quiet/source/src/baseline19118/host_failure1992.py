#!/usr/bin/env python3
"""Actual typed failure producer/queue regressions with declared transport peers.
The .91 frontend/model coverage is retained; .92 adds real null/exception/linkage/
memory/cancellation dispatch, unchanged fairness, actual measured threshold logs.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess

def mod(name,path):
 spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();results=[]
 tracked=[source/p for p in ['GpuQualification1961.java','GpuAnalysis1961.java','GpuSingle1960.java','GpuResidual1961.java','SingleStage1981.java','SingleCpu1981.java','CpuExact1978.java','ColourCache1976.java','CpuFinishCache1976.java','GpuStrongLayout1976.java','GpuStrongRouting1978.java','host_failure1992.py']]+sorted((source/'tests1992/failure').glob('*.java'))
 pins={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
 def run(label,args):
  p=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=180);(work/(label+'.log')).write_text(p.stdout+p.stderr)
  if p.returncode:raise AssertionError(label+'\n'+(p.stdout+p.stderr)[-14000:])
  return p.stdout
 fixtures=mod('failure92_queue_fixtures',source/'host_qualification1981.py');files=[]
 for name,body in fixtures.FIXTURES.items():
  if name.endswith('QueueHost1967.java'):continue
  if name.endswith('QueueFixtures1967.java'):body=body.replace('static long captureEpoch1953(){return epoch;}','static long captureEpoch1953(){return epoch;}static long captureId1989(long e){return e==epoch?1:0;}')
  p=work/'queue-peers'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);files.append(p)
 queue=work/'queue-classes';queue.mkdir(exist_ok=True)
 run('queue_compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',queue,source/'GpuQualification1961.java',source/'tests1992/failure/Qualification1992Test.java',source/'tests1992/failure/TypedFailure1992Test.java',*files])
 for testname in ['Qualification1992Test','TypedFailure1992Test']:
  results.append(json.loads(run(testname,[jdk/'bin/java','-ea','-cp',queue,'com.hiro.ulike.'+testname]).strip().splitlines()[-1]))
 previous=os.environ.get('ULIKE1990_CHAIN_CLASSES');candidate=work.parent/'chain/host_chain1978/classes'
 if previous is None and candidate.is_dir():os.environ['ULIKE1990_CHAIN_CLASSES']=str(candidate)
 try:
  retained=mod('failure92_frontmodel_retained',source/'host_frontmodel1991.py').test(source,work/'frontmodel',jdk,ndk)
 finally:
  if previous is None:os.environ.pop('ULIKE1990_CHAIN_CLASSES',None)
 results.append(retained)
 current=work/'frontmodel/classes';base_candidates=[Path(previous or '/missing'),candidate,source.parent/'work/host1990-chain/host_chain1978/classes']
 base=next((p for p in base_candidates if (p/'com/hiro/ulike/SingleNoise1955.class').is_file()),None)
 if base is None:raise AssertionError('Current chain peer declarations missing')
 producer=work/'producer-classes';producer.mkdir(exist_ok=True)
 cp=os.pathsep.join(map(str,[current,base]));run('producer_compile',[jdk/'bin/javac','--release','8','-cp',cp,'-d',producer,source/'tests1992/failure/ProducerFailure1992Test.java'])
 for family in ['single','residual','analysis']:
  for fault in [0,2,3,4,5,6]:
   output=run(family+'_'+str(fault),[jdk/'bin/java','-ea','-Xmx512m','-cp',str(producer)+os.pathsep+cp,'com.hiro.ulike.ProducerFailure1992Test',family,str(fault)])
   results.append(json.loads(output.strip().split('RESULT ',1)[1]))
 if pins!={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Sources changed during typed failure proof')
 report=dict(status='passed',assertions=sum(x['assertions'] for x in results),tests=results,source_sha256=pins,physical_android_tested=False,device_speedup_verified=False)
 (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
