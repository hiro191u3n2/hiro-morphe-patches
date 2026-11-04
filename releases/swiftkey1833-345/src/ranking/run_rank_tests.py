#!/usr/bin/env python3
"""Compile the rank-only donor, prove the old method, and run boundary regressions."""
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

ROOT = Path(__file__).resolve().parent
RANK = 'rank(Ljava/util/List;Ll40/c;Z)Ljava/util/List;'
BRIDGE_RANK = 'rank(Ljava/util/List;Ll40/c;)Ljava/util/List;'
BASELINE_RANK_SHA = 'a059975c9295141ff8f7b8000fdb81cf3ee3246cdd2a5077605fe4d1166bb585'
PINS = {
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'apktool.jar': 'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423',
    'r8.jar': 'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
}

def digest(raw):
    return hashlib.sha256(raw).hexdigest()

def sha(path):
    return digest(path.read_bytes())

def require(ok, message):
    if not ok:
        raise RuntimeError(message)

def run(command, log):
    result = subprocess.run([str(x) for x in command], text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    log.parent.mkdir(parents=True, exist_ok=True)
    log.write_text(result.stdout)
    require(result.returncode == 0, str(log) + '\n' + result.stdout[-6000:])
    return result.stdout

def method(path, signature):
    found = [m for m in re.findall(r'^\.method[^\n]+\n.*?^\.end method\n',
                                  path.read_text(), re.M | re.S)
             if m.splitlines()[0].rsplit(' ', 1)[-1] == signature]
    require(len(found) == 1, 'Method must occur exactly once: ' + str(path) + ': ' + signature)
    return found[0].encode()

def add(archive, name, raw):
    info = zipfile.ZipInfo(name, (2026, 11, 4, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = 0o100644 << 16
    archive.writestr(info, raw, compresslevel=9)

def compile_dex(source, tools, folder, dump, bridge=False):
    classes = folder / 'classes'
    classes.mkdir(parents=True)
    sources = sorted((ROOT / 'stubs').rglob('*.java')) + [source]
    if bridge:
        sources.append(ROOT / 'baseline/UsageBridge.java')
    run(['java', 'com.sun.tools.javac.Main', '--release', '8', '-encoding', 'UTF-8',
         '-d', classes, *sources], folder / 'javac.log')
    with zipfile.ZipFile(folder / 'donor.jar', 'w') as donor, zipfile.ZipFile(folder / 'stubs.jar', 'w') as stubs:
        for path in sorted(classes.rglob('*.class')):
            name = path.relative_to(classes).as_posix()
            is_donor = name == 'hiro/swiftkey/FrequencyLearning.class' or (bridge and name == 'hiro/swiftkey/UsageBridge.class')
            add(donor if is_donor else stubs, name, path.read_bytes())
    dex = folder / 'dex'
    dex.mkdir()
    run(['java', '-cp', tools / 'r8.jar', 'com.android.tools.r8.D8', '--debug', '--min-api', '26',
         '--lib', tools / 'android.jar', '--classpath', folder / 'stubs.jar', '--output', dex,
         folder / 'donor.jar'], folder / 'd8.log')
    run(dump + [dex / 'classes.dex', folder / 'smali'], folder / 'dump.log')
    return dex / 'classes.dex'

def replace_once(source, old, new):
    require(source.count(old) == 1, 'Host dependency substitution must occur once: ' + old)
    return source.replace(old, new)

def host(source, folder, baseline):
    folder.mkdir(parents=True)
    classes = folder / 'classes'
    classes.mkdir()
    # Only retained dependency declarations are substituted. rank() is untouched.
    text = replace_once(source.read_text(), 'private static native String reading(String input);',
                        'private static String reading(String input) { return RankTestEnvironment.reading(input); }')
    donor = folder / 'FrequencyLearning.java'
    donor.write_text(text)
    text = (ROOT / 'baseline/UsageBridge.java').read_text()
    text = replace_once(text, 'private static native boolean eligible(l40.c request);',
                        'private static boolean eligible(l40.c request) { return UsageBridgeTestEnvironment.eligible; }')
    text = replace_once(text, 'private static native boolean allowed(UsageBridge$Session session, wb0.n0 state);',
                        'private static boolean allowed(UsageBridge$Session session, wb0.n0 state) { return UsageBridgeTestEnvironment.allowed(session,state); }')
    bridge = folder / 'UsageBridge.java'
    bridge.write_text(text)
    run(['java', 'com.sun.tools.javac.Main', '--release', '8', '-encoding', 'UTF-8', '-d', classes,
         *sorted((ROOT / 'tests/host_stubs').rglob('*.java')),
         *sorted((ROOT / 'tests').glob('*.java')), donor, bridge], folder / 'javac.log')
    output = run(['java', '-cp', classes, 'hiro.swiftkey.RankingTest',
                  'baseline' if baseline else 'fixed'], folder / 'tests.log')
    return json.loads(output.strip().splitlines()[-1])

def build(tools, runtime, work):
    require(not work.exists(), 'Fresh ranking work directory required')
    for name, pin in PINS.items():
        require(sha(tools / name) == pin, 'Tool pin differs: ' + name)
    work.mkdir(parents=True)
    tool_classes = work / 'tool-classes'
    tool_classes.mkdir()
    run(['java', 'com.sun.tools.javac.Main', '-cp', tools / 'apktool.jar', '-d', tool_classes,
         ROOT.parent / 'runtime/tools_java/Dump1814.java'], work / 'compile-dump.log')
    dump = ['java', '-cp', str(tool_classes) + os.pathsep + str(tools / 'apktool.jar'), 'Dump1814']
    run(dump + [runtime, work / 'published'], work / 'dump-published.log')
    manifest = json.loads((ROOT / 'fixtures/manifest.json').read_text())
    require(len(manifest['fixtures']) == 7, 'Ranking evidence inventory differs')
    for name, pin in manifest['fixtures'].items():
        fixture = ROOT / 'fixtures' / name
        require(sha(fixture) == pin['sha256'] and fixture.stat().st_size == pin['bytes'], 'Fixture pin differs: ' + name)
        require(method(work / 'published' / pin['class'], pin['signature']) == fixture.read_bytes(),
                'Fixture is not from the supplied published runtime: ' + name)
    original_rank = method(work / 'published/hiro/swiftkey/FrequencyLearning.smali', RANK)
    require(digest(original_rank) == BASELINE_RANK_SHA, 'Published baseline rank differs')
    baseline_source = ROOT / 'baseline/FrequencyLearning.java'
    fixed_source = ROOT / 'src/hiro/swiftkey/FrequencyLearning.java'
    compile_dex(baseline_source, tools, work / 'baseline', dump, bridge=True)
    reconstructed = method(work / 'baseline/smali/hiro/swiftkey/FrequencyLearning.smali', RANK)
    require(reconstructed == original_rank, 'Baseline Java does not reconstruct published rank exactly')
    reconstructed_bridge = method(work / 'baseline/smali/hiro/swiftkey/UsageBridge.smali', BRIDGE_RANK)
    original_bridge = method(work / 'published/hiro/swiftkey/UsageBridge.smali', BRIDGE_RANK)
    require(reconstructed_bridge == original_bridge, 'Bridge Java does not reconstruct published rank boundary exactly')
    fixed_dex = compile_dex(fixed_source, tools, work / 'fixed', dump)
    fixed_rank = method(work / 'fixed/smali/hiro/swiftkey/FrequencyLearning.smali', RANK)
    require(fixed_rank != original_rank, 'Rank donor did not change')
    require(sorted(p.relative_to(work / 'fixed/smali').as_posix() for p in (work / 'fixed/smali').rglob('*.smali'))
            == ['hiro/swiftkey/FrequencyLearning.smali'], 'Compile stub leaked into donor DEX')
    old = host(baseline_source, work / 'host-baseline', True)
    new = host(fixed_source, work / 'host-fixed', False)
    require(old['status'] == new['status'] == 'PASSED', 'Host tests failed')
    require(old['context_order_regressions'] == 6 and new['context_order_regressions'] == 0,
            'Expected six old ordering failures and zero fixed ordering failures')
    shutil.copyfile(fixed_dex, work / 'fixed.dex')
    report = {
        'status': 'PASSED', 'baseline_runtime_sha256': sha(runtime),
        'baseline_rank_sha256': digest(original_rank), 'fixed_rank_sha256': digest(fixed_rank),
        'baseline_java_matches_published_rank': True, 'bridge_java_matches_published_rank': True,
        'baseline_rank_matches_published': True,
        'fixture_count': 7, 'fixture_manifest_sha256': sha(ROOT / 'fixtures/manifest.json'),
        'baseline_context_order_regressions': 6, 'fixed_context_order_regressions': 0,
        'baseline_host': old, 'fixed_host': new, 'fixed_dex_sha256': sha(work / 'fixed.dex'),
        'preserves_current_order_then_appends_unseen_history': True,
        'blocked_and_revoked_session_boundaries_tested': True,
        'native_preferences_are_modeled_inputs': True,
        'store_and_cleanup_are_models_bound_to_published_method_fixtures': True,
        'merge_must_preserve_all_other_methods_and_classes': True,
        'android_device_tested': False,
    }
    (work / 'report.json').write_text(json.dumps(report, indent=2, sort_keys=True) + '\n')
    print(json.dumps(report, sort_keys=True))

def main():
    parser = argparse.ArgumentParser()
    for name in ('tools', 'runtime', 'work'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    build(args.tools.resolve(), args.runtime.resolve(), args.work.resolve())

if __name__ == '__main__':
    main()
