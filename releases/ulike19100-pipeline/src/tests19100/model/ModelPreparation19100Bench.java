package com.hiro.ulike;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
/** Actual resident model graph timings, including uploads and all readbacks. */
public final class ModelPreparation19100Bench {
 static int assertions;static long words;
 static void check(boolean b,String why){assertions++;if(!b)throw new AssertionError(why);}
 static long hash(StrongNoise1958.Model model){
  long hash=0x191004d4fL;for(int[] map:StrongNoise1958.gpuMaps1960(model))for(int value:map){hash=(hash^value)*1099511628211L;words++;}
  for(float value:StrongNoise1958.gpuEvidence1960(model)){hash=(hash^Float.floatToRawIntBits(value))*1099511628211L;words++;}return hash;
 }
 static int[][] copyMaps(StrongNoise1958.Model model){int[][] maps=StrongNoise1958.gpuMaps1960(model),copy=new int[maps.length][];for(int i=0;i<maps.length;i++)copy[i]=maps[i].clone();return copy;}
 static int[] evidenceBits(StrongNoise1958.Model model){float[] values=StrongNoise1958.gpuEvidence1960(model);int[] bits=new int[values.length];for(int i=0;i<bits.length;i++)bits[i]=Float.floatToRawIntBits(values[i]);return bits;}
 static void write(DataOutputStream out,int[] values)throws Exception{out.writeInt(values.length);for(int v:values)out.writeInt(v);}
 public static void main(String[] args)throws Exception{
  check(args.length==1,"exact output destination");StringBuilder cases=new StringBuilder();
  try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[0])))){
   for(int id=0;id<2;id++)for(int variant=0;variant<3;variant++){
    GpuModelDefault1990.Fixture f=GpuModelDefault1990.speedFixture1994(id);long expected=0;long[] samples=new long[7];int[][] expectedMaps=null;int[] expectedEvidence=null;
    for(int pass=0;pass<10;pass++){
     long begin=System.nanoTime();StrongNoise1958.Model model=GpuAnalysis1961.residentCandidateExact1990(f.half,f.width,f.height,f.noise,f.shadows,f.evidence,f.runtime,variant);long elapsed=System.nanoTime()-begin;
     try{
      check(model!=null,"resident model available");long actual=hash(model);
      if(pass==0){expected=actual;expectedMaps=copyMaps(model);expectedEvidence=evidenceBits(model);out.writeInt(f.width);out.writeInt(f.height);out.writeInt(variant);out.writeInt(expectedMaps.length);for(int[] map:expectedMaps)write(out,map);write(out,expectedEvidence);}
      else{int[][] maps=StrongNoise1958.gpuMaps1960(model);check(maps.length==expectedMaps.length,"map count");for(int i=0;i<maps.length;i++)check(Arrays.equals(maps[i],expectedMaps[i]),"repeated complete map bits");check(Arrays.equals(evidenceBits(model),expectedEvidence),"repeated complete evidence bits");}
      if(pass>=3)samples[pass-3]=elapsed;
     }finally{if(model!=null)StrongNoise1958.discardResident1961(model);}
     check(!GpuNoise1960.sessionBusy(),"resident owner released");
    }
    Arrays.sort(samples);if(cases.length()>0)cases.append(',');cases.append("{\"width\":").append(f.width).append(",\"height\":").append(f.height).append(",\"variant\":").append(variant).append(",\"median_ns\":").append(samples[3]).append(",\"min_ns\":").append(samples[0]).append(",\"max_ns\":").append(samples[6]).append(",\"output_hash\":\"").append(Long.toHexString(expected)).append("\"}");
   }
  }
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"compared_words\":"+words+",\"warmup\":3,\"measured_samples\":7,\"model_benchmarks\":["+cases+"],\"actual_jni\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
