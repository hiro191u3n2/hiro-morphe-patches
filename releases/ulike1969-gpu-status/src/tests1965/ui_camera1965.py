#!/usr/bin/env python3
"""Target actual helper source against session reuse and unmeasured-animation regressions."""
import argparse
import importlib.util
import json
from pathlib import Path


def module(path):
    spec=importlib.util.spec_from_file_location(path.stem,path)
    value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value


RENDER=r'''package com.hiro.ulike;
import android.os.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
public final class UiCameraRenderer1965Test {
 static int checks;
 static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static RenderStartup1938.Pending ticket(Object recorder)throws Exception{
  Field f=RenderStartup1938.class.getDeclaredField("pending");f.setAccessible(true);
  return (RenderStartup1938.Pending)((Map<?,?>)f.get(null)).get(recorder);
 }
 static RenderTest1938.Recorder fresh(){CameraTrace1965.events.clear();PreviewOutput1965.observations=0;
  PreviewOutput1965.cancellations=0;return RenderTest1938.fresh();}
 static boolean phase(String name){for(String e:CameraTrace1965.events)if(e.startsWith(name+"|"))return true;return false;}
 public static void main(String[] args)throws Exception{
  RenderTest1938.Recorder r=fresh();RenderTest1938.arm(r);Handler.until(300);
  RenderStartup1938.Pending old=ticket(r);check(old!=null&&old.bound,"initial camera lifetime bound");
  RenderTest1938.Capture capture=new RenderTest1938.Capture();r.capture=capture;
  ManualLens170.values.put("capture",new WeakReference<>(capture));ManualLens170.values.put("epoch",2L);
  RenderTest1938.arm(r);RenderStartup1938.Pending next=ticket(r);
  check(next!=null&&next!=old,"same Surface with new capture/epoch must receive a fresh ticket");
  Handler.until(600);check(ticket(r)==next&&next.bound,"new request survives old bound-owner invalidation");
  RenderTest1938.all();check(r.starts==1,"one stopped native renderer replay belongs to new session");
  check(PreviewOutput1965.lastEpoch==2L&&PreviewOutput1965.observations>0,"output probe observes current new session");
  check(phase("renderer_request_replaced")&&phase("renderer_requested")&&phase("renderer_bound"),"request/binding timeline is correlated");
  r=fresh();RenderTest1938.arm(r);Handler.until(300);old=ticket(r);long deadline=old.deadline;
  Handler.until(1000);RenderTest1938.arm(r);
  check(ticket(r)==old&&ticket(r).deadline==deadline,"duplicate same-session request cannot extend existing deadline");
  RenderStartup1938.cancelled(r);
  r=fresh();RenderTest1938.arm(r);Handler.until(300);old=ticket(r);
  ManualLens170.values.put("epoch",2L);RenderTest1938.arm(r);next=ticket(r);
  check(next!=old,"epoch change on same capture and same Surface gets fresh request");
  RenderStartup1938.cancelled(r);
  r=fresh();RenderTest1938.arm(r);Handler.until(300);old=ticket(r);
  RenderTest1938.Output output=new RenderTest1938.Output();output.surface=r.render.surface;output.view.surface=output.surface;r.render=output;
  RenderTest1938.arm(r);next=ticket(r);
  check(next!=old,"new renderer owner with same output Surface gets fresh request");
  RenderStartup1938.cancelled(r);
  r=fresh();r.mCurRecordStatus=2;r.mRenderEnvActive=true;RenderTest1938.arm(r);Handler.until(300);
  check(r.starts==0,"running native renderer is never blindly restarted");
  check(phase("renderer_native_active"),"native active is recorded explicitly");
  for(String e:CameraTrace1965.events)if(e.startsWith("renderer_native_active|"))check(e.contains("visible_confirmed=false"),"native-active flags do not establish visible-frame success");
  check(PreviewOutput1965.observations>0&&PreviewOutput1965.cancellations==0,"native-active retirement keeps independent output probe armed");
  RenderStartup1938.cancelled(r);check(PreviewOutput1965.cancellations==1,"explicit stop cancels output observation");
  check(phase("renderer_cancelled"),"stop is recorded even after restart helper retired");
  r=fresh();RenderTest1938.arm(r);old=ticket(r);
  capture=new RenderTest1938.Capture();r.capture=capture;ManualLens170.values.put("capture",new WeakReference<>(capture));
  ManualLens170.values.put("epoch",2L);Handler.until(300);
  check(!old.bound&&ticket(r)==null,"unbound old request is rejected when a later camera session becomes current before new requested");
  check(r.starts==0&&PreviewOutput1965.observations==0,"unbound old epoch cannot restart or probe the new camera");
  check(phase("renderer_stale_owner_rejected"),"unbound stale request rejection is diagnosed");
  r=fresh();RenderTest1938.arm(r);old=ticket(r);
  capture=new RenderTest1938.Capture();r.capture=capture;ManualLens170.values.put("capture",new WeakReference<>(capture));Handler.until(300);
  check(!old.bound&&ticket(r)==null&&r.starts==0&&PreviewOutput1965.observations==0,"known capture replacement alone cannot be adopted by unbound request");
  r=fresh();RenderTest1938.arm(r);old=ticket(r);
  output=new RenderTest1938.Output();output.surface=r.render.surface;output.view.surface=output.surface;r.render=output;Handler.until(300);
  check(!old.bound&&ticket(r)==null&&r.starts==0&&PreviewOutput1965.observations==0,"known renderer replacement alone cannot be adopted by unbound request");
  r=fresh();capture=r.capture;r.capture=null;RenderTest1938.arm(r);old=ticket(r);
  r.capture=capture;Handler.until(300);
  check(old.bound&&PreviewOutput1965.observations>0,"null capture at request may bind later within the same epoch");
  RenderStartup1938.cancelled(r);
  r=fresh();output=r.render;r.render=null;RenderStartup1938.requested(r,output.surface);old=ticket(r);
  r.render=output;Handler.until(300);
  check(old.bound&&PreviewOutput1965.observations>0,"null renderer at request may bind later within the same epoch");
  RenderStartup1938.cancelled(r);
  r=fresh();capture=r.capture;r.capture=null;RenderTest1938.arm(r);old=ticket(r);
  r.capture=capture;ManualLens170.values.put("epoch",2L);Handler.until(300);
  check(!old.bound&&ticket(r)==null&&PreviewOutput1965.observations==0,"null request owner never permits late binding across epochs");
  r=fresh();RenderTest1938.arm(r);old=ticket(r);old.requestCapture.clear();Handler.until(300);
  check(!old.bound&&ticket(r)==null&&PreviewOutput1965.observations==0,"cleared known weak owner cannot be mistaken for originally null late-binding request");
  r=fresh();r.capture.a.facing=RenderTest1938.Facing.FACING_BACK;RenderTest1938.arm(r);Handler.until(300);
  check(r.starts==0&&r.replayCalls==0,"rear observation never opens or restarts the native renderer");
  check(PreviewOutput1965.observations>0&&phase("renderer_rear_observed"),"rear current valid Surface arms the same independent output probe");
  check(!phase("output_wait_timeout")&&ticket(r)==null,"valid rear observation retires front-only helper without a false timeout");
  r=fresh();r.capture.a.facing=RenderTest1938.Facing.FACING_BACK;r.render.view.width=0;RenderTest1938.arm(r);Handler.until(300);
  check(PreviewOutput1965.observations==0,"rear probe waits for real measured output ownership");
  r.render.view.width=1080;Handler.until(600);
  check(PreviewOutput1965.observations>0&&r.starts==0,"delayed rear output measurement arms probe with no camera retry");
  r=fresh();r.mCurRecordStatus=2;r.mRenderEnvActive=false;RenderTest1938.arm(r);RenderTest1938.all();
  check(r.starts==0&&phase("output_wait_timeout"),"ambiguous native state is diagnosed within bound, never blindly reset");
  System.out.println("UI_CAMERA_RENDERER1965_ASSERTIONS="+checks);
 }
}'''

LAYOUT=r'''package com.hiro.ulike;
import android.animation.ValueAnimator;
import android.os.SystemClock;
import com.bytedance.corecamera.ui.view.CameraShadeView;
public final class UiCameraLayout1965Test {
 static int checks;
 static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static CameraShadeView unmeasured(){SystemClock.now=0;CameraTrace1965.events.clear();
  CameraShadeView v=new CameraShadeView();v.c=1080;v.j=2340;v.u=v.r=100;v.t=v.s=800;v.i();
  v.measuredWidth=0;v.measuredHeight=0;PreviewLayout1922.attached(v);return v;}
 static ValueAnimator animator(final CameraShadeView v){final ValueAnimator a=new ValueAnimator();v.w=a;
  a.ending=()->{if(PreviewLayout1922.owns(v,a)){v.r=v.u;v.s=v.t;v.i();}
   if(PreviewLayout1922.finish(v,a))v.b();};return a;}
 static boolean phase(String name){for(String e:CameraTrace1965.events)if(e.startsWith(name+"|"))return true;return false;}
 public static void main(String[] args){
  CameraShadeView v=unmeasured();ValueAnimator a=animator(v);int before=v.notifications;
  PreviewLayout1922.ready(v);PreviewLayout1922.schedule(v);PreviewLayout1922.startCurrent(v);v.advance(1500);
  check(a.starts==0&&a.ends==0,"unmeasured shade cannot start or complete native animation with cached screen dimensions");
  check(v.w==a&&!a.cancelled,"native animation owner retained for first real measurement");
  check(v.notifications==before&&PreviewLayout1922.viewport()==null,"unmeasured shade cannot notify guessed geometry");
  check(v.queue.isEmpty(),"unmeasured animation does not spawn polling/watchdog work");
  check(!PreviewLayout1922.finish(v,a)&&v.w==a,"native end callback cannot deliver guessed geometry before measurement");
  check(phase("shade_first_frame_notice")&&phase("layout_wait_measurement"),"first-frame notice and missing measurement remain separate diagnostic events");
  v.measuredWidth=1080;v.measuredHeight=2340;PreviewLayout1922.afterLayout(v);
  check(v.w==a&&v.queue.size()==2,"first measured layout arms native fallback and settlement exactly once");
  v.advance(2000);check(a.starts==1,"retained animator starts after valid measurement");
  v.advance(2700);check(a.ends==1&&v.w==null,"measured animation still has one bounded native completion");
  check(v.notifications==before+1,"current measured camera clients notified once");
  check(phase("layout_geometry")&&phase("layout_settled"),"current measured geometry and settlement are traced");
  PreviewLayout1922.detached(v);check(phase("layout_detached"),"view teardown is recorded");
  v=unmeasured();a=animator(v);v.measuredWidth=1080;v.measuredHeight=2340;PreviewLayout1922.afterLayout(v);
  v.measuredWidth=0;v.measuredHeight=0;before=v.notifications;v.advance(1200);
  check(a.starts==0&&a.ends==0&&v.w==a,"lost measurement defers both queued start and settlement watchdog");
  check(v.notifications==before&&v.queue.isEmpty(),"no stale completion notification while container is unmeasured");
  v.measuredWidth=1080;v.measuredHeight=2340;PreviewLayout1922.afterLayout(v);v.advance(2400);
  check(a.ends==1&&v.w==null,"remeasure restores bounded owned completion");
  PreviewLayout1922.detached(v);
  System.out.println("UI_CAMERA_LAYOUT1965_ASSERTIONS="+checks);
 }
}'''


def test(root,work,jdk=None,ndk=None):
    root,work=Path(root).resolve(),Path(work).resolve()
    runner=module(root/'tests1963/ui_audit1963.py')
    counts={}
    counts['renderer']=runner.compile_run(root,work,'camera-renderer1965',
        ['RenderStartup1938.java','Scheduling1944.java','SpeedWorkers1935.java'],
        ['renderer1938-host'],{'com/hiro/ulike/UiCameraRenderer1965Test.java':RENDER},
        'com.hiro.ulike.UiCameraRenderer1965Test',jdk)
    counts['layout']=runner.compile_run(root,work,'camera-layout1965',
        ['PreviewLayout1922.java'],['layout1937-host'],
        {'com/hiro/ulike/UiCameraLayout1965Test.java':LAYOUT},
        'com.hiro.ulike.UiCameraLayout1965Test',jdk)
    result={'status':'passed','assertions':sum(counts.values()),'groups':counts,
        'scope':'actual production helper bodies; Android/SDK queue doubles; output-hook spy',
        'physical_android_tested':False}
    (work/'ui-camera1965.json').write_text(json.dumps(result,indent=2)+'\n')
    return result


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[1])
    parser.add_argument('--work',type=Path,required=True);parser.add_argument('--jdk',type=Path)
    args=parser.parse_args();print(json.dumps(test(args.root,args.work,args.jdk),indent=2))
