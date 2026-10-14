package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.os.Build;
import java.util.concurrent.CancellationException;

/** Fixed, bounded finishing graph admitted by independent device-local corpus
 * evidence. This is not a per-photograph exact proof or a speed forecast.
 * Immutable mask values and noise metadata preserve exact integer policy.
 * The in-session policy shader is separately checked against CPU generation;
 * unknown callbacks keep the original CPU policy builder.
 * Original exact-rejection keys remain authoritative for every photograph.
 */
public final class GpuFinishDefault1990 {
    private GpuFinishDefault1990() {}
    static final String SUITE="finish1993-exact-axis-policy32-corpus-v1";
    static final String CPU_SUITE1992="finish1993-cpu-policy32-corpus-v1";
    static final int CASES=28;
    private static volatile String status="補正GPUの起動時検証待ち";
    private static volatile String wideStatus1995="128行の検証待ち";
    private static volatile String firstWideFailure1995="",lastWideFailure1995="";
    private static final Object WIDE_LOCK1995=new Object();
    private static WideProbe1995 pendingWide1995;
    private static int wideAttempts1995;
    private static long nextWide1995;
    public static String summaryWide1995(){
        String state=wideReady1993()?"128行認定あり":GpuCertificates1990.rejected(wideSuite1993())?"128行は画質不一致で拒否":"128行未認定";
        String first=firstWideFailure1995,last=lastWideFailure1995;
        return state+" / "+wideStatus1995+(first.length()==0?"":" / 最初の失敗: "+first)+(last.length()==0||last.equals(first)?"":" / 最後の失敗: "+last);
    }
    private static void wideFailure1995(int id,String why){
        String value=(id<0?"検証準備":"case="+id)+" "+why;
        wideStatus1995=value;lastWideFailure1995=value;
        if(firstWideFailure1995.length()==0)firstWideFailure1995=value;
    }
    /** Synthetic metadata-only recovery. No image or callback is retained.
     * Completed attempts are bounded; capture cancellation does not consume one.
     * The next successful save may re-arm it after the thirty-second backoff. */
    static void requestWide1995(){
        if(!GpuQualification1961.optionalBenchmarksAllowed1995()||!ready()||wideReady1993()||
                GpuCertificates1990.rejected(wideSuite1993()))return;
        WideProbe1995 probe;
        synchronized(WIDE_LOCK1995){
            if(pendingWide1995!=null||wideAttempts1995>=3||System.nanoTime()-nextWide1995<0)return;
            probe=new WideProbe1995();pendingWide1995=probe;
        }
        // Queue callbacks may close a probe under the queue lock. Never acquire
        // that lock while holding WIDE_LOCK1995 (including rejection paths).
        try{GpuQualification1961.schedule("finish-wide1995-v1",512L,probe);}
        catch(RuntimeException unavailable){probe.close();}
        catch(LinkageError unavailable){probe.close();}
        catch(OutOfMemoryError unavailable){probe.close();}
    }
    private static final class WideProbe1995 implements GpuQualification1961.Probe {
        public void run(GpuQualification1961.Cancellation cancellation){
            if(!GpuQualification1961.optionalBenchmarksAllowed1995())return;
            boolean completed=false;
            try{check(cancellation);qualifyWide1995(cancellation);check(cancellation);completed=true;}
            finally{if(completed)synchronized(WIDE_LOCK1995){wideAttempts1995++;nextWide1995=System.nanoTime()+30000000000L;}}
        }
        public void close(){synchronized(WIDE_LOCK1995){if(pendingWide1995==this)pendingWide1995=null;}}
    }

    static String activeSuite1992(){return GpuFinishPolicy1992.ready()?SUITE:CPU_SUITE1992;}
    static String wideSuite1993(){return activeSuite1992()+":128-rows-geometry1994-v2";}
    static boolean wideReady1993(){return ready()&&GpuCertificates1990.restore(wideSuite1993())!=null;}
    public static boolean ready(){return GpuCertificates1990.restore(activeSuite1992())!=null;}
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
        if(!admissible1992(source,rotation,width,height,plan,exactPhotoKey))return null;
        final String selectedSuite1992=activeSuite1992();
        try {
            Bitmap result=GpuChain1961.runDefault1993(source,rotation,width,height,plan,refresh,trace);
            if(!selectedSuite1992.equals(activeSuite1992())||!ready()) {
                if(result!=null&&result!=source)result.recycle();return null;
            }
            return result;
        }
        catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){if(Thread.currentThread().isInterrupted())throw new CancellationException("finish startup route interrupted");return null;}
        catch(LinkageError unavailable){return null;}
        catch(OutOfMemoryError unavailable){return null;}
    }
    static boolean admissible1992(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan,String exactPhotoKey) {
        if(!owns(source,rotation,width,height,plan)||!ready()||exactPhotoKey==null||
                GpuQualification1961.exactRejected(exactPhotoKey)||
                GpuQualification1961.exactRejected(exactPhotoKey.replace("|finish-chain1976:v2|","|finish-chain1962:v1|")))return false;
        return true;
    }
    private static void check(GpuQualification1961.Cancellation cancellation) {
        if(Thread.currentThread().isInterrupted()||cancellation.cancelled())throw new CancellationException("finish startup cancelled");
    }
    /** Runs on the startup worker. Synthetic bounded images never retain a
     * camera frame; maximum source is 4080 x 191, maximum output 5712 x 257.
     * Measurements are corpus observations only and do not qualify photo speed.
     */
    public static boolean qualify(GpuQualification1961.Cancellation cancellation) {
        if(cancellation==null)return false;check(cancellation);
        // New policy has its own quality admission. Unsupported or incomplete
        // policy proof retains the existing CPU-policy + GPU-pixel standard.
        GpuFinishPolicy1992.verify(cancellation);
        final String selectedSuite1992=activeSuite1992();
        if(ready()){
            status="補正GPUの端末・処理版が一致する起動時検証記録あり";
            // A valid narrow certificate does not certify the independent wide
            // command schedule. Retrying it never revokes the narrow route.
            if(!wideReady1993()&&!GpuCertificates1990.rejected(wideSuite1993()))qualifyWide1995(cancellation);
            return ready();
        }
        if(GpuCertificates1990.rejected(selectedSuite1992)){status="補正GPUの起動時画質不一致記録あり";return false;}
        if(!GpuNoise1960.supports(GpuNoise1960.FINISH1961)||!GpuNoise1960.supports(GpuNoise1960.GEOMETRY)||
                !GpuNoise1960.supports(GpuNoise1960.ANALYSIS1961))return false;
        long cpu=0,gpu=0,wideGpu=0,pixels=0;int cases=0;
        final String wideSuite=wideSuite1993();boolean wide=!GpuCertificates1990.rejected(wideSuite);
        try {
            check(cancellation);
            for(int id=0;id<CASES;id++) {
                check(cancellation);status="補正GPUの起動時検証 "+id+"/"+CASES;
                Fixture f=fixture(id);Bitmap expected=null,actual=null;
                try {
                    if(!f.alpha){long start=System.nanoTime();expected=GpuChain1961.cpuFinish(f.source,f.rotation,f.width,f.height,f.plan,f.refresh);
                        cpu+=Math.max(1L,System.nanoTime()-start);check(cancellation);}
                    for(int tile:new int[]{32,128}) {
                    if(tile==128&&!wide)continue;
                    long start=System.nanoTime();
                    try{actual=GpuChain1961.runFinish1976(f.source,f.rotation,f.width,f.height,f.plan,f.refresh,null,tile);}
                    catch(CancellationException cancelled){throw cancelled;}
                    catch(RuntimeException unavailable){if(tile==32)throw unavailable;check(cancellation);wideFailure1995(id,"実行・転送を完了できず");wide=false;continue;}
                    catch(LinkageError unavailable){if(tile==32)throw unavailable;wideFailure1995(id,"GPU実装またはメモリを利用できず");wide=false;continue;}
                    catch(OutOfMemoryError unavailable){if(tile==32)throw unavailable;wideFailure1995(id,"GPU実装またはメモリを利用できず");wide=false;continue;}
                    if(tile==32)gpu+=Math.max(1L,System.nanoTime()-start);else wideGpu+=Math.max(1L,System.nanoTime()-start);check(cancellation);
                    String mismatch=f.alpha?(actual==null?null:"unsupported alpha admitted"):difference(expected,actual);
                    // A backend/admission failure is retryable, never a permanent
                    // mismatch and never a falsely completed finite certificate.
                    if(!f.alpha&&actual==null){if(tile==128){wideFailure1995(id,"出力未取得（実行・転送または利用条件）");wide=false;continue;}status="補正GPUの起動時検証を完了できません";return false;}
                    if(mismatch!=null){status="補正GPUの起動時画質不一致: case="+id+" "+mismatch;
                        GpuCertificates1990.reject(tile==32?selectedSuite1992:wideSuite,status);
                        if(tile==32)return false;wideFailure1995(id,"画質不一致: "+mismatch);wide=false;}
                    if(tile==32&&!f.alpha)pixels+=(long)f.width*f.height;
                    if(actual!=null&&actual!=f.source&&!actual.isRecycled())actual.recycle();actual=null;
                    }
                    cases++;
                }finally{if(expected!=null&&expected!=f.source&&!expected.isRecycled())expected.recycle();
                    if(actual!=null&&actual!=f.source&&!actual.isRecycled())actual.recycle();f.source.recycle();}
            }
            check(cancellation);if(!selectedSuite1992.equals(activeSuite1992()))return false;boolean saved=GpuCertificates1990.passed(selectedSuite1992,cases,pixels,cpu,gpu);
            if(saved&&wide){wide=GpuCertificates1990.passed(wideSuite,cases,pixels,cpu,wideGpu);
                if(wide)wideStatus1995="28例の検証完了（全画素一致26例・透過対象外2例）";
                else wideFailure1995(-1,"検証記録を保存できず");}
            status=saved?(wide?"補正GPUの起動時検証26例で32行・128行とも全画素一致・透過2例は対象外（撮影画像の比較ではありません）":"補正GPUの32行経路を検証済み・128行経路は未認定") :"補正GPU検証記録を保存できません";
            return saved;
        }catch(CancellationException cancelled){status="補正GPUの起動時検証中断";throw cancelled;}
        catch(RuntimeException unavailable){status="補正GPUの起動時検証を完了できません";return false;}
        catch(LinkageError unavailable){status="補正GPUの起動時検証を利用できません";return false;}
        catch(OutOfMemoryError unavailable){status="補正GPUの起動時検証メモリ不足";return false;}
    }
    /** Independent full corpus, identical pixels and alpha exclusion to startup.
     * It does not re-run or revoke a valid 32-row certificate. */
    static boolean qualifyWide1995(GpuQualification1961.Cancellation cancellation){
        if(cancellation==null)return false;check(cancellation);
        if(!ready())return false;
        final String baseline=activeSuite1992(),suite=wideSuite1993();
        if(GpuCertificates1990.rejected(suite)){wideStatus1995="画質不一致の拒否を保持";return false;}
        if(wideReady1993()){wideStatus1995="端末・処理版が一致する128行記録あり";return true;}
        long cpu=0,gpu=0,pixels=0;int cases=0,id=-1;
        try{
            if(!GpuNoise1960.supports(GpuNoise1960.FINISH1961)||!GpuNoise1960.supports(GpuNoise1960.GEOMETRY)||
                    !GpuNoise1960.supports(GpuNoise1960.ANALYSIS1961)){wideFailure1995(id,"必要なGPU処理に未対応");return false;}
            for(id=0;id<CASES;id++){
                check(cancellation);wideStatus1995="128行を再検証 "+id+"/"+CASES;
                Fixture f=fixture(id);Bitmap expected=null,actual=null;
                try{
                    if(!f.alpha){long started=System.nanoTime();expected=GpuChain1961.cpuFinish(f.source,f.rotation,f.width,f.height,f.plan,f.refresh);
                        cpu+=Math.max(1L,System.nanoTime()-started);check(cancellation);}
                    long started=System.nanoTime();actual=GpuChain1961.runFinish1976(f.source,f.rotation,f.width,f.height,f.plan,f.refresh,null,128);
                    gpu+=Math.max(1L,System.nanoTime()-started);check(cancellation);
                    if(!f.alpha&&actual==null){wideFailure1995(id,"出力未取得（実行・転送または利用条件）");return false;}
                    String mismatch=f.alpha?(actual==null?null:"unsupported alpha admitted"):difference(expected,actual);
                    if(mismatch!=null){wideFailure1995(id,"画質不一致: "+mismatch);GpuCertificates1990.reject(suite,wideStatus1995);return false;}
                    if(!f.alpha)pixels+=(long)f.width*f.height;cases++;
                }finally{if(expected!=null&&expected!=f.source&&!expected.isRecycled())expected.recycle();
                    if(actual!=null&&actual!=f.source&&!actual.isRecycled())actual.recycle();f.source.recycle();}
            }
            check(cancellation);
            if(!baseline.equals(activeSuite1992())||!ready()){wideFailure1995(-1,"基準認定が変更・失効");return false;}
            boolean saved=GpuCertificates1990.passed(suite,cases,pixels,cpu,gpu);
            if(saved)wideStatus1995="28例の再検証完了（全画素一致26例・透過対象外2例）";
            else wideFailure1995(-1,"検証記録を保存できず");
            return saved;
        }catch(CancellationException cancelled){wideFailure1995(id,"撮影・期限により中断（再試行可）");throw cancelled;}
        catch(RuntimeException unavailable){check(cancellation);wideFailure1995(id,"実行・転送を完了できず");return false;}
        catch(LinkageError unavailable){wideFailure1995(id,"GPU実装を利用できず");return false;}
        catch(OutOfMemoryError unavailable){wideFailure1995(id,"検証メモリ不足");return false;}
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
        int sw=wide?(id==24?3060:4080):65+2*(id%3),sh=wide?191:67+2*(id%5);
        int width=wide?(id==24?4284:5712):id%3==0?sw:id%3==1?93:37;
        int height=wide?257:id%3==0?sh:id%3==1?99:41;
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
