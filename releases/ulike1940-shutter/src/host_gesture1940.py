"""Production gesture router with mock Android, not device/ART execution."""
from pathlib import Path
import subprocess,re,json

FEEDBACK_STUBS={
 'android/util/DisplayMetrics.java':'''package android.util;public class DisplayMetrics{public float density=1;}''',
 'android/content/res/Resources.java':'''package android.content.res;public class Resources{public android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();public android.util.DisplayMetrics getDisplayMetrics(){return metrics;}}''',
 'android/graphics/Color.java':'''package android.graphics;public class Color{public static final int BLACK=0xff000000,WHITE=0xffffffff;}''',
 'android/graphics/ColorFilter.java':'''package android.graphics;public class ColorFilter{}''',
 'android/graphics/PixelFormat.java':'''package android.graphics;public class PixelFormat{public static final int TRANSLUCENT=-3;}''',
 'android/graphics/Paint.java':'''package android.graphics;public class Paint{public static final int ANTI_ALIAS_FLAG=1;public enum Style{FILL,STROKE};public int color,alpha;public Style style;public float stroke;public Paint(int flags){}public void setStyle(Style x){style=x;}public void setColor(int x){color=x;alpha=x>>>24;}public void setAlpha(int x){alpha=x;}public void setStrokeWidth(float x){stroke=x;}}''',
 'android/graphics/Canvas.java':'''package android.graphics;public class Canvas{public java.util.List<Entry> ops=new java.util.ArrayList<>();public RectF clip;public static class Entry{public RectF rect;public int color,alpha;public Paint.Style style;public float stroke;Entry(RectF r,Paint p){rect=r;color=p.color;alpha=p.alpha;style=p.style;stroke=p.stroke;}}public int save(){return 1;}public boolean clipRect(RectF r){clip=r;return true;}public void drawRect(RectF r,Paint p){ops.add(new Entry(r,p));}public void drawRect(float a,float b,float c,float d,Paint p){drawRect(new RectF(a,b,c,d),p);}public void restoreToCount(int saved){}}''',
 'android/graphics/drawable/Drawable.java':'''package android.graphics.drawable;public abstract class Drawable{public int left,top,right,bottom;public void setBounds(int a,int b,int c,int d){left=a;top=b;right=c;bottom=d;}public abstract void draw(android.graphics.Canvas c);public abstract void setAlpha(int a);public abstract void setColorFilter(android.graphics.ColorFilter c);public abstract int getOpacity();}''',
 'android/view/ViewOverlay.java':'''package android.view;public class ViewOverlay{public java.util.ArrayList<android.graphics.drawable.Drawable> drawables=new java.util.ArrayList<>();public void add(android.graphics.drawable.Drawable d){if(!drawables.contains(d))drawables.add(d);}public void remove(android.graphics.drawable.Drawable d){drawables.remove(d);}}''',
 'com/hiro/ulike/FeedbackTests1940.java':'''package com.hiro.ulike;
import android.os.*;import android.view.*;import android.graphics.*;import android.graphics.drawable.*;import com.bytedance.corecamera.ui.view.*;
public final class FeedbackTests1940 {
 static int count;static CameraShadeView shade;static View nativeFlash;static View frontFill;
 static void ck(String n,boolean ok){if(!ok)throw new AssertionError(n);count++;System.out.println("PASS\\t"+n);}
 static void setup(){Looper.worker=false;SystemClock.now+=1000;shade=new CameraShadeView();nativeFlash=new View();nativeFlash.root=shade;frontFill=new View();PreviewLayout1922.view=shade;PreviewLayout1922.rect=new RectF(0,60,1080,1500);}
 static Canvas frame(){Canvas canvas=new Canvas();shade.overlay.drawables.get(0).draw(canvas);return canvas;}
 static void fallback(String n){ck(n,!ShutterFeedback1940.show(nativeFlash)&&nativeFlash.visibility==0&&nativeFlash.clearAnimations==0&&shade.overlay.drawables.isEmpty());}
 public static void main(String[] args){
  setup();ck("accepted photo shows frame",ShutterFeedback1940.show(nativeFlash));
  ck("replaces original white mask only after readiness",nativeFlash.visibility==8&&nativeFlash.clearAnimations==1);
  ck("single drawable overlay never adds input view",shade.overlay.drawables.size()==1);
  ck("front fill light untouched",frontFill.visibility==0&&frontFill.clearAnimations==0&&frontFill.callbacks.isEmpty());
  Canvas c=frame();ck("one tint plus dark and white outline",c.ops.size()==3);
  ck("preview remains visible through 12 percent tint",c.ops.get(0).alpha==31&&c.ops.get(0).style==Paint.Style.FILL);
  ck("bright frame uses dark edge",c.ops.get(1).color==Color.BLACK&&c.ops.get(1).alpha==230&&c.ops.get(2).color==Color.WHITE&&c.ops.get(2).alpha==255);
  ck("five dp frame, eight dp outer edge",c.ops.get(1).stroke==8&&c.ops.get(2).stroke==5);
  ck("frame limited to preview, black controls excluded",c.clip.left==0&&c.clip.top==60&&c.clip.right==1080&&c.clip.bottom==1500);
  ck("hold protects cue from imperceptible blink",ShutterFeedback1940.strength(0)==1&&ShutterFeedback1940.strength(90)==1);
  SystemClock.now+=165;c=frame();ck("smooth fade after hold",c.ops.get(2).alpha>0&&c.ops.get(2).alpha<255);
  ck("fade endpoints bounded",ShutterFeedback1940.strength(-1)==0&&ShutterFeedback1940.strength(239)>0&&ShutterFeedback1940.strength(240)==0);
  Runnable old=shade.callbacks.keySet().iterator().next();SystemClock.now+=75;old.run();ck("240ms completion frees overlay and callbacks",shade.overlay.drawables.isEmpty()&&shade.callbacks.isEmpty());
  setup();ShutterFeedback1940.show(nativeFlash);old=shade.callbacks.keySet().iterator().next();SystemClock.now+=80;ck("quick accepted repeat restarts pulse",ShutterFeedback1940.show(nativeFlash)&&shade.overlay.drawables.size()==1&&shade.callbacks.size()==1);
  SystemClock.now+=160;old.run();ck("old callback cannot erase new cue",shade.overlay.drawables.size()==1&&shade.callbacks.size()==1);
  ck("new cue owns updated deadline",shade.callbacks.values().iterator().next()==SystemClock.now+80);
  SystemClock.now+=80;old.run();ck("new deadline removes cue",shade.overlay.drawables.isEmpty()&&shade.callbacks.isEmpty());
  setup();shade.w=new Object();shade.n.bottom=309;shade.o.top=1800;ck("transition uses actual drawn bounds",ShutterFeedback1940.show(nativeFlash)&&frame().clip.top==309&&frame().clip.bottom==1800);
  setup();Looper.worker=true;fallback("off-main returns native fallback with mask untouched");Looper.worker=false;
  setup();shade.attached=false;fallback("detached native fallback");
  setup();shade.shown=false;fallback("hidden shade native fallback");
  setup();shade.focus=false;fallback("unfocused native fallback");
  setup();nativeFlash.root=new View();fallback("other window native fallback");
  setup();PreviewLayout1922.rect=null;fallback("missing geometry native fallback");
  setup();PreviewLayout1922.rect=new RectF(0,60,1081,1500);fallback("invalid geometry native fallback");
  setup();PreviewLayout1922.rect=new RectF(Float.NaN,60,1080,1500);fallback("NaN geometry native fallback");
  setup();PreviewLayout1922.view=null;fallback("missing shade native fallback");
  setup();shade.width=0;fallback("unmeasured view native fallback");
  setup();shade.schedule=false;fallback("unscheduled cleanup rejects custom feedback");
  setup();shade.failOverlay=true;fallback("exception permits original native blink");shade.failOverlay=false;ck("exception cleanup permits later normal cue",ShutterFeedback1940.show(nativeFlash)&&shade.overlay.drawables.size()==1);
  setup();ck("null native view safe",!ShutterFeedback1940.show(null)&&shade.overlay.drawables.isEmpty());
  setup();ShutterFeedback1940.show(nativeFlash);shade.attached=false;old=shade.callbacks.keySet().iterator().next();old.run();ck("detachment immediately cleans pulse",shade.overlay.drawables.isEmpty()&&shade.callbacks.isEmpty());
  setup();int original=0;if(!ShutterFeedback1940.show(nativeFlash))original++;ck("success bypasses native blink",original==0);PreviewLayout1922.rect=null;if(!ShutterFeedback1940.show(nativeFlash))original++;ck("readiness failure requests original native blink exactly once",original==1);
  ck("all bounds validation handles empty areas",!ShutterFeedback1940.valid(new RectF(5,5,5,30),1080,2340)&&!ShutterFeedback1940.valid(new RectF(-1,0,40,40),1080,2340));
  System.out.println("HOST_FEEDBACK_ASSERTIONS="+count);System.out.println("MOCK_ANDROID_ONLY_NO_HARDWARE_OR_VISUAL_DEVICE_TEST");
 }
}'''
}

def test(root,work,android=None):
 folder=work/'gesture-host';src=folder/'src';classes=folder/'classes';src.mkdir(parents=True);classes.mkdir()
 files={
 'android/os/Looper.java':'''package android.os;public class Looper {static Looper main=new Looper();public static boolean worker;public static Looper myLooper(){return worker?null:main;}public static Looper getMainLooper(){return main;}}''',
 'android/os/SystemClock.java':'''package android.os;public class SystemClock{public static long now=5000;public static long uptimeMillis(){return now;}}''',
 'android/view/MotionEvent.java':'''package android.view;public class MotionEvent {public static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3,ACTION_POINTER_DOWN=5;public long time;public float x,y;public int pointers=1,action;public MotionEvent(long t,float a,float b){time=t;x=a;y=b;}public int getActionMasked(){return action;}public long getDownTime(){return time;}public float getRawX(){return x;}public float getRawY(){return y;}public int getPointerCount(){return pointers;}}''',
 'android/graphics/RectF.java':'''package android.graphics;public class RectF {public float left,top,right,bottom;public RectF(float l,float t,float r,float b){left=l;top=t;right=r;bottom=b;}}''',
 'android/view/View.java':'''package android.view;public class View {public boolean attached=true,shown=true,enabled=true,focus=true;public int visibility=0,clearAnimations=0,invalidations=0;public boolean schedule=true;public boolean failOverlay;public java.util.LinkedHashMap<Runnable,Long> callbacks=new java.util.LinkedHashMap<>();public android.view.ViewOverlay overlay=new android.view.ViewOverlay();public android.content.res.Resources resources=new android.content.res.Resources();public View root=this;public int width=1080,height=2340,sx=0,sy=24;public boolean isAttachedToWindow(){return attached;}public boolean isShown(){return shown;}public boolean isEnabled(){return enabled;}public boolean hasWindowFocus(){return focus;}public View getRootView(){return root;}public int getWidth(){return width;}public int getHeight(){return height;}public void getLocationOnScreen(int[] x){x[0]=sx;x[1]=sy;}public static final int GONE=8;public android.content.res.Resources getResources(){return resources;}public android.view.ViewOverlay getOverlay(){if(failOverlay)throw new IllegalStateException("overlay unavailable");return overlay;}public void clearAnimation(){clearAnimations++;}public void setVisibility(int v){visibility=v;}public boolean removeCallbacks(Runnable r){return callbacks.remove(r)!=null;}public boolean postDelayed(Runnable r,long t){if(schedule)callbacks.put(r,android.os.SystemClock.now+t);return schedule;}public void postInvalidateOnAnimation(){invalidations++;}public void invalidate(){invalidations++;}}''',
 'com/bytedance/corecamera/ui/view/GestureBgLayout.java':'''package com.bytedance.corecamera.ui.view;public class GestureBgLayout extends android.view.View {public d c;public interface d {boolean a(android.view.MotionEvent e);void onDoubleTap(android.view.MotionEvent e);}}''',
 'com/bytedance/corecamera/ui/view/CameraShadeView.java':'''package com.bytedance.corecamera.ui.view;public class CameraShadeView extends android.view.View {public Object w;public int c=1080,j=2340;public android.graphics.RectF n=new android.graphics.RectF(0,0,1080,60),o=new android.graphics.RectF(0,1500,1080,2340);}''',
 'com/hiro/ulike/PreviewLayout1922.java':'''package com.hiro.ulike;public class PreviewLayout1922 {public static com.bytedance.corecamera.ui.view.CameraShadeView view;public static android.graphics.RectF rect=new android.graphics.RectF(0,60,1080,1500);public static com.bytedance.corecamera.ui.view.CameraShadeView gestureView1925(){return view;}public static android.graphics.RectF viewport(){return rect;}}''',
 'com/hiro/ulike/ExitBusy1921.java':'''package com.hiro.ulike;public class ExitBusy1921 {public static boolean busy;public static boolean captureBusy(boolean ignored){return busy;}}''',
 'i/o/a/b1/a/g/y.java':'''package i.o.a.b1.a.g;public class y {public boolean l,recording;public int flips;public boolean c1(){return recording;}public void switchCamera(){flips++;}public static class h implements com.bytedance.corecamera.ui.view.GestureBgLayout.d {public final y a;public int singles,nativeDoubles;public h(y owner){a=owner;}public boolean a(android.view.MotionEvent e){singles++;return true;}public void onDoubleTap(android.view.MotionEvent e){nativeDoubles++;}}}''',
 'com/hiro/ulike/GestureTests1925.java':'''package com.hiro.ulike;
import android.view.*;import android.os.*;import android.graphics.*;import com.bytedance.corecamera.ui.view.*;import i.o.a.b1.a.g.y;
public class GestureTests1925 {
 static int count;static GestureBgLayout bg;static CameraShadeView shade;static y owner;static y.h listener;
 static void ck(String n,boolean v){if(!v)throw new AssertionError(n);count++;System.out.println("PASS\\t"+n);}
 static void setup(){bg=new GestureBgLayout();shade=new CameraShadeView();shade.root=bg;PreviewLayout1922.view=shade;PreviewLayout1922.rect=new RectF(0,60,1080,1500);owner=new y();listener=new y.h(owner);bg.c=listener;SystemClock.now+=2000;ExitBusy1921.busy=false;Looper.worker=false;}
 static MotionEvent at(long time,float x,float y){return new MotionEvent(time,x,y+24);}
 static void doubles(MotionEvent e){e.action=MotionEvent.ACTION_UP;BlackTap1925.singleUp(bg,e);e.action=MotionEvent.ACTION_DOWN;BlackTap1925.doubleTap(bg,e);}
 public static void main(String[] args){
  for(float ypos:new float[]{0,30,59,1500,1700,2339}){
   setup();MotionEvent e=at(100,400,ypos);e.action=1;ck("black first tap deferred y="+ypos,BlackTap1925.singleUp(bg,e)&&listener.singles==0);
   e.action=0;ck("black double consumed y="+ypos,BlackTap1925.doubleTap(bg,e));
   ck("black double has no camera action y="+ypos,owner.flips==0&&listener.nativeDoubles==0&&listener.singles==0);
   ck("no delayed single after double y="+ypos,!BlackTap1925.confirmed(bg,e)&&listener.singles==0);
  }
  setup();MotionEvent e=at(201,400,30);BlackTap1925.singleUp(bg,e);ck("confirmed single delivered exactly once",BlackTap1925.confirmed(bg,e)&&listener.singles==1&&!BlackTap1925.confirmed(bg,e));
  for(float ypos:new float[]{60,500,1499}){setup();e=at(301,400,ypos);ck("preview immediate single unchanged "+ypos,!BlackTap1925.singleUp(bg,e)&&listener.singles==1);ck("preview native double unchanged "+ypos,BlackTap1925.doubleTap(bg,e)&&listener.nativeDoubles==1&&owner.flips==0);ck("preview confirm not duplicated "+ypos,!BlackTap1925.confirmed(bg,e)&&listener.singles==1);}
  setup();e=at(401,400,1700);doubles(e);doubles(e);ck("rapid repeats never switch",owner.flips==0);SystemClock.now+=799;doubles(e);ck("799ms guarded",owner.flips==0);SystemClock.now++;doubles(e);ck("800ms later double remains inert",owner.flips==0);
  for(int busy=0;busy<3;busy++){setup();owner.recording=busy==0;owner.l=busy==1;ExitBusy1921.busy=busy==2;doubles(at(501,400,1700));ck("record capture save guard "+busy,owner.flips==0&&listener.nativeDoubles==0&&listener.singles==0);}
  setup();e=at(601,400,1700);BlackTap1925.singleUp(bg,e);shade.w=new Object();shade.n.bottom=309;shade.o.top=1800;BlackTap1925.doubleTap(bg,e);ck("rectangle change retains owned double without blank handler",owner.flips==0&&listener.nativeDoubles==0);
  setup();shade.w=new Object();shade.n.bottom=309;shade.o.top=1800;doubles(at(701,400,200));ck("drawn black bar during animation recognized",owner.flips==0&&listener.nativeDoubles==0);
  setup();e=at(801,400,1700);BlackTap1925.singleUp(bg,e);bg.attached=false;BlackTap1925.doubleTap(bg,e);ck("detached pending callback cannot flip",owner.flips==0);
  for(int state=0;state<3;state++){setup();e=at(901,400,1700);BlackTap1925.singleUp(bg,e);if(state==0)bg.shown=false;if(state==1)bg.focus=false;if(state==2)bg.enabled=false;BlackTap1925.doubleTap(bg,e);ck("hidden unfocused disabled owned gesture blocked "+state,owner.flips==0&&listener.nativeDoubles==0);}
  for(int action:new int[]{3,5}){setup();e=at(1001,400,1700);BlackTap1925.singleUp(bg,e);MotionEvent cancel=at(1001,400,1700);cancel.action=action;BlackTap1925.reset(bg,cancel);ck("cancel multitouch clears deferred single "+action,!BlackTap1925.confirmed(bg,e)&&listener.singles==0);}
  setup();e=at(1101,400,1700);BlackTap1925.singleUp(bg,e);BlackTap1925.detached(bg);ck("explicit detach clears pending",!BlackTap1925.confirmed(bg,e));
  setup();e=at(1201,400,1700);BlackTap1925.singleUp(bg,e);ck("wrong confirmation id ignored",!BlackTap1925.confirmed(bg,at(1202,400,1700))&&listener.singles==0);
  setup();Looper.worker=true;ck("non UI caller no native invocation",!BlackTap1925.doubleTap(bg,at(1301,400,1700))&&!BlackTap1925.singleUp(bg,at(1301,400,1700))&&owner.flips==0);Looper.worker=false;
  setup();e=at(1401,400,1700);BlackTap1925.singleUp(bg,e);e.pointers=2;BlackTap1925.doubleTap(bg,e);ck("owned multi pointer double cannot flip",owner.flips==0);
  setup();e=at(1501,400,1700);e.action=1;BlackTap1925.singleUp(bg,e);BlackTap1925.doubleTap(bg,e);ck("second up cannot repeat double action",owner.flips==0);
  setup();shade.sx=100;shade.sy=80;e=new MotionEvent(1601,300,110);ck("raw coordinates account for window offset",BlackTap1925.black(bg,e));
  shade.root=new View();ck("other window never classified as black",!BlackTap1925.black(bg,e));
  setup();shade.c=720;ck("stale dimension snapshot not used",!BlackTap1925.black(bg,at(1701,400,1700)));
  setup();PreviewLayout1922.rect=null;ck("missing viewport not guessed",!BlackTap1925.black(bg,at(1801,400,1700)));
  for(float[] xy:new float[][]{{-1,1700},{1080,1700},{400,-1},{400,2340},{Float.NaN,20},{10,Float.NaN},{Float.POSITIVE_INFINITY,10}})ck("out of range coordinates "+xy[0]+","+xy[1],!BlackTap1925.outside(new RectF(0,60,1080,1500),xy[0],xy[1],1080,2340));
  setup();bg.c=null;ck("absent native listener safe",!BlackTap1925.doubleTap(bg,at(1901,400,1700)));
  setup();final int[] other={0,0};bg.c=new GestureBgLayout.d(){public boolean a(MotionEvent e){other[0]++;return true;}public void onDoubleTap(MotionEvent e){other[1]++;}};e=at(2001,400,1700);BlackTap1925.singleUp(bg,e);BlackTap1925.doubleTap(bg,e);ck("non camera listener unchanged",other[0]==1&&other[1]==1&&owner.flips==0);
  setup();owner.switchCamera();ck("explicit native camera-switch action retained",owner.flips==1);
  System.out.println("HOST_GESTURE_ASSERTIONS="+count);System.out.println("PRODUCTION_ROUTER_WITH_MOCK_ANDROID_NOT_DEVICE_GESTURE_TEST");
 }
}'''
 }
 files.update(FEEDBACK_STUBS)
 for name,text in files.items():p=src/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
 baseline=(root/'feedback-host-baseline'/'BlackTap1925.java').read_text()
 if baseline.count('owner.switchCamera();')!=1:raise AssertionError('Pinned one-call source baseline')
 (src/'com/hiro/ulike/BlackTap1925.java').write_text(baseline.replace('owner.switchCamera();','/* black double-tap deliberately consumed without an action */'))
 r=subprocess.run(['javac','-source','8','-target','8','-encoding','UTF-8','-d',str(classes),*[str(p) for p in src.rglob('*.java')],str(root/'ShutterFeedback1940.java')],capture_output=True,text=True)
 (folder/'compile.log').write_text(r.stdout+r.stderr);r.check_returncode()
 r=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.GestureTests1925'],capture_output=True,text=True)
 (work/'host-gesture1940.txt').write_text(r.stdout+r.stderr);print((r.stdout+r.stderr)[-1200:]);r.check_returncode()
 count=int(re.search(r'HOST_GESTURE_ASSERTIONS=(\d+)',r.stdout)[1])
 flash=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.FeedbackTests1940'],capture_output=True,text=True)
 (work/'host-feedback1940.txt').write_text(flash.stdout+flash.stderr);flash.check_returncode()
 count+=int(re.search(r'HOST_FEEDBACK_ASSERTIONS=(\d+)',flash.stdout)[1])
 result={'suite':'gesture_feedback','status':'passed','passed':True,'assertions':count,'device_tested':False}
 (work/'host-gesture-feedback1940.json').write_text(json.dumps(result,indent=2))
 return result
