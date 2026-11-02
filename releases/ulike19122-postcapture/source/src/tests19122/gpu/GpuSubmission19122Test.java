package com.hiro.ulike;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Real production Stage constructor/admission/release, with scalar policy peer.
 * No GPU performance or physical Android execution is represented. */
public final class GpuSubmission19122Test {
    static int assertions;
    static final Class<?> STAGE;
    static {try{STAGE=Class.forName("com.hiro.ulike.GpuStrong1960$Stage");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    static void require(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static Object get(Object object,String name)throws Exception{return field(object.getClass(),name).get(object);}
    static Object stage(int banks,long gap)throws Exception{
        CapturePolicy19122.banks=banks;CapturePolicy19122.gap=gap;
        Constructor<?> model=StrongNoise1958.Model.class.getDeclaredConstructor(int.class,int.class,int[].class,int[].class,int[].class,float[].class,int.class);model.setAccessible(true);
        Object m=model.newInstance(8,64,new int[1],new int[1],new int[1],new float[64],0);
        Constructor<?> constructor=STAGE.getDeclaredConstructor(StrongNoise1958.Model.class);constructor.setAccessible(true);return constructor.newInstance(m);
    }
    static Object call(String name,Class<?>[] types,Object...args)throws Exception{
        Method m=GpuStrong1960.class.getDeclaredMethod(name,types);m.setAccessible(true);
        try{return m.invoke(null,args);}catch(InvocationTargetException e){Throwable t=e.getCause();if(t instanceof Exception)throw (Exception)t;throw (Error)t;}
    }
    static int claim(Object s)throws Exception{synchronized(s){return ((Integer)call("claimPreferred1970",new Class<?>[]{STAGE,long.class},s,System.nanoTime())).intValue();}}
    static void release(Object s,int bank)throws Exception{synchronized(s){call("releaseBank1983",new Class<?>[]{STAGE,int.class,boolean.class},s,bank,false);}}
    static boolean[] banks(Object s)throws Exception{return (boolean[])get(s,"banks");}
    static void queued(Object s,int count)throws Exception{
        long end=System.nanoTime()+2000000000L;
        while(System.nanoTime()<end){synchronized(s){if(((Deque<?>)get(s,"preferredWaiters")).size()==count)return;}Thread.yield();}
        throw new AssertionError("waiter did not enter admission queue");
    }
    static void oneBankAndGap()throws Exception{
        final Object s=stage(1,2000000L);
        require((Integer)get(s,"bankLimit19122")==1,"normal stage snapshots one GPU bank");
        CapturePolicy19122.banks=2;CapturePolicy19122.gap=0;
        require((Integer)get(s,"bankLimit19122")==1&&(Long)get(s,"submitGap19122")==2000000L,"mid-stage policy cannot change admission");
        require(claim(s)==0,"first submit gets bank zero");
        final int[] result={-2};final long[] admitted={0};final Throwable[] error={null};
        Thread waiter=new Thread(new Runnable(){public void run(){try{result[0]=claim(s);admitted[0]=System.nanoTime();}catch(Throwable t){error[0]=t;}}});
        waiter.start();queued(s,1);require(result[0]==-2&&!banks(s)[1],"free second bank must not admit a second submit");
        long[] before=GpuStrong1960.submissionWaitTotals19122();long due;
        synchronized(s){release(s,0);due=(Long)get(s,"submitAfter19122");}
        waiter.join(2000);require(!waiter.isAlive()&&error[0]==null&&result[0]==0,"collected bank can be reused");
        require(admitted[0]>=due,"next submit admitted only after post-collection gap");
        long[] after=GpuStrong1960.submissionWaitTotals19122();require(after[0]>before[0]&&after[1]>before[1],"actual submit-side waiting recorded");
        after[0]=-1;require(GpuStrong1960.submissionWaitTotals19122()[0]>=0,"wait snapshot is defensive");
        release(s,0);require(!banks(s)[0]&&!banks(s)[1],"all bank ownership released");
    }
    @SuppressWarnings("unchecked") static void legacyAndBounds()throws Exception{
        Object s=stage(2,2000000L);require((Long)get(s,"submitGap19122")==0,"comparison two-bank path has no artificial gap");
        require(claim(s)==0&&claim(s)==1,"comparison allows two distinct banks");release(s,0);release(s,1);
        require((Long)get(s,"submitAfter19122")==0,"comparison release does not add gap");
        s=stage(99,99000000L);require((Integer)get(s,"bankLimit19122")==2,"invalid high bank policy bounded");
        s=stage(-1,99000000L);require((Integer)get(s,"bankLimit19122")==1&&(Long)get(s,"submitGap19122")==8000000L,"gap capped at eight milliseconds");
        s=stage(1,-5);require((Long)get(s,"submitGap19122")==0,"negative gap cannot block");
        ThreadLocal<Boolean> flag=(ThreadLocal<Boolean>)field(GpuResident1976.class,"BENCHMARK").get(null);
        flag.set(Boolean.TRUE);try{s=stage(1,8000000L);require((Integer)get(s,"bankLimit19122")==2&&(Long)get(s,"submitGap19122")==0,"proof benchmark remains unchanged");}finally{flag.remove();}
        flag=(ThreadLocal<Boolean>)field(GpuResident1976.class,"CPU").get(null);flag.set(Boolean.TRUE);try{s=stage(1,8000000L);require((Integer)get(s,"bankLimit19122")==2,"CPU oracle admission unaffected");}finally{flag.remove();}
    }
    static void fifoAndCancellation()throws Exception{
        final Object s=stage(1,2000000L);require(claim(s)==0,"FIFO owner initially holds bank");
        final List<Integer> order=Collections.synchronizedList(new ArrayList<Integer>());final Throwable[] error=new Throwable[2];
        Thread[] threads=new Thread[2];for(int i=0;i<2;i++){final int id=i;threads[i]=new Thread(new Runnable(){public void run(){try{int b=claim(s);if(b<0)throw new AssertionError("unexpected refusal");order.add(id);}catch(Throwable t){error[id]=t;}}});threads[i].start();queued(s,i+1);}
        release(s,0);threads[0].join(2000);require(order.equals(Arrays.asList(0))&&threads[1].isAlive(),"FIFO first waiter wins single bank");
        release(s,0);threads[1].join(2000);require(order.equals(Arrays.asList(0,1))&&error[0]==null&&error[1]==null,"second waiter waits for first bank release");release(s,0);
        final Object cancelled=stage(1,8000000L);field(STAGE,"submitAfter19122").setLong(cancelled,System.nanoTime()+1000000000L);
        final Throwable[] failure={null};final boolean[] interrupted={false};Thread t=new Thread(new Runnable(){public void run(){try{claim(cancelled);}catch(Throwable f){failure[0]=f;interrupted[0]=Thread.currentThread().isInterrupted();}}});
        t.start();queued(cancelled,1);t.interrupt();t.join(2000);
        require(!t.isAlive()&&failure[0] instanceof CancellationException&&interrupted[0],"interruption during submit gap cancels and preserves flag");
        require(((Deque<?>)get(cancelled,"preferredWaiters")).isEmpty()&&!banks(cancelled)[0]&&!banks(cancelled)[1],"cancelled wait owns no bank and leaves no FIFO entry");
        Object expired=stage(1,2000000L);require(claim(expired)==0,"deadline owner holds bank");field(STAGE,"residentDeadline1981").setLong(expired,System.nanoTime()+5000000L);
        require(claim(expired)==-1&&banks(expired)[0],"existing deadline can refuse without releasing another owner");release(expired,0);
        field(STAGE,"closed").setBoolean(expired,true);require(claim(expired)==-1,"closed stage cannot acquire bank");
    }
    public static void main(String[] args)throws Exception{
        oneBankAndGap();legacyAndBounds();fifoAndCancellation();
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_production_stage\":true,\"physical_android_tested\":false,\"gpu_execution_tested\":false}");
    }
}
