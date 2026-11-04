#!/usr/bin/env python3
import argparse,hashlib,json,subprocess,sys
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
run(['java','com.sun.tools.javac.Main','--release','8','-encoding','UTF-8','-d',classes,*[stubs[k] for k in sorted(stubs)],ROOT/'src/hiro/swiftkey/WordCandidates.java',ROOT/'src/hiro/swiftkey/SpaceConversion.java',ROOT/'tests/SpaceConversionTest.java',ROOT/'tests/StateCacheRegression.java',ROOT/'tests/ContextSelectionRegression.java'],'compile')
report=json.loads(run(['java','-cp',classes,'hiro.swiftkey.SpaceConversionTest'],'tests'));assert report['status']=='PASSED'
context=json.loads(run(['java','-cp',classes,'hiro.swiftkey.ContextSelectionRegression'],'context-tests'));assert context['status']=='PASSED'
report['context_selection']=context
baseline=a.work/'baseline';baseline.mkdir()
run(['java','com.sun.tools.javac.Main','--release','8','-encoding','UTF-8','-d',baseline,*[stubs[k] for k in sorted(stubs)],ROOT/'src/hiro/swiftkey/WordCandidates.java',ROOT/'baseline/hiro/swiftkey/SpaceConversion.java',ROOT/'tests/SpaceConversionTest.java',ROOT/'tests/StateCacheRegression.java',ROOT/'tests/ContextSelectionRegression.java'],'baseline-compile')
regressions=json.loads(run(['java','-cp',baseline,'hiro.swiftkey.ContextSelectionRegression','baseline'],'baseline-tests'))
assert regressions['status']=='PASSED' and regressions['baseline_failures_reproduced']==4
report['baseline_regressions']=regressions
fixture=ROOT/'tests/fixtures/ib0_r_g.smali';row=ROOT/'tests/fixtures/ai-dictionary-row.tsv'
assert hashlib.sha256(fixture.read_bytes()).hexdigest()=='62b9518ae9a14472198f1fcf708bdbab8e72c717781f6865c6ce3d3d9f9bc7b5'
assert hashlib.sha256(row.read_bytes()).hexdigest()=='d3f53de2165130bc5e533d73f77a315aeadc220e3e71430d37748ad738e07b9e'
native=fixture.read_text()
assert '    :goto_7\n    if-eqz v5, :cond_a' in native
assert native.index('invoke-static {p1, v3}, Lhiro/swiftkey/WordCandidates;->enhance') > native.index('    :goto_7\n')
assert native.index('invoke-virtual {v0, v3}, Ln50/a;->execute') > native.index('    :goto_7\n')
assert 'invoke-virtual {v2, v4}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;' in native
assert 'invoke-direct {v6, v3, v5}, Lib0/n;-><init>(Ll40/c;Ljava/util/List;)V' in native
cache_fixed=json.loads(run(['java','-cp',classes,'hiro.swiftkey.StateCacheRegression',row,'fixed'],'cache-fixed'))
cache_baseline=json.loads(run(['java','-cp',baseline,'hiro.swiftkey.StateCacheRegression',row,'baseline'],'cache-baseline'))
assert cache_fixed['status']==cache_baseline['status']=='PASSED'
assert cache_fixed['mode']=='fixed' and cache_baseline['mode']=='baseline'
report['cache_delivery_preservation']={'baseline':cache_baseline,'fixed':cache_fixed,
    'native_fixture_sha256':hashlib.sha256(fixture.read_bytes()).hexdigest(),
    'dictionary_row_sha256':hashlib.sha256(row.read_bytes()).hexdigest(),
    'native_branch_translated_not_android_execution':True}
native_raw=a.work/'native-raw-evidence.json'
run([sys.executable,ROOT/'tests/check_native_raw.py','--output',native_raw],'native-raw-evidence')
report['native_raw_evidence']=json.loads(native_raw.read_text())
assert report['native_raw_evidence']['status']=='PASSED'
native_separator=a.work/'native-separator-evidence.json'
run([sys.executable,ROOT/'tests/check_native_separator.py','--output',native_separator],'native-separator-evidence')
report['native_separator_evidence']=json.loads(native_separator.read_text())
assert report['native_separator_evidence']['status']=='PASSED'
(a.work/'report.json').write_text(json.dumps(report,sort_keys=True,indent=2)+'\n');print(json.dumps(report))
