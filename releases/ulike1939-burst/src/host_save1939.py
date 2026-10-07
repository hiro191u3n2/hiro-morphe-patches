#!/usr/bin/env python3
"""Exercise real photo queue helpers through the pinned reflection ABI."""
from pathlib import Path
import importlib.util
import json
import subprocess

HARNESS=r'''package com.hiro.ulike;
import java.util.*;import java.util.concurrent.*;import android.graphics.Bitmap;
public class SaveHarness1935 {
 static int assertions;static synchronized void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
 static final BlockingQueue<Encoded> processing=new LinkedBlockingQueue<Encoded>();
 static final List<String> completed=Collections.synchronizedList(new ArrayList<String>());
 static volatile int finishes;static volatile boolean failNext;
 public static class Camera{final int code;Camera(int c){code=c;}public int D1(){return code;}}
 static class Encoded{
  final Bitmap bitmap;final int id,noise,rotation,direction;final boolean colour,fixed;
  final CountDownLatch startEncoding=new CountDownLatch(1),encoderStarted=new CountDownLatch(1),release=new CountDownLatch(1);
  Encoded(Bitmap b,int r,int d){bitmap=b;id=ShotContext1932.forBitmap(b).id;noise=AsyncSave1935.settings(PhotoDetail.live).noiseLevel;colour=AsyncSave1935.chroma(ChromaPipeline177.live);fixed=SaveQuality2.isFixed245Enabled();rotation=r;direction=d;}
 }
 public static String encode(i.o.a.q.c.c.b.e controller,Bitmap b,int rotation,int direction)throws Exception{
  Encoded e=new Encoded(b,rotation,direction);controller.e=b.getWidth();controller.f=b.getHeight();processing.add(e);
  if(!e.startEncoding.await(5,TimeUnit.SECONDS))throw new AssertionError("postprocess wait timeout");
  check(AsyncSave1935.settings(PhotoDetail.live).noiseLevel==e.noise,"fallback detail resolves original photo snapshot");
  AsyncSave1935.encoding(b);e.encoderStarted.countDown();
  if(!e.release.await(5,TimeUnit.SECONDS))throw new AssertionError("encode wait timeout");
  if(b.isRecycled())throw new AssertionError("bitmap recycled while processing");
  if(failNext){failNext=false;throw new IllegalStateException("scripted encoding failure");}
  return "photo-"+e.id;
 }
 static Encoded next()throws Exception{Encoded e=processing.poll(5,TimeUnit.SECONDS);check(e!=null,"actual photo reaches serial processor");return e;}
 static void finish(Encoded e)throws Exception{e.startEncoding.countDown();check(e.encoderStarted.await(5,TimeUnit.SECONDS),"encoder starts");e.release.countDown();}
 static void waitFor(java.util.function.BooleanSupplier p,String why)throws Exception{long end=System.nanoTime()+5000000000L;while(!p.getAsBoolean()&&System.nanoTime()<end)Thread.sleep(2);check(p.getAsBoolean(),why);}
 static Bitmap source(int id){Bitmap b=new Bitmap(new int[]{id,id+1,id+2,id+3});ShotContext1932.bind(b,id);i.f.l.n.s.a.b().image=b;return b;}
 static void idle()throws Exception{waitFor(()->SaveQueue1935.count()==0,"all actual photos drain");check(ExitJobs185.count()==0,"exit accounting balanced");}
 public static void main(String[]args)throws Exception{
  i.o.a.b1.a.b.f.a manager=new i.o.a.b1.a.b.f.a(new i.o.a.b1.a.b.f.a.c(){public void a(){finishes++;}public void b(boolean ok,int cost,String path,String error){completed.add(ok?path:"failure");}});
  Bitmap s1=source(101);AsyncSave1935.submitAuto(manager,90,2);Encoded first=next();
  waitFor(()->finishes==1,"manual shutter reopens during postprocessing before encoder begins");
  check(first.encoderStarted.getCount()==1,"early release does not require encoder handoff");check(!AsyncSave1935.captureBlocked(),"second real tap admitted during processing");
  check(first.bitmap!=s1,"photo owns full copy");s1.pixels[0]=999;check(first.bitmap.pixels[0]==101,"SDK bitmap reuse cannot change retained photo");
  check(first.bitmap.getConfig()==s1.getConfig()&&first.bitmap.colourSpace==s1.colourSpace,"copy keeps exact source pixel config and color space");
  CaptureYuv.state.yuv.w=6000;CaptureYuv.state.yuv.h=4000;check(AsyncSave1935.captureBlocked(),"current next reader dimensions bound overlap memory");CaptureYuv.state.yuv.w=2;CaptureYuv.state.yuv.h=2;
  CaptureYuv.ready=false;check(AsyncSave1935.captureBlocked(),"reader reconfiguration holds real shutter");CaptureYuv.ready=true;
  CaptureYuv.state.yuv.format=256;check(AsyncSave1935.captureBlocked(),"unknown non-YUV reader held");CaptureYuv.state.yuv.format=35;
  Object old=CaptureYuv.owner.e0;CaptureYuv.owner.e0=new Object();check(AsyncSave1935.captureBlocked(),"native reader identity replacement held");CaptureYuv.owner.e0=old;
  PhotoDetail.live=new PhotoDetail.Settings(3);ChromaPipeline177.live=false;SaveQuality2.live=false;Bitmap s2=source(202);AsyncSave1935.submitAuto(manager,270,1);
  waitFor(()->finishes==2,"second photo copy releases native SDK ownership independently");check(AsyncSave1935.captureBlocked(),"two actual photos bound the memory queue");check(SaveQueue1935.count()==2,"two photo ownership slots only");
  PhotoDetail.live=new PhotoDetail.Settings(4);ChromaPipeline177.live=true;SaveQuality2.live=true;
  int beforeFailure=SaveQuality2.failures;long started=System.nanoTime();AsyncSave1935.submitAuto(manager,0,0);
  check(System.nanoTime()-started<500000000L,"saturated handoff never blocks UI waiting for save");
  waitFor(()->SaveQuality2.failures==beforeFailure+1,"unexpected saturated handoff fails visibly");check(finishes==2,"reservation rejection cannot reset newer native capture");check(SaveQueue1935.count()==2,"rejected request is never stored for later execution");
  check(processing.isEmpty(),"pixel processing and encoder stay serialized");finish(first);Encoded second=next();
  waitFor(()->completed.size()==1,"first notification delivered before second save completion");
  check(first.id==101&&second.id==202,"exact source metadata stays attached per photo");
  check(first.noise==1&&first.colour&&first.fixed,"first detail, chroma and dimensions frozen");check(second.noise==3&&!second.colour&&!second.fixed,"queued photo options cannot drift to current controls");
  check(first.rotation==90&&first.direction==2&&second.rotation==270&&second.direction==1,"rotation/direction fields preserve native ordering");check(second.bitmap.pixels[0]==202,"second manual tap has its own actual frame");check(first.bitmap.isRecycled(),"first bitmap retained until its encoder closes");
  finish(second);idle();check(completed.equals(Arrays.asList("photo-101","photo-202")),"all photos published FIFO with separate receipts");check(finishes==2,"completion cannot issue duplicate shutter-ready callbacks");check(processing.isEmpty(),"no automatic or delayed capture after queue drain");
  Bitmap s3=source(303);failNext=true;AsyncSave1935.submitAuto(manager,0,0);Encoded third=next();finish(third);idle();check(completed.get(2).equals("failure"),"save failure notified");check(third.bitmap.isRecycled(),"failed save releases owned photo");
  int failures=SaveQuality2.failures;i.f.l.n.s.a.b().image=null;AsyncSave1935.submitAuto(manager,0,0);idle();waitFor(()->SaveQuality2.failures==failures+1,"preparation failure visible");check(!AsyncSave1935.captureBlocked(),"failed preparation cannot wedge next manual tap");
  for(int mode=0;mode<5;mode++){
   int prior=finishes;Bitmap s=source(400+mode);
   if(mode==0)Bitmap.failCopy=true;if(mode==1)s.config=Bitmap.Config.HARDWARE;if(mode==2)s.config=null;if(mode==3)s.gainmap=true;if(mode==4)s.config=Bitmap.Config.RGBA_F16;
   AsyncSave1935.submitAuto(manager,0,0);Encoded e=next();
   check(e.bitmap==s,"unsupported exact-copy route borrows unmodified source serially "+mode);
   check(AsyncSave1935.captureBlocked()&&finishes==prior,"source still in use keeps shutter busy "+mode);
   e.startEncoding.countDown();check(e.encoderStarted.await(5,TimeUnit.SECONDS),"serial fallback encoder reached");check(finishes==prior,"serial fallback remains held through encoding");e.release.countDown();idle();Bitmap.failCopy=false;
   check(!s.isRecycled(),"SDK-owned source never recycled by queue "+mode);check(finishes==prior+1,"serial fallback completion releases exactly once "+mode);
  }
  long boundary=SaveQueue1935.RESERVE+4*20+4*4;
  check(!SaveQueue1935.processingMemoryAllows(boundary-1,4,4),"early overlap requires full extra output reserve");check(SaveQueue1935.processingMemoryAllows(boundary,4,4),"early overlap exact memory boundary");check(!SaveQueue1935.processingMemoryAllows(Long.MAX_VALUE,4,32000001),"unreviewed output dimensions never overlap");
  check(!SaveQueue1935.memoryAllows(SaveQueue1935.RESERVE+79,4),"late handoff retains old memory reserve");check(SaveQueue1935.memoryAllows(SaveQueue1935.RESERVE+80,4),"late handoff memory boundary");
  check(AsyncSave1935.readiness(new Camera(7))==7,"native readiness failure preserved");check(AsyncSave1935.readiness(new Camera(0))==0,"native readiness unchanged idle");
  for(int mode=1;mode<=3;mode++){
   android.os.Handler.mode=mode;source(500+mode);AsyncSave1935.submitAuto(manager,0,0);Encoded e=next();finish(e);idle();check(e.bitmap.isRecycled(),"notification posting failure cannot leak photo");
   i.f.l.n.s.a.b().image=null;AsyncSave1935.submitAuto(manager,0,0);idle();
  }
  android.os.Handler.mode=0;
  System.out.println("PASS manual-save-response assertions="+assertions+" scenarios=21 device_tested=false");
 }
}'''

def test(root,work,android=None):
    root=Path(root);work=Path(work)/'host-save1939';work.mkdir(parents=True,exist_ok=True)
    spec=importlib.util.spec_from_file_location('save1935_fixture_base',root/'host_save1935.py')
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    fixtures=dict(module.FIXTURES)
    fixtures['android/graphics/Bitmap.java']='''package android.graphics; public class Bitmap {public enum Config{ARGB_8888,RGB_565,HARDWARE,RGBA_F16}public static volatile boolean failCopy;public int[] pixels;boolean recycled;public Config config=Config.ARGB_8888;public boolean gainmap;public Object colourSpace=new Object();public Bitmap(int[] p){pixels=p;}public int getWidth(){return 2;}public int getHeight(){return 2;}public Config getConfig(){return config;}public boolean hasGainmap(){return gainmap;}public Bitmap copy(Config c,boolean m){if(failCopy)throw new OutOfMemoryError("scripted copy");Bitmap b=new Bitmap(pixels.clone());b.config=c;b.colourSpace=colourSpace;b.gainmap=gainmap;return b;}public void recycle(){recycled=true;}public boolean isRecycled(){return recycled;}}'''
    fixtures['android/os/Build.java']='package android.os;public class Build{public static class VERSION{public static int SDK_INT=34;}}'
    fixtures['com/hiro/ulike/SaveQuality2.java']=fixtures['com/hiro/ulike/SaveQuality2.java'].replace('public static void showSaveFailure()', 'public static int[] output186(int w,int h,int r,boolean f){return new int[]{w,h};}public static void showSaveFailure()')
    fixtures['com/hiro/ulike/SaveHarness1935.java']=HARNESS
    source=work/'fixtures';classes=work/'classes';classes.mkdir(exist_ok=True)
    for name,text in fixtures.items():
        f=source/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(text)
    command=['javac','-encoding','UTF-8','-source','8','-target','8','-d',str(classes),str(root/'AsyncSave1935.java'),str(root/'SaveQueue1935.java')]+[str(p) for p in sorted(source.rglob('*.java'))]
    compile=subprocess.run(command,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=120);(work/'compile.log').write_text(compile.stdout)
    if compile.returncode:raise RuntimeError(compile.stdout)
    result=subprocess.run(['java','-Xmx256m','-cp',str(classes),'com.hiro.ulike.SaveHarness1935'],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=45);(work/'result.txt').write_text(result.stdout)
    if result.returncode:raise RuntimeError(result.stdout)
    n=int(result.stdout.split('assertions=')[1].split()[0]);out={'suite':'save1939','status':'passed','passed':True,'assertions':n,'scenarios':21,'device_tested':False};(work/'result.json').write_text(json.dumps(out,indent=2));return out

if __name__=='__main__':
    import sys
    print(json.dumps(test(Path(__file__).parent,Path(sys.argv[1])),indent=2))
