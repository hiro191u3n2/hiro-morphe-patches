#!/usr/bin/env python3
"""Execute Strong 1974 production control flow with deterministic transport.
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
        check(GpuNoise1960.consumptions1974==3,"every completed staged result transfers its live payload out of future debt");clean();
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
    work=Path(work).resolve()/'strong1974'; classes=work/'classes'; fixtures=work/'fixtures'
    if classes.exists():shutil.rmtree(classes)
    classes.mkdir(parents=True,exist_ok=True);fixtures.mkdir(parents=True,exist_ok=True)
    fixture=(ROOT/'latency73testfixtures/FixtureClasses.java').read_text()
    fixture=fixture.replace('static volatile long reservedJava,reservationLimit=MAX_BYTES;', 'static volatile long reservedJava,reservationLimit=MAX_BYTES;static int consumptions1974;')
    fixture=fixture.replace('reservedJava=0;reservationLimit=MAX_BYTES;', 'reservedJava=0;consumptions1974=0;reservationLimit=MAX_BYTES;')
    fixture=fixture.replace('static final class Lease1971 {final long javaBytes;', 'static final class Lease1971 {long javaBytes;')
    fixture=fixture.replace('Lease1971(long j){javaBytes=j;}boolean revalidate1971()', 'Lease1971(long j){javaBytes=j;}boolean consumeJava1974(long bytes){synchronized(GpuNoise1960.class){if(closed||bytes<0||bytes>javaBytes)return false;javaBytes-=bytes;reservedJava-=bytes;consumptions1974++;return true;}}boolean revalidate1971()')
    fixture_file=fixtures/'FixtureClasses.java';fixture_file.write_text(fixture)
    tests=(ROOT/'latency73testfixtures/StrongLatency1973Test.java').read_text()
    tests=tests.replace('GpuNoise1960.opens==0&&GpuNoise1960.modelUploads==0&&GpuNoise1960.reservationCalls==0,\n                "cold CPU reference precedes model transfer and allocation"', 'GpuNoise1960.opens>0&&GpuNoise1960.modelUploads>0&&GpuNoise1960.liveRangeLeases>0,\n                "cold CPU reference follows successful model and range preflight"')
    tests=tests.replace('    public static void main(String[] args)throws Exception{', EXTRA+'\n    public static void main(String[] args)throws Exception{\n'+
        '        run("failed_preflight_refunds_same_capture_key_and_budget",new Case(){public void run()throws Exception{failedPreflightRefundsSameCaptureProof();}});\n'+
        '        run("pending_and_committed_proofs_share_atomic_limit",new Case(){public void run()throws Exception{pendingAdmissionRefundIsBounded();}});\n'+
        '        run("nearly_complete_same_key_proof_feeds_gpu_follower",new Case(){public void run()throws Exception{nearCompleteProofFeedsSameCaptureFollower();}});\n'+
        '        run("cancelled_proof_reuse_wait_preserves_other_owner",new Case(){public void run()throws Exception{proofReuseWaitInterruptionPreservesOwner();}});\n'+
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
    report.update(strong_preflight1974_regressions_passed=True,strong_readback_consumption1974_regressions_passed=True,strong_same_capture_recovery1974_regressions_passed=True,strong_latency_controls_regressions_passed=True,gpu_safety_gates_preserved=True,physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',default=str(ROOT/'GpuStrong1960.java'));p.add_argument('--work',default=str(ROOT/'host-work'));p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),indent=2))
