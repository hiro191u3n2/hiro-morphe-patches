package com.hiro.ulike;

import java.security.MessageDigest;
import java.util.concurrent.CancellationException;

/** Independent arithmetic candidate. Old tile8 proofs only select the old
 * engine. This engine needs its own full ARGB/confidence corpus and two-width
 * reversed-order complete transfer timing before it can produce a photo. */
final class GpuStrongOptimized19119 {
    private GpuStrongOptimized19119(){}
    private static final String JOB="strong-standard-layout1994-optimized19119-v1";
    private static volatile String exactKey,fastKey;
    private static boolean requested,attempted;
    private static volatile boolean disabled;
    static boolean shape(int width,int height){return width==3060&&height==4080||width==4080&&height==3060;}
    static boolean ready(int width,int height){
        String exact=exactKey,fast=fastKey;
        return !disabled&&shape(width,height)&&GpuNoiseOptimized19119.prepared()&&exact!=null&&fast!=null&&
            GpuStrongDefault1990.Adaptive1994.faster()&&GpuCertificates1990.restore(exact)!=null&&GpuCertificates1990.restore(fast)!=null;
    }
    static void request(){
        if(disabled||!GpuStrongDefault1990.Adaptive1994.faster())return;
        synchronized(GpuStrongOptimized19119.class){if(requested||attempted)return;requested=true;}
        boolean transferred=false;
        try{transferred=GpuQualification1961.schedule(JOB,512,new Probe());}
        catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        finally{if(!transferred)synchronized(GpuStrongOptimized19119.class){requested=false;}}
    }
    private static final class Probe implements GpuQualification1961.Probe {
        public void run(GpuQualification1961.Cancellation cancellation){
            boolean complete=false;
            try{qualify(cancellation);complete=true;}
            finally{if(complete&&!cancellation.cancelled()&&!Thread.currentThread().isInterrupted())
                synchronized(GpuStrongOptimized19119.class){attempted=true;}}
        }
        public void close(){synchronized(GpuStrongOptimized19119.class){requested=false;}}
    }
    private static void check(GpuQualification1961.Cancellation cancellation){
        if(cancellation==null||cancellation.cancelled()||Thread.currentThread().isInterrupted())
            throw new CancellationException("optimized Strong proof cancelled");
    }
    private static String digest(String value){
        try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));StringBuilder out=new StringBuilder(64);
            for(byte b:bytes){out.append(Character.forDigit((b>>>4)&15,16));out.append(Character.forDigit(b&15,16));}return out.toString();
        }catch(Exception invalid){throw new IllegalStateException("candidate identity",invalid);}
    }
    private static void reject(String reason){
        disabled=true;String exact=exactKey;if(exact!=null)GpuCertificates1990.reject(exact,reason);
        GpuQualification1961.outcome1984("quality_mismatch");
    }
    static void unavailable(){disabled=true;}
    private static int[] confidence(GpuStrongDefault1990.Fixture f){return f.u[12]==0?null:new int[((f.u[0]+3)/4)*((f.u[3]-f.u[2]+3)/4)];}
    private static boolean oracle(GpuStrongDefault1990.Fixture f,int[] output,int[] confidence){
        return StrongNoise1958.gpuOracleSnapshot1961(f.source,output,f.u[0],f.u[1],f.u[2],f.u[3],f.u[4],f.u[5],
            f.u[6],f.u[8],f.u[9]!=0,f.model,f.policy,confidence);
    }
    private static final class Read {final GpuStrong1960.Result result;final long nanos;
        Read(GpuStrong1960.Result value,long time){result=value;nanos=time;}}
    private static Read timed(GpuStrongDefault1990.Fixture f,boolean candidate,GpuQualification1961.Cancellation cancellation){
        check(cancellation);long[] timing=new long[1];
        GpuStrong1960.Read1971 output=GpuStrong1960.ephemeralOptimized19119(f.source,StrongNoise1958.gpuEvidence1960(f.model),
            StrongNoise1958.gpuMaps1960(f.model),f.policy,f.u,candidate,timing);
        long elapsed=timing[0];check(cancellation);
        if(elapsed<=0||output==null||output.result==null||output.failure!=null){GpuQualification1961.outcome1984("transport_failure");return null;}
        return new Read(output.result,elapsed);
    }
    private static String compare(GpuStrongDefault1990.Fixture f,Read actual,int[] expected,int[] confidence,GpuQualification1961.Cancellation cancellation){
        return actual==null?"unavailable":GpuStrongDefault1990.difference19112(actual.result.pixels,actual.result.confidence,
            expected,confidence,f.u[0]*f.u[2],f.u[0]*(f.u[3]-f.u[2]),cancellation);
    }
    static boolean wins(long baselineBest,long candidateWorst){
        return baselineBest>0&&baselineBest!=Long.MAX_VALUE&&candidateWorst>0&&candidateWorst<=baselineBest-baselineBest/10;
    }
    private static boolean validWorkload(GpuStrong1960.WorkloadResult19119 value,boolean candidate,int width){
        return value!=null&&value.failure==null&&value.nanos>0&&value.maxInflight==2&&value.timedModelUploads==(candidate?1:0)&&
            value.pixels==12484800L&&(width==3060&&value.strips==16&&value.tailRows==240||width==4080&&value.strips==12&&value.tailRows==244);
    }
    static boolean qualify(GpuQualification1961.Cancellation cancellation){
        check(cancellation);
        if(!GpuNoiseOptimized19119.prepare()){GpuQualification1961.outcome1984("unsupported");return false;}
        String identity=GpuNoiseOptimized19119.fingerprint();if(identity==null||identity.length()==0)return false;
        exactKey="strong19119-quotient-tile8-exact70-v1:"+digest(identity);
        fastKey="strong19119-quotient-tile8-fullphoto-two-banks-two-trials-10pct-v2:"+digest(identity);
        check(cancellation);
        if(GpuCertificates1990.rejected(exactKey)){disabled=true;GpuQualification1961.outcome1984("quality_rejected");return false;}
        if(GpuCertificates1990.restore(exactKey)!=null&&GpuCertificates1990.restore(fastKey)!=null){GpuQualification1961.outcome1984("cached_preparation");return true;}
        if(!GpuQualification1961.optionalStrongBenchmarksAllowed19117())throw new CancellationException("defer arithmetic benchmark until idle");
        long cpu=0,gpu=0,pixels=0;boolean fast=true;
        for(int id=0;id<70;id++){
            check(cancellation);GpuQualification1961.progress1984("strong_cpu_reference",id,70);
            GpuStrongDefault1990.Fixture f=id<68?GpuStrongDefault1990.fixture19112(id,cancellation):
                GpuStrongDefault1990.Adaptive1994.benchmark19112(id-68+64,cancellation);
            int[] expected=new int[f.source.length],confidence=confidence(f);
            long started=System.nanoTime();boolean computed=oracle(f,expected,confidence);cpu+=Math.max(1L,System.nanoTime()-started);
            check(cancellation);if(!computed){GpuQualification1961.outcome1984("baseline_unavailable");return false;}
            Read actual=timed(f,true,cancellation);if(actual==null)return false;
            String mismatch=compare(f,actual,expected,confidence,cancellation);
            if(mismatch!=null){reject("case="+id+" "+mismatch);return false;}
            gpu+=actual.nanos;pixels+=(long)f.u[0]*(f.u[3]-f.u[2]);

        }
        for(int shape=0;shape<2;shape++){
            check(cancellation);GpuQualification1961.progress1984("strong_speed_compare",shape,2);
            GpuStrongDefault1990.Fixture seed=GpuStrongDefault1990.Adaptive1994.benchmark19112(shape+64,cancellation);
            GpuStrong1960.Workload19119 workload=GpuStrong1960.prepareWorkload19119(seed,cancellation);
            if(workload==null){GpuQualification1961.outcome1984("memory_or_reference_unavailable");return false;}
            try{
                GpuStrong1960.WorkloadResult19119 warm=workload.measure(false,cancellation);
                if(!validWorkload(warm,false,seed.u[0])){GpuQualification1961.outcome1984("reference_unstable");return false;}
                long best=Long.MAX_VALUE,worst=0;
                for(int trial=0;trial<2;trial++)for(int order=0;order<2;order++){
                    check(cancellation);boolean candidate=(trial+order)%2!=0;
                    GpuStrong1960.WorkloadResult19119 measured=workload.measure(candidate,cancellation);check(cancellation);
                    if(!validWorkload(measured,candidate,seed.u[0])){
                        if(candidate&&measured!=null&&("argb_mismatch".equals(measured.failure)||"confidence_mismatch".equals(measured.failure)))reject("workload="+shape+" "+measured.failure);
                        else GpuQualification1961.outcome1984(candidate?"transport_failure":"reference_unstable");return false;
                    }
                    if(candidate)worst=Math.max(worst,measured.nanos);else best=Math.min(best,measured.nanos);
                }
                fast&=wins(best,worst);
                try{CameraTrace1965.event("strong_optimized19119_speed",0,"width="+seed.u[0]+" height="+seed.model.height+" full_photo=true two_banks=true model_uploads_candidate=1 common_input_comparison_included=true baseline_tile8_ns="+best+" candidate_ns="+worst);}catch(Throwable optional){}
            }finally{workload.close();}
        }
        check(cancellation);GpuQualification1961.progress1984("strong_speed_compare",2,2);
        if(!identity.equals(GpuNoiseOptimized19119.fingerprint())){GpuQualification1961.outcome1984("reference_unstable");return false;}
        if(!GpuCertificates1990.passed(exactKey,70,pixels,cpu,gpu)){GpuQualification1961.outcome1984("comparison_incomplete");return false;}
        check(cancellation);
        if(fast&&!GpuCertificates1990.passed(fastKey,70,pixels,cpu,gpu)){GpuQualification1961.outcome1984("comparison_incomplete");return false;}
        GpuQualification1961.outcome1984(fast?"qualified":"speed_condition");return fast;
    }
}
