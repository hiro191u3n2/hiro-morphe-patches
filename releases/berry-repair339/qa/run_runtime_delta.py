#!/usr/bin/env python3
"""Verify the diagnostic-removal runtime scope against the active combined baseline."""
from pathlib import Path
from zipfile import ZipFile
import argparse
import hashlib
import json
import os
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
EXTENSION = 'extensions/berry_bfcache_restore.mpe'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


# These are source hashes from published 337 (commit 7cca97f...). The 338
# BFCache extension is byte-identical to 337; the rest of 338 is unrelated.
SOURCE337 = {
    'CompletionTarget.java': 'e15b9d7639f0584d66cbbdb662ade67c1368a129e7d2a1e9ad97bc256fa3c44f',
    'NativeCompletion.java': '6e46eb190cece94d4429ee3c402eaa05ada3934af4346f614aec4bb571481db6',
    'NativeRestoreTracker.java': '00fc3c864f939c7e1e12a901cdf0580a6de39ba4bb47b8665af164aaf3127a24',
    'PresentationRecovery.java': '0dacaac56f6e3938c0b666684c7b85fb40c773aa32561d02ce4f8250d9bd1efa',
    'RestoreCoordinator.java': 'f9b0b0e0a379be7b16f1170d43b3a50178b95547e7481c9bbaf16ec45e688ffa',
}
BASE_RUNTIME_SHA = 'be25832a7f7ab766b8a6c93ed0816562d05fba8dd4b5bd078a96922b17c64023'


def tokens(source):
    # Java strings/chars stay intact, so whitespace/comments inside strings
    # are not normalized and no source statement can disappear as whitespace.
    parts = re.findall(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/|[A-Za-z_$][\w$]*|[0-9]+|[^\s]', source)
    return [part for part in parts if not part.startswith(('//', '/*'))]


def approved_removal(name, text):
    def remove(old, new='', count=1):
        nonlocal text
        assert text.count(old) == count, (name, 'Unexpected diagnostic statement count', old)
        text = text.replace(old, new)
    if name in ('NativeRestoreTracker.java', 'RestoreCoordinator.java'):
        remove('import android.util.Log;\n')
        remove('    private static final String TAG = "BerryBFCache";\n')
    if name == 'NativeRestoreTracker.java':
        remove('        boolean failureLogged;\n')
        remove('        boolean identityLogged;\n')
        remove('''    private static void identityUnavailable(State s) {
        if (!s.identityLogged) {
            s.identityLogged = true;
            Log.w(TAG, "Native Page/navigation identity unavailable; JavaScript fallback retained", null);
        }
    }
''')
        remove('                        if (state.navigationPeer == null) identityUnavailable(state);\n')
        remove('            identityUnavailable(s);\n')
        for receiver, message, error, count in (
            ('s', 'Native Page restore tracking unavailable; JavaScript fallback retained', 'null', 1),
            ('s', 'Native Page callback was not on the UI thread; JavaScript fallback retained', 'null', 1),
            ('s', 'Native Page listener installation failed; JavaScript fallback retained', 'error', 1),
            ('state', 'Native Page callback was not on the UI thread; JavaScript fallback retained', 'null', 1),
            ('state', 'Native Page callback could not be read; JavaScript fallback retained', 'error', 1)):
            remove('unavailable(%s, "%s", %s)' % (receiver, message, error), 'unavailable(%s)' % receiver, count)
        remove('unavailable(State s, String message, Throwable error)', 'unavailable(State s)')
        remove('        if (!s.failureLogged) { s.failureLogged = true; Log.w(TAG, message, error); }\n')
    if name == 'RestoreCoordinator.java':
        remove('        boolean queryFailureLogged;\n')
        remove('        s.queryFailureLogged = false;\n')
        remove('        RenderDiagnostics.cancel(view);\n')
        remove('''            if (!s.queryFailureLogged) {
                s.queryFailureLogged = true;
                Log.w(TAG, "BFCache marker query was unavailable; retry is bounded", failure);
            }
''')
        remove('''        try {
            RenderDiagnostics.begin(view, proof, event);
        } catch (RuntimeException failure) {
            Log.w(TAG, "BFCache rendering diagnostics were unavailable", failure);
        }
''')
        for message in ('BFCache native proof completion was unavailable; retry is bounded',
                        'BFCache native completion was unavailable; retry is bounded',
                        'BFCache presentation recovery was unavailable'):
            remove('            Log.w(TAG, "%s", failure);\n' % message)
    if name == 'PresentationRecovery.java':
        remove('        String lastPhase;\n')
        remove('''    private static void record(State s, String phase) {
        if (phase.equals(s.lastPhase)) return;
        s.lastPhase = phase;
        WebView view = s.view.get();
        if (view != null) RenderDiagnostics.record(view, phase);
    }
''')
        remove('} else record(s, "WAIT_LAYOUT");', '}')
        # Every remaining record call is a standalone no-result observation.
        text, n = re.subn(r'record\((?:s|this|pending), "[A-Z_]+"\);', '', text)
        assert n == 15, (name, 'Unexpected phase observation count', n)
        text, n = re.subn(r'finish\((s|this|pending), "[A-Z_]+", false\)', r'finish(\1, false)', text)
        assert n == 16, (name, 'Unexpected diagnostic finish argument count', n)
        remove('finish(final State s, String phase, boolean deferObserverRemoval)', 'finish(final State s, boolean deferObserverRemoval)')
        remove('        record(s, phase);\n')
    return text


def source_preservation(reference, runtime_root, runtime_report):
    source_root = runtime_root / 'runtime_src/hiro/berrybfcache'
    assert {p.name for p in source_root.glob('*.java')} == set(SOURCE337), 'Unexpected runtime source inventory'
    rows = {}
    expected_build_sources = {}
    for name, pinned in SOURCE337.items():
        old, new = reference / 'runtime_src/hiro/berrybfcache' / name, source_root / name
        assert sha(old) == pinned, 'Published source337 pin mismatch: ' + name
        normalized = tokens(approved_removal(name, old.read_text()))
        assert normalized == tokens(new.read_text()), 'Non-diagnostic source token change: ' + name
        digest = sha(new)
        expected_build_sources['runtime_src/hiro/berrybfcache/' + name] = digest
        rows[name] = {'published337_sha256': pinned, 'candidate_sha256': digest,
                      'tokens_equal_after_explicit_diagnostic_deletions': True}
    report = json.loads(runtime_report.read_text())
    assert report['source_sha256'] == expected_build_sources, 'Test/build report source mismatch'
    assert report['result'] == 'PASS'
    return {'result': 'PASS_EXPLICIT_DIAGNOSTIC_DELETION_SOURCE_TOKENS',
            'reference_commit': '7cca97f00f1fbfbf96c3360793d1b0127921a42a',
            'source_files': rows, 'runtime_build_report_sha256': sha(runtime_report)}, report

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--dist', type=Path, required=True)
    parser.add_argument('--runtime-root', type=Path, default=ROOT / 'runtime')
    parser.add_argument('--reference-runtime', type=Path, default=ROOT.parent / 'reference337/runtime')
    parser.add_argument('--runtime-report', type=Path, required=True)
    args = parser.parse_args()
    preservation, runtime_report = source_preservation(args.reference_runtime, args.runtime_root, args.runtime_report)
    config = json.loads((ROOT / 'tool-paths.json').read_text())
    baseline = Path(config['baseline'])
    combined = args.dist / 'Hiro_Morphe_Patches_v1.0.339.mpp'
    single = args.dist / 'Berry_Browser_v1.4.5.mpp'
    work = ROOT / 'qa/runtime-delta'
    work.mkdir(exist_ok=True)
    paths = {}
    for name, artifact in [('baseline', baseline), ('combined', combined), ('single', single)]:
        with ZipFile(artifact) as archive:
            raw = archive.read(EXTENSION)
        paths[name] = work / (name + '.mpe')
        paths[name].write_bytes(raw)
    assert sha(paths['baseline']) == BASE_RUNTIME_SHA, 'Baseline runtime differs from published337'
    assert sha(paths['combined']) == runtime_report['extension_sha256'], 'Candidate build/source evidence not bound to shipped runtime'
    assert paths['combined'].read_bytes() == paths['single'].read_bytes(), 'Different Berry runtimes'
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    source = ROOT / 'qa/VerifyRuntimeDelta.java'
    host = config['tools']['classpath.jar']['path']
    subprocess.run([config['java'], '-jar', config['tools']['ecj.jar']['path'],
                    '-source', '11', '-target', '11', '-encoding', 'UTF-8',
                    '-cp', host, '-d', str(classes), str(source)], check=True)
    result = subprocess.run([config['java'], '-cp', str(classes) + os.pathsep + host,
                             'VerifyRuntimeDelta', str(paths['baseline']), str(paths['combined'])],
                            capture_output=True, text=True)
    (work / 'delta.log').write_text(result.stdout + result.stderr)
    result.check_returncode()
    delta = json.loads(result.stdout)
    delta['core_loading_and_presentation_preserved'] = True
    delta['core_source_tokens_preserved_after_explicit_diagnostic_deletions'] = True
    report = {'schema': 'berry-repair339-diagnostic-removal-runtime-delta-v1', 'result': 'PASS',
              'baseline_sha256': sha(baseline), 'combined_sha256': sha(combined),
              'standalone_sha256': sha(single), 'baseline_runtime_sha256': sha(paths['baseline']),
              'candidate_runtime_sha256': sha(paths['combined']), 'verifier_sha256': sha(source),
              'delta': delta, 'source_preservation': preservation, 'runner_sha256': sha(Path(__file__)), 'physical_device_tested': False}
    (ROOT / 'qa/QA_runtime_delta339.json').write_text(json.dumps(report, indent=2) + '\n')
    print(result.stdout.strip())


if __name__ == '__main__':
    main()
