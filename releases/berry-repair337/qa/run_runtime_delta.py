#!/usr/bin/env python3
"""Verify the diagnostic-only runtime scope against the active combined baseline."""
from pathlib import Path
from zipfile import ZipFile
import argparse
import hashlib
import json
import os
import subprocess

ROOT = Path(__file__).resolve().parents[1]
EXTENSION = 'extensions/berry_bfcache_restore.mpe'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--dist', type=Path, required=True)
    args = parser.parse_args()
    config = json.loads((ROOT / 'tool-paths.json').read_text())
    baseline = Path(config['baseline'])
    combined = args.dist / 'Hiro_Morphe_Patches_v1.0.337.mpp'
    single = args.dist / 'Berry_Browser_v1.4.4.mpp'
    work = ROOT / 'qa/runtime-delta'
    work.mkdir(exist_ok=True)
    paths = {}
    for name, artifact in [('baseline', baseline), ('combined', combined), ('single', single)]:
        with ZipFile(artifact) as archive:
            raw = archive.read(EXTENSION)
        paths[name] = work / (name + '.mpe')
        paths[name].write_bytes(raw)
    assert paths['combined'].read_bytes() == paths['single'].read_bytes(), 'Different Berry runtimes'
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    source = ROOT / 'qa/VerifyRuntimeDelta.java'
    host = config['tools']['classpath.jar']['path']
    subprocess.run([config['java'], '-jar', config['tools']['ecj.jar']['path'],
                    '-source', '11', '-target', '11', '-encoding', 'UTF-8',
                    '-cp', host, '-d', str(classes), str(source)], check=True)
    result = subprocess.run([config['java'], '-cp', str(classes) + os.pathsep + host,
                             'VerifyRuntimeDelta', str(paths['baseline']), str(paths['combined'])],
                            capture_output=True, text=True)
    (work / 'delta.log').write_text(result.stdout + result.stderr)
    result.check_returncode()
    delta = json.loads(result.stdout)
    report = {'schema': 'berry-repair337-diagnostic-runtime-delta-v1', 'result': 'PASS',
              'baseline_sha256': sha(baseline), 'combined_sha256': sha(combined),
              'standalone_sha256': sha(single), 'baseline_runtime_sha256': sha(paths['baseline']),
              'candidate_runtime_sha256': sha(paths['combined']), 'verifier_sha256': sha(source),
              'delta': delta, 'physical_device_tested': False}
    (ROOT / 'qa/QA_runtime_delta337.json').write_text(json.dumps(report, indent=2) + '\n')
    print(result.stdout.strip())


if __name__ == '__main__':
    main()
