package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/** GX23: one bounded, private, idle-only qualification job. Certificates contain
 * timings and configuration only. A new capture cancels old proofs before any
 * certificate is committed; candidates never reach the user's saved image. */
public final class GpuQualification1961 {
    private GpuQualification1961() {}
    public interface Cancellation {boolean cancelled();}
    public interface Probe {void run(Cancellation cancellation);void close();}
    public static final class Record {
        public final long cpuNanos,gpuNanos;
        public final int variant;
        Record(long cpu,long gpu,int choice){cpuNanos=cpu;gpuNanos=gpu;variant=choice;}
    }
    private static final Object LOCK=new Object();
    private static final Object PERSIST_LOCK=new Object();
    private static final int LIMIT=64;
    private static final long MAX_RETAINED=96L*1024*1024,QUIET=2000000000L;
    private static final String SCHEMA="gx1961-full-output-2wins5-v1";
    private static final LinkedHashMap<String,Record> RECORDS=new LinkedHashMap<String,Record>(LIMIT,.75f,true);
    private static final ThreadLocal<Job> CURRENT=new ThreadLocal<Job>();
    private static volatile SharedPreferences preferences;
    private static volatile String base="";
    private static Job pending;
    private static Thread worker;
    private static long lastCapture=System.nanoTime();
    private static final class Job implements Cancellation {
        final String key;final long epoch,bytes;final Probe probe;
        volatile boolean cancelled,running;
        Job(String key,long bytes,Probe probe){this.key=key;this.bytes=bytes;this.probe=probe;epoch=ProcessingTiming1947.captureEpoch1953();}
        public boolean cancelled(){return cancelled||Thread.currentThread().isInterrupted()||
            epoch!=ProcessingTiming1947.captureEpoch1953()||!SaveQueue1935.idle1953();}
    }
    public static void initialize(Context context) {
        if(context==null)return;
        try {
            Context app=context.getApplicationContext();if(app==null)app=context;
            PackageInfo info=app.getPackageManager().getPackageInfo(app.getPackageName(),0);
            long code=Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;
            String identity=Build.FINGERPRINT+"|"+Build.VERSION.SDK_INT+"|"+app.getPackageName()+"|"+info.versionName+":"+code+"|"+SCHEMA;
            if(Build.FINGERPRINT==null||Build.FINGERPRINT.length()==0||"unknown".equals(Build.FINGERPRINT))return;
            synchronized(LOCK){base=identity;preferences=app.getSharedPreferences("ulike_gx1961_proofs",Context.MODE_PRIVATE);}
        } catch(Exception unavailable) { }
    }
    public static boolean background(){return CURRENT.get()!=null;}
    public static boolean cancelled(){Job job=CURRENT.get();return job!=null&&job.cancelled();}
    public static long retainedBytes(){synchronized(LOCK){return pending==null?0:pending.bytes;}}
    public static void captureChanged() {
        synchronized(LOCK){lastCapture=System.nanoTime();if(pending!=null){pending.cancelled=true;
            if(pending.running&&worker!=null)worker.interrupt();}LOCK.notifyAll();}
    }
    public static void wake(){synchronized(LOCK){LOCK.notifyAll();}}
    /** Ownership of probe is transferred even if scheduling is declined. */
    public static boolean schedule(String key,long bytes,Probe probe) {
        if(probe==null)return false;
        boolean accepted=false;
        try {
            synchronized(LOCK) {
                if(key!=null&&key.length()>0&&bytes>0&&bytes<=MAX_RETAINED&&pending==null&&
                        !background()&&!Thread.currentThread().isInterrupted()) {
                    if(worker==null||!worker.isAlive()) {
                        Thread made=new Thread(new Runnable(){public void run(){loop();}},"Hiro-ULike-GX-proof");
                        made.setDaemon(true);made.setPriority(Thread.MIN_PRIORITY);made.start();worker=made;
                    }
                    pending=new Job(key,bytes,probe);accepted=true;LOCK.notifyAll();
                }
            }
        } catch(RuntimeException unavailable) { } catch(OutOfMemoryError unavailable) { }
        if(!accepted)close(probe);return accepted;
    }
    private static void loop() {
        for(;;) {
            Job job;
            synchronized(LOCK) {
                for(;;) {
                    job=pending;
                    if(job!=null&&(job.cancelled||job.epoch!=ProcessingTiming1947.captureEpoch1953()))break;
                    if(job!=null&&System.nanoTime()-lastCapture>=QUIET&&SaveQueue1935.idle1953()&&
                            !GpuNoise1960.sessionBusy()&&WholeRoute1953.retainedBytes()==0) {job.running=true;break;}
                    try{LOCK.wait(250);}catch(InterruptedException ignored){}
                }
            }
            try {
                if(!job.cancelled()){CURRENT.set(job);job.probe.run(job);}
            } catch(RuntimeException unavailable) { } catch(LinkageError unavailable) { } catch(OutOfMemoryError unavailable) { } catch(AssertionError unavailable) { }
            finally {
                CURRENT.remove();close(job.probe);
                synchronized(LOCK){if(pending==job)pending=null;LOCK.notifyAll();}
                Thread.interrupted();
            }
        }
    }
    private static void close(Probe probe){try{probe.close();}catch(RuntimeException ignored){}catch(LinkageError ignored){}catch(OutOfMemoryError ignored){}catch(AssertionError ignored){}}
    private static String environment() {
        String nativeIdentity=GpuNoise1960.fingerprint();
        return base.length()==0||nativeIdentity==null||nativeIdentity.length()==0?null:digest(base+"|"+nativeIdentity);
    }
    private static String recordKey(String key,String environment){return "proof-"+digest(environment+"|"+key);}
    public static Record restore(String key) {
        String environment=environment();if(environment==null||key==null)return null;
        String name=recordKey(key,environment);
        synchronized(LOCK) {
            Record cached=RECORDS.get(name);if(cached!=null)return cached;
            SharedPreferences p=preferences;if(p==null)return null;
            try {
                String raw=p.getString(name,"");String[] fields=raw.split(":",-1);
                if(fields.length!=6||!SCHEMA.equals(fields[0]))return null;
                long cpu=Long.parseLong(fields[1]),gpu=Long.parseLong(fields[2]),stamp=Long.parseLong(fields[4]);int variant=Integer.parseInt(fields[3]);
                String core=fields[0]+":"+fields[1]+":"+fields[2]+":"+fields[3]+":"+fields[4];
                if(cpu<=0||gpu<=0||gpu>cpu-cpu/20||variant<0||variant>2||stamp<=0||
                        !digest(environment+"|"+key+"|"+core).equals(fields[5]))return null;
                Record result=new Record(cpu,gpu,variant);RECORDS.put(name,result);trim();return result;
            } catch(RuntimeException malformed){return null;}
        }
    }
    /** Caller must have proved every output value in two full trials with the
     * same variant. The service also enforces the inclusive 5% timing margin. */
    public static void qualified(String key,long cpu,long gpu,int variant) {
        synchronized(PERSIST_LOCK){persistQualified(key,cpu,gpu,variant);}
    }
    private static void persistQualified(String key,long cpu,long gpu,int variant) {
        if(key==null||cpu<=0||gpu<=0||gpu>cpu-cpu/20||variant<0||variant>2)return;
        String environment=environment();if(environment==null)return;
        String name=recordKey(key,environment);SharedPreferences p=preferences;
        SharedPreferences.Editor edit=null;
        if(p!=null)try {
                edit=p.edit();Map<String,?> all=p.getAll();
                int count=0;String oldest=null;long oldestTime=Long.MAX_VALUE;
                for(Map.Entry<String,?> entry:all.entrySet())if(entry.getKey().startsWith("proof-")) {
                    count++;if(name.equals(entry.getKey()))continue;
                    String[] fields=String.valueOf(entry.getValue()).split(":",-1);long stamp=0;
                    try{if(fields.length==6)stamp=Long.parseLong(fields[4]);}catch(RuntimeException malformed){}
                    if(stamp<oldestTime){oldestTime=stamp;oldest=entry.getKey();}
                }
                if(count>=LIMIT&&!all.containsKey(name)&&oldest!=null)edit.remove(oldest);
                String core=SCHEMA+":"+cpu+":"+gpu+":"+variant+":"+System.currentTimeMillis();
                edit.putString(name,core+":"+digest(environment+"|"+key+"|"+core));
        } catch(RuntimeException unavailable) {edit=null;}
        synchronized(LOCK) {
            Job job=CURRENT.get();if(job!=null&&(pending!=job||job.cancelled()))return;
            if(!environment.equals(environment()))return;
            RECORDS.put(name,new Record(cpu,gpu,variant));trim();
            try{if(edit!=null)edit.apply();}catch(RuntimeException unavailable){}
        }
    }
    public static void reject(String key) {
        String environment=environment();if(environment==null||key==null)return;
        synchronized(PERSIST_LOCK){synchronized(LOCK){String name=recordKey(key,environment);RECORDS.remove(name);
            try{if(preferences!=null)preferences.edit().remove(name).apply();}catch(RuntimeException ignored){}}}
    }
    private static void trim(){while(RECORDS.size()>LIMIT)RECORDS.remove(RECORDS.keySet().iterator().next());}
    private static String digest(String value) {
        try {
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));
            StringBuilder out=new StringBuilder(bytes.length*2);char[] hex="0123456789abcdef".toCharArray();
            for(byte b:bytes){out.append(hex[(b>>>4)&15]);out.append(hex[b&15]);}return out.toString();
        } catch(Exception unavailable){throw new IllegalStateException("GX proof fingerprint",unavailable);}
    }
}
