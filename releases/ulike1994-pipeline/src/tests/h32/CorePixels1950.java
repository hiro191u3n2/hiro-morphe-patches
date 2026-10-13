package com.hiro.ulike;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/** Raw production JNI boundary tested on Mesa; the application wrapper has its own suite. */
public final class CorePixels1950 {
    static { System.loadLibrary("h32host1956"); }
    private static native boolean bilateralPairGpuNative(int[] source,int[] output,int[] horizontal,
        int width,int rows,int noise,boolean texture,boolean shadows,int begin,int end,int[] lr,int[] cc);
    private static native long retainedNativeScratchBytes();
    private static native void trimNativeScratch();
    private static native long control(int mode);
    static final int SENTINEL=0xa5a5a5a5;
    static final AtomicInteger checks=new AtomicInteger(), cases=new AtomicInteger();
    static final AtomicLong pixels=new AtomicLong();
    static void check(boolean condition,String detail){checks.incrementAndGet();if(!condition)throw new AssertionError(detail);}
    static final class Fixture {
        int width,rows,noise,begin,end;boolean texture,shadows;
        int[] source,lr,cc,expected,halo;
        Fixture(DataInputStream in)throws Exception {
            width=in.readInt();rows=in.readInt();noise=in.readInt();texture=in.readInt()!=0;
            shadows=in.readInt()!=0;begin=in.readInt();end=in.readInt();
            source=read(in,width*rows);lr=read(in,1280);cc=read(in,1280);
            expected=read(in,width*rows);halo=read(in,width*rows);
        }
        int[] read(DataInputStream in,int size)throws Exception {int[] a=new int[size];for(int i=0;i<size;i++)a[i]=in.readInt();return a;}
        void run(boolean success){
            int[] original=source.clone(),input=source.clone(),output=new int[source.length],horizontal=new int[source.length];
            Arrays.fill(output,SENTINEL);Arrays.fill(horizontal,SENTINEL);
            boolean ok=bilateralPairGpuNative(input,output,horizontal,width,rows,noise,texture,shadows,begin,end,lr,cc);
            check(ok==success,"GPU admission/failure differs");
            check(Arrays.equals(input,original),"source mutated");
            if(success){check(Arrays.equals(output,expected),"pixels differ");check(Arrays.equals(horizontal,halo),"halo differs");}
            else {for(int p:output)check(p==SENTINEL,"partial output commit");for(int p:horizontal)check(p==SENTINEL,"partial halo commit");}
            pixels.addAndGet(source.length);cases.incrementAndGet();
        }
        long bytes(){return 4L*width*(rows+end-begin+begin-Math.max(0,begin-3)+Math.min(rows,end+3)-end);}
    }
    public static void main(String[] args)throws Exception {
        ArrayList<Fixture> fixtures=new ArrayList<>();
        try(DataInputStream in=new DataInputStream(new FileInputStream(args[0]))){int count=in.readInt();for(int i=0;i<count;i++)fixtures.add(new Fixture(in));}
        trimNativeScratch();check(retainedNativeScratchBytes()==0,"initial pool");
        Fixture small=fixtures.get(0),large=fixtures.get(fixtures.size()-1);
        small.run(true);long count=control(0),bytes=retainedNativeScratchBytes();
        check(bytes==small.bytes(),"retained exact allocation");
        for(int i=0;i<12;i++)small.run(true);
        check(control(0)==count,"same dimensions allocated again");
        large.run(true);check(control(0)==count+1,"growth did not allocate once");
        check(retainedNativeScratchBytes()==large.bytes(),"growth accounting");
        small.run(true);check(control(0)==count+1,"smaller frame allocated");
        for(Fixture f:fixtures)f.run(true);
        control(2);small.run(false);small.run(true);
        trimNativeScratch();control(1);small.run(false);check(retainedNativeScratchBytes()==0,"failed allocation retained");small.run(true);
        int[] alias=small.source.clone(),out=small.source.clone();
        count=control(0);
        check(!bilateralPairGpuNative(alias,alias,out,small.width,small.rows,small.noise,small.texture,small.shadows,small.begin,small.end,small.lr,small.cc),"alias admitted");
        check(control(0)==count,"alias allocated");
        int old=small.lr[small.noise*256];small.lr[small.noise*256]=257;small.run(false);small.lr[small.noise*256]=old;
        final AtomicReference<Throwable> failure=new AtomicReference<>();
        CountDownLatch start=new CountDownLatch(1);Thread[] threads=new Thread[8];
        for(int t=0;t<threads.length;t++){final int index=t;threads[t]=new Thread(()->{try{start.await();for(int i=0;i<16;i++)fixtures.get((index*7+i)%fixtures.size()).run(true);}catch(Throwable e){failure.compareAndSet(null,e);}});threads[t].start();}
        start.countDown();for(Thread t:threads)t.join();if(failure.get()!=null)throw new AssertionError("parallel lease",failure.get());
        control(3);Thread paused=new Thread(()->{try{large.run(true);}catch(Throwable e){failure.set(e);}});paused.start();
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(control(4)==0&&System.nanoTime()<deadline)Thread.sleep(1);
        check(control(4)==1,"GPU fence not paused");
        ExecutorService ui=Executors.newSingleThreadExecutor();
        try{
            Future<Long> request=ui.submit(()->{long retained=retainedNativeScratchBytes();trimNativeScratch();return retained;});
            check(request.get(1,TimeUnit.SECONDS)>0,"active scratch unaccounted");
        } finally {control(5);paused.join();ui.shutdownNow();}
        if(failure.get()!=null)throw new AssertionError("active trim",failure.get());
        check(retainedNativeScratchBytes()==0,"pending trim lost");
        small.run(true);control(6);check(retainedNativeScratchBytes()==0,"idle trim");
        check(control(7)==1,"oversize ephemeral retained/counted incorrectly");
        small.run(true);trimNativeScratch();check(retainedNativeScratchBytes()==0,"final trim");
        System.out.println("H32_PASS assertions="+checks.get()+" cases="+cases.get()+" pixels="+pixels.get()+" concurrent=128");
    }
}
