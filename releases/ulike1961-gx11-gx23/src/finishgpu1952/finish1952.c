/* Exact GLES 3.1 eligible final chain. The photographed input is immutable.
 * Resident intermediate support is complete before its dependent sharp tile.
 * One final fence/readback; no Java pin is held while allocating or waiting. */
#include <jni.h>
#include <EGL/egl.h>
#include <GLES3/gl31.h>
#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include "finish_source1952.h"
#define JNI_FN(n) Java_com_hiro_ulike_GpuFinish1952_##n
#define MAX_BYTES (64u*1024u*1024u)
#define TILE_ROWS 32
#define FENCE_NS 3000000000ULL
static pthread_mutex_t mutex=PTHREAD_MUTEX_INITIALIZER;
typedef struct {
 EGLDisplay display;EGLContext context;EGLSurface surface;
 GLuint program,buffers[4];size_t capacities[4];
 void *workspace[3];size_t workspace_capacity[3];
 GLint uniforms[15],max_groups[3];GLint64 max_storage;
 int initialized,disabled;
} State;
static State gpu;
static int clean(void){return glGetError()==GL_NO_ERROR;}
static void leave(EGLenum previous){
 if(gpu.display!=EGL_NO_DISPLAY && eglGetCurrentContext()==gpu.context)
  if(!eglMakeCurrent(gpu.display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT))gpu.disabled=1;
 if(previous!=EGL_NONE)(void)eglBindAPI(previous);
}
static int enter(EGLenum *previous){
 *previous=eglQueryAPI();
 /* No borrowing/rebinding the camera context or its shared objects. */
 if(gpu.disabled || eglGetCurrentContext()!=EGL_NO_CONTEXT)return 0;
 if(!eglBindAPI(EGL_OPENGL_ES_API))return 0;
 if(gpu.initialized) {
  if(eglMakeCurrent(gpu.display,gpu.surface,gpu.surface,gpu.context))return 1;
  gpu.disabled=1;return 0;
 }
 const EGLint config[]={EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RENDERABLE_TYPE,0x40,
  EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_ALPHA_SIZE,8,EGL_NONE};
 const EGLint surface[]={EGL_WIDTH,1,EGL_HEIGHT,1,EGL_NONE};
 const EGLint context[]={EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE};
 EGLConfig chosen=0;EGLint count=0;
 gpu.display=eglGetDisplay(EGL_DEFAULT_DISPLAY);
 if(gpu.display==EGL_NO_DISPLAY || !eglInitialize(gpu.display,0,0) ||
  !eglChooseConfig(gpu.display,config,&chosen,1,&count) || count!=1)goto fail;
 gpu.surface=eglCreatePbufferSurface(gpu.display,chosen,surface);
 gpu.context=eglCreateContext(gpu.display,chosen,EGL_NO_CONTEXT,context);
 if(gpu.surface==EGL_NO_SURFACE || gpu.context==EGL_NO_CONTEXT ||
  !eglMakeCurrent(gpu.display,gpu.surface,gpu.surface,gpu.context))goto fail;
 GLint major=0,minor=0,blocks=0,bindings=0,invocations=0,sizes[3]={0};
 glGetIntegerv(GL_MAJOR_VERSION,&major);glGetIntegerv(GL_MINOR_VERSION,&minor);
 glGetIntegerv(GL_MAX_COMPUTE_SHADER_STORAGE_BLOCKS,&blocks);
 glGetIntegerv(GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS,&bindings);
 glGetIntegerv(GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS,&invocations);
 glGetInteger64v(GL_MAX_SHADER_STORAGE_BLOCK_SIZE,&gpu.max_storage);
 for(int i=0;i<3;i++) {
  glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT,(GLuint)i,&gpu.max_groups[i]);
  glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_SIZE,(GLuint)i,&sizes[i]);
 }
 if(major<3 || (major==3&&minor<1) || blocks<4 || bindings<4 || invocations<64 ||
  sizes[0]<8 || sizes[1]<8 || sizes[2]<1 || gpu.max_storage<4 ||
  gpu.max_groups[0]<1 || gpu.max_groups[1]<1)goto fail;
 GLuint shader=glCreateShader(GL_COMPUTE_SHADER);
 const GLchar *source=finish1952_source;
 if(!shader)goto fail;
 glShaderSource(shader,1,&source,0);glCompileShader(shader);
 GLint ok=0;glGetShaderiv(shader,GL_COMPILE_STATUS,&ok);
 if(!ok){glDeleteShader(shader);goto fail;}
 gpu.program=glCreateProgram();glAttachShader(gpu.program,shader);
 glLinkProgram(gpu.program);glDeleteShader(shader);
 glGetProgramiv(gpu.program,GL_LINK_STATUS,&ok);if(!ok)goto fail;
 const char *names[]={"uWidth","uRows","uOrigin","uBegin","uEnd","uOutputOrigin",
  "uPolicyOrigin","uMoire","uSharp","uGain","uFloor","uLimit","uTexture","uHalo","uSequential"};
 for(int i=0;i<15;i++)if((gpu.uniforms[i]=glGetUniformLocation(gpu.program,names[i]))<0)goto fail;
 glGenBuffers(4,gpu.buffers);if(!clean())goto fail;
 gpu.initialized=1;return 1;
fail:
 gpu.disabled=1;return 0;
}
static int buffer(int slot,size_t bytes,const void *source){
 if(!bytes || bytes>MAX_BYTES || (uint64_t)bytes>(uint64_t)gpu.max_storage)return 0;
 glBindBuffer(GL_SHADER_STORAGE_BUFFER,gpu.buffers[slot]);
 if(gpu.capacities[slot]<bytes){
  glBufferData(GL_SHADER_STORAGE_BUFFER,(GLsizeiptr)bytes,0,GL_DYNAMIC_COPY);
  if(!clean())return 0;
  gpu.capacities[slot]=bytes;
 }
 if(source)glBufferSubData(GL_SHADER_STORAGE_BUFFER,0,(GLsizeiptr)bytes,source);
 glBindBufferBase(GL_SHADER_STORAGE_BUFFER,(GLuint)slot,gpu.buffers[slot]);return clean();
}
static void *workspace(int slot,size_t bytes){
 if(!bytes || bytes>MAX_BYTES)return 0;
 if(gpu.workspace_capacity[slot]<bytes){
  void *next=realloc(gpu.workspace[slot],bytes);if(!next)return 0;
  gpu.workspace[slot]=next;gpu.workspace_capacity[slot]=bytes;
 }
 return gpu.workspace[slot];
}
static int dispatch(int width,int rows,int origin,int begin,int end,int output_origin,
 int policy_origin,int moire,int sharp,int gain,int floor,int limit,int texture,int halo,
 int corrected_center,GLuint source,GLuint middle,GLuint output){
 int gx=(width+7)/8,gy=(end-begin+7)/8;
 if(gx<1 || gy<1 || gx>gpu.max_groups[0] || gy>gpu.max_groups[1])return 0;
 int values[15]={width,rows,origin,begin,end,output_origin,policy_origin,moire,sharp,
  gain,floor,limit,texture,halo,corrected_center};
 glUseProgram(gpu.program);
 for(int i=0;i<15;i++)glUniform1i(gpu.uniforms[i],values[i]);
 glBindBufferBase(GL_SHADER_STORAGE_BUFFER,0,source);
 glBindBufferBase(GL_SHADER_STORAGE_BUFFER,1,middle);
 glBindBufferBase(GL_SHADER_STORAGE_BUFFER,2,output);
 glDispatchCompute((GLuint)gx,(GLuint)gy,1);return clean();
}
static int await_read(size_t bytes,int32_t *result){
 glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT|GL_BUFFER_UPDATE_BARRIER_BIT);
 GLsync fence=glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE,0);
 if(!fence || !clean())return 0;
 glFlush();GLenum status=glClientWaitSync(fence,GL_SYNC_FLUSH_COMMANDS_BIT,FENCE_NS);
 glDeleteSync(fence);
 if((status!=GL_ALREADY_SIGNALED && status!=GL_CONDITION_SATISFIED) || !clean())return 0;
 glBindBuffer(GL_SHADER_STORAGE_BUFFER,gpu.buffers[2]);
 void *mapped=glMapBufferRange(GL_SHADER_STORAGE_BUFFER,0,(GLsizeiptr)bytes,GL_MAP_READ_BIT);
 if(!mapped || !clean())return 0;
 memcpy(result,mapped,bytes);return glUnmapBuffer(GL_SHADER_STORAGE_BUFFER)==GL_TRUE && clean();
}
static int compute(const int32_t *source,const int32_t *policy,int32_t *result,
 int width,int rows,int first,int last,int input_origin,int input_rows,int moire,int sharp,
 int gain,int floor,int limit,int texture,int halo,int sequential){
 const int outer_first=sequential&&sharp?(first>4?first-4:0):first;
 const int outer_last=sequential&&sharp?(last<rows-4?last+4:rows):last;
 size_t source_bytes=(size_t)width*input_rows*4u,output_bytes=(size_t)width*(last-first)*4u;
 size_t middle_bytes=(size_t)width*(outer_last-outer_first)*4u;
 size_t policy_bytes=sharp?output_bytes*4u:4u;
 if(!buffer(0,source_bytes,source) || !buffer(1,middle_bytes,0) ||
  !buffer(2,output_bytes,0) || !buffer(3,policy_bytes,policy))return 0;
 if(moire && sharp){
  /* The first tile's full dependent 4-row sharp halo is ready before sharpening.
   * Each further moire tile writes only its not-yet-computed trailing rows.
   * GLES storage barriers order dependent kernels without a CPU completion wait.
   * Neither pass reads pixels from another tile's partially written destination. */
  int completed=outer_first;
  for(int start=first;start<last;start+=TILE_ROWS){
   int end=start+TILE_ROWS;if(end>last)end=last;
   int needed=sequential?(end<rows-4?end+4:rows):end;
   if(needed>completed && !dispatch(width,rows,input_origin,completed,needed,outer_first,
    first,1,0,0,0,0,0,0,0,gpu.buffers[0],gpu.buffers[0],gpu.buffers[1]))return 0;
   completed=needed;glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
   if(!dispatch(width,rows,sequential?outer_first:input_origin,start,end,first,first,
    0,1,gain,floor,limit,texture,halo,!sequential,
    sequential?gpu.buffers[1]:gpu.buffers[0],gpu.buffers[1],gpu.buffers[2]))return 0;
  }
 }else{
  for(int start=first;start<last;start+=TILE_ROWS){
   int end=start+TILE_ROWS;if(end>last)end=last;
   if(!dispatch(width,rows,input_origin,start,end,first,first,moire,sharp,gain,floor,
    limit,texture,halo,0,gpu.buffers[0],gpu.buffers[0],gpu.buffers[2]))return 0;
  }
 }
 return await_read(output_bytes,result);
}
JNIEXPORT jint JNICALL JNI_FN(nativeAbi)(JNIEnv *env,jclass cls){(void)env;(void)cls;return 19521;}
JNIEXPORT jboolean JNICALL JNI_FN(finishNative)(JNIEnv *env,jclass cls,
 jintArray input,jintArray output,jintArray policy,jint width,jint rows,jint first,jint last,
 jboolean moire,jboolean sharp,jint gain,jint floor,jint limit,jboolean texture,
 jboolean halo,jboolean sequential){
 (void)cls;
 if(!input || !output || !policy || (*env)->IsSameObject(env,input,output) ||
  (*env)->IsSameObject(env,input,policy) || (*env)->IsSameObject(env,output,policy) ||
  width<1 || rows<1 || width>16384 || first<0 || last<first || last>rows ||
  (sharp && (gain<0 || floor<0 || limit<0 || limit>255)))return JNI_FALSE;
 int64_t all=(int64_t)width*rows,count=(int64_t)width*(last-first);
 if(all>INT_MAX || all>(*env)->GetArrayLength(env,input) || all>(*env)->GetArrayLength(env,output) ||
  (sharp?count*4:1)>(*env)->GetArrayLength(env,policy) || count>MAX_BYTES/16)return JNI_FALSE;
 if(first==last)return JNI_TRUE;
 int reach=moire?32:4;if(sequential&&moire&&sharp)reach=36;
 int origin=first>reach?first-reach:0;
 int bottom=last<rows-reach?last+reach:rows,input_rows=bottom-origin;
 size_t input_bytes=(size_t)width*input_rows*4u,output_bytes=(size_t)count*4u;
 size_t policy_bytes=sharp?output_bytes*4u:4u;
 int ok=0;EGLenum previous=EGL_NONE;
 pthread_mutex_lock(&mutex);
 int32_t *source=workspace(0,input_bytes),*settings=workspace(1,policy_bytes),
  *result=workspace(2,output_bytes);
 if(source && settings && result){
  (*env)->GetIntArrayRegion(env,input,origin*width,width*input_rows,(jint*)source);
  if(!(*env)->ExceptionCheck(env))
   (*env)->GetIntArrayRegion(env,policy,0,(jsize)(policy_bytes/4u),(jint*)settings);
  if(!(*env)->ExceptionCheck(env) && enter(&previous)){
   ok=compute(source,settings,result,width,rows,first,last,origin,input_rows,moire,sharp,
    gain,floor,limit,texture,halo,sequential);
   if(!ok)gpu.disabled=1;
  }
  leave(previous);
  /* Final publication only after all dependent tiles and successful readback. */
  if(ok){
   (*env)->SetIntArrayRegion(env,output,first*width,(jsize)count,(jint*)result);
   ok=!(*env)->ExceptionCheck(env);
  }
 }
 pthread_mutex_unlock(&mutex);return ok?JNI_TRUE:JNI_FALSE;
}
