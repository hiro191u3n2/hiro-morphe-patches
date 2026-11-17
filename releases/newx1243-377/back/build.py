#!/usr/bin/env python3
"""Build scoped NewX root-back MPP; app and Android compile stubs are excluded."""
import argparse,hashlib,json,pathlib,subprocess,zipfile
P=pathlib.Path
ROOT=P(__file__).resolve().parent

def run(cmd,log):
 r=subprocess.run(list(map(str,cmd)),capture_output=True,text=True)
 (ROOT/'work'/log).write_text(r.stdout+r.stderr)
 if r.returncode:raise RuntimeError(log+'\n'+r.stdout+r.stderr)
 return r.stdout

def jar(path,entries):
 with zipfile.ZipFile(path,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
  for n,b in sorted(entries.items()):
   info=zipfile.ZipInfo(n,(2026,11,17,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16;z.writestr(info,b)

def files(root):return {p.relative_to(root).as_posix():p.read_bytes() for p in root.rglob('*.class')}

def main():
 a=argparse.ArgumentParser();a.add_argument('--tools',type=P,required=True);a.add_argument('--dex-dir',type=P,required=True);args=a.parse_args()
 w=ROOT/'work';w.mkdir(exist_ok=True)
 for n in ['patch','runtime','test','patch-dex','runtime-dex']:(w/n).mkdir(exist_ok=True)
 cp=args.tools/'classpath.jar';ecj=['java','-jar',args.tools/'ecj.jar','-source','8','-target','8','-encoding','UTF-8']
 run(ecj+['-cp',cp,'-d',w/'patch',ROOT/'src/patch/NewXBackPatch.java'],'patch-compile.log')
 run(ecj+['-cp',str(cp)+':'+str(w/'patch'),'-d',w/'test',ROOT/'src/tests/BackDexTest.java'],'dex-test-compile.log')
 dexlog=run(['java','-cp',str(w/'test')+':'+str(w/'patch')+':'+str(cp),'BackDexTest',args.dex_dir],'dex-test.log')
 run(ecj+['-d',w/'runtime',*sorted((ROOT/'src/stubs').rglob('*.java')),ROOT/'src/runtime/RootExit.java',ROOT/'src/tests/BackRuntimeTest.java'],'runtime-compile.log')
 hostlog=run(['java','-cp',w/'runtime','BackRuntimeTest'],'runtime-test.log')
 patch=files(w/'patch');allruntime=files(w/'runtime');runtime={n:b for n,b in allruntime.items() if n.startswith('app/hiro/newx/back/')};stubs={n:b for n,b in allruntime.items() if not n.startswith('app/hiro/') and not n.startswith('BackRuntimeTest')}
 assert patch and runtime and all(n.startswith('app/hiro/newx/') for n in patch)
 jar(w/'patch.jar',patch);jar(w/'runtime.jar',runtime);jar(w/'stubs.jar',stubs)
 for kind in ['patch','runtime']:
  run(['java','-cp',args.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--classpath',cp,'--classpath',w/'stubs.jar','--output',w/(kind+'-dex'),w/(kind+'.jar')],kind+'-d8.log')
  assert [p.name for p in (w/(kind+'-dex')).iterdir()]==['classes.dex']
 manifest='Manifest-Version: 1.0\r\nName: NewX Hiro Back Exit\r\nVersion: 1.0.0\r\nTimestamp: 2026-11-17T00:00:00\r\nDescription: NewX 12.43 native root back exit and remove task.\r\nPatcher-Version: 2.2.0\r\n\r\n'
 out=dict(patch);out['classes.dex']=(w/'patch-dex/classes.dex').read_bytes();out['extensions/newx_back_exit.mpe']=(w/'runtime-dex/classes.dex').read_bytes();out['META-INF/MANIFEST.MF']=manifest.encode()
 jar(ROOT/'component.mpp',out)
 report={'result':'PASS','component_sha256':hashlib.sha256((ROOT/'component.mpp').read_bytes()).hexdigest(),'runtime_stubs_packaged':False,'runtime_test':hostlog.strip(),'dex_test':dexlog.strip(),'physical_device_tested':False,'changed_methods':['com.x.android.main.ea.t()Z','com.x.android.main.u9.invoke()Object native handleHomeBack branch','com.x.android.main.MainActivity.onStart()V'],'native_navigation_fallback_preserved':True}
 (ROOT/'QA.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n');print(json.dumps(report,ensure_ascii=False))
if __name__=='__main__':main()
