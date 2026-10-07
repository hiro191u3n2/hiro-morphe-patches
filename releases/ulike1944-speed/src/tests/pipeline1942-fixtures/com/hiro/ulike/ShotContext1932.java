package com.hiro.ulike;
import android.graphics.Bitmap;import java.util.*;
public final class ShotContext1932 {
 public static final int LENS_UNKNOWN=0,LENS_FRONT=1,LENS_BACK=2,LENS_ULTRAWIDE=3,LENS_TELEPHOTO=4;
 public static final class Snapshot {
  public final int iso,lensKind;public final long exposureNanos;public final boolean metadataReliable;
  public final float beautyStrength;
  public Snapshot(int iso,long exposure,int lens,boolean reliable,float beauty){
   this.iso=iso;exposureNanos=exposure;lensKind=lens;metadataReliable=reliable;beautyStrength=beauty;}
 }
 public static final Snapshot UNKNOWN=new Snapshot(0,0,0,false,-1);
 public static final Map<Bitmap,Snapshot> SHOTS=Collections.synchronizedMap(new WeakHashMap<Bitmap,Snapshot>());
 public static volatile boolean failCopyOnce;
 public static Snapshot forBitmap(Bitmap b){Snapshot s=SHOTS.get(b);return s==null?UNKNOWN:s;}
 public static void copy(Bitmap from,Bitmap to){
  if(failCopyOnce){failCopyOnce=false;throw new IllegalStateException("injected metadata copy");}
  SHOTS.put(to,forBitmap(from));}
 public static void forget(Bitmap bitmap){SHOTS.remove(bitmap);}
}
