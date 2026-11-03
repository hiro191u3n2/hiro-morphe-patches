#!/usr/bin/env python3
"""Check pinned native raw-state excerpts; this does not execute Android code."""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parent / 'fixtures/native_raw'
MANIFEST_SHA256 = '38e3602f44537866b10fbb7691fba203f2515c79213a96c9b3581358a373990b'
APK_SHA256 = 'e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7'


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def check(output, native_root=None, apk=None):
    count = 0

    def require(value, message):
        nonlocal count
        count += 1
        if not value:
            raise AssertionError(message)

    manifest_bytes = (ROOT / 'manifest.json').read_bytes()
    require(sha(manifest_bytes) == MANIFEST_SHA256, 'Native raw manifest pin changed')
    manifest = json.loads(manifest_bytes)
    require(manifest['reference_apk_sha256'] == APK_SHA256, 'Wrong native reference APK')
    fragments = {}
    for row in manifest['fragments']:
        raw = (ROOT / row['name']).read_bytes()
        require(len(raw) == row['bytes'] and sha(raw) == row['sha256'], 'Fragment changed: ' + row['name'])
        fragments[row['name']] = raw.decode()
        if native_root is not None:
            source_bytes = (native_root / row['class_path']).read_bytes()
            require(sha(source_bytes) == row['class_sha256'], 'Disassembled class changed: ' + row['class_path'])
            source = source_bytes.decode()
            start = source.index(row['method'])
            end = source.index('.end method', start) + len('.end method')
            require(sha(source[start:end].encode()) == row['method_sha256'], 'Native method changed')
            lines = source.splitlines(keepends=True)
            excerpt = ''.join(lines[row['start_line'] - 1:row['end_line']])
            require(excerpt == raw.decode(), 'Fragment does not match stated native source lines')
    require(len(fragments) == 11, 'Incomplete native raw evidence')
    require(set(p.name for p in ROOT.iterdir()) == set(fragments) | {'manifest.json'}, 'Unexpected native raw evidence file')
    if apk is not None:
        require(sha(apk.read_bytes()) == APK_SHA256, 'Reference APK differs')
        with zipfile.ZipFile(apk) as archive:
            for name, expected in manifest['reference_dex_sha256'].items():
                require(sha(archive.read(name)) == expected, 'Reference DEX differs: ' + name)

    def ordered(name, *tokens):
        body = fragments[name]
        position = 0
        for token in tokens:
            found = body.find(token, position)
            require(found >= 0, 'Native operation missing/out of order: ' + name + ': ' + token)
            position = found + len(token)

    ordered('raw_getter.smali.txt', 'Lec0/f;->a0()Lec0/y;', 'Lec0/y;->l:Ljava/lang/String;', 'return-object v0')
    ordered('raw_append_key.smali.txt', 'Lec0/y;->l:Ljava/lang/String;', 'La6/c;->n(Ljava/lang/StringBuilder;Ljava/lang/String;Ljava/lang/String;)', 'iput-object v4, v0, Lec0/y;->l:Ljava/lang/String;')
    ordered('raw_append_text.smali.txt', 'Lec0/y;->l:Ljava/lang/String;', 'La6/c;->n(Ljava/lang/StringBuilder;Ljava/lang/String;Ljava/lang/String;)', 'iput-object v0, p0, Lec0/y;->l:Ljava/lang/String;')
    ordered('raw_append_helper.smali.txt', 'invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append', 'invoke-virtual {p0, p2}, Ljava/lang/StringBuilder;->append', 'toString()Ljava/lang/String;', 'return-object p0')
    ordered('raw_reset.smali.txt', 'Hangul;->split(Ljava/lang/String;)', 'move-result-object v0', 'iput-object v0, p0, Lec0/y;->l:Ljava/lang/String;')
    ordered('raw_correction_bridge.smali.txt', 'Lec0/y;->t(Ljava/lang/String;)V')
    ordered('raw_prepare_edit.smali.txt', 'const/4 v1, 0x5', 'const/4 v3, 0x6', 'getCorrectionSpanReplacementText()Ljava/lang/String;', 'Lec0/y;->h(Ljava/lang/String;)V', 'iput v3, p0, Lec0/y;->k:I')
    ordered('raw_prepare_provisional_edit.smali.txt', 'getCorrectionSpanReplacementText()Ljava/lang/String;', 'Lec0/y;->h(Ljava/lang/String;)V', 'iput v3, p0, Lec0/y;->k:I')
    ordered('native_romaji_input.smali.txt', 'Ldc0/a;->G()Ljava/lang/String;', 'append(Ljava/lang/String;)', 'append(Ljava/lang/String;)', 'toString()Ljava/lang/String;', 'Japanese;->romajiToHiragana(Ljava/lang/String;)')
    # invoke-direct/range starts with receiver v5; the eleventh argument is v16.
    ordered('request_raw_argument.smali.txt', 'iget-object v5, v6, Lec0/y;->l:Ljava/lang/String;', 'move-object/from16 v16, v5', 'invoke-direct/range {v5 .. v21}, Ll40/c;-><init>')
    ordered('request_raw_field.smali.txt', 'move-object/from16 p1, p11', 'iput-object p1, p0, Ll40/c;->k:Ljava/lang/String;')

    # These translations illustrate the observed append/copy operations only.
    # They do not claim that a handset or the user's recording produced them.
    examples = []
    for prefix, key in [('あ', 'i'), ('愛', 'i'), ('Ａ', 'Ｉ')]:
        marker_raw = prefix + key
        state_raw = marker_raw
        request_raw = marker_raw
        require(state_raw == request_raw and bool(state_raw), 'Translated raw-copy example differs')
        examples.append({'prefix': prefix, 'key': key, 'state_raw': state_raw,
                         'request_raw': request_raw, 'exact_match': True})
    report = {
        'status': 'PASSED', 'assertions': count, 'fragment_count': len(fragments),
        'manifest_sha256': MANIFEST_SHA256, 'reference_apk_sha256': APK_SHA256,
        'fragment_sha256': {row['name']: row['sha256'] for row in manifest['fragments']},
        'native_state_and_request_share_marker_raw': True,
        'native_raw_append_has_no_script_filter': True,
        'native_romaji_call_has_no_all_latin_precondition': True,
        'native_correction_text_can_be_reused_for_editing': True,
        'illustrative_translated_append_copy_cases': examples,
        'native_source_lines_rechecked': native_root is not None,
        'reference_apk_bytes_rechecked': apk is not None,
        'android_code_executed': False, 'device_tested': False,
        'user_video_raw_input_observed': False,
        'scope': 'Pinned source excerpts and translated append/copy examples; not native engine execution or a device reproduction.'
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + '\n')
    return report


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--native-root', type=Path)
    parser.add_argument('--apk', type=Path)
    args = parser.parse_args()
    print(json.dumps(check(args.output, args.native_root, args.apk), ensure_ascii=False))


if __name__ == '__main__':
    main()
