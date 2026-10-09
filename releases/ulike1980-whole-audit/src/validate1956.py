#!/usr/bin/env python3
"""Verify current-source host evidence and serialized package delta against .55."""
from pathlib import Path,PurePosixPath
import argparse,json
from build1956 import *
QA_NAME=f'QA_ULike_v{VERSION}.json'
SOURCE_GROUPS=['compiled_production_source_sha256','compiled_transformer_source_sha256','compiled_native_source_sha256','executed_host_source_sha256']

def file_pins(source,pins):
 require(isinstance(pins,dict) and pins,'Missing source pins')
 for name,digest in pins.items():
  p=PurePosixPath(name);require(not p.is_absolute() and '..' not in p.parts and '\\' not in name,'Unsafe source path')
  require((source/name).is_file() and sha((source/name).read_bytes())==digest,'Source differs '+name)

def host_checks(qa,source):
 require(qa.get('schema')=='ulike1956-optimization-v1' and qa.get('ulike_version')==VERSION and qa.get('bundle_version')==BUNDLE_VERSION and qa.get('baseline_ulike_version')==BASE_VERSION and qa.get('baseline_bundle_version')==BASE_BUNDLE_VERSION and qa.get('selected_candidates')==SELECTED,'Build identity')
 result=qa['host_quality_result'];require(result.get('status')=='passed' and result.get('selected')==SELECTED and type(result.get('assertions')) is int and result['assertions']>0,'Executed host result')
 reports=result['reports'];require(set(reports)=={'nr1_nr4_current_source_preserved','h28_h29_exact_cpu','h30_exact_residual','h31_persistent_dispatch','h32_native_scratch','h33_encode_publication_overlap','native_scratch_memory_budget'},'All focused regression suites required')
 require(all((r.get('status')=='passed' or r.get('passed') is True) and type(r.get('assertions')) is int and r['assertions']>0 for r in reports.values()) and sum(r['assertions'] for r in reports.values())==result['assertions'],'Host assertions sum')
 require(sha(json.dumps(result,sort_keys=True).encode())==qa['host_quality_result_sha256'],'Host result digest')
 for key in ['original_apk_apply_tested','original_split_merge_tested','ci_android_apply_tested','device_tested','device_quality_verified','device_save_speed_measured']:require(qa.get(key) is False,'Unperformed Android test claimed '+key)
 require(all(result.get(k) is False for k in ['physical_android_tested','device_speedup_verified','device_quality_improvement_verified']),'Device host scope')
 for group in SOURCE_GROUPS:file_pins(source,qa[group])
 require(qa['compiled_production_source_sha256']=={n+'.java':sha((source/(n+'.java')).read_bytes()) for n in PRODUCTION} and qa['replaced_helper_roots']==sorted(PRODUCTION),'Exact current production roots')
 require(qa['capture_fusion_enabled'] is False and qa['capture_class_bytecode_identical'] is True and qa['capture_begin_image_false_return_verified'] is True,'No-fusion capture retained')
 return result

def validate(args):
 qa=json.loads((args.dist/QA_NAME).read_text());result=host_checks(qa,ROOT)
 for name,digest,size in [(BASE_SINGLE,BASE_SINGLE_SHA256,BASE_SINGLE_BYTES),(BASE_BUNDLE,BASE_BUNDLE_SHA256,BASE_BUNDLE_BYTES)]:raw=(args.input/name).read_bytes();require(sha(raw)==digest and len(raw)==size,'Pinned input '+name)
 base,bundle=archive(args.input/BASE_SINGLE),archive(args.input/BASE_BUNDLE);current,combined=archive(args.dist/SINGLE),archive(args.dist/BUNDLE);delta={}
 for kind,old,new,version in [('standalone',base,current,VERSION),('bundle',bundle,combined,BUNDLE_VERSION)]:
  changed,added=resource_delta(old,new,kind);delta[kind+'_changed']=changed;delta[kind+'_added']=added
  require(qa['changed_'+kind+'_entries']==changed and qa['added_'+kind+'_entries']==added,'QA delta differs')
  require(headers(new['META-INF/MANIFEST.MF'])['Version']==version,'MPP version');dex_integrity(new['classes.dex'],kind);dex_integrity(new['ulike/runtime.dex'],kind+' runtime')
 require({n:b for n,b in current.items() if own(n)}=={n:b for n,b in combined.items() if own(n)},'Current ULike copies differ')
 require(all(combined[n]==b for n,b in bundle.items() if not own(n) and n not in ('classes.dex','META-INF/MANIFEST.MF')),'Other app resource')
 unchanged={n:{'sha256':sha(b),'bytes':len(b)} for n,b in base.items() if (n.endswith('.so') or n=='ulike186/runtime/0000.bin') and n not in {r[4] for r in NATIVES}}
 require(unchanged==qa['unchanged_native_payloads'] and len(unchanged)==4 and all(current[n]==base[n] for n in unchanged),'Unchanged native resources')
 native={}
 for key,folder,builder,reportfile,entry in NATIVES:
  raw=current[entry];report=qa['native_builds'][key];native[entry]={'sha256':sha(raw),'bytes':len(raw)}
  require(raw[:6]==b'\x7fELF\x02\x01' and raw[18:20]==b'\xb7\x00' and report['sha256']==sha(raw) and report['bytes']==len(raw) and report['ndk_revision']=='27.2.12479018' and report.get('physical_android_tested') is False,'Native provenance '+key)
  require(report['load_segment_alignments'] and min(report['load_segment_alignments'])>=16384,'Native alignment')
  file_pins(ROOT,{folder+'/'+n:v for n,v in report['sources'].items()})
 require(all(current[n]==base[n] for n in ['ulike/methods.dex','ulike/methods.tsv']),'Protected native method payloads')
 artifacts={n:{'sha256':sha((args.dist/n).read_bytes()),'bytes':(args.dist/n).stat().st_size} for n in [SINGLE,BUNDLE]};require(artifacts==qa['artifacts'],'QA artifact fingerprints')
 inv=json.loads((args.build/'emitted/optimization-inventory1956.json').read_text());require(all(qa.get(k)==v for k,v in inv.items()),'Serialized DEX inventory')
 log=(args.build/'emitted.log').read_text();require(all(t in log for t in ['serialized_dex_verified=true','unrelated_runtime_preserved=true','installer_row_count_preserved=9','begin_image_false_return_verified=true','save_publication_hooks_inverse_verified=true']),'Serialized preservation proof')
 for local,entry in [('runtime.dex','ulike/runtime.dex'),('loader.dex','classes.dex'),('UlikeHqMaxPatch.class',LOADER),('IntegrationPayload186.class',INSTALLER)]:require((args.build/'emitted'/local).read_bytes()==current[entry],'Audited bytes differ '+entry)
 require((args.build/'emitted/bundle-loader.dex').read_bytes()==combined['classes.dex'],'Audited bundle loader differs')
 evidence={'schema':'ulike1956-desktop-validation-v1','status':'passed','ulike_version':VERSION,'bundle_version':BUNDLE_VERSION,'selected_candidates':SELECTED,'artifacts':artifacts,'resource_delta':delta,'source_consistency_verified':True,'source_groups':{k:qa[k] for k in SOURCE_GROUPS},'host_quality_result_sha256':qa['host_quality_result_sha256'],'host_assertions':result['assertions'],'host_reports':result['reports'],'native_payloads':native,'unchanged_native_payloads':unchanged,'serialized_dex_preservation_verified':True,'save_publication_hooks_inverse_verified':True,'single_image_capture_preserved':True,'original_apk_apply_tested':False,'original_split_merge_tested':False,'device_tested':False,'device_quality_verified':False,'device_save_speed_measured':False}
 args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_bytes(json_bytes(evidence));print('PASS current-source focused tests and serialized .55 delta; original APKS/device untested');return evidence
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for name in ['build','dist','input','output']:p.add_argument('--'+name,type=Path,required=True)
 a=p.parse_args();validate(a)
