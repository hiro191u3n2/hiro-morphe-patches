#!/usr/bin/env python3
"""Actual asynchronous save adapter with retained SDK/Bitmap/codec ABI fixtures.
The timing sink is an assertion shim; host_timing1999 verifies its real implementation.
No device image quality, GPU output or speedup is claimed.
"""
from pathlib import Path
import argparse,importlib.util,json,subprocess,hashlib

def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True);jdk=Path(jdk)
    base=module('capture81_fixtures99',source/'host_capture_routes1981.py')
    fixtures,_=base.fixtures(source)
    key='com/hiro/ulike/ProcessingTiming1947.java';body=fixtures[key]
    body=body.replace('public boolean finished;','public boolean finished;public int binding1999,ownership1999;public long ticket1999;public final int[] dimensions1999=new int[10];')
    body=body.replace(' public static void skip(',''' public static final int WORKER_START1991=6,PREP_START1991=4,IMAGE_GET_START1992=13,IMAGE_GET_END1992=14,QUEUE_SUBMIT1991=5;
 public static Trace exactForKey1999(Object key){return forKey(key);}
 public static boolean finished1953(Trace t){return t==null||t.finished;}
 public static void mark1991(Trace t,int event){}
 public static void markAt1991(Trace t,int event,long time){}
 public static void bitmapInput1999(Trace t,int stage,int w,int h){if(t!=null){t.dimensions1999[stage*2]=w;t.dimensions1999[stage*2+1]=h;}}
 public static void saveInput1999(Trace t,long ticket,int binding,int ownership){if(t!=null){t.ticket1999=ticket;t.binding1999=binding;t.ownership1999=ownership;}}
 public static void skip(''')
    fixtures[key]=body
    key='com/hiro/ulike/ShotContext1932.java'
    fixtures[key]=fixtures[key].replace('public static boolean idle1953()', 'public static ProcessingTiming1947.Trace timingForBitmap1999(android.graphics.Bitmap b){return ProcessingTiming1947.forKey(b);}public static void forget(Object key){map.remove(key);}public static boolean idle1953()')
    paths=[]
    for name,body in fixtures.items():
        p=work/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);paths.append(p)
    roots=['AsyncSave1935.java','SaveQueue1935.java','ReflectionCache1945.java','SpeedWorkers1935.java','CodecPreparation1981.java','SaveMemory1981.java','FaceResult1981.java','PhotoAnalysis1981.java','EncoderTail1981.java']
    classes=work/'classes';classes.mkdir(exist_ok=True)
    def run(command,name):
        r=subprocess.run(list(map(str,command)),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=90)
        (work/name).write_text(r.stdout)
        if r.returncode:raise AssertionError(name+'\n'+r.stdout)
        return r.stdout
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-sourcepath',work/'fixtures','-d',classes,*[source/x for x in roots],*paths,source/'tests1999/capture/SaveInput1999Test.java'],'compile.log')
    output=run([jdk/'bin/java','-ea','-Xmx512m','-cp',classes,'com.hiro.ulike.SaveInput1999Test'],'SaveInput1999Test.log')
    result=json.loads(next(line[7:] for line in output.splitlines() if line.startswith('RESULT ')))
    result.update(physical_android_tested=False,device_speedup_verified=False,production_source_sha256={x:hashlib.sha256((source/x).read_bytes()).hexdigest() for x in roots})
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk',required=True);p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),ensure_ascii=False))
