#!/usr/bin/env python3
"""Build independent ARM64 exact two-slot photo GPU-session backend."""
from pathlib import Path
import argparse,hashlib,json,re,subprocess
NDK_REVISION='27.2.12479018'
def run(args):
 p=subprocess.run(list(map(str,args)),capture_output=True,text=True)
 if p.returncode:raise RuntimeError(p.stdout+p.stderr)
 return p.stdout+p.stderr
def shader_header(root,output):
 root=Path(root);output=Path(output)
 files=[root/'finish1953.c',root/'finish1953.comp',root/'build_finishgpu1953.py']
 identity=hashlib.sha256(b''.join(f.name.encode()+b'\0'+f.read_bytes()+b'\0' for f in files)).hexdigest()
 shader=(root/'finish1953.comp').read_text()
 (output/'finish_source1953.h').write_text('/* Generated exact GLSL and reviewed source identity. */\n#define FINISH1953_SOURCE_ID '+json.dumps(identity)+'\nstatic const char finish1953_source[] =\n'+''.join(json.dumps(line+'\n')+'\n' for line in shader.splitlines())+';\n')
 return identity
def build(ndk,output):
 root=Path(__file__).resolve().parent;ndk=Path(ndk).resolve();output=Path(output).resolve()
 output.mkdir(parents=True,exist_ok=True)
 if not re.search(r'^Pkg.Revision\s*=\s*'+re.escape(NDK_REVISION)+r'\s*$',(ndk/'source.properties').read_text(),re.M):raise RuntimeError('Pinned NDK r27c required')
 identity=shader_header(root,output)
 tools=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin'
 cc=tools/'aarch64-linux-android26-clang';target=output/'libulike_finish1953.so'
 flags=['-std=c11','-O3','-shared','-fPIC','-fvisibility=hidden','-fno-fast-math','-ffp-contract=off',
  '-Wl,--no-undefined','-Wl,--build-id=sha1','-Wl,-z,max-page-size=16384',
  '-Wl,-soname,libulike_finish1953.so','-Wall','-Wextra','-Werror','-I'+str(output)]
 run([cc,*flags,root/'finish1953.c','-lEGL','-lGLESv3','-o',target])
 elf=run([tools/'llvm-readelf','-h','-d','-Ws','--program-headers','--wide',target])
 methods=['nativeAbi','retainedNative','warmupNative','environmentNative','openNative','coreNative','haloNative',
  'submitNative','collectNative','statsNative','closeNative']
 exports=['Java_com_hiro_ulike_GpuFinish1953_'+n for n in methods]
 if 'AArch64' not in elf or not all(s in elf for s in exports):raise RuntimeError('H16 session ABI/export mismatch')
 needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',elf)
 if not {'libEGL.so','libGLESv3.so'}.issubset(needed) or any(s not in {'libEGL.so','libGLESv3.so','libc.so','libdl.so','libm.so'} for s in needed):raise RuntimeError('Unexpected .53 dependencies: '+repr(needed))
 aligns=[int(line.split()[-1],0) for line in elf.splitlines() if line.strip().startswith('LOAD ')]
 if not aligns or min(aligns)<16384:raise RuntimeError('.53 ELF LOAD alignment below16KB')
 (output/'finishgpu-readelf1953.txt').write_text(elf)
 files=[root/'finish1953.c',root/'finish1953.comp',root/'build_finishgpu1953.py']
 result={'schema':'ulike-finishgpu1953-v1','ndk_revision':NDK_REVISION,'target':'aarch64-linux-android26',
  'clang':run([cc,'--version']).splitlines()[0],'file':target.name,'bytes':target.stat().st_size,
  'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'exports':exports,'needed_libraries':needed,
  'load_segment_alignments':aligns,'jni_abi':19531,'reviewed_source_identity':identity,
  'private_gpu_owner_retained_context':True,'photo_token_scoped_source_halo_copy':True,
  'two_bounded_slots_submit_fence_without_completion_wait':True,'close_drains_before_pool_reuse':True,
  'lossless_raw4_packed2_constant4_policy':True,'adaptive_256_512_core_rows':True,
  'immutable_original_and_output_disjoint':True,'integer_moire_longwave_sharp_gpu_chain':True,
  'cached_actual_environment_no_foreground_fingerprint_setup':True,
  'explicit_native_memory_reserved_before_growth':True,
  'dependency_complete_36_row_halo':True,'java_float_and_mask_policy_preserved':True,
  'whole_photo_admission_required':True,'full_float_beauty_chain_gpu_ported':False,
  'physical_android_tested':False,'sources':{f.name:hashlib.sha256(f.read_bytes()).hexdigest() for f in files}}
 (output/'finishgpu-build1953.json').write_text(json.dumps(result,indent=2,sort_keys=True)+'\n')
 return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--ndk',required=True);p.add_argument('--output',required=True)
 a=p.parse_args();print(json.dumps(build(a.ndk,a.output),indent=2))
