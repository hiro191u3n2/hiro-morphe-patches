package com.hiro.ulike;

import android.content.Context;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;

/** Real qualification, snapshot accounting and tuner; controlled GPU transport.
 * These are ownership/admission regressions, never device-speed measurements. */
public final class TuningProgress1984Test {
    static int assertions;
    static final Map<String,Integer> tests=new LinkedHashMap<String,Integer>();
    static void check(boolean value,String text){assertions++;if(!value)throw new AssertionError(text);}
    static Field field(String name)throws Exception{Field f=GpuQualification1961.class.getDeclaredField(name);f.setAccessible(true);return f;}
    static int[] uniforms(int tag){return Tuning1979Test.uniforms(tag);}
    static String group(int[] u){return Tuning1979Test.group(u);}
    static String cpuGroup(int[] u){return Tuning1979Test.cpuGroup(u,2);}
    static String child(int[] u,int profile){return GpuStrong1960.profileKey1973(u,profile);}
    static void queue(int[] u){GpuStrongTuning1975.schedule(new int[64],new int[128],u,new StrongNoise1958.Model(),new GpuPolicy1960.Protection());}
    static void age()throws Exception{SaveQueue1935.idle=true;field("lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();}
    static void drain()throws Exception{
        long deadline=System.nanoTime()+15000000000L;
        while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>deadline)throw new AssertionError("proof did not release retained owners");Thread.sleep(5);}
        check(GpuNoise1960.sessions.get()==0,"GPU session closes before snapshot owner is released");
    }
    static void clean()throws Exception{
        SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged();
        if(GpuNoise1960.blockRelease!=null)GpuNoise1960.blockRelease.countDown();
        if(GpuStrongRouting1978.cpuRelease1984!=null)GpuStrongRouting1978.cpuRelease1984.countDown();
        drain();
        GpuNoise1960.blocked=GpuNoise1960.policyFailure=GpuNoise1960.failRead=GpuNoise1960.oldUnavailable=GpuNoise1960.rejectMemory=GpuNoise1960.failLease=GpuNoise1960.stallOld=false;
        GpuNoise1960.newMode=0;GpuNoise1960.blockProfile=GpuNoise1960.blockVariant=GpuNoise1960.policyFailureProfile=GpuNoise1960.policyFailureVariant=-1;
        GpuNoise1960.fastProfile1984=-1;GpuNoise1960.readsByProfile=new AtomicIntegerArray(8);StrongNoise1958.unstable=false;
        GpuStrongRouting1978.slowerCpu=false;GpuStrongRouting1978.cpuBlockProfile1984=-1;GpuStrongRouting1978.slowEarly1984=false;
    }
    static void onlyDirect(int[] u,int profile){
        for(int p=0;p<8;p++)if(p!=profile)GpuQualification1961.rejectExact1971(child(u,p),"policy_failure");
    }
    static void blockProfile(int profile,int variant){
        GpuNoise1960.blockProfile=profile;GpuNoise1960.blockVariant=variant;GpuNoise1960.blocked=true;
        GpuNoise1960.blockStarted=new CountDownLatch(1);GpuNoise1960.blockRelease=new CountDownLatch(1);
    }
    static void section(String name,int before){tests.put(name,assertions-before);}
    public static void main(String[] args)throws Exception{
        boolean diagnosticFault=args.length>0&&"diagnostic-fault".equals(args[0]);
        GpuQualification1961.initialize(new Context());clean();int before=assertions;

        int[] cooling=uniforms(1);onlyDirect(cooling,4);
        GpuQualification1961.rejectSpeed(cpuGroup(cooling));
        check(!GpuQualification1961.maySchedule(cpuGroup(cooling)),"parallel CPU speed comparison starts in cooldown");
        int cpuCalls=GpuStrongRouting1978.cpuCalls1984;
        queue(cooling);check(GpuQualification1961.retainedBytes()>0,"missing exact child is queued despite companion cooldown");
        age();drain();
        check(GpuQualification1961.restore(child(cooling,4))!=null,"two complete direct trials persist during CPU comparison cooldown");
        check(GpuStrongRouting1978.cpuCalls1984==cpuCalls,"cooldown forbids the actual CPU speed comparison");
        check(GpuStrongTuning1975.selectCpu1978(cooling,2)==null,"a child without the speed proof cannot become a foreground direct route");
        check(Tuning1979Test.retries(cpuGroup(cooling))==0,"independent exact progress spends no CPU comparison retry");
        clean();queue(cooling);
        check(GpuQualification1961.retainedBytes()==0,"completed exact progress plus cooldown avoids a redundant snapshot");
        Tuning1979Test.expire(cpuGroup(cooling));
        queue(cooling);age();drain();
        check(GpuNoise1960.readsByProfile.get(4)==0,"resume does not repeat an already certified profile's pixel trials");
        check(GpuStrongRouting1978.cpuCalls1984==cpuCalls+1,"resume executes exactly the missing CPU comparison");
        check(GpuStrongTuning1975.selectCpu1978(cooling,2)!=null,"only the completed worker-bound speed proof makes the resumed direct route usable");
        check(GpuStrongTuning1975.selectCpu1978(cooling,1)==null,"resumed speed evidence cannot cross worker-count conditions");
        section("cpu_companion_cooldown_isolated_and_exact_child_resumed",before);clean();before=assertions;

        int[] early=uniforms(2);
        for(int p=0;p<4;p++)GpuQualification1961.rejectExact1971(child(early,p),"policy_failure");
        blockProfile(5,0);queue(early);age();
        check(GpuNoise1960.blockStarted.await(6,TimeUnit.SECONDS),"next direct profile reaches controlled native wait");
        GpuStrongTuning1975.Choice first=GpuStrongTuning1975.selectCpu1978(early,2);
        check(first!=null&&first.profile==4,"complete first profile plus real-speed gate is usable before later profiles finish");
        check(GpuNoise1960.readsByProfile.get(4)==6,"first profile completed both full trials for each of its three layouts");
        check(GpuQualification1961.restore(group(early))==null,"no nonexistent legacy GPU baseline is invented");
        GpuQualification1961.Record saved=GpuQualification1961.restore(child(early,4));
        clean();
        check(GpuStrongTuning1975.selectCpu1978(early,2)!=null,"capture cancellation preserves the completed early aggregate");
        check(GpuQualification1961.restore(child(early,5))==null,"cancelled later candidate cannot certify itself");
        synchronized(field("LOCK").get(null)){((Map<?,?>)field("RECORDS").get(null)).clear();}
        GpuQualification1961.Record restored=GpuQualification1961.restore(child(early,4));
        check(restored!=null&&restored.variant==saved.variant&&restored.cpuNanos==saved.cpuNanos&&restored.gpuNanos==saved.gpuNanos,"early child restores the unchanged signed scalar proof");
        check(GpuStrongTuning1975.selectCpu1978(early,2)!=null,"early worker-bound aggregate also restores from persistence");
        section("direct_profile_checkpoint_survives_later_capture_cancellation",before);clean();before=assertions;

        int[] legacy=uniforms(3);GpuNoise1960.fastProfile1984=4;blockProfile(5,0);
        queue(legacy);age();check(GpuNoise1960.blockStarted.await(6,TimeUnit.SECONDS),"later profile pauses after a legacy-baseline checkpoint");
        first=GpuStrongTuning1975.select(legacy);
        check(first!=null&&first.profile==4,"first faster direct profile has a usable legacy-comparison aggregate before profile5");
        check(GpuNoise1960.readsByProfile.get(4)>=8,"legacy checkpoint includes two complete AB/BA fresh-upload candidate comparisons");
        saved=GpuQualification1961.restore(child(legacy,4));
        GpuQualification1961.Record aggregate=GpuQualification1961.restore(group(legacy));
        check(saved!=null&&aggregate!=null&&aggregate.variant==12+saved.variant&&aggregate.gpuNanos==saved.gpuNanos,"early legacy aggregate matches its fully compared child variant and timing");
        clean();check(GpuStrongTuning1975.select(legacy)!=null,"later capture cannot revoke a completed legacy-baseline checkpoint");
        GpuQualification1961.rejectExact1971(child(legacy,4),"confidence_mismatch");
        check(GpuStrongTuning1975.select(legacy)==null,"a new durable exact rejection still disables an early aggregate");
        check(GpuQualification1961.restore(child(legacy,4))==null,"early persistence never revives a rejected child");
        section("legacy_abba_checkpoint_and_later_exact_rejection",before);clean();before=assertions;

        int[] interrupted=uniforms(4);onlyDirect(interrupted,4);
        GpuStrongRouting1978.cpuBlockProfile1984=4;
        GpuStrongRouting1978.cpuStarted1984=new CountDownLatch(1);GpuStrongRouting1978.cpuRelease1984=new CountDownLatch(1);
        queue(interrupted);age();check(GpuStrongRouting1978.cpuStarted1984.await(6,TimeUnit.SECONDS),"speed comparison pauses after its exact child was saved");
        saved=GpuQualification1961.restore(child(interrupted,4));
        check(saved!=null&&GpuStrongTuning1975.selectCpu1978(interrupted,2)==null,"partial speed comparison cannot select the otherwise exact child");
        clean();cpuCalls=GpuStrongRouting1978.cpuCalls1984;
        queue(interrupted);age();drain();
        check(GpuNoise1960.readsByProfile.get(4)==0,"cancelled speed dependency resumes without redoing completed profile proof");
        check(GpuStrongRouting1978.cpuCalls1984==cpuCalls+1,"cancelled speed dependency receives one fresh complete comparison");
        restored=GpuQualification1961.restore(child(interrupted,4));
        check(restored!=null&&restored.variant==saved.variant&&restored.cpuNanos==saved.cpuNanos&&restored.gpuNanos==saved.gpuNanos,"resuming CPU speed does not rewrite the exact child certificate");
        check(GpuStrongTuning1975.selectCpu1978(interrupted,2)!=null,"resumed comparison completes an actual usable aggregate");
        section("cancelled_speed_stage_resumes_signed_child_without_reproof",before);clean();before=assertions;

        for(int mode=2;mode<=5;mode++){
            int[] bad=uniforms(10+mode);onlyDirect(bad,7);GpuNoise1960.newMode=mode;
            queue(bad);age();drain();
            check(GpuQualification1961.exactRejected(child(bad,7)),"full output/confidence/policy/second-trial mismatch remains durable: "+mode);
            check(GpuQualification1961.restore(child(bad,7))==null&&GpuStrongTuning1975.selectCpu1978(bad,2)==null,"mismatch cannot create an early child or aggregate: "+mode);
            if(!diagnosticFault){
                String[] history=GpuQualification1961.attemptSummary1984().split("\n");
                check(history.length>=3&&history[history.length-2].contains("今回の比較が不一致"),"the completed comparison reports a real quality mismatch, not unsupported GPU: "+mode);
            }
            clean();
        }
        int[] slow=uniforms(16);onlyDirect(slow,4);GpuStrongRouting1978.slowerCpu=true;
        queue(slow);age();drain();
        check(GpuQualification1961.restore(child(slow,4))!=null,"speed miss preserves independently established exactness");
        check(GpuStrongTuning1975.selectCpu1978(slow,2)==null&&!GpuQualification1961.maySchedule(cpuGroup(slow)),"a slower early candidate cannot pass the existing five-percent speed gate");
        check(!GpuQualification1961.exactRejected(child(slow,4)),"a speed miss is not a fabricated quality rejection");
        section("all_exact_outputs_and_five_percent_speed_gate_preserved",before);clean();before=assertions;

        int[] laterFast=uniforms(17);for(int p=0;p<4;p++)GpuQualification1961.rejectExact1971(child(laterFast,p),"policy_failure");
        GpuStrongRouting1978.slowEarly1984=true;GpuNoise1960.newMode=1;
        queue(laterFast);age();drain();
        first=GpuStrongTuning1975.selectCpu1978(laterFast,2);
        check(first!=null&&first.profile!=4,"a slower first profile does not cool down a faster later profile in the same job");
        check(Tuning1979Test.failure(cpuGroup(laterFast))==null,"successful later profile leaves no speed-failure retry debt");
        section("early_speed_miss_does_not_block_later_profile",before);clean();before=assertions;

        StrongNoise1958.Model model=new StrongNoise1958.Model();model.bytes1984=60L*1024*1024;
        GpuPolicy1960.Protection protection=new GpuPolicy1960.Protection();
        GpuStrongTuning1975.schedule(new int[64],new int[128],uniforms(20),model,protection);
        long one=GpuQualification1961.retainedBytes(),exclusive=one-model.bytes1984;
        check(exclusive>0&&exclusive<1024*1024,"first snapshot includes one complete immutable model and private strip owners");
        GpuStrongTuning1975.schedule(new int[64],new int[128],uniforms(21),model,protection);
        GpuStrongTuning1975.schedule(new int[64],new int[128],uniforms(22),model,protection);
        check(GpuQualification1961.retainedBytes()==model.bytes1984+3*exclusive,"three different strip keys charge their shared immutable model only once");
        int descriptors=GpuPolicy1960.Protection.dataCalls1984;
        StrongNoise1958.Model other=new StrongNoise1958.Model();other.bytes1984=model.bytes1984;
        long held=GpuQualification1961.retainedBytes();
        GpuStrongTuning1975.schedule(new int[64],new int[128],uniforms(23),other,protection);
        check(GpuQualification1961.retainedBytes()==held,"distinct models remain distinct owners under the strict96MiB cap");
        check(GpuPolicy1960.Protection.dataCalls1984==descriptors,"an unreservable snapshot stops before its descriptor and strip copies");
        clean();check(GpuQualification1961.retainedBytes()==0,"capture cancellation releases every private strip and the final shared model owner");
        section("shared_model_and_real_reservation_precede_snapshot_copy",before);

        StringBuilder out=new StringBuilder("{\"status\":\"passed\",\"assertions\":").append(assertions).append(",\"tests\":{");boolean comma=false;
        for(Map.Entry<String,Integer> entry:tests.entrySet()){if(comma)out.append(',');comma=true;out.append('"').append(entry.getKey()).append("\":").append(entry.getValue());}
        System.out.println(out.append("}}").toString());
    }
}
