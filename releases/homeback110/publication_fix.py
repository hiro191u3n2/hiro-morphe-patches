#!/usr/bin/env python3
"""Allow large immutable GitHub Contents reads; verify the Git blob identity."""
from pathlib import Path
import ast
p = Path(__file__).resolve().parent / 'publication/publish.py'
s = p.read_text()
old = '''def content(path, ref):
    row = api(f"repos/{REPO}/contents/{path}?ref={ref}")
    require(row.get("encoding") == "base64", "Unexpected repository file encoding")
    return base64.b64decode(row["content"])
'''
new = r'''def content(path, ref):
    checked_path(path)
    require(re.fullmatch(r"[0-9a-f]{40}", ref) is not None, "Read requires immutable commit")
    row = api(f"repos/{REPO}/contents/{path}?ref={ref}")
    require(row.get("type") == "file" and re.fullmatch(r"[0-9a-f]{40}", row.get("sha", "")) is not None, "Not a Git file")
    if row.get("encoding") == "base64":
        raw = base64.b64decode(row["content"])
    elif row.get("encoding") == "none":
        # GitHub Contents omits inline bytes above its size limit.
        raw = fetch(f"https://raw.githubusercontent.com/{REPO}/{ref}/{path}")
    else:
        raise RuntimeError("Unsupported repository file encoding")
    require(len(raw) == row["size"], "Immutable repository file size mismatch")
    object_id = hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\x00" + raw).hexdigest()
    require(object_id == row["sha"], "Immutable repository Git blob mismatch")
    return raw
'''
if old not in s:
    assert new in s, 'Unexpected publisher version'
else:
    assert s.count(old) == 1
    s = s.replace(old, new)
    ast.parse(s)
    p.write_text(s)
print('PASS size-limited GitHub policy reads use immutable, blob-verified bytes')
