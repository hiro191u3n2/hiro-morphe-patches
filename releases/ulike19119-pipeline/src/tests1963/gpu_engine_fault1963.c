/* Host-only driver faults. The production JNI and GL graph remain unchanged. */
#include <jni.h>
#include <EGL/egl.h>
#include <GLES3/gl31.h>
#include <stdatomic.h>
static atomic_int fault_mode,pending_map_error,destroyed_contexts,destroyed_surfaces,terminated_displays;
void *__real_glMapBufferRange(GLenum,GLintptr,GLsizeiptr,GLbitfield);
GLenum __real_glGetError(void);
void __real_glGetIntegerv(GLenum,GLint *);
EGLBoolean __real_eglDestroyContext(EGLDisplay,EGLContext);
EGLBoolean __real_eglDestroySurface(EGLDisplay,EGLSurface);
EGLBoolean __real_eglTerminate(EGLDisplay);
void *__wrap_glMapBufferRange(GLenum target,GLintptr offset,GLsizeiptr length,GLbitfield access){
 void *mapped=__real_glMapBufferRange(target,offset,length,access);
 if(mapped&&(access&GL_MAP_READ_BIT)&&atomic_load(&fault_mode)==1)atomic_store(&pending_map_error,1);
 return mapped;
}
GLenum __wrap_glGetError(void){
 if(atomic_exchange(&pending_map_error,0))return GL_INVALID_OPERATION;
 return __real_glGetError();
}
void __wrap_glGetIntegerv(GLenum name,GLint *value){
 __real_glGetIntegerv(name,value);
 if(name==GL_MAJOR_VERSION&&atomic_load(&fault_mode)==2)*value=2;
}
EGLBoolean __wrap_eglDestroyContext(EGLDisplay d,EGLContext c){atomic_fetch_add(&destroyed_contexts,1);return __real_eglDestroyContext(d,c);}
EGLBoolean __wrap_eglDestroySurface(EGLDisplay d,EGLSurface s){atomic_fetch_add(&destroyed_surfaces,1);return __real_eglDestroySurface(d,s);}
EGLBoolean __wrap_eglTerminate(EGLDisplay d){atomic_fetch_add(&terminated_displays,1);return __real_eglTerminate(d);}
JNIEXPORT void JNICALL Java_com_hiro_ulike_GpuEngine1963Test_setEngineFault(JNIEnv *env,jclass cls,jint mode){(void)env;(void)cls;atomic_store(&fault_mode,mode);}
JNIEXPORT jlongArray JNICALL Java_com_hiro_ulike_GpuEngine1963Test_engineFacts(JNIEnv *env,jclass cls){
 (void)cls;jlong facts[3]={atomic_load(&destroyed_contexts),atomic_load(&destroyed_surfaces),atomic_load(&terminated_displays)};
 jlongArray result=(*env)->NewLongArray(env,3);if(result)(*env)->SetLongArrayRegion(env,result,0,3,facts);return result;
}
