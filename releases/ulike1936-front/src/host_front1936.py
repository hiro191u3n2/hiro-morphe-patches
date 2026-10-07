"""Independent lifecycle regressions for the shipped front preview helper.

Runs Java production sources against deterministic Android/native camera doubles.
The doubles model the audited APK's state/ownership and pipeline ordering, not
Android ART, graphics drivers, sensor output or on-screen image correctness.
"""
from pathlib import Path
import shutil
import subprocess


def test(root, work, androidjar=None):
    root, work = Path(root), Path(work)
    folder = work / 'front1936-tests'
    folder.mkdir(parents=True, exist_ok=True)
    src, classes = folder / 'src', folder / 'classes'
    src.mkdir()
    classes.mkdir()
    for source in (root / 'front1936-host').rglob('*.java'):
        dest = src / source.relative_to(root / 'front1936-host')
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, dest)
    previous = root / 'front1936-baseline'
    for name in ('PreviewStart1927', 'PreviewInputs1929', 'ProviderLifecycle1929', 'FrontPreview1931'):
        source = previous / (name + '.java')
        shutil.copyfile(source, src / ('com/hiro/ulike/' + name + '.java'))
    shutil.copyfile(root / 'FrontPreview1936.java', src / 'com/hiro/ulike/FrontPreview1936.java')
    command = ['javac', '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-d', str(classes)]
    command += list(map(str, sorted(src.rglob('*.java'))))
    result = subprocess.run(command, capture_output=True, text=True)
    (folder / 'javac.log').write_text(result.stdout + result.stderr)
    result.check_returncode()
    result = subprocess.run(['java', '-cp', str(classes), 'com.hiro.ulike.FrontTest1936'], capture_output=True, text=True)
    (work / 'host-front1936.txt').write_text(result.stdout + result.stderr)
    result.check_returncode()
    return {'status': 'passed', 'assertions': int(result.stdout.strip().split('=')[-1]), 'device_tested': False}


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--work', type=Path, required=True)
    args = parser.parse_args()
    args.work.mkdir(parents=True, exist_ok=True)
    print(test(Path(__file__).resolve().parent, args.work))
