package com.hiro.ulike;

/** Controlled background providers; production timing and detail sinks are real. */
final class PipelineProviders1988 {
    static volatile int mask,failingProvider=-1,failure;
    static int calls;
    static void reset(){mask=0;failingProvider=-1;failure=0;calls=0;}
    static boolean read(int provider){
        calls++;
        if(provider==failingProvider)switch(failure){
            case 1:throw new RuntimeException("optional provider");
            case 2:throw new LinkageError("optional provider");
            case 3:throw new AssertionError("optional provider");
            case 4:throw new OutOfMemoryError("optional provider");
            default:break;
        }
        return (mask&(1<<provider))!=0;
    }
}
final class GpuResident1976 {
    static boolean benchmarking(){return PipelineProviders1988.read(1);}
    static boolean cpuOracle(){return PipelineProviders1988.read(2);}
}
final class CpuExact1978 {
    static boolean background(){return PipelineProviders1988.read(3);}
}
