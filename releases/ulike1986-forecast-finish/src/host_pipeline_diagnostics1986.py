#!/usr/bin/env python3
"""Run every retained pipeline diagnostic gate with an exact .86 source inverse.

The current full production closure and all backend/Resident cases execute.
Only the preservation input first restores the reviewed finish admission span
to the complete pinned .85 source. The frozen .83 capacity inverse and the
entire original .82 mathematical/source contract then run without alteration.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json

RETAINED = {
    'host_pipeline_diagnostics1985.py': '0725ab5493d58900d96ade019b8a0803863128391246f332d8fe84014ebc1e71',
    'host_pipeline_diagnostics1984.py': 'd00d4653039c803df583168a9188f5cd678e168fedd82c66fe1242d0e0106d96',
    'host_pipeline_diagnostics1983.py': '273ebeb9e62c94a23ff4ec2e556b69906880c215cd42e8865957488463518431',
    'host_resident_diagnostics1985.py': 'bc756e6479be7559135f525e943943ecd933b313bc99506c44589dbf04a18a2b',
}
REQUIRED_FLAGS = (
    'pipeline_reviewed_finish_inverse1986',
    'pipeline_retained_full_backend_suite1986',
)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    for name, expected in RETAINED.items():
        if sha(source / name) != expected:
            raise AssertionError('A frozen pipeline wrapper changed: ' + name)
    finish = module('pipeline86_reviewed_finish_inverse', source / 'host_finish_baseline1986.py')
    baseline85 = source / 'tests1986/finish_baseline/baseline85/GpuChain1961.java'
    reviewed = source / 'tests1986/finish_baseline/reviewed-spans.json'
    input_sha = {name: sha(source / name) for name in RETAINED}
    input_sha.update({name: sha(source / name) for name in (
        'GpuChain1961.java', 'host_finish_baseline1986.py',
        'tests1986/finish_baseline/reviewed-spans.json',
        'tests1986/finish_baseline/baseline85/GpuChain1961.java')})
    retained = module('pipeline86_retained85', source / 'host_pipeline_diagnostics1985.py')
    load85 = retained.module
    observed = {'wrapper84': 0, 'inverse83_module': 0, 'exact_inverse': 0}

    def load_with_reviewed_inverse(name, path):
        value = load85(name, path)
        if Path(path).resolve() == source / 'host_pipeline_diagnostics1984.py':
            observed['wrapper84'] += 1
            load84 = value.module

            def load84_with_inverse(inner_name, inner_path):
                inner = load84(inner_name, inner_path)
                if Path(inner_path).resolve() == source / 'host_pipeline_diagnostics1983.py':
                    observed['inverse83_module'] += 1
                    inverse83 = inner.inverse_chain_capacity1983

                    def exact_chain_inverse(current, baseline82):
                        restored85 = finish.inverse_chain1986(current, baseline85.read_bytes())
                        if hashlib.sha256(restored85).hexdigest() != finish.BASELINE85_SHA256:
                            raise AssertionError('The .86 preservation inverse did not produce the complete .85 source')
                        result = inverse83(restored85, baseline82)
                        observed['exact_inverse'] += 1
                        return result

                    inner.inverse_chain_capacity1983 = exact_chain_inverse
                return inner

            value.module = load84_with_inverse
        return value

    retained.module = load_with_reviewed_inverse
    result = retained.test(source, work / 'retained85', jdk=jdk, ndk=ndk)
    if observed != {'wrapper84': 1, 'inverse83_module': 1, 'exact_inverse': 1}:
        raise AssertionError('The exact .86/.83 inverse chain must execute once: ' + repr(observed))
    for flag in (
        'pipeline_retained_full_backend_suite1985', 'pipeline_reviewed_capacity_inverse1983',
        'pipeline_reviewed_resident_diagnostic_inverse1984', 'pipeline_resident_ticket_contract1984',
        'pipeline_published83_unreserved_copy_negative1984', 'pipeline_math_and_gates_preserved1982',
        'pipeline_resident_epoch_handoff1985', 'pipeline_resident_begin_gate1985',
        'pipeline_resident_copy_diagnostics1985'):
        if result.get(flag) is not True:
            raise AssertionError('A retained pipeline gate did not execute: ' + flag)
    if input_sha != {name: sha(source / name) for name in input_sha}:
        raise AssertionError('Reviewed inverse or frozen pipeline inputs changed during execution')
    result.update({flag: True for flag in REQUIRED_FLAGS})
    result.update(reviewed_finish_baseline85_sha256=finish.BASELINE85_SHA256,
                  reviewed_finish_inverse_source_sha256=sha(source / 'host_finish_baseline1986.py'),
                  reviewed_finish_spans_sha256=sha(reviewed),
                  retained_pipeline_wrapper_sha256=RETAINED,
                  inverse_execution1986=observed,
                  runner_source_sha256=sha(__file__))
    result['preservation_adapter1986'] = (
        'The exact reviewed .86 qualification method and five additive helpers are inverted only in the preservation input. '
        'The complete restored source must equal the pinned published .85 source before the frozen .83 inverse runs. '
        'Every existing mathematical, pixel, route, Resident proof, ownership, rejection and negative-control assertion executes. '
        'All execution compiles current production source; no historical source is substituted for runtime behavior.'
    )
    (work / 'report.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
