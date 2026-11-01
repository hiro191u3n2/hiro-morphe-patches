package com.hiro.ulike;

import android.graphics.Bitmap;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** A single-row controlled experiment AFTER the independent photo comparator
 * has already rejected its baseline. The owning CPU replay has not started its
 * Strong workers yet: its source bitmap and freshly built model are read-only
 * for this entire call. No photo, model or array is retained after return.
 *
 * This deliberately does not claim to reproduce the original strip layout or
 * prove the old baseline's inputs identical. It determines whether a numerical
 * difference is reproduced when all three backends share ONE materialized
 * policy, source/halo, model and set of uniforms. It never changes admission. */
public final class StrongInputs19105 {
    static final int FIELDS=14,MAX_WIDTH=8192;
    private static final int PROFILE=4,VARIANT=0;
    private StrongInputs19105(){}

    public static void run(Bitmap source,QualityPixels1932.Plan plan,StrongNoise1958.Model model,int level,boolean shadows){
        Object[] target=ResidentStages19104.fixedTarget19105();
        if(target==null)return; // No capture-time reads, allocations, GPU or CPU work.
        long[] result=new long[FIELDS];result[0]=4;
        try{
            int row=((Integer)target[0]).intValue();int[] oldGpu=(int[])target[1];
            int width=source.getWidth(),height=source.getHeight();
            result[1]=row+1L;result[13]=width;
            ResidentStages19104.checkFixed19105();
            if(width<1||width>MAX_WIDTH||height<1||row<0||row>=height||oldGpu.length!=width||
                model==null||model.width!=width||model.height!=height||level<1||level>4) return;
            result[13]=width;
            int first=Math.max(0,row-StrongNoise1958.HALO),last=Math.min(height,row+StrongNoise1958.HALO+1);
            int rows=last-first,begin=row-first,count=Math.multiplyExact(width,rows);
            int[][] maps=StrongNoise1958.gpuMaps1960(model);float[] evidence=StrongNoise1958.gpuEvidence1960(model);
            long modelBytes=4L*evidence.length;for(int[] map:maps)modelBytes+=4L*map.length;
            // Three source-sized Java buffers, row/policy/hash scratch, native
            // CPU transient copies, and a conservative cold GPU model peak.
            // The actual GPU owner independently reserves and rechecks every
            // upload/range/readback through ephemeral1973 before submission.
            long extra=12L*count+64L*width+131072L+StrongNoise1958.workspaceBytes(width,1)+2L*modelBytes;
            if(!GpuNoise1960.workspaceFits(extra)){result[0]=2;return;}
            int[] input=new int[count],cpu=new int[count],java=new int[count],policy=new int[width*2];
            source.getPixels(input,0,width,0,first,width,rows);
            new GpuPolicy1960.Protection(plan,true).freezePolicy1981(width,1,row,policy,false);
            int[] u=new int[32];u[0]=width;u[1]=rows;u[2]=begin;u[3]=begin+1;u[4]=0;u[5]=rows;u[6]=first;
            u[7]=height;u[8]=level;u[9]=shadows?1:0;u[10]=3;u[11]=1;u[12]=0;
            if(plan!=null){u[18]=plan.beautyQ8;u[19]=plan.shadowBudgetQ8;}
            byte[] frozen=signature(input,policy,u,maps,evidence);
            ResidentStages19104.checkFixed19105();
            result[2]=StrongNoise1958.diagnosticCpu19105(input,cpu,u,model,policy,false);
            int[] cpuRow=Arrays.copyOfRange(cpu,begin*width,(begin+1)*width);
            result[11]=ResidentStages19104.fixedCpuMatches19105(cpuRow)?1:2;
            ResidentStages19104.checkFixed19105();
            StrongNoise1958.diagnosticCpu19105(input,java,u,model,policy,true);
            ResidentStages19104.checkFixed19105();
            GpuStrong1960.Read1971 read=GpuStrong1960.ephemeral1973(input,evidence,maps,policy,u,null,VARIANT,PROFILE);
            ResidentStages19104.checkFixed19105();
            if(read==null||read.failure!=null||read.result==null||read.result.pixels==null||read.result.pixels.length!=width)return;
            if(!Arrays.equals(frozen,signature(input,policy,u,maps,evidence)))return;
            int[] gpu=read.result.pixels;result[10]=Arrays.equals(oldGpu,gpu)?1:2;
            for(int x=0;x<width;x++){
                int a=cpuRow[x],b=java[begin*width+x],c=gpu[x];
                if(a!=b)result[3]++;if(c!=a)result[4]++;if(c!=b)result[5]++;
                if(result[6]==0&&(a!=b||c!=a)){result[6]=x+1L;result[7]=a&0xffffffffL;result[8]=b&0xffffffffL;result[9]=c&0xffffffffL;}
            }
            result[12]=width;result[0]=1;
            if(traceEligible19106(result))
                StrongTrace19106.run(input,u,model,policy,cpuRow,
                    Arrays.copyOfRange(java,begin*width,(begin+1)*width),gpu,result);
        }catch(CancellationException cancelled){if(result[0]!=1)result[0]=3;throw cancelled;}
        catch(OutOfMemoryError unavailable){if(result[0]!=1)result[0]=2;}
        catch(Throwable unavailable){if(result[0]!=1)result[0]=4;}
        finally{ResidentStages19104.fixedReport19105(result);}
    }

    /** A completed, reproduced native/Java-vs-GPU difference is required before
     * tracing. This is diagnostic eligibility, never GPU output admission. */
    private static boolean traceEligible19106(long[] v){
        return ResidentStages19104.validFixed19105(v)&&v[0]==1&&v[2]==1&&
            v[3]==0&&v[4]>0&&v[5]>0&&v[6]>0&&v[6]<=v[12]&&
            v[10]==1&&v[11]==1&&v[12]==v[13];
    }

    /** Hash guards unexpected mutation of our shared fixed inputs. These hashes
     * are diagnostic only and are never used to certify pixels or a GPU route. */
    static byte[] signature(int[] input,int[] policy,int[] u,int[][] maps,float[] evidence)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] bytes=new byte[4096];
        update(digest,bytes,input);update(digest,bytes,policy);update(digest,bytes,u);
        for(int[] map:maps)update(digest,bytes,map);
        int[] words=new int[Math.min(1024,Math.max(1,evidence.length))];
        for(int from=0;from<evidence.length;from+=words.length){
            ResidentStages19104.checkFixed19105();int n=Math.min(words.length,evidence.length-from);
            for(int i=0;i<n;i++)words[i]=Float.floatToRawIntBits(evidence[from+i]);
            update(digest,bytes,words,n);
        }
        return digest.digest();
    }
    private static void update(MessageDigest digest,byte[] bytes,int[] value){update(digest,bytes,value,value.length);}
    private static void update(MessageDigest digest,byte[] bytes,int[] value,int length){
        for(int from=0;from<length;from+=bytes.length/4){
            ResidentStages19104.checkFixed19105();int n=Math.min(bytes.length/4,length-from);
            for(int i=0;i<n;i++){int x=value[from+i],at=i*4;bytes[at]=(byte)x;bytes[at+1]=(byte)(x>>>8);bytes[at+2]=(byte)(x>>>16);bytes[at+3]=(byte)(x>>>24);}
            digest.update(bytes,0,n*4);
        }
    }

    public static boolean valid(long[] v){
        return ResidentStages19104.validFixed19105(v);
    }
    public static void appendSummary(StringBuilder text,long[] v){
        if(!valid(v)||v[0]==0)return;
        text.append(" / 固定入力のStrong再比較（1行・標準profile 4/variant 0・認定に不使用）: ");
        if(v[0]!=1){text.append(v[0]==2?"メモリ予算不足":v[0]==3?"中断・時間上限":"未完了");return;}
        text.append("完了・行 ").append(v[1]-1).append(" / CPU経路: ");
        text.append(v[2]==1?"native":v[2]==2?"Java":v[2]==3?"native未使用・Java代替":v[2]==4?"native未完了・Java代替":"nativeリンク不可・Java代替");
        text.append(" / 不一致画素 CPU対Java ").append(v[3]).append('/').append(v[12])
            .append("・GPU対CPU ").append(v[4]).append('/').append(v[12])
            .append("・GPU対Java ").append(v[5]).append('/').append(v[12]);
        text.append(" / 元CPU行の署名: ").append(v[11]==1?"差未検出":"差あり")
            .append(" / 元GPU行との厳密比較: ").append(v[10]==1?"一致":"不一致");
        if(v[6]>0)text.append(" / 固定入力で最初の差 (x,y)=(").append(v[6]-1).append(',').append(v[1]-1)
            .append(")・ARGB CPU 0x").append(hex(v[7])).append("・Java 0x").append(hex(v[8])).append("・GPU 0x").append(hex(v[9]));
        text.append("（周辺最大18行・実ポリシー・モデル・uniformを共用。平滑マスク書込みなしの1行比較で、元区間全入力の一致証明ではありません）");
    }
    private static String hex(long value){String s=Long.toHexString(value&0xffffffffL);return "00000000".substring(s.length())+s;}
}
