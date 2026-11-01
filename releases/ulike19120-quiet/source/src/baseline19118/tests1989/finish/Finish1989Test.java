package com.hiro.ulike;

import android.graphics.Bitmap;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import static com.hiro.ulike.FinishFailures1988Test.*;

/** Executes the current queue and Finish qualification. Renderer outcomes and
 * elapsed clock are controlled by the frozen .86 seam, never the admission.
 * The published .88 code is an independently executed negative control. */
public final class Finish1989Test {
    static final Map<String,Integer> sections=new LinkedHashMap<String,Integer>();
    static boolean old;
    static void section89(String name,int start){sections.put(name,assertions-start);}
    static int scalar(String name)throws Exception{return integer(attempt(),name);}
    static long number(String name)throws Exception{Object a=attempt();return field(a,name).getLong(a);}
    static Map<String,Object> scalars(Object value)throws Exception{
        Map<String,Object> out=new TreeMap<String,Object>();
        for(Field f:value.getClass().getDeclaredFields()){f.setAccessible(true);check(f.getType().isPrimitive(),"attempt retains only primitive scalars");out.put(f.getName(),f.get(value));}
        return out;
    }
    static void sameScalars(Object value,Map<String,Object> before)throws Exception{
        for(Field f:value.getClass().getDeclaredFields()){f.setAccessible(true);check(Objects.equals(before.get(f.getName()),f.get(value)),"snapshot field stays immutable: "+f.getName());}
    }
    static void noViable()throws Exception{
        int start=assertions;
        for(int mode=0;mode<6;mode++){
            reset();allCompare(mode==1?5:mode==2?2:1);
            if(mode==3){allCompare(0);for(int i=0;i<4;i++)FinishControl1986.outputFault[i][0]=1;}
            if(mode==4){FinishControl1986.compareFault[0][0]=2;FinishControl1986.compareFault[1][0]=5;FinishControl1986.compareFault[3][0]=4;}
            Bitmap input=FinishControl1986.image(29,19,false);QualityPixels1932.Plan p=plan();String key=key(input,p);
            if(mode==5)GpuQualification1961.rejectExact(legacy(key));input.recycle();Bitmap.ALL.clear();
            key=execute();
            check(FinishControl1986.cpuCalls==(old?2:1),"all-terminal "+mode+" performs "+(old?"published redundant two":"one useful")+" CPU oracle(s)");
            check(total(FinishControl1986.comparisons)==(mode==5?3:4),"each eligible first comparison runs exactly once");
            check(total(FinishControl1986.outputs)==(mode==3?4:0),"unavailable comparison never invents a real output timing");
            check(GpuQualification1961.restore(key)==null,"zero viable candidates cannot qualify");
            counts(mode==1?6:mode==2?12:mode==4?14:8,0,1);
            check(failure(key)!=null&&!field(failure(key),"exact").getBoolean(failure(key)),"same nonexact bounded retry remains");
            check(GpuQualification1961.exactRejected(legacy(key))==(mode==2||mode==4||mode==5),"existing old-GPU exact journal is unchanged");
            if(!old){
                check(scalar("finishOracleRuns1989")==1&&scalar("finishOracleSkipped1989")==1,"actual oracle/skipped counts explain missing second trial");
                for(int i=0;i<4;i++){check(reason(i)>1,"no unattempted viable candidate is skipped");
                    check(trials("finishExact1986",i)==(mode==3?1:0)&&trials("finishSpeed1986",i)==0,"partial proof never upgraded to two trials");}
                String text=GpuQualification1961.attemptSummary1986();check(text.contains("CPU基準の実行 1回 / 継続候補がなく省略 1回"),"skip shown separately from GPU failure");
                check(text.contains("入力 29×19 → 出力 37×23"),"actual source and oracle output shape are shown");
            }
        }
        section89(old?"published88_redundant_second_oracle_negative":"no_candidate_skips_only_second_oracle",start);
    }
    static void survivors()throws Exception{
        int start=assertions;
        for(int mode=0;mode<6;mode++){
            reset();allCompare(1);int survivor=mode==0?0:mode==1?1:mode==2?2:mode==3?3:2;
            FinishControl1986.compareFault[survivor][0]=0;
            if(mode==4)FinishControl1986.outputFault[survivor][1]=1;
            if(mode==5)FinishControl1986.compareFault[survivor][1]=2;
            String key=execute();
            check(FinishControl1986.cpuCalls==2&&scalar("finishOracleRuns1989")==2&&scalar("finishOracleSkipped1989")==0,"one surviving old or new candidate retains both CPU references");
            check(FinishControl1986.comparisons[survivor]==2,"survivor receives both full output comparisons");
            if(mode<4){
                GpuQualification1961.Record proof=GpuQualification1961.restore(survivor==0?legacy(key):key);
                check(proof!=null,"valid surviving candidate still qualifies");
                check(FinishControl1986.outputs[survivor]==2&&FinishControl1986.comparedPixels[survivor]==2L*W*H,"two entire outputs are compared and independently timed");
                check(trials("finishExact1986",survivor)==2&&trials("finishSpeed1986",survivor)==2,"both counters remain required");
            }else{
                check(GpuQualification1961.restore(key)==null,"second comparison or actual-output failure cannot certify");
                check(trials("finishSpeed1986",survivor)==1,"one completed timing cannot masquerade as two");
                check(reason(survivor)==(mode==4?3:2),"second trial keeps exact typed cause");
            }
        }
        reset();FinishControl1986.unstable=true;execute();
        check(FinishControl1986.cpuCalls==2&&reason(1)==10&&FinishControl1986.comparisons[1]==1,"CPU digest stability remains necessary when a candidate survives");
        section89("one_legacy_or_new_survivor_preserves_both_proof_and_output_trials",start);
    }
    static void cancelledBoundary()throws Exception{
        int start=assertions;
        for(int boundary:new int[]{6,7}){
            reset();allCompare(1);Bitmap input=FinishControl1986.image(29,19,false);QualityPixels1932.Plan p=plan();String key=key(input,p);
            AtomicInteger afterLast=new AtomicInteger();
            Probe probe=start(key,c->GpuChain1961.qualifyFinish1978(input,0,W,H,p,true,()->c.cancelled()||
                total(FinishControl1986.comparisons)==4&&afterLast.incrementAndGet()>=boundary));
            end(probe);counts(4,0,0);
            check(FinishControl1986.cpuCalls==1,"cancellation at trial boundary avoids any extra CPU reference");
            check(scalar("finishOracleSkipped1989")== (boundary==6?0:1),"cancellation check precedes no-candidate break");
            check(failure(key)==null&&GpuQualification1961.maySchedule(key),"boundary cancellation consumes no retry or cooldown");
            check(GpuQualification1961.restore(key)==null,"canceled boundary cannot write certificate");
            synchronized(Bitmap.ALL){for(Bitmap b:Bitmap.ALL)if(b!=input)check(b.isRecycled(),"all oracle temporaries released across break/cancel");}
            input.recycle();Bitmap.ALL.clear();
        }
        section89("cancel_before_and_after_break_preserves_finally_and_retry",start);
    }
    static long[] memory(int candidate){
        long[] values=new long[33];Arrays.fill(values,-1);
        values[0]=1;values[1]=(260L+candidate*6)*1024*1024;values[2]=0;values[3]=1;values[4]=candidate==2?1:0;
        values[5]=2;values[6]=1;values[7]=12345;values[8]=3;
        for(int base:new int[]{9,21}){
            values[base]=512L*1024*1024;values[base+1]=200L*1024*1024;values[base+2]=312L*1024*1024;
            values[base+3]=4L*1024*1024;values[base+4]=5L*1024*1024;values[base+5]=6L*1024*1024;
            values[base+6]=30L*1024*1024;values[base+7]=10L*1024*1024;values[base+8]=12L*1024*1024;values[base+9]=8L*1024*1024;
            values[base+10]=2L*1024*1024;values[base+11]=(base==9?201L:270L)*1024*1024;
        }
        return values;
    }
    static void snapshots()throws Exception{
        int start=assertions;reset();ProcessingTiming1947.capture1989=1791670635624L;long epoch=ProcessingTiming1947.epoch;
        final String privateKey="fixture|finish-chain1976:v2|private-source-condition";
        Probe probe=new Probe(c->{
            GpuQualification1961.finishShape1989(3060,4080,4284,5712,0);
            GpuQualification1961.finishOracle1989(true);GpuQualification1961.finishOracle1989(false);
            for(int slot=0;slot<4;slot++){
                GpuQualification1961.finishBegin1988(slot-1,2);GpuQualification1961.finishStage1988(8);
                long[] report=memory(slot);GpuQualification1961.finishMemory1989(report);Arrays.fill(report,Long.MAX_VALUE);
                check(number("finishRequested"+slot+"1989")== (260L+slot*6)*1024*1024,"diagnostic copies each source scalar without retaining array alias");
                GpuQualification1961.finishCandidate1986(slot-1,4,0,0,1,1000,0,0);
            }
            check(number("captureId1989")==1791670635624L&&number("epoch")==epoch,"queued owner retains original capture identity despite newer displayed identity");
            check(number("finishBudgetBeforeAvailable1989")==201L*1024*1024&&number("finishBudgetAfterAvailable1989")==270L*1024*1024,"last per-owner budget sample copied independently");
            Object a=attempt();Map<String,Object> stable=scalars(a);
            GpuQualification1961.finishShape1989(1,1,2,2,90);sameScalars(a,stable);
            long[] malformed=memory(0);malformed[0]=2;GpuQualification1961.finishMemory1989(malformed);
            malformed=memory(0);malformed[5]=3;GpuQualification1961.finishMemory1989(malformed);
            malformed=memory(0);malformed[8]=4;GpuQualification1961.finishMemory1989(malformed);
            malformed=memory(0);malformed[9]=-2;GpuQualification1961.finishMemory1989(malformed);
            GpuQualification1961.finishMemory1989(null);GpuQualification1961.finishMemory1989(new long[32]);sameScalars(a,stable);
            // This is the real active CURRENT owner: unlike a main-thread late
            // call, it proves the finished guard itself rejects mutation.
            field(a,"finished").setBoolean(a,true);
            try{
                Map<String,Object> terminal=scalars(a);
                GpuQualification1961.finishShape1989(7,9,11,13,180);GpuQualification1961.finishOracle1989(true);GpuQualification1961.finishOracle1989(false);
                GpuQualification1961.finishMemory1989(memory(1));
                GpuQualification1961.finishCpu1988(2,15);GpuQualification1961.finishBegin1988(0,3);GpuQualification1961.finishStage1988(24);
                GpuQualification1961.finishFault1988(4,15);GpuQualification1961.finishEnd1988();
                sameScalars(a,terminal);
            }finally{field(a,"finished").setBoolean(a,false);}
            GpuQualification1961.finishEnd1988();
        });
        check(GpuQualification1961.schedule(privateKey,1,probe),"capture diagnostic owner admitted before worker starts");
        ProcessingTiming1947.capture1989=1791670639999L;quiet();end(probe);
        String ended=GpuQualification1961.attemptSummary1986();Map<String,Object> terminal=scalars(attempt());
        ProcessingTiming1947.epoch++;ProcessingTiming1947.capture1989=1791670648888L;
        GpuQualification1961.finishShape1989(1,1,1,1,0);GpuQualification1961.finishOracle1989(true);GpuQualification1961.finishMemory1989(memory(0));
        check(ended.equals(GpuQualification1961.attemptSummary1986()),"later capture cannot rewrite ended Finish identity or memory");sameScalars(attempt(),terminal);
        check(ended.contains("1791670635624")&&!ended.contains("1791670639999")&&!ended.contains("1791670648888"),"rendering uses captured owner, not current photo");
        check(ended.contains("3060×4080 → 出力 4284×5712")&&ended.contains("不足判定後の観測空き")&&ended.contains("ヒープ上限 512.0 MiB"),"summary exposes shape and correctly labeled post-guard observations");
        check(ended.contains("必要 260.0 MiB")&&ended.contains("必要 278.0 MiB"),"all four candidate requested capacities remain distinct");
        check(ended.contains("物理 RAM の残量ではありません"),"heap-and-owner budget is not mislabeled physical RAM");
        check(!ended.contains("private-source-condition")&&!CameraTrace1965.events.toString().contains("private-source-condition"),"no raw source, key or exception retained");
        int records=0;for(String event:CameraTrace1965.events)if(event.startsWith("gpu_finish89_")){
            records++;int split=event.indexOf(' ',event.indexOf(' ')+1);check(event.length()-split-1<=160,"new scalar events fit existing 160-character payload cap");}
        check(records>0&&records<=16,"one bounded set of event records per candidate observation");
        reset();ProcessingTiming1947.capture1989=0;allCompare(1);execute();check(number("captureId1989")==0&&GpuQualification1961.attemptSummary1986().contains("撮影 #未観測"),"missing identity remains explicitly unknown");
        section89("captured_owner_shape_memory_copy_finite_events_and_active_terminal_guard",start);
    }
    static void optionalFaults()throws Exception{
        int start=assertions;reset();String key=execute();GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        check(proof!=null&&proof.cpuNanos==800&&proof.gpuNanos==700,"throwing new diagnostics does not affect actual CPU/GPU baseline fairness");
        reset();allCompare(1);key=execute();check(FinishControl1986.cpuCalls==1&&GpuQualification1961.restore(key)==null,"skip is operational and independent of optional oracle counters");counts(8,0,1);
        section89("new_optional_api_faults_cannot_change_skip_or_certificate",start);
    }
    public static void main(String[] args)throws Exception{
        old=args.length>0&&args[0].equals("published88");baseline=false;assertions=0;
        boolean faults=args.length>0&&args[0].startsWith("fault-");
        if(faults)optionalFaults();else{
            noViable();
            if(!old){survivors();cancelledBoundary();snapshots();
                int start=assertions;failureMatrix();measuredAndMixed();legacyAndProofs();cancellation();retryAndPersistence();
                section89("unchanged_frozen88_failure_retry_exact_and_five_percent_assertions",start);}
        }
        reset();StringBuilder out=new StringBuilder("{\"status\":\"passed\",\"assertions\":").append(assertions).append(",\"cases\":{");boolean comma=false;
        for(Map.Entry<String,Integer> e:sections.entrySet()){if(comma)out.append(',');comma=true;out.append('"').append(e.getKey()).append("\":").append(e.getValue());}
        System.out.println(out.append("}}").toString());
    }
}
