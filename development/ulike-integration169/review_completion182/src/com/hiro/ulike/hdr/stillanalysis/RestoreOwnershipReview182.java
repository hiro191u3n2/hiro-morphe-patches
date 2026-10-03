package com.hiro.ulike.hdr.stillanalysis;
import android.content.Context;import com.ss.android.vesdk.VERecorder;import java.io.*;import java.util.*;
public final class RestoreOwnershipReview182{
 static int checks;static void ok(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
 static final class Owned implements AutoCloseable{int closed;final int[] pixels={1,4,9,16};Throwable fail;public void close()throws Exception{closed++;Arrays.fill(pixels,0);if(fail instanceof Exception)throw(Exception)fail;if(fail instanceof Error)throw(Error)fail;}}
 public static void main(String[]args)throws Exception{
  String mode=args[0];RecorderAdmission.installed(RecorderAdmission.HOOK_CONTRACT);VERecorder recorder=new VERecorder();Owned value=new Owned();IOException primary=new IOException("restore");AssertionError closeError=new AssertionError("close");
  if(mode.equals("close-error"))value.fail=closeError;if(mode.equals("close-interrupt"))value.fail=new InterruptedException("close");if(mode.equals("same-error"))value.fail=primary;
  Throwable seen=null;Owned returned=null;
  try{returned=PausedStockPreview.run(new Context(),recorder,recorder.b,50,(r,t)->{if(!mode.equals("success")&&!mode.equals("unjoined-native"))throw primary;},lease->{lease.requireInitializedAndNoOtherRecorder();if(mode.equals("unjoined-native")){Object nativeStill=new Object();RecorderAdmission.nativeInitialized(RecorderAdmission.beforeNativeInit(nativeStill),44,0);}return value;});}catch(Exception|Error e){seen=e;}
  if(mode.equals("success")){ok(returned==value&&seen==null&&value.closed==0,"success transfers ownership");ok(Arrays.equals(value.pixels,new int[]{1,4,9,16}),"success preserves pixels");value.close();ok(value.closed==1,"caller closesonce");}
  else{ok(returned==null&&seen!=null,"failure doesnotdeliver");ok(value.closed==1,"undelivered exactlyoneclose");ok(Arrays.equals(value.pixels,new int[4]),"ownedpixels wiped");if(mode.equals("unjoined-native"))ok(recorder.starts==0,"unjoinednative prevents restart");else ok(seen==primary,"primaryidentity retained");
   if(mode.equals("close-error"))ok(seen.getSuppressed().length==1&&seen.getSuppressed()[0]==closeError,"cleanupError suppressed");if(mode.equals("same-error"))ok(seen.getSuppressed().length==0,"no selfsuppression");if(mode.equals("close-interrupt"))ok(Thread.currentThread().isInterrupted()&&seen.getSuppressed()[0]==value.fail,"cleanupinterrupt retained");
   try{RecorderAdmission.beforeApplicationLifecycle(recorder,"restart");throw new AssertionError("quarantine bypass");}catch(IllegalStateException expected){checks++;}
  }
  Thread.interrupted();System.out.println("PASS RestoreOwnershipReview182 "+mode+" "+checks+" bounded checks; host lifecyclefixtures only");
 }
}
