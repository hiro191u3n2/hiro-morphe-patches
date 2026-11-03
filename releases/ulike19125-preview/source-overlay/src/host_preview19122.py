#!/usr/bin/env python3
"""Actual policy + save adapter ownership and terminal integration, scripted SDK.
Retains the existing245 preview-helper assertions and3 mutation controls.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,subprocess

def module(path,name):
    spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def actual_async(source,work,jdk,async_source=None,expect=True):
    source,work,jdk=map(Path,(source,work,jdk));work.mkdir(parents=True,exist_ok=True)
    values,_=module(source/'host_capture_routes1981.py','capture122fixtures').fixtures(source)
    key='com/hiro/ulike/ProcessingTiming1947.java';body=values[key]
    body=body.replace('public boolean finished;','public boolean finished;public int binding1999,ownership1999;public long ticket1999;public volatile String previewResult;public final int[] dimensions1999=new int[10];')
    body=body.replace(' public static void skip(',''' public static final int WORKER_START1991=6,PREP_START1991=4,IMAGE_GET_START1992=13,IMAGE_GET_END1992=14,QUEUE_SUBMIT1991=5;
 public static Trace exactForKey1999(Object key){return forKey(key);}public static boolean finished1953(Trace t){return t==null||t.finished;}
 public static void mark1991(Trace t,int event){}public static void markAt1991(Trace t,int event,long time){}
 public static void bitmapInput1999(Trace t,int stage,int w,int h){if(t!=null){t.dimensions1999[stage*2]=w;t.dimensions1999[stage*2+1]=h;}}
 public static void saveInput1999(Trace t,long ticket,int binding,int ownership){if(t!=null){t.ticket1999=ticket;t.binding1999=binding;t.ownership1999=ownership;}}
 public static void capturePreview19121(Trace t,String result,long elapsed){t.previewResult=result;}
 public static void skip(''');values[key]=body
    key='com/hiro/ulike/ShotContext1932.java';values[key]=values[key].replace('public static boolean idle1953()', 'public static ProcessingTiming1947.Trace timingForBitmap1999(android.graphics.Bitmap b){return ProcessingTiming1947.forKey(b);}public static void forget(Object key){map.remove(key);}public static boolean idle1953()')
    values.update(module(source/'host_policy19122.py','policy122fixtures').fixtures())
    for p in (source/'tests19121/preview').rglob('*.java'):
        name=str(p.relative_to(source/'tests19121/preview'))
        if name in {'android/os/Handler.java','android/os/Looper.java','android/graphics/Bitmap.java','com/hiro/ulike/AsyncSave1935.java','com/hiro/ulike/ProcessingTiming1947.java','com/hiro/ulike/CapturePreview19121Test.java','i/f/l/n/s/a.java'}:continue
        values[name]=p.read_text()
    values['android/os/Looper.java']='''package android.os;public class Looper{static final Looper MAIN=new Looper();static final ThreadLocal<Boolean> UI=new ThreadLocal<Boolean>();public static Looper getMainLooper(){return MAIN;}public static Looper myLooper(){return Boolean.TRUE.equals(UI.get())?MAIN:null;}public static void check(){if(myLooper()!=MAIN)throw new AssertionError("UI read off main");}}'''
    values['android/os/Handler.java']='''package android.os;import java.util.concurrent.*;public class Handler{static final ExecutorService MAIN=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"fixture-main");t.setDaemon(true);return t;});static final ConcurrentLinkedQueue<Runnable> PREVIEW=new ConcurrentLinkedQueue<Runnable>(),HELD=new ConcurrentLinkedQueue<Runnable>();public static volatile boolean rejectPreview,rejectAll,holdMain;public static int mode;public Handler(Looper l){}static void execute(Runnable r){MAIN.execute(()->{Looper.UI.set(true);try{r.run();}finally{Looper.UI.remove();}});}public boolean post(Runnable r){if(rejectAll)return false;if(r.getClass().getName().equals("com.hiro.ulike.CapturePreview19121$Pending")){if(rejectPreview)return false;PREVIEW.add(r);}else if(holdMain)HELD.add(r);else execute(r);return true;}public static int heldCount(){return HELD.size();}public static void drainMain()throws Exception{holdMain=false;Runnable r;while((r=HELD.poll())!=null)execute(r);MAIN.submit(()->{}).get(5,TimeUnit.SECONDS);}public static int previewCount(){return PREVIEW.size();}public static void drainPreview()throws Exception{Runnable r;while((r=PREVIEW.poll())!=null)execute(r);MAIN.submit(()->{}).get(5,TimeUnit.SECONDS);}}'''
    values['i/f/l/n/s/a.java']=values['i/f/l/n/s/a.java'].replace('public volatile Bitmap image;','public volatile Bitmap image,b;')
    values['i/o/a/b1/a/b/c.java']=values['i/o/a/b1/a/b/c.java'].replace('public static class a {','public static class a implements i.o.a.b1.a.b.f.a.c {').replace('public int finished;','public volatile int finished,receipts;public volatile boolean saved;public void b(boolean ok,int n,String path,String error){saved=ok;receipts++;}')
    values['com/hiro/ulike/PostSaveCleanup19122.java']='''package com.hiro.ulike;import java.util.*;public final class PostSaveCleanup19122{public static int starts,ends,queueIdle;public static boolean enabledAtStart;public static final Set<Long> active=new HashSet<Long>();public static synchronized void captureStarted19122(long id){if(active.add(id)){starts++;enabledAtStart=CapturePolicy19122.cleanupEnabled();}}public static synchronized void attachTrace19122(long id,long trace){if(!active.contains(id)||trace<=0)throw new AssertionError("cleanup trace owner");}public static synchronized void terminal19122(long id){if(active.remove(id))ends++;else throw new AssertionError("double cleanup terminal "+id);}public static synchronized void queueIdle19122(){queueIdle++;}}'''
    values['com/hiro/ulike/Responsiveness19122.java']='''package com.hiro.ulike;public final class Responsiveness19122{public static volatile int queued,started,finished;public static String policy;public static volatile int requests;public static volatile boolean observerFail;public static ProcessingTiming1947.Trace previewTrace;public static long previewInput,previewReturned;public static void previewRequested19125(ProcessingTiming1947.Trace t,long input,long returned){requests++;previewTrace=t;previewInput=input;previewReturned=returned;if(observerFail)throw new IllegalStateException("optional observer unavailable");}public static void policy19122(ProcessingTiming1947.Trace t){policy=CapturePolicy19122.activeName19122();}public static void terminalQueued19122(ProcessingTiming1947.Trace t){queued++;}public static void terminalStarted19122(ProcessingTiming1947.Trace t){android.os.Looper.check();started++;if(started>queued)throw new AssertionError("terminal starts before queued");}public static void terminalFinished19122(ProcessingTiming1947.Trace t){android.os.Looper.check();finished++;if(finished>started)throw new AssertionError("terminal finish before start");}}'''
    paths=[]
    for name,body in values.items():
        p=work/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);paths.append(p)
    roots=['CapturePolicy19122.java','AsyncSave1935.java','CapturePreview19121.java','SaveQueue1935.java','ReflectionCache1945.java','SpeedWorkers1935.java','CodecPreparation1981.java','SaveMemory1981.java','FaceResult1981.java','PhotoAnalysis1981.java','EncoderTail1981.java']
    classes=work/'classes';classes.mkdir(exist_ok=True)
    r=subprocess.run([str(jdk/'bin/javac'),'--release','8','-encoding','UTF-8','-sourcepath',str(work/'fixtures'),'-d',str(classes),*[str(async_source if x=='AsyncSave1935.java' and async_source else source/x) for x in roots],*map(str,paths),str(source/'tests19122/preview/AsyncPolicy19122Test.java'),str(source/'tests1999/capture/SaveInput1999Test.java')],capture_output=True,text=True,timeout=90)
    (work/'compile.log').write_text(r.stdout+r.stderr)
    if r.returncode:raise AssertionError(r.stderr)
    results=[]
    for name in (['SaveInput1999Test','AsyncPolicy19122Test'] if expect else ['AsyncPolicy19122Test']):
        r=subprocess.run([str(jdk/'bin/java'),'-ea','-Xmx512m','-cp',str(classes),'com.hiro.ulike.'+name],capture_output=True,text=True,timeout=90)
        (work/(name+'.log')).write_text(r.stdout+r.stderr)
        if not expect:
            if not r.returncode:raise AssertionError('Async mutation escaped')
            return None
        if r.returncode:raise AssertionError(r.stdout+r.stderr)
        results.append(json.loads(r.stdout.strip().splitlines()[-1].removeprefix('RESULT ')))
    result=results[1];result['baseline_source_reuse']=results[0];return result,roots

def test(source,work,jdk):
    source,work,jdk=map(lambda v:Path(v).resolve(),(source,work,jdk));work.mkdir(parents=True,exist_ok=True)
    dependency_names=['host_preview19122.py','host_policy19122.py','tests19122/preview/AsyncPolicy19122Test.java',
      'host_capture_routes1981.py','host_capture_save1947.py','host_save_baseline1935.py','pipeline80-fixtures/SaveHarness1935.java',
      'tests1999/capture/SaveInput1999Test.java','StrongNoise1958.java']
    initial={name:hashlib.sha256((source/name).read_bytes()).hexdigest() for name in dependency_names}
    old=module(source/'host_preview19121.py','retained_preview121');old.actual_async=actual_async
    report=old.test(source,work,jdk)
    original=(source/'AsyncSave1935.java').read_text()
    mutations={'release-owned-diagnostic-shutter':('this.serialOnly=serialOnly||policy19122("diagnostic19122");','this.serialOnly=serialOnly;'),
      'omit-cleanup-terminal':('optional19122("PostSaveCleanup19122","terminal19122",id);',''),
      'bypass-still-prepares-analysis':('if(!policy19122("bypassProcessing"))analysis=','analysis='),
      'queue-empty-before-terminal-policy':('if(policy19122("diagnostic19122"))return true;\n            if(count==0)return false;', 'if(count==0)return false;\n            if(policy19122("diagnostic19122"))return true;')}
    for name,(old_text,new) in mutations.items():
        if original.count(old_text)!=1:raise AssertionError('Missing async mutation '+name)
        p=work/'mutants'/name/'AsyncSave1935.java';p.parent.mkdir(parents=True,exist_ok=True);p.write_text(original.replace(old_text,new))
        actual_async(source,work/name,jdk,p,False);report['negative_controls'].append({'name':name,'status':'rejected'})
    if initial!={name:hashlib.sha256((source/name).read_bytes()).hexdigest() for name in dependency_names}:raise AssertionError('Async fixture source changed during execution')
    report['source_sha256'].update(initial)
    report['fixture_scope']='Actual capture policy/save adapter/queue/CPU admission and retained preview helper; scripted Android/SDK/encoder. Diagnostic single-shot ownership and normal pixel copies, terminal post failure and lifecycle probes executed. Pixel pipeline/GPU and actual presentation not measured.'
    (work/'result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');return report
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk',required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),ensure_ascii=False))
