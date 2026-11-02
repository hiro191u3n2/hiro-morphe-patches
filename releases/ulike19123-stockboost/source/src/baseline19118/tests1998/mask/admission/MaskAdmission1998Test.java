package com.hiro.ulike;
import java.lang.reflect.Field;
public final class MaskAdmission1998Test {
 static int checks;static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
 static Object get(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 public static void main(String[] args)throws Exception{
  GpuFinishPolicy1992.Fixture f=GpuFinishPolicy1992.fixture(10,1025,257);long baseline=4L*1025*128+32L*(1025+257)+2048+512,extra=128L*1025+4096;
  for(int declines=0;declines<3;declines++){
   GpuNoise1960.reset(declines);GpuFinishPolicy1992.Context c=GpuFinishPolicy1992.open(GpuNoise1960.open(),f.plan,1025,257,128);
   try{
    check(GpuNoise1960.requests[0]==baseline+extra,"optional cache has an explicit bounded first reservation");
    if(declines==0){check(c!=null&&GpuNoise1960.calls==1,"ordinary admission has one lease");check(get(c,"skinRows")!=null&&get(c,"confidenceRows")!=null,"admitted logical-worker caches exist");}
    else{check(GpuNoise1960.calls==2&&GpuNoise1960.requests[1]==baseline,"optional refusal retries exact old capacity and Java allowance");if(declines==1){check(c!=null&&get(c,"skinRows")==null&&get(c,"confidenceRows")==null,"base admission keeps original uncached samplers");}else check(c==null,"base refusal declines the original route");}
    check(GpuNoise1960.live==(c==null?0:1),"never retain an unsuccessful optional lease");
    if(c!=null){GpuNoise1960.Lease1971 lease=(GpuNoise1960.Lease1971)get(c,"lease");check(lease.remaining==512,"all allocated Java arrays leave only original fixed debt");}
   }finally{if(c!=null)c.close();}
   check(GpuNoise1960.live==0,"every successful admission closed");
  }
  final int[] callbacks={0};QualityPixels1932.Plan custom=f.plan.withFaceRegions(new QualityPixels1932.RegionMask(){public int skinQ8(int x,int y){callbacks[0]++;return 2;}public int detailQ8(int x,int y){return 0;}});
  GpuNoise1960.reset(0);check(GpuFinishPolicy1992.open(GpuNoise1960.open(),custom,1025,257,128)==null&&GpuNoise1960.calls==0&&callbacks[0]==0,"unknown callback declines before cache or GPU admission");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"optional_budget_fallback_verified\":true,\"one_owned_lease_verified\":true,\"admission_boundary_mocked\":true}");
 }
}
