"""Production gesture router with mock Android, not device/ART execution."""
from pathlib import Path
import subprocess,re

def test(root,work):
 folder=work/'gesture-host';src=folder/'src';classes=folder/'classes';src.mkdir(parents=True);classes.mkdir()
 files={
 'android/os/Looper.java':'''package android.os;public class Looper {static Looper main=new Looper();public static boolean worker;public static Looper myLooper(){return worker?null:main;}public static Looper getMainLooper(){return main;}}''',
 'android/os/SystemClock.java':'''package android.os;public class SystemClock{public static long now=5000;public static long uptimeMillis(){return now;}}''',
 'android/view/MotionEvent.java':'''package android.view;public class MotionEvent {public static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3,ACTION_POINTER_DOWN=5;public long time;public float x,y;public int pointers=1,action;public MotionEvent(long t,float a,float b){time=t;x=a;y=b;}public int getActionMasked(){return action;}public long getDownTime(){return time;}public float getRawX(){return x;}public float getRawY(){return y;}public int getPointerCount(){return pointers;}}''',
 'android/graphics/RectF.java':'''package android.graphics;public class RectF {public float left,top,right,bottom;public RectF(float l,float t,float r,float b){left=l;top=t;right=r;bottom=b;}}''',
 'android/view/View.java':'''package android.view;public class View {public boolean attached=true,shown=true,enabled=true,focus=true;public View root=this;public int width=1080,height=2340,sx=0,sy=24;public boolean isAttachedToWindow(){return attached;}public boolean isShown(){return shown;}public boolean isEnabled(){return enabled;}public boolean hasWindowFocus(){return focus;}public View getRootView(){return root;}public int getWidth(){return width;}public int getHeight(){return height;}public void getLocationOnScreen(int[] x){x[0]=sx;x[1]=sy;}}''',
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
   ck("only native switch invoked y="+ypos,owner.flips==1&&listener.nativeDoubles==0&&listener.singles==0);
   ck("no delayed single after double y="+ypos,!BlackTap1925.confirmed(bg,e)&&listener.singles==0);
  }
  setup();MotionEvent e=at(201,400,30);BlackTap1925.singleUp(bg,e);ck("confirmed single delivered exactly once",BlackTap1925.confirmed(bg,e)&&listener.singles==1&&!BlackTap1925.confirmed(bg,e));
  for(float ypos:new float[]{60,500,1499}){setup();e=at(301,400,ypos);ck("preview immediate single unchanged "+ypos,!BlackTap1925.singleUp(bg,e)&&listener.singles==1);ck("preview native double unchanged "+ypos,BlackTap1925.doubleTap(bg,e)&&listener.nativeDoubles==1&&owner.flips==0);ck("preview confirm not duplicated "+ypos,!BlackTap1925.confirmed(bg,e)&&listener.singles==1);}
  setup();e=at(401,400,1700);doubles(e);doubles(e);ck("rapid repeats cannot switch twice",owner.flips==1);SystemClock.now+=799;doubles(e);ck("799ms guarded",owner.flips==1);SystemClock.now++;doubles(e);ck("800ms new gesture may switch",owner.flips==2);
  for(int busy=0;busy<3;busy++){setup();owner.recording=busy==0;owner.l=busy==1;ExitBusy1921.busy=busy==2;doubles(at(501,400,1700));ck("record capture save guard "+busy,owner.flips==0&&listener.nativeDoubles==0&&listener.singles==0);}
  setup();e=at(601,400,1700);BlackTap1925.singleUp(bg,e);shade.w=new Object();shade.n.bottom=309;shade.o.top=1800;BlackTap1925.doubleTap(bg,e);ck("rectangle change retains owned double without blank handler",owner.flips==1&&listener.nativeDoubles==0);
  setup();shade.w=new Object();shade.n.bottom=309;shade.o.top=1800;doubles(at(701,400,200));ck("drawn black bar during animation recognized",owner.flips==1&&listener.nativeDoubles==0);
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
  System.out.println("HOST_GESTURE_ASSERTIONS="+count);System.out.println("PRODUCTION_ROUTER_WITH_MOCK_ANDROID_NOT_DEVICE_GESTURE_TEST");
 }
}'''
 }
 for name,text in files.items():p=src/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
 r=subprocess.run(['javac','--release','8','-encoding','UTF-8','-d',str(classes),*[str(p) for p in src.rglob('*.java')],str(root/'BlackTap1925.java')],capture_output=True,text=True)
 (folder/'compile.log').write_text(r.stdout+r.stderr);r.check_returncode()
 r=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.GestureTests1925'],capture_output=True,text=True)
 (work/'host-gesture1925.txt').write_text(r.stdout+r.stderr);print((r.stdout+r.stderr)[-1200:]);r.check_returncode()
 return int(re.search(r'HOST_GESTURE_ASSERTIONS=(\d+)',r.stdout)[1])
