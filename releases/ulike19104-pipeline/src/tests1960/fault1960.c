/* Host-only fault observation, linked to unchanged production native engine. */
#include <jni.h>
#include <EGL/egl.h>
#include <GLES3/gl31.h>
#include <stdatomic.h>
#include <stdint.h>
static atomic_int fault_mode,unmap_countdown;
static atomic_ullong fence_calls,map_calls,last_timeout,dispatch_calls,failed_unmaps;
GLenum __real_glClientWaitSync(GLsync,GLbitfield,GLuint64);
GLboolean __real_glUnmapBuffer(GLenum);
void __real_glDispatchCompute(GLuint,GLuint,GLuint);
void __wrap_glDispatchCompute(GLuint x,GLuint y,GLuint z){atomic_fetch_add(&dispatch_calls,1);__real_glDispatchCompute(x,y,z);}
GLenum __wrap_glClientWaitSync(GLsync s,GLbitfield f,GLuint64 timeout){
 atomic_fetch_add(&fence_calls,1);atomic_store(&last_timeout,timeout);
 if(atomic_load(&fault_mode)==1)return GL_TIMEOUT_EXPIRED;
 return __real_glClientWaitSync(s,f,timeout);
}
GLboolean __wrap_glUnmapBuffer(GLenum target){atomic_fetch_add(&map_calls,1);GLboolean result=__real_glUnmapBuffer(target);int mode=atomic_load(&fault_mode);int failure=mode==2||(mode==4&&atomic_fetch_sub(&unmap_countdown,1)==1);if(failure)atomic_fetch_add(&failed_unmaps,1);return failure?GL_FALSE:result;}
JNIEXPORT void JNICALL Java_com_hiro_ulike_Native1960Test_setFault(JNIEnv *e,jclass c,jint n){(void)e;(void)c;atomic_store(&unmap_countdown,n==4?2:0);atomic_store(&fault_mode,n);}
JNIEXPORT jlongArray JNICALL Java_com_hiro_ulike_Native1960Test_faultFacts(JNIEnv *e,jclass c){(void)c;jlong a[4]={(jlong)atomic_load(&fence_calls),(jlong)atomic_load(&map_calls),(jlong)atomic_load(&last_timeout),(jlong)atomic_load(&dispatch_calls)};jlongArray r=(*e)->NewLongArray(e,4);if(r)(*e)->SetLongArrayRegion(e,r,0,4,a);return r;}
static EGLDisplay display;static EGLSurface surface;static EGLContext context;static EGLenum api;
JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_Native1960Test_beginExternal(JNIEnv *e,jclass c){
 (void)e;(void)c;api=eglQueryAPI();if(eglGetCurrentContext()!=EGL_NO_CONTEXT||!eglBindAPI(EGL_OPENGL_ES_API))return JNI_FALSE;
 display=eglGetDisplay(EGL_DEFAULT_DISPLAY);EGLConfig config;EGLint n;
 const EGLint attrs[]={EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RENDERABLE_TYPE,0x40,EGL_NONE},surf[]={EGL_WIDTH,1,EGL_HEIGHT,1,EGL_NONE},ctx[]={EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE};
 if(display==EGL_NO_DISPLAY||!eglInitialize(display,0,0)||!eglChooseConfig(display,attrs,&config,1,&n)||n!=1)return JNI_FALSE;
 surface=eglCreatePbufferSurface(display,config,surf);context=eglCreateContext(display,config,EGL_NO_CONTEXT,ctx);
 return surface!=EGL_NO_SURFACE&&context!=EGL_NO_CONTEXT&&eglMakeCurrent(display,surface,surface,context)?JNI_TRUE:JNI_FALSE;
}
JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_Native1960Test_externalUnchanged(JNIEnv *e,jclass c){(void)e;(void)c;return eglGetCurrentContext()==context&&eglGetCurrentDisplay()==display&&eglGetCurrentSurface(EGL_READ)==surface&&eglGetCurrentSurface(EGL_DRAW)==surface&&eglQueryAPI()==EGL_OPENGL_ES_API?JNI_TRUE:JNI_FALSE;}
JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_Native1960Test_endExternal(JNIEnv *e,jclass c){(void)e;(void)c;int ok=eglMakeCurrent(display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);ok=eglDestroyContext(display,context)&&ok;ok=eglDestroySurface(display,surface)&&ok;ok=eglBindAPI(api)&&ok;return ok?JNI_TRUE:JNI_FALSE;}

JNIEXPORT jlong JNICALL Java_com_hiro_ulike_Facade1961Test_failedUnmaps(JNIEnv *e,jclass c){(void)e;(void)c;return (jlong)atomic_load(&failed_unmaps);}
