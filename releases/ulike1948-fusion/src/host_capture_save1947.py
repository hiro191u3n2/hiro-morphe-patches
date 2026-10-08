#!/usr/bin/env python3
"""Run retained capture/save contracts with diagnostic identity assertions.

The timing recorder is an assertion shim here; the real recorder is tested by
host_timing1947 separately. Camera, bitmap and codec contracts are the existing
scripted ABI tests, exercising the actual changed production adapters.
"""
from pathlib import Path
import importlib.util
import json
import os
import shutil

TIMING = r'''package com.hiro.ulike;
import java.util.*;
public final class ProcessingTiming1947 {
 public static final int FUSION=0,NOISE=1,CORRECTION=2,ENCODE=3,SAVE=4;
 public static long serial;public static int checks,completed,failed,stages;
 private static final Map<Object,Trace> MAP=Collections.synchronizedMap(new IdentityHashMap<Object,Trace>());
 private static final Map<Long,Trace> SHOTS=new HashMap<Long,Trace>();
 private static final ThreadLocal<Trace> CURRENT=new ThreadLocal<Trace>();
 public static final class Trace{public final long id;public long expected;public boolean finished;Trace(long n){id=n;}}
 public static final class Scope{final Trace previous,installed;final Thread thread;Scope(Trace p,Trace i){previous=p;installed=i;thread=Thread.currentThread();}}
 public static final class Token{final Trace trace;final int stage;boolean ended;Token(Trace t,int s){trace=t;stage=s;}}
 public static synchronized Trace begin(Object key){Trace t=new Trace(++serial);if(key!=null)MAP.put(key,t);return t;}
 public static synchronized Trace associate(Object key,long id){Trace t=MAP.get(key);if(t==null)t=SHOTS.get(id);if(t==null)t=begin(key);t.expected=id;SHOTS.put(id,t);if(key!=null)MAP.put(key,t);return t;}
 public static Trace forKey(Object key){return MAP.get(key);}
 public static synchronized Trace forShot(long id){return SHOTS.get(id);}
 public static Trace traceFor(Object key){Trace t=MAP.get(key);return t!=null?t:CURRENT.get();}
 public static void bind(Object key,Trace trace){if(key!=null){if(trace==null)MAP.remove(key);else MAP.put(key,trace);}}
 public static void transfer(Object from,Object to){if(from!=to)bind(to,MAP.get(from));}
 public static Scope enter(Trace trace){Scope s=new Scope(CURRENT.get(),trace);if(trace==null)CURRENT.remove();else CURRENT.set(trace);return s;}
 public static void restore(Scope s){if(s==null)return;check(s.thread==Thread.currentThread(),"scope restored on wrong thread");check(CURRENT.get()==s.installed,"nested/current trace leaked");if(s.previous==null)CURRENT.remove();else CURRENT.set(s.previous);}
 public static Token beginStage(Trace trace,int stage){if(trace!=null){check(CURRENT.get()==trace,"stage must use exact worker scope");synchronized(ProcessingTiming1947.class){stages++;}}return new Token(trace,stage);}
 public static void end(Token t){if(t!=null){check(!t.ended,"stage ended twice");t.ended=true;}}
 public static void skip(Trace t,int stage){}
 public static void finish(Trace t,boolean ok){if(t==null)return;synchronized(ProcessingTiming1947.class){check(!t.finished,"trace finished twice");t.finished=true;completed++;if(!ok)failed++;}}
 public static void settings(Trace t,String text){}
 public static void output(Trace t,int w,int h,String text){}
 public static void assertCurrent(Object key,long id){Trace t=MAP.get(key);check(t!=null&&t==CURRENT.get(),"bitmap/job/codec trace identity mismatch");check(t.expected==id,"another photo's diagnostic attached");}
 public static synchronized void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
}'''


def module(name, path):
    spec=importlib.util.spec_from_file_location(name,path)
    value=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def test(root, work):
    root=Path(root).resolve()
    work=Path(work).resolve()/'host-capture-save1947'
    view=work/'source-view'
    view.mkdir(parents=True,exist_ok=True)
    if not shutil.which('javac'):
        shim=work/'bin/javac'
        shim.parent.mkdir(parents=True,exist_ok=True)
        shim.write_text('#!/bin/sh\nexec java com.sun.tools.javac.Main "$@"\n')
        shim.chmod(0o755)
        os.environ['PATH']=str(shim.parent)+os.pathsep+os.environ.get('PATH','')
    for path in root.iterdir():
        target=view/path.name
        if not target.exists():target.symlink_to(path,target_is_directory=path.is_dir())
    for name in ('host_save_baseline1935.py','host_save1945.py','host_capture1943.py'):
        path=view/name
        if path.is_symlink():path.unlink()
        path.write_text((root/name).read_text())
    timing=view/'diagnostic-fixture/com/hiro/ulike/ProcessingTiming1947.java'
    timing.parent.mkdir(parents=True,exist_ok=True)
    timing.write_text(TIMING)

    # Keep source image, owned copy and watermark destination in one diagnostic.
    base=view/'host_save_baseline1935.py'
    text=base.read_text()
    text += '\nFIXTURES["com/hiro/ulike/ProcessingTiming1947.java"] = '+repr(TIMING)+'\n'
    text += '''
FIXTURES['com/hiro/ulike/ShotContext1932.java']=FIXTURES['com/hiro/ulike/ShotContext1932.java'].replace(
    'map.put(b,forBitmap(a));','map.put(b,forBitmap(a));ProcessingTiming1947.transfer(a,b);').replace(
    'map.put(b,new Snapshot(id));','map.put(b,new Snapshot(id));ProcessingTiming1947.begin(b);ProcessingTiming1947.associate(b,id);')
'''
    base.write_text(text)
    save=view/'host_save1945.py'
    text=save.read_text().replace(
        'Photo p=new Photo(image,rotation,direction);',
        'Photo p=new Photo(image,rotation,direction);ProcessingTiming1947.assertCurrent(image,p.id);')
    text=text.replace(
        '()->{try{p.writer=CodecDrain1945.build(',
        '()->{try{ProcessingTiming1947.assertCurrent(p.image,p.id);p.writer=CodecDrain1945.build(')
    # A completed success, encoder failure and correction failure all close the
    # exact trace, and the reusable codec producer never inherits another photo.
    text=text.replace(
        'System.out.println("PASS save1945 assertions="+assertions+" scenarios=28 device_tested=false");',
        'check(ProcessingTiming1947.completed>=19&&ProcessingTiming1947.failed>=4,"success and failure diagnostics finish independently: "+ProcessingTiming1947.completed+"/"+ProcessingTiming1947.failed);'
        'check(ProcessingTiming1947.checks>80,"diagnostic worker and codec scope checks ran");'
        'System.out.println("PASS save1945 assertions="+(assertions+ProcessingTiming1947.checks)+" scenarios=28 device_tested=false");')
    save.write_text(text)
    saved=module('timed_save1947',save).test(view,work)

    # Scripted callback owner identities change/reuse just as native callbacks
    # do. Start a fresh trace at each scripted accepted ordinary shutter.
    capture=view/'host_capture1943.py'
    text=capture.read_text().replace(
        "inputs+=[root/'tests/Capture1943Test.java'",
        "inputs+=[root/'diagnostic-fixture/com/hiro/ulike/ProcessingTiming1947.java',root/'tests/Capture1943Test.java'")
    capture.write_text(text)
    tests=view/'tests'
    if tests.is_symlink():tests.unlink()
    tests.mkdir(exist_ok=True)
    for path in (root/'tests').iterdir():
        target=tests/path.name
        if not target.exists():target.symlink_to(path,target_is_directory=path.is_dir())
    fixture=tests/'Capture1943Test.java'
    if fixture.is_symlink():fixture.unlink()
    text=(root/'tests/Capture1943Test.java').read_text().replace(
        'BurstCapture1933.canceled(o);\n            targetDelivery=',
        'BurstCapture1933.canceled(o);\n            ProcessingTiming1947.begin(callback);\n            targetDelivery=')
    text=text.replace('CAPTURE_1943_PASS assertions="+assertions',
                      'CAPTURE_1943_PASS assertions="+(assertions+ProcessingTiming1947.checks)')
    fixture.write_text(text)
    captured=module('timed_capture1947',capture).test(view,work)
    metadata_module=module('timed_metadata1947',root/'host_metadata1933.py')
    metadata_module.STUBS['com/hiro/ulike/ProcessingTiming1947.java']=TIMING
    metadata_module.TEST=metadata_module.TEST.replace(
        'ShotContext1932.bindDelivery(cb,new Object(),b);return b;',
        'ShotContext1932.bindDelivery(cb,new Object(),b);'
        'ShotContext1932.Snapshot s=ShotContext1932.forBitmap(b);'
        'if(s.shotId>0)check(ProcessingTiming1947.forKey(b)==ProcessingTiming1947.forShot(s.shotId),"delivered bitmap belongs to the exact shot trace");return b;')
    metadata=metadata_module.test(view,work)
    result={'status':'passed','suite':'capture-save1947','save':saved,'capture':captured,'metadata':metadata,
            'scope':'production adapters with diagnostic assertion recorder and scripted Android ABI; real recorder separately tested',
            'trace_copy_and_watermark_identity':True,'queued_job_and_codec_executor_scope':True,
            'fusion_probe_and_kernel_scope':True,'success_and_failure_completion':True,
            'camera_and_file_ownership_unchanged':True,'device_tested':False}
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    return result


if __name__=='__main__':
    import sys
    print(json.dumps(test(Path(__file__).parent,Path(sys.argv[1])),ensure_ascii=False,indent=2))
