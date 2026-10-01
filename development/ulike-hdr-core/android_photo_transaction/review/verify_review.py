#!/usr/bin/env python3
"""Independent geometry/recovery tests and rerun of actual process-crash HEIF save tests.

The production Android adapter is compiled with SDK36 and read against Android16
MediaProvider behavior. MediaProvider itself is NOT executed by this host review.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

HERE=Path(__file__).resolve().parent
MODULE=HERE.parent
CORE=MODULE.parent

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def run(args):
    result=subprocess.run(list(map(str,args)),capture_output=True,text=True)
    if result.returncode:raise RuntimeError(result.stdout+"\n"+result.stderr)
    return result.stdout.strip()

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ("jdk-bin","android-jar","ort-classes","d8"):p.add_argument("--"+name,type=Path,required=True)
    p.add_argument("--media-provider-source",type=Path,required=True)
    for name in ("models-dir","stock-libs","natural-zip","purity-zip","ort-host"):
        p.add_argument("--"+name,type=Path)
    p.add_argument("--report",type=Path,default=MODULE/"INDEPENDENT_REVIEW.json")
    args=p.parse_args()
    real_names=("models_dir","stock_libs","natural_zip","purity_zip","ort_host")
    real_options=[getattr(args,name) for name in real_names]
    if any(real_options) and not all(real_options):p.error('all five actual-model input options are required together')
    dep_roots=['android_gainmap_save/src/main/java','android_gainmap_codec/src/main/java','hdr_input/src/main/java',
               'android_beauty_image/src','android_model_runtime/src','android_hdr_beauty/src','android_hdr_color/src/main/java',
               'style_pipeline/src/main/java','face_analysis/src/main/java']
    sources=sorted((MODULE/'src/main/java').rglob('*.java'))
    deps=sorted(f for directory in dep_roots for f in (CORE/directory).rglob('*.java'))
    review=[HERE/'PhotoReview.java',Path(__file__).resolve(),MODULE/'verify.py']+sorted((MODULE/'src/test/java').rglob('*.java'))
    inventory=sources+deps+review
    before={str(f.relative_to(CORE)):sha(f) for f in inventory}
    with tempfile.TemporaryDirectory(prefix='ulike-independent-photo-') as directory:
        work=Path(directory);classes=work/'classes';classes.mkdir()
        cp=os.pathsep.join(map(str,(args.android_jar,args.ort_classes)))
        run([args.jdk_bin/'javac','--release','8','-cp',cp,'-d',classes,*deps,*sources,HERE/'PhotoReview.java'])
        independent=json.loads(run([args.jdk_bin/'java','-Xmx128m','-cp',classes,'com.hiro.ulike.hdr.photo.PhotoReview',work/'focused']))
        author_report=work/'author-rerun.json'
        command=[os.sys.executable,MODULE/'verify.py','--android-jar',args.android_jar,'--ort-classes',args.ort_classes,
                 '--jdk-bin',args.jdk_bin,'--d8',args.d8,'--report',author_report]
        if all(real_options):
            for name,value in zip(real_names,real_options):command.extend(['--'+name.replace('_','-'),value])
        rerun=json.loads(run(command));actual=json.loads(author_report.read_text())
    after={str(f.relative_to(CORE)):sha(f) for f in inventory}
    if before!=after:raise RuntimeError('Reviewed source changed during verification')
    report={
        'status':'PASS_INDEPENDENT_PHOTO_TRANSACTION_REVIEW','independent_tests':independent,
        'sdk36_compile':True,'d8_min_api':33,'author_process_and_codec_rerun':rerun,
        'restart_evidence':actual['restart'],'completed_heif_files':actual['files'],
        'actual_model_photo_integration':actual.get('actual_model_photo_integration',[]),
        'actual_model_quality_rejection':actual.get('actual_model_quality_rejection',[]),
        'android_media_store_executed':False,'samsung_device_tested':False,
        'resolved_findings':[
            'Corrupt journal or failed provider cleanup now reports an aggregated error after unrelated valid transactions have also been recovered.',
            'Private cleanup validates directory contents and preserves journal until last removal, allowing diagnosis/retry after failure.'
        ],
        'coverage':[
            'Independent forward rotation then upright mirror then crop oracle, all8 rotation/mirror combinations; same FP64 mapping for SDR and HDR.',
            'Cross64-pixel-block span integrity rejects tampering even when a full row was already cached; bounds and sentinel padding checked.',
            'Corrupt+valid journal coexistence, provider deletion failure with unrelated valid work, active process locks, retry after failure.',
            'Only exact owned matching pending rows deleted; published, foreign-owner and mismatched rows preserved.',
            'Real separate JVM process halts after MediaStore-contract insert, complete write and publication; new-process journal recovery.',
            'Actual Main10 HEIF outputs independently decoded with libheif and reconstructed from serialized metadata in rerun.',
            'Android16 MediaProvider source confirms exact image-item URIs include pending rows for update/delete and owner WHERE filtering is restricted to self-owned packages.'
        ],
        'reviewed_file_sha256':before,
        'tool_sha256':{f.name:sha(f) for f in (args.android_jar,args.ort_classes,args.d8,args.jdk_bin/'java',args.jdk_bin/'javac')},
        'android_reference':{
            'url':'https://android.googlesource.com/platform/packages/providers/MediaProvider/+/refs/heads/android16-release/src/com/android/providers/media/MediaProvider.java',
            'downloaded_source_sha256':sha(args.media_provider_source),
            'checked_behavior':['IMAGES_MEDIA_ID sets MATCH_INCLUDE for pending rows','OWNER_PACKAGE_NAME selection filtering allows self-owned packages']
        },
        'primary_sources':[
            'https://developer.android.com/reference/android/provider/MediaStore#QUERY_ARG_MATCH_PENDING',
            'https://developer.android.com/reference/android/provider/MediaStore.MediaColumns#IS_PENDING',
            'https://developer.android.com/reference/android/os/ParcelFileDescriptor#getStatSize()'
        ],
        'limits':[
            'Host gallery implements the pending/published protocol; actual Android ContentResolver/MediaProvider was not run.',
            'Crash evidence concerns process termination, not arbitrary power-loss filesystem durability.',
            'No UI/camera capture integration or native ULike appearance equivalence is established by this review.',
            'Actual-model integration fixtures use synthetic P010 and synthetic resolved remaining-style bindings; they do not prove real-scene appearance or handset performance.',
            'Geometry tile workspace excludes caller data and separate stored-row/hash/codec allocations.'
        ],
        'reproduction':'python review/verify_review.py --jdk-bin <JDK21bin> --android-jar <SDK36android.jar> --ort-classes <ORT1.30.0classes.jar> --d8 <r8-8.3.37.jar> --media-provider-source <official Android16 MediaProvider.java>'
    }
    args.report.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'status':report['status'],'independent_tests':independent,'rerun':rerun,'report':str(args.report)}))

if __name__=='__main__':main()
