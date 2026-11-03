#!/usr/bin/env python3
"""Private original/synthetic APK tests. Inputs and patched APKs are not publication assets."""
from pathlib import Path
import argparse,hashlib,json,subprocess,time
from concurrent.futures import ThreadPoolExecutor
from zipfile import ZipFile
NAMES=['ダブルタップから指を離さず片手ズーム','プレビュー作成を後回しにして表示中タブを優先',
       'ロックフリーMessageQueueを有効化','共有先ブラウザの表示を個別に切替','バックフォワードキャッシュの復帰処理を補正']
ROOT=Path(__file__).resolve().parents[1]
ORIGINAL_SHA='4a4da9029d1d71edf376b2d1edf3636e3fba946b49d1fafa5787d20252c8ad5b'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(java,host,artifact,apk,label,output_root,baseline=None):
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
    if not success:
        print(text[-14000:],flush=True)
        raise RuntimeError('Patch application failed: '+label)
    if baseline is not None:
        row['native_body_baseline_338_apk_sha256']=sha(baseline)
        audited=subprocess.run([str(java),'-Xmx1536m','-cp',str(ROOT/'qa/classes')+':'+str(host),'VerifyPatchedHooks',str(folder/'patched.apk'),str(baseline)],capture_output=True,text=True)
        (folder/'audit.log').write_text(audited.stdout+audited.stderr)
        if audited.returncode:raise RuntimeError('Hook audit failed: '+label+'\n'+audited.stdout+audited.stderr)
        row['hook_audit']=json.loads(audited.stdout)
        assert row['hook_audit']['result']=='PASS_ACTUAL_HOOKS'
    (folder/'application-evidence.json').write_text(json.dumps(row,ensure_ascii=False,indent=2)+'\n')
    print('PASS '+label+' '+str(round(time.monotonic()-started,1))+'s',flush=True)
    return row

def prepare(tools,jobs):
    java=Path(tools['java']);host=Path(tools['tools']['classpath.jar']['path'])
    original=Path(tools['original_apk']);assert sha(original)==ORIGINAL_SHA,'Original APK differs from published337 QA input'
    private=ROOT.parent/'private_qa';private.mkdir(exist_ok=True)
    classes=ROOT/'qa/classes';classes.mkdir(exist_ok=True)
    source=ROOT/'qa/ReobfuscateFixture.java'
    subprocess.run([str(java),'-jar',tools['tools']['ecj.jar']['path'],'-source','11','-target','11','-encoding','UTF-8',
                    '-cp',str(host),'-d',str(classes),str(source),str(ROOT/'qa/VerifyPatchedHooks.java')],check=True)
    renamed=private/'Berry_synthetic_renamed_unsigned.apk';fixture_report=private/'synthetic-fixture.json'
    fixture_pin=private/'synthetic-fixture-pin.json'
    expected={'input_sha256':sha(original),'transformer_sha256':sha(source),'classpath_sha256':sha(host)}
    if fixture_pin.exists():
        saved=json.loads(fixture_pin.read_text());assert all(saved[k]==v for k,v in expected.items()),'Synthetic fixture inputs changed'
        assert sha(renamed)==saved['output_sha256'],'Synthetic fixture changed'
    else:
        subprocess.run([str(java),'-Xmx1536m','-cp',str(classes)+':'+str(host),'ReobfuscateFixture',str(original),str(renamed),str(fixture_report)],check=True)
        expected['output_sha256']=sha(renamed);fixture_pin.write_text(json.dumps(expected,indent=2)+'\n')
    baseline=Path(tools['baseline']);baseline_root=private/'application-baseline338';baseline_root.mkdir(exist_ok=True)
    def prior(apk,kind):
        label=kind+'-baseline338';folder=baseline_root/label;record=folder/'application-evidence.json'
        if record.exists():
            row=json.loads(record.read_text())
            assert row['success'] and row['artifact_sha256']==sha(baseline) and row['input_apk_sha256']==sha(apk),'Stale baseline application'
            assert row['output_apk_sha256']==sha(folder/'patched.apk'),'Baseline APK changed'
        else:
            row=run(java,host,baseline,apk,label,baseline_root)
        return kind,folder/'patched.apk',row
    with ThreadPoolExecutor(max_workers=min(jobs,2)) as pool:
        futures=[pool.submit(prior,apk,kind) for apk,kind in [(original,'original'),(renamed,'renamed')]]
        prepared=[f.result() for f in futures]
    return original,renamed,{kind:path for kind,path,row in prepared},[row for kind,path,row in prepared]

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--dist',type=Path)
    p.add_argument('--out',type=Path)
    p.add_argument('--prepare-only',action='store_true')
    p.add_argument('--jobs',type=int,choices=(1,2,3,4),default=2)
    a=p.parse_args()
    tools=json.loads((ROOT/'tool-paths.json').read_text());java=Path(tools['java']);host=Path(tools['tools']['classpath.jar']['path'])
    old,renamed,baselines,baseline_rows=prepare(tools,a.jobs)
    if a.prepare_only:
        print('PASS original/synthetic fixtures and published338 baseline applications ready',flush=True)
        return
    if not a.dist or not a.out:raise RuntimeError('--dist and --out are required')
    a.out=a.out.resolve();a.out.mkdir(parents=True,exist_ok=False)
    combined=a.dist.resolve()/'Hiro_Morphe_Patches_v1.0.339.mpp';single=a.dist.resolve()/'Berry_Browser_v1.4.5.mpp'
    tasks=[(artifact,apk,label+'-'+kind,baselines[label])
           for artifact,kind in [(combined,'combined'),(single,'standalone')]
           for apk,label in [(old,'original'),(renamed,'renamed')]]
    with ThreadPoolExecutor(max_workers=a.jobs) as pool:
        futures=[pool.submit(run,java,host,artifact,apk,label,a.out,baseline) for artifact,apk,label,baseline in tasks]
        rows=[f.result() for f in futures]
    legacy_extensions=('extensions/berry_foreground_tabs.mpe','extensions/berry_browser_visibility.mpe',
                       'extensions/berry_onehand_zoom.mpe','extensions/berry_messagequeue.mpe')
    with ZipFile(tools['baseline']) as baseline:
        for artifact in (combined,single):
            with ZipFile(artifact) as candidate:
                for name in legacy_extensions:
                    assert candidate.read(name)==baseline.read(name),artifact.name+': '+name
    report={'schema':'berry-repair339-apk-application-v1','result':'PASS','physical_device_tested':False,'berry_3_84_12_tested':False,
            'combined_sha256':sha(combined),'standalone_sha256':sha(single),'native_body_baseline_338_mpp_sha256':sha(Path(tools['baseline'])),
            'actual_original_apk_application_passed':True,'synthetic_renamed_apk_application_passed':True,
            'four_legacy_extensions_identical_to_338':True,
            'input_provenance':{'original':'Previously user-provided original3.83.74 base APK restored from the original uploaded APKS; hash matches published337 QA.',
                'renamed':'Private synthetic transform of original3.83.74: all root classes and selected app members renamed; versionName9.99.99. Not a real released Berry version.'},
            'hook_audit_verifier_sha256':sha(Path(__file__).with_name('VerifyPatchedHooks.java')),'transformer_sha256':sha(Path(__file__).with_name('ReobfuscateFixture.java')),
            'baseline_applications':baseline_rows,'runs':rows}
    (a.out/'QA_apply339.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print('PASS four original/synthetic application and rebuild tests',flush=True)
if __name__=='__main__':main()
