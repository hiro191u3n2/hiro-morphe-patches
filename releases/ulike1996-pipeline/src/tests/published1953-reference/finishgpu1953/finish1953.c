/* H16/H18/H19 exact photo sessions. JNI entry points except ABI execute only on
 * the private Java owner thread. Its independent EGL context remains bound there.
 * Two bounded slots isolate submitted GPU work from subsequent CPU preparation;
 * source overlap is copied ONLY within this photo's unique token/coordinates. */
#include <jni.h>
#include <EGL/egl.h>
#include <GLES3/gl31.h>
#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <limits.h>
#include "finish_source1953.h"
#define JNI_FN(n) Java_com_hiro_ulike_GpuFinish1953_##n
#define MAX_BYTES (64u*1024u*1024u)
#define MAX_PHOTO_GPU_BYTES (128u*1024u*1024u)
#define TILE_ROWS 32
#define FENCE_NS 3000000000ULL
#define PHOTO_POOL 2

typedef struct {
 GLuint buffers[4];size_t capacities[4];GLsync fence;
 int pending,unsafe,first,last,input_origin,input_rows,source_valid;
 uint64_t source_token;
} Slot;
typedef struct {
 uint64_t token;int active,quarantined,width,height,core,reach;
 int moire,sharp,gain,floor,limit,texture,halo,sequential;
 int previous_slot,next_first;Slot slots[2];size_t planned[4];
 uint64_t submits,collects,uploaded_pixels,copied_pixels,policy_words,fence_waits;
 uint64_t peak_inflight,overlap_submits,unsignaled_submits,forced_drains;
} Photo;
typedef struct {
 EGLDisplay display;EGLContext context;EGLSurface surface;
 GLuint program;GLint uniforms[16],max_groups[3];GLint64 max_storage;
 void *workspace[3];size_t workspace_capacity[3];
 pthread_t owner;int owner_valid,initialized,disabled,context_binds;
 uint64_t sequence;Photo photos[PHOTO_POOL];char environment[4096];
} State;
static State gpu;
static int clean(void){return glGetError()==GL_NO_ERROR;}
static int owns_context(void){return gpu.owner_valid && pthread_equal(gpu.owner,pthread_self()) &&
 gpu.initialized && eglGetCurrentContext()==gpu.context;}
static int initialize(void){
 if(gpu.disabled)return 0;
 if(gpu.initialized)return owns_context();
 if(gpu.owner_valid && !pthread_equal(gpu.owner,pthread_self()))return 0;
 /* Owner is a newly created private worker, never the camera/preview thread. */
 if(eglGetCurrentContext()!=EGL_NO_CONTEXT)return 0;
 gpu.owner=pthread_self();gpu.owner_valid=1;
 EGLenum previous=eglQueryAPI();
 if(!eglBindAPI(EGL_OPENGL_ES_API))goto fail;
 const EGLint config[]={EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RENDERABLE_TYPE,0x40,
  EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_ALPHA_SIZE,8,EGL_NONE};
 const EGLint surface[]={EGL_WIDTH,1,EGL_HEIGHT,1,EGL_NONE};
 const EGLint context[]={EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE};
 EGLConfig selected=0;EGLint count=0;
 gpu.display=eglGetDisplay(EGL_DEFAULT_DISPLAY);
 if(gpu.display==EGL_NO_DISPLAY || !eglInitialize(gpu.display,0,0) ||
  !eglChooseConfig(gpu.display,config,&selected,1,&count) || count!=1)goto fail;
 gpu.surface=eglCreatePbufferSurface(gpu.display,selected,surface);
 gpu.context=eglCreateContext(gpu.display,selected,EGL_NO_CONTEXT,context);
 if(gpu.surface==EGL_NO_SURFACE || gpu.context==EGL_NO_CONTEXT ||
  !eglMakeCurrent(gpu.display,gpu.surface,gpu.surface,gpu.context))goto fail;
 gpu.context_binds++;
 GLint major=0,minor=0,blocks=0,bindings=0,invocations=0,sizes[3]={0};
 glGetIntegerv(GL_MAJOR_VERSION,&major);glGetIntegerv(GL_MINOR_VERSION,&minor);
 glGetIntegerv(GL_MAX_COMPUTE_SHADER_STORAGE_BLOCKS,&blocks);
 glGetIntegerv(GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS,&bindings);
 glGetIntegerv(GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS,&invocations);
 glGetInteger64v(GL_MAX_SHADER_STORAGE_BLOCK_SIZE,&gpu.max_storage);
 for(int i=0;i<3;i++){
  glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT,(GLuint)i,&gpu.max_groups[i]);
  glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_SIZE,(GLuint)i,&sizes[i]);
 }
 if(major<3 || (major==3&&minor<1) || blocks<4 || bindings<4 || invocations<64 ||
  sizes[0]<8 || sizes[1]<8 || sizes[2]<1 || gpu.max_storage<4 ||
  gpu.max_groups[0]<1 || gpu.max_groups[1]<1)goto fail;
 GLuint shader=glCreateShader(GL_COMPUTE_SHADER);
 const GLchar *source=finish1953_source;
 if(!shader)goto fail;
 glShaderSource(shader,1,&source,0);glCompileShader(shader);
 GLint ok=0;glGetShaderiv(shader,GL_COMPILE_STATUS,&ok);
 if(!ok){glDeleteShader(shader);goto fail;}
 gpu.program=glCreateProgram();glAttachShader(gpu.program,shader);
 glLinkProgram(gpu.program);glDeleteShader(shader);
 glGetProgramiv(gpu.program,GL_LINK_STATUS,&ok);if(!ok)goto fail;
 const char *names[]={"uWidth","uRows","uOrigin","uBegin","uEnd","uOutputOrigin",
  "uPolicyOrigin","uMoire","uSharp","uGain","uFloor","uLimit","uTexture","uHalo",
  "uSequential","uPolicyMode"};
 for(int i=0;i<16;i++)if((gpu.uniforms[i]=glGetUniformLocation(gpu.program,names[i]))<0)goto fail;
 if(!clean())goto fail;
 const char *vendor=(const char*)glGetString(GL_VENDOR),*renderer=(const char*)glGetString(GL_RENDERER),
  *version=(const char*)glGetString(GL_VERSION),*glsl=(const char*)glGetString(GL_SHADING_LANGUAGE_VERSION);
 if(!vendor || !renderer || !version || !glsl || !clean())goto fail;
 int length=snprintf(gpu.environment,sizeof(gpu.environment),
  "abi=19531;source=" FINISH1953_SOURCE_ID ";vendor=%s;renderer=%s;gl=%s;glsl=%s;storage=%lld;groups=%d,%d,%d;slots=2;tiles=32",
  vendor,renderer,version,glsl,(long long)gpu.max_storage,gpu.max_groups[0],gpu.max_groups[1],gpu.max_groups[2]);
 if(length<1 || (size_t)length>=sizeof(gpu.environment))goto fail;
 gpu.initialized=1;return 1;
fail:
 gpu.disabled=1;gpu.environment[0]=0;
 if(gpu.display!=EGL_NO_DISPLAY && eglGetCurrentContext()==gpu.context)
  (void)eglMakeCurrent(gpu.display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);
 if(previous!=EGL_NONE)(void)eglBindAPI(previous);
 /* Never eglTerminate: the EGL display may also serve the camera renderer. */
 return 0;
}
static Photo *photo(jlong token){
 if(!owns_context() || token<=0)return 0;
 for(int i=0;i<PHOTO_POOL;i++)if(gpu.photos[i].active && gpu.photos[i].token==(uint64_t)token)return &gpu.photos[i];
 return 0;
}
static void *workspace(int slot,size_t bytes){
 if(!bytes || bytes>MAX_BYTES)return 0;
 if(gpu.workspace_capacity[slot]<bytes){
  void *next=realloc(gpu.workspace[slot],bytes);if(!next)return 0;
  gpu.workspace[slot]=next;gpu.workspace_capacity[slot]=bytes;
 }
 return gpu.workspace[slot];
}
static int buffer(Slot *slot,int at,size_t bytes,const void *data){
 if(!bytes || bytes>MAX_BYTES || (uint64_t)bytes>(uint64_t)gpu.max_storage)return 0;
 glBindBuffer(GL_SHADER_STORAGE_BUFFER,slot->buffers[at]);
 if(slot->capacities[at]<bytes){
  glBufferData(GL_SHADER_STORAGE_BUFFER,(GLsizeiptr)bytes,0,GL_DYNAMIC_COPY);
  if(!clean())return 0;
  slot->capacities[at]=bytes;
 }
 if(data)glBufferSubData(GL_SHADER_STORAGE_BUFFER,0,(GLsizeiptr)bytes,data);
 glBindBufferBase(GL_SHADER_STORAGE_BUFFER,(GLuint)at,slot->buffers[at]);return clean();
}
static int allowed_core(int width,int height,int reach,int sequential,int sharp,int core){
 int count=core<height?core:height;
 uint64_t input=(uint64_t)width*(uint64_t)(count+reach*2)*4u;
 uint64_t middle=(uint64_t)width*(uint64_t)(count+(sequential&&sharp?8:0))*4u;
 uint64_t output=(uint64_t)width*(uint64_t)count*4u,policy=sharp?output*4u:4u;
 uint64_t maximum=input>middle?input:middle;if(output>maximum)maximum=output;if(policy>maximum)maximum=policy;
 return maximum<=MAX_BYTES && maximum<=(uint64_t)gpu.max_storage &&
  (input+middle+output+policy)*2u<=MAX_PHOTO_GPU_BYTES &&
  (width+7)/8<=gpu.max_groups[0] && (count+7)/8<=gpu.max_groups[1];
}
/* Explicit retained buffers + malloc workspaces. Before growth, every active
 * photo reserves the worst RAW4 shape, including both slots; global staging is
 * reserved to the largest active shape. This is published to Java lock-free. */
static uint64_t retained_bytes(void){
 uint64_t bytes=0,stage[3]={gpu.workspace_capacity[0],gpu.workspace_capacity[1],gpu.workspace_capacity[2]};
 for(int photo_index=0;photo_index<PHOTO_POOL;photo_index++){
  Photo *p=&gpu.photos[photo_index];
  for(int i=0;i<2;i++)for(int b=0;b<4;b++){
   uint64_t actual=p->slots[i].capacities[b];
   uint64_t plan=p->active?p->planned[b]:0;
   bytes+=actual>plan?actual:plan;
  }
  if(p->active){
   uint64_t required[3]={p->planned[0],p->planned[3],p->planned[2]};
   for(int i=0;i<3;i++)if(required[i]>stage[i])stage[i]=required[i];
  }
 }
 for(int i=0;i<3;i++)bytes+=stage[i];
 return bytes;
}
static uint64_t plan_photo(Photo *p,int width,int height,int reach,int sequential,int sharp,int core){
 int count=core<height?core:height;
 int input_rows=count+reach*2;if(input_rows>height)input_rows=height;
 int middle_rows=count+(sequential&&sharp?8:0);if(middle_rows>height)middle_rows=height;
 p->planned[0]=(size_t)width*input_rows*4u;p->planned[1]=(size_t)width*middle_rows*4u;
 p->planned[2]=(size_t)width*count*4u;p->planned[3]=sharp?p->planned[2]*4u:4u;
 uint64_t reserved=0;for(int i=0;i<2;i++)for(int b=0;b<4;b++){
  uint64_t actual=p->slots[i].capacities[b],plan=p->planned[b];reserved+=actual>plan?actual:plan;
 }
 return reserved;
}
static int dispatch(Photo *p,Slot *slot,int origin,int first,int last,int out_origin,int policy_origin,
 int moire,int sharp,int corrected_center,int policy_mode,GLuint source,GLuint middle,GLuint output){
 int gx=(p->width+7)/8,gy=(last-first+7)/8;
 if(gx<1 || gy<1 || gx>gpu.max_groups[0] || gy>gpu.max_groups[1])return 0;
 int values[16]={p->width,p->height,origin,first,last,out_origin,policy_origin,moire,sharp,
  p->gain,p->floor,p->limit,p->texture,p->halo,corrected_center,policy_mode};
 glUseProgram(gpu.program);for(int i=0;i<16;i++)glUniform1i(gpu.uniforms[i],values[i]);
 glBindBufferBase(GL_SHADER_STORAGE_BUFFER,0,source);
 glBindBufferBase(GL_SHADER_STORAGE_BUFFER,1,middle);
 glBindBufferBase(GL_SHADER_STORAGE_BUFFER,2,output);
 glBindBufferBase(GL_SHADER_STORAGE_BUFFER,3,slot->buffers[3]);
 glDispatchCompute((GLuint)gx,(GLuint)gy,1);return clean();
}
static int compute(Photo *p,Slot *s,int policy_mode){
 int first=s->first,last=s->last,outer_first=p->sequential&&p->sharp?(first>4?first-4:0):first;
 int outer_last=p->sequential&&p->sharp?(last<p->height-4?last+4:p->height):last;
 size_t middle=(size_t)p->width*(outer_last-outer_first)*4u,output=(size_t)p->width*(last-first)*4u;
 if(!buffer(s,1,middle,0) || !buffer(s,2,output,0))return 0;
 if(p->moire && p->sharp){
  int completed=outer_first;
  for(int start=first;start<last;start+=TILE_ROWS){
   int end=start+TILE_ROWS;if(end>last)end=last;
   int needed=p->sequential?(end<p->height-4?end+4:p->height):end;
   if(needed>completed && !dispatch(p,s,s->input_origin,completed,needed,outer_first,first,
    1,0,0,policy_mode,s->buffers[0],s->buffers[0],s->buffers[1]))return 0;
   completed=needed;glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
   if(!dispatch(p,s,p->sequential?outer_first:s->input_origin,start,end,first,first,0,1,
    !p->sequential,policy_mode,p->sequential?s->buffers[1]:s->buffers[0],s->buffers[1],s->buffers[2]))return 0;
  }
 }else for(int start=first;start<last;start+=TILE_ROWS){
  int end=start+TILE_ROWS;if(end>last)end=last;
  if(!dispatch(p,s,s->input_origin,start,end,first,first,p->moire,p->sharp,0,policy_mode,
   s->buffers[0],s->buffers[0],s->buffers[2]))return 0;
 }
 glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT|GL_BUFFER_UPDATE_BARRIER_BIT);
 s->fence=glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE,0);
 if(!s->fence || !clean())return 0;
 glFlush();
 /* Nonblocking status query only, instrumentation; no submit completion wait. */
 GLenum ready=glClientWaitSync(s->fence,0,0);
 if(ready==GL_TIMEOUT_EXPIRED)p->unsignaled_submits++;
 else if(ready!=GL_ALREADY_SIGNALED && ready!=GL_CONDITION_SATISFIED)return 0;
 return clean();
}
static int wait_slot(Photo *p,Slot *s){
 if(!s->pending)return !s->unsafe;
 if(!s->fence)return 0;
 p->fence_waits++;
 GLenum status=glClientWaitSync(s->fence,GL_SYNC_FLUSH_COMMANDS_BIT,FENCE_NS);
 if((status!=GL_ALREADY_SIGNALED && status!=GL_CONDITION_SATISFIED) || !clean())return 0;
 glDeleteSync(s->fence);s->fence=0;s->pending=0;return clean();
}
static jstring stats(JNIEnv *env,const Photo *p,int drained){
 char data[1024];
 int n=snprintf(data,sizeof(data),"{\"token\":%llu,\"core\":%d,\"halo\":%d,\"submissions\":%llu,\"collections\":%llu,"
  "\"source_uploaded_pixels\":%llu,\"halo_copied_pixels\":%llu,\"policy_words\":%llu,\"fence_waits\":%llu,"
  "\"submit_completion_waits\":0,\"peak_inflight\":%llu,\"submits_before_collect\":%llu,"
  "\"unsignaled_submissions\":%llu,\"forced_drains\":%llu,\"context_binds\":%d,"
  "\"cross_photo_halo_reuses\":0,\"close_drained\":%s}",
  (unsigned long long)p->token,p->core,p->reach,(unsigned long long)p->submits,(unsigned long long)p->collects,
  (unsigned long long)p->uploaded_pixels,(unsigned long long)p->copied_pixels,(unsigned long long)p->policy_words,
  (unsigned long long)p->fence_waits,(unsigned long long)p->peak_inflight,(unsigned long long)p->overlap_submits,
  (unsigned long long)p->unsignaled_submits,(unsigned long long)p->forced_drains,gpu.context_binds,drained?"true":"false");
 if(n<1 || (size_t)n>=sizeof(data))return 0;
 return (*env)->NewStringUTF(env,data);
}
JNIEXPORT jint JNICALL JNI_FN(nativeAbi)(JNIEnv *env,jclass cls){(void)env;(void)cls;return 19531;}
JNIEXPORT jlong JNICALL JNI_FN(retainedNative)(JNIEnv *env,jclass cls){
 (void)env;(void)cls;
 if(gpu.owner_valid && !pthread_equal(gpu.owner,pthread_self()))return 0;
 uint64_t bytes=retained_bytes();return bytes>INT64_MAX?INT64_MAX:(jlong)bytes;
}
JNIEXPORT jboolean JNICALL JNI_FN(warmupNative)(JNIEnv *env,jclass cls){(void)env;(void)cls;return initialize()?JNI_TRUE:JNI_FALSE;}
JNIEXPORT jstring JNICALL JNI_FN(environmentNative)(JNIEnv *env,jclass cls){
 (void)cls;return (*env)->NewStringUTF(env,owns_context()&&!gpu.disabled?gpu.environment:"");
}
JNIEXPORT jlong JNICALL JNI_FN(openNative)(JNIEnv *env,jclass cls,jint width,jint height,
 jboolean moire,jboolean sharp,jint gain,jint floor,jint limit,jboolean texture,jboolean halo,
 jboolean sequential,jint preferred_core){
 (void)env;(void)cls;
 if(width<1 || height<1 || width>16384 || (int64_t)width*height>INT_MAX ||
  (preferred_core!=256 && preferred_core!=512) || gain<0 || floor<0 || limit<0 || limit>255 ||
  !initialize())return 0;
 int reach=moire?(sequential&&sharp?36:32):(sharp?4:0);
 int core=preferred_core;
 if(!allowed_core(width,height,reach,sequential,sharp,core)){core=256;if(!allowed_core(width,height,reach,sequential,sharp,core))return 0;}
 if(core>height)core=height;
 Photo *p=0;for(int i=0;i<PHOTO_POOL;i++)if(!gpu.photos[i].active && !gpu.photos[i].quarantined){p=&gpu.photos[i];break;}
 if(!p || gpu.sequence>=INT64_MAX)return 0;
 if(plan_photo(p,width,height,reach,sequential,sharp,core)>MAX_PHOTO_GPU_BYTES){
  core=height<256?height:256;
  if(!allowed_core(width,height,reach,sequential,sharp,core) ||
   plan_photo(p,width,height,reach,sequential,sharp,core)>MAX_PHOTO_GPU_BYTES)return 0;
 }
 /* Capacities may survive a successful close; all resident-image ownership and
  * coordinates are reset before assigning the new unique photo token. */
 p->token=++gpu.sequence;p->active=1;p->width=width;p->height=height;p->core=core;p->reach=reach;
 p->moire=moire;p->sharp=sharp;p->gain=gain;p->floor=floor;p->limit=limit;p->texture=texture;
 p->halo=halo;p->sequential=sequential;p->previous_slot=-1;p->next_first=-1;
 p->submits=p->collects=p->uploaded_pixels=p->copied_pixels=p->policy_words=p->fence_waits=0;
 p->peak_inflight=p->overlap_submits=p->unsignaled_submits=p->forced_drains=0;
 for(int i=0;i<2;i++){
  Slot *s=&p->slots[i];s->pending=s->unsafe=s->source_valid=0;s->source_token=0;s->fence=0;
  if(!s->buffers[0])glGenBuffers(4,s->buffers);
 }
 if(!clean()){p->quarantined=1;p->active=0;gpu.disabled=1;return 0;}
 return (jlong)p->token;
}
JNIEXPORT jint JNICALL JNI_FN(coreNative)(JNIEnv *env,jclass cls,jlong token){(void)env;(void)cls;Photo *p=photo(token);return p?p->core:0;}
JNIEXPORT jint JNICALL JNI_FN(haloNative)(JNIEnv *env,jclass cls,jlong token){(void)env;(void)cls;Photo *p=photo(token);return p?p->reach:-1;}
JNIEXPORT jboolean JNICALL JNI_FN(submitNative)(JNIEnv *env,jclass cls,jlong token,jint index,
 jintArray input,jint origin,jint input_rows,jint first,jint last,jintArray policy,jint mode){
 (void)cls;Photo *p=photo(token);
 if(!p || gpu.disabled || index<0 || index>1 || !input || !policy || (*env)->IsSameObject(env,input,policy) ||
  first<0 || last<=first || last>p->height || last-first>p->core ||
  origin!=(first>p->reach?first-p->reach:0) ||
  input_rows!=(last<p->height-p->reach?last+p->reach:p->height)-origin ||
  (p->next_first>=0 && first!=p->next_first) || mode<0 || mode>2)return JNI_FALSE;
 Slot *s=&p->slots[index];if(s->pending || s->unsafe)return JNI_FALSE;
 int64_t n=(int64_t)p->width*(last-first),all=(int64_t)p->width*input_rows;
 int64_t words=mode==2?4:n*(mode==1?2:4);
 if(all>(*env)->GetArrayLength(env,input) || words>(*env)->GetArrayLength(env,policy) || words>MAX_BYTES/4)return JNI_FALSE;
 size_t policy_bytes=(size_t)words*4u;
 int32_t *settings=workspace(1,policy_bytes);if(!settings)return JNI_FALSE;
 (*env)->GetIntArrayRegion(env,policy,0,(jsize)words,(jint*)settings);
 if((*env)->ExceptionCheck(env))return JNI_FALSE;
 int overlap_first=origin,overlap_last=origin;
 Slot *previous=p->previous_slot>=0?&p->slots[p->previous_slot]:0;
 if(previous && previous!=s && previous->source_valid && previous->source_token==p->token){
  int prior_end=previous->input_origin+previous->input_rows,new_end=origin+input_rows;
  overlap_first=origin>previous->input_origin?origin:previous->input_origin;
  overlap_last=new_end<prior_end?new_end:prior_end;
  if(overlap_last<overlap_first)overlap_last=overlap_first;
 }
 /* Caller bands are monotonic; cached overlap must start at the new origin.
  * Unsupported geometry simply uploads the complete immutable source band. */
 if(overlap_first!=origin){overlap_first=overlap_last=origin;previous=0;}
 int copied_rows=overlap_last-overlap_first,fresh_rows=input_rows-copied_rows;
 size_t fresh_bytes=(size_t)p->width*fresh_rows*4u;
 int32_t *source=fresh_bytes?workspace(0,fresh_bytes):0;
 if(fresh_bytes && !source)return JNI_FALSE;
 if(fresh_rows)(*env)->GetIntArrayRegion(env,input,copied_rows*p->width,fresh_rows*p->width,(jint*)source);
 if((*env)->ExceptionCheck(env))return JNI_FALSE;
 s->first=first;s->last=last;s->input_origin=origin;s->input_rows=input_rows;
 size_t input_bytes=(size_t)all*4u;
 if(!buffer(s,0,input_bytes,0) || !buffer(s,3,policy_bytes,settings))goto failure;
 if(copied_rows){
  glBindBuffer(GL_COPY_READ_BUFFER,previous->buffers[0]);glBindBuffer(GL_COPY_WRITE_BUFFER,s->buffers[0]);
  glCopyBufferSubData(GL_COPY_READ_BUFFER,GL_COPY_WRITE_BUFFER,
   (GLintptr)((size_t)(origin-previous->input_origin)*p->width*4u),0,
   (GLsizeiptr)((size_t)copied_rows*p->width*4u));
 }
 if(fresh_rows){
  glBindBuffer(GL_SHADER_STORAGE_BUFFER,s->buffers[0]);
  glBufferSubData(GL_SHADER_STORAGE_BUFFER,(GLintptr)((size_t)copied_rows*p->width*4u),
   (GLsizeiptr)fresh_bytes,source);
 }
 glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT|GL_BUFFER_UPDATE_BARRIER_BIT);
 if(!clean() || !compute(p,s,mode))goto failure;
 s->pending=1;s->source_valid=1;s->source_token=p->token;p->previous_slot=index;p->next_first=last;
 p->submits++;p->uploaded_pixels+=(uint64_t)p->width*fresh_rows;p->copied_pixels+=(uint64_t)p->width*copied_rows;
 p->policy_words+=(uint64_t)words;
 int flight=p->slots[0].pending+p->slots[1].pending;
 if((uint64_t)flight>p->peak_inflight)p->peak_inflight=(uint64_t)flight;
 if(flight==2)p->overlap_submits++;
 return JNI_TRUE;
failure:
 s->unsafe=1;s->pending=s->fence!=0;p->quarantined=1;gpu.disabled=1;return JNI_FALSE;
}
JNIEXPORT jboolean JNICALL JNI_FN(collectNative)(JNIEnv *env,jclass cls,jlong token,jint index,
 jintArray output,jint offset){
 (void)cls;Photo *p=photo(token);
 if(!p || gpu.disabled || index<0 || index>1 || !output || offset<0)return JNI_FALSE;
 Slot *s=&p->slots[index];int64_t n=(int64_t)p->width*(s->last-s->first);
 if(!s->pending || n<1 || (int64_t)offset+n>(*env)->GetArrayLength(env,output))return JNI_FALSE;
 size_t bytes=(size_t)n*4u;
 int32_t *result=workspace(2,bytes);if(!result)return JNI_FALSE;
 if(!wait_slot(p,s))goto failure;
 glBindBuffer(GL_SHADER_STORAGE_BUFFER,s->buffers[2]);
 void *mapped=glMapBufferRange(GL_SHADER_STORAGE_BUFFER,0,(GLsizeiptr)bytes,GL_MAP_READ_BIT);
 if(!mapped || !clean())goto failure;
 memcpy(result,mapped,bytes);
 if(glUnmapBuffer(GL_SHADER_STORAGE_BUFFER)!=GL_TRUE || !clean())goto failure;
 (*env)->SetIntArrayRegion(env,output,offset,(jsize)n,(jint*)result);
 if((*env)->ExceptionCheck(env))goto failure;
 p->collects++;return JNI_TRUE;
failure:
 s->unsafe=1;p->quarantined=1;gpu.disabled=1;return JNI_FALSE;
}
JNIEXPORT jstring JNICALL JNI_FN(statsNative)(JNIEnv *env,jclass cls,jlong token){(void)cls;Photo *p=photo(token);return p?stats(env,p,0):(*env)->NewStringUTF(env,"");}
JNIEXPORT jstring JNICALL JNI_FN(closeNative)(JNIEnv *env,jclass cls,jlong token){
 (void)cls;Photo *p=photo(token);if(!p)return (*env)->NewStringUTF(env,"");
 int drained=1;
 for(int i=0;i<2;i++){
  Slot *s=&p->slots[i];
  if(s->pending){p->forced_drains++;if(!wait_slot(p,s))drained=0;}
  if(s->unsafe)drained=0;
  s->source_valid=0;s->source_token=0;
 }
 if(!drained){p->quarantined=1;gpu.disabled=1;}
 jstring report=stats(env,p,drained);
 /* Reset only after attempted bounded drain. Unknown completion permanently
  * quarantines its buffers; they are never handed to another photographed image. */
 p->previous_slot=-1;p->next_first=-1;p->active=0;return report;
}
