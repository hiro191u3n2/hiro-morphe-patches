package com.hiro.ulike;
import android.graphics.Bitmap;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/** Current private strip commit path, independent half oracle, controlled
 * Android premultiplication boundary, and a blocked I/O monitor observation. */
public final class FrontHalf1994Test {
 static int checks,cases;static long words;static Class<?> stripsType,workerType,halfType;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static Field field(Class<?> c,String name)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}
 static Method method(Class<?> c,String name,Class<?>...p)throws Exception{Method m=c.getDeclaredMethod(name,p);m.setAccessible(true);return m;}
 static Object make(Class<?> c,Class<?>[] types,Object...args)throws Exception{Constructor<?> v=c.getDeclaredConstructor(types);v.setAccessible(true);return v.newInstance(args);}
 static int[] image(int w,int h,boolean alpha){int[] v=new int[w*h];Random r=new Random(w*977+h);for(int i=0;i<v.length;i++)v[i]=((alpha&&i%7==0?127:255)<<24)|r.nextInt(1<<24);return v;}
 static int[] oracle(int[] p,int w,int h){int[] out=new int[((w+1)/2)*((h+1)/2)];for(int y=0;y<h;y+=2)for(int x=0;x<w;x+=2){int r=0,g=0,b=0,n=0;boolean opaque=true;for(int yy=y;yy<Math.min(y+2,h);yy++)for(int xx=x;xx<Math.min(x+2,w);xx++){int v=p[yy*w+xx];opaque&=v>>>24==255;r+=v>>>16&255;g+=v>>>8&255;b+=v&255;n++;}out[(y/2)*((w+1)/2)+x/2]=(opaque?0xff000000:0)|((r+n/2)/n<<16)|((g+n/2)/n<<8)|(b+n/2)/n;}return out;}
 static Object worker(Object strips)throws Exception{return make(workerType,new Class<?>[]{stripsType,QualityPixels1932.Plan.class,boolean.class,boolean.class},strips,null,false,false);}
 static void configure(Object worker,int[] image,int width,int first,int count)throws Exception {
  field(workerType,"first").setInt(worker,first);field(workerType,"count").setInt(worker,count);field(workerType,"start").setInt(worker,0);field(workerType,"rows").setInt(worker,count);
  int[] output=(int[])field(workerType,"output").get(worker);System.arraycopy(image,first*width,output,0,count*width);
 }
 static Object[] setup(int w,int h,int core,boolean alpha)throws Exception {
  Bitmap bitmap=Bitmap.from(w,h,new int[w*h],Bitmap.Config.ARGB_8888,true);Object strips=make(stripsType,new Class<?>[]{Bitmap.class,int.class,int.class},bitmap,core,2),half=make(halfType,new Class<?>[]{int.class,int.class},w,h);
  field(stripsType,"committedHalf").set(strips,half);return new Object[]{bitmap,strips,half};
 }
 static void cleanup(Object[] setup,List<Object> workers)throws Exception{
  method(halfType,"close").invoke(setup[2]);SpeedWorkers1935.release((int[])field(stripsType,"preceding").get(setup[1]));
  for(Object w:workers){SpeedWorkers1935.release((int[])field(workerType,"input").get(w));SpeedWorkers1935.release((int[])field(workerType,"output").get(w));}
  ((Bitmap)setup[0]).recycle();
 }
 static void shuffled(int w,int h,boolean alpha,int seed)throws Exception {
  int core=8;Object[] setup=setup(w,h,core,alpha);Bitmap bitmap=(Bitmap)setup[0];List<Object> workers=new ArrayList<Object>();int[] pixels=image(w,h,alpha);
  try{
   List<Integer> rows=new ArrayList<Integer>();for(int y=0;y<h;y+=core)rows.add(y);Collections.shuffle(rows,new Random(seed));
   Object a=worker(setup[1]),b=worker(setup[1]);workers.add(a);workers.add(b);
   int at=0;for(int y:rows){Object target=at++%2==0?a:b;configure(target,pixels,w,y,Math.min(core,h-y));method(stripsType,"write",workerType).invoke(setup[1],target);
    if(at<rows.size())check(method(halfType,"result").invoke(setup[2])==null,"partial commits never publish half");}
   int[] actual=(int[])method(halfType,"result").invoke(setup[2]);check(Arrays.equals(actual,oracle(bitmap.snapshot(),w,h)),"reused workers and shuffled commits match post-commit oracle");
   check(alpha?bitmap.reads>0:bitmap.reads==0,"only alpha pairs require Bitmap readback");words+=actual.length;cases++;
  }finally{cleanup(setup,workers);}
 }
 static void concurrent()throws Exception {
  int w=67,h=65,core=8;Object[] setup=setup(w,h,core,false);List<Object> workers=new ArrayList<Object>();List<Thread> threads=new ArrayList<Thread>();List<Throwable> failures=Collections.synchronizedList(new ArrayList<Throwable>());int[] pixels=image(w,h,false);
  try{
   final CountDownLatch start=new CountDownLatch(1);for(int y=0;y<h;y+=core){final Object worker=worker(setup[1]);workers.add(worker);configure(worker,pixels,w,y,Math.min(core,h-y));Thread t=new Thread(()->{try{start.await();method(stripsType,"write",workerType).invoke(setup[1],worker);}catch(Throwable error){failures.add(error);}});threads.add(t);t.start();}start.countDown();for(Thread t:threads)t.join(5000);
   check(failures.isEmpty(),"concurrent disjoint rows have no failures");for(Thread t:threads)check(!t.isAlive(),"all writes joined");
   check(Arrays.equals((int[])method(halfType,"result").invoke(setup[2]),oracle(((Bitmap)setup[0]).snapshot(),w,h)),"parallel half preparation exact");
  }finally{cleanup(setup,workers);}
 }
 static void lockAndFailures()throws Exception {
  Object[] setup=setup(65,8,8,false);Object worker=worker(setup[1]);List<Object> workers=Arrays.asList(worker);configure(worker,image(65,8,false),65,0,8);
  final Object[] ioSetup=setup;final Object ioWorker=worker;final Throwable[] failure={null};Thread t=new Thread(()->{try{method(stripsType,"write",workerType).invoke(ioSetup[1],ioWorker);}catch(Throwable e){failure[0]=e;}});
  try{
   synchronized(setup[1]){t.start();long end=System.nanoTime()+3000000000L;while(t.getState()!=Thread.State.BLOCKED&&System.nanoTime()<end)Thread.yield();
    check(t.getState()==Thread.State.BLOCKED,"write waits on shared Bitmap I/O monitor");check(field(workerType,"halfPrepared1994").getBoolean(worker),"half arithmetic completed before waiting for Bitmap I/O monitor");
    check(method(halfType,"result").invoke(setup[2])==null&&((Bitmap)setup[0]).writes==0,"prepared but uncommitted rows unpublished");}
   t.join(5000);check(failure[0]==null&&method(halfType,"result").invoke(setup[2])!=null,"successful commit publishes prepared half");
  }finally{cleanup(setup,workers);}
  setup=setup(7,9,8,false);worker=worker(setup[1]);workers=Arrays.asList(worker);configure(worker,image(7,9,false),7,0,8);
  try{Field f=Bitmap.class.getDeclaredField("writeFailure");f.setAccessible(true);f.setBoolean(setup[0],true);boolean rejected=false;
   try{method(stripsType,"write",workerType).invoke(setup[1],worker);}catch(InvocationTargetException expected){rejected=true;method(stripsType,"fail",Throwable.class).invoke(setup[1],expected.getCause());}
   check(rejected&&method(halfType,"result").invoke(setup[2])==null,"failed Bitmap write never publishes prepared half");
   check(field(halfType,"completedRows").getInt(setup[2])==0,"no failed rows marked completed");
  }finally{cleanup(setup,workers);}
  setup=setup(5,3,8,true);worker=worker(setup[1]);workers=Arrays.asList(worker);int[] p=image(5,3,true);configure(worker,p,5,0,3);
  try{Thread.currentThread().interrupt();method(halfType,"prepare1994",workerType).invoke(setup[2],worker);check(!field(workerType,"halfPrepared1994").getBoolean(worker)&&method(halfType,"result").invoke(setup[2])==null,"cancelled preparation stays unpublished");Thread.interrupted();
   ((Bitmap)setup[0]).setPixels(p,0,5,0,0,5,3);method(halfType,"record",workerType).invoke(setup[2],worker);check(Arrays.equals((int[])method(halfType,"result").invoke(setup[2]),oracle(((Bitmap)setup[0]).snapshot(),5,3)),"legacy direct record fallback including alpha remains exact");
  }finally{Thread.interrupted();cleanup(setup,workers);}
  setup=setup(7,9,8,false);worker=worker(setup[1]);workers=Arrays.asList(worker);
  try{Thread stopped=new Thread((Runnable)worker);stopped.interrupt();stopped.start();stopped.join(5000);
   check(!stopped.isAlive()&&field(stripsType,"failure").get(setup[1])!=null,"interrupted real worker records capture failure");
   check(((Bitmap)setup[0]).writes==0&&method(halfType,"result").invoke(setup[2])==null,"cancelled real worker publishes no half image");
  }finally{cleanup(setup,workers);}
 }
 public static void main(String[] args)throws Exception {
  stripsType=Class.forName("com.hiro.ulike.QualityPipeline1932$Strips");workerType=Class.forName("com.hiro.ulike.QualityPipeline1932$Worker");halfType=Class.forName("com.hiro.ulike.QualityPipeline1932$CommittedHalf1959");
  for(int[] d:new int[][]{{1,1},{5,3},{17,25},{67,65}})for(boolean alpha:new boolean[]{false,true})for(int seed=0;seed<3;seed++)shuffled(d[0],d[1],alpha,seed);
  concurrent();lockAndFailures();check(Bitmap.writesAfterRecycle.get()==0,"no worker writes recycled image");SpeedWorkers1935.trim();
  System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"half_cases\":"+cases+",\"half_words\":"+words+",\"controlled_bitmap_boundary\":true,\"physical_android_tested\":false}");
 }
}
