"""Independent lifecycle regressions for the shipped front preview helper.

Runs Java production sources against deterministic Android/native camera doubles.
The doubles model the audited APK's state/ownership and pipeline ordering, not
Android ART, graphics drivers, sensor output or on-screen image correctness.
"""
from pathlib import Path
import shutil
import subprocess


def test(root, work):
    root, work = Path(root), Path(work)
    folder = work / 'front-tests'
    folder.mkdir()
    src, classes = folder / 'src', folder / 'classes'
    src.mkdir()
    classes.mkdir()
    for source in (root / 'front-host').rglob('*.java'):
        dest = src / source.relative_to(root / 'front-host')
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, dest)
    previous = root / 'front-baseline'
    for name in ('PreviewStart1927', 'PreviewInputs1929', 'ProviderLifecycle1929'):
        source = previous / (name + '.java')
        shutil.copyfile(source, src / ('com/hiro/ulike/' + name + '.java'))
    shutil.copyfile(root / 'FrontPreview1931.java', src / 'com/hiro/ulike/FrontPreview1931.java')
    command = ['javac', '--release', '8', '-encoding', 'UTF-8', '-d', str(classes)]
    command += list(map(str, sorted(src.rglob('*.java'))))
    result = subprocess.run(command, capture_output=True, text=True)
    (folder / 'javac.log').write_text(result.stdout + result.stderr)
    result.check_returncode()
    result = subprocess.run(['java', '-cp', str(classes), 'com.hiro.ulike.FrontTest1931'], capture_output=True, text=True)
    (work / 'host-front1931.txt').write_text(result.stdout + result.stderr)
    result.check_returncode()
    return int(result.stdout.strip().split('=')[-1])


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--work', type=Path, required=True)
    args = parser.parse_args()
    args.work.mkdir(parents=True, exist_ok=True)
    print(test(Path(__file__).resolve().parent, args.work))
