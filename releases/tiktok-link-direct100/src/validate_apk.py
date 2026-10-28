#!/usr/bin/env python3
"""Apply both exact new MPPs to signed TikTok 47.4.3 and audit emitted branches.

No original/rebuilt APK or personal screenshot is copied into publication assets.
This validates static native routes and reconstruction, not Android device UI.
"""
from pathlib import Path
import argparse, json, os, subprocess, sys
ROOT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT))
from build import SINGLE, BUNDLE, TOOLS, sha, require, json_bytes
NAME='共有リンクの小窓を省略して動画を直接再生（TikTok 47.4.3）'
IDENTITY={'package':'com.zhiliaoapp.musically','version_name':'47.4.3','version_code':2024704030,
    'bytes':474921201,'sha256':'af7cbe2b00d33ff1339730f6abb8ed7e387a83b27ff2b16748b8f41e6fc87e0b',
    'signature_verified':True,'signer_certificate_sha256':'9041803e91bcb814b4b4399fb5c85a91640b755e5e8ba76813814bf4cf2ab5ba'}

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    for key in ('apk','dist','tools','work'):parser.add_argument('--'+key,type=Path,required=True)
    args=parser.parse_args()
    for key in vars(args):setattr(args,key,getattr(args,key).resolve())
    require(sha(args.apk)==IDENTITY['sha256'] and args.apk.stat().st_size==IDENTITY['bytes'],'Exact original APK required')
    for name in ('morphe.jar','morphe200.jar'):require(sha(args.tools/name)==TOOLS[name],'Patcher SDK pin changed')
    require(not args.work.exists(),'Use a fresh original-APK verification directory')
    args.work.mkdir(parents=True)
    evidence=ROOT/'validation';evidence.mkdir(exist_ok=True)
    logs=evidence/'logs';logs.mkdir(exist_ok=True)
    artifacts={name:{'bytes':(args.dist/name).stat().st_size,'sha256':sha(args.dist/name)} for name in (SINGLE,BUNDLE)}
    def run(command,label,timeout=1500):
        print('RUN '+label,flush=True)
        log=args.work/(label+'.log')
        with log.open('w') as output:result=subprocess.run(list(map(str,command)),stdout=output,stderr=subprocess.STDOUT,timeout=timeout)
        raw=log.read_text();(logs/log.name).write_text(raw)
        require(result.returncode==0,'Failed '+label+'\n'+raw[-18000:])
        return raw.strip()
    classes=args.work/'audit';classes.mkdir()
    sdk=args.tools/'morphe200.jar'
    cp=os.pathsep.join(map(str,(classes,args.dist/SINGLE,sdk)))
    sources=[ROOT/'test'/n for n in ('OriginalIdentity.java','LinkBranchAudit.java','TargetAdmissionTest.java')]
    run(['javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',classes,*sources],'audit-compile',300)
    signature=run(['java','-Xmx1g','-cp',cp,'OriginalIdentity',args.apk],'original-signature')
    require(signature.startswith('PASS OriginalIdentity verified=true signer_certificate_sha256='+IDENTITY['signer_certificate_sha256']), 'Original signature not confirmed')
    original=run(['java','-Xmx2g','-ea','-cp',cp,'LinkBranchAudit','original',args.apk],'original-link-contract')
    require(original.startswith('PASS LinkBranchAudit original:'),'Original native contract audit failed')
    admission={}
    for name in ('morphe.jar','morphe200.jar'):
        testcp=os.pathsep.join(map(str,(classes,args.dist/SINGLE,args.tools/name)))
        admission[name]=run(['java','-ea','-cp',testcp,'TargetAdmissionTest',args.work/('admission-'+name)],'admission-'+name,300)
        require(admission[name].startswith('PASS TargetAdmissionTest'),'Forced target rejection failed')
    cases=[]
    for label,name in (('standalone',SINGLE),('aggregate',BUNDLE)):
        folder=args.work/label;folder.mkdir()
        output=folder/'patched.apk';result_file=folder/'result.json'
        command=['java','-Xmx3g','-jar',sdk,'patch','--unsigned','--exclusive','--bytecode-mode=FULL',
                 '-o',output,'-t',folder/'temp','-r',result_file,'-p',args.dist/name,'-e',NAME,args.apk]
        run(command,label+'-apply')
        result=json.loads(result_file.read_text())
        require(result.get('success') is True and result.get('failedPatches')==[],'Incomplete patch result')
        require(result.get('appliedPatches')==[NAME],'Wrong applied patch inventory')
        steps=result.get('stepResults',[])
        require(steps and all(x.get('success') is True for x in steps)
                and {'PATCHING','REBUILDING'} <= {x.get('step') for x in steps},'Partial application pipeline')
        require(result.get('packageName')==IDENTITY['package'] and result.get('packageVersion')==IDENTITY['version_name'] and output.is_file(),'Wrong emitted APK')
        (logs/(label+'-result.json')).write_bytes(json_bytes(result))
        audit=run(['java','-Xmx2g','-ea','-cp',cp,'LinkBranchAudit','output',args.apk,output],label+'-link-audit')
        require(audit.startswith('PASS LinkBranchAudit output:'),'Emitted native direct-link audit failed')
        cases.append({'artifact':name,'artifact_sha256':artifacts[name]['sha256'],'result':'PASS',
                      'selected_patches':[NAME],'applied_patches':result['appliedPatches'],'failed_patches':[],
                      'steps_all_success':True,'output_apk_sha256':sha(output),'output_apk_bytes':output.stat().st_size,
                      'direct_link_audit':audit,'patcher_result':'logs/'+label+'-result.json'})
        print('PASS actual APK '+label,flush=True)
    require(artifacts=={name:{'bytes':(args.dist/name).stat().st_size,'sha256':sha(args.dist/name)} for name in artifacts},'MPP changed during verification')
    proof={'schema':'tiktok-link-direct-real-apk-v1','input':IDENTITY,'physical_android_tested':False,
           'artifacts':artifacts,'patcher_sdk_sha256':TOOLS['morphe200.jar'],'original_signature':signature,
           'original_contract':original,'forced_target_admission':admission,'cases':cases,
           'configuration':'New direct-link patch exclusively selected from standalone and aggregate; other features preserved byte-for-byte in aggregate but not selected in these APK cases.'}
    (evidence/'real-apk.json').write_bytes(json_bytes(proof))
    print('PASS both exact MPPs applied and emitted shared-video paths audited; Android device not tested',flush=True)

if __name__=='__main__':main()
