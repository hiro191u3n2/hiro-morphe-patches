package com.hiro.ulike;

import android.graphics.Bitmap;
import static com.hiro.ulike.FinishFailures1988Test.*;

/** Calls the actual Finish renderer graph and the actual FinishMemory helper.
 * Native transport/budget outcomes are named host inputs. Timed renderer work
 * advances the explicit clock; no proof or speed result is supplied by peers. */
public final class FinishMemoryGraph1989Test {
    static boolean old;
    static int scalar(String name)throws Exception{return integer(attempt(),name);}
    static long value(String name)throws Exception{Object a=attempt();return field(a,name).getLong(a);}
    static void resetGraph(int mode,int stage,long cost)throws Exception{reset();FinishStageControl1988.reset(stage,0);FinishMemoryControl1989.reset(mode,cost);}
    static void clean(){
        check(FinishStageControl1988.activeSessions==0,"actual Finish graph releases every active GPU session");
        check(FinishStageControl1988.activeLeases==0,"actual image/geometry/bank lease ownership is fully released");
        check(FinishStageControl1988.activePolicies==0,"actual prepared-policy ownership is released");
        check(FinishStageControl1988.activeTickets==0,"actual session cleanup drains outstanding tickets");
        check(Bitmap.writesAfterRecycle.get()==0,"no output row writes occur after recycle");
        check(GpuQualification1961.retainedBytes()==0,"queue source ownership released after failure or success");
    }
    static void allRefused()throws Exception{
        resetGraph(1,0,400);String key=execute();
        check(FinishControl1986.cpuCalls==(old?2:1),"published88 repeats CPU reference after four real stage8 refusals; current89 skips it");
        check(FinishStageControl1988.opens==0&&FinishStageControl1988.submitted==0,"stage8 rejection precedes every GPU session or submission");
        check(FinishMemoryControl1989.checks==(old?4:8)&&FinishMemoryControl1989.trims==(old?0:4),"at most one bounded relief and recheck per actually refused candidate");
        counts(6,0,1);check(GpuQualification1961.restore(key)==null,"real stage8 memory failure cannot produce a speed certificate");
        for(int i=0;i<4;i++){
            check(reason(i)==4&&trials("finishExact1986",i)==0&&trials("finishSpeed1986",i)==0,"all candidates preserve typed zero-output memory failure");
            if(!old){
                check(((scalar("finishStages1988")>>>(5*i))&31)==8,"failed actual guard records precise extra-workspace stage");
                check(((scalar("finishMemoryChecks1989")>>>(2*i))&3)==2,"each real refusal records exactly two original guard calls");
                check(((scalar("finishMemoryTrim1989")>>>(3*i))&7)==1,"per-candidate native relief attempt is explicit");
                check((scalar("finishMemoryFinal1989")&(1<<i))==0,"diagnostics never turn failed recheck into success");
            }
        }
        if(!old)check(scalar("finishMemorySeen1989")==15&&scalar("finishOracleSkipped1989")==1,"all four reports survive terminal skip");
        clean();
    }
    static void permittedAndOtherStages()throws Exception{
        resetGraph(0,0,400);String key=execute();
        check(GpuQualification1961.restore(key)!=null&&FinishControl1986.cpuCalls==2,"already affordable renderers retain full exact and speed proofs");
        check(FinishMemoryControl1989.trims==0&&FinishMemoryControl1989.samples==0&&scalar("finishMemorySeen1989")==0,"affordable background guard performs no unnecessary native relief or report allocation");clean();
        resetGraph(0,13,400);key=execute();
        check(FinishMemoryControl1989.trims==0&&FinishMemoryControl1989.samples==0,"later geometry-workspace refusal cannot trigger stage8-only relief");
        check(GpuQualification1961.restore(key)==null&&reason(1)==4,"later memory failure remains its original typed refusal");clean();
        resetGraph(1,0,400);
        Bitmap input=FinishControl1986.image(29,19,false),expected=FinishControl1986.image(W,H,false);
        try{
            int result=GpuChain1961.compareFinish1988(input,0,W,H,plan(),true,expected,32,true,null,false,()->false);
            check(result==GpuChain1961.FINISH_MEMORY1988,"foreground comparator retains original memory guard failure");
            check(FinishMemoryControl1989.checks==1&&FinishMemoryControl1989.trims==0&&FinishMemoryControl1989.samples==0,"foreground runFinishStages cannot use background relief or observation");
        }finally{input.recycle();expected.recycle();Bitmap.ALL.clear();}clean();
    }
    static void reliefIsTimed()throws Exception{
        for(long cost:new long[]{10,400}){
            resetGraph(2,0,cost);String key=execute();
            check(FinishMemoryControl1989.trims==16&&FinishMemoryControl1989.checks==32,"each of sixteen actual comparison/output calls receives at most one relief");
            check(FinishControl1986.cpuCalls==2&&scalar("finishOracleSkipped1989")==0,"relief success preserves both necessary CPU references");
            for(int i=0;i<4;i++){
                check(trials("finishExact1986",i)==2&&trials("finishSpeed1986",i)==2,"recovered capacity alone never replaces either full proof or actual output");
                check(((scalar("finishStages1988")>>>(5*i))&31)==24,"recovered render path completes normally");
            }
            check(value("finishCpu11986")==1000,"CPU baseline excludes optional diagnostics and GPU memory relief");
            check(value("finishGpu11986")==700+cost&&value("finishLegacy11986")==800+cost,"actual output GPU and old-GPU timings include complete memory-relief work");
            if(cost==10){GpuQualification1961.Record proof=GpuQualification1961.restore(key);
                check(proof!=null&&proof.cpuNanos==810&&proof.gpuNanos==710,"valid recovered output certifies against measured old-GPU baseline including trim");}
            else {check(GpuQualification1961.restore(key)==null&&GpuQualification1961.restore(legacy(key))==null,"trim cost cannot be hidden to manufacture a fast GPU candidate");counts(11,2,0);}
            clean();
        }
    }
    static void busyCancelledAndOptional()throws Exception{
        for(int mode:new int[]{3,4,5,6}){
            resetGraph(mode,0,10);String key=execute();
            if(mode==4){counts(4,0,0);check(failure(key)==null&&FinishMemoryControl1989.trims==0,"cancellation before relief spends neither memory work nor retry");}
            else if(mode==6){check(GpuQualification1961.restore(key)!=null,"optional native cleanup exception cannot override unchanged successful recheck");
                check(FinishMemoryControl1989.trims==16&&FinishMemoryControl1989.checks==32,"partial cleanup exception still allows only one existing-guard recheck per call");}
            else {counts(6,0,1);check(GpuQualification1961.restore(key)==null,"busy owner keeps conservative existing guard");
                check(FinishMemoryControl1989.checks==4&&FinishMemoryControl1989.trims==(mode==3?0:4),"active GPU or busy CPU is not repeatedly probed or reclaimed");}
            int status=mode==3?3:mode==4?4:mode==5?2:7;
            check((scalar("finishMemoryTrim1989")&7)==status,"precise bounded relief outcome attached to actual owner");
            clean();
        }
    }
    public static void main(String[] args)throws Exception{
        old=args.length>0&&args[0].equals("published88");baseline=false;assertions=0;
        allRefused();if(!old){permittedAndOtherStages();reliefIsTimed();busyCancelledAndOptional();}
        reset();System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+
            ",\"actual_finish_stage_graph\":true,\"actual_memory_helper\":"+(!old)+",\"physical_android_tested\":false}");
    }
}
