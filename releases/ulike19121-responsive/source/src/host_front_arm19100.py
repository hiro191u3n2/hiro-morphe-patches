#!/usr/bin/env python3
"""Retain every .97 AArch64 strip-boundary case on the optimized .100 JNI.
The separate front19100 runner proves raw-bit/full-output identity against .99.
"""
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
    tracked = [production, fixture, source / 'host_front_arm19100.py', source / 'native_identity1997_baseline.json', source / 'tests19100/front/reference99/single_noise1955.c']
    sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    before = {p.relative_to(source).as_posix(): sha(p) for p in tracked}
    # The historical assertion required an immutable .96 production kernel.
    # .100 intentionally optimizes it; keep its 128 numerical boundary cases
    # verbatim, and independently retain the exact published kernel as oracle.
    reference = source / 'tests19100/front/reference99/single_noise1955.c'
    if sha(reference) != '9ce27f6b990a68c32c42e936d9bcdeda9bfdbeebf7be48d2f57b64c3d39d1938':
        raise AssertionError('Published .99 Single oracle changed')
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
    report.update(source_sha256=before, baseline_version='current-kernel-256-row',retained97_cases_on_optimized_kernel=True, compiler=cross, qemu=qemu,
                  compiler_version=compiler_version, qemu_version=qemu_version,
                  flags=flags, binary_sha256=sha(binary), scope='Actual AArch64/NEON production JNI, old 256-row split versus every 16..256 row split; separate .99 bitwise differential required; no device timing claim')
    (work / 'result.json').write_text(json.dumps(report, indent=2) + '\n')
    return report
