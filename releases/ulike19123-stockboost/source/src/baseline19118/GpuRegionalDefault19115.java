package com.hiro.ulike;

import java.util.concurrent.CancellationException;

/** Optional Strong regional prepass. Its independent exact and speed evidence
 * never substitutes for startup readiness. Only the tested full-size 256-row
 * shapes may use it in a photograph; short final bands retain the old route. */
final class GpuRegionalDefault19115 {
    private GpuRegionalDefault19115() {}
    static final String EXACT="strong19115-gpu-regional-exact-divide-70cases-v1";
    static final String FAST=EXACT+":both-widths-two-trials-5pct";
    private static final String JOB="strong-regional19115-v1";
    private static boolean requested,attempted;

    static boolean testedShape(int width,int height) {
        return width==3060&&height==4080||width==4080&&height==3060;
    }
    static boolean eligible(int[] u) {
        return u!=null&&u.length>=20&&testedShape(u[0],u[7])&&u[3]-u[2]==256&&GpuStrongDefault1990.owns(u);
    }
    static boolean cached() {
        return GpuCertificates1990.restore(EXACT)!=null&&GpuCertificates1990.restore(FAST)!=null;
    }
    /** The regional speed proof compares profile4 only. A legacy adaptive
     * speed proof may already be faster than profile4, even before its shader
     * has been prepared in this process. Read its original keys directly:
     * Adaptive.cached() uses our current state and would recurse here. */
    static boolean legacyAdaptiveCached19115() {
        return GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.EXACT)!=null&&
                GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.FAST)!=null||
            GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1998.EXACT)!=null&&
                GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1998.FAST)!=null;
    }
    /** A scalar query only. Optional shader preparation is never on this path. */
    static boolean faster() {
        return GpuStrongDefault1990.ready()&&!legacyAdaptiveCached19115()&&GpuNoise1960.regionalReady19115()&&cached();
    }
    static boolean accepts(int[] u) {
        return eligible(u)&&GpuStrongDefault1990.accepts(u)&&faster();
    }
    static synchronized boolean pending(){return requested;}

    static void request() {
        // Even a restored certificate cannot compile the optional program
        // before the first successful publication in this process.
        if(!GpuQualification1961.optionalBenchmarksAllowed1995()||!GpuStrongDefault1990.ready()||
                GpuCertificates1990.rejected(EXACT)||legacyAdaptiveCached19115()||faster())return;
        final boolean heavy=!cached();
        // Cached preparation is not a new speed experiment. New heavy work
        // waits until both existing candidates have completed or have a sticky
        // exact rejection; capture cancellation never counts as completion.
        if(GpuStrongDefault1990.adaptivePending19115()||heavy&&!GpuStrongDefault1990.regionalHeavyAllowed19115())return;
        synchronized(GpuRegionalDefault19115.class){if(requested||attempted)return;requested=true;}
        boolean transferred=false;
        try {transferred=GpuQualification1961.schedule(JOB,512,new Probe19115(heavy));}
        catch(RuntimeException unavailable){}
        catch(LinkageError unavailable){}
        catch(OutOfMemoryError unavailable){}
        finally {if(!transferred)synchronized(GpuRegionalDefault19115.class){requested=false;}}
    }
    private static final class Probe19115 implements GpuQualification1961.Probe {
        private final boolean heavy;
        Probe19115(boolean heavy){this.heavy=heavy;}
        public void run(GpuQualification1961.Cancellation cancellation) {
            boolean completed=false;
            try {qualify(cancellation,heavy);completed=true;}
            finally {if(completed&&!cancellation.cancelled()&&!Thread.currentThread().isInterrupted())
                synchronized(GpuRegionalDefault19115.class){attempted=true;}}
        }
        public void close(){synchronized(GpuRegionalDefault19115.class){requested=false;}}
    }
    private static void check(GpuQualification1961.Cancellation cancellation) {
        if(cancellation==null||cancellation.cancelled()||Thread.currentThread().isInterrupted())
            throw new CancellationException("Strong regional proof cancelled");
    }
    private static void progress(String phase,int complete,int total) {
        try {GpuQualification1961.progress1984(phase,complete,total);}catch(Throwable optional){}
    }
    static boolean measuredWin(long baselineBest,long candidateWorst) {
        if(baselineBest<=0||baselineBest==Long.MAX_VALUE||candidateWorst<=0)return false;
        long required=baselineBest/20+(baselineBest%20==0?0:1);
        return candidateWorst<=baselineBest-required;
    }
    private static final class Read19115 {
        final GpuStrong1960.Result result;final long nanos;
        Read19115(GpuStrong1960.Result result,long nanos){this.result=result;this.nanos=nanos;}
    }
    private static Read19115 timed(GpuStrongDefault1990.Fixture f,boolean regional,
            GpuQualification1961.Cancellation cancellation) {
        check(cancellation);long started=System.nanoTime();
        GpuStrong1960.Read1971 actual=regional?
            GpuStrong1960.ephemeralRegional19115(f.source,StrongNoise1958.gpuEvidence1960(f.model),
                StrongNoise1958.gpuMaps1960(f.model),f.policy,f.u,null,0,4,true):
            GpuStrong1960.ephemeral1973(f.source,StrongNoise1958.gpuEvidence1960(f.model),
                StrongNoise1958.gpuMaps1960(f.model),f.policy,f.u,null,0,4);
        // Both APIs close their owned session before returning: this includes
        // allocation, transfers, waits, readback and new scratch reclamation.
        long elapsed=System.nanoTime()-started;check(cancellation);
        if(actual==null||actual.failure!=null||actual.result==null||regional&&!actual.regional19115) {
            GpuQualification1961.outcome1984(actual!=null&&"memory_budget".equals(actual.failure)?"memory_budget":"transport_failure");
            return null;
        }
        return new Read19115(actual.result,elapsed);
    }
    private static String compare(GpuStrongDefault1990.Fixture f,Read19115 actual,
            int[] expected,int[] confidence,GpuQualification1961.Cancellation cancellation) {
        return GpuStrongDefault1990.difference19112(actual.result.pixels,actual.result.confidence,expected,confidence,
            f.u[0]*f.u[2],f.u[0]*(f.u[3]-f.u[2]),cancellation);
    }
    /** Optional adapter for the independently compiled adaptive-layout owner.
     * The timer remains inside the same complete API used in this proof; method
     * lookup and adapter allocation cannot inflate its measured GPU baseline. */
    static Object[] timedBaseline19115(GpuStrongDefault1990.Fixture f,
            GpuQualification1961.Cancellation cancellation) {
        if(!faster())return null;
        Read19115 measured=timed(f,true,cancellation);
        return measured==null?null:new Object[]{measured.result,Long.valueOf(measured.nanos)};
    }
    private static void reject(boolean regional,int id,String mismatch) {
        GpuCertificates1990.reject(regional?EXACT:GpuStrongDefault1990.SUITE,
            "regional="+regional+" case="+id+" "+mismatch);
        if(!regional)GpuStartup1990.invalidateDomain();
        GpuQualification1961.outcome1984("quality_mismatch");
    }
    static boolean qualify(GpuQualification1961.Cancellation cancellation){return qualify(cancellation,true);}
    static boolean qualify(GpuQualification1961.Cancellation cancellation,boolean heavy) {
        if(cancellation==null)return false;check(cancellation);
        if(!GpuQualification1961.optionalBenchmarksAllowed1995())
            throw new CancellationException("optional regional preparation deferred until publication");
        if(!GpuStrongDefault1990.ready()){GpuQualification1961.outcome1984("baseline_unavailable");return false;}
        if(GpuCertificates1990.rejected(EXACT)){GpuQualification1961.outcome1984("quality_rejected");return false;}
        if(legacyAdaptiveCached19115()){GpuQualification1961.outcome1984("baseline_unavailable");return false;}
        if(GpuStrongDefault1990.adaptivePending19115()||heavy&&!cached()&&!GpuStrongDefault1990.regionalHeavyAllowed19115()){
            GpuQualification1961.outcome1984("baseline_unavailable");return false;
        }
        if(!GpuNoise1960.prepareRegional19115()){GpuQualification1961.outcome1984("unsupported");return false;}
        check(cancellation);
        if(cached()) {
            boolean selected=faster();GpuQualification1961.outcome1984(selected?"cached_preparation":"execution_unavailable");return selected;
        }
        if(!heavy)throw new CancellationException("regional cached preparation cannot become a heavy proof");
        long cpu=0,gpu=0,pixels=0;int cases=0;boolean speed=true;
        try {
            for(int id=0;id<GpuStrongDefault1990.CASES+2;id++) {
                check(cancellation);progress("strong_cpu_reference",id,GpuStrongDefault1990.CASES+2);
                GpuStrongDefault1990.Fixture f=id<GpuStrongDefault1990.CASES?
                    GpuStrongDefault1990.fixture19112(id,cancellation):
                    GpuStrongDefault1990.Adaptive1994.benchmark19112(64+id-GpuStrongDefault1990.CASES,cancellation);
                check(cancellation);
                int[] expected=new int[f.source.length];
                int[] confidence=f.u[12]==0?null:new int[((f.u[0]+3)/4)*((f.u[3]-f.u[2]+3)/4)];
                long started=System.nanoTime();
                boolean computed=StrongNoise1958.gpuOracleSnapshot1961(f.source,expected,f.u[0],f.u[1],f.u[2],f.u[3],
                    f.u[4],f.u[5],f.u[6],f.u[8],f.u[9]!=0,f.model,f.policy,confidence);
                cpu+=Math.max(1,System.nanoTime()-started);check(cancellation);
                if(!computed){GpuQualification1961.outcome1984("baseline_unavailable");return false;}
                Read19115 actual=timed(f,true,cancellation);if(actual==null)return false;
                String mismatch=compare(f,actual,expected,confidence,cancellation);
                if(mismatch!=null){reject(true,id,mismatch);return false;}
                gpu+=Math.max(1,actual.nanos);cases++;pixels+=(long)f.u[0]*(f.u[3]-f.u[2]);
                if(id>=GpuStrongDefault1990.CASES) {
                    progress("strong_speed_compare",id-GpuStrongDefault1990.CASES,2);
                    Read19115 baseline=timed(f,false,cancellation);if(baseline==null)return false;
                    mismatch=compare(f,baseline,expected,confidence,cancellation);
                    if(mismatch!=null){reject(false,id,mismatch);return false;}
                    long best=Long.MAX_VALUE,worst=0;
                    for(int trial=0;trial<2;trial++)for(int order=0;order<2;order++) {
                        boolean regional=(trial+order)%2!=0;
                        Read19115 measured=timed(f,regional,cancellation);if(measured==null)return false;
                        mismatch=compare(f,measured,expected,confidence,cancellation);
                        if(mismatch!=null){reject(regional,id,mismatch);return false;}
                        if(regional)worst=Math.max(worst,measured.nanos);else best=Math.min(best,measured.nanos);
                    }
                    speed&=measuredWin(best,worst);
                    try {CameraTrace1965.event("strong_regional19115_speed",0,"width="+f.u[0]+" rows=256 baseline_gpu_ns="+best+" candidate_gpu_ns="+worst);}catch(Throwable optional){}
                }
            }
            check(cancellation);progress("strong_speed_compare",2,2);
            if(legacyAdaptiveCached19115()){GpuQualification1961.outcome1984("baseline_unavailable");return false;}
            if(!GpuStrongDefault1990.regionalHeavyAllowed19115()){
                GpuQualification1961.outcome1984("baseline_unavailable");return false;
            }
            if(!GpuCertificates1990.passed(EXACT,cases,pixels,cpu,gpu)){GpuQualification1961.outcome1984("comparison_incomplete");return false;}
            check(cancellation);
            if(speed&&!GpuCertificates1990.passed(FAST,cases,pixels,cpu,gpu)){GpuQualification1961.outcome1984("comparison_incomplete");return false;}
            GpuQualification1961.outcome1984(speed?"qualified":"speed_condition");return speed;
        } catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){GpuQualification1961.exception1993(unavailable);GpuQualification1961.outcome1984("exception");return false;}
        catch(LinkageError unavailable){GpuQualification1961.outcome1984("unsupported");return false;}
        catch(OutOfMemoryError unavailable){GpuQualification1961.outcome1984("memory_budget");return false;}
    }
}
