#!/usr/bin/env python3
"""Execute production GPU JNI recovery on Mesa, never a simulated GPU backend."""
import importlib.util,json,os,pathlib,re,shutil,subprocess

def run(command,env=None):
 p=subprocess.run([str(x) for x in command],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,env=env)
 if p.returncode:raise RuntimeError(p.stdout)
 return p.stdout

def test(root,work,jdk,ndk):
 root=pathlib.Path(root).resolve();work=pathlib.Path(work).resolve();jdk=pathlib.Path(jdk).resolve();ndk=pathlib.Path(ndk).resolve();work.mkdir(parents=True,exist_ok=True)
 headers=work/'headers';headers.mkdir(exist_ok=True)
 for name in ('EGL','GLES3','KHR'):shutil.copytree(ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'/name,headers/name,dirs_exist_ok=True)
 spec=importlib.util.spec_from_file_location('native_builder1965',root/'native1960/build_native1960.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder);builder.shader_header(root/'native1960',work)
 compile_command=['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-DANDROID','-I'+str(headers),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work),root/'native1960/engine1960.c',root/'tests1965/native_fault1965.c','-Wl,--wrap=glClientWaitSync','-Wl,--wrap=glUnmapBuffer','-Wl,--wrap=glDispatchCompute','-Wl,--no-undefined','-l:libEGL.so.1','-l:libGL.so.1','-lm','-o',work/'libulike_gpu1960.so']
 run(compile_command)
 fixtures=work/'fixtures';fixtures.mkdir(exist_ok=True);package=fixtures/'com/hiro/ulike';package.mkdir(parents=True,exist_ok=True)
 for name,method in [('WholeRoute1953','retainedBytes'),('GpuFinish1953','retainedBytes'),('SpeedWorkers1935','nativeRetainedBytes1956')]:
  (package/(name+'.java')).write_text('package com.hiro.ulike; final class '+name+' {static long '+method+'(){return 0;}}\n')
 android=fixtures/'android/content';android.mkdir(parents=True,exist_ok=True);(android/'Context.java').write_text('package android.content; import java.io.File; public class Context{private final File directory;public Context(File f){directory=f;f.mkdirs();}public Context getApplicationContext(){return this;}public File getFilesDir(){return directory;}}\n')
 classes=work/'classes';classes.mkdir(exist_ok=True)
 run([jdk/'bin/javac','-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-d',classes,*sorted(fixtures.rglob('*.java')),root/'GpuNoise1960.java',root/'GpuRequired1965.java',root/'GpuRequiredFailure1965.java',root/'tests1965/RequiredNative1965Test.java'])
 env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1');reports={}
 for name in ('recovery','allfail','quarantine','stale','fp64','soft64','forbidden','logs'):
  command=[jdk/'bin/java','-Xcheck:jni','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.RequiredNative1965Test',name]
  if name=='logs':command.append(work/'logs')
  output=run(command,env);(work/(name+'.log')).write_text(output)
  if 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError('JNI checker rejected '+name+': '+output)
  found=re.search(r'^RESULT (\{[^\n]+\})$',output,re.M)
  if not found:raise AssertionError('Executed result missing '+name)
  report=json.loads(found.group(1));assert report['status']=='passed' and report['assertions']>1;reports[name]=report
 android_target=work/'android-platform-target';android_target.mkdir(exist_ok=True)
 android_compile=[*compile_command[:-1],android_target/'libulike_gpu1960.so','-D__ANDROID__'];run(android_compile)
 name='android-software-driver';command=[jdk/'bin/java','-Xcheck:jni','-Djava.library.path='+str(android_target),'-cp',classes,'com.hiro.ulike.RequiredNative1965Test',name];output=run(command,env);(work/(name+'.log')).write_text(output)
 if 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError('JNI checker rejected Android software-driver guard: '+output)
 found=re.search(r'^RESULT (\{[^\n]+\})$',output,re.M)
 if not found:raise AssertionError('Executed Android-target software-driver result missing')
 android_report=json.loads(found.group(1));assert android_report['status']=='passed' and android_report['actualDispatches']==0 and android_report['assertions']>=10;reports[name]=android_report
 report={'status':'passed','assertions':sum(v['assertions'] for v in reports.values()),'actualProductionJNI':True,'actualMesaCompute':True,'physicalAndroidTested':False,'android_software_driver_refused':True,'android_guard_actual_jni_no_dispatch':True,'platformBoundary':'Desktop arithmetic QA compiles the unchanged production shader/JNI source for the host; a separate JNI library explicitly compiled with the NDK predefined __ANDROID__ macro rejects actual llvmpipe before any image buffers/compute work. Production Android has no runtime bypass.','tests':reports}
 report['android_guard_driver_evidence']=next(line[len('ANDROID_GUARD '):] for line in output.splitlines() if line.startswith('ANDROID_GUARD '))
 (work/'required-native1965.json').write_text(json.dumps(report,indent=2)+'\n');return report

if __name__=='__main__':
 import argparse
 parser=argparse.ArgumentParser();parser.add_argument('--root',type=pathlib.Path,default=pathlib.Path(__file__).resolve().parents[1]);parser.add_argument('--work',type=pathlib.Path,required=True);parser.add_argument('--jdk',type=pathlib.Path,required=True);parser.add_argument('--ndk',type=pathlib.Path,required=True);args=parser.parse_args();print(json.dumps(test(args.root,args.work,args.jdk,args.ndk),indent=2))
