package com.hiro.ulike;
import java.util.*;

/** Real JNI/GLES comparison of old serial spectral mode2 and cooperative mode5.
 * Every histogram/count word and every result float bit is compared. */
public final class ModelSpectral19100Test {
 static long assertions,words;static int cases;
 static void check(boolean b,String why){assertions++;if(!b)throw new AssertionError(why);}
 static int[] pixels(int w,int h,int pattern){
  int[] result=new int[w*h];long random=0x191004d4fL+pattern;
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){
   random=(1664525L*random+1013904223L)&0xffffffffL;int jitter=((int)(random>>>24)&63)-32;
   int base=pattern==0?2:pattern==1?253:pattern==2?48:pattern==3?(x<w/2?64:192):((x*17+y*31)&255);
   int r=Math.max(0,Math.min(255,base+jitter)),g=Math.max(0,Math.min(255,base-jitter/2)),b=Math.max(0,Math.min(255,base+jitter/3));
   int alpha=pattern==5&&(x+2*y)%13==0?0:255;result[y*w+x]=alpha<<24|r<<16|g<<8|b;
  }return result;
 }
 static GpuNoise1960.Batch graph(int width,int height,int mode,int shader,int[] histogram,boolean merge){
  GpuNoise1960.Batch batch=new GpuNoise1960.Batch().upload(3,histogram);
  float[] initial=new float[16];for(int i=0;i<16;i++)initial[i]=merge?((i+3)%7)*.39f:0f;
  batch.upload(2,initial);int[] spectral=GpuPolicy1960.spectralUniforms(width,height,7);spectral[0]=mode;
  batch.dispatch(shader,new int[]{0,1,2,3,4,1},spectral,new float[32],spectral[3]*spectral[4]*(mode==5?64:1));
  int[] median=new int[32];median[0]=3;median[9]=7;median[11]=merge?1:0;
  return batch.dispatch(shader,new int[]{0,1,2,3,4,1},median,new float[32],16);
 }
 static int[][] execute(GpuNoise1960.Session s,int w,int h,int mode,int shader,int[] hist,boolean merge){
  int[][] result=s.execute(graph(w,h,mode,shader,hist,merge),new int[]{3,2},new int[]{hist.length,16});
  check(result!=null,"spectral execution mode="+mode+" shader="+shader);return result;
 }
 static void compare(int[][] old,int[][] newer,String label){
  for(int part=0;part<2;part++){check(old[part].length==newer[part].length,label+" shape");
   for(int at=0;at<old[part].length;at++)if(old[part][at]!=newer[part][at])throw new AssertionError(label+" part="+part+" at="+at+" serial="+Integer.toHexString(old[part][at])+" parallel="+Integer.toHexString(newer[part][at]));
   words+=old[part].length;assertions++;
  }
 }
 static void comparison(int w,int h,int pattern,int variant){
  int shader=GpuNoise1960.variant(GpuNoise1960.ANALYSIS,variant);check(GpuNoise1960.supports(shader),"analysis supported variant="+variant);
  // This existing shader intentionally uses 64 lanes for every analysis ID;
  // the model's Strong preparation kernels independently use 32/64/128.
  check(GpuNoise1960.workgroup(shader)==64,"actual analysis workgroup contract remains 64");
  GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"exclusive spectral session");
  try{
   check(s.upload(0,pixels(w,h,pattern))&&s.allocate(1,4)&&s.upload(4,GpuPolicy1960.strongBasis8()),"same source/basis uploaded once");
   int[] hist=new int[7+2064+9];Arrays.fill(hist,0,7,0x12345678);Arrays.fill(hist,7+2064,hist.length,0x76543210);
   for(boolean merge:new boolean[]{false,true}){
    int[][] serial=execute(s,w,h,2,shader,hist,merge),parallel=execute(s,w,h,5,shader,hist,merge);
    compare(serial,parallel,"w="+w+" h="+h+" pattern="+pattern+" variant="+variant+" merge="+merge);
    for(int i=0;i<7;i++)check(parallel[0][i]==0x12345678,"prefix guard");
    for(int i=7+2064;i<hist.length;i++)check(parallel[0][i]==0x76543210,"suffix guard");cases++;
   }
  }finally{s.close();}check(!GpuNoise1960.sessionBusy(),"comparison releases ownership");
 }
 static long timed(GpuNoise1960.Session s,int w,int h,int mode,int shader,int[] hist){long start=System.nanoTime();execute(s,w,h,mode,shader,hist,false);return System.nanoTime()-start;}
 static long[] benchmark(int w,int h){
  int shader=GpuNoise1960.ANALYSIS;GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"benchmark owner");
  try{
   check(s.upload(0,pixels(w,h,3))&&s.allocate(1,4)&&s.upload(4,GpuPolicy1960.strongBasis8()),"benchmark common input");int[] hist=new int[7+2064+9];
   for(int i=0;i<3;i++){compare(execute(s,w,h,2,shader,hist,false),execute(s,w,h,5,shader,hist,false),"timed fixture equal");}
   long[] serial=new long[7],parallel=new long[7];for(int i=0;i<7;i++){
    if((i&1)==0){serial[i]=timed(s,w,h,2,shader,hist);parallel[i]=timed(s,w,h,5,shader,hist);}else{parallel[i]=timed(s,w,h,5,shader,hist);serial[i]=timed(s,w,h,2,shader,hist);}
   }
   Arrays.sort(serial);Arrays.sort(parallel);return new long[]{serial[3],parallel[3],serial[0],parallel[0],serial[6],parallel[6]};
  }finally{s.close();}
 }
 public static void main(String[] args){
  int[][] shapes={{1,1},{1,9},{9,1},{7,8},{8,8},{8,9},{15,17},{16,16},{17,31},{65,67},{127,129},{1530,2040},{2040,1530}};
  for(int variant=0;variant<3;variant++)for(int i=0;i<shapes.length;i++)for(int pattern=0;pattern<6;pattern++)comparison(shapes[i][0],shapes[i][1],pattern,variant);
  long[] portrait=benchmark(1530,2040),landscape=benchmark(2040,1530);
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"spectral_cases\":"+cases+",\"compared_words\":"+words+",\"actual_jni\":true,\"spectral_old_new_bitwise_equal\":true,\"spectral_histogram_guards_preserved\":true,\"spectral_serial_fp32_order_preserved\":true,\"benchmark_environment\":\"host_software_GLES\",\"host_spectral_benchmarks\":[{\"width\":1530,\"height\":2040,\"serial_median_ns\":"+portrait[0]+",\"parallel_median_ns\":"+portrait[1]+"},{\"width\":2040,\"height\":1530,\"serial_median_ns\":"+landscape[0]+",\"parallel_median_ns\":"+landscape[1]+"}],\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
