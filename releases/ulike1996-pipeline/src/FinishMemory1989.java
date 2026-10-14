package com.hiro.ulike;

import java.util.Arrays;

/** One optional idle-native relief after the unchanged Finish budget refuses.
 * There are no images, context references, global observers or saved reports.
 * The caller's real output timer includes this complete operation. */
final class FinishMemory1989 {
    private FinishMemory1989() {}
    static final int COUNT=33,SCHEMA=0,REQUESTED=1,INITIAL_FIT=2,TRIM_STATUS=3,
        FINAL_FIT=4,CHECKS=5,BACKGROUND=6,TRIM_NANOS=7,SNAPSHOT_MASK=8,BEFORE=9,AFTER=21;
    static final int MAX_HEAP=0,USED_HEAP=1,HEADROOM=2,GPU=3,WHOLE=4,OLD_FINISH=5,
        NATIVE_TOTAL=6,NATIVE_H8=7,NATIVE_STRONG=8,NATIVE_SINGLE=9,RESERVED=10,AVAILABLE=11;
    static final int NONE=0,REQUESTED_TRIM=1,BUSY_CPU=2,BUSY_GPU=3,CANCELLED=4,
        FOREGROUND=5,LIMIT=6,PARTIAL_FAILURE=7;

    static boolean fits(long extra) {
        // In particular, an admitted call cannot incur a native trim or a new
        // report allocation. Foreground callers retain this exact old result.
        if(GpuNoise1960.workspaceFits(extra))return true;
        if(!GpuQualification1961.background())return false;
        long[] values=null;
        try {
            values=new long[COUNT];Arrays.fill(values,-1);
            values[SCHEMA]=1;values[REQUESTED]=extra;values[INITIAL_FIT]=0;
            values[BACKGROUND]=1;values[SNAPSHOT_MASK]=0;
            if(GpuNoise1960.snapshotMemory1989(values,BEFORE))values[SNAPSHOT_MASK]=1;
        }catch(Throwable optional){values=null;}
        boolean accepted=false;int status=NONE,checks=1;long nanos=0;
        if(extra<0||extra>GpuNoise1960.MAX_BYTES)status=LIMIT;
        else if(Thread.currentThread().isInterrupted()||GpuQualification1961.cancelled())status=CANCELLED;
        else if(GpuNoise1960.sessionBusy())status=BUSY_GPU;
        else {
            long started=System.nanoTime();int trim;
            try{trim=SpeedWorkers1935.trimNativeIdle1989();}catch(Throwable optional){trim=2;}
            nanos=Math.max(0,System.nanoTime()-started);
            if(trim==0)status=BUSY_CPU;
            else {
                status=trim==1?REQUESTED_TRIM:PARTIAL_FAILURE;
                // Same 512 MiB ceiling, 64 MiB reserve and all live owners.
                // No retry loop and no scalar observation influences this.
                checks=2;accepted=GpuNoise1960.workspaceFits(extra);
                try{if(values!=null&&GpuNoise1960.snapshotMemory1989(values,AFTER))values[SNAPSHOT_MASK]|=2;}
                catch(Throwable optional){}
            }
        }
        try {
            if(values!=null){
                values[TRIM_STATUS]=status;values[FINAL_FIT]=accepted?1:0;
                values[CHECKS]=checks;values[TRIM_NANOS]=nanos;
                GpuQualification1961.finishMemory1989(values);
            }
        }catch(Throwable optional){}
        return accepted;
    }
}
