"""Read the exact native 106-point template from a caller-supplied pinned ELF.

No vendor template values are embedded or redistributed by this source. The
caller is responsible for keeping extracted arrays outside a public source tree.
"""
from hashlib import sha256
from pathlib import Path

import numpy as np

EFFECT_SHA256 = "d40af10b250b91cf8f30f4a265ac1d3b7b5b88a82bbf63da332c3f47a415d48e"


def extract_template(library_path):
    p = Path(library_path)
    if not p.is_file() or p.stat().st_size > 128 * 1024 * 1024:
        raise ValueError("Expected the pinned libeffect.so")
    with p.open("rb") as f:
        data = f.read(128 * 1024 * 1024 + 1)
    if len(data) > 128 * 1024 * 1024 or sha256(data).hexdigest() != EFFECT_SHA256:
        raise ValueError("libeffect.so hash mismatch")
    # Pinned ELF's VA==file offset for this read-only segment. Native initializer
    # 0xcbd234..0xcbd458 copies the contiguous 848 bytes to a 106-Point2f vector.
    result = np.frombuffer(data, dtype="<f4", count=212, offset=0x116d7a0).reshape(106, 2)
    if not np.all(np.isfinite(result)):
        raise ValueError("Invalid native template")
    return result.copy()
