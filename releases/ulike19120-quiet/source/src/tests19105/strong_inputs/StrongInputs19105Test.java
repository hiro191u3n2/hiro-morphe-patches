package com.hiro.ulike;
import android.graphics.Bitmap;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

public final class StrongInputs19105Test {
 static int assertions,cases;static Cancel cancel;
 static final class Cancel implements GpuQualification1961.Cancellation {boolean value;public boolean cancelled(){return value;}}
 static void check(boolean value,String label){assertions++;if(!value)throw new AssertionError(label);}
 static void reset(){cases++;ResidentStages19104.clear();cancel=new Cancel();
  GpuNoise1960.memory=true;GpuNoise1960.calls=0;GpuNoise1960.requested=0;
  GpuPolicy1960.calls=0;StrongNoise1958.calls=StrongNoise1958.nativeCalls=StrongNoise1958.javaCalls=0;
  StrongNoise1958.backend=1;StrongNoise1958.javaDelta=-1;StrongNoise1958.fail=StrongNoise1958.oom=StrongNoise1958.mutateSource=StrongNoise1958.cancelAfterNative=false;
  GpuStrong1960.calls=0;GpuStrong1960.delta=-1;GpuStrong1960.fail=GpuStrong1960.mutateU=GpuStrong1960.mutatePolicy=GpuStrong1960.mutateModel=false;
 }
 static Bitmap source(int width,int height){Bitmap b=new Bitmap(width,height);for(int i=0;i<b.pixels.length;i++)b.pixels[i]=0xff685600+(i&127);return b;}
 static void setup(Bitmap source,int row,int x){
  ResidentStages19104.begin(cancel);ResidentStages19104.observe(5,source);
  Bitmap actual=new Bitmap(source.getWidth(),source.getHeight());actual.pixels=source.pixels.clone();actual.pixels[row*source.getWidth()+x]^=1;
  ResidentStages19104.baseline();ResidentStages19104.observe(5,actual);ResidentStages19104.replay();
 }
 static long[] run(Bitmap source){StrongInputs19105.run(source,new QualityPixels1932.Plan(),new StrongNoise1958.Model(source.getWidth(),source.getHeight()),4,true);
  long[] all=ResidentStages19104.finish(ResidentProof1978.MISMATCH);check(ResidentStages19104.valid(all),"valid combined report");return Arrays.copyOfRange(all,13,27);}
 static void idle(){
  for(int phase=0;phase<4;phase++){reset();Bitmap b=source(8,9);
   if(phase>0)ResidentStages19104.begin(cancel);if(phase==2)ResidentStages19104.baseline();if(phase==3)ResidentStages19104.replay();
   StrongInputs19105.run(b,new QualityPixels1932.Plan(),new StrongNoise1958.Model(8,9),4,true);
   check(b.reads==0&&StrongNoise1958.calls==0&&GpuStrong1960.calls==0&&GpuPolicy1960.calls==0&&GpuNoise1960.calls==0,"inactive/reference/baseline/no target is inert");
  }
 }
 static void geometry(){for(int width:new int[]{1,2,7,64,8192})for(int row:new int[]{0,18,39}){
  reset();Bitmap b=source(width,40);setup(b,row,0);int reads=b.reads;GpuStrong1960.delta=0;
  long[] v=run(b);check(v[0]==1&&v[4]==1&&v[3]==0&&v[5]==1&&v[10]==1&&v[11]==1,"controlled one-pixel reproduction");
  check(v[1]==row+1&&v[6]==1&&v[12]==width&&v[13]==width,"coordinates/counts");
  check(b.reads==reads+1&&GpuPolicy1960.calls==1&&StrongNoise1958.nativeCalls==1&&StrongNoise1958.javaCalls==1&&GpuStrong1960.calls==1,"single frozen capture and one run per backend");
  int first=Math.max(0,row-18),last=Math.min(40,row+19);int[] u=GpuStrong1960.lastU;
  check(u[6]==first&&u[1]==last-first&&u[2]==row-first&&u[3]==row-first+1,"halo coordinates");
  check(Arrays.equals(GpuStrong1960.lastSource,Arrays.copyOfRange(b.pixels,first*width,last*width)),"original immutable halo exact");
  check(GpuNoise1960.requested>12L*width*(last-first),"budget covers more than Java buffers");
  StringBuilder text=new StringBuilder();StrongInputs19105.appendSummary(text,v);check(text.toString().contains("認定に不使用")&&text.toString().contains("元区間全入力の一致証明ではありません"),"limits explicit");
 }}
 static void outcomes(){
  reset();Bitmap b=source(8,40);setup(b,20,2);StrongNoise1958.javaDelta=1;GpuStrong1960.delta=2;
  long[] v=run(b);check(v[3]==1&&v[4]==1&&v[5]==2&&v[6]==2,"CPU Java GPU distinct differences");
  reset();b=source(8,40);setup(b,20,2);v=run(b);check(v[3]==0&&v[4]==0&&v[5]==0&&v[6]==0&&v[10]==2,"not reproduced is not baseline match");
  for(int backend:new int[]{3,4,5}){reset();b=source(8,40);setup(b,20,2);StrongNoise1958.backend=backend;v=run(b);check(v[2]==backend,"native fallback distinguished");StringBuilder s=new StringBuilder();StrongInputs19105.appendSummary(s,v);check(s.toString().contains("Java代替"),"fallback rendered");}
  reset();b=source(8,40);setup(b,20,2);b.pixels[20*8]^=1;v=run(b);check(v[11]==2,"changed replay CPU row not stable");
 }
 static void failure(){
  reset();Bitmap b=source(8,40);setup(b,20,2);GpuNoise1960.memory=false;long[] v=run(b);check(v[0]==2&&StrongNoise1958.calls==0&&GpuStrong1960.calls==0&&GpuPolicy1960.calls==0,"memory denied before copied input and execution");
  for(int mode=0;mode<7;mode++){reset();b=source(8,40);setup(b,20,2);
   if(mode==0)StrongNoise1958.fail=true;if(mode==1)StrongNoise1958.oom=true;if(mode==2)GpuStrong1960.fail=true;
   if(mode==3)StrongNoise1958.mutateSource=true;if(mode==4)GpuStrong1960.mutateU=true;if(mode==5)GpuStrong1960.mutatePolicy=true;if(mode==6)GpuStrong1960.mutateModel=true;
   v=run(b);check(v[0]==(mode==1?2:4)&&v[12]==0,"failed or mutated input never complete "+mode);
  }
  reset();b=source(8,40);setup(b,20,2);StrongNoise1958.cancelAfterNative=true;boolean stopped=false;
  try{StrongInputs19105.run(b,new QualityPixels1932.Plan(),new StrongNoise1958.Model(8,40),4,true);}catch(CancellationException expected){stopped=true;}
  long[] all=ResidentStages19104.finish(ResidentProof1978.MISMATCH);v=Arrays.copyOfRange(all,13,27);
  check(stopped&&v[0]==3&&StrongNoise1958.javaCalls==0&&GpuStrong1960.calls==0,"capture cancellation stops later backends");
  check(ResidentStages19104.valid(all),"cancel report retained without changing rejected comparison");
 }
 public static void main(String[] args){idle();geometry();outcomes();failure();System.out.println("RESULT {\"status\":\"passed\",\"cases\":"+cases+",\"assertions\":"+assertions+",\"fixed_inputs_shared_by_three_backends\":true,\"actual_stage_replay_gate\":true,\"foreground_inert\":true,\"bounded_halo_and_memory\":true,\"native_fallback_distinct\":true,\"mutation_not_certified\":true,\"cancellation_propagated\":true,\"numerical_scope\":\"Contract peers; real kernels tested separately\"}");}
}
