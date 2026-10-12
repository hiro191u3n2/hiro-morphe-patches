#!/usr/bin/env python3
"""Execute current Single/Residual branch and CPU pixels against controlled GPU I/O."""
from pathlib import Path
import argparse, hashlib, json, os, re, subprocess

def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
 candidates=[Path(os.environ.get('ULIKE1990_CHAIN_CLASSES','/missing')),
  work.parent/'chain/host_chain1978/classes',source.parent/'work/host1990-chain/host_chain1978/classes']
 base=next((p for p in candidates if (p/'com/hiro/ulike/SingleNoise1955.class').is_file()),None)
 if base is None:raise AssertionError('Execute host1990_chain first or set ULIKE1990_CHAIN_CLASSES')
 inputs=[source/'GpuSingle1960.java',source/'GpuResidual1961.java',source/'tests1989/reasons/ReasonsTransport1989.java',source/'tests1990/frontcopy/FrontCopy1990Test.java']
 pins={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}
 classes=work/'classes';classes.mkdir(exist_ok=True)
 for name,cmd in [('compile',[jdk/'bin/javac','--release','8','-cp',base,'-d',classes,*inputs]),
  ('execute',[jdk/'bin/java','-ea','-Xmx512m','-cp',str(classes)+os.pathsep+str(base),'com.hiro.ulike.FrontCopy1990Test'])]:
  done=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,timeout=60);(work/(name+'.log')).write_text(done.stdout+done.stderr)
  if done.returncode:raise AssertionError(name+'\n'+done.stdout+done.stderr)
 result=json.loads(re.search(r'^RESULT (.*)$',done.stdout,re.M).group(1))
 if pins!={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}:raise AssertionError('Sources changed during foreground-copy test')
 result['source_sha256']=pins;result['transport_is_controlled']=True
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
