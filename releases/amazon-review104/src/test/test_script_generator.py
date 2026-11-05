#!/usr/bin/env python3
"""Verify javac reconstructs scripts larger than one constant without escaping loss."""
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('generate_script', root / 'runtime/generate_script.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def main():
    javac, java = sys.argv[1:3]
    # Exercise chunk boundaries, Japanese, surrogate pairs, NUL and literal escapes.
    source = ('A' * 9999 + '🛒' + '\n評価件数"\\\r\t\0') * 8
    assert len(source.encode('utf-8')) > 65535
    try:
        module.generate_java('A' * (100 * 1024 + 1))
    except ValueError:
        pass
    else:
        raise AssertionError('Oversized payload accepted')
    code = module.generate_java(source)
    literals = [json.loads(line.strip()[14:-2]) for line in code.splitlines() if line.strip().startswith('script.append(')]
    assert literals and all(len(chunk.encode('utf-8')) <= 10000 for chunk in literals)
    assert ''.join(literals) == source
    with tempfile.TemporaryDirectory(prefix='amazon-script-generator-') as folder:
        folder = Path(folder)
        generated = folder / 'ScriptPayload.java'
        generated.write_text(code, encoding='utf-8')
        probe = folder / 'ScriptProbe.java'
        probe.write_text('package app.hiro.amazonreviews; public class ScriptProbe { public static void main(String[] args) throws Exception { System.out.write(ScriptPayload.SCRIPT.getBytes("UTF-8")); } }', encoding='utf-8')
        subprocess.run([javac, '--release', '8', '-encoding', 'UTF-8', '-d', str(folder), str(generated), str(probe)], check=True, capture_output=True)
        actual = subprocess.run([java, '-cp', str(folder), 'app.hiro.amazonreviews.ScriptProbe'], check=True, capture_output=True).stdout
        assert actual == source.encode('utf-8'), 'Generated Java string differs from source bytes'
    print('PASS Java script generation beyond 65535 bytes; bounded literals, Unicode, escapes and chunk boundaries preserved')


if __name__ == '__main__':
    main()
