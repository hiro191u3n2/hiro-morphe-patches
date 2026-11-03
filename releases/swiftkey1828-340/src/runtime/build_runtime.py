#!/usr/bin/env python3
"""Build a narrowly scoped ranking/space overlay, test it, and verify native ABI."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import zipfile

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parent
BASE_SHA = '0a5822f3cd2b78ef5550b48ba7fffa488e11cdd925d007ecf93d3216e9f06477'
APK_SHA = 'e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7'
PINS = {
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'apktool.jar': 'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423',
    'r8.jar': 'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
    'morphe.jar': 'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
}
WORD = 'Lhiro/swiftkey/WordCandidates;'
ENHANCE = WORD + '->enhance(Ljava/util/List;Ll40/c;)Ljava/util/List;'
ENGLISH = 'Lhiro/swiftkey/KatakanaEnglishCandidates;'
CHANGED_METHODS = sorted([ENHANCE, ENGLISH + '->enhance(Ljava/util/List;Ll40/c;)Ljava/util/List;'])

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def require(ok, message):
    if not ok:
        raise RuntimeError(message)

def write_json(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2, sort_keys=True) + '\n')

def run(command, log):
    result = subprocess.run([str(x) for x in command], stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, text=True)
    log.parent.mkdir(parents=True, exist_ok=True)
    log.write_text(result.stdout)
    require(result.returncode == 0, 'Command failed: ' + str(log) + '\n' + result.stdout[-5000:])
    return result.stdout

def smali(folder):
    return {p.relative_to(folder).as_posix(): p.read_bytes() for p in sorted(folder.rglob('*.smali'))}

def helper(path):
    return any(path == family + '.class' or path.startswith(family + '$')
               for family in ('hiro/swiftkey/WordCandidates', 'hiro/swiftkey/SpaceConversion'))

def zip_add(archive, name, raw):
    info = zipfile.ZipInfo(name, (2026, 11, 3, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = 0o100644 << 16
    archive.writestr(info, raw, compresslevel=9)

def compile_source(source, tools, folder, dump):
    classes = folder / 'classes'
    classes.mkdir(parents=True)
    stubs = sorted((ROOT / 'stubs').rglob('*.java'))
    require(stubs, 'Missing production native ABI stubs')
    require(not any(p.relative_to(ROOT / 'stubs').parts[0] == 'android' for p in stubs),
            'Production build must use actual Android API jar')
    run(['java', 'com.sun.tools.javac.Main', '--release', '8', '-encoding', 'UTF-8',
         '-cp', tools / 'android.jar', '-d', classes, *stubs, *source], folder / 'javac.log')
    with zipfile.ZipFile(folder / 'runtime.jar', 'w') as runtime, \
            zipfile.ZipFile(folder / 'stubs.jar', 'w') as native:
        for path in sorted(classes.rglob('*.class')):
            name = path.relative_to(classes).as_posix()
            zip_add(runtime if helper(name) else native, name, path.read_bytes())
    dex = folder / 'dex'
    dex.mkdir()
    run(['java', '-cp', tools / 'r8.jar', 'com.android.tools.r8.D8', '--debug',
         '--min-api', '26', '--lib', tools / 'android.jar', '--classpath', folder / 'stubs.jar',
         '--output', dex, folder / 'runtime.jar'], folder / 'd8.log')
    run(dump + [dex / 'classes.dex', folder / 'smali'], folder / 'dump.log')
    return dex / 'classes.dex'

def clean_report(value):
    if isinstance(value, dict):
        return {k: clean_report(v) for k, v in value.items()
                if k not in ('elapsed_ms', 'elapsed_seconds')}
    if isinstance(value, list):
        return [clean_report(v) for v in value]
    return value

def build(runtime, tools, work, output, apk):
    require(sha(runtime) == BASE_SHA, 'Pinned published 1.8.27 runtime differs')
    for name, pin in PINS.items():
        require(sha(tools / name) == pin, 'Tool pin differs: ' + name)
    require(not work.exists(), 'Fresh runtime work directory required')
    require(not output.exists() or not any(output.iterdir()), 'Fresh runtime output directory required')
    work.mkdir(parents=True)
    output.mkdir(parents=True, exist_ok=True)
    tool_classes = work / 'tool-classes'
    tool_classes.mkdir()
    run(['java', 'com.sun.tools.javac.Main', '-cp', tools / 'apktool.jar', '-d', tool_classes,
         ROOT / 'tools_java/Dump1814.java'], work / 'compile-dump.log')
    run(['java', 'com.sun.tools.javac.Main', '-cp', tools / 'morphe.jar', '-d', tool_classes,
         ROOT / 'tools_java/Merge1828.java'], work / 'compile-merge.log')
    dump = ['java', '-cp', str(tool_classes) + os.pathsep + str(tools / 'apktool.jar'), 'Dump1814']
    run(dump + [runtime, work / 'before'], work / 'dump-before.log')
    before = smali(work / 'before')
    require(len(before) == 91, 'Published runtime class inventory differs')
    baseline_source = ROOT / 'baseline/hiro/swiftkey/WordCandidates.java'
    require(baseline_source.is_file(), 'Restored WordCandidates baseline source missing')
    compile_source([baseline_source], tools, work / 'baseline', dump)
    baseline = smali(work / 'baseline/smali')
    require(set(baseline) == {'hiro/swiftkey/WordCandidates.smali',
                            'hiro/swiftkey/WordCandidates$Index.smali',
                            'hiro/swiftkey/WordCandidates$1.smali'},
            'Restored WordCandidates class inventory differs')
    require(all(before[name] == raw for name, raw in baseline.items()),
            'Restored baseline source does not reconstruct published executable WordCandidates')
    sources = sorted((ROOT / 'src').rglob('*.java'))
    require((ROOT / 'src/hiro/swiftkey/WordCandidates.java') in sources and
            (ROOT / 'src/hiro/swiftkey/SpaceConversion.java') in sources,
            'Production ranking/space sources missing')
    compiled = compile_source(sources, tools, work / 'fixed', dump)
    java = os.environ.get('MORPHE_JAVA', 'java')
    run([java, '-cp', str(tool_classes) + os.pathsep + str(tools / 'morphe.jar'),
         'Merge1828', runtime, compiled, output / 'runtime.mpe', output / 'merge-report.json'],
        work / 'merge.log')
    merge = json.loads((output / 'merge-report.json').read_text())
    require(merge['status'] == 'PASSED' and merge['changed_methods'] == CHANGED_METHODS
            and merge['all_other_classes_preserved'], 'Runtime preservation contract failed')
    run(dump + [output / 'runtime.mpe', work / 'after'], work / 'dump-after.log')
    after = smali(work / 'after')
    require(set(before).issubset(after), 'Existing runtime class removed')
    changed = sorted('L' + name[:-6] + ';' for name in before if before[name] != after[name])
    require(changed == sorted([WORD, ENGLISH]), 'Unrelated runtime executable changed')
    added = sorted('L' + name[:-6] + ';' for name in after if name not in before)
    require(added == merge['added_classes'], 'Added runtime class evidence differs')
    host = {}
    for name in ('ranking', 'space'):
        runner = ROOT / ('tests/run_' + name + '_tests.py')
        require(runner.is_file(), 'Required meaningful host test runner missing: ' + name)
        folder = work / ('host-' + name)
        run([sys.executable, runner, '--work', folder], work / (name + '-tests.log'))
        host[name] = clean_report(json.loads((folder / 'report.json').read_text()))
        require(host[name].get('status') == 'PASSED', 'Host tests failed: ' + name)
    english_runner = ROOT / 'tests/run_english_priority_tests.py'
    require(english_runner.is_file(), 'Required final-DEX English priority test runner missing')
    english_work = work / 'host-english-priority'
    run([sys.executable, english_runner, '--baseline', runtime,
         '--runtime', output / 'runtime.mpe', '--tools', tools, '--work', english_work],
        work / 'english-priority-tests.log')
    host['english_priority'] = clean_report(json.loads((english_work / 'report.json').read_text()))
    require(host['english_priority'].get('status') == 'PASSED', 'Final-DEX English priority tests failed')
    runtime_sha = sha(output / 'runtime.mpe')
    abi_source = ROOT / 'tools_java/AuditRuntime1828.java'
    if apk:
        require(sha(apk) == APK_SHA, 'Native reference APK pin differs')
        cp = str(tools / 'morphe.jar') + os.pathsep + str(tools / 'r8.jar')
        run(['java', 'com.sun.tools.javac.Main', '-cp', cp, '-d', tool_classes, abi_source],
            work / 'compile-abi.log')
        run([java, '-Xmx1024m', '-cp', str(tool_classes) + os.pathsep + cp, 'AuditRuntime1828',
             apk, output / 'runtime.mpe', tools / 'android.jar', output / 'native-abi.json'], work / 'abi.log')
        abi = json.loads((output / 'native-abi.json').read_text())
        abi['verifier_source_sha256'] = sha(abi_source)
    else:
        evidence = ROOT / 'tests/native-abi.json'
        require(evidence.is_file(), 'Native ABI evidence missing; first build requires --apk')
        abi = json.loads(evidence.read_text())
    require(abi.get('status') == 'PASSED' and abi.get('extension_sha256') == runtime_sha
            and abi.get('apk_sha256') == APK_SHA and abi.get('android_jar_sha256') == PINS['android.jar']
            and abi.get('verifier_source_sha256') == sha(abi_source),
            'Native ABI evidence is not bound to built implementation/verifier')
    require(not abi['unresolved'] and not abi['inaccessible'] and not abi['incompatible_opcodes'],
            'Native ABI has unresolved or incompatible references')
    audited = set(abi['audited_classes'])
    require(audited == {name for name in after_as_types(after)
                        if name.startswith('Lhiro/swiftkey/WordCandidates')
                        or name.startswith('Lhiro/swiftkey/KatakanaEnglishCandidates')
                        or name.startswith('Lhiro/swiftkey/SpaceConversion')},
            'Native ABI audit family incomplete')
    write_json(output / 'native-abi.json', abi)
    report = {'status': 'PASSED', 'baseline_runtime_sha256': BASE_SHA,
              'runtime_sha256': runtime_sha, 'baseline_java_matches_published_dex': True,
              'changed_classes': changed, 'added_classes': added, 'changed_methods': CHANGED_METHODS,
              'all_other_classes_preserved': True, 'all_other_method_bodies_preserved': True,
              'merge': merge, 'host_tests_passed': True, 'host_report': host,
              'native_abi_verified': True, 'native_abi_report_sha256': sha(output / 'native-abi.json'),
              'native_abi_report': abi, 'device_tested': False, 'original_apk_apply_tested': False}
    write_json(output / 'runtime-report.json', report)
    print('PASS ranking/space runtime ' + runtime_sha)

def after_as_types(files):
    return ['L' + name[:-6] + ';' for name in files]

def main():
    parser = argparse.ArgumentParser()
    for name in ('runtime', 'tools', 'work', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--apk', type=Path)
    args = parser.parse_args()
    build(args.runtime.resolve(), args.tools.resolve(), args.work.resolve(), args.output.resolve(),
          args.apk.resolve() if args.apk else None)

if __name__ == '__main__':
    main()
