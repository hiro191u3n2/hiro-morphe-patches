package com.hiro.ulike;
import android.graphics.Bitmap;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
/** Private decoded-photo integration fixtures, supplied separately by environment.
 * No original photo is embedded. The host primary NR/colour code remains a surrogate;
 * this verifies final pipeline application, OFF/ON and no double filtering on real RGB.
 */
public final class PipelinePhoto1942Test {
 static int assertions,scenarios;
 static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
 static int[] read(String file,int width,int height)throws Exception{
  byte[] bytes=Files.readAllBytes(Paths.get(file));if(bytes.length!=(long)width*height*4)throw new IllegalArgumentException("fixture dimensions");
  int[] p=new int[width*height];ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(p);return p;
 }
 static double mean(int[] p){double s=0;for(int value:p)s+=QualityPixels1932.luma(value);return s/p.length;}
 static double gradientEnergy(int[] p,int w,int h){double sum=0;long n=0;for(int y=8;y<h-8;y++)for(int x=8;x<w-8;x++){
  int at=y*w+x,c=QualityPixels1932.luma(p[at]);double a=c-QualityPixels1932.luma(p[at+1]),b=c-QualityPixels1932.luma(p[at+w]);sum+=a*a+b*b;n+=2;
 }return sum/Math.max(1,n);}
 static void options(PhotoDetail.Settings settings,boolean colour,int w,int h){PhotoDetail.REQUEST.set(settings);AsyncSave1935.CAPTURED.remove();AsyncSave1935.COLOUR.remove();ChromaPipeline177.enabled=colour;SaveQuality2.SIZE.set(new int[]{w,h});SaveQuality2.FALLBACK.set(0);}
 public static void main(String[] args)throws Exception{
  if(args.length==0||args.length%4!=0)throw new IllegalArgumentException("name file width height groups");
  StringBuilder json=new StringBuilder("{\"status\":\"passed\",\"metrics\":{");boolean comma=false;FaceRegions1934.NO_FACE_MASK.set(Boolean.TRUE);
  try{for(int i=0;i<args.length;i+=4){String name=args[i];int w=Integer.parseInt(args[i+2]),h=Integer.parseInt(args[i+3]);int[] p=read(args[i+1],w,h);
   for(boolean colour:new boolean[]{false,true}){
    Bitmap source=Bitmap.from(w,h,p,Bitmap.Config.ARGB_8888,true);PhotoDetail.Settings off=new PhotoDetail.Settings(false,3,false,3,false,true,true),on=new PhotoDetail.Settings(true,3,true,3,false,true,true);
    options(off,false,w,h);Bitmap unchanged=QualityPipeline1932.normalize(source,0,false);QualityPipeline1932.applyDetail(unchanged,source,off);
    check(Arrays.equals(unchanged.snapshot(),p),"NR OFF preserves decoded real image fixture");
    PhotoDetail.Settings nrOnly=new PhotoDetail.Settings(true,3,false,3,false,true,true);
    options(nrOnly,colour,w,h);Bitmap denoised=QualityPipeline1932.normalize(source,0,false);int[] cleaned=denoised.snapshot();QualityPipeline1932.applyDetail(denoised,source,nrOnly);
    options(on,colour,w,h);Bitmap finalBitmap=QualityPipeline1932.normalize(source,0,false);int[] saved=finalBitmap.snapshot();
    check(SaveQuality2.FALLBACK.get()==0,"real image fixture follows final preparation without fallback");
    check(finalBitmap.getWidth()==w&&finalBitmap.getHeight()==h,"real crop final dimensions retained");
    check(Arrays.equals(source.snapshot(),p),"real crop source remains unchanged");
    int changed=0;for(int a=0;a<p.length;a++)if(p[a]!=saved[a])changed++;
    check(changed>0,"NR ON changes pixels reaching final saved bitmap "+name+":"+colour);
    double before=gradientEnergy(p,w,h),nr=gradientEnergy(cleaned,w,h),after=gradientEnergy(saved,w,h),meanShift=mean(saved)-mean(p);
    check(nr<before,"final NR-only lowers wall first-difference energy "+name+":"+colour+" before="+before+" nr="+nr);
    // Wallpaper contains real structure, so first-difference energy is not a pure
    // noise measurement. Allow bounded structure sharpening/8-bit rounding, while
    // the synthetic flat-wall suite separately rejects grain reamplification.
    check(after<=nr*1.04+.15,"output sharpen has bounded wall-energy effect "+name+":"+colour+" nr="+nr+" after="+after);
    check(Math.abs(meanShift)<2.0,"wall brightness mean retained "+name+":"+colour);
    int applies=ChromaPipeline177.APPLIES.get();QualityPipeline1932.applyDetail(finalBitmap,source,on);
    check(ChromaPipeline177.APPLIES.get()==applies&&Arrays.equals(finalBitmap.snapshot(),saved),"final detail hook never repeats NR on real crop");
    if(comma)json.append(',');comma=true;json.append('"').append(name).append(colour?"_chroma_on":"_chroma_off").append("\":{\"changed_pixels\":").append(changed).append(",\"first_difference_energy_before\":").append(before).append(",\"first_difference_energy_nr_only\":").append(nr).append(",\"first_difference_energy_after\":").append(after).append(",\"mean_luma_shift\":").append(meanShift).append('}');
    if(unchanged!=source)unchanged.recycle();if(denoised!=source)denoised.recycle();if(finalBitmap!=source)finalBitmap.recycle();source.recycle();scenarios++;
   }
  }}finally{FaceRegions1934.NO_FACE_MASK.remove();SpeedWorkers1935.trim();}
  json.append("},\"assertions\":").append(assertions).append(",\"scenarios\":").append(scenarios).append(",\"physical_device_verified\":false,\"metric_caution\":\"Reprocessed decoded saved-photo crops. First-difference energy includes real texture and is not a measured sensor-noise reduction percentage. Android/primary NR/colour are host fixtures.\"}");System.out.println(json.toString());
 }
}
