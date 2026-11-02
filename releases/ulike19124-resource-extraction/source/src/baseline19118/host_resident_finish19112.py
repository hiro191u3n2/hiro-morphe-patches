#!/usr/bin/env python3
"""Real128 carried-source finishing, profile isolation and phase-owned budgets.
Only budget observations/capture interruption are injected in fault copies;
production pixels, GPU commands, native ownership and exact gates are executed.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,shutil,subprocess,sys
ROOT=Path(__file__).resolve().parent

def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True);sys.path.insert(0,str(source));import build1992
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
 android=Path(os.environ['ULIKE_ANDROID_JAR']).resolve()
 if sha(android)!=build1992.TOOL_PINS['android.jar']:raise AssertionError('Pinned Android API jar required')
 classes=work/'classes';classes.mkdir(exist_ok=True);faults=work/'fault-source';faults.mkdir(exist_ok=True)
 sources=list(dict.fromkeys(build1992.compile_inputs([]).values()))
 harness=source/'tests19112/finish/ResidentFinish19112Test.java';peer=source/'tests1992/policy/FinishPolicy1992Test.java'
 sources.extend([source/'tests1960/Native1960Test.java',peer,harness])
 native=source/'native1960';fixtures=source/'tests/pipeline1942-fixtures'
 tracked=sources+[Path(__file__).resolve(),native/'build_native1960.py',native/'engine1960.c',source/'tests1960/fault1960.c',fixtures/'android/graphics/Bitmap.java',fixtures/'android/graphics/ColorSpace.java',fixtures/'com/hiro/ulike/HostAudit1932.java']+list(native.glob('*.comp'))
 pins={str(p.relative_to(source)):sha(p) for p in tracked};commands=[];environment=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
 def run(argv,name):
  argv=list(map(str,argv));commands.append(dict(name=name,argv=argv));r=subprocess.run(argv,capture_output=True,text=True,env=environment,timeout=300)
  output=r.stdout+r.stderr;(work/(name+'.log')).write_text(output)
  if r.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise RuntimeError(name+' failed:\n'+output[-14000:])
  return output
 # Build genuine current bodies first, so faults cannot hide a compile error.
 run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',android,'-d',classes,*sources],'production-compile')
 bitmap=work/'Bitmap.java';bitmap.write_text((fixtures/'android/graphics/Bitmap.java').read_text().replace('public final class Bitmap {','public final class Bitmap {public int getGenerationId(){return 1;}'))
 run([jdk/'bin/javac','--release','8','-cp',str(classes)+os.pathsep+str(android),'-d',classes,bitmap,fixtures/'android/graphics/ColorSpace.java',fixtures/'com/hiro/ulike/HostAudit1932.java'],'bitmap-fixture-compile')
 replacements={
 'GpuNoise1960.java':('private static long physicalBudget1971(boolean includeReservations){','private static long physicalBudget1971(boolean includeReservations){\n        if(ResidentFinish19112Test.refuseBudget)return 0;'),
 'GpuChain1961.java':('''        finishStage1988(8);
        checkCancelled();''','''        finishStage1988(8);
        ResidentFinish19112Test.finishBoundary();
        checkCancelled();''')}
 for name,(old,new) in replacements.items():
  raw=(source/name).read_text()
  if raw.count(old)!=1:raise AssertionError('Exact fault hook missing '+name)
  (faults/name).write_text(raw.replace(old,new))
 run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',str(classes)+os.pathsep+str(android),'-d',classes,*[faults/name for name in replacements]],'fault-budget-compile')
 graphics=ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
 for name in ('EGL','GLES3','KHR'):shutil.copytree(graphics/name,work/'headers'/name,dirs_exist_ok=True)
 spec=importlib.util.spec_from_file_location('resident_finish_native112',native/'build_native1960.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder);builder.shader_header(native,work)
 library=work/'libulike_gpu1960.so'
 run(['cc','-std=c11','-O3','-shared','-fPIC','-DANDROID','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-I'+str(work/'headers'),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work),native/'engine1960.c',source/'tests1960/fault1960.c','-Wl,--wrap=glClientWaitSync','-Wl,--wrap=glUnmapBuffer','-Wl,--wrap=glDispatchCompute','-l:libEGL.so.1','-l:libGL.so.1','-o',library],'native-compile')
 output=run([jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m','-Djava.library.path='+str(work),'-cp',str(classes)+os.pathsep+str(android),'com.hiro.ulike.ResidentFinish19112Test'],'resident-finish-jni')
 result=json.loads(output.split('RESULT ',1)[1].strip())
 if result.get('status')!='passed' or result.get('cases')!=17 or result.get('compared_pixels',0)<1000000:raise AssertionError('Complete resident128 pixel proof missing')
 # Parallel owners may edit other classes while initially developing. The
 # frozen CI source must still match every compiled dependency exactly.
 if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Production sources changed during resident128 proof')
 import resident_finish_inverse19112
 inverse=resident_finish_inverse19112.test(source)
 result['source_inverse']=inverse;result['assertions']+=inverse['assertions'];result['resident_finish_source_inverse19112']=True
 import host_resident_finish_profile19112
 profile=host_resident_finish_profile19112.test(source,work/'profile',jdk,ndk)
 result['profile']=profile;result['assertions']+=profile['assertions']
 for flag in ('foreground_profile_toctou_rejected','faulty_guard_removal_reproduces_128','stable_profiles_preserved','preflight_closed_on_all_paths'):
  result[flag]=profile[flag]
 result.update(source_sha256=pins,native_sha256=sha(library),android_jar_sha256=sha(android),fault_scope='Budget0 observation and capture interruption at existing finish boundary only; real shader pixels/session/lease code unchanged')
 (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
