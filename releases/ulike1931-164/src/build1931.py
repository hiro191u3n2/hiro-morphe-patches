#!/usr/bin/env python3
"""Build the front-camera restart repair; preserve the reviewed image pipeline and every other app."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
PINS = {
    'ULike_HQ_Texture_Online_v1.9.30.mpp': '8b7de29e47362d8e2291b5c2d506dd85688279443269ee8d854513d4cae5d723',
    'Hiro_Morphe_Patches_v1.0.163.mpp': 'd8fa1b78392803316a73a65c791fc5dd5e56e50a45884dbaf05097190230f1b4',
    'morphe.jar': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'd8.jar': '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
}
SINGLE = 'ULike_HQ_Texture_Online_v1.9.31.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.164.mpp'



def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def run(command, log):
    command = list(map(str, command))
    if command[0] == 'java':
        command.insert(1, '-XX:ActiveProcessorCount=1')
    with Path(log).open('wb') as stream:
        result = subprocess.run(command, stdout=stream,
                                stderr=subprocess.STDOUT, timeout=900)
    output = Path(log).read_text(errors='replace')
    if result.returncode:
        print(output[-12000:])
        raise RuntimeError('Command failed: ' + str(command[0]) + '; log=' + str(log))
    return output


def archive(path):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None, 'Corrupt ZIP')
        require(len(z.namelist()) == len(set(z.namelist())), 'Duplicate ZIP entries')
        for name in z.namelist():
            require(not Path(name).is_absolute() and '..' not in Path(name).parts, 'Unsafe ZIP entry')
        return {n: z.read(n) for n in z.namelist() if not n.endswith('/')}


def write_zip(path, entries):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for name in sorted(entries, key=lambda n: (n != 'META-INF/MANIFEST.MF', n)):
            info = zipfile.ZipInfo(name, (2026, 10, 7, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, entries[name], compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)


def headers(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '):
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    return dict(line.decode().split(': ', 1) for line in rows)


def manifest(fields):
    lines = []
    for key, value in fields.items():
        require(not any(c in value for c in '\r\n\0'), 'Invalid manifest')
        raw = (key + ': ' + value).encode()
        prefix = b''
        while len(prefix) + len(raw) > 72:
            cut = 72 - len(prefix)
            while raw[cut] & 192 == 128:
                cut -= 1
            lines.append(prefix + raw[:cut])
            raw, prefix = raw[cut:], b' '
        lines.append(prefix + raw)
    data = b'\r\n'.join(lines) + b'\r\n\r\n'
    require(headers(data) == fields, 'Manifest roundtrip')
    return data


def own(name):
    return name.startswith(('ulike/', 'ulike181/', 'ulike182/', 'ulike186/', 'ulike191/', 'app/hiro/ulike/patches/'))


def build(args):
    require(not args.work.exists(), 'Use a fresh work directory')
    args.work.mkdir(parents=True)
    args.output.mkdir(parents=True, exist_ok=True)
    for name, expected in PINS.items():
        parent = args.tools if name.endswith('.jar') else args.input
        require(sha((parent / name).read_bytes()) == expected, 'Pinned input mismatch: ' + name)
    require(sha((ROOT / 'stock-seed1931.dex').read_bytes()) == '5a3d209986efa6eb99d90ba90f65023023f9e981ae3e8a3328aee55f5c2d4f0a', 'Original cancellation seed changed')
    base = archive(args.input / 'ULike_HQ_Texture_Online_v1.9.30.mpp')
    bundle = archive(args.input / 'Hiro_Morphe_Patches_v1.0.163.mpp')
    require({n: b for n, b in base.items() if own(n)} ==
            {n: b for n, b in bundle.items() if own(n)}, 'Single and active bundle ULike baseline differ')
    baseline = args.work / 'baseline'
    for name, data in base.items():
        target = baseline / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    (args.work / 'bundle163.dex').write_bytes(bundle['classes.dex'])
    (baseline / 'bundle.dex').write_bytes(bundle['classes.dex'])
    classes, helper_classes, helper_dex = [args.work / n for n in ('classes', 'helper-classes', 'helper-dex')]
    for folder in (classes, helper_classes, helper_dex):
        folder.mkdir()
    run(['javac', '--release', '8', '-encoding', 'UTF-8', '-cp', args.tools / 'android.jar',
         '-d', helper_classes, *sorted((ROOT / 'front-stubs').rglob('*.java')), ROOT / 'FrontPreview1931.java'],
        args.work / 'helper-javac.log')
    selected = sorted((helper_classes / 'com/hiro/ulike').glob('FrontPreview1931*.class'))
    require(bool(selected), 'Missing production helper classes')
    run(['java', '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26',
         '--lib', args.tools / 'android.jar', '--classpath', helper_classes,
         '--output', helper_dex, *selected], args.work / 'd8.log')
    cp = os.pathsep.join(map(str, (args.tools / 'morphe.jar', baseline, classes)))
    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes,
         *[ROOT / (name + '.java') for name in ('MergePayloads', 'Transform1931', 'VerifyFront1931', 'VerifyHelperReferences')]],
        args.work / 'transform-javac.log')
    run(['javac', '-encoding', 'UTF-8', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED',
         '-cp', cp, '-d', classes, ROOT / 'PatchClass1931.java'], args.work / 'metadata-javac.log')
    for name in ('emitted', 'repeat'):
        run(['java', '-cp', cp, 'Transform1931', baseline, helper_dex / 'classes.dex',
             args.work / 'bundle163.dex', ROOT / 'stock-seed1931.dex', args.work / name, args.work / (name + '-audit.tsv')],
            args.work / (name + '.log'))
    emitted = args.work / 'emitted'
    require({p.name: p.read_bytes() for p in emitted.iterdir()} ==
            {p.name: p.read_bytes() for p in (args.work / 'repeat').iterdir()}, 'DEX build not deterministic')
    run(['java', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED', '-cp', cp,
         'PatchClass1931', baseline / 'app/hiro/ulike/patches/UlikeHqMaxPatch.class',
         emitted / 'UlikeHqMaxPatch.class'], args.work / 'metadata.log')
    verification = run(['java', '-cp', cp, 'VerifyFront1931', baseline, emitted], args.work / 'front-dex-verification.txt')
    run(['java', '-cp', cp, 'VerifyHelperReferences', baseline / 'ulike/methods.dex', baseline / 'ulike/runtime.dex',
         emitted / 'methods.dex', emitted / 'runtime.dex'], args.work / 'helper-references.txt')
    from host_front1931 import test as host_front
    host_count = host_front(ROOT, args.work)
    inventory = json.loads((emitted / 'front-inventory.json').read_text())
    final = dict(base)
    final.update({'classes.dex': (emitted / 'loader.dex').read_bytes(),
                  'ulike/runtime.dex': (emitted / 'runtime.dex').read_bytes(),
                  'ulike/methods.dex': (emitted / 'methods.dex').read_bytes(),
                  'ulike/methods.tsv': (emitted / 'methods.tsv').read_bytes(),
                  'app/hiro/ulike/patches/UlikeHqMaxPatch.class': (emitted / 'UlikeHqMaxPatch.class').read_bytes()})
    mf = headers(base['META-INF/MANIFEST.MF'])
    mf.update(Version='1.9.31', Timestamp='2026-10-07T00:00:00',
              Description='Repair front preview startup ordering and released input textures. Cancel stale starts on stop, switch, close or background. Image pipeline retained. Device untested.')
    final['META-INF/MANIFEST.MF'] = manifest(mf)
    integrated = dict(bundle)
    integrated.update({n: b for n, b in final.items() if own(n)})
    integrated['classes.dex'] = (emitted / 'bundle-loader.dex').read_bytes()
    mf = headers(bundle['META-INF/MANIFEST.MF'])
    mf.update(Version='1.0.164', Timestamp='2026-10-07T00:00:00',
              Description='ULike1.9.31 front preview restart repair. All other patches retained from1.0.163. Image pipeline retained. Device untested.')
    integrated['META-INF/MANIFEST.MF'] = manifest(mf)
    changes = sorted(n for n in final if final[n] != base[n])
    require(set(final) == set(base), 'Unexpected standalone resource entries')
    require(set(changes) == {'classes.dex', 'ulike/runtime.dex', 'ulike/methods.dex', 'ulike/methods.tsv', 'META-INF/MANIFEST.MF', 'app/hiro/ulike/patches/UlikeHqMaxPatch.class'},
            'Unexpected standalone resource changes')
    require(all(integrated[n] == b for n, b in bundle.items() if not own(n) and n not in ('classes.dex', 'META-INF/MANIFEST.MF')),
            'Non-ULike resource changed')
    require(set(integrated) == set(bundle), 'Unexpected integrated resource entries')
    artifacts = {}
    for name, entries in ((SINGLE, final), (BUNDLE, integrated)):
        path = args.output / name
        write_zip(path, entries)
        repeat = args.work / (name + '.repeat')
        write_zip(repeat, entries)
        require(path.read_bytes() == repeat.read_bytes(), 'MPP not deterministic')
        require(archive(path) == entries, 'Emitted ZIP mismatch')
        run(['java', '-jar', args.tools / 'morphe.jar', 'list-patches', '--patches', path], args.work / (name + '.loader.log'))
        artifacts[name] = {'sha256': sha(path.read_bytes()), 'bytes': path.stat().st_size}
    audit = (args.work / 'emitted-audit.tsv').read_text()
    kept_match = re.search(r'existing_runtime_methods_retained=(\d+)', audit)
    loader_match = re.search(r'other_app_loaders_retained=(\d+)', audit)
    require(kept_match and loader_match, 'Missing preservation counts')
    qa = {
        'schema': 'ulike1931-front-preview-v1', 'status': 'BUILT_HOST_VERIFIED_DEVICE_UNVERIFIED',
        'ulike_version': '1.9.31', 'bundle_version': '1.0.164', 'base_versions': ['1.9.30', '1.0.163'],
        'approved_lineage': '1.8.8', 'device_tested': False, 'device_fix_confirmed': False,
        'original_apk_apply_tested': False, 'ci_android_apply_tested': False,
        'non_ulike_resources_byte_identical': True, 'non_ulike_loader_classes_unchanged': True,
        'image_pipeline_byte_identical': True, 'rear_preview_retry_logic_unchanged': True,
        'front_preview_retry': True, 'front_released_texture_refresh': True,
        'front_deferred_surface_wait': True, 'remember_last_camera': True,
        'black_bar_gesture_byte_identical': True, 'rear_lens_ui_predraw_guard': True,
        'rear_lens_ui_selection_guard': True, 'standalone_and_bundle_ulike_resources_identical': True,
        'provider_lifecycle_byte_identical': True,
        'native_methods_byte_identical': False, 'native_methods_tsv_byte_identical': False,
        'changed_runtime_methods': inventory['changed_runtime_methods'],
        'changed_native_methods': inventory['changed_native_methods'],
        'new_native_hooks': inventory['new_native_hooks'],
        'new_helper_classes': inventory['new_helper_classes'],
        'existing_runtime_methods_unchanged': int(kept_match[1]),
        'non_ulike_loader_classes_preserved': int(loader_match[1]),
        'host_helper_assertions': host_count, 'dex_verification': verification.strip(),
        'changed_standalone_entries': changes, 'artifacts': artifacts, 'input_sha256': PINS,
        'native_seed_sha256': sha((ROOT / 'stock-seed1931.dex').read_bytes()),
        'published': False, 'manager_feed_updated': False,
        'limitations': ['No Android/Galaxy/ART or camera hardware execution.',
                        'Native-semantics host regressions and DEX verification do not prove device symptom resolution.'],
    }
    (args.output / 'QA_ULike_v1.9.31.json').write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    for name in ('emitted-audit.tsv', 'front-dex-verification.txt', 'helper-references.txt', 'host-front1931.txt'):
        shutil.copyfile(args.work / name, args.output / name)
    print(json.dumps({'status': qa['status'], 'host_helper_assertions': host_count,
                      'runtime_methods_unchanged': qa['existing_runtime_methods_unchanged'], 'artifacts': artifacts}, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('input', 'tools', 'work', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    options = parser.parse_args()
    if options.jdk:
        os.environ['PATH'] = str(options.jdk.resolve() / 'bin') + os.pathsep + os.environ.get('PATH', '')
    for key, value in vars(options).items():
        if value is not None: setattr(options, key, value.resolve())
    build(options)
