#!/usr/bin/env python3
"""Verify final MPPs on official Patcher 2.2.0 in Manager's default STRIP_FAST mode."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, subprocess, zipfile

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('builder',HERE.parent/'src/build.py')
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
TOOL_SHA='855dd2d6fa5ed997a5884a44f8807c9b5bc95e69b5ebd58dfd7e8b897c86e2d7'

def main():
    p=argparse.ArgumentParser()
    for name in ('dist','apk','sdk','build','work','java_home'):p.add_argument('--'+name.replace('_','-'),type=Path,required=True)
    a=p.parse_args()
    for key in vars(a):setattr(a,key,getattr(a,key).resolve())
    b.require(b.sha(a.apk)==b.SUPPLIED_APK_SHA,'Supplied APK differs')
    b.require(b.sha(a.sdk)==TOOL_SHA,'Official SDK differs')
    with zipfile.ZipFile(a.sdk) as z:b.require(b'version=2.2.0' in z.read('app/morphe/patcher/version.properties'),'Wrong Patcher version')
    b.require(not a.work.exists(),'Fresh verification work required');a.work.mkdir(parents=True)
    def run(cmd,name):
        print('RUN '+name,flush=True)
        with (a.work/name).open('w') as out:r=subprocess.run(list(map(str,cmd)),stdout=out,stderr=subprocess.STDOUT,timeout=900)
        text=(a.work/name).read_text();b.require(r.returncode==0,'Failed '+name+'\n'+text[-12000:]);return text.strip()
    java=a.java_home/'bin/java'
    cp=os.pathsep.join(map(str,(a.build/'test',a.build/'patch',a.sdk)))
    guards=run([java,'-Xmx3g','-ea','-cp',cp,'AmazonReviewAudit','guards',a.apk],'guards.txt')
    original_resources=b.resources(a.apk);applications=[];outputs=[]
    artifacts={n:{'bytes':(a.dist/n).stat().st_size,'sha256':b.sha(a.dist/n)} for n in (b.SINGLE,b.BUNDLE)}
    for name in (b.SINGLE,b.BUNDLE):
        listing=run([java,'-jar',a.sdk,'list-patches','--patches',a.dist/name,'-f',b.PACKAGE,'-p','-v'],name+'.listing.txt')
        b.require(listing.count('Name: '+b.PATCH_NAME+'\n')==1,'Patch missing or repeated')
        out=a.work/name;out.mkdir();apk,report=out/'patched.apk',out/'morphe.json'
        run([java,'-Xmx4g','-jar',a.sdk,'patch',a.apk,'-p',a.dist/name,'--exclusive','-e',b.PATCH_NAME,'--unsigned','--bytecode-mode','STRIP_FAST','-o',apk,'-t',out/'tmp','-r',report],name+'.apply.log')
        r=json.loads(report.read_text());applied=[x['name'] if isinstance(x,dict) else x for x in r.get('appliedPatches',[])]
        steps=r.get('stepResults',r.get('patchingSteps',[]))
        b.require(apk.exists() and r.get('success',True) is True and not r.get('failedPatches') and applied==[b.PATCH_NAME] and r.get('packageName')==b.PACKAGE and r.get('packageVersion')==b.APP_VERSION and steps and all(x.get('success') is True for x in steps),'APK patch pipeline failed')
        audit=run([java,'-Xmx4g','-ea','-cp',cp,'AmazonReviewAudit','audit',a.apk,apk,a.build/'runtime-dex/classes.dex'],name+'.audit.txt')
        b.require('PASS' in audit,'APK scope audit failed')
        b.require(b.resources(apk)==original_resources,'APK resources changed')
        outputs.append(b.entries(apk))
        applications.append({'bundle':name,'sdk_version':'2.2.0','desktop_version':'2.0.0','bytecode_mode':'STRIP_FAST','package':b.PACKAGE,'version':b.APP_VERSION,'result':'PASS','audit_summary':audit.splitlines()[-1]})
    b.require(outputs[0]==outputs[1],'Standalone and bundle APK entries differ')
    qa={'schema':'amazon-review100-patcher22-v1','artifacts':artifacts,'supplied_apk_sha256':b.SUPPLIED_APK_SHA,'physical_android_tested':False,'tool_sha256':TOOL_SHA,'applications':applications,'guard_tests':guards,'bytecode_mode':'STRIP_FAST','single_bundle_apk_entry_contents_equal':True,'supplied_apk_non_dex_resources_unchanged':True}
    (HERE/'QA_Amazon_Patcher22.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
    print('PASS official Patcher 2.2.0 standalone/bundle applications, exact class audit, unchanged resources')
if __name__=='__main__':main()
