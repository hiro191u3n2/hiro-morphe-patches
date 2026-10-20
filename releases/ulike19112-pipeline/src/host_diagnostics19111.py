#!/usr/bin/env python3
"""Resident timing diagnostics through the actual queue and versioned journal.
Uses independent JVMs for restoration; no diagnostic can supply admission proof.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, subprocess


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ['ULIKE_JDK_HOME'])
    fixture = source / 'tests19101/journal'
    tracked = [source / 'GpuQualification1961.java', source / 'VerificationJournal19101.java', source / 'host_qualification1981.py', Path(__file__).resolve(), source / 'tests19111/diagnostics/ResidentDiagnostics19111Test.java', *fixture.glob('*.java')]
    pins = {str(p.relative_to(source)): hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    spec = importlib.util.spec_from_file_location('journal_queue_peers19101', source / 'host_qualification1981.py')
    peers = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(peers)
    files = []
    for name, content in peers.FIXTURES.items():
        if name.endswith('QueueHost1967.java'):
            continue
        if name == 'android/content/Context.java':
            content = (fixture / 'Context.java').read_text()
        elif name == 'android/content/SharedPreferences.java':
            content = content.replace('void apply();', 'void apply();boolean commit();')
        elif name.endswith('QueueFixtures1967.java'):
            version = re.search(r'public static final String VERSION\s*=\s*"([^"]+)"', (source/'ProcessingTiming1947.java').read_text()).group(1)
            content = content.replace('final class ProcessingTiming1947 {', 'final class ProcessingTiming1947 {static final String VERSION="'+version+'";')
            content = content.replace('static volatile long epoch=1;', 'static volatile long epoch=1,photo=1;').replace('static long captureEpoch1953(){return epoch;}', 'static long captureEpoch1953(){return epoch;}static long captureId1989(long e){return e==epoch?photo:0;}')
        p = work / 'fixtures' / name
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(content)
        files.append(p)
    files += [source / 'GpuQualification1961.java', source / 'VerificationJournal19101.java', source / 'tests19111/diagnostics/ResidentDiagnostics19111Test.java']
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    def run(label, args):
        done = subprocess.run(list(map(str, args)), capture_output=True, text=True, timeout=60)
        (work / (label + '.log')).write_text(done.stdout + done.stderr)
        if done.returncode:
            raise AssertionError(label + '\n' + (done.stdout + done.stderr)[-16000:])
        return done.stdout
    run('compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-d', classes, *files])
    snapshot = work / 'vj8-restart.txt'
    result = json.loads(run('run', [jdk / 'bin/java', '-ea', '-Xmx64m', '-cp', classes, 'com.hiro.ulike.ResidentDiagnostics19111Test', snapshot]).strip().splitlines()[-1])
    restart = json.loads(run('restart', [jdk / 'bin/java', '-ea', '-Xmx64m', '-cp', classes, 'com.hiro.ulike.ResidentDiagnostics19111Test', '--restart', snapshot]).strip().splitlines()[-1])
    if restart.get('status') != 'passed' or restart.get('independent_jvm_vj8_restore111') is not True:
        raise AssertionError('Independent JVM VJ8 restoration failed')
    result['assertions'] += restart['assertions']
    result['cases'] += restart['cases']
    result['independent_jvm_vj8_restore111'] = True
    if pins != {str(p.relative_to(source)): hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:
        raise AssertionError('Production or fixture source changed during journal integration')
    result.update(source_sha256=pins, fixture_classes_in_runtime=False, physical_android_tested=False, device_speedup_verified=False)
    (work / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--source', required=True)
    p.add_argument('--work', required=True)
    p.add_argument('--jdk')
    p.add_argument('--ndk')
    a = p.parse_args()
    print(json.dumps(test(a.source, a.work, a.jdk, a.ndk), ensure_ascii=False, indent=2))
