package com.hiro.ulike;
import android.media.Image;
import android.hardware.camera2.TotalCaptureResult;

/** Exact-image phase adoption; no wall-clock/sensor-clock conversion or inferred event. */
public final class CapturePhases19104Test {
 static int assertions;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static ProcessingTiming1947.Trace start(Object callback){CaptureBinding1991Test.start(callback);return ProcessingTiming1947.exactForKey1999(callback);}
 public static void main(String[]args)throws Exception {
  Object callback=new Object();ProcessingTiming1947.Trace trace=start(callback);
  CaptureBinding1991Test.Owner owner=new CaptureBinding1991Test.Owner(callback);
  Image image=new Image(9001,4080,3060);TotalCaptureResult result=CaptureBinding1991Test.result(9001);
  Object token=ShotContext1932.cameraOwner19103(owner);
  ShotContext1932.acquired19103(owner,token,image,System.nanoTime(),System.nanoTime());
  long guardStart=System.nanoTime(),guardEnd=System.nanoTime();
  ShotContext1932.cameraGuard19104(owner,image,guardStart,guardEnd);ShotContext1932.q0Entry19104(owner,image,result);
  check(trace.cameraPhases19104[0]==0&&trace.cameraPhases19104[2]==0,"early phases await input and result matching");
  ShotContext1932.received(owner,image,result);
  check(trace.cameraPhases19104[0]==guardStart&&trace.cameraPhases19104[1]==guardEnd&&trace.cameraPhases19104[2]>=guardEnd,"same-image guard and Q0 times are adopted");
  String text=Timing1982Test.render(trace);
  check(text.contains("Image取得完了→所有権guard開始")&&text.contains("guard完了→Q0入場（State同期・入力受渡し等）")&&text.contains("Q0入場→対応画像受信"),"post-acquisition interval split without calling State time kernel time");
  check(text.contains("追加撮影・融合準備を実行せず")&&text.contains("結果通知はStateへの格納完了時点ではありません"),"single-image policy and result entry limitation explicit");
  long q0=trace.cameraPhases19104[2];ShotContext1932.q0Entry19104(owner,image,result);ShotContext1932.cameraGuard19104(owner,image,System.nanoTime(),System.nanoTime());ShotContext1932.received(owner,image,result);
  check(trace.cameraPhases19104[0]==guardStart&&trace.cameraPhases19104[2]==q0,"replay cannot rewrite actual first guard or Q0 entry");
  ShotContext1932.received(owner,new Image(9002,4080,3060),CaptureBinding1991Test.result(9002));
  check(trace.cameraPhases19104[0]==0&&trace.cameraPhases19104[2]==0&&!trace.cameraPhasesObserved19104,"ambiguous input invalidates all derived camera phases");ProcessingTiming1947.finish(trace,true);

  callback=new Object();trace=start(callback);owner=new CaptureBinding1991Test.Owner(callback);token=ShotContext1932.cameraOwner19103(owner);
  image=new Image(Long.MAX_VALUE-901,4080,3060);result=CaptureBinding1991Test.result(Long.MAX_VALUE-901);
  ShotContext1932.acquired19103(owner,token,image,System.nanoTime(),System.nanoTime());
  ShotContext1932.cameraGuard19104(owner,image,1,2);ShotContext1932.q0Entry19104(owner,new Image(Long.MAX_VALUE-901,4080,3060),result);
  ShotContext1932.received(owner,image,result);
  check(trace.cameraPhases19104[0]==0&&trace.cameraPhases19104[2]==0,"pre-shot clock and equal-timestamp other Image cannot fabricate phase");
  check(Timing1982Test.render(trace).contains("Q0入場→対応画像受信: 未観測"),"missing phase is explicit");ProcessingTiming1947.finish(trace,true);

  callback=new Object();trace=start(callback);owner=new CaptureBinding1991Test.Owner(callback);token=ShotContext1932.cameraOwner19103(owner);
  image=new Image(9021,4080,3060);result=CaptureBinding1991Test.result(9021);
  ShotContext1932.acquired19103(owner,token,image,System.nanoTime(),System.nanoTime());
  ProcessingTiming1947.Trace next=start(callback);
  ShotContext1932.cameraGuard19104(owner,image,System.nanoTime(),System.nanoTime());ShotContext1932.q0Entry19104(owner,image,result);ShotContext1932.received(owner,image,result);
  check(next.cameraPhases19104[0]==0&&next.cameraPhases19104[2]==0,"callback reuse cannot borrow prior Image observation");
  ProcessingTiming1947.finish(trace,true);ProcessingTiming1947.finish(next,true);
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"post_acquisition_phases_exact_owner_verified\":true,\"phase_replay_and_ambiguity_safe\":true,\"state_wait_not_inferred_from_result_entry\":true}");
 }
}
