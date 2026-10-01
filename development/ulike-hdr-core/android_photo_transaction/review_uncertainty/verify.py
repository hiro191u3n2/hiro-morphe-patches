#!/usr/bin/env python3
"""Independent publication uncertainty matrix against real transaction implementation.

Compiles actual Android production adapters with SDK36, then executes host-only
provider faults with real x265/FFmpeg-coded, hash-identical repeated image inputs.
Does not repeat unrelated neural-model inference or claim phone execution.
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
PINS={
 'android_jar':'d9eb9da824d9e247a352f570f01e1169e725b2954bca9e283a71786c59b59f9a',
 'ort_classes':'65e2e2d76d672253aaf0792a06c71cc40d09b0e2e251ccf925bee7175b91a1b0',
 'd8':'900dfbc649519969fc5a4c7520d6b7355338e565fa1249874e0190b8d61b1199',
}
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def run(args):
    result=subprocess.run(list(map(str,args)),capture_output=True,text=True,env={**os.environ,'ORT_DISABLE_TELEMETRY':'1'})
    if result.returncode:raise RuntimeError(result.stdout+'\n'+result.stderr)
    return result.stdout.strip()
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ('jdk-bin','android-jar','ort-classes','d8'):parser.add_argument('--'+name,type=Path,required=True)
    parser.add_argument('--report',type=Path,default=HERE/'INDEPENDENT_REVIEW.json')
    args=parser.parse_args()
    for name,pin in PINS.items():
        if sha(getattr(args,name))!=pin:raise RuntimeError('Pinned tool mismatch: '+name)
    roots=['android_gainmap_save/src/main/java','android_gainmap_codec/src/main/java','hdr_input/src/main/java',
           'android_beauty_image/src','android_model_runtime/src','android_hdr_beauty/src','android_hdr_color/src/main/java',
           'style_pipeline/src/main/java','face_analysis/src/main/java']
    production=sorted((MODULE/'src/main/java').rglob('*.java'))
    deps=sorted(f for d in roots for f in (CORE/d).rglob('*.java'))
    test=[HERE/'PublicationReview.java',MODULE/'src/test/java/com/hiro/ulike/hdr/gainmap/PhotoTestCodecFactory.java',
          CORE/'android_gainmap_save/src/test/java/com/hiro/ulike/hdr/gainmap/GainmapSaveTest.java']
    inventory=production+deps+test+[Path(__file__).resolve()]
    before={str(f.relative_to(CORE)):sha(f) for f in inventory}
    with tempfile.TemporaryDirectory(prefix='ulike-publication-independent-') as temp:
        work=Path(temp);classes=work/'classes';classes.mkdir()
        cp=os.pathsep.join(map(str,(args.android_jar,args.ort_classes)))
        run([args.jdk_bin/'javac','--release','8','-cp',cp,'-d',classes,*production,*deps])
        jar=work/'production.jar';run([args.jdk_bin/'jar','cf',jar,'-C',classes,'.'])
        dex=work/'dex';dex.mkdir()
        run([args.jdk_bin/'java','-cp',args.d8,'com.android.tools.r8.D8','--min-api','33','--lib',args.android_jar,'--classpath',args.ort_classes,'--output',dex,jar])
        dex_bytes=(dex/'classes.dex').stat().st_size
        run([args.jdk_bin/'javac','--release','8','-cp',os.pathsep.join(map(str,(classes,args.android_jar,args.ort_classes))),'-d',classes,*test])
        result=json.loads(run([args.jdk_bin/'java','-Xmx128m','-cp',classes,'com.hiro.ulike.hdr.photo.PublicationReview',work/'matrix']))
    after={str(f.relative_to(CORE)):sha(f) for f in inventory}
    if before!=after:raise RuntimeError('Reviewed source changed during tests')
    report={
      'status':'PASS_INDEPENDENT_PUBLICATION_UNCERTAINTY_REVIEW',
      'independent_tests':result,'sdk36_compile':True,'d8_min_api':33,'dex_bytes':dex_bytes,
      'reviewed_file_sha256':before,
      'tool_sha256':{str(getattr(args,n)):sha(getattr(args,n)) for n in PINS},
      'coverage':[
        'Independent 7 publication outcomes by 10 post-publication inspection outcomes; exact committed, pending, or uncertain classification.',
        'IOException, SecurityException, AssertionError, null row, wrong URI, wrong bytes, foreign owner, mismatch, reused Throwable identity.',
        'Durable checksummed PUBLISHING journal inspected inside provider publication call before side effect.',
        'Uncertain close is media-read/write-free, preserves journal and row; read-only reconcile never retries or deletes.',
        'Repeated save rejected before codec or provider after publication attempt and after provider write failure.',
        'Cancellation before publication vs after commit preserves interrupt and actual saved-state semantics.',
        'All five legacy/new v1 journal phases, both pending and visible actual rows; visible media preserved independent of stale phase name.'
      ],
      'limits':[
        'Android ContentResolver/MediaStore and Samsung handset were not executed.',
        'Only provider behavior is synthetic; two actual x265 encodes and FFmpeg decodes seed exact-input memoized codec reuse for this focused protocol matrix.',
        'No appearance, complete app integration, phone performance, filesystem power-loss, or new release verification is claimed.',
        'Core transaction read-only reconciliation verifies exact plan ownership, matching row URI, and expected byte count, not a post-publication content digest.'
      ]
    }
    args.report.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'status':report['status'],**result,'report':str(args.report)}))
if __name__=='__main__':main()
