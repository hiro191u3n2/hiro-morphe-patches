"""GX31 exact published .63 policy descriptors and observed mask-owner calls."""
from pathlib import Path
import hashlib,json,os,re,subprocess

def _run(args,log):
    row=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=180)
    Path(log).write_text(row.stdout+row.stderr)
    if row.returncode:raise RuntimeError(str(log)+'\n'+row.stdout[-2000:]+row.stderr[-8000:])
    if 'WARNING in native method' in row.stdout+row.stderr or 'FATAL ERROR' in row.stdout+row.stderr:raise AssertionError('JNI checker failed: '+str(log))
    found=re.search(r'^RESULT (\{[^\n]+\})$',row.stdout,re.M)
    return json.loads(found.group(1)) if found else None

def test(root,work,jdk=None,ndk=None):
    root=Path(root).resolve();work=Path(work).resolve()/'gx31-policy1964';work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME'])
    reference=root/'tests1964/published1963-reference';pins=json.loads((reference/'pins.json').read_text())
    if pins['baseline_version']!='1.9.63':raise AssertionError('GX31 requires published .63')
    reports={};dumps={};source_pins={}
    for label,tree,candidate in [('published1963',reference,False),('candidate1964',root,True)]:
        classes=work/(label+'-classes');classes.mkdir(exist_ok=True)
        sources=[tree/name for name in pins['pipeline_sources']]+[root/'tests1964/PolicyReuse1964Test.java']
        if candidate:sources.append(root/'tests1964/SingleParallel1964Test.java')
        for p in sources:
            if p.is_relative_to(reference):source_pins[str(p.relative_to(reference))]=hashlib.sha256(p.read_bytes()).hexdigest()
        _run([jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-d',classes,*sources],work/(label+'-compile.log'))
        dumps[label]=work/(label+'.bin')
        reports[label]=_run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-Djava.library.path='+str(work/'unavailable'),'-cp',classes,'com.hiro.ulike.PolicyReuse1964Test',dumps[label],str(candidate).lower()],work/(label+'-run.log'))
        if not reports[label] or reports[label]['status']!='passed':raise AssertionError('Executed GX31 report missing')
        if candidate:reports['single_parallel']=_run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-Djava.library.path='+str(work/'unavailable'),'-cp',classes,'com.hiro.ulike.SingleParallel1964Test'],work/'single-parallel.log')
        if candidate:
            _run(['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),root/'native1955/single_noise1955.c','-Wl,--no-undefined','-lm','-o',work/'libulike_nr1955.so'],work/'native-cpu-compile.log')
            reports['single_parallel_native']=_run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-Xcheck:jni','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.SingleParallel1964Test','native'],work/'single-parallel-native.log')
    before,after=reports['published1963'],reports['candidate1964']
    if dumps['published1963'].read_bytes()!=dumps['candidate1964'].read_bytes():raise AssertionError('GX31 changed published .63 mask, coefficient grid or oracle policy')
    for key in ('skin_calls','detail_calls'):
        if before[key]!=2*after[key] or after[key]<=0:raise AssertionError('GX31 did not remove the second exact '+key)
    for name,digest in source_pins.items():
        if hashlib.sha256((reference/name).read_bytes()).hexdigest()!=digest:raise AssertionError('Published .63 source changed: '+name)
    result={'status':'passed','assertions':sum(v['assertions'] for v in reports.values()),'cases':after['cases'],
        'published1963_exact_policy':True,'exact_output_sha256':hashlib.sha256(dumps['candidate1964'].read_bytes()).hexdigest(),
        'descriptor_bytes_compared':dumps['candidate1964'].stat().st_size,
        'mask_owner_calls_before':before['skin_calls']+before['detail_calls'],
        'mask_owner_calls_after':after['skin_calls']+after['detail_calls'],
        'double_mask_arithmetic_preserved':True,'nonzero_origins_and_odd_dimensions':True,
        'null_plan_and_cpu_only_policy_exact':True,'caller_owned_snapshot':True,
        'parallel_cpu_pixel_equivalence':reports['single_parallel']['parallel_cpu_pixel_equivalence'],
        'parallel_single_cases':reports['single_parallel']['cases'],
        'residual_production_parallel_cases':reports['single_parallel']['residual_production_parallel_cases'],
        'residual_parallel_pixel_equivalence':reports['single_parallel']['residual_parallel_pixel_equivalence'],
        'actual_native_residual_cpu_parallel':reports['single_parallel_native']['native_cpu'],
        'native_residual_production_parallel_cases':reports['single_parallel_native']['residual_production_parallel_cases'],
        'reports':reports,'baseline_source_pins':source_pins,'physical_android_tested':False}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[1]);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);a=p.parse_args();print(json.dumps(test(a.root,a.work,a.jdk),indent=2))
