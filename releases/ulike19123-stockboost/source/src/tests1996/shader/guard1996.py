#!/usr/bin/env python3
"""Actual GLES hint safety at cutoff ties and warm full-path cost by image type.

The small GL transport is retained from tests1962/strong_modes1962.py. Neither
shader is approximated or translated; all comparisons execute actual sources.
"""
import argparse,ctypes as c,hashlib,json,os,re,time
from pathlib import Path
import numpy as np

os.environ.setdefault('EGL_PLATFORM','surfaceless')
os.environ.setdefault('LIBGL_ALWAYS_SOFTWARE','1')
E=c.CDLL('libEGL.so.1');P=c.c_void_p;I=c.c_int;U=c.c_uint
for name,args,result in [('eglGetDisplay',[P],P),('eglInitialize',[P,c.POINTER(I),c.POINTER(I)],U),
 ('eglBindAPI',[U],U),('eglChooseConfig',[P,c.POINTER(I),c.POINTER(P),I,c.POINTER(I)],U),
 ('eglCreateContext',[P,P,P,c.POINTER(I)],P),('eglMakeCurrent',[P,P,P,P],U),('eglGetProcAddress',[c.c_char_p],P)]:
 fn=getattr(E,name);fn.argtypes=args;fn.restype=result
D=E.eglGetDisplay(None);major=I();minor=I();assert E.eglInitialize(D,c.byref(major),c.byref(minor))
assert E.eglBindAPI(0x30a0)
cfg=P();n=I();assert E.eglChooseConfig(D,(I*7)(0x3040,0x40,0x3033,1,0x3024,8,0x3038),c.byref(cfg),1,c.byref(n)) and n.value
context=E.eglCreateContext(D,cfg,None,(I*5)(0x3098,3,0x30fb,1,0x3038));assert context
assert E.eglMakeCurrent(D,None,None,context)
def gl(name,args,result):return c.CFUNCTYPE(result,*args)(E.eglGetProcAddress(name.encode()))
getstr=gl('glGetString',[U],c.c_char_p)
create=gl('glCreateShader',[U],U);setsource=gl('glShaderSource',[U,I,c.POINTER(c.c_char_p),c.POINTER(I)],None)
compile=gl('glCompileShader',[U],None);shaderget=gl('glGetShaderiv',[U,U,c.POINTER(I)],None)
shaderlog=gl('glGetShaderInfoLog',[U,I,c.POINTER(I),c.c_char_p],None)
createprogram=gl('glCreateProgram',[],U);attach=gl('glAttachShader',[U,U],None);link=gl('glLinkProgram',[U],None)
programget=gl('glGetProgramiv',[U,U,c.POINTER(I)],None);use=gl('glUseProgram',[U],None)
location=gl('glGetUniformLocation',[U,c.c_char_p],I);uniform=gl('glUniform1iv',[I,I,c.POINTER(I)],None)
gen=gl('glGenBuffers',[I,c.POINTER(U)],None);bind=gl('glBindBuffer',[U,U],None)
data=gl('glBufferData',[U,c.c_ssize_t,P,U],None);base=gl('glBindBufferBase',[U,U,U],None)
dispatch=gl('glDispatchCompute',[U,U,U],None);barrier=gl('glMemoryBarrier',[U],None);finish=gl('glFinish',[],None)
mapbuffer=gl('glMapBufferRange',[U,c.c_ssize_t,c.c_ssize_t,U],P);unmap=gl('glUnmapBuffer',[U],U);error=gl('glGetError',[],U)
def program(text):
 shader=create(0x91b9);raw=c.c_char_p(text);setsource(shader,1,c.byref(raw),None);compile(shader)
 ok=I();shaderget(shader,0x8b81,c.byref(ok));log=c.create_string_buffer(10000);length=I();shaderlog(shader,10000,c.byref(length),log)
 if not ok.value:raise AssertionError(log.value.decode())
 p=createprogram();attach(p,shader);link(p);programget(p,0x8b82,c.byref(ok));assert ok.value
 width=(I*3)();programget(p,0x8267,width);return p,width[0]
buffers=(U*8)();gen(8,buffers)
def read(slot,array):
 bind(0x90d2,buffers[slot]);ptr=mapbuffer(0x90d2,0,array.nbytes,1);assert ptr
 result=np.frombuffer(c.string_at(ptr,array.nbytes),dtype=array.dtype).copy();assert unmap(0x90d2);return result
def execute(prog,width,arrays,u,split):
 use(prog);loc=location(prog,b'u');assert loc>=0
 for slot,array in enumerate(arrays):bind(0x90d2,buffers[slot]);data(0x90d2,array.nbytes,array.ctypes.data,0x88e8);base(0x90d2,slot,buffers[slot])
 count=u[0]*(u[3]-u[2]);start=0
 while start<count:
  length=min(split,count-start);params=u.copy();params[31]=start
  uniform(loc,32,params.ctypes.data_as(c.POINTER(I)));dispatch((length+width-1)//width,1,1);barrier(0x2000);start+=length
 finish();assert error()==0
 for slot in [0,2,3,4,5,6]:
  if not np.array_equal(read(slot,arrays[slot]),arrays[slot]):raise AssertionError('immutable binding changed '+str(slot))
 return read(1,arrays[1]),read(7,arrays[7])

def cutoff_proof(candidate):
 # Exact production helpers; no reimplementation of the RNE divider. The two
 # variable-sample hints are extracted from their constant-sample source sites.
 math=candidate[candidate.index('#ifndef GX_IEEE_DIV73'):candidate.index('/* GX16/GX27:')]
 helper=re.search(r'bool impossibleDistance\([^}]+\}',candidate).group(0)
 hints=re.findall(r'precise float rejectionHint1996=([59]\.0\*\(2\.0\*variance\+5\.0\*h2\));',candidate)
 assert hints==['5.0*(2.0*variance+5.0*h2)','9.0*(2.0*variance+5.0*h2)']
 for samples in (5,9):
  assert candidate.count(f'if(distance>rejectionHint1996&&impossibleDistance(distance,{samples}.0,variance,h2))')==1
 text='''#version 310 es
#extension GL_EXT_gpu_shader5 : require
precision highp float;precision highp int;
#define GX_IEEE_DIV73 1
#define GX_REGIONAL_PASS19115 0
#define GX_REGIONAL_MAIN19115 0
#define GX_DIV_FAST19115 0
layout(local_size_x=64) in;
layout(std430,binding=0) readonly buffer In {float values[];};
layout(std430,binding=1) writeonly buffer Out {uint flags[];};uniform int u[32];
'''+math+helper+'''
void main(){
 int at=int(uint(u[31])+gl_GlobalInvocationID.x);if(at>=u[0])return;
 int b=at*5;precise float distance=values[b],finalDistance=values[b+1];
 precise float variance=values[b+2],h2=values[b+3],samples=values[b+4];
 precise float rejectionHint1996=samples==5.0?HINT5:HINT9;
 bool baselinePartial=impossibleDistance(distance,samples,variance,h2);
 bool baselineFinal=impossibleDistance(finalDistance,samples,variance,h2);
 bool guarded=distance>rejectionHint1996&&impossibleDistance(distance,samples,variance,h2);
 precise float adjusted=max(0.0,ieeeDiv1973(finalDistance,samples)-2.0*variance);
 precise float z=ieeeDiv1973(adjusted,h2);
 flags[at*8]=baselinePartial?1u:0u;flags[at*8+1]=baselineFinal?1u:0u;
 flags[at*8+2]=guarded?1u:0u;flags[at*8+3]=(guarded||baselineFinal)?1u:0u;
 flags[at*8+4]=(distance>0.0&&baselinePartial)||baselineFinal?1u:0u;
 flags[at*8+5]=(distance>uintBitsToFloat(0x7f800000u)&&baselinePartial)||baselineFinal?1u:0u;
 flags[at*8+6]=z==5.0?1u:0u;flags[at*8+7]=distance>rejectionHint1996?1u:0u;
}
'''
 text=text.replace('HINT5',hints[0]).replace('HINT9',hints[1])
 kernel=program(text.encode());records=[]
 for samples,factor in [(5,3.2),(9,2.6)]:
  for sy,sc in [(0,0),(.1,.1),(.4,.4),(.6,.6),(1,1),(3,4),(7,9),(16,19),(32,40)]:
   sy,sc=np.float32(sy),np.float32(sc)
   variance=np.float32(np.float32(sy*sy)+np.float32(np.float32(np.float32(.22)*sc)*sc))
   h2=max(np.float32(1.5),np.float32(variance*np.float32(factor)))
   center=np.float32(np.float32(samples)*np.float32(np.float32(variance*2)+np.float32(h2*5)))
   distances=[center];low=center;high=center
   for _ in range(32):
    low=np.nextafter(low,np.float32(-np.inf));high=np.nextafter(high,np.float32(np.inf));distances.extend([low,high])
   for partial in distances+[np.float32(0),np.float32(center*.01),np.float32(center*16)]:
    for added in [np.float32(0),np.float32(.125),np.float32(center),np.float32(center*4)]:
     records.append([partial,np.float32(partial+added),variance,h2,samples])
 rng=np.random.default_rng(1996001)
 for _ in range(4096):
  samples,factor=(5,3.2) if rng.integers(2)==0 else (9,2.6)
  variance=np.float32(2.0**rng.uniform(-20,20));h2=np.float32(max(1.5,float(variance)*factor))
  partial=np.float32(rng.uniform(0,64)*float(variance));added=np.float32(rng.uniform(0,64)*float(variance))
  records.append([partial,np.float32(partial+added),variance,h2,samples])
 values=np.asarray(records,np.float32).ravel();flags=np.zeros(len(records)*8,np.uint32)
 arrays=[values,flags,*[np.zeros(1,np.int32) for _ in range(6)]]
 u=np.zeros(32,np.int32);u[0]=len(records);u[3]=1
 actual,_=execute(*kernel,arrays,u,len(records));actual=actual.reshape((-1,8))
 assert np.all(actual[:,2]<=actual[:,0]),'Hint introduced an early rejection without exact confirmation'
 for column in (3,4,5):
  assert np.array_equal(actual[:,1],actual[:,column]),'Guard or artificial false hint changed final contribution'
 assert not np.any((actual[:,0]==1)&(actual[:,1]==0)),'Original partial rejection lacked monotonicity'
 ties=int(np.count_nonzero(actual[:,6]));saved=int(np.count_nonzero(actual[:,7]==0))
 confirmed=int(np.count_nonzero(actual[:,2]));missed=int(np.count_nonzero((actual[:,0]==1)&(actual[:,2]==0)))
 assert ties>0 and saved>0 and confirmed>0
 return dict(cases=len(records),assertions=len(records)*5,unsafe_rejections=0,
             final_contribution_differences=0,exact_cutoff_ties=ties,hints_skipping_exact_partial=saved,
             exact_confirmed_early_rejections=confirmed,missed_hints_deferred_to_final=missed,
             forced_false_positive_and_false_negative_hints_preserve_final=True)


def benchmark(texts):
 import statistics
 rng=np.random.default_rng(199596);cases=[];w,h=512,256;n=w*h
 for mode in (0,1,3):
  for shape in ('smooth','grain','texture'):
   u=np.zeros(32,np.int32);u[:13]=[w,h,0,h,0,h,0,h,3,0,mode,1,mode==3]
   rgb=np.full((h,w,3),90,np.int32)
   if shape=='grain':rgb+=rng.integers(-12,13,rgb.shape)
   elif shape=='texture':rgb+=np.where(np.indices((h,w)).sum(axis=0)%4<2,80,-70)[:,:,None]
   image=(np.uint32(0xff000000)|(rgb[:,:,0].astype(np.uint32)<<16)|(rgb[:,:,1].astype(np.uint32)<<8)|rgb[:,:,2].astype(np.uint32)).ravel()
   ev=np.full(16+3*((w+63)//64)*((h+63)//64),8,np.float32);ev[18::3]=0
   policy=np.zeros(n*2,np.int32);policy[::2]=256
   maps=[];mw,mh=w,h
   for k in range(3):
    mw=(mw+1)//2;mh=(mh+1)//2;maps.append(np.full(mw*mh,90<<24,np.uint32))
   arrays=[image,np.zeros(n,np.uint32),ev,policy,*maps,np.zeros(((w+3)//4)*((h+3)//4) if mode==3 else 1,np.int32)]
   cases.append((mode,shape,u,arrays))
 programs={}
 for mode in (0,1,3):
  for name,text in texts.items():
   defines=f'\n#define GX_LOCAL_SIZE 64\n#define GX_STRONG_MODE {mode}\n#define GX_TILE_WIDTH {0 if mode==3 else 8}\n#define GX_IEEE_DIV73 1\n#define GX_POW2_EXACT78 {0 if mode==3 else 1}\n'
   programs[(mode,name)]=program(text.replace('#version 310 es','#version 310 es'+defines,1).encode())
 results=[];assertions=0
 for mode,shape,u,arrays in cases:
  samples={name:[] for name in texts};outputs={}
  for _ in range(3):
   for name in texts:outputs[name]=execute(*programs[(mode,name)],arrays,u,n)
  assert all(np.array_equal(a,b) for a,b in zip(outputs['published95'],outputs['candidate96'])),(mode,shape)
  assertions+=2
  for trial in range(8):
   for name in (list(texts) if trial%2==0 else list(texts)[::-1]):
    begin=time.perf_counter();actual=execute(*programs[(mode,name)],arrays,u,n);elapsed=time.perf_counter()-begin
    assert all(np.array_equal(a,b) for a,b in zip(outputs['published95'],actual)),(mode,shape,name)
    assertions+=2;samples[name].append(elapsed*1000)
  medians={name:statistics.median(times) for name,times in samples.items()}
  result=dict(mode=mode,shape=shape,width=w,height=h,median_ms=medians,samples_ms=samples,
              ratio_candidate_to_baseline=medians['candidate96']/medians['published95'],pixels_equal=True,confidence_equal=True)
  results.append(result);print(json.dumps(result),flush=True)
 return dict(cases=results,assertions=assertions,scope='Software GLES host upload, dispatch, finish, readback and immutable-input checks; three warmups, eight alternating trials',speed_gate=False)


def main(source,work):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 candidate=(source/'native1960/strong1960.comp').read_text()
 baseline=(source/'tests1996/shader/reference95/strong1960.comp').read_text()
 cutoffs=cutoff_proof(candidate);cost=benchmark(dict(published95=baseline,candidate96=candidate))
 result=dict(status='passed',assertions=cutoffs['assertions']+cost['assertions'],actual_gles_execution=True,
             renderer=getstr(0x1f01).decode(),cutoff_proof=cutoffs,cost_by_image_type=cost,
             physical_android_tested=False,device_speedup_verified=False)
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))


if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--source',required=True);parser.add_argument('--work',required=True)
 args=parser.parse_args();main(args.source,args.work)
