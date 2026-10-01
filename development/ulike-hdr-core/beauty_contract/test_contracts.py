import unittest

import numpy as np

from contracts import (SDR_DOMAIN, NATURAL_SCALE, NATURAL_MARGIN,
                       prepare_natural_tensor, prepare_purity_tensor,
                       model_output_unit_channels, composite_samples,
                       similarity_fit, natural_crop_matrix)


class Contracts(unittest.TestCase):
    def test_normalization_all_byte_codes(self):
        codes = np.broadcast_to(np.arange(256, dtype=np.float64)[None, :, None],
                                (256, 256, 3))
        n = prepare_natural_tensor(codes, domain=SDR_DOMAIN)
        p = prepare_purity_tensor(codes, domain=SDR_DOMAIN)
        np.testing.assert_array_equal(n[0, 0, 0],
                                      (np.arange(256) * NATURAL_SCALE - 1).astype(np.float32))
        np.testing.assert_array_equal(p[0, 0, 0],
                                      (np.arange(256, dtype=np.float32) - 127.5) * np.float32(1 / 127.5))
        self.assertEqual(n.shape, (1, 3, 256, 256))
        self.assertEqual(n.dtype, np.float32)
        self.assertLess(n[0, 0, 0, 255], 0.99)
        self.assertAlmostEqual(p[0, 0, 0, 255], 1.0)

    def test_fractional_codes_not_quantized(self):
        code = np.zeros((256, 256, 3), np.float64)
        code[:, :, 0] = 128.125
        code[:, :, 1] = 128.25
        code[:, :, 2] = 128.375
        t = prepare_natural_tensor(code, domain=SDR_DOMAIN)
        self.assertEqual(len(np.unique(t)), 3)
        self.assertTrue(t.flags.c_contiguous)

    def test_output_precision_and_layout(self):
        t = np.empty((1, 4, 256, 256), dtype=np.float32)
        t[0, 0] = -1
        t[0, 1] = 0
        t[0, 2] = 1
        t[0, 3] = np.float32(0.123456)
        out = model_output_unit_channels(t, domain=SDR_DOMAIN)
        np.testing.assert_array_equal(out[0, 0, :3], [0, 0.5, 1])
        self.assertEqual(out.dtype, np.float64)
        self.assertNotEqual(out[0, 0, 3] * 255, round(out[0, 0, 3] * 255))
        t[0, 0, 0, 0] = np.nextafter(np.float32(-1), np.float32(-np.inf))
        self.assertEqual(model_output_unit_channels(t, domain=SDR_DOMAIN)[0, 0, 0], 0)
        t[0, 0, 0, 0] = np.nextafter(t[0, 0, 0, 0], np.float32(-np.inf))
        with self.assertRaises(ValueError):
            model_output_unit_channels(t, domain=SDR_DOMAIN)

    def test_actual_shader_equations(self):
        rng = np.random.default_rng(168)
        src, gen = rng.random((31, 29, 4)), rng.random((31, 29, 4))
        mask = rng.random((31, 29))
        for name in ("natural_blush", "purity2"):
            out = composite_samples(name, src, gen, mask, .7, domain=SDR_DOMAIN)
            for y in range(31):
                for x in range(29):
                    alpha = (min(mask[y, x], gen[y, x, 3]) if name == "natural_blush"
                             else mask[y, x] * gen[y, x, 3]) * .7
                    for c in range(3):
                        self.assertAlmostEqual(out[y, x, c], src[y, x, c] * (1-alpha) + gen[y, x, c]*alpha)
            self.assertTrue(np.all(out[..., 3] == 1))
        n = composite_samples("natural_blush", src, gen, mask, .7, domain=SDR_DOMAIN)
        p = composite_samples("purity2", src, gen, mask, .7, domain=SDR_DOMAIN)
        self.assertGreater(np.max(np.abs(n-p)), .01)

    def test_similarity_against_independent_lstsq(self):
        rng = np.random.default_rng(42)
        for _ in range(30):
            src = rng.random((106, 2)) * 500
            dst = rng.random((106, 2))
            rows, values = [], []
            for (x, y), (u, v) in zip(src, dst):
                rows.extend([[x, -y, 1, 0], [y, x, 0, 1]])
                values.extend([u, v])
            a, b, tx, ty = np.linalg.lstsq(rows, values, rcond=None)[0]
            expected = [[a, -b, tx], [b, a, ty], [0, 0, 1]]
            np.testing.assert_allclose(similarity_fit(src, dst), expected, atol=1e-12, rtol=1e-10)

    def test_natural_padding_order(self):
        rng = np.random.default_rng(12)
        template = rng.random((106, 2))
        # A known source->template transform establishes direction without a
        # self-referential expected matrix from the implementation.
        source = (template - [0.1, -0.2]) / 0.002
        M = natural_crop_matrix(source, template)
        points = (M @ np.c_[source, np.ones(106)].T).T[:, :2]
        expected = (template + NATURAL_MARGIN) * (256 / (1+2*NATURAL_MARGIN)) + [0, 15]
        np.testing.assert_allclose(points, expected, atol=1e-12)

    def test_reject_invalid_contracts(self):
        face = np.zeros((256, 256, 3))
        for domain in ("HLG", "PQ", "linear_hdr", None):
            with self.assertRaises(ValueError):
                prepare_natural_tensor(face, domain=domain)
        for value in (-.01, 255.01, np.nan, np.inf):
            bad = face.copy(); bad[0, 0, 0] = value
            with self.assertRaises(ValueError):
                prepare_natural_tensor(bad, domain=SDR_DOMAIN)
        with self.assertRaises(ValueError):
            prepare_natural_tensor(face[:255], domain=SDR_DOMAIN)
        with self.assertRaises(ValueError):
            model_output_unit_channels(np.zeros((1, 3, 256, 256)), domain=SDR_DOMAIN)
        with self.assertRaises(ValueError):
            model_output_unit_channels(np.ones((1, 4, 256, 256))*1.01, domain=SDR_DOMAIN)
        with self.assertRaises(ValueError):
            similarity_fit(np.zeros((106, 2)), np.zeros((106, 2)))
        with self.assertRaises(ValueError):
            natural_crop_matrix(np.zeros((105, 2)), np.zeros((105, 2)))
        with self.assertRaises(ValueError):
            points = np.arange(212).reshape(106,2)
            natural_crop_matrix(points, points, margin=1e308)
        rgba = np.zeros((2, 3, 4)); mask = np.zeros((2, 3))
        for kwargs in ({"style":"other"}, {"mask_red":np.zeros((3,2))}, {"intensity":2.0}):
            args = dict(style="natural_blush", source_rgba=rgba, generated_rgba=rgba,
                        mask_red=mask, intensity=.5, domain=SDR_DOMAIN)
            args.update(kwargs)
            with self.assertRaises(ValueError): composite_samples(**args)


if __name__ == "__main__":
    unittest.main()
