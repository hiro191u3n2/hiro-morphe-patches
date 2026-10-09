#!/usr/bin/env python3
"""Populate the publication contract from the exact locally validated release outputs."""
from pathlib import Path
import argparse, importlib.util, json, zipfile
from build1938 import config, require, sha, ROOT

def main():
 p=argparse.ArgumentParser();p.add_argument('--dist',type=Path,required=True);p.add_argument('--manifest',type=Path,default=ROOT.parent/'manifest.json');a=p.parse_args()
 reviewed=config(a.manifest);qa=json.loads((a.dist/'QA_ULike_v1.9.38.json').read_text())
 require(qa.get('original_apk_apply_tested') is True and qa.get('device_tested') is False,'Require actual original APKS validation, never imply device test')
 pub_path=ROOT.parent/'publication/publish1938.py';spec=importlib.util.spec_from_file_location('publish1938_review',pub_path);pub=importlib.util.module_from_spec(spec);spec.loader.exec_module(pub)
 require(qa.get('selected_candidates')==['front_startup_black_preview'],'Exact approved selection')
 reviewed.update(original_apk_apply_tested=True,change_plan_reviewed=True,qa_contract_reviewed=True,
   allowed_changed_bundle_entries=qa['changed_bundle_entries'],allowed_added_bundle_entries=qa['added_bundle_entries'],
   allowed_changed_standalone_entries=qa['changed_standalone_entries'],allowed_added_standalone_entries=qa['added_standalone_entries'],
   required_source_paths=sorted(k for k in qa['source_sha256'] if k.startswith('src/')),
   qa_required_values={k:v for k,v in qa.items() if k not in ('source_sha256','artifacts','input_sha256')},
   artifacts={name:{'sha256':sha((a.dist/name).read_bytes()),'bytes':(a.dist/name).stat().st_size} for name in pub.ASSETS})
 contract=reviewed['qa_required_values']
 for key in pub.REQUIRED_QA:contract[key]=pub.lookup_qa(qa,key)
 contract['host_quality_result.assertions']=qa['host_quality_result']['assertions']
 for suite,value in qa['host_quality_result']['suites'].items():contract['host_quality_result.suites.'+suite+'.status']=value['status']
 text=json.dumps(reviewed,ensure_ascii=False,indent=2,allow_nan=False)+'\n';a.manifest.write_text(text);(ROOT.parent/'publication/manifest.template.json').write_text(text)
 expected=pub.load_expected(a.manifest)
 with zipfile.ZipFile(a.dist/pub.SOURCE_ZIP) as z:entries={n:{'sha256':sha(z.read(n)),'bytes':len(z.read(n))} for n in z.namelist()}
 (ROOT.parent/'publication/source-zip-entries.json').write_text(json.dumps(entries,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps({'status':'reviewed','artifacts':reviewed['artifacts'],'required_sources':len(reviewed['required_source_paths'])},indent=2))
if __name__=='__main__':main()
