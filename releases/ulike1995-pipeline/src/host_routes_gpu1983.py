#!/usr/bin/env python3
"""Run every retained route assertion with .82 diagnostics and .83 transfer declarations.

The native-route javac invocation substitutes a declared host timing fixture;
the whole production javac invocation adds the new diagnostics helper missing
from the retained .81 build closure. No production source, test assertion,
shader, compiler option or runtime invocation changes. The added source, both
fixture versions and this adapter are hashed alongside the unchanged runner's
complete source evidence.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json

ROOT = Path(__file__).resolve().parent


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def adapt_memory_transport1983(body):
    """Preserve every old plan assertion and controlled outcome; declare only new APIs.

    Actual per-slot transfer budgets execute separately in host_memory1983.
    This fixture must continue to count the same plans/leases and inject the
    original budget failures into the production preflight, with no pixel I/O.
    """
    marker = '    static boolean planFits1981(long[] capacities,long javaBytes)'
    addition = '    static boolean planFits1983(long[] capacities,long[] uploads,long javaBytes){if(uploads==null||uploads.length!=capacities.length)throw new AssertionError("upload extent");for(int i=0;i<uploads.length;i++)if(uploads[i]<0||uploads[i]>capacities[i])throw new AssertionError("upload extent");return planFits1981(capacities,javaBytes);}\n'
    if body.count(marker) != 1:
        raise AssertionError('Retained preflight fixture signature changed')
    body = body.replace(marker, addition + marker)
    marker = '        Lease1971 reserveCapacity1971(int[] s,long[] n,long j)'
    addition = '        Lease1971 reserveCapacity1983(int[] s,long[] n,long[] u,long j){if(u==null||u.length!=n.length)throw new AssertionError("upload extent");for(int i=0;i<u.length;i++)if(u[i]<0||u[i]>n[i])throw new AssertionError("upload extent");return reserveCapacity1971(s,n,j);}\n'
    if body.count(marker) != 1:
        raise AssertionError('Retained lease fixture signature changed')
    return body.replace(marker, addition + marker)


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    original = source / "host_routes_gpu1981.py"
    old_transport = source / "tests1981/GpuPlanTransport1981.java"
    transport = work / "transfer-fixture83/GpuPlanTransport1981.java"
    transport.parent.mkdir(parents=True, exist_ok=True)
    transport.write_text(adapt_memory_transport1983(old_transport.read_text()))
    old_fixture = source / "tests1981/GpuRouteNativeFixtures1981.java"
    new_fixture = source / "tests1982/strong/GpuRouteNativeFixtures1982.java"
    diagnostic_helper = source / "PipelineDiagnostics1982.java"
    addition = "    static synchronized void strongGpuLockWait1982(Trace t,long nanos){if(t!=trace||nanos<0)throw new AssertionError(\"invalid additive lock timing\");}\n"
    marker = "    static synchronized void strongGpuWork1973(Trace t,long cpu,long gpu,long wait,int reuse){}\n"
    old_body, new_body = old_fixture.read_text(), new_fixture.read_text()
    if old_body.count(marker) != 1 or old_body.replace(marker, marker + addition) != new_body:
        raise AssertionError("Additive host timing fixture differs beyond the one declared API")
    pins = {str(p): sha(p) for p in (original, old_fixture, new_fixture,
                                    diagnostic_helper, old_transport, transport, Path(__file__))}
    spec = importlib.util.spec_from_file_location("routes1982_retained81", original)
    retained = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(retained)
    original_run = retained.run
    substitutions = []
    production_additions = []
    transfer_substitutions = []

    def adapted_run(command, directory, label, timeout=240, env=None):
        command = list(command)
        matches = [i for i, item in enumerate(command) if str(item) == str(old_fixture)]
        if matches:
            if label != "route-compile" or len(matches) != 1:
                raise AssertionError("Unexpected fixture substitution outside native route compile")
            command[matches[0]] = new_fixture
            substitutions.append(label)
        transfer_matches = [i for i, item in enumerate(command) if str(item) == str(old_transport)]
        if transfer_matches:
            if label != "fixture-compile" or len(transfer_matches) != 1:
                raise AssertionError("Unexpected transfer peer outside the plan fixture compile")
            command[transfer_matches[0]] = transport
            transfer_substitutions.append(label)
        if label == "production-compile":
            if str(diagnostic_helper) in map(str, command):
                raise AssertionError("Retained .81 closure unexpectedly already includes .82 diagnostics")
            command.append(diagnostic_helper)
            production_additions.append(str(diagnostic_helper))
        return original_run(command, directory, label, timeout=timeout, env=env)

    retained.run = adapted_run
    result = retained.test(source, work, jdk=jdk, ndk=ndk)
    if transfer_substitutions != ["fixture-compile"]:
        raise AssertionError("The complete retained plan suite must use the additive transfer fixture")
    if substitutions != ["route-compile"]:
        raise AssertionError("The unchanged native route regression did not compile with its additive API fixture")
    if production_additions != [str(diagnostic_helper)]:
        raise AssertionError("The unchanged plan regression did not compile with its additive diagnostics helper")
    if pins != {name: sha(name) for name in pins}:
        raise AssertionError("Route fixture/runner changed during execution")
    result.update(upload_budget_fixture_adapter1983_verified=True,
                  additive_timing_fixture_adapter1982_verified=True,
                  additive_diagnostic_helper_adapter1982_verified=True,
                  diagnostic_adapter_source_sha256=pins,
                  diagnostic_fixture_substitutions=substitutions,
                  diagnostic_production_source_additions=production_additions,
                  retained_runner_source_sha256=sha(original),
                  runner_source_sha256=sha(__file__))
    result['plan']['source_sha256'][str(diagnostic_helper)] = pins[str(diagnostic_helper)]
    result['plan']['diagnostic_production_source_additions'] = production_additions
    result['source_sha256'].update(pins)
    (work / "plan/result.json").write_text(json.dumps(result['plan'], ensure_ascii=False, indent=2) + "\n")
    (work / "result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", default=str(ROOT))
    parser.add_argument("--work", required=True)
    parser.add_argument("--jdk")
    parser.add_argument("--ndk")
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
