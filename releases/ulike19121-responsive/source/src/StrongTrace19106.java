package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** Bounded, after-save Java/GPU numerical observation. Neither the trace nor
 * its summary is qualification evidence. The native route is checked only by
 * the preceding fixed-row output comparison; these intermediates are Java. */
public final class StrongTrace19106 {
    static final int FIELDS=24,WORDS=4096,PRESENCE=2048;
    private StrongTrace19106(){}

    private static boolean eligible19106(long[] v){
        return ResidentStages19104.validFixed19105(v)&&v[0]==1&&v[2]==1&&v[3]==0&&v[4]>0&&v[5]>0&&
            v[10]==1&&v[11]==1&&v[6]>0&&v[6]<=v[12]&&v[12]==v[13];
    }

    public static void run(int[] input,int[] u,StrongNoise1958.Model model,int[] policy,
            int[] cpuRow,int[] javaRow,int[] gpuRow,long[] fixed){
        if(!eligible19106(fixed))return;
        long[] result=new long[FIELDS];result[0]=1;result[1]=3;result[2]=fixed[6];result[3]=fixed[1];
        try{
            ResidentStages19104.checkFixed19105();
            int width=(int)fixed[13],x=(int)fixed[6]-1;
            if(u==null||u.length!=32||model==null||input==null||policy==null||cpuRow==null||javaRow==null||gpuRow==null||
                u[0]!=width||u[2]+u[6]!=fixed[1]-1||cpuRow.length!=width||javaRow.length!=width||gpuRow.length!=width||
                !Arrays.equals(cpuRow,javaRow)||cpuRow[x]==gpuRow[x]||
                (cpuRow[x]&0xffffffffL)!=fixed[7]||(javaRow[x]&0xffffffffL)!=fixed[8]||(gpuRow[x]&0xffffffffL)!=fixed[9])return;
            long scratch=StrongNoise1958.workspaceBytes(width,1)+2L*WORDS*4+131072L;
            if(!GpuNoise1960.workspaceFits(scratch)){result[1]=5;return;}
            int[][] maps=StrongNoise1958.gpuMaps1960(model);float[] evidence=StrongNoise1958.gpuEvidence1960(model);
            byte[] frozen=StrongInputs19105.signature(input,policy,u,maps,evidence);
            int[] java=StrongNoise1958.diagnosticTrace19106(input,u,model,policy,x);
            ResidentStages19104.checkFixed19105();
            if(!shape(java))return;
            if(present(java,93)){result[12]=word(java,93);result[14]=java[93]==javaRow[x]?1:2;}
            copyFinal(result,java,true);
            GpuStrong1960.Trace19106 gpu=GpuStrong1960.trace19106(input,evidence,maps,policy,u,x);
            ResidentStages19104.checkFixed19105();
            if(gpu==null||gpu.pixels==null||gpu.pixels.length!=width||!shape(gpu.words))return;
            if(present(gpu.words,93))result[13]=word(gpu.words,93);
            int gpuReproduction=!Arrays.equals(gpu.pixels,gpuRow)?2:!present(gpu.words,93)?0:gpu.words[93]==gpu.pixels[x]?1:2;
            result[14]|=(long)gpuReproduction<<2;
            copyFinal(result,gpu.words,false);
            if(!Arrays.equals(frozen,StrongInputs19105.signature(input,policy,u,maps,evidence)))return;
            if((result[14]&3)==0||((result[14]>>>2)&3)==0)return;
            if(result[14]!=5){result[1]=6;return;}
            if(result[15]!=3)return;
            compare(result,java,gpu.words);
        }catch(CancellationException cancelled){result[1]=4;throw cancelled;}
        catch(OutOfMemoryError unavailable){result[1]=5;}
        catch(Throwable unavailable){result[1]=3;}
        finally{ResidentStages19104.traceReport19106(result);}
    }

    private static boolean shape(int[] v){if(v==null||v.length!=WORDS)return false;for(int i=PRESENCE;i<WORDS;i++)if(v[i]!=0&&v[i]!=1)return false;return true;}
    private static boolean present(int[] v,int id){return v[PRESENCE+id]==1;}
    private static long word(int[] v,int id){return v[id]&0xffffffffL;}
    private static void copyFinal(long[] r,int[] v,boolean java){
        if(!present(v,90)||!present(v,91)||!present(v,92))return;
        r[15]|=java?1:2;int at=java?16:19;for(int i=0;i<3;i++)r[at+i]=word(v,90+i);
    }
    /** First differing observed checkpoint along the output calculation. Nested
     * helper points are examined only when their returned value differs. This
     * avoids blaming harmless differences in rejected NLM candidate traversal. */
    private static void compare(long[] r,int[] a,int[] b){
        for(int id=0;id<PRESENCE;id++)if(present(a,id)||present(b,id)){r[23]++;if(present(a,id)&&present(b,id))r[22]++;}
        for(int id=0;id<=93;id++){
            if(id==11||id==12)continue; // confidence mask is disabled in this experiment
            if(id>=32&&id<=36&&present(a,31)&&present(b,31)&&a[31]==0&&b[31]==0)continue;
            if(!present(a,id)||!present(b,id)){r[4]=id+1L;r[1]=3;return;}
            if(a[id]==b[id])continue;
            int observed=nestedDifference(a,b,id);r[4]=observed+1L;r[5]=word(a,observed);r[6]=word(b,observed);
            operands(r,a,b,observed);r[1]=1;return;
        }
        r[1]=2;
    }
    private static int difference(int[] a,int[] b,int from,int count){for(int id=from;id<from+count;id++)if(present(a,id)&&present(b,id)&&a[id]!=b[id])return id;return -1;}
    private static int nestedDifference(int[] a,int[] b,int id){
        int found=-1;
        if(id>=6&&id<=8)found=difference(a,b,1472+8*(id-6),7);
        else if(id==9||id==10)found=difference(a,b,1408,23);
        else if(id>=16&&id<=21){
            int[] order={0,1,2,3,4,13,14,15,5,6,7,10,12,8,9,11};
            outer:for(int n=0;n<25;n++)for(int offset:order){int at=128+16*n+offset;if(present(a,at)&&present(b,at)&&a[at]!=b[at]){found=at;break outer;}}
        }else if(id>=32&&id<=34){
            for(int n=0;n<8&&found<0;n++){
                int at=544+96*n;
                found=difference(a,b,at,5); // geometry, noise, and accepted/rejected branch
                if(found>=0||!present(a,at+4)||!present(b,at+4)||a[at+4]!=1||b[at+4]!=1)continue;
                found=difference(a,b,at+8,60);
                if(found<0)found=difference(a,b,at+6,2);
                if(found<0)found=difference(a,b,at+5,1);
                if(found<0)found=difference(a,b,at+68,4);
            }
        }else if(id>=48&&id<90){int k=(id-48)/14,offset=(id-48)%14;if(offset<4)found=difference(a,b,1520+32*k+8*offset,7);}
        return found<0?id:found;
    }
    /** Operand pairs are emitted only for a directly observed binary add/sub.
     * Compound expressions are left unspecified, not reconstructed as proof. */
    private static void operands(long[] r,int[] a,int[] b,int id){
        int left=-1,right=-1;
        if(id==45){left=42;right=43;}else if(id==47){left=42;right=44;}
        if(left>=0&&present(a,left)&&present(a,right)&&present(b,left)&&present(b,right)){
            r[7]=1;r[8]=word(a,left);r[9]=word(a,right);r[10]=word(b,left);r[11]=word(b,right);
        }
    }

    public static boolean valid(long[] v){
        if(v==null||v.length!=FIELDS)return false;boolean zero=true;for(long n:v){if(n<0)return false;if(n!=0)zero=false;}if(zero)return true;
        if(v[0]!=1||v[1]<1||v[1]>6||v[2]<1||v[2]>8192||v[3]<1||v[3]>Integer.MAX_VALUE||
            v[4]>2048||(v[4]>0&&!knownCheckpoint((int)v[4]-1))||v[7]>1||v[14]>10||(v[14]&3)==3||((v[14]>>>2)&3)==3||v[15]>3||v[22]>v[23]||v[23]>2048)return false;
        for(int i:new int[]{5,6,8,9,10,11,12,13,16,17,18,19,20,21})if(v[i]>0xffffffffL)return false;
        if(v[7]==0&&(v[8]!=0||v[9]!=0||v[10]!=0||v[11]!=0))return false;
        if((v[15]&1)==0&&(v[16]!=0||v[17]!=0||v[18]!=0)||(v[15]&2)==0&&(v[19]!=0||v[20]!=0||v[21]!=0))return false;
        if(v[1]==1&&(v[4]==0||v[5]==v[6]||v[14]!=5||v[15]!=3||v[22]==0))return false;
        if(v[1]==2&&(v[4]!=0||v[5]!=0||v[6]!=0||v[14]!=5||v[15]!=3||v[22]==0))return false;
        if(v[4]==0&&(v[5]!=0||v[6]!=0||v[7]!=0)||v[1]!=1&&v[7]!=0)return false;
        if((v[14]&3)==0&&v[12]!=0||((v[14]>>>2)&3)==0&&v[13]!=0)return false;
        return v[1]!=6||(v[14]&3)==2||((v[14]>>>2)&3)==2;
    }
    private static boolean knownCheckpoint(int id){
        return id>=0&&id<=93||id>=128&&id<528||id>=544&&id<1312&&(id-544)%96<72||
            id>=1408&&id<=1430||id>=1472&&id<1496&&(id-1472)%8<7||id>=1520&&id<1616&&(id-1520)%8<7;
    }
    public static void appendSummary(StringBuilder text,long[] v){
        if(!valid(v)||v[0]==0)return;
        text.append(" / Strong中間値（固定1画素・Java基準対GPU・認定に不使用）: ");
        text.append(v[1]==1?"差を観測":v[1]==2?"記録した比較点に差なし":v[1]==4?"中断・時間上限":v[1]==5?"メモリ予算不足":v[1]==6?"計測経路で元の出力を再現せず":"未完了");
        text.append(" (x,y)=(").append(v[2]-1).append(',').append(v[3]-1).append(')');
        if(v[1]==1){int id=(int)v[4]-1;text.append(" / 最初に差を観測した比較点: ").append(label(id)).append(" [").append(id).append(']');
            text.append(" / 生ビット Java 0x").append(hex(v[5])).append("・GPU 0x").append(hex(v[6]));
            if(v[7]!=0)text.append(" / この演算の入力生ビット Java=(0x").append(hex(v[8])).append(",0x").append(hex(v[9])).append(")・GPU=(0x").append(hex(v[10])).append(",0x").append(hex(v[11])).append(')');
        }else if(v[1]==3&&v[4]>0)text.append(" / 比較点の記録不足: ").append(label((int)v[4]-1));
        text.append(" / 計測付き出力の再現: Java ").append(reproduction((int)(v[14]&3))).append("・GPU行 ").append(reproduction((int)((v[14]>>>2)&3)));
        if(v[15]==3)text.append(" / 整数化前RGB生ビット Java=(0x").append(hex(v[16])).append(",0x").append(hex(v[17])).append(",0x").append(hex(v[18])).append(")・GPU=(0x").append(hex(v[19])).append(",0x").append(hex(v[20])).append(",0x").append(hex(v[21])).append(')');
        if(v[23]>0)text.append(" / 両経路で記録あり ").append(v[22]).append('/').append(v[23]).append("点");
        text.append("（nativeは先行する行出力比較のみ。記録点間の未観測演算・元区間全体の入力一致は未証明）");
    }
    private static String reproduction(int value){return value==1?"一致":value==2?"不一致":"未観測";}
    private static String hex(long n){String s=Long.toHexString(n&0xffffffffL);return "00000000".substring(s.length())+s;}
    static String label(int id){
        if(id>=128&&id<528){int n=(id-128)/16,o=(id-128)%16;String[] names={"入力ARGB","Y","R残差","B残差","空間重み","Y重み","色重み","Y累積","R残差累積","B残差累積","Y重み累積","色重み累積","分散累積","Y差","R残差差","B残差差"};return "局所近傍("+(n%5-2)+","+(n/5-2)+")・"+names[o];}
        if(id>=544&&id<1312){int n=(id-544)/96,o=(id-544)%96;if(o>=8&&o<68)return "非局所候補"+n+"・標本"+((o-8)/12)+"・"+new String[]{"基準Y","基準R残差","基準B残差","候補Y","候補R残差","候補B残差","標本距離","距離累積","Y累積","R残差累積","B残差累積","透過判定"}[(o-8)%12];return "非局所候補"+n+"・"+(o<8?new String[]{"x","y","分散","h2","採否","重み","補正距離","z"}[o]:o>=68&&o<72?new String[]{"総重み","Y累積","R残差累積","B残差累積"}[o-68]:"比較点"+o);}
        if(id>=1408&&id<=1430)return "保護ガイド・"+new String[]{"左Y","右Y","上Y","下Y","左R残差","右R残差","上R残差","下R残差","左B残差","右B残差","上B残差","下B残差","中央Y","中央R残差","中央B残差","Y曲率","色曲率","補正前Y境界","補正前色境界","Y境界","色境界","Y出力","色出力"}[id-1408];
        if(id>=1472&&id<1496)return "領域推定"+((id-1472)/8)+"・"+new String[]{"fx","fy","a","b","c","d","補間結果","予約"}[(id-1472)%8];
        if(id>=1520&&id<1616)return "モデル補間"+((id-1520)/32)+"・成分"+((id-1520)%32/8)+"・"+new String[]{"fx","fy","a","b","c","d","補間結果","予約"}[(id-1520)%8];
        if(id>=48&&id<90)return "多段合成"+((id-48)/14)+"・"+new String[]{"モデルR","モデルG","モデルB","モデルY","ゲート","合成率","Y","R残差","B残差","Y合成率","色合成率","R補正累積","G補正累積","B補正累積"}[(id-48)%14];
        String[] names={"入力ARGB","保護予算","細部保護","基準Y","基準R残差","基準B残差","領域Y","領域色","領域模様","保護ガイドY","保護ガイド色","平滑信頼度","平滑整数値","暗部係数","Y閾値","色閾値","局所Y累積","局所R残差累積","局所B残差累積","局所Y重み累積","局所色重み累積","局所分散累積","強度","質感係数","信頼度","残差係数","局所Y合成率","局所色合成率","局所目標Y","局所目標R残差","局所目標B残差","非局所実行判定","非局所Y","非局所R残差","非局所B残差","非局所Y合成率","非局所色合成率","合成後目標Y","合成後目標R残差","合成後目標B残差","更新後Y合成率","更新後色合成率","Y差分","R残差差分","B残差差分","R補正","G補正","B補正"};
        if(id>=0&&id<names.length)return names[id];if(id>=90&&id<=93)return new String[]{"整数化前R","整数化前G","整数化前B","最終ARGB"}[id-90];return "比較点"+id;
    }
}
