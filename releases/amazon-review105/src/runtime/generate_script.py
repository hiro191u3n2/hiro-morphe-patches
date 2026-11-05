#!/usr/bin/env python3
"""Embed the audited JS using bounded, non-concatenated Java string constants."""
import json
from pathlib import Path
import sys


def generate_java(source):
    if len(source.encode('utf-8')) > 100 * 1024:
        raise ValueError('Script exceeds the reviewed 100 KiB payload limit')
    # Standard UTF-8 chunks stay below 10,000 bytes without splitting a code point.
    # Even modified UTF-8's NUL/surrogate expansion remains below Java's 65,535 limit.
    chunks, held, width = [], [], 0
    for char in source:
        size = len(char.encode('utf-8'))
        if width + size > 10000:
            chunks.append(''.join(held))
            held, width = [], 0
        held.append(char)
        width += size
    if held:
        chunks.append(''.join(held))
    lines = ['package app.hiro.amazonreviews;', 'final class ScriptPayload {',
             '    private ScriptPayload() {}', '    static final String SCRIPT = build();',
             '    private static String build() {', '        StringBuilder script = new StringBuilder();']
    for chunk in chunks:
        lines.append('        script.append(' + json.dumps(chunk, ensure_ascii=True) + ');')
    lines.extend(['        return script.toString();', '    }', '}', ''])
    return '\n'.join(lines)


def main():
    source = Path(__file__).with_name('review-sort.js').read_text(encoding='utf-8')
    out = Path(sys.argv[1])
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(generate_java(source), encoding='utf-8')


if __name__ == '__main__':
    main()
