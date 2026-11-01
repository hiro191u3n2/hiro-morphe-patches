package com.hiro.ulike;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** GX12/GX14 exact integer protection and final finishing-policy preparation.
 * Face anchors, all double-precision ellipses/affine sampling and unknown custom
 * masks retain their original CPU authority. No image or mask is held statically.
 * Two complete CPU/GPU matches and a transfer-inclusive 5% win precede reuse. */
public final class GpuProtection1961 {
    private GpuProtection1961() {}
    private static final int SHADER=6;
    private static final State[] STATES=new State[32];
    private static int victim;
    private static final class State {
        final String key,environment;int pairs;long cpu,gpu;
        boolean busy,admitted;
        State(String key,String environment){this.key=key;this.environment=environment;}
    }
    private interface Oracle {int[] run();}
    private static final class DeferredGpu extends RuntimeException {}
    static boolean available() {
        return !Thread.currentThread().isInterrupted() && !GpuNoise1960.sessionBusy()
            && GpuNoise1960.supports(SHADER);
    }
    private static void interrupted(){if(Thread.currentThread().isInterrupted())throw new IllegalStateException("protection interrupted");}
    private static boolean mayTry(String key) {
        String environment=GpuNoise1960.fingerprint();
        synchronized(STATES){for(State state:STATES)if(state!=null&&state.key.equals(key)&&state.environment.equals(environment))return !state.busy&&GpuQualification1961.maySchedule(proofKey(key));}
        return GpuQualification1961.maySchedule(proofKey(key));
    }
    private static String proofKey(String key){return "protection1962:"+key;}
    static boolean mayTryFace(int width,int height,int faces) {
        return faces>0&&GpuNoise1960.workspaceFits((long)width*height*(faces*8L+32L)+32768)
            &&mayTry("face:"+width+":"+height+":"+faces);
    }
    private static State acquire(String key) {
        String environment=GpuNoise1960.fingerprint();if(environment==null||environment.isEmpty())return null;
        synchronized(STATES) {
            State found=null;for(State state:STATES)if(state!=null&&state.key.equals(key)&&state.environment.equals(environment)){found=state;break;}
            if(found==null){found=new State(key,environment);STATES[victim++%STATES.length]=found;}
            if(found.busy||GpuQualification1961.exactRejected(proofKey(key)))return null;found.busy=true;return found;
        }
    }
    private static int[] evaluate(String key,int[][] ints,float[] grid,int[] u,float[] f,int outputCount,Oracle oracle) {
        if(!available()||outputCount<1)return null;
        long bytes=4L*outputCount+32768;for(int[] a:ints)bytes+=4L*a.length;if(grid!=null)bytes+=4L*grid.length;
        if(bytes>GpuNoise1960.MAX_BYTES||!GpuNoise1960.workspaceFits(bytes+8L*outputCount))return null;
        final State state=acquire(key);if(state==null)return null;final String proofKey=proofKey(key);int[] reference=null;boolean gpuAttempt=false;
        try {
            GpuQualification1961.Record saved=GpuQualification1961.restore(proofKey);
            if(saved!=null){state.admitted=true;state.pairs=2;state.cpu=saved.cpuNanos*2;state.gpu=saved.gpuNanos*2;}
            if(state.admitted) {
                gpuAttempt=true;long start=System.nanoTime();int[] result=candidate(ints,grid,u,f,outputCount);long elapsed=System.nanoTime()-start;
                interrupted();if(result==null){state.admitted=false;GpuQualification1961.rejectSpeed(proofKey);return null;}
                long baseline=state.cpu/Math.max(1,state.pairs);
                if(elapsed>baseline-baseline/20){state.admitted=false;state.pairs=0;GpuQualification1961.rejectSpeed(proofKey);}
                return result;
            }
            long start=System.nanoTime();reference=oracle.run();long cpu=System.nanoTime()-start;interrupted();
            if(reference!=null&&reference.length==outputCount&&cpu>0&&GpuQualification1961.maySchedule(proofKey)&&
                    !GpuQualification1961.background()&&GpuQualification1961.retainedBytes()==0&&
                    GpuNoise1960.workspaceFits(bytes+12L*outputCount)) {
                // The Oracle may own a Plan or a mask lease. Only its complete
                // immutable result and copied descriptors cross into idle work.
                ProtectionProbe probe=new ProtectionProbe(proofKey,ints,grid,u,f,reference,cpu);
                GpuQualification1961.schedule(proofKey,probe.bytes(),probe);
            }
            return reference;
        } catch(DeferredGpu unavailable){interrupted();return reference;}
          catch(CancellationException cancelled){throw cancelled;}
          catch(RuntimeException optionalFailure){interrupted();state.admitted=false;if(gpuAttempt)GpuQualification1961.rejectSpeed(proofKey);return reference;}
          catch(LinkageError optionalFailure){state.admitted=false;if(gpuAttempt)GpuQualification1961.rejectSpeed(proofKey);return reference;}
          catch(OutOfMemoryError optionalFailure){state.admitted=false;return reference;}
        finally {synchronized(STATES){state.busy=false;}}
    }
    private static final class ProtectionProbe implements GpuQualification1961.Probe {
        final String key;final long cpu;int[][] ints;float[] grid,f;int[] u,reference;
        ProtectionProbe(String key,int[][] ints,float[] grid,int[] u,float[] f,int[] reference,long cpu) {
            this.key=key;this.cpu=cpu;this.ints=new int[ints.length][];
            for(int i=0;i<ints.length;i++)this.ints[i]=ints[i].clone();
            this.grid=grid==null?null:grid.clone();this.u=u.clone();this.f=f==null?null:f.clone();this.reference=reference.clone();
        }
        long bytes(){long bytes=4L*(reference.length+u.length)+32768;for(int[] a:ints)bytes+=4L*a.length;if(grid!=null)bytes+=4L*grid.length;if(f!=null)bytes+=4L*f.length;return bytes;}
        public void run(GpuQualification1961.Cancellation cancellation) {
            try {
            long worst=0;
            for(int trial=0;trial<2;trial++) {
                if(cancellation.cancelled())return;
                long start=System.nanoTime();int[] result=candidate(ints,grid,u,f,reference.length);long gpu=System.nanoTime()-start;
                if(cancellation.cancelled())return;
                if(result==null){GpuQualification1961.rejectSpeed(key);return;}
                if(!Arrays.equals(reference,result)){GpuQualification1961.rejectExact(key);return;}
                if(gpu<=0||gpu>cpu-cpu/20){GpuQualification1961.rejectSpeed(key);return;}worst=Math.max(worst,gpu);
            }
            if(!cancellation.cancelled())GpuQualification1961.qualified(key,cpu,worst,0);
            } catch(DeferredGpu unavailable) { }
              catch(CancellationException cancelled){throw cancelled;}
              catch(RuntimeException failure){if(!cancellation.cancelled())GpuQualification1961.rejectSpeed(key);}
              catch(LinkageError failure){if(!cancellation.cancelled())GpuQualification1961.rejectSpeed(key);}
              catch(OutOfMemoryError unavailable) { }
        }
        public void close(){ints=null;grid=null;f=null;u=null;reference=null;}
    }
    /** One submission uploads all immutable descriptors, dispatches, and reads
     * only this owned result. The engine drains once for the batch read. */
    private static int[] candidate(int[][] ints,float[] grid,int[] u,float[] f,int outputCount) {
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)throw new DeferredGpu();
        try {
            int invocations=u[0]==2?outputCount/4:u[0]==1?outputCount:outputCount/2;
            GpuNoise1960.Batch batch=new GpuNoise1960.Batch();
            batch.upload(0,ints[0]).allocate(1,4L*outputCount)
                .upload(2,ints[1]).upload(3,ints[2]).upload(4,grid==null?new float[]{0}:grid)
                .upload(5,ints[3]).dispatch(SHADER,new int[]{0,1,2,3,4,5},u,f,invocations);
            int[][] read=session.execute(batch,new int[]{1},new int[]{outputCount});
            return read==null?null:read[0];
        } finally {session.close();}
    }
    /** Exact CPU-double geometric eligibility/protection, one packed plane per
     * accepted face. Bit16 records visited pixels, including geometric zeros. */
    static int[] faceRaster(final int[] source,final int width,final int height,final int[][] faces) {
        if(source==null||faces==null||faces.length<1||faces.length>8||source.length!=(long)width*height)return null;
        final int count=source.length;long elements=(long)count*faces.length;
        if(elements>Integer.MAX_VALUE)return null;
        int[] geometry=new int[(int)elements];
        for(int i=0;i<faces.length;i++) {
            if(faces[i]==null||faces[i].length!=count)return null;
            System.arraycopy(faces[i],0,geometry,i*count,count);
        }
        int[] u=new int[32];u[1]=width;u[2]=height;u[3]=faces.length;u[4]=count;
        return evaluate("face:"+width+":"+height+":"+faces.length,new int[][]{source,new int[]{0},geometry,new int[]{0}},null,u,null,count*2,
            new Oracle(){public int[] run(){return faceReference(source,width,height,faces);}});
    }
    static int[] faceReference(int[] source,int width,int height,int[][] faces) {
        int[] result=new int[source.length*2];
        for(int[] face:faces)for(int i=0;i<source.length;i++) {
            if((i&4095)==0)interrupted();int geometry=face[i];if((geometry&65536)==0)continue;
            int eligibility=geometry&255,protect=(geometry>>>8)&255;
            int p=source[i],r=(p>>>16)&255,g=(p>>>8)&255,b=p&255;
            int l=(77*r+150*g+29*b+128)>>8;
            boolean skin=l>=28&&r>=g-10&&r>b+2&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))>6;
            int lo=255,hi=0,x=i%width,y=i/width;
            for(int yy=Math.max(0,y-1);yy<=Math.min(height-1,y+1);yy++)for(int xx=Math.max(0,x-1);xx<=Math.min(width-1,x+1);xx++) {
                int c=source[yy*width+xx],v=(77*((c>>>16)&255)+150*((c>>>8)&255)+29*(c&255)+128)>>8;
                lo=Math.min(lo,v);hi=Math.max(hi,v);
            }
            eligibility=skin?eligibility*Math.max(0,48-(hi-lo))/48:0;
            eligibility=eligibility*(255-protect)/255;
            result[i*2]=Math.max(result[i*2],eligibility);result[i*2+1]=Math.max(result[i*2+1],protect);
        }
        return result;
    }
    /** NR13 final 4x4 protection minimum; a single nonopaque or protected pixel
     * remains a veto. Sampling the finished byte raster still uses CPU doubles. */
    public static int[] smoothCells(final int[] input,final int width,final int stripRows,final int begin,final int end,
            final int origin,final int fullHeight,final int[] policy,final int[] sharedConfidence) {
        if(input==null||policy==null||sharedConfidence==null||width<1||stripRows<1||begin<0||end<=begin||end>stripRows||origin<0||
            ((begin+origin)&3)!=0||((end-begin)%4!=0&&end+origin!=fullHeight)||input.length<(long)width*stripRows)return null;
        final int columns=(width+3)/4,cells=columns*((end-begin+3)/4);
        if(policy.length<(long)width*(end-begin)*2||sharedConfidence.length<cells)return null;
        int[] u=new int[32];u[0]=1;u[1]=width;u[2]=stripRows;u[3]=begin;u[4]=end;u[5]=origin;u[6]=fullHeight;u[7]=columns;u[8]=cells;
        return evaluate("smooth:"+width+":"+stripRows+":"+begin+":"+end+":"+(fullHeight-end-origin),
            new int[][]{input,policy,sharedConfidence,new int[]{0}},null,u,null,cells,
            new Oracle(){public int[] run(){return smoothReference(input,width,stripRows,begin,end,origin,fullHeight,policy,sharedConfidence);}});
    }
    static int[] smoothReference(int[] input,int width,int stripRows,int begin,int end,int origin,int fullHeight,int[] policy,int[] sharedConfidence) {
        int columns=(width+3)/4;int[] output=new int[columns*((end-begin+3)/4)];
        for(int row=begin;row<end;row+=4) {
            interrupted();int y=row+origin;
            for(int x=0;x<width;x+=4) {
                int minimum=256;
                for(int dy=0;dy<4&&y+dy<fullHeight;dy++)for(int dx=0;dx<4&&x+dx<width;dx++) {
                    int r=row+dy,c=x+dx;
                    if(r>=stripRows||(input[r*width+c]>>>24)!=255){minimum=0;continue;}
                    int at=((r-begin)*width+c)*2;int budget=policy[at]*(256-policy[at+1])>>8;
                    minimum=Math.min(minimum,budget);
                }
                int cell=((row-begin)/4)*columns+x/4;
                output[cell]=Math.min(255,sharedConfidence[cell]*minimum>>8);
            }
        }
        return output;
    }
    private static Object field(Object source,String name) {
        try{Field field=source.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(source);}
        catch(ReflectiveOperationException unavailable){throw new IllegalArgumentException("unsupported protection descriptor",unavailable);}
    }
    private static boolean known(Object mask,String name) {
        return mask==null || mask.getClass().getClassLoader()==GpuProtection1961.class.getClassLoader() && mask.getClass().getName().equals(name);
    }
    static FinishPolicy1953.Band finishBand(final QualityPixels1932.Plan plan,final int width,final int rows,
            final int first,final int last,final int originY) {
        if(plan==null||width<1||rows<1||first<0||last<=first||last>rows||first+(long)originY<0||
            (long)width*(last-first)>Integer.MAX_VALUE/4||!available())return null;
        // Arbitrary custom masks may be stateful; never evaluate/collapse their calls.
        if(!known(plan.faceRegions,"com.hiro.ulike.FaceRegions1934$Mask"))return null;
        if(plan.smoothedRegions!=null) {
            String name=plan.smoothedRegions.getClass().getName();
            if(plan.smoothedRegions.getClass().getClassLoader()!=GpuProtection1961.class.getClassLoader()||
                !(name.equals("com.hiro.ulike.QualityPipeline1932$SmoothRegions1958")||name.equals("com.hiro.ulike.QualityPipeline1932$SmoothRegions1958$1")))return null;
        }
        if(!Float.isFinite(plan.sourceSigma)||plan.sourceSigma<0||!Float.isFinite(plan.outputScale)||plan.outputScale<=0)return null;
        boolean variableNoise=plan.localNoise!=null&&(plan.noiseMapAtOutput||Math.abs(plan.outputScale-1f)<.00001f);
        boolean variableMask=plan.texturePriority&&plan.faceRegions!=null;
        if(!variableNoise&&!variableMask&&plan.smoothedRegions==null)return null; // Existing constant shortcut is faster.
        final int pixels=width*(last-first);
        if(!GpuNoise1960.workspaceFits(64L*pixels+32768))return null;
        int[] masks=new int[pixels*2],u=new int[32];float[] f=new float[32];
        u[0]=2;u[1]=width;u[2]=last-first;u[3]=first+originY;u[7]=plan.sharpFloorQ8;
        u[8]=Math.max(2,Math.round(Math.min(32f,plan.sourceSigma)*.85f));
        u[9]=Math.round((2f+Math.min(32f,plan.sourceSigma)*2.4f)*256f);u[10]=20+(plan.beautyQ8>>3);
        f[0]=plan.sourceSigma;f[1]=!plan.noiseMapAtOutput&&plan.outputScale<1f?(float)Math.sqrt(plan.outputScale):1f;
        // u6 is known before exporting any expensive double-sampled mask.
        u[6]=variableNoise?1:0;
        String key="finish:"+width+":"+(last-first)+":"+u[6]+":"+variableMask+":"+(plan.smoothedRegions!=null)+":"+u[7]+":"+Float.floatToRawIntBits(f[1]);
        if(!mayTry(key))return null;
        try {
            for(int y=first;y<last;y++) {interrupted();for(int x=0;x<width;x++) {
                int at=((y-first)*width+x)*2;
                masks[at]=variableMask?plan.skinAt(x,y+originY):0;
                masks[at+1]=plan.smoothedRegions==null?0:Math.max(0,Math.min(256,plan.smoothedRegions.smoothingQ8(x,y+originY)));
            }}
            float[] grid=new float[]{0};
            if(variableNoise) {
                SpatialNoise1934 noise=plan.localNoise;
                if(noise.width!=width||first+(long)originY<0||last+(long)originY>noise.height)return null;
                grid=((float[])field(noise,"sigma")).clone();for(float value:grid)if(!Float.isFinite(value)||value<0)return null;
                u[4]=noise.columns;u[5]=noise.rows;u[6]=1;
                f[2]=(Float)field(noise,"left");f[3]=(Float)field(noise,"top");f[4]=(Float)field(noise,"stepX");f[5]=(Float)field(noise,"stepY");
            }
            int[] raw=evaluate(key,
                new int[][]{new int[]{0},new int[]{0},new int[]{0},masks},grid,u,f,pixels*4,
                new Oracle(){public int[] run(){
                    FinishPolicy1953.Band band=FinishPolicy1953.prepareCpu1961(plan,width,rows,first,last,originY);
                    if(band==null)return null;try {
                        int[] out=new int[pixels*4];for(int i=0;i<pixels;i++) {
                            if(band.mode==FinishPolicy1953.CONSTANT4)System.arraycopy(band.words,0,out,i*4,4);
                            else if(band.mode==FinishPolicy1953.RAW4)System.arraycopy(band.words,i*4,out,i*4,4);
                            else {out[i*4]=band.words[i*2]&65535;out[i*4+1]=(band.words[i*2]>>>16)&65535;
                                out[i*4+2]=band.words[i*2+1]&65535;out[i*4+3]=(band.words[i*2+1]>>>16)&65535;}
                        }return out;
                    } finally {band.close();}
                }});
            return raw==null?null:FinishPolicy1953.fromRaw1961(raw,pixels);
        } catch(RuntimeException optionalFailure){interrupted();return null;}
          catch(OutOfMemoryError optionalFailure){return null;}
    }
    /** Test only calls the production candidate, bypassing speed qualification. */
    static int[] testCandidate(int[][] ints,float[] grid,int[] u,float[] f,int outputCount){return candidate(ints,grid,u,f,outputCount);}
}
