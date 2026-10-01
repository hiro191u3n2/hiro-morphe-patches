"""Check actual stock DEX API signatures and bounded native lifecycle evidence.

Requires androguard, pyelftools and capstone. Reads caller-owned stock files.
Never writes native binaries, decompiled classes, models or loader material.
"""
import argparse, hashlib, json, struct
from pathlib import Path

APIS = {
 'Lcom/ss/android/vesdk/runtime/VERuntime;': [
  ('getInstance','()Lcom/ss/android/vesdk/runtime/VERuntime;'),
  ('getContext','()Landroid/content/Context;'),('getEnv','()Lcom/ss/android/vesdk/runtime/VEEnv;')],
 'Lcom/ss/android/vesdk/runtime/VEEnv;': [('getDetectModelsDir','()Ljava/lang/String;')],
 'Lcom/ss/android/medialib/RecordInvoker;': [
  ('<init>','()V'),('setNativeInitListener2','(Lcom/ss/android/medialib/listener/NativeInitListener;)V'),
  ('initBeautyPlay','(IILjava/lang/String;IILjava/lang/String;I)I'),
  ('setEffectBuildChainType','(I)V'),('setDetectionMode','(Z)V'),('forceFirstFrameHasEffect','(Z)V'),
  ('setCaptureRenderWidth','(II)V'),('initFaceDetectExtParam','(IZZ)V'),
  ('startPlay','(IILjava/lang/String;II)I'),
  ('registerFaceResultCallback','(ZLcom/ss/android/medialib/RecordInvoker$FaceResultCallback;)V'),
  ('renderPicture','(Lcom/ss/android/medialib/camera/ImageFrame;IILcom/ss/android/medialib/RecordInvoker$OnPictureCallbackV2;)I'),
  ('stopPlay','()I'),('unRegisterFaceResultCallback','()V'),('uninitBeautyPlay','()I')],
 'Lcom/ss/android/medialib/camera/ImageFrame;': [('<init>','(Landroid/graphics/Bitmap;I)V')],
 'Lcom/ss/android/medialib/listener/NativeInitListener;': [
  ('onNativeInitCallBack','(I)V'),('onNativeInitHardEncoderRetCallback','(II)V')],
 'Lcom/ss/android/medialib/RecordInvoker$FaceResultCallback;': [
  ('onResult','(Lcom/ss/android/medialib/model/FaceAttributeInfo;Lcom/ss/android/medialib/model/FaceDetectInfo;)V')],
 'Lcom/ss/android/medialib/RecordInvoker$OnPictureCallbackV2;': [
  ('onImage','([III)V'),('onResult','(II)V')],
 'Lcom/ss/android/medialib/model/FaceDetectInfo;': [('getInfo','()[Lcom/ss/android/medialib/model/FaceDetect;')],
 'Lcom/ss/android/medialib/model/FaceDetect;': [('getPoints','()[Landroid/graphics/PointF;'),('getScore','()F')]
}
NATIVE = {
 0x3fdbc8: ('mov','w25, w3'),
 0x3fdbe4: ('mov','w24, w4'),
 0x5b50e4: ('mov','w21, w6'),
 0x5b50e8: ('mov','w22, w5'),
 0x3fdc10: ('str','w25, [x23, #0x448]'),
 0x3fdc14: ('str','w24, [x23, #0x44c]'),
 0x5ad708: ('mov','x0, x23'),
 0x5ad70c: ('mov','x1, xzr'),
 0x5b132c: ('mov','w2, w22'),
 0x5b1330: ('mov','w3, w21'),
 0x3fdc20: ('bl','#0x41cec4'),
 0x41ced8: ('mov','w23, w2'),
 0x41cee8: ('mov','w21, w3'),
 0x41cf20: ('mov','w10, #0x168'),
 0x41cf24: ('sdiv','w11, w23, w10'),
 0x41cf38: ('msub','w4, w11, w10, w23'),
 0x41cf64: ('str','w4, [x19, #0x594]'),
 0x41cf6c: ('str','w21, [x19, #0x360]'),
 0x4206b8: ('ldr','w4, [x19, #0x594]'),
 0x4206bc: ('ldr','w5, [x19, #0x360]'),
 0x4206c8: ('bl','#0x5bb1b8'),
 0x40d420: ('bl','#0x42bbe8'),
 0x42bcfc: ('bl','#0x432784'),
 0x4327b8: ('strb','w9, [x19, #0x10]'),
 0x4327d4: ('ldr','x0, [x19, #0x18]'),
 0x4327e4: ('b','#0x1b8d78'),
 0x3fe240: ('ldr','x0, [x19, #0x9c8]'),
 0x3fe244: ('bl','#0x59ab54'),
 0x59ab58: ('b','#0x1b8d78'),
 0x3fe2a0: ('bl','#0x432784'),
}
LIB_SHA='67f1b97b92859630ae200cafad41f7e3db3d8d2fbcac3ffd5c7bab0683471095'
DEX_PINS={
 'classes.dex':'4fb5923b8cf7c28f393ba91dca6a9d4be91f69a8d2a4ae067670987e642b0f83',
 'classes2.dex':'9c71360debb8d11fed8403bfca3b0247198d99218bd02d129d7205aa39df6933',
 'classes3.dex':'7d63365d601600f64f6552d6c84f375db36279d95785d27f49157d4d1e4247e0',
 'classes4.dex':'2ffe409e7b98e36cd2f3d84fe9a7c7146c1e84a3b084961fe089c044229c5392'}
def require(value, message):
 if not value: raise ValueError(message)
def verify(dex_directory, library):
 from loguru import logger
 logger.disable('androguard')
 from androguard.core.dex import DEX
 from elftools.elf.elffile import ELFFile
 from capstone import Cs,CS_ARCH_ARM64,CS_MODE_ARM
 found={}; dex_pins={}
 for path in sorted(dex_directory.glob('classes*.dex')):
  data=path.read_bytes(); dex_pins[path.name]=hashlib.sha256(data).hexdigest()
  require(dex_pins[path.name]==DEX_PINS.get(path.name),'Unsupported stock DEX')
  dex=DEX(data)
  for cls in dex.get_classes():
   if cls.get_name() not in APIS: continue
   for method in cls.get_methods():
    key=(method.get_name(),method.get_descriptor().replace(' ',''))
    if key in APIS[cls.get_name()]:
     require(method.get_access_flags() & 1, 'SDK API is not public')
     full=cls.get_name()+'->'+key[0]+key[1]
     require(full not in found,'Ambiguous class definition');found[full]=path.name
  del dex,data
 expected=sum(len(v) for v in APIS.values())
 require(dex_pins==DEX_PINS,'Incomplete stock DEX set')
 require(len(found)==expected, f'Expected {expected} public SDK signatures, found {len(found)}')
 data=library.read_bytes(); require(hashlib.sha256(data).hexdigest()==LIB_SHA,'Unsupported SDK binary')
 with library.open('rb') as stream:
  elf=ELFFile(stream); loads=[s for s in elf.iter_segments() if s['p_type']=='PT_LOAD']
  def offset(addr):
   for s in loads:
    if s['p_vaddr']<=addr<s['p_vaddr']+s['p_filesz']:return addr-s['p_vaddr']+s['p_offset']
   raise ValueError('Bad address')
  md=Cs(CS_ARCH_ARM64,CS_MODE_ARM)
  for addr, expected_op in NATIVE.items():
   instruction=list(md.disasm(data[offset(addr):offset(addr)+4],addr))
   require(len(instruction)==1 and (instruction[0].mnemonic,instruction[0].op_str)==expected_op,'Native contract mismatch '+hex(addr))
  plt=elf.get_section_by_name('.plt'); symbols=elf.get_section_by_name('.dynsym')
  imports={plt['sh_addr']+32+16*i:symbols.get_symbol(rel['r_info_sym']).name
   for i,rel in enumerate(elf.get_section_by_name('.rela.plt').iter_relocations())}
  require(imports[0x1b8d78]=='pthread_join','Worker join import differs')
 return {'status':'PASS_STATIC_STOCK_API_AND_LIFECYCLE_CONTRACT',
  'public_sdk_signatures_checked':len(found),'native_instructions_checked':len(NATIVE),
  'library_sha256':LIB_SHA,'dex_sha256':dex_pins,'sdk_methods':found,
  'findings':[
   'Explicit RecordInvoker.startPlay(width,height,device,0,0) writes zero rotation/mirror fields consumed by still processor.',
   'Unregister disables callback worker then pthread_join; stopPlay also joins render thread and callback worker.',
   'The high-level null-view wrapper supplies -1 geometry/rotation/mirror; the probe does not use that wrapper.',
   'Stock onNativeCallback_Init sets mIsRenderReady true even after negative callback; new ledger checks actual status instead.',
   'Stock uninit clears static listeners; app-wide idle lease is a mandatory integration precondition, not supplied here.'
  ],'native_executed':False,'landmark_coordinate_contract_verified':False,
  'runtime_backend_selection_verified':False,'normal_license_and_model_initialization_verified_on_device':False}
if __name__=='__main__':
 parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('dex_directory',type=Path);parser.add_argument('library',type=Path)
 args=parser.parse_args();print(json.dumps(verify(args.dex_directory,args.library),indent=2))
