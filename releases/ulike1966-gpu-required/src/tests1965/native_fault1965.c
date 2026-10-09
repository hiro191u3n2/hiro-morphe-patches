/* Host-only fault adapter. Never packaged in an Android library. */
#include <jni.h>
#include <EGL/egl.h>
#include <GLES3/gl31.h>
#include <stdatomic.h>
static atomic_int mode;
static atomic_ullong dispatches,waits,unmaps;
GLenum __real_glClientWaitSync(GLsync,GLbitfield,GLuint64);
GLboolean __real_glUnmapBuffer(GLenum);
void __real_glDispatchCompute(GLuint,GLuint,GLuint);
GLenum __wrap_glClientWaitSync(GLsync sync,GLbitfield flags,GLuint64 timeout){atomic_fetch_add(&waits,1);if(atomic_load(&mode)==1)return GL_TIMEOUT_EXPIRED;return __real_glClientWaitSync(sync,flags,timeout);}
GLboolean __wrap_glUnmapBuffer(GLenum target){atomic_fetch_add(&unmaps,1);GLboolean ok=__real_glUnmapBuffer(target);int current=atomic_load(&mode);if(current==3){atomic_store(&mode,0);return GL_FALSE;}return current==2?GL_FALSE:ok;}
void __wrap_glDispatchCompute(GLuint x,GLuint y,GLuint z){atomic_fetch_add(&dispatches,1);__real_glDispatchCompute(x,y,z);}
JNIEXPORT void JNICALL Java_com_hiro_ulike_RequiredNative1965Test_fault(JNIEnv *env,jclass cls,jint value){(void)env;(void)cls;atomic_store(&mode,value);}
JNIEXPORT jlongArray JNICALL Java_com_hiro_ulike_RequiredNative1965Test_facts(JNIEnv *env,jclass cls){(void)cls;jlong values[3]={(jlong)atomic_load(&dispatches),(jlong)atomic_load(&waits),(jlong)atomic_load(&unmaps)};jlongArray result=(*env)->NewLongArray(env,3);if(result)(*env)->SetLongArrayRegion(env,result,0,3,values);return result;}
static EGLDisplay display;static EGLContext context;static EGLSurface surface;
JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_RequiredNative1965Test_external(JNIEnv *env,jclass cls,jint operation){
 (void)env;(void)cls;if(operation==1){return eglGetCurrentContext()==context&&eglGetCurrentSurface(EGL_DRAW)==surface?JNI_TRUE:JNI_FALSE;}
 if(operation==2){int ok=eglMakeCurrent(display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);ok=eglDestroyContext(display,context)&&ok;ok=eglDestroySurface(display,surface)&&ok;return ok?JNI_TRUE:JNI_FALSE;}
 EGLConfig config;EGLint count=0;const EGLint attributes[]={EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RENDERABLE_TYPE,0x40,EGL_NONE},pbuffer[]={EGL_WIDTH,1,EGL_HEIGHT,1,EGL_NONE},ctx[]={EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE};
 display=eglGetDisplay(EGL_DEFAULT_DISPLAY);if(display==EGL_NO_DISPLAY||!eglInitialize(display,0,0)||!eglBindAPI(EGL_OPENGL_ES_API)||!eglChooseConfig(display,attributes,&config,1,&count)||count!=1)return JNI_FALSE;
 surface=eglCreatePbufferSurface(display,config,pbuffer);context=eglCreateContext(display,config,EGL_NO_CONTEXT,ctx);return surface!=EGL_NO_SURFACE&&context!=EGL_NO_CONTEXT&&eglMakeCurrent(display,surface,surface,context)?JNI_TRUE:JNI_FALSE;
}
