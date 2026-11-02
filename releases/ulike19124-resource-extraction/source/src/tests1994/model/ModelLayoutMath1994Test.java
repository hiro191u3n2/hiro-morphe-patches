package com.hiro.ulike;

/** The additional full-width timing inputs also run through all real JNI
 * layouts and the production native CPU oracle before release. */
public final class ModelLayoutMath1994Test {
 public static void main(String[] args){
  int assertions=0,cases=0;long words=0;
  if(!StrongNoise1958.nativeAvailable())throw new AssertionError("native CPU oracle required");assertions++;
  for(int id=0;id<2;id++){
   GpuModelDefault1990.Fixture f=GpuModelDefault1990.speedFixture1994(id);
   StrongNoise1958.Model cpu=StrongNoise1958.preparedSnapshotCpu1961(f.half,f.width,f.height,f.noise,f.shadows,f.evidence,f.runtime);
   for(int variant=0;variant<3;variant++){
    StrongNoise1958.Model gpu=GpuAnalysis1961.residentCandidateExact1990(f.half,f.width,f.height,f.noise,f.shadows,f.evidence,f.runtime,variant);
    try{
     if(gpu==null||!StrongNoise1958.sameModel1961(cpu,gpu))throw new AssertionError("model speed corpus "+id+" variant="+variant+" "+StrongNoise1958.modelDifference1991(cpu,gpu));assertions++;
     for(int[] map:StrongNoise1958.gpuMaps1960(gpu))words+=map.length;words+=64+StrongNoise1958.gpuEvidence1960(gpu).length;cases++;
    }finally{if(gpu!=null)StrongNoise1958.discardResident1961(gpu);}
    if(GpuNoise1960.sessionBusy())throw new AssertionError("leaked resident");assertions++;
   }
  }
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"model_cases\":"+cases+",\"compared_words\":"+words+",\"actual_cpu_jni\":true,\"actual_jni\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
