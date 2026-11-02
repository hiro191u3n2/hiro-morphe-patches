package com.hiro.ulike;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;

public final class CpuPair1956Test {
 static int assertions,cases;static long pixels;
 static void check(boolean b,String message){assertions++;if(!b)throw new AssertionError(message);}
 static void field(String name,int value)throws Exception {Field f=CorePixels1950.class.getDeclaredField(name);f.setAccessible(true);f.setInt(null,value);}
 static DetailPixels.Work work(int[] source){DetailPixels.Work w=new DetailPixels.Work(source.length);System.arraycopy(source,0,w.source,0,source.length);Arrays.fill(w.horizontal,0x13579bdf);Arrays.fill(w.denoised,0x2468ace0);Arrays.fill(w.output,0x5a5a5a5a);return w;}
 public static void main(String[] args)throws Exception {
  H8DexOracle1951.init(args[0]);field("verified",1);field("gpuVerified",1);
  Field loaded=CorePixels1950.class.getDeclaredField("GPU_LOADED");loaded.setAccessible(true);check(!loaded.getBoolean(null),"CPU-only native endpoint loaded");
  Random random=new Random(1951);int width=17,rows=79;
  int[][] regions={{0,rows},{1,rows-2},{8,3},{rows-4,4},{0,3}};
  for(int noise=1;noise<=4;noise++)for(int sharp=0;sharp<=4;sharp++)for(int region=0;region<regions.length;region++) {
   int first=regions[region][0],count=regions[region][1],flags=(noise+sharp+region)&7;boolean texture=(flags&1)!=0,halos=(flags&2)!=0,shadows=(flags&4)!=0;
   int[] source=new int[width*rows];for(int i=0;i<source.length;i++){int x=i%width,y=i/width,k=random.nextInt(13)-6;int r=55+x*3+y*2+k,g=61+x*3+y*2+k,b=68+x*3+y*2+k;source[i]=0xff000000|r<<16|g<<8|b;if(i%43==0)source[i]&=0x7fffffff;}
   DetailPixels.Work expected=work(source),actual=work(source);
   H8DexOracle1951.filter(expected,width,rows,first,count,noise,sharp,texture,halos,shadows,false);
   Arrays.fill(DetailSerial186.stages,0);H8DexOracle1951.gpuCalls=0;
   // Admission is already independently tested. Pin it fast here to exercise
   // the production helper's GPU-success route on every exact-output case.
   Field key=CorePixels1950.class.getDeclaredField("gpuAdmissionKey");key.setAccessible(true);key.setLong(null,((((((long)width*31+noise)*31+sharp)*2+(texture?1:0))*2+(halos?1:0))*2+(shadows?1:0)));
   field("gpuAdmission",-1);
   CorePixels1950.filter(actual,width,rows,first,count,noise,sharp,texture,halos,shadows);
   check(H8DexOracle1951.gpuCalls==0,"GPU endpoint absent; exact CPU pair executed");
   check(DetailSerial186.stages[0]==0 && DetailSerial186.stages[1]==0,"phase argument first: CPU denoise suppressed");
   check(DetailSerial186.stages[2]==(sharp>0?1:0) && DetailSerial186.stages[3]==(sharp>0?1:0),"packing and sharpening retained");
   for(int i=0;i<source.length;i++)check(actual.output[i]==expected.output[i],"production filter differs sharp="+sharp+" region="+region+" pixel="+i+" expected="+Integer.toHexString(expected.output[i])+" actual="+Integer.toHexString(actual.output[i]));
   check(Arrays.equals(source,actual.source),"source immutable");cases++;pixels+=(long)width*count;
  }
  // The tiny interval makes the old (first,last,phase) interpretation also
  // suppress packing/sharpening, so cover the boundary explicitly.
  for(int sharp=0;sharp<=4;sharp++) {
   int[] source={0xff39332c};DetailPixels.Work expected=work(source),actual=work(source);
   H8DexOracle1951.filter(expected,1,1,0,1,2,sharp,true,true,true,false);
   Field key=CorePixels1950.class.getDeclaredField("gpuAdmissionKey");key.setAccessible(true);key.setLong(null,((((((long)1*31+2)*31+sharp)*2+1)*2+1)*2+1));field("gpuAdmission",-1);
   Arrays.fill(DetailSerial186.stages,0);CorePixels1950.filter(actual,1,1,0,1,2,sharp,true,true,true);
   check(Arrays.equals(expected.output,actual.output),"one-row filter preserves final output");
   check(DetailSerial186.stages[0]==0&&DetailSerial186.stages[1]==0,"one-row phase suppression");
   check(DetailSerial186.stages[2]==(sharp>0?1:0)&&DetailSerial186.stages[3]==(sharp>0?1:0),"one-row finishing retained");cases++;pixels++;
  }
  // Stale/unsupported CPU pair preserves the original stage and exception route.
  field("cpuPairVerified1956",-1);
  for(int sharp=0;sharp<=4;sharp++) {
   int[] source={0xff39332c,0xff4a4039,0xff57534c,0xff6a6258};
   DetailPixels.Work expected=work(source),actual=work(source);
   H8DexOracle1951.filter(expected,2,2,0,2,2,sharp,true,true,true,false);
   Arrays.fill(DetailSerial186.stages,0);CorePixels1950.filter(actual,2,2,0,2,2,sharp,true,true,true);
   check(Arrays.equals(expected.output,actual.output),"unavailable paired CPU preserves retained filter output");
   check(DetailSerial186.stages[0]==1&&DetailSerial186.stages[1]==1,"unavailable pair executes original horizontal and vertical stages");
   cases++;
  }
  int[] same=new int[4];Arrays.fill(same,0x13579bdf);
  check(!CorePixels1950.runPair1956(same,same,new int[4],2,2,2,true,true,0,2),"source/output alias refused");
  check(same[0]==0x13579bdf,"alias refusal preserves source");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"pixels_compared\":"+pixels+",\"stage_parameter_order_verified\":true,\"sharpening_halo_pixel_exact\":true,\"production_filter_dex_oracle_executed\":true,\"stage_0_1_suppressed\":true,\"stage_2_3_preserved\":true,\"cpu_only_pair_executed\":true,\"physical_android_tested\":false}");
 }
}
