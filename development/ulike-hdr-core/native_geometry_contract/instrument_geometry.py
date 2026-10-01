#!/usr/bin/env python3
"""Private same-still diagnostics. Writes caller-owned styles only outside source."""
import hashlib, importlib.util, json, shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parent
BASE=ROOT.parent/'android_style_binding'
spec=importlib.util.spec_from_file_location('hiro_style_base',BASE/'instrument_style.py')
base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)
MESSAGE_ID=0x554c4701

def appendix(feature,nonce):
 if feature not in ['AmazingFeature1','AmazingFeature3','AmazingFeature5','AmazingFeature6','AmazingFeature7']:
  raise ValueError('unsupported pinned 2D feature')
 if isinstance(nonce,bool) or not isinstance(nonce,int) or not 1<=nonce<=2147483647:
  raise ValueError('positive int nonce required')
 return (ROOT/'late_geometry.lua.in').read_text().replace('@FEATURE@',feature).replace('@NONCE@',str(nonce)).replace('@ATTRIBUTE@','POSITION' if feature=='AmazingFeature6' else 'TEXCOORD7').replace('@COMPONENTS@','3' if feature=='AmazingFeature6' else '2')

def instrument(archive,style,output,nonce):
 output=Path(output).resolve()
 if output==ROOT.parent or ROOT.parent in output.parents:raise ValueError('private output must be outside distributable core')
 # The existing installer snapshots and pins caller archive bytes and every original script.
 manifest=base.instrument(archive,style,output,nonce)
 try:
  specs=[x for x in json.loads((BASE/'SCRIPT_OBSERVER_PINS.json').read_text())[style] if x['kind']=='uniform']
  patched=[]
  for item in specs:
   path=output/item['path'];source=path.read_text();marker='exports.EffectFaceMakeupSystemScript = EffectFaceMakeupSystemScript'
   if source.count(marker)!=1:raise ValueError('pinned insertion marker mismatch')
   feature=item['path'].split('/')[0]
   path.write_text(source.replace(marker,appendix(feature,nonce)+'\n'+marker))
   patched.append({'path':item['path'],'original_sha256':item['sha256'],'patched_sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
  result={'schema':'ulike-private-late-mesh-diagnostic-1','style':style,'nonce':nonce,'message_id':MESSAGE_ID,
          'expected_features':[x['path'].split('/')[0] for x in specs],'patched':patched,
          'composes_with_same_still_observer':True,'production_geometry_verified':False,
          'per_face_ranges_known':False,'legacy_v2_opacity_known':False,'android_device_execution':False,
          'usage':'New owned SDK instance; one immutable submitted image; same positive nonce for G1 and S1; callback collector must not accept G1 as S1 or photo output.'}
  (output/'hiro-geometry-manifest.json').write_text(json.dumps(result,indent=2)+'\n')
  return result
 except BaseException:
  shutil.rmtree(output);raise

if __name__=='__main__':
 import argparse
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('archive');p.add_argument('style',choices=['natural','purity']);p.add_argument('output');p.add_argument('--nonce',type=int,required=True)
 a=p.parse_args();print(json.dumps(instrument(a.archive,a.style,a.output,a.nonce),indent=2))
