#!/usr/bin/env python3
"""Exercise H32's unmodified production JNI unit with Mesa and allocation/failure probes."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, random, re, shutil, struct, subprocess


def run(command, log, env=None):
    result=subprocess.run(list(map(str,command)),capture_output=True,text=True,env=env,timeout=240)
    Path(log).write_text(result.stdout+result.stderr)
    if result.returncode:raise RuntimeError(result.stdout[-3000:]+result.stderr[-10000:])
    return result.stdout


def test(root,work,jdk=None,ndk=None):
    root=Path(root).resolve();work=Path(work).resolve()/'host-h32-1956';work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);ndk=Path(ndk or os.environ['ULIKE_NDK_HOME'])
    headers=work/'headers';headers.mkdir(exist_ok=True)
    graphics=ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
    for folder in ('EGL','GLES3','KHR'):shutil.copytree(graphics/folder,headers/folder,dirs_exist_ok=True)
    shader=(root/'h8gpu/bilateral1950.comp').read_text()
    (work/'bilateral_source1951.h').write_text('static const char bilateral1951_source[] =\n'+''.join(json.dumps(line+'\n')+'\n' for line in shader.splitlines())+';\n')
    run(['cc','-D__GBM__','-std=c11','-O3','-shared','-fPIC','-Wall','-Wextra','-Werror','-fno-fast-math','-ffp-contract=off',
         '-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(headers),'-I'+str(work),'-I'+str(root/'h8gpu'),
         root/'tests/h32/H32Host1956.c','-Wl,--no-undefined','-Wl,-l:libEGL.so.1','-Wl,-l:libGL.so.1','-lpthread','-o',work/'libh32host1956.so'],work/'native-build.log')
    classes=work/'classes';classes.mkdir(exist_ok=True)
    run([jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-d',classes,root/'tests/h32/CorePixels1950.java'],work/'java-build.log')
    spec=importlib.util.spec_from_file_location('h32_oracle',root/'h8gpu/test_bilateral1950.py')
    oracle=importlib.util.module_from_spec(spec);spec.loader.exec_module(oracle)
    # Formula independently retained since .50; never obtains expected pixels
    # from the new native staging or GLES dispatch implementation.
    rng=random.Random(195632)
    dimensions=[(7,5)]+[(rng.randrange(1,68),rng.randrange(1,53)) for _ in range(64)]+[(71,67)]
    fixtures=[]
    for index,(width,rows) in enumerate(dimensions):
        noise=1+index%4;texture=bool(index&1);shadows=bool(index&2)
        begin=0 if index%3==0 else rng.randrange(rows)
        end=rows if index%3==0 else rng.randrange(begin+1,rows+1)
        if index==len(dimensions)-1:begin,end=0,rows
        source=[(0xff000000 if p%23 else 0x7f000000)|rng.randrange(1<<24) for p in range(width*rows)]
        lr=[rng.randrange(257) for _ in range(1280)];cc=[rng.randrange(257) for _ in range(1280)]
        first=max(0,begin-3);last=min(rows,end+3)
        middle=oracle.reference(source,source,width,rows,noise,True,texture,shadows,first,last,lr,cc)
        expected=oracle.reference(source,middle,width,rows,noise,False,texture,shadows,begin,end,lr,cc)
        halo=[0xa5a5a5a5]*(width*rows)
        halo[first*width:begin*width]=middle[first*width:begin*width]
        halo[end*width:last*width]=middle[end*width:last*width]
        fixtures.append([width,rows,noise,int(texture),int(shadows),begin,end]+source+lr+cc+expected+halo)
    fixture_path=work/'fixtures.bin'
    with fixture_path.open('wb') as stream:
        stream.write(struct.pack('>I',len(fixtures)))
        for fixture in fixtures:stream.write(struct.pack('>'+'I'*len(fixture),*fixture))
    env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
    output=run([jdk/'bin/java','-Xcheck:jni','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.CorePixels1950',fixture_path],work/'jni-run.log',env)
    if 'WARNING' in output or 'FATAL ERROR' in output:raise AssertionError('JNI check failed: '+output)
    match=re.search(r'H32_PASS assertions=(\d+) cases=(\d+) pixels=(\d+) concurrent=(\d+)',output)
    if not match:raise AssertionError('Production JNI suite did not execute: '+output)
    assertions,cases,pixels,concurrent=map(int,match.groups())
    result={'status':'passed','assertions':assertions,'cases':cases,'pixels_compared':pixels,'concurrent_calls':concurrent,
        'production_jni_mesa_executed':True,'same_and_smaller_request_reuse_without_allocation':True,
        'growth_allocation_once':True,'source_and_alias_protection':True,'success_before_output_commit':True,
        'late_unmap_failure_no_partial_commit':True,'allocation_failure_cpu_fallback':True,
        'concurrent_exclusive_leases':True,'active_scratch_accounted':True,'trim_during_gpu_wait_nonblocking':True,
        'trim_during_lease_deferred_until_release':True,'idle_expiry_seconds':30,'retention_cap_bytes':32*1024*1024,
        'oversize_one_shot_accounted_then_freed':True,'physical_android_tested':False,
        'source_sha256':hashlib.sha256((root/'h8gpu/h8_gpu.c').read_bytes()).hexdigest(),
        'oracle_sha256':hashlib.sha256((root/'h8gpu/test_bilateral1950.py').read_bytes()).hexdigest()}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--root',type=Path,default=Path(__file__).parent)
    parser.add_argument('--work',type=Path,required=True);parser.add_argument('--jdk',type=Path);parser.add_argument('--ndk',type=Path)
    args=parser.parse_args();print(json.dumps(test(args.root,args.work,args.jdk,args.ndk),indent=2))
