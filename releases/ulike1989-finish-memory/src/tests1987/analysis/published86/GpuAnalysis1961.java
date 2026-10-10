package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** GX11/GX20: original sampled evidence and a resident coarse model graph.
 * Foreground unknown configurations use the original CPU algorithm. Detached,
 * idle-only proofs compare every value twice before a GPU configuration is used.
 * No photograph, detector result or pooled foreground workspace is retained. */
public final class GpuAnalysis1961 {
    private GpuAnalysis1961() {}
    private static boolean rejected(String key){return !GpuQualification1961.maySchedule(key);}
    private static void reject(String key){GpuQualification1961.rejectExact(key);}
    private static void slow(String key){GpuQualification1961.rejectSpeed(key);}
    private static void cancelled(){if(Thread.currentThread().isInterrupted())throw new CancellationException("GPU analysis cancelled");}
    private static boolean win(long cpu,long gpu){return cpu>0&&gpu>0&&gpu<=cpu-cpu/20;}
    private static final class Deferred extends RuntimeException {}
    private static int ceil2(int x){return x/2+(x&1);}
    private static float[] floats(int[] bits){float[] result=new float[bits.length];for(int i=0;i<bits.length;i++)result[i]=Float.intBitsToFloat(bits[i]);return result;}
    private static boolean equal(float[] a,float[] b){if(a==null||b==null||a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(Float.floatToRawIntBits(a[i])!=Float.floatToRawIntBits(b[i]))return false;return true;}
    private static final class Snapshot {
        int[] pixels;
        final int[] xs,ys;final int pw,ph,count;final long gatherNanos;
        Snapshot(int[] p,int[] x,int[] y,int w,int h,long ns){pixels=p;xs=x;ys=y;pw=w;ph=h;count=x.length;gatherNanos=ns;}
        Reader reader(){return new Reader(this);}
        long bytes(){return 4L*(pixels.length+xs.length+ys.length)+256;}
        void close(){pixels=null;}
    }
    private static final class Reader implements SpatialNoise1934.Patches,StrongNoise1958.Patches {
        final Snapshot snapshot;int cursor;
        Reader(Snapshot s){snapshot=s;}
        public void read(int[] dst,int x,int y,int w,int h){
            if(cursor>=snapshot.count||w!=snapshot.pw||h!=snapshot.ph||x!=snapshot.xs[cursor]||y!=snapshot.ys[cursor])throw new IllegalArgumentException("GPU detached patch traversal");
            System.arraycopy(snapshot.pixels,cursor*w*h,dst,0,w*h);cursor++;
        }
    }
    private static Snapshot spatialSnapshot(SpatialNoise1934.Patches source,int width,int height){
        long start=System.nanoTime();int pw=Math.min(64,width),ph=Math.min(64,height),nx=Math.max(1,Math.min(13,(width+127)/128)),ny=Math.max(1,Math.min(13,(height+127)/128));
        int count=nx*ny;long length=(long)count*pw*ph;if(length>Integer.MAX_VALUE||!GpuNoise1960.workspaceFits(length*4+32768))throw new Deferred();
        int[] all=new int[(int)length],patch=new int[pw*ph],xs=new int[count],ys=new int[count];
        for(int gy=0;gy<ny;gy++)for(int gx=0;gx<nx;gx++){
            cancelled();int at=gy*nx+gx;xs[at]=nx==1?(width-pw)/2:Math.round((float)(width-pw)*gx/(nx-1));ys[at]=ny==1?(height-ph)/2:Math.round((float)(height-ph)*gy/(ny-1));
            source.read(patch,xs[at],ys[at],pw,ph);System.arraycopy(patch,0,all,at*pw*ph,pw*ph);
        }
        return new Snapshot(all,xs,ys,pw,ph,System.nanoTime()-start);
    }
    static SpatialNoise1934 spatial(SpatialNoise1934.Patches source,final int width,final int height){
        if(source==null||width<1||height<1)throw new IllegalArgumentException("noise map source");
        final String key="spatial1961:v2:"+width+":"+height;
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof!=null&&!rejected(key)&&!GpuNoise1960.sessionBusy()){
            Snapshot s=null;
            try{long start=System.nanoTime();s=spatialSnapshot(source,width,height);SpatialNoise1934 candidate=spatialCandidate1961(s.pixels,width,height,proof.variant);long elapsed=System.nanoTime()-start;cancelled();if(candidate!=null){if(!win(proof.cpuNanos,elapsed))slow(key);return candidate;}slow(key);}
            catch(Deferred unavailable){}catch(CancellationException stop){throw stop;}catch(RuntimeException failure){slow(key);}catch(LinkageError failure){slow(key);}catch(OutOfMemoryError unavailable){}
            finally{if(s!=null)s.close();}
        }
        long start=System.nanoTime();final SpatialNoise1934 reference=SpatialNoise1934.probeCpu1961(source,width,height);final long originalCpu=System.nanoTime()-start;
        if(rejected(key)||GpuQualification1961.background()||GpuNoise1960.sessionBusy()||!GpuNoise1960.available())return reference;
        try{
            int pw=Math.min(64,width),ph=Math.min(64,height),nx=Math.max(1,Math.min(13,(width+127)/128)),ny=Math.max(1,Math.min(13,(height+127)/128));
            long estimate=4L*nx*ny*((long)pw*ph+2)+256;
            if(!GpuQualification1961.canQueue(key,estimate))return reference;
            final Snapshot s=spatialSnapshot(source,width,height);
            if(!GpuQualification1961.canQueue(key,s.bytes())){s.close();return reference;}
            GpuQualification1961.schedule(key,s.bytes(),new GpuQualification1961.Probe(){
                public void run(GpuQualification1961.Cancellation c){
                    try {
                    if(c.cancelled())return;if(!GpuNoise1960.supports(GpuNoise1960.ANALYSIS1961)){if(!c.cancelled()&&!GpuNoise1960.sessionBusy())slow(key);return;}
                    long minCpu=Long.MAX_VALUE,maxGpu=0;
                    for(int pair=0;pair<2;pair++){
                        if(c.cancelled())return;
                        long cpuStart=System.nanoTime();SpatialNoise1934 oracle=SpatialNoise1934.probeCpu1961(s.reader(),width,height);long cpu=Math.min(originalCpu,System.nanoTime()-cpuStart+s.gatherNanos);
                        long start=System.nanoTime();SpatialNoise1934 candidate=spatialCandidate1961(s.pixels,width,height,0);long gpu=System.nanoTime()-start+s.gatherNanos;
                        if(c.cancelled())return;
                        if(candidate==null){slow(key);return;}
                        if(!SpatialNoise1934.same1961(reference,oracle)||!SpatialNoise1934.same1961(oracle,candidate)){reject(key);return;}
                        if(!win(cpu,gpu)){slow(key);return;}
                        minCpu=Math.min(minCpu,cpu);maxGpu=Math.max(maxGpu,gpu);
                    }
                    if(!c.cancelled()){if(win(minCpu,maxGpu))GpuQualification1961.qualified(key,minCpu,maxGpu,0);else slow(key);}
                    }catch(Deferred unavailable){}catch(CancellationException stop){throw stop;}
                    catch(RuntimeException failure){if(!c.cancelled())slow(key);}
                    catch(LinkageError failure){if(!c.cancelled())slow(key);}
                    catch(OutOfMemoryError unavailable){}
                }
                public void close(){s.close();}
            });
        }catch(Deferred unavailable){}catch(CancellationException stop){throw stop;}catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        return reference;
    }
    /** Direct host QA entry: exact production command graph; no admission bypass
     * is used by the photograph route. Patch pixels must be in original order. */
    static SpatialNoise1934 spatialCandidate1961(int[] patches,int width,int height,int variant){
        int pw=Math.min(64,width),ph=Math.min(64,height),nx=Math.max(1,Math.min(13,(width+127)/128)),ny=Math.max(1,Math.min(13,(height+127)/128)),count=nx*ny;
        if(pw<2||ph<2)return null;
        int per=(pw-1)*(ph-1),samples=count*per,chunks=(samples+255)/256,hist=2051*(count+1),values=5*(count+1)+count+5;
        long bytes=4L*(patches.length+2L*samples+chunks+hist+values)+65536;
        if(patches.length!=count*pw*ph||!GpuNoise1960.workspaceFits(bytes+4L*values)||GpuNoise1960.sessionBusy())throw new Deferred();
        int shader=GpuNoise1960.variant(GpuNoise1960.ANALYSIS1961,variant);if(!GpuNoise1960.supports(shader))return null;
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)throw new Deferred();
        try{
            GpuNoise1960.Batch b=new GpuNoise1960.Batch().upload(0,patches).allocate(1,4L*samples).allocate(2,4L*values).allocate(3,4L*hist).allocate(4,4).allocate(5,4L*samples).allocate(6,4L*chunks).allocate(7,4);
            int[] u=new int[32];u[1]=pw;u[2]=ph;u[3]=count;u[4]=per;u[5]=samples;u[6]=chunks;u[7]=hist;int[] bindings={0,1,2,3,4,5,6,7};float[] f=new float[32];
            u[0]=6;b.dispatch(shader,bindings,u,f,hist);u[0]=0;b.dispatch(shader,bindings,u,f,samples);u[0]=1;b.dispatch(shader,bindings,u,f,chunks);u[0]=2;b.dispatch(shader,bindings,u,f,1);u[0]=3;b.dispatch(shader,bindings,u,f,samples);u[0]=4;b.dispatch(shader,bindings,u,f,count+1);u[0]=5;b.dispatch(shader,bindings,u,f,1);
            int[][] result=session.execute(b,new int[]{2},new int[]{values});cancelled();if(result==null)return null;
            float[] a=floats(result[0]),sigma=Arrays.copyOfRange(a,5*(count+1),5*(count+1)+count);int at=5*(count+1)+count;
            return SpatialNoise1934.fromGpu1961(width,height,nx,ny,sigma,new QualityPixels1932.NoiseStats(a[at],a[at+1],a[at+2],a[at+3],result[0][at+4]));
        }finally{session.close();}
    }

    /** GX24: sampled post-geometry analysis keeps the full ARGB image in its
     * caller's GPU session. Only the bounded map/statistics are read back for
     * the exact Java finishing policy and CPU double-sampled masks. Slots 8-16
     * are reserved by this helper; imageSlot must be outside that range. Whole
     * chain qualification, rather than a stage-only proof, owns admission. */
    static SpatialNoise1934 residentSpatial1962(GpuNoise1960.Session session,
            int imageSlot,int width,int height) {
        if(session==null||imageSlot>=8&&imageSlot<=16||width<2||height<2)return null;
        int pw=Math.min(64,width),ph=Math.min(64,height),nx=Math.max(1,Math.min(13,(width+127)/128)),ny=Math.max(1,Math.min(13,(height+127)/128)),count=nx*ny;
        int per=(pw-1)*(ph-1),samples=count*per,chunks=(samples+255)/256,hist=2051*(count+1),values=5*(count+1)+count+5;
        long bytes=4L*(count*pw*ph+2L*samples+chunks+hist+values+2L*count)+65536;
        if(!GpuNoise1960.workspaceFits(bytes+4L*values))throw new Deferred();
        int[] coordinates=new int[count*2];
        for(int gy=0;gy<ny;gy++)for(int gx=0;gx<nx;gx++) {
            int at=(gy*nx+gx)*2;
            coordinates[at]=nx==1?(width-pw)/2:Math.round((float)(width-pw)*gx/(nx-1));
            coordinates[at+1]=ny==1?(height-ph)/2:Math.round((float)(height-ph)*gy/(ny-1));
        }
        int[] u=new int[32];u[0]=20;u[1]=pw;u[2]=ph;u[3]=count;u[4]=per;u[5]=samples;u[6]=chunks;u[7]=hist;u[8]=width;
        int shader=GpuNoise1960.ANALYSIS1961;float[] f=new float[32];
        GpuNoise1960.Batch b=new GpuNoise1960.Batch().allocate(8,4L*count*pw*ph)
            .allocate(9,4L*samples).allocate(10,4L*values).allocate(11,4L*hist)
            .allocate(12,4).allocate(13,4L*samples).allocate(14,4L*chunks).allocate(15,4).uploadDirect(16,coordinates);
        b.dispatch(shader,new int[]{imageSlot,8,10,11,12,13,14,16},u,f,count*pw*ph);
        int[] bindings={8,9,10,11,12,13,14,15};
        u[0]=6;b.dispatch(shader,bindings,u,f,hist);u[0]=0;b.dispatch(shader,bindings,u,f,samples);u[0]=1;b.dispatch(shader,bindings,u,f,chunks);u[0]=2;b.dispatch(shader,bindings,u,f,1);u[0]=3;b.dispatch(shader,bindings,u,f,samples);u[0]=4;b.dispatch(shader,bindings,u,f,count+1);u[0]=5;b.dispatch(shader,bindings,u,f,1);
        int[] result=new int[values];if(!session.executeInto(b,10,values,result,0))return null;cancelled();
        float[] a=floats(result),sigma=Arrays.copyOfRange(a,5*(count+1),5*(count+1)+count);int at=5*(count+1)+count;
        return SpatialNoise1934.fromGpu1961(width,height,nx,ny,sigma,new QualityPixels1932.NoiseStats(a[at],a[at+1],a[at+2],a[at+3],result[at+4]));
    }

    interface RegionsCpu {float[] compute(StrongNoise1958.Patches source);}
    private static Snapshot regionSnapshot(StrongNoise1958.Patches source,int width,int height){
        long start=System.nanoTime();int pw=Math.min(32,width),ph=Math.min(32,height),nx=(width+63)/64,ny=(height+63)/64,count=nx*ny;
        long length=(long)count*pw*ph;if(length>Integer.MAX_VALUE||!GpuNoise1960.workspaceFits(length*4+32768))throw new Deferred();
        int[] all=new int[(int)length],patch=new int[pw*ph],xs=new int[count],ys=new int[count];
        for(int gy=0;gy<ny;gy++)for(int gx=0;gx<nx;gx++){
            cancelled();int at=gy*nx+gx;xs[at]=Math.max(0,Math.min(width-pw,gx*64+32-pw/2));ys[at]=Math.max(0,Math.min(height-ph,gy*64+32-ph/2));
            source.read(patch,xs[at],ys[at],pw,ph);System.arraycopy(patch,0,all,at*pw*ph,pw*ph);
        }
        return new Snapshot(all,xs,ys,pw,ph,System.nanoTime()-start);
    }
    static float[] regions(StrongNoise1958.Patches source,final int width,final int height,final float[] evidence,final RegionsCpu cpu){
        return regions1982(source,width,height,evidence,cpu,null);
    }
    /** Optional caller-owned scalar; the returned CPU reference is also success. */
    static float[] regions1982(StrongNoise1958.Patches source,final int width,final int height,final float[] evidence,final RegionsCpu cpu,int[] selected1982){
        final String key="regions1961:v2:"+width+":"+height;
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof!=null&&!rejected(key)&&!GpuNoise1960.sessionBusy()){
            Snapshot s=null;
            try{long start=System.nanoTime();s=regionSnapshot(source,width,height);float[] result=regionsCandidate1961(s.pixels,width,height,evidence,proof.variant);long elapsed=System.nanoTime()-start;cancelled();if(result!=null){if(!win(proof.cpuNanos,elapsed))slow(key);if(selected1982!=null)selected1982[0]=1;return result;}slow(key);}
            catch(Deferred unavailable){}catch(CancellationException stop){throw stop;}catch(RuntimeException failure){slow(key);}catch(LinkageError failure){slow(key);}catch(OutOfMemoryError unavailable){}
            finally{if(s!=null)s.close();}
        }
        long start=System.nanoTime();final float[] reference=cpu.compute(source);final long originalCpu=System.nanoTime()-start;
        if(selected1982!=null)selected1982[0]=0;
        if(rejected(key)||GpuQualification1961.background()||GpuNoise1960.sessionBusy()||!GpuNoise1960.available())return reference;
        try{
            int pw=Math.min(32,width),ph=Math.min(32,height),nx=(width+63)/64,ny=(height+63)/64;
            long estimate=4L*nx*ny*((long)pw*ph+2)+256+4L*(16+reference.length);
            if(!GpuQualification1961.canQueue(key,estimate))return reference;
            final Snapshot s=regionSnapshot(source,width,height);final float[] ev=Arrays.copyOf(evidence,16);
            if(!GpuQualification1961.canQueue(key,s.bytes()+4L*(ev.length+reference.length))){s.close();return reference;}
            GpuQualification1961.schedule(key,s.bytes()+4L*(ev.length+reference.length),new GpuQualification1961.Probe(){
                public void run(GpuQualification1961.Cancellation c){
                    try {
                    if(c.cancelled())return;if(!GpuNoise1960.supports(GpuNoise1960.ANALYSIS1961)){if(!c.cancelled()&&!GpuNoise1960.sessionBusy())slow(key);return;}
                    long minCpu=Long.MAX_VALUE,maxGpu=0;
                    for(int pair=0;pair<2;pair++){
                        if(c.cancelled())return;long cpuStart=System.nanoTime();float[] oracle=cpu.compute(s.reader());long cpuTime=Math.min(originalCpu,System.nanoTime()-cpuStart+s.gatherNanos);long start=System.nanoTime();float[] candidate=regionsCandidate1961(s.pixels,width,height,ev,0);long gpu=System.nanoTime()-start+s.gatherNanos;
                        if(c.cancelled())return;if(candidate==null){slow(key);return;}if(!equal(reference,oracle)||!equal(oracle,candidate)){reject(key);return;}if(!win(cpuTime,gpu)){slow(key);return;}minCpu=Math.min(minCpu,cpuTime);maxGpu=Math.max(maxGpu,gpu);
                    }
                    if(!c.cancelled()){if(win(minCpu,maxGpu))GpuQualification1961.qualified(key,minCpu,maxGpu,0);else slow(key);}
                    }catch(Deferred unavailable){}catch(CancellationException stop){throw stop;}
                    catch(RuntimeException failure){if(!c.cancelled())slow(key);}
                    catch(LinkageError failure){if(!c.cancelled())slow(key);}
                    catch(OutOfMemoryError unavailable){}
                }
                public void close(){s.close();}
            });
        }catch(Deferred unavailable){}catch(CancellationException stop){throw stop;}catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        return reference;
    }
    static float[] regionsCandidate1961(int[] patches,int width,int height,float[] ev,int variant){
        int pw=Math.min(32,width),ph=Math.min(32,height),regions=((width+63)/64)*((height+63)/64),values=3*regions;
        long bytes=4L*(patches.length+2064L*regions+values)+65536;
        if(patches.length!=(long)regions*pw*ph||!GpuNoise1960.workspaceFits(bytes+4L*values)||GpuNoise1960.sessionBusy())throw new Deferred();
        int shader=GpuNoise1960.variant(GpuNoise1960.ANALYSIS1961,variant);if(!GpuNoise1960.supports(shader))return null;
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)throw new Deferred();
        try{
            GpuNoise1960.Batch b=new GpuNoise1960.Batch().upload(0,patches).allocate(1,4).allocate(2,4L*values).allocate(3,4L*2064*regions).upload(4,Arrays.copyOf(ev,16)).allocate(5,4).allocate(6,4).allocate(7,4);
            int[] u=new int[32];u[0]=10;u[1]=pw;u[2]=ph;u[3]=regions;b.dispatch(shader,new int[]{0,1,2,3,4,5,6,7},u,new float[32],regions);
            int[][] result=session.execute(b,new int[]{2},new int[]{values});cancelled();return result==null?null:floats(result[0]);
        }finally{session.close();}
    }

    /** The accepted model stage transfers this single owner to its first full
     * Strong stage. Closing an unused lease releases all resident coarse maps. */
    static final class Resident implements AutoCloseable {
        final GpuNoise1960.Session session;final long setupNanos;boolean closed;
        Resident(GpuNoise1960.Session s,long ns){session=s;setupNanos=ns;}
        public synchronized void close(){if(!closed){closed=true;session.close();}}
    }
    private static String residentKey(int width,int height,int noise,boolean shadows){return "resident1961:v2:"+width+":"+height+":"+noise+":"+shadows;}
    static StrongNoise1958.Model residentIfQualified(int[] h,int width,int height,int noise,boolean shadows,float[] evidence,float[] runtime){
        String key=residentKey(width,height,noise,shadows);GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof==null||rejected(key)||GpuQualification1961.background()||GpuNoise1960.sessionBusy())return null;
        StrongNoise1958.Model model=null;boolean publish=false;
        try{
            model=residentCandidate1961(h,width,height,noise,shadows,evidence,runtime,proof.variant);
            cancelled();if(model==null){slow(key);return null;}
            Resident lease=StrongNoise1958.takeResident1961(model);if(!win(proof.cpuNanos,lease.setupNanos))slow(key);StrongNoise1958.attachResident1961(model,lease);publish=true;return model;
        }catch(Deferred unavailable){return null;}catch(CancellationException stop){throw stop;}catch(RuntimeException failure){slow(key);return null;}catch(LinkageError failure){slow(key);return null;}catch(OutOfMemoryError unavailable){return null;}
        finally{if(!publish&&model!=null)StrongNoise1958.discardResident1961(model);}
    }
    static void scheduleResident(int[] halfSource,final int width,final int height,final int noise,final boolean shadows,float[] evidence,float[] runtime,final StrongNoise1958.Model reference,final long originalCpu){
        final String key=residentKey(width,height,noise,shadows);
        if(rejected(key)||GpuQualification1961.restore(key)!=null||GpuQualification1961.background()||GpuNoise1960.sessionBusy()||!GpuNoise1960.available())return;
        long bytes=4L*halfSource.length+4L*(16+runtime.length)+reference.residentBytes();
        if(!GpuQualification1961.canQueue(key,bytes)||bytes>96L*1024*1024||!GpuNoise1960.workspaceFits(4L*halfSource.length+4L*(16+runtime.length)))return;
        try{
            GpuQualification1961.schedule(key,bytes,new ResidentProbe(key,halfSource.clone(),width,height,noise,shadows,Arrays.copyOf(evidence,16),runtime.clone(),reference,originalCpu));
        }catch(CancellationException stop){throw stop;}catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
    }
    private static final class ResidentProbe implements GpuQualification1961.Probe {
        final String key;final int width,height,noise;final boolean shadows;final long originalCpu;
        int[] h;float[] ev,rt;StrongNoise1958.Model reference;
        ResidentProbe(String key,int[] h,int width,int height,int noise,boolean shadows,float[] ev,float[] rt,StrongNoise1958.Model reference,long cpu){this.key=key;this.h=h;this.width=width;this.height=height;this.noise=noise;this.shadows=shadows;this.ev=ev;this.rt=rt;this.reference=reference;originalCpu=cpu;}
        public void run(GpuQualification1961.Cancellation c){
            try {
            if(c.cancelled())return;if(!GpuNoise1960.supports(GpuNoise1960.ANALYSIS)||!GpuNoise1960.supports(GpuNoise1960.strongProgram(0,0))||!GpuNoise1960.supports(GpuNoise1960.strongProgram(1,0))||!GpuNoise1960.supports(GpuNoise1960.strongProgram(2,0))){if(!c.cancelled()&&!GpuNoise1960.sessionBusy())slow(key);return;}
            long minCpu=Long.MAX_VALUE,maxGpu=0;
            for(int pair=0;pair<2;pair++){
                if(c.cancelled())return;
                long cpuStart=System.nanoTime();StrongNoise1958.Model oracle=StrongNoise1958.preparedSnapshotCpu1961(h,width,height,noise,shadows,ev,rt);long cpu=Math.min(originalCpu,System.nanoTime()-cpuStart);
                long start=System.nanoTime();StrongNoise1958.Model candidate=null;long gpu;
                try{candidate=residentCandidate1961(h,width,height,noise,shadows,ev,rt,0);}
                finally{if(candidate!=null)StrongNoise1958.discardResident1961(candidate);gpu=System.nanoTime()-start;}
                if(c.cancelled())return;
                if(candidate==null){slow(key);return;}
                if(!StrongNoise1958.sameModel1961(reference,oracle)||!StrongNoise1958.sameModel1961(oracle,candidate)){reject(key);return;}if(!win(cpu,gpu)){slow(key);return;}minCpu=Math.min(minCpu,cpu);maxGpu=Math.max(maxGpu,gpu);
            }
            if(!c.cancelled()){if(win(minCpu,maxGpu))GpuQualification1961.qualified(key,minCpu,maxGpu,0);else slow(key);}
            }catch(Deferred unavailable){}catch(CancellationException stop){throw stop;}
            catch(RuntimeException failure){if(!c.cancelled())slow(key);}
            catch(LinkageError failure){if(!c.cancelled())slow(key);}
            catch(OutOfMemoryError unavailable){}
        }
        public void close(){h=null;ev=null;rt=null;reference=null;}
    }
    /** GX20: one upload, two exact average2 stages, unchanged Haar and D8 bins,
     * three original preparation modes, one fence. The private maps are read
     * only for the CPU fallback oracle; their SSBOs remain resident for Strong. */
    static StrongNoise1958.Model residentCandidate1961(int[] halfSource,int width,int height,int noise,boolean shadows,float[] sourceEvidence,float[] runtime,int variant){
        long start=System.nanoTime();int hw=ceil2(width),hh=ceil2(height),qw=ceil2(hw),qh=ceil2(hh),ew=ceil2(qw),eh=ceil2(qh);
        int hn=hw*hh,qn=qw*qh,en=ew*eh;long maps=(long)hn+qn+en;
        long bytes=8L*maps+4L*(64+runtime.length)+65536;
        if(halfSource.length!=hn||!GpuNoise1960.workspaceFits(bytes+4L*maps+4L*(64+runtime.length))||GpuNoise1960.sessionBusy())throw new Deferred();
        int analysis=GpuNoise1960.variant(GpuNoise1960.ANALYSIS,variant);
        if(!GpuNoise1960.supports(analysis))return null;
        for(int mode=0;mode<3;mode++)if(!GpuNoise1960.supports(GpuNoise1960.strongProgram(mode,variant)))return null;
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)throw new Deferred();boolean transferred=false;
        try{
            float[] initial=new float[64];System.arraycopy(sourceEvidence,0,initial,0,16);int[] emptyHistogram=new int[2064];float[] f=new float[32];
            GpuNoise1960.Batch b=new GpuNoise1960.Batch().upload(0,halfSource).allocate(14,4L*qn).allocate(15,4L*en).allocate(4,4L*hn).allocate(5,4L*qn).allocate(6,4L*en).upload(17,initial).upload(18,GpuPolicy1960.strongBasis8()).allocate(19,4);
            int[] pyramid=GpuPolicy1960.pyramidUniforms(hw,hh);b.dispatch(analysis,new int[]{0,14,17,19,18,19},pyramid,f,qn);
            pyramid=GpuPolicy1960.pyramidUniforms(qw,qh);b.dispatch(analysis,new int[]{14,15,17,19,18,19},pyramid,f,en);
            int[] ws={hw,qw,ew},hs={hh,qh,eh},slots={0,14,15},counts={hn,qn,en};
            for(int level=0;level<3;level++){
                int w=ws[level],h=hs[level],offset=16+level*16;int[] bindings={slots[level],19,17,16,18,19};
                b.upload(16,emptyHistogram);
                if(w>1&&h>1){int step=Math.max(1,(int)Math.sqrt((long)w*h/24000.0));int[] lag=GpuPolicy1960.lagUniforms(w,h,step,1,0,0,w,h,0);b.dispatch(analysis,bindings,lag,f,GpuPolicy1960.lagInvocations(lag));}
                int[] median=new int[32];median[0]=3;median[10]=offset;b.dispatch(analysis,bindings,median,f,16);
                if(w>=8&&h>=8){b.upload(16,emptyHistogram);int[] spectral=GpuPolicy1960.spectralUniforms(w,h,0);b.dispatch(analysis,bindings,spectral,f,spectral[3]*spectral[4]);median[11]=1;b.dispatch(analysis,bindings,median,f,16);}
                int[] u=new int[32];u[0]=w;u[1]=h;u[3]=h;u[5]=h;u[7]=h;u[8]=noise;u[9]=shadows?1:0;u[10]=level;u[20]=offset;
                b.dispatch(GpuNoise1960.strongProgram(level,variant),new int[]{slots[level],4+level,17,19,4,5,6,19},u,f,counts[level]);
            }
            int[][] result=session.execute(b,new int[]{17,4,5,6},new int[]{64,hn,qn,en});cancelled();if(result==null)return null;
            StrongNoise1958.Model model=StrongNoise1958.model1961(width,height,result[1],result[2],result[3],floats(result[0]),runtime);
            if(!session.run(new GpuNoise1960.Batch().upload(2,runtime)))return null;cancelled();
            StrongNoise1958.attachResident1961(model,new Resident(session,System.nanoTime()-start));transferred=true;return model;
        }finally{if(!transferred)session.close();}
    }
}

