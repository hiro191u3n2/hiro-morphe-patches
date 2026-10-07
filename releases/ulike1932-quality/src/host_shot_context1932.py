#!/usr/bin/env python3
"""Host verification of exact-shot metadata ownership; no Android device claims."""
import argparse
import json
from pathlib import Path
import subprocess

STUBS = {
    "android/graphics/Bitmap.java": """package android.graphics;
public final class Bitmap {
 private final int width,height; private boolean recycled;
 public Bitmap(int w,int h){width=w;height=h;}
 public int getWidth(){return width;} public int getHeight(){return height;}
 public boolean isRecycled(){return recycled;} public void recycle(){recycled=true;}
}""",
    "android/media/Image.java": """package android.media;
public final class Image {
 private final long timestamp; private final int width,height; public boolean closed;
 public Image(long t,int w,int h){timestamp=t;width=w;height=h;}
 public long getTimestamp(){if(closed)throw new IllegalStateException();return timestamp;}
 public int getWidth(){return width;} public int getHeight(){return height;}
}""",
    "android/os/Build.java": """package android.os;
public final class Build {public static final class VERSION {public static int SDK_INT=36;}}""",
    "android/util/SizeF.java": """package android.util;
public final class SizeF {private final float w,h;public SizeF(float x,float y){w=x;h=y;}
public float getWidth(){return w;}public float getHeight(){return h;}}""",
    "android/hardware/camera2/CameraDevice.java": """package android.hardware.camera2;
public final class CameraDevice {private final String id;
public CameraDevice(String value){id=value;} public String getId(){return id;}}""",
    "android/hardware/camera2/CameraCharacteristics.java": """package android.hardware.camera2;
public final class CameraCharacteristics {
 public static final int LENS_FACING_FRONT=0,LENS_FACING_BACK=1;
 public static final Key<Integer> LENS_FACING=new Key<Integer>();
 public static final Key<android.util.SizeF> SENSOR_INFO_PHYSICAL_SIZE=new Key<android.util.SizeF>();
 public static final class Key<T>{}
 private final java.util.Map<Key<?>,Object> values=new java.util.HashMap<Key<?>,Object>();
 public <T>void put(Key<T> key,T value){values.put(key,value);}
 @SuppressWarnings("unchecked")public <T>T get(Key<T> key){return (T)values.get(key);}
}""",
    "android/hardware/camera2/CaptureResult.java": """package android.hardware.camera2;
public class CaptureResult {
 public static final class Key<T>{}
 public static final Key<Long> SENSOR_TIMESTAMP=new Key<Long>();
 public static final Key<Integer> SENSOR_SENSITIVITY=new Key<Integer>();
 public static final Key<Long> SENSOR_EXPOSURE_TIME=new Key<Long>();
 public static final Key<Float> LENS_FOCAL_LENGTH=new Key<Float>();
 private final java.util.Map<Key<?>,Object> values=new java.util.HashMap<Key<?>,Object>();
 public <T>void put(Key<T> key,T value){values.put(key,value);}
 @SuppressWarnings("unchecked")public <T>T get(Key<T> key){return (T)values.get(key);}
}""",
    "android/hardware/camera2/TotalCaptureResult.java": """package android.hardware.camera2;
public class TotalCaptureResult extends CaptureResult {
 public final java.util.Map<String,CaptureResult> physical=new java.util.HashMap<String,CaptureResult>();
 public java.util.Map<String,CaptureResult> getPhysicalCameraResults(){return physical;}
}""",
    "com/ss/android/vesdk/VERecorder.java": """package com.ss.android.vesdk;
public final class VERecorder {}""",
    "com/ss/android/vesdk/VERecorder$13.java": """package com.ss.android.vesdk;
public final class VERecorder$13 {public final Object a; public final boolean b=false;
 public final VERecorder d; public VERecorder$13(VERecorder r,Object callback){d=r;a=callback;}}""",
    "com/ss/android/vesdk/TECameraVideoRecorder$60.java": """package com.ss.android.vesdk;
public final class TECameraVideoRecorder$60 {public final Object a;
 public TECameraVideoRecorder$60(Object callback){a=callback;}}""",
    "i/s/a/w/q.java": """package i.s.a.w;
public final class q {public static final class g {public final Object c;
 public g(Object callback){c=callback;}
 public static final class a {public final g a;public a(g parent){a=parent;}}
}}""",
    "com/hiro/ulike/OpticalZoom.java": """package com.hiro.ulike;
import android.hardware.camera2.*;
public final class OpticalZoom {
 private static final java.util.Map<CameraDevice,Route> routes=new java.util.IdentityHashMap<CameraDevice,Route>();
 public static final class Route {public String physicalId;public CameraCharacteristics lens;public int nominal;
  public Route(String p,CameraCharacteristics c,int n){physicalId=p;lens=c;nominal=n;}}
 public static void register(CameraDevice d,Route r){routes.put(d,r);}
 private static Route route(CameraDevice d){return routes.get(d);}
}""",
}

TEST = r"""package com.hiro.ulike;
import android.graphics.Bitmap;
import android.hardware.camera2.*;
import android.media.Image;
import com.ss.android.vesdk.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CyclicBarrier;
public final class ShotContextHost1932 {
 private static int checks;
 private static void check(boolean value,String description){checks++;if(!value)throw new AssertionError(description);}
 private static boolean near(float a,float b){return Math.abs(a-b)<0.00001f;}
 public static final class Owner {public Object x0;public CameraDevice j;public CameraCharacteristics a;
  Owner(Object cb,int facing){x0=cb;j=new CameraDevice("camera-"+facing);a=characteristics(facing);}}
 private static CameraCharacteristics characteristics(int facing){CameraCharacteristics c=new CameraCharacteristics();c.put(CameraCharacteristics.LENS_FACING,facing);return c;}
 private static TotalCaptureResult result(long timestamp,int iso){TotalCaptureResult r=new TotalCaptureResult();
  r.put(CaptureResult.SENSOR_TIMESTAMP,timestamp);r.put(CaptureResult.SENSOR_SENSITIVITY,iso);
  r.put(CaptureResult.SENSOR_EXPOSURE_TIME,20000000L);r.put(CaptureResult.LENS_FOCAL_LENGTH,6.3f);return r;}
 private static Bitmap deliver(Object cb){Bitmap b=new Bitmap(400,300);ShotContext1932.bindDelivery(cb,new Object(),b);return b;}
 private static void start(Object recorder,Object callback){ShotContext1932.beginRecorder(recorder,callback);ShotContext1932.begin(null,callback);}
 public static void main(String[] args)throws Exception {
  check(ShotContext1932.forBitmap(new Bitmap(2,2))==ShotContext1932.UNKNOWN,"unrelated bitmap must have no latest-result fallback");
  Object ra=new Object(),rb=new Object(),ca=new Object(),cb=new Object();
  ShotContext1932.observeBeauty(ra,"smooth",0.65f);ShotContext1932.observeBeauty(rb,"smooth",0f);
  start(ra,ca);start(rb,cb);ShotContext1932.observeBeauty(ra,"smooth",0.1f);
  Owner oa=new Owner(ca,0);ShotContext1932.received(oa,new Image(101,4080,3060),result(101,800));
  Bitmap ba=deliver(ca),bb=deliver(cb);
  ShotContext1932.Snapshot sa=ShotContext1932.forBitmap(ba),sb=ShotContext1932.forBitmap(bb);
  check(sa.metadataReliable&&sa.iso==800&&sa.exposureNanos==20000000L,"metadata from exact timestamp");
  check(sa.sensorTimestampNanos==101&&sa.sourceWidth==4080&&sa.sourceHeight==3060,"capture dimensions and timestamp");
  check(sa.lensFacing==0&&sa.lensKind==ShotContext1932.LENS_FRONT,"front metadata");
  check(near(sa.beautyStrength,0.65f),"beauty snapshot must not follow a later slider change");
  check(near(sb.beautyStrength,0f)&&!sb.metadataReliable,"other renderer explicitly off and no stale ISO");
  check(sa.shotId!=sb.shotId,"independent shot identities");
  Bitmap copy=new Bitmap(800,600);ShotContext1932.copy(ba,copy);
  check(ShotContext1932.forBitmap(copy)==sa,"explicit copy propagates immutable snapshot");
  ShotContext1932.forget(ba);check(ShotContext1932.forBitmap(ba)==ShotContext1932.UNKNOWN,"explicit cleanup");
  check(ShotContext1932.forBitmap(copy)==sa,"cleaning original does not remove distinct copy");
  ShotContext1932.transfer(new Object(),copy);
  check(ShotContext1932.forBitmap(copy)==ShotContext1932.UNKNOWN,"reused destination clears stale context if new source is unknown");

  Object mismatch=new Object();start(ra,mismatch);
  ShotContext1932.received(new Owner(mismatch,1),new Image(201,400,300),result(202,6400));
  ShotContext1932.Snapshot bad=ShotContext1932.forBitmap(deliver(mismatch));
  check(!bad.metadataReliable&&bad.iso==0,"mismatched timestamp discarded");
  Object noResult=new Object();start(ra,noResult);
  ShotContext1932.received(new Owner(noResult,1),new Image(300,400,300),null);
  check(!ShotContext1932.forBitmap(deliver(noResult)).metadataReliable,"missing result safe");

  Object physicalCb=new Object();start(ra,physicalCb);Owner po=new Owner(physicalCb,1);
  OpticalZoom.register(po.j,new OpticalZoom.Route("tele-5",characteristics(1),5));
  TotalCaptureResult logical=result(999,100);logical.physical.put("tele-5",result(401,1600));
  ShotContext1932.received(po,new Image(401,400,300),logical);
  ShotContext1932.Snapshot ps=ShotContext1932.forBitmap(deliver(physicalCb));
  check(ps.metadataReliable&&ps.iso==1600&&"tele-5".equals(ps.physicalId),"physical result chosen instead of logical ISO");
  check(ps.lensKind==ShotContext1932.LENS_TELEPHOTO,"verified route lens kind");
  Object crop=new Object();start(ra,crop);Owner cropped=new Owner(crop,1);
  OpticalZoom.register(cropped.j,new OpticalZoom.Route("",characteristics(1),5));
  ShotContext1932.received(cropped,new Image(451,400,300),result(451,800));
  check(ShotContext1932.forBitmap(deliver(crop)).lensKind==ShotContext1932.LENS_BACK,"logical digital zoom cannot claim a telephoto sensor");
  Object absent=new Object();start(ra,absent);Owner ap=new Owner(absent,1);
  OpticalZoom.register(ap.j,new OpticalZoom.Route("tele-missing",characteristics(1),3));
  ShotContext1932.received(ap,new Image(501,400,300),result(501,100));
  check(!ShotContext1932.forBitmap(deliver(absent)).metadataReliable,"missing physical result must not use logical metadata");

  VERecorder renderer=new VERecorder();Object callback=new Object();
  ShotContext1932.observeBeauty(renderer,"smooth",0.4f);
  TECameraVideoRecorder$60 nativeCb=new TECameraVideoRecorder$60(new VERecorder$13(renderer,callback));
  ShotContext1932.begin(null,nativeCb);
  i.s.a.w.q.g.a wrapped=new i.s.a.w.q.g.a(new i.s.a.w.q.g(nativeCb));
  ShotContext1932.received(new Owner(wrapped,0),new Image(601,400,300),result(601,320));
  ShotContext1932.Snapshot wrappedShot=ShotContext1932.forBitmap(deliver(callback));
  check(wrappedShot.iso==320&&near(wrappedShot.beautyStrength,0.4f),"stock wrapper chain and VERecorder$13.d beauty owner");

  Object reused=new Object();start(ra,reused);Bitmap first=deliver(reused);
  ShotContext1932.Snapshot firstSnapshot=ShotContext1932.forBitmap(first);
  start(rb,reused);Bitmap second=deliver(reused);
  check(ShotContext1932.forBitmap(first)==firstSnapshot,"reused callback must not rewrite old bitmap record");
  check(ShotContext1932.forBitmap(second).shotId!=firstSnapshot.shotId,"reused callback starts new shot");

  Object multi=new Object();start(ra,multi);Owner mo=new Owner(multi,0);
  ShotContext1932.received(mo,new Image(701,400,300),result(701,800));
  ShotContext1932.received(mo,new Image(702,400,300),result(702,200));
  check(ShotContext1932.forBitmap(deliver(multi))==ShotContext1932.UNKNOWN,"ambiguous multi-frame callback rejected");
  Object failed=new Object();start(ra,failed);ShotContext1932.failed(failed,-1);
  check(ShotContext1932.forBitmap(deliver(failed))==ShotContext1932.UNKNOWN,"capture failure invalidates only undelivered shot");

  ShotContext1932.observeBeautyResult(ra,"smooth",0.8f,-1);Object rc=new Object();start(ra,rc);
  check(near(ShotContext1932.forBitmap(deliver(rc)).beautyStrength,0.1f),"failed SDK update does not become effective state");
  ShotContext1932.invalidateBeauty(ra);Object invalidBeauty=new Object();start(ra,invalidBeauty);
  check(ShotContext1932.forBitmap(deliver(invalidBeauty)).beautyStrength<0,"composer change invalidates numeric beauty snapshot");
  ShotContext1932.observeBeauty(ra,"smooth",Float.NaN);Object nan=new Object();start(ra,nan);
  check(ShotContext1932.forBitmap(deliver(nan)).beautyStrength<0,"invalid beauty value is unknown, not off");
  ShotContext1932.observeBeautyArrayResult(ra,new String[]{"smooth","other"},new float[]{0.3f,0.9f},0);
  Object array=new Object();start(ra,array);check(near(ShotContext1932.forBitmap(deliver(array)).beautyStrength,0.3f),"tuned array smoothing observed");

  final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
  final CyclicBarrier round=new CyclicBarrier(4);
  Thread[] workers=new Thread[4];
  for(int k=0;k<workers.length;k++) {final int worker=k;workers[k]=new Thread(new Runnable(){public void run(){
   try{Object r=new Object();ShotContext1932.observeBeauty(r,"smooth",worker*0.2f);
    for(int i=0;i<100;i++){round.await();Object c=new Object();start(r,c);long t=10000+worker*1000+i;
     ShotContext1932.received(new Owner(c,worker%2),new Image(t,400,300),result(t,100+worker));
     ShotContext1932.Snapshot s=ShotContext1932.forBitmap(deliver(c));
     if(s.iso!=100+worker||s.sensorTimestampNanos!=t||!near(s.beautyStrength,worker*0.2f))throw new AssertionError("cross-thread contamination");
    }
   }catch(Throwable t){failure.compareAndSet(null,t);round.reset();}
  }});workers[k].start();}
  for(Thread t:workers)t.join();if(failure.get()!=null)throw new AssertionError(failure.get());
  check(true,"400 interleaved captures keep matching metadata");
  Field keys=ShotContext1932.class.getDeclaredField("SHOTS");keys.setAccessible(true);
  check(((List<?>)keys.get(null)).size()<=96,"bounded weak identity map");
  System.out.println("SHOT_CONTEXT_1932_PASS checks="+checks+" interleaved_captures=400");
 }
}
"""


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--javac", required=True)
    parser.add_argument("--java", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    output = Path(args.out).resolve()
    source_dir = output / "src"
    class_dir = output / "classes"
    files = []
    for relative, text in STUBS.items():
        path = source_dir / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text + "\n", encoding="utf-8")
        files.append(str(path))
    test_file = source_dir / "com/hiro/ulike/ShotContextHost1932.java"
    test_file.write_text(TEST, encoding="utf-8")
    helper = Path(__file__).resolve().with_name("ShotContext1932.java")
    files += [str(test_file), str(helper)]
    class_dir.mkdir(parents=True, exist_ok=True)
    subprocess.run([args.javac, "-source", "8", "-target", "8", "-Xlint:-options",
                    "-d", str(class_dir)] + files, check=True)
    run = subprocess.run([args.java, "-cp", str(class_dir),
                          "com.hiro.ulike.ShotContextHost1932"],
                         text=True, capture_output=True)
    if run.returncode:
        raise RuntimeError(run.stdout + run.stderr)
    print(run.stdout, end="")
    (output / "host-shot-context1932.json").write_text(json.dumps({
        "status": "passed", "scope": "host metadata ownership and callback isolation",
        "output": run.stdout.strip(), "physical_device_verified": False,
    }, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
