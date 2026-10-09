package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Compiles the unchanged production class; no simulated certificate service. */
public final class PreferredCertificate1973Test {
    static int assertions;
    static final String SCHEMA="gx1964-full-output-parallel-2wins5-v1";
    static final String PREFERRED=SCHEMA+"-strong-exact2-preferred-v1";
    static final String PREFIX="strong-gx1964-parallel-policy-bank-v1:3:";
    static final Map<String,Integer> tests=new LinkedHashMap<String,Integer>();
    static void check(boolean value,String label){assertions++;if(!value)throw new AssertionError(label);}
    static void section(String name,int start){tests.put(name,assertions-start);}
    static String generic(String suffix){return key(suffix).replace(PREFIX,"strong-gx1971-generic-policy-bank-v1:3:");}
    static String exact73(String suffix){return key(suffix).replace(PREFIX,"strong-gx1973-ieee-div-policy-bank-v1:3:");}
    static String key(String suffix){return PREFIX+"128:272:256:0:272:0:0:4284:4:1:1:1:192:256:"+suffix;}
    static Field field(String name)throws Exception {Field f=GpuQualification1961.class.getDeclaredField(name);f.setAccessible(true);return f;}
    static Object call(String name,Class<?>[] types,Object... values)throws Exception {Method m=GpuQualification1961.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(null,values);}
    static String environment()throws Exception {return (String)call("environment",new Class<?>[0]);}
    static String digest(String value)throws Exception {return (String)call("digest",new Class<?>[]{String.class},value);}
    static String stored(String key)throws Exception {return (String)call("recordKey",new Class<?>[]{String.class,String.class},key,environment());}
    static void reload()throws Exception {synchronized(field("LOCK").get(null)){((Map<?,?>)field("RECORDS").get(null)).clear();((Map<?,?>)field("FAILURES").get(null)).clear();}}
    static void writeRecord(String key,String marker,long cpu,long gpu,int variant,long stamp,boolean signed)throws Exception {
        String core=marker+":"+cpu+":"+gpu+":"+variant+":"+stamp;
        SharedPreferences p=(SharedPreferences)field("preferences").get(null);
        p.edit().putString(stored(key),core+":"+(signed?digest(environment()+"|"+key+"|"+core):"tampered")).apply();reload();
    }
    interface Condition {boolean ok()throws Exception;}
    static void waitFor(Condition predicate,String label)throws Exception {long until=System.nanoTime()+4000000000L;while(!predicate.ok()){if(System.nanoTime()>until)throw new AssertionError("timeout "+label);Thread.sleep(5);}check(true,label);}
    static void enableWorker()throws Exception {SaveQueue1935.idle=true;field("lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();}
    static void clean()throws Exception {SaveQueue1935.idle=false;GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;waitFor(()->GpuQualification1961.retainedBytes()==0,"detached ownership drained");}
    static void failRetries(String key)throws Exception {
        GpuQualification1961.rejectSpeed(key);
        synchronized(field("LOCK").get(null)){
            Object failure=((Map<?,?>)field("FAILURES").get(null)).get(stored(key));
            Field retries=failure.getClass().getDeclaredField("retries"),after=failure.getClass().getDeclaredField("retryAfter");
            retries.setAccessible(true);after.setAccessible(true);retries.setInt(failure,3);after.setLong(failure,System.nanoTime()+60000000000L);
        }
    }
    static void logic()throws Exception {
        Context context=new Context();GpuQualification1961.initialize(context);clean();assertions=0;
        int n=assertions;String slow=key("slow");
        GpuQualification1961.qualified(slow,100,160,1);check(GpuQualification1961.restore(slow)==null,"ordinary API retains five percent rule for full key");
        GpuQualification1961.qualifiedStrongPreferred1970(slow,100,160,1);
        GpuQualification1961.Record record=GpuQualification1961.restore(slow);check(record!=null&&record.cpuNanos==100&&record.gpuNanos==160&&record.variant==1,"preferred full route admits positive slower timing");
        SharedPreferences p=(SharedPreferences)field("preferences").get(null);
        check(p.getString(stored(slow),"").startsWith(PREFERRED+":"),"explicit marker signed in existing proof slot");
        reload();check(GpuQualification1961.restore(slow)!=null,"preferred proof restores from persisted fields");
        check(!GpuQualification1961.canQueue(slow,1),"accepted preferred proof cannot duplicate admission");
        String[] ordinary={"residual-v1:test","analysis-v1:test","single-v1:test","policy-v1:test","strong-gx1964-parallel-policy-bank-v1:0:test","strong-gx1964-parallel-policy-bank-v1:1:test","strong-gx1964-parallel-policy-bank-v1:2:test","strong-other-policy:3:test","not-strong:3:test"};
        for(String other:ordinary){GpuQualification1961.qualifiedStrongPreferred1970(other,100,160,0);check(GpuQualification1961.restore(other)==null,"preferred API excludes "+other);GpuQualification1961.qualified(other,100,96,0);check(GpuQualification1961.restore(other)==null,"ordinary timing exclusion retained "+other);GpuQualification1961.qualified(other,100,95,2);check(GpuQualification1961.restore(other)!=null,"ordinary five percent boundary retained "+other);}
        GpuQualification1961.qualifiedStrongPreferred1970(null,100,160,0);check(GpuQualification1961.restore(null)==null,"null preferred key excluded");
        for(int[] values:new int[][]{{0,160,0},{100,0,0},{-1,160,0},{100,-1,0},{100,160,-1},{100,160,3}}){String bad=key("invalid"+values[0]+"-"+values[1]+"-"+values[2]);GpuQualification1961.qualifiedStrongPreferred1970(bad,values[0],values[1],values[2]);check(GpuQualification1961.restore(bad)==null,"preferred invalid scalar excluded");}
        section("narrow_full_strong_and_unchanged_classic_admission",n);n=assertions;
        String malformed=key("malformed");
        writeRecord(malformed,PREFERRED,100,160,0,1,false);check(GpuQualification1961.restore(malformed)==null,"unsigned preferred proof excluded");
        writeRecord(malformed,PREFERRED,100,160,0,0,true);check(GpuQualification1961.restore(malformed)==null,"nonpositive timestamp excluded");
        writeRecord(malformed,PREFERRED,100,160,3,1,true);check(GpuQualification1961.restore(malformed)==null,"unsupported persisted variant excluded");
        writeRecord(malformed,PREFERRED,0,160,0,1,true);check(GpuQualification1961.restore(malformed)==null,"nonpositive persisted CPU timing excluded");
        writeRecord(malformed,PREFERRED,100,0,0,1,true);check(GpuQualification1961.restore(malformed)==null,"nonpositive persisted GPU timing excluded");
        writeRecord(malformed,PREFERRED+"-future",100,160,0,1,true);check(GpuQualification1961.restore(malformed)==null,"unknown marker excluded");
        writeRecord(malformed,SCHEMA,100,96,0,1,true);check(GpuQualification1961.restore(malformed)==null,"slow classic proof cannot reinterpret as preferred");
        writeRecord(malformed,SCHEMA,100,95,2,1,true);check(GpuQualification1961.restore(malformed)!=null,"legacy signed five percent proof remains valid");
        writeRecord(ordinary[0],PREFERRED,100,160,0,1,true);check(GpuQualification1961.restore(ordinary[0])==null,"validly signed preferred tag denied for other family");
        writeRecord(ordinary[4],PREFERRED,100,160,0,1,true);check(GpuQualification1961.restore(ordinary[4])==null,"preferred tag denied for preparation");
        writeRecord(malformed,PREFERRED,100,160,0,1,true);String originalName=stored(malformed);String originalRaw=p.getString(originalName,"");
        String otherKey=key("copied-signature");p.edit().putString(stored(otherKey),originalRaw).apply();reload();check(GpuQualification1961.restore(otherKey)==null,"signature binds complete layout key");
        String originalFingerprint=Build.FINGERPRINT;Build.FINGERPRINT="changed-host-driver";GpuQualification1961.initialize(context);reload();check(GpuQualification1961.restore(malformed)==null,"certificate bound to unchanged environment");
        Build.FINGERPRINT=originalFingerprint;GpuQualification1961.initialize(context);reload();check(GpuQualification1961.restore(malformed)!=null,"original environment certificate preserved");
        section("preferred_signed_payload_validation_and_legacy_compatibility",n);n=assertions;
        String retry=key("exhausted-speed");failRetries(retry);
        check(GpuQualification1961.maySchedule(retry),"full Strong reproof ignores exhausted old speed retries");
        check(GpuQualification1961.restore(retry)==null,"old speed failure is never directly promoted");
        check(GpuQualification1961.canQueue(retry,1),"full Strong can provide fresh proof during old speed cooldown");
        GpuQualification1961.qualifiedStrongPreferred1970(retry,100,160,0);check(GpuQualification1961.restore(retry)!=null,"fresh preferred proof replaces only speed failure");
        check(!((Map<?,?>)field("FAILURES").get(null)).containsKey(stored(retry)),"fresh proof clears its obsolete speed failure");
        String classicRetry="other-exhausted-speed";failRetries(classicRetry);check(!GpuQualification1961.maySchedule(classicRetry),"other family retry budget unchanged");check(!GpuQualification1961.canQueue(classicRetry,1),"other family cannot skip speed cooldown");
        String exact=key("exact-negative");GpuQualification1961.rejectExact(exact);check(!GpuQualification1961.maySchedule(exact),"full Strong exact rejection still blocks retry");GpuQualification1961.qualifiedStrongPreferred1970(exact,100,160,0);check(GpuQualification1961.restore(exact)==null,"preferred API cannot override exact rejection");
        reload();check(GpuQualification1961.exactRejected(exact)&&!GpuQualification1961.maySchedule(exact),"exact negative stable after reload");
        check(GpuQualification1961.status1967().contains("保存済み拒否")&&!GpuQualification1961.status1967().contains("画素不一致"),"diagnostic bucket accurately covers verification and execution failures");
        section("speed_only_reproof_and_exact_negative_safety",n);n=assertions;
        String interrupted=key("interrupted");Thread.currentThread().interrupt();try{GpuQualification1961.qualifiedStrongPreferred1970(interrupted,100,160,0);}finally{Thread.interrupted();}check(GpuQualification1961.restore(interrupted)==null&&!p.getAll().containsKey(stored(interrupted)),"interrupted foreground proof cannot commit cache or prefs");
        String stale=key("stale");CountDownLatch started=new CountDownLatch(1),finish=new CountDownLatch(1);AtomicInteger closed=new AtomicInteger();
        GpuQualification1961.Probe probe=new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){started.countDown();for(;;)try{finish.await();break;}catch(InterruptedException ignored){}Thread.interrupted();GpuQualification1961.qualifiedStrongPreferred1970(stale,100,160,0);}public void close(){closed.incrementAndGet();}};
        check(GpuQualification1961.schedule(stale,1,probe),"preferred stale proof scheduled");enableWorker();check(started.await(3,TimeUnit.SECONDS),"preferred stale proof runs");GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;finish.countDown();waitFor(()->closed.get()==1&&GpuQualification1961.retainedBytes()==0,"stale preferred owner closed once");
        check(GpuQualification1961.restore(stale)==null&&!p.getAll().containsKey(stored(stale)),"epoch changed preferred proof cannot commit after clearing interrupt");clean();
        String running=key("actual-job"),unrelated=key("unrelated-job");AtomicInteger ownedClose=new AtomicInteger();
        GpuQualification1961.Probe owned=new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){GpuQualification1961.qualifiedStrongPreferred1970(unrelated,100,160,0);GpuQualification1961.qualifiedStrongPreferred1970(running,100,160,2);}public void close(){ownedClose.incrementAndGet();}};
        check(GpuQualification1961.schedule(running,1,owned),"active matching preferred proof scheduled");enableWorker();waitFor(()->ownedClose.get()==1&&GpuQualification1961.retainedBytes()==0,"active preferred proof completed");check(GpuQualification1961.restore(unrelated)==null,"background proof cannot publish unrelated preferred key");check(GpuQualification1961.restore(running)!=null&&GpuQualification1961.restore(running).variant==2,"active matching proof commits");clean();
        String changed=key("environment-changed-while-persisting"),beforeFingerprint=Build.FINGERPRINT;
        SharedPreferences mutation=new SharedPreferences(){public String getString(String k,String fallback){return p.getString(k,fallback);}public Map<String,?> getAll(){return p.getAll();}public Editor edit(){Build.FINGERPRINT="changed-during-certificate-edit";GpuQualification1961.initialize(context);return p.edit();}};
        field("preferences").set(null,mutation);GpuQualification1961.qualifiedStrongPreferred1970(changed,100,160,0);
        Build.FINGERPRINT=beforeFingerprint;GpuQualification1961.initialize(context);reload();check(GpuQualification1961.restore(changed)==null&&!p.getAll().containsKey(stored(changed)),"foreground environment changed between prepare and commit writes no certificate");
        section("foreground_interrupt_background_identity_epoch_and_environment_guards",n);n=assertions;
        String legacy=key("old-tiled-negative");GpuQualification1961.rejectExact(legacy);
        check("cached_legacy_negative".equals(GpuQualification1961.exactFailure1971(legacy)),"old untyped rejection accurately labelled");
        String fresh=generic("old-tiled-negative");check(GpuQualification1961.exactFailure1971(fresh)==null&&GpuQualification1961.maySchedule(fresh),"different generic program has no borrowed tiled proof or failure");
        for(int variant=0;variant<3;variant++){String candidate=generic("layout"+variant);GpuQualification1961.qualifiedStrongPreferred1970(candidate,100,160,variant);reload();check(GpuQualification1961.restore(candidate)!=null&&GpuQualification1961.restore(candidate).variant==variant,"generic preferred layouts restore");}
        GpuQualification1961.qualifiedStrongPreferred1970(fresh,100,160,0);check(GpuQualification1961.restore(fresh)!=null&&GpuQualification1961.restore(legacy)==null,"fresh generic proof never removes tiled rejection");
        for(String cause:new String[]{"argb_mismatch","confidence_mismatch","policy_failure"}){
            String bad=generic("typed-"+cause);GpuQualification1961.rejectExact1971(bad,cause);reload();
            String expected=cause.equals("argb_mismatch")?"cached_argb_negative":cause.equals("confidence_mismatch")?"cached_confidence_negative":"cached_policy_negative";
            check(expected.equals(GpuQualification1961.exactFailure1971(bad)),"typed failure restores accurately");
            GpuQualification1961.qualifiedStrongPreferred1970(bad,100,160,0);check(GpuQualification1961.restore(bad)==null&&!GpuQualification1961.maySchedule(bad),"typed diagnostics never weaken rejection");
            String metadata="failure-cause-"+digest(environment()+"|"+bad);p.edit().putString(metadata,"untrusted arbitrary text").apply();reload();
            check("cached_legacy_negative".equals(GpuQualification1961.exactFailure1971(bad))&&GpuQualification1961.exactRejected(bad),"bad cause metadata preserves negative");
        }
        String untrusted=generic("untrusted");GpuQualification1961.rejectExact1971(untrusted,"pixel=private-data");reload();check("cached_legacy_negative".equals(GpuQualification1961.exactFailure1971(untrusted)),"arbitrary cause denied");
        check(!p.getAll().toString().contains("private-data"),"arbitrary content not persisted");
        SharedPreferences unavailableMetadata=new SharedPreferences(){public String getString(String name,String fallback){if(name.startsWith("failure-cause-"))throw new ClassCastException("wrong metadata type");return p.getString(name,fallback);}public Map<String,?> getAll(){return p.getAll();}public Editor edit(){return p.edit();}};
        field("preferences").set(null,unavailableMetadata);reload();check(GpuQualification1961.exactRejected(untrusted)&&"cached_legacy_negative".equals(GpuQualification1961.exactFailure1971(untrusted)),"unavailable diagnostic metadata never removes rejection");field("preferences").set(null,p);reload();
        String interruptedFailure=generic("interrupted-failure");Thread.currentThread().interrupt();try{GpuQualification1961.rejectExact1971(interruptedFailure,"argb_mismatch");}finally{Thread.interrupted();}check(!GpuQualification1961.exactRejected(interruptedFailure),"interrupted rejection commits no partial cause");
        section("generic_program_scope_and_typed_failure_metadata",n);n=assertions;
        for(int i=0;i<80;i++)GpuQualification1961.qualifiedStrongPreferred1970(key("bounded"+i),100,160,i%3);
        int count=0;for(String name:p.getAll().keySet())if(name.startsWith("proof-"))count++;
        check(count<=64&&((Map<?,?>)field("RECORDS").get(null)).size()<=64,"preferred and classic successes share existing bounded cache/persistence");
        section("preferred_certificate_persistence_limit",n);n=assertions;
        // New arithmetic profile certificates stay independent from all old profiles.
        for(int variant=0;variant<3;variant++){
            String precise=exact73("exact-layout-"+variant);
            GpuQualification1961.qualified(precise,100,160,variant);
            check(GpuQualification1961.restore(precise)==null,"exact profile ordinary API still needs speed margin");
            GpuQualification1961.qualifiedStrongPreferred1970(precise,100,160,variant);reload();
            GpuQualification1961.Record exactRecord=GpuQualification1961.restore(precise);
            check(exactRecord!=null&&exactRecord.variant==variant&&exactRecord.gpuNanos==160,"independent exact profile preferred certificate persisted");
            check(!GpuQualification1961.canQueue(precise,1),"exact preferred certificate suppresses duplicate background proof");
            check(GpuQualification1961.restore(key("exact-layout-"+variant))==null&&GpuQualification1961.restore(generic("exact-layout-"+variant))==null,"exact profile never grants old arithmetic certificates");
        }
        for(String wrong:new String[]{"strong-gx1973-ieee-div-policy-bank-v1:0:test","strong-gx1973-ieee-div-policy-bank-v1:1:test","strong-gx1973-ieee-div-policy-bank-v1:2:test","strong-gx1973-ieee-div-policy-bank-v10:3:test","strong-gx1973-ieee-div-policy-bank-v1:30:test"}){
            GpuQualification1961.qualifiedStrongPreferred1970(wrong,100,160,0);
            check(GpuQualification1961.restore(wrong)==null,"narrow exact profile mode/prefix exclusion "+wrong);
        }
        String oldNegative=generic("independent-negative"),newPrecise=exact73("independent-negative");
        GpuQualification1961.rejectExact1971(oldNegative,"argb_mismatch");
        check(!GpuQualification1961.exactRejected(newPrecise),"old generic negative cannot reject distinct new arithmetic");
        GpuQualification1961.qualifiedStrongPreferred1970(newPrecise,100,160,2);reload();
        check(GpuQualification1961.restore(newPrecise)!=null&&GpuQualification1961.exactRejected(oldNegative),"new full proof preserves old rejection");
        for(String cause:new String[]{"argb_mismatch","confidence_mismatch","policy_failure"}){
            String precise=exact73("exact-rejected-"+cause);
            GpuQualification1961.rejectExact1971(precise,cause);reload();
            check(GpuQualification1961.exactRejected(precise)&&!GpuQualification1961.maySchedule(precise),"new exact arithmetic rejection remains fail closed");
            GpuQualification1961.qualifiedStrongPreferred1970(precise,100,160,1);
            check(GpuQualification1961.restore(precise)==null,"new profile preferred proof cannot erase exact rejection");
        }
        String signatureSource=exact73("signature-source");
        GpuQualification1961.qualifiedStrongPreferred1970(signatureSource,100,160,0);
        String signedRaw=p.getString(stored(signatureSource),"");
        p.edit().putString(stored(generic("signature-source")),signedRaw).apply();reload();
        check(GpuQualification1961.restore(generic("signature-source"))==null,"signature cannot be transplanted to old arithmetic profile");
        section("exact_division_profile_certificates_are_narrow_independent_and_fail_closed",n);
    }
    static final class DiskContext extends Context {
        final SharedPreferences disk;DiskContext(File file){disk=new DiskPreferences(file);}
        public SharedPreferences getSharedPreferences(String name,int mode){return disk;}
    }
    static final class DiskPreferences implements SharedPreferences {
        final File file;final Properties values=new Properties();
        DiskPreferences(File file){this.file=file;if(file.isFile())try(InputStream in=new FileInputStream(file)){values.load(in);}catch(IOException failure){throw new IllegalStateException(failure);}}
        public synchronized String getString(String name,String fallback){return values.getProperty(name,fallback);}
        public synchronized Map<String,?> getAll(){Map<String,String> copy=new HashMap<String,String>();for(String name:values.stringPropertyNames())copy.put(name,values.getProperty(name));return copy;}
        public Editor edit(){return new Editor(){final Map<String,String> changes=new HashMap<String,String>();public Editor putString(String name,String value){changes.put(name,value);return this;}public Editor remove(String name){changes.put(name,null);return this;}public void apply(){synchronized(DiskPreferences.this){for(Map.Entry<String,String> change:changes.entrySet())if(change.getValue()==null)values.remove(change.getKey());else values.setProperty(change.getKey(),change.getValue());try{file.getParentFile().mkdirs();try(OutputStream out=new FileOutputStream(file)){values.store(out,"host prefs");}}catch(IOException failure){throw new IllegalStateException(failure);}}}};}
    }
    static void write(File file,boolean all)throws Exception {
        GpuQualification1961.initialize(new DiskContext(file));int n=assertions;
        if(all){for(int i=0;i<64;i++)GpuQualification1961.rejectExact1971(key("negative"+i),"argb_mismatch");check(!((SharedPreferences)field("preferences").get(null)).getAll().containsKey("reject-all-"+environment()),"64 cause attributes do not double-count bounded negatives");GpuQualification1961.rejectExact1971(key("negative64"),"confidence_mismatch");check(((SharedPreferences)field("preferences").get(null)).getAll().containsKey("reject-all-"+environment()),"bounded exact negatives preserve global fail-closed marker");}
        else {GpuQualification1961.qualifiedStrongPreferred1970(key("restart-slow"),100,160,2);GpuQualification1961.qualified(key("restart-fast"),100,95,1);GpuQualification1961.rejectExact(key("restart-exact"));GpuQualification1961.rejectExact1971(generic("restart-typed"),"confidence_mismatch");GpuQualification1961.qualifiedStrongPreferred1970(generic("restart-generic"),100,160,1);GpuQualification1961.qualifiedStrongPreferred1970(exact73("restart-exact73"),100,160,2);GpuQualification1961.rejectExact1971(exact73("restart-exact73-bad"),"argb_mismatch");check(GpuQualification1961.restore(key("restart-slow"))!=null,"write preferred proof to disk fixture");}
        section(all?"reject_all_persisted":"cross_process_persistence_written",n);
    }
    static void restore(File file,boolean all)throws Exception {
        GpuQualification1961.initialize(new DiskContext(file));int n=assertions;
        if(all){String blocked=key("never-seen");check("cache_limit_negative".equals(GpuQualification1961.exactFailure1971(blocked)),"fresh JVM identifies bounded fail-closed marker");check(GpuQualification1961.exactRejected(blocked)&&!GpuQualification1961.maySchedule(blocked),"restart preserves global exact rejection for new full key");GpuQualification1961.qualifiedStrongPreferred1970(blocked,100,160,0);check(GpuQualification1961.restore(blocked)==null,"preferred proof cannot override global exact rejection");}
        else {GpuQualification1961.Record slow=GpuQualification1961.restore(key("restart-slow")),fast=GpuQualification1961.restore(key("restart-fast"));check(slow!=null&&slow.gpuNanos==160&&slow.variant==2,"fresh JVM restores slower preferred certificate");check(fast!=null&&fast.gpuNanos==95&&fast.variant==1,"fresh JVM preserves classic certificate");check(GpuQualification1961.exactRejected(key("restart-exact"))&&!GpuQualification1961.maySchedule(key("restart-exact")),"fresh JVM preserves existing exact negative");check(!GpuQualification1961.canQueue(key("restart-slow"),1),"fresh restored preferred certificate does not queue");check("cached_confidence_negative".equals(GpuQualification1961.exactFailure1971(generic("restart-typed"))),"fresh JVM restores typed failure");check(GpuQualification1961.restore(generic("restart-generic"))!=null&&GpuQualification1961.restore(generic("restart-generic")).variant==1,"fresh JVM restores generic program proof");}
        if(!all){GpuQualification1961.Record precise=GpuQualification1961.restore(exact73("restart-exact73"));check(precise!=null&&precise.variant==2&&precise.gpuNanos==160,"fresh JVM restores new exact arithmetic certificate");check(GpuQualification1961.exactRejected(exact73("restart-exact73-bad")),"fresh JVM preserves new arithmetic rejection");}
        else check(GpuQualification1961.exactRejected(exact73("never-seen")),"global reject-all applies to the new arithmetic profile");
        section(all?"reject_all_cross_process_restore":"preferred_and_classic_cross_process_restore",n);
    }
    public static void main(String[] args)throws Exception {
        String mode=args.length==0?"logic":args[0];
        if("logic".equals(mode))logic();else if("write".equals(mode)||"allwrite".equals(mode))write(new File(args[1]),"allwrite".equals(mode));else if("restore".equals(mode)||"allrestore".equals(mode))restore(new File(args[1]),"allrestore".equals(mode));else throw new IllegalArgumentException(mode);
        StringBuilder json=new StringBuilder("{\"status\":\"passed\",\"assertions\":"+assertions+",\"tests\":{");boolean first=true;for(Map.Entry<String,Integer> test:tests.entrySet()){if(!first)json.append(',');first=false;json.append('"').append(test.getKey()).append("\":{\"status\":\"passed\",\"assertions\":").append(test.getValue()).append('}');}System.out.println(json.append("}}").toString());
    }
}
