#!/usr/bin/env python3
"""Execute current Strong admission/latency, CPU arithmetic, JNI and camera tests.

Controlled Java tests verify failure behavior. Production JNI and software GLES
execute actual shaders. ARM64 arithmetic proofs execute under qemu; no result
establishes physical Android performance or a Galaxy failure cause.
"""
from pathlib import Path
import importlib.util
ROOT = Path(__file__).resolve().parent

def load(name):
    spec=importlib.util.spec_from_file_location(name+'_aggregate1975',ROOT/(name+'.py'))
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve()
    runners=[('strong','tests1975/host_strong1975'),('certificate','host_certificate1973'),
             ('tuning','host_tuning1975'),('memory','host_memory1975'),('timing','host_timing1975'),
             ('native_facade','host_native_facade1975'),('precision','host_precision1973'),
             ('camera','host_camera1973')]
    reports={}
    for label,name in runners:
        arguments={'jdk':jdk}
        if label in ('memory','native_facade','precision','camera'):arguments['ndk']=ndk
        report=load(name).test(source,work/label,**arguments)
        if not isinstance(report,dict) or report.get('status')!='passed' or type(report.get('assertions')) is not int or report['assertions']<=0:
            raise AssertionError('Fresh executed positive report absent: '+label)
        if report.get('physical_android_tested') is not False:
            raise AssertionError('Physical Android untested disclosure absent: '+label)
        reports[label]=report
    requirements={
        'strong':['strong_latency_controls_regressions_passed','strong_latency_regressions_passed','latency_budget_regressions_passed','cold_proof_bounds_passed','strong_singleflight_regressions_passed','strong_first_mismatch_identity_regressions_passed','gpu_safety_gates_preserved','strong_preflight1974_regressions_passed','strong_preflight1975_regressions_passed','strong_overlap1975_regressions_passed','strong_upload_reuse1975_regressions_passed','strong_buffer_reuse1975_regressions_passed'],
        'certificate':['qualification_certificate_regressions_passed','gpu_safety_gates_preserved'],
        'tuning':['tuning_full_exact_gate_verified', 'tuning_snapshot_ownership_and_cancel_verified', 'scalar_only_tuning_persistence_verified'],
        'memory':['native_memory_admission_regressions_passed','native_failure_diagnostics_regressions_passed','gpu_safety_gates_preserved','materialized_java_readback_counted_once','remaining_java_allocation_stays_reserved','reusable_multi_output_readback_verified','private_partial_output_never_committed','overlap_scratch_admission_serial_fallback_verified','multi_output_cancel_and_quarantine_verified'],
        'timing':['backend_diagnostics_regressions_passed','backend_visibility_regressions_passed','strong_preference_telemetry_regressions_passed','strong_failure_diagnostics_regressions_passed','strong_work_latency_regressions_passed','strong_full_mismatch_regressions_passed'],
        'native_facade':['native_facade_regressions_passed','gpu_shader_execution_on_host','strong_overlap_actual_jni1975','strong_overlap_failure_drain1975','strong_serial_fallback_actual_jni1975','strong_repeat_no_large_upload1975','strong_reusable_readback_actual_jni1975'],
        'precision':['exact_division_regressions_passed','legacy_program_semantics_preserved','gpu_safety_gates_preserved'],
        'camera':['latest_camera_layout_regressions_passed','fresh_camera_host_execution_in_this_release'],
    }
    for label,flags in requirements.items():
        for flag in flags:
            if reports[label].get(flag) is not True:raise AssertionError('Fresh current regression absent: '+label+'/'+flag)
    result={'status':'passed','assertions':sum(report['assertions'] for report in reports.values()),
            'tests':reports,'physical_android_tested':False,'device_speedup_verified':False,
            'samsung_gpu_failure_root_cause_confirmed':False,'fresh_gpu_execution_in_this_release':True,
            'gpu_execution_on_physical_android':False}
    for flags in requirements.values():
        for flag in flags:result[flag]=True
    # These broad flags summarize three separately executed evidence groups,
    # rather than labelling superseded .71 control expectations as fresh tests.
    result['strong_preference_evidence_groups']=['strong','native_facade','certificate']
    result['strong_gpu_preference_regressions_passed']=True
    result['strong_generic_fallback_regressions_passed']=True
    result['superseded_strong_control_expectations']='.74 sequential CPU/GPU proof expectations replaced by .75 independent overlap, retained uploads and private reusable readback tests'
    return result
