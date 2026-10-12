#!/usr/bin/env python3
"""Regression evidence for accepted front previews that still await their first image.

Runs the actual old and new helpers with deterministic native camera queues.
The old source must reproduce each reported defect. SDK doubles model ordering
and ownership only: no Android camera, pixels, or real-device speed is tested.
"""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess


HARNESS = r'''package com.hiro.ulike;
import android.os.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import com.ss.android.vesdk.VECameraCapture;
import i.s.a.w.q;
public final class CameraAudit1980Test {
    static int checks;
    static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    static boolean trace(String needle){for(String row:CameraTrace1965.rows)if(row.contains(needle))return true;return false;}
    static int traces(String needle){int count=0;for(String row:CameraTrace1965.rows)if(row.contains(needle))count++;return count;}
    static Object ticket()throws Exception {
        Field f=FrontPreview1936.class.getDeclaredField("pending");f.setAccessible(true);
        return ((Map<?,?>)f.get(null)).get(FrontTest1936.capture);
    }
    static long deadline()throws Exception {
        Object ticket=ticket();check(ticket!=null,"test request owns a pending input observation");
        Field f=ticket.getClass().getDeclaredField("deadline");f.setAccessible(true);return f.getLong(ticket);
    }
    static void reset(boolean ready){
        FrontTest1936.reset(false,false);if(ready)FrontTest1936.ready();
        FrontTest1936.capture.autoPixels=false;CameraTrace1965.rows.clear();
    }
    static void exhaustAcceptedRetries()throws Exception {
        reset(false);check(FrontTest1936.begin()==-100,"missing native inputs preserve native rejection");
        FrontTest1936.ready();
        FrontTest1936.capture.during=new Runnable(){public void run(){
            if(FrontTest1936.capture.starts==2){FrontTest1936.capture.p.set(false);FrontTest1936.capture.during=null;}
        }};
        Handler.until(210L);check(FrontTest1936.capture.starts==2&&FrontTest1936.capture.accepted==0,"first replay loses initialization before native guard");
        FrontTest1936.capture.p.set(true);Handler.until(310L);
        check(FrontTest1936.capture.starts==3&&FrontTest1936.capture.accepted==1,"last allowed replay accepted an asynchronous native preview");
        Handler.until(450L);
    }
    static void budget()throws Exception {
        exhaustAcceptedRetries();
        check(ticket()!=null,"BUG1980_BUDGET: accepted last replay must retain first-image observation");
        long until=deadline();FrontPreview1936.pixel(FrontTest1936.capture.latestProvider,new Object());
        check(trace("front_preview_frame "),"late matching frame is recorded after replay budget is spent");
        Handler.until(until+100L);
        check(FrontTest1936.capture.starts==3&&q.INSTANCE.stops==0,"observation does not grant extra native replay or stop");
        check(ticket()==null&&Handler.queued()==0&&!trace("ANOMALY input_preview_timeout"),"matching late frame cancels the original timeout and all work");
    }
    static void window()throws Exception {
        reset(true);check(FrontTest1936.begin()==0,"initial native preview accepted");
        long until=deadline();Handler.until(until-1100L);
        check(q.INSTANCE.stops==1&&FrontTest1936.capture.starts==2,"one stopped-session recovery remains the unchanged maximum");
        check(ticket()!=null,"BUG1980_WINDOW: insufficient restart margin must not discard an accepted first-image wait");
        int nativePosts=q.INSTANCE.mHandler.posts;int observations=traces("front_preview_wait ");
        Handler.until(until-100L);
        check(q.INSTANCE.mHandler.posts==nativePosts,"frame-only waiting does not poll the native camera handler");
        check(traces("front_preview_wait ")==observations,"frame-only waiting does not repeat trace work");
        FrontPreview1936.pixel(FrontTest1936.capture.latestProvider,new Object());
        check(trace("front_preview_frame "),"frame near original deadline still completes observation");
        Handler.until(until+100L);
        check(ticket()==null&&Handler.queued()==0&&!trace("ANOMALY input_preview_timeout"),"late frame succeeds without a false timeout or renewed deadline");
    }
    static void timeout()throws Exception {
        reset(true);FrontTest1936.begin();long until=deadline();Handler.until(until+100L);
        check(traces("ANOMALY input_preview_timeout")==1,"BUG1980_TIMEOUT: no first image freezes exactly one real owned timeout");
        check(FrontTest1936.capture.starts==2&&q.INSTANCE.stops==1,"timeout does not start a second native recovery");
        check(ticket()==null&&Handler.queued()==0,"timed out observation retires all callbacks");
        FrontPreview1936.pipelines(FrontTest1936.capture);Handler.until(until+1000L);
        check(traces("ANOMALY input_preview_timeout")==1&&FrontTest1936.capture.starts==2,"late pipelines cannot rearm spent observation");
        exhaustAcceptedRetries();until=deadline();Handler.until(until+100L);
        check(traces("ANOMALY input_preview_timeout")==1,"last accepted replay also records its original frame timeout");
        check(FrontTest1936.capture.starts==3&&q.INSTANCE.stops==0&&Handler.queued()==0,"exhausted retry budget never gains another native operation");
    }
    static void cancellation()throws Exception {
        for(int change=0;change<8;change++){
            reset(true);FrontTest1936.begin();long until=deadline();Handler.until(until-1100L);
            check(ticket()!=null,"accepted observation survives until cancellation case");
            switch(change){
                case 0:FrontPreview1936.cancel(FrontTest1936.capture);break;
                case 1:ManualLens170.f.put("foreground",false);break;
                case 2:ManualLens170.f.put("epoch",8L);break;
                case 3:ManualLens170.f.put("capture",new WeakReference<Object>(new VECameraCapture()));break;
                case 4:q.INSTANCE.mCameraClient=new i.s.a.w.k();break;
                case 5:q.INSTANCE.mCameraSettings=new com.ss.android.ttvecamera.TECameraSettings();break;
                case 6:FrontTest1936.camera2.K=new android.hardware.camera2.CameraDevice();break;
                default:q.INSTANCE.mHandler=new Handler();
            }
            // A new native handler with the same live provider may legitimately
            // produce an image. Here it produces none: only timeout ownership is
            // under test, without inventing a stale frame on the replacement.
            if(change!=7)FrontPreview1936.pixel(FrontTest1936.capture.latestProvider,new Object());
            Handler.until(until+100L);
            check(!trace("front_preview_frame ")&&!trace("ANOMALY input_preview_timeout"),"stale/background observation cannot certify frame or report owned failure "+change);
            check(FrontTest1936.capture.starts==2&&q.INSTANCE.stops==1&&Handler.queued()==0,"stale observation cannot restart or retain callbacks "+change);
        }
    }
    static void lateBusy()throws Exception {
        reset(true);FrontTest1936.begin();long until=deadline();ExitBusy1921.busy=true;
        Handler.until(until-1000L);ExitBusy1921.busy=false;Handler.until(until-500L);
        check(q.INSTANCE.stops==0&&FrontTest1936.capture.starts==1,"too little safe stop margin prevents native recovery");
        check(ticket()!=null,"accepted original request remains observed in unsafe-stop margin");
        FrontPreview1936.pixel(FrontTest1936.capture.latestProvider,new Object());Handler.until(until+100L);
        check(trace("front_preview_frame ")&&!trace("ANOMALY input_preview_timeout")&&Handler.queued()==0,"late original camera frame completes without unnecessary restart");
    }
    public static void main(String[] args)throws Exception {
        if(args.length>0){if(args[0].equals("budget"))budget();else if(args[0].equals("window"))window();else if(args[0].equals("timeout"))timeout();else throw new AssertionError("unknown proof");}
        else {budget();window();timeout();cancellation();lateBusy();}
        System.out.println("CAMERA_AUDIT1980_ASSERTIONS="+checks);
    }
}
'''


def _run(command, log, timeout=90):
    r = subprocess.run(list(map(str, command)), text=True, capture_output=True, timeout=timeout)
    Path(log).write_text(r.stdout + r.stderr)
    return r


def _compile(source, work, front, javac):
    src, classes = work / 'src', work / 'classes'
    src.mkdir(parents=True, exist_ok=True); classes.mkdir(parents=True, exist_ok=True)
    for name in ('front1936-host', 'front1968-host', 'front1971-host'):
        for p in sorted((source / name).rglob('*.java')):
            out = src / p.relative_to(source / name)
            out.parent.mkdir(parents=True, exist_ok=True); shutil.copyfile(p, out)
    java_dir = src / 'com/hiro/ulike'
    for name in ('PreviewStart1927', 'PreviewInputs1929', 'ProviderLifecycle1929', 'FrontPreview1931'):
        shutil.copyfile(source / 'front1936-baseline' / (name + '.java'), java_dir / (name + '.java'))
    for name in ('CameraSession1965', 'Scheduling1944', 'SpeedWorkers1935'):
        shutil.copyfile(source / (name + '.java'), java_dir / (name + '.java'))
    shutil.copyfile(front, java_dir / 'FrontPreview1936.java')
    (java_dir / 'CameraAudit1980Test.java').write_text(HARNESS)
    r = _run([javac, '--release', '8', '-encoding', 'UTF-8', '-d', classes, *sorted(src.rglob('*.java'))], work / 'compile.log')
    if r.returncode: raise RuntimeError('camera audit compile failed\n' + r.stdout + r.stderr)
    return classes


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve(); work.mkdir(parents=True, exist_ok=True)
    javac = Path(jdk) / 'bin/javac' if jdk else shutil.which('javac')
    java = Path(jdk) / 'bin/java' if jdk else shutil.which('java')
    if not javac or not java: raise RuntimeError('JDK required')
    baseline = source / 'tests1980/camera1979-reference/FrontPreview1936.java'
    baseline_sha = hashlib.sha256(baseline.read_bytes()).hexdigest()
    if baseline_sha != 'd0a37c32cd9ac708bf39dab153cfe9c77a307c5c850241e4664e4a16eece22bf':
        raise RuntimeError('Exact released 1.9.79 front observer reference changed')
    old = _compile(source, work / 'baseline1979', baseline, javac)
    reproduced = {}
    for name in ('budget', 'window', 'timeout'):
        r = _run([java, '-ea', '-cp', old, 'com.hiro.ulike.CameraAudit1980Test', name], work / ('baseline-' + name + '.log'))
        marker = 'BUG1980_' + name.upper() + ':'
        if r.returncode == 0 or marker not in r.stderr: raise RuntimeError('reported old-source defect did not reproduce: ' + name + '\n' + r.stdout + r.stderr)
        reproduced[name] = True
    classes = _compile(source, work / 'current1980', source / 'FrontPreview1936.java', javac)
    r = _run([java, '-ea', '-cp', classes, 'com.hiro.ulike.CameraAudit1980Test'], work / 'current1980.log')
    if r.returncode: raise RuntimeError('camera audit failed\n' + r.stdout + r.stderr)
    count = int(r.stdout.strip().split('=')[-1])
    report = {'status': 'passed', 'assertions': count, 'physical_android_tested': False,
              'pre_fix_defects_reproduced': reproduced, 'native_restart_limits_preserved': True,
              'original_input_deadline_preserved': True, 'frame_only_wait_skips_native_polling': True,
              'production_source_sha256': {n: hashlib.sha256((source/n).read_bytes()).hexdigest() for n in ('FrontPreview1936.java', 'CameraSession1965.java')},
              'reference_source_sha256': baseline_sha,
              'scope': 'actual production helper with controlled Android/native camera queues; no physical preview or device speed claim'}
    (work / 'host-audit-camera1980.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


if __name__ == '__main__':
    import argparse
    p = argparse.ArgumentParser(); p.add_argument('--source', type=Path, default=Path(__file__).resolve().parent)
    p.add_argument('--work', type=Path, required=True); p.add_argument('--jdk', type=Path); a = p.parse_args()
    print(json.dumps(test(a.source, a.work, jdk=a.jdk), indent=2))
