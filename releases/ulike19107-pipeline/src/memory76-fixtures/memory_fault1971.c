/* Host-only wrappers; the released engine and shaders are compiled unchanged. */
#include <jni.h>
#include <GLES3/gl31.h>
#include <stdatomic.h>
static atomic_int fault,unmaps;
void __real_glBufferData(GLenum,GLsizeiptr,const void*,GLenum);
void __real_glBufferSubData(GLenum,GLintptr,GLsizeiptr,const void*);
void __real_glCopyBufferSubData(GLenum,GLenum,GLintptr,GLintptr,GLsizeiptr);
void __real_glDispatchCompute(GLuint,GLuint,GLuint);
GLboolean __real_glUnmapBuffer(GLenum);
GLenum __real_glClientWaitSync(GLsync,GLbitfield,GLuint64);
static void invalid(void){glBindBuffer(0,0);}
void __wrap_glBufferData(GLenum target,GLsizeiptr size,const void *data,GLenum usage){__real_glBufferData(target,size,data,usage);if(atomic_load(&fault)==1)invalid();}
void __wrap_glBufferSubData(GLenum target,GLintptr offset,GLsizeiptr size,const void *data){__real_glBufferSubData(target,offset,size,data);if(atomic_load(&fault)==2)invalid();}
void __wrap_glCopyBufferSubData(GLenum read,GLenum write,GLintptr from,GLintptr to,GLsizeiptr bytes){__real_glCopyBufferSubData(read,write,from,to,bytes);if(atomic_load(&fault)==7)invalid();}
void __wrap_glDispatchCompute(GLuint x,GLuint y,GLuint z){__real_glDispatchCompute(x,y,z);if(atomic_load(&fault)==3)invalid();}
GLboolean __wrap_glUnmapBuffer(GLenum target){GLboolean ok=__real_glUnmapBuffer(target);int index=atomic_fetch_add(&unmaps,1)+1;return atomic_load(&fault)==4||(atomic_load(&fault)==6&&index==2)?GL_FALSE:ok;}
GLenum __wrap_glClientWaitSync(GLsync sync,GLbitfield flags,GLuint64 timeout){return atomic_load(&fault)==5?GL_TIMEOUT_EXPIRED:__real_glClientWaitSync(sync,flags,timeout);}
JNIEXPORT void JNICALL Java_com_hiro_ulike_Memory1971Test_setFault(JNIEnv *env,jclass cls,jint code){(void)env;(void)cls;atomic_store(&fault,code);atomic_store(&unmaps,0);}
