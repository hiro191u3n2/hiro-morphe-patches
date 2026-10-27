#!/usr/bin/env python3
"""Build additive JVM/Android loader hooks without recompiling existing patches."""
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
HELPER = PREFIX + 'CandidateToolbarPatch'

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

def build(root, tools, work, standalone, bundle, apk=None):
    if work.exists():
        shutil.rmtree(work)
    for name in ('jvm', 'helper_classes', 'tool_classes', 'helper_dex'):
        (work / name).mkdir(parents=True, exist_ok=True)
    runtime_java = os.environ.get('MORPHE_JAVA', 'java')
    cp = os.pathsep.join(map(str, (tools / 'morphe.jar', standalone)))
    compile_tool = ['java', 'com.sun.tools.javac.Main', '-g:none', '-encoding', 'UTF-8']
    source = root / 'patch_java' / (HELPER + '.java')
    run(compile_tool + ['--release', '8', '-cp', cp, '-d', work / 'helper_classes', source], work / 'javac-helper.log')
    compiled = sorted(p.relative_to(work / 'helper_classes').as_posix()
                      for p in (work / 'helper_classes').rglob('*.class'))
    require(compiled == [HELPER + '.class'], 'Unexpected patch helper class inventory')
    helper_jar = work / 'toolbar-helper.jar'
    with zipfile.ZipFile(helper_jar, 'w') as z:
        z.write(work / 'helper_classes' / (HELPER + '.class'), HELPER + '.class')
    baseline_classpath = work / 'baseline-patch-classpath.jar'
    with zipfile.ZipFile(standalone) as old, zipfile.ZipFile(baseline_classpath, 'w') as z:
        for name in old.namelist():
            if name.startswith(PREFIX) and name.endswith('.class'):
                z.writestr(name, old.read(name))
    run(['java', '-cp', tools / 'r8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26',
         '--lib', tools / 'android.jar', '--classpath', tools / 'morphe.jar', '--classpath', baseline_classpath,
         '--output', work / 'helper_dex', helper_jar], work / 'd8-helper.log')
    tool_cp = os.pathsep.join((str(work / 'helper_classes'), cp, str(tools / 'r8.jar')))
    run(compile_tool + ['-cp', tool_cp, '-d', work / 'tool_classes',
                       root / 'tools_java/UpdateToolbarLoader.java',
                       root / 'tests/CandidateToolbarPatchProbe.java'], work / 'javac-tools.log')
    run(compile_tool + ['-cp', tools / 'apktool.jar', '-d', work / 'tool_classes',
                       root / 'tools_java/Dump1814.java'], work / 'javac-dump.log')
    executable_cp = os.pathsep.join((str(work / 'tool_classes'), tool_cp))
    update = [runtime_java, '-cp', executable_cp, 'UpdateToolbarLoader']
    with zipfile.ZipFile(standalone) as first, zipfile.ZipFile(bundle) as second:
        require(first.read(MAIN + '.class') == second.read(MAIN + '.class'), 'Baseline JVM main differs between MPPs')
        require(HELPER + '.class' not in first.namelist() and HELPER + '.class' not in second.namelist(), 'Toolbar loader already installed')
    run(update + ['jvm', standalone, work / 'helper_classes', work / 'jvm'], work / 'jvm-bridge.log')
    dump = ['java', '-cp', os.pathsep.join(map(str, (work / 'tool_classes', tools / 'apktool.jar'))), 'Dump1814']
    android = {}
    for label, baseline in (('single', standalone), ('bundle', bundle)):
        with zipfile.ZipFile(baseline) as z:
            (work / (label + '-before.dex')).write_bytes(z.read('classes.dex'))
        output = work / (label + '.dex')
        run(update + ['dex', baseline, work / 'helper_dex/classes.dex', output], work / (label + '-bridge.log'))
        run(dump + [work / (label + '-before.dex'), work / (label + '-before')], work / (label + '-dump-before.log'))
        run(dump + [output, work / (label + '-after')], work / (label + '-dump-after.log'))
        before, after = smali(work / (label + '-before')), smali(work / (label + '-after'))
        require(set(after) == set(before) | {HELPER + '.smali'}, 'Android loader class inventory differs')
        require({n: b for n, b in before.items() if n != MAIN + '.smali'} ==
                {n: b for n, b in after.items() if n not in (MAIN + '.smali', HELPER + '.smali')},
                'Unrelated Android patch class changed')
        pattern = (rb'(?m)^    invoke-static/range \{p0 \.\. p0\}, Lapp/hiro/swiftkey/patches/CandidateToolbarPatch;->'
                   rb'(?:apply|checkOriginal)\(Lapp/morphe/patcher/patch/BytecodePatchContext;\)V\n\n')
        restored, count = re.subn(pattern, b'', after[MAIN + '.smali'])
        require(count == 2 and restored == before[MAIN + '.smali'], 'Android main change exceeds two bridge calls')
        android[label] = {'before_classes': len(before), 'after_classes': len(after),
                          'unchanged_existing_classes': len(before) - 1,
                          'changed_existing_classes': [MAIN + '.smali'], 'added_classes': [HELPER + '.smali'],
                          'only_two_bridge_calls_added': True,
                          'sha256': sha(output.read_bytes())}
    require((work / 'single-after' / (HELPER + '.smali')).read_bytes() ==
            (work / 'bundle-after' / (HELPER + '.smali')).read_bytes(), 'Android helper differs between loaders')
    probe = [runtime_java, '-cp', executable_cp, 'app.hiro.swiftkey.patches.CandidateToolbarPatchProbe']
    run(probe + [root / 'tests/native_toolbar_fixture', work / 'native-hook-report.json'], work / 'native-hooks.log')
    native = json.loads((work / 'native-hook-report.json').read_text())
    require(native['status'] == 'PASSED' and native['native_methods'] == 7
            and native['inserted_runtime_calls'] == 8 and native['native_return_labels_enter_hooks']
            and native['all_target_drift_rejected_before_mutation'], 'Native hook fixture verification failed')
    current_cp = os.pathsep.join(map(str, (work / 'tool_classes', work / 'helper_classes',
                                           tools / 'morphe-current.jar', standalone, tools / 'r8.jar')))
    run([runtime_java, '-cp', current_cp, 'app.hiro.swiftkey.patches.CandidateToolbarPatchProbe',
         root / 'tests/native_toolbar_fixture', work / 'native-hook-current-report.json'],
        work / 'native-hooks-current.log')
    require(json.loads((work / 'native-hook-current-report.json').read_text()) == native,
            'Current Morphe SDK disagrees on actual native hook transformations')
    if apk:
        run(probe + [apk, work / 'native-archive-hook-report.json'], work / 'native-archive-hooks.log')
    report = {'status': 'PASSED', 'scope': 'Additive patch loader hooks and exact native method transformations',
              'jvm_main_only_two_calls_added': True, 'jvm_original_normalized_class_content_preserved': True,
              'existing_named_patch_registration_unchanged': True,
              'native_hook_tests': native,
              'native_hook_sdks_tested': ['morphe-desktop1.17.0-dev.6', 'morphe-desktop1.19.1/patcher2.0.1'],
              'native_hook_sdks_agree': True, 'android': android,
              'jvm': {n: {'bytes': (work / 'jvm' / n).stat().st_size,
                          'sha256': sha((work / 'jvm' / n).read_bytes())}
                      for n in (MAIN + '.class', HELPER + '.class')},
              'device_tested': False, 'original_apk_apply_tested': False}
    (work / 'report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print('PASS toolbar loader addition: JVM existing main + helper; Android existing main + helper; exact native hooks')
    return report

def main():
    p = argparse.ArgumentParser()
    for name in ('source-dir', 'tools', 'work', 'standalone', 'bundle'):
        p.add_argument('--' + name, type=Path, required=True)
    p.add_argument('--apk', type=Path)
    a = p.parse_args()
    build(a.source_dir.resolve(), a.tools.resolve(), a.work.resolve(), a.standalone.resolve(),
          a.bundle.resolve(), a.apk.resolve() if a.apk else None)

if __name__ == '__main__':
    main()
