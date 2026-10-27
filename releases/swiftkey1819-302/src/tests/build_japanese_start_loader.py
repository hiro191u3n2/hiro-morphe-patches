#!/usr/bin/env python3
"""Add startup hooks to pinned 1.8.18 loaders; optionally compose the candidate replacement."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import zipfile

PREFIX = 'app/hiro/swiftkey/patches/'
MAIN = PREFIX + 'JapaneseSamsungEmojiPatch'
HELPER = PREFIX + 'JapaneseStartPatch'
CANDIDATE = PREFIX + 'CandidateScrollPatch'
BASELINES = {
    'single': '35251f48280adc69108d0218b3a8f464686deeb44314f59195698bc6bc8583ef',
    'bundle': '3fbc234e9248d0e2682ee996a31cd37d5b0b079a080ad2b8fc2b65ce6e65f163',
}


def require(value, message):
    if not value:
        raise RuntimeError(message)


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def run(command, log):
    result = subprocess.run([str(x) for x in command], stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, text=True)
    log.parent.mkdir(parents=True, exist_ok=True)
    log.write_text(result.stdout)
    require(result.returncode == 0, 'Command failed: ' + str(log) + '\n' + result.stdout)
    return result.stdout


def smali(directory):
    return {p.relative_to(directory).as_posix(): p.read_bytes() for p in sorted(directory.rglob('*.smali'))}


def patch_classes(path):
    with zipfile.ZipFile(path) as z:
        return {n: z.read(n) for n in z.namelist() if n.startswith(PREFIX) and n.endswith('.class')}


def stage(baseline, output, replacement):
    with zipfile.ZipFile(baseline) as old, zipfile.ZipFile(output, 'w') as new:
        old_names = set(old.namelist())
        require(len(old_names) == len(old.infolist()), 'Duplicate MPP entries')
        for entry in old.infolist():
            new.writestr(entry, replacement.get(entry.filename, old.read(entry.filename)))
        for name, data in replacement.items():
            if name not in old_names:
                new.writestr(name, data)


def build(root, tools, work, standalone, bundle, apk=None, candidate_loader_dir=None):
    baselines = {'single': standalone, 'bundle': bundle}
    for label, path in baselines.items():
        require(sha(path.read_bytes()) == BASELINES[label], 'Unverified 1.8.18 baseline ' + label)
    before_jvm = patch_classes(standalone)
    require(len(before_jvm) == 11 and before_jvm == patch_classes(bundle), 'Baseline JVM inventory differs')
    require(HELPER + '.class' not in before_jvm, 'Startup helper already present')
    if work.exists():
        shutil.rmtree(work)
    for directory in ('jvm', 'helper_classes', 'tool_classes', 'helper_dex'):
        (work / directory).mkdir(parents=True)
    java21 = tools / 'jdk-21.0.8+9-jre/bin/java'
    java = os.environ.get('MORPHE_JAVA', str(java21) if java21.is_file() else 'java')
    cp = os.pathsep.join(map(str, (tools / 'morphe.jar', standalone)))
    compile_tool = ['java', 'com.sun.tools.javac.Main', '-g:none', '-encoding', 'UTF-8']
    source = root / 'patch_java' / (HELPER + '.java')
    run(compile_tool + ['--release', '8', '-cp', cp, '-d', work / 'helper_classes', source], work / 'javac-helper.log')
    compiled = sorted(p.relative_to(work / 'helper_classes').as_posix() for p in (work / 'helper_classes').rglob('*.class'))
    require(compiled == [HELPER + '.class'], 'Unexpected compiled startup patch inventory')
    helper_jar = work / 'japanese-start-helper.jar'
    with zipfile.ZipFile(helper_jar, 'w') as z:
        z.write(work / 'helper_classes' / (HELPER + '.class'), HELPER + '.class')
    baseline_classpath = work / 'baseline-patch-classpath.jar'
    with zipfile.ZipFile(baseline_classpath, 'w') as z:
        for name, data in before_jvm.items():
            z.writestr(name, data)
    run(['java', '-cp', tools / 'r8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26',
         '--lib', tools / 'android.jar', '--classpath', tools / 'morphe.jar', '--classpath', baseline_classpath,
         '--output', work / 'helper_dex', helper_jar], work / 'd8-helper.log')
    tool_cp = os.pathsep.join((str(work / 'helper_classes'), cp, str(tools / 'r8.jar')))
    run(compile_tool + ['-cp', tool_cp, '-d', work / 'tool_classes',
                       root / 'tools_java/UpdateJapaneseStartLoader.java',
                       root / 'tests/JapaneseStartPatchProbe.java'], work / 'javac-tools.log')
    run(compile_tool + ['-cp', tools / 'apktool.jar', '-d', work / 'tool_classes',
                       root / 'tools_java/Dump1814.java'], work / 'javac-dump.log')
    executable_cp = os.pathsep.join((str(work / 'tool_classes'), tool_cp))
    update = [java, '-cp', executable_cp, 'UpdateJapaneseStartLoader']
    run(update + ['jvm', standalone, work / 'helper_classes', work / 'jvm'], work / 'jvm-bridge.log')
    candidate_report = None
    composed = dict(baselines)
    if candidate_loader_dir:
        candidate_report = json.loads((candidate_loader_dir / 'report.json').read_text())
        require(candidate_report['status'] == 'PASSED', 'Candidate loader was not verified')
        require(candidate_report['baseline_mpp_sha256'] == BASELINES, 'Candidate loader baseline differs')
        replacement = candidate_loader_dir / 'jvm' / (CANDIDATE + '.class')
        candidate_class = replacement.read_bytes()
        require(sha(candidate_class) == candidate_report['jvm'][CANDIDATE + '.class']['sha256'], 'Candidate JVM output drift')
        for label, baseline in baselines.items():
            dex = (candidate_loader_dir / (label + '.dex')).read_bytes()
            require(sha(dex) == candidate_report['android'][label]['sha256'], 'Candidate Android output drift')
            composed[label] = work / (label + '-candidate-base.mpp')
            stage(baseline, composed[label], {CANDIDATE + '.class': candidate_class, 'classes.dex': dex})
        # Final output is complete and directly consumable by the root packager.
        (work / 'jvm' / (CANDIDATE + '.class')).write_bytes(candidate_class)
    dump = ['java', '-cp', os.pathsep.join(map(str, (work / 'tool_classes', tools / 'apktool.jar'))), 'Dump1814']
    android = {}
    for label, baseline in baselines.items():
        with zipfile.ZipFile(baseline) as z:
            (work / (label + '-before.dex')).write_bytes(z.read('classes.dex'))
        output = work / (label + '.dex')
        run(update + ['dex', composed[label], work / 'helper_dex/classes.dex', output], work / (label + '-bridge.log'))
        run(dump + [work / (label + '-before.dex'), work / (label + '-before')], work / (label + '-dump-before.log'))
        run(dump + [output, work / (label + '-after')], work / (label + '-dump-after.log'))
        before, after = smali(work / (label + '-before')), smali(work / (label + '-after'))
        require(set(after) == set(before) | {HELPER + '.smali'}, 'Android class inventory differs')
        expected_changed = {MAIN + '.smali'} | ({CANDIDATE + '.smali'} if candidate_loader_dir else set())
        changed = {n for n in before if before[n] != after[n]}
        require(changed <= expected_changed and MAIN + '.smali' in changed, 'Unrelated Android loader changed')
        pattern = (rb'(?m)^    invoke-static/range \{p0 \.\. p0\}, Lapp/hiro/swiftkey/patches/JapaneseStartPatch;->'
                   rb'(?:apply|checkOriginal)\(Lapp/morphe/patcher/patch/BytecodePatchContext;\)V\n\n')
        restored, count = re.subn(pattern, b'', after[MAIN + '.smali'])
        require(count == 2 and restored == before[MAIN + '.smali'], 'Main Android change exceeds two bridge calls')
        if candidate_loader_dir:
            require(after[CANDIDATE + '.smali'] == (candidate_loader_dir / (label + '-after') / (CANDIDATE + '.smali')).read_bytes(),
                    'Composed candidate Android helper differs')
        android[label] = {'before_classes': len(before), 'after_classes': len(after),
                          'unchanged_existing_classes': len(before) - len(changed),
                          'changed_existing_classes': sorted(changed), 'added_classes': [HELPER + '.smali'],
                          'only_two_main_bridge_calls_added': True, 'punctuation_patch_unchanged': True,
                          'sha256': sha(output.read_bytes())}
    require((work / 'single-after' / (HELPER + '.smali')).read_bytes() ==
            (work / 'bundle-after' / (HELPER + '.smali')).read_bytes(), 'Startup Android helper differs between loaders')
    staged = work / 'japanese-start-loader-check.mpp'
    replacements = {p.relative_to(work / 'jvm').as_posix(): p.read_bytes() for p in (work / 'jvm').rglob('*.class')}
    replacements['classes.dex'] = (work / 'single.dex').read_bytes()
    stage(standalone, staged, replacements)
    after_jvm = patch_classes(staged)
    require(set(after_jvm) == set(before_jvm) | {HELPER + '.class'}, 'Staged JVM inventory differs')
    changed_jvm = sorted(n for n in before_jvm if before_jvm[n] != after_jvm[n])
    require(set(changed_jvm) <= {MAIN + '.class', CANDIDATE + '.class'} and MAIN + '.class' in changed_jvm,
            'Unrelated staged JVM class changed')
    reports, archive_reports = {}, {}
    for label, sdk in (('archived_sdk', 'morphe.jar'), ('current_sdk', 'morphe-current.jar')):
        # Actual staged MPP precedes every SDK; loose helpers are absent.
        probe_cp = os.pathsep.join(map(str, (work / 'tool_classes', tools / sdk, staged, tools / 'r8.jar')))
        probe = [java, '-cp', probe_cp, 'app.hiro.swiftkey.patches.JapaneseStartPatchProbe']
        path = work / label / 'native-hook-report.json'
        run(probe + [root / 'tests/native_japanese_start_fixture', path], work / label / 'native-hooks.log')
        reports[label] = json.loads(path.read_text())
        require(reports[label]['status'] == 'PASSED' and reports[label]['native_methods'] == 4
                and reports[label]['native_hook_sites'] == 4 and reports[label]['shared_lifecycle_glide_hook_preserved'],
                'Native startup fixture or prior Glide coexistence failed')
        if apk:
            path = work / label / 'native-archive-hook-report.json'
            run(probe + [apk, path], work / label / 'native-archive-hooks.log')
            archive_reports[label] = json.loads(path.read_text())
            require(archive_reports[label]['status'] == 'PASSED' and not archive_reports[label]['native_fixture'],
                    'Archived APK native startup hooks failed')
    require(reports['archived_sdk'] == reports['current_sdk'], 'SDKs disagree on startup fixture')
    if apk:
        require(archive_reports['archived_sdk'] == archive_reports['current_sdk'], 'SDKs disagree on archived native hooks')
    report = {
        'status': 'PASSED', 'scope': 'Additive startup loader hooks with published prior Glide transform; no device/full APK apply',
        'baseline_mpp_sha256': BASELINES, 'baseline_jvm_patch_classes': 11, 'final_jvm_patch_classes': 12,
        'jvm_main_only_two_calls_added': True, 'jvm_original_normalized_class_content_preserved': True,
        'existing_named_patch_registration_unchanged': True, 'punctuation_patch_unchanged': True,
        'changed_jvm_classes': changed_jvm, 'added_jvm_classes': [HELPER + '.class'],
        'unchanged_jvm_classes': {n: sha(raw) for n, raw in before_jvm.items() if n not in changed_jvm},
        'candidate_loader_composed': bool(candidate_loader_dir),
        'candidate_loader_report_sha256': sha((candidate_loader_dir / 'report.json').read_bytes()) if candidate_loader_dir else None,
        'native_hook_tests': reports['archived_sdk'], 'native_hook_tests_by_sdk': reports,
        'native_archive_hook_tests': archive_reports, 'native_hook_sdks_agree': True,
        'native_hook_sdks_tested': ['morphe-desktop1.17.0-dev.6', 'morphe-desktop1.19.1/patcher2.0.1'],
        'published_prior_glide_transform_executed': True, 'shared_lifecycle_glide_hook_preserved': True,
        'native_probe_loader': 'Verified published 1.8.18 MPP with only intended loader entries replaced; no loose helper classpath',
        'android': android,
        'jvm': {n: {'bytes': len(raw), 'sha256': sha(raw)} for n, raw in replacements.items() if n.endswith('.class')},
        'source_sha256': {'patch_java/' + HELPER + '.java': sha(source.read_bytes()),
                          'tests/JapaneseStartPatchProbe.java': sha((root / 'tests/JapaneseStartPatchProbe.java').read_bytes())},
        'apk_sha256': sha(apk.read_bytes()) if apk else None,
        'device_tested': False, 'original_apk_apply_tested': False,
    }
    (work / 'report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print('PASS Japanese startup loader: 4 native methods / 4 sites; actual prior Glide transform; both SDKs')
    return report


def main():
    p = argparse.ArgumentParser()
    for name in ('source-dir', 'tools', 'work', 'standalone', 'bundle'):
        p.add_argument('--' + name, type=Path, required=True)
    p.add_argument('--apk', type=Path)
    p.add_argument('--candidate-loader-dir', type=Path)
    a = p.parse_args()
    build(a.source_dir.resolve(), a.tools.resolve(), a.work.resolve(), a.standalone.resolve(), a.bundle.resolve(),
          a.apk.resolve() if a.apk else None, a.candidate_loader_dir.resolve() if a.candidate_loader_dir else None)


if __name__ == '__main__':
    main()
