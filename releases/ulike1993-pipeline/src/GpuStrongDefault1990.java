package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** Fixed production Strong route with versioned, device-local preflight.
 * This is finite corpus evidence for the tested implementation, not a claim
 * that a photograph was compared, or a timing prediction for an unseen size.
 * The CPU protection policy is uploaded unchanged; no mask inference occurs.
 * The old per-shape exact rejection remains authoritative for this program.
 */
public final class GpuStrongDefault1990 {
    private GpuStrongDefault1990() {}
    static final int PROFILE=4,VARIANT=0;
    static final String SUITE="strong1990-direct-ieee39-aligned-confidence-corpus68-v2";
    static final int CASES=68;
    private static volatile String status="起動時の画質検証待ち";
    private static final float[] SIGMA={0f,.39f,.40f,.59f,.60f,1.2f,3.7f,12f,24f};

    /** The same bounded integer geometry accepted by the CPU implementation.
     * This does not certify a new per-photo shape or relax a pixel comparison. */
    static boolean owns(int[] u) {
        if(u==null||u.length<20||u[10]!=3||u[0]<1||u[0]>8192||u[1]<1||u[7]<1||u[7]>8192||
                u[8]<1||u[8]>4||(u[9]!=0&&u[9]!=1)||(u[11]!=0&&u[11]!=1)||(u[12]!=0&&u[12]!=1)||
                u[4]<0||u[5]>u[1]||u[4]>=u[5]||u[2]<u[4]||u[3]>u[5]||u[3]<=u[2]||
                u[3]-u[2]>256||(long)u[0]*u[1]>Integer.MAX_VALUE||(long)u[6]+u[4]<0||
                (long)u[6]+u[5]>u[7])return false;
        int first=u[6]+u[2],last=u[6]+u[3];
        // Production native CPU and Java agree on this sparse-confidence
        // layout. Special misaligned partial cells retain the old CPU path;
        // a Java-only edge oracle must not certify a different native result.
        if(u[12]!=0&&((first&3)!=0||((u[3]-u[2])&3)!=0&&last!=u[7]))return false;
        return !(first>0&&u[2]-u[4]<Math.min(StrongNoise1958.HALO,first)||
                last<u[7]&&u[5]-u[3]<Math.min(StrongNoise1958.HALO,u[7]-last));
    }
    public static boolean ready() {
        return GpuCertificates1990.restore(SUITE)!=null;
    }
    static boolean accepts(int[] u) {
        return owns(u)&&ready()&&!GpuQualification1961.exactRejected(GpuStrong1960.profileKey1973(u,PROFILE));
    }
    public static String summary(){return status;}
    private static void check(GpuQualification1961.Cancellation cancellation) {
        if(Thread.currentThread().isInterrupted()||cancellation.cancelled())throw new CancellationException("Strong startup cancelled");
    }
    /** Called only by the startup worker, before first-shot readiness is set. */
    public static boolean qualify(GpuQualification1961.Cancellation cancellation) {
        if(cancellation==null)return false;
        check(cancellation);
        if(ready()){status="端末・処理版が一致する起動時検証記録あり";return true;}
        if(GpuCertificates1990.rejected(SUITE)){status="起動時検証の画質不一致記録あり";return false;}
        if(!GpuNoise1960.supports(GpuStrong1960.program1973(PROFILE,VARIANT))){status="対象GPU処理を利用できません";return false;}
        long cpu=0,gpu=0,pixels=0;int cases=0;
        try {
            for(int id=0;id<CASES;id++) {
                check(cancellation);status="起動時の画質検証 "+id+"/"+CASES;
                Fixture fixture=fixture(id);
                int[] expected=new int[fixture.source.length];
                int confidenceCount=fixture.u[12]==0?0:((fixture.u[0]+3)/4)*((fixture.u[3]-fixture.u[2]+3)/4);
                int[] expectedConfidence=confidenceCount==0?null:new int[confidenceCount];
                long start=System.nanoTime();
                boolean ok=StrongNoise1958.gpuOracleSnapshot1961(fixture.source,expected,fixture.u[0],fixture.u[1],fixture.u[2],fixture.u[3],
                    fixture.u[4],fixture.u[5],fixture.u[6],fixture.u[8],fixture.u[9]!=0,fixture.model,fixture.policy,expectedConfidence);
                cpu+=Math.max(1L,System.nanoTime()-start);check(cancellation);
                if(!ok){status="起動時検証のCPU比較結果を取得できません";return false;}
                start=System.nanoTime();
                GpuStrong1960.Read1971 actual=GpuStrong1960.ephemeral1973(fixture.source,StrongNoise1958.gpuEvidence1960(fixture.model),
                    StrongNoise1958.gpuMaps1960(fixture.model),fixture.policy,fixture.u,null,VARIANT,PROFILE);
                gpu+=Math.max(1L,System.nanoTime()-start);check(cancellation);
                if(actual==null||actual.failure!=null||actual.result==null){status="起動時GPU検証を完了できません: "+(actual==null?"no_result":actual.failure);return false;}
                int count=fixture.u[0]*(fixture.u[3]-fixture.u[2]),offset=fixture.u[0]*fixture.u[2];
                String mismatch=difference(actual.result.pixels,actual.result.confidence,expected,expectedConfidence,offset,count);
                if(mismatch!=null){
                    String detail="case="+id+" "+mismatch;status="起動時GPU画質不一致: "+detail;
                    GpuCertificates1990.reject(SUITE,detail);
                    try{CameraTrace1965.event("strong_preflight_mismatch1990",0,detail);}catch(Throwable optional){}
                    return false;
                }
                cases++;pixels+=count;
            }
            check(cancellation);
            boolean saved=GpuCertificates1990.passed(SUITE,cases,pixels,cpu,gpu);
            status=saved?"起動時検証"+CASES+"例・全画素と信頼度が一致（撮影画像の比較ではありません）":"起動時検証記録を保存できません";
            return saved;
        }catch(CancellationException cancelled){status="起動時検証中断";throw cancelled;}
        catch(RuntimeException unavailable){status="起動時検証を完了できません";return false;}
        catch(LinkageError unavailable){status="起動時検証の処理を利用できません";return false;}
        catch(OutOfMemoryError unavailable){status="起動時検証の作業メモリ不足";return false;}
    }
    static String difference(int[] actual,int[] actualConfidence,int[] expected,int[] expectedConfidence,int offset,int count) {
        if(actual==null||actual.length!=count)return "output_shape";
        for(int i=0;i<count;i++)if(actual[i]!=expected[offset+i])return "argb index="+i+" cpu="+Integer.toHexString(expected[offset+i])+" gpu="+Integer.toHexString(actual[i]);
        if(!Arrays.equals(actualConfidence,expectedConfidence)){
            if(actualConfidence==null||expectedConfidence==null||actualConfidence.length!=expectedConfidence.length)return "confidence_shape";
            for(int i=0;i<actualConfidence.length;i++)if(actualConfidence[i]!=expectedConfidence[i])return "confidence index="+i+" cpu="+expectedConfidence[i]+" gpu="+actualConfidence[i];
        }
        return null;
    }
    static final class Fixture {
        final int[] source,policy,u;final StrongNoise1958.Model model;
        Fixture(int[] source,int[] policy,int[] u,StrongNoise1958.Model model){this.source=source;this.policy=policy;this.u=u;this.model=model;}
    }
    /** Synthetic pixels never originate from or retain a user photograph. */
    static Fixture fixture(int id) {
        if(id<0||id>=CASES)throw new IllegalArgumentException("strong corpus case");
        boolean wide=id>=64&&id<66,partial=id==66,duplicate=id==67;
        int width=partial||duplicate?65:wide?(id==64?3060:4080):new int[]{17,31,32,33,63,65,97,129}[(id>>>3)&7];
        int rows=duplicate?5:partial?65:wide?68:72+(id&7),origin=partial||duplicate?0:wide?1374:(id%3==0?0:64+(id&7)),height=wide?(id==64?4080:3060):origin+rows;
        int begin=duplicate?1:partial?36:wide?18:(id%3==0?0:18),end=duplicate?5:partial?37:wide?50:(id%3==0?rows:rows-18);
        int[] u=new int[32];u[0]=width;u[1]=rows;u[2]=begin;u[3]=end;u[4]=0;u[5]=rows;u[6]=origin;u[7]=height;
        u[8]=id>=64?4:1+(id&3);u[9]=id>=64?1:(id>>>2)&1;u[10]=3;u[11]=id>=64?1:(id>>>3)&1;u[12]=id>=64?1:(id>>>4)&1;
        if(u[12]!=0&&(((origin+begin)&3)!=0||((end-begin)&3)!=0&&origin+end!=height))u[12]=0;
        int[] source=new int[width*rows];long random=0x5eed1990L+id;
        for(int y=0;y<rows;y++)for(int x=0;x<width;x++){
            random=(random*1664525L+1013904223L)&0xffffffffL;int jitter=((int)(random>>>24)&15)-8;
            int mode=(id>>>2)%6,base=mode==0?16:mode==1?128:mode==2?240:mode==3?(x<width/2?32:224):mode==4?((x*17+y*31)&255):(int)(random>>>24);
            int r=clamp(base+jitter),g=clamp(base+(mode==5?37:0)-jitter),b=clamp(base+(mode==4?71:0)+jitter/2);
            int alpha=((id&7)==7&&(x+y)%11==0)?((x+y)&255):255;
            source[y*width+x]=(alpha<<24)|(r<<16)|(g<<8)|b;
        }
        // Exercise these special partial shapes for ARGB only. Their sparse
        // confidence convention is outside the production domain above.
        if(duplicate)Arrays.fill(source,0xff707070);
        int count=width*(end-begin);int[] policy=u[11]==0?null:new int[count*2];
        if(policy!=null)for(int i=0;i<count;i++){policy[i*2]=new int[]{0,1,64,127,128,191,255,256}[(i+id)&7];policy[i*2+1]=((i*37+id*17)%257);}
        if(duplicate)for(int i=0;i<count;i++){policy[i*2]=i/width==end-begin-1?0:256;policy[i*2+1]=0;}
        int[][] maps=new int[3][];
        for(int scale=0;scale<3;scale++){
            int divisor=1<<(scale+1),mw=(width+divisor-1)/divisor,mh=(height+divisor-1)/divisor;maps[scale]=new int[mw*mh];
            for(int i=0;i<maps[scale].length;i++){
                int guide=(i*7+id*13)&255,dr=((i+id)%15)-7,dg=((i*3+id)%15)-7,db=((i*7+id)%15)-7;
                maps[scale][i]=(guide<<24)|((dr&255)<<16)|((dg&255)<<8)|(db&255);
            }
        }
        float[] evidence=new float[64];for(int i=0;i<64;i++)evidence[i]=SIGMA[(i+id)%SIGMA.length];
        int regions=((width+63)/64)*((height+63)/64);float[] runtime=new float[16+regions*3];System.arraycopy(evidence,0,runtime,0,16);
        for(int i=0;i<regions;i++){runtime[16+i*3]=SIGMA[(id+i)%SIGMA.length];runtime[17+i*3]=SIGMA[(id+i+3)%SIGMA.length];runtime[18+i*3]=(i%5)*.25f;}
        if(duplicate)for(int i=0;i<regions;i++){runtime[16+i*3]=12f;runtime[17+i*3]=12f;runtime[18+i*3]=0;}
        return new Fixture(source,policy,u,StrongNoise1958.model1961(width,height,maps[0],maps[1],maps[2],evidence,runtime));
    }
    private static int clamp(int value){return Math.max(0,Math.min(255,value));}
}
