#!/usr/bin/env python3
"""Run all .81 GPU fault assertions with one additive production declaration.

Only the retained builder's compiler-only source map is extended. The original
fault runner, fixtures, native fault libraries, negative controls and every
assertion execute unchanged, with the real .82 diagnostic helper compiled too.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json

ROOT = Path(__file__).resolve().parent
RETAINED = {
    'host_audit_gpu1981.py': '1ad32a7bf6cbd4ade79d25a20e26a1314e9145234fcf0de12001cd6b4fa923a7',
    'build1981.py': 'aed18972af3bc3255b7ed8df24afb1db81f4051105d82793a176dfa423c1bb27',
}
ADDITIONAL_SOURCE = 'PipelineDiagnostics1982.java'
COMPILER_KEY = 'com/hiro/ulike/' + ADDITIONAL_SOURCE


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def adapted_runner(source):
    source = Path(source).resolve()
    for name, digest in RETAINED.items():
        if sha(source / name) != digest:
            raise AssertionError('Retained GPU audit source changed: ' + name)
    extra = source / ADDITIONAL_SOURCE
    pins = {name: sha(source / name) for name in (*RETAINED, ADDITIONAL_SOURCE)}
    pins[Path(__file__).name] = sha(__file__)
    spec = importlib.util.spec_from_file_location('gpu1982_retained81', source / 'host_audit_gpu1981.py')
    retained = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(retained)
    original_module = retained.module
    additions = []

    def module(name, path):
        loaded = original_module(name, path)
        if Path(path).resolve() == source / 'build1981.py':
            if name != 'gpu1981_current_compile':
                raise AssertionError('Unexpected GPU compiler-closure import')
            original_inputs = loaded.compile_inputs

            def compile_inputs():
                previous = original_inputs()
                if COMPILER_KEY in previous or any(Path(p).name == ADDITIONAL_SOURCE for p in previous.values()):
                    raise AssertionError('Diagnostic source already exists in the retained closure')
                result = dict(previous)
                result[COMPILER_KEY] = extra
                if {k: v for k, v in result.items() if k != COMPILER_KEY} != previous:
                    raise AssertionError('Retained GPU compiler inputs were replaced')
                additions.append(ADDITIONAL_SOURCE)
                return result

            loaded.compile_inputs = compile_inputs
        return loaded

    retained.module = module
    return retained, additions, pins


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    retained, additions, pins = adapted_runner(source)
    result = retained.test(source, work, jdk=jdk, ndk=ndk)
    if additions != [ADDITIONAL_SOURCE]:
        raise AssertionError('The retained GPU closure was not extended exactly once')
    if result.get('production_source_sha256', {}).get(ADDITIONAL_SOURCE) != pins[ADDITIONAL_SOURCE]:
        raise AssertionError('The executed GPU evidence omits the diagnostic declaration')
    if not (work / 'production-classes/com/hiro/ulike/PipelineDiagnostics1982.class').is_file():
        raise AssertionError('The added GPU diagnostic source did not compile')
    if pins != {name: sha(source / name) for name in pins}:
        raise AssertionError('GPU adapter input changed during execution')
    result.update(gpu_diagnostic_closure_adapter1982_verified=True,
                  diagnostic_adapter_source_sha256=pins,
                  diagnostic_compile_additions1982=additions,
                  retained_runner_source_sha256=RETAINED['host_audit_gpu1981.py'],
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
