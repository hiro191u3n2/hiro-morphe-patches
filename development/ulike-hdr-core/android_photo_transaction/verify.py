#!/usr/bin/env python3
"""Actual row-file/codec/photo transaction verification with process-kill restart recovery.

The host gallery implements the MediaStore transactional contract on disk. It is explicitly
not an Android ContentProvider execution. Completed HEIF pixels are independently decoded.
"""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile

import numpy as np

CHECKS=0
def check(value,message):
    global CHECKS
    CHECKS+=1
    if not value: raise AssertionError(message)

def run(argv,expected=0):
    p=subprocess.run([str(a) for a in argv],capture_output=True,text=True,env={**os.environ,'ORT_DISABLE_TELEMETRY':'1'})
    if p.returncode!=expected: raise RuntimeError(f'exit {p.returncode}: {p.stderr[-6000:]}\n{p.stdout[-2000:]}')
    return p.stdout

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--android-jar',type=Path,required=True);ap.add_argument('--ort-classes',type=Path,required=True)
    ap.add_argument('--jdk-bin',type=Path,required=True);ap.add_argument('--d8',type=Path,required=True);ap.add_argument('--report',type=Path,required=True)
    ap.add_argument('--models-dir',type=Path);ap.add_argument('--stock-libs',type=Path);ap.add_argument('--natural-zip',type=Path);ap.add_argument('--purity-zip',type=Path);ap.add_argument('--ort-host',type=Path)
    args=ap.parse_args()
    real_options=[args.models_dir,args.stock_libs,args.natural_zip,args.purity_zip,args.ort_host]
    if any(real_options) and not all(real_options): ap.error('all five real-model input options are required together')
    root=Path(__file__).resolve().parent;core=root.parent
    dep_roots=['android_gainmap_save/src/main/java','android_gainmap_codec/src/main/java','hdr_input/src/main/java',
               'android_beauty_image/src','android_model_runtime/src','android_hdr_beauty/src','android_hdr_color/src/main/java',
               'style_pipeline/src/main/java','face_analysis/src/main/java']
    sources=sorted((root/'src/main/java').rglob('*.java'));deps=sorted(p for d in dep_roots for p in (core/d).rglob('*.java'))
    sys.path.insert(0,str(core/'android_gainmap_save'))
    from inspect_file import inspect_file,reconstruct
    spec=importlib.util.spec_from_file_location('gainmap_verify',core/'android_gainmap_save/verify.py');gv=importlib.util.module_from_spec(spec);spec.loader.exec_module(gv)
    report={'status':'PASS','sdk36_compile':True,'d8_min_api':33,'android_media_store_executed':False,'android_device_execution':False,
            'source_sha256':{str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},
            'dependencies_sha256':{str(p.relative_to(core)):hashlib.sha256(p.read_bytes()).hexdigest() for p in deps},'restart':{},'files':[]}
    with tempfile.TemporaryDirectory(prefix='ulike-photo-transaction-') as directory:
        work=Path(directory);classes=work/'classes';classes.mkdir();sdkcp=os.pathsep.join(map(str,(args.android_jar,args.ort_classes)))
        run([args.jdk_bin/'javac','--release','8','-cp',sdkcp,'-d',classes,*deps,*sources])
        jar=work/'photo-transaction.jar';run([args.jdk_bin/'jar','cf',jar,'-C',classes,'.']);dex=work/'dex';dex.mkdir()
        run([args.jdk_bin/'java','-cp',args.d8,'com.android.tools.r8.D8','--min-api','33','--lib',args.android_jar,
             '--classpath',args.ort_classes,'--output',dex,jar]);check((dex/'classes.dex').is_file(),'D8 output exists')
        tests=sorted((root/'src/test/java').rglob('*.java'))+[core/'android_gainmap_save/src/test/java/com/hiro/ulike/hdr/gainmap/GainmapSaveTest.java',core/'android_hdr_beauty/test/com/hiro/ulike/hdr/input/HdrBeautyIntegration.java']
        run([args.jdk_bin/'javac','--release','11','-cp',os.pathsep.join(map(str,(classes,args.ort_classes))),'-d',classes,*tests])
        happy=work/'happy';java=json.loads(run([args.jdk_bin/'java','-cp',classes,'com.hiro.ulike.hdr.photo.TransactionTest',happy]));report['java_checks']=java['checks'];report['geometry_tile_workspace_tested']=java['max_geometry_tile_workspace']
        report['uncertain_publication_cases']=java['uncertain_publication_cases']
        galleries=[happy/'gallery']
        for fault in ('crash_insert','crash_write','crash_publish'):
            case=work/fault
            run([args.jdk_bin/'java','-cp',classes,'com.hiro.ulike.hdr.photo.TransactionTest',case,fault],expected=88)
            recovered=json.loads(run([args.jdk_bin/'java','-cp',classes,'com.hiro.ulike.hdr.photo.TransactionTest',case,'recover']))
            check(recovered['recovered']==1 and recovered['transactions_after']==0,'actual new-process recovery removed private journal')
            check(recovered['pending_after']==0,'no orphan owned pending photo')
            check(recovered['photos_after']==(1 if fault=='crash_publish' else 0),'preserve complete visible image, remove only unpublished pending item')
            report['restart'][fault]=recovered;galleries.append(case/'gallery')
        for gallery in galleries:
            for file in sorted(gallery.glob('*.heic')):
                info=inspect_file(file);w,h=info['width'],info['height'];arrays=[]
                for item in (1,2):
                    yuv,version=gv.decode_yuv(file,item,core/'android_gainmap_save');arrays.append(gv.rgb(yuv))
                hdr=reconstruct(*arrays,info['metadata']);yy,xx=np.mgrid[:h,:w];r=(xx+.4*yy)/(w-1+.4*(h-1));sdr=.02+.65*r
                expected=np.stack([sdr*(1.3+3*r+.15*c) for c in range(3)],axis=2)
                error=float(np.abs(hdr-expected).max());check(error<.05,'published actual HEIF reconstructs intended processed HDR')
                info.update({'sha256':hashlib.sha256(file.read_bytes()).hexdigest(),'libheif':version,'max_hdr_error':error,
                             'origin_case':gallery.parent.name,'filename_unique_uuid':file.stem,'published_only_after_complete_write':True})
                report['files'].append(info)
        report['actual_model_photo_integration']=[]
        report['actual_model_quality_rejection']=[]
        if all(real_options):
            for style,filename,archive in [('NATURAL_BLUSH','tt_baoman.model',args.natural_zip),('PURITY2','tt_goodlike.model',args.purity_zip)]:
                actual_work=work/('actual_'+style)
                result=json.loads(run([args.jdk_bin/'java','-cp',os.pathsep.join(map(str,(classes,args.ort_host))),
                    'com.hiro.ulike.hdr.photo.ActualHdrPhotoTest',style,args.models_dir/filename,args.stock_libs/'libbytenn.so',args.stock_libs/'libeffect.so',archive,actual_work]))
                files=list((actual_work/'gallery').glob('*.heic'));check(len(files)==1,'one actual-model completed photo')
                info=inspect_file(files[0]);w,h=info['width'],info['height'];decoded=[]
                for item in (1,2):
                    yuv,version=gv.decode_yuv(files[0],item,core/'android_gainmap_save');decoded.append(gv.rgb(yuv))
                target=np.frombuffer((actual_work/'expected.hdr').read_bytes(),'<f8').reshape(h,w,3)
                sdr=np.frombuffer((actual_work/'expected.sdr').read_bytes(),'<f8').reshape(h,w,3)
                restored=reconstruct(*decoded,info['metadata']);encoded=decoded[0].astype(np.float64)/1023
                actual_sdr=np.where(encoded<.081,encoded/4.5,((encoded+.099)/1.099)**(1/.45))
                maximum=float(np.abs(restored-target).max());rmse=float(np.sqrt(np.mean((restored-target)**2)))
                check(maximum<=.5 and rmse<=.05,'actual pinned-model HDR photo within declared save quality')
                check(float(target.max())>1 and float(restored.max())>1,'processed and reconstructed actual-model fixture both retain HDR above SDR white')
                check((w,h)==(382,508),'actual model paired output rotation/crop dimensions')
                result.update({'file_sha256':hashlib.sha256(files[0].read_bytes()).hexdigest(),'bytes':files[0].stat().st_size,'libheif':version,
                    'max_hdr_error':maximum,'hdr_rmse':rmse,'max_sdr_error':float(np.abs(actual_sdr-sdr).max()),
                    'processed_hdr_peak':float(target.max()),'reconstructed_hdr_peak':float(restored.max()),'quality_limits':{'max_sdr':.2,'max_hdr':.5,'hdr_rmse':.05},
                    'model_sha256':hashlib.sha256((args.models_dir/filename).read_bytes()).hexdigest(),'style_archive_sha256':hashlib.sha256(archive.read_bytes()).hexdigest()})
                report['actual_model_photo_integration'].append(result)
            # The sharp fixture deliberately exceeds the same unmodified Main10 4:2:0 gate.
            # Exercise an actual model again and verify that no pending/visible item is left.
            rejection_work=work/'actual_sharp_rejection'
            rejected=json.loads(run([args.jdk_bin/'java','-cp',os.pathsep.join(map(str,(classes,args.ort_host))),
                'com.hiro.ulike.hdr.photo.ActualHdrPhotoTest','NATURAL_BLUSH',args.models_dir/'tt_baoman.model',
                args.stock_libs/'libbytenn.so',args.stock_libs/'libeffect.so',args.natural_zip,rejection_work,'sharp']))
            check(rejected['published_photo_count']==0 and rejected['quality_rejection'] is not None,'sharp actual-model quality failure leaves no published photo')
            check(not list((rejection_work/'gallery').glob('*')),'quality failure never inserts a pending media item')
            report['actual_model_quality_rejection'].append(rejected)
    report['independent_checks']=CHECKS;report['libheif_plane_checks']=gv.CHECKS
    report['limits']=['MediaStore production implementation compiled against SDK36 but not executed on a phone.',
                      'Restart tests use actual separate killed JVM processes and a persistent host gallery implementation, not Android MediaProvider.',
                      'No claim of completed native ULike appearance bindings, photo EXIF preservation, 24.5MP native input, or zero-cost phone memory/storage.',
                      'Integer rotation/mirror/crop is physically applied to both stored renditions, without resizing; camera/app must supply the true planned geometry.',
                      'Actual-model tests use synthetic P010 input, actual pinned inference/masks, and explicitly synthetic resolved remaining-style bindings.',
                      'Main10 4:2:0 chroma conversion can exceed the unchanged quality gate; such images are refused before MediaStore insertion.',
                      'Unconfirmed provider publication throws PublicationUncertainException and retains its journal/media; it is neither a confirmed save nor proof of no saved photograph. No automatic retry is performed.']
    report['test_sha256']={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((root/'src/test/java').rglob('*.java'))}
    report['test_dependency_sha256']={str(p.relative_to(core)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tests if root not in p.parents}
    report['verifier_sha256']=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
    args.report.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'status':'PASS','java_checks':java['checks'],'independent_checks':CHECKS,'decoded_completed_files':len(report['files'])+len(report['actual_model_photo_integration']),'actual_model_quality_rejections':len(report['actual_model_quality_rejection']),'restart_cases':3,'android_device_execution':False}))

if __name__=='__main__':main()
