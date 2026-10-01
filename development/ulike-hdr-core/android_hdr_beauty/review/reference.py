"""Independent high-precision reference for the DECLARED HDR replacement policy.

Only BT.2100 transfer/OOTF and W3C color matrices are standard equations.
The bounded generated-image embedding, gamut policy, and final SDR mapping
are project appearance choices; they do not recover clipped native HDR data.
"""
from decimal import Decimal as D, localcontext
from fractions import Fraction as F
from pathlib import Path
import importlib.util
import math
import random
import struct

COLOR_REFERENCE = Path(__file__).resolve().parents[2] / "android_hdr_color/review/verify_review.py"
spec = importlib.util.spec_from_file_location("color_reference", COLOR_REFERENCE)
color = importlib.util.module_from_spec(spec)
spec.loader.exec_module(color)

def invert3(a):
    # Exact Gauss-Jordan inverse of independent rational standard matrices.
    rows=[list(a[i])+[F(int(i==j)) for j in range(3)] for i in range(3)]
    for i in range(3):
        pivot=rows[i][i]
        rows[i]=[v/pivot for v in rows[i]]
        for j in range(3):
            if i!=j:
                factor=rows[j][i]
                rows[j]=[v-factor*w for v,w in zip(rows[j],rows[i])]
    return [r[3:] for r in rows]

BT2020_TO_SRGB = [[sum(color.FROM_XYZ[i][k]*color.TO_XYZ[k][j] for k in range(3))
                  for j in range(3)] for i in range(3)]
SRGB_TO_BT2020 = invert3(BT2020_TO_SRGB)

def dec(v):
    if isinstance(v,F): return D(v.numerator)/D(v.denominator)
    if isinstance(v,float): return D.from_float(v)
    return D(v)

def mat(m,rgb):
    return [sum(dec(a)*b for a,b in zip(row,rgb)) for row in m]

def power(x,p):
    return (x.ln()*p).exp() if x else D(0)

def srgb(code):
    return code/D("12.92") if code<=D(".04045") else power((code+D(".055"))/D("1.055"),D("2.4"))

def appearance(kind,values,exposure=1,peak=1000):
    with localcontext() as ctx:
        ctx.prec=60
        rgb=list(map(dec,values));exposure=dec(exposure);peak=dec(peak)
        k=D(1000)/203
        flags=0
        if kind==0:
            y=sum(v*w for v,w in zip(rgb,map(D,(".2627",".678",".0593"))));low=min(rgb)
            if y<=0:
                flags=2 if any(rgb) else 0
                rgb=[D(0)]*3
            else:
                if low<0:
                    flags=1
                    t=y/(y-low)
                    rgb=[max(D(0),y+(v-y)*t) for v in rgb]
                rgb=[v*k*power(y,D(".2")) for v in rgb]
        elif kind==1:
            rgb=mat(SRGB_TO_BT2020,[srgb(v) for v in rgb])
            y=sum(v*w for v,w in zip(rgb,map(D,(".2627",".678",".0593"))))
            if y<=0:
                rgb=[D(0)]*3
            else:
                cap=peak/203
                wanted=None if y>=1 else k*power(y,D(".2"))/power(exposure*(1-y),D("1.2"))
                capscale=cap/max(rgb)
                flags=int(wanted is None or wanted>=capscale)
                scale=capscale if flags else wanted
                rgb=[v*scale for v in rgb]
        elif kind==2:
            rgb=mat(BT2020_TO_SRGB,rgb);low=min(rgb)
            if low<0:
                flags=1
                # Y is exact second row of sRGB->XYZ; conversion is D65.
                lum=sum(v*w for v,w in zip(rgb, map(dec,(F(87098,409605),F(175762,245763),F(12673,175545)))) )
                rgb=[D(0)]*3 if not lum else [max(D(0),lum+(v-lum)*lum/(lum-low)) for v in rgb]
        elif kind==3:
            scale=max(D(1),*rgb)
            rgb=[v/scale for v in rgb]
        elif kind==4:
            rgb=mat(SRGB_TO_BT2020,rgb)
        else:
            raise AssertionError(kind)
        return flags,list(map(float,rgb))

def fixtures(path):
    records=[]
    def add(kind,rgb,exposure=1,peak=1000,storage=10000):
        flags,result=appearance(kind,rgb,exposure,peak)
        records.append((kind,*rgb,exposure,peak,storage,flags,*result))
    for limited in (False,True):
        for code in range(1024):
            linear=color.hlg(F(code-(64 if limited else 0),876 if limited else 1023))
            add(0,(linear,)*3)
    for rgb in ((0,0,0),(-1,0,0),(-1,-1,-1),(-.1,.7,.3),(1,-.1,.3),(.1,.1,-.1),
                (-.1,1.0,2.0),(1.5,1.2,.5),(.26496256042100724,)*3,(2,2,2)):
        add(0,rgb)
    for exposure in (1e-6,.25,1.0,4.0,1e6):
        for peak in (203,1000,10000):
            for i in range(1025):add(1,(i/1024.0,)*3,exposure,peak)
            for rgb in ((1,0,0),(0,1,0),(0,0,1),(1,.5,0),(.03,.05,.2),(.999999,.99999,1)):
                add(1,rgb,exposure,peak)
    rng=random.Random(73816295)
    for i in range(256):
        display=tuple(rng.random()*(10000/203) for _ in range(3))
        add(2,display);add(3,display)
        add(4,tuple(rng.random() for _ in range(3)))
        add(1,tuple(rng.randrange(65536)/65535.0 for _ in range(3)))
    for rgb in ((0,0,0),(1,1,1),(1,0,0),(0,1,0),(0,0,1),(10000/203,)*3,(.1,.2,.3)):
        add(2,rgb);add(3,rgb);add(4,rgb)
    with path.open("wb") as output:
        output.write(struct.pack(">i",len(records)))
        for record in records:output.write(struct.pack(">i6di3d",*record))
    return len(records)
