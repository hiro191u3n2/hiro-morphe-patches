#!/usr/bin/env python3
"""Execute production ExitBusy/ExitFlow Java against explicitly mocked Android state.
The production DEX field inventory is checked separately by Transform1921.
No device, Camera2, native saving or Android input is executed here.
"""
from pathlib import Path
import subprocess

def test(root, work):
 folder=work/'exit-host';folder.mkdir();src=folder/'src';src.mkdir();out=folder/'classes';out.mkdir()
 schema=(work/'emitted/busy-schema.tsv').read_text().splitlines()
 assert len(schema)==16 and 'StyleStill4\tcallbacks\tABSENT\t0' in schema
 required={'OpticalZoom':{'inFlight','stillUntil'},'CaptureYuv':{'states'},'CaptureYuv$State':{'delivering','selected','pending','captureCallback'},'LensRelease163':{'LOCK','callbacks','nativeCallbacks','deliveries','stops'}}
 for cls,fields in required.items():
  assert fields.issubset({r.split('\t')[1] for r in schema if r.split('\t')[0]==cls}), 'Mock field absent from actual DEX: '+cls
 files={
 'android/app/Activity.java':'package android.app;public class Activity {}',
 'android/os/SystemClock.java':'package android.os;public class SystemClock {public static long now=100;public static long uptimeMillis(){return now;}}',
 'android/os/Looper.java':'package android.os;public class Looper {static final Looper MAIN=new Looper(),OTHER=new Looper();public static boolean main=true;public static Looper myLooper(){return main?MAIN:OTHER;}public static Looper getMainLooper(){return MAIN;}}',
 'android/util/Log.java':'package android.util;public class Log {public static int warnings;public static int w(String t,String m,Throwable e){warnings++;return 0;}}',
 'com/hiro/ulike/OpticalZoom.java':'package com.hiro.ulike;import java.util.concurrent.atomic.AtomicInteger;public class OpticalZoom {private static final AtomicInteger inFlight=new AtomicInteger();private static long stillUntil;public static void set(int n,long t){inFlight.set(n);stillUntil=t;}}',
 'com/hiro/ulike/CaptureYuv.java':'''package com.hiro.ulike;import java.util.*;public class CaptureYuv {private static final Map<Object,Object> states=new HashMap<>();static class State {private boolean delivering,selected;private Object pending,captureCallback;}static void reset(){states.clear();}static State add(Object k){State s=new State();states.put(k,s);return s;}static void set(State s,String name,boolean value)throws Exception {java.lang.reflect.Field f=s.getClass().getDeclaredField(name);f.setAccessible(true);f.set(s,value);}static void object(State s,String n,Object v)throws Exception{java.lang.reflect.Field f=s.getClass().getDeclaredField(n);f.setAccessible(true);f.set(s,v);}static void bad(){states.put("bad",new Object());}static void nullState(){states.put("null",null);}}''',
 'com/hiro/ulike/LensRelease163.java':'''package com.hiro.ulike;import java.util.*;public class LensRelease163 {private static final Object LOCK=new Object();private static final Map<Object,Object> callbacks=new HashMap<>(),nativeCallbacks=new HashMap<>(),deliveries=new HashMap<>(),stops=new HashMap<>();static void set(String n,boolean busy)throws Exception {java.lang.reflect.Field f=LensRelease163.class.getDeclaredField(n);f.setAccessible(true);Map<Object,Object> m=(Map<Object,Object>)f.get(null);synchronized(LOCK){m.clear();if(busy)m.put("job",new Object());}}static void reset(){callbacks.clear();nativeCallbacks.clear();deliveries.clear();stops.clear();}}''',
 'com/hiro/ulike/StyleStill4.java':'''package com.hiro.ulike;import java.util.*;public class StyleStill4 {private static final String TAG="ULikeStyleStill";private static final ThreadLocal<Object> armedRecorder=new ThreadLocal<>();private static final Map<Object,Object> strictRequests=new HashMap<>();static void configure(){strictRequests.put(new Object(),new Object());}}''',
 'com/hiro/ulike/BackExit185.java':'''package com.hiro.ulike;public class BackExit185 {ExitFlow185 flow;public static boolean cancelExit1921(BackExit185 c){return c.flow.cancelPending1921();}}''',
 'com/hiro/ulike/BackRoute1920.java':'''package com.hiro.ulike;public class BackRoute1920 {static int calls;static boolean consumed;public static boolean consume(BackExit185 c,android.app.Activity a){calls++;return consumed;}}''',
 'com/hiro/ulike/Tests1921.java':r'''package com.hiro.ulike;
import java.util.*;import android.os.*;import android.app.Activity;
public class Tests1921 {
 static int count;static List<String> results=new ArrayList<>();static void check(String name,boolean ok){if(!ok)throw new AssertionError(name);count++;results.add("PASS\t"+name);}
 static void reset(){OpticalZoom.set(0,0);CaptureYuv.reset();LensRelease163.reset();SystemClock.now=100;Looper.main=true;}
 static class Fake implements ExitFlow185.Port {
  long time=100,stamp;boolean capture,save,record,stopAccepted=true,sealAccepted=true,realProbe;
  int closes,kills,stops,seals,waits,cancels,schedules;long lastDelay;
  public long now(){return time;}public long failureStamp(){return stamp;}
  public boolean captureBusy(){return capture||(realProbe&&ExitBusy1921.captureBusy(closes>0));}
  public boolean saveBusy(){return save;}public boolean recording(){return record;}
  public boolean requestRecordStop(){stops++;return stopAccepted;}public boolean seal(){seals++;return sealAccepted&&!captureBusy()&&!save&&!record;}
  public void closeScreens(){closes++;}public void terminate(){if(captureBusy()||save||record)throw new AssertionError("unsafe termination");kills++;}
  public void waiting(){if(captureBusy()||save||record)waits++;}public void cancelled(){cancels++;}public void schedule(long delay){schedules++;lastDelay=delay;}
  void tick(ExitFlow185 flow,long at){time=at;flow.tick();}
  void finish(ExitFlow185 flow){time+=401;flow.tick();time+=100;flow.tick();time+=351;flow.tick();}
 }
 public static void main(String[] args)throws Exception {
  reset();boolean legacyBusy=false;try{StyleStill4.class.getDeclaredField("callbacks");}catch(ReflectiveOperationException e){legacyBusy=true;}
  check("baseline missing diagnostics field reproduces fail-closed busy (host model)",legacyBusy);
  check("fixed empty camera is not busy",!ExitBusy1921.captureBusy(false));
  StyleStill4.configure();check("persistent style configuration is not pending work",!ExitBusy1921.captureBusy(false));
  OpticalZoom.set(1,0);check("in-flight capture protected",ExitBusy1921.captureBusy(false));check("closed screen ignores retired lens front-end count only",!ExitBusy1921.captureBusy(true));
  OpticalZoom.set(0,101);check("capture deadline protected",ExitBusy1921.captureBusy(false));SystemClock.now=101;check("elapsed lens deadline is idle",!ExitBusy1921.captureBusy(false));reset();
  for(String name:new String[]{"delivering","selected"}){CaptureYuv.State s=CaptureYuv.add(name);CaptureYuv.set(s,name,true);check(name+" true protected",ExitBusy1921.captureBusy(false));CaptureYuv.set(s,name,false);check(name+" complete becomes idle",!ExitBusy1921.captureBusy(false));}
  for(String name:new String[]{"pending","captureCallback"}){CaptureYuv.State s=CaptureYuv.add(name);CaptureYuv.object(s,name,new Object());check(name+" nonnull protected",ExitBusy1921.captureBusy(false));check(name+" remains protected after screen close",ExitBusy1921.captureBusy(true));CaptureYuv.object(s,name,null);check(name+" cleared becomes idle",!ExitBusy1921.captureBusy(false));}
  CaptureYuv.nullState();check("null weak state ignored",!ExitBusy1921.captureBusy(false));
  for(String name:new String[]{"callbacks","nativeCallbacks","deliveries","stops"}){LensRelease163.set(name,true);check("lens "+name+" protected",ExitBusy1921.captureBusy(false));check("lens "+name+" protected after screens close",ExitBusy1921.captureBusy(true));LensRelease163.set(name,false);check("lens "+name+" completion releases wait",!ExitBusy1921.captureBusy(false));}
  CaptureYuv.bad();check("unexpected live state remains fail closed",ExitBusy1921.captureBusy(false));check("failed inspection logs once",android.util.Log.warnings==1);ExitBusy1921.captureBusy(false);check("no repeated warning flood",android.util.Log.warnings==1);reset();
  Fake p=new Fake();p.realProbe=true;ExitFlow185 f=new ExitFlow185(p);f.request();check("fixed idle no false saving toast",p.waits==0);check("idle first Back requests exit",f.requested());check("idle retains first quiet safety window",p.closes==0&&p.lastDelay==100);p.tick(f,499);check("cannot close before400ms quiet",p.closes==0);p.tick(f,500);check("idle closes screens once after400ms",p.closes==1&&p.kills==0);p.tick(f,600);p.tick(f,949);check("postclose quiet protects writes",p.kills==0);p.tick(f,950);check("idle seals and exits after both quiet windows",p.kills==1&&p.seals==1);f.tick();f.request();check("exit cannot terminate twice",p.kills==1&&p.closes==1);
  for(String busy:new String[]{"capture","save"}){p=new Fake();p.capture=busy.equals("capture");p.save=busy.equals("save");f=new ExitFlow185(p);f.request();p.tick(f,1000);check(busy+" actual busy prevents close/kill",p.closes==0&&p.kills==0&&p.waits==1);p.capture=p.save=false;p.tick(f,1100);p.finish(f);check(busy+" completion permits normal exit",p.kills==1&&p.closes==1);}
  p=new Fake();p.save=true;f=new ExitFlow185(p);f.request();p.tick(f,15099);check("wait remains pending just before15s",f.requested()&&p.closes==0);p.tick(f,15100);check("15s cancels wait without discarding save",!f.requested()&&p.save&&p.cancels==1&&p.kills==0&&p.closes==0&&p.seals==0);p.save=false;p.finish(f);check("old timer cannot close after timeout",p.closes==0&&p.kills==0);
  p=new Fake();p.save=true;f=new ExitFlow185(p);f.request();check("second Back cancels pending exit",f.cancelPending1921()&&!f.requested()&&p.save&&p.kills==0);check("duplicate cancellation harmless",!f.cancelPending1921()&&p.cancels==1);p.save=false;p.finish(f);check("cancelled timer cannot late-exit",p.kills==0);f.request();p.finish(f);check("new explicit Back after cancel can exit",p.kills==1);
  for(String method:new String[]{"dispatchTouchEvent","dispatchGenericMotionEvent","dispatchKeyShortcutEvent"}){p=new Fake();p.capture=true;f=new ExitFlow185(p);f.request();check(method+" releases input and cancels wait",!f.blockCallback1921(method)&&!f.requested()&&p.capture&&p.cancels==1);p.finish(f);check(method+" cancelled timers do not close",p.closes==0);}
  p=new Fake();p.save=true;f=new ExitFlow185(p);f.request();check("non-Back key cancels wait and propagates",!f.blockInput1921()&&!f.requested());
  p=new Fake();f=new ExitFlow185(p);f.request();for(String method:new String[]{"onWindowFocusChanged","onContentChanged","onWindowAttributesChanged","onAttachedToWindow"})check(method+" does not cancel pending exit",f.blockCallback1921(method)&&f.requested()&&p.cancels==0);p.tick(f,500);check("closing cannot be cancelled",!f.cancelPending1921()&&f.blockInput1921()&&p.cancels==0);p.tick(f,600);p.tick(f,950);check("lifecycle events do not prevent termination",p.kills==1);
  p=new Fake();p.record=true;f=new ExitFlow185(p);f.request();p.tick(f,1000);p.tick(f,2000);check("recording stop requested exactly once",p.stops==1&&p.kills==0&&p.closes==0);p.record=false;p.save=true;p.tick(f,2100);check("recording output flush protected",p.kills==0&&p.closes==0);p.save=false;p.tick(f,2200);p.finish(f);check("recording exits only after flush",p.kills==1);
  p=new Fake();p.record=true;p.stopAccepted=false;f=new ExitFlow185(p);f.request();check("rejected record stop cancels instead of killing",!f.requested()&&p.cancels==1&&p.kills==0&&p.record);
  p=new Fake();p.record=true;f=new ExitFlow185(p);f.request();p.tick(f,15100);check("record-stop hang cancels wait without forcing process death",!f.requested()&&p.kills==0&&p.record&&p.stops==1);
  p=new Fake();p.save=true;f=new ExitFlow185(p);f.request();p.stamp++;p.tick(f,200);check("writer failure stamp cancels exit",!f.requested()&&p.kills==0&&p.closes==0);
  p=new Fake();f=new ExitFlow185(p);f.request();p.tick(f,500);p.tick(f,600);p.sealAccepted=false;p.tick(f,950);check("failed seal never terminates",p.kills==0);p.save=true;p.tick(f,1100);check("late writer remains protected after close",p.kills==0);p.tick(f,90100);check("original postclose deadline never forces kill",p.kills==0&&!f.requested());
  p=new Fake();p.realProbe=true;CaptureYuv.bad();f=new ExitFlow185(p);f.request();p.tick(f,15100);check("unknown capture state bounded and operable",!f.requested()&&p.closes==0&&p.kills==0&&!f.blockInput1921());reset();
  BackExit185 controller=new BackExit185();p=new Fake();p.save=true;controller.flow=new ExitFlow185(p);controller.flow.request();BackRoute1920.calls=0;check("adapter second Back cancels before native routing",ExitBusy1921.route(controller,new Activity())&&!controller.flow.requested()&&BackRoute1920.calls==0);
  BackRoute1920.consumed=true;check("adapter preserves native panel consume",ExitBusy1921.route(controller,new Activity())&&BackRoute1920.calls==1);
  BackRoute1920.consumed=false;check("adapter bare camera delegates to original exit",!ExitBusy1921.route(controller,new Activity())&&BackRoute1920.calls==2);
  controller.flow.request();Looper.main=false;check("worker adapter does not mutate pending flow",!ExitBusy1921.route(controller,new Activity())&&controller.flow.requested());Looper.main=true;
  for(String line:results)System.out.println(line);System.out.println("PASS exit assertions="+count+"; production Java, mocked platform/states; no Android device execution");
 }
}'''
 }
 for name,text in files.items():p=src/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
 log=''
 for cmd in [['javac','--release','8','-encoding','UTF-8','-d',out,*sorted(src.rglob('*.java')),root/'ExitBusy1921.java',root/'ExitFlow185.java'],['java','-cp',out,'com.hiro.ulike.Tests1921']]:
  p=subprocess.run(list(map(str,cmd)),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True);log+=p.stdout;print(p.stdout,end='',flush=True);p.check_returncode()
 (work/'host-tests1921.txt').write_text(log)
 import re
 return int(re.search(r'exit assertions=(\d+)',log)[1])
