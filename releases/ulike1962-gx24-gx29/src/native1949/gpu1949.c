/* Independent exact-integer GLES3.1 backend. No existing camera EGL context,
 * float fusion, JNI critical section or destination pixel array is involved. */
#include <jni.h>
#include <EGL/egl.h>
#include <GLES3/gl31.h>
#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include "shader_sources1949.h"

#define JNINAME(name) Java_com_hiro_ulike_GpuInteger1949_##name
#define MAX_BYTES (64u*1024u*1024u)
#define FENCE_NS 200000000ULL

typedef struct {
    EGLDisplay display;
    EGLContext context;
    EGLSurface surface;
    GLuint programs[4],buffers[5];
    GLint locations[4][16];
    size_t capacities[5];
    GLint64 max_storage;
    GLint max_groups[3];
    int initialized,disabled;
    int range_cached;
    jint range_values[33*256];
    void *workspace[4];
    size_t workspace_capacity[4];
} GpuState;
static pthread_mutex_t gpu_mutex=PTHREAD_MUTEX_INITIALIZER;
static GpuState gpu;
static const char *const uniform_names[4][16]={
    {"width","height","step","smallWidth","smallHeight",0},
    {"count",0},
    {"width","begin","end","lo","hi","radius","inputOrigin","inputRows",0},
    {"width","begin","end","lo","hi","radius","inputOrigin","inputRows","noise","shadows","globalBudget","beauty","smoothLimit","packedPolicy","seedFromInput",0}
};

static int clean_gl(void){return glGetError()==GL_NO_ERROR;}
static GLuint compile_program(const char *source) {
    GLuint shader=glCreateShader(GL_COMPUTE_SHADER),program=0;
    GLint ok=0;if(!shader)return 0;
    glShaderSource(shader,1,&source,0);glCompileShader(shader);
    glGetShaderiv(shader,GL_COMPILE_STATUS,&ok);
    if(ok){program=glCreateProgram();if(program){glAttachShader(program,shader);glLinkProgram(program);glGetProgramiv(program,GL_LINK_STATUS,&ok);}}
    glDeleteShader(shader);
    if(!ok || !clean_gl()){if(program)glDeleteProgram(program);return 0;}
    return program;
}
/* The EGL display is process shared. Never terminate it: that would invalidate
 * unrelated camera/render contexts. Own pbuffer/context have no shared objects. */
static int initialize_gpu(void) {
    const EGLint attributes[]={EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RENDERABLE_TYPE,0x40,
        EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_ALPHA_SIZE,8,EGL_NONE};
    const EGLint pbuffer[]={EGL_WIDTH,1,EGL_HEIGHT,1,EGL_NONE};
    const EGLint context_attributes[]={EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE};
    EGLConfig config=0;EGLint count=0;
    gpu.display=eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if(gpu.display==EGL_NO_DISPLAY || !eglInitialize(gpu.display,0,0) ||
        !eglChooseConfig(gpu.display,attributes,&config,1,&count) || count!=1)return 0;
    gpu.surface=eglCreatePbufferSurface(gpu.display,config,pbuffer);
    gpu.context=eglCreateContext(gpu.display,config,EGL_NO_CONTEXT,context_attributes);
    if(gpu.surface==EGL_NO_SURFACE || gpu.context==EGL_NO_CONTEXT ||
        !eglMakeCurrent(gpu.display,gpu.surface,gpu.surface,gpu.context))return 0;
    GLint major=0,minor=0,bindings=0,compute_blocks=0,combined_blocks=0,shared=0,invocations=0,sizes[3]={0};
    glGetIntegerv(GL_MAJOR_VERSION,&major);glGetIntegerv(GL_MINOR_VERSION,&minor);
    glGetIntegerv(GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS,&bindings);
    glGetIntegerv(GL_MAX_COMPUTE_SHADER_STORAGE_BLOCKS,&compute_blocks);
    glGetIntegerv(GL_MAX_COMBINED_SHADER_STORAGE_BLOCKS,&combined_blocks);
    glGetIntegerv(GL_MAX_COMPUTE_SHARED_MEMORY_SIZE,&shared);
    glGetIntegerv(GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS,&invocations);
    glGetInteger64v(GL_MAX_SHADER_STORAGE_BLOCK_SIZE,&gpu.max_storage);
    for(int i=0;i<3;i++) {
        glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT,(GLuint)i,&gpu.max_groups[i]);
        glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_SIZE,(GLuint)i,&sizes[i]);
    }
    if(major<3 || (major==3 && minor<1) || bindings<5 || compute_blocks<5 || combined_blocks<5 ||
        shared<2048 || invocations<64 || sizes[0]<64 || sizes[1]<8 || sizes[2]<1 ||
        gpu.max_storage<4 || gpu.max_groups[0]<1 || gpu.max_groups[1]<1 || !clean_gl())return 0;
    const char *sources[4]={fusion_sums1949_source,pack_rgb1949_source,residual1949_source,residual_finish1949_source};
    for(int p=0;p<4;p++) {
        gpu.programs[p]=compile_program(sources[p]);if(!gpu.programs[p])return 0;
        for(int i=0;uniform_names[p][i];i++) {
            gpu.locations[p][i]=glGetUniformLocation(gpu.programs[p],uniform_names[p][i]);
            if(gpu.locations[p][i]<0)return 0;
        }
    }
    glGenBuffers(5,gpu.buffers);
    if(!clean_gl())return 0;
    gpu.initialized=1;return 1;
}
/* Called under the mutex. Reject an already current external context instead
 * of ever rebinding the camera's context. Save the caller's EGL API binding. */
static int enter_gpu(EGLenum *previous_api) {
    *previous_api=eglQueryAPI();
    if(gpu.disabled || eglGetCurrentContext()!=EGL_NO_CONTEXT)return 0;
    if(!eglBindAPI(EGL_OPENGL_ES_API))return 0;
    if(!gpu.initialized) {
        if(!initialize_gpu()){gpu.disabled=1;return 0;}
    } else if(!eglMakeCurrent(gpu.display,gpu.surface,gpu.surface,gpu.context)) {
        gpu.disabled=1;return 0;
    }
    return 1;
}
static void leave_gpu(EGLenum previous_api) {
    if(gpu.display!=EGL_NO_DISPLAY && eglGetCurrentContext()==gpu.context)
        if(!eglMakeCurrent(gpu.display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT))gpu.disabled=1;
    if(previous_api!=EGL_NONE)(void)eglBindAPI(previous_api);
}
/* G9 only caches capacity and executable programs, never an image upload. */
static int upload_buffer(int slot,size_t bytes,const void *data) {
    if(bytes==0 || bytes>MAX_BYTES || (uint64_t)bytes>(uint64_t)gpu.max_storage)return 0;
    glBindBuffer(GL_SHADER_STORAGE_BUFFER,gpu.buffers[slot]);
    if(gpu.capacities[slot]<bytes) {
        size_t capacity=(bytes+4095u)&~(size_t)4095u;
        if((uint64_t)capacity>(uint64_t)gpu.max_storage || capacity>MAX_BYTES)capacity=bytes;
        glBufferData(GL_SHADER_STORAGE_BUFFER,(GLsizeiptr)capacity,0,GL_DYNAMIC_COPY);
        if(!clean_gl())return 0;
        gpu.capacities[slot]=capacity;
    }
    if(data)glBufferSubData(GL_SHADER_STORAGE_BUFFER,0,(GLsizeiptr)bytes,data);
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER,(GLuint)slot,gpu.buffers[slot]);
    return clean_gl();
}
/* Values, not Java object identity, admit a retained immutable range table.
 * Shader storage slots are rebound every call; all image bytes remain fresh. */
static int upload_ranges(const jint *ranges) {
    size_t bytes=33u*256u*sizeof(jint);
    int same=gpu.range_cached && memcmp(gpu.range_values,ranges,bytes)==0;
    if(!upload_buffer(4,bytes,same?0:ranges))return 0;
    if(!same){memcpy(gpu.range_values,ranges,bytes);gpu.range_cached=1;}
    return 1;
}
/* Host staging is exclusively owned under gpu_mutex. Retain bounded capacity,
 * overwrite all live image bytes per call, and never hold Java arrays while
 * allocating, dispatching or waiting for the GPU. */
static void *borrow_workspace(int slot,size_t bytes) {
    if(bytes==0 || bytes>MAX_BYTES)return 0;
    if(gpu.workspace_capacity[slot]<bytes) {
        size_t capacity=(bytes+4095u)&~(size_t)4095u;
        if(capacity>MAX_BYTES)capacity=bytes;
        void *next=realloc(gpu.workspace[slot],capacity);
        if(!next)return 0;
        gpu.workspace[slot]=next;gpu.workspace_capacity[slot]=capacity;
    }
    return gpu.workspace[slot];
}
static void uniform_value(int p,int at,int value){glUniform1i(gpu.locations[p][at],value);}
static int dispatch_ok(int x,int y) {
    return x>0 && y>0 && x<=gpu.max_groups[0] && y<=gpu.max_groups[1];
}
/* Never map a buffer after an unsignaled/failed fence. A timeout disables this
 * backend for the process; retained resources are reclaimed at process exit. */
static int await_gpu(void) {
    glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT|GL_BUFFER_UPDATE_BARRIER_BIT);
    GLsync fence=glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE,0);
    if(!fence || !clean_gl())return 0;
    glFlush();GLenum result=glClientWaitSync(fence,GL_SYNC_FLUSH_COMMANDS_BIT,FENCE_NS);
    glDeleteSync(fence);
    return (result==GL_ALREADY_SIGNALED || result==GL_CONDITION_SATISFIED) && clean_gl();
}
static int read_buffer(int slot,size_t bytes,void *destination) {
    glBindBuffer(GL_SHADER_STORAGE_BUFFER,gpu.buffers[slot]);
    void *mapped=glMapBufferRange(GL_SHADER_STORAGE_BUFFER,0,(GLsizeiptr)bytes,GL_MAP_READ_BIT);
    if(!mapped || !clean_gl())return 0;
    memcpy(destination,mapped,bytes);
    return glUnmapBuffer(GL_SHADER_STORAGE_BUFFER)==GL_TRUE && clean_gl();
}
static int different(JNIEnv *env,jobject *objects,int count) {
    for(int i=0;i<count;i++){if(!objects[i])return 0;
        for(int j=i+1;j<count;j++)if((*env)->IsSameObject(env,objects[i],objects[j]))return 0;}
    return 1;
}

JNIEXPORT jint JNICALL JNINAME(nativeAbi)(JNIEnv *env,jclass cls){(void)env;(void)cls;return 1949;}

JNIEXPORT jboolean JNICALL JNINAME(fusionSumsNative)(JNIEnv *env,jclass cls,
        jbyteArray input,jint width,jint height,jint step,jintArray sums,jintArray counts) {
    (void)cls;jobject objects[3]={input,sums,counts};
    if(!different(env,objects,3) || width<1 || height<1 || width>16384 || height>16384 || step<1 || step>256)return JNI_FALSE;
    int64_t pixels=(int64_t)width*height,small_width=((int64_t)width+step-1)/step,
        small_height=((int64_t)height+step-1)/step,n=small_width*small_height;
    if(pixels>MAX_BYTES || n<1 || n>(int64_t)(MAX_BYTES/sizeof(jint)) ||
        (*env)->GetArrayLength(env,input)<pixels || (*env)->GetArrayLength(env,sums)<n ||
        (*env)->GetArrayLength(env,counts)<n)return JNI_FALSE;
    size_t input_bytes=((size_t)pixels+3u)&~(size_t)3u,output_bytes=(size_t)n*sizeof(jint);
    void *source=calloc(1,input_bytes);jint *result=malloc(output_bytes*2u);
    if(!source || !result){free(source);free(result);return JNI_FALSE;}
    (*env)->GetByteArrayRegion(env,input,0,(jsize)pixels,(jbyte *)source);
    if((*env)->ExceptionCheck(env)){free(source);free(result);return JNI_FALSE;}
    int ok=0;EGLenum previous_api=EGL_NONE;
    pthread_mutex_lock(&gpu_mutex);
    if(enter_gpu(&previous_api)) {
        int groups_x=((int)small_width+7)/8,groups_y=((int)small_height+7)/8;
        if(dispatch_ok(groups_x,groups_y) && upload_buffer(0,input_bytes,source) &&
            upload_buffer(1,output_bytes,0) && upload_buffer(2,output_bytes,0)) {
            glUseProgram(gpu.programs[0]);uniform_value(0,0,width);uniform_value(0,1,height);
            uniform_value(0,2,step);uniform_value(0,3,(int)small_width);uniform_value(0,4,(int)small_height);
            glDispatchCompute((GLuint)groups_x,(GLuint)groups_y,1);
            ok=clean_gl() && await_gpu() && read_buffer(1,output_bytes,result) &&
                read_buffer(2,output_bytes,result+n);
            if(!ok)gpu.disabled=1;
        }
    }
    leave_gpu(previous_api);pthread_mutex_unlock(&gpu_mutex);
    /* Critical sections contain only the final copies; no GPU/JNI allocation,
       waiting, EGL operation or callback runs while Java arrays are held. */
    if(ok) {
        jint *a=(*env)->GetPrimitiveArrayCritical(env,sums,0),*b=0;
        if(a)b=(*env)->GetPrimitiveArrayCritical(env,counts,0);
        if(a && b){memcpy(a,result,output_bytes);memcpy(b,result+n,output_bytes);}
        else ok=0;
        if(b)(*env)->ReleasePrimitiveArrayCritical(env,counts,b,ok?0:JNI_ABORT);
        if(a)(*env)->ReleasePrimitiveArrayCritical(env,sums,a,ok?0:JNI_ABORT);
    }
    free(source);free(result);return ok?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jboolean JNICALL JNINAME(aggregateNative)(JNIEnv *env,jclass cls,
        jintArray input,jintArray meta,jintArray summary,jint width,jint rows,
        jint begin,jint end,jint lo,jint hi,jint radius,jintArray range) {
    (void)cls;jobject objects[4]={input,meta,summary,range};
    if(!different(env,objects,4) || width<1 || rows<1 || width>16384 || radius<1 || radius>4 ||
        lo<0 || hi>rows || lo>=hi || begin<lo || end<begin || end>hi)return JNI_FALSE;
    int64_t all=(int64_t)width*rows,n=(int64_t)width*(end-begin);
    if(all>(*env)->GetArrayLength(env,input) || n>(*env)->GetArrayLength(env,meta) ||
        n*3>(*env)->GetArrayLength(env,summary) || (*env)->GetArrayLength(env,range)<33*256 ||
        n*3>(int64_t)(MAX_BYTES/sizeof(jint)))return JNI_FALSE;
    if(begin==end)return JNI_TRUE;
    int origin=begin-radius;if(origin<lo)origin=lo;
    int bottom=end>hi-radius?hi:end+radius,input_rows=bottom-origin;
    int64_t compact=(int64_t)width*input_rows;
    if(compact<1 || compact>(int64_t)(MAX_BYTES/sizeof(jint)))return JNI_FALSE;
    size_t input_bytes=(size_t)compact*sizeof(jint),meta_bytes=(size_t)n*sizeof(jint),output_bytes=meta_bytes*3u;
    jint *source=malloc(input_bytes),*metadata=malloc(meta_bytes),*result=malloc(output_bytes),*ranges=malloc(33u*256u*sizeof(jint));
    if(!source || !metadata || !result || !ranges){free(source);free(metadata);free(result);free(ranges);return JNI_FALSE;}
    (*env)->GetIntArrayRegion(env,input,(jsize)((int64_t)origin*width),(jsize)compact,source);
    (*env)->GetIntArrayRegion(env,meta,0,(jsize)n,metadata);
    (*env)->GetIntArrayRegion(env,summary,0,(jsize)(n*3),result);
    (*env)->GetIntArrayRegion(env,range,0,33*256,ranges);
    int valid=!(*env)->ExceptionCheck(env);
    for(int i=0;valid && i<33*256;i++)if(ranges[i]<0 || ranges[i]>256)valid=0;
    for(int64_t i=0;valid && i<n;i++)if((uint32_t)metadata[i]>>31) {
        int sigma=metadata[i]&63,threshold=((uint32_t)metadata[i]>>6)&63;
        if(sigma<2 || sigma>32 || threshold<3)valid=0;
    }
    int ok=0;EGLenum previous_api=EGL_NONE;
    if(valid) {
        pthread_mutex_lock(&gpu_mutex);
        if(enter_gpu(&previous_api)) {
            int groups_x=(width+7)/8,groups_y=(end-begin+7)/8,pack_groups=((int)compact+63)/64;
            if(dispatch_ok(groups_x,groups_y) && dispatch_ok(pack_groups,1) &&
                upload_buffer(0,input_bytes,source) && upload_buffer(1,input_bytes,0) &&
                upload_buffer(2,meta_bytes,metadata) && upload_buffer(3,output_bytes,result) &&
                upload_ranges(ranges)) {
                glUseProgram(gpu.programs[1]);uniform_value(1,0,(int)compact);
                glDispatchCompute((GLuint)pack_groups,1,1);
                /* G5 intermediate packed Y/chroma never crosses back to CPU. */
                glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
                glUseProgram(gpu.programs[2]);int values[8]={width,begin,end,lo,hi,radius,origin,input_rows};
                for(int i=0;i<8;i++)uniform_value(2,i,values[i]);
                glDispatchCompute((GLuint)groups_x,(GLuint)groups_y,1);
                ok=clean_gl() && await_gpu() && read_buffer(3,output_bytes,result);
                if(!ok)gpu.disabled=1;
            }
        }
        leave_gpu(previous_api);pthread_mutex_unlock(&gpu_mutex);
    }
    if(ok) {
        jint *destination=(*env)->GetPrimitiveArrayCritical(env,summary,0);
        if(destination){memcpy(destination,result,output_bytes);(*env)->ReleasePrimitiveArrayCritical(env,summary,destination,0);}
        else ok=0;
    }
    free(source);free(metadata);free(result);free(ranges);return ok?JNI_TRUE:JNI_FALSE;
}


/* Fused residual aggregate and exact Java integer pixel finalization. Only the
 * final tile is read back. Full original input is immutable; halos are compact
 * and all inactive output entries retain the caller's original destination. */
static jboolean finish_gpu(JNIEnv *env,jclass cls,
        jintArray input,jintArray meta,jintArray policy,jintArray output,jint output_offset,
        jint width,jint rows,jint begin,jint end,jint lo,jint hi,jint radius,
        jintArray range,jint noise,jboolean shadows,jint global,jint beauty,jint smooth_limit,
        int packed_policy,int seed_from_input) {
    (void)cls;jobject objects[5]={input,meta,policy,output,range};
    if(!different(env,objects,5) || output_offset<0 || width<1 || rows<1 || width>16384 || radius<1 || radius>4 ||
        lo<0 || hi>rows || lo>=hi || begin<lo || end<begin || end>hi ||
        noise<1 || noise>4 || global<0 || global>256 || beauty<0 || beauty>256 ||
        smooth_limit<0 || smooth_limit>512)return JNI_FALSE;
    int64_t all=(int64_t)width*rows,n=(int64_t)width*(end-begin);
    if(all>(*env)->GetArrayLength(env,input) || n>(*env)->GetArrayLength(env,meta) ||
        n*(packed_policy?1:3)>(*env)->GetArrayLength(env,policy) ||
        (int64_t)output_offset+n>(*env)->GetArrayLength(env,output) ||
        (*env)->GetArrayLength(env,range)<33*256 || n*(packed_policy?2:4)>(int64_t)(MAX_BYTES/sizeof(jint)))return JNI_FALSE;
    if(begin==end)return JNI_TRUE;
    int origin=begin-radius;if(origin<lo)origin=lo;
    int bottom=end>hi-radius?hi:end+radius,input_rows=bottom-origin;
    int64_t compact=(int64_t)width*input_rows;
    if(compact<1 || compact>(int64_t)(MAX_BYTES/sizeof(jint)))return JNI_FALSE;
    size_t input_bytes=(size_t)compact*sizeof(jint),
        meta_bytes=(size_t)n*(packed_policy?2u:4u)*sizeof(jint),output_bytes=(size_t)n*sizeof(jint);
    int ok=0;EGLenum previous_api=EGL_NONE;
    pthread_mutex_lock(&gpu_mutex);
    jint *source=borrow_workspace(0,input_bytes),*metadata=borrow_workspace(1,meta_bytes),
        *result=borrow_workspace(2,output_bytes),*ranges=borrow_workspace(3,33u*256u*sizeof(jint));
    if(source && metadata && result && ranges) {
        (*env)->GetIntArrayRegion(env,input,(jsize)((int64_t)origin*width),(jsize)compact,source);
        (*env)->GetIntArrayRegion(env,meta,0,(jsize)n,metadata);
        (*env)->GetIntArrayRegion(env,policy,0,(jsize)(n*(packed_policy?1:3)),metadata+n);
        if(!seed_from_input)(*env)->GetIntArrayRegion(env,output,output_offset,(jsize)n,result);
        (*env)->GetIntArrayRegion(env,range,0,33*256,ranges);
        int valid=!(*env)->ExceptionCheck(env);
        for(int i=0;valid && i<33*256;i++)if(ranges[i]<0 || ranges[i]>256)valid=0;
        for(int64_t i=0;valid && i<n;i++)if((uint32_t)metadata[i]>>31) {
            int sigma=metadata[i]&63,threshold=((uint32_t)metadata[i]>>6)&63;
            if(sigma<2 || sigma>32 || threshold<3)valid=0;
            if(packed_policy){
                uint32_t p=(uint32_t)metadata[n+i];
                if((p>>27)!=0 || (p&511u)>256u || ((p>>9)&511u)>256u ||
                        ((p>>18)&511u)>256u)valid=0;
            }else{
                jint *p=metadata+n+i*3;
                if(p[0]<0 || p[0]>256 || p[1]<0 || p[1]>256 || p[2]<0 || p[2]>256)valid=0;
            }
        }
        if(valid && enter_gpu(&previous_api)) {
            int groups_x=(width+7)/8,groups_y=(end-begin+7)/8,pack_groups=((int)compact+63)/64;
            if(dispatch_ok(groups_x,groups_y) && dispatch_ok(pack_groups,1) &&
                upload_buffer(0,input_bytes,source) && upload_buffer(1,input_bytes,0) &&
                upload_buffer(2,meta_bytes,metadata) &&
                upload_buffer(3,output_bytes,seed_from_input?0:result) && upload_ranges(ranges)) {
                glUseProgram(gpu.programs[1]);uniform_value(1,0,(int)compact);
                glDispatchCompute((GLuint)pack_groups,1,1);
                glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
                glUseProgram(gpu.programs[3]);
                int values[13]={width,begin,end,lo,hi,radius,origin,input_rows,noise,shadows?1:0,global,beauty,smooth_limit};
                for(int i=0;i<13;i++)uniform_value(3,i,values[i]);
                uniform_value(3,13,packed_policy);uniform_value(3,14,seed_from_input);
                glDispatchCompute((GLuint)groups_x,(GLuint)groups_y,1);
                ok=clean_gl() && await_gpu() && read_buffer(3,output_bytes,result);
                if(!ok)gpu.disabled=1;
            }
        }
        leave_gpu(previous_api);
        if(ok) {
            // Commit only this disjoint destination slice. A copying JVM must
            // never copy/release the whole full-frame array for a bounded tile.
            (*env)->SetIntArrayRegion(env,output,output_offset,(jsize)n,result);
            ok=!(*env)->ExceptionCheck(env);
        }
    }
    pthread_mutex_unlock(&gpu_mutex);
    return ok?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jboolean JNICALL JNINAME(finishNative)(JNIEnv *env,jclass cls,
        jintArray input,jintArray meta,jintArray policy,jintArray output,
        jint width,jint rows,jint begin,jint end,jint lo,jint hi,jint radius,
        jintArray range,jint noise,jboolean shadows,jint global,jint beauty,jint smooth_limit) {
    return finish_gpu(env,cls,input,meta,policy,output,0,width,rows,begin,end,lo,hi,radius,
        range,noise,shadows,global,beauty,smooth_limit,0,0);
}
JNIEXPORT jboolean JNICALL JNINAME(finishIntoNative)(JNIEnv *env,jclass cls,
        jintArray input,jintArray meta,jintArray policy,jintArray output,jint output_offset,
        jint width,jint rows,jint begin,jint end,jint lo,jint hi,jint radius,
        jintArray range,jint noise,jboolean shadows,jint global,jint beauty,jint smooth_limit) {
    return finish_gpu(env,cls,input,meta,policy,output,output_offset,width,rows,begin,end,lo,hi,radius,
        range,noise,shadows,global,beauty,smooth_limit,0,0);
}
JNIEXPORT jboolean JNICALL JNINAME(finishSavedIntoNative)(JNIEnv *env,jclass cls,
        jintArray input,jintArray meta,jintArray packed_policy,jintArray output,jint output_offset,
        jint width,jint rows,jint begin,jint end,jint lo,jint hi,jint radius,
        jintArray range,jint noise,jboolean shadows,jint global,jint beauty,jint smooth_limit) {
    return finish_gpu(env,cls,input,meta,packed_policy,output,output_offset,width,rows,begin,end,lo,hi,radius,
        range,noise,shadows,global,beauty,smooth_limit,1,1);
}
