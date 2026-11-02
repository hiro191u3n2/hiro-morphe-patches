#!/usr/bin/env python3
"""Run all .81 pipeline assertions with one additive production declaration.

The retained runner and compiler closure are hash-pinned. Its input list is
extended only by PipelineDiagnostics1982.java; compiler options, fixtures,
negative controls, execution commands and assertions remain unchanged.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json

ROOT = Path(__file__).resolve().parent
RETAINED = {
    'host_pipeline_routes1981.py': 'ac0dd96ea31b65776087311320fabc4a2f8a9241b7331ffd3de7e50fae121528',
    'build1980.py': 'a3a02b74388750223dbc577f0c6714a8b8dee7716a6aad0fd2f52f76c5b7f29f',
}
ADDITIONAL_SOURCE = 'PipelineDiagnostics1982.java'


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def adapted_runner(source):
    source = Path(source).resolve()
    for name, digest in RETAINED.items():
        if sha(source / name) != digest:
            raise AssertionError('Retained pipeline source changed: ' + name)
    extra = source / ADDITIONAL_SOURCE
    pins = {name: sha(source / name) for name in (*RETAINED, ADDITIONAL_SOURCE)}
    pins[Path(__file__).name] = sha(__file__)
    spec = importlib.util.spec_from_file_location('pipeline1982_retained81', source / 'host_pipeline_routes1981.py')
    retained = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(retained)
    original_inputs = retained.compile_inputs
    additions = []

    def compile_inputs(current_source):
        if Path(current_source).resolve() != source:
            raise AssertionError('Pipeline compile source differs from the reviewed source')
        previous = list(original_inputs(current_source))
        if extra in previous or any(Path(p).name == ADDITIONAL_SOURCE for p in previous):
            raise AssertionError('Diagnostic source already exists in the retained closure')
        result = previous + [extra]
        if result[:-1] != previous or result[-1] != extra:
            raise AssertionError('Retained pipeline compiler inputs were replaced')
        additions.append(ADDITIONAL_SOURCE)
        return result

    retained.compile_inputs = compile_inputs
    return retained, additions, pins


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    retained, additions, pins = adapted_runner(source)
    result = retained.test(source, work, jdk=jdk, ndk=ndk)
    if additions != [ADDITIONAL_SOURCE]:
        raise AssertionError('The retained pipeline closure was not extended exactly once')
    if result.get('production_source_sha256', {}).get(ADDITIONAL_SOURCE) != pins[ADDITIONAL_SOURCE]:
        raise AssertionError('The executed pipeline evidence omits the diagnostic declaration')
    if not (work / 'production-classes/com/hiro/ulike/PipelineDiagnostics1982.class').is_file():
        raise AssertionError('The added pipeline diagnostic source did not compile')
    if pins != {name: sha(source / name) for name in pins}:
        raise AssertionError('Pipeline adapter input changed during execution')
    result.update(pipeline_diagnostic_closure_adapter1982_verified=True,
                  diagnostic_adapter_source_sha256=pins,
                  diagnostic_compile_additions1982=additions,
                  retained_runner_source_sha256=RETAINED['host_pipeline_routes1981.py'],
                  adapter_source_sha256=sha(__file__))
    (work / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=ROOT)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    parser.add_argument('--ndk', type=Path)
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
