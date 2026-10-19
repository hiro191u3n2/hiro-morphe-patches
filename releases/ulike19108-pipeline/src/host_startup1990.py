#!/usr/bin/env python3
"""Host policy tests, explicitly not Android GPU performance measurements."""
import argparse, importlib.util, json, pathlib, shutil, subprocess, tempfile
ROOT=pathlib.Path(__file__).resolve().parent
SOURCES={
'android/content/Context.java': '''package android.content; public class Context { public static final int MODE_PRIVATE=0; public final SharedPreferences prefs=new SharedPreferences(); public Context getApplicationContext(){return this;} public SharedPreferences getSharedPreferences(String name,int mode){return prefs;} }''',
'android/content/SharedPreferences.java': '''package android.content; import java.util.*; public class SharedPreferences { public final Map<String,String> data=new HashMap<>(); public String getString(String key,String fallback){return data.containsKey(key)?data.get(key):fallback;} public Editor edit(){return new Editor();} public class Editor {private final Map<String,String> pending=new HashMap<>();public Editor putString(String k,String v){pending.put(k,v);return this;}public Editor remove(String k){pending.put(k,null);return this;}public void apply(){for(Map.Entry<String,String> e:pending.entrySet()){if(e.getValue()==null)data.remove(e.getKey());else data.put(e.getKey(),e.getValue());}}}}''',
'com/hiro/ulike/GpuQualification1961.java': '''package com.hiro.ulike; public class GpuQualification1961 {static String environment="device-os-app-kernel-driver-A";static int initCalls,pauses,resumes;static boolean paused;static void startupPause1990(boolean value){paused=value;if(value)pauses++;else resumes++;} public static void initialize(android.content.Context c){initCalls++;}static String environment1990(){return environment;}public static void corpusCertificate1994(){} public interface Cancellation{boolean cancelled();}}''',
'com/hiro/ulike/GpuNoise1960.java': '''package com.hiro.ulike; public class GpuNoise1960 { static volatile int calls; static volatile boolean available=true,unsupported,holdAux,busy;static boolean sessionBusy(){return busy;} static final java.util.concurrent.CountDownLatch aux=new java.util.concurrent.CountDownLatch(1); static boolean warmEnvironment1973(){return available;} static boolean supports(int p){calls++;try{if(holdAux&&p!=39)aux.await();Thread.sleep(3);}catch(InterruptedException e){Thread.currentThread().interrupt();}return !(unsupported&&p==1);} }''',
'com/hiro/ulike/GpuFinish1953.java': '''package com.hiro.ulike; public class GpuFinish1953{static int calls;static void warmupAsync(){calls++;}}''',
'android/os/Looper.java': '''package android.os;public class Looper{public static volatile boolean ui;private static final Object MAIN=new Object();public static Object getMainLooper(){return MAIN;}public static Object myLooper(){return ui&&Thread.currentThread().getName().equals("main")?MAIN:null;}}''',
'android/os/SystemClock.java': '''package android.os;public class SystemClock{public static long uptimeMillis(){return System.nanoTime()/1000000L;}}''',
'com/hiro/ulike/CameraTrace1965.java': '''package com.hiro.ulike;public class CameraTrace1965{static int initCalls;static long firstEvent;public static void init(android.content.Context c){initCalls++;}public static void event(String phase,long epoch,String fields){if(firstEvent==0)firstEvent=System.nanoTime();}public static void anomaly(String p,long e,String f){}}''',
'com/hiro/ulike/ManualLens170.java': '''package com.hiro.ulike;public class ManualLens170{public static long number(String s){return 0;}public static Object get(String s){return null;}public static boolean yes(String s){return false;}}''',
'com/hiro/ulike/ProviderLifecycle1929.java': '''package com.hiro.ulike;public class ProviderLifecycle1929{public static Object get(Object o,String s){return null;}public static Object call(Object o,String s){return null;}}''',
'com/hiro/ulike/GpuFinishDefault1990.java': '''package com.hiro.ulike; public class GpuFinishDefault1990{static final String SUITE="finish-suite";static int calls;static boolean passed,wide;static int wideCalls;static boolean ready(){return passed;}static boolean wideReady1993(){return wide;}static String wideSuite1993(){return SUITE+":wide";}static String summaryWide1995(){return wide?"128-ready":"128-pending";}static void requestWide1995(){}static boolean qualifyWide1995(GpuQualification1961.Cancellation c){wideCalls++;return wide=!c.cancelled();}static boolean qualify(GpuQualification1961.Cancellation c){calls++;wide=!c.cancelled();return passed=wide;}}''',
'com/hiro/ulike/GpuModelDefault1990.java': '''package com.hiro.ulike; public class GpuModelDefault1990{static final String SUITE="model-suite";static String summary(){return passed?"model ready":"model pending";}static int calls;static boolean passed;static boolean ready(){return passed;}static void requestOptional1994(){}static boolean qualify(GpuQualification1961.Cancellation c){calls++;return passed=!c.cancelled();}}''',
'com/hiro/ulike/GpuStrongDefault1990.java': '''package com.hiro.ulike; public class GpuStrongDefault1990{static final String SUITE="strong-suite";static volatile int calls;static boolean result=true,failFirst,reject,passed; static boolean qualify(GpuQualification1961.Cancellation c){calls++;if(reject){GpuCertificates1990.reject(SUITE,"controlled complete mismatch");return false;}return passed=result&&!(failFirst&&calls==1)&&!c.cancelled();}static boolean ready(){return passed;}static void request1994(){}}''',
}
def test(source,work,jdk,ndk=None):
    source=pathlib.Path(source).resolve();out=pathlib.Path(work).resolve();out.mkdir(parents=True,exist_ok=True)
    fixture=out/'fixtures';classes=out/'classes'
    if fixture.exists():shutil.rmtree(fixture)
    if classes.exists():shutil.rmtree(classes)
    for name,content in SOURCES.items():
        p=fixture/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
    sources=[str(p) for p in fixture.rglob('*.java')]+[str(source/p) for p in ('GpuStartup1990.java','GpuCertificates1990.java','CameraSession1965.java','tests1990/startup/Startup1990Test.java')]
    jdk=pathlib.Path(jdk) if jdk else None
    javac=str(jdk/'bin/javac') if jdk else 'javac';java=str(jdk/'bin/java') if jdk else 'java'
    subprocess.run([javac,'-encoding','UTF-8','-d',str(classes),*sources],check=True)
    results=[]
    for scenario in ('ready','partial','unavailable','certificates','unsupported','core','camera','retry','reject','cached_busy','cached_timeout','ui'):
        done=subprocess.run([java,'-cp',str(classes),'com.hiro.ulike.Startup1990Test',scenario],check=True,capture_output=True,text=True)
        results.append(json.loads(done.stdout))
    # Exercise the real idle scheduler as well as the startup transport fixture.
    spec=importlib.util.spec_from_file_location('startup_queue_fixtures1990',source/'host_qualification1981.py')
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    qfixture=out/'queue-fixtures';qclasses=out/'queue-classes'
    if qfixture.exists():shutil.rmtree(qfixture)
    if qclasses.exists():shutil.rmtree(qclasses)
    qfiles=[]
    for name,content in module.FIXTURES.items():
        if name.endswith('QueueHost1967.java'):continue
        if name.endswith('QueueFixtures1967.java'):
            content=content.replace('static long captureEpoch1953(){return epoch;}', 'static long captureEpoch1953(){return epoch;}static long captureId1989(long value){return value;}')
        p=qfixture/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content);qfiles.append(str(p))
    qfiles += [str(source/'GpuQualification1961.java'),str(source/'tests1990/startup/StartupQueue1990Test.java')]
    subprocess.run([javac,'-encoding','UTF-8','-d',str(qclasses),*qfiles],check=True)
    done=subprocess.run([java,'-cp',str(qclasses),'com.hiro.ulike.StartupQueue1990Test'],check=True,capture_output=True,text=True,timeout=15)
    results.append(json.loads(done.stdout))
    report={'status':'passed','assertions':sum(x['assertions'] for x in results),'physical_android_tested':False,'tests':results}
    (out/'result.json').write_text(json.dumps(report,indent=2)+'\n')
    return report

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source',type=pathlib.Path,default=ROOT);parser.add_argument('--work',type=pathlib.Path);parser.add_argument('--jdk',type=pathlib.Path)
    args=parser.parse_args()
    if args.work:report=test(args.source,args.work,args.jdk)
    else:
        with tempfile.TemporaryDirectory(prefix='ulike-startup1990-') as temp:report=test(args.source,temp,args.jdk)
    print(json.dumps(report,ensure_ascii=False))
