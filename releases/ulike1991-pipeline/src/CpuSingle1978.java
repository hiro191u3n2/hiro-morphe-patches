package com.hiro.ulike;

import java.util.Arrays;

/** Per-worker Single NR storage. Only empty capacity is reused; immutable image
 * and model snapshots belong to one after-save proof and are released with it. */
final class CpuSingle1978 {
    private CpuSingle1978() {}
    private static final Object MEMORY=new Object();
    private static long nativeBytes;
    static {
        SpeedWorkers1935.installScratchMemory1978(new SpeedWorkers1935.ScratchMemory(){
            public long retainedBytes(){long owned;synchronized(MEMORY){owned=nativeBytes;}long residual=SingleResidual1961.retainedBytes1981();return residual>Long.MAX_VALUE-owned?Long.MAX_VALUE:owned+residual;}
            // Every retained block is owned by an active shot/proof workspace.
            // Its close follows the worker barrier; trim cannot revoke a lease.
            public void trim(){}
        });
    }
    static long bytes(int width,int rows){return (long)width*(4L*rows+16L*Math.min(64,rows))+128;}
    static long create(int width,int rows) {
        long bytes=bytes(width,rows);
        if(width<1||rows<1||rows>256||bytes<1||bytes>32L*1024*1024)return 0;
        synchronized(MEMORY) {
            if(bytes>64L*1024*1024-nativeBytes)return 0;
            nativeBytes+=bytes;
        }
        // Do not hold MEMORY while asking the GPU owner to trim: that owner
        // includes this reservation in its physical-budget callback.
        long handle=0;
        try{if(!GpuNoise1960.workspaceFits(1))return 0;handle=createWorkspaceNative(width,rows);return handle;}
        catch(RuntimeException unavailable){return 0;}catch(LinkageError unavailable){return 0;}
        finally{if(handle==0)synchronized(MEMORY){nativeBytes-=bytes;}}
    }
    static void release(long handle,int width,int rows) {
        synchronized(MEMORY) {
            releaseWorkspaceNative(handle);nativeBytes-=bytes(width,rows);
            if(nativeBytes<0)throw new IllegalStateException("single NR native accounting");
        }
    }
    static int[] geometry(int width,int rows,int begin,int end,int vb,int ve,int origin,int noise,
            boolean shadows,SingleNoise1955.Model m) {
        return new int[]{width,rows,begin,end,vb,ve,origin,noise,shadows?1:0,m.width,m.height,
            m.columns,m.rows,m.gpuPatchWidth1960(),m.gpuPatchHeight1960()};
    }
    static String key(boolean nativePath,int[] u,boolean policy) {
        int[] shape=u.clone();shape[6]&=7;
        return "cpu-single1978-workspace-exact2-time5-v1:"+(nativePath?1:0)+":"+policy+":"+Arrays.toString(shape);
    }
    static boolean nativeCall(int[] input,int[] output,int[] u,float[] model,int[] policy,SingleNoise1955.Workspace workspace) {
        return nativeCall(input,output,u,model,policy,workspace,false);
    }
    private static boolean nativeCall(int[] input,int[] output,int[] u,float[] model,int[] policy,SingleNoise1955.Workspace workspace,boolean required) {
        long handle=workspace==null?0:workspace.nativeHandle(u[0],u[3]-u[2]);boolean active=false;
        try {
            if(handle!=0)active=beginWorkspaceNative(handle);
            if(required&&!active)return false;
            return ColourCache1976.cpu(input,output,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[7],u[8]!=0,
                u[9],u[10],u[11],u[12],u[13],u[14],model,policy);
        }finally{if(active)endWorkspaceNative(handle);}
    }
    static void offerNative(String key,final int[] source,final int[] u,final float[] model,final int[] policy) {
        final int count=Math.multiplyExact(u[0],u[1]),core=Math.multiplyExact(u[0],u[3]-u[2]);
        long retained=4L*(count+model.length+(policy==null?0:core*2))+4096;
        long peak=16L*count+24L*core+bytes(u[0],u[3]-u[2])+1024L*1024;
        CpuExact1978.offer(key,retained,peak,new CpuExact1978.Factory(){
            public CpuExact1978.Work create(){return new Native(Arrays.copyOf(source,count),u.clone(),model.clone(),policy==null?null:Arrays.copyOf(policy,core*2));}
        });
    }
    private static final class Native implements CpuExact1978.Work {
        int[] source,u,policy;float[] model;SingleNoise1955.Workspace workspace=new SingleNoise1955.Workspace();
        Native(int[] source,int[] u,float[] model,int[] policy){this.source=source;this.u=u;this.model=model;this.policy=policy;}
        public Object run(boolean reused) {
            if(source==null)return null;int[] output=new int[source.length],prepared=null;
            workspace.acquire();
            try {
                if(policy!=null){prepared=reused?workspace.policy(policy.length):new int[policy.length];System.arraycopy(policy,0,prepared,0,policy.length);}
                return nativeCall(source,output,u,model,prepared,reused?workspace:null,reused)?output:null;
            }finally{workspace.relinquish();}
        }
        public void close(){source=null;u=null;policy=null;model=null;if(workspace!=null){workspace.close();workspace=null;}}
    }
    static void javaRange(final int[] source,int[] output,final int width,final int rows,final int begin,final int end,
            final int vb,final int ve,final int origin,final int noise,final boolean shadows,final SingleNoise1955.Model model,
            SingleNoise1955.Protection protection,SingleNoise1955.Workspace workspace) {
        final int[] u=geometry(width,rows,begin,end,vb,ve,origin,noise,shadows,model);
        final String key=key(false,u,protection!=null);
        boolean pure=protection==null||protection instanceof SingleCpu1981.FrozenProtection||protection instanceof GpuPolicy1960.Protection&&
            CpuFinishPolicy1978.knownFace(((GpuPolicy1960.Protection)protection).plan);
        boolean reused=pure&&CpuExact1978.enabled(key);
        if(!reused)workspace.original();
        SingleNoise1955.processJavaRange1978(source,output,width,rows,begin,end,vb,ve,origin,noise,shadows,model,protection,reused?workspace:null);
        if(!pure||reused||end<=begin||noise<=0)return;
        final int count=Math.multiplyExact(width,rows),core=Math.multiplyExact(width,end-begin);
        final SingleNoise1955.Protection original=protection;
        long retained=4L*count+8L*core+8L*model.gpuData1960().length+4096;
        long peak=16L*count+24L*core+bytes(width,end-begin)+1024L*1024;
        CpuExact1978.offer(key,retained,peak,new CpuExact1978.Factory(){
            public CpuExact1978.Work create() {
                int[] frozen=null;
                if(original!=null){frozen=new int[core*2];for(int y=begin;y<end;y++)for(int x=0;x<width;x++) {
                    if(((y-begin)*width+x)%4096==0&&Thread.currentThread().isInterrupted())return null;
                    int at=((y-begin)*width+x)*2;
                    frozen[at]=Math.max(0,Math.min(256,original.budgetQ8(x,y+origin)));
                    frozen[at+1]=Math.max(0,Math.min(256,original.detailQ8(x,y+origin)));
                }}
                return new Java(Arrays.copyOf(source,count),u.clone(),model,frozen);
            }
        });
    }
    private static final class Java implements CpuExact1978.Work {
        int[] source,u,policy;SingleNoise1955.Model model;SingleNoise1955.Workspace workspace=new SingleNoise1955.Workspace();
        Java(int[] source,int[] u,SingleNoise1955.Model model,int[] policy){this.source=source;this.u=u;this.model=model;this.policy=policy;}
        public Object run(boolean reused) {
            if(source==null)return null;int[] output=new int[source.length];
            SingleNoise1955.Protection frozen=policy==null?null:new SingleNoise1955.Protection(){
                public int budgetQ8(int x,int y){return policy[((y-u[6]-u[2])*u[0]+x)*2];}
                public int detailQ8(int x,int y){return policy[((y-u[6]-u[2])*u[0]+x)*2+1];}
            };
            workspace.acquire();try {
                SingleNoise1955.processJavaRange1978(source,output,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[7],u[8]!=0,model,frozen,reused?workspace:null);
                return output;
            }finally{workspace.relinquish();}
        }
        public void close(){source=null;u=null;policy=null;model=null;if(workspace!=null){workspace.close();workspace=null;}}
    }
    static String probeKey(int width,int height){return "cpu-single1978-probe-exact2-time5-v1:"+width+":"+height;}
    static SingleNoise1955.Model probe(final SingleNoise1955.Patches source,final int width,final int height) {
        if(source==null||width<1||height<1)return SingleNoise1955.probe1978(source,width,height,false);
        final String key=probeKey(width,height);boolean reused=CpuExact1978.enabled(key);
        int nx=Math.max(1,Math.min(13,(width+255)/256)),ny=Math.max(1,Math.min(13,(height+255)/256));
        final int pw=Math.min(48,width),ph=Math.min(48,height),count=nx*ny;
        final long retained=4L*count*pw*ph+4096,peak=2L*1024*1024;
        int[] capture=null;
        if(!reused&&CpuExact1978.canCapture(key,retained,peak))try{capture=new int[count*pw*ph];}catch(OutOfMemoryError optional){}
        final int[] patches=capture;
        SingleNoise1955.Patches reader=patches==null?source:new SingleNoise1955.Patches(){int at;
            public void read(int[] out,int x,int y,int w,int h){source.read(out,x,y,w,h);System.arraycopy(out,0,patches,at++*pw*ph,pw*ph);}
        };
        SingleNoise1955.Model result=SingleNoise1955.probe1978(reader,width,height,reused);
        if(patches!=null)CpuExact1978.offer(key,retained,peak,new CpuExact1978.Factory(){
            public CpuExact1978.Work create(){return new Probe(patches,width,height);}
        });
        return result;
    }
    private static final class Probe implements CpuExact1978.Work {
        int[] patches;final int width,height;
        Probe(int[] patches,int width,int height){this.patches=patches;this.width=width;this.height=height;}
        public Object run(boolean reused) {
            if(patches==null)return null;final int pw=Math.min(48,width),ph=Math.min(48,height);
            SingleNoise1955.Patches reader=new SingleNoise1955.Patches(){int at;
                public void read(int[] out,int x,int y,int w,int h){if(w!=pw||h!=ph||(long)(at+1)*pw*ph>patches.length)throw new IllegalStateException("single probe snapshot traversal");System.arraycopy(patches,at++*pw*ph,out,0,pw*ph);}
            };
            SingleNoise1955.Model m=SingleNoise1955.probe1978(reader,width,height,reused);
            return new Object[]{m.gpuData1960(),new int[]{m.width,m.height,m.columns,m.rows,m.samples}};
        }
        public void close(){patches=null;}
    }
    private static native long createWorkspaceNative(int width,int coreRows);
    private static native void releaseWorkspaceNative(long handle);
    private static native boolean beginWorkspaceNative(long handle);
    private static native void endWorkspaceNative(long handle);
}
