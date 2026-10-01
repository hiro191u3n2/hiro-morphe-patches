"""Independent structural reader and libheif RGB decoder for Android gainmap-save host QA.

Does not import the writer/parser. The unpatched installed decoder does not
apply tmap: metadata reconstruction below is separate, explicitly so reported.
"""
import ctypes as C
import ctypes.util
import math
import os
from pathlib import Path
import struct

import numpy as np


def boxes(blob,begin=0,end=None):
    end=len(blob) if end is None else end
    while begin<end:
        if end-begin<8: raise ValueError('truncated box header')
        length,kind=struct.unpack_from('>I4s',blob,begin)
        if length<8 or begin+length>end: raise ValueError('invalid box extent')
        yield kind,begin+8,begin+length
        begin+=length


def unique_boxes(blob,begin=0,end=None):
    result={}
    for kind,start,stop in boxes(blob,begin,end):
        if kind in result: raise ValueError('duplicate box in restricted layout')
        result[kind]=(start,stop)
    return result


def parse_iso(payload):
    if len(payload)!=142 or payload[:6]!=b'\x00\x00\x00\x00\x00\xc0':
        raise ValueError('unexpected tmap version / ISO flags / payload size')
    position=6
    def rational(signed):
        nonlocal position
        n,d=struct.unpack_from('>iI' if signed else '>II',payload,position)
        position+=8
        if not d: raise ValueError('zero metadata denominator')
        return n/d
    base,alternate=rational(False),rational(False)
    channels=[]
    for _ in range(3):
        channels.append([rational(True),rational(True),rational(False),rational(True),rational(True)])
    if not (0<=base<alternate) or not all(c[0]<=c[1] and c[2]>0 and min(c[3:])>=0 for c in channels):
        raise ValueError('invalid metadata semantics')
    return dict(base_headroom_log2=base,alternate_headroom_log2=alternate,channels=channels)


def inspect_file(path):
    blob=Path(path).read_bytes()
    top=unique_boxes(blob)
    if set(top)!={b'ftyp',b'meta',b'mdat'}: raise ValueError('unexpected top-level boxes')
    fstart,fend=top[b'ftyp']; ftyp=blob[fstart:fend]
    if ftyp[:4]!=b'heix' or ftyp[8:]!=b'mif1heixtmap': raise ValueError('wrong brands')
    mstart,mend=top[b'meta']
    if blob[mstart:mstart+4]!=bytes(4): raise ValueError('meta version')
    meta=unique_boxes(blob,mstart+4,mend)
    pitm=meta[b'pitm'][0]
    if blob[pitm:pitm+6]!=bytes(4)+b'\x00\x01': raise ValueError('wrong primary')
    iprp=unique_boxes(blob,*meta[b'iprp'])
    props=list(boxes(blob,*iprp[b'ipco']))
    ipma=blob[slice(*iprp[b'ipma'])]
    if ipma[:8]!=bytes(4)+struct.pack('>I',3): raise ValueError('ipma header')
    assoc={};pos=8
    for _ in range(3):
        item,count=struct.unpack_from('>HB',ipma,pos);pos+=3
        assoc[item]=[(v&127,bool(v&128)) for v in ipma[pos:pos+count]];pos+=count
    if pos!=len(ipma) or set(assoc)!={1,2,3}: raise ValueError('ipma entries')
    item_props={}
    for item,associations in assoc.items():
        values={}
        for index,essential in associations:
            if index<1 or index>len(props): raise ValueError('bad property index')
            kind,start,stop=props[index-1]
            if kind in values: raise ValueError('duplicate image property')
            values[kind]=blob[start:stop]
            if kind==b'hvcC' and not essential: raise ValueError('nonessential decoder config')
        if values[b'pixi']!=bytes(4)+b'\x03\x0a\x0a\x0a': raise ValueError('not RGB10 pixi')
        transfer={1:1,2:8,3:16}[item]
        matrix=0 if item==3 else 9
        if values[b'colr']!=b'nclx'+struct.pack('>HHHB',9,transfer,matrix,128): raise ValueError('color contract')
        if item<3:
            hvcc=values[b'hvcC']
            if hvcc[0]!=1 or hvcc[16]&3!=1 or hvcc[17]&7!=2 or hvcc[18]&7!=2 or hvcc[21]&3!=3:
                raise ValueError('not 420/10bit/length4 hvcC')
        item_props[item]=values
    iinf=meta[b'iinf'][0]
    if blob[iinf:iinf+6]!=bytes(4)+b'\x00\x03': raise ValueError('iinf header')
    infos={}
    for kind,start,stop in boxes(blob,iinf+6,meta[b'iinf'][1]):
        data=blob[start:stop]
        if kind!=b'infe' or data[0]!=2: raise ValueError('infe version')
        item,protection=struct.unpack_from('>HH',data,4)
        infos[item]=(int.from_bytes(data[1:4],'big'),data[8:12])
        if protection: raise ValueError('protected item')
    if infos!={1:(0,b'hvc1'),2:(1,b'hvc1'),3:(0,b'tmap')}: raise ValueError('item types/visibility')
    iref=meta[b'iref'][0]
    refs=unique_boxes(blob,iref+4,meta[b'iref'][1])
    if set(refs)!={b'dimg'} or blob[slice(*refs[b'dimg'])]!=struct.pack('>HHHH',3,2,1,2):
        raise ValueError('incorrect tmap dependency order')
    groups=unique_boxes(blob,*meta[b'grpl'])
    if set(groups)!={b'altr'} or blob[slice(*groups[b'altr'])]!=bytes(4)+struct.pack('>IIII',4,2,3,1):
        raise ValueError('incorrect alternative rendition group')
    iloc=blob[slice(*meta[b'iloc'])]
    if iloc[:8]!=bytes(4)+b'\x44\x00\x00\x03': raise ValueError('iloc header')
    items={}; extents=[];pos=8
    for _ in range(3):
        item,reference,count,offset,length=struct.unpack_from('>HHHII',iloc,pos);pos+=14
        if reference or count!=1 or item in items: raise ValueError('invalid item extent record')
        if offset<top[b'mdat'][0] or offset+length>top[b'mdat'][1]: raise ValueError('item outside mdat')
        extents.append((offset,offset+length));items[item]=blob[offset:offset+length]
    if pos!=len(iloc) or set(items)!={1,2,3}: raise ValueError('iloc entries')
    extents.sort()
    if extents[0][0]!=top[b'mdat'][0] or extents[-1][1]!=top[b'mdat'][1] or any(a[1]!=b[0] for a,b in zip(extents,extents[1:])):
        raise ValueError('overlapping, missing or trailing item bytes')
    metadata=parse_iso(items[3])
    for item in (1,2):
        sample=items[item];pos=0;nal_count=0
        while pos<len(sample):
            if pos+4>len(sample): raise ValueError('truncated NAL size')
            size=struct.unpack_from('>I',sample,pos)[0];pos+=4
            if size<3 or pos+size>len(sample): raise ValueError('invalid NAL payload')
            if (sample[pos]>>1)&63 in (32,33,34): raise ValueError('unexpected in-band parameter set')
            pos+=size;nal_count+=1
        if not nal_count: raise ValueError('no picture NALs')
    width,height=struct.unpack('>II',item_props[1][b'ispe'][4:])
    if any(p[b'ispe']!=item_props[1][b'ispe'] for p in item_props.values()): raise ValueError('geometry mismatch')
    return {'width':width,'height':height,'metadata':metadata,'bytes':len(blob),
            'coded_items':[1,2],'tmap_id':3,'brands':['mif1','heix','tmap'],
            'declared_depths':[10,10],'jpeg_items':0}


class HeifError(C.Structure):
    _fields_=[('code',C.c_int),('subcode',C.c_int),('message',C.c_char_p)]


def decode_libheif(path,item):
    default='/opt/codex/runtimes/codex-primary-runtime/dependencies/native/libheif/libheif/lib/libheif.so.1'
    library=os.environ.get('HEIF_VERIFY_LIBRARY') or (default if Path(default).exists() else C.util.find_library('heif'))
    if not library: raise RuntimeError('libheif decoder unavailable')
    lib=C.CDLL(library)
    def signature(name,result,*args):
        f=getattr(lib,name);f.restype=result;f.argtypes=list(args);return f
    ptr=C.c_void_p; pp=C.POINTER(ptr)
    alloc=signature('heif_context_alloc',ptr)
    free=signature('heif_context_free',None,ptr)
    read=signature('heif_context_read_from_file',HeifError,ptr,C.c_char_p,ptr)
    get=signature('heif_context_get_image_handle',HeifError,ptr,C.c_uint32,pp)
    release=signature('heif_image_handle_release',None,ptr)
    decode=signature('heif_decode_image',HeifError,ptr,pp,C.c_int,C.c_int,ptr)
    img_release=signature('heif_image_release',None,ptr)
    width_fn=signature('heif_image_get_width',C.c_int,ptr,C.c_int)
    height_fn=signature('heif_image_get_height',C.c_int,ptr,C.c_int)
    depth_fn=signature('heif_image_get_bits_per_pixel_range',C.c_int,ptr,C.c_int)
    plane_fn=signature('heif_image_get_plane_readonly',C.POINTER(C.c_uint8),ptr,C.c_int,C.POINTER(C.c_int))
    version=signature('heif_get_version',C.c_char_p)().decode()
    def check(error):
        if error.code: raise RuntimeError(f'libheif {error.code}/{error.subcode}: {error.message.decode()}')
    context=alloc();handle=ptr();image=ptr()
    if not context: raise MemoryError('libheif allocation failed')
    try:
        check(read(context,os.fsencode(path),None));check(get(context,item,C.byref(handle)))
        check(decode(handle,C.byref(image),1,3,None))  # RGB, planar 4:4:4; no 8-bit request
        channels=[]
        for channel in (3,4,5):  # R,G,B
            width,height=width_fn(image,channel),height_fn(image,channel)
            if depth_fn(image,channel)!=10: raise RuntimeError('decoder did not expose 10-bit RGB')
            stride=C.c_int();plane=plane_fn(image,channel,C.byref(stride))
            if not plane or width<=0 or height<=0 or stride.value<width*2: raise RuntimeError('invalid decoder plane')
            raw=C.string_at(plane,stride.value*height)
            channel_data=np.ndarray((height,width),dtype='<u2',buffer=raw,strides=(stride.value,2)).copy()
            channels.append(channel_data)
        return np.stack(channels,axis=2),version
    finally:
        if image: img_release(image)
        if handle: release(handle)
        free(context)


def reconstruct(base_codes,map_codes,metadata):
    encoded=base_codes.astype(np.float64)/1023
    base=np.where(encoded<0.081,encoded/4.5,((encoded+0.099)/1.099)**(1/0.45))
    values=map_codes.astype(np.float64)/1023
    channels=np.asarray(metadata['channels'])
    normalized=values**(1/channels[:,2])
    log_gain=channels[:,0]+(channels[:,1]-channels[:,0])*normalized
    return (base+channels[:,3])*np.exp2(log_gain)-channels[:,4]


if __name__=='__main__':
    import json,sys
    result=inspect_file(sys.argv[1])
    for item in result['coded_items']:
        samples,version=decode_libheif(sys.argv[1],item)
        result[f'item_{item}']={'shape':list(samples.shape),'max_code':int(samples.max()),'unique_codes':int(np.unique(samples).size)}
    result['libheif']=version
    result['automatic_tmap_render_tested']=False
    print(json.dumps(result,indent=2))
