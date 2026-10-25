package com.hiro.ulike;
import java.util.*;
import java.util.concurrent.*;

/** Controlled noise is separated from scene truth. Ratios are image-domain host
 * metrics, not a claim of smartphone speed or camera output quality. */
public final class NoiseQuality1942Test {
 static int assertions,scenarios;
 static Map<String,Double> metrics=new LinkedHashMap<String,Double>();
 static void check(boolean v,String m){assertions++;if(!v)throw new AssertionError(m);}
 static int gray(int y){y=Math.max(0,Math.min(255,y));return 0xff000000|(y<<16)|(y<<8)|y;}
 static int rgb(int r,int g,int b){return 0xff000000|(r<<16)|(g<<8)|b;}
 static double mse(int[] a,int[] b,int w,int x0,int y0,int x1,int y1){
  double sum=0;int count=0;
  for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++){int i=y*w+x;double d=QualityPixels1932.luma(a[i])-QualityPixels1932.luma(b[i]);sum+=d*d;count++;}
  return sum/count;
 }
 static double mean(int[] a,int w,int x0,int y0,int x1,int y1){double sum=0;int count=0;for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++){sum+=QualityPixels1932.luma(a[y*w+x]);count++;}return sum/count;}
 static QualityPixels1932.Plan plan(float sigma,int noise,int sharp){
  return QualityPixels1932.plan(new QualityPixels1932.NoiseStats(sigma,sigma,128,0,4096),400,10000000L,2,0,noise,sharp,true,true,1).withLocalNoise(null,noise);
 }
 static int[] finish(int[] p,int w,int h,QualityPixels1932.Plan plan,boolean shadows){
  int[] q=p.clone();QualityShadow1932.smoothRange(p,q,w,h,0,h,0,h,plan.noiseLevel,shadows,4,plan,0);return q;
 }
 static int[] legacy(int[] p,int w,int h,boolean shadows){return legacy(p,w,h,shadows,plan(8,4,0));}
 static int[] legacy(int[] p,int w,int h,boolean shadows,QualityPixels1932.Plan plan){int[] q=p.clone();NoiseLegacy1941.smoothRange(p,q,w,h,0,h,0,h,4,shadows,3,plan,0);return q;}
 static int[] random(int[] truth,double sigma,long seed){Random r=new Random(seed);int[] p=truth.clone();for(int i=0;i<p.length;i++)p[i]=gray((int)Math.round(QualityPixels1932.luma(truth[i])+sigma*r.nextGaussian()));return p;}

 static void noise(){
  scenarios++;int w=192,h=160;int[] truth=new int[w*h];Arrays.fill(truth,gray(150));
  int[] p=random(truth,8,1941),q=finish(p,w,h,plan(8,4,0),false),old=legacy(p,w,h,false);
  double a=mse(p,truth,w,12,12,w-12,h-12),b=mse(q,truth,w,12,12,w-12,h-12),c=mse(old,truth,w,12,12,w-12,h-12);
  metrics.put("flat_iid_input_mse",a);metrics.put("flat_iid_new_mse",b);metrics.put("flat_iid_legacy_mse",c);
  check(b<a*.50,"flat random grain attenuated, ratio="+b/a);
  check(b<c*.85,"flat cleanup improves legacy, ratio="+b/c);
  check(Math.abs(mean(q,w,12,12,w-12,h-12)-150)<.35,"flat mean tone maintained");

  // Smoothly correlated grain spans more than one pixel. A small-support-only
  // filter cannot remove it as effectively as the coarse residual support.
  scenarios++;Random r=new Random(9412);int[] coarse=new int[((w+1)/2)*((h+1)/2)];
  for(int i=0;i<coarse.length;i++)coarse[i]=(int)Math.round(7*r.nextGaussian());
  for(int y=0;y<h;y++)for(int x=0;x<w;x++)p[y*w+x]=gray(150+coarse[(y/2)*((w+1)/2)+x/2]+(int)Math.round(2*r.nextGaussian()));
  q=finish(p,w,h,plan(7,4,0),false);old=legacy(p,w,h,false,plan(7,4,0));
  a=mse(p,truth,w,12,12,w-12,h-12);b=mse(q,truth,w,12,12,w-12,h-12);c=mse(old,truth,w,12,12,w-12,h-12);
  metrics.put("correlated_input_mse",a);metrics.put("correlated_new_mse",b);metrics.put("correlated_legacy_mse",c);
  int[] fineOnly=p.clone();QualityShadow1932.smoothRange(p,fineOnly,w,h,0,h,0,h,4,false,1,plan(7,4,0),0);
  double fineError=mse(fineOnly,truth,w,12,12,w-12,h-12);metrics.put("correlated_fine_only_mse",fineError);
  check(b<a*.72,"coarser grain attenuated, ratio="+b/a);
  check(b<c*.90,"coarse support improves legacy, ratio="+b/c);
  check(b<fineError*.95,"coarse residual scale improves fine-only cleanup, ratio="+b/fineError);

  scenarios++;for(int y=0;y<h;y++)for(int x=0;x<w;x++)truth[y*w+x]=gray(25);
  p=random(truth,6,9421);q=finish(p,w,h,plan(6,4,0),true);
  check(mse(q,truth,w,12,12,w-12,h-12)<mse(p,truth,w,12,12,w-12,h-12)*.50,"dark noise attenuated");
  check(Math.abs(mean(q,w,12,12,w-12,h-12)-25)<.4,"shadow mean not crushed or lifted");
 }

 static void lowHaarCorrelated(){
  // A weak fine-scale estimate can coexist with visible two-pixel correlated
  // residual. This synthetic scene has no wallpaper or mesh to erase.
  scenarios++;final int w=256,h=192;final int[] p=new int[w*h],truth=new int[w*h];Arrays.fill(truth,gray(206));
  Random random=new Random(194242);int[] blocks=new int[(w/2)*(h/2)];
  for(int i=0;i<blocks.length;i++)blocks[i]=(int)Math.round(2.8*random.nextGaussian());
  for(int y=0;y<h;y++)for(int x=0;x<w;x++)p[y*w+x]=gray(206+blocks[(y/2)*(w/2)+x/2]+(int)Math.round(.65*random.nextGaussian()));
  SpatialNoise1934 map=SpatialNoise1934.probe(new SpatialNoise1934.Patches(){
   public void read(int[] out,int x,int y,int pw,int ph){for(int row=0;row<ph;row++)System.arraycopy(p,(row+y)*w+x,out,row*pw,pw);}
  },w,h);
  QualityPixels1932.Plan plan=QualityPixels1932.plan(map.global,0,0,2,0,4,0,true,true,1).withLocalNoise(map,4);
  int[] out=finish(p,w,h,plan,true),old=legacy(p,w,h,true,plan);
  double input=mse(p,truth,w,12,12,w-12,h-12),newError=mse(out,truth,w,12,12,w-12,h-12),oldError=mse(old,truth,w,12,12,w-12,h-12);
  metrics.put("correlated_low_haar_sigma",(double)map.global.lumaSigma);metrics.put("correlated_low_haar_input_mse",input);
  metrics.put("correlated_low_haar_1941_mse",oldError);metrics.put("correlated_low_haar_1942_mse",newError);
  check(map.global.lumaSigma<1.5,"controlled low fine-scale estimate");
  check(newError<oldError*.92,"weak Haar estimate still enables correlated residual cleanup: sigma="+map.global.lumaSigma+" ratio="+newError/oldError+" input="+input+" old="+oldError+" new="+newError);
  check(Math.abs(mean(out,w,12,12,w-12,h-12)-206)<.25,"low-sigma wall tone maintained");
 }

 static void spatialAndFaces(){
  scenarios++;final int w=384,h=320;final int[] truth=new int[w*h];Arrays.fill(truth,gray(130));final int[] p=truth.clone();Random random=new Random(41941);
  for(int y=0;y<h;y++)for(int x=0;x<w;x++)p[y*w+x]=gray(130+(int)Math.round((x<128?0:x<256?4:10)*random.nextGaussian()));
  SpatialNoise1934 map=SpatialNoise1934.probe(new SpatialNoise1934.Patches(){
   public void read(int[] out,int x,int y,int pw,int ph){for(int row=0;row<ph;row++)System.arraycopy(p,(row+y)*w+x,out,row*pw,pw);}
  },w,h);
  QualityPixels1932.Plan plan=plan(8,4,0).withLocalNoise(map,4);int[] full=finish(p,w,h,plan,false),tiles=p.clone();
  check(mse(full,p,w,8,8,60,h-8)==0,"clean local patch is unchanged beside noisy patch");
  check(mse(full,truth,w,300,8,w-8,h-8)<mse(p,truth,w,300,8,w-8,h-8)*.55,"spatial noisy patch is cleaned");
  for(int first=0;first<h;first+=29){int count=Math.min(29,h-first),top=Math.max(0,first-4),end=Math.min(h,first+count+4);
   int[] input=Arrays.copyOfRange(p,top*w,end*w),out=input.clone();QualityShadow1932.smoothRange(input,out,w,end-top,first-top,first-top+count,0,end-top,4,false,4,plan,top);
   System.arraycopy(out,(first-top)*w,tiles,first*w,count*w);
  }check(Arrays.equals(full,tiles),"spatial map and absolute row coordinates preserve exact strip equality");
  scenarios++;QualityPixels1932.RegionMask mask=new QualityPixels1932.RegionMask(){public int skinQ8(int x,int y){return 0;}public int detailQ8(int x,int y){return x>300?256:0;}};
  int[] guarded=finish(p,w,h,plan.withFaceRegions(mask),false);
  check(mse(guarded,p,w,310,8,w-8,h-8)<mse(full,p,w,310,8,w-8,h-8)*.10,"eye/lip detail mask reduces smoothing displacement");
  // A rotated/resized save has a fresh noise map in output coordinates. Its
  // noisy background must not use the low source/global estimate by accident.
  scenarios++;QualityPixels1932.Plan sharpBase=QualityPixels1932.plan(new QualityPixels1932.NoiseStats(2,2,130,0,4096),400,10000000L,2,0,0,4,true,true,2f);
  int[] mapped=new int[p.length],unmapped=new int[p.length];
  QualityPixels1932.finishStrip(p,mapped,w,h,0,h,sharpBase.withOutputNoise(map).withFaceRegions(mask).withHaloSuppression(true),false,true);
  QualityPixels1932.finishStrip(p,unmapped,w,h,0,h,sharpBase.withLocalNoise(map,0),false,true);
  double mappedError=mse(mapped,truth,w,300,8,w-8,h-8),unmappedError=mse(unmapped,truth,w,300,8,w-8,h-8);
  metrics.put("rescaled_output_map_sharp_mse",mappedError);metrics.put("source_coordinate_fallback_sharp_mse",unmappedError);
  check(mappedError<unmappedError*.98,"output-coordinate residual map reduces grain sharpening after rescaling");
 }

 static void structure(){
  int w=128,h=96;int[] truth=new int[w*h];
  scenarios++;for(int y=0;y<h;y++)for(int x=0;x<w;x++)truth[y*w+x]=gray(x<64?45:205);
  int[] q=finish(truth,w,h,plan(8,4,0),true);check(Arrays.equals(truth,q),"clean strong edge exact");
  int[] p=random(truth,6,14941);q=finish(p,w,h,plan(6,4,0),true);
  double step=mean(q,w,65,8,69,h-8)-mean(q,w,59,8,63,h-8);
  metrics.put("noisy_step_contrast",step);check(step>156,"strong edge retained");
  scenarios++;for(int y=0;y<h;y++)for(int x=0;x<w;x++)truth[y*w+x]=gray((x+y)%2==0?110:150);
  q=finish(truth,w,h,plan(8,4,0),false);check(mse(q,truth,w,8,8,w-8,h-8)==0,"period-two woven luma texture exact in supported interior");
  for(int period:new int[]{3,4})for(int direction=0;direction<4;direction++){
   scenarios++;for(int y=0;y<h;y++)for(int x=0;x<w;x++){
    int phase=direction==0?x:direction==1?y:direction==2?x+y:x-y;
    truth[y*w+x]=gray(134+(int)Math.round(6*Math.cos(2*Math.PI*phase/period)));
   }
   q=finish(truth,w,h,plan(2,4,0),false);
   check(mse(q,truth,w,8,8,w-8,h-8)==0,"low contrast period"+period+" directional texture exact "+direction);
  }
  scenarios++;for(int y=0;y<h;y++)for(int x=0;x<w;x++)truth[y*w+x]=((x/4+y/4)&1)==0?rgb(230,20,30):rgb(20,30,230);
  check(Arrays.equals(truth,finish(truth,w,h,plan(8,4,0),false)),"saturated colour boundary and woven motif exact");
  scenarios++;Arrays.fill(truth,gray(160));for(int y=20;y<h-20;y++)for(int x=30;x<95;x++)if(x%7==0||y%9==0)truth[y*w+x]=gray(30);
  q=finish(truth,w,h,plan(8,4,0),false);check(Arrays.equals(truth,q),"high contrast fine text-like strokes exact");
 }

 static void invariants(){
  int w=96,h=80;int[] p=new int[w*h];
  scenarios++;for(int level:new int[]{0,1,2,3,4})for(int tone:new int[]{0,1,8,25,128,240,254,255}){
   Arrays.fill(p,gray(tone));check(Arrays.equals(p,finish(p,w,h,plan(8,level,0),true)),"constant tone exact "+level+" "+tone);
  }
  Arrays.fill(p,rgb(242,31,64));check(Arrays.equals(p,finish(p,w,h,plan(8,4,0),false)),"constant saturated tone exact");
  scenarios++;for(int i=0;i<p.length;i++)p[i]=(i*1079)&0x00ffffff|((i%255)<<24);
  check(Arrays.equals(p,finish(p,w,h,plan(8,4,0),true)),"non-opaque pixels preserved exactly");
  for(int y=0;y<h;y++)for(int x=0;x<w;x++)p[y*w+x]=x<w/2?0x0000ff20:gray(120);
  check(Arrays.equals(p,finish(p,w,h,plan(8,4,0),true)),"opaque boundary does not mix invisible RGB into visible flat tone");
  scenarios++;Arrays.fill(p,gray(128));p=random(p,8,10194);check(Arrays.equals(p,finish(p,w,h,plan(8,0,0),true)),"NR OFF exact");
  for(int level=1;level<=4;level++){
   int[] weaker=finish(p,w,h,plan(8,level,0),true);
   if(level>1){int[] previous=finish(p,w,h,plan(8,level-1,0),true);check(mse(weaker,p,w,8,8,w-8,h-8)>=mse(previous,p,w,8,8,w-8,h-8),"selected noise levels remain ordered "+level);}
  }
  int[] q=new int[p.length];QualityPixels1932.finishStrip(p,q,w,h,0,h,plan(8,0,0),false,true);check(Arrays.equals(p,q),"sharp OFF exact");
  final int[] clean=new int[p.length];Arrays.fill(clean,gray(128));SpatialNoise1934 map=SpatialNoise1934.probe(new SpatialNoise1934.Patches(){
   public void read(int[] out,int x,int y,int pw,int ph){for(int row=0;row<ph;row++)System.arraycopy(clean,(row+y)*96+x,out,row*pw,pw);}
  },w,h);
  check(Arrays.equals(p,finish(p,w,h,plan(8,4,0).withLocalNoise(map,4),true)),"measured clean patch blocks unnecessary final cleanup");
  scenarios++;for(int y=0;y<h;y++)for(int x=0;x<w;x++)p[y*w+x]=rgb(80+x,50+x,30+x);
  q=finish(p,w,h,plan(8,4,0),false);check(mse(q,p,w,8,8,w-8,h-8)<.05,"smooth tonal gradient avoids banding or bending");
 }

 static void tiling() throws Exception {
  scenarios++;final int w=127,h=141;int[] base=new int[w*h];Arrays.fill(base,gray(110));final int[] p=random(base,8,19413);
  final QualityPixels1932.Plan plan=plan(8,4,0);final int[] full=finish(p,w,h,plan,true),tiles=p.clone();
  ExecutorService pool=Executors.newFixedThreadPool(4);List<Future<?>> jobs=new ArrayList<Future<?>>();
  for(int f=0;f<h;f+=19){final int first=f;jobs.add(pool.submit(new Runnable(){public void run(){int count=Math.min(19,h-first),top=Math.max(0,first-4),end=Math.min(h,first+count+4);
   int[] input=Arrays.copyOfRange(p,top*w,end*w),out=input.clone();
   QualityShadow1932.smoothRange(input,out,w,end-top,first-top,first-top+count,0,end-top,4,true,4,plan,top);
   System.arraycopy(out,(first-top)*w,tiles,first*w,count*w);
  }}));}
  for(Future<?> job:jobs)job.get();pool.shutdown();check(Arrays.equals(full,tiles),"NR whole frame equals four-worker halo strips exactly");
  int[] fullSharp=new int[p.length],tileSharp=new int[p.length];QualityPixels1932.finishStrip(p,fullSharp,w,h,0,h,plan(8,0,4),false,true);
  for(int first=0;first<h;first+=19){int count=Math.min(19,h-first),top=Math.max(0,first-QualityPixels1932.HALO),end=Math.min(h,first+count+QualityPixels1932.HALO);
   int[] input=Arrays.copyOfRange(p,top*w,end*w),out=input.clone();QualityPixels1932.finishStripAt(input,out,w,end-top,first-top,first-top+count,plan(8,0,4),false,true,top);
   System.arraycopy(out,(first-top)*w,tileSharp,first*w,count*w);
  }check(Arrays.equals(fullSharp,tileSharp),"noise-aware sharpening whole frame equals halo strips exactly");
 }

 static void sharpening(){
  scenarios++;int w=160,h=128;int[] truth=new int[w*h];Arrays.fill(truth,gray(150));int[] p=random(truth,5,4194),out=new int[p.length];
  QualityPixels1932.finishStrip(p,out,w,h,0,h,plan(5,0,4),false,true);
  double ratio=mse(out,truth,w,8,8,w-8,h-8)/mse(p,truth,w,8,8,w-8,h-8);metrics.put("flat_sharpen_noise_mse_ratio",ratio);
  check(ratio<1.02,"flat noise not materially amplified by maximum sharpen, ratio="+ratio);
  scenarios++;for(int y=0;y<h;y++)for(int x=0;x<w;x++)p[y*w+x]=gray(x<77?90:x==77?94:x==78?110:x==79?150:x==80?174:x==81?188:190);
  QualityPixels1932.finishStrip(p,out,w,h,0,h,plan(2,0,4),false,true);int changed=0;for(int i=0;i<p.length;i++)if(p[i]!=out[i])changed++;
  metrics.put("real_edge_sharpen_changed_pixels",(double)changed);check(changed>0,"coherent real edge keeps sharpening");
  for(int i=0;i<p.length;i++)check(QualityPixels1932.luma(out[i])>=90&&QualityPixels1932.luma(out[i])<=190,"edge sharpen has no overshoot");
 }

 public static void main(String[] args) throws Exception {
  noise();lowHaarCorrelated();structure();invariants();tiling();spatialAndFaces();sharpening();
  System.out.print("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+",\"metrics\":{");int n=0;
  for(Map.Entry<String,Double> e:metrics.entrySet())System.out.printf(Locale.ROOT,"%s\"%s\":%.8f",n++==0?"":",",e.getKey(),e.getValue());
  System.out.println("},\"physical_device_verified\":false}");
 }
}
