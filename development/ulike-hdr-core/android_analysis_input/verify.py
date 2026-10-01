#!/usr/bin/env python3
"""Same-capture raster ownership and independently computed source/pixel digests; no device run."""
import argparse, hashlib, json, math, os, re, struct, subprocess, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
CORE=ROOT.parent
os.environ['ORT_DISABLE_TELEMETRY']='1'
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def run(cmd):
    p=subprocess.run(list(map(str,cmd)),capture_output=True,text=True)
    if p.returncode: raise RuntimeError(p.stdout+'\n'+p.stderr)
    return p.stdout.strip()
def independent(work):
    w,h=64,48
    planes=[[((x*3+y*7)%1024) for y in range(h) for x in range(w)],
            [440+(x*5+y*3)%144 for y in range(h//2) for x in range(w//2)],
            [464+(x*3+y*7)%96 for y in range(h//2) for x in range(w//2)]]
    data=bytearray(b'ULHF\1')
    data+=struct.pack('>ii',w,h)
    def text(s):
        if s is None:return struct.pack('>i',-1)
        b=s.encode();return struct.pack('>i',len(b))+b
    data+=text('BT2020_NCL_HLG_LIMITED')+struct.pack('>qqq',100000,123,9)
    data+=text('0')+text(None)+text('5')+struct.pack('>?q?i',True,10000000,True,206)
    for component,plane in enumerate(planes):
        data+=struct.pack('>ii',component,len(plane))+struct.pack('>'+'H'*len(plane),*plane)
    a=.17883277;b=1-4*a;c=.5-a*math.log(4*a)
    def inverse(v):
        s=abs(v);out=s*s/3 if s<=.5 else (math.exp((s-c)/a)+b)/12
        return math.copysign(out,v)
    def chroma(plane,x,y):
        cx=min(w//2-1,x/2);cy=min(h//2-1,y/2);x0=int(cx);y0=int(cy);x1=min(w//2-1,x0+1);y1=min(h//2-1,y0+1);fx=cx-x0;fy=cy-y0
        top=plane[y0*w//2+x0]*(1-fx)+plane[y0*w//2+x1]*fx
        bottom=plane[y1*w//2+x0]*(1-fx)+plane[y1*w//2+x1]*fx
        return top*(1-fy)+bottom*fy
    def encode(v):
        t=max(0,min(1,v));return 12.92*t if t<=.0031308 else 1.055*t**(1/2.4)-.055
    expected=[]
    for y in range(h):
        for x in range(w):
            yp=(planes[0][y*w+x]-64)/876;u=(chroma(planes[1],x,y)-512)/896;v=(chroma(planes[2],x,y)-512)/896
            rp=yp+1.4746*v;bp=yp+1.8814*u;gp=(yp-.2627*rp-.0593*bp)/.678
            r,g,bl=map(inverse,(rp,gp,bp));scale=3/(1+max(0,.2627*r+.678*g+.0593*bl)*3);r*=scale;g*=scale;bl*=scale
            rgb=(encode(1.6604910021084345*r-.5876411387885495*g-.07284986331988488*bl),
                 encode(-.12455047452159074*r+1.1328998971259603*g-.008349422604369477*bl),
                 encode(-.018150763354905303*r-.10057889800800739*g+1.1187296613629127*bl))
            rr,gg,bb=[int(math.floor(z*255+.5)) for z in rgb];expected.append(0xff000000|(rr<<16)|(gg<<8)|bb)
    raw=struct.pack('>'+'I'*len(expected),*expected)
    actual=(work/'proxy.argb').read_bytes();assert actual==raw,'independent RGB8 raster mismatch'
    result=json.loads((work/'fixture.json').read_text())
    assert result['sourceSha256']==hashlib.sha256(data).hexdigest(),'canonical HDR digest mismatch'
    assert result['proxySha256']==hashlib.sha256(b'ULSP\1'+struct.pack('>ii',w,h)+raw).hexdigest(),'native bridge digest mismatch'
    return {'independent_argb_pixels':len(expected),'source_sha256':result['sourceSha256'],'proxy_sha256':result['proxySha256']}
def independent_stream(work):
    result=json.loads((work/'stream-native.json').read_text());w,h=4080,3060;count=w*h
    def txt(value):
        if value is None:return struct.pack('>i',-1)
        raw=value.encode();return struct.pack('>i',len(raw))+raw
    src=hashlib.sha256(b'ULHF\1'+struct.pack('>ii',w,h)+txt('BT2020_NCL_HLG_LIMITED')+
        struct.pack('>qqq',777,18,17)+txt('0')+txt(None)+txt('5')+struct.pack('>?q?i',True,10000000,True,206))
    chunk=struct.pack('>H',512)*4096
    for i,n in enumerate((count,count//4,count//4)):
        src.update(struct.pack('>ii',i,n))
        for _ in range(n//4096):src.update(chunk)
        src.update(chunk[:2*(n%4096)])
    a=.17883277;b=1-4*a;c=.5-a*math.log(4*a);signal=(512-64)/876
    light=signal*signal/3 if signal<=.5 else (math.exp((signal-c)/a)+b)/12
    mapped=3*light/(1+3*light);encoded=12.92*mapped if mapped<=.0031308 else 1.055*mapped**(1/2.4)-.055
    code=math.floor(encoded*255+.5);argb=0xff000000|code<<16|code<<8|code
    proxy=hashlib.sha256(b'ULSP\1'+struct.pack('>ii',w,h));row=struct.pack('>I',argb)*w
    for _ in range(h):proxy.update(row)
    assert result['argb']==argb and result['width']==w and result['height']==h
    assert result['sourceSha256']==src.hexdigest() and result['proxySha256']==proxy.hexdigest()
    assert result['host_max_heap']<=64*1024*1024
    return {'full_grid_pixels':count,'full_grid_source_sha256':src.hexdigest(),'full_grid_proxy_sha256':proxy.hexdigest(),
            'stream_host_heap_limit_bytes':result['host_max_heap'],'independent_full_grid_digest':True}
def main():
    p=argparse.ArgumentParser()
    for name in ('jdk','android-jar','ort-classes','r8','report'):p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args();sources=[]
    for folder in ('hdr_input/src/main/java','android_model_runtime/src','android_beauty_image/src','android_hdr_color/src/main/java','face_analysis/src/main/java','android_face_backend/src/main/java','android_still_analysis/src/main/java','android_analysis_input/src/main/java'):
        sources+=sorted((CORE/folder).rglob('*.java'))
    pins={str(f.relative_to(CORE)):sha(f) for f in sources}
    with tempfile.TemporaryDirectory(prefix='ulike-analysis-input-') as work:
        work=Path(work);classes=work/'classes';classes.mkdir()
        run([a.jdk/'javac','--release','8','-cp',os.pathsep.join(map(str,(a.android_jar,a.ort_classes))),'-d',classes,*sources])
        jar=work/'input.jar';run([a.jdk/'jar','cf',jar,'-C',classes,'.']);dex=work/'dex';dex.mkdir()
        run([a.jdk/'java','-cp',a.r8,'com.android.tools.r8.D8','--min-api','26','--lib',a.android_jar,'--classpath',a.ort_classes,'--output',dex,jar])
        tests=sorted((ROOT/'src/test').rglob('*.java'))
        run([a.jdk/'javac','--release','8','-cp',os.pathsep.join(map(str,(classes,a.ort_classes))),'-d',classes,*tests])
        output=run([a.jdk/'java','-Xmx128m','-cp',os.pathsep.join(map(str,(classes,a.ort_classes))),'com.hiro.ulike.hdr.input.AnalysisInputTest',work]);print(output)
        oracle=independent(work)
        stream_output=run([a.jdk/'java','-Xmx64m','-cp',os.pathsep.join(map(str,(classes,a.ort_classes))),'com.hiro.ulike.hdr.input.StreamingInputTest',work]);print(stream_output)
        stream_oracle=independent_stream(work)
        ownership_output=run([a.jdk/'java','-Xmx80m','-cp',os.pathsep.join(map(str,(classes,a.ort_classes))),'com.hiro.ulike.hdr.input.SubmissionOwnershipTest']);print(ownership_output)
    assert pins=={str(f.relative_to(CORE)):sha(f) for f in sources},'source changed during verification'
    report={'status':'PASS_HOST_OWNED_SAME_CAPTURE_INPUT_AND_SDK36_D8','host_checks':int(re.search(r'PASS (\d+)',output).group(1)),**oracle,
            'source_sha256':pins,'test_sha256':{str(f.relative_to(ROOT)):sha(f) for f in tests},'verifier_sha256':sha(Path(__file__)),
            'actual_android_bitmap_or_native_execution':False,'native_geometry_calibrated':False,'legacy_maximum_analysis_pixels':4194304,
            'candidate_maximum_analysis_pixels':12484800,'candidate_known_payload_bytes':187288320,'native_4080x3060_streamed_without_resize':True,
            'stream_host_checks':int(re.search(r'PASS (\d+)',stream_output).group(1)),
            'ownership_host_checks':int(re.search(r'PASS (\d+)',ownership_output).group(1)),**stream_oracle,'no_hdr_photo_quantization':True,
            'tool_sha256':{str(f.name):sha(f) for f in (a.android_jar,a.ort_classes,a.r8,a.jdk/'javac')}}
    a.report.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'status':report['status'],'host_checks':report['host_checks'],**oracle}))
if __name__=='__main__':main()
