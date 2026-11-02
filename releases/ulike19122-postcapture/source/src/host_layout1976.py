#!/usr/bin/env python3
"""Production geometry chooser and real idle queue with controlled GPU transport.
Transport timing is deterministic test evidence, never an Android speed claim.
"""
from pathlib import Path
import hashlib, importlib.util, json, shutil, subprocess

STUBS=r'''package com.hiro.ulike;
import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
final class ProcessingTiming1947 {static volatile long epoch=1;static long captureEpoch1953(){return epoch;}}
final class SaveQueue1935 {static volatile boolean idle;static boolean idle1953(){return idle;}}
final class WholeRoute1953 {static long retainedBytes(){return 0;}}
final class SpeedWorkers1935 {static int maxWorkers(){return 4;}static int availableWorkers1944(){return 4;}static void run(Runnable[] a){for(Runnable r:a)r.run();}}
final class QualityPixels1932 {static class Plan {Local localNoise;}static class Local {int columns=1,rows=1;}}
final class GpuPolicy1960 {
    static final class PolicyData {final int[] masks,u;final float[] grid,f;final int count;PolicyData(int[] m,int[] u,float[] g,float[] f,int c){masks=m;this.u=u;grid=g;this.f=f;count=c;}}
    static final class Protection {QualityPixels1932.Plan plan;PolicyData original;Protection(int w,int rows,int y){int[] m=new int[w*rows*2],u=new int[32];u[0]=4;u[1]=w;u[2]=rows;u[3]=y;for(int i=0;i<w*rows;i++){m[i*2]=(i/w+y);m[i*2+1]=i%w;}original=new PolicyData(m,u,new float[]{55},new float[]{66},w*rows);}PolicyData data(int w,int r,int y){return original;}}
    __COPY_METHOD__
    __SLICE_METHOD__
}
final class StrongNoise1958 {
    static final int HALO=18;static AtomicInteger cpuRuns=new AtomicInteger();static volatile boolean unstable,slowSplit;
    static class Model {final int width,height;Model(int w,int h){width=w;height=h;}long residentBytes(){return 1024;}}
    static float[] gpuEvidence1960(Model m){return new float[16];}static int[][] gpuMaps1960(Model m){return new int[][]{{1},{2},{3}};}
    static long workspaceBytes(int w,int rows){return 1048576+64L*w*rows;}
    static void pause(int ms){try{Thread.sleep(ms);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException();}}
    static boolean gpuOracleSnapshot1961(int[] s,int[] d,int w,int r,int b,int e,int vb,int ve,int oy,int n,boolean shadow,Model model,int[] p,int[] cf){
        int turn=cpuRuns.incrementAndGet();pause(e-b==256?8:slowSplit?10:1);
        for(int y=b;y<e;y++)for(int x=0;x<w;x++){int i=y*w+x;d[i]=s[i]^(p==null?0:p[((y-b)*w+x)*2]);}
        if(unstable&&turn%3==0)d[b*w]^=1;
        if(cf!=null)for(int y=0;y<(e-b+3)/4;y++)for(int x=0;x<(w+3)/4;x++)cf[y*((w+3)/4)+x]=(oy+b+y*4)*31+x;
        return true;
    }
}
final class GpuNoise1960 {
    static AtomicInteger sessions=new AtomicInteger(),closed=new AtomicInteger(),reads=new AtomicInteger(),submits=new AtomicInteger(),leases=new AtomicInteger();
    static volatile CountDownLatch blockStarted,blockRelease;static volatile boolean blocked,failRead,slowSplit,rejectMemory,mismatchPixels,mismatchConfidence;static volatile int rejectReserve=-1,reserveCalls;
    static final List<Integer> submitted=new ArrayList<Integer>();static final List<String> sequence=new ArrayList<String>();
    static String fingerprint(){return "layout-host-driver-v1";}static boolean sessionBusy(){return sessions.get()!=0;}
    static boolean workspaceFits(long b){return !rejectMemory&&b<512L*1024*1024;}static boolean supports(int p){return true;}
    static class Lease1971 {boolean done;Lease1971(){leases.incrementAndGet();}boolean revalidate1971(){return true;}void close(){if(!done){done=true;leases.decrementAndGet();}}}
    static class Ticket {Batch batch;final int bank;Ticket(Batch b,int k){batch=b;bank=k;}}
    static class Batch {int[] source,policy,u;GpuPolicy1960.PolicyData descriptor;int profile,variant;Batch(int[] s,int[] p,int[] u,GpuPolicy1960.PolicyData d,int v,int f){source=s;policy=p;this.u=u;descriptor=d;variant=v;profile=f;}}
    static class Session {boolean done;Lease1971 reserveCapacity1971(int[] b,long[] sizes,long j){return new Lease1971();}boolean upload(int i,int[] v){return true;}boolean upload(int i,float[] v){return true;}
        Ticket submit(Batch b,int bank){submits.incrementAndGet();submitted.add(b.u[3]-b.u[2]);sequence.add("submit"+bank);return new Ticket(b,bank);}void close(){if(!done){done=true;sessions.decrementAndGet();closed.incrementAndGet();sequence.add("close");}}
    }
    static Session open(){sessions.incrementAndGet();return new Session();}
}
final class GpuStrongTuning1975 {static class Choice {final int profile=1,variant=0;}static Choice select(int[] u){return null;}}
final class GpuStrong1960 {
    static String profileKey1973(int[] u,int p){return (p==0?"strong-gx1973-ieee-div-policy-bank-v1:":p==1?"strong-gx1964-parallel-policy-bank-v1:":"strong-gx1971-generic-policy-bank-v1:")+"3:"+u[0]+":"+u[7]+":"+u[1]+":"+u[2]+":"+u[3]+":"+u[6];}
    static int program1973(int p,int v){return p*3+v;}
    static class Result {final int[] pixels,confidence;Result(int[] p,int[] c){pixels=p;confidence=c;}}
    static class Read1971 {final Result result;final String failure;Read1971(Result r,String f){result=r;failure=f;}}
    static GpuNoise1960.Lease1971 reserveRange1971(GpuNoise1960.Session s,int[] src,int[] p,int[] u,GpuPolicy1960.PolicyData d,int[] bank){return ++GpuNoise1960.reserveCalls==GpuNoise1960.rejectReserve?null:new GpuNoise1960.Lease1971();}
    static GpuNoise1960.Batch commands1973(int[] s,int[] p,int[] u,GpuPolicy1960.PolicyData d,int[] bank,int v,int f){return new GpuNoise1960.Batch(s,p,u,d,v,f);}
    static Read1971 read1971(GpuNoise1960.Session session,GpuNoise1960.Ticket t,int[] u,int[] bank){
        GpuNoise1960.reads.incrementAndGet();GpuNoise1960.sequence.add("read"+t.bank);GpuNoise1960.Batch b=t.batch;
        if(GpuNoise1960.blocked){GpuNoise1960.blockStarted.countDown();try{GpuNoise1960.blockRelease.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException();}}
        if(GpuNoise1960.failRead)return new Read1971(null,"readback_failed");
        int rows=u[3]-u[2];StrongNoise1958.pause(rows==256?20:GpuNoise1960.slowSplit?18:1);
        int core=u[0]*rows,offset=u[0]*u[2];int[] out=new int[core],cf=u[12]==0?null:new int[((u[0]+3)/4)*((rows+3)/4)];
        for(int i=0;i<core;i++){
            int y=i/u[0]+u[2]+u[6],x=i%u[0];
            if(b.source[offset+i]!=(y*65536+x))throw new AssertionError("source crop / absolute coordinate");
            if(b.policy!=null&&b.policy[i*2]!=y)throw new AssertionError("policy crop / absolute coordinate");
            if(b.descriptor!=null&&(b.descriptor.masks[i*2]!=y||b.descriptor.masks[i*2+1]!=x||b.descriptor.u[2]!=rows||b.descriptor.u[3]!=u[6]+u[2]||b.descriptor.grid[0]!=55||b.descriptor.f[0]!=66))throw new AssertionError("policy descriptor crop / ownership");
            out[i]=b.source[offset+i]^(b.policy==null?0:b.policy[i*2]);
        }
        if(cf!=null)for(int y=0;y<(rows+3)/4;y++)for(int x=0;x<(u[0]+3)/4;x++)cf[y*((u[0]+3)/4)+x]=(u[6]+u[2]+y*4)*31+x;
        if(rows==128&&GpuNoise1960.mismatchPixels)out[core-1]^=1;
        if(rows==128&&cf!=null&&GpuNoise1960.mismatchConfidence)cf[cf.length-1]^=1;
        return new Read1971(new Result(out,cf),null);
    }
}
'''
HARNESS=r'''package com.hiro.ulike;
import android.content.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;
public final class Layout1976Test {
    static int assertions;static final Map<String,Integer> tests=new LinkedHashMap<String,Integer>();
    static void check(boolean c,String s){assertions++;if(!c)throw new AssertionError(s);}
    interface Condition {boolean yes()throws Exception;}
    static void waitFor(Condition c,String s)throws Exception {long until=System.nanoTime()+8000000000L;while(!c.yes()){if(System.nanoTime()>until)throw new AssertionError("timeout "+s);Thread.sleep(5);}check(true,s);}
    static Field field(String n)throws Exception {Field f=GpuQualification1961.class.getDeclaredField(n);f.setAccessible(true);return f;}
    static Object get(Object o,String n)throws Exception {Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    static void age()throws Exception {SaveQueue1935.idle=true;field("lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();}
    static void drain()throws Exception {waitFor(()->GpuQualification1961.retainedBytes()==0,"all native/snapshot ownership drained");check(GpuNoise1960.sessions.get()==0,"native session closes before snapshot release");check(GpuNoise1960.leases.get()==0,"leases released");}
    static void clean()throws Exception {SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged();if(GpuNoise1960.blockRelease!=null)GpuNoise1960.blockRelease.countDown();drain();GpuNoise1960.blocked=false;GpuNoise1960.failRead=false;GpuNoise1960.slowSplit=false;GpuNoise1960.rejectMemory=false;GpuNoise1960.mismatchPixels=false;GpuNoise1960.mismatchConfidence=false;GpuNoise1960.rejectReserve=-1;GpuNoise1960.reserveCalls=0;StrongNoise1958.unstable=false;StrongNoise1958.slowSplit=false;GpuNoise1960.submitted.clear();GpuNoise1960.sequence.clear();}
    static int[] uniforms(int width,int edge){int[] u=new int[32];u[0]=width;u[1]=edge==2?256:274;u[2]=0;u[3]=u[2]+256;u[4]=0;u[5]=u[1];u[6]=0;u[7]=edge==2?256:1024;u[8]=4;u[9]=1;u[10]=3;u[11]=1;u[12]=1;return u;}
    static String key(int[] u){return "strong-layout1976:3:"+u[0]+":"+u[7]+":"+u[8]+":"+u[9]+":"+u[12]+":0:0:1:0:1000:2000";}
    static void certify(int[] u){GpuQualification1961.qualifiedStrongPreferred1970(GpuStrong1960.profileKey1973(u,1),1000,2000,0);}
    static void queue(int[] u){int[] s=new int[u[0]*u[1]],p=new int[u[0]*256*2];for(int i=0;i<s.length;i++)s[i]=(i/u[0]+u[6])*65536+i%u[0];for(int i=0;i<p.length/2;i++){p[i*2]=i/u[0]+u[6]+u[2];p[i*2+1]=i%u[0];}GpuStrongLayout1976.schedule(s,p,u,new StrongNoise1958.Model(u[0],u[7]),new GpuPolicy1960.Protection(u[0],256,u[6]+u[2]));}
    static int selection(int[] u,int baseline){return GpuStrongLayout1976.selectRows(u[0],u[7],u[8],u[9]!=0,u[12]!=0,0,0,baseline);}
    static void section(String n,int a){tests.put(n,assertions-a);}
    public static void main(String[] args)throws Exception {
        Context context=new Context();GpuQualification1961.initialize(context);clean();int n=assertions;
        int[] u=uniforms(8,0);queue(u);check(GpuQualification1961.retainedBytes()==0,"uncertified original shape cannot benchmark");check(selection(u,256)==256,"cold baseline retained");
        for(int edge=0;edge<3;edge++){
            u=uniforms(8+edge,edge);certify(u);queue(u);long held=GpuQualification1961.retainedBytes();check(held>0&&held<=96L*1024*1024,"bounded snapshot queued");queue(u);check(GpuQualification1961.retainedBytes()==held,"one frozen block per geometry key");
            Object job=((Deque<?>)field("QUEUED").get(null)).peekFirst(),probe=get(job,"probe");int[] frozen=(int[])get(probe,"u");int original=frozen[0];u[0]=999;check(frozen[0]==original,"uniforms detached");u[0]=original;
            age();drain();check(selection(u,256)==128,"same-image faster exact128 selected");check(selection(u,64)==64,"never overrides smaller memory geometry");check(selection(u,128)==128,"doesnotraise128limit");
            check(GpuNoise1960.submitted.equals(Arrays.asList(256,128,128,128,128,256)),"balancedtwo-trialorder");
            check(String.join(",",GpuNoise1960.sequence).contains("submit0,submit1,read0,read1"),"second producer submitted before first collection");
            GpuQualification1961.Record r=GpuQualification1961.restore(key(u));check(r!=null&&r.variant==1&&r.gpuNanos<=r.cpuNanos-r.cpuNanos/20,"real walltime includes5percentmargin");
            // Arithmetic certificates for the new 128 shape are deliberately NOT
            // manufactured by this geometry recommendation.
            int[] split=u.clone();split[3]=split[2]+128;check(GpuQualification1961.restore(GpuStrong1960.profileKey1973(split,1))==null,"new128shape retainsindependentexactproof");clean();
            // A different workgroup/reproof token must retire the geometry hint
            // without erasing the original arithmetic certificate.
            GpuQualification1961.qualifiedStrongPreferred1970(GpuStrong1960.profileKey1973(u,1),1001,2001,1);
            check(selection(u,256)==256,"kernel workgroup / certificate change retires stale geometry");
        }
        section("same_image_top_internal_boundary_bottom_exact_geometry_pipeline_and_bounded_memory",n);n=assertions;
        u=uniforms(16,1);certify(u);GpuNoise1960.slowSplit=true;queue(u);age();drain();check(selection(u,256)==256,"GPU slower split rejected");check(GpuQualification1961.restore(key(u))==null,"slow timings not fabricated as proof");clean();
        u=uniforms(17,1);certify(u);StrongNoise1958.slowSplit=true;queue(u);age();drain();check(selection(u,256)==256,"CPU fallback regression rejected despite GPU win");clean();section("gpu_and_cpu_regression_guard",n);n=assertions;
        for(int failure=0;failure<4;failure++){
            u=uniforms(20+failure,1);certify(u);GpuNoise1960.mismatchPixels=failure==0;GpuNoise1960.mismatchConfidence=failure==1;GpuNoise1960.failRead=failure==2;StrongNoise1958.unstable=failure==3;queue(u);age();drain();check(selection(u,256)==256,"fullARGBconfidenceCPUinstabilitytransport fail closed "+failure);check(GpuQualification1961.restore(GpuStrong1960.profileKey1973(u,1))!=null,"existing256proofpreserved");clean();
        }
        section("late_pixel_confidence_reference_and_transport_mismatch",n);n=assertions;
        u=uniforms(30,1);certify(u);GpuNoise1960.rejectMemory=true;queue(u);check(GpuQualification1961.retainedBytes()==0,"lowmemorysnapshotdeclined");clean();
        u=uniforms(31,1);certify(u);GpuNoise1960.rejectReserve=3;queue(u);age();drain();check(selection(u,256)==256,"secondbankleasefailure closespendingfirstbank");clean();
        u=uniforms(32,1);certify(u);GpuNoise1960.blocked=true;GpuNoise1960.blockStarted=new CountDownLatch(1);GpuNoise1960.blockRelease=new CountDownLatch(1);queue(u);age();check(GpuNoise1960.blockStarted.await(4,TimeUnit.SECONDS),"pendingGPUtrial reached");clean();check(selection(u,256)==256,"newcapture cancels beforegeometrycommit");
        u=uniforms(33,1);certify(u);queue(u);check(GpuQualification1961.retainedBytes()>0,"queuedcancelsetup");clean();check(selection(u,256)==256,"queuedcapturecancel hasnochoice");section("admission_second_bank_failure_and_queued_running_cancel",n);n=assertions;
        // Exercise real policy slice validation and independent descriptors.
        GpuPolicy1960.PolicyData data=new GpuPolicy1960.Protection(7,256,256).original;
        GpuPolicy1960.PolicyData p=GpuPolicy1960.slice1976(data,128,128);check(p.count==7*128&&p.u[2]==128&&p.u[3]==384,"descriptor coordinates");
        data.masks[128*7*2]=-1;data.grid[0]=-1;data.f[0]=-1;data.u[2]=-1;check(p.masks[0]==384&&p.grid[0]==55&&p.f[0]==66&&p.u[2]==128,"descriptorarraysindependent");
        for(int[] bad:new int[][]{{-1,1},{0,0},{255,2},{0,Integer.MAX_VALUE}}){boolean threw=false;try{GpuPolicy1960.slice1976(p,bad[0],bad[1]);}catch(IllegalArgumentException e){threw=true;}check(threw,"invalidslice rejects before allocation");}
        boolean nullRejected=false;try{GpuPolicy1960.slice1976(null,0,128);}catch(IllegalArgumentException e){nullRejected=true;}check(nullRejected,"missing descriptor rejects safely");section("policy_slice_ownership_and_bounds",n);
        StringBuilder out=new StringBuilder("{\"status\":\"passed\",\"assertions\":").append(assertions).append(",\"tests\":{");boolean first=true;for(Map.Entry<String,Integer> e:tests.entrySet()){if(!first)out.append(',');first=false;out.append('"').append(e.getKey()).append("\":").append(e.getValue());}System.out.println(out.append("}}"));
    }
}
'''

def method(body,name):
    start=body.index('    static PolicyData '+name+'(');end=body.index('\n    }',start)+6
    return body[start:end]

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    spec=importlib.util.spec_from_file_location('queue1976',source/'host_qualification1967.py');q=importlib.util.module_from_spec(spec);spec.loader.exec_module(q)
    fixtures=work/'fixtures';classes=work/'classes';classes.mkdir(exist_ok=True)
    for name,body in q.FIXTURES.items():
        if name.startswith('android/'):
            p=fixtures/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body)
    policy=(source/'GpuPolicy1960.java').read_text()
    stub=fixtures/'com/hiro/ulike/LayoutFixtures1976.java';stub.parent.mkdir(parents=True,exist_ok=True)
    stub.write_text(STUBS.replace('__COPY_METHOD__',method(policy,'detached1975')).replace('__SLICE_METHOD__',method(policy,'slice1976')))
    harness=stub.with_name('Layout1976Test.java');harness.write_text(HARNESS)
    javac=str(Path(jdk)/'bin/javac') if jdk else shutil.which('javac');java=str(Path(jdk)/'bin/java') if jdk else shutil.which('java')
    if not javac or not java:raise RuntimeError('JDK required')
    production=[source/'GpuQualification1961.java',source/'GpuStrongLayout1976.java']
    for name,command in [('compile',[javac,'--release','8','-encoding','UTF-8','-d',str(classes),*map(str,production),*map(str,fixtures.rglob('*.java'))]),('run',[java,'-ea','-cp',str(classes),'com.hiro.ulike.Layout1976Test'])]:
        r=subprocess.run(command,text=True,capture_output=True,timeout=90);(work/(name+'.log')).write_text(r.stdout+r.stderr)
        if r.returncode:raise RuntimeError(name+'\n'+r.stdout+r.stderr)
    result=json.loads(r.stdout.strip().splitlines()[-1]);result.update(physical_android_tested=False,fixture_classes_in_runtime=False,geometry_two_exact_trials_verified=True,geometry_queue_ownership_and_cancel_verified=True,independent_shape_certificates_required=True,actual_android_speedup_claimed=False,production_source_sha256={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in production})
    (work/'layout-host-result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),indent=2))
