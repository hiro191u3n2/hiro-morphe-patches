#!/usr/bin/env python3
"""Actual resident pixel comparator, fail-closed rejection and persistent journal.
Dispatch/kernel APIs are compile-only peers which fail if invoked. These tests
make no Android GPU quality, speed or timing claim.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, subprocess

def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();fixture=source/'tests19103/validation'
 production=[source/name for name in ('GpuResident1976.java','ResidentProof1978.java','GpuQualification1961.java','VerificationJournal19101.java')]
 tracked=production+[fixture/'Validation19103Test.java',fixture/'Peers19103.java',Path(__file__).resolve()]
 pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
 queue=module('validation_queue_peers',source/'host_qualification1981.py');proof=module('validation_bitmap_peer',source/'host_resident1978.py')
 files=[]
 for name,body in queue.FIXTURES.items():
  if name.startswith('com/'):continue
  if name=='android/content/SharedPreferences.java':body=body.replace('void apply();','void apply();boolean commit();')
  if name=='android/content/Context.java':body=body.replace('public void apply(){','public boolean commit(){apply();return true;}public void apply(){')
  p=work/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);files.append(p)
 version=re.search(r'public static final String VERSION\s*=\s*"([^"]+)"',(source/'ProcessingTiming1947.java').read_text()).group(1)
 for name,body in [('android/graphics/Bitmap.java',proof.BITMAP),('com/hiro/ulike/Peers19103.java',(fixture/'Peers19103.java').read_text().replace('@VERSION@',version))]:
  p=work/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);files.append(p)
 classes=work/'classes';classes.mkdir(exist_ok=True)
 def run(name,args):
  result=subprocess.run(list(map(str,args)),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=60)
  (work/(name+'.log')).write_text(result.stdout)
  if result.returncode:raise AssertionError(name+'\n'+result.stdout[-16000:])
  return result.stdout
 run('compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*production,*files,fixture/'Validation19103Test.java'])
 output=run('run',[jdk/'bin/java','-ea','-Xmx128m','-cp',classes,'com.hiro.ulike.Validation19103Test'])
 result=json.loads(next(line[7:] for line in output.splitlines() if line.startswith('RESULT ')))
 if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('test inputs changed during execution')
 result.update(source_sha256=pins,fixture_classes_in_runtime=False,runtime_version=version)
 (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),ensure_ascii=False,indent=2))
