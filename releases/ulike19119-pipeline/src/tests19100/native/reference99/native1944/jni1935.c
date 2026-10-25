#include <jni.h>
#include "kernels1935.h"
#define JNINAME(name) Java_com_hiro_ulike_NativeSpeed1935_##name
JNIEXPORT jint JNICALL JNINAME(nativeAbi)(JNIEnv *env,jclass cls) { (void)env;(void)cls;return 1935; }
JNIEXPORT jboolean JNICALL JNINAME(packNative)(JNIEnv *env,jclass cls,
    jobject y,jint ys,jint yl,jint yr,jint yp,jobject u,jint us,jint ul,jint ur,jint up,
    jobject v,jint vs,jint vl,jint vr,jint vp,jint width,jint height,jbyteArray out) {
    (void)cls;
    if(!y || !u || !v || !out || width<2 || height<2 || (width&1) || (height&1))return JNI_FALSE;
    int64_t pixels=(int64_t)width*height;
    if(pixels>1431655754)return JNI_FALSE; /* bound before multiplying by three */
    int64_t size=pixels+pixels/2;
    if(size>0x7ffffff7 || (*env)->GetArrayLength(env,out)<size)return JNI_FALSE;
    if(!speed1935_plane_valid(ys,yl,yr,yp,width,height) || !speed1935_plane_valid(us,ul,ur,up,width/2,height/2) || !speed1935_plane_valid(vs,vl,vr,vp,width/2,height/2))return JNI_FALSE;
    if((*env)->GetDirectBufferCapacity(env,y)<yl || (*env)->GetDirectBufferCapacity(env,u)<ul || (*env)->GetDirectBufferCapacity(env,v)<vl)return JNI_FALSE;
    uint8_t *yb=(*env)->GetDirectBufferAddress(env,y),*ub=(*env)->GetDirectBufferAddress(env,u),*vb=(*env)->GetDirectBufferAddress(env,v);
    if(!yb || !ub || !vb)return JNI_FALSE;
    jbyte *dest=(*env)->GetPrimitiveArrayCritical(env,out,0);if(!dest)return JNI_FALSE;
    speed1935_pack(yb,ys,yr,yp,ub,us,ur,up,vb,vs,vr,vp,width,height,(uint8_t*)dest);
    (*env)->ReleasePrimitiveArrayCritical(env,out,dest,0);return JNI_TRUE;
}
JNIEXPORT jboolean JNICALL JNINAME(horizontalNative)(JNIEnv *env,jclass cls,jintArray raw,jintArray offsets,jintArray indices,jfloatArray weights,jfloatArray out,jint width) {
    (void)cls;
    if(!raw || !offsets || !indices || !weights || !out || (*env)->IsSameObject(env,weights,out))return JNI_FALSE;
    int nr=(*env)->GetArrayLength(env,raw),no=(*env)->GetArrayLength(env,offsets),ni=(*env)->GetArrayLength(env,indices),nw=(*env)->GetArrayLength(env,weights),nd=(*env)->GetArrayLength(env,out);
    if(width<=0 || width>=no || width>0x7fffffff/3 || nd<width*3 || nr<=0)return JNI_FALSE;
    jint *r=0,*o=0,*ix=0;float *wt=0,*dest=0;jboolean ok=JNI_FALSE;
    r=(*env)->GetPrimitiveArrayCritical(env,raw,0);if(!r)goto done;
    o=(*env)->GetPrimitiveArrayCritical(env,offsets,0);if(!o)goto done;
    ix=(*env)->GetPrimitiveArrayCritical(env,indices,0);if(!ix)goto done;
    wt=(*env)->GetPrimitiveArrayCritical(env,weights,0);if(!wt)goto done;
    dest=(*env)->GetPrimitiveArrayCritical(env,out,0);if(!dest)goto done;
    if(speed1935_horizontal_valid(r,nr,o,no,ix,ni,wt,nw,nd,width)){speed1935_horizontal(r,o,ix,wt,dest,width);ok=JNI_TRUE;}
done:
    if(dest)(*env)->ReleasePrimitiveArrayCritical(env,out,dest,ok?0:JNI_ABORT);
    if(wt)(*env)->ReleasePrimitiveArrayCritical(env,weights,wt,JNI_ABORT);
    if(ix)(*env)->ReleasePrimitiveArrayCritical(env,indices,ix,JNI_ABORT);
    if(o)(*env)->ReleasePrimitiveArrayCritical(env,offsets,o,JNI_ABORT);
    if(r)(*env)->ReleasePrimitiveArrayCritical(env,raw,r,JNI_ABORT);
    return ok;
}
JNIEXPORT jboolean JNICALL JNINAME(verticalAddNative)(JNIEnv *env,jclass cls,jfloatArray accum,jfloatArray minimum,jfloatArray maximum,jfloatArray row,jfloat weight,jint count) {
    (void)cls;
    if(!accum || !minimum || !maximum || !row || count<0 || (*env)->GetArrayLength(env,accum)<count || (*env)->GetArrayLength(env,minimum)<count || (*env)->GetArrayLength(env,maximum)<count || (*env)->GetArrayLength(env,row)<count)return JNI_FALSE;
    if((*env)->IsSameObject(env,accum,minimum) || (*env)->IsSameObject(env,accum,maximum) || (*env)->IsSameObject(env,accum,row) || (*env)->IsSameObject(env,minimum,maximum) || (*env)->IsSameObject(env,minimum,row) || (*env)->IsSameObject(env,maximum,row))return JNI_FALSE;
    union { float f; uint32_t u; } bits={weight};if((bits.u&0x7f800000u)==0x7f800000u)return JNI_FALSE;
    float *a=0,*mn=0,*mx=0,*r=0;jboolean ok=JNI_FALSE;
    a=(*env)->GetPrimitiveArrayCritical(env,accum,0);if(!a)goto done;
    mn=(*env)->GetPrimitiveArrayCritical(env,minimum,0);if(!mn)goto done;
    mx=(*env)->GetPrimitiveArrayCritical(env,maximum,0);if(!mx)goto done;
    r=(*env)->GetPrimitiveArrayCritical(env,row,0);if(!r)goto done;
    speed1935_vertical(a,mn,mx,r,weight,count);ok=JNI_TRUE;
done:
    if(r)(*env)->ReleasePrimitiveArrayCritical(env,row,r,JNI_ABORT);
    if(mx)(*env)->ReleasePrimitiveArrayCritical(env,maximum,mx,ok?0:JNI_ABORT);
    if(mn)(*env)->ReleasePrimitiveArrayCritical(env,minimum,mn,ok?0:JNI_ABORT);
    if(a)(*env)->ReleasePrimitiveArrayCritical(env,accum,a,ok?0:JNI_ABORT);
    return ok;
}
