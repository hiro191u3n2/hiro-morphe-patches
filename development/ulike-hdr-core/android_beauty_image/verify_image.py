#!/usr/bin/env python3
"""Compile Android production classes; compare host Java against frozen photo math.

All weights, models, extracted templates/masks and photo tensors remain in a
temporary directory outside the public source tree. No Android execution claim.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile

if not __debug__:
    raise RuntimeError("Do not disable verification assertions")
os.environ["ORT_DISABLE_TELEMETRY"]="1"
ROOT=Path(__file__).resolve().parent

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def run(args):
    p=subprocess.run(list(map(str,args)),text=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    if p.returncode:raise RuntimeError(p.stdout+"\n"+p.stderr)
    return p.stdout.strip()

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ("jdk","android-jar","ort-android-classes","ort-host-jar","r8","baoman","goodlike","bytenn","effect","natural-zip","purity-zip"):
        p.add_argument("--"+name,type=Path,required=True)
    p.add_argument("--report",type=Path)
    a=p.parse_args()
    for sibling in ("beauty_contract","model_replay","model_inspect"):
        sys.path.insert(0,str(ROOT.parent/sibling))
    import numpy as np
    from fullimage_component import run_neural_image_component,natural_crop_matrix,purity_crop_matrix
    from style_mask import load_neural_mask
    from extract_natural_template import extract_template
    from contracts import SDR_DOMAIN
    runtime=ROOT.parent/"android_model_runtime"
    sources=sorted((ROOT/"src").rglob("*.java"))+sorted((runtime/"src").rglob("*.java"))
    tests=sorted((ROOT/"test").rglob("*.java"))
    report={"status":"PASS_ANDROID_SDK36_COMPILE_HOST_JAVA_FULL_IMAGE_COMPONENT","android_device_execution":False,"hdr_preserved":False,"complete_style":False,"native_pixel_parity":False,"caller_same_frame_landmarks_required":True,"input_and_output_dimensions":[512,384],"model_dimensions":[256,256],"telemetry_opt_out_before_runtime":True,"cases":[]}
    with tempfile.TemporaryDirectory(prefix="ulike-beauty-java-") as tmp:
        tmp=Path(tmp);classes=tmp/"classes";classes.mkdir();production=tmp/"production";production.mkdir()
        run([a.jdk/"javac","-source","8","-target","8","-bootclasspath",a.android_jar,"-cp",a.ort_android_classes,"-d",production,*sources])
        run([a.jdk/"javac","-source","8","-target","8","-bootclasspath",a.android_jar,"-cp",a.ort_android_classes,"-d",classes,*sources])
        run([a.jdk/"javac","--release","8","-cp",os.pathsep.join(map(str,[classes,a.ort_android_classes])),"-d",classes,*tests])
        jar=tmp/"production.jar";run([a.jdk/"jar","--create","--file",jar,"-C",production,"."])
        dex=tmp/"dex";dex.mkdir();run([a.jdk/"java","-cp",a.r8,"com.android.tools.r8.D8","--min-api","26","--lib",a.android_jar,"--classpath",a.ort_android_classes,"--output",dex,jar]);report["sdk36_compile_and_d8_min26"]=True
        fixtures=tmp/"fixtures";fixtures.mkdir();y,x=np.mgrid[:384,:512]
        # Synthetic fractional RGB: not a portrait or real camera capture.
        image=np.stack((x/511,y/383,.35+.2*np.sin(x*.037)*np.cos(y*.051)),axis=-1).astype(np.float32)
        image[::11,::13,0]=np.float32(-0.0)
        image.astype("<f4").tofile(fixtures/"source.bin")
        matrices={"affine":np.array([[.86,.073,-51],[-.073,.86,-36],[0,0,1.]]),"rotated":np.array([[.19,-.94,260],[.94,.19,-112],[0,0,1.]]),"reflected":np.array([[-.93,.08,365],[.08,.93,-42],[0,0,1.]]),"border":np.array([[1.11,0,92],[0,1.11,65],[0,0,1.]])}
        for name,matrix in matrices.items():matrix.astype("<f8").tofile(fixtures/(name+".bin"))
        template=extract_template(a.effect).astype(np.float64)
        landmarks=template@np.array([[242,31],[-31,242.]])+np.array([104,72])
        landmarks.astype("<f8").tofile(fixtures/"landmarks.bin")
        for name,java_style,model,archive in (("natural_blush","NATURAL_BLUSH",a.baoman,a.natural_zip),("purity2","PURITY2",a.goodlike,a.purity_zip)):
            out=tmp/name
            message=run([a.jdk/"java","-Xmx384m","-cp",os.pathsep.join(map(str,[classes,a.ort_host_jar])),"hiro.ulike.beauty.RunImage",java_style,model,a.bytenn,a.effect,archive,fixtures,out])
            print(message,flush=True)
            mask=load_neural_mask(archive,name,vertical_flip=False)
            assert np.array_equal(np.fromfile(out/"mask.bin",dtype="<f8").reshape(320,320),mask)
            assert np.array_equal(np.fromfile(out/"mask-flipped.bin",dtype="<f8").reshape(320,320),mask[::-1])
            assert np.array_equal(np.fromfile(out/"template.bin",dtype="<f8").reshape(106,2),template)
            expected_matrix=natural_crop_matrix(landmarks,template) if name=="natural_blush" else purity_crop_matrix(landmarks,template)
            java_matrix=np.fromfile(out/"matrix.bin",dtype="<f8").reshape(3,3)
            assert np.max(np.abs(expected_matrix-java_matrix))<5e-12
            cases=[]
            for fixture,matrix in list(matrices.items())+[("landmarks",expected_matrix)]:
                expected=run_neural_image_component(name,model,a.bytenn,a.effect,image,matrix,mask,.63,domain=SDR_DOMAIN).image
                actual=np.fromfile(out/(fixture+".bin"),dtype="<f4").reshape(image.shape)
                difference=np.abs(actual.astype(np.float64)-expected)
                # Float sink rounds FP64 composite once; same source and model arithmetic.
                rounding_bound=.5*np.abs(np.spacing(expected.astype(np.float32)).astype(np.float64))+1e-12
                assert np.all(difference<=rounding_bound),(name,fixture,float(difference.max()))
                cases.append({"fixture":fixture,"sample_values":int(actual.size),"maximum_absolute_error":float(difference.max()),"output_sha256":digest(out/(fixture+".bin"))})
                print(name,fixture,"max_abs",float(difference.max()),flush=True)
            report["cases"].append({"style":name,"host_guards":message,"raw_mask_and_template_bit_exact":True,"landmark_matrix_max_abs":float(np.max(np.abs(expected_matrix-java_matrix))),"comparisons":cases})
    report["source_sha256"]={str(f.relative_to(ROOT)):digest(f) for f in sorted(ROOT.rglob("*.java"))+[Path(__file__).resolve()]}
    report["runtime_source_sha256"]={str(f.relative_to(runtime)):digest(f) for f in sorted((runtime/"src").rglob("*.java"))}
    report["dependency_sha256"]={f.name:digest(f) for f in (a.android_jar,a.ort_android_classes,a.ort_host_jar,a.r8)}
    report["workspace"]={"max_pixels":25000000,"maximum_dimension":16384,"default_tile_rows":8,"module_java_array_budget_bytes":8*1024*1024,"full_photo_array_allocated_by_component":False,"excluded":["caller-owned source and transactional sink storage","compiled model loading and graph buffers","ORT direct input and native activation allocations","JVM object overhead"]}
    text=json.dumps(report,indent=2)+"\n"
    if a.report:a.report.write_text(text)
    else:print(text)

if __name__=="__main__":main()
