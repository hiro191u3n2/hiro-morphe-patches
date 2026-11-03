#!/usr/bin/env python3
"""Freeze/reconstruct complete .125 sources using exact public .124 bytes and a pinned delta.

The public deliverable remains the complete source ZIP. Only repository staging
uses a delta, avoiding thousands of duplicate historical files. Reconstruction
checks the base archive, every overlay file, deletions and the entire final tree.
"""
from pathlib import Path, PurePosixPath
import argparse
import hashlib
import io
import json
import shutil
import urllib.request
import zipfile

BASE_URL = 'https://github.com/hiro191u3n2/hiro-morphe-patches/releases/download/ulike-v1.9.124/ULike_v1.9.124_sources_and_QA.zip'
BASE_SHA = '1d883a4040eaf80ce11c8cba576758f1c0f611ec346374a2509cfbfa73125be0'
BASE_BYTES = 33784478
CONFIG = 'source-delta19125.json'
OVERLAY = 'source-overlay'


def require(value, message):
    if not value:
        raise RuntimeError(message)


def checked(name):
    p = PurePosixPath(name)
    require(name and not p.is_absolute() and '..' not in p.parts and '\\' not in name
            and str(p) == name and not name.endswith('/'), 'Unsafe source path: ' + name)
    return p


def row(raw):
    return {'bytes': len(raw), 'sha256': hashlib.sha256(raw).hexdigest()}


def base_entries(path=None):
    raw = Path(path).read_bytes() if path else urllib.request.urlopen(BASE_URL, timeout=240).read()
    require(row(raw) == {'bytes': BASE_BYTES, 'sha256': BASE_SHA}, 'Public .124 source ZIP differs')
    with zipfile.ZipFile(io.BytesIO(raw)) as archive:
        require(archive.testzip() is None, 'Base source archive CRC failure')
        names = [n for n in archive.namelist() if not n.endswith('/')]
        require(len(names) == len(set(names)), 'Duplicate base source entries')
        for n in names:
            checked(n)
        entries = {n: archive.read(n) for n in names}
    manifest = json.loads(entries['manifest.json'])
    require(manifest.get('version') == '1.9.124', 'Unexpected base source release')
    require(set(entries) == set(manifest['files']) | {'manifest.json'}, 'Base manifest inventory differs')
    require(all(row(entries[n]) == pin for n, pin in manifest['files'].items()), 'Base manifest hash differs')
    return entries


def local_entries(source):
    entries = {}
    for p in sorted(Path(source).rglob('*')):
        if not p.is_file() or '__pycache__' in p.parts or '.git' in p.parts or p.suffix in ('.pyc', '.pyo'):
            continue
        require(not p.is_symlink(), 'Source symlinks forbidden')
        name = p.relative_to(source).as_posix()
        checked(name)
        require(p.suffix.lower() not in ('.apk', '.apks', '.apkm', '.jar'), 'Private app/tool in source: ' + name)
        entries[name] = p.read_bytes()
    return entries


def create(source, release, base=None):
    source, release = Path(source).resolve(), Path(release).resolve()
    require(source.is_dir(), 'Complete reviewed source tree required')
    before, final = base_entries(base), local_entries(source)
    manifest = json.loads(final['manifest.json'])
    require(manifest.get('version') == '1.9.125' and
            set(final) == set(manifest['files']) | {'manifest.json'}, 'Current source manifest inventory differs')
    require(all(row(final[n]) == pin for n, pin in manifest['files'].items()), 'Current source manifest hashes differ')
    overlay = {n: raw for n, raw in final.items() if before.get(n) != raw}
    removed = sorted(set(before) - set(final))
    release.mkdir(parents=True, exist_ok=True)
    destination = release / OVERLAY
    require(not destination.exists() and not (release / CONFIG).exists(), 'Fresh delta staging paths required')
    destination.mkdir()
    for name, raw in overlay.items():
        p = destination / name
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_bytes(raw)
    config = {'schema': 'ulike19125-source-delta-v1', 'version': '1.9.125',
              'base': {'url': BASE_URL, 'bytes': BASE_BYTES, 'sha256': BASE_SHA},
              'overlay': {n: row(raw) for n, raw in overlay.items()}, 'removed': removed,
              'final_files': {n: row(raw) for n, raw in final.items()}}
    (release / CONFIG).write_text(json.dumps(config, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
    require({n: raw for n, raw in before.items() if n not in removed} | overlay == final,
            'Frozen delta does not reproduce the complete reviewed source tree')
    print(json.dumps({'status': 'delta_frozen_no_remote_writes', 'base_files': len(before),
                      'overlay_files': len(overlay), 'removed_files': len(removed),
                      'final_files': len(final)}, ensure_ascii=False))
    return config


def hydrate(release, destination, base=None):
    release, destination = Path(release).resolve(), Path(destination).resolve()
    require(not destination.exists(), 'Fresh reconstructed-source directory required')
    config = json.loads((release / CONFIG).read_text())
    require(config.get('schema') == 'ulike19125-source-delta-v1' and config.get('version') == '1.9.125',
            'Unknown source delta schema/version')
    require(config.get('base') == {'url': BASE_URL, 'bytes': BASE_BYTES, 'sha256': BASE_SHA},
            'Unreviewed source base')
    entries = base_entries(base)
    removed = config['removed']
    require(isinstance(removed, list) and len(removed) == len(set(removed)), 'Duplicate deletion')
    require(not set(removed) & set(config['overlay']), 'Deletion overlaps overlay')
    for name in removed:
        checked(name)
        require(name in entries, 'Deleting absent base entry')
        del entries[name]
    actual_overlay = local_entries(release / OVERLAY)
    require(set(actual_overlay) == set(config['overlay']), 'Unexpected/missing overlay file')
    for name, pin in config['overlay'].items():
        checked(name)
        require(row(actual_overlay[name]) == pin, 'Overlay hash differs: ' + name)
        entries[name] = actual_overlay[name]
    for name in config['final_files']:
        checked(name)
    require({n: row(raw) for n, raw in entries.items()} == config['final_files'], 'Complete final source closure differs')
    destination.mkdir(parents=True)
    for name, raw in entries.items():
        p = destination / name
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_bytes(raw)
    require({n: row(raw) for n, raw in local_entries(destination).items()} == config['final_files'],
            'Written reconstructed-source closure differs')
    print(json.dumps({'status': 'complete_source_reconstructed', 'files': len(entries),
                      'overlay_files': len(actual_overlay)}))
    return destination


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=('create', 'hydrate'))
    parser.add_argument('--release', type=Path, required=True)
    parser.add_argument('--source', type=Path)
    parser.add_argument('--destination', type=Path)
    parser.add_argument('--base', type=Path)
    args = parser.parse_args()
    if args.action == 'create':
        require(args.source is not None, '--source required for create')
        create(args.source, args.release, args.base)
    else:
        require(args.destination is not None, '--destination required for hydrate')
        hydrate(args.release, args.destination, args.base)
