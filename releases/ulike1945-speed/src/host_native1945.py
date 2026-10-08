#!/usr/bin/env python3
"""Execute exact P10/P7 native kernels on host and emulated AArch64.

The ARM64 kernel object is compiled by the same pinned NDK compiler/flags as
the Android shared library, then linked to a Linux test harness for qemu-user.
This is instruction/algorithm validation, not a Galaxy performance benchmark.
"""
import argparse
import hashlib
import json
import os
import pathlib
import re
import subprocess
import sys


def run(args, env=None):
    result = subprocess.run([str(v) for v in args], text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            env=env)
    if result.returncode:
        raise RuntimeError(result.stdout)
    return result.stdout


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def canonical_disassembly(disassembly, obj):
    header = str(obj) + ':\tfile format elf64-littleaarch64'
    if disassembly.splitlines().count(header) != 1:
        raise RuntimeError('Unexpected llvm-objdump object header')
    return disassembly.replace(header, obj.name + ':\tfile format elf64-littleaarch64', 1), header


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--jdk', required=True)
    parser.add_argument('--ndk', required=True)
    parser.add_argument('--cross-gcc', default='aarch64-linux-gnu-gcc')
    parser.add_argument('--qemu', default='qemu-aarch64')
    parser.add_argument('--cross-library-path')
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    src = pathlib.Path(__file__).resolve().parent
    native = src / 'native1944'
    output = pathlib.Path(args.output).resolve()
    output.mkdir(parents=True, exist_ok=True)
    ndk = pathlib.Path(args.ndk).resolve()
    if 'Pkg.Revision = 27.2.12479018' not in (ndk / 'source.properties').read_text():
        raise RuntimeError('NDK r27c is required')
    bindir = ndk / 'toolchains/llvm/prebuilt/linux-x86_64/bin'
    clang = bindir / 'aarch64-linux-android26-clang'
    flags = ['-std=c11', '-O3', '-Wall', '-Wextra', '-Werror']
    kernel = native / 'residual1944.c'
    harness = native / 'test_residual1945.c'
    host = output / 'test-residual1945-host'
    run(['cc', *flags, kernel, harness, '-o', host])
    scalar = json.loads(run([host]))
    if not scalar.get('passed') or scalar.get('neon'):
        raise RuntimeError('Host scalar oracle failed')
    sanitizer = output / 'test-residual1945-sanitizers'
    run(['cc', '-std=c11', '-O1', '-g', '-Wall', '-Wextra', '-Werror',
         '-fsanitize=address,undefined', '-fno-omit-frame-pointer', kernel,
         harness, '-o', sanitizer])
    sanitizer_env = os.environ.copy()
    # LeakSanitizer cannot inspect this container's restricted /proc tree.
    # ASan bounds and UBSan checks remain enabled; tests free every allocation.
    sanitizer_env['ASAN_OPTIONS'] = 'detect_leaks=0'
    sanitized = json.loads(run([sanitizer], env=sanitizer_env))
    ndk_flags = ['-std=c11', '-O3', '-fPIC', '-ffreestanding', '-fno-builtin',
                 '-fno-stack-protector', '-fvisibility=hidden', '-fno-fast-math',
                 '-ffp-contract=off', '-Werror', '-Wall', '-Wextra']
    obj = output / 'production-residual1945.o'
    run([clang, *ndk_flags, '-c', kernel, '-o', obj])
    env = os.environ.copy()
    if args.cross_library_path:
        env['LD_LIBRARY_PATH'] = args.cross_library_path + (
            ':' + env['LD_LIBRARY_PATH'] if env.get('LD_LIBRARY_PATH') else '')
    arm = output / 'test-residual1945-ndk-arm64'
    run([args.cross_gcc, *flags, '-static', obj, harness, '-o', arm], env=env)
    arm_result = json.loads(run([args.qemu, arm], env=env))
    if not arm_result.get('passed') or not arm_result.get('neon'):
        raise RuntimeError('Actual AArch64 NEON execution failed')
    for field in ['cases', 'pixels', 'exact_int_comparisons_and_checks']:
        if scalar[field] != arm_result[field] or sanitized[field] != scalar[field]:
            raise RuntimeError('Fixture counts differ: ' + field)
    jni = run([sys.executable, src / 'host_native1944.py', '--jdk', args.jdk,
               '--output', output / 'jni'])
    print(jni, end='')
    disassembly, raw_objdump_header = canonical_disassembly(
        run([bindir / 'llvm-objdump', '-d', obj]), obj)
    (output / 'production-residual1945-disassembly.txt').write_text(disassembly)
    if not any(op in disassembly for op in ['uabd', 'umla', 'mla\t']):
        raise RuntimeError('Expected integer NEON instructions absent')
    cross_banner = run([args.cross_gcc, '--version'], env=env).splitlines()[0]
    cross_version = run([args.cross_gcc, '-dumpfullversion'], env=env).strip()
    qemu_banner = run([args.qemu, '--version'], env=env).splitlines()[0]
    qemu_version = re.search(r'version ([0-9]+\.[0-9]+\.[0-9]+)', qemu_banner)
    if cross_version != '13.3.0' or qemu_version is None or qemu_version.group(1) != '8.2.2':
        raise RuntimeError('Unreviewed cross-linker or emulator semantic version')
    diagnostics = {'cross_linker_banner': cross_banner, 'qemu_banner': qemu_banner,
                   'cross_gcc_path': str(pathlib.Path(args.cross_gcc).resolve()),
                   'qemu_path': str(pathlib.Path(args.qemu).resolve()),
                   'llvm_objdump_header': raw_objdump_header}
    (output / 'native-tool-identities1945.diagnostics.json').write_text(json.dumps(diagnostics, indent=2) + '\n')
    report = {
        'schema': 'ulike-native1945-p10-p7-tests-v1', 'pass': True,
        'host_scalar': scalar, 'host_asan_ubsan': sanitized,
        'aarch64_ndk_neon_execution': arm_result,
        'actual_jni_host': jni.strip(),
        'ndk_revision': '27.2.12479018',
        'ndk_compiler': run([clang, '--version']).splitlines()[0],
        'ndk_kernel_flags': ndk_flags,
        'cross_linker': 'GCC ' + cross_version,
        'qemu': 'qemu-aarch64 ' + qemu_version.group(1),
        'production_kernel_object_sha256': digest(obj),
        'guard_pages': True, 'noncanonical_x_retained': True,
        'physical_android_tested': False, 'device_speed_measured': False,
        'sources': {p.name: digest(p) for p in [kernel, native / 'residual1944.h',
                    harness, pathlib.Path(__file__).resolve()]},
    }
    (output / 'host-native1945.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report))


if __name__ == '__main__':
    main()
