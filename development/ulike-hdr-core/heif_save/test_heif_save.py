#!/usr/bin/env python3
import copy
import dataclasses
import hashlib
import json
import math
from pathlib import Path
import struct
import subprocess
import tempfile

import numpy as np

from heif_boxes import iso_metadata, parse_sps
from heif_save import Frame, generate_map, save_pair, srgb_decode, validate_pair
from verify_file import decode_libheif, inspect_file, parse_iso, reconstruct


CHECKS=0
def check(value,message):
    global CHECKS
    CHECKS+=1
    if not value: raise AssertionError(message)


def rejects(action,message):
    try: action()
    except (ValueError,RuntimeError): check(True,message);return
    raise AssertionError('did not reject '+message)


def roundtrip(sdr,hdr,frame,path):
    report=save_pair(sdr,hdr,frame,frame,path)
    structure=inspect_file(path)
    base,version=decode_libheif(path,1)
    gainmap,_=decode_libheif(path,2)
    expected_base=np.floor(np.where(sdr<=0.0031308,sdr*12.92,1.055*sdr**(1/2.4)-0.055)*1023+0.5).astype(np.uint16)
    expected_map,_=generate_map(srgb_decode(base),hdr,16)
    check(np.array_equal(base,expected_base),'independent container/base decode exact')
    check(np.array_equal(gainmap,expected_map),'independent container/map decode exact')
    check(base.dtype==np.uint16 and gainmap.dtype==np.uint16,'actual 16-bit storage')
    restored=reconstruct(base,gainmap,structure['metadata'])
    error=restored-hdr
    metrics={'max_hdr_abs_error':float(np.abs(error).max()),'hdr_rmse':float(np.sqrt(np.mean(error**2))),
             'base_unique_codes':int(np.unique(base).size),'map_unique_codes':int(np.unique(gainmap).size),
             'libheif':version,'file_bytes':path.stat().st_size,
             'sha256':hashlib.sha256(path.read_bytes()).hexdigest()}
    check(metrics['max_hdr_abs_error']<0.03,'bounded full-file HDR quantization error')
    check(structure['jpeg_items']==0 and report['jpeg_stages']==0,'no JPEG item or encoder stage')
    check(report['profile_idc']==4 and report['chroma_format_idc']==3,'honest RExt444 profile')
    return metrics,base,gainmap,structure['metadata']


def main():
    root=Path(__file__).resolve().parent
    out=root/'build';out.mkdir(exist_ok=True)
    w=h=64
    index=np.arange(w*h*3).reshape(h,w,3)
    base_codes=(index%1024).astype(np.uint16)
    sdr=srgb_decode(base_codes)
    ideal_map=((index*37)%1024)/1023
    hdr=(sdr+1/64)*2**(3*ideal_map)-1/64
    frame=Frame(w,h,'synthetic-capture-1','identity-64x64','processed-v1')
    first,base,old_map,old_metadata=roundtrip(sdr,hdr,frame,out/'rgb10_gainmap10.heic')
    check(first['base_unique_codes']==1024,'fixture proves all 1024 base levels')
    check(first['map_unique_codes']==1024,'fixture proves all 1024 map levels')
    check(first['max_hdr_abs_error']<1e-11,'exact ramp map reconstruction')
    # Different spatial transform and nonlinear local exposure represent a
    # post-beauty processed pair. The test is about math, not an AI style claim.
    sdr2=np.roll(sdr,7,axis=1)
    field=0.4+0.6*np.sin(index*0.017)**2
    hdr2=np.roll(hdr,7,axis=1)*field
    frame2=dataclasses.replace(frame,geometry_id='crop-warp-v2',processing_id='relit-v2')
    second,base2,map2,metadata2=roundtrip(sdr2.astype(np.float32),hdr2,frame2,out/'processed_rgb10_gainmap10.heic')
    stale_error=float(np.abs(reconstruct(base2,old_map,old_metadata)-hdr2).max())
    check(stale_error>1,'stale pre-processing map demonstrably fails')
    check(second['max_hdr_abs_error']<stale_error/100,'post-processing map repaired')
    # Serializer fixture with exactly representable rational values, built
    # independently with struct, protects version/flags/order/signedness.
    md={'base':'SDR','use_base_color_space':True,'base_headroom_log2':0.,'alternate_headroom_log2':4.,
        'channels':[[-1.,3.,1.,1/64,1/64]]*3}
    expected=struct.pack('>HHBIIII',0,0,0xC0,0,1,4,1)
    channel=struct.pack('>iIiIIIiIiI',-1,1,3,1,1,1,1,64,1,64)
    check(iso_metadata(md)==expected+channel*3,'exact ISO rational fixture')
    decoded=parse_iso(b'\0'+iso_metadata(md))
    check(decoded['channels']==md['channels'],'metadata independent parse')
    for name,change in (
        ('backward direction',lambda x:x.update(base='HDR')),
        ('gamma rounds zero',lambda x:x['channels'][0].__setitem__(2,1e-10)),
        ('headroom rounds zero',lambda x:x.update(alternate_headroom_log2=1e-9)),
        ('gain bounds collapse',lambda x:x['channels'][0].__setitem__(1,-1+1e-10)),
        ('nonfinite gamma',lambda x:x['channels'][0].__setitem__(2,float('nan'))),
    ):
        bad=copy.deepcopy(md);change(bad)
        rejects(lambda:iso_metadata(bad),name)
    # Never silently reinterpret absent/mismatched pixel provenance or depth.
    for name,bad_frame in (
        ('geometry mismatch',dataclasses.replace(frame,geometry_id='other')),
        ('processing mismatch',dataclasses.replace(frame,processing_id='other')),
        ('unknown primaries',dataclasses.replace(frame,primaries='UNKNOWN')),
        ('unknown reference white',dataclasses.replace(frame,reference_white_nits=80)),
    ):
        rejects(lambda:validate_pair(sdr,hdr,frame,bad_frame,16),name)
        if name in ('unknown primaries','unknown reference white'):
            rejects(lambda:validate_pair(sdr,hdr,bad_frame,bad_frame,16),'matched but '+name)
    for name,bad_sdr,bad_hdr in (
        ('8bit input',np.zeros((h,w,3),dtype=np.uint8),hdr),
        ('SDR out of range',sdr+1,hdr),
        ('HDR out of headroom',sdr,hdr+16),
        ('HDR nonfinite',sdr,np.full_like(hdr,np.nan)),
    ):
        rejects(lambda:validate_pair(bad_sdr,bad_hdr,frame,frame,16),name)
    rejects(lambda:parse_sps(b'\x42\x01\x0e'),'illegal SPS sublayer count')
    preserved=out/'preserve.heic';preserved.write_bytes(b'previous complete file')
    zeros=np.zeros_like(sdr)
    rejects(lambda:save_pair(zeros,zeros,frame,frame,preserved,math.nextafter(1,math.inf)),
            'near-zero headroom cannot overwrite prior file')
    check(preserved.read_bytes()==b'previous complete file','failed save is atomic')
    # Malformed rational and container references are detectable independently.
    malformed=bytearray(b'\0'+iso_metadata(md));malformed[10:14]=bytes(4)
    rejects(lambda:parse_iso(malformed),'zero rational denominator')
    raw=(out/'rgb10_gainmap10.heic').read_bytes()
    with tempfile.TemporaryDirectory(dir=out) as tmp:
        p=Path(tmp)/'truncated.heic';p.write_bytes(raw[:-5])
        rejects(lambda:inspect_file(p),'truncated mdat')
        altered=bytearray(raw);position=raw.index(b'dimg')+4
        altered[position:position+8]=struct.pack('>HHHH',3,2,2,1)
        p.write_bytes(altered)
        rejects(lambda:inspect_file(p),'reversed base/map dependency')
    qa={'status':'PASS','checks':CHECKS,'scope':'host 10-bit HEVC items + minimal HEIF/tmap writer',
        'fixtures':{'full_10bit_levels':first,'post_processing':second},
        'stale_map_max_hdr_error':stale_error,'independent_decode':'libheif/libde265 both coded items',
        'gainmap_math':'linked ../gainmap C++17, FP64 arithmetic, quantized RGB10 map',
        'automatic_tmap_decoder_or_hdr_display_verified':False,'android_or_camera_integration_verified':False,
        'full_iso_conformance_certified':False,'jpeg_stages':0,'base_bits':10,'map_bits':10,
        'command':'make test','ffmpeg':subprocess.check_output(['ffmpeg','-version'],text=True).splitlines()[0]}
    (root/'QA.json').write_text(json.dumps(qa,indent=2)+'\n')
    print(json.dumps(qa,indent=2))


if __name__=='__main__': main()
