package com.hiro.ulike;
import android.os.*;
import android.view.*;
import com.ss.android.vesdk.VEListener;
import java.lang.ref.WeakReference;

/** Deterministic two-queue regressions; no claim of physical rendering verification. */
public final class RenderTest1938 {
 static int checks;
 static void check(boolean value,String text){checks++;if(!value)throw new AssertionError(text);}
 public enum Facing {FACING_FRONT,FACING_BACK}
 public static class Settings {public Facing facing=Facing.FACING_FRONT;public Facing getCameraFacing(){return facing;}}
 public static class Capture {public Settings a=new Settings();}
 public static class Output {
  public SurfaceView view=new SurfaceView();public Surface surface=view.surface;
  public Surface getSurface(){return surface;}public SurfaceView getSurfaceView(){return view;}
 }
 public static class Recorder {
  public boolean t1,a1,mRenderEnvActive;
  public int mCurRecordStatus=1,replayCalls,starts;public boolean cancelBeforeNativeEntry;
  public Capture capture=new Capture();public Output render=new Output();
  public Handler executor=new Handler();
  public Capture getCurrentCameraCapture(){return capture;}public Output getRenderView(){return render;}
  public void startPreviewAsync(final Surface s,final VEListener.VECallListener callback){
   check(Looper.myLooper()==OpticalZoom.MAIN.getLooper(),"replay requested on MAIN");
   replayCalls++;if(cancelBeforeNativeEntry)RenderStartup1938.cancelled(this);RenderStartup1938.requestedAsync(this,s,callback);
   executor.post(new Runnable(){public void run(){
    if(!RenderStartup1938.admitted(callback))return;
    check(Looper.myLooper()==executor.getLooper(),"native start admitted on SDK executor");
    starts++;mCurRecordStatus=2;mRenderEnvActive=true;
    RenderStartup1938.event(Recorder.this,1000);
    if(callback!=null)callback.onDone(0);
   }});
  }
 }
 static Recorder fresh(){
  Handler.reset();SystemClock.now=0;ManualLens170.values.clear();
  ManualLens170.values.put("foreground",true);ManualLens170.values.put("recording",false);ManualLens170.values.put("epoch",1L);
  ExitBusy1921.busy=false;OpticalZoom.MAIN.accept=true;
  Recorder r=new Recorder();ManualLens170.values.put("capture",new WeakReference<>(r.capture));return r;
 }
 static void arm(Recorder r){RenderStartup1938.requested(r,r.render.surface);}
 static void all(){Handler.until(9000);}
 public static void main(String[] args){
  Recorder r=fresh();arm(r);Handler.until(1700);check(r.starts==0,"startup grace");all();check(r.starts==1&&r.replayCalls==1,"one stopped front renderer replay");
  r=fresh();r.mCurRecordStatus=0;arm(r);all();check(r.starts==1,"idle native renderer may initialize");
  r=fresh();r.mCurRecordStatus=2;r.mRenderEnvActive=true;arm(r);all();check(r.starts==0,"healthy native renderer untouched");
  r=fresh();r.mCurRecordStatus=2;arm(r);all();check(r.starts==0,"active state without GL is not blindly restarted");
  r=fresh();r.mRenderEnvActive=true;arm(r);all();check(r.starts==0,"existing GL environment is not restarted");
  r=fresh();r.capture.a.facing=Facing.FACING_BACK;arm(r);all();check(r.starts==0,"rear native renderer untouched");
  r=fresh();arm(r);Handler.until(300);ManualLens170.values.put("foreground",false);all();check(r.starts==0,"background cancels");
  r=fresh();arm(r);Handler.until(300);RenderStartup1938.cancelled(r);all();check(r.starts==0,"explicit stop cancels");
  r=fresh();arm(r);Handler.until(300);r.a1=true;all();check(r.starts==0,"destroyed surface cancels");
  r=fresh();arm(r);Handler.until(300);r.render.view.attached=false;all();check(r.starts==0,"detached surface cancels");
  r=fresh();arm(r);Handler.until(300);r.render.view.focused=false;all();check(r.starts==0,"lost focus cancels");
  r=fresh();arm(r);Handler.until(300);r.render.surface.valid=false;all();check(r.starts==0,"invalid Surface cancels");
  r=fresh();arm(r);Handler.until(300);r.render=new Output();all();check(r.starts==0,"replaced renderer identity cancels");
  r=fresh();arm(r);Handler.until(300);r.capture=new Capture();ManualLens170.values.put("capture",new WeakReference<>(r.capture));all();check(r.starts==0,"capture replacement cancels");
  r=fresh();arm(r);Handler.until(300);ManualLens170.values.put("epoch",2L);all();check(r.starts==0,"foreground generation cancels");
  r=fresh();arm(r);Handler.until(300);ExitBusy1921.busy=true;Handler.until(5000);check(r.starts==0,"still capture waits");ExitBusy1921.busy=false;all();check(r.starts==1,"busy completion within bounded deadline can replay");
  r=fresh();arm(r);ManualLens170.values.put("recording",true);all();check(r.starts==0,"video recording untouched");
  r=fresh();r.t1=true;arm(r);all();check(r.starts==0,"destroyed SDK recorder untouched");
  r=fresh();arm(r);Handler.until(1000);arm(r);Handler.until(1810);check(r.replayCalls==1,"duplicate request does not renew grace");all();check(r.replayCalls==1,"native replay does not create infinite ticket");
  r=fresh();arm(r);Handler.until(300);RenderStartup1938.event(r,1001);Handler.until(1810);check(r.starts==1,"GL-destroy event does not masquerade as first frame");
  r=fresh();arm(r);Handler.until(300);r.mCurRecordStatus=2;r.mRenderEnvActive=true;RenderStartup1938.event(r,1000);all();check(r.starts==0,"GL-created plus native active state completes without replay");
  r=fresh();r.executor.held=true;arm(r);Handler.until(2000);check(r.replayCalls==1&&r.starts==0,"native queue held after helper submission");RenderStartup1938.cancelled(r);r.executor.held=false;all();check(r.starts==0,"stop after submit wins before actual SDK start");
  r=fresh();r.executor.held=true;arm(r);Handler.until(2000);ManualLens170.values.put("foreground",false);r.executor.held=false;all();check(r.starts==0,"background after submit wins on SDK worker");
  r=fresh();r.executor.held=true;arm(r);Handler.until(2000);ExitBusy1921.busy=true;r.executor.held=false;all();check(r.starts==0,"new still capture after submit wins on SDK worker");
  r=fresh();r.executor.held=true;arm(r);Handler.until(2000);ManualLens170.values.put("epoch",2L);r.executor.held=false;all();check(r.starts==0,"epoch after submit wins on SDK worker");
  r=fresh();r.executor.held=true;arm(r);Handler.until(2000);OpticalZoom.MAIN.held=true;SystemClock.now=8100;r.executor.held=false;Handler.until(8200);OpticalZoom.MAIN.held=false;all();check(r.starts==0,"deadline enforced on SDK even MAIN watchdog blocked");
  r=fresh();r.executor.held=true;arm(r);Handler.until(2000);r.mCurRecordStatus=2;r.mRenderEnvActive=true;r.executor.held=false;all();check(r.starts==0,"native completion before helper queue executes wins");
  r=fresh();r.cancelBeforeNativeEntry=true;arm(r);all();check(r.starts==0&&r.replayCalls==1,"cancellation at native API entry cannot self-rearm fresh ticket");
  check(RenderStartup1938.admitted(null),"null ordinary SDK listener unchanged");
  check(RenderStartup1938.admitted(new VEListener.VECallListener(){public void onDone(int ignored){}}),"ordinary SDK listener unchanged");
  r=fresh();OpticalZoom.MAIN.accept=false;arm(r);OpticalZoom.MAIN.accept=true;all();check(r.starts==0,"rejected MAIN scheduling never leaves replay");
  System.out.println("PASS renderer1938 assertions="+checks);
 }
}
