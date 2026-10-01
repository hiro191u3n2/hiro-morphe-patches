#!/usr/bin/env python3
"""Read-only independent review of the deliberately disabled candidate APKs.

This compiles production bridge code against SDK36, checks old runtime DEX
preservation, reruns exact APK payload/resource contracts, and freshly decodes
each output manifest. It neither runs an Android device nor enables the path.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
import xml.etree.ElementTree as ET
import zipfile

HERE=Path(__file__).resolve().parent
MODULE=HERE.parent
ANDROID='{http://schemas.android.com/apk/res/android}'

def sha(path):
    h=hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda:stream.read(1024*1024),b''):h.update(block)
    return h.hexdigest()

def run(args):
    result=subprocess.run(list(map(str,args)),text=True,capture_output=True)
    if result.returncode:raise RuntimeError(result.stdout+'\n'+result.stderr)
    return result.stdout.strip()

def manifest(apk,work,jdk,tools):
    # Give the official binary manifest decoder only the two files it needs;
    # no executable/native payload is loaded or run during this operation.
    minimal=work/'manifest-only.apk'
    with zipfile.ZipFile(apk) as source,zipfile.ZipFile(minimal,'w') as dest:
        for name in ('AndroidManifest.xml','resources.arsc'):dest.writestr(name,source.read(name))
        binary_sha=hashlib.sha256(source.read('AndroidManifest.xml')).hexdigest()
    decoded=work/'decoded'
    run([jdk/'java','-jar',tools/'apktool.jar','d','-f','-r','-s','--force-manifest','--no-assets','-o',decoded,minimal])
    root=ET.parse(decoded/'AndroidManifest.xml').getroot();app=root.find('application')
    if app is None:raise ValueError('application missing')
    providers=[p.get(ANDROID+'name','') for p in app.findall('provider')]
    if root.get('package')!='com.gorgeous.liteinternational':raise ValueError('wrong output package')
    if app.get(ANDROID+'extractNativeLibs')!='true':raise ValueError('ORT native extraction not enabled')
    if any('onnx' in name.lower() or 'ort'==name.lower() for name in providers):raise ValueError('unexpected ORT startup provider')
    return {'binary_manifest_sha256':binary_sha,'fresh_apktool_decode':True,
            'package':root.get('package'),'application':app.get(ANDROID+'name'),
            'extractNativeLibs':True,'onnx_startup_provider_present':False,'provider_count':len(providers)}

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('tools','jdk-bin','apks'):p.add_argument('--'+name,type=Path,required=True)
    for kind in ('standalone','integrated'):
        for item in ('apk','mpp','result'):p.add_argument('--'+kind+'-'+item,type=Path,required=True)
    p.add_argument('--report',type=Path,default=MODULE/'INDEPENDENT_REVIEW.json')
    args=p.parse_args();tools=args.tools;jdk=args.jdk_bin;build=MODULE/'build'
    runtime_sources=sorted((MODULE/'runtime/src').rglob('*.java'))
    own_sources=[HERE/'DisabledBridgeReview.java',HERE/'OpticalZoom.java',HERE/'ArtifactReview169.java',Path(__file__).resolve()]
    helper_sources=sorted((MODULE/'integration').glob('*.java'))+sorted((MODULE/'tools').glob('*.java'))
    artifacts=[build/'runtime.dex',build/'old/ulike/runtime.dex',build/'methods.tsv',MODULE/'runtime/helpers.jar',
        MODULE/'deltas/runtime-contracts.tsv',MODULE/'deltas/method-contracts.tsv',MODULE/'integration/verify_applied_resources.py']
    for name in ('MergePayloads','VerifyApplied'):artifacts.append(build/'java'/(name+'.class'))
    all_paths=runtime_sources+own_sources+helper_sources+artifacts
    before={str(f.relative_to(MODULE)):sha(f) for f in all_paths}
    cases={kind:{name:getattr(args,kind+'_'+name) for name in ('apk','mpp','result')} for kind in ('standalone','integrated')}
    input_pins={kind:{name:sha(path) for name,path in case.items()} for kind,case in cases.items()}
    with tempfile.TemporaryDirectory(prefix='ulike-candidate169-independent-') as directory:
        work=Path(directory);classes=work/'classes';classes.mkdir()
        android_cp=os.pathsep.join(map(str,[tools/'android.jar',MODULE/'runtime/helpers.jar']))
        run([jdk/'javac','--release','8','-cp',android_cp,'-d',classes,*runtime_sources,HERE/'OpticalZoom.java',HERE/'DisabledBridgeReview.java'])
        host=json.loads(run([jdk/'java','-cp',os.pathsep.join(map(str,[classes,tools/'android.jar',MODULE/'runtime/helpers.jar'])),
                            'com.hiro.ulike.integration169.DisabledBridgeReview']))
        dex_cp=os.pathsep.join(map(str,[build/'java',tools/'morphe-1.16.jar',build/'old']))
        run([jdk/'javac','-cp',dex_cp,'-d',classes,HERE/'ArtifactReview169.java'])
        dex=json.loads(run([jdk/'java','-cp',str(classes)+os.pathsep+dex_cp,'ArtifactReview169',build/'old/ulike/runtime.dex',build/'runtime.dex']))
        output={}
        for kind,case in cases.items():
            folder=work/kind;folder.mkdir()
            dex_result=run([jdk/'java','-Xmx2g','-cp',dex_cp,'VerifyApplied',build/'runtime.dex',build/'methods.tsv',case['apk']])
            resource_report=folder/'resources.json'
            run([os.sys.executable,MODULE/'integration/verify_applied_resources.py','--apks',args.apks,'--mpp',case['mpp'],
                '--apk',case['apk'],'--result',case['result'],'--report',resource_report,'--native-contract','nv21-effect-flag'])
            output[kind]={'dex_payload_verification':dex_result,'resource_contract':json.loads(resource_report.read_text()),
                          'manifest':manifest(case['apk'],folder,jdk,tools)}
    after={str(f.relative_to(MODULE)):sha(f) for f in all_paths}
    if before!=after:raise RuntimeError('Reviewed source/build changed while verifying')
    if input_pins!={kind:{name:sha(path) for name,path in case.items()} for kind,case in cases.items()}:raise RuntimeError('Reviewed APK/MPP/result changed')
    report={'status':'PASS_DISABLED_CANDIDATE_REVIEW','host_guard_tests':host,'runtime_preservation':dex,'apk_cases':output,
        'reviewed_file_sha256':before,'artifact_sha256':input_pins,'input_apks_sha256':sha(args.apks),
        'tool_sha256':{name:sha(tools/name) for name in ('android.jar','apktool.jar','morphe-1.16.jar')},
        'jdk_sha256':{name:sha(jdk/name) for name in ('java','javac')},
        'resolved_review_findings':[
            'Queued stale capture failures and sequence aborts check the exact currently submitted request/session before cleanup.',
            'Rejected builders, requests, sessions and unsupported bursts remove their owned pending state; local reader allocation has failure cleanup.',
            'A single photo choice remains owned through worker completion, preventing an unbounded queue of owned full-resolution frames.',
            'Expired pre-copy choices are marked before cancellation; exact-choice cancellation uses safe monitor ordering and cannot consume a newer choice.',
            'Mutable OpticalZoom route failure is checked after delegated request building.',
            'After a photo has committed, inspection/UI failure retains the URI and attempts non-success shutter cleanup; no ordinary image-error callback is generated.',
            'Resource validation now permits exactly the two pinned ORT native additions and four notices while checking all previous native/asset contracts.'
        ],
        'limits':[
            'CandidateGate169 is hard false in actual DEX; no application Processor is installed. This is not a functional full-HDR capture release.',
            'Global native recorder admission helpers are bundled but unhooked and uninstalled; complete native bypass coverage and restoration barrier are not proven.',
            'The test-only OpticalZoom recorder verifies delegate routing on a host; it is not packaged, and it is not Android Camera2 execution.',
            'P010 callback matching and saved-URI active paths are source reviewed; real Camera2, MediaStore UI, Samsung native inference, lens/focus speed and visual quality require device validation.',
            'Automatic-save handoff is the only implemented UI route; Bitmap editor and burst HDR modes are explicitly unsupported.',
            'High-resolution P010 alone does not establish sensor-native24.5MP input, chroma siting, or end-to-end stock appearance equivalence.'
        ]}
    args.report.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'status':report['status'],'host_checks':host['checks'],'runtime':dex,'apk_cases':list(output),
                      'report':str(args.report),'report_sha256':sha(args.report)}))

if __name__=='__main__':main()
