#!/usr/bin/env python3
"""Actual production GLSL helper and regional sampler, executed through EGL/GLES."""
from pathlib import Path
import argparse,ctypes as c,hashlib,json,os,sys
os.environ.setdefault('EGL_PLATFORM','surfaceless');os.environ.setdefault('LIBGL_ALWAYS_SOFTWARE','1')
import numpy as np
P=c.c_void_p;I=c.c_int;U=c.c_uint
E=c.CDLL('libEGL.so.1')
for name,args,result in [('eglGetDisplay',[P],P),('eglInitialize',[P,c.POINTER(I),c.POINTER(I)],U),('eglBindAPI',[U],U),('eglChooseConfig',[P,c.POINTER(I),c.POINTER(P),I,c.POINTER(I)],U),('eglCreateContext',[P,P,P,c.POINTER(I)],P),('eglMakeCurrent',[P,P,P,P],U),('eglGetProcAddress',[c.c_char_p],P),('eglDestroyContext',[P,P],U),('eglTerminate',[P],U)]:
    fn=getattr(E,name);fn.argtypes=args;fn.restype=result
D=E.eglGetDisplay(None);major=I();minor=I()
assert E.eglInitialize(D,c.byref(major),c.byref(minor));assert E.eglBindAPI(0x30a0)
cfg=P();n=I();assert E.eglChooseConfig(D,(I*7)(0x3040,0x40,0x3033,1,0x3024,8,0x3038),c.byref(cfg),1,c.byref(n)) and n.value
C=E.eglCreateContext(D,cfg,None,(I*5)(0x3098,3,0x30fb,1,0x3038));assert C;assert E.eglMakeCurrent(D,None,None,C)
def gl(name,args,result):
    address=E.eglGetProcAddress(name.encode());assert address,name;return c.CFUNCTYPE(result,*args)(address)
getstr=gl('glGetString',[U],c.c_char_p);create=gl('glCreateShader',[U],U);setsource=gl('glShaderSource',[U,I,c.POINTER(c.c_char_p),c.POINTER(I)],None);compile=gl('glCompileShader',[U],None);shaderget=gl('glGetShaderiv',[U,U,c.POINTER(I)],None);shaderlog=gl('glGetShaderInfoLog',[U,I,c.POINTER(I),c.c_char_p],None)
createprogram=gl('glCreateProgram',[],U);attach=gl('glAttachShader',[U,U],None);link=gl('glLinkProgram',[U],None);programget=gl('glGetProgramiv',[U,U,c.POINTER(I)],None);programlog=gl('glGetProgramInfoLog',[U,I,c.POINTER(I),c.c_char_p],None);use=gl('glUseProgram',[U],None)
location=gl('glGetUniformLocation',[U,c.c_char_p],I);uniform=gl('glUniform1iv',[I,I,c.POINTER(I)],None);gen=gl('glGenBuffers',[I,c.POINTER(U)],None);bind=gl('glBindBuffer',[U,U],None);data=gl('glBufferData',[U,c.c_ssize_t,P,U],None);base=gl('glBindBufferBase',[U,U,U],None);dispatch=gl('glDispatchCompute',[U,U,U],None);barrier=gl('glMemoryBarrier',[U],None);finish=gl('glFinish',[],None);mapbuffer=gl('glMapBufferRange',[U,c.c_ssize_t,c.c_ssize_t,U],P);unmap=gl('glUnmapBuffer',[U],U);error=gl('glGetError',[],U)
deletebuffers=gl('glDeleteBuffers',[I,c.POINTER(U)],None);deleteprogram=gl('glDeleteProgram',[U],None);deleteshader=gl('glDeleteShader',[U],None)
def program(text):
    shader=create(0x91b9);raw=c.c_char_p(text.encode());setsource(shader,1,c.byref(raw),None);compile(shader)
    ok=I();shaderget(shader,0x8b81,c.byref(ok));log=c.create_string_buffer(20000);length=I();shaderlog(shader,20000,c.byref(length),log)
    assert ok.value,log.value.decode();p=createprogram();attach(p,shader);link(p);programget(p,0x8b82,c.byref(ok));programlog(p,20000,c.byref(length),log);assert ok.value,log.value.decode();deleteshader(shader);return p

def execute(text,arrays,params,count,words):
    p=program(text);buffers=(U*len(arrays))();gen(len(arrays),buffers);use(p)
    for slot,array in enumerate(arrays):
        bind(0x90d2,buffers[slot]);data(0x90d2,array.nbytes,array.ctypes.data,0x88e8);base(0x90d2,slot,buffers[slot])
    loc=location(p,b'u');assert loc>=0
    for offset in range(0,count,65536):
        params[31]=offset;uniform(loc,32,params.ctypes.data_as(c.POINTER(I)));dispatch((min(65536,count-offset)+63)//64,1,1);barrier(0x2000)
    finish();assert error()==0
    def read(slot):
        bind(0x90d2,buffers[slot]);ptr=mapbuffer(0x90d2,0,arrays[slot].nbytes,1);assert ptr
        actual=np.frombuffer(c.string_at(ptr,arrays[slot].nbytes),dtype=np.uint32).copy();assert unmap(0x90d2);return actual
    result=read(1)[:words]
    for slot in (0,2):assert np.array_equal(read(slot),arrays[slot]),'immutable input mutation'
    deletebuffers(len(arrays),buffers);deleteprogram(p);return result

PREFIX='''#version 310 es
#extension GL_EXT_gpu_shader5 : require
precision highp float;precision highp int;
#define GX_IEEE_DIV73 1
#define GX_TRACE19106 0
layout(local_size_x=64) in;
layout(std430,binding=0) readonly buffer Input19107 {uint inputs19107[];};
layout(std430,binding=1) writeonly buffer Output19107 {uint outputs19107[];};
layout(std430,binding=2) readonly buffer Evidence1960 {float evidence[];};
uniform int u[32];
'''
def main(source,work):
    source,work=Path(source),Path(work);shader=(source/'native1960/strong1960.comp').read_text()
    a=shader.index('// BEGIN GX_REGIONAL_RNE19107');b=shader.index('// END GX_REGIONAL_RNE19107',a)+len('// END GX_REGIONAL_RNE19107');helper=shader[a:b]
    main='''\nvoid main(){int at=int(gl_GlobalInvocationID.x)+u[31];if(at>=u[0])return;int i=at*6;
 outputs19107[at]=regionalInterpolateBits19107(inputs19107[i],inputs19107[i+1],inputs19107[i+2],inputs19107[i+3],int(inputs19107[i+4]),int(inputs19107[i+5]));}
'''
    records=np.fromfile(work/'cases.bin',dtype='>u4').astype(np.uint32);count=len(records)//6;params=np.zeros(32,np.int32);params[0]=count
    arrays=[records,np.zeros(count,np.uint32),np.zeros(1,np.uint32)]
    result=execute(PREFIX+helper+main,arrays,params,count,count);result.astype('>u4').tofile(work/'gpu.bin')
    # Three deliberately wrong numerical implementations must disagree with the exact expected outputs.
    expected=np.fromfile(work/'oracle.bin',dtype='>u4').astype(np.uint32);negatives=[]
    mutations={
        'output_lsb':'outputs19107[at]=regionalInterpolateBits19107(inputs19107[i],inputs19107[i+1],inputs19107[i+2],inputs19107[i+3],int(inputs19107[i+4]),int(inputs19107[i+5]))^1u;',
        'reversed_horizontal_weights':'outputs19107[at]=regionalInterpolateBits19107(inputs19107[i],inputs19107[i+1],inputs19107[i+2],inputs19107[i+3],128-int(inputs19107[i+4]),int(inputs19107[i+5]));',
        'truncate_input_mantissa':'outputs19107[at]=regionalInterpolateBits19107(inputs19107[i]&0xfffffffeu,inputs19107[i+1]&0xfffffffeu,inputs19107[i+2]&0xfffffffeu,inputs19107[i+3]&0xfffffffeu,int(inputs19107[i+4]),int(inputs19107[i+5]));'}
    for name,change in mutations.items():
        bad=execute(PREFIX+helper+main[:-2]+change+'}\n',arrays,params,count,count);mismatches=int(np.count_nonzero(bad!=expected));assert mismatches>0,name;negatives.append(dict(name=name,mismatches=mismatches))
    # This changes the actual production scale rounding from ties-even to ties-away;
    # an independent oracle must reject it, including the minimum-subnormal tie.
    anchor='tail==halfway&&(rounded&1u)!=0u'
    assert helper.count(anchor)==1
    bad=execute(PREFIX+helper.replace(anchor,'tail==halfway',1)+main,arrays,params,count,count)
    mismatches=int(np.count_nonzero(bad!=expected));assert mismatches>0,'ties-away rounding mutation'
    negatives.append(dict(name='scale_ties_away_instead_of_even',mismatches=mismatches))
    # The exact production float-to-dyadic-weight gate rejects every adjacent
    # non-dyadic bit pattern, all signed/nonfinite values and out-of-range input.
    valid=np.asarray([np.float32(p/128) for p in range(129)],np.float32).view(np.uint32)
    badweights=sorted({int(b)+d for b in valid for d in (-1,1) if 0<=int(b)+d<=0xffffffff}-set(map(int,valid)))
    weightbits=np.asarray(list(valid)+badweights+[0x80000000,0xbf800000,0x7f800000,0xff800000,0x7fc00000,0x7f800001],np.uint32)
    weightmain='\nvoid main(){int at=int(gl_GlobalInvocationID.x)+u[31];if(at>=u[0])return;outputs19107[at]=uint(regionalWeight19107(uintBitsToFloat(inputs19107[at])));}'
    params[0]=len(weightbits)
    wt=execute(PREFIX+helper+weightmain,[weightbits,np.zeros(len(weightbits),np.uint32),np.zeros(1,np.uint32)],params,len(weightbits),len(weightbits))
    assert np.array_equal(wt[:129],np.arange(129,dtype=np.uint32)) and np.all(wt[129:]==0xffffffff),'dyadic weight validation'
    # Invalid-domain contract: no negative/nonfinite evidence or non-dyadic weight acceptance.
    invalid=np.asarray([[v,0,0,0,64,64] for v in (0x80000000,0xbf800000,0x7f800000,0xff800000,0x7fc00000,0x7f800001)]+[[0,0,0,0,p,q] for p,q in ((-1,0),(129,0),(0,-1),(0,129))],np.int64).astype(np.uint32).ravel()
    params[0]=len(invalid)//6;iv=execute(PREFIX+helper+main,[invalid,np.zeros(params[0],np.uint32),np.zeros(1,np.uint32)],params,int(params[0]),int(params[0]));assert np.all(iv==0x7fc00000),iv
    # Execute the actual regional function with its unmodified coordinate/index/crop math.
    math=shader[shader.index('#ifndef GX_IEEE_DIV73'):shader.index('/* GX16/GX27:')]
    regional=shader[shader.index('#if GX_IEEE_DIV73\n/* .100:'):shader.index('/* GX28:')]
    regionalmain='''\nvoid main(){int at=int(gl_GlobalInvocationID.x)+u[31];if(at>=u[29])return;
 vec3 v=regionalEvidence19100(int(inputs19107[at*2]),int(inputs19107[at*2+1]));for(int p=0;p<3;p++)outputs19107[at*3+p]=floatBitsToUint(v[p]);}\n'''
    groups=json.loads((work/'regions.json').read_text());regionresults=[]
    for index,g in enumerate(groups):
        points=np.fromfile(work/g['points'],dtype='>u4').astype(np.uint32);evidence=np.fromfile(work/g['evidence'],dtype='>u4').astype(np.uint32);expect=np.fromfile(work/g['expected'],dtype='>u4').astype(np.uint32)
        params=np.zeros(32,np.int32);params[0]=g['width'];params[7]=g['height'];params[13]=g['first_grid_y'];params[29]=len(points)//2
        for pow2 in (0,1):
            actual=execute(PREFIX+'\n#define GX_POW2_EXACT78 '+str(pow2)+'\n'+math+regional+regionalmain,[points,np.zeros(len(expect),np.uint32),evidence],params,len(points)//2,len(expect));bad=np.flatnonzero(actual!=expect)
            assert not len(bad),'regional group '+str(index)+' pow2 '+str(pow2)+' first '+str(bad[:3])+' actual '+str(actual[bad[:3]])+' expected '+str(expect[bad[:3]])
        regionresults.append(dict(width=g['width'],height=g['height'],first_grid_y=g['first_grid_y'],points=len(points)//2,planes=3,variants=2))
    report=dict(status='passed',actual_gles_execution=True,cases=count,invalid_domain_cases=len(iv),weight_gate_cases=len(weightbits),negative_controls=negatives,regional_groups=regionresults,renderer=getstr(0x1f01).decode(),gl_version=getstr(0x1f02).decode(),helper_sha256=hashlib.sha256(helper.encode()).hexdigest())
    (work/'gles-result.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
    E.eglMakeCurrent(D,None,None,None);E.eglDestroyContext(D,C);E.eglTerminate(D)
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);a=p.parse_args();main(a.source,a.work)
