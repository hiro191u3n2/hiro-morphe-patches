package com.hiro.ulike;

/** Compiler-only bridge for frozen historical math fixtures. These methods
 * deliberately record nothing. The .88 detail suite compiles the actual facade
 * and actual Timing implementation instead; this file is never an APK input. */
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
    static ProcessingTiming1947.Trace foreground(ProcessingTiming1947.Trace trace){return null;}
    static ProcessingTiming1947.Trace current(){return null;}
    static ProcessingTiming1947.Trace owner(Object image){return null;}
    static void record(ProcessingTiming1947.Trace trace,int phase,int backend,int reason,int units,long nanos){}
    static void copy(ProcessingTiming1947.Trace trace,int phase,int outcome,long bytes,long nanos){}
}
