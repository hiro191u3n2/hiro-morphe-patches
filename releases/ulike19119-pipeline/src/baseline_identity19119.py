"""Verify the immutable published .118 source graph before rebuilding legacy native."""
from pathlib import Path
import hashlib,json
MANIFEST_SHA256='6ee3a313a762c3b9d7000221712bffbea6d66b7a1ccce55b8f7ecfdaab9fea35'
def verify(source):
    tree=Path(source)/'baseline19118';raw=(tree/'published-manifest19118.json').read_bytes()
    assert hashlib.sha256(raw).hexdigest()==MANIFEST_SHA256,'Published118 manifest differs'
    pins=json.loads(raw)['reviewed_source_sha256']
    for name,digest in pins.items():
        p=tree/name
        assert p.is_file() and not p.is_symlink() and hashlib.sha256(p.read_bytes()).hexdigest()==digest,'Published118 source differs: '+name
    return tree
