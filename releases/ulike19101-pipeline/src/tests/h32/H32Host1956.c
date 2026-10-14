#define _POSIX_C_SOURCE 200809L
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <stdatomic.h>
#include <time.h>
#include <GLES3/gl31.h>
static _Atomic long allocations, fail_allocation, fail_unmap, pause_wait, wait_entered, clock_offset;
static void *counted_malloc(size_t n){
    atomic_fetch_add(&allocations,1);
    return atomic_exchange(&fail_allocation,0)?0:malloc(n);
}
static GLboolean fault_unmap(GLenum target){
    GLboolean value=glUnmapBuffer(target);
    return atomic_exchange(&fail_unmap,0)?GL_FALSE:value;
}
static GLenum paused_wait(GLsync sync,GLbitfield flags,GLuint64 nanos){
    if(atomic_load(&pause_wait)){
        atomic_store(&wait_entered,1);
        struct timespec delay={0,1000000};
        while(atomic_load(&pause_wait))nanosleep(&delay,0);
    }
    return glClientWaitSync(sync,flags,nanos);
}
static int offset_clock(clockid_t id,struct timespec *value){
    int r=clock_gettime(id,value);
    if(r==0)value->tv_sec+=(time_t)atomic_load(&clock_offset);
    return r;
}
#define malloc counted_malloc
#define glUnmapBuffer fault_unmap
#define glClientWaitSync paused_wait
#define clock_gettime offset_clock
#include "h8_gpu.c"
#undef malloc
#undef glUnmapBuffer
#undef glClientWaitSync
#undef clock_gettime
/* Instrumentation is host-only; compiled Android translation unit is unchanged. */
JNIEXPORT jlong JNICALL Java_com_hiro_ulike_CorePixels1950_control(JNIEnv *env,jclass owner,jint mode){
    (void)env;(void)owner;
    switch(mode){
    case 0:return atomic_load(&allocations);
    case 1:atomic_store(&fail_allocation,1);return 0;
    case 2:atomic_store(&fail_unmap,1);return 0;
    case 3:atomic_store(&wait_entered,0);atomic_store(&pause_wait,1);return 0;
    case 4:return atomic_load(&wait_entered);
    case 5:atomic_store(&pause_wait,0);return 0;
    case 6:atomic_fetch_add(&clock_offset,31);return 0;
    case 7:{
        if(pthread_mutex_lock(&scratch_lock1956))return -1;
        int temporary=0;size_t bytes=SCRATCH_RETAIN_LIMIT1956+4;
        int32_t *p=scratch_acquire1956(bytes,&temporary);
        int ok=p&&temporary&&atomic_load(&scratch_accounted1956)==bytes;
        scratch_release1956(p,temporary);
        return ok&&atomic_load(&scratch_accounted1956)==0;
    }
    default:return -1;
    }
}
