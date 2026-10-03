import hashlib,json
from pathlib import Path
root=Path('native-build182')
manifest=json.loads((root/'sources.json').read_text())
assert set(manifest['files'])=={'build_android.sh','native/readback_core.cpp','native/readback_core.h','native/native_capture.cpp'}
for name,item in manifest['files'].items():
    data=(root/name).read_bytes()
    assert len(data)==item['bytes'] and hashlib.sha256(data).hexdigest()==item['sha256'],name
print('Native source catalog verified')
