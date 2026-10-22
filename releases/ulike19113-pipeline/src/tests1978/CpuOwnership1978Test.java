package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Actual production C/JNI with allocation-failure observation supplied only by
 * the generated test translation unit. The production native source is intact. */
public final class CpuOwnership1978Test {
    static {System.loadLibrary("ulike_nr1955");}
    static native void fault(int allocation);static native long nativeOwners();
    static long checks,compared;static final Class<?> OWNER=CpuSingle1978.class;
    static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    static Method method(String name,Class<?>... types)throws Exception{Method m=OWNER.getDeclaredMethod(name,types);m.setAccessible(true);return m;}
    static Object call(String name,Class<?>[] types,Object... args)throws Exception{
        try{return method(name,types).invoke(null,args);}catch(InvocationTargetException e){Throwable t=e.getCause();if(t instanceof Exception)throw (Exception)t;if(t instanceof Error)throw (Error)t;throw e;}
    }
    static long create(int w,int h)throws Exception{return (Long)call("createWorkspaceNative",new Class<?>[]{int.class,int.class},w,h);}
    static boolean begin(long id)throws Exception{return (Boolean)call("beginWorkspaceNative",new Class<?>[]{long.class},id);}
    static void end(long id)throws Exception{call("endWorkspaceNative",new Class<?>[]{long.class},id);}
    static void release(long id)throws Exception{call("releaseWorkspaceNative",new Class<?>[]{long.class},id);}
    static void drained(){check(nativeOwners()==0,"all native slots/bytes/active leases drained");check(SpeedWorkers1935.nativeRetainedBytes1956()==0,"Java native reservation drained");}
    static int[] source(int w,int h,int seed){Random r=new Random(seed);int[] p=new int[w*h];for(int i=0;i<p.length;i++){p[i]=0xff000000|r.nextInt(0xffffff);if(i%97==0)p[i]&=0x7fffffff;}return p;}
    static float[] model(){float[] a=new float[91];Arrays.fill(a,1.2f);a[0]=6.3f;a[1]=9.7f;a[2]=112.4f;return a;}
    static int[] geometry(int w,int h){return new int[]{w,h,0,h,0,h,0,4,1,w,h,1,1,w,h};}
    static boolean original(int[] input,int[] output,int[] u,float[] m){return ColourCache1976.cpu(input,output,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[7],u[8]!=0,u[9],u[10],u[11],u[12],u[13],u[14],m,null);}
    static void same(int[] a,int[] b,String why){check(Arrays.equals(a,b),why);compared+=a.length;}
    static void handles()throws Exception {
        check(create(0,64)==0&&create(7,257)==0&&create(Integer.MAX_VALUE,256)==0,"invalid native capacity refused");
        long[] all=new long[4];for(int i=0;i<4;i++){all[i]=create(43+i,71);check(all[i]>0,"bounded slot acquired");}check(create(8,8)==0,"fifth owner refused");
        for(long id:all)release(id);drained();
        long old=create(43,71);release(old);long fresh=create(43,71);check(fresh!=old&&!begin(old),"retired generation cannot address new owner");release(old);check(begin(fresh),"stale release cannot free fresh slot");check(!begin(fresh),"same-thread nested lease refused");
        AtomicReference<Throwable> failure=new AtomicReference<Throwable>();final long id=fresh;
        Thread foreign=new Thread(()->{try{check(!begin(id),"foreign active lease refused");end(id);release(id);}catch(Throwable t){failure.set(t);}});
        foreign.start();foreign.join();if(failure.get()!=null)throw new AssertionError(failure.get());
        check((nativeOwners()>>>40)==1,"foreign release retires but does not free active storage");
        int[] src=source(43,71,1),a=new int[src.length],b=new int[src.length],u=geometry(43,71);float[] m=model();
        check(original(src,a,u,m),"retired active storage remains valid until owner end");end(id);check(!begin(id),"retired handle unavailable after drain");drained();
        check(original(src,b,u,m),"original fallback after scope");same(a,b,"foreign release cannot change private pixel result");
    }
    static void allocation()throws Exception {
        for(int fail=1;fail<=3;fail++) {
            fault(fail);check(CpuSingle1978.create(67,81)==0,"partial native allocation declined "+fail);fault(0);drained();
        }
        Field held=GpuNoise1960.class.getDeclaredField("retained"),busy=GpuNoise1960.class.getDeclaredField("activeSession");held.setAccessible(true);busy.setAccessible(true);
        long previous=held.getLong(null);boolean active=busy.getBoolean(null);
        try{busy.setBoolean(null,true);held.setLong(null,Long.MAX_VALUE);check(CpuSingle1978.create(67,81)==0,"physical memory refusal before native allocation");}
        finally{held.setLong(null,previous);busy.setBoolean(null,active);}
        drained();
    }
    static void scope()throws Exception {
        int w=43,h=71;int[] src=source(w,h,3),original=src.clone(),u=geometry(w,h),expected=new int[src.length],actual=new int[src.length];float[] m=model();
        check(original(src,expected,u,m),"baseline JNI available");SingleNoise1955.Workspace workspace=new SingleNoise1955.Workspace();
        workspace.acquire();try {
            check(CpuSingle1978.nativeCall(src,actual,u,m,null,workspace),"candidate production JNI succeeds");same(expected,actual,"complete native output exact");
            boolean rejected=false;try{workspace.close();}catch(IllegalStateException yes){rejected=true;}check(rejected,"busy Java workspace cannot be closed");
            long handle=workspace.nativeHandle(w,h);check(begin(handle),"successful call restored thread-local scope");end(handle);
            int[] truncated=Arrays.copyOf(u,4);boolean exceptional=false;
            try{CpuSingle1978.nativeCall(src,actual,truncated,m,null,workspace);}catch(ArrayIndexOutOfBoundsException expectedException){exceptional=true;}
            check(exceptional&&begin(handle),"Java exception after begin restores thread-local scope");end(handle);
            Arrays.fill(actual,0x13579bdf);int[] untouched=actual.clone(),bad=u.clone();bad[4]=1;
            check(!CpuSingle1978.nativeCall(src,actual,bad,m,null,workspace),"bad geometry declines JNI");same(actual,untouched,"failed JNI does not commit private output");
            check(begin(handle),"failed JNI restored lease");end(handle);
            Thread.currentThread().interrupt();check(CpuSingle1978.nativeCall(src,actual,u,m,null,workspace),"native call drains its complete transaction when interrupted");Thread.interrupted();same(expected,actual,"interrupted JNI still atomic private result");
        }finally{Thread.interrupted();workspace.relinquish();workspace.close();}
        same(src,original,"source never modified by lease/error/cancel");drained();
        boolean closed=false;try{workspace.acquire();}catch(IllegalStateException yes){closed=true;}check(closed,"closed workspace cannot be re-leased");
    }
    static void parallel()throws Exception {
        final int workers=4;CountDownLatch ready=new CountDownLatch(workers),held=new CountDownLatch(workers),start=new CountDownLatch(1),close=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<Throwable>();AtomicLong pixels=new AtomicLong();Thread[] threads=new Thread[workers];
        for(int i=0;i<workers;i++){final int index=i;threads[i]=new Thread(()->{
            SingleNoise1955.Workspace workspace=new SingleNoise1955.Workspace();
            try{ready.countDown();start.await();int w=43+index,h=73;
                for(int trial=0;trial<6;trial++) {
                    int[] src=source(w,h,1978+index*31+trial),copy=src.clone(),expected=new int[src.length],actual=new int[src.length],u=geometry(w,h);float[] m=model();
                    if((trial&1)!=0)u[3]=53;
                    if(!original(src,expected,u,m))throw new AssertionError("parallel original unavailable");
                    workspace.acquire();try{if(!CpuSingle1978.nativeCall(src,actual,u,m,null,workspace))throw new AssertionError("parallel candidate unavailable");}finally{workspace.relinquish();}
                    if(!Arrays.equals(expected,actual)||!Arrays.equals(src,copy))throw new AssertionError("shared/stale accumulation or source corruption");pixels.addAndGet(src.length);
                }
                held.countDown();close.await();
            }catch(Throwable t){failure.compareAndSet(null,t);held.countDown();}
            finally{try{workspace.close();}catch(Throwable t){failure.compareAndSet(null,t);}}
        });threads[i].start();}
        check(ready.await(5,TimeUnit.SECONDS),"four workers ready");start.countDown();check(held.await(20,TimeUnit.SECONDS),"four workers completed private ranges");
        if(failure.get()==null){check((nativeOwners()>>>32&255)==4,"four isolated shot owners remain until barrier");check(SpeedWorkers1935.nativeRetainedBytes1956()>0,"all active shot capacity is charged");SpeedWorkers1935.trim();check((nativeOwners()>>>32&255)==4,"trim never revokes owned shot storage");}
        close.countDown();for(Thread t:threads)t.join();if(failure.get()!=null)throw new AssertionError(failure.get());compared+=pixels.get();drained();
    }
    public static void main(String[] args)throws Exception {
        handles();allocation();scope();parallel();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+(checks+compared)+",\"pixels_compared\":"+compared+",\"cpu78_native_lease_fault_cancel_verified\":true,\"cpu78_native_foreign_release_and_generation_verified\":true,\"cpu78_native_budget_and_stage_close_verified\":true,\"cpu78_four_exclusive_workers_verified\":true}");
    }
}
