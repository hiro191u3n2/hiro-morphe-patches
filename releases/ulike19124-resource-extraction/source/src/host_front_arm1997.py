#!/usr/bin/env python3
"""Actual AArch64 strip-boundary differential of the unchanged Single JNI."""
from pathlib import Path
import hashlib
import json
import os
import shutil
import subprocess


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    cross, qemu = shutil.which('aarch64-linux-gnu-gcc'), shutil.which('qemu-aarch64')
    if not cross or not qemu:
        raise AssertionError('AArch64 compiler and qemu-aarch64 required')
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or os.environ['JAVA_HOME']).resolve()
    include = jdk / 'include'
    if not (include / 'jni.h').is_file():
        raise AssertionError('JNI headers missing')
    production = source / 'native1955/single_noise1955.c'
    fixture = source / 'tests1997/front/FrontArm1997.c'
    tracked = [production, fixture, source / 'host_front_arm1997.py', source / 'native_identity1997_baseline.json']
    sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    before = {p.relative_to(source).as_posix(): sha(p) for p in tracked}
    baseline = json.loads((source / 'native_identity1997_baseline.json').read_text())
    if before['native1955/single_noise1955.c'] != baseline['native_source_sha256']['native1955/single_noise1955.c']:
        raise AssertionError('Strip proof requires exact immutable .96 Single kernel')
    binary = work / 'front-arm1997'
    flags = ['-O3', '-std=c11', '-Wall', '-Wextra', '-Werror', '-ffp-contract=off', '-fno-fast-math', '-pthread', '-static']
    cmd = [cross, *flags, '-I'+str(include), '-I'+str(include/'linux'), str(fixture), '-lm', '-o', str(binary)]
    build = subprocess.run(cmd, text=True, capture_output=True, timeout=180)
    (work / 'compile.log').write_text(build.stdout + build.stderr)
    if build.returncode:
        raise AssertionError('Front ARM compilation failed: ' + build.stderr[-6000:])
    run = subprocess.run([qemu, str(binary)], text=True, capture_output=True, timeout=240)
    (work / 'execute.log').write_text(run.stdout + run.stderr)
    if run.returncode:
        raise AssertionError('Front ARM differential failed: ' + run.stdout[-3000:] + run.stderr[-6000:])
    report = json.loads(run.stdout)
    if report.get('status') != 'passed' or report.get('pixel_cases') != 128 or report.get('compared_pixels', 0) <= 0:
        raise AssertionError('Front ARM assertions missing')
    if before != {p.relative_to(source).as_posix(): sha(p) for p in tracked}:
        raise AssertionError('Front ARM compiled sources changed during execution')
    compiler_version = subprocess.run([cross, '--version'], check=True, text=True, capture_output=True, timeout=20).stdout.splitlines()[0]
    qemu_version = subprocess.run([qemu, '--version'], check=True, text=True, capture_output=True, timeout=20).stdout.splitlines()[0]
    report.update(source_sha256=before, baseline_version='1.9.96', compiler=cross, qemu=qemu,
                  compiler_version=compiler_version, qemu_version=qemu_version,
                  flags=flags, binary_sha256=sha(binary), scope='Actual AArch64/NEON production JNI, old 256-row split versus every 16..256 row split; no device timing claim')
    (work / 'result.json').write_text(json.dumps(report, indent=2) + '\n')
    return report
