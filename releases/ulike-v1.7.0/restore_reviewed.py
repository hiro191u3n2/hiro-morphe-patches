#!/usr/bin/env python3
"""Restore only the hash-bound, reviewed text sources. Never execute source deltas."""
import base64, hashlib, json, lzma, zipfile
from pathlib import Path, PurePosixPath

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
PAYLOAD_SHA = '9573ce4cbc3b2165a21d9a8b2c712a4bc2fd5ef45d80a3da169e0f378f0d8a98'
BASE_SHA = 'e0ec03bf1246e34d09740e51bd4dd2e580ea3d0f120ee5ca047b14b59749c7a7'
BANNED = {'.apk', '.apks', '.aab', '.so', '.keystore', '.jks', '.p12', '.jar', '.dex', '.mpp'}
def sha(data):
    return hashlib.sha256(data).hexdigest()
def safe(name):
    p = PurePosixPath(name)
    assert name and not p.is_absolute() and '..' not in p.parts and '\\' not in name
    assert str(p) == name and p.suffix.lower() not in BANNED
    return p

def main():
    parts = []
    for i, size in enumerate([8000, 8000, 8000, 8000, 4328]):
        part = (HERE / ('source.b64.%02d' % i)).read_text(encoding='ascii').strip()
        assert len(part) == size, 'Source chunk length mismatch: ' + str(i)
        parts.append(part)
    compressed = base64.b64decode(''.join(parts), validate=True)
    assert sha(compressed) == PAYLOAD_SHA, 'Source payload SHA mismatch'
    decoder = lzma.LZMADecompressor(memlimit=128 * 1024 * 1024)
    raw = decoder.decompress(compressed, max_length=2 * 1024 * 1024)
    assert decoder.eof and not decoder.unused_data
    packet = json.loads(raw)
    assert packet['schema'] == 1 and packet['base_sha256'] == BASE_SHA
    original = ROOT / 'input/ULike_v1.6.9_sources_and_QA.zip'
    assert sha(original.read_bytes()) == BASE_SHA, 'Source predecessor SHA mismatch'
    restored = {}
    with zipfile.ZipFile(original) as z:
        assert z.testzip() is None
        for name, spec in packet['files'].items():
            safe(name)
            if 'text' in spec:
                assert set(spec) == {'text'}
                text = spec['text']
            else:
                assert set(spec) <= {'base', 'edits'}
                safe(spec['base'])
                lines = z.read(spec['base']).decode('utf-8').splitlines(keepends=True)
                previous = 0
                for start, end, replacement in spec.get('edits', []):
                    assert previous <= start <= end <= len(lines) and isinstance(replacement, str)
                    previous = end
                for start, end, replacement in reversed(spec.get('edits', [])):
                    lines[start:end] = [replacement]
                text = ''.join(lines)
            assert isinstance(text, str)
            restored[name] = text.encode('utf-8')
    manifest = json.loads(restored['SOURCE_FILES.json'])
    assert len(manifest) == 85 and set(restored) == set(manifest) | {'SOURCE_FILES.json'}
    for name, expected in manifest.items():
        assert sha(restored[name]) == expected, 'Restored source SHA mismatch: ' + name
    destination = ROOT / 'source170'
    assert not destination.exists(), 'Refusing to overwrite existing source directory'
    destination.mkdir()
    for name, data in restored.items():
        path = destination.joinpath(*safe(name).parts)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    print('PASS: 86 reviewed text files restored and SHA-256 verified; no original APKs or keys.')
if __name__ == '__main__':
    main()
