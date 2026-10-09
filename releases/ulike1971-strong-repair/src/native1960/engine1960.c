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
#ifndef GL_CONTEXT_LOST
#define GL_CONTEXT_LOST 0x0507
#endif
#define JNI1960(n) Java_com_hiro_ulike_GpuNoise1960_##n
#define SLOTS 24
#define BASE_PROGRAMS 9
#define VARIANTS 3
#define STRONG_MODES 4
#define STRONG_PROGRAM_BASE (BASE_PROGRAMS*VARIANTS)
#define PROGRAMS (STRONG_PROGRAM_BASE+STRONG_MODES*VARIANTS)
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
 uint64_t sequence,token;char environment[2048],failure[256];int failureCode1971;
} State;
static State state;
static int clean_gl(void){return glGetError()==GL_NO_ERROR;}
/* Fixed scalar diagnostics preserve the first failure. Generic batch wrappers
 * must not erase the originating allocation/upload/dispatch/read/fence fault. */
enum { FAILURE_NONE1971=0,FAILURE_ALLOCATION1971=1,FAILURE_UPLOAD1971=2,
 FAILURE_DISPATCH1971=3,FAILURE_READBACK1971=4,FAILURE_FENCE1971=5,FAILURE_UNKNOWN1971=6 };
static void note_failure1971(int code,const char *reason){
 if(!state.failureCode1971){state.failureCode1971=code;snprintf(state.failure,sizeof(state.failure),"%s",reason);}
}
static void fail_code1971(int code,const char *reason){state.failed=1;note_failure1971(code,reason);}
static void fail(const char *reason){
 int code=strstr(reason,"allocation")?FAILURE_ALLOCATION1971:
  strstr(reason,"readback")?FAILURE_READBACK1971:
  (strstr(reason,"fence")||strstr(reason,"ticket"))?FAILURE_FENCE1971:
  (strstr(reason,"upload")||strstr(reason,"input")||strstr(reason,"coefficient"))?FAILURE_UPLOAD1971:
  (strstr(reason,"dispatch")||strstr(reason,"compute"))?FAILURE_DISPATCH1971:FAILURE_UNKNOWN1971;
 fail_code1971(code,reason);
}
static int precision64(void){const char *e=(const char*)glGetString(GL_EXTENSIONS);return e && strstr(e,"GL_EXT_shader_explicit_arithmetic_types_float64")!=NULL;}
/* No commands or buffers exist while initialization is incomplete. Release
 * only the private objects we created; eglTerminate on the default display
 * could invalidate the camera's separately owned contexts. */
static int initialization_failed(void){
 state.disabled=1;
 if(state.display!=EGL_NO_DISPLAY){
  if(state.context!=EGL_NO_CONTEXT&&eglGetCurrentContext()==state.context)
   eglMakeCurrent(state.display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);
  if(state.context!=EGL_NO_CONTEXT)eglDestroyContext(state.display,state.context);
  if(state.surface!=EGL_NO_SURFACE)eglDestroySurface(state.display,state.surface);
 }
 state.context=EGL_NO_CONTEXT;state.surface=EGL_NO_SURFACE;return 0;
}
static int initialize(void){
 if(state.disabled)return 0;if(state.initialized)return 1;
 if(eglGetCurrentContext()!=EGL_NO_CONTEXT){state.disabled=1;return 0;}
 const EGLint configAttrs[]={EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RENDERABLE_TYPE,0x40,EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_ALPHA_SIZE,8,EGL_NONE};
 const EGLint surfaceAttrs[]={EGL_WIDTH,1,EGL_HEIGHT,1,EGL_NONE};
 const EGLint contextAttrs[]={EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE};
 EGLConfig config;EGLint count=0;
 state.display=eglGetDisplay(EGL_DEFAULT_DISPLAY);
 if(state.display==EGL_NO_DISPLAY || !eglInitialize(state.display,NULL,NULL) || !eglBindAPI(EGL_OPENGL_ES_API) || !eglChooseConfig(state.display,configAttrs,&config,1,&count)||count!=1)return initialization_failed();
 state.surface=eglCreatePbufferSurface(state.display,config,surfaceAttrs);
 state.context=eglCreateContext(state.display,config,EGL_NO_CONTEXT,contextAttrs);
 if(state.surface==EGL_NO_SURFACE || state.context==EGL_NO_CONTEXT || !eglMakeCurrent(state.display,state.surface,state.surface,state.context))return initialization_failed();
 GLint major=0,minor=0,blocks=0,invocations=0,local=0;
 glGetIntegerv(GL_MAJOR_VERSION,&major);glGetIntegerv(GL_MINOR_VERSION,&minor);
 glGetIntegerv(GL_MAX_COMPUTE_SHADER_STORAGE_BLOCKS,&blocks);glGetIntegerv(GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS,&state.maxBindings);
 glGetIntegerv(GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS,&invocations);glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_SIZE,0,&local);
 state.maxLocal=local;state.maxInvocations=invocations;glGetIntegerv(GL_MAX_COMPUTE_SHARED_MEMORY_SIZE,&state.maxShared);
 glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT,0,&state.maxGroups);glGetInteger64v(GL_MAX_SHADER_STORAGE_BLOCK_SIZE,&state.maxStorage);
 if(major<3||(major==3&&minor<1)||blocks<8||state.maxBindings<8||invocations<64||local<64||state.maxGroups<1||state.maxStorage<4||!clean_gl())return initialization_failed();
 const char *vendor=(const char*)glGetString(GL_VENDOR),*renderer=(const char*)glGetString(GL_RENDERER),*version=(const char*)glGetString(GL_VERSION),*glsl=(const char*)glGetString(GL_SHADING_LANGUAGE_VERSION);
 snprintf(state.environment,sizeof(state.environment),"GX1961:%s|%s|%s|%s|ssbo=%d|maxStorage=%lld|fp64=%d|source=%s",vendor?vendor:"",renderer?renderer:"",version?version:"",glsl?glsl:"",blocks,(long long)state.maxStorage,precision64(),GPU1960_SOURCE_SHA256);
 state.initialized=1;return 1;
}
static int program(int id){
 if(id<0||id>=PROGRAMS||!initialize())return 0;
 if(state.attempted[id])return state.programs[id]!=0;state.attempted[id]=1;
 GLuint shader=glCreateShader(GL_COMPUTE_SHADER),p=0;GLint ok=0;
 if(!shader)return 0;
 /* Existing0..26 program IDs retain their exact meaning. GX27 appends
  * mode0..3 at27+mode+choice*4; each compiles unchanged NR with its actual
  * half/full5, quarter7 or eighth3 pixel cache support. */
 int specialized=id>=STRONG_PROGRAM_BASE;
 int mode=specialized?(id-STRONG_PROGRAM_BASE)%STRONG_MODES:-1;
 int base=specialized?0:id%BASE_PROGRAMS;
 int variant=specialized?(id-STRONG_PROGRAM_BASE)/STRONG_MODES:id/BASE_PROGRAMS;
 int requested=variant==1?32:variant==2?128:64;
 if(base==0){int radius=mode==2?3:(mode==0||mode==3)?5:7;
  size_t shared;
  if(specialized){
   int rows=requested/8,diff_radius=mode==1?7:5,offsets=mode==1?12:4;
   shared=(size_t)(8+2*radius)*(size_t)(rows+2*radius)*16u;
   if(mode!=2)shared+=(size_t)offsets*(size_t)(8+diff_radius+1)*(size_t)(rows+diff_radius+1)*4u;
  }else shared=(size_t)(requested+2*radius)*(size_t)(2*radius+1)*(requested<=64?16u:8u);
  if(shared>(size_t)state.maxShared){glDeleteShader(shader);return 0;}}
 const char *original=sources[base],*newline=strchr(original,'\n');
 if(!newline){glDeleteShader(shader);return 0;}
 char define[160];snprintf(define,sizeof(define),"\n#define GX_LOCAL_SIZE %d\n#define GX_STRONG_MODE %d\n#define GX_TILE_WIDTH %d\n",requested,mode,specialized?8:0);
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
 if(!slot_valid(slot)||bytes==0||bytes>MAX_BYTES||(uint64_t)bytes>(uint64_t)state.maxStorage){note_failure1971(FAILURE_ALLOCATION1971,"SSBO allocation limit");return 0;}
 state.used[slot]=bytes;if(state.capacity[slot]>=bytes)return 1;
 size_t cap=(bytes+4095u)&~(size_t)4095u;if(cap>MAX_BYTES||(uint64_t)cap>(uint64_t)state.maxStorage)cap=bytes;
 if(state.total-state.capacity[slot]+state.stagingCapacity>MAX_BYTES-cap){note_failure1971(FAILURE_ALLOCATION1971,"SSBO allocation budget");return 0;}
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
JNIEXPORT jlong JNICALL JNI1960(openNative)(JNIEnv *env,jclass cls){(void)env;(void)cls;if(state.token||!initialize())return 0;state.failed=0;state.failure[0]=0;state.failureCode1971=0;state.poolRequested=0;memset(state.used,0,sizeof(state.used));state.token=++state.sequence;if(!state.token)state.token=++state.sequence;return (jlong)state.token;}
JNIEXPORT jboolean JNICALL JNI1960(supportedNative)(JNIEnv *env,jclass cls,jint id){(void)env;(void)cls;return program(id)?JNI_TRUE:JNI_FALSE;}
JNIEXPORT jint JNICALL JNI1960(workgroupNative)(JNIEnv *env,jclass cls,jint id){(void)env;(void)cls;return program(id)?state.localX[id]:0;}
JNIEXPORT jlong JNICALL JNI1960(retainedNative)(JNIEnv *env,jclass cls){(void)env;(void)cls;return (jlong)(state.total+state.stagingCapacity);}
JNIEXPORT jlongArray JNICALL JNI1960(capacityNative1971)(JNIEnv *env,jclass cls,jlong token){
 (void)cls;if(!state.initialized||token<=0||(uint64_t)token!=state.token)return NULL;
 jlong values[27];for(int i=0;i<SLOTS;i++)values[i]=(jlong)state.capacity[i];
 values[24]=(jlong)state.stagingCapacity;values[25]=(jlong)state.maxStorage;values[26]=(jlong)state.total;
 jlongArray result=(*env)->NewLongArray(env,27);if(result)(*env)->SetLongArrayRegion(env,result,0,27,values);return result;
}
JNIEXPORT jint JNICALL JNI1960(failureCodeNative1971)(JNIEnv *env,jclass cls){
 (void)env;(void)cls;return state.failureCode1971?state.failureCode1971:(state.failed||state.disabled?FAILURE_UNKNOWN1971:FAILURE_NONE1971);
}
JNIEXPORT jstring JNICALL JNI1960(environmentNative)(JNIEnv *env,jclass cls){(void)cls;return (*env)->NewStringUTF(env,state.environment);}
JNIEXPORT jboolean JNICALL JNI1960(allocateNative)(JNIEnv *env,jclass cls,jlong token,jint slot,jlong bytes){(void)env;(void)cls;if(!active(token)||bytes<1||(uint64_t)bytes>MAX_BYTES)return JNI_FALSE;return allocate(slot,(size_t)bytes)?JNI_TRUE:JNI_FALSE;}
static int upload(JNIEnv *env,jlong token,jint slot,jarray values,int floats){
 if(!active(token)||!slot_valid(slot)||!values)return 0;
 jsize n=(*env)->GetArrayLength(env,values);if(n<1||(uint64_t)n*4u>MAX_BYTES)return 0;
 size_t bytes=(size_t)n*4u;if(!allocate(slot,bytes))return 0;
 void *copy=state.staging;int temporary=0;
 if(bytes>STAGING_BYTES){if(state.total+state.stagingCapacity>MAX_BYTES-bytes){note_failure1971(FAILURE_ALLOCATION1971,"GPU temporary upload budget");return 0;}copy=malloc(bytes);temporary=1;}
 else if(state.stagingCapacity<bytes){
  size_t cap=(bytes+4095u)&~(size_t)4095u;
  if(state.total>MAX_BYTES-cap){note_failure1971(FAILURE_ALLOCATION1971,"GPU upload staging budget");return 0;}void *replacement=realloc(state.staging,cap);if(!replacement){note_failure1971(FAILURE_ALLOCATION1971,"GPU upload staging allocation");return 0;}
  state.staging=replacement;state.stagingCapacity=cap;copy=replacement;
 }
 if(!copy){note_failure1971(FAILURE_ALLOCATION1971,"GPU temporary upload allocation");return 0;}
 if(floats)(*env)->GetFloatArrayRegion(env,(jfloatArray)values,0,n,copy);else (*env)->GetIntArrayRegion(env,(jintArray)values,0,n,copy);
 if((*env)->ExceptionCheck(env)){if(temporary)free(copy);return 0;}
 glBindBuffer(GL_SHADER_STORAGE_BUFFER,state.buffers[slot]);glBufferSubData(GL_SHADER_STORAGE_BUFFER,0,(GLsizeiptr)bytes,copy);if(temporary)free(copy);state.submitted++;
 if(!clean_gl()){fail("GPU input upload error");return 0;}return 1;
}
JNIEXPORT jboolean JNICALL JNI1960(uploadIntsNative)(JNIEnv *e,jclass c,jlong token,jint slot,jintArray a){(void)c;return upload(e,token,slot,a,0)?JNI_TRUE:JNI_FALSE;}
JNIEXPORT jboolean JNICALL JNI1960(uploadFloatsNative)(JNIEnv *e,jclass c,jlong token,jint slot,jfloatArray a){(void)c;return upload(e,token,slot,a,1)?JNI_TRUE:JNI_FALSE;}
/* GX35: byte-identical native-order coefficient words in a Java-owned direct
 * buffer. GetDirectBufferAddress does not pin a heap array. glBufferSubData
 * finishes consuming client memory before return; a pending GPU ticket only
 * refers to its private SSBO. The Java Packet keeps the buffer alive for this
 * entire JNI call. Do not dereference an unvalidated or short buffer. */
static int upload_direct_buffer(JNIEnv *env,jlong token,jint slot,jobject values,jlong count){
 if(!active(token)||!slot_valid(slot)||!values||count<4||
    (count&3)!=0||(uint64_t)count>MAX_BYTES)return 0;
 jlong capacity=(*env)->GetDirectBufferCapacity(env,values);
 void *address=(*env)->GetDirectBufferAddress(env,values);
 if((*env)->ExceptionCheck(env)||capacity<count||!address)return 0;
 if(!allocate(slot,(size_t)count))return 0;
 glBindBuffer(GL_SHADER_STORAGE_BUFFER,state.buffers[slot]);
 glBufferSubData(GL_SHADER_STORAGE_BUFFER,0,(GLsizeiptr)count,address);
 state.submitted++;
 if(!clean_gl()){fail("GPU direct coefficient upload error");return 0;}return 1;
}
/* GX26: fill the mapped GPU input directly from the unchanged Java values.
 * Mapping is optional: an unavailable map retains the exact staged upload.
 * A failed unmap invalidates this private candidate; no output is committed.
 * No Java array is pinned across a driver call. */
static int upload_mapped(JNIEnv *env,jlong token,jint slot,jarray values,int floats){
 if(!active(token)||!slot_valid(slot)||!values)return 0;
 jsize n=(*env)->GetArrayLength(env,values);if(n<1||(uint64_t)n*4u>MAX_BYTES)return 0;
 size_t bytes=(size_t)n*4u;if(!allocate(slot,bytes))return 0;
 glBindBuffer(GL_SHADER_STORAGE_BUFFER,state.buffers[slot]);
 void *mapped=glMapBufferRange(GL_SHADER_STORAGE_BUFFER,0,(GLsizeiptr)bytes,GL_MAP_WRITE_BIT|GL_MAP_INVALIDATE_BUFFER_BIT);
 GLenum error=glGetError();
 if(!mapped){
  if(error==GL_CONTEXT_LOST){fail("GPU mapped input lost context");state.disabled=1;return 0;}
  return upload(env,token,slot,values,floats);
 }
 if(error!=GL_NO_ERROR){glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);fail("GPU mapped input error");return 0;}
 if(floats)(*env)->GetFloatArrayRegion(env,(jfloatArray)values,0,n,mapped);
 else (*env)->GetIntArrayRegion(env,(jintArray)values,0,n,mapped);
 int java_failed=(*env)->ExceptionCheck(env);
 GLboolean valid=glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);error=glGetError();
 if(java_failed||!valid||error!=GL_NO_ERROR){fail("GPU mapped input transaction failure");return 0;}
 state.submitted++;return 1;
}
JNIEXPORT jboolean JNICALL JNI1960(dispatchNative)(JNIEnv *env,jclass cls,jlong token,jint id,jintArray bindings,jintArray ints,jfloatArray floats,jint invocations){
 (void)cls;if(!active(token)||invocations<1||(uint64_t)invocations>MAX_BYTES/4u||!bindings||!ints||!program(id))return JNI_FALSE;
 jsize nb=(*env)->GetArrayLength(env,bindings),ni=(*env)->GetArrayLength(env,ints),nf=floats?(*env)->GetArrayLength(env,floats):0;
 if(nb<1||nb>8||ni>32||ni<1||nf>32)return JNI_FALSE;
 jint slots[8],u[32]={0};jfloat f[32]={0};
 (*env)->GetIntArrayRegion(env,bindings,0,nb,slots);(*env)->GetIntArrayRegion(env,ints,0,ni,u);if(nf)(*env)->GetFloatArrayRegion(env,floats,0,nf,f);
 if((*env)->ExceptionCheck(env))return JNI_FALSE;
 if(id>=STRONG_PROGRAM_BASE&&u[10]!=(id-STRONG_PROGRAM_BASE)%4)return JNI_FALSE;
 for(int b=0;b<nb;b++){if(!slot_valid(slots[b])||!state.buffers[slots[b]]||!state.used[slots[b]])return JNI_FALSE;glBindBufferBase(GL_SHADER_STORAGE_BUFFER,(GLuint)b,state.buffers[slots[b]]);}
 glUseProgram(state.programs[id]);if(state.fLocation[id]>=0&&state.fCount[id])glUniform1fv(state.fLocation[id],state.fCount[id],f);
 uint64_t local=(uint64_t)state.localX[id];
 if(id>=STRONG_PROGRAM_BASE){
  /* GX33 tile8 topology pads workgroups, never image resolution. u31 remains
   * a lane offset, hence split launches use groupStart*local exactly. */
  int64_t rows=(int64_t)u[3]-u[2];
  if(u[0]<1||rows<1||(uint64_t)u[0]*(uint64_t)rows!=(uint64_t)invocations||local<8u||local%8u)return JNI_FALSE;
  uint64_t cols=((uint64_t)u[0]+7u)/8u,tile_rows=local/8u;
  uint64_t groups=cols*(((uint64_t)rows+tile_rows-1u)/tile_rows);
  if(groups>INT_MAX/local)return JNI_FALSE;
  uint64_t first=0;
  while(first<groups){uint64_t count=groups-first;if(count>(uint64_t)state.maxGroups)count=(uint64_t)state.maxGroups;
   u[31]=(jint)(first*local);glUniform1iv(state.uLocation[id],32,u);glDispatchCompute((GLuint)count,1,1);
   glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);if(!clean_gl()){fail("GPU tile compute dispatch error");return JNI_FALSE;}state.submitted++;first+=count;}
  return JNI_TRUE;
 }
 uint64_t maximum=(uint64_t)state.maxGroups*local;uint64_t base=0,total=(uint64_t)invocations;
 while(base<total){uint64_t count=total-base;if(count>maximum)count=maximum;u[31]=(jint)base;glUniform1iv(state.uLocation[id],32,u);glDispatchCompute((GLuint)((count+local-1u)/local),1,1);glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);if(!clean_gl()){fail("GPU compute dispatch error");return JNI_FALSE;}state.submitted++;base+=count;}
 return JNI_TRUE;
}
static jintArray read_one(JNIEnv *env,int slot,int count){
 if(!slot_valid(slot)||count<1||(uint64_t)count*4u>state.used[slot])return NULL;
 glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT);glBindBuffer(GL_SHADER_STORAGE_BUFFER,state.buffers[slot]);
 size_t bytes=(size_t)count*4u;void *mapped=glMapBufferRange(GL_SHADER_STORAGE_BUFFER,0,(GLsizeiptr)bytes,GL_MAP_READ_BIT);
 GLenum error=glGetError();
 if(!mapped||error!=GL_NO_ERROR){if(mapped)glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);fail("GPU readback map failure");return NULL;}
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
/* GX26 target is a caller-owned private candidate. The caller must discard it
 * on false, including an unmap failure after SetIntArrayRegion. */
JNIEXPORT jboolean JNICALL JNI1960(readIntoNative)(JNIEnv *env,jclass cls,jlong token,jlong ticket,jint slot,jint count,jintArray target,jint offset){
 (void)cls;
 if(!active(token)||!slot_valid(slot)||count<1||!target||offset<0||
    (uint64_t)count*4u>state.used[slot])return JNI_FALSE;
 jsize size=(*env)->GetArrayLength(env,target);if(offset>size||count>size-offset)return JNI_FALSE;
 if(ticket>0?!wait_ticket((uint64_t)ticket):!drain())return JNI_FALSE;
 glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT);glBindBuffer(GL_SHADER_STORAGE_BUFFER,state.buffers[slot]);
 void *mapped=glMapBufferRange(GL_SHADER_STORAGE_BUFFER,0,(GLsizeiptr)((size_t)count*4u),GL_MAP_READ_BIT);
 GLenum error=glGetError();
 if(!mapped||error!=GL_NO_ERROR){if(mapped)glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);fail("GPU direct readback map failure");return JNI_FALSE;}
 (*env)->SetIntArrayRegion(env,target,offset,count,(const jint*)mapped);
 int java_failed=(*env)->ExceptionCheck(env);
 GLboolean valid=glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);
 if(java_failed||!valid||!clean_gl()){fail("GPU direct readback transaction failure");return JNI_FALSE;}
 return JNI_TRUE;
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
  else if(kinds[i]==6){jobject payload=(*env)->GetObjectArrayElement(env,payloadArray,i);ok=upload_direct_buffer(env,token,slots[i],payload,bytes[i]);if(payload)(*env)->DeleteLocalRef(env,payload);}
  else if(kinds[i]==1||kinds[i]==2||kinds[i]==4||kinds[i]==5){jobject payload=(*env)->GetObjectArrayElement(env,payloadArray,i);ok=kinds[i]>=4?upload_mapped(env,token,slots[i],(jarray)payload,kinds[i]==5):upload(env,token,slots[i],(jarray)payload,kinds[i]==2);if(payload)(*env)->DeleteLocalRef(env,payload);}
  else if(kinds[i]==3){jintArray bindings=(*env)->GetObjectArrayElement(env,bindingArray,i),u=(*env)->GetObjectArrayElement(env,uniformArray,i);jfloatArray f=(*env)->GetObjectArrayElement(env,floatArray,i);ok=JNI1960(dispatchNative)(env,NULL,token,shaders[i],bindings,u,f,counts[i]);if(bindings)(*env)->DeleteLocalRef(env,bindings);if(u)(*env)->DeleteLocalRef(env,u);if(f)(*env)->DeleteLocalRef(env,f);}
  if(!ok||(*env)->ExceptionCheck(env)){
   int code=kinds[i]==0?FAILURE_ALLOCATION1971:(kinds[i]==1||kinds[i]==2||kinds[i]==4||kinds[i]==5||kinds[i]==6)?FAILURE_UPLOAD1971:kinds[i]==3?FAILURE_DISPATCH1971:FAILURE_UNKNOWN1971;
   fail_code1971(code,"GPU batch command failed");return 0;
  }
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
