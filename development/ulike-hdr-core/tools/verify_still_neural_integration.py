#!/usr/bin/env python3
"""Connect owned P010/color/real-model/row interfaces on the host, not an Android app."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

os.environ['ORT_DISABLE_TELEMETRY'] = '1'


def run(argv):
    result = subprocess.run(list(map(str, argv)), capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(result.stdout + '\n' + result.stderr)
    return result.stdout.strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('jdk-bin', 'android-jar', 'ort-android-classes', 'ort-host-jar',
                 'baoman', 'goodlike', 'bytenn', 'effect', 'natural-zip', 'purity-zip', 'report'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    sources = []
    for directory in ('hdr_input/src/main/java', 'face_analysis/src/main/java',
                      'android_hdr_color/src/main/java', 'android_model_runtime/src',
                      'android_beauty_image/src', 'qa/src'):
        sources.extend(sorted((root / directory).rglob('*.java')))
    digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    before = {str(path.relative_to(root)): digest(path) for path in sources}
    with tempfile.TemporaryDirectory(prefix='still-neural-integration-') as classes:
        run([args.jdk_bin / 'javac', '--release', '8', '-cp',
             os.pathsep.join(map(str, (args.android_jar, args.ort_android_classes))),
             '-d', classes, *sources])
        output = run([args.jdk_bin / 'java', '-Xmx384m', '-cp',
                      os.pathsep.join(map(str, (classes, args.ort_host_jar))),
                      'com.hiro.ulike.hdr.input.StillNeuralIntegration',
                      args.baoman, args.goodlike, args.bytenn, args.effect,
                      args.natural_zip, args.purity_zip])
    for path in sources:
        if digest(path) != before[str(path.relative_to(root))]:
            raise RuntimeError('Source changed during integration verification')
    report = {
        'status': 'PASS_HOST_P010_TO_SDR_REAL_NEURAL_COMPONENT', 'output': output,
        'source_sha256': before, 'verifier_sha256': digest(Path(__file__)),
        'dependencies_sha256': {p.name: digest(p) for p in (
            args.android_jar, args.ort_android_classes, args.ort_host_jar)},
        'runtime_telemetry_opt_out': True, 'android_device_execution': False,
        'face_detector_used': False, 'source': 'synthetic owned 512x384 P010 frame',
        'geometry': 'explicit synthetic affine', 'processed_hdr_output': False,
        'complete_style': False, 'camera_or_save_integration': False,
        'limitations': 'Input HDR retained; neural output explicitly SDR. Real interfaces and actual pinned models are exercised, not a production camera-to-HDR pipeline.',
    }
    args.report.write_text(json.dumps(report, indent=2) + '\n')
    print(output)


if __name__ == '__main__':
    main()
