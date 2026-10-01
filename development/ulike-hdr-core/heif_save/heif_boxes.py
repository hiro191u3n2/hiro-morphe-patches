"""Small, deliberately restricted HEIF/tmap writer. No JPEG codec or embedding.

Only one intra HEVC RGB 4:4:4 10-bit image per item, SDR base, RGB gainmap.
This is not a general ISO BMFF editor or an Android integration layer.
"""
import math
import re
import struct
from fractions import Fraction


def u16(n): return struct.pack('>H', n)
def u32(n): return struct.pack('>I', n)
def box(kind, data): return u32(8 + len(data)) + kind.encode('ascii') + data
def full(kind, data, version=0, flags=0):
    return box(kind, bytes([version]) + flags.to_bytes(3, 'big') + data)


def fraction(value, signed=False):
    if not math.isfinite(value) or (not signed and value < 0):
        raise ValueError('invalid metadata rational')
    # Reproducible signed 32-bit numerator, unsigned denominator, <= 2^-25
    # absolute rounding for ordinary photographic ranges. Reject, never wrap.
    denominator = 1 << 24
    maximum = (1 << (31 if signed else 32)) - 1
    while denominator > 1 and abs(value) * denominator > maximum:
        denominator //= 2
    numerator = round(value * denominator)
    if abs(numerator) > maximum:
        raise ValueError('metadata rational out of range')
    f = Fraction(numerator, denominator)
    return struct.pack('>iI' if signed else '>II', f.numerator, f.denominator)


def iso_metadata(metadata):
    """ISO 21496-1 version-0 separate-rational form used by libultrahdr.

    This subset explicitly rejects HDR base/backward mapping. Channel values
    are RGB log2 bounds, encoding gamma, and linear offsets, respectively.
    No JPEG APP2 namespace prefix belongs in this HEIF item.
    """
    if metadata.get('base') != 'SDR' or metadata.get('use_base_color_space') is not True:
        raise ValueError('only SDR base in the base color space is implemented')
    b, a = metadata['base_headroom_log2'], metadata['alternate_headroom_log2']
    if not (0 <= b < a and math.isfinite(a)):
        raise ValueError('invalid display headroom')
    channels = metadata['channels']
    if len(channels) != 3:
        raise ValueError('RGB metadata required')
    def encoded_value(encoded, signed=False):
        n,d=struct.unpack('>iI' if signed else '>II',encoded)
        return n/d
    base_fraction,alternate_fraction=fraction(b),fraction(a)
    if encoded_value(base_fraction)>=encoded_value(alternate_fraction):
        raise ValueError('headroom collapses at ISO rational precision')
    payload = u16(0) + u16(0) + bytes([0xC0]) + base_fraction + alternate_fraction
    for c in channels:
        lo, hi, gamma, base_offset, alternate_offset = c
        if not all(math.isfinite(x) for x in c) or lo > hi or gamma <= 0 or min(base_offset,alternate_offset) < 0:
            raise ValueError('invalid gainmap channel')
        lo_f,hi_f,gamma_f=fraction(lo,True),fraction(hi,True),fraction(gamma)
        if encoded_value(gamma_f)<=0:
            raise ValueError('gamma collapses at ISO rational precision')
        if lo!=hi and encoded_value(lo_f,True)==encoded_value(hi_f,True):
            raise ValueError('gain range collapses at ISO rational precision')
        payload += lo_f + hi_f + gamma_f + fraction(base_offset, True) + fraction(alternate_offset, True)
    return payload


class Bits:
    def __init__(self, data): self.data, self.pos = data, 0
    def read(self, n):
        if self.pos+n > len(self.data)*8: raise ValueError('truncated SPS')
        result=0
        for _ in range(n):
            result = (result << 1) | ((self.data[self.pos//8] >> (7-self.pos%8)) & 1)
            self.pos += 1
        return result
    def ue(self):
        z=0
        while self.read(1)==0:
            z+=1
            if z>31: raise ValueError('oversized SPS Exp-Golomb')
        return (1<<z)-1+self.read(z)


def rbsp(nal):
    result=bytearray(); zeros=0
    for byte in nal[2:]:
        if zeros >= 2 and byte == 3:
            zeros=0
            continue
        result.append(byte)
        zeros = zeros+1 if byte==0 else 0
    return bytes(result)


def parse_sps(nal):
    bits=Bits(rbsp(nal))
    bits.read(4); layers=bits.read(3); nested=bits.read(1)
    if layers==7: raise ValueError('invalid HEVC sublayer count')
    ptl=bytes(bits.read(8) for _ in range(12))
    flags=[(bits.read(1),bits.read(1)) for _ in range(layers)]
    if layers:
        for _ in range(layers,8): bits.read(2)
    for profile, level in flags:
        if profile: bits.read(88)
        if level: bits.read(8)
    bits.ue(); chroma=bits.ue()
    separate=bits.read(1) if chroma==3 else 0
    width,height=bits.ue(),bits.ue()
    crop=[bits.ue() for _ in range(4)] if bits.read(1) else [0]*4
    depth_l,depth_c=bits.ue()+8,bits.ue()+8
    if chroma!=3 or separate or depth_l!=10 or depth_c!=10:
        raise ValueError('writer requires ordinary RGB 4:4:4 10-bit SPS')
    width-=crop[0]+crop[1]; height-=crop[2]+crop[3]
    return dict(width=width,height=height,chroma=chroma,depth=depth_l,
                layers=layers+1,nested=nested,ptl=ptl)


def hevc_item(annexb, width, height):
    starts=list(re.finditer(b'\x00\x00\x00?\x01',annexb))
    if not starts or starts[0].start()!=0:
        raise ValueError('expected Annex B HEVC')
    nals=[]
    for i,s in enumerate(starts):
        end=starts[i+1].start() if i+1<len(starts) else len(annexb)
        nal=annexb[s.end():end].rstrip(b'\0')
        if len(nal)<3 or nal[0]&0x80 or ((nal[0]&1)<<5)|(nal[1]>>3) or (nal[1]&7)!=1:
            raise ValueError('invalid or layered HEVC NAL')
        nals.append(nal)
    params={t:[n for n in nals if (n[0]>>1)&63==t] for t in (32,33,34)}
    if any(len(ns)!=1 for ns in params.values()):
        raise ValueError('exactly one VPS/SPS/PPS is required')
    vcl=[n for n in nals if ((n[0]>>1)&63)<=31]
    if not vcl or sum(bool(rbsp(n)[0]&0x80) for n in vcl)!=1:
        raise ValueError('exactly one coded picture is required')
    if any(((n[0]>>1)&63) not in (19,20) for n in vcl):
        raise ValueError('only independently decodable IDR pictures supported')
    info=parse_sps(params[33][0])
    if (info['width'],info['height'])!=(width,height):
        raise ValueError('SPS dimensions disagree with image contract')
    hvcc=(b'\x01'+info['ptl']+b'\xf0\x00\xfc'+bytes([0xfc|info['chroma'],0xfa,0xfa])
          +b'\x00\x00'+bytes([(info['layers']<<3)|(info['nested']<<2)|3,3]))
    for kind,ns in params.items():
        hvcc+=bytes([0x80|kind])+u16(len(ns))
        for n in ns: hvcc+=u16(len(n))+n
    samples=b''.join(u32(len(n))+n for n in nals if ((n[0]>>1)&63) not in params)
    return hvcc,samples,info


def nclx(transfer):
    # BT.2020 RGB primaries, identity matrix, full range. The numerical map
    # uses linear transfer; derived rendition uses PQ after reconstruction.
    return box('colr',b'nclx'+u16(9)+u16(transfer)+u16(0)+b'\x80')


def build_heif(base_hevc, map_hevc, width, height, metadata):
    # Internal trusted-encoder interface: encode_codes() checks actual color
    # VUI using ffprobe before invoking this. Not a generic HEVC validator.
    base_cfg,base_sample,base_info=hevc_item(base_hevc,width,height)
    map_cfg,map_sample,map_info=hevc_item(map_hevc,width,height)
    tmap_sample=b'\x00'+iso_metadata(metadata)
    data=[base_sample,map_sample,tmap_sample]
    ftyp=box('ftyp',b'heix'+u32(0)+b'mif1heixtmap')
    properties=[]; associations=[]
    for item_id,cfg,transfer in ((1,base_cfg,13),(2,map_cfg,8),(3,None,16)):
        local=[full('ispe',u32(width)+u32(height)),full('pixi',b'\x03\x0a\x0a\x0a')]
        essential=[False,False]
        if cfg: local.append(box('hvcC',cfg)); essential.append(True)
        local.append(nclx(transfer)); essential.append(False)
        association=[]
        for prop,needed in zip(local,essential):
            properties.append(prop)
            association.append(len(properties)|(0x80 if needed else 0))
        associations.append(u16(item_id)+bytes([len(association),*association]))
    iprp=box('iprp',box('ipco',b''.join(properties))+full('ipma',u32(3)+b''.join(associations)))
    entries=b''.join(full('infe',u16(i)+u16(0)+kind+name+b'\x00',2,1 if i==2 else 0)
                     for i,kind,name in ((1,b'hvc1',b'SDR10'),(2,b'hvc1',b'RGB gainmap10'),(3,b'tmap',b'HDR alternate')))
    iinf=full('iinf',u16(3)+entries)
    iref=full('iref',box('dimg',u16(3)+u16(2)+u16(1)+u16(2)))
    grpl=box('grpl',full('altr',u32(4)+u32(2)+u32(3)+u32(1)))
    def meta(start):
        loc=b'\x44\x00'+u16(3)
        offset=start
        for i,payload in enumerate(data,1):
            loc+=u16(i)+u16(0)+u16(1)+u32(offset)+u32(len(payload))
            offset+=len(payload)
        return full('meta',full('hdlr',u32(0)+b'pict'+bytes(12)+b'hiro host gainmap\x00')
                    +full('pitm',u16(1))+full('iloc',loc)+iinf+iprp+iref+grpl)
    preliminary=meta(0)
    final=meta(len(ftyp)+len(preliminary)+8)
    if len(final)!=len(preliminary): raise AssertionError('unstable offsets')
    result=ftyp+final+box('mdat',b''.join(data))
    return result, {'base':base_info,'gainmap':map_info}
