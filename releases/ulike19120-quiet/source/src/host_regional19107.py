#!/usr/bin/env python3
"""Exact rational and strict Java oracles for actual GLSL regional interpolation.

The rational oracle rounds a mathematical Fraction independently after every
operator in the frozen Java expression; it does not port the shader's integer
mantissa/guard/sticky algorithm. All cases here are synthetic, not phone input.
"""
from pathlib import Path
from fractions import Fraction
from functools import lru_cache
import argparse,hashlib,json,os,random,struct,subprocess,sys
ROOT=Path(__file__).resolve().parent
NAN=0x7fc00000;INF=0x7f800000
TIES={'even_lower':0,'even_upper':0,'underflow':0}
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
@lru_cache(maxsize=65536)
def rational(bits):
    if bits&0x80000000 or bits>=INF:raise ValueError('finite positive binary32 required')
    e=bits>>23;m=bits&0x7fffff
    if e:m|=1<<23;exponent=e-150
    else:exponent=-149
    return Fraction(m<<exponent,1) if exponent>=0 else Fraction(m,1<<-exponent)

def nearest(q):
    if q==0:return 0
    n,d=q.numerator,q.denominator;e=n.bit_length()-d.bit_length()
    if (n < d<<e) if e>=0 else (n<<-e < d):e-=1
    if e>127:return INF
    shift=max(e-23,-149)
    a,b=(n,d<<shift) if shift>=0 else (n<<-shift,d)
    whole,remainder=divmod(a,b);twice=2*remainder
    if twice==b:
        TIES['even_upper' if whole&1 else 'even_lower']+=1
        if e<-126:TIES['underflow']+=1
    rounded=whole+(twice>b or (twice==b and whole&1))
    if rounded==0:return 0
    if shift==-149 and rounded<(1<<23):return int(rounded)
    if rounded==(1<<24):rounded>>=1;e+=1
    if e>127:return INF
    e=max(e,-126)
    return ((e+127)<<23)|(int(rounded)&0x7fffff)

@lru_cache(maxsize=131072)
def mul(a,b):
    if a>NAN or b>NAN or a==NAN or b==NAN:return NAN
    if a==INF or b==INF:return NAN if a==0 or b==0 else INF
    return nearest(rational(a)*rational(b))
@lru_cache(maxsize=131072)
def add(a,b):
    if a==NAN or b==NAN:return NAN
    if a==INF or b==INF:return INF
    return nearest(rational(a)+rational(b))
@lru_cache(maxsize=129)
def weight(p):return nearest(Fraction(p,128))
def interpolate(a,b,c,d,px,py):
    left,right,bottom,top=weight(128-px),weight(px),weight(128-py),weight(py)
    upper=add(mul(a,left),mul(b,right));lower=add(mul(c,left),mul(d,right))
    return add(mul(upper,bottom),mul(lower,top))
def write_words(path,values):
    with Path(path).open('wb') as out:
        for v in values:out.write(struct.pack('>I',v))
def read_words(path):
    data=Path(path).read_bytes();return list(struct.unpack('>'+str(len(data)//4)+'I',data))

def corpus(work):
    records=[];coverage={};rng=random.Random(1910701)
    patterns=[(0,0,0,0),(0,1,2,3),(1,2,3,4),(0x007fffff,0x00800000,0x00800001,1),
        (0x3f800000,0x3f800001,0x3f800002,0x3f7fffff),
        (0x405cb9bb,0x405cb9bc,0x408ac93d,0x408ac93e),
        (0x42faffff,0x42fb0000,0x42fb0001,0x42fb0003),
        (0,0x3f800000,0,0x3f800000),(0x3ecccccd,0x3f666666,0x400ccccd,0x4155999a),
        (0x00800000,0x7f000000,0x7e800001,0),
        (0x00000001,0x3f800000,0x3f800001,0x00800000),
        (0x7f7fffff,0x7f7ffffe,0x7f7ffffd,0x7f7ffffc)]
    for p in patterns:
        for x in range(129):
            for y in range(129):records.append((*p,x,y))
    coverage['dyadic_grid_patterns']=len(patterns);coverage['dyadic_grid_cases']=len(records);coverage['weight_pairs']=129*129
    start=len(records)
    # Every finite exponent, both parity significands, widely separated operands,
    # exponent carry, minimum/max subnormal and normal/subnormal boundaries.
    for e in range(255):
        for m in (0,1,2,0x3fffff,0x7ffffe,0x7fffff):
            a=(e<<23)|m;b=min(0x7f7fffff,a+1)
            for px,py in ((0,128),(128,0),(64,0),(64,64),(1,127),(127,1),(63,65),(65,63)):
                records.append((a,b,0 if e&1 else a,max(0,a-1),px,py))
    coverage['exponent_boundary_cases']=len(records)-start
    start=len(records)
    for _ in range(8192):
        p=tuple(rng.randrange(0x7f800000) for _ in range(4));records.append((*p,rng.randrange(129),rng.randrange(129)))
    coverage['seeded_random_cases']=len(records)-start
    start=len(records)
    # Values straddling every 8-bit integerization half threshold. This is
    # synthetic boundary stress, not the unavailable six phone input values.
    for k in range(256):
        center=nearest(Fraction(2*k+1,2))
        for delta in (-2,-1,0,1,2):
            for px,py in ((1,1),(63,65),(64,64),(127,127)):
                records.append((max(0,center+delta),center,center+1,max(0,center-1),px,py))
    coverage['integerization_boundary_cases']=len(records)-start
    expected=[interpolate(*r) for r in records]
    write_words(work/'cases.bin',(v for r in records for v in r));write_words(work/'oracle.bin',expected)
    coverage['total_cases']=len(records);coverage['zero_results']=expected.count(0);coverage['subnormal_results']=sum(0<v<0x00800000 for v in expected);coverage['nonfinite_results']=sum(v>=INF for v in expected)
    assert coverage['subnormal_results']>0 and coverage['zero_results']>0
    assert TIES['even_lower']>0 and TIES['even_upper']>0 and TIES['underflow']>0
    return coverage

def regional_corpus(work):
    rng=random.Random(1910702);groups=[]
    shapes=[(1,1,0),(63,65,0),(64,64,0),(65,129,0),(127,129,0),(128,129,0),(129,129,0),(3060,4080,0),(3060,4080,4),(3060,4080,62)]
    for group,(w,h,first) in enumerate(shapes):
        rw,rh=(w+63)//64,(h+63)//64
        ev=[0]*16+[nearest(Fraction(rng.randrange(100000),32768)) for _ in range(3*rw*rh)]
        # All three planes vary; include zero and smallest subnormal so they
        # cannot be silently dropped when the regional float SSBO is read.
        for i in range(16,len(ev),37):ev[i]=0
        for i in range(17,len(ev),53):ev[i]=1
        ys=list(range(h)) if w*h<=18000 else [0,1,31,32,63,64,65,94,127,128,129,255,256,346,520,4031,4032,4078,4079]
        points=[];expected=[]
        for y in ys:
            fy=max(Fraction(0),min(Fraction(rh-1),Fraction(2*y+1,128)-Fraction(1,2)));iy=int(fy);ny=min(rh-1,iy+1)
            if iy<first:continue
            py=int((fy-iy)*128)
            for x in range(w):
                fx=max(Fraction(0),min(Fraction(rw-1),Fraction(2*x+1,128)-Fraction(1,2)));ix=int(fx);nx=min(rw-1,ix+1);px=int((fx-ix)*128)
                points.extend((x,y))
                for plane in range(3):
                    a,b,c,d=(ev[16+3*(gy*rw+gx)+plane] for gx,gy in ((ix,iy),(nx,iy),(ix,ny),(nx,ny)))
                    expected.append(interpolate(a,b,c,d,px,py))
        assert points
        names=dict(points=f'region{group}-points.bin',evidence=f'region{group}-evidence.bin',expected=f'region{group}-expected.bin')
        write_words(work/names['points'],points);write_words(work/names['evidence'],ev[:16]+ev[16+3*rw*first:]);write_words(work/names['expected'],expected)
        groups.append(dict(width=w,height=h,first_grid_y=first,point_count=len(points)//2,**names))
    (work/'regions.json').write_text(json.dumps(groups,indent=2)+'\n');return groups

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    rational.cache_clear();mul.cache_clear();add.cache_clear();weight.cache_clear()
    for key in TIES:TIES[key]=0
    jdk=Path(jdk or os.environ.get('JAVA_HOME') or os.environ['ULIKE_JDK_HOME']);fixture=source/'tests19107/regional'
    tracked=[Path(__file__),source/'native1960/strong1960.comp',source/'StrongNoise1958.java',fixture/'RegionalOracle19107.java',fixture/'regional_gles19107.py'];pins={str(p.relative_to(source)):sha(p) for p in tracked};commands=[]
    env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
    def run(args,name):
        args=list(map(str,args));commands.append(dict(name=name,argv=args));p=subprocess.run(args,text=True,capture_output=True,env=env,timeout=600);out=p.stdout+p.stderr;(work/(name+'.log')).write_text(out)
        if p.returncode:raise RuntimeError(name+'\n'+out[-16000:])
        return out
    coverage=corpus(work);groups=regional_corpus(work)
    (work/'coverage.json').write_text(json.dumps(dict(coverage=coverage,rounding_ties=TIES,groups=groups),indent=2)+'\n')
    run([jdk/'bin/javac','--release','8','-d',work,fixture/'RegionalOracle19107.java'],'java-compile')
    java=json.loads(run([jdk/'bin/java','-cp',work,'RegionalOracle19107',work/'cases.bin',work/'java.bin'],'java-oracle').split('RESULT ',1)[1]);assert java['cases']==coverage['total_cases']
    expected=read_words(work/'oracle.bin');actual=read_words(work/'java.bin');bad=[i for i,(a,b) in enumerate(zip(actual,expected)) if a!=b]
    assert len(actual)==len(expected) and not bad,'Java / exact rational mismatch '+str(bad[:8])
    run([sys.executable,fixture/'regional_gles19107.py','--source',source,'--work',work],'actual-gles')
    actual=read_words(work/'gpu.bin');bad=[i for i,(a,b) in enumerate(zip(actual,expected)) if a!=b]
    if len(actual)!=len(expected) or bad:
        rows=read_words(work/'cases.bin');detail=[dict(index=i,input=[hex(x) for x in rows[i*6:i*6+6]],expected=hex(expected[i]),actual=hex(actual[i])) for i in bad[:12]]
        raise AssertionError('GPU / exact rational mismatch '+json.dumps(detail))
    gles=json.loads((work/'gles-result.json').read_text());assert gles['cases']==coverage['total_cases']
    assert pins=={str(p.relative_to(source)):sha(p) for p in tracked},'Test source changed during run'
    regional_words=sum(g['point_count']*3*2 for g in groups)
    result=dict(status='passed',assertions=coverage['total_cases']*2+regional_words+gles['invalid_domain_cases']+gles['weight_gate_cases']+len(gles['negative_controls']),actual_gles_execution=True,independent_rational_oracle=True,independent_java_oracle=True,exact_round_to_nearest_even=True,all_dyadic_weights_0_to_128=True,regional_coordinate_sampling_verified=True,arithmetic_negative_controls=len(gles['negative_controls']),physical_android_tested=False,device_speedup_verified=False,device_mismatch_reproduced=False,source_sha256=pins,coverage=coverage,rounding_ties=dict(TIES),gles=gles,java=java,regional_compared_words=regional_words,exact_mismatches=0,physical_log_values_are_not_test_inputs=True,full_strong_regression_required=['strong_trace106','strong_math105'],corpus_sha256=sha(work/'cases.bin'))
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
