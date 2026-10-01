#!/usr/bin/env python3
"""Independent full-grid stream/ownership review; no Android Bitmap or SDK execution."""
import argparse, hashlib, json, math, os, struct, subprocess, tempfile
from pathlib import Path

HERE=Path(__file__).resolve().parent
CORE=HERE.parent.parent
os.environ['ORT_DISABLE_TELEMETRY']='1'

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def run(args):
    result=subprocess.run(list(map(str,args)),capture_output=True,text=True)
    if result.returncode:raise RuntimeError(result.stdout+'\n'+result.stderr)
    return result.stdout.strip()

def oracle(actual):
    width,height=4080,3060
    source=hashlib.sha256(b'ULHF\1'+struct.pack('>ii',width,height))
    def text(value):
        if value is None:return struct.pack('>i',-1)
        encoded=value.encode('utf8');return struct.pack('>i',len(encoded))+encoded
    source.update(text('BT2020_NCL_HLG_FULL')+struct.pack('>qqq',987654321,73,19))
    source.update(text('0')+text(None)+text('5')+struct.pack('>?q?i',True,10000000,True,206))
    source.update(struct.pack('>ii',0,width*height))
    for y in range(height):
        source.update(struct.pack('>'+str(width)+'H',*((29*x+53*y)%1024 for x in range(width))))
    for component in (1,2):
        source.update(struct.pack('>ii',component,width*height//4))
        row=struct.pack('>'+str(width//2)+'H',*([512]*(width//2)))
        for _ in range(height//2):source.update(row)
    a=.17883277;b=1-4*a;c=.5-a*math.log(4*a)
    gray=[]
    for level in range(1024):
        value=level/1023
        linear=value*value/3 if value<=.5 else (math.exp((value-c)/a)+b)/12
        tone=linear/(1+linear)
        q=int(math.floor(255*(12.92*tone if tone<=.0031308 else 1.055*tone**(1/2.4)-.055)+.5))
        gray.append(0xff000000|q<<16|q<<8|q)
    proxy=hashlib.sha256(b'ULSP\1'+struct.pack('>ii',width,height))
    for y in range(height):proxy.update(struct.pack('>'+str(width)+'I',*(gray[(29*x+53*y)%1024] for x in range(width))))
    assert actual['source']==source.hexdigest(),'independent Python full-source digest mismatch'
    assert actual['proxy']==proxy.hexdigest(),'independent Python full-raster digest mismatch'
    assert actual['settings']==hashlib.sha256(bytes([7,4,1])).hexdigest()
    return {'argb_pixels':width*height,'p010_samples':3*width*height//2,
            'canonical_source_sha256':source.hexdigest(),'canonical_proxy_sha256':proxy.hexdigest(),
            'max_python_row_payload_bytes':4*width}

def main():
    p=argparse.ArgumentParser()
    for name in ('jdk-bin','android-jar','ort-classes'):p.add_argument('--'+name,type=Path,required=True)
    p.add_argument('--report',type=Path,default=HERE/'INDEPENDENT_REVIEW.json');a=p.parse_args()
    production=[]
    for root in ('hdr_input/src/main/java','android_model_runtime/src','android_beauty_image/src','android_hdr_color/src/main/java',
                 'face_analysis/src/main/java','android_face_backend/src/main/java','android_still_analysis/src/main/java','android_analysis_input/src/main/java'):
        production+=sorted((CORE/root).rglob('*.java'))
    files=production+[HERE/'FullresReview.java',Path(__file__).resolve()]
    before={str(f.relative_to(CORE)):sha(f) for f in files}
    with tempfile.TemporaryDirectory(prefix='ulike-fullres-independent-') as temp:
        work=Path(temp);classes=work/'classes';classes.mkdir()
        run([a.jdk_bin/'javac','--release','8','-cp',os.pathsep.join(map(str,(a.android_jar,a.ort_classes))),
             '-d',classes,*production,HERE/'FullresReview.java'])
        classpath=os.pathsep.join(map(str,(classes,a.ort_classes)))
        lifecycle=json.loads(run([a.jdk_bin/'java','-Xmx96m','-cp',classpath,'com.hiro.ulike.hdr.input.FullresReview','lifecycle']))
        run([a.jdk_bin/'java','-Xmx64m','-cp',classpath,'com.hiro.ulike.hdr.input.FullresReview','full',work/'full.json'])
        full=json.loads((work/'full.json').read_text());independent=oracle(full)
    assert before=={str(f.relative_to(CORE)):sha(f) for f in files},'source changed during independent review'
    report={'status':'PASS_INDEPENDENT_FULL_GRID_STREAM_AND_EXCLUSIVE_OWNERSHIP',
            'lifecycle':lifecycle,'native_grid_host':full,'independent_python_oracle':independent,
            'reviewed_file_sha256':before,'tool_sha256':{str(f.name):sha(f) for f in (a.jdk_bin/'javac',a.android_jar,a.ort_classes)},
            'actual_android_bitmap_execution':False,'actual_native_sdk_execution':False,
            'phone_memory_or_latency_verified':False,'production_geometry_verified':False,
            'manual_source_review':[
                'Streaming owns immutable HDR rendition and one borrowed row; no full ARGB raster is retained.',
                'Native-size Android bridge transfers its sole created Bitmap into OwnedBitmapSubmission and clears its outer recycle reference.',
                'StockStillAnalysis.runOwned digests the same Bitmap using one row and calls StockStillFaceProbe.runOwned without Bitmap.copy.',
                'Probe preflights before transfer and releases its transferred Bitmap only after every attempted native teardown returns successfully; otherwise a process-static quarantine retains resource, recorder, callbacks and lease.',
                'Early or duplicate image callbacks are rejected before owned pixel cloning; full diagnostic grid dimensions and pixel count must match exactly.',
                'Full-size callback ownership requires one SDK callback int[P] plus one owned diagnostic int[P]. Consuming conversion wipes owned data without a second diagnostic clone.',
                'Native photographic orientation comparison remains disabled for this candidate; it is not silently inferred or treated as calibrated.',
                'Legacy Bitmap entry points and orientation diagnostic keep the 4MP policy. Candidate allows at most 12,484,800 pixels and dimensions at most 4080.',
                'The 15P+row memory estimate excludes app baseline, native/GPU, model storage and allocator overhead; 64MiB stream host test does not establish phone capture memory sufficiency.'
            ],'blocking_findings':[],
            'limitations':['Native stop/unregister/uninit join semantics were not executed on a phone.','Stale-callback correlation still depends on actual exclusive SDK lifetime and native same-frame calibration.','No real Android Bitmap allocation, rendering, P010 capture, or HDR save was run by this review.']}
    a.report.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'status':report['status'],'host_checks':lifecycle['checks']+full['checks'],'pixels':independent['argb_pixels'],'report':str(a.report)}))

if __name__=='__main__':main()
