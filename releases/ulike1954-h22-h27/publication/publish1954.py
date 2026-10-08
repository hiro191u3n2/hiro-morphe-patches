#!/usr/bin/env python3
"""Publish reviewed ULike1.9.54 optimizations on exact .53/.186 baseline while retaining other apps.

The independent --expected manifest pins all six artifacts and the exact QA
contract. Read-only --local-only and --preflight-only never write to GitHub.
Both feed branches advance atomically to direct descendants of the leased HEADs.
The checkout, its normal index, and all unrelated repository paths are preserved.
"""
from __future__ import annotations
import argparse
import datetime as dt
import hashlib
import base64
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import time
import urllib.error
import urllib.request
import tempfile
import zipfile

REPO = "hiro191u3n2/hiro-morphe-patches"
VERSION, PREVIOUS_APP = "1.9.54", "1.9.53"
PREVIOUS, BUNDLE = "1.0.186", "1.0.187"
TAG, RAW_BRANCH = "ulike-v1.9.54", "release/ulike1954-187"
SINGLE = "ULike_HQ_Texture_Online_v1.9.54.mpp"
COMBINED = "Hiro_Morphe_Patches_v1.0.187.mpp"
BASE_NAME = "Hiro_Morphe_Patches_v1.0.186.mpp"
BASE_SHA256 = "ef55d7ac2126fd5f08d1e881fda3c60ade904e7cb31fab4c542de6ce0ad17a39"
BASE_BYTES = 17599636
BASE_URL = (f"https://raw.githubusercontent.com/{REPO}/"
            f"cb78a66a8d344f09355d6736da8267632a0a9906/downloads/{BASE_NAME}")
SINGLE_BASE_NAME = "ULike_HQ_Texture_Online_v1.9.53.mpp"
SINGLE_BASE_SHA256 = "0169006ac7d86349ce6dd274b263332a7bd202845fe8bb28f8c947ae662ee982"
SINGLE_BASE_BYTES = 952658
SINGLE_BASE_URL = f"https://github.com/{REPO}/releases/download/ulike-v1.9.53/{SINGLE_BASE_NAME}"
QA_NAME = "QA_ULike_v1.9.54.json"
SOURCE_ZIP = "ULike_v1.9.54_sources_and_QA.zip"
RECEIPT_NAME = "publication_ULike_v1.9.54.json"
ASSETS = (SINGLE, COMBINED, QA_NAME, SOURCE_ZIP, "RELEASE_NOTES.txt", "SHA256SUMS.txt")
RELEASE_ROOT = "releases/ulike1954-h22-h27"
POLICY_PATH = "releases/ULike_ACTIVE.json"
PAGE = f"https://github.com/{REPO}/releases/tag/{TAG}"
MF = "META-INF/MANIFEST.MF"
RUNTIME = "ulike/runtime.dex"
ULIKE_LOADER = "app/hiro/ulike/patches/UlikeHqMaxPatch.class"
NATIVE_ENTRY = "ulike1935/runtime/libulike_speed1935.so"
NATIVE_INSTALLER = "app/hiro/ulike/patches/IntegrationPayload186.class"
NATIVE_SHA256 = "50613d3ed433de1aa3b6c0608753dc98479ac9ce8238033b972cd025b67b297a"
NATIVE_BYTES = 18560
GPU_ENTRY="ulike1949/runtime/libulike_gpu1949.so"
CORE_ENTRY="ulike1950/runtime/libulike_core1950.so"
MOIRE_ENTRY="ulike1951/runtime/libulike_moire1951.so"
H8_GPU_ENTRY="ulike1951/runtime/libulike_h8gpu1951.so"
FINISH1952_ENTRY="ulike1952/runtime/libulike_finish1952.so"
FINISH_GPU_ENTRY="ulike1953/runtime/libulike_finish1953.so"
ALLOWED_ADDED = set()
ALLOWED_CHANGED = {MF, RUNTIME, "classes.dex", ULIKE_LOADER, NATIVE_INSTALLER, FINISH_GPU_ENTRY}
REQUIRED_CHANGED = set(ALLOWED_CHANGED)
NATIVE_PAYLOADS = ("ulike/methods.dex", "ulike/methods.tsv")
TOOLCHAIN_SHA256 = {
    "morphe.jar": "82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c",
    "android.jar": "4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b",
    "d8.jar": "305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5",
}
DIAGNOSTIC_STAGES = ["fusion", "noise", "correction", "compression", "save"]
REQUIRED_QA = {
    "schema": "ulike1954-optimization-v1", "ulike_version": VERSION, "bundle_version": BUNDLE,
    "selected_candidates": ["H22", "H23", "H24", "H25", "H26", "H27"],
    "input_sha256": {
        SINGLE_BASE_NAME: SINGLE_BASE_SHA256, BASE_NAME: BASE_SHA256,
        "ULike_HQ_Texture_Online_v1.9.52.mpp": "62428a1bb7856e2b4640640a360b70ea4ff2a8602c7a0df1dfddef80849c6b77",
        "ULike_HQ_Texture_Online_v1.9.51.mpp": "8b96457398e0a16d66965dbfb38306ba601bf43a7a02b1cdbabcc3ce8ae60bef",
        "ULike_HQ_Texture_Online_v1.9.50.mpp": "ca872a0b6df5f1ca5a949eac0f23d6cba97c7ce662ec7257f4445ba39302a490",
        **TOOLCHAIN_SHA256,
    },
    "host_quality_result.pixel_equivalence_to_baseline": True,
    "host_quality_result.full_resolution_nv21_checked": "4080x3060",
    "approved_lineage": "1.8.8", "device_tested": False, "device_quality_verified": False,
    "non_ulike_resources_byte_identical": True, "non_ulike_loader_classes_unchanged": True,
    "manual_capture_safety_checks_preserved": True, "resolution_and_save_format_preserved": True,
    "standalone_and_bundle_ulike_resources_identical": True,
    "host_quality_passed": True, "production_source_consistency_verified": True,
    "save_lease_hooks_inverse_bytecode_verified": True,
    "save_lease_hooks_serialized_dex_verified": True,
    "save_lease_hooks_target_count": 2, "save_lease_hooks_recycle_call_count": 4,
    "quality_algorithm_and_settings_preserved": True,
    "host_pixel_equivalence_passed": True, "stage_timing_payload_byte_identical": False, "stage_timing_algorithm_preserved": True, "timing_version_only_change": False,
    "gpu_processing_added": True,
    "gpu_shader_execution_on_host":True,"gpu_execution_on_physical_android":False,
    "gpu_policy":"H22-H27 exact dispatch, private readback, policy/geometry reuse, halo transfer and frozen ownership admission with CPU fallback",
    "host_quality_result.suites.primary_denoise_native_exact.production_c_jni_executed":True,
    "host_quality_result.suites.fused_gpu_finishing_exact.software_egl_shaders_executed":True,
    "host_quality_result.suites.gpu_runtime_admission1950.transfer_inclusive_timing_gate":True,
    "host_quality_result.suites.gpu_runtime_admission1950.canonical_boundary_shape_key":True,
    "host_quality_result.suites.gpu_runtime_admission1950.cpu_reference_not_global_lock":True,
    "host_quality_result.suites.primary_denoise_native_exact.production_dex_oracle_executed":True,
    "host_quality_result.suites.primary_denoise_native_exact.runtime_native_selfcheck_passed":True,
    "host_quality_result.suites.primary_denoise_native_exact.jni_copied_output_race_test.copied_output_refused_before_writes":True,
    "host_quality_result.suites.primary_denoise_native_exact.jni_copied_output_race_test.concurrent_disjoint_rows_preserved":True,
    "host_quality_result.suites.fused_gpu_finishing_exact.pixel_equivalence_to_baseline":True,
    "host_quality_result.suites.fused_gpu_finishing_exact.summary_readback_removed":True,
    "host_quality_result.suites.fused_gpu_finishing_exact.direct_destination_slice_commit":True,
    "host_quality_result.suites.fused_gpu_finishing_exact.extra_java_seed_and_copyback_removed":True,
    "host_quality_result.suites.fused_gpu_finishing_exact.native_workspace_reuse":True,
    "host_quality_result.suites.fused_gpu_finishing_exact.batching_rows":128,
    "host_quality_result.suites.fused_gpu_finishing_exact.foreign_egl_context_preserved":True,
    "host_quality_result.suites.fused_gpu_finishing_exact.bounded_timeout_without_readback":True,
    "host_quality_result.suites.exact_correction_copy_elision.general_correction_contract_preserved":True,
    "host_quality_result.suites.exact_correction_copy_elision.actual_native_binary_pinned":True,
    "host_quality_result.suites.exact_correction_copy_elision.actual_production_dex_executed":True,
    "host_quality_result.suites.exact_correction_copy_elision.pixel_equivalence_to_baseline":True,
    "host_quality_result.suites.exact_correction_copy_elision.production_copy_only_inverse_verified":True,
    "host_quality_result.suites.h6_moire_exact.host_C_to_original_Java_pixel_exact":True,
    "host_quality_result.suites.h6_moire_exact.native_sharpen_c_executed":True,
    "host_quality_result.suites.h6_moire_exact.native_sharpen_pixel_exact":True,
    "host_quality_result.suites.h6_moire_exact.combined_moire_sharpen_pixel_exact":True,
    "host_quality_result.suites.h7_primary_denoise_exact.baseline_c_jni_pixel_exact":True,
    "host_quality_result.suites.h7_primary_denoise_exact.output_copy_race_guard":True,
    "host_quality_result.suites.h8_two_pass_gpu_exact.software_egl_two_pass_pixel_exact":True,
    "host_quality_result.suites.h8_two_pass_gpu_exact.stage_parameter_order_verified":True,
    "host_quality_result.suites.h8_two_pass_gpu_exact.sharpening_halo_pixel_exact":True,
    "host_quality_result.suites.h8_two_pass_gpu_exact.production_filter_dex_oracle_executed":True,
    "host_quality_result.suites.h8_two_pass_gpu_exact.software_gpu_two_pass_exact_cases":640,
    "host_quality_result.suites.h8_two_pass_gpu_exact.gpu_execution_on_physical_android":False,
    "host_quality_result.suites.h9_h11_gpu_exact.software_egl_saved_output_pixel_exact":True,
    "host_quality_result.suites.h9_h11_gpu_exact.overlap_and_cpu_fallback_pixel_exact":True,
    "host_quality_result.suites.h9_h11_gpu_exact.policy_transfer_bytes_per_pixel_before":16,
    "host_quality_result.suites.h9_h11_gpu_exact.policy_transfer_bytes_per_pixel_after":8,
    "save_format_and_codec_configuration_preserved": True,
    "native_library_byte_identical": True, "native_installer_byte_identical": False,"native_installer_existing_rows_preserved":False,"native_installer_existing_row_count_preserved":True,"native_installer_preserved_rows":7,"native_installer_legacy_cpu_rows_preserved":True,"native_installer_baseline_gpu_row_updated":True,
    "native_methods_byte_identical": True, "native_methods_tsv_byte_identical": True,
    "inherited_native_libraries_byte_identical": True,
    "host_quality_result.suites.h12_finish_gpu_exact.status":"passed",
    "host_quality_result.suites.h13_dependency_tiling.status":"passed",
    "host_quality_result.suites.h14_whole_route.status":"passed",
    "host_quality_result.suites.h15_performance_hints.status":"passed",
    "host_quality_result.suites.h16_photo_gpu_session.status":"passed",
    "host_quality_result.suites.h17_lossless_policy_transfer.status":"passed",
    "host_quality_result.suites.h17_lossless_policy_transfer.production_helper_executed":True,
    "host_quality_result.suites.h17_lossless_policy_transfer.unchanged_java_float_mask_policy":True,
    "host_quality_result.suites.h17_lossless_policy_transfer.no_precision_reduction":True,
    "host_quality_result.suites.h17_lossless_policy_transfer.all_rounded_java_policy_values_exact":True,
    "host_quality_result.suites.h17_lossless_policy_transfer.signed_mask_wrap_exact":True,
    "host_quality_result.suites.h17_lossless_policy_transfer.uint16_boundary_fallback_exact":True,
    "host_quality_result.suites.h17_lossless_policy_transfer.borrowed_lease_cleanup_checked":True,
    "host_quality_result.suites.h18_resident_source_halo.status":"passed",
    "host_quality_result.suites.h19_cpu_gpu_overlap.status":"passed",
    "host_quality_result.suites.h20_blank_candidate_ownership.status":"passed",
    "host_quality_result.suites.h21_background_admission.status":"passed",
    "host_quality_result.suites.h21_background_admission.actual_java_engine_executed":True,
    "host_quality_result.suites.h21_background_admission.actual_preferences_profile_logic_executed":True,
    "host_quality_result.suites.h21_background_admission.foreground_cpu_returns_before_background_gpu":True,
    "host_quality_result.suites.h21_background_admission.original_foreground_cpu_runs_once":True,
    "host_quality_result.suites.h21_background_admission.successful_associated_save_required":True,
    "host_quality_result.suites.h21_background_admission.exact_capture_preparation_codec_encoder_idle_required":True,
    "host_quality_result.suites.h21_background_admission.native_snapshot_budget_reserved_before_clone":True,
    "host_quality_result.suites.h21_background_admission.queued_probe_budget_released_before_capture_readiness_returns":True,
    "host_quality_result.suites.h21_background_admission.active_gpu_cancel_release_after_real_drain":True,
    "host_quality_result.suites.h21_background_admission.background_candidate_never_published":True,
    "host_quality_result.suites.h21_background_admission.full_pixel_equality_required":True,
    "host_quality_result.suites.h21_background_admission.gpu_timing_includes_candidate_policy_transfer_queue_wait_readback":True,
    "host_quality_result.suites.h21_background_admission.foreground_cpu_duplication_after_admission":False,
    "host_quality_result.suites.h21_background_admission.unknown_environment_never_restores":True,
    "host_quality_result.suites.h21_background_admission.newly_verified_matching_history_requires_current_exact_proof":True,
    "host_quality_result.suites.h21_background_admission.qualified_profile_checksum_required":True,
    "host_quality_result.suites.h21_background_admission.profile_contains_images_or_masks":False,
    "host_quality_result.suites.h21_background_admission.gpu_execution_claimed":False,
    "host_quality_result.suites.h21_background_admission.physical_android_tested":False,
    "host_quality_result.suites.published1952_pipeline_preservation.status":"passed",
    "host_quality_result.suites.published1951_pipeline_preservation.same_output_pixels":True,
    "host_quality_result.suites.published1951_pipeline_preservation.pixel_equivalence_to_baseline":True,
    "host_quality_result.suites.published1951_pipeline_preservation.gpu_unavailable_cpu_fallback_exact":True,
    "host_quality_result.suites.published1951_pipeline_preservation.captured_options_and_rotation_geometry_exact":True,
    "host_quality_result.suites.published1951_pipeline_preservation.source_and_prepared_ownership_preserved":True,
    "host_quality_result.suites.published1951_pipeline_preservation.copy_failure_fallback_preserved":True,
    "host_quality_result.suites.published1951_pipeline_preservation.normalization_and_detail_call_contracts_preserved":True,
    "host_quality_result.suites.published1951_pipeline_preservation.worker_hint_integration.worker_budget_preserved":True,
    "host_quality_result.suites.published1951_pipeline_preservation.worker_hint_integration.nested_work_not_double_reported":True,
    "host_quality_result.suites.published1951_pipeline_preservation.worker_hint_integration.worker_lifecycle_closes":True,
    "host_quality_result.suites.published1951_pipeline_preservation.worker_hint_integration.optional_hint_failure_preserves_work":True,
    "host_quality_result.suites.published1951_pipeline_preservation.worker_hint_integration.cancellation_drains_before_return":True,
    "host_quality_result.suites.published1951_pipeline_preservation.gpu_bitmap_integration.actual_bitmap_gpu_candidate_executed":True,
    "host_quality_result.suites.published1951_pipeline_preservation.gpu_bitmap_integration.whole_cpu_bitmap_candidate_pixel_exact":True,
    "host_quality_result.suites.published1951_pipeline_preservation.gpu_bitmap_integration.gpu_copy_write_interrupt_failure_fallback_exact":True,
    "host_quality_result.suites.published1951_pipeline_preservation.gpu_bitmap_integration.published_candidate_shot_ownership_preserved":True,
    "host_quality_result.suites.published1951_pipeline_preservation.gpu_bitmap_integration.source_unmodified_before_cpu_fallback":True,
    "host_quality_result.full_float_beauty_chain_gpu_ported":False,
    "host_quality_result.device_speedup_verified":False,
    "host_quality_result.suites.h14_whole_route.whole_final_stage_wall_timing":True,
    "host_quality_result.suites.h14_whole_route.candidate_preparation_and_context_wait_included":True,
    "host_quality_result.suites.h14_whole_route.initial_reference_output":True,
    "host_quality_result.suites.h14_whole_route.slowdown_uses_last_reliable_cpu_wall_baseline":True,
    "host_quality_result.suites.h14_whole_route.slowdown_affects_following_photograph":True,
    "host_quality_result.suites.h14_whole_route.captured_shape_settings_keys_only":True,
    "host_quality_result.suites.h14_whole_route.in_flight_shape_eviction":False,
    "host_quality_result.suites.h15_performance_hints.real_worker_tid_checked":True,
    "host_quality_result.suites.h15_performance_hints.repeated_work_feedback_checked":True,
    "host_quality_result.suites.h15_performance_hints.worker_lifetime_close_checked":True,
    "host_quality_result.suites.h15_performance_hints.unsupported_optional_api_checked":True,
    "host_quality_result.suites.h15_performance_hints.production_helper_compiled":True,
    "host_quality_result.suites.h15_performance_hints.baseline_android_compile_checked":True,
    "host_quality_result.suites.h15_performance_hints.optional_os_hint_does_not_modify_pixels":True,
    "host_quality_result.suites.h14_whole_route.per_photo_cpu_duplication_after_admission":False,
    "host_quality_result.suites.h14_whole_route.periodic_whole_photo_duplication":False,
    "host_quality_result.suites.h14_whole_route.minimum_timing_margin_percent":5,
    "host_quality_result.suites.h14_whole_route.admitted_slowdown_threshold_percent":10,
    "host_quality_result.suites.h12_finish_gpu_exact.mesa_gles_shader_executed":True,
    "host_quality_result.suites.h12_finish_gpu_exact.production_jni_executed":True,
    "host_quality_result.suites.h12_finish_gpu_exact.gpu_chain_pixel_exact":True,
    "host_quality_result.suites.h12_finish_gpu_exact.java_float_and_mask_policy_exact":True,
    "host_quality_result.suites.h12_finish_gpu_exact.candidate_failure_destination_unchanged":True,
    "host_quality_result.suites.h12_finish_gpu_exact.interrupted_candidate_destination_unchanged":True,
    "host_quality_result.suites.h12_finish_gpu_exact.partial_destination_rows_untouched":True,
    "host_quality_result.suites.h12_finish_gpu_exact.source_immutable":True,
    "host_quality_result.suites.h12_finish_gpu_exact.concurrent_capture_workspace_exact":True,
    "host_quality_result.suites.h12_finish_gpu_exact.one_final_readback_no_intermediate_cpu_wait":True,
    "host_quality_result.suites.h13_dependency_tiling.sequential_corrected_neighborhood_pixel_exact":True,
    "host_quality_result.suites.h13_dependency_tiling.dependency_36_row_halo_pixel_exact":True,
    "host_quality_result.suites.h13_dependency_tiling.dependency_complete_tiles_executed":True,
    "original_apk_apply_tested":False, "host_quality_result.status": "passed",
    "jdk_identity.runtime_version": "21.0.8+9-LTS", "jdk_identity.vendor": "Eclipse Adoptium",
    "jdk_identity.javac_version": "21.0.8",
    "diagnostic_report_updates_on_success": True,
    "legacy_report_not_displayed_as_current": True,
    "diagnostic_overlapping_wall_clock": True,
    "diagnostic_per_shot_owner": True,
    "diagnostic_stage_names": DIAGNOSTIC_STAGES,
}

OTHER_APPS = ("Microsoft SwiftKey Beta", "Microsoft SwiftKey Beta Keyboard", "SwiftKey Beta",
              "Berry Browser", "Instagram", "X", "Twitter", "Trip.com", "TikTok",
              "Yahoo!乗換案内", "Y!乗換案内", "Amazonショッピング", "Amazon Shopping", "Hanull Reader",
              "Uber Eats", "BrightnessClick", "明るさタッチ", "簡単検索くん", "LINE")
MANAGER_SOURCE = ("https://github.com/MorpheApp/morphe-manager/blob/"
                  "0ed521a8fddd1b8d72c70ac2faa0c9658ba7a3ff/"
                  "app/src/main/java/app/morphe/manager/util/ChangelogParser.kt")
HEADING = re.compile(r"^#{1,3}\s+(?:\S+\s+)?(?:\[([^]]+)]\([^)]*\)|([^\s\[(]+))\s+\((\d{4}-\d{2}-\d{2})\)", re.I)
SCOPE = re.compile(r"^\* \*\*(.+?):\*\*")
EXPERIMENTAL_ONLY = re.compile(r"^Add(?:ed)?\s+experimental\s+support\s+for\b", re.I)
INVENTORY = re.compile(r"(?m)^(ULike：)v(1\.9\.53)(（5\.6\.2／740）)\r?$")
SUMMARY = "H22～H27を統合。GPU処理行数の実測選択、専用受信、一定の補助値と座標の再利用、周辺画素の差分転送、可逆な形式展開と凍結画像の所有権で採用判定の複製を削減。画素一致と転送・待機込みの保存画像仕上げ速度で採用し、不成立時は既存経路へ復帰。画質・撮影・保存設定を維持。元APKSへの今回の適用とGalaxy実機の速度・画質は未確認。"

for _suite in ['published1953_pipeline_preservation', 'h22_dispatch_batch_exact', 'h23_direct_candidate_readback', 'h24_exact_coordinate_policy_reuse', 'h25_source_halo_band_reuse', 'h26_single_pass_policy_fallback', 'h27_frozen_probe_ownership']:
    REQUIRED_QA["host_quality_result.suites." + _suite + ".status"] = "passed"

H54_REQUIRED_FLAGS = {'h22_dispatch_batch_exact': ['h22_dispatch_batch_pixel_exact', 'dispatch_options_pixel_exact_to_pinned_java', 'sequential_36_row_dependency_halo_preserved', 'larger_dispatch_batches_reduce_command_count', 'bounded_two_slot_fences_preserved'], 'h23_direct_candidate_readback': ['h23_direct_candidate_readback_pixel_exact', 'direct_candidate_no_native_staging_memcpy', 'direct_candidate_pixels_equal_staged_readback', 'old_staged_collect_preserved', 'late_unmap_failure_old_staged_destination_unchanged', 'late_unmap_failure_private_candidate_written_but_rejected', 'late_unmap_failure_original_source_unchanged', 'late_failure_other_pending_slot_drained_and_failed_slot_quarantined', 'cancelled_private_ticket_destination_unchanged'], 'h24_exact_coordinate_policy_reuse': ['exact_float_coordinate_coefficients', 'constant_policy_shortcut', 'policy_integer_equivalence_to_published1953', 'pixel_equivalence_to_published1953'], 'h26_single_pass_policy_fallback': ['immutable_mask_single_pass_prefix_expansion_exact', 'unknown_custom_mask_original_call_order_preserved', 'policy_integer_equivalence_to_published1953', 'pixel_equivalence_to_published1953'], 'h25_source_halo_band_reuse': ['source_halo_suffix_reads_measured', 'native_coverage_fallback_exact', 'actual_production_gpu_jni_executed', 'actual_bitmap_gpu_candidate_executed'], 'h27_frozen_probe_ownership': ['frozen_source_reference_leases_checked', 'unknown_mutable_owner_never_leased', 'recycle_resize_save_cancel_failure_cleanup_checked', 'actual_production_gpu_jni_executed', 'actual_bitmap_gpu_candidate_executed', 'actual_normalize_cpu_destination_prepared_detail_save_cancel_checked']}
for _suite, _flags in H54_REQUIRED_FLAGS.items():
    for _flag in _flags:
        REQUIRED_QA["host_quality_result.suites." + _suite + "." + _flag] = True


for _flag in ('h22_actual_dispatch_selection_engine_executed','h22_all_mode_full_pixel_proofs','h22_transfer_inclusive_mode_selection','h22_unknown_dispatch_history_requires_fresh_proof','h22_private_candidate_released_between_trials','h22_mode_selection_cancellation_checked'):
    REQUIRED_QA['host_quality_result.suites.h22_dispatch_batch_exact.dispatch_engine_integration.'+_flag]=True
REQUIRED_QA['host_quality_result.suites.h22_dispatch_batch_exact.dispatch_engine_integration.h22_dispatch_choices']=[32,64,0]
for _key,_value in {'reference_snapshot_copies':0,'source_snapshot_copies':0,'cpu_destination_copies_per_probe':1,'physical_android_tested':False,'device_speed_measured':False}.items():
    REQUIRED_QA['host_quality_result.suites.h27_frozen_probe_ownership.'+_key]=_value
SAVE_LEASE_METHOD_PINS={
 'Lcom/hiro/ulike/SaveFd186;->recycle(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;)V':'8f7b3d03416c1ba35a749377d414805b53e84ab45c0d304ea9862b0d2f282a83',
 'Lcom/hiro/ulike/SaveQuality2;->saveFinalBefore1947(Landroid/graphics/Bitmap;Ljava/io/File;Landroid/graphics/Bitmap$CompressFormat;I)Z':'3e9028bdb526d223e68c3d3d6c66e1139b381e4329e88dd5c598ba5a1caebd84'}
REQUIRED_QA['save_lease_hook_audit.published1953_method_pins']=SAVE_LEASE_METHOD_PINS
REQUIRED_QA['save_lease_hook_audit.target_methods']=sorted(SAVE_LEASE_METHOD_PINS)


def require(condition, message):
    if not condition:
        raise RuntimeError(message)

def sha(data):
    return hashlib.sha256(data).hexdigest()

def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode()

def checked_path(name):
    p = PurePosixPath(name)
    require(name and not p.is_absolute() and ".." not in p.parts and "\\" not in name
            and str(p) == name and not name.endswith("/"),
            "Unsafe archive/repository path: " + name)
    return p

def archive(raw):
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        names = z.namelist()
        require(len(names) == len(set(names)), "Duplicate archive paths")
        for name in names:
            checked_path(name.rstrip("/"))
        require(z.testzip() is None, "Corrupt ZIP")
        return {name: z.read(name) for name in names if not name.endswith("/")}

def headers(raw):
    rows = []
    for line in raw.replace(b"\r\n", b"\n").split(b"\n"):
        if line.startswith(b" "):
            require(bool(rows), "Manifest continuation without a header")
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    pairs = [line.decode().split(": ", 1) for line in rows]
    require(all(len(pair) == 2 for pair in pairs), "Malformed MPP manifest")
    require(len(pairs) == len(dict(pairs)), "Duplicate MPP manifest key")
    return dict(pairs)

def version_tuple(value):
    require(re.fullmatch(r"v?\d+\.\d+\.\d+", value) is not None,
            "Expected stable three-part bundle version")
    return tuple(map(int, value.removeprefix("v").split(".")))

def parsed_entries(markdown):
    """Only the heading/scope subset used by this stable release is needed."""
    entries, current = [], None
    for line in markdown.splitlines():
        m = HEADING.match(line)
        if m:
            current = {"version": m[1] or m[2], "date": m[3], "bullets": []}
            entries.append(current)
        elif current is not None:
            s = SCOPE.match(line.strip())
            if s:
                body = line.strip()[s.end():].strip()
                current["bullets"].append((s[1], body))
    return entries

def has_changes_for(markdown, installed, app_names):
    """Mirror Manager's scope matching for the stable versions used here."""
    entries = parsed_entries(markdown)
    old_date = next((e["date"] for e in entries if e["version"].removeprefix("v") == installed), None)
    for entry in entries:
        if not re.fullmatch(r"v?\d+\.\d+\.\d+", entry["version"]):
            continue
        if version_tuple(entry["version"]) <= version_tuple(installed):
            continue
        if old_date and entry["date"] < old_date:
            continue
        for scope, body in entry["bullets"]:
            if EXPERIMENTAL_ONLY.match(body):
                continue
            for app in app_names:
                if scope.casefold() == app.casefold() or scope.casefold().startswith(app.casefold() + " - "):
                    return True
    return False

def lookup_qa(data, dotted_path):
    for component in dotted_path.split("."):
        require(isinstance(data, dict) and component in data, "Missing QA field: " + dotted_path)
        data = data[component]
    return data

def api(path, data=None, method=None, absent=False):
    command = ["gh", "api", path]
    if method:
        command += ["--method", method]
    if data is not None:
        command += ["--input", "-"]
    result = subprocess.run(command, input=None if data is None else json.dumps(data),
                            text=True, capture_output=True, check=False)
    if result.returncode:
        if absent and data is None and "(HTTP 404)" in result.stderr:
            return None
        raise RuntimeError("GitHub API failure: " + result.stderr.strip())
    return json.loads(result.stdout) if result.stdout.strip() else None


def fetch(url):
    for attempt in range(4):
        try:
            request = urllib.request.Request(url, headers={
                "User-Agent": "Hiro-ULike1954-Publication",
                "Accept-Encoding": "identity", "Cache-Control": "no-cache",
            })
            with urllib.request.urlopen(request, timeout=45) as response:
                require(response.status == 200, "Public download did not return HTTP200")
                return response.read()
        except (OSError, urllib.error.URLError) as error:
            if attempt == 3 or isinstance(error, urllib.error.HTTPError) and error.code not in (404, 429, 500, 502, 503, 504):
                raise
            time.sleep(2 ** attempt)


def head(branch):
    return api(f"repos/{REPO}/git/ref/heads/{branch}")["object"]["sha"]


def content(path, ref):
    row = api(f"repos/{REPO}/contents/{path}?ref={ref}")
    require(row.get("encoding") == "base64", "Unexpected repository file encoding")
    return base64.b64decode(row["content"])


def snapshot(branch):
    current = head(branch)
    commit = api(f"repos/{REPO}/git/commits/{current}")
    return {"branch": branch, "head": current, "tree": commit["tree"]["sha"],
            "manifest": json.loads(content("patches-bundle.json", current)),
            "log": content("CHANGELOG.md", current).decode(),
            "ulike_policy": content(POLICY_PATH, current)}


def check_heads(states):
    for state in states.values():
        require(head(state["branch"]) == state["head"], "Concurrent update: " + state["branch"])


def git(repo, *args, input=None, env=None):
    command = ["git", "-C", str(repo), "-c", "credential.helper=",
               "-c", "credential.helper=!gh auth git-credential", *map(str, args)]
    result = subprocess.run(command, input=input, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                            env=env, check=False)
    require(result.returncode == 0, "Git command failed: " + " ".join(map(str, args)) + "\n" + result.stderr.decode(errors="replace"))
    return result.stdout.decode().strip()

def prepare_commit(repo, state, files, message):
    """Use a private index; preserve the checkout, its index, and every other path."""
    env = {**os.environ, "GIT_AUTHOR_NAME": "github-actions[bot]", "GIT_COMMITTER_NAME": "github-actions[bot]",
           "GIT_AUTHOR_EMAIL": "41898282+github-actions[bot]@users.noreply.github.com",
           "GIT_COMMITTER_EMAIL": "41898282+github-actions[bot]@users.noreply.github.com"}
    with tempfile.TemporaryDirectory(prefix="ulike1954-index-") as temp:
        env["GIT_INDEX_FILE"] = str(Path(temp) / "index")
        git(repo, "read-tree", state["head"], env=env)
        for path, raw in files.items():
            checked_path(path)
            blob = git(repo, "hash-object", "-w", "--stdin", input=raw, env=env)
            git(repo, "update-index", "--add", "--cacheinfo", f"100644,{blob},{path}", env=env)
        tree = git(repo, "write-tree", env=env)
        commit = git(repo, "commit-tree", tree, "-p", state["head"], "-F", "-", input=(message + "\n").encode(), env=env)
        require(git(repo, "show", "-s", "--format=%P", commit) == state["head"], "New commit is not a direct descendant")
        actual_changes = set(git(repo, "diff-tree", "--no-commit-id", "--name-only", "-r", commit).splitlines())
        require(actual_changes and actual_changes.issubset(files), "Prepared commit changes unrelated files")
        return {**state, "head": commit, "tree": tree}

def commit_feeds_atomically(repo, states, updates, message):
    require(set(states) == {"main", "dev"} and set(updates) == set(states), "Both feed branches are required")
    check_heads(states)
    git(repo, "fetch", "--no-tags", f"https://github.com/{REPO}.git", states["main"]["head"], states["dev"]["head"])
    prepared = {branch: prepare_commit(repo, state, updates[branch], message) for branch, state in states.items()}
    check_heads(states)
    # Each proposed commit has exactly the leased old HEAD as its sole parent.
    # The lease adds race protection; it does not permit rewriting old history.
    git(repo, "push", "--atomic",
        f"--force-with-lease=refs/heads/main:{states['main']['head']}",
        f"--force-with-lease=refs/heads/dev:{states['dev']['head']}",
        f"https://github.com/{REPO}.git",
        f"{prepared['main']['head']}:refs/heads/main", f"{prepared['dev']['head']}:refs/heads/dev")
    check_heads(prepared)
    return prepared

def utc_created(states):
    now = dt.datetime.now(dt.timezone.utc).replace(tzinfo=None, microsecond=0)
    require(all(now > dt.datetime.fromisoformat(s["manifest"]["created_at"]) for s in states.values()),
            "Current UTC time must follow both old feed timestamps")
    return now.isoformat()


def is_ulike(name):
    return name.startswith("app/hiro/ulike/patches/") or re.match(r"^ulike(?:\d+)?/", name) is not None


def metadata(state, url, created, notes):
    old = state["manifest"]
    require("元APKSへの今回の適用は未実施" in notes,
            "Manager feed must disclose that this release was not applied to the original APKS")
    require(old["version"] == PREVIOUS and old["download_url"] == BASE_URL,
            "Another bundle or baseline source is active")
    require(dt.datetime.fromisoformat(created) > dt.datetime.fromisoformat(old["created_at"]), "created_at must advance")
    require(len(list(INVENTORY.finditer(old["description"]))) == 1, "Missing or ambiguous current ULike inventory")
    previous_description = INVENTORY.sub(lambda m: m[1] + "v" + VERSION + m[3], old["description"], count=1)
    feed = {**old, "version": BUNDLE, "created_at": created, "download_url": url,
            "page_url": PAGE, "signature_download_url": "", "description": notes.strip() + "\n\n" + previous_description}
    require(feed["description"].startswith(notes.strip()) and
            "元APKSへの今回の適用は未実施" in feed["description"].split("\n\n" + previous_description)[0],
            "Current feed validation scope was lost")
    require(not any(HEADING.match(line) for line in notes.splitlines()), "Release notes contain a competing version heading")
    changelog = f"# {BUNDLE} ({created[:10]})\n\n* **ULike:** v{VERSION}：{SUMMARY}\n\n" + notes.strip() + "\n\n" + state["log"]
    require(has_changes_for(changelog, PREVIOUS, ["ULike"]), "Missing ULike update scope")
    for installed in tuple("1.0."+str(v) for v in range(171,187)):
        require(has_changes_for(changelog, installed, ["ULike"]), "Prior ULike installation must receive a repatch update: " + installed)
    require(not has_changes_for(changelog, BUNDLE, ["ULike"]), "Freshly patched ULike would still be outdated")
    historical_scopes = {scope for entry in parsed_entries(state["log"]) for scope, _ in entry["bullets"]}
    other_apps = {app for app in set(OTHER_APPS) | historical_scopes
                  if app.casefold() != "ulike" and not app.casefold().startswith("ulike - ")}
    require(not any(has_changes_for(changelog, PREVIOUS, [app]) for app in other_apps), "False update scope for another app")
    entry = parsed_entries(changelog)[0]
    require(entry["version"] == BUNDLE and {scope for scope, _ in entry["bullets"]} == {"ULike"}, "Missing or unrelated app scope")
    require(changelog.endswith(state["log"]), "Prior change history was lost")
    # The exact old description is retained except for its one ULike inventory row.
    require(feed["description"].endswith(previous_description), "Prior description was lost")
    return feed, changelog


def entry_plan(expected, key):
    value = expected.get(key)
    require(isinstance(value, list) and all(isinstance(x, str) for x in value), "Missing entry plan: " + key)
    require(len(value) == len(set(value)), "Duplicate entry plan: " + key)
    for name in value:
        checked_path(name)
    return set(value)


def configure(expected):
    """Pin the current reviewed bundle; fail closed if it advances before publish."""
    global PREVIOUS, BUNDLE, COMBINED, BASE_NAME, BASE_SHA256, BASE_BYTES, BASE_URL
    global RAW_BRANCH, ASSETS, REQUIRED_QA
    require(expected.get("ulike_version") == VERSION and expected.get("baseline_ulike_version") == PREVIOUS_APP,
            "ULike version or lineage base differs")
    require(expected.get("baseline_ulike_sha256") == SINGLE_BASE_SHA256,
            "Approved ULike1.9.53 checksum differs")
    require(expected.get("baseline_ulike_bytes") == SINGLE_BASE_BYTES and expected.get("baseline_ulike_url") == SINGLE_BASE_URL,
            "Approved ULike1.9.53 source differs")
    previous, bundle = expected.get("baseline_bundle_version"), expected.get("bundle_version")
    require(isinstance(previous, str) and isinstance(bundle, str), "Bundle versions are missing")
    pv, nv = version_tuple(previous), version_tuple(bundle)
    require(previous == "1.0.186" and bundle == "1.0.187" and pv == (1, 0, 186) and nv == (1, 0, 187),
            "This reviewed diagnostics release uses exact baseline1.0.186 and publishes1.0.187")
    name = "Hiro_Morphe_Patches_v" + previous + ".mpp"
    digest, size, url = [expected.get("baseline_bundle_" + key) for key in ("sha256", "bytes", "url")]
    require(isinstance(digest, str) and re.fullmatch(r"[0-9a-f]{64}", digest), "Unfilled baseline digest")
    require(type(size) is int and size > 0, "Unfilled baseline size")
    require(isinstance(url, str) and re.fullmatch(
        r"https://raw\.githubusercontent\.com/hiro191u3n2/hiro-morphe-patches/[0-9a-f]{40}/downloads/" + re.escape(name), url),
        "Baseline must be an immutable current repository MPP URL")
    require(digest == BASE_SHA256 and size == BASE_BYTES and url == BASE_URL,
            "Exact baseline bundle1.0.186 digest, size and immutable source are required")
    PREVIOUS, BUNDLE = previous, bundle
    COMBINED = "Hiro_Morphe_Patches_v" + BUNDLE + ".mpp"
    BASE_NAME, BASE_SHA256, BASE_BYTES, BASE_URL = name, digest, size, url
    RAW_BRANCH = "release/ulike1954-" + str(nv[2])
    ASSETS = (SINGLE, COMBINED, QA_NAME, SOURCE_ZIP, "RELEASE_NOTES.txt", "SHA256SUMS.txt")
    REQUIRED_QA = {**REQUIRED_QA, "bundle_version": BUNDLE}
    require(expected.get("native_library_sha256") == NATIVE_SHA256
            and expected.get("native_library_bytes") == NATIVE_BYTES, "Diagnostics native45 library must remain exact")
    require(isinstance(expected.get("gpu_native_library_sha256"),str) and re.fullmatch("[0-9a-f]{64}",expected["gpu_native_library_sha256"]) is not None and expected["gpu_native_library_sha256"]!="0"*64,"Pinned GPU native digest required")
    require(type(expected.get("gpu_native_library_bytes")) is int and expected["gpu_native_library_bytes"]>0,"Pinned GPU native length required")
    for prefix in ("core", "gpu", "moire", "h8gpu", "finishgpu1952", "finishgpu"):
        digest, size = expected.get(prefix + "_native_library_sha256"), expected.get(prefix + "_native_library_bytes")
        require(isinstance(digest, str) and re.fullmatch("[0-9a-f]{64}", digest) is not None and digest != "0" * 64,
                "Pinned native digest required: " + prefix)
        require(type(size) is int and size > 0, "Pinned native length required: " + prefix)
    require(expected.get("ndk_revision")=="27.2.12479018","Pinned NDK r27c revision required")
    require(expected.get("toolchain_sha256") == TOOLCHAIN_SHA256, "Reviewed toolchain differs")
    require(expected.get("jdk_runtime_version") == "21.0.8+9-LTS"
            and expected.get("jdk_vendor") == "Eclipse Adoptium", "Reviewed JDK identity differs")


def load_expected(path):
    expected = json.loads(path.read_text())
    configure(expected)
    require(expected.get("schema") == "ulike1954-publication-v1", "Wrong publication manifest schema")
    for key, value in {"ulike_version": VERSION, "bundle_version": BUNDLE,
                       "baseline_bundle_version": PREVIOUS, "baseline_bundle_sha256": BASE_SHA256,
                       "baseline_ulike_version": PREVIOUS_APP, "baseline_ulike_sha256": SINGLE_BASE_SHA256}.items():
        require(expected.get(key) == value, "Wrong reviewed publication value: " + key)
    require(expected.get("change_plan_reviewed") is True and expected.get("qa_contract_reviewed") is True,
            "Review the exact entry delta and QA contract before publishing")
    require(expected.get("android_device_tested") is False, "Galaxy execution is unverified")
    require(expected.get("original_apk_apply_tested") is False, "Original APKS application was unavailable for this release")
    require(set(expected.get("artifacts", {})) == set(ASSETS), "Pin exactly the six release assets")
    for name, row in expected["artifacts"].items():
        require(type(row.get("bytes")) is int and row["bytes"] > 0, "Unfilled asset size: " + name)
        require(isinstance(row.get("sha256"), str) and re.fullmatch(r"[0-9a-f]{64}", row["sha256"]) is not None
                and row["sha256"] != "0" * 64,
                "Unfilled asset checksum: " + name)
    for kind in ("bundle", "standalone"):
        changed = entry_plan(expected, "allowed_changed_" + kind + "_entries")
        require(REQUIRED_CHANGED <= changed <= ALLOWED_CHANGED, "Change plan exceeds the reviewed diagnostics edit")
        require(entry_plan(expected, "allowed_added_" + kind + "_entries") == ALLOWED_ADDED, "H22-H27 updates existing resources without new installer rows")
    contract = expected.get("qa_required_values")
    require(isinstance(contract, dict), "Expected QA contract is absent")
    for key, value in REQUIRED_QA.items():
        require(type(contract.get(key)) is type(value) and contract[key] == value, "Missing semantic QA requirement: " + key)
    require(type(contract.get("host_quality_result.assertions")) is int
            and contract["host_quality_result.assertions"] > 0, "Pin the real positive host assertion count")
    require(contract.get("original_apk_apply_tested") is expected["original_apk_apply_tested"], "APK validation scope differs")
    for key in ("changed_runtime_methods", "changed_native_methods", "new_helper_classes",
                "new_runtime_aliases", "new_native_methods"):
        rows = contract.get(key)
        require(isinstance(rows, list) and all(isinstance(row, str) and row.startswith("L")
                and ";" in row and not re.search(r"TODO|PLACEHOLDER|TBD", row) for row in rows),
                "Missing reviewed DEX change inventory: " + key)
        require(len(rows) == len(set(rows)), "Duplicate reviewed DEX change inventory: " + key)
    roots = expected.get("production_helper_roots")
    require(isinstance(roots, list) and len(roots) == len(set(roots))
            and {"QualityPipeline1932", "ProcessingTiming1947", "SaveQueue1935", "ShotContext1932", "AsyncSave1935", "GpuFinish1953", "FinishPolicy1953", "WholeRoute1953", "SpatialNoise1934"} == set(roots)
            and contract.get("replaced_helper_roots") == sorted(roots),
            "Reviewed H22-H27 production helper inventory is required")
    require(bool(contract.get("changed_runtime_methods")), "Diagnostics must bind the actual runtime edit inventory")
    source_names = expected.get("required_source_paths")
    require(isinstance(source_names, list) and source_names and len(source_names) == len(set(source_names)),
            "Missing reviewed source inventory")
    for name in source_names:
        require(isinstance(name, str) and name.startswith("src/"), "Missing source path")
        checked_path(name)
        require(not re.search(r"TODO|PLACEHOLDER|TBD", name), "Unfilled required source path")
    required = {"src/build1954.py", "src/validate1954.py", "src/finalize1954.py",
                "src/host_regression1954.py", "src/finishgpu1953/finish1953.c", "src/finishgpu1953/build_finishgpu1953.py",
                *("src/" + root + ".java" for root in roots)}
    require(required <= set(source_names), "Reproducible diagnostics build and host regression sources required")
    return expected


def validate_entry_delta(old, new, expected, kind):
    removed, added = set(old) - set(new), set(new) - set(old)
    changed = {name for name in old.keys() & new.keys() if old[name] != new[name]}
    require(not removed, kind + " entries were removed")
    require(changed == entry_plan(expected, "allowed_changed_" + kind + "_entries"), "Unexpected " + kind + " changed entries: " + repr(sorted(changed)))
    require(added == entry_plan(expected, "allowed_added_" + kind + "_entries"), "Unexpected " + kind + " added entries")
    if ULIKE_LOADER in changed:
        require(new[ULIKE_LOADER].startswith(b"\xca\xfe\xba\xbe"), "Invalid ULike JVM loader")
    return changed, added


def validate_local(dist, expected, baseline, standalone_baseline, repo):
    require(len(baseline) == BASE_BYTES and sha(baseline) == BASE_SHA256, "Wrong bundle baseline bytes")
    require(len(standalone_baseline) == SINGLE_BASE_BYTES and sha(standalone_baseline) == SINGLE_BASE_SHA256, "Wrong ULike baseline bytes")
    payloads = {}
    for name, row in expected["artifacts"].items():
        raw = (dist / name).read_bytes()
        require(len(raw) == row["bytes"] and sha(raw) == row["sha256"], "Artifact differs from reviewed bytes: " + name)
        payloads[name] = raw
    old, new = archive(baseline), archive(payloads[COMBINED])
    old_single, single = archive(standalone_baseline), archive(payloads[SINGLE])
    for items, version in ((old, PREVIOUS), (old_single, PREVIOUS_APP), (new, BUNDLE), (single, VERSION)):
        require(headers(items[MF])["Version"] == version, "MPP manifest version mismatch")
        require(items.get("classes.dex", b"").startswith(b"dex\n"), "Invalid patch-loader DEX")
        require(items.get(RUNTIME, b"").startswith(b"dex\n"), "Invalid ULike runtime DEX")
    require(headers(new[MF]).get("Name") == headers(old[MF]).get("Name"), "Bundle source name changed")
    require(headers(single[MF]).get("Name") == headers(old_single[MF]).get("Name"), "ULike source name changed")
    changed, added = validate_entry_delta(old, new, expected, "bundle")
    single_changed, single_added = validate_entry_delta(old_single, single, expected, "standalone")
    own = lambda items: {name: raw for name, raw in items.items() if is_ulike(name)}
    other = lambda items: {name: raw for name, raw in items.items() if not is_ulike(name) and name not in (MF, "classes.dex")}
    require(own(old) == own(old_single), "Bundle and standalone ULike baselines differ")
    require(own(new) == own(single), "Bundle and standalone ULike payloads differ")
    require(other(old) == other(new), "Non-ULike resources changed from the exact baseline bundle")
    require(all(is_ulike(name) or name in (MF, "classes.dex") for name in single), "Standalone has another application's files")
    qa = json.loads(payloads[QA_NAME])
    for key, value in expected["qa_required_values"].items():
        actual = lookup_qa(qa, key)
        require(type(actual) is type(value) and actual == value, "QA assertion failed: " + key)
    suites = qa.get("host_quality_result", {}).get("suites", {})
    for stage in ("h6_moire_exact", "h7_primary_denoise_exact", "h8_two_pass_gpu_exact", "h9_h11_gpu_exact", "h12_finish_gpu_exact", "h13_dependency_tiling", "h14_whole_route", "h15_performance_hints", "published1952_pipeline_preservation", "published1953_pipeline_preservation", "h22_dispatch_batch_exact", "h23_direct_candidate_readback", "h24_exact_coordinate_policy_reuse", "h25_source_halo_band_reuse", "h26_single_pass_policy_fallback", "h27_frozen_probe_ownership", "h16_photo_gpu_session", "h17_lossless_policy_transfer", "h18_resident_source_halo", "h19_cpu_gpu_overlap", "h20_blank_candidate_ownership", "h21_background_admission"):
        require(isinstance(suites.get(stage), dict) and bool(suites[stage]),
                "Missing executed H22-H27 host suite: " + stage)
    require(type(suites["h6_moire_exact"].get("pixels_compared")) is int
            and suites["h6_moire_exact"]["pixels_compared"] >= 100000,
            "H6 oracle did not compare enough actual pixels")
    require(suites["h7_primary_denoise_exact"].get("assertions") == 122827264,
            "H7 native-denoise exhaustive suite did not execute")
    h8_suite=suites["h8_two_pass_gpu_exact"]
    require(type(h8_suite.get("production_filter_wrapper_assertions")) is int
            and h8_suite["production_filter_wrapper_assertions"] > 0
            and h8_suite.get("software_gpu_two_pass_exact_cases") == 640
            and h8_suite.get("assertions") == 640 + h8_suite["production_filter_wrapper_assertions"],
            "H8 software EGL two-pass cases did not execute")
    require(suites["h9_h11_gpu_exact"].get("assertions") == 32 + 39990 + 14310,
            "H9-H11 saved-output and overlap cases did not execute")
    native_unchanged = old["ulike/methods.dex"] == new["ulike/methods.dex"]
    inventory_unchanged = old["ulike/methods.tsv"] == new["ulike/methods.tsv"]
    require(new[NATIVE_ENTRY] == old[NATIVE_ENTRY] and new[NATIVE_INSTALLER] != old[NATIVE_INSTALLER],
            "Retained legacy CPU kernel and updated installer required")
    require(qa.get("native_library_byte_identical") is True and qa.get("native_installer_byte_identical") is False
            and qa.get("native_installer_legacy_cpu_rows_preserved") is True,
            "Native preservation claims must match bytes")
    for label, entry in (("gpu", GPU_ENTRY), ("core", CORE_ENTRY),
                         ("moire", MOIRE_ENTRY), ("h8gpu", H8_GPU_ENTRY), ("finishgpu1952", FINISH1952_ENTRY), ("finishgpu", FINISH_GPU_ENTRY)):
        raw = new.get(entry, b"")
        require(raw[:6] == b"\x7fELF\x02\x01" and raw[18:20] == b"\xb7\x00",
                "ELF64 AArch64 resource missing: " + entry)
        require(sha(raw) == expected[label + "_native_library_sha256"] == qa[label + "_native_library_sha256"]
                and len(raw) == expected[label + "_native_library_bytes"] == qa[label + "_native_library_bytes"],
                "Native resource fingerprint differs: " + entry)
        require(qa[label + "_native_build"]["ndk_revision"] == expected["ndk_revision"],
                "Pinned NDK compiler differs: " + entry)
    require(all(new[entry]==old[entry] for entry in (GPU_ENTRY,CORE_ENTRY,MOIRE_ENTRY,H8_GPU_ENTRY,FINISH1952_ENTRY)), "Inherited native payload bytes changed")
    require(sha(new[NATIVE_ENTRY]) == expected["native_library_sha256"] and len(new[NATIVE_ENTRY]) == expected["native_library_bytes"], "Pinned native kernel differs")
    require(qa["native_methods_byte_identical"] is native_unchanged,
            "Native payload preservation claim does not match actual bytes")
    require(qa["native_methods_tsv_byte_identical"] is inventory_unchanged,
            "Native inventory preservation claim does not match actual bytes")
    require(native_unchanged or qa["changed_native_methods"], "Changed native payload requires exact method inventory")
    for name in NATIVE_PAYLOADS:
        require(name in new and name in single, "Missing native ULike payload: " + name)
    for name in (SINGLE, COMBINED):
        row = qa.get("artifacts", {}).get(name, {})
        require(row.get("sha256") == sha(payloads[name]) and row.get("bytes") == len(payloads[name]), "QA MPP fingerprint mismatch: " + name)
    checksums = {}
    for line in payloads["SHA256SUMS.txt"].decode().splitlines():
        if not line.strip():
            continue
        match = re.fullmatch(r"([0-9a-f]{64}) [ *](.+)", line)
        require(match is not None, "Malformed SHA256SUMS line")
        digest, name = match.groups()
        require(name in payloads and name != "SHA256SUMS.txt" and name not in checksums, "Unknown or duplicate checksum entry")
        require(sha(payloads[name]) == digest, "SHA256SUMS mismatch: " + name)
        checksums[name] = digest
    require(set(checksums) == set(ASSETS) - {"SHA256SUMS.txt"}, "Checksums must cover all five other assets")
    sources = archive(payloads[SOURCE_ZIP])
    require(sources and set(expected["required_source_paths"]) <= sources.keys(), "Missing reviewed implementation source")
    require("evidence/validation.json" in sources, "New release validation disclosure is absent")
    desktop = json.loads(sources["evidence/validation.json"])
    require(desktop.get("schema") == "ulike1954-desktop-validation-v1"
            and desktop.get("original_apk_apply_tested") is False
            and desktop.get("device_tested") is False
            and desktop.get("device_quality_verified") is False,
            "Desktop validation scope falsely claims original APKS application")
    require(desktop.get("artifacts") == qa.get("artifacts")
            and desktop.get("bundle_version") == BUNDLE,
            "Desktop evidence must identify both exact rebuilt MPPs")
    require(qa.get("desktop_evidence_sha256") == sha(sources["evidence/validation.json"]),
            "Packaged desktop validation bytes differ from finalized QA")
    require("save-lease-hooks1954.json" in sources,"Serialized final-save lease hook proof is absent")
    lease_bytes=sources["save-lease-hooks1954.json"];lease_audit=json.loads(lease_bytes)
    require(lease_audit==qa.get("save_lease_hook_audit")
            and sha(lease_bytes)==qa.get("save_lease_hook_audit_sha256")==desktop.get("save_lease_hook_audit_sha256")
            and lease_audit.get("schema")=="ulike-save-lease-hooks1954-v1" and lease_audit.get("status")=="passed"
            and lease_audit.get("inverse_bytecode_verified") is True and lease_audit.get("serialized_dex_verified") is True
            and lease_audit.get("recycle_call_count")==4 and len(lease_audit.get("target_methods",[]))==2
            and lease_audit.get("baseline_runtime_sha256")==sha(old_single[RUNTIME])
            and lease_audit.get("updated_runtime_sha256")==sha(single[RUNTIME]),
            "Packaged final-save lease hook inverse proof differs from exact MPP payloads")
    source_hashes = qa.get("source_sha256")
    require(isinstance(source_hashes, dict) and set(expected["required_source_paths"]) <= source_hashes.keys(),
            "Missing reviewed source checksums")
    for name, digest in source_hashes.items():
        checked_path(name)
        require(name in sources and sha(sources[name]) == digest, "Source package differs from QA: " + name)
        if name.startswith(("src/", "publication/")):
            reviewed = repo / RELEASE_ROOT / name
            require(reviewed.is_file() and reviewed.read_bytes() == sources[name],
                    "Packaged source differs from checked-out review commit: " + name)
    require(not any(PurePosixPath(name).suffix.casefold() in (".apk", ".apks", ".aab", ".jks", ".keystore") for name in sources), "Source archive contains application binary or signing key")
    notes = payloads["RELEASE_NOTES.txt"].decode()
    require(all(value in notes for value in (VERSION, PREVIOUS_APP, BUNDLE, "撮影", "合成", "ノイズ", "補正", "圧縮", "保存", "元APKS", "未確認", "再適用")),
            "Release notes omit scope, prior version repatch, or device uncertainty")
    require("元APKSへの今回の適用は未実施" in notes and "実機" in notes,
            "Release notes must disclose both unverified application and physical device")
    require(not re.search(r"\b(?:TODO|PLACEHOLDER|TBD)\b|レビュー後に確定", notes), "Unfilled release notes")
    report = {"bundle_changed_entries": sorted(changed), "bundle_added_entries": sorted(added),
              "standalone_changed_entries": sorted(single_changed), "standalone_added_entries": sorted(single_added),
              "non_ulike_resources_unchanged": len(other(new)), "ulike_resources_identical": len(own(new)),
              "native_methods_byte_identical": native_unchanged, "native_methods_tsv_byte_identical": inventory_unchanged,
              "baseline_other_app_resources_byte_identical": True,
              "qa_assertions_verified": list(expected["qa_required_values"])}
    return payloads, notes, report


def validate_policy(raw):
    policy = json.loads(raw)
    require(policy.get("ulike_version") == PREVIOUS_APP, "Active ULike advanced")
    require(policy.get("source_sha256") == SINGLE_BASE_SHA256, "Active ULike source bytes differ")
    require(policy.get("approved_lineage_version") == "1.8.8" and "1.9.17" in policy.get("withdrawn_versions", []), "Approved ULike lineage differs")
    return policy


def new_policy(old, expected):
    """Retain the reviewed quality policy; add actual per-shot timing coverage."""
    qa = expected["qa_required_values"]
    policy = {**old, "ulike_version": VERSION, "bundle_version": BUNDLE, "source_filename": SINGLE,
              "source_sha256": expected["artifacts"][SINGLE]["sha256"], "source_release": TAG,
              "active_release": TAG, "base_ulike_version": PREVIOUS_APP,
              "base_bundle_version": PREVIOUS, "base_source_sha256": SINGLE_BASE_SHA256,
              "reason": "H22-H27 optimize measured dispatch rows, private readback, constant policies, exact geometry caches, fresh halo suffix transfer, lossless overflow expansion and frozen probe ownership. Fresh host/source/package checks pass; original APKS application and Galaxy speed/quality are unverified.",
              "future_ulike_base": "Use ULike1.9.54 H22-H27 from approved1.8.8 lineage and exact1.9.53 baseline. Preserve H1-H15 and G10 exactness, final-finishing-stage transfer-inclusive speed admission, integer precision, immutable captured options and CPU fallback on unsupported/error/mismatch/slower GPU. Preserve output resolution, compression, beauty/color, capture reference, up to four-frame fusion, FIFO encoding, codec ownership and per-shot timing. Background preparation must respect idle/cancel/memory ownership. Validate original APKS and Galaxy hardware separately.",
              "selected_candidates": ["H22","H23","H24","H25","H26","H27"],
              "retained_baseline_candidates": {"ulike_version":"1.9.53","candidates":["H1","H2","H3","H4","H5","H6","H7","H8","H9","H10","H11","H12","H13","H14","H15","H16","H17","H18","H19","H20","H21","G10","G1","G3","G5","G6","G9","M1","M2","M4","M8","M3","M6","M7","M10"]},
              "current_fix_scope": ["H22","H23","H24","H25","H26","H27"],
              "release_status": "h22_h27_host_verified_original_apks_unverified",
              "gpu_processing_added":True,"gpu_execution_on_physical_android":False,"gpu_shader_execution_on_host":True,
              "gpu_policy":"H22-H27 exact dispatch, private readback, policy/geometry reuse, halo transfer and frozen ownership admission with CPU fallback",
              "gpu_native_library_sha256":expected["gpu_native_library_sha256"],"gpu_native_library_bytes":expected["gpu_native_library_bytes"],
              "core_native_library_sha256":expected["core_native_library_sha256"],"core_native_library_bytes":expected["core_native_library_bytes"],
              "moire_native_library_sha256":expected["moire_native_library_sha256"],"moire_native_library_bytes":expected["moire_native_library_bytes"],
              "h8gpu_native_library_sha256":expected["h8gpu_native_library_sha256"],"h8gpu_native_library_bytes":expected["h8gpu_native_library_bytes"],
              "finishgpu1952_native_library_sha256":expected["finishgpu1952_native_library_sha256"],"finishgpu1952_native_library_bytes":expected["finishgpu1952_native_library_bytes"],
              "finishgpu_native_library_sha256":expected["finishgpu_native_library_sha256"],"finishgpu_native_library_bytes":expected["finishgpu_native_library_bytes"],
              "android_device_tested": False, "device_quality_verified": False,
              "original_apk_apply_tested": expected["original_apk_apply_tested"],
              "ci_rebuild_matches_checked_out_source": True,
              "ci_rebuild_matches_local_tested_artifacts": False,
              "all_existing_runtime_unchanged": False,
              "baseline_other_apps_preserved": True,
              "diagnostic_stage_names": DIAGNOSTIC_STAGES,
              "diagnostic_overlapping_wall_clock": True,
              "diagnostic_measurement_scope": "Saved-image fusion/noise/additional correction/compression/save intervals; encoder start-to-stop excludes construction/prewarm, and native standard beauty and capture wait are outside stage measurements. Overall elapsed starts at trace admission, not necessarily at the shutter button."}
    for key, value in qa.items():
        if "." not in key and (key.endswith("_byte_identical") or key.endswith("_preserved")
                              or key.startswith("diagnostic_") or key.startswith("legacy_report_")):
            policy[key] = value
    for key in ("black_tap_native_switch", "black_tap_disabled", "front_input_readiness_timeout_ms",
                "shutter_feedback_duration_ms", "shutter_feedback_device_tested"):
        require(policy.get(key) == old.get(key) and (key in policy) == (key in old),
                "Inherited camera/feedback duration policy changed: " + key)
    require(policy.get("shutter_feedback_duration_ms") == 320, "320ms visible feedback duration required")
    return policy


def run(command):
    subprocess.run(list(map(str, command)), check=True)


def verify_assets(release_id, expected, public):
    release = api(f"repos/{REPO}/releases/{release_id}")
    require(release["tag_name"] == TAG and release["draft"] is not public and release["prerelease"] is True,
            "Release identity or visibility differs")
    assets = {asset["name"]: asset for asset in release["assets"]}
    require(set(assets) == set(expected) and len(assets) == len(release["assets"]), "Release asset inventory differs")
    for name, row in expected.items():
        asset = assets[name]
        require(asset["size"] == row["bytes"] and asset["state"] == "uploaded", "Release asset size/state differs: " + name)
        if asset.get("digest"):
            require(asset["digest"] == "sha256:" + row["sha256"], "GitHub asset digest differs: " + name)
        raw = fetch(asset["browser_download_url"]) if public else subprocess.check_output(
            ["gh", "api", f"repos/{REPO}/releases/assets/{asset['id']}", "-H", "Accept: application/octet-stream"])
        require(len(raw) == row["bytes"] and sha(raw) == row["sha256"], "Downloaded release bytes differ: " + name)


def publication(args):
    expected = load_expected(args.expected)
    baseline = args.baseline.read_bytes() if args.baseline else fetch(BASE_URL)
    standalone_baseline = args.standalone_baseline.read_bytes() if args.standalone_baseline else fetch(SINGLE_BASE_URL)
    payloads, notes, report = validate_local(args.dist, expected, baseline, standalone_baseline, args.repo)
    if args.local_only:
        print(json.dumps({"status": "local_artifacts_verified", **report}, ensure_ascii=False, indent=2))
        return
    states = {branch: snapshot(branch) for branch in ("main", "dev")}
    check_heads(states)
    require(states["main"]["manifest"] == states["dev"]["manifest"], "main/dev baseline feeds differ")
    require(all(state["manifest"]["version"] == PREVIOUS for state in states.values()), "Active bundle has advanced")
    if os.environ.get("GITHUB_SHA"):
        require(states["main"]["head"] == os.environ["GITHUB_SHA"], "main moved after this workflow checkout")
        require(git(args.repo, "rev-parse", "HEAD") == os.environ["GITHUB_SHA"], "Checkout differs from workflow source")
    require(sha(fetch(states["main"]["manifest"]["download_url"])) == BASE_SHA256, "Active bundle bytes differ")
    policies = {branch: validate_policy(content(POLICY_PATH, state["head"])) for branch, state in states.items()}
    created = utc_created(states)
    for state in states.values():
        metadata(state, "https://example.invalid/preflight-only", created, notes)
    require(api(f"repos/{REPO}/releases/tags/{TAG}", absent=True) is None, "Release already exists; inspect before retry")
    require(api(f"repos/{REPO}/git/ref/tags/{TAG}", absent=True) is None, "Release tag already exists; inspect before retry")
    require(api(f"repos/{REPO}/git/ref/heads/{RAW_BRANCH}", absent=True) is None, "Distribution branch exists; inspect before retry")
    if args.preflight_only:
        print(json.dumps({"status": "preflight_passed_no_remote_writes", "heads": {b: s["head"] for b, s in states.items()}, **report}, ensure_ascii=False, indent=2))
        return
    receipt_path = args.dist / RECEIPT_NAME
    require(not receipt_path.exists(), "Prior publication receipt exists; inspect before retry")
    receipt = {"schema": "ulike1954-publication-receipt-v1", "ulike_version": VERSION, "bundle_version": BUNDLE,
               "baseline_bundle_version": PREVIOUS, "baseline_bundle_sha256": BASE_SHA256,
               "baseline_ulike_version": PREVIOUS_APP, "baseline_ulike_sha256": SINGLE_BASE_SHA256,
               "source_commit": os.environ.get("GITHUB_SHA") or git(args.repo, "rev-parse", "HEAD"),
               "publication_manifest_sha256": sha(args.expected.read_bytes()),
               "assets": expected["artifacts"], "local_validation": report, "android_device_tested": False,
               "original_apk_apply_tested": expected["original_apk_apply_tested"], "manager_parser_source": MANAGER_SOURCE,
               "manager_device_update_badge_observed": False, "operations": []}

    def checkpoint(status, **changes):
        receipt.update(status=status, **changes)
        receipt_path.write_bytes(json_bytes(receipt))
        print(status, flush=True)

    checkpoint("preflight_passed")
    try:
        check_heads(states)
        release = api(f"repos/{REPO}/releases", {"tag_name": TAG, "target_commitish": states["main"]["head"],
            "name": f"ULike v{VERSION} H22～H27 画素一致高速化 / Hiro Morphe v{BUNDLE}",
            "body": notes, "draft": True, "prerelease": True, "make_latest": "false"})
        checkpoint("draft_created", release_id=release["id"])
        for name in ASSETS:
            run(["gh", "release", "upload", TAG, "--repo", REPO, args.dist / name])
        verify_assets(release["id"], expected["artifacts"], False)
        checkpoint("draft_assets_verified")
        check_heads(states)
        api(f"repos/{REPO}/releases/{release['id']}", {"draft": False, "prerelease": True, "make_latest": "false"}, "PATCH")
        verify_assets(release["id"], expected["artifacts"], True)
        checkpoint("release_bytes_verified")
        check_heads(states)
        git(args.repo, "fetch", "--no-tags", f"https://github.com/{REPO}.git", states["main"]["head"])
        raw_state = prepare_commit(args.repo, {**states["main"], "branch": RAW_BRANCH},
            {"downloads/" + name: payloads[name] for name in (SINGLE, COMBINED)},
            f"release: exact ULike1954 and bundle{BUNDLE} immutable MPP downloads")
        git(args.repo, "push", f"--force-with-lease=refs/heads/{RAW_BRANCH}:",
            f"https://github.com/{REPO}.git", f"{raw_state['head']}:refs/heads/{RAW_BRANCH}")
        require(head(RAW_BRANCH) == raw_state["head"], "Distribution branch moved")
        immutable_urls = {name: f"https://raw.githubusercontent.com/{REPO}/{raw_state['head']}/downloads/{name}" for name in (SINGLE, COMBINED)}
        for name, url in immutable_urls.items():
            require(fetch(url) == payloads[name], "Immutable download mismatch: " + name)
        checkpoint("immutable_downloads_verified", raw_commit=raw_state["head"], download_urls=immutable_urls)
        check_heads(states)
        new_metadata, updates, expected_policies = {}, {}, {}
        for branch, state in states.items():
            feed, changelog = metadata(state, immutable_urls[COMBINED], created, notes)
            new_metadata[branch] = (feed, changelog)
            policy = new_policy(policies[branch], expected)
            expected_policies[branch] = policy
            updates[branch] = {"patches-bundle.json": json_bytes(feed), "CHANGELOG.md": changelog.encode(), POLICY_PATH: json_bytes(policy)}
            if branch == "main":
                for name in (QA_NAME, "RELEASE_NOTES.txt", "SHA256SUMS.txt"):
                    updates[branch][RELEASE_ROOT + "/" + name] = payloads[name]
        active = commit_feeds_atomically(args.repo, states, updates,
            f"release: ULike1954 H22-H27 noise/correction optimization and bundle{BUNDLE} Manager update")
        for branch, state in active.items():
            feed, changelog = new_metadata[branch]
            require(json.loads(content("patches-bundle.json", state["head"])) == feed, "Remote feed mismatch")
            require(content("CHANGELOG.md", state["head"]).decode() == changelog, "Remote changelog mismatch")
            require(json.loads(content(POLICY_PATH, state["head"])) == expected_policies[branch], "Remote ULike policy mismatch")
            require(fetch(feed["download_url"]) == payloads[COMBINED], "Manager MPP bytes mismatch")
            require(json.loads(fetch(f"https://raw.githubusercontent.com/{REPO}/{branch}/patches-bundle.json?ulike1954={state['head']}")) == feed, "Public branch feed mismatch")
            require(fetch(f"https://raw.githubusercontent.com/{REPO}/{branch}/CHANGELOG.md?ulike1954={state['head']}").decode() == changelog, "Public changelog mismatch")
            receipt["operations"].append({"branch": branch, "commit": state["head"], "feed_verified": True,
                "changelog_verified": True, "ulike_policy_verified": True, "manager_download_verified": True})
        check_heads(active)
        verify_assets(release["id"], expected["artifacts"], True)
        checkpoint("published_and_verified", published=True, release_url=PAGE, manager_main_dev_updated_atomically=True,
            manager_ulike_update_eligible=True, previous_ulike1950_repatch_eligible=True,
            other_apps_false_updates=False, baseline_other_apps_preserved=True, fixed_chatgpt_site_updated=False)
        receipt_bytes = receipt_path.read_bytes()
        saved = commit_feeds_atomically(args.repo, active,
            {branch: {RELEASE_ROOT + "/" + RECEIPT_NAME: receipt_bytes} for branch in active},
            "docs: verified ULike1954 publication receipt")
        for branch, state in saved.items():
            require(content(RELEASE_ROOT + "/" + RECEIPT_NAME, state["head"]) == receipt_bytes, "Receipt commit mismatch")
            require(json.loads(content(POLICY_PATH, state["head"])) == expected_policies[branch], "ULike policy changed while saving receipt")
            require(json.loads(content("patches-bundle.json", state["head"])) == new_metadata[branch][0],
                    "Feed changed while saving receipt")
        run(["gh", "release", "upload", TAG, "--repo", REPO, receipt_path])
        published_assets = {**expected["artifacts"], RECEIPT_NAME: {"bytes": len(receipt_bytes), "sha256": sha(receipt_bytes)}}
        verify_assets(release["id"], published_assets, True)
        check_heads(saved)
        print("PASS exact Release and immutable MPP bytes, atomic main/dev feeds, ULike scope, baseline bundle other-app retention and saved receipt")
    except BaseException as error:
        checkpoint("failed_requires_inspection", error=type(error).__name__ + ": " + str(error))
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dist", required=True, type=Path)
    parser.add_argument("--repo", required=True, type=Path)
    parser.add_argument("--expected", required=True, type=Path)
    parser.add_argument("--baseline", type=Path)
    parser.add_argument("--standalone-baseline", type=Path)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--local-only", action="store_true")
    mode.add_argument("--preflight-only", action="store_true")
    args = parser.parse_args()
    for key in ("dist", "repo", "expected", "baseline", "standalone_baseline"):
        if getattr(args, key) is not None:
            setattr(args, key, getattr(args, key).resolve())
    require(not args.local_only or (args.baseline is not None and args.standalone_baseline is not None), "--local-only requires both local baselines")
    publication(args)


if __name__ == "__main__":
    main()
