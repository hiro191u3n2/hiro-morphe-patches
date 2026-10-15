package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/** Production row cache against the retained pre-pairing independent samplers.
 * Android image IO is outside this test. No quality threshold is relaxed. */
public final class SourceMaskRows19103Test {
 static long checks,pixels,cachedCases,fallbackCases,readsBefore,readsAfter;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static void compare(FaceRegions1934.Mask now,FaceRowsReference19103.Mask old,boolean shuffled){
  FaceRegions1934.Mask.PairRows19103 rows=now.pairRows19103(now.width);
  if(rows==null){fallbackCases++;for(int y=-1;y<=now.height;y++)for(int x=-1;x<=now.width;x++){
   int actual=now.samplePair1976(x,y);check((actual&65535)==old.skinQ8(x,y)&&(actual>>>16)==old.detailQ8(x,y),"rotated fallback exact");pixels++;}return;}
  cachedCases++;int[] order=new int[now.height+2];order[0]=-1;for(int y=0;y<now.height;y++)order[y+1]=y;order[order.length-1]=now.height;
  if(shuffled){Random random=new Random(19103);for(int i=order.length-1;i>0;i--){int j=random.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v;}}
  for(int y:order){rows.prepare(y);for(int x=-1;x<=now.width;x++){
   int actual=rows.sample(x);check((actual&65535)==old.skinQ8(x,y)&&(actual>>>16)==old.detailQ8(x,y),"cached pair equals independent per-pixel bilinear samplers");pixels++;}}
  if(!shuffled){readsBefore+=8L*now.width*now.height;readsAfter+=4L*rows.rowsBuilt*now.width;}
 }
 static void corpus(){
  Random random=new Random(19103);int[][] sizes={{1,1},{2,3},{7,9},{65,69},{193,257},{3060,4080}};
  for(int[] size:sizes)for(int turn:new int[]{0,90,180,270})for(int rotation:new int[]{0,90,180,270}){
   int w=size[0],h=size[1],rw=Math.max(1,w/7),rh=Math.max(1,h/7);byte[] skin=new byte[rw*rh],detail=new byte[skin.length];random.nextBytes(skin);random.nextBytes(detail);
   FaceRegions1934.Mask now=FaceRegions1934.uprightRaster(w,h,rw,rh,skin.clone(),detail.clone(),turn).resample(rotation,w,h);
   FaceRowsReference19103.Mask old=FaceRowsReference19103.uprightRaster(w,h,rw,rh,skin.clone(),detail.clone(),turn).resample(rotation,w,h);
   // Full-size cases exercise all four axis orientations without making host
   // tests needlessly repeat all sixteen equivalent rotation compositions.
   if(w>1000&&rotation!=0)continue;
   compare(now,old,w<1000);
  }
  check(FaceRegions1934.empty(3060,4080).pairRows19103(3060)==null,"empty unreliable mask has no allocations");
 }
 static Object mask(Class<?> type,int width,int height,int rw,int rh,byte[] skin,byte[] detail,double[] m)throws Exception{
  Constructor<?> c=type.getDeclaredConstructor(int.class,int.class,int.class,int.class,byte[].class,byte[].class,double.class,double.class,double.class,double.class,double.class,double.class,boolean.class);c.setAccessible(true);
  return c.newInstance(width,height,rw,rh,skin,detail,m[0],m[1],m[2],m[3],m[4],m[5],true);
 }
 static void boundaries()throws Exception{
  Random random=new Random(103);byte[] s=new byte[63],d=new byte[63];random.nextBytes(s);random.nextBytes(d);
  double[][] transforms={{.125,0,-.5,0,.2,-.5},{-.125,-0.0,8.5,-0.0,-.2,6.5},{0,0,8,0,0,6},{.125,0,-.500000001,0,.2,-.500000001},{.125,0,8.500000001,0,.2,6.500000001},{.125,.01,0,0,.2,0}};
  for(double[] m:transforms){FaceRegions1934.Mask now=(FaceRegions1934.Mask)mask(FaceRegions1934.Mask.class,65,69,9,7,s,d,m);FaceRowsReference19103.Mask old=(FaceRowsReference19103.Mask)mask(FaceRowsReference19103.Mask.class,65,69,9,7,s,d,m);compare(now,old,true);check(now.pairRows19103(64)==null,"wrong image width retains fallback");}
 }
 static void parallel()throws Exception{
  int w=193,h=257,rw=29,rh=39;byte[] s=new byte[rw*rh],d=s.clone();new Random(7).nextBytes(s);new Random(11).nextBytes(d);
  final FaceRegions1934.Mask mask=FaceRegions1934.uprightRaster(w,h,rw,rh,s,d,0);final FaceRowsReference19103.Mask oracle=FaceRowsReference19103.uprightRaster(w,h,rw,rh,s,d,0);
  final FaceRegions1934.Mask.PairRows19103[] rows=new FaceRegions1934.Mask.PairRows19103[4];for(int i=0;i<4;i++)rows[i]=mask.pairRows19103(w);
  for(int i=0;i<4;i++)for(int j=0;j<i;j++)check(rows[i].skinA!=rows[j].skinA&&rows[i].detailA!=rows[j].detailA&&rows[i].lo!=rows[j].lo,"independent worker-owned mutable arrays");
  ExecutorService workers=Executors.newFixedThreadPool(4);try{List<Future<Boolean>> futures=new ArrayList<Future<Boolean>>();for(int i=0;i<4;i++){final int id=i;futures.add(workers.submit(new Callable<Boolean>(){public Boolean call(){for(int pass=0;pass<3;pass++)for(int y=id;y<h;y+=4){rows[id].prepare(y);for(int x=0;x<w;x++){int value=rows[id].sample(x);if((value&65535)!=oracle.skinQ8(x,y)||(value>>>16)!=oracle.detailQ8(x,y))return false;}}return true;}}));}for(Future<Boolean> f:futures)check(f.get(),"parallel exact masks");}finally{workers.shutdownNow();}
  Thread.currentThread().interrupt();boolean stopped=false;try{mask.pairRows19103(w);}catch(CancellationException expected){stopped=true;}finally{Thread.interrupted();}check(stopped,"cancel constructor without cache commit");
  Thread.currentThread().interrupt();stopped=false;try{rows[0].prepare(7);}catch(CancellationException expected){stopped=true;}finally{Thread.interrupted();}check(stopped,"cancel row preparation");rows[0].prepare(7);check(rows[0].sample(5)==mask.samplePair1976(5,7),"explicit caller-owned cache reusable after cancellation");
 }
 public static void main(String[] args)throws Exception{corpus();boundaries();parallel();check(readsAfter*2<readsBefore,"full-size cache reduces mask byte reads by more than half");System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"compared_pixels\":"+pixels+",\"cache_cases\":"+cachedCases+",\"fallback_cases\":"+fallbackCases+",\"baseline_mask_byte_reads\":"+readsBefore+",\"cached_mask_byte_reads_upper_bound\":"+readsAfter+",\"independent_reference\":true,\"parallel_ownership_verified\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");}
}
