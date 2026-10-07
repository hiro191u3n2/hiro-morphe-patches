#!/usr/bin/env python3
"""Script actual T1 production helper across native reflection boundaries."""
from pathlib import Path
import json, subprocess

FIXTURES={
'android/graphics/Bitmap.java':'''package android.graphics; public class Bitmap {public enum Config{ARGB_8888,HARDWARE} public static volatile boolean failCopy;public int[] pixels;boolean recycled;public Bitmap(int[] p){pixels=p;}public int getWidth(){return 2;}public int getHeight(){return 2;}public Config getConfig(){return Config.ARGB_8888;}public Bitmap copy(Config c,boolean m){if(failCopy)throw new OutOfMemoryError("scripted copy");return new Bitmap(pixels.clone());}public void recycle(){recycled=true;}public boolean isRecycled(){return recycled;}}''',
'android/os/Looper.java':'''package android.os;public class Looper{static final Looper MAIN=new Looper();public static Looper getMainLooper(){return MAIN;}}''',
'android/os/Handler.java':'''package android.os;import java.util.concurrent.*;public class Handler{static final ExecutorService MAIN=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"fake-main");t.setDaemon(true);return t;});public static volatile int mode;public Handler(Looper l){if(mode==3)throw new OutOfMemoryError("handler allocation");}public boolean post(Runnable r){if(mode==1)return false;if(mode==2)throw new OutOfMemoryError("post allocation");MAIN.execute(r);return true;}}''',
'com/hiro/ulike/CaptureYuv.java':'''package com.hiro.ulike;import java.util.*;import java.lang.ref.*;public class CaptureYuv{public static boolean ready=true;public static class Reader{public int w=2,h=2,format=35;public int getWidth(){return w;}public int getHeight(){return h;}public int getImageFormat(){return format;}}public static class Owner{public Object e0=new Object();}public static class State{public boolean ready=true,closed;public Object original;public Reader yuv=new Reader();public State(Owner o){original=o.e0;}}public static Owner owner=new Owner();public static State state=new State(owner);private static WeakReference<Object> active=new WeakReference<Object>(owner);private static Map<Object,Object> states=new HashMap<Object,Object>();static{states.put(owner,state);}public static boolean isReady(){return ready;}}''',
'com/hiro/ulike/PhotoDetail.java':'''package com.hiro.ulike;public class PhotoDetail{public static volatile Settings live=new Settings(1);public static class Settings{public final int noiseLevel;Settings(int n){noiseLevel=n;}}public static Settings snapshot1932(){return live;}}''',
'com/hiro/ulike/ChromaPipeline177.java':'''package com.hiro.ulike;public class ChromaPipeline177{public static volatile boolean live=true;public static boolean enabled1932(){return live;}}''',
'com/hiro/ulike/SaveQuality2.java':'''package com.hiro.ulike;public class SaveQuality2{public static volatile boolean live=true;public static volatile int failures;public static boolean isFixed245Enabled(){return AsyncSave1935.fixed(live);}public static void showSaveFailure(){failures++;}}''',
'com/hiro/ulike/ExitJobs185.java':'''package com.hiro.ulike;public class ExitJobs185{public static int jobs;public static synchronized void begin(){jobs++;}public static synchronized void end(){if(--jobs<0)throw new AssertionError("exit balance");}public static synchronized int count(){return jobs;}}''',
'com/hiro/ulike/ShotContext1932.java':'''package com.hiro.ulike;import android.graphics.Bitmap;import java.util.*;public class ShotContext1932{static Map<Bitmap,Snapshot> map=Collections.synchronizedMap(new WeakHashMap<Bitmap,Snapshot>());public static class Snapshot{public int sourceWidth=2,sourceHeight=2;public int id;public Snapshot(int id){this.id=id;}}public static Snapshot forBitmap(Bitmap b){Snapshot s=map.get(b);return s==null?new Snapshot(0):s;}public static void copy(Bitmap a,Bitmap b){map.put(b,forBitmap(a));}public static void bind(Bitmap b,int id){map.put(b,new Snapshot(id));}}''',
'i/f/l/n/s/a.java':'''package i.f.l.n.s;import android.graphics.Bitmap;public class a{static final a INSTANCE=new a();public volatile Bitmap image;public static a b(){return INSTANCE;}public Bitmap e(){return image;}}''',
'i/f/l/n/q/y/n.java':'''package i.f.l.n.q.y;import android.graphics.Bitmap;public class n{public static Bitmap f(){return null;}}''',
'i/o/a/p/b.java':'''package i.o.a.p;public class b{public static final b a=new b();public void f(){}}''',
'i/o/a/b1/a/b/f/a.java':'''package i.o.a.b1.a.b.f;import i.o.a.q.c.c.b.e;public class a{public e a;public c b;public e.c c;public a(c listener){b=listener;c=(success,message,path,error)->b.b(success,0,path,error);a=new e(c);}public interface c{void a();void b(boolean s,int n,String p,String e);}}''',
'i/o/a/b1/a/b/f/a$a.java':'''package i.o.a.b1.a.b.f;public class a$a{public final int c,j;public final a k;public a$a(a manager,int direction,int rotation){k=manager;c=direction;j=rotation;}}''',
'i/o/a/q/c/c/b/e.java':'''package i.o.a.q.c.c.b;import android.graphics.Bitmap;import com.hiro.ulike.*;public class e{public c a;public String b="",g="";public long d,c;public int e,f;public e(c listener){a=listener;}public interface c{void a(boolean s,String m,String p,String er);}public String r(Bitmap bitmap,int rotation,int direction,boolean watermark)throws Exception{return com.hiro.ulike.SaveHarness1935.encode(this,bitmap,rotation,direction);}public void q(String p){if(a!=null)a.a(p!=null&&!p.isEmpty(),"",b,g);}}''',
'i/o/a/q/c/c/b/e$a.java':'''package i.o.a.q.c.c.b;import android.graphics.Bitmap;public class e$a{final e owner;public e$a(e o,Bitmap b,int r,int d,boolean w){owner=o;}public void c(){owner.q(owner.b);}}''',
'com/hiro/ulike/SaveHarness1935.java':r'''package com.hiro.ulike;
import java.util.*;import java.util.concurrent.*;import android.graphics.Bitmap;
public class SaveHarness1935 {
 static int assertions;static void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
 static final BlockingQueue<Encoded> ready=new LinkedBlockingQueue<Encoded>();
 static final List<String> completed=Collections.synchronizedList(new ArrayList<String>());
 static volatile int finishes;static volatile boolean failNext;
 public static class Camera{final int code;Camera(int c){code=c;}public int D1(){return code;}}
 static class Encoded{final Bitmap bitmap;final int id,noise,rotation,direction;final boolean colour,fixed;final CountDownLatch release=new CountDownLatch(1);Encoded(Bitmap b,int r,int d){bitmap=b;id=ShotContext1932.forBitmap(b).id;noise=AsyncSave1935.settings(PhotoDetail.live).noiseLevel;colour=AsyncSave1935.chroma(ChromaPipeline177.live);fixed=SaveQuality2.isFixed245Enabled();rotation=r;direction=d;}}
 public static String encode(i.o.a.q.c.c.b.e controller,Bitmap b,int rotation,int direction)throws Exception{
  Encoded e=new Encoded(b,rotation,direction);controller.e=b.getWidth();controller.f=b.getHeight();
  AsyncSave1935.encoding(b);ready.add(e);if(!e.release.await(5,TimeUnit.SECONDS))throw new AssertionError("test encode timeout");
  if(b.isRecycled())throw new AssertionError("bitmap recycled during encode");
  if(failNext){failNext=false;throw new IllegalStateException("scripted encoding failure");}
  return "photo-"+e.id;
 }
 static Encoded next()throws Exception{Encoded e=ready.poll(5,TimeUnit.SECONDS);check(e!=null,"next encoder");return e;}
 static void waitFor(java.util.function.BooleanSupplier p,String message)throws Exception{long end=System.nanoTime()+5000000000L;while(!p.getAsBoolean()&&System.nanoTime()<end)Thread.sleep(2);check(p.getAsBoolean(),message);}
 public static void main(String[]args)throws Exception{
  i.o.a.b1.a.b.f.a manager=new i.o.a.b1.a.b.f.a(new i.o.a.b1.a.b.f.a.c(){public void a(){finishes++;}public void b(boolean ok,int cost,String path,String error){completed.add(ok?path:"failure");}});
  Bitmap source1=new Bitmap(new int[]{11,12,13,14});ShotContext1932.bind(source1,101);i.f.l.n.s.a.b().image=source1;
  AsyncSave1935.submitAuto(manager,90,2);check(AsyncSave1935.captureBlocked(),"admission closes synchronously before native busy clears");
  Encoded first=next();waitFor(()->finishes==1,"capture reopened after owned final pixels");check(!AsyncSave1935.captureBlocked(),"real next capture allowed during first encode");
  CaptureYuv.state.yuv.w=6000;CaptureYuv.state.yuv.h=4000;check(AsyncSave1935.captureBlocked(),"larger next camera reader uses current dimensions and holds for memory");CaptureYuv.state.yuv.w=2;CaptureYuv.state.yuv.h=2;
  CaptureYuv.ready=false;check(AsyncSave1935.captureBlocked(),"reconfiguring reader cannot reuse prior photo geometry");CaptureYuv.ready=true;
  CaptureYuv.state.yuv.format=256;check(AsyncSave1935.captureBlocked(),"unverified non-YUV next route remains serial");CaptureYuv.state.yuv.format=35;
  Object originalReader=CaptureYuv.owner.e0;CaptureYuv.owner.e0=new Object();check(AsyncSave1935.captureBlocked(),"replaced native reader identity blocks overlap");CaptureYuv.owner.e0=originalReader;
  check(!AsyncSave1935.captureBlocked(),"stable current reader re-enables overlap");
  check(ExitJobs185.count()==1,"exit waits for encoder");check(first.bitmap!=source1,"owned copy");source1.pixels[0]=999;check(first.bitmap.pixels[0]==11,"original reused without corrupting encoder");
  PhotoDetail.live=new PhotoDetail.Settings(3);ChromaPipeline177.live=false;SaveQuality2.live=false;
  Bitmap source2=new Bitmap(new int[]{21,22,23,24});ShotContext1932.bind(source2,202);i.f.l.n.s.a.b().image=source2;
  AsyncSave1935.submitAuto(manager,270,1);check(AsyncSave1935.captureBlocked(),"third shutter blocked with two actual photos");check(SaveQueue1935.count()==2,"bounded two ownerships");
  PhotoDetail.live=new PhotoDetail.Settings(4);ChromaPipeline177.live=true;SaveQuality2.live=true;
  Thread.sleep(40);check(ready.isEmpty(),"FIFO encoder serial");check(!first.bitmap.isRecycled(),"encoder bitmap held until close");
  first.release.countDown();Encoded second=next();waitFor(()->completed.size()==1,"first save callback");
  check(first.id==101&&second.id==202,"distinct exact bitmap metadata");check(first.noise==1&&first.colour&&first.fixed,"first selected options frozen");check(second.noise==3&&!second.colour&&!second.fixed,"second options captured before later UI changes");
  check(first.rotation==90&&first.direction==2&&second.rotation==270&&second.direction==1,"native argument ordering");check(second.bitmap.pixels[0]==21,"second real source image");check(first.bitmap.isRecycled(),"first released only after encode");
  second.release.countDown();waitFor(()->SaveQueue1935.count()==0,"queue drained");check(completed.equals(Arrays.asList("photo-101","photo-202")),"FIFO receipts");check(finishes==2,"one capture-finish callback per shot");check(ExitJobs185.count()==0,"exit jobs balance");
  Bitmap source3=new Bitmap(new int[]{31,32,33,34});ShotContext1932.bind(source3,303);i.f.l.n.s.a.b().image=source3;failNext=true;AsyncSave1935.submitAuto(manager,0,0);Encoded third=next();third.release.countDown();waitFor(()->SaveQueue1935.count()==0,"failed save drain");check(completed.get(2).equals("failure"),"failed photo notified");check(third.bitmap.isRecycled(),"failed encoder releases own bitmap");check(ExitJobs185.count()==0,"failed save exit balance");
  i.f.l.n.s.a.b().image=null;AsyncSave1935.submitAuto(manager,0,0);waitFor(()->SaveQueue1935.count()==0,"missing source drain");waitFor(()->SaveQuality2.failures==1,"preparation error shown");check(ExitJobs185.count()==0,"preparation exit balance");
  check(!SaveQueue1935.memoryAllows(SaveQueue1935.RESERVE+80-1,4),"memory below exact bound holds shutter");check(SaveQueue1935.memoryAllows(SaveQueue1935.RESERVE+80,4),"memory at bound");check(!SaveQueue1935.memoryAllows(Long.MAX_VALUE,32000001),"unsupported source extent serializes");check(!AsyncSave1935.captureBlocked(),"idle shutter usable after failures");
  int completedBefore=completed.size(),finishesBefore=finishes;
  Bitmap source4=new Bitmap(new int[]{41,42,43,44});ShotContext1932.bind(source4,404);i.f.l.n.s.a.b().image=source4;Bitmap.failCopy=true;
  AsyncSave1935.submitAuto(manager,0,0);Encoded fourth=next();check(fourth.bitmap==source4,"copy OOM keeps exact source for serial save");check(AsyncSave1935.captureBlocked(),"copy fallback never permits another capture");check(finishes==finishesBefore,"copy fallback holds native capture finish");
  fourth.release.countDown();waitFor(()->SaveQueue1935.count()==0,"copy fallback drains");Bitmap.failCopy=false;check(!source4.isRecycled(),"borrowed native bitmap not recycled");check(completed.size()==completedBefore+1&&completed.get(completedBefore).equals("photo-404"),"copy fallback saves same quality photo");
  check(AsyncSave1935.readiness(new Camera(7))==7,"native readiness failure unchanged");check(AsyncSave1935.readiness(new Camera(0))==0,"native readiness success when idle");
  for(int mode=1;mode<=3;mode++){
   android.os.Handler.mode=mode;
   Bitmap source=new Bitmap(new int[]{51,52,53,54});ShotContext1932.bind(source,500+mode);i.f.l.n.s.a.b().image=source;
   AsyncSave1935.submitAuto(manager,0,0);Encoded encoded=next();encoded.release.countDown();waitFor(()->SaveQueue1935.count()==0,"notification failure drains");check(ExitJobs185.count()==0,"notification failure exit balance");check(encoded.bitmap.isRecycled(),"notification failure owned bitmap released");
   i.f.l.n.s.a.b().image=null;AsyncSave1935.submitAuto(manager,0,0);waitFor(()->SaveQueue1935.count()==0,"prepare notification failure drains");check(ExitJobs185.count()==0,"prepare notification failure exit balance");
  }
  android.os.Handler.mode=0;
  System.out.println("PASS T1 assertions="+assertions+" scenarios=15 device_tested=false");
 }
}'''}

def test(root, work):
    root,work=Path(root),Path(work)/'host-save1935';work.mkdir(parents=True,exist_ok=True)
    fixtures=work/'fixtures';classes=work/'classes';classes.mkdir(exist_ok=True)
    for name,text in FIXTURES.items():
        f=fixtures/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(text)
    cmd=['javac','-encoding','UTF-8','-source','8','-target','8','-d',str(classes),str(root/'AsyncSave1935.java'),str(root/'SaveQueue1935.java')]+[str(p) for p in sorted(fixtures.rglob('*.java'))]
    compile=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(work/'compile.log').write_text(compile.stdout)
    if compile.returncode:raise RuntimeError(compile.stdout)
    result=subprocess.run(['java','-Xmx256m','-cp',str(classes),'com.hiro.ulike.SaveHarness1935'],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=30);(work/'result.txt').write_text(result.stdout)
    if result.returncode:raise RuntimeError(result.stdout)
    n=int(result.stdout.split('assertions=')[1].split()[0]);out={'suite':'save1935','status':'passed','passed':True,'assertions':n,'scenarios':15,'device_tested':False};(work/'result.json').write_text(json.dumps(out,indent=2));return out
if __name__=='__main__':
 import sys;print(json.dumps(test(Path(__file__).parent,Path(sys.argv[1])),indent=2))
