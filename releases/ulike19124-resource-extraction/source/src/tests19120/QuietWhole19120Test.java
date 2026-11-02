package com.hiro.ulike;

/** Actual WholeRoute engine with observable mock work; no device performance claim. */
public final class QuietWhole19120Test {
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static final class Clock extends WholeRoute1953.Clock {long value;long now(){return ++value;}}
    static final class Work implements WholeRoute1953.Work {
        int cpu,gpu,snapshots,budgetReads,published,discarded;boolean throwCpu,selection;
        public long probeBytes(){budgetReads++;return 4096;}
        public WholeRoute1953.Probe snapshotProbe(){snapshots++;throw new AssertionError("unqualified photo must not be copied for background verification");}
        public boolean cpu(){cpu++;if(throwCpu)throw new IllegalStateException("original failure");return true;}
        public boolean gpu(){gpu++;return true;}
        public boolean equal(){throw new AssertionError("foreground must not replay pixel comparisons");}
        public void publishGpu(){published++;}public void discardGpu(){discarded++;}
        public boolean dispatchSelection(){return selection;}
    }
    static final class History implements WholeRoute1953.History {
        final int dispatch;History(int dispatch){this.dispatch=dispatch;}
        public String environment(){return "verified-driver";}
        public long restore(int[] key,String environment){return 10000;}
        public WholeRoute1953.Qualification restoreQualified(int[] key,String environment){return new WholeRoute1953.Qualification(10000,dispatch);}
        public void qualified(int[] key,String environment,long cpu){throw new AssertionError("no new proof may be saved");}
        public void rejected(int[] key,String environment){}
    }
    public static void main(String[] args)throws Exception{
        WholeRoute1953.Engine cold=new WholeRoute1953.Engine(new Clock());int[] key={1953,100,100,7};
        for(int i=0;i<5;i++){
            Work w=new Work();Object trace=new Object();check(cold.run(key,w,trace),"uncertified processing retains normal CPU output");
            check(w.cpu==1&&w.gpu==0&&w.snapshots==0&&w.budgetReads==0,"no probe planning, copy, duplicate CPU or GPU on cold path");
            cold.saved(trace,true);cold.wake();check(cold.retainedBytes==0,"saving never retains a verification image");
        }
        Work failed=new Work();failed.throwCpu=true;boolean threw=false;
        try{cold.run(key,failed,new Object());}catch(IllegalStateException expected){threw=expected.getMessage().equals("original failure");}
        check(threw&&failed.cpu==1,"original processing failure propagates");
        Work after=new Work();check(cold.run(key,after,new Object())&&after.cpu==1&&after.budgetReads==0,"route ownership is released after CPU failure");
        for(int dispatch:new int[]{-1,32,64,0}){
            WholeRoute1953.Engine certified=new WholeRoute1953.Engine(new Clock(),new History(dispatch));
            Work w=new Work();w.selection=dispatch!=-1;check(certified.run(key,w,new Object()),"previously certified route remains usable");
            check(w.gpu==1&&w.cpu==0&&w.published==1&&w.discarded==1&&w.snapshots==0,"certified GPU executes exactly once without CPU replay or snapshots");
        }
        WholeRoute1953.Engine insufficient=new WholeRoute1953.Engine(new Clock(),new History(-1));Work needsDispatch=new Work();needsDispatch.selection=true;
        check(insufficient.run(key,needsDispatch,new Object()),"missing dispatch proof still produces CPU output");
        check(needsDispatch.cpu==1&&needsDispatch.gpu==0&&needsDispatch.snapshots==0,"scalar evidence cannot authorize an unproved dispatch choice");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"suite\":\"quiet_whole_route\"}");
    }
}
