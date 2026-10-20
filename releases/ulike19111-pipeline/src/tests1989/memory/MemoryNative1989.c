/* Host JNI bridge calls the unchanged production native pool implementations.
 * It injects only leases and sentinel bytes, never a budget or fit decision. */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include "h8gpu/h8_gpu.c"
#include "native1958/scratch1959.h"

#define TEST1989(name) Java_com_hiro_ulike_MemoryNative1989_##name
JNIEXPORT jlong JNICALL TEST1989(strongAcquire)(JNIEnv *env,jclass cls,jint bytes,jint value) {
    (void)env;(void)cls;if(bytes<1)return 0;Scratch1959 temp={0};
    Scratch1959 *s=scratch_acquire1959(&temp);if(!s)return 0;
    void *p=scratch_reserve1959(s,0,(size_t)bytes);
    if(!p){scratch_release1959(s);return 0;}memset(p,value,(size_t)bytes);
    return (jlong)(intptr_t)s;
}
JNIEXPORT jboolean JNICALL TEST1989(strongRelease)(JNIEnv *env,jclass cls,jlong handle,jint bytes,jint value) {
    (void)env;(void)cls;Scratch1959 *s=(Scratch1959*)(intptr_t)handle;
    if(!s||bytes<1||s->capacities[0]<(size_t)bytes)return JNI_FALSE;
    int okay=1;unsigned char *p=s->blocks[0];
    for(int i=0;i<bytes;i++)if(p[i]!=(unsigned char)value){okay=0;break;}
    scratch_release1959(s);return okay?JNI_TRUE:JNI_FALSE;
}
JNIEXPORT jlong JNICALL TEST1989(strongBytes)(JNIEnv *env,jclass cls){(void)env;(void)cls;return (jlong)scratch_bytes1959();}
JNIEXPORT void JNICALL TEST1989(strongTrim)(JNIEnv *env,jclass cls){(void)env;(void)cls;scratch_trim1959();}

JNIEXPORT jlong JNICALL TEST1989(h8Acquire)(JNIEnv *env,jclass cls,jint bytes,jint value) {
    (void)env;(void)cls;if(bytes<1||bytes>32*1024*1024)return 0;
    if(pthread_mutex_lock(&scratch_lock1956)!=0)return 0;
    int temporary=0;int32_t *p=scratch_acquire1956((size_t)bytes,&temporary);
    if(!p||temporary){if(p)scratch_release1956(p,temporary);else pthread_mutex_unlock(&scratch_lock1956);return 0;}
    memset(p,value,(size_t)bytes);return (jlong)(intptr_t)p;
}
JNIEXPORT jboolean JNICALL TEST1989(h8Release)(JNIEnv *env,jclass cls,jlong handle,jint bytes,jint value) {
    (void)env;(void)cls;unsigned char *p=(unsigned char*)(intptr_t)handle;
    if(!p||bytes<1)return JNI_FALSE;int okay=1;
    for(int i=0;i<bytes;i++)if(p[i]!=(unsigned char)value){okay=0;break;}
    scratch_release1956((int32_t*)p,0);return okay?JNI_TRUE:JNI_FALSE;
}
JNIEXPORT jlong JNICALL TEST1989(h8Bytes)(JNIEnv *env,jclass cls){return Java_com_hiro_ulike_CorePixels1950_retainedNativeScratchBytes(env,cls);}
JNIEXPORT void JNICALL TEST1989(h8Trim)(JNIEnv *env,jclass cls){Java_com_hiro_ulike_CorePixels1950_trimNativeScratch(env,cls);}
