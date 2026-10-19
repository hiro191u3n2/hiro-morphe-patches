package com.hiro.ulike;
import android.graphics.Bitmap;
import java.util.*;
public final class FinishMemory1991Test {
 static int checks;static final long M=1024L*1024;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static QualityPixels1932.Plan plan(){return QualityPixels1932.plan(null,800,16666666L,0,0,4,4,true,true,1.2f).withHaloSuppression(true);}
 static Bitmap image(int w,int h){int[] p=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++)p[y*w+x]=FinishControl1986.pixel(x,y);return Bitmap.from(w,h,p,Bitmap.Config.ARGB_8888,false);}
 static void clean(){check(FinishStageControl1988.activeSessions==0,"all sessions closed");check(FinishStageControl1988.activeLeases==0,"all leases closed");check(FinishStageControl1988.activePolicies==0,"all policies closed");check(FinishStageControl1988.activeTickets==0,"all tickets drained");}
 public static void main(String[] args){
  long n=3060L*4080,out=4284L*5712;
  for(int rows:new int[]{32,64,128}){long peak=GpuChain1961.finishPeak1991(n,out,4284,rows,2L*4284*32*4,0,12L*4284*40,true,true);
   long old=8L*n+8L*out+48L*4284*rows+16*M;
   check(peak<264*M,"logged shape fits the observed budget without changing reserve");check(old-peak>90*M,"real lifetime separation saves two source images");}
  for(boolean identity:new boolean[]{false,true})for(int rows:new int[]{32,64,128}){
   FinishStageControl1988.reset(0,0);FinishMemoryControl1989.reset(0,0);
   Bitmap input=image(identity?37:80,identity?67:80),expected=image(37,67),output=null;
   try{output=GpuChain1961.runFinish1976(input,0,37,67,plan(),false,null,rows);check(output!=null,"real output graph finishes");
    check(Arrays.equals(output.snapshot(),expected.snapshot()),"full output copied from each completed band");
    List<String> e=FinishStageControl1988.events;check(e.contains("release:0"),"input released after moire");
    if(!identity){check(e.indexOf("release:0")<e.indexOf("allocate:2"),"input ends before output geometry begins");check(e.contains("release:1")&&e.contains("release:4")&&e.contains("release:6")&&e.contains("release:7"),"all dead geometry owners freed");}
    for(int trial=0;trial<2;trial++)check(GpuChain1961.compareFinish1988(input,0,37,67,plan(),false,expected,rows,true,null,false,()->false)==ResidentProof1978.EXACT,"full comparator succeeds on every pixel twice");
   }finally{if(output!=null)output.recycle();input.recycle();expected.recycle();}clean();
  }
  FinishStageControl1988.reset(0,0);FinishMemoryControl1989.reset(0,0);
  Bitmap carriedInput=image(80,80);int[] before=carriedInput.snapshot();GpuNoise1960.Session carried=GpuNoise1960.open();
  for(int slot=0;slot<25;slot++)carried.allocate(slot,1024);
  GpuChain1961.Preflight1981 preflight=GpuChain1961.preflightResident1981(carriedInput,0,37,67,1600);
  check(preflight!=null&&Arrays.equals(preflight.slots,new int[]{24}),"resident reserves only its persistent image slot");
  check(preflight.javaPeak>0&&preflight.bind(carried),"future phase debt admitted before Strong processing");
  Bitmap carriedResult=null;GpuNoise1960.largestRequest1991=0;
  try{carriedResult=GpuChain1961.runResident1976(carriedInput,carried,0,37,67,plan(),false,null);
   check(carriedResult!=null,"resident handoff reaches final output");
   long fullPeak=GpuChain1961.finishPeak1991(6400,37L*67,37,64,0,8,4,true,true);
   check(GpuNoise1960.largestRequest1991==fullPeak-4L*6400,"already retained source24 charged once in carried forecast");
   List<String> events=FinishStageControl1988.events;int sourceEnd=events.indexOf("release:24");
   for(int slot=0;slot<24;slot++)check(events.indexOf("release:"+slot)<sourceEnd,"Strong model bank released before source24 last use");
   check(Arrays.equals(before,carriedInput.snapshot()),"resident fallback source remains byte-for-byte unchanged");
  }finally{preflight.close();if(carriedResult!=null)carriedResult.recycle();carriedInput.recycle();carried.close();}clean();
  for(int release=1;release<=2;release++){
   FinishStageControl1988.reset(0,0);FinishMemoryControl1989.reset(0,0);FinishStageControl1988.releaseFailure=release;
   Bitmap input=image(80,80),output=null;boolean stopped=false;
   try{output=GpuChain1961.runFinish1976(input,0,37,67,plan(),false,null,32);}catch(RuntimeException expected){stopped=true;}
   finally{if(output!=null)output.recycle();input.recycle();}
   check(!stopped&&output==null,"release failure returns unavailable without exposing partial output");clean();
  }
  FinishStageControl1988.reset(22,0);FinishMemoryControl1989.reset(0,0);Bitmap input=image(80,80),expected=image(37,67);
  try{check(GpuChain1961.compareFinish1988(input,0,37,67,plan(),false,expected,32,true,null,false,()->false)==ResidentProof1978.MISMATCH,"last pixel mismatch still fails full proof");}
  finally{input.recycle();expected.recycle();}clean();
  System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"actual_finish_graph\":true,\"full_pixel_comparator\":true,\"native_transport_mocked\":true,\"physical_android_tested\":false}");
 }
}
