#!/usr/bin/env python3
"""Run actual .87 analysis wrappers and unchanged CPU estimators against .86.

Reservation/transport peers provide controlled failures and result buffers; they
do not emulate hardware speed. The same one-read assertion must fail with the
immutable published .86 wrapper and pass with the candidate wrapper.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parent
PUBLISHED_SHA = '47b046c926c2cd9750f248598c4bc9720206842bac89a389a3d8144323d4dc95'
FLAGS = (
    'analysis87_single_source_traversal_verified',
    'analysis87_actual_cpu_floatbits_exact',
    'analysis87_reservation_fault_cancel_ownership',
    'analysis87_same_snapshot_transport_fallback',
    'analysis87_exact2_transfer_time5_preserved',
)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def reviewed_analysis86_source1987(text):
    """Invert only reviewed .87 edits for an inherited preservation contract.

    Never use this text as a runtime compilation input. Each changed block is
    pinned, all remaining bytes must reproduce the immutable published source,
    and an unreviewed candidate edit makes this inverse fail closed.
    """
    baseline_path = ROOT / 'tests1987/analysis/published86/GpuAnalysis1961.java'
    if sha(baseline_path) != PUBLISHED_SHA:
        raise AssertionError('Published analysis baseline changed')
    baseline = baseline_path.read_text()
    blocks = (
        ('    /** The CPU traversal is the only read of a cold source.',
         '    private static Snapshot spatialSnapshot(',
         '21f5f9361628787e4ffa0660dd2faef409a627ce15e0f0d9589bec5793ffcae8', True),
        ('    static SpatialNoise1934 spatial(', '    /** Direct host QA entry:',
         '71d28de9646522e7d1c12ad40fec86bf37318ca7d7d4aa43d765ac7cbe62bd2c', False),
        ('    static float[] regions1982(', '    static float[] regionsCandidate1961(',
         '7f8f0b91ceb25b1c1e827fd4b65703fc5524d43a3bba93c68e83eec2d66606ea', False),
    )
    for first, last, expected, added in blocks:
        begin = text.index(first)
        end = text.index(last, begin)
        if hashlib.sha256(text[begin:end].encode()).hexdigest() != expected:
            raise AssertionError('Unreviewed analysis block: ' + first)
        replacement = '' if added else baseline[baseline.index(first):baseline.index(last, baseline.index(first))]
        text = text[:begin] + replacement + text[end:]
    if hashlib.sha256(text.encode()).hexdigest() != PUBLISHED_SHA:
        raise AssertionError('Analysis changed outside reviewed .87 blocks')
    return text


def run(command, log, expected_failure=False):
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True, timeout=180)
    Path(log).write_text(result.stdout + result.stderr)
    if expected_failure:
        if result.returncode == 0 or 'FIRST_READ_CAPTURE_MISSING' not in result.stderr:
            raise AssertionError('Published .86 negative control did not reproduce repeated source reads: ' + str(log))
    elif result.returncode:
        raise AssertionError(str(log) + '\n' + (result.stdout + result.stderr)[-14000:])
    return result.stdout + result.stderr


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve() / 'host-analysis1987'
    if source.is_file():
        source = source.parent
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME', '/usr/lib/jvm/java-17-openjdk-amd64')).resolve()
    candidates = [Path(os.environ['ULIKE_ANDROID_JAR'])] if os.environ.get('ULIKE_ANDROID_JAR') else []
    candidates += [source.parent / 'tools/android.jar']
    jar = next((p.resolve() for p in candidates if p.is_file()), None)
    if jar is None:
        raise AssertionError('Pinned Android compiler declarations required')
    reference = source / 'tests1987/analysis/published86/GpuAnalysis1961.java'
    if sha(reference) != PUBLISHED_SHA:
        raise AssertionError('Immutable published .86 analysis source changed')
    reviewed_analysis86_source1987((source / 'GpuAnalysis1961.java').read_text())
    sys.path.insert(0, str(source))
    spec = importlib.util.spec_from_file_location('analysis87_compile_closure', source / 'build1986.py')
    builder = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(builder)
    sources = list(dict.fromkeys(list(builder.compile_inputs().values()) + [builder.production_path(n) for n in builder.PRODUCTION]))
    production = work / 'production-classes'
    if production.exists():
        shutil.rmtree(production)
    production.mkdir()
    run([jdk / 'bin/javac', '-source', '8', '-target', '8', '-Xlint:-options', '-encoding', 'UTF-8',
         '-bootclasspath', jar, '-d', production, *sources], work / 'production-compile.log')
    fixture = source / 'tests1987/analysis'
    inputs = [source / 'GpuAnalysis1961.java', source / 'SpatialNoise1934.java',
              source / 'QualityPixels1932.java', source / 'StrongNoise1958.java',
              fixture / 'AnalysisPeers1987.java', fixture / 'AnalysisCapture1987Test.java', reference]
    before = {str(p.relative_to(source)): sha(p) for p in inputs}
    reports = {}
    for label, wrapper, negative in [('published86', reference, True), ('candidate87', source / 'GpuAnalysis1961.java', False)]:
        classes = work / (label + '-classes')
        if classes.exists():
            shutil.rmtree(classes)
        classes.mkdir()
        classpath = str(production) + os.pathsep + str(jar)
        run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', classpath,
             '-d', classes, wrapper, fixture / 'AnalysisPeers1987.java', fixture / 'AnalysisCapture1987Test.java'],
            work / (label + '-compile.log'))
        output = run([jdk / 'bin/java', '-ea', '-Xmx1g', '-XX:ActiveProcessorCount=4', '-cp',
                      str(classes) + os.pathsep + classpath, 'com.hiro.ulike.AnalysisCapture1987Test',
                      *(['negative'] if negative else [])], work / (label + '-run.log'), negative)
        if not negative:
            found = re.search(r'^RESULT (\{[^\n]+\})$', output, re.M)
            if not found:
                raise AssertionError('Executed analysis regression report missing')
            report = json.loads(found.group(1))
            if report.get('status') != 'passed' or report.get('assertions', 0) < 90 or not all(report.get(k) is True for k in FLAGS):
                raise AssertionError('Incomplete executed analysis coverage')
            reports[label] = report
    if before != {str(p.relative_to(source)): sha(p) for p in inputs}:
        raise AssertionError('Analysis source changed during regression')
    report = dict(reports['candidate87'],
                  analysis87_published86_negative_control_reproduced=True,
                  published86_source_sha256=PUBLISHED_SHA,
                  source_sha256=before,
                  android_jar_sha256=sha(jar),
                  scope='Actual GpuAnalysis1961 plus unchanged SpatialNoise1934, QualityPixels1932 and StrongNoise1958 CPU estimators; controlled reservation and GPU readback peers. No physical GPU/Android performance claim.')
    (work / 'result.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', default=str(ROOT))
    parser.add_argument('--work', default=str(ROOT.parent / 'analysis87-work'))
    parser.add_argument('--jdk')
    parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), indent=2))
