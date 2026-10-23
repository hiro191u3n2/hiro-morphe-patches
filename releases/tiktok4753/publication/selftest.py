#!/usr/bin/env python3
"""Challenge publication gates by corrupting copies of the real, successful proof."""
from pathlib import Path
import argparse, copy, importlib.util, json

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--dist', type=Path, required=True)
args = parser.parse_args(); root = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('publication', root/'publish.py')
pub = importlib.util.module_from_spec(spec); spec.loader.exec_module(pub)
qa = json.loads((args.dist/pub.QA_NAME).read_text())
pub.validate_qa(qa)
original = json.loads((pub.SOURCE_ROOT/'src/validation/real-apk.json').read_text())
pub.validate_real_apk(original, qa['artifacts'])
count = 0

def reject(label, mutate):
    global count
    proof = copy.deepcopy(original)
    mutate(proof)
    try:
        pub.validate_real_apk(proof, qa['artifacts'])
    except (RuntimeError, KeyError, ValueError, TypeError):
        count += 1
        print('PASS reject '+label)
        return
    raise AssertionError('Unsafe proof was accepted: '+label)

def case(proof, identity='standalone_native'):
    return next(c for c in proof['cases'] if c['id'] == identity)

reject('different original package', lambda p: p['input'].update(package='com.ss.android.ugc.trill'))
reject('different original version', lambda p: p['input'].update(version_name='47.4.3'))
reject('missing signature verification', lambda p: p['input'].update(signature_verified=False))
reject('wrong original SHA', lambda p: p['input'].update(sha256='0'*64))
reject('unperformed device claim', lambda p: p.update(physical_android_tested=True))
reject('missing aggregate case', lambda p: p.update(cases=[case(p)]))
reject('different tested MPP bytes', lambda p: case(p).update(artifact_sha256='0'*64))
reject('different tested MPP filename', lambda p: case(p).update(artifact=pub.COMBINED))
reject('top-level success with failed pipeline step', lambda p: (p.update(success=True), case(p).update(steps_all_success=False)))
reject('top-level success with failed patch', lambda p: (p.update(success=True), case(p).update(failed_patches=['fixture failed patch'])))
reject('unselected native patch', lambda p: case(p)['selected_patches'].pop())
reject('missing applied native patch', lambda p: case(p)['applied_patches'].pop())
reject('repeated patch substituted for another', lambda p: case(p)['applied_patches'].__setitem__(0, case(p)['applied_patches'][1]))
reject('missing privacy output audit', lambda p: case(p)['audits'].pop('privacy'))
reject('missing native output audit', lambda p: case(p)['audits'].pop('native'))
reject('wrong OneBack audit type', lambda p: case(p)['audits'].update(oneback='PASS host only'))
reject('unchanged output APK', lambda p: case(p).update(output_apk_sha256=pub.ORIGINAL_APK_SHA256))
reject('wrong upstream Settings bytes', lambda p: case(p,'aggregate_settings_ja').update(upstream_sha256='0'*64))
reject('missing Japanese native settings audit', lambda p: case(p,'aggregate_settings_ja')['audits'].pop('settings_japanese'))
reject('missing limitations', lambda p: p.update(limitations=[]))
print('PASS publication evidence rejection tests: '+str(count)+'; baseline is the real recorded proof, never synthetic success')
