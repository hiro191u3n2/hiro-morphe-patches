#!/usr/bin/env python3
"""Run real EmojiPanelUi sources against explicit fake Android framework callbacks."""
import argparse,hashlib,json,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--source-dir',type=Path,required=True);p.add_argument('--tools',type=Path,required=True);p.add_argument('--work',type=Path,required=True)
a=p.parse_args();root=a.source_dir.resolve();work=a.work.resolve();work.mkdir(parents=True,exist_ok=True)
classes=work/'classes'
if classes.exists():shutil.rmtree(classes)
classes.mkdir()
production=root/'src/hiro/swiftkey/EmojiPanelUi.java'
assert production.is_file(), 'Missing production EmojiPanelUi source'
sources=sorted((root/'tests/host_stubs').rglob('*.java'))+[production,root/'tests/host/EmojiPanelUiHostTest.java']
commands=[['java','com.sun.tools.javac.Main','--release','8','-encoding','UTF-8','-g:none','-d',str(classes),*[str(s) for s in sources]],['java','-ea','-cp',str(classes),'EmojiPanelUiHostTest']]
for i,command in enumerate(commands):
 result=subprocess.run(command,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 (work/('compile.log' if i==0 else 'host.log')).write_text(result.stdout)
 print(result.stdout,end='',flush=True)
 if result.returncode:raise SystemExit(result.returncode)
line=next((line[7:] for line in result.stdout.splitlines() if line.startswith('REPORT ')),None)
assert line is not None and 'PASS EmojiPanelUi' in result.stdout,'Missing positive test receipt'
report=json.loads(line);report['production_source_sha256']=hashlib.sha256(production.read_bytes()).hexdigest()
report['scope']='Production Java runtime with deterministic fake Android views, preferences, main-loop queue and lifecycle callbacks; not an Android device test'
(work/'report.json').write_text(json.dumps(report,ensure_ascii=False,sort_keys=True,indent=2)+'\n')
