package com.hiro.ulike;
import android.graphics.Bitmap;
import android.media.Image;
import java.lang.reflect.Method;

/** Real capture binding + real timing recorder; scripted camera ABI only. */
public final class CaptureProvenance1999Test {
 static int assertions;
 static void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
 static ProcessingTiming1947.Trace start(Object cb){CaptureBinding1991Test.start(cb);return ProcessingTiming1947.exactForKey1999(cb);}
 static String render(ProcessingTiming1947.Trace t)throws Exception{return Timing1982Test.render(t);}
 static void ambiguous()throws Exception{
  Object cb=new Object(),delivery=new Object();ProcessingTiming1947.Trace t=start(cb);
  CaptureBinding1991Test.Owner owner=new CaptureBinding1991Test.Owner(cb);
  ShotContext1932.received(owner,new Image(101,4080,3060),CaptureBinding1991Test.result(101));
  check(t.captureMetadataObserved1995&&t.captureIso1995==100,"first exact sensor metadata observed");
  ShotContext1932.received(owner,new Image(102,4080,3060),CaptureBinding1991Test.result(102));
  check(!t.captureMetadataObserved1995&&t.captureIso1995==0&&t.captureWidth1995==0,"ambiguous callback clears previously recorded ISO and dimensions");
  Bitmap image=new Bitmap(1920,1440);ShotContext1932.bindDelivery(cb,delivery,image);
  check(ShotContext1932.forBitmap(image)==ShotContext1932.UNKNOWN,"ambiguous metadata stays unavailable for pixel decisions");
  check(ProcessingTiming1947.exactForKey1999(image)==t&&ProcessingTiming1947.exactForKey1999(delivery)==t,"metadata rejection preserves exact callback-to-bitmap trace");
  check(t.events1991[ProcessingTiming1947.BITMAP_DELIVERED1991]>0,"actual rendered delivery is still timed");
  check(t.bitmapDimensions1999[0]==1920&&t.bitmapDimensions1999[1]==1440,"rendered bitmap dimensions remain measured despite missing camera metadata");
  check(render(t).contains("複数の入力時刻が競合・撮影条件は無効化"),"ambiguous reason explicit");
  check(!ShotContext1932.idle1953(),"invalid metadata cannot release background work before save");
  ProcessingTiming1947.finish(t,true);check(ShotContext1932.idle1953(),"actual save terminal releases invalid-metadata shot without TTL wait");
  ProcessingTiming1947.captureMetadata1995(t,800,1000,2,4080,3060);
  check(!t.captureMetadataObserved1995,"late metadata cannot repair or rewrite finished ambiguous shot");
 }
 static void dimensionsAndIsolation()throws Exception{
  Object cb=new Object();ProcessingTiming1947.Trace t=start(cb);Bitmap image=new Bitmap(1920,1440);
  ShotContext1932.bindDelivery(cb,new Object(),image);
  check(!t.captureMetadataObserved1995&&t.bitmapDimensions1999[0]==1920,"missing sensor result does not hide actual Bitmap dimensions or invent metadata");
  Method evict=ProcessingTiming1947.class.getDeclaredMethod("removeKey",Object.class);evict.setAccessible(true);evict.invoke(null,image);
  check(ProcessingTiming1947.exactForKey1999(image)==null&&ShotContext1932.timingForBitmap1999(image)==t,"independent timing-key eviction retains exact source owner without using a latest callback");
  check(ShotContext1932.timingForBitmap1999(new Bitmap(1920,1440))==null,"equal dimensions never recover another bitmap's trace");
  ProcessingTiming1947.bind(image,ShotContext1932.timingForBitmap1999(image));
  ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(t);
  Object unrelated=new Object();check(ProcessingTiming1947.forKey(unrelated)==t,"legacy lexical fallback retained for explicit pipeline calls");
  check(ProcessingTiming1947.exactForKey1999(unrelated)==null,"shared holder admission cannot borrow unrelated thread scope");
  ProcessingTiming1947.restore(scope);
  ProcessingTiming1947.bitmapInput1999(t,1,1920,1440);ProcessingTiming1947.bitmapInput1999(t,2,1920,1440);
  ProcessingTiming1947.bitmapInput1999(t,3,1920,1440);ProcessingTiming1947.bitmapInput1999(t,4,1920,1440);
  ProcessingTiming1947.saveInput1999(t,91,1,1);ProcessingTiming1947.output(t,4284,5712,"画質補正");
  String text=render(t);
  check(text.contains("保存ID 91")&&text.contains("取得Bitmapの元撮影記録に一致"),"capture and save identities are separate explicit counters");
  check(text.contains("保存取得 1920×1440（2764800画素）")&&text.contains("ノイズ処理入力 1920×1440（2764800画素）"),"actual save and noise domains visible independently");
  check(text.contains("保存取得 2764800画素 → 出力 24470208画素"),"larger output never described as high resolution input");
  ProcessingTiming1947.bitmapInput1999(t,1,4080,3060);check(t.bitmapConflictMask1999==2&&t.bitmapDimensions1999[2]==1920,"conflicting repeated dimension retained as ambiguity without overwriting first evidence");
  ProcessingTiming1947.bitmapInput1999(t,-1,1,1);ProcessingTiming1947.bitmapInput1999(t,5,1,1);ProcessingTiming1947.bitmapInput1999(t,2,0,1);
  check(t.bitmapConflictMask1999==2&&t.bitmapDimensions1999[4]==1920,"invalid observations cannot corrupt neighboring stages");
  ProcessingTiming1947.finish(t,true);String done=render(t);ProcessingTiming1947.bitmapInput1999(t,4,2,2);ProcessingTiming1947.saveInput1999(t,92,2,2);
  check(done.equals(render(t)),"terminal diagnostic is immutable");
  Object failed=new Object();ProcessingTiming1947.Trace f=start(failed);ShotContext1932.failed(failed,-1);Bitmap late=new Bitmap(8,8);
  ShotContext1932.bindDelivery(failed,new Object(),late);
  check(ProcessingTiming1947.exactForKey1999(late)==null&&f.events1991[3]==0,"capture failure is distinct from metadata ambiguity");
 }
 static void candidateReasons()throws Exception{
  Object cb=new Object();ProcessingTiming1947.Trace t=start(cb);CaptureBinding1991Test.Owner owner=new CaptureBinding1991Test.Owner(cb);
  ShotContext1932.received(owner,new Image(101,4080,3060),null);
  check(render(t).contains("撮影条件の照合: 撮影結果なし"),"null capture result explains missing ISO");
  ShotContext1932.received(owner,new Image(101,4080,3060),CaptureBinding1991Test.result(102));
  check(render(t).contains("Imageと撮影結果の時刻不一致"),"unmatched metadata reason explicit");
  ShotContext1932.received(owner,new Image(101,4080,3060),CaptureBinding1991Test.result(101));
  Bitmap small=new Bitmap(1920,1440);ShotContext1932.bindDelivery(cb,new Object(),small);ProcessingTiming1947.bitmapInput1999(t,1,1920,1440);
  check(render(t).contains("保存取得Bitmapは一致確認済みCamera2入力より小さい"),"camera versus bitmap dimensions distinguished without assuming cause");
  ProcessingTiming1947.finish(t,true);
 }
 static void history()throws Exception{
  RecentCapture1998Test.clear();ProcessingTiming1947.Trace a=RecentCapture1998Test.trace(8001,3395),b=RecentCapture1998Test.trace(8002,1057);
  for(int stage=0;stage<5;stage++){ProcessingTiming1947.bitmapInput1999(a,stage,3060,4080);ProcessingTiming1947.bitmapInput1999(b,stage,1440,1920);}
  ProcessingTiming1947.saveInput1999(a,11,1,1);ProcessingTiming1947.saveInput1999(b,12,1,1);
  RecentCapture1998Test.done(a);RecentCapture1998Test.done(b);String text=RecentCapture1998Test.recent();
  check(text.contains("比較可能な2枚以上の組なし"),"same camera metadata and output cannot group different actual save inputs");
  check(text.contains("3060×4080（12484800画素）")&&text.contains("1440×1920（2764800画素）"),"recent history preserves actual input pixels for fast-shot investigation");
  check(text.contains("保存ID 11")&&text.contains("保存ID 12"),"history retains each exact save identity");
 }
 public static void main(String[] args)throws Exception{
  ambiguous();dimensionsAndIsolation();candidateReasons();history();
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"capture_bitmap_provenance_verified\":true,\"ambiguous_metadata_trace_preserved\":true,\"exact_input_lookup_verified\":true}");
 }
}
