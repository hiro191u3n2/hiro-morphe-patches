from pathlib import Path
import subprocess

def test(root:Path,work:Path):
 src=work/'host-src';out=work/'host-classes';src.mkdir(parents=True);out.mkdir()
 files={
 'android/os/Looper.java':'''package android.os; public class Looper {static final Looper MAIN=new Looper();public static boolean main=true;public static Looper myLooper(){return main?MAIN:null;}public static Looper getMainLooper(){return MAIN;}}''',
 'android/os/SystemClock.java':'''package android.os;public class SystemClock {public static long now=100;public static long uptimeMillis(){return now;}}''',
 'android/os/Handler.java':'''package android.os;import java.util.*;public class Handler {public static final List<Runnable> tasks=new ArrayList<>();public Handler(Looper l){}public boolean postDelayed(Runnable r,long ms){tasks.add(r);return true;}}''',
 'android/app/Activity.java':'''package android.app;public class Activity {public boolean finishing,destroyed;public int finishes,posts;public Runnable pending;public boolean isFinishing(){return finishing;}public boolean isDestroyed(){return destroyed;}public void finish(){finishing=true;finishes++;}public void runOnUiThread(Runnable r){posts++;pending=r;}}''',
 'android/util/Log.java':'''package android.util;public class Log {public static int warnings;public static int w(String t,String m){warnings++;return 0;}public static int w(String t,String m,Throwable e){warnings++;return 0;}}''',
 'android/view/KeyEvent.java':'''package android.view;public class KeyEvent {public static final int KEYCODE_BACK=4,ACTION_DOWN=0,ACTION_UP=1;final int action,code;public KeyEvent(long d,long e,int a,int c,int r){action=a;code=c;}public int getAction(){return action;}public int getKeyCode(){return code;}}''',
 'android/hardware/camera2/CameraDevice.java':'''package android.hardware.camera2;public class CameraDevice {}''',
 'android/hardware/camera2/params/SessionConfiguration.java':'''package android.hardware.camera2.params;public class SessionConfiguration {final java.util.concurrent.Executor executor;public SessionConfiguration(java.util.concurrent.Executor e){executor=e;}public java.util.concurrent.Executor getExecutor(){return executor;}}''',
 'com/light/beauty/basisplatform/appsetting/AppSettingsActivity.java':'''package com.light.beauty.basisplatform.appsetting;public class AppSettingsActivity extends android.app.Activity {}''',
 'com/light/beauty/mainpage/MainActivity.java':'''package com.light.beauty.mainpage;public class MainActivity extends android.app.Activity {public com.light.beauty.mc.preview.page.main.UlikeMainPage t;}''',
 'com/light/beauty/mc/preview/page/main/UlikeMainPage.java':'''package com.light.beauty.mc.preview.page.main;import android.view.KeyEvent;public class UlikeMainPage {public boolean added=true,up,throwsRuntime,throwsLinkage;public String panel;public int downCalls,upCalls;public boolean isAdded(){return added;}public boolean G2(int key,KeyEvent e){if(key!=4||e.getKeyCode()!=4||e.getAction()!=0)throw new AssertionError("wrong down event");downCalls++;if(throwsRuntime)throw new IllegalStateException();if(throwsLinkage)throw new NoSuchMethodError();if(panel!=null){panel=null;return true;}return false;}public boolean H2(int key,KeyEvent e){if(key!=4||e.getKeyCode()!=4||e.getAction()!=1)throw new AssertionError("wrong up event");upCalls++;return up;}}''',
 'com/hiro/ulike/BackExit185.java':'''package com.hiro.ulike;public class BackExit185 {public static int rootRequests;public static void requestOnMain1920(BackExit185 c,android.app.Activity a){if(!BackRoute1920.consume(c,a))rootRequests++;}}''',
 'com/hiro/ulike/ManualLens170.java':'''package com.hiro.ulike;public class ManualLens170 {public static boolean foreground=true,ready=true;static boolean yes(String n)throws ReflectiveOperationException{return foreground;}static boolean ready()throws ReflectiveOperationException{return ready;}}''',
 'com/hiro/ulike/CaptureAdvanced3.java':'''package com.hiro.ulike;import android.hardware.camera2.CameraDevice;import android.hardware.camera2.params.SessionConfiguration;public class CaptureAdvanced3 {static final class State {volatile boolean configured,maximum,restoring;volatile long token,timedOutToken1920;volatile CameraDevice device;}static Object owner;static State state;static int calls,retries;public static void sessionCore1920(Object o,CameraDevice d,SessionConfiguration c){calls++;}static State state1920(Object o){return o==owner?state:null;}static boolean current1920(Object o,State s){return o==owner&&s==state;}static void retry1920(Object o,State s,Throwable t){if(!current1920(o,s))throw new AssertionError("stale retry");if(!(t instanceof java.util.concurrent.TimeoutException))throw new AssertionError("wrong cause");retries++;s.restoring=true;s.maximum=false;}}''',
 'com/hiro/ulike/Tests1920.java':'''package com.hiro.ulike;
import android.os.*;import android.app.Activity;import android.hardware.camera2.*;import android.hardware.camera2.params.*;import com.light.beauty.mainpage.MainActivity;import com.light.beauty.mc.preview.page.main.UlikeMainPage;import com.light.beauty.basisplatform.appsetting.AppSettingsActivity;import java.util.*;import java.util.concurrent.*;
public class Tests1920 {
 static int assertions;static List<String> log=new ArrayList<>();static void check(String n,boolean b){if(!b)throw new AssertionError(n);assertions++;log.add("PASS\\t"+n);}
 static MainActivity camera(String panel){MainActivity a=new MainActivity();a.t=new UlikeMainPage();a.t.panel=panel;return a;}
 static class Queue implements Executor {List<Runnable> q=new ArrayList<>();boolean reject;public void execute(Runnable r){if(reject)throw new RejectedExecutionException();q.add(r);}void flush(){new ArrayList<>(q).forEach(Runnable::run);q.clear();}}
 static class Setup {Object owner=new Object();CameraDevice device=new CameraDevice();CaptureAdvanced3.State state=new CaptureAdvanced3.State();Queue executor=new Queue();SessionConfiguration config=new SessionConfiguration(executor);
 Setup(){Looper.main=true;Handler.tasks.clear();SystemClock.now=100;ManualLens170.foreground=true;ManualLens170.ready=true;CaptureAdvanced3.calls=0;CaptureAdvanced3.retries=0;CaptureAdvanced3.owner=owner;CaptureAdvanced3.state=state;state.maximum=true;state.token=42;state.device=device;}
 void arm(){CameraSession1920.session(owner,device,config);}void timeout(){SystemClock.now+=10000;new ArrayList<>(Handler.tasks).forEach(Runnable::run);}}
 public static void main(String[] args){
 BackExit185 ctrl=new BackExit185();check("null",!BackRoute1920.consume(ctrl,null));
 Activity other=new Activity();check("unrelated activity",!BackRoute1920.consume(ctrl,other));
 MainActivity root=camera(null);check("bare camera continues existing exit",!BackRoute1920.consume(ctrl,root)&&root.t.downCalls==1&&root.t.upCalls==1&&root.finishes==0);
 for(String panel:new String[]{"fine-adjustment","style","beauty","filter"}){MainActivity a=camera(panel);check(panel+" closes without exiting",BackRoute1920.consume(ctrl,a)&&a.t.panel==null&&a.finishes==0&&a.t.upCalls==0);check(panel+" next Back reaches root",!BackRoute1920.consume(ctrl,a));}
 MainActivity up=camera(null);up.t.up=true;check("native key-up consumer",BackRoute1920.consume(ctrl,up)&&up.t.upCalls==1);
 MainActivity notAdded=camera("style");notAdded.t.added=false;check("unattached page not invoked",!BackRoute1920.consume(ctrl,notAdded)&&notAdded.t.downCalls==0);
 MainActivity noPage=new MainActivity();check("no page may exit",!BackRoute1920.consume(ctrl,noPage));
 MainActivity failed=camera("beauty");failed.t.throwsRuntime=true;check("panel error cannot exit",BackRoute1920.consume(ctrl,failed)&&failed.finishes==0);
 failed.t.throwsRuntime=false;failed.t.throwsLinkage=true;check("linkage error cannot exit",BackRoute1920.consume(ctrl,failed));
 MainActivity dead=camera("style");dead.destroyed=true;check("destroyed activity untouched",BackRoute1920.consume(ctrl,dead)&&dead.t.downCalls==0);
 AppSettingsActivity settings=new AppSettingsActivity();check("settings closes once",BackRoute1920.consume(ctrl,settings)&&settings.finishes==1);check("repeat settings Back harmless",BackRoute1920.consume(ctrl,settings)&&settings.finishes==1);
 Looper.main=false;MainActivity worker=camera("filter");check("worker dispatch deferred",BackRoute1920.consume(ctrl,worker)&&worker.posts==1&&worker.t.downCalls==0);Looper.main=true;worker.pending.run();check("worker closes on main",worker.t.panel==null&&BackExit185.rootRequests==0);
 Looper.main=false;MainActivity workerRoot=camera(null);check("worker root deferred",BackRoute1920.consume(ctrl,workerRoot)&&workerRoot.posts==1);Looper.main=true;workerRoot.pending.run();check("worker root delegates exactly once",BackExit185.rootRequests==1);
 Looper.main=false;AppSettingsActivity workerSettings=new AppSettingsActivity();check("settings worker deferred",BackRoute1920.consume(ctrl,workerSettings)&&workerSettings.posts==1&&workerSettings.finishes==0);Looper.main=true;workerSettings.pending.run();check("settings worker closes",workerSettings.finishes==1);
 Looper.main=false;MainActivity laterDead=camera("style");BackRoute1920.consume(ctrl,laterDead);laterDead.destroyed=true;Looper.main=true;laterDead.pending.run();check("destroyed after enqueue untouched",laterDead.t.downCalls==0);
 int backCount=assertions;
 Setup s=new Setup();s.arm();check("original session called exactly once",CaptureAdvanced3.calls==1);check("one bounded timeout armed",Handler.tasks.size()==1);Handler.tasks.get(0).run();check("no premature timeout",s.executor.q.size()==0&&CaptureAdvanced3.retries==0);s.timeout();check("recovery posted to original executor",s.executor.q.size()==1&&CaptureAdvanced3.retries==0);s.executor.flush();check("timeout invokes existing fallback",CaptureAdvanced3.retries==1&&!s.state.maximum&&s.state.restoring);s.timeout();s.executor.flush();check("no repeated fallback",CaptureAdvanced3.retries==1);
 s=new Setup();s.state.maximum=false;s.arm();check("normal mode unmodified",Handler.tasks.size()==0&&CaptureAdvanced3.calls==1);
 s=new Setup();s.state.configured=true;s.arm();check("synchronous success unmodified",Handler.tasks.size()==0);
 s=new Setup();s.state.restoring=true;s.arm();check("existing recovery not duplicated",Handler.tasks.size()==0);
 s=new Setup();s.state.device=new CameraDevice();s.arm();check("different device not armed",Handler.tasks.size()==0);
 s=new Setup();CaptureAdvanced3.owner=new Object();s.arm();check("unknown owner not armed",Handler.tasks.size()==0);
 s=new Setup();s.arm();s.state.configured=true;s.timeout();check("asynchronous success cancels by state",s.executor.q.size()==0&&CaptureAdvanced3.retries==0);
 s=new Setup();s.arm();s.state.token++;s.timeout();check("new token invalidates timer",s.executor.q.size()==0);
 s=new Setup();s.arm();CaptureAdvanced3.owner=new Object();s.timeout();check("changed owner invalidates timer",s.executor.q.size()==0);
 s=new Setup();s.arm();s.timeout();s.state.configured=true;s.executor.flush();check("success racing executor is retained",CaptureAdvanced3.retries==0);
 s=new Setup();s.arm();s.timeout();s.state.token++;s.executor.flush();check("token racing executor is retained",CaptureAdvanced3.retries==0);
 s=new Setup();s.arm();ManualLens170.foreground=false;s.timeout();check("background camera not reopened",s.executor.q.size()==0);
 s=new Setup();s.arm();ManualLens170.ready=false;s.timeout();check("hidden unfocused camera not reopened",s.executor.q.size()==0);
 s=new Setup();s.arm();s.executor.reject=true;s.timeout();check("executor rejection contained",CaptureAdvanced3.retries==0);
 s=new Setup();s.arm();check("configured callback atomically wins",CameraSession1920.acceptConfigured1920(s.owner,s.state,42)&&s.state.configured);s.timeout();s.executor.flush();check("successful callback cannot be rolled back",CaptureAdvanced3.retries==0);
 s=new Setup();s.arm();s.timeout();s.executor.flush();check("late configured callback rejected after timeout",!CameraSession1920.acceptConfigured1920(s.owner,s.state,42)&&s.state.timedOutToken1920==42);
 s.state.token=43;s.state.restoring=false;s.state.maximum=true;check("next generation not poisoned by previous timeout",CameraSession1920.acceptConfigured1920(s.owner,s.state,43));
 s=new Setup();check("stale callback rejected",!CameraSession1920.acceptConfigured1920(s.owner,s.state,41)&&!s.state.configured);
 s=new Setup();CaptureAdvanced3.owner=new Object();check("wrong callback owner rejected",!CameraSession1920.acceptConfigured1920(s.owner,s.state,42));
 for(int k=0;k<20;k++){
  final Setup race=new Setup();race.arm();race.timeout();final boolean[] accepted={false};
  Thread a=new Thread(()->accepted[0]=CameraSession1920.acceptConfigured1920(race.owner,race.state,42));Thread b=new Thread(()->race.executor.flush());
  a.start();b.start();try{a.join();b.join();}catch(InterruptedException e){throw new AssertionError(e);}
  check("callback-timeout race "+k,(accepted[0]&&CaptureAdvanced3.retries==0)||(!accepted[0]&&CaptureAdvanced3.retries==1));
 }
 for(String l:log)System.out.println(l);System.out.println("PASS total="+assertions+" back="+backCount+" session="+(assertions-backCount)+"; host mocks, not Android/device execution");
 }
}'''
 }
 for name,text in files.items():p=src/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
 cmds=[['javac','-encoding','UTF-8','-d',str(out),*map(str,sorted(src.rglob('*.java'))),str(root/'BackRoute1920.java'),str(root/'CameraSession1920.java'),str(root/'stubs/com/hiro/ulike/SettingsReturn1918.java')],['java','-cp',str(out),'com.hiro.ulike.Tests1920']]
 text=''
 for cmd in cmds:
  p=subprocess.run(cmd,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True);text+=p.stdout;print(p.stdout,end='');p.check_returncode()
 (work/'host-tests1920.txt').write_text(text);return text
if __name__=='__main__':
 import sys
 test(Path(sys.argv[1]).resolve(),Path(sys.argv[2]).resolve())
