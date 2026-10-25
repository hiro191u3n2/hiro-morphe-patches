package com.hiro.ulike;
import java.util.concurrent.CancellationException;
import java.lang.reflect.Field;
import java.util.List;
public final class StrongWorkload19119Test {
 static int assertions,complete,candidateCount,cancellations,qualityNegatives;static long pixels;
 static final GpuQualification1961.Cancellation GO=()->false;
 static void need(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
 static int liveReservations(){try{Field f=GpuNoise1960.class.getDeclaredField("leases1971");f.setAccessible(true);return ((List<?>)f.get(null)).size();}catch(Exception error){throw new AssertionError(error);}}
 static void clean(String why){need(!GpuNoise1960.sessionBusy(),why+" releases owner");need(GpuNoise1960.reservedBytes1971()==0,why+" releases leases");}
 static void check(GpuStrong1960.WorkloadResult19119 r,int count,int tail,boolean candidate){
  need(r!=null&&r.failure==null,"full work success: "+(r==null?"null":r.failure));need(r.nanos>0,"real elapsed recorded");need(r.strips==count,"all strips");need(r.pixels==12484800L,"all exact compared pixels");need(r.maxInflight==2,"two native banks in flight");need(r.timedModelUploads==(candidate?1:0),"candidate model charged once");need(r.tailRows==tail,"real tail");complete++;if(candidate)candidateCount++;pixels+=r.pixels;
 }
 public static void main(String[] args)throws Exception{
  StrongTransport1995Test.store();need(StrongNoise1958.nativeAvailable(),"actual CPU oracle JNI");
  need(GpuNoise1960.supports(GpuStrong1960.program1973(7,0)),"actual old tile8");need(GpuNoiseOptimized19119.prepare(),"actual optional native");
  int recordsBefore=StrongTransport1995Test.preferences.size();
  for(int index=0;index<2;index++){
   GpuStrongDefault1990.Fixture seed=GpuStrongDefault1990.Adaptive1994.benchmark19112(64+index,GO);
   GpuNoise1960.Session occupied=GpuNoise1960.open();need(occupied!=null,"occupied session");need(GpuStrong1960.prepareWorkload19119(seed,GO)==null,"cannot prepare while foreground owns GPU");occupied.close();clean("refused preparation");
   final int[] preparationCalls={0};boolean cancelled=false;
   try{GpuStrong1960.prepareWorkload19119(seed,()->++preparationCalls[0]>100);}catch(CancellationException expected){cancelled=true;}
   need(cancelled,"cancel during expected preparation");cancellations++;clean("cancelled preparation");
   GpuStrong1960.Workload19119 plan=GpuStrong1960.prepareWorkload19119(seed,GO);need(plan!=null,"bounded actual CPU reference plan");clean("ready plan");
   int count=index==0?16:12,tail=index==0?240:244;
   need(plan.count==count&&plan.tailRows==tail,"complete geometry");need(plan.source.length==2&&plan.policy.length==2&&plan.outputs.length==2,"only two mutable banks");
   long stored=0;for(int[] band:plan.expected)stored+=band.length;need(stored==12484800L,"compact complete CPU expected output");
   for(int b=0;b<2;b++)for(int[] e:plan.expected)need(e!=plan.outputs[b][0]&&e!=plan.source[b],"CPU expected has independent ownership");
   try{
    check(plan.measure(false,GO),count,tail,false);clean("baseline whole photo");
    check(plan.measure(true,GO),count,tail,true);clean("candidate whole photo");
    final int[] twoLeases={0};cancelled=false;
    // First observation of both bank leases is just before second submit.
    // Its next cancellation check occurs after submit, before first collect.
    try{plan.measure(true,()->liveReservations()==2&&++twoLeases[0]>=2);}catch(CancellationException expected){cancelled=true;}
    need(cancelled&&twoLeases[0]==2,"cancel after both synchronous submissions");cancellations++;clean("cancelled measured frame");
    int before=plan.expected[0][0];plan.expected[0][0]^=1;
    GpuStrong1960.WorkloadResult19119 bad=plan.measure(true,GO);need(bad!=null&&"argb_mismatch".equals(bad.failure)&&bad.strips==0&&bad.pixels==0,"one wrong ARGB cannot complete benchmark");qualityNegatives++;plan.expected[0][0]=before;clean("ARGB refusal");
    before=plan.confidence[0][0];plan.confidence[0][0]^=1;
    bad=plan.measure(true,GO);need(bad!=null&&"confidence_mismatch".equals(bad.failure)&&bad.strips==0&&bad.pixels==0,"one wrong confidence cannot complete benchmark");qualityNegatives++;plan.confidence[0][0]=before;clean("confidence refusal");
   }finally{plan.close();}
   need(plan.expected==null&&plan.confidence==null&&plan.source==null&&plan.policy==null&&plan.outputs==null,"closed plan releases retained arrays");
   GpuStrong1960.WorkloadResult19119 closed=plan.measure(true,GO);need(closed.nanos==0&&"workload_closed".equals(closed.failure),"closed plan cannot supply speed evidence");clean("closed plan");GpuNoise1960.trimIdle();
  }
  need(StrongTransport1995Test.preferences.size()==recordsBefore,"workload tests never grant runtime certificates");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"complete_workloads\":"+complete+",\"candidate_workloads\":"+candidateCount+",\"compared_pixels\":"+pixels+",\"cancellation_controls\":"+cancellations+",\"quality_negative_controls\":"+qualityNegatives+",\"actual_jni_gles\":true,\"actual_native_cpu\":true,\"all_origins_and_tails\":true,\"two_mutable_banks\":true,\"one_candidate_model_upload\":true,\"closed_and_refused_no_speed_evidence\":true,\"two_submitted_ticket_cancel\":true,\"runtime_certificates_written\":false,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
