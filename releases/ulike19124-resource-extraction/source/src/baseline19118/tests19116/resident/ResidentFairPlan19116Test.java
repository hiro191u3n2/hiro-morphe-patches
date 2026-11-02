package com.hiro.ulike;

/** Checks that the existing four complete speed measurements cannot certify
 * a photo when their Strong workloads used different worker plans. */
public final class ResidentFairPlan19116Test {
    private static int checks;
    private static void check(boolean condition,String reason) {
        checks++;if(!condition)throw new AssertionError(reason);
    }
    private static long[] complete() {
        long[] trials=new long[116];
        for(int i=0;i<4;i++) {
            int at=i*29;
            trials[at]=1;trials[at+1]=1;trials[at+2]=1000000000L;
            trials[at+21]=4;trials[at+22]=256;
            trials[at+23]=(180+i*20)*1024L*1024L;
            trials[at+24]=16;trials[at+25]=0;
        }
        return trials;
    }
    public static void main(String[] args) {
        long[] records=complete();
        check(GpuQualification1961.comparableResidentPlans19116(records),
            "memory budget changes alone do not change the executed worker plan");
        records[21]=3;
        check(!GpuQualification1961.comparableResidentPlans19116(records),
            "first existing route with three workers cannot compete with four");
        records=complete();records[29+22]=128;
        check(!GpuQualification1961.comparableResidentPlans19116(records),
            "first connected route with different strip rows is incomparable");
        records=complete();records[2*29+21]=2;
        check(!GpuQualification1961.comparableResidentPlans19116(records),
            "second existing route must match the first trial");
        records=complete();records[3*29+21]=3;
        check(!GpuQualification1961.comparableResidentPlans19116(records),
            "second connected route must match all preceding trials");
        records=complete();records[3*29+24]=15;records[3*29+25]=1;
        check(!GpuQualification1961.comparableResidentPlans19116(records),
            "a CPU fallback in one call changes the executed Strong route");
        records=complete();records[2*29+1]=3;
        check(!GpuQualification1961.comparableResidentPlans19116(records),
            "failed or cancelled measurements cannot support a speed proof");
        records=complete();records[3*29+21]=-1;
        check(!GpuQualification1961.comparableResidentPlans19116(records),
            "missing worker observations cannot support a speed proof");
        check(!GpuQualification1961.comparableResidentPlans19116(new long[29]),
            "a partial trial is not a complete comparison");
        records=complete();
        for(int i=0;i<4;i++){int at=i*29;records[at+4]=records[at+2];records[at+26]=-1;}
        StringBuilder text=new StringBuilder();
        VerificationJournal19101.appendResidentTiming19111(text,records);
        check(text.toString().contains("全体実経過は速度判定・Strong計画は比較条件・工程内訳は診断値"),
            "display identifies the measurements and Strong plan used by qualification");
        check(!text.toString().contains("診断値・認定には不使用"),
            "display does not deny the admitted role of whole-call timing");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+
            ",\"incomparable_plan_speed_rejection_blocked\":true,\"physical_android_tested\":false}");
    }
}
