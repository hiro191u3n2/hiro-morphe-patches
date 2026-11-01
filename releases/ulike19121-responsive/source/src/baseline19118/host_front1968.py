"""Run production front readiness against deterministic native mode lifecycle fixtures.

These host checks establish ordering/ownership and bounds, not real device output.
"""
from pathlib import Path
import json
import shutil
import subprocess


def test(root, work, jdk=None):
    root, work = Path(root).resolve(), Path(work).resolve()
    javac = str(Path(jdk).resolve() / 'bin/javac') if jdk else 'javac'
    java = str(Path(jdk).resolve() / 'bin/java') if jdk else 'java'
    folder = work / 'front1968-tests'
    src, classes = folder / 'src', folder / 'classes'
    src.mkdir(parents=True, exist_ok=True)
    classes.mkdir(parents=True, exist_ok=True)
    for fixtures in ('front1936-host', 'front1968-host'):
        for source in (root / fixtures).rglob('*.java'):
            destination = src / source.relative_to(root / fixtures)
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, destination)
    for name in ('PreviewStart1927', 'PreviewInputs1929', 'ProviderLifecycle1929', 'FrontPreview1931'):
        shutil.copyfile(root / 'front1936-baseline' / (name + '.java'), src / 'com/hiro/ulike' / (name + '.java'))
    for name in ('FrontPreview1936', 'Scheduling1944', 'SpeedWorkers1935'):
        shutil.copyfile(root / (name + '.java'), src / 'com/hiro/ulike' / (name + '.java'))
    result = subprocess.run([javac, '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-d', str(classes),
                             *map(str, sorted(src.rglob('*.java')))], capture_output=True, text=True, timeout=120)
    (folder / 'javac.log').write_text(result.stdout + result.stderr)
    result.check_returncode()
    counts = {}
    for main in ('FrontTest1936', 'FrontModeReady1968Test'):
        result = subprocess.run([java, '-cp', str(classes), 'com.hiro.ulike.' + main], capture_output=True, text=True, timeout=60)
        (folder / (main + '.log')).write_text(result.stdout + result.stderr)
        result.check_returncode()
        counts[main] = int(result.stdout.strip().split('=')[-1])
    report = {'status': 'passed', 'assertions': sum(counts.values()),
              'groups': {name: {'status': 'passed', 'assertions': count} for name, count in counts.items()},
              'scope': 'actual production front helper; native mode readiness, ownership and bounded lifecycle fixtures',
              'physical_android_tested': False}
    (work / 'host-front1968.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    args = parser.parse_args()
    print(json.dumps(test(Path(__file__).resolve().parent, args.work, jdk=args.jdk), indent=2))
