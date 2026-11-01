package com.hiro.ulike;
import android.hardware.camera2.*;
import android.media.*;
import java.lang.reflect.*;

/** Exercise the real existing Burst observers with an instrumentation spy. */
public final class BurstArrivalHook19103Test {
 static int assertions;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception {
  Capture1948Test.Owner owner=new Capture1948Test.Owner();owner.x0=new Object();
  ImageReader reader=owner.e0;Capture1948Test.ReaderState state=new Capture1948Test.ReaderState(reader);
  Image image=new Image(2,2,35,901,new Image.Plane[0]);reader.offer(image);
  Image output=BurstCapture1933.acquireYuv(owner,state,reader);
  check(output==image&&reader.acquired==1,"real YUV acquisition preserves identity and one original reader call");
  check(ShotContext1932.arrivals==1&&ShotContext1932.lastImage==image&&ShotContext1932.lastOwner==owner,"observer receives exact source and owner");
  check(ShotContext1932.lastStart<=ShotContext1932.lastEnd&&ShotContext1932.lastStart>0,"acquisition timestamps surround the call");
  Class<?> replayClass=Class.forName("com.hiro.ulike.BurstCapture1933$Replay");
  Constructor<?> constructor=replayClass.getDeclaredConstructors()[0];constructor.setAccessible(true);
  Object replay=constructor.newInstance(state,reader,image);
  Field field=BurstCapture1933.class.getDeclaredField("REPLAY");field.setAccessible(true);
  @SuppressWarnings("unchecked") ThreadLocal<Object> local=(ThreadLocal<Object>)field.get(null);local.set(replay);
  try {check(BurstCapture1933.acquireYuv(owner,state,reader)==image,"replay returns original retained image");}
  finally {local.remove();}
  check(reader.acquired==1&&ShotContext1932.arrivals==1,"replay neither reacquires nor overwrites true acquisition timing");
  CaptureRequest.Builder builder=new CaptureRequest.Builder();
  builder.set(CaptureRequest.CONTROL_CAPTURE_INTENT,1);TotalCaptureResult preview=new TotalCaptureResult();preview.request=builder.build();
  BurstCapture1933.observedResult(owner,preview);
  check(ShotContext1932.results==0,"preview callbacks cannot appear as still timing");
  builder.set(CaptureRequest.CONTROL_CAPTURE_INTENT,CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
  TotalCaptureResult still=new TotalCaptureResult();still.request=builder.build();
  BurstCapture1933.observedResult(owner,still);
  check(ShotContext1932.results==1&&ShotContext1932.lastResult==still,"ordinary still result observer preserves exact object identity");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"real_burst_hooks_verified\":true,\"replay_does_not_retime_image\":true,\"preview_results_excluded\":true}");
 }
}
