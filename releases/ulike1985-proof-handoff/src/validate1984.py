#!/usr/bin/env python3
"""Validate .84 qualification repairs, exact native inheritance and tested original-APKS identity."""
from pathlib import Path, PurePosixPath
import argparse
import json
import application_evidence1984
from build1984 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    BASE_SINGLE, BASE_BUNDLE, BASE_SINGLE_SHA256, BASE_BUNDLE_SHA256, BASE_SINGLE_BYTES,
    BASE_BUNDLE_BYTES, SINGLE, BUNDLE, SELECTED, PRODUCTION, CHANGED, ADDED, INSTALLER, LOADER,
    require, sha, archive, headers, own, dex_integrity, json_bytes, production_path,
    source_pins, publication_pins, inventory_checks, GPU_ENTRY, NATIVE_ENTRIES, NEW_JNI, HOST_FLAGS,
    QUALIFICATION_PROOF_SCHEMA1977, QUALIFICATION_PREFERRED_SCHEMA1977, QUALIFICATION_EXACT_SCHEMA1977,
    INHERITED_NUMERICAL_VALIDATION1978, ROUTE_POLICY1981)

QA_NAME='QA_ULike_v1.9.84.json'
SOURCE_GROUPS={'compiled_production_source_sha256':'','compiled_compile_only_source_sha256':'',
               'compiled_transformer_source_sha256':'','executed_host_source_sha256':'',
               'inherited_gpu_native_source_sha256':'native1960'}

def file_pins(source,pins,prefix=''):
    require(isinstance(pins,dict) and bool(pins),'Missing compiler/execution source pins')
    for name,digest in pins.items():
        path=PurePosixPath(name)
        require(not path.is_absolute() and '..' not in path.parts and '\\' not in name,'Unsafe source pin path')
        target=source/prefix/name
        require(target.is_file() and sha(target.read_bytes())==digest,'Source changed after compilation/test: '+name)
    return pins

def host_checks(qa,source):
    require(qa.get('schema')=='ulike1984-qualification-v1'
            and qa.get('status')=='MPP_GPU_QUALIFICATION84_HOST_AND_ORIGINAL_APKS_VERIFIED_DEVICE_UNVERIFIED'
            and qa.get('ulike_version')==VERSION and qa.get('bundle_version')==BUNDLE_VERSION
            and qa.get('baseline_ulike_version')==BASE_VERSION and qa.get('baseline_bundle_version')==BASE_BUNDLE_VERSION
            and qa.get('selected_candidates')==SELECTED,'Wrong .84 repair identity/scope')
    inventory_checks(qa)
    result=qa.get('strong_gpu_host_evidence',{})
    require(qa.get('host_quality_passed') is True and result.get('status')=='passed'
            and type(result.get('assertions')) is int and result['assertions']>0,'Fresh executed repair tests required')
    require(sha(json.dumps(result,sort_keys=True).encode())==qa.get('strong_gpu_host_evidence_sha256'),'Current evidence digest differs')
    require(result.get('physical_android_tested') is False,'Physical Android untested disclosure required')
    for key,expected in (('qualification_proof_schema1977',QUALIFICATION_PROOF_SCHEMA1977),
                         ('qualification_preferred_schema1977',QUALIFICATION_PREFERRED_SCHEMA1977),
                         ('exact_schema1977',QUALIFICATION_EXACT_SCHEMA1977)):
        require(qa.get(key)==expected and result.get(key)==expected,'Certificate schema changed: '+key)
    for key in HOST_FLAGS:
        require(result.get(key) is True and qa.get(key) is True,'Current regression missing: '+key)
    contract=json.loads((source/'qa_contract1984.json').read_text())
    reports=result.get('tests',{})
    require(set(reports)=={label for label,_ in contract['runners']},'Executed group inventory differs')
    for label,report in reports.items():
        require(report.get('status')=='passed' and type(report.get('assertions')) is int
                and report['assertions']>0 and report.get('physical_android_tested') is False,'Incomplete test group: '+label)
    require(sum(report['assertions'] for report in reports.values())==result['assertions'],'Current assertion total differs')
    for label,flags in contract['requirements'].items():
        parts=label.split('.')
        evidence=reports[parts[0]]
        for part in parts[1:]:
            evidence=evidence.get(part,{})
        require(evidence.get('status')=='passed','Nested evidence missing: '+label)
        for flag in flags:
            require(evidence.get(flag) is True,'Mandatory nested regression missing: '+label+'/'+flag)
    require(qa.get('production_source_consistency_verified') is True
            and qa.get('compiled_production_source_sha256')=={production_path(n).relative_to(ROOT).as_posix():sha(production_path(n).read_bytes()) for n in PRODUCTION},'Compiled production input differs')
    for key,prefix in SOURCE_GROUPS.items():
        file_pins(source,qa.get(key),prefix)
    require(qa.get('executed_host_source_sha256')==source_pins(),'Complete tested source graph differs')
    for key in ('persistent_rolling_across_restarts','anomaly_snapshots_protected','user_incident_snapshot',
                'logging_async_bounded','camera_trace_log_data_excluded','camera_trace_logging_methods_preserved',
                'inherited_native_libraries_byte_identical','native_library_byte_identical','native_installer_byte_identical',
                'native_installer_inverse_verified','native_installer_existing_rows_preserved','native_installer_existing_row_count_preserved',
                'non_ulike_loader_classes_unchanged','non_ulike_resources_byte_identical','standalone_and_bundle_ulike_resources_identical',
                'resolution_and_save_format_preserved','save_publication_hooks_preserved','save_publication_contract_preserved',
                'save_format_and_codec_configuration_preserved','unmodified_pixel_kernels_identical_to_baseline',
                
                'unmodified_native_shader_sources_byte_identical','reviewed_native_scope_verified',
                'stage_timing_algorithm_preserved','unmodified_gpu_runtime_helpers_bytecode_identical',
                'camera_control_lifecycle_bytecode_identical','preview_output_observer_bytecode_identical','save_encoding_helpers_byte_identical','nonreviewed_runtime_methods_byte_identical1984','inherited_save_hook_contracts_preserved1982','save_helpers_byte_identical_to_baseline83','fresh_diagnostic_regressions_in_this_release','gpu_safety_gates_preserved','runtime_all_helper_references_verified','fresh_whole_app_audit_in_this_release','timing_camera_trace_hooks_preserved'):
        require(qa.get(key) is True,'Preservation proof absent: '+key)
    require(qa.get('camera_trace_version_only_preserved') is True and qa.get('timing_version_only_preserved') is False and qa.get('diagnostic_instrumentation_added1982') is True,'Diagnostic source changes must be disclosed')
    require(qa.get('native_installer_preserved_rows')==12 and qa.get('native_installer_total_rows')==12
            and qa.get('native_installer_updated_rows')==0 and qa.get('native_installer_appended_rows')==0,'Installer row inventory differs')
    require(qa.get('diagnostic_stage_names')==['fusion','noise','correction','compression','save'],'Diagnostic stages changed')
    for key in ('changed_native_methods','new_native_methods','new_jni_methods'):
        require(qa.get(key)==[],'No native/JNI change is permitted: '+key)
    for key in ('intentional_quality_algorithm_change','capture_fusion_enabled','gpu_arithmetic_implementation_changed',
                'ci_android_apply_tested','device_tested',
                'device_quality_verified','device_save_speed_measured','camera_visible_preview_verified_on_device',
                'gpu_execution_on_physical_android','all_processing_on_gpu','gx9_beauty_interop_supported',
                'gx10_encoder_interop_supported','unmodified_gpu_shader_and_cpu_sources_byte_identical','unmodified_cpu_and_other_shader_sources','diagnostic_measurement_and_logger_bodies_preserved','save_hooks1981_reapplied'):
        require(qa.get(key) is False,'Unperformed capability claimed: '+key)
    require(qa.get('inherited_host_validation_source_version')==BASE_VERSION
            and qa.get('inherited_gpu_validation',{}).get('fresh_gpu_execution_in_this_release') is False,'Inherited evidence presented as new execution')
    require(qa.get('inherited_numerical_validation1978')==INHERITED_NUMERICAL_VALIDATION1978,'Inherited .78 numerical evidence provenance differs')
    for key, value in ROUTE_POLICY1981.items():
        require(type(qa.get(key)) is type(value) and qa[key] == value, 'Current route policy differs: ' + key)
    require(qa.get('strong_new_gpu_program_ids')==[] and qa.get('legacy_gpu_program_ids')==list(range(51)), 'Program scope differs')
    require(qa.get('rebuilt_native_payloads')=={} and NATIVE_ENTRIES==set() and NEW_JNI==[],'Unexpected native rebuild')
    require(qa.get('gpu_native_build',{}).get('build_action')=='inherited_byte_identical_from_1.9.83','Shipped GPU provenance differs')
    scope=qa.get('source_scope_evidence',{})
    require(scope.get('status')=='passed' and sha(json.dumps(scope,sort_keys=True).encode())==qa.get('source_scope_evidence_sha256'),'Source scope report differs')
    declaration=json.loads((source.parent/'manifest.json').read_text())
    require(declaration.get('reviewed_source_sha256')==source_pins()
            and declaration.get('reviewed_publication_sha256')==publication_pins(),'Reviewed graph changed after build')
    require(qa.get('original_apk_apply_tested') is True and qa.get('original_split_merge_tested') is True, 'Original APKS apply missing')
    require(qa.get('original_application') == application_evidence1984.read(source, qa['artifacts']), 'Original application evidence differs')
    return result

def validate(args):
    qa=json.loads((args.dist/QA_NAME).read_text())
    result=host_checks(qa,ROOT)
    inputs=args.input or args.build.parent/'input'
    for name,digest,size in ((BASE_SINGLE,BASE_SINGLE_SHA256,BASE_SINGLE_BYTES),(BASE_BUNDLE,BASE_BUNDLE_SHA256,BASE_BUNDLE_BYTES)):
        raw=(inputs/name).read_bytes()
        require(sha(raw)==digest and len(raw)==size,'Wrong pinned .83/.216 baseline')
    base,old_bundle=archive(inputs/BASE_SINGLE),archive(inputs/BASE_BUNDLE)
    current,combined=archive(args.dist/SINGLE),archive(args.dist/BUNDLE)
    for items,version in ((base,BASE_VERSION),(old_bundle,BASE_BUNDLE_VERSION),(current,VERSION),(combined,BUNDLE_VERSION)):
        require(headers(items['META-INF/MANIFEST.MF']).get('Version')==version,'MPP version differs')
        for name in ('ulike/runtime.dex','classes.dex'):
            dex_integrity(items[name],name)
    delta={}
    for kind,old,new in (('standalone',base,current),('bundle',old_bundle,combined)):
        changed={n for n in old.keys()&new.keys() if old[n]!=new[n]}
        added=set(new)-set(old)
        require(set(old)<=set(new) and changed==CHANGED and added==ADDED,'Unexpected '+kind+' resource delta')
        require(qa.get('changed_'+kind+'_entries')==sorted(changed) and qa.get('added_'+kind+'_entries')==sorted(added),'QA resource inventory differs')
        delta[kind+'_changed'],delta[kind+'_added']=sorted(changed),sorted(added)
    require({n:b for n,b in base.items() if own(n)}=={n:b for n,b in old_bundle.items() if own(n)}
            and {n:b for n,b in current.items() if own(n)}=={n:b for n,b in combined.items() if own(n)},'Standalone/bundle ULike copies differ')
    require(all(combined[n]==b for n,b in old_bundle.items() if not own(n) and n not in ('classes.dex','META-INF/MANIFEST.MF')),'Other app resource changed')
    inherited={n:{'sha256':sha(b),'bytes':len(b)} for n,b in base.items() if n.endswith('.so') or n=='ulike186/runtime/0000.bin'}
    require(len(inherited)==12 and inherited==qa.get('inherited_native_payloads')
            and all(current[n]==base[n] for n in inherited),'One of twelve native payloads changed')
    require(all(current[n]==base[n] for n in ('ulike/methods.dex','ulike/methods.tsv',INSTALLER)),'Installer or native method resources changed')
    gpu=current[GPU_ENTRY]
    require(gpu==base[GPU_ENTRY] and sha(gpu)==qa.get('gpu_native_library_sha256') and len(gpu)==qa.get('gpu_native_library_bytes'),'Retained GPU/certificate identity differs')
    require('PASS all 12 native payloads' in (args.build/'native-installer-metadata.log').read_text(),'Native identity proof missing')
    artifacts={name:{'sha256':sha((args.dist/name).read_bytes()),'bytes':(args.dist/name).stat().st_size} for name in (SINGLE,BUNDLE)}
    require(qa.get('artifacts')==artifacts,'QA MPP fingerprints differ')
    inventory=json.loads((args.build/'emitted/whole-audit-inventory1984.json').read_text())
    inventory_checks(inventory)
    require(all(qa.get(key)==value for key,value in inventory.items()),'Serialized DEX inventory differs')
    for name,entry in (('runtime.dex','ulike/runtime.dex'),('loader.dex','classes.dex'),('UlikeHqMaxPatch.class',LOADER)):
        require((args.build/'emitted'/name).read_bytes()==current[entry],'MPP differs from audited transformed bytes: '+entry)
    require((args.build/'emitted/bundle-loader.dex').read_bytes()==combined['classes.dex'],'Bundle loader differs')
    require((args.build/'helper-references.txt').read_bytes().startswith(b'PASS helper references in '),'Fresh helper linkage proof absent')
    require('exact executable loader implementation preserved' in (args.build/'metadata.log').read_text(),'Loader metadata inverse proof absent')
    evidence={'schema':'ulike1984-desktop-validation-v1','status':'passed','ulike_version':VERSION,'bundle_version':BUNDLE_VERSION,
              'selected_candidates':SELECTED,'artifacts':artifacts,'resource_delta':delta,'source_consistency_verified':True,
              'strong_gpu_host_evidence_sha256':qa['strong_gpu_host_evidence_sha256'],'host_assertions':result['assertions'],
              'host_reports':result['tests'],'source_groups':{key:qa[key] for key in SOURCE_GROUPS},
              'inherited_native_payloads':inherited,'native_installer_byte_identical':True,'native_installer_inverse_verified':True,
              'native_methods_byte_identical':True,'native_methods_tsv_byte_identical':True,
              'serialized_dex_preservation_verified':True,'single_image_capture_admission_verified':True,
              'unmodified_pixel_kernel_helpers_bytecode_identical':True,'intentional_quality_algorithm_change':False,
              'original_apk_apply_tested':True,'original_split_merge_tested':True,'device_tested':False,
              'device_quality_verified':False,'device_save_speed_measured':False,'camera_visible_preview_verified_on_device':False,
              'original_apk_application':qa['original_application']}
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_bytes(json_bytes(evidence))
    print('PASS .84 current qualification retention, retry, progress, diagnostics and retained route tests, exact original-APKS application, all twelve native payloads and installer identity; physical Android untested')
    return evidence

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ('build','dist','output'):
        parser.add_argument('--'+name,type=Path,required=True)
    parser.add_argument('--input',type=Path)
    args=parser.parse_args()
    for name,value in vars(args).items():
        if isinstance(value,Path):setattr(args,name,value.resolve())
    validate(args)
