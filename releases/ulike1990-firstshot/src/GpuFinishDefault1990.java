package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.os.Build;
import java.util.concurrent.CancellationException;

/** Fixed, bounded finishing graph admitted by independent device-local corpus
 * evidence. This is not a per-photograph exact proof or a speed forecast.
 * Sigma, noise maps and immutable masks still run through the original CPU
 * policy builder on EVERY output coordinate. Only its rounded integer words
 * reach the shader; photo-specific float bits are not the cache identity here.
 * Original exact-rejection keys remain authoritative for every photograph.
 */
public final class GpuFinishDefault1990 {
    private GpuFinishDefault1990() {}
    static final String SUITE="finish1990-cpu-policy32-corpus-v1";
    static final int CASES=28;
    private static volatile String status="補正GPUの起動時検証待ち";

    public static boolean ready(){return GpuCertificates1990.restore(SUITE)!=null;}
    public static String summary(){return status;}
    private static boolean known(Object value,String name){return value==null||
        value.getClass().getClassLoader()==GpuFinishDefault1990.class.getClassLoader()&&value.getClass().getName().equals(name);}
    /** Bounds describe an admitted implementation domain, not exhaustive test
     * coverage. Actual opaque-source checking, memory admission and CPU integer
     * policy construction remain in runFinish1976; no sampled mask is reused. */
    static boolean owns(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan) {
        if(source==null||source.isRecycled()||source.getConfig()!=Bitmap.Config.ARGB_8888||plan==null||
                width<2||height<2||width>8192||height>8192||source.getWidth()<2||source.getHeight()<2||
                source.getWidth()>8192||source.getHeight()>8192||rotation!=0&&rotation!=90&&rotation!=180&&rotation!=270||
                plan.noiseLevel<0||plan.noiseLevel>4||plan.sharpLevel<1||plan.sharpLevel>4||
                plan.sharpGainQ8<1||plan.sharpGainQ8>256||plan.sharpFloorQ8<0||plan.sharpFloorQ8>16384||
                plan.sharpLimit<1||plan.sharpLimit>11||!(plan.sourceSigma>=0f&&plan.sourceSigma<=32f)||
                !(plan.outputScale>=.25f&&plan.outputScale<=4f))return false;
        ColorSpace color=source.getColorSpace();
        if(color!=null&&!color.isSrgb()||Build.VERSION.SDK_INT>=34&&source.hasGainmap())return false;
        if(!known(plan.faceRegions,"com.hiro.ulike.FaceRegions1934$Mask"))return false;
        return plan.smoothedRegions==null||known(plan.smoothedRegions,"com.hiro.ulike.QualityPipeline1932$SmoothRegions1958")||
            known(plan.smoothedRegions,"com.hiro.ulike.QualityPipeline1932$SmoothRegions1958$1");
    }
    static Bitmap run(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan,
            boolean refresh,ProcessingTiming1947.Trace trace,String exactPhotoKey) {
        if(!owns(source,rotation,width,height,plan)||!ready()||exactPhotoKey==null||
                GpuQualification1961.exactRejected(exactPhotoKey)||
                GpuQualification1961.exactRejected(exactPhotoKey.replace("|finish-chain1976:v2|","|finish-chain1962:v1|")))return null;
        try {return GpuChain1961.runFinish1976(source,rotation,width,height,plan,refresh,trace,32);}
        catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){if(Thread.currentThread().isInterrupted())throw new CancellationException("finish startup route interrupted");return null;}
        catch(LinkageError unavailable){return null;}
        catch(OutOfMemoryError unavailable){return null;}
    }
    private static void check(GpuQualification1961.Cancellation cancellation) {
        if(Thread.currentThread().isInterrupted()||cancellation.cancelled())throw new CancellationException("finish startup cancelled");
    }
    /** Runs on the startup worker. Synthetic bounded images never retain a
     * camera frame; maximum source is 4080 x 68, maximum output 5712 x 96.
     * Measurements are corpus observations only and do not qualify photo speed.
     */
    public static boolean qualify(GpuQualification1961.Cancellation cancellation) {
        if(cancellation==null)return false;check(cancellation);
        if(ready()){status="補正GPUの端末・処理版が一致する起動時検証記録あり";return true;}
        if(GpuCertificates1990.rejected(SUITE)){status="補正GPUの起動時画質不一致記録あり";return false;}
        if(!GpuNoise1960.supports(GpuNoise1960.FINISH1961)||!GpuNoise1960.supports(GpuNoise1960.GEOMETRY)||
                !GpuNoise1960.supports(GpuNoise1960.ANALYSIS1961))return false;
        long cpu=0,gpu=0,pixels=0;int cases=0;
        try {
            for(int id=0;id<CASES;id++) {
                check(cancellation);status="補正GPUの起動時検証 "+id+"/"+CASES;
                Fixture f=fixture(id);Bitmap expected=null,actual=null;
                try {
                    if(!f.alpha){long start=System.nanoTime();expected=GpuChain1961.cpuFinish(f.source,f.rotation,f.width,f.height,f.plan,f.refresh);
                        cpu+=Math.max(1L,System.nanoTime()-start);check(cancellation);}
                    long start=System.nanoTime();actual=GpuChain1961.runFinish1976(f.source,f.rotation,f.width,f.height,f.plan,f.refresh,null,32);
                    gpu+=Math.max(1L,System.nanoTime()-start);check(cancellation);
                    String mismatch=f.alpha?(actual==null?null:"unsupported alpha admitted"):difference(expected,actual);
                    // A backend/admission failure is retryable, never a permanent
                    // mismatch and never a falsely completed finite certificate.
                    if(!f.alpha&&actual==null){status="補正GPUの起動時検証を完了できません";return false;}
                    if(mismatch!=null){status="補正GPUの起動時画質不一致: case="+id+" "+mismatch;
                        GpuCertificates1990.reject(SUITE,status);return false;}
                    cases++;if(!f.alpha)pixels+=(long)f.width*f.height;
                }finally{if(expected!=null&&expected!=f.source&&!expected.isRecycled())expected.recycle();
                    if(actual!=null&&actual!=f.source&&!actual.isRecycled())actual.recycle();f.source.recycle();}
            }
            check(cancellation);boolean saved=GpuCertificates1990.passed(SUITE,cases,pixels,cpu,gpu);
            status=saved?"補正GPUの起動時検証26例で全画素一致・透過2例は対象外（撮影画像の比較ではありません）":"補正GPU検証記録を保存できません";
            return saved;
        }catch(CancellationException cancelled){status="補正GPUの起動時検証中断";throw cancelled;}
        catch(RuntimeException unavailable){status="補正GPUの起動時検証を完了できません";return false;}
        catch(LinkageError unavailable){status="補正GPUの起動時検証を利用できません";return false;}
        catch(OutOfMemoryError unavailable){status="補正GPUの起動時検証メモリ不足";return false;}
    }
    static String difference(Bitmap expected,Bitmap actual) {
        if(expected==null||actual==null)return "output missing";
        int width=expected.getWidth(),height=expected.getHeight();
        if(actual.getWidth()!=width||actual.getHeight()!=height)return "output shape";
        int[] a=new int[width],b=new int[width];
        for(int y=0;y<height;y++){if(Thread.currentThread().isInterrupted())throw new CancellationException("finish compare interrupted");
            expected.getPixels(a,0,width,0,y,width,1);actual.getPixels(b,0,width,0,y,width,1);
            for(int x=0;x<width;x++)if(a[x]!=b[x])return "argb x="+x+" y="+y+" cpu="+Integer.toHexString(a[x])+" gpu="+Integer.toHexString(b[x]);}
        return null;
    }
    static final class Fixture {
        final Bitmap source;final int rotation,width,height;final QualityPixels1932.Plan plan;final boolean refresh,alpha;
        Fixture(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan,boolean refresh,boolean alpha){
            this.source=source;this.rotation=rotation;this.width=width;this.height=height;this.plan=plan;this.refresh=refresh;this.alpha=alpha;}
    }
    static Fixture fixture(final int id) {
        if(id<0||id>=CASES)throw new IllegalArgumentException("finish corpus case");
        boolean wide=id>=24&&id<26,alpha=id>=26;
        int sw=wide?(id==24?3060:4080):65+2*(id%3),sh=wide?68:67+2*(id%5);
        int width=wide?(id==24?4284:5712):id%3==0?sw:id%3==1?93:37;
        int height=wide?96:id%3==0?sh:id%3==1?99:41;
        int rotation=wide?0:(id%4)*90;
        Bitmap source=Bitmap.createBitmap(sw,sh,Bitmap.Config.ARGB_8888);int[] row=new int[sw];long random=0x19905eedL+id;
        for(int y=0;y<sh;y++){
            for(int x=0;x<sw;x++){
                random=(random*1664525L+1013904223L)&0xffffffffL;int jitter=((int)(random>>>24)&15)-8;
                int pattern=(id/4)%6,base=pattern==0?16:pattern==1?128:pattern==2?240:pattern==3?(x<sw/2?32:224):pattern==4?((x*17+y*31)&255):(int)(random>>>24);
                int r=clamp(base+jitter),g=clamp(base-jitter),b=clamp(base+(pattern==4?71:0)+jitter/2);
                int a=alpha&&y==sh-1&&x==sw-1?127:255;row[x]=(a<<24)|(r<<16)|(g<<8)|b;
            }source.setPixels(row,0,sw,0,y,sw,1);
        }
        float sigma=new float[]{0f,.6f,3.8f,12f,24f,32f}[id%6];
        float scale=Math.max((float)width/sw,(float)height/sh);
        QualityPixels1932.Plan plan=QualityPixels1932.plan(new QualityPixels1932.NoiseStats(sigma,sigma,128f,0f,64),
            800,16666666L,0,.35f,4,1+id%4,(id&1)!=0,true,scale).withHaloSuppression((id&2)!=0);
        if((id&1)!=0)plan=plan.withFaceRegions(new QualityPixels1932.RegionMask(){
            public int skinQ8(int x,int y){return new int[]{0,1,127,128,255,256}[(x/7+y/5)%6];}
            public int detailQ8(int x,int y){return (x*13+y*9)%257;}
        }).withSmoothedRegions(new QualityPixels1932.SmoothMask(){public int smoothingQ8(int x,int y){return (x/4+y/4)%3==0?256:(x/4+y/4)%3==1?0:127;}});
        return new Fixture(source,rotation,width,height,plan,(id&4)!=0||wide,alpha);
    }
    private static int clamp(int value){return Math.max(0,Math.min(255,value));}
}
