#!/usr/bin/env python3
"""Merge only the reviewed Berry BFCache addition into immutable Hiro bundle 328."""
from pathlib import Path
import argparse
import copy
import hashlib
import json
import os
import shutil
import subprocess
import tempfile
import zipfile

BASE_SHA = 'c2c5ae2773c4855aca14bbe6f232718326c2045a923d453f49c8eb2a97348f4e'
ADD_SHA = 'ec31d178533b026302ff348c4e119b4242a3527c1f20c35f15ae8fbae82eae2c'
NEW_PREFIX = 'app/hiro/berrybfcache/'
NEW_EXTENSION = 'extensions/berry_bfcache_restore.mpe'

def sha(data):
    return hashlib.sha256(data).hexdigest()

def manifest(raw):
    unfolded = raw.decode('utf-8').replace('\r\n ', '').splitlines()
    return dict(line.split(': ', 1) for line in unfolded if line)

def encode_manifest(fields):
    output = bytearray()
    for key, value in fields.items():
        part = (key + ': ' + value).encode('utf-8')
        while len(part) > 70:
            end = 70
            while part[end] & 0xc0 == 0x80:
                end -= 1
            output.extend(part[:end] + b'\r\n')
            part = b' ' + part[end:]
        output.extend(part + b'\r\n')
    output.extend(b'\r\n')
    return bytes(output)

def fixed_info(name):
    info = zipfile.ZipInfo(name, (2026, 11, 2, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    info.create_system = 3
    info.external_attr = 0o100644 << 16
    return info

def main():
    p = argparse.ArgumentParser()
    p.add_argument('--base', type=Path, required=True)
    p.add_argument('--addition', type=Path, required=True)
    p.add_argument('--java', type=Path, required=True)
    p.add_argument('--d8', type=Path, required=True)
    p.add_argument('--ecj', type=Path, required=True)
    p.add_argument('--classpath', type=Path, required=True)
    p.add_argument('--out', type=Path, required=True)
    args = p.parse_args()
    for key in ('base', 'addition', 'java', 'd8', 'ecj', 'classpath', 'out'):
        setattr(args, key, getattr(args, key).resolve())
    assert sha(args.base.read_bytes()) == BASE_SHA
    assert sha(args.addition.read_bytes()) == ADD_SHA
    args.out.mkdir(parents=True, exist_ok=True)
    stage = Path(tempfile.mkdtemp(prefix='merge_', dir=args.out))
    try:
        classes = stage / 'classes'
        dex = stage / 'dex'
        classes.mkdir(); dex.mkdir()
        with zipfile.ZipFile(args.base) as baseline, zipfile.ZipFile(args.addition) as addition:
            assert baseline.testzip() is None and addition.testzip() is None
            assert len(baseline.namelist()) == len(set(baseline.namelist()))
            assert len(addition.namelist()) == len(set(addition.namelist()))
            bm = manifest(baseline.read('META-INF/MANIFEST.MF'))
            assert bm['Version'] == '1.0.328' and bm['Name'] == 'Hiro Morphe Patches'
            expected_new = sorted(n for n in addition.namelist() if n.startswith(NEW_PREFIX) and n.endswith('.class'))
            assert len(expected_new) == 3
            additions = expected_new + [NEW_EXTENSION]
            assert not any(n in baseline.namelist() for n in additions)
            old_berry = [n for n in addition.namelist() if n not in additions + ['META-INF/MANIFEST.MF', 'classes.dex']]
            assert len(old_berry) == 46
            assert all(baseline.read(n) == addition.read(n) for n in old_berry)
            provider_entries = sorted(n for n in baseline.namelist() if n.endswith('.class')) + expected_new
            for name in provider_entries:
                dest = classes / name
                dest.parent.mkdir(parents=True, exist_ok=True)
                dest.write_bytes((addition if name in additions else baseline).read(name))
            # Existing DEX contains providers absent from the JVM entries. Do not
            # rebuild it from JVM classes; compile only the three new providers.
            cpjar = stage / 'existing-providers-classpath.jar'
            with zipfile.ZipFile(cpjar, 'w') as cp:
                for name in provider_entries:
                    if name not in additions:
                        cp.writestr(name, baseline.read(name))
            command = [str(args.java), '-cp', str(args.d8), 'com.android.tools.r8.D8', '--release', '--min-api', '26',
                       '--classpath', str(args.classpath), '--classpath', str(cpjar), '--output', str(dex),
                       *[str(classes / n) for n in expected_new]]
            proc = subprocess.run(command, capture_output=True, text=True)
            (args.out / 'd8.log').write_text(proc.stdout + proc.stderr)
            assert proc.returncode == 0, proc.stderr
            assert [p.name for p in dex.glob('*.dex')] == ['classes.dex']
            base_dex = stage / 'baseline.dex'
            base_dex.write_bytes(baseline.read('classes.dex'))
            merger_classes = stage / 'merger_classes'
            merger_classes.mkdir()
            merger_source = Path(__file__).resolve().with_name('MergeDex.java')
            compile_command = [str(args.java), '-jar', str(args.ecj), '-source', '11', '-target', '11',
                               '-encoding', 'UTF-8', '-cp', str(args.classpath), '-d', str(merger_classes), str(merger_source)]
            compiled = subprocess.run(compile_command, capture_output=True, text=True)
            assert compiled.returncode == 0, compiled.stderr
            merged_dex = stage / 'merged.dex'
            dex_verification_path = args.out / 'QA_dex_definition_preservation.json'
            merged = subprocess.run([str(args.java), '-cp', str(merger_classes) + os.pathsep + str(args.classpath),
                                     'MergeDex', str(base_dex), str(dex / 'classes.dex'), str(merged_dex), str(dex_verification_path)], capture_output=True, text=True)
            (args.out / 'merge_dex.log').write_text(merged.stdout + merged.stderr)
            assert merged.returncode == 0, merged.stderr
            dex_verification = json.loads(dex_verification_path.read_text())
            assert dex_verification['result'] == 'PASS_DEX_DEFINITION_PRESERVATION'
            bm.update({
                'Version': '1.0.329',
                'Timestamp': '2026-11-02T00:00:00',
                'Description': 'Berry BFCache restore repair v1.0.3: complete native load state and synchronize redraw after a verified cached restore. Existing four Berry patches and all other app patches are retained. Physical device rendering remains unverified.',
                'Patcher-Version': '2.2.0-dev.2',
            })
            output = args.out / 'Hiro_Morphe_Patches_v1.0.329.mpp'
            with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as result:
                for info in baseline.infolist():
                    if info.filename == 'META-INF/MANIFEST.MF':
                        data = encode_manifest(bm)
                    elif info.filename == 'classes.dex':
                        data = merged_dex.read_bytes()
                    else:
                        data = baseline.read(info.filename)
                    result.writestr(copy.copy(info), data)
                for name in additions:
                    result.writestr(fixed_info(name), addition.read(name))
            with zipfile.ZipFile(output) as result:
                assert result.testzip() is None
                changed = [n for n in baseline.namelist() if baseline.read(n) != result.read(n)]
                assert sorted(changed) == ['META-INF/MANIFEST.MF', 'classes.dex']
                assert sorted(set(result.namelist()) - set(baseline.namelist())) == sorted(additions)
                assert not set(baseline.namelist()) - set(result.namelist())
                report = {
                    'result': 'PASS_ENTRY_PRESERVATION',
                    'bundle_version': '1.0.329', 'berry_repair_version': '1.0.3',
                    'baseline_sha256': BASE_SHA, 'addition_sha256': ADD_SHA,
                    'output_sha256': sha(output.read_bytes()), 'output_bytes': output.stat().st_size,
                    'baseline_entries': len(baseline.namelist()), 'output_entries': len(result.namelist()),
                    'existing_entries_changed': changed, 'entries_added': additions, 'entries_removed': [],
                    'all_original_berry_providers_and_extensions_identical': True,
                    'all_unrelated_entries_identical': True,
                    'provider_class_count': len(provider_entries),
                    'dex_merge_strategy': 'Keep every baseline DEX class definition and add only the new provider namespace',
                    'dex_definition_preservation_verified': True,
                    'dex_definition_verification': dex_verification,
                    'd8_sha256': sha(args.d8.read_bytes()),
                    'ecj_sha256': sha(args.ecj.read_bytes()),
                    'compile_classpath_sha256': sha(args.classpath.read_bytes()),
                    'manifest': bm, 'device_tested': False,
                }
                (args.out / 'QA_bundle_merge.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
                print(json.dumps(report, ensure_ascii=False))
    finally:
        shutil.rmtree(stage)

if __name__ == '__main__':
    main()
