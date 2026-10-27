#!/usr/bin/env python3
"""Run unchanged emoji-slide production Java with isolated recording JVM fakes.

Native dispatch/delegation and helper state ownership are exercised here. Android
rendering, the native engine, native timers and ART are not executed by this test.
"""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source-dir', type=Path, default=Path(__file__).resolve().parent.parent)
    parser.add_argument('--work', type=Path, required=True)
    args = parser.parse_args()
    root, work = args.source_dir.resolve(), args.work.resolve()
    work.mkdir(parents=True, exist_ok=True)
    classes = work / 'classes'
    if classes.exists():
        shutil.rmtree(classes)
    classes.mkdir()
    production = [root / ('src/hiro/swiftkey/' + name + '.java') for name in
                  ('EmojiRecentsSlide', 'EmojiRecentsBridge', 'EmojiToolbar')]
    # A relative-path map avoids duplicate classes. Only this runner's host
    # overrides replace the compile-only native stubs; old test fakes are intact.
    source_map = {name: root / 'stubs' / name for name in
                  ('fc0/h.java', 'fc0/k1.java', 'hc0/b.java', 'cm0/d.java', 'oz/i6.java')}
    directory = root / 'tests/emoji_slide_host_stubs'
    for path in directory.rglob('*.java'):
        source_map[path.relative_to(directory).as_posix()] = path
    harnesses = sorted((root / 'tests/emoji_slide_host').rglob('*.java'))
    if not harnesses or not all(path.is_file() for path in production):
        raise RuntimeError('Missing emoji slide production sources or host harness')
    inputs = [*production, *harnesses, *sorted(source_map.values())]
    input_hashes = {p.relative_to(root).as_posix(): sha(p) for p in inputs}
    command = ['java', 'com.sun.tools.javac.Main', '--release', '8', '-g:none', '-encoding', 'UTF-8',
               '-d', str(classes), *map(str, inputs)]
    compiled = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (work / 'compile.log').write_text(compiled.stdout)
    print(compiled.stdout, end='')
    if compiled.returncode:
        raise RuntimeError('Emoji slide host compilation failed')
    tested = subprocess.run(['java', '-cp', str(classes), 'hiro.swiftkey.EmojiSlideHostTest'],
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (work / 'host.log').write_text(tested.stdout)
    print(tested.stdout, end='')
    match = re.search(r'PASS EmojiSlide scenarios=(\d+) assertions=(\d+)', tested.stdout)
    if tested.returncode or not match:
        raise RuntimeError('Emoji slide host behavior checks failed')
    if input_hashes != {p.relative_to(root).as_posix(): sha(p) for p in inputs}:
        raise RuntimeError('Host test inputs changed while compiling or executing; rerun the settled sources')
    scenarios, assertions = map(int, match.groups())
    report = {
        'status': 'PASSED',
        'scope': 'Unchanged production EmojiRecentsSlide, EmojiRecentsBridge and EmojiToolbar Java with isolated JVM Android/native fakes',
        'scenarios': scenarios, 'assertions': assertions,
        'production_source_sha256': sha(production[0]),
        'production_source_sha256_by_file': {p.relative_to(root).as_posix(): sha(p) for p in production},
        'device_tested': False, 'android_instrumentation_tested': False, 'original_apk_apply_tested': False,
        'native_emoji_key_identity_verified': True,
        'recent_order_limit_and_graphemes_verified': True,
        'native_input_and_history_delegation_verified': True,
        'private_history_guard_delegation_verified': True,
        'input_once_and_failure_no_retry_verified': True,
        'upward_sensitivity_verified': True,
        'ordinary_tap_and_hold_preserved': True,
        'continuous_slide_release_selection_verified': True,
        'outside_release_and_cancel_verified': True,
        'lifecycle_and_multitouch_drain_verified': True,
        'popup_geometry_and_no_focus_steal_verified': True,
        'reentrant_cancel_and_error_drain_verified': True,
        'repeated_gestures_and_listener_cleanup_verified': True,
        'native_input_engine_executed': False,
        'native_history_guard_executed_on_android': False,
        'native_timer_executed': False,
        'test_sources': dict(sorted({**input_hashes, Path(__file__).resolve().relative_to(root).as_posix(): sha(Path(__file__))}.items())),
        'limitations': [
            'Host fakes record native input and history callback delegation; the native IME input engine and real editor composition are not executed.',
            'The callback fake follows the separately reviewed native private-history guard; its behavior is not evidence of Android execution.',
            'Overlay geometry, complete text draw calls and pointer ownership are exercised; Android glyph shaping, rendering and real touch dispatch are not executed.',
            'Native long-press actor identity and deadline preservation are checked; Android native timer scheduling is not executed.',
            'No handset, ART verifier, screen reader or clean-original-APK application was executed.'
        ]
    }
    (work / 'report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print('PASS report.json unchanged production emoji slide behavior verified')


if __name__ == '__main__':
    main()
