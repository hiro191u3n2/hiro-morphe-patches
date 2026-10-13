#!/usr/bin/env python3
"""Verify actual recorder ownership, recovery, direct ZIP and Android export faults."""
from pathlib import Path
import argparse,hashlib,json,re,shutil,subprocess
ROOT=Path(__file__).resolve().parent
def test(source,work,jdk=None):
    source=Path(source).resolve()
    if source.is_dir():source=source/'CameraTrace1965.java'
    src=source.parent
    fixtures=src/'tests1965/trace-fixtures'
    if not fixtures.is_dir():fixtures=ROOT/'repo/releases/ulike1965-camera-trace/src/tests1965/trace-fixtures'
    if not fixtures.is_dir():raise AssertionError('Inherited Android trace boundaries unavailable')
    testfile=ROOT/'camera_agent_tests/CameraExport1967Test.java'
    work=Path(work).resolve()/'camera-export1967';classes=work/'classes';classes.mkdir(parents=True,exist_ok=True)
    data=work/'data';shutil.rmtree(data,ignore_errors=True);data.mkdir()
    historical=fixtures.parent/'TraceAudit1965.java'
    if not historical.is_file():raise AssertionError('Existing executed recorder regressions unavailable')
    original=historical.read_text()
    adapted=original.replace('initial.contains("1.9.65")','initial.contains("1.9.67")').replace('dialog.items.length==2','dialog.items.length==3')
    if original.count('initial.contains("1.9.65")')!=1 or original.count('dialog.items.length==2')!=1:raise AssertionError('Exact inherited expectation adaptation not found')
    historical_copy=work/'legacy/TraceAudit1965.java';historical_copy.parent.mkdir(exist_ok=True);historical_copy.write_text(adapted)
    inheritance={'source_sha256':hashlib.sha256(original.encode()).hexdigest(),'adapted_sha256':hashlib.sha256(adapted.encode()).hexdigest(),'expectation_adaptations':['helper process-start version 1.9.65 to 1.9.67','diagnostic menu item count 2 to 3'],'all_other_assertions_retained':True}
    clock_source=fixtures/'android/os/SystemClock.java';clock_original=clock_source.read_text()
    needle=' public static long uptimeMillis(){'
    if clock_original.count(needle)!=1:raise AssertionError('Exact controlled uptime fixture boundary absent')
    clock_fault=clock_original.replace(needle,' public static volatile boolean startupFault1967;\n'+needle+'\n  if(startupFault1967){startupFault1967=false;throw new IllegalStateException("scripted constructor first-uptime fault");}',1)
    clock_copy=work/'controlled-fixtures/android/os/SystemClock.java';clock_copy.parent.mkdir(parents=True,exist_ok=True);clock_copy.write_text(clock_fault)
    clock_report={'original_sha256':hashlib.sha256(clock_original.encode()).hexdigest(),'fault_fixture_sha256':hashlib.sha256(clock_fault.encode()).hexdigest(),'added_boundary':'one-shot first-uptime failure after owned Storage raw stream open','historical_fixture_logic_retained':True}
    java=Path(jdk).resolve()/'bin/java' if jdk else Path(shutil.which('java') or '/usr/lib/jvm/java-17-openjdk-amd64/bin/java').resolve()
    javac=java.parent/'javac'
    compiler=[str(javac)] if javac.is_file() else [str(java),'-m','jdk.compiler/com.sun.tools.javac.Main']
    sources=[source,testfile,historical_copy,clock_copy,*[p for p in sorted(fixtures.rglob('*.java')) if p!=clock_source]]
    result=subprocess.run(compiler+['-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-d',str(classes),*map(str,sources)],capture_output=True,text=True,timeout=60)
    (work/'compile.log').write_text(result.stdout+result.stderr)
    if result.returncode:raise RuntimeError((work/'compile.log').read_text())
    base=[str(java),'-Xmx128m','-cp',str(classes),'com.hiro.ulike.CameraExport1967Test']
    reports={}
    def run(mode,path,historical=False):
        label=('inherited-' if historical else '')+mode
        command=base[:-1]+['com.hiro.ulike.TraceAudit1965'] if historical else base
        result=subprocess.run(command+[mode,str(path)],capture_output=True,text=True,timeout=45)
        (work/(label+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise RuntimeError((work/(label+'.log')).read_text())
        if historical:
            match=re.search(r'checks=(\d+)',result.stdout)
            if not match:raise AssertionError('Inherited actual recorder result absent: '+mode)
            reports[label]={'status':'passed','assertions':int(match.group(1)),'physical_android_tested':False}
            return
        match=re.search(r'^RESULT (\{[^\n]+\})$',result.stdout,re.M)
        if not match:raise AssertionError('Executed recorder result absent: '+mode)
        reports[mode]=json.loads(match.group(1))
    owner=data/'owners';hold=subprocess.Popen(base+['hold',str(owner)],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
    try:
        ready=hold.stdout.readline().strip()
        if not ready.startswith('READY '):raise AssertionError('Cross-process owner failed: '+ready+' '+hold.stderr.read())
        (work/'holder.log').write_text(ready+'\n')
        run('ownership',owner)
    finally:
        if hold.poll() is None:
            hold.stdin.write('\n');hold.stdin.flush()
        stdout,stderr=hold.communicate(timeout=10)
        with (work/'holder.log').open('a') as log:log.write(stdout+stderr)
        if hold.returncode:raise RuntimeError('Cross-process owner release failed '+stderr)
    run('oversized',data/'oversized');run('ui',data/'ui');run('retry',data/'retry');run('startup',data/'startup')
    for mode in ('storage','unclean-write','unclean-read','cleanup-retry','helper'):run(mode,data/'inherited',historical=True)
    for path in data.rglob('*.jsonl'):
        raw=path.read_bytes()
        # An active foreign writer partial-tail injection intentionally remains
        # untouched; validate completed prefixes rather than alter the fixture.
        tail=raw[:raw.rfind(b'\n')+1] if raw else raw
        for line in tail.splitlines():
            if line:json.loads(line.decode('utf-8'))
    report={'status':'passed','assertions':sum(r['assertions'] for r in reports.values()),'reports':reports,'inherited_regressions':inheritance,'controlled_constructor_fault':clock_report,'physical_android_tested':False,'camera_export_regressions_passed':True,'trace_storage_regressions_passed':True,'trace_export_regressions_passed':True,'multiprocessing_writer_isolation_passed':True,'bounded_oversize_recovery_passed':True,'cross_process_owner_isolation_verified':True,'bounded_owner_slots_verified':True,'oversized_tail_recovery_verified':True,'direct_downloads_zip_verified':True,'provider_failure_cleanup_and_retry_verified':True,'dead_writer_reinit_verified':True,'constructor_failure_fd_cleanup_verified':True}
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',default=str(ROOT/'CameraTrace1965.java'));p.add_argument('--work',default=str(ROOT/'camera_agent_tests/work'));p.add_argument('--jdk');a=p.parse_args()
    print(json.dumps(test(a.source,a.work,a.jdk),indent=2))

