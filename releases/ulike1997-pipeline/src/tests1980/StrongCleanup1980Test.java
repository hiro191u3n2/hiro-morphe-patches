package com.hiro.ulike;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.Random;

/** Independent published .79 and current Strong sources execute this same
 * driver in separate JVMs. Compare every model word and every output pixel. */
public final class StrongCleanup1980Test {
    static int assertions,pixels,floatBits,cases;
    static void ints(DataOutputStream out,int[] data)throws Exception{out.writeInt(data.length);for(int value:data){out.writeInt(value);pixels++;}}
    public static void main(String[] args)throws Exception {
        boolean old=Boolean.parseBoolean(args[1]),found=false;
        for(Method m:StrongNoise1958.class.getDeclaredMethods())if(m.getName().equals("prepareScale"))found=true;
        if(found!=old)throw new AssertionError("unreachable old per-scale entry inventory");assertions++;
        Method down=StrongNoise1958.class.getDeclaredMethod("downsample",int[].class,int.class,int.class,boolean.class);down.setAccessible(true);
        Random random=new Random(1980);
        try(DataOutputStream out=new DataOutputStream(new FileOutputStream(args[0]))) {
            for(final int width:new int[]{1,2,3,7,8,16,33,64})for(final int height:new int[]{1,2,5,16,35})for(int pattern=0;pattern<2;pattern++) {
                final int[] input=new int[width*height];
                for(int i=0;i<input.length;i++){int y=pattern==0?96+random.nextInt(64):(i*37)&255;input[i]=(pattern==1&&i%17==0?0:0xff000000)|y<<16|((y+7)&255)<<8|((y+19)&255);}
                int[] reduced=(int[])down.invoke(null,input,width,height,false);ints(out,reduced);
                StrongNoise1958.Model model=StrongNoise1958.prepareJava(new StrongNoise1958.Patches(){public void read(int[] a,int x,int y,int w,int h){for(int row=0;row<h;row++)System.arraycopy(input,(y+row)*width+x,a,row*w,w);}},width,height,2,pattern!=0);
                for(int[] map:StrongNoise1958.gpuMaps1960(model))ints(out,map);
                for(float value:StrongNoise1958.gpuEvidence1960(model)){out.writeInt(Float.floatToRawIntBits(value));floatBits++;}
                int[] output=new int[input.length];
                StrongNoise1958.processJavaRange(input,output,width,height,0,height,0,height,0,2,pattern!=0,model,null);ints(out,output);
                out.writeInt(model.nonlocalCandidates);cases++;assertions++;
            }
        }
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"pixels\":"+pixels+",\"float_bits\":"+floatBits+",\"physical_android_tested\":false}");
    }
}
