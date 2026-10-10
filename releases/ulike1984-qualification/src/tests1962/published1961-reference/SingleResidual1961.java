package com.hiro.ulike;

/** Exact native1955 binary64 preparation for the FP32 GPU residual finish.
 * Records contain only the original float casts; all transforms, thresholds,
 * residual safeguards and source samples remain the current CPU arithmetic. */
final class SingleResidual1961 {
    private SingleResidual1961() {}
    static float[] prepare(int[] input,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,
            SingleNoise1955.Model model) {
        return prepareNative(input,width,rows,begin,end,validBegin,validEnd,originY,
            noise,shadows,model.height,model.columns,model.rows,
            model.gpuPatchWidth1960(),model.gpuPatchHeight1960(),model.gpuData1960());
    }
    private static native float[] prepareNative(int[] input,int width,int rows,int begin,int end,
        int validBegin,int validEnd,int originY,int noise,boolean shadows,int modelHeight,
        int columns,int modelRows,int patchWidth,int patchHeight,float[] modelData);
}
