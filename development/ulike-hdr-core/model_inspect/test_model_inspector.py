#!/usr/bin/env python3
"""Synthetic boundary tests plus optional, hash-bound local model observations."""
import argparse
import hashlib
import json
import struct
import tempfile
import unittest
import zipfile
from pathlib import Path

import model_inspector as mi


def word(value):
    return struct.pack("<I", value)


def fixture(checksum=True):
    entries = []
    for key, typ, payload in [("integer", 1, struct.pack("<i", -42)),
                              ("float", 2, struct.pack("<f", .4)),
                              ("text", 0, b"input_0"), ("weights", 3, b"\xff\x00\x81")]:
        header = bytearray(296)
        header[:len(key)] = key.encode()
        header[288:296] = word(typ) + word(len(payload))
        entries.append(bytes(header) + payload)
    body = b"".join(word(len(e)) for e in entries) + b"".join(entries)
    group = bytearray(292)
    group[:4] = b"test"
    group[288:292] = word(len(entries))
    for byte in body:
        group[256] ^= byte
    group = bytes(group) + body
    header = bytearray(40)
    header[4] = int(checksum)
    header[36:40] = word(1)
    out = header + word(len(group)) + group
    out[:4] = word(len(out))
    return bytes(out)


def legacy_fixture(version="v3"):
    def blob(b):
        return word(len(b)) + b
    header = bytearray(version.encode() + b"\0" * 14)
    group = blob(b"v0") + blob(b"opaque auxiliary") + word(5) + blob(b"encoded") + word(6) + blob(b"tail")
    d = header + word(0) + blob(b"test_v1.0") + word(1) + group
    if version == "v3":
        d += word(1) + blob(b"CropMarginv0") + struct.pack("<f", .2)
        d += word(1) + blob(b"CropWidthv0") + word(256)
    d[16:20] = word(len(d))
    return bytes(d)


class InspectorTests(unittest.TestCase):
    def reject(self, data, kind="bach"):
        with self.assertRaises(mi.FormatError):
            mi.inspect(data, kind)

    def test_typed_and_opaque_values(self):
        result = mi.inspect(fixture())
        p = result["groups"][0]["parameters"]
        self.assertEqual(p[0]["payload"]["value"], -42)
        self.assertAlmostEqual(p[1]["payload"]["value"], .4, places=7)
        self.assertEqual(p[2]["payload"]["value"], "input_0")
        self.assertNotIn("value", p[3]["payload"])
        self.assertEqual(p[3]["payload"]["bytes"], 3)
        self.assertEqual(result["groups"][0]["checksum"]["computed"],
                         result["groups"][0]["checksum"]["stored"])
        self.assertFalse(result["tensor_graph_decoded"])

    def test_all_fixture_truncations(self):
        data = fixture()
        for cut in range(len(data)):
            self.reject(data[:cut])

    def test_inconsistent_counts_and_sizes(self):
        # Checksum disabled so structural validation is independently exercised.
        base = fixture(False)
        for position, value in [(0, 0xffffffff), (36, 0xffffffff), (36, 0),
                                (40, 0xffffffff), (40, 1), (44 + 288, 0xffffffff),
                                (44 + 292, 1), (44 + 292, 0xffffffff),
                                (44 + 292 + 16 + 292, 0xffffffff)]:
            data = bytearray(base)
            data[position:position + 4] = word(value)
            self.reject(data)
        data = bytearray(base + b"extra")
        data[:4] = word(len(data))
        self.reject(data)

    def test_bounded_names(self):
        for position in (44, 44 + 292 + 16):
            data = bytearray(fixture(False))
            data[position:position + 256] = b"A" * 256
            self.reject(data)
            data[position:position + 256] = b"\x00" * 256
            self.reject(data)

    def test_checksum_and_sha(self):
        data = bytearray(fixture())
        data[-1] ^= 1
        self.reject(data)
        with self.assertRaisesRegex(mi.FormatError, "SHA-256"):
            mi.inspect(fixture(), expected_sha256="0" * 64)
        result = mi.inspect(fixture(), expected_sha256=hashlib.sha256(fixture()).hexdigest())
        self.assertEqual(result["kind"], "bach_packed_buffer")

    def test_noncanonical_typed_values_remain_opaque(self):
        for typ, payload in [(1, b"\1"), (2, struct.pack("<f", float("nan"))),
                             (0, b"\xff\0"), (33, b"abc")]:
            p = mi.payload_description(payload, 0, len(payload), typ)
            self.assertNotIn("value", p)
            json.dumps(p, allow_nan=False)

    def test_unknown_does_not_claim_decode(self):
        self.assertEqual(mi.inspect(b"arbitrary data")["status"], "not_decoded")
        self.reject(b"", "auto")

    def test_ambiguous_header_requires_explicit_kind(self):
        data = bytearray(fixture(False))
        data[16:20] = word(len(data))
        self.assertEqual(mi.inspect(data)["kind"], "ambiguous")
        self.assertEqual(mi.inspect(data, "bach")["kind"], "bach_packed_buffer")

    def test_legacy_header_only(self):
        name = b"test_v1.0"
        d = bytearray(24) + name + b"opaque payload"
        d[16:24] = word(len(d)) + word(len(name))
        r = mi.inspect(d)
        self.assertEqual(r["name_candidate"], "test_v1.0")
        self.assertEqual(r["status"], "header_identified_payload_opaque")
        d[20:24] = word(0xffffffff)
        self.reject(d, "legacy")

    def test_legacy_v2_v3_metadata(self):
        for version in ("v2", "v3"):
            d = legacy_fixture(version)
            r = mi.inspect(d)
            self.assertEqual(r["version"], version)
            self.assertEqual(r["groups"][0]["name"], "v0")
            self.assertFalse(r["decoded_payload_checksums_verified"])
            self.assertFalse(r["tensor_graph_decoded"])
            if version == "v3":
                self.assertAlmostEqual(r["parameters"][0]["payload"]["value"], .2, places=7)
                self.assertEqual(r["parameters"][1]["payload"]["value"], 256)
            for cut in range(len(d)):
                self.reject(d[:cut], "legacy")

    def test_legacy_envelope_field_corruption(self):
        d = legacy_fixture()
        # 16-byte version + size + name length/name + group count.
        group_count_offset = 24 + len(b"test_v1.0")
        for offset in (group_count_offset, group_count_offset + 4,
                       group_count_offset + 4 + 4 + 2):
            broken = bytearray(d)
            broken[offset:offset + 4] = word(0xffffffff)
            self.reject(broken, "legacy")
        broken = bytearray(d + b"extra")
        broken[16:20] = word(len(broken))
        self.reject(broken, "legacy")

    def test_bm_header_only(self):
        d = b"BM\x00\x02" + word(12) + b"xxxx"
        self.assertEqual(mi.inspect(d)["status"], "header_identified_payload_opaque")
        self.reject(d[:-1], "bm")

    def test_zip_crc_duplicate_missing_and_size(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "fixture.zip"
            data = fixture()
            with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_STORED) as z:
                z.writestr("model.bin", data)
            self.assertEqual(mi.read_input(path, "model.bin"), data)
            with self.assertRaises(mi.FormatError):
                mi.read_input(path, "missing")
            raw = bytearray(path.read_bytes())
            # Local header30 + filename9; mutate member bytes, retain stored CRC.
            raw[39 + len(data) - 1] ^= 1
            path.write_bytes(raw)
            with self.assertRaises(zipfile.BadZipFile):
                mi.read_input(path, "model.bin")
            with zipfile.ZipFile(path, "w") as z:
                z.writestr("model.bin", data)
                z.writestr("model.bin", data)
            with self.assertRaisesRegex(mi.FormatError, "duplicated"):
                mi.read_input(path, "model.bin")


PINS = {
    "tt_baoman": "e64f0772bb857e4d990789237c1007b62fb86020a7edcfb0c3a61a7bedc6e2c0",
    "tt_goodlike": "0d60ea7e684f32628daac031cb37fd32c50bf898aa3fb62bcbc25673b5725cad",
}


def real_models(directory, output):
    results = {name: mi.inspect(mi.read_input(directory / (name + ".model")), expected_sha256=pin)
               for name, pin in PINS.items()}
    groups = {g["name"]: g for g in results["tt_baoman"]["groups"]}
    assert set(groups) == {"inference_model", "face_align"}
    params = {p["key"]: p["payload"] for p in groups["face_align"]["parameters"]}
    assert params["target_width"]["value"] == params["target_height"]["value"] == 256
    assert params["crop_offset_x"]["value"] == 0 and params["crop_offset_y"]["value"] == 15
    model = next(p for p in groups["inference_model"]["parameters"] if p["key"] == "model_name")
    p = model["payload"]
    original = (directory / "tt_baoman.model").read_bytes()
    results["tt_baoman"]["embedded_model_header"] = mi.inspect(original[p["offset"]:p["offset"] + p["bytes"]])
    assert results["tt_goodlike"]["name_candidate"] == "tt_goodlike_v1.0"
    goodlike = results["tt_goodlike"]
    assert goodlike["version"] == "v3"
    assert [g["name"] for g in goodlike["groups"]] == ["v0", "face_point"]
    params = {p["key"]: p["payload"]["value"] for p in goodlike["parameters"]}
    assert params["CropWidthv0"] == params["CropHeightv0"] == 256
    assert params["CropOffsetXv0"] == params["CropOffsetYv0"] == 0
    result = {"status": "PASS_LOCAL_BYTES_METADATA_ONLY", "models": results,
              "device_model_identity_verified": False,
              "inference_executed": False,
              "limits": ["No model weights or raw binary are included in this report.",
                         "Face alignment dimensions describe a face crop, not the whole saved photo.",
                         "Type3 payload and legacy encoded/tail payloads remain opaque; tensor graph and HDR behavior unverified."]}
    output.write_text(json.dumps(result, indent=2, allow_nan=False) + "\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--models", type=Path)
    parser.add_argument("--output", type=Path, default=Path("QA_REAL_MODELS.json"))
    args = parser.parse_args()
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(InspectorTests)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    if not result.wasSuccessful():
        raise SystemExit(1)
    if args.models:
        real_models(args.models, args.output)
        print("PASS actual model metadata; no inference executed")
