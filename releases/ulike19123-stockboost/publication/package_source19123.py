#!/usr/bin/env python3
"""Package current .123 source and actual local evidence without private APKs.

This creates no remote state. Runtime sources are never edited by packaging.
The publication workflow independently reconstructs the same source ZIP.
"""
from pathlib import Path
import argparse
import json
import shutil
import zipfile
import publish19123 as p

REQUIRED = {'background', 'cached', 'cleanup', 'diagnostic', 'gpu',
            'gpu-submission', 'policy', 'preview', 'quiet', 'responsiveness', 'stock-boost'}
FAILURE_ONLY = {'background', 'diagnostic'}
MUTATION_ONLY = REQUIRED - FAILURE_ONLY - {'responsiveness'}


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
                 'native_assets_byte_identical', 'installer_byte_identical',
                 'stock_method_original_contracts_preserved', 'stock_boost_callsite_only_changed', 'helper_references_verified'):
        p.require(build.get(flag) is True, 'Build proof missing: ' + flag)
    p.require(build['native_payload_count'] == 14, 'Expected fourteen preserved native payloads')
    p.verify_source_hashes(build, 'compiled_input_sha256', args.source / 'src')
    p.verify_application(application, digest['sha256'], 'Standalone')
    p.verify_source_hashes(application, 'validator_sources', args.source / 'src')
    p.require(application['application_script_sha256'] ==
              p.sha((args.source / 'src/apply_original19123.py').read_bytes()), 'Application script changed')
    p.require(abi == application['preview_original_apk_abi'], 'Single-APK ABI proof differs')
    p.require(abi['mpp_sha256'] == digest['sha256'] and
              abi['apk_sha256'] == application['output_apk']['sha256'] and
              abi['exact_reflection_chain_verified'] is True and
              abi['display_only_endpoint_verified'] is True and
              abi['physical_android_tested'] is False, 'Single-APK reflection proof invalid')
    p.verify_source_hashes(abi, 'source_sha256', args.source / 'src')
    p.require(set(args.host_reports) == REQUIRED, 'Exactly the eleven reviewed host suites are required')
    suites, raw, mutations, failures = {}, {}, 0, 0
    for name, path in sorted(args.host_reports.items()):
        report = p.read_json(path)
        summary = p.host_summary(report)
        p.verify_source_hashes(report, 'source_sha256', args.source / 'src')
        raw[name] = report
        if name in FAILURE_ONLY:
            mutation, failed_condition = 0, summary['negative_controls']
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
    return '''# ULike v1.9.123 source and QA

This package contains the exact Java/native reference source closure and local evidence for ULike v1.9.123. The matching combined release is Hiro Morphe Patches v1.0.333. Physical Android execution, performance and visual quality are not claimed.

Use Temurin JDK 21.0.8+9 (21.0.8+9-LTS), the pinned Morphe 1.16.0 JAR, Android platform-35_r02 android.jar and build-tools_r35_linux d8.jar. Their SHA-256 pins and the observed JDK identity are in evidence/build19123.json. Toolchain binaries and original APKs are not included.

Build from this directory with fresh output/work directories:

```sh
python3 src/build19123.py --baseline baseline122/ULike_HQ_Texture_Online_v1.9.122.mpp --tools "$ULIKE_TOOLS" --jdk "$ULIKE_JDK" --families stockboost-families19123.json --work "$ULIKE_BUILD_WORK" --output "$ULIKE_OUTPUT"
```

The eleven host suites are background, cached restore, idle cleanup, diagnostic refresh, GPU completion wait, GPU submission, comparison policy, actual save integration/preview, quiet runtime, responsiveness, and stock-boost suppression/inverse verification. Exact commands and report paths are in the same GitHub release source tree at publication/ci-suites19123.json. Run every suite against the packaged src directory and the helper-classes from this exact build. Host assertions, mutation controls and controlled failure scenarios are reported separately in QA_ULike_v1.9.123.json; APK reflection assertions are not included in host totals.

For the private original-APKS application check, use src/apply_original19123.py with its --help options and the exact ULike 5.6.2 (740) APKS SHA-256 recorded in evidence/application19123.json. The APKS, merged/unsigned APK and private logs must remain outside this source tree. This rebuild does not sign, install or execute Android code on a device. The preview reflection proof is bound to the actual standalone-applied APK; it is not a device rendering test.

This revision suppresses only the optional stock still-photo CPU boost callsite. Normal image-processing arithmetic, output settings, existing CPU/GPU concurrency limits, post-save idle cache cleanup and bounded observations are preserved. The explicitly selected one-shot diagnostic bypass changes image processing and is not a normal-quality mode. Cached GPU certification does not authorize new unverified candidates. Automatic additional qualification stays disabled.

The v1.9.122 standalone baseline is pinned under baseline122. Earlier metadata and evidence remain under inherited122, inherited121 and inherited120 as historical provenance; only the current root QA and evidence directory describe this release. Build-stage pending host/application fields are superseded by the completed evidence listed in root QA.

manifest.json lists every ZIP entry except itself with bytes and SHA-256. The ZIP sorts entries by pathlib.Path, fixes timestamps to 2026-11-03 00:00:00, uses mode 100644 and DEFLATE. Publication CI reconstructs the ZIP and MPPs, reruns all eleven host suites, verifies original-APKS proofs and preserves unrelated combined-bundle applications before any feed update.
'''


def preserve_prior_metadata(source):
    prior = source / 'inherited122'
    names = ['QA_ULike_v1.9.122.json', 'REBUILD_v1.9.122.md', 'RELEASE_NOTES_v1.9.122.txt', 'responsive-families19122.json']
    manifest = source / 'manifest.json'
    if manifest.is_file() and p.read_json(manifest).get('version') == '1.9.122': names.append('manifest.json')
    if (source / 'evidence/build19122.json').is_file() and not (source / 'evidence/build19123.json').exists(): names.append('evidence')
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
    for path, name in ((args.build_report, 'build19123.json'),
                       (args.application, 'application19123.json'), (args.abi, 'native-abi19123.json')):
        shutil.copyfile(path, evidence_dir / name)
    for name, path in args.host_reports.items():
        shutil.copyfile(path, evidence_dir / 'host' / (name + '.json'))
    helpers = application['all_apk_helper_types']
    p.require(helpers['status'] == 'passed' and helpers['missing_helper_types'] == 0,
              'Applied helper type validation failed')
    qa = {
        'schema': 'ulike19123-source-and-qa-v1', 'status': 'passed', 'version': p.VERSION,
        'artifact': {'name': p.SINGLE, **digest}, 'baseline': build['baseline'],
        'fix_scope': ['suppress_stock_still_photo_cpu_boost_request',
                      'count_suppressed_requests_without_claiming_activation'],
        'stock_boost_patch': build['stock_boost_patch'],
        'stock_boost_applied': application['stock_boost_applied'],
        'stock_methods_and_contracts_byte_identical': False,
        'stock_method_original_contracts_preserved': True,
        'stock_boost_callsite_only_changed': True,
        'automatic_qualification_enabled': False, 'unqualified_gpu_admission_enabled': False,
        'normal_pixel_algorithms_changed': False,
        'diagnostic_quality_pipeline_bypass_available': True,
        'diagnostic_bypass_requires_explicit_one_shot_selection': True,
        'pixel_preservation_scope': 'All fourteen native payloads, pixel-processing helpers and installer are preserved from v1.9.122. Only the optional stock still-photo boost callsite changes in the stock payload; its complete inverse and all other methods are checked. This is source/asset preservation evidence, not a device visual-quality comparison.',
        'preview_scope': 'Early HD release is disabled by default and opt-in for one comparison. Host ownership/lifecycle gates and the standalone-applied original-APK reflection chain are checked. Main callback acceptance and camera input notices are not frame presentation or display FPS.',
        'observation_scope': 'Bounded capture/post-save callback intervals, first notification delay, terminal-main callback waiting/work, returned cache capacities and available thermal state. A missing observation is not a zero delay. The runtime measurement was host-tested; no physical-device measurement is included.',
        'native_assets_byte_identical': True, 'native_payloads_preserved': build['native_payload_count'],
        'installer_byte_identical': True, 'deterministic_archive_verified': True,
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
        'host_count_scope': 'Eleven host suites. Assertions are checks, not test cases. Standalone-applied APK reflection assertions are separate and are not added to host_assertions.',
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
        'source_provenance': {'public_baseline_version': '1.9.122',
            'public_baseline_sha256': p.SINGLE_BASE_SHA256,
            'public_baseline_source_commit': '18877f214d500a6ff8afcedd4bf3e21379248236',
            'prior_metadata_directory': 'inherited122'},
        'evidence': {'build': 'evidence/build19123.json', 'single_application': 'evidence/application19123.json',
                     'single_applied_preview_abi': 'evidence/native-abi19123.json'},
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
    (args.source / 'REBUILD_v1.9.123.md').write_text(rebuild_document())
    notes = args.source / 'RELEASE_NOTES_v1.9.123.txt'
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
    manifest = {'schema': 'ulike19123-source-manifest-v1', 'version': p.VERSION,
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
    for name in ('source', 'dist', 'build-report', 'application', 'abi'):
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
