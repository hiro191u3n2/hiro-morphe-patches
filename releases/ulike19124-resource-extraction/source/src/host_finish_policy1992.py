#!/usr/bin/env python3
"""Current immutable-mask CPU oracle versus exact resident GPU policy and full finishing."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,shutil,subprocess,sys
ROOT=Path(__file__).resolve().parent
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 sys.path.insert(0,str(source));import build1992
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
 android=Path(os.environ.get('ULIKE_ANDROID_JAR',str(source.parent/'tools/android.jar'))).resolve()
 if not android.is_file() or sha(android)!=build1992.TOOL_PINS['android.jar']:
  raise AssertionError('Pinned Android API jar required; set ULIKE_ANDROID_JAR to the verified tool path')
 classes=work/'classes';classes.mkdir(exist_ok=True)
 sources=list(dict.fromkeys(build1992.compile_inputs([]).values()));harness=source/'tests1992/policy/FinishPolicy1992Test.java'
 sources.append(source/'tests1960/Native1960Test.java')
 native=source/'native1960';fixtures=source/'tests/pipeline1942-fixtures'
 tracked=sources+[harness,source/'host_finish_policy1992.py',native/'build_native1960.py',native/'engine1960.c',source/'tests1960/fault1960.c',fixtures/'android/graphics/Bitmap.java',fixtures/'android/graphics/ColorSpace.java',fixtures/'com/hiro/ulike/HostAudit1932.java']+list(native.glob('*.comp'))
 pins={str(p.relative_to(source)):sha(p) for p in tracked};commands=[]
 environment=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
 def run(argv,name):
  argv=list(map(str,argv));commands.append(dict(name=name,argv=argv));r=subprocess.run(argv,capture_output=True,text=True,env=environment,timeout=300)
  output=r.stdout+r.stderr;(work/(name+'.log')).write_text(output)
  if r.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise RuntimeError(name+' failed:\n'+output[-10000:])
  return output
 run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',android,'-d',classes,*sources,harness],'production-compile')
 bitmap=work/'Bitmap.java';bitmap.write_text((fixtures/'android/graphics/Bitmap.java').read_text().replace('public final class Bitmap {','public final class Bitmap {public int getGenerationId(){return 1;}'))
 run([jdk/'bin/javac','--release','8','-cp',str(classes)+os.pathsep+str(android),'-d',classes,bitmap,fixtures/'android/graphics/ColorSpace.java',fixtures/'com/hiro/ulike/HostAudit1932.java'],'bitmap-fixture-compile')
 graphics=ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
 for name in ('EGL','GLES3','KHR'):shutil.copytree(graphics/name,work/'headers'/name,dirs_exist_ok=True)
 spec=importlib.util.spec_from_file_location('policy_native1992',native/'build_native1960.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder);builder.shader_header(native,work)
 library=work/'libulike_gpu1960.so'
 run(['cc','-std=c11','-O3','-shared','-fPIC','-DANDROID','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-I'+str(work/'headers'),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work),native/'engine1960.c',source/'tests1960/fault1960.c','-Wl,--wrap=glClientWaitSync','-Wl,--wrap=glUnmapBuffer','-Wl,--wrap=glDispatchCompute','-l:libEGL.so.1','-l:libGL.so.1','-o',library],'native-compile')
 output=run([jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m','-Djava.library.path='+str(work),'-cp',str(classes)+os.pathsep+str(android),'com.hiro.ulike.FinishPolicy1992Test'],'policy-finish-jni')
 result=json.loads(output.split('RESULT ',1)[1].strip())
 if result.get('status')!='passed' or result.get('policy_cases')!=24 or result.get('finish_cases')!=72 or result.get('compared_pixels',0)<100000 or result.get('resident_policy_trace_verified') is not True:raise AssertionError('Complete resident policy/finish evidence missing')
 if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Production sources changed during policy proof')
 result.update(source_sha256=pins,native_sha256=sha(library),android_jar_sha256=sha(android),gl_environment=output.split('ENV ',1)[1].splitlines()[0],original_double_mask_sampling=True,full_policy_equal=True,full_output_equal=True,native_failure_cleanup=True,mask_transfer_bytes_per_pixel=4)
 (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
