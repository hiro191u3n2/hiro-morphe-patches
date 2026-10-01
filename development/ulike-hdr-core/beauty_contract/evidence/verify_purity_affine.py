#!/usr/bin/env python3
"""Run only the bounded original Purity similarity-math block on synthetic points.

No model weights, native constructors, I/O, licence checks, or image decoding run.
A passing result validates this block, not complete native image parity.
"""
if not __debug__:
    raise RuntimeError("Validation requires assertions; run without -O or -OO")

import argparse,hashlib,json,struct
from pathlib import Path
import numpy as np
from unicorn import Uc,UC_ARCH_ARM64,UC_MODE_ARM
from unicorn.arm64_const import UC_ARM64_REG_SP,UC_ARM64_REG_X22,UC_ARM64_REG_X28,UC_ARM64_REG_Q7

PIN='d40af10b250b91cf8f30f4a265ac1d3b7b5b88a82bbf63da332c3f47a415d48e'
MAX_INPUT_BYTES = 64 * 1024 * 1024

def bounded_read(path):
    with path.open('rb') as stream:
        value = stream.read(MAX_INPUT_BYTES + 1)
    if len(value) > MAX_INPUT_BYTES:
        raise ValueError('Input exceeds the 64 MiB limit')
    return value

def pinned_library(path):
    value = bounded_read(path)
    if hashlib.sha256(value).hexdigest() != PIN:
        raise ValueError('Unsupported libeffect.so SHA-256')
    return value

def main():
    ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('library',type=Path);ap.add_argument('--output',type=Path);args=ap.parse_args()
    library=pinned_library(args.library)
    uc=Uc(UC_ARCH_ARM64,UC_MODE_ARM)
    for addr,size in [(0xc90000,0x30000),(0xe40000,0x60000)]:
        uc.mem_map(addr,size);uc.mem_write(addr,library[addr:addr+size])
    sp=0x30000000;points=0x31000000;result=0x32000000
    for addr in (sp,points,result):uc.mem_map(addr,0x20000)
    rng=np.random.default_rng(90817);rows=[]
    for case,(a,b,tx,ty) in enumerate([(1,0,0,0),(.8,.4,.2,.3),(.6,-.25,-.4,.9),(-.3,.8,.8,.1),(2.3,1.8,2,-1),(.05,0,.5,.5)]):
        source=rng.normal(size=(106,2)).astype(np.float32)
        target=(source @ np.array([[a,b],[-b,a]],np.float32)+np.array([tx,ty],np.float32)).astype(np.float32)
        sm=source.mean(axis=0,dtype=np.float64).astype(np.float32)
        tm=target.mean(axis=0,dtype=np.float64).astype(np.float32)
        s=(source-sm).astype(np.float32);t=(target-tm).astype(np.float32)
        uc.mem_write(points,s.tobytes());uc.mem_write(points+0x1000,t.tobytes())
        uc.mem_write(sp+0x6ea8,struct.pack('<Q',points));uc.mem_write(sp+0x6d18,struct.pack('<Q',points+0x1000))
        uc.mem_write(sp+0x40,sm.tobytes()+bytes(8));uc.reg_write(UC_ARM64_REG_Q7,int.from_bytes(tm.tobytes()+bytes(8),'little'))
        uc.reg_write(UC_ARM64_REG_SP,sp);uc.reg_write(UC_ARM64_REG_X22,106*8);uc.reg_write(UC_ARM64_REG_X28,result)
        uc.emu_start(0xca02f8,0xca0370,count=20000)
        actual=np.frombuffer(uc.mem_read(result+0x1c0,24),dtype='<f4').reshape(2,3)
        s64=s.astype(np.float64);t64=t.astype(np.float64);den=(s64*s64).sum()
        aa=(s64*t64).sum()/den;bb=(s64[:,0]*t64[:,1]-s64[:,1]*t64[:,0]).sum()/den
        linear=np.array([[aa,-bb],[bb,aa]]);expected=np.c_[linear,tm.astype(np.float64)-linear@sm.astype(np.float64)]
        error=float(np.max(np.abs(actual-expected)))
        assert error<2e-6,(case,error)
        rows.append({'case':case,'points':106,'max_abs_matrix_error_vs_float64':error})
    output={'status':'PASS_BOUNDED_NATIVE_PURITY_SIMILARITY','library_sha256':PIN,'source_sha256':hashlib.sha256(bounded_read(Path(__file__))).hexdigest(),'emulator':'unicorn 2.1.4',
      'native_entry':'0xca02f8','native_stop_before':'0xca0370','helpers':['0xe5222c','0xe4a12c','0xe771dc'],
      'cases':rows,'max_abs_matrix_error':max(x['max_abs_matrix_error_vs_float64'] for x in rows),
      'contract':'The executed block consumes centered source and target arrays plus supplied means, and builds a source-to-target similarity matrix [a,-b,tx;b,a,ty].',
      'limits':['Only this arithmetic block runs; inputs are synthetic already-centered corresponding points.','Face selection, mean accumulation, crop sampling, matrix multiplication, full model inference and HDR are not validated by this bounded test.']}
    if args.output:args.output.write_text(json.dumps(output,indent=2)+'\n')
    print(json.dumps(output,indent=2))
if __name__=='__main__':main()
