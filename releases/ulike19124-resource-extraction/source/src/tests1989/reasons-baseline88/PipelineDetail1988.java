package com.hiro.ulike;

/** Optional, bounded observations at operation boundaries. Capture an explicit
 * foreground owner once, then pass it through the work. This class retains no
 * owner, image, model, provider, decision object or pixel data. */
final class PipelineDetail1988 {
    private PipelineDetail1988() {}
    static final int FRONT_PROBE=0, FRONT_STRIP=1, MODEL_SOURCE=2, MODEL_REGIONS=3, MODEL_MAPS=4;
    static final int CORRECTION_MOIRE=5, CORRECTION_GEOMETRY=6, CORRECTION_SHARP=7;
    static final int AUX_SPATIAL=8, AUX_PROTECTION=9, CORRECTION_JOINED=10;
    static final int UNKNOWN_BACKEND=-1, CPU=0, GPU=1;
    static final int UNKNOWN=0, CPU_FIXED=1, PROOF_MISSING=2, QUALIFICATION_BLOCKED=3;
    static final int SESSION_BUSY=4, BACKGROUND=5, UNAVAILABLE=6, MEMORY_LIMIT=7;
    static final int CANDIDATE_UNAVAILABLE=8, CANDIDATE_FAILURE=9, QUEUE_OCCUPIED=10, CUSTOM_OR_CONSTANT=11;
    static final int RESERVED=1, CAPTURED=2, QUEUED=3, QUEUE_DECLINED=4, SKIPPED=5;
    static final int INTERRUPTED=6, ALLOCATION_FAILED=7, REPLAYED=8, INCOMPLETE=9, LEGACY_UNUSED_PREP_SKIPPED=10;

    static ProcessingTiming1947.Trace foreground(ProcessingTiming1947.Trace trace) {
        try {
            return trace==null||GpuQualification1961.background()||GpuResident1976.benchmarking()||
                GpuResident1976.cpuOracle()||CpuExact1978.background()?null:trace;
        }catch(Throwable optional){return null;}
    }
    /** Only the explicitly entered thread scope is eligible; no latest-photo fallback. */
    static ProcessingTiming1947.Trace current() {
        try{return foreground(ProcessingTiming1947.current());}catch(Throwable optional){return null;}
    }
    static ProcessingTiming1947.Trace owner(Object image) {
        try{return foreground(ProcessingTiming1947.traceFor(image));}catch(Throwable optional){return null;}
    }
    /** backend -1 is unobserved, 0 CPU and 1 GPU. nanos -1 is unmeasured;
     * zero is measured zero. A reason describes an observed CPU decision only. */
    static void record(ProcessingTiming1947.Trace trace,int phase,int backend,int reason,int units,long nanos) {
        try {
            if(foreground(trace)!=null)ProcessingTiming1947.pipelineDetail1988(trace,phase,backend,reason,units,nanos);
        }catch(Throwable optional){}
    }
    /** One snapshot/preparation event; it never asserts a selected GPU output.
     * bytes/nanos -1 means unavailable, while zero is an observed zero. Counts
     * from different outcomes may describe the same snapshot and are not added. */
    static void copy(ProcessingTiming1947.Trace trace,int phase,int outcome,long bytes,long nanos) {
        try {
            if(foreground(trace)!=null)ProcessingTiming1947.pipelineCopy1988(trace,phase,outcome,bytes,nanos);
        }catch(Throwable optional){}
    }
}
