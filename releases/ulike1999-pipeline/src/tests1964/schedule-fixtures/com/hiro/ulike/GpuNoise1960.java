package com.hiro.ulike;

/** Controlled transport timing only. Production StrongNoise arithmetic,
 * worker pool, qualification and foreground bank admission remain unchanged.
 * Exact shaders have a separate JNI/Mesa gate; this is no device benchmark. */
public final class GpuNoise1960 {
    public static final int STRONG=0,SINGLE=1,ANALYSIS=2,GEOMETRY=3,
        ANALYSIS1961=4,RESIDUAL1961=5,PROTECTION1961=6,COMPARE1961=7,FINISH1961=8;
    public static final long MAX_BYTES=512L*1024*1024;
    public static volatile String identity="timed-transport-gx30-gx36";
    public static volatile boolean busy;
    static int[] pixels,confidence;
    static long openDelay,uploadDelay,closeDelay;
    static int opens,closes,submits;
    static boolean corrupt,unavailable;
    static void reset(int[] expected,int[] mask){pixels=expected;confidence=mask;openDelay=uploadDelay=closeDelay=0;opens=closes=submits=0;corrupt=unavailable=false;}
    static void pause(long nanos){long until=System.nanoTime()+nanos;while(System.nanoTime()<until){if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("transport interrupted");java.util.concurrent.locks.LockSupport.parkNanos(Math.min(1000000L,until-System.nanoTime()));}}
    static boolean available(){return true;}
    static boolean sessionBusy(){return busy;}
    static boolean workspaceFits(long bytes){return true;}
    public static long retainedBytes(){return 0;}
    static String fingerprint(){return identity;}
    static boolean supports(int shader){return true;}
    static int strongProgram(int mode,int variant){return STRONG;}
    static Session open(){opens++;pause(openDelay);return unavailable?null:new Session();}
    static final class Batch {
        int[] u;
        Batch allocate(int slot,long bytes){return this;}
        Batch upload(int slot,int[] values){return this;}
        Batch upload(int slot,float[] values){return this;}
        Batch dispatch(int shader,int[] slots,int[] uniform,float[] f,int count){if(shader==STRONG)u=uniform.clone();return this;}
    }
    static final class Ticket {final Batch batch;Ticket(Batch b){batch=b;}}
    static final class Session {
        boolean upload(int slot,int[] values){pause(uploadDelay);return true;}
        boolean upload(int slot,float[] values){pause(uploadDelay);return true;}
        boolean allocate(int slot,long bytes){return true;}
        boolean dispatch(int shader,int[] slots,int[] u,float[] f,int count){return true;}
        boolean run(Batch batch){return true;}
        int[] readInts(int slot,int count){return new int[count];}
        Ticket submit(Batch batch,int bank){submits++;return new Ticket(batch);}
        int[][] execute(Batch batch,int[] slots,int[] counts){return answer(batch,counts);}
        int[][] collect(Ticket ticket,int[] slots,int[] counts){return answer(ticket.batch,counts);}
        private int[][] answer(Batch batch,int[] counts){
            pause(uploadDelay);int[][] out=new int[counts.length][];
            out[0]=new int[counts[0]];System.arraycopy(pixels,batch.u[2]*batch.u[0],out[0],0,counts[0]);
            if(corrupt)out[0][0]^=1;
            if(counts.length==3){out[1]=confidence.clone();out[2]=new int[]{0};}else out[1]=new int[]{0};
            return out;
        }
        void close(){pause(closeDelay);closes++;}
    }
}
