import struct
import unittest
import legacy_face_template as template

class TemplateTests(unittest.TestCase):
    def fixture(self):
        return b'0.250000,0.750000,\n' * 106, struct.pack('<212f', *([0.25, 0.75] * 106))

    def test_template_pair(self):
        text, payload = self.fixture()
        self.assertEqual(template.validate_template_pair(text, payload), [(0.25, 0.75)] * 106)

    def test_bad_sizes_and_rows(self):
        text, payload = self.fixture()
        for a, b in [(text[:-1].rsplit(b'\n', 1)[0], payload), (text, payload[:-4]),
                     (text + b'0.0,1.0,\n', payload), (bytes(8193), payload)]:
            with self.assertRaises(ValueError):
                template.validate_template_pair(a, b)

    def test_mismatch_and_nonfinite(self):
        text, payload = self.fixture()
        for value in (0.25001, float('inf'), float('nan'), -4.1, 4.1):
            with self.assertRaises(ValueError):
                template.validate_template_pair(text, struct.pack('<f', value) + payload[4:])

    def test_invalid_text(self):
        text, payload = self.fixture()
        for bad in (b'nan,0.75,', b'0.25;0.75', b'0.25,0.75,0.1,', b'0.25 0.75', b'1e300,0.75,'):
            with self.assertRaises(ValueError):
                template.validate_template_pair(bad + b'\n' + b'\n'.join(text.splitlines()[1:]), payload)

if __name__ == '__main__':
    unittest.main()
