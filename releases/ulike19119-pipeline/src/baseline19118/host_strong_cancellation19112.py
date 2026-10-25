#!/usr/bin/env python3
"""Bounded optional proof cancellation; full v111 fixture/compare equivalence.

Native CPU already polls interruption every 16 rows. Its source and the GPU
submit/collect/drain code are deliberately untouched by this change. This proof
does not claim submitted GPU work can be preempted or that device drain latency
is bounded by these CPU loop checkpoints.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess,sys
ROOT=Path(__file__).resolve().parent

UNCHANGED_NUMERICAL_PINS={'StrongNoise1958.java': '0adad39df071e47c58bc47d99e39ff1725ddb11c923ed3144c9c7aaaf74e33b4', 'native1958/smooth_noise1958.c': 'f0d6d2cd68d5f55194d96a2268177ade0e634c79849120e1aa16061167d854d6', 'native1958/scratch1959.h': '9b61f46bc2b0b4e3c86ea19a88f2719166e0611abbb4e3dea9328a7580c5bb9e', 'native1958/neon1959.h': '0f5f589a2d641e0929541a0c509af0212ddb8ec15e2a8b87b2f11b6d9d917b04'}
UNCHANGED_DRAIN_SEGMENTS=[('GpuStrong1960.java', '    static Read1971 ephemeral1973(', '    /** Private post-save arithmetic observations.', '2b8c8f265beda6dcded3b972fcaaa17be7e1320d629193ea99b64f2f5fd893e7'), ('GpuNoise1960.java', '        /** GX19 bounded two-bank submission:', '        /** .91 release only obsolete whole-route slots.', '5ebd75db5f8001bf09db9f9e1ba84abf347866f87aa561b0184e5990fee3740c')]
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 sys.path.insert(0,str(source));import build1992
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);android=Path(os.environ['ULIKE_ANDROID_JAR'])
 fixtures=source/'tests19112/cancellation';owned=[source/'GpuStrongDefault1990.java',source/'cancellation_inverse19112.py',source/'source_inverse19115.py',source/'source_inverse19115_manifest.json',source/'host_strong_cancellation19112.py',*fixtures.rglob('*.java')]
 pins={str(p.relative_to(source)):sha(p) for p in owned};commands=[]
 # Historical numerical pins are checked after the exact reviewed whole-source
 # inverses. Current Java/native inputs remain the compile inputs below.
 from source_inverse19115 import REVIEWED,restore as restore115
 for name,digest in UNCHANGED_NUMERICAL_PINS.items():
  raw=(source/name).read_bytes()
  if name in REVIEWED:raw=restore115(source,name,raw)
  if hashlib.sha256(raw).hexdigest()!=digest:raise AssertionError('cancellation change unexpectedly altered numerical source '+name)
 from source_inverse19117 import REVIEWED as REVIEWED117,restore_to116
 for name,start,end,digest in UNCHANGED_DRAIN_SEGMENTS:
  raw=(source/name).read_bytes()
  # The new observed readback branch lives inside the older GpuNoise drain
  # segment. Compare the exact published116 source after fail-closed inverse;
  # the current branch is exercised by the real-JNI Strong transport tests.
  text=(restore_to116(source,name,raw) if name in REVIEWED117 else raw).decode()
  if text.count(start)!=1 or text.count(end)!=1 or hashlib.sha256(text[text.index(start):text.index(end)].encode()).hexdigest()!=digest:raise AssertionError('GPU ownership/drain unexpectedly changed '+name)
 def run(argv,name):
  argv=list(map(str,argv));commands.append(dict(name=name,argv=argv));result=subprocess.run(argv,capture_output=True,text=True,timeout=300)
  output=result.stdout+result.stderr;(work/(name+'.log')).write_text(output)
  if result.returncode:raise RuntimeError(name+'\n'+output[-12000:])
  return output
 classes=work/'classes';classes.mkdir(exist_ok=True)
 sources=list(dict.fromkeys(build1992.compile_inputs([]).values()))
 # Current build declarations may grow new production helpers in this release.
 # Select all concrete root Java helpers as well, while preserving unique names.
 selected=list(sources)
 for p in source.glob('*.java'):
  if p.name.endswith('19112.java') and p not in selected:selected.append(p)
 run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',android,'-d',classes,*selected,fixtures/'StrongCancellation19112Test.java'],'compile-current-production')
 cp=str(classes)+os.pathsep+str(android);java=[jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-Xmx768m']
 current=json.loads(run(java+['-cp',cp,'com.hiro.ulike.StrongCancellation19112Test'],'current-fixtures-cancellation').split('RESULT ',1)[1])
 baseline=work/'baseline';baseline.mkdir(exist_ok=True)
 run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',baseline,fixtures/'reference111/GpuStrongDefault1990.java'],'compile-published111-fixture')
 old=json.loads(run(java+['-cp',str(baseline)+os.pathsep+cp,'com.hiro.ulike.StrongCancellation19112Test','baseline'],'published111-fixtures').split('RESULT ',1)[1])
 if current['fixtures']!=70 or old['fixtures']!=70 or current['corpus_sha256']!=old['corpus_sha256'] or current['comparison_sha256']!=old['comparison_sha256']:raise AssertionError('published111 optional corpus or exact compare changed')
 if current['cancelled_cases']!=18:raise AssertionError('optional CPU cancellation cases incomplete')
 observed={}
 seam='                int guide=(i*7+id*13)&255,'
 run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',classes,fixtures/'StrongCancellationObserved19112Test.java'],'compile-observed-interrupt-harness')
 for label,path,wanted in [('published111',fixtures/'reference111/GpuStrongDefault1990.java',False),('current112',source/'GpuStrongDefault1990.java',True)]:
  code=path.read_text()
  if code.count(seam)!=1:raise AssertionError('interrupt observer map seam drift')
  copied=work/('observed-'+label);copied.mkdir(exist_ok=True)
  (copied/'GpuStrongDefault1990.java').write_text(code.replace(seam,'                if(i==4096)Thread.currentThread().interrupt();\n'+seam))
  run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',copied,copied/'GpuStrongDefault1990.java'],'compile-interrupt-'+label)
  observed[label]=json.loads(run(java+['-cp',str(copied)+os.pathsep+cp,'com.hiro.ulike.StrongCancellationObserved19112Test',str(wanted).lower()],'interrupt-'+label))
 state=work/'state';state.mkdir(exist_ok=True)
 run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',state,source/'GpuStrongDefault1990.java',fixtures/'CancellationPeers19112.java',fixtures/'StrongCancellationState19112Test.java'],'compile-production-state')
 controlled=json.loads(run(java+['-cp',state,'com.hiro.ulike.StrongCancellationState19112Test'],'cancel-certificate-progress'))
 if controlled['cancelled_qualifiers']!=4 or controlled['partial_proofs_written']!=0 or controlled['completed_oracle_calls']!=70 or controlled['completed_gpu_submits']!=80:raise AssertionError('optional state/ownership/corpus test incomplete')
 spec=importlib.util.spec_from_file_location('strong_cancellation_inverse',source/'cancellation_inverse19112.py');inverse=importlib.util.module_from_spec(spec);spec.loader.exec_module(inverse)
 text=(source/'GpuStrongDefault1990.java').read_text();restored=inverse.restore_text(text,source)
 if restored!=(fixtures/'reference111/GpuStrongDefault1990.java').read_text():raise AssertionError('strict v111 restoration failed')
 from source_inverse19115 import restore as restore115
 text=restore115(source,'GpuStrongDefault1990.java',text.encode()).decode()
 # Historical cancellation mutants must reach its original guard.
 mutations=[('id<CASES','id<CASES-1'),('baselineBest/10','baselineBest/9'),('cancellation.cancelled()','false'),('if((i&4095)==0)checkOptional19112(cancellation);',''),('check(cancellation);long start=System.nanoTime();','long start=System.nanoTime();'),('random*1664525L','random*1664527L')]
 for oldText,newText in mutations:
  if oldText not in text:raise AssertionError('missing inverse mutation seam '+oldText)
  try:inverse._restore114_text(text.replace(oldText,newText,1),source)
  except AssertionError:pass
  else:raise AssertionError('inverse accepted behavioral drift '+oldText)
 if pins!={str(p.relative_to(source)):sha(p) for p in owned}:raise AssertionError('cancellation proof sources changed during execution')
 result=dict(status='passed',assertions=current['assertions']+old['assertions']+controlled['assertions']+len(mutations)+1+sum(r['assertions'] for r in observed.values()),fixture_cases=70,cancellation_cases=18,cancelled_qualifiers=4,inverse_mutation_negatives=6,historical_negative_scope19115='restored114_source',historical_numerical_source_scope19115='strict115_to_published114',current_numerical_source_executed19115=True,fixture_corpus_bit_exact=True,old_unchecked_map_interrupt_reproduced=True,exact_comparison_unchanged=True,full_256_row_speed_trials_preserved=True,partial_certificate_writes_zero=True,diagnostic_failure_nonfatal=True,checkpoints_outside_timed_gpu_calls=True,native_and_gpu_drain_unchanged=True,drain_source_scope19117='strict117_to_published116',published111_restored_exactly=True,physical_android_tested=False,device_speedup_verified=False,current=current,baseline=old,controlled=controlled,observed_interrupt=observed,source_sha256=pins)
 (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
