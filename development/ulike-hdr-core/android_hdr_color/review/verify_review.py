#!/usr/bin/env python3
"""Independent BT.2100 reference fixtures + SDK36 compile/D8 + host Java review.

No downloaded images, models, Android phone or ORT execution are involved.
The Decimal and rational matrix references do not call production code.
"""
import argparse
from decimal import Decimal as D, localcontext
from fractions import Fraction as F
import hashlib
import json
import math
import os
from pathlib import Path
import struct
import subprocess
import tempfile

if not __debug__:
    raise RuntimeError("Assertions must remain enabled")
HERE = Path(__file__).resolve().parent
MODULE = HERE.parent
CORE = MODULE.parent

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def command(args):
    p = subprocess.run(list(map(str, args)), capture_output=True, text=True)
    if p.returncode:
        raise RuntimeError(p.stdout + "\n" + p.stderr)
    return p.stdout.strip()

def hlg(value):
    # High precision independent calculation of BT.2100 Table5 and declared extension.
    with localcontext() as context:
        context.prec = 55
        x = D(value.numerator) / D(value.denominator) if isinstance(value,F) else D(str(value))
        sign = -1 if x < 0 else 1
        x = abs(x)
        a = D(".17883277")
        out = x*x/3 if x <= D(".5") else (((x-(D(".5")-a*(4*a).ln()))/a).exp()+1-4*a)/12
        return float(out * sign)

TO_XYZ = [[F(63426534,99577255),F(20160776,139408157),F(47086771,278816314)],
          [F(26158966,99577255),F(472592308,697040785),F(8267143,139408157)],
          [F(0),F(19567812,697040785),F(295819943,278816314)]]
FROM_XYZ = [[F(12831,3959),-F(329,214),-F(1974,3959)],
            [-F(851781,878810),F(1648619,878810),F(36519,878810)],
            [F(705,12673),-F(2585,12673),F(705,667)]]
MATRIX = [[float(sum(FROM_XYZ[i][k]*TO_XYZ[k][j] for k in range(3)))
           for j in range(3)] for i in range(3)]
assert all(sum(FROM_XYZ[i][k]*TO_XYZ[k][j] for k in range(3) for j in range(3))==1 for i in range(3))

def chroma(codes, w, h, x, y, hx, vy):
    # Sum all tent-kernel supports, assigning outside support to the nearest border.
    qx=(F(x)-F(int(hx),2))/2
    qy=(F(y)-F(int(vy),2))/2
    out=F(0)
    for row in range(math.floor(qy)-1, math.floor(qy)+3):
        wy=max(F(0),1-abs(qy-row))
        for col in range(math.floor(qx)-1,math.floor(qx)+3):
            wx=max(F(0),1-abs(qx-col))
            out += wx*wy*codes[max(0,min(h-1,row))*w+max(0,min(w-1,col))]
    return out

def scene(w,h,limited,hx,vy,codes):
    out=[]
    for y in range(h):
        for x in range(w):
            yy=F(codes[0][y*w+x]-(64 if limited else 0),876 if limited else 1023)
            cb=(chroma(codes[1],w//2,h//2,x,y,hx,vy)-512)/(896 if limited else 1023)
            cr=(chroma(codes[2],w//2,h//2,x,y,hx,vy)-512)/(896 if limited else 1023)
            kr,kb=F(2627,10000),F(593,10000);kg=1-kr-kb
            rgb=(yy+2*(1-kr)*cr,
                 yy-2*kb*(1-kb)/kg*cb-2*kr*(1-kr)/kg*cr,
                 yy+2*(1-kb)*cb)
            out.extend(hlg(v) for v in rgb)
    return out

def sdr(rgb, exposure):
    luminance=sum(a*b for a,b in zip(rgb,(.2627,.678,.0593)))
    scaled=[v*exposure/(1+max(0,luminance)*exposure) for v in rgb]
    linear=[sum(a*b for a,b in zip(row,scaled)) for row in MATRIX]
    result=[]
    for v in linear:
        v=min(1,max(0,v))
        result.append(12.92*v if v<=.0031308 else 1.055*v**(1/2.4)-.055)
    packed=0
    for v in result:packed=(packed<<8)|int(v*255+.5)
    return packed,result

def fixtures(path):
    specs=[]
    for limited in (False,True):
        specs.append((1024,2,limited,False,False,[list(range(1024))*2,[512]*512,[512]*512]))
        w,h=8,6
        codes=[[(x*223+y*179)%1024 for y in range(h) for x in range(w)],
               [0,1023,70,900, 1000,17,723,340, 64,960,512,1023],
               [1023,0,902,140, 34,940,511,999, 960,64,800,0]]
        for hx in (False,True):
            for vy in (False,True):specs.append((w,h,limited,hx,vy,codes))
    with path.open("wb") as f:
        f.write(struct.pack(">i",len(specs)))
        for w,h,limited,hx,vy,codes in specs:
            f.write(struct.pack(">ii???",w,h,limited,hx,vy))
            for plane in codes:f.write(struct.pack(">"+"H"*len(plane),*plane))
            values=scene(w,h,limited,hx,vy,codes)
            f.write(struct.pack(">"+"d"*len(values),*values))
            exposures=(.000001,.25,1.0,1000000.0)
            f.write(struct.pack(">i",len(exposures)))
            for exposure in exposures:
                f.write(struct.pack(">d",exposure))
                for i in range(0,len(values),3):
                    packed,rgb=sdr(values[i:i+3],exposure)
                    f.write(struct.pack(">iddd",packed,*rgb))

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for arg in ("jdk","android-jar","ort-android-classes","r8"):p.add_argument("--"+arg,type=Path,required=True)
    p.add_argument("--report",type=Path,default=MODULE/"INDEPENDENT_REVIEW.json")
    a=p.parse_args()
    roots=[MODULE/"src",CORE/"hdr_input/src/main",CORE/"face_analysis/src/main",
           CORE/"android_beauty_image/src",CORE/"android_model_runtime/src"]
    production=sorted(f for root in roots for f in root.rglob("*.java"))
    inventory=production+[HERE/"ColorReview.java",Path(__file__).resolve()]
    before={str(f.relative_to(CORE)):sha(f) for f in inventory}
    with tempfile.TemporaryDirectory(prefix="ulike-independent-color-") as td:
        td=Path(td);classes=td/"classes";classes.mkdir();fixture=td/"fixtures.bin"
        fixtures(fixture)
        command([a.jdk/"javac","-source","8","-target","8","-bootclasspath",a.android_jar,
                 "-cp",a.ort_android_classes,"-d",classes,*production])
        jar=td/"production.jar";command([a.jdk/"jar","--create","--file",jar,"-C",classes,"."])
        dex=td/"dex";dex.mkdir()
        command([a.jdk/"java","-cp",a.r8,"com.android.tools.r8.D8","--min-api","26","--lib",a.android_jar,
                 "--classpath",a.ort_android_classes,"--output",dex,jar])
        command([a.jdk/"javac","--release","8","-cp",os.pathsep.join(map(str,[classes,a.ort_android_classes])),
                 "-d",classes,HERE/"ColorReview.java"])
        qa=json.loads(command([a.jdk/"java","-Xmx128m","-cp",classes,"review.ColorReview",fixture]))
        fixture_sha=sha(fixture)
    after={str(f.relative_to(CORE)):sha(f) for f in inventory}
    if before!=after:raise RuntimeError("Reviewed sources changed during execution")
    report={"status":"PASS_INDEPENDENT_COLOR_CONVERSION_SDK36_D8_HOST",
            "android_device_execution":False,"native_camera_color_parity":False,"complete_hdr_beauty":False,
            "master_domain":"relative scene-linear BT2020, explicit sign-reflected negative extension",
            "sdr_policy":"explicit scene-luminance Reinhard and sRGB gamut clip; no OOTF, not native ULike",
            "synthetic_fixtures":10,"fixture_sha256":fixture_sha,"host_review":qa,
            "sdk36_compile":True,"d8_min26":True,"host_java_heap_max_bytes":134217728,
            "reviewed_file_sha256":before,
            "tool_sha256":{str(f.name):sha(f) for f in (a.android_jar,a.ort_android_classes,a.r8,a.jdk/"javac",a.jdk/"java")},
            "coverage":["All1024 greys in full and limited ranges, full neutral512, limited64/940 anchors",
                        "Subblack, superwhite, chromaticnegative and >1 values retained in HDR",
                        "Independent 55-digit Decimal HLG reference and rational NCL/chroma equations",
                        "Allfour independent X/Y sitings, border extension, partial/padded rows",
                        "Invalid bounds reject before destination mutation, foreign-frame RGB8 rejection",
                        "Four exposure settings, fractional floatSDR separate from RGB8proxy",
                        "HDR planes unchanged after all conversions; borrowed input recycled before processing"],
            "primary_sources":[
                {"url":"https://www.itu.int/dms_pubrec/itu-r/rec/bt/R-REC-BT.2100-3-202502-I!!PDF-E.pdf","use":"Tables5,6,9: inverse OETF,NCL,full/limited quantization; note5h: preserve headroom"},
                {"url":"https://www.w3.org/TR/css-color-4/#color-conversion-code","use":"Exact rational matrices BT2020->XYZ D65 and XYZ->linear sRGB; sRGB encoding"},
                {"url":"https://docs.vulkan.org/spec/latest/chapters/textures.html#textures-chroma-reconstruction-explicit","use":"Cosited/midpoint explicit bilinear chroma positions"},
                {"url":"https://developer.android.com/reference/android/graphics/ImageFormat#YCBCR_P010","use":"P010 sample layout does not establish measured siting"}],
            "reproduction":"python review/verify_review.py --jdk <JDKbin> --android-jar <SDK36android.jar> --ort-android-classes <ORT1.30.0classes.jar> --r8 <r8-8.3.37.jar> --report INDEPENDENT_REVIEW.json"}
    a.report.write_text(json.dumps(report,indent=2)+"\n")
    print(json.dumps({"status":report["status"],**qa,"report":str(a.report)}))

if __name__=="__main__":main()
