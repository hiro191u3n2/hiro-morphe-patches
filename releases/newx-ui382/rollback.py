#!/usr/bin/env python3
"""Restore published380 executable payloads; only package release metadata changes."""
from pathlib import Path
import json,zipfile
import zip_helpers as helper
ROOT=Path(__file__).resolve().parent
TIMESTAMP='2026-11-17T10:00:26'
PAIRS=[
 ('Hiro_Morphe_Patches_v1.0.380.mpp','3672fa208fc53f791e8e5c34b1f401709fdb1f19a701e9c2018d6ce0b01a201a','Hiro_Morphe_Patches_v1.0.382.mpp','1.0.382'),
 ('NewX_UI_Options_v1.1.0.mpp','17ca92ef30c3c582b665ac8118c0521b0db834c02519dcfd68317c265678b194','NewX_UI_Options_v1.1.2.mpp','1.1.2'),
]
def main():
 out=ROOT/'dist';out.mkdir(exist_ok=True);artifacts={};proof={}
 for source,sha,target,version in PAIRS:
  original=ROOT/'inputs'/source;assert helper.sha(original)==sha,source
  records=helper.local_records(original)
  with zipfile.ZipFile(original)as z:before={n:z.read(n)for n in z.namelist()}
  meta=helper.headers(before[helper.MF]);meta.update(Version=version,Timestamp=TIMESTAMP)
  if target.startswith('Hiro_'):meta['Description']='Rollback v381 optimization; restore exact v380 executable payloads and all four UI settings.'
  changed=helper.manifest(meta);destination=out/target
  with zipfile.ZipFile(destination,'w')as z:
   for name,(info,raw)in records.items():
    if name==helper.MF:info,raw=helper.encoded(name,changed)
    helper.append_raw(z,info,raw)
  with zipfile.ZipFile(destination)as z:
   assert z.testzip()is None
   after={n:z.read(n)for n in z.namelist()}
  assert set(before)==set(after)
  same=set(before)-{helper.MF};assert all(before[n]==after[n]for n in same)
  result_records=helper.local_records(destination)
  assert all(records[n][1]==result_records[n][1]for n in same)
  assert helper.headers(after[helper.MF])['Version']==version
  artifacts[target]={'bytes':destination.stat().st_size,'sha256':helper.sha(destination)}
  proof[target]={'source':source,'source_sha256':sha,'exact_payload_entries':len(same),'exact_compressed_records':len(same),'only_changed_entry':helper.MF,'classes_dex_identical':True,'all_extensions_identical':True,'all_resources_identical':True}
 report={'result':'PASS','version':'1.0.382','timestamp':TIMESTAMP,'restored_version':'1.0.380','rollback_payloads_match380':True,'artifacts':artifacts,'payload_proof':proof,'all_non_ui_extensions_identical':True,'dex_preservation':{'result':'PASS','existing_non_ui_provider_classes_preserved':True,'entire_provider_dex_identical_to380':True,'all_runtime_dex_identical_to380':True},'user_regression_report':'User reported worse scrolling after381 and requested the previous patch. The381 formatter shortcut and ThreadLocal state have been withdrawn.'}
 (out/'build-evidence.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report,ensure_ascii=False))
if __name__=='__main__':main()
