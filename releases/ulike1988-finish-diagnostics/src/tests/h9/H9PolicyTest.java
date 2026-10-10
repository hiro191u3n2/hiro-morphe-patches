package com.hiro.ulike;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

public final class H9PolicyTest {
    public static void main(String[] args) throws Exception {
        Method general=QualityShadow1932.class.getDeclaredMethod("finishSummary",int[].class,
            int[].class,int[].class,int[].class,int[].class,int.class,int.class,int.class,
            int.class,int.class,boolean.class,QualityPixels1932.Plan.class);
        Method packed=QualityShadow1932.class.getDeclaredMethod("finishSummaryPacked",int[].class,
            int[].class,int[].class,int[].class,int[].class,int.class,int.class,int.class,
            int.class,int.class,boolean.class,QualityPixels1932.Plan.class);
        general.setAccessible(true);packed.setAccessible(true);
        int width=43,rows=31,count=width*rows,checks=0;
        QualityPixels1932.Plan plan=QualityPixels1932.plan(
            new QualityPixels1932.NoiseStats(7,7,128,0,4096),400,10000000L,2,0,4,0,true,true,1)
            .withLocalNoise(null,4);
        Random random=new Random(1951);
        for(int trial=0;trial<30;trial++) {
            int[] input=new int[count],meta=new int[count],policy=new int[count*3],
                narrow=new int[count],summary=new int[count*3];
            for(int i=0;i<count;i++) {
                input[i]=0xff000000|random.nextInt(0x1000000);
                int budget=random.nextInt(257),base=random.nextInt(257),skin=random.nextInt(257);
                policy[i*3]=budget;policy[i*3+1]=base;policy[i*3+2]=skin;
                narrow[i]=budget|(base<<9)|(skin<<18);
                int yc=QualityShadow1932.y(input[i]),tolerance=3+random.nextInt(24);
                meta[i]=Integer.MIN_VALUE|(yc<<18)|(tolerance<<12)|9;
                summary[i*3]=0xff000000|random.nextInt(0x1000000);
                summary[i*3+1]=0xff000000|random.nextInt(0x1000000);
                summary[i*3+2]=Integer.MIN_VALUE|(random.nextInt(256)<<16)|
                    (random.nextInt(256)<<8)|random.nextInt(256);
                if(i%13==0)summary[i*3+2]=0;
            }
            int[] old=input.clone(),next=input.clone();
            general.invoke(null,input,meta,policy,summary,old,0,width,0,count,4,true,plan);
            packed.invoke(null,input,meta,narrow,summary,next,0,width,0,count,4,true,plan);
            if(!Arrays.equals(old,next))throw new AssertionError("policy packing changed pixel at trial "+trial);
            checks+=count;
        }
        System.out.println("Lossless packed-policy CPU finish: "+checks+" pixels");
    }
}
