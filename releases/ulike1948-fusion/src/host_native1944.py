#!/usr/bin/env python3
"""Exercise actual JNI glue plus C residual kernel against independent Java."""
import argparse, hashlib, json, pathlib, subprocess
def run(args):
    result=subprocess.run([str(x) for x in args],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    if result.returncode:raise RuntimeError(result.stdout)
    return result.stdout
def main():
    p=argparse.ArgumentParser();p.add_argument('--jdk',required=True);p.add_argument('--output',required=True);a=p.parse_args()
    src=pathlib.Path(__file__).resolve().parent;native=src/'native1944';jdk=pathlib.Path(a.jdk).resolve();out=pathlib.Path(a.output).resolve();out.mkdir(parents=True,exist_ok=True)
    classes=out/'classes';classes.mkdir(exist_ok=True);lib=out/'libulike_speed1935.so'
    flags=['-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror']
    run(['cc',*flags,'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),native/'kernels1935.c',native/'jni1935.c',native/'residual1944.c',native/'jni1944.c','-o',lib])
    run([jdk/'bin/javac','-d',classes,src/'NativeSpeed1944.java',src/'NativeSpeed1935.java',src/'SpeedWorkers1935.java',src/'FastPixels1933.java',native/'FastPixels1933Oracle1944.java',native/'Native1944Test.java'])
    result=run([jdk/'bin/java','-Djava.library.path='+str(out),'-cp',classes,'com.hiro.ulike.Native1944Test'])
    print(result,end='')
    report={'schema':'ulike-native1944-host-v1','pass':True,'checks':result.strip(),'compiler':run(['cc','-dumpfullversion','-dumpversion']).strip(),
        'java':run([jdk/'bin/java','-version']).splitlines()[0],'flags':flags,'physical_android_tested':False,
        'sources':{f.name:hashlib.sha256(f.read_bytes()).hexdigest() for f in [native/'residual1944.c',native/'jni1944.c',native/'Native1944Test.java',native/'FastPixels1933Oracle1944.java',src/'FastPixels1933.java',src/'NativeSpeed1944.java']}}
    (out/'host-native1944.json').write_text(json.dumps(report,indent=2)+'\n')
if __name__=='__main__':main()
