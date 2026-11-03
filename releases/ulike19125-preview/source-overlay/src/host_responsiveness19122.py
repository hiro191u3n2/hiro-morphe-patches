#!/usr/bin/env python3
"""Execute real bounded response recorder with controlled public Android callbacks.
No presentation FPS, thermal causation or device-performance claim is made.
"""
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess
ROOT=Path(__file__).resolve().parent
STUBS={
'android/content/Context.java':'''package android.content; public class Context {public Context getApplicationContext(){return this;}public Object getSystemService(String n){return new android.os.PowerManager();}}''',
'android/content/ContextWrapper.java':'''package android.content; public class ContextWrapper extends Context {final Context base;public ContextWrapper(Context c){base=c;}public Context getBaseContext(){return base;}public Context getApplicationContext(){return base.getApplicationContext();}}''',
'android/app/Application.java':'''package android.app; import android.os.Bundle; public class Application extends android.content.Context {public ActivityLifecycleCallbacks callbacks;public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks c){callbacks=c;}public interface ActivityLifecycleCallbacks {void onActivityCreated(Activity a,Bundle b);void onActivityStarted(Activity a);void onActivityResumed(Activity a);void onActivityPaused(Activity a);void onActivityStopped(Activity a);void onActivitySaveInstanceState(Activity a,Bundle b);void onActivityDestroyed(Activity a);}}''',
'android/app/Activity.java':'''package android.app; public class Activity extends android.content.ContextWrapper {public boolean focus=true,finishing,destroyed;public Activity(Application app){super(app);}public boolean isFinishing(){return finishing;}public boolean isDestroyed(){return destroyed;}public boolean hasWindowFocus(){return focus;}}''',
'android/os/Bundle.java':'package android.os; public class Bundle {}',
'android/os/Looper.java':'package android.os; public class Looper {public static Looper getMainLooper(){return new Looper();}}',
'android/os/Process.java':'package android.os; public class Process {public static volatile int nice;public static void setThreadPriority(int n){nice=n;}}',
'android/os/Debug.java':'package android.os; public class Debug {public static volatile boolean fail;public static long getNativeHeapAllocatedSize(){if(fail)throw new RuntimeException();return 123456;}}',
'android/os/PowerManager.java':'package android.os; public class PowerManager {public static volatile boolean fail;public static int status=3;public int getCurrentThermalStatus(){if(fail)throw new RuntimeException();return status;}}',
'android/os/Handler.java':'''package android.os; import java.util.ArrayDeque;public class Handler {public static boolean fail;private static final ArrayDeque<Runnable> queue=new ArrayDeque<Runnable>();public Handler(Looper l){}public boolean post(Runnable r){if(fail)return false;synchronized(queue){queue.add(r);}return true;}public static void drain(){for(int i=0;i<100;i++){Runnable r;synchronized(queue){r=queue.poll();}if(r==null)return;r.run();}throw new AssertionError("unbounded main queue");}}''',
'android/view/Choreographer.java':'''package android.view; public class Choreographer {public interface FrameCallback{void doFrame(long frameTimeNanos);}private static final Choreographer INSTANCE=new Choreographer();public static FrameCallback callback;public static int posts;public static Choreographer getInstance(){return INSTANCE;}public void postFrameCallback(FrameCallback c){posts++;callback=c;}public void removeFrameCallback(FrameCallback c){if(callback==c)callback=null;}public static void fire(){FrameCallback c=callback;callback=null;if(c!=null)c.doFrame(1);}}''',
'com/hiro/ulike/ProcessingTiming1947.java':'''package com.hiro.ulike; public class ProcessingTiming1947 {public static final class Trace {public final long id;public volatile String summary="";public Trace(long n){id=n;}}public static void responsiveness19122(Trace t,String s){t.summary=s;}}''',
'com/hiro/ulike/CapturePolicy19122.java':'''package com.hiro.ulike;public class CapturePolicy19122 {public static String activeName19122(){return "mode-normal";}public static int cpuWorkerLimit(){return 2;}public static int gpuBanks(){return 1;}public static long gpuSubmissionGapNanos(){return 2000000L;}public static boolean earlyPreviewEnabled(){return true;}public static boolean cleanupEnabled(){return true;}}'''
}

def test(source,work,jdk=None):
 source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
 production=source/'Responsiveness19122.java';suite=source/'tests19122/response/Responsiveness19122Test.java'
 tracked=[production,suite,Path(__file__).resolve()]
 pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
 paths=[]
 for name,body in STUBS.items():
  p=work/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);paths.append(p)
 classes=work/'classes';classes.mkdir(exist_ok=True)
 def run(label,args):
  done=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=60)
  (work/(label+'.log')).write_text(done.stdout+done.stderr)
  if done.returncode:raise AssertionError(label+'\n'+(done.stdout+done.stderr)[-16000:])
  return done.stdout
 run('compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,production,suite,*paths])
 result=json.loads(run('run',[jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.Responsiveness19122Test']).strip().splitlines()[-1])
 mutation=work/'mutation';mutation.mkdir(exist_ok=True)
 mutant=mutation/'Responsiveness19122.java';original=production.read_text();seam='if(firstUi==0)firstUi=now;'
 if original.count(seam)!=1:raise AssertionError('Initial notification mutation seam missing')
 mutant.write_text(original.replace(seam,''))
 mutant_classes=mutation/'classes';mutant_classes.mkdir(exist_ok=True)
 run('mutation-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',mutant_classes,mutant,suite,*paths])
 rejected=subprocess.run(list(map(str,[jdk/'bin/java','-ea','-cp',mutant_classes,'com.hiro.ulike.Responsiveness19122Test'])),capture_output=True,text=True,timeout=30)
 (work/'mutation-run.log').write_text(rejected.stdout+rejected.stderr)
 if rejected.returncode==0 or 'first arrival delay survives later regular intervals' not in rejected.stderr:raise AssertionError('Initial-notification omission mutation escaped intended test')
 result['negative_controls']['initial_notification_delay_mutation']=True
 result['mutation_control_names']=['initial_notification_delay_mutation']
 extra_mutants={
  'overwrite-first-preview-notice':('&&r.previewCamera19125==0','', 'later input notices cannot replace first post-API notice'),
  'accept-provider-switch':('&&cameraGeneration19125==r.previewChanges19125','', 'provider switch and switch-back cannot bridge request ownership'),
  'ignore-ring-history-loss':('if(overwrittenThrough19125>=from)return -1;','', 'overwritten first notice is explicitly unknown'),
  'omit-interposed-input-history':('r.previewCamera19125=Math.max(0L,first);','r.previewCamera19125=0;', 'input between API return and optional hook is recovered'),
  'ignore-late-owner-since':('&&cameraOwnerSince19125<=returned','', 'provider first verified after API return cannot be attributed')
 }
 for name,(old,new,message) in extra_mutants.items():
  if original.count(old)!=1:raise AssertionError('Preview observation mutation seam missing: '+name)
  path=work/'mutants'/name;path.mkdir(parents=True,exist_ok=True);changed=path/'Responsiveness19122.java';changed.write_text(original.replace(old,new))
  classes=path/'classes';classes.mkdir(exist_ok=True)
  run(name+'-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,changed,suite,*paths])
  rejected=subprocess.run(list(map(str,[jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.Responsiveness19122Test'])),capture_output=True,text=True,timeout=30)
  (work/(name+'-run.log')).write_text(rejected.stdout+rejected.stderr)
  if rejected.returncode==0 or message not in rejected.stderr:raise AssertionError('Preview observation mutation escaped intended test: '+name+'\n'+rejected.stdout+rejected.stderr)
  result['negative_controls'][name]=True;result['mutation_control_names'].append(name)

 if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Source changed during response checks')
 result.update(source_sha256=pins,generated_fixture_sha256={str(p.relative_to(work)):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths},physical_android_tested=False,device_speedup_verified=False,fixture_scope='Actual bounded recorder, callback expiry, generation, main/provider separation, owner lookup rate limit, histogram overflow and optional thermal/native API failure execute with public Android host fixtures; not displayed FPS or device recovery.')
 (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',default=ROOT);p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),ensure_ascii=False))
