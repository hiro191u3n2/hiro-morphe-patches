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
    public static String summary(){return status;}
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
    private static int clamp(int value){return Math.max(0,Math.min(255,value));}
}
