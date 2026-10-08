#!/usr/bin/env python3
"""Build exact source-domain bilateral ARM64/NEON kernel with NDK r27c."""
from pathlib import Path
import argparse,hashlib,json,re,subprocess
NDK_REVISION='27.2.12479018'
def run(args):
 p=subprocess.run(list(map(str,args)),capture_output=True,text=True)
 if p.returncode:raise RuntimeError(p.stdout+p.stderr)
 return p.stdout+p.stderr
def build(ndk,output):
 source=Path(__file__).resolve().parent;ndk=Path(ndk).resolve();output=Path(output).resolve();output.mkdir(parents=True,exist_ok=True)
 if not re.search(r'^Pkg.Revision\s*=\s*'+re.escape(NDK_REVISION)+r'\s*$',(ndk/'source.properties').read_text(),re.M):raise RuntimeError('Pinned NDK revision required')
 tools=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin';cc=tools/'aarch64-linux-android26-clang';target=output/'libulike_core1950.so'
 flags=['-std=c11','-O3','-shared','-fPIC','-fvisibility=hidden','-fno-fast-math','-ffp-contract=off','-Wl,--no-undefined','-Wl,--build-id=sha1','-Wl,-z,max-page-size=16384','-Wl,-soname,libulike_core1950.so','-Wall','-Wextra','-Werror']
 run([cc,*flags,source/'core1950.c','-o',target]);elf=run([tools/'llvm-readelf','-h','-d','-Ws','--program-headers','--wide',target])
 exports=['Java_com_hiro_ulike_CorePixels1950_'+n for n in ['nativeAbi','bilateralNative','bilateralPairCpuAbi','bilateralPairCpuNative']]
 for n in exports:
  if n not in elf:raise RuntimeError('Missing JNI export '+n)
 needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',elf);alignment=[int(l.split()[-1],0) for l in elf.splitlines() if l.strip().startswith('LOAD ')]
 if 'AArch64' not in elf or any(n not in {'libc.so','libdl.so'} for n in needed) or not alignment or any(a<16384 for a in alignment):raise RuntimeError('Unexpected ELF architecture/dependencies/alignment')
 (output/'native1950-readelf.txt').write_text(elf)
 report={'schema':'ulike-native1950-v1','ndk_revision':NDK_REVISION,'target':'aarch64-linux-android26','clang_version':run([cc,'--version']).splitlines()[0], 'flags':flags,'file':target.name,'bytes':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'exports':exports,'needed_libraries':needed,'load_segment_alignments':alignment,'native_main_denoise':True,'neon_integer_guide_predecode':True,'h28_four_center_integer_bilateral':True,'h29_bounded_cpu_pair_shared_guide':True,'cpu_pair_native_abi':1956,'float_processing_added':False,'same_tap_order_rounding_opacity_texture_shadow_rules':True,'original_bytecode_fallback':True,'copied_output_rejected_before_writes':True,'disjoint_row_writes_preserved':True,'runtime_one_time_exact_selfcheck':True,'physical_android_tested':False,'sources':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in [source/'core1950.c',source/'build_native1950.py']}}
 (output/'native-build1950.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--ndk',required=True);p.add_argument('--output',required=True);a=p.parse_args();print(json.dumps(build(a.ndk,a.output)))
