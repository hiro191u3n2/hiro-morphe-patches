#!/usr/bin/env python3
"""Resolve the native default character for keys whose long press opens a popup."""
from __future__ import annotations
import argparse, copy, hashlib, json, os, re, shutil, struct, subprocess, time, urllib.request, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OLD_SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.12.mpp'
OLD_BUNDLE = 'Hiro_Morphe_Patches_v1.0.295.mpp'
SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.13.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.296.mpp'
QA_NAME = 'QA_SwiftKey_v1.8.13.json'
SOURCE_ZIP = 'SwiftKeyBeta_v1.8.13_sources_and_QA.zip'
MF, EXT = 'META-INF/MANIFEST.MF', 'extensions/swiftkey_japanese.mpe'
STAMP = '2026-10-27T20:00:00'
JVM_PREFIX = 'app/hiro/swiftkey/patches/'
JVM_CLASSES = tuple(JVM_PREFIX + n + '.class' for n in (
    'JapaneseSamsungEmojiPatch', 'MethodContract', 'InputLearningPatch', 'DictionarySupplementPatch',
    'KeySwapPatch', 'PasteProtectionPatch', 'JapaneseGlidePatch', 'DeleteProtectionPatch'))
KEY_SWAP = 'hiro/swiftkey/KeySwap.smali'
GLIDE = 'hiro/swiftkey/JapaneseGlide.smali'
HOOKS = frozenset((KEY_SWAP, GLIDE))
DESCRIPTION = '長押し候補の取得経路を見直し、数字3〜9の上フリックに対応。複数候補があるキーではSwiftKey本来の既定候補を使用。1.8.12の感度と認識方式を維持。実機での修正確認は未実施。'
BASE = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/e862b4c8977907e962b798c9b0dddb1a76a4e1e4/downloads/'
PINS = {
    OLD_SINGLE: '027de3ebce368dcbd983a3cca788d3e601b9fda055b25f4d32da54d7dbee99d4',
    OLD_BUNDLE: '5703cd152ca89d76aa6e1e3c1b29ab133e7075d74ce133afd449a973f6964185',
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
def helper(path): return path == 'hiro/swiftkey/FlickSymbols.smali' or path.startswith('hiro/swiftkey/FlickSymbols$')
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
    names = ('build1813.py', 'publish1813.py', 'publisher_tests.py', 'workflow.yml', 'RELEASE_NOTES.txt', 'README.txt', 'audit_runtime_abi.py')
    paths = [ROOT / n for n in names if (ROOT / n).is_file()]
    for name in ('src', 'stubs', 'tests', 'tools_java'):
        paths.extend(p for p in (ROOT / name).rglob('*') if p.is_file() and p.suffix in ('.java', '.py', '.json', '.txt', '.md', '.yml'))
    return sorted(set(paths))
def zip_add(z, name, raw):
    info = zipfile.ZipInfo(name, (2026, 10, 27, 20, 0, 0)); info.compress_type = zipfile.ZIP_DEFLATED; info.external_attr = 0o100644 << 16
    z.writestr(info, raw, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)

def build(inputs, tools, work, output, compile_only=False):
    for name, pin in PINS.items(): require(sha(((inputs if name.endswith('.mpp') else tools) / name).read_bytes()) == pin, 'Pinned input differs: ' + name)
    if work.exists(): shutil.rmtree(work)
    for d in (work, work/'tool_classes', work/'classes', work/'new_dex', output, output/'evidence'): d.mkdir(parents=True, exist_ok=True)
    evidence = output/'evidence'
    if (evidence/'runtime').exists(): shutil.rmtree(evidence/'runtime')
    old_single, old_bundle = archive(inputs/OLD_SINGLE), archive(inputs/OLD_BUNDLE)
    require(old_single[EXT] == old_bundle[EXT], 'Baseline SwiftKey runtime differs')
    require(sha(old_single[EXT]) == '2921df5d3a0f9be9e85fb25baada590f9cab9fc589f63161837227b7206ae80a', 'Baseline extension differs')
    require({n:v for n,v in old_single.items() if n.startswith('swiftkey/')} == {n:v for n,v in old_bundle.items() if n.startswith('swiftkey/')}, 'Baseline dictionaries differ')
    require({n for n in old_single if n.startswith(JVM_PREFIX) and n.endswith('.class')} == set(JVM_CLASSES), 'Baseline standalone JVM class set differs')
    require({n for n in old_bundle if n.startswith(JVM_PREFIX) and n.endswith('.class')} == set(JVM_CLASSES), 'Baseline combined JVM class set differs')
    require(all(old_bundle[n] == old_single[n] for n in JVM_CLASSES), 'Baseline JVM classes differ')
    (work/'old.mpe').write_bytes(old_single[EXT])
    javac(['-cp', tools/'morphe.jar', '-d', work/'tool_classes', ROOT/'tools_java/Merge1813.java'], work/'javac-tools.txt')
    javac(['-cp', tools/'apktool.jar', '-d', work/'tool_classes', ROOT/'tools_java/Dump1813.java'], work/'javac-dump.txt')
    dump = ['java', '-cp', str(work/'tool_classes')+os.pathsep+str(tools/'apktool.jar'), 'Dump1813']
    run(dump+[work/'old.mpe', work/'before'], work/'dump-before.txt')
    before = smali_files(work/'before')
    require(len(before) == 59 and len([n for n in before if helper(n)]) == 3, 'Unexpected baseline runtime inventory')
    require(all(before[n].count(b'Lhiro/swiftkey/FlickSymbols;->') == count for n, count in ((KEY_SWAP, 2), (GLIDE, 3))), 'Existing flick hooks differ')
    production, stubs = sorted((ROOT/'src').rglob('*.java')), sorted((ROOT/'stubs').rglob('*.java'))
    require(production and stubs and all(p.relative_to(ROOT/'src').as_posix() == 'hiro/swiftkey/FlickSymbols.java' for p in production), 'Unexpected production source set')
    javac(['--release', '8', '-g:none', '-encoding', 'UTF-8', '-cp', tools/'android.jar', '-d', work/'classes', *stubs, *production], work/'javac-runtime.txt')
    with zipfile.ZipFile(work/'runtime.jar', 'w') as runtime, zipfile.ZipFile(work/'stubs.jar', 'w') as stubjar:
        for p in sorted((work/'classes').rglob('*.class')):
            name = p.relative_to(work/'classes').as_posix()
            is_runtime = name == 'hiro/swiftkey/FlickSymbols.class' or name.startswith('hiro/swiftkey/FlickSymbols$')
            (runtime if is_runtime else stubjar).write(p, name)
    run(['java', '-cp', tools/'r8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26', '--lib', tools/'android.jar', '--classpath', work/'stubs.jar', '--output', work/'new_dex', work/'runtime.jar'], work/'d8.txt')
    run(['java', '-cp', str(work/'tool_classes')+os.pathsep+str(tools/'morphe.jar'), 'Merge1813', work/'old.mpe', work/'new_dex/classes.dex', work/'new.mpe'], evidence/'merge.txt')
    for name, path in [('after', work/'new.mpe'), ('overlay', work/'new_dex/classes.dex')]: run(dump+[path, work/name], work/('dump-'+name+'.txt'))
    after, overlay = (smali_files(work/n) for n in ('after', 'overlay'))
    require(set(overlay) == {n for n in before if helper(n)}, 'Runtime helper class inventory changed')
    require({n:v for n,v in before.items() if not helper(n)} == {n:v for n,v in after.items() if not helper(n)}, 'Existing non-flick runtime changed')
    require(overlay == {n:v for n,v in after.items() if helper(n)}, 'Merged runtime overlay differs')
    require(set(after) == set(before), 'Existing runtime class inventory changed')
    changed = sorted(n for n in before if before[n] != after[n])
    require(changed and all(helper(n) for n in changed), 'Expected only flick-family changes')
    require(all(before[n] == after[n] for n in HOOKS), 'Existing gesture/settings hooks changed')
    for name, raw in overlay.items():
        path = evidence/'runtime'/name; path.parent.mkdir(parents=True, exist_ok=True); path.write_bytes(raw)
    extension = (work/'new.mpe').read_bytes()
    if compile_only:
        print('COMPILED ONLY: runtime output for independent ABI inspection: '+str(work/'new.mpe'), flush=True)
        return {'extension_sha256': sha(extension), 'release_ready': False}
    proof_path = ROOT/'tests/native_flick_review.json'; require(proof_path.is_file(), 'Missing native flick target review')
    proof = json.loads(proof_path.read_text())
    require(proof.get('status') == 'PASSED' and proof.get('device_tested') is False and proof.get('original_apk_apply_tested') is False, 'Native flick review scope invalid')
    require(proof.get('apk_sha256') == 'e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7', 'Native flick review APK differs')
    popup_proof_path = ROOT/'tests/native_popup_review.json'
    require(popup_proof_path.is_file(), 'Missing native popup-default route review')
    popup_proof = json.loads(popup_proof_path.read_text())
    require(popup_proof.get('status') == 'PASSED' and popup_proof.get('device_tested') is False
            and popup_proof.get('original_apk_apply_tested') is False
            and popup_proof.get('apk_sha256') == proof['apk_sha256'], 'Native popup-default review scope or APK differs')
    abi_path = ROOT/'tests/native_abi_review.json'; require(abi_path.is_file(), 'Missing runtime ABI review; inspect '+str(work/'new.mpe'))
    abi = json.loads(abi_path.read_text())
    require(abi.get('status') == 'PASSED' and not abi.get('unresolved') and not abi.get('inaccessible') and not abi.get('incompatible_opcodes'), 'Runtime ABI review is not a pass')
    require(abi.get('extension_sha256') == sha(extension) and abi.get('apk_sha256') == proof['apk_sha256'], 'Runtime ABI review targets different extension or APK')
    require(abi.get('device_tested') is False and abi.get('original_apk_apply_tested') is False, 'Runtime ABI review scope invalid')
    runner = ROOT/'tests/run_tests.py'; require(runner.is_file(), 'Missing flick behavior regression tests')
    test_output = run(['python', runner, '--source-dir', ROOT, '--tools', tools, '--work', work/'host_tests'], evidence/'host-tests.txt')
    require('PASS' in test_output, 'Host regression tests did not pass')
    host_report_path = work/'host_tests/report.json'; require(host_report_path.is_file(), 'Missing deterministic host test report')
    host_report = json.loads(host_report_path.read_text())
    require(host_report.get('status') == 'PASSED' and host_report.get('device_tested') is False and host_report.get('android_instrumentation_tested') is False, 'Host test report scope or status differs')
    require(host_report.get('production_source_sha256') == sha((ROOT/'src/hiro/swiftkey/FlickSymbols.java').read_bytes()), 'Host tests target different production source')
    require(host_report.get('scenarios', 0) > 0 and host_report.get('assertions', 0) > 0, 'Host tests contain no behavior assertions')
    single = make_mpp(inputs/OLD_SINGLE, output/SINGLE, extension, '1.8.12', '1.8.13')
    bundle = make_mpp(inputs/OLD_BUNDLE, output/BUNDLE, extension, '1.0.295', '1.0.296')
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
        report = json.loads(report_path.read_text()); require(report.get('status') == 'PASSED', 'Real loader probe failed')
        require(report.get('selected_extension_stream_verified') is True
                and report.get('loader_selected_extension_matches_packaged_runtime') is True
                and report.get('selected_extension_sha256') == sha(extension)
                and report.get('baseline_extension_sha256') == sha(old_bundle[EXT]),
                'Real loader extension stream differs from the reviewed runtime')
        parity_reports.append(report)
    require(parity_reports[0] == parity_reports[1], 'Loader SDKs disagree on selection or metadata')
    qa = {'status': 'FLICK_POPUP_DEFAULT_HOST_VERIFIED_DEVICE_UNVERIFIED', 'swiftkey_version': '1.8.13', 'bundle_version': '1.0.296',
          'target_package': 'com.touchtype.swiftkey.beta', 'target_version': '9.13.16.4', 'target_version_code': 1236271168,
          'device_tested': False, 'original_apk_apply_tested': False, 'device_fix_confirmed': False,
          'runtime_unrelated_classes_unchanged': True, 'all_dictionary_resources_preserved': True, 'other_apps_byte_identical': True,
          'standalone_and_bundle_runtime_equal': True, 'loader_entries_byte_identical': True,
          'jvm_swiftkey_loader_matches_standalone': True, 'combined_jvm_swiftkey_discovered': True, 'previous_jvm_patches_preserved': True,
          'all_static_checks_passed': True, 'host_tests_passed': True, 'current_manager_patcher_selection_verified': True, 'two_loader_sdks_agree': True,
          'selected_extension_stream_verified': True, 'loader_selected_extension_matches_packaged_runtime': True,
          'runtime_only_flick_family_changed': True, 'existing_hooks_unchanged': True, 'runtime_class_inventory_unchanged': True,
          'native_flick_review_verified': True, 'native_flick_review_replayed_in_ci': False,
          'native_flick_review': {'source': 'tests/native_flick_review.json', 'sha256': sha(proof_path.read_bytes()), 'apk_sha256': proof['apk_sha256']},
          'native_popup_default_review_verified': True, 'native_popup_default_review_replayed_in_ci': False,
          'native_popup_default_review': {'source': 'tests/native_popup_review.json', 'sha256': sha(popup_proof_path.read_bytes()), 'apk_sha256': popup_proof['apk_sha256']},
          'native_abi_review_verified': True, 'native_abi_replayed_in_ci': False,
          'native_abi_review': {'source': 'tests/native_abi_review.json', 'sha256': sha(abi_path.read_bytes()), 'apk_sha256': abi['apk_sha256'], 'extension_sha256': abi['extension_sha256'],
                               'native_and_extension_reference_count': len(abi.get('native_and_extension_references', [])), 'unresolved': [], 'inaccessible': []},
          'host_test_report': host_report,
          'runtime': {'before_classes': len(before), 'after_classes': len(after), 'unchanged_existing_classes': len(before)-len(changed),
                      'changed': changed, 'added': [], 'removed': [], 'inserted_flick_calls': 0,
                      'unchanged_non_flick_classes': len(before)-len(overlay),
                      'extension_sha256': sha(extension), 'canonical_smali_equality_verified': True, 'existing_hooks_unchanged': True},
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
    print('PASS upward-flick native popup default selection and diagnostics SwiftKey 1.8.13 / bundle 1.0.296', flush=True)
    return qa
def main():
    p = argparse.ArgumentParser(); p.add_argument('--input-dir', type=Path, required=True); p.add_argument('--tools', type=Path, required=True)
    p.add_argument('--work', type=Path, required=True); p.add_argument('--output', type=Path, required=True); p.add_argument('--download', action='store_true'); p.add_argument('--compile-only', action='store_true', help='Build runtime only for independent ABI review; never emits release MPPs')
    a = p.parse_args(); inputs = a.input_dir.resolve(); tools = a.tools.resolve(); work = a.work.resolve(); output = a.output.resolve()
    if a.download: download(inputs, tools)
    build(inputs, tools, work, output, a.compile_only)
if __name__ == '__main__': main()
