#!/usr/bin/env python3
"""Actual capture metadata and save-stage idle hooks with explicit ABI fixtures."""
from pathlib import Path
import argparse,json,os,re,shutil,subprocess
import host_metadata1933

ROUTE=r'''package com.hiro.ulike;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
/** Explicit observer only; full H21 background engine executes in its own suite. */
public final class WholeRoute1953 {
 public static final AtomicInteger captures=new AtomicInteger(),wakes=new AtomicInteger();
 public static volatile CountDownLatch woke;public static long retained;
 public static void foregroundStarted(){captures.incrementAndGet();}
 public static void wake(){wakes.incrementAndGet();CountDownLatch l=woke;if(l!=null)l.countDown();}
 public static long retainedBytes(){return retained;}
 public static void saved(Object trace,boolean success){}
 public static void initialize(android.content.Context context){}
}'''
TEST=r'''package com.hiro.ulike;
import android.graphics.Bitmap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
public final class CaptureIdle1953Test {
 static int assertions;
 static void yes(boolean v,String why){assertions++;if(!v)throw new AssertionError(why);}
 static Object begin(){Object cb=new Object();ShotContext1932.beginRecorder(new Object(),cb);return cb;}
 static void rejection(int mode){

  Object cb=begin();yes(!ShotContext1932.idle1953(),"real recorder admission marks capture busy before native input");
  Bitmap bitmap=mode==0?null:new Bitmap(mode==2?0:2,2);if(mode==1)bitmap.recycle();
  ShotContext1932.bindDelivery(cb,new Object(),bitmap);
  yes(!ShotContext1932.idle1953(),"null/recycled/empty delivery cannot falsely release active capture "+mode);
  ShotContext1932.failed(cb,-1);
  yes(ShotContext1932.idle1953(),"canonical failed capture releases invalid delivery ownership "+mode);
  yes(ProcessingTiming1947.forKey(cb).state==2,"canonical rejection ends exact failed timing trace "+mode);
  yes(ProcessingTiming1947.finished1953(ProcessingTiming1947.forKey(cb)),"rejection finishes its own callback trace "+mode);
 }
 static void terminal(){
  Object cb=begin();yes(!ShotContext1932.idle1953(),"undelivered live capture remains busy");
  ProcessingTiming1947.finish(ProcessingTiming1947.forKey(cb),false);
  yes(ShotContext1932.idle1953(),"terminal capture timing cannot retain an undelivered pending shot");
  Object next=begin();Bitmap b=new Bitmap(2,2);ShotContext1932.bindDelivery(next,new Object(),b);
  yes(!ShotContext1932.idle1953(),"delivered capture keeps background blocked until actual save terminal");
  yes(!ProcessingTiming1947.finished1953(ProcessingTiming1947.forKey(next)),"valid delivery does not prematurely finish save trace");
  ProcessingTiming1947.finish(ProcessingTiming1947.forKey(next),true);
  yes(ShotContext1932.idle1953(),"successful trace terminal releases delivered capture background guard");
  Object nativeCb=new Object();int capture=WholeRoute1953.captures.get();ShotContext1932.begin(null,nativeCb);
  yes(WholeRoute1953.captures.get()>capture,"native-only capture also invalidates background admission");
  yes(!ShotContext1932.idle1953(),"native-only callback holds capture busy");
  ShotContext1932.failed(nativeCb,-1);yes(ShotContext1932.idle1953(),"native failure restores capture idle");
 }
 static final class Owner {public Object x0;Owner(Object cb){x0=cb;}}
 static android.hardware.camera2.CaptureResult result(long stamp){
  android.hardware.camera2.CaptureResult r=new android.hardware.camera2.CaptureResult();
  r.put(android.hardware.camera2.CaptureResult.SENSOR_TIMESTAMP,stamp);return r;
 }
 static void invalidMetadata(){
  Object cb=begin();Owner owner=new Owner(cb);
  yes(ShotContext1932.receivedValues1933(owner,cb,101,2,2,result(101)),"first exact metadata reference accepted");
  yes(!ShotContext1932.receivedValues1933(owner,cb,102,2,2,result(102)),"second contradictory metadata reference invalidates original metadata");
  Bitmap output=new Bitmap(2,2);Object delivery=new Object();ShotContext1932.bindDelivery(cb,delivery,output);
  yes(ShotContext1932.forBitmap(output)==ShotContext1932.UNKNOWN,"metadata-invalid delivered bitmap does not claim valid camera data");
  yes(ProcessingTiming1947.forKey(output)==ProcessingTiming1947.forKey(cb)&&ProcessingTiming1947.forKey(delivery)==ProcessingTiming1947.forKey(cb),"valid delivered bitmap and callback retain exact original trace despite metadata rejection");
  yes(!ShotContext1932.idle1953(),"metadata invalid flag alone cannot start background work before save");
  ProcessingTiming1947.finish(ProcessingTiming1947.forKey(output),true);
  yes(ShotContext1932.idle1953(),"metadata-invalid delivered trace can finish and restore idle without TTL wait");
 }
 static void overlap(){
  Object first=begin();ProcessingTiming1947.Trace own=ProcessingTiming1947.forKey(first);
  yes(ProcessingTiming1947.otherCapturesIdle1953(own),"current exact capture alone does not contaminate its CPU reference");
  long epoch=ProcessingTiming1947.captureEpoch1953();
  ProcessingTiming1947.associate(first,own.shotId);
  yes(ProcessingTiming1947.captureEpoch1953()==epoch,"rebinding exact same capture cannot alter contention epoch");
  Object second=begin();ProcessingTiming1947.Trace competing=ProcessingTiming1947.forKey(second);
  yes(ProcessingTiming1947.captureEpoch1953()!=epoch,"new actual capture changes contention epoch before native save admission");
  yes(!ProcessingTiming1947.otherCapturesIdle1953(own),"other unfinished camera/native capture blocks CPU baseline eligibility");
  yes(!ProcessingTiming1947.otherCapturesIdle1953(competing),"exclusion follows exact trace identity, not global latest capture");
  int wakes=WholeRoute1953.wakes.get();ProcessingTiming1947.finish(competing,false);
  yes(WholeRoute1953.wakes.get()>wakes,"unrelated failed capture terminal wakes background idle admission");
  yes(ProcessingTiming1947.otherCapturesIdle1953(own),"terminal competing capture restores baseline eligibility");
  yes(ProcessingTiming1947.captureEpoch1953()!=epoch,"capture that finishes during measurement still invalidates earlier baseline epoch");
  ProcessingTiming1947.finish(own,true);
  yes(ProcessingTiming1947.otherCapturesIdle1953(null),"background calibration without capture trace requires every actual capture terminal");
 }
 static void queue()throws Exception{
  yes(SaveQueue1935.count()==0&&SaveQueue1935.idle1953(),"initial save queue has no owned stages");
  long room=SaveQueue1935.RESERVE+4L*24;
  yes(SaveQueue1935.memoryAllows(room,4),"memory boundary allows unchanged photo budget without background/native ownership");
  WholeRoute1953.retained=32;GpuFinish1953.retained=32;
  yes(!SaveQueue1935.memoryAllows(room,4)&&SaveQueue1935.memoryAllows(room+64,4),"both background native snapshots and cached GPU allocations count in capture admission");
  WholeRoute1953.retained=Long.MAX_VALUE;GpuFinish1953.retained=1;
  yes(!SaveQueue1935.memoryAllows(Long.MAX_VALUE,4),"native snapshot plus GPU accounting rejects arithmetic overflow");
  WholeRoute1953.retained=0;GpuFinish1953.retained=0;
  SaveQueue1935.reserve();yes(!SaveQueue1935.idle1953(),"reserved photo blocks background calibration");SaveQueue1935.release();
  yes(SaveQueue1935.idle1953(),"reservation release alone is idle when no stage owns a bitmap");
  final CountDownLatch encoding=new CountDownLatch(1),release=new CountDownLatch(1);
  final AtomicReference<Throwable> error=new AtomicReference<Throwable>();
  WholeRoute1953.woke=new CountDownLatch(1);int before=WholeRoute1953.wakes.get();
  Runnable job=new Runnable(){public void run(){try{
   SaveQueue1935.beginEncoding(this);SaveQueue1935.release();encoding.countDown();
   if(!release.await(5,TimeUnit.SECONDS))throw new AssertionError("test job release timed out");
  }catch(Throwable failure){error.set(failure);}}};
  SaveQueue1935.reserve();SaveQueue1935.submit(job,4,32);
  yes(encoding.await(5,TimeUnit.SECONDS),"real FIFO job reaches codec ownership");
  yes(SaveQueue1935.count()==0,"reservation can reach zero before actual encoder finally");
  yes(!SaveQueue1935.idle1953(),"count zero cannot launch GPU while codec/encoding still owned");
  yes(SaveQueue1935.nativeBytes()==32,"native bitmap budget retained until actual stage finally");
  release.countDown();yes(WholeRoute1953.woke.await(5,TimeUnit.SECONDS),"actual final stage cleanup wakes background admission");
  yes(error.get()==null,"actual queue job completes without lifecycle errors");
  yes(SaveQueue1935.idle1953()&&SaveQueue1935.nativeBytes()==0,"all queue stages and native ownership drain before idle");
  yes(WholeRoute1953.wakes.get()>before,"wake occurs after genuine stage release");
 }
 public static void main(String[]args)throws Exception{
  yes(ShotContext1932.idle1953(),"fresh capture registry is idle");
  for(int mode=0;mode<3;mode++)rejection(mode);
  terminal();invalidMetadata();overlap();queue();
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"invalid_metadata_delivery_keeps_timing_owner\":true,\"actual_capture_idle_hooks\":true,\"invalid_delivery_finishes_exact_trace\":true,\"native_capture_invalidates_background\":true,\"terminal_undelivered_capture_idle\":true,\"save_count_zero_not_early_idle\":true,\"actual_codec_finally_wakes_background\":true,\"physical_android_tested\":false}");
 }
}'''

def test(root,work,android=None):
 root=Path(root).resolve();out=Path(work)/'host-capture-idle1953';src=out/'src';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
 fixtures=dict(host_metadata1933.STUBS)
 fixtures.update({'com/hiro/ulike/WholeRoute1953.java':ROUTE,'com/hiro/ulike/CaptureIdle1953Test.java':TEST})
 sources=[]
 for name,code in fixtures.items():
  p=src/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(code);sources.append(p)
 sources += [root/'tests/compat1953-fixtures/com/hiro/ulike/GpuFinish1953.java']
 sources += [root/n for n in ('ShotContext1932.java','SaveQueue1935.java','ProcessingTiming1947.java','PerformanceHints1952.java','SpeedWorkers1935.java')]
 sources += list((root/'tests/timing1947-fixtures').rglob('*.java'))
 sources += [root/'tests/compat1952-fixtures/android/os'/n for n in ('Process.java','SystemClock.java')]
 javac=os.environ.get('ULIKE_JAVAC') or shutil.which('javac');java=os.environ.get('ULIKE_JAVA') or (str(Path(javac).with_name('java')) if javac else shutil.which('java'))
 compiler=[javac] if javac else [java,'com.sun.tools.javac.Main']
 for label,cmd in [('compile',compiler+['-source','8','-target','8','-Xlint:-options','-d',classes,*sources]),('test',[java,'-XX:ActiveProcessorCount=4','-cp',classes,'com.hiro.ulike.CaptureIdle1953Test'])]:
  p=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,timeout=90);(out/(label+'.log')).write_text(p.stdout+p.stderr)
  if p.returncode:raise RuntimeError(p.stdout[-3000:]+p.stderr[-10000:])
 result=json.loads(p.stdout);result['fixture_scope']='Actual ShotContext, ProcessingTiming and SaveQueue execute; Android metadata/preferences/UI/OS clock and background wake are explicit ABI observer fixtures.'
 (out/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--out',required=True);a=p.parse_args();print(json.dumps(test(Path(__file__).resolve().parent,a.out),indent=2))
