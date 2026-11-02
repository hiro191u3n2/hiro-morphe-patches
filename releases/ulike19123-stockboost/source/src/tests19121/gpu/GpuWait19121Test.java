package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.concurrent.ThreadFactory;

public final class GpuWait19121Test {
    static int assertions;
    static native void configure(int ticket,int zeros,boolean fail);
    static native int calls(int ticket);
    static native int timeouts();
    static void require(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static GpuNoise1960.Session session()throws Exception{Constructor<GpuNoise1960.Session> c=GpuNoise1960.Session.class.getDeclaredConstructor(long.class);c.setAccessible(true);return c.newInstance(1L);}
    static GpuNoise1960.Ticket ticket(GpuNoise1960.Session s,int bank,int id)throws Exception{GpuNoise1960.Ticket t=new GpuNoise1960.Ticket(s,bank,id);pending(s)[bank]=t;return t;}
    static GpuNoise1960.Ticket[] pending(GpuNoise1960.Session s)throws Exception{return (GpuNoise1960.Ticket[])field(GpuNoise1960.Session.class,"pending").get(s);}
    static boolean flag(Object object,String name)throws Exception{return field(object.getClass(),name).getBoolean(object);}
    static void pixels(int[] out,int offset){require(out!=null,"output exists");for(int i=0;i<8;i++)require(out[offset+i]==0x7f000000+i,"pixels unchanged");}
    static boolean collect(int kind,GpuNoise1960.Session s,GpuNoise1960.Ticket t,int[] out,long[] metrics){
        if(kind<2){int[][] got=kind==0?s.collect(t,new int[]{1},new int[]{8}):s.collectObserved19117(t,new int[]{1},new int[]{8},metrics);if(got==null)return false;System.arraycopy(got[0],0,out,2,8);return true;}
        if(kind==2)return s.collectInto(t,1,8,out,2);
        if(kind==3)return s.collectManyInto(t,new int[]{1},new int[]{8},new int[][]{out},new int[]{2});
        return s.collectManyIntoObserved19117(t,new int[]{1},new int[]{8},new int[][]{out},new int[]{2},metrics);
    }
    static void backoff()throws Exception{
        Class<?> type=Class.forName("com.hiro.ulike.GpuNoise1960$CompletionWait19121");Constructor<?> ctor=type.getDeclaredConstructor();ctor.setAccessible(true);
        Method pause=type.getDeclaredMethod("pause");pause.setAccessible(true);Field next=field(type,"nextNanos");
        Object wait=ctor.newInstance();long[] expected={250000L,500000L,1000000L,2000000L,2000000L,2000000L};
        Thread.currentThread().interrupt();try{for(long value:expected){require(next.getLong(wait)==value,"bounded backoff schedule");pause.invoke(wait);require(Thread.currentThread().isInterrupted(),"park preserves interruption");}}finally{Thread.interrupted();}
        require(next.getLong(ctor.newInstance())==250000L,"new collect resets backoff");
    }
    static void success()throws Exception{
        for(int kind=0;kind<5;kind++){
            GpuNoise1960.Session s=session();GpuNoise1960.Ticket t=ticket(s,0,kind+1),other=ticket(s,1,kind+10);configure(kind+1,6,false);
            long[] before=GpuNoise1960.completionWaitTotals19121(),metrics=new long[6];int[] out=new int[12];Arrays.fill(out,123);
            require(collect(kind,s,t,out,metrics),"collect variant "+kind);pixels(out,2);require(out[0]==123&&out[1]==123&&out[10]==123&&out[11]==123,"destination guards");
            require(pending(s)[0]==null&&pending(s)[1]==other,"only collected bank released");require(flag(t,"collected")&&!flag(t,"collecting")&&!flag(s,"failed"),"ownership success");
            long[] after=GpuNoise1960.completionWaitTotals19121();require(after[0]-before[0]==1&&after[1]-before[1]==7&&after[2]-before[2]==6&&after[3]>before[3],"completed counters");
            long[] copy=GpuNoise1960.completionWaitTotals19121();copy[0]=-999;require(GpuNoise1960.completionWaitTotals19121()[0]==after[0],"snapshot defensive copy");
            if(kind==1||kind==4)require(metrics[0]>=metrics[1]&&metrics[1]>=0&&metrics[2]>=0&&metrics[3]==11&&metrics[4]==22&&metrics[5]==33,"original six timing fields");
            require(!collect(kind,s,t,out,metrics)&&calls(kind+1)==7,"consumed ticket refused");
        }
        require(SpeedWorkers1935.entered.get()==1&&"Hiro-ULike-GX1960".equals(SpeedWorkers1935.ownerName),"baseline owner wrapped once");
        Class<?> type=Class.forName("com.hiro.ulike.GpuNoiseOptimized19119$OwnerFactory");Constructor<?> ctor=type.getDeclaredConstructor();ctor.setAccessible(true);ThreadFactory factory=(ThreadFactory)ctor.newInstance();
        Thread thread=factory.newThread(new Runnable(){public void run(){}});require(thread.isDaemon(),"optional owner daemon");thread.start();thread.join();
        require(SpeedWorkers1935.entered.get()==2&&"ULike-GPU-optimized19119".equals(SpeedWorkers1935.ownerName),"optional owner wrapped once");
    }
    static void failures()throws Exception{
        for(int kind=0;kind<5;kind++){
            GpuNoise1960.Session s=session();GpuNoise1960.Ticket t=ticket(s,0,kind+20);configure(kind+20,-2,false);int[] out=new int[12];Arrays.fill(out,123);
            require(!collect(kind,s,t,out,new long[6]),"ready failure");require(flag(s,"failed")&&!flag(t,"collecting")&&!flag(t,"collected")&&pending(s)[0]==t,"failed bank retained");
            require(out[2]==123,"failed query never writes");
        }
        GpuNoise1960.Session s=session();GpuNoise1960.Ticket t=ticket(s,0,30);configure(30,2,true);int[] out=new int[12];Arrays.fill(out,123);
        require(!collect(4,s,t,out,new long[6])&&flag(s,"failed")&&pending(s)[0]==t,"read failure retains bank");require(out[2]==123,"fixture read failure uncommitted");
        s=session();t=ticket(s,0,31);configure(31,0,false);Thread.currentThread().interrupt();try{require(!collect(2,s,t,out,new long[6])&&calls(31)==0,"preinterrupt refuses collection");require(!flag(t,"collecting")&&!flag(t,"collected"),"preinterrupt keeps ticket ownership");}finally{Thread.interrupted();}
        final GpuNoise1960.Session interrupted=session();final GpuNoise1960.Ticket blocked=ticket(interrupted,0,32);configure(32,-1,false);final boolean[] result={true},interrupt={false};
        Thread caller=new Thread(new Runnable(){public void run(){result[0]=collect(4,interrupted,blocked,new int[12],new long[6]);interrupt[0]=Thread.currentThread().isInterrupted();}});
        caller.start();Thread.sleep(30);caller.interrupt();caller.join(2000);require(!caller.isAlive()&&!result[0]&&interrupt[0],"in-flight interrupt exits and preserves flag");
        require(flag(interrupted,"failed")&&!flag(blocked,"collecting")&&pending(interrupted)[0]==blocked,"interrupted pending bank retained");
        s=session();t=ticket(s,0,33);configure(33,-1,false);long began=System.nanoTime();require(!collect(2,s,t,new int[12],new long[6]),"deadline returns failure");long elapsed=System.nanoTime()-began;
        require(elapsed>=5000000000L&&elapsed<9000000000L&&timeouts()==1,"original five second deadline");require(pending(s)[0]==t&&!flag(t,"collecting")&&flag(s,"failed"),"timed out bank retained");
    }
    public static void main(String[] args)throws Exception{
        System.loadLibrary("wait19121");field(GpuNoise1960.class,"loaded").setInt(null,1);field(GpuNoise1960.class,"environment").set(null,"host-transport-fixture");
        backoff();success();failures();System.out.println("RESULT assertions="+assertions+" physicalAndroid=false GLES=false");
    }
}
