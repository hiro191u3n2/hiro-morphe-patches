#!/usr/bin/env python3
"""H30 exact residual no-op skipping vs byte-pinned published .55.

The active NR1-NR4 .55 pipeline remains untouched. This gate covers the retained
residual fallback, including actual Java/JNI, scalar C and Mesa compute; ARM
NEON is an additional required CI gate when ULIKE_REQUIRE_ARM_NEON=1.
Counter probes are added only to generated test copies, never shipped sources.
"""
from pathlib import Path
import argparse,ctypes as C,hashlib,importlib.util,json,os,random,shutil,subprocess

def run(args,log,env=None,timeout=240):
 p=subprocess.run(list(map(str,args)),text=True,capture_output=True,env=env,timeout=timeout)
 Path(log).write_text(p.stdout+p.stderr)
 if p.returncode:raise RuntimeError(p.stdout[-2000:]+p.stderr[-8000:])
 return p.stdout

def instrument_java(text):
 text=text.replace('public final class QualityShadow1932 {','public final class QualityShadow1932 {\n    public static long H30_TAPS,H30_SKIPPED;')
 marker='                    for(int xi=0;xi<taps;xi++) {'
 assert text.count(marker)==1
 text=text.replace(marker,marker+'H30_TAPS++;')
 text=text.replace('if(periodic==256)continue;','if(periodic==256){H30_SKIPPED++;continue;}')
 return text

def instrument_c(text,baseline=False):
 # Tap-lane counts represent actual scalar/NEON loop iterations, not inferred
 # skip opportunities. Four SIMD lanes count as four even when partly masked.
 prefix='h30_base_taps' if baseline else 'h30_taps'
 text='#include <stdint.h>\nuint64_t '+prefix+'=0;\n'+('' if baseline else 'uint64_t h30_skipped=0;\n')+text
 split=text.index('static void neon_pixels(')
 scalar=text[:split].replace('for(int xi=0;xi<taps;xi++){','for(int xi=0;xi<taps;xi++){'+prefix+'++;')
 rest=text[split:].replace('for(int xi=0;xi<taps;xi++){','for(int xi=0;xi<taps;xi++){'+prefix+'+=4;',1)
 text=scalar+rest
 if baseline:
  text=text.replace('residual1944_aggregate','residual1955_baseline').replace('residual1944_valid','residual1955_valid')
 else:
  text=text.replace('if(periodic==256)return;','if(periodic==256){h30_skipped++;return;}')
  text=text.replace('if(periodic[lane]==256)live[lane]=0;','if(periodic[lane]==256){h30_skipped++;live[lane]=0;}')
 return text

def gpu_test(root,ref,work):
 spec=importlib.util.spec_from_file_location('h30_egl',root/'native1949/host_gpu1949.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
 gpu=module.SoftwareGpu(root/'native1949');shader=root/'native1949/residual_finish1949.comp'
 gpu.programs.append(gpu.compile('residual_finish1949'));current=3
 gpu.source=work
 (work/'baseline.comp').write_bytes((ref/'native1949/residual_finish1949.comp').read_bytes())
 gpu.programs.append(gpu.compile('baseline'));old=4
 for name,path in [('oldcounts',ref/'native1949/residual_finish1949.comp'),('newcounts',shader)]:
  s=path.read_text();marker='        for(int xi=0;xi<taps;xi++) {';assert s.count(marker)==1
  s=s.replace(marker,marker+'atomicAdd(destination.values[width*(end-begin)],1u);')
  s=s.replace('if(periodic==256)return;','if(periodic==256){atomicAdd(destination.values[width*(end-begin)+1],1u);return;}')
  (work/(name+'.comp')).write_text(s);gpu.programs.append(gpu.compile(name))
 rng=random.Random(1956);pixels=cases=changed=base_taps=new_taps=skipped=0
 ranges=[0 if not sigma else 256//(1+d//4) for sigma in range(33) for d in range(256)]
 def execute(program,src,meta,seed,w,begin,end,lo,hi,radius,seed_input):
  origin=max(lo,begin-radius);bottom=min(hi,end+radius);compact=src[origin*w:bottom*w]
  arr=lambda x:(C.c_int32*len(x))(*x)
  n=w*(end-begin);policy=[256|(256<<9)]*n
  gpu.upload(0,arr(compact));gpu.upload(1,None,len(compact)*4);gpu.upload(2,arr(meta+policy));gpu.upload(3,arr(seed+[0,0]));gpu.upload(4,arr(ranges))
  gpu.use(1,dict(count=len(compact)));gpu.functions['glDispatchCompute']((len(compact)+63)//64,1,1);gpu.functions['glMemoryBarrier'](0x2000)
  gpu.use(program,dict(width=w,begin=begin,end=end,lo=lo,hi=hi,radius=radius,inputOrigin=origin,inputRows=bottom-origin,
      noise=4,shadows=1,globalBudget=256,beauty=128,smoothLimit=22,packedPolicy=1,seedFromInput=seed_input))
  gpu.functions['glDispatchCompute']((w+7)//8,(end-begin+7)//8,1);gpu.fence();return gpu.read(3,n+2)
 try:
  for w,h in [(1,1),(7,9),(17,19),(33,31),(65,49)]:
   for radius in [1,2,3,4]:
    for mode in range(7):
     lo=1 if h>12 else 0;hi=h-1 if h>12 else h
     begin=lo+(hi-lo)//3 if mode==6 else lo;end=hi-(hi-lo)//4 if mode==6 else hi
     src=[]
     for row in range(h):
      for col in range(w):
       v=(38 if (col+row)%2 else 68) if mode in [1,2] else 127 if mode==4 else 40+rng.randrange(21)
       if mode==2:v+=rng.randrange(3)
       alpha=128 if mode==3 and col%4==0 else 0 if mode==5 else 255
       src.append(alpha<<24|v*0x010101)
     n=w*(end-begin);meta=[]
     for i in range(n):
      p=src[begin*w+i];v=(77*((p>>16)&255)+150*((p>>8)&255)+29*(p&255)+128)>>8
      meta.append((0x80000000 if i%17 else 0)|16|(8<<6)|(7<<12)|(v<<18))
     if mode==4:meta=[0]*n # no-noise policy leaves caller seed untouched
     seed=[rng.getrandbits(32) for _ in range(n)]
     for seed_input in [0,1]:
      expected=execute(old,src,meta,seed,w,begin,end,lo,hi,radius,seed_input)
      actual=execute(current,src,meta,seed,w,begin,end,lo,hi,radius,seed_input)
      if expected!=actual:raise AssertionError('Mesa residual final pixels differ '+str((w,h,radius,mode,seed_input)))
      seeded=src[begin*w:end*w] if seed_input else seed
      changed+=sum((a&0xffffffff)!=(b&0xffffffff) for a,b in zip(actual[:n],seeded));pixels+=n;cases+=1
      if w==65 and mode in [0,1,2,3,4]:
       ac=execute(6,src,meta,seed,w,begin,end,lo,hi,radius,seed_input);bc=execute(5,src,meta,seed,w,begin,end,lo,hi,radius,seed_input)
       if ac[:n]!=actual[:n] or bc[:n]!=expected[:n]:raise AssertionError('Counter instrumentation affected pixels')
       base_taps+=bc[n];new_taps+=ac[n];skipped+=ac[n+1]
  if not(changed>0 and skipped>0 and new_taps<base_taps):raise AssertionError('Mesa fixtures lack exact skipped work')
  return dict(status='passed',cases=cases,pixels=pixels,changed=changed,baselineTaps=base_taps,candidateTaps=new_taps,skipped=skipped,renderer=gpu.renderer,production_shader_executed=True,barrier_tails_and_seed_modes=True)
 finally:gpu.close()

def test(root,work,baseline=None,jdk=None,ndk=None):
 root=Path(root).resolve();work=Path(work).resolve()/'host-h30-1956';work.mkdir(parents=True,exist_ok=True)
 ref=Path(baseline).resolve() if baseline else root/'tests/published1955-h30'
 pins=json.loads((root/'tests/published1955-h30/pins.json').read_text())
 for name,digest in pins.items():
  if hashlib.sha256((ref/name).read_bytes()).hexdigest()!=digest:raise AssertionError('Published .55 pin differs: '+name)
 java=Path(jdk)/'bin/java' if jdk else Path(os.environ.get('ULIKE_JAVA') or shutil.which('java'))
 javac=Path(jdk)/'bin/javac' if jdk else Path(os.environ.get('ULIKE_JAVAC') or shutil.which('javac'))
 if not jdk:jdk=javac.resolve().parent.parent
 jdk=Path(jdk);gen=work/'generated';gen.mkdir(exist_ok=True);classes=work/'classes';classes.mkdir(exist_ok=True)
 new=gen/'QualityShadow1932.java';new.write_text(instrument_java((root/'QualityShadow1932.java').read_text()))
 old=gen/'QualityShadowH30Oracle.java';s=instrument_java((ref/'QualityShadow1932.java').read_text());s=s.replace('QualityShadow1932','QualityShadowH30Oracle').replace('if(NativeSpeed1944.available()) {','if(false) {');old.write_text(s)
 worker=gen/'SpeedWorkers1935.java';s=(ref/'SpeedWorkers1935.java').read_text();s=s.replace('public static int[] borrowInts(int length) {','public static int[] borrowInts(int length) { if(Boolean.getBoolean("h30.oom"))throw new OutOfMemoryError("test workspace");');worker.write_text(s)
 names=[n for n in pins if n.endswith('.java') and n not in ['QualityShadow1932.java','SpeedWorkers1935.java']]
 run([javac,'-source','8','-target','8','-Xlint:-options','-d',classes,new,old,worker,*[ref/n for n in names],root/'tests/H30Residual1956Test.java'],work/'java-compile.log')
 native=root/'native1944';lib=work/'native';lib.mkdir(exist_ok=True)
 run(['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),
     native/'kernels1935.c',native/'jni1935.c',native/'residual1944.c',native/'jni1944.c','-o',lib/'libulike_speed1935.so'],work/'jni-compile.log')
 variants={}
 for label,opts in [('java-cache',[]),('java-uncached',['-Dh30.oom=true']),('actual-native-jni',['-Xcheck:jni','-Djava.library.path='+str(lib)])]:
  output=run([java,*opts,'-cp',classes,'com.hiro.ulike.H30Residual1956Test'],work/(label+'.log'))
  variants[label]=json.loads(output)
  if label=='actual-native-jni' and not variants[label]['jniExecuted']:raise AssertionError('Native JNI route did not execute')
 # Native standalone harness exercises bit30 opt-in and retained non-opt ABI.
 oldc=gen/'baseline.c';oldc.write_text(instrument_c((ref/'native1944/residual1944.c').read_text(),True))
 newc=gen/'candidate.c';newc.write_text(instrument_c((native/'residual1944.c').read_text()))
 harness=root/'tests/H30Native1956Test.c';flags=['-std=c11','-O3','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-I'+str(native)]
 run(['cc',*flags,oldc,newc,harness,'-o',work/'native-test'],work/'native-compile.log')
 scalar=json.loads(run([work/'native-test'],work/'native-test.log'))
 arm=None;cross=shutil.which('aarch64-linux-gnu-gcc');qemu=shutil.which('qemu-aarch64')
 if cross and qemu:
  run([cross,*flags,'-static',oldc,newc,harness,'-o',work/'arm-test'],work/'arm-compile.log')
  arm=json.loads(run([qemu,work/'arm-test'],work/'arm-test.log'));assert arm['neon']
 elif any(os.environ.get(n)=='1' for n in ['ULIKE_REQUIRE_ARM_NEON','ULIKE_ARM_QEMU_REQUIRED']):raise RuntimeError('Required ARM NEON gate needs qemu-aarch64 and aarch64-linux-gnu-gcc')
 gpu=gpu_test(root,ref,work)
 subreports=[*variants.values(),scalar,gpu]+([arm] if arm is not None else [])
 for report in subreports:
  if report.get('status')!='passed' or not isinstance(report.get('cases'),int) or report['cases']<=0:
   raise AssertionError('H30 differential subreport incomplete')
 assertions=sum(report['cases'] for report in subreports)
 result=dict(status='passed',assertions=assertions,assertion_count_scope='completed exact differential cases; conservative lower bound',reference='published 1.9.55 byte-pinned sources',java=variants,native_scalar=scalar,arm_neon=arm,arm_neon_executed=arm is not None,mesa=gpu,
     exact_noop_skip=True,retained_aggregate_abi_exact=True,scope='retained residual fallback only; active NR1-NR4 route unchanged',physical_android_tested=False,device_speed_measured=False,baseline_pins=pins)
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--out',required=True);p.add_argument('--baseline');p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args()
 print(json.dumps(test(Path(__file__).resolve().parent,a.out,a.baseline,a.jdk,a.ndk),indent=2))
