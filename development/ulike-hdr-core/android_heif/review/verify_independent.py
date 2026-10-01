#!/usr/bin/env python3
"""Independent HEIF box/offset, native 10-bit decode and malformed-input review."""
import argparse
import ctypes as C
import hashlib
import json
import os
from pathlib import Path
import struct
import subprocess
import tempfile

if not __debug__:
    raise RuntimeError("Independent verification requires assertions")


def run(command):
    p=subprocess.run([str(x) for x in command],stdout=subprocess.PIPE,
                     stderr=subprocess.PIPE,text=True)
    if p.returncode:
        raise RuntimeError(f'{command[0]} failed ({p.returncode}): {p.stdout[-1000:]} {p.stderr[-4000:]}')
    return p.stdout


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def u16(b,p):return struct.unpack_from(">H",b,p)[0]
def u32(b,p):return struct.unpack_from(">I",b,p)[0]
def i32(b,p):return struct.unpack_from(">i",b,p)[0]


def boxes(data,start,end):
    out=[]
    while start<end:
        if end-start<8:raise ValueError("short box header")
        size=u32(data,start)
        if size<8 or size>end-start:raise ValueError("invalid box size")
        out.append((data[start+4:start+8],start+8,start+size))
        if len(out)>128:raise ValueError("excess boxes")
        start+=size
    return out


def children(data,entries):
    result={}
    for kind,start,end in entries:
        if kind in result:raise ValueError("duplicate box")
        result[kind]=(start,end)
    return result


def inspect(data):
    """Reader independent of the Java writer. Bounds every iloc extent first."""
    if len(data)>65*1024*1024:raise ValueError("file bound")
    data=bytes(data)
    top=children(data,boxes(data,0,len(data)))
    if set(top)!={b'ftyp',b'meta',b'mdat'}:raise ValueError("top-level items")
    s,e=top[b'ftyp']
    if data[s:e]!=b'heix\0\0\0\0mif1heix':raise ValueError("brands")
    s,e=top[b'meta']
    if data[s:s+4]!=bytes(4):raise ValueError("meta fullbox")
    meta=children(data,boxes(data,s+4,e))
    if set(meta)!={b'hdlr',b'pitm',b'iloc',b'iinf',b'iprp'}:raise ValueError("meta structure")
    s,e=meta[b'pitm']
    if data[s:e]!=b'\0\0\0\0\0\1':raise ValueError("primary item")
    s,e=meta[b'iloc'];p=s+4
    if data[s:s+4]!=bytes(4) or data[p:p+2]!=b'\x44\0' or u16(data,p+2)!=1:raise ValueError("iloc header")
    p+=4
    if (u16(data,p),u16(data,p+2),u16(data,p+4))!=(1,0,1):raise ValueError("iloc item")
    offset_field=p+6;length_field=p+10;offset=u32(data,offset_field);length=u32(data,length_field)
    ms,me=top[b'mdat']
    if p+14!=e or offset!=ms or length!=me-ms or offset>len(data) or length>len(data)-offset:
        raise ValueError("hostile or inconsistent item extent")
    s,e=meta[b'iinf']
    if data[s:s+6]!=b'\0\0\0\0\0\1':raise ValueError("iinf count")
    inf=children(data,boxes(data,s+6,e));s,e=inf[b'infe']
    if data[s:e]!=b'\2\0\0\0\0\1\0\0hvc1HLG10\0':raise ValueError("image item declaration")
    s,e=meta[b'iprp'];props=children(data,boxes(data,s,e))
    if set(props)!={b'ipco',b'ipma'}:raise ValueError("properties")
    s,e=props[b'ipco'];entries=boxes(data,s,e);prop=children(data,entries)
    if set(prop) not in ({b'ispe',b'pixi',b'hvcC',b'colr'},{b'ispe',b'pixi',b'hvcC',b'colr',b'clap'}):raise ValueError("property subset")
    s,e=props[b'ipma'];q=data[s:e]
    if q[:10]!=b'\0\0\0\0\0\0\0\1\0\1' or q[10]!=len(entries) or len(q)!=11+len(entries):raise ValueError("property associations")
    for index,value in enumerate(q[11:],1):
        if value&127!=index:raise ValueError("property index")
        if entries[index-1][0] in (b'hvcC',b'clap') and not(value&128):raise ValueError("required property not essential")
    s,e=prop[b'ispe']
    if e-s!=12 or data[s:s+4]!=bytes(4):raise ValueError("ispe")
    width,height=u32(data,s+4),u32(data,s+8)
    if not(1<=width<=32768 and 1<=height<=32768 and width*height<=32_000_000):raise ValueError("pixel budget")
    s,e=prop[b'pixi']
    if data[s:e]!=b'\0\0\0\0\3\12\12\12':raise ValueError("10-bit pixi")
    s,e=prop[b'colr'];nclx=data[s:e]
    if len(nclx)!=11 or nclx[:4]!=b'nclx' or struct.unpack_from('>HHH',nclx,4)!=(9,18,9) or nclx[10]&127:raise ValueError("HLG nclx")
    crop=None
    if b'clap' in prop:
        s,e=prop[b'clap']
        if e-s!=32:raise ValueError("clap length")
        cw,wd,ch,hd=u32(data,s),u32(data,s+4),u32(data,s+8),u32(data,s+12)
        hx,dx,vy,dy=i32(data,s+16),u32(data,s+20),i32(data,s+24),u32(data,s+28)
        if wd!=1 or hd!=1 or dx!=2 or dy!=2:raise ValueError("clap denominators")
        left=(hx+width-cw)/2;top_=(vy+height-ch)/2
        if not(left.is_integer() and top_.is_integer() and cw>0 and ch>0 and left>=0 and top_>=0 and left+cw<=width and top_+ch<=height):raise ValueError("clap extent")
        crop=(int(left),int(top_),cw,ch)
    s,e=prop[b'hvcC'];cfg=data[s:e]
    if len(cfg)<23 or cfg[0]!=1 or cfg[1]&31!=2 or cfg[16]&3!=1 or cfg[17]&7!=2 or cfg[18]&7!=2 or cfg[21]&3!=3 or cfg[22]!=3:raise ValueError("hvcC Main10 configuration")
    pos=23;parameter=[]
    for expected in (32,33,34):
        if pos+5>len(cfg) or cfg[pos]!=(128|expected) or u16(cfg,pos+1)!=1:raise ValueError("hvcC array")
        count=u16(cfg,pos+3);pos+=5
        if count<3 or pos+count>len(cfg):raise ValueError("hvcC NAL length")
        nal=cfg[pos:pos+count];pos+=count
        if (nal[0]>>1)&63!=expected:raise ValueError("hvcC NAL type")
        parameter.append(nal)
    if pos!=len(cfg):raise ValueError("hvcC trailing bytes")
    nals=[];pos=offset
    while pos<offset+length:
        if pos+4>offset+length:raise ValueError("sample NAL header")
        count=u32(data,pos);pos+=4
        if count<3 or count>offset+length-pos:raise ValueError("sample NAL length")
        nals.append(data[pos:pos+count]);pos+=count
        if len(nals)>4096:raise ValueError("sample NAL count")
    elementary=b''.join(b'\0\0\0\1'+n for n in parameter+nals)
    return {'width':width,'height':height,'crop':crop,'full_range':bool(nclx[10]&128),
            'offset_field':offset_field,'length_field':length_field,'elementary':elementary,
            'parameter_sets':len(parameter),'sample_nals':len(nals)}


class Error(C.Structure):
    _fields_=[('code',C.c_int),('subcode',C.c_int),('message',C.c_char_p)]


class ColorOptions(C.Structure):
    _fields_=[('version',C.c_uint8),('downsampling',C.c_int),('upsampling',C.c_int),('preferred_only',C.c_uint8)]


class Nclx(C.Structure):
    _fields_=[('version',C.c_uint8),('primaries',C.c_int),('transfer',C.c_int),('matrix',C.c_int),('full',C.c_uint8),('coordinates',C.c_float*8)]


class DecodeOptions(C.Structure):
    # Public libheif 1.23.2 heif_decoding.h ABI, allocated by libheif itself.
    _fields_=[('version',C.c_uint8),('ignore_transformations',C.c_uint8),
              ('start',C.c_void_p),('progress',C.c_void_p),('end',C.c_void_p),('user',C.c_void_p),
              ('convert_hdr_to_8bit',C.c_uint8),('strict_decoding',C.c_uint8),('decoder_id',C.c_char_p),
              ('color_options',ColorOptions),('cancel',C.c_void_p),('extended_color_options',C.c_void_p),
              ('ignore_sequence',C.c_int),('output_nclx',C.c_void_p),('library_threads',C.c_int),
              ('codec_threads',C.c_int),('autocorrect',C.c_uint8),('nclx_passthrough',C.c_uint8)]


def decode(path,library):
    import numpy as np
    lib=C.CDLL(str(library));ptr=C.c_void_p;pp=C.POINTER(ptr)
    def sig(name,result,*args):
        f=getattr(lib,name);f.restype=result;f.argtypes=list(args);return f
    def ok(e):
        if e.code:raise RuntimeError(f'libheif {e.code}/{e.subcode}: {e.message.decode()}')
    alloc=sig('heif_context_alloc',ptr);free=sig('heif_context_free',None,ptr)
    read=sig('heif_context_read_from_file',Error,ptr,C.c_char_p,ptr)
    get=sig('heif_context_get_primary_image_handle',Error,ptr,pp)
    release_handle=sig('heif_image_handle_release',None,ptr)
    release_image=sig('heif_image_release',None,ptr)
    allocate_options=sig('heif_decoding_options_alloc',C.POINTER(DecodeOptions))
    free_options=sig('heif_decoding_options_free',None,C.POINTER(DecodeOptions))
    decode_image=sig('heif_decode_image',Error,ptr,pp,C.c_int,C.c_int,C.POINTER(DecodeOptions))
    get_width=sig('heif_image_get_width',C.c_int,ptr,C.c_int)
    get_height=sig('heif_image_get_height',C.c_int,ptr,C.c_int)
    get_bits=sig('heif_image_get_bits_per_pixel_range',C.c_int,ptr,C.c_int)
    get_plane=sig('heif_image_get_plane_readonly',C.POINTER(C.c_uint8),ptr,C.c_int,C.POINTER(C.c_int))
    colorspace=sig('heif_image_get_colorspace',C.c_int,ptr)
    chroma=sig('heif_image_get_chroma_format',C.c_int,ptr)
    get_nclx=sig('heif_image_handle_get_nclx_color_profile',Error,ptr,C.POINTER(C.POINTER(Nclx)))
    free_nclx=sig('heif_nclx_color_profile_free',None,C.POINTER(Nclx))
    context=alloc();handle=ptr();image=ptr();profile=C.POINTER(Nclx)();options=allocate_options()
    if not context or not options:raise MemoryError('libheif allocation')
    try:
        if options.contents.version<10:raise RuntimeError('libheif needs native NCLX passthrough option v10')
        options.contents.strict_decoding=1
        options.contents.convert_hdr_to_8bit=0
        options.contents.nclx_passthrough=1
        options.contents.codec_threads=1
        ok(read(context,os.fsencode(path),None));ok(get(context,C.byref(handle)))
        ok(get_nclx(handle,C.byref(profile)))
        nclx=(profile.contents.primaries,profile.contents.transfer,profile.contents.matrix,bool(profile.contents.full))
        ok(decode_image(handle,C.byref(image),99,99,options))
        if colorspace(image)!=0 or chroma(image)!=1:
            raise RuntimeError(f'native YCbCr420 expected: colorspace={colorspace(image)}, chroma={chroma(image)}')
        planes=[]
        for channel in (0,1,2):
            w,h=get_width(image,channel),get_height(image,channel)
            if w<1 or h<1 or w*h>32_000_000 or get_bits(image,channel)!=10:raise RuntimeError('decode dimensions/depth')
            stride=C.c_int();plane=get_plane(image,channel,C.byref(stride))
            if not plane or stride.value<w*2 or stride.value*h>256_000_000:raise RuntimeError('decoded plane allocation bound')
            raw=C.string_at(plane,stride.value*h)
            samples=np.ndarray((h,w),dtype=np.uint16,buffer=raw,strides=(stride.value,2)).copy()
            if samples.max()>1023:raise RuntimeError('non-10-bit output code')
            planes.append(samples)
        return planes,nclx,sig('heif_get_version',C.c_char_p)().decode()
    finally:
        if profile:free_nclx(profile)
        if image:release_image(image)
        if handle:release_handle(handle)
        free_options(options);free(context)


def split_nals(data):
    import re
    chunks=re.split(b'\x00\x00(?:\x00)?\x01',data)
    assert not chunks[0].strip(b'\0')
    return [x.rstrip(b'\0') for x in chunks[1:]]


def rbsp(nal):
    out=bytearray();zero=0
    for v in nal[2:]:
        if zero>=2 and v==3:zero=0;continue
        out.append(v);zero=zero+1 if v==0 else 0
    return bytes(out)


def escaped(header,data):
    out=bytearray(header);zero=0
    for v in data:
        if zero>=2 and v<=3:out.append(3);zero=0
        out.append(v);zero=zero+1 if v==0 else 0
    return bytes(out)


def replace_bits(nal,start,count,replacement):
    bits=''.join(f'{v:08b}' for v in rbsp(nal))
    result=bits[:start]+replacement+bits[start+count:]
    result+='0'*((-len(result))%8)
    return escaped(nal[:2],int(result,2).to_bytes(len(result)//8,'big'))


def mutations(source):
    nals=split_nals(source);idx={((n[0]>>1)&63):i for i,n in enumerate(nals)}
    def modified(kind,replacement):
        a=nals.copy();a[idx[kind]]=replacement;return b''.join(b'\0\0\0\1'+v for v in a)
    yield 'non-annexb-prefix',b'X'+source
    yield 'truncated-vps',modified(32,bytes([64,1,128]))
    yield 'truncated-pps',modified(34,bytes([68,1,128]))
    yield 'mismatched-vps-id',modified(32,replace_bits(nals[idx[32]],0,4,'0001'))
    yield 'mismatched-sps-vps-reference',modified(33,replace_bits(nals[idx[33]],0,4,'0001'))
    # Generated x265 fixtures have max_sub_layers_minus1=0 and IDs zero.
    assert (rbsp(nals[idx[33]])[0]>>1)&7==0
    assert ''.join(f'{v:08b}' for v in rbsp(nals[idx[33]]))[104]=='1'
    yield 'mismatched-sps-id',modified(33,replace_bits(nals[idx[33]],104,1,'010'))
    yield 'mismatched-pps-id',modified(34,replace_bits(nals[idx[34]],0,1,'010'))
    yield 'mismatched-pps-sps-reference',modified(34,replace_bits(nals[idx[34]],1,1,'010'))
    vcl=next(k for k in idx if k in (19,20))
    assert ''.join(f'{v:08b}' for v in rbsp(nals[idx[vcl]]))[2]=='1'
    yield 'mismatched-slice-pps-reference',modified(vcl,replace_bits(nals[idx[vcl]],2,1,'010'))
    yield 'truncated-idr',modified(vcl,bytes([vcl<<1,1,128]))
    for kind in (32,33,34,39):
        if kind in idx:
            yield f'bad-epb-{kind}',modified(kind,nals[idx[kind]]+b'\0\0\3\4')
    yield 'unsupported-layer',modified(vcl,bytes([nals[idx[vcl]][0]|1])+nals[idx[vcl]][1:])
    yield 'zero-temporal-id',modified(vcl,nals[idx[vcl]][:1]+b'\0'+nals[idx[vcl]][2:])
    yield 'forbidden-header-bit',modified(vcl,bytes([nals[idx[vcl]][0]|128])+nals[idx[vcl]][1:])
    yield 'second-picture',source+b'\0\0\0\1'+nals[idx[vcl]]
    yield 'missing-sps',b''.join(b'\0\0\0\1'+n for n in nals if ((n[0]>>1)&63)!=33)
    yield 'too-many-nals',source+(b'\0\0\0\1\x46\x01\x80'*4097)
    yield 'oversized-parameter',modified(33,nals[idx[33]]+b'\x55'*65536)


def main():
    import numpy as np
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('core','tools','libheif','report'):
        p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args();root=a.core/'android_heif'
    source=sorted((root/'src/main/java').rglob('*.java'))
    encoders=sorted((a.core/'android_encoder/src/main/java').rglob('*.java'))
    pins={str(v.relative_to(a.core)):sha(v) for v in source+encoders}
    jdk=a.tools/'jdk21/jdk-21.0.12.1+1/bin';sdk=a.tools/'android.jar'
    records=[];rejections=[];hostile_offsets=0
    with tempfile.TemporaryDirectory(prefix='ulike-heif-independent-') as temp:
        work=Path(temp);classes=work/'classes';classes.mkdir()
        # Match the module's Android API compile route. The frozen encoder's
        # Java lambda needs the JDK bootstrap metafactory; android.jar alone
        # does not supply javac's invokedynamic bootstrap stub.
        run([jdk/'javac','--release','8','-cp',sdk,'-d',classes,*encoders,*source])
        run([jdk/'javac','--release','11','-cp',classes,'-d',classes,root/'review/HeifReviewBridge.java'])
        bridge=[jdk/'java','-Xmx384m','-cp',classes,'com.hiro.ulike.hdr.heif.HeifReviewBridge']
        fixtures=work/'fixtures';fixtures.mkdir()
        for name,width,height,range_ in (('hlg_limited',64,32,'limited'),
                                         ('hlg_full',64,32,'full'),
                                         ('hlg_padded',66,34,'limited')):
            y,x=np.mgrid[:height,:width];cy,cx=np.mgrid[:height//2,:width//2]
            planes=[64+(x*13+y*29)%876,112+(cx*47+cy*31)%800,120+(cx*83+cy*23)%800]
            raw=fixtures/(name+'.input.yuv')
            raw.write_bytes(b''.join(v.astype('<u2').tobytes() for v in planes))
            run(['ffmpeg','-v','error','-y','-f','rawvideo','-pix_fmt','yuv420p10le','-s',f'{width}x{height}',
                 '-i',raw,'-frames:v','1','-pix_fmt','yuv420p10le','-c:v','libx265','-x265-params',
                 f'pools=1:frame-threads=1:repeat-headers=1:colorprim=bt2020:transfer=arib-std-b67:colormatrix=bt2020nc:range={range_}:log-level=error',
                 '-f','hevc',fixtures/(name+'.hevc')])
        for name,elementary,width,height,full,crop in (
                ('limited','hlg_limited.hevc',64,32,False,None),
                ('full','hlg_full.hevc',64,32,True,None),
                ('padded','hlg_padded.hevc',66,34,False,None),
                ('odd','hlg_padded.hevc',66,34,False,(0,0,65,33)),
                ('offset','hlg_padded.hevc',66,34,False,(2,2,63,31))):
            src=fixtures/elementary;dst=work/(name+'.heic')
            run([*bridge,'write',src,dst,width,height,str(full).lower(),*(crop or ())])
            data=dst.read_bytes();container=inspect(data)
            assert (container['width'],container['height'],container['crop'],container['full_range'])==(width,height,crop,full)
            planes,nclx,version=decode(dst,a.libheif)
            assert nclx==(9,18,9,full)
            recovered=work/(name+'.hevc');recovered.write_bytes(container['elementary'])
            probe=json.loads(run(['ffprobe','-v','error','-show_entries','stream=profile,pix_fmt,width,height,color_range,color_space,color_transfer,color_primaries','-of','json',recovered]))['streams'][0]
            assert (probe['profile'],probe['pix_fmt'],probe['width'],probe['height'])==('Main 10','yuv420p10le',width,height)
            assert (probe['color_primaries'],probe['color_transfer'],probe['color_space'],probe['color_range'])==('bt2020','arib-std-b67','bt2020nc','pc' if full else 'tv')
            raw=work/(name+'.yuv')
            run(['ffmpeg','-v','error','-y','-i',src,'-frames:v','1','-pix_fmt','yuv420p10le','-f','rawvideo',raw])
            coded=np.frombuffer(raw.read_bytes(),dtype='<u2');assert coded.size==width*height*3//2
            luma=width*height;chroma=luma//4
            expected=[coded[:luma].reshape(height,width),coded[luma:luma+chroma].reshape(height//2,width//2),coded[luma+chroma:].reshape(height//2,width//2)]
            if crop:
                x,y,w,h=crop
                expected=[expected[0][y:y+h,x:x+w],*[v[y//2:y//2+(h+1)//2,x//2:x//2+(w+1)//2] for v in expected[1:]]]
            differences=[]
            for actual,original in zip(planes,expected):
                assert actual.shape==original.shape,(name,actual.shape,original.shape)
                error=int(np.max(np.abs(actual.astype(np.int32)-original.astype(np.int32))))
                assert np.array_equal(actual,original),(name,error)
                differences.append(error)
            for field,value in (('offset_field',0),('offset_field',len(data)+1),('offset_field',0xffffffff),('length_field',0xffffffff)):
                bad=bytearray(data);struct.pack_into('>I',bad,container[field],value)
                try:inspect(bad)
                except ValueError:hostile_offsets+=1
                else:raise AssertionError('hostile iloc not rejected')
            records.append({'fixture':name,'container_dimensions':[width,height],'display_dimensions':[planes[0].shape[1],planes[0].shape[0]],
                            'nclx':list(nclx),'libheif':version,'all_plane_bits':10,'all_plane_max_abs_errors':differences,
                            'native_nclx_passthrough':True,'ffprobe_recovered_bitstream':probe,'atomic_and_ownership_checks':'PASS'})
        base=(fixtures/'hlg_limited.hevc').read_bytes()
        for name,bad in mutations(base):
            path=work/(name+'.hevc');path.write_bytes(bad)
            stdout=run([*bridge,'reject',path,work/'unused',64,32,'false'])
            assert stdout.startswith('REJECTED ')
            rejections.append(name)
        crop_rejections=[]
        for crop in ((1,0,65,33),(0,1,65,33),(1,1,65,33),(1,1,64,32)):
            stdout=run([*bridge,'reject',fixtures/'hlg_padded.hevc',work/'unused',66,34,'false',*crop])
            assert stdout.startswith('REJECTED ')
            crop_rejections.append(list(crop))
    assert pins=={str(v.relative_to(a.core)):sha(v) for v in source+encoders}
    report={'status':'PASS_INDEPENDENT_HEIF_REVIEW','source_sha256':pins,'sdk36_compile':'PASS',
            'compile_mode':'javac --release 8 with real Android SDK36 on classpath; JDK8 standard-library bootstrap',
            'libheif_sha256':sha(a.libheif),'actual_decodes':records,
            'malformed_stream_rejections':rejections,'hostile_extent_rejections':hostile_offsets,
            'odd_origin_crop_rejections':crop_rejections,
            'limits':['Fixtures use host x265, not Android MediaCodec output.','Native code equality compares libheif HEIF decode with FFmpeg HEVC decode; lossy encoding is not lossless source preservation.','No phone decoder/gallery behavior, HDR display or gainmap preservation tested.','HEVC gate is bounded metadata/reference validation, not a complete CABAC decoder.','Atomic replacement was exercised on this host filesystem; directory durability across sudden power loss is not claimed.']}
    a.report.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'status':report['status'],'decodes':len(records),'nal_rejections':len(rejections),'extent_rejections':hostile_offsets}))


if __name__=='__main__':main()
