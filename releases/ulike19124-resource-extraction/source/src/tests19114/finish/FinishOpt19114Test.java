package com.hiro.ulike;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;

/** Published .113 is a separately compiled oracle; plan callbacks and real
 * GLES policy output supply independent checks of the new row proof. */
public final class FinishOpt19114Test {
 static long checks,pixels,constantRows,constantPixels,policyWords;static long checksum;
 static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
 static Object field(Object owner,String name)throws Exception {Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
 static int expected(GpuFinishPolicy1992.Fixture f,int x,int y){
  int skin=f.plan.texturePriority&&f.plan.faceRegions!=null?f.plan.skinAt(x,y):0;
  int confidence=f.plan.smoothedRegions==null?0:f.plan.smoothedRegions.smoothingQ8(x,y);
  return skin|(confidence<<16);
 }
 static void fill(byte[] values,int pattern,int columns,int salt){
  Random random=new Random(77114+salt);
  for(int i=0;i<values.length;i++)values[i]=(byte)(pattern==0?0:pattern==1?255:pattern==2?127+salt%2:
    pattern==3?(i/columns*37+salt*11)&255:random.nextInt(256));
 }
 static void pattern(GpuFinishPolicy1992.Fixture f,int kind)throws Exception {
  if(f.plan.faceRegions!=null){byte[] values=(byte[])field(f.plan.faceRegions,"skin");int columns=(Integer)field(f.plan.faceRegions,"rw");fill(values,kind==6?0:kind==7?4:kind,columns,0);}
  if(f.plan.smoothedRegions!=null){Object owner=f.plan.smoothedRegions;try{owner=field(owner,"this$0");}catch(NoSuchFieldException unmapped){}
   fill((byte[])field(owner,"confidence"),kind==7?0:kind==6?4:kind,(Integer)field(owner,"columns"),1);}
 }
 static void rows() {
  final int width=257,height=19;
  final double[] extremes={Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,-1e300,-.5,-0.0,0.0,.5,1.5,2.0,1e300};
  for(final boolean smooth:new boolean[]{false,true})for(int value=0;value<256;value++)for(final boolean invalid:new boolean[]{false,true}){
   GpuFinishPolicy1992.Axis x=new GpuFinishPolicy1992.Axis(width,true,3,1,new GpuFinishPolicy1992.Coordinate(){public double at(int i){return invalid&&i<extremes.length?extremes[i]:smooth?i*9.0/(width-1):i*2.0/(width-1);}},smooth,10);
   GpuFinishPolicy1992.Axis y=new GpuFinishPolicy1992.Axis(height,false,3,3,new GpuFinishPolicy1992.Coordinate(){public double at(int i){return invalid&&i<extremes.length?extremes[i]:smooth?i*9.0/(height-1):i*2.0/(height-1);}},smooth,10);
   byte[] values=new byte[9];for(int i=0;i<9;i++)values[i]=(byte)((value+i/3)&255);
   GpuFinishPolicy1992.Sampler source=new GpuFinishPolicy1992.Sampler(values,x,y,smooth);
   GpuFinishPolicy1992.RowSampler1998 cached=new GpuFinishPolicy1992.RowSampler1998(source,width);
   for(int n=0;n<height*2;n++){int row=n<height?n:2*height-1-n;cached.prepare(row);
    if(cached.constant19114>=0){constantRows++;constantPixels+=width;}
    for(int col=0;col<width;col++){int oracle=source.sample(col,row);check(cached.sample(col)==oracle,"original exact sampler output");if(cached.constant19114>=0)check(cached.constant19114==oracle,"rounded extrema bound every sample");pixels++;}
   }
   Thread.currentThread().interrupt();boolean cancelled=false;try{cached.prepare(0);}catch(CancellationException expected){cancelled=true;}finally{Thread.interrupted();}check(cancelled,"constant-row admission does not suppress interruption");
  }
  // Internal descriptors cannot normally contain these fractions. A corrupt
  // cache must decline the shortcut instead of proving an unsafe constant.
  for(final double fraction:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,-1,2}){
   GpuFinishPolicy1992.Axis x=new GpuFinishPolicy1992.Axis(2,true,2,1,new GpuFinishPolicy1992.Coordinate(){public double at(int i){return i;}},false,2);
   GpuFinishPolicy1992.Axis y=new GpuFinishPolicy1992.Axis(2,false,2,2,new GpuFinishPolicy1992.Coordinate(){public double at(int i){return i;}},false,2);
   y.fraction[0]=fraction;GpuFinishPolicy1992.RowSampler1998 cached=new GpuFinishPolicy1992.RowSampler1998(new GpuFinishPolicy1992.Sampler(new byte[]{20,20,20,20},x,y,false),2);
   cached.prepare(0);check(cached.constant19114<0,"nonfinite/out-of-domain fraction declines shortcut");
  }
 }
 static void actual(int id,int kind,boolean gpu)throws Exception {
  try{java.lang.reflect.Method reset=GpuNoise1960.class.getDeclaredMethod("reset",int.class);reset.setAccessible(true);reset.invoke(null,0);}catch(NoSuchMethodException realJni){}
  GpuFinishPolicy1992.Fixture f=GpuFinishPolicy1992.fixture(id,4284,257);pattern(f,kind);
  int[] original=new int[f.width*f.height];
  GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"published reference session");
  BaselineFinishPolicy19113.Context old=null;
  try{old=BaselineFinishPolicy19113.open(session,f.plan,f.width,f.height,128);check(old!=null,"published reference context");
   for(int first=0;first<f.height;first+=128){int last=Math.min(first+128,f.height);BaselineFinishPolicy19113.Band b=old.prepare(first,last,(first/128)&1);try{System.arraycopy((int[])field(old,"masks"),0,original,first*f.width,(last-first)*f.width);}finally{b.close();}}
  }finally{if(old!=null)old.close();session.close();}
  session=GpuNoise1960.open();check(session!=null,"new exact session");GpuFinishPolicy1992.Context current=null;GpuNoise1960.Lease1971 output=null;
  try{current=GpuFinishPolicy1992.open(session,f.plan,f.width,f.height,128);check(current!=null,"new exact128 context");
   if(gpu){output=session.reserveCapacity1983(new int[]{3},new long[]{16L*f.width*128},new long[]{0},16L*f.width*128);check(output!=null,"actual GPU policy output lease");}
   for(int first:new int[]{0,128,256,128,0}){int last=Math.min(first+128,f.height),count=(last-first)*f.width;GpuFinishPolicy1992.Band b=current.prepare(first,last,(first/128)&1);
    try{int[] masks=(int[])field(current,"masks");int at=0;
     for(int y=first;y<last;y++)for(int x=0;x<f.width;x++,at++){check(masks[at]==original[y*f.width+x],"published .113 packed mask unchanged");check(masks[at]==expected(f,x,y),"real mask callback oracle unchanged");pixels++;}
     if(gpu&&first==0){int[][] actual=session.execute(b.append(new GpuNoise1960.Batch(),3),new int[]{3},new int[]{count*4});check(actual!=null,"actual GPU policy output");FinishPolicy1953.Band oracle=FinishPolicy1953.prepareCpu1961(f.plan,f.width,f.height,first,last,0);check(oracle!=null,"CPU policy oracle");
      try{for(int i=0;i<count;i++)for(int lane=0;lane<4;lane++){int value=oracle.mode==FinishPolicy1953.CONSTANT4?oracle.words[lane]:oracle.mode==FinishPolicy1953.RAW4?oracle.words[i*4+lane]:oracle.words[i*2+lane/2]>>>(16*(lane&1))&65535;check(actual[0][i*4+lane]==value,"all four exact GPU policy channels");policyWords++;}}finally{oracle.close();}}
    }finally{b.close();}
   }
  }finally{if(output!=null)output.close();if(current!=null)current.close();session.close();}
 }
 public static void main(String[] args)throws Exception {
  boolean gpu=args.length==0||!args[0].equals("mock");rows();
  for(int id:new int[]{4,10,5,11})for(int kind:new int[]{0,1,2,3,4,6,7})actual(id,kind,gpu&&(id==4||id==10)&&(kind==0||kind==3||kind==4));
  check(constantRows>10000&&constantPixels>2500000,"bulk row proof removes per-pixel interpolation/round for uniform masks");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"compared_pixels\":"+pixels+",\"constant_rows\":"+constantRows+",\"constant_pixels\":"+constantPixels+",\"gpu_policy_words\":"+policyWords+",\"published113_masks_equal\":true,\"exact_cpu_oracle_equal\":true,\"nonfinite_domain_declined\":true,\"rotated_fallback_verified\":true,\"actual_jni\":"+gpu+",\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
