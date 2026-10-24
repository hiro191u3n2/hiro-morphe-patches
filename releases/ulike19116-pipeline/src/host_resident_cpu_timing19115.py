#!/usr/bin/env python3
"""Existing CPU-oracle observations only: real observer, queue and bounded journal."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re,subprocess

def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path); m=importlib.util.module_from_spec(spec); spec.loader.exec_module(m); return m

def test(source,work,jdk=None,ndk=None,backend=True):
 source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True);jdk=Path(jdk or os.environ['ULIKE_JDK_HOME'])
 local=source/'tests19115/cpu_timing'
 tracked=[source/n for n in ('tests19115/provenance/reference114/GpuResident1976.java','QualityPipeline1932.java','GpuResident1976.java','ResidentTiming19111.java','GpuQualification1961.java','VerificationJournal19101.java','host_qualification1981.py','tests19111/timing/TimingPeers19111.java','tests19111/timing/ResidentTiming19111Test.java','tests19111/diagnostics/ResidentDiagnostics19111Test.java','tests19101/journal/Context.java')]+[Path(__file__).resolve(),*local.glob('*.java')]
 pins={p.relative_to(source).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
 def run(label,args):
  p=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=90);output=p.stdout+p.stderr;(work/(label+'.log')).write_text(output)
  if p.returncode:raise AssertionError(label+'\n'+output[-14000:])
  return output
 def result(label,args):return json.loads(next(line[7:] for line in run(label,args).splitlines() if line.startswith('RESULT ')))
 reports={}
 resident=(source/'GpuResident1976.java').read_text();baseline=(source/'tests19115/provenance/reference114/GpuResident1976.java').read_text()
 body='                                residentTiming19111("beginCpu19115",trial,0,0);\n                                try {\n                                    oracle=QualityPipeline1932.strongFinish1978(owned,frozenPlan,level,shadows,null,rotation,width,height,frozenOutput,false,true,identity);\n                                    residentTiming19111("stopCpu19115",oracle==null?2:1,0,0);\n                                } finally {residentTiming19111("publishCpu19115",0,0,0);CPU.remove();}'
 if resident.count(body)!=1:raise AssertionError('CPU wrapper must contain only original call and optional observation')
 calls=lambda text:re.findall(r'QualityPipeline1932\.([A-Za-z0-9_]+)\(',text)
 if calls(resident)!=calls(baseline):raise AssertionError('No additional image processing trial may be introduced')
 pipeline=(source/'QualityPipeline1932.java').read_text();p2=pipeline.index('residentTiming19111("phase",2,0,0)');p3=pipeline.index('residentTiming19111("phase",3,0,0)',p2);p4=pipeline.index('residentTiming19111("phase",4,0,0)',p3)
 if not (p2<p3<pipeline.index('SpeedWorkers1935.run(workers)',p3)<p4<pipeline.index('stage19104(5',p4)):raise AssertionError('Strong work boundary must end before image signatures')
 reports['source_scope']={'status':'passed','assertions':3,'existing_cpu_oracle_calls_only':True,'production_strong_boundary_excludes_signatures':True}
 classes=work/'observer';classes.mkdir(exist_ok=True)
 peers=(source/'tests19111/timing/TimingPeers19111.java').read_text().replace(' static volatile boolean throwSink;',' static final List<long[]> cpuRecords=Collections.synchronizedList(new ArrayList<long[]>());\n public static void residentCpuTiming19115(long[] value){if(throwSink)throw new AssertionError("controlled unavailable sink");cpuRecords.add(value);}\n static volatile boolean throwSink;')
 peer=work/'TimingPeers19111.java';peer.write_text(peers)
 run('observer_compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,source/'ResidentTiming19111.java',peer,source/'tests19111/timing/ResidentTiming19111Test.java',local/'CpuObserver19115Test.java'])
 for name in ('ResidentTiming19111Test','CpuObserver19115Test'):
  reports[name]=result(name,[jdk/'bin/java','-ea','-Xmx96m','-cp',classes,'com.hiro.ulike.'+name])
 fixture=source/'tests19101/journal';peermod=module('queue_cpu19115',source/'host_qualification1981.py');files=[]
 for name,content in peermod.FIXTURES.items():
  if name.endswith('QueueHost1967.java'):continue
  if name=='android/content/Context.java':content=(fixture/'Context.java').read_text()
  elif name=='android/content/SharedPreferences.java':content=content.replace('void apply();','void apply();boolean commit();')
  elif name.endswith('QueueFixtures1967.java'):
   version=re.search(r'public static final String VERSION\s*=\s*"([^"]+)"',(source/'ProcessingTiming1947.java').read_text()).group(1)
   content=content.replace('final class ProcessingTiming1947 {','final class ProcessingTiming1947 {static final String VERSION="'+version+'";').replace('static volatile long epoch=1;','static volatile long epoch=1,photo=1;').replace('static long captureEpoch1953(){return epoch;}','static long captureEpoch1953(){return epoch;}static long captureId1989(long e){return e==epoch?photo:0;}')
  p=work/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content);files.append(p)
 files += [source/'ResidentTiming19111.java',local/'JournalGpuResidentPeer19115.java',source/'GpuQualification1961.java',source/'VerificationJournal19101.java',source/'tests19111/diagnostics/ResidentDiagnostics19111Test.java',local/'CpuJournal19115Test.java']
 classes=work/'journal';classes.mkdir(exist_ok=True);run('journal_compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*files]);snapshot=work/'vj9.txt'
 reports['journal']=result('journal',[jdk/'bin/java','-ea','-Xmx96m','-cp',classes,'com.hiro.ulike.CpuJournal19115Test',snapshot])
 reports['restart']=result('restart',[jdk/'bin/java','-ea','-Xmx96m','-cp',classes,'com.hiro.ulike.CpuJournal19115Test','--restart',snapshot])
 if backend:reports['backend']=module('cpu_backend19115',source/'host_cpu_strong_backend19115.py').test(source,work/'backend',jdk,ndk)
 flags={}
 for group in reports.values():
  if group.get('status')!='passed':raise AssertionError('failed CPU diagnostic proof')
  for k,v in group.items():
   if v is True:flags[k]=True
 if pins!={p.relative_to(source).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Production or fixture changed during test')
 out={'schema':'ulike19115-resident-cpu-timing-proof-v1','status':'passed','assertions':sum(r['assertions'] for r in reports.values()),'tests':reports,'source_sha256':pins,'physical_android_tested':False,'device_speedup_verified':False,'save_2000ms_achieved':False,'extra_image_work_added':False,**flags}
 (work/'result.json').write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n');return out

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');p.add_argument('--skip-backend',action='store_true');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk,not a.skip_backend),ensure_ascii=False,indent=2))
