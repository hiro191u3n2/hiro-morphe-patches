#!/usr/bin/env python3
"""Build Java mux, inspect boxes independently, decode with libheif and compare native YUV10.

Uses real x265/FFmpeg fixtures, not an Android codec. No original images/models.
"""
import argparse
import array
import ctypes as C
import ctypes.util
import hashlib
import json
import os
from pathlib import Path
import struct
import subprocess
import sys
import tempfile

import numpy as np

CHECKS = 0


def check(value, message):
    global CHECKS
    CHECKS += 1
    if not value:
        raise AssertionError(message)


def run(argv):
    p = subprocess.run([str(a) for a in argv], capture_output=True, text=True)
    if p.returncode:
        raise RuntimeError(f'{argv[0]}: {p.stderr[-4000:]}')
    return p.stdout


def boxes(data, start=0, end=None):
    end = len(data) if end is None else end
    result = {}
    while start < end:
        if start + 8 > end:
            raise ValueError('truncated box')
        size, kind = struct.unpack_from('>I4s', data, start)
        if size < 8 or start + size > end or kind in result:
            raise ValueError('invalid/repeated box')
        result[kind] = (start + 8, start + size)
        start += size
    return result


def inspect(path, width, height, full, crop):
    data = path.read_bytes()
    top = boxes(data)
    check(set(top) == {b'ftyp', b'meta', b'mdat'}, 'top boxes')
    check(data[slice(*top[b'ftyp'])] == b'heix\0\0\0\0mif1heix', 'brands')
    meta_start, meta_end = top[b'meta']
    check(data[meta_start:meta_start+4] == bytes(4), 'meta version')
    meta = boxes(data, meta_start+4, meta_end)
    check(data[slice(*meta[b'pitm'])] == bytes(4)+b'\0\1', 'primary id')
    iloc = data[slice(*meta[b'iloc'])]
    check(iloc[:8] == bytes(4)+b'\x44\0\0\1', 'iloc sizes/count')
    item, reference, count, offset, length = struct.unpack('>HHHII', iloc[8:])
    check((item, reference, count) == (1, 0, 1), 'one file-relative extent')
    check((offset, offset+length) == top[b'mdat'], 'exact mdat extent')
    iinf_start, iinf_end = meta[b'iinf']
    check(data[iinf_start:iinf_start+6] == bytes(4)+b'\0\1', 'one info item')
    infe = boxes(data, iinf_start+6, iinf_end)
    entry = data[slice(*infe[b'infe'])]
    check(entry[:12] == b'\x02\0\0\0\0\1\0\0hvc1', 'hvc1 item')
    iprp = boxes(data, *meta[b'iprp'])
    props = boxes(data, *iprp[b'ipco'])
    check(set(props) == ({b'ispe', b'pixi', b'hvcC', b'colr'} | ({b'clap'} if crop else set())), 'image properties')
    check(data[slice(*props[b'ispe'])] == bytes(4)+struct.pack('>II', width, height), 'ispe dimensions')
    check(data[slice(*props[b'pixi'])] == bytes(4)+b'\x03\x0a\x0a\x0a', 'three ten-bit components')
    check(data[slice(*props[b'colr'])] == b'nclx'+struct.pack('>HHHB', 9, 18, 9, 128 if full else 0), 'HLG color property')
    ipma = data[slice(*iprp[b'ipma'])]
    check(ipma[:10] == bytes(4)+struct.pack('>IH', 1, 1), 'ipma item')
    check(ipma[10:] == (bytes([5, 1, 2, 0x83, 4, 0x85]) if crop else bytes([4, 1, 2, 0x83, 4])), 'essential hvcC/clap associations')
    if crop:
        left, top_, cw, ch = crop
        check(data[slice(*props[b'clap'])] == struct.pack('>IIIIiIiI', cw, 1, ch, 1,
            2*left+cw-width, 2, 2*top_+ch-height, 2), 'signed rational clean aperture')
    hvcc = data[slice(*props[b'hvcC'])]
    check(len(hvcc) > 23 and hvcc[0] == 1 and hvcc[1] & 31 == 2, 'Main10 decoder config')
    check(hvcc[16] & 3 == 1 and hvcc[17] & 7 == 2 and hvcc[18] & 7 == 2, '420 actual depth10')
    check(hvcc[21] & 3 == 3 and hvcc[22] == 3, 'NAL length size and parameter arrays')
    parameters = {}; position = 23
    for kind in (32, 33, 34):
        check(hvcc[position] == 0x80 | kind, 'complete parameter array')
        number = struct.unpack_from('>H', hvcc, position+1)[0]
        size = struct.unpack_from('>H', hvcc, position+3)[0]
        position += 5
        check(number == 1 and position+size <= len(hvcc), 'bounded single parameter')
        parameters[kind] = hvcc[position:position+size]; position += size
        check((parameters[kind][0] >> 1) & 63 == kind, 'parameter NAL kind')
    check(position == len(hvcc), 'no trailing hvcC bytes')
    sample = data[offset:offset+length]
    position = 0; slices = 0
    while position < len(sample):
        check(position+4 <= len(sample), 'NAL length available')
        size = struct.unpack_from('>I', sample, position)[0]; position += 4
        check(size >= 3 and position+size <= len(sample), 'NAL payload bounded')
        kind = sample[position] >> 1 & 63
        check(kind not in (32, 33, 34), 'hvc1 has no in-band parameter sets')
        if kind in (19, 20):
            slices += 1
        position += size
    check(slices > 0, 'IDR sample present')
    return {'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest(),
            'nclx': [9, 18, 9, full], 'hvcC_depths': [10, 10], 'jpeg_items': 0,
            'gainmap_items': 0, 'clap': crop}


class Error(C.Structure):
    _fields_ = [('code', C.c_int), ('subcode', C.c_int), ('message', C.c_char_p)]


class Conversion(C.Structure):
    _fields_ = [('version', C.c_uint8), ('downsampling', C.c_int), ('upsampling', C.c_int), ('only_preferred', C.c_uint8)]


class Decoding(C.Structure):
    # Official libheif v1.23.2 heif_decoding.h, versions 1 through 10.
    _fields_ = [('version', C.c_uint8), ('ignore_transformations', C.c_uint8),
                ('start', C.c_void_p), ('progress', C.c_void_p), ('end', C.c_void_p), ('user', C.c_void_p),
                ('convert_hdr_to_8bit', C.c_uint8), ('strict_decoding', C.c_uint8), ('decoder_id', C.c_char_p),
                ('conversion', Conversion), ('cancel', C.c_void_p), ('conversion_ext', C.c_void_p),
                ('ignore_editlist', C.c_int), ('output_nclx', C.c_void_p),
                ('library_threads', C.c_int), ('codec_threads', C.c_int), ('autocorrect', C.c_uint8),
                ('passthrough', C.c_uint8)]


def decode(path):
    default = '/opt/codex/runtimes/codex-primary-runtime/dependencies/native/libheif/libheif/lib/libheif.so.1'
    location = os.environ.get('HEIF_VERIFY_LIBRARY') or (default if Path(default).exists() else ctypes.util.find_library('heif'))
    if not location:
        raise RuntimeError('independent libheif decoder required')
    lib = C.CDLL(location)
    def function(name, ret, *params):
        f = getattr(lib, name); f.restype = ret; f.argtypes = list(params); return f
    ptr = C.c_void_p; pp = C.POINTER(ptr)
    alloc = function('heif_context_alloc', ptr)
    free = function('heif_context_free', None, ptr)
    read = function('heif_context_read_from_file', Error, ptr, C.c_char_p, ptr)
    primary = function('heif_context_get_primary_image_handle', Error, ptr, pp)
    release = function('heif_image_handle_release', None, ptr)
    decoder = function('heif_decode_image', Error, ptr, pp, C.c_int, C.c_int, ptr)
    image_release = function('heif_image_release', None, ptr)
    width = function('heif_image_get_width', C.c_int, ptr, C.c_int)
    height = function('heif_image_get_height', C.c_int, ptr, C.c_int)
    depth = function('heif_image_get_bits_per_pixel_range', C.c_int, ptr, C.c_int)
    plane = function('heif_image_get_plane_readonly', C.POINTER(C.c_uint8), ptr, C.c_int, C.POINTER(C.c_int))
    version = function('heif_get_version', C.c_char_p)().decode()
    options_alloc = function('heif_decoding_options_alloc', C.POINTER(Decoding))
    options_free = function('heif_decoding_options_free', None, C.POINTER(Decoding))
    def okay(error):
        if error.code:
            raise ValueError(f'libheif {error.code}/{error.subcode}: {error.message.decode()}')
    context = alloc(); handle = ptr(); image = ptr(); options = options_alloc()
    if not context:
        raise MemoryError('libheif allocation')
    try:
        if not options or options.contents.version < 10:
            raise RuntimeError('libheif decoding options v10 required for explicit NCLX passthrough')
        options.contents.passthrough = 1
        options.contents.strict_decoding = 1
        options.contents.convert_hdr_to_8bit = 0
        okay(read(context, os.fsencode(path), None)); okay(primary(context, C.byref(handle)))
        okay(decoder(handle, C.byref(image), 99, 99, options))  # unspecified color/chroma + explicit NCLX passthrough
        arrays = []
        for channel in (0, 1, 2):
            w, h, bits = width(image, channel), height(image, channel), depth(image, channel)
            check(bits == 10 and w > 0 and h > 0, 'independent decoder exposes actual 10-bit plane')
            stride = C.c_int(); data = plane(image, channel, C.byref(stride))
            check(bool(data) and stride.value >= w*2, 'decoder plane bounds')
            raw = C.string_at(data, stride.value*h)
            arrays.append(np.ndarray((h, w), '<u2', buffer=raw, strides=(stride.value, 2)).copy())
        return arrays, version
    finally:
        if image: image_release(image)
        if handle: release(handle)
        if options: options_free(options)
        free(context)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--android-jar', type=Path, required=True)
    ap.add_argument('--jdk-bin', type=Path, required=True)
    ap.add_argument('--report', type=Path, required=True)
    args = ap.parse_args()
    root = Path(__file__).resolve().parent
    main_sources = sorted((root/'src/main/java').rglob('*.java'))
    dependency = sorted((root.parent/'android_encoder/src/main/java').rglob('*.java'))
    report = {'status': 'PASS', 'android_device_executed': False, 'gainmap_implemented': False,
              'source_sha256': {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in main_sources},
              'dependency_sha256': {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in dependency},
              'sdk_sha256': hashlib.sha256(args.android_jar.read_bytes()).hexdigest(), 'files': {}}
    with tempfile.TemporaryDirectory(prefix='ulike-main10-heif-') as directory:
        work = Path(directory); classes = work/'classes'; classes.mkdir()
        run([args.jdk_bin/'javac', '--release', '8', '-cp', args.android_jar, '-d', classes, *dependency, *main_sources])
        report['sdk36_compile'] = 'PASS'
        run([args.jdk_bin/'javac', '--release', '11', '-cp', classes, '-d', classes,
             *sorted((root/'src/test/java').rglob('*.java'))])
        decoded_originals = {}
        for name, w, h, pix, transfer, range_ in [
            ('hlg_limited', 64, 32, 'yuv420p10le', 'arib-std-b67', 'limited'),
            ('hlg_full', 64, 32, 'yuv420p10le', 'arib-std-b67', 'full'),
            ('hlg_padded', 66, 34, 'yuv420p10le', 'arib-std-b67', 'limited'),
            ('sdr8', 64, 32, 'yuv420p', 'bt709', 'limited'),
        ]:
            # Distinct non-neutral chroma patterns exercise component preservation.
            values = array.array('H', [64+i % 876 for i in range(w*h)] +
                [128+(i*7) % 760 for i in range(w*h//4)] + [160+(i*11) % 720 for i in range(w*h//4)])
            if sys.byteorder != 'little': values.byteswap()
            raw = work/(name+'.yuv'); raw.write_bytes(values.tobytes())
            encoded = work/(name+'.hevc')
            run(['ffmpeg','-v','error','-y','-f','rawvideo','-pix_fmt','yuv420p10le','-s',f'{w}x{h}',
                 '-i',raw,'-frames:v','1','-pix_fmt',pix,'-c:v','libx265','-x265-params',
                 f'pools=1:frame-threads=1:repeat-headers=1:colorprim=bt2020:transfer={transfer}:colormatrix=bt2020nc:range={range_}:log-level=error',
                 '-f','hevc',encoded])
            decoded = work/(name+'.decoded.yuv')
            run(['ffmpeg','-v','error','-y','-i',encoded,'-frames:v','1','-pix_fmt','yuv420p10le','-f','rawvideo',decoded])
            flat = np.frombuffer(decoded.read_bytes(), dtype='<u2')
            decoded_originals[name] = [flat[:w*h].reshape(h,w),flat[w*h:w*h+w*h//4].reshape(h//2,w//2),flat[w*h+w*h//4:].reshape(h//2,w//2)]
        report['java_checks'] = json.loads(run([args.jdk_bin/'java','-cp',classes,
            'com.hiro.ulike.hdr.heif.Main10HeifTest',work]))
        for name, source, w, h, full, crop in [
            ('hlg_limited','hlg_limited',64,32,False,None),
            ('hlg_full','hlg_full',64,32,True,None),
            ('hlg_padded','hlg_padded',66,34,False,None),
            ('hlg_odd_crop','hlg_padded',66,34,False,[0,0,65,33]),
            ('hlg_offset_crop','hlg_padded',66,34,False,[2,2,63,31]),
        ]:
            path=work/(name+'.heic'); info=inspect(path,w,h,full,crop)
            actual, version=decode(path)
            expected=decoded_originals[source]
            if crop:
                x,y,cw,ch=crop
                expected=[expected[0][y:y+ch,x:x+cw],
                          expected[1][y//2:(y+ch+1)//2,x//2:(x+cw+1)//2],
                          expected[2][y//2:(y+ch+1)//2,x//2:(x+cw+1)//2]]
            for component,(a,e) in enumerate(zip(actual,expected)):
                check(np.array_equal(a,e),f'{name} component {component}: HEIF {a.shape} vs original {e.shape}; max difference '+str(int(np.abs(a.astype(int)-e.astype(int)).max()) if a.shape==e.shape else 'shape mismatch'))
            info.update({'libheif':version,'native_yuv10_match':'EXACT',
                         'decoded_shapes':[list(a.shape) for a in actual],
                         'decoded_samples_compared':sum(a.size for a in actual)})
            report['files'][name]=info
        broken=work/'truncated.heic'; broken.write_bytes((work/'hlg_limited.heic').read_bytes()[:-23])
        try:
            decode(broken)
            raise AssertionError('independent decoder accepted truncated HEIF')
        except ValueError:
            check(True,'independent decoder rejects truncated item extent')
    report['independent_checks']=CHECKS
    report['limits']=['No Android device executed.', 'No gainmap/tmap is present; this is direct HLG10 HEIF.',
                      'No phone HDR display/viewer test, exposure/EXIF metadata preservation or complete HEVC conformance proof.',
                      'HEVC encoding is lossy; exact comparison proves mux preservation relative to already encoded HEVC.']
    args.report.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'status':'PASS','java_checks':report['java_checks']['checks'],'independent_checks':CHECKS,
                      'files':len(report['files']),'android_device_executed':False}))


if __name__=='__main__':
    main()
