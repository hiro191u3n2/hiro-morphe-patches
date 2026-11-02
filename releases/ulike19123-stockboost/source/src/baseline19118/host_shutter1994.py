#!/usr/bin/env python3
"""Host lifecycle checks for the real shutter timing helper; no device latency claim."""
from pathlib import Path
import argparse, hashlib, json, subprocess

MOTION_EVENT = '''package android.view;
public final class MotionEvent {
 public static final int ACTION_DOWN=0; private final int action; public boolean fail;
 public MotionEvent(int action){this.action=action;}
 public int getAction(){if(fail)throw new IllegalStateException("unreadable event");return action;}
}'''

def test(source, work, jdk=None, ndk=None):
    source, work, jdk = Path(source), Path(work), Path(jdk)
    classes = work/'classes'; classes.mkdir(parents=True, exist_ok=True)
    event = work/'fixtures/android/view/MotionEvent.java'
    event.parent.mkdir(parents=True, exist_ok=True); event.write_text(MOTION_EVENT)
    def run(command, name):
        process = subprocess.run([str(x) for x in command], capture_output=True, text=True, timeout=60)
        (work/name).write_text(process.stdout + process.stderr)
        if process.returncode: raise RuntimeError(process.stderr[-6000:])
        return process.stdout
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,
         event,source/'ShutterTiming1994.java',source/'tests1994/shutter/ShutterTiming1994Test.java'], 'compile.log')
    output = run([jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.ShutterTiming1994Test'], 'test.log')
    result = json.loads(output.split('RESULT ',1)[1])
    result.update(physical_android_tested=False, device_speedup_verified=False,
                  production_source_sha256=hashlib.sha256((source/'ShutterTiming1994.java').read_bytes()).hexdigest())
    (work/'result.json').write_text(json.dumps(result, indent=2)+'\n')
    return result

if __name__ == '__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--source', required=True); parser.add_argument('--work', required=True)
    parser.add_argument('--jdk', required=True); parser.add_argument('--ndk')
    args=parser.parse_args()
    print(json.dumps(test(args.source,args.work,args.jdk,args.ndk)))
