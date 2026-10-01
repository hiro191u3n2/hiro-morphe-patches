package com.hiro.ulike.hdr.faceprobe;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.os.Build;

/** Moves one private analysis Bitmap. No Bitmap.copy and no implicit orientation comparison.
 * The caller irrevocably relinquishes mutation/recycling after adopt; only the probe's claim joins it.
 */
public final class OwnedBitmapSubmission implements AutoCloseable {
    public final int width,height;
    public final AnalysisCapacity capacity;
    private final SubmissionOwnership<Bitmap> ownership;
    private OwnedBitmapSubmission(Bitmap bitmap,AnalysisCapacity capacity) {
        if(bitmap==null || capacity==null)throw new NullPointerException();
        if(bitmap.isRecycled() || bitmap.getConfig()!=Bitmap.Config.ARGB_8888 ||
                !ColorSpace.get(ColorSpace.Named.SRGB).equals(bitmap.getColorSpace()) ||
                (Build.VERSION.SDK_INT>=34 && bitmap.hasGainmap()))
            throw new IllegalArgumentException("Owned explicit sRGB ARGB_8888 without gain map required");
        width=bitmap.getWidth();height=bitmap.getHeight();this.capacity=capacity;
        capacity.requireTransferredPayload(width,height);
        ownership=new SubmissionOwnership<>(bitmap,Bitmap::recycle);
    }
    /** Ownership is adopted only when this method returns successfully. */
    public static OwnedBitmapSubmission adopt(Bitmap bitmap,AnalysisCapacity capacity){return new OwnedBitmapSubmission(bitmap,capacity);}
    public SubmissionOwnership.State state(){return ownership.state();}
    /** Read-only borrow for a streaming digest before native transfer. */
    public Bitmap borrowBeforeTransfer(){return ownership.beforeTransfer();}
    SubmissionOwnership.Claim<Bitmap> transferToProbe(){return ownership.transfer();}
    @Override public void close(){ownership.close();}
}
