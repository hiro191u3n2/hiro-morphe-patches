"""Deterministic archives and current-evidence inventory for TikTok 47.4.3."""
from pathlib import Path, PurePosixPath
import hashlib, json, zipfile

def require(value, message):
    if not value:
        raise RuntimeError(message)

def sha(path):
    digest = hashlib.sha256()
    with Path(path).open('rb') as handle:
        for data in iter(lambda: handle.read(1024 * 1024), b''):
            digest.update(data)
    return digest.hexdigest()

def sha_bytes(value):
    return hashlib.sha256(value).hexdigest()

def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode()

def archive(path):
    with zipfile.ZipFile(path) as source:
        names = source.namelist()
        require(len(names) == len(set(names)), 'Duplicate ZIP entries')
        result = {}
        for name in names:
            clean = name.rstrip('/')
            p = PurePosixPath(clean)
            require(clean and not p.is_absolute() and '..' not in p.parts
                    and '\\' not in clean and str(p) == clean, 'Unsafe ZIP entry: ' + name)
            if not name.endswith('/'):
                result[name] = source.read(name)
        return result

def write_zip(path, entries):
    with zipfile.ZipFile(path, 'w') as target:
        for name, value in sorted(entries.items()):
            info = zipfile.ZipInfo(name, (2026, 10, 24, 0, 0, 0))
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            target.writestr(info, value, compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)

def headers(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '):
            require(rows, 'Orphan manifest continuation')
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    values = [row.decode().split(': ', 1) for row in rows]
    require(all(len(v) == 2 for v in values), 'Malformed manifest')
    require(len(values) == len(dict(values)), 'Duplicate manifest header')
    return dict(values)

def manifest(fields):
    rows = []
    for key, value in fields.items():
        require(isinstance(value, str) and not any(c in value for c in '\r\n\0'), 'Invalid header')
        raw, prefix = (key + ': ' + value).encode(), b''
        while len(prefix) + len(raw) > 72:
            cut = 72 - len(prefix)
            while raw[cut] & 192 == 128:
                cut -= 1
            rows.append(prefix + raw[:cut])
            raw, prefix = raw[cut:], b' '
        rows.append(prefix + raw)
    raw = b'\r\n'.join(rows) + b'\r\n\r\n'
    require(headers(raw) == fields, 'Manifest round-trip mismatch')
    return raw

def class_files(folder):
    return {p.relative_to(folder).as_posix(): p.read_bytes()
            for p in sorted(Path(folder).rglob('*.class'))}

def retired_source(name):
    """Old output proofs stay in their original immutable release, never this one.

    Explicit filtering also keeps a restored scratch copy of a retired proof
    from entering the source archive or pretending to verify the new MPP.
    Executed regression tests and all implementation sources remain included.
    """
    if name.startswith(('validation/oneback/', 'validation/native/',
                        'validation/privacy/', 'validation/prior-4753-v4.0.0/',
                        'validation/manager/', 'validation/real-apk/4753/')):
        return True
    if name == 'validation/real-apk-4753.json':
        return True
    if name.startswith('validation/metra/'):
        return name not in ('validation/metra/SettingsHookAudit.java', 'validation/metra/VerifyOriginal.java')
    return False

def source_files(root):
    root = Path(root)
    return {p.relative_to(root).as_posix(): p for p in sorted(root.rglob('*'))
            if p.is_file() and '__pycache__' not in p.parts
            and not retired_source(p.relative_to(root).as_posix())}

def source_ids(root):
    return {name: sha(p) for name, p in source_files(root).items()}
