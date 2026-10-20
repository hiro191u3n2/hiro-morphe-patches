package com.hiro.ulike;
/** Controlled output-only sink. This is deliberately not the timing collector proof. */
public final class ResidentTiming19111 {
    static int calls,fault;static String key;static long[] values;
    public static void dispatch(String name,long[] payload){
        calls++;key=name;values=payload.clone();java.util.Arrays.fill(payload,Long.MAX_VALUE);
        if(fault==1)throw new IllegalStateException("observer unavailable");
        if(fault==2)throw new LinkageError("observer unavailable");
        if(fault==3)throw new OutOfMemoryError("observer unavailable");
        if(fault==4)throw new AssertionError("observer unavailable");
    }
}
