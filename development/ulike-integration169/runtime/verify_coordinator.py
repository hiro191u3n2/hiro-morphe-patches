#!/usr/bin/env python3
"""Original coordinator sequencing tests and real SDK36/D8 compilation, no phone claim."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import tempfile

HERE = Path(__file__).resolve().parent
APP = HERE.parent
WORK = APP.parent
CORE = WORK / "hdr_rebuild167/core"
TOOLS = WORK / "models168/tools"
JDK = TOOLS / "jdk21/jdk-21.0.12.1+1/bin"
REPORT = APP / "QA_COORDINATOR.json"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(args):
    completed = subprocess.run(list(map(str, args)), capture_output=True, text=True, check=True,
                               env={**os.environ, "ORT_DISABLE_TELEMETRY": "1"})
    return completed.stdout.strip()


def production_sources():
    sources = []
    for path in CORE.rglob("*.java"):
        if "src" not in path.parts or any(x in path.parts for x in
                ("test", "tests", "review", "android_runtime_package", "qa")) or any(x.startswith("test-") for x in path.parts):
            continue
        if "native_geometry_contract" in path.parts:
            continue
        sources.append(path)
    # The separately published 1.8.1 trial classes link to the released
    # ModelLookup/ModelFiles helpers. They are not part of this disabled HDR
    # coordinator closure; their own verifier compiles against that baseline.
    trial_sources = {
        "com/hiro/ulike/TrialModelAccess181.java",
        "com/hiro/ulike/trial181/TrialDiagnostics181.java",
        "com/hiro/ulike/trial181/TrialNativeInputs181.java",
        "com/hiro/ulike/trial181/TrialTensorCheck181.java",
    }
    app_sources = [path for path in (HERE / "src").rglob("*.java")
                   if path.relative_to(HERE / "src").as_posix() not in trial_sources]
    return sorted(sources + app_sources)


def main():
    with tempfile.TemporaryDirectory(prefix="ulike-coordinator-qa-") as temporary:
        root = Path(temporary)
        tests = sorted((HERE / "test").glob("*.java"))
        host_sources = [CORE / "android_style_binding/src/com/hiro/ulike/binding/ShotStyleSettings.java"]
        host_sources += [HERE / "src/com/hiro/ulike/integration169" / name for name in
                         ("CapturedSettings169.java", "ProcessingSequence169.java", "PublicationOutcome169.java")]
        run([JDK / "javac", "--release", "8", "-d", root / "host", *host_sources, *tests])
        host = {}
        for cls in ("com.hiro.ulike.integration169.ProcessingSequence169Test", "com.hiro.ulike.binding.CapturedSettings169Test", "com.hiro.ulike.integration169.PublicationOutcome169Test"):
            host[cls] = run([JDK / "java", "-cp", root / "host", cls])
        sources = production_sources()
        before = {str(path.relative_to(WORK)): sha(path) for path in sources}
        ort = TOOLS / "onnxruntime-android-1.30.0-classes.jar"
        assert sha(ort) == "65e2e2d76d672253aaf0792a06c71cc40d09b0e2e251ccf925bee7175b91a1b0"
        optical_stub = APP / "compile_stubs/com/hiro/ulike/OpticalZoom.java"
        run([JDK / "javac", "--release", "8", "-cp", TOOLS / "android.jar", "-d", root / "stub", optical_stub])
        cp = os.pathsep.join(map(str, (TOOLS / "android.jar", ort, root / "stub")))
        run([JDK / "javac", "--release", "8", "-cp", cp, "-d", root / "classes", *sources])
        run([JDK / "jar", "cf", root / "helpers.jar", "-C", root / "classes", "."])
        (root / "dex").mkdir()
        run([JDK / "java", "-cp", TOOLS / "r8-8.3.37.jar", "com.android.tools.r8.D8", "--min-api", "26",
             "--lib", TOOLS / "android.jar", "--classpath", ort, "--classpath", root / "stub",
             "--output", root / "dex", root / "helpers.jar"])
        assert before == {str(path.relative_to(WORK)): sha(path) for path in sources}, "Sources changed during compilation"
        report = {
            "status": "PASS_HOST_SEQUENCE_AND_SDK36_D8_PRIVATE_DISABLED_COORDINATOR",
            "host_results": host,
            "host_checks": sum(int(line.rsplit("=", 1)[1]) for line in host.values()),
            "compiled_production_sources": len(sources),
            "compiled_dex_bytes": (root / "dex/classes.dex").stat().st_size,
            "compiled_dex_sha256": sha(root / "dex/classes.dex"),
            "android_device_execution": False,
            "processor_installed": False,
            "gate_enabled": False,
            "native_calibrated_plan_provider_implemented": False,
            "native_full_style_binding_provider_implemented": False,
            "native_replay_and_restore_barriers_complete": False,
            "publication_exception_commit_status": "Typed uncertainty has a separate terminal UI/callback route; in-flight save authority suppresses external unsaved failures; confirmed core receipt survives later Android URI conversion failure",
            "full_resolution_4080x3060_analysis": "Explicit AnalysisCapacity native-size candidate and Streaming ownership are connected; no resize or int[P] input staging, diagnostic consumed/closed before neural/HDR processing; device memory/geometry unverified",
            "excluded_unconnected_app_modules": ["native_geometry_contract (standalone diagnostic parser, no application provider connection)"],
            "host_scope": ["ordered stages", "failure at every stage", "exception and linkage error cleanup",
                           "post-commit cleanup distinction", "exact identity cancellation", "20 concurrent admission/cancel races",
                           "interrupt preservation", "canonical actual request snapshot serialization",
                           "publication save authority and 100 terminal outcome races",
                           "blocked save suppresses 200 external failure attempts across eventual commit/uncertainty"],
            "not_tested_by_this_report": ["Android camera/native SDK/ORT/MediaCodec/MediaStore execution", "visual native style parity", "all lenses/modes"],
            "source_sha256": before,
            "test_sha256": {str(p.relative_to(APP)): sha(p) for p in tests + [Path(__file__)]},
            "tool_sha256": {str(p.relative_to(WORK)): sha(p) for p in (TOOLS / "android.jar", TOOLS / "r8-8.3.37.jar", ort)},
        }
        REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
        print(json.dumps({"report": str(REPORT), "host_checks": report["host_checks"],
                          "compiled_sources": len(sources), "dex_bytes": report["compiled_dex_bytes"]}))


if __name__ == "__main__":
    main()
