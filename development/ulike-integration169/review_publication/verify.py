#!/usr/bin/env python3
"""Independent publication ownership review. No MediaStore or Android UI execution."""
import argparse,hashlib,json,os,subprocess,tempfile
from pathlib import Path
HERE=Path(__file__).resolve().parent
APP=HERE.parent
CORE=APP.parent/'hdr_rebuild167/core'
RUNTIME=APP/'runtime/src/com/hiro/ulike/integration169'
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def run(args):
    result=subprocess.run(list(map(str,args)),capture_output=True,text=True)
    if result.returncode:raise RuntimeError(result.stdout+'\n'+result.stderr)
    return result.stdout.strip()
def main():
    p=argparse.ArgumentParser();p.add_argument('--jdk-bin',type=Path,required=True);p.add_argument('--report',type=Path,default=HERE/'INDEPENDENT_REVIEW.json');a=p.parse_args()
    files=[RUNTIME/'PublicationOutcome169.java',RUNTIME/'SavedUriHandoff169.java',RUNTIME/'CameraBridge169.java',
           CORE/'android_photo_transaction/src/main/java/com/hiro/ulike/hdr/photo/PhotoTransaction.java',HERE/'PublicationReview.java',Path(__file__).resolve()]
    before={os.path.relpath(f,APP):sha(f) for f in files}
    with tempfile.TemporaryDirectory(prefix='ulike-independent-publication-') as temp:
        run([a.jdk_bin/'javac','--release','8','-d',temp,files[0],HERE/'PublicationReview.java'])
        host=json.loads(run([a.jdk_bin/'java','-Xmx32m','-cp',temp,'com.hiro.ulike.integration169.PublicationReview']))
    assert before=={os.path.relpath(f,APP):sha(f) for f in files},'publication source changed during review'
    report={'status':'PASS_INDEPENDENT_PUBLICATION_ATTEMPT_OWNERSHIP_REVIEW','host':host,'reviewed_file_sha256':before,
      'fixed_finding':{'previous_race':'Concurrent fail() could mark unsaved while the provider save was in flight, causing eventual committed/uncertain resolution to fail as already terminal.',
                       'observed_fix':'beginSave transitions to PUBLISHING and gives one opaque SaveAttempt; external failure cannot resolve it, and only that same claim can record the save result.'},
      'manual_source_review':[
        'SavedUriHandoff acquires the SaveAttempt inside the same synchronized token check used to set saving, before transaction.savePair.',
        'External failBeforePublication has no transition from PUBLISHING, COMMITTED or PUBLICATION_UNCERTAIN.',
        'Committed and uncertain result handlers resolve with the local same SaveAttempt; foreign and terminal claims are rejected.',
        'Core transaction marks publication uncertain before provider publish, re-inspects exact identity and bytes, and retains a recovery journal if publication cannot be confirmed.',
        'Core confirmedPublishedUri becomes non-null only after an exact visible committed row is observed. Exception/Error handling consults that receipt before reporting unsaved.',
        'Uncertain path does not call image-error, save-success, last-photo assignment or automatic insert retry; real Android callbacks are not run in this review.'
      ],'blocking_findings':[],'actual_android_provider_execution':False,'actual_ui_callbacks_executed':False,
      'limitations':['The deterministic blocked operation is the real Java latch with a host synchronization gate, not a mocked result claimed as MediaStore evidence.','Provider cancellation, recovery and callback behavior still require actual Android testing.']}
    a.report.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'status':report['status'],'host':host}))
if __name__=='__main__':main()
