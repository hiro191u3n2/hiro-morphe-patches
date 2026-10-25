#!/usr/bin/env python3
"""Real current Stage/JNI/CPU proof for optional prepass and scratch lifetime.

Controlled test certificates prove selection wiring, not device speed. The one
fault build refuses only the new zero-upload scratch reservation. Numerical
shader, native pool and CPU oracle remain the exact current production files.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,shutil,subprocess,sys

def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or os.environ['JAVA_HOME']);jar=Path(os.environ['ULIKE_ANDROID_JAR'])
 header_names=('EGL/egl.h','GLES3/gl31.h','KHR/khrplatform.h')
 configured=os.environ.get('ULIKE_HOST_GLES_INCLUDE')
 headers=Path(configured) if configured else Path('/usr/include')
 if not all((headers/name).is_file() for name in header_names):
  if configured:raise AssertionError('ULIKE_HOST_GLES_INCLUDE must contain EGL, GLES3 and KHR headers')
  ndk_headers=Path(ndk or os.environ['ULIKE_NDK_HOME'])/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
  # Copy only graphics headers: adding the full Android sysroot would replace
  # host libc/stdint headers with incompatible Bionic definitions.
  headers=work/'ndk-gl-headers';headers.mkdir(exist_ok=True)
  for name in ('EGL','GLES3','KHR'):shutil.copytree(ndk_headers/name,headers/name,dirs_exist_ok=True)
  if not all((headers/name).is_file() for name in header_names):raise AssertionError('NDK graphics headers incomplete')
 sys.path.insert(0,str(source));import build1990
 prod=list(dict.fromkeys(build1990.compile_inputs([]).values()));local=source/'tests19115/stage'
 fixtures=[source/'tests1995/strong/StrongTransport1995Test.java',source/'tests1995/strong/GpuTransportFault1995.java',local/'RegionalStage19115Test.java']
 tracked=prod+fixtures+[local/'RegionalStageProbe19115.c',source/'native1960/engine1960.c',source/'native1958/smooth_noise1958.c',source/'native1958/scratch1959.h',Path(__file__)]+list((source/'native1960').glob('*.comp'))
 pins={str(p.relative_to(source)):sha(p) for p in tracked};commands=[]
 env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
 def run(label,args,timeout=300):
  args=list(map(str,args));commands.append({'label':label,'argv':args});r=subprocess.run(args,capture_output=True,text=True,env=env,timeout=timeout);out=r.stdout+r.stderr;(work/(label+'.log')).write_text(out)
  if r.returncode or 'WARNING in native method' in out or 'FATAL ERROR' in out:raise AssertionError(label+'\n'+out[-16000:])
  return out
 classes=work/'classes';classes.mkdir(exist_ok=True);native=work/'native';native.mkdir(exist_ok=True)
 run('java-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',jar,'-d',classes,*prod,*fixtures])
 spec=importlib.util.spec_from_file_location('stage115_native',source/'native1960/build_native1960.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder);builder.shader_header(source/'native1960',native)
 flags=['-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation']
 inc=['-I'+str(p) for p in [jdk/'include',jdk/'include/linux',headers,native,source/'native1960']]
 run('native-compile',['cc',*flags,'-DANDROID',*inc,local/'RegionalStageProbe19115.c','-Wl,--no-undefined','-l:libEGL.so.1','-l:libGL.so.1','-o',native/'libulike_gpu1960.so'])
 run('cpu-native-compile',['cc',*flags,*inc,source/'native1958/smooth_noise1958.c','-lm','-pthread','-o',native/'libulike_smooth1958.so'])
 cp=str(classes)+os.pathsep+str(jar);java=[jdk/'bin/java','-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx768m','-Djava.library.path='+str(native)]
 normal=json.loads(run('stage-normal',java+['-cp',cp,'com.hiro.ulike.RegionalStage19115Test']).split('RESULT ',1)[1])
 transport=(source/'GpuNoise1960.java').read_text();anchor='''            return reserveCapacity1983(slots,targetBytes,uploadBytes,javaReadbackBytes,true);'''
 if transport.count(anchor)!=1:raise AssertionError('Explicit capacity fault anchor changed')
 injected='''            if(Boolean.getBoolean("ulike.test.refuseRegional")&&slots!=null&&uploadBytes!=null)
                for(int i=0;i<slots.length;i++)if((slots[i]==12||slots[i]==22)&&uploadBytes[i]==0)return null;
'''
 decorated=work/'fault/GpuNoise1960.java';decorated.parent.mkdir(exist_ok=True)
 read='return readManyIntoNative(token,ticket.nativeTicket,slots,counts,targets,offsets);'
 if transport.count(read)!=1:raise AssertionError('Real readback revocation anchor changed')
 transport=transport.replace(anchor,injected+anchor).replace(read,'return RegionalStage19115Test.afterRead(readManyIntoNative(token,ticket.nativeTicket,slots,counts,targets,offsets));')
 decorated.write_text(transport)
 fault=work/'fault-classes';fault.mkdir(exist_ok=True);run('fault-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',fault,decorated])
 refused=json.loads(run('stage-refused',java+['-cp',str(fault)+os.pathsep+cp,'com.hiro.ulike.RegionalStage19115Test','refused']).split('RESULT ',1)[1])
 if normal['lifecycle_cases']!=6 or normal['cases']!=70 or refused['cases']!=2 or normal['revocation_cases']!=1 or refused['revocation_cases']!=1:raise AssertionError('Missing Stage/lifecycle coverage')
 engine=(source/'native1960/engine1960.c').read_text();close=engine[engine.index('JNIEXPORT jboolean JNICALL JNI1960(closeNative)'):]
 if close.index('if(!completed)')>close.index('for(int slot=12;slot<=22;slot+=10)'):raise AssertionError('Unknown completion must quarantine before selective release')
 if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Stage proof source changed during execution')
 result=dict(status='passed',assertions=normal['assertions']+refused['assertions']+1,normal=normal,refused=refused,source_sha256=pins,actual_jni=True,actual_native_cpu=True,actual_prepass_stage_selected=True,actual_new_certificate_rechecked_before_submit_and_commit=True,standard_route_no_optional_compile=True,required_prepass_fallback_forbidden=True,regional_scratch_released_after_completion=True,unknown_completion_quarantine_source_order_only=True,physical_android_tested=False,device_speedup_verified=False,test_certificate_scope='Controlled EXACT+FAST records test selection only; not speed evidence',fault_scope='Only new scratch reservation refusal and exact-certificate revocation after actual JNI readback injected',headers_origin=str(headers),test_probe_scope='Additional read-only JNI scalar observer; production engine included unchanged')
 (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
