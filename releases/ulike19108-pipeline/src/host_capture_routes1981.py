#!/usr/bin/env python3
"""Execute .81 capture/storage/memory/analysis ownership with scripted Android ABI.

Only Android/SDK calls are scripted. Queue, constructor state, analysis ownership,
admission arithmetic, reflection cache and worker CPU permits use production code.
The Strong model allocation formulas are extracted verbatim from production.
"""
from pathlib import Path
import hashlib
import importlib.util
import json
import re
import shutil
import subprocess

REFERENCE_ASYNC_SHA = '7d0ea0908596ddec641266261f61485aedb1d699758cfa9489eea84a88fd17ae'
REFERENCE_QUEUE_SHA = 'e96b714d119f2486d343a1a37e0deacb0096286aa00c7823eb1697108895dd37'


def module(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


def method(source, name):
    match = re.search(r'(?m)^\s*(?:public|private) static [^\n{;]+\b' + re.escape(name) + r'\([^\n{]*\)\s*\{', source)
    if not match:
        raise AssertionError('Missing unchanged memory estimator method: ' + name)
    start = match.start()
    position = source.index('{', match.start()) + 1
    depth = 1
    while depth:
        if source[position] == '{':
            depth += 1
        elif source[position] == '}':
            depth -= 1
        position += 1
    return source[start:position].strip()


def fixtures(source):
    values = dict(module(source / 'host_save_baseline1935.py', 'baseline_capture1981').FIXTURES)
    values['com/hiro/ulike/SaveHarness1935.java'] = (source / 'pipeline80-fixtures/SaveHarness1935.java').read_text()
    values['android/graphics/ColorSpace.java'] = '''package android.graphics;public final class ColorSpace{public final boolean srgb;public ColorSpace(boolean s){srgb=s;}public boolean isSrgb(){return srgb;}}'''
    values['android/graphics/Bitmap.java'] = '''package android.graphics;
public class Bitmap {
 public enum Config{ARGB_8888,HARDWARE,RGB_565,RGBA_F16}
 public static volatile boolean failCopy;
 public int[] pixels;public int width=2,height=2,density=160,generation=1;public boolean alpha=true,premultiplied=true,gainmap,recycled;
 public Config config=Config.ARGB_8888;public ColorSpace space;public int recycleCalls;
 public Bitmap(int[] p){pixels=p;}
 public int getWidth(){return width;}public int getHeight(){return height;}public Config getConfig(){return config;}
 public int getAllocationByteCount(){return width*height*(config==Config.RGBA_F16?8:config==Config.RGB_565?2:4);}
 public int getGenerationId(){return generation;}public int getDensity(){return density;}public boolean hasAlpha(){return alpha;}
 public boolean isPremultiplied(){return premultiplied;}public ColorSpace getColorSpace(){return space;}public boolean hasGainmap(){return gainmap;}
 public Bitmap copy(Config c,boolean m){if(failCopy)throw new OutOfMemoryError("scripted copy");Bitmap b=new Bitmap(pixels.clone());b.width=width;b.height=height;b.density=density;b.alpha=alpha;b.premultiplied=premultiplied;b.config=c;b.space=space;b.gainmap=gainmap;return b;}
 public void getPixels(int[] out,int offset,int stride,int x,int y,int w,int h){if(recycled)throw new IllegalStateException("read after recycle");for(int r=0;r<h;r++)for(int c=0;c<w;c++)out[offset+r*stride+c]=pixels[(y+r)*width+x+c];}
 public void recycle(){recycleCalls++;recycled=true;}public boolean isRecycled(){return recycled;}
}'''
    values['android/os/Build.java'] = '''package android.os;public final class Build{public static final class VERSION{public static int SDK_INT=34;}}'''
    values['android/app/ActivityManager.java'] = '''package android.app;public class ActivityManager{public static long available=2L*1024*1024*1024,threshold=64L*1024*1024;public static boolean low,fail;public static class MemoryInfo{public long availMem,threshold;public boolean lowMemory;}public void getMemoryInfo(MemoryInfo m){if(fail)throw new IllegalStateException("memory info unavailable");m.availMem=available;m.threshold=threshold;m.lowMemory=low;}}'''
    values['com/hiro/ulike/HostRouter1981.java'] = '''package com.hiro.ulike;import android.graphics.Bitmap;public final class HostRouter1981{public interface Encoder{String run(i.o.a.q.c.c.b.e controller,Bitmap b,int rotation,int direction)throws Exception;}public static volatile Encoder encoder;public static String encode(i.o.a.q.c.c.b.e c,Bitmap b,int r,int d)throws Exception{return encoder==null?SaveHarness1935.encode(c,b,r,d):encoder.run(c,b,r,d);}}'''
    values['com/hiro/ulike/WarmProbe1981.java'] = '''package com.hiro.ulike;public final class WarmProbe1981{public static volatile int initialized,constructed;}'''
    values['i/o/a/q/c/c/b/e.java'] = values['i/o/a/q/c/c/b/e.java'].replace('public class e{', 'public class e{static{com.hiro.ulike.WarmProbe1981.initialized++;}').replace('public e(c listener){', 'public e(c listener){com.hiro.ulike.WarmProbe1981.constructed++;').replace('com.hiro.ulike.SaveHarness1935.encode', 'com.hiro.ulike.HostRouter1981.encode')
    values['com/hiro/ulike/PhotoDetail.java'] = '''package com.hiro.ulike;public class PhotoDetail{
 public static volatile Settings live=new Settings(1);public static int snapshots;
 public static class Settings{public final int noiseLevel,sharpLevel;public final boolean noiseOn,sharpOn,texturePriority,haloSuppression,shadowPriority;
 public Settings(int n){this(true,n,true,n,true,true,true);}public Settings(boolean no,int nl,boolean so,int sl,boolean t,boolean h,boolean s){noiseOn=no;noiseLevel=nl;sharpOn=so;sharpLevel=sl;texturePriority=t;haloSuppression=h;shadowPriority=s;}}
 public static Settings snapshot1932(){snapshots++;if(SaveHarness1935.failSnapshot){SaveHarness1935.failSnapshot=false;throw SaveHarness1935.failure();}return live;}}
'''
    values['com/hiro/ulike/SaveQuality2.java'] = '''package com.hiro.ulike;public class SaveQuality2{
 public static volatile boolean live=true,systemInfo,forceFixed;public static volatile int failures;
 public static final class App{public Object getSystemService(String name){return new android.app.ActivityManager();}}
 public static Object app186(){return systemInfo?new App():null;}
 public static int[] output186(int w,int h,int r,boolean fixed){if(fixed&&forceFixed)return new int[]{5712,4284};return r==90||r==270?new int[]{h,w}:new int[]{w,h};}
 public static boolean isFixed245Enabled(){return AsyncSave1935.fixed(live);}public static void showSaveFailure(){failures++;}}
'''
    values['com/hiro/ulike/ProcessingTiming1947.java'] = module(source / 'host_capture_save1947.py', 'timing_capture1981').TIMING.replace(
        'public static Trace forKey(Object key){return MAP.get(key);}',
        'public static Trace forKey(Object key){if(SaveHarness1935.failTimingLookup){SaveHarness1935.failTimingLookup=false;throw new NoSuchMethodError("scripted timing lookup resolution");}return MAP.get(key);}'
    ).replace('public static void finish(Trace t,boolean ok){', 'public static void finish(Trace t,boolean ok){if(SaveHarness1935.failTimingFinish){SaveHarness1935.failTimingFinish=false;SaveHarness1935.failedTimingFinishes++;throw new NoSuchMethodError("scripted timing finish resolution");}')
    values['com/hiro/ulike/WholeRoute1953.java'] = '''package com.hiro.ulike;public class WholeRoute1953{public static long retainedBytes(){return 0;}public static void foregroundStarted(){}public static void wake(){}}'''
    values['com/hiro/ulike/GpuFinish1953.java'] = '''package com.hiro.ulike;public class GpuFinish1953{public static long retainedBytes(){if(SaveHarness1935.failAfterSubmit&&SaveQueue1935.nativeBytes()>0){SaveHarness1935.failAfterSubmit=false;throw new NoSuchMethodError("scripted queued helper");}return 0;}}'''
    values['com/hiro/ulike/QualityPipeline1932.java'] = '''package com.hiro.ulike;import android.graphics.Bitmap;public class QualityPipeline1932{public static volatile Bitmap leased;public static volatile boolean fail;public static void recycle1954(Bitmap b){if(fail)throw new NoSuchMethodError("scripted recycle linkage");if(b!=null&&b!=leased&&!b.isRecycled())b.recycle();}}'''
    values['com/hiro/ulike/CodecDrain1945.java'] = '''package com.hiro.ulike;public class CodecDrain1945{public static void awaitClosed(){}}'''
    values['com/hiro/ulike/ShotContext1932.java'] = values['com/hiro/ulike/ShotContext1932.java'].replace('public static Snapshot forBitmap(', 'public static boolean idle1953(){return true;}public static Snapshot forBitmap(').replace(
        'map.put(b,forBitmap(a));', 'SaveHarness1935.lastOwned=b;if(SaveHarness1935.failTransfer){SaveHarness1935.failTransfer=false;throw SaveHarness1935.failure();}map.put(b,forBitmap(a));ProcessingTiming1947.transfer(a,b);').replace(
        'map.put(b,new Snapshot(id));', 'map.put(b,new Snapshot(id));ProcessingTiming1947.begin(b);ProcessingTiming1947.associate(b,id);')
    values['com/hiro/ulike/FaceRegions1934.java'] = '''package com.hiro.ulike;import android.graphics.Bitmap;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public final class FaceRegions1934{public static volatile int found=1;public static final AtomicInteger calls=new AtomicInteger();public static volatile CountDownLatch entered,release;public static volatile boolean fail;
 public static final class Mask{public final int value;public Mask(int v){value=v;}}
 public static Mask forBitmap(Bitmap b,int rotation){calls.incrementAndGet();if(entered!=null)entered.countDown();if(release!=null)try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}if(b.isRecycled())throw new AssertionError("analysis read after recycle");if(fail)return new Mask(-1);if(found<=0||found>8){FaceResult1981.completedEmpty1981(found);return new Mask(0);}Mask mask=new Mask(b.pixels[0]+rotation);FaceResult1981.completed1981();return mask;}}
'''
    values['com/hiro/ulike/SpatialNoise1934.java'] = '''package com.hiro.ulike;public final class SpatialNoise1934{public interface Patches{void read(int[] p,int x,int y,int w,int h);}public final int value;public static int calls;SpatialNoise1934(int v){value=v;}public static SpatialNoise1934 probe(Patches src,int w,int h){calls++;int[] p=new int[w*h];src.read(p,0,0,w,h);return new SpatialNoise1934(p[0]);}}'''
    strong = (source / 'StrongNoise1958.java').read_text()
    constants = '\n'.join(re.findall(r'(?:public|private) static final int (?:HALO|PREP_ROWS|REGION)\s*=\s*\d+;', strong))
    names = ('modelMemoryBytes', 'workspaceBytes', 'preparationWorkspaceBytes', 'half', 'geometry', 'pixels')
    methods = {name: method(strong, name) for name in names}
    values['com/hiro/ulike/StrongNoise1958.java'] = 'package com.hiro.ulike;public final class StrongNoise1958{' + constants + '\n' + '\n'.join(methods.values()) + '}\n'
    return values, {name: hashlib.sha256(body.encode()).hexdigest() for name, body in methods.items()}


def test(source, work, jdk=None, ndk=None):
    source = Path(source).resolve()
    work = Path(work).resolve() / 'host-capture-routes1981'
    work.mkdir(parents=True, exist_ok=True)
    java = Path(jdk) / 'bin/java' if jdk else Path(shutil.which('java') or '/usr/bin/java')
    javac = Path(jdk) / 'bin/javac' if jdk else None
    compiler = [str(javac)] if javac and javac.exists() else [str(java), 'com.sun.tools.javac.Main']
    values, formula_hashes = fixtures(source)
    paths = []
    for name, body in values.items():
        path = work / 'fixtures' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(body)
        paths.append(path)
    roots = ['AsyncSave1935.java', 'SaveQueue1935.java', 'ReflectionCache1945.java', 'SpeedWorkers1935.java',
             'CodecPreparation1981.java', 'SaveMemory1981.java', 'FaceResult1981.java', 'PhotoAnalysis1981.java', 'EncoderTail1981.java']
    inputs = [source / name for name in roots] + sorted((source / 'tests1981/capture').glob('*.java')) + paths
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    result = subprocess.run(compiler + ['-encoding', 'UTF-8', '-source', '8', '-target', '8', '-sourcepath', str(work / 'fixtures'), '-d', str(classes)] + list(map(str, inputs)),
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=90)
    (work / 'compile.log').write_text(result.stdout)
    if result.returncode:
        raise AssertionError(result.stdout)
    cases = [(path.stem, []) for path in sorted((source / 'tests1981/capture').glob('*Test.java'))]
    modes = ('snapshot-link', 'transfer-link', 'snapshot-assertion', 'transfer-assertion', 'snapshot-thread-death',
             'transfer-thread-death', 'after-submit', 'timing-link', 'transfer-finish-link', 'transfer-finish-assertion')
    cases += [('SaveHarness1935', [mode]) for mode in modes]
    runs, assertions, comparisons = [], 0, []
    for name, arguments in cases:
        result = subprocess.run([str(java), '-Xmx512m', '-cp', str(classes), 'com.hiro.ulike.' + name] + arguments,
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=30)
        label = '-'.join([name] + arguments)
        (work / (label + '.log')).write_text(result.stdout)
        if result.returncode:
            raise AssertionError(label + ': ' + result.stdout)
        match = re.search(r'PASS [^\n]*assertions=(\d+)', result.stdout)
        if not match:
            raise AssertionError('No executed assertions: ' + label)
        count = int(match.group(1))
        assertions += count
        runs.append({'case': label, 'assertions': count, 'exit_code': result.returncode})
        for line in result.stdout.splitlines():
            if line.startswith('MEMORY_1981 '):
                comparisons.append(json.loads(line[len('MEMORY_1981 '):]))
    required = ('CodecPreparation1981Test', 'SaveMemory1981Test', 'PhotoAnalysis1981Test', 'ReflectionWarmup1981Test', 'EncoderTail1981Test')
    if any(name not in {case[0] for case in cases} for name in required):
        raise AssertionError('Missing required capture route test class')

    # The old runner's fixed production compile list predates the new helpers.
    # Preserve its published .79 negative controls in this expanded runner,
    # with its byte-identical published queue and the same error harness.
    reference = source / 'tests1980/pipeline79-reference/AsyncSave1935.java'
    queue_reference = source / 'tests1978/cpu77-reference/SaveQueue1935.java'
    if hashlib.sha256(reference.read_bytes()).hexdigest() != REFERENCE_ASYNC_SHA:
        raise AssertionError('Published .79 capture negative control changed')
    if hashlib.sha256(queue_reference.read_bytes()).hexdigest() != REFERENCE_QUEUE_SHA:
        raise AssertionError('Published queue negative control changed')
    reference_classes = work / 'published79/classes'
    reference_classes.mkdir(parents=True, exist_ok=True)
    reference_inputs = [reference, queue_reference] + [source / name for name in ('ReflectionCache1945.java', 'SpeedWorkers1935.java', 'FaceResult1981.java')] + paths
    result = subprocess.run(compiler + ['-encoding', 'UTF-8', '-source', '8', '-target', '8', '-sourcepath', str(work / 'fixtures'), '-d', str(reference_classes)] + list(map(str, reference_inputs)),
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=90)
    (work / 'published79/compile.log').write_text(result.stdout)
    if result.returncode:
        raise AssertionError('Published .79 negative-control compilation: ' + result.stdout)
    for mode in ('snapshot-link', 'transfer-link', 'timing-link', 'transfer-finish-link'):
        result = subprocess.run([str(java), '-Xmx512m', '-cp', str(reference_classes), 'com.hiro.ulike.SaveHarness1935', mode],
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=15)
        (work / ('published79/' + mode + '.log')).write_text(result.stdout)
        if result.returncode == 0 or 'capture and exit reservation leaked' not in result.stdout:
            raise AssertionError('Published .79 must reproduce actual reservation leak, not an unrelated failure: ' + result.stdout)
        runs.append({'case': 'published79-' + mode, 'expected_regression': True, 'exit_code': result.returncode})
    output = {'suite': 'capture-routes1981', 'status': 'passed', 'assertions': assertions, 'physical_android_tested': False,
              'codec_close_event_resumes_deferred_prep': True, 'storage_before_codec_fence': True,
              'storage_cancel_joins_cleanup': True, 'memory_domains_and_stage_budget': True,
              'analysis_same_photo_and_failure_distinction': True, 'analysis_read_lease_drained': True,
              'constructor_metadata_without_initialization': True, 'encoder_tail_pristine_and_proof_lease': True,
              'capture_error_regressions_preserved': True, 'published79_linkage_reservation_leaks_reproduced': True,
              'candidate_error_paths_release_owned_bitmap_and_reservation': True, 'fatal_errors_rethrown_after_cleanup': True,
              'next_capture_after_failure_succeeds': True, 'submitted_job_not_released_twice': True,
              'diagnostic_linkage_failure_does_not_block_terminal_cleanup': True,
              'baseline_source_sha256': REFERENCE_ASYNC_SHA, 'baseline_queue_sha256': REFERENCE_QUEUE_SHA,
              'strong_memory_formula_sha256': formula_hashes,
              'memory_comparisons': comparisons, 'source_sha256': {name: hashlib.sha256((source / name).read_bytes()).hexdigest() for name in roots}, 'runs': runs}
    (work / 'result.json').write_text(json.dumps(output, ensure_ascii=False, indent=2) + '\n')
    return output


if __name__ == '__main__':
    import sys
    print(json.dumps(test(Path(__file__).parent, sys.argv[1], sys.argv[2] if len(sys.argv)>2 else None), ensure_ascii=False, indent=2))
