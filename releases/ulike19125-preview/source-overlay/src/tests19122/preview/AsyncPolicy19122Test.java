package com.hiro.ulike;
import android.graphics.Bitmap;import android.os.Handler;import java.lang.ref.WeakReference;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
import com.bytedance.corecamera.camera.basic.PureCameraFragment;
/** Real save lifecycle; the encoder and SDK boundaries are scripted. */
public final class AsyncPolicy19122Test {
 static int assertions;
 static synchronized void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
 static void until(java.util.function.BooleanSupplier test,String why)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(!test.getAsBoolean()&&System.nanoTime()<end)Thread.yield();check(test.getAsBoolean(),why);}
 static final class Scene {
  final Bitmap source=new Bitmap(new int[]{71,72,73,74});final int[] pristine=source.pixels.clone();final PureCameraFragment fragment=new PureCameraFragment();final i.o.a.b1.a.b.c.a callback=new i.o.a.b1.a.b.c.a();final i.o.a.b1.a.b.f.a manager;
  Scene(){
   OpticalZoom.foreground=true;OpticalZoom.recording=false;OpticalZoom.switching=false;OpticalZoom.epoch++;
   OpticalZoom.active=new Object();OpticalZoom.capture=new WeakReference<Object>(new Object());
   CaptureYuv.Owner owner=new CaptureYuv.Owner();CaptureYuv.State state=new CaptureYuv.State();state.original=owner.e0;
   CaptureYuv.active=new WeakReference<Object>(owner);CaptureYuv.states.clear();CaptureYuv.states.put(owner,state);
   fragment.camera.recorder.capture=OpticalZoom.capture.get();fragment.camera.supplier.b=fragment.camera.recorder;fragment.camera.supplier.h.a=fragment.camera.recorder;
   PreviewFixtures19121.Controller controller=new PreviewFixtures19121.Controller();controller.a=fragment;
   i.o.a.b1.a.b.c bridge=new i.o.a.b1.a.b.c();bridge.c=controller;callback.a=bridge;manager=new i.o.a.b1.a.b.f.a(callback);
   i.f.l.n.s.a.b().image=source;i.f.l.n.s.a.b().b=source;ShotContext1932.bind(source,19122);
  }
 }
 static void scenario(int policy,boolean owned,String fault)throws Exception{
  Scene scene=new Scene();Bitmap.failCopy=!owned;
  int starts=PostSaveCleanup19122.starts,ends=PostSaveCleanup19122.ends;
  int queued=Responsiveness19122.queued,finished=Responsiveness19122.finished;
  int observed=Responsiveness19122.requests;Responsiveness19122.observerFail=fault.equals("observer-throw");
  check(CapturePolicy19122.arm19122(policy),"arm mode "+policy);check(!CapturePolicy19122.bypassProcessing(),"arming never changes idle quality");
  CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicReference<Bitmap> encoded=new AtomicReference<Bitmap>();AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
  HostRouter1981.encoder=(save,input,r,d)->{
   try{
    encoded.set(input);check(owned?input!=scene.source:input==scene.source,"exact owned/borrowed routing");
    check(!input.isRecycled()&&Arrays.equals(input.pixels,scene.pristine),"encoder sees exact source values");
    check(CapturePolicy19122.diagnostic19122()==(policy!=0),"pipeline policy remains active");
    check(CapturePolicy19122.bypassProcessing()==(policy==6),"only chosen diagnostic bypass active");
    if(policy==6)check(AsyncSave1935.photoContext1981(input)==null,"bypass does not allocate speculative analysis context");
    entered.countDown();release.await();
    if(fault.equals("encode-fail"))throw new IllegalStateException("scripted encoding failure");
    AsyncSave1935.encoding(input);AsyncSave1935.hardwareClosed1956();return "saved";
   }catch(Throwable t){if(!(t instanceof IllegalStateException))failure.set(t);throw new RuntimeException(t);}
  };
  Handler.rejectPreview=fault.equals("preview-post-fail");scene.fragment.camera.supplier.fail=fault.equals("sdk-throw");
  AsyncSave1935.submitAuto(scene.manager,0,0);check(entered.await(5,TimeUnit.SECONDS),"save entered "+policy+"/"+owned+"/"+fault);
  ProcessingTiming1947.Trace trace=ProcessingTiming1947.forKey(encoded.get());
  until(()->Handler.previewCount()>0||trace.previewResult!=null,"preview decision made");
  check(AsyncSave1935.captureBlocked()&&SaveQueue1935.count()==1&&ExitJobs185.count()==1,"diagnostic/borrowed save owns one shutter and reservation");
  check(scene.callback.finished==0,"diagnostic owned bitmap also holds next shutter until terminal");
  check(!CapturePolicy19122.arm19122(2),"mid-save comparison cannot change policy");
  check(PostSaveCleanup19122.starts==starts+1&&PostSaveCleanup19122.ends==ends,"cleanup lifecycle starts once before save");
  check(PostSaveCleanup19122.enabledAtStart==(policy!=1&&policy!=5),"cleanup flag snapshotted before reset");
  if(fault.equals("epoch-change"))OpticalZoom.epoch++;
  if(fault.equals("background"))OpticalZoom.foreground=false;
  if(fault.equals("source-replaced"))i.f.l.n.s.a.b().b=new Bitmap(new int[]{1,2,3,4});
  if(!fault.equals("finish-before-ui"))Handler.drainPreview();
  check(scene.callback.finished==0&&AsyncSave1935.captureBlocked(),"preview request never unlocks shutter");
  check(!scene.source.isRecycled()&&Arrays.equals(scene.pristine,scene.source.pixels),"preview source remains exact");
  Handler.rejectAll=fault.equals("terminal-post-fail");Handler.holdMain=fault.equals("terminal-pending");release.countDown();
  if(fault.equals("terminal-pending")){
   until(()->SaveQueue1935.count()==0&&SaveQueue1935.idle1953()&&Handler.heldCount()==1,"save workers release queue while terminal UI remains pending");
   check(CapturePolicy19122.diagnostic19122()&&ExitJobs185.count()==1&&PostSaveCleanup19122.ends==ends,"terminal remains owner of diagnostic policy and exit guard");
   check(AsyncSave1935.captureBlocked(),"empty worker queue cannot admit another diagnostic before terminal UI");
   check(!CapturePolicy19122.arm19122(0)&&scene.callback.receipts==0&&scene.callback.finished==0,"pending terminal preserves one-shot comparison");
   Handler.drainMain();
  }
  until(()->SaveQueue1935.count()==0&&SaveQueue1935.idle1953()&&ExitJobs185.count()==0&&PostSaveCleanup19122.ends==ends+1,"terminal resource ownership drained");
  Handler.rejectAll=false;Handler.drainPreview();
  boolean postFail=fault.equals("terminal-post-fail");
  check(scene.callback.receipts==(postFail?0:1)&&scene.callback.finished==(postFail?0:1),"exactly one receipt and capture finish unless UI rejects post");
  check(postFail||scene.callback.saved!=fault.equals("encode-fail"),"terminal success/failure belongs to exact photo");
  check(Responsiveness19122.queued==queued+1&&Responsiveness19122.finished==finished+(postFail?0:1),"terminal instrumentation follows actual posted callback");
  check(PostSaveCleanup19122.active.isEmpty()&&!CapturePolicy19122.diagnostic19122(),"policy and cleanup record reset after terminal");
  check(!AsyncSave1935.captureBlocked()&&!scene.source.isRecycled()&&scene.source.recycleCalls==0&&Arrays.equals(scene.pristine,scene.source.pixels),"SDK source survives terminal unchanged");
  check(!owned||encoded.get().isRecycled()&&encoded.get().recycleCalls==1,"owned save copy disposed exactly once");
  int expected=policy!=4&&!owned&&(fault.equals("success")||fault.equals("sdk-throw")||fault.equals("observer-throw")||fault.equals("terminal-pending")||fault.equals("encode-fail")||fault.equals("terminal-post-fail"))?1:0;
  check(scene.fragment.camera.supplier.calls==expected,"display-only call matches explicit mode/lifecycle: "+policy+"/"+owned+"/"+fault+" actual="+scene.fragment.camera.supplier.calls);
  int observedExpected=expected==1&&!fault.equals("sdk-throw")?1:0;
  check(Responsiveness19122.requests-observed==observedExpected,"observation occurs only after successful release API, including normal mode and skipped comparison");
  if(observedExpected==1)check(Responsiveness19122.previewTrace==trace&&Responsiveness19122.previewInput>0&&Responsiveness19122.previewReturned>=Responsiveness19122.previewInput,"release observation owns exact save trace and ordered timestamps");
  Responsiveness19122.observerFail=false;
  check(failure.get()==null,"encoder assertion failures propagate: "+failure.get());
 }
 static void preparationFailure(boolean postFail)throws Exception{
  Scene scene=new Scene();int starts=PostSaveCleanup19122.starts,ends=PostSaveCleanup19122.ends;
  check(CapturePolicy19122.arm19122(6),"arm failed prep diagnostic");SaveHarness1935.failSnapshot=true;Handler.rejectAll=postFail;
  AsyncSave1935.submitAuto(scene.manager,0,0);
  until(()->SaveQueue1935.count()==0&&ExitJobs185.count()==0&&PostSaveCleanup19122.ends==ends+1,"preparation failure drains all guards");
  Handler.rejectAll=false;check(PostSaveCleanup19122.starts==starts+1&&!CapturePolicy19122.diagnostic19122(),"failed snapshot consumes one comparison then resets");
  check(scene.callback.finished==(postFail?0:1),"failed prep terminal callback once if post accepted");
  check(!scene.source.isRecycled(),"failed prep retains SDK image");
 }
 public static void main(String[] args)throws Exception{
  SaveQuality2.systemInfo=true;PhotoDetail.live=new PhotoDetail.Settings(false,1,false,1,true,true,true);
  for(int mode=0;mode<=6;mode++)scenario(mode,false,"success");
  for(int mode=1;mode<=6;mode++)scenario(mode,true,"success");
  for(String fault:new String[]{"sdk-throw","observer-throw","preview-post-fail","epoch-change","background","source-replaced","finish-before-ui","encode-fail","terminal-post-fail"})scenario(0,false,fault);
  scenario(6,true,"encode-fail");scenario(6,true,"terminal-post-fail");scenario(6,true,"terminal-pending");scenario(6,false,"terminal-pending");preparationFailure(false);preparationFailure(true);
  check(CapturePolicy19122.arm19122(0),"default selected after every fault");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_async_adapter\":true,\"single_shot_comparison_owned_and_borrowed\":true,\"terminal_failure_cleanup\":true,\"normal_quality_source_copies_exact\":true}");
 }
}
