#!/usr/bin/env python3
"""Actual resident producer, comparator, queue and VJ6 memory diagnostics.
Controlled dispatch simulates typed refusal only; actual chain/session/budget
behavior is checked independently. No physical GPU timing claim is made.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re,subprocess

def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True);jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
 fixture=source/'tests19108/diagnostics';production=[source/n for n in ('GpuResident1976.java','ResidentProof1978.java','GpuQualification1961.java','VerificationJournal19101.java')]
 tracked=production+list(fixture.glob('*.java'))+[Path(__file__).resolve()];pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
 queue=module('memory108_queue',source/'host_qualification1981.py');proof=module('memory108_bitmap',source/'host_resident1978.py');files=[]
 for name,body in queue.FIXTURES.items():
  if name.startswith('com/'):continue
  if name=='android/content/SharedPreferences.java':body=body.replace('void apply();','void apply();boolean commit();')
  if name=='android/content/Context.java':body=body.replace('public void apply(){','public boolean commit(){apply();return true;}public void apply(){')
  p=work/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);files.append(p)
 version=re.search(r'public static final String VERSION\s*=\s*"([^"]+)"',(source/'ProcessingTiming1947.java').read_text()).group(1)
 for name,body in [('android/graphics/Bitmap.java',proof.BITMAP),('com/hiro/ulike/Peers19108.java',(fixture/'Peers19108.java').read_text().replace('@VERSION@',version))]:
  p=work/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);files.append(p)
 classes=work/'classes';classes.mkdir(exist_ok=True)
 def run(name,args):
  r=subprocess.run(list(map(str,args)),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=90);(work/(name+'.log')).write_text(r.stdout)
  if r.returncode:raise AssertionError(name+'\n'+r.stdout[-16000:])
  return r.stdout
 run('compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*production,*files,fixture/'ResidentMemory19108Test.java'])
 output=run('run',[jdk/'bin/java','-ea','-Xmx128m','-cp',classes,'com.hiro.ulike.ResidentMemory19108Test']);result=json.loads(next(l[7:] for l in output.splitlines() if l.startswith('RESULT ')))
 if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Memory diagnostics testinputs changed')
 result.update(source_sha256=pins,controlled_dispatch_fixture=True,actual_resident_producer_queue_journal=True,device_speedup_verified=False);(work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),ensure_ascii=False,indent=2))
