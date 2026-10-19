package com.hiro.ulike;
/** Deterministic admission boundary only; this harness does not model GPU work. */
final class GpuNoise1960 {
 static final int PROTECTION1961=5;static int declines,calls,live;static long[] requests=new long[8];
 static void reset(int refuse){declines=refuse;calls=live=0;requests=new long[8];}
 static boolean supports(int id){return true;}static Session open(){return new Session();}
 static final class Lease1971 {long remaining;boolean closed;Lease1971(long n){remaining=n;live++;}boolean consumeJava1974(long n){if(n<0||n>remaining)throw new AssertionError("excess Java budget consumption");remaining-=n;return true;}boolean revalidate1971(){return true;}public void close(){if(!closed){closed=true;live--;}}}
 static final class Session {Lease1971 reserveCapacity1983(int[] slots,long[] targets,long[] uploads,long javaBytes){requests[calls++]=javaBytes;if(declines>0){declines--;return null;}return new Lease1971(javaBytes);}boolean run(Batch b){return true;}int[][] execute(Batch b,int[] slots,int[] counts){throw new AssertionError("GPU not part of admission stub");}public void close(){}}
 static final class Batch {Batch uploadDirect(int slot,float[] values){return this;}Batch uploadDirect(int slot,int[] values){return this;}Batch allocate(int slot,long bytes){return this;}Batch dispatch(int shader,int[] slots,int[] u,float[] f,int count){return this;}}
}
