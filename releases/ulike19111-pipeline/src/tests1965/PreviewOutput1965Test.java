package com.hiro.ulike;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.PixelCopy;
import android.view.Surface;
import android.view.SurfaceView;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Production observation helper; only Android delivery and renderer state are doubles. */
public final class PreviewOutput1965Test {
 static int checks,ownershipCases,visibilityCases;
 static void check(boolean yes,String message){checks++;if(!yes)throw new AssertionError(message);}
 public static final class Output {
  public SurfaceView view=new SurfaceView();
  private void main(){if(Looper.myLooper()!=Looper.getMainLooper())throw new AssertionError("render View access outside MAIN");}
  public Surface getSurface(){main();return view.surface;}public SurfaceView getSurfaceView(){main();return view;}
 }
 public static final class Recorder {
  public boolean t1,a1;public Object capture=new Object();public Output render=new Output();
  public Object getCurrentCameraCapture(){return capture;}public Output getRenderView(){return render;}
 }
 static Recorder owner(){Recorder r=new Recorder();ManualLens170.values.put("capture",new WeakReference<Object>(r.capture));return r;}
 static Recorder fresh()throws Exception{
  Field field=PreviewOutput1965.class.getDeclaredField("inFlight");field.setAccessible(true);
  check(field.getInt(null)==0,"previous sample completed before fixture reset");
  Field sessions=PreviewOutput1965.class.getDeclaredField("sessions");sessions.setAccessible(true);((Map<?,?>)sessions.get(null)).clear();
  Handler.reset();SystemClock.now=0;OpticalZoom.MAIN.accept=true;OpticalZoom.MAIN.held=false;
  Build.VERSION.SDK_INT=36;ManualLens170.values.clear();ManualLens170.values.put("foreground",true);ManualLens170.values.put("epoch",1L);
  PixelCopy.reset();Bitmap.ALL.clear();Bitmap.failCreate=false;CameraTrace1965.EVENTS.clear();CameraTrace1965.fail=false;
  return owner();
 }
 static ThreadPoolExecutor pool()throws Exception{Field field=PreviewOutput1965.class.getDeclaredField("executor");field.setAccessible(true);return (ThreadPoolExecutor)field.get(null);}
 static void settle()throws Exception{
  ThreadPoolExecutor pool=pool();if(pool==null||pool.isShutdown())return;
  final CountDownLatch done=new CountDownLatch(1);long limit=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
  while(true){try{pool.execute(new Runnable(){public void run(){done.countDown();}});break;}catch(RejectedExecutionException full){if(System.nanoTime()>limit)throw new AssertionError("bounded worker did not drain");Thread.sleep(1);}}
  if(!done.await(2,TimeUnit.SECONDS))throw new AssertionError("copy worker did not return");
 }
 static void advance(long time)throws Exception{
  while(Handler.nextTime()<=time){Handler.until(Math.max(SystemClock.now,Handler.nextTime()));settle();}
  Handler.until(time);settle();Handler.until(time);
 }
 static void observe(Recorder recorder,long epoch){PreviewOutput1965.observe(recorder,recorder.render.view,recorder.render.view.surface,epoch);}
 static void recycled(){for(Bitmap bitmap:Bitmap.ALL)check(bitmap.recycled,"temporary bitmap recycled after completion");}
 static int inFlight()throws Exception{Field field=PreviewOutput1965.class.getDeclaredField("inFlight");field.setAccessible(true);return field.getInt(null);}
 static void unchanged(Recorder r){check(r.render.view.surface.releases==0&&!r.t1&&!r.a1,"observer never releases Surface or mutates recorder");}
 public static void main(String[] args)throws Exception{
  Recorder r=fresh();observe(r,1);observe(r,1);advance(199);check(PixelCopy.REQUESTS.size()==0,"first request waits 200ms");
  advance(230);check(PixelCopy.REQUESTS.size()==1&&CameraTrace1965.EVENTS.size()==1,"first owned output sample completes");
  advance(999);check(PixelCopy.REQUESTS.size()==1,"second request waits 1000ms");advance(1030);
  observe(r,1);advance(4000);check(PixelCopy.REQUESTS.size()==2&&CameraTrace1965.EVENTS.size()==2,"duplicate polls cannot renew two samples");
  for(String row:CameraTrace1965.EVENTS){check(row.contains("mean_luma_q8=32768")&&row.contains("min_luma=128")&&row.contains("max_luma=128"),"scalar luma evidence exact");check(row.contains("buffer_copy_success=1")&&row.contains("visible_preview_unverified=1"),"buffer evidence never claims screen visibility");visibilityCases++;}
  recycled();unchanged(r);
  r=fresh();r.render.view.width=Integer.MAX_VALUE;r.render.view.height=Integer.MAX_VALUE;PixelCopy.luma=255;observe(r,1);advance(2000);
  for(String row:CameraTrace1965.EVENTS){check(row.substring(row.indexOf(" n=")+1).length()<=160&&row.contains("mean_luma_q8=65280")&&row.endsWith("max_luma=255"),"even maximal dimensions preserve all scalars inside trace field limit");}
  recycled();unchanged(r);
  r=fresh();PixelCopy.luma=0;observe(r,1);advance(2000);check(CameraTrace1965.EVENTS.get(0).contains("pixelcopy_status=0")&&CameraTrace1965.EVENTS.get(0).contains("min_luma=0"),"dark copy is successful evidence, not automatic camera failure");recycled();unchanged(r);
  for(int status:new int[]{1,2,3,4,5}){r=fresh();PixelCopy.result=status;observe(r,1);advance(2000);check(CameraTrace1965.EVENTS.size()==2&&CameraTrace1965.EVENTS.get(0).contains("pixelcopy_status="+status),"platform error status preserved "+status);check(!CameraTrace1965.EVENTS.get(0).contains("mean_luma"),"failed buffer has no fabricated pixels "+status);recycled();unchanged(r);}
  r=fresh();observe(r,1);PreviewOutput1965.cancelled(r);advance(2000);check(PixelCopy.REQUESTS.size()==0,"cancel before issue drops both delayed samples");
  for(String changed:new String[]{"epoch","capture","surface","renderer","background","stopped"}){
   r=fresh();PixelCopy.automatic=false;observe(r,1);advance(200);check(PixelCopy.REQUESTS.size()==1,"one pending request before "+changed);
   if(changed.equals("epoch"))ManualLens170.values.put("epoch",2L);
   else if(changed.equals("capture")){r.capture=new Object();ManualLens170.values.put("capture",new WeakReference<Object>(r.capture));}
   else if(changed.equals("surface"))r.render.view.surface=new Surface();
   else if(changed.equals("renderer"))r.render=new Output();
   else if(changed.equals("background"))ManualLens170.values.put("foreground",false);
   else PreviewOutput1965.cancelled(r);
   PixelCopy.complete(0,0,128);advance(2000);check(CameraTrace1965.EVENTS.size()==0&&PixelCopy.REQUESTS.size()==1,"old callback/scheduled followup rejected after "+changed);ownershipCases++;recycled();unchanged(r);
  }
  r=fresh();PixelCopy.automatic=false;observe(r,1);advance(2600);
  check(PixelCopy.REQUESTS.size()==2&&inFlight()==2,"stalled native requests remain globally bounded");
  for(Bitmap bitmap:Bitmap.ALL)check(!bitmap.recycled,"deadline does not recycle native-owned destination");
  check(CameraTrace1965.EVENTS.size()==2&&CameraTrace1965.EVENTS.get(0).contains("callback_deadline"),"bounded callback deadlines recorded");
  PreviewOutput1965.cancelled(r);Recorder replacement=owner();observe(replacement,1);advance(4000);
  check(PixelCopy.REQUESTS.size()==2&&inFlight()==2,"new owner cannot exceed outstanding memory cap");
  int events=CameraTrace1965.EVENTS.size();PixelCopy.complete(0,0,128);PixelCopy.complete(1,0,128);advance(4000);
  check(CameraTrace1965.EVENTS.size()==events&&inFlight()==0,"late old callbacks only release ownership");recycled();unchanged(r);unchanged(replacement);
  r=fresh();PixelCopy.failRequest=true;observe(r,1);advance(2000);check(PixelCopy.REQUESTS.size()==0&&inFlight()==0,"synchronous request rejection releases each destination");check(CameraTrace1965.EVENTS.size()==2,"request exception is scalar-only evidence");recycled();unchanged(r);
  r=fresh();Bitmap.failCreate=true;observe(r,1);advance(2000);check(Bitmap.ALL.size()==0&&inFlight()==0,"allocation error cannot retain slot or break camera");unchanged(r);
  r=fresh();CameraTrace1965.fail=true;observe(r,1);advance(2000);check(inFlight()==0,"logger exception cannot leak destination");recycled();unchanged(r);
  r=fresh();OpticalZoom.MAIN.accept=false;observe(r,1);OpticalZoom.MAIN.accept=true;advance(2000);check(PixelCopy.REQUESTS.size()==0,"rejected MAIN scheduling leaves no sample");
  r=fresh();Build.VERSION.SDK_INT=23;observe(r,1);advance(2000);check(PixelCopy.REQUESTS.size()==0&&Handler.queued()==0,"unsupported API never requests PixelCopy");
  r=fresh();PixelCopy.automatic=false;PixelCopy.entered=new CountDownLatch(1);PixelCopy.release=new CountDownLatch(1);
  observe(r,1);Handler.until(200);check(PixelCopy.entered.await(2,TimeUnit.SECONDS),"worker reaches blocking native-copy double");
  final boolean[] uiRan={false};OpticalZoom.MAIN.post(new Runnable(){public void run(){uiRan[0]=true;}});Handler.until(1000);
  check(uiRan[0]&&pool().getQueue().size()==1&&inFlight()==2,"MAIN remains responsive while bounded worker blocks and second sample waits");
  ManualLens170.values.put("epoch",2L);PixelCopy.release.countDown();settle();advance(1000);
  check(PixelCopy.REQUESTS.size()==1&&inFlight()==1,"queued task rechecks epoch before requesting output buffer");ownershipCases++;
  int before=CameraTrace1965.EVENTS.size();PixelCopy.complete(0,0,128);advance(1000);
  check(CameraTrace1965.EVENTS.size()==before&&inFlight()==0,"blocked old native callback cannot report new epoch");recycled();unchanged(r);
  r=fresh();PixelCopy.automatic=false;PixelCopy.throwAfterSubmit=true;observe(r,1);advance(2000);
  check(PixelCopy.REQUESTS.size()==2&&inFlight()==2,"uncertain post-submission exception retains bounded native-owned slots");
  for(Bitmap bitmap:Bitmap.ALL)check(!bitmap.recycled,"post-submission failure cannot recycle native-owned bitmap");
  check(CameraTrace1965.EVENTS.size()==2&&CameraTrace1965.EVENTS.get(0).contains("request_exception_uncertain"),"uncertain native failure records scalars only");
  PixelCopy.complete(0,0,128);PixelCopy.complete(1,0,128);advance(2000);check(inFlight()==0,"late uncertain-copy callbacks release their destinations");recycled();unchanged(r);
  r=fresh();PixelCopy.automatic=false;observe(r,1);advance(200);r.capture=new Object();ManualLens170.values.put("capture",new WeakReference<Object>(r.capture));observe(r,1);advance(400);
  check(PixelCopy.REQUESTS.size()==2,"replacement capture can start a fresh session on same view/surface/epoch");
  PreviewOutput1965.cancelled(r);PixelCopy.complete(0,0,128);PixelCopy.complete(1,0,128);advance(400);
  check(CameraTrace1965.EVENTS.size()==0&&inFlight()==0,"replaced same-surface session callbacks remain gated");ownershipCases++;recycled();unchanged(r);
  for(String changed:new String[]{"capture","renderer"}){
   r=fresh();observe(r,1);
   if(changed.equals("capture")){r.capture=new Object();ManualLens170.values.put("capture",new WeakReference<Object>(r.capture));}
   else {Output replacementOutput=new Output();replacementOutput.view=r.render.view;r.render=replacementOutput;}
   advance(2000);check(PixelCopy.REQUESTS.size()==0&&CameraTrace1965.EVENTS.size()==0,"request-time owner change before first MAIN bind cannot issue probe: "+changed);
   observe(r,1);advance(4000);check(PixelCopy.REQUESTS.size()==2&&CameraTrace1965.EVENTS.size()==2,"fresh observation after queued owner replacement samples new owner: "+changed);ownershipCases++;
   recycled();unchanged(r);
  }
  for(String absent:new String[]{"capture","renderer"}){
   r=fresh();SurfaceView originalView=r.render.view;
   if(absent.equals("capture")){r.capture=null;ManualLens170.values.put("capture",new WeakReference<Object>(null));}
   else r.render=null;
   PreviewOutput1965.observe(r,originalView,originalView.surface,1);
   if(absent.equals("capture")){r.capture=new Object();ManualLens170.values.put("capture",new WeakReference<Object>(r.capture));}
   else {r.render=new Output();r.render.view=originalView;}
   advance(2000);check(PixelCopy.REQUESTS.size()==0&&CameraTrace1965.EVENTS.size()==0,"known-null request owner cannot be rebound during queued initial validation: "+absent);
   observe(r,1);advance(4000);check(PixelCopy.REQUESTS.size()==2&&CameraTrace1965.EVENTS.size()==2,"fresh observation follows known-null owner arrival: "+absent);ownershipCases++;
   recycled();unchanged(r);
  }
  r=fresh();pool().shutdown();observe(r,1);advance(2000);
  check(PixelCopy.REQUESTS.size()==0&&inFlight()==0,"worker rejection releases both pre-submission destinations");
  check(CameraTrace1965.EVENTS.size()==2,"worker rejection reports scalar failures without throwing to camera");recycled();unchanged(r);
  check(ownershipCases==12,"all executed ownership variants covered");check(visibilityCases==2,"two successful output buffers explicitly retain visibility uncertainty");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"physical_android_tested\":false,\"temporary_destination_max_bytes\":2048,\"photos_saved\":false,\"output_probe_session_ownership_tests_passed\":true,\"sampled_input_is_not_visible_success\":true,\"worker_main_thread_separation_tests_passed\":true,\"bounded_native_bitmap_lifetime_tests_passed\":true,\"trace_field_limit_preserves_scalar_evidence\":true,\"request_time_owner_pinning_tests_passed\":true,\"initial_observation_owner_pin_tests_passed\":true,\"ownership_variants_executed\":"+ownershipCases+"}");
 }
}
