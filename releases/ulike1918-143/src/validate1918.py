#!/usr/bin/env python3
"""Verify both original-APKS applications. Patched APKs never leave the local output directory."""
from pathlib import Path
import argparse, io, json, os, subprocess, zipfile
from build1918 import sha, require, run, archive, SINGLE, BUNDLE, PINS
ROOT=Path(__file__).resolve().parent
ORIGINAL_SHA='73c6d3a3008b9975645f63238f07dc9c1960ad982c70f141ee4b5dfe60f7a293'

def verify(dist,tools,apks,work,source_base,existing=False):
 require(sha(apks.read_bytes())==ORIGINAL_SHA,'Original APKS mismatch')
 require(sha((tools/'morphe.jar').read_bytes())==PINS['morphe.jar'],'Morphe pin mismatch')
 require(sha((tools/'android.jar').read_bytes())==PINS['android.jar'],'SDK pin mismatch')
 work.mkdir(parents=True,exist_ok=True);base=archive(source_base)
 removed_native=[]
 for table in ('ulike181/resourceitems.tsv','ulike182/resourceitems.tsv'):
  for line in base[table].decode().splitlines():
   if line.strip():removed_native.append(line.split('\t')[0])
 variants=[];common_native=None
 for kind,name,index in [('standalone',SINGLE,0),('integrated',BUNDLE,1)]:
  folder=work/kind;folder.mkdir(exist_ok=True);mpp=dist/name;payload=archive(mpp);ex=folder/'payload';ex.mkdir(exist_ok=True)
  for n,raw in payload.items():p=ex/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(raw)
  cls=folder/'verifier';cls.mkdir(exist_ok=True);cp=os.pathsep.join(map(str,[tools/'morphe.jar',ex,cls]));
  run(['javac','-cp',cp,'-d',cls,ROOT/'MergePayloads.java',ROOT/'VerifyApplied1918.java',ROOT/'Analyze1918.java'])
  apk=folder/'patched-local-only.apk';result=folder/'result.json'
  if not existing:
   for p in (apk,result):p.unlink(missing_ok=True)
   run(['java','-Xmx3g','-jar',tools/'morphe.jar','patch',apks,'--exclusive','-p',mpp,'--ei',str(index),'--unsigned','--bytecode-mode','FULL','--striplibs','arm64-v8a','-o',apk,'-t',folder/'tmp','-r',result],folder/'apply.log')
  r=json.loads(result.read_text());require(r['packageName']=='com.gorgeous.liteinternational' and r['packageVersion']=='5.6.2','Wrong applied app')
  require(not r['failedPatches'] and len(r['appliedPatches'])==1 and all(s['success'] for s in r['patchingSteps']),'Patch not actually applied')
  dex=run(['java','-Xmx2g','-cp',cp,'VerifyApplied1918',ex/'ulike/runtime.dex',ex/'ulike/methods.tsv',apk],folder/'dex-verification.txt')
  require('1532 runtime/payload contracts' in dex,'Unexpected applied method inventory')
  analysis=run(['java','-Xmx2g','-cp',cp,'Analyze1918',apk,tools/'android.jar',dist/'cleanup-audit1918.tsv',folder/'register-analysis.tsv'])
  require('tested=42\tfailures=0' in analysis,'Register analysis failed')
  with zipfile.ZipFile(apk) as z:
   names=set(z.namelist());require(len(names)==len(z.namelist()),'Duplicate APK entry')
   for n in removed_native:require(n not in names,'Removed trial/native payload remains: '+n)
   for table in ('ulike/assets.tsv','ulike186/resourceitems.tsv'):
    for line in payload[table].decode().splitlines():
     if not line.strip():continue
     parts=line.split('\t');dest=parts[0];source=parts[-1]
     require(source in payload and dest in names,'Missing applied asset '+dest)
     require(z.read(dest)==payload[source],'Applied asset bytes differ '+dest)
   natives={n:sha(z.read(n)) for n in names if n.startswith('lib/') and not n.endswith('/')}
   require(all(n.startswith('lib/arm64-v8a/') for n in natives),'Non-arm64 native library')
   if common_native is None:common_native=natives
   else:require(natives==common_native,'Native libraries differ between standalone and bundle')
  variants.append({'kind':kind,'mpp_filename':name,'mpp_sha256':sha(mpp.read_bytes()),'mpp_bytes':mpp.stat().st_size,'result':'PASS_ORIGINAL_APKS_APPLICATION','applied_patches':r['appliedPatches'],'apk_sha256':sha(apk.read_bytes()),'runtime_and_stock_method_contracts_verified':1532,'register_flow_targets':42,'register_flow_failures':0,'removed_trial_resources_absent':True,'remaining_asset_bytes_verified':True,'native_architectures':['arm64-v8a'],'apk_local_only':True})
 report={'schema':'ulike1918-original-apks-apply-1','result':'PASS_BOTH_MPP_VARIANTS_ORIGINAL_APKS_APPLICATION','ulike_version':'1.9.18','bundle_version':'1.0.143','original_apk_apply_tested':True,'original_apks_sha256':ORIGINAL_SHA,'execution_environment':'authoring_container_with_Morphe_1.16.0','android_device_tested':False,'variants':variants,'removed_trial_resource_paths':removed_native,'native_library_count':len(common_native),'source_sha256':{n:sha((ROOT/n).read_bytes()) for n in ('validate1918.py','VerifyApplied1918.java','Analyze1918.java')},'limitations':['No Android/Galaxy device execution.','DEX MethodAnalyzer is a host register-flow check, not ART runtime verification.','Successful APK patching does not establish actual camera/photo/save behavior.']}
 (dist/'APK_APPLY1918_QA.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report,ensure_ascii=False,indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for key in ('dist','tools','apks','work','source-base'):p.add_argument('--'+key,type=Path,required=True)
 p.add_argument('--existing',action='store_true');a=p.parse_args();verify(a.dist.resolve(),a.tools.resolve(),a.apks.resolve(),a.work.resolve(),a.source_base.resolve(),a.existing)
