package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.*;
/** Actual production guard and admission entry points, without a GPU substitute. */
public final class RegionalIngress19107Test {
    static int assertions,generated;
    static void check(boolean pass,String why){assertions++;if(!pass)throw new AssertionError(why);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static float[] evidence(int w,int h){return new float[16+3*((w+63)/64)*((h+63)/64)];}
    static void guards()throws Exception{
        float[] good=evidence(65,129);
        for(int i=0;i<16;i++)good[i]=Float.intBitsToFloat(new int[]{0x80000000,0xbf800000,0x7f800000,0x7fc00001}[i&3]);
        check(GpuStrong1960.regionalEvidence19107(good,65,129),"unrelated first 16 global values are untouched");
        int[] positives={0,1,2,0x007fffff,0x00800000,0x3f000000,0x3f800000,0x7f7fffff};
        for(int bits:positives)for(int at:new int[]{16,good.length-1}){good[at]=Float.intBitsToFloat(bits);check(GpuStrong1960.regionalEvidence19107(good,65,129),"valid positive binary32 including subnormal");good[at]=0f;}
        int[] invalid={0x80000000,0x80000001,0xbf800000,0xff7fffff,0x7f800000,0xff800000,0x7fc00001,0x7f800001,0xffc00001};
        for(int bits:invalid)for(int at:new int[]{16,good.length-1}){good[at]=Float.intBitsToFloat(bits);check(!GpuStrong1960.regionalEvidence19107(good,65,129),"invalid regional binary32 rejected");good[at]=0f;}
        check(!GpuStrong1960.regionalEvidence19107(null,65,129),"null shape rejected");
        check(!GpuStrong1960.regionalEvidence19107(Arrays.copyOf(good,good.length-1),65,129),"short plane rejected");
        check(!GpuStrong1960.regionalEvidence19107(Arrays.copyOf(good,good.length+1),65,129),"long plane rejected");
        for(int n:new int[]{-1,0,8193,Integer.MAX_VALUE}){check(!GpuStrong1960.regionalEvidence19107(good,n,129),"bad width rejected");check(!GpuStrong1960.regionalEvidence19107(good,65,n),"bad height rejected");}
        check(GpuStrong1960.regionalEvidence19107(evidence(8192,8192),8192,8192),"maximum actual geometry accepted");
        // Entry points reject before session open or native availability probes.
        Field loaded=field(GpuNoise1960.class,"loaded");check(loaded.getInt(null)==0,"JNI untouched before ingress probes");
        GpuStrongDefault1990.Fixture fixture=GpuStrongDefault1990.fixture(0);
        float[] modelEvidence=StrongNoise1958.gpuEvidence1960(fixture.model);int[][] maps=StrongNoise1958.gpuMaps1960(fixture.model);
        Class<?> stageType=Class.forName("com.hiro.ulike.GpuStrong1960$Stage");Constructor<?> constructor=stageType.getDeclaredConstructor(StrongNoise1958.Model.class,boolean.class);constructor.setAccessible(true);
        Method initialize=GpuStrong1960.class.getDeclaredMethod("initialize1971",stageType);initialize.setAccessible(true);
        for(int bits:invalid){
            modelEvidence[16]=Float.intBitsToFloat(bits);
            GpuStrong1960.Read1971 result=GpuStrong1960.ephemeral1973(fixture.source,modelEvidence,maps,fixture.policy,fixture.u,null,0,4);
            check(result.result==null&&"regional_input_unsupported19107".equals(result.failure),"ephemeral full Strong refuses invalid domain");
            Object stage=constructor.newInstance(fixture.model,false);
            check(Boolean.FALSE.equals(initialize.invoke(null,stage)),"stage cannot upload invalid regional input");
            check(field(stageType,"unavailable").getBoolean(stage)&&"regional_input_unsupported19107".equals(field(stageType,"unavailableReason1971").get(stage)),"failure is unavailable, not quality rejection");
            check(field(stageType,"session").get(stage)==null&&loaded.getInt(null)==0,"no GPU allocation/dispatch on rejected ingress");
        }
    }
    static void generated()throws Exception{
        for(int i=0;i<GpuStrongDefault1990.CASES;i++){
            GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.fixture(i);
            check(GpuStrong1960.regionalEvidence19107(StrongNoise1958.gpuEvidence1960(f.model),f.model.width,f.model.height),"actual startup corpus regional domain");generated++;
        }
        for(final int[] shape:new int[][]{{1,1},{17,19},{65,67},{129,130}})for(final int pattern:new int[]{0,1,2,3}){
            final int w=shape[0],h=shape[1];final int[] pixels=new int[w*h];
            long random=0x19107;for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                random=(random*1664525L+1013904223L)&0xffffffffL;
                int base=pattern==0?0:pattern==1?255:pattern==2?((x^y)&1)*255:(int)(random>>>24);
                pixels[y*w+x]=0xff000000|(base<<16)|(((base+x*13)&255)<<8)|((base+y*7)&255);
            }
            StrongNoise1958.Patches source=new StrongNoise1958.Patches(){public void read(int[] dest,int x,int y,int width,int height){for(int row=0;row<height;row++)System.arraycopy(pixels,(y+row)*w+x,dest,row*width,width);}};
            StrongNoise1958.Model model=StrongNoise1958.prepareJava(source,w,h,4,true);
            check(model!=null&&GpuStrong1960.regionalEvidence19107(StrongNoise1958.gpuEvidence1960(model),w,h),"actual CPU regional estimation emits supported domain");generated++;
        }
    }
    public static void main(String[] ignored)throws Exception{
        guards();generated();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"generated_models\":"+generated+",\"invalid_binary32_classes\":9}");
    }
}
