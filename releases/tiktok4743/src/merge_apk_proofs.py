#!/usr/bin/env python3
"""Merge two genuine version-specific runs only when all four exact-MPP cases pass."""
from pathlib import Path
import argparse
import importlib.util
import json

ROOT = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('publication', ROOT.parent/'publication/publish.py')
pub = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pub)
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--dist', type=Path, required=True)
args = parser.parse_args()
artifacts = {name: {'bytes': (args.dist/name).stat().st_size, 'sha256': pub.sha((args.dist/name).read_bytes())}
             for name in (pub.SINGLE, pub.COMBINED)}
cases = []
preflight = {}
for version, identity in sorted(pub.INPUTS.items()):
    part = json.loads((ROOT/'validation'/('real-apk-'+version.replace('.', '')+'.json')).read_text())
    pub.require(part.get('schema') == 'tiktok4743-real-apk-part-v1' and part.get('version') == version,
                'Missing actual version-specific run: '+version)
    pub.require(part.get('input') == identity and part.get('physical_android_tested') is False,
                'Wrong input identity or device claim: '+version)
    pub.require(part.get('patcher_sdk_sha256') == pub.TOOLS['morphe200.jar'], 'Different original-APK SDK')
    pub.require(len(part.get('cases', [])) == 2, 'Both configurations are required for '+version)
    preflight[version] = part['original_preflight']
    cases.extend(part['cases'])
proof = {
    'schema': 'tiktok4743-real-apk-v1',
    'inputs': pub.INPUTS,
    'physical_android_tested': False,
    'patcher_sdk_sha256': pub.TOOLS['morphe200.jar'],
    'original_preflight': preflight,
    'cases': cases,
    'limitations': [
        'No physical Android device interaction, rendered UI, or live service traffic was tested. Full original-APK reconstruction and actual emitted native, OneBack, privacy, inventory and Settings/Japanese hooks were audited.',
        'Required release configurations are authored Hiro native six plus OneBack, and aggregate authored eight plus external Metra Settings, on each 47.4.3 and 47.5.3 original. Arbitrary user-selected patch combinations require separate evidence.',
        'The retained 47.5.3 compatibility does not repair the twelve failing patches in the historical external Metra 0.9.0-dev.1 all-136 exploratory run. That result concerns 47.5.3, not the new 47.4.3 original.',
        'Preview thumbnails require thumbnail data supplied by TikTok for the selected video; native LIVE/photo/protected-media seek restrictions are retained.',
    ],
}
pub.validate_real_apk(proof, artifacts)
(ROOT/'validation/real-apk.json').write_bytes(pub.json_bytes(proof))
print('PASS all four real-APK cases bound to the same exact release MPP bytes')
