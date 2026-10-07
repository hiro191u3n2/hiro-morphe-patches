#!/usr/bin/env python3
"""Exercise production response helper: ready manual photo taps only, no capture."""
from pathlib import Path
import json, subprocess

FIXTURES={
'android/view/View.java':'''package android.view; import java.util.*;public class View {public boolean enabled=true,shown=true,attached=true,focus=true,postAllowed=true;public long now,lastDelay;public int posted,removed;public static class Task{Runnable run;long due;Task(Runnable r,long d){run=r;due=d;}}public ArrayList<Task> tasks=new ArrayList<Task>();public boolean isEnabled(){return enabled;}public boolean isShown(){return shown;}public boolean isAttachedToWindow(){return attached;}public boolean hasWindowFocus(){return focus;}public boolean postDelayed(Runnable r,long delay){lastDelay=delay;posted++;if(!postAllowed)return false;tasks.add(new Task(r,now+delay));return true;}public boolean removeCallbacks(Runnable r){removed++;return tasks.removeIf(t->t.run==r);}public void advance(long time){now=time;for(;;){Task found=null;for(Task t:tasks)if(t.due<=time){found=t;break;}if(found==null)return;tasks.remove(found);found.run.run();}}}''',
'com/hiro/ulike/AsyncSave1935.java':'''package com.hiro.ulike;public class AsyncSave1935 {public static int checks;public static boolean blocked,throwError;public static int readiness(Object camera){checks++;if(throwError)throw new LinkageError("scripted unavailable");return blocked?3:((Camera)camera).code;}}''',
'com/hiro/ulike/Camera.java':'''package com.hiro.ulike;public class Camera {public int code,captures;public boolean X(int count,boolean normal){captures++;return true;}}''',
'i/o/a/b1/a/w/b/a/b.java':'''package i.o.a.b1.a.w.b.a;public class b {public Object camera;public Object P(){return camera;}public static class a {public final b a;public a(b owner){a=owner;}}}''',
'i/o/a/b1/a/w/b/c/q.java':'''package i.o.a.b1.a.w.b.c;public class q extends i.o.a.b1.a.w.b.a.b {public static class b {public final q a;public b(q owner){a=owner;}}}''',
'com/hiro/ulike/ResponseTest1939.java':r'''package com.hiro.ulike;
public class ResponseTest1939 {
 public static class Button extends android.view.View{public int q=1002,f0=1;public boolean J,e0;public long L;public Object N;}
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static void windows(Button b,long t,long p,String why){check(ShutterResponse1939.touchWindow(b)==t,why+" touch");check(ShutterResponse1939.photoWindow(b)==p,why+" photo");}
 public static void main(String[]args){
  Button button=new Button();Camera camera=new Camera();i.o.a.b1.a.w.b.a.b owner=new i.o.a.b1.a.w.b.a.b();owner.camera=camera;button.N=new i.o.a.b1.a.w.b.a.b.a(owner);
  windows(button,0,0,"ready actual photo");
  for(int code:new int[]{1,3,7}){camera.code=code;windows(button,200,500,"native camera busy retains intervals");}camera.code=0;
  AsyncSave1935.blocked=true;windows(button,200,500,"save queue/memory blocked");AsyncSave1935.blocked=false;
  button.f0=0;windows(button,0,0,"normal main photo-on-release route");
  for(int status:new int[]{2,3,4}){button.f0=status;windows(button,200,500,"video/disabled status");}button.f0=1;
  button.q=1003;windows(button,200,500,"long-video surface");button.q=1002;
  button.J=true;windows(button,200,500,"active recording");button.J=false;
  button.enabled=false;windows(button,200,500,"disabled view");button.enabled=true;
  button.shown=false;windows(button,200,500,"hidden view");button.shown=true;
  button.attached=false;windows(button,200,500,"detached view");button.attached=true;
  button.focus=false;windows(button,200,500,"background view");button.focus=true;
  Object listener=button.N;button.N=new Object();windows(button,200,500,"unknown listener");button.N=null;windows(button,200,500,"unbound listener");button.N=listener;
  owner.camera=null;windows(button,200,500,"unbound camera");owner.camera=camera;
  AsyncSave1935.throwError=true;windows(button,200,500,"unavailable linkage fails closed");AsyncSave1935.throwError=false;
  check(ShutterResponse1939.touchWindow(null)==200&&ShutterResponse1939.photoWindow(new Object())==500,"unknown/non-view unchanged");
  i.o.a.b1.a.w.b.c.q assist=new i.o.a.b1.a.w.b.c.q();assist.camera=camera;button.N=new i.o.a.b1.a.w.b.c.q.b(assist);windows(button,0,0,"audited assist photo listener");
  for(int i=0;i<100;i++)windows(button,0,0,"repeated manual eligibility");
  check(camera.captures==0,"eligibility cannot call capture or queue any tap");
  camera.code=3;windows(button,200,500,"current busy state re-read after prior readiness");
  camera.code=0;button.f0=0;final int[] videos={0};Runnable nativeHold=()->{if(!button.e0&&!button.J){videos[0]++;button.J=true;}};
  button.now=0;button.L=0;button.e0=false;check(ShutterResponse1939.postHold(button,nativeHold,300),"original hold scheduled");
  button.advance(100);button.e0=true;button.advance(150);button.L=150;button.e0=false;check(ShutterResponse1939.postHold(button,nativeHold,300),"second manual down schedules own hold");
  button.advance(300);check(videos[0]==0,"old300ms task cannot turn new150ms press into video");
  button.advance(310);button.e0=true;button.advance(450);check(videos[0]==0,"second short tap never starts video");check(button.removed>=1,"prior native hold callback canceled");
  button.advance(500);button.L=500;button.e0=false;ShutterResponse1939.postHold(button,nativeHold,300);button.advance(799);check(videos[0]==0,"no premature video before original threshold");button.advance(800);check(videos[0]==1,"valid native hold still starts once at original300ms");button.advance(900);check(videos[0]==1,"valid native hold never duplicates");button.J=false;button.e0=true;
  for(int i=0;i<10;i++){long time=1000+i*50;button.advance(time);button.L=time;button.e0=false;ShutterResponse1939.postHold(button,nativeHold,300);button.advance(time+10);button.e0=true;}button.advance(2000);check(videos[0]==1,"rapid manual taps cannot leak old video timers");
  button.L=2000;button.e0=false;ShutterResponse1939.postHold(button,nativeHold,300);button.f0=1;button.advance(2300);check(videos[0]==1,"mode transition cancels old hold");button.f0=0;
  button.L=2300;button.e0=false;ShutterResponse1939.postHold(button,nativeHold,300);Object savedListener=button.N;button.N=new Object();button.advance(2600);check(videos[0]==1,"listener transition cancels old hold");button.N=savedListener;
  button.L=2600;button.e0=false;ShutterResponse1939.postHold(button,nativeHold,300);button.L=2650;button.advance(2900);check(videos[0]==1,"press timestamp change cannot reuse old hold");
  button.L=2900;button.e0=false;ShutterResponse1939.postHold(button,nativeHold,300);button.attached=false;button.advance(3200);check(videos[0]==1,"detached view cannot start video");button.attached=true;
  button.postAllowed=false;check(!ShutterResponse1939.postHold(button,nativeHold,300),"native post failure preserved");button.postAllowed=true;
  check(button.lastDelay==300,"native hold delay never shortened");check(camera.captures==0,"neither tap eligibility nor video guard starts or queues a photo");
  System.out.println("PASS response1939 assertions="+checks+" scenarios=29 device_tested=false");
 }
}'''}

def test(root,work,android=None):
    root,work=Path(root),Path(work)/'host-response1939';work.mkdir(parents=True,exist_ok=True)
    fixtures=work/'fixtures';classes=work/'classes';classes.mkdir(exist_ok=True)
    for name,text in FIXTURES.items():
        path=fixtures/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text)
    cmd=['javac','-encoding','UTF-8','-source','8','-target','8','-d',str(classes),str(root/'ShutterResponse1939.java')]+[str(p) for p in sorted(fixtures.rglob('*.java'))]
    built=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(work/'compile.log').write_text(built.stdout)
    if built.returncode:raise RuntimeError(built.stdout)
    ran=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.ResponseTest1939'],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=30);(work/'result.txt').write_text(ran.stdout)
    if ran.returncode:raise RuntimeError(ran.stdout)
    count=int(ran.stdout.split('assertions=')[1].split()[0]);result={'suite':'response1939','status':'passed','passed':True,'assertions':count,'scenarios':29,'device_tested':False}
    (work/'result.json').write_text(json.dumps(result,indent=2));return result
if __name__=='__main__':
    import sys;print(json.dumps(test(Path(__file__).parent,Path(sys.argv[1])),indent=2))
