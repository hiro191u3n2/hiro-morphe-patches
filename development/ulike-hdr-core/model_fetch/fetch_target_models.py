#!/usr/bin/env python3
"""Reproduce the two public model downloads observed on 2026-10-01.

Read-only HTTPS requests, no application key/cookie/device identifier required.
The result is research input, not proof of the model version used by a phone.
Model binaries must remain outside the public source repository.
"""
import argparse
import hashlib
import json
from pathlib import Path
import urllib.parse
import urllib.request

ENDPOINT = 'https://i18n-ulike-api3.faceucam.com/model/api/model'
HOSTS = {'lf16-effectcdn.byteeffecttos-g.com', 'lf19-effectcdn.byteeffecttos-g.com'}
PINS = {
    'tt_baoman': {'version': '1.0', 'bytes': 972989,
        'md5': 'c9c4a3cbd789c561472cf83e9786a199',
        'sha256': 'e64f0772bb857e4d990789237c1007b62fb86020a7edcfb0c3a61a7bedc6e2c0'},
    'tt_goodlike': {'version': '1.0', 'bytes': 1015321,
        'md5': 'f4bf8f7116ecceb69f91fddab19e9345',
        'sha256': '0d60ea7e684f32628daac031cb37fd32c50bf898aa3fb62bcbc25673b5725cad'},
}


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, message, headers, newurl):
        return None


def read_bounded(url, limit):
    if urllib.parse.urlsplit(url).scheme != 'https':
        raise ValueError('HTTPS required')
    opener = urllib.request.build_opener(NoRedirect())
    with opener.open(url, timeout=30) as response:
        if response.status != 200:
            raise ValueError('HTTP status is not 200')
        data = response.read(limit + 1)
        if len(data) > limit:
            raise ValueError('Response exceeds size limit')
        return data


def validate_bytes(name, data):
    pin = PINS[name]
    if len(data) != pin['bytes'] or hashlib.md5(data).hexdigest() != pin['md5'] \
            or hashlib.sha256(data).hexdigest() != pin['sha256']:
        raise ValueError('Model differs from reviewed content: ' + name)


def download(name, directory):
    query = {'sdk_version': '14.5.0', 'device_type': 'SM-S948Q',
             'device_platform': 'android', 'status': '1', 'name': name}
    raw = read_bounded(ENDPOINT + '?' + urllib.parse.urlencode(query), 1024 * 1024)
    response = json.loads(raw)
    if response.get('status_code') != 0:
        raise ValueError('Metadata request did not succeed')
    model = response['data']
    pin = PINS[name]
    if model['name'] != name or model['version'] != pin['version'] \
            or model['file_url']['uri'] != pin['md5']:
        raise ValueError('Server model changed; review required before using it')
    urls = model['file_url']['url_list']
    if not urls:
        raise ValueError('No model URL')
    url = urls[0]
    parsed = urllib.parse.urlsplit(url)
    if parsed.scheme != 'https' or parsed.hostname not in HOSTS \
            or parsed.username or parsed.password or parsed.port not in (None, 443) \
            or parsed.path != '/obj/ies.fe.effect.alisg/' + pin['md5'] \
            or parsed.query or parsed.fragment:
        raise ValueError('Unexpected model URL')
    data = read_bounded(url, pin['bytes'])
    validate_bytes(name, data)
    directory.mkdir(parents=True, exist_ok=True)
    target = directory / (name + '.model')
    if target.exists():
        with target.open('rb') as existing:
            validate_bytes(name, existing.read(pin['bytes'] + 1))
    else:
        with target.open('xb') as stream:
            stream.write(data)
    report = {'model_name': name, **pin, 'source_url': url,
              'authentication_supplied': False, 'metadata_query': query,
              'matched_sdk_version': response.get('matched_sdk_version'),
              'device_cache_version_confirmed': False,
              'standalone_inference_verified': False}
    (directory / (name + '.acquisition.json')).write_text(
        json.dumps(report, indent=2) + '\n', encoding='utf-8')
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True,
                        help='Private research input directory outside this source tree')
    args = parser.parse_args()
    script = Path(__file__).resolve()
    # Use the complete checkout when present, otherwise the surrounding research
    # source root in the documented core/model_fetch distribution layout.
    here = next((p for p in script.parents if (p / '.git').exists()), script.parents[2])
    destination = args.output.resolve()
    if destination == here or here in destination.parents:
        parser.error('Model binaries must be kept outside the source directory')
    for name in PINS:
        report = download(name, destination)
        print(name, report['bytes'], report['sha256'])


if __name__ == '__main__':
    main()
