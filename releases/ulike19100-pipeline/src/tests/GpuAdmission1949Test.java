package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/** Tests the real admission/lifecycle Java code with an explicitly mocked GPU.
 * Kernel execution is covered independently by native tests, not this adapter. */
public final class GpuAdmission1949Test {
    static final AtomicInteger assertions=new AtomicInteger();static int scenarios;
    static void req(boolean ok,String why){assertions.incrementAndGet();if(!ok)throw new AssertionError(why);}
    static final class Timer extends GpuInteger1949.Clock {
        int at;long[] times={0,10,10,100};
        void fast(){at=0;times=new long[]{0,10,10,100};}
        void slow(){at=0;times=new long[]{0,100,100,110};}
        void zero(){at=0;times=new long[]{0,0,0,0};}
        long now(){return times[Math.min(at++,times.length-1)];}
    }
    static final class Adapter implements GpuInteger1949.Backend {
        boolean enabled=true,bad,miss,throwLink;int fusionCalls,noiseCalls;
        final AtomicInteger active=new AtomicInteger(),maximum=new AtomicInteger();
        public boolean available(){return enabled;}
        void entered(){int n=active.incrementAndGet();for(;;){int m=maximum.get();if(m>=n||maximum.compareAndSet(m,n))break;}}
        public boolean fusion(byte[] data,int w,int h,int step,int[] sums,int[] counts){
            fusionCalls++;if(throwLink)throw new UnsatisfiedLinkError("scripted unavailable");
            entered();try{oracle(data,w,h,step,sums,counts);if(bad)sums[0]^=1;return !miss;}
            finally{active.decrementAndGet();}
        }
        public boolean aggregate(int[] input,int[] meta,int[] out,int w,int rows,
                int begin,int end,int lo,int hi,int radius,int[] range){
            noiseCalls++;if(throwLink)throw new UnsatisfiedLinkError("scripted unavailable");
            entered();try{noiseOracle(input,meta,out,w,begin,end);if(bad)out[0]^=1;return !miss;}
            finally{active.decrementAndGet();}
        }
    }
    static void oracle(byte[] src,int width,int height,int step,int[] sums,int[] counts){
        int columns=(int)(((long)width+step-1)/step),rows=(int)(((long)height+step-1)/step);
        Arrays.fill(sums,0);Arrays.fill(counts,0);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            int at=(y/step)*columns+x/step;sums[at]+=src[y*width+x]&255;counts[at]++;
        }
        if(rows*columns>sums.length)throw new AssertionError("oracle shape");
    }
    static byte[] data(int w,int h,int seed){byte[] out=new byte[w*h];long state=seed;
        for(int i=0;i<out.length;i++){state=state*1664525+1013904223;out[i]=(byte)(state>>13);}return out;}
    static void sumEqual(byte[] src,int w,int h,int step,int[] sums,int[] counts){
        int[] s=new int[sums.length],c=new int[counts.length];oracle(src,w,h,step,s,c);
        req(Arrays.equals(sums,s),"exact integer sums");req(Arrays.equals(counts,c),"exact counts");
        for(int i=0;i<s.length;i++)req(Float.floatToRawIntBits((float)(long)sums[i]/counts[i])==
                Float.floatToRawIntBits((float)(long)s[i]/c[i]),"original float division bits");
    }
    static void noiseOracle(int[] src,int[] meta,int[] out,int w,int begin,int end){
        for(int i=0;i<w*(end-begin);i++){
            if(meta[i]<0){out[i*3]=(src[begin*w+i]^0x123456)&0xffffff;
                out[i*3+1]=(src[begin*w+i]^0x456789)&0xffffff;out[i*3+2]=0x80020107;}
            else out[i*3+2]=0;
        }
    }
    static final class Noise implements GpuInteger1949.CpuAggregate {
        final int w=17,rows=13,begin=4,end=9,lo=2,hi=11,radius=4;
        final int[] input=new int[w*rows],meta=new int[w*(end-begin)],range=new int[33*256];
        int cpu;boolean fail;
        Noise(){for(int i=0;i<input.length;i++)input[i]=(i*0x1234)&0xffffff;
            for(int i=0;i<meta.length;i++)meta[i]=(i%3==0?0:0x80000000)|12;}
        public boolean run(int[] out){cpu++;if(fail)return false;noiseOracle(input,meta,out,w,begin,end);return true;}
        int[] seed(){int[] out=new int[meta.length*3];Arrays.fill(out,0x715273);return out;}
        boolean invoke(GpuInteger1949.Engine e,int[] out){return e.aggregate(input,meta,out,w,rows,begin,end,lo,hi,radius,range,this);}
        void equal(int[] out){int[] expected=seed();noiseOracle(input,meta,expected,w,begin,end);
            req(Arrays.equals(expected,out),"summary equality including inactive seeds");}
    }
    static void fusionAdmission(){scenarios++;
        Adapter a=new Adapter();Timer t=new Timer();GpuInteger1949.Engine e=new GpuInteger1949.Engine(a,t);
        byte[] src=data(517,259,7),before=src.clone();int step=4,n=130*65;
        int[] sums=new int[n],counts=new int[n];
        t.slow();req(e.fusion(src,517,259,step,sums,counts),"cold warmup CPU result");sumEqual(src,517,259,step,sums,counts);
        for(int j=0;j<2;j++){t.fast();req(e.fusion(src,517,259,step,sums,counts),"timed probation");sumEqual(src,517,259,step,sums,counts);}
        t.fast();req(e.fusion(src,517,259,step,sums,counts),"admitted GPU result");sumEqual(src,517,259,step,sums,counts);
        req(Arrays.equals(src,before),"source immutable");req(a.fusionCalls==4,"one GPU dispatch per invocation");
        // Same dimensions, distinct capture content: never reuse an image.
        src=data(517,259,19);t.fast();req(e.fusion(src,517,259,step,sums,counts),"second capture");sumEqual(src,517,259,step,sums,counts);
    }
    static void fusionFailure(int mode){scenarios++;
        Adapter a=new Adapter();Timer t=new Timer();GpuInteger1949.Engine e=new GpuInteger1949.Engine(a,t);
        byte[] src=data(33,17,mode+1);int[] sums=new int[45],counts=new int[45];
        if(mode==0)a.bad=true;if(mode==1)a.miss=true;if(mode==2)t.slow();
        req(e.fusion(src,33,17,4,sums,counts),"failure probation uses CPU result");sumEqual(src,33,17,4,sums,counts);
        if(mode==2){t.slow();req(e.fusion(src,33,17,4,sums,counts),"second slower probe CPU");}
        int calls=a.fusionCalls;
        req(!e.fusionAvailable(33,17,4),"rejected shape skips staging allocation");
        req(e.fusionAvailable(35,17,4),"another shape has independent probation");
        req(!e.fusion(src,33,17,4,sums,counts),"rejected stage delegates original CPU");req(a.fusionCalls==calls,"no GPU repeat on rejection");
    }
    static void noiseAdmission(){scenarios++;
        Adapter a=new Adapter();Timer t=new Timer();GpuInteger1949.Engine e=new GpuInteger1949.Engine(a,t);Noise n=new Noise();
        int[] before=n.input.clone(),metaBefore=n.meta.clone();
        for(int j=0;j<3;j++){t.fast();int[] out=n.seed();req(n.invoke(e,out),"noise probation");n.equal(out);}
        req(n.cpu==3,"all three original CPU comparisons executed");
        t.fast();int[] out=n.seed();req(n.invoke(e,out),"noise admitted");n.equal(out);req(n.cpu==3,"no per-call reference after admission");
        req(Arrays.equals(before,n.input)&&Arrays.equals(metaBefore,n.meta),"noise inputs immutable");
        n.input[0]^=0x456;n.input[n.begin*n.w+2]^=0x7654;t.fast();out=n.seed();req(n.invoke(e,out),"fresh noise content");n.equal(out);
    }
    static void noiseFailure(int mode){scenarios++;
        Adapter a=new Adapter();Timer t=new Timer();GpuInteger1949.Engine e=new GpuInteger1949.Engine(a,t);Noise n=new Noise();
        a.bad=mode==0;a.miss=mode==1;n.fail=mode==2;
        t.fast();int[] out=n.seed();boolean result=n.invoke(e,out);
        req(result==(mode!=2),"noise CPU failure propagated");if(mode!=2)n.equal(out);
        int calls=a.noiseCalls;t.fast();req(!n.invoke(e,n.seed()),"noise rejected stage");req(a.noiseCalls==calls,"no rejected dispatch");
        req(!e.aggregateAvailable(n.w,n.rows,n.begin,n.end,n.lo,n.hi,n.radius),"rejected noise shape skips adapter allocation");
    }
    static void refresh(boolean mismatch){scenarios++;
        Adapter a=new Adapter();Timer t=new Timer();GpuInteger1949.Engine e=new GpuInteger1949.Engine(a,t);Noise n=new Noise();
        for(int j=0;j<3;j++){t.fast();req(n.invoke(e,n.seed()),"refresh admission");}
        for(int j=0;j<63;j++){t.fast();req(n.invoke(e,n.seed()),"fast reuse");}
        req(n.cpu==3,"no CPU before refresh boundary");a.bad=mismatch;t.slow();int[] out=n.seed();
        req(n.invoke(e,out),"refresh returns CPU result");n.equal(out);req(n.cpu==4,"refresh exact CPU ran");
        t.fast();req(!n.invoke(e,n.seed()),"refresh rejection returns to existing CPU");
    }
    static void unavailableAndBounds(){scenarios++;
        Adapter a=new Adapter();Timer t=new Timer();GpuInteger1949.Engine e=new GpuInteger1949.Engine(a,t);
        byte[] src=data(8,8,4);int[] sums=new int[16],counts=new int[16];Arrays.fill(sums,71);a.enabled=false;
        req(!e.fusion(src,8,8,2,sums,counts),"unavailable CPU fallback");req(a.fusionCalls==0&&sums[0]==71,"unavailable no writes");a.enabled=true;
        req(!e.fusion(src,8,8,2,sums,sums),"sum/count alias");req(!e.fusion(src,8,8,0,sums,counts),"invalid step");
        req(!e.fusion(src,8,8,4096,sums,counts),"integer overflow guard");req(!e.fusion(src,9,8,2,sums,counts),"input bounds");
        req(!e.fusion(src,8,8,1,sums,counts),"output bounds");
        Noise n=new Noise();req(!e.aggregate(n.input,n.meta,n.input,n.w,n.rows,n.begin,n.end,n.lo,n.hi,4,n.range,n),"noise input alias");
        req(!e.aggregate(n.input,n.meta,n.seed(),n.w,n.rows,n.begin,n.end,n.lo,n.hi,5,n.range,n),"noise radius bound");
        req(!e.aggregate(n.input,n.meta,n.seed(),n.w,n.rows,n.begin,n.end,n.lo,n.hi,4,n.range,null),"no independent CPU reference");
        int[] out=n.seed(),before=out.clone();req(e.aggregate(n.input,n.meta,out,n.w,n.rows,4,4,n.lo,n.hi,4,n.range,n),"empty noise stage");
        req(Arrays.equals(out,before),"empty no writes");req(a.fusionCalls==0&&a.noiseCalls==0,"bounds never dispatch");
    }
    static void linkFailure(){scenarios++;
        Adapter a=new Adapter();Timer t=new Timer();GpuInteger1949.Engine e=new GpuInteger1949.Engine(a,t);a.throwLink=true;
        byte[] src=data(8,8,3);req(!e.fusion(src,8,8,2,new int[16],new int[16]),"link failure original fallback");
        req(!e.available(),"link failure process disable");a.throwLink=false;t.fast();
        req(!e.fusion(src,8,8,2,new int[16],new int[16]),"no reload storm");req(a.fusionCalls==1,"one unavailable call");
    }
    static void zeroDuration(){scenarios++;
        Adapter a=new Adapter();Timer t=new Timer();GpuInteger1949.Engine e=new GpuInteger1949.Engine(a,t);byte[] src=data(8,8,2);
        t.zero();req(e.fusion(src,8,8,2,new int[16],new int[16]),"zero-time cold result");
        t.zero();req(e.fusion(src,8,8,2,new int[16],new int[16]),"zero-time CPU result");
        req(!e.fusion(src,8,8,2,new int[16],new int[16]),"zero CPU duration cannot claim faster");
    }
    static void boundedShapes(){scenarios++;
        Adapter a=new Adapter();Timer t=new Timer();GpuInteger1949.Engine e=new GpuInteger1949.Engine(a,t);
        for(int w=1;w<=40;w++){t.fast();byte[] src=data(w,3,w);req(e.fusion(src,w,3,1,new int[w*3],new int[w*3]),"bounded admission table");}
        req(a.fusionCalls==40,"new shapes probation individually");
    }
    static void dimensions(){int[][] shapes={{1,1,1},{17,1,2},{1,19,4},{511,513,4},{4080,3060,16}};
        for(int[] shape:shapes){scenarios++;int w=shape[0],h=shape[1],step=shape[2],n=((w+step-1)/step)*((h+step-1)/step);
            Adapter a=new Adapter();Timer t=new Timer();GpuInteger1949.Engine e=new GpuInteger1949.Engine(a,t);
            byte[] src=data(w,h,w+h);int[] sums=new int[n],counts=new int[n];req(e.fusion(src,w,h,step,sums,counts),"real dimension CPU result");sumEqual(src,w,h,step,sums,counts);}
    }
    static void cancellation(){scenarios++;Adapter a=new Adapter();Timer t=new Timer();GpuInteger1949.Engine e=new GpuInteger1949.Engine(a,t);
        Thread.currentThread().interrupt();boolean cancelled=false;
        try{e.fusion(data(8,8,3),8,8,2,new int[16],new int[16]);}catch(IllegalStateException expected){cancelled=true;}
        finally{Thread.interrupted();}req(cancelled,"original CPU cancellation propagated");
    }
    static void contention() throws Exception {scenarios++;
        final Adapter a=new Adapter();final GpuInteger1949.Engine e=new GpuInteger1949.Engine(a,new GpuInteger1949.Clock());
        final CountDownLatch start=new CountDownLatch(1);final Throwable[] fail=new Throwable[6];Thread[] threads=new Thread[6];
        for(int i=0;i<threads.length;i++){final int index=i;threads[i]=new Thread(new Runnable(){public void run(){try{start.await();
            for(int j=0;j<8;j++){int w=131+index,h=57,step=4,n=((w+3)/4)*((h+3)/4);byte[] src=data(w,h,j+index*71);
                int[] sums=new int[n],counts=new int[n];if(!e.fusion(src,w,h,step,sums,counts))GpuInteger1949.cpuSums(src,w,h,step,sums,counts);
                sumEqual(src,w,h,step,sums,counts);}}
            catch(Throwable problem){fail[index]=problem;}}});threads[i].start();}
        start.countDown();for(Thread thread:threads)thread.join(10000);
        for(int i=0;i<threads.length;i++){req(!threads[i].isAlive(),"contention completes");if(fail[i]!=null)throw new AssertionError(fail[i]);}
        req(a.maximum.get()==1,"context operations never concurrent");
    }
    public static void main(String[] args)throws Exception{
        fusionAdmission();for(int i=0;i<3;i++)fusionFailure(i);noiseAdmission();for(int i=0;i<3;i++)noiseFailure(i);
        refresh(false);refresh(true);unavailableAndBounds();linkFailure();zeroDuration();boundedShapes();dimensions();cancellation();contention();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions.get()+",\"scenarios\":"+scenarios+"}");
    }
}
