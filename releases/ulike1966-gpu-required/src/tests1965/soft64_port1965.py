#!/usr/bin/env python3
"""Port the needed Mesa BSD3 SoftFloat GLSL to core GLES3.1 uint32 pairs.

This is a reproducible source adaptation, not a floating point approximation.
Public binary64 representation is uvec2(low-word, high-word).
"""
from pathlib import Path
import base64, urllib.request, re, hashlib, json

ROOT=Path(__file__).resolve().parents[1]
URL='https://android.googlesource.com/platform/external/mesa3d/+/refs/heads/main/src/compiler/glsl/float64.glsl?format=TEXT'
SOURCE=Path(__file__).with_name('soft64_mesa_upstream.glsl')
if not SOURCE.exists():
    SOURCE.write_bytes(base64.b64decode(urllib.request.urlopen(URL,timeout=20).read()))
s=SOURCE.read_text()
SOURCE_SHA='2fa2575aca36750f40faf91d043babf65f5f3b6c34db596822b05a16f725e149'
assert hashlib.sha256(s.encode()).hexdigest()==SOURCE_SHA, 'Unreviewed Mesa source revision'
functions={}
for m in re.finditer(r'(?m)^(\w+)\s*\n?(\w+)\s*\(([^)]*)\)\s*\{',s):
    start=m.start(); pos=m.end(); depth=1
    while depth:
        depth+=(s[pos]=='{')-(s[pos]=='}');pos+=1
    functions[m.group(2)]=(start,s[start:pos])

entry={'__fabs64','__fneg64','__feq64','__flt64','__fge64','__fadd64','__fmul64','__fsqrt64',
       '__fp32_to_fp64','__fp64_to_fp32','__uint_to_fp64','__int_to_fp64','__fp64_to_int',
       '__ftrunc64','__ffloor64','__fmin64','__fmax64','__normalizeFloat64Subnormal','__roundAndPackFloat64'}
selected=set()
def select(name):
    if name in selected:return
    if name not in functions:raise ValueError('missing function '+name)
    selected.add(name)
    for call in re.findall(r'\b(\w+)\s*\(',functions[name][1]):
        if call in functions and call!=name:select(call)
for name in sorted(entry):select(name)
text='\n\n'.join(functions[name][1] for name in sorted(selected,key=lambda n:functions[n][0]))
text=text.replace('uint64_t','uvec2')
text=re.sub(r'\b0[xX]([0-9a-fA-F]+)[uU][lL]\b',lambda m:'uvec2(0x%08xu,0x%08xu)'%(int(m.group(1),16)&0xffffffff,int(m.group(1),16)>>32),text)
text=re.sub(r'\b(\d+)[uU][lL]\b',lambda m:'uvec2(%su,0u)'%m.group(1),text)
# Correct the upstream signedness typo in the subnormal rounding branch.
assert 'increment = zFrac2 < 0u;' in text
text=text.replace('increment = zFrac2 < 0u;','increment = int(zFrac2) < 0;')
# Exact halfway: increment then clear the LSB (round-to-nearest, ties-even).
# Upstream translated the SoftFloat `(extra + extra) == 0` test incorrectly.
text=text.replace('zFrac1 &= ~((zFrac2 + uint(zFrac2 == 0u)) & uint(roundNearestEven));',
                  'zFrac1 &= ~uint((zFrac2 == 0x80000000u) && roundNearestEven);')
text=text.replace('(a.y & b.y & 0x80000000u) != 0;', '(a.y & b.y & 0x80000000u) != 0u;')
text=text.replace('uint sign_of_difference = 0;', 'uint sign_of_difference = 0u;')
text=text.replace('sign_of_difference = 0x80000000;', 'sign_of_difference = 0x80000000u;')
text=text.replace('aFrac0 == 0)', 'aFrac0 == 0u)')
# GLES forbids overloading builtins. Explicit selection preserves integer bits.
text=re.sub(r'\bmix\s*\(', 's64m_mix(', text)
# The desktop implementation computes unselected shift expressions eagerly.
# Core GLES leaves negative or >=32 shifts undefined; mask every variable
# uint32 shift distance, preserving all selected paths' intended word shifts.
text=re.sub(r'([<>]{2})\s*shiftDist\s*-\s*32', r'\1 ((shiftDist - 32) & 31)', text)
text=re.sub(r'([<>]{2})\s*\(\s*-\s*shiftCount\s*\)', r'\1 ((-shiftCount) & 31)', text)
text=re.sub(r'([<>]{2})\s*\(\s*fracBits\s*-\s*32\s*\)', r'\1 ((fracBits - 32) & 31)', text)
text=re.sub(r'([<>]{2})\s*(count|negCount|shiftCount|shiftDist|fracBits)\b', r'\1 (\2 & 31)', text)
text=text.replace('aFrac <<= shiftCount;', 'aFrac <<= (shiftCount & 31);')
assert 'int64_t' not in text
license=s[:s.index('#version')]
header=license+'''\n/* Portable adaptation for ULike v1.9.65. Original source SHA256: SOURCE_SHA.
 * RNE, separate add/multiply, subnormal results retained; no native uint64/fp64.
 * Source: https://android.googlesource.com/platform/external/mesa3d/+/refs/heads/main/src/compiler/glsl/float64.glsl
 * GLSL callers use uvec2(lo,hi) and explicit s64_* arithmetic, never vector operators.
 */
#ifndef ULIKE_SOFT64_1965
#define ULIKE_SOFT64_1965
#define FLOAT_ROUND_NEAREST_EVEN 0
#define FLOAT_ROUND_TO_ZERO 1
#define FLOAT_ROUND_DOWN 2
#define FLOAT_ROUND_UP 3
#define FLOAT_ROUNDING_MODE FLOAT_ROUND_NEAREST_EVEN
#define RELAXED_NAN_PROPAGATION
#define EXCHANGE(a,b) do { a^=b; b^=a; a^=b; } while(false)
uvec2 packUint2x32(uvec2 v){return v;}
uvec2 unpackUint2x32(uvec2 v){return v;}
uvec2 s64m_mix(uvec2 a,uvec2 b,bool c){return c?b:a;}
uvec2 s64m_mix(uvec2 a,uvec2 b,bvec2 c){return uvec2(c.x?b.x:a.x,c.y?b.y:a.y);}
uint s64m_mix(uint a,uint b,bool c){return c?b:a;}
int s64m_mix(int a,int b,bool c){return c?b:a;}
float s64m_mix(float a,float b,bool c){return c?b:a;}
'''
header=header.replace('SOURCE_SHA',hashlib.sha256(s.encode()).hexdigest())
result=header+text+'\n'+(Path(__file__).with_name('soft64_api1965.glsl')).read_text()+'\n#endif\n'
result=re.sub(r'\b__(\w+)',r's64m_\1',result)
(ROOT/'native1960/soft64_1965.glsl').write_text(result)
print(json.dumps({'functions':len(selected),'sha256':hashlib.sha256(s.encode()).hexdigest(),'destination':'native1960/soft64_1965.glsl'}))
