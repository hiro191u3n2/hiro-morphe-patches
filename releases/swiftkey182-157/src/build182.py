#!/usr/bin/env python3
"""Rebuild the one-call SwiftKey cursor fix from pinned published input MPPs."""
from __future__ import annotations
import argparse, copy, hashlib, json, os, re, subprocess, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OLD_SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.1.mpp'
OLD_BUNDLE = 'Hiro_Morphe_Patches_v1.0.156.mpp'
SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.2.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.157.mpp'
EXT = 'extensions/swiftkey_japanese.mpe'
MANIFEST = 'META-INF/MANIFEST.MF'
CURSOR = 'hiro/swiftkey/CursorLatinBoundary.smali'
HELPER = 'hiro/swiftkey/UnicodeLatinToken.smali'
OLD_CALL = 'Lhiro/swiftkey/LatinBoundary;->isLatinToken(Ljava/lang/String;)Z'
NEW_CALL = 'Lhiro/swiftkey/UnicodeLatinToken;->isLatinToken(Ljava/lang/String;)Z'
PINS = {
    OLD_SINGLE: '077ad014caa5825a690fa2aad9d4d743ecc36267ae86491563e12ffade9ee8e0',
    OLD_BUNDLE: '40c885d205d0a087192be599263a13858ff5c48fec935835791bfc93a364de88',
    'apktool.jar': 'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423',
    'morphe.jar': 'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
    'r8.jar': 'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
}
URLS = {
    OLD_SINGLE: 'https://hiro-morphe-patches.otoha10.chatgpt.site/' + OLD_SINGLE,
    OLD_BUNDLE: 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/906600c1098fdd412ce8b22018929850e2543d14/downloads/' + OLD_BUNDLE,
    'apktool.jar': 'https://github.com/iBotPeaches/Apktool/releases/download/v3.0.3/apktool_3.0.3.jar',
    'morphe.jar': 'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.17.0-dev.6/morphe-desktop-1.17.0-dev.6-all.jar',
    'r8.jar': 'https://storage.googleapis.com/r8-releases/raw/8.7.18/r8.jar',
}
DESCRIPTION = ('日本語入力中、カーソルを戻した既存英字の自動選択・再変換を抑止。'
               'Pokémonのé、結合アクセント、全角・互換ラテン文字を判定対象に追加。'
               '既存の日本語変換、明示範囲選択、辞書、キー配置、LINE改行を維持。実機未確認。')
NOTES = '''Microsoft SwiftKey Beta v1.8.2 / Hiro Morphe Patches v1.0.157

日本語入力中に「UNITE | Pokémon UNITE API」を貼り付け、カーソルを戻した際、
「Pokémon」などの英字を日本語の再変換対象として自動選択する漏れを修正します。
従来の判定はASCII限定で、éや結合アクセントを含む既存単語を除外していました。
カーソル移動後の既存保護処理からUnicode対応判定を呼ぶようにしました。
全角英数、ラテン文字のアクセント、一般的な引用符・ハイフン、互換ラテン表記にも対応します。
正規化は判定だけに使い、貼り付けた本文・アクセント・字体・カーソル位置を書き換えません。

新しく入力する日本語ローマ字、明示的な範囲選択、英語入力モードの既存処理を維持します。
通常の数字だけ・記号だけ・日本語や他文字体系を混ぜた変換文字列は対象外です。
NFKCでラテン文字へ変わる互換表記（全角、装飾英字、ローマ数字等）は保護対象です。
日本語と英字を含む変換文字列全体を無条件に確定する変更ではありません。

元の単体v1.8.1と総合v1.0.156が土台です。
各MPPの変更はマニフェストとSwiftKey拡張DEXの2エントリーだけです。
拡張の既存35クラス、旧言語切替のLatinBoundary、キー配置、横画面修正、
辞書、ポケモン名・スラング、候補学習、Samsung絵文字、LINE長押し改行を保持します。
既存CursorLatinBoundaryは1か所の判定呼び出し先だけを変更し、新規1クラスを追加します。
総合版のULikeを含む他アプリ用コード・資産とトップclasses.dexはバイト一致です。

検証: 実際に組み込むJava判定のホスト試験、旧ASCII不具合の分類再現、DEX再展開比較、
既存の言語・選択・カーソル移動ガードの構造確認、ZIP全エントリー比較を実施します。
Morphe Desktopで単体のパッチ一覧を読み込み、既存の対応アプリ・パッチ名を確認します。
構造検査はSwiftKeyのネイティブ変換エンジンや実際の入力欄の動作試験ではありません。
今回の未改造APKへの一括適用、Android/ART実行、Galaxy実機での自動選択解消は未確認です。
GitHub側では同じソースを再ビルドし、検証したMPPとのSHA-256一致を条件に公開します。

対象: Microsoft SwiftKey Beta 9.13.16.4（versionCode 1236271168）。
Morphe Managerでソースを更新してから、元の未改造APK/APKSへ
「日本語入力・予測変換を改善しSamsung絵文字に統一」を再適用してください。
更新インストールには従来と同じ署名を使います。アプリデータ削除や設定初期化は不要です。
ソースの更新だけではインストール済みSwiftKeyの動作は変わりません。
'''

def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def require(ok: bool, message: str):
    if not ok:
        raise RuntimeError(message)

def run(command, log: Path):
    p = subprocess.run([str(x) for x in command], text=True,
                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    log.write_text(p.stdout, encoding='utf-8')
    print(p.stdout, end='', flush=True)
    require(p.returncode == 0, 'Command failed: ' + str(command[0]))
    return p.stdout

def parse_manifest(data):
    lines = []
    for line in data.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' ') and lines:
            lines[-1] += line[1:]
        elif line:
            lines.append(line)
    return {k.decode('ascii'): v.decode('utf-8')
            for k, v in (line.split(b': ', 1) for line in lines)}

def write_manifest(values):
    result = []
    for key, value in values.items():
        line = b''
        for char in key + ': ' + value:
            raw = char.encode('utf-8')
            if len(line) + len(raw) > 70:
                result.append(line)
                line = b' '
            line += raw
        result.append(line)
    data = b'\r\n'.join(result) + b'\r\n\r\n'
    require(parse_manifest(data) == values, 'Manifest UTF-8 roundtrip differs')
    return data

def files_under(path):
    return {p.relative_to(path).as_posix(): p.read_bytes() for p in path.rglob('*.smali')}

def make_mpp(original, output, extension, old_version, version):
    with zipfile.ZipFile(original) as old:
        names = old.namelist()
        require(old.testzip() is None and len(names) == len(set(names)), 'Invalid baseline ZIP')
        manifest = parse_manifest(old.read(MANIFEST))
        require(manifest['Version'] == old_version, 'Unexpected baseline version')
        manifest.update(Version=version, Timestamp='2026-10-07T00:00:00', Description=DESCRIPTION)
        manifest_bytes = write_manifest(manifest)
        with zipfile.ZipFile(output, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as new:
            for info in old.infolist():
                data = extension if info.filename == EXT else manifest_bytes if info.filename == MANIFEST else old.read(info)
                new.writestr(copy.copy(info), data)
        with zipfile.ZipFile(output) as new:
            require(new.testzip() is None and new.namelist() == names, 'ZIP structure changed')
            changed = [n for n in names if new.read(n) != old.read(n)]
            require(set(changed) == {MANIFEST, EXT}, 'Unexpected changed entries: ' + repr(changed))
            return {'changed_entries': changed, 'unchanged_entries': len(names) - 2,
                    'top_classes_dex_byte_identical': new.read('classes.dex') == old.read('classes.dex'),
                    'zip_crc_verified': True,
                    'entries': {n: {'unchanged': n not in changed, 'sha256': sha(new.read(n)),
                                    'bytes': len(new.read(n))} for n in names}}

def build(input_dir, tools, work, output):
    for name, pin in PINS.items():
        p = (input_dir if name.endswith('.mpp') else tools) / name
        require(sha(p.read_bytes()) == pin, 'Pinned input differs: ' + name)
    work.mkdir(parents=True, exist_ok=True)
    output.mkdir(parents=True, exist_ok=True)
    evidence = output / 'evidence'
    evidence.mkdir(exist_ok=True)
    for name in ['tool_classes', 'dump_classes', 'helper_classes', 'test_classes', 'd8', 'cursor_smali/hiro/swiftkey']:
        (work / name).mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(input_dir / OLD_SINGLE) as s, zipfile.ZipFile(input_dir / OLD_BUNDLE) as b:
        for n in s.namelist():
            if n.startswith(('swiftkey/', 'extensions/')):
                require(s.read(n) == b.read(n), 'Single/bundle baseline differs: ' + n)
        (work / 'old.mpe').write_bytes(b.read(EXT))
        (work / 'old-bundle.dex').write_bytes(b.read('classes.dex'))
        (work / 'old-single.dex').write_bytes(s.read('classes.dex'))
    run(['javac', '-cp', tools / 'morphe.jar', '-d', work / 'tool_classes', ROOT / 'Dex182.java'], work / 'javac-tool.log')
    run(['javac', '-cp', tools / 'apktool.jar', '-d', work / 'dump_classes', ROOT / 'Dump182.java'], work / 'javac-dump.log')
    dump = ['java', '-cp', str(work / 'dump_classes') + os.pathsep + str(tools / 'apktool.jar'), 'Dump182']
    dex = ['java', '-cp', str(work / 'tool_classes') + os.pathsep + str(tools / 'morphe.jar'), 'Dex182']
    run(dump + [work / 'old.mpe', work / 'before'], work / 'dump-before.log')
    before = files_under(work / 'before')
    require(len(before) == 36 and CURSOR in before and HELPER not in before, 'Unexpected baseline extension')
    cursor = before[CURSOR].decode()
    require(cursor.count(OLD_CALL) == 1 and NEW_CALL not in cursor, 'Expected exactly one matcher call')
    start = cursor.index('.method static afterSelection(')
    end = cursor.index('.end method', start)
    require(start < cursor.index(OLD_CALL) < end, 'Matcher call is outside guarded afterSelection')
    patched = cursor.replace(OLD_CALL, NEW_CALL)
    (work / 'cursor_smali' / CURSOR).write_text(patched)
    run(dex + ['assemble', work / 'cursor_smali', work / 'cursor.dex'], work / 'assemble-cursor.log')
    run(['javac', '--release', '8', '-g:none', '-encoding', 'UTF-8', '-d', work / 'helper_classes',
         ROOT / 'hiro/swiftkey/UnicodeLatinToken.java'], work / 'javac-helper.log')
    run(['java', '-cp', tools / 'r8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26',
         '--output', work / 'd8', work / 'helper_classes/hiro/swiftkey/UnicodeLatinToken.class'], work / 'd8.log')
    run(dex + ['merge', work / 'old.mpe', work / 'cursor.dex', work / 'd8/classes.dex', work / 'new.mpe'], work / 'merge.log')
    run(dump + [work / 'new.mpe', work / 'after'], work / 'dump-after.log')
    after = files_under(work / 'after')
    require(set(after) == set(before) | {HELPER}, 'Extension class set differs')
    for n, content in before.items():
        require(after[n] == (patched.encode() if n == CURSOR else content), 'Unexpected runtime change: ' + n)
    run(dump + [work / 'd8/classes.dex', work / 'helper_dump'], work / 'dump-helper.log')
    require(files_under(work / 'helper_dump') == {HELPER: after[HELPER]}, 'Production helper differs after merge')
    # Compare the active Android patch loaders, independent of ZIP/class order and index relocation.
    run(dump + [work / 'old-bundle.dex', work / 'bundle_patchers', 'Lapp/hiro/swiftkey/.*'], work / 'dump-bundle-patchers.log')
    run(dump + [work / 'old-single.dex', work / 'single_patchers'], work / 'dump-single-patchers.log')
    require(files_under(work / 'bundle_patchers') == files_under(work / 'single_patchers'), 'Single/bundle SwiftKey Android loaders differ')
    run(['javac', '--release', '8', '-encoding', 'UTF-8', '-cp', work / 'helper_classes', '-d', work / 'test_classes',
         ROOT / 'tests/UnicodeLatinTokenTest.java'], work / 'javac-tests.log')
    test_output = run(['java', '-cp', str(work / 'helper_classes') + os.pathsep + str(work / 'test_classes'),
                      'UnicodeLatinTokenTest', work / 'after' / CURSOR], evidence / 'host-tests.txt')
    (evidence / 'before-CursorLatinBoundary.smali').write_bytes(before[CURSOR])
    (evidence / 'after-CursorLatinBoundary.smali').write_bytes(after[CURSOR])
    (evidence / 'UnicodeLatinToken.smali').write_bytes(after[HELPER])
    single_report = make_mpp(input_dir / OLD_SINGLE, output / SINGLE, (work / 'new.mpe').read_bytes(), '1.8.1', '1.8.2')
    bundle_report = make_mpp(input_dir / OLD_BUNDLE, output / BUNDLE, (work / 'new.mpe').read_bytes(), '1.0.156', '1.0.157')
    listing = run(['java', '-jar', tools / 'morphe.jar', 'list-patches', '--patches', output / SINGLE, '-v', '-o'], evidence / 'morphe-single-list.txt')
    require('日本語入力・予測変換を改善しSamsung絵文字に統一' in listing, 'SwiftKey patch is absent')
    require(listing.count('Name: ') == 1, 'Unexpected standalone patch count')
    qa = {
        'status': 'HOST_AND_STATIC_CHECKS_PASSED_DEVICE_UNVERIFIED',
        'swiftkey_version': '1.8.2', 'bundle_version': '1.0.157',
        'target_package': 'com.touchtype.swiftkey.beta', 'target_version': '9.13.16.4', 'target_version_code': 1236271168,
        'device_tested': False, 'original_apk_apply_tested': False,
        'cause_confirmed_in_code': 'ASCII-only token check excludes accented Latin in existing cursor protection',
        'device_cause_confirmed': False, 'device_fix_confirmed': False,
        'host_tests_output': test_output,
        'runtime': {'existing_class_count': 36, 'unchanged_existing_classes': 35,
                    'changed_existing_methods': 1, 'changed_invocation_references': 1, 'new_classes': 1,
                    'canonical_dex_equality_verified': True, 'original_language_switch_preserved': True,
                    'japanese_mode_guard_preserved': True, 'explicit_selection_guard_preserved': True,
                    'cursor_change_guards_preserved': True, 'document_mutation_not_added': True},
        'single_and_bundle_swiftkey_android_loaders_equal': True,
        'morphe_desktop_single_list_passed': True,
        'morphe_manager_device_tested': False,
        'all_dictionary_resources_preserved': True, 'other_apps_byte_identical': True,
        'base_and_tool_sha256': PINS,
        'single_preservation': single_report, 'bundle_preservation': bundle_report,
        'artifacts': {n: {'bytes': (output / n).stat().st_size, 'sha256': sha((output / n).read_bytes())} for n in (SINGLE, BUNDLE)},
    }
    (output / 'QA_SwiftKey_v1.8.2.json').write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    (output / 'RELEASE_NOTES.txt').write_text(NOTES)
    print('PASS: original selection/language guards and all non-target runtime preserved; only two ZIP entries changed per MPP')
    print(json.dumps(qa['artifacts'], indent=2))
    finalize(output)
    return qa

def finalize(output):
    """Package reproducible source/evidence; no timestamps, private APK or signing data."""
    source_zip = 'SwiftKeyBeta_v1.8.2_sources_and_QA.zip'
    content = {}
    for path in sorted(ROOT.rglob('*')):
        if path.is_file() and path.suffix in ('.java', '.py'):
            content['src/' + path.relative_to(ROOT).as_posix()] = path.read_bytes()
    require('src/publish182.py' in content, 'Publication source missing from source package')
    for path in sorted((output / 'evidence').rglob('*')):
        if path.is_file():
            content['evidence/' + path.relative_to(output / 'evidence').as_posix()] = path.read_bytes()
    for name in ('QA_SwiftKey_v1.8.2.json', 'RELEASE_NOTES.txt'):
        content[name] = (output / name).read_bytes()
    content['BUILD_INPUTS.json'] = (json.dumps({'sha256': PINS, 'urls': URLS, 'java': '21',
        'command': 'python src/build182.py --input input --tools tools --work build --output dist'},
        ensure_ascii=False, indent=2) + '\n').encode()
    with zipfile.ZipFile(output / source_zip, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for name, raw in sorted(content.items()):
            info = zipfile.ZipInfo(name, (2026, 10, 7, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            z.writestr(info, raw, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)
    with zipfile.ZipFile(output / source_zip) as z:
        require(z.testzip() is None and {n: z.read(n) for n in z.namelist()} == content, 'Source archive differs')
    names = (SINGLE, BUNDLE, 'QA_SwiftKey_v1.8.2.json', source_zip, 'RELEASE_NOTES.txt')
    (output / 'SHA256SUMS.txt').write_text(''.join(sha((output / n).read_bytes()) + '  ' + n + '\n' for n in names))
    print('PASS complete deterministic source, evidence, MPPs and SHA256SUMS')

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--input', required=True, type=Path)
    p.add_argument('--tools', required=True, type=Path)
    p.add_argument('--work', required=True, type=Path)
    p.add_argument('--output', required=True, type=Path)
    args = p.parse_args()
    build(args.input.resolve(), args.tools.resolve(), args.work.resolve(), args.output.resolve())
