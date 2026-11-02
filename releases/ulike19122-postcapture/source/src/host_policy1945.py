#!/usr/bin/env python3
"""v1944 pixel oracle plus v1945 policy leases, cache misses, geometry and allocation faults."""
from pathlib import Path
import argparse,json,os,shutil,subprocess


def test(root,work,android=None):
    root=Path(root);work=Path(work);out=work/'host-policy1945';classes=out/'classes'
    classes.mkdir(parents=True,exist_ok=True)
    compiler=os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
    runtime=os.environ.get('ULIKE_JAVA') or (str(Path(compiler).with_name('java')) if compiler else 'java')
    javac=[compiler] if compiler else ['java','com.sun.tools.javac.Main']
    names=('QualityPixels1932.java','NativeMoire1951.java','PolicyCache1945.java','QualityShadow1932.java',
           'NoiseCache1944.java','NativeSpeed1944.java','GpuInteger1949.java','NativeSpeed1935.java',
           'SpatialNoise1934.java','LongMoire1934.java',
           'cache1945-reference/QualityPixelsReference1944.java',
           'cache1945-reference/QualityShadowReference1944.java',
           'tests/PolicyCache1945Test.java','tests/noise-fixtures/com/hiro/ulike/QualityPipeline1932.java')
    # Fault injection exists only in this generated host fixture.
    fixture=out/'fault-fixture';fixture.mkdir(parents=True,exist_ok=True)
    worker=(root/'SpeedWorkers1935.java').read_text().replace(
        'public static int[] borrowInts(int length) {',
        'public static int[] borrowInts(int length) {\n'
        '        if(Boolean.getBoolean("ulike.test.policyFault"))throw new OutOfMemoryError("test policy workspace");')
    worker_path=fixture/'SpeedWorkers1935.java';worker_path.write_text(worker)
    compile_result=subprocess.run([*javac,'-source','8','-target','8','-Xlint:-options',
        '-d',str(classes),str(worker_path),*[str(root/name) for name in names]],capture_output=True,text=True,timeout=120)
    (out/'compile.log').write_text(compile_result.stdout+compile_result.stderr)
    if compile_result.returncode:raise RuntimeError(compile_result.stderr[-8000:])
    variants=[('java',[],[]),('allocation-fallback',['-Dulike.test.policyFault=true'],['oom']),
              ('low-memory-fallback',['-Xmx64m'],['lowmemory'])]
    native=os.environ.get('ULIKE_HOST_NATIVE')
    if native:variants.append(('native',['-Djava.library.path='+native],[]))
    results={}
    for label,flags,args in variants:
        run=subprocess.run([runtime,'-Xmx768m',*flags,'-cp',str(classes),
            'com.hiro.ulike.PolicyCache1945Test',*args],capture_output=True,text=True,timeout=240)
        (out/(label+'.log')).write_text(run.stdout+run.stderr)
        if run.returncode:raise RuntimeError(label+': '+run.stderr[-8000:])
        results[label]=json.loads(run.stdout)
    result={'suite':'policy-cache1945','status':'passed',
        'assertions':sum(v['comparedPixels']+v['sigmaBitComparisons'] for v in results.values()),
        'equal':all(v['equal'] for v in results.values()),'variants':results,
        'oracle':'published-v1944-arithmetic','max_policy_pixel_bytes_per_worker':2*1024*1024}
    (out/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--out',type=Path,required=True)
    args=parser.parse_args();print(json.dumps(test(Path(__file__).resolve().parent,args.out)))
