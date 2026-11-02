package com.hiro.ulike;

import java.lang.reflect.Constructor;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/** Exact integer-representation tests. The actual pinned .52 policy math is executed. */
public final class FinishPolicy1953Test {
    private static long assertions,cases,pixels,policyBytes,denseBytes;
    private static int packedCases,constantCases,rawCases;
    private static void need(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    private static int decoded(FinishPolicy1953.Band band,int pixel,int channel){
        if(band.mode==FinishPolicy1953.CONSTANT4)return band.words[channel];
        if(band.mode==FinishPolicy1953.RAW4)return band.words[pixel*4+channel];
        return (band.words[pixel*2+channel/2] >>> ((channel&1)*16)) & 65535;
    }
    private static QualityPixels1932.Plan standard(int noise,int sharp,boolean texture,float beauty,float scale,int mode){
        return QualityPixels1932.plan(new QualityPixels1932.NoiseStats(.1f+mode*2.07f,.37f+mode*1.63f,
            27f+mode*13f,.17f,mode%2==0?200:12),mode==0?0:100<<Math.min(6,mode),
            mode%2==0?10000000L:47000000L,mode%4,beauty,noise,sharp,texture,(mode&1)==0,scale)
            .withHaloSuppression((mode&2)==0);
    }
    private static SpatialNoise1934 map(float[] data)throws Exception {
        Constructor<SpatialNoise1934> c=SpatialNoise1934.class.getDeclaredConstructor(int.class,int.class,
            int.class,int.class,float.class,float.class,float.class,float.class,float[].class,
            QualityPixels1932.NoiseStats.class);
        c.setAccessible(true);return c.newInstance(512,384,3,3,-19.25f,-13.5f,41.75f,33.25f,data,
            new QualityPixels1932.NoiseStats(2.77f,4.01f,128f,.12f,200));
    }
    private static void compare(QualityPixels1932.Plan plan,int width,int rows,int first,int last,int origin)throws Exception {
        FinishPolicy1953.Band band=FinishPolicy1953.prepare(plan,width,rows,first,last,origin);
        need(band!=null,"valid band accepted");int[] reference=new int[4];boolean uniform=true,fits=true;
        int[] firstPolicy=null;int at=0;
        for(int y=first;y<last;y++)for(int x=0;x<width;x++,at++){
            NativeMoire1951.preparePolicy(plan,x,y+origin,reference,0);
            if(firstPolicy==null)firstPolicy=reference.clone();
            for(int k=0;k<4;k++){
                need(decoded(band,at,k)==reference[k],"exact decode case="+cases+" x="+x+" y="+y+" channel="+k);
                if(reference[k]!=firstPolicy[k])uniform=false;
                if(reference[k]<0 || reference[k]>65535)fits=false;
            }
        }
        int expectedMode=uniform?FinishPolicy1953.CONSTANT4:fits?FinishPolicy1953.PACKED2:FinishPolicy1953.RAW4;
        need(band.mode==expectedMode,"lossless smallest supported mode");
        need(band.pixels==width*(last-first),"pixel count includes every column/corner");
        need(band.words.length==(expectedMode==FinishPolicy1953.CONSTANT4?4:band.pixels*(expectedMode==FinishPolicy1953.PACKED2?2:4)),"bounded exact array words");
        need(band.policyBytes==(long)band.words.length*4L && band.denseBytes==(long)band.pixels*16L,"byte metrics represent actual arrays");
        need(band.rawFallback==(expectedMode==FinishPolicy1953.RAW4),"raw fallback metrics");
        policyBytes+=band.policyBytes;denseBytes+=band.denseBytes;pixels+=band.pixels;cases++;
        if(band.mode==FinishPolicy1953.PACKED2)packedCases++;else if(band.mode==FinishPolicy1953.CONSTANT4)constantCases++;else rawCases++;
        band.close();band.close();
    }
    private static void allSettings()throws Exception {
        final float[] scales={.125f,.999998f,1f,1.000002f,2.7f};
        final float[] beauty={0f,.15f,.5f,1f};
        final SpatialNoise1934 local=map(new float[]{.1f,.41f,1.9f,2.37f,4.21f,8.77f,15.19f,19.27f,31.83f});
        int id=0;
        for(int noise=0;noise<=4;noise++)for(int sharp=0;sharp<=4;sharp++)for(int texture=0;texture<2;texture++)
        for(float strength:beauty)for(float scale:scales){
            final int mode=id++;
            QualityPixels1932.Plan p=standard(noise,sharp,texture!=0,strength,scale,mode%8);
            if((mode%3)==1)p=p.withLocalNoise(local,noise);
            else if((mode%3)==2)p=p.withOutputNoise(local);
            p=p.withFaceRegions(new QualityPixels1932.RegionMask(){
                public int skinQ8(int x,int y){return (x*23+y*11+mode)&511;}
                public int detailQ8(int x,int y){return (x-y)&255;}
            });
            compare(p,23+(mode%5),29,mode%3,25+(mode%4),(mode%4==0)?-41:mode%61);
        }
        QualityPixels1932.Plan uniform=standard(4,4,true,1f,1f,3).withFaceRegions(new QualityPixels1932.RegionMask(){
            public int skinQ8(int x,int y){return -1000000;}
            public int detailQ8(int x,int y){return 0;}
        });
        compare(uniform,51,61,3,57,-37); // Signed uniform policy still fits four raw constants.
    }
    private static void signedOverflow()throws Exception {
        final int[] unusual={Integer.MIN_VALUE,Integer.MAX_VALUE,-1000000,1000000,-65536,65536,-1,0,256,1000000007};
        QualityPixels1932.Plan base=standard(4,4,true,1f,1f,7);
        for(int mode=0;mode<unusual.length;mode++){
            final int offset=mode;
            compare(base.withFaceRegions(new QualityPixels1932.RegionMask(){
                public int skinQ8(int x,int y){return unusual[(x+y*3+offset)%unusual.length];}
                public int detailQ8(int x,int y){return Integer.MIN_VALUE+x*y;}
            }),33,37,2,35,777+mode*13);
        }
        Random random=new Random(195317L);
        for(int mode=0;mode<30;mode++){
            final int seed=random.nextInt();
            compare(base.withFaceRegions(new QualityPixels1932.RegionMask(){
                public int skinQ8(int x,int y){return seed ^ (x*1103515245+y*1234567);}
                public int detailQ8(int x,int y){return seed;}
            }),39,43,5,40,-987);
        }
        // NaN/infinite/extreme map values still use original Java Math.round and wrap.
        compare(base.withOutputNoise(map(new float[]{Float.MAX_VALUE,Float.NaN,Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,-Float.MAX_VALUE,1f,-2f,256f,4096f})),41,47,3,45,1);
    }
    private static int skinFor(QualityPixels1932.Plan p,int target){
        int coefficient=20+(p.beautyQ8>>3);
        int guess=(int)(((long)256-target)*256/coefficient);
        for(int v=guess-20;v<=guess+20;v++)if(256-(v*coefficient>>8)==target)return v;
        throw new AssertionError("cannot construct boundary "+target);
    }
    private static void uint16Edges()throws Exception {
        final QualityPixels1932.Plan p=standard(4,4,true,1f,1f,2);
        final int[] targets={0,1,65534,65535};final int[] skin=new int[targets.length];
        for(int i=0;i<skin.length;i++)skin[i]=skinFor(p,targets[i]);
        compare(p.withFaceRegions(new QualityPixels1932.RegionMask(){
            public int skinQ8(int x,int y){return skin[(x+y)%skin.length];}
            public int detailQ8(int x,int y){return 0;}
        }),45,49,4,46,29);
        for(int target:new int[]{-1,65536}){
            final int bad=skinFor(p,target);
            compare(p.withFaceRegions(new QualityPixels1932.RegionMask(){
                public int skinQ8(int x,int y){return x==44?bad:0;}
                public int detailQ8(int x,int y){return 0;}
            }),45,49,4,46,29);
        }
        // Only boundary and corner coordinates differ: no candidate skip is permitted.
        compare(p.withFaceRegions(new QualityPixels1932.RegionMask(){
            public int skinQ8(int x,int y){return x==0 || x==44 || y==33 || y==74?256:0;}
            public int detailQ8(int x,int y){return 0;}
        }),45,49,4,46,29);
    }
    private static void fullWidthAndUniformScan()throws Exception {
        final int[] calls={0};QualityPixels1932.Plan p=standard(4,4,true,.5f,1f,3).withFaceRegions(new QualityPixels1932.RegionMask(){
            public int skinQ8(int x,int y){calls[0]++;return (x+y)&255;}
            public int detailQ8(int x,int y){return 0;}
        });
        FinishPolicy1953.Band packed=FinishPolicy1953.prepare(p,4080,64,11,44,219);
        need(packed.mode==FinishPolicy1953.PACKED2 && packed.policyBytes*2==packed.denseBytes,"4080-wide policy saves exactly half transfer bytes");
        need(calls[0]==4080*33,"nonuniform packing does one complete policy scan");packed.close();
        compare(p,4080,64,11,44,219);
        calls[0]=0;p=p.withFaceRegions(new QualityPixels1932.RegionMask(){
            public int skinQ8(int x,int y){calls[0]++;return 256;}
            public int detailQ8(int x,int y){return 0;}
        });
        FinishPolicy1953.Band constant=FinishPolicy1953.prepare(p,4080,64,11,44,219);
        need(constant.mode==FinishPolicy1953.CONSTANT4 && constant.policyBytes==16,"4080-wide uniform policy saves all repeated storage");
        need(calls[0]==4080*33,"uniform proof scans every coordinate");constant.close();
        compare(p,4080,64,11,44,219);
    }
    private static void leasesAndInterruption()throws Exception {
        final int width=29,rows=31;final QualityPixels1932.Plan base=standard(4,4,true,1f,1f,3);
        need(FinishPolicy1953.prepare(null,1,1,0,1,0)==null,"null plan declines");
        need(FinishPolicy1953.prepare(base,0,1,0,1,0)==null && FinishPolicy1953.prepare(base,1,1,0,0,0)==null,"invalid/empty geometry declines");
        need(FinishPolicy1953.prepare(base,1,1,-1,1,0)==null && FinishPolicy1953.prepare(base,1,1,0,2,0)==null,"invalid rows decline");
        need(FinishPolicy1953.prepare(base,Integer.MAX_VALUE,2,0,2,0)==null,"overflow geometry declines before allocation");
        SpeedWorkers1935.trim();
        int[] expectedScratch=SpeedWorkers1935.borrowInts(4),expectedPacked=SpeedWorkers1935.borrowInts(width*rows*2);
        SpeedWorkers1935.release(expectedScratch);SpeedWorkers1935.release(expectedPacked);
        QualityPixels1932.Plan interrupted=base.withFaceRegions(new QualityPixels1932.RegionMask(){
            public int skinQ8(int x,int y){if(x==11 && y==2)Thread.currentThread().interrupt();return (x+y)&255;}
            public int detailQ8(int x,int y){return 0;}
        });
        try{FinishPolicy1953.prepare(interrupted,width,rows,0,rows,0);throw new AssertionError("interrupt lost");}
        catch(IllegalStateException failure){need("quality interrupted".equals(failure.getMessage()),"interruption propagated");}
        need(Thread.interrupted(),"interrupt flag retained");
        int[] scratch=SpeedWorkers1935.borrowInts(4),packed=SpeedWorkers1935.borrowInts(width*rows*2);
        need(scratch==expectedScratch && packed==expectedPacked,"interruption returns all borrowed leases");
        SpeedWorkers1935.release(scratch);SpeedWorkers1935.release(packed);
        final RuntimeException maskFailure=new RuntimeException("mask failed");
        try{FinishPolicy1953.prepare(base.withFaceRegions(new QualityPixels1932.RegionMask(){
            public int skinQ8(int x,int y){if(x==11 && y==2)throw maskFailure;return (x+y)&255;}
            public int detailQ8(int x,int y){return 0;}
        }),width,rows,0,rows,0);throw new AssertionError("mask failure lost");}
        catch(RuntimeException real){need(real==maskFailure,"original mask failure preserved");}
        scratch=SpeedWorkers1935.borrowInts(4);packed=SpeedWorkers1935.borrowInts(width*rows*2);
        need(scratch==expectedScratch && packed==expectedPacked,"mask failure returns all borrowed leases");
        SpeedWorkers1935.release(scratch);SpeedWorkers1935.release(packed);
        SpeedWorkers1935.trim();
        expectedScratch=SpeedWorkers1935.borrowInts(4);
        int[] expectedRaw=SpeedWorkers1935.borrowInts(width*rows*4);
        SpeedWorkers1935.release(expectedScratch);SpeedWorkers1935.release(expectedRaw);
        final int[] calls={0};
        try{FinishPolicy1953.prepare(base.withFaceRegions(new QualityPixels1932.RegionMask(){
            public int skinQ8(int x,int y){
                if(++calls[0]==width*rows+20)Thread.currentThread().interrupt();
                return x==0?1000000:-1000000;
            }
            public int detailQ8(int x,int y){return 0;}
        }),width,rows,0,rows,0);throw new AssertionError("raw recompute interrupt lost");}
        catch(IllegalStateException failure){need("quality interrupted".equals(failure.getMessage()),"raw fallback interruption propagated");}
        need(Thread.interrupted(),"raw fallback interrupt flag retained");
        scratch=SpeedWorkers1935.borrowInts(4);int[] raw=SpeedWorkers1935.borrowInts(width*rows*4);
        need(scratch==expectedScratch && raw==expectedRaw,"raw fallback interruption returns all borrowed leases");
        SpeedWorkers1935.release(scratch);SpeedWorkers1935.release(raw);
        FinishPolicy1953.Band c=FinishPolicy1953.prepare(base,3,4,0,4,0);int[] lease=c.words;c.close();c.close();
        scratch=SpeedWorkers1935.borrowInts(4);int[] other=SpeedWorkers1935.borrowInts(4);
        need(scratch==lease && other!=lease,"idempotent close cannot double-lease same policy");
        SpeedWorkers1935.release(scratch);SpeedWorkers1935.release(other);
    }
    private static void concurrent()throws Exception {
        final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();Thread[] threads=new Thread[4];
        for(int k=0;k<threads.length;k++){final int offset=k;threads[k]=new Thread(new Runnable(){public void run(){
            try {
                QualityPixels1932.Plan p=standard(3,4,true,.5f,1f,offset).withFaceRegions(new QualityPixels1932.RegionMask(){
                    public int skinQ8(int x,int y){return (x+y+offset)&255;}
                    public int detailQ8(int x,int y){return 0;}
                });
                for(int pass=0;pass<8;pass++){
                    FinishPolicy1953.Band b=FinishPolicy1953.prepare(p,67,71,3,68,offset*127);int[] expected=new int[4];
                    for(int y=3;y<68;y++)for(int x=0;x<67;x++){
                        NativeMoire1951.preparePolicy(p,x,y+offset*127,expected,0);
                        for(int c=0;c<4;c++)if(decoded(b,(y-3)*67+x,c)!=expected[c])throw new AssertionError("concurrent lease mixed");
                    }
                    b.close();
                }
            }catch(Throwable error){failure.compareAndSet(null,error);}
        }});threads[k].start();}
        for(Thread t:threads)t.join();if(failure.get()!=null)throw new AssertionError(failure.get());
        need(true,"concurrent captures retain independent exact policy leases");
    }
    public static void main(String[] args)throws Exception {
        allSettings();signedOverflow();uint16Edges();fullWidthAndUniformScan();leasesAndInterruption();concurrent();
        need(packedCases>100 && constantCases>100 && rawCases>=40,"all representation modes exercised");
        need(policyBytes<denseBytes*3/4,"actual policy transfer/storage savings measured");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"pixels_compared\":"+pixels+
            ",\"packed2_cases\":"+packedCases+",\"constant4_cases\":"+constantCases+",\"raw4_cases\":"+rawCases+
            ",\"policy_bytes\":"+policyBytes+",\"dense_policy_bytes\":"+denseBytes+
            ",\"all_rounded_java_policy_values_exact\":true,\"signed_mask_wrap_exact\":true,\"uint16_boundary_fallback_exact\":true,"+
            "\"all_corner_coordinates_checked\":true,\"borrowed_lease_cleanup_checked\":true,\"physical_android_tested\":false}");
    }
}
