"""Host reference save path for a matched processed linear HDR/SDR pair.

Public entry: save_pair(sdr, hdr, sdr_frame, hdr_frame, destination).
No Android, Camera2, AI inference, resizing, tone mapping or 8-bit fallback.
Requires numpy, FFmpeg/libx265, and the compiled gainmap math bridge.
"""
import ctypes
import dataclasses
import json
import math
import os
from pathlib import Path
import subprocess
import tempfile

import numpy as np

from heif_boxes import build_heif


@dataclasses.dataclass(frozen=True)
class Frame:
    width: int
    height: int
    capture_id: str
    geometry_id: str
    processing_id: str
    primaries: str = 'BT2020'
    reference_white_nits: float = 203.0


def validate_pair(sdr, hdr, sdr_frame, hdr_frame, headroom):
    if sdr_frame != hdr_frame:
        raise ValueError('HDR/SDR provenance, geometry or color space differs')
    f=sdr_frame
    if not isinstance(f,Frame) or not all(isinstance(i,str) and i for i in (f.capture_id,f.geometry_id,f.processing_id)):
        raise ValueError('explicit capture, geometry and processing IDs required')
    if f.primaries!='BT2020' or f.reference_white_nits!=203.0:
        raise ValueError('this writer contract is BT.2020 linear RGB / 203-nit SDR white')
    if not all(isinstance(i,int) and not isinstance(i,bool) for i in (f.width,f.height)) or min(f.width,f.height)<16:
        raise ValueError('invalid dimensions; HEVC fixture path requires at least 16 pixels')
    if not math.isfinite(headroom) or not 1 < headroom <= 10000/203:
        raise ValueError('explicit headroom must fit the PQ-derived output contract')
    for name,data in (('SDR',sdr),('HDR',hdr)):
        if not isinstance(data,np.ndarray) or data.dtype not in (np.dtype('float32'),np.dtype('float64')):
            raise ValueError(name+' must be FP32/FP64 RGB')
        if data.shape!=(f.height,f.width,3) or not np.all(np.isfinite(data)) or np.any(data<0):
            raise ValueError(name+' shape or nonnegative linear samples invalid')
    if np.any(sdr>1) or np.any(hdr>headroom):
        raise ValueError('samples exceed declared SDR/HDR bounds; no silent clipping')


def srgb_encode(linear):
    return np.where(linear<=0.0031308,12.92*linear,1.055*np.power(linear,1/2.4)-0.055)


def srgb_decode(code):
    value=code.astype(np.float64)/1023
    return np.where(value<=0.04045,value/12.92,np.power((value+0.055)/1.055,2.4))


def gbr_bytes(rgb):
    return np.ascontiguousarray(rgb[:,:,[1,2,0]].transpose(2,0,1),dtype='<u2').tobytes()


def decode_gbr(raw,width,height):
    if len(raw)!=width*height*6: raise RuntimeError('decoder returned wrong sample count')
    gbr=np.frombuffer(raw,dtype='<u2').reshape(3,height,width)
    rgb=np.stack([gbr[2],gbr[0],gbr[1]],axis=2).copy()
    if np.any(rgb>1023): raise RuntimeError('decoder returned samples outside 10-bit range')
    return rgb


def run_codec(args):
    result=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=False)
    if result.returncode:
        raise RuntimeError('codec failed: '+result.stderr.decode(errors='replace')[-6000:])
    return result.stdout


def encode_codes(rgb,directory,name,transfer):
    height,width,_=rgb.shape
    raw=directory/(name+'.gbr10'); hevc=directory/(name+'.hevc')
    raw.write_bytes(gbr_bytes(rgb))
    run_codec(['ffmpeg','-hide_banner','-loglevel','error','-y','-f','rawvideo',
               '-pixel_format','gbrp10le','-video_size',f'{width}x{height}','-framerate','1',
               '-i',str(raw),'-frames:v','1','-c:v','libx265','-preset','medium',
               '-profile:v','main444-10-intra',
               '-x265-params','lossless=1:repeat-headers=1:info=0:pools=1:frame-threads=1:log-level=error',
               '-color_primaries','bt2020','-color_trc',transfer,'-colorspace','rgb','-color_range','pc',
               '-f','hevc',str(hevc)])
    probe=json.loads(run_codec(['ffprobe','-v','error','-select_streams','v:0',
                               '-show_entries','stream=profile,pix_fmt,width,height,color_range,color_space,color_transfer,color_primaries',
                               '-of','json',str(hevc)]))
    streams=probe.get('streams',[])
    expected={'profile':'Rext','pix_fmt':'gbrp10le','width':width,'height':height,
              'color_range':'pc','color_space':'gbr','color_transfer':transfer,'color_primaries':'bt2020'}
    if len(streams)!=1 or any(streams[0].get(key)!=value for key,value in expected.items()):
        raise RuntimeError('encoded HEVC VUI/profile disagrees with 10-bit RGB color contract')
    # Mandatory actual codec check. Never mark a lossy or converted stream as
    # preserving codes based solely on encoder options or declared bit depth.
    decoded=decode_gbr(run_codec(['ffmpeg','-hide_banner','-loglevel','error','-threads','1',
                         '-i',str(hevc),'-frames:v','1','-pix_fmt','gbrp10le','-f','rawvideo','pipe:1']),width,height)
    if not np.array_equal(decoded,rgb):
        raise RuntimeError('10-bit codec changed RGB codes; refusing save')
    return hevc.read_bytes(),decoded


def generate_map(base_linear,hdr,headroom):
    lib=ctypes.CDLL(str(Path(__file__).parent/'build/libgainmap_bridge.so'))
    ptr=ctypes.POINTER(ctypes.c_double)
    lib.gm_encode.argtypes=[ctypes.c_uint,ctypes.c_uint,ptr,ptr,ctypes.c_double,ptr,
                           ctypes.POINTER(ctypes.c_ushort),ctypes.c_char_p,ctypes.c_uint]
    lib.gm_encode.restype=ctypes.c_int
    base=np.ascontiguousarray(base_linear,dtype=np.float64)
    alternate=np.ascontiguousarray(hdr,dtype=np.float64)
    bounds=np.zeros(6,dtype=np.float64)
    codes=np.empty(base.shape,dtype=np.uint16)
    error=ctypes.create_string_buffer(1024)
    height,width,_=base.shape
    if lib.gm_encode(width,height,base.ctypes.data_as(ptr),alternate.ctypes.data_as(ptr),headroom,
                     bounds.ctypes.data_as(ptr),codes.ctypes.data_as(ctypes.POINTER(ctypes.c_ushort)),error,len(error)):
        raise ValueError(error.value.decode(errors='replace'))
    metadata={'base':'SDR','use_base_color_space':True,'base_headroom_log2':0.0,
              'alternate_headroom_log2':math.log2(headroom),
              'channels':[[float(bounds[c]),float(bounds[c+3]),1.0,1/64,1/64] for c in range(3)]}
    return codes,metadata


def save_pair(sdr,hdr,sdr_frame,hdr_frame,destination,headroom=16.0):
    validate_pair(sdr,hdr,sdr_frame,hdr_frame,headroom)
    # Caller synchronizes the pair and must not mutate it during this call;
    # two array copies cannot provide an atomic snapshot of external writes.
    # The private copies keep both math passes consistent afterwards.
    sdr=np.array(sdr,dtype=np.float64,copy=True)
    hdr=np.array(hdr,dtype=np.float64,copy=True)
    validate_pair(sdr,hdr,sdr_frame,hdr_frame,headroom)
    base_codes=np.floor(srgb_encode(sdr)*1023+0.5).astype(np.uint16)
    destination=Path(destination)
    destination.parent.mkdir(parents=True,exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='heif-save-',dir=destination.parent) as tmp:
        tmp=Path(tmp)
        base_hevc,decoded_base=encode_codes(base_codes,tmp,'base','iec61966-2-1')
        # Recompute AFTER final base quantization and actual encode/decode, so
        # this map describes the pixels that a decoder will actually receive.
        map_codes,metadata=generate_map(srgb_decode(decoded_base),hdr,headroom)
        map_hevc,_=encode_codes(map_codes,tmp,'map','linear')
        container,codec_info=build_heif(base_hevc,map_hevc,sdr_frame.width,sdr_frame.height,metadata)
        staged=tmp/'output.heic'
        staged.write_bytes(container)
        # Atomic rename only after encoding and all mandatory code checks pass.
        os.replace(staged,destination)
    return {'path':str(destination),'bytes':len(container),'frame':dataclasses.asdict(sdr_frame),
            'metadata':metadata,'encoded_bit_depth':10,'gainmap_bit_depth':10,
            'codec_code_roundtrip_exact':True,'jpeg_stages':0,
            'profile_idc':codec_info['base']['ptl'][0]&31,
            'chroma_format_idc':codec_info['base']['chroma'],
            'device_verified':False}
