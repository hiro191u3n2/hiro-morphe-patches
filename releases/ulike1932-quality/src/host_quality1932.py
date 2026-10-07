#!/usr/bin/env python3
"""Aggregate build gate for ULike 1.9.32 quality and ownership host tests.

test(ROOT, work) uses java/javac from PATH, writes complete human-readable logs,
and returns deterministic evidence without timings, timestamps or machine paths.
The Android runtime, camera HAL and physical image quality are outside this gate.
"""
import argparse
import json
from pathlib import Path
import re
import subprocess
import sys


PIPELINE_SCENARIOS = {
    "source_NR_before_enlarge_final_sharp",
    "large_input_downsample_before_NR",
    "periodic_chroma_before_resize",
    "exact_rotation_and_output_tags",
    "fractional_center_crop_with_rotation",
    "OFF_and_identity_completion_markers",
    "legacy_snapshot_and_nested_context_restore",
    "concurrent_state_plan_isolation",
    "parallel_strips_match_immutable_whole_frame",
    "non_sRGB_hardware_gainmap_fallback",
    "local_beauty_shadow_budget",
    "halo_setting_reaches_final_sharpen",
    "interrupted_worker_cleanup",
    "failure_cleanup_create",
    "failure_cleanup_write",
    "failure_cleanup_density",
    "failure_cleanup_copy",
    "failure_cleanup_copy_metadata",
    "failure_cleanup_final_metadata",
    "failure_cleanup_worker_write",
    "failure_cleanup_nr",
    "null_and_recycled_fallback",
}


def _require(condition, description):
    if not condition:
        raise RuntimeError(description)


def _run(arguments, log, append=False):
    result = subprocess.run([str(arg) for arg in arguments], text=True,
                            capture_output=True, timeout=120)
    mode = "a" if append else "w"
    with Path(log).open(mode, encoding="utf-8") as stream:
        stream.write(result.stdout)
        stream.write(result.stderr)
    _require(result.returncode == 0,
             "Host test command failed; inspect " + Path(log).name + ": " + result.stderr[-3000:])
    return result.stdout


def test(ROOT, work):
    root = Path(ROOT).resolve()
    work = Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    generated = work / "host-quality-1932"
    kernel_classes = generated / "kernel-classes"
    kernel_classes.mkdir(parents=True, exist_ok=True)

    # Compile the same kernel source that the Android/D8 stage packages. Only the
    # test main and its generated .class files are host-only.
    # Raw process output can include elapsed times and compiler fixture paths.
    # Keep it exclusively in generated build state, never in the published logs.
    kernel_log = generated / "kernel-debug.log"
    _run(["javac", "-source", "8", "-target", "8", "-Xlint:-options", "-d", kernel_classes,
          root / "QualityPixels1932.java", root / "tests/QualityPixels1932Test.java"], kernel_log)
    kernel_stdout = _run(["java", "-cp", kernel_classes, "com.hiro.ulike.QualityPixels1932Test"],
                         kernel_log, append=True)
    kernel = json.loads(kernel_stdout)
    _require(kernel.get("status") == "passed" and int(kernel.get("assertions", 0)) > 0,
             "Actual kernel tests did not report successful assertions")
    kernel_metrics = {name: value for name, value in sorted(kernel.get("metrics", {}).items())
                      if name != "host_test_seconds"}

    # This runner generates independent pixel-capable Bitmap fixtures and exercises
    # the production Pipeline source. Legacy NR and camera metadata are collaborators
    # with explicit host fixtures, while all new pixel operations remain production.
    pipeline_out = generated / "pipeline"
    _run([sys.executable, root / "host_pipeline1932.py", "--javac", "javac", "--java", "java",
          "--out", pipeline_out], generated / "pipeline-debug.log")
    pipeline = json.loads((pipeline_out / "host-pipeline1932.json").read_text(encoding="utf-8"))
    scenarios = pipeline.get("scenarios", {})
    _require(pipeline.get("status") == "passed" and int(pipeline.get("assertions", 0)) > 0,
             "Production pipeline host assertions did not pass")
    _require(PIPELINE_SCENARIOS <= set(scenarios) and all(value == "passed" for value in scenarios.values()),
             "Required pipeline ownership/order/concurrency scenario missing or failed")
    _require(pipeline.get("physical_device_verified") is False,
             "Host pipeline evidence must not claim physical device verification")

    # The independently owned capture-context runner generates its own fixtures,
    # exercising the actual metadata helper without sharing pipeline test doubles.
    context_out = generated / "context"
    context_stdout = _run([sys.executable, root / "host_shot_context1932.py", "--javac", "javac",
                           "--java", "java", "--out", context_out],
                          generated / "context-debug.log")
    context = json.loads((context_out / "host-shot-context1932.json").read_text(encoding="utf-8"))
    match = re.search(r"SHOT_CONTEXT_1932_PASS checks=(\d+) interleaved_captures=(\d+)", context_stdout)
    _require(context.get("status") == "passed" and match is not None and int(match.group(1)) > 0,
             "Exact-shot metadata host assertions did not pass")
    _require(int(match.group(2)) == 400 and context.get("physical_device_verified") is False,
             "Missing concurrent shot isolation check or incorrect physical-device claim")

    suites = {
        "pixel_kernel": {
            "status": "passed",
            "assertions": int(kernel["assertions"]),
            "deterministic_pixel_metrics": kernel_metrics,
        },
        "pipeline": {
            "status": "passed",
            "assertions": int(pipeline["assertions"]),
            "scenarios": dict(sorted(scenarios.items())),
        },
        "shot_context": {
            "status": "passed",
            "assertions": int(match.group(1)),
            "interleaved_captures": int(match.group(2)),
        },
    }
    # finalize1932 includes these three top-level .txt files in the source ZIP.
    # Publish only explicitly selected, parsed evidence; never copy raw stdout or
    # unknown fields that might contain timing, host paths, or machine details.
    published_scopes = {
        "pixel_kernel": "Production pixel kernels executed on the host",
        "pipeline": "Production pipeline with pixel-capable Android host fixtures and explicit legacy collaborators",
        "shot_context": "Production shot metadata helper with independent camera callback host fixtures",
    }
    published_names = {
        "pixel_kernel": "host-quality-kernel.txt",
        "pipeline": "host-quality-pipeline.txt",
        "shot_context": "host-quality-context.txt",
    }
    for name, suite in suites.items():
        published = {
            "schema": "ulike1932-host-suite-v1",
            "suite": name,
            "scope": published_scopes[name],
            "physical_device_verified": False,
            **suite,
        }
        (work / published_names[name]).write_text(
            json.dumps(published, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
            encoding="utf-8")
    return {
        "schema": "ulike1932-host-quality-v1",
        "status": "passed",
        "assertions": sum(suite["assertions"] for suite in suites.values()),
        "physical_device_verified": False,
        "suites": suites,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument("--work", type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(test(args.root, args.work), indent=2))


if __name__ == "__main__":
    main()
