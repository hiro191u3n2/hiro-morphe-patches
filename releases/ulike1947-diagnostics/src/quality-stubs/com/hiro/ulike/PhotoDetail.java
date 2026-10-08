package com.hiro.ulike;
import android.graphics.Bitmap;
public final class PhotoDetail {
    private static volatile Settings settings;
    /** Only this accessor is copied into the existing production class. */
    public static Settings snapshot1932() { return settings; }
    public static Bitmap applyDetailLegacy177Before1947(Bitmap bitmap,Bitmap reference){return bitmap;}
    public static final class Settings {
        public final boolean noiseOn, sharpOn, texturePriority, haloSuppression, shadowPriority;
        public final int noiseLevel, sharpLevel;
        public Settings(boolean n, int nl, boolean s, int sl, boolean t, boolean h, boolean d) {
            noiseOn=n; noiseLevel=nl; sharpOn=s; sharpLevel=sl;
            texturePriority=t; haloSuppression=h; shadowPriority=d;
        }
    }
}
