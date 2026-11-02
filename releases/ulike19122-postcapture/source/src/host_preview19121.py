#!/usr/bin/env python3
"""Actual preview helper on audited reflection/lifecycle fixtures; no device FPS claim."""
from pathlib import Path
import argparse,hashlib,json,subprocess,importlib.util

def actual_async(source,work,jdk):
    spec=importlib.util.spec_from_file_location('capture_baseline19121',source/'host_capture1999.py');base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)
    # First retain the established source-copy/reuse regression check unchanged.
    baseline=base.test(source,work/'baseline',jdk)
    values={str(p.relative_to(work/'baseline/fixtures')):p.read_text() for p in (work/'baseline/fixtures').rglob('*.java')}
    for p in (source/'tests19121/preview').rglob('*.java'):
        name=str(p.relative_to(source/'tests19121/preview'))
        if name in {'android/os/Handler.java','android/os/Looper.java','android/graphics/Bitmap.java','com/hiro/ulike/AsyncSave1935.java','com/hiro/ulike/ProcessingTiming1947.java','com/hiro/ulike/CapturePreview19121Test.java','i/f/l/n/s/a.java'}:continue
        values[name]=p.read_text()
    values['android/os/Looper.java']='''package android.os;public class Looper{static final Looper MAIN=new Looper();static final ThreadLocal<Boolean> UI=new ThreadLocal<Boolean>();public static Looper getMainLooper(){return MAIN;}public static Looper myLooper(){return Boolean.TRUE.equals(UI.get())?MAIN:null;}public static void check(){if(myLooper()!=MAIN)throw new AssertionError("UI read off main");}}'''
    values['android/os/Handler.java']='''package android.os;import java.util.concurrent.*;public class Handler{static final ExecutorService MAIN=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"fixture-main");t.setDaemon(true);return t;});static final ConcurrentLinkedQueue<Runnable> PREVIEW=new ConcurrentLinkedQueue<Runnable>();public static volatile boolean rejectPreview;public static int mode;public Handler(Looper l){}static void execute(Runnable r){MAIN.execute(()->{Looper.UI.set(true);try{r.run();}finally{Looper.UI.remove();}});}public boolean post(Runnable r){if(r.getClass().getName().equals("com.hiro.ulike.CapturePreview19121$Pending")){if(rejectPreview)return false;PREVIEW.add(r);}else execute(r);return true;}public static int previewCount(){return PREVIEW.size();}public static void drainPreview()throws Exception{Runnable r;while((r=PREVIEW.poll())!=null)execute(r);MAIN.submit(()->{}).get(5,TimeUnit.SECONDS);}}'''
    values['i/f/l/n/s/a.java']=values['i/f/l/n/s/a.java'].replace('public volatile Bitmap image;','public volatile Bitmap image,b;')
    values['i/o/a/b1/a/b/c.java']=values['i/o/a/b1/a/b/c.java'].replace('public static class a {','public static class a implements i.o.a.b1.a.b.f.a.c {').replace('public int finished;','public volatile int finished,receipts;public volatile boolean saved;public void b(boolean ok,int n,String path,String error){saved=ok;receipts++;}')
    values['com/hiro/ulike/ProcessingTiming1947.java']=values['com/hiro/ulike/ProcessingTiming1947.java'].replace('public boolean finished;','public boolean finished;public volatile String previewResult;').replace(' public static void skip(', ' public static void capturePreview19121(Trace t,String result,long elapsed){t.previewResult=result;} public static void skip(')
    paths=[]
    for name,body in values.items():
        p=work/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);paths.append(p)
    roots=['AsyncSave1935.java','CapturePreview19121.java','SaveQueue1935.java','ReflectionCache1945.java','SpeedWorkers1935.java','CodecPreparation1981.java','SaveMemory1981.java','FaceResult1981.java','PhotoAnalysis1981.java','EncoderTail1981.java']
    classes=work/'classes';classes.mkdir(exist_ok=True)
    compiled=subprocess.run([str(jdk/'bin/javac'),'--release','8','-encoding','UTF-8','-sourcepath',str(work/'fixtures'),'-d',str(classes),*[str(source/x) for x in roots],*map(str,paths),str(source/'tests19121/preview_integration/AsyncPreview19121Test.java')],capture_output=True,text=True,timeout=60)
    (work/'compile.log').write_text(compiled.stdout+compiled.stderr)
    if compiled.returncode:raise AssertionError(compiled.stderr)
    run=subprocess.run([str(jdk/'bin/java'),'-ea','-Xmx512m','-cp',str(classes),'com.hiro.ulike.AsyncPreview19121Test'],capture_output=True,text=True,timeout=60)
    (work/'actual-async.log').write_text(run.stdout+run.stderr)
    if run.returncode:raise AssertionError(run.stdout+run.stderr)
    result=json.loads(run.stdout.strip().splitlines()[-1]);result['baseline_source_reuse']=baseline;return result,roots

def test(source,work,jdk):
    source,work,jdk=map(lambda p:Path(p).resolve(),(source,work,jdk));work.mkdir(parents=True,exist_ok=True)
    fixtures=sorted((source/'tests19121/preview').rglob('*.java'))
    tracked=[source/'CapturePreview19121.java',source/'AsyncSave1935.java',source/'ReflectionCache1945.java',Path(__file__).resolve(),source/'tests19121/preview_integration/AsyncPreview19121Test.java',*fixtures]
    pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    def execute(label,helper,expect=True):
        classes=work/label;classes.mkdir(exist_ok=True)
        compiled=subprocess.run([str(jdk/'bin/javac'),'--release','8','-encoding','UTF-8','-d',str(classes),str(helper),str(source/'ReflectionCache1945.java'),*map(str,fixtures)],capture_output=True,text=True,timeout=60)
        (work/(label+'-compile.log')).write_text(compiled.stdout+compiled.stderr)
        if compiled.returncode:raise AssertionError(compiled.stderr)
        result=subprocess.run([str(jdk/'bin/java'),'-ea','-cp',str(classes),'com.hiro.ulike.CapturePreview19121Test'],capture_output=True,text=True,timeout=60)
        (work/(label+'.log')).write_text(result.stdout+result.stderr)
        if expect and result.returncode:raise AssertionError(result.stdout+result.stderr)
        if not expect and not result.returncode:raise AssertionError('Negative control unexpectedly passed: '+label)
        return result
    result=json.loads(execute('production',source/'CapturePreview19121.java').stdout.strip().splitlines()[-1])
    original=(source/'CapturePreview19121.java').read_text()
    mutants={
      'ignore-epoch':('||((Number)stat(ZOOM,"epoch")).longValue()!=token.epoch',''),
      'ignore-source-identity':('||field(store,"b")!=input',''),
      'release-sdk-callback':('return "HD描画停止の解除要求を送信（次の撮影は保護を継続）";','ReflectionCache1945.method(token.callback.getClass(),"a",NONE).invoke(token.callback);return "HD描画停止の解除要求を送信（次の撮影は保護を継続）";')}
    negatives=[]
    for label,(old,new) in mutants.items():
        if original.count(old)!=1:raise AssertionError('negative-control mutation missing: '+label)
        path=work/'mutants'/label/'CapturePreview19121.java';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(original.replace(old,new))
        execute(label,path,False);negatives.append({'name':label,'status':'rejected'})
    integrated,roots=actual_async(source,work/'actual-async',jdk)
    if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('sources changed during tests')
    pins.update({x:hashlib.sha256((source/x).read_bytes()).hexdigest() for x in roots})
    result['helper_assertions']=result['assertions'];result['assertions']+=integrated['assertions']+integrated['baseline_source_reuse']['assertions'];result['actual_async_integration']=integrated
    result.update(negative_controls=negatives,source_sha256=pins,physical_android_tested=False,device_speedup_verified=False,
      fixture_scope='Actual helper and reflection cache; current/obsolete camera identities, HD branch, foreground, source reuse and retained shutter ownership on deterministic Android fixtures. No frame presentation or speed measured.')
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk',required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),ensure_ascii=False))
