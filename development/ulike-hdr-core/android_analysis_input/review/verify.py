#!/usr/bin/env python3
"""Independent source/pixel digest, raster and lifecycle review; no native/device run."""
import argparse,hashlib,json,math,os,struct,subprocess,tempfile
from pathlib import Path
HERE=Path(__file__).resolve().parent
CORE=HERE.parent.parent
os.environ['ORT_DISABLE_TELEMETRY']='1'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(args):
    p=subprocess.run(list(map(str,args)),capture_output=True,text=True)
    if p.returncode:raise RuntimeError(p.stdout+'\n'+p.stderr)
    return p.stdout.strip()
def reference(work):
    count=0;cases=[]
    for fixture in range(8):
        w,h=16,8
        planes=[[(i*97+fixture*23+c*331)%1024 for i in range(w*h if c==0 else w*h//4)] for c in range(3)]
        canonical=bytearray(b'ULHF\x01'+struct.pack('>ii',w,h))
        def text(s):
            if s is None:return struct.pack('>i',-1)
            data=s.encode('utf8');return struct.pack('>i',len(data))+data
        encoding='BT2020_NCL_HLG_FULL' if fixture%2==0 else 'BT2020_NCL_HLG_LIMITED'
        canonical+=text(encoding)+struct.pack('>qqq',100001+fixture,100+fixture,fixture+1)
        canonical+=text('cameraπ')+text(None if fixture%2==0 else 'physical5')+text(None if fixture%3==0 else 'active7')
        canonical+=b'\0' if fixture%2==0 else b'\1'+struct.pack('>q',1234567)
        canonical+=b'\0' if fixture%3==0 else b'\1'+struct.pack('>i',217)
        for c,plane in enumerate(planes):canonical+=struct.pack('>ii',c,len(plane))+struct.pack('>'+'H'*len(plane),*plane)
        a=.17883277;b=1-4*a;c=.5-a*math.log(4*a)
        def hlg(v):
            s=abs(v);return math.copysign(s*s/3 if s<=.5 else (math.exp((s-c)/a)+b)/12,v)
        def sample(plane,x,y):
            u=min(w/2-1,max(0,(x-(.5 if fixture%2 else 0))*.5))
            v=min(h/2-1,max(0,(y-(.5 if fixture%3==0 else 0))*.5))
            ix,iy=int(u),int(v);fx,fy=u-ix,v-iy
            out=0
            for dx in (0,1):
                for dy in (0,1):out+=plane[min(iy+dy,h//2-1)*(w//2)+min(ix+dx,w//2-1)]*(fx if dx else 1-fx)*(fy if dy else 1-fy)
            return out
        def srgb(v):
            v=max(0,min(1,v));return v*12.92 if v<=.0031308 else 1.055*v**(1/2.4)-.055
        expected=[]
        for y in range(h):
            for x in range(w):
                luma=planes[0][y*w+x];u=sample(planes[1],x,y)-512;v=sample(planes[2],x,y)-512
                if fixture%2:luma=(luma-64)/876;u/=896;v/=896
                else:luma/=1023;u/=1023;v/=1023
                encoded_r=luma+1.4746*v;encoded_b=luma+1.8814*u
                encoded_g=(luma-.2627*encoded_r-.0593*encoded_b)/.678
                r,g,blue=hlg(encoded_r),hlg(encoded_g),hlg(encoded_b)
                exposure=2**(fixture-3);factor=exposure/(1+max(0,.2627*r+.678*g+.0593*blue)*exposure)
                r*=factor;g*=factor;blue*=factor
                rgb=[srgb(1.6604910021084345*r-.5876411387885495*g-.07284986331988488*blue),
                     srgb(-.12455047452159074*r+1.1328998971259603*g-.008349422604369477*blue),
                     srgb(-.018150763354905303*r-.10057889800800739*g+1.1187296613629127*blue)]
                values=[int(math.floor(v*255+.5)) for v in rgb];expected.append(0xff000000+(values[0]<<16)+(values[1]<<8)+values[2])
        raw=struct.pack('>'+'I'*len(expected),*expected);actual=(work/f'fixture{fixture}.argb').read_bytes()
        assert raw==actual,('independent RGB raster mismatch',fixture)
        report=json.loads((work/f'fixture{fixture}.json').read_text())
        assert report['source']==hashlib.sha256(canonical).hexdigest(),('canonical HDR digest mismatch',fixture)
        assert report['proxy']==hashlib.sha256(b'ULSP\1'+struct.pack('>ii',w,h)+raw).hexdigest(),('canonical proxy digest mismatch',fixture)
        assert report['settings']==hashlib.sha256(bytes([4,9,3])).hexdigest(),'application setting digest mismatch'
        count+=len(expected);cases.append(report)
    return {'independent_pixels':count,'canonical_hdr_and_proxy_cases':len(cases),'cases':cases}
def main():
    p=argparse.ArgumentParser()
    for n in ('jdk-bin','android-jar','ort-classes'):p.add_argument('--'+n,type=Path,required=True)
    p.add_argument('--report',type=Path,default=HERE.parent/'INDEPENDENT_REVIEW.json');a=p.parse_args()
    production=[]
    for root in ('hdr_input/src/main/java','android_model_runtime/src','android_beauty_image/src','android_hdr_color/src/main/java','face_analysis/src/main/java','android_face_backend/src/main/java','android_still_analysis/src/main/java','android_analysis_input/src/main/java'):
        production+=sorted((CORE/root).rglob('*.java'))
    inventory=production+[HERE/'InputReview.java',Path(__file__).resolve()]
    before={str(f.relative_to(CORE)):sha(f) for f in inventory}
    with tempfile.TemporaryDirectory(prefix='ulike-independent-analysis-input-') as work:
        work=Path(work);classes=work/'classes';classes.mkdir()
        run([a.jdk_bin/'javac','--release','8','-cp',os.pathsep.join(map(str,(a.android_jar,a.ort_classes))),'-d',classes,*production,HERE/'InputReview.java'])
        lifecycle=json.loads(run([a.jdk_bin/'java','-Xmx128m','-cp',os.pathsep.join(map(str,(classes,a.ort_classes))),'com.hiro.ulike.hdr.input.InputReview',work]))
        oracle=reference(work)
    assert before=={str(f.relative_to(CORE)):sha(f) for f in inventory},'source changed during review'
    report={'status':'PASS_INDEPENDENT_SAME_CAPTURE_ANALYSIS_INPUT','lifecycle':lifecycle,'oracle':oracle,
      'reviewed_file_sha256':before,'manual_android_bridge_review':[
        'Actual prepared opaque RGB pixels transferred to same-size sRGB Bitmap; no resize or JPEG stage.',
        'Compares SHA-256 of owned submitted proxy from StockStillAnalysis to digest of prepared pixels, plus exact Request object, nonce, dimensions and complete callback evidence.',
        'Requires optional diagnostic callback to match nonce and full submitted grid when requested.',
        'Bitmap recycled in finally; this review does not claim real Android Bitmap memory erasure or native callback execution.',
        'Source hash is canonical content and capture metadata, not native geometry calibration. BoundOutcome explicitly keeps nativeGeometryCalibrated=false.',
        '4194304-pixel native analysis ceiling is enforced without resizing; a 4080x3060 input cannot yet traverse this analysis backend.',
        'RecorderAdmission/NativeLifetimeBoundary were inspected after dependency changes: tickets reject concurrent native operations; uncertain completion quarantines. NativeLifetimeHooks remains compile-time disabled and supplies no actual installed whole-app coverage. Input ownership and source-binding contracts are unchanged.'
      ],'blocking_findings':[],'android_bitmap_or_native_execution':False,'device_geometry_calibrated':False,'complete_app_integration':False,
      'sdk36_production_and_review_compile':True,'host_heap_limit_bytes':134217728}
    a.report.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'status':report['status'],'lifecycle':lifecycle,'independent_pixels':oracle['independent_pixels']}))
if __name__=='__main__':main()
