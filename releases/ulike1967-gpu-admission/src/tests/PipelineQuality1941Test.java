package com.hiro.ulike;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
/** Final-save integration properties. Android/primary-NR fixtures are explicit;
 * production residual NR, spatial probing, resample, ownership and final sharp run.
 */
public final class PipelineQuality1941Test {
 static int assertions,scenarios;static final Map<String,Object> metrics=new LinkedHashMap<String,Object>();
 static void check(boolean b,String message){assertions++;if(!b)throw new AssertionError(message);}
 static PhotoDetail.Settings options(boolean noise,boolean sharp){return new PhotoDetail.Settings(noise,3,sharp,4,false,true,true);}
 static int[] grain(int w,int h,int amplitude,int mean,long seed){int[] p=new int[w*h];Random r=new Random(seed);for(int i=0;i<p.length;i++){int g=mean+r.nextInt(amplitude*2+1)-amplitude;p[i]=0xff000000|(g<<16)|(g<<8)|g;}return p;}
 static Bitmap bitmap(int w,int h,int[] p){return Bitmap.from(w,h,p,Bitmap.Config.ARGB_8888,true);}
 static ShotContext1932.Snapshot metadata(int iso){return new ShotContext1932.Snapshot(iso,30000000L,ShotContext1932.LENS_FRONT,true,.4f);}
 static void bind(PhotoDetail.Settings s,boolean chroma,int w,int h){PhotoDetail.REQUEST.set(s);AsyncSave1935.CAPTURED.remove();AsyncSave1935.COLOUR.remove();ChromaPipeline177.enabled=chroma;SaveQuality2.SIZE.set(new int[]{w,h});HostAudit1932.EVENTS.get().clear();SaveQuality2.FALLBACK.set(0);}
 static double variance(int[] p,int width,int x,int y,int w,int h){double sum=0,sum2=0;int n=w*h;for(int row=y;row<y+h;row++)for(int col=x;col<x+w;col++){double a=QualityPixels1932.luma(p[row*width+col]);sum+=a;sum2+=a*a;}return Math.max(0,sum2/n-(sum/n)*(sum/n));}
 static SpatialNoise1934 probe(final Bitmap b){return SpatialNoise1934.probe(new SpatialNoise1934.Patches(){public void read(int[] p,int x,int y,int w,int h){b.getPixels(p,0,w,x,y,w,h);}},b.getWidth(),b.getHeight());}
 static int countDifferent(int[] a,int[] b){int n=0;for(int i=0;i<a.length;i++)if(a[i]!=b[i])n++;return n;}
 static void normalize(PhotoDetail.Settings settings,boolean colour,int turn,int ow,int oh){
  int w=192,h=145;int[] p=grain(w,h,18,119,1941);Bitmap source=bitmap(w,h,p);ShotContext1932.Snapshot shot=metadata(800);ShotContext1932.SHOTS.put(source,shot);
  bind(settings,colour,ow,oh);QualityPixels1932.Plan outer=QualityPixels1932.plan(null,0,0,0,0,0,0,false,false,1);
  PhotoDetail.Settings previous=options(false,false);HostAudit1932.<QualityPixels1932.Plan>local("CURRENT").set(outer);HostAudit1932.<PhotoDetail.Settings>local("LEGACY").set(previous);
  Bitmap output=QualityPipeline1932.normalize(source,turn,false);
  check(output.getWidth()==ow&&output.getHeight()==oh,"exact final dimensions "+turn);
  check(ShotContext1932.forBitmap(output)==shot,"shot metadata follows exact final saved bitmap");
  check(Arrays.equals(source.snapshot(),p)&&!source.isRecycled(),"input bitmap retained without writes");
  check(HostAudit1932.<QualityPixels1932.Plan>local("CURRENT").get()==outer,"outer plan restored");
  check(HostAudit1932.<PhotoDetail.Settings>local("LEGACY").get()==previous,"outer legacy settings restored");
  check(SaveQuality2.FALLBACK.get()==0,"valid prepare avoids fallback");
  PhotoDetail.Settings actual=ChromaPipeline177.LAST_SETTINGS.get();
  check(actual.noiseOn==(settings.noiseOn&&settings.noiseLevel>0)&&!actual.sharpOn,"source has NR only; sharp deferred until final size");
  List<String> events=HostAudit1932.EVENTS.get();int filter=-1,resize=-1;for(int i=0;i<events.size();i++){if(events.get(i).startsWith("source:"))filter=i;if(events.get(i).startsWith("resample:"))resize=i;}
  check(filter>=0&&resize>=0,"source processing and resize both reached");
  boolean reduced=ow<(turn==90||turn==270?h:w)||oh<(turn==90||turn==270?w:h);
  check(reduced?resize<filter:filter<resize,"bounded reduction first, otherwise source cleanup precedes resize");
  int before=ChromaPipeline177.APPLIES.get();int[] finalPixels=output.snapshot();
  check(QualityPipeline1932.applyDetail(output,source,settings)==output,"identity-bound completion reused");
  check(ChromaPipeline177.APPLIES.get()==before&&Arrays.equals(output.snapshot(),finalPixels),"prepared final image never filtered twice");
  check(!HostAudit1932.map("PREPARED").containsKey(output),"completion consumed without retaining bitmap");
  HostAudit1932.local("CURRENT").remove();HostAudit1932.local("LEGACY").remove();
  if(output!=source)output.recycle();source.recycle();scenarios++;
 }
 static void residualAndSharp(){
  int w=224,h=389;int[] p=grain(w,h,24,116,771);Bitmap source=bitmap(w,h,p);ShotContext1932.SHOTS.put(source,metadata(1200));
  bind(options(true,false),false,w,h);Bitmap denoised=QualityPipeline1932.normalize(source,0,false);QualityPipeline1932.applyDetail(denoised,source,options(true,false));
  double original=variance(p,w,12,12,w-24,h-24),clean=variance(denoised.snapshot(),w,12,12,w-24,h-24);
  check(clean<original*.70,"NR ON with chroma OFF changes final bitmap and lowers flat-wall variance");
  bind(options(true,true),false,w,h);Bitmap sharp=QualityPipeline1932.normalize(source,0,false);QualityPipeline1932.applyDetail(sharp,source,options(true,true));
  double finalVariance=variance(sharp.snapshot(),w,12,12,w-24,h-24);
  check(finalVariance<=clean*1.03+.10,"output sharpen retains residual noise reduction");
  check(Arrays.equals(source.snapshot(),p),"noise/sharp processing never writes original capture");
  metrics.put("flat_original_variance",original);metrics.put("final_nr_variance",clean);metrics.put("final_nr_sharp_variance",finalVariance);
  source.recycle();denoised.recycle();sharp.recycle();scenarios++;
 }
 static void snapshotOptions(){
  int w=144,h=113;int[] p=grain(w,h,21,112,822);Bitmap source=bitmap(w,h,p);bind(options(true,true),true,w,h);
  PhotoDetail.Settings captured=options(false,false);AsyncSave1935.CAPTURED.set(captured);AsyncSave1935.COLOUR.set(Boolean.FALSE);
  Bitmap finalBitmap=QualityPipeline1932.normalize(source,0,false);
  check(Arrays.equals(finalBitmap.snapshot(),p),"captured OFF options override later live ON changes");
  check(!ChromaPipeline177.LAST_SETTINGS.get().noiseOn&&!ChromaPipeline177.LAST_SETTINGS.get().sharpOn,"same snapshot propagated to primary path");
  check(!ChromaPipeline177.LAST_PLAN.get().noiseMapAtOutput,"source plan retains source-domain grid flag");
  QualityPipeline1932.applyDetail(finalBitmap,source,captured);AsyncSave1935.CAPTURED.remove();AsyncSave1935.COLOUR.remove();
  if(finalBitmap!=source)finalBitmap.recycle();source.recycle();scenarios++;
 }
 static int[] quadrants(int w,int h){int[] p=new int[w*h];Random r=new Random(4322);for(int y=0;y<h;y++)for(int x=0;x<w;x++){
  boolean noisy=(x<w/2)^(y<h/2);int spread=noisy?32:3;
  int structure=((x/3+y/4)%2==0?18:-18);int g=116+structure+r.nextInt(spread*2+1)-spread;
  p[y*w+x]=0xff000000|(g<<16)|(g<<8)|g;
 }return p;}
 static void outputCoordinates(int turn,int ow,int oh){
  final int w=385,h=291;int[] p=quadrants(w,h);Bitmap source=bitmap(w,h,p);ShotContext1932.SHOTS.put(source,metadata(1200));
  bind(options(true,false),false,ow,oh);Bitmap clean=QualityPipeline1932.normalize(source,turn,false);QualityPipeline1932.applyDetail(clean,source,options(true,false));
  bind(options(true,true),false,ow,oh);Bitmap actual=QualityPipeline1932.normalize(source,turn,false);
  QualityPixels1932.Plan sourcePlan=ChromaPipeline177.LAST_PLAN.get();
  check(sourcePlan!=null&&!sourcePlan.noiseMapAtOutput,"source map tagged in original coordinates");
  Bitmap expected=clean.copy(Bitmap.Config.ARGB_8888,true);SpatialNoise1934 outputMap=probe(clean);
  QualityPixels1932.Plan finalPlan=sourcePlan.withOutputNoise(outputMap);
  check(finalPlan.noiseMapAtOutput&&finalPlan.localNoise.width==ow&&finalPlan.localNoise.height==oh,"final map measured in exact output domain");
  check(finalPlan.sharpGainQ8==sourcePlan.sharpGainQ8&&finalPlan.sharpFloorQ8==sourcePlan.sharpFloorQ8&&finalPlan.sourceSigma==sourcePlan.sourceSigma,"geometry-map replacement preserves captured source gain/floor");
  QualityPipeline1932.finishInPlace(expected,finalPlan,false,true);
  check(Arrays.equals(actual.snapshot(),expected.snapshot()),"final saved pixels use correctly oriented/cropped noise map turn "+turn);
  Bitmap stale=clean.copy(Bitmap.Config.ARGB_8888,true);QualityPipeline1932.finishInPlace(stale,sourcePlan,false,true);
  int detected=countDifferent(stale.snapshot(),actual.snapshot());metrics.put("changed_map_pixels_turn"+turn+"_"+ow+"x"+oh,detected);
  check(detected>0,"heterogeneous fixture detects a stale source map after geometry change");
  QualityPipeline1932.applyDetail(actual,source,options(true,true));clean.recycle();actual.recycle();expected.recycle();stale.recycle();source.recycle();scenarios++;
 }
 static void completionIdentity(){
  int w=72,h=65;int[] p=grain(w,h,8,117,88);Bitmap source=bitmap(w,h,p),other=bitmap(w,h,p);bind(options(false,false),false,w,h);Bitmap output=QualityPipeline1932.normalize(source,0,false);
  int before=ChromaPipeline177.APPLIES.get();QualityPipeline1932.applyDetail(output,other,options(false,false));
  check(ChromaPipeline177.APPLIES.get()==before+1,"another capture cannot consume completion as its own");
  check(!HostAudit1932.map("PREPARED").containsKey(output),"mismatched identity completion removed");
  if(output!=source)output.recycle();source.recycle();other.recycle();scenarios++;
 }
 static void fallback(boolean oom){
  int w=193,h=145;int[] p=grain(w,h,16,110,5);Bitmap source=bitmap(w,h,p);bind(options(false,false),false,320,241);
  if(oom)Bitmap.failCreateOnce=true;else Bitmap.failNextCreatedWrite=true;
  Bitmap output=QualityPipeline1932.normalize(source,0,false);
  check(SaveQuality2.FALLBACK.get()==1,"preparation failure returns to original normalization");
  check(Arrays.equals(source.snapshot(),p)&&!source.isRecycled(),"failure keeps original capture intact");
  check(HostAudit1932.current()==null&&HostAudit1932.local("LEGACY").get()==null,"failure clears helper thread-local state");
  check(!HostAudit1932.map("PREPARED").containsKey(output),"failure does not claim final image already prepared");
  int before=ChromaPipeline177.APPLIES.get();QualityPipeline1932.applyDetail(output,source,options(false,false));
  check(ChromaPipeline177.APPLIES.get()==before+1,"failed preparation permits established detail fallback");
  check(Bitmap.writesAfterRecycle.get()==0,"workers drain before any recycle");
  if(output!=source)output.recycle();source.recycle();scenarios++;
 }
 static void unreadable(){
  int w=48,h=37;int[] p=grain(w,h,4,111,8);Bitmap gain=bitmap(w,h,p),wide=bitmap(w,h,p),f16=Bitmap.from(w,h,p,Bitmap.Config.RGBA_F16,true);
  gain.setGainmapForTest(true);wide.setColorSpaceForTest(new ColorSpace(false));
  for(Bitmap source:new Bitmap[]{gain,wide,f16}){bind(options(true,true),true,w,h);int before=ChromaPipeline177.APPLIES.get();Bitmap output=QualityPipeline1932.normalize(source,0,false);
   check(SaveQuality2.FALLBACK.get()==1&&ChromaPipeline177.APPLIES.get()==before,"unreadable/gainmap/wide-colour uses established normalization");
   check(Arrays.equals(source.snapshot(),p),"special-format fallback does not rewrite source");output.recycle();source.recycle();scenarios++;}
 }
 static void concurrent()throws Exception{
  final AtomicReference<Throwable> error=new AtomicReference<Throwable>();final Bitmap[] inputs=new Bitmap[2],outputs=new Bitmap[2];final int[][] pixels=new int[2][];
  for(int k=0;k<2;k++){pixels[k]=grain(112,389,k==0?24:3,k==0?95:155,99+k);inputs[k]=bitmap(112,389,pixels[k]);ShotContext1932.SHOTS.put(inputs[k],metadata(k==0?1600:100));}
  Thread[] threads=new Thread[2];for(int k=0;k<2;k++){final int index=k;threads[k]=new Thread(new Runnable(){public void run(){try{
   PhotoDetail.REQUEST.set(options(true,true));AsyncSave1935.CAPTURED.set(options(index==0,index==0));AsyncSave1935.COLOUR.set(Boolean.FALSE);SaveQuality2.SIZE.set(new int[]{112,389});
   outputs[index]=QualityPipeline1932.normalize(inputs[index],0,false);QualityPipeline1932.applyDetail(outputs[index],inputs[index],options(true,true));
  }catch(Throwable e){error.set(e);}}});threads[k].start();}
  for(Thread t:threads){t.join(15000);check(!t.isAlive(),"concurrent photo preparation drains");}
  check(error.get()==null,"concurrent preparation succeeds");check(Arrays.equals(outputs[1].snapshot(),pixels[1]),"OFF shot never borrows another shot's ON plan/options");
  check(variance(outputs[0].snapshot(),112,8,8,96,373)<variance(pixels[0],112,8,8,96,373)*.70,"ON shot preserves own noise treatment");
  for(int k=0;k<2;k++){check(Arrays.equals(inputs[k].snapshot(),pixels[k]),"parallel source unchanged");check(ShotContext1932.forBitmap(outputs[k])==ShotContext1932.forBitmap(inputs[k]),"parallel metadata remains image-bound");if(outputs[k]!=inputs[k])outputs[k].recycle();inputs[k].recycle();}
  check(Bitmap.writesAfterRecycle.get()==0,"parallel workers never write a recycled bitmap");scenarios++;
 }
 public static void main(String[] args)throws Exception{
  for(boolean colour:new boolean[]{false,true}){
   normalize(options(false,false),colour,0,192,145);normalize(options(true,false),colour,0,192,145);
   normalize(options(false,true),colour,90,145,192);normalize(options(true,true),colour,270,145,192);
   normalize(options(true,true),colour,0,288,218);normalize(options(true,true),colour,90,97,128);
  }
  residualAndSharp();snapshotOptions();outputCoordinates(90,291,385);outputCoordinates(270,291,385);outputCoordinates(180,385,291);outputCoordinates(0,481,364);
  completionIdentity();fallback(false);fallback(true);unreadable();concurrent();SpeedWorkers1935.trim();
  StringBuilder j=new StringBuilder("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+",\"physical_device_verified\":false,\"metrics\":{");boolean comma=false;
  for(Map.Entry<String,Object> e:metrics.entrySet()){if(comma)j.append(',');comma=true;j.append('"').append(e.getKey()).append("\":").append(e.getValue());}j.append("}}");System.out.println(j.toString());
 }
}
