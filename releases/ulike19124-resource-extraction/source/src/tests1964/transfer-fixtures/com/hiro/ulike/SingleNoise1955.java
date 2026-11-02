package com.hiro.ulike;
/** JNI preparation test descriptor; arithmetic oracle is immutable .63 C. */
final class SingleNoise1955 {
    static final class Model {
        final int height,columns,rows,patchWidth,patchHeight;
        final float[] data;
        Model(int h,int c,int r,int pw,int ph,float[] d){height=h;columns=c;rows=r;patchWidth=pw;patchHeight=ph;data=d;}
        int gpuPatchWidth1960(){return patchWidth;}
        int gpuPatchHeight1960(){return patchHeight;}
        float[] gpuData1960(){return data;}
    }
}
