package com.hiro.ulike;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.IdentityHashMap;

/** Exact finishing-policy candidates. Only pinned immutable production masks
 * may share one smoothing sample or cross a CPU worker boundary. Stateful and
 * unknown callbacks retain the original row/column traversal and RAW4 replay. */
final class CpuFinishPolicy1978 {
    private CpuFinishPolicy1978() {}
    static boolean knownFace(QualityPixels1932.Plan plan) {
        return plan==null||plan.faceRegions==null||known(plan.faceRegions,"com.hiro.ulike.FaceRegions1934$Mask");
    }
    static boolean knownSmooth(QualityPixels1932.SmoothMask mask) {
        return mask==null||known(mask,"com.hiro.ulike.QualityPipeline1932$SmoothRegions1958")||
            known(mask,"com.hiro.ulike.QualityPipeline1932$SmoothRegions1958$1");
    }
    private static boolean known(Object object,String name) {
        return object!=null&&object.getClass().getClassLoader()==CpuFinishPolicy1978.class.getClassLoader()&&object.getClass().getName().equals(name);
    }
    static boolean immutable(QualityPixels1932.Plan plan){return plan!=null&&!plan.scopedPolicy1978()&&knownFace(plan)&&knownSmooth(plan.smoothedRegions);}
    private static boolean variable(QualityPixels1932.Plan p) {
        return p.localNoise!=null&&(p.noiseMapAtOutput||Math.abs(p.outputScale-1f)<.00001f)||
            p.texturePriority&&p.faceRegions!=null||p.smoothedRegions!=null;
    }
    static String key(QualityPixels1932.Plan p,int width,int rows,int first,int last,int origin,int workers,boolean nativePath) {
        return "cpu-finish-policy1978-exact2-time5-v1:"+nativePath+":"+width+":"+rows+":"+first+":"+last+":"+origin+":"+workers+":"+
            p.sharpFloorQ8+":"+p.beautyQ8+":"+Float.floatToRawIntBits(p.sourceSigma)+":"+Float.floatToRawIntBits(p.outputScale)+":"+
            p.noiseMapAtOutput+":"+p.texturePriority+":"+(p.localNoise!=null)+":"+(p.faceRegions!=null)+":"+
            (p.smoothedRegions==null?"none":p.smoothedRegions.getClass().getName());
    }
    static FinishPolicy1953.Band prepare(final QualityPixels1932.Plan plan,final int width,final int rows,
            final int first,final int last,final int origin) {
        if(!immutable(plan)||width<1||rows<1||first<0||last<=first||last>rows||!variable(plan))
            return FinishPolicy1953.prepareCpu1961(plan,width,rows,first,last,origin);
        long pixels=(long)width*(last-first);
        if(pixels>Integer.MAX_VALUE/4L||pixels*16>32L*1024*1024)
            return FinishPolicy1953.prepareCpu1961(plan,width,rows,first,last,origin);
        final int workers=Math.min(last-first,Math.min(SpeedWorkers1935.maxWorkers(),SpeedWorkers1935.availableWorkers1944()));
        final String key=key(plan,width,rows,first,last,origin,workers,false);
        if(CpuExact1978.enabled(key)) {
            try {
                if(GpuNoise1960.workspaceFits(pixels*24+65536))return prepareCandidate(plan,width,rows,first,last,origin,workers);
            }catch(OutOfMemoryError optional){SpeedWorkers1935.trim();}
             catch(LinkageError unavailable){}
        }
        FinishPolicy1953.Band result=FinishPolicy1953.prepareCpu1961(plan,width,rows,first,last,origin);
        if(result!=null) {
            long held=retainedPlan(plan);
            if(held<Long.MAX_VALUE-4096)CpuExact1978.offer(key,held+4096,80L*pixels+4L*1024*1024,new CpuExact1978.Factory(){
                public CpuExact1978.Work create(){return new BandProof(plan,width,rows,first,last,origin,workers);}
            });
        }
        return result;
    }
    static FinishPolicy1953.Band prepareCandidate(final QualityPixels1932.Plan plan,final int width,final int rows,
            final int first,final int last,final int origin,int requestedWorkers) {
        if(!immutable(plan))return FinishPolicy1953.prepareCpu1961(plan,width,rows,first,last,origin);
        if(width<1||rows<1||first<0||last<=first||last>rows||(long)width*(last-first)>Integer.MAX_VALUE/4L)return null;
        final int pixels=Math.multiplyExact(width,last-first),workers=Math.max(1,Math.min(last-first,requestedWorkers));
        int[] storage=SpeedWorkers1935.borrowInts(Math.multiplyExact(pixels,4));final int[] raw=storage;
        try {
            Runnable[] tasks=new Runnable[workers];
            for(int i=0;i<workers;i++) {
                final int from=first+(int)((long)(last-first)*i/workers),to=first+(int)((long)(last-first)*(i+1)/workers);
                tasks[i]=new Runnable(){public void run(){
                    for(int y=from;y<to;y++) {
                        interrupted();
                        for(int x=0;x<width;x++) {
                            if((x&4095)==0)interrupted();
                            NativeMoire1951.preparePolicy1978(plan,x,y+origin,raw,((y-first)*width+x)*4);
                        }
                    }
                }};
            }
            if(workers==1)tasks[0].run();else SpeedWorkers1935.run(tasks);
            interrupted();FinishPolicy1953.Band band=FinishPolicy1953.fromRaw1961(raw,pixels);
            if(band!=null&&band.words==raw)storage=null;
            return band;
        }finally{SpeedWorkers1935.release(storage);}
    }
    static boolean nativeEnabled(QualityPixels1932.Plan p,int width,int rows,int first,int last,int origin) {
        return immutable(p)&&p.smoothedRegions!=null&&CpuExact1978.enabled(key(p,width,rows,first,last,origin,1,true));
    }
    static void offerNative(final QualityPixels1932.Plan plan,final int[] source,final int width,final int rows,
            final int first,final int last,final int origin) {
        if(!immutable(plan)||plan.smoothedRegions==null||first>=last)return;
        final long count=(long)width*rows,core=(long)width*(last-first),held=retainedPlan(plan);
        if(count<1||count>Integer.MAX_VALUE||core>Integer.MAX_VALUE/4L||held>96L*1024*1024)return;
        final String key=key(plan,width,rows,first,last,origin,1,true);
        CpuExact1978.offer(key,held+4L*count+4096,64L*core+65536,new CpuExact1978.Factory(){
            public CpuExact1978.Work create(){return new NativeProof(plan,Arrays.copyOf(source,(int)count),width,rows,first,last,origin);}
        });
    }
    private static final class NativeProof implements CpuExact1978.Work {
        QualityPixels1932.Plan plan;int[] source;final int width,rows,first,last,origin;
        NativeProof(QualityPixels1932.Plan plan,int[] source,int width,int rows,int first,int last,int origin){this.plan=plan;this.source=source;this.width=width;this.rows=rows;this.first=first;this.last=last;this.origin=origin;}
        public Object run(boolean reused) {
            if(plan==null)return null;int[] out=new int[width*(last-first)*4];
            for(int y=first;y<last;y++) {
                interrupted();if(y==0||y==rows-1)continue;
                for(int x=1;x<width-1;x++)if(NativeMoire1951.candidate1978(source,y*width+x,width,plan.sharpFloorQ8)) {
                    int at=((y-first)*width+x)*4;
                    if(reused)NativeMoire1951.preparePolicy1978(plan,x,y+origin,out,at);
                    else NativeMoire1951.preparePolicy(plan,x,y+origin,out,at);
                }
            }
            return out;
        }
        public void close(){plan=null;source=null;}
    }
    private static final class BandProof implements CpuExact1978.Work {
        QualityPixels1932.Plan plan;final int width,rows,first,last,origin,workers;
        BandProof(QualityPixels1932.Plan plan,int width,int rows,int first,int last,int origin,int workers){this.plan=plan;this.width=width;this.rows=rows;this.first=first;this.last=last;this.origin=origin;this.workers=workers;}
        public Object run(boolean reused) {
            if(plan==null)return null;
            FinishPolicy1953.Band band=reused?prepareCandidate(plan,width,rows,first,last,origin,workers):FinishPolicy1953.prepareCpu1961(plan,width,rows,first,last,origin);
            if(band==null)return null;
            try {
                int[] out=new int[band.pixels*4];
                for(int i=0;i<band.pixels;i++)for(int lane=0;lane<4;lane++) {
                    int value=band.mode==FinishPolicy1953.CONSTANT4?band.words[lane]:
                        band.mode==FinishPolicy1953.RAW4?band.words[i*4+lane]:
                        band.words[i*2+lane/2]>>>(16*(lane&1))&65535;
                    out[i*4+lane]=value;
                }
                return new Object[]{out,new int[]{band.mode,band.rawFallback?1:0,band.pixels}};
            }finally{band.close();}
        }
        public void close(){plan=null;}
    }
    /** Only these final/private production owners are admitted. Their complete
     * primitive-array graph is charged while a proof retains the immutable shot
     * plan. No Bitmap, worker lease, arbitrary callback or unaccounted graph is
     * retained; a new capture cancels the proof through the common queue. */
    private static long retainedPlan(QualityPixels1932.Plan plan) {
        try{return retainedObject(plan,new IdentityHashMap<Object,Boolean>(),0);}
        catch(ReflectiveOperationException unavailable){return Long.MAX_VALUE;}
        catch(RuntimeException unavailable){return Long.MAX_VALUE;}
        catch(LinkageError unavailable){return Long.MAX_VALUE;}
    }
    private static long retainedObject(Object object,IdentityHashMap<Object,Boolean> seen,int depth) throws ReflectiveOperationException {
        if(object==null||seen.put(object,Boolean.TRUE)!=null)return 0;
        if(depth>8)return Long.MAX_VALUE;Class<?> type=object.getClass();
        if(type.isArray()) {
            Class<?> c=type.getComponentType();if(!c.isPrimitive())return Long.MAX_VALUE;
            int bytes=c==byte.class||c==boolean.class?1:c==short.class||c==char.class?2:c==long.class||c==double.class?8:4;
            return 32L+(long)java.lang.reflect.Array.getLength(object)*bytes;
        }
        String name=type.getName();
        if(type.getClassLoader()!=CpuFinishPolicy1978.class.getClassLoader()||
                !(name.equals("com.hiro.ulike.QualityPixels1932$Plan")||name.equals("com.hiro.ulike.QualityPixels1932$NoiseStats")||
                name.equals("com.hiro.ulike.SpatialNoise1934")||name.equals("com.hiro.ulike.SpatialNoise1934$Axis")||
                name.equals("com.hiro.ulike.FaceRegions1934$Mask")||
                name.equals("com.hiro.ulike.QualityPipeline1932$SmoothRegions1958")||name.equals("com.hiro.ulike.QualityPipeline1932$SmoothRegions1958$1")))return Long.MAX_VALUE;
        long bytes=256;
        for(Field f:type.getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())&&!f.getType().isPrimitive()) {
            f.setAccessible(true);long child=retainedObject(f.get(object),seen,depth+1);
            if(child>Long.MAX_VALUE-bytes)return Long.MAX_VALUE;bytes+=child;
        }
        return bytes;
    }
    private static void interrupted(){if(Thread.currentThread().isInterrupted())throw new IllegalStateException("quality interrupted");}
}
