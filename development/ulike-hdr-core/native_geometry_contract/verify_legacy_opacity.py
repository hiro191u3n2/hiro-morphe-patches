#!/usr/bin/env python3
"""Verify the supplied legacy blusher's alpha inputs without publishing its assets."""
import argparse,hashlib,json,zipfile
from pathlib import Path
ARCHIVE='cd5da20fefe1f9bd594e4e0d055d6320392fa54fb5df9d34b8f159c791c5b117'
PREFIX='materials/016/FaceMakeupV2_byExport2/'
PINS={
 'makeup/blusher.vert':'6e243b450bf20d0070a891b53d34d20c5f2cec862c7dcdff4204dd5831b543b7',
 'makeup/blusher.frag':'2fc5e67fab8ae3de9992379493686623e27676b6fbab4c8db8b4d6720d4bf3f2',
 'FaceMakeupModule.lua':'414e3ef606ff8c822ba7ce0a7ee6b0881ae2cbeb75f28daa5e75848151959c76'}
def verify(archive):
 if hashlib.sha256(archive.read_bytes()).hexdigest()!=ARCHIVE:raise ValueError('unsupported private archive')
 with zipfile.ZipFile(archive) as z:
  text={}
  for path,pin in PINS.items():
   raw=z.read(PREFIX+path)
   if hashlib.sha256(raw).hexdigest()!=pin:raise ValueError('unsupported supplied shader/controller')
   text[path]=raw.decode('utf-8')
 vertex=text['makeup/blusher.vert'];fragment=text['makeup/blusher.frag'];controller=text['FaceMakeupModule.lua']
 for pattern,source in [('attribute float attOpacity;',vertex),('varOpacity = attOpacity;',vertex),('float alpha = sucai.a * intensity* varOpacity;',fragment),('percentage * self.dataTable[i].Opacity',controller)]:
  if pattern not in source:raise ValueError('pinned dependency changed')
 return {'status':'PASS_PINNED_LEGACY_ALPHA_DEPENDENCIES','archive_sha256':ARCHIVE,'private_asset_sha256':PINS,
  'facts':{'per_vertex_input':'attOpacity','raster_interpolant':'varOpacity','alpha_factors':['material texture alpha','intensity uniform','interpolated per-vertex opacity'],'controller_updates':'intensity from slider times authored opacity; does not replace the independent per-vertex opacity input'},
  'remaining_evidence_required':['actual per-vertex attOpacity values for the same still and selected native filter','actual native vertex/index routing and final uMVPMatrix/uSTMatrix','frame-correlated native draw order and completion'],
  'limits':['D1 observes selected Amazing mesh components, not legacy FaceMakeupV2 CPU/GPU buffers','getAMGScene can return null; no forced engine switching or opaque pointer cast is implemented','shader alpha dependence alone cannot reconstruct the native opacity array','no original shader/model/library is distributed'],
  'per_vertex_opacity_observed':False,'production_binding_verified':False,'device_execution':False}
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--purity',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=verify(a.purity);out['verifier_sha256']=hashlib.sha256(Path(__file__).read_bytes()).hexdigest();a.output.write_text(json.dumps(out,indent=2)+'\n');print(out['status'])
