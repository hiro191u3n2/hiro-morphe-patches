package com.hiro.ulike;
public final class ExitJobs185 {public static int count;public static boolean sealed;public static void begin(){if(sealed)throw new IllegalStateException("sealed");count++;}public static void end(){FusionPixels1933.assertIdle("ExitJobs.end");if(count<=0)throw new AssertionError("unbalanced counter");count--;}}

