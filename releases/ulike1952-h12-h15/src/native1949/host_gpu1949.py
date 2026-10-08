#!/usr/bin/env python3
"""Execute production integer shaders on surfaceless software EGL, not a mock."""
import argparse, ctypes as C, hashlib, json, math, os, pathlib, random, subprocess, time

class SoftwareGpu:
    def __init__(self,source):
        os.environ['EGL_PLATFORM']='surfaceless';os.environ['LIBGL_ALWAYS_SOFTWARE']='1'
        self.source=pathlib.Path(source);self.egl=C.CDLL('libEGL.so.1');self.functions={};self.buffers=[];self.capacities=[0]*5
        def egl(name,restype,args):
            fn=getattr(self.egl,name);fn.restype=restype;fn.argtypes=args;return fn
        self.getproc=egl('eglGetProcAddress',C.c_void_p,[C.c_char_p])
        self.display=egl('eglGetDisplay',C.c_void_p,[C.c_void_p])(None)
        self.terminate=egl('eglTerminate',C.c_uint,[C.c_void_p])
        self.makecurrent=egl('eglMakeCurrent',C.c_uint,[C.c_void_p,C.c_void_p,C.c_void_p,C.c_void_p])
        self.destroycontext=egl('eglDestroyContext',C.c_uint,[C.c_void_p,C.c_void_p])
        self.destroysurface=egl('eglDestroySurface',C.c_uint,[C.c_void_p,C.c_void_p])
        if not egl('eglInitialize',C.c_uint,[C.c_void_p,C.POINTER(C.c_int),C.POINTER(C.c_int)])(self.display,None,None):raise RuntimeError('Software EGL initialize failed')
        if not egl('eglBindAPI',C.c_uint,[C.c_uint])(0x30A0):raise RuntimeError('Software GLES API binding failed')
        attrs=(C.c_int*13)(0x3033,1,0x3040,0x40,0x3024,8,0x3023,8,0x3022,8,0x3021,8,0x3038)
        config=C.c_void_p();n=C.c_int()
        if not egl('eglChooseConfig',C.c_uint,[C.c_void_p,C.POINTER(C.c_int),C.POINTER(C.c_void_p),C.c_int,C.POINTER(C.c_int)])(self.display,attrs,C.byref(config),1,C.byref(n)) or n.value!=1:raise RuntimeError('Software GLES3 config unavailable')
        pbuffer=(C.c_int*5)(0x3057,1,0x3056,1,0x3038);ctxattrs=(C.c_int*3)(0x3098,3,0x3038)
        self.surface=egl('eglCreatePbufferSurface',C.c_void_p,[C.c_void_p,C.c_void_p,C.POINTER(C.c_int)])(self.display,config,pbuffer)
        self.context=egl('eglCreateContext',C.c_void_p,[C.c_void_p,C.c_void_p,C.c_void_p,C.POINTER(C.c_int)])(self.display,config,None,ctxattrs)
        if not self.surface or not self.context or not self.makecurrent(self.display,self.surface,self.surface,self.context):raise RuntimeError('Software GLES3 context failed')
        self.gl('glGetString',C.c_char_p,[C.c_uint]);self.renderer=self.functions['glGetString'](0x1F01).decode();self.version=self.functions['glGetString'](0x1F02).decode()
        if 'OpenGL ES 3.' not in self.version:raise RuntimeError('Software GLES3.1 compute unavailable')
        specs={
            'glGetError':(C.c_uint,[]),'glCreateShader':(C.c_uint,[C.c_uint]),'glShaderSource':(None,[C.c_uint,C.c_int,C.POINTER(C.c_char_p),C.POINTER(C.c_int)]),
            'glCompileShader':(None,[C.c_uint]),'glGetShaderiv':(None,[C.c_uint,C.c_uint,C.POINTER(C.c_int)]),
            'glGetShaderInfoLog':(None,[C.c_uint,C.c_int,C.POINTER(C.c_int),C.c_char_p]),'glDeleteShader':(None,[C.c_uint]),
            'glCreateProgram':(C.c_uint,[]),'glAttachShader':(None,[C.c_uint,C.c_uint]),'glLinkProgram':(None,[C.c_uint]),
            'glGetProgramiv':(None,[C.c_uint,C.c_uint,C.POINTER(C.c_int)]),'glGetProgramInfoLog':(None,[C.c_uint,C.c_int,C.POINTER(C.c_int),C.c_char_p]),
            'glDeleteProgram':(None,[C.c_uint]),'glUseProgram':(None,[C.c_uint]),'glGetUniformLocation':(C.c_int,[C.c_uint,C.c_char_p]),'glUniform1i':(None,[C.c_int,C.c_int]),
            'glGenBuffers':(None,[C.c_int,C.POINTER(C.c_uint)]),'glDeleteBuffers':(None,[C.c_int,C.POINTER(C.c_uint)]),
            'glBindBuffer':(None,[C.c_uint,C.c_uint]),'glBufferData':(None,[C.c_uint,C.c_ssize_t,C.c_void_p,C.c_uint]),
            'glBufferSubData':(None,[C.c_uint,C.c_ssize_t,C.c_ssize_t,C.c_void_p]),'glBindBufferBase':(None,[C.c_uint,C.c_uint,C.c_uint]),
            'glDispatchCompute':(None,[C.c_uint,C.c_uint,C.c_uint]),'glMemoryBarrier':(None,[C.c_uint]),
            'glFenceSync':(C.c_void_p,[C.c_uint,C.c_uint]),'glClientWaitSync':(C.c_uint,[C.c_void_p,C.c_uint,C.c_uint64]),'glDeleteSync':(None,[C.c_void_p]),'glFlush':(None,[]),
            'glMapBufferRange':(C.c_void_p,[C.c_uint,C.c_ssize_t,C.c_ssize_t,C.c_uint]),'glUnmapBuffer':(C.c_uint,[C.c_uint])}
        for name,(result,args) in specs.items():self.gl(name,result,args)
        self.programs=[self.compile(name) for name in ['fusion_sums1949','pack_rgb1949','residual1949']]
        self.buffer_array=(C.c_uint*5)();self.functions['glGenBuffers'](5,self.buffer_array);self.buffers=list(self.buffer_array);self.check()
    def gl(self,name,restype,args):
        address=self.getproc(name.encode());
        if not address:raise RuntimeError('Missing GLES function '+name)
        self.functions[name]=C.CFUNCTYPE(restype,*args)(address)
    def check(self):
        error=self.functions['glGetError']()
        if error:raise RuntimeError('GL error '+hex(error))
    def compile(self,name):
        f=self.functions;s=f['glCreateShader'](0x91B9);text=(self.source/(name+'.comp')).read_bytes();ptr=C.c_char_p(text)
        f['glShaderSource'](s,1,C.byref(ptr),None);f['glCompileShader'](s);ok=C.c_int();f['glGetShaderiv'](s,0x8B81,C.byref(ok))
        if not ok.value:
            log=C.create_string_buffer(8192);f['glGetShaderInfoLog'](s,8192,None,log);raise RuntimeError(name+': '+log.value.decode())
        p=f['glCreateProgram']();f['glAttachShader'](p,s);f['glLinkProgram'](p);f['glDeleteShader'](s);f['glGetProgramiv'](p,0x8B82,C.byref(ok))
        if not ok.value:
            log=C.create_string_buffer(8192);f['glGetProgramInfoLog'](p,8192,None,log);raise RuntimeError(name+': '+log.value.decode())
        self.check();return p
    def upload(self,slot,data,size=None):
        f=self.functions
        if isinstance(data,bytes):holder=C.create_string_buffer(data);size=len(data);pointer=C.cast(holder,C.c_void_p)
        elif data is None:pointer=None
        else:size=C.sizeof(data);pointer=C.cast(data,C.c_void_p)
        f['glBindBuffer'](0x90D2,self.buffers[slot]);
        if size>self.capacities[slot]:
            capacity=(size+4095)&~4095;f['glBufferData'](0x90D2,capacity,None,0x88EA);self.capacities[slot]=capacity
        if pointer:f['glBufferSubData'](0x90D2,0,size,pointer)
        f['glBindBufferBase'](0x90D2,slot,self.buffers[slot]);self.check()
    def use(self,index,values):
        f=self.functions;p=self.programs[index];f['glUseProgram'](p)
        for name,value in values.items():
            location=f['glGetUniformLocation'](p,name.encode())
            if location<0:raise RuntimeError('Missing uniform '+name)
            f['glUniform1i'](location,value)
    def read(self,slot,count):
        f=self.functions;f['glBindBuffer'](0x90D2,self.buffers[slot]);p=f['glMapBufferRange'](0x90D2,0,count*4,1)
        if not p:raise RuntimeError('Map failed')
        data=list((C.c_int32*count).from_address(p));
        if not f['glUnmapBuffer'](0x90D2):raise RuntimeError('Map invalidated')
        self.check();return data
    def fence(self):
        f=self.functions;f['glMemoryBarrier'](0x2000|0x0200);sync=f['glFenceSync'](0x9117,0);f['glFlush']()
        if not sync:raise RuntimeError('Fence missing')
        # Software CI may be slow; same bounded ordering, with a2second test timeout.
        result=f['glClientWaitSync'](sync,1,2000000000);f['glDeleteSync'](sync)
        if result not in [0x911A,0x911C]:raise RuntimeError('Software fence failed '+hex(result))
        self.check()
    def fusion(self,data,w,h,step):
        sw=(w+step-1)//step;sh=(h+step-1)//step;n=sw*sh;data=data[:w*h]+bytes((-w*h)%4)
        self.upload(0,data);self.upload(1,None,n*4);self.upload(2,None,n*4)
        self.use(0,dict(width=w,height=h,step=step,smallWidth=sw,smallHeight=sh));self.functions['glDispatchCompute']((sw+7)//8,(sh+7)//8,1)
        self.fence();return self.read(1,n),self.read(2,n)
    def aggregate(self,src,meta,seed,w,begin,end,lo,hi,radius,ranges):
        origin=max(lo,begin-radius);bottom=min(hi,end+radius);compact=src[origin*w:bottom*w]
        arr=lambda values:(C.c_int32*len(values))(*values)
        self.upload(0,arr(compact));self.upload(1,None,len(compact)*4);self.upload(2,arr(meta));self.upload(3,arr(seed));self.upload(4,arr(ranges))
        self.use(1,dict(count=len(compact)));self.functions['glDispatchCompute']((len(compact)+63)//64,1,1)
        self.functions['glMemoryBarrier'](0x2000)
        self.use(2,dict(width=w,begin=begin,end=end,lo=lo,hi=hi,radius=radius,inputOrigin=origin,inputRows=bottom-origin))
        self.functions['glDispatchCompute']((w+7)//8,(end-begin+7)//8,1);self.fence();return self.read(3,len(seed))
    def close(self):
        self.functions['glDeleteBuffers'](5,self.buffer_array)
        for p in self.programs:self.functions['glDeleteProgram'](p)
        self.makecurrent(self.display,None,None,None);self.destroycontext(self.display,self.context);self.destroysurface(self.display,self.surface);self.terminate(self.display)

def test(root,work):
    root=pathlib.Path(root);source=root if (root/'gpu1949.c').exists() else root/'native1949';work=pathlib.Path(work);work.mkdir(parents=True,exist_ok=True)
    oracle=work/'residual-oracle1949.so'
    subprocess.run(['cc','-std=c11','-O2','-shared','-fPIC','-DULIKE_RESIDUAL_SCALAR',str(source.parent/'native1944/residual1944.c'),'-o',str(oracle)],check=True)
    cpu=C.CDLL(str(oracle.resolve()));ip=C.POINTER(C.c_int32)
    cpu.residual1944_aggregate.argtypes=[ip,ip,ip,C.c_int,C.c_int,C.c_int,C.c_int,C.c_int,C.c_int,ip,ip,ip]
    rng=random.Random(1949);gpu=SoftwareGpu(source);assertions=0;fusion_cases=0;noise_cases=0;started=time.monotonic()
    fusion_dimensions=[(1,1,1),(3,5,2),(16,17,1),(257,259,2),(513,511,4),(1025,769,8),(4080,3060,16),(4095,3071,16)]
    try:
        for w,h,step in fusion_dimensions:
            data=rng.randbytes(w*h);before=hashlib.sha256(data).hexdigest();sums,counts=gpu.fusion(data,w,h,step);expected_sums=[];expected_counts=[]
            for y in range(0,h,step):
                for x in range(0,w,step):
                    right=min(w,x+step);bottom=min(h,y+step)
                    expected_sums.append(sum(sum(data[yy*w+x:yy*w+right]) for yy in range(y,bottom)))
                    expected_counts.append((right-x)*(bottom-y))
            if sums!=expected_sums or counts!=expected_counts:raise AssertionError('Fusion integer sums mismatch '+str((w,h,step)))
            if before!=hashlib.sha256(data).hexdigest():raise AssertionError('Fusion input changed')
            assertions+=len(sums)*2+1;fusion_cases+=1
        cases=[(1,1),(3,5),(7,9),(8,8),(9,7),(17,19),(33,31),(128,17),(4080,32)]
        ranges=[int(math.floor(256*math.exp(-d*d/(2*s*s))+.5)) if s else 0 for s in range(33) for d in range(256)]
        for w,rows in cases:
            for radius in [1,2,3,4]:
                for mode in ['noise','periodic','transparent','mixed','zero_range','white_maximum']:
                    lo=1 if rows>5 else 0;hi=rows-1 if rows>5 else rows
                    begin=lo+(hi-lo)//3 if mode=='mixed' else lo;end=hi-(hi-lo)//4 if mode=='mixed' else hi
                    src=[]
                    for y in range(rows):
                        for x in range(w):
                            if mode=='white_maximum':g=255
                            elif mode=='periodic':g=[8,24,8,24][(x+y)%4]+rng.randrange(5)
                            else:g=100+rng.randrange(-20,21)
                            r=max(0,min(255,g+rng.randrange(-12,13)));b=max(0,min(255,g+rng.randrange(-12,13)))
                            if mode=='white_maximum':r=b=255
                            alpha=0 if mode=='transparent' or mode=='mixed' and rng.randrange(5)==0 else 255
                            src.append((alpha<<24)|(r<<16)|(g<<8)|b)
                    n=w*(end-begin);meta=[(0x80000000 if i%7 else 0)|rng.randrange(2,33)|(rng.randrange(3,64)<<6) for i in range(n)]
                    seed=[rng.getrandbits(32) for _ in range(n*3)]
                    active_ranges=[0]*(33*256) if mode=='zero_range' else [256]*(33*256) if mode=='white_maximum' else ranges
                    array=lambda values:(C.c_int32*len(values))(*values)
                    a=array(src);m=array(meta);out=array(seed);r=array(active_ranges);ring=(C.c_int32*(w*(radius*2+1)))()
                    offsets=[-4,-2,-1,0,1,2,4] if radius==4 else list(range(-radius,radius+1))
                    x=array([max(0,min(w-1,col+dx)) for col in range(w) for dx in offsets])
                    cpu.residual1944_aggregate(a,m,out,w,begin,end,lo,hi,radius,r,ring,x)
                    got=gpu.aggregate(src,meta,seed,w,begin,end,lo,hi,radius,active_ranges);expected=list(out)
                    if got!=expected:
                        first=next(i for i,(left,right) in enumerate(zip(got,expected)) if left!=right)
                        raise AssertionError('Residual mismatch '+str((w,rows,radius,mode,first,got[first],expected[first])))
                    assertions+=n*3+3;noise_cases+=1
        diagnostics={'renderer':gpu.renderer,'gles_version':gpu.version,'elapsed_seconds':round(time.monotonic()-started,3)}
        (work/'host-gpu1949-environment.json').write_text(json.dumps(diagnostics,indent=2)+'\n')
        print('Software EGL diagnostics: '+json.dumps(diagnostics),flush=True)
        report={'schema':'ulike-gpu1949-software-test-v1','status':'passed','assertions':assertions,'fusion_cases':fusion_cases,'noise_cases':noise_cases,
            'renderer':'Mesa software EGL','gles_version':'OpenGL ES3.1 compute or newer','software_egl_shaders_executed':True,
            'integer_summary_bit_exact':True,'reference':'unchanged scalar residual1944.c','full_4080x3060_thumbnail_tested':True,
            'opaque_zero_weight_and_maximum_rgb_accumulation_tested':True,
            'repeated_buffer_growth_and_reuse_tested':True,'physical_android_tested':False,
            'sources':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in [source/'host_gpu1949.py',source/'gpu1949.c']+sorted(source.glob('*.comp'))}}
        (work/'host-gpu1949.json').write_text(json.dumps(report,indent=2)+'\n');return report
    finally:gpu.close()
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--work',required=True);args=parser.parse_args()
    print(json.dumps(test(pathlib.Path(__file__).resolve().parent,args.work)))
