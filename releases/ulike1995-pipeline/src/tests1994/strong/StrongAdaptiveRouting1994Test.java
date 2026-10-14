package com.hiro.ulike;
/** Real dispatcher with explicit controlled certificate and transport inputs. */
public final class StrongAdaptiveRouting1994Test {
 static int assertions;static void check(boolean v,String s){assertions++;if(!v)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  StrongDefaultRouting1990Test.reset();StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);
  check(d.route().profile==4,"baseline selected at stage start");GpuStrongDefault1990.optional1994=true;
  check(d.route().profile==4&&d.process(d.route()),"new candidate certificate cannot change active stage");GpuStrong1960.endStage(d.model);d.exact();
  StrongDefaultRouting1990Test.reset();d=new StrongDiagnostics1982Test.Data();GpuStrongDefault1990.optional1994=true;GpuStrong1960.beginStage(d.model);
  check(d.route().profile==7&&d.process(d.route()),"new stage adopts independently certified candidate");
  GpuStrongDefault1990.optional1994=false;check(d.route().profile<0,"candidate revoked mid-stage chooses CPU without changing GPU profile");
  GpuStrong1960.endStage(d.model);d.exact();
  StrongDefaultRouting1990Test.reset();d=new StrongDiagnostics1982Test.Data();GpuStrongDefault1990.optional1994=true;GpuStrong1960.beginStage(d.model);
  GpuQualification1961.rejectExact1971(d.key(7),"controlled argb rejection");check(d.route().profile<0&&d.process(d.route()),"per-shape candidate rejection remains authoritative");GpuStrong1960.endStage(d.model);d.exact();
  check(GpuNoise1960.leases==0&&GpuNoise1960.active==0,"candidate paths retire ownership");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_jni\":false}");
 }
}
