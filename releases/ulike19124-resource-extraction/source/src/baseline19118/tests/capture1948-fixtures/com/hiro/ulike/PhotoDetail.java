package com.hiro.ulike;
public final class PhotoDetail {
    public static Settings current=new Settings(true,2);public static Settings snapshot1932(){return current;}
    public static final class Settings {public final boolean noiseOn;public final int noiseLevel;public Settings(boolean n,int l){noiseOn=n;noiseLevel=l;}}
}

