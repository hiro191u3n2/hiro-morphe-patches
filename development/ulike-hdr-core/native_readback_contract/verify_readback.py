#!/usr/bin/env python3
"""Pinned read-only evidence audit. No native call, ABI shim, raw pointer or asset output."""
import argparse, hashlib, json, zipfile
from pathlib import Path

EFFECT='d40af10b250b91cf8f30f4a265ac1d3b7b5b88a82bbf63da332c3f47a415d48e'
ARCHIVES={'natural':'5b50343dcedc7e9aadd218626e621ad683cd67372d225024f118b47f493546f0','purity':'cd5da20fefe1f9bd594e4e0d055d6320392fa54fb5df9d34b8f159c791c5b117'}
INSTRUCTIONS={
 0x4aa28c:('cmp','w0, #1'),0x4aa298:('cmp','w0, #2'),
 0x4aa2d4:('ldr','x3, [x20, #0x978]'),0x4aa2e4:('bl','#0x4aa35c'),
 0x4aa2ec:('ldr','x2, [x8, #0xcd0]'),0x4aa2f4:('bl','#0x524104'),
 0xaf5804:('add','x0, x0, #0xa1d'),0xaf5808:('add','x1, x1, #0xa27'),
 0xaf5848:('adrp','x8, #0xaf7000'),0xaf584c:('add','x8, x8, #0xffc'),
 0xaf8058:('lsl','w8, w8, #2'),0xaf80a0:('bl','#0x347fd0'),0xaf80b4:('mov','w27, #0x437f0000'),
 0xaf80cc:('ldrh','w9, [x8]'),0xaf80d0:('ldrh','w8, [x8, #2]'),
 0xaf80f4:('ldr','b1, [x8]'),0xaf8100:('ldr','b4, [x8, #3]'),
 0xaf8114:('fdiv','s1, s1, s0'),0xaf8120:('fdiv','s0, s4, s0'),
 0xa40c20:('add','x0, x0, #0xca6'),0xa40c30:('add','x1, x1, #0x968'),
 0x86021c:('ldr','x0, [x0, #0xe8]'),
 0xb12c2c:('add','x1, x1, #0xf6c'),0xb14a48:('add','x20, x20, #0xf6c'),
 0xbd5048:('add','x1, x1, #0x949')}
def sha(b):return hashlib.sha256(b).hexdigest()
def verify(effect,archives):
 from elftools.elf.elffile import ELFFile
 from capstone import Cs,CS_ARCH_ARM64,CS_MODE_ARM
 data=effect.read_bytes()
 if sha(data)!=EFFECT:raise ValueError('unsupported effect library')
 with effect.open('rb') as f:
  elf=ELFFile(f);segments=list(elf.iter_segments());rel={r['r_offset']:r['r_addend'] for r in elf.get_section_by_name('.rela.dyn').iter_relocations() if r['r_info_type']==1027}
 def offset(addr):
  for s in segments:
   if s['p_type']=='PT_LOAD' and s['p_vaddr']<=addr<s['p_vaddr']+s['p_filesz']:return addr-s['p_vaddr']+s['p_offset']
  raise ValueError('not file-backed')
 def string(addr):
  p=offset(addr);return data[p:data.index(b'\0',p)].decode('ascii')
 expected_relocations={0x1982a80:0x1194246,0x1982a88:0x4aa278,0x1988620:0x19afcd0,0x1988610:0x11a0eed,0x1988668:0x11a0ef5,0x1988678:0x11a0f00,0x1988688:0x11a0f09}
 for a,v in expected_relocations.items():
  if rel.get(a)!=v:raise ValueError('registration relocation changed')
 expected_strings={0x1194246:'getTexture',0x11a0eed:'Texture',0x11a0ef5:'addUVFrame',0x11a0f00:'getWidth',0x11a0f09:'getHeight',0x1269a1d:'getPixels',0x1269a27:'positions',0x125bca6:'getVertexArray',0x126bf6c:'onLateUpdate',0x127a949:'onEndFrame'}
 for a,v in expected_strings.items():
  if string(a)!=v:raise ValueError('registration string changed')
 if b'_p_BRC__Texture\0Texture\0' not in data:raise ValueError('BRC texture descriptor missing')
 md=Cs(CS_ARCH_ARM64,CS_MODE_ARM)
 for a,expected in INSTRUCTIONS.items():
  i=list(md.disasm(data[offset(a):offset(a)+4],a))
  if len(i)!=1 or (i[0].mnemonic,i[0].op_str)!=expected:raise ValueError('instruction mismatch at '+hex(a))
 assets={}
 for style,path in archives.items():
  if sha(path.read_bytes())!=ARCHIVES[style]:raise ValueError('unsupported archive')
  with zipfile.ZipFile(path) as z:
   prefix='materials/016/';skin='AmazingFeature2' if style=='natural' else 'AmazingFeature8'
   config=json.loads(z.read(prefix+'config.json'));links=config['effect']['Link'];order=[x['path'].rstrip('/') for x in sorted(links,key=lambda x:x['zorder'])]
   if style=='natural' and order[-1:]!=[skin]:raise ValueError('skin is not final')
   if style=='purity' and order[-2:]!=[skin,'AmazingFeature9']:raise ValueError('unexpected post-skin graph')
   names=['config.json',skin+'/xshader/skinseg.frag',skin+'/xshader/skinseg.vert',skin+'/material/SkinSeg.material',skin+'/main.scene']
   fragment=z.read(prefix+names[1]).decode();material=z.read(prefix+names[3]);
   if 'vec2(texcoord1.x, 1.0 - texcoord1.y)' not in fragment or 'texture2D(maskTexture, maskCoord).a' not in fragment:raise ValueError('mask sampling changed')
   if b'maskTexture' not in material or b'share://skinsegmask.texture' not in material:raise ValueError('shared native skin binding absent')
   if style=='purity':
    names+=['AmazingFeature9/xshader/pass0.frag','AmazingFeature9/xshader/pass0.vert','AmazingFeature9/material/pass0.material']
    final=z.read(prefix+names[-3]).decode()
    if 'texture2D(inputImageTexture,uv0)' not in final:raise ValueError('final input sampler changed')
   assets[style]={'archive_sha256':ARCHIVES[style],'ordered_effect_features':order,'original_file_sha256':{name:sha(z.read(prefix+name)) for name in names}}
 return {'status':'PASS_PINNED_READ_ONLY_NATIVE_AND_MATERIAL_EVIDENCE','effect_sha256':EFFECT,'instruction_checks':len(INSTRUCTIONS),'registration_relocations':len(expected_relocations),'registration_strings':len(expected_strings),'assets':assets,
  'established':{'bef_segmask_getTexture_returns':'BRC::Texture userdata, not AmazingEngine::RenderTexture','brc_exposed_methods':['addUVFrame','getWidth','getHeight'],'amazing_rendertexture_getPixels_native_read':'width*height*4-byte buffer; unsigned-short coordinate pairs; four 8-bit channels divided by 255 (channel layout not independently calibrated)','mesh_getVertexArray_registration_exists':True,'late_update_and_end_frame_named_dispatch_exist':True,'diagnostic_final_shader_sampling_route_supported_by_pinned_graph':True},
  'not_established':{'safe_BRC_to_Amazing_texture_bridge':False,'direct_native_skin_mask_cpu_readback_executed':False,'lua_late_update_after_final_2d_makeup_geometry':False,'mesh_accessor_returns_final_per_face_positions_and_uvs':False,'renderpicture_mask_orientation_and_channel_transfer_calibrated':False,'mask_matches_full_capture_resolution':False,'all_faces_and_empty_scene_geometry_supported':False,'android_device_execution':False},
  'policy':'No guessed ABI or raw address invocation. Native addresses identify read-only instruction evidence only.'}
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 p.add_argument('--effect',type=Path,required=True);p.add_argument('--natural-zip',type=Path,required=True);p.add_argument('--purity-zip',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
 result=verify(a.effect,{'natural':a.natural_zip,'purity':a.purity_zip});result['verifier_sha256']=sha(Path(__file__).read_bytes());a.output.write_text(json.dumps(result,indent=2)+'\n');print('PASS',result['instruction_checks'],'instruction anchors;',result['registration_relocations'],'relocations;',result['registration_strings'],'strings')
