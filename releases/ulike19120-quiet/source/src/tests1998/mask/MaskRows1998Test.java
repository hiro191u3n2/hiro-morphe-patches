package com.hiro.ulike;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
public final class MaskRows1998Test {
 static long checks,pixels,checksum;static long oldReads,newReads;static int cases,rotated;
 static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
 static void raw(double expected,double actual,String why){check(Double.doubleToRawLongBits(expected)==Double.doubleToRawLongBits(actual),why);}
 static double horizontal(GpuFinishPolicy1992.Sampler s,int x,int row){int x0=s.x.lo[x];if(x0<0)return 0;int x1=s.x.hi[x];double fx=s.x.fraction[x];return (s.values[row+x0]&255)*(1-fx)+(s.values[row+x1]&255)*fx;}
 static void compare(GpuFinishPolicy1992.Fixture f,boolean smoothing,int[] rowOrder){
  GpuFinishPolicy1992.Sampler s=GpuFinishPolicy1992.sampler(smoothing?f.plan.smoothedRegions:f.plan.faceRegions,f.width,f.height,smoothing);
  if(s==null)return;
  GpuFinishPolicy1992.RowSampler1998[] rows=GpuFinishPolicy1992.rowSamplers1998(s,f.width);
  if(!s.x.horizontal||s.y.horizontal){check(rows==null,"rotated axes keep original sampler");rotated++;return;}
  check(rows!=null&&rows.length==4,"one bounded pair for each logical worker");
  for(int i=0;i<4;i++)for(int j=0;j<i;j++)check(rows[i]!=rows[j]&&rows[i].a!=rows[j].a&&rows[i].b!=rows[j].b,"workers never share writable rows");
  int worker=0;
  for(int y:rowOrder){
   // Change logical worker only at band boundaries; revisit rows and jump in
   // both directions so neither thread identity nor monotonic y is required.
   if((y&31)==0)worker=(worker+1)&3;
   GpuFinishPolicy1992.RowSampler1998 cached=rows[worker];cached.prepare(y);
   for(int x=0;x<f.width;x++){
    int expected=smoothing?f.plan.smoothedRegions.smoothingQ8(x,y):f.plan.faceRegions.skinQ8(x,y);
    check(cached.sample(x)==expected,"unchanged complete callback oracle");check(cached.sample(x)==s.sample(x,y),"unchanged axis sampler");
    if(s.x.lo[x]>=0&&s.y.lo[y]>=0){raw(horizontal(s,x,s.y.lo[y]),cached.top[x],"horizontal top raw double");raw(horizontal(s,x,s.y.hi[y]),cached.bottom[x],"horizontal bottom raw double");oldReads+=4;}
    pixels++;
   }
  }
  for(GpuFinishPolicy1992.RowSampler1998 row:rows)newReads+=row.rowsBuilt*2*f.width;
  cases++;
 }
 static void corpus()throws Exception{
  for(int id=0;id<24;id++)for(int size=0;size<2;size++){
   int width=size==0?257:4284,height=size==0?263:5712;
   GpuFinishPolicy1992.Fixture f=GpuFinishPolicy1992.fixture(id,width,height);
   int n=size==0?height:160;int[] order=new int[n*2+4];
   for(int i=0;i<n;i++){order[i]=i;order[n+i]=height-1-i;}
   order[n*2]=height/2;order[n*2+1]=height/2;order[n*2+2]=0;order[n*2+3]=height-1;
   compare(f,false,order);compare(f,true,order);
  }
 }
 static void invalidAndCancel(){
  final double[] values={Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,-1.0,-0.0,0.0,0.5,1.0,1.5,2.0};
  for(boolean smooth:new boolean[]{false,true}){
   GpuFinishPolicy1992.Axis x=new GpuFinishPolicy1992.Axis(values.length,true,2,1,new GpuFinishPolicy1992.Coordinate(){public double at(int i){return values[i];}},smooth,2);
   GpuFinishPolicy1992.Axis y=new GpuFinishPolicy1992.Axis(values.length,false,2,2,new GpuFinishPolicy1992.Coordinate(){public double at(int i){return values[i];}},smooth,2);
   GpuFinishPolicy1992.Sampler s=new GpuFinishPolicy1992.Sampler(new byte[]{0,(byte)255,(byte)255,0},x,y,smooth);
   GpuFinishPolicy1992.RowSampler1998 r=GpuFinishPolicy1992.rowSamplers1998(s,values.length)[0];
   for(int row=0;row<values.length;row++){r.prepare(row);for(int col=0;col<values.length;col++)check(r.sample(col)==s.sample(col,row),"NaN infinite signedzero edges unchanged");}
   Thread.currentThread().interrupt();boolean stopped=false;
   try{r.prepare(0);}catch(CancellationException expected){stopped=true;}finally{check(Thread.currentThread().isInterrupted(),"interruption retained");Thread.interrupted();}
   check(stopped,"cache observes cancellation before filling");r.prepare(5);for(int col=0;col<values.length;col++)check(r.sample(col)==s.sample(col,5),"subsequent valid prepare has no partial cache");
  }
  check(GpuFinishPolicy1992.rowSamplers1998(null,100)==null,"absent descriptor original path");
  check(GpuFinishPolicy1992.rowSamplers1998(new GpuFinishPolicy1992.Sampler(null,null,null,false),100)==null,"unreliable face uses original zero sampler");
 }
 static long scan(GpuFinishPolicy1992.Sampler s,boolean cache,int width,int height){
  GpuFinishPolicy1992.RowSampler1998 row=cache?new GpuFinishPolicy1992.RowSampler1998(s,width):null;
  long started=System.nanoTime(),sum=0;
  for(int y=0;y<height;y++){if(cache)row.prepare(y);for(int x=0;x<width;x++)sum+=cache?row.sample(x):s.sample(x,y);}
  long time=System.nanoTime()-started;checksum+=sum;return time;
 }
 static long[] benchmark()throws Exception{
  GpuFinishPolicy1992.Fixture f=GpuFinishPolicy1992.fixture(10,4284,5712);
  GpuFinishPolicy1992.Sampler s=GpuFinishPolicy1992.sampler(f.plan.faceRegions,f.width,f.height,false);
  check(s!=null&&s.x.horizontal&&!s.y.horizontal,"photo-sized 180 degree benchmark admitted");
  for(int i=0;i<2;i++){scan(s,false,f.width,128);scan(s,true,f.width,128);}
  long[] base=new long[5],cached=new long[5];
  for(int i=0;i<5;i++){if((i&1)==0){base[i]=scan(s,false,f.width,f.height);cached[i]=scan(s,true,f.width,f.height);}else{cached[i]=scan(s,true,f.width,f.height);base[i]=scan(s,false,f.width,f.height);}}
  Arrays.sort(base);Arrays.sort(cached);return new long[]{base[2],cached[2]};
 }
 public static void main(String[] args)throws Exception{
  corpus();invalidAndCancel();long[] timing=benchmark();check(cases>=20&&rotated>=20&&pixels>5000000,"complete mask cases and fallbacks exercised");
  check(newReads<oldReads/2,"less than half the immutable byte reads and horizontal interpolation pairs");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"compared_pixels\":"+pixels+",\"cache_cases\":"+cases+",\"rotated_fallback_cases\":"+rotated+",\"baseline_mask_byte_reads\":"+oldReads+",\"cached_mask_byte_reads_upper_bound\":"+newReads+",\"host_baseline_median_ns\":"+timing[0]+",\"host_cached_median_ns\":"+timing[1]+",\"checksum\":"+checksum+",\"raw_double_intermediates_equal\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
