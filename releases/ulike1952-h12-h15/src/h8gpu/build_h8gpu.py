#!/usr/bin/env python3
"""Build the independent arm64 H8 two-pass integer GLES 3.1 backend."""
from pathlib import Path
import argparse,hashlib,json,re,subprocess
NDK_REVISION='27.2.12479018'

def run(args):
    p=subprocess.run(list(map(str,args)),capture_output=True,text=True)
    if p.returncode:raise RuntimeError(p.stdout+p.stderr)
    return p.stdout+p.stderr

def build(ndk,output):
    root=Path(__file__).resolve().parent
    ndk=Path(ndk).resolve();output=Path(output).resolve();output.mkdir(parents=True,exist_ok=True)
    if not re.search(r'^Pkg.Revision\s*=\s*'+re.escape(NDK_REVISION)+r'\s*$',(ndk/'source.properties').read_text(),re.M):
        raise RuntimeError('Pinned NDK r27c required')
    shader=(root/'bilateral1950.comp').read_text()
    (output/'bilateral_source1951.h').write_text('/* Generated from bilateral1950.comp. */\nstatic const char bilateral1951_source[] =\n'+''.join(json.dumps(line+'\n')+'\n' for line in shader.splitlines())+';\n')
    tools=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin'
    cc=tools/'aarch64-linux-android26-clang';target=output/'libulike_h8gpu1951.so'
    flags=['-std=c11','-O3','-shared','-fPIC','-fvisibility=hidden','-fno-fast-math','-ffp-contract=off',
           '-Wl,--no-undefined','-Wl,--build-id=sha1','-Wl,-z,max-page-size=16384',
           '-Wl,-soname,libulike_h8gpu1951.so','-Wall','-Wextra','-Werror','-I'+str(output)]
    run([cc,*flags,root/'h8_gpu.c','-lEGL','-lGLESv3','-o',target])
    elf=run([tools/'llvm-readelf','-h','-d','-Ws','--program-headers','--wide',target])
    exports=['Java_com_hiro_ulike_CorePixels1950_bilateralPairGpuAbi',
             'Java_com_hiro_ulike_CorePixels1950_bilateralPairGpuNative']
    if 'AArch64' not in elf or not all(s in elf for s in exports):raise RuntimeError('H8 ABI/export mismatch')
    needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',elf)
    if not {'libEGL.so','libGLESv3.so'}.issubset(needed) or any(s not in {'libEGL.so','libGLESv3.so','libc.so','libdl.so','libm.so'} for s in needed):raise RuntimeError('Unexpected H8 dependencies: '+repr(needed))
    aligns=[int(line.split()[-1],0) for line in elf.splitlines() if line.strip().startswith('LOAD ')]
    if not aligns or min(aligns)<16384:raise RuntimeError('H8 ELF LOAD alignment below 16 KB')
    (output/'h8gpu-readelf.txt').write_text(elf)
    files=[root/'h8_gpu.c',root/'bilateral1950.comp',root/'build_h8gpu.py']
    result={'schema':'ulike-h8gpu1951-v1','ndk_revision':NDK_REVISION,'target':'aarch64-linux-android26',
        'clang':run([cc,'--version']).splitlines()[0],'file':target.name,'bytes':target.stat().st_size,
        'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'exports':exports,'needed_libraries':needed,
        'load_segment_alignments':aligns,'integer_gpu_horizontal_vertical_with_intermediate_gpu_resident':True,
        'cpu_and_camera_egl_untouched_on_admission_failure':True,'physical_android_tested':False,
        'sources':{f.relative_to(root).as_posix():hashlib.sha256(f.read_bytes()).hexdigest() for f in files}}
    (output/'h8gpu-build.json').write_text(json.dumps(result,indent=2,sort_keys=True)+'\n')
    return result

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--ndk',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();print(json.dumps(build(a.ndk,a.output),indent=2))
