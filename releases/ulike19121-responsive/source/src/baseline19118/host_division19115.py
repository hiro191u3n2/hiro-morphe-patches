#!/usr/bin/env python3
"""Actual GLSL division against independent rational and strict Java oracles.

The candidate changes only how an exact product is updated while finding the
integer floor quotient. Forced inaccurate guesses exercise the unchanged long
division fallback. No physical-device performance claim is made.
"""
from pathlib import Path
from fractions import Fraction
import argparse, hashlib, importlib.util, json, os, random, shutil, struct, subprocess, sys
import numpy as np

ROOT=Path(__file__).resolve().parent
INF=0x7f800000;NAN=0x7fc00000

def rational(bits):
    e=bits>>23;m=bits&0x7fffff
    if e:m|=1<<23;exponent=e-150
    else:exponent=-149
    return Fraction(m<<exponent,1) if exponent>=0 else Fraction(m,1<<-exponent)

def nearest(q):
    if not q:return 0
    n,d=q.numerator,q.denominator;e=n.bit_length()-d.bit_length()
    if (n<d<<e) if e>=0 else (n<<-e<d):e-=1
    if e>127:return INF
    shift=max(e-23,-149)
    a,b=(n,d<<shift) if shift>=0 else (n<<-shift,d)
    whole,remainder=divmod(a,b);twice=2*remainder
    rounded=whole+(twice>b or (twice==b and whole&1))
    if not rounded:return 0
    if shift==-149 and rounded<(1<<23):return int(rounded)
    if rounded==1<<24:rounded>>=1;e+=1
    if e>127:return INF
    return ((max(e,-126)+127)<<23)|(int(rounded)&0x7fffff)

def divide(ax,bx):
    sign=(ax^bx)&0x80000000;a=ax&0x7fffffff;b=bx&0x7fffffff
    if a>INF or b>INF:return NAN
    if a==INF:return NAN if b==INF else sign|INF
    if b==INF:return sign
    if not b:return NAN if not a else sign|INF
    if not a:return sign
    return sign|nearest(rational(a)/rational(b))

def corpus():
    pairs=[];edges=[0,1,2,3,0x3fffff,0x7ffffe,0x7fffff,0x800000,0x800001,
                   0x3f000000,0x3f7fffff,0x3f800000,0x3f800001,0x40000000,
                   0x40400000,0x7f000000,0x7f7ffffe,0x7f7fffff,INF,INF+1,NAN,0x7fffffff]
    for a in edges:
        for b in edges:
            for sa in (0,0x80000000):
                for sb in (0,0x80000000):pairs.append((a|sa,b|sb))
    for e in range(255):
        for m in (0,1,2,0x3fffff,0x7ffffe,0x7fffff):
            a=(e<<23)|m
            for b in (1,0x7fffff,0x800000,0x3f000000,0x3f7fffff,0x3f800000,0x3f800001,0x40000000,0x40400000,0x7f7fffff):
                pairs.extend(((a,b),(a|0x80000000,b),(b,a)))
    # Underflow halfway cases round to both even neighbors, with both signs.
    for k in range(1,2048,2):
        for sign in (0,0x80000000):pairs.append((k|sign,0x40000000))
    rng=random.Random(1911501)
    for _ in range(65536):pairs.append((rng.getrandbits(32),rng.getrandbits(32)))
    return pairs

JAVA='''import java.io.*;
public strictfp final class DivisionOracle19115 {
 public static void main(String[] a)throws Exception{
  DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(a[0])));
  DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(a[1])));
  try{for(;;){int x=in.readInt(),y=in.readInt();out.writeInt(Float.floatToIntBits(Float.intBitsToFloat(x)/Float.intBitsToFloat(y)));}}
  catch(EOFException done){}finally{in.close();out.close();}
 }
}
'''

def test(source,work,jdk=None,ndk=None):
    source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    pairs=corpus();expected=np.array([divide(*p) for p in pairs],np.uint32)
    words=np.array(pairs,np.uint32).ravel();(work/'pairs.bin').write_bytes(words.astype('>u4').tobytes())
    javac=Path(jdk)/'bin/javac' if jdk else Path(shutil.which('javac') or '/missing/javac')
    java=javac.with_name('java');java_source=work/'DivisionOracle19115.java';java_source.write_text(JAVA)
    subprocess.run([str(javac),str(java_source)],check=True,capture_output=True)
    subprocess.run([str(java),'-cp',str(work),'DivisionOracle19115',str(work/'pairs.bin'),str(work/'java.bin')],check=True,capture_output=True)
    java_bits=np.fromfile(work/'java.bin',dtype='>u4').astype(np.uint32)
    assert np.array_equal(expected,java_bits),'independent rational and Java oracle disagree'
    path=source/'native1960/strong1960.comp';text=path.read_text()
    math=text[text.index('#ifndef GX_IEEE_DIV73'):text.index('/* GX16/GX27:')]
    spec=importlib.util.spec_from_file_location('division115_gl',source/'tests1996/shader/guard1996.py')
    gl=importlib.util.module_from_spec(spec);spec.loader.exec_module(gl)
    def program(fast,perturb,mutant=False):
        body=math
        if mutant:
            needle='if(twice>mb||(twice==mb&&(q&1u)!=0u))q++;'
            assert body.count(needle)==1
            body=body.replace(needle,'if(twice>=mb)q++;',1)
        source_code='''#version 310 es
#extension GL_EXT_gpu_shader5 : require
precision highp float;precision highp int;
#define GX_IEEE_DIV73 1
'''+f'#define GX_DIV_FAST19115 {fast}\n#define GX_DIV_GUESS_PERTURB73 {perturb}\n'+'''
layout(local_size_x=64) in;
layout(std430,binding=0) readonly buffer Input {uint words[];};
layout(std430,binding=1) writeonly buffer Output {uint result[];};
uniform int u[32];
'''+body+'''
void main(){int i=int(uint(u[31])+gl_GlobalInvocationID.x);if(i>=u[0])return;
result[i]=ieeeDivBits1973(words[2*i],words[2*i+1]);}
'''
        return gl.program(source_code.encode())
    arrays=[words,np.zeros(len(pairs),np.uint32),*[np.zeros(1,np.uint32) for _ in range(6)]]
    u=np.zeros(32,np.int32);u[0]=len(pairs);u[3]=1
    comparisons=0;perturbations=(0,-1,1,-7,7,-65,65,-1048576,1048576)
    for fast in (0,1):
        for perturb in perturbations:
            actual,_=gl.execute(*program(fast,perturb),arrays,u,len(pairs))
            assert np.array_equal(expected,actual),f'division mismatch fast={fast} perturb={perturb} at {np.flatnonzero(expected!=actual)[:8]}'
            comparisons+=len(pairs)
    mutant,_=gl.execute(*program(1,0,True),arrays,u,len(pairs))
    rejected=int(np.count_nonzero(mutant!=expected));assert rejected>0,'rounding mutant went undetected'
    result={'status':'passed','assertions':comparisons+len(pairs)+1,
            'input_pairs':len(pairs),'glsl_bit_comparisons':comparisons,
            'strict_java_oracle':True,'independent_fraction_oracle':True,
            'guess_perturbations':list(perturbations),'rounding_mutant_differences':rejected,
            'shader_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),
            'physical_android_tested':False,'device_speedup_verified':False}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',default=str(ROOT));p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk)))
