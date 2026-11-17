#!/usr/bin/env python3
"""Upgrade only the UI component within v380, preserving other compressed records."""
from pathlib import Path
import json,subprocess,zipfile,hashlib
import zip_helpers as helper
ROOT=Path(__file__).resolve().parent
BASE=ROOT/'inputs/Hiro_Morphe_Patches_v1.0.380.mpp'
BASE_SHA='3672fa208fc53f791e8e5c34b1f401709fdb1f19a701e9c2018d6ce0b01a201a'
OLD_UI=ROOT/'inputs/NewX_UI_Options_v1.1.0.mpp'
OLD_UI_SHA='17ca92ef30c3c582b665ac8118c0521b0db834c02519dcfd68317c265678b194'
def main():
 assert helper.sha(BASE)==BASE_SHA
 assert helper.sha(OLD_UI)==OLD_UI_SHA
 out=ROOT/'dist';out.mkdir(exist_ok=True);w=out/'work';w.mkdir(exist_ok=True)
 with zipfile.ZipFile(BASE)as z:base={n:z.read(n)for n in z.namelist()}
 with zipfile.ZipFile(ROOT/'component.mpp')as z:add={n:z.read(n)for n in z.namelist()}
 with zipfile.ZipFile(OLD_UI)as z:old={n:z.read(n)for n in z.namelist()}
 for n in old:
  if n not in [helper.MF,'classes.dex']:assert base[n]==old[n],n
 for n,d in [('base',base),('addon',add),('old-ui',old)]:(w/(n+'.dex')).write_bytes(d['classes.dex'])
 cp=ROOT/'tools/morphe.jar'
 subprocess.run(['java','-XX:-UsePerfData','-jar',str(ROOT/'tools/ecj.jar'),'-11','-nowarn','-cp',str(cp),'-d',str(w),str(ROOT/'MergeProviders.java')],check=True)
 subprocess.run(['java','-XX:-UsePerfData','-cp',str(w)+':'+str(cp),'MergeProviders',str(w/'base.dex'),str(w/'addon.dex'),str(w/'old-ui.dex'),str(w/'merged.dex'),str(w/'preservation.json')],check=True)
 mf=helper.headers(base[helper.MF]);assert mf['Version']=='1.0.380'
 mf.update(Version='1.0.381',Timestamp='2026-11-17T09:30:00',Description='NewX reduce timeline label formatting overhead while preserving all UI options.')
 changes={helper.MF:helper.manifest(mf),'classes.dex':(w/'merged.dex').read_bytes()}
 replacements={n:b for n,b in add.items()if n not in changes}
 old_names=set(old)-{helper.MF,'classes.dex'}
 assert (set(base)&set(replacements))<=old_names
 assert old_names<=set(replacements), 'Old provider classes must be explicitly retained or upgraded'
 changes.update({n:b for n,b in replacements.items()if n in base and b!=base[n]})
 additions={n:b for n,b in replacements.items()if n not in base}
 output=out/'Hiro_Morphe_Patches_v1.0.381.mpp';records=helper.local_records(BASE)
 with zipfile.ZipFile(output,'w')as z:
  for n,(i,raw)in records.items():
   if n in changes:i,raw=helper.encoded(n,changes[n])
   helper.append_raw(z,i,raw)
  for n,b in sorted(additions.items()):helper.append_raw(z,*helper.encoded(n,b))
 with zipfile.ZipFile(output)as z:assert z.testzip()is None;assert{n:z.read(n)for n in z.namelist()}==dict(base,**changes,**additions)
 new=helper.local_records(output);preserved=[n for n in base if n not in changes]
 assert all(new[n][1]==records[n][1]for n in preserved)
 standalone=out/'NewX_UI_Options_v1.1.1.mpp';standalone.write_bytes((ROOT/'component.mpp').read_bytes())
 report={'result':'PASS','baseline_sha256':BASE_SHA,'old_ui_component_sha256':OLD_UI_SHA,'artifacts':{p.name:{'bytes':p.stat().st_size,'sha256':helper.sha(p)}for p in [output,standalone]},'preserved_zip_entries':len(preserved),'replaced_ui_entries':sorted(set(changes)-{helper.MF,'classes.dex'}),'all_non_ui_extensions_identical':True,'dex_preservation':json.loads((w/'preservation.json').read_text())}
 (out/'build-evidence.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':main()
