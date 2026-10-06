#!/usr/bin/env python3
"""Build the scoped rear-lens UI repair. Hardware execution is not simulated."""
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
    'ULike_HQ_Texture_Online_v1.9.29.mpp': '2faae3114d5663cfb924f544b7c114f910caadc5b517eb575ea38b1d89311f6c',
    'Hiro_Morphe_Patches_v1.0.158.mpp': '4d0dca101976cfdd14b48949ec7f828c11b9c0dcf2e0c01e99a379dd6898aded',
    'morphe.jar': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'd8.jar': '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
}
SINGLE = 'ULike_HQ_Texture_Online_v1.9.30.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.159.mpp'
TARGETS = [
    'Lcom/hiro/ulike/OpticalZoom;->select(I)V',
    'Lcom/hiro/ulike/OpticalZoom;->show()Z',
    'Lcom/hiro/ulike/OpticalZoomUi$Bar;->onPreDraw()Z',
]


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def run(command, log):
    with Path(log).open('wb') as stream:
        result = subprocess.run(list(map(str, command)), stdout=stream,
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
    base = archive(args.input / 'ULike_HQ_Texture_Online_v1.9.29.mpp')
    bundle = archive(args.input / 'Hiro_Morphe_Patches_v1.0.158.mpp')
    require({n: b for n, b in base.items() if own(n)} ==
            {n: b for n, b in bundle.items() if own(n)}, 'Single and active bundle ULike baseline differ')
    baseline = args.work / 'baseline'
    for name, data in base.items():
        target = baseline / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    (args.work / 'bundle158.dex').write_bytes(bundle['classes.dex'])
    (baseline / 'bundle.dex').write_bytes(bundle['classes.dex'])
    classes, helper_classes, helper_dex = [args.work / n for n in ('classes', 'helper-classes', 'helper-dex')]
    for folder in (classes, helper_classes, helper_dex):
        folder.mkdir()
    run(['javac', '--release', '8', '-encoding', 'UTF-8', '-cp', args.tools / 'android.jar',
         '-d', helper_classes, *sorted((ROOT / 'ui-stubs').rglob('*.java')), ROOT / 'RearLensUi1930.java'],
        args.work / 'helper-javac.log')
    selected = helper_classes / 'com/hiro/ulike/RearLensUi1930.class'
    require(selected.is_file(), 'Missing production helper class')
    run(['java', '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26',
         '--lib', args.tools / 'android.jar', '--classpath', helper_classes,
         '--output', helper_dex, selected], args.work / 'd8.log')
    cp = os.pathsep.join(map(str, (args.tools / 'morphe.jar', baseline, classes)))
    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes,
         *[ROOT / (name + '.java') for name in ('MergePayloads', 'Transform1930', 'VerifyUi1930', 'VerifyHelperReferences')]],
        args.work / 'transform-javac.log')
    run(['javac', '-encoding', 'UTF-8', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED',
         '-cp', cp, '-d', classes, ROOT / 'PatchClass1930.java'], args.work / 'metadata-javac.log')
    for name in ('emitted', 'repeat'):
        run(['java', '-cp', cp, 'Transform1930', baseline, helper_dex / 'classes.dex',
             args.work / 'bundle158.dex', args.work / name, args.work / (name + '-audit.tsv')],
            args.work / (name + '.log'))
    emitted = args.work / 'emitted'
    require({p.name: p.read_bytes() for p in emitted.iterdir()} ==
            {p.name: p.read_bytes() for p in (args.work / 'repeat').iterdir()}, 'DEX build not deterministic')
    run(['java', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED', '-cp', cp,
         'PatchClass1930', baseline / 'app/hiro/ulike/patches/UlikeHqMaxPatch.class',
         emitted / 'UlikeHqMaxPatch.class'], args.work / 'metadata.log')
    verification = run(['java', '-cp', cp, 'VerifyUi1930', baseline, emitted], args.work / 'ui-dex-verification.txt')
    run(['java', '-cp', cp, 'VerifyHelperReferences', baseline / 'ulike/methods.dex', baseline / 'ulike/runtime.dex',
         emitted / 'methods.dex', emitted / 'runtime.dex'], args.work / 'helper-references.txt')
    from host_ui1930 import test as host_ui
    host_count = host_ui(ROOT, args.work)
    final = dict(base)
    final.update({'classes.dex': (emitted / 'loader.dex').read_bytes(),
                  'ulike/runtime.dex': (emitted / 'runtime.dex').read_bytes(),
                  'app/hiro/ulike/patches/UlikeHqMaxPatch.class': (emitted / 'UlikeHqMaxPatch.class').read_bytes()})
    mf = headers(base['META-INF/MANIFEST.MF'])
    mf.update(Version='1.9.30', Timestamp='2026-10-07T00:00:00',
              Description='Hide rear zoom and macro controls while front camera is selected. Guard pre-draw and stale clicks. Camera/image processing unchanged. Device untested.')
    final['META-INF/MANIFEST.MF'] = manifest(mf)
    integrated = dict(bundle)
    integrated.update({n: b for n, b in final.items() if own(n)})
    integrated['classes.dex'] = (emitted / 'bundle-loader.dex').read_bytes()
    mf = headers(bundle['META-INF/MANIFEST.MF'])
    mf.update(Version='1.0.159', Timestamp='2026-10-07T00:00:00',
              Description='ULike1.9.30 front camera rear-lens UI repair. All other patches retained from1.0.158 including SwiftKey1.8.3. Device untested.')
    integrated['META-INF/MANIFEST.MF'] = manifest(mf)
    changes = sorted(n for n in final if final[n] != base[n])
    require(set(final) == set(base), 'Unexpected standalone resource entries')
    require(set(changes) == {'classes.dex', 'ulike/runtime.dex', 'META-INF/MANIFEST.MF', 'app/hiro/ulike/patches/UlikeHqMaxPatch.class'},
            'Unexpected standalone resource changes')
    require(all(integrated[n] == b for n, b in bundle.items() if not own(n) and n not in ('classes.dex', 'META-INF/MANIFEST.MF')),
            'Non-ULike resource changed')
    require(set(integrated) == set(bundle), 'Unexpected integrated resource entries')
    require(final['ulike/methods.dex'] == base['ulike/methods.dex'] and final['ulike/methods.tsv'] == base['ulike/methods.tsv'],
            'Native payload must remain byte identical')
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
        'schema': 'ulike1930-rear-lens-ui-v1', 'status': 'BUILT_HOST_VERIFIED_DEVICE_UNVERIFIED',
        'ulike_version': '1.9.30', 'bundle_version': '1.0.159', 'base_versions': ['1.9.29', '1.0.158'],
        'approved_lineage': '1.8.8', 'device_tested': False, 'device_fix_confirmed': False,
        'original_apk_apply_tested': False, 'ci_android_apply_tested': False,
        'native_methods_byte_identical': True, 'native_methods_tsv_byte_identical': True,
        'non_ulike_resources_byte_identical': True, 'non_ulike_loader_classes_unchanged': True,
        'image_pipeline_byte_identical': True, 'camera_lifecycle_byte_identical': True,
        'black_bar_gesture_byte_identical': True, 'rear_lens_ui_predraw_guard': True,
        'rear_lens_ui_selection_guard': True, 'standalone_and_bundle_ulike_resources_identical': True,
        'changed_runtime_methods': TARGETS, 'new_helper_classes': ['Lcom/hiro/ulike/RearLensUi1930;'],
        'existing_runtime_methods_unchanged': int(kept_match[1]),
        'non_ulike_loader_classes_preserved': int(loader_match[1]),
        'host_helper_assertions': host_count, 'dex_verification': verification.strip(),
        'changed_standalone_entries': changes, 'artifacts': artifacts, 'input_sha256': PINS,
        'published': False, 'manager_feed_updated': False,
        'limitations': ['No Android/Galaxy/ART or camera hardware execution.',
                        'Host helper checks and DEX control-flow checks do not prove the device symptom is resolved.',
                        'Unmodified image and camera-session code is preserved; no new image-quality claim.'],
    }
    (args.output / 'QA_ULike_v1.9.30.json').write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    for name in ('emitted-audit.tsv', 'ui-dex-verification.txt', 'helper-references.txt', 'host-ui1930.txt'):
        shutil.copyfile(args.work / name, args.output / name)
    print(json.dumps({'status': qa['status'], 'host_helper_assertions': host_count,
                      'runtime_methods_unchanged': qa['existing_runtime_methods_unchanged'], 'artifacts': artifacts}, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('input', 'tools', 'work', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    options = parser.parse_args()
    for key, value in vars(options).items():
        setattr(options, key, value.resolve())
    build(options)
