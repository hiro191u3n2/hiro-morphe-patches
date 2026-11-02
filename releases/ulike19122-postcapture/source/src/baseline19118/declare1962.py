#!/usr/bin/env python3
"""Pin the reviewed GX source and publication graph before build/publication."""
from pathlib import Path
import argparse,importlib.util,json
from build1962 import ROOT,VERSION,BASE_VERSION,BUNDLE_VERSION,BASE_BUNDLE_VERSION,BASE_SINGLE_SHA256,BASE_BUNDLE_SHA256,BASE_SINGLE_BYTES,BASE_BUNDLE_BYTES,SELECTED,GX_STATUS,PRODUCTION,CHANGED,ADDED,TOOL_PINS,json_bytes,sha,source_pins,source_preserved_roots

def declaration(source):
 publication=source.parent/'publication'
 spec=importlib.util.spec_from_file_location('gx1962_publisher',publication/'publish1962.py');pub=importlib.util.module_from_spec(spec);spec.loader.exec_module(pub)
 return {'schema':'ulike1962-build-declaration-v1','change_plan_reviewed':True,'qa_contract_reviewed':True,'ulike_version':VERSION,'bundle_version':BUNDLE_VERSION,'baseline_ulike_version':BASE_VERSION,'baseline_bundle_version':BASE_BUNDLE_VERSION,'baseline_ulike_sha256':BASE_SINGLE_SHA256,'baseline_bundle_sha256':BASE_BUNDLE_SHA256,'baseline_ulike_bytes':BASE_SINGLE_BYTES,'baseline_bundle_bytes':BASE_BUNDLE_BYTES,'baseline_ulike_url':pub.SINGLE_BASE_URL,'baseline_bundle_url':pub.BASE_URL,'selected_candidates':SELECTED,'gx_implementation_status':GX_STATUS,'production_helper_roots':PRODUCTION,'source_preserved_helper_roots':source_preserved_roots(),'intentionally_modified_helper_roots':sorted(set(PRODUCTION)-set(source_preserved_roots())),'toolchain_sha256':TOOL_PINS,'ndk_revision':'27.2.12479018','jdk_runtime_version':'21.0.8+9-LTS','jdk_vendor':'Eclipse Adoptium','android_device_tested':False,'original_apk_apply_tested':False,'allowed_changed_bundle_entries':sorted(CHANGED),'allowed_added_bundle_entries':sorted(ADDED),'allowed_changed_standalone_entries':sorted(CHANGED),'allowed_added_standalone_entries':sorted(ADDED),'reviewed_source_sha256':source_pins(),'reviewed_publication_sha256':{name:sha((publication/name).read_bytes()) for name in ('publish1962.py','ulike1962-gx24-gx29-publish.yml','README.md')}}
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,default=ROOT.parent/'manifest.json');p.add_argument('--gpu-native',type=Path);a=p.parse_args();data=declaration(ROOT)
 if a.gpu_native:data['gpu_native_payload']={'sha256':sha(a.gpu_native.read_bytes()),'bytes':a.gpu_native.stat().st_size}
 a.output.write_bytes(json_bytes(data));print('PASS declaration pins complete reviewed source/publication graph')
