#!/usr/bin/env python3
"""Exact immutable horizontal rows, complete CPU/GPU oracle, and owned-budget fallback.

Host timing is diagnostic only. It is neither an Android latency measurement nor
an acceptance gate; exact pixels, resource ownership and fewer operations gate.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess

def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or os.environ['JAVA_HOME']).resolve()
 android=Path(os.environ.get('ULIKE_ANDROID_JAR',str(source.parent/'tools/android.jar'))).resolve()
 tests=source/'tests1998/mask'
 owned=[source/'GpuFinishPolicy1992.java',source/'host_mask_rows1998.py',tests/'MaskRows1998Test.java',tests/'MaskParallel1998Test.java',tests/'admission/GpuNoise1960.java',tests/'admission/MaskAdmission1998Test.java']
 pins={str(p.relative_to(source)):sha(p) for p in owned}
 spec=importlib.util.spec_from_file_location('mask_policy_inherited1998',source/'host_finish_policy1992.py');inherited=importlib.util.module_from_spec(spec);spec.loader.exec_module(inherited)
 evidence=inherited.test(source,work/'policy',jdk,ndk)
 pins.update(evidence['source_sha256'])
 commands=[];environment=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
 def run(argv,name):
  argv=list(map(str,argv));commands.append(dict(name=name,argv=argv))
  r=subprocess.run(argv,capture_output=True,text=True,env=environment,timeout=300);output=r.stdout+r.stderr;(work/(name+'.log')).write_text(output)
  if r.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError(name+' failed:\n'+output[-10000:])
  return output
 classes=work/'classes';classes.mkdir(exist_ok=True);cp=os.pathsep.join(map(str,[work/'policy/classes',android]))
 run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',classes,tests/'MaskRows1998Test.java',tests/'MaskParallel1998Test.java'],'mask-compile')
 outputs={}
 for name,java in [('rows','MaskRows1998Test'),('parallel','MaskParallel1998Test')]:
  output=run([jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m','-Djava.library.path='+str(work/'policy'),'-cp',str(classes)+os.pathsep+cp,'com.hiro.ulike.'+java],'mask-'+name)
  outputs[name]=json.loads(output.split('RESULT ',1)[1].strip())
 admission=work/'admission';admission.mkdir(exist_ok=True)
 run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',admission,tests/'admission/GpuNoise1960.java',source/'GpuFinishPolicy1992.java',tests/'admission/MaskAdmission1998Test.java'],'admission-compile')
 output=run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-Xmx512m','-cp',str(admission)+os.pathsep+cp,'com.hiro.ulike.MaskAdmission1998Test'],'admission-boundary')
 outputs['admission']=json.loads(output.split('RESULT ',1)[1].strip())
 rows,parallel,budget=(outputs[k] for k in ('rows','parallel','admission'))
 if not all(x.get('status')=='passed' for x in [evidence,rows,parallel,budget]):raise AssertionError('Missing passing proof')
 if rows.get('compared_pixels',0)<24000000 or rows.get('cache_cases',0)<32 or rows.get('rotated_fallback_cases',0)<42 or not rows.get('raw_double_intermediates_equal'):raise AssertionError('Incomplete exact mask corpus')
 if rows.get('cached_mask_byte_reads_upper_bound',10**12)*2>=rows.get('baseline_mask_byte_reads',0):raise AssertionError('Immutable row operation reduction not demonstrated')
 if parallel.get('parallel_compared_pixels',0)<1300000 or not all(parallel.get(k) is True for k in ['worker_failure_join_verified','cancel_reuse_verified','actual_jni']):raise AssertionError('Missing parallel failure/cancellation ownership')
 if not all(budget.get(k) is True for k in ['optional_budget_fallback_verified','one_owned_lease_verified','admission_boundary_mocked']):raise AssertionError('Missing admission boundary proof')
 if pins!={name:sha(source/name) for name in pins}:raise AssertionError('Production or proof source changed during mask verification')
 result={'status':'passed','assertions':sum(x['assertions'] for x in [evidence,rows,parallel,budget]),'inherited_policy':evidence,'rows':rows,'parallel':parallel,'admission':budget,
  'full_policy_equal':True,'full_output_equal':True,'raw_double_intermediates_equal':True,'optional_budget_fallback_verified':True,'worker_failure_join_verified':True,'rotation_fallback_verified':True,
  'max_cache_java_bytes_at_width4284':128*4284+4096,'actual_jni':True,'host_timing_is_diagnostic_only':True,'physical_android_tested':False,'device_speedup_verified':False,'source_sha256':pins}
 (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for name in ('source','work'):p.add_argument('--'+name,required=True)
 p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
