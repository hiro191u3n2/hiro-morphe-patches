#!/usr/bin/env python3
"""Build exact ARM64 moire and final sharpening; Java supplies exact integer policy."""
from pathlib import Path
import argparse,hashlib,json,re,subprocess
NDK_REVISION='27.2.12479018'
def run(args):
    process=subprocess.run(list(map(str,args)),capture_output=True,text=True)
    if process.returncode:raise RuntimeError(process.stdout+process.stderr)
    return process.stdout+process.stderr
def build(ndk,output):
    source=Path(__file__).resolve().parent;ndk=Path(ndk).resolve();output=Path(output).resolve()
    output.mkdir(parents=True,exist_ok=True)
    if not re.search(r'^Pkg.Revision\s*=\s*'+re.escape(NDK_REVISION)+r'\s*$',
                     (ndk/'source.properties').read_text(),re.M):
        raise RuntimeError('Pinned NDK revision required')
    tools=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin'
    cc=tools/'aarch64-linux-android26-clang'
    target=output/'libulike_moire1951.so'
    flags=['-std=c11','-O3','-shared','-fPIC','-fvisibility=hidden',
           '-fno-fast-math','-ffp-contract=off','-Wl,--no-undefined',
           '-Wl,--build-id=sha1','-Wl,-z,max-page-size=16384',
           '-Wl,-soname,libulike_moire1951.so','-Wall','-Wextra','-Werror']
    run([cc,*flags,source/'moire1951.c','-o',target])
    elf=run([tools/'llvm-readelf','-h','-d','-Ws','--program-headers','--wide',target])
    exports=['Java_com_hiro_ulike_NativeMoire1951_'+name
             for name in ['nativeAbi','finishStripNative']]
    if any(name not in elf for name in exports):
        raise RuntimeError('Missing JNI export')
    needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',elf)
    alignments=[int(line.split()[-1],0) for line in elf.splitlines()
                if line.strip().startswith('LOAD ')]
    if ('AArch64' not in elf or
        any(name not in {'libc.so','libdl.so'} for name in needed) or
        not alignments or any(a<16384 for a in alignments)):
        raise RuntimeError('Unexpected ELF architecture/dependencies/alignment')
    (output/'native1951-readelf.txt').write_text(elf)
    report={'schema':'ulike-native1951-v1','ndk_revision':NDK_REVISION,
            'target':'aarch64-linux-android26','file':target.name,
            'bytes':target.stat().st_size,
            'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),
            'exports':exports,'needed_libraries':needed,
            'load_segment_alignments':alignments,'native_moire_and_long_wave':True,'native_final_sharpening':True,
            'java_float_and_mask_policy_preserved':True,'jni_abi':19512,
            'integer_only':True,'source_image_immutable':True,
            'original_java_fallback':True,'runtime_pixel_exact_selfcheck':True,
            'disjoint_row_writes_and_copied_output_rejection':True,
            'physical_android_tested':False,
            'sources':{p.name:hashlib.sha256(p.read_bytes()).hexdigest()
                       for p in [source/'moire1951.c',source/'build_native1951.py']}}
    (output/'native-build1951.json').write_text(json.dumps(report,indent=2)+'\n')
    return report
if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--ndk',required=True)
    parser.add_argument('--output',required=True)
    args=parser.parse_args()
    print(json.dumps(build(args.ndk,args.output)))
