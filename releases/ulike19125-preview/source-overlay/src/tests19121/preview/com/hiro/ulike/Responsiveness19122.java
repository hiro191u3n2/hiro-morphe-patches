package com.hiro.ulike;
/** Spy for the optional observation boundary; never supplies a render result. */
public final class Responsiveness19122 {
    public static int requests;
    public static boolean fail;
    public static ProcessingTiming1947.Trace trace;
    public static long inputReady,returned;
    public static void reset(){requests=0;fail=false;trace=null;inputReady=returned=0;}
    public static void previewRequested19125(ProcessingTiming1947.Trace value,long input,long end){
        requests++;trace=value;inputReady=input;returned=end;
        if(fail)throw new IllegalStateException("optional observer unavailable");
    }
}
