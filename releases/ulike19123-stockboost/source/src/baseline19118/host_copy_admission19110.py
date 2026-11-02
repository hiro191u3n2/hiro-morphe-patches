#!/usr/bin/env python3
"""Current Resident classification and unchanged Finish/copy-turn regression.

Uses real producer/queue/comparator and real Finish pressure branch/copy arbiter.
Controlled Strong sentinel/dispatch is tested separately from native admission.
No fixture enters the payload; no device performance or GPU timing is claimed.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re,subprocess,sys

def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();jar=Path(os.environ.get('ULIKE_ANDROID_JAR',str(jdk.parent/'android.jar'))).resolve()
 local=source/'tests19110/copy';tracked=[source/n for n in ('GpuResident1976.java','ResidentProof1978.java','GpuQualification1961.java','GpuSnapshotBudget1981.java','GpuChain1961.java','resident_inverse19110.py')]+list(local.rglob('*.java'))+[local/'resident-admission-inverse.json',Path(__file__).resolve()]
 pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
 def run(label,args):
  r=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=180);text=r.stdout+r.stderr;(work/(label+'.log')).write_text(text)
  if r.returncode:raise AssertionError(label+'\n'+text[-18000:])
  return text
 queue=module('copy110_queue',source/'host_qualification1981.py');proof=module('copy110_bitmap',source/'host_resident1978.py');files=[]
 for name,body in queue.FIXTURES.items():
  if name.startswith('com/'):continue
  p=work/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);files.append(p)
 version=re.search(r'public static final String VERSION\s*=\s*"([^"]+)"',(source/'ProcessingTiming1947.java').read_text()).group(1)
 peers=(source/'tests19108/diagnostics/Peers19108.java').read_text().replace('@VERSION@',version)
 peers=peers[:peers.index('final class GpuSnapshotBudget1981')]+peers[peers.index('final class PipelineDiagnostics1982'):]
 peers=peers.replace('int active,long started,long epoch','int active,boolean started,long epoch')
 peers=peers.replace('else if(mode!=0&&mode!=4&&candidateRuns==failAt)fail();','else if(mode<10&&mode!=0&&mode!=4&&candidateRuns==failAt)fail();')
 anchor='comparisons++;open=true;try{'
 assert peers.count(anchor)==1
 peers=peers.replace(anchor,anchor+'if(mode>=10&&resident&&comparisons==2*failAt)throw GpuStrong1960.fail19110(mode);')
 for name,body in [('android/graphics/Bitmap.java',proof.BITMAP),('com/hiro/ulike/Peers19110.java',peers)]:
  p=work/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);files.append(p)
 reports={'source_inverse':module('copy110_resident_inverse',source/'resident_inverse19110.py').test(source)}
 for label,resident,args in [('baseline',local/'baseline108/GpuResident1976.java',['baseline']),('current',source/'GpuResident1976.java',[])]:
  classes=work/label;classes.mkdir(exist_ok=True)
  current=[resident,source/'ResidentProof1978.java',source/'GpuQualification1961.java',source/'GpuSnapshotBudget1981.java',local/'StrongAdmissionPeer19110.java',local/'ResidentAdmission19110Test.java']
  run(label+'-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*current,*files])
  output=run(label+'-run',[jdk/'bin/java','-ea','-Xmx128m','-cp',classes,'com.hiro.ulike.ResidentAdmission19110Test',*args])
  reports[label]=json.loads(next(l[7:] for l in output.splitlines() if l.startswith('RESULT ')))
 # Build current compiler closure unchanged; only the tiny clock/skip observer
 # is shadowed at execution of the pressure-return path. No pixel work runs.
 sys.path.insert(0,str(source));builder=module('copy110_builder',source/'build19101.py')
 roots=['GpuChain1961','GpuSnapshotBudget1981'];inputs=list(builder.compile_inputs(roots).values())+[builder.production_path(n) for n in roots]
 production=work/'copy-production';production.mkdir(exist_ok=True)
 copied=[];compilepins={}
 for p in inputs:
  rel=p.relative_to(source);raw=p.read_bytes();q=work/'copy-source'/rel;q.parent.mkdir(parents=True,exist_ok=True);q.write_bytes(raw);copied.append(q);compilepins[str(rel)]=hashlib.sha256(raw).hexdigest()
 run('copy-production-compile',[jdk/'bin/javac','-source','8','-target','8','-encoding','UTF-8','-bootclasspath',jar,'-d',production,*copied])
 fixtures=work/'copy-fixtures';fixtures.mkdir(exist_ok=True)
 cp=os.pathsep.join(map(str,[production,jar]))
 run('copy-fixtures-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',fixtures,local/'CopyTimingPeer19110.java',local/'CopyRotation19110Test.java'])
 output=run('copy-rotation-run',[jdk/'bin/java','-ea','-cp',os.pathsep.join(map(str,[fixtures,production,jar])),'com.hiro.ulike.CopyRotation19110Test'])
 reports['copy_rotation']=json.loads(next(l[7:] for l in output.splitlines() if l.startswith('RESULT ')))
 if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Relevant source changed during test')
 assert reports['baseline']['cases']==1 and reports['current']['cases']==27
 proofs={}
 for label,names in [('source_inverse',['resident_source_inverse19110']),('baseline',['published108_strong_generic_exception_reproduced19110']),('current',['resident_strong_typed_classification19110','resident_incomplete_gate_preserved19110','actual_resident_queue_executed19110']),('copy_rotation',['finish_pressure_order_reproduced19110','next_capture_resident_rotation_verified19110','copy_ownership_and_limits_preserved19110','actual_finish_pressure_and_copy_budget_executed19110'])]:
  for name in names:
   assert reports[label].get(name) is True,(label,name)
   proofs[name]=reports[label][name]
 result=dict(status='passed',assertions=sum(r['assertions'] for r in reports.values()),tests=reports,source_sha256=pins,copy_compile_input_sha256=compilepins,
  physical_android_tested=False,device_speedup_verified=False,**proofs)
 (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),ensure_ascii=False,indent=2))
