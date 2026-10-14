#!/usr/bin/env python3
"""Retain all .86 backend diagnostics through exact .87 preservation inverses.

Only the preservation input restores the reviewed Finish scheduling and analysis
capture spans. Every current-source backend, Resident and copy ownership test
still compiles and executes the unmodified current production classes.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json

RETAINED86_SHA256 = 'a4b881cb8754b40da468970b655e27670eef04aa38d7260af4c73d297037cd0a'
REQUIRED_FLAGS = ('pipeline_reviewed_scheduling_inverse1987_verified',
                  'pipeline_reviewed_analysis_inverse1987_verified',
                  'pipeline_current_complete_backend_suite1987_verified')


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec);spec.loader.exec_module(result)
    return result


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve();work.mkdir(parents=True, exist_ok=True)
    retained_path = source / 'host_pipeline_diagnostics1986.py'
    if sha(retained_path) != RETAINED86_SHA256:
        raise AssertionError('Frozen .86 pipeline diagnostic runner changed')
    finish = module('pipeline87_finish_inverse', source / 'host_finish_baseline1987.py')
    analysis = module('pipeline87_analysis_inverse', source / 'host_analysis1987.py')
    tracked = [source / name for name in ('host_pipeline_diagnostics1986.py', 'host_finish_baseline1987.py',
        'host_finish_reservation1987.py', 'host_analysis1987.py', 'GpuChain1961.java', 'GpuAnalysis1961.java',
        'tests1987/finish_reservation/baseline86/GpuChain1961.java',
        'tests1987/analysis/published86/GpuAnalysis1961.java')]
    tracked.append(Path(__file__).resolve());pins = {str(path): sha(path) for path in tracked}
    observed = {'finish': [], 'analysis': [], 'backend_module': 0}

    def install82(value):
        observed['backend_module'] += 1;original_preservation = value.preservation

        def preservation(tree):
            tree = Path(tree).resolve()
            if work not in tree.parents:
                raise AssertionError('Analysis inverse is restricted to the existing preservation-only tree')
            declaration_name = 'tests1982/pipeline/preserved81.json'
            declaration = json.loads((tree / declaration_name).read_text())
            destination = work / 'analysis-preservation-only'
            for name in set(declaration['complete_files']) | set(declaration['methods']) | {declaration_name}:
                path = destination / name;path.parent.mkdir(parents=True, exist_ok=True)
                if path.exists() or path.is_symlink():
                    raise AssertionError('Unexpected pre-existing analysis preservation input: ' + str(path))
                if name == 'GpuAnalysis1961.java':
                    current = (tree / name).read_text()
                    restored = analysis.reviewed_analysis86_source1987(current)
                    path.write_text(restored)
                    observed['analysis'].append(dict(current_sha256=hashlib.sha256(current.encode()).hexdigest(),
                                                      restored86_sha256=sha(path)))
                else:
                    path.symlink_to(tree / name)
            return original_preservation(destination)

        value.preservation = preservation
        return value

    def install84(value):
        original = value.module
        def load(name, requested):
            loaded = original(name, requested)
            return install82(loaded) if Path(requested).resolve() == source / 'host_pipeline_diagnostics1982.py' else loaded
        value.module = load;return value

    def install85(value):
        original = value.module
        def load(name, requested):
            loaded = original(name, requested)
            return install84(loaded) if Path(requested).resolve() == source / 'host_pipeline_diagnostics1984.py' else loaded
        value.module = load;return value

    retained = module('pipeline87_retained86', retained_path);original = retained.module
    def load(name, requested):
        loaded = original(name, requested);path = Path(requested).resolve()
        if path == source / 'host_finish_baseline1986.py':
            return finish.install_inverse1987(loaded, source, observed['finish'])
        if path == source / 'host_pipeline_diagnostics1985.py':
            return install85(loaded)
        return loaded
    retained.module = load
    result = retained.test(source, work / 'retained86', jdk=jdk, ndk=ndk)
    if len(observed['finish']) != 1 or len(observed['analysis']) != 1 or observed['backend_module'] != 1:
        raise AssertionError('Both exact .87 preservation inverses and the complete backend suite must execute once')
    if any(result.get(flag) is not True for flag in retained.REQUIRED_FLAGS):
        raise AssertionError('A retained complete pipeline diagnostic requirement did not execute')
    if pins != {str(path): sha(path) for path in tracked}:
        raise AssertionError('Current analysis/Finish or frozen contract input changed during execution')
    result.update({flag: True for flag in REQUIRED_FLAGS})
    result.update(inverse_execution1987=observed, wrapper_input_sha256=pins,
                  retained_diagnostics86_sha256=RETAINED86_SHA256, runner_source_sha256=sha(__file__))
    result['preservation_adapter1987'] = ('Two reviewed control-flow changes are inverted only in source-preservation inputs. '
        'The entire restored .86 files must match immutable pins. Every retained current-source backend, ownership and proof test executes unchanged production.')
    (work / 'report.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True);parser.add_argument('--work', required=True)
    parser.add_argument('--jdk');parser.add_argument('--ndk');args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
