"""GX26 real JNI/GL input and direct readback tests, including ownership faults."""
from pathlib import Path
import importlib.util,json,os,re,shutil,subprocess

def test(root,work,jdk,ndk=None):
 root=Path(root).resolve();work=Path(work).resolve()/'transfer1962';work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk);ndk=Path(ndk or os.environ['ULIKE_NDK_HOME'])
 def run(args,name,env=None):
  p=subprocess.run(list(map(str,args)),text=True,capture_output=True,env=env,timeout=120)
  (work/(name+'.log')).write_text(p.stdout+p.stderr)
  if p.returncode:raise RuntimeError(name+'\n'+p.stdout[-5000:]+p.stderr[-5000:])
  return p.stdout
 headers=work/'headers';headers.mkdir(exist_ok=True)
 for name in ('EGL','GLES3','KHR'):shutil.copytree(ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'/name,headers/name,dirs_exist_ok=True)
 spec=importlib.util.spec_from_file_location('transfer_builder',root/'native1960/build_native1960.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder);builder.shader_header(root/'native1960',work)
 run(['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-DANDROID','-I'+str(headers),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work),root/'native1960/engine1960.c',root/'native1960/residual_blocks1961.c',root/'tests1960/fault1960.c','-Wl,--wrap=glClientWaitSync','-Wl,--wrap=glUnmapBuffer','-Wl,--wrap=glDispatchCompute','-Wl,--no-undefined','-l:libEGL.so.1','-l:libGL.so.1','-lm','-o',work/'libulike_gpu1960.so'],'native')
 from host_gpu1962 import all_sources
 classes=work/'classes';classes.mkdir(exist_ok=True)
 run([jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-d',classes,*all_sources(root),root/'tests1960/Native1960Test.java',root/'tests1962/Transfer1962Test.java'],'java')
 env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1');reports={}
 for mode in ('','upload','unmap','timeout'):
  output=run([jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.Transfer1962Test',*([mode] if mode else [])],'run-'+(mode or 'normal'),env)
  if 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError('JNI checker rejected '+mode)
  match=re.search(r'^RESULT (\{[^\n]+\})$',output,re.M)
  if not match:raise AssertionError('No executed transfer report '+mode)
  reports[mode or 'normal']=json.loads(match.group(1))
 return {'status':'passed','assertions':sum(x['assertions'] for x in reports.values()),'mapped_transfer_executed':True,'direct_readback_executed':True,'ownership_faults_executed':True,'reports':reports,'device_speedup_verified':False}
