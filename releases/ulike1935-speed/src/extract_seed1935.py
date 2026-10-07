#!/usr/bin/env python3
"""Extract only reviewed native-method seeds from the pinned original ULike APKS."""
from pathlib import Path
import argparse
import json
import os
import zipfile
from build1935 import ROOT, TOOL_PINS, BASE_SINGLE_SHA256, archive, require, run, sha, verify_jdk

ORIGINAL_SHA256 = '73c6d3a3008b9975645f63238f07dc9c1960ad982c70f141ee4b5dfe60f7a293'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('original', 'baseline', 'tools', 'output', 'hashes', 'work'):
        parser.add_argument('--' + name, required=True, type=Path)
    parser.add_argument('--jdk', type=Path)
    args = parser.parse_args()
    for key, value in vars(args).items():
        if value is not None:
            setattr(args, key, value.resolve())
    if args.jdk:
        os.environ['PATH'] = str(args.jdk / 'bin') + os.pathsep + os.environ.get('PATH', '')
    verify_jdk()
    require(sha(args.original.read_bytes()) == ORIGINAL_SHA256, 'Original ULike APKS checksum differs')
    require(sha(args.baseline.read_bytes()) == BASE_SINGLE_SHA256, 'Approved ULike baseline checksum differs')
    for filename, digest in TOOL_PINS.items():
        require(sha((args.tools / filename).read_bytes()) == digest, 'Toolchain checksum differs: ' + filename)
    require(args.output.suffix == '.dex' and args.hashes.suffix == '.tsv', 'Seed output types differ')
    require(args.output != args.hashes and args.original not in (args.output, args.hashes), 'Conflicting output paths')
    require(not args.work.exists(), 'Use a fresh seed extraction work directory')
    args.work.mkdir(parents=True)
    classes = args.work / 'classes'
    classes.mkdir()
    stock = args.work / 'original-base.apk'
    with zipfile.ZipFile(args.original) as source_archive:
        require(source_archive.namelist().count('base.apk') == 1, 'Expected exactly one original base APK')
        stock.write_bytes(source_archive.read('base.apk'))
    baseline = args.work / 'baseline'
    for name, data in archive(args.baseline).items():
        if name.startswith('app/hiro/ulike/patches/') and name.endswith('.class'):
            target = baseline / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
    require((baseline / 'app/hiro/ulike/patches/MethodContract.class').is_file(),
            'Missing approved method-contract implementation')
    cp = os.pathsep.join(map(str, (args.tools / 'morphe.jar', baseline, classes)))
    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes,
         ROOT / 'MergePayloads.java', ROOT / 'SeedSave1935.java'], args.work / 'seed-javac.log')
    outputs = []
    for name in ('first', 'repeat'):
        dex, hashes = args.work / (name + '.dex'), args.work / (name + '-hashes.tsv')
        run(['java', '-Xmx2g', '-cp', cp, 'SeedSave1935', stock, dex, hashes],
            args.work / (name + '.log'))
        data, listing = dex.read_bytes(), hashes.read_bytes()
        require(len(data) >= 112 and data[:4] == b'dex\n', 'Invalid native-method seed DEX')
        require(listing.strip(), 'Missing original native-method inventory')
        outputs.append((data, listing))
    require(outputs[0] == outputs[1], 'Original method extraction is not deterministic')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.hashes.parent.mkdir(parents=True, exist_ok=True)
    # Existing reviewed seed files may only be confirmed byte-for-byte. A revised
    # seed requires explicit preparation of new output paths and review of pins.
    for target, data in ((args.output, outputs[0][0]), (args.hashes, outputs[0][1])):
        require(not target.exists() or target.read_bytes() == data, 'Existing seed differs: ' + str(target))
        target.write_bytes(data)
    print(json.dumps({'status': 'seed_extracted_not_yet_publication_reviewed',
                      'original_apks_sha256': ORIGINAL_SHA256,
                      'baseline_ulike_sha256': BASE_SINGLE_SHA256,
                      'seed_sha256': sha(outputs[0][0]), 'seed_bytes': len(outputs[0][0]),
                      'hashes_sha256': sha(outputs[0][1]),
                      'hashes_bytes': len(outputs[0][1]), 'published': False}, indent=2))


if __name__ == '__main__':
    main()
