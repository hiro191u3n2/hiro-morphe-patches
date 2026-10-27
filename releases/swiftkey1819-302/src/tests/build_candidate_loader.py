#!/usr/bin/env python3
"""Replace the installed candidate helper, retaining the complete 1.8.18 loader."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

PREFIX = 'app/hiro/swiftkey/patches/'
MAIN = PREFIX + 'JapaneseSamsungEmojiPatch'
HELPER = PREFIX + 'CandidateScrollPatch'
PUNCTUATION = PREFIX + 'PunctuationPatch'
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
    return {p.relative_to(directory).as_posix(): p.read_bytes()
            for p in sorted(directory.rglob('*.smali'))}


def patch_classes(path):
    with zipfile.ZipFile(path) as z:
        return {name: z.read(name) for name in z.namelist()
                if name.startswith(PREFIX) and name.endswith('.class')}


def build(root, tools, work, standalone, bundle, apk=None):
    for label, baseline in (('single', standalone), ('bundle', bundle)):
        require(sha(baseline.read_bytes()) == BASELINES[label], 'Unverified baseline ' + label)
    before_jvm = patch_classes(standalone)
    require(len(before_jvm) == 11 and before_jvm == patch_classes(bundle),
            'Baseline SwiftKey JVM patch inventory differs')
    require(all(n + '.class' in before_jvm for n in (MAIN, HELPER, PUNCTUATION)),
            'Baseline candidate/main/punctuation patch missing')
    if work.exists():
        shutil.rmtree(work)
    for name in ('jvm', 'helper_classes', 'tool_classes', 'helper_dex'):
        (work / name).mkdir(parents=True, exist_ok=True)
    java21 = tools / 'jdk-21.0.8+9-jre/bin/java'
    runtime_java = os.environ.get('MORPHE_JAVA', str(java21) if java21.is_file() else 'java')
    cp = os.pathsep.join(map(str, (tools / 'morphe.jar', standalone)))
    compile_tool = ['java', 'com.sun.tools.javac.Main', '-g:none', '-encoding', 'UTF-8']
    source = root / 'patch_java' / (HELPER + '.java')
    run(compile_tool + ['--release', '8', '-cp', cp, '-d', work / 'helper_classes', source],
        work / 'javac-helper.log')
    compiled = sorted(p.relative_to(work / 'helper_classes').as_posix()
                      for p in (work / 'helper_classes').rglob('*.class'))
    require(compiled == [HELPER + '.class'], 'Unexpected patch helper class inventory')
    helper_jar = work / 'candidate-helper.jar'
    with zipfile.ZipFile(helper_jar, 'w') as z:
        z.write(work / 'helper_classes' / (HELPER + '.class'), HELPER + '.class')
    baseline_classpath = work / 'baseline-patch-classpath.jar'
    with zipfile.ZipFile(baseline_classpath, 'w') as z:
        for name, raw in before_jvm.items():
            if name != HELPER + '.class':
                z.writestr(name, raw)
    run(['java', '-cp', tools / 'r8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26',
         '--lib', tools / 'android.jar', '--classpath', tools / 'morphe.jar',
         '--classpath', baseline_classpath, '--output', work / 'helper_dex', helper_jar],
        work / 'd8-helper.log')
    tool_cp = os.pathsep.join((str(work / 'helper_classes'), cp, str(tools / 'r8.jar')))
    run(compile_tool + ['-cp', tool_cp, '-d', work / 'tool_classes',
                       root / 'tools_java/UpdateCandidateLoader.java',
                       root / 'tests/CandidateScrollPatchProbe.java'], work / 'javac-tools.log')
    run(compile_tool + ['-cp', tools / 'apktool.jar', '-d', work / 'tool_classes',
                       root / 'tools_java/Dump1814.java'], work / 'javac-dump.log')
    executable_cp = os.pathsep.join((str(work / 'tool_classes'), tool_cp))
    update = [runtime_java, '-cp', executable_cp, 'UpdateCandidateLoader']
    run(update + ['jvm', standalone, work / 'helper_classes', work / 'jvm'], work / 'jvm-replace.log')
    replacement = (work / 'jvm' / (HELPER + '.class')).read_bytes()
    require(replacement != before_jvm[HELPER + '.class'], 'No candidate patch change')
    require(sorted(p.relative_to(work / 'jvm').as_posix() for p in (work / 'jvm').rglob('*.class'))
            == [HELPER + '.class'], 'Unintended JVM replacement output')
    dump = ['java', '-cp', os.pathsep.join(map(str, (work / 'tool_classes', tools / 'apktool.jar'))), 'Dump1814']
    android = {}
    for label, baseline in (('single', standalone), ('bundle', bundle)):
        with zipfile.ZipFile(baseline) as z:
            (work / (label + '-before.dex')).write_bytes(z.read('classes.dex'))
        output = work / (label + '.dex')
        run(update + ['dex', baseline, work / 'helper_dex/classes.dex', output], work / (label + '-replace.log'))
        run(dump + [work / (label + '-before.dex'), work / (label + '-before')], work / (label + '-dump-before.log'))
        run(dump + [output, work / (label + '-after')], work / (label + '-dump-after.log'))
        before, after = smali(work / (label + '-before')), smali(work / (label + '-after'))
        require(set(after) == set(before), 'Android loader class inventory changed')
        changed = sorted(name for name in before if before[name] != after[name])
        require(changed == [HELPER + '.smali'], 'Unrelated Android loader classes changed: ' + str(changed))
        require(before[MAIN + '.smali'] == after[MAIN + '.smali'], 'Main Android patch changed')
        require(before[PUNCTUATION + '.smali'] == after[PUNCTUATION + '.smali'], 'Punctuation Android patch changed')
        android[label] = {
            'before_classes': len(before), 'after_classes': len(after), 'inventory_identical': True,
            'unchanged_existing_classes': len(before) - 1, 'changed_existing_classes': changed,
            'added_classes': [], 'main_smali_unchanged': True, 'punctuation_smali_unchanged': True,
            'sha256': sha(output.read_bytes()),
        }
    require((work / 'single-after' / (HELPER + '.smali')).read_bytes() ==
            (work / 'bundle-after' / (HELPER + '.smali')).read_bytes(), 'Android helpers differ between packages')

    # Load the real baseline MPP with only the two intended loader entries
    # replaced. Keep loose helper_classes out of the runtime probe classpath.
    check_mpp = work / 'candidate-loader-check.mpp'
    with zipfile.ZipFile(standalone) as old, zipfile.ZipFile(check_mpp, 'w') as new:
        for entry in old.infolist():
            data = replacement if entry.filename == HELPER + '.class' else (
                (work / 'single.dex').read_bytes() if entry.filename == 'classes.dex' else old.read(entry.filename))
            new.writestr(entry, data)
    staged_jvm = patch_classes(check_mpp)
    changed_jvm = sorted(name for name in before_jvm if before_jvm[name] != staged_jvm[name])
    require(set(staged_jvm) == set(before_jvm) and changed_jvm == [HELPER + '.class'],
            'Staged MPP JVM replacement exceeded candidate patch')
    reports, archive_reports = {}, {}
    for label, sdk in (('archived_sdk', 'morphe.jar'), ('current_sdk', 'morphe-current.jar')):
        probe_cp = os.pathsep.join(map(str, (work / 'tool_classes', tools / sdk, check_mpp, tools / 'r8.jar')))
        probe = [runtime_java, '-cp', probe_cp, 'app.hiro.swiftkey.patches.CandidateScrollPatchProbe']
        report_path = work / label / 'native-hook-report.json'
        run(probe + [root / 'tests/native_candidate_fixture', report_path], work / label / 'native-hooks.log')
        result = json.loads(report_path.read_text())
        require(result['status'] == 'PASSED' and result['native_methods'] == 6
                and result['native_hook_sites'] == 9, 'Native fixture verification failed')
        reports[label] = result
        if apk:
            report_path = work / label / 'native-archive-hook-report.json'
            run(probe + [apk, report_path], work / label / 'native-archive-hooks.log')
            archive_reports[label] = json.loads(report_path.read_text())
            require(archive_reports[label]['status'] == 'PASSED'
                    and not archive_reports[label]['native_fixture'], 'Archived APK native verification failed')
    require(reports['archived_sdk'] == reports['current_sdk'], 'Morphe SDKs disagree on native fixture')
    if apk:
        require(archive_reports['archived_sdk'] == archive_reports['current_sdk'], 'Morphe SDKs disagree on archived APK')
    report = {
        'status': 'PASSED', 'scope': 'Installed candidate helper replacement and exact native method transformations; no device/full APK apply',
        'baseline_mpp_sha256': BASELINES, 'baseline_jvm_patch_classes': 11,
        'changed_jvm_classes': changed_jvm, 'jvm_main_unchanged': True, 'punctuation_patch_unchanged': True,
        'unchanged_jvm_classes': {name: sha(raw) for name, raw in before_jvm.items() if name not in changed_jvm},
        'existing_named_patch_registration_unchanged': True,
        'native_hook_tests': reports['archived_sdk'], 'native_hook_tests_by_sdk': reports,
        'native_archive_hook_tests': archive_reports, 'native_hook_sdks_agree': True,
        'native_hook_sdks_tested': ['morphe-desktop1.17.0-dev.6', 'morphe-desktop1.19.1/patcher2.0.1'],
        'native_probe_loader': 'Verified published 1.8.18 MPP with only CandidateScrollPatch.class and classes.dex replaced; no loose helper classpath',
        'android': android,
        'jvm': {HELPER + '.class': {'bytes': len(replacement), 'sha256': sha(replacement)}},
        'source_sha256': {'patch_java/' + HELPER + '.java': sha(source.read_bytes()),
                          'tests/CandidateScrollPatchProbe.java': sha((root / 'tests/CandidateScrollPatchProbe.java').read_bytes())},
        'apk_sha256': sha(apk.read_bytes()) if apk else None,
        'device_tested': False, 'original_apk_apply_tested': False,
    }
    (work / 'report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print('PASS candidate loader replacement: JVM/Android CandidateScrollPatch only; 6 native methods / 9 hook sites; both SDKs')
    return report


def main():
    parser = argparse.ArgumentParser()
    for name in ('source-dir', 'tools', 'work', 'standalone', 'bundle'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--apk', type=Path)
    args = parser.parse_args()
    build(args.source_dir.resolve(), args.tools.resolve(), args.work.resolve(), args.standalone.resolve(),
          args.bundle.resolve(), args.apk.resolve() if args.apk else None)


if __name__ == '__main__':
    main()
