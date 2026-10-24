#!/usr/bin/env python3
"""Bind two supported-APK successes and two removed-version rejections to exact MPP bytes."""
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
pub.validate_input_identities()
version = '47.4.3'
part = json.loads((ROOT/'validation/real-apk-4743.json').read_text())
pub.require(part.get('schema') == 'tiktok4743-backfix-real-apk-part-v1' and part.get('version') == version,
            'Missing supported-version original-APK run')
pub.require(part.get('input') == pub.INPUTS[version] and part.get('physical_android_tested') is False,
            'Wrong supported input identity or device claim')
pub.require(part.get('patcher_sdk_sha256') == pub.TOOLS['morphe200.jar'], 'Different original-APK SDK')
pub.require(len(part.get('cases', [])) == 2, 'Both supported-version configurations are required')
rejection = json.loads((ROOT/'validation/rejection-apk-4753.json').read_text())
pub.validate_rejection(rejection, artifacts)
integration = json.loads((ROOT/'validation/integration-139.json').read_text())
pub.validate_integration(integration, artifacts)
proof = {
    'schema': 'tiktok4743-backfix-real-apk-v1',
    'inputs': pub.INPUTS,
    'unsupported_inputs': pub.UNSUPPORTED_INPUTS,
    'physical_android_tested': False,
    'patcher_sdk_sha256': pub.TOOLS['morphe200.jar'],
    'original_preflight': {version: part['original_preflight']},
    'cases': part['cases'],
    'rejection': rejection,
    'integration': integration,
    'limitations': [
        'No physical Android device interaction, rendered UI, or live service traffic was tested. Actual emitted native, OneBack, privacy, inventory and Settings/Japanese hooks are audited after full supported-APK reconstruction.',
        'Required detailed positive configurations are authored Hiro native six plus OneBack, and aggregate authored eight plus external Metra Settings on the signed original 47.4.3.',
        'The additional integration selects all 136 upstream entries and the three Hiro entries shown in the user screenshot. The upstream 120 selection is unknown; the exact user configuration and all 139 runtime features were not reproduced. The integration audits its complete pipeline and emitted OneBack/Settings/Japanese hooks.',
        'TikTok 47.5.3 is removed from compatibility and production admission; its two cases are forced-selection rejection tests, never positive support evidence.',
        'Preview thumbnails require thumbnail data supplied by TikTok for the selected video; native LIVE/photo/protected-media restrictions are retained.',
    ],
}
pub.validate_real_apk(proof, artifacts)
(ROOT/'validation/real-apk.json').write_bytes(pub.json_bytes(proof))
print('PASS two detailed 47.4.3 apply cases, one 139-patch integration and two forced 47.5.3 rejection cases bound to exact release MPP bytes')
