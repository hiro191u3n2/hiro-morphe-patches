#!/usr/bin/env python3
import argparse,json,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
p=argparse.ArgumentParser();p.add_argument('--work',type=Path,required=True);a=p.parse_args();a.work.mkdir(parents=True,exist_ok=False)
classes=a.work/'classes';classes.mkdir()
def run(cmd,name):
 r=subprocess.run(list(map(str,cmd)),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True);(a.work/(name+'.log')).write_text(r.stdout)
 if r.returncode:raise RuntimeError(r.stdout)
 return r.stdout
stubs={str(f.relative_to(ROOT/'stubs')):f for f in (ROOT/'stubs').rglob('*.java')}
for folder in ('tests/ranking_stubs/android','tests/space_stubs'):
 for f in (ROOT/folder).rglob('*.java'):
  name=str(f.relative_to(ROOT/folder));name=('android/'+name) if folder.endswith('/android') else name;stubs[name]=f
# Java-only surrogate for the native romaji routine; algorithm itself is unchanged.
japanese=a.work/'Japanese.java';japanese.write_text('package com.microsoft.fluency; public final class Japanese { public static String romajiToHiragana(String s){return "kyou".equalsIgnoreCase(s)?"きょう":s;} }')
stubs['com/microsoft/fluency/Japanese.java']=japanese
run(['java','com.sun.tools.javac.Main','--release','8','-encoding','UTF-8','-d',classes,*[stubs[k] for k in sorted(stubs)],ROOT/'src/hiro/swiftkey/WordCandidates.java',ROOT/'src/hiro/swiftkey/SpaceConversion.java',ROOT/'tests/SpaceConversionTest.java'],'compile')
report=json.loads(run(['java','-cp',classes,'hiro.swiftkey.SpaceConversionTest'],'tests'));assert report['status']=='PASSED'
(a.work/'report.json').write_text(json.dumps(report,sort_keys=True,indent=2)+'\n');print(json.dumps(report))
