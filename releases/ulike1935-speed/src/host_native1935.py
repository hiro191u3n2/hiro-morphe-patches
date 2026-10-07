#!/usr/bin/env python3
"""Native exactness and JNI integration; host runs are not ARM64/device timing claims."""
import argparse,json,os,pathlib,shutil,subprocess

def test(root,work):
 root=pathlib.Path(root).resolve();work=pathlib.Path(work).resolve()/'host-native1935';work.mkdir(parents=True,exist_ok=True)
 native=root/'native1935';jdk=pathlib.Path(shutil.which('javac')).resolve().parent.parent;lib=work/'lib';lib.mkdir(exist_ok=True);classes=work/'classes';classes.mkdir(exist_ok=True)
 def run(args):
  p=subprocess.run(list(map(str,args)),text=True,capture_output=True,timeout=180)
  if p.returncode:raise RuntimeError(str(args)+'\n'+p.stdout+p.stderr)
  return p.stdout
 flags=['-std=c11','-O3','-fno-fast-math','-ffp-contract=off','-fno-tree-vectorize']
 run(['gcc',*flags,native/'kernels1935.c',native/'test_kernels1935.c','-o',work/'test-kernels']);kernel=json.loads(run([work/'test-kernels']))
 run(['gcc',*flags,'-shared','-fPIC','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),native/'kernels1935.c',native/'jni1935.c','-o',lib/'libulike_speed1935.so'])
 capture=root/'tests/capture-fixtures';sources=[root/'NativeSpeed1935.java',root/'YuvPlanes1934.java',root/'tests/Native1935Test.java',capture/'android/media/Image.java',capture/'android/graphics/Rect.java',capture/'android/graphics/ImageFormat.java']
 run(['javac','-source','8','-target','8','-d',classes,*sources]);results={}
 for mode,path in [('fallback',work/'missing'),('native',lib)]:results[mode]=json.loads(run(['java','-Xmx512m','-Djava.library.path='+str(path),'-cp',classes,'com.hiro.ulike.Native1935Test',mode]))
 report={'status':'passed','assertions':kernel['assertions']+sum(x['checks'] for x in results.values()),'kernel':kernel,'jni':results,'physical_android_tested':False};(work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',type=pathlib.Path,default=pathlib.Path(__file__).resolve().parent);p.add_argument('--work',required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work),indent=2))
