package com.hiro.ulike;
import android.content.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public final class Tuning1979Test {
    static int assertions;static final Map<String,Integer> tests=new LinkedHashMap<String,Integer>();
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static Field field(String name)throws Exception{Field f=GpuQualification1961.class.getDeclaredField(name);f.setAccessible(true);return f;}
    static int[] uniforms(int tag){int[] u=new int[32];u[0]=8;u[1]=8;u[3]=8;u[5]=8;u[6]=tag;u[7]=8;u[8]=4;u[9]=1;u[10]=3;u[11]=1;u[12]=1;return u;}
    static String group(int[] u){String k=GpuStrong1960.profileKey1973(u,0);return "strong-gx1978-tuning-policy-bank-v1:"+k.substring(k.indexOf(':')+1);}
    static String cpuGroup(int[] u,int workers){return group(u).replace("strong-gx1978-tuning-","strong-gx1978-cpu-tuning-")+":"+workers;}
    static Object failure(String key)throws Exception{Method env=GpuQualification1961.class.getDeclaredMethod("environment");env.setAccessible(true);Method find=GpuQualification1961.class.getDeclaredMethod("failure",String.class,String.class);find.setAccessible(true);synchronized(field("LOCK").get(null)){return find.invoke(null,key,(String)env.invoke(null));}}
    static int retries(String key)throws Exception{Object f=failure(key);if(f==null)return -1;Field count=f.getClass().getDeclaredField("retries");count.setAccessible(true);return count.getInt(f);}
    static void expire(String key)throws Exception{Object f=failure(key);check(f!=null,"speed failure exists before advancing its clock");Field clock=f.getClass().getDeclaredField("retryAfter");clock.setAccessible(true);clock.setLong(f,System.nanoTime()-1);}
    static String choices(int[] u,GpuStrongTuning1975.Choice choice){StringBuilder out=new StringBuilder(" choice=").append(choice==null?"null":choice.profile+"/"+choice.variant);for(int p=0;p<8;p++){GpuQualification1961.Record r=GpuQualification1961.restore(GpuStrong1960.profileKey1973(u,p));out.append(" p").append(p).append('=').append(r==null?"null":r.variant+"/"+r.gpuNanos);}return out.toString();}
    static void retainedChoice(int[] u,GpuStrongTuning1975.Choice choice,String message){
        // The contract rejects every slower new profile. The relative order
        // among old profiles is measured, so scheduler jitter may change it.
        check(choice!=null&&choice.profile>=0&&choice.profile<4,message+choices(u,choice));
        String key=GpuStrong1960.profileKey1973(u,choice.profile);GpuQualification1961.Record child=GpuQualification1961.restore(key),aggregate=GpuQualification1961.restore(group(u));
        check(key.equals(choice.key)&&!GpuQualification1961.exactRejected(key)&&child!=null&&child.variant==choice.variant,"retained choice has its own matching exact child"+choices(u,choice));
        check(aggregate!=null&&aggregate.variant==choice.profile*3+choice.variant&&aggregate.gpuNanos==child.gpuNanos,"retained aggregate matches measured child and workgroup"+choices(u,choice));
    }
    static void queue(int[] u){GpuStrongTuning1975.schedule(new int[64],new int[128],u,new StrongNoise1958.Model(),new GpuPolicy1960.Protection());}
    static void age()throws Exception{SaveQueue1935.idle=true;field("lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();}
    static void drain()throws Exception{long until=System.nanoTime()+15000000000L;while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>until)throw new AssertionError("idle proof did not drain");Thread.sleep(5);}check(GpuNoise1960.sessions.get()==0,"native closes before retained snapshot release");}
    static void clean()throws Exception{
        SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged();
        if(GpuNoise1960.blockRelease!=null)GpuNoise1960.blockRelease.countDown();drain();
        GpuNoise1960.blocked=GpuNoise1960.policyFailure=GpuNoise1960.failRead=GpuNoise1960.oldUnavailable=GpuNoise1960.rejectMemory=GpuNoise1960.failLease=GpuNoise1960.stallOld=false;
        GpuNoise1960.newMode=0;GpuNoise1960.blockProfile=GpuNoise1960.blockVariant=GpuNoise1960.policyFailureProfile=GpuNoise1960.policyFailureVariant=-1;GpuNoise1960.readsByProfile=new AtomicIntegerArray(8);StrongNoise1958.unstable=false;GpuStrongRouting1978.slowerCpu=false;
    }
    static void section(String label,int start){tests.put(label,assertions-start);}
    public static void main(String[] args)throws Exception{
        Context context=new Context();GpuQualification1961.initialize(context);clean();int n=assertions;
        int[] u=uniforms(1),src=new int[64],policy=new int[128];src[0]=123;policy[0]=77;GpuPolicy1960.Protection protection=new GpuPolicy1960.Protection();
        int cpuBefore=StrongNoise1958.cpuRuns.get(),reads=GpuNoise1960.reads.get(),repeats=GpuNoise1960.repeats.get();GpuNoise1960.newMode=1;
        GpuStrongTuning1975.schedule(src,policy,u,new StrongNoise1958.Model(),protection);long held=GpuQualification1961.retainedBytes();
        check(held>0&&held<=96L*1024*1024,"bounded immutable snapshot admitted while saving");queue(u);check(held==GpuQualification1961.retainedBytes(),"duplicate geometry not cloned twice");
        Thread.sleep(30);check(StrongNoise1958.cpuRuns.get()==cpuBefore&&GpuNoise1960.reads.get()==reads,"saving performs no new CPU or GPU proof");
        src[0]=999;policy[0]=999;protection.original.masks[0]=999;protection.original.u[0]=999;protection.original.grid[0]=999;protection.original.f[0]=999;
        age();drain();GpuStrongTuning1975.Choice choice=GpuStrongTuning1975.select(u);
        check(choice!=null&&choice.profile==7&&choice.variant==2,"new fastest exact candidate selected after balanced confirmation");
        check(GpuQualification1961.restore(group(u)).variant==23,"24th candidate encoded as23");
        check(StrongNoise1958.cpuRuns.get()==cpuBefore+2,"two independent CPU references");
        check(GpuNoise1960.repeats.get()==repeats+21,"second full trial reuses uploads for21 passing variant trials");
        check(GpuNoise1960.readsByProfile.get(7)>=8,"new winner also completed two balanced fresh-upload trials");
        for(int p=1;p<8;p++)check(GpuQualification1961.restore(GpuStrong1960.profileKey1973(u,p))!=null,"independent complete child proof "+p);
        check(GpuQualification1961.exactRejected(GpuStrong1960.profileKey1973(u,0)),"old actual exact rejection retained");
        for(Object value:context.getSharedPreferences("ulike_gx1961_proofs",0).getAll().values())check(value instanceof String&&((String)value).length()<280,"only signed scalar evidence persisted");
        synchronized(field("LOCK").get(null)){((Map<?,?>)field("RECORDS").get(null)).clear();((Map<?,?>)field("FAILURES").get(null)).clear();}
        check(GpuStrongTuning1975.select(u).profile==7,"new selection restores from scalar proof");
        section("deferred_idle_24_candidates_policy_and_snapshot_ownership",n);n=assertions;clean();
        for(int mode:new int[]{0,6,7,8}){
            int[] slow=uniforms(20+mode);GpuNoise1960.newMode=mode==8?0:mode;GpuNoise1960.stallOld=mode==8;queue(slow);age();drain();choice=GpuStrongTuning1975.select(slow);
            retainedChoice(slow,choice,"slow shader/upload/balanced recheck retains an exact old route "+mode);
            if(mode==8){check(choice.profile!=2,"controlled old-profile pause exercises a different retained winner"+choices(slow,choice));System.out.println("CONTROL old-profile-pause"+choices(slow,choice));}
            clean();
        }
        section("inclusive_upload_and_balanced_five_percent_speed_gate",n);n=assertions;
        for(int mode=2;mode<=5;mode++){
            int[] bad=uniforms(40+mode);GpuNoise1960.newMode=mode;queue(bad);age();drain();choice=GpuStrongTuning1975.select(bad);
            retainedChoice(bad,choice,"new lastARGB/confidence/policy/second-trial mismatch keeps an exact old winner "+mode);
            check(GpuQualification1961.exactRejected(GpuStrong1960.profileKey1973(bad,7)),"new failure is durable exact evidence "+mode);
            check(GpuQualification1961.restore(GpuStrong1960.profileKey1973(bad,7))==null,"partial trial never certifies "+mode);clean();
        }
        int cohortBefore=GpuStrongRouting1978.calls;int[] absent=uniforms(60);for(int p=0;p<4;p++)GpuQualification1961.rejectExact1971(GpuStrong1960.profileKey1973(absent,p),"policy_failure");GpuNoise1960.newMode=1;queue(absent);age();drain();
        check(GpuStrongTuning1975.select(absent)==null&&GpuNoise1960.readsByProfile.get(7)>0,"new direct candidates are reachable without inventing an old GPU baseline");
        check(GpuStrongTuning1975.selectCpu1978(absent,2)!=null,"separate measured parallel CPU gate can recover a direct candidate");check(GpuStrongTuning1975.selectCpu1978(absent,1)==null,"CPU comparison binds exact worker count");
        check(GpuStrongRouting1978.calls==cohortBefore+1,"CPU-only recovery continues to mixed cohort proof in the same idle job");
        for(int p=0;p<4;p++)check(GpuQualification1961.exactRejected(GpuStrong1960.profileKey1973(absent,p)),"old policy failure remains durable "+p);clean();
        int[] cpuFast=uniforms(59);GpuNoise1960.oldUnavailable=true;GpuNoise1960.newMode=1;GpuStrongRouting1978.slowerCpu=true;cohortBefore=GpuStrongRouting1978.calls;queue(cpuFast);age();drain();
        check(GpuStrongTuning1975.select(cpuFast)==null&&GpuStrongTuning1975.selectCpu1978(cpuFast,2)==null,"slower GPU cannot pass the separate CPU-only speed baseline");
        check(GpuStrongRouting1978.calls==cohortBefore,"failed CPU baseline cannot enter mixed route proof");
        section("full_argb_confidence_policy_and_old_baseline_requirements",n);n=assertions;
        String cpuName=cpuGroup(cpuFast,2);check(retries(cpuName)==0&&!GpuQualification1961.maySchedule(cpuName),"initial failure starts worker-bound cooldown without spending retry");
        check(failure(group(cpuFast))==null,"CPU speed failure never fabricates preferred aggregate failure");
        cpuBefore=StrongNoise1958.cpuRuns.get();queue(cpuFast);check(GpuQualification1961.retainedBytes()==0&&StrongNoise1958.cpuRuns.get()==cpuBefore,"cooldown stops another snapshot before clone");
        expire(cpuName);GpuNoise1960.blockProfile=7;GpuNoise1960.blocked=true;GpuNoise1960.blockStarted=new CountDownLatch(1);GpuNoise1960.blockRelease=new CountDownLatch(1);
        queue(cpuFast);age();check(GpuNoise1960.blockStarted.await(6,TimeUnit.SECONDS),"worker-bound retry reaches selected profile");clean();check(retries(cpuName)==0,"capture cancellation does not spend companion retry");
        for(int attempt=1;attempt<=3;attempt++){
            expire(cpuName);GpuNoise1960.oldUnavailable=true;GpuNoise1960.newMode=1;GpuStrongRouting1978.slowerCpu=true;
            check(GpuQualification1961.maySchedule(cpuName),"eligible unsuccessful retry "+attempt);queue(cpuFast);age();drain();
            check(retries(cpuName)==attempt,"exactly one worker-bound retry counted "+attempt);
            check(failure(group(cpuFast))==null,"normal aggregate remains absent during CPU fallback retry "+attempt);
        }
        expire(cpuName);cpuBefore=StrongNoise1958.cpuRuns.get();queue(cpuFast);
        check(!GpuQualification1961.maySchedule(cpuName)&&GpuQualification1961.retainedBytes()==0&&StrongNoise1958.cpuRuns.get()==cpuBefore,"three failed retries stay blocked after cooldown expires");
        check(GpuQualification1961.maySchedule(cpuGroup(cpuFast,3)),"other worker-count condition does not borrow speed failure");
        check(!GpuQualification1961.exactRejected(GpuStrong1960.profileKey1973(cpuFast,7)),"speed failures never become an exact rejection");
        final int[] closes={0};GpuQualification1961.Probe invalidProbe=new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){throw new AssertionError("invalid companion admitted");}public void close(){closes[0]++;}};
        check(!GpuQualification1961.scheduleStrong1978(group(cpuFast),cpuGroup(uniforms(999),2),1,invalidProbe)&&closes[0]==1,"companion must have the same exact geometry and ownership closes once");clean();
        section("worker_bound_cpu_speed_cooldown_three_retries_and_cancel",n);n=assertions;
        int[] unstable=uniforms(61);StrongNoise1958.unstable=true;reads=GpuNoise1960.reads.get();queue(unstable);age();drain();check(GpuNoise1960.reads.get()==reads,"unstable CPU references stop every GPU candidate");clean();
        int[] noMemory=uniforms(62);GpuNoise1960.rejectMemory=true;queue(noMemory);check(GpuQualification1961.retainedBytes()==0,"memory refusal before clone");clean();
        int[] noLease=uniforms(63);GpuNoise1960.failLease=true;reads=GpuNoise1960.reads.get();queue(noLease);age();drain();check(GpuNoise1960.reads.get()==reads,"failed lease never submits GPU");clean();
        int[] failed=uniforms(64);GpuNoise1960.failRead=true;queue(failed);age();drain();check(GpuStrongTuning1975.select(failed)==null,"transport error never certifies");for(int p=0;p<8;p++)check(!GpuQualification1961.exactRejected(GpuStrong1960.profileKey1973(failed,p)),"transport is not fake exact evidence "+p);clean();
        section("unstable_reference_memory_and_transport_safety",n);n=assertions;
        int[] cancelled=uniforms(70);GpuNoise1960.newMode=1;GpuNoise1960.blockProfile=7;GpuNoise1960.blocked=true;GpuNoise1960.blockStarted=new CountDownLatch(1);GpuNoise1960.blockRelease=new CountDownLatch(1);
        queue(cancelled);age();check(GpuNoise1960.blockStarted.await(6,TimeUnit.SECONDS),"new GPU profile reached native wait");clean();
        check(GpuQualification1961.restore(group(cancelled))==null,"next capture cancels aggregate commit");check(GpuQualification1961.restore(GpuStrong1960.profileKey1973(cancelled,7))==null,"cancelled new child is never accepted");
        section("capture_cancel_pending_gpu_proof",n);n=assertions;
        int[] legacy=uniforms(80);String current=group(legacy),older=current.replace("gx1978","gx1976"),oldest=current.replace("gx1978","gx1975");
        GpuQualification1961.qualifiedStrongPreferred1970(GpuStrong1960.profileKey1973(legacy,3),100,200,2);GpuQualification1961.qualifiedStrongPreferred1970(older,100,200,11);
        choice=GpuStrongTuning1975.select(legacy);check(choice!=null&&choice.profile==3&&choice.variant==2,"old76 aggregate remains separately usable");check(GpuQualification1961.restore(current)==null,"old restore does not forge new aggregate");
        for(int invalid:new int[]{-1,24,100}){GpuQualification1961.qualifiedStrongPreferred1970(current,100,200,invalid);check(GpuQualification1961.restore(current)==null,"new candidate bound rejects "+invalid);}
        GpuQualification1961.qualifiedStrongPreferred1970(current,100,200,23);check(GpuQualification1961.restore(current).variant==23,"new bound23");
        check(GpuStrongTuning1975.select(legacy)==null,"new aggregate without matching child cannot be used");check(GpuQualification1961.restore(current)==null,"stale aggregate is retired");
        for(int invalid:new int[]{9,11,23}){GpuQualification1961.qualifiedStrongPreferred1970(oldest,100,200,invalid);check(GpuQualification1961.restore(oldest)==null,"old75 bound isnotexpanded "+invalid);}
        GpuQualification1961.qualifiedStrongProfile1975(current,GpuStrong1960.profileKey1973(legacy,7),100,1,2);check(GpuQualification1961.restore(GpuStrong1960.profileKey1973(legacy,7))==null,"foreground cannot forge an idle child proof");
        section("new_and_legacy_namespaces_bounds_and_authority",n);

        // A real idle Qualification job must publish a completed legacy child
        // before it tries the next variant. The latch observes the production
        // tuner between variants, without calling the proof method ourselves.
        n=assertions;clean();int[] progress=uniforms(10);String progressChild=GpuStrong1960.profileKey1973(progress,1);
        GpuNoise1960.blockProfile=1;GpuNoise1960.blockVariant=1;GpuNoise1960.blocked=true;
        GpuNoise1960.blockStarted=new CountDownLatch(1);GpuNoise1960.blockRelease=new CountDownLatch(1);
        queue(progress);age();check(GpuNoise1960.blockStarted.await(6,TimeUnit.SECONDS),"later legacy variant reached its controlled native wait");
        check(GpuNoise1960.readsByProfile.get(1)==3,"legacy variant0 completed exactly two reads before variant1");
        check(GpuQualification1961.retainedBytes()>0&&GpuNoise1960.sessions.get()==1,"unfinished tuning job still owns its snapshot and active session");
        GpuQualification1961.Record first=GpuQualification1961.restore(progressChild);
        check(first!=null&&first.variant==0&&first.cpuNanos>0&&first.gpuNanos>0,"first complete legacy exact2 child is available before later variants finish");
        check(GpuQualification1961.restore(group(progress))==null,"partial progress cannot forge a completed aggregate");
        check(!GpuQualification1961.exactRejected(progressChild),"completed first variant has no fabricated rejection");
        synchronized(field("LOCK").get(null)){((Map<?,?>)field("RECORDS").get(null)).clear();}
        GpuQualification1961.Record persisted=GpuQualification1961.restore(progressChild);
        check(persisted!=null&&persisted.variant==0&&persisted.cpuNanos==first.cpuNanos&&persisted.gpuNanos==first.gpuNanos,"early child restores from signed scalar persistence while later variant is blocked");
        clean();GpuQualification1961.Record retained=GpuQualification1961.restore(progressChild);
        check(retained!=null&&retained.variant==0&&retained.cpuNanos==first.cpuNanos&&retained.gpuNanos==first.gpuNanos,"capture cancellation preserves the already completed legacy child unchanged");
        check(GpuQualification1961.restore(group(progress))==null,"capture cancellation cannot complete an unfinished aggregate");
        check(!GpuQualification1961.exactRejected(progressChild),"capture cancellation does not turn progress into an exact failure");
        section("first_legacy_exact2_progress_survives_capture_cancellation",n);n=assertions;
        int[] policyLate=uniforms(11);String policyChild=GpuStrong1960.profileKey1973(policyLate,1);
        GpuNoise1960.blockProfile=1;GpuNoise1960.blockVariant=1;GpuNoise1960.blocked=true;
        GpuNoise1960.policyFailureProfile=1;GpuNoise1960.policyFailureVariant=1;
        GpuNoise1960.blockStarted=new CountDownLatch(1);GpuNoise1960.blockRelease=new CountDownLatch(1);
        queue(policyLate);age();check(GpuNoise1960.blockStarted.await(6,TimeUnit.SECONDS),"later policy-failing variant reached its controlled native wait");
        first=GpuQualification1961.restore(policyChild);
        check(first!=null&&first.variant==0,"legacy exact2 proof exists before later policy failure");
        check(GpuQualification1961.restore(group(policyLate))==null,"later policy failure test observes an unfinished aggregate");
        GpuNoise1960.blockRelease.countDown();drain();
        check(GpuQualification1961.exactRejected(policyChild),"later policy failure durably rejects the whole affected profile");
        check("cached_policy_negative".equals(GpuQualification1961.exactFailure1971(policyChild)),"later failure retains its precise policy classification");
        check(GpuQualification1961.restore(policyChild)==null,"later policy rejection removes the early child proof");
        choice=GpuStrongTuning1975.select(policyLate);
        check(choice==null||choice.profile!=1,"rejected early child cannot supply the final aggregate winner");
        check(GpuQualification1961.restore(progressChild)!=null,"later policy rejection does not erase another geometry's completed proof");
        synchronized(field("LOCK").get(null)){((Map<?,?>)field("RECORDS").get(null)).clear();((Map<?,?>)field("FAILURES").get(null)).clear();}
        check(GpuQualification1961.exactRejected(policyChild)&&GpuQualification1961.restore(policyChild)==null,"later policy rejection and proof removal survive a signed persistence reload");
        clean();section("later_policy_rejection_retires_early_legacy_child",n);

        StringBuilder out=new StringBuilder("{\"status\":\"passed\",\"assertions\":").append(assertions).append(",\"tests\":{");boolean comma=false;
        for(Map.Entry<String,Integer> entry:tests.entrySet()){if(comma)out.append(',');comma=true;out.append('"').append(entry.getKey()).append("\":").append(entry.getValue());}
        System.out.println(out.append("}}").toString());
    }
}
