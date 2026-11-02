#!/usr/bin/env python3
"""Resident timing diagnostics through the actual queue and versioned journal.
Uses independent JVMs for restoration; no diagnostic can supply admission proof.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, subprocess

LEGACY_FIXTURE_SHA19115 = '8086b511749b95d91923ca3f36b35f610cdcc62df5a4ed7fc9ea506bd046e13f'

def legacy_wire_sizes19115(body):
    """Extend only retained capacity checks; the VJ8 wire stays exactly269 words."""
    if hashlib.sha256(body.encode()).hexdigest() != LEGACY_FIXTURE_SHA19115:
        raise AssertionError('Frozen resident111 journal fixture differs')
    def once(old, new):
        nonlocal body
        if body.count(old) != 1: raise AssertionError('Resident111 capacity adapter anchor differs')
        body = body.replace(old, new)
    once('VerificationJournal19101.VALUES==269',
         'VerificationJournal19101.GPU_TIMING_VALUES==269&&VerificationJournal19101.VALUES==303')
    for version, count in [('1.9.110', 153), ('1.9.108', 133)]:
        entry = 'new VerificationJournal19101.Entry("legacy-producer","'+version+'",Arrays.copyOf(without,'+str(count)+'),oldSource.labels)'
        once(entry+'.v.length==269', 'legacySize19115('+entry+','+str(count)+')==269')
    once(' static void noTiming(VerificationJournal19101.Entry e,String why){for(int i=START;i<START+WIDTH*SLOTS;i++)check(e.v[i]==0,why);}',
         ' static void noTiming(VerificationJournal19101.Entry e,String why){for(int i=START;i<START+WIDTH*SLOTS;i++)check(e.v[i]==0,why);noCpuTiming19115(e,why);}')
    marker=' static String codec()throws Exception{'
    helpers=''' static int cpuTailWords19115,legacyPaddingWords19115,cpuCapacityChecks19115;
 static void noCpuTiming19115(VerificationJournal19101.Entry e,String why){
  check(e.v.length==303,"current fixed scalar capacity");cpuCapacityChecks19115++;
  for(int i=269;i<303;i++){check(e.v[i]==0,"legacy CPU tail remains unobserved: "+why);cpuTailWords19115++;}
 }
 static int legacySize19115(VerificationJournal19101.Entry e,int oldCount){
  noCpuTiming19115(e,"legacy producer");
  for(int i=oldCount;i<269;i++){check(e.v[i]==0,"legacy producer preserves original269-word zero extension");legacyPaddingWords19115++;}
  return VerificationJournal19101.GPU_TIMING_VALUES;
 }
'''
    once(marker, helpers+marker)
    roundtrip='check(Arrays.equals(e.v,VerificationJournal19101.decode(encoded).get(0).v),"all269scalar exact round trip");'
    once(roundtrip, roundtrip+'noCpuTiming19115(e,"VJ8 producer");noCpuTiming19115(VerificationJournal19101.decode(encoded).get(0),"VJ8 decoded");')
    executed='  live();String encoded=codec();Files.write(Paths.get(args[0]),encoded.getBytes(StandardCharsets.UTF_8));'
    once(executed, executed+'\n  System.out.println("COMPAT19115 "+cpuTailWords19115+" "+legacyPaddingWords19115+" "+cpuCapacityChecks19115);')
    return body


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
    # The published111 fixture itself stays immutable. Only this generated
    # copy understands the larger in-memory record; all VJ8 schema, rejection,
    # first269 scalar and restart assertions continue to execute unchanged.
    historical_fixture = source / 'tests19111/diagnostics/ResidentDiagnostics19111Test.java'
    generated_fixture = work / 'fixtures/com/hiro/ulike/ResidentDiagnostics19111Test.java'
    generated_fixture.parent.mkdir(parents=True, exist_ok=True)
    generated_fixture.write_text(legacy_wire_sizes19115(historical_fixture.read_text()))
    files += [source / 'GpuQualification1961.java', source / 'VerificationJournal19101.java', generated_fixture]
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
    output = run('run', [jdk / 'bin/java', '-ea', '-Xmx64m', '-cp', classes, 'com.hiro.ulike.ResidentDiagnostics19111Test', snapshot])
    result = json.loads(output.strip().splitlines()[-1])
    compatibility = re.findall(r'^COMPAT19115 (\d+) (\d+) (\d+)$', output, re.M)
    if compatibility != [('612', '252', '18')]:
        raise AssertionError('Executed legacy scalar/tail compatibility checks incomplete')
    restart = json.loads(run('restart', [jdk / 'bin/java', '-ea', '-Xmx64m', '-cp', classes, 'com.hiro.ulike.ResidentDiagnostics19111Test', '--restart', snapshot]).strip().splitlines()[-1])
    if restart.get('status') != 'passed' or restart.get('independent_jvm_vj8_restore111') is not True:
        raise AssertionError('Independent JVM VJ8 restoration failed')
    result['assertions'] += restart['assertions']
    result['cases'] += restart['cases']
    result['independent_jvm_vj8_restore111'] = True
    if pins != {str(p.relative_to(source)): hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:
        raise AssertionError('Production or fixture source changed during journal integration')
    result.update(source_sha256=pins, fixture_classes_in_runtime=False, physical_android_tested=False, device_speedup_verified=False,
                  legacy_vj8_269_prefix_preserved19115=True, legacy_cpu_tail_zero_checked19115=True,
                  legacy_cpu_tail_zero_words19115=612, legacy_zero_extension_words19115=252,
                  legacy_memory_capacity_checks19115=18,
                  historical_fixture_sha256=LEGACY_FIXTURE_SHA19115,
                  generated_fixture_sha256=hashlib.sha256(generated_fixture.read_bytes()).hexdigest())
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
