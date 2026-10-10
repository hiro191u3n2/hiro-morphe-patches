#!/usr/bin/env python3
"""Execute original .75 and current native source through actual JNI."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess

def sha(p): return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
 frozen=source/'tests1976/colour-cache-reference'
 for name,digest in json.loads((frozen/'pins.json').read_text()).items():
  if sha(frozen/name)!=digest: raise AssertionError('Frozen .75 CPU baseline changed: '+name)
 production=[source/name for name in ('native1955/single_noise1955.c','native1960/residual_blocks1961.c','ColourCache1976.java','SingleNoise1955.java','SingleResidual1961.java')]
 before={p.relative_to(source).as_posix():sha(p) for p in production}
 def run(cmd,name):
  p=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,timeout=180)
  (work/(name+'.log')).write_text(p.stdout+p.stderr)
  if p.returncode or 'WARNING in native method' in p.stdout+p.stderr or 'FATAL ERROR' in p.stdout+p.stderr:raise AssertionError(name+': '+p.stdout[-3000:]+p.stderr[-8000:])
  return p.stdout
 paths=[]
 for prefix,root in [('old',frozen),('new',source)]:
  single=(root/'native1955/single_noise1955.c').read_text()
  residual=(root/'native1960/residual_blocks1961.c').read_text().split('static int export_block1961',1)[1]
  text=single+'\nstatic int export_block1961'+residual
  for a,b in [('SingleNoise1955_nativeAbi',prefix+'Abi'),('SingleNoise1955_processNative',prefix+'Process'),('SingleResidual1961_prepareDirectNative',prefix+'Direct'),('SingleResidual1961_prepareNative',prefix+'Prepare')]:
   text=text.replace('Java_com_hiro_ulike_'+a,'Java_com_hiro_ulike_ColourCache1976Test_'+b)
  text=text.replace('static double luma(uint32_t p) {','static double luma(uint32_t p) { colour_count1976++;')
  if prefix=='new':
   # The direct baseline differential explicitly forces the candidate, while
   # the production scoped JNI wrappers still override false/true per call.
   text=text.replace('static _Thread_local int colour_enabled1976;','static _Thread_local int colour_enabled1976 = 1;')
   text=text.replace('int previous=colour_enabled1976;colour_enabled1976=cached?1:0;','native_gate1976(cached);int previous=colour_enabled1976;colour_enabled1976=cached?1:0;')
   text=text.replace('colour_enabled1976=previous;return result;', 'if(gate_mode1976==3&&cached&&result){jint bad=0x87654321;(*env)->SetIntArrayRegion(env,output_array,begin*width,1,&bad);}'+'colour_enabled1976=previous;return result;',1)
  pre='''#include <stdlib.h>\n#include <jni.h>\nstatic _Thread_local long colour_count1976;\nstatic _Thread_local int fail_after1976,allocations1976;\nstatic void *cache_malloc1976(size_t n){return fail_after1976&&++allocations1976==fail_after1976?NULL:malloc(n);}\nstatic void *cache_calloc1976(size_t n,size_t size){return fail_after1976&&++allocations1976==fail_after1976?NULL:calloc(n,size);}\n#define malloc cache_malloc1976\n#define calloc cache_calloc1976\n'''
  post='''\nJNIEXPORT jlong JNICALL Java_com_hiro_ulike_ColourCache1976Test_%sCount(JNIEnv *e,jclass c){(void)e;(void)c;long n=colour_count1976;colour_count1976=0;return n;}\nJNIEXPORT void JNICALL Java_com_hiro_ulike_ColourCache1976Test_%sFault(JNIEnv *e,jclass c,jint n){(void)e;(void)c;fail_after1976=n;allocations1976=0;}\n'''%(prefix,prefix)
  if prefix=='new':
   pre='#define _POSIX_C_SOURCE 200809L\n'+pre+'\n#include <time.h>\nstatic int gate_mode1976;static long gate_old1976,gate_new1976;\nstatic void native_gate1976(int cached){if(cached)gate_new1976++;else gate_old1976++;if((gate_mode1976==1&&!cached)||(gate_mode1976==2&&cached)){struct timespec t={0,6000000};nanosleep(&t,NULL);}}\n'
   post+='\nJNIEXPORT void JNICALL Java_com_hiro_ulike_ColourGate1976Test_mode(JNIEnv*e,jclass c,jint mode){(void)e;(void)c;gate_mode1976=mode;}\nJNIEXPORT jlong JNICALL Java_com_hiro_ulike_ColourGate1976Test_calls(JNIEnv*e,jclass c,jboolean cached){(void)e;(void)c;long n=cached?gate_new1976:gate_old1976;if(cached)gate_new1976=0;else gate_old1976=0;return n;}\n'
  path=work/(prefix+'.c');path.write_text(pre+text+post);paths.append(path)
 flags=['-std=c11','-O3','-shared','-fPIC','-Wall','-Wextra','-Werror','-fno-fast-math','-ffp-contract=off','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux')]
 library=work/'libcolourcache1976.so';run(['cc',*flags,*paths,'-lm','-o',library],'native-compile')
 classes=work/'classes';classes.mkdir(exist_ok=True);fixture=source/'tests1976/ColourCache1976Test.java'
 gate_fixtures=list((source/'tests1976/colour-gate-fixtures').rglob('*.java'))
 run([jdk/'bin/javac','--release','8','-d',classes,fixture,source/'ColourCache1976.java',*gate_fixtures],'java-compile')
 report=json.loads(run([jdk/'bin/java','-ea','-Xcheck:jni','-Xmx512m','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.ColourCache1976Test'],'differential').strip())
 if before!={p.relative_to(source).as_posix():sha(p) for p in production}:raise AssertionError('Production source changed during colour tests')
 gate=json.loads(run([jdk/'bin/java','-ea','-Xcheck:jni','-Xmx512m','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.ColourGate1976Test'],'runtime-admission').strip())
 report['runtime_admission']=gate;report['assertions']+=gate['assertions']
 cross,qemu=shutil.which('aarch64-linux-gnu-gcc'),shutil.which('qemu-aarch64')
 if cross and qemu:
  arm=work/'colour-cache-arm1976'
  armflags=[x for x in flags if x not in ('-shared','-fPIC')]
  run([cross,*armflags,'-static',*paths,source/'tests1976/colour_cache_arm1976.c','-lm','-o',arm],'arm-compile')
  armreport=json.loads(run([qemu,arm],'arm-differential').strip())
  report['actual_arm_neon']=armreport;report['assertions']+=armreport['assertions'];report['actual_arm_neon_executed']=True
 elif os.environ.get('ULIKE_REQUIRE_ARM_NEON')=='1':raise AssertionError('ARM64 compiler and qemu required for publish gate')
 else:report['actual_arm_neon_executed']=False
 if before!={p.relative_to(source).as_posix():sha(p) for p in production}:raise AssertionError('Production source changed during ARM differential')
 report.update(production_source_sha256=before,test_fixture_sha256={fixture.name:sha(fixture),'colour_cache_arm1976.c':sha(source/'tests1976/colour_cache_arm1976.c'),**{p.relative_to(source).as_posix():sha(p) for p in gate_fixtures}},baseline75_source_sha256=json.loads((frozen/'pins.json').read_text()),runner_sha256=sha(__file__),native_library_sha256=sha(library),fixed_colour_cache_bytes=4224,device_speedup_verified=False)
 (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
 import argparse
 p=argparse.ArgumentParser();p.add_argument('--out',required=True);p.add_argument('--jdk');a=p.parse_args()
 print(json.dumps(test(Path(__file__).parent,a.out,a.jdk),indent=2))
