package com.hiro.ulike;
import java.util.Arrays;
import java.util.Random;
/** Executes the actual JNI entry; false/fallback is a test failure. */
public final class NativeSharp1951 {
    public static void main(String[] args) {
        Random random=new Random(19512);
        long pixels=0,changed=0,moireChanges=0,combinedSharpChanges=0;int cases=0;
        for(int size=0;size<6;size++) {
            final int width=size<3?size+1:87,rows=size<3?size+2:91;
            final int[] src=new int[width*rows];
            for(int y=0;y<rows;y++)for(int x=0;x<width;x++) {
                int value=size==3?random.nextInt(256):
                    size==4?120+((x%4)<2?25:-25):100+random.nextInt(31);
                src[y*width+x]=0xff000000|((value&255)<<16)|((value&255)<<8)|(value&255);
                if(size==4) {
                    int chroma=(x&1)==0?24:-24;
                    src[y*width+x]=0xff000000|((120+chroma)<<16)|(120<<8)|(120-chroma);
                }
                if(size==5 && (x+y*13)%97==0)src[y*width+x]&=0x7fffffff;
            }
            for(int level=0;level<=4;level++)for(int option=0;option<8;option++) {
                final int maskMode=option;
                QualityPixels1932.Plan plan=QualityPixels1932.plan(
                    new QualityPixels1932.NoiseStats(option*.21f,.2f,128f,0f,96),100,
                    option==3?60000000L:10000000L,2,option*.14f,4,level,
                    (option&1)!=0,false,option==4?.6f:option==5?2f:1f)
                    .withHaloSuppression((option&2)!=0);
                if((option&4)!=0)plan=plan.withFaceRegions(new QualityPixels1932.RegionMask() {
                    public int skinQ8(int x,int y){return maskMode==7?Integer.MAX_VALUE:(x*11+y*17)&511;}
                    public int detailQ8(int x,int y){return 0;}
                });
                if(option>=6) {
                    SpatialNoise1934 noise=SpatialNoise1934.probe(new SpatialNoise1934.Patches() {
                        public void read(int[] p,int x,int y,int w,int h) {
                            for(int row=0;row<h;row++)System.arraycopy(src,(row+y)*width+x,p,row*w,w);
                        }
                    },width,rows);
                    plan=option==6?plan.withOutputNoise(noise):plan.withLocalNoise(noise,4);
                }
                for(int phase=0;phase<3;phase++)for(int m=0;m<2;m++) {
                    int first=phase==0?0:Math.min(phase==1?5:33,rows);
                    int last=phase==0?rows:phase==1?Math.max(first,rows-3):Math.min(rows,79);
                    int[] expected=new int[src.length],actual=new int[src.length],identity=src.clone();
                    Arrays.fill(expected,0x13579bdf);Arrays.fill(actual,0x13579bdf);
                    QualityPixels1932.finishStripAtBefore1951(src,expected,width,rows,first,last,plan,m==1,true,71);
                    if(!NativeMoire1951.testNativeFinish(src,actual,width,rows,first,last,plan,m==1,true,71))
                        throw new AssertionError("JNI native path disabled size="+size+" level="+level+" option="+option);
                    if(!Arrays.equals(src,identity))throw new AssertionError("source changed");
                    for(int i=0;i<src.length;i++) {
                        if(actual[i]!=expected[i])throw new AssertionError("JNI mismatch case="+cases+" index="+i);
                        if(i>=first*width && i<last*width && actual[i]!=src[i])changed++;
                    }
                    if(m==1 && level>0) {
                        int[] moireOnly=new int[src.length];Arrays.fill(moireOnly,0x13579bdf);
                        QualityPixels1932.finishStripAtBefore1951(src,moireOnly,width,rows,first,last,plan,true,false,71);
                        for(int i=first*width;i<last*width;i++) {
                            if(moireOnly[i]!=src[i])moireChanges++;
                            if(actual[i]!=moireOnly[i])combinedSharpChanges++;
                        }
                    }
                    pixels+=src.length;cases++;
                }
            }
        }
        if(changed==0 || moireChanges==0 || combinedSharpChanges==0)
            throw new AssertionError("combined corrections not exercised "+changed+"/"+moireChanges+"/"+combinedSharpChanges);
        ownershipAndInterruption();
        System.out.println("JNI_PASS cases="+cases+" pixels="+pixels+" changed="+changed+" moire="+moireChanges+" combined_sharp="+combinedSharpChanges);
    }
    private static void ownershipAndInterruption() {
        final int width=87,rows=91;
        final int[] src=new int[width*rows],parallel=new int[src.length],expected=new int[src.length];
        for(int y=0;y<rows;y++)for(int x=0;x<width;x++) {
            int value=(x*17+y*23+x*y)&255;
            src[y*width+x]=0xff000000|(value<<16)|(value<<8)|value;
        }
        final QualityPixels1932.Plan plan=QualityPixels1932.plan(
            new QualityPixels1932.NoiseStats(.4f,.3f,128f,0f,96),100,10000000L,2,.8f,4,4,true,false,1f)
            .withFaceRegions(new QualityPixels1932.RegionMask() {
                public int skinQ8(int x,int y){return (x*3+y*11)&255;}
                public int detailQ8(int x,int y){return 0;}
            });
        QualityPixels1932.finishStripAtBefore1951(src,expected,width,rows,0,rows,plan,true,true,71);
        final Throwable[] failures=new Throwable[3];Thread[] workers=new Thread[3];
        for(int worker=0;worker<3;worker++) {
            final int slot=worker,first=worker*30,last=worker==2?rows:first+30;
            workers[worker]=new Thread(new Runnable() {
                public void run() {try {
                    PolicyCache1945 cache=PolicyCache1945.borrow(plan,width,71+first,71+last);
                    try {
                        QualityPixels1932.Plan cached=cache==null?plan:plan.withPolicyCache(cache);
                        if(!NativeMoire1951.testNativeFinish(src,parallel,width,rows,first,last,cached,true,true,71))
                            throw new AssertionError("parallel native path disabled");
                    } finally {if(cache!=null)cache.close();}
                } catch(Throwable failure){failures[slot]=failure;}}
            });workers[worker].start();
        }
        for(Thread worker:workers)try {worker.join();}catch(InterruptedException failure){throw new AssertionError(failure);}
        for(Throwable failure:failures)if(failure!=null)throw new AssertionError(failure);
        if(!Arrays.equals(expected,parallel))throw new AssertionError("parallel disjoint writes");
        if(NativeMoire1951.testNativeFinish(src,src,width,rows,0,rows,plan,true,true,71))
            throw new AssertionError("native source alias accepted");
        int[] interrupted=new int[src.length],partial=new int[src.length];
        Arrays.fill(interrupted,0x13579bdf);Arrays.fill(partial,0x13579bdf);
        QualityPixels1932.finishStripAtBefore1951(src,partial,width,rows,5,16,plan,true,true,71);
        Thread.currentThread().interrupt();boolean thrown=false;
        try {NativeMoire1951.testNativeFinish(src,interrupted,width,rows,5,31,plan,true,true,71);}
        catch(IllegalStateException failure){thrown="quality interrupted".equals(failure.getMessage());}
        finally {Thread.interrupted();}
        if(!thrown || !Arrays.equals(interrupted,partial))throw new AssertionError("unaligned interruption boundary");
    }

}
