package com.hiro.ulike;
final class GpuStrong1960 {
 private static final class Refused extends RuntimeException {final String reason;Refused(String r){super("resident strong admission: controlled/"+r);reason=r;}}
 static final String[] REASONS={"memory_budget","session_unavailable","input_invalid","capacity_unavailable","storage_limit","lease_limit","native_limit","upload_failed","allocation_failed","stage_unavailable","forecast_unavailable"};
 static RuntimeException fail19110(int mode){
  if(mode==100)return new IllegalStateException("resident strong admission: controlled/memory_budget");
  if(mode==101)return new RuntimeException(new Refused("memory_budget"));
  if(mode==102){Thread.currentThread().interrupt();return new Refused("memory_budget");}
  if(mode==103)return new java.util.concurrent.CancellationException("controlled cancellation");
  if(mode==104)return new Refused("future_unknown");
  return new Refused(REASONS[mode-10]);
 }
 static String residentAdmissionReason19110(RuntimeException e){
  if(QualityPipeline1932.open)throw new AssertionError("GPU owner not closed before typed classification");
  return e instanceof Refused?((Refused)e).reason:null;
 }
}
