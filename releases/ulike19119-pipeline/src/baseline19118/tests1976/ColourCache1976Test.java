package com.hiro.ulike;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

/** Actual JNI differential against the pinned .75 CPU and coefficient paths. */
public final class ColourCache1976Test {
 static { System.loadLibrary("colourcache1976"); }
 static long assertions, cases, pixels, records;
 static final int SENTINEL=0x156789ab;
 static native boolean oldProcess(int[] in,int[] out,int width,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,int mw,int mh,int columns,int mr,int pw,int ph,float[] model,int[] protection);
 static native boolean newProcess(int[] in,int[] out,int width,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,int mw,int mh,int columns,int mr,int pw,int ph,float[] model,int[] protection);
 static native float[] oldPrepare(int[] in,int width,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,int mh,int columns,int mr,int pw,int ph,float[] model);
 static native float[] newPrepare(int[] in,int width,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,int mh,int columns,int mr,int pw,int ph,float[] model);
 static native boolean oldDirect(ByteBuffer cache,ByteBuffer records,int[] in,int width,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,int mh,int columns,int mr,int pw,int ph,float[] model);
 static native boolean newDirect(ByteBuffer cache,ByteBuffer records,int[] in,int width,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,int mh,int columns,int mr,int pw,int ph,float[] model);
 static native long oldCount(); static native long newCount();
 static native void oldFault(int allocation); static native void newFault(int allocation);
 static synchronized void check(boolean value,String message) { assertions++; if(!value)throw new AssertionError(message); }
 static void ints(int[] a,int[] b,String why) {check(Arrays.equals(a,b),why);synchronized(ColourCache1976Test.class){pixels+=a.length;}}
 static void floats(float[] a,float[] b,String why) {check(a!=null&&b!=null&&a.length==b.length,why+" length");for(int i=0;i<a.length;i++)check(Float.floatToRawIntBits(a[i])==Float.floatToRawIntBits(b[i]),why+" at "+i);synchronized(ColourCache1976Test.class){records+=a.length;}}
 static int[] input(int width,int height,int seed,boolean nonopaque) {Random r=new Random(seed);int[] a=new int[width*height];for(int y=0;y<height;y++)for(int x=0;x<width;x++){int p=r.nextInt(); a[y*width+x]=0xff000000|(p&0xffffff);if(nonopaque&&(x+y*3)%47==0)a[y*width+x]=p&0x7fffffff;}return a;}
 static float[] model(int seed) {Random r=new Random(seed);float[] m=new float[3+8*2+8*3*3];m[0]=5.4f;m[1]=9.2f;m[2]=113.4f;for(int i=3;i<19;i++)m[i]=.8f+r.nextFloat()*5;for(int i=19;i<m.length;i++)m[i]=.4f+r.nextFloat()*1.5f;return m;}
 static boolean process(boolean newer,int[] in,int[] out,int w,int h,int begin,int end,int vb,int ve,int origin,int mh,int nr,boolean shadows,float[] m,int[] protection){return newer?newProcess(in,out,w,h,begin,end,vb,ve,origin,nr,shadows,w,mh,1,1,w,mh,m,protection):oldProcess(in,out,w,h,begin,end,vb,ve,origin,nr,shadows,w,mh,1,1,w,mh,m,protection);}
 static float[] prepare(boolean newer,int[] in,int w,int h,int begin,int end,int origin,int mh,int nr,boolean shadows,float[] m){return newer?newPrepare(in,w,h,begin,end,0,h,origin,nr,shadows,mh,1,1,w,mh,m):oldPrepare(in,w,h,begin,end,0,h,origin,nr,shadows,mh,1,1,w,mh,m);}
 static void one(int w,int h,int seed,boolean nonopaque,int nr) {
  int[] in=input(w,h,seed,nonopaque),snapshot=in.clone(),a=new int[in.length],b=new int[in.length];float[] m=model(seed);
  Arrays.fill(a,SENTINEL);Arrays.fill(b,SENTINEL);int[] protection=new int[w*h*2];for(int i=0;i<protection.length;i++)protection[i]=(i*31)%257;
  check(process(false,in,a,w,h,0,h,0,h,0,h,nr,true,m,protection),"old process");
  check(process(true,in,b,w,h,0,h,0,h,0,h,nr,true,m,protection),"new process");ints(a,b,"whole exact output");ints(in,snapshot,"source immutable");
  if(nr>0){floats(prepare(false,in,w,h,0,h,0,h,nr,true,m),prepare(true,in,w,h,0,h,0,h,nr,true,m),"exact coefficients and safeguards");}
  synchronized(ColourCache1976Test.class){cases++;}
 }
 static int recordBytes(int w,int begin,int end){int first=Math.floorDiv(begin-7,4)*4;return ((w+3)/4+1)*((end-first+3)/4)*248*4;}
 static void direct() {
  int w=37,h=143;int[] in=input(w,h,987,true);float[] m=model(33);int cacheBytes=80+m.length*4+((w+3)/4+1)*4*1260;
  ByteBuffer a=ByteBuffer.allocateDirect(cacheBytes).order(ByteOrder.nativeOrder()),b=ByteBuffer.allocateDirect(cacheBytes).order(ByteOrder.nativeOrder());
  for(int pass=0;pass<4;pass++)for(int begin=0;begin<h;begin+=23){int end=Math.min(h,begin+23),bytes=recordBytes(w,begin,end);ByteBuffer ar=ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder()),br=ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
   if(pass==1)in[begin*w]^=0x010101; if(pass==2)m[2]+=.125f;
   boolean x=oldDirect(a,ar,in,w,h,begin,end,0,h,0,4,true,h,1,1,w,h,m),y=newDirect(b,br,in,w,h,begin,end,0,h,0,4,true,h,1,1,w,h,m);check(x&&y,"direct success");
   for(int i=0;i<bytes;i+=4)check(ar.getInt(i)==br.getInt(i),"direct exact record");
   for(int i=0;i<cacheBytes;i+=4)check(a.getInt(i)==b.getInt(i),"tail cache exact including counters");
  }
 }
 static void strips() {
  int w=53,h=151;int[] all=input(w,h,904,false);float[] m=model(36);int[] ref=new int[w*h],got=new int[w*h];
  check(process(false,all,ref,w,h,0,h,0,h,0,h,4,false,m,null),"whole reference");
  for(int begin=0;begin<h;begin+=19){int end=Math.min(h,begin+19),origin=Math.max(0,begin-7),bottom=Math.min(h,end+7),rows=bottom-origin;int[] src=Arrays.copyOfRange(all,origin*w,bottom*w),out=new int[src.length];
   check(process(true,src,out,w,rows,begin-origin,end-origin,0,rows,origin,h,4,false,m,null),"strip origin/halo");System.arraycopy(out,(begin-origin)*w,got,begin*w,(end-begin)*w);
   floats(prepare(false,src,w,rows,begin-origin,end-origin,origin,h,4,false,m),prepare(true,src,w,rows,begin-origin,end-origin,origin,h,4,false,m),"strip exact coefficients");
  }ints(ref,got,"strip output agrees with unpartitioned reference");
 }
 static void failures() {
  int w=31,h=47;int[] in=input(w,h,45,false),out=new int[w*h],sentinel=new int[w*h];Arrays.fill(out,SENTINEL);Arrays.fill(sentinel,SENTINEL);float[] m=model(17);
  for(int fail=1;fail<=3;fail++){newFault(fail);check(!process(true,in,out,w,h,0,h,0,h,0,h,4,true,m,null),"allocation failure");ints(out,sentinel,"failed private allocation never commits");}newFault(0);
  check(!process(true,in,out,w,h,3,h-3,3,h-3,0,h,4,true,m,null),"incomplete halo refused");ints(out,sentinel,"invalid halo unchanged");
  int[] copy=in.clone();check(!process(true,in,in,w,h,0,h,0,h,0,h,4,true,m,null),"source/output alias refused");ints(in,copy,"alias source unchanged");
  m[0]=Float.NaN;check(!process(true,in,out,w,h,0,h,0,h,0,h,4,true,m,null),"invalid model refused");ints(out,sentinel,"invalid model unchanged");
  m=model(17);check(process(true,in,out,w,h,0,h,0,h,0,h,4,true,m,null),"clean call after previous aborted transactions");
  int[] ref=new int[out.length];check(process(false,in,ref,w,h,0,h,0,h,0,h,4,true,m,null),"recovery reference");ints(out,ref,"aborted call retains no colour state");
 }
 static void parallel() throws Exception {
  ExecutorService workers=Executors.newFixedThreadPool(4);List<Future<?>> jobs=new ArrayList<>();for(int t=0;t<8;t++){final int seed=t;jobs.add(workers.submit(()->{one(17+seed*2,25+seed*3,130+seed,(seed&1)!=0,1+seed%4);return null;}));}for(Future<?> f:jobs)f.get();workers.shutdown();check(workers.awaitTermination(30,TimeUnit.SECONDS),"workers joined");
 }
 public static void main(String[] args) throws Exception {
  for(int[] d:new int[][]{{1,1},{1,17},{3,5},{7,9},{8,8},{15,16},{16,17},{17,33},{31,63},{37,65},{65,79}})for(int nr=1;nr<=4;nr++)one(d[0],d[1],d[0]*41+d[1]+nr,nr%2==0,nr);
  one(35,41,904,false,0);strips();direct();failures();parallel();
  oldCount();newCount();one(129,73,724,false,4);long old=oldCount(),now=newCount();check(now<old/2,"at least half of repeated binary64 colour conversions eliminated");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"pixels_compared\":"+pixels+",\"residual_floats_compared\":"+records+",\"baseline_colour_conversions\":"+old+",\"cached_colour_conversions\":"+now+",\"actual_production_jni_executed\":true,\"baseline75_exact\":true,\"private_abort_rollback\":true,\"concurrent_workers_exact\":true,\"physical_android_tested\":false}");
 }
}
