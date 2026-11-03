#!/usr/bin/env python3
"""Verify small pinned native commit excerpts; this does not execute Android."""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parent / 'fixtures/native_separator'
MANIFEST_SHA256 = '651a86c4f75b2b2d716941b9f9a09bb87ab68b7d3bb039d68a5bb4cb580dde30'
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
    require(sha(manifest_bytes) == MANIFEST_SHA256, 'Native separator manifest pin changed')
    manifest = json.loads(manifest_bytes)
    require(manifest['reference_apk_sha256'] == APK_SHA256, 'Wrong native reference APK')
    fragments = {}
    for row in manifest['fragments']:
        raw = (ROOT / row['name']).read_bytes()
        require(len(raw) == row['bytes'] and sha(raw) == row['sha256'], 'Fragment changed: ' + row['name'])
        fragments[row['name']] = raw.decode()
        if native_root is not None:
            source_bytes = (native_root / row['class_path']).read_bytes()
            require(sha(source_bytes) == row['class_sha256'], 'Native class changed: ' + row['class_path'])
            source = source_bytes.decode()
            if row['method'] is not None:
                start = source.index(row['method'])
                end = source.index('.end method', start) + len('.end method')
                require(sha(source[start:end].encode()) == row['method_sha256'], 'Native method changed')
            lines = source.splitlines(keepends=True)
            excerpt = ''.join(lines[row['start_line'] - 1:row['end_line']])
            require(excerpt == raw.decode(), 'Fragment differs from stated native source lines')
    require(len(fragments) == 29, 'Incomplete native separator evidence')
    require(set(p.name for p in ROOT.iterdir()) == set(fragments) | {'manifest.json'}, 'Unexpected native separator evidence file')
    if apk is not None:
        require(sha(apk.read_bytes()) == APK_SHA256, 'Reference APK differs')
        with zipfile.ZipFile(apk) as archive:
            for name, expected in manifest['reference_dex_sha256'].items():
                require(sha(archive.read(name)) == expected, 'Reference DEX differs: ' + name)

    def ordered(name, *tokens):
        body = fragments[name + '.smali.txt']
        position = 0
        for token in tokens:
            found = body.find(token, position)
            require(found >= 0, 'Native operation missing/out of order: ' + name + ': ' + token)
            position = found + len(token)

    for name in ('immutable_setter', 'flow_immutable_setter'):
        ordered(name, '.method public final setTrailingSeparator', 'new-instance p1, Ljava/lang/UnsupportedOperationException;',
                'invoke-direct {p1}, Ljava/lang/UnsupportedOperationException;-><init>()V', 'throw p1')
        require('if-' not in fragments[name + '.smali.txt'], 'Setter throw unexpectedly conditional')
    ordered('complex_extends_immutable', '.class public final Lm40/w;', '.super Lm40/c0;')
    ordered('engine_to_factory', 'Lm40/r;->d(Ljava/util/List;Ll40/c;Lm40/c;)Ljava/util/ArrayList;', 'move-result-object')
    ordered('prediction_factory', 'new-instance v5, Lm40/w;', 'Lm40/c0;-><init>',
            'new-instance v5, Lm40/c0;', 'Lm40/c0;-><init>', 'invoke-virtual {p2, v5}')
    ordered('native_event_factory', 'new-instance p1, Lyb0/e0;', 'invoke-direct {p1, v2, p2, p3, p4}', 'return-object p1')
    ordered('site_precommit_default', 'getTrailingSeparator()Ljava/lang/String;', 'if-nez v0, :cond_0',
            'Lz90/c;->a(Lec0/n;)Ljava/lang/String;', 'setTrailingSeparator(Ljava/lang/String;)V')
    ordered('site_input_connection', 'getTrailingSeparator()Ljava/lang/String;', 'move-result-object v4',
            'Lwb0/e;->e(Ljava/lang/String;Lec0/n;Ljava/lang/String;Ljava/lang/String;Z)Z')
    ordered('site_marker_commit', 'Lec0/y;->i(Lm40/b;Lwb0/j;I)V', 'getTrailingSeparator()Ljava/lang/String;',
            'Lec0/f;->l0(Ljava/lang/String;Lec0/y;Ljava/util/List;Ljava/lang/String;)V')
    ordered('site_wrapper_visitor', 'Lm40/x;->b:Lm40/b;', 'getTokens()Ljava/util/List;', 'getTrailingSeparator()Ljava/lang/String;')
    ordered('site_native_visitor_suffix', 'invoke-virtual {v2}, Lm40/c0;->getTrailingSeparator()',
            'move-result-object v6', 'Ljava/lang/StringBuilder;->append(Ljava/lang/String;)')
    ordered('site_native_visitor_marker', 'invoke-virtual {v2}, Lm40/c0;->getTrailingSeparator()',
            'move-result-object v2', 'Lec0/f;->r0()V')
    ordered('site_postcommit_suffix', 'getTrailingSeparator()Ljava/lang/String;', 'move-result-object p2',
            'Lac0/f;->h(Lwb0/n0;Lec0/n;Ljava/lang/String;Z)V')
    site_rows = [row for row in manifest['fragments'] if row['name'].startswith('site_')]
    require(len(site_rows) == manifest['commit_separator_sites'] == 7, 'Expected seven commit separator reads')
    require(len({(row['class_path'], row['method']) for row in site_rows}) == manifest['commit_separator_methods'] == 6,
            'Expected six commit methods')
    for row in site_rows:
        require(fragments[row['name']].count('->getTrailingSeparator()Ljava/lang/String;') == 1,
                'Site excerpt must identify exactly one separator read')
    ordered('visitor_handoff', 'new-instance v0, Lec0/w;', 'Lec0/w;-><init>', 'Lm40/b;->accept(Lm40/a;)Ljava/lang/Object;')
    ordered('context_guard', 'Ldc0/a;->K()Ljava/lang/String;', 'Lm40/b;->subrequest()Ll40/c;',
            'Ll40/c;->i:Ljava/lang/String;', 'Lkotlin/jvm/internal/m;->b(Ljava/lang/Object;Ljava/lang/Object;)Z',
            'if-nez v2, :cond_1', 'Prediction no longer relevant. Ignoring...', 'goto :goto_0',
            ':cond_1', 'Lac0/b;->c(Lwb0/n0;Lm40/b;Lec0/n;Lwb0/j;I)V')
    ordered('context_getter', 'Lec0/f;->i:Lde0/g;', 'Lde0/g;->b:Ljava/lang/Object;',
            'check-cast v0, Ljava/lang/String;', 'return-object v0')
    ordered('request_context_field', 'move-object/from16 p1, p16', 'Ll40/c;->i:Ljava/lang/String;')
    ordered('transparent_visitor', 'Lm40/g0;->a:Lm40/b;', 'Lm40/b;->accept(Lm40/a;)Ljava/lang/Object;')
    ordered('email_visitor_unwrap', manifest['email_wrapper_accessor'], 'Lm40/b;->accept(Lm40/a;)Ljava/lang/Object;')
    ordered('email_get_wrapped', '.method public getWrapped()Lm40/b;', '->mWrappedCandidate:Lm40/b;', 'return-object v0')
    require(len(manifest['wrapper_fields']) == 9, 'Unexpected wrapper chain field count')
    for cls, field in manifest['wrapper_fields'].items():
        require(fragments['wrapper_field_' + cls + '.smali.txt'].strip() == '.field public final ' + field,
                'Wrapper field access or descriptor differs: ' + cls)

    report = {
        'status': 'PASSED', 'assertions': count, 'fragment_count': len(fragments),
        'manifest_sha256': MANIFEST_SHA256, 'reference_apk_sha256': APK_SHA256,
        'fragment_sha256': {row['name']: row['sha256'] for row in manifest['fragments']},
        'native_fluency_candidate_setter_always_throws': True,
        'native_prediction_factory_produces_immutable_candidates': True,
        'native_precommit_null_separator_calls_setter': True,
        'commit_separator_sites': len(site_rows), 'commit_separator_methods': 6,
        'native_visitor_unwraps_candidates': True,
        'native_context_guard_checks_K_against_request_i': True,
        'wrapper_chain_public_field_count': 9, 'email_wrapper_has_public_accessor': True,
        'native_source_lines_rechecked': native_root is not None,
        'reference_apk_bytes_rechecked': apk is not None,
        'android_code_executed': False, 'device_tested': False,
        'user_device_candidate_type_observed': False,
        'scope': 'Pinned native source and call-site evidence; no native engine execution or handset reproduction.'
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
