#!/usr/bin/env python3
"""Execute .69 camera detail UI and retained storage/export regressions on real Java source."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, re, shutil, subprocess
ROOT = Path(__file__).resolve().parent
BASELINE_SHA256 = '2232b1a6b7de05fb3920b0203f18b347a403483cb467253dc19fe21ce88ac94c'

def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def checked_replace(text, before, after, count=1):
    if text.count(before) != count:
        raise AssertionError('Exact expectation adaptation missing: '+before)
    return text.replace(before, after)

def test(source, work, jdk=None):
    source = Path(source).resolve()
    if source.is_dir():
        source = source / 'CameraTrace1965.java'
    work = Path(work).resolve() / 'camera-details1969'
    shutil.rmtree(work, ignore_errors=True)
    work.mkdir(parents=True)
    reference = ROOT / 'tests1969/reference68/CameraTrace1965.java'
    if digest(reference) != BASELINE_SHA256:
        raise AssertionError('Canonical released .68 source pin does not match')
    baseline, candidate = reference.read_bytes(), source.read_bytes()
    start = b'    /** Opened only through the existing timing result row;'
    end = b'    public static String exportStatus1967()'
    if baseline.count(start) != 1 or baseline.count(end) != 1 or candidate.count(start) != 1 or candidate.count(end) != 1:
        raise AssertionError('Reviewed camera UI boundary is ambiguous')
    if candidate[:candidate.index(start)].replace(b'VERSION="1.9.69"', b'VERSION="1.9.68"') != baseline[:baseline.index(start)]:
        raise AssertionError('Recorder prefix changed beyond the version literal')
    if candidate[candidate.index(end):] != baseline[baseline.index(end):]:
        raise AssertionError('Storage/export/layout recording suffix changed')
    roots = [source.parent, ROOT, ROOT.parent / 'baseline68/src']
    inherited = next((p for p in roots if (p / 'tests1965/trace-fixtures').is_dir() and (p / 'host_camera_export1967.py').is_file() and (p / 'camera_agent_tests/CameraExport1967Test.java').is_file()), None)
    if inherited is None:
        raise AssertionError('Pinned inherited Android/storage/export fixtures are unavailable')
    fixtures = inherited / 'tests1965/trace-fixtures'
    own = ROOT / 'camera69testfixtures'
    own_sources = sorted(own.rglob('*.java'))
    if len(own_sources) != 2:
        raise AssertionError('Focused camera fixture graph incomplete')
    java = Path(jdk).resolve() / 'bin/java' if jdk else Path(shutil.which('java') or '/usr/lib/jvm/java-17-openjdk-amd64/bin/java').resolve()
    javac = java.parent / 'javac'
    compiler = [str(javac)] if javac.is_file() else [str(java), '-m', 'jdk.compiler/com.sun.tools.javac.Main']
    classes = work / 'classes'
    reference_classes = work / 'reference68-classes'
    boundary_sources = sorted(fixtures.rglob('*.java'))
    def compile_java(sources, destination, label):
        destination.mkdir(parents=True, exist_ok=True)
        result = subprocess.run(compiler+['-source', '8', '-target', '8', '-Xlint:-options', '-encoding', 'UTF-8', '-d', str(destination), *map(str, sources)], capture_output=True, text=True, timeout=60)
        (work / (label+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:
            raise RuntimeError((work / (label+'.log')).read_text())
    compile_java([source, *boundary_sources, *own_sources], classes, 'compile')
    compile_java([reference, *boundary_sources], reference_classes, 'compile-reference68')
    def trace_inventory(path):
        return sorted(p.name for p in (path / 'com/hiro/ulike').glob('CameraTrace1965*.class'))
    old_inventory, new_inventory = trace_inventory(reference_classes), trace_inventory(classes)
    if old_inventory != new_inventory:
        raise AssertionError('Camera recorder anonymous class inventory changed')
    javap = java.parent / 'javap'
    disassembler = [str(javap)] if javap.is_file() else [str(java), '-m', 'jdk.jdeps/com.sun.tools.javap.Main']
    def accessors(path):
        result = subprocess.run(disassembler+['-private', '-classpath', str(path), 'com.hiro.ulike.CameraTrace1965'], capture_output=True, text=True, timeout=20)
        if result.returncode:
            raise RuntimeError(result.stderr)
        return [line.strip() for line in result.stdout.splitlines() if ' access$' in line]
    old_accessors, new_accessors = accessors(reference_classes), accessors(classes)
    if old_accessors != new_accessors:
        raise AssertionError('Existing camera synthetic accessor identities changed')
    data = work / 'focused-data'
    result = subprocess.run([str(java), '-Xmx128m', '-cp', str(classes), 'com.hiro.ulike.CameraDetails1969Test', str(data)], capture_output=True, text=True, timeout=45)
    (work / 'focused.log').write_text(result.stdout+result.stderr)
    if result.returncode:
        raise RuntimeError((work / 'focused.log').read_text())
    match = re.search(r'^RESULT (\{[^\n]+\})$', result.stdout, re.M)
    if not match:
        raise AssertionError('Executed focused UI assertion result absent')
    focused = json.loads(match.group(1))
    # Execute the original 375 assertions against the actual .69 candidate. Only
    # the version and newly appended menu count expectations are adapted. The
    # historical source and fixtures remain unchanged in the published graph.
    original_runner_path = inherited / 'host_camera_export1967.py'
    original_test_path = inherited / 'camera_agent_tests/CameraExport1967Test.java'
    original_runner, original_test = original_runner_path.read_text(), original_test_path.read_text()
    adapted_runner = checked_replace(original_runner, 'initial.contains("1.9.67")', 'initial.contains("1.9.69")')
    adapted_runner = checked_replace(adapted_runner, 'dialog.items.length==3', 'dialog.items.length==4')
    adapted_runner = checked_replace(adapted_runner, 'helper process-start version 1.9.65 to 1.9.67', 'helper process-start version 1.9.65 to 1.9.69')
    adapted_runner = checked_replace(adapted_runner, 'diagnostic menu item count 2 to 3', 'diagnostic menu item count 2 to 4')
    adapted_runner = checked_replace(adapted_runner, "ROOT/'repo/releases/ulike1965-camera-trace/src/tests1965/trace-fixtures'", 'Path('+repr(str(fixtures))+')')
    adapted_test = checked_replace(original_test, 'items.length==3', 'items.length==4')
    adapted_test = checked_replace(adapted_test, 'all.contains("1.9.67")', 'all.contains("1.9.69")')
    adaptor = work / 'controlled-inherited-adaptor'
    (adaptor / 'camera_agent_tests').mkdir(parents=True)
    runner_copy = adaptor / 'host_camera_export1969_adapted.py'
    runner_copy.write_text(adapted_runner)
    test_copy = adaptor / 'camera_agent_tests/CameraExport1967Test.java'
    test_copy.write_text(adapted_test)
    spec = importlib.util.spec_from_file_location('controlled_camera_export1969', runner_copy)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    retained = module.test(source, work / 'retained-logging', jdk=jdk)
    if retained.get('status') != 'passed' or retained.get('assertions', 0) < 375:
        raise AssertionError('Retained storage/export suite did not execute every original assertion')
    preservation = {
        'baseline68_sha256': digest(reference), 'candidate69_sha256': digest(source),
        'recording_prefix_identical_except_version': True, 'all_code_from_export_status_through_storage_identical': True,
        'compiled_camera_class_inventory_unchanged': True, 'compiled_camera_class_count': len(new_inventory),
        'synthetic_accessors_unchanged': True, 'synthetic_accessors': new_accessors,
        'canonical_reference_path': 'tests1969/reference68/CameraTrace1965.java'
    }
    adapters = {
        'runner_original_sha256': digest(original_runner_path), 'runner_adapted_sha256': digest(runner_copy),
        'fixture_original_sha256': digest(original_test_path), 'fixture_adapted_sha256': digest(test_copy),
        'expectation_adaptations': ['historical helper version .65 to .69', 'historical menu count 2 to 4', 'export fixture version .67 to .69', 'export fixture menu count 3 to 4'],
        'fixture_path_redirect_only': True, 'all_other_assertions_retained': True
    }
    report = {
        'status': 'passed', 'assertions': focused['assertions']+retained['assertions']+5,
        'focused_ui_assertions': focused['assertions'], 'retained_logging_assertions': retained['assertions'],
        'camera_details_regressions_passed': True, 'logging_regressions_passed': True,
        'full_summary_dialog_verified': True, 'summary_explanation_verified': True,
        'detail_action_has_no_diagnostic_side_effects': True, 'existing_menu_actions_preserved': True,
        'bounded_optional_failure_toast_verified': True, 'logger_source_preservation_verified': True,
        'trace_storage_regressions_passed': retained['trace_storage_regressions_passed'],
        'trace_export_regressions_passed': retained['trace_export_regressions_passed'],
        'multiprocessing_writer_isolation_passed': retained['multiprocessing_writer_isolation_passed'],
        'bounded_oversize_recovery_passed': retained['bounded_oversize_recovery_passed'],
        'physical_android_tested': False, 'source_preservation': preservation,
        'controlled_inherited_adaptations': adapters, 'retained_logging_report': retained
    }
    (work / 'result.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    return report

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', default=str(ROOT / 'CameraTrace1965.java'))
    parser.add_argument('--work', default=str(ROOT / 'work'))
    parser.add_argument('--jdk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk), ensure_ascii=False, indent=2))
