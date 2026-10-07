#!/usr/bin/env python3
"""Reproducible Android ARM64 shared object. Requires Android NDK r27c exactly."""
import argparse, hashlib, json, os, pathlib, re, subprocess
NDK_REVISION='27.2.12479018'
def run(args):
    return subprocess.check_output([str(x) for x in args],text=True,stderr=subprocess.STDOUT)
def build(ndk,output):
    src=pathlib.Path(__file__).resolve().parent; ndk=pathlib.Path(ndk).resolve(); output=pathlib.Path(output).resolve(); output.mkdir(parents=True,exist_ok=True)
    props=(ndk/'source.properties').read_text()
    if not re.search(r'^Pkg.Revision\s*=\s*'+re.escape(NDK_REVISION)+r'\s*$',props,re.M):raise RuntimeError('NDK revision must be '+NDK_REVISION)
    bindir=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin'
    cc=bindir/'aarch64-linux-android26-clang'; target=output/'libulike_speed1935.so'
    flags=['-std=c11','-O3','-shared','-fPIC','-ffreestanding','-fno-builtin','-fno-stack-protector','-fvisibility=hidden','-fno-fast-math','-ffp-contract=off','-nostdlib','-Wl,--no-undefined','-Wl,--build-id=sha1','-Wl,-z,max-page-size=16384','-Wl,-soname,libulike_speed1935.so','-Werror','-Wall','-Wextra']
    cmd=[cc,*flags,src/'kernels1935.c',src/'jni1935.c','-o',target];run(cmd)
    elf=run([bindir/'llvm-readelf','-h','-d','-Ws','--program-headers','--wide',target]);asm=run([bindir/'llvm-objdump','-d',target])
    if 'AArch64' not in elf or '(NEEDED)' in elf:raise RuntimeError('Unexpected native target/dependency')
    alignments=[int(line.split()[-1],0) for line in elf.splitlines() if line.strip().startswith('LOAD ')]
    if not alignments or any(a<16384 for a in alignments):raise RuntimeError('ELF LOAD segment alignment below 16 KB')
    for n in ['nativeAbi','packNative','horizontalNative','verticalAddNative']:
        if 'Java_com_hiro_ulike_NativeSpeed1935_'+n not in elf:raise RuntimeError('Missing JNI export '+n)
    if not re.search(r'\bfmul\b',asm) or not re.search(r'\bfadd\b',asm) or re.search(r'\b(fmadd|fmla|fmls|fmsub)\b',asm):raise RuntimeError('Exact separate NEON multiplication/addition contract violated')
    if not re.search(r'\b(ld2|st2)\b',asm):raise RuntimeError('Missing YUV NEON packing instructions')
    (output/'native1935-readelf.txt').write_text(elf);(output/'native1935-disassembly.txt').write_text(asm)
    report={'schema':'ulike-native1935-v1','ndk_revision':NDK_REVISION,'clang_version':run([cc,'--version']).splitlines()[0],'target':'aarch64-linux-android26','flags':flags,'file':target.name,'bytes':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'neon_compiled':True,'fma_instructions':False,'needed_libraries':[],'load_segment_alignments':alignments,'physical_android_tested':False,'sources':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in [src/'kernels1935.c',src/'kernels1935.h',src/'jni1935.c']}}
    (output/'native-build1935.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
    return report
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--ndk',required=True);p.add_argument('--output',required=True);a=p.parse_args();build(a.ndk,a.output)
