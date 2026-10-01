import unittest
from unittest.mock import patch

import numpy as np

from contracts import SDR_DOMAIN
from fullimage_component import (bilinear_samples, crop_image,
                                  project_neural_crop, purity_crop_matrix,
                                  HostBudget, DEFAULT_BUDGET)


class FullImageMath(unittest.TestCase):
    def test_bilinear_against_independent_scalar(self):
        image = np.arange(60,dtype=float).reshape(4,5,3)/59
        def scalar(x,y,c,border):
            if border == "clamp": x,y=min(4,max(0,x)),min(3,max(0,y))
            total=0
            for row in range(4):
                for col in range(5):
                    total += image[row,col,c]*max(0,1-abs(y-row))*max(0,1-abs(x-col))
            return total
        for border in ("zero","clamp"):
            for x,y in ((-.5,1.2),(-1,0),(4.5,3),(1.125,2.375),(3,0),(-50,1),(0,40)):
                expected=[scalar(x,y,c,border) for c in range(3)]
                np.testing.assert_allclose(bilinear_samples(image,x,y,border=border),expected,atol=1e-15)

    def test_identity_integer_centres(self):
        y,x=np.mgrid[:280,:300]
        src=np.stack((x/300,y/280,(x+y)/580),axis=-1)
        cropped=crop_image(src,np.eye(3))
        np.testing.assert_array_equal(cropped,src[:256,:256])

    def test_inverse_warp_direction_and_fractional_precision(self):
        y,x=np.mgrid[:300,:300]
        src=np.stack((x/300,y/300,(x+y)/600),axis=-1)
        m=np.array([[2.,0,10],[0,2.,15],[0,0,1.]])
        crop=crop_image(src,m)
        np.testing.assert_allclose(crop[31,41],[(41-10)/600,(31-15)/600,(41-10+31-15)/1200])
        self.assertTrue(np.all(crop[0,0]==0))
        # Different from 1/32 coordinate quantization at 0.01 subpixel shift.
        shifted=np.eye(3);shifted[0,2]=-.01
        crop2=crop_image(src,shifted)
        self.assertAlmostEqual(crop2[20,20,0],20.01/300)
        self.assertNotEqual(crop2[20,20,0],20/300)

    def test_zero_border_mixes_individual_taps(self):
        src=np.ones((1,1,3))
        np.testing.assert_array_equal(bilinear_samples(src,-.5,-.5,border="zero"),[.25]*3)

    def test_projection_outside_unchanged_and_tiles(self):
        rng=np.random.default_rng(16)
        source=rng.random((270,290,3)).astype(np.float32)
        generated=np.ones((256,256,4));generated[...,0]=.2
        mask=np.ones((320,320))
        for style in ("natural_blush","purity2"):
            out=project_neural_crop(source,generated,np.eye(3),mask,.7,style=style,domain=SDR_DOMAIN)
            out1=project_neural_crop(source,generated,np.eye(3),mask,.7,style=style,domain=SDR_DOMAIN,tile_rows=1)
            np.testing.assert_array_equal(out,out1)
            np.testing.assert_array_equal(out[256:,:],source[256:,:])
            np.testing.assert_array_equal(out[:,256:],source[:,256:])
            np.testing.assert_allclose(out[:256,:256],source[:256,:256].astype(float)*.3+generated[:,:,:3]*.7,atol=1e-15)
            zero=project_neural_crop(source,generated,np.eye(3),mask,0,style=style,domain=SDR_DOMAIN)
            np.testing.assert_array_equal(zero,source)

    def test_purity_model_specific_margin(self):
        rng=np.random.default_rng(99);template=rng.random((106,2))
        landmarks=template*200+[20,30]
        m=purity_crop_matrix(landmarks,template)
        xy=(m@np.c_[landmarks,np.ones(106)].T).T[:,:2]
        margin=float(np.float32(.2))
        np.testing.assert_allclose(xy,(template+margin)*256/(1+2*margin),atol=1e-12)

    def test_zero_weight_preserves_exact_samples_including_negative_zero(self):
        source=np.full((257,258,3),-.0)
        source[::2,::3]=.12345678901234567
        generated=np.ones((256,256,4));mask=np.zeros((320,320))
        for style in ("natural_blush","purity2"):
            for inactive in ("mask","alpha","intensity"):
                m=mask if inactive=="mask" else np.ones_like(mask)
                g=generated.copy()
                if inactive=="alpha":g[...,3]=0
                intensity=0 if inactive=="intensity" else .7
                out=project_neural_crop(source,g,np.eye(3),m,intensity,
                                        style=style,domain=SDR_DOMAIN)
                self.assertEqual(out.tobytes(),source.tobytes())

    def test_raster_budgets_reject_broadcast_views_before_pixel_scan(self):
        # These arrays have only a scalar backing allocation. Scanning or
        # allocating their logical shape would defeat pre-allocation rejection.
        oversized=np.broadcast_to(np.array(0.),(1,10**12,3))
        small=np.broadcast_to(np.array(0.),(20,20,3))
        cases=((oversized,DEFAULT_BUDGET),
               (small,HostBudget(max_pixels=399)),
               (small,HostBudget(max_output_bytes=20*20*3*8-1)))
        with patch("fullimage_component.np.isfinite",side_effect=AssertionError("pixel scan before budget rejection")):
            for image,budget in cases:
                with self.assertRaisesRegex(ValueError,"raster budget"):
                    crop_image(image,np.eye(3),budget=budget)
                with self.assertRaisesRegex(ValueError,"raster budget"):
                    bilinear_samples(image,0,0,border="zero",budget=budget)

    def test_sampler_broadcast_budget_checked_before_scan_or_conversion(self):
        src=np.ones((1,1,3))
        x=np.broadcast_to(np.array(0,dtype=np.uint64),(1,10**12))
        y=np.zeros((2,1))
        with patch("fullimage_component.np.isfinite",side_effect=AssertionError("scan before sampler limit")):
            with self.assertRaisesRegex(ValueError,"sampling budget"):
                bilinear_samples(src,x,y,border="zero")
            with self.assertRaisesRegex(ValueError,"sampling budget"):
                bilinear_samples(src,np.arange(4),0,border="zero",
                                 budget=HostBudget(max_output_bytes=95))

    def test_custom_sampling_budget_changes_tiles_without_changing_pixels(self):
        source=np.full((8,128,3),.123456789)
        gen=np.full((256,256,4),.625);mask=np.ones((320,320))
        kwargs=dict(style="purity2",domain=SDR_DOMAIN)
        reference=project_neural_crop(source,gen,np.eye(3),mask,.7,**kwargs)
        one_row=project_neural_crop(source,gen,np.eye(3),mask,.7,
                                    budget=HostBudget(max_sample_points=128),**kwargs)
        np.testing.assert_array_equal(reference,one_row)
        with self.assertRaisesRegex(ValueError,"One image row"):
            project_neural_crop(source,gen,np.eye(3),mask,.7,
                                budget=HostBudget(max_sample_points=127),**kwargs)

    def test_invalid_budgets(self):
        for kwargs in ({"max_pixels":0},{"max_output_bytes":-1},
                       {"max_sample_points":True},{"max_pixels":2.5}):
            with self.assertRaises(ValueError):HostBudget(**kwargs)
        with self.assertRaises(ValueError):crop_image(np.ones((1,1,3)),np.eye(3),budget=None)

    def test_invalid_transform_domain_and_masks(self):
        src=np.zeros((4,4,3));gen=np.zeros((256,256,4));mask=np.zeros((320,320))
        for m in (np.zeros((3,3)),np.ones((2,3)),np.eye(3)*np.nan,
                  [[1,0,0],[0,1,0],[1,0,1]],[[1,0,0],[0,1e-15,0],[0,0,1]]):
            with self.assertRaises(ValueError):crop_image(src,m)
        for changes in ({"domain":"HLG"},{"neural_mask_red":np.zeros((256,256))},
                        {"intensity":np.nan},{"tile_rows":0},{"style":"unknown"}):
            kw=dict(image=src,generated_rgba=gen,source_to_crop=np.eye(3),neural_mask_red=mask,
                    intensity=.7,style="natural_blush",domain=SDR_DOMAIN)
            kw.update(changes)
            with self.assertRaises(ValueError):project_neural_crop(**kw)
        with self.assertRaises(ValueError):crop_image(np.full((4,4,3),2.),np.eye(3))


if __name__ == "__main__": unittest.main()
