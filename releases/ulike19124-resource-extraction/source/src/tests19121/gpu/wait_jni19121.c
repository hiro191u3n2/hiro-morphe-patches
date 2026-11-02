#include <jni.h>
#define WAIT(n) Java_com_hiro_ulike_GpuWait19121Test_##n
#define GPU(n) Java_com_hiro_ulike_GpuNoise1960_##n
/* Deterministic host transport fixture only. No GLES, driver or Android speed
 * is represented. It drives the unchanged Java owner/collect boundaries. */
static int remaining[64],calls[64],fail_read[64],timeouts;
JNIEXPORT void JNICALL WAIT(configure)(JNIEnv*e,jclass c,jint ticket,jint zeros,jboolean fail){(void)e;(void)c;remaining[ticket]=zeros;calls[ticket]=0;fail_read[ticket]=fail;}
JNIEXPORT jint JNICALL WAIT(calls)(JNIEnv*e,jclass c,jint ticket){(void)e;(void)c;return calls[ticket];}
JNIEXPORT jint JNICALL WAIT(timeouts)(JNIEnv*e,jclass c){(void)e;(void)c;return timeouts;}
JNIEXPORT jint JNICALL GPU(ticketReadyNative)(JNIEnv*e,jclass c,jlong token,jlong ticket){(void)e;(void)c;(void)token;calls[ticket]++;if(remaining[ticket]==-2)return -1;if(remaining[ticket]<0)return 0;return remaining[ticket]-->0?0:1;}
JNIEXPORT jboolean JNICALL GPU(timeoutNative)(JNIEnv*e,jclass c,jlong token){(void)e;(void)c;(void)token;timeouts++;return JNI_FALSE;}
JNIEXPORT jlong JNICALL GPU(retainedNative)(JNIEnv*e,jclass c){(void)e;(void)c;return 0;}
static int write_pixels(JNIEnv*e,jintArray out,jint offset,jint count){jint *p=(*e)->GetIntArrayElements(e,out,0);if(!p)return 0;for(int i=0;i<count;i++)p[offset+i]=0x7f000000+i;(*e)->ReleaseIntArrayElements(e,out,p,0);return 1;}
static void times(JNIEnv*e,jlongArray t){jlong values[3]={11,22,33};(*e)->SetLongArrayRegion(e,t,0,3,values);}
JNIEXPORT jobjectArray JNICALL GPU(readManyNative)(JNIEnv*e,jclass c,jlong token,jlong ticket,jintArray slots,jintArray counts){(void)c;(void)token;(void)slots;if(fail_read[ticket])return 0;jsize size=(*e)->GetArrayLength(e,counts);jclass ints=(*e)->FindClass(e,"[I");jobjectArray out=(*e)->NewObjectArray(e,size,ints,0);(*e)->DeleteLocalRef(e,ints);for(jsize i=0;i<size;i++){jint count;(*e)->GetIntArrayRegion(e,counts,i,1,&count);jintArray row=(*e)->NewIntArray(e,count);write_pixels(e,row,0,count);(*e)->SetObjectArrayElement(e,out,i,row);(*e)->DeleteLocalRef(e,row);}return out;}
JNIEXPORT jobjectArray JNICALL GPU(readManyObservedNative19117)(JNIEnv*e,jclass c,jlong token,jlong ticket,jintArray slots,jintArray counts,jlongArray t){times(e,t);return GPU(readManyNative)(e,c,token,ticket,slots,counts);}
JNIEXPORT jboolean JNICALL GPU(readIntoNative)(JNIEnv*e,jclass c,jlong token,jlong ticket,jint slot,jint count,jintArray out,jint offset){(void)c;(void)token;(void)slot;if(fail_read[ticket])return JNI_FALSE;return write_pixels(e,out,offset,count)?JNI_TRUE:JNI_FALSE;}
JNIEXPORT jboolean JNICALL GPU(readManyIntoNative)(JNIEnv*e,jclass c,jlong token,jlong ticket,jintArray slots,jintArray counts,jobjectArray targets,jintArray offsets){if(fail_read[ticket])return JNI_FALSE;jsize size=(*e)->GetArrayLength(e,counts);for(jsize i=0;i<size;i++){jint count,offset,slot;(*e)->GetIntArrayRegion(e,counts,i,1,&count);(*e)->GetIntArrayRegion(e,offsets,i,1,&offset);(*e)->GetIntArrayRegion(e,slots,i,1,&slot);jintArray row=(jintArray)(*e)->GetObjectArrayElement(e,targets,i);jboolean ok=GPU(readIntoNative)(e,c,token,ticket,slot,count,row,offset);(*e)->DeleteLocalRef(e,row);if(!ok)return JNI_FALSE;}return JNI_TRUE;}
JNIEXPORT jboolean JNICALL GPU(readManyIntoObservedNative19117)(JNIEnv*e,jclass c,jlong token,jlong ticket,jintArray slots,jintArray counts,jobjectArray targets,jintArray offsets,jlongArray t){times(e,t);return GPU(readManyIntoNative)(e,c,token,ticket,slots,counts,targets,offsets);}
