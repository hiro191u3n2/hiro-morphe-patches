#!/usr/bin/env python3
"""Audit the 1.8.27 English upward-flick runtime against actual APK + pinned Android API35."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--apk', required=True, type=Path)
    p.add_argument('--extension', required=True, type=Path)
    p.add_argument('--output', required=True, type=Path)
    p.add_argument('--tools', required=True, type=Path)
    p.add_argument('--source-dir', type=Path, default=Path(__file__).resolve().parent.parent)
    p.add_argument('--work', required=True, type=Path)
    a = p.parse_args()
    a.work.mkdir(parents=True, exist_ok=True)
    paths = [a.tools / 'morphe.jar', a.tools / 'r8.jar']
    pins = ['b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58', 'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa']
    for path, pin in zip(paths, pins):
        if sha(path) != pin:
            raise RuntimeError('ABI tool hash changed: ' + path.name)
    android = a.tools / 'android.jar'
    if sha(android) != '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b':
        raise RuntimeError('Pinned Android API35 jar changed')
    cp = os.pathsep.join(map(str, paths))
    source = a.source_dir / 'tools_java/AuditSensitivityAbi.java'
    subprocess.run(['java', 'com.sun.tools.javac.Main', '-g:none', '-encoding', 'UTF-8', '-cp', cp,
                    '-d', str(a.work), str(source)], check=True)
    result = subprocess.run([os.environ.get('MORPHE_JAVA', 'java'), '-Xmx1200m', '-cp', str(a.work) + os.pathsep + cp,
                             'AuditSensitivityAbi', str(a.apk), str(a.extension), str(android), str(a.output)],
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (a.work / 'audit.log').write_text(result.stdout)
    print(result.stdout, end='')
    if not a.output.is_file():
        raise RuntimeError('ABI verifier produced no report')
    report = json.loads(a.output.read_text())
    report['verifier_source_sha256'] = sha(source)
    report['wrapper_source_sha256'] = sha(Path(__file__))
    a.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    if result.returncode or report['status'] != 'PASSED':
        raise RuntimeError('Native upward-flick sensitivity ABI verification failed: ' + str(a.output))


if __name__ == '__main__':
    main()

