import struct
import unittest
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
import legacy_graph_metadata as legacy

class LegacyTests(unittest.TestCase):
    def encode(self, plain, key):
        padding = b'\0' * ((-len(plain)) % 16)
        encoder = Cipher(algorithms.AES(key), modes.ECB()).encryptor()
        return encoder.update(plain + padding) + encoder.finalize() + struct.pack('<I', len(plain))

    def test_lengths_and_key_sizes(self):
        for size in (16, 24, 32):
            key = bytes(range(size))
            for length in (1, 15, 16, 17, 31, 32, 63, 64, 65):
                plain = bytes((x * 17 + 5) % 256 for x in range(length))
                self.assertEqual(legacy.decode_ecb_blob(self.encode(plain, key), key), plain)

    def test_invalid_blocks(self):
        for length in (0, 1, 4, 16, 19, 21, 31, 35, 37):
            with self.assertRaises(ValueError):
                legacy.decode_ecb_blob(b'\0' * length, bytes(32))

    def test_declared_length_bounds(self):
        for n in (0, 17, 0x7fffffff, 0xffffffff):
            with self.assertRaises(ValueError):
                legacy.decode_ecb_blob(bytes(16) + struct.pack('<I', n), bytes(32))
        with self.assertRaises(ValueError):
            legacy.decode_ecb_blob(bytes(32) + struct.pack('<I', 16), bytes(32))

    def test_oversized_rejected(self):
        with self.assertRaises(ValueError):
            legacy.decode_ecb_blob(bytes(legacy.MAX_DECODE_BYTES + 32) + struct.pack('<I', 1), bytes(32))

    def test_invalid_material(self):
        for length in (0, 1, 15, 17, 31, 33, 48):
            with self.assertRaises(ValueError):
                legacy.decode_ecb_blob(bytes(16) + struct.pack('<I', 16), bytes(length))

    def test_unknown_library_rejected_before_instruction_read(self):
        for value in (b'', bytes(4), b'not the library'):
            with self.assertRaises(ValueError):
                legacy._loader_material(value)

    def test_checksum_known_fixture(self):
        self.assertEqual(legacy.native_graph_checksum(b''), 0x4e67c6a7)
        self.assertEqual(legacy.native_graph_checksum(b'abc'), 446371745)

if __name__ == '__main__':
    unittest.main()
