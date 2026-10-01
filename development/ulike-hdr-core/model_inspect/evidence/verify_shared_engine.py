"""Read-only bounded static proof; no model, key, or weights in output."""
from pathlib import Path
import argparse,hashlib,json,io
from capstone import Cs,CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN
from elftools.elf.elffile import ELFFile
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('library',type=Path)
parser.add_argument('--output',type=Path)
args=parser.parse_args()
with args.library.open('rb') as source:
 lib=source.read(64*1024*1024+1)
if len(lib)>64*1024*1024: raise ValueError('library size bound')
if not __debug__: raise RuntimeError('verification assertions must be enabled')
assert hashlib.sha256(lib).hexdigest()=='cedda347b55c03de82cc22b7f003777f40750d4f341494a29587d7b821ad7853'
e=ELFFile(io.BytesIO(lib));dyn=e.get_section_by_name('.dynsym');pl=e.get_section_by_name('.plt')['sh_addr']
imports={pl+32+16*n:dyn.get_symbol(r.entry.r_info_sym).name for n,r in enumerate(e.get_section_by_name('.rela.plt').iter_relocations())}
rel={r.entry.r_offset:r.entry for r in e.get_section_by_name('.rela.dyn').iter_relocations()}
md=Cs(CS_ARCH_ARM64,CS_MODE_LITTLE_ENDIAN)
checks=[]
def exact(addr,mn,op):
 i=list(md.disasm(lib[addr:addr+4],addr))[0]
 assert (i.mnemonic,i.op_str)==(mn,op),(hex(addr),i.mnemonic,i.op_str)
 checks.append({'address':hex(addr),'instruction':mn+' '+op})
def imp(addr,target):
 i=list(md.disasm(lib[addr:addr+4],addr))[0];assert i.mnemonic=='bl'
 name=imports[int(i.op_str[1:],16)];assert target in name
 checks.append({'address':hex(addr),'import':name})
imp(0x3bcc0,'ByteNNEngineImplC1Ev')
imp(0x3be10,'ByteNNEngineImpl15InitForEspresso')
exact(0x3bdbc,'str','wzr, [x21, #8]!')
exact(0x2550c,'ldr','w9, [x8], #0x80')
exact(0x25510,'str','w9, [x20, #0x40]!')
exact(0x256d8,'bl','#0x2af74')
exact(0x23ef8,'bl','#0x2af74')
exact(0x2b448,'bl','#0x3d320')
assert dyn.get_symbol(rel[0x231d60].r_info_sym).name=='_ZTVN6BYTENN10LabNetWorkE'
assert rel[0x229278].r_addend==0x3e3f4
exact(0x25738,'ldr','x8, [x8]')
exact(0x2573c,'blr','x8')
exact(0x3e718,'bl','#0x4b030')
exact(0x4b080,'bl','#0x4f2a8')
exact(0x4f3c0,'bl','#0x4f4c4')
exact(0x3e8c8,'bl','#0x4b0e0')
exact(0x4b1b0,'bl','#0x4f4c4')
imp(0x3c104,'ByteNNEngineImpl10GetNetworkEv')
exact(0x3c11c,'ldr','x8, [x8, #0x10]')
exact(0x3c120,'blr','x8')
result={'status':'PASS_STATIC_SHARED_ENGINE_CONTRACT','library_sha256':hashlib.sha256(lib).hexdigest(),
 'instruction_and_relocation_checks':checks,'relocations':{'GOT_0x231d60':'_ZTVN6BYTENN10LabNetWorkE','LabNetWork_init_slot_0x229278':'0x3e3f4'},
 'conclusions':[
 'Espresso::Thrustor is a compatibility wrapper implemented by the same pinned libbytenn library. It constructs ByteNNEngineImpl and delegates CreateNet to InitForEspresso(Config&).',
 'CreateNet sets Config forward type to zero. InitForEspresso copies it and uses the same network factory as ByteNN Init(ConfigExt). Factory zero path selects LabNetWork at 0x3d320; ELF relocation proves its initializer is 0x3e3f4.',
 'The legacy graph/weight route 0x3e718 -> 0x4b030 -> 0x4f2a8 -> 0x4f4c4 converges on the same graph build routine as BM route 0x3e8c8 -> 0x4b0e0 -> 0x4f4c4.',
 'Purity therefore uses the previously audited shared graph/operator and FP32 weight contracts: Conv OHWI, DW HWC, per-layer optional bias, flag-gated ReLU; same square-kernel limit applies.',
 'Inference delegates to ByteNN GetNetwork and its execution slot; this is not merely matching exported names.'
 ],'limitations':['Static link and parser proof only; no full native model output parity is claimed.','No private keys, decoded graph text, weights, or native library bytes are published.']}
if args.output: args.output.write_text(json.dumps(result,indent=2)+'\n')
print('PASS',len(checks),'static calls/instructions and 2 ELF relocations')
