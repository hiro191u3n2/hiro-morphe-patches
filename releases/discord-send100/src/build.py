#!/usr/bin/env python3
"""Reproducibly add a Discord raw-resource provider to immutable Hiro 1.0.374.

No APK or Android execution is implied by this build and loader-preservation audit.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import subprocess
import sys
import zip_helpers as z

ROOT = Path(__file__).resolve().parent
BASE_SHA = '20b758f4ff9c8de8900137035ebadf7aae0642ccb0afadc70c5c24c16b12c714'
BASE_VERSION, VERSION, BUNDLE_VERSION = '1.0.374', '1.0.0', '1.0.375'
PREFIX = 'app/hiro/discord/patches/'
PROVIDER = PREFIX + 'DiscordSendButtonPatch.class'
MF = 'META-INF/MANIFEST.MF'
SINGLE = 'Discord_Send_Button_v1.0.0.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.375.mpp'
SDK_SHA = '8044782202eee6bc5f9c426730b99030fb8eb8f44f8e25894682c84d8da69f60'
TOOLS = {
    'ecj.jar': '74c93172475fee0cd4e6eefb44233516dd40525642da35d009146d97ea598229',
    'd8.jar': '38a9ffaaebbd17d1514e3ecbff5a85b989bb13c551b9e224c7409514eea84ff5',
    'classpath.jar': 'fe4bc92815bb14976150e4ec83e4985932b46d1d47edec75d37604481572e864',
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ('base', 'tools', 'sdk', 'out'):
        parser.add_argument('--' + key, type=Path, required=True)
    parser.add_argument('--timestamp', required=True)
    parser.add_argument('--patch-name', default='送信ボタンの表示消失を修正')
    args = parser.parse_args()
    for key in ('base', 'tools', 'sdk', 'out'):
        setattr(args, key, getattr(args, key).resolve())
    z.require(z.sha(args.base) == BASE_SHA, 'Hiro 1.0.374 baseline hash differs')
    z.require(z.sha(args.sdk) == SDK_SHA, 'Pinned Morphe 2.4.0-dev.8 SDK differs')
    for name, expected in TOOLS.items():
        z.require(z.sha(args.tools / name) == expected, 'Pinned tool differs: ' + name)
    z.require(not args.out.exists() or not any(args.out.iterdir()), 'Use a fresh output directory')
    args.out.mkdir(parents=True, exist_ok=True)
    work = args.out / 'work'
    work.mkdir()

    def run(command, log, timeout=600):
        result = subprocess.run(list(map(str, command)), text=True, capture_output=True, timeout=timeout)
        (work / log).write_text(result.stdout + result.stderr)
        z.require(result.returncode == 0, log + ' failed:\n' + (result.stdout + result.stderr)[-10000:])
        return result.stdout

    java_version = run(['java', '--version'], 'java-version.txt')
    z.require('21.0.13' in java_version, 'Reproduce with Java 21.0.13')
    run([sys.executable, ROOT / 'compile_api.py', args.sdk, work / 'compile-api'], 'compile-api.log')
    classes = work / 'classes'
    classes.mkdir()
    sources = sorted((ROOT / 'patch').rglob('*.java'))
    z.require(bool(sources), 'No patch sources in src/patch')
    run(['java', '-jar', args.tools / 'ecj.jar', '-8', '-nowarn', '-encoding', 'UTF-8',
         '-cp', os.pathsep.join(map(str, [work / 'compile-api', args.sdk])),
         '-d', classes, *sources], 'compile-provider.log')
    fresh = {p.relative_to(classes).as_posix(): p.read_bytes() for p in sorted(classes.rglob('*.class'))}
    z.require(PROVIDER in fresh, 'Discord provider is missing')
    z.require(all(name.startswith(PREFIX) for name in fresh), 'Compile-only or unrelated classes leaked')
    z.write_zip(work / 'provider.jar', fresh)
    dex = work / 'dex'
    dex.mkdir()
    run(['java', '-Xmx1g', '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8',
         '--release', '--min-api', '26', '--classpath', args.sdk,
         '--output', dex, work / 'provider.jar'], 'provider-d8.log')
    z.require({p.name for p in dex.iterdir()} == {'classes.dex'}, 'Unexpected DEX output')
    audit = work / 'audit'
    audit.mkdir()
    run(['java', '-jar', args.tools / 'ecj.jar', '-11', '-nowarn', '-encoding', 'UTF-8',
         '-cp', args.tools / 'classpath.jar', '-d', audit, ROOT / 'LoaderAudit.java'], 'compile-audit.log')
    base = z.archive(args.base)
    fields = z.headers(base[MF])
    z.require(fields['Version'] == BASE_VERSION, 'Baseline version differs')
    z.require(not any(name.startswith(PREFIX) for name in base), 'Discord provider already exists')
    z.require(set(base).isdisjoint(fresh), 'New JVM class collides with existing entry')
    (work / 'baseline.dex').write_bytes(base['classes.dex'])
    run(['java', '-Xmx2g', '-cp', os.pathsep.join(map(str, [audit, args.tools / 'classpath.jar'])),
         'LoaderAudit', work / 'baseline.dex', dex / 'classes.dex', work / 'combined.dex',
         work / 'loader-audit.json'], 'loader-audit.log')
    fields.update(Version=BUNDLE_VERSION, Timestamp=args.timestamp,
                  Description='Discord/Venus351.11: repair send-button visibility; preserve existing app patches and resources.')
    single_fields = dict(fields, Name='Hiro Discord Send Button', Version=VERSION)
    combined = dict(base)
    combined.update(fresh)
    combined['classes.dex'] = (work / 'combined.dex').read_bytes()
    combined[MF] = z.manifest(fields)
    # Optional patch-time assets must use this new provider's namespace.
    resources = ROOT / 'resources'
    asset_data = {}
    if resources.exists():
        asset_data = {p.relative_to(resources).as_posix(): p.read_bytes()
                      for p in sorted(resources.rglob('*')) if p.is_file()}
        z.require(all(n.startswith('discord-send100/') for n in asset_data), 'Unowned resource path')
        z.require(set(base).isdisjoint(asset_data), 'New resource replaces existing entry')
        combined.update(asset_data)
    retained = z.write_combined_zip(args.out / BUNDLE, combined, args.base, base, work / 'changed.zip')
    single = {**fresh, **asset_data, MF: z.manifest(single_fields), 'classes.dex': (dex / 'classes.dex').read_bytes()}
    z.write_zip(args.out / SINGLE, single)
    unchanged = {n: b for n, b in base.items() if n not in (MF, 'classes.dex')}
    z.require(all(combined[n] == b for n, b in unchanged.items()), 'Existing archive entry changed')
    discovery = {}
    for name in (SINGLE, BUNDLE):
        output = run(['java', '-jar', args.sdk, 'list-patches', '--patches', args.out / name,
                      '-f', 'com.discord.venus', '-p', '-v'], name + '.listing.txt')
        z.require(output.count('Name: ' + args.patch_name + '\n') == 1,
                  'Patch was not discovered exactly once: ' + name)
        discovery[name] = True
    report = {
        'schema': 'discord-send100-build-v1', 'bundle_version': BUNDLE_VERSION,
        'patch_version': VERSION, 'patch_name': args.patch_name,
        'baseline_sha256': BASE_SHA, 'timestamp': args.timestamp,
        'tool_sha256': dict(TOOLS, **{'morphe-dev19.jar': SDK_SHA}),
        'source_sha256': {p.relative_to(ROOT).as_posix(): z.sha(p)
                          for p in sorted(ROOT.rglob('*')) if p.is_file() and p.suffix in ('.py', '.java')},
        'provider_classes': {n: hashlib.sha256(b).hexdigest() for n, b in fresh.items()},
        'loader_audit': json.loads((work / 'loader-audit.json').read_text()),
        'unchanged_archive_entries': len(unchanged), 'unchanged_compressed_records': retained,
        'compile_stubs_excluded': True, 'patch_discovered_once': discovery,
        'artifacts': {name: {'sha256': z.sha(args.out / name), 'size': (args.out / name).stat().st_size}
                      for name in (SINGLE, BUNDLE)},
    }
    (args.out / 'QA_build375.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
