package com.hiro.ulike;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/** Test transport only. Production adapters, CPU arithmetic and persistent
 * qualification are compiled unchanged in the surrounding test closure. */
final class GpuNoise1960 {
    static final int STRONG=0,SINGLE=1,ANALYSIS=2,GEOMETRY=3,ANALYSIS1961=4,
        RESIDUAL1961=5,PROTECTION1961=6,COMPARE1961=7,FINISH1961=8;
    static final long MAX_BYTES=512L*1024*1024;
    static boolean enabled,single=true,busy,budget=true;static int fault,operations,opens,closes,active,modelUploads,openAttempts;
    static final String ENV="pipeline1981-transport-fixture";
    static int[] readback;static Runnable afterRead;
    static boolean available(){return enabled;}
    static boolean supports(int p){return enabled&&(p!=SINGLE||single);}
    static int variant(int p,int v){return p+v*9;}
    static int strongProgram(int m,int v){return 27+m+4*v;}
    static String fingerprint(){return enabled?ENV:"";}
    static boolean sessionBusy(){return busy||active!=0;}
    static boolean workspaceFits(long bytes){return budget&&bytes>=0&&bytes<=MAX_BYTES;}
    static long retainedBytes(){return 0;}
    static long reservedBytes1971(){return 0;}
    static boolean operation(){
        operations++;
        if(fault==2)throw new IllegalStateException("injected GPU transfer failure");
        if(fault==3)throw new UnsatisfiedLinkError("injected GPU entry unavailable");
        if(fault==4){Thread.currentThread().interrupt();return false;}
        if(fault==5)throw new java.util.concurrent.CancellationException("explicit transport cancellation");
        return fault==1;
    }
    static Session open(){openAttempts++;if(!enabled||busy||active!=0)return null;opens++;active++;return new Session();}
    static final class Batch {
        Batch upload(int s,int[] a){return this;}Batch upload(int s,float[] a){return this;}
        Batch uploadDirect(int s,int[] a){return this;}Batch uploadDirect(int s,float[] a){return this;}
        Batch uploadDirect(int s,ByteBuffer a){return this;}Batch allocate(int s,long n){return this;}
        Batch dispatch(int p,int[] b,int[] u,float[] f,int n){return this;}
    }
    static final class Ticket {final int bank;Ticket(int b){bank=b;}}
    static final class Lease1971 implements AutoCloseable {
        boolean revalidate1971(){return true;}boolean consumeJava1974(long n){return true;}
        public void close(){}
    }
    static final class Session implements AutoCloseable {
        boolean closed;
        Batch newBatch(){return new Batch();}
        boolean upload(int s,int[] a){return operation();}boolean upload(int s,float[] a){if(s==2)modelUploads++;return operation();}
        boolean allocate(int s,long n){return operation();}
        boolean dispatch(int p,int[] b,int[] u,float[] f,int n){return operation();}
        boolean run(Batch b){return operation();}
        int[] readInts(int s,int n){if(!operation())return null;int[] result=readback==null?new int[n]:Arrays.copyOf(readback,n);Runnable action=afterRead;afterRead=null;if(action!=null)action.run();return result;}
        int[][] execute(Batch b,int[] s,int[] n){return reads(n);}
        int[][] readMany(int[] s,int[] n){return reads(n);}
        private int[][] reads(int[] n){if(!operation())return null;int[][] out=new int[n.length][];for(int i=0;i<n.length;i++)out[i]=new int[n[i]];return out;}
        boolean executeInto(Batch b,int s,int n,int[] out,int off){if(!operation())return false;Arrays.fill(out,off,off+n,0);return true;}
        Ticket submit(Batch b,int bank){return operation()?new Ticket(bank):null;}
        boolean collectInto(Ticket t,int s,int n,int[] out,int off){if(!operation())return false;Arrays.fill(out,off,off+n,0);return true;}
        boolean copy1976(int a,int b,int c,int d,int n){return operation();}
        boolean uploadRange1976(int s,int dst,int[] a,int off,int n){return operation();}
        Lease1971 reserveCapacity1971(int[] s,long[] n,long j){return new Lease1971();}
        public void close(){if(!closed){closed=true;active--;closes++;}}
    }
}

final class SaveQueue1935 {static volatile boolean idle;static boolean idle1953(){return idle;}}
final class WholeRoute1953 {static long retainedBytes(){return 0;}}
