#!/usr/bin/env python3
"""Reject missing helper members masked by external superclass/interface fallback.

The real member verifier reads deliberately malformed miniature DEX fixtures.
The pinned pre-hardening verifier must falsely accept the same missing members;
the current verifier must reject them while retaining six exact external APIs.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
BASE_NAME = 'ULike_HQ_Texture_Online_v1.9.80.mpp'
BASE_SHA256 = '2be00d742db6a8954a08e0dc43c27b11e1fb2d20c864d09a77b8010bb9a1ea25'
MORPHE_SHA256 = '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c'
OLD_SHA256 = 'ec744032317eedb0584d793a9d5a2705d80c72502002afa349c38c55b29871e8'


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def run(command, log):
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True, timeout=120)
    log.write_text(result.stdout + result.stderr)
    if result.returncode:
        raise AssertionError(str(log) + '\n' + (result.stdout + result.stderr)[-8000:])
    return result.stdout


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
    morphe = Path(os.environ.get('ULIKE_MORPHE_JAR') or os.environ.get('ULIKE1973_MORPHE_JAR') or jdk.parent.parent / 'tools/morphe.jar')
    baseline = Path(os.environ.get('ULIKE1973_BASELINE_MPP') or source.parent.parent / 'input' / BASE_NAME)
    if sha(morphe) != MORPHE_SHA256 or sha(baseline) != BASE_SHA256:
        raise AssertionError('Pinned Morphe and published .80 MPP required')
    checker = source / 'VerifyAllHelperReferences1980.java'
    previous = source / 'tests1980/helper-prehardening/VerifyAllHelperReferences1980.java'
    fixture = source / 'tests1980/HelperReferences1980Test.java'
    merger = source / 'MergePayloads.java'
    tracked = [checker, previous, fixture, merger, source / 'host_helper_references1981.py']
    pins = {p.relative_to(source).as_posix(): sha(p) for p in tracked}
    if sha(previous) != OLD_SHA256:
        raise AssertionError('Pre-hardening false-PASS reference changed')
    retained = work / 'retained-patch-classes'
    retained.mkdir(exist_ok=True)
    with zipfile.ZipFile(baseline) as archive:
        names = [n for n in archive.namelist() if n == 'app/hiro/ulike/patches/MethodContract.class'
                 or n.startswith('app/hiro/ulike/patches/MethodContract$') and n.endswith('.class')]
        if 'app/hiro/ulike/patches/MethodContract.class' not in names:
            raise AssertionError('Retained real MethodContract absent')
        for name in names:
            target = retained / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.read(name))
    reports = {}
    for label, validator, old in (('prehardening', previous, True), ('current', checker, False)):
        classes = work / (label + '-classes')
        classes.mkdir(exist_ok=True)
        cp = os.pathsep.join(map(str, (morphe, retained, classes)))
        run([jdk / 'bin/javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes, merger, validator, fixture],
            work / (label + '-compile.log'))
        output = run([jdk / 'bin/java', '-XX:+PerfDisableSharedMem', '-ea', '-cp', cp,
                      'HelperReferences1980Test', work / (label + '-dex'), str(old).lower()],
                     work / (label + '-run.log'))
        report = json.loads(output.strip().splitlines()[-1])
        if (report.get('status') != 'passed' or report.get('assertions', 0) < 40
                or report.get('prehardening_false_pass_reproduced') is not old
                or report.get('physical_android_tested') is not False):
            raise AssertionError('Missing executed checker negative-control evidence')
        reports[label] = report
    if pins != {p.relative_to(source).as_posix(): sha(p) for p in tracked}:
        raise AssertionError('Reference checker source changed during execution')
    result = {'status': 'passed', 'assertions': reports['current']['assertions'],
              'physical_android_tested': False, 'tests': reports,
              'helper_missing_members_rejected': True,
              'external_interface_false_pass_reproduced': True,
              'only_exact_platform_member_descriptors_deferred': True,
              'external_member_actual_superclass_verified': True,
              'included_inherited_members_preserved': True,
              'source_sha256': pins, 'morphe_sha256': MORPHE_SHA256,
              'retained_method_contract_mpp_sha256': BASE_SHA256}
    (work / 'result.json').write_text(json.dumps(result, sort_keys=True, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=ROOT)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk), sort_keys=True, indent=2))
