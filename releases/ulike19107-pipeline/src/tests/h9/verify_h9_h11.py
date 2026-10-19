#!/usr/bin/env python3
"""Host proof for H9 packed policy, H10 overlap, H11 seedless GLES output."""
import importlib.util
import os
import pathlib
import subprocess
import sys

here=pathlib.Path(__file__).resolve().parent
root=here.parents[1]
build=pathlib.Path(os.environ.get('ULIKE_H9_WORK',root.parent/'build'))
work=build/'h9h11-verify';work.mkdir(parents=True,exist_ok=True)
java=os.environ.get('ULIKE_JDK_HOME','')
java=str(pathlib.Path(java)/'bin/java') if java else 'java'
def run(args,env=None):
    subprocess.run([str(p) for p in args],check=True,env=env)

source=root/'native1949'
spec=importlib.util.spec_from_file_location('shader_builder',source/'build_native1949.py')
module=importlib.util.module_from_spec(spec);sys.modules[spec.name]=module;spec.loader.exec_module(module)
module.shader_header(source,work)
prebuilt=pathlib.Path(os.environ.get('ULIKE_H9_PREBUILT',build/'gpu-finish1950-host'))
headers=prebuilt/'headers'
run(['cc','-std=c11','-O2','-fno-fast-math','-ffp-contract=off','-Wall',
    '-Wextra','-Werror','-DANDROID','-pthread','-I'+str(here),'-I'+str(headers),
    '-I'+str(work),source/'gpu1949.c',here/'test_gpu.c',
    '-l:libEGL.so.1','-l:libGL.so.1','-lm','-o',work/'h9h11-gpu-test'])
env=os.environ.copy();env.update(EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
run([work/'h9h11-gpu-test'],env)

classes=work/'classes';classes.mkdir(exist_ok=True)
sources=[root/name for name in ('QualityShadow1932.java','GpuInteger1949.java',
    'QualityPixels1932.java','NativeMoire1951.java','PolicyCache1945.java','NoiseCache1944.java',
    'NativeSpeed1944.java','NativeSpeed1935.java','SpatialNoise1934.java',
    'LongMoire1934.java','SpeedWorkers1935.java',
    'tests/noise-fixtures/com/hiro/ulike/QualityPipeline1932.java')]
run([java,'-m','jdk.compiler/com.sun.tools.javac.Main','-d',classes,*sources,
    here/'H9PolicyTest.java'])
run([java,'-cp',classes,'com.hiro.ulike.H9PolicyTest'])
run([sys.executable,here/'h10_host.py'])
