package com.hiro.ulike;
import java.nio.*;
import java.util.*;
public final class ColourGate1976Test {
 static {System.loadLibrary("colourcache1976");}static int assertions;static native void mode(int value);static native long calls(boolean cached);
 static void check(boolean b,String s){assertions++;if(!b)throw new AssertionError(s);}
 static int w=33,h=43;static int[] input(){int[] a=new int[w*h];Random r=new Random(788);for(int i=0;i<a.length;i++)a[i]=0xff000000|r.nextInt(0xffffff);return a;}
 static float[] model(){float[]m=new float[91];Arrays.fill(m,1.6f);m[0]=6;m[1]=9;m[2]=114;return m;}
 // These helpers deliberately own detached inputs for verifier controls.
 // Normal production entry points are separately checked not to capture.
 static Object detachOwned(int kind,int[] input,int[] u,float[] model,ByteBuffer cache){
  if(Thread.currentThread().isInterrupted()||GpuQualification1961.background())return null;
  try{java.lang.reflect.Method key=ColourCache1976.class.getDeclaredMethod("key",int.class,int[].class,boolean.class);key.setAccessible(true);
   String value=(String)key.invoke(null,kind,u,false);
   java.lang.reflect.Method method=ColourCache1976.class.getDeclaredMethod("detach",String.class,int.class,int[].class,int[].class,float[].class,int[].class,ByteBuffer.class);method.setAccessible(true);
   return method.invoke(null,value,kind,input,u,model,null,cache);
  }catch(Exception failure){throw new AssertionError(failure);}
 }
 static void submitOwned(Object pending){if(pending==null)return;try{java.lang.reflect.Method submit=pending.getClass().getDeclaredMethod("submit");submit.setAccessible(true);submit.invoke(pending);}catch(Exception failure){throw new AssertionError(failure);}}
 static void closeOwned(Object pending){if(pending==null)return;try{java.lang.reflect.Field f=pending.getClass().getDeclaredField("probe");f.setAccessible(true);((GpuQualification1961.Probe)f.get(pending)).close();}catch(Exception failure){throw new AssertionError(failure);}}
 static int[] shape(int width,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,int mh,int columns,int mr,int pw,int ph){return new int[]{width,rows,begin,end,vb,ve,origin,noise,shadows?1:0,width,mh,columns,mr,pw,ph};}
 static boolean cpu(int[] source,int[]out,float[]m){boolean okay=ColourCache1976.cpu(source,out,w,h,0,h,0,h,0,4,true,w,h,1,1,w,h,m,null);if(okay)submitOwned(detachOwned(0,source,shape(w,h,0,h,0,h,0,4,true,h,1,1,w,h),m,null));return okay;}
 static float[] prepareOwned(int[] input,int width,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,int mh,int columns,int mr,int pw,int ph,float[] model){
  float[] result=ColourCache1976.prepare(input,width,rows,begin,end,vb,ve,origin,noise,shadows,mh,columns,mr,pw,ph,model);
  if(result!=null)submitOwned(detachOwned(1,input,shape(width,rows,begin,end,vb,ve,origin,noise,shadows,mh,columns,mr,pw,ph),model,null));return result;
 }
 static boolean directOwned(ByteBuffer cache,ByteBuffer output,int[] input,int width,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,int mh,int columns,int mr,int pw,int ph,float[] model){
  Object pending=detachOwned(2,input,shape(width,rows,begin,end,vb,ve,origin,noise,shadows,mh,columns,mr,pw,ph),model,cache);
  boolean okay=false;try{okay=ColourCache1976.direct(cache,output,input,width,rows,begin,end,vb,ve,origin,noise,shadows,mh,columns,mr,pw,ph,model);if(okay)submitOwned(pending);return okay;}finally{if(!okay)closeOwned(pending);}
 }
 static void normalCaptureOff(){
  reset();int[] input=input(),output=new int[input.length];float[] model=model();
  check(ColourCache1976.cpu(input,output,w,h,0,h,0,h,0,4,true,w,h,1,1,w,h,model,null)&&GpuQualification1961.pending==null,"normal CPU route creates no proof");
  check(ColourCache1976.prepare(input,w,h,0,h,0,h,0,4,true,h,1,1,w,h,model)!=null&&GpuQualification1961.pending==null,"normal preparation creates no proof");
  ByteBuffer cache=ByteBuffer.allocateDirect(80+91*4+((w+3)/4+1)*4*1260).order(ByteOrder.nativeOrder()),record=ByteBuffer.allocateDirect(recordBytes(0,h)).order(ByteOrder.nativeOrder());
  check(ColourCache1976.direct(cache,record,input,w,h,0,h,0,h,0,4,true,h,1,1,w,h,model)&&GpuQualification1961.pending==null,"normal direct route creates no proof");
  check(GpuQualification1961.pendingBytes==0,"normal colour routes retain zero proof bytes");
 }

 static void reset(){GpuQualification1961.reset();mode(0);calls(false);calls(true);}
 static byte[] bytes(ByteBuffer buffer){byte[]a=new byte[buffer.capacity()];ByteBuffer v=buffer.duplicate();v.clear();v.get(a);return a;}
 static int recordBytes(int begin,int end){int first=Math.floorDiv(begin-7,4)*4;return ((w+3)/4+1)*((end-first+3)/4)*248*4;}
 static Object field(String name){try{java.lang.reflect.Field f=GpuQualification1961.pending.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(GpuQualification1961.pending);}catch(Exception e){throw new AssertionError(e);}}
 public static void main(String[] args) {
  normalCaptureOff();
  reset();int[] source=input(),out=new int[source.length];float[]m=model();check(cpu(source,out,m),"unqualified foreground succeeds");check(calls(false)==1&&calls(true)==0,"unqualified foreground uses original source loader");check(GpuQualification1961.pending!=null,"probe scheduled");
  int[] reference=out.clone();check(field("input")!=source&&Arrays.equals((int[])field("input"),source),"detached source snapshot");check(field("model")!=m&&Arrays.equals((float[])field("model"),m),"detached model snapshot");source[2]^=255;m[0]=31;mode(1);GpuQualification1961.run(0);check(GpuQualification1961.qualified==1,"two measured exact cached wins qualify");check(calls(false)==3&&calls(true)==3,"warmup plus two trials both algorithms");
  source=input();m=model();calls(false);calls(true);check(cpu(source,out,m),"qualified foreground succeeds");check(calls(true)==1&&calls(false)==0,"verified cache selected");check(Arrays.equals(out,reference),"qualified exact original output");check(GpuQualification1961.pending==null,"accepted key does not retain more photos");
  reset();cpu(input(),out,model());mode(2);GpuQualification1961.run(0);check(GpuQualification1961.qualified==0&&GpuQualification1961.slow.size()==1,"slower cache refused");calls(false);calls(true);cpu(input(),out,model());check(calls(false)==1&&calls(true)==0,"speed rejection preserves original algorithm");
  reset();cpu(input(),out,model());mode(3);GpuQualification1961.run(0);check(GpuQualification1961.qualified==0&&GpuQualification1961.exact.size()==1,"single pixel mismatch permanently refused");
  reset();cpu(input(),out,model());mode(1);GpuQualification1961.run(2);check(GpuQualification1961.qualified==0&&GpuQualification1961.slow.isEmpty()&&GpuQualification1961.exact.isEmpty(),"new capture cancellation leaves no certificate/rejection");check(GpuQualification1961.pendingBytes==0,"cancelled snapshots released");
  reset();GpuNoise1960.fit=false;cpu(input(),out,model());check(GpuQualification1961.pending==null&&GpuQualification1961.qualified==0,"memory shortage retains old foreground without detached allocation");
  reset();float[] first=prepareOwned(input(),w,h,0,h,0,h,0,4,true,h,1,1,w,h,model());check(first!=null,"legacy residual preparation");mode(1);GpuQualification1961.run(0);check(GpuQualification1961.qualified==1,"full residual record cache speed proof");float[] second=prepareOwned(input(),w,h,0,h,0,h,0,4,true,h,1,1,w,h,model());check(Arrays.equals(first,second),"qualified residual exact");
  reset();int cacheBytes=80+91*4+((w+3)/4+1)*4*1260,recordBytes=((w+3)/4+1)*((h+8+3)/4)*248*4;ByteBuffer cache=ByteBuffer.allocateDirect(cacheBytes).order(ByteOrder.nativeOrder()),record=ByteBuffer.allocateDirect(recordBytes).order(ByteOrder.nativeOrder());
  byte[] preCall=bytes(cache);check(directOwned(cache,record,input(),w,h,0,h,0,h,0,4,true,h,1,1,w,h,model()),"direct foreground");check(Arrays.equals(preCall,bytes((ByteBuffer)field("cache"))),"cold proof owns exact pre-call tail state");check(!Arrays.equals(preCall,bytes(cache)),"native direct call actually changed the live tail");byte[] snapshot=new byte[recordBytes];record.duplicate().get(snapshot);mode(1);GpuQualification1961.run(0);check(GpuQualification1961.qualified==1,"direct path full exact timing proof");check(directOwned(cache,record,input(),w,h,0,h,0,h,0,4,true,h,1,1,w,h,model()),"qualified direct foreground");byte[] actual=new byte[recordBytes];record.duplicate().get(actual);check(Arrays.equals(snapshot,actual),"direct qualified output exact");
  reset();cache=ByteBuffer.allocateDirect(cacheBytes).order(ByteOrder.nativeOrder());record=ByteBuffer.allocateDirect(recordBytes(0,23)).order(ByteOrder.nativeOrder());
  check(directOwned(cache,record,input(),w,h,0,23,0,h,0,4,true,h,1,1,w,h,model()),"first warm-cache band");reset();preCall=bytes(cache);record=ByteBuffer.allocateDirect(recordBytes(23,h)).order(ByteOrder.nativeOrder());
  check(directOwned(cache,record,input(),w,h,23,h,0,h,0,4,true,h,1,1,w,h,model()),"second warm-cache band");check(Arrays.equals(preCall,bytes((ByteBuffer)field("cache"))),"warm proof owns previous band's pre-call tail");check(!Arrays.equals(preCall,bytes(cache)),"second band advances live tail");mode(1);GpuQualification1961.run(0);check(GpuQualification1961.qualified==1,"warm pre-state full comparison qualifies");
  reset();Thread.currentThread().interrupt();cpu(input(),out,model());check(GpuQualification1961.pending==null,"interrupted foreground never schedules");Thread.interrupted();
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"production_java_and_jni\":true,\"explicit_test_owned_inputs\":true,\"normal_foreground_capture_disabled\":true,\"default_original_loader\":true,\"exact2_and_five_percent_gate\":true,\"actual_mode_branches\":true,\"idle_queue_fixture\":true,\"cancellation_and_memory_refusal\":true,\"physical_android_tested\":false}");
 }
}
