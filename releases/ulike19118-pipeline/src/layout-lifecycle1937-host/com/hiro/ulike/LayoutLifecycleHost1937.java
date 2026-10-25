package com.hiro.ulike;
import android.os.Handler;
import android.animation.ValueAnimator;
import com.bytedance.corecamera.ui.view.CameraShadeView;
import com.ss.android.vesdk.VEPreviewRadio;
public class LayoutLifecycleHost1937 {
 static int checks;
 static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 static CameraShadeView view(){CameraShadeView v=new CameraShadeView();PreviewLayout1922.attached(v);return v;}
 static void detach(CameraShadeView v){PreviewLayout1922.detached(v);v.attached=false;}
 static i.o.a.b1.a.d.b camera(CameraShadeView v,int grid){i.o.a.b1.a.d.b c=new i.o.a.b1.a.d.b();c.b=v;c.c=grid;return c;}
 static i.o.a.m.j.f preview(CameraShadeView v,int ratio){i.o.a.m.j.f c=new i.o.a.m.j.f();c.a=v;c.d=ratio;return c;}
 public static void main(String[] args){
  CameraShadeView v=view();Handler h=new Handler();i.o.a.b1.a.d.b c=camera(v,0);i.o.a.m.j.f f=preview(v,1);
  check(LayoutLifecycle1937.camera(c,1,0),"current 3:4 callback accepted");check(LayoutLifecycle1937.preview(f,1),"legacy current ratio accepted");
  LayoutLifecycle1937.postCamera(h,new i.o.a.b1.a.d.a(c,1,0),200);check(h.lastDelay==200,"native 200ms preserved");h.drain();check(c.executions==1,"current native callback executes exactly once");
  LayoutLifecycle1937.postPreview(h,new i.o.a.m.j.b(f,1),200);h.drain();check(f.executions==1,"current legacy callback executes once");
  i.f.l.v.b.b end=new i.f.l.v.b.b(v);LayoutLifecycle1937.postSettled(h,end,50);check(h.lastDelay==50,"native 50ms preserved");h.drain();check(end.executions==1,"settled current listener receives completion");
  LayoutLifecycle1937.postCamera(h,new i.o.a.b1.a.d.a(c,1,0),200);LayoutLifecycle1937.postPreview(h,new i.o.a.m.j.b(f,1),200);LayoutLifecycle1937.postSettled(h,end,50);
  PreviewLayout1922.prepare(v);PreviewLayout1922.ready(v);h.drain();check(c.executions==2&&f.executions==2&&end.executions==1,"geometry-only generation preserves ratio/focus delivery but rejects earlier end notification");
  check(LayoutLifecycle1937.camera(c,1,0),"generation does not block newly requested valid work");
  LayoutLifecycle1937.postCamera(h,new i.o.a.b1.a.d.a(c,1,0),200);detach(v);v.attached=true;PreviewLayout1922.attached(v);h.drain();check(c.executions==2,"same-object reattach never revives old generation task");
  LayoutLifecycle1937.postCamera(h,new i.o.a.b1.a.d.a(c,1,0),200);LayoutLifecycle1937.postPreview(h,new i.o.a.m.j.b(f,1),200);LayoutLifecycle1937.postSettled(h,end,50);CameraShadeView old=v;detach(v);v=view();c.b=v;f.a=v;h.drain();check(c.executions==2&&f.executions==2&&end.executions==1,"reused controller with identical ratio on replacement view rejects old captured owner");
  check(!LayoutLifecycle1937.settled(old),"detached completion rejected");check(!LayoutLifecycle1937.camera(camera(old,0),1,0),"old controller cannot publish new global viewport");
  c.c=5;v.v=VEPreviewRadio.RADIO_ROUND;check(LayoutLifecycle1937.camera(c,2,5),"native round-grid ratio retained");check(!LayoutLifecycle1937.camera(c,2,0),"stale grid rejected");check(!LayoutLifecycle1937.camera(c,1,5),"stale 3:4 ratio rejected");
  VEPreviewRadio[] ratios={VEPreviewRadio.RADIO_FULL,VEPreviewRadio.RADIO_3_4,VEPreviewRadio.RADIO_1_1,VEPreviewRadio.RADIO_9_16};
  for(int value=0;value<4;value++){v.v=ratios[value];f.d=value;c.c=0;for(int incoming=0;incoming<4;incoming++){check(LayoutLifecycle1937.camera(c,incoming,0)==(value==incoming),"camera exact ratio contract");check(LayoutLifecycle1937.preview(f,incoming)==(value==incoming),"legacy exact ratio contract");}}
  v.v=VEPreviewRadio.RADIO_3_4;f.d=1;v.w=new ValueAnimator();check(!LayoutLifecycle1937.settled(v),"completion cannot overtake next transition");v.w=null;
  check(!LayoutLifecycle1937.camera(null,1,0)&&!LayoutLifecycle1937.preview(null,1)&&!LayoutLifecycle1937.settled(null),"null owner rejected");
  final int[] fallback={0};Runnable unknown=()->fallback[0]++;LayoutLifecycle1937.postCamera(h,unknown,7);LayoutLifecycle1937.postPreview(h,unknown,8);LayoutLifecycle1937.postSettled(h,unknown,9);h.drain();check(fallback[0]==3,"unexpected scheduling is preserved instead of cast failure");
  // Native setup can post before the first attachment/measurement. Its D callback
  // must still update both renderer and focus-overlay margins after first layout.
  detach(v);v=new CameraShadeView();v.attached=false;v.measuredWidth=0;v.measuredHeight=0;c=camera(v,0);
  LayoutLifecycle1937.postCamera(h,new i.o.a.b1.a.d.a(c,1,0),200);
  v.attached=true;PreviewLayout1922.attached(v);v.measuredWidth=742;v.measuredHeight=1536;v.margin=131;
  PreviewLayout1922.prepare(v);PreviewLayout1922.afterLayout(v);h.drain();
  check(c.executions==1,"pre-attach callback survives initial measurement");
  check(c.deliveredTop==131&&c.deliveredBottom==416&&c.deliveredHeight==989,"delayed native D reads corrected current 3:4 geometry");
  check(c.focusTop==131&&c.focusBottom==416,"native focus overlay updates were not lost");
  LayoutLifecycle1937.postCamera(h,new i.o.a.b1.a.d.a(c,1,0),200);v.margin=150;PreviewLayout1922.offsetChanged(v);h.drain();
  check(c.executions==2&&c.focusTop==150&&c.deliveredHeight==989,"late inset preserves native callback using new margins");
  // An A→B→A pending task can only reapply the currently selected A geometry.
  // This is intentionally harmless, whereas canceling it can lose native focus updates.
  LayoutLifecycle1937.postCamera(h,new i.o.a.b1.a.d.a(c,1,0),200);
  v.v=VEPreviewRadio.RADIO_1_1;PreviewLayout1922.prepare(v);v.v=VEPreviewRadio.RADIO_3_4;v.margin=160;PreviewLayout1922.prepare(v);PreviewLayout1922.afterLayout(v);
  h.drain();check(c.executions==3&&c.deliveredTop==160&&c.deliveredHeight==989,"old matching A applies latest A geometry rather than old captured bounds");
  // A replaced active view invalidates old ownership even without a detach callback.
  LayoutLifecycle1937.postCamera(h,new i.o.a.b1.a.d.a(c,1,0),200);old=v;v=view();PreviewLayout1922.attached(old);h.drain();check(c.executions==3,"active-owner replacement invalidates returning old view tasks");
  detach(old);detach(v);System.out.println("HOST_LAYOUT_LIFECYCLE1937_ASSERTIONS="+checks);
 }
}
