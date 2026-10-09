package com.hiro.ulike;

import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    static final int STRONG_PROGRAM_BASE=SHADERS*VARIANTS,PROGRAMS=STRONG_PROGRAM_BASE+5*VARIANTS;
    static int variant(int shader,int choice){return shader>=0&&shader<SHADERS&&choice>=0&&choice<VARIANTS?shader+choice*SHADERS:-1;}
    static int strongProgram(int mode,int choice){return mode>=0&&mode<4&&choice>=0&&choice<VARIANTS?STRONG_PROGRAM_BASE+mode+choice*4:-1;}
    static int strongExactProgram1973(int choice){return choice>=0&&choice<VARIANTS?STRONG_PROGRAM_BASE+4*VARIANTS+choice:-1;}
    public static final long MAX_BYTES=512L*1024*1024;
    private static volatile int loaded;
    private static volatile long knownSupported,knownUnsupported;
    private static volatile long retained;
    private static volatile boolean activeSession;
    private static volatile String environment="";
    // Only OWNER mutates these scalar reservations. Actual native capacities
    // replace their pending growth after upload/submit; already resident banks
    // are never charged again. No image or command payload belongs to a lease.
    private static final ArrayList<Lease1971> leases1971=new ArrayList<Lease1971>();
    private static volatile long reserved1971;
    private static final long STAGING1971=64L*1024*1024;
    static final int FAILURE_NONE1971=0,FAILURE_ALLOCATION1971=1,FAILURE_UPLOAD1971=2,
        FAILURE_DISPATCH1971=3,FAILURE_READBACK1971=4,FAILURE_FENCE1971=5,FAILURE_UNKNOWN1971=6;
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
        return physicalBudget1971(true);
    }
    private static long physicalBudget1971(boolean includeReservations){
        Runtime r=Runtime.getRuntime();
        long available=r.maxMemory()-(r.totalMemory()-r.freeMemory());
        long[] owners={retained,WholeRoute1953.retainedBytes(),GpuFinish1953.retainedBytes(),
            SpeedWorkers1935.nativeRetainedBytes1956(),64L*1024*1024,includeReservations?reserved1971:0};
        for(long bytes:owners){if(bytes<0||bytes>available)return 0;available-=bytes;}
        return available;
    }
    /** Warm only the bounded private context identity for persisted admission.
     * No program compilation, session, SSBO allocation or GPU command occurs. */
    static boolean warmEnvironment1973(){
        try{
            if(!available()||Thread.currentThread().isInterrupted())return false;
            if(environment.length()!=0)return true;
            return owner(new Callable<Boolean>(){public Boolean call(){
                String e=environmentNative();if(environment.length()==0&&e!=null)environment=e;
                refresh();return Boolean.valueOf(environment.length()!=0);
            }},Boolean.FALSE);
        }catch(LinkageError unavailable){loaded=-1;return false;}
        catch(RuntimeException unavailable){return false;}
        catch(OutOfMemoryError unavailable){return false;}
    }
    static String fingerprint(){return environment;}
    private static void refresh(){
        retained=retainedNative();
        reconcile1971();
        // The private native context is initialized once and quarantined on a
        // fatal driver error. Its fingerprint cannot change during this process;
        // avoid another JNI String allocation for every strip/bank command.
        if(environment.length()==0){String e=environmentNative();environment=e==null?"":e;}
    }
    static long reservedBytes1971(){return reserved1971;}
    /** A bounded scalar lease for native capacity growth and private Java
     * readback peaks. Close after the caller commits or discards every result. */
    static final class Lease1971 implements AutoCloseable {
        final long token;final long[] capacities;final long staging,temporary,javaBytes;
        private volatile boolean closed;
        private Lease1971(long token,long[] capacities,long staging,long temporary,long javaBytes){
            this.token=token;this.capacities=capacities;this.staging=staging;this.temporary=temporary;this.javaBytes=javaBytes;
        }
        /** Recheck after CPU scratch/oracle growth and immediately before GPU
         * submission. A failed check leaves ownership intact for close(). */
        boolean revalidate1971(){
            if(closed||Thread.currentThread().isInterrupted())return false;
            return owner(new Callable<Boolean>(){public Boolean call(){
                if(closed||!leases1971.contains(Lease1971.this))return Boolean.FALSE;
                long[] actual=capacities1971(token);long[] debt=actual==null?null:debt1971(actual);
                if(debt==null){reserved1971=Long.MAX_VALUE;return Boolean.FALSE;}
                reserved1971=debt[1];
                return debt[0]<=MAX_BYTES-retained&&debt[1]<=physicalBudget1971(false);
            }},Boolean.FALSE);
        }
        public void close(){
            if(closed)return;
            owner(new Callable<Boolean>(){public Boolean call(){
                if(!closed){closed=true;leases1971.remove(Lease1971.this);reconcile1971();}return Boolean.TRUE;
            }},Boolean.FALSE);
        }
    }
    private static long round1971(long bytes,long maximum){
        long rounded=(bytes+4095L)&~4095L;return rounded>MAX_BYTES||rounded>maximum?bytes:rounded;
    }
    /** OWNER only. Native snapshot remains available after a session failure so
     * quarantined capacities continue to count, even when work is no longer usable. */
    private static long[] capacities1971(long token){
        long[] values=capacityNative1971(token);
        if(values==null||values.length!=27||values[24]<0||values[25]<1||values[26]<0||
                values[24]>MAX_BYTES||values[26]>MAX_BYTES-values[24])return null;
        long total=0;for(int i=0;i<24;i++){if(values[i]<0||values[i]>MAX_BYTES||values[i]>MAX_BYTES-total)return null;total+=values[i];}
        if(total!=values[26])return null;retained=values[24]+values[26];return values;
    }
    private static long[] debt1971(long[] actual){
        long[] desired=new long[24];System.arraycopy(actual,0,desired,0,24);
        long staging=actual[24],temporary=0,javaBytes=0;
        for(Lease1971 lease:leases1971)if(!lease.closed){
            for(int i=0;i<24;i++)desired[i]=Math.max(desired[i],lease.capacities[i]);
            staging=Math.max(staging,lease.staging);temporary=Math.max(temporary,lease.temporary);
            if(lease.javaBytes>MAX_BYTES-javaBytes)return null;javaBytes+=lease.javaBytes;
        }
        long nativeGrowth=staging-actual[24];
        for(int i=0;i<24;i++){long growth=desired[i]-actual[i];if(growth>MAX_BYTES-nativeGrowth)return null;nativeGrowth+=growth;}
        if(temporary>MAX_BYTES-nativeGrowth)return null;nativeGrowth+=temporary;
        if(javaBytes>MAX_BYTES-nativeGrowth)return null;
        return new long[]{nativeGrowth,nativeGrowth+javaBytes};
    }
    private static void reconcile1971(){
        if(leases1971.isEmpty()){reserved1971=0;return;}
        // A failed diagnostic allocation cannot leave an optimistic old debt.
        reserved1971=Long.MAX_VALUE;
        long[] actual=capacities1971(leases1971.get(0).token);
        long[] debt=actual==null?null:debt1971(actual);
        reserved1971=debt==null?Long.MAX_VALUE:debt[1];
    }
    private static void releaseLeases1971(long token){
        for(int i=leases1971.size()-1;i>=0;i--)if(leases1971.get(i).token==token){leases1971.remove(i).closed=true;}
        reconcile1971();
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
        /** GX35: caller-owned direct coefficient words. Recording freezes the
         * byte range; its contents belong to the caller until submit returns.
         * The native upload consumes them synchronously before returning a
         * ticket, so no Java array or pinned array is needed in this path. */
        Batch uploadDirect(int slot,ByteBuffer values){
            if(slot<0||slot>=24||values==null||!values.isDirect()||
                    values.order()!=ByteOrder.nativeOrder()||values.position()!=0||
                    values.remaining()<4||(values.remaining()&3)!=0||values.remaining()>MAX_BYTES)
                throw new IllegalArgumentException("GPU direct coefficient buffer");
            ByteBuffer view=values.slice().order(ByteOrder.nativeOrder());
            commands.add(new Command(6,slot,view.remaining(),0,view,null,null,null,0));return this;
        }
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
        private int lastFailure1971;
        private long dispatched;
        private final Ticket[] pending=new Ticket[2];
        private Session(long token){this.token=token;}
        /** Actual SSBO capacities, staging, maxStorage and totalSSBO scalars.
         * No driver strings, memory addresses or picture contents are returned. */
        synchronized long[] capacity1971(){
            if(closed)return null;
            return owner(new Callable<long[]>(){public long[] call(){return capacities1971(token);}},null);
        }
        synchronized int failureCode1971(){
            if(closed)return lastFailure1971;
            int code=owner(new Callable<Integer>(){public Integer call(){return failureCodeNative1971();}},Integer.valueOf(FAILURE_UNKNOWN1971));
            if(code<0||code>FAILURE_UNKNOWN1971)code=FAILURE_UNKNOWN1971;
            if(lastFailure1971==0&&code!=0)lastFailure1971=code;
            return lastFailure1971;
        }
        /** Reserve target capacities before any CPU verification or GPU upload.
         * Include every slot the batch may allocate/upload. Its largest target
         * conservatively covers shared upload staging; Java bytes include both
         * old and new readback results if they can coexist across two trials. */
        synchronized Lease1971 reserveCapacity1971(final int[] slots,final long[] targetBytes,final long javaReadbackBytes){
            if(!usable()||slots==null||targetBytes==null||slots.length<1||slots.length>24||
                    slots.length!=targetBytes.length||javaReadbackBytes<0||javaReadbackBytes>MAX_BYTES)return null;
            final int[] privateSlots=slots.clone();final long[] privateBytes=targetBytes.clone();
            return owner(new Callable<Lease1971>(){public Lease1971 call(){
                if(leases1971.size()>=3)return null;
                long[] actual=capacities1971(token);if(actual==null)return null;
                long[] desired=new long[24];boolean[] seen=new boolean[24];long largestSmall=0,largestTemporary=0;
                for(int i=0;i<privateSlots.length;i++){
                    int slot=privateSlots[i];long bytes=privateBytes[i];
                    if(slot<0||slot>=24||seen[slot]||bytes<1||bytes>MAX_BYTES||bytes>actual[25])return null;
                    seen[slot]=true;desired[slot]=round1971(bytes,actual[25]);
                    if(bytes<=STAGING1971)largestSmall=Math.max(largestSmall,bytes);
                    else largestTemporary=Math.max(largestTemporary,bytes);
                }
                long staging=largestSmall==0?0:round1971(largestSmall,Long.MAX_VALUE);
                long temporary=largestTemporary;
                Lease1971 lease=new Lease1971(token,desired,staging,temporary,javaReadbackBytes);
                boolean admitted=false;leases1971.add(lease);
                try{
                    long[] debt=debt1971(actual);
                    if(debt==null||debt[0]>MAX_BYTES-retained||debt[1]>physicalBudget1971(false))return null;
                    reserved1971=debt[1];admitted=true;return lease;
                }finally{
                    if(!admitted){
                        leases1971.remove(lease);lease.closed=true;
                        try{reconcile1971();}catch(RuntimeException unavailable){reserved1971=leases1971.isEmpty()?0:Long.MAX_VALUE;}
                        catch(LinkageError unavailable){reserved1971=leases1971.isEmpty()?0:Long.MAX_VALUE;}
                        catch(OutOfMemoryError unavailable){reserved1971=leases1971.isEmpty()?0:Long.MAX_VALUE;}
                    }
                }
            }},null);
        }
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
                try{int code=failureCodeNative1971();if(lastFailure1971==0&&code>0&&code<=FAILURE_UNKNOWN1971)lastFailure1971=code;}
                catch(RuntimeException unavailable){if(lastFailure1971==0)lastFailure1971=FAILURE_UNKNOWN1971;}
                catch(LinkageError unavailable){if(lastFailure1971==0)lastFailure1971=FAILURE_UNKNOWN1971;}
                // A timeout closes the native token while retaining quarantined
                // allocations whose completion is unknown. Those bytes remain
                // counted; they do not represent an active photograph session.
                activeSession=false;releaseLeases1971(token);refresh();
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
    private static native long[] capacityNative1971(long token);
    private static native int failureCodeNative1971();
    private static native String environmentNative();
    private static native boolean allocateNative(long token,int slot,long bytes);
    private static native boolean uploadIntsNative(long token,int slot,int[] values);
    private static native boolean uploadFloatsNative(long token,int slot,float[] values);
    private static native boolean dispatchNative(long token,int shader,int[] bindings,int[] u,float[] f,int invocations);
    private static native int[] readIntsNative(long token,int slot,int count);
    private static native boolean closeNative(long token);
}
