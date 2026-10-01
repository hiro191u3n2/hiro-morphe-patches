#!/usr/bin/env python3
"""Independent replay of pinned ARM64 scalar/NEON kernels on synthetic tensors.

Requires the original caller-supplied libbytenn.so; no vendor bytes embedded.
This is kernel emulation, not Android engine, device, or full-graph execution.
"""
import argparse
import hashlib
import io
import json
import math
from pathlib import Path
import struct

import numpy as np
from elftools.elf.elffile import ELFFile
from unicorn import Uc, UC_ARCH_ARM64, UC_MODE_ARM, UC_HOOK_CODE
from unicorn.arm64_const import UC_ARM64_REG_X0, UC_ARM64_REG_X1, UC_ARM64_REG_X2, UC_ARM64_REG_X30, UC_ARM64_REG_SP, UC_ARM64_REG_S0, UC_ARM64_REG_PC

LIB_SHA = 'cedda347b55c03de82cc22b7f003777f40750d4f341494a29587d7b821ad7853'
ARENA, STOP = 0x1000000, 0x2000000


class Kernel:
    def __init__(self, path):
        with Path(path).open('rb') as source:
            raw = source.read(64*1024*1024+1)
        if len(raw)>64*1024*1024:
            raise ValueError('library size bound')
        if hashlib.sha256(raw).hexdigest() != LIB_SHA:
            raise ValueError('unsupported original library')
        self.uc = Uc(UC_ARCH_ARM64, UC_MODE_ARM)
        self.uc.mem_map(0, 0x300000)
        with io.BytesIO(raw) as f:
            elf = ELFFile(f)
            for segment in elf.iter_segments():
                if segment['p_type'] == 'PT_LOAD':
                    self.uc.mem_write(segment['p_vaddr'], segment.data())
        self.uc.mem_map(ARENA, 0x800000)
        self.uc.mem_map(STOP, 0x1000)
        self.uc.hook_add(UC_HOOK_CODE, self.stub)
        self.reset()

    def reset(self):
        self.cursor = ARENA
        self.uc.reg_write(UC_ARM64_REG_SP, ARENA + 0x7ff000)
        self.uc.reg_write(UC_ARM64_REG_X30, STOP)

    def allocate(self, size, contents=b''):
        ptr = self.cursor
        self.cursor += (size + 63) & ~63
        if self.cursor >= ARENA + 0x700000:
            raise ValueError('synthetic allocation bound')
        self.uc.mem_write(ptr, b'\0' * size)
        if contents:
            self.uc.mem_write(ptr, contents)
        return ptr

    def word(self, ptr, value, wide=False):
        self.uc.mem_write(ptr, struct.pack('<Q' if wide else '<I', value))

    def tensor(self, array):
        n, h, w, c = array.shape
        ptr = self.allocate(0x100)
        data = self.allocate(array.nbytes, array.tobytes())
        for off, value in zip((0x24,0x28,0x2c,0x30,0x34,0x3c), (n,h,w,c,array.size,4)):
            self.word(ptr+off, value)
        self.word(ptr+0x78, data, True)
        return ptr, data

    def stub(self, uc, address, size, user_data):
        if address == 0x1ec00:  # imported memcpy
            dest, source, length = [uc.reg_read(r) for r in (UC_ARM64_REG_X0,UC_ARM64_REG_X1,UC_ARM64_REG_X2)]
            if length > 1000000:
                raise ValueError('unexpected memcpy size')
            uc.mem_write(dest, bytes(uc.mem_read(source, length)))
            uc.reg_write(UC_ARM64_REG_PC, uc.reg_read(UC_ARM64_REG_X30))  # PC
        elif address == 0x1ef00:  # imported expf, only scalar Tanh tail
            x = struct.unpack('<f', struct.pack('<I', uc.reg_read(UC_ARM64_REG_S0)))[0]
            value = math.exp(x) if x < 88.722839 else math.inf
            uc.reg_write(UC_ARM64_REG_S0, struct.unpack('<I', struct.pack('<f', value))[0])
            uc.reg_write(UC_ARM64_REG_PC, uc.reg_read(UC_ARM64_REG_X30))

    def run(self, address, x0, x1=0):
        self.uc.reg_write(UC_ARM64_REG_X0, x0)
        self.uc.reg_write(UC_ARM64_REG_X1, x1)
        self.uc.emu_start(address, STOP, timeout=10_000_000, count=20_000_000)
        # Fail closed if emulation budget expires instead of returning.
        from unicorn.arm64_const import UC_ARM64_REG_PC
        if self.uc.reg_read(UC_ARM64_REG_PC) != STOP:
            raise RuntimeError('kernel did not return')

    def floats(self, pointer, shape):
        return np.frombuffer(bytes(self.uc.mem_read(pointer, math.prod(shape)*4)), dtype='<f4').copy().reshape(shape)


def verify(path):
    if not __debug__:
        raise RuntimeError('verification assertions must be enabled')
    kernel = Kernel(path)
    rng = np.random.default_rng(168)
    result = {'library_sha256': LIB_SHA, 'scope': 'original ARM64 kernel emulation on synthetic input; no complete original-engine execution', 'eltwise': [], 'concat': [], 'tanh': {}}
    for count in (1,3,8,16,17,31,32,64,257):
        for relu in (0,1):
            kernel.reset()
            a = rng.uniform(-2,2,(1,1,1,count)).astype('float32')
            b = rng.uniform(-2,2,a.shape).astype('float32')
            ta, _ = kernel.tensor(a)
            tb, _ = kernel.tensor(b)
            tc, out = kernel.tensor(np.zeros_like(a))
            inputs = kernel.allocate(16,struct.pack('<QQ',ta,tb))
            layer = kernel.allocate(0x200)
            kernel.word(layer+0xc8, inputs, True)
            kernel.word(layer+0xf8, tc, True)
            kernel.word(layer+0x108, relu)
            kernel.run(0xe0c18,layer)
            got = kernel.floats(out,a.shape)
            expected = a+b
            if relu: expected = np.maximum(expected,np.float32(0))
            assert np.array_equal(got,expected), ('eltwise',count,relu)
            result['eltwise'].append({'elements':count,'relu':bool(relu),'bit_exact':True})
    for n,h,w,c0,c1 in ((1,3,5,2,3),(1,2,3,8,17),(2,4,5,16,8),(1,1,1,1,1)):
        kernel.reset()
        a=rng.normal(size=(n,h,w,c0)).astype('float32');b=rng.normal(size=(n,h,w,c1)).astype('float32')
        ta,_=kernel.tensor(a);tb,_=kernel.tensor(b)
        inputs=kernel.allocate(16,struct.pack('<QQ',ta,tb))
        vector=kernel.allocate(16,struct.pack('<QQ',inputs,inputs+16))
        shape=(n,h,w,c0+c1)
        output=kernel.allocate(math.prod(shape)*4)
        worker=kernel.allocate(0x40)
        kernel.word(worker+8,vector,True);kernel.word(worker+0x10,output,True)
        kernel.word(worker+0x18,w);kernel.word(worker+0x1c,c0+c1)
        span=kernel.allocate(8,struct.pack('<II',0,h))
        kernel.run(0x79ea0,worker,span)
        got=kernel.floats(output,shape)
        assert np.array_equal(got,np.concatenate((a,b),axis=3)), ('concat',shape)
        result['concat'].append({'nhwc':list(shape),'source_channels':[c0,c1],'bit_exact':True})
    kernel.reset()
    a=np.linspace(-8,8,4096,dtype='float32')
    source=kernel.allocate(a.nbytes,a.tobytes());output=kernel.allocate(a.nbytes)
    worker=kernel.allocate(0x20)
    kernel.word(worker+8,source,True);kernel.word(worker+0x10,output,True)
    span=kernel.allocate(8,struct.pack('<II',0,len(a)))
    kernel.run(0x89254,worker,span)
    got=kernel.floats(output,a.shape)
    reference=np.tanh(a.astype('float64'))
    assert np.isfinite(got).all() and np.max(np.abs(got-reference))<0.008
    result['tanh']={'samples':len(a),'range':[-8,8],'native_implementation':'polynomial exp + ARM64 FRECPE reciprocal estimate, scalar expf tail','high_precision_max_absolute_difference':float(np.max(np.abs(got-reference))),'high_precision_rmse':float(np.sqrt(np.mean((got-reference)**2))),'native_min':float(got.min()),'native_max':float(got.max()),'not_bit_equal_to_standard_tanh':True}
    result['status']='PASS_SYNTHETIC_NATIVE_KERNEL_REPLAY'
    return result


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('library',type=Path)
    parser.add_argument('--output',type=Path)
    args=parser.parse_args()
    value=verify(args.library)
    encoded=json.dumps(value,indent=2,allow_nan=False)+'\n'
    if args.output: args.output.write_text(encoded)
    else: print(encoded,end='')
