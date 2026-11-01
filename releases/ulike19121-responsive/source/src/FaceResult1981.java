package com.hiro.ulike;

import android.graphics.Bitmap;

/** Distinguish a completed zero-face analysis from the legacy empty-mask
 * fallback after allocation, format or detector failure. No face pixels retained. */
public final class FaceResult1981 {
    public final boolean completed;
    public final FaceRegions1934.Mask mask;
    private static final ThreadLocal<Observation> CURRENT=new ThreadLocal<Observation>();
    private static final class Observation {boolean completed;}
    private FaceResult1981(boolean completed,FaceRegions1934.Mask mask){this.completed=completed;this.mask=mask;}

    /** DEX hooks invoke this only at the two audited normal-return paths. */
    public static void completed1981(){Observation current=CURRENT.get();if(current!=null)current.completed=true;}
    public static void completedEmpty1981(int found){if(found==0)completed1981();}
    static FaceResult1981 analyze(Bitmap bitmap,int rotation) {
        Observation previous=CURRENT.get(),observation=new Observation();
        CURRENT.set(observation);
        try {
            FaceRegions1934.Mask mask=FaceRegions1934.forBitmap(bitmap,rotation);
            return new FaceResult1981(observation.completed&&mask!=null,mask);
        } finally {if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    }
}
