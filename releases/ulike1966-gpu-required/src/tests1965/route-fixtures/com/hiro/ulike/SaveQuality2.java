package com.hiro.ulike;
import android.graphics.Bitmap;
public final class SaveQuality2 {
 public static final java.util.concurrent.atomic.AtomicInteger FAILURES=new java.util.concurrent.atomic.AtomicInteger();
 public static void showSaveFailure(){FAILURES.incrementAndGet();}
 public static final ThreadLocal<int[]> SIZE=new ThreadLocal<int[]>();
 public static final ThreadLocal<Integer> FALLBACK=new ThreadLocal<Integer>(){protected Integer initialValue(){return 0;}};
 public static int[] output186(int w,int h,int rotation,boolean fixed){
  int[] s=SIZE.get();if(s!=null)return s.clone();
  return rotation==90||rotation==270?new int[]{h,w}:new int[]{w,h};}
 public static Bitmap normalize186(Bitmap bitmap,int rotation,boolean fixed){
  FALLBACK.set(FALLBACK.get()+1);HostAudit1932.event("fallback");
  if(bitmap==null||bitmap.isRecycled())return bitmap;
  return bitmap.copy(Bitmap.Config.ARGB_8888,true);}
}
