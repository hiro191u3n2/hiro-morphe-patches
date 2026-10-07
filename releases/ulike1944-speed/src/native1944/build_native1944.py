#!/usr/bin/env python3
"""Build exact residual kernels and retained 1935 ABI using Android NDK r27c."""
import argparse, hashlib, json, pathlib, re, subprocess
NDK_REVISION='27.2.12479018'
def run(args):
    result=subprocess.run([str(x) for x in args],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    if result.returncode:raise RuntimeError(result.stdout)
    return result.stdout
def build(ndk,output):
    src=pathlib.Path(__file__).resolve().parent
    ndk=pathlib.Path(ndk).resolve(); output=pathlib.Path(output).resolve(); output.mkdir(parents=True,exist_ok=True)
    if not re.search(r'^Pkg.Revision\s*=\s*'+re.escape(NDK_REVISION)+r'\s*$',(ndk/'source.properties').read_text(),re.M):
        raise RuntimeError('NDK revision must be '+NDK_REVISION)
    bindir=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin'; cc=bindir/'aarch64-linux-android26-clang'
    target=output/'libulike_speed1935.so'
    flags=['-std=c11','-O3','-shared','-fPIC','-ffreestanding','-fno-builtin','-fno-stack-protector','-fvisibility=hidden',
        '-fno-fast-math','-ffp-contract=off','-nostdlib','-Wl,--no-undefined','-Wl,--build-id=sha1',
        '-Wl,-z,max-page-size=16384','-Wl,-soname,libulike_speed1935.so','-Werror','-Wall','-Wextra']
    files=[src/n for n in ['kernels1935.c','jni1935.c','residual1944.c','jni1944.c']]
    run([cc,*flags,*files,'-o',target])
    elf=run([bindir/'llvm-readelf','-h','-d','-Ws','--program-headers','--wide',target]); asm=run([bindir/'llvm-objdump','-d',target])
    if 'AArch64' not in elf or '(NEEDED)' in elf:raise RuntimeError('Unexpected native target/dependency')
    alignments=[int(line.split()[-1],0) for line in elf.splitlines() if line.strip().startswith('LOAD ')]
    if not alignments or any(a<16384 for a in alignments):raise RuntimeError('ELF LOAD alignment below16KB')
    exports=['Java_com_hiro_ulike_NativeSpeed1935_'+n for n in ['nativeAbi','packNative','horizontalNative','verticalAddNative']]
    exports+=['Java_com_hiro_ulike_NativeSpeed1944_'+n for n in ['nativeAbi','aggregateNative','verticalBatchNative']]
    for name in exports:
        if name not in elf:raise RuntimeError('Missing JNI export '+name)
    if re.search(r'\b(fmadd|fmla|fmls|fmsub)\b',asm):raise RuntimeError('Floating contraction violates retained1935exactness')
    (output/'native1944-readelf.txt').write_text(elf);(output/'native1944-disassembly.txt').write_text(asm)
    report={'schema':'ulike-native1944-v1','ndk_revision':NDK_REVISION,'clang_version':run([cc,'--version']).splitlines()[0],
        'target':'aarch64-linux-android26','flags':flags,'file':target.name,'bytes':target.stat().st_size,
        'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'exports':exports,'integer_residual_exact':True,
        'retained_1935_abi':True,'fma_instructions':False,'needed_libraries':[],
        'load_segment_alignments':alignments,'physical_android_tested':False,
        'sources':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in files+[src/'residual1944.h',src/'kernels1935.h']}}
    (output/'native-build1944.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report));return report
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--ndk',required=True);p.add_argument('--output',required=True)
    a=p.parse_args();build(a.ndk,a.output)
