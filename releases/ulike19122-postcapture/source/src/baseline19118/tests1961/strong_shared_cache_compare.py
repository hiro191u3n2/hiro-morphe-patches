#!/usr/bin/env python3
"""Run GX16 actual GLES shaders against the frozen published .60 shader.

No shader substitute is used. All three real layouts compare every ARGB and
NR13 confidence integer; input/evidence/policy/map buffers are read back to
prove immutability. Covers odd row crossings, bounded source/map/evidence rows,
alpha, noise0--4, masks and nonzero split-dispatch invocation origins.
"""
import argparse,ctypes as c,hashlib,json,os,time
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
root=Path(__file__).resolve().parents[1]
new=root/'native1960/strong1960.comp'
old=root/'tests1961/published1960-reference/native1960/strong1960.comp'
reference=program(old.read_bytes())
variants={width:program(new.read_bytes().replace(b'#version 310 es',b'#version 310 es\n#define GX_LOCAL_SIZE '+str(width).encode(),1)) for width in [32,64,128]}
for width,(_,actual) in variants.items():assert width==actual
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

def main(out):
 rng=np.random.default_rng(196116);cases=96;pixel_count=confidence_count=0;changed=0;started=time.monotonic();mode_counts=[0]*4;shifted_grid=offset_bins=0
 dims=[(1,1),(3,5),(17,19),(31,33),(65,67),(67,65),(129,39),(257,17)]
 for case in range(cases):
  w,h=dims[case%len(dims)];mode=(case//len(dims))%4;mode_counts[mode]+=1;u=np.zeros(32,np.int32);u[:13]=[w,h,0,h,0,h,0,h,case%5,case%2,mode,case%2,mode==3]
  if h>=65 and case%3!=0:u[2]=20;u[3]=44
  if mode==3 and h>=65 and case%2:u[6]=128;u[7]=h+128
  u[12]=int(mode==3 and ((u[6]+u[2])&3)==0 and ((u[3]-u[2])%4==0 or u[6]+u[3]==u[7]))
  n=w*(u[3]-u[2]);cn=((w+3)//4)*((u[3]-u[2]+3)//4) if u[12] else 1
  rgb=rng.integers(0,256,(w*h,3),dtype=np.uint32)
  if case%3:rgb=np.clip(rng.integers(-17,18,(w*h,3))+[55,54,53],0,255).astype(np.uint32)
  image=(np.uint32(0xff000000)|rgb[:,0]<<16|rgb[:,1]<<8|rgb[:,2]).astype(np.uint32)
  if case%11==0:image[::13]&=np.uint32(0x00ffffff)
  radius=7 if mode==1 else 5;first=max(0,int(u[2])-radius);last=min(h,int(u[3])+radius);u[14]=first;image=image[first*w:last*w].copy()
  gw,gh=(w+63)//64,(u[7]+63)//64;ev=np.zeros(16+3*gw*gh,np.float32);ev[:16]=rng.uniform(.1,9,16)
  ev[16::3]=rng.uniform(.5,7,gw*gh);ev[17::3]=rng.uniform(.5,9,gw*gh);ev[18::3]=rng.uniform(0,.8,gw*gh)
  if mode==3:
   fy=max(0,min(gh-1,(float(u[6]+u[2])+.5)/64-.5));ly=max(0,min(gh-1,(float(u[6]+u[3]-1)+.5)/64-.5))
   firstgrid=int(fy);lastgrid=min(gh,int(ly)+2);u[13]=firstgrid;shifted_grid+=int(firstgrid>0);ev=np.concatenate((ev[:16],ev[16+3*gw*firstgrid:16+3*gw*lastgrid]))
  if mode!=3 and case%3==0:
   u[20]=16*(mode+1);ev=rng.uniform(.1,9,64).astype(np.float32);offset_bins+=1
  policy=np.empty(n*2,np.int32);policy[::2]=rng.choice([0,90,256],n);policy[1::2]=rng.choice([0,90,192,256],n)
  maps=[];mw,mh=w,int(u[7])
  for k in range(3):
   mw=(mw+1)//2;mh=(mh+1)//2;channels=rng.integers(-32,33,(mw*mh,3),dtype=np.int32).astype(np.uint32)&255
   values=((np.uint32(55)<<24)|channels[:,0]<<16|channels[:,1]<<8|channels[:,2]).astype(np.uint32)
   if mode==3:
    scale=1<<(k+1);firstmap=int(max(0,min(mh-1,(float(u[6]+u[2])+.5)/scale-.5)))
    lastmap=min(mh,int(max(0,min(mh-1,(float(u[6]+u[3]-1)+.5)/scale-.5)))+2);u[15+k]=firstmap;values=values[firstmap*mw:lastmap*mw].copy()
   maps.append(values)
  arrays=[image,np.zeros(n,np.uint32),ev,policy,*maps,np.zeros(cn,np.int32)]
  baseline=list(arrays)
  if u[20]:baseline[2]=ev[u[20]:u[20]+16].copy()
  expected,ec=execute(*reference,baseline,u,n)
  for width,prog in variants.items():
   actual,ac=execute(*prog,arrays,u,173 if case%7==0 else n)
   if not np.array_equal(actual,expected) or not np.array_equal(ac,ec):
    diff=np.flatnonzero(actual!=expected);raise AssertionError('case=%d layout=%d mode=%d dims=%s diff=%s confidence=%d'%(case,width,mode,(w,h),diff[:12].tolist(),np.count_nonzero(ac!=ec)))
   pixel_count+=n;confidence_count+=cn if u[12] else 0
  if mode==3:changed+=int(np.count_nonzero(expected!=image[(int(u[2])-first)*w:(int(u[3])-first)*w]))
 assert pixel_count>100000 and changed>1000 and min(mode_counts)>0 and shifted_grid>0 and offset_bins>0
 result={'status':'passed','actual_mesa_gles_execution':True,'oracle':'published1.9.60actualGPUshader','layouts':[32,64,128],
  'cases':cases,'pixels_compared':int(pixel_count),'confidence_values_compared':int(confidence_count),'changed_full_pixels':int(changed),
  'immutable_bindings_verified':True,'bounded_source_map_and_evidence_rows_verified':True,'split_nonzero_invocation_origins_verified':True,
  'seconds':time.monotonic()-started,'gl_version':getstr(0x1f02).decode(),'renderer':getstr(0x1f01).decode(),
  'reference_sha256':hashlib.sha256(old.read_bytes()).hexdigest(),'shader_sha256':hashlib.sha256(new.read_bytes()).hexdigest(),'physical_android_tested':False}
 result['mode_cases']=mode_counts;result['nonzero_evidence_grid_origins']=shifted_grid;result['resident_preparation_evidence_offsets']=offset_bins
 Path(out).parent.mkdir(parents=True,exist_ok=True);Path(out).write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2));return result
def test(root,work):
 if Path(root).resolve()!=Path(__file__).resolve().parents[1]:raise ValueError('source root differs from executing shader')
 return main(Path(work)/'strong-shared-result.json')
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--out',required=True);main(parser.parse_args().out)
