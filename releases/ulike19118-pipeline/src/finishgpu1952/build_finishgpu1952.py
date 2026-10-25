#!/usr/bin/env python3
"""Build independent ARM64 exact moire/sharp resident dependency chain."""
from pathlib import Path
import argparse,hashlib,json,re,subprocess
NDK_REVISION='27.2.12479018'
def run(args):
 p=subprocess.run(list(map(str,args)),capture_output=True,text=True)
 if p.returncode:raise RuntimeError(p.stdout+p.stderr)
 return p.stdout+p.stderr
def shader_header(root,output):
 shader=(root/'finish1952.comp').read_text()
 (output/'finish_source1952.h').write_text('/* Generated from exact finish1952.comp. */\nstatic const char finish1952_source[] =\n'+''.join(json.dumps(line+'\n')+'\n' for line in shader.splitlines())+';\n')
def build(ndk,output):
 root=Path(__file__).resolve().parent;ndk=Path(ndk).resolve();output=Path(output).resolve()
 output.mkdir(parents=True,exist_ok=True)
 if not re.search(r'^Pkg.Revision\s*=\s*'+re.escape(NDK_REVISION)+r'\s*$',(ndk/'source.properties').read_text(),re.M):raise RuntimeError('Pinned NDK r27c required')
 shader_header(root,output)
 tools=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin'
 cc=tools/'aarch64-linux-android26-clang';target=output/'libulike_finish1952.so'
 flags=['-std=c11','-O3','-shared','-fPIC','-fvisibility=hidden','-fno-fast-math','-ffp-contract=off',
  '-Wl,--no-undefined','-Wl,--build-id=sha1','-Wl,-z,max-page-size=16384',
  '-Wl,-soname,libulike_finish1952.so','-Wall','-Wextra','-Werror','-I'+str(output)]
 run([cc,*flags,root/'finish1952.c','-lEGL','-lGLESv3','-o',target])
 elf=run([tools/'llvm-readelf','-h','-d','-Ws','--program-headers','--wide',target])
 exports=['Java_com_hiro_ulike_GpuFinish1952_'+n for n in ['nativeAbi','finishNative']]
 if 'AArch64' not in elf or not all(s in elf for s in exports):raise RuntimeError('H12 ABI/export mismatch')
 needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',elf)
 if not {'libEGL.so','libGLESv3.so'}.issubset(needed) or any(s not in {'libEGL.so','libGLESv3.so','libc.so','libdl.so','libm.so'} for s in needed):raise RuntimeError('Unexpected H12 dependencies: '+repr(needed))
 aligns=[int(line.split()[-1],0) for line in elf.splitlines() if line.strip().startswith('LOAD ')]
 if not aligns or min(aligns)<16384:raise RuntimeError('H12 ELF LOAD alignment below 16 KB')
 (output/'finishgpu-readelf1952.txt').write_text(elf)
 files=[root/'finish1952.c',root/'finish1952.comp',root/'build_finishgpu1952.py']
 result={'schema':'ulike-finishgpu1952-v1','ndk_revision':NDK_REVISION,'target':'aarch64-linux-android26',
  'clang':run([cc,'--version']).splitlines()[0],'file':target.name,'bytes':target.stat().st_size,
  'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'exports':exports,'needed_libraries':needed,
  'load_segment_alignments':aligns,'integer_moire_longwave_sharp_gpu_chain':True,
  'single_source_upload_final_readback':True,'dependency_complete_36_row_halo':True,
  'gpu_tile_storage_barriers_without_intermediate_cpu_wait':True,
  'java_float_and_mask_policy_preserved':True,'whole_photo_admission_required':True,
  'full_float_beauty_chain_gpu_ported':False,'physical_android_tested':False,
  'sources':{f.name:hashlib.sha256(f.read_bytes()).hexdigest() for f in files}}
 (output/'finishgpu-build1952.json').write_text(json.dumps(result,indent=2,sort_keys=True)+'\n')
 return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--ndk',required=True);p.add_argument('--output',required=True)
 a=p.parse_args();print(json.dumps(build(a.ndk,a.output),indent=2))
