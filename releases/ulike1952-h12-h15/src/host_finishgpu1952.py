#!/usr/bin/env python3
"""Execute production H12/H13 GLES/JNI against pinned pre-GPU Java pixels.

The actual shader runs on Mesa surfaceless EGL; no fake backend is substituted.
Both combined baseline semantics and natural two-stage corrected-neighbor chains
are tested, including partial stripes, transparent pixels and custom mask wrap.
"""
from pathlib import Path
import argparse,hashlib,json,os,re,shutil,subprocess
import host_moire1951

def run(cmd,log,env=None,timeout=180):
 p=subprocess.run(list(map(str,cmd)),text=True,capture_output=True,env=env,timeout=timeout)
 Path(log).write_text(p.stdout+p.stderr)
 if p.returncode:raise RuntimeError(p.stdout[-2000:]+p.stderr[-12000:])
 return p.stdout

def test(root,work,jdk=None,ndk=None):
 root=Path(root).resolve();work=Path(work).resolve()/'host-finishgpu1952';work.mkdir(parents=True,exist_ok=True)
 baseline=root.parent.parent/'ulike1950-h1-h5'/'src'
 if not (baseline/'QualityPixels1932.java').is_file():raise RuntimeError('Pinned pre-GPU Java oracle absent')
 if jdk:
  os.environ['ULIKE_JAVAC']=str(Path(jdk)/'bin/javac')
 baseline_classes=work/'baseline-classes'
 host_moire1951.compile_java(baseline,baseline_classes,[root/'tests/MoireOracle1951.java',root/'tests/FinishSequenceOracle1952.java'])
 java=str(Path(jdk)/'bin/java') if jdk else shutil.which('java')
 for cls,filename in [('MoireOracle1951','combined.bin'),('FinishSequenceOracle1952','sequential.bin')]:
  run([java,'-cp',baseline_classes,'com.hiro.ulike.'+cls,work/filename],work/(cls+'.log'))
 java_info=subprocess.run([java,'-XshowSettings:properties','-version'],capture_output=True,text=True,check=True)
 home=re.search(r'^\s*java.home\s*=\s*(.+)$',java_info.stderr,re.M).group(1)
 include=Path(os.environ.get('ULIKE_JNI_INCLUDE',str(Path(jdk or home)/'include')))
 ndk=ndk or os.environ.get('ULIKE_NDK_HOME')
 graphics=(Path(ndk)/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include') if ndk else Path(os.environ.get('ULIKE_GPU_HEADERS','/workspace/scratch/d6f5652bff71/audit_h9/prebuilt/headers'))
 if not (graphics/'GLES3/gl31.h').is_file() or not (include/'jni.h').is_file():raise RuntimeError('JNI and GLES3.1 headers required')
 # Copy only graphics headers: Android libc/sysroot headers must not shadow the
 # host libc when executing the unchanged production translation unit on Mesa.
 host_headers=work/'headers';host_headers.mkdir(exist_ok=True)
 for folder in ['EGL','GLES3','KHR']:
  shutil.copytree(graphics/folder,host_headers/folder,dirs_exist_ok=True)
 graphics=host_headers
 import importlib.util
 spec=importlib.util.spec_from_file_location('finish_build1952',root/'finishgpu1952/build_finishgpu1952.py')
 builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder)
 builder.shader_header(root/'finishgpu1952',work)
 library=work/'libulike_finish1952.so'
 run(['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror',
  '-I'+str(include),'-I'+str(include/'linux'),'-I'+str(graphics),'-I'+str(work),
  root/'finishgpu1952/finish1952.c','-Wl,--no-undefined','-Wl,-l:libEGL.so.1','-Wl,-l:libGL.so.1','-lpthread','-o',library],work/'jni-build.log')
 current_classes=work/'current-classes'
 host_moire1951.compile_java(root,current_classes,[root/'GpuFinish1952.java',root/'tests/FinishGpu1952Test.java'])
 env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
 output=run([java,'-Xcheck:jni','-Djava.library.path='+str(work),'-cp',current_classes,'com.hiro.ulike.FinishGpu1952Test',
  work/'combined.bin',work/'sequential.bin'],work/'jni-run.log',env=env,timeout=240)
 if 'WARNING' in (work/'jni-run.log').read_text() or 'FATAL ERROR' in (work/'jni-run.log').read_text():raise AssertionError('JNI checker rejected GPU execution')
 matched=re.search(r'H12_JNI_PASS cases=(\d+) sequential=(\d+) pixels=(\d+) changed=(\d+)',output)
 if not matched:raise AssertionError('Production GLES/JNI endpoint did not execute: '+output)
 cases,sequences,pixels,changed=map(int,matched.groups())
 if cases<600 or sequences<200 or pixels<4000000 or changed<1000:raise AssertionError('Insufficient exact GPU coverage')
 result={'status':'passed','cases':cases,'sequential_chain_cases':sequences,'pixels_compared':pixels,
  'corrected_pixels':changed,'mesa_gles_shader_executed':True,'production_jni_executed':True,
  'gpu_chain_pixel_exact':True,'java_float_and_mask_policy_exact':True,
  'sequential_corrected_neighborhood_pixel_exact':True,'source_immutable':True,
  'dependency_36_row_halo_pixel_exact':True,'dependency_complete_tiles_executed':True,'concurrent_capture_workspace_exact':True,
  'partial_destination_rows_untouched':True,'candidate_failure_destination_unchanged':True,
  'interrupted_candidate_destination_unchanged':True,'one_final_readback_no_intermediate_cpu_wait':True,
  'pinned_java_oracle_version':'1.9.50','physical_android_tested':False,
  'full_float_beauty_chain_gpu_ported':False,
  'jni_result':output.strip(),'shader_sha256':hashlib.sha256((root/'finishgpu1952/finish1952.comp').read_bytes()).hexdigest()}
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n')
 return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--out',required=True);p.add_argument('--jdk');p.add_argument('--ndk')
 a=p.parse_args();print(json.dumps(test(Path(__file__).resolve().parent,a.out,a.jdk,a.ndk),indent=2))
