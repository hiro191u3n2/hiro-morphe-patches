package com.hiro.ulike;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual candidate JNI direct preparation against separately loaded .63 JNI.
 * Synthetic descriptors deliberately vary image alpha, halos, model bytes and
 * band boundaries. The direct scheduler tests include cancellation quiescence. */
public final class ResidualTransfer1964Test {
    private static int assertions,cases;private static long floats;
    private static final ByteOrder ORDER=ByteOrder.nativeOrder();
    private static native float[] prepareReference(int[] input,int width,int rows,int begin,int end,
        int validBegin,int validEnd,int originY,int noise,boolean shadows,int modelHeight,
        int columns,int modelRows,int patchWidth,int patchHeight,float[] modelData);
    private static void check(boolean okay,String why){assertions++;if(!okay)throw new AssertionError(why);}
    private static SingleNoise1955.Model model(int width,int height,int columns,int rows) {
        int cells=columns*rows;float[] data=new float[cells*3+88];
        for(int i=0;i<cells;i++){data[i]=2.5f+i*.17f;data[cells+i]=4.75f+i*.13f;data[cells*2+i]=110+i;}
        for(int i=cells*3;i<data.length;i++)data[i]=i<cells*3+16?3.25f+i*.03f:.75f+(i%7)*.05f;
        return new SingleNoise1955.Model(height,columns,rows,columns==1?width:Math.min(width-1,7),rows==1?height:Math.min(height-1,7),data);
    }
    private static int[] source(int width,int rows,long seed,boolean alpha) {
        int[] a=new int[width*rows];Random r=new Random(seed);
        for(int i=0;i<a.length;i++){int g=55+r.nextInt(101),red=Math.max(0,Math.min(255,g+r.nextInt(31)-15)),blue=Math.max(0,Math.min(255,g+r.nextInt(31)-15));a[i]=0xff000000|red<<16|g<<8|blue;}
        if(alpha)for(int i=3;i<a.length;i+=23)a[i]&=0x7fffffff;
        return a;
    }
    private static float[] reference(int[] a,int w,int rows,int lo,int hi,int vb,int ve,int origin,int noise,boolean shadows,SingleNoise1955.Model m) {
        return prepareReference(a,w,rows,lo,hi,vb,ve,origin,noise,shadows,m.height,m.columns,m.rows,m.patchWidth,m.patchHeight,m.data);
    }
    private static void same(ByteBuffer b,float[] expected,String why) {
        check(b!=null&&expected!=null&&b.isDirect()&&b.order()==ORDER&&b.position()==0&&b.remaining()==expected.length*4,why+" exact direct extent");
        for(int i=0;i<expected.length;i++)check(b.getInt(i*4)==Float.floatToRawIntBits(expected[i]),why+" raw float "+i);
        floats+=expected.length;cases++;
    }
    private static ByteBuffer cache(SingleResidual1961.Preparation p)throws Exception {Field f=SingleResidual1961.Preparation.class.getDeclaredField("cache");f.setAccessible(true);return (ByteBuffer)f.get(p);}
    private static void fixtures()throws Exception {
        int[][] shapes={{1,1,1,1,0},{5,7,1,1,0},{17,131,2,3,0},{65,193,3,4,0},{33,145,2,2,7}};
        for(int[] shape:shapes)for(int noise=1;noise<=4;noise++)for(boolean shadows:new boolean[]{false,true}) {
            int w=shape[0],h=shape[1],origin=shape[4];int[] a=source(w,h,19643435L+w+noise,h<8);
            int[] before=a.clone();SingleNoise1955.Model m=model(w,h+origin*2,shape[2],shape[3]);float[] md=m.data.clone();
            int begin=origin==0?0:7,end=origin==0?h:h-7;
            SingleResidual1961.Preparation p=new SingleResidual1961.Preparation(a,w,h,0,h,origin,noise,shadows,m);
            ByteBuffer c=cache(p);check(c.capacity()==SingleResidual1961.Preparation.cacheBytes(w,m.data.length),"bounded exact cache allocation");
            try {
                for(int lo=begin;lo<end;lo+=64){int hi=Math.min(end,lo+64);same(p.prepare(lo,hi),reference(a,w,h,lo,hi,0,h,origin,noise,shadows,m),"published63 "+w+"/"+h+"/"+lo+"/"+noise+"/"+shadows);}
                if(end-begin>64)check(c.getInt(18*4)>0,"actual overlapping8 block reuse on last band");
                check(Arrays.equals(a,before)&&Arrays.equals(m.data,md),"source/model immutable after real JNI");
            } finally {p.close();}
            boolean closed=false;try{p.prepare(begin,Math.min(end,begin+64));}catch(IllegalStateException okay){closed=true;}check(closed,"closed lease rejects new source read");
        }
        // One-row bands exercise reuse of both scales, including unaligned core.
        int w=17,h=131;int[] a=source(w,h,3,false);SingleNoise1955.Model m=model(w,h,2,2);
        SingleResidual1961.Preparation p=new SingleResidual1961.Preparation(a,w,h,0,h,0,4,true,m);
        try {
            same(p.prepare(40,41),reference(a,w,h,40,41,0,h,0,4,true,m),"one-row first");
            same(p.prepare(41,42),reference(a,w,h,41,42,0,h,0,4,true,m),"one-row second");
            check(cache(p).getInt(18*4)>0&&cache(p).getInt(19*4)>0,"both8/4 scales actually reused");
            a[42*w+5]^=0x00010307;
            same(p.prepare(42,43),reference(a,w,h,42,43,0,h,0,4,true,m),"changed overlap source recomputes");
            m.data[0]+=.125f;
            same(p.prepare(43,44),reference(a,w,h,43,44,0,h,0,4,true,m),"changed model recomputes");
            check(cache(p).getInt(18*4)==0&&cache(p).getInt(19*4)==0,"model byte change invalidates all cached blocks");
            same(p.prepare(97,98),reference(a,w,h,97,98,0,h,0,4,true,m),"nonadjacent range exact");
        } finally {p.close();}
    }
    private static void failures()throws Exception {
        int w=17,h=131;int[] a=source(w,h,7,false);SingleNoise1955.Model m=model(w,h,2,2);
        Method nativeMethod=SingleResidual1961.class.getDeclaredMethod("prepareDirectNative",ByteBuffer.class,ByteBuffer.class,int[].class,int.class,int.class,int.class,int.class,int.class,int.class,int.class,int.class,boolean.class,int.class,int.class,int.class,int.class,int.class,float[].class);nativeMethod.setAccessible(true);
        ByteBuffer c=ByteBuffer.allocateDirect((int)SingleResidual1961.Preparation.cacheBytes(w,m.data.length)).order(ORDER);
        int bytes=reference(a,w,h,0,64,0,h,0,4,true,m).length*4;
        ByteBuffer good=ByteBuffer.allocateDirect(bytes).order(ORDER);
        Object[] args={c,good,a,w,h,0,64,0,h,0,4,true,m.height,m.columns,m.rows,m.patchWidth,m.patchHeight,m.data};
        check((Boolean)nativeMethod.invoke(null,args),"direct JNI baseline accepted");
        for(ByteBuffer bad:new ByteBuffer[]{ByteBuffer.allocate(bytes),ByteBuffer.allocateDirect(bytes-4),ByteBuffer.allocateDirect(bytes+4)}) {
            args[1]=bad;check(!(Boolean)nativeMethod.invoke(null,args),"non-direct or wrong-size coefficient bank rejected");
        }
        args[1]=good;args[0]=ByteBuffer.allocateDirect(c.capacity()-1);check(!(Boolean)nativeMethod.invoke(null,args),"undersized cache rejected");
        ByteBuffer unaligned=ByteBuffer.allocateDirect(c.capacity()+1);unaligned.position(1);args[0]=unaligned.slice();check(!(Boolean)nativeMethod.invoke(null,args),"unaligned cache rejected");
        args[0]=c;args[4]=h+1;check(!(Boolean)nativeMethod.invoke(null,args),"source dimensions overrun rejected");args[4]=h;
        args[10]=0;check(!(Boolean)nativeMethod.invoke(null,args),"invalid noise rejected");args[10]=4;
        args[7]=1;check(!(Boolean)nativeMethod.invoke(null,args),"missing source halo rejected");args[7]=0;
        m.data[0]=Float.NaN;check(!(Boolean)nativeMethod.invoke(null,args),"nonfinite model rejected before record publication");m.data[0]=2.5f;
        check((Boolean)nativeMethod.invoke(null,args),"valid JNI call recovers after failures");
        float[] ref=reference(a,w,h,0,64,0,h,0,4,true,m);same(good,ref,"recovered exact direct JNI");
        SingleResidual1961.Preparation p=new SingleResidual1961.Preparation(a,w,h,0,h,0,4,true,m);
        try{Thread.currentThread().interrupt();boolean cancelled=false;try{p.prepare(0,64);}catch(CancellationException okay){cancelled=true;}check(cancelled,"interrupted direct preparation rejects before borrowing source");check(Thread.currentThread().isInterrupted(),"preparation preserves caller interrupt");}finally{Thread.interrupted();p.close();}
    }
    private static final ResidualOverlap1962.Guard GUARD=new ResidualOverlap1962.Guard(){public void check(){}};
    private static final class Ticket {final int[] snapshot;Ticket(ByteBuffer b){snapshot=new int[b.remaining()/4];for(int i=0;i<snapshot.length;i++)snapshot[i]=b.getInt(i*4);}}
    private static void directSchedule()throws Exception {
        final AtomicInteger readers=new AtomicInteger(),submissions=new AtomicInteger(),collections=new AtomicInteger();
        final ByteBuffer[] banks={ByteBuffer.allocateDirect(2*64*4).order(ORDER),ByteBuffer.allocateDirect(2*64*4).order(ORDER)};
        final int[] expected=new int[2*131];for(int i=0;i<expected.length;i++)expected[i]=i*7+3;
        final int[] next={0};
        ResidualOverlap1962.DirectDriver d=new ResidualOverlap1962.DirectDriver(){
            public ByteBuffer prepare(int lo,int hi){readers.incrementAndGet();try{ByteBuffer a=banks[next[0]];next[0]^=1;a.clear();a.limit((hi-lo)*2*4);for(int i=0;i<(hi-lo)*2;i++)a.putInt(i*4,expected[lo*2+i]);return a.slice().order(ORDER);}finally{readers.decrementAndGet();}}
            public Object submit(ByteBuffer b,int bank,int lo,int hi){check(bank==(submissions.getAndIncrement()%2),"direct banks alternate");Ticket t=new Ticket(b);for(int i=0;i<b.remaining()/4;i++)b.putInt(i*4,-99);return t;}
            public boolean collect(Object t,int bank,int count,int[] target,int at){collections.incrementAndGet();System.arraycopy(((Ticket)t).snapshot,0,target,at,count);return true;}
        };
        check(Arrays.equals(expected,ResidualOverlap1962.runDirect(2,0,131,64,d,GUARD)),"direct snapshot/reused-bank output exact");check(readers.get()==0&&submissions.get()==3&&collections.get()==3,"bounded direct schedule finishes every reader/ticket");
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),stopping=new CountDownLatch(1),done=new CountDownLatch(1);
        final AtomicBoolean cancelled=new AtomicBoolean(),interrupt=new AtomicBoolean();final Throwable[] error={null};
        Thread controller=new Thread(new Runnable(){public void run(){final Thread owner=Thread.currentThread();
            ResidualOverlap1962.DirectDriver blocked=new ResidualOverlap1962.DirectDriver(){
                public ByteBuffer prepare(int lo,int hi){readers.incrementAndGet();try{if(Thread.currentThread()!=owner){entered.countDown();for(;;)try{release.await();break;}catch(InterruptedException waiting){stopping.countDown();}}return ByteBuffer.allocateDirect((hi-lo)*8).order(ORDER);}finally{readers.decrementAndGet();}}
                public Object submit(ByteBuffer b,int bank,int lo,int hi){return new Ticket(b);}
                public boolean collect(Object t,int bank,int count,int[] target,int at){return true;}
            };
            try{ResidualOverlap1962.runDirect(2,0,131,64,blocked,GUARD);error[0]=new AssertionError("cancelled direct route returned");}catch(CancellationException okay){cancelled.set(true);interrupt.set(Thread.currentThread().isInterrupted());}catch(Throwable failed){error[0]=failed;}finally{done.countDown();}
        }},"direct-buffer-controller");
        controller.start();check(entered.await(5,TimeUnit.SECONDS),"actual direct worker started");controller.interrupt();check(stopping.await(5,TimeUnit.SECONDS),"direct worker receives cancel");check(readers.get()==1&&done.getCount()==1,"direct lease cannot close while JNI-like reader active");release.countDown();check(done.await(5,TimeUnit.SECONDS),"direct cancel joins reader");controller.join(5000);check(cancelled.get()&&interrupt.get()&&error[0]==null&&readers.get()==0,"direct cancel preserves interrupt and quiesces source");
    }
    public static void main(String[] args)throws Exception {System.load(args[0]);System.load(args[1]);fixtures();failures();directSchedule();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"record_floats\":"+floats+",\"reference_version\":\"1.9.63\",\"binary64_preparation_preserved\":true,\"direct_transfer_verified\":true,\"overlap_reuse_verified\":true,\"source_quiescence_verified\":true,\"physical_android_tested\":false}");}
}
