#!/usr/bin/env python3
"""Resident Finish recovery: actual Java + JNI pools/GLES ownership and scoped graph.

The competing memory owners are controlled pressure seams, never substitute
admission algorithms. Graph transport is explicitly mocked in a separate JVM.
No result asserts physical Android timing, GPU equivalence or certification.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, shutil, subprocess

FLAGS=('resident_memory_same_budget_retry19108','resident_memory_active_gpu_preserved19108',
 'resident_memory_busy_cpu_preserved19108','resident_memory_cancellation_preserved19108',
 'resident_memory_original_paths_preserved19108','resident_memory_actual_jni19108',
 'resident_memory_bounded_report19108','resident_memory_typed_refusal19108','resident_memory_carried_scope19108')

def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);ndk=Path(ndk or os.environ['ULIKE_NDK_HOME'])
 jar=Path(os.environ.get('ULIKE_ANDROID_JAR',str(jdk.parent/'android.jar')))
 local=source/'tests19108/memory';pins=json.loads((local/'unchanged-native107.json').read_text())
 for name,digest in pins.items():
  if sha(source/name)!=digest:raise AssertionError('Original budget/native ownership changed: '+name)
 tracked=[source/n for n in ['FinishMemory1989.java','GpuChain1961.java','GpuNoise1960.java','SpeedWorkers1935.java']]+list(local.glob('*'))+[Path(__file__).resolve()]
 before={str(p.relative_to(source)):sha(p) for p in tracked};commands=[]
 def run(label,args,env=None):
  args=list(map(str,args));commands.append({'label':label,'argv':args})
  r=subprocess.run(args,capture_output=True,text=True,timeout=240,env=env);output=r.stdout+r.stderr;(work/(label+'.log')).write_text(output)
  if r.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError(label+'\n'+output[-18000:])
  return r.stdout
 def result(label,args,env=None):return json.loads(run(label,args,env).strip().splitlines()[-1])
 native=work/'native';native.mkdir(exist_ok=True);classes=native/'classes';classes.mkdir(exist_ok=True)
 builder=module('memory108_native_builder',source/'native1960/build_native1960.py');builder.shader_header(source/'native1960',native)
 shader=(source/'h8gpu/bilateral1950.comp').read_text()
 (native/'bilateral_source1951.h').write_text('static const char bilateral1951_source[] =\n'+''.join(json.dumps(line+'\n')+'\n' for line in shader.splitlines())+';\n')
 headers=native/'headers';sysroot=ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
 for name in ('EGL','GLES3','KHR'):shutil.copytree(sysroot/name,headers/name,dirs_exist_ok=True)
 flags=['-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-DANDROID']
 includes=['-I'+str(p) for p in (source,headers,jdk/'include',jdk/'include/linux',native)];link=['-Wl,--no-undefined','-l:libEGL.so.1','-l:libGL.so.1','-lm','-pthread']
 run('gpu-native-compile',['cc',*flags,*includes,source/'native1960/engine1960.c',*link,'-o',native/'libulike_gpu1960.so'])
 run('pools-native-compile',['cc',*flags,*includes,source/'tests1989/memory/MemoryNative1989.c',*link,'-o',native/'libmemory_native1989.so'])
 files=[source/n for n in ['GpuNoise1960.java','SpeedWorkers1935.java','FinishMemory1989.java','tests1989/memory/Memory1989Test.java','tests1997/memory/ForegroundMemory1997Test.java']]+[local/'MemoryOwners19108.java',local/'ResidentMemory19108Test.java']
 run('native-java-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*files])
 env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
 base=[jdk/'bin/java','-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xms64m','-Xmx768m','-Djava.library.path='+str(native),'-cp',classes]
 tests={}
 for label,main in [('original_background','Memory1989Test'),('original_foreground','ForegroundMemory1997Test'),('resident_native','ResidentMemory19108Test')]:
  tests[label]=result(label,[*base,'com.hiro.ulike.'+main],env)
 # Compile the entire current production closure unchanged. The separate graph
 # JVM overlays only named Android/transport/pressure peers; no source slicing.
 build=module('memory108_build',source/'build1990.py');production=work/'production';production.mkdir(exist_ok=True)
 run('production-compile',[jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-bootclasspath',jar,'-d',production,*build.compile_inputs([]).values()])
 fixtures=module('memory108_graph_fixtures',source/'host_qualification1981.py');graph=work/'graph';graph.mkdir(exist_ok=True);graphfiles=[]
 for name,body in fixtures.FIXTURES.items():
  if name.endswith('QueueHost1967.java'):continue
  if name.endswith('QueueFixtures1967.java'):body='package com.hiro.ulike;\nfinal class SaveQueue1935 {static volatile boolean idle;static boolean idle1953(){return idle;}}\nfinal class WholeRoute1953 {static long retainedBytes(){return 0;}}'
  p=graph/'fixture-src'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);graphfiles.append(p)
 graphfiles += [source/n for n in ['tests/pipeline1942-fixtures/android/graphics/Bitmap.java','tests/pipeline1942-fixtures/android/graphics/ColorSpace.java','tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java','tests1986/finish_baseline/FinishControl1986.java','tests1992/finish_memory/FinishStagePeers1992.java','tests1992/finish_memory/FinishMemory1992Test.java']]+[local/'ResidentFinish19108Test.java']
 graphclasses=graph/'classes';graphclasses.mkdir(exist_ok=True)
 run('graph-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[production,jar])),'-d',graphclasses,*graphfiles])
 tests['resident_graph']=result('resident-graph',[jdk/'bin/java','-ea','-Xmx768m','-cp',os.pathsep.join(map(str,[graphclasses,production,jar])),'com.hiro.ulike.ResidentFinish19108Test'])
 if before!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Memory source/fixtures changed during execution')
 evidence=dict(status='passed',assertions=sum(t['assertions'] for t in tests.values()),tests=tests,source_sha256=before,unchanged_native107_sha256=pins,commands=commands,physical_android_tested=False,device_speedup_verified=False)
 for flag in FLAGS:
  if not any(t.get(flag) is True for t in tests.values()):raise AssertionError('Missing executed memory proof '+flag)
  evidence[flag]=True
 (work/'result.json').write_text(json.dumps(evidence,indent=2)+'\n');return evidence

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
