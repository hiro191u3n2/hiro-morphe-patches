#!/usr/bin/env python3
"""Verify a pinned retained runtime repair against actual .62 dex, including serialization."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import zipfile


def test(root, work, jdk=None, ndk=None):
    root, work = Path(root).resolve(), Path(work).resolve()
    folder = work / 'ui-blacktap1963'
    baseline, classes = folder / 'baseline', folder / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    package = Path(os.environ.get('ULIKE1963_BASELINE_MPP',
        str(root.parents[3] / 'build-env/input/ULike_HQ_Texture_Online_v1.9.62.mpp')))
    morphe = Path(os.environ.get('ULIKE1963_MORPHE_JAR',
        str(root.parents[3] / 'build-env/tools/morphe.jar')))
    with zipfile.ZipFile(package) as archive:
        for name in archive.namelist():
            if name.endswith('.class') or name == 'ulike/runtime.dex':
                path = baseline / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(archive.read(name))
    classpath = str(morphe) + os.pathsep + str(baseline)
    java = str(Path(jdk) / 'bin/java') if jdk else 'java'
    javac = str(Path(jdk) / 'bin/javac') if jdk else 'javac'
    result = subprocess.run([javac, '-cp', classpath, '-d', str(classes),
        str(root / 'MergePayloads.java'), str(root / 'TransformBlackTap1963.java'),
        str(root / 'tests1963/UiBlackTap1963Test.java')], capture_output=True, text=True, timeout=120)
    (folder / 'compile.log').write_text(result.stdout + result.stderr)
    if result.returncode:
        raise RuntimeError(result.stdout + result.stderr)
    result = subprocess.run([java, '-XX:ActiveProcessorCount=4', '-cp',
        str(classes) + os.pathsep + classpath, 'UiBlackTap1963Test',
        str(baseline / 'ulike/runtime.dex'), str(folder / 'patched-blacktap.dex')],
        capture_output=True, text=True, timeout=120)
    (folder / 'run.log').write_text(result.stdout + result.stderr)
    if result.returncode:
        raise RuntimeError(result.stdout + result.stderr)
    print(result.stdout, end='')
    report = {'status': 'passed', 'assertions': int(result.stdout.strip().split('=')[-1]),
              'physical_android_tested': False,
              'scope': 'actual published .62 dex; normalized prefix/class preservation; emitted dex serialization',
              'changed_retained_runtime_methods': [
                  'Lcom/hiro/ulike/BlackTap1925;->doubleTap(Lcom/bytedance/corecamera/ui/view/GestureBgLayout;Landroid/view/MotionEvent;)Z']}
    (folder / 'result.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    args = parser.parse_args()
    print(json.dumps(test(args.root, args.work, args.jdk), indent=2))
