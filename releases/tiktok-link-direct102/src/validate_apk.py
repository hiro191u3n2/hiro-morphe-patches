#!/usr/bin/env python3
"""Apply exact Link Direct 1.0.2/bundle310 and baseline309 to signed TikTok 47.4.3.

Four real applies are required: baseline OneBack alone, each new MPP with Link
Direct alone, and the new aggregate with Link Direct plus the current OneBack.
All new outputs receive popup, fast-route and Back audits. The combined case
uses the actual baseline OneBack output as its preservation reference.
No physical Android UI test or distribution of an APK is implied.
"""
from pathlib import Path
import argparse, json, os, subprocess, sys

ROOT = Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT))
from build import (SINGLE, BUNDLE, QA, BASE_VERSION, BASE_SHA, BASE_BYTES, BASE_ONEBACK_EXTENSION,
                   EXTENSION, TOOLS, sha, sha_bytes, require, json_bytes, archive, validate_baseline)

NAME = '共有リンクの小窓を省略して動画を直接再生（TikTok 47.4.3）'
ONEBACK = '戻る1回で終了・履歴削除'
BASELINE_NAME = 'Hiro_Morphe_Patches_v'+BASE_VERSION+'.mpp'
IDENTITY = {'package':'com.zhiliaoapp.musically','version_name':'47.4.3','version_code':2024704030,
    'bytes':474921201,'sha256':'af7cbe2b00d33ff1339730f6abb8ed7e387a83b27ff2b16748b8f41e6fc87e0b',
    'signature_verified':True,'signer_certificate_sha256':'9041803e91bcb814b4b4399fb5c85a91640b755e5e8ba76813814bf4cf2ab5ba'}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ('apk','dist','base','tools','work'): parser.add_argument('--'+key,type=Path,required=True)
    args = parser.parse_args()
    for key in vars(args): setattr(args,key,getattr(args,key).resolve())
    require(sha(args.apk) == IDENTITY['sha256'] and args.apk.stat().st_size == IDENTITY['bytes'], 'Exact original APK required')
    require(sha(args.base) == BASE_SHA and args.base.stat().st_size == BASE_BYTES, 'Exact published baseline309 required')
    baseline_entries = archive(args.base); validate_baseline(baseline_entries)
    for name in ('morphe.jar','morphe200.jar'): require(sha(args.tools/name) == TOOLS[name], 'Patcher SDK pin changed')
    require(not args.work.exists(), 'Use a fresh original-APK verification directory')
    artifacts = {name:{'bytes':(args.dist/name).stat().st_size,'sha256':sha(args.dist/name)} for name in (SINGLE,BUNDLE)}
    qa = json.loads((args.dist/QA).read_text())
    require(qa.get('schema') == 'tiktok-link-direct-build-v3' and qa.get('artifacts') == artifacts, 'Build evidence uses different MPPs')
    require(qa.get('runtime_host_test','').startswith('PASS SharedLinkExitHostTest '), 'Actual compiled runtime host tests required')
    require(qa.get('runtime_extension_audit','').startswith('PASS RuntimeExtensionAudit '), 'Serialized runtime extension audit required')
    require(qa.get('baseline_oneback_extension') == BASE_ONEBACK_EXTENSION, 'Build proof uses a different OneBack extension')
    single_entries, combined_entries = archive(args.dist/SINGLE), archive(args.dist/BUNDLE)
    runtime = single_entries.get(EXTENSION)
    require(runtime is not None and combined_entries.get(EXTENSION) == runtime, 'Runtime absent or differs between new MPPs')
    runtime_identity = {'path':EXTENSION,'bytes':len(runtime),'sha256':sha_bytes(runtime)}
    require(qa.get('runtime_extension') == runtime_identity, 'Build runtime identity differs')
    oneback_path = BASE_ONEBACK_EXTENSION['path']
    require(combined_entries.get(oneback_path) == baseline_entries[oneback_path], 'Aggregate changed current OneBack runtime')
    args.work.mkdir(parents=True)
    evidence = ROOT/'validation'; evidence.mkdir(exist_ok=True)
    logs = evidence/'logs'; logs.mkdir(exist_ok=True)

    def run(command,label,timeout=1500):
        print('RUN '+label,flush=True)
        log = args.work/(label+'.log')
        with log.open('w') as output:
            result = subprocess.run(list(map(str,command)),stdout=output,stderr=subprocess.STDOUT,timeout=timeout)
        raw = log.read_text(); (logs/log.name).write_text(raw)
        require(result.returncode == 0, 'Failed '+label+'\n'+raw[-18000:])
        return raw.strip()

    classes = args.work/'audit'; classes.mkdir()
    sdk = args.tools/'morphe200.jar'
    cp = os.pathsep.join(map(str,(classes,args.dist/SINGLE,sdk)))
    sources = [ROOT/'test'/name for name in ('OriginalIdentity.java','LinkBranchAudit.java',
               'LinkFastRouteAudit.java','LinkBackAudit.java','TargetAdmissionTest.java')]
    require(all(p.is_file() for p in sources), 'All original, popup, fast-route, Back and admission audit sources must exist')
    run(['javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',classes,*sources],'audit-compile',300)
    signature = run(['java','-Xmx1g','-cp',cp,'OriginalIdentity',args.apk],'original-signature')
    require(signature.startswith('PASS OriginalIdentity verified=true signer_certificate_sha256='
            +IDENTITY['signer_certificate_sha256']+' v1=true v2=true v3=true'), 'Original signature not confirmed')
    contracts = {}
    for label, audit in (('original_contract','LinkBranchAudit'),('original_fast_route_contract','LinkFastRouteAudit'),
                         ('original_back_contract','LinkBackAudit')):
        contracts[label] = run(['java','-Xmx2g','-ea','-cp',cp,audit,'original',args.apk],label.replace('_','-'))
        require(contracts[label].startswith('PASS '+audit+' original:'), 'Original native contract audit failed: '+audit)
    admission = {}
    for name in ('morphe.jar','morphe200.jar'):
        testcp = os.pathsep.join(map(str,(classes,args.dist/SINGLE,args.tools/name)))
        admission[name] = run(['java','-ea','-cp',testcp,'TargetAdmissionTest',args.work/('admission-'+name)],'admission-'+name,300)
        require(admission[name].startswith('PASS TargetAdmissionTest'), 'Forced target rejection failed')

    def apply(label,mpp,name,pin,selected):
        folder = args.work/label; folder.mkdir()
        output, result_file = folder/'patched.apk', folder/'result.json'
        command = ['java','-Xmx3g','-jar',sdk,'patch','--unsigned','--exclusive','--bytecode-mode=FULL',
                   '-o',output,'-t',folder/'temp','-r',result_file,'-p',mpp]
        for patch in selected: command += ['-e',patch]
        command.append(args.apk)
        run(command,label+'-apply')
        result = json.loads(result_file.read_text())
        require(result.get('success') is True and result.get('failedPatches') == [], 'Incomplete patch result')
        applied = result.get('appliedPatches')
        require(isinstance(applied,list) and len(applied) == len(selected) and set(applied) == set(selected), 'Wrong applied patch inventory')
        steps = result.get('stepResults',[])
        require(steps and all(x.get('success') is True for x in steps)
                and {'PATCHING','REBUILDING'} <= {x.get('step') for x in steps}, 'Partial application pipeline')
        require(result.get('packageName') == IDENTITY['package'] and result.get('packageVersion') == IDENTITY['version_name']
                and output.is_file(), 'Wrong emitted APK')
        (logs/(label+'-result.json')).write_bytes(json_bytes(result))
        row = {'case':label,'artifact':name,'artifact_sha256':pin,'result':'PASS','selected_patches':selected,
               'applied_patches':applied,'failed_patches':[],'steps_all_success':True,'output_apk_sha256':sha(output),
               'output_apk_bytes':output.stat().st_size,'patcher_result':'logs/'+label+'-result.json'}
        return output,row

    baseline_output, baseline_case = apply('baseline_oneback',args.base,BASELINE_NAME,BASE_SHA,[ONEBACK])
    cases = []
    for label,name,selected in (('standalone_link',SINGLE,[NAME]),('aggregate_link',BUNDLE,[NAME]),
                                ('aggregate_link_oneback',BUNDLE,[NAME,ONEBACK])):
        output,row = apply(label,args.dist/name,name,artifacts[name]['sha256'],selected)
        reference = [baseline_output] if label == 'aggregate_link_oneback' else []
        for field,audit in (('direct_link_audit','LinkBranchAudit'),('fast_route_audit','LinkFastRouteAudit'),
                            ('back_exit_audit','LinkBackAudit')):
            row[field] = run(['java','-Xmx2g','-ea','-cp',cp,audit,'output',args.apk,output,*reference],label+'-'+field)
            require(row[field].startswith('PASS '+audit+' output:'), 'Emitted native audit failed: '+audit)
        if reference: row['baseline_reference_apk_sha256'] = baseline_case['output_apk_sha256']
        cases.append(row)
        print('PASS actual APK '+label,flush=True)

    require(artifacts == {name:{'bytes':(args.dist/name).stat().st_size,'sha256':sha(args.dist/name)} for name in artifacts}, 'MPP changed during verification')
    require(sha(args.base) == BASE_SHA and args.base.stat().st_size == BASE_BYTES, 'Baseline MPP changed during verification')
    require(sha(baseline_output) == baseline_case['output_apk_sha256'], 'Baseline OneBack-only output changed during verification')
    require(sha(args.apk) == IDENTITY['sha256'] and args.apk.stat().st_size == IDENTITY['bytes'], 'Original APK changed during verification')
    proof = {'schema':'tiktok-link-direct-real-apk-v3','input':IDENTITY,'physical_android_tested':False,
             'artifacts':artifacts,'baseline':{'artifact':BASELINE_NAME,'bytes':BASE_BYTES,'sha256':BASE_SHA},
             'baseline_oneback_extension':BASE_ONEBACK_EXTENSION,'runtime_extension':runtime_identity,
             'runtime_host_test':qa['runtime_host_test'],'patcher_sdk_sha256':TOOLS['morphe200.jar'],
             'original_signature':signature,**contracts,'forced_target_admission':admission,
             'baseline_oneback':baseline_case,'cases':cases,
             'configuration':'Four exact original-APK applies: baseline309 OneBack only; Link Direct 1.0.2 alone from standalone and bundle310; bundle310 Link Direct plus current OneBack. All three new outputs require popup, fast-route and Back audits; the combined case compares against the actual baseline OneBack-only output. Android device not tested.'}
    (evidence/'real-apk.json').write_bytes(json_bytes(proof))
    print('PASS four exact MPP applications; popup, fast-route, Back and OneBack preservation audited; Android device not tested',flush=True)

if __name__ == '__main__': main()
