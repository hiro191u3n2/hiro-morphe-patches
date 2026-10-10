package com.hiro.ulike;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/** Explicit resource/transfer fault holder. Actual GpuChain, GpuPolicy and
 * StrongNoise execute from the release closure. No GPU output is synthesized. */
final class GpuNoise1960 {
    static final int STRONG=0,SINGLE=1,ANALYSIS=2,GEOMETRY=3,ANALYSIS1961=4,
        RESIDUAL1961=5,PROTECTION1961=6,COMPARE1961=7,FINISH1961=8;
    static final long MAX_BYTES=512L*1024*1024;
    static boolean enabled,single=true,busy,budget=true;static int fault,operations,opens,closes,active;
    static long plannedJava,plannedMaximum=Long.MAX_VALUE,reserved;static long[] plannedCapacities;
    static int planCalls,leaseCalls,leaseCloses,leases,revalidation;static boolean reserve=true;
    static void resetPlan(){plannedJava=reserved=0;plannedCapacities=null;planCalls=leaseCalls=leaseCloses=leases=revalidation=0;plannedMaximum=Long.MAX_VALUE;reserve=budget=true;}
    static boolean planFits1981(long[] capacities,long javaBytes){planCalls++;plannedCapacities=capacities.clone();plannedJava=javaBytes;for(long n:capacities)if(n>plannedMaximum)return false;return budget;}
    static long reservedBytes1971(){return reserved;}
    static boolean warmEnvironment1973(){return enabled;}
    static final String ENV="gpu1980-transport-fault-fixture";
    static boolean available(){return enabled;}
    static boolean supports(int p){return enabled&&(p!=SINGLE||single);}
    static int variant(int p,int v){return p+v*9;}
    static int strongProgram(int m,int v){return 27+m+4*v;}
    static String fingerprint(){return enabled?ENV:"";}
    static boolean sessionBusy(){return busy||active!=0;}
    static boolean workspaceFits(long bytes){return budget&&bytes>=0&&bytes<=MAX_BYTES;}
    static long retainedBytes(){return 0;}
    static boolean operation(){
        operations++;
        if(fault==2)throw new IllegalStateException("injected GPU transfer failure");
        if(fault==3)throw new UnsatisfiedLinkError("injected GPU entry unavailable");
        if(fault==4){Thread.currentThread().interrupt();return false;}
        if(fault==5)throw new java.util.concurrent.CancellationException("explicit transport cancellation");
        return fault==1;
    }
    static Session open(){if(!enabled||busy||active!=0)return null;opens++;active++;return new Session();}
    static final class Batch {
        Batch upload(int s,int[] a){return this;}Batch upload(int s,float[] a){return this;}
        Batch uploadDirect(int s,int[] a){return this;}Batch uploadDirect(int s,float[] a){return this;}
        Batch uploadDirect(int s,ByteBuffer a){return this;}Batch allocate(int s,long n){return this;}
        Batch dispatch(int p,int[] b,int[] u,float[] f,int n){return this;}
    }
    static final class Ticket {final int bank;Ticket(int b){bank=b;}}
    static final class Lease1971 implements AutoCloseable {
        final long debt;boolean closed;
        Lease1971(long n){debt=n;leases++;reserved+=n;}
        boolean revalidate1971(){if(revalidation==2)throw new java.util.concurrent.CancellationException("preflight revalidate cancel");if(revalidation==3)throw new IllegalStateException("preflight revalidate fault");return revalidation==0;}
        boolean consumeJava1974(long n){return true;}
        public void close(){if(!closed){closed=true;leases--;leaseCloses++;reserved-=debt;}}
    }
    static final class Session implements AutoCloseable {
        boolean closed;
        Batch newBatch(){return new Batch();}
        boolean upload(int s,int[] a){return operation();}boolean upload(int s,float[] a){return operation();}
        boolean allocate(int s,long n){return operation();}
        boolean dispatch(int p,int[] b,int[] u,float[] f,int n){return operation();}
        boolean run(Batch b){return operation();}
        int[] readInts(int s,int n){return operation()?new int[n]:null;}
        int[][] execute(Batch b,int[] s,int[] n){return reads(n);}
        int[][] readMany(int[] s,int[] n){return reads(n);}
        private int[][] reads(int[] n){if(!operation())return null;int[][] out=new int[n.length][];for(int i=0;i<n.length;i++)out[i]=new int[n[i]];return out;}
        boolean executeInto(Batch b,int s,int n,int[] out,int off){if(!operation())return false;Arrays.fill(out,off,off+n,0);return true;}
        Ticket submit(Batch b,int bank){return operation()?new Ticket(bank):null;}
        boolean collectInto(Ticket t,int s,int n,int[] out,int off){if(!operation())return false;Arrays.fill(out,off,off+n,0);return true;}
        boolean copy1976(int a,int b,int c,int d,int n){return operation();}
        boolean uploadRange1976(int s,int dst,int[] a,int off,int n){return operation();}
        Lease1971 reserveCapacity1971(int[] s,long[] n,long j){leaseCalls++;long total=j;for(long a:n)total+=a;return reserve?new Lease1971(total):null;}
        public void close(){if(!closed){closed=true;active--;closes++;}}
    }
}


final class SaveQueue1935 {static boolean idle1953(){return false;}}
final class WholeRoute1953 {static long retainedBytes(){return 0;}}
