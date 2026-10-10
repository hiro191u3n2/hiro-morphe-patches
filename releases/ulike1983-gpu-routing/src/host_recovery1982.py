#!/usr/bin/env python3
"""Execute every retained recovery case with the current cache-count label.

The original runner and fixture remain unchanged. Only the current-service
javac invocation receives a generated fixture whose one display literal has
changed; the real .76 service and its behavioral negative controls still use
the original fixture. All persistence, count, admission and retry assertions
are executed. Legacy setup assertions are disclosed separately from current
production assertions.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json


ROOT = Path(__file__).resolve().parent
OLD_LABEL = '"個別拒否（直近照会）64件"'
NEW_LABEL = '"画質不一致の拒否64件"'
SETUP_SECTION = "legacy_1976_service_generates_real_overflow_and_signed_proofs"


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    original = source / "host_recovery1977.py"
    retained_fixture = source / "recovery77-fixtures/Recovery1977Test.java"
    legacy_service = source / "recovery77-fixtures/reference76/GpuQualification1961.java"
    production = source / "GpuQualification1961.java"
    stub_provider = source / "host_qualification1967.py"
    body = retained_fixture.read_text(encoding="utf-8")
    if body.count(OLD_LABEL) != 1:
        raise AssertionError("Retained recovery cache-count display expectation changed")
    generated = work / "diagnostic-fixture/Recovery1977Test.java"
    generated.parent.mkdir(parents=True, exist_ok=True)
    generated.write_text(body.replace(OLD_LABEL, NEW_LABEL), encoding="utf-8")
    if generated.read_text(encoding="utf-8").replace(NEW_LABEL, OLD_LABEL) != body:
        raise AssertionError("Generated recovery fixture differs beyond one display literal")
    inputs = (original, retained_fixture, generated, legacy_service, production,
              stub_provider, Path(__file__).resolve())
    pins = {str(path): sha(path) for path in inputs}

    spec = importlib.util.spec_from_file_location("recovery1982_retained1977", original)
    retained = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(retained)
    original_subprocess = retained.subprocess
    substitutions, legacy_compiles = [], []

    class SubprocessAdapter:
        def __getattr__(self, name):
            return getattr(original_subprocess, name)

        def run(self, command, *args, **kwargs):
            command = list(command)
            values = list(map(str, command))
            matches = [i for i, item in enumerate(values) if item == str(retained_fixture)]
            if matches:
                if len(matches) != 1 or Path(values[0]).name != "javac":
                    raise AssertionError("Unexpected recovery fixture invocation")
                if str(production) in values and str(legacy_service) not in values:
                    command[matches[0]] = str(generated)
                    substitutions.append({"service": "current", "from": str(retained_fixture),
                                          "to": str(generated)})
                elif str(legacy_service) in values and str(production) not in values:
                    legacy_compiles.append(str(retained_fixture))
                else:
                    raise AssertionError("Recovery compilation does not select one real qualification service")
            return original_subprocess.run(command, *args, **kwargs)

    # This replaces only the imported runner's module reference, never the
    # shared Python subprocess module or any other evidence group's tools.
    retained.subprocess = SubprocessAdapter()
    result = retained.test(source, work, jdk=jdk, ndk=ndk)
    if len(substitutions) != 1 or legacy_compiles != [str(retained_fixture)]:
        raise AssertionError("Both current-adapted and original-legacy compilations must execute")
    if pins != {name: sha(name) for name in pins}:
        raise AssertionError("Recovery production, original fixtures or adapter changed during execution")
    if result.get("production_source_sha256") != pins[str(production)]:
        raise AssertionError("Retained runner did not execute the pinned production service")
    setup = result["tests"][SETUP_SECTION]
    if setup.get("status") != "passed" or setup.get("assertions") != 5:
        raise AssertionError("Original .76 persistence seed assertions did not execute")
    retained_total = result["assertions"]
    current_assertions = sum(evidence["assertions"] for name, evidence in result["tests"].items()
                             if name != SETUP_SECTION)
    if retained_total != current_assertions + setup["assertions"] or current_assertions <= 0:
        raise AssertionError("Recovery current/setup assertion accounting differs")
    if (result.get("recovery_individual_exact_failures") != 655
            or result.get("hot_failure_cache_limit") != 64
            or len(result.get("negative_controls", [])) != 2):
        raise AssertionError("Retained recovery count and negative-control coverage differs")

    result.update(
        assertions=current_assertions,
        current_assertions=current_assertions,
        current_counts_only=True,
        legacy_setup_assertions=setup["assertions"],
        retained_assertions_including_legacy_setup=retained_total,
        legacy_setup_sections=[SETUP_SECTION],
        diagnostic_recovery_label_adapter1982_verified=True,
        inherited_recovery_assertions_preserved=True,
        diagnostic_fixture_substitutions=substitutions,
        diagnostic_display_expectations1982={OLD_LABEL: NEW_LABEL},
        diagnostic_adapter_source_sha256=pins,
        source_sha256=pins,
        retained_runner_source_sha256=pins[str(original)],
        retained_test_source_sha256=pins[str(retained_fixture)],
        legacy_test_source_sha256=pins[str(retained_fixture)],
        test_source_sha256=pins[str(generated)],
        runner_source_sha256=pins[str(Path(__file__).resolve())],
    )
    (work / "recovery-host-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", default=str(ROOT))
    parser.add_argument("--work", required=True)
    parser.add_argument("--jdk")
    parser.add_argument("--ndk")
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
