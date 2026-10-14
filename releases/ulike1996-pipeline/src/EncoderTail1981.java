package com.hiro.ulike;

import android.graphics.Bitmap;

/** Called only after the owned HeifWriter close, callback drain and codec release.
 * Verification below this point reads the encoded file and never these pixels.
 * The pristine source and any still-leased exact-proof image stay alive. */
public final class EncoderTail1981 {
    private EncoderTail1981() {}
    public static void release(Bitmap finished,Bitmap pristine) {
        if(finished==null||finished==pristine)return;
        try {
            if(!finished.isRecycled())QualityPipeline1932.recycle1954(finished);
            if(finished.isRecycled())AsyncSave1935.encoderInputReleased1981(finished);
        }catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        // The original terminal cleanup remains an idempotent last resort.
    }
}
