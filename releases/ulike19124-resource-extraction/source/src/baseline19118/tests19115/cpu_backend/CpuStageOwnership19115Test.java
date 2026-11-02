package com.hiro.ulike;
import java.util.Arrays;

/** Actual Stage token lifecycle; backend scalar reports are controlled here. */
public final class CpuStageOwnership19115Test {
 static int assertions,scenarios;
 static void check(boolean ok,String label){assertions++;if(!ok)throw new AssertionError(label);}
 static StrongDiagnostics1982Test.Data start(boolean benchmark,boolean oracle)throws Exception{
  scenarios++;StrongDefaultRouting1990Test.reset();GpuResident1976.benchmark=benchmark;GpuResident1976.oracle=oracle;GpuResident1976.cancelled=false;ResidentTiming19111.calls=ResidentTiming19111.cpuCalls=ResidentTiming19111.fault=0;ResidentTiming19111.values=null;ResidentTiming19111.key=null;
  StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();GpuStrong1960.beginStage(d.model);return d;
 }
 static Object begin(StrongDiagnostics1982Test.Data d){Object token=GpuStrong1960.beginCpuStrip19115(d.model);check(token!=null,"CPU oracle receives its actual Stage owner");return token;}
 static void complete(StrongDiagnostics1982Test.Data d,Object owner,int result,int nf,int lf){check(d.process(d.route()),"actual CPU-oracle route completes");GpuStrong1960.endCpuStrip19115(owner,d.model,result,nf,lf);}
 static void noPublish(){check(ResidentTiming19111.cpuCalls==0,"partial or invalid CPU coverage remains unobserved");}
 static void noForeground(){check(ProcessingTiming1947.cpu==0&&ProcessingTiming1947.gpu==0&&ProcessingTiming1947.workCalls==0,"CPU diagnostic stays outside foreground timing counters");check(GpuNoise1960.opens==0&&GpuNoise1960.leases==0&&GpuNoise1960.active==0,"CPU observation does not open GPU or retain leases");}
 public static void main(String[] args)throws Exception{
  for(boolean benchmark:new boolean[]{false,true}){
   StrongDiagnostics1982Test.Data d=start(benchmark,false);check(GpuStrong1960.beginCpuStrip19115(d.model)==null,"ordinary foreground and GPU benchmark are unobserved");GpuStrong1960.endCpuStrip19115(new Object(),d.model,1,0,0);GpuStrong1960.endStage(d.model);noPublish();
  }
  StrongDiagnostics1982Test.Data d=start(false,true);for(int result=1;result<=5;result++){Object owner=begin(d);if(result<5)complete(d,owner,result,result==2?2:0,result==2?3:0);else GpuStrong1960.endCpuStrip19115(owner,d.model,result,0,0);noPublish();}
  GpuStrong1960.endStage(d.model);check(ResidentTiming19111.cpuCalls==1&&Arrays.equals(ResidentTiming19111.values,new long[]{1,1,1,1,1,2,3}),"joined Stage publishes seven actual report categories once");noForeground();GpuStrong1960.endStage(d.model);check(ResidentTiming19111.cpuCalls==1,"second Stage close cannot duplicate backend counts");
  d=start(true,true);Object owner=begin(d);StrongNoise1958.Model other=new StrongNoise1958.Model();check(GpuStrong1960.beginCpuStrip19115(other)==null,"wrong model cannot acquire Stage token");GpuStrong1960.endCpuStrip19115(owner,other,1,0,0);GpuStrong1960.endCpuStrip19115(new Object(),d.model,1,0,0);complete(d,owner,2,1,0);GpuStrong1960.endStage(d.model);check(ResidentTiming19111.cpuCalls==1&&Arrays.equals(ResidentTiming19111.values,new long[]{0,1,0,0,0,1,0}),"wrong owner and model cannot contaminate valid counts");noForeground();
  d=start(false,true);owner=begin(d);complete(d,owner,1,0,0);GpuStrong1960.endCpuStrip19115(owner,d.model,1,0,0);GpuStrong1960.endStage(d.model);noPublish();noForeground();
  d=start(false,true);owner=begin(d);begin(d);complete(d,owner,1,0,0);GpuStrong1960.endCpuStrip19115(owner,d.model,1,0,0);GpuStrong1960.endStage(d.model);noPublish();noForeground();
  d=start(false,true);owner=begin(d);begin(d);complete(d,owner,1,0,0);GpuStrong1960.endStage(d.model);noPublish();noForeground();
  d=start(false,true);check(d.process(d.route()),"unobserved old caller still completes CPU route");GpuStrong1960.endStage(d.model);noPublish();noForeground();
  d=start(false,true);owner=begin(d);complete(d,owner,1,0,0);check(d.process(d.route()),"second legacy caller completes without observer");GpuStrong1960.endStage(d.model);noPublish();noForeground();
  d=start(false,true);owner=begin(d);GpuStrong1960.endStage(d.model);noPublish();GpuStrong1960.endCpuStrip19115(owner,d.model,1,0,0);noPublish();
  GpuStrong1960.beginStage(d.model);Object current=begin(d);GpuStrong1960.endCpuStrip19115(owner,d.model,1,0,0);complete(d,current,3,0,0);GpuStrong1960.endStage(d.model);check(ResidentTiming19111.cpuCalls==1&&Arrays.equals(ResidentTiming19111.values,new long[]{0,0,1,0,0,0,0}),"old closed Stage token cannot report into replacement Stage");noForeground();
  d=start(false,true);owner=begin(d);GpuResident1976.cancelled=true;boolean cancelled=false;try{d.process(d.route());}catch(java.util.concurrent.CancellationException expected){cancelled=true;}check(cancelled&&d.calls==0,"cancelled CPU Stage performs no completed oracle call");GpuStrong1960.endCpuStrip19115(owner,d.model,5,0,0);GpuStrong1960.endStage(d.model);check(ResidentTiming19111.cpuCalls==1&&Arrays.equals(ResidentTiming19111.values,new long[]{0,0,0,0,1,0,0}),"cancelled owner reports only its own incomplete work");noForeground();
  Object canceledOwner=owner;StrongNoise1958.Model canceledModel=d.model;d=start(false,true);Object next=begin(d);GpuStrong1960.endCpuStrip19115(canceledOwner,canceledModel,1,0,0);complete(d,next,1,0,0);GpuStrong1960.endStage(d.model);check(ResidentTiming19111.cpuCalls==1&&Arrays.equals(ResidentTiming19111.values,new long[]{1,0,0,0,0,0,0}),"cancelled Stage cannot contaminate later model or photo");noForeground();
  for(int[] bad:new int[][]{{0,0,0},{6,0,0},{1,-1,0},{1,1000001,0},{1,0,-1}}){d=start(false,true);owner=begin(d);GpuStrong1960.endCpuStrip19115(owner,d.model,bad[0],bad[1],bad[2]);GpuStrong1960.endStage(d.model);noPublish();noForeground();}
  for(int fault=1;fault<=2;fault++){d=start(false,true);owner=begin(d);complete(d,owner,1,0,0);ResidentTiming19111.fault=fault;GpuStrong1960.endStage(d.model);check(ResidentTiming19111.cpuCalls==1&&Arrays.equals(ResidentTiming19111.values,new long[]{1,0,0,0,0,0,0}),"optional sink failure cannot prevent cleanup");noForeground();}
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+",\"actual_gpu_strong_stage\":true,\"stage_token_ownership_preserved\":true,\"foreground_unobserved\":true,\"partial_reports_unobserved\":true,\"controlled_backend_reports\":true,\"actual_jni\":false}");
 }
}
