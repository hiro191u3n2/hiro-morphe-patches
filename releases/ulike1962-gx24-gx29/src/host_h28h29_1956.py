#!/usr/bin/env python3
"""Fresh H28/H29 native differential, active single-image JNI and retained DEX tests.

The optional ARM gate uses actual aarch64 compiled NEON under qemu-user. Host
lane emulation is explicitly separate and is never described as ARM execution.
"""
from pathlib import Path
import argparse, hashlib, json, os, shutil, subprocess
ROOT=Path(__file__).resolve().parent

def run(command,log,timeout=240):
    p=subprocess.run(list(map(str,command)),capture_output=True,text=True,timeout=timeout)
    Path(log).write_text(p.stdout+p.stderr)
    if p.returncode:raise AssertionError('H28/H29 command failed: '+p.stdout[-2000:]+p.stderr[-9000:])
    return p.stdout

def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()

def adapters(root,out):
    t=root/'tests/h28h29'
    wrappers={
      'core': '''\nvoid {p}_core(const uint32_t*g,const uint32_t*v,uint32_t*o,int width,int rows,int noise,int horizontal,int texture,int shadows,int begin,int end,const int32_t*lr,const int32_t*cc){{
       uint32_t*d=calloc((size_t)width*rows,4),*vd=calloc((size_t)width*rows,4);if(!d||!vd)abort();
       for(int den=5120;den<=16384;den++)reciprocal[den]=(uint32_t)((1ull<<32)/(uint32_t)den);
       bilateral(g,v,o,width,rows,noise,horizontal,texture,shadows,begin,end,lr,cc,d,vd);free(d);free(vd);
      }}\n''',
      'single': '\nvoid {p}_transform(double*v,int n,int inverse){{double temp[64];transform(v,temp,n,inverse);}}\n',
    }
    result=[]
    for prefix in ('old','new'):
      for kind in ('core','single'):
        source=(t/('baseline_core1955.c' if kind=='core' else 'baseline_single_noise1955.c')
                if prefix=='old' else root/('native1950/core1950.c' if kind=='core' else 'native1955/single_noise1955.c'))
        text=source.read_text().replace('Java_com_hiro_ulike_',prefix+'_Java_com_hiro_ulike_')
        text='#include <stdlib.h>\n'+text+wrappers[kind].format(p=prefix)
        if prefix=='new' and kind=='core':text+='''\nvoid new_pair(const uint32_t*g,uint32_t*o,uint32_t*h,int width,int rows,int noise,int texture,int shadows,int begin,int end,const int32_t*lr,const int32_t*cc){
         uint32_t*d=calloc((size_t)width*rows,4),*vd=calloc((size_t)width*rows,4);if(!d||!vd)abort();
         bilateral_pair1956(g,o,h,width,rows,noise,texture,shadows,begin,end,lr,cc,d,vd);free(d);free(vd);
        }\n'''
        file=out/(prefix+'_'+kind+'.c');file.write_text(text);result.append(file)
    return result

def differential(root,out,compiler,jdk,arm=False,qemu=None):
    folder=out/('arm-neon' if arm else 'host-neon-model');folder.mkdir(exist_ok=True)
    generated=adapters(root,folder);objects=[]
    flags=['-O3','-std=c11','-Wall','-Wextra','-Werror','-fno-fast-math','-ffp-contract=off',
      '-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux')]
    for source in generated:
      obj=source.with_suffix('.o');extra=[]
      if not arm and source.name.startswith('new_'):
        extra=['-D__aarch64__','-I'+str(root/'tests/h28h29')]
      run([compiler,*flags,*extra,'-c',source,'-o',obj],source.with_suffix('.log'))
      objects.append(obj)
    exe=folder/'diff1956'
    run([compiler,*flags,*(['-static'] if arm else []),root/'tests/h28h29/diff1956.c',*objects,'-lm','-o',exe],folder/'link.log')
    result=json.loads(run([qemu,exe] if arm else [exe],folder/'test.log'))
    result.update(actual_arm_neon_executed=arm,host_intrinsic_lane_model=not arm,physical_android_tested=False)
    if arm:
      objdump=shutil.which('aarch64-linux-gnu-objdump')
      if not objdump:raise AssertionError('Actual ARM disassembly tool required')
      assembly=run([objdump,'-d',exe],folder/'instructions.log')
      import re
      if not re.search(r'fmul\s+v\d+\.2d',assembly) or not re.search(r'fadd\s+v\d+\.2d',assembly) or not re.search(r'mul\s+v\d+\.4s',assembly):
        raise AssertionError('Actual double/integer NEON arithmetic instructions missing')
      result['actual_neon_instruction_disassembly_verified']=True
    return result

def test(root,work,android=None):
    root=Path(root).resolve();out=Path(work).resolve()/'host-h28h29-1956';out.mkdir(parents=True,exist_ok=True)
    t=root/'tests/h28h29'
    for name,digest in json.loads((t/'pins.json').read_text()).items():
      if sha(t/name)!=digest:raise AssertionError('Pinned .55 algorithm oracle changed: '+name)
    executed=[root/'CorePixels1950.java',root/'native1950/core1950.c',root/'native1950/build_native1950.py',root/'native1955/single_noise1955.c',root/'native1955/build_native1955.py',Path(__file__).resolve(),*[p for p in t.glob('*') if p.is_file()]]
    starting_pins={p.relative_to(root).as_posix():sha(p) for p in executed}
    javac=Path(os.environ.get('ULIKE_JAVAC') or shutil.which('javac'))
    java=Path(os.environ.get('ULIKE_JAVA') or javac.with_name('java'));jdk=javac.resolve().parent.parent
    cc=os.environ.get('CC') or shutil.which('cc')
    reports={'host_neon_lane_differential':differential(root,out,cc,jdk)}
    cross=shutil.which('aarch64-linux-gnu-gcc');qemu=shutil.which('qemu-aarch64')
    if cross and qemu:reports['actual_arm_neon_differential']=differential(root,out,cross,jdk,True,qemu)
    elif os.environ.get('ULIKE_REQUIRE_ARM_NEON')=='1':raise AssertionError('Publication requires qemu-aarch64 and aarch64-linux-gnu-gcc for actual ARM NEON execution')
    else:reports['actual_arm_neon_differential']={'status':'not_tested','assertions':0,'actual_arm_neon_executed':False,'reason':'Cross compiler/qemu unavailable in local host; publication gate must execute in CI'}
    # Real production JNI uses the same new SIMD branch in the explicit host lane
    # model, then the unchanged Java .55 single-image algorithm checks full pixels.
    native=out/'native';native.mkdir(exist_ok=True)
    cflags=['-std=c11','-O3','-shared','-fPIC','-Wall','-Wextra','-Werror','-fno-fast-math','-ffp-contract=off',
      '-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-D__aarch64__','-I'+str(t)]
    for source,library in [('native1950/core1950.c','libulike_core1950.so'),('native1955/single_noise1955.c','libulike_nr1955.so')]:
      run([cc,*cflags,root/source,'-lm','-o',native/library],native/(library+'.log'))
    classes=out/'classes';classes.mkdir(exist_ok=True)
    tool=Path(os.environ['ULIKE_MORPHE_JAR'])
    sources=[root/'CorePixels1950.java',root/'h8gpu/H8DexOracle1951.java',t/'CpuPair1956Test.java',
      root/'tests/core1950/com/hiro/ulike/DetailPixels.java',root/'tests/core1950/com/hiro/ulike/SpeedWorkers1935.java',
      root/'h8gpu/fixtures/com/hiro/ulike/DetailSerial186.java',root/'SingleNoise1955.java',root/'tests/SingleNoise1955Test.java']
    run([javac,'-source','8','-target','8','-Xlint:-options','-cp',tool,'-d',classes,*sources],out/'java-compile.log')
    for key,main,args in [('cpu_pair_retained_filter','com.hiro.ulike.CpuPair1956Test',[t/'prepair-runtime.dex']),('single_image_simd_jni','com.hiro.ulike.SingleNoise1955Test',[])]:
      reports[key]=json.loads(run([java,'-Xcheck:jni','-Djava.library.path='+str(native),'-cp',str(classes)+os.pathsep+str(tool),main,*args],out/(key+'.log')))
    if not reports['cpu_pair_retained_filter'].get('cpu_only_pair_executed') or reports['single_image_simd_jni'].get('nativeParityCases')!=5:
      raise AssertionError('Actual CPU pair/filter and active single-image JNI evidence missing')
    # Refuse copied shared outputs before any writes, for both paired output roles.
    guard=out/'copy-guard'
    run([cc,'-std=c11','-O2','-Wall','-Wextra','-Werror','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),root/'native1950/core1950.c',t/'test_pair_copy1956.c','-o',guard],out/'copy-guard-compile.log')
    reports['pair_horizontal_copy_guard']=json.loads(run([guard],out/'horizontal-copy.log'))
    reports['pair_output_copy_guard']=json.loads(run([guard,'output'],out/'output-copy.log'))
    if starting_pins!={p.relative_to(root).as_posix():sha(p) for p in executed}:raise AssertionError('H28/H29 source changed during native compile/execution')
    production=[root/'CorePixels1950.java',root/'native1950/core1950.c',root/'native1955/single_noise1955.c']
    focused=[Path(__file__).resolve(),*t.glob('*')]
    result={'status':'passed','assertions':sum(x.get('assertions',0) for x in reports.values()),'suites':reports,
      'h28_core_6_neighbor_math_exact':True,'h28_active_single_image_double_simd_exact':True,
      'h29_cpu_horizontal_vertical_pair_exact':True,'h29_retained_packing_sharpening_executed':True,
      'h29_source_immutable':True,'h29_copied_shared_output_guard_executed':True,
      'actual_arm_neon_executed':reports['actual_arm_neon_differential']['actual_arm_neon_executed'],
      'single_frame_only':True,'pixel_equivalence_to_1955':True,'physical_android_tested':False,'device_speedup_verified':False,
      'source_sha256':starting_pins,'production_source_consistency_verified':True,
      'fixture_scope':'Real production C, retained filter/stage DEX and unchanged Java .55 NR oracle. Host NEON lane emulation is separate from real ARM qemu gate. No physical camera/encoding execution.'}
    (out/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);a=p.parse_args();print(json.dumps(test(ROOT,a.out)))
