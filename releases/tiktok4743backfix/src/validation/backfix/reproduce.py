#!/usr/bin/env python3
"""Rebuild released corresponding Java sources and compare one identical host case.
This is not an execution of DEX, an emulator, or a physical Android device.
"""
import argparse,hashlib,json,os,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--workspace',type=Path,required=True);p.add_argument('--work',type=Path,required=True);a=p.parse_args()
r=a.workspace.resolve();w=a.work.resolve();w.mkdir(parents=True,exist_ok=True)
s=r/'hiro-morphe-patches/releases/tiktok4743backfix/src/oneback';old=r/'hiro-morphe-patches/releases/tiktok4743/src/oneback/runtime';j=r/'tiktok-work/jdk/bin';t=r/'tiktok-work/tools'
for name in ('new-runtime','old-runtime','host','host4743'):(w/name).mkdir(exist_ok=True)
def run(name,args,ok=True):
 result=subprocess.run([str(x) for x in args],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
 (w/(name+'.log')).write_text(result.stdout)
 if ok and result.returncode:raise RuntimeError(name+': '+result.stdout)
 return result
for name,folder in (('new-runtime',s/'runtime'),('old-runtime',old)):
 run(name+'-compile',[j/'javac','--release','8','-encoding','UTF-8','-cp',t/'android.jar','-d',w/name,*sorted(folder.glob('*.java'))])
cp=os.pathsep.join(map(str,[w/'host',w/'new-runtime',t/'android.jar']))
classic=[p for p in (s/'test-runtime').glob('*.java') if not p.stem.startswith(('Back4743','Back4753'))]
run('host-compile',[j/'javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',w/'host',*sorted((s/'test-stubs').rglob('*.java')),*classic])
cp=os.pathsep.join(map(str,[w/'host4743',w/'host',w/'new-runtime',t/'android.jar']))
run('4743-compile',[j/'javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',w/'host4743',*sorted((s/'test-stubs4743').rglob('*.java')),s/'test-runtime/Back4743NativeBackTest.java'])
results={}
for version,runtime in (('1.1.21-corresponding-source','old-runtime'),('1.1.22-source','new-runtime')):
 cp=os.pathsep.join(map(str,[w/'host4743',w/'host',w/runtime,t/'android.jar']))
 result=run(version,[j/'java','-ea','-cp',cp,'app.hiro.oneback.runtime.Back4743NativeBackTest','--reproduce-root'],ok=False)
 results[version]={'exit_code':result.returncode,'log':version+'.log'}
 if runtime=='old-runtime':
  assert result.returncode!=0 and 'one Back excludes and removes its own task' in result.stdout,result.stdout
 else:assert result.returncode==0 and 'PASS Back4743NativeBackTest root regression' in result.stdout,result.stdout
cp=os.pathsep.join(map(str,[w/'host4743',w/'host',w/'new-runtime',t/'android.jar']))
full=run('new-full-host',[j/'java','-ea','-cp',cp,'app.hiro.oneback.runtime.Back4743NativeBackTest'])
proof={'schema':'tiktok4743-back-reproduction-v1','physical_android_execution':False,'old_provenance':'Java sources corresponding to released 1.1.21; not extracted DEX and not asserted byte-identical to published MPE','old_source_root':str(old.relative_to(r)),'same_scenario':'Back4743NativeBackTest --reproduce-root; no visible feed/tab labels, native HOME and page_feed, original native guards then timer/root boundaries','results':results,'new_host_summary':full.stdout.splitlines()[-2:],'sources':{}}
for prefix,folder in (('old',old),('new',s/'runtime')):
 for f in sorted(folder.glob('*.java')):proof['sources'][prefix+'/'+f.name]=hashlib.sha256(f.read_bytes()).hexdigest()
proof['fixture_sha256']=hashlib.sha256((s/'test-runtime/Back4743NativeBackTest.java').read_bytes()).hexdigest()
(w/'proof.json').write_text(json.dumps(proof,ensure_ascii=False,indent=2)+'\n')
print('PASS old 1.1.21 corresponding-source failure and new 1.1.22 source success');print(*proof['new_host_summary'],sep='\n')
