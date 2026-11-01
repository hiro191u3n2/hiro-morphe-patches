package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import android.graphics.Bitmap;

public final class StageDiagnostic19104Test {
 static int checks,cases;
 static final class Cancel implements GpuQualification1961.Cancellation {boolean value;public boolean cancelled(){return value;}}
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static void scenario(){cases++;ResidentStages19104.clear();GpuNoise1960.reset();Thread.interrupted();}
 static int state(long[] row,int stage){return (int)((row[0]>>>(stage*2))&3);}
 static int[] data(int count){int[] a=new int[count];for(int i=0;i<count;i++)a[i]=0xff123400+i*17;return a;}
 static void begin(){ResidentStages19104.begin(new Cancel());}
 static void replayWords(int stage,int[] values,int width){boolean stopped=false;try{ResidentStages19104.observeWords(stage,values,width);}catch(CancellationException expected){stopped=true;}check(stopped&&ResidentStages19104.replayComplete(),"CPU replay stops exactly after target row observation");}
 static void replayObject(int stage,Object value){boolean stopped=false;try{ResidentStages19104.observe(stage,value);}catch(CancellationException expected){stopped=true;}check(stopped&&ResidentStages19104.replayComplete(),"CPU replay stops after target nested observation");}
 static void stages(){
  for(int stage=0;stage<13;stage++){
   scenario();begin();int[] reference=data(12);ResidentStages19104.observeWords(stage,reference,4);ResidentStages19104.baseline();ResidentStages19104.observeWords(stage,reference.clone(),4);
   long[] row=ResidentStages19104.finish(ResidentProof1978.EXACT);
   check(state(row,stage)==1&&state(row,13)==1&&row[1]==0&&row[7]==0,"identical stage retains independent final result "+stage);
   check(ResidentStages19104.finish(ResidentProof1978.EXACT)==null,"finish clears context "+stage);
  }
  for(int stage=0;stage<13;stage++){
   scenario();begin();int[] reference=data(12),actual=reference.clone();actual[6]^=0x80000001;
   ResidentStages19104.observeWords(stage,reference,4);ResidentStages19104.baseline();ResidentStages19104.observeWords(stage,actual,4);
   check(ResidentStages19104.needsReplay(),"stage mismatch requests CPU replay "+stage);ResidentStages19104.replay();replayWords(stage,reference,4);
   long[] row=ResidentStages19104.finish(ResidentProof1978.MISMATCH);
   check(state(row,stage)==2&&row[1]==stage+1&&row[2]==2&&row[3]==3,"first exact word coordinates "+stage);
   check(row[4]==(reference[6]&0xffffffffL)&&row[5]==(actual[6]&0xffffffffL)&&row[7]==1,"full unsigned word evidence only after stable CPU replay "+stage);
   check(state(row,13)==2&&row[8]==12&&row[9]==8,"independent final mismatch and bounded comparison progress "+stage);
  }
 }
 static void replayAndShapes(){
  scenario();begin();int[] original=data(12),gpu=original.clone(),unstable=original.clone();gpu[7]++;unstable[5]++;
  ResidentStages19104.observeWords(5,original,4);ResidentStages19104.baseline();ResidentStages19104.observeWords(5,gpu,4);ResidentStages19104.replay();replayWords(5,unstable,4);
  long[] row=ResidentStages19104.finish(-1);check(row[7]==0&&row[11]==1&&row[3]==0,"unstable CPU replay never supplies pixel attribution");check(state(row,13)==2,"diagnostic failure cannot erase original mismatch");
  scenario();begin();ResidentStages19104.observeWords(5,original,4);ResidentStages19104.baseline();ResidentStages19104.observeWords(5,data(16),4);check(!ResidentStages19104.needsReplay(),"shape mismatch has no fabricated row to replay");row=ResidentStages19104.finish(-1);check(row[1]==6&&row[8]==12&&row[9]==16&&row[2]==0&&row[3]==0,"shape counts recorded separately from word mismatch");
  scenario();begin();ResidentStages19104.baseline();ResidentStages19104.observeWords(5,original,4);row=ResidentStages19104.finish(0);check(state(row,5)==3&&state(row,13)==3&&row[1]==0,"missing reference remains unobserved/incomplete");
  scenario();begin();ResidentStages19104.observeWords(6,original,4);ResidentStages19104.observeWords(5,original,4);ResidentStages19104.baseline();ResidentStages19104.observeWords(5,gpu,4);ResidentStages19104.observeWords(6,gpu,4);row=ResidentStages19104.finish(-1);check(row[1]==6&&state(row,6)==0,"downstream stage cannot overwrite first mismatch");
  scenario();begin();ResidentStages19104.observeWords(5,original,4);ResidentStages19104.baseline();ResidentStages19104.observeWords(5,gpu,4);ResidentStages19104.replay();ResidentStages19104.observeWords(6,original,4);row=ResidentStages19104.finish(-1);check(row[7]==0&&row[3]==0,"wrong stage replay cannot certify mismatch");
 }
 static GpuNoise1960.Session source(int width,int height){GpuNoise1960.Session session=new GpuNoise1960.Session();session.values.put(4,data(width*height));return session;}
 static void gpu(){
  scenario();GpuNoise1960.Session session=source(7,70);ResidentStages19104.observeGpu(5,session,4,7,70);check(session.reserve==0&&session.dispatches==0,"foreground outside diagnostic does no GPU work");
  scenario();begin();session=source(7,70);ResidentStages19104.observeGpu(5,session,4,7,70);check(session.reserve==0,"reference role cannot touch GPU");ResidentStages19104.replay();ResidentStages19104.observeGpu(5,session,4,7,70);check(session.reserve==0,"CPU replay cannot touch GPU");ResidentStages19104.clear();
  scenario();begin();session=source(7,70);int[] original=session.values.get(4).clone();ResidentStages19104.observeWords(5,original,7);ResidentStages19104.baseline();ResidentStages19104.observeGpu(5,session,4,7,70);long[] row=ResidentStages19104.finish(1);
  check(state(row,5)==1&&session.dispatches==3&&session.reads==3,"GPU exact result over three batches");check(session.batchStarts.equals(Arrays.asList(0,32,64))&&session.batchRows.equals(Arrays.asList(32,32,6)),"mode0 exact crop handles final short batch");check(Arrays.equals(original,session.values.get(4))&&session.closes==1&&session.releases==1&&session.liveLeases==0&&!session.values.containsKey(22)&&!session.values.containsKey(23),"source unchanged and all scratch ownership released");
  scenario();begin();session=source(7,70);original=session.values.get(4).clone();ResidentStages19104.observeWords(5,original,7);session.values.get(4)[35*7+2]^=0x100;ResidentStages19104.baseline();ResidentStages19104.observeGpu(5,session,4,7,70);check(session.dispatches==2&&ResidentStages19104.needsReplay(),"GPU stops at first different batch row");ResidentStages19104.replay();replayWords(5,original,7);row=ResidentStages19104.finish(-1);check(row[1]==6&&row[2]==36&&row[3]==3&&row[7]==1&&session.closes==1,"crop mismatch localizes exact global row and word");
  for(int mode=0;mode<9;mode++){
   scenario();begin();session=source(7,70);ResidentStages19104.observeWords(5,session.values.get(4),7);ResidentStages19104.baseline();
   if(mode==0)session.deny=true;if(mode==1)session.invalidate=true;if(mode==2)session.failAllocate=true;if(mode==3)session.failDispatch=true;if(mode==4)session.nullRead=true;if(mode==5)session.shortRead=true;if(mode==6)session.throwRead=true;if(mode==7)GpuNoise1960.supported=false;if(mode==8)session.failRelease=true;
   ResidentStages19104.observeGpu(5,session,4,7,70);row=ResidentStages19104.finish(-1);
   check(state(row,5)==(mode==8?1:3)&&state(row,13)==2&&row[7]==0,"GPU failure keeps independent original mismatch intact "+mode);if(mode==8)check(row[11]==1,"failed scratch release is explicit observer failure");
   check(session.closes==(mode==0||mode==7?0:1)&&session.releases==session.closes&&session.liveLeases==0,"every acquired GPU lease released on failure "+mode);
  }
  scenario();Cancel cancel=new Cancel();ResidentStages19104.begin(cancel);session=source(7,70);ResidentStages19104.observeWords(5,session.values.get(4),7);ResidentStages19104.baseline();cancel.value=true;
  boolean stopped=false;try{ResidentStages19104.observeGpu(5,session,4,7,70);}catch(CancellationException expected){stopped=true;}finally{ResidentStages19104.clear();}
  check(stopped&&session.reserve==0,"cancel before GPU reservation starts no work");
  scenario();cancel=new Cancel();ResidentStages19104.begin(cancel);session=source(7,70);ResidentStages19104.observeWords(5,session.values.get(4),7);ResidentStages19104.baseline();session.cancelOnRead=true;session.cancellation=cancel;stopped=false;
  try{ResidentStages19104.observeGpu(5,session,4,7,70);}catch(CancellationException expected){stopped=true;}finally{ResidentStages19104.clear();}
  check(stopped&&session.dispatches==1&&session.closes==1&&session.releases==1,"cancel during GPU read releases leases before propagation");
 }
 static void bounds()throws Exception {
  scenario();begin();ResidentStages19104.observeWords(0,new int[65537],1);long[] row=ResidentStages19104.finish(0);check(row[11]==1&&row[1]==0,"2MiB row-hash cap rejects excessive signature rows");
  scenario();begin();ResidentStages19104.observeWords(0,new int[8193],8193);row=ResidentStages19104.finish(0);check(row[11]==1,"row scratch cap rejects width greater than8192");
  scenario();begin();GpuNoise1960.memory=false;ResidentStages19104.observeWords(0,data(4),4);row=ResidentStages19104.finish(0);check(row[11]==1,"runtime workspace refusal keeps diagnostic bounded");
  scenario();begin();Field active=ResidentStages19104.class.getDeclaredField("ACTIVE");active.setAccessible(true);Object context=((ThreadLocal<?>)active.get(null)).get();Field deadline=context.getClass().getDeclaredField("deadline");deadline.setAccessible(true);deadline.setLong(context,System.nanoTime()-1);ResidentStages19104.observeWords(0,data(4),4);row=ResidentStages19104.finish(0);check(row[11]==1,"expired observer deadline fails closed");
  scenario();begin();ResidentStages19104.observeWords(0,new int[32768],1);GpuNoise1960.memory=false;ResidentStages19104.observeWords(0,new int[32768],1);ResidentStages19104.observeWords(0,new int[32768],1);GpuNoise1960.memory=true;ResidentStages19104.observeWords(1,new int[65536],1);ResidentStages19104.baseline();ResidentStages19104.observeWords(1,new int[65536],1);row=ResidentStages19104.finish(0);check(state(row,1)==3,"failed replacement never uncharges retained reference or bypasses total2MiB cap");
  scenario();begin();Thread.currentThread().interrupt();boolean stopped=false;try{ResidentStages19104.observeWords(0,data(4),4);}catch(CancellationException expected){stopped=true;}finally{Thread.interrupted();ResidentStages19104.clear();}check(stopped,"interruption propagates cancellation");
  scenario();begin();ResidentStages19104.observeWords(0,data(4),4);final boolean[] child={true};Thread t=new Thread(()->child[0]=ResidentStages19104.finish(1)!=null);t.start();t.join();check(!child[0]&&ResidentStages19104.finish(1)!=null,"diagnostic context remains thread-local");
 }
 static void replayBudgetAndInvalid()throws Exception {
  scenario();check(!ResidentStages19104.canReplay(1000),"no replay budget outside diagnostic");begin();ResidentStages19104.observeWords(0,data(12),4);check(ResidentStages19104.canReplay(1000),"bounded replay budget includes retained signatures");check(GpuNoise1960.lastWorkspace==1000+96+16L*8192+131072L,"replay memory charge accounts image, hashes, scratch");ResidentStages19104.clear();
  scenario();begin();GpuNoise1960.memory=false;check(!ResidentStages19104.canReplay(1000),"replay rejected by workspace gate");check(ResidentStages19104.finish(-1)[11]==1,"replay workspace refusal recorded without changing final proof");
  scenario();begin();check(!ResidentStages19104.canReplay(-1),"negative replay image size rejected");ResidentStages19104.clear();
  scenario();begin();check(!ResidentStages19104.canReplay(Long.MAX_VALUE),"overflow replay image size rejected");ResidentStages19104.clear();
  scenario();begin();ResidentStages19104.baseline();GpuNoise1960.Session session=source(4,3);for(int stage:new int[]{-1,13,14,Integer.MAX_VALUE}){ResidentStages19104.observeWords(stage,data(12),4);ResidentStages19104.observeGpu(stage,session,4,4,3);ResidentStages19104.observe(stage,data(12));}long[] row=ResidentStages19104.finish(0);check(session.reserve==0&&row[11]==0&&row[1]==0,"out-of-range stage IDs are harmless and touch no GPU");
  scenario();begin();int[] original=data(12),changed=original.clone();changed[5]++;ResidentStages19104.observeWords(5,original,4);ResidentStages19104.baseline();ResidentStages19104.observeWords(5,changed,4);ResidentStages19104.replay();Field active=ResidentStages19104.class.getDeclaredField("ACTIVE");active.setAccessible(true);Object context=((ThreadLocal<?>)active.get(null)).get();Field deadline=context.getClass().getDeclaredField("deadline");deadline.setAccessible(true);deadline.setLong(context,System.nanoTime()-1);boolean stopped=false;try{ResidentStages19104.observe(0,original);}catch(CancellationException expected){stopped=true;}check(stopped&&!ResidentStages19104.replayComplete(),"expired CPU replay stops at earlier stage boundary without completion claim");row=ResidentStages19104.finish(-1);check(row[11]==1&&row[7]==0&&state(row,13)==2,"replay deadline cannot erase strict mismatch or invent stable row");
  scenario();begin();ResidentStages19104.failed();row=ResidentStages19104.finish(-1);check(row[11]==1&&state(row,13)==2,"explicit observer failure preserves independent final mismatch");
 }
 static void structures(){
  scenario();begin();Bitmap image=new Bitmap(5,3);image.pixels=data(15);ResidentStages19104.observe(5,image);ResidentStages19104.baseline();ResidentStages19104.observe(5,image);long[] row=ResidentStages19104.finish(1);check(state(row,5)==1,"Bitmap input scans exact full rows");
  scenario();begin();StrongNoise1958.Model model=new StrongNoise1958.Model(8,new int[][]{data(8),data(4),data(2)},new float[]{.5f,Float.intBitsToFloat(0x7fc00001)},new float[]{-0f});ResidentStages19104.observe(1,model);ResidentStages19104.baseline();StrongNoise1958.Model changed=new StrongNoise1958.Model(8,new int[][]{data(8),data(4),data(2)},new float[]{.5f,Float.intBitsToFloat(0x7fc00001)},new float[]{0f});ResidentStages19104.observe(1,changed);check(ResidentStages19104.needsReplay(),"model evidence float raw bits preserve signed zero difference");ResidentStages19104.replay();replayObject(1,model);row=ResidentStages19104.finish(-1);check(row[1]==5&&row[3]==3&&row[6]==2&&row[7]==1,"model map/evidence earliest stage localized");
  scenario();begin();QualityPixels1932.Plan plan=new QualityPixels1932.Plan();plan.faceRegions=new QualityPixels1932.Mask(new byte[]{1,2,3},new byte[]{4,5});ResidentStages19104.observe(7,plan);ResidentStages19104.baseline();QualityPixels1932.Plan changedPlan=new QualityPixels1932.Plan();changedPlan.faceRegions=new QualityPixels1932.Mask(new byte[]{1,2,3},new byte[]{4,6});ResidentStages19104.observe(7,changedPlan);ResidentStages19104.replay();replayObject(7,plan);row=ResidentStages19104.finish(-1);check(state(row,7)==1&&row[1]==9&&row[7]==1&&row[3]>0,"source plan primitive equality distinguishes replay-verified face-mask difference");
  scenario();begin();plan=new QualityPixels1932.Plan();ResidentStages19104.observe(11,plan);ResidentStages19104.baseline();changedPlan=new QualityPixels1932.Plan();changedPlan.localNoise.amount=2f;ResidentStages19104.observe(11,changedPlan);ResidentStages19104.replay();replayObject(11,plan);row=ResidentStages19104.finish(-1);check(state(row,11)==1&&row[1]==13&&row[7]==1&&row[3]>0,"finish plan and output noise are replay-verified separately");
 }
 public static void main(String[]args)throws Exception {
  stages();replayAndShapes();gpu();bounds();replayBudgetAndInvalid();structures();
  check(cases>=20,"meaningful diagnostic case floor");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"cases\":"+cases+",\"stage_attribution_exact\":true,\"cpu_replay_instability_not_gpu_blame\":true,\"foreground_gpu_untouched\":true,\"gpu_crop_exact\":true,\"gpu_lease_cleanup\":true,\"diagnostic_bounded\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
