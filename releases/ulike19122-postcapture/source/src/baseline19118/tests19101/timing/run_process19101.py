#!/usr/bin/env python3
"""Focused process display integration, after retained timing fixtures compile.

The missing-provider test runs first; then the real production Journal is
mandatory and two separate JVMs transfer an immutable saved photo snapshot.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess

def focused(source,work,jdk,classes):
    source,work,jdk,classes=map(lambda p:Path(p).resolve(),(source,work,jdk,classes));work.mkdir(parents=True,exist_ok=True)
    journal=source/'VerificationJournal19101.java'
    if not journal.is_file():raise AssertionError('Real production verification journal required')
    harness=source/'tests19101/timing/TimingProcess19101Test.java';missing=source/'tests19101/timing/TimingProcessMissing19101Test.java'
    paths=[source/'ProcessingTiming1947.java',journal,harness,missing,Path(__file__).resolve()]
    pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
    def run(command,name):
        r=subprocess.run(list(map(str,command)),capture_output=True,text=True,timeout=90);text=r.stdout+r.stderr;(work/(name+'.log')).write_text(text)
        if r.returncode:raise AssertionError(name+'\n'+text[-10000:])
        return text
    # Use the already compiled narrow Android/peer fixtures. This first JVM
    # proves optional linkage failure cannot imply current-process ownership.
    if (classes/'com/hiro/ulike/VerificationJournal19101.class').exists():raise AssertionError('Run focused test with a fresh retained fixture directory')
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',classes,'-d',classes,source/'ProcessingTiming1947.java',missing],'compile-missing')
    results={}
    results['missing']=json.loads(run([jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.TimingProcessMissing19101Test'],'missing').split('RESULT ',1)[1])
    prefs=work/'fixtures/android/content/SharedPreferences.java';prefs.parent.mkdir(parents=True,exist_ok=True)
    prefs.write_text('''package android.content;
public interface SharedPreferences {
 long getLong(String key,long fallback); String getString(String key,String fallback);
 Editor edit(); interface Editor {Editor putString(String key,String value);Editor putLong(String key,long value);void apply();boolean commit();}
}''')
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',classes,'-d',classes,prefs,journal,harness],'compile-real-journal')
    snapshot=work/'previous-photo.bin'
    for mode in ('produce','consume'):
        output=run([jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-cp',classes,'com.hiro.ulike.TimingProcess19101Test',mode,snapshot],mode)
        results[mode]=json.loads(output.split('RESULT ',1)[1])
    if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}:raise AssertionError('Timing or journal changed during focused process proof')
    result=dict(status='passed',assertions=sum(r['assertions'] for r in results.values()),cases=results,real_journal_process_identity=True,saved_snapshot_process_identity=True,cross_process_photo_live_separation=True,legacy_photo_identity_unknown=True,empty_current_history_explicit=True,missing_journal_identity_unknown=True,diagnostic_snapshot_immutable=True,physical_android_tested=False,device_speedup_verified=False,source_sha256=pins)
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();jdk=Path(jdk or os.environ['ULIKE_JDK_HOME'])
    spec=importlib.util.spec_from_file_location('process101_retained',source/'host_timing19101.py');retained=importlib.util.module_from_spec(spec);spec.loader.exec_module(retained)
    prior=retained.test(source,work/'retained',jdk,ndk)
    result=focused(source,work/'process',jdk,work/'retained/classes')
    # Retained fixtures are rebuilt and re-executed as setup, but their 880
    # assertions belong to the existing timing group and are not counted twice.
    result['fixture_setup']=dict(status=prior['status'],retained_assertions_reexecuted=prior['assertions'],included_in_this_group_assertion_count=False)
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk',required=True);p.add_argument('--ndk');p.add_argument('--classes');a=p.parse_args()
    result=focused(a.source,a.work,a.jdk,a.classes) if a.classes else test(a.source,a.work,a.jdk,a.ndk)
    print(json.dumps(result,ensure_ascii=False))
