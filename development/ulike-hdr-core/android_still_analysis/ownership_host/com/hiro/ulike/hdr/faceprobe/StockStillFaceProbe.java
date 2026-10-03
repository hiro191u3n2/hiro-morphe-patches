package com.hiro.ulike.hdr.faceprobe;
import android.content.Context;
/** Host-only lease shape. No native implementation or vendor code. */
public final class StockStillFaceProbe {
    public interface IdleSdkLease { Context context();void requireInitializedAndNoOtherRecorder(); }
}
