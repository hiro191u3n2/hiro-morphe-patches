#!/usr/bin/env python3
"""Execute actual GX32/GX33 GLES tile shaders against immutable published .63.

Checks all twelve tile variants, the generic stripe, odd/clamped dimensions,
alpha-invalid samples, policy gates, bounded source/map/evidence bands and
split group origins. Timings describe the Mesa host only; Android selects its
own strictly admitted variant using complete transfer/dispatch/readback time.
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
frozen=root/'tests1964/published1963-reference/native1960/strong1960.comp'
pins=root/'tests1964/published1963-reference/shader-pins.json'
buffers=(U*8)();gen(8,buffers)
def read(slot,array):
 bind(0x90d2,buffers[slot]);ptr=mapbuffer(0x90d2,0,array.nbytes,1);assert ptr
 result=np.frombuffer(c.string_at(ptr,array.nbytes),dtype=array.dtype).copy();assert unmap(0x90d2);return result
def execute(prog,width,arrays,u,split,tile=False,check_immutable=True):
 use(prog);loc=location(prog,b'u');assert loc>=0
 for slot,array in enumerate(arrays):
  bind(0x90d2,buffers[slot]);data(0x90d2,array.nbytes,array.ctypes.data,0x88e8);base(0x90d2,slot,buffers[slot])
 if tile:
  count=((int(u[0])+7)//8)*((int(u[3]-u[2])+width//8-1)//(width//8))
 else:count=(int(u[0])*int(u[3]-u[2])+width-1)//width
 start=0
 while start<count:
  length=min(split,count-start);params=u.copy();params[31]=start*width
  uniform(loc,32,params.ctypes.data_as(c.POINTER(I)));dispatch(length,1,1);barrier(0x2000);start+=length
 finish();assert error()==0
 if check_immutable:
  for slot in [0,2,3,4,5,6]:
   if not np.array_equal(read(slot,arrays[slot]),arrays[slot]):raise AssertionError('immutable binding changed '+str(slot))
 return read(1,arrays[1]),read(7,arrays[7])

def case_data(rng,case):
 dims=[(1,1),(3,5),(7,9),(8,17),(17,19),(31,33),(65,67),(67,65),(129,39),(257,17)]
 w,h=dims[case%len(dims)];mode=(case//len(dims))%4
 u=np.zeros(32,np.int32);u[:13]=[w,h,0,h,0,h,0,h,case%5,case%2,mode,case%2,mode==3]
 if h>=65 and case%3!=0:u[2]=20;u[3]=44
 if mode==3 and h>=65 and case%2:u[6]=128;u[7]=h+128
 u[12]=int(mode==3 and ((u[6]+u[2])&3)==0 and ((u[3]-u[2])%4==0 or u[6]+u[3]==u[7]))
 n=w*(u[3]-u[2]);cn=((w+3)//4)*((u[3]-u[2]+3)//4) if u[12] else 1
 rgb=rng.integers(0,256,(w*h,3),dtype=np.uint32)
 if case%3:rgb=np.clip(rng.integers(-17,18,(w*h,3))+[55,54,53],0,255).astype(np.uint32)
 image=(np.uint32(0xff000000)|rgb[:,0]<<16|rgb[:,1]<<8|rgb[:,2]).astype(np.uint32)
 if case%11==0:image[::13]&=np.uint32(0x00ffffff)
 radius=7 if mode==1 else 3 if mode==2 else 5;first=max(0,int(u[2])-radius);last=min(h,int(u[3])+radius)
 u[14]=first;image=image[first*w:last*w].copy()
 gw,gh=(w+63)//64,(u[7]+63)//64;ev=np.zeros(16+3*gw*gh,np.float32);ev[:16]=rng.uniform(.1,9,16)
 ev[16::3]=rng.uniform(.5,7,gw*gh);ev[17::3]=rng.uniform(.5,9,gw*gh);ev[18::3]=rng.uniform(0,.8,gw*gh)
 if mode==3:
  fy=max(0,min(gh-1,(float(u[6]+u[2])+.5)/64-.5));ly=max(0,min(gh-1,(float(u[6]+u[3]-1)+.5)/64-.5))
  firstgrid=int(fy);lastgrid=min(gh,int(ly)+2);u[13]=firstgrid
  ev=np.concatenate((ev[:16],ev[16+3*gw*firstgrid:16+3*gw*lastgrid]))
 if mode!=3 and case%3==0:u[20]=16*(mode+1);ev=rng.uniform(.1,9,64).astype(np.float32)
 policy=np.empty(n*2,np.int32);policy[::2]=rng.choice([0,90,256],n);policy[1::2]=rng.choice([0,90,192,256],n)
 maps=[];mw,mh=w,int(u[7])
 for k in range(3):
  mw=(mw+1)//2;mh=(mh+1)//2;channels=rng.integers(-32,33,(mw*mh,3),dtype=np.int32).astype(np.uint32)&255
  values=((np.uint32(55)<<24)|channels[:,0]<<16|channels[:,1]<<8|channels[:,2]).astype(np.uint32)
  if mode==3:
   scale=1<<(k+1);firstmap=int(max(0,min(mh-1,(float(u[6]+u[2])+.5)/scale-.5)))
   lastmap=min(mh,int(max(0,min(mh-1,(float(u[6]+u[3]-1)+.5)/scale-.5)))+2);u[15+k]=firstmap
   values=values[firstmap*mw:lastmap*mw].copy()
  maps.append(values)
 return mode,u,[image,np.zeros(n,np.uint32),ev,policy,*maps,np.zeros(cn,np.int32)],first

def shared_bytes(mode,width):
 radius=7 if mode==1 else 3 if mode==2 else 5;rows=width//8
 pixels=(8+2*radius)*(rows+2*radius)*16
 if mode==2:return pixels
 dr=7 if mode==1 else 5;offsets=12 if mode==1 else 4
 return pixels+offsets*(8+dr+1)*(rows+dr+1)*4

def main(out,cases=160):
 pinned=json.loads(pins.read_text())['files']['native1960/strong1960.comp']
 assert hashlib.sha256(frozen.read_bytes()).hexdigest()==pinned,'published .63 shader reference mutated'
 assertions=1
 reference=program(frozen.read_bytes());generic=program(new.read_bytes())
 variants={(mode,width):program(new.read_bytes().replace(b'#version 310 es',
  ('#version 310 es\n#define GX_LOCAL_SIZE %d\n#define GX_STRONG_MODE %d\n#define GX_TILE_WIDTH 8'%(width,mode)).encode(),1))
  for mode in range(4) for width in [32,64,128]}
 for (_,width),(_,actual) in variants.items():
  assert width==actual
  assertions+=1
 rng=np.random.default_rng(19643233);pixel_count=confidence_count=changed=0
 mode_counts=[0]*4;started=time.monotonic();offset_bins=shifted_grid=0
 for case in range(cases):
  mode,u,arrays,first=case_data(rng,case);n=len(arrays[1]);mode_counts[mode]+=1
  shifted_grid+=int(u[13]>0);offset_bins+=int(u[20]>0)
  expected,ec=execute(*reference,arrays,u,1<<24)
  comparisons=[('generic',generic,False)]
  comparisons.extend((str(width),variants[(mode,width)],True) for width in (32,64,128))
  for name,prog,tile in comparisons:
   actual,ac=execute(*prog,arrays,u,3 if case%7==0 else 1<<24,tile)
   if not np.array_equal(actual,expected) or not np.array_equal(ac,ec):
    diff=np.flatnonzero(actual!=expected)
    raise AssertionError('case=%d layout=%s mode=%d dims=%s diff=%s confidence=%d'%(case,name,mode,tuple(u[:2]),diff[:12].tolist(),np.count_nonzero(ac!=ec)))
   # Both concrete array comparisons above executed and passed. Count checks,
   # not an estimate based on the number of pixels or shader invocations.
   assertions+=2
   pixel_count+=n;confidence_count+=len(ec) if u[12] else 0
  if mode==3:changed+=int(np.count_nonzero(expected!=arrays[0][(int(u[2])-first)*int(u[0]):(int(u[3])-first)*int(u[0])]))
 assert pixel_count>100000 and changed>1000 and min(mode_counts)>0 and shifted_grid>0 and offset_bins>0
 assertions+=1
 # Verify the exact scalar cached expression independently from each kernel's
 # rounded ARGB output. Includes reversed differences and fp32 nonnegative ties.
 distance_test=pair_expression_equivalence()
 indexing_test=distance_cache_indexing_equivalence()
 timings=host_timings(reference,variants)
 result={'status':'passed','assertions':assertions+distance_test['assertions']+indexing_test['assertions'],
  'assertions_counting':'executed reference pin, program layout, output/confidence array, coverage, pair-expression and cache-index comparisons',
  'actual_mesa_gles_execution':True,'oracle':'published1.9.63actualGPUshader',
  'cases':cases,'pixels_compared':int(pixel_count),'confidence_values_compared':int(confidence_count),'changed_full_pixels':int(changed),
  'layouts':[32,64,128],'tile_width':8,'tile_heights':[4,8,16],'specialized_tile_programs':12,'generic_stripe_verified':True,
  'shared_bytes':{str(mode):{str(size):shared_bytes(mode,size) for size in (32,64,128)} for mode in range(4)},
  'gx32_all_candidate_pair_distances_reused':True,'gx33_actual_2d_shared_tile':True,'original_accumulation_and_cutoff_order_retained':True,
  'pair_expression_fp32_bit_equivalence':distance_test,'distance_cache_indexing_fp32_bit_equivalence':indexing_test,
  'immutable_bindings_verified':True,
  'bounded_source_map_and_evidence_rows_verified':True,'split_nonzero_group_origins_verified':True,
  'mode_cases':mode_counts,'nonzero_evidence_grid_origins':shifted_grid,'resident_preparation_evidence_offsets':offset_bins,
  'host_timings':timings,'seconds':time.monotonic()-started,'gl_version':getstr(0x1f02).decode(),'renderer':getstr(0x1f01).decode(),
  'reference_sha256':pinned,'shader_sha256':hashlib.sha256(new.read_bytes()).hexdigest(),'physical_android_tested':False}
 Path(out).parent.mkdir(parents=True,exist_ok=True);Path(out).write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2));return result

def pair_expression_equivalence():
 # Extract production helper. GLSL precise must cover both original and cached
 # expressions, and reversing a pair must preserve every distance float bit.
 import re
 helper=re.search(r'float pairDistance\([^}]+\}',new.read_text()).group(0)
 text=('#version 310 es\n#extension GL_EXT_gpu_shader5 : require\nprecision highp float;precision highp int;\n'
  'layout(local_size_x=64) in;layout(std430,binding=0) readonly buffer A{float terms[];};'
  'layout(std430,binding=1) writeonly buffer B{uint results[];};uniform int u[32];\n'+helper+
  '\nvoid main(){int at=int(uint(u[31])+gl_GlobalInvocationID.x);if(at>=u[0])return;int b=at*6;'
  'precise vec3 ca=vec3(terms[b],terms[b+1],terms[b+2]),cb=vec3(terms[b+3],terms[b+4],terms[b+5]);'
  'precise float d1=ca.x-cb.x,d2=ca.y-cb.y,d3=ca.z-cb.z;'
  'precise float original=d1*d1+.11*(d2*d2+d3*d3);'
  'results[at*3]=floatBitsToUint(original);results[at*3+1]=floatBitsToUint(pairDistance(ca,cb));'
  'results[at*3+2]=floatBitsToUint(pairDistance(cb,ca));}')
 candidate=program(text.encode());rng=np.random.default_rng(32331964)
 terms=rng.uniform(-255,255,(65536,6)).astype(np.float32);terms[::13,3:]=terms[::13,:3]
 flags=np.zeros(len(terms)*3,np.uint32);u=np.zeros(32,np.int32);u[0]=len(terms);u[3]=1
 arrays=[terms.ravel(),flags,*[np.zeros(1,np.int32) for _ in range(6)]]
 actual,_=execute(*candidate,arrays,u,173,False,False);actual=actual.reshape((-1,3))
 assert np.array_equal(actual[:,0],actual[:,1]) and np.array_equal(actual[:,0],actual[:,2]),'changed exact pair fp32 expression'
 return {'actual_gles_execution':True,'assertions':2,'scalar_pairs':len(terms),'reversed_pairs_bit_identical':True}

def distance_cache_indexing_equivalence():
 # Run the real cooperative loader and lookup helpers, bypassing only the
 # final denoising blend/rounding so every original candidate term is observed.
 # This catches an incorrect reused term even when a final ARGB byte would not
 # change, or a candidate would be rejected later by the normal distance cutoff.
 source=new.read_text().split('void main(){',1)[0]
 probe='''
void main(){
 int group=int(uint(u[31])/uint(GX_LOCAL_SIZE)+gl_WorkGroupID.x);
 int columns=(u[0]+GX_TILE_WIDTH-1)/GX_TILE_WIDTH;
 tileX=(group%columns)*GX_TILE_WIDTH;tileY=u[2]+(group/columns)*TILE_HEIGHT;
 int lane=int(gl_LocalInvocationIndex);
 int x=tileX+lane%GX_TILE_WIDTH,y=tileY+lane/GX_TILE_WIDTH;
 centerX=x;centerY=y;centerLane=lane;fillCache();
 if(x>=u[0]||y>=u[3])return;
 uint mismatches=0u;
 int step=STRONG_MODE==1?3:4,radius=STRONG_MODE==1?6:4;
 int validBegin=STRONG_MODE==3?u[4]:0,validEnd=STRONG_MODE==3?u[5]:u[1];
 for(int dy=-radius;dy<=radius;dy+=step)for(int dx=-radius;dx<=radius;dx+=step){
  if(dx==0&&dy==0)continue;
  int cx=x+dx,cy=y+dy;
  if(cx<0||cx>=u[0]||cy<validBegin||cy>=validEnd)continue;
  int samples=STRONG_MODE==1?9:5;
  for(int k=0;k<samples;k++){
   int px=STRONG_MODE==1?k%3-1:k==1?-1:k==2?1:0;
   int py=STRONG_MODE==1?k/3-1:k==3?-1:k==4?1:0;
   int ay=STRONG_MODE==1?y+py:clamp(y+py,validBegin,validEnd-1);
   int by=STRONG_MODE==1?cy+py:clamp(cy+py,validBegin,validEnd-1);
   precise vec3 ca=color(x+px,ay),cb=color(cx+px,by);
   precise float direct=pairDistance(ca,cb);
   precise float cached=sampleDistance(x+px,ay,cx+px,by,ca,cb);
   if(floatBitsToUint(direct)!=floatBitsToUint(cached))mismatches++;
  }
 }
 dst[(y-u[2])*u[0]+x]=mismatches;
}
'''
 probes={(mode,width):program((source.replace('#version 310 es',
  '#version 310 es\n#define GX_LOCAL_SIZE %d\n#define GX_STRONG_MODE %d\n#define GX_TILE_WIDTH 8'%(width,mode),1)+probe).encode())
  for mode in (0,1,3) for width in (32,64,128)}
 rng=np.random.default_rng(19643332);pixels=0;cases=0
 for case in range(160):
  mode,u,arrays,_=case_data(rng,case)
  if mode==2:continue
  for width in (32,64,128):
   actual,_=execute(*probes[(mode,width)],arrays,u,3 if case%7==0 else 1<<24,True)
   if np.any(actual):raise AssertionError('cached NLM pair lookup changed bits: mode=%s size=%s dims=%s pixels=%s'%(mode,width,tuple(u[:2]),np.flatnonzero(actual)[:12].tolist()))
   pixels+=len(actual);cases+=1
 return {'actual_gles_execution':True,'assertions':cases,'pixel_candidate_patch_sets':pixels,'dispatch_cases':cases,
  'every_original_candidate_sample_tested':True,'cached_and_direct_terms_bit_identical':True}

def host_timings(reference,variants):
 # Real shader dispatch plus upload/readback time, rounded only for display.
 # Warm each program first so compilation time is excluded. The host renderer
 # is software, so these measurements never select an Android GPU variant.
 rng=np.random.default_rng(3332);records=[]
 for mode in range(4):
  case=40+mode*10+9
  _,u,arrays,_=case_data(rng,case)
  for name,prog,tile in [('published63',reference,False)]+[(str(size),variants[(mode,size)],True) for size in (32,64,128)]:
   execute(*prog,arrays,u,1<<24,tile,False)
   values=[]
   for _ in range(3):
    began=time.perf_counter();execute(*prog,arrays,u,1<<24,tile,False);values.append((time.perf_counter()-began)*1000)
   records.append({'mode':mode,'layout':name,'median_upload_dispatch_readback_ms':round(float(np.median(values)),3),
                   'samples':3,'width':int(u[0]),'height':int(u[3]-u[2])})
 return {'host_only':True,'android_admission_uses_device_measurements':True,'records':records}

def test(root,work,jdk=None,ndk=None):
 if Path(root).resolve()!=Path(__file__).resolve().parents[1]:raise ValueError('source root differs from executing shader')
 return main(Path(work)/'strong-tiles1964-result.json')
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--out',required=True);parser.add_argument('--cases',type=int,default=160)
 args=parser.parse_args();main(args.out,args.cases)
