#!/usr/bin/env python3
"""Reusable current-production GLES fixture for exact protection/geometry tests.

Compiles current Java helpers and the actual JNI/shader bytes. Android services
use the declared host fixtures; this is never an Android speed benchmark.
"""
from pathlib import Path
import importlib.util
import json
import os
import re
import shutil
import subprocess


def run(command, log, env=None, timeout=120):
    result=subprocess.run(list(map(str,command)),capture_output=True,text=True,env=env,timeout=timeout)
    Path(log).write_text(result.stdout+result.stderr)
    if result.returncode:raise RuntimeError(str(log)+'\n'+result.stdout[-3000:]+result.stderr[-9000:])
    return result.stdout


def compile_classes(root, work, jdk, extra=()):
    root,work,jdk=Path(root),Path(work),Path(jdk)
    work.mkdir(parents=True,exist_ok=True);classes=work/'classes';classes.mkdir(exist_ok=True)
    from host_gpu1961 import all_sources
    sources=all_sources(root)+[root/'tests1961/ReferenceFacePixels1960.java',root/'tests1961/Protection1961Test.java']
    sources+=list(map(Path,extra));sources=list(dict.fromkeys(sources))
    run([jdk/'bin/javac','-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-d',classes,*sources],work/'java-compile.log')
    return classes


def compile_native(root, work, jdk, ndk):
    root,work,jdk,ndk=map(Path,(root,work,jdk,ndk));work.mkdir(parents=True,exist_ok=True)
    graphics=ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include';headers=work/'headers';headers.mkdir(exist_ok=True)
    for name in ('EGL','GLES3','KHR'):shutil.copytree(graphics/name,headers/name,dirs_exist_ok=True)
    spec=importlib.util.spec_from_file_location('protection1961_builder',root/'native1960/build_native1960.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder)
    builder.shader_header(root/'native1960',work)
    library=work/'libulike_gpu1960.so'
    run(['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-DANDROID',
         '-I'+str(headers),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work),root/'native1960/engine1960.c',*sorted((root/'native1960').glob('residual_blocks1961.c')),
         '-Wl,--no-undefined','-l:libEGL.so.1','-l:libGL.so.1','-lm','-o',library],work/'native-compile.log')
    return work


def execute(classes,native_dir,jdk,klass,log,args=()):
    output=run([Path(jdk)/'bin/java','-XX:ActiveProcessorCount=4','-Xcheck:jni','-Djava.library.path='+str(native_dir),'-cp',classes,'com.hiro.ulike.'+klass,*args],log,
        env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1'))
    found=re.search(r'^RESULT (\{[^\n]+\})$',output,re.M)
    if not found or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError('Missing actual JNI checked result '+klass)
    result=json.loads(found.group(1))
    if result.get('status')!='passed' or result.get('assertions',0)<1:raise AssertionError('Failed actual production test '+klass)
    return result


def test(root,work,jdk=None,ndk=None):
    root=Path(root).resolve();work=Path(work).resolve()/'host-protection1961';work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);ndk=Path(ndk or os.environ['ULIKE_NDK_HOME'])
    classes=compile_classes(root,work,jdk);native=compile_native(root,work,jdk,ndk)
    result=execute(classes,native,jdk,'Protection1961Test',work/'protection-run.log')
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
