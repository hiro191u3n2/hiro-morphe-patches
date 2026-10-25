package com.hiro.ulike;
import android.media.*;
import android.hardware.camera2.*;
import java.lang.reflect.*;
import com.ss.android.vesdk.TECameraVideoRecorder$60;

/** Same controlled input against published .103 source and the real .104 admission. */
public final class SingleAdmission19104Test {
 static int assertions;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static Object fixture(String name,Class<?>[] types,Object...args)throws Exception {Method m=Capture1948Test.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(null,args);}
 public static void main(String[]args)throws Exception {
  boolean legacy=args.length>0&&args[0].equals("103");
  Capture1948Test.Owner owner=new Capture1948Test.Owner();
  TECameraVideoRecorder$60 callback=new TECameraVideoRecorder$60();owner.x0=callback;
  owner.e0=new ImageReader(4080,3060,35);Capture1948Test.ReaderState state=new Capture1948Test.ReaderState(owner.e0);
  Image image=(Image)fixture("image",new Class<?>[]{int.class,int.class,long.class,int.class,boolean.class},4080,3060,1000000000L,7,false);
  CaptureRequest request=(CaptureRequest)fixture("ordinaryRequest",new Class<?>[]{int.class,long.class},800,30000000L);
  TotalCaptureResult result=(TotalCaptureResult)fixture("result",new Class<?>[]{CaptureRequest.class,long.class,int.class,long.class},request,1000000000L,800,30000000L);
  owner.e0.offer(image);Image acquired=BurstCapture1933.acquireYuv(owner,state,owner.e0);
  check(acquired==image,"actual acquisition preserves source identity");
  check(!BurstCapture1933.yuvImage(owner,state,image),"ordinary ownership guard preserves original Q0 route");
  int planesBefore=image.planeReads,analysesBefore=FusionPixels1933.analysisAllocations;
  boolean admitted=BurstCapture1933.beginImage(owner,image,result);
  check(image.closes==0,"admission never closes the original borrowed input");
  if(legacy){
   check(admitted,"published 103 admits obsolete supplemental capture under valid conditions");
   check(image.planeReads>planesBefore,"103 admission copies full input planes before Q0 received");
   check(FusionPixels1933.analysisAllocations>analysesBefore,"103 admission allocates temporal analysis state");
   while(owner.k.one()){}
   check(!owner.d.calls.isEmpty(),"103 submits a supplemental camera request");
   long until=System.nanoTime()+3000000000L;
   while(FusionPixels1933.prepareFrameCalls==0&&System.nanoTime()<until)Thread.sleep(1);
   check(FusionPixels1933.prepareFrameCalls>0,"103 starts temporal frame preparation");
   BurstCapture1933.canceled(owner);
  }else{
   check(!admitted,"104 retains single-image admission policy regardless of valid temporal conditions");
   check(image.planeReads==planesBefore,"104 admission performs zero NV21 plane copies");
   check(FusionPixels1933.analysisAllocations==analysesBefore,"104 admission allocates no temporal analysis state");
   while(owner.k.one()){}
   check(owner.d.calls.isEmpty()&&FusionPixels1933.prepareFrameCalls==0&&ExitJobs185.count==0,"104 has no extra request, preparation worker or capture job");
   check(ShotContext1932.q0Entries==1,"104 preserves exact Q0 entry diagnostic");
   Capture1948Test.originalQ0(owner,image,result);
   check(callback.deliveries==1&&callback.failures==0,"original single-image Q0 still reaches renderer once");
   check(callback.output.width==4080&&callback.output.height==3060&&image.closes==0,"single image keeps full resolution and caller ownership");
   check(owner.d.calls.isEmpty()&&FusionPixels1933.prepareFrameCalls==0,"renderer delivery does not resurrect supplementary admission");
  }
  image.close();check(image.closes==1,"caller closes input exactly once");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"baseline_reproduced\":"+legacy+",\"production_single_image_policy_tested\":"+(!legacy)+"}");
 }
}
