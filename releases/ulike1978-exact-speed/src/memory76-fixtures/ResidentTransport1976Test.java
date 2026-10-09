package com.hiro.ulike;
import java.util.*;
import java.lang.reflect.*;
public final class ResidentTransport1976Test {
 static int checks;
 static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 static GpuNoise1960.Session open(){GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"actual session");return s;}
 static void same(int[] a,int[] b,String label){check(Arrays.equals(a,b),label);}
 static long token(GpuNoise1960.Session s)throws Exception {Field f=s.getClass().getDeclaredField("token");f.setAccessible(true);return f.getLong(s);}
 static boolean nativeCall(String name,Class<?>[] types,Object[] args)throws Exception {Method m=GpuNoise1960.class.getDeclaredMethod(name,types);m.setAccessible(true);return (Boolean)m.invoke(null,args);}
 static void normal()throws Exception {
  GpuNoise1960.Session s=open();long[] cap=s.capacity1976();check(cap.length==28&&cap[24]==0&&cap[25]==0&&cap[27]==0,"new slot24 and 28-value snapshot");
  int[] slots=new int[25];long[] bytes=new long[25];for(int i=0;i<25;i++){slots[i]=i;bytes[i]=4;}
  GpuNoise1960.Lease1971 all=s.reserveCapacity1971(slots,bytes,0);check(all!=null&&GpuNoise1960.reservedBytes1971()==26*4096,"all25 capacities and staging budgeted");
  for(int i=0;i<25;i++)check(s.upload(i,new int[]{100+i}),"legacy and new slot upload "+i);
  for(int i=0;i<25;i++)same(s.readInts(i,1),new int[]{100+i},"distinct slot semantic "+i);
  cap=s.capacity1976();long[] legacy=s.capacity1971();check(cap[24]==4096&&cap[27]==25*4096&&legacy.length==27&&legacy[26]==cap[27]&&legacy[24]==cap[25],"slot24 accounted without shifting old diagnostic projection");
  check(GpuNoise1960.reservedBytes1971()==0&&GpuNoise1960.retainedBytes()==26*4096,"materialized slot24 replaces only its own debt");all.close();s.close();
  s=open();GpuNoise1960.Lease1971 frame=s.reserveCapacity1971(new int[]{0,24},new long[]{48,64},0);check(frame!=null,"resident range admission");
  int[] original=new int[16];Arrays.fill(original,-1);int[] source={10,11,12,13,14,15,16,17,18,19,20,21};
  check(s.upload(0,source)&&s.upload(24,original),"initialize bounded frame");check(s.copy1976(0,24,2,3,7),"copy offset words");System.arraycopy(source,2,original,3,7);
  check(s.uploadRange1976(24,11,new int[]{30,31,32,33,34,35},1,4),"CPU fallback range offset upload");System.arraycopy(new int[]{31,32,33,34},0,original,11,4);
  same(s.readInts(24,16),original,"exact offset copy, fallback range and untouched borders");
  check(s.copy1976(0,24,11,15,1),"last legal word");original[15]=21;same(s.readInts(24,16),original,"inclusive last-word bound");
  long retained=GpuNoise1960.retainedBytes();check(s.reserveCapacity1971(new int[]{24},new long[]{GpuNoise1960.MAX_BYTES},0)==null&&GpuNoise1960.retainedBytes()==retained,"new frame cannot bypass512MiB or storage limit");
  frame.close();s.close();check(GpuNoise1960.reservedBytes1971()==0,"resident leases close");
 }
 static void bounds()throws Exception {
  GpuNoise1960.Session s=open();check(s.upload(0,new int[]{1,2,3,4})&&s.upload(24,new int[]{9,9,9,9}),"bounds fixture");
  check(!s.copy1976(-1,24,0,0,1)&&!s.copy1976(0,25,0,0,1)&&!s.copy1976(24,24,0,0,1)&&!s.copy1976(0,24,-1,0,1)&&!s.copy1976(0,24,0,0,0),"invalid Java copy rejected before native");
  check(!s.uploadRange1976(25,0,new int[]{1},0,1)&&!s.uploadRange1976(24,-1,new int[]{1},0,1)&&!s.uploadRange1976(24,0,new int[]{1},1,1),"invalid Java range rejected before native");
  final long t=token(s);final Class<?>[] copyTypes={long.class,int.class,int.class,int.class,int.class,int.class};
  final Class<?>[] uploadTypes={long.class,int.class,int.class,int[].class,int.class,int.class};
  // Native entry points require the GL owner. Run reflection inside that same executor.
  Method owner=GpuNoise1960.class.getDeclaredMethod("owner",java.util.concurrent.Callable.class,Object.class);owner.setAccessible(true);
  Boolean result=(Boolean)owner.invoke(null,new java.util.concurrent.Callable<Boolean>(){public Boolean call()throws Exception{
   check(!nativeCall("copyNative1976",copyTypes,new Object[]{t,0,24,4,0,1}),"source used-size bound");
   check(!nativeCall("copyNative1976",copyTypes,new Object[]{t,0,24,0,4,1}),"destination used-size bound");
   check(!nativeCall("copyNative1976",copyTypes,new Object[]{t,0,24,Integer.MAX_VALUE,0,1}),"overflow-safe source offset");
   check(!nativeCall("copyNative1976",copyTypes,new Object[]{t,0,24,0,Integer.MAX_VALUE,1}),"overflow-safe destination offset");
   check(!nativeCall("copyNative1976",copyTypes,new Object[]{t+1,0,24,0,0,1}),"stale copy token");
   check(!nativeCall("uploadRangeNative1976",uploadTypes,new Object[]{t,24,3,new int[]{1,2},0,2}),"native destination bound beforewrite");
   check(!nativeCall("uploadRangeNative1976",uploadTypes,new Object[]{t,24,0,new int[]{1,2},1,2}),"native Java array bound beforewrite");
   check(!nativeCall("uploadRangeNative1976",uploadTypes,new Object[]{t,24,Integer.MAX_VALUE,new int[]{1},0,1}),"overflow-safe range offset");return Boolean.TRUE;
  }},Boolean.FALSE);check(result,"owner bounds exercise");same(s.readInts(24,4),new int[]{9,9,9,9},"invalid native bounds leave destination untouched");
  check(s.copy1976(0,24,0,0,4),"bounds refusal doesn't poison native state");s.close();
 }
 static void fault(boolean copy)throws Exception {
  GpuNoise1960.Session s=open();check(s.upload(0,new int[]{1,2,3,4})&&s.upload(24,new int[]{9,9,9,9}),"fault fixture");
  Memory1971Test.setFault(copy?7:2);check(copy?!s.copy1976(0,24,0,0,4):!s.uploadRange1976(24,0,new int[]{1,2,3,4},0,4),"late GPU failure never reports successful handoff");check(s.readInts(24,4)==null,"failed private image cannot publish");Memory1971Test.setFault(0);s.close();check(GpuNoise1960.reservedBytes1971()==0,"failed resident ownership retires");GpuNoise1960.Session next=open();next.close();
 }
 static void pending(String mode)throws Exception {
  GpuNoise1960.Session s=open();check(s.allocate(24,16),"frame allocate");GpuNoise1960.Ticket ticket=s.submit(new GpuNoise1960.Batch().upload(0,new int[]{4,3,2,1}),0);check(ticket!=null,"live bank ticket");
  if(mode.equals("interrupt")){Thread.currentThread().interrupt();check(!s.copy1976(0,24,0,0,4),"interrupted caller cannot commit resident frame");check(Thread.currentThread().isInterrupted(),"interrupt preserved");Thread.interrupted();s.close();GpuNoise1960.Session next=open();next.close();return;}
  check(s.copy1976(0,24,0,0,4),"copy ordered after pending producer");
  if(mode.equals("quarantine")){Memory1971Test.setFault(5);check(s.collect(ticket,new int[]{24},new int[]{4})==null,"unknown completion cannot publish resident data");s.close();check(GpuNoise1960.retainedBytes()>0&&GpuNoise1960.open()==null,"resident frame quarantined with outstanding producer");return;}
  int[][] value=s.collect(ticket,new int[]{24},new int[]{4});check(value!=null,"pending producer collected");same(value[0],new int[]{4,3,2,1},"copy ordering preserves producer words");s.close();
 }
 public static void main(String[] args)throws Exception {String mode=args[0];if(mode.equals("normal"))normal();else if(mode.equals("bounds"))bounds();else if(mode.equals("copyfault"))fault(true);else if(mode.equals("uploadfault"))fault(false);else pending(mode);System.out.println("{\"assertions\":"+checks+",\"mode\":\""+mode+"\",\"status\":\"passed\"}");}
}
