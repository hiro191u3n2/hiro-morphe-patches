#include <jni.h>
#include <GLES3/gl31.h>
/* Host-only observation/faults around actual production native commands. */
static int large_uploads, read_fault;
void __real_glBufferSubData(GLenum target,GLintptr offset,GLsizeiptr size,const void *data);
void __wrap_glBufferSubData(GLenum target,GLintptr offset,GLsizeiptr size,const void *data){
 if(size>4)large_uploads++;
 __real_glBufferSubData(target,offset,size,data);
}
GLboolean __real_glUnmapBuffer(GLenum target);
GLboolean __wrap_glUnmapBuffer(GLenum target){
 GLboolean value=__real_glUnmapBuffer(target);
 if(read_fault){read_fault=0;return GL_FALSE;}
 return value;
}
JNIEXPORT jint JNICALL Java_com_hiro_ulike_NativeOverlap1975Test_largeUploads(JNIEnv *env,jclass cls){(void)env;(void)cls;return large_uploads;}
JNIEXPORT void JNICALL Java_com_hiro_ulike_NativeOverlap1975Test_failRead(JNIEnv *env,jclass cls){(void)env;(void)cls;read_fault=1;}
JNIEXPORT void JNICALL Java_com_hiro_ulike_NativeOverlap1975Test_clearFault(JNIEnv *env,jclass cls){(void)env;(void)cls;read_fault=0;}
