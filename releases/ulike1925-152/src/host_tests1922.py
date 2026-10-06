"""Run production layout coordination Java with mocked Android animation/View clock.
These tests do not execute the stock Android layout implementation or a Galaxy camera.
"""
from pathlib import Path
import subprocess,re

def test(root,work):
 folder=work/'layout-host';src=folder/'src';classes=folder/'classes';src.mkdir(parents=True);classes.mkdir()
 files={
 'android/animation/Animator.java':'package android.animation;public class Animator {}',
 'android/animation/ValueAnimator.java':'''package android.animation;public class ValueAnimator extends Animator {public boolean started,cancelled;public int starts,removes,updateRemoves;public Runnable instant;public boolean isStarted(){return started;}public void start(){started=true;starts++;if(instant!=null)instant.run();}public void cancel(){cancelled=true;started=false;}public void removeAllUpdateListeners(){updateRemoves++;}public void removeAllListeners(){removes++;}}''',
  'android/graphics/RectF.java':'''package android.graphics;public class RectF {public float left,top,right,bottom;public RectF(){}public RectF(RectF r){left=r.left;top=r.top;right=r.right;bottom=r.bottom;}public RectF(float l,float t,float r,float b){left=l;top=t;right=r;bottom=b;}public boolean equals(Object o){if(!(o instanceof RectF))return false;RectF r=(RectF)o;return left==r.left&&top==r.top&&right==r.right&&bottom==r.bottom;}}''',
 'android/util/DisplayMetrics.java':'package android.util;public class DisplayMetrics {public int widthPixels=1080,heightPixels=2340;}',
 'android/content/res/Resources.java':'package android.content.res;public class Resources {public android.util.DisplayMetrics getDisplayMetrics(){return new android.util.DisplayMetrics();}}', 
 'com/ss/android/vesdk/VEPreviewRadio.java':(root/'layout-stubs/com/ss/android/vesdk/VEPreviewRadio.java').read_text(),
 'com/bytedance/corecamera/ui/view/CameraShadeView.java':r'''package com.bytedance.corecamera.ui.view;
import java.util.*;import android.animation.ValueAnimator;import android.graphics.RectF;import com.ss.android.vesdk.VEPreviewRadio;import com.hiro.ulike.PreviewLayout1922;
public class CameraShadeView {
 public static int d0,e0,S;public static float Q,R;public static VEPreviewRadio U;public int notifications;public int c=1080,j=2340,r,s,t,u,A;public boolean G;public VEPreviewRadio v=VEPreviewRadio.RADIO_3_4;public ValueAnimator w;public RectF n=new RectF(),o=new RectF(),p=new RectF(),q=new RectF();public static float b0,c0;public int margin=100,measuredWidth=-1,measuredHeight=-1;public int redraws,recalculations;public long now;public List<Task> queue=new ArrayList<>();
 public static class Task {public Runnable action;public long at;Task(Runnable r,long t){action=r;at=t;}}
 public int getWidth(){return measuredWidth<0?c:measuredWidth;} public int getHeight(){return measuredHeight<0?j:measuredHeight;}public int getRatio34TopMargin(){return margin+A;}public android.content.res.Resources getResources(){return new android.content.res.Resources();}public void b(){notifications++;}public void i(){redraws++;n.bottom=r+((v==VEPreviewRadio.RADIO_1_1||v==VEPreviewRadio.RADIO_ROUND)?1:0);o.top=j-s;}
 public boolean m(VEPreviewRadio ratio,boolean a,boolean b,boolean d){recalculations++;PreviewLayout1922.cancel(this);v=ratio;u=margin+A;t=j-(int)((long)c*4/3)-u;PreviewLayout1922.reconcile(this);return false;}
 public boolean postDelayed(Runnable r,long delay){queue.add(new Task(r,now+delay));return true;}
 public boolean removeCallbacks(Runnable r){queue.removeIf(t->t.action==r);return true;}
 public void advance(long time){while(true){Task next=null;for(Task t:queue)if(t.at<=time&&(next==null||t.at<next.at))next=t;if(next==null)break;queue.remove(next);now=next.at;next.action.run();}now=time;}
}''',
 'com/hiro/ulike/Tests1922.java':r'''package com.hiro.ulike;
import java.util.*;import android.animation.*;import com.bytedance.corecamera.ui.view.CameraShadeView;import com.ss.android.vesdk.VEPreviewRadio;
public class Tests1922 {
 static int count;static List<String> log=new ArrayList<>();static void check(String name,boolean pass){if(!pass)throw new AssertionError(name);count++;log.add("PASS\t"+name);}
 static CameraShadeView view(){CameraShadeView v=new CameraShadeView();v.u=100;v.t=800;v.r=100;v.s=800;v.i();return v;}
 static ValueAnimator pending(CameraShadeView v){ValueAnimator a=new ValueAnimator();v.w=a;PreviewLayout1922.ready(v);return a;}
 static void tick(CameraShadeView v,ValueAnimator a,int top,int bottom){if(PreviewLayout1922.owns(v,a)){v.r=top;v.s=bottom;v.i();}}
 public static void main(String[] args){
  CameraShadeView v=view();ValueAnimator old=new ValueAnimator();v.w=old;old.start();v.w=null;v.u=80;v.t=820;v.r=300;v.s=820;v.i();
  check("legacy cleared handle can leave stale mask (explicit host model)",v.n.bottom!=v.u&&old.isStarted());
  v=view();ValueAnimator a=pending(v);PreviewLayout1922.startCurrent(v);check("running animator retains ownership",v.w==a&&a.starts==1);PreviewLayout1922.cancel(v);check("cancel clears ownership and removes both callback classes",v.w==null&&a.cancelled&&a.removes==1&&a.updateRemoves==1);
  tick(v,a,310,500);check("obsolete update cannot write masks",v.r==100&&v.s==800);check("obsolete end cannot notify clients",!PreviewLayout1922.finish(v,a));
  a=pending(v);PreviewLayout1922.startCurrent(v);PreviewLayout1922.startCurrent(v);check("duplicate first-frame start not restarted",a.starts==1);v.margin=70;v.u=70;v.t=830;tick(v,a,90,810);check("current animation may interpolate",v.r==90&&v.s==810);PreviewLayout1922.reconcile(v);check("draw repair leaves live animation alone",v.r==90&&v.s==810);check("current end accepted",PreviewLayout1922.finish(v,a));check("end aligns masks with latest native targets",v.r==70&&v.s==830&&v.n.bottom==70&&v.o.top==1510&&v.w==null);check("duplicate end ignored",!PreviewLayout1922.finish(v,a));
  v=view();a=pending(v);PreviewLayout1922.schedule(v);check("first frame and fallback scheduled once",v.queue.size()==2);PreviewLayout1922.schedule(v);PreviewLayout1922.ready(v);check("repeated first frame does not flood/reset deadline",v.queue.size()==2);v.advance(199);check("native 200ms first-frame delay preserved",a.starts==0);v.advance(200);check("first frame starts exactly once and removes fallback",a.starts==1&&v.queue.isEmpty());v.advance(1000);check("old fallback cannot restart animation",a.starts==1);
  v=view();a=pending(v);v.advance(499);check("missing-first-frame fallback not early",a.starts==0);v.advance(500);check("missing first frame has bounded start",a.starts==1&&v.w==a);
  v=view();a=pending(v);PreviewLayout1922.schedule(v);List<Runnable> late=new ArrayList<>();for(CameraShadeView.Task t:v.queue)late.add(t.action);PreviewLayout1922.cancel(v);ValueAnimator newer=pending(v);for(Runnable task:late)task.run();check("stale queued tasks cannot start newer generation",a.starts==0&&newer.starts==0&&v.w==newer);v.advance(500);check("new generation retains own fallback",newer.starts==1);
  v=view();a=pending(v);PreviewLayout1922.schedule(v);PreviewLayout1922.cancel(v);v.advance(2000);check("detach cancels all queued starts",a.starts==0&&v.queue.isEmpty()&&v.w==null);
  v=view();a=pending(v);PreviewLayout1922.cancel(v);PreviewLayout1922.cancel(v);check("repeated detach/cancel is idempotent",a.removes==1&&a.updateRemoves==1);
  v=view();a=pending(v);final CameraShadeView instantView=v;final ValueAnimator instantAnimator=a;a.instant=()->PreviewLayout1922.finish(instantView,instantAnimator);PreviewLayout1922.startCurrent(v);PreviewLayout1922.ready(v);check("duration-zero synchronous end leaves no orphan ownership",v.w==null&&v.queue.isEmpty());
  v=view();int draws=v.redraws;PreviewLayout1922.reconcile(v);PreviewLayout1922.reconcile(v);check("stable draw does not invalidate repeatedly",v.redraws==draws);v.n.bottom=310;PreviewLayout1922.reconcile(v);check("settled stale top rectangle repaired",v.n.bottom==100&&v.redraws==draws+1);v.o.top=900;PreviewLayout1922.reconcile(v);check("settled bottom rectangle repaired",v.o.top==1540);draws=v.redraws;PreviewLayout1922.reconcile(v);check("repair converges after one frame",v.redraws==draws);
  for(VEPreviewRadio ratio:VEPreviewRadio.values()){v=view();v.v=ratio;v.r=1;v.s=2;PreviewLayout1922.reconcile(v);float expected=100+((ratio==VEPreviewRadio.RADIO_1_1||ratio==VEPreviewRadio.RADIO_ROUND)?1:0);check("preserve native target and square seam for "+ratio,v.n.bottom==expected&&v.o.top==1540);}
  v=view();v.c=0;v.r=1;PreviewLayout1922.reconcile(v);check("unmeasured width not normalized",v.r==1);v.c=1080;v.j=0;PreviewLayout1922.reconcile(v);check("unmeasured height not normalized",v.r==1);v.j=2340;v.v=null;PreviewLayout1922.reconcile(v);check("unset ratio not normalized",v.r==1);
  PreviewLayout1922.cancel(null);PreviewLayout1922.ready(null);PreviewLayout1922.schedule(null);PreviewLayout1922.reconcile(null);PreviewLayout1922.offsetChanged(null);check("null lifecycle input is harmless",!PreviewLayout1922.owns(null,new ValueAnimator())&&!PreviewLayout1922.finish(null,new ValueAnimator()));
  v=view();a=pending(v);v.margin=40;v.A=35;PreviewLayout1922.offsetChanged(v);check("changed top inset recomputes native ratio and cancels old transition",v.recalculations==1&&v.u==75&&a.cancelled);check("3:4 native height remains width based",v.o.top-v.n.bottom==1440);v.c=0;PreviewLayout1922.offsetChanged(v);check("early inset change waits for measurement",v.recalculations==1);
  for(int generation=0;generation<50;generation++) {v=view();ValueAnimator obsolete=pending(v);PreviewLayout1922.startCurrent(v);PreviewLayout1922.cancel(v);v.margin=40+generation;v.u=40+generation;v.t=860-generation;ValueAnimator current=pending(v);PreviewLayout1922.startCurrent(v);tick(v,current,v.u+10,v.t-10);tick(v,obsolete,300,900);boolean ignored=!PreviewLayout1922.finish(v,obsolete);boolean accepted=PreviewLayout1922.finish(v,current);check("racing transition permutation "+generation,ignored&&accepted&&v.r==v.u&&v.s==v.t&&v.n.bottom==v.u&&v.w==null);}
  
  v=view();v.c=742;v.j=1536;v.margin=61;v.u=61;v.t=487;PreviewLayout1922.prepare(v);PreviewLayout1922.ready(v);
  android.graphics.RectF rect=PreviewLayout1922.viewport();check("viewport uses live view width and height instead of cached display",rect.left==0&&rect.top==61&&rect.right==742&&rect.bottom==1050);
  rect.top=999;check("viewport never publishes shared mutable RectF",PreviewLayout1922.viewport().top==61);
  int notifications=v.notifications;PreviewLayout1922.afterLayout(v);PreviewLayout1922.ready(v);check("settled callback converges without notification loop",v.notifications==notifications);
  CameraShadeView.Q=999;CameraShadeView.R=999;CameraShadeView.S=999;PreviewLayout1922.reconcile(v);check("draw and render metadata share the current view coordinate system",CameraShadeView.Q==61&&CameraShadeView.R==486&&CameraShadeView.S==1536);
  check("local width and height reflect measurement",PreviewLayout1922.localWidth(v)==742&&PreviewLayout1922.localHeight(v)==1536);
  CameraShadeView.d0=20;CameraShadeView.e0=20;PreviewLayout1922.reconcile(v);rect=PreviewLayout1922.viewport();check("stale square side insets removed for 3:4",rect.left==0&&rect.right==742);CameraShadeView.d0=0;CameraShadeView.e0=0;
  PreviewLayout1922.detached(v);check("detached view cannot supply stale crop to new camera",PreviewLayout1922.viewport()==null);

  // New regression: old TARGETS, not only the drawn rectangles, can be invalid.
  v=view();v.c=742;v.j=1536;v.margin=61;v.u=v.r=309;v.t=v.s=416;v.i();
  PreviewLayout1922.attached(v);rect=PreviewLayout1922.viewport();
  check("reproduced stale target corrected to native 3:4 margin",v.u==61&&v.t==486&&v.r==61&&v.s==486);
  check("preview span equals selected aspect ratio rather than stale crop",rect.bottom-rect.top==989);
  notifications=v.notifications;PreviewLayout1922.schedule(v);
  check("first frame redelivers same geometry for a new renderer",v.notifications==notifications+1);
  notifications=v.notifications;PreviewLayout1922.attached(v);
  check("reattach redelivers even with unchanged window size",v.notifications==notifications+1);
  v.margin=80;PreviewLayout1922.reconcile(v);
  check("native top margin change repaired without bounds change",v.u==80&&v.t==467&&PreviewLayout1922.viewport().top==80);
  v.A=23;PreviewLayout1922.reconcile(v);
  check("insets reflected without offset-setter notification",v.u==103&&v.t==444);
  v.measuredWidth=778;v.measuredHeight=1600;PreviewLayout1922.reconcile(v);
  check("measured view extent overrides stale cached dimensions",v.c==778&&v.j==1600&&v.u==103&&v.t==460);
  check("mask and render rectangles share measured extents",v.n.right==778&&v.o.right==778&&v.o.bottom==1600&&PreviewLayout1922.viewport().right==778);
  rect=PreviewLayout1922.viewport();v.u=333;check("renderer reads immutable last published target not torn fields",PreviewLayout1922.viewport().top==103);PreviewLayout1922.reconcile(v);
  ValueAnimator bad=new ValueAnimator();v.w=bad;bad.start();v.u=309;v.t=416;PreviewLayout1922.ready(v);
  check("bad pending target cancels obsolete transition before repair",bad.cancelled&&v.w==null&&v.u==103);
  for(int width:new int[]{320,360,742,778,1080,1440})for(int top:new int[]{0,40,61,150}) {
   v=view();v.c=width;v.j=width*2+40;v.margin=top;v.u=309;v.t=416;v.i();
   PreviewLayout1922.afterLayout(v);rect=PreviewLayout1922.viewport();
   check("3:4 invariant "+width+" margin "+top,rect.right==width&&rect.top==top&&(int)(rect.bottom-rect.top)==(int)((long)width*4/3));
  }
  v=view();v.margin=2000;PreviewLayout1922.reconcile(v);
  check("margin clamped to available ratio-fitting container",v.u==900&&v.t==0);
  v=view();v.c=1200;v.j=700;v.u=10;v.t=10;v.r=10;v.s=10;v.i();PreviewLayout1922.reconcile(v);
  check("too short container not assigned a negative mask",v.u==10&&v.t==10);
  v=view();v.v=VEPreviewRadio.RADIO_1_1;v.u=80;v.t=1180;CameraShadeView.d0=12;CameraShadeView.e0=12;PreviewLayout1922.reconcile(v);
  check("native square target and intentional side mask retained",v.u==80&&v.t==1180&&CameraShadeView.d0==12&&CameraShadeView.e0==12);
  CameraShadeView.d0=0;CameraShadeView.e0=0;PreviewLayout1922.detached(v);
  check("new snapshot removed on detach",PreviewLayout1922.viewport()==null);
  for(String s:log)System.out.println(s);System.out.println("HOST_LAYOUT_ASSERTIONS="+count);System.out.println("MOCKED_ANDROID_NOT_A_DEVICE_TEST");
 }
}'''
 }
 for name,text in files.items():p=src/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
 cmd=['javac','--release','8','-encoding','UTF-8','-d',str(classes),*[str(p) for p in sorted(src.rglob('*.java'))],str(root/'PreviewLayout1922.java')]
 r=subprocess.run(cmd,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True);(folder/'compile.txt').write_text(r.stdout);r.check_returncode()
 r=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.Tests1922'],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True);(work/'host-tests1922.txt').write_text(r.stdout);print(r.stdout,end='');r.check_returncode()
 return int(re.search(r'HOST_LAYOUT_ASSERTIONS=(\d+)',r.stdout)[1])
