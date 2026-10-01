#!/usr/bin/env python3
"""Compile the staged Java components together; does not build or run ULike."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
import zipfile

SOURCE_DIRS = (
    'hdr_input/src/main/java', 'hdr_input/src/android/java',
    'android_capture/src/main/java', 'android_capture/src/android/java',
    'android_encoder/src/main/java', 'android_model_runtime/src',
    'face_analysis/src/main/java', 'android_heif/src/main/java',
)


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(argv):
    process = subprocess.run([str(arg) for arg in argv], capture_output=True, text=True)
    if process.returncode:
        raise RuntimeError(process.stderr[-6000:])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('jdk-bin', 'android-jar', 'ort-android-classes', 'r8', 'report'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    sources = []
    for directory in SOURCE_DIRS:
        found = sorted((root / directory).rglob('*.java'))
        if not found:
            raise ValueError('Missing staged source directory: ' + directory)
        sources.extend(found)
    sources = sorted(set(sources))
    before = {str(path.relative_to(root)): digest(path) for path in sources}
    with tempfile.TemporaryDirectory(prefix='ulike-staged-android-') as scratch:
        scratch = Path(scratch)
        classes, dex = scratch / 'classes', scratch / 'dex'
        classes.mkdir()
        dex.mkdir()
        import os
        classpath = os.pathsep.join(map(str, (args.android_jar, args.ort_android_classes)))
        run([args.jdk_bin / 'javac', '--release', '8', '-classpath', classpath,
             '-d', classes] + sources)
        jar = scratch / 'staged-components.jar'
        with zipfile.ZipFile(jar, 'w') as archive:
            for path in sorted(classes.rglob('*.class')):
                archive.write(path, str(path.relative_to(classes)))
        run([args.jdk_bin / 'java', '-cp', args.r8, 'com.android.tools.r8.D8',
             '--lib', args.android_jar, '--lib', args.ort_android_classes,
             '--min-api', '26', '--output', dex, jar])
        dex_files = {path.name: path.stat().st_size for path in dex.glob('*.dex')}
        if not dex_files:
            raise RuntimeError('D8 produced no DEX')
        for path in sources:
            if digest(path) != before[str(path.relative_to(root))]:
                raise RuntimeError('Source changed during compilation')
        report = {
            'status': 'PASS_COMBINED_ANDROID_SOURCE_COMPILE_AND_D8',
            'source_files': len(sources), 'source_sha256': before,
            'dependencies_sha256': {
                'android.jar': digest(args.android_jar),
                'ort_android_classes.jar': digest(args.ort_android_classes),
                'r8.jar': digest(args.r8),
            },
            'd8_min_api': 26, 'dex_files': dex_files,
            'runtime_execution': False,
            'ulike_app_manifest_jni_packaging_or_hooks_verified': False,
            'scope': 'Staged Java production sources compile together; not an app build, device run or end-to-end image pipeline',
            'verifier_sha256': digest(Path(__file__)),
        }
        args.report.write_text(json.dumps(report, indent=2) + '\n')
        print(json.dumps({'status': report['status'], 'sources': len(sources), 'dex_files': dex_files}))


if __name__ == '__main__':
    main()
