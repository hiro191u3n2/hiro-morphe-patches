package com.hiro.ulike;

/** Controlled transport only. Production helper arithmetic and classification
 * are compiled unchanged; real shader parity is covered by other suites. */
public final class GpuNoise1960 {
    public static final int STRONG=0,SINGLE=1,ANALYSIS=2,GEOMETRY=3,
        ANALYSIS1961=4,RESIDUAL1961=5,PROTECTION1961=6,COMPARE1961=7,FINISH1961=8;
    public static final long MAX_BYTES=512L*1024*1024;
    static String mode="ok";
    static int[] result;
    static int reads,opens,closes;
    static volatile boolean cancelled;
    static void reset(String next,int[] expected){mode=next;result=expected;reads=opens=closes=0;cancelled=false;}
    static boolean available(){return true;}
    static boolean sessionBusy(){return false;}
    static boolean workspaceFits(long bytes){return true;}
    public static long retainedBytes(){return 0;}
    static String fingerprint(){return "controlled-helper-transport1962";}
    static boolean supports(int shader){return true;}
    static Session open(){
        opens++;
        if("defer".equals(mode))return null;
        if("oom".equals(mode))throw new OutOfMemoryError("controlled transport allocation");
        return new Session();
    }
    static final class Batch {
        Batch allocate(int slot,long bytes){return this;}
        Batch upload(int slot,int[] values){return this;}
        Batch upload(int slot,float[] values){return this;}
        Batch dispatch(int shader,int[] slots,int[] u,float[] f,int count){return this;}
    }
    static final class Session {
        boolean upload(int slot,int[] values){return true;}
        boolean upload(int slot,float[] values){return true;}
        boolean allocate(int slot,long bytes){return true;}
        boolean dispatch(int shader,int[] slots,int[] u,float[] f,int count){return true;}
        boolean run(Batch batch){return true;}
        int[] readInts(int slot,int count){
            reads++;
            if("runtime".equals(mode))throw new IllegalStateException("controlled transport fault");
            if("linkage".equals(mode))throw new UnsatisfiedLinkError("controlled transport fault");
            if("cancel".equals(mode)){cancelled=true;return null;}
            if("null".equals(mode))return null;
            if("slow".equals(mode)){
                long until=System.nanoTime()+100000000L;
                while(System.nanoTime()<until)java.util.concurrent.locks.LockSupport.parkNanos(1000000L);
            }
            if(result==null||result.length!=count)throw new AssertionError("fixture result length");
            return result.clone();
        }
        int[][] execute(Batch batch,int[] slots,int[] counts){
            int[] out=readInts(slots[0],counts[0]);return out==null?null:new int[][]{out};
        }
        void close(){closes++;}
    }
}
