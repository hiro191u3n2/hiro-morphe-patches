/* GX1-GX8/GX17-GX22: private, bounded GLES 3.1 compute owner. Batches retain
 * known-complete workspace capacities under a counted128MiB pool limit.
 * No output is committed by this generic engine.
 * Unsupported precision/extensions/programs return failure for exact CPU fallback.
 * Camera EGL objects are never shared or terminated. */
#include <jni.h>
#include <EGL/egl.h>
#include <GLES3/gl31.h>
#include <stdint.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <limits.h>
#include <time.h>
#include "shader_sources1960.h"
#define JNI1960(n) Java_com_hiro_ulike_GpuNoise1960_##n
#define SLOTS 24
#define BASE_PROGRAMS 9
#define VARIANTS 3
#define PROGRAMS (BASE_PROGRAMS*VARIANTS)
#define POOL_BYTES (128u*1024u*1024u)
#define STAGING_BYTES (64u*1024u*1024u)
#define MAX_BYTES (512u*1024u*1024u)
#define FENCE_TIMEOUT_NS 5000000000ULL
static const char *const source_names[BASE_PROGRAMS]={"strong1960","single1960","analysis1960","geometry1960","analysis1961","residual1961","protection1961","compare1961","finish1961"};
static const char *const sources[BASE_PROGRAMS]={strong1960_source,single1960_source,analysis1960_source,geometry1960_source,analysis1961_source,residual1961_source,protection1961_source,compare1961_source,finish1961_source};
typedef struct {
 EGLDisplay display; EGLContext context; EGLSurface surface;
 GLuint programs[PROGRAMS],buffers[SLOTS];
 GLint uLocation[PROGRAMS],fLocation[PROGRAMS],uCount[PROGRAMS],fCount[PROGRAMS],localX[PROGRAMS];
 unsigned char attempted[PROGRAMS];size_t capacity[SLOTS],used[SLOTS],total;
 void *staging;size_t stagingCapacity;int poolRequested;
 GLsync fences[2];uint64_t tickets[2],fenceGeneration[2],ticketSequence,submitted,completed;
 GLint64 maxStorage;GLint maxGroups, maxBindings,maxLocal,maxInvocations,maxShared;int initialized,disabled,failed;
 uint64_t sequence,token;char environment[2048],failure[256];
} State;
static State state;
static int clean_gl(void){return glGetError()==GL_NO_ERROR;}
static void fail(const char *reason){state.failed=1;snprintf(state.failure,sizeof(state.failure),"%s",reason);}
static int precision64(void){const char *e=(const char*)glGetString(GL_EXTENSIONS);return e && strstr(e,"GL_EXT_shader_explicit_arithmetic_types_float64")!=NULL;}
static int initialize(void){
 if(state.disabled)return 0;if(state.initialized)return 1;
 if(eglGetCurrentContext()!=EGL_NO_CONTEXT){state.disabled=1;return 0;}
 const EGLint configAttrs[]={EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RENDERABLE_TYPE,0x40,EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_ALPHA_SIZE,8,EGL_NONE};
 const EGLint surfaceAttrs[]={EGL_WIDTH,1,EGL_HEIGHT,1,EGL_NONE};
 const EGLint contextAttrs[]={EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE};
 EGLConfig config;EGLint count=0;
 state.display=eglGetDisplay(EGL_DEFAULT_DISPLAY);
 if(state.display==EGL_NO_DISPLAY || !eglInitialize(state.display,NULL,NULL) || !eglBindAPI(EGL_OPENGL_ES_API) || !eglChooseConfig(state.display,configAttrs,&config,1,&count)||count!=1){state.disabled=1;return 0;}
 state.surface=eglCreatePbufferSurface(state.display,config,surfaceAttrs);
 state.context=eglCreateContext(state.display,config,EGL_NO_CONTEXT,contextAttrs);
 if(state.surface==EGL_NO_SURFACE || state.context==EGL_NO_CONTEXT || !eglMakeCurrent(state.display,state.surface,state.surface,state.context)){state.disabled=1;return 0;}
 GLint major=0,minor=0,blocks=0,invocations=0,local=0;
 glGetIntegerv(GL_MAJOR_VERSION,&major);glGetIntegerv(GL_MINOR_VERSION,&minor);
 glGetIntegerv(GL_MAX_COMPUTE_SHADER_STORAGE_BLOCKS,&blocks);glGetIntegerv(GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS,&state.maxBindings);
 glGetIntegerv(GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS,&invocations);glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_SIZE,0,&local);
 state.maxLocal=local;state.maxInvocations=invocations;glGetIntegerv(GL_MAX_COMPUTE_SHARED_MEMORY_SIZE,&state.maxShared);
 glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT,0,&state.maxGroups);glGetInteger64v(GL_MAX_SHADER_STORAGE_BLOCK_SIZE,&state.maxStorage);
 if(major<3||(major==3&&minor<1)||blocks<8||state.maxBindings<8||invocations<64||local<64||state.maxGroups<1||state.maxStorage<4||!clean_gl()){state.disabled=1;return 0;}
 const char *vendor=(const char*)glGetString(GL_VENDOR),*renderer=(const char*)glGetString(GL_RENDERER),*version=(const char*)glGetString(GL_VERSION),*glsl=(const char*)glGetString(GL_SHADING_LANGUAGE_VERSION);
 snprintf(state.environment,sizeof(state.environment),"GX1961:%s|%s|%s|%s|ssbo=%d|maxStorage=%lld|fp64=%d|source=%s",vendor?vendor:"",renderer?renderer:"",version?version:"",glsl?glsl:"",blocks,(long long)state.maxStorage,precision64(),GPU1960_SOURCE_SHA256);
 state.initialized=1;return 1;
}
static int program(int id){
 if(id<0||id>=PROGRAMS||!initialize())return 0;
 if(state.attempted[id])return state.programs[id]!=0;state.attempted[id]=1;
 GLuint shader=glCreateShader(GL_COMPUTE_SHADER),p=0;GLint ok=0;
 if(!shader)return 0;
 int base=id%BASE_PROGRAMS,variant=id/BASE_PROGRAMS,requested=variant==1?32:variant==2?128:64;
 const char *original=sources[base],*newline=strchr(original,'\n');
 if(!newline){glDeleteShader(shader);return 0;}
 char define[80];snprintf(define,sizeof(define),"\n#define GX_LOCAL_SIZE %d\n",requested);
 GLint prefix=(GLint)(newline-original);const char *parts[3]={original,define,newline+1};GLint lengths[3]={prefix,(GLint)strlen(define),(GLint)strlen(newline+1)};
 glShaderSource(shader,3,parts,lengths);glCompileShader(shader);glGetShaderiv(shader,GL_COMPILE_STATUS,&ok);
 if(ok){p=glCreateProgram();if(p){glAttachShader(p,shader);glLinkProgram(p);glGetProgramiv(p,GL_LINK_STATUS,&ok);}}
 if(!ok){char message[192]={0};glGetShaderInfoLog(shader,sizeof(message)-1,NULL,message);snprintf(state.failure,sizeof(state.failure),"compile %s: %.190s",source_names[base],message);}
 glDeleteShader(shader);if(!ok||!p||!clean_gl()){if(p)glDeleteProgram(p);return 0;}
 state.uLocation[id]=glGetUniformLocation(p,"u[0]");state.fLocation[id]=glGetUniformLocation(p,"f[0]");
 GLint active=0;glGetProgramiv(p,GL_ACTIVE_UNIFORMS,&active);
 for(GLint i=0;i<active;i++){char name[128];GLint size=0;GLenum type=0;glGetActiveUniform(p,(GLuint)i,sizeof(name),NULL,&size,&type,name);if(!strcmp(name,"u[0]")&&type==GL_INT)state.uCount[id]=size;if(!strcmp(name,"f[0]")&&type==GL_FLOAT)state.fCount[id]=size;}
 if(state.uLocation[id]<0||state.uCount[id]<32||state.uCount[id]>32||state.fCount[id]>32||!clean_gl()){glDeleteProgram(p);return 0;}
 GLint workgroup[3]={0};glGetProgramiv(p,GL_COMPUTE_WORK_GROUP_SIZE,workgroup);
 if(workgroup[0]<1||workgroup[0]>state.maxLocal||workgroup[0]>state.maxInvocations||workgroup[1]!=1||workgroup[2]!=1||!clean_gl()){glDeleteProgram(p);return 0;}
 state.localX[id]=workgroup[0];state.programs[id]=p;return 1;
}
static int active(jlong token){return state.initialized && !state.disabled && !state.failed && token>0 && (uint64_t)token==state.token;}
static int slot_valid(int slot){return slot>=0&&slot<SLOTS;}
static int allocate(int slot,size_t bytes){
 if(!slot_valid(slot)||bytes==0||bytes>MAX_BYTES||(uint64_t)bytes>(uint64_t)state.maxStorage)return 0;
 state.used[slot]=bytes;if(state.capacity[slot]>=bytes)return 1;
 size_t cap=(bytes+4095u)&~(size_t)4095u;if(cap>MAX_BYTES||(uint64_t)cap>(uint64_t)state.maxStorage)cap=bytes;
 if(state.total-state.capacity[slot]+state.stagingCapacity>MAX_BYTES-cap)return 0;
 if(!state.buffers[slot])glGenBuffers(1,&state.buffers[slot]);glBindBuffer(GL_SHADER_STORAGE_BUFFER,state.buffers[slot]);
 glBufferData(GL_SHADER_STORAGE_BUFFER,(GLsizeiptr)cap,NULL,GL_DYNAMIC_COPY);
 if(!clean_gl()){fail("SSBO allocation GL error");return 0;}
 state.total=state.total-state.capacity[slot]+cap;state.capacity[slot]=cap;state.submitted++;return 1;
}
static int drain(void){
 if(state.completed>=state.submitted)return 1;
 GLsync fence=glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE,0);if(!fence||!clean_gl()){fail("GPU fence creation");return 0;}
 glFlush();GLenum status=glClientWaitSync(fence,GL_SYNC_FLUSH_COMMANDS_BIT,FENCE_TIMEOUT_NS);
 glDeleteSync(fence);if((status!=GL_ALREADY_SIGNALED&&status!=GL_CONDITION_SATISFIED)||!clean_gl()){fail("GPU fence wait timeout/error");state.disabled=1;return 0;}state.completed=state.submitted;return 1;
}
JNIEXPORT jint JNICALL JNI1960(nativeAbi)(JNIEnv *env,jclass cls){(void)env;(void)cls;return 19601;}
JNIEXPORT jlong JNICALL JNI1960(openNative)(JNIEnv *env,jclass cls){(void)env;(void)cls;if(state.token||!initialize())return 0;state.failed=0;state.failure[0]=0;state.poolRequested=0;memset(state.used,0,sizeof(state.used));state.token=++state.sequence;if(!state.token)state.token=++state.sequence;return (jlong)state.token;}
JNIEXPORT jboolean JNICALL JNI1960(supportedNative)(JNIEnv *env,jclass cls,jint id){(void)env;(void)cls;return program(id)?JNI_TRUE:JNI_FALSE;}
JNIEXPORT jint JNICALL JNI1960(workgroupNative)(JNIEnv *env,jclass cls,jint id){(void)env;(void)cls;return program(id)?state.localX[id]:0;}
JNIEXPORT jlong JNICALL JNI1960(retainedNative)(JNIEnv *env,jclass cls){(void)env;(void)cls;return (jlong)(state.total+state.stagingCapacity);}
JNIEXPORT jstring JNICALL JNI1960(environmentNative)(JNIEnv *env,jclass cls){(void)cls;return (*env)->NewStringUTF(env,state.environment);}
JNIEXPORT jboolean JNICALL JNI1960(allocateNative)(JNIEnv *env,jclass cls,jlong token,jint slot,jlong bytes){(void)env;(void)cls;if(!active(token)||bytes<1||(uint64_t)bytes>MAX_BYTES)return JNI_FALSE;return allocate(slot,(size_t)bytes)?JNI_TRUE:JNI_FALSE;}
static int upload(JNIEnv *env,jlong token,jint slot,jarray values,int floats){
 if(!active(token)||!slot_valid(slot)||!values)return 0;
 jsize n=(*env)->GetArrayLength(env,values);if(n<1||(uint64_t)n*4u>MAX_BYTES)return 0;
 size_t bytes=(size_t)n*4u;if(!allocate(slot,bytes))return 0;
 void *copy=state.staging;int temporary=0;
 if(bytes>STAGING_BYTES){if(state.total+state.stagingCapacity>MAX_BYTES-bytes)return 0;copy=malloc(bytes);temporary=1;}
 else if(state.stagingCapacity<bytes){
  size_t cap=(bytes+4095u)&~(size_t)4095u;
  if(state.total>MAX_BYTES-cap)return 0;void *replacement=realloc(state.staging,cap);if(!replacement)return 0;
  state.staging=replacement;state.stagingCapacity=cap;copy=replacement;
 }
 if(!copy)return 0;
 if(floats)(*env)->GetFloatArrayRegion(env,(jfloatArray)values,0,n,copy);else (*env)->GetIntArrayRegion(env,(jintArray)values,0,n,copy);
 if((*env)->ExceptionCheck(env)){if(temporary)free(copy);return 0;}
 glBindBuffer(GL_SHADER_STORAGE_BUFFER,state.buffers[slot]);glBufferSubData(GL_SHADER_STORAGE_BUFFER,0,(GLsizeiptr)bytes,copy);if(temporary)free(copy);state.submitted++;
 if(!clean_gl()){fail("GPU input upload error");return 0;}return 1;
}
JNIEXPORT jboolean JNICALL JNI1960(uploadIntsNative)(JNIEnv *e,jclass c,jlong token,jint slot,jintArray a){(void)c;return upload(e,token,slot,a,0)?JNI_TRUE:JNI_FALSE;}
JNIEXPORT jboolean JNICALL JNI1960(uploadFloatsNative)(JNIEnv *e,jclass c,jlong token,jint slot,jfloatArray a){(void)c;return upload(e,token,slot,a,1)?JNI_TRUE:JNI_FALSE;}
JNIEXPORT jboolean JNICALL JNI1960(dispatchNative)(JNIEnv *env,jclass cls,jlong token,jint id,jintArray bindings,jintArray ints,jfloatArray floats,jint invocations){
 (void)cls;if(!active(token)||invocations<1||(uint64_t)invocations>MAX_BYTES/4u||!bindings||!ints||!program(id))return JNI_FALSE;
 jsize nb=(*env)->GetArrayLength(env,bindings),ni=(*env)->GetArrayLength(env,ints),nf=floats?(*env)->GetArrayLength(env,floats):0;
 if(nb<1||nb>8||ni>32||ni<1||nf>32)return JNI_FALSE;
 jint slots[8],u[32]={0};jfloat f[32]={0};
 (*env)->GetIntArrayRegion(env,bindings,0,nb,slots);(*env)->GetIntArrayRegion(env,ints,0,ni,u);if(nf)(*env)->GetFloatArrayRegion(env,floats,0,nf,f);
 if((*env)->ExceptionCheck(env))return JNI_FALSE;
 for(int b=0;b<nb;b++){if(!slot_valid(slots[b])||!state.buffers[slots[b]]||!state.used[slots[b]])return JNI_FALSE;glBindBufferBase(GL_SHADER_STORAGE_BUFFER,(GLuint)b,state.buffers[slots[b]]);}
 glUseProgram(state.programs[id]);if(state.fLocation[id]>=0&&state.fCount[id])glUniform1fv(state.fLocation[id],state.fCount[id],f);
 uint64_t local=(uint64_t)state.localX[id];
 uint64_t maximum=(uint64_t)state.maxGroups*local;uint64_t base=0,total=(uint64_t)invocations;
 while(base<total){uint64_t count=total-base;if(count>maximum)count=maximum;u[31]=(jint)base;glUniform1iv(state.uLocation[id],32,u);glDispatchCompute((GLuint)((count+local-1u)/local),1,1);glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);if(!clean_gl()){fail("GPU compute dispatch error");return JNI_FALSE;}state.submitted++;base+=count;}
 return JNI_TRUE;
}
static jintArray read_one(JNIEnv *env,int slot,int count){
 if(!slot_valid(slot)||count<1||(uint64_t)count*4u>state.used[slot])return NULL;
 glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT);glBindBuffer(GL_SHADER_STORAGE_BUFFER,state.buffers[slot]);
 size_t bytes=(size_t)count*4u;void *mapped=glMapBufferRange(GL_SHADER_STORAGE_BUFFER,0,(GLsizeiptr)bytes,GL_MAP_READ_BIT);if(!mapped||!clean_gl()){fail("GPU readback map failure");return NULL;}
 jintArray output=(*env)->NewIntArray(env,count);if(output)(*env)->SetIntArrayRegion(env,output,0,count,(jint*)mapped);
 GLboolean ok=glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);if(!ok||!clean_gl()||(*env)->ExceptionCheck(env)){fail("GPU readback transaction failure");return NULL;}return output;
}
JNIEXPORT jintArray JNICALL JNI1960(readIntsNative)(JNIEnv *env,jclass cls,jlong token,jint slot,jint count){
 (void)cls;if(!active(token)||!slot_valid(slot)||count<1||(uint64_t)count*4u>state.used[slot]||!drain())return NULL;return read_one(env,slot,count);
}
static int wait_ticket(uint64_t ticket){
 int bank=state.tickets[0]==ticket?0:state.tickets[1]==ticket?1:-1;if(bank<0||!state.fences[bank])return 0;
 GLenum result=glClientWaitSync(state.fences[bank],GL_SYNC_FLUSH_COMMANDS_BIT,FENCE_TIMEOUT_NS);
 if((result!=GL_ALREADY_SIGNALED&&result!=GL_CONDITION_SATISFIED)||!clean_gl()){fail("GPU ticket wait timeout/error");state.disabled=1;return 0;}
 if(state.completed<state.fenceGeneration[bank])state.completed=state.fenceGeneration[bank];
 glDeleteSync(state.fences[bank]);state.fences[bank]=0;state.tickets[bank]=0;return clean_gl();
}
JNIEXPORT jint JNICALL JNI1960(ticketReadyNative)(JNIEnv *env,jclass cls,jlong token,jlong ticket){
 (void)env;(void)cls;if(!active(token)||ticket<=0)return -1;int bank=state.tickets[0]==(uint64_t)ticket?0:state.tickets[1]==(uint64_t)ticket?1:-1;if(bank<0)return -1;
 GLenum ready=glClientWaitSync(state.fences[bank],0,0);
 if(ready==GL_ALREADY_SIGNALED||ready==GL_CONDITION_SATISFIED)return clean_gl()?1:-1;
 if(ready==GL_TIMEOUT_EXPIRED&&clean_gl())return 0;fail("GPU asynchronous fence query failed");state.disabled=1;return -1;
}
JNIEXPORT jboolean JNICALL JNI1960(timeoutNative)(JNIEnv *env,jclass cls,jlong token){(void)env;(void)cls;if(!active(token))return JNI_FALSE;fail("GPU asynchronous fence timeout");state.disabled=1;return JNI_TRUE;}
JNIEXPORT jobjectArray JNICALL JNI1960(readManyNative)(JNIEnv *env,jclass cls,jlong token,jlong ticket,jintArray slotArray,jintArray countArray){
 (void)cls;if(!active(token)||!slotArray||!countArray)return NULL;
 jsize n=(*env)->GetArrayLength(env,slotArray);if(n<1||n>SLOTS||(*env)->GetArrayLength(env,countArray)!=n)return NULL;
 jint slots[SLOTS],counts[SLOTS];(*env)->GetIntArrayRegion(env,slotArray,0,n,slots);(*env)->GetIntArrayRegion(env,countArray,0,n,counts);if((*env)->ExceptionCheck(env))return NULL;
 uint64_t total=0;for(int i=0;i<n;i++){if(!slot_valid(slots[i])||counts[i]<1||(uint64_t)counts[i]*4u>state.used[slots[i]])return NULL;total+=(uint64_t)counts[i]*4u;}if(total>MAX_BYTES)return NULL;
 if(ticket>0?!wait_ticket((uint64_t)ticket):!drain())return NULL;
 jclass arrayClass=(*env)->FindClass(env,"[I");if(!arrayClass)return NULL;jobjectArray result=(*env)->NewObjectArray(env,n,arrayClass,NULL);(*env)->DeleteLocalRef(env,arrayClass);if(!result)return NULL;
 for(int i=0;i<n;i++){jintArray output=read_one(env,slots[i],counts[i]);if(!output){(*env)->DeleteLocalRef(env,result);return NULL;}(*env)->SetObjectArrayElement(env,result,i,output);(*env)->DeleteLocalRef(env,output);if((*env)->ExceptionCheck(env)){(*env)->DeleteLocalRef(env,result);return NULL;}}
 return result;
}
#define BATCH_ARGS jlong token,jintArray kindArray,jintArray slotArray,jlongArray byteArray,jintArray shaderArray,jobjectArray payloadArray,jobjectArray bindingArray,jobjectArray uniformArray,jobjectArray floatArray,jintArray countArray
#define BATCH_PASS token,kindArray,slotArray,byteArray,shaderArray,payloadArray,bindingArray,uniformArray,floatArray,countArray
static int batch(JNIEnv *env,BATCH_ARGS){
 if(!active(token)||!kindArray||!slotArray||!byteArray||!shaderArray||!payloadArray||!bindingArray||!uniformArray||!floatArray||!countArray)return 0;
 jsize n=(*env)->GetArrayLength(env,kindArray);if(n<1||n>128)return 0;
 jarray arrays[]={slotArray,byteArray,shaderArray,payloadArray,bindingArray,uniformArray,floatArray,countArray};for(int a=0;a<8;a++)if((*env)->GetArrayLength(env,arrays[a])!=n)return 0;
 jint kinds[128],slots[128],shaders[128],counts[128];jlong bytes[128];
 (*env)->GetIntArrayRegion(env,kindArray,0,n,kinds);(*env)->GetIntArrayRegion(env,slotArray,0,n,slots);(*env)->GetIntArrayRegion(env,shaderArray,0,n,shaders);(*env)->GetIntArrayRegion(env,countArray,0,n,counts);(*env)->GetLongArrayRegion(env,byteArray,0,n,bytes);if((*env)->ExceptionCheck(env))return 0;
 state.poolRequested=1;
 for(int i=0;i<n;i++){
  int ok=0;
  if(kinds[i]==0)ok=bytes[i]>0&&(uint64_t)bytes[i]<=MAX_BYTES&&allocate(slots[i],(size_t)bytes[i]);
  else if(kinds[i]==1||kinds[i]==2){jobject payload=(*env)->GetObjectArrayElement(env,payloadArray,i);ok=upload(env,token,slots[i],(jarray)payload,kinds[i]==2);if(payload)(*env)->DeleteLocalRef(env,payload);}
  else if(kinds[i]==3){jintArray bindings=(*env)->GetObjectArrayElement(env,bindingArray,i),u=(*env)->GetObjectArrayElement(env,uniformArray,i);jfloatArray f=(*env)->GetObjectArrayElement(env,floatArray,i);ok=JNI1960(dispatchNative)(env,NULL,token,shaders[i],bindings,u,f,counts[i]);if(bindings)(*env)->DeleteLocalRef(env,bindings);if(u)(*env)->DeleteLocalRef(env,u);if(f)(*env)->DeleteLocalRef(env,f);}
  if(!ok||(*env)->ExceptionCheck(env)){fail("GPU batch command failed");return 0;}
 }
 return 1;
}
JNIEXPORT jboolean JNICALL JNI1960(batchNative)(JNIEnv *env,jclass cls,BATCH_ARGS){(void)cls;return batch(env,BATCH_PASS)?JNI_TRUE:JNI_FALSE;}
JNIEXPORT jobjectArray JNICALL JNI1960(executeNative)(JNIEnv *env,jclass cls,BATCH_ARGS,jintArray reads,jintArray lengths){if(!batch(env,BATCH_PASS))return NULL;return JNI1960(readManyNative)(env,cls,token,0,reads,lengths);}
JNIEXPORT jlong JNICALL JNI1960(submitNative)(JNIEnv *env,jclass cls,jlong token,jint bank,jintArray kindArray,jintArray slotArray,jlongArray byteArray,jintArray shaderArray,jobjectArray payloadArray,jobjectArray bindingArray,jobjectArray uniformArray,jobjectArray floatArray,jintArray countArray){
 (void)cls;if(bank<0||bank>1||state.tickets[bank]||!batch(env,BATCH_PASS))return 0;
 GLsync fence=glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE,0);if(!fence||!clean_gl()){fail("GPU batch submission fence");return 0;}glFlush();
 uint64_t ticket=++state.ticketSequence;if(!ticket)ticket=++state.ticketSequence;state.fences[bank]=fence;state.tickets[bank]=ticket;state.fenceGeneration[bank]=state.submitted;return (jlong)ticket;
}
static void release_buffers(void){glDeleteBuffers(SLOTS,state.buffers);memset(state.buffers,0,sizeof(state.buffers));memset(state.capacity,0,sizeof(state.capacity));state.total=0;free(state.staging);state.staging=NULL;state.stagingCapacity=0;}
JNIEXPORT jboolean JNICALL JNI1960(trimNative)(JNIEnv *env,jclass cls){(void)env;(void)cls;if(state.token||state.disabled||!state.initialized)return JNI_FALSE;release_buffers();return clean_gl()?JNI_TRUE:JNI_FALSE;}
JNIEXPORT jboolean JNICALL JNI1960(closeNative)(JNIEnv *env,jclass cls,jlong token){
 (void)env;(void)cls;if(token<=0||(uint64_t)token!=state.token)return JNI_FALSE;
 int priorFailure=state.failed;int completed=state.disabled?0:drain();
 if(!completed){state.disabled=1;state.token=0;return JNI_FALSE;}
 for(int b=0;b<2;b++){if(state.fences[b])glDeleteSync(state.fences[b]);state.fences[b]=0;state.tickets[b]=0;}
 int ok=!priorFailure;
 if(!state.poolRequested||priorFailure)release_buffers();
 else{
  /* Reuse only known-complete capacities, never photograph contents. Retained
   * cache remains visible to capture admission and can be reclaimed when idle. */
  while(state.total+state.stagingCapacity>POOL_BYTES){int largest=-1;for(int i=0;i<SLOTS;i++)if(state.capacity[i]&&(largest<0||state.capacity[i]>state.capacity[largest]))largest=i;if(largest<0){free(state.staging);state.staging=NULL;state.stagingCapacity=0;break;}glDeleteBuffers(1,&state.buffers[largest]);state.buffers[largest]=0;state.total-=state.capacity[largest];state.capacity[largest]=0;}
 }
 memset(state.used,0,sizeof(state.used));state.token=0;if(!clean_gl())ok=0;return ok?JNI_TRUE:JNI_FALSE;
}
