#!/usr/bin/env python3
"""Generate the Java literal from the audited JS, without runtime assets/network access."""
import json
from pathlib import Path
import sys

source = Path(__file__).with_name('review-sort.js').read_text(encoding='utf-8')
if len(source.encode('utf-8')) > 50000:
    raise SystemExit('Script exceeds conservative Java constant limit')
out = Path(sys.argv[1])
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text('package app.hiro.amazonreviews;\nfinal class ScriptPayload {\n'
               '    private ScriptPayload() {}\n'
               '    static final String SCRIPT = ' + json.dumps(source, ensure_ascii=True) + ';\n}\n', encoding='utf-8')
