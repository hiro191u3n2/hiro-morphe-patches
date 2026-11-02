package com.hiro.ulike;

import java.lang.reflect.Field;
import java.util.concurrent.CancellationException;

/** Exact CPU-to-GPU policy/axis descriptors. The existing saved-image detector
 * remains the authority for skin/detail: its double precision face geometry is
 * never replaced by a float-only segmentation surrogate. GPU protection amounts
 * and local-noise interpolation use these exact shot inputs. No bitmap or frame
 * is stored statically and descriptors are caller-owned until session close. */
public final class GpuPolicy1960 {
    private GpuPolicy1960() {}
    private static final class DeferredGpu extends RuntimeException {}
    private static final java.util.LinkedHashMap<String,Boolean> REJECTED=new java.util.LinkedHashMap<String,Boolean>(32,.75f,true);
    private static boolean blocked(String key){synchronized(REJECTED){return REJECTED.containsKey(key);}}
    private static void reject(String key){synchronized(REJECTED){REJECTED.put(key,Boolean.TRUE);while(REJECTED.size()>32)REJECTED.remove(REJECTED.keySet().iterator().next());}GpuQualification1961.reject(key);}
    private static boolean win(long cpu,long gpu){return cpu>0&&gpu>0&&gpu<=cpu-cpu/20;}
    /** Same Worker policy as1959, with an explicit immutable descriptor source. */
    public static final class Protection implements SingleNoise1955.Protection,StrongNoise1958.Protection {
        public final QualityPixels1932.Plan plan;
        public final boolean strong;
        public Protection(QualityPixels1932.Plan plan,boolean strong){this.plan=plan;this.strong=strong;}
        public int budgetQ8(int x,int y) {
            if(plan==null)return 256;
            int budget=256-((plan.skinAt(x,y)*plan.beautyQ8)>>9);
            return strong?budget:plan.shadowBudgetQ8*budget>>8;
        }
        public int detailQ8(int x,int y){return plan==null?0:plan.detailAt(x,y);}
        public PolicyData data(int width,int rows,int firstY){return sourcePlan(plan,width,rows,firstY,strong);}
    }

    /** Dispatch into a caller's resident policy slot; no photograph is changed. */
    static boolean uploadPolicy(GpuNoise1960.Session session,PolicyData data,
            int policySlot,int maskSlot,int gridSlot,int sigmaSlot,int dummySlot) {
        if(session==null||data==null)return false;
        return session.upload(maskSlot,data.masks)&&session.upload(gridSlot,data.grid)
            &&session.allocate(policySlot,8L*data.count)&&session.allocate(sigmaSlot,4L*data.count)
            &&session.allocate(dummySlot,4)&&session.dispatch(GpuNoise1960.ANALYSIS,
                new int[]{dummySlot,dummySlot,sigmaSlot,policySlot,gridSlot,maskSlot},data.u,data.f,data.count);
    }

    /** Exact average2 candidate for an already materialized pyramid level. No
     * bitmap source copy or external frame is introduced. Two private integer
     * oracle matches and a5% transfer-inclusive win are required before reuse.
     * The integer expression is independent of float precision and driver modes.
     * Returns null when unavailable, leaving StrongNoise's original loop active. */
    static int[] gpuPyramid(int[] source,final int width,final int height) {
        if(source==null||width<1||height<1||source.length!=(long)width*height)return null;
        String key="pyramid1961:bg:v2:"+width+":"+height;
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof!=null&&!blocked(key)&&!GpuNoise1960.sessionBusy())try{
            long start=System.nanoTime();int[] result=pyramidCandidate(source,width,height);long gpu=System.nanoTime()-start;interrupted();
            if(result==null){reject(key);return null;}if(!win(proof.cpuNanos,gpu))reject(key);return result;
        }catch(DeferredGpu unavailable){}catch(CancellationException stop){throw stop;}catch(RuntimeException failure){reject(key);}catch(LinkageError failure){reject(key);}catch(OutOfMemoryError unavailable){}
        long start=System.nanoTime();int[] reference=pyramidReference(source,width,height);long cpu=System.nanoTime()-start;
        if(!blocked(key)&&!GpuQualification1961.background()&&GpuQualification1961.retainedBytes()==0&&!GpuNoise1960.sessionBusy()&&GpuNoise1960.available())schedulePolicy(key,source,width,height,reference,null,cpu);
        return reference;
    }
    private static int[] pyramidCandidate(int[] source,int width,int height) {
        int[] u=pyramidUniforms(width,height);int count=u[3]*u[4];
        long nativeBytes=4L*source.length+4L*count+32768;
        if(nativeBytes>GpuNoise1960.MAX_BYTES||!GpuNoise1960.workspaceFits(nativeBytes+4L*count))throw new DeferredGpu();
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)throw new DeferredGpu();
        try {
            if(!session.upload(0,source)||!session.allocate(1,4L*count)||!session.allocate(2,4))return null;
            if(!session.dispatch(GpuNoise1960.ANALYSIS,new int[]{0,1,2,2,2,2},u,new float[32],count))return null;
            return session.readInts(1,count);
        } finally {session.close();}
    }
    private static int[] pyramidReference(int[] source,int width,int height) {
        int ow=width/2+(width&1),oh=height/2+(height&1);int[] out=new int[ow*oh];
        for(int y=0;y<oh;y++) {
            interrupted();
            for(int x=0;x<ow;x++) {
                int r=0,g=0,b=0,n=0;boolean opaque=true;
                for(int yy=y*2;yy<Math.min(height,y*2+2);yy++)for(int xx=x*2;xx<Math.min(width,x*2+2);xx++) {
                    int p=source[yy*width+xx];opaque&=(p>>>24)==255;r+=(p>>>16)&255;g+=(p>>>8)&255;b+=p&255;n++;
                }
                out[y*ow+x]=(opaque?0xff000000:0)|((r+n/2)/n<<16)|((g+n/2)/n<<8)|(b+n/2)/n;
            }
        }
        return out;
    }

    public interface EvidenceCpu {float[] compute();}

    /** GX4 sampled Haar/D8 evidence from the exact current materialized pyramid
     * image. CPU supplies the unchanged independent oracle during qualification;
     * current histogram bins, sample locations and sequential D8 products stay
     * identical. Integer atomics accumulate only histogram counts, never floats. */
    static float[] strongEvidence(int[] source,int width,int height,EvidenceCpu cpu) {
        if(source==null||cpu==null||width<2||height<2||source.length!=(long)width*height)return null;
        String key="evidence1961:bg:v2:"+width+":"+height;
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof!=null&&!blocked(key)&&!GpuNoise1960.sessionBusy())try{
            long start=System.nanoTime();float[] result=evidenceCandidate(source,width,height);long gpu=System.nanoTime()-start;interrupted();
            if(result==null){reject(key);return null;}if(!win(proof.cpuNanos,gpu))reject(key);return result;
        }catch(DeferredGpu unavailable){}catch(CancellationException stop){throw stop;}catch(RuntimeException failure){reject(key);}catch(LinkageError failure){reject(key);}catch(OutOfMemoryError unavailable){}
        long start=System.nanoTime();float[] reference=cpu.compute();long elapsed=System.nanoTime()-start;
        if(!blocked(key)&&!GpuQualification1961.background()&&GpuQualification1961.retainedBytes()==0&&!GpuNoise1960.sessionBusy()&&GpuNoise1960.available())schedulePolicy(key,source,width,height,null,reference,elapsed);
        return reference;
    }
    private static void schedulePolicy(String key,int[] source,int width,int height,int[] pyramid,float[] evidence,long cpu) {
        long bytes=4L*source.length+(pyramid==null?0:4L*pyramid.length)+(evidence==null?0:4L*evidence.length)+256;
        if(bytes>96L*1024*1024||!GpuNoise1960.workspaceFits(bytes))return;
        try{GpuQualification1961.schedule(key,bytes,new PolicyProbe(key,source.clone(),width,height,pyramid,evidence,cpu));}
        catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
    }
    private static final class PolicyProbe implements GpuQualification1961.Probe {
        final String key;final int width,height;final long cpu;int[] source,expectedPyramid;float[] expectedEvidence;
        PolicyProbe(String k,int[] s,int w,int h,int[] p,float[] e,long n){key=k;source=s;width=w;height=h;expectedPyramid=p;expectedEvidence=e;cpu=n;}
        public void run(GpuQualification1961.Cancellation c){
            if(c.cancelled())return;if(!GpuNoise1960.supports(GpuNoise1960.ANALYSIS)){if(!c.cancelled()&&!GpuNoise1960.sessionBusy())reject(key);return;}long sum=0;
            for(int pair=0;pair<2;pair++){
                if(c.cancelled())return;boolean exact;long start,gpu;
                if(expectedPyramid!=null){int[] oracle=pyramidReference(source,width,height);start=System.nanoTime();int[] candidate=pyramidCandidate(source,width,height);gpu=System.nanoTime()-start;exact=java.util.Arrays.equals(expectedPyramid,oracle)&&java.util.Arrays.equals(oracle,candidate);}
                else{float[] oracle=StrongNoise1958.evidenceSnapshotCpu1961(source,width,height);start=System.nanoTime();float[] candidate=evidenceCandidate(source,width,height);gpu=System.nanoTime()-start;exact=evidenceEqual(expectedEvidence,oracle)&&evidenceEqual(oracle,candidate);}
                if(c.cancelled())return;if(!exact||!win(cpu,gpu)){reject(key);return;}sum+=gpu;
            }
            if(!c.cancelled())GpuQualification1961.qualified(key,cpu,sum/2,0);
        }
        public void close(){source=null;expectedPyramid=null;expectedEvidence=null;}
    }
    private static boolean evidenceEqual(float[] a,float[] b) {
        if(a==null||b==null||a.length!=16||b.length!=16)return false;
        for(int i=0;i<16;i++)if(Float.floatToRawIntBits(a[i])!=Float.floatToRawIntBits(b[i]))return false;return true;
    }
    private static float[] evidenceCandidate(int[] source,int width,int height) {
        long bytes=4L*source.length+65536L;
        if(bytes>GpuNoise1960.MAX_BYTES||!GpuNoise1960.workspaceFits(bytes+8192))throw new DeferredGpu();
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)throw new DeferredGpu();
        try {
            int[] histogram=new int[16*128+16];float[] zeros=new float[16];
            if(!session.upload(0,source)||!session.upload(1,histogram)||!session.upload(2,zeros)||
                !session.upload(3,strongBasis8())||!session.allocate(4,4))return null;
            int[] bindings={0,4,2,1,3,4};float[] f=new float[32];
            int step=Math.max(1,(int)Math.sqrt((long)width*height/24000.0));
            int[] lag=lagUniforms(width,height,step,1,0,0,width,height,0);
            if(!session.dispatch(GpuNoise1960.ANALYSIS,bindings,lag,f,lagInvocations(lag)))return null;
            int[] median=new int[32];median[0]=3;
            if(!session.dispatch(GpuNoise1960.ANALYSIS,bindings,median,f,16))return null;
            if(width>=8&&height>=8) {
                if(!session.upload(1,histogram))return null;
                int[] spectral=spectralUniforms(width,height,0);
                if(!session.dispatch(GpuNoise1960.ANALYSIS,bindings,spectral,f,spectral[3]*spectral[4]))return null;
                median[11]=1;
                if(!session.dispatch(GpuNoise1960.ANALYSIS,bindings,median,f,16))return null;
            }
            int[] bits=session.readInts(2,16);if(bits==null||Thread.currentThread().isInterrupted())return null;
            float[] result=new float[16];for(int i=0;i<16;i++)result[i]=Float.intBitsToFloat(bits[i]);return result;
        } finally {session.close();}
    }

    public static final class PolicyData {
        public final int[] masks,u;
        public final float[] grid,f;
        public final int count;
        private PolicyData(int[] masks,int[] u,float[] grid,float[] f,int count) {
            this.masks=masks;this.u=u;this.grid=grid;this.f=f;this.count=count;
        }
    }

    /** masks bind5, grid bind4, output protection pair bind3, output sigma bind2.
     * firstY is absolute, not the strip-local first row. Shader writes budget and
     * detail interleaved, in exactly the format consumed by existing NR backends.
     * Unsupported private layouts reject rather than substitute evidence. */
    public static PolicyData sourcePlan(QualityPixels1932.Plan plan,int width,int rows,
            int firstY,boolean strong) {
        if(width<1||rows<1||firstY<0||(long)width*rows>Integer.MAX_VALUE/2)
            throw new IllegalArgumentException("GPU policy geometry");
        int count=width*rows;int[] masks=new int[count*2],u=new int[32];float[] f=new float[32];
        u[0]=4;u[1]=width;u[2]=rows;u[3]=firstY;u[6]=plan==null?256:plan.shadowBudgetQ8;
        u[7]=plan==null?0:plan.beautyQ8;u[8]=strong?1:0;f[0]=plan==null?0:plan.sourceSigma;
        for(int y=0;y<rows;y++) {
            interrupted();
            for(int x=0;x<width;x++) {
                int at=(y*width+x)*2;
                masks[at]=plan==null?0:plan.skinAt(x,y+firstY);
                masks[at+1]=plan==null?0:plan.detailAt(x,y+firstY);
            }
        }
        float[] grid=new float[]{0};
        if(plan!=null&&plan.localNoise!=null) {
            SpatialNoise1934 noise=plan.localNoise;
            if(noise.width!=width||firstY+(long)rows>noise.height)
                throw new IllegalArgumentException("GPU policy noise coordinates");
            // Reflection reads the pinned private immutable implementation. A
            // missing/changed field is an unavailable optional GPU route.
            grid=((float[])field(noise,"sigma")).clone();
            u[9]=noise.columns;u[10]=noise.rows;u[11]=1;
            f[1]=(Float)field(noise,"left");f[2]=(Float)field(noise,"top");
            f[3]=(Float)field(noise,"stepX");f[4]=(Float)field(noise,"stepY");
        }
        return new PolicyData(masks,u,grid,f,count);
    }

    /** Always compare a candidate before using its mask for a photograph. */
    public static boolean policyMatches(PolicyData data,int[] protection,float[] sigma,
            QualityPixels1932.Plan plan) {
        if(data==null||protection==null||sigma==null||protection.length<data.count*2||sigma.length<data.count)return false;
        for(int i=0;i<data.count;i++) {
            int expected=256-((data.masks[i*2]*data.u[7])>>9);
            if(data.u[8]==0)expected=data.u[6]*expected>>8;
            expected=Math.max(0,Math.min(256,expected));
            if(protection[i*2]!=expected||protection[i*2+1]!=Math.max(0,Math.min(256,data.masks[i*2+1])))return false;
            int x=i%data.u[1],y=i/data.u[1]+data.u[3];
            float wanted=plan==null?0:plan.localSigmaAt(x,y);
            if(Float.floatToRawIntBits(sigma[i])!=Float.floatToRawIntBits(wanted))return false;
        }
        return true;
    }

    public static final class GeometryData {
        public final int[] u,tables;
        public final float[] weights;
        public final boolean exactCrop;
        public final int horizontalInvocations,outputInvocations;
        private GeometryData(int[] u,int[] tables,float[] weights,boolean exactCrop) {
            this.u=u;this.tables=tables;this.weights=weights;this.exactCrop=exactCrop;
            horizontalInvocations=u[4]*u[14];outputInvocations=u[4]*u[5];
        }
        public int[] uniforms(int mode) {int[] result=u.clone();result[0]=mode;return result;}
        public long workspaceBytes() {
            return 4L*(tables.length+weights.length)+(exactCrop?0:12L*horizontalInvocations);
        }
    }

    /** Exact axes are exported from FastPixels1933.prepare, rather than regenerated
     * in GLSL using a device-dependent sin/cos implementation. CPU preparation is
     * small O(width+height); the complete two-pass resampling executes on GPU. */
    public static GeometryData geometry(int sw,int sh,int rotation,int width,int height) {
        if(sw<1||sh<1||width<1||height<1||(long)sw*sh>Integer.MAX_VALUE||
                (long)width*height>Integer.MAX_VALUE||
                rotation!=0&&rotation!=90&&rotation!=180&&rotation!=270)
            throw new IllegalArgumentException("GPU resize geometry");
        boolean quarter=rotation==90||rotation==270;int rw=quarter?sh:sw,rh=quarter?sw:sh;
        if((long)width*rh>Integer.MAX_VALUE/3)throw new IllegalArgumentException("GPU resize intermediate");
        double scale=Math.max((double)width/rw,(double)height/rh);
        double cw=Math.min((double)rw,width/scale),ch=Math.min((double)rh,height/scale);
        double left=Math.max(0,(rw-cw)*.5),top=Math.max(0,(rh-ch)*.5);
        FastPixels1933.Plan plan=FastPixels1933.prepare(rw,rh,width,height,left,top,cw,ch);
        int[] u=new int[32];u[1]=sw;u[2]=sh;u[3]=rotation;u[4]=width;u[5]=height;
        u[6]=plan.cropLeft;u[7]=plan.cropTop;u[14]=rh;u[15]=0;u[16]=height;
        if(plan.exactCrop)return new GeometryData(u,new int[]{0},new float[]{0},true);
        Object h=plan.horizontal,v=plan.vertical;
        int[] ho=(int[])field(h,"offset"),hi=(int[])field(h,"indices");
        int[] vo=(int[])field(v,"offset"),vi=(int[])field(v,"indices");
        float[] hw=(float[])field(h,"weights"),vw=(float[])field(v,"weights");
        long tableCount=(long)ho.length+hi.length+vo.length+vi.length;
        if(tableCount>Integer.MAX_VALUE||hw.length+(long)vw.length>Integer.MAX_VALUE)
            throw new IllegalArgumentException("GPU resize axis size");
        int[] tables=new int[(int)tableCount];float[] weights=new float[hw.length+vw.length];
        u[8]=0;u[9]=ho.length;u[10]=ho.length+hi.length;u[11]=u[10]+vo.length;
        System.arraycopy(ho,0,tables,u[8],ho.length);System.arraycopy(hi,0,tables,u[9],hi.length);
        System.arraycopy(vo,0,tables,u[10],vo.length);System.arraycopy(vi,0,tables,u[11],vi.length);
        u[12]=0;u[13]=hw.length;System.arraycopy(hw,0,weights,0,hw.length);System.arraycopy(vw,0,weights,hw.length,vw.length);
        return new GeometryData(u,tables,weights,false);
    }

    public static int[] pyramidUniforms(int width,int height) {
        if(width<1||height<1)throw new IllegalArgumentException("GPU pyramid dimensions");
        int[] u=new int[32];u[0]=0;u[1]=width;u[2]=height;u[3]=width/2+(width&1);u[4]=height/2+(height&1);return u;
    }
    public static float[] strongBasis8() {
        float[][] basis=(float[][])field(StrongNoise1958.class,"D8");float[] values=new float[64];
        for(int i=0;i<8;i++)System.arraycopy(basis[i],0,values,i*8,8);return values;
    }
    public static int[] lagUniforms(int width,int height,int step,int lag,
            int left,int top,int patchWidth,int patchHeight,int histogramOffset) {
        if(width<1||height<1||step<1||lag<1||patchWidth<=lag||patchHeight<=lag||left<0||top<0||
                left+(long)patchWidth>width||top+(long)patchHeight>height||histogramOffset<0)
            throw new IllegalArgumentException("GPU noise histogram geometry");
        int[] u=new int[32];u[0]=1;u[1]=width;u[2]=height;u[3]=step;u[4]=lag;
        u[5]=left;u[6]=top;u[7]=patchWidth;u[8]=patchHeight;u[9]=histogramOffset;return u;
    }
    public static int lagInvocations(int[] u) {
        return ((u[7]-u[4]+u[3]-1)/u[3])*((u[8]-u[4]+u[3]-1)/u[3]);
    }
    public static int[] spectralUniforms(int width,int height,int histogramOffset) {
        int[] u=new int[32];u[0]=2;u[1]=width;u[2]=height;
        u[3]=Math.min(17,Math.max(1,width/16));u[4]=Math.min(17,Math.max(1,height/16));u[9]=histogramOffset;return u;
    }
    private static Object field(Object owner,String name) {
        try {
            Class<?> type=owner instanceof Class?(Class<?>)owner:owner.getClass();
            Field field=type.getDeclaredField(name);field.setAccessible(true);
            return field.get(owner instanceof Class?null:owner);
        } catch(ReflectiveOperationException unavailable) {
            throw new IllegalStateException("GPU exact descriptor unavailable: "+name,unavailable);
        }
    }
    private static void interrupted(){if(Thread.currentThread().isInterrupted())throw new CancellationException("GPU analysis cancelled before commit");}
}
