#!/usr/bin/env python3
"""Production idle tuner/qualification decisions with controlled GPU transport.

This checks ownership, complete comparisons, winner selection and cancellation;
production shaders are validated separately, not benchmarked as Android here.
"""
from pathlib import Path
import hashlib, importlib.util, json, shutil, subprocess

STUBS=r'''package com.hiro.ulike;
import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
final class ProcessingTiming1947 {static volatile long epoch=1;static long captureEpoch1953(){return epoch;}}
final class SaveQueue1935 {static volatile boolean idle;static boolean idle1953(){return idle;}}
final class WholeRoute1953 {static long retainedBytes(){return 0;}}
final class SpeedWorkers1935 {static int maxWorkers(){return 2;}static int availableWorkers1944(){return 2;}static void run(Runnable[] a){for(Runnable r:a)r.run();}}
final class QualityPixels1932 {static class Plan {Local localNoise;}static class Local {int columns=1,rows=1;}}
final class GpuPolicy1960 {
    static final class PolicyData {final int[] masks,u;final float[] grid,f;final int count;PolicyData(int[] m,int[] u,float[] g,float[] f,int c){masks=m;this.u=u;grid=g;this.f=f;count=c;}}
    static final class Protection {QualityPixels1932.Plan plan;PolicyData original=new PolicyData(new int[]{33},new int[]{44},new float[]{55},new float[]{66},64);PolicyData data(int w,int r,int y){return original;}}
    __COPY_METHOD__
}
final class StrongNoise1958 {
    static AtomicInteger cpuRuns=new AtomicInteger();static volatile boolean unstable;
    static class Model {long residentBytes(){return 1024;}int height=8;}
    static float[] gpuEvidence1960(Model m){return new float[16];}static int[][] gpuMaps1960(Model m){return new int[][]{{1},{2},{3}};}
    static boolean gpuOracleSnapshot1961(int[] s,int[] d,int w,int r,int b,int e,int vb,int ve,int oy,int n,boolean shadow,Model model,int[] p,int[] cf){int turn=cpuRuns.incrementAndGet();for(int i=b*w;i<e*w;i++)d[i]=s[i]^(p==null?0:p[0]);if(unstable&&turn%2==0)d[b*w]^=1;if(cf!=null)Arrays.fill(cf,23);return true;}
}
final class GpuNoise1960 {
    static AtomicInteger sessions=new AtomicInteger(),closed=new AtomicInteger(),reads=new AtomicInteger(),uploads=new AtomicInteger(),repeats=new AtomicInteger();
    static volatile CountDownLatch blockStarted,blockRelease;static volatile boolean blocked,policyFailure,failRead;
    static String fingerprint(){return "tuning-host-driver-v1";}static boolean sessionBusy(){return sessions.get()!=0;}
    static boolean workspaceFits(long b){return b<512L*1024*1024;}static boolean supports(int p){return true;}
    static class Lease1971 {boolean revalidate1971(){return true;}void close(){}}
    static class Ticket {Batch batch;Ticket(Batch b){batch=b;}}
    static class Batch {int[] source,policy,u;GpuPolicy1960.PolicyData descriptor;int profile,variant;boolean repeat;Batch(int[] s,int[] p,int[] u,GpuPolicy1960.PolicyData d,int v,int f,boolean repeat){source=s;policy=p;this.u=u;descriptor=d;variant=v;profile=f;this.repeat=repeat;}}
    static class Session {Batch resident;boolean done;Lease1971 reserveCapacity1971(int[] b,long[] sizes,long j){return new Lease1971();}boolean upload(int i,int[] v){uploads.incrementAndGet();return true;}boolean upload(int i,float[] v){uploads.incrementAndGet();return true;}
        Ticket submit(Batch b,int bank){if(b.repeat){repeats.incrementAndGet();b.source=resident.source;b.policy=resident.policy;}else resident=b;return new Ticket(b);}void close(){if(!done){done=true;sessions.decrementAndGet();closed.incrementAndGet();}}
    }
    static Session open(){sessions.incrementAndGet();return new Session();}
}
final class GpuStrong1960 {
    static String profileKey1973(int[] u,int p){return (p==0?"strong-gx1973-ieee-div-policy-bank-v1:":p==1?"strong-gx1964-parallel-policy-bank-v1:":"strong-gx1971-generic-policy-bank-v1:")+"3:"+u[0]+":"+u[1]+":"+u[2]+":"+u[3]+":"+u[6];}
    static int program1973(int p,int v){return p*3+v;}
    static class Result {final int[] pixels,confidence;Result(int[] p,int[] c){pixels=p;confidence=c;}}
    static class Read1971 {final Result result;final String failure;Read1971(Result r,String f){result=r;failure=f;}}
    static GpuNoise1960.Lease1971 reserveRange1971(GpuNoise1960.Session s,int[] src,int[] p,int[] u,GpuPolicy1960.PolicyData d,int[] bank){return new GpuNoise1960.Lease1971();}
    static GpuNoise1960.Batch commands1973(int[] s,int[] p,int[] u,GpuPolicy1960.PolicyData d,int[] bank,int v,int f){return new GpuNoise1960.Batch(s,p,u,d,v,f,false);}
    static GpuNoise1960.Batch commandsRepeat1975(int[] u,GpuPolicy1960.PolicyData d,int[] bank,int v,int f){return new GpuNoise1960.Batch(null,null,u,d,v,f,true);}
    static Read1971 read1971(GpuNoise1960.Session session,GpuNoise1960.Ticket t,int[] u,int[] bank){
        GpuNoise1960.reads.incrementAndGet();GpuNoise1960.Batch b=t.batch;
        if(GpuNoise1960.blocked){GpuNoise1960.blockStarted.countDown();try{GpuNoise1960.blockRelease.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException();}}
        if(GpuNoise1960.failRead)return new Read1971(null,"readback_failed");
        try{Thread.sleep(b.profile==2&&b.variant==2?1:20);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException();}
        if(GpuNoise1960.policyFailure||b.descriptor!=null&&(b.descriptor.masks[0]!=33||b.descriptor.u[0]!=44||b.descriptor.grid[0]!=55||b.descriptor.f[0]!=66))return new Read1971(null,"policy_failure");
        int core=u[0]*(u[3]-u[2]),offset=u[0]*u[2];int[] out=new int[core],cf=u[12]==0?null:new int[((u[0]+3)/4)*((u[3]-u[2]+3)/4)];
        for(int i=0;i<core;i++)out[i]=b.source[offset+i]^(b.policy==null?0:b.policy[0]);if(cf!=null)Arrays.fill(cf,23);
        if(b.profile==0){if(b.variant==1&&cf!=null)cf[0]++;else out[core-1]^=1;}
        return new Read1971(new Result(out,cf),null);
    }
}
'''
HARNESS=r'''package com.hiro.ulike;
import android.content.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;
public final class Tuning1975Test {
    static int assertions;static final Map<String,Integer> tests=new LinkedHashMap<String,Integer>();
    static void check(boolean c,String s){assertions++;if(!c)throw new AssertionError(s);}
    interface Condition {boolean yes()throws Exception;}
    static void waitFor(Condition c,String s)throws Exception {long until=System.nanoTime()+8000000000L;while(!c.yes()){if(System.nanoTime()>until)throw new AssertionError("timeout "+s);Thread.sleep(5);}check(true,s);}
    static Field field(String n)throws Exception {Field f=GpuQualification1961.class.getDeclaredField(n);f.setAccessible(true);return f;}
    static void age()throws Exception {SaveQueue1935.idle=true;field("lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();}
    static void drain()throws Exception {waitFor(()->GpuQualification1961.retainedBytes()==0,"all native/snapshot ownership drained");check(GpuNoise1960.sessions.get()==0,"native session closes before snapshot release");}
    static void clean()throws Exception {SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged();if(GpuNoise1960.blockRelease!=null)GpuNoise1960.blockRelease.countDown();drain();GpuNoise1960.blocked=false;GpuNoise1960.policyFailure=false;GpuNoise1960.failRead=false;StrongNoise1958.unstable=false;}
    static int[] uniforms(int tag){int[] u=new int[32];u[0]=8;u[1]=8;u[2]=0;u[3]=8;u[4]=0;u[5]=8;u[6]=tag;u[7]=8;u[8]=4;u[9]=1;u[10]=3;u[11]=1;u[12]=1;return u;}
    static String group(int[] u){String k=GpuStrong1960.profileKey1973(u,0);return "strong-gx1975-tuning-policy-bank-v1:"+k.substring(k.indexOf(':')+1);}
    static void queue(int[] u){GpuStrongTuning1975.schedule(new int[64],new int[128],u,new StrongNoise1958.Model(),new GpuPolicy1960.Protection());}
    static void section(String n,int a){tests.put(n,assertions-a);}
    public static void main(String[] args)throws Exception {
        Context context=new Context();GpuQualification1961.initialize(context);clean();int n=assertions;
        int[] u=uniforms(1),src=new int[64],pol=new int[128];src[0]=123;pol[0]=77;GpuPolicy1960.Protection protect=new GpuPolicy1960.Protection();
        GpuStrongTuning1975.schedule(src,pol,u,new StrongNoise1958.Model(),protect);long held=GpuQualification1961.retainedBytes();check(held>0&&held<=96L*1024*1024,"bounded foreground snapshot admitted");
        queue(u);check(GpuQualification1961.retainedBytes()==held,"duplicate key doesn't allocate/retain again");
        src[0]=999;pol[0]=222;protect.original.masks[0]=0;protect.original.u[0]=0;protect.original.grid[0]=0;protect.original.f[0]=0;
        age();drain();GpuStrongTuning1975.Choice choice=GpuStrongTuning1975.select(u);
        check(choice!=null&&choice.profile==2&&choice.variant==2,"fastest complete exact profile/workgroup selected");check(GpuNoise1960.repeats.get()==6,"second exact trial reuses GPU input for six exact candidates");
        check(StrongNoise1958.cpuRuns.get()==2,"two independent CPU references retained");check(GpuQualification1961.exactRejected(GpuStrong1960.profileKey1973(u,0)),"all mismatching variants reject profile");
        check(GpuQualification1961.restore(choice.key).gpuNanos>0,"complete timing stored");check(GpuQualification1961.restore(group(u)).variant==8,"aggregate selected choice persisted as scalar");
        SharedPreferences prefs=context.getSharedPreferences("ulike_gx1961_proofs",0);for(Object v:prefs.getAll().values())check(v instanceof String&&((String)v).length()<250,"only bounded scalar signed proof persisted");
        synchronized(field("LOCK").get(null)){((Map<?,?>)field("RECORDS").get(null)).clear();((Map<?,?>)field("FAILURES").get(null)).clear();}
        check(GpuStrongTuning1975.select(u).variant==2,"selected winner restored from persisted scalars");
        section("foreground_snapshot_two_exact_trials_fastest_and_scalar_restore",n);n=assertions;
        SaveQueue1935.idle=false;GpuQualification1961.rejectExact1971(choice.key,"argb_mismatch");check(GpuStrongTuning1975.select(u)==null,"negative child invalidates selection");
        check(GpuQualification1961.restore(group(u))==null,"stale aggregate retired only");check(GpuQualification1961.exactRejected(choice.key),"child exact rejection preserved");
        queue(u);check(GpuQualification1961.retainedBytes()>0,"remaining nonnegative profiles can retune after stale aggregate");clean();
        GpuQualification1961.qualifiedStrongProfile1975(null,"anything",10,20,0);GpuQualification1961.qualifiedStrongProfile1975(group(u),GpuStrong1960.profileKey1973(uniforms(55),1),10,20,0);
        check(GpuQualification1961.restore(GpuStrong1960.profileKey1973(uniforms(55),1))==null,"foreground cannot publish child certificate");section("stale_aggregate_recovery_and_exact_negative_preservation",n);n=assertions;
        int[] cancel=uniforms(2);GpuNoise1960.blocked=true;GpuNoise1960.blockStarted=new CountDownLatch(1);GpuNoise1960.blockRelease=new CountDownLatch(1);queue(cancel);age();check(GpuNoise1960.blockStarted.await(4,TimeUnit.SECONDS),"running GPU trial reached");
        clean();check(GpuQualification1961.restore(group(cancel))==null,"new capture cancels aggregate commit");for(int p=0;p<3;p++)check(GpuQualification1961.restore(GpuStrong1960.profileKey1973(cancel,p))==null,"cancelled child not certified");section("new_capture_cancels_native_trial_before_certificate",n);n=assertions;
        int[] unstable=uniforms(3);StrongNoise1958.unstable=true;int reads=GpuNoise1960.reads.get();queue(unstable);age();drain();check(GpuNoise1960.reads.get()==reads,"unstable CPU full reference prevents every GPU qualification");check(GpuQualification1961.restore(group(unstable))==null,"unstable CPU no aggregate");clean();
        int[] policy=uniforms(4);GpuNoise1960.policyFailure=true;queue(policy);age();drain();for(int p=0;p<3;p++)check(GpuQualification1961.exactRejected(GpuStrong1960.profileKey1973(policy,p)),"policy mismatch retains exact rejection");check(GpuStrongTuning1975.select(policy)==null,"policy failure never selected");clean();
        int[] unavailable=uniforms(5);GpuNoise1960.failRead=true;queue(unavailable);age();drain();check(GpuQualification1961.restore(group(unavailable))==null,"transport failure cannot become certificate");for(int p=0;p<3;p++)check(!GpuQualification1961.exactRejected(GpuStrong1960.profileKey1973(unavailable,p)),"transport failure not misclassified exact");clean();section("unstable_reference_policy_and_transport_failure",n);
        StringBuilder out=new StringBuilder("{\"status\":\"passed\",\"assertions\":").append(assertions).append(",\"tests\":{");boolean first=true;for(Map.Entry<String,Integer> e:tests.entrySet()){if(!first)out.append(',');first=false;out.append('"').append(e.getKey()).append("\":").append(e.getValue());}System.out.println(out.append("}}"));
    }
}
'''
def test(source,work,jdk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    spec=importlib.util.spec_from_file_location('queue1975',source/'host_qualification1967.py');q=importlib.util.module_from_spec(spec);spec.loader.exec_module(q)
    fixtures=work/'fixtures';classes=work/'classes';classes.mkdir(exist_ok=True)
    for name,body in q.FIXTURES.items():
        if name.startswith('android/'):
            p=fixtures/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body)
    policy=(source/'GpuPolicy1960.java').read_text();start=policy.index('    static PolicyData detached1975(');end=policy.index('\n    }',start)+6
    stub=fixtures/'com/hiro/ulike/TuningFixtures1975.java';stub.parent.mkdir(parents=True,exist_ok=True);stub.write_text(STUBS.replace('__COPY_METHOD__',policy[start:end]))
    harness=stub.with_name('Tuning1975Test.java');harness.write_text(HARNESS)
    javac=str(Path(jdk)/'bin/javac') if jdk else shutil.which('javac');java=str(Path(jdk)/'bin/java') if jdk else shutil.which('java')
    if not javac or not java:raise RuntimeError('JDK required')
    production=[source/'GpuQualification1961.java',source/'GpuStrongTuning1975.java']
    commands=[('compile',[javac,'--release','8','-encoding','UTF-8','-d',str(classes),*map(str,production),*map(str,fixtures.rglob('*.java'))]),('run',[java,'-ea','-cp',str(classes),'com.hiro.ulike.Tuning1975Test'])]
    for name,command in commands:
        r=subprocess.run(command,text=True,capture_output=True,timeout=90);(work/(name+'.log')).write_text(r.stdout+r.stderr)
        if r.returncode:raise RuntimeError(name+'\n'+r.stdout+r.stderr)
    result=json.loads(r.stdout.strip().splitlines()[-1]);result.update(physical_android_tested=False,fixture_classes_in_runtime=False,tuning_full_exact_gate_verified=True,tuning_snapshot_ownership_and_cancel_verified=True,scalar_only_tuning_persistence_verified=True,production_source_sha256={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in production})
    (work/'tuning-host-result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),indent=2))
