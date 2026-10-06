from pathlib import Path
import subprocess, shutil

def test(root,work):
    folder=work/'start-tests';folder.mkdir();src=folder/'src';src.mkdir();classes=folder/'classes';classes.mkdir()
    sources={
    'android/os/SystemClock.java': 'package android.os;public class SystemClock{public static long now=100;public static long uptimeMillis(){return now;}}',
    'android/util/Log.java': 'package android.util;public class Log{public static int i(String t,String m){return 0;}public static int w(String t,String m,Throwable e){return 0;}}',
    'android/os/Handler.java': '''package android.os;import java.util.*;public class Handler{public boolean accept=true;public final Map<Runnable,Long> tasks=new IdentityHashMap<>();public boolean postDelayed(Runnable r,long d){if(!accept)return false;tasks.put(r,SystemClock.now+d);return true;}public void removeCallbacks(Runnable r){tasks.remove(r);}public void until(long t){int steps=0;while(true){Runnable next=null;long at=Long.MAX_VALUE;for(Map.Entry<Runnable,Long>e:tasks.entrySet())if(e.getValue()<at){at=e.getValue();next=e.getKey();}if(next==null||at>t)break;if(++steps>1000)throw new AssertionError("runaway");SystemClock.now=at;tasks.remove(next);next.run();}SystemClock.now=t;}}''',
    'com/ss/android/vesdk/ConcurrentList.java':'package com.ss.android.vesdk;import java.util.*;public class ConcurrentList<T>{public final List<T>list=new ArrayList<>();public boolean isEmpty(){return list.isEmpty();}public List<T>getImmutableList(){return new ArrayList<>(list);}}',
    'com/ss/android/vesdk/frame/TECapturePipeline.java':'package com.ss.android.vesdk.frame;public class TECapturePipeline{public boolean preview=true,valid=true;public boolean isPreview(){return preview;}public boolean isValid(){return valid;}}',
    'com/ss/android/vesdk/VECameraCapture.java':'''package com.ss.android.vesdk;import java.util.concurrent.atomic.AtomicBoolean;import com.ss.android.vesdk.frame.TECapturePipeline;
public class VECameraCapture {public ConcurrentList<TECapturePipeline>n;public AtomicBoolean p=new AtomicBoolean(true);public int starts,accepted,forced=Integer.MAX_VALUE;public RuntimeException failure;public Runnable during;
 public int startPreview(){starts++;if(failure!=null)throw failure;if(during!=null)during.run();if(forced!=Integer.MAX_VALUE)return forced;if(n==null||n.isEmpty())return -100;if(!p.get())return -105;for(TECapturePipeline pipe:n.getImmutableList())if(pipe!=null&&pipe.isValid()){accepted++;return 0;}return -1;}}''',
    'com/hiro/ulike/OpticalZoom.java':'''package com.hiro.ulike;import android.os.Handler;import java.lang.ref.WeakReference;public class OpticalZoom{static final Handler MAIN=new Handler();static Object field(Object o,String s)throws ReflectiveOperationException{return o.getClass().getField(s).get(o);}static final class Route{long epoch=7;boolean rear=true,failed,configured;int frames;WeakReference<Object>mode;Route(Object m){mode=new WeakReference<>(m);}}}''',
    'com/hiro/ulike/ManualLens170.java':'''package com.hiro.ulike;import java.util.*;public class ManualLens170{static final Map<String,Object>f=new HashMap<>();static Object get(String s)throws ReflectiveOperationException{return f.get(s);}static long number(String s)throws ReflectiveOperationException{return ((Number)get(s)).longValue();}static boolean yes(String s)throws ReflectiveOperationException{return Boolean.TRUE.equals(get(s));}}''',
    'com/hiro/ulike/RearRestart1926.java':'package com.hiro.ulike;public class RearRestart1926{static boolean ready=true;public static boolean recoveryWindowReady(){return ready;}}',
    'com/hiro/ulike/ExitBusy1921.java':'package com.hiro.ulike;public class ExitBusy1921{static boolean busy;public static boolean captureBusy(boolean b){return busy;}}',
    'com/hiro/ulike/StartTest1927.java':r'''package com.hiro.ulike;
import java.util.*;import java.lang.ref.WeakReference;import java.lang.reflect.Field;import com.ss.android.vesdk.*;import com.ss.android.vesdk.frame.*;import android.os.*;
public class StartTest1927{
 static int checks;static VECameraCapture c;static OpticalZoom.Route r;static Mode m;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static class Camera{public int I=2;public Object M,K;}
 public static class Mode{public Object j=new Object(),d;public Camera g=new Camera();Mode(){g.M=this;g.K=j;}}
 static Map<Object,Object> map(String n)throws Exception{Field f=PreviewStart1927.class.getDeclaredField(n);f.setAccessible(true);return(Map)f.get(null);}
 static void reset()throws Exception{OpticalZoom.MAIN.tasks.clear();OpticalZoom.MAIN.accept=true;map("pending").clear();map("calls").clear();SystemClock.now=100;ManualLens170.f.clear();c=new VECameraCapture();m=new Mode();r=new OpticalZoom.Route(m);ManualLens170.f.put("capture",new WeakReference<Object>(c));ManualLens170.f.put("active",r);ManualLens170.f.put("foreground",true);ManualLens170.f.put("recording",false);ManualLens170.f.put("epoch",7L);RearRestart1926.ready=true;ExitBusy1921.busy=false;}
 static void ready(){c.n=new ConcurrentList<>();c.n.list.add(new TECapturePipeline());}
 static void initial(){check(PreviewStart1927.start(c)==-100,"original empty return unchanged");}
 static void tick(){OpticalZoom.MAIN.until(SystemClock.now+100);}
 public static void main(String[]a)throws Exception{
 reset();check(c.startPreview()==-100,"reproduce unpatched missing pipeline");ready();check(c.starts==1&&c.accepted==0,"unpatched setter does not replay lost start");
 reset();initial();tick();check(c.starts==1,"wait without native spin");ready();PreviewStart1927.pipelines(c);OpticalZoom.MAIN.until(SystemClock.now);check(c.starts==2&&c.accepted==1,"late pipeline event replays exactly once");OpticalZoom.MAIN.until(8000);check(c.starts==2&&map("pending").isEmpty(),"success is terminal");
 reset();initial();ready();tick();check(c.accepted==1,"late ready polled if setter event absent");
 reset();ready();check(PreviewStart1927.start(c)==0,"normal start unchanged");PreviewStart1927.pipelines(c);tick();check(c.starts==1,"do not restart normal active capture");
 reset();initial();ready();check(PreviewStart1927.start(c)==0,"native start supersedes pending");tick();check(c.starts==2,"no duplicate after native success");
 reset();initial();ready();for(int i=0;i<20;i++)PreviewStart1927.pipelines(c);check(OpticalZoom.MAIN.tasks.size()==1,"event coalescing");tick();check(c.starts==2,"event storm single replay");
 reset();initial();OpticalZoom.MAIN.until(6000);ready();PreviewStart1927.pipelines(c);tick();check(c.starts==1&&map("pending").isEmpty(),"bounded five-second deadline no camera restart");
 for(int kind=0;kind<12;kind++){
  reset();initial();ready();
  if(kind==0)ManualLens170.f.put("foreground",false);if(kind==1)ManualLens170.f.put("epoch",8L);if(kind==2)ManualLens170.f.put("capture",new WeakReference<Object>(new VECameraCapture()));if(kind==3)r.rear=false;if(kind==4)r.failed=true;if(kind==5)r.configured=true;if(kind==6)r.frames=1;if(kind==7)m.d=new Object();if(kind==8)ManualLens170.f.put("active",new OpticalZoom.Route(m));if(kind==9)ManualLens170.f.put("active",null);if(kind==10)r.epoch=8;if(kind==11)r.mode=new WeakReference<>(null);
  tick();check(c.starts==1,"stale/front/configured/closed exclusion "+kind);
  if(kind!=11)check(map("pending").isEmpty(),"stale work removed "+kind);
 }
 for(int mask=0;mask<64;mask++){
  reset();initial();ready();boolean blocked=mask!=0;
  if((mask&1)!=0)ManualLens170.f.put("recording",true);if((mask&2)!=0)ExitBusy1921.busy=true;if((mask&4)!=0)RearRestart1926.ready=false;if((mask&8)!=0)c.p.set(false);if((mask&16)!=0)m.g.I=1;if((mask&32)!=0)m.g.K=new Object();
  tick();check(c.starts==(blocked?1:2),"busy/window/native state gate "+mask);
  if(blocked){ManualLens170.f.put("recording",false);ExitBusy1921.busy=false;RearRestart1926.ready=true;c.p.set(true);m.g.I=2;m.g.K=m.j;tick();check(c.starts==2,"resume after safe readiness "+mask);}
 }
 for(int kind=0;kind<5;kind++){reset();ready();if(kind==0)c.n=null;if(kind==1)c.n.list.clear();if(kind==2)c.n.list.set(0,null);if(kind==3)c.n.list.get(0).preview=false;if(kind==4)c.n.list.get(0).valid=false;check(!PreviewStart1927.sourcesReady(c),"source validation "+kind);}
 reset();c.p.set(false);ready();check(PreviewStart1927.start(c)==-105,"native closed return");tick();check(c.starts==1,"wait opened state");c.p.set(true);tick();check(c.accepted==1,"opened after pipeline readiness");
 for(int error:new int[]{-401,-402,-407,-408,-409,-410,-425}){reset();c.forced=error;check(PreviewStart1927.start(c)==error,"return native error "+error);ready();tick();check(c.starts==1&&map("pending").isEmpty(),"no permission/driver retry "+error);}
 reset();ready();c.forced=-100;check(PreviewStart1927.start(c)==-100,"valid-input error unchanged");tick();check(c.starts==1,"not a general HAL retry loop");
 reset();r.rear=false;initial();ready();tick();check(c.starts==1,"front camera original-only path");
 reset();OpticalZoom.MAIN.accept=false;initial();check(map("pending").isEmpty(),"handler rejection does not leak pending");
 reset();c.failure=new IllegalStateException("native");try{PreviewStart1927.start(c);throw new AssertionError("not thrown");}catch(IllegalStateException expected){check(expected==c.failure,"native exception identity retained");}check(map("calls").isEmpty(),"exception releases call gate");
 reset();initial();ready();c.failure=new IllegalStateException("retry failure");tick();check(c.starts==2&&map("pending").isEmpty(),"optional retry exception ends cleanly");
 reset();initial();ready();map("calls").put(c,1);tick();check(c.starts==1,"ongoing original native call not duplicated");map("calls").clear();tick();check(c.accepted==1,"native call completion unblocks");
 reset();initial();ready();c.during=new Runnable(){public void run(){c.n=null;}};tick();check(c.starts==2,"first failed replay");ready();tick();check(c.starts==3,"second failed replay");ready();tick();check(c.starts==3&&map("pending").isEmpty(),"no more than two replays");
 System.out.println("PASS production lost-start handshake checks="+checks);
 }
}'''
    }
    for n,t in sources.items():p=src/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(t)
    shutil.copy(root/'PreviewStart1927.java',src/'com/hiro/ulike/PreviewStart1927.java')
    cp=subprocess.run(['javac','--release','8','-encoding','UTF-8','-d',str(classes),*map(str,sorted(src.rglob('*.java')))],capture_output=True,text=True);(folder/'javac.log').write_text(cp.stdout+cp.stderr);cp.check_returncode()
    cp=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.StartTest1927'],capture_output=True,text=True);(work/'host-start1927.txt').write_text(cp.stdout+cp.stderr);cp.check_returncode();return int(cp.stdout.strip().split('=')[-1])
