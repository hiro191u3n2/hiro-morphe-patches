package com.hiro.ulike;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
/** Controlled transport and CPU fixtures. No real shader/device speed claim. */
final class GpuQualification1961 {
    interface Cancellation { boolean cancelled(); }
    interface Probe { void run(Cancellation c); void close(); }
    interface RowTask1964 { boolean run(int begin,int end); }
    static final class Record {final long cpuNanos,gpuNanos;final int variant;Record(long c,long g,int v){cpuNanos=c;gpuNanos=g;variant=v;}}
    static final Map<String,Record> records=Collections.synchronizedMap(new HashMap<String,Record>());
    static final Set<String> exact=Collections.synchronizedSet(new HashSet<String>()),speed=Collections.synchronizedSet(new HashSet<String>());
    static final Map<String,String> exactCauses=Collections.synchronizedMap(new HashMap<String,String>());
    static volatile int preferredCalls,ordinaryCalls,schedules;
    static volatile boolean busyBackground,cancel,throwPreferred;
    static long lastQueueBytes;
    static void reset(){records.clear();exact.clear();exactCauses.clear();speed.clear();preferredCalls=ordinaryCalls=schedules=0;busyBackground=cancel=throwPreferred=false;lastQueueBytes=0;}
    static Record restore(String key){return records.get(key);}
    static boolean exactRejected(String key){return exact.contains(key);}
    static boolean maySchedule(String key){return !exact.contains(key);}
    static boolean background(){return busyBackground;}static boolean cancelled(){return cancel;}
    static long retainedBytes(){return 0;}
    static boolean canQueue(String key,long bytes){lastQueueBytes=bytes;return bytes>0&&!exactRejected(key);}
    static boolean schedule(String key,long bytes,Probe p){schedules++;p.close();return false;}
    static void rejectExact(String key){exact.add(key);records.remove(key);}
    static void rejectExact1971(String key,String cause){rejectExact(key);exactCauses.put(key,cause);}
    static String exactFailure1971(String key){String cause=exactCauses.get(key);return cause==null?"cached_legacy_negative":"argb_mismatch".equals(cause)?"cached_argb_negative":"confidence_mismatch".equals(cause)?"cached_confidence_negative":"policy_failure".equals(cause)?"cached_policy_negative":cause;}
    static void rejectSpeed(String key){speed.add(key);records.remove(key);}
    static void qualified(String key,long cpu,long gpu,int variant){ordinaryCalls++;if(cpu>0&&gpu>0&&gpu<=cpu-cpu/20&&!exactRejected(key))records.put(key,new Record(cpu,gpu,variant));}
    static synchronized void qualifiedStrongPreferred1970(String key,long cpu,long gpu,int variant){preferredCalls++;if(throwPreferred)throw new IllegalStateException("optional certificate storage unavailable");if(cpu>0&&gpu>0&&!exactRejected(key)){speed.remove(key);records.put(key,new Record(cpu,gpu,variant));}}
    static long parallelRows1964(final int begin,final int end,int unit,final Cancellation c,final RowTask1964 task){
        ExecutorService pool=Executors.newFixedThreadPool(4);long start=System.nanoTime();List<Future<Boolean>> jobs=new ArrayList<Future<Boolean>>();
        try{int count=(end-begin+unit-1)/unit;for(int worker=0;worker<4;worker++){final int b=begin+count*worker/4*unit,e=Math.min(end,begin+count*(worker+1)/4*unit);if(e>b)jobs.add(pool.submit(new Callable<Boolean>(){public Boolean call(){if(c.cancelled())throw new CancellationException();return task.run(b,e);}}));}for(Future<Boolean> job:jobs)if(!job.get())return 0;if(c.cancelled())throw new CancellationException();return System.nanoTime()-start;}
        catch(ExecutionException e){throw new RuntimeException(e.getCause());}catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException();}finally{pool.shutdownNow();}
    }
}
final class GpuNoise1960 {
    static final int STRONG=0,ANALYSIS=2,COMPARE1961=7;
    static final long MAX_BYTES=512L*1024*1024;
    static volatile int opens,closes,modelUploads,executes,submits,collects,inflight,maxInflight,bankReuse;
    static volatile long uploadDelay,closeDelay,gpuDelay,warmDelay,supportDelay;
    static volatile int warms,supportsCalls,badExactMask=7,unsupportedExactMask;
    static volatile boolean badAlwaysPixel,fullMismatch,throwLeaseClose;
    static volatile boolean badSecondPixel,badSecondConfidence,badConfidence,failSecondExecute,failFirstExecute,failModelUpload,failSubmit,throwCollect,available=true,supported=true,fits=true,holdTickets;
    static volatile int policyFlag,badVariantMask=7,badTiledMask=7,badGenericMask=7,unsupportedVariantMask,unsupportedGenericMask;
    static volatile boolean badOutputShape;
    static volatile int nativeFailureCode,reservationCalls,liveRangeLeases,maxRangeLeases,revalidations;
    static volatile long reservedJava,reservationLimit=MAX_BYTES;
    static final List<Reservation> reservations=Collections.synchronizedList(new ArrayList<Reservation>());
    static final List<Integer> submittedPrograms=Collections.synchronizedList(new ArrayList<Integer>());
    static volatile String rejectOnSupports;
    static final List<Integer> submittedMarkers=Collections.synchronizedList(new ArrayList<Integer>());
    static final Set<Integer> failedCollectTickets=Collections.synchronizedSet(new HashSet<Integer>());
    static final Map<Integer,CountDownLatch> ticketGates=new ConcurrentHashMap<Integer,CountDownLatch>();
    static void reset(){opens=closes=modelUploads=executes=submits=collects=inflight=maxInflight=bankReuse=nativeFailureCode=reservationCalls=liveRangeLeases=maxRangeLeases=revalidations=warms=supportsCalls=0;unsupportedExactMask=0;badExactMask=7;badAlwaysPixel=fullMismatch=throwLeaseClose=false;reservedJava=0;reservationLimit=MAX_BYTES;uploadDelay=closeDelay=gpuDelay=warmDelay=supportDelay=0;badSecondPixel=badSecondConfidence=badConfidence=failSecondExecute=failFirstExecute=failModelUpload=failSubmit=throwCollect=holdTickets=badOutputShape=false;available=supported=fits=true;policyFlag=unsupportedVariantMask=unsupportedGenericMask=0;badVariantMask=badTiledMask=badGenericMask=7;rejectOnSupports=null;submittedMarkers.clear();submittedPrograms.clear();reservations.clear();ticketGates.clear();failedCollectTickets.clear();}
    static void pause(long nanos){long end=System.nanoTime()+nanos;while(System.nanoTime()<end){if(Thread.currentThread().isInterrupted())throw new CancellationException("fixture interrupted");java.util.concurrent.locks.LockSupport.parkNanos(Math.min(1000000L,end-System.nanoTime()));}}
    static boolean warmEnvironment1973(){synchronized(GpuNoise1960.class){warms++;}pause(warmDelay);return available;}
    static boolean supports(int shader){synchronized(GpuNoise1960.class){supportsCalls++;}pause(supportDelay);if(shader>=39)return supported&&(unsupportedExactMask&(1<<(shader-39)))==0;if(rejectOnSupports!=null)GpuQualification1961.rejectExact(rejectOnSupports);return supported&&(shader<27?(unsupportedGenericMask&(1<<(shader/9)))==0:(unsupportedVariantMask&(1<<((shader-27)/4)))==0);}static boolean available(){return available;}static boolean workspaceFits(long bytes){return fits;}
    static boolean sessionBusy(){return false;}static String fingerprint(){return "controlled-strong70";}
    static int strongProgram(int mode,int choice){return 27+mode+choice*4;}
    static int variant(int shader,int choice){return shader+choice*9;}
    static synchronized Session open(){if(!available)return null;opens++;return new Session();}
    static void releaseTicket(int id){CountDownLatch gate=ticketGates.get(id);if(gate!=null)gate.countDown();}
    static void releaseAll(){for(CountDownLatch gate:ticketGates.values())gate.countDown();}
    static final class Reservation {final int[] slots;final long[] target;final long javaBytes;Reservation(int[] s,long[] t,long j){slots=s.clone();target=t.clone();javaBytes=j;}}
    static final class Lease1971 {final long javaBytes;boolean closed;Lease1971(long j){javaBytes=j;}boolean revalidate1971(){synchronized(GpuNoise1960.class){if(closed)throw new AssertionError("closed reservation revalidated");revalidations++;return fits;}}void close(){synchronized(GpuNoise1960.class){if(closed)throw new AssertionError("reservation closed twice");closed=true;reservedJava-=javaBytes;if(javaBytes>0)liveRangeLeases--;if(reservedJava<0||liveRangeLeases<0)throw new AssertionError("reservation accounting underflow");if(throwLeaseClose&&javaBytes>0)throw new IllegalStateException("fixture lease close after release");}}}
    static final class Batch {int[] u,source;int variant,program,profile=1;boolean generic;Batch upload(int slot,int[] x){if(slot==0||slot==14)source=x.clone();return this;}Batch allocate(int slot,long bytes){return this;}Batch dispatch(int shader,int[] bindings,int[] uniforms,float[] f,int count){if(shader>=39&&shader<42){u=uniforms.clone();variant=shader-39;program=shader;profile=0;generic=false;}else if(shader>=27&&shader<39){u=uniforms.clone();variant=(shader-27)/4;program=shader;generic=false;}else if(shader==0||shader==9||shader==18){u=uniforms.clone();variant=shader/9;program=shader;profile=2;generic=true;}return this;}}
    static final class Ticket {final Batch b;final int bank,id;final Session session;Ticket(Batch b,int bank,int id,Session s){this.b=b;this.bank=bank;this.id=id;session=s;}}
    static final class Session {
        int runs;final int[] variantRuns=new int[9];boolean closed;final Ticket[] pending=new Ticket[2];
        final long[] capacity1971=new long[27];
        int failureCode1971(){return nativeFailureCode;}
        Lease1971 reserveCapacity1971(int[] slots,long[] target,long javaBytes){synchronized(GpuNoise1960.class){reservationCalls++;if(slots.length!=target.length||javaBytes<0)throw new AssertionError("invalid reservation shape");long future=0;long[] next=capacity1971.clone();for(int i=0;i<slots.length;i++){if(slots[i]<0||slots[i]>=next.length||target[i]<0)throw new AssertionError("invalid reservation target");next[slots[i]]=Math.max(next[slots[i]],target[i]);}for(long bytes:next)future+=bytes;if(!fits||future+reservedJava+javaBytes>reservationLimit)return null;System.arraycopy(next,0,capacity1971,0,next.length);reservations.add(new Reservation(slots,target,javaBytes));reservedJava+=javaBytes;if(javaBytes>0){liveRangeLeases++;maxRangeLeases=Math.max(maxRangeLeases,liveRangeLeases);}return new Lease1971(javaBytes);}}
        boolean upload(int slot,int[] a){synchronized(GpuNoise1960.class){modelUploads++;}pause(uploadDelay);return !failModelUpload;}
        boolean upload(int slot,float[] a){synchronized(GpuNoise1960.class){modelUploads++;}pause(uploadDelay);return !failModelUpload;}
        synchronized Ticket submit(Batch b,int bank){
            if(closed)throw new IllegalStateException("closed session");if(failSubmit)return null;
            if(bank<0||bank>1||pending[bank]!=null){bankReuse++;throw new AssertionError("mutable bank reused before collection");}
            int id;synchronized(GpuNoise1960.class){if(b.u[10]==3&&liveRangeLeases==0)throw new AssertionError("Strong submits outside range reservation");id=++submits;inflight++;maxInflight=Math.max(maxInflight,inflight);}
            Ticket t=new Ticket(b,bank,id,this);pending[bank]=t;submittedMarkers.add(b.source==null?0:b.source[0]);submittedPrograms.add(b.program);if(holdTickets)ticketGates.put(id,new CountDownLatch(1));return t;
        }
        int[][] collect(Ticket t,int[] slots,int[] counts){
            if(t.session!=this)throw new AssertionError("ticket owner changed");
            try{CountDownLatch gate=ticketGates.get(t.id);if(gate!=null&&!gate.await(10,TimeUnit.SECONDS))throw new AssertionError("test ticket gate timed out");if(throwCollect||failedCollectTickets.contains(t.id))throw new IllegalStateException("read failure");synchronized(GpuNoise1960.class){collects++;}return execute(t.b,slots,counts);}
            catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new CancellationException("fixture collect interrupted");}
            finally{synchronized(this){if(pending[t.bank]!=t)throw new AssertionError("ticket bank owner lost");pending[t.bank]=null;}synchronized(GpuNoise1960.class){inflight--;}}
        }
        int[][] execute(Batch b,int[] slots,int[] counts){
            int run;synchronized(this){if(closed)throw new IllegalStateException("closed session");runs++;run=++variantRuns[b.profile*3+b.variant];}synchronized(GpuNoise1960.class){executes++;}
            pause(gpuDelay);if((failFirstExecute&&run==1)||(failSecondExecute&&run==2))return null;
            int[][] out=new int[counts.length][];out[0]=new int[counts[0]];
            for(int i=0;i<out[0].length;i++)out[0][i]=StrongNoise1958.pixel(b.source[b.u[2]*b.u[0]+i]);
            boolean wrong=(badVariantMask&(1<<b.variant))!=0&&((b.profile==0?badExactMask:b.generic?badGenericMask:badTiledMask)&(1<<b.variant))!=0;
            if(wrong&&(badAlwaysPixel||(badSecondPixel&&run==2)))out[0][counts[0]-1]^=1;
            if(counts.length==3){out[1]=new int[counts[1]];Arrays.fill(out[1],37);if(wrong&&(badConfidence||(badSecondConfidence&&run==2)))out[1][out[1].length-1]++;out[2]=new int[]{policyFlag};}else out[1]=new int[]{policyFlag};if(fullMismatch&&wrong){out[0][17]^=1;int last=out[0].length-1,green=(out[0][last]>>>8)&255;out[0][last]=(out[0][last]&~0x0000ff00)|((green<=238?green+17:green-17)<<8);if(counts.length==3){out[1][0]+=5;out[1][out[1].length-1]+=2;}}if(badOutputShape)out[0]=Arrays.copyOf(out[0],out[0].length-1);return out;
        }
        synchronized void close(){pause(closeDelay);if(closed)throw new AssertionError("session closed twice");closed=true;synchronized(GpuNoise1960.class){if(liveRangeLeases!=0||reservedJava!=0)throw new AssertionError("session closed before range reservations release");closes++;}for(Ticket ticket:pending)if(ticket!=null)throw new AssertionError("session closed before collectors join");}
    }
}
final class StrongNoise1958 {
    static final class Model {int height=32;}
    interface Patches {void read(int[] data,int x,int y,int width,int height);}
    static volatile long cpuDelay=1000000L;
    static final AtomicInteger oracleCalls=new AtomicInteger();
    static int pixel(int value){return value^0x00010203;}
    static float[] gpuEvidence1960(Model m){return new float[16];}
    static int[][] gpuMaps1960(Model m){return new int[][]{new int[1000],new int[250],new int[63]};}
    static GpuAnalysis1961.Resident takeResident1961(Model m){return null;}static void discardResident1961(Model m){}
    static boolean gpuPrepareOracleSnapshot1961(int[] src,int[] out,int w,int h,int noise,boolean shadows,float[] ev,int mode){oracleCalls.incrementAndGet();GpuNoise1960.pause(cpuDelay);for(int i=0;i<w*h;i++)out[i]=pixel(src[i]);return true;}
    static boolean gpuOracleSnapshot1961(int[] src,int[] out,int w,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,Model m,int[] policy,int[] confidence){oracleCalls.incrementAndGet();GpuNoise1960.pause(cpuDelay);for(int i=begin*w;i<end*w;i++)out[i]=pixel(src[i]);if(confidence!=null)Arrays.fill(confidence,0,((w+3)/4)*((end-begin+3)/4),37);return true;}
}
final class GpuAnalysis1961 {static final class Resident {final GpuNoise1960.Session session;final long setupNanos;Resident(GpuNoise1960.Session s,long n){session=s;setupNanos=n;}void close(){session.close();}}}
final class GpuPolicy1960 {
    static final class LocalNoise {int columns=1,rows=1;}
    static final class Plan {int beautyQ8,shadowBudgetQ8;LocalNoise localNoise;}
    static final class PolicyData {int count;int[] grid=new int[0],masks=new int[0],u=new int[32];float[] f=new float[32];}
    static final class Protection {Plan plan;PolicyData data(int width,int rows,int y){PolicyData p=new PolicyData();p.count=width*rows;return p;}}
}
final class ProcessingTiming1947 {
    static final class Trace {final long captureId=1970,id=Long.MAX_VALUE;}
    static final Trace trace=new Trace();static int backendCalls,lastGpu,lastCpu,verification;
    static final Map<String,Integer> reasons=new HashMap<String,Integer>();
    static final Map<String,Integer> trials=new HashMap<String,Integer>();
    static long cpuWork,gpuWork,waitWork,argbCount=-1,confidenceCount=-1,confidenceMax=-1;static int argbMax=-1,workCalls,mismatchCalls;
    static String firstFailure;static int firstProgram,firstLayout,firstX,firstY,firstWidth,firstRows;static long firstDelta;
    static void reset(){backendCalls=lastGpu=lastCpu=verification=workCalls=mismatchCalls=0;cpuWork=gpuWork=waitWork=0;argbCount=confidenceCount=confidenceMax=-1;argbMax=-1;reasons.clear();trials.clear();firstFailure=null;}
    static Trace current(){return trace;}
    static synchronized void strongGpuWork1973(Trace t,long c,long g,long w,int reuse){if(t!=trace)throw new AssertionError("work trace lost");workCalls++;cpuWork+=c;gpuWork+=g;waitWork+=w;}
    static synchronized void strongGpuMismatch1973(Trace t,long a,int m,long c,long n){if(t!=trace)throw new AssertionError("mismatch trace lost");mismatchCalls++;argbCount=a;argbMax=m;confidenceCount=c;confidenceMax=n;}
    static synchronized void noiseBackend(Trace t,int gpu,int cpu){if(t!=trace)throw new AssertionError("capture trace lost");backendCalls++;lastGpu=gpu;lastCpu=cpu;}
    static synchronized void strongGpuReason1970(Trace t,String reason,int count){if(t!=trace)throw new AssertionError("reason trace lost");Integer old=reasons.get(reason);reasons.put(reason,(old==null?0:old)+count);}
    static synchronized void strongGpuVerification1970(Trace t,int count){if(t!=trace)throw new AssertionError("verification trace lost");verification+=count;}
    static synchronized void strongGpuTrial1971(Trace t,String kind,int count){if(t!=trace)throw new AssertionError("trial trace lost");Integer old=trials.get(kind);trials.put(kind,(old==null?0:old)+count);}
    static synchronized void strongGpuFailure1971(Trace t,String kind,int program,int layout,int width,int rows,int x,int y,long delta){if(t!=trace)throw new AssertionError("failure trace lost");if(firstFailure==null){firstFailure=kind;firstProgram=program;firstLayout=layout;firstWidth=width;firstRows=rows;firstX=x;firstY=y;firstDelta=delta;}}
}
final class CameraTrace1965 {static final List<String> events=Collections.synchronizedList(new ArrayList<String>());static void event(String phase,long epoch,String fields){events.add(phase+" "+epoch+" "+fields);}static void event(String phase,Object owner,long epoch,String fields){event(phase,epoch,fields);}}
