package com.hiro.ulike;

import android.graphics.Bitmap;

/** Optional scalar observations of selected foreground work. No image, model,
 * policy, queue decision or backend certificate is retained by this helper. */
final class PipelineDiagnostics1982 {
    private PipelineDiagnostics1982() {}
    static ProcessingTiming1947.Trace foreground(ProcessingTiming1947.Trace trace) {
        try {
            return trace==null||GpuQualification1961.background()||GpuResident1976.benchmarking()||
                GpuResident1976.cpuOracle()||CpuExact1978.background()?null:trace;
        }catch(Throwable optional){return null;}
    }
    static ProcessingTiming1947.Trace owner(Bitmap image) {
        try {
            if(GpuQualification1961.background()||GpuResident1976.benchmarking()||
                    GpuResident1976.cpuOracle()||CpuExact1978.background())return null;
            return ProcessingTiming1947.traceFor(image);
        } catch(Throwable optional){return null;}
    }
    static void selected(ProcessingTiming1947.Trace trace,int part,int backend,int units,long nanos) {
        try {if(trace!=null)ProcessingTiming1947.backend1982(trace,part,backend,units,nanos);}
        catch(Throwable optional){}
    }
    static void defaultRoute(ProcessingTiming1947.Trace trace) {
        try {if(trace!=null)ProcessingTiming1947.detailDefaultRoute1982(trace);}
        catch(Throwable optional){}
    }
    static void residentExit(ProcessingTiming1947.Trace trace,long retained,int reason) {
        try {if(trace!=null)ProcessingTiming1947.residentExit1982(trace,retained,reason);}
        catch(Throwable optional){}
    }
}
