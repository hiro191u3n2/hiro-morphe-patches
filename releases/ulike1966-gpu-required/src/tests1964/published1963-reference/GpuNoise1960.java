package com.hiro.ulike;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.ThreadFactory;

/** GX1-GX8 real compute engine. Every candidate remains private until the exact
 * whole-route owner admits it. No precision substitute is accepted by this API.
 * One private EGL-owner thread; per-photo persistent GPU SSBOs; bounded memory.
 * Camera/native codec/GPU interop is separately capability checked, never assumed. */
public final class GpuNoise1960 {
    private GpuNoise1960() { }
    public static final int STRONG=0,SINGLE=1,ANALYSIS=2,GEOMETRY=3,
        ANALYSIS1961=4,RESIDUAL1961=5,PROTECTION1961=6,COMPARE1961=7,FINISH1961=8;
    static final int SHADERS=9,VARIANTS=3;
    // GX27: preserve published0..26 IDs, append four mode-specific strong
    // programs for each layout; mode0half,1quarter,2eighth,3full.
    static final int STRONG_PROGRAM_BASE=SHADERS*VARIANTS,PROGRAMS=STRONG_PROGRAM_BASE+4*VARIANTS;
    static int variant(int shader,int choice){return shader>=0&&shader<SHADERS&&choice>=0&&choice<VARIANTS?shader+choice*SHADERS:-1;}
    static int strongProgram(int mode,int choice){return mode>=0&&mode<4&&choice>=0&&choice<VARIANTS?STRONG_PROGRAM_BASE+mode+choice*4:-1;}
    public static final long MAX_BYTES=512L*1024*1024;
    private static volatile int loaded;
    private static volatile long knownSupported,knownUnsupported;
    private static volatile long retained;
    private static volatile boolean activeSession;
    private static volatile String environment="";
    private static final ExecutorService OWNER=Executors.newSingleThreadExecutor(new ThreadFactory(){
        public Thread newThread(Runnable task){Thread t=new Thread(task,"Hiro-ULike-GX1960");t.setDaemon(true);return t;}
    });
    static boolean available(){
        if(loaded==0)synchronized(GpuNoise1960.class){if(loaded==0){
            try{System.loadLibrary("ulike_gpu1960");loaded=nativeAbi()==19601?1:-1;}
            catch(LinkageError unavailable){loaded=-1;}
            catch(SecurityException unavailable){loaded=-1;}
        }}
        return loaded>0;
    }
    public static long retainedBytes(){return retained;}
    static boolean sessionBusy(){return activeSession;}
    /** Native GPU allocations share the capture's physical memory allowance.
     * The heap figures already include all live Java buffers; callers pass only
     * their additional peak allocation, including private qualification results. */
    static boolean workspaceFits(long extra){
        if(extra<0||extra>MAX_BYTES)return false;
        if(extra<=freeBudget())return true;
        if(!activeSession&&retained>0){trimIdle();return extra<=freeBudget();}
        return false;
    }
    private static long freeBudget(){
        Runtime r=Runtime.getRuntime();
        long available=r.maxMemory()-(r.totalMemory()-r.freeMemory());
        long[] owners={retained,WholeRoute1953.retainedBytes(),GpuFinish1953.retainedBytes(),
            SpeedWorkers1935.nativeRetainedBytes1956(),64L*1024*1024};
        for(long bytes:owners){if(bytes<0||bytes>available)return 0;available-=bytes;}
        return available;
    }
    static String fingerprint(){return environment;}
    private static void refresh(){
        retained=retainedNative();
        // The private native context is initialized once and quarantined on a
        // fatal driver error. Its fingerprint cannot change during this process;
        // avoid another JNI String allocation for every strip/bank command.
        if(environment.length()==0){String e=environmentNative();environment=e==null?"":e;}
    }
    private static <T> T owner(Callable<T> task,T fallback){
        Future<T> f;
        try{f=OWNER.submit(task);}catch(RuntimeException unavailable){return fallback;}catch(OutOfMemoryError unavailable){return fallback;}
        boolean interrupted=false;
        try{for(;;)try{return f.get();}catch(InterruptedException cancelled){interrupted=true;}catch(ExecutionException failed){
            if(failed.getCause() instanceof LinkageError)loaded=-1;return fallback;
        }}finally{if(interrupted)Thread.currentThread().interrupt();}
    }
    static void warmupAsync(){
        if(!available())return;
        try{OWNER.execute(new Runnable(){public void run(){try{supportedNative(STRONG);refresh();}catch(LinkageError unavailable){loaded=-1;}catch(RuntimeException unavailable){}}});}
        catch(RuntimeException unavailable){}catch(OutOfMemoryError unavailable){}
    }
    static boolean supports(final int shader){
        if(shader<0||shader>=PROGRAMS||!available()||Thread.currentThread().isInterrupted())return false;
        long bit=1L<<shader;if((knownSupported&bit)!=0)return true;if((knownUnsupported&bit)!=0)return false;
        boolean result=owner(new Callable<Boolean>(){public Boolean call(){try{return supportedNative(shader);}finally{refresh();}}},Boolean.FALSE);
        synchronized(GpuNoise1960.class){if(result)knownSupported|=bit;else knownUnsupported|=bit;}return result;
    }
    static int workgroup(final int shader){
        if(shader<0||shader>=PROGRAMS||!available()||Thread.currentThread().isInterrupted())return 0;
        return owner(new Callable<Integer>(){public Integer call(){return workgroupNative(shader);}},Integer.valueOf(0));
    }
    static Session open(){
        if(!available()||Thread.currentThread().isInterrupted())return null;
        return owner(new Callable<Session>(){public Session call(){
            long token=openNative();refresh();if(token==0)return null;activeSession=true;
            boolean passed=false;try{Session s=new Session(token);passed=true;return s;}finally{if(!passed){try{closeNative(token);}finally{activeSession=false;refresh();}}}
        }},null);
    }
    /** GX18 immutable command descriptors; upload arrays belong to caller and
     * must stay unchanged until run/submit/execute returns. Uniforms are copied
     * when recorded. Upload bytes are copied before an asynchronous ticket returns. */
    static final class Batch {
        private final ArrayList<Command> commands=new ArrayList<Command>();
        Batch allocate(int slot,long bytes){commands.add(new Command(0,slot,bytes,0,null,null,null,null,0));return this;}
        Batch upload(int slot,int[] values){commands.add(new Command(1,slot,0,0,values,null,null,null,0));return this;}
        Batch upload(int slot,float[] values){commands.add(new Command(2,slot,0,0,values,null,null,null,0));return this;}
        /** GX26 optional mapped inputs; source ownership lasts through submit. */
        Batch uploadDirect(int slot,int[] values){commands.add(new Command(4,slot,0,0,values,null,null,null,0));return this;}
        Batch uploadDirect(int slot,float[] values){commands.add(new Command(5,slot,0,0,values,null,null,null,0));return this;}
        Batch dispatch(int shader,int[] bindings,int[] u,float[] f,int count){
            commands.add(new Command(3,0,0,shader,null,bindings==null?null:bindings.clone(),u==null?null:u.clone(),f==null?null:f.clone(),count));return this;
        }
        private Packet packet(){return new Packet(commands);}
    }
    private static final class Command {
        final int kind,slot,shader,count;final long bytes;final Object payload;
        final int[] bindings,u;final float[] f;
        Command(int k,int s,long b,int p,Object a,int[] v,int[] i,float[] n,int c){kind=k;slot=s;bytes=b;shader=p;payload=a;bindings=v;u=i;f=n;count=c;}
    }
    private static final class Packet {
        final int[] kinds,slots,shaders,counts;final long[] bytes;final Object[] payloads;
        final int[][] bindings,u;final float[][] f;
        Packet(ArrayList<Command> list){
            int n=list.size();if(n<1||n>128)throw new IllegalArgumentException("GPU batch commands");
            kinds=new int[n];slots=new int[n];shaders=new int[n];counts=new int[n];bytes=new long[n];payloads=new Object[n];bindings=new int[n][];u=new int[n][];f=new float[n][];
            for(int i=0;i<n;i++){Command c=list.get(i);kinds[i]=c.kind;slots[i]=c.slot;shaders[i]=c.shader;counts[i]=c.count;bytes[i]=c.bytes;payloads[i]=c.payload;bindings[i]=c.bindings;u[i]=c.u;f[i]=c.f;}
        }
        boolean run(long token){return batchNative(token,kinds,slots,bytes,shaders,payloads,bindings,u,f,counts);}
    }
    static final class Ticket {
        final Session session;final int bank;long nativeTicket;private boolean collected,collecting;
        Ticket(Session s,int b,long n){session=s;bank=b;nativeTicket=n;}
    }
    static final class Session implements AutoCloseable {
        private final long token;
        private boolean failed,closed;
        private long dispatched;
        private final Ticket[] pending=new Ticket[2];
        private Session(long token){this.token=token;}
        Batch newBatch(){return new Batch();}
        private boolean usable(){return !closed&&!failed&&!Thread.currentThread().isInterrupted();}
        synchronized boolean allocate(final int slot,final long bytes){
            if(!usable()||slot<0||slot>=24||bytes<=0||bytes>MAX_BYTES)return false;
            boolean ok=owner(new Callable<Boolean>(){public Boolean call(){try{return allocateNative(token,slot,bytes);}finally{refresh();}}},Boolean.FALSE);
            if(!ok)failed=true;return ok;
        }
        synchronized boolean upload(final int slot,final int[] values){
            if(!usable()||slot<0||slot>=24||values==null||values.length==0||(long)values.length*4>MAX_BYTES)return false;
            boolean ok=owner(new Callable<Boolean>(){public Boolean call(){try{return uploadIntsNative(token,slot,values);}finally{refresh();}}},Boolean.FALSE);
            if(!ok)failed=true;return ok;
        }
        synchronized boolean upload(final int slot,final float[] values){
            if(!usable()||slot<0||slot>=24||values==null||values.length==0||(long)values.length*4>MAX_BYTES)return false;
            boolean ok=owner(new Callable<Boolean>(){public Boolean call(){try{return uploadFloatsNative(token,slot,values);}finally{refresh();}}},Boolean.FALSE);
            if(!ok)failed=true;return ok;
        }
        /** u[31] is reserved for the native invocation offset. Kernel workgroup width is read from its linked program; Y/Z must be1.
         * Each kernel explicitly bounds total pixels/blocks in its parameters. */
        synchronized boolean dispatch(final int shader,final int[] bindings,final int[] u,final float[] f,final int invocations){
            if(!usable()||shader<0||shader>=PROGRAMS||bindings==null||bindings.length<1||bindings.length>8||u==null||u.length<1||u.length>32||f!=null&&f.length>32||invocations<1||(long)invocations>MAX_BYTES/4)return false;
            for(int slot:bindings)if(slot<0||slot>=24)return false;
            boolean ok=owner(new Callable<Boolean>(){public Boolean call(){return dispatchNative(token,shader,bindings,u,f,invocations);}},Boolean.FALSE);
            if(!ok)failed=true;else dispatched+=invocations;return ok;
        }
        synchronized int[] readInts(final int slot,final int count){
            if(!usable()||slot<0||slot>=24||count<1||(long)count*4>MAX_BYTES)return null;
            int[] result=owner(new Callable<int[]>(){public int[] call(){return readIntsNative(token,slot,count);}},null);
            if(result==null)failed=true;return result;
        }
        synchronized boolean run(Batch batch){
            if(!usable()||batch==null)return false;
            final Packet p=batch.packet();
            boolean ok=owner(new Callable<Boolean>(){public Boolean call(){try{return p.run(token);}finally{refresh();}}},Boolean.FALSE);
            if(!ok)failed=true;return ok;
        }
        /** GX18 one owner/JNI command graph followed by one drain and all private
         * readbacks. A late readback failure discards the complete result array. */
        synchronized int[][] execute(Batch batch,final int[] slots,final int[] counts){
            if(!usable()||batch==null||!readsValid(slots,counts))return null;
            final Packet p=batch.packet();
            int[][] result=owner(new Callable<int[][]>(){public int[][] call(){try{return executeNative(token,p.kinds,p.slots,p.bytes,p.shaders,p.payloads,p.bindings,p.u,p.f,p.counts,slots,counts);}finally{refresh();}}},null);
            if(result==null)failed=true;return result;
        }
        /** GX26 write into an already allocated private candidate. On false the
         * target must be discarded; it is never a published/borrowed bitmap. */
        synchronized boolean executeInto(Batch batch,final int slot,final int count,final int[] target,final int offset){
            if(!usable()||batch==null||!targetValid(slot,count,target,offset))return false;
            final Packet p=batch.packet();
            boolean ok=owner(new Callable<Boolean>(){public Boolean call(){try{
                return p.run(token)&&readIntoNative(token,0,slot,count,target,offset);
            }finally{refresh();}}},Boolean.FALSE);
            if(!ok)failed=true;return ok;
        }
        synchronized int[][] readMany(final int[] slots,final int[] counts){
            if(!usable()||!readsValid(slots,counts))return null;
            int[][] result=owner(new Callable<int[][]>(){public int[][] call(){return readManyNative(token,0,slots,counts);}},null);
            if(result==null)failed=true;return result;
        }
        /** GX19 bounded two-bank submission: input snapshots complete before
         * return, GPU work does not wait. Bank slots must be disjoint until collect. */
        synchronized Ticket submit(Batch batch,final int bank){
            if(!usable()||batch==null||bank<0||bank>1||pending[bank]!=null)return null;
            final Packet p=batch.packet();
            Ticket ticket=new Ticket(this,bank,0);
            long nativeTicket=owner(new Callable<Long>(){public Long call(){try{
                return submitNative(token,bank,p.kinds,p.slots,p.bytes,p.shaders,p.payloads,p.bindings,p.u,p.f,p.counts);
            }finally{refresh();}}},Long.valueOf(0));
            if(nativeTicket==0){failed=true;return null;}
            ticket.nativeTicket=nativeTicket;pending[bank]=ticket;return ticket;
        }
        int[][] collect(final Ticket ticket,final int[] slots,final int[] counts){
            synchronized(this){if(!usable()||ticket==null||ticket.session!=this||ticket.collected||ticket.collecting||pending[ticket.bank]!=ticket||!readsValid(slots,counts))return null;ticket.collecting=true;}
            int[][] result=null;
            final long deadline=System.nanoTime()+5000000000L;
            try{
                for(;;){
                    synchronized(this){if(!usable())return null;}
                    int ready=owner(new Callable<Integer>(){public Integer call(){return ticketReadyNative(token,ticket.nativeTicket);}},Integer.valueOf(-1));
                    if(ready<0)return null;
                    if(ready>0){result=owner(new Callable<int[][]>(){public int[][] call(){return readManyNative(token,ticket.nativeTicket,slots,counts);}},null);return result;}
                    if(System.nanoTime()-deadline>=0){owner(new Callable<Boolean>(){public Boolean call(){return timeoutNative(token);}},Boolean.FALSE);return null;}
                    // Yield the private EGL owner to the other bank's commands.
                    LockSupport.parkNanos(250000L);
                }
            }finally{synchronized(this){ticket.collecting=false;if(result==null)failed=true;else{pending[ticket.bank]=null;ticket.collected=true;}}}
        }
        private boolean readsValid(int[] slots,int[] counts){
            if(slots==null||counts==null||slots.length<1||slots.length>24||slots.length!=counts.length)return false;
            long sum=0;for(int i=0;i<slots.length;i++){if(slots[i]<0||slots[i]>=24||counts[i]<1)return false;sum+=(long)counts[i]*4;if(sum>MAX_BYTES)return false;}return true;
        }
        private boolean targetValid(int slot,int count,int[] target,int offset){
            return slot>=0&&slot<24&&count>0&&(long)count*4<=MAX_BYTES&&target!=null&&
                offset>=0&&offset<=target.length&&count<=target.length-offset;
        }
        /** GX25/GX26 two-bank collection without allocating an intermediate
         * Java result array. The same completion and ownership rules apply. */
        boolean collectInto(final Ticket ticket,final int slot,final int count,final int[] target,final int offset){
            synchronized(this){if(!usable()||ticket==null||ticket.session!=this||ticket.collected||ticket.collecting||
                pending[ticket.bank]!=ticket||!targetValid(slot,count,target,offset))return false;ticket.collecting=true;}
            boolean ok=false;final long deadline=System.nanoTime()+5000000000L;
            try{
                for(;;){
                    synchronized(this){if(!usable())return false;}
                    int ready=owner(new Callable<Integer>(){public Integer call(){return ticketReadyNative(token,ticket.nativeTicket);}},Integer.valueOf(-1));
                    if(ready<0)return false;
                    if(ready>0){ok=owner(new Callable<Boolean>(){public Boolean call(){try{
                        return readIntoNative(token,ticket.nativeTicket,slot,count,target,offset);
                    }finally{refresh();}}},Boolean.FALSE);return ok;}
                    if(System.nanoTime()-deadline>=0){owner(new Callable<Boolean>(){public Boolean call(){return timeoutNative(token);}},Boolean.FALSE);return false;}
                    LockSupport.parkNanos(250000L);
                }
            }finally{synchronized(this){ticket.collecting=false;if(!ok)failed=true;else{pending[ticket.bank]=null;ticket.collected=true;}}}
        }
        synchronized String stats(){return "gx1960 dispatched="+dispatched+" residentBytes="+retained+" failed="+failed;}
        public synchronized void close(){
            if(closed)return;
            boolean ok=owner(new Callable<Boolean>(){public Boolean call(){try{return closeNative(token);}finally{
                // A timeout closes the native token while retaining quarantined
                // allocations whose completion is unknown. Those bytes remain
                // counted; they do not represent an active photograph session.
                activeSession=false;refresh();
            }}},Boolean.FALSE);
            if(!ok)failed=true;closed=true;pending[0]=pending[1]=null;
        }
    }
    static void trimIdle(){if(!available()||activeSession)return;owner(new Callable<Boolean>(){public Boolean call(){try{return trimNative();}finally{refresh();}}},Boolean.FALSE);}
    private static native boolean batchNative(long token,int[] kinds,int[] slots,long[] bytes,int[] shaders,Object[] payloads,int[][] bindings,int[][] u,float[][] f,int[] counts);
    private static native int[][] executeNative(long token,int[] kinds,int[] slots,long[] bytes,int[] shaders,Object[] payloads,int[][] bindings,int[][] u,float[][] f,int[] counts,int[] readSlots,int[] readCounts);
    private static native long submitNative(long token,int bank,int[] kinds,int[] slots,long[] bytes,int[] shaders,Object[] payloads,int[][] bindings,int[][] u,float[][] f,int[] counts);
    private static native int[][] readManyNative(long token,long ticket,int[] slots,int[] counts);
    private static native boolean readIntoNative(long token,long ticket,int slot,int count,int[] target,int offset);
    private static native boolean trimNative();
    private static native int ticketReadyNative(long token,long ticket);
    private static native boolean timeoutNative(long token);
    private static native int workgroupNative(int shader);
    private static native int nativeAbi();
    private static native long openNative();
    private static native boolean supportedNative(int id);
    private static native long retainedNative();
    private static native String environmentNative();
    private static native boolean allocateNative(long token,int slot,long bytes);
    private static native boolean uploadIntsNative(long token,int slot,int[] values);
    private static native boolean uploadFloatsNative(long token,int slot,float[] values);
    private static native boolean dispatchNative(long token,int shader,int[] bindings,int[] u,float[] f,int invocations);
    private static native int[] readIntsNative(long token,int slot,int count);
    private static native boolean closeNative(long token);
}
