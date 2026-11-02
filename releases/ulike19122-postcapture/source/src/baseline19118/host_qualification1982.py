#!/usr/bin/env python3
"""Retain every .81 queue assertion with two updated diagnostic label checks.

Only host fixture strings change. Production source, scheduling, cancellation,
fairness, retention, persistence and retry assertions run through the original
.81 runner. The historical runner and fixture bytes remain untouched.
"""
from pathlib import Path
import hashlib
import importlib.util
import json

ROOT = Path(__file__).resolve().parent


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    original = source / "host_qualification1981.py"
    spec = importlib.util.spec_from_file_location("qualification1982_retained81", original)
    retained = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(retained)
    fixtures = dict(retained.FIXTURES)
    name = "com/hiro/ulike/QueueHost1967.java"
    body = fixtures[name]
    for old, new in ((".contains(\"待ち2件\")", ".contains(\"予約済み2件\")"),
                     (".contains(\"待ち7件\")", ".contains(\"予約済み7件\")")):
        if body.count(old) != 1:
            raise AssertionError("Retained queue label assertion changed: " + old)
        body = body.replace(old, new)
    fixtures[name] = body
    retained.FIXTURES = fixtures
    original_sha = hashlib.sha256(original.read_bytes()).hexdigest()
    result = retained.test(source, work, jdk=jdk)
    if hashlib.sha256(original.read_bytes()).hexdigest() != original_sha:
        raise AssertionError("Historical queue runner changed during execution")
    result.update(diagnostic_queue_label_adapter1982_verified=True,
                  retained_runner_source_sha256=original_sha,
                  adapted_fixture_source_sha256=hashlib.sha256(body.encode()).hexdigest(),
                  runner_source_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                  adapter_source_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest())
    (work / "qualification-host-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    return result


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True)
    parser.add_argument("--work", required=True)
    parser.add_argument("--jdk")
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk), ensure_ascii=False, indent=2))
