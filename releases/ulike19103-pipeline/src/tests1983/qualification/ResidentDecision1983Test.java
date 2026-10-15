package com.hiro.ulike;

import android.graphics.Bitmap;

/** Actual Resident producer with controlled queue decisions, including races. */
public final class ResidentDecision1983Test {
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static void reset()throws Exception {
        ResidentDiagnostics1982Test.reset();GpuQualification1961.declineReason1983=13;
        GpuQualification1961.scheduleReason1983=12;GpuQualification1961.scheduleCalls1983=0;
        GpuQualification1961.afterDecision1983=GpuQualification1961.afterSchedule1983=null;
        ProcessingTiming1947.queueReason1983= -1;
    }
    static void refusals()throws Exception {
        reset();Bitmap source=ResidentDiagnostics1982Test.image();int before=Bitmap.copies;
        GpuQualification1961.declineAt=1;GpuQualification1961.declineReason1983=9;
        GpuQualification1961.afterDecision1983=new Runnable(){public void run(){GpuQualification1961.declineReason1983=8;}};
        check(ResidentDiagnostics1982Test.invoke(source)==null,"cooldown keeps foreground CPU route");
        check(ProcessingTiming1947.reason==6&&ProcessingTiming1947.queueReason1983==9,"producer records returned decision, not a later changed cause");
        check(ProcessingTiming1947.retryRemaining1983==1234000000L&&ProcessingTiming1947.queueRetries1983==2,"cooldown metadata from same result retained");
        check(GpuQualification1961.queueCalls==1&&GpuQualification1961.scheduleCalls1983==0&&Bitmap.copies==before,"refusal never asks scheduler again for diagnostic text or clones image");
        check(ProcessingTiming1947.terminalCalls==1,"detailed refusal is one terminal event");
        reset();source=ResidentDiagnostics1982Test.image();before=Bitmap.recycles;
        GpuQualification1961.declineAt=2;GpuQualification1961.declineReason1983=16;
        ResidentDiagnostics1982Test.invoke(source);
        check(ProcessingTiming1947.reason==6&&ProcessingTiming1947.queueReason1983==16,"post-copy retention race retains exact second decision");
        check(GpuQualification1961.queueCalls==2&&GpuQualification1961.scheduleCalls1983==0&&Bitmap.recycles==before+1,"post-copy rejection closes sole snapshot without re-admission");
        check(!source.isRecycled()&&source.pixels[0]==0xff123456,"caller bitmap remains pristine after rejected clone");
        reset();source=ResidentDiagnostics1982Test.image();before=Bitmap.recycles;
        GpuQualification1961.decline=true;GpuQualification1961.scheduleReason1983=11;
        GpuQualification1961.afterSchedule1983=new Runnable(){public void run(){GpuQualification1961.scheduleReason1983=13;}};
        ResidentDiagnostics1982Test.invoke(source);
        check(ProcessingTiming1947.queueReason1983==11&&ProcessingTiming1947.reason==6,"final certification race is neither stale preflight nor later live queue");
        check(GpuQualification1961.queueCalls==2&&GpuQualification1961.scheduleCalls1983==1,"original two preflights and one schedule preserved");
        check(Bitmap.recycles==before+1&&GpuQualification1961.queued==null,"schedule refusal owns and releases clone exactly once");
    }
    static void acceptedAndEarlyExit()throws Exception {
        reset();Bitmap source=ResidentDiagnostics1982Test.image();ResidentDiagnostics1982Test.invoke(source);
        check(ProcessingTiming1947.reason==2&&ProcessingTiming1947.queueReason1983==1,"accepted schedule differs from ready preflight");
        check(GpuQualification1961.queued!=null&&GpuQualification1961.queueCalls==2&&GpuQualification1961.scheduleCalls1983==1,"diagnostics add no queue side effects");
        check(ProcessingTiming1947.queueRequested1983==ProcessingTiming1947.retained&&ProcessingTiming1947.queueHeld1983==GpuQualification1961.held,"scheduled snapshot records actual owned bytes");
        check(ProcessingTiming1947.queueCount1983==1&&ProcessingTiming1947.runningCount1983==0,"post-admission queue count includes this job");
        GpuQualification1961.drop();check(!source.isRecycled(),"queue owns clone only");
        reset();source=ResidentDiagnostics1982Test.image();GpuSnapshotBudget1981.Copy held=GpuSnapshotBudget1981.tryCopy(1,GpuSnapshotBudget1981.FINISH);
        try{ResidentDiagnostics1982Test.invoke(source);check(ProcessingTiming1947.reason==10&&ProcessingTiming1947.queueReason1983==-1,"snapshot cap does not invent a queue rejection");
            check(GpuQualification1961.queueCalls==0,"snapshot refusal keeps original short circuit");}finally{held.close();}
        reset();source=ResidentDiagnostics1982Test.image();GpuQualification1961.negatives.add(ResidentDiagnostics1982Test.key(source));ResidentDiagnostics1982Test.invoke(source);
        check(ProcessingTiming1947.reason==4&&ProcessingTiming1947.queueReason1983==-1&&GpuQualification1961.queueCalls==0,"known exact rejection is not overwritten by guessed queue state");
        reset();source=ResidentDiagnostics1982Test.image();GpuQualification1961.declineAt=1;ProcessingTiming1947.sinkFailure=true;
        check(ResidentDiagnostics1982Test.invoke(source)==null&&!source.isRecycled(),"optional detailed recording failure cannot change route or source lifetime");
    }
    public static void main(String[] args)throws Exception {
        refusals();acceptedAndEarlyExit();GpuQualification1961.drop();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"resident_terminal_decisions1983\":true,\"resident_no_diagnostic_readmission1983\":true,\"resident_snapshot_ownership1983\":true}");
    }
}
