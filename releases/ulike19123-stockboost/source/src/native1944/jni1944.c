#include <jni.h>
#include "residual1944.h"
#include "kernels1935.h"
#define JNINAME(name) Java_com_hiro_ulike_NativeSpeed1944_##name
JNIEXPORT jint JNICALL JNINAME(nativeAbi)(JNIEnv *env,jclass cls){(void)env;(void)cls;return 1944;}
JNIEXPORT jboolean JNICALL JNINAME(aggregateNative)(JNIEnv *env,jclass cls,
        jintArray input,jintArray meta,jintArray out,jint width,jint rows,
        jint begin,jint end,jint lo,jint hi,jint radius,jintArray range,
        jintArray ring,jintArray x){
    (void)cls;
    if(!input || !meta || !out || !range || !ring || !x)return JNI_FALSE;
    jobject objects[6]={input,meta,out,range,ring,x};
    for(int i=0;i<6;i++)for(int j=i+1;j<6;j++)if((*env)->IsSameObject(env,objects[i],objects[j]))return JNI_FALSE;
    int counts[6];for(int i=0;i<6;i++)counts[i]=(*env)->GetArrayLength(env,(jarray)objects[i]);
    jint *p[6]={0};jboolean ok=JNI_FALSE;
    /* No JNI/heap allocation/callback occurs after all primitive arrays are held.
       Each Java invocation is bounded to a small tile, limiting critical time. */
    for(int i=0;i<6;i++){p[i]=(*env)->GetPrimitiveArrayCritical(env,(jarray)objects[i],0);if(!p[i])goto done;}
    if(!residual1944_valid(p[1],counts[1],width,rows,begin,end,lo,hi,radius,
            counts[0],counts[2],counts[3],counts[4],counts[5]))goto done;
    for(int i=0;i<33*256;i++)if(p[3][i]<0 || p[3][i]>256)goto done;
    int taps=radius==4?7:radius*2+1;
    for(int i=0;i<width*taps;i++)if(p[5][i]<0 || p[5][i]>=width)goto done;
    residual1944_aggregate(p[0],p[1],p[2],width,begin,end,lo,hi,radius,p[3],p[4],p[5]);ok=JNI_TRUE;
done:
    for(int i=5;i>=0;i--)if(p[i])(*env)->ReleasePrimitiveArrayCritical(env,(jarray)objects[i],p[i],i==2&&ok?0:JNI_ABORT);
    return ok;
}
JNIEXPORT jboolean JNICALL JNINAME(verticalBatchNative)(JNIEnv *env,jclass cls,
        jobjectArray rows,jfloatArray weights,jint first,jint taps,jfloatArray accum,
        jfloatArray minimum,jfloatArray maximum,jint count){
    (void)cls;
    if(!rows || !weights || !accum || !minimum || !maximum || taps<1 || taps>128 ||
        first<0 || count<1 || (*env)->GetArrayLength(env,rows)<taps ||
        (int64_t)first+taps>(*env)->GetArrayLength(env,weights) ||
        (*env)->GetArrayLength(env,accum)<count || (*env)->GetArrayLength(env,minimum)<count ||
        (*env)->GetArrayLength(env,maximum)<count || (*env)->IsSameObject(env,accum,minimum) ||
        (*env)->IsSameObject(env,accum,maximum) || (*env)->IsSameObject(env,minimum,maximum) ||
        (*env)->IsSameObject(env,weights,accum) || (*env)->IsSameObject(env,weights,minimum) ||
        (*env)->IsSameObject(env,weights,maximum))return JNI_FALSE;
    jobject refs[128];float *values[128];int nrefs=0,nvalues=0;
    float *wt=0,*a=0,*mn=0,*mx=0;jboolean ok=JNI_FALSE;
    /* Resolve object-array entries before entering primitive critical sections. */
    for(int t=0;t<taps;t++){
        refs[t]=(*env)->GetObjectArrayElement(env,rows,t);
        nrefs=t+1;
        if(!refs[t] || (*env)->GetArrayLength(env,(jarray)refs[t])<count ||
            (*env)->IsSameObject(env,refs[t],accum) || (*env)->IsSameObject(env,refs[t],minimum) ||
            (*env)->IsSameObject(env,refs[t],maximum))goto done;
    }
    wt=(*env)->GetPrimitiveArrayCritical(env,weights,0);if(!wt)goto done;
    for(int t=0;t<taps;t++){
        union{float f;uint32_t u;} bits={wt[first+t]};
        if((bits.u&0x7f800000u)==0x7f800000u)goto done;
    }
    a=(*env)->GetPrimitiveArrayCritical(env,accum,0);if(!a)goto done;
    mn=(*env)->GetPrimitiveArrayCritical(env,minimum,0);if(!mn)goto done;
    mx=(*env)->GetPrimitiveArrayCritical(env,maximum,0);if(!mx)goto done;
    for(int t=0;t<taps;t++){
        values[t]=(*env)->GetPrimitiveArrayCritical(env,(jarray)refs[t],0);
        if(!values[t])goto done;
        nvalues=t+1;
    }
    /* Existing exact float kernel, original tap order, original FPCR discipline.
       No regrouping/reassociation occurs inside each component's sum. */
    for(int t=0;t<taps;t++)speed1935_vertical(a,mn,mx,values[t],wt[first+t],count);
    ok=JNI_TRUE;
done:
    for(int t=nvalues-1;t>=0;t--)(*env)->ReleasePrimitiveArrayCritical(env,(jarray)refs[t],values[t],JNI_ABORT);
    if(mx)(*env)->ReleasePrimitiveArrayCritical(env,maximum,mx,ok?0:JNI_ABORT);
    if(mn)(*env)->ReleasePrimitiveArrayCritical(env,minimum,mn,ok?0:JNI_ABORT);
    if(a)(*env)->ReleasePrimitiveArrayCritical(env,accum,a,ok?0:JNI_ABORT);
    if(wt)(*env)->ReleasePrimitiveArrayCritical(env,weights,wt,JNI_ABORT);
    for(int t=0;t<nrefs;t++)if(refs[t])(*env)->DeleteLocalRef(env,refs[t]);
    return ok;
}
