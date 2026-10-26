#!/usr/bin/env python3
"""Add optional emoji-panel controls while preserving all existing SwiftKey behavior."""
from __future__ import annotations
import argparse, copy, hashlib, json, os, shutil, struct, subprocess, time, urllib.request, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OLD_SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.5.mpp'
OLD_BUNDLE = 'Hiro_Morphe_Patches_v1.0.288.mpp'
SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.6.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.289.mpp'
QA_NAME = 'QA_SwiftKey_v1.8.6.json'
SOURCE_ZIP = 'SwiftKeyBeta_v1.8.6_sources_and_QA.zip'
MF, EXT = 'META-INF/MANIFEST.MF', 'extensions/swiftkey_japanese.mpe'
STAMP = '2026-10-26T08:00:00'
JVM_PREFIX = 'app/hiro/swiftkey/patches/'
JVM_CLASSES = tuple(JVM_PREFIX + n + '.class' for n in (
    'JapaneseSamsungEmojiPatch', 'MethodContract', 'InputLearningPatch', 'DictionarySupplementPatch',
    'KeySwapPatch', 'PasteProtectionPatch', 'JapaneseGlidePatch', 'DeleteProtectionPatch'))
KEY_SWAP = 'hiro/swiftkey/KeySwap.smali'
GLIDE = 'hiro/swiftkey/JapaneseGlide.smali'
HOOKS = frozenset((KEY_SWAP, GLIDE))
DESCRIPTION = '絵文字の検索欄と「AI の作成」ボタンを個別に非表示にする設定を追加。初期設定は両方オフ。辞書・Samsung絵文字・キー配置・LINE改行・グライド修正とJVMローダー修復を保持。実機未確認。'
BASE = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/ff04f16afe93e8c24487f4af7c0c8e79a348025d/downloads/'
PINS = {
    OLD_SINGLE: 'fd5c3eed07ff86a5b169bbffc237bb2dd835ab77afe357a8e75c6a72589010b4',
    OLD_BUNDLE: '9421cd1efa035f1186331e8a1adadc595d2f69a344b2c1d0ee53013e118b4ec8',
    'apktool.jar': 'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423',
    'morphe.jar': 'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
    'morphe-current.jar': 'a51472252cd63b2eac635dbb5a154ff918bfc15268da89679feb3d760230c573',
    'r8.jar': 'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
}
URLS = {
    OLD_SINGLE: BASE + OLD_SINGLE, OLD_BUNDLE: BASE + OLD_BUNDLE,
    'apktool.jar': 'https://github.com/iBotPeaches/Apktool/releases/download/v3.0.3/apktool_3.0.3.jar',
    'morphe.jar': 'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.17.0-dev.6/morphe-desktop-1.17.0-dev.6-all.jar',
    'morphe-current.jar': 'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.19.1/morphe-desktop-1.19.1-all.jar',
    'r8.jar': 'https://storage.googleapis.com/r8-releases/raw/8.7.18/r8.jar',
    'android.jar': 'https://raw.githubusercontent.com/Sable/android-platforms/master/android-35/android.jar',
}

def sha(raw): return hashlib.sha256(raw).hexdigest()
def require(ok, message):
    if not ok: raise RuntimeError(message)
def run(command, log):
    result = subprocess.run([str(a) for a in command], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    log.parent.mkdir(parents=True, exist_ok=True); log.write_text(result.stdout)
    print(result.stdout, end='', flush=True)
    require(result.returncode == 0, 'Command failed; see ' + str(log)); return result.stdout
def javac(args, log): return run(['java', 'com.sun.tools.javac.Main', *args], log)
def smali_files(path): return {p.relative_to(path).as_posix(): p.read_bytes() for p in sorted(path.rglob('*.smali'))}
def helper(path): return path == 'hiro/swiftkey/EmojiPanelUi.smali' or path.startswith('hiro/swiftkey/EmojiPanelUi$')
def manifest(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '): require(bool(rows), 'Invalid manifest continuation'); rows[-1] += line[1:]
        elif line: rows.append(line)
    pairs = [line.decode().split(': ', 1) for line in rows]
    require(all(len(p) == 2 for p in pairs) and len(dict(pairs)) == len(pairs), 'Invalid manifest')
    return dict(pairs)
def manifest_bytes(values):
    lines = []
    for key, value in values.items():
        line = b''
        for char in key + ': ' + value:
            raw = char.encode()
            if len(line) + len(raw) > 70: lines.append(line); line = b' '
            line += raw
        lines.append(line)
    raw = b'\r\n'.join(lines) + b'\r\n\r\n'; require(manifest(raw) == values, 'Manifest roundtrip failed'); return raw
def download(inputs, tools):
    for name, pin in PINS.items():
        path = (inputs if name.endswith('.mpp') else tools) / name
        if path.exists(): require(sha(path.read_bytes()) == pin, 'Existing pinned input differs: ' + name); continue
        path.parent.mkdir(parents=True, exist_ok=True)
        for attempt in range(3):
            try:
                raw = urllib.request.urlopen(URLS[name], timeout=120).read()
                require(sha(raw) == pin, 'Downloaded input hash differs: ' + name)
                path.write_bytes(raw); break
            except Exception:
                if attempt == 2: raise
                time.sleep(2 * (attempt + 1))
def archive(path):
    with zipfile.ZipFile(path) as z:
        require(len(z.namelist()) == len(set(z.namelist())) and z.testzip() is None, 'Invalid ZIP ' + str(path))
        return {n: z.read(n) for n in z.namelist()}
def make_mpp(source, target, extension, old_version, version):
    with zipfile.ZipFile(source) as old:
        values = manifest(old.read(MF)); require(values['Version'] == old_version, 'Baseline version differs')
        values.update(Version=version, Timestamp=STAMP, Description=DESCRIPTION)
        updates = {MF: manifest_bytes(values), EXT: extension}
        with zipfile.ZipFile(target, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as new:
            for info in old.infolist(): new.writestr(copy.copy(info), updates.get(info.filename, old.read(info)))
    before, after = archive(source), archive(target)
    require(list(before) == list(after), 'Output archive entries changed')
    changed = sorted(n for n in before if before[n] != after[n])
    require(set(changed) == set(updates), 'Unexpected MPP changes ' + repr(changed))
    return {'changed_entries': changed, 'added_entries': [], 'unchanged_entries': len(before) - 2,
            'bytes': target.stat().st_size, 'sha256': sha(target.read_bytes()),
            'extension_sha256': sha(extension), 'all_other_entries_byte_identical': True}
def source_paths():
    names = ('build186.py', 'publish186.py', 'publisher_tests.py', 'workflow.yml', 'RELEASE_NOTES.txt', 'README.txt', 'audit_runtime_abi.py')
    paths = [ROOT / n for n in names if (ROOT / n).is_file()]
    for name in ('src', 'stubs', 'tests', 'tools_java'):
        paths.extend(p for p in (ROOT / name).rglob('*') if p.is_file() and p.suffix in ('.java', '.py', '.json', '.txt', '.md', '.yml'))
    return sorted(set(paths))
def zip_add(z, name, raw):
    info = zipfile.ZipInfo(name, (2026, 10, 26, 8, 0, 0)); info.compress_type = zipfile.ZIP_DEFLATED; info.external_attr = 0o100644 << 16
    z.writestr(info, raw, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)

def add_runtime_hooks(before):
    """Only insert calls; exact inverse restores every original instruction and label."""
    changes = {
        KEY_SWAP: (
            ('    :try_start_0\n    invoke-static {p0}, Lhiro/swiftkey/JapaneseGlide;->init(Landroid/content/SharedPreferences;)V',
             '    :try_start_0\n    invoke-static {p0}, Lhiro/swiftkey/EmojiPanelUi;->init(Landroid/content/SharedPreferences;)V\n\n    invoke-static {p0}, Lhiro/swiftkey/JapaneseGlide;->init(Landroid/content/SharedPreferences;)V'),
            ('    invoke-static {p0}, Lhiro/swiftkey/KeySwap;->addDictionarySources(Landroidx/preference/PreferenceScreen;)V',
             '    invoke-static {p0}, Lhiro/swiftkey/EmojiPanelUi;->install(Landroidx/preference/PreferenceScreen;)V\n\n    invoke-static {p0}, Lhiro/swiftkey/KeySwap;->addDictionarySources(Landroidx/preference/PreferenceScreen;)V'),
        ),
        GLIDE: (
            ('    invoke-static {p0}, Lhiro/swiftkey/JapaneseGlide;->prepare(Landroid/content/Context;)V\n\n    return-void\n.end method\n\n.method private static supportedTool',
             '    invoke-static {p0}, Lhiro/swiftkey/JapaneseGlide;->prepare(Landroid/content/Context;)V\n\n    invoke-static {p0}, Lhiro/swiftkey/EmojiPanelUi;->start(Landroid/inputmethodservice/InputMethodService;)V\n\n    return-void\n.end method\n\n.method private static supportedTool'),
        ),
    }
    hooks = {}
    for name, substitutions in changes.items():
        original = before[name].decode(); updated = original
        require('EmojiPanelUi' not in original, 'UI hooks already present')
        for old, new in substitutions:
            require(updated.count(old) == 1, 'Hook anchor missing or ambiguous: ' + name)
            updated = updated.replace(old, new, 1)
        inverse = updated
        for old, new in reversed(substitutions):
            require(inverse.count(new) == 1, 'Hook inverse ambiguous: ' + name)
            inverse = inverse.replace(new, old, 1)
        require(inverse == original, 'Hook insertion changed existing code: ' + name)
        require(updated.count('Lhiro/swiftkey/EmojiPanelUi;->') == len(substitutions), 'Unexpected hook count')
        hooks[name] = updated.encode()
    return hooks

def build(inputs, tools, work, output):
    for name, pin in PINS.items(): require(sha(((inputs if name.endswith('.mpp') else tools) / name).read_bytes()) == pin, 'Pinned input differs: ' + name)
    if work.exists(): shutil.rmtree(work)
    for d in (work, work/'tool_classes', work/'classes', work/'new_dex', output, output/'evidence'): d.mkdir(parents=True, exist_ok=True)
    evidence = output/'evidence'
    if (evidence/'runtime').exists(): shutil.rmtree(evidence/'runtime')
    old_single, old_bundle = archive(inputs/OLD_SINGLE), archive(inputs/OLD_BUNDLE)
    require(old_single[EXT] == old_bundle[EXT], 'Baseline SwiftKey runtime differs')
    require(sha(old_single[EXT]) == '1f88dff32491bbed606026166b81fe45abdda482e690f25c4f9f7eff8d29bd7f', 'Baseline extension differs')
    require({n:v for n,v in old_single.items() if n.startswith('swiftkey/')} == {n:v for n,v in old_bundle.items() if n.startswith('swiftkey/')}, 'Baseline dictionaries differ')
    require({n for n in old_single if n.startswith(JVM_PREFIX) and n.endswith('.class')} == set(JVM_CLASSES), 'Baseline standalone JVM class set differs')
    require({n for n in old_bundle if n.startswith(JVM_PREFIX) and n.endswith('.class')} == set(JVM_CLASSES), 'Baseline combined JVM class set differs')
    require(all(old_bundle[n] == old_single[n] for n in JVM_CLASSES), 'Baseline JVM classes differ')
    (work/'old.mpe').write_bytes(old_single[EXT])
    javac(['-cp', tools/'morphe.jar', '-d', work/'tool_classes', ROOT/'tools_java/Merge186.java', ROOT/'tools_java/Assemble186.java'], work/'javac-tools.txt')
    javac(['-cp', tools/'apktool.jar', '-d', work/'tool_classes', ROOT/'tools_java/Dump186.java'], work/'javac-dump.txt')
    dump = ['java', '-cp', str(work/'tool_classes')+os.pathsep+str(tools/'apktool.jar'), 'Dump186']
    run(dump+[work/'old.mpe', work/'before'], work/'dump-before.txt')
    before = smali_files(work/'before'); require(len(before) == 49 and not any(helper(n) for n in before), 'Unexpected runtime inventory')
    hook_source = add_runtime_hooks(before)
    for name, raw in hook_source.items():
        path = work/'hooks'/name; path.parent.mkdir(parents=True, exist_ok=True); path.write_bytes(raw)
    run(['java', '-cp', str(work/'tool_classes')+os.pathsep+str(tools/'morphe.jar'), 'Assemble186', work/'hooks', work/'hooks.dex'], work/'assemble-hooks.txt')
    production, stubs = sorted((ROOT/'src').rglob('*.java')), sorted((ROOT/'stubs').rglob('*.java'))
    require(production and stubs and all(p.relative_to(ROOT/'src').as_posix() == 'hiro/swiftkey/EmojiPanelUi.java' for p in production), 'Unexpected production source set')
    javac(['--release', '8', '-g:none', '-encoding', 'UTF-8', '-cp', tools/'android.jar', '-d', work/'classes', *stubs, *production], work/'javac-runtime.txt')
    with zipfile.ZipFile(work/'runtime.jar', 'w') as runtime, zipfile.ZipFile(work/'stubs.jar', 'w') as stubjar:
        for p in sorted((work/'classes').rglob('*.class')):
            name = p.relative_to(work/'classes').as_posix()
            is_runtime = name == 'hiro/swiftkey/EmojiPanelUi.class' or name.startswith('hiro/swiftkey/EmojiPanelUi$')
            (runtime if is_runtime else stubjar).write(p, name)
    run(['java', '-cp', tools/'r8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26', '--lib', tools/'android.jar', '--classpath', work/'stubs.jar', '--output', work/'new_dex', work/'runtime.jar'], work/'d8.txt')
    run(['java', '-cp', str(work/'tool_classes')+os.pathsep+str(tools/'morphe.jar'), 'Merge186', work/'old.mpe', work/'new_dex/classes.dex', work/'hooks.dex', work/'new.mpe'], evidence/'merge.txt')
    for name, path in [('after', work/'new.mpe'), ('overlay', work/'new_dex/classes.dex'), ('hooks-check', work/'hooks.dex')]: run(dump+[path, work/name], work/('dump-'+name+'.txt'))
    after, overlay, checked_hooks = (smali_files(work/n) for n in ('after', 'overlay', 'hooks-check'))
    require(checked_hooks == hook_source, 'Hook assembly changed existing instructions or labels')
    require(all(helper(n) for n in overlay), 'Runtime overlay contains an unrelated class')
    require({n:v for n,v in before.items() if n not in HOOKS} == {n:v for n,v in after.items() if n not in HOOKS and not helper(n)}, 'Existing unrelated runtime changed')
    require({**hook_source, **overlay} == {n:v for n,v in after.items() if n in HOOKS or helper(n)}, 'Merged runtime overlay differs')
    require(set(after) == set(before)|set(overlay), 'Existing runtime classes removed or unexpected classes added')
    for name, raw in {**hook_source, **overlay}.items():
        path = evidence/'runtime'/name; path.parent.mkdir(parents=True, exist_ok=True); path.write_bytes(raw)
    extension = (work/'new.mpe').read_bytes()
    proof_path = ROOT/'tests/native_ui_review.json'; require(proof_path.is_file(), 'Missing native UI target review')
    proof = json.loads(proof_path.read_text())
    require(proof.get('status') == 'PASSED' and proof.get('device_tested') is False and proof.get('original_apk_apply_tested') is False, 'Native UI review scope invalid')
    require(proof.get('apk_sha256') == 'e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7', 'Native UI review APK differs')
    abi_path = ROOT/'tests/native_abi_review.json'; require(abi_path.is_file(), 'Missing runtime ABI review; inspect '+str(work/'new.mpe'))
    abi = json.loads(abi_path.read_text())
    require(abi.get('status') == 'PASSED' and not abi.get('unresolved') and not abi.get('inaccessible') and not abi.get('incompatible_opcodes'), 'Runtime ABI review is not a pass')
    require(abi.get('extension_sha256') == sha(extension) and abi.get('apk_sha256') == proof['apk_sha256'], 'Runtime ABI review targets different extension or APK')
    require(abi.get('device_tested') is False and abi.get('original_apk_apply_tested') is False, 'Runtime ABI review scope invalid')
    runner = ROOT/'tests/run_tests.py'; require(runner.is_file(), 'Missing UI behavior regression tests')
    test_output = run(['python', runner, '--source-dir', ROOT, '--tools', tools, '--work', work/'host_tests'], evidence/'host-tests.txt')
    require('PASS' in test_output, 'Host regression tests did not pass')
    host_report_path = work/'host_tests/report.json'; require(host_report_path.is_file(), 'Missing deterministic host test report')
    host_report = json.loads(host_report_path.read_text())
    require(host_report.get('status') == 'PASSED' and host_report.get('device_tested') is False and host_report.get('android_instrumentation_tested') is False, 'Host test report scope or status differs')
    require(host_report.get('production_source_sha256') == sha((ROOT/'src/hiro/swiftkey/EmojiPanelUi.java').read_bytes()), 'Host tests target different production source')
    require(host_report.get('scenarios', 0) > 0 and host_report.get('assertions', 0) > 0, 'Host tests contain no behavior assertions')
    single = make_mpp(inputs/OLD_SINGLE, output/SINGLE, extension, '1.8.5', '1.8.6')
    bundle = make_mpp(inputs/OLD_BUNDLE, output/BUNDLE, extension, '1.0.288', '1.0.289')
    new_single, new_bundle = archive(output/SINGLE), archive(output/BUNDLE)
    require(new_single[EXT] == new_bundle[EXT], 'Standalone/combined runtime differs')
    require(new_single['classes.dex'] == old_single['classes.dex'] and new_bundle['classes.dex'] == old_bundle['classes.dex'], 'Android patch loader changed')
    require(all(new_single[n] == new_bundle[n] == old_single[n] == old_bundle[n] for n in JVM_CLASSES), 'SwiftKey JVM loaders changed')
    dex_count = struct.unpack_from('<I', new_bundle['classes.dex'], 96)[0]; require(dex_count == 275, 'Android loader class count differs')
    probe = ROOT/'tests/LoaderParityProbe.java'; require(probe.is_file(), 'Missing actual loader regression test')
    javac(['--release', '8', '-g:none', '-encoding', 'UTF-8', '-d', work/'tool_classes', probe], work/'javac-probe.txt')
    runtime_java = os.environ.get('MORPHE_JAVA', 'java'); parity_reports = []
    for tool, suffix in [('morphe.jar', ''), ('morphe-current.jar', '-current')]:
        report_path = evidence/('loader-parity'+suffix+'.json')
        run([runtime_java, '-cp', str(work/'tool_classes')+os.pathsep+str(tools/tool), 'LoaderParityProbe', inputs/OLD_BUNDLE, output/SINGLE, output/BUNDLE, report_path], evidence/('loader-probe'+suffix+'.txt'))
        report = json.loads(report_path.read_text()); require(report.get('status') == 'PASSED', 'Real loader probe failed'); parity_reports.append(report)
    require(parity_reports[0] == parity_reports[1], 'Loader SDKs disagree on selection or metadata')
    qa = {'status': 'UI_OPTIONS_HOST_VERIFIED_DEVICE_UNVERIFIED', 'swiftkey_version': '1.8.6', 'bundle_version': '1.0.289',
          'target_package': 'com.touchtype.swiftkey.beta', 'target_version': '9.13.16.4', 'target_version_code': 1236271168,
          'device_tested': False, 'original_apk_apply_tested': False, 'device_fix_confirmed': False,
          'runtime_unrelated_classes_unchanged': True, 'all_dictionary_resources_preserved': True, 'other_apps_byte_identical': True,
          'standalone_and_bundle_runtime_equal': True, 'loader_entries_byte_identical': True,
          'jvm_swiftkey_loader_matches_standalone': True, 'combined_jvm_swiftkey_discovered': True, 'previous_jvm_patches_preserved': True,
          'all_static_checks_passed': True, 'host_tests_passed': True, 'current_manager_patcher_selection_verified': True, 'two_loader_sdks_agree': True,
          'existing_runtime_changes_are_three_call_insertions': True,
          'native_ui_review_verified': True, 'native_ui_review_replayed_in_ci': False,
          'native_ui_review': {'source': 'tests/native_ui_review.json', 'sha256': sha(proof_path.read_bytes()), 'apk_sha256': proof['apk_sha256']},
          'native_abi_review_verified': True, 'native_abi_replayed_in_ci': False,
          'native_abi_review': {'source': 'tests/native_abi_review.json', 'sha256': sha(abi_path.read_bytes()), 'apk_sha256': abi['apk_sha256'], 'extension_sha256': abi['extension_sha256'],
                               'native_and_extension_reference_count': len(abi.get('native_and_extension_references', [])), 'unresolved': [], 'inaccessible': []},
          'host_test_report': host_report,
          'runtime': {'before_classes': len(before), 'after_classes': len(after), 'unchanged_existing_classes': len(before)-len(HOOKS),
                      'changed': sorted(HOOKS), 'added': sorted(overlay), 'removed': [], 'inserted_calls': 3,
                      'extension_sha256': sha(extension), 'canonical_smali_equality_verified': True, 'exact_hook_inverse_verified': True},
          'runtime_sha256': sha(extension), 'android_loader_class_count': dex_count,
          'loader_sdk_versions_tested': ['morphe-desktop1.17.0-dev.6', 'morphe-desktop1.19.1/patcher2.0.1'],
          'loader_parity': parity_reports[0], 'single': single, 'bundle': bundle,
          'inputs': {n: {'url': URLS[n], 'sha256': pin} for n,pin in PINS.items()}}
    (output/QA_NAME).write_text(json.dumps(qa, ensure_ascii=False, indent=2)+'\n')
    notes = ROOT/'RELEASE_NOTES.txt'; require(notes.is_file(), 'Missing reviewed release notes')
    (output/'RELEASE_NOTES.txt').write_bytes(notes.read_bytes())
    with zipfile.ZipFile(output/SOURCE_ZIP, 'w') as z:
        for p in source_paths(): zip_add(z, 'src/'+p.relative_to(ROOT).as_posix(), p.read_bytes())
        for p in sorted((evidence/'runtime').rglob('*.smali')): zip_add(z, 'evidence/runtime/'+p.relative_to(evidence/'runtime').as_posix(), p.read_bytes())
        zip_add(z, QA_NAME, (output/QA_NAME).read_bytes())
        for suffix in ('', '-current'): zip_add(z, 'evidence/loader-parity'+suffix+'.json', (evidence/('loader-parity'+suffix+'.json')).read_bytes())
    names = (SINGLE, BUNDLE, QA_NAME, SOURCE_ZIP, 'RELEASE_NOTES.txt')
    (output/'SHA256SUMS.txt').write_text(''.join(sha((output/n).read_bytes())+'  '+n+'\n' for n in names))
    print('PASS optional emoji panel controls SwiftKey 1.8.6 / bundle 1.0.289', flush=True)
    return qa
def main():
    p = argparse.ArgumentParser(); p.add_argument('--input-dir', type=Path, required=True); p.add_argument('--tools', type=Path, required=True)
    p.add_argument('--work', type=Path, required=True); p.add_argument('--output', type=Path, required=True); p.add_argument('--download', action='store_true')
    a = p.parse_args(); inputs = a.input_dir.resolve(); tools = a.tools.resolve(); work = a.work.resolve(); output = a.output.resolve()
    if a.download: download(inputs, tools)
    build(inputs, tools, work, output)
if __name__ == '__main__': main()
