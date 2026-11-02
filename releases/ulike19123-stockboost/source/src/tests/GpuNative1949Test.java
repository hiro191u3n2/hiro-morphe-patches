package com.hiro.ulike;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/** Execute the production C/JNI transaction, including its real EGL context,
 * uploads, shaders, barriers, bounded fence and final Java-array commits.
 * The host-only adapter supplies an unchanged scalar-C reference and an
 * explicitly injected unsignaled fence; it never replaces GPU arithmetic. */
public final class GpuNative1949Test {
    private static Method fusion,aggregate,abi;
    private static long assertions;
    private static int fusionCases,residualCases,invalidCases,parallelCases;
    private static final Random RANDOM=new Random(19491948L);
    private static native void scalarAggregate(int[] input,int[] meta,int[] out,
            int width,int begin,int end,int lo,int hi,int radius,int[] range);
    private static native boolean beginExternal();
    private static native boolean externalUnchanged();
    private static native boolean endExternal();
    private static native void forceTimeout(boolean enabled);
    private static native long[] fenceFacts();

    private static void req(boolean condition,String message){
        assertions++;if(!condition)throw new AssertionError(message);
    }
    private static boolean invoke(Method method,Object... args)throws Exception{
        try{return (Boolean)method.invoke(null,args);}
        catch(InvocationTargetException failed){
            Throwable cause=failed.getCause();
            if(cause instanceof Exception)throw (Exception)cause;
            if(cause instanceof Error)throw (Error)cause;
            throw failed;
        }
    }
    private static boolean sums(byte[] data,int w,int h,int step,int[] sum,int[] count)throws Exception{
        return invoke(fusion,data,w,h,step,sum,count);
    }
    private static boolean residual(int[] src,int[] meta,int[] out,int w,int rows,
            int begin,int end,int lo,int hi,int radius,int[] range)throws Exception{
        return invoke(aggregate,src,meta,out,w,rows,begin,end,lo,hi,radius,range);
    }
    private static void same(int[] actual,int[] expected,String context){
        req(actual.length==expected.length,context+" length");
        for(int i=0;i<actual.length;i++)req(actual[i]==expected[i],context+" index="+i+" expected="+expected[i]+" actual="+actual[i]);
    }
    private static int[] seeds(int size){
        int[] values=new int[size];for(int i=0;i<size;i++)values[i]=RANDOM.nextInt();return values;
    }
    private static int[] ranges(){
        int[] range=new int[33*256+5];
        for(int s=1;s<=32;s++)for(int d=0;d<256;d++)
            range[s*256+d]=(int)Math.round(256.0*Math.exp(-(double)d*d/(2.0*s*s)));
        Arrays.fill(range,33*256,range.length,-987654321);return range;
    }
    private static void fusionFixture(int w,int h,int step,int mode)throws Exception{
        byte[] input=new byte[w*h+7];RANDOM.nextBytes(input);
        if(mode==1)Arrays.fill(input,0,w*h,(byte)255);
        if(mode==2)for(int i=0;i<w*h;i++)input[i]=(byte)(i*71+i/w*17);
        byte[] before=input.clone();int n=((w+step-1)/step)*((h+step-1)/step);
        int[] actualS=seeds(n+7),actualC=seeds(n+9),expectS=actualS.clone(),expectC=actualC.clone();
        int at=0;
        for(int y=0;y<h;y+=step)for(int x=0;x<w;x+=step){
            int sum=0,count=0;
            for(int yy=y;yy<Math.min(h,y+step);yy++)for(int xx=x;xx<Math.min(w,x+step);xx++){
                sum+=input[yy*w+xx]&255;count++;
            }
            expectS[at]=sum;expectC[at++]=count;
        }
        req(sums(input,w,h,step,actualS,actualC),"fusion JNI failed "+w+"x"+h+" step="+step);
        same(actualS,expectS,"fusion exact sums/tail");same(actualC,expectC,"fusion exact counts/tail");
        req(Arrays.equals(input,before),"fusion input changed");fusionCases++;
    }
    private static void residualFixture(int w,int rows,int radius,int mode)throws Exception{
        int lo=rows>6?1:0,hi=rows>6?rows-1:rows;
        int begin=mode==3?lo+(hi-lo)/3:lo,end=mode==3?hi-(hi-lo)/4:hi;
        int[] input=new int[w*rows+5],meta=new int[w*(end-begin)+3],range=ranges();
        for(int i=0;i<input.length;i++){
            int x=i%w,y=i/w,g;
            if(mode==1)g=((x+y)%2==0?17:39);
            else if(mode==2)g=(x%3==0?0:x%3==1?255:127);
            else g=90+RANDOM.nextInt(41);
            int r=Math.max(0,Math.min(255,g+RANDOM.nextInt(21)-10));
            int b=Math.max(0,Math.min(255,g+RANDOM.nextInt(21)-10));
            int alpha=mode==4?0:mode==3&&RANDOM.nextInt(5)==0?254:255;
            input[i]=(alpha<<24)|(r<<16)|(g<<8)|b;
        }
        for(int i=0;i<meta.length;i++)meta[i]=(i%7==0?0:0x80000000)|(2+RANDOM.nextInt(31))|((3+RANDOM.nextInt(61))<<6);
        if(mode==5)Arrays.fill(range,0,33*256,0);
        int[] actual=seeds(w*(end-begin)*3+11),expected=actual.clone();
        int[] beforeInput=input.clone(),beforeMeta=meta.clone(),beforeRange=range.clone();
        scalarAggregate(input,meta,expected,w,begin,end,lo,hi,radius,range);
        req(residual(input,meta,actual,w,rows,begin,end,lo,hi,radius,range),
                "residual JNI failed "+w+"x"+rows+" radius="+radius+" mode="+mode);
        same(actual,expected,"scalar C residual summary/tail");
        same(input,beforeInput,"residual source immutable");same(meta,beforeMeta,"metadata immutable");
        same(range,beforeRange,"ranges immutable");residualCases++;
    }
    private static void rejectedFusion(byte[] input,int w,int h,int step,int[] sum,int[] count)throws Exception{
        byte[] before=input==null?null:input.clone();int[] bs=sum==null?null:sum.clone(),bc=count==null?null:count.clone();
        req(!sums(input,w,h,step,sum,count),"invalid fusion accepted");
        if(input!=null)req(Arrays.equals(input,before),"invalid fusion source changed");
        if(sum!=null)same(sum,bs,"invalid sums no write");if(count!=null)same(count,bc,"invalid counts no write");invalidCases++;
    }
    private static void rejectedResidual(int[] src,int[] meta,int[] out,int w,int rows,
            int begin,int end,int lo,int hi,int radius,int[] range)throws Exception{
        int[][] arrays={src,meta,out,range},before=new int[4][];
        for(int i=0;i<4;i++)if(arrays[i]!=null)before[i]=arrays[i].clone();
        req(!residual(src,meta,out,w,rows,begin,end,lo,hi,radius,range),"invalid residual accepted");
        for(int i=0;i<4;i++)if(arrays[i]!=null)same(arrays[i],before[i],"invalid residual no Java-array write "+i);invalidCases++;
    }
    private static void invalidFixtures()throws Exception{
        byte[] data={1,2,3,4};int[] sum={17,18,19,20},count={23,24,25,26};
        rejectedFusion(null,2,2,1,sum,count);rejectedFusion(data,2,2,1,null,count);
        rejectedFusion(data,2,2,1,sum,null);rejectedFusion(data,2,2,1,sum,sum);
        for(int[] dims:new int[][]{{0,2,1},{2,0,1},{-1,2,1},{2,2,0},{2,2,257},{16385,1,1},{1,16385,1},{16384,16384,1}})
            rejectedFusion(data,dims[0],dims[1],dims[2],sum,count);
        rejectedFusion(new byte[3],2,2,1,sum,count);
        rejectedFusion(data,2,2,1,new int[3],count);rejectedFusion(data,2,2,1,sum,new int[3]);
        int[] src=new int[4],meta=new int[4],out=new int[12],range=ranges();
        Arrays.fill(src,0xff414243);Arrays.fill(meta,0x80000000|9|(5<<6));Arrays.fill(out,0x13579bdf);
        rejectedResidual(null,meta,out,2,2,0,2,0,2,1,range);
        rejectedResidual(src,null,out,2,2,0,2,0,2,1,range);
        rejectedResidual(src,meta,null,2,2,0,2,0,2,1,range);
        rejectedResidual(src,meta,out,2,2,0,2,0,2,1,null);
        // Every pair of int-array parameters must reject aliasing before JNI access.
        for(int a=0;a<4;a++)for(int b=a+1;b<4;b++){
            int[][] arrays={src.clone(),meta.clone(),out.clone(),range.clone()};arrays[b]=arrays[a];
            rejectedResidual(arrays[0],arrays[1],arrays[2],2,2,0,2,0,2,1,arrays[3]);
        }
        for(int[] dims:new int[][]{{0,2,0,2,0,2,1},{2,0,0,0,0,0,1},{16385,2,0,2,0,2,1},
                {2,2,0,2,0,2,0},{2,2,0,2,0,2,5},{2,2,0,2,-1,2,1},{2,2,0,2,0,3,1},
                {2,2,0,2,1,2,1},{2,2,1,0,0,2,1},{2,2,0,3,0,2,1},{2,2,0,0,0,0,1}})
            rejectedResidual(src,meta,out,dims[0],dims[1],dims[2],dims[3],dims[4],dims[5],dims[6],range);
        rejectedResidual(new int[3],meta,out,2,2,0,2,0,2,1,range);
        rejectedResidual(src,new int[3],out,2,2,0,2,0,2,1,range);
        rejectedResidual(src,meta,new int[11],2,2,0,2,0,2,1,range);
        rejectedResidual(src,meta,out,2,2,0,2,0,2,1,new int[33*256-1]);
        for(int bad:new int[]{0x80000000|1|(3<<6),0x80000000|33|(3<<6),0x80000000|9|(2<<6)}){
            int[] m=meta.clone();m[2]=bad;rejectedResidual(src,m,out,2,2,0,2,0,2,1,range);
        }
        for(int value:new int[]{-1,257}){
            int[] r=range.clone();r[8447]=value;rejectedResidual(src,meta,out,2,2,0,2,0,2,1,r);
        }
        int[] emptySeed=out.clone();
        req(residual(src,meta,out,2,2,1,1,0,2,1,range),"valid empty range rejected");
        same(out,emptySeed,"empty range no write");
    }
    private static void foreignContext()throws Exception{
        req(beginExternal(),"host external EGL context creation failed");
        try{
            rejectedFusion(new byte[]{4,3,2,1},2,2,1,new int[]{7,8,9,10},new int[]{11,12,13,14});
            req(externalUnchanged(),"foreign EGL context/API was rebound");
            int[] m={0x80000000|9|(4<<6)};
            rejectedResidual(new int[]{0xff454545},m,new int[]{17,18,19},1,1,0,1,0,1,1,ranges());
            req(externalUnchanged(),"residual rebound foreign EGL context/API");
        }finally{req(endExternal(),"host external EGL context release failed");}
        fusionFixture(7,9,2,2);residualFixture(9,11,4,1);
    }
    private static void parallel()throws Exception{
        // CPU references are independent per thread; source/metadata differ to
        // detect a stale upload or cross-call retained-buffer contamination.
        final int threads=4,iterations=8;Throwable[] failures=new Throwable[threads];
        long[] checks=new long[threads];Thread[] workers=new Thread[threads];
        for(int t=0;t<threads;t++){final int thread=t;workers[t]=new Thread(()->{
            try{
                for(int call=0;call<iterations;call++){
                    int w=17+thread,rows=11+call%3,radius=1+call%4;
                    int[] src=new int[w*rows],meta=new int[w*rows],expected=new int[w*rows*3],actual=new int[expected.length];
                    for(int i=0;i<src.length;i++){int g=40+thread*17+(i*3+call*7)%21;src[i]=0xff000000|g<<16|g<<8|g;meta[i]=0x80000000|9|(5<<6);}
                    int[] range=new int[33*256];Arrays.fill(range,256);
                    scalarAggregate(src,meta,expected,w,0,rows,0,rows,radius,range);
                    if(!residual(src,meta,actual,w,rows,0,rows,0,rows,radius,range))throw new AssertionError("parallel JNI rejected");
                    checks[thread]++;
                    for(int i=0;i<actual.length;i++){if(actual[i]!=expected[i])throw new AssertionError("parallel contamination");checks[thread]++;}
                }
            }catch(Throwable failure){failures[thread]=failure;}
        });workers[t].start();}
        for(Thread worker:workers)worker.join();
        for(int t=0;t<threads;t++){if(failures[t]!=null)throw new AssertionError(failures[t]);assertions+=checks[t];}
        parallelCases=threads*iterations;
    }
    private static void failedFence()throws Exception{
        long[] success=fenceFacts();
        req(success[0]==200000000L,"production fence timeout is not200ms");
        req(success[1]>0&&success[2]>0,"actual production fence and readback were not reached");
        forceTimeout(true);
        int[] m={0x80000000|9|(4<<6)};
        rejectedResidual(new int[]{0xff454545},m,new int[]{17,18,19},1,1,0,1,0,1,1,ranges());
        long[] failure=fenceFacts();
        req(failure[0]==200000000L&&failure[1]==1,"injected timeout did not traverse production200ms fence");
        req(failure[2]==0,"production mapped after unsignaled fence");
        forceTimeout(false);
        rejectedFusion(new byte[]{1},1,1,1,new int[]{7},new int[]{8});
        rejectedResidual(new int[]{0xff454545},m,new int[]{17,18,19},1,1,0,1,0,1,1,ranges());
        long[] disabled=fenceFacts();
        req(disabled[1]==0&&disabled[2]==0,"disabled backend dispatched or mapped again");
    }
    public static void main(String[] args)throws Exception{
        System.loadLibrary("ulike_gpu1949");
        abi=GpuInteger1949.class.getDeclaredMethod("nativeAbi");abi.setAccessible(true);
        fusion=GpuInteger1949.class.getDeclaredMethod("fusionSumsNative",byte[].class,int.class,int.class,int.class,int[].class,int[].class);fusion.setAccessible(true);
        aggregate=GpuInteger1949.class.getDeclaredMethod("aggregateNative",int[].class,int[].class,int[].class,int.class,int.class,int.class,int.class,int.class,int.class,int.class,int[].class);aggregate.setAccessible(true);
        req(((Integer)abi.invoke(null))==1949,"production JNI ABI mismatch");
        req(GpuInteger1949.available(),"production library failed to load");
        invalidFixtures();
        for(int[] dims:new int[][]{{1,1,1},{3,5,2},{7,9,1},{8,8,3},{9,7,4},{17,19,5},{33,31,8},{131,129,16},{256,256,256},{257,259,256}})
            for(int mode=0;mode<3;mode++)fusionFixture(dims[0],dims[1],dims[2],mode);
        // Alternate capacities/shapes and new content, retaining the same EGL
        // context/programs/buffers exactly as production does.
        for(int[] dims:new int[][]{{1,1},{3,5},{7,9},{8,8},{9,7},{17,19},{33,31},{131,19}})
            for(int radius=1;radius<=4;radius++)for(int mode=0;mode<6;mode++)residualFixture(dims[0],dims[1],radius,mode);
        foreignContext();parallel();failedFence();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+
                ",\"fusion_cases\":"+fusionCases+",\"residual_cases\":"+residualCases+
                ",\"invalid_no_write_cases\":"+invalidCases+",\"parallel_cases\":"+parallelCases+
                ",\"production_c_jni_executed\":true,\"actual_gpu_execution_on_host\":true,"+
                "\"integer_summary_bit_exact\":true,\"input_arrays_immutable\":true,"+
                "\"foreign_current_context_preserved\":true,\"fence_timeout_ns\":200000000,"+
                "\"injected_unsignaled_fence_no_map_no_write\":true,\"timeout_disables_backend\":true}");
    }
}
