#!/usr/bin/env python3
"""Stage pinned official ORT binaries/notices for a private Morphe build. No app execution."""
import argparse, hashlib, io, json, re, zipfile
from pathlib import Path

AAR_SHA='e7fb945e402205f6db858d65bb78d2bdb0812317b383976c9e3bceb4862c73f1'
CLASSES_SHA='65e2e2d76d672253aaf0792a06c71cc40d09b0e2e251ccf925bee7175b91a1b0'

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--aar',type=Path,required=True)
    p.add_argument('--official-notices',type=Path,required=True)
    p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();data=a.aar.read_bytes()
    if hashlib.sha256(data).hexdigest()!=AAR_SHA:raise ValueError('Unsupported ORT AAR hash')
    source=Path(__file__).parent/'src/app/hiro/ulike/patches/IntegrationPayload169.java'
    entries=re.findall(r'\{"([^"]+)","([0-9a-f]{64})","([0-9]+)","([^"]+)"\}',source.read_text())
    if len(entries)!=6:raise ValueError('Expected six fixed payload contracts')
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        classes=z.read('classes.jar')
        if hashlib.sha256(classes).hexdigest()!=CLASSES_SHA:raise ValueError('Classes hash mismatch')
        with zipfile.ZipFile(io.BytesIO(classes)) as jar:one_ds=jar.read('META-INF/LICENSE-1DS')
        blobs=[z.read('jni/arm64-v8a/libonnxruntime.so'),z.read('jni/arm64-v8a/libonnxruntime4j_jni.so'),
               (a.official_notices/'LICENSE').read_bytes(),(a.official_notices/'ThirdPartyNotices.txt').read_bytes(),
               (a.official_notices/'Privacy.md').read_bytes(),one_ds]
    # Verify every input before any staging output is written.
    for (target,sha,length,resource),blob in zip(entries,blobs):
        if len(blob)!=int(length) or hashlib.sha256(blob).hexdigest()!=sha:raise ValueError('Payload pin mismatch: '+target)
    a.output.mkdir(parents=True,exist_ok=True)
    for (_,_,_,resource),blob in zip(entries,blobs):
        dest=a.output/resource;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(blob)
    (a.output/'onnxruntime-android-1.30.0-classes.jar').write_bytes(classes)
    manifest=''.join('\t'.join(e)+'\n' for e in entries)
    (a.output/'resourceitems.tsv').write_text(manifest)
    report={'aar_sha256':AAR_SHA,'classes_sha256':CLASSES_SHA,'resources':[dict(zip(('target','sha256','bytes','resource'),e)) for e in entries],
            'merge_aar_manifest':False,'startup_provider_allowed':False,'explicit_process_telemetry_opt_out_before_runtime_required':True,
            'abi':'arm64-v8a','runtime_executed':False,'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest()}
    (a.output/'PAYLOAD_MANIFEST.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'payload_files':len(entries),'payload_bytes':sum(map(len,blobs)),'classes_bytes':len(classes)}))

if __name__=='__main__':main()
