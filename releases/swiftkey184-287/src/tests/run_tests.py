#!/usr/bin/env python3
"""Replay decoder tests with the packaged dictionary and production runtime tests."""
import argparse
from pathlib import Path
import subprocess
import sys

def run(command):
    result = subprocess.run([str(x) for x in command], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    print(result.stdout, end='', flush=True)
    if result.returncode:
        raise SystemExit(result.returncode)
    return result.stdout

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source-dir', type=Path, required=True)
    parser.add_argument('--tools', type=Path, required=True)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--dictionary', type=Path, required=True)
    args = parser.parse_args()
    root, work = args.source_dir.resolve(), args.work.resolve()
    classes = work/'decoder_classes'
    classes.mkdir(parents=True, exist_ok=True)
    run(['java','com.sun.tools.javac.Main','--release','8','-encoding','UTF-8','-g:none','-d',classes,
         root/'src/hiro/swiftkey/JapaneseGlideDecoder.java',root/'tests/decoder/DecoderRegressionTest.java',
         root/'tests/decoder/DecoderAssetValidationTest.java',root/'tests/decoder/DecoderAspectTest.java'])
    assets = run(['java','-Xmx256m','-cp',classes,'DecoderAssetValidationTest',args.dictionary.resolve()])
    if 'PASS' not in assets:
        raise RuntimeError('Asset tests produced no success assertions')
    decoder = run(['java','-Xmx256m','-cp',classes,'DecoderRegressionTest',args.dictionary.resolve()])
    if 'PASS' not in decoder:
        raise RuntimeError('Decoder tests produced no success assertions')
    aspect = run(['java','-Xmx256m','-cp',classes,'DecoderAspectTest',args.dictionary.resolve()])
    if 'PASS' not in aspect:
        raise RuntimeError('Physical aspect tests produced no success assertions')
    runtime = run([sys.executable,root/'tests/run_runtime_tests.py','--root',root,'--tools',args.tools.resolve(),
                   '--work',work/'runtime_host'])
    if 'PASS' not in runtime:
        raise RuntimeError('Runtime tests produced no success assertions')
    print('PASS production decoder and runtime regression suites')

if __name__ == '__main__':
    main()
