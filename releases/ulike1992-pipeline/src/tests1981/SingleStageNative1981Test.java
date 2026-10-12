package com.hiro.ulike;

import android.content.Context;
import java.lang.reflect.*;
import java.util.*;

/** Actual unchanged GLSL/JNI on Mesa. The qualification is explicitly local to
 * this host test; no device qualification or timing claim is exported. */
public final class SingleStageNative1981Test {
    static long assertions,pixels;static boolean measuredStageAdmitted;
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static void same(int[] a,int[] b,String message){check(a!=null&&b!=null&&a.length==b.length,message+" extent");for(int i=0;i<a.length;i++){check(a[i]==b[i],message+" pixel "+i+" "+Integer.toHexString(a[i])+"/"+Integer.toHexString(b[i]));pixels++;}}
    static int[] image(int w,int h,int seed){Random r=new Random(seed);int[] out=new int[w*h];for(int i=0;i<out.length;i++){int gray=90+r.nextInt(45);out[i]=0xff000000|gray<<16|gray<<8|gray;}return out;}
    static SingleNoise1955.Model model(final int[] source,final int width,final int height) {
        return SingleNoise1955.probe1978(new SingleNoise1955.Patches(){public void read(int[] p,int x,int y,int w,int h){for(int row=0;row<h;row++)System.arraycopy(source,(y+row)*width+x,p,row*w,w);}},width,height,false);
    }
    static Method run(int backend)throws Exception {
        Class<?> type=backend==SingleStage1981.SINGLE?GpuSingle1960.class:GpuResidual1961.class;
        for(Method m:type.getDeclaredMethods())if(m.getName().equals("run1981")){m.setAccessible(true);return m;}
        throw new AssertionError("actual GPU route method absent");
    }
    static int[] apply(Method method,int backend,int[] input,int width,int height,int begin,int end,SingleNoise1955.Model model,SingleStage1981.Lease lease)throws Exception {
        try {
            int count=width*(end-begin);
            if(backend==SingleStage1981.SINGLE) {
                int first=Math.floorDiv(begin-7,4)*4,cols=(width+3)/4+1,rows=(end-first+3)/4,blocks=cols*rows;
                return (int[])method.invoke(null,input,width,height,begin,end,0,height,0,4,true,model,null,
                    first,cols,rows,blocks,count,blocks*992L,lease);
            }
            return (int[])method.invoke(null,input,width,height,begin,end,0,height,0,4,true,model,null,count,null,lease);
        }catch(InvocationTargetException failed){Throwable e=failed.getCause();if(e instanceof Error)throw (Error)e;if(e instanceof Exception)throw (Exception)e;throw failed;}
    }
    static void backend(int backend,int seed)throws Exception {
        int width=33,height=145;int[] original=image(width,height,seed);SingleNoise1955.Model model=model(original,width,height);
        int[] starts={0,48,96,7},ends={48,96,145,31};int[][] inputs=new int[starts.length][],reference=new int[starts.length][];
        Method method=run(backend);
        for(int pass=0;pass<starts.length;pass++) {
            inputs[pass]=original.clone();for(int i=pass;i<inputs[pass].length;i+=17)inputs[pass][i]^=0x00010301;
            int[] cpu=new int[original.length];SingleNoise1955.processCpuRange(inputs[pass],cpu,width,height,starts[pass],ends[pass],0,height,0,4,true,model,null);
            reference[pass]=apply(method,backend,inputs[pass],width,height,starts[pass],ends[pass],model,null);
            same(Arrays.copyOfRange(cpu,starts[pass]*width,ends[pass]*width),reference[pass],"legacy actual GPU versus CPU "+backend+"/"+pass);
        }
        String key="host81-stage-native:"+backend+":"+seed;
        GpuQualification1961.qualified(SingleStage1981.key(key),1000000000L,1L,1);
        check(CpuExact1978.enabled(SingleStage1981.key(key)),"local stage certificate usable");
        SingleStage1981 stage=SingleStage1981.begin(model);GpuNoise1960.Session first=null;
        try {
            for(int pass=0;pass<starts.length;pass++) {
                int[] pristine=inputs[pass].clone();SingleStage1981.Lease lease=SingleStage1981.acquire(model,backend,key);
                check(lease!=null,"actual stage lease acquired");
                if(first==null)first=lease.session;else check(first==lease.session,"actual native session preserved across different ranges");
                try {same(reference[pass],apply(method,backend,inputs[pass],width,height,starts[pass],ends[pass],model,lease),"retained actual GPU exact "+backend+"/"+pass);}
                finally{lease.close();}
                same(pristine,inputs[pass],"actual GPU leaves caller pixels immutable");
                check(GpuNoise1960.sessionBusy(),"successful range keeps only photo session alive");
            }
        }finally{stage.close();}
        check(!GpuNoise1960.sessionBusy()&&!SingleStage1981.owns(model),"photo completion releases actual GPU owner");
        check(SingleResidual1961.retainedBytes1981()==0,"photo completion releases every direct storage owner");
    }
    static final class CountOperation implements SingleStage1981.Operation {
        final SingleStage1981.Operation delegate;int cpuCalls,gpuCalls,closes;
        CountOperation(SingleStage1981.Operation delegate){this.delegate=delegate;}
        public String referenceKey(){return delegate.referenceKey();}
        public SingleStage1981.CpuSample cpu(int pass,GpuQualification1961.Cancellation c){cpuCalls++;SingleStage1981.CpuSample s=delegate.cpu(pass,c);if(s!=null)pixels+=s.pixels.length;return s;}
        public int[] run(SingleStage1981.Lease lease,int pass){gpuCalls++;int[] p=delegate.run(lease,pass);if(p!=null)pixels+=p.length;return p;}
        public long preparationNanos(){return delegate.preparationNanos();}
        public void close(){closes++;delegate.close();}
    }
    static void independentProof()throws Exception {
        int width=33,height=145,begin=7,end=121;int[] source=image(width,height,49181),expected=new int[source.length];
        SingleNoise1955.Model model=model(source,width,height);
        SingleNoise1955.processCpuRange(source,expected,width,height,begin,end,0,height,0,4,true,model,null);
        String original="host81-independent-original",reference=SingleCpu1981.referenceKey(original,width,height,begin,end,0,height,0,4,true,model,null);
        GpuQualification1961.rejectSpeed(original);
        Class<?> gateType=Class.forName("com.hiro.ulike.GpuResidual1961$Gate"),probeType=Class.forName("com.hiro.ulike.GpuResidual1961$ResidualProbe");
        Constructor<?> gc=gateType.getDeclaredConstructor();gc.setAccessible(true);Object gate=gc.newInstance();
        Constructor<?> pc=null;for(Constructor<?> c:probeType.getDeclaredConstructors())if(c.getParameterTypes().length==15)pc=c;
        check(pc!=null,"actual detached residual CPU/GPU snapshot constructor");pc.setAccessible(true);
        Object snapshot=pc.newInstance(original,gate,source,expected,width,height,begin,end,0,height,0,4,true,model,null);
        Class<?> resourceType=Class.forName("com.hiro.ulike.GpuResidual1961$Resources1981");
        Constructor<?> rc=resourceType.getDeclaredConstructor(probeType);rc.setAccessible(true);
        CountOperation operation=new CountOperation((SingleStage1981.Operation)rc.newInstance(snapshot));
        Class<?> proofType=Class.forName("com.hiro.ulike.SingleStage1981$Proof");
        Constructor<?> qc=proofType.getDeclaredConstructor(String.class,String.class,SingleNoise1955.Model.class,int.class,long.class,SingleStage1981.Operation.class);qc.setAccessible(true);
        String stageKey=SingleStage1981.key(reference);
        GpuQualification1961.Probe proof=(GpuQualification1961.Probe)qc.newInstance(original,stageKey,model,SingleStage1981.RESIDUAL,16L*1024*1024,operation);
        try{proof.run(new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}});}
        finally{proof.close();}
        check(operation.cpuCalls==6&&operation.gpuCalls==6,"actual two-source warmup and two full CPU-versus-stage pairs executed");
        check(!GpuQualification1961.exactRejected(stageKey),"every actual GPU pixel matched independently measured CPU output and stable second trial");
        check(operation.closes==1&&!GpuNoise1960.sessionBusy()&&SingleResidual1961.retainedBytes1981()==0,"actual independent proof closes CPU workspaces, direct buffers and GPU owner");
        check(GpuQualification1961.restore(original)==null&&!GpuQualification1961.exactRejected(original),"actual cold stage never promotes or erases the old GPU record");
        measuredStageAdmitted=CpuExact1978.enabled(stageKey);
        if(measuredStageAdmitted){GpuQualification1961.Record r=GpuQualification1961.restore(stageKey);check(r.gpuNanos<=r.cpuNanos-r.cpuNanos/20,"actual admission satisfies measured complete-call 5 percent threshold");}
        else check(!GpuQualification1961.maySchedule(stageKey),"actual slower host candidate remains disabled under bounded speed retry");
    }
    public static void main(String[] args)throws Exception {
        System.loadLibrary("ulike_nr1955");
        check(GpuNoise1960.warmEnvironment1973(),"real EGL environment available");
        GpuQualification1961.initialize(new Context());
        boolean fp64=GpuNoise1960.supports(GpuNoise1960.SINGLE);
        check(GpuNoise1960.supports(GpuNoise1960.RESIDUAL1961),"actual residual shader capability");
        for(int seed:new int[]{1981,8174}){if(fp64)backend(SingleStage1981.SINGLE,seed);backend(SingleStage1981.RESIDUAL,seed);}
        independentProof();
        GpuQualification1961.captureChanged();GpuNoise1960.trimIdle();SpeedWorkers1935.trim();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"pixels\":"+pixels+",\"actual_gpu_jni\":true,\"actual_residual_glsl\":true,\"actual_independent_cpu_stage_trials\":true,\"host_stage_speed_admitted\":"+measuredStageAdmitted+",\"single_fp64_supported\":"+fp64+",\"actual_single_glsl\":"+fp64+",\"physical_android_tested\":false,\"device_speedup_verified\":false}");
    }
}

