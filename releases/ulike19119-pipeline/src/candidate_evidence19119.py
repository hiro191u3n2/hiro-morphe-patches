"""Strict executed-evidence contract for the independent .119 candidates."""
from pathlib import Path
import hashlib
GROUPS={'noise_arithmetic119','noise_integration119'}

def positive(tests,source,require):
 source=Path(source)
 def check(r,k,expected):require(type(r.get(k)) is type(expected) and r[k]==expected,'Candidate evidence differs: '+k)
 def minimum(r,k,n):require(type(r.get(k)) is int and r[k]>=n,'Candidate coverage missing: '+k)
 def bound(pins):
  require(type(pins) is dict and len(pins)>0,'Candidate source map absent')
  for name,value in pins.items():
   p=source/name;require(p.is_file() and p.resolve().is_relative_to(source.resolve()) and isinstance(value,str) and hashlib.sha256(p.read_bytes()).hexdigest()==value,'Candidate source mismatch: '+name)
 def flags(r,*keys):
  for k in keys:check(r,k,True)
 def scoped(r):
  check(r,'status','passed');minimum(r,'assertions',1)
  check(r,'physical_android_tested',False);check(r,'device_speedup_verified',False)
 a=tests['noise_arithmetic119'];i=tests['noise_integration119']
 scoped(a);scoped(i);check(a,'schema','ulike19119-noise-arithmetic-host-v1');check(i,'schema','ulike19119-noise-integration-host-v1')
 flags(a,'actual_packaged_candidate_sources');check(a,'positive_controls',54);check(a,'negative_controls',7);bound(a['source_sha256'])
 require(set(a['tests'])=={'strong','front','division'},'Arithmetic subgroups changed')
 s=a['tests']['strong'];f=a['tests']['front'];d=a['tests']['division']
 scoped(s);scoped(f);scoped(d)
 check(s,'cases',50);check(s,'pixels_compared',183189);check(s,'confidence_words_compared',7814)
 flags(s,'actual_full_shader_gles','actual_program48_macros_verified','unchanged_shader_regions_verified')
 minimum(s,'nonzero_output_changes',1001);minimum(s,'nonzero_confidence',1);minimum(s,'split_dispatches',1)
 require(set(s['negative_controls'])=={'pixel_xor','confidence_xor','quotient_weight_zero','fast_divider_wrong_return'},'Arithmetic negative controls absent')
 for row in s['negative_controls'].values():flags(row,'detected');require(row['argb_mismatches']+row['confidence_mismatches']>0,'Arithmetic mutant not observed')
 flags(f,'actual_candidate_wrapper_executed','actual_native_jni_entry','actual_arm64_execution','host_jni_adapter');check(f,'negative_controls_detected',2);bound(f['identity_source_sha256'])
 require(set(f['architectures'])=={'host','aarch64'},'Real ARM64 execution missing')
 for arch,row in f['architectures'].items():
  for corpus in ('broad','targeted'):
   check(row['candidate'][corpus],'sha256',row['baseline'][corpus]['sha256']);check(row['candidate'][corpus],'bytes',row['baseline'][corpus]['bytes'])
  broad=row['candidate']['broad']['result'];target=row['candidate']['targeted']['result']
  for k,v in [('pixel_cases',320),('pixels',522120),('transform_cases',8192),('model_cases',32768)]:check(broad,k,v)
  check(target,'cases',640);check(target,'pixels',3069312);minimum(target,'skipped_blocks',1)
  require(row['mutant']['targeted']['sha256']!=row['baseline']['targeted']['sha256'],'Wrong skip mutant escaped '+arch)
 minimum(d,'input_pairs',115420);minimum(d,'glsl_bit_comparisons',2077560);minimum(d,'rounding_mutant_differences',1);flags(d,'strict_java_oracle','independent_fraction_oracle')
 flags(i,'current_production_java_executed','baseline_native_with_current_wrappers','source_stability_verified');bound(i['current_source_sha256'])
 rows=i['tests'];required={'strong_transport','model_parallel','strong_adaptive','memory','front_gate','capture_timing','gpu_owner','gpu_foreground','gpu_workload','gpu_gate'}
 require(set(rows)==required,'Current integration subgroups missing')
 for name,row in rows.items():check(row,'status','passed');minimum(row,'assertions',1)
 t=rows['strong_transport'];flags(t,'actual_jni','actual_native_cpu');check(t['direct_transport'],'direct_cases',136);check(t['post_read_faults'],'fault_modes',5);bound(t['source_sha256'])
 m=rows['model_parallel'];flags(m,'actual_jni','actual_native_cpu','whole_model_bitwise_equal','model_ticket_joined_before_handoff','callback_once_and_fallback_verified');check(m['tests'][1],'model_cases',108);check(m['tests'][2],'fault_modes',7);bound(m['source_sha256'])
 ad=rows['strong_adaptive'];flags(ad,'existing_profile7_preserved','profile6_bit_exact','profile6_independent_certificates','one_heavy_candidate_per_capture');check(ad,'profile6_cases',70);bound(ad['source_sha256'])
 mem=rows['memory'];flags(mem,'actual_production_uploader_executed','allocation_bound_245mp_at_32m_heap','opacity_failures_and_cancellation');minimum(mem,'all_pixels_verified',49000001)
 front=rows['front_gate'];flags(front,'native_jni_executed','production_hook_inverse_verified','scheduler_is_host_adapter','speed_is_controlled_gate_test_not_benchmark');check(front,'positive_controls',10);check(front,'negative_controls',16);minimum(front,'assertions',1055)
 bound({'CpuFrontOptimized19119.java':front['candidate_java_sha256'],'native19119/front/front19119.c':front['candidate_wrapper_sha256'],'native19119/front/front_core19119.c':front['candidate_core_sha256']})
 minimum(rows['capture_timing'],'assertions',12)
 owner=rows['gpu_owner'];flags(owner,'actual_jni_gles','independent_native_contexts','global_second_open_refused','per_engine_capacity_and_failure_routing','combined_retained_counted','trim_both_engines','wrong_owner_negative_control_detected');check(owner,'negative_controls',3);minimum(owner,'positive_assertions',233);bound(owner['source_sha256'])
 stage=rows['gpu_foreground'];flags(stage,'actual_foreground_begin_process_end','actual_jni_gles','actual_native_cpu','test_only_certificate_preseed','certificate_evidence_is_test_only');check(stage,'direct_cases',8);check(stage,'candidate_cases',4);minimum(stage,'compared_pixels',800001);check(stage,'negative_controls',2);bound(stage['source_sha256'])
 workload=rows['gpu_workload'];flags(workload,'actual_jni_gles','actual_native_cpu','all_origins_and_tails','two_mutable_banks','one_candidate_model_upload','closed_and_refused_no_speed_evidence','benchmark_scope_is_actual_full_photo','two_submitted_ticket_cancel');check(workload,'runtime_certificates_written',False);check(workload,'complete_workloads',4);check(workload,'candidate_workloads',2);check(workload,'compared_pixels',49939200);check(workload,'cancellation_controls',4);check(workload,'quality_negative_controls',4);bound(workload['source_sha256'])
 gate=rows['gpu_gate'];check(gate,'positive_controls',10);check(gate,'negative_controls',18);minimum(gate,'assertions',28);bound(gate['sources']);bound({'GpuStrongOptimized19119.java':gate['candidate_helper_sha256']})
 require(a['assertions']==sum(r['assertions'] for r in a['tests'].values())+1,'Arithmetic assertion sum differs')
 require(i['assertions']==sum(r['assertions'] for r in rows.values()),'Integration assertion sum differs')

def negative_controls(tests,source):
 """Reject deliberately incomplete/mislabeled reports at their intended guard."""
 from copy import deepcopy
 paths=[('noise_arithmetic119','actual_packaged_candidate_sources'),('noise_arithmetic119','negative_controls'),
  ('noise_arithmetic119','tests','strong','actual_full_shader_gles'),('noise_arithmetic119','tests','strong','cases'),
  ('noise_arithmetic119','tests','front','actual_arm64_execution'),('noise_arithmetic119','tests','front','negative_controls_detected'),
  ('noise_arithmetic119','tests','division','strict_java_oracle'),('noise_arithmetic119','tests','division','rounding_mutant_differences'),
  ('noise_integration119','current_production_java_executed'),('noise_integration119','tests','strong_transport','actual_jni'),
  ('noise_integration119','tests','model_parallel','model_ticket_joined_before_handoff'),('noise_integration119','tests','front_gate','production_hook_inverse_verified'),
  ('noise_integration119','tests','gpu_owner','combined_retained_counted'),('noise_integration119','tests','gpu_owner','negative_controls'),
  ('noise_integration119','tests','gpu_gate','negative_controls'),('noise_integration119','tests','capture_timing','assertions')]
 def need(ok,message):
  if not ok:raise AssertionError(message)
 for path in paths:
  bad=deepcopy(tests);row=bad
  for key in path[:-1]:row=row[key]
  row[path[-1]]=False if type(row[path[-1]]) is bool else 0
  try:positive(bad,source,need)
  except AssertionError:continue
  raise AssertionError('Incomplete candidate evidence admitted: '+str(path))
 return len(paths)
