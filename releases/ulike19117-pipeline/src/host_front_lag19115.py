#!/usr/bin/env python3
"""Execute the reviewed pure-lag pruning against frozen114 on x86 and AArch64.

QEMU and process-CPU microbenchmarks are never device/save-time evidence.
Counters exist only in separate instrumented test translation units.
"""
from pathlib import Path
import argparse,hashlib,json,os,shutil,statistics,subprocess
from front_lag_delta19115 import BASELINE_SHA256,audit
ROOT=Path(__file__).resolve().parent
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or os.environ['JAVA_HOME']).resolve()
    baseline=source/'tests19115/provenance/reference114/native1955/single_noise1955.c'
    candidate=source/'native1955/single_noise1955.c'
    if sha(baseline)!=BASELINE_SHA256:raise AssertionError('Frozen114 front source changed')
    contract=audit(candidate.read_text())
    tracked=[baseline,candidate,source/'host_front_lag19115.py',source/'front_lag_delta19115.py',
        source/'tests19115/front/GuardBranches19115.c',source/'tests19114/front/FrontKernel19114.c',
        source/'tests19100/front/FrontKernel19100.c',source/'tests19100/front/FrontBench19100.c',
        source/'tests19100/front/FrontBenchKernel19100.c',source/'tests19113/front/FrontBenchCached19113.c']
    pins={p.relative_to(source).as_posix():sha(p) for p in tracked};commands=[]
    def run(args,name,timeout=480):
        args=list(map(str,args));commands.append({'name':name,'argv':args})
        done=subprocess.run(args,text=True,capture_output=True,timeout=timeout)
        (work/(name+'.log')).write_text(done.stdout+done.stderr)
        if done.returncode:raise AssertionError(name+': '+done.stdout[-2000:]+done.stderr[-6000:])
        return done
    inc=jdk/'include'
    if not (inc/'jni.h').is_file():raise AssertionError('Actual JDK JNI headers required')
    flags=['-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off','-fno-fast-math','-pthread','-I'+str(inc),'-I'+str(inc/'linux')]
    configurations=[('host',shutil.which('cc'),[]),('arm64',shutil.which('aarch64-linux-gnu-gcc'),[shutil.which('qemu-aarch64')])]
    if any(not cc or any(not tool for tool in runner) for _,cc,runner in configurations):
        raise AssertionError('Both host and AArch64 compiler/QEMU execution required')
    result={'schema':'ulike19115-front-lag-proof-v1','status':'passed','source_sha256':pins,'source_contract':contract,
        'physical_android_tested':False,'device_speedup_verified':False,'save_2000ms_achieved':False,
        'actual_arm_neon_executed':True,'existing_jni_transaction_fixture':True,
        'test_counters_only':True,'architectures':{}}
    fixture=source/'tests19114/front/FrontKernel19114.c'
    branch_fixture=source/'tests19115/front/GuardBranches19115.c'
    for arch,cc,runner in configurations:
        aflags=flags+(['-static'] if arch=='arm64' else [])
        proofs={};reports={}
        for label,kernel in [('baseline',baseline),('candidate',candidate)]:
            binary=work/(arch+'-'+label)
            run([cc,*aflags,'-DFRONT_KERNEL="'+str(kernel)+'"',fixture,'-lm','-o',binary],arch+'-'+label+'-build')
            proofs[label]={};reports[label]={}
            for mode,name in [(0,'pixels'),(2,'safeguards'),(3,'pairs'),(4,'coefficients')]:
                output=work/(arch+'-'+label+'-'+name+'.bin')
                reports[label][name]=json.loads(run([*runner,binary,output,str(mode)],arch+'-'+label+'-'+name).stdout)
                proofs[label][name]={'sha256':sha(output),'bytes':output.stat().st_size};output.unlink()
        if proofs['baseline']!=proofs['candidate']:raise AssertionError(arch+' native output differs')
        expected_cases={'pixels':('pixel_cases',320),'safeguards':('safeguard_cases',32768),'pairs':('pair_cases',512),'coefficients':('coefficient_cases',8192)}
        for name,(field,count) in expected_cases.items():
            if reports['candidate'][name].get('status')!='passed' or reports['candidate'][name].get(field)!=count:
                raise AssertionError('Incomplete actual front corpus '+name)
        branches={}
        text=candidate.read_text()
        for label,body in [('baseline',baseline.read_text()),('candidate',text),
                ('remove-needed-lag1',text.replace('r2 > .25 ? lag(residual, n, 1) : 0','0')),
                ('remove-needed-lag4',text.replace('n == 8 && r2 < -.15 ? lag(residual, n, 4) : 0','0'))]:
            kernel=work/(arch+'-'+label+'-branches.c');kernel.write_text(body);binary=work/(arch+'-'+label+'-branches')
            run([cc,*aflags,'-DFRONT_KERNEL="'+str(kernel)+'"',branch_fixture,'-lm','-o',binary],arch+'-'+label+'-branches-build')
            branches[label]=run([*runner,binary],arch+'-'+label+'-branches').stdout
        if branches['baseline']!=branches['candidate']:raise AssertionError('Safeguard required branches differ')
        for name in ('remove-needed-lag1','remove-needed-lag4'):
            if branches[name]==branches['baseline']:raise AssertionError('Required-lag mutation not detected: '+name)
        counters={}
        for label,path in [('baseline',baseline),('candidate',candidate)]:
            body=path.read_text();pattern='static double lag(const double *values, int n, int distance) {'
            if body.count(pattern)!=1:raise AssertionError('Lag function instrumentation target changed')
            body='#include <stdio.h>\n'+body.replace(pattern,'static uint64_t observed_lags[5];\n'+pattern+'\n    observed_lags[distance]++;')
            body+='\n__attribute__((destructor)) static void observed_report(void){fprintf(stderr,"{\\"lag1\\":%llu,\\"lag2\\":%llu,\\"lag4\\":%llu}\\n",(unsigned long long)observed_lags[1],(unsigned long long)observed_lags[2],(unsigned long long)observed_lags[4]);}\n'
            kernel=work/(arch+'-'+label+'-count.c');kernel.write_text(body)
            binary=work/(arch+'-'+label+'-count');output=work/(arch+'-'+label+'-count.bin')
            run([cc,*aflags,'-DFRONT_KERNEL="'+str(kernel)+'"',fixture,'-lm','-o',binary],arch+'-'+label+'-count-build')
            measured=run([*runner,binary,output,'2'],arch+'-'+label+'-count')
            counters[label]=json.loads(measured.stderr)
            if sha(output)!=proofs[label]['safeguards']['sha256']:raise AssertionError('Test counters changed output')
            output.unlink()
        old_calls=sum(counters['baseline'].values());new_calls=sum(counters['candidate'].values())
        if not 0<new_calls<old_calls or counters['baseline']['lag2']!=counters['candidate']['lag2']:
            raise AssertionError('Actual lag calls not reduced with same lag2 count')
        benches={}
        for mode,bench_fixture in [('uncached','tests19100/front/FrontBenchKernel19100.c'),('cached','tests19113/front/FrontBenchCached19113.c')]:
            objects=[]
            for label,kernel in [('baseline',baseline),('candidate',candidate)]:
                obj=work/(arch+'-'+mode+'-'+label+'.o');objects.append(obj)
                run([cc,*aflags,'-DFRONT_KERNEL="'+str(kernel)+'"','-DFRONT_ENTRY=front_'+label+'19100','-c',source/bench_fixture,'-o',obj],arch+'-'+mode+'-'+label+'-bench-build')
            binary=work/(arch+'-'+mode+'-bench')
            run([cc,*aflags,source/'tests19100/front/FrontBench19100.c',*objects,'-lm','-o',binary],arch+'-'+mode+'-bench-link')
            bench=json.loads(run([*runner,binary],arch+'-'+mode+'-bench').stdout)
            if bench.get('status')!='passed' or bench.get('benchmark_argb_exact') is not True or bench.get('same_process') is not True:
                raise AssertionError('Paired real-kernel benchmark incomplete')
            samples=bench['samples_ns']
            if any(len(samples[label])!=12 or any(type(value)!=int or value<=0 for value in samples[label]) for label in ['baseline','candidate']):
                raise AssertionError('Incomplete alternating timings')
            old=statistics.median(samples['baseline']);new=statistics.median(samples['candidate'])
            bench.update(baseline_median_ns=old,candidate_median_ns=new,candidate_over_baseline=new/old,host_or_emulation_only=True)
            benches[mode]=bench
        result['architectures'][arch]={'raw_bits_exact':True,'proofs':proofs['candidate'],'cases':reports['candidate'],
            'required_lag_negative_controls':2,'branch_outputs':branches,'actual_call_counts':counters,
            'baseline_lag_calls':old_calls,'candidate_lag_calls':new_calls,'lag_calls_removed':old_calls-new_calls,
            'benchmark_scope':'192x128 front kernel, process CPU time; not full photograph or device',
            'benchmarks':benches,'compiler':run([cc,'--version'],arch+'-compiler-version').stdout.splitlines()[0]}
    if pins!={p.relative_to(source).as_posix():sha(p) for p in tracked}:raise AssertionError('Front proof sources changed during execution')
    result['assertions']=contract['negative_controls']+sum(a['cases']['pixels']['assertions']+32768+512+8192+8+2+3 for a in result['architectures'].values())
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n')
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path)
    a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
