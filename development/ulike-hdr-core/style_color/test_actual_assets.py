"""Compare the C++ core with SciPy's independent image interpolator.

Private PNGs are read in place, not distributed by this component. Temporary
RGBA fixtures are destroyed. Test decode contract is RGBA bytes with source row
order, no ICC/gamma conversion; this does not assert the original engine contract.
"""
from pathlib import Path
import argparse, hashlib, io, json, struct, subprocess, tempfile, zipfile
import numpy as np
from PIL import Image
from scipy.ndimage import map_coordinates

HERE=Path(__file__).resolve().parent
parser=argparse.ArgumentParser(description='Local private ZIPs are never copied into source artifacts.')
parser.add_argument('natural_zip',type=Path)
parser.add_argument('purity_zip',type=Path)
args=parser.parse_args()
zip_paths={'natural':args.natural_zip,'purity':args.purity_zip}
zip_pins={'natural':'5b50343dcedc7e9aadd218626e621ad683cd67372d225024f118b47f493546f0',
          'purity':'cd5da20fefe1f9bd594e4e0d055d6320392fa54fb5df9d34b8f159c791c5b117'}
for label,p in zip_paths.items():
    assert hashlib.sha256(p.read_bytes()).hexdigest()==zip_pins[label],f'{label}: unexpected source ZIP'
def read(name):
    label,member=name.split('/',1)
    with zipfile.ZipFile(zip_paths[label]) as z:
        info=z.getinfo(member)
        assert info.file_size<5_000_000
        return z.read(member)
expected_pins={
    'purity/materials/016/AmazingFeature9/image/filter.png':'ad920ba45ce41a189f8203fb66a33f9ed9a4dec14e050a565509b1c236f3c6b7',
    'natural/materials/016/AmazingFeature2/image/filter_bg.png':'3b6693011b425c4d124aa4b1de71e18d71b509257b305c18e8bb48837d704a40',
    'natural/materials/016/AmazingFeature2/image/filter_skin.png':'38d2730d4033c4dba337f57d8b8905b782f942645f0da646e83ad5082a39b78b',
    'purity/materials/016/AmazingFeature9/xshader/pass0.frag':'89203357634dbd103fca5d8e0c6fe2e2ed7e544f10c57e540be775fcbe41eb96',
    'natural/materials/016/AmazingFeature2/xshader/skinseg.frag':'9168eb81e4b3245952514803ded08f215a994d06cb31403ad83d020e1346574b',
    'purity/materials/016/AmazingFeature8/xshader/skinseg.frag':'9168eb81e4b3245952514803ded08f215a994d06cb31403ad83d020e1346574b',
}
paths=[
    'purity/materials/016/AmazingFeature9/image/filter.png',
    'natural/materials/016/AmazingFeature2/image/filter_bg.png',
    'natural/materials/016/AmazingFeature2/image/filter_skin.png',
]
arrays=[];pins={}
for name in paths:
    data=read(name)
    pins[name]={'sha256':hashlib.sha256(data).hexdigest()}
    assert pins[name]['sha256']==expected_pins[name]
    with Image.open(io.BytesIO(data)) as im:
        a=np.asarray(im.convert('RGBA'),dtype=np.uint8)
    arrays.append(a)
    pins[name]['shape']=list(a.shape)
assert arrays[0].shape==(1024,1024,4)
assert arrays[1].shape==arrays[2].shape==(512,512,4)
for name in [
    'purity/materials/016/AmazingFeature9/xshader/pass0.frag',
    'natural/materials/016/AmazingFeature2/xshader/skinseg.frag',
    'purity/materials/016/AmazingFeature8/xshader/skinseg.frag',
]:
    pins[name]={'sha256':hashlib.sha256(read(name)).hexdigest()}
    assert pins[name]['sha256']==expected_pins[name]
assert pins['natural/materials/016/AmazingFeature2/xshader/skinseg.frag']==pins['purity/materials/016/AmazingFeature8/xshader/skinseg.frag']
for leaf in ['filter_bg.png','filter_skin.png']:
    assert read(f'natural/materials/016/AmazingFeature2/image/{leaf}')==read(f'purity/materials/016/AmazingFeature8/image/{leaf}')

rng=np.random.default_rng(167)
rows=list(rng.random((2048,6)))
for b in np.linspace(0,1,64):
    for delta in [-1e-8,0,1e-8]:
        for alpha in [0.,1.]:
            rows.append([.314159,.98765,float(np.clip(b+delta,0,1)),.37,alpha,.63])
for v in [0.,1.]:
    for alpha in [0.,.5,1.]: rows.append([v,v,v,1.,alpha,v])
x=np.asarray(rows,dtype=np.float64);rgb=x[:,:3];source=x[:,:4]

def sample(a,slice_index,dtype=np.float64):
    # Texture sampling is SciPy code, independent of the C++ bilinear sampler.
    s=slice_index.astype(dtype);r=rgb.astype(dtype)
    tile=np.column_stack((np.remainder(s,8),np.floor_divide(s,8))).astype(dtype)
    uv=(tile*dtype(1/8)+dtype(.5/512)+dtype(1/8-1/512)*r[:,:2]).astype(dtype)
    h,w=a.shape[:2]
    coords=np.stack((uv[:,1].astype(float)*h-.5,uv[:,0].astype(float)*w-.5))
    return np.column_stack([map_coordinates(a[:,:,i].astype(float)/255,coords,order=1,mode='nearest',prefilter=False) for i in range(4)])

blue=rgb[:,2]*63;floor=np.floor(blue);ceil=np.ceil(blue)
lo=sample(arrays[0],floor);hi=sample(arrays[0],ceil)
mapped=lo+(hi-lo)*(blue-floor)[:,None];mapped[:,3]=source[:,3]
expected_purity=source+(mapped-source)*x[:,4,None]
bg=sample(arrays[1],floor);skin=sample(arrays[2],floor)
mixed=bg+(skin-bg)*x[:,5,None];mixed[:,3]=source[:,3]
expected_skin=source+(mixed-source)*x[:,4,None]
expected=np.column_stack((expected_purity,expected_skin))
with tempfile.TemporaryDirectory(prefix='ulike_lut_test_') as tmp:
    bins=[]
    for i,a in enumerate(arrays):
        p=Path(tmp)/f'{i}.rgba';p.write_bytes(struct.pack('<II',a.shape[1],a.shape[0])+a.tobytes());bins.append(str(p))
    data=''.join(' '.join(format(v,'.17g') for v in row)+'\n' for row in x)
    result=subprocess.run([str(HERE/'build/probe'),*bins],input=data,text=True,capture_output=True,check=True)
actual=np.loadtxt(result.stdout.splitlines())
assert actual.shape==expected.shape
error=float(np.max(np.abs(actual-expected)))
assert error<2e-12,error
assert np.array_equal(actual[:,3],source[:,3]) and np.array_equal(actual[:,7],source[:,3])
assert np.array_equal(actual[x[:,4]==0,:4],source[x[:,4]==0])
assert np.array_equal(actual[x[:,4]==0,4:],source[x[:,4]==0])
# Demonstrate the sources differ: floor-only skin LUT is not trilinear blue interpolation.
wrong_bg=bg+(sample(arrays[1],ceil)-bg)*(blue-floor)[:,None]
wrong_skin=skin+(sample(arrays[2],ceil)-skin)*(blue-floor)[:,None]
wrong=wrong_bg+(wrong_skin-wrong_bg)*x[:,5,None];wrong[:,3]=source[:,3]
wrong=source+(wrong-source)*x[:,4,None]
wrong_delta=float(np.max(np.abs(wrong-expected_skin)))
assert wrong_delta>1e-4
qa={
    'status':'PASS','scope':'Two explicit SDR-domain color components; not full styles or HDR beauty',
    'actual_asset_cases':len(rows),'scalar_comparisons':int(actual.size),
    'independent_sampler':'scipy.ndimage.map_coordinates(order=1, mode=nearest)',
    'max_absolute_error':error,'floor_only_vs_wrong_blue_interpolation_max_difference':wrong_delta,
    'texture_contract':'linear clamp-to-edge, source-row order, decoded RGBA/255, no ICC/gamma conversion',
    'original_engine_sampler_and_color_decode_verified':False,
    'actual_gpu_execution':False,'actual_device_execution':False,
    'asset_pins':pins,
    'archive_pins':zip_pins,
    'source_pins':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in [HERE/'style_color.hpp',HERE/'style_color.cpp',HERE/'test_style_color.cpp',HERE/'probe.cpp',Path(__file__)]},
}
(HERE/'QA_ACTUAL_ASSETS.json').write_text(json.dumps(qa,indent=2)+'\n')
print(f'PASS actual LUTs: {len(rows)} cases / {actual.size} scalar comparisons; max error={error:.3g}')
