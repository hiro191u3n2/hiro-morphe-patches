package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** Same-image, transfer-inclusive strong strip geometry tuning. A geometry
 * certificate changes only producer granularity. It never replaces the normal
 * two-exact-trial arithmetic certificate for a newly encountered GPU shape. */
public final class GpuStrongLayout1976 {
    private GpuStrongLayout1976() {}
    private static final Object SNAPSHOT_LOCK=new Object();
    private static final int[][] BANKS={{0,1,3,7,8,9,10,11,12,13},{14,15,16,17,18,19,20,21,22,23}};
    private static String key(int[] u,Choice choice) {
        return "strong-layout1976:3:"+u[0]+":"+u[7]+":"+u[8]+":"+u[9]+":"+u[12]+":"+u[18]+":"+u[19]+
            ":"+choice.profile+":"+choice.variant+":"+choice.cpu+":"+choice.gpu;
    }
    private static final class Choice {
        final int profile,variant;final long cpu,gpu;
        Choice(int profile,GpuQualification1961.Record record){this.profile=profile;variant=record.variant;cpu=record.cpuNanos;gpu=record.gpuNanos;}
    }
    private static Choice choice(int[] u){
        GpuStrongTuning1975.Choice best=GpuStrongTuning1975.select(u);
        if(best!=null){
            GpuQualification1961.Record record=GpuQualification1961.restore(GpuStrong1960.profileKey1973(u,best.profile));
            if(record!=null&&record.variant==best.variant)return new Choice(best.profile,record);
        }
        for(int profile=0;profile<3;profile++){
            String child=GpuStrong1960.profileKey1973(u,profile);
            GpuQualification1961.Record record=GpuQualification1961.exactRejected(child)?null:GpuQualification1961.restore(child);
            if(record!=null&&record.variant>=0&&record.variant<=2)return new Choice(profile,record);
        }
        return null;
    }
    /** Never raises the caller's independently calculated memory limit. Smaller
     * strips let the existing bounded producers feed the same two GPU banks
     * sooner; no threads, banks, mathematical radius, or halo are added. */
    public static int selectRows(int width,int height,int level,boolean shadows,boolean confidence,int beauty,int shadowBudget,int baseline) {
        if(baseline<256||width<1||height<256)return baseline;
        try {
            // The first producer's geometry is deterministic. Tie its layout
            // hint to the current kernel, workgroup and exact proof timings;
            // later profile tuning or reproof therefore retires a stale hint.
            int[] u=new int[32];u[0]=width;u[1]=Math.min(height,256+StrongNoise1958.HALO);u[3]=256;u[5]=u[1];
            u[7]=height;u[8]=level;u[9]=shadows?1:0;u[10]=3;u[11]=1;u[12]=confidence?1:0;u[18]=beauty;u[19]=shadowBudget;
            Choice current=choice(u);if(current==null)return baseline;
            GpuQualification1961.Record layout=GpuQualification1961.restore(key(u,current));
            return layout!=null&&layout.variant==1?128:baseline;
        }catch(RuntimeException unavailable){return baseline;}
        catch(LinkageError unavailable){return baseline;}
        catch(OutOfMemoryError unavailable){return baseline;}
    }
    /** Called before a worker releases or reuses its source/policy arrays. A
     * single representative 256-row block is retained in the existing bounded,
     * capture-cancellable idle queue, never a full additional photograph. */
    public static void schedule(int[] source,int[] policy,int[] u,StrongNoise1958.Model model,
            GpuPolicy1960.Protection protection) {
        if(GpuQualification1961.background()||Thread.currentThread().isInterrupted()||source==null||
                model==null||u==null||u.length<20||u[10]!=3||u[3]-u[2]!=256||
                u[0]<1||u[1]!=Math.min(model.height,256+StrongNoise1958.HALO)||u[4]!=0||u[5]!=u[1]||u[2]!=0||u[6]!=0||u[11]!=1)return;
        try{synchronized(SNAPSHOT_LOCK){
            long count=(long)u[0]*u[1],core=(long)u[0]*256;
            if(count>Integer.MAX_VALUE||core>Integer.MAX_VALUE/2||source.length<count||policy!=null&&policy.length<core*2)return;
            // Only the deterministic first strip is retained. A new profile,
            // workgroup or proof timing produces a different geometry key.
            Choice chosen=choice(u);if(chosen==null)return;
            String name=key(u,chosen);int profile=chosen.profile,variant=chosen.variant;
            long grid=1;
            if(protection!=null&&protection.plan!=null&&protection.plan.localNoise!=null)
                grid=Math.max(1L,(long)protection.plan.localNoise.columns*protection.plan.localNoise.rows);
            long bytes=4L*count+(policy==null?0:8L*core)+model.residentBytes()+4096L+
                (protection==null?0:8L*core+4L*grid+1024L);
            if(bytes>96L*1024*1024||!GpuQualification1961.canQueue(name,bytes)||
                    !GpuNoise1960.workspaceFits(bytes+28L*count+40L*core+2L*StrongNoise1958.workspaceBytes(u[0],256)))return;
            GpuPolicy1960.PolicyData descriptor=protection==null?null:
                GpuPolicy1960.detached1975(protection.data(u[0],256,u[6]+u[2]));
            GpuQualification1961.schedule(name,bytes,new Proof(name,Arrays.copyOf(source,(int)count),
                policy==null?null:Arrays.copyOf(policy,(int)core*2),u.clone(),model,descriptor,profile,variant));
        }}catch(CancellationException cancelled){Thread.currentThread().interrupt();}
        catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
    }
    private static final class Part {
        final int[] source,policy,u;final GpuPolicy1960.PolicyData descriptor;
        final int offset;
        Part(int[] source,int[] policy,int[] u,GpuPolicy1960.PolicyData descriptor,int offset){
            this.source=source;this.policy=policy;this.u=u;this.descriptor=descriptor;this.offset=offset;
        }
    }
    private static final class Measurement {
        final int[] pixels,confidence;final long elapsed;
        Measurement(int[] pixels,int[] confidence,long elapsed){this.pixels=pixels;this.confidence=confidence;this.elapsed=elapsed;}
    }
    private static final class Proof implements GpuQualification1961.Probe {
        final String name;final int profile,variant;
        int[] source,policy,u;StrongNoise1958.Model model;GpuPolicy1960.PolicyData descriptor;
        Proof(String name,int[] source,int[] policy,int[] u,StrongNoise1958.Model model,GpuPolicy1960.PolicyData descriptor,int profile,int variant){
            this.name=name;this.source=source;this.policy=policy;this.u=u;this.model=model;this.descriptor=descriptor;this.profile=profile;this.variant=variant;
        }
        private void check(GpuQualification1961.Cancellation cancellation){
            if(cancellation.cancelled())throw new CancellationException("strong layout cancelled");
        }
        private Part part(int first,int rows){
            int begin=u[2]+first,end=begin+rows;
            int readBegin=Math.max(u[4],begin-StrongNoise1958.HALO),readEnd=Math.min(u[5],end+StrongNoise1958.HALO);
            int[] shape=u.clone();shape[1]=readEnd-readBegin;shape[2]=begin-readBegin;shape[3]=end-readBegin;
            shape[4]=0;shape[5]=shape[1];shape[6]+=readBegin;
            int[] pixels=Arrays.copyOfRange(source,readBegin*u[0],readEnd*u[0]);
            int[] protection=policy==null?null:Arrays.copyOfRange(policy,first*u[0]*2,(first+rows)*u[0]*2);
            return new Part(pixels,protection,shape,descriptor==null?null:GpuPolicy1960.slice1976(descriptor,first,rows),first);
        }
        private Measurement cpu(int rows,GpuQualification1961.Cancellation cancellation){
            long start=System.nanoTime();int[] output=new int[u[0]*256];
            int[] confidence=u[12]==0?null:new int[((u[0]+3)/4)*64];
            for(int first=0;first<256;first+=rows){
                check(cancellation);Part p=part(first,rows);int[] destination=new int[p.source.length];
                int[] cf=confidence==null?null:new int[((u[0]+3)/4)*(rows/4)];
                if(!StrongNoise1958.gpuOracleSnapshot1961(p.source,destination,u[0],p.u[1],p.u[2],p.u[3],0,p.u[1],p.u[6],u[8],u[9]!=0,model,p.policy,cf))return null;
                check(cancellation);System.arraycopy(destination,p.u[2]*u[0],output,first*u[0],rows*u[0]);
                if(cf!=null)System.arraycopy(cf,0,confidence,(first/4)*((u[0]+3)/4),cf.length);
            }
            return new Measurement(output,confidence,Math.max(1L,System.nanoTime()-start));
        }
        /** Submission of the second 128-row producer precedes collection of the
         * first. Thus this measures the same two-bank pipeline, including CPU
         * source/mask slicing, JNI upload, dispatch, fencing and final copies. */
        private Measurement gpu(GpuNoise1960.Session session,int rows,GpuQualification1961.Cancellation cancellation){
            long start=System.nanoTime();int[] output=new int[u[0]*256];
            int[] confidence=u[12]==0?null:new int[((u[0]+3)/4)*64];
            int count=256/rows;Part[] parts=new Part[count];GpuNoise1960.Ticket[] tickets=new GpuNoise1960.Ticket[count];
            GpuNoise1960.Lease1971[] leases=new GpuNoise1960.Lease1971[count];
            try {
                for(int i=0;i<count;i++){
                    check(cancellation);Part p=parts[i]=part(i*rows,rows);
                    leases[i]=GpuStrong1960.reserveRange1971(session,p.source,p.policy,p.u,p.descriptor,BANKS[i]);
                    if(leases[i]==null||!leases[i].revalidate1971())return null;
                    tickets[i]=session.submit(GpuStrong1960.commands1973(p.source,p.policy,p.u,p.descriptor,BANKS[i],variant,profile),i);
                    if(tickets[i]==null)return null;
                }
                for(int i=0;i<count;i++){
                    check(cancellation);Part p=parts[i];
                    GpuStrong1960.Read1971 read=GpuStrong1960.read1971(session,tickets[i],p.u,BANKS[i]);
                    if(read==null||read.failure!=null||read.result==null)return null;
                    GpuStrong1960.Result result=read.result;
                    if(result.pixels==null||result.pixels.length!=rows*u[0]||confidence!=null&&
                            (result.confidence==null||result.confidence.length!=((u[0]+3)/4)*(rows/4)))return null;
                    check(cancellation);System.arraycopy(result.pixels,0,output,p.offset*u[0],result.pixels.length);
                    if(confidence!=null)System.arraycopy(result.confidence,0,confidence,(p.offset/4)*((u[0]+3)/4),result.confidence.length);
                }
                return new Measurement(output,confidence,Math.max(1L,System.nanoTime()-start));
            }finally{for(GpuNoise1960.Lease1971 lease:leases)if(lease!=null)lease.close();}
        }
        private boolean equal(Measurement a,Measurement b){
            return a!=null&&b!=null&&Arrays.equals(a.pixels,b.pixels)&&Arrays.equals(a.confidence,b.confidence);
        }
        public void run(GpuQualification1961.Cancellation cancellation){
            check(cancellation);if(source==null)return;
            if(!GpuNoise1960.workspaceFits(28L*source.length+40L*u[0]*256+2L*StrongNoise1958.workspaceBytes(u[0],256)+1048576L))return;
            String child=GpuStrong1960.profileKey1973(u,profile);
            if(GpuQualification1961.exactRejected(child)||!GpuNoise1960.supports(GpuStrong1960.program1973(profile,variant)))return;
            Measurement[] expected=new Measurement[2];long cpuWhole=Long.MAX_VALUE,cpuSplit=0;
            for(int trial=0;trial<2;trial++){
                check(cancellation);Measurement whole=cpu(256,cancellation),split=cpu(128,cancellation);
                if(!equal(whole,split)||trial!=0&&!equal(expected[0],whole))return;
                expected[trial]=whole;cpuWhole=Math.min(cpuWhole,whole.elapsed);cpuSplit=Math.max(cpuSplit,split.elapsed);
            }
            long[] whole=new long[2],split=new long[2];long overhead=0;
            long opened=System.nanoTime();GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)return;
            try {
                float[] evidence=StrongNoise1958.gpuEvidence1960(model);int[][] maps=StrongNoise1958.gpuMaps1960(model);
                GpuNoise1960.Lease1971 lease=session.reserveCapacity1971(new int[]{2,4,5,6},
                    new long[]{4L*evidence.length,4L*maps[0].length,4L*maps[1].length,4L*maps[2].length},0);
                try{if(lease==null||!session.upload(2,evidence)||!session.upload(4,maps[0])||!session.upload(5,maps[1])||!session.upload(6,maps[2]))return;}
                finally{if(lease!=null)lease.close();}
                overhead=System.nanoTime()-opened;
                for(int trial=0;trial<2;trial++){
                    // Balanced order avoids giving every smaller-strip sample
                    // the warmed device and every baseline the colder device.
                    for(int turn=0;turn<2;turn++){
                        check(cancellation);boolean small=(trial+turn)%2!=0;
                        Measurement measured=gpu(session,small?128:256,cancellation);
                        if(!equal(expected[trial],measured))return;
                        if(small)split[trial]=measured.elapsed;else whole[trial]=measured.elapsed;
                    }
                }
            }finally{long closing=System.nanoTime();try{session.close();}finally{overhead+=System.nanoTime()-closing;}}
            check(cancellation);
            // Require both candidate trials to beat even the best baseline by
            // 5%, and avoid a >5% CPU fallback regression at the same geometry.
            long baseline=Math.min(whole[0],whole[1])+overhead/4;
            long candidate=Math.max(split[0],split[1])+overhead/4;
            if(cpuSplit<=cpuWhole+cpuWhole/20&&candidate<=baseline-baseline/20)
                GpuQualification1961.qualified(name,baseline,candidate,1);
            else GpuQualification1961.rejectSpeed(name);
        }
        public void close(){source=null;policy=null;u=null;model=null;descriptor=null;}
    }
}
