package com.hiro.ulike;
/** Compile-only ABI for the existing production class. */
public final class PhotoDetail {
    public static Settings snapshot1932() { return null; }
    public static final class Settings {
        public final boolean noiseOn, sharpOn, texturePriority, haloSuppression, shadowPriority;
        public final int noiseLevel, sharpLevel;
        public Settings(boolean n,int nl,boolean s,int sl,boolean t,boolean h,boolean d) {
            noiseOn=n; noiseLevel=nl; sharpOn=s; sharpLevel=sl;
            texturePriority=t; haloSuppression=h; shadowPriority=d;
        }
    }
}
