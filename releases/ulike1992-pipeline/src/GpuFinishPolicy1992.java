package com.hiro.ulike;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CancellationException;

/** Exact resident finishing policy. The caller already owns the private GPU
 * session and a whole-finish admission covering this versioned graph. Immutable
 * masks retain their original Java double sampling; one packed pair is uploaded
 * per coordinate. Float noise interpolation and all four final policy channels
 * are produced directly into the finish bank, without Java policy/readback. */
final class GpuFinishPolicy1992 {
    private GpuFinishPolicy1992() {}
    static final int GRID_SLOT=20,MASK_SLOT0=21,MASK_SLOT1=22;
    static final int CASES=24;
    static final String SUITE="finish-policy1992-packedmask-exact-ieeediv-corpus-v1";
    private static volatile String verificationFailure="未検証";
    static String verificationFailure(){return verificationFailure;}
    static boolean ready(){return GpuCertificates1990.restore(SUITE)!=null;}
    private static Object field(Object owner,String name){
        try{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
        catch(ReflectiveOperationException unavailable){throw new IllegalArgumentException("unsupported finish policy descriptor",unavailable);}
    }
    private static void check(){if(Thread.currentThread().isInterrupted())throw new CancellationException("resident finish policy interrupted");}
    private static boolean finite(float value){return !Float.isNaN(value)&&!Float.isInfinite(value);}
    /** Zero native commands on a declined domain. Source/output pixels and old
     * CPU mask callback traversal are never modified by this optional builder. */
    static Context open(GpuNoise1960.Session session,QualityPixels1932.Plan plan,int width,int height,int tileRows){
        if(session==null||!CpuFinishPolicy1978.immutable(plan)||width<1||height<1||tileRows<1||
                (long)width*Math.min(tileRows,height)>Integer.MAX_VALUE/4L||
                !finite(plan.sourceSigma)||plan.sourceSigma<0f||plan.sourceSigma>32f||
                !finite(plan.outputScale)||plan.outputScale<=0f||
                plan.sharpFloorQ8<0||plan.sharpFloorQ8>16384||plan.beautyQ8<0||plan.beautyQ8>256)return null;
        boolean noise=plan.localNoise!=null&&(plan.noiseMapAtOutput||Math.abs(plan.outputScale-1f)<.00001f);
        boolean face=plan.texturePriority&&plan.faceRegions!=null,smooth=plan.smoothedRegions!=null;
        if(!noise&&!face&&!smooth)return null; // original four-word constant is cheaper
        int[] u=new int[32];float[] f=new float[32];float[] grid=new float[]{0f};
        u[0]=3;u[1]=width;u[6]=noise?1:0;u[7]=plan.sharpFloorQ8;
        u[8]=Math.max(2,Math.round(Math.min(32f,plan.sourceSigma)*.85f));
        u[9]=Math.round((2f+Math.min(32f,plan.sourceSigma)*2.4f)*256f);u[10]=20+(plan.beautyQ8>>3);
        u[11]=face||smooth?0:1;
        f[0]=plan.sourceSigma;f[1]=!plan.noiseMapAtOutput&&plan.outputScale<1f?(float)Math.sqrt(plan.outputScale):1f;
        if(noise){
            SpatialNoise1934 map=plan.localNoise;
            if(map.width!=width||map.height!=height||map.columns<1||map.rows<1||(long)map.columns*map.rows>169)return null;
            try{
                grid=((float[])field(map,"sigma")).clone();
                if(grid.length!=map.columns*map.rows)return null;
                for(float value:grid)if(!finite(value)||value<0f||value>96f)return null;
                u[4]=map.columns;u[5]=map.rows;
                f[2]=((Float)field(map,"left")).floatValue();f[3]=((Float)field(map,"top")).floatValue();
                f[4]=((Float)field(map,"stepX")).floatValue();f[5]=((Float)field(map,"stepY")).floatValue();
                for(int i=2;i<=5;i++)if(!finite(f[i]))return null;
                if(map.columns>1&&f[4]<=0f||map.rows>1&&f[5]<=0f)return null;
            }catch(RuntimeException unavailable){return null;}
        }
        check();if(!GpuNoise1960.supports(GpuNoise1960.PROTECTION1961))return null;
        int capacity=Math.multiplyExact(width,Math.min(tileRows,height)),maskCapacity=face||smooth?capacity:1;
        GpuNoise1960.Lease1971 lease=session.reserveCapacity1983(new int[]{GRID_SLOT,MASK_SLOT0,MASK_SLOT1},
            new long[]{4L*grid.length,4L*maskCapacity,4L*maskCapacity},
            new long[]{4L*grid.length,4L*maskCapacity,4L*maskCapacity},4L*maskCapacity+512L);
        if(lease==null)return null;
        boolean transferred=false;
        try{
            int[] masks=new int[maskCapacity];lease.consumeJava1974(4L*maskCapacity);
            check();if(!lease.revalidate1971())return null;
            if(!session.run(new GpuNoise1960.Batch().uploadDirect(GRID_SLOT,grid)))throw new IllegalStateException("resident policy metadata upload failed");
            Context context=new Context(session,plan,width,height,capacity,u,f,masks,face,smooth,lease,4L*grid.length);
            transferred=true;return context;
        }finally{if(!transferred)lease.close();}
    }
    static final class Context implements AutoCloseable {
        private final GpuNoise1960.Session session;private final QualityPixels1932.Plan plan;
        private final int width,height,capacity;private final int[] u,masks;private final float[] f;
        private final boolean face,smooth;private GpuNoise1960.Lease1971 lease;
        final long metadataUploadBytes;private boolean closed,bandOpen;
        Context(GpuNoise1960.Session session,QualityPixels1932.Plan plan,int width,int height,int capacity,
                int[] u,float[] f,int[] masks,boolean face,boolean smooth,GpuNoise1960.Lease1971 lease,long uploaded){
            this.session=session;this.plan=plan;this.width=width;this.height=height;this.capacity=capacity;this.u=u;this.f=f;
            this.masks=masks;this.face=face;this.smooth=smooth;this.lease=lease;metadataUploadBytes=uploaded;
        }
        /** Caller has collected this bank before reuse; submit consumes this
         * single owned CPU array synchronously before Band.close allows refill. */
        synchronized Band prepare(int first,int last,int bank){
            if(closed||bandOpen||first<0||last<=first||last>height||bank<0||bank>1||
                    (long)width*(last-first)>capacity)throw new IllegalArgumentException("resident finish policy band ownership");
            check();long started=System.nanoTime();int count=width*(last-first);
            if(face||smooth){int at=0;
                for(int y=first;y<last;y++){check();for(int x=0;x<width;x++,at++){
                    if((x&4095)==0)check();
                    int skin=face?plan.skinAt(x,y):0;
                    if(skin<0||skin>256)throw new IllegalArgumentException("immutable skin outside exact packed domain");
                    int confidence=smooth?Math.max(0,Math.min(256,plan.smoothedRegions.smoothingQ8(x,y))):0;
                    masks[at]=skin|(confidence<<16);
                }}
            }else masks[0]=0;
            check();if(!lease.revalidate1971())throw new IllegalStateException("resident finish policy workspace unavailable");
            int[] parameters=u.clone();parameters[2]=last-first;parameters[3]=first;
            Band band=new Band(this,parameters,bank,count,System.nanoTime()-started,4L*masks.length);
            bandOpen=true;return band;
        }
        public synchronized void close(){if(closed)return;closed=true;bandOpen=false;if(lease!=null){lease.close();lease=null;}}
    }
    static final class Band implements AutoCloseable {
        final int mode=FinishPolicy1953.RAW4,pixels;final long cpuNanos,uploadBytes;
        private final Context owner;private final int[] u;private final int bank;private boolean appended,closed;
        Band(Context owner,int[] u,int bank,int pixels,long cpuNanos,long uploadBytes){this.owner=owner;this.u=u;this.bank=bank;this.pixels=pixels;this.cpuNanos=cpuNanos;this.uploadBytes=uploadBytes;}
        GpuNoise1960.Batch append(GpuNoise1960.Batch batch,int policySlot){
            synchronized(owner){
                if(batch==null||closed||appended||owner.closed||!owner.bandOpen||policySlot<0||policySlot>=25||
                        policySlot==GRID_SLOT||policySlot==MASK_SLOT0||policySlot==MASK_SLOT1)throw new IllegalArgumentException("resident policy append ownership");
                check();int maskSlot=bank==0?MASK_SLOT0:MASK_SLOT1;
                batch.uploadDirect(maskSlot,owner.masks).allocate(policySlot,16L*pixels)
                    .dispatch(GpuNoise1960.PROTECTION1961,new int[]{GRID_SLOT,policySlot,GRID_SLOT,GRID_SLOT,GRID_SLOT,maskSlot},u,owner.f,pixels);
                appended=true;return batch;
            }
        }
        public void close(){synchronized(owner){if(closed)return;closed=true;owner.bandOpen=false;}}
    }
    /** Bounded synthetic complete-policy proof included by the new whole-finish
     * startup suite. Every pixel and four channels are compared twice against
     * the original CPU builder, with actual immutable face/smoothing owners. */
    static boolean verify(GpuQualification1961.Cancellation cancellation){
        if(cancellation==null)return false;
        if(cancellation.cancelled())throw new CancellationException("finish policy verification interrupted");check();
        if(ready()){verificationFailure="";return true;}
        if(GpuCertificates1990.rejected(SUITE)){verificationFailure="policy prior mismatch";return false;}
        verificationFailure="policy verification incomplete";
        long cpuNanos=0,gpuNanos=0,pixels=0;int cases=0;
        try{
            for(int id=0;id<CASES;id++){
                if(cancellation==null||cancellation.cancelled())throw new CancellationException("finish policy verification interrupted");check();
                Fixture fixture=fixture(id);
                for(int pass=0;pass<2;pass++){
                    GpuNoise1960.Session session=GpuNoise1960.open();if(session==null){verificationFailure="policy session unavailable";return false;}
                    Context context=null;GpuNoise1960.Lease1971 outputLease=null;
                    try{
                        int tile=Math.min(fixture.height,pass==0?7:17);
                        long opened=System.nanoTime();context=open(session,fixture.plan,fixture.width,fixture.height,tile);gpuNanos+=System.nanoTime()-opened;
                        if(context==null){verificationFailure="policy descriptor unavailable case="+id;return false;}
                        outputLease=session.reserveCapacity1983(new int[]{3},new long[]{16L*fixture.width*tile},new long[]{0},16L*fixture.width*tile);
                        if(outputLease==null){verificationFailure="policy memory unavailable";return false;}
                        for(int first=0;first<fixture.height;first+=tile){
                            if(cancellation.cancelled())throw new CancellationException("finish policy verification interrupted");
                            int last=Math.min(fixture.height,first+tile),count=fixture.width*(last-first);
                            long started=System.nanoTime();Band band=context.prepare(first,last,0);int[][] actual;
                            try{actual=session.execute(band.append(new GpuNoise1960.Batch(),3),new int[]{3},new int[]{count*4});}
                            finally{band.close();}
                            gpuNanos+=System.nanoTime()-started;
                            if(actual==null){verificationFailure="policy transport unavailable";return false;}
                            started=System.nanoTime();FinishPolicy1953.Band expected=FinishPolicy1953.prepareCpu1961(fixture.plan,fixture.width,fixture.height,first,last,0);cpuNanos+=System.nanoTime()-started;
                            if(expected==null){verificationFailure="policy CPU reference unavailable";return false;}
                            try{for(int i=0;i<count;i++)for(int lane=0;lane<4;lane++){
                                int value=expected.mode==FinishPolicy1953.CONSTANT4?expected.words[lane]:expected.mode==FinishPolicy1953.RAW4?expected.words[i*4+lane]:expected.words[i*2+lane/2]>>>(16*(lane&1))&65535;
                                if(actual[0][i*4+lane]!=value){verificationFailure="policy mismatch case="+id+" x="+(i%fixture.width)+" y="+(first+i/fixture.width)+" channel="+lane;GpuCertificates1990.reject(SUITE,verificationFailure);return false;}
                            }}finally{expected.close();}
                            pixels+=count;
                        }
                        cases++;
                    }finally{if(outputLease!=null)outputLease.close();if(context!=null)context.close();session.close();}
                }
            }
            if(cancellation.cancelled())throw new CancellationException("finish policy verification interrupted");check();
            boolean saved=GpuCertificates1990.passed(SUITE,cases,pixels,cpuNanos,gpuNanos);
            verificationFailure=saved?"":"policy certificate unavailable";return saved;
        }catch(CancellationException cancelled){throw cancelled;}
        catch(ReflectiveOperationException unavailable){verificationFailure="policy fixture unavailable";return false;}
        catch(LinkageError unavailable){verificationFailure="policy backend unsupported";return false;}
        catch(OutOfMemoryError unavailable){verificationFailure="policy workspace unavailable";return false;}
        catch(RuntimeException unavailable){verificationFailure="policy verification exception";return false;}
    }
    static final class Fixture {final QualityPixels1932.Plan plan;final int width,height;Fixture(QualityPixels1932.Plan p,int w,int h){plan=p;width=w;height=h;}}
    static Fixture fixture(int id)throws ReflectiveOperationException{
        if(id<0||id>=CASES)throw new IllegalArgumentException("finish policy corpus id");
        int sw=35,sh=39,turn=(id&3)*90,width=id==8?sw:id%3==0?17:id%3==1?65:97,height=id==8?sh:id%3==0?19:id%3==1?67:43;
        float sigma=new float[]{0f,.6f,3.8f,12f,24f,32f}[id%6],scale=id%5==0?.5f:id%5==1?1f:1.4f;
        QualityPixels1932.Plan plan=QualityPixels1932.plan(new QualityPixels1932.NoiseStats(sigma,sigma,128f,0f,64),800,16666666L,0,.35f,4,1+id%4,true,true,scale);
        int columns=id%6==0?1:id%6==5?13:3,rows=id%6==1?1:id%6==5?13:3;
        float[] grid=new float[columns*rows];for(int i=0;i<grid.length;i++)grid[i]=new float[]{0f,.6000001f,3.8f,12.25f,31.999998f,64f,96f}[(i+id)%7];
        Constructor<SpatialNoise1934> noiseConstructor=SpatialNoise1934.class.getDeclaredConstructor(int.class,int.class,int.class,int.class,float.class,float.class,float.class,float.class,float[].class,QualityPixels1932.NoiseStats.class);
        noiseConstructor.setAccessible(true);
        SpatialNoise1934 noise=noiseConstructor.newInstance(width,height,columns,rows,.25f,.75f,columns==1?1f:(width-2f)/(columns-1),rows==1?1f:(height-3f)/(rows-1),grid,new QualityPixels1932.NoiseStats(sigma,sigma,128f,0f,64));
        plan=id%5==0?plan.withLocalNoise(noise,4):plan.withOutputNoise(noise);
        if(id%3!=0||id%5==0){
            Class<?> type=Class.forName("com.hiro.ulike.FaceRegions1934$Mask");
            Constructor<?> c=type.getDeclaredConstructor(int.class,int.class,int.class,int.class,byte[].class,byte[].class,double.class,double.class,double.class,double.class,double.class,double.class,boolean.class);c.setAccessible(true);
            byte[] skin=new byte[63],detail=new byte[63];for(int i=0;i<skin.length;i++){skin[i]=(byte)((i*37+id*29)&255);detail[i]=(byte)((i*71+id)&255);}
            QualityPixels1932.RegionMask mask=(QualityPixels1932.RegionMask)c.newInstance(sw,sh,9,7,skin,detail,8.0/(sw-1),0.0,0.0,0.0,6.0/(sh-1),0.0,true);
            Method resample=type.getMethod("resample",int.class,int.class,int.class);plan=plan.withFaceRegions((QualityPixels1932.RegionMask)resample.invoke(mask,turn,width,height));
        }
        if(id%4!=0||id==8){
            Class<?> type=Class.forName("com.hiro.ulike.QualityPipeline1932$SmoothRegions1958");Constructor<?> c=type.getDeclaredConstructor(int.class,int.class);c.setAccessible(true);Object mask=c.newInstance(sw,sh);
            byte[] confidence=(byte[])field(mask,"confidence");for(int i=0;i<confidence.length;i++)confidence[i]=(byte)((i*53+id*11)&255);
            Method output=type.getDeclaredMethod("outputMask",int.class,int.class,int.class);output.setAccessible(true);
            plan=plan.withSmoothedRegions((QualityPixels1932.SmoothMask)output.invoke(mask,turn,width,height));
        }
        return new Fixture(plan,width,height);
    }
}
