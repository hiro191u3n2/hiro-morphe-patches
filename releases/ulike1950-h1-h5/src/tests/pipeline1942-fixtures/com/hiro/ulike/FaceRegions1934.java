package com.hiro.ulike;import android.graphics.Bitmap;
public final class FaceRegions1934 {
 public static int calls;
 public static final ThreadLocal<Boolean> NO_FACE_MASK=new ThreadLocal<Boolean>();
 public static final class Mask implements QualityPixels1932.RegionMask {public int skinQ8(int x,int y){return ((x/12+y/9)%3==0)?192:0;}public int detailQ8(int x,int y){return x%11<4?180:0;}public Mask resample(int r,int w,int h){return this;}}
 public static Mask forBitmap(Bitmap b,int rotation){calls++;return Boolean.TRUE.equals(NO_FACE_MASK.get())?null:new Mask();}
}
