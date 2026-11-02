package com.hiro.ulike;
import android.content.Context;
public final class PerformanceHintsUnavailable1952Test {
    public static void main(String[] args) {
        PerformanceHints1952.init(new Context(new Object()));
        if(PerformanceHints1952.begin(1000000L)!=null)throw new AssertionError("absent Android API should be ignored");
        final int[] calls={0};PerformanceHints1952.worker(new Runnable(){public void run(){calls[0]++;PerformanceHints1952.complete(PerformanceHints1952.begin(1000000L));}}).run();
        if(calls[0]!=1)throw new AssertionError("work lost with absent service");
        System.out.println("{\"status\":\"passed\",\"assertions\":2,\"performance_hint_class_absent_checked\":true}");
    }
}
