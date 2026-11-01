#!/usr/bin/env python3
"""Executed H12-H15 exact final-stage routes and preserved .51 save contracts.

Reports preserve explicit host-vs-physical-Android scope. GPU output is compared
against independent pre-GPU Java pixels and actual whole CPU bitmap finishing;
this report never claims device speed, scheduler behavior or camera verification.
"""
from pathlib import Path
import hashlib,importlib.util,json,os,sys

BASELINE_SHA256='8b96457398e0a16d66965dbfb38306ba601bf43a7a02b1cdbabcc3ce8ae60bef'
H50_SHA256='ca872a0b6df5f1ca5a949eac0f23d6cba97c7ce662ec7257f4445ba39302a490'

def load(root,name):
 spec=importlib.util.spec_from_file_location('ulike1952_'+name,Path(root)/(name+'.py'))
 m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def require(value,message):
 if not value:raise AssertionError(message)

def checked(value,name):
 require(value.get('status')=='passed' and type(value.get('assertions')) is int and value['assertions']>0,'Actual passing assertions missing: '+name)
 return value

def test(root,work,android,baseline):
 root=Path(root).resolve();work=Path(work).resolve()/'host-regression1952';work.mkdir(parents=True,exist_ok=True)
 android,baseline=Path(android),Path(baseline)
 require(hashlib.sha256(baseline.read_bytes()).hexdigest()==BASELINE_SHA256,'Published .51 baseline MPP changed')
 h50=Path(os.environ.get('ULIKE_H50_BASELINE',''))
 require(h50.is_file() and hashlib.sha256(h50.read_bytes()).hexdigest()==H50_SHA256,'Pinned .50 DEX oracle required for inherited H8 negatives')
 # Inherited runners import actual new optional helpers through this root.
 if str(root) not in sys.path:sys.path.insert(0,str(root))
 original=load(root,'host_regression1951').test(root,work,android,h50)
 suites=dict(original['suites'])
 gpu=load(root,'host_finishgpu1952').test(root,work,jdk=os.environ.get('ULIKE_JDK_HOME'),ndk=os.environ.get('ULIKE_NDK_HOME'))
 require(gpu.get('status')=='passed' and gpu.get('cases',0)>=600 and gpu.get('pixels_compared',0)>4000000,'Actual GLES/JNI oracle coverage missing')
 for key in ('mesa_gles_shader_executed','production_jni_executed','gpu_chain_pixel_exact',
             'java_float_and_mask_policy_exact','candidate_failure_destination_unchanged',
             'interrupted_candidate_destination_unchanged','partial_destination_rows_untouched',
             'source_immutable','one_final_readback_no_intermediate_cpu_wait',
             'concurrent_capture_workspace_exact'):
  require(gpu.get(key) is True,'H12 executed evidence missing: '+key)
 for key in ('sequential_corrected_neighborhood_pixel_exact','dependency_36_row_halo_pixel_exact','dependency_complete_tiles_executed'):
  require(gpu.get(key) is True,'H13 executed evidence missing: '+key)
 # Distinct oracle cases allocate assertion counts between the combined and
 # corrected-neighbor suites once. Full pixel totals remain in the raw report.
 seq=gpu['sequential_chain_cases'];combined=gpu['cases']-seq
 require(seq>0 and combined>0,'Distinct combined/sequential native comparisons missing')
 suites['h12_finish_gpu_exact']={**gpu,'assertions':combined,
  'distinct_oracle_cases_counted':combined,'assertion_count_basis':'Passing non-sequential production GLES/JNI oracle cases; excludes H13 sequence cases.',
  'gpu_execution_on_physical_android':False}
 suites['h13_dependency_tiling']={key:gpu[key] for key in (
  'status','sequential_corrected_neighborhood_pixel_exact','dependency_36_row_halo_pixel_exact',
  'dependency_complete_tiles_executed','one_final_readback_no_intermediate_cpu_wait',
  'partial_destination_rows_untouched','source_immutable','sequential_chain_cases')}
 suites['h13_dependency_tiling'].update(assertions=seq,distinct_oracle_cases_counted=seq,
  assertion_count_basis='Passing sequential corrected-neighbor production GLES/JNI oracle cases; excludes H12 combined cases.',
  gpu_execution_on_physical_android=False)
 suites['h14_whole_route']=checked(load(root,'host_route1952').test(root,work),'H14 whole route')
 suites['h15_performance_hints']=checked(load(root,'host_hints1952').test(root,work,android),'H15 public hint lifecycle')
 pipeline=checked(load(root,'host_pipeline1952').test(root,work,android,
  gpu_library=work/'host-finishgpu1952/libulike_finish1952.so'),'Actual .51/.52 bitmap route and worker integration')
 require(pipeline.get('same_output_pixels') is True and pipeline.get('gpu_bitmap_integration',{}).get('actual_bitmap_gpu_candidate_executed') is True,'Complete saved bitmap equality/ownership evidence missing')
 suites['published1951_pipeline_preservation']=pipeline
 result={**original,'status':'passed','assertions':sum(value['assertions'] for value in suites.values()),'suites':suites,
  'h12_h15_selected':['H12','H13','H14','H15'],'selected_candidates':['H12','H13','H14','H15'],
  'baseline_version':'1.9.51','baseline_mpp_sha256':BASELINE_SHA256,
  'pixel_equivalence_to_baseline':True,'full_resolution_nv21_checked':'4080x3060',
  'gpu_shader_execution_on_host':True,'gpu_execution_on_physical_android':False,
  'device_tested':False,'performance_measured_on_device':False,
  'full_float_beauty_chain_gpu_ported':False,
  'h12_h15_scope':'Exact eligible final integer moire/sharpening chain, 36-row dependency tiles, whole final-stage route choice, optional Android worker hints.',
  'device_speedup_verified':False}
 (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+'\n')
 return result
