#!/usr/bin/env python3
"""Prove the .105 GPU rebuild changes only source-identity inputs over .104.

All 47 native dependencies exactly match published .104, including the
previously reviewed fingerprint builder. Only one new diagnostic helper is
classified as excluded; all numerical C and shader sources remain identical.
No ELF byte ranges are exempted: an independent same-source forward build must
match the current payload, and restoring only the generated fingerprint literal
must recover the complete published .104 GPU ELF. The NR1955 ELF is inherited.
"""
from pathlib import Path
import copy
import importlib.util
import json
import re
import subprocess

from native_identity1995 import need, sha, sections
from native_identity1999 import inspect, generated_header

ROOT = Path(__file__).resolve().parent
BASELINE_FILE = 'native_identity19101_baseline.json'
BASELINE_FILE_SHA256 = '38851f43f315eda6734534b8c682730d5390bdf955747e2e4a7d68ac8a8f1c92'
BASE_SHA256 = '68ce66210a594fa0ec28068fbca1757cced3c9f1cce3e8d4c6a87102fc130cef'
BASE_BYTES = 171856
BASE_FINGERPRINT = 'd67bd025b2788831d014c0a6fb10c392569ea05e174ca22d1972bf53f41cce0b'
NR_SHA256 = '77aecb1fafdb0b13b152225f70a9e01a7cbd303c1267aa386181246e456e9f10'
NR_BYTES = 25984
NR_BASE_SHA256 = NR_SHA256
NR_BASE_BYTES = NR_BYTES
NDK_REVISION = '27.2.12479018'
MANIFEST_PATH = 'native1960/compute_dependencies1990.json'
DIAGNOSTIC_NAME = 'StrongInputs19105.java'
DIAGNOSTIC_REASON = 'Bounded post-save Strong comparison over frozen diagnostic inputs; cannot grant quality or speed admission.'
MANIFEST_DELTA = {'excluded_runtime_java': {DIAGNOSTIC_NAME: DIAGNOSTIC_REASON}}
BUILDER_PATH = 'native1960/build_native1960.py'
REVIEWED_BUILDER_SHA256 = '0320b7f96a7a9ea5c58c7e487e64c1d5811b7c292df7e6383bb9e5349b962c23'
FLAGS = ['-std=c11', '-O3', '-shared', '-fPIC', '-fvisibility=hidden',
         '-fno-fast-math', '-ffp-contract=off', '-Wl,--no-undefined',
         '-Wl,--build-id=sha1', '-Wl,-z,max-page-size=16384',
         '-Wl,-soname,libulike_gpu1960.so', '-Wall', '-Wextra', '-Werror',
         '-Wno-misleading-indentation']


def strict_json(raw):
    """Reject duplicate JSON keys instead of silently replacing classifications."""
    def pairs(items):
        result = {}
        for key, value in items:
            need(key not in result, 'Duplicate native proof JSON key: ' + key)
            result[key] = value
        return result
    return json.loads(raw, object_pairs_hook=pairs)


def exact_equal(value, expected):
    """Proof booleans and byte counts cannot be replaced by numeric lookalikes."""
    if type(value) is not type(expected):
        return False
    if isinstance(expected, dict):
        return value.keys() == expected.keys() and all(exact_equal(value[key], item)
                                                       for key, item in expected.items())
    if isinstance(expected, list):
        return len(value) == len(expected) and all(exact_equal(left, right)
                                                   for left, right in zip(value, expected))
    return value == expected


def verify_nr(raw):
    need(isinstance(raw, bytes) and len(raw) == NR_BYTES and sha(raw) == NR_SHA256,
         'NR1955 must equal the complete published .104 payload')
    table = sections(raw)
    need('.text' in table, 'Inherited NR1955 executable section absent')


def verify_native_sources(source=ROOT):
    source = Path(source)
    baseline_path = source / BASELINE_FILE
    need(baseline_path.is_file() and not baseline_path.is_symlink(),
         'Fixed native baseline file required')
    raw = baseline_path.read_bytes()
    need(sha(raw) == BASELINE_FILE_SHA256, 'Published .104 native baseline pins changed')
    pins = strict_json(raw)
    need(pins.get('schema') == 'ulike19101-native-source-baseline-v1'
         and pins.get('baseline_release') == 'ulike-v1.9.104', 'Native baseline schema/release differs')
    need(pins['gpu'] == {'sha256': BASE_SHA256, 'bytes': BASE_BYTES,
                         'compute_fingerprint': BASE_FINGERPRINT}, 'GPU baseline identity differs')
    need(pins['nr1955'] == {'sha256': NR_SHA256, 'bytes': NR_BYTES}, 'NR baseline identity differs')
    original_raw = pins['compute_manifest_utf8'].encode('utf-8')
    need(sha(original_raw) == pins['compute_manifest_sha256'], 'Original dependency manifest hash differs')
    original = strict_json(original_raw)
    expected = copy.deepcopy(original)
    need(DIAGNOSTIC_NAME not in expected['excluded_runtime_java'], 'Diagnostic helper already classified in baseline')
    expected['excluded_runtime_java'][DIAGNOSTIC_NAME] = DIAGNOSTIC_REASON
    manifest_path = source / MANIFEST_PATH
    need(manifest_path.is_file() and not manifest_path.is_symlink(), 'Native dependency manifest required')
    current_raw = manifest_path.read_bytes()
    current = strict_json(current_raw)
    need(current == expected, 'Unreviewed compute dependency classification delta')
    entries = current['native_dependencies']
    need(len(entries) == len(set(entries)) == len(pins['native_source_sha256']) == 47
         and set(entries) == set(pins['native_source_sha256']), 'Native source inventory differs from .104')
    actual = {}
    delta = []
    for name, digest in pins['native_source_sha256'].items():
        path = source / name
        relative = Path(name)
        need(not relative.is_absolute() and '..' not in relative.parts and '\\' not in name,
             'Unsafe baseline native source path')
        need(path.is_file() and not path.is_symlink(), 'Native source missing or symlink: ' + name)
        actual[name] = sha(path.read_bytes())
        need(actual[name] == digest, 'Native/shader/CPU source changed from .104: ' + name)
    need(actual[BUILDER_PATH] == REVIEWED_BUILDER_SHA256, 'Inherited reviewed fingerprint builder changed')
    return {'baseline_file_sha256': BASELINE_FILE_SHA256,
            'native_source_sha256': actual,
            'published_baseline_qa_sha256': pins['published_qa_sha256'],
            'inherited_native_identity_proof_sha256': sha((json.dumps(pins['inherited_native_identity_proof'],sort_keys=True,separators=(',',':'))+'\n').encode()),
            'baseline_compute_manifest_sha256': pins['compute_manifest_sha256'],
            'compute_manifest_sha256': sha(current_raw),
            'reviewed_compute_manifest_delta': copy.deepcopy(MANIFEST_DELTA),
            'native_shader_and_cpu_sources_identical_to_19104': True,
            'reviewed_native_builder_sha256': REVIEWED_BUILDER_SHA256,
            'native_source_delta': delta, 'added_jni_exports': [],
            'nr1955_inherited': {'sha256': NR_SHA256, 'bytes': NR_BYTES,
                                 'byte_identical_to_19104': True}}


def current_fingerprint(source=ROOT):
    source = Path(source)
    # Verify before importing the builder so modified source cannot execute here.
    verify_native_sources(source)
    path = source / 'native1960/build_native1960.py'
    spec = importlib.util.spec_from_file_location('native_identity_compute19101', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module.compute_fingerprint1990(source / 'native1960')[0]


def verify(raw, fingerprint, report, source=ROOT, nr=None):
    inspect(raw, fingerprint)
    need(isinstance(report, dict), 'Native inverse proof must be an object')
    need(report.get('schema') == 'ulike19101-native-fingerprint-inverse-build-v1'
         and report.get('status') == 'passed', 'Native inverse proof absent')
    need(report.get('current_sha256') == sha(raw) and exact_equal(report.get('current_bytes'), len(raw)),
         'Native differs from exact rebuilt current payload')
    need(fingerprint == current_fingerprint(source), 'Expected native fingerprint differs from current source')
    need(report.get('compute_fingerprint') == fingerprint
         and report.get('baseline_compute_fingerprint') == BASE_FINGERPRINT, 'Native fingerprint proof differs')
    need(report.get('baseline_sha256') == report.get('inverse_rebuild_sha256') == BASE_SHA256
         and exact_equal(report.get('baseline_bytes'), BASE_BYTES), 'Baseline inverse proof differs')
    need(report.get('same_ndk_inverse_rebuild_verified') is True
         and report.get('ndk_revision') == NDK_REVISION, 'Pinned native inverse build missing')
    need(report.get('same_ndk_forward_rebuild_verified') is True
         and report.get('forward_rebuild_sha256') == sha(raw), 'Pinned current native forward rebuild missing')
    need(report.get('generated_header_delta') == ['GPU1960_SOURCE_SHA256'], 'Unreviewed generated header delta')
    need(report.get('compile_flags') == FLAGS and report.get('link_libraries') == ['EGL', 'GLESv3', 'm'],
         'Native inverse compiler/link flags differ')
    current = generated_header(source, fingerprint)
    inverse = generated_header(source, BASE_FINGERPRINT)
    need(report.get('generated_header_sha256') == sha(current.encode())
         and report.get('inverse_header_sha256') == sha(inverse.encode()), 'Generated shader header proof differs')
    for key, value in verify_native_sources(source).items():
        need(exact_equal(report.get(key), value), 'Native source proof differs: ' + key)
    if nr is not None:
        verify_nr(nr)
    return report


def build_inverse(source, ndk, native_out, work, baseline, nr_baseline=None):
    source, ndk, native_out, work = map(Path, (source, ndk, native_out, work))
    need(isinstance(baseline, bytes) and sha(baseline) == BASE_SHA256 and len(baseline) == BASE_BYTES,
         'Wrong .104 published native')
    if nr_baseline is not None:
        verify_nr(nr_baseline)
    pins = verify_native_sources(source)
    fingerprint = current_fingerprint(source)
    current = (native_out / 'libulike_gpu1960.so').read_bytes()
    inspect(current, fingerprint)
    need(not work.exists(), 'Fresh inverse native build directory required')
    work.mkdir(parents=True)
    header = (native_out / 'shader_sources1960.h').read_text()
    need(header == generated_header(source, fingerprint), 'Exact generated shader header required')
    old_literal = '#define GPU1960_SOURCE_SHA256 "' + fingerprint + '"'
    new_literal = '#define GPU1960_SOURCE_SHA256 "' + BASE_FINGERPRINT + '"'
    need(header.count(old_literal) == 1 and header.count(fingerprint) == 1,
         'Only one complete generated fingerprint literal may change')
    inverse = header.replace(old_literal, new_literal, 1)
    need(inverse == generated_header(source, BASE_FINGERPRINT), 'Shader header changed beyond fingerprint')
    need(re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',
                   (ndk / 'source.properties').read_text(), re.M), 'Pinned NDK r27c required')
    clang = ndk / 'toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang'

    def compile_header(directory, body, label):
        directory.mkdir(parents=True)
        (directory / 'shader_sources1960.h').write_text(body)
        target = directory / 'libulike_gpu1960.so'
        command = [str(value) for value in [clang, *FLAGS, '-I' + str(directory),
                   source / 'native1960/engine1960.c', source / 'native1960/residual_blocks1961.c',
                   '-lEGL', '-lGLESv3', '-lm', '-o', target]]
        process = subprocess.run(command, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        (work / (label + '-build.log')).write_text(process.stdout)
        need(process.returncode == 0, label + ' native build failed: ' + process.stdout[-4000:])
        return target.read_bytes()

    forward = compile_header(work / 'forward', header, 'forward')
    need(forward == current, 'Same-source/flags forward rebuild differs from packaged .105 ELF')
    restored = compile_header(work / 'inverse', inverse, 'inverse')
    need(restored == baseline, 'Rebuilt inverse native differs from exact published .104 ELF')
    report = {'schema': 'ulike19101-native-fingerprint-inverse-build-v1', 'status': 'passed',
              'current_sha256': sha(current), 'current_bytes': len(current),
              'baseline_sha256': BASE_SHA256, 'baseline_bytes': BASE_BYTES,
              'inverse_rebuild_sha256': sha(restored), 'compute_fingerprint': fingerprint,
              'baseline_compute_fingerprint': BASE_FINGERPRINT,
              'same_ndk_forward_rebuild_verified': True, 'forward_rebuild_sha256': sha(forward),
              'same_ndk_inverse_rebuild_verified': True, 'ndk_revision': NDK_REVISION,
              'generated_header_delta': ['GPU1960_SOURCE_SHA256'],
              'generated_header_sha256': sha(header.encode()), 'inverse_header_sha256': sha(inverse.encode()),
              'compile_flags': list(FLAGS), 'link_libraries': ['EGL', 'GLESv3', 'm'], **pins}
    verify(current, fingerprint, report, source, nr_baseline)
    (work / 'result.json').write_text(json.dumps(report, sort_keys=True, indent=2) + '\n')
    return report
