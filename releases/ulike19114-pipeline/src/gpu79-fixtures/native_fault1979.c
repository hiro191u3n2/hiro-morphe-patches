#define _POSIX_C_SOURCE 200809L
#include <jni.h>
#include <GLES3/gl31.h>
#include <stdatomic.h>
#include <time.h>
/* Test-only scalar faults and contention. Production engine/shaders are linked
 * verbatim; delay outcomes prove decisions, never physical device performance. */
static _Atomic int dispatches,delay_ms,fail_read;
void __real_glDispatchCompute(GLuint x,GLuint y,GLuint z);
void __wrap_glDispatchCompute(GLuint x,GLuint y,GLuint z){
    atomic_fetch_add(&dispatches,1);int ms=atomic_load(&delay_ms);
    if(ms>0){struct timespec t={ms/1000,(ms%1000)*1000000L};nanosleep(&t,NULL);}
    __real_glDispatchCompute(x,y,z);
}
void *__real_glMapBufferRange(GLenum target,GLintptr offset,GLsizeiptr length,GLbitfield access);
void *__wrap_glMapBufferRange(GLenum target,GLintptr offset,GLsizeiptr length,GLbitfield access){
    if((access&GL_MAP_READ_BIT)&&atomic_exchange(&fail_read,0))return NULL;
    return __real_glMapBufferRange(target,offset,length,access);
}
JNIEXPORT jint JNICALL Java_com_hiro_ulike_NativeSpeed1979Test_dispatches(JNIEnv *e,jclass c){(void)e;(void)c;return atomic_load(&dispatches);}
JNIEXPORT void JNICALL Java_com_hiro_ulike_NativeSpeed1979Test_delay(JNIEnv *e,jclass c,jint ms){(void)e;(void)c;atomic_store(&delay_ms,ms);}
JNIEXPORT void JNICALL Java_com_hiro_ulike_NativeSpeed1979Test_failRead(JNIEnv *e,jclass c){(void)e;(void)c;atomic_store(&fail_read,1);}
JNIEXPORT void JNICALL Java_com_hiro_ulike_NativeSpeed1979Test_clearFault(JNIEnv *e,jclass c){(void)e;(void)c;atomic_store(&fail_read,0);atomic_store(&delay_ms,0);}
