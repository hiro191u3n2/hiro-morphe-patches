package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** v1.9.75: private idle qualification plus fastest exact GPU selection. The
 * queue owns copies of mutable inputs; only scalar certificates survive. GPU
 * preference does not permit an unverified faster kernel to reach a photo. */
public final class GpuStrongTuning1975 {
    private GpuStrongTuning1975() {}
    private static final Object SNAPSHOT_LOCK=new Object();
    private static final int[] BANK={0,1,3,7,8,9,10,11,12,13};
    private static final String PREFIX="strong-gx1976-tuning-policy-bank-v1:";
    private static final String PREVIOUS_PREFIX1976="strong-gx1975-tuning-policy-bank-v1:";
    public static final class Choice {
        public final int profile,variant;public final String key;
        Choice(int profile,int variant,String key){this.profile=profile;this.variant=variant;this.key=key;}
    }
    private static String group(int[] u){
        String profile=GpuStrong1960.profileKey1973(u,0);
        return PREFIX+profile.substring(profile.indexOf(':')+1);
    }
    /** A persisted aggregate is usable only while its exact child certificate
     * still exists with the selected variant and has no exact rejection. */
    public static Choice select(int[] u) {
        try{return selectChecked1975(u);}
        catch(CancellationException cancelled){Thread.currentThread().interrupt();return null;}
        catch(RuntimeException unavailable){return null;}
        catch(LinkageError unavailable){return null;}
        catch(OutOfMemoryError unavailable){return null;}
    }
    private static Choice selectChecked1975(int[] u) {
        if(u==null||u.length<20||u[10]!=3)return null;
        String selectedGroup=group(u);GpuQualification1961.Record winner=GpuQualification1961.restore(selectedGroup);
        if(winner==null){selectedGroup=PREVIOUS_PREFIX1976+selectedGroup.substring(PREFIX.length());winner=GpuQualification1961.restore(selectedGroup);}
        int limit=selectedGroup.startsWith(PREFIX)?11:8;
        if(winner==null||winner.variant<0||winner.variant>limit)return null;
        int profile=winner.variant/3,variant=winner.variant%3;
        String key=GpuStrong1960.profileKey1973(u,profile);
        GpuQualification1961.Record proof=GpuQualification1961.exactRejected(key)?null:GpuQualification1961.restore(key);
        if(proof!=null&&proof.variant==variant)return new Choice(profile,variant,key);
        GpuQualification1961.invalidateStrongTuning1975(selectedGroup,winner.variant);return null;
    }
    /** Called while the foreground still owns its strip source. A background
     * oracle never recursively schedules, and duplicate geometry never clones. */
    public static void schedule(int[] source,int[] policy,int[] u,StrongNoise1958.Model model,
            GpuPolicy1960.Protection protection) {
        if(GpuQualification1961.background()||Thread.currentThread().isInterrupted()||source==null||
                model==null||u==null||u.length<20||u[10]!=3)return;
        try {synchronized(SNAPSHOT_LOCK){
            long count=(long)u[0]*u[1],core=(long)u[0]*(u[3]-u[2]);
            if(count<=0||count>Integer.MAX_VALUE||core<=0||core>Integer.MAX_VALUE/2||
                    source.length<count||policy!=null&&policy.length<core*2)return;
            select(u);String key=group(u);boolean eligible=false;
            for(int p=0;p<4;p++)if(!GpuQualification1961.exactRejected(GpuStrong1960.profileKey1973(u,p)))eligible=true;
            if(!eligible)return;
            // Include original model evidence, all retained arrays and generous
            // fixed object/header overhead. Model maps/evidence are immutable.
            long bytes=4L*(count+(policy==null?0:core*2))+model.residentBytes()+4096L;
            if(protection!=null){
                long grid=1;
                if(protection.plan!=null&&protection.plan.localNoise!=null)
                    grid=Math.max(1L,(long)protection.plan.localNoise.columns*protection.plan.localNoise.rows);
                bytes+=8L*core+4L*grid+1024L;
            }
            if(bytes>96L*1024*1024||!GpuQualification1961.canQueue(key,bytes)||
                    !GpuNoise1960.workspaceFits(bytes+16L*count+24L*core))return;
            GpuPolicy1960.PolicyData descriptor=protection==null?null:
                GpuPolicy1960.detached1975(protection.data(u[0],u[3]-u[2],u[6]+u[2]));
            Proof proof=new Proof(key,Arrays.copyOf(source,(int)count),
                policy==null?null:Arrays.copyOf(policy,(int)core*2),u.clone(),model,descriptor);
            GpuQualification1961.schedule(key,bytes,proof);
        }}catch(CancellationException cancelled){Thread.currentThread().interrupt();}
        catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
    }
    private static final class Proof implements GpuQualification1961.Probe {
        final String key;int[] source,policy,u;StrongNoise1958.Model model;GpuPolicy1960.PolicyData descriptor;
        Proof(String key,int[] source,int[] policy,int[] u,StrongNoise1958.Model model,GpuPolicy1960.PolicyData descriptor){
            this.key=key;this.source=source;this.policy=policy;this.u=u;this.model=model;this.descriptor=descriptor;
        }
        private boolean cpu(int[] output,int[] confidence){
            return StrongNoise1958.gpuOracleSnapshot1961(source,output,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[8],u[9]!=0,model,policy,confidence);
        }
        private String difference(int[] actual,int[] actualConfidence,int[] expected,int[] confidence,int core,int offset){
            if(actual==null||actual.length!=core)return "output_shape_failed";
            for(int i=0;i<core;i++)if(actual[i]!=expected[offset+i])return "argb_mismatch";
            if(confidence!=null&&(actualConfidence==null||!Arrays.equals(actualConfidence,confidence)))return "confidence_mismatch";
            return null;
        }
        private static final class Measured {
            final int[] pixels,confidence;final String failure;final long nanos;
            Measured(int[] p,int[] c,String f,long n){pixels=p;confidence=c;failure=f;nanos=n;}
        }
        /** Same resident immutable model and strip bank as production. A second
         * exact trial recomputes policy and pixels without uploading them again. */
        private Measured measure(GpuNoise1960.Session session,int profile,int variant,boolean repeat,int core,int cf,
                GpuQualification1961.Cancellation cancellation) {
            long start=System.nanoTime();GpuNoise1960.Lease1971 lease=null;
            int[] committed=null,committedConfidence=null;String failure=null;
            try {
                // Materialize final-commit storage before admission so Runtime's
                // used heap includes it. The held lease reserves the additional
                // private readback; no unreserved copy is allocated after it.
                committed=new int[core];committedConfidence=cf==0?null:new int[cf];
                lease=GpuStrong1960.reserveRange1971(session,source,policy,u,descriptor,BANK);
                if(lease==null||!lease.revalidate1971())failure="memory_budget";
                else {
                    if(cancellation.cancelled())return new Measured(null,null,"cancelled",0);
                    GpuNoise1960.Ticket ticket=session.submit(repeat?
                        GpuStrong1960.commandsRepeat1975(u,descriptor,BANK,variant,profile):
                        GpuStrong1960.commands1973(source,policy,u,descriptor,BANK,variant,profile),0);
                    GpuStrong1960.Read1971 read=ticket==null?null:GpuStrong1960.read1971(session,ticket,u,BANK);
                    if(cancellation.cancelled())return new Measured(null,null,"cancelled",0);
                    if(read==null||read.failure!=null||read.result==null)failure=read==null?"submit_failed":read.failure==null?"readback_failed":read.failure;
                    else {
                        GpuStrong1960.Result result=read.result;
                        if(result.pixels==null||result.pixels.length!=core||cf!=0&&(result.confidence==null||result.confidence.length!=cf))failure="output_shape_failed";
                        else {
                            System.arraycopy(result.pixels,0,committed,0,core);
                            if(cf!=0)System.arraycopy(result.confidence,0,committedConfidence,0,cf);
                        }
                    }
                }
            }finally{if(lease!=null)lease.close();}
            return new Measured(committed,committedConfidence,failure,Math.max(1L,System.nanoTime()-start));
        }
        public void run(GpuQualification1961.Cancellation cancellation){
            if(cancellation.cancelled()||source==null)return;
            int core=u[0]*(u[3]-u[2]),offset=u[0]*u[2];
            int cf=u[12]==0?0:((u[0]+3)/4)*((u[3]-u[2]+3)/4);
            // Peak room for two full CPU references, private returned GPU data
            // and its measured final commit is checked before allocating them.
            if(!GpuNoise1960.workspaceFits(8L*source.length+24L*core+8L*cf+1048576L))return;
            int[][] expected=new int[2][],confidence=new int[2][];long minCpu=Long.MAX_VALUE;
            for(int trial=0;trial<2;trial++){
                if(cancellation.cancelled())return;
                expected[trial]=new int[source.length];confidence[trial]=cf==0?null:new int[cf];
                long start=System.nanoTime();if(!cpu(expected[trial],confidence[trial]))return;
                minCpu=Math.min(minCpu,Math.max(1L,System.nanoTime()-start));
                if(cancellation.cancelled())return;
            }
            for(int i=0;i<core;i++)if(expected[0][offset+i]!=expected[1][offset+i])return;
            if(cf!=0&&!Arrays.equals(confidence[0],confidence[1]))return;
            int bestProfile=-1,bestVariant=-1;long bestTime=Long.MAX_VALUE;
            float[] evidence=StrongNoise1958.gpuEvidence1960(model);int[][] maps=StrongNoise1958.gpuMaps1960(model);
            for(int profile=0;profile<4;profile++){
                // New strict tiled arithmetic is eligible only after an old
                // candidate completed both exact trials in this same benchmark.
                // A missing baseline never turns novelty into a speed proof.
                if(profile==3&&bestProfile<0)break;
                String child=GpuStrong1960.profileKey1973(u,profile);
                if(cancellation.cancelled())return;
                if(GpuQualification1961.exactRejected(child))continue;
                int profileBest=-1,bad=0;long profileTime=Long.MAX_VALUE;String cause=null;
                GpuQualification1961.Record old=GpuQualification1961.restore(child);
                for(int variant=0;variant<3;variant++){
                    if(cancellation.cancelled())return;
                    if(!GpuNoise1960.supports(GpuStrong1960.program1973(profile,variant)))continue;
                    boolean exact=true;long worst=0,overhead=0;int completed=0;
                    long opened=System.nanoTime();GpuNoise1960.Session session=GpuNoise1960.open();
                    if(session==null)continue;
                    try {
                        GpuNoise1960.Lease1971 modelLease=session.reserveCapacity1971(new int[]{2,4,5,6},
                            new long[]{4L*evidence.length,4L*maps[0].length,4L*maps[1].length,4L*maps[2].length},0);
                        boolean uploaded=false;
                        try {if(modelLease!=null)uploaded=session.upload(2,evidence)&&session.upload(4,maps[0])&&session.upload(5,maps[1])&&session.upload(6,maps[2]);}
                        finally{if(modelLease!=null)modelLease.close();}
                        overhead=System.nanoTime()-opened;
                        if(!uploaded)exact=false;
                        else for(int trial=0;trial<2;trial++){
                            if(cancellation.cancelled())return;
                            Measured measured=measure(session,profile,variant,trial==1,core,cf,cancellation);
                            if(cancellation.cancelled())return;
                            if(measured.failure!=null){
                                exact=false;
                                if("policy_failure".equals(measured.failure)){
                                    GpuQualification1961.rejectExact1971(child,"policy_failure");cause="policy_failure";
                                }
                                break;
                            }
                            String mismatch=difference(measured.pixels,measured.confidence,expected[trial],confidence[trial],core,offset);
                            if(mismatch!=null){
                                exact=false;bad++;cause=mismatch;
                                // A failed reproof of the currently accepted
                                // variant invalidates that certificate.
                                if(old!=null&&old.variant==variant)GpuQualification1961.rejectExact1971(child,mismatch);
                                break;
                            }
                            completed++;worst=Math.max(worst,measured.nanos);
                        }
                    } finally {
                        long closing=System.nanoTime();
                        try{session.close();}finally{overhead+=System.nanoTime()-closing;}
                    }
                    if(completed!=2)exact=false;
                    if(exact)worst+=overhead/2;
                    if(cancellation.cancelled())return;
                    if(GpuQualification1961.exactRejected(child))break;
                    if(exact&&worst>0&&worst<profileTime){profileTime=worst;profileBest=variant;}
                }
                if(cancellation.cancelled())return;
                if(bad==3)GpuQualification1961.rejectExact1971(child,cause);
                if(profileBest>=0&&!GpuQualification1961.exactRejected(child)){
                    GpuQualification1961.qualifiedStrongProfile1975(key,child,minCpu,profileTime,profileBest);
                    GpuQualification1961.Record saved=GpuQualification1961.restore(child);
                    if(saved!=null&&saved.variant==profileBest&&profileTime<bestTime){bestTime=profileTime;bestProfile=profile;bestVariant=profileBest;}
                }
            }
            if(cancellation.cancelled()||bestProfile<0)return;
            String winner=GpuStrong1960.profileKey1973(u,bestProfile);
            if(GpuQualification1961.exactRejected(winner))return;
            GpuQualification1961.Record saved=GpuQualification1961.restore(winner);
            if(saved==null||saved.variant!=bestVariant)return;
            GpuQualification1961.qualifiedStrongPreferred1970(key,minCpu,bestTime,bestProfile*3+bestVariant);
        }
        public void close(){source=null;policy=null;u=null;model=null;descriptor=null;}
    }
}
