import json
import os
import re
import shutil
import subprocess
from pathlib import Path


def test(root, work, android_jar=None):
    root, work = Path(root), Path(work)
    classes = work / 'reflection1945-classes'
    classes.mkdir(parents=True, exist_ok=True)
    javac = os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
    java = os.environ.get('ULIKE_JAVA') or (str(Path(javac).with_name('java')) if javac else shutil.which('java'))
    compiler = [javac] if javac else [java, 'com.sun.tools.javac.Main']
    sources = [root / 'ReflectionCache1945.java', root / 'tests/ReflectionCache1945Test.java']
    built = subprocess.run(compiler + ['-source', '8', '-target', '8', '-Xlint:-options', '-d', str(classes), *map(str, sources)], capture_output=True, text=True, timeout=120)
    if built.returncode:
        raise RuntimeError(built.stdout + built.stderr)
    result = subprocess.run([java, '-cp', str(classes), 'com.hiro.ulike.ReflectionCache1945Test'], capture_output=True, text=True, timeout=120)
    if result.returncode or not result.stdout.startswith('PASS reflection metadata caching checks='):
        raise RuntimeError(result.stdout + result.stderr)
    checks = re.search(r'checks=(\d+)', result.stdout)
    if checks is None or int(checks.group(1)) <= 0:
        raise RuntimeError('Executed reflection checks absent')
    report = {'pass': True, 'status': 'passed', 'assertions': int(checks.group(1)), 'output': result.stdout.strip(), 'dynamic_values_uncached': True,
              'classloader_identity': True, 'public_method_and_hierarchy_semantics': True,
              'concurrent_lookup': True, 'failed_lookups_uncached': True, 'bounded_metadata_only': True}
    (work / 'host-reflection1945-result.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--work', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(test(Path(__file__).resolve().parent, args.work), indent=2))
