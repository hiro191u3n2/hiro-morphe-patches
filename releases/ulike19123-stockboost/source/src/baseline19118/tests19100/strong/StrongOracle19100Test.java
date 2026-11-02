package com.hiro.ulike;

import android.graphics.Bitmap;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Production CPU model maps and complete independent Strong/finish oracles.
 * The driver runs the same complete-image corpus with the old prepareJava
 * selection and the new CPU-native selection, and compares every output byte. */
public final class StrongOracle19100Test {
    static long assertions,words,pixels;static int models,images;
    public static final java.util.concurrent.atomic.AtomicInteger nativeCalls=new java.util.concurrent.atomic.AtomicInteger();
    public static int gpuCalls;
    static final java.util.concurrent.atomic.AtomicBoolean interruptNative=new java.util.concurrent.atomic.AtomicBoolean();
    public static void nativeCall(){nativeCalls.incrementAndGet();if(interruptNative.compareAndSet(true,false))Thread.currentThread().interrupt();}
    static void check(boolean b,String why){assertions++;if(!b)throw new AssertionError(why);}
    static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
    @SuppressWarnings("unchecked")static ThreadLocal<Boolean> cpu()throws Exception{return (ThreadLocal<Boolean>)field(GpuResident1976.class,"CPU").get(null);}
    static final class Source implements StrongNoise1958.Patches {
        final int w,h;final int[] data;int reads,cancelAt=-1;
        Source(int width,int height,int seed){w=width;h=height;data=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int n=(x*1103515245+y*12345+seed*7817),v=80+((n>>>24)&31),r=v,g=v,b=v;
            if(seed%3==1){r=(n>>>16)&255;g=(n>>>8)&255;b=n&255;}
            if(seed%3==2){r+=((x/4+y/4)&1)*25;g+=((x/8+y/7)&1)*12;}
            data[y*w+x]=((seed%5==0&&(x+y)%11==0)?0x70000000:0xff000000)|(r<<16)|(g<<8)|b;
        }}
        public void read(int[] out,int x,int y,int width,int height){reads++;if(reads==cancelAt)Thread.currentThread().interrupt();for(int row=0;row<height;row++)System.arraycopy(data,(y+row)*w+x,out,row*width,width);}
    }
    static void modelCorpus(boolean actualNative)throws Exception {
        int[][] shapes={{1,1},{7,9},{16,17},{63,65},{65,129},{259,263},{513,97},{3060,33}};
        for(int[] shape:shapes)for(int level=1;level<=4;level++)for(boolean shadow:new boolean[]{false,true}){
            Source s=new Source(shape[0],shape[1],models+1);int[] input=s.data.clone();
            StrongNoise1958.Model java=StrongNoise1958.prepareJava(s,s.w,s.h,level,shadow);int reads=s.reads;s.reads=0;
            int before=nativeCalls.get();StrongNoise1958.Model first=StrongNoise1958.prepareCpuOracle19100(s,s.w,s.h,level,shadow);
            check(StrongNoise1958.sameModel1961(java,first),"all model map/evidence bits equal "+models);
            check(s.reads==reads,"same independent source traversal");s.reads=0;
            StrongNoise1958.Model second=StrongNoise1958.prepareCpuOracle19100(s,s.w,s.h,level,shadow);
            check(s.reads==reads&&StrongNoise1958.sameModel1961(first,second),"second independently rebuilt complete model");
            check(first!=second,"no result cache");int[][] a=StrongNoise1958.gpuMaps1960(first),b=StrongNoise1958.gpuMaps1960(second);
            for(int i=0;i<3;i++){check(a[i]!=b[i],"each oracle owns new map");words+=a[i].length;}
            check(StrongNoise1958.takeResident1961(first)==null&&StrongNoise1958.takeResident1961(second)==null,"CPU maps have no GPU lease");
            check(Arrays.equals(input,s.data),"source stays immutable");
            check(actualNative?nativeCalls.get()>before:nativeCalls.get()==before,"actual configured native/fallback backend");models++;
        }
        Source s=new Source(257,259,1);s.cancelAt=5;boolean cancelled=false;
        try{StrongNoise1958.prepareCpuOracle19100(s,s.w,s.h,4,true);}catch(CancellationException expected){cancelled=true;}finally{Thread.interrupted();}
        check(cancelled&&s.reads==5,"capture interrupt stops independent source traversal");
        s.cancelAt=-1;s.reads=0;
        if(actualNative){
            interruptNative.set(true);cancelled=false;
            try{StrongNoise1958.prepareCpuOracle19100(s,s.w,s.h,4,true);}catch(CancellationException expected){cancelled=true;}finally{Thread.interrupted();}
            check(cancelled&&!interruptNative.get(),"native map worker cancellation propagates");
            check(SpeedWorkers1935.cpuIdle1944(),"cancelled native map workers fully drained");
        }
        StrongNoise1958.Model recovered=StrongNoise1958.prepareCpuOracle19100(s,s.w,s.h,4,true);
        check(StrongNoise1958.sameModel1961(recovered,StrongNoise1958.prepareJava(s,s.w,s.h,4,true)),"recovery after cancellation remains exact");
    }
    static void fullImages(String output)throws Exception {
        try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(output)))){
            for(int pattern=0;pattern<3;pattern++)for(boolean shadow:new boolean[]{false,true})for(boolean identity:new boolean[]{false,true}){
                Bitmap source=Chain1962Oracle.image(65,131,pattern);int[] original=Chain1962Oracle.pixels(source);int width=identity?65:83,height=identity?131:149;
                QualityPixels1932.Plan plan=Chain1962Oracle.plan(source,width,height,pattern%2).withLocalNoise(Chain1962Oracle.noise(source),4);
                Bitmap first=null,second=null;
                cpu().set(Boolean.TRUE);
                try{
                    // A bogus half input must never be consumed by either independent CPU oracle.
                    int[] poisonedHalf={0x12345678};
                    first=QualityPipeline1932.strongFinish1978(source,plan,4,shadow,poisonedHalf,0,width,height,plan,false,true,identity);
                    second=QualityPipeline1932.strongFinish1978(source,plan,4,shadow,poisonedHalf,0,width,height,plan,false,true,identity);
                    int[] a=Chain1962Oracle.pixels(first),b=Chain1962Oracle.pixels(second);check(Arrays.equals(a,b),"two independent whole-image oracles equal");
                    out.writeInt(a.length);for(int p:a)out.writeInt(p);pixels+=a.length;images++;
                    check(Arrays.equals(original,Chain1962Oracle.pixels(source))&&!source.isRecycled(),"whole-image source immutable");
                }finally{cpu().remove();if(first!=null)first.recycle();if(second!=null)second.recycle();source.recycle();}
            }
        }
    }
    static String bench()throws Exception {
        Source s=new Source(1024,768,11);long[][] nanos=new long[2][5];
        for(int trial=-2;trial<5;trial++)for(int turn=0;turn<2;turn++){
            int which=(trial&1)==0?turn:1-turn;long start=System.nanoTime();StrongNoise1958.Model m=which==0?StrongNoise1958.prepareJava(s,s.w,s.h,4,true):StrongNoise1958.prepareCpuOracle19100(s,s.w,s.h,4,true);
            long elapsed=System.nanoTime()-start;check(m!=null,"benchmark complete model");if(trial>=0)nanos[which][trial]=elapsed;
        }
        Arrays.sort(nanos[0]);Arrays.sort(nanos[1]);return "{\"width\":1024,\"height\":768,\"warmups\":2,\"trials\":5,\"java_median_ns\":"+nanos[0][2]+",\"native_cpu_median_ns\":"+nanos[1][2]+",\"device_speedup_verified\":false}";
    }
    public static void main(String[] args)throws Exception {
        boolean actualNative=args[1].equals("native");check(StrongNoise1958.nativeAvailable()==actualNative,"native library availability observed");
        modelCorpus(actualNative);fullImages(args[0]);String timing=actualNative?bench():"null";
        check(gpuCalls==0,"CPU oracle never enters model GPU selection/region/candidate reservation");
        check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0&&GpuQualification1961.retainedBytes()==0,"no GPU or optional snapshot ownership");
        check(field(GpuStrong1960.class,"active").get(null)==null,"all Strong workers drained");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"model_cases\":"+models+",\"model_words\":"+words+",\"image_cases\":"+images+",\"compared_pixels\":"+pixels+",\"native_calls\":"+nativeCalls.get()+",\"gpu_calls\":"+gpuCalls+",\"actual_native_cpu\":"+actualNative+",\"benchmark\":"+timing+"}");
    }
}
