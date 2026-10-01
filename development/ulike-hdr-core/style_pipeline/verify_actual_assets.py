#!/usr/bin/env python3
"""Actual-asset numerical verification; no vendor texture or script is emitted.

The reference evaluates the source shader stages separately in scalar Python.
The implementation under test is the Android-compatible Java tile pipeline.
Source image rows and straight-to-premultiplied conversion are explicit fixture
policies, not assertions about the original runtime's upload conventions.
"""
import argparse
import hashlib
import io
import json
import math
from pathlib import Path
import struct
import subprocess
import tempfile
import zipfile

import numpy as np
from PIL import Image

PINS = {
    "natural": "5b50343dcedc7e9aadd218626e621ad683cd67372d225024f118b47f493546f0",
    "purity": "cd5da20fefe1f9bd594e4e0d055d6320392fa54fb5df9d34b8f159c791c5b117",
}
# pass enum index, feature, shader basename, material basename, texture(s).
ASSETS = {
    "NATURAL_BLUSHER": (0, "natural", "AmazingFeature1", "mask_faceuv22995", "mask_faceuv22995_MATERIAL",
                         ["image/blusher/blusher000.png"]),
    "PURITY_LIPS": (1, "purity", "AmazingFeature1", "lips_keypoint_faceu2996", "lips_keypoint_faceu2996_MATERIAL",
                    ["image/lips2/lips0000.png", "image/lips2/lips0001.png"]),
    "PURITY_BLUSHER": (2, "purity", "FaceMakeupV2_byExport2", "makeup/blusher", None,
                       ["blusher/blusher000.png"]),
    "PURITY_FACIAL": (3, "purity", "AmazingFeature3", "mask_faceuv22994", "mask_faceuv22994_MATERIAL",
                      ["image/facialfeatures/facialSeq_000.png"]),
    "PURITY_3D": (4, "purity", "3dmakeup4", "makeup", "face", ["image/makeup3d_open.png"]),
    "PURITY_EYE_MULTIPLY": (5, "purity", "AmazingFeature5", "eye_part_faceu2987", "eye_part_faceu2987_MATERIAL",
                            ["image/eye/eye000.png"]),
    "PURITY_EYE_SCREEN": (6, "purity", "AmazingFeature5", "eye_part_faceu2988", "eye_part_faceu2988_MATERIAL",
                          ["image/eye_srceen/eye000.png"]),
    "PURITY_EYELASH": (7, "purity", "AmazingFeature6", "jiemao_faceu_e3ffcd1e800b950f0e616295904d1451", "jiemaoFaceU_msqo_1669694037",
                       ["image/eyelash.png"]),
    "PURITY_SHADOW_MULTIPLY": (8, "purity", "AmazingFeature7", "eye_part_faceu2991", "eye_part_faceu2991_MATERIAL",
                               ["image/eye/eye000.png"]),
    "PURITY_SHADOW_SCREEN": (9, "purity", "AmazingFeature7", "eye_part_faceu2992", "eye_part_faceu2992_MATERIAL",
                             ["image/eye_screen/eye000.png"]),
}


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(b):
    return hashlib.sha256(b).hexdigest()


def uniforms(b):
    result = {}
    for key in ["intensity", "opacity", "inputColor", "colorR", "colorG", "colorB", "uniAlpha"]:
        marker = struct.pack("<H", len(key)) + key.encode() + bytes.fromhex("004798ae05000000")
        if marker not in b:
            continue
        require(b.count(marker) == 1, "ambiguous pinned material property")
        offset = b.index(marker) + len(marker)
        value = struct.unpack_from("<d", b, offset)[0]
        require(math.isfinite(value), "nonfinite material property")
        result[key] = value
    return result


def decode(z, path):
    raw = z.read(path)
    with Image.open(io.BytesIO(raw)) as image:
        expected = "JPEG" if path == "materials/016/AmazingFeature9/image/filter.png" else "PNG"
        require(image.format == expected, "unexpected pinned image content format")
        rgba = np.asarray(image.convert("RGBA"), dtype=np.float64) / 255.0
    return rgba


def bilinear(tex, u, v):
    h, w, _ = tex.shape
    x, y = max(0, min(w-1, u*w-.5)), max(0, min(h-1, v*h-.5))
    x0, y0 = math.floor(x), math.floor(y)
    x1, y1 = min(w-1, x0+1), min(h-1, y0+1)
    fx, fy = x-x0, y-y0
    return [(tex[y0, x0, c]*(1-fx)+tex[y0, x1, c]*fx)*(1-fy)
            + (tex[y1, x0, c]*(1-fx)+tex[y1, x1, c]*fx)*fy for c in range(tex.shape[2])]


def reference_makeup(name, base, overlay, coverage, intensity, opacity, color, seg, inside, shader_base):
    out = np.array(base, copy=True)
    for i, (b, ov, cov) in enumerate(zip(base, overlay, coverage)):
        a = ov[3]
        if name == "PURITY_3D":
            if cov == 0: continue
            color3d = [v/a if a > 0 else 0 for v in ov[:3]]
            fragment = [x+(x*s-x)*(a*intensity) for x,s in zip(shader_base[i],color3d)]
            out[i] = [x+(f-x)*cov for x,f in zip(b,fragment)]
            continue
        if cov == 0 or intensity == 0 or (opacity == 0 and name not in ["PURITY_EYELASH", "PURITY_3D"]):
            continue
        if a == 0:
            require(name != "PURITY_FACIAL", "undefined facial sample")
            continue
        s = [min(1, max(0, t/a)) for t in ov[:3]]
        if color is not None:
            s = color
        if name == "PURITY_FACIAL":
            effect = [2*x*y+x*x*(1-2*y) if y < .5 else math.sqrt(x)*(2*y-1)+2*x*(1-y)
                      for x, y in zip(b, s)]
            red = min(1, max(0, ov[0]/a))
            alpha = min(1, max(0, (abs(red-.5)-2/255)*32)) * intensity * opacity
            final = [e*alpha+x*(1-alpha) for x, e in zip(b, effect)]
        else:
            rgb = b if shader_base is None else shader_base[i]
            effect = [1-(1-x)*(1-y) if name.endswith("SCREEN") else x*y for x, y in zip(rgb, s)]
            if name in ["PURITY_EYELASH", "PURITY_3D"]:
                final = [x+(e-x)*a for x, e in zip(b, effect)]
                final = [x+(e-x)*intensity for x, e in zip(b, final)]
            else:
                final = [x+(e-x)*(a*intensity*opacity) for x, e in zip(b, effect)]
            if seg is not None:
                if name != "PURITY_EYELASH" and a < .001:
                    continue
                weight = seg[i] if inside[i] else (0 if name == "PURITY_EYELASH" else 1)
                final = [x+(e-x)*weight for x, e in zip(b, final)]
        out[i] = [x+(e-x)*cov for x, e in zip(b, final)]
    return out


def reference_lut(base, first, second, mask, intensity):
    out = []
    for i, b in enumerate(base):
        blue = b[2]*63
        lo, hi = math.floor(blue), math.ceil(blue)
        def sample(tex, idx):
            u = (idx % 8)/8+.5/512+(1/8-1/512)*b[0]
            v = (idx // 8)/8+.5/512+(1/8-1/512)*b[1]
            return bilinear(tex, u, v)[:3]
        low = sample(first, lo)
        if second is not None:
            high, t = sample(second, lo), mask[i]
        else:
            high, t = sample(first, hi), blue-lo
        edited = [x*(1-t)+y*t for x, y in zip(low, high)]
        out.append([x*(1-intensity)+y*intensity for x, y in zip(b, edited)])
    return np.asarray(out)


def write_int(f, n): f.write(struct.pack(">i", n))
def write_bool(f, b): f.write(bytes([bool(b)]))
def write_double(f, d): f.write(struct.pack(">d", d))
def write_array(f, a):
    a = np.asarray(a, dtype=">f8").ravel()
    write_int(f, a.size); f.write(a.tobytes())
def write_texture(f, tex):
    write_int(f, tex.shape[1]); write_int(f, tex.shape[0]); write_array(f, tex[:, :, :3])


def verify(natural, purity, java, classes, output):
    archives = {}
    evidence = {"schema": "ulike-style-pipeline-1", "archives": {}, "passes": [], "lut_assets": {},
                "published_vendor_assets": False, "runtime_uniforms_confirmed": False,
                "full_style_reproduction_verified": False, "hdr_domain_supported": False,
                "android_device_verified": False,
                "fixture_policy": "source image rows, decoded byte samples without ICC, explicitly straight-to-premultiplied before bilinear sampling",
                "purity_final_lut_content_format": "JPEG/JFIF (despite .png extension); existing authored LUT losses cannot be reversed",
                "source_over_policy": "ONE / ONE_MINUS_SRC_ALPHA replacement for premultiplied fragments; original GPU state not executed",
                "same_z_order": "caller must provide resolved order between paired eye renderers"}
    for name, file in [("natural", natural), ("purity", purity)]:
        require(sha(Path(file).read_bytes()) == PINS[name], "archive mismatch: "+name)
        archives[name] = zipfile.ZipFile(file)
        evidence["archives"][name] = PINS[name]
    rng = np.random.default_rng(16820261001)
    n = 1024
    jobs, expected, labels = [], [], []
    for name, (index, style, feature, shader, material, textures) in ASSETS.items():
        z = archives[style]; prefix = "materials/016/"+feature+"/"
        shader_path = prefix + (shader if "/" in shader else "xshader/"+shader) + ".frag"
        paths = [shader_path]
        if material:
            material_path = prefix+"material/"+material+".material"; paths.append(material_path)
            props = uniforms(z.read(material_path))
        else:
            props = {"intensity": json.loads(z.read(prefix+"makeup.json"))["filters"][0]["intensity"]}
            paths.append(prefix+"makeup.json")
        color = ([props[k] for k in ["colorR", "colorG", "colorB"]]
                 if props.get("inputColor", 0) > 0 else None)
        segmented = name in ["PURITY_LIPS", "PURITY_EYELASH"] or "EYE_" in name or "SHADOW_" in name
        for texture in textures:
            texture_path = prefix+texture; paths.append(texture_path)
            tex = decode(z, texture_path); tex[:, :, :3] *= tex[:, :, 3:4]
            uv = rng.uniform(-.03, 1.03, (n, 2))
            overlay = np.asarray([bilinear(tex, u, v) for u, v in uv])
            base = rng.random((n, 3)); base[0] = [-0., .123456789012345, 1.]
            coverage = rng.random(n); coverage[::11] = 0.; coverage[::13] = 1.
            seg = rng.random(n) if segmented else None
            inside = rng.random(n) > .15 if segmented else None
            intensity, opacity = props["intensity"]*.7, .83
            shader_base = rng.random((n,3)) if name in ["PURITY_BLUSHER", "PURITY_3D"] else None
            jobs.append((0, index, base, overlay, coverage, intensity, opacity, color, seg, inside, shader_base))
            expected.append(reference_makeup(name, base, overlay, coverage, intensity, opacity, color, seg, inside, shader_base))
            labels.append(name+":"+texture)
        # Include the scripts that resolve slider events, while retaining only hashes.
        for candidate in [prefix+"lua/EffectFaceMakeupSystemScript.lua", prefix+"lua/Face3DSystem.lua",
                          prefix+"FaceMakeupModule.lua", prefix+"main.scene"]:
            if candidate in z.namelist(): paths.append(candidate)
        evidence["passes"].append({"pass": name, "authored_uniforms": props,
              "authored_uniforms_are_runtime_values": False, "segmentation_required": segmented,
              "source_sha256": {path: sha(z.read(path)) for path in paths}})
    for style, feature in [("natural", "AmazingFeature2"), ("purity", "AmazingFeature8")]:
        z = archives[style]; prefix = "materials/016/"+feature+"/"
        bg, skin = decode(z, prefix+"image/filter_bg.png"), decode(z, prefix+"image/filter_skin.png")
        mat = uniforms(z.read(prefix+"material/SkinSeg.material"))
        base, mask = rng.random((n, 3)), rng.random(n)
        base[:64, 2] = np.arange(64)/63
        alpha = mat["uniAlpha"]*(.7 if style == "natural" else .69)
        jobs.append((1, base, bg, skin, mask, alpha));expected.append(reference_lut(base, bg, skin, mask, alpha));labels.append(style+":skin_lut")
        paths = [prefix+p for p in ["image/filter_bg.png", "image/filter_skin.png", "material/SkinSeg.material", "ComposerUpdate.lua", "xshader/skinseg.frag"]]
        evidence["lut_assets"][style] = {"authored_uniforms": mat, "source_sha256": {p: sha(z.read(p)) for p in paths}}
    z = archives["purity"]; prefix = "materials/016/AmazingFeature9/"
    tex = decode(z, prefix+"image/filter.png");mat = uniforms(z.read(prefix+"material/pass0.material"))
    base = rng.random((n, 3));base[:64, 2] = np.arange(64)/63;alpha = mat["uniAlpha"]*.69
    jobs.append((2, base, tex, alpha));expected.append(reference_lut(base, tex, None, None, alpha));labels.append("purity:final_lut")
    paths = [prefix+p for p in ["image/filter.png", "material/pass0.material", "ComposerUpdate.lua", "xshader/pass0.frag"]]
    evidence["lut_assets"]["purity_final"] = {"authored_uniforms": mat, "source_sha256": {p: sha(z.read(p)) for p in paths}}
    for style, z in archives.items():
        graph = json.loads(z.read("materials/016/config.json"))["effect"]["Link"]
        evidence.setdefault("declared_graph_order", {})[style] = [{"path": g["path"], "zorder": g["zorder"]} for g in graph]
        evidence.setdefault("graph_config_sha256", {})[style] = sha(z.read("materials/016/config.json"))
    with tempfile.TemporaryDirectory(prefix="ulike-style-") as temp:
        inp, out = Path(temp)/"fixtures.bin", Path(temp)/"results.bin"
        with inp.open("wb") as f:
            write_int(f, 0x53545931); write_int(f, len(jobs))
            for job in jobs:
                write_int(f, job[0]); write_int(f, n)
                if job[0] == 0:
                    _, index, base, overlay, coverage, intensity, opacity, color, seg, inside, shader_base = job
                    write_array(f, base);write_int(f, index);write_array(f, overlay);write_array(f, coverage)
                    write_double(f, intensity);write_double(f, opacity);write_bool(f, color is not None)
                    if color is not None: write_array(f, color)
                    write_bool(f, seg is not None)
                    if seg is not None: write_array(f, seg);f.write(bytes(bool(x) for x in inside))
                    write_bool(f, shader_base is not None)
                    if shader_base is not None: write_array(f, shader_base)
                elif job[0] == 1:
                    _, base, bg, skin, mask, alpha = job
                    write_array(f, base);write_texture(f, bg);write_texture(f, skin);write_array(f, mask);write_double(f, alpha)
                else:
                    _, base, tex, alpha = job
                    write_array(f, base);write_texture(f, tex);write_double(f, alpha)
        subprocess.run([java, "-cp", str(classes), "com.hiro.ulike.style.FixtureProbe", str(inp), str(out)], check=True)
        b = out.read_bytes(); pos = 0
        count = struct.unpack_from(">i", b, pos)[0];pos += 4
        require(count == len(jobs), "missing results")
        results = []
        for label, reference in zip(labels, expected):
            length = struct.unpack_from(">i", b, pos)[0];pos += 4
            require(length == n*3, "wrong result shape")
            actual = np.frombuffer(b, dtype=">f8", count=length, offset=pos).reshape(n, 3);pos += length*8
            error = float(np.max(np.abs(reference-actual)))
            require(np.isfinite(actual).all() and error <= 1e-14, "numeric mismatch "+label)
            results.append({"fixture": label, "pixels": n, "max_abs_error": error})
        require(pos == len(b), "trailing results")
    evidence["actual_asset_results"] = results
    evidence["scalar_comparisons"] = len(results)*n*3
    evidence["status"] = "PASS"
    evidence["max_abs_error"] = max(r["max_abs_error"] for r in results)
    root = Path(__file__).resolve().parent
    evidence["implementation_sha256"] = {str(p.relative_to(root)): sha(p.read_bytes()) for p in sorted((root/"src").rglob("*.java"))}
    Path(output).write_text(json.dumps(evidence, indent=2)+"\n")
    print(json.dumps({k: evidence[k] for k in ["status", "scalar_comparisons", "max_abs_error"]}))


if __name__ == "__main__":
    p = argparse.ArgumentParser();p.add_argument("natural");p.add_argument("purity")
    p.add_argument("--java", required=True);p.add_argument("--classes", required=True);p.add_argument("--output", required=True)
    args = p.parse_args();verify(args.natural, args.purity, args.java, args.classes, args.output)
