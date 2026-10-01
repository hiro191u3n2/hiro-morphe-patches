#!/usr/bin/env python3
"""Bounded, read-only native checks for Purity crop construction and call direction."""
if not __debug__:
    raise RuntimeError("Validation requires assertions; run without -O or -OO")

import argparse,hashlib,io,json
from pathlib import Path
from capstone import Cs,CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN
from elftools.elf.elffile import ELFFile
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
 ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('library',type=Path);ap.add_argument('--output',type=Path);a=ap.parse_args();b=pinned_library(a.library);md=Cs(CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN);checks=[]
 expected=[
 (0xc9ef44,'adrp','x1, #0x1294000'),(0xc9ef48,'add','x1, x1, #0xe35'),
 (0xc9f004,'cmp','x25, #0x350'),(0xc9f00c,'ldr','x8, [x24, x25]'),(0xc9f018,'bl','#0xcb1c74'),
 (0xca0170,'ldr','x21, [sp, #0x6b90]'),(0xca02a8,'ldp','x1, x2, [x26, #0xd0]'),
 (0xca01cc,'ldr','s8, [x26, #0x110]'),(0xca01c8,'ldr','w21, [x26, #0x114]'),(0xca01c0,'ldr','w8, [x26, #0x118]'),
 (0xca01d4,'ldr','w8, [x26, #0x11c]'),(0xca01e4,'ldr','w8, [x26, #0x120]'),
 (0xca0314,'bl','#0xe4a12c'),(0xca0320,'bl','#0xe771dc'),(0xca0324,'fneg','s2, s1'),
 (0xca0350,'str','s0, [x28, #0x1c0]'),(0xca0354,'str','s0, [x28, #0x1d0]'),
 (0xca03d0,'fmadd','s13, s8, s10, s9'),(0xca044c,'fdiv','s1, s12, s13'),
 (0xca0464,'str','s1, [x28, #0x1d8]'),(0xca0468,'fmul','s1, s1, s8'),
 (0xca0470,'str','s1, [x28, #0x1e0]'),(0xca0474,'fdiv','s1, s11, s3'),(0xca0498,'str','s1, [x28, #0x1e8]'),
 (0xca049c,'str','s0, [x28, #0x1ec]'),(0xca04ac,'bl','#0xe639f4'),
 (0xe639fc,'add','x1, sp, #6, lsl #12'),(0xe63a04,'add','x0, x0, #0xea8'),(0xe63a08,'add','x1, x1, #0xf08'),(0xe63a0c,'b','#0xf539d4'),
 (0xf52fa0,'adrp','x1, #0x19f9000'),(0xf52fa4,'add','x1, x1, #0x690'),
 (0x393ba0,'add','x8, x8, #0x66c'),(0x393bd4,'add','x9, x9, #0xce0'),(0x393bd8,'stur','x9, [x8, #0x24]'),
 (0xca05bc,'str','s8, [x28, #0x1e0]'),(0xca05c4,'str','s1, [x24, #0x14]'),(0xca05d4,'bl','#0xe639f4'),
 (0xca06b8,'mov','w4, #1'),(0xca06bc,'bl','#0xe4c578'),(0xe4c578,'mov','w5, wzr'),(0xe4c57c,'b','#0xfe6174')]
 for address,mn,op in expected:
  i=list(md.disasm(b[address:address+4],address))[0];assert (i.mnemonic,i.op_str)==(mn,op),(hex(address),i.mnemonic,i.op_str);checks.append({'address':hex(address),'instruction':mn+' '+op})
 assert b[0x1294e35:0x1294e35+11]==b'face_point\0'
 elf=ELFFile(io.BytesIO(b));rel={r.entry.r_offset:r.entry for r in elf.get_section_by_name('.rela.dyn').iter_relocations()}
 assert rel[0x1965db8+8].r_addend==0x1347b54
 assert b[0x1347b54:].split(b'\0',1)[0]==b'N9mobilecv210MatOp_GEMME'
 assert rel[0x1965cf8].r_addend==0xf553d8
 result={'status':'PASS_STATIC_PURITY_AFFINE_CONTRACT','library_sha256':PIN,'source_sha256':hashlib.sha256(bounded_read(Path(__file__))).hexdigest(),
  'instruction_checks':checks,
  'template':{'group':'face_point','points':106,'stored_at_state_offset':'0xd0','read_key_address':'0x1294e35','load_loop':'0xc9f004..0xc9f01c','bytes':848},
  'parameters':{'margin':'state+0x110, model CropMarginv0=float32(0.2)','width':'state+0x114, model CropWidthv0=256','height':'state+0x118, model CropHeightv0=256','offset_x':'state+0x11c, model CropOffsetXv0=0','offset_y':'state+0x120, model CropOffsetYv0=0'},
  'source_selection':'0xca0170 uses the first source face-point pointer assembled at stack+0x6b90, then copies exactly the template vector length into an input vector at stack+0x88. Coordinate origin and live detector ordering require caller-side validation.',
  'similarity':{'source':'runtime face landmarks copied at 0xca0184..0xca01b0','target':'model template state+0xd0','direction':'source -> normalized model template','matrix':'T = [[a,-b,tx],[b,a,ty],[0,0,1]]','math':'Center both sets; D=sum(sx^2+sy^2); a=sum(sx*dx+sy*dy)/D; b=sum(sx*dy-sy*dx)/D; t=target_mean-linear*source_mean.','evidence':'0xca01ec..0xca0370, centered arithmetic helper 0xe4a12c, divisors 0xe771dc. Native bounded arithmetic separately executes successfully on six synthetic 106-point cases.'},
  'crop_matrix':{'order':'M = O @ S @ T','general_scaling':'k=1+2*m; sx=W/k; vertical_margin=((H/W)*k-1)/2; sy=H/(1+2*vertical_margin); S=[[sx,0,sx*m],[0,sy,sy*vertical_margin],[0,0,1]]; O=[[1,0,offset_x],[0,1,offset_y],[0,0,1]].','pinned_square_case':'W=H=256,m=float32(0.2),offsets=0, so sx=sy=256/(1+2*m) and translations=sx*m.','matrix_operation_proof':'0xca04ac and0xca05d4 supply left matrix S/O and right previous transform to0xf539d4. It builds flags=0 MatOp_GEMM using global object0x19f9690. Initializer0x393bd8 sets its vtable0x1965ce0; ELF RTTI names MatOp_GEMM; slot+0x18=0xf553d8 calls GEMM0xf95f64 with operandA thenB.','precision':'Original scalar construction is float32 with fused arithmetic; a float64 implementation preserves the mathematical contract without claiming byte identity.'},
  'warp_call':{'entry':'0xfe6174','caller':'0xca0690..0xca06bc through0xe4c578','input':'source image Mat stack+0x6ca0','output':'state+0x128','matrix':'M at stack+0x6c40','size':'W,H at state+0x114/+0x118','flags':1,'border_mode':0,'inverse_map_bit_set':False,'note':'The parent generic warp audit owns sampling, internal inversion and borders; this report proves the call arguments and constructed forward direction.'},
  'limits':['Native arithmetic test is scoped to centered similarity math only; no complete crop/warp pixel parity claimed.','Live detector coordinates, pose handling, source colour/orientation, image readback and downstream full-style ordering remain outside this proof.','No original model, coordinate table, keys or library bytes are embedded.']}
 if a.output:a.output.write_text(json.dumps(result,indent=2)+'\n')
 print('PASS',len(checks),'instruction checks; template key and MatOp_GEMM RTTI/vtable links')
if __name__=='__main__':main()
