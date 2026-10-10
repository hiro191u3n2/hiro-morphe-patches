#!/usr/bin/env python3
"""Execute all .86 timing gates with the two current-version display literals.

The historical .82 test and every .86/earlier runner remain immutable. Just
before its normal javac call, the .86-generated fixture receives the exact
ULike v1.9.88 display expectation in its two version-specific assertions.
Every other byte, assertion and current production input is preserved.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import re

PINS = {
    'host_timing1987.py': 'fbe9ff69df1eecab64f0c5a35ba45a63ee7ad9648b9d626ff20b4e8c003c21aa',
    'host_timing1986.py': '2562a0f0f25a056439c13158e48e1c2169c1f33183c2c1bbc9d4975ec81334ab',
    'host_timing1985.py': '42e05ea41392b2e4f7d1ed31809668c1442b4b61ce84e5a7316956114d8d657e',
    'Timing1982Test.java': '2f52053b21fc738a5bd522ce91a60ecc314ca212326b162c37b76c7d3facf9a7',
}
REQUIRED_FLAGS = ('timing_version_fixture_adapter1988_verified',)
OLD_LITERAL = '"ULike v1.9.86 / 新しい撮影の計測待ち"'
NEW_LITERAL = '"ULike v1.9.88 / 新しい撮影の計測待ち"'


def sha(value):
    return hashlib.sha256(value if isinstance(value, bytes) else Path(value).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec);spec.loader.exec_module(result)
    return result


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve();work.mkdir(parents=True, exist_ok=True)
    if source.is_file():source = source.parent
    for name, expected in PINS.items():
        if sha(source / name) != expected:
            raise AssertionError('Frozen timing fixture or runner changed: ' + name)
    production = source / 'ProcessingTiming1947.java'
    if len(re.findall(r'^    public static final String VERSION = "1\.9\.88";', production.read_text(), re.M)) != 1:
        raise AssertionError('The current production timing helper must declare exactly version 1.9.88')
    original_text = (source / 'Timing1982Test.java').read_text()
    if original_text.count('1.9.82') != 2:
        raise AssertionError('Only the two historical display version expectations may change')
    expected86 = original_text.replace('1.9.82', '1.9.86')
    expected88 = original_text.replace('1.9.82', '1.9.88')
    current_work = work / 'retained86'
    copied = current_work / 'fixtures/com/hiro/ulike/Timing1982Test.java'
    tracked = [source / name for name in PINS] + [production, Path(__file__).resolve()]
    before = {str(path): sha(path) for path in tracked}
    observed = []
    retained = module('timing88_retained86', source / 'host_timing1986.py');load = retained.module

    def load_with_current_version(name, requested):
        value = load(name, requested)
        if Path(requested).resolve() == source / 'host_timing1985.py':
            execute = value.run
            def run(command, log):
                if Path(log).resolve() == current_work / 'compile.log':
                    if observed or str(production) not in map(str, command) or str(copied) not in map(str, command):
                        raise AssertionError('Version fixture update must precede the one current-production timing compile')
                    body = copied.read_text()
                    if body != expected86 or body.count(OLD_LITERAL) != 2:
                        raise AssertionError('The .86-generated test differs beyond its two expected version strings')
                    updated = body.replace(OLD_LITERAL, NEW_LITERAL)
                    if updated != expected88 or updated.replace(NEW_LITERAL, OLD_LITERAL) != body:
                        raise AssertionError('A non-version test byte would change')
                    copied.write_text(updated)
                    observed.append(dict(replacement_count=2, before_sha256=sha(body.encode()),
                                         after_sha256=sha(updated.encode()),
                                         assertion_names=['initial_wait_state_is_visible_and_does_not_claim_gpu_use',
                                                          'expanded_snapshot_and_old_version_restore1982']))
                return execute(command, log)
            value.run = run
        return value

    retained.module = load_with_current_version
    result = retained.test(source, current_work, jdk=jdk, ndk=ndk)
    expected_cases = {'retained82', 'retained83', 'attempts1984', 'handoff1985', 'forecast_finish1986'}
    if len(observed) != 1 or set(result.get('cases', {})) != expected_cases:
        raise AssertionError('All five retained timing suites and one exact version adaptation must execute')
    if any(report.get('status') != 'passed' or report.get('assertions', 0) <= 0 for report in result['cases'].values()):
        raise AssertionError('A retained timing suite did not pass')
    if before != {str(path): sha(path) for path in tracked} or copied.read_text() != expected88:
        raise AssertionError('Historical timing input, current production or adapted fixture changed during execution')
    if result.get('production_source_sha256') != before[str(production)]:
        raise AssertionError('Timing evidence did not execute the current production helper')
    result.update(timing_version_fixture_adapter1987_verified=True,
                  timing_version_fixture_adapter1988_verified=True,
                  timing_version_fixture_changes1988=observed,
                  timing_version_fixture_input_sha2561988=before,
                  runner_source_sha256=sha(__file__))
    # The exact version-only adaptation invariant introduced in .87 is retained.
    result['fixture_adapter1988'] = ('Only the two explicit version strings in the generated copy of Timing1982Test change '
        'from ULike v1.9.86 to v1.9.88. Initial-state, stale-version rejection, GPU counts, timing, immutability, '
        'concurrency, queue/provider failures and all remaining historical assertions execute unchanged current production.')
    (work / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True);parser.add_argument('--work', required=True)
    parser.add_argument('--jdk');parser.add_argument('--ndk');args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
