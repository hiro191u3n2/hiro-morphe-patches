#!/usr/bin/env python3
"""SDK compile + pure Java boundary checks + independent FFmpeg generated fixtures.

No Android device, mock MediaCodec implementation, original APK, or model is used.
"""
import argparse
import array
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile


def run(argv):
    result = subprocess.run([str(v) for v in argv], capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(f'{argv[0]} failed ({result.returncode}): {result.stderr[-4000:]}')
    return result.stdout


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--android-jar', type=Path, required=True)
    ap.add_argument('--jdk-bin', type=Path, required=True)
    ap.add_argument('--report', type=Path, required=True)
    args = ap.parse_args()
    root = Path(__file__).resolve().parent
    sources = sorted((root / 'src/main/java').rglob('*.java'))
    report = {'status': 'PASS', 'android_device_executed': False,
              'heif_mux_or_gainmap_implemented': False,
              'sdk_jar_sha256': hashlib.sha256(args.android_jar.read_bytes()).hexdigest(),
              'source_sha256': {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest()
                                for p in sources},
              'fixtures': {}}
    with tempfile.TemporaryDirectory(prefix='ulike-encoder-') as directory:
        work = Path(directory)
        classes = work / 'classes'
        classes.mkdir()
        run([args.jdk_bin / 'javac', '--release', '8', '-cp', args.android_jar,
             '-d', classes, *sources])
        report['android_sdk_compile'] = 'PASS'
        run([args.jdk_bin / 'javac', '--release', '11', '-cp', classes, '-d', classes,
             *sorted((root / 'src/test/java').rglob('*.java'))])
        codes = array.array('H', [i % 1024 for i in range(64 * 32)] + [512] * (64 * 32 // 2))
        if sys.byteorder != 'little':
            codes.byteswap()
        raw = work / 'input.yuv'
        raw.write_bytes(codes.tobytes())
        for name, pix, transfer, range_, expected in [
            ('hlg_limited', 'yuv420p10le', 'arib-std-b67', 'limited', True),
            ('hlg_full', 'yuv420p10le', 'arib-std-b67', 'full', True),
            ('sdr8', 'yuv420p', 'bt709', 'limited', False),
            ('pq10', 'yuv420p10le', 'smpte2084', 'limited', False),
        ]:
            output = work / (name + '.hevc')
            settings = (f'pools=1:frame-threads=1:repeat-headers=1:colorprim=bt2020:'
                        f'transfer={transfer}:colormatrix=bt2020nc:range={range_}:log-level=error')
            run(['ffmpeg', '-v', 'error', '-y', '-f', 'rawvideo', '-pix_fmt', 'yuv420p10le',
                 '-s', '64x32', '-i', raw, '-frames:v', '1', '-pix_fmt', pix,
                 '-c:v', 'libx265', '-x265-params', settings, '-f', 'hevc', output])
            probe = json.loads(run(['ffprobe', '-v', 'error', '-show_entries',
                                   'stream=profile,pix_fmt,width,height,color_range,color_space,color_transfer,color_primaries',
                                   '-of', 'json', output]))['streams'][0]
            assert probe['pix_fmt'] == pix
            assert probe['color_transfer'] == transfer
            assert probe['color_range'] == ('pc' if range_ == 'full' else 'tv')
            assert probe['width'] == 64 and probe['height'] == 32
            assert probe['color_space'] == 'bt2020nc' and probe['color_primaries'] == 'bt2020'
            assert probe['profile'] == ('Main 10' if pix.endswith('10le') else 'Main')
            # An independent decode proves the generated host test fixture is decodable.
            # It does not validate an unconnected Android codec.
            decoded = work / (name + '.yuv')
            run(['ffmpeg', '-v', 'error', '-y', '-i', output, '-frames:v', '1',
                 '-pix_fmt', pix, '-f', 'rawvideo', decoded])
            assert decoded.stat().st_size == (64 * 32 * 3 if pix.endswith('10le') else 64 * 32 * 3 // 2)
            report['fixtures'][name] = {'sha256': hashlib.sha256(output.read_bytes()).hexdigest(),
                                        'ffprobe': probe, 'independent_host_decode': 'PASS',
                                        'expected_hlg_accept': expected}
        report['java_checks'] = json.loads(run([args.jdk_bin / 'java', '-cp', classes,
            'com.hiro.ulike.hdr.encoder.EncoderBoundaryTest', work]))
    report['limits'] = [
        'No Android MediaCodec enumeration, configure, input, output or hardware execution was performed.',
        'SPS/VUI contract parsing is not a complete HEVC decoder or a precision/fidelity proof.',
        'No HEIF mux, gainmap, processed RGB-to-HLG conversion, app hook or UI preference was added.',
        'HEVC VBR is lossy. Hardware designation is a manufacturer report.',
    ]
    args.report.write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps({'status': report['status'], 'checks': report['java_checks']['checks'],
                      'sdk_compile': report['android_sdk_compile'], 'android_device_executed': False}))


if __name__ == '__main__':
    main()
