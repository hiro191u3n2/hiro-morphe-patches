#!/usr/bin/env python3
"""Independent HDR appearance math, owned-frame processing, SDK36 and D8 review.

Synthetic numerical fixtures and synthetic generated crops only. This review
does not execute ORT, a phone camera, Android MediaCodec or an HDR display.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
from reference import fixtures, COLOR_REFERENCE

HERE=Path(__file__).resolve().parent
MODULE=HERE.parent
CORE=MODULE.parent

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def run(args):
    result=subprocess.run(list(map(str,args)),capture_output=True,text=True)
    if result.returncode:raise RuntimeError(result.stdout+"\n"+result.stderr)
    return result.stdout.strip()

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ("jdk-bin","android-jar","ort-android-classes","r8"):parser.add_argument("--"+name,type=Path,required=True)
    parser.add_argument("--report",type=Path,default=MODULE/"INDEPENDENT_REVIEW.json")
    args=parser.parse_args()
    roots=[MODULE/"src",CORE/"android_hdr_color/src",CORE/"hdr_input/src/main",CORE/"face_analysis/src/main",
           CORE/"android_beauty_image/src",CORE/"android_model_runtime/src",CORE/"style_pipeline/src/main"]
    production=sorted(f for root in roots for f in root.rglob("*.java"))
    review=[HERE/"AppearanceReview.java",HERE/"ProcessorReview.java",HERE/"reference.py",Path(__file__).resolve(),COLOR_REFERENCE]
    inventory=production+review
    before={str(path.relative_to(CORE)):sha(path) for path in inventory}
    with tempfile.TemporaryDirectory(prefix="ulike-independent-hdr-beauty-") as name:
        temp=Path(name);classes=temp/"classes";classes.mkdir();fixture=temp/"fixtures.bin"
        fixture_count=fixtures(fixture)
        cp=os.pathsep.join(map(str,(args.android_jar,args.ort_android_classes)))
        run([args.jdk_bin/"javac","--release","8","-cp",cp,"-d",classes,*production])
        jar=temp/"reviewed-production.jar";run([args.jdk_bin/"jar","--create","--file",jar,"-C",classes,"."])
        dex=temp/"dex";dex.mkdir()
        run([args.jdk_bin/"java","-cp",args.r8,"com.android.tools.r8.D8","--min-api","26","--lib",args.android_jar,
             "--classpath",args.ort_android_classes,"--output",dex,jar])
        host_cp=os.pathsep.join(map(str,(classes,args.ort_android_classes)))
        run([args.jdk_bin/"javac","--release","8","-cp",host_cp,"-d",classes,HERE/"AppearanceReview.java",HERE/"ProcessorReview.java"])
        math=json.loads(run([args.jdk_bin/"java","-Xmx128m","-cp",host_cp,"review.AppearanceReview",fixture]))
        processor=json.loads(run([args.jdk_bin/"java","-Xmx128m","-cp",host_cp,"hiro.ulike.beauty.ProcessorReview"]))
        fixture_sha=sha(fixture)
    after={str(path.relative_to(CORE)):sha(path) for path in inventory}
    if before!=after:raise RuntimeError("Reviewed source changed during execution; rerun on frozen files")
    report={
        "status":"PASS_INDEPENDENT_HDR_APPEARANCE_AND_LAYERED_PROCESSING",
        "android_device_execution":False,"native_hdr_style_equivalence":False,
        "hdr_trained_neural_model":False,"full_captured_hdr_information_retained_under_generated_patch":False,
        "actual_onnx_execution_in_this_review":False,"complete_app_integration_verified_by_this_review":False,
        "sdk36_compile":True,"d8_min26":True,"host_heap_limit_bytes":134217728,
        "production_java_files":len(production),"independent_reference_fixtures":fixture_count,
        "fixture_sha256":fixture_sha,"appearance_math":math,"owned_frame_processor":processor,
        "scope":"Declared high-precision HDR appearance replacement using SDR-authored model/style layers; not recovered native ULike HDR",
        "policy":[
            "BT.2100 HLG scene->display OOTF: 1000-nit reference, gamma1.2, black0, display RGB divided by203nits",
            "Source negative BT2020 explicitly desaturated preserving positive Y; nonpositive Y maps to black; source frame immutable",
            "Generated patch alone embedded by global inverse declared Reinhard+OOTF, bounded by explicit peak; no captured residual or old gain ratio",
            "Remaining effects sampled full strength in normalized sRGB of CURRENT edited HDR, then source-over using actual layer weight in display light",
            "Explicit matched external HDR source used for Purity blusher/3D; no SDR-only base accepted",
            "Final SDR derived from fully processed HDR; only a later fresh paired save may make a gainmap"],
        "coverage":[
            "All1024 full/limited grey codes; subblack/superwhite and chromatic-negative scene fixtures",
            "15721 generated-patch fixtures across five exposures and three peak bounds; whites, near-whites, saturated and fractional samples",
            "Independent 60-digit Decimal nonlinear math and exact rational W3C matrices",
            "Opaque neural patch independent of original pixel luminance; half-weight linear blend; zero-weight exact identity",
            "Same P010 frame object/rendition and raster; frame planes unchanged after original camera buffers recycled",
            "Owned layer copies and actual neural intensity fingerprint; private transaction failures never commit",
            "Tiny LUT strength remains continuous for colors outside sRGB; explicit wide-gamut Purity 3D base survives zero intensity",
            "Foreign frame/settings/geometry, incomplete/repeated passes, missing or SDR-only external HDR base reject",
            "Partial final tiles and tile-height invariance; budget rejection before staging"],
        "reviewed_file_sha256":before,
        "tool_sha256":{path.name:sha(path) for path in (args.android_jar,args.ort_android_classes,args.r8,args.jdk_bin/"java",args.jdk_bin/"javac")},
        "primary_sources":[
            {"url":"https://www.itu.int/dms_pubrec/itu-r/rec/bt/R-REC-BT.2100-3-202502-I!!PDF-E.pdf","use":"HLG inverse OETF distinct from scene->display OOTF, 1000-nit gamma1.2 reference"},
            {"url":"https://www.itu.int/dms_pub/itu-r/opb/rep/R-REP-BT.2408-6-2023-PDF-E.pdf","use":"HDR reference white203nits; project explicitly uses this normalization"},
            {"url":"https://www.w3.org/TR/css-color-4/#color-conversion-code","use":"Rational D65 color matrices and sRGB transfer function"},
            {"url":"https://www.w3.org/TR/compositing-1/#simplealphacompositing","use":"Source-over algebra; use in HDR display-linear units is an explicit project extension"},
            {"url":"https://developer.apple.com/videos/play/wwdc2024/10177/","use":"Brightness-changing HDR edits require a newly matched SDR/HDR pair and gainmap; old map cannot simply be reused"}],
        "reproduction":"python review/verify_review.py --jdk-bin <JDK21bin> --android-jar <SDK36android.jar> --ort-android-classes <ORT1.30.0classes.jar> --r8 <r8-8.3.37.jar>"
    }
    args.report.write_text(json.dumps(report,indent=2)+"\n")
    print(json.dumps({"status":report["status"],"appearance_math":math,"processor":processor,"report":str(args.report)}))

if __name__=="__main__":main()
