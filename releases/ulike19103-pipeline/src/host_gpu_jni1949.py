#!/usr/bin/env python3
"""Execute unchanged production GPU C/JNI against real Mesa software GLES.

Only the host adapter is generated: it calls the unchanged scalar C oracle,
holds a foreign EGL context, and intercepts two GL entry points to observe the
production fence bound and deliberately return an unsignaled-fence result.
The production translation unit and embedded shaders are compiled unchanged.
No software-host timing is treated as a physical Android speed measurement.
"""
import argparse
import hashlib
import importlib.util
import json
import os
import pathlib
import re
import shutil
import subprocess

ADAPTER = r'''
#include <jni.h>
#include <EGL/egl.h>
#include <GLES3/gl31.h>
#include <stdint.h>
#include <stdlib.h>
#include "residual1944.h"
#define TESTNAME(name) Java_com_hiro_ulike_GpuNative1949Test_##name
static int forced_timeout;
static uint64_t last_timeout,fence_calls,map_calls;
GLenum glClientWaitSync(GLsync sync,GLbitfield flags,GLuint64 timeout) {
    last_timeout=timeout;fence_calls++;
    if(forced_timeout)return GL_TIMEOUT_EXPIRED;
    PFNGLCLIENTWAITSYNCPROC real=(PFNGLCLIENTWAITSYNCPROC)eglGetProcAddress("glClientWaitSync");
    return real?real(sync,flags,timeout):GL_WAIT_FAILED;
}
void *glMapBufferRange(GLenum target,GLintptr offset,GLsizeiptr length,GLbitfield access) {
    map_calls++;
    PFNGLMAPBUFFERRANGEPROC real=(PFNGLMAPBUFFERRANGEPROC)eglGetProcAddress("glMapBufferRange");
    return real?real(target,offset,length,access):0;
}
JNIEXPORT void JNICALL TESTNAME(forceTimeout)(JNIEnv *env,jclass cls,jboolean enabled) {
    (void)env;(void)cls;forced_timeout=enabled;last_timeout=fence_calls=map_calls=0;
}
JNIEXPORT jlongArray JNICALL TESTNAME(fenceFacts)(JNIEnv *env,jclass cls) {
    (void)cls;jlong values[3]={(jlong)last_timeout,(jlong)fence_calls,(jlong)map_calls};
    jlongArray out=(*env)->NewLongArray(env,3);if(out)(*env)->SetLongArrayRegion(env,out,0,3,values);return out;
}
static EGLDisplay external_display;
static EGLContext external_context;
static EGLSurface external_surface;
static EGLenum external_api;
JNIEXPORT jboolean JNICALL TESTNAME(beginExternal)(JNIEnv *env,jclass cls) {
    (void)env;(void)cls;external_api=eglQueryAPI();
    if(eglGetCurrentContext()!=EGL_NO_CONTEXT||!eglBindAPI(EGL_OPENGL_ES_API))return JNI_FALSE;
    external_display=eglGetDisplay(EGL_DEFAULT_DISPLAY);
    EGLConfig config=0;EGLint count=0;
    const EGLint attrs[]={EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RENDERABLE_TYPE,0x40,
        EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_ALPHA_SIZE,8,EGL_NONE};
    const EGLint surface[]={EGL_WIDTH,1,EGL_HEIGHT,1,EGL_NONE};
    const EGLint context[]={EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE};
    if(external_display==EGL_NO_DISPLAY||!eglInitialize(external_display,0,0)||
            !eglChooseConfig(external_display,attrs,&config,1,&count)||count!=1)return JNI_FALSE;
    external_surface=eglCreatePbufferSurface(external_display,config,surface);
    external_context=eglCreateContext(external_display,config,EGL_NO_CONTEXT,context);
    return external_surface!=EGL_NO_SURFACE&&external_context!=EGL_NO_CONTEXT&&
        eglMakeCurrent(external_display,external_surface,external_surface,external_context)?JNI_TRUE:JNI_FALSE;
}
JNIEXPORT jboolean JNICALL TESTNAME(externalUnchanged)(JNIEnv *env,jclass cls) {
    (void)env;(void)cls;
    return eglGetCurrentContext()==external_context&&eglGetCurrentDisplay()==external_display&&
        eglGetCurrentSurface(EGL_READ)==external_surface&&eglGetCurrentSurface(EGL_DRAW)==external_surface&&
        eglQueryAPI()==EGL_OPENGL_ES_API?JNI_TRUE:JNI_FALSE;
}
JNIEXPORT jboolean JNICALL TESTNAME(endExternal)(JNIEnv *env,jclass cls) {
    (void)env;(void)cls;
    int ok=eglGetCurrentContext()==external_context;
    ok=eglMakeCurrent(external_display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT)&&ok;
    ok=eglDestroyContext(external_display,external_context)&&ok;
    ok=eglDestroySurface(external_display,external_surface)&&ok;
    /* Shared display intentionally remains initialized, as in production. */
    ok=eglBindAPI(external_api)&&ok;return ok?JNI_TRUE:JNI_FALSE;
}
JNIEXPORT void JNICALL TESTNAME(scalarAggregate)(JNIEnv *env,jclass cls,
        jintArray input,jintArray meta,jintArray output,jint width,jint begin,jint end,
        jint lo,jint hi,jint radius,jintArray range) {
    (void)cls;
    jint *src=(*env)->GetIntArrayElements(env,input,0),*m=(*env)->GetIntArrayElements(env,meta,0);
    jint *out=(*env)->GetIntArrayElements(env,output,0),*r=(*env)->GetIntArrayElements(env,range,0);
    int slots=radius*2+1,taps=radius==4?7:slots;
    int32_t *ring=malloc((size_t)width*slots*sizeof(int32_t));
    int32_t *x=malloc((size_t)width*taps*sizeof(int32_t));
    const int offsets[7]={-4,-2,-1,0,1,2,4};
    if(src&&m&&out&&r&&ring&&x) {
        for(int col=0;col<width;col++)for(int at=0;at<taps;at++){
            int dx=radius==4?offsets[at]:at-radius,sx=col+dx;
            x[col*taps+at]=sx<0?0:sx>=width?width-1:sx;
        }
        residual1944_aggregate(src,m,out,width,begin,end,lo,hi,radius,r,ring,x);
    } else {
        jclass error=(*env)->FindClass(env,"java/lang/OutOfMemoryError");
        if(error)(*env)->ThrowNew(env,error,"Host-only scalar reference allocation failed");
    }
    free(ring);free(x);
    if(r)(*env)->ReleaseIntArrayElements(env,range,r,JNI_ABORT);
    if(out)(*env)->ReleaseIntArrayElements(env,output,out,0);
    if(m)(*env)->ReleaseIntArrayElements(env,meta,m,JNI_ABORT);
    if(src)(*env)->ReleaseIntArrayElements(env,input,src,JNI_ABORT);
}
'''


def run(args, env=None):
    completed = subprocess.run([str(v) for v in args], text=True,
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                               env=env)
    if completed.returncode:
        raise RuntimeError(completed.stdout)
    return completed.stdout


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def test(root, work, jdk=None, ndk=None):
    root = pathlib.Path(root).resolve()
    source = root / 'native1949'
    work = pathlib.Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = pathlib.Path(jdk or os.environ.get('ULIKE_JDK_HOME', root.parent.parent / 'jdk21')).resolve()
    ndk = pathlib.Path(ndk or os.environ.get('ULIKE_NDK_HOME', root.parent.parent / 'ndk27c')).resolve()
    if not re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',
                     (ndk / 'source.properties').read_text(), re.M):
        raise RuntimeError('Production-JNI host test requires pinned NDK r27c headers')
    headers = work / 'headers'
    headers.mkdir(exist_ok=True)
    includes = ndk / 'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
    for folder in ['EGL', 'GLES3', 'KHR']:
        shutil.copytree(includes / folder, headers / folder, dirs_exist_ok=True)
    spec = importlib.util.spec_from_file_location('native1949_host_headers', source / 'build_native1949.py')
    builder = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(builder)
    header = builder.shader_header(source, work)
    adapter = work / 'gpu1949-host-adapter.c'
    adapter.write_text(ADAPTER)
    library = work / 'libulike_gpu1949.so'
    flags = ['-std=c11', '-O3', '-shared', '-fPIC', '-fno-fast-math', '-ffp-contract=off',
             '-Wall', '-Wextra', '-Werror', '-DULIKE_RESIDUAL_SCALAR', '-DANDROID',
             '-Wl,--no-undefined', '-Wl,-Bsymbolic-functions', '-pthread']
    run(['cc', *flags, '-I'+str(headers), '-I'+str(jdk / 'include'),
         '-I'+str(jdk / 'include/linux'), '-I'+str(work), '-I'+str(root / 'native1944'),
         source / 'gpu1949.c', root / 'native1944/residual1944.c', adapter,
         '-l:libEGL.so.1', '-l:libGL.so.1', '-o', library])
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    java_test = root / 'tests/GpuNative1949Test.java'
    run([jdk / 'bin/javac', '-d', classes, root / 'GpuInteger1949.java', java_test])
    environment = os.environ.copy()
    environment['EGL_PLATFORM'] = 'surfaceless'
    environment['LIBGL_ALWAYS_SOFTWARE'] = '1'
    output = run([jdk / 'bin/java', '-Xcheck:jni', '-Djava.library.path='+str(work),
                  '-cp', classes, 'com.hiro.ulike.GpuNative1949Test'], env=environment)
    (work / 'gpu-native1949-java.txt').write_text(output)
    if 'WARNING' in output or 'FATAL ERROR' in output:
        raise RuntimeError('JNI runtime checker rejected production execution:\n'+output)
    report = json.loads(output.strip())
    if report.get('status') != 'passed' or report.get('assertions', 0) < 1:
        raise RuntimeError('Production JNI test did not pass')
    report.update({
        'schema': 'ulike-gpu1949-production-jni-host-v1',
        'host_execution': 'Mesa software EGL surfaceless GLES3.1 or newer',
        'production_translation_unit_unchanged': True,
        'production_shader_header_generator_used': True,
        'reference': 'unchanged scalar residual1944.c',
        'host_only_fence_interposition': True,
        'jni_runtime_checker_passed': True,
        'physical_android_tested': False,
        'device_speed_measured': False,
        'fence_timeout_ns': 200000000,
        'sources': {str(path.relative_to(root)): digest(path) for path in [
            source / 'gpu1949.c', source / 'build_native1949.py', root / 'native1944/residual1944.c',
            root / 'native1944/residual1944.h', root / 'GpuInteger1949.java', java_test,
            pathlib.Path(__file__).resolve(), *sorted(source.glob('*.comp'))]},
        'generated_shader_header_sha256': digest(header),
        'host_adapter_sha256': hashlib.sha256(ADAPTER.encode()).hexdigest(),
    })
    (work / 'host-gpu-jni1949.json').write_text(json.dumps(report, indent=2)+'\n')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(pathlib.Path(__file__).resolve().parent, args.work, args.jdk, args.ndk)))
