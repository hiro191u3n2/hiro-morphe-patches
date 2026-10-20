"""Invert only the exact reviewed .107 region delta and .106 diagnostic blocks.

Used solely by inherited historical source contracts. Every actual JNI/GLES
execution still compiles the unmodified current shader. The .107 native inverse
proof independently pins the complete helper/call, preserves all other bytes,
and rebuilds the complete published .106 ELF; arithmetic is tested separately.
"""
import re
import hashlib


REGIONAL_BEGIN='// BEGIN GX_REGIONAL_RNE19107\n'
REGIONAL_END='// END GX_REGIONAL_RNE19107\n'
REGIONAL_BLOCK_SHA256='0686fcce964681d3f8d4b6cb48a063e2ea869214ca04576b4c8748ba043676ed'
REGIONAL_BEFORE='    values[plane]=(a*(1.0-fx)+b*fx)*(1.0-fy)+(c*(1.0-fx)+d*fx)*fy;\n'
REGIONAL_AFTER='    values[plane]=uintBitsToFloat(regionalInterpolateBits19107(floatBitsToUint(a),floatBitsToUint(b),floatBitsToUint(c),floatBitsToUint(d),regionalWeight19107(fx),regionalWeight19107(fy)));\n'

def restore_regional19107(text,required=False):
    """Restore exactly the reviewed single helper/call; reject partial deltas."""
    if '19107' not in text:
        if required:raise AssertionError('Regional .107 delta required')
        return text
    if text.count(REGIONAL_BEGIN)!=1 or text.count(REGIONAL_END)!=1:
        raise AssertionError('Regional helper marker count changed')
    start=text.index(REGIONAL_BEGIN);finish=text.index(REGIONAL_END)+len(REGIONAL_END)
    block=text[start:finish]
    if hashlib.sha256(block.encode()).hexdigest()!=REGIONAL_BLOCK_SHA256:
        raise AssertionError('Regional helper bytes changed')
    if text.count(REGIONAL_AFTER)!=1 or text.count(REGIONAL_BEFORE)!=0:
        raise AssertionError('Regional call delta changed')
    restored=(text[:start]+text[finish:]).replace(REGIONAL_AFTER,REGIONAL_BEFORE,1)
    if '19107' in restored:raise AssertionError('Unreviewed regional delta remains')
    return restored


def production_text(text):
    text=restore_regional19107(text)
    default='#ifndef GX_TRACE19106\n#define GX_TRACE19106 0\n#endif\n'
    if text.count(default)>1:raise AssertionError('Duplicate diagnostic default')
    text=text.replace(default,'',1)
    result=[];depth=0;keep=False;blocks=0
    for line in text.splitlines():
        directive=line.strip()
        if depth==0:
            if directive=='#if GX_TRACE19106':
                depth=1;keep=False;blocks+=1
            else:
                result.append(line)
            continue
        if re.match(r'^#\s*(if|ifdef|ifndef)\b',directive):
            depth+=1
            if keep:result.append(line)
        elif re.match(r'^#\s*endif\b',directive):
            depth-=1
            if depth and keep:result.append(line)
        elif depth==1 and directive=='#else':
            keep=True
        elif depth==1 and directive.startswith('#elif'):
            raise AssertionError('Unreviewed diagnostic conditional alternative')
        elif keep:
            result.append(line)
    if depth:raise AssertionError('Unclosed diagnostic preprocessor branch')
    # Insertion of diagnostic statements may leave empty lines. Every other
    # byte of production statements and historical comments remains exact.
    return '\n'.join(line for line in result if line.strip())+'\n'
