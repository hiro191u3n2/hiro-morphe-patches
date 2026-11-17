#!/usr/bin/env python3
"""Preserve v378 raw ZIP records and all provider definitions, add UI options."""
from pathlib import Path
import json,subprocess,zipfile,hashlib
import zip_helpers as helper
ROOT=Path(__file__).resolve().parent
BASE=ROOT/'inputs/Hiro_Morphe_Patches_v1.0.378.mpp'
BASE_SHA='e68e835c0f3f16441d41960bfa2a5f4201678702d52c4b6a8868b25cf87d08f5'
def main():
 assert helper.sha(BASE)==BASE_SHA
 out=ROOT/'dist';out.mkdir(exist_ok=True);w=out/'work';w.mkdir(exist_ok=True)
 with zipfile.ZipFile(BASE)as z:base={n:z.read(n)for n in z.namelist()}
 with zipfile.ZipFile(ROOT/'component.mpp')as z:add={n:z.read(n)for n in z.namelist()}
 for n,d in [('base',base),('addon',add)]:(w/(n+'.dex')).write_bytes(d['classes.dex'])
 cp=ROOT/'tools/morphe.jar'
 subprocess.run(['java','-XX:-UsePerfData','-jar',str(ROOT/'tools/ecj.jar'),'-11','-nowarn','-cp',str(cp),'-d',str(w),str(ROOT/'MergeProviders.java')],check=True)
 subprocess.run(['java','-XX:-UsePerfData','-cp',str(w)+':'+str(cp),'MergeProviders',str(w/'base.dex'),str(w/'addon.dex'),str(w/'merged.dex'),str(w/'preservation.json')],check=True)
 mf=helper.headers(base[helper.MF]);assert mf['Version']=='1.0.378'
 mf.update(Version='1.0.379',Timestamp='2026-11-17T00:00:00',Description='NewX chat recommendations, fixed bars and 120Hz settings. Existing patches retained.')
 changes={helper.MF:helper.manifest(mf),'classes.dex':(w/'merged.dex').read_bytes()}
 additions={n:b for n,b in add.items()if n not in changes}
 assert not(base.keys()&additions.keys())
 output=out/'Hiro_Morphe_Patches_v1.0.379.mpp';records=helper.local_records(BASE)
 with zipfile.ZipFile(output,'w')as z:
  for n,(i,raw)in records.items():
   if n in changes:i,raw=helper.encoded(n,changes[n])
   helper.append_raw(z,i,raw)
  for n,b in sorted(additions.items()):helper.append_raw(z,*helper.encoded(n,b))
 with zipfile.ZipFile(output)as z:assert z.testzip()is None;assert{n:z.read(n)for n in z.namelist()}==dict(base,**changes,**additions)
 new=helper.local_records(output);preserved=[n for n in base if n not in changes]
 assert all(new[n][1]==records[n][1]for n in preserved)
 standalone=out/'NewX_UI_Options_v1.0.0.mpp';standalone.write_bytes((ROOT/'component.mpp').read_bytes())
 report={'result':'PASS','baseline_sha256':BASE_SHA,'artifacts':{p.name:{'bytes':p.stat().st_size,'sha256':helper.sha(p)}for p in [output,standalone]},'preserved_zip_entries':len(preserved),'all_existing_extensions_identical':True,'dex_preservation':json.loads((w/'preservation.json').read_text())}
 (out/'build-evidence.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':main()
