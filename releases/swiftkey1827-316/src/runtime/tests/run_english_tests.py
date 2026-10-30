#!/usr/bin/env python3
"""Run English regression and retained Japanese/sensitivity host suites."""
import argparse,hashlib,json,re,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--source-dir',type=Path,required=True);p.add_argument('--work',type=Path,required=True)
a=p.parse_args();root=a.source_dir.resolve();work=a.work.resolve();work.mkdir(parents=True,exist_ok=True)
def run(command,name):
 r=subprocess.run(list(map(str,command)),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
 (work/(name+'.log')).write_text(r.stdout);print(r.stdout,end='',flush=True)
 if r.returncode:raise RuntimeError('Host test failed: '+name)
 return r.stdout
def receipt(out):
 return json.loads(next(s[7:] for s in out.splitlines() if s.startswith('REPORT ')))
reports={}
for kind in ['baseline','fixed']:
 classes=work/kind;classes.mkdir()
 production=root/('baseline' if kind=='baseline' else 'src')/'hiro/swiftkey/FlickSymbols.java'
 sources=sorted((root/'tests/host_stubs').rglob('*.java'))+sorted((root/'tests/host').rglob('*.java'))+[production]
 run(['java','com.sun.tools.javac.Main','--release','8','-g:none','-encoding','UTF-8','-d',classes,*sources],kind+'-javac')
 reports[kind]=receipt(run(['java','-ea','-cp',classes,'EnglishFlickHostTest',kind],kind+'-english'))
 if kind=='fixed':
  for test in ['FlickSymbolsHostTest','SensitivityHostTest','SensitivityFloorRegressionTest']:
   reports[test]=receipt(run(['java','-ea','-cp',classes,test],test))
  out=run(['java','-ea','-cp',classes,'JapaneseGlideRuntimeTest'],'JapaneseGlideRuntimeTest')
  reports['JapaneseGlideRuntimeTest']={'assertions':int(re.search(r'PASSED (\d+) production-runtime host assertions',out)[1])}
report={'status':'PASSED','device_tested':False,'android_instrumentation_tested':False,
 'english_upflick_enabled':True,'japanese_upflick_preserved':True,'gesture_safety_tests_passed':True,
 'baseline_english_failure_reproduced':reports['baseline']['baseline_english_failure_reproduced'],
 'scenarios':sum(v.get('scenarios',0) for v in reports.values()),'assertions':sum(v['assertions'] for v in reports.values()),
 'production_source_sha256':hashlib.sha256((root/'src/hiro/swiftkey/FlickSymbols.java').read_bytes()).hexdigest(),
 'baseline_source_sha256':hashlib.sha256((root/'baseline/hiro/swiftkey/FlickSymbols.java').read_bytes()).hexdigest(),
 'suites':reports,'scope':'Production gesture source and dispatcher using explicit Android/native host fakes; no native Flow engine or device execution.'}
(work/'report.json').write_text(json.dumps(report,sort_keys=True,indent=2)+'\n')
