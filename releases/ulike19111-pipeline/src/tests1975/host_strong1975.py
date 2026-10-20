#!/usr/bin/env python3
"""Execute Strong 1975 production control flow with deterministic transport.
Fixtures test ownership/admission/output selection, not physical Android speed.
"""
from pathlib import Path
import argparse, hashlib, json, re, shutil, subprocess
ROOT=Path(__file__).resolve().parent.parent
EXTRA=r'''
    static void failedPreflightRefundsSameCaptureProof()throws Exception{
        StrongNoise1958.Model model=new StrongNoise1958.Model();GpuStrong1960.beginStage(model);
        try{
            GpuNoise1960.reservationLimit=8000L;
            for(int i=0;i<3;i++){
                Job blocked=new Job(model,1400+i);check(blocked.complete(),"capacity refusal still completes ordinary CPU output");blocked.exact("preflight fallback");
                check(blocked.cpuCalls==1&&GpuNoise1960.executes==0,"preflight refusal performs no repeated CPU or GPU proof");
                check(ProcessingTiming1947.verification==0,"ordinary CPU fallback is not a qualification reference");
            }
            GpuNoise1960.reservationLimit=GpuNoise1960.MAX_BYTES;
            Job recovered=new Job(model,1410);check(recovered.complete(),"same capture can qualify after transient memory recovers");recovered.exact("same-capture recovery");
            check(recovered.cpuCalls==2&&GpuNoise1960.executes==2,"refunded preflight does not poison key or consume proof budget");
            Job reuse=new Job(model,1411);check(reuse.complete(),"same capture reuses new certificate");reuse.exact("certificate reuse");
            check(reuse.cpuCalls==0&&GpuNoise1960.executes==3,"reused GPU result needs no CPU reference");
        }finally{close(model);}
        check(ProcessingTiming1947.lastGpu==2&&ProcessingTiming1947.lastCpu==3,"selected GPU counts reflect real recovered GPU outputs");
        check(GpuNoise1960.consumptions1974==2,"two private cold buffers allocate once and certified reuse allocates none");clean();
    }
    static void pendingAdmissionRefundIsBounded()throws Exception{
        Method admit=GpuStrong1960.class.getDeclaredMethod("admit1973",Class.forName("com.hiro.ulike.GpuStrong1960$Stage"),StrongNoise1958.Model.class,String.class);
        Method release=GpuStrong1960.class.getDeclaredMethod("release1973",Class.forName("com.hiro.ulike.GpuStrong1960$Flight1973"));
        Method commit=GpuStrong1960.class.getDeclaredMethod("commit1974",Class.forName("com.hiro.ulike.GpuStrong1960$Flight1973"));
        admit.setAccessible(true);release.setAccessible(true);commit.setAccessible(true);
        StrongNoise1958.Model model=new StrongNoise1958.Model();Object a=admit.invoke(null,null,model,"one"),b=admit.invoke(null,null,model,"two");
        Object refused=admit.invoke(null,null,model,"three");Field failure=refused.getClass().getDeclaredField("failure");failure.setAccessible(true);
        check("cold_proof_budget".equals(failure.get(refused)),"two tentative owners already bound total future proof work");
        release.invoke(null,a);Object retry=admit.invoke(null,null,model,"one");
        check(failure.get(retry)==null&&field("activeCold1973").getInt(null)==2,"unused tentative owner refunds same key and one slot");
        commit.invoke(null,retry);commit.invoke(null,retry);commit.invoke(null,b);release.invoke(null,retry);release.invoke(null,b);
        Object exhausted=admit.invoke(null,null,model,"four");check("cold_proof_budget".equals(failure.get(exhausted)),"committed attempts remain bounded even after lease release");
        clean();
    }
    static void nearCompleteProofFeedsSameCaptureFollower()throws Exception{
        StrongNoise1958.Model model=new StrongNoise1958.Model();GpuNoise1960.holdTickets=true;
        final Worker first=new Worker(new Job(model,1500)),follower=new Worker(new Job(model,1501));Thread owner=new Thread(first),next=new Thread(follower);
        GpuStrong1960.beginStage(model);try{
            owner.start();until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==1;}},"first exact trial starts");
            next.start();until(new Callable<Boolean>(){public Boolean call(){return next.getState()==Thread.State.TIMED_WAITING;}},"same key briefly awaits its private proof owner");
            GpuNoise1960.holdTickets=false;GpuNoise1960.releaseAll();join(owner,"initial owner publishes exact certificate");join(next,"follower reuses certificate within same capture");
            check(first.failure.get()==null&&follower.failure.get()==null&&first.done&&follower.done,"both workers finish successfully");
            first.job.exact("owner output");follower.job.exact("follower output");
            check(first.job.cpuCalls==2&&follower.job.cpuCalls==0&&GpuNoise1960.executes==3,"same-key follower runs GPU once and never repeats CPU proof");
        }finally{GpuNoise1960.holdTickets=false;GpuNoise1960.releaseAll();if(owner.isAlive())owner.join(5000);if(next.isAlive())next.join(5000);close(model);}
        check(ProcessingTiming1947.lastGpu==2&&ProcessingTiming1947.lastCpu==0,"successful proof reuse records two real GPU outputs");clean();
    }
    static void proofReuseWaitInterruptionPreservesOwner()throws Exception{
        StrongNoise1958.Model model=new StrongNoise1958.Model();GpuNoise1960.holdTickets=true;
        final Worker first=new Worker(new Job(model,1550)),follower=new Worker(new Job(model,1551));Thread owner=new Thread(first),next=new Thread(follower);
        GpuStrong1960.beginStage(model);try{
            owner.start();until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==1;}},"owner holds candidate before reuse cancellation");
            next.start();until(new Callable<Boolean>(){public Boolean call(){return next.getState()==Thread.State.TIMED_WAITING;}},"follower waits for proof completion");
            next.interrupt();join(next,"interrupted reuse waiter exits promptly");
            check(follower.failure.get() instanceof CancellationException&&follower.job.cpuCalls==0,"interrupted reuse wait propagates cancellation without CPU or new proof");
            check(GpuNoise1960.submits==1&&field("activeCold1973").getInt(null)==1,"cancelled waiter cannot release the other worker's proof ownership");
            GpuNoise1960.holdTickets=false;GpuNoise1960.releaseAll();join(owner,"original proof survives follower cancellation");
            check(first.failure.get()==null&&first.done&&first.job.cpuCalls==2,"owner still completes both exact comparisons");first.job.exact("owner after waiter cancellation");
        }finally{GpuNoise1960.holdTickets=false;GpuNoise1960.releaseAll();if(owner.isAlive())owner.join(5000);if(next.isAlive())next.join(5000);close(model);}
        check(ProcessingTiming1947.lastGpu==1&&GpuQualification1961.preferredCalls==1,"only owner publishes a verified GPU result");clean();
    }
    static void overlappingProofReusesBankInputsAndReadbacks()throws Exception{
        StrongNoise1958.Model model=new StrongNoise1958.Model();Job first=new Job(model,1700);first.overlapExpectation1975=1;first.expectedOracleThread1975=Thread.currentThread();
        GpuStrong1960.beginStage(model);try{
            check(first.complete(),"overlapping proof completes");first.exact("overlap both trials");
            check(first.cpuCalls==2&&GpuNoise1960.submits==2&&GpuNoise1960.executes==2,"both overlapping trials keep complete CPU and GPU work");
            check(GpuNoise1960.sourceUploads1975==1,"second GPU trial reuses source upload under the same exclusive bank");
            check(GpuNoise1960.readbacks1975==2&&GpuNoise1960.seenTargets1975.size()==2,"cold trials use distinct private targets preserving first candidate");
            Job repeat=new Job(model,1701);check(repeat.complete(),"certified same-shape strip completes");repeat.exact("reused readback");
            check(repeat.cpuCalls==0&&GpuNoise1960.bufferReuses1975==1&&GpuNoise1960.consumptions1974==2,"certified strip reuses first buffer with zero image-sized Java allocation");
            check(GpuNoise1960.sourceUploads1975==2,"next strip uploads its own input once and cannot reuse previous picture data");
        }finally{close(model);}clean();
    }
    static void scratchDeclineRetainsSequentialGpuProof()throws Exception{
        GpuNoise1960.scratchFits1975=false;StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,1800);
        job.overlapExpectation1975=-1;job.expectedOracleThread1975=Thread.currentThread();GpuStrong1960.beginStage(model);
        try{check(job.complete(),"insufficient concurrent scratch preserves serial verification GPU path");job.exact("serial low memory proof");}
        finally{close(model);}
        check(job.cpuCalls==2&&GpuNoise1960.submits==2&&GpuNoise1960.executes==2&&ProcessingTiming1947.lastGpu==1,"declined overlap does not cause CPU-only capture");
        check(GpuNoise1960.sourceUploads1975==1&&GpuNoise1960.scratch1975==0,"serial fallback still reuses immutable trial inputs and releases all debt");clean();
    }
    static void pendingOracleFailureQuarantinesBank()throws Exception{
        StrongNoise1958.Model model=new StrongNoise1958.Model();Job failed=new Job(model,1900);failed.throwSecond=true;failed.overlapExpectation1975=1;
        GpuStrong1960.beginStage(model);try{
            check(!failed.raw(),"partial overlapping CPU oracle failure returns caller repair path");
            check(GpuNoise1960.submits==2&&GpuNoise1960.inflight==1,"uncollected second GPU candidate is still owned by failed stage");
            Job follow=new Job(model,1901);check(follow.complete(),"later strip has ordinary CPU fallback");follow.exact("quarantined bank CPU fallback");
            check(follow.cpuCalls==1&&GpuNoise1960.submits==2,"failed stage cannot reassign live native bank to another strip");
        }finally{close(model);}
        check(GpuNoise1960.inflight==0&&GpuNoise1960.scratch1975==0,"stage close drains its pending ticket and all scratch debt");clean();
    }
    static void certifiedReadbackReservesOneResultOnly()throws Exception{
        StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,1600);certified(job,0,0);
        GpuStrong1960.beginStage(model);try{check(job.complete(),"certified route completes");job.exact("single readback");}finally{close(model);}
        long expected=4L*(1024+64+1)+256;boolean found=false;
        for(GpuNoise1960.Reservation r:GpuNoise1960.reservations)if(r.javaBytes>0){found=true;check(r.javaBytes==expected,"certified route reserves exactly one result plus bounded metadata");}
        check(found&&GpuNoise1960.consumptions1974==1&&job.cpuCalls==0,"one GPU result is consumed once without CPU qualification");clean();
    }
'''
def test(source,work,jdk=None):
    source=Path(source).resolve()
    if source.is_dir():source=source/'GpuStrong1960.java'
    work=Path(work).resolve()/'strong1975'; classes=work/'classes'; fixtures=work/'fixtures'
    if classes.exists():shutil.rmtree(classes)
    classes.mkdir(parents=True,exist_ok=True);fixtures.mkdir(parents=True,exist_ok=True)
    fixture=(ROOT/'latency73testfixtures/FixtureClasses.java').read_text()
    fixture=fixture.replace('static volatile long reservedJava,reservationLimit=MAX_BYTES;', 'static volatile long reservedJava,reservationLimit=MAX_BYTES;static int consumptions1974;')
    fixture=fixture.replace('reservedJava=0;reservationLimit=MAX_BYTES;', 'reservedJava=0;consumptions1974=0;reservationLimit=MAX_BYTES;')
    fixture=fixture.replace('static final class Lease1971 {final long javaBytes;', 'static final class Lease1971 {long javaBytes;')
    fixture=fixture.replace('Lease1971(long j){javaBytes=j;}boolean revalidate1971()', 'Lease1971(long j){javaBytes=j;}boolean consumeJava1974(long bytes){synchronized(GpuNoise1960.class){if(closed||bytes<0||bytes>javaBytes)return false;javaBytes-=bytes;reservedJava-=bytes;consumptions1974++;return true;}}boolean revalidate1971()')
    fixture=fixture.replace('static volatile long reservedJava,reservationLimit=MAX_BYTES;', 'static volatile long reservedJava,reservationLimit=MAX_BYTES;static boolean scratchFits1975=true;static long scratch1975;static int sourceUploads1975,readbacks1975,bufferReuses1975;static java.util.Set<int[]> seenTargets1975=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<int[],Boolean>());')
    fixture=fixture.replace('reservedJava=0;consumptions1974=0;', 'reservedJava=0;consumptions1974=0;scratchFits1975=true;scratch1975=0;sourceUploads1975=readbacks1975=bufferReuses1975=0;seenTargets1975.clear();')
    fixture=fixture.replace('Lease1971(long j){javaBytes=j;}boolean consumeJava1974', 'long scratch;Lease1971(long j){javaBytes=j;}boolean tryReserveScratch1975(long bytes){synchronized(GpuNoise1960.class){if(!scratchFits1975)return false;if(scratch!=0)throw new AssertionError("nested scratch");scratch=bytes;scratch1975+=bytes;return true;}}void releaseScratch1975(){synchronized(GpuNoise1960.class){scratch1975-=scratch;scratch=0;}}boolean consumeJava1974')
    fixture=fixture.replace('if(slot==0||slot==14)source=x.clone();', 'if(slot==0||slot==14){source=x.clone();sourceUploads1975++;}')
    fixture=fixture.replace('final Ticket[] pending=new Ticket[2];', 'final Ticket[] pending=new Ticket[2];final int[][] residentSources1975=new int[2][];')
    fixture=fixture.replace('int id;synchronized(GpuNoise1960.class)', 'if(b.source!=null)residentSources1975[bank]=b.source;else b.source=residentSources1975[bank];int id;synchronized(GpuNoise1960.class)')
    fixture=fixture.replace('        int[][] collect(final Ticket', '        int[][] collect(final Ticket')
    fixture=fixture.replace('        int[][] collect(Ticket t,int[] slots,int[] counts){', '        boolean collectManyInto(Ticket t,int[] slots,int[] counts,int[][] targets,int[] offsets){int[][] actual=collect(t,slots,counts);if(actual==null)return false;for(int i=0;i<counts.length;i++){if(actual[i].length!=counts[i])return false;System.arraycopy(actual[i],0,targets[i],offsets[i],counts[i]);}synchronized(GpuNoise1960.class){readbacks1975++;if(!seenTargets1975.add(targets[0]))bufferReuses1975++;}return true;}\n        int[][] collect(Ticket t,int[] slots,int[] counts){')
    fixture=fixture.replace('for(Ticket ticket:pending)if(ticket!=null)throw new AssertionError("session closed before collectors join");', 'for(int i=0;i<pending.length;i++)if(pending[i]!=null){pending[i]=null;inflight--;}')
    fixture=fixture.replace('static int pixel(int value)', 'static long workspaceBytes(int width,int rows){return (long)width*(rows*48L+640L)+1048576L;}static int pixel(int value)')
    fixture+='\nfinal class GpuStrongTuning1975 {static final class Choice {int profile,variant;String key;}static Choice select(int[] u){return null;}static void schedule(int[] a,int[] b,int[] u,StrongNoise1958.Model m,GpuPolicy1960.Protection p){}}\n'
    fixture_file=fixtures/'FixtureClasses.java';fixture_file.write_text(fixture)
    tests=(ROOT/'latency73testfixtures/StrongLatency1973Test.java').read_text()
    tests=tests.replace('GpuNoise1960.opens==0&&GpuNoise1960.modelUploads==0&&GpuNoise1960.reservationCalls==0,\n                "cold CPU reference precedes model transfer and allocation"', 'GpuNoise1960.opens>0&&GpuNoise1960.modelUploads>0&&GpuNoise1960.liveRangeLeases>0,\n                "cold CPU reference follows successful model and range preflight"')
    tests=tests.replace('GpuPolicy1960.Protection protection;', 'GpuPolicy1960.Protection protection;int overlapExpectation1975;Thread expectedOracleThread1975;')
    tests=tests.replace('cpuCalls++;if(assertFirstCold', 'cpuCalls++;if(expectedOracleThread1975!=null)check(Thread.currentThread()==expectedOracleThread1975,"CPU oracle remains on original exclusive workspace worker");if(overlapExpectation1975!=0)check(overlapExpectation1975>0?GpuNoise1960.inflight>0:GpuNoise1960.inflight==0,"CPU oracle observes required overlap or low-memory serial route");if(assertFirstCold')
    tests=tests.replace('check(GpuNoise1960.submits==0&&GpuQualification1961.preferredCalls==0,"interrupted oracle cannot publish proof or output")', 'check(GpuNoise1960.submits==1&&GpuQualification1961.preferredCalls==0,"interrupted overlapping oracle cannot publish proof or output")')
    tests=tests.replace('unstable or failed second CPU never submits or certifies a second GPU candidate', 'unstable or failed second CPU never commits or certifies a second GPU candidate')
    tests=tests.replace('GpuNoise1960.reservedJava==0,', 'GpuNoise1960.reservedJava==0&&GpuNoise1960.scratch1975==0,')
    tests=tests.replace('    public static void main(String[] args)throws Exception{', EXTRA+'\n    public static void main(String[] args)throws Exception{\n'+
        '        run("failed_preflight_refunds_same_capture_key_and_budget",new Case(){public void run()throws Exception{failedPreflightRefundsSameCaptureProof();}});\n'+
        '        run("pending_and_committed_proofs_share_atomic_limit",new Case(){public void run()throws Exception{pendingAdmissionRefundIsBounded();}});\n'+
        '        run("nearly_complete_same_key_proof_feeds_gpu_follower",new Case(){public void run()throws Exception{nearCompleteProofFeedsSameCaptureFollower();}});\n'+
        '        run("cancelled_proof_reuse_wait_preserves_other_owner",new Case(){public void run()throws Exception{proofReuseWaitInterruptionPreservesOwner();}});\n'+
        '        run("both_trials_overlap_on_owner_and_reuse_inputs_readbacks",new Case(){public void run()throws Exception{overlappingProofReusesBankInputsAndReadbacks();}});\n'+
        '        run("scratch_peak_decline_retains_serial_gpu_proof",new Case(){public void run()throws Exception{scratchDeclineRetainsSequentialGpuProof();}});\n'+
        '        run("oracle_failure_quarantines_pending_native_bank",new Case(){public void run()throws Exception{pendingOracleFailureQuarantinesBank();}});\n'+
        '        run("certified_dispatch_reserves_one_readback",new Case(){public void run()throws Exception{certifiedReadbackReservesOneResultOnly();}});')
    tests_file=fixtures/'StrongLatency1973Test.java';tests_file.write_text(tests)
    java=Path(jdk).resolve()/'bin/java' if jdk else Path(shutil.which('java') or '/usr/lib/jvm/java-17-openjdk-amd64/bin/java').resolve()
    javac=java.parent/'javac';compiler=[str(javac)] if javac.is_file() else [str(java),'-m','jdk.compiler/com.sun.tools.javac.Main']
    for name,cmd in [('compile',compiler+['-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-d',str(classes),str(source),str(fixture_file),str(tests_file)]),('run',[str(java),'-XX:ActiveProcessorCount=4','-cp',str(classes),'com.hiro.ulike.StrongLatency1973Test'])]:
        result=subprocess.run(cmd,capture_output=True,text=True,timeout=60);log=work/(name+'.log');log.write_text(result.stdout+result.stderr)
        if result.returncode:raise RuntimeError(log.read_text())
    match=re.search(r'^RESULT (\{[^\n]+\})$',result.stdout,re.M)
    if not match:raise AssertionError('Executed production Strong result absent')
    report=json.loads(match.group(1));report['production_source_sha256']=hashlib.sha256(source.read_bytes()).hexdigest()
    report.update(strong_preflight1974_regressions_passed=True,strong_preflight1975_regressions_passed=True,strong_overlap1975_regressions_passed=True,strong_upload_reuse1975_regressions_passed=True,strong_buffer_reuse1975_regressions_passed=True,strong_readback_consumption1974_regressions_passed=True,strong_same_capture_recovery1974_regressions_passed=True,strong_latency_controls_regressions_passed=True,gpu_safety_gates_preserved=True,physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',default=str(ROOT/'GpuStrong1960.java'));p.add_argument('--work',default=str(ROOT/'host-work'));p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),indent=2))
