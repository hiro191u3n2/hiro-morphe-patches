#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include <pthread.h>
#include <EGL/egl.h>
#include <GLES3/gl31.h>
#include "bilateral_source1951.h"

/* A dedicated pbuffer context belongs solely to the save worker. We refuse
 * callers with an active EGL context, and never terminate the process display. */
typedef struct {
    EGLDisplay display;
    EGLContext context;
    EGLSurface surface;
    GLuint program, buffers[5];
    GLsizeiptr capacities[5];
    GLint uniforms[8];
    int disabled, initialized;
} State;
static State gpu;
static pthread_mutex_t gpu_lock=PTHREAD_MUTEX_INITIALIZER;

static int enter(EGLenum *old_api){
    if(gpu.disabled || eglGetCurrentContext()!=EGL_NO_CONTEXT)return 0;
    *old_api=eglQueryAPI();
    if(!eglBindAPI(EGL_OPENGL_ES_API))return 0;
    if(!gpu.initialized){
        const EGLint cfg[]={EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RENDERABLE_TYPE,0x40,
            EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_ALPHA_SIZE,8,EGL_NONE};
        const EGLint surf[]={EGL_WIDTH,1,EGL_HEIGHT,1,EGL_NONE};
        const EGLint context[]={EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE};
        EGLConfig chosen=0;EGLint count=0;
        gpu.display=eglGetDisplay(EGL_DEFAULT_DISPLAY);
        if(gpu.display==EGL_NO_DISPLAY || !eglInitialize(gpu.display,0,0) ||
                !eglChooseConfig(gpu.display,cfg,&chosen,1,&count) || count!=1)goto fail;
        gpu.surface=eglCreatePbufferSurface(gpu.display,chosen,surf);
        gpu.context=eglCreateContext(gpu.display,chosen,EGL_NO_CONTEXT,context);
        if(gpu.surface==EGL_NO_SURFACE || gpu.context==EGL_NO_CONTEXT)goto fail;
        if(!eglMakeCurrent(gpu.display,gpu.surface,gpu.surface,gpu.context))goto fail;
        GLint major=0,minor=0;
        glGetIntegerv(GL_MAJOR_VERSION,&major);glGetIntegerv(GL_MINOR_VERSION,&minor);
        if(major<3 || (major==3&&minor<1))goto fail;
        GLuint shader=glCreateShader(GL_COMPUTE_SHADER);
        const GLchar *source=bilateral1951_source;
        glShaderSource(shader,1,&source,0);glCompileShader(shader);
        GLint ok=GL_FALSE;glGetShaderiv(shader,GL_COMPILE_STATUS,&ok);
        if(!ok){glDeleteShader(shader);goto fail;}
        gpu.program=glCreateProgram();glAttachShader(gpu.program,shader);
        glLinkProgram(gpu.program);glDeleteShader(shader);
        glGetProgramiv(gpu.program,GL_LINK_STATUS,&ok);if(!ok)goto fail;
        const char *names[]={"uWidth","uRows","uNoise","uHorizontal","uTexture","uShadows","uBegin","uEnd"};
        for(int i=0;i<8;i++)if((gpu.uniforms[i]=glGetUniformLocation(gpu.program,names[i]))<0)goto fail;
        glGenBuffers(5,gpu.buffers);
        if(glGetError()!=GL_NO_ERROR)goto fail;
        gpu.initialized=1;
        return 1;
fail:
        gpu.disabled=1;
        if(gpu.display!=EGL_NO_DISPLAY&&eglGetCurrentContext()==gpu.context)
            (void)eglMakeCurrent(gpu.display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);
        if(*old_api!=EGL_NONE)(void)eglBindAPI(*old_api);
        return 0;
    }
    if(!eglMakeCurrent(gpu.display,gpu.surface,gpu.surface,gpu.context)){
        gpu.disabled=1;
        if(*old_api!=EGL_NONE)(void)eglBindAPI(*old_api);
        return 0;
    }
    return 1;
}

static void leave(EGLenum old_api){
    if(gpu.display!=EGL_NO_DISPLAY&&eglGetCurrentContext()==gpu.context)
        if(!eglMakeCurrent(gpu.display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT))gpu.disabled=1;
    if(old_api!=EGL_NONE)(void)eglBindAPI(old_api);
}

static int buffer(int slot,GLsizeiptr bytes,const void *content){
    glBindBuffer(GL_SHADER_STORAGE_BUFFER,gpu.buffers[slot]);
    if(gpu.capacities[slot]<bytes){
        glBufferData(GL_SHADER_STORAGE_BUFFER,bytes,0,GL_DYNAMIC_COPY);
        gpu.capacities[slot]=bytes;
    }
    if(content)glBufferSubData(GL_SHADER_STORAGE_BUFFER,0,bytes,content);
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER,(GLuint)slot,gpu.buffers[slot]);
    return glGetError()==GL_NO_ERROR;
}
static void pass(int width,int rows,int noise,int horizontal,int texture,int shadows,int begin,int end,
        GLuint guide,GLuint values,GLuint out){
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER,0,guide);
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER,1,values);
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER,2,out);
    glUniform1i(gpu.uniforms[0],width);glUniform1i(gpu.uniforms[1],rows);
    glUniform1i(gpu.uniforms[2],noise);glUniform1i(gpu.uniforms[3],horizontal);
    glUniform1i(gpu.uniforms[4],texture);glUniform1i(gpu.uniforms[5],shadows);
    glUniform1i(gpu.uniforms[6],begin);glUniform1i(gpu.uniforms[7],end);
    glDispatchCompute((GLuint)((width+15)/16),(GLuint)((end-begin+15)/16),1);
}
static int compute(const int32_t *source,int32_t *result,int32_t *top_rows,int32_t *bottom_rows,
        int width,int rows,int noise,
        int texture,int shadows,int begin,int end,const int32_t *luma,const int32_t *colour){
    GLint64 limit=0;glGetInteger64v(GL_MAX_SHADER_STORAGE_BLOCK_SIZE,&limit);
    size_t pixels=(size_t)width*rows;
    GLsizeiptr all=(GLsizeiptr)(pixels*sizeof(int32_t));
    GLsizeiptr slice=(GLsizeiptr)((size_t)(end-begin)*width*sizeof(int32_t));
    if(limit<all || width>65535*16 || end-begin>65535*16)return 0;
    if(!buffer(0,all,source) || !buffer(1,all,0) || !buffer(2,all,0) ||
            !buffer(3,1280*sizeof(int32_t),luma) || !buffer(4,1280*sizeof(int32_t),colour))return 0;
    glUseProgram(gpu.program);
    int first=begin>3?begin-3:0,last=end<rows-3?end+3:rows;
    pass(width,rows,noise,1,texture,shadows,first,last,gpu.buffers[0],gpu.buffers[0],gpu.buffers[1]);
    glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
    pass(width,rows,noise,0,texture,shadows,begin,end,gpu.buffers[0],gpu.buffers[1],gpu.buffers[2]);
    glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT|GL_BUFFER_UPDATE_BARRIER_BIT);
    GLsync sync=glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE,0);
    if(!sync || glGetError()!=GL_NO_ERROR)return 0;
    glFlush();
    GLenum status=glClientWaitSync(sync,GL_SYNC_FLUSH_COMMANDS_BIT,3000000000ULL);
    glDeleteSync(sync);
    if(status!=GL_ALREADY_SIGNALED && status!=GL_CONDITION_SATISFIED)return 0;
    glBindBuffer(GL_SHADER_STORAGE_BUFFER,gpu.buffers[2]);
    void *mapped=glMapBufferRange(GL_SHADER_STORAGE_BUFFER,
        (GLintptr)((size_t)begin*width*sizeof(int32_t)),slice,GL_MAP_READ_BIT);
    if(!mapped)return 0;
    memcpy(result,mapped,(size_t)slice);
    GLboolean unmapped=glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);
    if(unmapped!=GL_TRUE||glGetError()!=GL_NO_ERROR)return 0;
    // Production stage 0 computes a three-row halo outside the consumed
    // interval. Stage 2 packs into Work.horizontal inside the interval, while
    // stage 3 can read the halo. Return only those six border rows; the actual
    // intermediate for the consumed region remains on GPU through both passes.
    glBindBuffer(GL_SHADER_STORAGE_BUFFER,gpu.buffers[1]);
    if(first<begin){
        GLsizeiptr bytes=(GLsizeiptr)((size_t)(begin-first)*width*sizeof(int32_t));
        void *p=glMapBufferRange(GL_SHADER_STORAGE_BUFFER,
            (GLintptr)((size_t)first*width*sizeof(int32_t)),bytes,GL_MAP_READ_BIT);
        if(!p)return 0;
        memcpy(top_rows,p,(size_t)bytes);
        if(glUnmapBuffer(GL_SHADER_STORAGE_BUFFER)!=GL_TRUE)return 0;
    }
    if(end<last){
        GLsizeiptr bytes=(GLsizeiptr)((size_t)(last-end)*width*sizeof(int32_t));
        void *p=glMapBufferRange(GL_SHADER_STORAGE_BUFFER,
            (GLintptr)((size_t)end*width*sizeof(int32_t)),bytes,GL_MAP_READ_BIT);
        if(!p)return 0;
        memcpy(bottom_rows,p,(size_t)bytes);
        if(glUnmapBuffer(GL_SHADER_STORAGE_BUFFER)!=GL_TRUE)return 0;
    }
    return glGetError()==GL_NO_ERROR;
}

__attribute__((visibility("default"))) JNIEXPORT jint JNICALL
Java_com_hiro_ulike_CorePixels1950_bilateralPairGpuAbi(JNIEnv *env,jclass owner){
    (void)env;(void)owner;return 1951;
}
__attribute__((visibility("default"))) JNIEXPORT jboolean JNICALL
Java_com_hiro_ulike_CorePixels1950_bilateralPairGpuNative(JNIEnv *env,jclass owner,
    jintArray guide,jintArray output,jintArray horizontal,jint width,jint rows,jint noise,jboolean texture,
    jboolean shadows,jint begin,jint end,jintArray lr,jintArray cc){
    (void)owner;
    if(!guide||!output||!horizontal||!lr||!cc||width<1||rows<1||noise<1||noise>4||
            begin<0||end<=begin||end>rows||(int64_t)width*rows>INT_MAX||
            (*env)->IsSameObject(env,guide,output)||
            (*env)->IsSameObject(env,guide,horizontal)||
            (*env)->IsSameObject(env,output,horizontal))return JNI_FALSE;
    jint n=width*rows,offset=begin*width,slice=(end-begin)*width;
    if((*env)->GetArrayLength(env,guide)<n||(*env)->GetArrayLength(env,output)<n||
            (*env)->GetArrayLength(env,horizontal)<n||
            (*env)->GetArrayLength(env,lr)<1280||(*env)->GetArrayLength(env,cc)<1280)return JNI_FALSE;
    int first=begin>3?begin-3:0,last=end<rows-3?end+3:rows;
    int top_count=(begin-first)*width,bottom_count=(last-end)*width;
    int32_t *input=(int32_t*)malloc((size_t)n*sizeof(int32_t));
    int32_t *result=(int32_t*)malloc((size_t)slice*sizeof(int32_t));
    int32_t *top_rows=top_count?(int32_t*)malloc((size_t)top_count*sizeof(int32_t)):0;
    int32_t *bottom_rows=bottom_count?(int32_t*)malloc((size_t)bottom_count*sizeof(int32_t)):0;
    if(!input||!result||(top_count&&!top_rows)||(bottom_count&&!bottom_rows)){
        free(input);free(result);free(top_rows);free(bottom_rows);return JNI_FALSE;
    }
    int32_t luma[1280],colour[1280];
    (*env)->GetIntArrayRegion(env,guide,0,n,(jint*)input);
    (*env)->GetIntArrayRegion(env,lr,0,1280,(jint*)luma);
    (*env)->GetIntArrayRegion(env,cc,0,1280,(jint*)colour);
    if((*env)->ExceptionCheck(env)){
        free(input);free(result);free(top_rows);free(bottom_rows);return JNI_FALSE;
    }
    int valid=1;
    for(int i=noise*256;i<(noise+1)*256;i++)
        if(luma[i]<0||luma[i]>256||colour[i]<0||colour[i]>256){valid=0;break;}
    int success=0;
    if(valid&&pthread_mutex_lock(&gpu_lock)==0){
        EGLenum previous=EGL_NONE;
        if(enter(&previous)){
            success=compute(input,result,top_rows,bottom_rows,width,rows,noise,
                texture,shadows,begin,end,luma,colour);
            leave(previous);
        }
        pthread_mutex_unlock(&gpu_lock);
    }
    free(input);
    if(success){
        // Partial GPU failures never commit even one row; only this disjoint
        // destination interval becomes visible to later packing/sharpening.
        (*env)->SetIntArrayRegion(env,output,offset,slice,(jint*)result);
        if(top_count)(*env)->SetIntArrayRegion(env,horizontal,first*width,top_count,(jint*)top_rows);
        if(bottom_count)(*env)->SetIntArrayRegion(env,horizontal,end*width,bottom_count,(jint*)bottom_rows);
        success=!(*env)->ExceptionCheck(env);
    }
    free(result);free(top_rows);free(bottom_rows);
    return success?JNI_TRUE:JNI_FALSE;
}
