#!/usr/bin/env python3
"""Execute Strong admission against actual Java + unchanged JNI pools/GLES.

Independent memory pressure and verification context are controlled peers.
The production Strong/memory algorithms are compiled whole, never sliced.
These checks do not establish Android speed or image-quality certification.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, shutil, subprocess

FLAGS=('strong_admission_same_budget_retry19110','strong_admission_typed_refusal19110',
 'strong_admission_live_ownership19110','strong_admission_cancellation19110',
 'strong_admission_repeat_trial19110','strong_admission_actual_jni19110',
 'strong_admission_optional_report19110','strong_admission_original_budget19110')
def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']);jar=Path(os.environ.get('ULIKE_ANDROID_JAR',str(jdk.parent/'android.jar')))
 local=source/'tests19110/strong';tracked=[source/n for n in ['GpuStrong1960.java','GpuNoise1960.java','SpeedWorkers1935.java','StrongNoise1958.java','GpuChain1961.java','native1960/engine1960.c','native1958/scratch1959.h','h8gpu/h8_gpu.c','tests1989/memory/Memory1989Test.java','tests1989/memory/MemoryNative1989.c','capacity_inverse19110.py']]+list(local.glob('*'))+[Path(__file__).resolve()]
 before={str(p.relative_to(source)):sha(p) for p in tracked};commands=[]
 def run(label,args,env=None):
  args=list(map(str,args));commands.append({'label':label,'argv':args});r=subprocess.run(args,capture_output=True,text=True,timeout=240,env=env);output=r.stdout+r.stderr;(work/(label+'.log')).write_text(output)
  if r.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError(label+'\n'+output[-18000:])
  return r.stdout
 native=work/'native';native.mkdir(exist_ok=True)
 builder=module('strong110_native_builder',source/'native1960/build_native1960.py');builder.shader_header(source/'native1960',native)
 shader=(source/'h8gpu/bilateral1950.comp').read_text();(native/'bilateral_source1951.h').write_text('static const char bilateral1951_source[] =\n'+''.join(json.dumps(line+'\n')+'\n' for line in shader.splitlines())+';\n')
 headers=native/'headers';sysroot=ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
 for name in ('EGL','GLES3','KHR'):shutil.copytree(sysroot/name,headers/name,dirs_exist_ok=True)
 flags=['-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-DANDROID'];includes=['-I'+str(p) for p in (source,headers,jdk/'include',jdk/'include/linux',native)];link=['-Wl,--no-undefined','-l:libEGL.so.1','-l:libGL.so.1','-lm','-pthread']
 run('gpu-native-compile',['cc',*flags,*includes,source/'native1960/engine1960.c',*link,'-o',native/'libulike_gpu1960.so'])
 run('pools-native-compile',['cc',*flags,*includes,source/'tests1989/memory/MemoryNative1989.c',*link,'-o',native/'libmemory_native1989.so'])
 build=module('strong110_build',source/'build1990.py');production=work/'production';production.mkdir(exist_ok=True)
 run('production-compile',[jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-bootclasspath',jar,'-d',production,*build.compile_inputs([]).values()])
 classes=work/'classes';classes.mkdir(exist_ok=True);files=[source/'tests1989/memory/Memory1989Test.java',local/'StrongOwners19110.java',local/'StrongAdmission19110Test.java']
 run('fixtures-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[production,jar])),'-d',classes,*files])
 env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1');base=[jdk/'bin/java','-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xms64m','-Xmx768m','-Djava.library.path='+str(native),'-cp',os.pathsep.join(map(str,[classes,production,jar]))]
 executed=json.loads(run('strong-admission-native',[*base,'com.hiro.ulike.StrongAdmission19110Test'],env).strip().splitlines()[-1])
 proof=module('strong110_capacity_inverse',source/'capacity_inverse19110.py');inverse=proof.test(source)
 baseline=work/'published108';baseline.mkdir(exist_ok=True);baseline_sources=[];baseline_sha={}
 for name in ('GpuStrong1960.java','GpuNoise1960.java'):
  data=proof.restore(source,name);path=baseline/'src'/name;path.parent.mkdir(exist_ok=True);path.write_bytes(data);baseline_sources.append(path);baseline_sha[name]=hashlib.sha256(data).hexdigest()
 baseline_classes=baseline/'classes';baseline_classes.mkdir(exist_ok=True)
 run('published108-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[production,jar])),'-d',baseline_classes,*baseline_sources])
 baseline_command=[*base[:-1],os.pathsep.join(map(str,[classes,baseline_classes,production,jar])),'com.hiro.ulike.StrongAdmission19110Test']
 commands.append({'label':'published108-regression','argv':list(map(str,baseline_command))});old=subprocess.run(list(map(str,baseline_command)),capture_output=True,text=True,timeout=240,env=env);output=old.stdout+old.stderr;(work/'published108-regression.log').write_text(output)
 if old.returncode==0 or 'java.lang.AssertionError: UNUSED_NATIVE_CACHE_BLOCKS_STRONG_RESIDENT_ADMISSION trial 1' not in output or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError('Published .108 did not reproduce the specific admission failure: '+output[-8000:])
 regression=dict(status='passed',assertions=1,reproduced=True,source_sha256=baseline_sha)
 if before!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Strong source/fixtures changed during execution')
 evidence=dict(status='passed',assertions=executed['assertions']+inverse['assertions']+regression['assertions'],tests={'strong_native':executed,'original_budget':inverse,'published108_regression':regression},source_sha256=before,commands=commands,physical_android_tested=False,device_speedup_verified=False,pixel_equivalence_tested=False)
 for flag in FLAGS:
  if not any(t.get(flag) is True for t in evidence['tests'].values()):raise AssertionError('Missing executed Strong proof '+flag)
  evidence[flag]=True
 (work/'result.json').write_text(json.dumps(evidence,indent=2)+'\n');return evidence
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
