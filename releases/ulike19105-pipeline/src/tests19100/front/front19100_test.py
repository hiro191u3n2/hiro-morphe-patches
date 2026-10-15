#!/usr/bin/env python3
"""Bit-exact .99/.100 native comparison and paired AArch64 emulation benchmark.

The production JNI and NEON execute directly. JNI array operations are explicit
fixtures. Emulated process CPU time is measured, never reported as phone latency.
"""
from pathlib import Path
import argparse,hashlib,json,os,shutil,statistics,subprocess
BASE_SHA='9ce27f6b990a68c32c42e936d9bcdeda9bfdbeebf7be48d2f57b64c3d39d1938'

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    baseline=source/'tests19100/front/reference99/single_noise1955.c'
    candidate=source/'native1955/single_noise1955.c';fixture=source/'tests19100/front/FrontKernel19100.c'
    sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()
    if sha(baseline)!=BASE_SHA:raise AssertionError('Published .99 oracle changed')
    bench_kernel=source/'tests19100/front/FrontBenchKernel19100.c';bench_main=source/'tests19100/front/FrontBench19100.c'
    tracked=[baseline,candidate,fixture,bench_kernel,bench_main,Path(__file__).resolve()]
    pins={str(p):sha(p) for p in tracked};commands=[];reports={};assertions=0
    cross,qemu,cc=shutil.which('aarch64-linux-gnu-gcc'),shutil.which('qemu-aarch64'),shutil.which('cc')
    if not all([cross,qemu,cc]):raise AssertionError('Actual AArch64 compiler/emulator and host compiler are required')
    flags=['-O3','-std=c11','-Wall','-Wextra','-Werror','-fno-fast-math','-ffp-contract=off','-pthread']
    include=['-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux')]
    def run(label,cmd):
        commands.append(dict(label=label,argv=list(map(str,cmd))))
        proc=subprocess.run(list(map(str,cmd)),text=True,capture_output=True,timeout=300)
        (work/(label+'.log')).write_text(proc.stdout+proc.stderr)
        if proc.returncode:raise AssertionError(label+'\n'+proc.stdout[-3000:]+proc.stderr[-6000:])
        return proc.stdout
    for platform,compiler in [('host',cc),('arm',cross)]:
        for label,kernel in [('baseline',baseline),('candidate',candidate)]:
            binary=work/(label+'-'+platform)
            cmd=[compiler,*flags,*(['-static'] if platform=='arm' else []),*include,'-DFRONT_KERNEL="'+str(kernel)+'"']
            if label=='baseline':cmd.append('-DFRONT_BASELINE=1')
            run(label+'-'+platform+'-compile',cmd+[fixture,'-lm','-o',binary])
            report=json.loads(run(label+'-'+platform,[*([qemu] if platform=='arm' else []),binary,work/(label+'-'+platform+'.bin'),'0']))
            if report.get('pixel_cases')!=320 or report.get('pixels')!=522120 or report.get('transform_cases')!=8192 or report.get('model_cases')!=32768:
                raise AssertionError('Native comparison coverage incomplete')
            reports[label+'-'+platform]=report;assertions+=report['assertions']
        # This compares every output byte: raw double DCT values, raw float
        # model sigma values, and all JNI-completed output pixels/alpha.
        old=(work/('baseline-'+platform+'.bin')).read_bytes();new=(work/('candidate-'+platform+'.bin')).read_bytes()
        if not old or old!=new:raise AssertionError(platform+' raw-bit/pixel mismatch')
        reports[platform+'-exact']=dict(compared_bytes=len(old),baseline_sha256=sha(work/('baseline-'+platform+'.bin')),candidate_sha256=sha(work/('candidate-'+platform+'.bin')))
        assertions+=1
    # Both actual kernels live in separate translation units within one QEMU
    # process. Alternate their order every trial, after warming both kernels.
    # This avoids comparing independently scheduled emulator lifetimes.
    for label,kernel in [('baseline',baseline),('candidate',candidate)]:
        run(label+'-bench-compile',[cross,*flags,*include,'-DFRONT_ENTRY=front_'+label+'19100',
            '-DFRONT_KERNEL="'+str(kernel)+'"','-c',bench_kernel,'-o',work/(label+'-bench.o')])
    run('bench-link',[cross,*flags,'-static',bench_main,work/'baseline-bench.o',work/'candidate-bench.o','-lm','-o',work/'bench'])
    measured=json.loads(run('bench-interleaved',[qemu,work/'bench']))
    samples=measured.get('samples_ns',{})
    if measured.get('same_process') is not True or measured.get('benchmark_argb_exact') is not True:
        raise AssertionError('Same-process bit-exact benchmark absent')
    if set(samples)!={'baseline','candidate'} or any(len(v)!=12 or any(n<=0 for n in v) for v in samples.values()):
        raise AssertionError('Invalid measured CPU timing')
    medians={k:statistics.median(v) for k,v in samples.items()}
    ratios=[b/a for a,b in zip(samples['baseline'],samples['candidate'])];ratio=statistics.median(ratios)
    assertions+=12
    # Emulator throughput is evidence for implementation selection, not a
    # deterministic numerical CI predicate or Android device speed guarantee.
    # Always retain an unfavorable measurement instead of retrying to hide it.
    if pins!={str(p):sha(p) for p in tracked}:raise AssertionError('Compiled source changed during native test')
    result=dict(status='passed',assertions=assertions,tests=reports,source_sha256=pins,
        front19100_same_argb=True,front19100_same_dct_raw_bits=True,front19100_same_model_raw_bits=True,
        front19100_arm_neon_executed=True,front19100_emulated_cpu_benchmarked=True,front19100_benchmark_same_process=True,
        front19100_emulated_cpu_improved=ratio<1,
        benchmark=dict(clock='CLOCK_PROCESS_CPUTIME_ID under qemu-aarch64',samples_ns=samples,median_ns=medians,
                       candidate_over_baseline=ratio,paired_ratios=ratios,reduction_percent=(1-ratio)*100,pixels_per_sample=73728,
                       order='alternating baseline/candidate and candidate/baseline each of 12 trials',phone_latency_prediction=False),
        compile_flags=flags,baseline_version='1.9.99',physical_android_tested=False,device_speedup_verified=False)
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n')
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args()
    print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
