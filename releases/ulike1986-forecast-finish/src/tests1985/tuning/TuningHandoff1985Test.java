package com.hiro.ulike;

import android.content.Context;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;

/** Production tuner, parallel comparator, qualification and snapshot ownership.
 * The transport and CPU oracle are controlled host peers, not Android timings. */
public final class TuningHandoff1985Test {
    static int assertions;
    static boolean diagnosticFault;
    static final Map<String,Integer> tests=new LinkedHashMap<String,Integer>();
    static void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    static Field field(String name)throws Exception{Field f=GpuQualification1961.class.getDeclaredField(name);f.setAccessible(true);return f;}
    static int[] uniforms(int tag){int[] u=new int[32];u[0]=u[1]=u[3]=u[5]=u[7]=8;u[6]=tag;u[8]=4;u[9]=u[11]=u[12]=1;u[10]=3;u[18]=tag;return u;}
    static String child(int[] u,int profile){return GpuStrong1960.profileKey1973(u,profile);}
    static String group(int[] u){String k=child(u,0);return "strong-gx1978-tuning-policy-bank-v1:"+k.substring(k.indexOf(':')+1);}
    static String cpuGroup(int[] u,int workers){return group(u).replace("strong-gx1978-tuning-","strong-gx1978-cpu-tuning-")+":"+workers;}
    static void permit(int[] u,int... profiles){for(int p=0;p<8;p++){boolean allowed=false;for(int v:profiles)allowed|=p==v;if(!allowed)GpuQualification1961.rejectExact1971(child(u,p),"policy_failure");}}
    static void queue(int[] u){GpuStrongTuning1975.schedule1978(new int[64],new int[128],u,new StrongNoise1958.Model(),new GpuPolicy1960.Protection(),SpeedWorkers1935.workers1985);}
    static void age()throws Exception{SaveQueue1935.idle=true;field("lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();}
    static void drain()throws Exception{long deadline=System.nanoTime()+15000000000L;while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>deadline)throw new AssertionError("qualification ownership did not drain");Thread.sleep(5);}check(GpuNoise1960.sessions.get()==0,"all real comparator sessions close before the snapshot is released");}
    static void clean()throws Exception{
        SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged();
        if(GpuNoise1960.blockRelease!=null)GpuNoise1960.blockRelease.countDown();
        if(GpuStrong1960.cohortRelease1985!=null)GpuStrong1960.cohortRelease1985.countDown();
        drain();
        GpuNoise1960.blocked=GpuNoise1960.policyFailure=GpuNoise1960.failRead=GpuNoise1960.oldUnavailable=GpuNoise1960.rejectMemory=GpuNoise1960.failLease=GpuNoise1960.stallOld=false;
        GpuNoise1960.newMode=0;GpuNoise1960.blockProfile=GpuNoise1960.blockVariant=GpuNoise1960.policyFailureProfile=GpuNoise1960.policyFailureVariant=-1;
        GpuNoise1960.fastProfile1984=4;GpuNoise1960.readsByProfile=new AtomicIntegerArray(8);
        GpuNoise1960.lateFailure1985=new int[8];GpuNoise1960.lateAfter1985=new int[8];
        GpuStrong1960.cohortFailureProfile1985=-1;GpuStrong1960.cohortFailure1985=0;GpuStrong1960.cohortBlock1985=false;
        GpuStrong1960.cohortReads1985.set(0);StrongNoise1958.unstable=false;StrongNoise1958.cpuDelay1985=40;
        SpeedWorkers1935.workers1985=2;SpeedWorkers1935.runs1985.set(0);
    }
    static String last(){String[] lines=GpuQualification1961.attemptSummary1984().split("\n");return lines.length<3?"":lines[lines.length-2];}
    static void section(String name,int start){tests.put(name,assertions-start);}

    static void lastBaseline()throws Exception{
        int start=assertions;
        for(int mode=1;mode<=3;mode++){
            clean();int[] u=uniforms(10+mode);permit(u,1,4);
            GpuNoise1960.lateFailure1985[1]=mode;GpuNoise1960.lateAfter1985[1]=6;
            queue(u);age();drain();
            check(GpuQualification1961.exactRejected(child(u,1)),"a real late legacy ARGB/confidence/policy mismatch is retained: "+mode);
            check(GpuQualification1961.restore(child(u,1))==null,"the rejected legacy child cannot remain usable");
            GpuQualification1961.Record exact=GpuQualification1961.restore(child(u,4));
            check(exact!=null&&!GpuQualification1961.exactRejected(child(u,4)),"the independently exact direct profile survives a different profile's failure");
            GpuStrongTuning1975.Choice selected=GpuStrongTuning1975.selectCpu1978(u,2);
            check(selected!=null&&selected.profile==4&&selected.variant==exact.variant,"last rejected legacy baseline must hand the exact direct child to the real parallel CPU comparison");
            check(SpeedWorkers1935.runs1985.get()==4&&GpuStrong1960.cohortReads1985.get()==4,"CPU/GPU AB/BA really executes both bounded worker trials");
            check(GpuStrongTuning1975.selectCpu1978(u,1)==null&&GpuStrongTuning1975.selectCpu1978(u,3)==null,"recovered parallel proof cannot cross worker counts");
            check(GpuStrongTuning1975.select(u)==null,"no missing legacy GPU proof is fabricated");
        }
        section("last_legacy_rejection_reaches_real_parallel_cpu",start);
    }
    static void alternateBaseline()throws Exception{
        clean();int start=assertions;int[] u=uniforms(20);permit(u,1,2,4);
        GpuNoise1960.lateFailure1985[2]=1;GpuNoise1960.lateAfter1985[2]=6;
        queue(u);age();drain();
        check(GpuQualification1961.exactRejected(child(u,2)),"the initially faster legacy comparison reference really fails");
        GpuStrongTuning1975.Choice selected=GpuStrongTuning1975.select(u);
        check(selected!=null&&selected.profile==4,"another retained exact legacy baseline must complete the direct candidate's missing speed proof");
        check(GpuNoise1960.readsByProfile.get(1)>=8,"the replacement legacy reference was measured in both fresh-upload orders");
        check(SpeedWorkers1935.runs1985.get()==0,"an available exact legacy baseline does not borrow a CPU comparison");
        GpuQualification1961.Record aggregate=GpuQualification1961.restore(group(u)),exact=GpuQualification1961.restore(child(u,4));
        check(aggregate!=null&&exact!=null&&aggregate.variant==12+exact.variant&&aggregate.gpuNanos==exact.gpuNanos,"replacement baseline selects the fully compared child and variant");
        section("invalid_legacy_reference_reselects_another_exact_baseline",start);
    }
    static void cohortTerminal()throws Exception{
        int start=assertions;
        for(int mode=1;mode<=3;mode++){
            clean();int[] u=uniforms(30+mode);permit(u,1);SpeedWorkers1935.workers1985=3;
            GpuStrong1960.cohortFailureProfile1985=1;GpuStrong1960.cohortFailure1985=mode;
            queue(u);age();drain();
            check(GpuStrong1960.cohortReads1985.get()>0,"the production optional cohort comparator really ran");
            check(GpuQualification1961.exactRejected(child(u,1)),"the production cohort comparator records its true exact mismatch: "+mode);
            check(GpuQualification1961.restore(group(u))==null,"completion must retire the aggregate whose exact child failed in the optional cohort");
            check(GpuStrongTuning1975.select(u)==null&&GpuStrongTuning1975.selectCpu1978(u,3)==null,"a terminal mismatch cannot leave an applicable aggregate");
            if(!diagnosticFault)check(last().contains("今回の比較が不一致")&&!last().contains("認定記録を作成"),"cohort rejection must not finish with the old successful qualification label: "+last());
        }
        section("cohort_exact_rejection_rechecks_terminal_selection",start);
    }
    static void cohortFallback()throws Exception{
        clean();int start=assertions;int[] u=uniforms(40);permit(u,1,2);SpeedWorkers1935.workers1985=3;
        GpuStrong1960.cohortFailureProfile1985=2;GpuStrong1960.cohortFailure1985=1;
        queue(u);age();drain();
        check(GpuQualification1961.exactRejected(child(u,2)),"the selected faster legacy child is invalidated by its cohort output");
        GpuStrongTuning1975.Choice selected=GpuStrongTuning1975.select(u);
        check(selected!=null&&selected.profile==1,"completion preserves a different fully exact legacy route when one remains");
        check(!GpuQualification1961.exactRejected(child(u,1))&&GpuQualification1961.restore(child(u,1))!=null,"fallback never erases another profile's valid proof");
        check(!GpuStrongRouting1978.accepted(u,1,selected.variant,3),"a fallback aggregate cannot fabricate a mixed-cohort speed hint");
        if(!diagnosticFault)check(last().contains("認定記録を作成"),"successful terminal replacement has a matching current aggregate");
        section("cohort_failure_preserves_an_independent_legacy_route",start);
    }
    static void cohortSpeed()throws Exception{
        clean();int start=assertions;int[] u=uniforms(41);permit(u,1);SpeedWorkers1935.workers1985=3;StrongNoise1958.cpuDelay1985=80;
        queue(u);age();drain();GpuStrongTuning1975.Choice selected=GpuStrongTuning1975.select(u);
        check(selected!=null&&selected.profile==1,"a slow optional mixed cohort does not invalidate its exact base route");
        check(!GpuQualification1961.exactRejected(selected.key),"cohort speed is not a fake quality rejection");
        check(!GpuStrongRouting1978.accepted(u,1,selected.variant,3),"the slower mixed cohort cannot pass the unchanged five-percent gate");
        if(!diagnosticFault)check(last().contains("認定記録を作成"),"optional speed failure still reports the valid base selection");
        section("optional_cohort_speed_miss_preserves_base_proof",start);
    }
    static void handoffBinding()throws Exception{
        clean();int start=assertions;int[] u=uniforms(42);permit(u,4);queue(u);age();drain();
        GpuStrongTuning1975.Choice selected=GpuStrongTuning1975.selectCpu1978(u,2);
        check(selected!=null&&selected.profile==4,"standalone direct recovery completes the real worker-bound comparison");
        check(GpuStrongTuning1975.selectCpu1978(u,1)==null&&GpuStrongTuning1975.selectCpu1978(u,3)==null,"worker count is part of the stored condition");
        int[] other=u.clone();other[18]++;
        check(GpuStrongTuning1975.selectCpu1978(other,2)==null,"different protection conditions cannot inherit the aggregate");
        GpuQualification1961.Record exact=GpuQualification1961.restore(selected.key);
        GpuQualification1961.qualifiedStrongPreferred1970(selected.key,exact.cpuNanos,exact.gpuNanos,(exact.variant+1)%3);
        check(GpuStrongTuning1975.selectCpu1978(u,2)==null&&GpuQualification1961.restore(cpuGroup(u,2))==null,"a changed authoritative child variant retires only the stale aggregate");
        check(GpuQualification1961.restore(selected.key)!=null&&!GpuQualification1961.exactRejected(selected.key),"stale aggregate retirement is not a fabricated exact rejection");
        section("worker_condition_child_variant_handoff_is_authoritative",start);
    }
    static void cancelledCohort()throws Exception{
        clean();int start=assertions;int[] u=uniforms(43);permit(u,1);SpeedWorkers1935.workers1985=3;
        GpuStrong1960.cohortBlock1985=true;GpuStrong1960.cohortStarted1985=new CountDownLatch(1);GpuStrong1960.cohortRelease1985=new CountDownLatch(1);
        queue(u);age();check(GpuStrong1960.cohortStarted1985.await(6,TimeUnit.SECONDS),"production optional cohort reaches its controlled readback wait");
        GpuStrongTuning1975.Choice before=GpuStrongTuning1975.select(u);check(before!=null,"completed base aggregate exists before optional comparison cancellation");
        clean();GpuStrongTuning1975.Choice after=GpuStrongTuning1975.select(u);
        check(after!=null&&after.profile==before.profile&&after.variant==before.variant,"capture cancellation preserves its already completed exact aggregate");
        check(!GpuQualification1961.exactRejected(after.key),"cancellation does not invent a quality mismatch");
        if(!diagnosticFault)check(last().contains("撮影変更・中断で取消"),"cancellation remains the terminal cause despite completed earlier proof");
        section("optional_cohort_cancellation_keeps_completed_progress",start);
    }
    static void safetyBoundaries()throws Exception{
        clean();int start=assertions;int[] bad=uniforms(44);permit(bad,1,4);
        GpuNoise1960.lateFailure1985[4]=1;
        queue(bad);age();drain();
        check(GpuQualification1961.exactRejected(child(bad,4))&&GpuQualification1961.restore(child(bad,4))==null,"an actually mismatching direct candidate cannot use CPU fallback to bypass rejection");
        check(GpuStrongTuning1975.selectCpu1978(bad,2)==null&&SpeedWorkers1935.runs1985.get()==0,"bad direct output never enters speed-only recovery");
        clean();int[] slow=uniforms(45);permit(slow,1,4);StrongNoise1958.cpuDelay1985=0;
        GpuNoise1960.lateFailure1985[1]=1;GpuNoise1960.lateAfter1985[1]=6;
        queue(slow);age();drain();
        check(SpeedWorkers1935.runs1985.get()==4,"after baseline invalidation the real CPU speed comparator still executes");
        check(GpuQualification1961.restore(child(slow,4))!=null&&!GpuQualification1961.exactRejected(child(slow,4)),"speed-only failure preserves the independently exact direct output proof");
        check(GpuStrongTuning1975.selectCpu1978(slow,2)==null&&!GpuQualification1961.maySchedule(cpuGroup(slow,2)),"the recovery route cannot relax the actual CPU five-percent margin or cooldown");
        clean();int[] cooling=uniforms(46);permit(cooling,1,4);GpuQualification1961.rejectSpeed(cpuGroup(cooling,2));
        GpuNoise1960.lateFailure1985[1]=1;GpuNoise1960.lateAfter1985[1]=6;
        queue(cooling);age();drain();
        check(GpuQualification1961.exactRejected(child(cooling,1))&&GpuQualification1961.restore(child(cooling,4))!=null,"legacy invalidation can coexist with retained exact direct progress during CPU cooldown");
        check(SpeedWorkers1935.runs1985.get()==0&&GpuStrongTuning1975.selectCpu1978(cooling,2)==null,"baseline recovery cannot spend a forbidden worker-bound CPU retry");
        section("exact_rejections_cpu_margin_and_cooldown_are_preserved",start);
    }
    public static void main(String[] args)throws Exception{
        String selected=args.length>0?args[0]:"all";diagnosticFault="diagnostic-fault".equals(selected);GpuQualification1961.initialize(new Context());
        if("last-baseline".equals(selected))lastBaseline();
        else if("cohort-terminal".equals(selected))cohortTerminal();
        else if("alternate-baseline".equals(selected))alternateBaseline();
        else {lastBaseline();alternateBaseline();cohortTerminal();cohortFallback();cohortSpeed();handoffBinding();cancelledCohort();safetyBoundaries();}
        clean();StringBuilder out=new StringBuilder("{\"status\":\"passed\",\"assertions\":").append(assertions).append(",\"tests\":{");boolean comma=false;
        for(Map.Entry<String,Integer> entry:tests.entrySet()){if(comma)out.append(',');comma=true;out.append('"').append(entry.getKey()).append("\":").append(entry.getValue());}
        System.out.println(out.append("}}").toString());
    }
}
