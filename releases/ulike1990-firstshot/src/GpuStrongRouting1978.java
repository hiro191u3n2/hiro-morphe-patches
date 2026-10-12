package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** .78 measured Strong cohorts. A hint compares real bounded worker execution,
 * never a serial CPU time divided by a worker count. It does not claim to time
 * the complete photograph or the separately qualified resident finish route.
 * Only scalar certificates survive the existing cancellable idle job. */
final class GpuStrongRouting1978 {
    private GpuStrongRouting1978() {}
    private static final int[][] BANKS={{0,1,3,7,8,9,10,11,12,13},{14,15,16,17,18,19,20,21,22,23}};
    private static final long WAIT_NS=15000000000L;
    static String key(int[] u,int profile,int variant,int workers,GpuQualification1961.Record child){
        if(u==null||child==null||workers<3||workers>4||child.variant!=variant)return null;
        String exact=GpuStrong1960.profileKey1973(u,profile);
        return "strong-gx1978-cohort-v1:"+exact.substring(exact.indexOf(':')+1)+":"+profile+":"+variant+":"+workers+":"+child.cpuNanos+":"+child.gpuNanos;
    }
    static boolean accepted(int[] u,int profile,int variant,int workers){
        if(workers<3||workers>4)return false;
        String exact=GpuStrong1960.profileKey1973(u,profile);
        if(GpuQualification1961.exactRejected(exact))return false;
        GpuQualification1961.Record child=GpuQualification1961.restore(exact);
        String name=key(u,profile,variant,workers,child);
        if(name==null||GpuQualification1961.exactRejected(name))return false;
        GpuQualification1961.Record route=GpuQualification1961.restore(name);
        return route!=null&&route.variant==1;
    }
    private static void check(GpuQualification1961.Cancellation cancellation){
        if(Thread.currentThread().isInterrupted()||cancellation.cancelled())throw new CancellationException("Strong cohort cancelled");
    }
    private static final class Measured {
        final int[][] pixels,confidence;final long nanos;final String failure;
        Measured(int[][] p,int[][] c,long n,String f){pixels=p;confidence=c;nanos=n;failure=f;}
    }
    private static final class Banks {
        final boolean[] busy=new boolean[2];
        synchronized int take(GpuQualification1961.Cancellation cancellation){
            long started=System.nanoTime();
            for(;;){
                check(cancellation);
                for(int i=0;i<2;i++)if(!busy[i]){busy[i]=true;return i;}
                if(System.nanoTime()-started>=WAIT_NS)return -1;
                try{wait(25);}catch(InterruptedException cancelled){Thread.currentThread().interrupt();throw new CancellationException("Strong cohort bank cancelled");}
            }
        }
        synchronized void release(int bank){busy[bank]=false;notifyAll();}
    }
    /** mode0 is a complete parallel CPU reference, mode1 queues every task to
     * the same two GPU banks, mode2 keeps two GPU tasks and runs the remaining
     * tasks on CPU concurrently. All modes perform exactly workers strips. */
    private static Measured measure(final int mode,final int workers,final int[] source,final int[] policy,
            final int[] u,final StrongNoise1958.Model model,final GpuPolicy1960.PolicyData descriptor,
            final int profile,final int variant,final GpuQualification1961.Cancellation cancellation){
        final int count=u[0]*(u[3]-u[2]),offset=u[0]*u[2],cf=u[12]==0?0:((u[0]+3)/4)*((u[3]-u[2]+3)/4);
        final int[][] pixels=new int[workers][],confidence=new int[workers][];
        // The foreground also acquires its worker outputs before run(). Private
        // GPU readbacks are reused per bank just as in the production stage.
        for(int i=0;i<workers;i++){pixels[i]=new int[source.length];confidence[i]=cf==0?null:new int[cf];}
        final int[][][] targets=mode==0?null:new int[2][][];
        if(targets!=null)for(int b=0;b<2;b++)targets[b]=cf==0?new int[][]{new int[count],new int[1]}:new int[][]{new int[count],new int[cf],new int[1]};
        final boolean[] complete=new boolean[workers];final String[] failure=new String[workers];
        GpuNoise1960.Session opened=null;long started=System.nanoTime();
        try{
            if(mode!=0){
                opened=GpuNoise1960.open();if(opened==null)return new Measured(null,null,0,"session_unavailable");
                float[] evidence=StrongNoise1958.gpuEvidence1960(model);int[][] maps=StrongNoise1958.gpuMaps1960(model);
                GpuNoise1960.Lease1971 lease=opened.reserveCapacity1971(new int[]{2,4,5,6},new long[]{4L*evidence.length,4L*maps[0].length,4L*maps[1].length,4L*maps[2].length},0);
                if(lease==null)return new Measured(null,null,0,"memory_budget");
                try{if(!lease.revalidate1971()||!opened.upload(2,evidence)||!opened.upload(4,maps[0])||!opened.upload(5,maps[1])||!opened.upload(6,maps[2]))return new Measured(null,null,0,"upload_failed");}
                finally{lease.close();}
            }
            final GpuNoise1960.Session session=opened;final Banks banks=new Banks();
            final GpuPolicy1960.PolicyData activeDescriptor=GpuStrong1960.directPolicy1978(profile)?null:descriptor;
            Runnable[] tasks=new Runnable[workers];
            for(int i=0;i<workers;i++){
                final int index=i;
                tasks[i]=new Runnable(){public void run(){
                    check(cancellation);
                    if(mode==0||mode==2&&index>=2){
                        complete[index]=StrongNoise1958.gpuOracleSnapshot1961(source,pixels[index],u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[8],u[9]!=0,model,policy,confidence[index]);
                        check(cancellation);return;
                    }
                    int bank=banks.take(cancellation);if(bank<0){failure[index]="bank_busy";return;}
                    GpuNoise1960.Lease1971 lease=null;
                    try{
                        lease=GpuStrong1960.reserveRange1971(session,source,policy,u,activeDescriptor,BANKS[bank]);
                        if(lease==null||!lease.revalidate1971()){failure[index]="memory_budget";return;}
                        check(cancellation);
                        GpuNoise1960.Ticket ticket=session.submit(GpuStrong1960.commands1973(source,policy,u,activeDescriptor,BANKS[bank],variant,profile),bank);
                        if(ticket==null){failure[index]="submit_failed";return;}
                        GpuStrong1960.Read1971 read=GpuStrong1960.readCohort1978(session,ticket,u,BANKS[bank],targets[bank]);
                        check(cancellation);
                        if(read.failure!=null){failure[index]=read.failure;return;}
                        System.arraycopy(read.result.pixels,0,pixels[index],offset,count);
                        if(cf!=0)System.arraycopy(read.result.confidence,0,confidence[index],0,cf);
                        complete[index]=true;
                    }finally{if(lease!=null)lease.close();banks.release(bank);}
                }};
            }
            SpeedWorkers1935.run(tasks);check(cancellation);
        }finally{if(opened!=null)opened.close();}
        long elapsed=Math.max(1L,System.nanoTime()-started);
        for(int i=0;i<workers;i++)if(!complete[i])return new Measured(null,null,elapsed,failure[i]==null?"cpu_unavailable":failure[i]);
        return new Measured(pixels,confidence,elapsed,null);
    }
    private static String difference(Measured actual,Measured expected,int[] u){
        if(actual==null||actual.failure!=null||actual.pixels==null)return actual==null?"output_shape_failed":actual.failure;
        int offset=u[0]*u[2],count=u[0]*(u[3]-u[2]);
        for(int worker=0;worker<actual.pixels.length;worker++){
            int[] p=actual.pixels[worker],q=expected.pixels[0];
            if(p==null||p.length!=q.length)return "output_shape_failed";
            for(int i=0;i<count;i++)if(p[offset+i]!=q[offset+i])return "argb_mismatch";
            if(!Arrays.equals(actual.confidence[worker],expected.confidence[0]))return "confidence_mismatch";
        }
        return null;
    }
    /** With no working legacy GPU, compare a corrected new GPU against actual
     * parallel CPU execution. This is a separate worker-bound admission, not a
     * fabricated old-GPU reference and not a serial/worker-count estimate. */
    static long[] compareCpu1978(int[] source,int[] policy,int[] u,StrongNoise1958.Model model,GpuPolicy1960.PolicyData descriptor,
            int profile,int variant,int workers,GpuQualification1961.Cancellation cancellation){
        check(cancellation);
        if(!GpuQualification1961.background()||workers<1||workers>4||!SpeedWorkers1935.cpuIdle1944()||
                workers>SpeedWorkers1935.maxWorkers()||workers>SpeedWorkers1935.availableWorkers1944())return null;
        String exact=GpuStrong1960.profileKey1973(u,profile);
        GpuQualification1961.Record child=GpuQualification1961.exactRejected(exact)?null:GpuQualification1961.restore(exact);
        if(child==null||child.variant!=variant)return null;
        int core=u[0]*(u[3]-u[2]),cf=u[12]==0?0:((u[0]+3)/4)*((u[3]-u[2]+3)/4);
        long peak=12L*workers*(source.length+cf)+16L*(core+cf+1)+workers*StrongNoise1958.workspaceBytes(u[0],u[3]-u[2])+1048576L;
        if(!GpuNoise1960.workspaceFits(peak))return null;
        long competition=SpeedWorkers1935.competitionEpoch1944(),baseline=Long.MAX_VALUE,candidate=0;Measured first=null;
        for(int trial=0;trial<2;trial++){
            Measured cpu=null,gpu=null;
            for(int turn=0;turn<2;turn++){
                check(cancellation);boolean useGpu=(trial+turn)%2!=0;
                Measured result=measure(useGpu?1:0,workers,source,policy,u,model,descriptor,profile,variant,cancellation);
                check(cancellation);if(result.failure!=null){
                    if(useGpu&&"policy_failure".equals(result.failure))GpuQualification1961.rejectExact1971(exact,result.failure);
                    return null;
                }
                if(useGpu)gpu=result;else cpu=result;
            }
            if(difference(cpu,cpu,u)!=null||first!=null&&difference(cpu,first,u)!=null)return null;
            if(first==null)first=cpu;
            String mismatch=difference(gpu,cpu,u);
            if(mismatch!=null){
                if("argb_mismatch".equals(mismatch)||"confidence_mismatch".equals(mismatch)||"policy_failure".equals(mismatch))GpuQualification1961.rejectExact1971(exact,mismatch);
                return null;
            }
            baseline=Math.min(baseline,cpu.nanos);candidate=Math.max(candidate,gpu.nanos);
        }
        check(cancellation);
        GpuQualification1961.Record current=GpuQualification1961.restore(exact);
        if(current==null||current.variant!=child.variant||current.cpuNanos!=child.cpuNanos||current.gpuNanos!=child.gpuNanos||
                competition!=SpeedWorkers1935.competitionEpoch1944())return null;
        return new long[]{baseline,candidate};
    }
    static void prove(int[] source,int[] policy,int[] u,StrongNoise1958.Model model,GpuPolicy1960.PolicyData descriptor,
            int profile,int variant,GpuQualification1961.Cancellation cancellation){
        check(cancellation);
        if(!GpuQualification1961.background()||!SpeedWorkers1935.cpuIdle1944())return;
        final int workers=Math.min(4,Math.min(SpeedWorkers1935.maxWorkers(),SpeedWorkers1935.availableWorkers1944()));
        if(workers<3)return;
        String exact=GpuStrong1960.profileKey1973(u,profile);
        GpuQualification1961.Record child=GpuQualification1961.exactRejected(exact)?null:GpuQualification1961.restore(exact);
        String name=key(u,profile,variant,workers,child);
        if(name==null||GpuQualification1961.restore(name)!=null||!GpuQualification1961.maySchedule(name))return;
        int core=u[0]*(u[3]-u[2]),cf=u[12]==0?0:((u[0]+3)/4)*((u[3]-u[2]+3)/4);
        long peak=16L*workers*(source.length+cf)+16L*(core+cf+1)+workers*StrongNoise1958.workspaceBytes(u[0],u[3]-u[2])+1048576L;
        if(!GpuNoise1960.workspaceFits(peak))return;
        long competition=SpeedWorkers1935.competitionEpoch1944(),baseline=Long.MAX_VALUE,candidate=0;
        Measured first=null;
        for(int trial=0;trial<2;trial++){
            check(cancellation);
            Measured cpu=measure(0,workers,source,policy,u,model,descriptor,profile,variant,cancellation);
            if(cpu.failure!=null)return;
            if(difference(cpu,cpu,u)!=null||first!=null&&difference(cpu,first,u)!=null)return;
            if(first==null)first=cpu;
            Measured gpu=null,mixed=null;
            // AB/BA prevents always giving the candidate the warmed second run.
            for(int turn=0;turn<2;turn++){
                boolean mix=(trial+turn)%2!=0;
                Measured run=measure(mix?2:1,workers,source,policy,u,model,descriptor,profile,variant,cancellation);
                check(cancellation);String mismatch=difference(run,cpu,u);
                if(mismatch!=null){
                    if("argb_mismatch".equals(mismatch)||"confidence_mismatch".equals(mismatch)||"policy_failure".equals(mismatch)){
                        GpuQualification1961.rejectExact1971(exact,mismatch);GpuQualification1961.rejectExact1971(name,mismatch);
                    }
                    return;
                }
                if(mix)mixed=run;else gpu=run;
            }
            if(competition!=SpeedWorkers1935.competitionEpoch1944()||GpuQualification1961.exactRejected(exact))return;
            // Both candidate trials must beat the best real parallel reference,
            // whether that reference is all CPU or queues every task to GPU.
            baseline=Math.min(baseline,Math.min(cpu.nanos,gpu.nanos));candidate=Math.max(candidate,mixed.nanos);
        }
        check(cancellation);
        GpuQualification1961.Record current=GpuQualification1961.restore(exact);
        if(current==null||current.variant!=child.variant||current.cpuNanos!=child.cpuNanos||current.gpuNanos!=child.gpuNanos||
                competition!=SpeedWorkers1935.competitionEpoch1944())return;
        if(candidate>0&&candidate<=baseline-baseline/20)GpuQualification1961.qualified(name,baseline,candidate,1);
        else GpuQualification1961.rejectSpeed(name);
    }
}
