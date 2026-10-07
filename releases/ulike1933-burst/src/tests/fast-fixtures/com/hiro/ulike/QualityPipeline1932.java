package com.hiro.ulike;
import android.graphics.Bitmap;
/** Host adapter to the byte-pinned original resampler, with explicit rotation. */
public final class QualityPipeline1932 {
 public static int fallbacks;
 public static Bitmap resample(final Bitmap input,final int rotation,final int width,final int height){
  fallbacks++;
  final int sw=input.getWidth(),sh=input.getHeight(),rw=rotation==90||rotation==270?sh:sw,rh=rotation==90||rotation==270?sw:sh;
  if(rotation==0&&sw==width&&sh==height)return input;
  final int[] original=input.pixels();
  final Bitmap result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
  result.setDensity(input.getDensity());result.setHasAlpha(input.hasAlpha());
  double scale=Math.max((double)width/rw,(double)height/rh),cw=Math.min((double)rw,width/scale),ch=Math.min((double)rh,height/scale);
  QualityPixels1932.resizeCrop(new QualityPixels1932.RowSource(){public void readRow(int y,int[] p){
   for(int x=0;x<rw;x++){
    int sx=rotation==90?y:rotation==270?sw-1-y:rotation==180?sw-1-x:x;
    int sy=rotation==90?sh-1-x:rotation==270?x:rotation==180?sh-1-y:y;
    p[x]=original[sy*sw+sx];
   }
  }},rw,rh,new QualityPixels1932.RowSink(){public void writeRow(int y,int[] p){result.setPixels(p,0,width,0,y,width,1);}},width,height,
    Math.max(0,(rw-cw)*0.5),Math.max(0,(rh-ch)*0.5),cw,ch);
  return result;
 }
}
