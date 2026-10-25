"""Exact .113 shader delta for inherited source contracts only.

Actual GPU proofs compile current shader bytes. Restore is not a runtime path.
"""
from hashlib import sha256

BASELINE_SHA256 = "4f1bb151e3e49636d71db2abb6dad1b89d1a8572317af525069c306515605e36"
DELTA = [('#if GX_STRONG_MODE >= 0 && GX_STRONG_MODE < 3\n        // .100: this identical rounded quotient/square feeds both weights.\n        // Evaluate it once; each weight retains its original operation order.\n        precise float lumaRatioSquared19100=square(ieeeDiv1973((cq.x-cp.x),thresholdY));\n        precise float wy=ieeeDiv1973(base,(1.0+lumaRatioSquared19100));\n#else\n        precise float wy=ieeeDiv1973(base,(1.0+square(ieeeDiv1973((cq.x-cp.x),thresholdY))));\n#endif', '// BEGIN GX_FULL_RATIO19113\n#if (GX_STRONG_MODE >= 0 && GX_STRONG_MODE < 3) || (GX_STRONG_MODE == 3 && GX_POW2_EXACT78 && GX_TILE_WIDTH == 0)\n        // .113: extend exact reuse only to optional stripe/pow2 profile6.\n        // Standard profile4 and tile profile7 preserve the original code.\n        // Adaptive1998 requires full quality proof and a complete-path 10%\n        // GPU-versus-GPU speed win before profile6 may replace profile4.\n        // .100: this identical rounded quotient/square feeds both weights.\n        // Evaluate it once; each weight retains its original operation order.\n        precise float lumaRatioSquared19100=square(ieeeDiv1973((cq.x-cp.x),thresholdY));\n        precise float wy=ieeeDiv1973(base,(1.0+lumaRatioSquared19100));\n#else\n        precise float wy=ieeeDiv1973(base,(1.0+square(ieeeDiv1973((cq.x-cp.x),thresholdY))));\n#endif\n// END GX_FULL_RATIO19113'), ('#if GX_STRONG_MODE >= 0 && GX_STRONG_MODE < 3\n        precise float wc=ieeeDiv1973(base,(1.0+.12*lumaRatioSquared19100+square(ieeeDiv1973((cq.y-cp.y),thresholdC))+square(ieeeDiv1973((cq.z-cp.z),thresholdC))));\n#else\n        precise float wc=ieeeDiv1973(base,(1.0+.12*square(ieeeDiv1973((cq.x-cp.x),thresholdY))+square(ieeeDiv1973((cq.y-cp.y),thresholdC))+square(ieeeDiv1973((cq.z-cp.z),thresholdC))));\n#endif', '// BEGIN GX_FULL_CHROMA19113\n#if (GX_STRONG_MODE >= 0 && GX_STRONG_MODE < 3) || (GX_STRONG_MODE == 3 && GX_POW2_EXACT78 && GX_TILE_WIDTH == 0)\n        precise float wc=ieeeDiv1973(base,(1.0+.12*lumaRatioSquared19100+square(ieeeDiv1973((cq.y-cp.y),thresholdC))+square(ieeeDiv1973((cq.z-cp.z),thresholdC))));\n#else\n        precise float wc=ieeeDiv1973(base,(1.0+.12*square(ieeeDiv1973((cq.x-cp.x),thresholdY))+square(ieeeDiv1973((cq.y-cp.y),thresholdC))+square(ieeeDiv1973((cq.z-cp.z),thresholdC))));\n#endif\n// END GX_FULL_CHROMA19113')]

def extend_shader19113(text):
    for old,new in DELTA:
        if text.count(old)!=1:raise AssertionError('Strong .113 forward anchor mismatch')
        text=text.replace(old,new,1)
    return text

def restore_shader19113(text,required=False):
    if 'GX_FULL_RATIO19113' not in text:
        if required:raise AssertionError('Strong .113 delta absent')
        return text
    for old,new in DELTA:
        if text.count(new)!=1:raise AssertionError('Strong .113 inverse anchor mismatch')
        text=text.replace(new,old,1)
    if '19113' in text:raise AssertionError('Unreviewed Strong .113 delta remains')
    return text
