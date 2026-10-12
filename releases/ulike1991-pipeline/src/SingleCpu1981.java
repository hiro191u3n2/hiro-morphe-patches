package com.hiro.ulike;

import java.util.HashMap;
import java.util.Map;

/** Independent CPU workspaces for detached GPU comparisons. The original GPU
 * equality key never changes when only the CPU execution route becomes faster. */
final class SingleCpu1981 implements AutoCloseable {
    /** Only private copied Q8 arrays may implement this internal marker. */
    interface FrozenProtection extends SingleNoise1955.Protection {}
    /** A completed numerical comparison is different from an unavailable run. */
    static final class Mismatch extends RuntimeException {
        Mismatch(String message){super(message);}
    }
    private final Map<String,SingleNoise1955.Workspace> workspaces=new HashMap<String,SingleNoise1955.Workspace>();
    private boolean closed;
    static String referenceKey(String exact,int width,int rows,int begin,int end,int vb,int ve,int origin,
            int noise,boolean shadows,SingleNoise1955.Model model,SingleNoise1955.Protection protection) {
        int[] u=CpuSingle1978.geometry(width,rows,begin,end,vb,ve,origin,noise,shadows,model);
        int units=(int)(((long)end-begin+3)/4),workers=Math.max(1,Math.min(units,SpeedWorkers1935.maxWorkers()));
        // The detached oracle compares serial execution with the full idle
        // worker pool. Foreground calls inside a worker must form the same key;
        // transient available-permit counts therefore do not belong here.
        StringBuilder state=new StringBuilder();state.append(reuseState(u,protection!=null));
        if(workers>1)for(int i=0;i<workers;i++) {
            int[] part=u.clone();part[2]=begin+(int)((long)units*i/workers)*4;
            part[3]=i+1==workers?end:begin+(int)((long)units*(i+1)/workers)*4;
            state.append(reuseState(part,protection!=null));
        }
        return "single-cpu-reference1981-v2:"+SingleNoise1955.nativeAvailable()+":"+workers+":"+state+":"+exact;
    }
    private static int reuseState(int[] u,boolean policy) {
        return (CpuExact1978.enabled(CpuSingle1978.key(true,u,policy))?1:0)|
            (CpuExact1978.enabled(CpuSingle1978.key(false,u,policy))?2:0)|
            (ColourCache1976.cpuEnabled1981(u,policy)?4:0);
    }
    static String speedKey(String reference,boolean stage){return "single-cpu-speed1981-v2:"+stage+":"+reference;}
    static String speedKey(String exact,int width,int rows,int begin,int end,int vb,int ve,int origin,
            int noise,boolean shadows,SingleNoise1955.Model model,SingleNoise1955.Protection protection) {
        String reference=referenceKey(exact,width,rows,begin,end,vb,ve,origin,noise,shadows,model,protection);
        return speedKey(reference,SingleStage1981.selected(model,reference));
    }
    static boolean prefersCpu(String speed) {
        GpuQualification1961.Record record=GpuQualification1961.restore(speed);
        return record!=null?record.variant==1:!GpuQualification1961.maySchedule(speed);
    }
    static void measured(String exact,String speed,long fastestCpu,long slowestCpu,long fastestGpu,long slowestGpu) {
        if(fastestCpu>0&&slowestGpu>0&&slowestGpu<=fastestCpu-fastestCpu/20) {
            GpuQualification1961.qualified(speed,fastestCpu,slowestGpu,0);
        } else if(fastestGpu>0&&slowestCpu>0&&slowestCpu<=fastestGpu-fastestGpu/20&&!exact.equals(speed))
            GpuQualification1961.qualified(speed,fastestGpu,slowestCpu,1);
        else GpuQualification1961.rejectSpeed(speed);
    }
    void process(int[] input,int[] output,int width,int rows,int begin,int end,int vb,int ve,int origin,
            int noise,boolean shadows,SingleNoise1955.Model model,SingleNoise1955.Protection protection) {
        SingleNoise1955.Workspace workspace;
        synchronized(this) {
            if(closed)throw new IllegalStateException("single CPU proof workspace closed");
            // Serial and the first disjoint parallel slice are sequential and
            // share their start. Reuse the larger capacity between them so
            // four parallel workers never compete with a fifth serial handle.
            // Each detached owner has fixed image geometry; simultaneous
            // slices have distinct starts and therefore distinct workspaces.
            String part=Integer.toString(begin);
            workspace=workspaces.get(part);
            if(workspace==null){workspace=new SingleNoise1955.Workspace();workspaces.put(part,workspace);}
        }
        workspace.acquire();
        try {SingleNoise1955.processCpuRange1978(input,output,width,rows,begin,end,vb,ve,origin,
            noise,shadows,model,protection,workspace);}
        finally {workspace.relinquish();}
    }
    /** Complete actual serial and disjoint-worker CPU routes are both checked
     * before the faster wall time becomes a stage admission reference. */
    SingleStage1981.CpuSample sample(final int[] input,int[] foreground,final int width,final int rows,
            final int begin,final int end,final int vb,final int ve,final int origin,final int noise,
            final boolean shadows,final SingleNoise1955.Model model,final SingleNoise1955.Protection protection,
            final GpuQualification1961.Cancellation cancellation) {
        if(cancellation.cancelled()||Thread.currentThread().isInterrupted()||!SpeedWorkers1935.cpuIdle1944())return null;
        final long competition=SpeedWorkers1935.competitionEpoch1944();
        final int[] output=new int[input.length];long serial;
        boolean permit=SpeedWorkers1935.enterLegacy();
        try {
            if(cancellation.cancelled()||Thread.currentThread().isInterrupted())return null;
            long started=System.nanoTime();
            process(input,output,width,rows,begin,end,vb,ve,origin,noise,shadows,model,protection);
            serial=System.nanoTime()-started;
        } finally {SpeedWorkers1935.leaveLegacy(permit);}
        int count=Math.multiplyExact(width,end-begin);
        int[] reference=new int[count];System.arraycopy(output,begin*width,reference,0,count);
        if(cancellation.cancelled()||Thread.currentThread().isInterrupted())return null;
        if(foreground!=null&&!java.util.Arrays.equals(foreground,reference))throw new Mismatch("captured and serial CPU pixels differ");
        // A partially available pool cannot certify the full-pool speed key.
        if(!SpeedWorkers1935.cpuIdle1944()||competition!=SpeedWorkers1935.competitionEpoch1944())return null;
        long parallel=GpuQualification1961.parallelRows1964(begin,end,4,cancellation,new GpuQualification1961.RowTask1964(){
            public boolean run(int first,int last) {
                process(input,output,width,rows,first,last,vb,ve,origin,noise,shadows,model,protection);return true;
            }
        });
        if(cancellation.cancelled()||Thread.currentThread().isInterrupted()||serial<=0||parallel<=0)return null;
        for(int i=0;i<count;i++)if(reference[i]!=output[begin*width+i])throw new Mismatch("serial and parallel CPU pixels differ");
        if(!SpeedWorkers1935.cpuIdle1944()||competition!=SpeedWorkers1935.competitionEpoch1944())return null;
        return new SingleStage1981.CpuSample(reference,Math.min(serial,parallel));
    }
    public synchronized void close() {
        if(closed)return;closed=true;
        Throwable failure=null;
        for(SingleNoise1955.Workspace workspace:workspaces.values())try{workspace.close();}
            catch(RuntimeException error){if(failure==null)failure=error;}
            catch(Error error){if(failure==null)failure=error;}
        workspaces.clear();
        if(failure instanceof Error)throw (Error)failure;
        if(failure!=null)throw (RuntimeException)failure;
    }
}
