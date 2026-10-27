#!/usr/bin/env python3
"""Independently disassemble a final merge and restore/check the exact old KeySwap class."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess


KEY = 'hiro/swiftkey/KeySwap.smali'
SIG = '(Lsc0/c;Landroid/content/res/Resources;)Lhiro/swiftkey/KeySwap$Wrapped;'
PRIOR = 'wrapBeforePunctuation1817'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def require(ok, message):
    if not ok:
        raise RuntimeError(message)


def files(root):
    return {p.relative_to(root).as_posix(): p.read_text() for p in root.rglob('*.smali')}


def extract(source, name):
    pattern = r'(?m)^\.method public static ' + re.escape(name + SIG) + r'\n[\s\S]*?^\.end method\n'
    found = list(re.finditer(pattern, source))
    require(len(found) == 1, 'Expected one method ' + name)
    return found[0]


def main():
    p = argparse.ArgumentParser()
    for name in ['baseline', 'overlay', 'extension', 'tools', 'work', 'output']:
        p.add_argument('--' + name, required=True, type=Path)
    p.add_argument('--source-dir', type=Path, default=Path(__file__).resolve().parent.parent)
    a = p.parse_args()
    a.work.mkdir(parents=True, exist_ok=True)
    dump = a.source_dir / 'tools_java/Dump1814.java'
    subprocess.run(['java', 'com.sun.tools.javac.Main', '-g:none', '-cp', str(a.tools / 'apktool.jar'),
                    '-d', str(a.work), str(dump)], check=True)
    cp = str(a.work) + os.pathsep + str(a.tools / 'apktool.jar')
    sources = {}
    for name, path in [('baseline', a.baseline), ('overlay', a.overlay), ('extension', a.extension)]:
        target = a.work / name
        require(not target.exists(), 'Use an unused verification work directory: ' + str(target))
        subprocess.run(['java', '-cp', cp, 'Dump1814', str(path), str(target)], check=True)
        sources[name] = files(target)
    before, overlay, after = (sources[n] for n in ['baseline', 'overlay', 'extension'])
    require(len(before) == 71 and set(before).issubset(after), 'Prior runtime inventory changed')
    additions = sorted(set(after) - set(before))
    require(additions and set(additions) == set(overlay), 'Overlay addition inventory differs')
    require(all(n == 'hiro/swiftkey/PunctuationKeys.smali' or n.startswith('hiro/swiftkey/PunctuationKeys$') for n in additions), 'Non-punctuation class added')
    require(all(after[n] == overlay[n] for n in additions), 'Compiled overlay semantics changed')
    changed = sorted(n for n in before if before[n] != after[n])
    require(changed == [KEY], 'Unrelated existing runtime changed: ' + repr(changed))
    original = extract(before[KEY], 'wrap').group()
    renamed = extract(after[KEY], PRIOR).group()
    require(renamed.replace(PRIOR + SIG, 'wrap' + SIG, 1) == original, 'Old wrap body/metadata altered')
    wrapper = extract(after[KEY], 'wrap')
    body = [line.strip() for line in wrapper.group().splitlines() if line.strip() and not line.strip().startswith('.')]
    expected = [
        'invoke-static {p0, p1}, Lhiro/swiftkey/KeySwap;->' + PRIOR + SIG,
        'move-result-object v0',
        'invoke-static {p0, p1, v0}, Lhiro/swiftkey/PunctuationKeys;->wrap(Lsc0/c;Landroid/content/res/Resources;Lhiro/swiftkey/KeySwap$Wrapped;)Lhiro/swiftkey/KeySwap$Wrapped;',
        'move-result-object v0',
        'return-object v0',
    ]
    require(body == expected, 'Wrapper does not forward original arguments and exact prior result once')
    restored = after[KEY][:wrapper.start()] + after[KEY][wrapper.end():]
    restored = restored.replace(PRIOR + SIG, 'wrap' + SIG, 1)
    # Removing a complete method leaves its two separator newlines. Ignore only
    # excess blank lines, preserving every instruction, field and annotation.
    normalize = lambda text: re.sub(r'\n{3,}', '\n\n', text)
    require(normalize(restored) == normalize(before[KEY]), 'KeySwap contains changes beyond lossless rename+wrapper')
    report = {
        'status': 'PASSED', 'scope': 'Independent canonical smali preservation and exact wrapper forwarding verification',
        'baseline_extension_sha256': sha(a.baseline), 'extension_sha256': sha(a.extension), 'overlay_sha256': sha(a.overlay),
        'unchanged_existing_classes': 70, 'changed_existing_classes': [KEY], 'added_classes': additions,
        'old_wrap_body_and_metadata_identical': True, 'old_keyswap_class_restores_identically': True,
        'wrapper_has_exactly_five_instructions': True, 'original_arguments_and_prior_result_forwarded_once': True,
        'device_tested': False, 'original_apk_apply_tested': False,
    }
    a.output.parent.mkdir(parents=True, exist_ok=True)
    a.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print('PASS independent punctuation merge preservation; all70 other classes identical, exact5-instruction forwarding')


if __name__ == '__main__':
    main()
