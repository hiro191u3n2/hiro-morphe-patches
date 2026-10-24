#!/usr/bin/env python3
"""Execute current UI/metadata helpers against targeted lifecycle regressions.

Android/SDK objects are host doubles. The production Java helper bodies are
compiled unchanged; this does not claim a Galaxy device camera execution.
"""
import argparse
import importlib.util
import json
from pathlib import Path
import subprocess


def module(path):
    spec = importlib.util.spec_from_file_location(path.stem, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


LAYOUT = r'''package com.hiro.ulike;
import com.bytedance.corecamera.ui.view.CameraShadeView;
import com.ss.android.vesdk.VEPreviewRadio;
public final class UiViewport1963Test {
 static int checks;
 static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static CameraShadeView fresh(){
  CameraShadeView v=new CameraShadeView();v.v=VEPreviewRadio.RADIO_FULL;
  v.c=v.measuredWidth=742;v.j=v.measuredHeight=1536;
  v.u=v.r=100;v.t=v.s=200;v.i();PreviewLayout1922.attached(v);return v;
 }
 public static void main(String[] args){
  CameraShadeView v=fresh();check(PreviewLayout1922.viewport()!=null,"valid initial viewport");
  int notifications=v.notifications;
  for(int i=0;i<20;i++){PreviewLayout1922.ready(v);PreviewLayout1922.reconcile(v);}
  check(v.notifications==notifications,"valid repeated layouts remain deduplicated");
  v.u=-1;PreviewLayout1922.afterLayout(v);
  check(PreviewLayout1922.viewport()==null,"invalid target clears the previous valid viewport");
  check(v.notifications==notifications,"invalid target never delivers old rectangle as new geometry");
  v.u=v.r=100;v.i();PreviewLayout1922.afterLayout(v);
  check(PreviewLayout1922.viewport()!=null,"valid repair republishes viewport");
  check(v.notifications==notifications+1,"repair hands off one valid current rectangle");
  v.measuredWidth=0;v.measuredHeight=0;PreviewLayout1922.afterLayout(v);
  check(PreviewLayout1922.viewport()==null,"temporarily unmeasured active view clears cache");
  v.measuredWidth=742;v.measuredHeight=1536;PreviewLayout1922.afterLayout(v);
  check(PreviewLayout1922.viewport()!=null,"next real measurement restores viewport");
  v.u=Integer.MAX_VALUE;v.t=Integer.MAX_VALUE;PreviewLayout1922.afterLayout(v);
  check(PreviewLayout1922.viewport()==null,"overflowing margin sums cannot publish negative extents");
  PreviewLayout1922.detached(v);
  System.out.println("UI_VIEWPORT1963_ASSERTIONS="+checks);
 }
}'''

TIMING = r'''package com.hiro.ulike;
public final class ProcessingTiming1947 {
 public static final int FUSION=0; public static int binds;
 public static final class Trace {boolean finished;}
 public static Trace begin(Object cb){return new Trace();}
 public static void associate(Object cb,long id){}
 public static void bind(Object key,Trace trace){binds++;}
 public static void transfer(Object a,Object b){}
 public static void skip(Trace trace,int phase){}
 public static void finish(Trace trace,boolean ok){trace.finished=true;}
 public static boolean finished1953(Trace trace){return trace.finished;}
}'''

METADATA = r'''package com.hiro.ulike;
import android.graphics.Bitmap;
import android.media.Image;
import android.hardware.camera2.*;
import com.ss.android.vesdk.*;
public final class UiMetadata1963Test {
 static int checks;
 static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 public static final class Owner {public Object x0;public CameraDevice j=new CameraDevice("front");
  public CameraCharacteristics a=new CameraCharacteristics();Owner(Object callback){x0=callback;
   a.put(CameraCharacteristics.LENS_FACING,CameraCharacteristics.LENS_FACING_FRONT);}}
 public static class DuringRead extends CaptureResult {
  Runnable action;DuringRead(long timestamp,Runnable action){this.action=action;
   put(SENSOR_TIMESTAMP,timestamp);put(SENSOR_SENSITIVITY,640);}
  @Override public <T>T get(Key<T> key){if(key==SENSOR_TIMESTAMP&&action!=null){
   Runnable task=action;action=null;task.run();}return super.get(key);}
 }
 static void start(Object renderer,Object callback){ShotContext1932.beginRecorder(renderer,callback);
  ShotContext1932.begin(null,callback);}
 static ShotContext1932.Snapshot deliver(Object callback){Bitmap bitmap=new Bitmap(400,300);
  ShotContext1932.bindDelivery(callback,new Object(),bitmap);return ShotContext1932.forBitmap(bitmap);}
 static void directRace(){
  final Object renderer=new Object(),callback=new Object();start(renderer,callback);
  Owner owner=new Owner(callback);
  ShotContext1932.received(owner,new Image(1001,400,300),new DuringRead(1001,new Runnable(){public void run(){start(renderer,callback);}}));
  ShotContext1932.Snapshot shot=deliver(callback);
  check(shot.shotId>0&&!shot.metadataReliable&&shot.sensorTimestampNanos==0,
   "late metadata cannot replace a newly reused direct callback's shot");
 }
 static void replacedOwnerRace(){
  Object renderer=new Object(),callback=new Object();start(renderer,callback);final Owner owner=new Owner(callback);
  ShotContext1932.received(owner,new Image(1002,400,300),new DuringRead(1002,new Runnable(){public void run(){owner.x0=new Object();}}));
  check(!deliver(callback).metadataReliable,"owner callback replacement rejects late metadata");
 }
 static void wrapperRace(boolean burst){
  final VERecorder renderer=new VERecorder();final Object callback=new Object();start(renderer,callback);
  final TECameraVideoRecorder$60 wrapper=new TECameraVideoRecorder$60(new VERecorder$13(renderer,callback));
  ShotContext1932.begin(null,wrapper);Owner owner=new Owner(wrapper);
  DuringRead result=new DuringRead(1003,new Runnable(){public void run(){start(renderer,callback);}});
  if(burst)check(!ShotContext1932.receivedValues1933(owner,wrapper,1003,400,300,result),
    "old burst wrapper cannot overwrite terminal callback reused during metadata read");
  else ShotContext1932.received(owner,new Image(1003,400,300),result);
  ShotContext1932.Snapshot shot=deliver(callback);
  check(shot.shotId>0&&!shot.metadataReliable,
   "old native wrapper cannot restore the previous shot over a new terminal callback");
 }
 public static void main(String[] args){
  directRace();replacedOwnerRace();wrapperRace(false);wrapperRace(true);
  Object callback=new Object();start(new Object(),callback);ShotContext1932.failed(callback,-1);
  int before=ProcessingTiming1947.binds;Bitmap failed=new Bitmap(400,300);
  ShotContext1932.bindDelivery(callback,new Object(),failed);
  check(ProcessingTiming1947.binds==before,"failed-shot delivery cannot bind image/timing owners");
  check(ShotContext1932.forBitmap(failed)==ShotContext1932.UNKNOWN,"failed-shot bitmap has no shot context");
  callback=new Object();start(new Object(),callback);Owner owner=new Owner(callback);
  ShotContext1932.received(owner,new Image(2001,400,300),new DuringRead(2001,null));
  ShotContext1932.Snapshot normal=deliver(callback);
  check(normal.metadataReliable&&normal.iso==640&&normal.sensorTimestampNanos==2001,
   "unchanged matched metadata is still accepted");
  System.out.println("UI_METADATA1963_ASSERTIONS="+checks);
 }
}'''

RENDER = r'''package com.hiro.ulike;
import android.os.*;
import com.ss.android.vesdk.VEListener;
import java.lang.reflect.Field;
import java.util.Map;
public final class UiRenderer1963Test {
 static int checks;
 static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static RenderStartup1938.Pending ticket(Object recorder)throws Exception{
  Field field=RenderStartup1938.class.getDeclaredField("pending");field.setAccessible(true);
  return (RenderStartup1938.Pending)((Map<?,?>)field.get(null)).get(recorder);
 }
 public static void main(String[] args)throws Exception{
  RenderTest1938.Recorder r=RenderTest1938.fresh();RenderTest1938.arm(r);
  RenderStartup1938.Replay replay=new RenderStartup1938.Replay(ticket(r));
  RenderStartup1938.cancelled(r);check(Handler.queued()==0,"cancel removes outstanding poll");
  int posts=OpticalZoom.MAIN.posts;replay.onDone(0);
  check(OpticalZoom.MAIN.posts==posts&&Handler.queued()==0,"late native completion cannot enqueue cancelled poll");
  RenderTest1938.arm(r);RenderStartup1938.Pending current=ticket(r);
  posts=OpticalZoom.MAIN.posts;int queued=Handler.queued();replay.onDone(0);
  check(ticket(r)==current&&OpticalZoom.MAIN.posts==posts&&Handler.queued()==queued,
   "old completion cannot poll or displace replacement ticket");
  new RenderStartup1938.Replay(current).onDone(0);
  check(OpticalZoom.MAIN.posts==posts+1&&Handler.queued()==queued,
   "current native completion preserves one coalesced poll");
  RenderStartup1938.cancelled(r);
  System.out.println("UI_RENDERER1963_ASSERTIONS="+checks);
 }
}'''


def compile_run(root, work, name, helpers, fixtures, injected, main, jdk=None):
    folder = work / name
    sources, classes = folder / 'src', folder / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    files = []
    for relative, contents in injected.items():
        target = sources / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(contents + '\n')
        files.append(target)
    for fixture in fixtures:
        files += sorted((root / fixture).rglob('*.java'))
    files += [root / helper for helper in helpers]
    javac = str(Path(jdk) / 'bin/javac') if jdk else 'javac'
    java = str(Path(jdk) / 'bin/java') if jdk else 'java'
    result = subprocess.run([javac, '-source', '8', '-target', '8', '-encoding', 'UTF-8',
                             '-d', str(classes), *map(str, files)],
                            capture_output=True, text=True, timeout=120)
    (folder / 'compile.log').write_text(result.stdout + result.stderr)
    if result.returncode:
        raise RuntimeError(result.stdout + result.stderr)
    result = subprocess.run([java, '-XX:ActiveProcessorCount=4', '-cp', str(classes), main],
                            capture_output=True, text=True, timeout=120)
    (folder / 'run.log').write_text(result.stdout + result.stderr)
    if result.returncode:
        raise RuntimeError(result.stdout + result.stderr)
    print(result.stdout, end='')
    return int(result.stdout.strip().split('=')[-1])


def test(root, work, jdk=None, ndk=None):
    root, work = Path(root).resolve(), Path(work).resolve()
    counts = {}
    counts['viewport'] = compile_run(root, work, 'ui-viewport', ['PreviewLayout1922.java'],
        ['layout1937-host'], {'com/hiro/ulike/UiViewport1963Test.java': LAYOUT},
        'com.hiro.ulike.UiViewport1963Test', jdk)
    stubs = dict(module(root / 'host_metadata1933.py').STUBS)
    stubs['com/hiro/ulike/ProcessingTiming1947.java'] = TIMING
    stubs['com/hiro/ulike/UiMetadata1963Test.java'] = METADATA
    counts['metadata'] = compile_run(root, work, 'ui-metadata', ['ShotContext1932.java'],
        [], stubs, 'com.hiro.ulike.UiMetadata1963Test', jdk)
    counts['renderer'] = compile_run(root, work, 'ui-renderer',
        ['RenderStartup1938.java', 'Scheduling1944.java', 'SpeedWorkers1935.java'],
        ['renderer1938-host'], {'com/hiro/ulike/UiRenderer1963Test.java': RENDER},
        'com.hiro.ulike.UiRenderer1963Test', jdk)
    result = {'status': 'passed', 'assertions': sum(counts.values()), 'groups': counts,
              'scope': 'current production Java bodies; deterministic Android/SDK doubles',
              'physical_android_tested': False}
    (work / 'ui-audit1963.json').write_text(json.dumps(result, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    args = parser.parse_args()
    print(json.dumps(test(args.root, args.work, args.jdk), indent=2))
