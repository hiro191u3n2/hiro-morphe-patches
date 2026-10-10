#!/usr/bin/env python3
"""Validate fresh GPU/source/package evidence; physical Android remains untested."""
from pathlib import Path,PurePosixPath
import argparse,json
from build1961 import (ROOT,VERSION,BASE_VERSION,BUNDLE_VERSION,BASE_BUNDLE_VERSION,
 BASE_SINGLE,BASE_BUNDLE,BASE_SINGLE_SHA256,BASE_BUNDLE_SHA256,BASE_SINGLE_BYTES,
 BASE_BUNDLE_BYTES,SINGLE,BUNDLE,SELECTED,GX_STATUS,PRODUCTION,CHANGED,ADDED,GPU_ENTRY,
 require,sha,archive,headers,own,dex_integrity,json_bytes,production_path)
QA_NAME='QA_ULike_v1.9.61.json'
SOURCE_GROUPS={'compiled_production_source_sha256':'','compiled_transformer_source_sha256':'','compiled_gpu_native_source_sha256':'native1960','executed_host_source_sha256':''}

def file_pins(source,pins,prefix=''):
 require(isinstance(pins,dict) and bool(pins),'Missing compiled/executed source pins')
 for name,digest in pins.items():
  p=PurePosixPath(name);require(not p.is_absolute() and '..' not in p.parts and '\\' not in name,'Unsafe source path')
  target=source/prefix/name;require(target.is_file() and sha(target.read_bytes())==digest,'Source changed after compilation/execution: '+prefix+'/'+name)
 return pins

def host_checks(qa,source):
 require(qa.get('schema')=='ulike1961-gx11-gx23-v1' and qa.get('ulike_version')==VERSION and qa.get('bundle_version')==BUNDLE_VERSION and qa.get('baseline_ulike_version')==BASE_VERSION and qa.get('baseline_bundle_version')==BASE_BUNDLE_VERSION and qa.get('selected_candidates')==SELECTED,'Wrong .61 identity/GX scope')
 require(qa.get('gx_implementation_status')==GX_STATUS,'GX capability/status table differs');result=qa.get('host_quality_result',{})
 require(qa.get('host_quality_passed') is True and result.get('status')=='passed' and type(result.get('assertions')) is int and result['assertions']>0,'Missing fresh GPU host tests')
 require(result.get('gpu_shader_execution_on_host') is True and result.get('host_pixel_equivalence_to_baseline') is True,'Real host GPU execution and pinned baseline pixel proof required')
 require(sha(json.dumps(result,sort_keys=True).encode())==qa.get('host_quality_result_sha256'),'Host result digest mismatch')
 require(result.get('schema')=='ulike-gx1961-executed-production-jni-v1' and result.get('unchanged_production_engine_and_shader_sources') is True,'Unchanged executed production JNI/shader source proof required')
 reports=result.get('reports',{});require(isinstance(reports,dict) and {'pixels','foreign_context','late_unmap_failure','unknown_fence_completion'}<=set(reports),'GPU result/fault report inventory differs')
 for name in ('pixels','foreign_context','late_unmap_failure','unknown_fence_completion'):
  row=reports[name];require(row.get('status')=='passed' and type(row.get('assertions')) is int and row['assertions']>0,'Executed GPU report missing assertions: '+name)
 require(sum(row.get('assertions',0) for row in reports.values())==result['assertions'],'Executed GPU assertion count differs')
 pixels=reports['pixels'];require(pixels.get('pixelDifferences')==0 and pixels.get('confidenceDifferences')==0 and pixels.get('cases',0)>=80 and pixels.get('pixels',0)>100000,'Frozen .60 shader pixel equivalence absent')
 oracle=result.get('oracle',{});require(oracle.get('status')=='passed' and oracle.get('referenceNativeEnabled') is False and oracle.get('records',0)>=80,'Independent frozen .60 Java oracle absent')
 require(type(result.get('single_fp64_shader_executed')) is bool and result['single_fp64_shader_executed']==pixels.get('singleFp64Supported'),'Mobile FP64 capability report differs')
 if not result['single_fp64_shader_executed']:require(pixels.get('unsupportedShaderCases',0)>0 and result.get('cpu_fallback_capability_required') is True,'Unsupported FP64 must explicitly report CPU fallback')
 file_pins(source,result.get('sources'));file_pins(source,result.get('tests'))
 for name in ('production_facades_gpu','production_facades_unavailable'):
  row=reports.get(name,{});require(row.get('status')=='passed' and type(row.get('assertions')) is int and row['assertions']>0 and row.get('strongCases',0)>0 and row.get('singleCases',0)>0 and row.get('geometryCases',0)>0 and row.get('physicalAndroidTested') is False,'Fresh production facade/fallback gate absent: '+name)
 late=reports.get('production_facade_late_failure',{});require(late.get('status')=='passed' and type(late.get('assertions')) is int and late['assertions']>0 and late.get('strongCases',0)>0 and late.get('actualGpuStageProbes',0)>0 and late.get('actualInjectedFailedUnmaps',0)>0 and late.get('physicalAndroidTested') is False,'Targeted Strong late-failure fallback test absent')
 require(reports['production_facades_gpu'].get('nativeAvailable') is True and reports['production_facades_gpu'].get('actualGpuStageProbes',0)>0 and reports['production_facades_unavailable'].get('nativeAvailable') is False and reports['production_facade_late_failure'].get('lateFailureFallback') is True,'Production facade native/unsupported/late-failure paths not executed')
 for name in ('published1960_pipeline','candidate1961_pipeline'):
  row=reports.get(name,{});require(row.get('status')=='passed' and type(row.get('assertions')) is int and row['assertions']>0 and row.get('cases',0)>=11 and row.get('androidSdkAndBeautyAreExplicitHostFixtures') is True,'Complete published/candidate host save pipeline absent: '+name)
 require(reports['candidate1961_pipeline'].get('actualGpuDispatches',0)>0 and len(reports['candidate1961_pipeline'].get('exact_published1960_dump_sha256',''))==64,'Complete save pipeline actual GPU dispatch and independent output digest absent')


 if 'desktop_single_fp64' in reports:
  desktop=reports['desktop_single_fp64'];require(desktop.get('errors')==[] and desktop.get('cases',0)>=16 and desktop.get('pixels',0)>6000 and desktop.get('double_precision_preserved') is True and desktop.get('mobile_fp64_confirmed') is False and desktop.get('physical_android_tested') is False,'Desktop FP64 algebra test must not claim Android coverage')


 for name in ('protection1961','analysis1961','batch_engine1961','residual1961','qualification1961','geometry_tiles1961','single_residual_jni1961','batch_second_unmap_failure','batch_two_bank_unknown_completion','analysis_background1961','strong_facade1961','strong_shared_cache1961'):
  row=reports.get(name,{});require(row.get('status')=='passed' and type(row.get('assertions')) is int and row['assertions']>0,'Required new .61 executed suite absent: '+name)
 require(result.get('gx11_gx23_actual_host_coverage') is True,'New .61 runtime evidence absent')
 protection=reports['protection1961'];require(protection.get('values',0)>900000 and protection.get('shader')==6 and protection.get('faceOracle')=='frozen-v1.9.60','Protection kernel/frozen face output gate absent')
 analysis=reports['analysis1961'];require(analysis.get('cases',0)>=42 and analysis.get('exactWords',0)>0 and analysis.get('actualVariants')==[64,32,128] and analysis.get('residentGraph') is True and analysis.get('regionalLag4Periodic') is True,'Full spatial/regional/resident graph proof absent')
 residual=reports['residual1961'];require(residual.get('cases',0)>=26 and residual.get('pixels',0)>165000 and residual.get('changed_pixels',0)>0 and residual.get('rounding_boundary_cases')==3 and residual.get('production_shader_unchanged') is True and residual.get('double_preparation_preserved') is True and residual.get('errors')==[],'Actual residual GPU/frozen60 CPU exact proof absent')
 require(reports['single_residual_jni1961'].get('cases',0)>=17 and reports['single_residual_jni1961'].get('binary64_preparation_preserved') is True,'Real Single residual JNI preparation/transaction absent')
 batch=reports['batch_engine1961'];require(batch.get('actualVariants')==[64,32,128] and batch.get('asyncBanks')==2 and batch.get('observedCompletionWaits',0)>0,'Batch/readmany/async/variant engine proof absent')
 geometry=reports['geometry_tiles1961'];require(geometry.get('exactPixels',0)>49000000 and geometry.get('geometryCases',0)>=32 and geometry.get('residentChainCases',0)>=9,'Full24.5MP tiled geometry and resident chain proof absent')
 strong=reports['strong_shared_cache1961'];require(strong.get('actual_mesa_gles_execution') is True and strong.get('cases',0)>=96 and strong.get('pixels_compared',0)>500000 and strong.get('layouts')==[32,64,128] and strong.get('immutable_bindings_verified') is True and strong.get('bounded_source_map_and_evidence_rows_verified') is True,'Shared-cache exact frozen60 GPU shader proof absent')
 certified=reports['strong_facade1961'];require(certified.get('controlledAdmissionMetadata') is True and certified.get('actualProductionGpuCommands') is True and certified.get('actualGpuDispatches',0)>0 and certified.get('twoBankCases',0)>=3 and certified.get('gpuPolicyCompareCases',0)>0 and certified.get('physicalSpeedClaim') is False,'Controlled certified production Strong GPU route proof absent')
 bg=reports['analysis_background1961'];require(bg.get('actualBackgroundDispatches') is True and bg.get('unqualifiedForegroundCpuOnly') is True and bg.get('detachedMutableSource') is True and bg.get('residentStageAdoption') is True and bg.get('transientBusyRetry') is True,'Actual background qualification/resident adoption safety gate absent')

 require(qa.get('intentional_quality_algorithm_change') is False and qa.get('host_pixel_equivalence_to_baseline') is True,'Preserve .60 pixel algorithms/settings')
 for key in ('original_apk_apply_tested','original_split_merge_tested','ci_android_apply_tested','device_tested','device_quality_verified','device_save_speed_measured','gpu_execution_on_physical_android','all_processing_on_gpu','gx9_beauty_interop_supported','gx10_encoder_interop_supported'):
  require(qa.get(key) is False,'Unperformed/unsupported capability claimed: '+key)
 for key in ('physical_android_tested','device_speedup_verified','device_quality_improvement_verified'):
  require(result.get(key) is False,'Unverified physical device result claimed: '+key)
 require(qa.get('replaced_helper_roots')==sorted(PRODUCTION) and json.loads((source/'production1961.json').read_text())==PRODUCTION,'Reviewed production helper inventory differs')
 require(qa.get('compiled_production_source_sha256')=={production_path(name).relative_to(ROOT).as_posix():sha(production_path(name).read_bytes()) for name in PRODUCTION} and qa.get('production_source_consistency_verified') is True,'Current production differs from MPP compiler input')
 for key,prefix in SOURCE_GROUPS.items():file_pins(source,qa.get(key),prefix)
 for key in ('compiled_sources_sha256','compiled_native_sources_sha256'):
  if key in result:file_pins(source,result[key])
 timing=(source/'ProcessingTiming1947.java').read_text().replace(VERSION,BASE_VERSION)
 hooks=[('        try { GpuQualification1961.initialize(context); } catch(Throwable optional) { }\n',''),('if(trace.shotId!=shotId){GpuQualification1961.captureChanged();captureEpoch1953++;}','if(trace.shotId!=shotId)captureEpoch1953++;'),('            GpuQualification1961.wake();\n','')]
 for added,old in hooks:
  require(timing.count(added)==1,'Exact reviewed timing qualification hook absent');timing=timing.replace(added,old)
 require(sha(timing.encode())=='1668fab5d6870f5957944e7506c199435a9356336a7223fb24e92283d3789a43','Timing stage core differs after exact lifecycle hook inverse')
 require(qa.get('timing_version_only_change') is False and qa.get('timing_new_idle_gpu_qualification_hooks') is True and qa.get('timing_stage_core_source_inverse_verified') is True,'Timing lifecycle/core facts differ')
 require(qa.get('capture_fusion_enabled') is False and qa.get('capture_begin_image_false_return_verified') is True and qa.get('capture_class_bytecode_identical') is True,'Single-image capture proof absent')
 return result

def validate(args):
 qa=json.loads((args.dist/QA_NAME).read_text());result=host_checks(qa,ROOT);inputs=args.input or args.build.parent/'input'
 for name,digest,size in ((BASE_SINGLE,BASE_SINGLE_SHA256,BASE_SINGLE_BYTES),(BASE_BUNDLE,BASE_BUNDLE_SHA256,BASE_BUNDLE_BYTES)):
  raw=(inputs/name).read_bytes();require(sha(raw)==digest and len(raw)==size,'Wrong pinned .60/.193 baseline')
 base,old_bundle=archive(inputs/BASE_SINGLE),archive(inputs/BASE_BUNDLE);current,combined=archive(args.dist/SINGLE),archive(args.dist/BUNDLE)
 for items,version in ((base,BASE_VERSION),(old_bundle,BASE_BUNDLE_VERSION),(current,VERSION),(combined,BUNDLE_VERSION)):
  require(headers(items['META-INF/MANIFEST.MF']).get('Version')==version,'MPP version differs')
  for name in ('ulike/runtime.dex','classes.dex'):dex_integrity(items[name],name)
 delta={}
 for kind,old,new in (('standalone',base,current),('bundle',old_bundle,combined)):
  changed={name for name in old.keys()&new.keys() if old[name]!=new[name]};added=set(new)-set(old)
  require(set(old)<=set(new) and changed==CHANGED and added==ADDED,'Unexpected resource delta: '+kind)
  require(qa.get('changed_'+kind+'_entries')==sorted(changed) and qa.get('added_'+kind+'_entries')==sorted(added),'QA resource inventory differs')
  delta[kind+'_changed'],delta[kind+'_added']=sorted(changed),sorted(added)
 require({n:b for n,b in base.items() if own(n)}=={n:b for n,b in old_bundle.items() if own(n)} and {n:b for n,b in current.items() if own(n)}=={n:b for n,b in combined.items() if own(n)},'Standalone/bundle ULike differ')
 require(all(combined[n]==b for n,b in old_bundle.items() if not own(n) and n not in ('classes.dex','META-INF/MANIFEST.MF')),'Other application resource changed')
 inherited={n:{'sha256':sha(b),'bytes':len(b)} for n,b in base.items() if (n.endswith('.so') or n=='ulike186/runtime/0000.bin') and n!=GPU_ENTRY}
 require(len(inherited)==11 and inherited==qa.get('inherited_native_payloads') and all(current[n]==base[n] for n in inherited),'Inherited native library changed')
 require(all(current[n]==base[n] for n in ('ulike/methods.dex','ulike/methods.tsv')),'Legacy native methods changed')
 gpu=current[GPU_ENTRY];require(gpu[:6]==b'\x7fELF\x02\x01' and gpu[18:20]==b'\xb7\x00' and sha(gpu)==qa.get('gpu_native_library_sha256') and len(gpu)==qa.get('gpu_native_library_bytes'),'GPU native payload invalid/changed')
 native=qa.get('gpu_native_build',{})
 require(native.get('ndk_revision')=='27.2.12479018' and native.get('native_abi')==19601 and native.get('abi')=='arm64-v8a' and native.get('min_sdk')==26 and native.get('library')=='libulike_gpu1960.so' and native.get('physical_android_tested') is False and native.get('sha256')==sha(gpu) and native.get('bytes')==len(gpu),'GPU build provenance differs');file_pins(ROOT,native.get('sources'),'native1960')
 require(len(qa.get('gpu_native_jni_exports',[]))==20 and qa.get('gpu_native_load_segment_alignments') and all(a>=16384 for a in qa['gpu_native_load_segment_alignments']) and set(qa.get('gpu_native_needed_libraries',[]))<={'libEGL.so','libGLESv3.so','libc.so','libm.so','libdl.so'},'GPU JNI/alignment/dependencies differ')
 artifacts={n:{'sha256':sha((args.dist/n).read_bytes()),'bytes':(args.dist/n).stat().st_size} for n in (SINGLE,BUNDLE)};require(qa.get('artifacts')==artifacts,'QA MPP fingerprints differ')
 inventory=json.loads((args.build/'emitted/gpu-inventory1961.json').read_text());require(all(qa.get(k)==v for k,v in inventory.items()),'Serialized DEX inventory differs')
 require(all(v in (args.build/'emitted.log').read_text() for v in ('begin_image_false_return_verified=true','serialized_dex_verified=true','unrelated_runtime_preserved=true','installer_old_rows_preserved=11')),'Fresh serialized/inverse audit missing')
 for path,entry in (('runtime.dex','ulike/runtime.dex'),('loader.dex','classes.dex'),('UlikeHqMaxPatch.class','app/hiro/ulike/patches/UlikeHqMaxPatch.class'),('IntegrationPayload186.class','app/hiro/ulike/patches/IntegrationPayload186.class')):
  require((args.build/'emitted'/path).read_bytes()==current[entry],'Audited bytes differ from package')
 require((args.build/'emitted/bundle-loader.dex').read_bytes()==combined['classes.dex'],'Audited bundle loader differs')
 evidence={'schema':'ulike1961-desktop-validation-v1','status':'passed','ulike_version':VERSION,'bundle_version':BUNDLE_VERSION,'selected_candidates':SELECTED,'artifacts':artifacts,'resource_delta':delta,'source_consistency_verified':True,'host_quality_result_sha256':qa['host_quality_result_sha256'],'host_assertions':result['assertions'],'host_reports':result.get('reports',{}),'compiled_production_source_sha256':qa['compiled_production_source_sha256'],'source_groups':{k:qa[k] for k in SOURCE_GROUPS},'inherited_native_payloads':inherited,'gpu_native_payload':{'sha256':sha(gpu),'bytes':len(gpu)},'serialized_dex_preservation_verified':True,'single_image_capture_admission_verified':True,'intentional_quality_algorithm_change':False,'original_apk_apply_tested':False,'original_split_merge_tested':False,'device_tested':False,'device_quality_verified':False,'device_save_speed_measured':False,'original_apk_application':{'status':'not_tested','reason':'Original APKS unavailable; package and host validation only.'}}
 args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_bytes(json_bytes(evidence));print('PASS fresh GPU/source/package evidence; original APKS and physical Android untested');return evidence
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for n in ('build','dist','output'):p.add_argument('--'+n,type=Path,required=True)
 p.add_argument('--input',type=Path);a=p.parse_args()
 for n,v in vars(a).items():
  if v is not None:setattr(a,n,v.resolve())
 validate(a)
