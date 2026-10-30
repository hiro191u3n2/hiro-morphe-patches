#!/usr/bin/env python3
"""Run production FlickSymbols and the integrated glide control flow on host fakes."""
import argparse,hashlib,json,re,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--source-dir',type=Path,required=True);p.add_argument('--tools',type=Path,required=True);p.add_argument('--work',type=Path,required=True)
a=p.parse_args();root=a.source_dir.resolve();work=a.work.resolve();work.mkdir(parents=True,exist_ok=True)
classes=work/'classes'
if classes.exists():shutil.rmtree(classes)
classes.mkdir()
production=root/'src/hiro/swiftkey/FlickSymbols.java'
assert production.is_file(),'Missing production source'
sources=sorted((root/'tests/host_stubs').rglob('*.java'))+[production,*sorted((root/'tests/host').rglob('*.java'))]
commands=[('compile',['java','com.sun.tools.javac.Main','--release','8','-encoding','UTF-8','-g:none','-d',str(classes),*[str(s) for s in sources]]),('flick',['java','-ea','-cp',str(classes),'FlickSymbolsHostTest']),('glide',['java','-ea','-cp',str(classes),'JapaneseGlideRuntimeTest'])]
outputs={}
for name,command in commands:
 result=subprocess.run(command,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 (work/(name+'.log')).write_text(result.stdout)
 print(result.stdout,end='',flush=True)
 if result.returncode:raise SystemExit(result.returncode)
 outputs[name]=result.stdout
line=next((line[7:] for line in outputs['flick'].splitlines() if line.startswith('REPORT ')),None)
assert line is not None and 'PASS FlickSymbols' in outputs['flick'],'Missing positive flick receipt'
report=json.loads(line)
glide_count=re.search(r'PASSED (\d+) production-runtime host assertions',outputs['glide'])
assert glide_count,'Missing existing glide positive receipt'
report['existing_glide_assertions']=int(glide_count[1]);report['production_source_sha256']=hashlib.sha256(production.read_bytes()).hexdigest()
report['integrated_glide_source_sha256']=hashlib.sha256((root/'tests/host/JapaneseGlide.java').read_bytes()).hexdigest()
report['scope']='Production FlickSymbols with explicit host Android/native fakes plus existing JapaneseGlide Java control flow with the same hook semantics; not ART/device or native-engine execution'
(work/'report.json').write_text(json.dumps(report,ensure_ascii=False,sort_keys=True,indent=2)+'\n')

