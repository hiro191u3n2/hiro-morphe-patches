#!/usr/bin/env python3
"""Twelve-candidate idle tuning: real production chooser/qualification service,
controlled GPU timing, complete old/new profile comparisons and legacy restore.
The original1975 fixture is retained as regression input without modifying it.
"""
from pathlib import Path
import hashlib, importlib.util, json, shutil, subprocess

def fixture(source):
    spec=importlib.util.spec_from_file_location('tuning75_reference',source/'host_tuning1975.py');old=importlib.util.module_from_spec(spec);spec.loader.exec_module(old)
    stubs=old.STUBS.replace('static volatile CountDownLatch blockStarted', 'static volatile int newMode,blockProfile=-1;static volatile boolean oldMismatch,oldUnavailable,rejectMemory,failLease;static AtomicIntegerArray readsByProfile=new AtomicIntegerArray(4);static volatile CountDownLatch blockStarted')
    stubs=stubs.replace('return b<512L*1024*1024;', 'return !rejectMemory&&b<512L*1024*1024;')
    stubs=stubs.replace('p==0?"strong-gx1973', 'p==3?"strong-gx1976-ieee-tile8-policy-bank-v1:":p==0?"strong-gx1973')
    stubs=stubs.replace('return new GpuNoise1960.Lease1971();}', 'return GpuNoise1960.failLease?null:new GpuNoise1960.Lease1971();}')
    stubs=stubs.replace('Ticket submit(Batch b,int bank){if(b.repeat)', '''Ticket submit(Batch b,int bank){if(b.profile==3&&newMode==6&&!b.repeat)try{Thread.sleep(25);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException();}if(b.repeat)''')
    stubs=stubs.replace('GpuNoise1960.Batch b=t.batch;', 'GpuNoise1960.Batch b=t.batch;GpuNoise1960.readsByProfile.incrementAndGet(b.profile);')
    stubs=stubs.replace('if(GpuNoise1960.blocked){', 'if(GpuNoise1960.blocked&&(GpuNoise1960.blockProfile<0||GpuNoise1960.blockProfile==b.profile)){')
    stubs=stubs.replace('if(GpuNoise1960.failRead)', 'if(GpuNoise1960.failRead||GpuNoise1960.oldUnavailable&&b.profile<3)')
    stubs=stubs.replace('Thread.sleep(b.profile==2&&b.variant==2?1:20);', 'Thread.sleep(b.profile==3?(GpuNoise1960.newMode==0?25:b.variant==2?1:20):b.profile==2&&b.variant==2?8:20);')
    stubs=stubs.replace('if(GpuNoise1960.policyFailure||b.descriptor', 'if(GpuNoise1960.policyFailure||b.profile==3&&GpuNoise1960.newMode==4||b.descriptor')
    stubs=stubs.replace('if(b.profile==0){', 'if(GpuNoise1960.oldMismatch&&b.profile<3)out[0]^=1;if(b.profile==3){if(GpuNoise1960.newMode==2||GpuNoise1960.newMode==5&&b.repeat)out[core-1]^=1;if(GpuNoise1960.newMode==3&&cf!=null)cf[cf.length-1]^=1;}if(b.profile==0){')
    harness=old.HARNESS.replace('Tuning1975Test','Tuning1976Test').replace('"strong-gx1975-tuning-policy-bank-v1:"','"strong-gx1976-tuning-policy-bank-v1:"')
    harness=harness.replace('StrongNoise1958.unstable=false;}', 'StrongNoise1958.unstable=false;GpuNoise1960.newMode=0;GpuNoise1960.blockProfile=-1;GpuNoise1960.oldMismatch=false;GpuNoise1960.oldUnavailable=false;GpuNoise1960.rejectMemory=false;GpuNoise1960.failLease=false;}')
    harness=harness.replace('GpuNoise1960.repeats.get()==6', 'GpuNoise1960.repeats.get()==9').replace('second exact trial reuses GPU input for six exact candidates','second exact trial reuses GPU input for nine exact candidates')
    helpers=r'''    static void signedPersistedChoice(Context context,String key,int variant)throws Exception {
        Method env=GpuQualification1961.class.getDeclaredMethod("environment");env.setAccessible(true);
        Method digest=GpuQualification1961.class.getDeclaredMethod("digest",String.class);digest.setAccessible(true);
        Method recordKey=GpuQualification1961.class.getDeclaredMethod("recordKey",String.class,String.class);recordKey.setAccessible(true);
        Field schema=GpuQualification1961.class.getDeclaredField("STRONG_PREFERRED_SCHEMA");schema.setAccessible(true);
        String environment=(String)env.invoke(null),core=(String)schema.get(null)+":100:200:"+variant+":"+System.currentTimeMillis();
        context.getSharedPreferences("ulike_gx1961_proofs",0).edit().putString((String)recordKey.invoke(null,key,environment),core+":"+digest.invoke(null,environment+"|"+key+"|"+core)).apply();
        synchronized(field("LOCK").get(null)){((Map<?,?>)field("RECORDS").get(null)).clear();}
    }
'''
    harness=harness.replace('    public static void main(String[] args)throws Exception {',helpers+'    public static void main(String[] args)throws Exception {')
    mark='        StringBuilder out=new StringBuilder('

    more=r'''
        n=assertions;
        // The old route remains selected when the new exact profile is slower.
        int[] fast=uniforms(101);GpuNoise1960.newMode=1;queue(fast);age();drain();
        GpuStrongTuning1975.Choice newest=GpuStrongTuning1975.select(fast);
        check(newest!=null&&newest.profile==3&&newest.variant==2,"new exact tile8+division profile wins only when faster");
        check(GpuQualification1961.restore(group(fast)).variant==11,"12th candidate encoded as11");
        check(GpuQualification1961.restore(GpuStrong1960.profileKey1973(fast,3))!=null,"new winner has independent full pixel child proof");
        for(int p=1;p<=3;p++)check(GpuQualification1961.restore(GpuStrong1960.profileKey1973(fast,p))!=null,"passing profile independently certified "+p);
        synchronized(field("LOCK").get(null)){((Map<?,?>)field("RECORDS").get(null)).clear();((Map<?,?>)field("FAILURES").get(null)).clear();}
        newest=GpuStrongTuning1975.select(fast);check(newest!=null&&newest.profile==3&&newest.variant==2,"new group choice11 restores from scalar proof");clean();
        int[] slow=uniforms(102);queue(slow);age();drain();newest=GpuStrongTuning1975.select(slow);
        check(newest!=null&&newest.profile==2&&newest.variant==2,"slower new exact profile retains faster old winner");
        check(GpuQualification1961.restore(GpuStrong1960.profileKey1973(slow,3))!=null,"slower exact candidate may have proof but isnotchosen");clean();
        int[] transfer=uniforms(103);GpuNoise1960.newMode=6;queue(transfer);age();drain();newest=GpuStrongTuning1975.select(transfer);
        check(newest!=null&&newest.profile==2,"fast dispatch with slower upload cannot beat inclusive baseline");clean();
        section("new_fourth_profile_faster_slower_and_transfer_inclusive_selection",n);n=assertions;
        for(int mode=2;mode<=5;mode++){
            int[] bad=uniforms(110+mode);GpuNoise1960.newMode=mode;queue(bad);age();drain();newest=GpuStrongTuning1975.select(bad);
            check(newest!=null&&newest.profile==2,"new pixel/confidence/policy/secondtrial mismatch retains old winner "+mode);
            check(GpuQualification1961.exactRejected(GpuStrong1960.profileKey1973(bad,3)),"new profile complete exact mismatch rejected "+mode);
            check(GpuQualification1961.restore(GpuStrong1960.profileKey1973(bad,3))==null,"no new child certificate from partial proof "+mode);clean();
        }
        // A hypothetical faster new shader cannot establish the speed baseline
        // itself. At least one old candidate must complete both exact trials.
        int[] noBaseline=uniforms(120);GpuNoise1960.oldUnavailable=true;GpuNoise1960.newMode=1;int before=GpuNoise1960.readsByProfile.get(3);
        queue(noBaseline);age();drain();check(GpuNoise1960.readsByProfile.get(3)==before,"no old completed benchmark prevents new candidate execution");
        check(GpuStrongTuning1975.select(noBaseline)==null,"no old benchmark cannot promote new profile");clean();
        int[] noExact=uniforms(121);GpuNoise1960.oldMismatch=true;GpuNoise1960.newMode=1;before=GpuNoise1960.readsByProfile.get(3);
        queue(noExact);age();drain();check(GpuNoise1960.readsByProfile.get(3)==before,"all old exact mismatches prevent ungrounded new promotion");check(GpuStrongTuning1975.select(noExact)==null,"no old exact reference stays unselected");clean();
        section("new_profile_all_output_gates_and_required_old_speed_baseline",n);n=assertions;
        int[] legacy=uniforms(130);String legacyGroup=group(legacy).replace("strong-gx1976-","strong-gx1975-");
        GpuQualification1961.qualifiedStrongPreferred1970(GpuStrong1960.profileKey1973(legacy,2),100,200,2);
        GpuQualification1961.qualifiedStrongPreferred1970(legacyGroup,100,200,8);
        newest=GpuStrongTuning1975.select(legacy);check(newest!=null&&newest.profile==2&&newest.variant==2,"old aggregate fallback retains v75 proof");
        check(GpuQualification1961.restore(group(legacy))==null,"legacy restore doesnotforge v76 aggregate");
        int[] bound=uniforms(131);String older=group(bound).replace("strong-gx1976-","strong-gx1975-");
        for(int invalid:new int[]{-1,9,10,11,12}){
            GpuQualification1961.qualifiedStrongPreferred1970(older,100,200,invalid);
            check(GpuQualification1961.restore(older)==null,"legacy range rejects "+invalid);
        }
        for(int invalid:new int[]{-1,12,100}){
            GpuQualification1961.qualifiedStrongPreferred1970(group(bound),100,200,invalid);
            check(GpuQualification1961.restore(group(bound))==null,"new range rejects "+invalid);
        }
        GpuQualification1961.qualifiedStrongPreferred1970(group(bound),100,200,11);
        check(GpuQualification1961.restore(group(bound)).variant==11,"new scalar upperbound11 accepted");
        check(GpuStrongTuning1975.select(bound)==null,"new aggregate without matching profile3 child notusable");
        check(GpuQualification1961.restore(group(bound))==null,"unusable new aggregate retired");
        GpuQualification1961.qualifiedStrongPreferred1970(GpuStrong1960.profileKey1973(bound,3),100,200,3);
        check(GpuQualification1961.restore(GpuStrong1960.profileKey1973(bound,3))==null,"profile workgroup bound remains0through2");
        synchronized(field("LOCK").get(null)){((Map<?,?>)field("RECORDS").get(null)).clear();((Map<?,?>)field("FAILURES").get(null)).clear();}
        newest=GpuStrongTuning1975.select(legacy);check(newest!=null&&newest.profile==2,"legacy aggregate survives process-cache reset");
        signedPersistedChoice(context,older,9);check(GpuQualification1961.restore(older)==null,"legacy persisted signed choice9 rejected on restore");
        signedPersistedChoice(context,group(bound),12);check(GpuQualification1961.restore(group(bound))==null,"new persisted signed choice12 rejected on restore");
        signedPersistedChoice(context,group(bound),11);check(GpuQualification1961.restore(group(bound)).variant==11,"new persisted signed choice11 accepted on restore");
        section("legacy_fallback_and_separate_old_new_scalar_bounds",n);n=assertions;
        int[] lowMemory=uniforms(140);GpuNoise1960.rejectMemory=true;queue(lowMemory);check(GpuQualification1961.retainedBytes()==0,"snapshot lowmemory rejected beforeclone");clean();
        int[] noLease=uniforms(141);GpuNoise1960.failLease=true;before=GpuNoise1960.reads.get();queue(noLease);age();drain();check(GpuNoise1960.reads.get()==before,"failedcapacitylease neverdispatches");check(GpuStrongTuning1975.select(noLease)==null,"lowcapacity no certificate");clean();
        section("additional_snapshot_and_native_capacity_gates",n);n=assertions;
        int[] cancelNew=uniforms(150);GpuNoise1960.newMode=1;GpuNoise1960.blockProfile=3;GpuNoise1960.blocked=true;GpuNoise1960.blockStarted=new CountDownLatch(1);GpuNoise1960.blockRelease=new CountDownLatch(1);
        queue(cancelNew);age();check(GpuNoise1960.blockStarted.await(4,TimeUnit.SECONDS),"newprofile native trial reached");clean();
        check(GpuQualification1961.restore(group(cancelNew))==null,"capture during newprofile prevents aggregate commit");check(GpuQualification1961.restore(GpuStrong1960.profileKey1973(cancelNew,3))==null,"cancelled newprofile child nevercertified");
        section("new_profile_pending_native_cancellation",n);
'''
    if mark not in harness:raise RuntimeError('reference harness marker changed')
    harness=harness.replace(mark,more+'\n'+mark)
    return stubs,harness

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    spec=importlib.util.spec_from_file_location('queue1976',source/'host_qualification1967.py');q=importlib.util.module_from_spec(spec);spec.loader.exec_module(q)
    fixtures=work/'fixtures';classes=work/'classes';classes.mkdir(exist_ok=True)
    for name,body in q.FIXTURES.items():
        if name.startswith('android/'):
            p=fixtures/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body)
    policy=(source/'GpuPolicy1960.java').read_text();start=policy.index('    static PolicyData detached1975(');end=policy.index('\n    }',start)+6
    stubs,harness=fixture(source)
    stub=fixtures/'com/hiro/ulike/TuningFixtures1976.java';stub.parent.mkdir(parents=True,exist_ok=True);stub.write_text(stubs.replace('__COPY_METHOD__',policy[start:end]))
    stub.with_name('Tuning1976Test.java').write_text(harness)
    javac=str(Path(jdk)/'bin/javac') if jdk else shutil.which('javac');java=str(Path(jdk)/'bin/java') if jdk else shutil.which('java')
    if not javac or not java:raise RuntimeError('JDK required')
    production=[source/'GpuQualification1961.java',source/'GpuStrongTuning1975.java']
    for name,command in [('compile',[javac,'--release','8','-encoding','UTF-8','-d',str(classes),*map(str,production),*map(str,fixtures.rglob('*.java'))]),('run',[java,'-ea','-cp',str(classes),'com.hiro.ulike.Tuning1976Test'])]:
        r=subprocess.run(command,text=True,capture_output=True,timeout=120);(work/(name+'.log')).write_text(r.stdout+r.stderr)
        if r.returncode:raise RuntimeError(name+'\n'+r.stdout+r.stderr)
    result=json.loads(r.stdout.strip().splitlines()[-1]);result.update(physical_android_tested=False,fixture_classes_in_runtime=False,tuning_full_exact_gate_verified=True,tuning_snapshot_ownership_and_cancel_verified=True,scalar_only_tuning_persistence_verified=True,twelve_candidate_profile_tuning_verified=True,legacy_aggregate_restore_verified=True,new_candidate_requires_old_exact_timing_baseline=True,production_source_sha256={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in production})
    (work/'tuning-host-result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),indent=2))
