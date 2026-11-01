package com.hiro.ulike;
import android.graphics.Bitmap;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
/** Same fixture compiled in separate immutable .96 and candidate VMs. */
public final class FrontPixels1997 {
 static int assertions,cases,pixels;
 static void check(boolean b,String m){assertions++;if(!b)throw new AssertionError(m);}
 static Object value(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static Object construct(Class<?> c,Class<?>[] sig,Object...args)throws Exception{Constructor<?> k=c.getDeclaredConstructor(sig);k.setAccessible(true);return k.newInstance(args);}
 static Object call(Object o,String n,Class<?>[] sig,Object...args)throws Exception{Method m=o.getClass().getDeclaredMethod(n,sig);m.setAccessible(true);return m.invoke(o,args);}
 static void value(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static final SingleNoise1955.Protection protection=new SingleNoise1955.Protection(){public int budgetQ8(int x,int y){return (x+y)%11==0?0:224;}public int detailQ8(int x,int y){return (x*3+y)%9*16;}};
 static SingleNoise1955.Model model(final int[] input,final int w,final int h){return SingleNoise1955.probe(new SingleNoise1955.Patches(){public void read(int[] d,int x,int y,int pw,int ph){for(int row=0;row<ph;row++)System.arraycopy(input,(y+row)*w+x,d,row*pw,pw);}},w,h);}
 static int[] input(int scene,int w,int h){int[] in=new int[w*h];Random r=new Random(1997+scene);for(int i=0;i<in.length;i++){int n=90+r.nextInt(57);in[i]=(scene==1&&i%7==0?0x70000000:0xff000000)|(scene==2?((i%w<17?80:160)<<16)|n<<8|n:n<<16|n<<8|n);}return in;}
 static int[] average(Bitmap image){int w=image.getWidth(),h=image.getHeight(),hw=(w+1)/2;int[] src=image.snapshot(),out=new int[hw*((h+1)/2)];for(int y=0;y<h;y+=2)for(int x=0;x<w;x+=2){int r=0,g=0,b=0,n=0;boolean opaque=true;for(int yy=y;yy<Math.min(h,y+2);yy++)for(int xx=x;xx<Math.min(w,x+2);xx++){int v=src[yy*w+xx];r+=v>>>16&255;g+=v>>>8&255;b+=v&255;opaque&=(v>>>24)==255;n++;}out[(y/2)*hw+x/2]=(opaque?0xff000000:0)|((r+n/2)/n<<16)|((g+n/2)/n<<8)|(b+n/2)/n;}return out;}
 static int[][] run(final int[] input,final int w,final int h,final int core,final int count,final boolean shadows)throws Exception{
  check(SingleNoise1955.nativeAvailable(),"actual Single JNI loaded");
  final Class<?> stripsType=Class.forName("com.hiro.ulike.QualityPipeline1932$Strips"),workerType=Class.forName("com.hiro.ulike.QualityPipeline1932$Worker"),halfType=Class.forName("com.hiro.ulike.QualityPipeline1932$CommittedHalf1959");
  final SingleNoise1955.Model model=model(input,w,h);final float[] nativeData=(float[])value(model,"nativeData");final Bitmap image=Bitmap.from(w,h,input,Bitmap.Config.ARGB_8888,true);
  final Object strips=construct(stripsType,new Class[]{Bitmap.class,int.class,int.class},image,core,SingleNoise1955.HALO);
  final Object half=construct(halfType,new Class[]{int.class,int.class},w,h);value(strips,"committedHalf",half);
  final int[] total=new int[input.length];final Object[] workers=new Object[count];Runnable[] jobs=new Runnable[count];final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
  try{
   for(int index=0;index<count;index++){
    final Object worker=construct(workerType,new Class[]{stripsType,QualityPixels1932.Plan.class,boolean.class,boolean.class},strips,null,false,false);workers[index]=worker;
    jobs[index]=new Runnable(){public void run(){try{while((Integer)call(strips,"read",new Class[]{workerType},worker)>=0){
     int first=(Integer)value(worker,"first"),rows=(Integer)value(worker,"rows"),start=(Integer)value(worker,"start"),owned=(Integer)value(worker,"count");
     int[] in=(int[])value(worker,"input"),out=(int[])value(worker,"output"),policy=new int[owned*w*2];
     for(int y=0;y<owned;y++)for(int x=0;x<w;x++){int at=(y*w+x)*2;policy[at]=protection.budgetQ8(x,first+y);policy[at+1]=protection.detailQ8(x,first+y);}
     int[] u=CpuSingle1978.geometry(w,rows,start,start+owned,0,rows,first-start,3,shadows,model);
     if(!CpuSingle1978.nativeCall(in,out,u,nativeData,policy,null))throw new AssertionError("actual native call must succeed; no Java fallback");
     System.arraycopy(out,start*w,total,first*w,owned*w);call(strips,"write",new Class[]{workerType},worker);
    }}catch(Throwable e){failure.compareAndSet(null,e);try{call(strips,"fail",new Class[]{Throwable.class},e);}catch(Exception ignored){}}}};
   }
   SpeedWorkers1935.run(jobs);if(failure.get()!=null)throw new AssertionError("worker failure",failure.get());
   int[] carried=(int[])call(half,"result",new Class[]{});check(Arrays.equals(average(image),carried),"half equals committed odd/alpha pixels");
   check((Integer)value(half,"completedRows")==((h+1)/2),"every half row published once");
   return new int[][]{total,image.snapshot(),carried};
  }finally{call(half,"close",new Class[]{});for(Object worker:workers)if(worker!=null){SpeedWorkers1935.release((int[])value(worker,"input"));SpeedWorkers1935.release((int[])value(worker,"output"));}SpeedWorkers1935.release((int[])value(strips,"preceding"));image.recycle();}
 }
 static void write(DataOutputStream out,int[] v)throws IOException{out.writeInt(v.length);for(int x:v)out.writeInt(x);}
 static int[] read(DataInputStream in)throws IOException{int[] out=new int[in.readInt()];for(int i=0;i<out.length;i++)out[i]=in.readInt();return out;}
 public static void main(String[] args)throws Exception{
  try(DataOutputStream out=new DataOutputStream(new FileOutputStream(args[0]))){for(int scene=0;scene<4;scene++){int w=scene==3?17:33,h=scene==3?33:519;int[] in=input(scene,w,h);int[] java=new int[in.length];SingleNoise1955.processJavaRange(in,java,w,h,0,h,0,h,0,3,scene%2==0,model(in,w,h),protection);int[][] exact=run(in,w,h,256,4,scene%2==0);check(Arrays.equals(java,exact[0]),"immutable .96 Java whole vs native 256 rows");out.writeInt(w);out.writeInt(h);write(out,in);for(int[] values:exact)write(out,values);}}
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"immutable_96_oracle\":true}");
 }
 static int[] verify(String path)throws Exception{
  try(DataInputStream in=new DataInputStream(new FileInputStream(path))){for(int scene=0;scene<4;scene++){int w=in.readInt(),h=in.readInt();int[] source=read(in);int[][] expected={read(in),read(in),read(in)};
   for(int count:new int[]{1,4})for(int core=16;core<=256;core+=16){int[][] actual=run(source,w,h,core,count,scene%2==0);for(int kind=0;kind<3;kind++){check(Arrays.equals(expected[kind],actual[kind]),"immutable .96 equality scene="+scene+" core="+core+" workers="+count+" kind="+kind);pixels+=actual[kind].length;}cases++;}
  }check(in.read()==-1,"complete oracle consumed");}return new int[]{assertions,cases,pixels};
 }
}
