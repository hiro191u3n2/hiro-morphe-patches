#!/usr/bin/env python3
"""Verify the local original-APKS application against the exact rebuilt MPP."""
from pathlib import Path
import hashlib
import json

REPORT = 'tests1988/original-application.json'
ORIGINAL_SHA256 = '73c6d3a3008b9975645f63238f07dc9c1960ad982c70f141ee4b5dfe60f7a293'
MORPHE_SHA256 = '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c'
VALIDATOR_SOURCES = ('MergePayloads.java', 'VerifyApplied1920.java',
                     'VerifyAllHelperReferences1980.java', 'tests1981/VerifyAppliedTypes1981.java')

def read(source, artifacts):
    source = Path(source)
    path = source / REPORT
    raw = path.read_bytes()
    data = json.loads(raw)
    if data.get('schema') != 'ulike1988-original-apks-application-v1' or data.get('status') != 'passed':
        raise AssertionError('Positive original-APKS application evidence required')
    if data.get('artifact_stage') != 'final':
        raise AssertionError('Final candidate original-APKS application required')
    source_hashes = {name: hashlib.sha256((source / name).read_bytes()).hexdigest()
                     for name in VALIDATOR_SOURCES}
    if data.get('validator_sources') != source_hashes:
        raise AssertionError('Original-APKS validator sources changed after application')
    if data.get('application_script_sha256') != hashlib.sha256((source / 'apply_original1988.py').read_bytes()).hexdigest():
        raise AssertionError('Original-APKS application script changed after application')
    if (data.get('morphe_jar', {}).get('sha256') != MORPHE_SHA256
            or data.get('java_toolchain') != {'runtime_version':'21.0.8+9-LTS',
                'vendor':'Eclipse Adoptium', 'compiler_version':'21.0.8'}):
        raise AssertionError('Original-APKS application used a different toolchain')
    if data.get('original_input') != {'bytes':89511266, 'sha256':ORIGINAL_SHA256}:
        raise AssertionError('Original APKS identity differs')
    mpp = data.get('mpp', {})
    name = 'ULike_HQ_Texture_Online_v1.9.88.mpp'
    if mpp.get('filename') != name or {k:mpp.get(k) for k in ('bytes','sha256')} != artifacts.get(name):
        raise AssertionError('Application evidence does not cover these exact rebuilt MPP bytes')
    for key in ('original_apk_apply_tested', 'original_split_merge_tested'):
        if data.get(key) is not True:
            raise AssertionError('Original application stage not executed: ' + key)
    for key in ('device_tested', 'physical_android_tested', 'signing_or_installation_tested',
                'ci_android_apply_tested', 'output_publicly_distributed'):
        if data.get(key) is not False:
            raise AssertionError('Unperformed physical/installation validation claimed: ' + key)
    for key in ('expected_runtime_contracts', 'all_apk_linkage_count'):
        if type(data.get(key)) is not int or data[key] <= 0:
            raise AssertionError('Actual APK inspection missing: ' + key)
    for key in ('actual_split_merge_tested', 'full_dex_compilation_tested',
                'resource_compilation_tested', 'apk_alignment_tested',
                'arm64_only_verified', 'all_patching_steps_succeeded', 'output_unsigned'):
        if data.get(key) is not True:
            raise AssertionError('Actual APK application stage missing: ' + key)
    if (data.get('package_name') != 'com.gorgeous.liteinternational'
            or data.get('package_version') != '5.6.2'
            or data.get('original_native_library_count') != 58
            or data.get('original_native_libraries_byte_identical') != 57
            or len(data.get('installed_patch_native_assets', {})) != 12):
        raise AssertionError('Original/appended native or package scope differs')
    types = data.get('all_apk_helper_types', {})
    if types.get('status') != 'passed' or types.get('missing_helper_types') != 0:
        raise AssertionError('All-APK helper type and reflection validation missing')
    if data.get('applied_patch_names') != ['高画質撮影・質感美肌・素材通信を復旧']:
        raise AssertionError('Original-APKS application patch selection differs')
    # Raw APKs and logs stay private; the source graph contains this small,
    # sanitized report. CI reproduces the MPP and verifies the same bytes.
    return {'status':'passed', 'report_path':REPORT,
            'report_sha256':hashlib.sha256(raw).hexdigest(),
            'executed_locally':True, 'ci_repeated':False,
            'physical_android_tested':False, 'report':data}
