#!/usr/bin/env python3
"""Retain standard/profile7 proof; add exact stripe profile6 and capture-separated queue proof."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess
ROOT=Path(__file__).resolve().parent
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 tracked=[source/p for p in ['GpuStrongDefault1990.java','GpuStrong1960.java','host_strong_adaptive1998.py','tests1998/strong/StrongStripe1998Test.java','tests1998/strong/StrongStripeState1998Test.java','tests1998/strong/StripePeers1998.java','tests1998/strong/StrongRecovery1998Test.java']]
 pins={str(p.relative_to(source)):sha(p) for p in tracked}
 spec=importlib.util.spec_from_file_location('strong98_retained94',source/'host_strong_adaptive1994.py');base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)
 retained=base.test(source,work/'retained94',jdk,ndk)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);android=Path(os.environ.get('ULIKE_ANDROID_JAR',str(source.parent/'tools/android.jar')))
 classes=work/'retained94/standard/classes';cp=str(classes)+os.pathsep+str(android);env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1');commands=[]
 def run(argv,name):
  argv=list(map(str,argv));commands.append(dict(name=name,argv=argv));p=subprocess.run(argv,text=True,capture_output=True,env=env,timeout=300);out=p.stdout+p.stderr;(work/(name+'.log')).write_text(out)
  if p.returncode or 'WARNING in native method' in out or 'FATAL ERROR' in out:raise RuntimeError(name+' failed:\n'+out[-12000:])
  return out
 java=[jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m']
 run([jdk/'bin/javac','--release','8','-cp',cp,'-d',classes,source/'tests1998/strong/StrongStripe1998Test.java'],'stripe-compile')
 numerical=json.loads(run(java+['-Djava.library.path='+str(work/'retained94/standard'),'-cp',cp,'com.hiro.ulike.StrongStripe1998Test'],'stripe-program45-native-proof').split('RESULT ',1)[1])
 state=work/'state-classes';state.mkdir(exist_ok=True)
 run([jdk/'bin/javac','--release','8','-d',state,source/'GpuStrongDefault1990.java',source/'tests1998/strong/StripePeers1998.java',source/'tests1998/strong/StrongStripeState1998Test.java',source/'tests1998/strong/StrongRecovery1998Test.java'],'stripe-state-compile')
 controlled=json.loads(run(java+['-cp',state,'com.hiro.ulike.StrongStripeState1998Test'],'stripe-state-owner-proof'))
 recovery=json.loads(run(java+['-cp',state,'com.hiro.ulike.StrongRecovery1998Test'],'stripe-write-recovery-proof'))
 if recovery.get('status')!='passed' or recovery.get('assertions',0)<30:raise AssertionError('Missing stripe partial-write recovery')
 if numerical.get('status')!='passed' or numerical.get('optional_cases')!=70 or numerical.get('compared_pixels',0)<1800000:raise AssertionError('Missing complete actual program45 proof')
 if controlled.get('status')!='passed' or controlled.get('assertions',0)<40:raise AssertionError('Missing stripe owner/epoch/certificate faults')
 if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Strong stripe proof inputs changed')
 result=dict(retained)
 result.update(status='passed',assertions=retained['assertions']+numerical['assertions']+controlled['assertions']+recovery['assertions'],retained94=retained,stripe_profile6=numerical,stripe_state=controlled,stripe_recovery=recovery,profile6_program=45,profile6_cases=70,profile6_compared_pixels=numerical['compared_pixels'],profile6_bit_exact=True,profile6_independent_certificates=True,profile6_preserves_10pct_gate=True,one_heavy_candidate_per_capture=True,existing_profile7_preserved=True,physical_android_tested=False,device_speedup_verified=False,source_sha256=pins)
 (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
