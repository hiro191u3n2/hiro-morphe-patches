#!/usr/bin/env python3
"""Package current .124 source and actual local evidence without private APKs.

This creates no remote state. Runtime sources are never edited by packaging.
The publication workflow independently reconstructs the same source ZIP.
"""
from pathlib import Path
import argparse
import json
import shutil
import zipfile
import publish19125 as p

REQUIRED = {'background', 'cached', 'cleanup', 'diagnostic', 'gpu',
            'gpu-submission', 'policy', 'preview', 'quiet', 'responsiveness', 'stock-boost', 'resource-extraction'}
FAILURE_ONLY = {'background', 'diagnostic'}
MUTATION_ONLY = REQUIRED - FAILURE_ONLY - {'responsiveness', 'resource-extraction'}


def evidence(args):
    single = args.dist / p.SINGLE
    digest = p.file_row(single)
    build = p.read_json(args.build_report)
    application = p.read_json(args.application)
    abi = p.read_json(args.abi)
    p.require(build['artifact'] == {'name': p.SINGLE, **digest}, 'Build targets different artifact')
    p.require(build['version'] == p.VERSION and build['baseline']['sha256'] == p.SINGLE_BASE_SHA256,
              'Build version/baseline mismatch')
    for flag in ('deterministic_dex_verified', 'deterministic_archive_verified',
                 'native_assets_byte_identical', 'stock_methods_and_contracts_byte_identical',
                 'stock_method_original_contracts_preserved', 'helper_references_verified'):
        p.require(build.get(flag) is True, 'Build proof missing: ' + flag)
    p.verify_build_preservation(build)
    p.require(build['native_payload_count'] == 14, 'Expected fourteen preserved native payloads')
    p.verify_source_hashes(build, 'compiled_input_sha256', args.source / 'src')
    p.verify_application(application, digest['sha256'], 'Standalone')
    p.verify_source_hashes(application, 'validator_sources', args.source / 'src')
    p.require(application['application_script_sha256'] ==
              p.sha((args.source / 'src/apply_original19125.py').read_bytes()), 'Application script changed')
    p.require(abi == application['preview_original_apk_abi'], 'Single-APK ABI proof differs')
    p.require(abi['mpp_sha256'] == digest['sha256'] and
              abi['apk_sha256'] == application['output_apk']['sha256'] and
              abi['exact_reflection_chain_verified'] is True and
              abi['display_only_endpoint_verified'] is True and
              abi['physical_android_tested'] is False, 'Single-APK reflection proof invalid')
    p.verify_source_hashes(abi, 'source_sha256', args.source / 'src')
    p.require(set(args.host_reports) == REQUIRED, 'Exactly the twelve reviewed host suites are required')
    suites, raw, mutations, failures = {}, {}, 0, 0
    for name, path in sorted(args.host_reports.items()):
        report = p.read_json(path)
        summary = p.host_summary(report)
        p.verify_source_hashes(report, 'source_sha256', args.source / 'src')
        raw[name] = report
        if name in FAILURE_ONLY:
            mutation, failed_condition = 0, summary['negative_controls']
        elif name == 'resource-extraction':
            mutation, failed_condition = report.get('mutation_controls'), report.get('failed_condition_controls')
            p.require(type(mutation) is int and type(failed_condition) is int and mutation >= 0 and failed_condition >= 0 and
                      mutation + failed_condition == summary['negative_controls'], 'Resource suite must distinguish mutations and expected failures')
        elif name in MUTATION_ONLY:
            mutation, failed_condition = summary['negative_controls'], 0
        else:
            names = report.get('mutation_control_names')
            p.require(isinstance(names, list) and len(names) == len(set(names)),
                      'Responsiveness must distinguish mutation from controlled failures')
            mutation = len(names)
            failed_condition = summary['negative_controls'] - mutation
            p.require(failed_condition >= 0, 'Invalid responsiveness control count')
        suites[name] = {**summary, 'evidence': 'evidence/host/' + name + '.json',
                        'mutation_controls': mutation, 'failed_condition_controls': failed_condition}
        mutations += mutation; failures += failed_condition
    return digest, build, application, abi, suites, raw, mutations, failures


def rebuild_document():
    return """# ULike v1.9.125 source and QA

This package rebuilds ULike v1.9.125 with exact public v1.9.124 as baseline. Matching bundle: 1.0.338 over unchanged active bundle 1.0.337. Android execution, displayed responsiveness and visual quality remain unmeasured.

The existing guarded HD preview release is enabled after save input protection. Capture generation, image ownership, activity/lifecycle, camera route and source bitmap guards remain. Preview release requests and later input notices are distinct measurements; neither proves displayed frame completion. Numerical image-processing helpers, output resolution/formats, stock methods, CPU/GPU scheduling, disabled automatic qualification, fourteen native payloads and the .124 resource extraction fix remain inherited.

Build with Temurin21.0.8+9-LTS and pinned compiler tools:

```sh
python3 src/build19125.py --baseline baseline124/ULike_HQ_Texture_Online_v1.9.124.mpp --tools "$ULIKE_TOOLS" --jdk "$ULIKE_JDK" --families preview-families19125.json --work "$ULIKE_BUILD_WORK" --output "$ULIKE_OUTPUT"
```

Run the publication/ci-suites19125.json plan. Assertion totals count checks; mutation and controlled-failure counts are kept distinct. The resource suite runs the actual .125 candidate against real modern ResourcePatchContext. Its single display description is normalized to the prior display metadata solely for the historical complete-code inverse; the three resource reads and every guard/instruction remain verified, including ten structural mutations and six controlled failures.

Use src/apply_original19125.py and publication/apply_combined19125.py with modern Morphe Desktop2.0.0-dev.4 / embedded patcher2.2.0-dev.2. --validator-tools uses the pinned historical serializer only for canonical contract checks. Actual patching uses current on-request decoding and FULL DEX rebuild. All 217 original assets, three replaced asset ZIPs, native payloads, helper type/member linkage and the reflection chain must pass. Original and patched APKs, raw logs and toolchains are excluded.

Root QA/current evidence describe .125. evidence/reproduction19124.json is the historical .124 patcher/resource regression evidence, not a new .125 user-device reproduction. manifest.json binds every packaged file. Source ZIP ordering, timestamp2026-11-03, mode100644 and DEFLATE are deterministic. Publication CI reconstructs the full source tree from hash-pinned public .124 bytes plus a fully pinned delta, reproduces both MPPs/source ZIP and reruns every declared host suite before atomic Manager feed updates.
"""


def preserve_prior_metadata(source):
    prior = source / 'inherited124'
    names = ['QA_ULike_v1.9.124.json', 'REBUILD_v1.9.124.md', 'RELEASE_NOTES_v1.9.124.txt', 'stockboost-families19123.json']
    manifest = source / 'manifest.json'
    if manifest.is_file() and p.read_json(manifest).get('version') == '1.9.124': names.append('manifest.json')
    if (source / 'evidence/build19123.json').is_file() and not (source / 'evidence/build19125.json').exists(): names.append('evidence')
    for name in names:
        path = source / name
        if not path.exists(): continue
        target = prior / name
        p.require(not target.exists(), 'Prior metadata archive already exists: ' + name)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(path), str(target))


def package(args):
    digest, build, application, abi, suites, reports, mutations, failures = evidence(args)
    preserve_prior_metadata(args.source)
    evidence_dir = args.source / 'evidence'
    evidence_dir.mkdir(exist_ok=True)
    (evidence_dir / 'host').mkdir(exist_ok=True)
    for path, name in ((args.build_report, 'build19125.json'),
                       (args.application, 'application19125.json'), (args.abi, 'native-abi19125.json')):
        shutil.copyfile(path, evidence_dir / name)
    for name, path in args.host_reports.items():
        shutil.copyfile(path, evidence_dir / 'host' / (name + '.json'))
    reproduction = p.read_json(args.reproduction)
    p.verify_reproduction(reproduction)
    shutil.copyfile(args.reproduction, evidence_dir / 'reproduction19124.json')
    helpers = application['all_apk_helper_types']
    p.require(helpers['status'] == 'passed' and helpers['missing_helper_types'] == 0,
              'Applied helper type validation failed')
    qa = {
        'schema': 'ulike19125-source-and-qa-v1', 'status': 'passed', 'version': p.VERSION,
        'artifact': {'name': p.SINGLE, **digest}, 'baseline': build['baseline'],
        'fix_scope': ['enable_guarded_hd_preview_release_after_save_input_protection', 'separate_preview_release_request_and_input_resumption', 'preserve_pixel_arithmetic_and_capture_ownership'],
        'resource_extraction_fix_inherited': True,
        'resource_extraction_applied': application['resource_extraction'],
        'reproduction': reproduction,
        'stock_boost_applied': application['stock_boost_applied'],
        'stock_methods_and_contracts_byte_identical': True,
        'stock_method_original_contracts_preserved': True,
        'stock_boost_suppression_preserved': True,
        'automatic_qualification_enabled': False, 'unqualified_gpu_admission_enabled': False,
        'normal_pixel_algorithms_changed': False, 'default_guarded_hd_preview_release_enabled': True,
        'diagnostic_quality_pipeline_bypass_available': True,
        'diagnostic_bypass_requires_explicit_one_shot_selection': True,
        'pixel_preservation_scope': 'All fourteen native payloads, numerical image-processing helpers, stock methods and asset contracts are preserved from v1.9.124. Seven reviewed preview/diagnostic families and display/version metadata change. This is source/asset preservation evidence, not device visual-quality comparison.',
        'preview_scope': 'Existing guarded early HD release is enabled by default after save input protection. Capture ownership remains held until normal finish. Release request timing and subsequent camera input notices are separate observations, neither proving displayed frame completion. Host lifecycle/ownership gates and actual applied reflection chain are checked.',
        'observation_scope': 'Bounded capture/post-save callback intervals, first notification delay, terminal-main callback waiting/work, returned cache capacities and available thermal state. A missing observation is not a zero delay. The runtime measurement was host-tested; no physical-device measurement is included.',
        'native_assets_byte_identical': True, 'native_payloads_preserved': build['native_payload_count'],
        'installer_byte_identical': True, 'resource_extraction_fix_inherited': True, 'deterministic_archive_verified': True,
        'deterministic_dex_verified': True,
        'compiled_input_count': len(build['compiled_input_sha256']),
        'compiled_input_sha256': build['compiled_input_sha256'],
        'all_compiled_inputs_match_packaged_sources': True,
        'all_host_source_pins_match_packaged_sources': True,
        'application_validator_source_pins_match_packaged_sources': True,
        'host_suites': suites, 'host_assertions': sum(x['assertions'] for x in suites.values()),
        'host_negative_controls': mutations + failures,
        'stock_boost_structural_checks': reports['stock-boost']['structural_checks'],
        'stock_boost_structural_checks_scope': 'Static structural checks from the stock-boost suite; reported separately and not added to the functional host assertion total.',
        'host_mutation_controls': mutations, 'host_failed_condition_controls': failures,
        'host_count_scope': 'Twelve host suites. Assertions are checks, not test cases. Standalone-applied APK reflection assertions are separate and are not added to host_assertions.',
        'negative_control_scope': 'Mutation controls deliberately alter implementation and must be rejected. Failed-condition controls exercise unavailable APIs, hidden/stale objects or other expected failures. Counts are derived from the named reviewed suites and are not all mutations.',
        'original_apks_apply_tested': True, 'full_dex_compilation_tested': True,
        'exact_applied_runtime_contracts': application['expected_runtime_contracts'],
        'all_apk_helper_member_linkage_count': application['all_apk_linkage_count'],
        'all_apk_helper_references_verified': True, 'all_apk_helper_types_verified': True,
        'preview_original_apk_abi': abi,
        'physical_android_tested': False, 'device_performance_measured': False,
        'device_speedup_verified': False, 'device_quality_verified': False,
        'signing_or_installation_tested': False, 'original_or_patched_apk_included': False,
        'build_evidence_scope': 'The immutable build-stage report precedes host/application runs. Completed reports listed here supersede its pending host/application flags.',
        'source_provenance': {'public_baseline_version': '1.9.124',
            'public_baseline_sha256': p.SINGLE_BASE_SHA256,
            'public_baseline_release': 'ulike-v1.9.124',
            'prior_metadata_directory': 'inherited124'},
        'evidence': {'build': 'evidence/build19125.json', 'single_application': 'evidence/application19125.json',
                     'single_applied_preview_abi': 'evidence/native-abi19125.json', 'historical_resource_reproduction': 'evidence/reproduction19124.json'},
        'limitations': ['No physical Android execution or observed responsiveness improvement.',
                       'No device thermal, memory-return, GPU-utilization or visual-quality claim.',
                       'The suppressed stock performance request might reduce or increase save latency; device behavior is unmeasured.',
                       'The 3000ms HD value is a requested CPU boost duration, not a sleep or demonstrated UI blockage.',
                       'Actual device acceptance of the prior boost request and causality of the reported lag are unverified.',
                       'Model GPU batch arithmetic and partitioning remain unchanged.',
                       'Idle GPU trimming covers the Noise engine. Finish-specific GPU pools keep their existing native retention because no separate safe idle trim API exists; not all GPU retained capacity is reclaimed.',
                       'One-shot diagnostic processing bypass intentionally changes image quality.'],
    }
    (args.source / p.QA_NAME).write_bytes(p.json_bytes(qa))
    (args.source / 'REBUILD_v1.9.125.md').write_text(rebuild_document())
    notes = args.source / 'RELEASE_NOTES_v1.9.125.txt'
    p.require(notes.is_file() and p.VERSION in notes.read_text() and p.BUNDLE in notes.read_text(),
              'Reviewed current release notes missing')
    files = {}
    for path in sorted(args.source.rglob('*')):
        if not path.is_file():
            continue
        relative = path.relative_to(args.source)
        if relative.as_posix() == 'manifest.json' or '__pycache__' in relative.parts or '.git' in relative.parts or path.suffix in ('.pyc', '.pyo'):
            continue
        p.require(not path.is_symlink(), 'Source symlink forbidden')
        p.require(path.suffix.lower() not in ('.apk', '.apks', '.apkm', '.jar'), 'Private application/toolchain archive in source tree: ' + str(relative))
        p.require(relative.parts[0] not in ('private', 'toolchain', 'dist', 'build'), 'Private/generated work directory in source tree')
        p.checked_path(relative.as_posix())
        files[relative.as_posix()] = p.file_row(path)
    manifest = {'schema': 'ulike19125-source-manifest-v1', 'version': p.VERSION,
                'artifact': {'name': p.SINGLE, **digest}, 'files': files}
    (args.source / 'manifest.json').write_bytes(p.json_bytes(manifest))
    target = args.dist / p.SOURCE_ZIP
    names = sorted(set(files) | {'manifest.json'}, key=Path)
    with zipfile.ZipFile(target, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name in names:
            info = zipfile.ZipInfo(name, (2026, 11, 3, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED; info.external_attr = 0o100644 << 16
            archive.writestr(info, (args.source / name).read_bytes())
    with zipfile.ZipFile(target) as archive:
        p.require(archive.testzip() is None and set(archive.namelist()) == set(names), 'Source ZIP closure differs')
        for name, row in files.items():
            raw = archive.read(name)
            p.require(row == {'bytes': len(raw), 'sha256': p.sha(raw)}, 'Packaged source changed: ' + name)
    if args.stage_source:
        p.require(not args.stage_source.exists(), 'Use a fresh canonical staged source directory')
        for name in names:
            destination = args.stage_source / name
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(args.source / name, destination)
    print(json.dumps({'status': 'packaged_no_remote_writes', 'source_zip': p.file_row(target),
          'source_files': len(files), 'host_assertions': qa['host_assertions'],
          'mutation_controls': mutations, 'failed_condition_controls': failures,
          'physical_android_tested': False}, indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('source', 'dist', 'build-report', 'application', 'abi', 'reproduction'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--host-report', action='append', default=[], metavar='NAME=PATH')
    parser.add_argument('--stage-source', type=Path)
    args = parser.parse_args()
    for name, value in vars(args).items():
        if isinstance(value, Path):
            setattr(args, name, value.resolve())
    args.host_reports = p.parse_host_specs(args.host_report)
    package(args)


if __name__ == '__main__':
    main()
