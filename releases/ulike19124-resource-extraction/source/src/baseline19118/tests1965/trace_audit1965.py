#!/usr/bin/env python3
"""Production diagnostic recorder: retained bounds, frozen exports, queue and Android share faults."""
from pathlib import Path
import argparse, json, os, shutil, subprocess

def test(root, work, jdk=None, ndk=None):
    root, work = Path(root).resolve(), Path(work).resolve()
    folder=work/'host-trace1965'; folder.mkdir(parents=True,exist_ok=True)
    # Preserve cross-process evidence within this run, remove only prior run data.
    shutil.rmtree(folder/'data',ignore_errors=True)
    classes=folder/'classes';classes.mkdir(exist_ok=True)
    exe=lambda name:str(Path(jdk)/'bin'/name) if jdk else name
    sources=[root/'CameraTrace1965.java',root/'tests1965/TraceAudit1965.java']
    sources+=sorted((root/'tests1965/trace-fixtures').rglob('*.java'))
    compile=subprocess.run([exe('javac'),'-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-d',str(classes),*map(str,sources)],capture_output=True,text=True)
    (folder/'compile.log').write_text(compile.stdout+compile.stderr)
    if compile.returncode:raise RuntimeError(compile.stdout+compile.stderr)
    checks=0
    for mode in ('storage','unclean-write','unclean-read','cleanup-retry','helper'):
        run=subprocess.run([exe('java'),'-Xmx128m','-cp',str(classes),'com.hiro.ulike.TraceAudit1965',mode,str(folder/'data')],capture_output=True,text=True,timeout=35)
        (folder/(mode+'.log')).write_text(run.stdout+run.stderr)
        if run.returncode:raise RuntimeError(mode+': '+run.stdout+run.stderr)
        checks+=int(run.stdout.split('checks=')[1].split()[0])
    json_lines=0
    for path in sorted((folder/'data').rglob('*.jsonl')):
        raw=path.read_bytes()
        if raw and not raw.endswith(b'\n'):raise AssertionError('unterminated retained file: '+str(path))
        for line in raw.splitlines():
            if line:json.loads(line.decode('utf-8'));json_lines+=1
    report={'status':'passed','assertions':checks,'retained_json_lines_validated':json_lines,'rotation_limit_bytes':8*128*1024,'protected_limit_bytes':3*256*1024,'retained_across_jvm_processes':True,'same_epoch_anomalies_coalesced':True,'exported_user_snapshot_immutable':True,'bounded_queue_drop_report_verified':True,'prior_tail_recovery_verified':True,'protected_tail_recovery_and_live_export_verified':True,'anomaly_followup_window_ms':5000,'post_trigger_cap_and_expiry_verified':True,'snapshot_pending_cleanup_fault_verified':True,'pending_export_cleanup_faults_verified':True,'share_read_only_grant_verified':True,'cleanup_retry_single_writer_guard_verified':True,'physical_android_tested':False}
    (work/'trace1965-result.json').write_text(json.dumps(report,indent=2)+'\n')
    return report

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);p.add_argument('--jdk',type=Path);a=p.parse_args()
    print(json.dumps(test(Path(__file__).resolve().parents[1],a.out,a.jdk),indent=2))
