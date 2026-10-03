#!/usr/bin/env python3
"""Production pause/restore flow with host SDK fixtures and full-size result ownership."""
import argparse, hashlib, json, subprocess, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def run(args):
    result=subprocess.run([str(x) for x in args],capture_output=True,text=True)
    if result.returncode:raise RuntimeError(result.stdout+'\n'+result.stderr)
    return result.stdout.strip()
def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--jdk-bin',type=Path,required=True)
    parser.add_argument('--report',type=Path,default=ROOT/'QA_OUTPUT_OWNERSHIP.json')
    args=parser.parse_args()
    production=[ROOT/'src/main/java/com/hiro/ulike/hdr/stillanalysis'/name for name in ('PausedStockPreview.java','RecorderAdmission.java')]
    fixtures=sorted((ROOT/'ownership_host').rglob('*.java'))
    before={str(p.relative_to(ROOT)):sha(p) for p in production+fixtures+[Path(__file__).resolve()]}
    scenarios=('success','work-exception','work-error','restore-exception','restore-error','restore-interrupt','restore-close-exception','restore-close-error','restore-same-failure')
    with tempfile.TemporaryDirectory(prefix='ulike-paused-output-') as temp:
        classes=Path(temp)/'classes';classes.mkdir()
        run([args.jdk_bin/'javac','--release','8','-Xlint:all','-d',classes,*production,*fixtures])
        results=[json.loads(run([args.jdk_bin/'java','-Xmx80m','-cp',classes,'com.hiro.ulike.hdr.stillanalysis.PausedOutputOwnershipTest',scenario])) for scenario in scenarios]
    assert before=={str(p.relative_to(ROOT)):sha(p) for p in production+fixtures+[Path(__file__).resolve()]}
    report={'status':'PASS_HOST_PAUSE_RESTORE_UNDELIVERED_RESULT_OWNERSHIP','scenarios':results,'source_sha256':before,
        'actual_android_bitmap_execution':False,'actual_native_sdk_execution':False,'application_lifecycle_hook_coverage_verified':False,
        'limitations':['SDK lifecycle callbacks are host fixtures, not measured vendor callbacks.','An 80MiB host Java heap for the output fixture does not establish phone total memory capacity.']}
    args.report.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'status':report['status'],'scenarios':len(results),'host_checks':sum(x['checks'] for x in results)}))
if __name__=='__main__':main()
