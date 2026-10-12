#!/usr/bin/env python3
"""Execute the production GX37 queue against isolated deterministic host stubs.

The production Java file is compiled unchanged. Fixture classes are never copied
into the released DEX; no Android/GPU performance claim is made by these tests.
"""
from pathlib import Path
import hashlib
import json
import os
import shutil
import subprocess


FIXTURES = {
    "android/content/SharedPreferences.java": r'''package android.content;
import java.util.Map;
public interface SharedPreferences {
    String getString(String key,String fallback);Map<String,?> getAll();Editor edit();
    interface Editor {Editor putString(String key,String value);Editor remove(String key);void apply();}
}''',
    "android/content/Context.java": r'''package android.content;
import android.content.pm.PackageManager;import java.util.*;
public class Context {
    public static final int MODE_PRIVATE=0;private final Memory prefs=new Memory();
    public Context getApplicationContext(){return this;}public String getPackageName(){return "test.ulike";}
    public PackageManager getPackageManager(){return new PackageManager();}
    public SharedPreferences getSharedPreferences(String name,int mode){return prefs;}
    public static class Memory implements SharedPreferences {
        private final Map<String,String> values=new HashMap<String,String>();
        public synchronized String getString(String key,String fallback){String v=values.get(key);return v==null?fallback:v;}
        public synchronized Map<String,?> getAll(){return new HashMap<String,String>(values);}
        public Editor edit(){return new Editor(){Map<String,String> changes=new HashMap<String,String>();
            public Editor putString(String k,String v){changes.put(k,v);return this;}
            public Editor remove(String k){changes.put(k,null);return this;}
            public void apply(){synchronized(Memory.this){for(Map.Entry<String,String> e:changes.entrySet())if(e.getValue()==null)values.remove(e.getKey());else values.put(e.getKey(),e.getValue());}}
        };}
    }
}''',
    "android/content/pm/PackageInfo.java": r'''package android.content.pm;
public class PackageInfo {public int versionCode=65;public String versionName="1.9.65";public long getLongVersionCode(){return versionCode;}}''',
    "android/content/pm/PackageManager.java": r'''package android.content.pm;
public class PackageManager {public PackageInfo getPackageInfo(String name,int flags){return new PackageInfo();}}''',
    "android/os/Build.java": r'''package android.os;
public class Build {public static String FINGERPRINT="host-qualified-driver";public static class VERSION {public static int SDK_INT=36;}}''',
    "com/hiro/ulike/QueueFixtures1967.java": r'''package com.hiro.ulike;
final class ProcessingTiming1947 {static volatile long epoch=1;static long captureEpoch1953(){return epoch;}}
final class SaveQueue1935 {static volatile boolean idle;static boolean idle1953(){return idle;}}
final class WholeRoute1953 {static volatile long retained;static long retainedBytes(){return retained;}}
final class GpuNoise1960 {static volatile boolean busy;static String fingerprint(){return "unchanged-pixel-shader-v1";}static boolean sessionBusy(){return busy;}}
final class SpeedWorkers1935 {
    static int maxWorkers(){return 2;}static int availableWorkers1944(){return 2;}
    static void run(Runnable[] tasks){Thread[] workers=new Thread[tasks.length];for(int i=0;i<tasks.length;i++){workers[i]=new Thread(tasks[i]);workers[i].start();}
        for(Thread t:workers)try{t.join();}catch(InterruptedException stop){Thread.currentThread().interrupt();throw new java.util.concurrent.CancellationException();}}
}''',
    "com/hiro/ulike/QueueHost1967.java": r'''package com.hiro.ulike;
import android.content.Context;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public class QueueHost1967 {
    static int assertions;static final long M=1024L*1024;static final Map<String,Integer> tests=new LinkedHashMap<String,Integer>();
    interface Check {boolean ok()throws Exception;}
    static void check(boolean condition,String why){assertions++;if(!condition)throw new AssertionError(why);}
    static void waitFor(Check condition,String why)throws Exception {long until=System.nanoTime()+4000000000L;while(!condition.ok()){if(System.nanoTime()>until)throw new AssertionError("timeout: "+why);Thread.sleep(5);}check(true,why);}
    static Field field(String name)throws Exception{Field f=GpuQualification1961.class.getDeclaredField(name);f.setAccessible(true);return f;}
    static void age()throws Exception{field("lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();}
    static void clearCache()throws Exception {Object lock=field("LOCK").get(null);synchronized(lock){((Map<?,?>)field("RECORDS").get(null)).clear();((Map<?,?>)field("FAILURES").get(null)).clear();}}
    static void clean()throws Exception {SaveQueue1935.idle=false;GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;waitFor(()->GpuQualification1961.retainedBytes()==0,"all old ownership released");GpuNoise1960.busy=false;WholeRoute1953.retained=0;}
    static class Probe implements GpuQualification1961.Probe {
        final String key;final AtomicInteger runs=new AtomicInteger(),closes=new AtomicInteger();final List<String> order;boolean qualify;
        Probe(String k){this(k,null,false);}Probe(String k,List<String> order,boolean q){key=k;this.order=order;qualify=q;}
        public void run(GpuQualification1961.Cancellation c){runs.incrementAndGet();if(order!=null)order.add(key);if(qualify)GpuQualification1961.qualified(key,100,80,0);}
        public void close(){closes.incrementAndGet();}
    }
    static void report(String name,int start){tests.put(name,assertions-start);}
    public static void main(String[] args)throws Exception {
        GpuQualification1961.initialize(new Context());clean();assertions=0;int n=assertions;
        GpuQualification1961.qualified("accepted",100,95,0);
        check(GpuQualification1961.restore("accepted")!=null,"inclusive five percent proof accepted");
        check(GpuQualification1961.maySchedule("accepted"),"accepted foreground GPU still eligible");
        check(!GpuQualification1961.canQueue("accepted",1),"accepted proof does not requeue");
        Probe accepted=new Probe("accepted");check(!GpuQualification1961.schedule("accepted",1,accepted),"accepted schedule declined");check(accepted.closes.get()==1&&accepted.runs.get()==0,"declined accepted probe closed once");
        GpuQualification1961.qualified("too-slow",100,96,0);check(GpuQualification1961.restore("too-slow")==null,"less than five percent win rejected");
        GpuQualification1961.qualified("bad-variant",100,80,3);check(GpuQualification1961.restore("bad-variant")==null,"unsupported variant rejected");
        report("accepted_keys_and_five_percent_admission",n);n=assertions;
        Probe a=new Probe("a"),b=new Probe("b"),dup=new Probe("a");
        check(GpuQualification1961.schedule("a",10,a),"first key queued");check(GpuQualification1961.schedule("b",20,b),"second key queued same capture");
        check(!GpuQualification1961.schedule("a",10,dup),"duplicate key declined");check(dup.closes.get()==1,"duplicate closed");
        check(GpuQualification1961.retainedBytes()==30,"all queued bytes counted");check(GpuQualification1961.status1967().contains("待ち2件"),"current queued diagnostics");
        GpuQualification1961.captureChanged();check(a.closes.get()==1&&b.closes.get()==1,"all queued snapshots closed on capture");check(a.runs.get()==0&&b.runs.get()==0,"queued canceled probes never run");check(GpuQualification1961.retainedBytes()==0,"queued canceled bytes released");
        report("multiple_unknown_keys_duplicate_and_capture_cleanup",n);n=assertions;
        Probe big=new Probe("strong-test:3:big"),tail=new Probe("strong-test:3:tail"),over=new Probe("strong-test:3:over");
        check(GpuQualification1961.schedule(big.key,60*M,big),"sixty MiB full proof queued");check(GpuQualification1961.schedule(tail.key,36*M,tail),"combined exact ninety-six MiB accepted");
        check(!GpuQualification1961.schedule(over.key,1,over),"combined memory overflow declined");check(GpuQualification1961.retainedBytes()==96*M,"combined hard cap");check(over.closes.get()==1,"overflow probe closed");clean();
        for(int i=0;i<7;i++)check(GpuQualification1961.schedule("early-"+i,1,new Probe("early-"+i)),"early slot admitted");
        Probe noSlot=new Probe("early-8");check(!GpuQualification1961.schedule(noSlot.key,1,noSlot),"one slot reserved for full Strong");
        check(GpuQualification1961.schedule("strong-test:3:slot",1,new Probe("strong-test:3:slot")),"late full Strong consumes reserved slot");
        check(GpuQualification1961.canQueue("strong-test:3:ninth",1),"full Strong may reclaim one lower-priority slot");check(GpuQualification1961.status1967().contains("待ち7件"),"preflight frees slot before cloning");clean();
        for(int i=0;i<8;i++)check(GpuQualification1961.schedule("strong-test:3:cap-"+i,1,new Probe("strong-test:3:cap-"+i)),"full Strong slot admitted");
        check(!GpuQualification1961.canQueue("strong-test:3:cap-ninth",1),"eight full Strong jobs cannot evict each other");clean();
        Probe resident=new Probe("resident-large"),small=new Probe("small-early");
        check(GpuQualification1961.schedule(resident.key,54*M,resident),"oversized resident model remains eligible");
        check(GpuQualification1961.schedule(small.key,24*M,small),"early sampled graph may coexist");
        GpuQualification1961.qualified("strong-test:3:already",100,80,0);
        check(!GpuQualification1961.canQueue("strong-test:3:already",64*M),"accepted Strong does not reclaim any snapshot");check(resident.closes.get()==0&&small.closes.get()==0,"accepted-key check precedes eviction");
        check(GpuQualification1961.canQueue("strong-test:3:reserved",64*M),"late full Strong preflight can recover budget");
        check(resident.closes.get()==1&&resident.runs.get()==0,"queued large lower-priority snapshot retired before caller cloning");
        check(small.closes.get()==0&&GpuQualification1961.retainedBytes()==24*M,"only needed queued lower-priority bytes released");
        check(GpuQualification1961.schedule("strong-test:3:reserved",64*M,new Probe("strong-test:3:reserved")),"late Strong queued after preflight reclamation");
        check(GpuQualification1961.canQueue(resident.key,1),"retired key remains eligible for future fresh snapshot");clean();
        report("retained_memory_job_limit_and_strong_reservation",n);n=assertions;
        Probe idle=new Probe("idle-gates");check(GpuQualification1961.schedule(idle.key,1,idle),"idle proof queued");age();Thread.sleep(40);check(idle.runs.get()==0,"save busy blocks proof");
        SaveQueue1935.idle=true;GpuNoise1960.busy=true;GpuQualification1961.wake();Thread.sleep(40);check(idle.runs.get()==0,"foreground GPU session blocks proof");
        GpuNoise1960.busy=false;WholeRoute1953.retained=1;GpuQualification1961.wake();Thread.sleep(40);check(idle.runs.get()==0,"whole route retained evidence blocks proof");
        WholeRoute1953.retained=0;GpuQualification1961.wake();waitFor(()->idle.closes.get()==1,"idle proof completes");check(idle.runs.get()==1,"proof ran once when every idle gate opens");clean();
        report("save_gpu_and_whole_route_idle_gates",n);n=assertions;
        final List<String> order=Collections.synchronizedList(new ArrayList<String>());
        Probe light=new Probe("early-order",order,true),strong=new Probe("strong-test:3:order",order,true),strong2=new Probe("strong-test:3:order2",order,true);
        check(GpuQualification1961.schedule(light.key,1,light),"early graph queued before strong");check(GpuQualification1961.schedule(strong.key,1,strong),"strong graph queued");check(GpuQualification1961.schedule(strong2.key,1,strong2),"second strong graph queued");
        SaveQueue1935.idle=true;age();waitFor(()->light.closes.get()==1&&strong.closes.get()==1&&strong2.closes.get()==1,"all distinct keys processed from same capture");
        check(order.equals(Arrays.asList(strong.key,strong2.key,light.key)),"Strong priority with FIFO among Strong keys");
        check(GpuQualification1961.restore(light.key)!=null&&GpuQualification1961.restore(strong.key)!=null&&GpuQualification1961.restore(strong2.key)!=null,"each active job commits own certificate");check(GpuQualification1961.retainedBytes()==0,"completed queue released all retained bytes");clean();
        report("single_worker_strong_priority_fifo_and_commit_identity",n);n=assertions;
        final CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);final AtomicInteger runClose=new AtomicInteger();
        GpuQualification1961.Probe blocking=new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){started.countDown();for(;;)try{release.await();break;}catch(InterruptedException expected){}GpuQualification1961.qualified("cancel-running",100,80,0);GpuQualification1961.rejectSpeed("cancel-running");}public void close(){runClose.incrementAndGet();}};
        Probe waiting=new Probe("cancel-queued");check(GpuQualification1961.schedule("cancel-running",10,blocking),"blocking probe queued");check(GpuQualification1961.schedule(waiting.key,20,waiting),"second probe queued for cancellation");SaveQueue1935.idle=true;age();check(started.await(3,TimeUnit.SECONDS),"active probe started");
        check(!GpuQualification1961.canQueue("strong-test:3:no-fit",96*M),"active probe budget cannot be evicted");check(waiting.closes.get()==0&&GpuQualification1961.retainedBytes()==30,"insufficient-capacity simulation preserves queued snapshot");
        GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;check(waiting.closes.get()==1&&waiting.runs.get()==0,"queued canceled probe closed immediately");check(runClose.get()==0,"running arrays not cleared while oracle executes");check(GpuQualification1961.retainedBytes()==10,"active canceled bytes retained until join");release.countDown();waitFor(()->runClose.get()==1&&GpuQualification1961.retainedBytes()==0,"active canceled owner eventually released");
        check(GpuQualification1961.restore("cancel-running")==null,"stale canceled proof cannot commit");check(GpuQualification1961.maySchedule("cancel-running"),"cancellation does not consume speed retry or cooldown");clean();
        report("running_cancellation_stale_commit_and_retry_preservation",n);n=assertions;
        GpuQualification1961.rejectExact("pixel-mismatch");check(GpuQualification1961.exactRejected("pixel-mismatch"),"exact mismatch remembered");check(!GpuQualification1961.maySchedule("pixel-mismatch"),"exact mismatch cannot retry");
        GpuQualification1961.qualified("pixel-mismatch",100,80,0);check(GpuQualification1961.restore("pixel-mismatch")==null,"certificate cannot override exact mismatch");clearCache();check(GpuQualification1961.exactRejected("pixel-mismatch"),"exact mismatch persists after cache reload");
        GpuQualification1961.rejectSpeed("speed-cooldown");check(!GpuQualification1961.maySchedule("speed-cooldown"),"speed failure cooldown enforced");
        Object lock=field("LOCK").get(null);synchronized(lock){Map<?,?> failures=(Map<?,?>)field("FAILURES").get(null);for(Object failure:failures.values()){Field exact=failure.getClass().getDeclaredField("exact");exact.setAccessible(true);if(!exact.getBoolean(failure)){Field retry=failure.getClass().getDeclaredField("retryAfter");retry.setAccessible(true);retry.setLong(failure,System.nanoTime()-1);}}}
        check(GpuQualification1961.maySchedule("speed-cooldown"),"speed retry eligible after cooldown");
        report("exact_reject_persistence_and_speed_cooldown",n);n=assertions;
        final int[] rows=new int[16];long elapsed=GpuQualification1961.parallelRows1964(0,16,4,()->false,(first,last)->{for(int i=first;i<last;i++)rows[i]++;return true;});check(elapsed>0,"CPU baseline measures real join wall time");for(int value:rows)check(value==1,"parallel CPU ranges cover each row once");
        boolean cancelled=false;try{GpuQualification1961.parallelRows1964(0,16,4,()->true,(first,last)->true);}catch(CancellationException expected){cancelled=true;}check(cancelled,"parallel baseline respects cancellation");
        report("real_parallel_cpu_oracle_range_and_cancellation",n);
        StringBuilder out=new StringBuilder("{\"status\":\"passed\",\"assertions\":"+assertions+",\"physical_android_tested\":false,\"tests\":{");boolean first=true;for(Map.Entry<String,Integer> e:tests.entrySet()){if(!first)out.append(',');first=false;out.append('"').append(e.getKey()).append("\":{\"status\":\"passed\",\"assertions\":").append(e.getValue()).append('}');}out.append("},\"admission_queue_regressions_passed\":true,\"gpu_safety_gates_preserved\":true}");System.out.println(out);
    }
}''',
}


def test(source, work, jdk=None):
    source, work = Path(source), Path(work)
    production = source / "GpuQualification1961.java"
    if not production.is_file():
        raise FileNotFoundError(production)
    fixture_dir, classes = work / "fixture-src", work / "classes"
    fixture_dir.mkdir(parents=True, exist_ok=True)
    classes.mkdir(parents=True, exist_ok=True)
    files = []
    for name, content in FIXTURES.items():
        path = fixture_dir / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content)
        files.append(path)
    copied = fixture_dir / "com/hiro/ulike/GpuQualification1961.java"
    copied.write_bytes(production.read_bytes())
    files.append(copied)
    if jdk:
        java, javac = str(Path(jdk) / "bin/java"), str(Path(jdk) / "bin/javac")
    else:
        java, javac = shutil.which("java"), shutil.which("javac")
    if not java or not javac:
        raise RuntimeError("A Java JDK with javac is required for fresh qualification tests")
    compile_result = subprocess.run([javac, "--release", "8", "-encoding", "UTF-8", "-d", str(classes), *map(str, files)], capture_output=True, text=True, timeout=90)
    if compile_result.returncode:
        raise RuntimeError("Qualification host compilation failed:\n" + compile_result.stdout + compile_result.stderr)
    execution = subprocess.run([java, "-ea", "-cp", str(classes), "com.hiro.ulike.QueueHost1967"], capture_output=True, text=True, timeout=45)
    if execution.returncode:
        raise RuntimeError("Qualification regressions failed:\n" + execution.stdout + execution.stderr)
    result = json.loads(execution.stdout.strip().splitlines()[-1])
    result["production_source_sha256"] = hashlib.sha256(production.read_bytes()).hexdigest()
    result["runner_source_sha256"] = hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
    result["fixture_classes_in_runtime"] = False
    (work / "qualification-host-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    return result


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True)
    parser.add_argument("--work", required=True)
    parser.add_argument("--jdk")
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk), ensure_ascii=False, indent=2))
