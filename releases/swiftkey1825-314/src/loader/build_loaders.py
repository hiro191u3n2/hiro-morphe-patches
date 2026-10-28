#!/usr/bin/env python3
"""Update pinned loader resource hashes and exact per-resource capacity only."""
import argparse,hashlib,json,os,struct,subprocess,zipfile,copy
from pathlib import Path
ROOT=Path(__file__).resolve().parent
JVM='app/hiro/swiftkey/patches/JapaneseSamsungEmojiPatch.class'
JVM_SHA='b21d978c7e9b6543b8208acd2e6ada4521732a3370650ed40a34d02eba942a95'
OLD_WORD='1f15766663f5cbf1de8bce2df6a2b8f5e80a488166950979b92ecac8f9e27ca0'
OLD_LICENSE='cab6e0d7ddc41d32e6ea7a7b6d4629367a7d1dfd7744aa14bd8b87bf53b89fca'
LIMIT=192*1024*1024
def sha(b):return hashlib.sha256(b).hexdigest()
def require(ok,message):
 if not ok:raise RuntimeError(message)
def run(cmd,log):
 with log.open('w') as f:r=subprocess.run(list(map(str,cmd)),stdout=f,stderr=subprocess.STDOUT)
 require(r.returncode==0,'Failed; inspect '+str(log))
def patch_class(raw,word_hash,license_hash):
 require(sha(raw)==JVM_SHA,'Baseline JVM pin differs')
 require(raw[:4]==b'\xca\xfe\xba\xbe','Invalid class magic')
 count=struct.unpack_from('>H',raw,8)[0];offset=10;index=1;out=bytearray(raw);changes=[];counts=[0,0,0]
 while index<count:
  tag=raw[offset];offset+=1
  if tag==1:
   n=struct.unpack_from('>H',raw,offset)[0];offset+=2;value=raw[offset:offset+n]
   for i,(old,new) in enumerate(((OLD_WORD,word_hash),(OLD_LICENSE,license_hash))):
    if value==old.encode():
     require(len(new)==n,'Replacement UTF8 length differs');out[offset:offset+n]=new.encode();counts[i]+=1;changes.append((offset,n))
   offset+=n
  elif tag in (3,4):
   if tag==3 and struct.unpack_from('>i',raw,offset)[0]==33554432:
    struct.pack_into('>i',out,offset,LIMIT);counts[2]+=1;changes.append((offset,4))
   offset+=4
  elif tag in (5,6):offset+=8;index+=1
  elif tag in (7,8,16,19,20):offset+=2
  elif tag in (9,10,11,12,17,18):offset+=4
  elif tag==15:offset+=3
  else:raise RuntimeError('Unknown class constant tag '+str(tag))
  index+=1
 require(counts==[1,1,1],'JVM pin/limit occurrence count differs '+repr(counts))
 restored=bytearray(out)
 for start,n in changes:restored[start:start+n]=raw[start:start+n]
 require(restored==raw and len(out)==len(raw),'Unrelated JVM data changed')
 return bytes(out),{'status':'PASSED','jvm_sha256':sha(out),'baseline_jvm_sha256':JVM_SHA,'constant_pool_indices_and_lengths_preserved':True,'method_bytecode_and_all_other_data_byte_identical':True,'replacements':counts,'old_limit':33554432,'new_limit':LIMIT}
def temporary_package(old,path,updates):
 with zipfile.ZipFile(old) as z,zipfile.ZipFile(path,'w') as out:
  for info in z.infolist():out.writestr(copy.copy(info),updates.get(info.filename,z.read(info)))
def main():
 p=argparse.ArgumentParser();p.add_argument('--single',type=Path,required=True);p.add_argument('--bundle',type=Path,required=True);p.add_argument('--words',type=Path,required=True);p.add_argument('--license',type=Path,required=True);p.add_argument('--tools',type=Path,required=True);p.add_argument('--work',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
 require(not a.work.exists(),'Fresh work directory required');a.work.mkdir(parents=True);a.output.mkdir(parents=True,exist_ok=True)
 words=a.words.read_bytes();license=a.license.read_bytes();require(0<len(words)<=LIMIT,'Dictionary size exceeds bound');wh,lh=sha(words),sha(license)
 with zipfile.ZipFile(a.single) as z:base_jvm=z.read(JVM)
 with zipfile.ZipFile(a.bundle) as z:require(z.read(JVM)==base_jvm,'Baseline JVM classes differ')
 jvm,jvm_report=patch_class(base_jvm,wh,lh);(a.output/'JapaneseSamsungEmojiPatch.class').write_bytes(jvm)
 classes=a.work/'classes';classes.mkdir()
 run(['java','com.sun.tools.javac.Main','-cp',a.tools/'morphe.jar','-d',classes,ROOT/'PatchDictionaryLoader.java',ROOT/'LoaderResourceProbe.java'],a.work/'javac.log')
 dex_reports=[];resource_reports=[]
 for kind,old in [('single',a.single),('bundle',a.bundle)]:
  with zipfile.ZipFile(old) as z:baseline=z.read('classes.dex')
  baseline_path=a.work/(kind+'-baseline.dex');baseline_path.write_bytes(baseline)
  output=a.output/(kind+'-classes.dex');report=a.output/(kind+'-dex-report.json')
  run(['java','-cp',str(classes)+os.pathsep+str(a.tools/'morphe.jar'),'PatchDictionaryLoader',baseline_path,output,wh,lh,report],a.work/(kind+'-dex.log'))
  dr=json.loads(report.read_text());require(dr['status']=='PASSED','DEX validation failed');dex_reports.append(dr)
  package=a.work/(kind+'-resources.mpp');temporary_package(old,package,{'classes.dex':output.read_bytes(),JVM:jvm,'swiftkey/words_ja.tsv':words,'swiftkey/words_LICENSE.txt':license})
  # The real JVM class resolves resources from its original complete MPP classpath.
  # Each SDK is independently loaded into its own process, with no previous class cache.
  for sdk in ['morphe.jar','morphe-current.jar']:
   rr=a.output/(kind+'-'+sdk+'-resources.json')
   run([os.environ.get('MORPHE_JAVA','java'),'-Xmx1536m','-cp',str(classes)+os.pathsep+str(a.tools/sdk),'LoaderResourceProbe',package,rr],a.work/(kind+'-'+sdk+'-resources.log'))
   data=json.loads(rr.read_text());require(data['status']=='PASSED','Resource loader probe failed');resource_reports.append(data)
 result={'status':'PASSED','unrelated_android_classes_preserved':True,'unrelated_jvm_classes_preserved':True,'single_android_sha256':dex_reports[0]['android_sha256'],'bundle_android_sha256':dex_reports[1]['android_sha256'],'jvm_sha256':sha(jvm),'dictionary_sha256':wh,'license_sha256':lh,'jvm_report':jvm_report,'android_reports':dex_reports,'resource_reports':resource_reports,'new_asset_limit':LIMIT,'native_android_loader_executed':False,'real_jvm_resource_loader_verified':True}
 (a.output/'loader-report.json').write_text(json.dumps(result,ensure_ascii=False,indent=2,sort_keys=True)+'\n');print(json.dumps(result,ensure_ascii=False))
if __name__=='__main__':main()
