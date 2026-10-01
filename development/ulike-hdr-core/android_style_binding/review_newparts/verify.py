#!/usr/bin/env python3
"""Independent PNG/texture/raster/owned-archive review; caller-owned assets stay local."""
import argparse, hashlib, io, json, os, struct, subprocess, tempfile, zipfile, zlib
from pathlib import Path
import numpy as np
from PIL import Image

HERE=Path(__file__).resolve().parent
MODULE=HERE.parent
CORE=MODULE.parent
WORK=CORE.parent.parent
CHECKS=0
def check(value,message):
    global CHECKS
    CHECKS+=1
    if not value: raise AssertionError(message)
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def run(command):
    result=subprocess.run(list(map(str,command)),capture_output=True,text=True)
    if result.returncode: raise RuntimeError(result.stdout+'\n'+result.stderr)
    return result.stdout.strip()
def chunk(name,data):return struct.pack('>I',len(data))+name+data+struct.pack('>I',zlib.crc32(name+data)&0xffffffff)
def png(width,height,color,raw,palette=None,alpha=None,filter_type=0):
    channels={2:3,3:1,6:4}[color];scan=bytearray();prev=bytes(width*channels)
    for y in range(height):
        row=raw[y*width*channels:(y+1)*width*channels];scan.append(filter_type)
        for i,value in enumerate(row):
            a=row[i-channels] if i>=channels else 0;b=prev[i];c=prev[i-channels] if i>=channels else 0
            p=a+b-c;choices=[abs(p-a),abs(p-b),abs(p-c)]
            predictor=[0,a,b,(a+b)//2,(a,b,c)[choices.index(min(choices))]][min(filter_type,4)]
            scan.append((value-predictor)&255)
        prev=row
    pieces=[b'\x89PNG\r\n\x1a\n',chunk(b'IHDR',struct.pack('>IIBBBBB',width,height,8,color,0,0,0))]
    if palette is not None:pieces.append(chunk(b'PLTE',palette))
    if alpha is not None:pieces.append(chunk(b'tRNS',alpha))
    pieces.extend([chunk(b'IDAT',zlib.compress(scan,0)),chunk(b'IEND',b'')]);return b''.join(pieces)
def fixtures(root,archive):
    cases=[]
    def add(name,data,expected=None):
        (root/name).write_bytes(data);cases.append(name+'\t'+('reject' if expected is None else 'pixels'))
        if expected is not None:(root/(name+'.rgba')).write_bytes(expected)
    w,h=7,5
    rgb=bytes((i*73+11)%256 for i in range(w*h*3));rgba=bytes((i*89+3)%256 for i in range(w*h*4))
    rgb_expected=bytes(v for i in range(w*h) for v in (*rgb[i*3:i*3+3],255))
    for f in range(5):
        add('rgb-filter'+str(f)+'.png',png(w,h,2,rgb,filter_type=f),rgb_expected)
        add('rgba-filter'+str(f)+'.png',png(w,h,6,rgba,filter_type=f),rgba)
    palette=bytes([255,0,0,0,255,0,0,0,255]);alpha=bytes([0,128]);indices=bytes(i%3 for i in range(w*h))
    expected=bytes(v for i in indices for v in (*palette[i*3:i*3+3],alpha[i] if i<len(alpha) else 255))
    add('palette.png',png(w,h,3,indices,palette,alpha),expected)
    edge=bytes([255,0,0,0,0,0,255,255]);add('transparent-edge.png',png(2,1,6,edge),edge)
    add('mutation-a.png',png(2,1,6,bytes([11,22,33,255,44,55,66,255])),bytes([11,22,33,255,44,55,66,255]))
    add('mutation-b.png',png(2,1,6,bytes([111,122,133,255,144,155,166,255])),bytes([111,122,133,255,144,155,166,255]))
    valid=png(w,h,6,rgba)
    bad=bytearray(valid);bad[30]^=1;add('bad-crc.png',bytes(bad))
    add('trailing.png',valid+b'X');add('truncated.png',valid[:-5]);add('invalid-filter.png',png(w,h,6,rgba,filter_type=5))
    add('invalid-index.png',png(1,1,3,b'\x03',palette));add('missing-palette.png',png(1,1,3,b'\x00'))
    add('oversize.png',b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',2049,1,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b'\0'+bytes(4)))+chunk(b'IEND',b''))
    (root/'cases.tsv').write_text('\n'.join(cases)+'\n')
    with zipfile.ZipFile(archive) as original,zipfile.ZipFile(root/'altered.zip','w') as altered:
        for entry in original.infolist():
            data=original.read(entry)
            if entry.filename=='materials/016/AmazingFeature1/image/blusher/blusher000.png':data=b'controlled changed texture after archive hash'
            altered.writestr(entry,data)
    return len(cases)
def reference(path,assets):
    data=memoryview(path.read_bytes());offset=0
    def integer():
        nonlocal offset
        value=struct.unpack_from('>i',data,offset)[0];offset+=4;return value
    def array(kind):
        nonlocal offset
        n=integer();values=np.frombuffer(data,dtype=kind,count=n,offset=offset).astype(np.float64 if kind=='>f8' else np.int32);offset+=n*np.dtype(kind).itemsize;return values
    w,h=integer(),integer();vertices=array('>f8').reshape(-1,3);matrix=array('>f8').reshape(4,4);uv=array('>f8').reshape(-1,2);indices=array('>i4').reshape(-1,3)
    actual_coverage=array('>f8').reshape(h,w);actual_rgba=array('>f8').reshape(h,w,4);actual_base=array('>f8').reshape(h,w,3);check(offset==len(data),'complete reviewer binary read')
    clip=np.column_stack([vertices,np.ones(len(vertices))])@matrix.T;ndc=clip[:,:3]/clip[:,3,None];screen=np.column_stack([(ndc[:,0]+1)*w/2,(1-ndc[:,1])*h/2]);bg=(ndc[:,:2]+1)/2
    rgba=np.asarray(Image.open(assets/'purity/3dmakeup4/image/makeup3d_open.png').convert('RGBA'),dtype=float)/255;rgba[:,:,:3]*=rgba[:,:,3,None]
    def sample(coords):
        p=np.maximum(0,np.minimum([rgba.shape[1]-1,rgba.shape[0]-1],coords*np.array([rgba.shape[1],rgba.shape[0]])-.5));lo=np.floor(p).astype(int);hi=np.minimum(lo+1,[rgba.shape[1]-1,rgba.shape[0]-1]);weight=p-lo
        return ((rgba[lo[:,1],lo[:,0]]*(1-weight[:,0,None])+rgba[lo[:,1],hi[:,0]]*weight[:,0,None])*(1-weight[:,1,None])+
                (rgba[hi[:,1],lo[:,0]]*(1-weight[:,0,None])+rgba[hi[:,1],hi[:,0]]*weight[:,0,None])*weight[:,1,None])
    expected_coverage=np.zeros((h,w));expected_rgba=np.zeros((h,w,4));expected_base=np.zeros((h,w,3));covered_primitives=0
    for tri in indices:
        points=screen[tri];transform=np.column_stack([points[1]-points[0],points[2]-points[0]])
        if abs(np.linalg.det(transform))<1e-15:continue
        minimum=np.maximum([0,0],np.ceil(points.min(axis=0)-.5)).astype(int);maximum=np.minimum([w-1,h-1],np.floor(points.max(axis=0)-.5)).astype(int)
        if np.any(maximum<minimum):continue
        ys,xs=np.mgrid[minimum[1]:maximum[1]+1,minimum[0]:maximum[0]+1];x=xs.ravel();y=ys.ravel();positions=np.column_stack([x+.5,y+.5])
        solved=np.linalg.solve(transform,(positions-points[0]).T).T;weights=np.column_stack([1-solved.sum(axis=1),solved]);inside=np.all(weights>=0,axis=1)
        x=x[inside];y=y[inside];weights=weights[inside]
        if not len(x):continue
        perspective=weights/clip[tri,3];perspective/=perspective.sum(axis=1)[:,None]
        coords=perspective@uv[tri];coords[:,1]+=.085;base_uv=perspective@bg[tri]
        expected_coverage[y,x]=1;expected_rgba[y,x]=sample(coords);expected_base[y,x]=np.column_stack([base_uv,.25*base_uv[:,0]+.5*base_uv[:,1]]);covered_primitives+=1
    check(np.array_equal(actual_coverage,expected_coverage),'independent linear-solve raster coverage')
    rgba_error=float(np.abs(actual_rgba-expected_rgba).max());base_error=float(np.abs(actual_base-expected_base).max())
    check(rgba_error<2e-12,'independent perspective texture RGBA');check(base_error<2e-12,'independent perspective shader base')
    yy,xx=np.mgrid[:h,:w];non_screen=float(np.max(np.abs(actual_base[:,:,0][actual_coverage>0]-(xx[actual_coverage>0]+.5)/w)))
    check(non_screen>.001,'fixture exercises perspective-interpolated uv1, not screen UV shortcut')
    check(np.count_nonzero(actual_coverage)>2000,'meaningful nonplanar perspective coverage')
    return {'covered_pixels':int(np.count_nonzero(actual_coverage)),'covered_primitives':covered_primitives,'max_rgba_error':rgba_error,'max_shader_base_error':base_error,'max_uv1_vs_screen_u_difference':non_screen}
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--report',type=Path,default=MODULE/'INDEPENDENT_NEW_PARTS_REVIEW.json');args=parser.parse_args()
    assets=WORK/'hdr_rebuild167/model_audit168/LOCAL_ONLY_styles';archive=WORK.parent/'upload/ULike_Natural_blush_1790815043265.zip'
    jdk=WORK/'models168/tools/jdk21/jdk-21.0.12.1+1/bin';sdk=WORK/'models168/tools/android.jar'
    production=sorted((MODULE/'src').rglob('*.java'));deps=[CORE/'android_still_analysis/src/main/java/com/hiro/ulike/hdr/stillanalysis/StillMessageCollector.java',CORE/'style_pipeline/src/main/java/com/hiro/ulike/style/SampledMakeupPipeline.java']
    inventory=production+deps+[HERE/'NewPartsReview.java',Path(__file__).resolve()];before={str(p.relative_to(CORE)):sha(p) for p in inventory}
    with tempfile.TemporaryDirectory(prefix='style-new-review-') as temporary:
        root=Path(temporary);classes=root/'classes';classes.mkdir();android=root/'android';android.mkdir();count=fixtures(root,archive)
        run([jdk/'javac','-d',classes,*production,*deps,HERE/'NewPartsReview.java'])
        run([jdk/'javac','-source','8','-target','8','-bootclasspath',sdk,'-d',android,*production,*deps])
        java=json.loads(run([jdk/'java','-Xmx256m','-cp',classes,'com.hiro.ulike.binding.NewPartsReview',root,assets,archive]));raster=reference(root/'raster.bin',assets)
    after={str(p.relative_to(CORE)):sha(p) for p in inventory};check(before==after,'source freeze held throughout review')
    report={'status':'PASS_INDEPENDENT_NEW_STYLE_PARTS_REVIEW','scope':['PinnedPngTexture','Purity3dBinding','StyleObserverInstaller'],
        'sdk36_compile':True,'java_checks':java['checks'],'independent_numpy_checks':CHECKS,'constructed_png_cases':count,'raster':raster,
        'resolved_findings':['Caller PNG bytes are cloned before verification and parsing; deterministic mutation-at-hash-completion test preserves verified pixels.',
                             'Archive is captured to bounded owned bytes, then hashed and extracted from that same snapshot; deterministic path-switch test cannot substitute unpinned texture.'],
        'reviewed_file_sha256':before,'sdk_sha256':sha(sdk),'primary_sources':['https://www.w3.org/TR/png-3/','https://registry.khronos.org/OpenGL/specs/es/3.2/GLSL_ES_Specification_3.20.html'],
        'limits':['Synthetic nonplanar observed positions exercise real pinned Purity UV/topology/material; actual device mesh/GPU parity is untested.',
                  'Explicit replacement raster policy remains no cull/depth, last triangle, single pixel center; original sampler/orientation/renderer overrides are unverified.',
                  'One-still ownership depends on the separately reviewed lifecycle and actual SDK callback/source binding; installer alone is not a same-still proof.',
                  'No full style provider, missing 2D geometry/masks, live device execution, or native visual equivalence is claimed.',
                  'Skin-mask diagnostics are independently reviewed by another reviewer in native_readback_contract/INDEPENDENT_DIAGNOSTIC_REVIEW.json.']}
    args.report.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:report[k] for k in ('status','java_checks','independent_numpy_checks','constructed_png_cases','raster')}))
if __name__=='__main__':main()
