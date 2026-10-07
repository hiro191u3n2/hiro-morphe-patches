package com.hiro.ulike;
import java.util.*;
public final class Benchmark1933 {
 static final int W=1280,H=960,OW=1920,OH=1440;
 static int[] source=new int[W*H], sink=new int[OW*OH];
 static volatile long checksum;
 static long run(boolean fast){long start=System.nanoTime();
  if(fast)FastPixels1933.resizeCrop(new FastPixels1933.RowSource(){public void readRow(int y,int[] p){System.arraycopy(source,y*W,p,0,W);}},W,H,new FastPixels1933.RowSink(){public void writeRow(int y,int[] p){System.arraycopy(p,0,sink,y*OW,OW);}},OW,OH,0,0,W,H);
  else QualityPixels1932.resizeCrop(new QualityPixels1932.RowSource(){public void readRow(int y,int[] p){System.arraycopy(source,y*W,p,0,W);}},W,H,new QualityPixels1932.RowSink(){public void writeRow(int y,int[] p){System.arraycopy(p,0,sink,y*OW,OW);}},OW,OH,0,0,W,H);
  long nanos=System.nanoTime()-start;long hash=1;for(int p:sink)hash=hash*31+p;checksum=hash;return nanos;
 }
 public static void main(String[] args){Random r=new Random(1933);for(int i=0;i<source.length;i++)source[i]=0xff000000|r.nextInt(0x1000000);
  for(int i=0;i<3;i++){run(false);run(true);}long[] old=new long[7],now=new long[7];long a=0,b=0;
  for(int i=0;i<7;i++){if((i&1)==0){old[i]=run(false);a=checksum;now[i]=run(true);b=checksum;}else{now[i]=run(true);b=checksum;old[i]=run(false);a=checksum;}if(a!=b)throw new AssertionError("pixel hash");}
  Arrays.sort(old);Arrays.sort(now);System.out.println("{\"status\":\"passed\",\"scope\":\"single-thread Java pixel kernel, synthetic opaque RGB\",\"source\":\"1280x960\",\"output\":\"1920x1440\",\"repetitions\":7,\"old_median_ms\":"+old[3]/1e6+",\"new_median_ms\":"+now[3]/1e6+",\"output_checksum_equal\":true,\"device_tested\":false}");}
}
