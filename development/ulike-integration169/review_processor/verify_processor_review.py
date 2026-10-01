#!/usr/bin/env python3
"""Independent current-source coordinator compile and pure-Java state/settings review.

Does not build an APK, install a processor, initialize native code, access Android UI,
publish a real photo, or assert native calibration/replay/graph completeness.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

os.environ['ORT_DISABLE_TELEMETRY']='1'
HERE=Path(__file__).resolve().parent
MODULE=HERE.parent
CORE=MODULE.parent/'hdr_rebuild167/core'

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def run(command):
    completed=subprocess.run(list(map(str,command)),capture_output=True,text=True)
    if completed.returncode:raise RuntimeError(completed.stdout+'\n'+completed.stderr)
    return completed.stdout.strip()

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--tools',type=Path,required=True)
    parser.add_argument('--jdk-bin',type=Path,required=True)
    parser.add_argument('--report',type=Path,default=MODULE/'PROCESSOR_INDEPENDENT_REVIEW.json')
    args=parser.parse_args();tools=args.tools;jdk=args.jdk_bin
    sources=[]
    for path in CORE.rglob('*.java'):
        if 'src' not in path.parts or any(x in path.parts for x in ('test','tests','review','android_runtime_package','native_geometry_contract','qa')) or any(x.startswith('test-') for x in path.parts):continue
        sources.append(path.resolve())
    sources+=list((MODULE/'runtime/src').rglob('*.java'))
    sources=sorted(sources)
    tests=[HERE/'SequenceReview.java',HERE/'CapturedSettingsReview.java']
    files=sources+tests+[Path(__file__).resolve(),MODULE/'compile_stubs/com/hiro/ulike/OpticalZoom.java']
    before={str(path.relative_to(MODULE.parent)):sha(path) for path in files}
    with tempfile.TemporaryDirectory(prefix='ulike-independent-processor-') as folder:
        folder=Path(folder);classes=folder/'classes';classes.mkdir()
        cp=os.pathsep.join(map(str,(tools/'android.jar',tools/'onnxruntime-android-1.30.0-classes.jar')))
        run([jdk/'javac','--release','8','-cp',cp,'-d',classes,*sources,MODULE/'compile_stubs/com/hiro/ulike/OpticalZoom.java'])
        jar=folder/'production.jar';run([jdk/'jar','cf',jar,'-C',classes,'.'])
        dex=folder/'dex';dex.mkdir()
        run([jdk/'java','-cp',tools/'r8-8.3.37.jar','com.android.tools.r8.D8','--min-api','26','--lib',tools/'android.jar','--classpath',tools/'onnxruntime-android-1.30.0-classes.jar','--output',dex,jar])
        run([jdk/'javac','--release','8','-cp',classes,'-d',classes,*tests])
        host={}
        for name,clazz in [('sequence','review.SequenceReview'),('settings','com.hiro.ulike.binding.CapturedSettingsReview')]:
            host[name]=json.loads(run([jdk/'java','-Xmx128m','-cp',classes,clazz]))
        dex_bytes=(dex/'classes.dex').stat().st_size
    after={str(path.relative_to(MODULE.parent)):sha(path) for path in files}
    if before!=after:raise RuntimeError('Reviewed source changed during execution: '+', '.join(k for k in before if before[k]!=after[k]))
    report={
        'status':'PASS_DECLARED_COORDINATOR_SOURCE_AND_HOST_STATE_SETTINGS_REVIEW',
        'production_java_files':len(sources),'sdk36_compile':True,'d8_min26':True,'classes_dex_bytes':dex_bytes,
        'excluded_unconnected_core_modules':['native_geometry_contract'],
        'host':host,'reviewed_file_sha256':before,
        'resolved_review_findings':[
            'OwnedShot sensor crop is private with a defensive-copy accessor; shutter date is frozen once at Choice creation.',
            'NativeBindings neural list is copied with bounded iteration; every observed SDK face ID must match exactly once.',
            'Plan expected feature identifiers are bounded, copied and immutable; complete resolved graph settings are copied.',
            'Work.close attempts pair, transaction, owned analysis input and plan even if an earlier close throws Error, retaining suppressed failures.',
            'ProcessingSequence rejects overlapping ownership, cancels only the exact accepted shot, runs cleanup with a pending interrupt cleared, and lets a successful committed URI win late cancellation.'
        ],
        'reviewed_dataflow':[
            'Exact OwnedShot P010/rendition/geometry and shutter style identity are required before analysis.',
            'CapturedSettings canonicalizes successful requested composer values plus caller-provided verified replay bytes; serialization alone proves no native replay.',
            'Actual AnalysisInput -> PausedStockPreview -> AndroidAnalysisInput requires owned source binding and verified preview restoration before returning.',
            'Native binding adapter must provide every observed face and complete ordered graph settings; no default/fallback adapter exists.',
            'Ordered neural preflight occurs before opening the actual Android inference engine; generated layers retain exact same rendition/style.',
            'Processed HDR and matching SDR enter GeometryPairWriter under one PhotoIdentity before saved-URI handoff.',
            'After a known committed URI, saved-URI handoff reports subsequent inspection/UI errors with that URI and blocks ordinary image-error callbacks.'
        ],
        'limitations':[
            'This review does not execute the full coordinator on Android or exercise actual Camera2, native composer, SDK/ORT, MediaCodec, MediaStore or application UI.',
            'CandidateGate169 remains hard false; no processor, native plan provider or full binding provider is installed.',
            'Our diagnostic bridge imposes a conservative <=4194304-pixel guard and currently rejects 4080x3060 before preview interruption. This guard is not an established native SDK/sensor limit; a reviewed full-resolution ownership route and device calibration remain unresolved.',
            'Native face order, geometry/calibration, complete replay, restore barrier and all-mode lens execution still need actual implementations/evidence.',
            'If MediaStore publication occurs but the underlying PhotoTransaction confirming inspect fails before returning a URI, commit status is not confirmed; cleanup preserves visible rows but this review does not promise successful URI delivery for that unresolved provider-failure window.',
            'An Error raised by final post-commit cleanup is propagated after owner release; the already committed photo is not removed.',
            'Automatic-save route only; original Bitmap editor, bursts and full mode support remain unsupported.'
        ],
        'native_initialization':False,'android_device_execution':False,'real_photo_publication':False,
        'tool_sha256':{name:sha(tools/name) for name in ('android.jar','onnxruntime-android-1.30.0-classes.jar','r8-8.3.37.jar')},
        'jdk_sha256':{name:sha(jdk/name) for name in ('java','javac')}
    }
    args.report.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'status':report['status'],'host':host,'report_sha256':sha(args.report)}))

if __name__=='__main__':main()
