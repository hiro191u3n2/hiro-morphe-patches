#!/usr/bin/env python3
"""Private original/synthetic APK tests. Inputs and patched APKs are not publication assets."""
from pathlib import Path
import argparse,hashlib,json,subprocess,time
from concurrent.futures import ThreadPoolExecutor
from zipfile import ZipFile
NAMES=['ダブルタップから指を離さず片手ズーム','プレビュー作成を後回しにして表示中タブを優先',
       'ロックフリーMessageQueueを有効化','共有先ブラウザの表示を個別に切替','バックフォワードキャッシュの復帰処理を補正']
ROOT=Path(__file__).resolve().parents[1]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(java,host,artifact,apk,label,output_root,expect_success=True):
    folder=output_root/label
    folder.mkdir(parents=True,exist_ok=False)
    command=[str(java),'-Xmx1536m','-jar',str(host),'patch',str(apk),'-p',str(artifact),'--exclusive','--unsigned',
             '--bytecode-mode','FULL','-o',str(folder/'patched.apk'),'-t',str(folder/'temporary'),'-r',str(folder/'report.json')]
    for name in NAMES:command+=['-e',name]
    started=time.monotonic()
    with (folder/'apply.log').open('w') as log:
        result=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,timeout=300)
    text=(folder/'apply.log').read_text()
    report=json.loads((folder/'report.json').read_text()) if (folder/'report.json').exists() else {}
    failed=report.get('failedPatches',[])
    applied=report.get('appliedPatches',[])
    actual=[p if isinstance(p,str) else p.get('name') for p in applied]
    steps=report.get('patchingSteps',report.get('stepResults',[]))
    success=result.returncode==0 and not failed and set(actual)==set(NAMES) and bool(steps) and all(s.get('success') is True for s in steps)
    row={'artifact':artifact.name,'artifact_sha256':sha(artifact),'input_apk_sha256':sha(apk),'input_kind':label.split('-')[0],
         'input_version_name':'9.99.99' if label.startswith('renamed-') else '3.83.74','selected_patches':NAMES,
         'exit_code':result.returncode,'success':success,'applied_patches':actual,'failed_patches':failed,
         'all_build_steps_success':bool(steps) and all(s.get('success') is True for s in steps),
         'output_apk_sha256':sha(folder/'patched.apk') if (folder/'patched.apk').exists() else None,
         'physical_device_tested':False}
    if expect_success and not success:
        print(text[-14000:],flush=True)
        raise RuntimeError('Patch application failed: '+label)
    if not expect_success and (success or '未検証の WebView 構造' not in text):
        raise RuntimeError('Baseline rejection did not match expected unknown-structure guard: '+label)
    if success:
        baseline=ROOT.parent/'work332/qa/application-final2'/label/'patched.apk'
        row['native_body_baseline_332_apk_sha256']=sha(baseline)
        audited=subprocess.run([str(java),'-Xmx1536m','-cp',str(ROOT/'qa/classes')+':'+str(host),'VerifyPatchedHooks',str(folder/'patched.apk'),str(baseline)],capture_output=True,text=True,check=True)
        row['hook_audit']=json.loads(audited.stdout)
        assert row['hook_audit']['result']=='PASS_ACTUAL_HOOKS'
    print('PASS '+label+' '+str(round(time.monotonic()-started,1))+'s',flush=True)
    return row
def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--dist',type=Path)
    p.add_argument('--out',type=Path,required=True)
    p.add_argument('--baseline-only',action='store_true')
    p.add_argument('--jobs',type=int,choices=(1,2,3,4),default=2)
    a=p.parse_args()
    a.out=a.out.resolve();a.out.mkdir(parents=True,exist_ok=False)
    tools=json.loads((ROOT/'tool-paths.json').read_text());java=Path(tools['java']);host=Path(tools['tools']['classpath.jar']['path'])
    fixture_root=Path('/workspace/scratch/fc81f0480f96/work331')
    old=fixture_root/'inputs/Berry_3.83.74_original_base.apk';renamed=fixture_root/'qa/Berry_synthetic_renamed_unsigned.apk'
    if a.baseline_only:
        row=run(java,host,fixture_root/'inputs/Hiro_Morphe_Patches_v1.0.330.mpp',renamed,'renamed-baseline330',a.out,False)
        (a.out/'baseline-rejection.json').write_text(json.dumps(row,ensure_ascii=False,indent=2)+'\n')
        return
    if not a.dist:raise RuntimeError('--dist is required')
    subprocess.run([str(java),'-jar',tools['tools']['ecj.jar']['path'],'-source','11','-target','11','-encoding','UTF-8',
                    '-cp',str(host),'-d',str(ROOT/'qa/classes'),str(ROOT/'qa/VerifyPatchedHooks.java')],check=True)
    combined=a.dist.resolve()/'Hiro_Morphe_Patches_v1.0.335.mpp';single=a.dist.resolve()/'Berry_Browser_v1.4.3.mpp'
    tasks=[(artifact,apk,label+'-'+kind)
           for artifact,kind in [(combined,'combined'),(single,'standalone')]
           for apk,label in [(old,'original'),(renamed,'renamed')]]
    with ThreadPoolExecutor(max_workers=a.jobs) as pool:
        futures=[pool.submit(run,java,host,artifact,apk,label,a.out) for artifact,apk,label in tasks]
        rows=[f.result() for f in futures]
    legacy_extensions=('extensions/berry_foreground_tabs.mpe','extensions/berry_browser_visibility.mpe',
                       'extensions/berry_onehand_zoom.mpe','extensions/berry_messagequeue.mpe')
    with ZipFile(tools['baseline']) as baseline:
        for artifact in (combined,single):
            with ZipFile(artifact) as candidate:
                for name in legacy_extensions:
                    assert candidate.read(name)==baseline.read(name),artifact.name+': '+name
    report={'schema':'berry-repair335-apk-application-v1','result':'PASS','physical_device_tested':False,'berry_3_84_12_tested':False,
            'combined_sha256':sha(combined),'standalone_sha256':sha(single),
            'actual_original_apk_application_passed':True,'synthetic_renamed_apk_application_passed':True,
            'four_legacy_extensions_identical_to_334':True,
            'input_provenance':{'original':'Previously user-provided original3.83.74 base APK',
                'renamed':'Private synthetic transform of original3.83.74: all root classes and selected app members renamed; versionName9.99.99. Not a real released Berry version.'},
            'hook_audit_verifier_sha256':sha(Path(__file__).with_name('VerifyPatchedHooks.java')),'transformer_sha256':sha(Path(__file__).with_name('ReobfuscateFixture.java')),'runs':rows}
    (a.out/'QA_apply335.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print('PASS four original/synthetic application and rebuild tests',flush=True)
if __name__=='__main__':main()
