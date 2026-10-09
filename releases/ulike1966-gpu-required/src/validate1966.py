#!/usr/bin/env python3
"""Validate source-pinned mandatory-GPU tests, package and native provenance."""
from pathlib import Path,PurePosixPath
import argparse,json
from build1966 import (ROOT,VERSION,BASE_VERSION,BUNDLE_VERSION,BASE_BUNDLE_VERSION,
 BASE_SINGLE,BASE_BUNDLE,BASE_SINGLE_SHA256,BASE_BUNDLE_SHA256,BASE_SINGLE_BYTES,
 BASE_BUNDLE_BYTES,SINGLE,BUNDLE,SELECTED,GX_STATUS,PRODUCTION,CHANGED,ADDED,GPU_ENTRY,LICENSE_ENTRY,
 require,sha,archive,headers,own,dex_integrity,json_bytes,production_path,source_preserved_roots,NEW_JNI,NATIVE_TOOL_PINS,ORACLE_SINGLE,ORACLE_SINGLE_SHA256,ORACLE_SINGLE_BYTES,PIXEL_ORACLE)
QA_NAME='QA_ULike_v1.9.66.json'
SOURCE_GROUPS={'compiled_production_source_sha256':'','compiled_transformer_source_sha256':'','compiled_gpu_native_source_sha256':'native1960','executed_host_source_sha256':''}
def file_pins(source,pins,prefix=''):
 require(isinstance(pins,dict) and bool(pins),'Missing compiled/executed source pins')
 for name,digest in pins.items():
  p=PurePosixPath(name);require(not p.is_absolute() and '..' not in p.parts and '\\' not in name,'Unsafe source path')
  target=source/prefix/name;require(target.is_file() and sha(target.read_bytes())==digest,'Source changed after compilation/execution: '+prefix+'/'+name)
 return pins
def host_checks(qa,source):
 require(qa.get('schema')=='ulike1966-gpu-required-v1' and qa.get('ulike_version')==VERSION and qa.get('bundle_version')==BUNDLE_VERSION and qa.get('baseline_ulike_version')==BASE_VERSION and qa.get('baseline_bundle_version')==BASE_BUNDLE_VERSION,'Wrong strict-GPU release identity')
 require(qa.get('selected_candidates')==SELECTED and qa.get('gx_implementation_status')==GX_STATUS,'Reviewed mandatory-GPU scope differs')
 result=qa.get('host_quality_result',{})
 require(qa.get('host_quality_passed') is True and result.get('schema')=='ulike1966-mandatory-gpu-executed-host-v1' and result.get('status')=='passed' and type(result.get('assertions')) is int and result['assertions']>0,'Missing fresh mandatory-GPU host evidence')
 require(sha(json.dumps(result,sort_keys=True).encode())==qa.get('host_quality_result_sha256'),'Host report digest differs')
 require(result.get('gpu_shader_execution_on_host') is True and result.get('strict_gpu_required_routes_verified') is True and result.get('baseline_mpp_sha256')==ORACLE_SINGLE_SHA256 and qa.get('pixel_oracle_input')==PIXEL_ORACLE,'GPU execution and frozen numerical-oracle proof required')
 require(result.get('production_gles_required_math_functional') is True,'Required original-GLES GPU image calculations did not execute successfully')
 reports=result.get('reports',{});required={'gpu_binary64_foundation1965','strong_shader_regression','gpu_model1965','gpu_plan1965','gpu_policy1965','gpu_chroma1965','strict_route1965','gpu_recovery1965','camera_regression1965','gpu_trace_export1966'}
 require(isinstance(reports,dict) and set(reports)==required,'Exact reviewed executed suite inventory required')
 for name,row in reports.items():
  require(row.get('status')=='passed' and type(row.get('assertions')) is int and row['assertions']>0,'Suite did not execute successful assertions: '+name)
  require(row.get('physical_android_tested',row.get('physicalAndroidTested',False)) is False,'Host test claims physical-device coverage')
 require(result['assertions']==sum(row['assertions'] for row in reports.values()),'Host assertion sum differs')
 require(qa.get('android_software_driver_refused') is True and result.get('android_software_driver_refused') is True and reports['gpu_recovery1965'].get('android_software_driver_refused') is True,'Android software GL must be refused before GPU dispatch')
 route=reports['strict_route1965']
 require(route.get('strict_gpu_required_routes_verified') is True and route.get('actual_production_strict_routes_executed') is True and route.get('cpu_image_processing_fallback_enabled') is False and route.get('untreated_success_on_gpu_failure_enabled') is False,'Mandatory current production route/failure evidence missing')
 for name in ('gpu_required_noise','gpu_required_protection','gpu_required_correction','bounded_gpu_recovery'):
  require(qa.get(name) is True,'Mandatory-GPU semantic contract differs: '+name)
 for name in ('cpu_image_processing_fallback_enabled','untreated_success_on_gpu_failure_enabled'):
  require(qa.get(name) is False,'Forbidden CPU/untreated fallback enabled: '+name)
 for key in ('camera_baseline_classes_bytecode_identical','camera_timing_hooks_preserved','gpu_diagnostics_in_camera_zip'):require(qa.get(key) is True,'Retained camera/GPU diagnostic contract missing: '+key)
 require(reports['gpu_trace_export1966'].get('boundedGpuDiagnosticsIncluded') is True and reports['gpu_trace_export1966'].get('frozenCameraSnapshotPreserved') is True,'GPU trace export changed frozen camera snapshots')
 require(qa.get('gpu_recovery_max_attempts')==3,'Bounded retry contract differs')
 require(qa.get('host_pixel_equivalence_to_baseline') is False and result.get('host_pixel_equivalence_to_baseline') is False,'Complete pipeline equivalence is not tested by these kernel/route suites')
 require(result.get('historical_cpu_fallback_routes_intentionally_not_asserted') is True,'Intentional route change must be disclosed')
 for key in ('original_apk_apply_tested','original_split_merge_tested','ci_android_apply_tested','device_tested','device_quality_verified','device_save_speed_measured','gpu_execution_on_physical_android','all_processing_on_gpu'):
  require(qa.get(key) is False,'Unperformed/unsupported coverage claimed: '+key)
 for key in ('physical_android_tested','device_speedup_verified','device_quality_improvement_verified'):
  require(result.get(key) is False,'Host result claims unverified physical device result: '+key)
 require(qa.get('replaced_helper_roots')==sorted(PRODUCTION) and json.loads((source/'production1966.json').read_text())==PRODUCTION,'Production helper graph differs')
 require(qa.get('compiled_production_source_sha256')=={production_path(n).relative_to(ROOT).as_posix():sha(production_path(n).read_bytes()) for n in PRODUCTION} and qa.get('production_source_consistency_verified') is True,'Current production differs from MPP compiler source')
 for key,prefix in SOURCE_GROUPS.items():file_pins(source,qa.get(key),prefix)
 file_pins(source,result.get('sources'));file_pins(source,result.get('tests'))
 require(qa.get('unchanged_source_helper_roots_bytecode_identical') is True and qa.get('source_preserved_helper_roots')==source_preserved_roots() and qa.get('intentionally_modified_helper_roots')==sorted(set(PRODUCTION)-set(source_preserved_roots())),'Unchanged helper preservation evidence absent')
 require(qa.get('capture_fusion_enabled') is False and qa.get('capture_begin_image_false_return_verified') is True and qa.get('capture_class_bytecode_identical') is True,'Single-image capture contract absent')
 require(qa.get('new_jni_methods')==sorted(NEW_JNI),'Exact new JNI descriptor inventory differs')
 require(qa.get('audit_blacktap_bytecode_preserved') is True and qa.get('audit_exitbusy_bytecode_preserved') is True,'Inherited audited UI bytecode changed')
 return result

def validate(args):
 qa=json.loads((args.dist/QA_NAME).read_text());result=host_checks(qa,ROOT);inputs=args.input or args.build.parent/'input'
 for name,digest,size in ((BASE_SINGLE,BASE_SINGLE_SHA256,BASE_SINGLE_BYTES),(BASE_BUNDLE,BASE_BUNDLE_SHA256,BASE_BUNDLE_BYTES)):
  raw=(inputs/name).read_bytes();require(sha(raw)==digest and len(raw)==size,'Pinned .65/.198 baseline differs')
 base,old_bundle=archive(inputs/BASE_SINGLE),archive(inputs/BASE_BUNDLE);current,combined=archive(args.dist/SINGLE),archive(args.dist/BUNDLE)
 oracle_raw=(inputs/ORACLE_SINGLE).read_bytes();require(sha(oracle_raw)==ORACLE_SINGLE_SHA256 and len(oracle_raw)==ORACLE_SINGLE_BYTES,'Frozen .64 numerical oracle input differs');oracle=archive(inputs/ORACLE_SINGLE)
 require(qa.get('baseline_runtime_dex_sha256')==sha(base['ulike/runtime.dex']),'Current .65 package baseline runtime differs')
 require(qa.get('pixel_oracle_runtime_dex_sha256')==sha(oracle['ulike/runtime.dex'])==result['reports']['gpu_chroma1965'].get('published_runtime_dex_sha256'),'Chroma oracle runtime differs from pinned published .64')
 for items,version in ((base,BASE_VERSION),(old_bundle,BASE_BUNDLE_VERSION),(current,VERSION),(combined,BUNDLE_VERSION)):
  require(headers(items['META-INF/MANIFEST.MF']).get('Version')==version,'MPP manifest version differs')
  for name in ('ulike/runtime.dex','classes.dex'):dex_integrity(items[name],name)
 delta={}
 for kind,old,new in (('standalone',base,current),('bundle',old_bundle,combined)):
  changed={name for name in old.keys()&new.keys() if old[name]!=new[name]};added=set(new)-set(old)
  require(set(old)<=set(new) and changed==CHANGED and added==ADDED,'Unexpected resource delta: '+kind)
  require(qa.get('changed_'+kind+'_entries')==sorted(changed) and qa.get('added_'+kind+'_entries')==sorted(added),'QA resource inventory differs')
  delta[kind+'_changed'],delta[kind+'_added']=sorted(changed),sorted(added)
 require({n:b for n,b in base.items() if own(n)}=={n:b for n,b in old_bundle.items() if own(n)} and {n:b for n,b in current.items() if own(n)}=={n:b for n,b in combined.items() if own(n)},'Standalone/bundle ULike copies differ')
 require(all(combined[n]==b for n,b in old_bundle.items() if not own(n) and n not in ('classes.dex','META-INF/MANIFEST.MF')),'Other-app payload changed')
 inherited={n:{'sha256':sha(b),'bytes':len(b)} for n,b in base.items() if (n.endswith('.so') or n=='ulike186/runtime/0000.bin') and n!=GPU_ENTRY}
 require(len(inherited)==11 and inherited==qa.get('inherited_native_payloads') and all(current[n]==base[n] for n in inherited),'Inherited native library differs')
 require(all(current[n]==base[n] for n in ('ulike/methods.dex','ulike/methods.tsv')),'Legacy native methods differ')
 require(current.get(LICENSE_ENTRY)==(ROOT/'THIRD_PARTY_LICENSES.txt').read_bytes() and combined.get(LICENSE_ENTRY)==current[LICENSE_ENTRY],'Binary redistribution license notice differs')
 gpu=current[GPU_ENTRY];require(gpu[:6]==b'\x7fELF\x02\x01' and gpu[18:20]==b'\xb7\x00' and sha(gpu)==qa.get('gpu_native_library_sha256') and len(gpu)==qa.get('gpu_native_library_bytes'),'GPU ELF payload/fingerprint invalid')
 native=qa.get('gpu_native_build',{})
 require(native.get('ndk_revision')=='27.2.12479018' and native.get('native_abi')==19601 and native.get('abi')=='arm64-v8a' and native.get('min_sdk')==26 and native.get('physical_android_tested') is False and native.get('sha256')==sha(gpu) and native.get('bytes')==len(gpu),'Native provenance differs');file_pins(ROOT,native.get('sources'),'native1960')
 require(native.get('toolchain_sha256')==qa.get('gpu_native_toolchain_sha256')==NATIVE_TOOL_PINS and 'Android (12470979' in native.get('compiler_identity',''),'Official r27c native compiler provenance differs')
 exports=qa.get('gpu_native_jni_exports',[])
 require(len(exports)==27 and len(set(exports))==27 and all('Java_com_hiro_ulike_GpuNoise1960_'+name in exports for name in ('recoverNative1965','diagnosticsNative1965','failureNative1965','fp64ProbeNative1965','soft64ProbeNative1965')),'JNI native linkage differs')
 require(qa.get('gpu_native_load_segment_alignments') and all(a>=16384 for a in qa['gpu_native_load_segment_alignments']) and set(qa.get('gpu_native_needed_libraries',[]))<={'libEGL.so','libGLESv3.so','libc.so','libm.so','libdl.so'},'Native page alignment/dependencies differ')
 artifacts={n:{'sha256':sha((args.dist/n).read_bytes()),'bytes':(args.dist/n).stat().st_size} for n in (SINGLE,BUNDLE)};require(qa.get('artifacts')==artifacts,'MPP fingerprints differ')
 inventory=json.loads((args.build/'emitted/gpu-inventory1966.json').read_text());require(all(qa.get(k)==v for k,v in inventory.items()),'Serialized DEX inventory differs')
 require(all(v in (args.build/'emitted.log').read_text() for v in ('begin_image_false_return_verified=true','serialized_dex_verified=true','unrelated_runtime_preserved=true','installer_old_rows_preserved=11','source_preserved_helper_roots_bytecode_identical=true')),'Fresh serialized/inverse preservation proof absent')
 for path,entry in (('runtime.dex','ulike/runtime.dex'),('loader.dex','classes.dex'),('UlikeHqMaxPatch.class','app/hiro/ulike/patches/UlikeHqMaxPatch.class'),('IntegrationPayload186.class','app/hiro/ulike/patches/IntegrationPayload186.class')):
  require((args.build/'emitted'/path).read_bytes()==current[entry],'Audited bytes differ from package')
 require((args.build/'emitted/bundle-loader.dex').read_bytes()==combined['classes.dex'],'Bundle DEX differs from audited bytes')
 evidence={'schema':'ulike1966-desktop-validation-v1','status':'passed','ulike_version':VERSION,'bundle_version':BUNDLE_VERSION,'selected_candidates':SELECTED,'artifacts':artifacts,'resource_delta':delta,'source_consistency_verified':True,'host_quality_result_sha256':qa['host_quality_result_sha256'],'host_assertions':result['assertions'],'host_reports':result['reports'],'compiled_production_source_sha256':qa['compiled_production_source_sha256'],'source_groups':{k:qa[k] for k in SOURCE_GROUPS},'inherited_native_payloads':inherited,'gpu_native_payload':{'sha256':sha(gpu),'bytes':len(gpu)},'serialized_dex_preservation_verified':True,'single_image_capture_admission_verified':True,'original_apk_apply_tested':False,'original_split_merge_tested':False,'device_tested':False,'device_quality_verified':False,'device_save_speed_measured':False,'original_apk_application':{'status':'not_tested','reason':'This CI does not run original-APKS application; separate local application evidence is reported independently.'}}
 args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_bytes(json_bytes(evidence));print('PASS mandatory GPU route/native/source/package evidence; physical Android untested');return evidence
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for n in ('build','dist','output'):p.add_argument('--'+n,type=Path,required=True)
 p.add_argument('--input',type=Path);a=p.parse_args()
 for n,v in vars(a).items():
  if v is not None:setattr(a,n,v.resolve())
 validate(a)
