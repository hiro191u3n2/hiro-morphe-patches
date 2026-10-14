package com.hiro.ulike;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/** Actual unchanged production Java and JNI execute in software GLES. */
public final class Memory1971Test {
    static int assertions;
    static final long M=1024L*1024;
    static native void setFault(int code);
    static void check(boolean value,String label){assertions++;if(!value)throw new AssertionError(label);}
    static int leases()throws Exception {Field f=GpuNoise1960.class.getDeclaredField("leases1971");f.setAccessible(true);return ((List<?>)f.get(null)).size();}
    static GpuNoise1960.Session open(){GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"actual private native session opens");return s;}
    static long rounded(long bytes){return (bytes+4095)&~4095L;}
    static void logic()throws Exception {
        GpuNoise1960.Session s=open();long[] initial=s.capacity1971();
        check(initial!=null&&initial.length==27&&initial[25]>=80*M,"actual bounded capacity/maxStorage JNI snapshot");
        check(initial[24]==0&&initial[26]==0,"fresh SSBO and staging capacities start empty");
        check(s.failureCode1971()==0,"fresh fixed failure code is clear");
        check(s.reserveCapacity1971(new int[]{0},new long[]{0},0)==null,"zero target excluded");
        check(s.reserveCapacity1971(new int[]{25},new long[]{4},0)==null,"slot25 outside expanded storage excluded");
        check(s.reserveCapacity1971(new int[]{0,0},new long[]{4,8},0)==null,"duplicate slot target excluded");
        check(s.reserveCapacity1971(new int[]{0},new long[]{GpuNoise1960.MAX_BYTES+1},0)==null,"native maximum unchanged");
        check(s.reserveCapacity1971(new int[]{0,1,2,3},new long[]{128*M,128*M,128*M,128*M},0)==null,"combined target and temporary peak cannot exceed native 512 MiB limit");
        check(s.reserveCapacity1971(new int[]{0},new long[]{4},-1)==null,"negative Java peak excluded");
        check(GpuNoise1960.reservedBytes1971()==0&&leases()==0,"invalid requests retain no debt");
        GpuNoise1960.Lease1971 model=s.reserveCapacity1971(new int[]{2,4,5,6},new long[]{256,24*M,6*M,1536*1024},0);
        long modelBytes=rounded(256)+24*M+6*M+1536*1024;
        check(model!=null&&GpuNoise1960.reservedBytes1971()==modelBytes+24*M,"initial model includes largest upload staging peak");
        model.close();check(GpuNoise1960.reservedBytes1971()==0,"unused model debt released");
        GpuNoise1960.Lease1971 mixed=s.reserveCapacity1971(new int[]{0,1},new long[]{32*M,80*M},0);
        check(mixed!=null&&GpuNoise1960.reservedBytes1971()==224*M,"small persistent staging and large temporary upload coexist");
        mixed.close();
        int size=65537;long bytes=4L*size,cap=rounded(bytes),javaPeak=65536;
        GpuNoise1960.Lease1971 a=s.reserveCapacity1971(new int[]{0},new long[]{bytes},javaPeak);
        GpuNoise1960.Lease1971 b=s.reserveCapacity1971(new int[]{14},new long[]{bytes},javaPeak);
        check(a!=null&&b!=null,"two bank target reservations coexist");
        check(GpuNoise1960.reservedBytes1971()==3*cap+2*javaPeak,"shared staging reserved once and Java peaks sum");
        check(s.upload(0,new int[size]),"first materialized upload completes");
        long[] materialized=s.capacity1971();check(materialized[0]==cap&&materialized[24]==cap&&materialized[26]==cap,"native query reports actual rounded materialization");
        check(GpuNoise1960.retainedBytes()==2*cap&&GpuNoise1960.reservedBytes1971()==cap+2*javaPeak,"actual capacity growth replaces debt exactly");
        check(s.upload(14,new int[size]),"second bank materializes");
        check(GpuNoise1960.retainedBytes()==3*cap&&GpuNoise1960.reservedBytes1971()==2*javaPeak,"resident banks leave only private Java debt");
        check(a.revalidate1971()&&b.revalidate1971(),"held leases revalidate fresh actual memory");
        WholeRoute1953.retained=Long.MAX_VALUE;
        check(!a.revalidate1971()&&!GpuNoise1960.workspaceFits(1),"future owner growth refuses GPU admission without deleting ownership");
        WholeRoute1953.retained=0;check(a.revalidate1971(),"fresh memory recovery can revalidate existing reservation");
        a.close();a.close();b.close();check(leases()==0&&GpuNoise1960.reservedBytes1971()==0,"lease close is idempotent and releases all debt");
        GpuNoise1960.Lease1971 reuse=s.reserveCapacity1971(new int[]{0},new long[]{bytes},javaPeak);
        check(reuse!=null&&GpuNoise1960.reservedBytes1971()==javaPeak,"existing bank and staging capacity never double charged");
        WholeRoute1953.fail=true;check(s.reserveCapacity1971(new int[]{20},new long[]{4},0)==null,"failed concurrent budget query declines only new lease");WholeRoute1953.fail=false;
        check(leases()==1&&GpuNoise1960.reservedBytes1971()==javaPeak&&reuse.revalidate1971(),"failed new admission preserves earlier owned readback debt");reuse.close();
        GpuNoise1960.Lease1971[] limits=new GpuNoise1960.Lease1971[3];
        for(int i=0;i<3;i++)limits[i]=s.reserveCapacity1971(new int[]{i+8},new long[]{4},0);
        check(limits[0]!=null&&limits[1]!=null&&limits[2]!=null,"bounded model and two-bank leases permitted");
        check(s.reserveCapacity1971(new int[]{20},new long[]{4},0)==null,"fourth lease refused");
        for(GpuNoise1960.Lease1971 lease:limits)lease.close();
        ExecutorService callers=Executors.newFixedThreadPool(4);CountDownLatch attempt=new CountDownLatch(1);
        List<Future<GpuNoise1960.Lease1971>> futures=new ArrayList<Future<GpuNoise1960.Lease1971>>();
        for(int i=0;i<4;i++){final int slot=8+i;futures.add(callers.submit(()->{attempt.await();return s.reserveCapacity1971(new int[]{slot},new long[]{4},0);}));}
        attempt.countDown();int owned=0;List<GpuNoise1960.Lease1971> concurrent=new ArrayList<GpuNoise1960.Lease1971>();for(Future<GpuNoise1960.Lease1971> future:futures){GpuNoise1960.Lease1971 lease=future.get(3,TimeUnit.SECONDS);if(lease!=null){owned++;concurrent.add(lease);}}
        check(owned==3&&leases()==3,"four simultaneous callers cannot race bounded scalar admission");
        for(GpuNoise1960.Lease1971 lease:concurrent)lease.close();callers.shutdownNow();check(leases()==0&&GpuNoise1960.reservedBytes1971()==0,"concurrent caller ownership releases completely");
        WholeRoute1953.fail=true;check(s.reserveCapacity1971(new int[]{20},new long[]{4},0)==null,"post-add physical-budget exception returns declined admission");WholeRoute1953.fail=false;
        check(leases()==0&&GpuNoise1960.reservedBytes1971()==0,"exception rolls back unreachable lease and debt");
        WholeRoute1953.oom=true;check(s.reserveCapacity1971(new int[]{20},new long[]{4},0)==null,"post-add physical-budget OOM declines admission");WholeRoute1953.oom=false;
        check(leases()==0&&GpuNoise1960.reservedBytes1971()==0,"OOM rolls back unreachable lease and debt");
        GpuNoise1960.Lease1971 retained=s.reserveCapacity1971(new int[]{20},new long[]{4},0);check(retained!=null,"later valid reservation recovers after failures");
        s.close();check(leases()==0&&GpuNoise1960.reservedBytes1971()==0&&!retained.revalidate1971(),"session close releases every scalar lease");
        check(s.capacity1971()==null,"closed session cannot expose a new owner's capacities");retained.close();
        GpuNoise1960.Session next=open();check(next.failureCode1971()==0&&s.failureCode1971()==0,"closed clear diagnostic stable across new session");next.close();
        check(GpuNoise1960.MAX_BYTES==512*M,"fixed native limit preserved");
    }
    static GpuNoise1960.Batch compareBatch(){int[] u=new int[32];u[0]=4;return new GpuNoise1960.Batch().upload(0,new int[]{1,2,3,4}).upload(1,new int[]{1,2,3,4}).upload(2,new int[]{0}).dispatch(GpuNoise1960.COMPARE1961,new int[]{0,1,2},u,null,4);}
    static void fault(int code)throws Exception {
        check(GpuNoise1960.supports(GpuNoise1960.COMPARE1961),"real comparison program supported");
        GpuNoise1960.Session s=open();GpuNoise1960.Lease1971 lease=s.reserveCapacity1971(new int[]{0,1,2},new long[]{16,16,4},128);check(lease!=null,"fault candidate has owned scalar reservation");
        if(code==4){check(s.upload(0,new int[]{1,2,3,4}),"readback fault input upload succeeds");setFault(code);check(s.readInts(0,4)==null,"actual readback fault returns no candidate");}
        else if(code==5){GpuNoise1960.Ticket ticket=s.submit(compareBatch(),0);check(ticket!=null,"fence test submits actual private ticket");setFault(code);check(s.collect(ticket,new int[]{2},new int[]{1})==null,"actual timeout cannot return output");}
        else {setFault(code);check(!s.run(compareBatch()),"actual wrapped native command fault returns failure");}
        check(s.failureCode1971()==code,"fixed failure code retains originating stage "+code);
        setFault(0);check(s.failureCode1971()==code,"later generic failure does not overwrite first native reason");
        s.close();check(GpuNoise1960.reservedBytes1971()==0&&leases()==0,"failure session releases Java and pending capacity debt");lease.close();check(s.failureCode1971()==code,"closed session retains first scalar reason");
        if(code==5){check(GpuNoise1960.retainedBytes()>0,"unknown completion native capacities remain quarantined and counted");check(GpuNoise1960.open()==null,"timeout cannot reopen quarantined GPU context");GpuNoise1960.trimIdle();check(GpuNoise1960.retainedBytes()>0,"idle trim cannot free unknown-completion allocations");}
        else {GpuNoise1960.Session next=open();check(next.failureCode1971()==0&&s.failureCode1971()==code,"new session reset does not erase previous session cause");next.close();}
    }
    static void materializedReadback()throws Exception {
        GpuNoise1960.Session s=open();
        final int words=1024*1024;final long bytes=4L*words,headers=256;
        GpuNoise1960.Lease1971 lease=s.reserveCapacity1971(new int[]{0},new long[]{bytes},2*bytes+headers);
        check(lease!=null,"two actual readbacks have initial scalar admission");
        check(s.upload(0,new int[words]),"actual JNI source upload succeeds");
        long nativeBytes=GpuNoise1960.retainedBytes();
        check(GpuNoise1960.reservedBytes1971()==2*bytes+headers,"only future Java readbacks remain after native materialization");
        int[] first=s.readInts(0,words);
        check(first!=null&&first.length==words,"first private candidate materializes through JNI");
        long originalDebt=GpuNoise1960.reservedBytes1971();
        check(!lease.consumeJava1974(-1)&&!lease.consumeJava1974(originalDebt+1),"invalid consumption cannot release future allocation debt");
        check(GpuNoise1960.reservedBytes1971()==originalDebt,"invalid consumption leaves all debt intact");
        Runtime runtime=Runtime.getRuntime();
        long heapFree=runtime.maxMemory()-(runtime.totalMemory()-runtime.freeMemory());
        WholeRoute1953.retained=heapFree-nativeBytes-64*M-6*M;
        check(WholeRoute1953.retained>0,"host has room for narrow remaining-capacity case");
        check(!lease.revalidate1971(),"double charging first candidate rejects the next otherwise-fitting readback");
        check(lease.consumeJava1974(bytes),"first already-materialized Java payload retires only its future debt");
        check(GpuNoise1960.reservedBytes1971()==bytes+headers,"one future readback and conservative headers remain");
        check(GpuNoise1960.retainedBytes()==nativeBytes,"materialized Java accounting cannot release native banks or staging");
        check(lease.revalidate1971(),"second readback admitted with live first candidate counted once");
        WholeRoute1953.retained=0;
        int[] second=s.readInts(0,words);
        check(second!=null&&second.length==words,"second private candidate materializes through JNI");
        check(lease.consumeJava1974(bytes),"second candidate retires only remaining future payload");
        check(GpuNoise1960.reservedBytes1971()==headers,"header safety margin remains after both candidates");
        check(!lease.consumeJava1974(bytes),"duplicate payload consumption cannot erase header safety margin");
        check(first[0]==0&&first[words-1]==0&&second[0]==0&&second[words-1]==0,"both unchanged private candidates remain live through accounting");
        WholeRoute1953.retained=Long.MAX_VALUE;
        check(!lease.revalidate1971(),"other-owner memory pressure still blocks GPU after consumption");
        WholeRoute1953.retained=0;
        lease.close();check(!lease.consumeJava1974(0)&&GpuNoise1960.reservedBytes1971()==0,"closed lease refuses consumption and releases exactly its own debt");
        s.close();
    }
    static void overlapScratch()throws Exception {
        GpuNoise1960.Session s=open();final long javaPeak=4*M;
        GpuNoise1960.Lease1971 lease=s.reserveCapacity1971(new int[]{0},new long[]{4},javaPeak);
        check(lease!=null,"serial range admitted before optional overlap scratch");
        check(s.upload(0,new int[]{17}),"native capacities materialize before scratch test");
        long debt=GpuNoise1960.reservedBytes1971(),nativeBytes=GpuNoise1960.retainedBytes();
        check(debt==javaPeak,"baseline private readback debt only");
        check(!lease.tryReserveScratch1975(-1)&&!lease.tryReserveScratch1975(GpuNoise1960.MAX_BYTES+1),"invalid scratch peak rejected without mutation");
        Runtime runtime=Runtime.getRuntime();
        long heapFree=runtime.maxMemory()-(runtime.totalMemory()-runtime.freeMemory());
        WholeRoute1953.retained=heapFree-nativeBytes-64*M-6*M;
        check(!lease.tryReserveScratch1975(4*M),"optional overlap rejected when only serial range fits");
        check(GpuNoise1960.reservedBytes1971()==debt&&lease.revalidate1971(),"failed overlap leaves viable serial lease and exact debt");
        WholeRoute1953.retained=0;
        check(lease.tryReserveScratch1975(4*M),"scratch peak admitted after real budget recovery");
        check(GpuNoise1960.reservedBytes1971()==debt+4*M,"future CPU peak counted against host memory");
        check(GpuNoise1960.retainedBytes()==nativeBytes,"host peak changes no GPU allocation count");
        check(!lease.tryReserveScratch1975(4*M),"duplicate scratch ownership refused");
        check(GpuNoise1960.reservedBytes1971()==debt+4*M,"duplicate attempt cannot remove owned peak");
        lease.releaseScratch1975();check(GpuNoise1960.reservedBytes1971()==debt,"completed CPU peak releases only optional future debt");
        lease.releaseScratch1975();check(GpuNoise1960.reservedBytes1971()==debt,"scratch release is idempotent");
        WholeRoute1953.fail=true;check(!lease.tryReserveScratch1975(4*M),"physical query exception declines optional overlap");WholeRoute1953.fail=false;
        check(GpuNoise1960.reservedBytes1971()==debt&&lease.revalidate1971(),"exception rollback retains usable serial range");
        check(lease.tryReserveScratch1975(4*M),"scratch can be reserved anew for second CPU proof");
        s.close();check(GpuNoise1960.reservedBytes1971()==0&&!lease.tryReserveScratch1975(4*M),"closing with active optional peak releases all ownership");
        lease.releaseScratch1975();
    }
    static GpuNoise1960.Batch outputs(int[] pixels,int[] confidence,int status,int bank){
        return new GpuNoise1960.Batch().upload(bank,pixels).upload(bank+1,confidence).upload(bank+2,new int[]{status});
    }
    static void fill(int[][] values,int sentinel){for(int[] value:values)Arrays.fill(value,sentinel);}
    static void untouched(int[][] values,int sentinel,String label){for(int[] value:values)for(int word:value)check(word==sentinel,label);}
    static void reusedReadback()throws Exception {
        GpuNoise1960.Session s=open();int sentinel=0x5a17d00d;
        int[] pixels=new int[257],confidence=new int[19];for(int i=0;i<pixels.length;i++)pixels[i]=i*7919;for(int i=0;i<confidence.length;i++)confidence[i]=i*13;
        int[] slots={0,1,2},counts={pixels.length,confidence.length,1},offsets={3,2,1};
        int[][] targets={new int[pixels.length+7],new int[confidence.length+5],new int[4]};
        int[][] identity=targets.clone();fill(targets,sentinel);
        GpuNoise1960.Ticket ticket=s.submit(outputs(pixels,confidence,0,0),0);check(ticket!=null,"real three-output ticket submitted");
        check(!s.collectManyInto(ticket,slots,counts,new int[][]{targets[0],targets[1],targets[1]},offsets),"aliasing private targets rejected before ticket consumption");
        check(!s.collectManyInto(ticket,new int[]{0,1,1},counts,targets,offsets),"duplicate slots rejected before ticket consumption");
        check(!s.collectManyInto(ticket,slots,counts,targets,new int[]{3,2,4}),"last short output rejected before earlier target write");
        check(!s.collectManyInto(ticket,slots,new int[]{pixels.length,confidence.length,0},targets,offsets),"zero output count rejected");
        check(!s.collectManyInto(ticket,slots,counts,targets,new int[]{-1,2,1}),"negative offset rejected");
        untouched(targets,sentinel,"malformed transactions leave all private targets untouched");
        for(int iteration=0;iteration<20;iteration++){
            if(iteration>0){pixels[0]=iteration;confidence[0]=iteration*7;ticket=s.submit(outputs(pixels,confidence,0,0),0);check(ticket!=null,"same bank reusable after complete collection");}
            check(s.collectManyInto(ticket,slots,counts,targets,offsets),"actual JNI reuses all three private output arrays");
            for(int i=0;i<3;i++)check(targets[i]==identity[i],"private array identity remains unchanged across strips");
            for(int i=0;i<pixels.length;i++)check(targets[0][i+3]==pixels[i],"ARGB complete exact reusable readback");
            for(int i=0;i<confidence.length;i++)check(targets[1][i+2]==confidence[i],"confidence complete exact reusable readback");
            check(targets[2][1]==0,"policy status included in same successful transaction");
            check(targets[0][0]==sentinel&&targets[0][targets[0].length-1]==sentinel&&targets[1][0]==sentinel&&targets[1][targets[1].length-1]==sentinel&&targets[2][0]==sentinel&&targets[2][3]==sentinel,"reused buffer prefixes and tails untouched");
            check(!s.collectManyInto(ticket,slots,counts,targets,offsets),"completed ticket cannot be consumed twice");
        }
        GpuNoise1960.Ticket one=s.submit(outputs(pixels,confidence,0,0),0),two=s.submit(outputs(confidence,pixels,0,14),1);
        check(one!=null&&two!=null,"both native banks pending independently");
        int[][] other={new int[19],new int[257],new int[1]};
        check(s.collectManyInto(two,new int[]{14,15,16},new int[]{19,257,1},other,new int[]{0,0,0}),"bank one collects before bank zero");
        check(s.collectManyInto(one,slots,counts,targets,offsets),"older bank zero ticket still collects after newer bank");
        s.close();
        GpuNoise1960.Session bounded=open();int[][] invalid={new int[257],new int[20],new int[1]};fill(invalid,sentinel);
        ticket=bounded.submit(outputs(pixels,confidence,0,0),0);check(ticket!=null,"native-capacity validation ticket submitted");
        check(!bounded.collectManyInto(ticket,slots,new int[]{257,20,1},invalid,new int[]{0,0,0}),"native validates every output against actual used GPU bytes");
        untouched(invalid,sentinel,"late native shape rejection happens before any private write");bounded.close();
    }
    static void privateReadbackFault()throws Exception {
        GpuNoise1960.Session s=open();int sentinel=0x5a17d00d;
        int[][] privateOutput={new int[4],new int[3],new int[1]};fill(privateOutput,sentinel);
        int[][] published={new int[4],new int[3],new int[1]};fill(published,sentinel);
        GpuNoise1960.Ticket ticket=s.submit(outputs(new int[]{11,22,33,44},new int[]{55,66,77},0,0),0);
        check(ticket!=null,"late readback-fault candidate submits");setFault(6);
        boolean valid=s.collectManyInto(ticket,new int[]{0,1,2},new int[]{4,3,1},privateOutput,new int[]{0,0,0});
        if(valid)for(int i=0;i<3;i++)System.arraycopy(privateOutput[i],0,published[i],0,privateOutput[i].length);
        check(!valid,"second-output unmap fault invalidates entire transaction");
        check(privateOutput[0][0]==11&&privateOutput[1][0]==55&&privateOutput[2][0]==sentinel,"late fault dirties only private candidate and stops remaining reads");
        untouched(published,sentinel,"partial private bytes never reach published arrays");
        check(s.failureCode1971()==4,"multi-readback retains originating readback fault code");
        check(s.submit(outputs(new int[]{1},new int[]{1},0,0),0)==null,"failed private transaction cannot reuse bank");
        setFault(0);s.close();check(GpuNoise1960.reservedBytes1971()==0,"late failure close retires all Java ownership");
        GpuNoise1960.Session next=open();next.close();
    }
    static void cancelledReadback()throws Exception {
        final GpuNoise1960.Session s=open();final int sentinel=0x5a17d00d;
        final int[][] targets={new int[4],new int[3],new int[1]};fill(targets,sentinel);
        final GpuNoise1960.Ticket ticket=s.submit(outputs(new int[]{11,22,33,44},new int[]{55,66,77},0,0),0);
        check(ticket!=null,"cancel test ticket submitted");setFault(5);
        final boolean[] completed={true};Thread caller=new Thread(()->completed[0]=s.collectManyInto(ticket,new int[]{0,1,2},new int[]{4,3,1},targets,new int[]{0,0,0}));
        caller.start();Thread.sleep(30);caller.interrupt();caller.join(3000);
        check(!caller.isAlive()&&!completed[0]&&caller.isInterrupted(),"interrupted pending collection returns false and preserves cancellation");
        untouched(targets,sentinel,"cancelled collection never touches private targets");
        setFault(0);s.close();GpuNoise1960.Session next=open();next.close();
    }
    static void multiFenceQuarantine()throws Exception {
        GpuNoise1960.Session s=open();int[][] targets={new int[4],new int[3],new int[1]};fill(targets,7);
        GpuNoise1960.Ticket ticket=s.submit(outputs(new int[]{1,2,3,4},new int[]{5,6,7},0,0),0);check(ticket!=null,"multi-readback fence test submits");
        setFault(5);check(!s.collectManyInto(ticket,new int[]{0,1,2},new int[]{4,3,1},targets,new int[]{0,0,0}),"unknown completion cannot return reusable output");
        check(s.failureCode1971()==5,"multi-readback timeout retains fence failure code");untouched(targets,7,"timeout produces no private output");
        setFault(0);s.close();check(GpuNoise1960.retainedBytes()>0&&GpuNoise1960.open()==null,"unknown completion keeps native allocations quarantined");
        GpuNoise1960.trimIdle();check(GpuNoise1960.retainedBytes()>0,"trim cannot erase quarantined multi-readback ownership");
    }
    public static void main(String[] args)throws Exception {if(args.length==0||"logic".equals(args[0]))logic();else if("readback".equals(args[0]))materializedReadback();else if("scratch".equals(args[0]))overlapScratch();else if("reuse".equals(args[0]))reusedReadback();else if("late".equals(args[0]))privateReadbackFault();else if("cancel".equals(args[0]))cancelledReadback();else if("multifence".equals(args[0]))multiFenceQuarantine();else fault(Integer.parseInt(args[0]));System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+"}");}
}

final class WholeRoute1953 {static volatile long retained;static volatile boolean fail,oom;static long retainedBytes(){if(fail)throw new IllegalStateException("budget fixture fault");if(oom)throw new OutOfMemoryError("budget fixture fault");return retained;}}
final class GpuFinish1953 {static long retainedBytes(){return 0;}}
final class SpeedWorkers1935 {static long nativeRetainedBytes1956(){return 0;}}
