#!/usr/bin/env python3
"""Exercise production renderer supervision with independent MAIN/SDK queues.

The doubles model native state and ownership, not physical camera/PixelCopy output.
"""
import argparse
import importlib.util
import json
from pathlib import Path


TEST = r'''package com.hiro.ulike;
import android.os.*;
import android.view.*;
import com.ss.android.vesdk.VEListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
public final class Renderer1968Test {
 static int checks;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 public enum Facing {FACING_FRONT,FACING_BACK,UNKNOWN}
 public static class Settings {public Facing facing=Facing.FACING_FRONT;public Facing getCameraFacing(){return facing;}}
 public static class Capture {public Settings a=new Settings();}
 public static class Output {
  public SurfaceView view=new SurfaceView();public Surface surface=view.surface;
  public Surface getSurface(){return surface;}public SurfaceView getSurfaceView(){return view;}
 }
 public static class Recorder {
  public boolean t1,a1,mRenderEnvActive,failNative,cancelAtEntry;
  public int mCurRecordStatus=1,replayCalls,starts;
  public Capture capture=new Capture();public Output render=new Output();public Handler executor=new Handler();
  public Capture getCurrentCameraCapture(){return capture;}public Output getRenderView(){return render;}
  public void startPreviewAsync(final Surface surface,final VEListener.VECallListener callback){
   check(Looper.myLooper()==OpticalZoom.MAIN.getLooper(),"replay is submitted on MAIN");
   replayCalls++;
   if(cancelAtEntry)RenderStartup1938.cancelled(this);
   RenderStartup1938.requestedAsync(this,surface,callback);
   executor.post(new Runnable(){public void run(){
    if(!RenderStartup1938.admitted(callback))return;
    check(Looper.myLooper()==executor.getLooper(),"replay admission runs on SDK executor");
    starts++;
    if(!failNative){mCurRecordStatus=2;mRenderEnvActive=true;RenderStartup1938.event(Recorder.this,1000);}
    callback.onDone(failNative?-1:0);
   }});
  }
 }
 static Recorder fresh(Facing facing){
  Handler.reset();SystemClock.now=0;ManualLens170.values.clear();
  ManualLens170.values.put("foreground",true);ManualLens170.values.put("recording",false);ManualLens170.values.put("epoch",1L);
  ExitBusy1921.busy=false;OpticalZoom.MAIN.accept=true;OpticalZoom.MAIN.held=false;
  CameraTrace1965.events.clear();PreviewOutput1965.observations=PreviewOutput1965.cancellations=0;
  Recorder recorder=new Recorder();recorder.capture.a.facing=facing;
  ManualLens170.values.put("capture",new WeakReference<>(recorder.capture));return recorder;
 }
 static void arm(Recorder recorder){RenderStartup1938.requested(recorder,recorder.render.surface);}
 static void epoch(long value){ManualLens170.values.put("epoch",value);}
 static void all(){Handler.until(9000);}
 static RenderStartup1938.Pending ticket(Recorder recorder)throws Exception {
  Field field=RenderStartup1938.class.getDeclaredField("pending");field.setAccessible(true);
  return (RenderStartup1938.Pending)((Map<?,?>)field.get(null)).get(recorder);
 }
 static boolean phase(String name){for(String event:CameraTrace1965.events)if(event.startsWith(name+"|"))return true;return false;}
 static void rollover()throws Exception {
  // Exact incident order: output request, 38ms later internal camera close,
  // same live output resumes before the first 150ms observer poll.
  Recorder r=fresh(Facing.FACING_BACK);r.mCurRecordStatus=2;r.mRenderEnvActive=true;
  arm(r);RenderStartup1938.Pending old=ticket(r);Handler.until(38);epoch(2);Handler.until(300);
  check(phase("renderer_epoch_rebound"),"camera close between request and first poll rebinds observer");
  check(PreviewOutput1965.lastEpoch==2&&PreviewOutput1965.observations>0,"current retained output gets epoch-2 observation");
  check(!old.bound&&r.starts==0,"retired epoch-1 observer does not start active renderer");
  check(phase("renderer_native_active")&&!phase("renderer_stale_owner_rejected"),"rebind is distinct from stale ownership rejection");
  r=fresh(Facing.FACING_FRONT);arm(r);old=ticket(r);Handler.until(38);epoch(2);Handler.until(300);
  RenderStartup1938.Pending next=ticket(r);
  check(next!=old&&next.bound&&next.epoch==2,"same-owner rollover creates a new pinned ticket");
  check(next.started==old.started&&next.deadline==old.deadline,"handoff retains original grace and deadline");
  all();check(r.starts==1&&r.replayCalls==1,"rebound stopped renderer gets one native replay");
  r=fresh(Facing.FACING_BACK);arm(r);Handler.until(300);old=ticket(r);epoch(2);Handler.until(600);
  next=ticket(r);check(next!=old&&next.bound&&next.deadline==old.deadline,"bound same output can follow internal reopen");
  r=fresh(Facing.FACING_BACK);r.mCurRecordStatus=2;arm(r);old=ticket(r);
  for(int i=1;i<=35;i++){Handler.until(i*200);epoch(i+1);arm(r);}
  check(ticket(r).deadline==old.deadline&&ticket(r).started==old.started,"native same-output repeated requests cannot renew deadline");
  all();check(r.starts==0&&ticket(r)==null&&phase("output_wait_timeout"),"epoch churn remains bounded for ambiguous native state");
  r=fresh(Facing.FACING_FRONT);arm(r);Handler.until(300);old=ticket(r);Handler.until(1000);arm(r);
  check(ticket(r)==old,"same-session duplicate does not replace or renew watcher");
  RenderStartup1938.cancelled(r);
 }
 static void stoppedStates()throws Exception {
  for(Facing facing:new Facing[]{Facing.FACING_FRONT,Facing.FACING_BACK}) {
   for(int state:new int[]{0,1}) {
    Recorder r=fresh(facing);r.mCurRecordStatus=state;arm(r);Handler.until(1700);
    check(r.starts==0,"stopped renderer observes startup grace");all();
    check(r.starts==1&&r.replayCalls==1,"both facings replay only stopped state 0/1 once");
    check(phase("renderer_stopped_replay_requested"),"replay is explicitly diagnosed");
   }
   Recorder r=fresh(facing);r.failNative=true;arm(r);all();
   check(r.starts==1&&r.replayCalls==1&&ticket(r)==null,"native replay failure never loops and deadline retires watcher");
   check(phase("output_wait_timeout"),"failed replay has bounded diagnostic timeout");
   r=fresh(facing);r.mCurRecordStatus=2;r.mRenderEnvActive=true;arm(r);all();
   check(r.starts==0&&PreviewOutput1965.observations>0,"active renderer is observed without restart");
   for(String event:CameraTrace1965.events)if(event.startsWith("renderer_native_active|"))
    check(event.contains("visible_confirmed=false"),"native active is never proof of a visible preview");
   r=fresh(facing);r.mCurRecordStatus=2;arm(r);all();
   check(r.starts==0&&phase("output_wait_timeout"),"active-state missing GL is not blindly reset");
   r=fresh(facing);r.mRenderEnvActive=true;arm(r);all();
   check(r.starts==0,"live GL in stopped native state is untouched");
  }
 }
 static void exclusions()throws Exception {
  Recorder r=fresh(Facing.FACING_BACK);arm(r);Handler.until(38);epoch(2);r.capture=new Capture();
  ManualLens170.values.put("capture",new WeakReference<>(r.capture));all();
  check(r.starts==0&&PreviewOutput1965.observations==0&&ticket(r)==null,"different capture cannot be adopted on rollover");
  check(phase("renderer_stale_owner_rejected"),"replacement diagnosis is observer-only");
  for(String event:CameraTrace1965.events)if(event.startsWith("renderer_stale_owner_rejected|"))
   check(event.contains("observer_only=true")&&event.contains("native_start_cancelled=false"),"stale observer event does not claim native request cancellation");
  r=fresh(Facing.FACING_FRONT);arm(r);Handler.until(38);epoch(2);Output replacement=new Output();
  replacement.surface=r.render.surface;replacement.view.surface=replacement.surface;r.render=replacement;all();
  check(r.starts==0&&PreviewOutput1965.observations==0,"different renderer owner cannot inherit same Surface observer");
  r=fresh(Facing.FACING_FRONT);arm(r);Handler.until(300);epoch(2);r.render.view=new SurfaceView();r.render.view.surface=r.render.surface;all();
  check(r.starts==0,"bound view replacement is never adopted despite retained Surface");
  r=fresh(Facing.FACING_BACK);arm(r);Handler.until(38);epoch(2);r.render.surface=new Surface();r.render.view.surface=r.render.surface;all();
  check(r.starts==0&&PreviewOutput1965.observations==0,"different output Surface cannot inherit request");
  r=fresh(Facing.FACING_FRONT);arm(r);RenderStartup1938.Pending old=ticket(r);old.requestCapture.clear();epoch(2);all();
  check(r.starts==0&&PreviewOutput1965.observations==0,"collected known owner is not treated as originally null");
  r=fresh(Facing.FACING_FRONT);Capture c=r.capture;r.capture=null;arm(r);r.capture=c;epoch(2);all();
  check(r.starts==0&&PreviewOutput1965.observations==0,"unknown request capture cannot bind across epochs");
  r=fresh(Facing.FACING_FRONT);c=r.capture;r.capture=null;arm(r);r.capture=c;Handler.until(300);
  check(ticket(r).bound,"unknown request capture can bind within its own epoch");RenderStartup1938.cancelled(r);
  for(int change=0;change<7;change++){
   r=fresh(Facing.FACING_BACK);arm(r);Handler.until(300);
   switch(change){case 0:RenderStartup1938.cancelled(r);break;case 1:ManualLens170.values.put("foreground",false);break;
    case 2:r.a1=true;break;case 3:r.t1=true;break;case 4:r.render.view.attached=false;break;
    case 5:r.render.view.focused=false;break;case 6:r.render.surface.valid=false;break;}
   epoch(2);all();check(r.starts==0,"stop/background/teardown/detach/focus/invalid Surface prevent rollover replay");
   check(ticket(r)==null,"invalidated observer is retired");
  }
  r=fresh(Facing.FACING_BACK);r.render.view.width=0;arm(r);Handler.until(600);
  check(r.starts==0&&PreviewOutput1965.observations==0,"unmeasured output is not replayed");r.render.view.width=1080;all();
  check(r.starts==1,"same measured output can become ready within original bound");
  r=fresh(Facing.UNKNOWN);arm(r);all();check(r.starts==0,"unknown facing retains native behavior");
  r=fresh(Facing.FACING_BACK);arm(r);ManualLens170.values.put("recording",true);all();
  check(r.starts==0,"recording prevents optional output recovery");
  r=fresh(Facing.FACING_BACK);ExitBusy1921.busy=true;arm(r);Handler.until(5000);
  check(r.starts==0,"still capture busy defers replay");ExitBusy1921.busy=false;all();
  check(r.starts==1,"busy can finish safely within fixed deadline");
  r=fresh(Facing.FACING_BACK);ExitBusy1921.busy=true;arm(r);all();ExitBusy1921.busy=false;all();
  check(r.starts==0&&Handler.queued()==0,"busy deadline cannot be renewed");
 }
 static void queuedRaces()throws Exception {
  for(int change=0;change<9;change++){
   Recorder r=fresh(Facing.FACING_BACK);r.executor.held=true;arm(r);Handler.until(2000);
   check(r.replayCalls==1&&r.starts==0,"SDK queue is held after one submission");
   switch(change){case 0:RenderStartup1938.cancelled(r);break;
    case 1:ManualLens170.values.put("foreground",false);break;case 2:ExitBusy1921.busy=true;break;
    case 3:ManualLens170.values.put("recording",true);break;case 4:epoch(2);break;
    case 5:r.capture=new Capture();ManualLens170.values.put("capture",new WeakReference<>(r.capture));break;
    case 6:r.render=new Output();break;case 7:r.mCurRecordStatus=2;r.mRenderEnvActive=true;break;
    case 8:r.render.view.attached=false;RenderStartup1938.cancelled(r);break;}
   r.executor.held=false;all();check(r.starts==0,"SDK entry rejects stale stop/background/busy/recording/epoch/owner/active/detach replay");
  }
  Recorder r=fresh(Facing.FACING_BACK);r.executor.held=true;arm(r);Handler.until(2000);
  RenderStartup1938.Pending old=ticket(r);RenderStartup1938.Replay completion=new RenderStartup1938.Replay(old);
  epoch(2);Handler.until(2300);RenderStartup1938.Pending next=ticket(r);
  check(next!=null&&next!=old&&next.replayed,"handoff retains consumed replay budget");
  completion.onDone(0);old.run();check(ticket(r)==next,"late old completion and poll cannot replace/drop current ticket");
  r.executor.held=false;all();check(r.starts==0&&r.replayCalls==1,"old queued replay cannot be revived by same-output handoff");
  r=fresh(Facing.FACING_BACK);r.executor.held=true;arm(r);Handler.until(2000);OpticalZoom.MAIN.held=true;
  SystemClock.now=8100;r.executor.held=false;Handler.until(8200);OpticalZoom.MAIN.held=false;all();
  check(r.starts==0,"native executor enforces deadline even when MAIN watchdog is blocked");
  r=fresh(Facing.FACING_BACK);r.cancelAtEntry=true;arm(r);all();
  check(r.starts==0&&r.replayCalls==1&&ticket(r)==null,"stop reentrancy at native API entry cannot self-rearm");
  r=fresh(Facing.FACING_BACK);arm(r);Handler.until(300);old=ticket(r);RenderStartup1938.cancelled(r);epoch(2);
  old.run();new RenderStartup1938.Replay(old).onDone(0);RenderStartup1938.event(r,1000);all();
  check(r.starts==0&&ticket(r)==null&&Handler.queued()==0,"cancelled callbacks and native events cannot create observer tickets");
  check(RenderStartup1938.admitted(null),"ordinary null SDK callback is preserved");
  check(RenderStartup1938.admitted(new VEListener.VECallListener(){public void onDone(int result){}}),"ordinary SDK callback is preserved");
  r=fresh(Facing.FACING_BACK);OpticalZoom.MAIN.accept=false;arm(r);OpticalZoom.MAIN.accept=true;all();
  check(r.starts==0&&ticket(r)==null,"scheduler rejection leaves no surviving optional retry");
 }
 public static void main(String[] args)throws Exception{
  rollover();stoppedStates();exclusions();queuedRaces();
  System.out.println("RENDERER1968_ASSERTIONS="+checks);
 }
}'''


def test(root, work, jdk=None):
    root, work = Path(root).resolve(), Path(work).resolve()
    path = root / 'tests1963/ui_audit1963.py'
    spec = importlib.util.spec_from_file_location('renderer1968_compile', path)
    runner = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(runner)
    count = runner.compile_run(root, work, 'renderer1968',
        ['RenderStartup1938.java', 'Scheduling1944.java', 'SpeedWorkers1935.java'],
        ['renderer1938-host'], {'com/hiro/ulike/Renderer1968Test.java': TEST},
        'com.hiro.ulike.Renderer1968Test', jdk)
    report = {'status': 'passed', 'assertions': count,
        'groups': {'renderer_ownership_and_native_queue': {'status': 'passed', 'assertions': count}},
        'scope': 'actual production helper body; deterministic MAIN/SDK queues; output observation spy',
        'physical_android_tested': False}
    (work / 'host-renderer1968.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    args = parser.parse_args()
    print(json.dumps(test(args.root, args.work, args.jdk), indent=2))
