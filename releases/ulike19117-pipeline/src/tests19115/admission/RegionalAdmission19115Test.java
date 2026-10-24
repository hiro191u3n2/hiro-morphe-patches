package com.hiro.ulike;

import java.lang.reflect.Field;
import java.util.concurrent.CancellationException;

/** Real admission state machine against explicit controlled transports. */
public final class RegionalAdmission19115Test {
    static int assertions,scenarios;
    static final GpuQualification1961.Cancellation GO=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
    static final GpuQualification1961.Cancellation STOP=new GpuQualification1961.Cancellation(){public boolean cancelled(){return true;}};
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static void reset()throws Exception{
        scenarios++;Thread.interrupted();GpuCertificates1990.proofs.clear();GpuCertificates1990.rejects.clear();GpuCertificates1990.environment="device-A";GpuCertificates1990.failWrite=null;GpuCertificates1990.interruptExact=false;GpuCertificates1990.writes=0;
        GpuNoise1960.prepared=false;GpuNoise1960.supported=true;GpuNoise1960.prepareCalls=GpuNoise1960.readinessCalls=0;
        GpuQualification1961.allowed=true;GpuQualification1961.queueAccept=true;GpuQualification1961.rejected=false;GpuQualification1961.throwSchedule=GpuQualification1961.oomSchedule=GpuQualification1961.underHelperLock=false;GpuQualification1961.scheduled=GpuQualification1961.exceptions=0;GpuQualification1961.outcome=GpuQualification1961.key=null;GpuQualification1961.queued=null;
        GpuStrong1960.fault=GpuStrong1960.candidateCalls=GpuStrong1960.baselineCalls=GpuStrong1960.requiredCalls=GpuStrong1960.slowWidth=0;GpuStrong1960.returnFallback=GpuStrong1960.cancelTransport=GpuStrong1960.transportThrow=GpuStrong1960.transportOom=false;GpuStrong1960.order.setLength(0);
        StrongNoise1958.oracleSuccess=true;StrongNoise1958.oracleThrow=StrongNoise1958.oracleOom=false;StrongNoise1958.oracles=0;GpuStartup1990.invalidated=false;ProcessingTiming1947.epoch=2;
        for(String name:new String[]{"requested","attempted"}){Field f=GpuRegionalDefault19115.class.getDeclaredField(name);f.setAccessible(true);f.setBoolean(null,false);}
        GpuCertificates1990.passed(GpuStrongDefault1990.SUITE,68,1,1,1);
    }
    static void proof(){GpuCertificates1990.passed(GpuRegionalDefault19115.EXACT,70,1,1,1);GpuCertificates1990.passed(GpuRegionalDefault19115.FAST,70,1,1,1);}
    static boolean cancelled(Runnable action){try{action.run();return false;}catch(CancellationException expected){return true;}}
    static void noCandidateProof(String why){check(GpuCertificates1990.restore(GpuRegionalDefault19115.FAST)==null,why+" cannot certify speed");check(!GpuRegionalDefault19115.faster(),why+" cannot select candidate");}
    static void runQueued(GpuQualification1961.Cancellation c){GpuQualification1961.Probe probe=GpuQualification1961.queued;check(probe!=null,"queue transferred a concrete probe");try{probe.run(c);}finally{probe.close();}}
    public static void main(String[] args)throws Exception{
        reset();
        check(!GpuRegionalDefault19115.EXACT.equals(GpuRegionalDefault19115.FAST),"exact and speed proofs independent");
        check(!GpuRegionalDefault19115.EXACT.equals(GpuStrongDefault1990.SUITE),"new candidate never borrows standard exact proof");
        check(GpuRegionalDefault19115.testedShape(3060,4080)&&GpuRegionalDefault19115.testedShape(4080,3060),"both tested orientations");
        for(int[] shape:new int[][]{{3060,3060},{4080,4080},{65,65},{0,4080},{3060,0},{-1,3060}})check(!GpuRegionalDefault19115.testedShape(shape[0],shape[1]),"unmeasured geometry refused");
        int[] u=GpuStrongDefault1990.Adaptive1994.benchmark19112(64,GO).u;
        check(GpuRegionalDefault19115.eligible(u),"256 tested rows eligible");u=u.clone();u[3]--;check(!GpuRegionalDefault19115.eligible(u),"short tail not speed certified");u[3]+=2;check(!GpuRegionalDefault19115.eligible(u),"257 rows not speed certified");
        check(!GpuRegionalDefault19115.eligible(null)&&!GpuRegionalDefault19115.eligible(new int[4]),"malformed input refused");
        int[] invalid=GpuStrongDefault1990.Adaptive1994.benchmark19112(64,GO).u;invalid[10]=0;check(!GpuRegionalDefault19115.eligible(invalid),"standard input domain remains necessary");
        check(!GpuRegionalDefault19115.faster()&&GpuNoise1960.prepareCalls==0,"foreground readiness never prepares");
        GpuNoise1960.prepared=true;GpuCertificates1990.passed(GpuRegionalDefault19115.EXACT,70,1,1,1);check(!GpuRegionalDefault19115.faster(),"quality alone cannot select");GpuNoise1960.prepared=false;
        proof();check(!GpuRegionalDefault19115.faster(),"restored certificates still need program preparation");GpuNoise1960.prepared=true;check(GpuRegionalDefault19115.faster(),"separate exact+speed+prepared select");
        check(GpuRegionalDefault19115.accepts(GpuStrongDefault1990.Adaptive1994.benchmark19112(65,GO).u),"prepared certified orientation accepted");
        GpuQualification1961.rejected=true;check(!GpuRegionalDefault19115.accepts(GpuStrongDefault1990.Adaptive1994.benchmark19112(65,GO).u),"per-shape standard rejection remains authoritative");GpuQualification1961.rejected=false;
        GpuCertificates1990.proofs.remove(GpuCertificates1990.full(GpuStrongDefault1990.SUITE));check(!GpuRegionalDefault19115.faster(),"standard proof remains independently required");GpuCertificates1990.passed(GpuStrongDefault1990.SUITE,68,1,1,1);
        GpuCertificates1990.environment="device-B";check(!GpuRegionalDefault19115.faster(),"certificate fingerprint boundary respected");GpuCertificates1990.environment="device-A";
        GpuCertificates1990.reject(GpuRegionalDefault19115.EXACT,"controlled mismatch");check(!GpuRegionalDefault19115.faster(),"sticky quality rejection wins");
        reset();for(long[] pair:new long[][]{{100,95},{100,94},{20,19},{1000,950}})check(GpuRegionalDefault19115.measuredWin(pair[0],pair[1]),"5 percent inclusive threshold");
        for(long[] pair:new long[][]{{100,96},{100,100},{0,1},{1,0},{-1,1},{Long.MAX_VALUE,1}})check(!GpuRegionalDefault19115.measuredWin(pair[0],pair[1]),"invalid or insufficient measured improvement refused");
        reset();GpuQualification1961.allowed=false;GpuRegionalDefault19115.request();check(GpuQualification1961.scheduled==0&&GpuNoise1960.prepareCalls==0,"no optional queue/compile before publication");
        proof();GpuRegionalDefault19115.request();check(GpuQualification1961.scheduled==0&&GpuNoise1960.prepareCalls==0,"cached proof does not compile before publication");
        check(cancelled(new Runnable(){public void run(){GpuRegionalDefault19115.qualify(GO,false);}}),"direct cached preparation checks publication boundary");check(GpuNoise1960.prepareCalls==0,"direct cached call cannot compile early");
        reset();GpuCertificates1990.proofs.clear();check(!GpuRegionalDefault19115.qualify(GO),"standard baseline must be available");check(GpuNoise1960.prepareCalls==0,"baseline absent no optional compile");
        reset();GpuCertificates1990.reject(GpuRegionalDefault19115.EXACT,"restored mismatch");check(!GpuRegionalDefault19115.qualify(GO),"known rejected candidate not retried");check(GpuNoise1960.prepareCalls==0,"known rejection no compile");
        reset();GpuNoise1960.supported=false;check(!GpuRegionalDefault19115.qualify(GO),"unsupported native program refused");noCandidateProof("unsupported");check(!GpuCertificates1990.rejected(GpuRegionalDefault19115.EXACT),"unavailable is not a quality rejection");
        reset();proof();check(GpuRegionalDefault19115.qualify(GO,false),"cached preparation after publication succeeds");check(GpuNoise1960.prepareCalls==1&&GpuStrong1960.candidateCalls==0&&StrongNoise1958.oracles==0,"cached prepare does not benchmark");
        reset();check(cancelled(new Runnable(){public void run(){GpuRegionalDefault19115.qualify(GO,false);}}),"uncached lightweight job cannot become heavy proof");noCandidateProof("uncached lightweight");
        reset();check(cancelled(new Runnable(){public void run(){GpuRegionalDefault19115.qualify(STOP);}}),"capture cancellation before proof propagates");check(GpuNoise1960.prepareCalls==0,"cancelled proof cannot prepare");
        for(int fault:new int[]{1,2,3,4,6,7,8}){
            reset();GpuStrong1960.fault=fault;check(!GpuRegionalDefault19115.qualify(GO),"controlled output/transport fault refused");noCandidateProof("fault"+fault);
            check(GpuCertificates1990.rejected(GpuRegionalDefault19115.EXACT)==(fault==1||fault==2||fault==6||fault==7),"only output mismatch is sticky");
            check(GpuStrongDefault1990.ready()&&!GpuStartup1990.invalidated,"new candidate failure preserves standard");
        }
        reset();GpuStrong1960.returnFallback=true;check(!GpuRegionalDefault19115.qualify(GO),"unreported old-route fallback cannot certify candidate");noCandidateProof("fallback");check(!GpuCertificates1990.rejected(GpuRegionalDefault19115.EXACT),"fallback is not quality mismatch");
        reset();StrongNoise1958.oracleSuccess=false;check(!GpuRegionalDefault19115.qualify(GO),"missing CPU result refused");noCandidateProof("oracle unavailable");
        reset();StrongNoise1958.oracleThrow=true;check(!GpuRegionalDefault19115.qualify(GO),"oracle exception refused");noCandidateProof("oracle exception");
        reset();StrongNoise1958.oracleOom=true;check(!GpuRegionalDefault19115.qualify(GO),"oracle memory refusal");noCandidateProof("oracle OOM");
        reset();GpuStrong1960.transportThrow=true;check(!GpuRegionalDefault19115.qualify(GO),"transport exception refused");noCandidateProof("transport exception");
        reset();GpuStrong1960.transportOom=true;check(!GpuRegionalDefault19115.qualify(GO),"transport OOM refused");noCandidateProof("transport OOM");
        reset();GpuStrong1960.cancelTransport=true;check(cancelled(new Runnable(){public void run(){GpuRegionalDefault19115.qualify(GO);}}),"in-flight cancellation propagates");noCandidateProof("transport cancellation");
        reset();GpuStrong1960.fault=5;check(!GpuRegionalDefault19115.qualify(GO),"actual standard mismatch refused");check(GpuStartup1990.invalidated&&!GpuStrongDefault1990.ready(),"actual standard mismatch invalidates standard domain");check(!GpuCertificates1990.rejected(GpuRegionalDefault19115.EXACT),"standard fault never labels candidate mismatching");
        reset();check(GpuRegionalDefault19115.qualify(GO),"all quality plus both-width timing conditions admit candidate");check(GpuRegionalDefault19115.faster(),"successful proof selectable");
        GpuCertificates1990.Evidence evidence=GpuCertificates1990.restore(GpuRegionalDefault19115.EXACT);check(evidence!=null&&evidence.cases==70&&evidence.pixels==1864832L,"70 whole output/confidence cases certified");
        check(GpuStrong1960.candidateCalls==74&&GpuStrong1960.requiredCalls==74&&GpuStrong1960.baselineCalls==6,"every candidate call requires actual new route and both widths get two trials");
        check(GpuStrong1960.order.toString().equals("3060C,3060B,3060B,3060C,3060C,3060B,4080C,4080B,4080B,4080C,4080C,4080B,"),"two orientations measured AB/BA after both warm paths");
        for(int width:new int[]{3060,4080}){reset();GpuStrong1960.slowWidth=width;check(!GpuRegionalDefault19115.qualify(GO),"either measured orientation can veto speed");check(GpuCertificates1990.restore(GpuRegionalDefault19115.EXACT)!=null,"slower exact candidate keeps only quality proof");noCandidateProof("one slow width");}
        reset();GpuCertificates1990.failWrite=GpuRegionalDefault19115.EXACT;check(!GpuRegionalDefault19115.qualify(GO),"failed exact persistence refuses adoption");noCandidateProof("exact write failed");
        reset();GpuCertificates1990.failWrite=GpuRegionalDefault19115.FAST;check(!GpuRegionalDefault19115.qualify(GO),"failed speed persistence refuses adoption");noCandidateProof("speed write failed");
        reset();GpuCertificates1990.interruptExact=true;check(cancelled(new Runnable(){public void run(){GpuRegionalDefault19115.qualify(GO);}}),"cancellation between certificate writes is respected");Thread.interrupted();noCandidateProof("interrupted persistence");
        reset();GpuRegionalDefault19115.request();check(GpuRegionalDefault19115.pending(),"accepted request remains pending until close");GpuRegionalDefault19115.request();check(GpuQualification1961.scheduled==1,"pending request not duplicated");check(!GpuQualification1961.underHelperLock,"queue entry occurs outside helper lock");
        check(cancelled(new Runnable(){public void run(){runQueued(STOP);}}),"queued cancellation propagates");check(!GpuRegionalDefault19115.pending(),"queued cancellation close clears pending");GpuRegionalDefault19115.request();check(GpuQualification1961.scheduled==2&&GpuRegionalDefault19115.pending(),"cancelled request rearms");GpuQualification1961.queued.close();check(!GpuRegionalDefault19115.pending(),"close without run clears pending");
        reset();GpuQualification1961.queueAccept=false;GpuRegionalDefault19115.request();check(!GpuRegionalDefault19115.pending(),"queue refusal clears pending");GpuQualification1961.queueAccept=true;GpuRegionalDefault19115.request();check(GpuRegionalDefault19115.pending()&&GpuQualification1961.scheduled==2,"queue refusal can rearm");GpuQualification1961.queued.close();
        reset();GpuQualification1961.throwSchedule=true;GpuRegionalDefault19115.request();check(!GpuRegionalDefault19115.pending(),"queue exception clears pending");GpuQualification1961.throwSchedule=false;GpuRegionalDefault19115.request();check(GpuRegionalDefault19115.pending(),"queue exception can rearm");GpuQualification1961.queued.close();
        reset();GpuQualification1961.oomSchedule=true;GpuRegionalDefault19115.request();check(!GpuRegionalDefault19115.pending(),"queue OOM clears pending");GpuQualification1961.oomSchedule=false;GpuRegionalDefault19115.request();check(GpuRegionalDefault19115.pending(),"queue OOM can rearm");GpuQualification1961.queued.close();
        reset();GpuNoise1960.supported=false;GpuRegionalDefault19115.request();runQueued(GO);check(!GpuRegionalDefault19115.pending(),"completed failure close clears pending");GpuRegionalDefault19115.request();check(GpuQualification1961.scheduled==1,"completed unsuccessful optional proof is bounded per process");
        reset();proof();GpuRegionalDefault19115.request();runQueued(GO);check(!GpuRegionalDefault19115.pending()&&GpuRegionalDefault19115.faster(),"cached preparation closes ownership and selects");GpuRegionalDefault19115.request();check(GpuQualification1961.scheduled==1,"ready candidate not repeatedly queued");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+",\"controlled_transport_only\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
    }
}
