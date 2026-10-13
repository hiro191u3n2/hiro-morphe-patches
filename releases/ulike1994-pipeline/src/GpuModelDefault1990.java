package com.hiro.ulike;

import java.util.concurrent.CancellationException;

/** Device-local finite-corpus admission for the existing resident model graph.
 * The graph keeps all three scale maps on its original session for Strong.
 * No photograph shadow computation or invented per-shape timing is needed.
 */
public final class GpuModelDefault1990 {
    private GpuModelDefault1990() {}
    static final String SUITE="model1991-exact-prep51-div-corpus32-v2";
    static final int CASES=32;
    private static volatile String status="モデルGPU起動時検証待ち";
    private static volatile String layoutStatus1994="モデル配置は標準64スレッド";
    private static final boolean[] pending1994=new boolean[3];
    private static final boolean[] attempted1994=new boolean[3];
    private static volatile int unavailableLayouts1994;
    private static volatile int warmedLayouts1994;
    public static String summary(){int selected=variant1994();return status+" / "+(selected==0?layoutStatus1994:
        "モデル配置は画質一致・実測高速の"+(selected==1?32:128)+"スレッドを選択");}
    public static boolean ready(){return GpuCertificates1990.restore(SUITE)!=null;}
    static boolean owns(int[] half,int width,int height,int noise,float[] evidence,float[] runtime){
        if(half==null||width<1||height<1||width>8192||height>8192||noise<1||noise>4||
                half.length!=(long)((width+1)/2)*((height+1)/2)||evidence==null||evidence.length<16||
                runtime==null||runtime.length!=16+3*((width+63)/64)*((height+63)/64))return false;
        for(int i=0;i<16;i++)if(Float.isNaN(evidence[i])||Float.isInfinite(evidence[i]))return false;
        for(float value:runtime)if(Float.isNaN(value)||Float.isInfinite(value))return false;
        return true;
    }
    static boolean accepts(int[] half,int width,int height,int noise,boolean shadows,float[] evidence,float[] runtime){
        return owns(half,width,height,noise,evidence,runtime)&&ready()&&
            !GpuQualification1961.exactRejected(GpuAnalysis1961.residentExactKey1991(width,height,noise,shadows));
    }
    static String layoutSuite1994(int variant){return "model1994-layout"+variant+"-exact-corpus32-v1";}
    static String speedSuite1994(int variant){return "model1994-layout"+variant+"-bothwidths2trials10pct-v1";}
    private static String jobKey1994(int variant){return "model-layout1994-optional-v1:"+variant;}
    /** Separate quality and measured-layout certificates never weaken the
     * original standard model certificate or a photograph's exact rejection. */
    static int variant1994(){
        if(!ready())return 0;
        int selected=0;long best=Long.MAX_VALUE;
        for(int variant=1;variant<=2;variant++){
            if((unavailableLayouts1994&(1<<variant))!=0||(warmedLayouts1994&(1<<variant))==0)continue;
            GpuCertificates1990.Evidence exact=GpuCertificates1990.restore(layoutSuite1994(variant));
            GpuCertificates1990.Evidence speed=GpuCertificates1990.restore(speedSuite1994(variant));
            if(exact!=null&&speed!=null&&speed.gpuNanos<best){best=speed.gpuNanos;selected=variant;}
        }
        return selected;
    }
    static boolean admitted1994(int variant){return variant==0?ready():variant>=1&&variant<=2&&ready()&&
        (unavailableLayouts1994&(1<<variant))==0&&(warmedLayouts1994&(1<<variant))!=0&&GpuCertificates1990.restore(layoutSuite1994(variant))!=null&&
        GpuCertificates1990.restore(speedSuite1994(variant))!=null;}
    private static synchronized void warmed1994(int variant){warmedLayouts1994|=1<<variant;}
    static synchronized void unavailable1994(int variant){
        if(variant>=1&&variant<=2){unavailableLayouts1994|=1<<variant;layoutStatus1994="モデル配置候補を利用できず標準64スレッドへ復帰";}
    }
    static boolean faster1994(long baseline,long candidate){
        return baseline>0&&candidate>0&&candidate<=baseline-baseline/10;
    }
    /** Metadata-only jobs join the existing single idle queue AFTER standard
     * startup readiness. A shutter cancels them; no photo or bitmap is copied,
     * and foreground model preparation never waits for a layout proof. */
    static void requestOptional1994(){
        if(!ready()||GpuQualification1961.background())return;
        for(int variant=1;variant<=2;variant++){
            final int candidate=variant;
            if(GpuCertificates1990.rejected(layoutSuite1994(candidate))||
                    (warmedLayouts1994&(1<<candidate))!=0&&GpuCertificates1990.restore(speedSuite1994(candidate))!=null)continue;
            synchronized(pending1994){if(pending1994[candidate]||attempted1994[candidate])continue;pending1994[candidate]=true;}
            boolean transferred=false;
            try{
                transferred=GpuQualification1961.schedule(jobKey1994(candidate),512L,new GpuQualification1961.Probe(){
                    public void run(GpuQualification1961.Cancellation cancellation){
                        try{qualifyVariant1994(candidate,cancellation);}
                        finally{if(!cancellation.cancelled()&&!Thread.currentThread().isInterrupted())
                            synchronized(pending1994){attempted1994[candidate]=true;}}
                    }
                    public void close(){synchronized(pending1994){pending1994[candidate]=false;}}
                });
            }catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
            finally{if(!transferred)synchronized(pending1994){pending1994[candidate]=false;}}
        }
    }
    private static void checkLayout1994(GpuQualification1961.Cancellation cancellation){
        check(cancellation);if(!ready())throw new CancellationException("model baseline revoked");
    }
    private static long compareLayout1994(Fixture fixture,StrongNoise1958.Model expected,int variant,
            GpuQualification1961.Cancellation cancellation){
        checkLayout1994(cancellation);StrongNoise1958.Model actual=null;long started=System.nanoTime(),elapsed;
        try{
            actual=GpuAnalysis1961.residentCandidateExact1990(fixture.half,fixture.width,fixture.height,
                fixture.noise,fixture.shadows,fixture.evidence,fixture.runtime,variant);
            elapsed=Math.max(1L,System.nanoTime()-started);checkLayout1994(cancellation);
            if(actual==null){GpuQualification1961.outcome1984("execution_unavailable");return -1L;}
            if(!StrongNoise1958.sameModel1961(expected,actual)){
                GpuCertificates1990.reject(variant==0?SUITE:layoutSuite1994(variant),
                    "model_layout="+variant+" "+StrongNoise1958.modelDifference1991(expected,actual));
                if(variant==0)GpuStartup1990.invalidateModelDomain();
                GpuQualification1961.outcome1984("quality_mismatch");
                return -1L;
            }
            return elapsed;
        }finally{if(actual!=null)StrongNoise1958.discardResident1961(actual);}
    }
    static boolean qualifyVariant1994(int variant,GpuQualification1961.Cancellation cancellation){
        if(variant<1||variant>2||cancellation==null||!ready()||
                GpuCertificates1990.rejected(layoutSuite1994(variant)))return false;
        try{
            checkLayout1994(cancellation);
            if(!GpuNoise1960.supports(GpuNoise1960.variant(GpuNoise1960.ANALYSIS,variant))){
                GpuQualification1961.outcome1984("unsupported");return false;
            }
            for(int mode=0;mode<3;mode++){
                checkLayout1994(cancellation);
                if(!GpuNoise1960.supports(GpuNoise1960.strongExactPrepare1990(mode,variant))){
                    GpuQualification1961.outcome1984("unsupported");return false;
                }
            }
            checkLayout1994(cancellation);warmed1994(variant);
            // Driver program objects are process-local even when the exact
            // corpus/speed evidence survives a restart. Recompile only here,
            // on the idle owner; until then foreground uses prepared layout0.
            if(GpuCertificates1990.restore(layoutSuite1994(variant))!=null&&
                    GpuCertificates1990.restore(speedSuite1994(variant))!=null){
                GpuQualification1961.outcome1984("cached_preparation");return true;
            }
            long cpu=0,gpu=0,pixels=0;
            if(GpuCertificates1990.restore(layoutSuite1994(variant))==null){
                for(int id=0;id<CASES;id++){
                    checkLayout1994(cancellation);Fixture fixture=fixture(id);long started=System.nanoTime();
                    StrongNoise1958.Model expected=StrongNoise1958.preparedSnapshotCpu1961(fixture.half,fixture.width,
                        fixture.height,fixture.noise,fixture.shadows,fixture.evidence,fixture.runtime);
                    cpu+=Math.max(1L,System.nanoTime()-started);
                    long elapsed=compareLayout1994(fixture,expected,variant,cancellation);if(elapsed<0)return false;gpu+=elapsed;
                    for(int[] map:StrongNoise1958.gpuMaps1960(expected))pixels+=map.length;
                }
                checkLayout1994(cancellation);
                if(!GpuCertificates1990.passed(layoutSuite1994(variant),CASES,pixels,cpu,gpu)){
                    GpuQualification1961.outcome1984("comparison_incomplete");return false;
                }
            }
            long speedCpu=0,speedGpu=0,speedPixels=0;boolean faster=true;
            for(int id=0;id<2;id++){
                checkLayout1994(cancellation);Fixture fixture=speedFixture1994(id);long started=System.nanoTime();
                StrongNoise1958.Model expected=StrongNoise1958.preparedSnapshotCpu1961(fixture.half,fixture.width,
                    fixture.height,fixture.noise,fixture.shadows,fixture.evidence,fixture.runtime);
                speedCpu+=Math.max(1L,System.nanoTime()-started);
                // Both routes warm their compiled programs and allocation pool.
                // Every warmup output is compared too, outside timed selection.
                if(compareLayout1994(fixture,expected,0,cancellation)<0||
                        compareLayout1994(fixture,expected,variant,cancellation)<0)return false;
                for(int pair=0;pair<2;pair++){
                    long baseline,candidate;
                    if(pair==0){baseline=compareLayout1994(fixture,expected,0,cancellation);candidate=compareLayout1994(fixture,expected,variant,cancellation);}
                    else{candidate=compareLayout1994(fixture,expected,variant,cancellation);baseline=compareLayout1994(fixture,expected,0,cancellation);}
                    if(baseline<0||candidate<0)return false;
                    faster&=faster1994(baseline,candidate);speedGpu+=candidate;
                    for(int[] map:StrongNoise1958.gpuMaps1960(expected))speedPixels+=map.length;
                    try{CameraTrace1965.event("model_layout_speed1994",0,"variant="+variant+" width="+fixture.width+
                        " trial="+pair+" baseline_gpu_ns="+baseline+" candidate_gpu_ns="+candidate);}catch(Throwable optional){}
                }
            }
            checkLayout1994(cancellation);
            if(!faster){layoutStatus1994="モデル配置候補は実測速度条件に未達・標準を保持";
                GpuQualification1961.outcome1984("speed_condition");return false;}
            // Fields keep their actual CPU-oracle / candidate-GPU meanings.
            // The suite name certifies BOTH widths and BOTH alternating trials
            // beat the baseline GPU by 10%; baseline timing is logged as GPU.
            boolean saved=GpuCertificates1990.passed(speedSuite1994(variant),4,speedPixels,speedCpu,speedGpu);
            GpuQualification1961.outcome1984(saved?"qualified":"comparison_incomplete");
            if(saved)layoutStatus1994="モデル配置は画質一致・実測高速の"+(variant1994()==1?32:128)+"スレッドを選択";
            return saved;
        }catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){GpuQualification1961.exception1993(unavailable);GpuQualification1961.outcome1984("exception");return false;}
        catch(LinkageError unavailable){GpuQualification1961.outcome1984("unsupported");return false;}
        catch(OutOfMemoryError unavailable){GpuQualification1961.outcome1984("memory_budget");return false;}
    }
    private static void check(GpuQualification1961.Cancellation cancellation){
        if(Thread.currentThread().isInterrupted()||cancellation.cancelled())throw new CancellationException("model startup cancelled");
    }
    public static boolean qualify(GpuQualification1961.Cancellation cancellation){
        if(cancellation==null)return false;check(cancellation);
        if(ready()){status="モデルGPUの端末・処理版一致検証記録あり";return true;}
        if(GpuCertificates1990.rejected(SUITE)){status="モデルGPUの画質不一致記録あり";return false;}
        long cpu=0,gpu=0,pixels=0;int cases=0;
        try{
            if(!GpuNoise1960.supports(GpuNoise1960.ANALYSIS)){status="モデルGPU分析を利用できません";return false;}
            for(int mode=0;mode<3;mode++)if(!GpuNoise1960.supports(GpuNoise1960.strongExactPrepare1990(mode,0))){status="モデルGPU処理を利用できません";return false;}
            for(int id=0;id<CASES;id++){
                check(cancellation);status="モデルGPU起動時検証 "+id+"/"+CASES;
                Fixture fixture=fixture(id);long start=System.nanoTime();
                StrongNoise1958.Model expected=StrongNoise1958.preparedSnapshotCpu1961(fixture.half,fixture.width,fixture.height,
                    fixture.noise,fixture.shadows,fixture.evidence,fixture.runtime);
                cpu+=Math.max(1L,System.nanoTime()-start);check(cancellation);
                StrongNoise1958.Model actual=null;start=System.nanoTime();
                try{
                    actual=GpuAnalysis1961.residentCandidateExact1990(fixture.half,fixture.width,fixture.height,fixture.noise,fixture.shadows,
                        fixture.evidence,fixture.runtime,0);
                    check(cancellation);
                    if(actual==null){status="モデルGPU起動時検証を完了できません";return false;}
                    if(!StrongNoise1958.sameModel1961(expected,actual)){
                        String reason="model_case="+id+" "+StrongNoise1958.modelDifference1991(expected,actual);status="モデルGPU画質不一致: "+reason;
                        GpuCertificates1990.reject(SUITE,reason);
                        try{CameraTrace1965.event("model_preflight_mismatch1990",0,reason);}catch(Throwable optional){}
                        return false;
                    }
                    cases++;for(int[] map:StrongNoise1958.gpuMaps1960(actual))pixels+=map.length;
                }finally{if(actual!=null)StrongNoise1958.discardResident1961(actual);gpu+=Math.max(1L,System.nanoTime()-start);}
            }
            check(cancellation);boolean saved=GpuCertificates1990.passed(SUITE,cases,pixels,cpu,gpu);
            status=saved?"モデルGPU32例・全マップと解析値のビットが一致":"モデルGPU検証記録を保存できません";
            return saved;
        }catch(CancellationException cancelled){status="モデルGPU起動時検証中断";throw cancelled;}
        catch(RuntimeException unavailable){status="モデルGPU起動時検証を完了できません";return false;}
        catch(LinkageError unavailable){status="モデルGPU処理を利用できません";return false;}
        catch(OutOfMemoryError unavailable){status="モデルGPU起動時検証の作業メモリ不足";return false;}
    }
    static final class Fixture {
        final int width,height,noise;final boolean shadows;final int[] half;final float[] evidence,runtime;
        Fixture(int w,int h,int n,boolean s,int[] p,float[] e,float[] r){width=w;height=h;noise=n;shadows=s;half=p;evidence=e;runtime=r;}
    }
    static Fixture fixture(int id){
        if(id<0||id>=CASES)throw new IllegalArgumentException("model startup case");
        int width=65+(id%3)*32,height=67+(id%5)*2;
        if(id>=24&&id<30){int[][] edges={{1,1},{1,9},{9,1},{3,5},{16,16},{33,65}};width=edges[id-24][0];height=edges[id-24][1];}
        if(id==30){width=3060;height=74;}if(id==31){width=4080;height=66;}
        int noise=id>=30?4:1+(id&3);boolean shadows=id>=30||((id>>>2)&1)!=0;
        int hw=(width+1)/2,hh=(height+1)/2;int[] half=new int[hw*hh];long random=0x19904d4fL+id;
        for(int y=0;y<hh;y++)for(int x=0;x<hw;x++){
            random=(random*1664525L+1013904223L)&0xffffffffL;int jitter=((int)(random>>>24)&31)-16;
            int pattern=(id>>>3)%3,base=pattern==0?48:pattern==1?(x<hw/2?60:192):((x*17+y*31)&255);
            int r=clamp(base+jitter),g=clamp(base-jitter/2),b=clamp(base+jitter/3);
            int alpha=(id%5==4&&(x+2*y)%13==0)?0:255;half[y*hw+x]=(alpha<<24)|(r<<16)|(g<<8)|b;
        }
        float[] evidence=new float[16];for(int i=0;i<16;i++)evidence[i]=new float[]{0f,.39f,.4f,.6f,1.2f,3.7f,12f,24f}[(id+i)&7];
        int regions=((width+63)/64)*((height+63)/64);float[] runtime=new float[16+regions*3];System.arraycopy(evidence,0,runtime,0,16);
        for(int i=0;i<regions;i++){runtime[16+i*3]=.6f+(i%7);runtime[17+i*3]=1.2f+(i%5);runtime[18+i*3]=(i%5)*.25f;}
        return new Fixture(width,height,noise,shadows,half,evidence,runtime);
    }
    static Fixture speedFixture1994(int id){
        if(id<0||id>1)throw new IllegalArgumentException("model layout speed case");
        int width=id==0?3060:4080,height=258,hw=(width+1)/2,hh=(height+1)/2;
        int[] half=new int[hw*hh];long random=0x19944d4fL+id;
        for(int y=0;y<hh;y++)for(int x=0;x<hw;x++){
            random=(random*1664525L+1013904223L)&0xffffffffL;int jitter=((int)(random>>>24)&31)-16;
            int base=x<hw/2?48:176;int r=clamp(base+jitter),g=clamp(base-jitter/2),b=clamp(base+jitter/3);
            half[y*hw+x]=0xff000000|(r<<16)|(g<<8)|b;
        }
        float[] evidence=new float[16];for(int i=0;i<16;i++)evidence[i]=new float[]{.4f,.6f,1.2f,3.7f,12f,24f,3.7f,1.2f}[i&7];
        int regions=((width+63)/64)*((height+63)/64);float[] runtime=new float[16+regions*3];System.arraycopy(evidence,0,runtime,0,16);
        for(int i=0;i<regions;i++){runtime[16+i*3]=.6f+(i%7);runtime[17+i*3]=1.2f+(i%5);runtime[18+i*3]=(i%5)*.25f;}
        return new Fixture(width,height,4,true,half,evidence,runtime);
    }
    private static int clamp(int value){return Math.max(0,Math.min(255,value));}
}
