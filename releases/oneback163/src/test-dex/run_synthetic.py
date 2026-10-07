#!/usr/bin/env python3
"""Run real Morphe DEX+resource integration on original synthetic fixtures only."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import time

PACKAGES = (
    "com.ss.android.ugc.trill",
    "com.zhiliaoapp.musically",
    "com.instagram.android",
    "com.twitter.android",
    "ctrip.english",
)
PATCH_NAME = "戻る1回で終了・履歴削除"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", required=True, type=Path)
    parser.add_argument("--morphe", required=True, type=Path)
    parser.add_argument("--build", required=True, type=Path)
    parser.add_argument("--dist", required=True, type=Path)
    parser.add_argument("--fixtures", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--report", required=True, type=Path)
    args = parser.parse_args()
    for name in ("java", "morphe", "build", "dist", "fixtures", "out", "report"):
        setattr(args, name, getattr(args, name).resolve())
    args.out.mkdir(parents=True, exist_ok=True)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    classpath = ":".join(str(p) for p in (args.build / "auditclasses", args.build / "patchclasses", args.morphe))
    java = [str(args.java), "-XX:-UsePerfData", "-Xmx768m"]
    bundles = {
        "standalone": args.dist / "OneBack_Exit_NoRecents_v1.0.0.mpp",
        "combined": args.dist / "Hiro_Morphe_Patches_v1.0.163.mpp",
    }
    source = Path(__file__).resolve().parent
    result = {
        "schema": "oneback163-synthetic-integration-v1",
        "result": "IN_PROGRESS",
        "blocking_findings": [],
        "input_kind": "original synthetic fixtures; no third-party app code/resources",
        "physical_device_tested": False,
        "packages": list(PACKAGES),
        "expected_runs": len(PACKAGES) * len(bundles),
        "bundle_sha256": {key: sha256(value) for key, value in bundles.items()},
        "audit_source_sha256": {name: sha256(source / name) for name in ("DexAudit.java", "Fixtures.java", "run_synthetic.py")},
        "runs": [],
    }

    def save() -> None:
        args.report.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")

    def execute(command: list[str], log: Path) -> None:
        with log.open("w") as out:
            process = subprocess.run(command, stdout=out, stderr=subprocess.STDOUT, check=False)
        if process.returncode:
            raise RuntimeError(f"Command failed ({process.returncode}): {log}")

    save()
    try:
        for variant, bundle in bundles.items():
            folder = args.out / variant
            folder.mkdir(exist_ok=True)
            for package in PACKAGES:
                started = time.monotonic()
                original = args.fixtures / f"{package}.apk"
                patched = folder / f"{package}.apk"
                morphe_report = folder / f"{package}.morphe.json"
                manifest_report = folder / f"{package}.manifest-qa.json"
                dex_report = folder / f"{package}.dex-qa.json"
                print(f"RUN {variant}: {package}", flush=True)
                execute(java + ["-jar", str(args.morphe), "patch", str(original), "-p", str(bundle),
                        "--exclusive", "-e", PATCH_NAME, "--unsigned", "--bytecode-mode", "FULL",
                        "--striplibs", "arm64-v8a", "-o", str(patched), "-t", str(folder / f"{package}.tmp"),
                        "-r", str(morphe_report)], folder / f"{package}.morphe.log")
                # Morphe can return exit code 0 after reporting an internal failure.
                report = json.loads(morphe_report.read_text())
                if not patched.is_file() or report.get("failedPatches") or not report.get("patchingSteps"):
                    raise RuntimeError(f"Morphe did not finish {variant}/{package}: {report}")
                if not all(step.get("success") is True for step in report["patchingSteps"]):
                    raise RuntimeError(f"Morphe patch/rebuild failure for {variant}/{package}: {report}")
                if report.get("appliedPatches") != [{"name": PATCH_NAME}]:
                    raise RuntimeError(f"Unexpected selected patches for {variant}/{package}: {report}")
                execute(java + ["-cp", classpath, "Fixtures", "verify", str(original), str(patched), str(manifest_report)], folder / f"{package}.manifest-qa.log")
                execute(java + ["-cp", classpath, "DexAudit", "audit-app", str(original), str(patched), str(dex_report)], folder / f"{package}.dex-qa.log")
                manifest_qa = json.loads(manifest_report.read_text())
                dex_qa = json.loads(dex_report.read_text())
                for qa in (manifest_qa, dex_qa):
                    if qa.get("result") != "PASS" or qa.get("blocking_findings") != []:
                        raise RuntimeError(f"Independent audit failure for {variant}/{package}: {qa}")
                result["runs"].append({
                    "variant": variant, "package": package, "version": "999.0", "result": "PASS",
                    "input_sha256": sha256(original), "output_sha256": sha256(patched),
                    "morphe": report, "manifest_resources": manifest_qa, "dex": dex_qa,
                    "seconds": round(time.monotonic() - started, 3),
                })
                save()
                print(f"PASS {variant}: {package}; DEX assertions={dex_qa['assertions']}", flush=True)
        result["result"] = "PASS"
        result["completed_runs"] = len(result["runs"])
        result["assertions"] = sum(run["manifest_resources"]["assertions"] + run["dex"]["assertions"] for run in result["runs"])
        save()
        print(f"PASS all {len(result['runs'])} synthetic package/bundle combinations", flush=True)
    except BaseException as failure:
        result["result"] = "FAIL"
        result["blocking_findings"].append(str(failure))
        save()
        raise


if __name__ == "__main__":
    main()
