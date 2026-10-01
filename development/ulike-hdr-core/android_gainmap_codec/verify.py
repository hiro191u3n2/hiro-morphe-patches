#!/usr/bin/env python3
"""Actual SDK compile, pure Java tests and independently encoded/decoded HEVC fixtures.
No fake Android classes or codec mocks. Host fixtures do not prove phone codec execution."""
if not __debug__:
    raise RuntimeError('run verification without -O')
import argparse,array,hashlib,json,subprocess,sys,tempfile
from pathlib import Path

def run(args):
    p=subprocess.run(list(map(str,args)),capture_output=True,text=True)
    if p.returncode: raise RuntimeError(f'{args[0]} failed: {p.stderr[-6000:]}')
    return p.stdout

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--android-jar',type=Path,required=True);ap.add_argument('--jdk-bin',type=Path,required=True);ap.add_argument('--report',type=Path,required=True)
    a=ap.parse_args();root=Path(__file__).resolve().parent
    sources=sorted((root/'src/main/java').rglob('*.java'))
    report={'status':'PASS','android_device_executed':False,'actual_phone_codec_selected':False,'sdk_jar_sha256':hashlib.sha256(a.android_jar.read_bytes()).hexdigest(),'source_sha256':{str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},'fixtures':{}}
    with tempfile.TemporaryDirectory(prefix='gainmap-codec-') as d:
        work=Path(d);classes=work/'classes';classes.mkdir()
        run([a.jdk_bin/'javac','--release','8','-cp',a.android_jar,'-d',classes,*sources]);report['sdk36_compile']='PASS'
        run([a.jdk_bin/'javac','--release','11','-cp',str(classes)+':'+str(a.android_jar),'-d',classes,*sorted((root/'src/test/java').rglob('*.java'))])
        raw=work/'input.yuv';codes=array.array('H',[i%1024 for i in range(64*32)]+[512]*(64*32//2))
        if sys.byteorder!='little':codes.byteswap()
        raw.write_bytes(codes.tobytes())
        settings=[('base','yuv420p10le','bt709','full','bt2020nc',True),('gainmap','yuv420p10le','linear','full','bt2020nc',True),('hlg','yuv420p10le','arib-std-b67','full','bt2020nc',False),('sdr8','yuv420p','bt709','full','bt2020nc',False),('limited','yuv420p10le','bt709','limited','bt2020nc',False),('smpte170m','yuv420p10le','smpte170m','full','bt2020nc',False),('bt709matrix','yuv420p10le','bt709','full','bt709',False)]
        for name,pix,transfer,range_,matrix,accepted in settings:
            out=work/(name+'.hevc')
            params=f'pools=1:frame-threads=1:repeat-headers=1:colorprim=bt2020:transfer={transfer}:colormatrix={matrix}:range={range_}:log-level=error'
            run(['ffmpeg','-v','error','-y','-f','rawvideo','-pix_fmt','yuv420p10le','-s','64x32','-i',raw,'-frames:v','1','-pix_fmt',pix,'-c:v','libx265','-x265-params',params,'-f','hevc',out])
            probe=json.loads(run(['ffprobe','-v','error','-show_entries','stream=profile,pix_fmt,width,height,color_range,color_space,color_transfer,color_primaries','-of','json',out]))['streams'][0]
            assert probe['pix_fmt']==('yuvj420p' if pix=='yuv420p' and range_=='full' else pix) and probe['color_transfer']==transfer and probe['color_range']==('pc' if range_=='full' else 'tv'), (name,probe)
            assert probe['color_space']==matrix and probe['color_primaries']=='bt2020' and probe['width']==64 and probe['height']==32
            decoded=work/(name+'.yuv');run(['ffmpeg','-v','error','-y','-i',out,'-frames:v','1','-pix_fmt',pix,'-f','rawvideo',decoded])
            assert decoded.stat().st_size==64*32*(3 if '10' in pix else 1.5)
            report['fixtures'][name]={'sha256':hashlib.sha256(out.read_bytes()).hexdigest(),'ffprobe':probe,'independent_host_decode':'PASS','accepted_in_correct_role':accepted}
        report['java_tests']=json.loads(run([a.jdk_bin/'java','-cp',str(classes)+':'+str(a.android_jar),'com.hiro.ulike.hdr.gainmapcodec.CodecBoundaryTest',work]))
    report['limits']=['Actual Android encoder/decoder availability, configure and P010 execution unverified.','No HEIF mux in this low-level module; downstream gainmap save owns the container and RGB/YUV conversion.','SPS/VUI and parameter-set validation is restricted to still single-layer/no-sublayer/no-HRD/no-extension streams; it is not entropy decode.','Codec calls configure/start/stop are synchronous platform calls without a hard interruption guarantee; dequeue/copy loops have deadlines and interruption checks.','VBR HEVC remains lossy; this module proves role and bit depth, not fidelity.','Imported capture provenance is caller supplied; content digest binds bytes to actual decoder input only.']
    a.report.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'status':report['status'],'checks':report['java_tests']['checks'],'sdk36_compile':report['sdk36_compile'],'android_device_executed':False}))
if __name__=='__main__':main()
