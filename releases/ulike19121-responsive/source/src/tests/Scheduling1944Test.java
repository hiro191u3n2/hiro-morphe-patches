package com.hiro.ulike;
import android.graphics.Bitmap;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Full-image oracle checks strip boundaries/ownership independently of scheduler. */
public final class Scheduling1944Test {
    static int assertions,scenarios;
    static void check(boolean b,String message){assertions++;if(!b)throw new AssertionError(message);}
    static int[] image(int width,int height) {
        int[] p=new int[width*height];Random r=new Random(1944001L);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            int g=70+(x/3+y/7)%81+r.nextInt(11)-5;
            int rr=Math.min(255,g+((x+y)%2==0?14:0)),bb=Math.max(0,g-(x%4==0?12:0));
            int alpha=(x==2&&y%31==0)?96:255;
            p[y*width+x]=(alpha<<24)|(rr<<16)|(g<<8)|bb;
        }
        return p;
    }
    static void pixels(int height,int expectedCore,boolean moire,boolean sharp) {
        int width=80;int[] source=image(width,height),expected=new int[source.length];
        QualityPixels1932.Plan plan=QualityPixels1932.plan(null,1600,20000000L,
            QualityPixels1932.LENS_FRONT,.6f,3,4,true,true,1f)
            .withFaceRegions(new FaceRegions1934.Mask());
        QualityPixels1932.finishStripAt(source,expected,width,height,0,height,plan,moire,sharp,0);
        Bitmap b=Bitmap.from(width,height,source,Bitmap.Config.ARGB_8888,true);
        int chosen=Scheduling1944.coreRows(width,height,QualityPixels1932.HALO,4,128L*1024*1024);
        check(chosen==expectedCore,"expected halo amortization strip size "+height);
        QualityPipeline1932.finishInPlace(b,plan,moire,sharp);
        check(Arrays.equals(expected,b.snapshot()),"whole-image exact pixels across strips "+expectedCore+" mode "+moire+":"+sharp);
        check(Arrays.equals(source,image(width,height)),"oracle immutable original halo "+height);
        b.recycle();scenarios++;
    }
    static void tuning() {
        check(SpeedWorkers1935.maxWorkers()==4,"test uses deterministic 4-CPU cap");
        long pixels=1000000L;
        check(Scheduling1944.workers(0,pixels,100,4)==4,"initial conservative cap");
        for(int i=0;i<6;i++)Scheduling1944.measured(0,4,pixels,12000000L,true);
        Scheduling1944.measured(0,2,pixels,6000000L,true);
        Scheduling1944.measured(0,2,pixels,6000000L,true);
        check(Scheduling1944.workers(0,pixels,100,4)==4,"two observations cannot switch worker budget");
        Scheduling1944.measured(0,2,pixels,6000000L,true);
        check(Scheduling1944.workers(0,pixels,100,4)==2,"stable substantial lower-budget speed improvement selected");
        for(int i=0;i<6;i++)Scheduling1944.measured(0,1,pixels,3000000L,false);
        check(Scheduling1944.workers(0,pixels,100,4)==2,"competing stages cannot train single-worker preference");
        check(Scheduling1944.workers(0,pixels,100,1)==1,"memory cap overrides performance preference");
        check(Scheduling1944.coreRows(4080,3060,32,4,8L*1024*1024)==128,"low memory cannot enlarge workspace");
        check(Scheduling1944.coreRows(4080,3060,32,4,128L*1024*1024)==256,"12MP save amortizes halo under sufficient memory");
        scenarios++;
    }
    static void sharedCpu()throws Exception {
        final AtomicInteger running=new AtomicInteger(),peak=new AtomicInteger(),done=new AtomicInteger();
        final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        Runnable caller=new Runnable(){public void run(){try {
            Runnable[] tasks=new Runnable[8];
            for(int i=0;i<tasks.length;i++)tasks[i]=new Runnable(){public void run(){
                int active=running.incrementAndGet();for(;;){int old=peak.get();if(active<=old||peak.compareAndSet(old,active))break;}
                try {long sum=0;for(int k=0;k<2000000;k++)sum=(sum+k)*1664525L+1013904223L;if(sum==0)throw new AssertionError("work");done.incrementAndGet();}
                finally {running.decrementAndGet();}
            }};
            SpeedWorkers1935.run(tasks);
        }catch(Throwable e){failure.compareAndSet(null,e);}}};
        Thread a=new Thread(caller),b=new Thread(caller);a.start();b.start();a.join();b.join();
        if(failure.get()!=null)throw new AssertionError(failure.get());
        check(done.get()==16,"every overlapping shot task completes exactly once");
        check(peak.get()<=4&&running.get()==0&&SpeedWorkers1935.cpuIdle1944(),"shared CPU budget never exceeds four, no leaked permits");
        scenarios++;
    }
    public static void main(String[] args)throws Exception {
        tuning();sharedCpu();
        for(int mode=1;mode<=3;mode++) {
            pixels(129,128,(mode&1)!=0,(mode&2)!=0);
            pixels(2053,256,(mode&1)!=0,(mode&2)!=0);
            pixels(4103,512,(mode&1)!=0,(mode&2)!=0);
        }
        SpeedWorkers1935.trim();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+",\"device_tested\":false}");
    }
}
