package com.hiro.ulike;
import java.util.*;
/** Numerical host tests for production spatial policy and long-period chroma code. */
public final class SpatialQuality1934Test {
 static int assertions;static Map<String,Double> metrics=new LinkedHashMap<String,Double>();
 static void check(boolean v,String m){assertions++;if(!v)throw new AssertionError(m);}
 static int gray(int n){n=Math.max(0,Math.min(255,n));return 0xff000000|(n<<16)|(n<<8)|n;}
 static int rgb(int r,int g,int b){return 0xff000000|(r<<16)|(g<<8)|b;}
 static int luma(int p){return QualityPixels1932.luma(p);}
 static double variance(int[] p,int w,int x0,int y0,int x1,int y1,boolean chroma){
  double s=0,sq=0;int n=0;
  for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++){
   int pixel=p[y*w+x];double v=chroma?(pixel&255)-((pixel>>>8)&255):luma(pixel);s+=v;sq+=v*v;n++;
  }return sq/n-s*s/n/n;
 }
 static SpatialNoise1934 map(final int[] p,final int w,final int h){return SpatialNoise1934.probe(new SpatialNoise1934.Patches(){
  public void read(int[] q,int x,int y,int pw,int ph){for(int row=0;row<ph;row++)System.arraycopy(p,(y+row)*w+x,q,row*pw,pw);}
 },w,h);}
 static QualityPixels1932.Plan plan(int noise){return QualityPixels1932.plan(new QualityPixels1932.NoiseStats(8,12,100,0,1000),400,10000000L,2,1,noise,0,true,true,1);}
 static int[] finish(int[] p,int w,int h){int[] out=new int[p.length];QualityPixels1932.finishStrip(p,out,w,h,0,h,plan(0),true,false);return out;}
 static void localNoise(){
  int w=768,h=512;int[] p=new int[w*h];Random r=new Random(1934);
  for(int y=0;y<h;y++)for(int x=0;x<w;x++)p[y*w+x]=gray((int)Math.round(100+(x<w/2?1.0:12.0)*r.nextGaussian()));
  SpatialNoise1934 m=map(p,w,h);metrics.put("sigma_clean",(double)m.sigmaAt(100,200));metrics.put("sigma_noisy",(double)m.sigmaAt(650,200));
  check(m.sigmaAt(100,200)<2,"clean local estimate");check(m.sigmaAt(650,200)>8,"noisy local estimate");
  check(m.budgetQ8(100,200)<12,"clean area retained");check(m.budgetQ8(650,200)>240,"noisy area full user budget");
  int maxStep=0;for(int x=1;x<w;x++)maxStep=Math.max(maxStep,Math.abs(m.budgetQ8(x,200)-m.budgetQ8(x-1,200)));
  metrics.put("maximum_budget_step_q8",(double)maxStep);check(maxStep<=5,"smooth spatial budget without grid seam");
  QualityPixels1932.Plan q=plan(3).withLocalNoise(m,3);check(q.noiseLevel==3,"user max unchanged");check(plan(0).withLocalNoise(m,0).noiseLevel==0,"OFF intact");
  int[] blurred=p.clone();for(int y=1;y<h-1;y++)for(int x=1;x<w-1;x++){
   int sum=0;for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)sum+=luma(p[(y+dy)*w+x+dx]);blurred[y*w+x]=gray((sum+4)/9);
  }
  QualityPixels1932.localDenoiseMix(p,blurred,w,1,h-1,0,q);
  double cleanRatio=variance(blurred,w,80,80,200,400,false)/variance(p,w,80,80,200,400,false);
  double noisyRatio=variance(blurred,w,550,80,680,400,false)/variance(p,w,550,80,680,400,false);
  metrics.put("nr_clean_variance_ratio",cleanRatio);metrics.put("nr_noisy_variance_ratio",noisyRatio);
  check(cleanRatio>.90,"clean image not globally over-smoothed");check(noisyRatio<.16,"noisy area retains selected NR");
  int[] flat=new int[w*h];Arrays.fill(flat,gray(100));SpatialNoise1934 clean=map(flat,w,h);
  int[] altered=flat.clone();Arrays.fill(altered,gray(98));QualityPixels1932.localDenoiseMix(flat,altered,w,0,h,0,plan(4).withLocalNoise(clean,4));
  check(Arrays.equals(flat,altered),"zero-noise map blocks unnecessary smoothing exactly");
  int[] tiled=p.clone();for(int first=0;first<h;first+=63){
   int count=Math.min(63,h-first),top=Math.max(0,first-1),end=Math.min(h,first+count+1);
   int[] src=Arrays.copyOfRange(p,top*w,end*w),dst=Arrays.copyOfRange(blurred,top*w,end*w);
   // Test the absolute-map lookup using a known candidate independent of tile layout.
   for(int i=0;i<dst.length;i++)dst[i]=gray(100);
   QualityPixels1932.localDenoiseMix(src,dst,w,first-top,first-top+count,top,q);System.arraycopy(dst,(first-top)*w,tiled,first*w,count*w);
  }
  int[] whole=new int[p.length];Arrays.fill(whole,gray(100));QualityPixels1932.localDenoiseMix(p,whole,w,0,h,0,q);
  check(Arrays.equals(whole,tiled),"per-pixel spatial mix tiled equivalence");
 }
 static void moire(){
  int w=256,h=224;
  for(int direction=0;direction<4;direction++)for(int period:new int[]{6,8,12,16}){
   int[] p=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++){
    int pos=direction==0?x:direction==1?y:direction==2?x+y:x-y;
    int c=(int)Math.round(13*Math.cos(2*Math.PI*pos/period));p[y*w+x]=rgb(150+c,150,150-3*c);
   }
   int[] out=finish(p,w,h);double ratio=variance(out,w,40,40,w-40,h-40,true)/variance(p,w,40,40,w-40,h-40,true);
   metrics.put("long_moire_ratio_d"+direction+"_p"+period,ratio);check(ratio<.83,"long period attenuated "+direction+" "+period+" ratio="+ratio);
   int maxLuma=0;for(int i=0;i<p.length;i++)maxLuma=Math.max(maxLuma,Math.abs(luma(p[i])-luma(out[i])));check(maxLuma<=1,"luminance maintained");
  }
  for(int period:new int[]{6,8,12,16}){
   int[] motif=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++){
    int c=(int)Math.round(20*Math.cos(2*Math.PI*x/period));motif[y*w+x]=rgb(165+c,135+c,110+c);
   }check(Arrays.equals(motif,finish(motif,w,h)),"warm real woven motif preserved "+period);
   for(int y=0;y<h;y++)for(int x=0;x<w;x++)motif[y*w+x]=(x%period)<period/2?rgb(220,35,25):rgb(25,40,220);
   check(Arrays.equals(motif,finish(motif,w,h)),"saturated color motif preserved "+period);
  }
  int[] lines=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++)lines[y*w+x]=x==128?rgb(190,148,98):gray(150);
  check(Arrays.equals(lines,finish(lines,w,h)),"isolated warm line preserved");
  for(int y=0;y<h;y++)for(int x=0;x<w;x++)lines[y*w+x]=rgb(70+x/3,60+x/3,45+x/3);
  check(Arrays.equals(lines,finish(lines,w,h)),"warm gradient preserved");
  int[] pattern=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++){
   int c=(int)Math.round(13*Math.cos(2*Math.PI*(x+y)/16));pattern[y*w+x]=rgb(150+c,150,150-3*c);
  }
  int[] entire=finish(pattern,w,h),tiled=new int[pattern.length];int halo=QualityPixels1932.HALO;
  for(int first=0;first<h;first+=37){int count=Math.min(37,h-first),top=Math.max(0,first-halo),end=Math.min(h,first+count+halo);
   int[] src=Arrays.copyOfRange(pattern,top*w,end*w),out=new int[src.length];
   QualityPixels1932.finishStripAt(src,out,w,end-top,first-top,first-top+count,plan(0),true,false,top);
   System.arraycopy(out,(first-top)*w,tiled,first*w,count*w);
  }check(Arrays.equals(entire,tiled),"long moire full frame and strips identical");
 }
 static void faceBudgets(){
  QualityPixels1932.RegionMask masks=new QualityPixels1932.RegionMask(){public int skinQ8(int x,int y){return x<50?256:0;}public int detailQ8(int x,int y){return x<10?256:0;}};
  QualityPixels1932.Plan p=plan(4).withFaceRegions(masks);int skin=rgb(190,145,120);
  check(QualityPixels1932.denoiseBudgetQ8(skin,1,p,5,5)<QualityPixels1932.denoiseBudgetQ8(skin,1,p,30,5),"eyes/lips protected beyond skin");
  check(QualityPixels1932.shadowBudgetQ8(skin,1,p,30,5)<QualityPixels1932.shadowBudgetQ8(skin,1,p,70,5),"same-colored background is not facial skin");
  check(QualityPixels1932.shadowBudgetQ8(skin,1,plan(4))==plan(4).shadowBudgetQ8,"unknown faces do not become color-only skin masks");
 }
 public static void main(String[] args){localNoise();moire();faceBudgets();System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"metrics\":{");int i=0;for(Map.Entry<String,Double> e:metrics.entrySet())System.out.printf(Locale.ROOT,"%s\"%s\":%.8f",i++==0?"":",",e.getKey(),e.getValue());System.out.println("}}");}
}
