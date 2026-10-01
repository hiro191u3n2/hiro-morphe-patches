#!/usr/bin/env python3
"""Static exact-binary evidence. No native address is invoked and no vendor asset is output."""
import argparse,hashlib,json,zipfile
from pathlib import Path
EFFECT='d40af10b250b91cf8f30f4a265ac1d3b7b5b88a82bbf63da332c3f47a415d48e'
ANCHORS={
0x85fb5c:('add','x0, x0, #0xabc'),0x85fb60:('add','x1, x1, #0xbc'),0x8600bc:('ldr','x0, [x0, #0x90]'),
0x861b5c:('str','x0, [x19, #0xe8]'),0x86021c:('ldr','x0, [x0, #0xe8]'),
0x862840:('ldr','x0, [x19, #0x90]'),0x862844:('bl','#0xa41768'),0x86287c:('mov','x0, sp'),0x862880:('mov','w4, #2'),0x86288c:('mov','w3, wzr'),0x862890:('bl','#0x856a18'),
0x856b44:('ldr','w16, [x15, w14, sxtw #2]'),0x856b50:('str','w16, [x8, w17, sxtw #2]'),
0x845be4:('mov','w3, w27'),0x845be8:('blr','x8'),0x845bd8:('ldr','x8, [x8, #0xd8]'),
0x862b38:('ldr','x20, [x0, #0x90]'),0x862b70:('b','#0xa461ac'),
0xe72590:('ldr','x8, [x22]'),0xe7259c:('ldr','x8, [x8, #0xa0]'),0xe725a0:('br','x8'),
0xac8d50:('bl','#0xe72590'),0xac8d5c:('b','#0xac8c9c'),0xac8d6c:('mov','w1, #0x44e'),0xac8d74:('bl','#0xa766a0'),
0xb13e80:('add','x2, x2, #0xa08'),0xb13e84:('mov','w1, #0x44e'),0xb13e88:('bl','#0xe4bcb8'),
0xb14a48:('add','x20, x20, #0xf6c'),0xb14b10:('bl','#0xb12be4'),0xb12c2c:('add','x1, x1, #0xf6c'),
0xa411d0:('add','x0, x0, #0xd71'),0xa41254:('add','x8, x8, #0x64c'),
0xa43670:('mov','w20, w3'),0xa43674:('mov','w19, w2'),0xa43678:('mov','w23, w1'),
0xa43698:('bl','#0xe5bf98'),0xa4369c:('bl','#0xa442f0'),0xe5bf9c:('mov','w1, w23'),
0xa43738:('bl','#0xe4c704'),0xa4373c:('bl','#0xe7a1e8'),0xa43740:('b.gt','#0xa4374c'),0xa43748:('sub','w8, w0, w19'),0xa4374c:('add','w1, w8, w8, lsl #1'),0xa437a8:('ldr','w16, [x14, #8]'),
0xa43854:('bl','#0xe4c704'),0xa43858:('bl','#0xe7a1e8'),0xe7a1e8:('sub','w8, w0, w19'),0xe7a1ec:('cmp','w8, w20'),0xe7a1f0:('mov','w8, w20'),
0xa4385c:('b.gt','#0xa43868'),0xa43864:('sub','w8, w0, w19'),0xa43868:('lsl','w1, w8, #1'),0xa4386c:('bl','#0xe7ea74'),
0xa43878:('bl','#0x837c00'),0xa438c4:('str','w16, [x15]'),0xa438c8:('ldr','w16, [x14, #4]'),0xa438d0:('str','w16, [x15, #4]'),
0xa3f060:('add','x1, x1, #0x982'),0xa3f064:('mov','w2, #0xd'),
0x48f074:('ldrb','w1, [x8, #0x279]'),0x48f14c:('bl','#0x677470'),0x677478:('ldr','x0, [x0, #0x130]'),0x677488:('ldr','x8, [x0]'),0x67748c:('ldr','x1, [x8, #0x88]'),0x677498:('mov','x0, xzr')}
STRINGS={0x1210abc:'templateMesh',0x122e28d:'makeupEntity',0x117cf3b:'FaceMakeupV2System',0x126bf6c:'onLateUpdate',0x125bd71:'getAttributeData',0x125b982:'TEXCOORD7',0x1188789:'getUseAmazing',0x1183a32:'getAMGScene'}
RELOCATIONS={0x18ddfc8:0x845684,0x18e0928:0x8627e8,0x19810a8:0x1188789,0x19810b0:0x48f008,0x19810b8:0x1183a32,0x19810c0:0x48f0e0}
def sha(b):return hashlib.sha256(b).hexdigest()
def verify(path):
 from elftools.elf.elffile import ELFFile
 from capstone import Cs,CS_ARCH_ARM64,CS_MODE_ARM
 b=path.read_bytes()
 if sha(b)!=EFFECT:raise ValueError('unsupported effect library')
 with path.open('rb') as f:
  e=ELFFile(f);segments=list(e.iter_segments());rel={r['r_offset']:r['r_addend'] for r in e.get_section_by_name('.rela.dyn').iter_relocations() if r['r_info_type']==1027}
 def offset(a):
  for s in segments:
   if s['p_type']=='PT_LOAD' and s['p_vaddr']<=a<s['p_vaddr']+s['p_filesz']:return a-s['p_vaddr']+s['p_offset']
  raise ValueError('address not file-backed')
 md=Cs(CS_ARCH_ARM64,CS_MODE_ARM)
 for a,want in ANCHORS.items():
  ins=list(md.disasm(b[offset(a):offset(a)+4],a))
  if len(ins)!=1 or (ins[0].mnemonic,ins[0].op_str)!=want:raise ValueError('instruction mismatch '+hex(a))
 for a,s in STRINGS.items():
  q=offset(a)
  if b[q:b.index(b'\0',q)].decode()!=s:raise ValueError('string mismatch')
 for a,v in RELOCATIONS.items():
  if rel.get(a)!=v:raise ValueError('vtable/registration mismatch')
 if b[offset(0x13278bc):offset(0x13278bc)+4]!=bytes.fromhex('00611a3c'):raise ValueError('attribute width branch table')
 return {'status':'PASS_PINNED_STATIC_GEOMETRY_PATH','effect_sha256':EFFECT,'instruction_checks':len(ANCHORS),'string_checks':len(STRINGS),'relocation_checks':len(RELOCATIONS),
  'facts':{'effect_component_template_mesh_getter_offset':'0x90','native_same_field_updated':True,'scene_system_updates_precede_lua_late_update_event':'0x44e','getter':'Mesh.getAttributeData(semantic, start, count)','position_attribute_for_selected_2d_meshes':'TEXCOORD7=13/two floats except Purity eyelash POSITION=0/three floats','count_bound':'positive count clamped to min(mesh vertex count minus start, count); observer always uses start=0','face_routing':'component update may compact active faces; faceIds is declared eligibility, not verified final draw range','legacy_v2_getAMGScene':'registered; may return null, do not force setUseAmazing'},
  'not_proven':{'picture_callback_after_late_update':False,'same_still_algorithm_attribution':False,'all_active_face_ranges':False,'no_future_mesh_overwrite':False,'exact_render_matrices_uvs_and_draw_state':False,'legacy_v2_per_vertex_opacity':False,'android_device_execution':False}}
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--effect',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=verify(a.effect);out['verifier_sha256']=sha(Path(__file__).read_bytes());a.output.write_text(json.dumps(out,indent=2)+'\n');print(out['status'],out['instruction_checks'])
