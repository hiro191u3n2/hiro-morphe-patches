"""Check public SDK fields/methods and Lua message binding against caller-owned stock.
Writes evidence only; does not redistribute vendor code, models, or authorization data.
"""
import argparse, hashlib, importlib.util, json
from pathlib import Path

APIS={
 'Lcom/ss/android/medialib/RecordInvoker;': [('setMessageListenerV2','(Lcom/bef/effectsdk/message/MessageCenter$Listener;)V'),('getHandler','()J')],
 'Lcom/ss/android/vesdk/VERecorder;': [('startPreviewAsync','(Landroid/view/Surface;Lcom/ss/android/vesdk/VEListener$VECallListener;)V'),('stopPreviewAsync','(Lcom/ss/android/vesdk/VEListener$VECallListener;)V')],
 'Lcom/ss/android/vesdk/TECameraVideoRecorder;': [('getRecordStatus','()I'),('releaseInteralRecorder','()V')],
 'Lcom/ss/android/vesdk/VEListener$VECallListener;': [('onDone','(I)V')],
 'Lcom/bef/effectsdk/message/MessageCenter$Listener;': [('onMessageReceived','(IIILjava/lang/String;)V')],
 'Lcom/ss/android/medialib/model/FaceDetectInfo;': [('getInfo','()[Lcom/ss/android/medialib/model/FaceDetect;')],
 'Lcom/ss/android/medialib/model/FaceDetect;': [
  ('getPoints','()[Landroid/graphics/PointF;'),('getPointVisibility','()[F'),('getRect','()Landroid/graphics/Rect;'),
  ('getFaceID','()I'),('getTrackCount','()I'),('getAction','()I'),('getScore','()F'),('getEyeDistance','()F'),
  ('getYaw','()F'),('getPitch','()F'),('getRoll','()F'),('getFaceExtInfo','()Lcom/ss/android/medialib/model/FaceDetect$FaceExtInfo;')]
}
FIELDS={
 'Lcom/ss/android/vesdk/VERecorder;': {'b':'Lcom/ss/android/vesdk/TERecorderBase;'},
 'Lcom/ss/android/vesdk/TECameraVideoRecorder;': {'mRecordPresenter':'Lcom/ss/android/medialib/presenter/MediaRecordPresenter;','g1':'Landroid/view/Surface;'},
 'Lcom/ss/android/medialib/presenter/MediaRecordPresenter;': {'mfbInvoker':'Lcom/ss/android/medialib/RecordInvoker;'},
 'Lcom/ss/android/medialib/model/FaceDetect$FaceExtInfo;': {**dict.fromkeys(['eyeCount','eyebrowCount','irisCount','lipCount'],'I'),**dict.fromkeys(['eyeLeftPoints','eyeRightPoints','eyeBrowLeftPoints','eyeBrowRightPoints','irisLeftPoints','irisRightPoints','lipPoints'],'[Landroid/graphics/PointF;')},

}
# Lua MessageCenter wrappers enforce exactly four arguments: numeric 1..3, string4.
NATIVE={
 0x4faef4:('cmp','w0, #4'),0x4faf00:('cmp','w0, #5'),
 0x4faf2c:('mov','w1, #1'),0x4faf30:('bl','#0x523f98'),
 0x4faf38:('mov','w1, #2'),0x4faf3c:('bl','#0x523f98'),
 0x4faf44:('mov','w1, #3'),0x4faf48:('bl','#0x523f98'),
 0x4faf50:('mov','w1, #4'),0x4faf54:('bl','#0x524048'),
 0x4faf78:('bl','#0xf3b2c4'),
 0x4fb0bc:('bl','#0xf3b2fc'),
}
def verify(dex_dir,effect):
 from loguru import logger
 logger.disable('androguard')
 from androguard.core.dex import DEX
 from elftools.elf.elffile import ELFFile
 from capstone import Cs,CS_ARCH_ARM64,CS_MODE_ARM
 parent=Path(__file__).resolve().parent.parent/'android_face_backend/verify_stock_contract.py'
 spec=importlib.util.spec_from_file_location('stock_face_contract',parent);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
 methods={};fields={};pins={}
 for name in ['classes.dex','classes3.dex']:
  path=dex_dir/name;data=path.read_bytes();sha=hashlib.sha256(data).hexdigest()
  if sha!=module.DEX_PINS[name]:raise ValueError('Unsupported stock DEX')
  pins[name]=sha
  for cls in DEX(data).get_classes():
   cname=cls.get_name()
   if cname in APIS:
    for m in cls.get_methods():
     key=(m.get_name(),m.get_descriptor().replace(' ',''))
     if key in APIS[cname]:
      if not m.get_access_flags()&1:raise ValueError('Not public method')
      methods[cname+'->'+key[0]+key[1]]=True
   if cname in FIELDS:
    for f in cls.get_fields():
     if f.get_name() in FIELDS[cname]:
      if f.get_descriptor()!=FIELDS[cname][f.get_name()] or not f.get_access_flags()&1:raise ValueError('Field contract')
      fields[cname+'->'+f.get_name()+':'+f.get_descriptor()]=True
 if len(methods)!=sum(map(len,APIS.values())) or len(fields)!=sum(map(len,FIELDS.values())):raise ValueError('Missing public SDK member')
 data=effect.read_bytes();sha=hashlib.sha256(data).hexdigest()
 # Exact stock library pin is populated below; no acceptance of a merely similar library.
 if sha!=EFFECT_SHA:raise ValueError('Unsupported effect library')
 with effect.open('rb') as f:
  elf=ELFFile(f);segments=list(elf.iter_segments());rel={r['r_offset']:r['r_addend'] for r in elf.get_section_by_name('.rela.dyn').iter_relocations()}
  def offset(addr):
   for seg in segments:
    if seg['p_type']=='PT_LOAD' and seg['p_vaddr']<=addr<seg['p_vaddr']+seg['p_filesz']:return addr-seg['p_vaddr']+seg['p_offset']
   raise ValueError('Address outside file')
  def string(addr):
   p=offset(addr);return data[p:data.index(b'\0',p)].decode()
  for addr,target,name in [(0x19961f0,0x4faee0,'MessageCenter_sendMessage'),(0x1996200,0x4fb024,'MessageCenter_postMessage')]:
   if string(rel[addr])!=name or rel[addr+8]!=target:raise ValueError('Lua binding mismatch')
  dis=Cs(CS_ARCH_ARM64,CS_MODE_ARM)
  for addr,expected in NATIVE.items():
   instruction=list(dis.disasm(data[offset(addr):offset(addr)+4],addr))
   if len(instruction)!=1 or (instruction[0].mnemonic,instruction[0].op_str)!=expected:raise ValueError('Native instruction mismatch at '+hex(addr))
 return {'status':'PASS','dex_sha256':pins,'effect_sha256':sha,'public_methods':list(methods),'public_fields':list(fields),'native_checks':len(NATIVE)+4,
         'runtime_message_delivery_verified':False,'skin_mask_cpu_readback_verified':False}

EFFECT_SHA='d40af10b250b91cf8f30f4a265ac1d3b7b5b88a82bbf63da332c3f47a415d48e'
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--dex-dir',type=Path,required=True);parser.add_argument('--effect-library',type=Path,required=True);parser.add_argument('--output',type=Path,required=True);args=parser.parse_args()
 result=verify(args.dex_dir,args.effect_library);args.output.write_text(json.dumps(result,indent=2)+'\n');print('PASS',len(result['public_methods']),'methods',len(result['public_fields']),'fields',result['native_checks'],'native checks')
