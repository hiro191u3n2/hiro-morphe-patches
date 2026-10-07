package com.hiro.ulike;import android.graphics.Bitmap;
public final class FaceRegions1934 {
 public static final class Mask implements QualityPixels1932.RegionMask {public int skinQ8(int x,int y){return 0;}public int detailQ8(int x,int y){return 0;}public Mask resample(int r,int w,int h){return this;}}
 public static Mask forBitmap(Bitmap b,int rotation){return new Mask();}
}
