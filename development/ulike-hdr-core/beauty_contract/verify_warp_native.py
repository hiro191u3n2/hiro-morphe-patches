#!/usr/bin/env python3
"""Bounded pinned ARM64 coordinate/border proof; no full image warp claim.

Requires caller-owned libeffect.so, pyelftools, capstone and unicorn.
No original library bytes, model data, templates or private keys are bundled.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import struct

from capstone import Cs, CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN
from elftools.elf.elffile import ELFFile
from unicorn import Uc, UC_ARCH_ARM64, UC_MODE_ARM
from unicorn import arm64_const as regs

PIN = 'd40af10b250b91cf8f30f4a265ac1d3b7b5b88a82bbf63da332c3f47a415d48e'
MEM, STOP = 0x3000000, 0x4000000


def verify(path):
    if not __debug__:
        raise RuntimeError('verification assertions must be enabled')
    with Path(path).open('rb') as source:
        raw = source.read(64*1024*1024+1)
    if len(raw)>64*1024*1024 or hashlib.sha256(raw).hexdigest()!=PIN:
        raise ValueError('unsupported original library')
    uc=Uc(UC_ARCH_ARM64,UC_MODE_ARM)
    uc.mem_map(0,0x1c00000)
    elf=ELFFile(io.BytesIO(raw))
    for segment in elf.iter_segments():
        if segment['p_type']=='PT_LOAD':
            uc.mem_write(segment['p_vaddr'],segment.data())
    uc.mem_map(MEM,0x200000)
    uc.mem_map(STOP,0x1000)
    uc.reg_write(regs.UC_ARM64_REG_TPIDR_EL0,MEM+0x100)
    uc.reg_write(regs.UC_ARM64_REG_SP,MEM+0x1ff000)
    uc.reg_write(regs.UC_ARM64_REG_FPCR,0)  # nearest, ties to even
    md=Cs(CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN)
    assertions=[]
    def exact(address,mnemonic,operands):
        ins=list(md.disasm(raw[address:address+4],address))[0]
        assert (ins.mnemonic,ins.op_str)==(mnemonic,operands),(hex(address),ins.mnemonic,ins.op_str)
        assertions.append({'address':hex(address),'instruction':mnemonic+' '+operands})
    for a,m,o in [
        (0xcadf24,'ldr','d0, [x9, #0xcc0]'),
        (0xcadf50,'str','d0, [x20, #0x568]'),
        (0xcadf54,'str','wzr, [x20, #0x570]'),
        (0x97d0e8,'movi','v0.2d, #0000000000000000'),
        (0x97d0ec,'stp','q0, q0, [x19]'),
        (0xcba750,'ldp','w4, w5, [x21, #0xb4]'),
        (0xcba758,'bl','#0xfe6174'),
        (0xfe6280,'and','w9, w21, #7'),
        (0xfe6308,'tbnz','w21, #4, #0xfe6364'),
        (0xfe5f58,'mov','x10, #0x4090000000000000'),
        (0xfe5f68,'scvtf','d1, w9'),
        (0xfe5f78,'frintx','d0, d0'),
        (0xfe8028,'add','w15, w8, w22'),
        (0xfe8044,'fmadd','d0, d0, d4, d1'),
        (0xfe8058,'frintx','d0, d0'),
        (0xfe8068,'add','w15, w26, w15'),
        (0xfe8160,'asr','w5, w3, #0xa'),
        (0xfe816c,'bfxil','w6, w3, #5, #5'),
        (0xfe5b10,'mov','w9, #0x3d000000'),
        (0xfe5b2c,'fsub','s2, s0, s1'),
        (0xfe5c4c,'fmul','s1, s1, s0'),
        (0xf5cf70,'mov','w0, #-1'),
        (0xfe1150,'ldrb','w8, [sp, #0x108]'),
        (0xfe1184,'ldrb','w9, [sp, #0x108]'),
        (0xfe11ac,'ldrb','w10, [sp, #0x108]'),
        (0xfe11d0,'ldrb','w11, [sp, #0x108]'),
    ]: exact(a,m,o)
    defaults=struct.unpack_from('<II',raw,0x116ecc0)
    assert defaults==(0,1)
    assert raw[0x1347c64]==0
    def xr(n,value): uc.reg_write(getattr(regs,'UC_ARM64_REG_X'+str(n)),value & ((1<<64)-1))
    def wr(n,value): uc.reg_write(getattr(regs,'UC_ARM64_REG_W'+str(n)),value & 0xffffffff)
    def run(start,end):
        uc.emu_start(start,end,timeout=2_000_000,count=20000)
        if uc.reg_read(regs.UC_ARM64_REG_PC)!=end:
            raise RuntimeError('native slice failed to reach endpoint')
    borders=[]
    for length in (1,2,5,17):
        for point in (-100,-2,-1,0,1,length-1,length,length+1,100):
            wr(0,point);wr(1,length);wr(2,0);xr(30,STOP)
            run(0xf5cefc,STOP)
            result=uc.reg_read(regs.UC_ARM64_REG_W0)
            if result>=1<<31:result-=1<<32
            assert result==(point if 0<=point<length else -1)
            borders.append({'length':length,'index':point,'result':result})
    matrix_ptr,context,columns,xy,fract=MEM+0x1000,MEM+0x2000,MEM+0x3000,MEM+0x4000,MEM+0x4100
    uc.mem_write(context+0x100,struct.pack('<Q',matrix_ptr))
    matrices=[(1,0,0,0,1,0),(.5,0,.25,0,.5,-.25),(1,.125,-.5,-.25,1,.125),
              (1.00048828125,-.0625,.00048828125,.125,.75,-.00048828125)]
    cases=0
    for m in matrices:
        uc.mem_write(matrix_ptr,struct.pack('<6d',*m))
        for x in (0,1,3,7):
            xr(24,matrix_ptr);xr(9,x);xr(10,0x4090000000000000)
            xr(20,columns);xr(22,columns+0x100)
            run(0xfe5f64,0xfe5f98)
            ax=struct.unpack('<i',bytes(uc.mem_read(columns+x*4,4)))[0]
            ay=struct.unpack('<i',bytes(uc.mem_read(columns+0x100+x*4,4)))[0]
            assert ax==round(1024*m[0]*x) and ay==round(1024*m[3]*x)
            for y in (0,1,2,9):
                xr(20,context);xr(8,0);wr(22,y);wr(10,1);xr(28,xy)
                xr(27,0x4090000000000000);wr(26,16)
                run(0xfe8024,0xfe8078)
                x0=uc.reg_read(regs.UC_ARM64_REG_W15);y0=uc.reg_read(regs.UC_ARM64_REG_W16)
                if x0>=1<<31:x0-=1<<32
                if y0>=1<<31:y0-=1<<32
                assert x0==round(1024*(m[1]*y+m[2]))+16
                assert y0==round(1024*(m[4]*y+m[5]))+16
                wr(3,ax);wr(4,ay);wr(15,x0);wr(16,y0);wr(29,32767)
                wr(0,1);xr(14,xy);xr(30,fract)
                run(0xfe8158,0xfe81a0)
                ix,iy=struct.unpack('<hh',bytes(uc.mem_read(xy,4)))
                f=struct.unpack('<H',bytes(uc.mem_read(fract,2)))[0]
                assert ix==(ax+x0)>>10 and iy==(ay+y0)>>10
                assert f==((((ay+y0)>>5)&31)*32+(((ax+x0)>>5)&31))
                cases+=1
    return {'status':'PASS_PINNED_WARP_COORDINATE_BORDER_SLICES','library_sha256':PIN,
            'instruction_checks':assertions,'constructor_defaults':{'affine_mode':defaults[0],'warp_flags':defaults[1],'border_type':0,'border_scalar':[0,0,0,0]},
            'provided_asset_override_check':{'source':'Companion beauty contract audit of both selected materials/016 Lua, JSON and shader assets; source hashes in EVIDENCE.json','affine_mode_warp_flags_border_type_mentions':0,'actual_runtime_property_state_verified':False},
            'coordinate_cases':cases,'border_cases':len(borders),'border_results':borders,
            'coordinate_formula':{'inverse_matrix':'I = inverse(source_to_crop)','Ax':'round_even(1024*I00*x)','Ay':'round_even(1024*I10*x)','X0':'round_even(1024*(I01*y+I02))+16','Y0':'round_even(1024*(I11*y+I12))+16','integer_xy':'((Ax+X0)>>10,(Ay+Y0)>>10), saturating int16 storage','fraction_xy':'((((Ax+X0)>>5)&31)/32,(((Ay+Y0)>>5)&31)/32)'},
            'limits':['Only specified original ARM64 coordinate slices and constant-border indexing executed. No complete native warp, Android, face detector or GPU shader executed.','Synthetic coordinates remain inside int16 range; native saturation is shown statically and is outside these numerical cases.','CPU crop uses integer destination pixel centers and quantized 1/32 fractions. Continuous high-precision bilinear sampling is an intentional replacement, not bitwise original-image parity.','GPU projection NDC/UV half-pixel conventions are separate and not established by this crop proof.']}


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('library',type=Path);p.add_argument('--output',type=Path)
    args=p.parse_args();text=json.dumps(verify(args.library),indent=2,allow_nan=False)+'\n'
    if args.output:args.output.write_text(text)
    else:print(text,end='')
