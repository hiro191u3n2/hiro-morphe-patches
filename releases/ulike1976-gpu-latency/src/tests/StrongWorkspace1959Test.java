package com.hiro.ulike;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises actual preparation scheduling and exclusive reusable leases. */
public final class StrongWorkspace1959Test {
    private static int assertions,parallelPeak;
    private static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    private static int[] source(int w,int h){Random random=new Random(19594043);int[] out=new int[w*h];for(int i=0;i<out.length;i++){int r=42+random.nextInt(17),g=48+random.nextInt(17),b=53+random.nextInt(17);out[i]=0xff000000|r<<16|g<<8|b;}return out;}
    private static StrongNoise1958.Patches patches(final int[] src,final int w){return new StrongNoise1958.Patches(){public void read(int[] out,int x,int y,int width,int height){for(int row=0;row<height;row++)System.arraycopy(src,(y+row)*w+x,out,row*width,width);}};}
    private static int[] half(int[] src,int w,int h){int ow=(w+1)/2,oh=(h+1)/2;int[] out=new int[ow*oh];for(int y=0;y<oh;y++)for(int x=0;x<ow;x++){int r=0,g=0,b=0,n=0;boolean opaque=true;for(int dy=0;dy<2&&y*2+dy<h;dy++)for(int dx=0;dx<2&&x*2+dx<w;dx++){int p=src[(y*2+dy)*w+x*2+dx];opaque&=(p>>>24)==255;r+=(p>>>16)&255;g+=(p>>>8)&255;b+=p&255;n++;}out[y*ow+x]=(opaque?0xff000000:0)|((r+n/2)/n)<<16|((g+n/2)/n)<<8|(b+n/2)/n;}return out;}
    private static void sameModel(StrongNoise1958.Model a,StrongNoise1958.Model b)throws Exception{for(String name:new String[]{"halfMap","quarterMap","eighthMap","evidence","runtimeEvidence"}){Field field=StrongNoise1958.Model.class.getDeclaredField(name);field.setAccessible(true);Object x=field.get(a),y=field.get(b);check(x instanceof int[]?Arrays.equals((int[])x,(int[])y):Arrays.equals((float[])x,(float[])y),"carried half changed "+name);}}
    private static void preparation()throws Exception{
        final AtomicInteger active=new AtomicInteger(),peak=new AtomicInteger();final CountDownLatch overlap=new CountDownLatch(Math.min(2,SpeedWorkers1935.maxWorkers()));
        SpeedWorkers1935.installHints(new SpeedWorkers1935.Hints(){public Runnable worker(Runnable task){return task;}public Object begin(long ns){int n=active.incrementAndGet();for(;;){int old=peak.get();if(n<=old||peak.compareAndSet(old,n))break;}overlap.countDown();try{if(!overlap.await(5,TimeUnit.SECONDS))throw new AssertionError("no preparation overlap");}catch(InterruptedException e){Thread.currentThread().interrupt();}return Boolean.TRUE;}public void complete(Object token,boolean success){active.decrementAndGet();}});
        try{int w=65,h=1025;int[] src=source(w,h);StrongNoise1958.prepareJava(patches(src,w),w,h,4,true);check(active.get()==0,"preparation returned with live workers");parallelPeak=peak.get();check(parallelPeak>=Math.min(2,SpeedWorkers1935.maxWorkers()),"preparation was not parallel");check(parallelPeak<=SpeedWorkers1935.maxWorkers(),"preparation exceeded CPU cap");}finally{SpeedWorkers1935.installHints(null);}
        for(int[] size:new int[][]{{1,1},{3,5},{33,67},{65,257}}){int w=size[0],h=size[1];int[] src=source(w,h);for(int i=13;i<src.length;i+=47)src[i]&=0x7fffffff;int[] carried=half(src,w,h),snapshot=carried.clone();StrongNoise1958.Model ordinary=StrongNoise1958.prepare(patches(src,w),w,h,4,true),cached=StrongNoise1958.prepare(patches(src,w),w,h,4,true,carried);sameModel(ordinary,cached);check(Arrays.equals(carried,snapshot),"prepared half modified");}
    }
    private static void workspace()throws Exception{
        int w=37,h=65;int[] src=source(w,h);StrongNoise1958.Model model=StrongNoise1958.prepare(patches(src,w),w,h,4,true);
        StrongNoise1958.Workspace workspace=new StrongNoise1958.Workspace(true);
        final AtomicInteger budget=new AtomicInteger(),detail=new AtomicInteger();StrongNoise1958.Protection protection=new StrongNoise1958.Protection(){public int budgetQ8(int x,int y){budget.incrementAndGet();return 256;}public int detailQ8(int x,int y){detail.incrementAndGet();return 0;}};
        try{int[] out=src.clone();StrongNoise1958.processRange(src,out,w,h,0,h,0,h,0,4,true,model,protection,workspace);check(budget.get()==w*h&&detail.get()==w*h,"policy evaluated more than once");int[] firstPolicy=workspace.policy(),firstConfidence=workspace.confidence();check(firstConfidence!=null,"aligned confidence missing");for(int y=0;y<h;y+=4)for(int x=0;x<w;x+=4){int yy=Math.min(h-1,y+1),xx=Math.min(w-1,x+1),expected=model.smoothingQ8(src,w,h,xx,yy,0,h,0);check(firstConfidence[(y/4)*((w+3)/4)+x/4]==expected,"shared guide confidence changed");}
            StrongNoise1958.processRange(src,out,w,h,0,h,0,h,0,4,true,model,protection,workspace);check(firstPolicy==workspace.policy()&&firstConfidence==workspace.confidence(),"stable worker buffers not reused");
            StrongNoise1958.processRange(src,out,w,h,1,h,0,h,0,4,true,model,protection,workspace);check(workspace.confidence()==null,"unaligned confidence should use oracle fallback");
        }finally{workspace.close();}
        final StrongNoise1958.Workspace exclusive=new StrongNoise1958.Workspace();final CountDownLatch inside=new CountDownLatch(1),release=new CountDownLatch(1);final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        final int[] input=src;final StrongNoise1958.Model m=model;
        Thread owner=new Thread(new Runnable(){public void run(){try{StrongNoise1958.processRange(input,input.clone(),37,65,0,65,0,65,0,4,true,m,new StrongNoise1958.Protection(){public int budgetQ8(int x,int y){if(x==0&&y==0){inside.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}}return 256;}public int detailQ8(int x,int y){return 0;}},exclusive);}catch(Throwable t){failure.set(t);}}});owner.start();
        check(inside.await(5,TimeUnit.SECONDS),"exclusive owner did not start");boolean rejected=false;try{StrongNoise1958.processRange(src,src.clone(),w,h,0,h,0,h,0,4,true,model,null,exclusive);}catch(IllegalStateException expected){rejected=true;}check(rejected,"simultaneous workspace lease accepted");rejected=false;try{exclusive.close();}catch(IllegalStateException expected){rejected=true;}check(rejected,"active workspace closed");release.countDown();owner.join(10000);check(!owner.isAlive()&&failure.get()==null,"workspace owner failed");exclusive.close();
    }
    private static void cancellation()throws Exception{
        final CountDownLatch entered=new CountDownLatch(1);final AtomicInteger active=new AtomicInteger();final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        SpeedWorkers1935.installHints(new SpeedWorkers1935.Hints(){public Runnable worker(Runnable task){return task;}public Object begin(long ns){active.incrementAndGet();entered.countDown();try{new CountDownLatch(1).await(10,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}return Boolean.TRUE;}public void complete(Object token,boolean success){active.decrementAndGet();}});
        Thread prepare=new Thread(new Runnable(){public void run(){try{int[] src=source(65,1025);StrongNoise1958.prepareJava(patches(src,65),65,1025,4,true);failure.set(new AssertionError("cancelled preparation succeeded"));}catch(Throwable cancelled){failure.set(cancelled);}}});
        try{prepare.start();check(entered.await(5,TimeUnit.SECONDS),"cancel preparation never entered pool");prepare.interrupt();prepare.join(10000);check(!prepare.isAlive(),"cancel preparation not drained");check(failure.get() instanceof RuntimeException,"cancel preparation not propagated");check(active.get()==0,"cancel returned before worker release");}finally{SpeedWorkers1935.installHints(null);prepare.interrupt();prepare.join(10000);}
    }
    public static void main(String[] args)throws Exception{preparation();workspace();cancellation();System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"parallelPreparationPeak\":"+parallelPeak+",\"exclusiveWorkspaceChecked\":true,\"drainedCancellationChecked\":true,\"carriedHalfExactChecked\":true,\"sharedConfidenceExactChecked\":true}");}
}
