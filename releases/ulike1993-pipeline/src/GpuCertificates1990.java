package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/** Device preflight evidence, deliberately separate from photograph/shape proofs.
 * An entry says that a named, versioned finite corpus matched on this exact
 * app/OS/native-kernel/driver. It never fabricates a GpuQualification Record,
 * claims exhaustive input coverage, or supplies invented timing for a photo.
 * Actual mismatches remain sticky for this suite and environment. */
public final class GpuCertificates1990 {
    private GpuCertificates1990() {}
    private static final Object LOCK=new Object();
    private static final String SCHEMA="gx1990-device-corpus-v1";
    private static volatile SharedPreferences preferences;
    private static String cachedEnvironment;
    /** Small scalar cache only. The raw preference values are still checked on
     * every read, so external corruption or a rejection cannot reuse a pass. */
    private static final LinkedHashMap<String,Cached> CACHE=new LinkedHashMap<String,Cached>(16,.75f,true){
        protected boolean removeEldestEntry(Map.Entry<String,Cached> entry){return ((Map<?,?>)this).size()>16;}
    };
    private static final class Cached {
        final String name;String passRaw,rejectRaw;Evidence evidence;boolean rejected;
        Cached(String environment,String suite){name=name(environment,suite);}
    }
    private static Cached cached(String environment,String suite){
        if(!environment.equals(cachedEnvironment)){CACHE.clear();cachedEnvironment=environment;}
        Cached cached=CACHE.get(suite);
        if(cached==null){cached=new Cached(environment,suite);CACHE.put(suite,cached);}
        return cached;
    }
    static final class Evidence {
        final int cases;final long pixels,cpuNanos,gpuNanos;
        Evidence(int count,long checked,long cpu,long gpu){cases=count;pixels=checked;cpuNanos=cpu;gpuNanos=gpu;}
    }
    static void initialize(Context context) {
        if(context==null)return;
        try {Context app=context.getApplicationContext();if(app==null)app=context;
            synchronized(LOCK){preferences=app.getSharedPreferences("ulike_gpu_preflight1990",Context.MODE_PRIVATE);CACHE.clear();cachedEnvironment=null;}
        }catch(RuntimeException unavailable){}
    }
    private static boolean valid(String suite){return suite!=null&&suite.length()>0&&suite.length()<=200;}
    private static String environment(){return GpuQualification1961.environment1990();}
    private static String name(String environment,String suite){return digest(SCHEMA+"|"+environment+"|"+suite);}
    static Evidence restore(String suite) {
        if(!valid(suite))return null;
        String environment=environment();if(environment==null)return null;
        synchronized(LOCK) {
            SharedPreferences p=preferences;if(p==null)return null;
            try {
                Cached cached=cached(environment,suite);
                if(rejected(p,cached,environment,suite))return null;
                String raw=p.getString("pass-"+cached.name,"");
                if(raw.equals(cached.passRaw))return environment.equals(environment())?cached.evidence:null;
                cached.passRaw=raw;cached.evidence=null;
                String[] fields=raw.split(":",-1);
                if(fields.length!=6||!SCHEMA.equals(fields[0]))return null;
                int count=Integer.parseInt(fields[1]);long pixels=Long.parseLong(fields[2]),cpu=Long.parseLong(fields[3]),gpu=Long.parseLong(fields[4]);
                String core=fields[0]+":"+fields[1]+":"+fields[2]+":"+fields[3]+":"+fields[4];
                if(count<1||pixels<1||cpu<1||gpu<1||!digest(environment+"|"+suite+"|"+core).equals(fields[5]))return null;
                if(!environment.equals(environment()))return null;
                cached.evidence=new Evidence(count,pixels,cpu,gpu);return cached.evidence;
            }catch(RuntimeException malformed){return null;}
        }
    }
    static boolean passed(String suite,int cases,long pixels,long cpuNanos,long gpuNanos) {
        if(!valid(suite)||cases<1||pixels<1||cpuNanos<1||gpuNanos<1||Thread.currentThread().isInterrupted())return false;
        String environment=environment();if(environment==null)return false;
        synchronized(LOCK) {
            SharedPreferences p=preferences;if(p==null)return false;
            try {
                Cached cached=cached(environment,suite);String name=cached.name;
                if(rejected(p,cached,environment,suite))return false;
                String core=SCHEMA+":"+cases+":"+pixels+":"+cpuNanos+":"+gpuNanos;
                SharedPreferences.Editor edit=p.edit().putString("pass-"+name,core+":"+digest(environment+"|"+suite+"|"+core));
                if(Thread.currentThread().isInterrupted()||!environment.equals(environment()))return false;
                edit.apply();CACHE.remove(suite);return true;
            }catch(RuntimeException unavailable){return false;}
        }
    }
    static boolean rejected(String suite) {
        if(!valid(suite))return true;String environment=environment();if(environment==null)return false;
        synchronized(LOCK){SharedPreferences p=preferences;return p!=null&&rejected(p,cached(environment,suite),environment,suite);}
    }
    private static boolean rejected(SharedPreferences p,Cached cached,String environment,String suite) {
        try {
            String raw=p.getString("reject-"+cached.name,"");
            if(raw.equals(cached.rejectRaw))return cached.rejected;
            String[] fields=raw.split(":",-1);
            boolean rejected=fields.length==3&&SCHEMA.equals(fields[0])&&digest(environment+"|"+suite+"|reject|"+fields[1]).equals(fields[2]);
            cached.rejectRaw=raw;cached.rejected=rejected;
            if(rejected){cached.evidence=null;cached.passRaw=null;}
            return rejected;
        }catch(RuntimeException unavailable){return true;}
    }
    /** Retained binary signature for previously compiled helper callers. */
    private static boolean rejected(SharedPreferences p,String name,String environment,String suite) {
        try {String[] fields=p.getString("reject-"+name,"").split(":",-1);
            return fields.length==3&&SCHEMA.equals(fields[0])&&digest(environment+"|"+suite+"|reject|"+fields[1]).equals(fields[2]);
        }catch(RuntimeException unavailable){return true;}
    }
    /** Only the caller observing a completed unequal pixel/confidence comparison
     * uses this method. Compilation/allocation/cancellation never becomes a
     * permanent mismatch. No photo/source image is retained in preferences. */
    static void reject(String suite,String reason) {
        if(!valid(suite)||Thread.currentThread().isInterrupted())return;
        String environment=environment();if(environment==null)return;
        synchronized(LOCK){SharedPreferences p=preferences;if(p==null)return;
            try {String name=name(environment,suite),cause=digest(reason==null?"mismatch":reason);
                SharedPreferences.Editor edit=p.edit().remove("pass-"+name).putString("reject-"+name,
                    SCHEMA+":"+cause+":"+digest(environment+"|"+suite+"|reject|"+cause));
                if(!Thread.currentThread().isInterrupted()&&environment.equals(environment())){edit.apply();CACHE.remove(suite);}
            }catch(RuntimeException unavailable){}
        }
    }
    private static String digest(String value) {
        try {byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));
            StringBuilder out=new StringBuilder(64);for(byte b:bytes){int n=b&255;out.append(Character.forDigit(n>>>4,16)).append(Character.forDigit(n&15,16));}return out.toString();
        }catch(Exception unavailable){throw new IllegalStateException("preflight digest unavailable",unavailable);}
    }
}
