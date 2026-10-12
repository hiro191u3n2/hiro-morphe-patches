package com.hiro.ulike;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Executes direct coefficient-word input using real production JNI and Mesa. */
public final class DirectTransfer1964Test {
 private static int assertions;
 private static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
 private static ByteBuffer words(int n){ByteBuffer b=ByteBuffer.allocateDirect(n*4).order(ByteOrder.nativeOrder());for(int i=0;i<n;i++)b.putInt(i*4,i%5==0?0x80000000:i%5==1?0x7fc12345:i*0x19abcdef^0x87654321);return b;}
 private static int[] expected(ByteBuffer b){int[] a=new int[b.remaining()/4];for(int i=0;i<a.length;i++)a[i]=b.getInt(i*4);return a;}
 private static void exact(int[] target,int[] source){for(int i=0;i<target.length;i++)check(target[i]==(i>=3&&i<source.length+3?source[i-3]:0x42424242),"raw word and output sentinel "+i);}
 private static void invalid(ByteBuffer b){boolean rejected=false;try{new GpuNoise1960.Batch().uploadDirect(0,b);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid direct range rejected");}
 private static void normal()throws Exception{
  check(GpuNoise1960.available(),"production native available");
  for(int n:new int[]{1,31,63,64,65,211,513,4097}){
   ByteBuffer source=words(n);int[] wanted=expected(source),out=new int[n+6];Arrays.fill(out,0x42424242);
   GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"session");try{
    GpuNoise1960.Batch b=s.newBatch().uploadDirect(0,source);
    source.limit(4); // recorded view retains exact original byte range
    check(s.executeInto(b,0,n,out,3),"coefficient ByteBuffer transfer");exact(out,wanted);
    ByteBuffer readOnly=words(n).asReadOnlyBuffer().order(ByteOrder.nativeOrder());
    Arrays.fill(out,0x42424242);check(s.executeInto(s.newBatch().uploadDirect(1,readOnly),1,n,out,3),"read-only direct input");exact(out,expected(readOnly));
   }finally{s.close();}
  }
  invalid(ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()));invalid(words(2).position(4));invalid(words(2).limit(3));
  invalid(words(1).order(ByteOrder.nativeOrder()==ByteOrder.LITTLE_ENDIAN?ByteOrder.BIG_ENDIAN:ByteOrder.LITTLE_ENDIAN));invalid(words(1).limit(0));
  GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"async session");try{
   ByteBuffer a=words(211),b=words(317);int[] wa=expected(a),wb=expected(b);
   GpuNoise1960.Ticket first=s.submit(s.newBatch().uploadDirect(0,a),0),second=s.submit(s.newBatch().uploadDirect(14,b),1);
   check(first!=null&&second!=null,"two disjoint direct coefficient banks");
   for(int i=0;i<a.capacity();i+=4)a.putInt(i,0);for(int i=0;i<b.capacity();i+=4)b.putInt(i,0);
   int[] oa=new int[217],ob=new int[323];Arrays.fill(oa,0x42424242);Arrays.fill(ob,0x42424242);
   check(s.collectInto(first,0,211,oa,3),"first source consumed before submit returns");check(s.collectInto(second,14,317,ob,3),"second source consumed before submit returns");exact(oa,wa);exact(ob,wb);
  }finally{s.close();}
  // Direct invocation probes native capacity checks independently of Java validation.
  for(Object[] pair:new Object[][]{{ByteBuffer.allocate(16),16L},{words(1),8L},{words(1),3L}}){
   s=GpuNoise1960.open();check(s!=null,"native range session");try{
    Field token=GpuNoise1960.Session.class.getDeclaredField("token");token.setAccessible(true);
    Method m=GpuNoise1960.class.getDeclaredMethod("batchNative",long.class,int[].class,int[].class,long[].class,int[].class,Object[].class,int[][].class,int[][].class,float[][].class,int[].class);m.setAccessible(true);
    boolean ok=(Boolean)m.invoke(null,token.getLong(s),new int[]{6},new int[]{0},new long[]{(Long)pair[1]},new int[]{0},new Object[]{pair[0]},new int[1][],new int[1][],new float[1][],new int[]{0});check(!ok,"native non-direct/short/misaligned range rejected");
   }finally{s.close();}
  }
 }
 private static void fault(){GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"fault session");int[] external={71,72,73,74},candidate=new int[4];try{
  GpuNoise1960.Ticket t=s.submit(s.newBatch().uploadDirect(0,words(4)),0);check(t!=null,"private upload submitted");Native1960Test.setFault(2);
  boolean ok=s.collectInto(t,0,4,candidate,0);if(ok)System.arraycopy(candidate,0,external,0,4);check(!ok,"failed readback unmap rejected");check(Arrays.equals(external,new int[]{71,72,73,74}),"external photograph unchanged");
 }finally{Native1960Test.setFault(0);s.close();}}
 public static void main(String[] args)throws Exception{if(args.length>0)fault();else normal();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"rawWordsEqual\":true,\"rangeValidation\":true}");}
}
