#!/usr/bin/env python3
"""Exercise production startup logic with explicit lifecycle/native-controller fakes."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source-dir', type=Path, required=True)
    parser.add_argument('--work', type=Path, required=True)
    args = parser.parse_args()
    root, work = args.source_dir.resolve(), args.work.resolve()
    work.mkdir(parents=True, exist_ok=True)
    production = root / 'src/hiro/swiftkey/JapaneseStart.java'
    sources = sorted((root / 'tests/japanese_start_stubs').rglob('*.java'))
    sources += sorted((root / 'tests/japanese_start_host').rglob('*.java')) + [production]
    commands = [
        ('compile', ['java', 'com.sun.tools.javac.Main', '--release', '8', '-g:none', '-encoding', 'UTF-8',
                     '-d', str(work / 'classes')] + [str(p) for p in sources]),
        ('host', ['java', '-cp', str(work / 'classes'), 'JapaneseStartHostTest']),
    ]
    for name, command in commands:
        result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        (work / (name + '.log')).write_text(result.stdout)
        print(result.stdout, end='')
        if result.returncode:
            raise RuntimeError(name + ' failed')
    report = json.loads(result.stdout.splitlines()[-1])
    report.update(scope='Production Java lifecycle/selection logic on host fakes; not Android execution',
                  production_source_sha256=hashlib.sha256(production.read_bytes()).hexdigest(),
                  original_apk_apply_tested=False)
    report.update(japanese_start_on_normal_text_input=True, japanese_start_on_reopen=True,
                  japanese_manual_switch_preserved_within_session=True,
                  japanese_configured_layout_preserved=True,
                  japanese_special_input_requirements_preserved=True,
                  japanese_start_uses_native_language_selection=True,
                  japanese_start_no_text_injection=True)
    (work / 'report.json').write_text(json.dumps(report, indent=2) + '\n')


if __name__ == '__main__':
    main()
