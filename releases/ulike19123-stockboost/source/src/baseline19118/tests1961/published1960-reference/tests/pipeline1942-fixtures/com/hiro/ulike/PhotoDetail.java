package com.hiro.ulike;
public final class PhotoDetail {
 public static final ThreadLocal<Settings> REQUEST=new ThreadLocal<Settings>();
 public static Settings snapshot1932(){Settings s=REQUEST.get();return s==null?new Settings(false,2,false,2,true,true,true):s;}
 public static final class Settings {
  public final boolean noiseOn,sharpOn,texturePriority,haloSuppression,shadowPriority;
  public final int noiseLevel,sharpLevel;
  public Settings(boolean n,int nl,boolean s,int sl){this(n,nl,s,sl,true,true,true);}
  public Settings(boolean n,int nl,boolean s,int sl,boolean t,boolean h,boolean d){
   noiseOn=n;noiseLevel=nl;sharpOn=s;sharpLevel=sl;texturePriority=t;haloSuppression=h;shadowPriority=d;}
 }
}
