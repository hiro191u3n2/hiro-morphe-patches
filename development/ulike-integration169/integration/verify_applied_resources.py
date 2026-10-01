#!/usr/bin/env python3
"""Check patch assets and either unchanged native libraries or one pinned NV21 fix.

Candidate builds must opt in with ``--native-contract nv21-effect-flag``. The
opt-in requires the exact reviewed four-byte change; it does not permit general
native-library replacement or silently accept an unchanged candidate library.
"""
import argparse,hashlib,io,json,zipfile
from pathlib import Path
NATIVE_ENTRY='lib/arm64-v8a/libttvesdk.so'
NATIVE_BEFORE='67f1b97b92859630ae200cafad41f7e3db3d8d2fbcac3ffd5c7bab0683471095'
NATIVE_AFTER='eac57bcaa613860c7ca03ea516c4370520f9775cdfe700688d74e223bd59c2bb'
NATIVE_OFFSET=0x40b864
NATIVE_INSTRUCTION_BEFORE=bytes.fromhex('26008052')
NATIVE_INSTRUCTION_AFTER=bytes.fromhex('a6e34039')
def sha(data):return hashlib.sha256(data).hexdigest()
def require(condition,message):
 if not condition:raise ValueError(message)
def verify_native_contract(native,emitted,original_bytes,output_bytes,contract):
 """Compare complete native inventories and, if selected, every pinned ELF byte."""
 require(set(native)==set(emitted),'Native libraries missing or added')
 if contract=='unchanged':
  require(emitted==native,'Native libraries changed under unchanged contract')
  return []
 require(contract=='nv21-effect-flag','Unknown native contract')
 require(native.get(NATIVE_ENTRY)==NATIVE_BEFORE,'Unexpected original NV21 library hash')
 require(emitted.get(NATIVE_ENTRY)==NATIVE_AFTER,'Expected NV21 native fix missing or mismatched')
 require(original_bytes is not None and output_bytes is not None,'NV21 native library bytes unavailable')
 require(sha(original_bytes)==NATIVE_BEFORE and sha(output_bytes)==NATIVE_AFTER,'Native bytes disagree with pinned hashes')
 require(len(original_bytes)==len(output_bytes),'NV21 native library length changed')
 end=NATIVE_OFFSET+4
 require(original_bytes[NATIVE_OFFSET:end]==NATIVE_INSTRUCTION_BEFORE,'NV21 original instruction differs')
 require(output_bytes[NATIVE_OFFSET:end]==NATIVE_INSTRUCTION_AFTER,'NV21 replacement instruction differs')
 require(original_bytes[:NATIVE_OFFSET]==output_bytes[:NATIVE_OFFSET] and original_bytes[end:]==output_bytes[end:],
         'NV21 native bytes outside the reviewed instruction changed')
 require(all(emitted[n]==h for n,h in native.items() if n!=NATIVE_ENTRY),'Unrelated native library changed')
 return [dict(entry=NATIVE_ENTRY,before_sha256=NATIVE_BEFORE,after_sha256=NATIVE_AFTER,
              file_offset=NATIVE_OFFSET,file_offset_hex=hex(NATIVE_OFFSET),
              before_hex=NATIVE_INSTRUCTION_BEFORE.hex(),after_hex=NATIVE_INSTRUCTION_AFTER.hex(),
              instruction_bytes=4,library_bytes=len(original_bytes),all_other_bytes_unchanged=True)]
def main():
 p=argparse.ArgumentParser()
 for k in ['apks','mpp','apk','result','report']:p.add_argument('--'+k,type=Path,required=True)
 p.add_argument('--native-contract',choices=['unchanged','nv21-effect-flag'],default='unchanged',
                help='Default requires all native bytes unchanged; candidate mode requires the exact reviewed NV21 instruction fix')
 a=p.parse_args();r=json.loads(a.result.read_text())
 assert r['packageName']=='com.gorgeous.liteinternational' and r['packageVersion']=='5.6.2'
 assert r['failedPatches']==[] and r['appliedPatches']==[{'name':'高画質撮影・質感美肌・素材通信を復旧'}]
 assert r['patchingSteps'] and all(v['success'] for v in r['patchingSteps'])
 assert {v['step'] for v in r['patchingSteps']}>={'PATCHING','REBUILDING'}
 native={};assets={};native_original=None
 with zipfile.ZipFile(a.apks) as bundle:
  for partname in bundle.namelist():
   if not partname.endswith('.apk'):continue
   with zipfile.ZipFile(io.BytesIO(bundle.read(partname))) as part:
    for n in part.namelist():
     if n.endswith('/'):continue
     dest=native if n.startswith('lib/') else assets if n.startswith('assets/') else None
     if dest is not None:
      data=part.read(n);h=sha(data);assert n not in dest or dest[n]==h;dest[n]=h
      if n==NATIVE_ENTRY:native_original=data
 changed=[]
 with zipfile.ZipFile(a.apk) as out,zipfile.ZipFile(a.mpp) as patch:
  assert len(out.namelist())==len(set(out.namelist())) and out.testzip() is None
  emitted={n:sha(out.read(n)) for n in out.namelist() if n.startswith('lib/') and not n.endswith('/')}
  native_output=out.read(NATIVE_ENTRY) if NATIVE_ENTRY in emitted else None
  additions={}
  for line in patch.read('ulike169/resourceitems.tsv').decode().splitlines():
   target,digest,length,resource=line.split('\t')
   assert target not in native and target not in assets and target not in additions
   data=patch.read(resource);assert sha(data)==digest and len(data)==int(length)
   assert out.read(target)==data;additions[target]=digest
  assert set(additions)=={'lib/arm64-v8a/libonnxruntime.so','lib/arm64-v8a/libonnxruntime4j_jni.so','assets/ulike169/onnxruntime-LICENSE.txt','assets/ulike169/onnxruntime-ThirdPartyNotices.txt','assets/ulike169/onnxruntime-Privacy.md','assets/ulike169/onnxruntime-LICENSE-1DS.txt'}
  assert set(emitted)==set(native)|{n for n in additions if n.startswith('lib/')}
  assert {n for n in out.namelist() if n.startswith('assets/') and not n.endswith('/')}==set(assets)|{n for n in additions if n.startswith('assets/')}
  native_changes=verify_native_contract(native,{n:h for n,h in emitted.items() if n not in additions},native_original,native_output,a.native_contract)
  for line in patch.read('ulike/assets.tsv').decode().splitlines():
   if not line:continue
   target,before,after,payload=line.split('\t');assert assets[target]==before
   assert sha(out.read(target))==after==sha(patch.read(payload));changed.append(target)
  preserved=0
  for n,h in assets.items():
   if n not in changed:assert sha(out.read(n))==h,'Unrelated asset changed '+n;preserved+=1
 result=dict(result='PASS',input_apks_sha256=sha(a.apks.read_bytes()),mpp_sha256=sha(a.mpp.read_bytes()),apk_sha256=sha(a.apk.read_bytes()),runtime_additions=additions,native_contract=a.native_contract,native_libraries_total=len(native),native_libraries_preserved=len(native)-len(native_changes),native_changes=native_changes,contracted_assets=changed,unrelated_assets_preserved=preserved,patching_result=r)
 a.report.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
 print(f'PASS resources: {len(native)-len(native_changes)} native libraries and {preserved} unrelated assets unchanged; {len(native_changes)} exact native instruction contract(s); {len(changed)} assets match contracts.')
if __name__=='__main__':main()
