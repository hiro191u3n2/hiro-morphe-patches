package com.hiro.ulike;

import android.graphics.Bitmap;
import java.lang.reflect.*;

/** Actual runFinishStages/bank/readback/ResidentProof control, with named
 * transport refusals. These are host command-ownership tests, not GPU images. */
public final class FinishStages1988Test {
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static int value(String name)throws Exception{return FinishFailures1988Test.integer(FinishFailures1988Test.attempt(),name);}
    static void clean(){check(FinishStageControl1988.activeSessions==0,"session released at every stopping point");
        check(FinishStageControl1988.activeLeases==0,"all admitted image/geometry/bank leases closed exactly once");
        check(FinishStageControl1988.activePolicies==0,"prepared policy closed on submit/readback failure");
        check(FinishStageControl1988.activeTickets==0,"session drains or cancels every outstanding bank ticket");
        check(Bitmap.writesAfterRecycle.get()==0,"no writes after image recycling");}
    static void run(int stage,int fault)throws Exception{
        FinishFailures1988Test.reset();FinishStageControl1988.reset(stage,fault);
        String key=FinishFailures1988Test.execute();Object a=FinishFailures1988Test.attempt();
        if(stage==0){check(GpuQualification1961.restore(key)!=null,"unmodified command graph can finish both exact and real-output trials");
            check(FinishStageControl1988.submitted==FinishStageControl1988.collected&&FinishStageControl1988.submitted>0,"successful real graph collects all bank results");
            for(int i=0;i<4;i++)check(((value("finishStages1988")>>>(i*5))&31)==24,"complete candidate ends at finite complete stage");}
        else {
            boolean onlyPipeline=stage==19||stage==20||stage==21||stage==25;
            boolean onlyLegacy=stage==27;
            for(int i=0;i<4;i++)if(!(onlyPipeline&&i==0)&&!(onlyLegacy&&i!=0)&&!(fault==4&&i>0)){
                check(((value("finishStages1988")>>>(i*5))&31)==stage,"actual failing API leaves precise last stage "+stage+" slot "+i);
                int expectedReason=fault==4?6:stage==8||stage==13||stage==17||fault==3?4:stage==22?2:3;
                check(FinishFailures1988Test.reason(i)==expectedReason,"operation failure keeps typed candidate reason at stage "+stage);
                int expectedFault=fault==4?4:stage==17?3:stage==23?1:fault;
                check(((value("finishFaults1988")>>>(i*3))&7)==expectedFault,"exception class reduced to finite scalar at stage "+stage);
            }
            if(!onlyLegacy&&!onlyPipeline&&fault!=4)check(GpuQualification1961.restore(key)==null,"failed graph cannot write ordinary certificate");
            if(fault==4){check(FinishFailures1988Test.failure(key)==null&&value("outcome")==4,"graph cancellation does not consume retry budget");
                for(int i=1;i<4;i++)check(((value("finishPhases1988")>>>(i*2))&3)==1&&FinishFailures1988Test.reason(i)==6,"cancel before a candidate begins retains CPU-only phase rather than inventing a GPU trial");}
        }
        clean();String text=GpuQualification1961.attemptSummary1986();check(!text.contains("private fixture"),"exception strings do not enter observations");
    }
    static void externalResultContract()throws Exception{
        FinishFailures1988Test.reset();FinishStageControl1988.reset(8,0);
        Bitmap source=FinishControl1986.image(29,19,false),expected=FinishControl1986.image(37,23,false);
        try {
            int old=GpuChain1961.compareFinish1978(source,0,37,23,FinishFailures1988Test.plan(),true,expected,32,true,null,false,()->false);
            int typed=GpuChain1961.compareFinish1988(source,0,37,23,FinishFailures1988Test.plan(),true,expected,32,true,null,false,()->false);
            check(old==ResidentProof1978.UNAVAILABLE&&typed==GpuChain1961.FINISH_MEMORY1988,"shared comparator keeps three-result contract while qualification sees explicit budget refusal");
        }finally{source.recycle();expected.recycle();Bitmap.ALL.clear();}clean();
    }
    public static void main(String[] args)throws Exception{
        run(0,0);
        for(int stage:new int[]{7,8,10,11,12,13,14,15,16,17,18,19,20,21,22,23,25,27})run(stage,stage==7?1:0);
        for(int fault=1;fault<=4;fault++)run(10,fault);
        externalResultContract();FinishFailures1988Test.reset();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"production_finish_stage_graph_faults1988_verified\":true,\"physical_android_tested\":false}");
    }
}
