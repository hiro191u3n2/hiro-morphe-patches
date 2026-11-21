"""Restore the published Instagram update from its pinned previous bundle."""
import copy
import hashlib
import sys
from pathlib import Path
from zipfile import ZipFile

base, changes, output = map(Path, sys.argv[1:])
baseline_hash = "39f499da5554ab400aec857656d9ab606a260a600d55218c5308cc431d2302d5"
release_hash = "ed49643607f33fbd9eac07bc327471e016ca65d4bc7e3067e50296a010939d43"
if hashlib.sha256(base.read_bytes()).hexdigest() != baseline_hash:
    raise ValueError("Unexpected 1.0.388 bundle")
with ZipFile(base) as previous, ZipFile(changes) as replacements, ZipFile(output, "w") as result:
    for entry in previous.infolist():
        source = replacements if entry.filename in replacements.NameToInfo else previous
        result.writestr(copy.copy(entry), source.read(entry.filename))
    for entry in replacements.infolist():
        if entry.filename not in previous.NameToInfo:
            result.writestr(copy.copy(entry), replacements.read(entry.filename))
if hashlib.sha256(output.read_bytes()).hexdigest() != release_hash:
    raise ValueError("Restored binary differs from Hiro 1.0.389")
