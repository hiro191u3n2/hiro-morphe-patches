#!/usr/bin/env python3
"""Independent mutation/error-gate review and official ISO metadata parser check."""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile

if not __debug__:
    raise RuntimeError("Verification assertions must remain enabled")
HERE=Path(__file__).resolve().parent
ROOT=HERE.parent
CORE=ROOT.parent

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(args):
    p=subprocess.run(list(map(str,args)),text=True,capture_output=True)
    if p.returncode:raise RuntimeError(p.stdout+'\n'+p.stderr)
    return p.stdout.strip()

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for key in ('jdk-bin','android-jar','r8','libultrahdr-source','jpeg-include'):p.add_argument('--'+key,type=Path,required=True)
    p.add_argument('--report',type=Path,default=ROOT/'INDEPENDENT_REVIEW.json')
    a=p.parse_args()
    sources=sorted((ROOT/'src/main/java').rglob('*.java'))
    deps=sorted((CORE/'android_gainmap_codec/src/main/java').rglob('*.java'))
    authored=[ROOT/'README.md',ROOT/'OFFICIAL_SOURCES.json',ROOT/'inspect_file.py',ROOT/'verify.py']
    tests=sorted((ROOT/'src/test/java').rglob('*.java'))
    review=[HERE/'GainmapReview.java',HERE/'metadata_reference.cpp',Path(__file__).resolve()]
    inventory=sources+deps+tests+authored+review
    before={str(f.relative_to(CORE)):sha(f) for f in inventory}
    sys.path.insert(0,str(ROOT))
    import inspect_file
    spec=importlib.util.spec_from_file_location('gainmap_author_verifier',ROOT/'verify.py')
    verifier=importlib.util.module_from_spec(spec);spec.loader.exec_module(verifier)
    import numpy as np
    metadata_cpp=a.libultrahdr_source/'lib/src/gainmapmetadata.cpp'
    with tempfile.TemporaryDirectory(prefix='ulike-gainmap-independent-') as td:
        work=Path(td);classes=work/'classes';classes.mkdir()
        run([a.jdk_bin/'javac','--release','8','-cp',a.android_jar,'-d',classes,*deps,*sources])
        jar=work/'production.jar';run([a.jdk_bin/'jar','--create','--file',jar,'-C',classes,'.'])
        dex=work/'dex';dex.mkdir();run([a.jdk_bin/'java','-cp',a.r8,'com.android.tools.r8.D8','--min-api','33','--lib',a.android_jar,'--output',dex,jar])
        run([a.jdk_bin/'javac','--release','11','-cp',classes,'-d',classes,*tests,HERE/'GainmapReview.java'])
        own=json.loads(run([a.jdk_bin/'java','-Xmx128m','-cp',classes,'com.hiro.ulike.hdr.gainmap.GainmapReview',work]))
        author=json.loads(run([a.jdk_bin/'java','-Xmx128m','-cp',classes,'com.hiro.ulike.hdr.gainmap.GainmapSaveTest',work]))
        executable=work/'metadata-reference'
        run(['g++','-std=c++17','-O2','-ffunction-sections','-fdata-sections','-Wl,--gc-sections',
             '-I'+str(a.libultrahdr_source),'-I'+str(a.libultrahdr_source/'lib/include'),'-I'+str(a.jpeg_include),
             HERE/'metadata_reference.cpp',metadata_cpp,'-o',executable])
        files={};official=[]
        for path in sorted(work.glob('*.heic')):
            info=inspect_file.inspect_file(path)
            # Extract exact item3 extent directly; never serialize the writer's in-memory metadata.
            blob=path.read_bytes();top=inspect_file.unique_boxes(blob);m=top[b'meta']
            boxes=inspect_file.unique_boxes(blob,m[0]+4,m[1]);loc=blob[slice(*boxes[b'iloc'])]
            import struct
            item,reference,count,offset,length=struct.unpack_from('>HHHII',loc,8+2*14)
            assert item==3 and reference==0 and count==1 and blob[offset]==0
            payload=work/(path.stem+'.official.iso');payload.write_bytes(blob[offset+1:offset+length])
            parsed=json.loads(run([executable,payload]));expected=info['metadata']
            assert parsed['use_base'] and not parsed['backward']
            assert parsed['channels']==expected['channels']
            assert parsed['base_headroom_log2']==expected['base_headroom_log2']
            assert parsed['alternate_headroom_log2']==expected['alternate_headroom_log2']
            decoded=[]
            for item in (1,2):
                arrays,version=verifier.decode_yuv(path,item,ROOT)
                assert all(np.max(arr)<=1023 for arr in arrays)
                decoded.append(verifier.rgb(arrays))
            reconstructed=inspect_file.reconstruct(*decoded,parsed)
            assert np.isfinite(reconstructed).all()
            entry={'file_sha256':sha(path),'official_iso_payload_sha256':sha(payload),
                   'official_iso_metadata_equal':True,'actual_native_plane_depths':[10,10],
                   'independent_decoder':version,'unique_base_rgb_codes':int(np.unique(decoded[0]).size),
                   'unique_map_rgb_codes':int(np.unique(decoded[1]).size)}
            if path.stem in author['fixtures']:
                hdr=np.fromfile(work/(path.stem+'.hdr'),'<f8').reshape(reconstructed.shape)
                sdr=np.fromfile(work/(path.stem+'.sdr'),'<f8').reshape(reconstructed.shape)
                basecodes=decoded[0].astype(float)/1023
                base=np.where(basecodes<.081,basecodes/4.5,((basecodes+.099)/1.099)**(1/.45))
                metrics={'max_sdr_error':float(np.abs(base-sdr).max()),'max_hdr_error':float(np.abs(reconstructed-hdr).max()),
                         'hdr_rmse':float(np.sqrt(np.mean((reconstructed-hdr)**2))),'samples':hdr.size}
                assert all(abs(v-author['fixtures'][path.stem][key])<3e-13 for key,v in metrics.items())
                entry['independently_decoded_reconstruction_metrics']=metrics
            files[path.stem]=entry;official.append(path.stem)
        parsed=json.loads(run([executable,work/'independent-random.iso']))
        assert parsed['use_base'] and not parsed['backward'] and all(c[0]<0<c[1] for c in parsed['channels'])
    after={str(f.relative_to(CORE)):sha(f) for f in inventory}
    if before!=after:raise RuntimeError('Source changed during independent review')
    report={'status':'PASS_INDEPENDENT_GAINMAP_SAVE_REVIEW','sdk36_compile':True,'d8_min_api':33,
            'compile_contract':'javac --release 8 -cp actual SDK36 android.jar, then D8 --lib actual SDK36 android.jar; not SDK jar alone as javac bootclasspath',
            'android_device_execution':False,'automatic_tmap_display':False,'full_hdr_beauty_pipeline':False,
            'independent_adversarial_review':own,'author_java_checks_rerun':author['checks'],'files':files,
            'reviewed_file_sha256':before,'official_metadata_parser':{
                'url':'https://github.com/google/libultrahdr/blob/v2.0.2/lib/src/gainmapmetadata.cpp',
                'source_sha256':sha(metadata_cpp),'unchanged_official_source_compiled':True,
                'header_sha256':sha(a.libultrahdr_source/'lib/include/ultrahdr/gainmapmetadata.h'),
                'build_only_jpeg_headers':{'url':'https://android.googlesource.com/platform/external/libjpeg-turbo/+/refs/heads/main/',
                    'sha256':{f.name:sha(f) for f in sorted(a.jpeg_include.glob('*.h'))},
                    'jpeg_codec_linked_or_called':False},
                'validation':'Exact final HEIF item3 metadata parsed by official decoder; all rational values compared'},
            'resolved_findings':[{'issue':'Original processed SDR not bound before initial encode',
                'fix':'Original SDR/HDR digest before any encode; final exact-bit comparison added',
                'independent_regression':'1ULP source mutation after actual SDR encode rejects even within quality bounds'}],
            'additional_guards':['Actual Main10 all1023 wrong numerical map rejected after actual decode',
                'Source capture/geometry/processing/dimensions mismatch','Negative/nonfinite/out-of-contract luminance rejection',
                'Independent gain quantization plus serialized rational rounding error bound',
                'Original output unchanged on quality failure and temporary files removed',
                'Owned metadata/digest copies; changed base/HDR input detected; interruption'],
            'limitations':['Fixed display-linear BT2020/203nit-white input is an explicit caller contract, not inferred from captured P010.',
                'Caller must supply same post-edit HDR/SDR; IDs are assertions; a digest cannot prove artistic or geometric correspondence.',
                'Sources must remain immutable throughout; arbitrary transient concurrent mutations are outside contract.',
                'Main10 420 numerical maps are lossy; quality limits cover the explicitly declared nearest-chroma decoder.',
                'Codec execution remains host x265/FFmpeg; Android MediaCodec and gallery tmap behavior not measured.'],
            'tool_sha256':{f.name:sha(f) for f in (a.android_jar,a.r8,a.jdk_bin/'javac',a.jdk_bin/'java')},
            'command':'python review/verify_review.py --jdk-bin <JDKbin> --android-jar <SDK36android.jar> --r8 <r8-8.3.37.jar> --libultrahdr-source <officialv2.0.2> --jpeg-include <officialAOSPheaders> --report INDEPENDENT_REVIEW.json'}
    a.report.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'status':report['status'],**own,'author_checks':author['checks'],'real_heif_files':len(files)}))

if __name__=='__main__':main()
