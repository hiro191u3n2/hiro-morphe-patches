"""Run both real models inside the supplied-landmark full-size SDR component."""
if not __debug__:
    raise RuntimeError("Validation requires assertions; run without -O or -OO")

import argparse
import hashlib
import json
from pathlib import Path
import sys

import numpy as np

from contracts import SDR_DOMAIN
from extract_natural_template import extract_template
from fullimage_component import (run_landmark_image_component, project_neural_crop,
                                 DEFAULT_BUDGET)
from style_mask import load_neural_mask, PINS


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("baoman",type=Path)
    p.add_argument("goodlike",type=Path)
    p.add_argument("effect_library",type=Path)
    p.add_argument("bytenn_library",type=Path)
    p.add_argument("output_qa",type=Path)
    p.add_argument("--natural-style",type=Path,required=True)
    p.add_argument("--purity-style",type=Path,required=True)
    p.add_argument("--large-raster-checks",action="store_true",
                   help="Also exercise 12.5/24.5 MP host projection; needs over 600 MB spare RAM")
    args=p.parse_args()
    sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"model_inspect"))
    from legacy_face_template import load_goodlike_face_template
    template=extract_template(args.effect_library)
    pt,pm=load_goodlike_face_template(args.goodlike,args.effect_library)
    np.testing.assert_array_equal(template,np.asarray(pt,dtype=np.float32))
    yy,xx=np.mgrid[:384,:512]
    source=np.stack((xx/512,yy/384,(xx+yy)/896),axis=-1).astype(np.float32)
    # Synthetic geometry with a known scale/rotation/translation. This does not
    # simulate face detection and is not a real person's photograph.
    lin=np.array([[130.,-10.],[10.,130.]])
    landmarks=template.astype(float)@lin.T+[170.,100.]
    results=[]
    large_results=[]
    for style,model,templ in (("natural_blush",args.baoman,template),
                              ("purity2",args.goodlike,pt)):
        archive=args.natural_style if style=="natural_blush" else args.purity_style
        mask=load_neural_mask(archive,style,vertical_flip=False)
        reverse=load_neural_mask(archive,style,vertical_flip=True)
        np.testing.assert_array_equal(reverse,mask[::-1])
        out=run_landmark_image_component(style,model,args.bytenn_library,args.effect_library,
                                         source,landmarks,templ,mask,.7,domain=SDR_DOMAIN)
        assert out.image.shape==source.shape
        assert out.image.dtype==np.float64 and np.isfinite(out.image).all()
        assert out.image.min()>=0 and out.image.max()<=1
        m=out.source_to_crop
        x=m[0,0]*xx+m[0,1]*yy+m[0,2]
        y=m[1,0]*xx+m[1,1]*yy+m[1,2]
        outside=(x<-.5)|(x>=255.5)|(y<-.5)|(y>=255.5)
        np.testing.assert_array_equal(out.image[outside],source[outside])
        changed=np.any(out.image!=source,axis=-1)
        assert changed.any() and not changed[outside].any()
        assert not out.native_pixel_parity and not out.complete_style and not out.hdr_preserved
        results.append({"style":style,"status":"PASS_REAL_MODEL_FULL_SIZE_SDR_COMPONENT",
                        "image_shape":list(out.image.shape),"output_precision":"float64",
                        "model_sha256":hashlib.sha256(model.read_bytes()).hexdigest(),
                        "mask_asset_sha256":PINS[style][2],
                        "mask_orientation":"explicit PNG-first-row to crop-upper-row; native upload parity unverified",
                        "generated_crop_sha256":hashlib.sha256(out.generated_crop.tobytes()).hexdigest(),
                        "output_sha256":hashlib.sha256(out.image.tobytes()).hexdigest(),
                        "outside_crop_samples_exactly_unchanged":int(outside.sum()),
                        "changed_inside_samples":int(changed.sum()),
                        "source_to_crop":m.tolist(),
                        "native_pixel_parity":False,"complete_style":False,"hdr_preserved":False})
        if args.large_raster_checks:
            for width,height in ((4080,3060),(5712,4284)):
                # A broadcast input intentionally avoids allocating an extra
                # full source raster, but projection allocates the real full
                # FP64 destination and processes the entire image. This is
                # neither a phone capture nor a native 24.5 MP input claim.
                bigsrc=np.broadcast_to(np.array([.123456789,.234567891,.345678912]),
                                       (height,width,3))
                left=(width-256)//2;top=(height-256)//2
                bigmatrix=np.array([[1.,0.,-left],[0.,1.,-top],[0.,0.,1.]])
                bigout=project_neural_crop(bigsrc,out.generated_crop,bigmatrix,mask,.7,
                                           style=style,domain=SDR_DOMAIN)
                assert bigout.shape==(height,width,3) and bigout.dtype==np.float64
                changed_inside=0;unchanged_outside=0
                for row in range(0,height,64):
                    stop=min(row+64,height)
                    coords_y=np.arange(row,stop)[:,None]
                    coords_x=np.arange(width)[None,:]
                    inside=((coords_y>=top)&(coords_y<top+256)&
                            (coords_x>=left)&(coords_x<left+256))
                    block=bigout[row:stop]
                    source_block=bigsrc[row:stop]
                    np.testing.assert_array_equal(block[~inside],source_block[~inside])
                    assert np.isfinite(block).all() and block.min()>=0 and block.max()<=1
                    changed_inside+=int(np.count_nonzero(np.any(block[inside]!=source_block[inside],axis=-1)))
                    unchanged_outside+=int(np.count_nonzero(~inside))
                assert changed_inside>0 and unchanged_outside==width*height-256*256
                large_results.append({"style":style,"shape":[height,width,3],
                                      "status":"PASS_FULL_HOST_PROJECTION",
                                      "allocated_output_bytes":bigout.nbytes,
                                      "outside_pixels_exactly_unchanged":unchanged_outside,
                                      "changed_inside_pixels":changed_inside,
                                      "input":"broadcast synthetic RGB values; actual model generated crop and pinned style mask",
                                      "native_camera_capture":False,"android_memory_test":False})
                del bigout
    report={"status":"PASS_BOTH_REAL_MODELS_SUPPLIED_LANDMARK_COMPONENT",
            "source":"synthetic full-size SDR image with supplied synthetic106geometry and pinned real stylemask",
            "geometry":"verified style-specific similarity/margin/offset matrix",
            "sampler":"continuous integer-lattice HQ; original CPU1/32fractions not reproduced",
            "projection":"explicit new integer-lattice convention; original GPU halfpixel parity unverified",
            "template_bytes_identical":True,"results":results,
            "large_raster_results":large_results,
            "host_budget":{"max_pixels":DEFAULT_BUDGET.max_pixels,
                           "max_output_bytes":DEFAULT_BUDGET.max_output_bytes,
                           "max_sample_points":DEFAULT_BUDGET.max_sample_points,
                           "total_process_memory_limited":False},
            "android_integrated":False,"full_style_complete":False,"hdr_preserved":False}
    args.output_qa.write_text(json.dumps(report,indent=2)+"\n")
    print(json.dumps(report,indent=2))


if __name__=="__main__":main()
