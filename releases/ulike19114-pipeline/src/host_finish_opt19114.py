#!/usr/bin/env python3
"""Exact .113 packed-mask oracle plus current real-JNI policy for constant rows.

Uniform-row detection proves equality using monotone bounds evaluated with the
original double operations and Math.round. No mask/image resolution or quality
parameter is changed. Operation savings are proven; Android timing is unmeasured.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess
BASELINE_SHA256='5905cc2f0b3cb2327309788bb5476c40477c580703fc0944b4353c3a7de9f397'
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or os.environ['JAVA_HOME']).resolve()
 android=Path(os.environ.get('ULIKE_ANDROID_JAR',str(source.parent/'tools/android.jar'))).resolve()
 fixture=source/'tests19114/finish/FinishOpt19114Test.java';benchmark=source/'tests19114/finish/FinishBench19114.java';baseline=source/'tests19114/finish/reference113/GpuFinishPolicy1992.java'
 if sha(baseline)!=BASELINE_SHA256:raise AssertionError('Published .113 finish-policy oracle changed')
 owned=[source/'GpuFinishPolicy1992.java',source/'host_finish_opt19114.py',fixture,benchmark,baseline]
 pins={str(p.relative_to(source)):sha(p) for p in owned}
 spec=importlib.util.spec_from_file_location('finish114_policy',source/'host_finish_policy1992.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
 inherited=module.test(source,work/'policy',jdk,ndk);pins.update(inherited['source_sha256'])
 renamed=work/'BaselineFinishPolicy19113.java';renamed.write_text(baseline.read_text().replace('GpuFinishPolicy1992','BaselineFinishPolicy19113'))
 classes=work/'classes';classes.mkdir(exist_ok=True);cp=os.pathsep.join(map(str,[work/'policy/classes',android]))
 commands=[];environment=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
 def run(argv,name):
  argv=list(map(str,argv));commands.append(dict(name=name,argv=argv))
  response=subprocess.run(argv,capture_output=True,text=True,env=environment,timeout=300);output=response.stdout+response.stderr;(work/(name+'.log')).write_text(output)
  if response.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError(name+' failed:\n'+output[-10000:])
  return output
 run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',classes,renamed,fixture,benchmark],'finish114-compile')
 output=run([jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx768m','-Djava.library.path='+str(work/'policy'),'-cp',str(classes)+os.pathsep+cp,'com.hiro.ulike.FinishOpt19114Test'],'finish114-exact')
 proof=json.loads(output.split('RESULT ',1)[1].strip())
 benchmark_output=run([jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx768m','-Djava.library.path='+str(work/'policy'),'-cp',str(classes)+os.pathsep+cp,'com.hiro.ulike.FinishBench19114'],'finish114-benchmark')
 timing=json.loads(benchmark_output.split('RESULT ',1)[1].strip())
 if timing.get('status')!='passed' or timing.get('host_timing_is_diagnostic_only') is not True:raise AssertionError('Missing diagnostic finish benchmark')
 if proof.get('status')!='passed' or proof.get('compared_pixels',0)<60000000 or proof.get('constant_pixels',0)<2500000 or proof.get('gpu_policy_words',0)<10000000:raise AssertionError('Incomplete exact-row policy proof')
 for key in ('published113_masks_equal','exact_cpu_oracle_equal','nonfinite_domain_declined','rotated_fallback_verified','actual_jni'):
  if proof.get(key) is not True:raise AssertionError('Missing exact finish proof '+key)
 if pins!={name:sha(source/name) for name in pins}:raise AssertionError('Current source changed during finish114 proof')
 result=dict(proof,assertions=proof['assertions']+inherited['assertions'],inherited_policy=inherited,
  source_sha256=pins,published113_reference_sha256=BASELINE_SHA256,host_benchmark=timing,
  original_rounding_preserved=True,extra_image_buffers=0,extra_mask_arrays=0,
  scope='Immutable unsigned-byte mask rows with finite nonnegative bilinear weights; unknown/rotated owners use original sampling. Constant rows prove both original-rounded extrema equal.',
  operation_reduction_verified=True,host_timing_is_diagnostic_only=True)
 (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for name in ('source','work'):p.add_argument('--'+name,required=True)
 p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
