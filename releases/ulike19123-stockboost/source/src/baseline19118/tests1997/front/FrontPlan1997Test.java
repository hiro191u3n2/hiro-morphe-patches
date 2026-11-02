package com.hiro.ulike;
import android.graphics.Bitmap;
import java.lang.reflect.*;
import java.util.*;
public final class FrontPlan1997Test {
 static int assertions,plans,cases;static long pixels;
 static void check(boolean b,String m){assertions++;if(!b)throw new AssertionError(m);}
 static Object value(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void value(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static Object construct(Class<?> c,Class<?>[] sig,Object...args)throws Exception{Constructor<?> k=c.getDeclaredConstructor(sig);k.setAccessible(true);return k.newInstance(args);}
 static Object call(Object o,String n,Class<?>[] sig,Object...args)throws Exception{Method m=o.getClass().getDeclaredMethod(n,sig);m.setAccessible(true);return m.invoke(o,args);}
 static void plans()throws Exception{
  for(int height:new int[]{1,17,129,513,4080})for(int available=1;available<=4;available++)for(int preferred:new int[]{16,32,64,128,256}){
   long[] costs=new long[16];for(int i=0;i<16;i++)costs[i]=7000L+Math.min(height,16*(i+1)+14)*170L+16L*(i+1)*380L;
   for(long budget=0;budget<=1500000;budget+=9973){
    long shared=1234,half=17L*((height+1)/2)*4+2000;
    QualityPipeline1932.FrontPlan1997 p=QualityPipeline1932.chooseFront1997(height,preferred,4,available,budget,shared,half,true,costs);
    long remaining=Math.max(0,budget-shared);int oldRows=preferred;while(costs[oldRows/16-1]>remaining&&oldRows>16)oldRows/=2;
    int oldCount=Math.max(1,(int)Math.min(available,remaining/costs[oldRows/16-1]));oldCount=Math.min(oldCount,1+(height-1)/oldRows);
    check(p.workers==oldCount,"half cannot change old worker count");check(p.workers<=available&&p.workers<=1+(height-1)/p.rows,"worker caps");
    if(p.halfReason==0){
     check(p.halfBytes==half&&p.workerBytes*p.workers+shared+half<=budget,"simultaneous full reservation");
     check(p.rows>=Math.max(16,oldRows/2)&&p.rows<=oldRows,"halo overhead bounded by at most halved strips");
     for(int row=p.rows+16;row<=oldRows;row+=16)
      check(costs[row/16-1]>(Math.max(0,budget-shared)-half)/oldCount,"largest fitting rows at original concurrency");
    }else check(p.rows==oldRows&&p.workers==oldCount&&p.halfBytes==0&&p.halfReason==3,"no-fit keeps original plan");
    plans++;
   }
  }
  long[] c=new long[16];Arrays.fill(c,100);QualityPipeline1932.FrontPlan1997 p=QualityPipeline1932.chooseFront1997(4096,256,4,4,500,0,101,true,c);
  check(p.workers==4&&p.halfReason==3,"never trade fourth worker for half");
  p=QualityPipeline1932.chooseFront1997(4096,256,4,4,501,0,101,true,c);check(p.workers==4&&p.rows==256&&p.halfReason==0,"exact admission boundary");
  p=QualityPipeline1932.chooseFront1997(4096,256,4,4,1000,0,100,false,c);check(p.halfReason==2&&p.halfBytes==0,"format refusal explicit");
  check(QualityPipeline1932.frontHalfBudget1997(500,400,101)==100,"declining budget caps optional half; mandatory workers still fit");
  check(QualityPipeline1932.frontHalfBudget1997(501,400,101)==101,"exact half admission");
  check(QualityPipeline1932.frontHalfBudget1997(800,400,101)==101,"new memory cannot increase planned optional budget");
  check(QualityPipeline1932.frontHalfBudget1997(399,400,101)==0,"insufficient mandatory budget never grants optional bytes");
  Class<?> halfType=Class.forName("com.hiro.ulike.QualityPipeline1932$CommittedHalf1959");
  Method size=halfType.getDeclaredMethod("memoryBytes",int.class,int.class);size.setAccessible(true);
  Method create=halfType.getDeclaredMethod("create",Bitmap.class,long.class,int[].class);create.setAccessible(true);
  Bitmap image=Bitmap.createBitmap(17,33,Bitmap.Config.ARGB_8888);long needed=(Long)size.invoke(null,17,33);int[] reason={0};
  Object optional=create.invoke(null,image,QualityPipeline1932.frontHalfBudget1997(400+needed-1,400,needed),reason);
  check(optional==null&&reason[0]==3,"real half allocation refuses reduced residual without throwing or changing workers");image.recycle();
  boolean rejected=false;try{QualityPipeline1932.chooseFront1997(4096,240,4,4,1000,0,100,true,c);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"halving requires power-of-two initial rows");
 }
 public static void main(String[] args)throws Exception{plans();int[] proof=FrontPixels1997.verify(args[0]);assertions+=proof[0];System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"planner_cases\":"+plans+",\"pixel_cases\":"+proof[1]+",\"compared_pixels\":"+proof[2]+",\"native_and_half_bitwise_equal\":true,\"no_worker_sacrifice\":true,\"simultaneous_budget_verified\":true,\"largest_fitting_rows_verified\":true,\"optional_half_budget_drop_verified\":true,\"bounded_halo_growth\":true,\"immutable_96_pixels_equal\":true,\"multiple_workers_verified\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");}
}
