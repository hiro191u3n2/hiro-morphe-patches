#!/usr/bin/env python3
"""Execute frozen .113/current native kernels; compare raw bits and whole JNI output.
AArch64 is emulated, not a target-device speed or quality claim.
"""
from pathlib import Path
from front_delta19114 import audit as alias_audit19114
import argparse, hashlib, json, os, shutil, statistics, subprocess
ROOT=Path(__file__).resolve().parent
BASELINE='db3eef06e6fa3fffc324255e856cdd871abd07f0eb8b078dc469275d0a99b780'
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or os.environ['JAVA_HOME']).resolve()
    reference=source/'tests19114/front/reference113/single_noise1955.c';candidate=source/'native1955/single_noise1955.c'
    if sha(reference)!=BASELINE:raise AssertionError('Frozen .113 native reference changed')
    fixture=source/'tests19114/front/FrontKernel19114.c'
    tracked=[reference,candidate,fixture,source/'host_front19114.py',source/'front_delta19114.py',source/'source_inverse19115.py',source/'source_inverse19115_manifest.json',source/'native1960/residual_blocks1961.c',source/'tests19100/front/FrontKernel19100.c',source/'tests19113/front/FrontKernel19113.c',source/'tests19100/front/FrontBench19100.c',source/'tests19100/front/FrontBenchKernel19100.c',source/'tests19113/front/FrontBenchCached19113.c']
    alias_contract=alias_audit19114(source)
    pins={p.relative_to(source).as_posix():sha(p) for p in tracked};commands=[]
    def run(args,name,timeout=360):
        args=list(map(str,args));commands.append(dict(name=name,argv=args))
        r=subprocess.run(args,text=True,capture_output=True,timeout=timeout)
        (work/(name+'.log')).write_text(r.stdout+r.stderr)
        if r.returncode:raise AssertionError(name+' failed: '+r.stdout[-2000:]+r.stderr[-6000:])
        return r.stdout
    include=jdk/'include'
    flags=['-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off','-fno-fast-math','-pthread','-I'+str(include),'-I'+str(include/'linux')]
    configurations=[('host',shutil.which('cc'),[]),('arm64',shutil.which('aarch64-linux-gnu-gcc'),[shutil.which('qemu-aarch64')])]
    if any(not cc or any(not v for v in runner) for _,cc,runner in configurations):raise AssertionError('Both host cc and AArch64 compiler/qemu required')
    result={'schema':'ulike19114-front-native-proof-v1','status':'passed','baseline_version':'1.9.113','source_sha256':pins,'alias_contract':alias_contract,'architectures':{},'physical_android_tested':False,'device_speedup_verified':False,'device_tested':False,'device_speed_measured':False,'target_device_gain_claimed':False}
    for arch,cc,runner in configurations:
        aflags=flags+(['-static'] if arch=='arm64' else [])
        binaries={};proofs={};stats={}
        for label,kernel in [('baseline',reference),('candidate',candidate)]:
            binary=work/(arch+'-'+label)
            run([cc,*aflags,'-DFRONT_KERNEL="'+str(kernel)+'"',fixture,'-lm','-o',binary],arch+'-'+label+'-build')
            binaries[label]=binary;proofs[label]={};stats[label]={}
            for mode,scope in [(0,'pixels'),(2,'safeguards'),(3,'pairs'),(4,'coefficients')]:
                output=work/(arch+'-'+label+'-'+scope+'.bin')
                stats[label][scope]=json.loads(run([*runner,binary,output,str(mode)],arch+'-'+label+'-'+scope))
                proofs[label][scope]={'sha256':sha(output),'bytes':output.stat().st_size}
        if proofs['baseline']!=proofs['candidate']:raise AssertionError(arch+' raw bits/full outputs differ from .113')
        if stats['candidate']['coefficients'].get('coefficient_cases')!=8192:raise AssertionError('Missing coefficient selection proof cases')
        if stats['candidate']['pixels'].get('pixel_cases')!=320 or stats['candidate']['safeguards'].get('safeguard_cases')!=32768 or stats['candidate']['pairs'].get('pair_cases')!=512:raise AssertionError('Missing exact proof cases')
        negatives=[]
        for name,old,new,mode,scope in [
            ('lag-numerator','numerator += p*q;','numerator += p*p;',2,'safeguards'),
            ('lag-denominator','numerator/sqrt(a*b)','numerator/sqrt(a*b*1.05)',2,'safeguards')]:
            text=candidate.read_text()
            if text.count(old)!=(2 if name=='lag-numerator' else 1):raise AssertionError('Missing negative-control mutation '+name)
            mutant=work/(arch+'-'+name+'.c');mutant.write_text(text.replace(old,new))
            binary=work/(arch+'-'+name);output=work/(arch+'-'+name+'.bin')
            run([cc,*aflags,'-DFRONT_KERNEL="'+str(mutant)+'"',fixture,'-lm','-o',binary],arch+'-'+name+'-build')
            run([*runner,binary,output,str(mode)],arch+'-'+name+'-run')
            if sha(output)==proofs['baseline'][scope]['sha256']:raise AssertionError('Mutation not detected '+name)
            negatives.append(name)
        benches={}
        for mode,bench_fixture in [('uncached',source/'tests19100/front/FrontBenchKernel19100.c'),('cached',source/'tests19113/front/FrontBenchCached19113.c')]:
            objects=[]
            for label,kernel in [('baseline',reference),('candidate',candidate)]:
                obj=work/(arch+'-'+mode+'-'+label+'.o');objects.append(obj)
                run([cc,*aflags,'-DFRONT_KERNEL="'+str(kernel)+'"','-DFRONT_ENTRY=front_'+label+'19100','-c',bench_fixture,'-o',obj],arch+'-'+mode+'-'+label+'-bench-build')
            binary=work/(arch+'-'+mode+'-bench')
            run([cc,*aflags,source/'tests19100/front/FrontBench19100.c',*objects,'-lm','-o',binary],arch+'-'+mode+'-bench-link')
            bench=json.loads(run([*runner,binary],arch+'-'+mode+'-bench',timeout=480))
            samples=bench['samples_ns'];old=statistics.median(samples['baseline']);new=statistics.median(samples['candidate'])
            bench.update(baseline_median_ns=old,candidate_median_ns=new,candidate_over_baseline=new/old,host_or_emulation_only=True)
            benches[mode]=bench
        result['architectures'][arch]={'exact_output':True,'raw_bits_exact':True,'proofs':proofs['candidate'],'cases':stats['candidate'],'negative_controls':negatives,'benchmarks':benches,'binary_sha256':{k:sha(v) for k,v in binaries.items()},'compiler':run([cc,'--version'],arch+'-compiler-version').splitlines()[0]}
    if pins!={p.relative_to(source).as_posix():sha(p) for p in tracked}:raise AssertionError('Source changed during native differential')
    result['assertions']=sum(a['cases']['pixels']['assertions']+a['cases']['safeguards']['safeguard_cases']+a['cases']['pairs']['pair_cases']+a['cases']['coefficients']['coefficient_cases']+len(a['negative_controls']) for a in result['architectures'].values())
    result['actual_arm_neon_executed']=True;result['existing_jni_transaction_fixture']=True
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
