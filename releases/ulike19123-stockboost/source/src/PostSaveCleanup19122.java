package com.hiro.ulike;

import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** One bounded idle cleanup after the terminal receipt and queue release.
 * Stores scalar save/trace identities only, never an image, activity or SDK
 * object. This is capacity relief, not a measured cause or frame-rate fix. */
public final class PostSaveCleanup19122 {
    private PostSaveCleanup19122() {}
    private static final Object LOCK=new Object();
    private static final HashMap<Long,Long> ACTIVE=new HashMap<Long,Long>();
    private static final ScheduledThreadPoolExecutor EXECUTOR=executor();
    private static long generation,lastClosedSave,lastClosedTrace=-1,lastAttemptGeneration=-1;
    private static long attempts,completed,skipped;
    private static ScheduledFuture<?> pending;
    private static long pendingGeneration=-1;
    private static boolean generationCleanupEnabled=true;
    // status, generation, trace, elapsed, finishNanos, javaBefore, javaAfter,
    // nativeBefore, nativeAfter, gpuBefore, gpuAfter, cpuStatus, gpuStatus.
    private static long[] latest={0,0,-1,0,0,-1,-1,-1,-1,-1,-1,0,0};
    private static ScheduledThreadPoolExecutor executor() {
        ScheduledThreadPoolExecutor made=new ScheduledThreadPoolExecutor(1,new CleanupFactory());
        made.setRemoveOnCancelPolicy(true);
        made.setKeepAliveTime(15,TimeUnit.SECONDS);
        made.allowCoreThreadTimeOut(true);
        return made;
    }
    private static final class CleanupFactory implements ThreadFactory {
        public Thread newThread(Runnable work) {
            Thread thread=new Thread(SpeedWorkers1935.backgroundWorker19121(work),"ULike-idle19122");
            thread.setDaemon(true);return thread;
        }
    }
    /** Register before a newly reserved save can execute. Duplicate calls do
     * not consume a second terminal notification or allocate a second record. */
    public static void captureStarted19122(long saveId) {
        if(saveId<=0)return;
        boolean enabled=enabled19122();
        synchronized(LOCK) {
            Long key=Long.valueOf(saveId);
            if(ACTIVE.containsKey(key))return;
            ACTIVE.put(key,Long.valueOf(-1));generation++;generationCleanupEnabled=enabled;
            if(pending!=null){pending.cancel(false);pending=null;pendingGeneration=-1;}
        }
    }
    public static void attachTrace19122(long saveId,long traceId) {
        if(saveId<=0||traceId<=0)return;
        synchronized(LOCK) {
            Long key=Long.valueOf(saveId);
            if(ACTIVE.containsKey(key))ACTIVE.put(key,Long.valueOf(traceId));
        }
    }
    /** Invoke after the actual terminal UI callback, or after a failed post
     * has surrendered its terminal guard. SaveQueue has a separate final gate. */
    public static void terminal19122(long saveId) {
        synchronized(LOCK) {
            Long trace=ACTIVE.remove(Long.valueOf(saveId));
            if(trace==null)return;
            if(saveId>=lastClosedSave){lastClosedSave=saveId;lastClosedTrace=trace.longValue();}
        }
        request19122();
    }
    /** SaveQueue invokes outside its own monitor after every owner is cleared. */
    public static void queueIdle19122(){request19122();}
    private static boolean enabled19122() {
        try{return CapturePolicy19122.cleanupEnabled();}catch(Throwable optional){return true;}
    }
    private static void request19122() {
        long current;
        synchronized(LOCK) {
            if(generation==0||!ACTIVE.isEmpty()||generation==lastAttemptGeneration)return;
            current=generation;
            if(pending!=null&&pendingGeneration==current)return;
        }
        // Never acquire queue/shot/CPU locks while holding the coordinator.
        if(!isCurrentIdle19122(current))return;
        synchronized(LOCK) {
            if(current!=generation||!ACTIVE.isEmpty()||current==lastAttemptGeneration)return;
            if(pending!=null&&pendingGeneration==current)return;
            pendingGeneration=current;
            try{pending=EXECUTOR.schedule(new Cleanup(current,lastClosedTrace,generationCleanupEnabled),250,TimeUnit.MILLISECONDS);}
            catch(Throwable unavailable){pending=null;pendingGeneration=-1;}
        }
    }
    /** Also used immediately on the GPU owner. Native owners have their own
     * atomic lease checks; this scalar generation check never frees a resource. */
    public static boolean isCurrentIdle19122(long expectedGeneration) {
        synchronized(LOCK){if(expectedGeneration!=generation||!ACTIVE.isEmpty())return false;}
        try {
            if(!SaveQueue1935.idle1953()||!ShotContext1932.idle1953()
                    ||!SpeedWorkers1935.cpuIdle1944())return false;
        }catch(Throwable unavailable){return false;}
        synchronized(LOCK){return expectedGeneration==generation&&ACTIVE.isEmpty();}
    }
    private static final class Cleanup implements Runnable {
        private final long expected,trace;
        private final boolean enabled;
        Cleanup(long expected,long trace,boolean enabled){this.expected=expected;this.trace=trace;this.enabled=enabled;}
        public void run() {
            long start=System.nanoTime();
            long[] result={0,expected,trace,0,0,-1,-1,-1,-1,-1,-1,0,0};
            synchronized(LOCK) {
                if(pendingGeneration==expected){pending=null;pendingGeneration=-1;}
                if(expected!=generation||!ACTIVE.isEmpty()||expected==lastAttemptGeneration)return;
                lastAttemptGeneration=expected;attempts++;
            }
            try {
                if(!enabled){result[0]=4;return;}
                if(Thread.currentThread().isInterrupted()||!isCurrentIdle19122(expected)){result[0]=2;return;}
                long[] cpu=SpeedWorkers1935.trimLargeIdle19122();
                result[11]=cpu[0];result[5]=cpu[1];result[6]=cpu[2];result[7]=cpu[3];result[8]=cpu[4];
                if(cpu[0]==0||Thread.currentThread().isInterrupted()||!isCurrentIdle19122(expected)){result[0]=2;return;}
                long[] gpu=GpuNoise1960.trimIdle19122(8L*1024*1024,expected);
                result[12]=gpu[0];result[9]=gpu[1];result[10]=gpu[2];
                result[0]=!isCurrentIdle19122(expected)||(gpu[0]==0&&gpu[1]!=0)?2:((cpu[0]==2||gpu[0]==3)?3:1);
            }catch(Throwable optional){result[0]=3;}
            finally {
                result[3]=Math.max(0,System.nanoTime()-start);result[4]=System.nanoTime();
                synchronized(LOCK) {
                    if(result[0]==1)completed++;else skipped++;
                    if(expected>=latest[1])latest=result;
                }
                // Observation must never extend ownership or fail a saved image.
                if(trace>0)try{Responsiveness19122.cleanup19122(trace,status19122(result[0]),result[3],
                    result[5]>=0&&result[6]>=0?Math.max(0,result[5]-result[6]):-1);}catch(Throwable optional){}
            }
        }
    }
    private static String status19122(long status) {
        if(status==1)return "未使用領域の整理完了";
        if(status==2)return "撮影・処理再開のため整理見送り";
        if(status==3)return "任意の整理で未確認・失敗あり";
        if(status==4)return "比較設定により整理なし";
        return "未実行";
    }
    /** Scalar process observation; no promise that dropped references were
     * collected, native capacity equals RSS, or UI response has recovered. */
    public static long[] snapshot19122() {
        synchronized(LOCK) {
            long[] result=new long[latest.length+4];
            result[0]=attempts;result[1]=completed;result[2]=skipped;result[3]=ACTIVE.size();
            System.arraycopy(latest,0,result,4,latest.length);return result;
        }
    }
    public static String summary19122() {
        long[] value=snapshot19122();
        return "保存後の未使用領域整理: "+status19122(value[4])+" / 試行 "+value[0]+"回 / 実行中の保存 "+value[3]+"件"
            +" / Java返却済み領域 "+bytes19122(value[9])+" → "+bytes19122(value[10])
            +" / native CPU容量 "+bytes19122(value[11])+" → "+bytes19122(value[12])
            +" / native GPU容量 "+bytes19122(value[13])+" → "+bytes19122(value[14])
            +"（保持容量の観測。実メモリ回収・操作復帰は別計測）";
    }
    private static String bytes19122(long bytes) {
        return bytes<0?"未観測":String.valueOf(bytes/1024)+" KiB";
    }
}
