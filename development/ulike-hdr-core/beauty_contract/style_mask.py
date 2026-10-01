"""Load the pinned neural blend mask from the user's supplied style archive."""
from hashlib import sha256
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile

import numpy as np

PINS = {
    "natural_blush": ("materials/016/AmazingFeature0/image/fusion_mask.png",41691,
                      "e438c9ec4336a387a0bba52982b4259fb9bff3cde60b7695b6efbc6451ef45c1","RGB"),
    "purity2": ("materials/016/AmazingFeature0/image/mask.png",37816,
                "e0d9a8ccce809ec53f3691b495f65fa59eda516844c0f8a77f9be80cfd035067","RGBA"),
}


def load_neural_mask(archive_path, style, *, vertical_flip):
    """Return 320x320 float64 mask.r; orientation must be specified explicitly.

    `False` maps PNG's first decoded row to the declared crop's upper row; `True`
    reverses those rows. This is a caller-selected sampling contract, not a claim
    that the original engine uploads PNG rows in that order. No ICC or gamma
    transform is applied: the blend shader samples the image's red channel.
    The alpha channel in Purity's PNG is not the neural blend mask channel.
    """
    from PIL import Image

    if style not in PINS or not isinstance(vertical_flip,bool):
        raise ValueError("Known style and explicit boolean mask orientation required")
    path=Path(archive_path)
    if not path.is_file() or path.stat().st_size>32*1024*1024:
        raise ValueError("Expected a bounded supplied style ZIP")
    member,size,digest,mode=PINS[style]
    with ZipFile(path) as archive:
        infos=archive.infolist()
        matches=[info for info in infos if info.filename==member]
        if len(infos)>10000 or len(matches)!=1 or matches[0].file_size!=size:
            raise ValueError("Missing, duplicate or unexpected mask member")
        with archive.open(matches[0]) as handle:
            data=handle.read(size+1)
    if len(data)!=size or sha256(data).hexdigest()!=digest:
        raise ValueError("Style mask hash mismatch")
    with Image.open(BytesIO(data)) as image:
        if image.format!="PNG" or image.size!=(320,320) or image.mode!=mode:
            raise ValueError("Unexpected pinned mask decode layout")
        pixels=np.asarray(image)
    red=pixels[...,0].astype(np.float64)/255.
    return red[::-1].copy() if vertical_flip else red
