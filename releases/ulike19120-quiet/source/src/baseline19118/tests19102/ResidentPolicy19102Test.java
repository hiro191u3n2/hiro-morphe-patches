package com.hiro.ulike;
import java.lang.reflect.Field;

/** Invokes compiled production helpers. Does not simulate numerical GPU results. */
public final class ResidentPolicy19102Test {
 static int assertions;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 @SuppressWarnings("unchecked") static <T> ThreadLocal<T> local(String name)throws Exception{
  Field field=GpuResident1976.class.getDeclaredField(name);field.setAccessible(true);return (ThreadLocal<T>)field.get(null);
 }
 static void budget(){
  int[] widths={1,17,640,1920,3060,4080,5712,8192};
  int[] heights={1,2,63,127,255,256,257,3060,4080,8192};
  for(int width:widths)for(int height:heights){
   long upload=GpuStrong1960.residentUploadBytes19102(width,height);
   check(upload==4L*width*Math.min(height,256),"exact maximum production CPU strip");
   check(upload>0,"positive transfer budget");
   check(upload<=4L*width*height,"budget does not exceed resident capacity");
  }
  check(GpuStrong1960.residentUploadBytes19102(4080,3060)==4177920L,"landscape strip");
  check(GpuStrong1960.residentUploadBytes19102(3060,4080)==3133440L,"portrait strip");
  int[][] invalid={{0,1},{1,0},{-1,2},{2,-1},{Integer.MAX_VALUE,2},{65536,65536}};
  for(int[] size:invalid){boolean failed=false;try{GpuStrong1960.residentUploadBytes19102(size[0],size[1]);}catch(IllegalArgumentException expected){failed=true;}check(failed,"bad dimensions fail closed");}
 }
 static void permission()throws Exception{
  final ThreadLocal<Boolean> benchmark=local("BENCHMARK"),cpu=local("CPU");
  final ThreadLocal<GpuQualification1961.Cancellation> owner=local("CANCELLATION");
  try{
   for(int b=0;b<2;b++)for(int c=0;c<2;c++)for(int o=0;o<2;o++)for(int x=0;x<2;x++){
    final boolean cancelled=x==1;benchmark.set(b==1);cpu.set(c==1);
    owner.set(o==1?new GpuQualification1961.Cancellation(){public boolean cancelled(){return cancelled;}}:null);
    check(GpuResident1976.deployedProbe19102()==(b==1&&c==0&&o==1&&x==0),"owned non-CPU uncancelled benchmark only");
   }
   benchmark.set(true);cpu.set(false);owner.set(new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}});
   final boolean[] inherited={true};Thread child=new Thread(new Runnable(){public void run(){inherited[0]=GpuResident1976.deployedProbe19102();}});child.start();child.join();
   check(!inherited[0],"probe permission cannot leak to child thread");
  }finally{benchmark.remove();cpu.remove();owner.remove();}
  check(!GpuResident1976.deployedProbe19102(),"permission cleared after cleanup");
 }
 public static void main(String[] args)throws Exception{
  budget();permission();
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_production_helpers\":true,\"physical_android_tested\":false,\"gpu_pixels_tested\":false}");
 }
}
