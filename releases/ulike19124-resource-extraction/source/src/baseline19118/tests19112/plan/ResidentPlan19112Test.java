package com.hiro.ulike;

import java.util.concurrent.CancellationException;

/** Executed current whole-production planner with controlled resource observations.
 * Byte budgets model an already charged resident image/leases, never physical RAM. */
public final class ResidentPlan19112Test {
    static int assertions,plans;
    static final int WIDTH=3060,HEIGHT=4080,HALO=18;
    static long mib(double value){return (long)(value*1024*1024);}
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static final class Memory implements QualityPipeline1932.StrongMemory19112 {
        long before,after,nativeBytes;int available=4,afterAvailable=4,trimCalls,budgetCalls,rowLimit=256,trimCode=1,checks;
        boolean cancelBefore,cancelAfter,realTrim;long gpuLive=168100000,reserved=47000000;
        Memory(long before,long after,long retained){this.before=before;this.after=after;nativeBytes=retained;}
        public long budget(){budgetCalls++;return trimCalls==0?before:after;}
        public int workers(){return trimCalls==0?available:afterAvailable;}
        public int rows(long budget){return Math.min(rowLimit,Math.min(256,Scheduling1944.coreRows(WIDTH,HEIGHT,HALO,4,budget)));}
        public long retained(){return nativeBytes;}
        public int trim(){trimCalls++;if(realTrim)return SpeedWorkers1935.trimNativeIdle1989();return trimCode;}
        public void check(){checks++;if(cancelBefore||cancelAfter&&trimCalls!=0)throw new CancellationException("fixture capture");}
        QualityPipeline1932.StrongPlan19112 plan(boolean resident){return QualityPipeline1932.strongPlan19112(WIDTH,HEIGHT,HALO,4,resident,this);}
    }
    static void plans(){
        for(int width:new int[]{1,17,1024,3060,8192})for(int height:new int[]{1,17,128,513,4080})
        for(int available=1;available<=4;available++)for(int initial:new int[]{16,32,64,128,256})
        for(long budget:new long[]{0,1,1048576,16777216,38692454,96783564,143235481,536870912}){
            long[] previous=PublishedStrongPlan111.choose(width,height,HALO,initial,4,available,budget);
            QualityPipeline1932.StrongPlan19112 current=QualityPipeline1932.chooseStrong19112(width,height,HALO,initial,4,available,budget);
            check(current.rows==previous[0]&&current.workers==previous[1]&&current.workerBytes==previous[2],"exact published .111 plan retained");
            check(current.budget==budget&&current.workers<=available&&current.workers<=4,"no worker/budget inflation");
            long required=current.workerBytes*current.workers+(long)width*HALO*4;
            if(required>budget)check(current.rows==16&&current.workers==1,"only original mandatory minimum may need requireMemory refusal");
            plans++;
        }
        long bytes=QualityPipeline1932.chooseStrong19112(WIDTH,HEIGHT,HALO,256,4,4,Long.MAX_VALUE).workerBytes;
        long shared=(long)WIDTH*HALO*4;
        QualityPipeline1932.StrongPlan19112 below=QualityPipeline1932.chooseStrong19112(WIDTH,HEIGHT,HALO,256,4,4,bytes*2+shared-1);
        QualityPipeline1932.StrongPlan19112 exact=QualityPipeline1932.chooseStrong19112(WIDTH,HEIGHT,HALO,256,4,4,bytes*2+shared);
        check(below.workers==1&&exact.workers==2,"two complete CPU fallback workspaces required at exact boundary");
        int invalid=0;
        for(int rows:new int[]{0,15,17,240,512})try{QualityPipeline1932.chooseStrong19112(WIDTH,HEIGHT,HALO,rows,4,4,1);}catch(IllegalArgumentException expected){invalid++;}
        check(invalid==5,"unbounded or non-power-of-two plan rejected");
    }
    static void resources(){
        Memory old=new Memory(mib(36.9),mib(100.9),mib(64));
        QualityPipeline1932.StrongPlan19112 oldPlan=old.plan(false);
        check(oldPlan.rows==128&&oldPlan.workers==1&&old.trimCalls==0,"published nonresident reproducer unchanged");
        Memory relieved=new Memory(mib(36.9),mib(100.9),mib(64));
        QualityPipeline1932.StrongPlan19112 p=relieved.plan(true);
        check(p.rows==256&&p.workers==2&&relieved.trimCalls==1&&relieved.budgetCalls==2,"accepted resident now reclaims before committing degraded plan");
        check(p.budget==mib(100.9)&&relieved.gpuLive==168100000&&relieved.reserved==47000000,"observations never release GPU owner or outstanding debt");
        Memory insufficient=new Memory(mib(36.9),mib(92.3),mib(55.4));p=insufficient.plan(true);
        check(p.rows==256&&p.workers==1&&insufficient.trimCalls==1,"reclaimed bytes do not fabricate capacity for second worker");
        Memory race=new Memory(mib(36.9),mib(20),mib(64));race.afterAvailable=1;p=race.plan(true);
        check(p.budget==mib(20)&&p.rows==64&&p.workers==1,"budget drop and CPU competition re-read after trim");
        Memory refused=new Memory(mib(36.9),mib(36.9),mib(64));refused.trimCode=0;p=refused.plan(true);
        check(p.rows==128&&p.workers==1&&refused.trimCalls==1,"busy trim return grants no reclaimed bytes and no retry loop");
        Memory partial=new Memory(mib(36.9),mib(50),mib(64));partial.trimCode=2;p=partial.plan(true);
        check(p.rows==256&&p.workers==1&&p.budget==mib(50),"partial failure uses live measured remainder only");
        for(long retained:new long[]{0,-1,Long.MAX_VALUE}){Memory missing=new Memory(mib(36.9),mib(100.9),retained);p=missing.plan(true);check(missing.trimCalls==0&&p.rows==128,"unknown or absent retained capacity cannot authorize trim");}
        Memory enough=new Memory(mib(300),mib(300),mib(64));p=enough.plan(true);check(p.workers==4&&p.rows==256&&enough.trimCalls==0,"unconstrained plan preserves cache");
        Memory cpuBusy=new Memory(mib(117.9),mib(181.9),mib(64));cpuBusy.available=1;p=cpuBusy.plan(true);check(p.workers==1&&cpuBusy.trimCalls==0,"CPU permits alone do not justify reclaim");
        Memory layout=new Memory(mib(117.9),mib(181.9),mib(64));layout.rowLimit=128;p=layout.plan(true);check(p.rows==128&&p.workers==4&&layout.trimCalls==0,"certified narrow-row hint preserved");
        Memory cancel=new Memory(mib(36.9),mib(100.9),mib(64));cancel.cancelBefore=true;
        boolean threw=false;try{cancel.plan(true);}catch(CancellationException expected){threw=true;}check(threw&&cancel.trimCalls==0,"cancel before reclaim prevents trim");
        cancel=new Memory(mib(36.9),mib(100.9),mib(64));cancel.cancelAfter=true;threw=false;
        try{cancel.plan(true);}catch(CancellationException expected){threw=true;}check(threw&&cancel.trimCalls==1&&cancel.budgetCalls==1,"capture during trim aborts before planning/allocation");
    }
    static void actualWorkerLease(){
        final int[] trims={0};
        SpeedWorkers1935.installScratchMemory1959(new SpeedWorkers1935.ScratchMemory(){public long retainedBytes(){return mib(64);}public void trim(){trims[0]++;}});
        boolean lease=SpeedWorkers1935.enterLegacy();
        try{
            Memory busy=new Memory(mib(36.9),mib(36.9),mib(64));busy.realTrim=true;
            QualityPipeline1932.StrongPlan19112 p=busy.plan(true);
            check(trims[0]==0&&p.rows==128&&p.workers==1,"real SpeedWorkers active CPU lease blocks idle native trim");
        }finally{SpeedWorkers1935.leaveLegacy(lease);}
        Memory idle=new Memory(mib(36.9),mib(100.9),mib(64));idle.realTrim=true;idle.plan(true);
        check(trims[0]==1,"same production native owner trims once after lease release");
        SpeedWorkers1935.installScratchMemory1959(null);
    }
    static void publishedAnonymousAbi()throws Exception{
        Class<?> owner=Class.forName("com.hiro.ulike.QualityPipeline1932$6");
        check(SpatialNoise1934.Patches.class.isAssignableFrom(owner),"published anonymous spatial-noise owner keeps its interface");
        check(owner.getDeclaredConstructor(android.graphics.Bitmap.class).getParameterTypes().length==1,"published anonymous Bitmap constructor retained");
        check(owner.getDeclaredMethod("read",int[].class,int.class,int.class,int.class,int.class).getReturnType()==void.class,"published anonymous read ABI retained");
    }
    public static void main(String[] args)throws Exception{
        plans();resources();actualWorkerLease();publishedAnonymousAbi();
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"planner_cases\":"+plans+",\"current_resident_planner_executed19112\":true,\"accepted_degraded_resident_reclaimed19112\":true,\"same_budget_replan19112\":true,\"cpu_lease_cancel_preserved19112\":true,\"published111_plan_equivalence19112\":true,\"published111_anonymous_abi_preserved19112\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
    }
}
