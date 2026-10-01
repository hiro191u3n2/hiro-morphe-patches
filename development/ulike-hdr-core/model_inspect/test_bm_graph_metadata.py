#!/usr/bin/env python3
"""Synthetic format tests: no actual graph, weights or table embedded."""
import struct
import unittest

import bm_graph_metadata as bm
import model_inspector as mi


def fixture():
    text = b"synthetic graph only\n\0"
    salt = bytes(range(8))
    encoded = bytes(value ^ salt[i % 8] for i, value in enumerate(text))
    weights = struct.pack("<fI", .5, 42)
    data = b"BM\0\2" + struct.pack("<8I", 36 + len(text) + len(weights) + 8,
            3, len(text), 36, len(weights), 36 + len(text), 0, 36 + len(text) + len(weights))
    return data + encoded + weights + salt, text


class Tests(unittest.TestCase):
    def test_decode(self):
        data, text = fixture()
        decoded, metadata = bm.decode_v2(data, bytes(range(256)))
        self.assertEqual(decoded, text)
        self.assertEqual(metadata["weight_values_bytes"], 4)
        self.assertEqual(metadata["weight_tail_marker_u32"], 42)

    def test_truncation_and_boundary(self):
        data, _ = fixture()
        for cut in range(len(data)):
            broken = bytearray(data[:cut])
            if len(broken) >= 8:
                broken[4:8] = struct.pack("<I", len(broken))
            with self.assertRaises(mi.FormatError):
                bm.decode_v2(broken, bytes(range(256)))
        for off, value in [(8, 4), (12, 0xffffffff), (16, 0xffffffff),
                           (20, 2), (24, 0xffffffff), (32, 0xffffffff)]:
            broken = bytearray(data)
            struct.pack_into("<I", broken, off, value)
            with self.assertRaises(mi.FormatError):
                bm.decode_v2(broken, bytes(range(256)))

    def test_bad_table_and_text(self):
        data, _ = fixture()
        for table in (bytes(256), bytes(255), bytes(257)):
            with self.assertRaises(mi.FormatError):
                bm.decode_v2(data, table)
        broken = bytearray(data)
        broken[36] = 255
        with self.assertRaises(mi.FormatError):
            bm.decode_v2(broken, bytes(range(256)))


if __name__ == "__main__":
    unittest.main()
