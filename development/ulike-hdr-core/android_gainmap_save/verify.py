#!/usr/bin/env python3
"""Build Android Java sources, run actual Main10 codec fixtures, inspect/decode finished HEIF.

Independent libheif decodes BOTH coded items. Native YUV10 and explicit nearest-chroma
RGB conversion are checked; automatic ISO-tmap HDR rendering is not claimed.
"""
import argparse
import ctypes as C
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile

import numpy as np

from inspect_file import inspect_file, reconstruct, decode_libheif

CHECKS=0

def check(value, message):
    global CHECKS
    CHECKS+=1
    if not value:
        raise AssertionError(message)

def run(argv):
    result=subprocess.run([str(a) for a in argv],capture_output=True,text=True)
    if result.returncode:
        raise RuntimeError(result.stderr[-5000:])
    return result.stdout

def decoder_types(root):
    # Existing independently verified official libheif v1.23.2 decoding-options layout.
    spec=importlib.util.spec_from_file_location('main10_heif_verifier',root.parent/'android_heif/verify.py')
    module=importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    return module.Error,module.Decoding

def decode_yuv(path,item,root):
    Error,Decoding=decoder_types(root)
    location=os.environ.get('HEIF_VERIFY_LIBRARY','/opt/codex/runtimes/codex-primary-runtime/dependencies/native/libheif/libheif/lib/libheif.so.1')
    lib=C.CDLL(location)
    def fn(name,result,*args):
        f=getattr(lib,name); f.restype=result; f.argtypes=list(args); return f
    ptr=C.c_void_p; pp=C.POINTER(ptr)
    alloc=fn('heif_context_alloc',ptr); free=fn('heif_context_free',None,ptr)
    read=fn('heif_context_read_from_file',Error,ptr,C.c_char_p,ptr)
    get=fn('heif_context_get_image_handle',Error,ptr,C.c_uint32,pp)
    release=fn('heif_image_handle_release',None,ptr)
    decode=fn('heif_decode_image',Error,ptr,pp,C.c_int,C.c_int,ptr)
    release_image=fn('heif_image_release',None,ptr)
    width=fn('heif_image_get_width',C.c_int,ptr,C.c_int)
    height=fn('heif_image_get_height',C.c_int,ptr,C.c_int)
    depth=fn('heif_image_get_bits_per_pixel_range',C.c_int,ptr,C.c_int)
    plane=fn('heif_image_get_plane_readonly',C.POINTER(C.c_uint8),ptr,C.c_int,C.POINTER(C.c_int))
    options_alloc=fn('heif_decoding_options_alloc',C.POINTER(Decoding))
    options_free=fn('heif_decoding_options_free',None,C.POINTER(Decoding))
    version=fn('heif_get_version',C.c_char_p)().decode()
    context=alloc(); handle=ptr(); image=ptr(); options=options_alloc()
    def okay(error):
        if error.code:
            raise ValueError(f'libheif {error.code}/{error.subcode}: {error.message.decode()}')
    try:
        if not context or not options or options.contents.version<10:
            raise RuntimeError('libheif options v10 required')
        options.contents.passthrough=1; options.contents.strict_decoding=1
        options.contents.convert_hdr_to_8bit=0
        okay(read(context,os.fsencode(path),None)); okay(get(context,item,C.byref(handle)))
        okay(decode(handle,C.byref(image),99,99,options))
        arrays=[]
        for c in (0,1,2):
            w,h=width(image,c),height(image,c)
            check(w>0 and h>0 and depth(image,c)==10,'actual native ten-bit plane')
            stride=C.c_int(); data=plane(image,c,C.byref(stride))
            check(bool(data) and stride.value>=w*2,'native plane bounds')
            raw=C.string_at(data,stride.value*h)
            a=np.ndarray((h,w),'<u2',buffer=raw,strides=(stride.value,2)).copy()
            check(np.all(a<=1023),'native ten-bit code bounds')
            arrays.append(a)
        return arrays,version
    finally:
        if image: release_image(image)
        if handle: release(handle)
        if options: options_free(options)
        if context: free(context)

def rgb(yuv):
    y,cb,cr=[a.astype(np.float64) for a in yuv]
    cb=np.repeat(np.repeat(cb,2,0),2,1)-512
    cr=np.repeat(np.repeat(cr,2,0),2,1)-512
    r=y+1.4746*cr; b=y+1.8814*cb; g=(y-.2627*r-.0593*b)/.6780
    return np.floor(np.clip(np.stack([r,g,b],axis=2),0,1023)+.5).astype(np.uint16)

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--android-jar',type=Path,required=True)
    ap.add_argument('--jdk-bin',type=Path,required=True)
    ap.add_argument('--report',type=Path,required=True)
    args=ap.parse_args(); root=Path(__file__).resolve().parent
    sources=sorted((root/'src/main/java').rglob('*.java'))
    dependencies=sorted((root.parent/'android_gainmap_codec/src/main/java').rglob('*.java'))
    report={'status':'PASS','android_device_executed':False,'sdk36_compile':'PASS',
            'processed_hdr_sdr_pair_required':True,'actual_encoded_base_decoded_before_map':True,
            'actual_encoded_map_decoded_before_acceptance':True,'automatic_tmap_display_tested':False,
            'source_sha256':{str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},
            'dependency_sha256':{str(p.relative_to(root.parent)):hashlib.sha256(p.read_bytes()).hexdigest() for p in dependencies},
            'sdk_sha256':hashlib.sha256(args.android_jar.read_bytes()).hexdigest(),'files':{}}
    with tempfile.TemporaryDirectory(prefix='ulike-gainmap-save-') as directory:
        work=Path(directory); classes=work/'classes'; classes.mkdir()
        run([args.jdk_bin/'javac','--release','8','-cp',args.android_jar,'-d',classes,*dependencies,*sources])
        run([args.jdk_bin/'javac','--release','11','-cp',classes,'-d',classes,*sorted((root/'src/test/java').rglob('*.java'))])
        java=json.loads(run([args.jdk_bin/'java','-cp',classes,'com.hiro.ulike.hdr.gainmap.GainmapSaveTest',work]))
        report['java_checks']=java['checks']
        for name,expected in java['fixtures'].items():
            path=work/(name+'.heic'); info=inspect_file(path); w,h=info['width'],info['height']; decoded=[]
            for item,role in ((1,'SDR_BASE'),(2,'NUMERICAL_GAINMAP')):
                yuv,version=decode_yuv(path,item,root)
                flat=np.frombuffer((work/f'{name}_{role}.decoded.yuv').read_bytes(),'<u2')
                ffmpeg=[flat[:w*h].reshape(h,w),flat[w*h:w*h+w*h//4].reshape(h//2,w//2),flat[w*h+w*h//4:].reshape(h//2,w//2)]
                for a,b in zip(yuv,ffmpeg): check(np.array_equal(a,b),'independent libheif vs FFmpeg native plane exact')
                converted=rgb(yuv)
                java_rgb=np.frombuffer((work/f'{name}_{role}.decoded.rgb10').read_bytes(),'<u2').reshape(h,w,3)
                check(np.array_equal(converted,java_rgb),'independent nearest BT2020 conversion vs Java exact')
                decoded.append(converted)
            actual=reconstruct(*decoded,info['metadata'])
            hdr=np.frombuffer((work/(name+'.hdr')).read_bytes(),'<f8').reshape(h,w,3)
            sdr=np.frombuffer((work/(name+'.sdr')).read_bytes(),'<f8').reshape(h,w,3)
            encoded=decoded[0].astype(np.float64)/1023
            base=np.where(encoded<.081,encoded/4.5,((encoded+.099)/1.099)**(1/.45))
            metrics={'max_sdr_error':float(np.abs(base-sdr).max()),'max_hdr_error':float(np.abs(actual-hdr).max()),
                     'hdr_rmse':float(np.sqrt(np.mean((actual-hdr)**2))),'samples':int(hdr.size)}
            for k,v in metrics.items(): check(abs(v-expected[k])<3e-13,'serialized-file error metrics equal Java: '+k)
            # A second decoder's RGB upsampler may differ: quantify, never claim viewer-independent pixels.
            rgb_base,_=decode_libheif(path,1); rgb_map,_=decode_libheif(path,2)
            viewer_rgb=reconstruct(rgb_base,rgb_map,info['metadata'])
            viewer_metrics={'max_hdr_error':float(np.abs(viewer_rgb-hdr).max()),
                            'hdr_rmse':float(np.sqrt(np.mean((viewer_rgb-hdr)**2)))}
            check(np.isfinite(viewer_rgb).all(),'alternate RGB conversion reconstruction finite')
            info.update({'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'libheif':version,
                         'native_yuv10_match':'EXACT','metrics':metrics,'libheif_rgb_conversion_metrics':viewer_metrics,
                         'base_nclx':[9,1,9,True],'map_nclx':[9,8,9,True],'derived_nclx':[9,16,0,True],
                         'map_chroma_subsampled':True,'actual_android_codec':False})
            info['decoded_rgb_unique_codes']=[int(np.unique(a).size) for a in decoded]
            if name!='black':
                check(all(np.any(a%4) for a in decoded),'actual decoded lower ten-bit code bits used')
            if name=='changed_base':
                # Counterfactual map from the pre-encode SDR would encode the wrong gain.
                log_gain=np.log2(hdr+1/64)-np.log2(sdr+1/64)
                lo=log_gain.min(axis=(0,1)); hi=log_gain.max(axis=(0,1))
                stale_codes=np.floor((log_gain-lo)/(hi-lo)*1023+.5)
                stale=(base+1/64)*np.exp2(lo+(hi-lo)*stale_codes/1023)-1/64
                stale_max=float(np.abs(stale-hdr).max())
                check(stale_max>metrics['max_hdr_error']*10,'decoded-base recomputation materially prevents stale-map error')
                info['counterfactual_preencode_sdr_map_max_error']=stale_max
            report['files'][name]=info
        broken=work/'truncated.heic'; broken.write_bytes((work/'smooth.heic').read_bytes()[:-20])
        try:
            inspect_file(broken)
            raise AssertionError('accepted truncated container')
        except ValueError:
            check(True,'truncated container rejected')
    report['independent_checks']=CHECKS
    report['limits']=['Actual MediaCodec encode/decode adapter compiles but has not run on a phone.',
                      'Caller must supply matching post-edit display-linear HDR/SDR images; this is not the missing HDR beauty engine.',
                      'Main10 4:2:0 plus RGB conversion is lossy; explicit actual-decoded SDR/HDR quality limits can reject a save.',
                      'Chroma upsampling differs between viewers; nearest-policy and independent libheif RGB results are both reported.',
                      'No automatic tmap rendering, EXIF preservation, odd-size padding, or native24.5MP claim.']
    args.report.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'status':'PASS','java_checks':java['checks'],'independent_checks':CHECKS,'files':len(report['files']),
                      'android_device_executed':False}))

if __name__=='__main__':
    main()
