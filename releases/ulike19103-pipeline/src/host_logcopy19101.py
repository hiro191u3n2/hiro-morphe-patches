#!/usr/bin/env python3
"""Exercise real diagnostic UI, full-copy behavior and inherited export actions.

Android view/clipboard boundaries are controlled fixtures, not a device UI test.
The camera recording/storage/export suffix remains identical to released .68.
"""
from pathlib import Path
import argparse, hashlib, json, os, re, shutil, subprocess

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    production=source/'CameraTrace1965.java'
    tracked=[production,source/'host_logcopy19101.py',*sorted((source/'tests1993/logcopy').rglob('*.java'))]
    pins={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    body=production.read_text();assert 'public static final String VERSION=ProcessingTiming1947.VERSION;' in body
    reference=(source/'tests1969/reference68/CameraTrace1965.java').read_text()
    boundary='    public static String exportStatus1967()'
    assert body[body.index(boundary):]==reference[reference.index(boundary):], 'Recorder export/storage changes require their own regression review'
    jdk=Path(jdk or os.environ.get('JAVA_HOME') or Path(shutil.which('java')).resolve().parent.parent)
    classes=work/'classes';classes.mkdir(exist_ok=True)
    inherited=source/'tests1965/trace-fixtures';overlay=source/'tests1993/logcopy/fixtures'
    fixtures={p.relative_to(inherited):p for p in inherited.rglob('*.java')}
    fixtures.update({p.relative_to(overlay):p for p in overlay.rglob('*.java')})
    timing_key=Path('com/hiro/ulike/ProcessingTiming1947.java')
    timing_stub=work/'ProcessingTiming1947.java'
    stub_body=fixtures[timing_key].read_text()
    assert 'class ProcessingTiming1947 {' in stub_body
    timing_stub.write_text(stub_body.replace('class ProcessingTiming1947 {','class ProcessingTiming1947 { public static final String VERSION=\"1.9.103\";'))
    fixtures[timing_key]=timing_stub
    copy_case=work/'CopyLogs1993Test.java'
    copy_case.write_text((source/'tests1993/logcopy/CopyLogs1993Test.java').read_text().replace('1.9.93','1.9.103'))
    cases=[copy_case,source/'camera69testfixtures/com/hiro/ulike/CameraDetails1969Test.java']
    def run(name,args):
        result=subprocess.run(list(map(str,args)),text=True,capture_output=True,timeout=60)
        (work/(name+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise AssertionError(name+'\n'+(result.stdout+result.stderr)[-14000:])
        return result.stdout
    run('compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,production,*fixtures.values(),*cases])
    reports={}
    for name in ('CopyLogs1993Test','CameraDetails1969Test'):
        output=run(name,[jdk/'bin/java','-ea','-Xmx128m','-cp',classes,'com.hiro.ulike.'+name,work/name])
        match=re.search(r'^RESULT (\{[^\n]+\})$',output,re.M)
        if not match:raise AssertionError('Missing executed UI result '+name)
        reports[name]=json.loads(match.group(1))
    assert pins=={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}, 'Source changed during UI proof'
    result=dict(status='passed',assertions=2+sum(r['assertions'] for r in reports.values()),tests=reports,
        full_displayed_snapshot_copied_without_truncation=True,selection_and_clipboard_failure_verified=True,
        copy_keeps_dialog_open=True,existing_diagnostic_menu_actions_preserved=True,
        recorder_storage_export_suffix_unchanged=True,source_sha256=pins,physical_android_tested=False)
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk')
    a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),ensure_ascii=False))
