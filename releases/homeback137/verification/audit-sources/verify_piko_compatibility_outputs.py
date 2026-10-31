#!/usr/bin/env python3
"""Additional read-only Piko321 output verification after complete Manager QA."""
from pathlib import Path
import argparse,hashlib,json,os,subprocess
ROOT=Path(__file__).resolve().parent
WORK=ROOT.parent

def check(ok,why):
    if not ok: raise RuntimeError(why)

def identity(p):
    with p.open('rb') as f:return {'bytes':p.stat().st_size,'sha256':hashlib.file_digest(f,'sha256').hexdigest()}

def write(p,d):p.write_text(json.dumps(d,ensure_ascii=False,indent=2,sort_keys=True)+'\n')

def main():
    p=argparse.ArgumentParser();p.add_argument('--summary',type=Path,required=True);p.add_argument('--summary-sha256',required=True);p.add_argument('--mpp',type=Path,required=True);p.add_argument('--build-proof',type=Path,required=True);p.add_argument('--source-root',type=Path,required=True);a=p.parse_args()
    summary_path=a.summary.resolve();summary_id=identity(summary_path)
    check(summary_id['sha256']==a.summary_sha256,'Summary differs')
    summary=json.loads(summary_path.read_text())
    check(summary['schema']=='homeback137-manager-final-v1' and summary['result']=='PASS' and summary['complete'] is True,'Complete final321 QA required')
    check(summary['intended_release_scope']=='external_profile_back_to_reels_then_exit','Wrong feature scope')
    driver_path=Path(__file__).resolve();driver_id=identity(driver_path)
    mpp=a.mpp.resolve();build_proof=a.build_proof.resolve();source_root=a.source_root.resolve()
    check(identity(mpp)==summary['mpp'],'Final MPP differs')
    check(identity(build_proof)==summary['build_proof'],'Final build proof differs')
    build=json.loads(build_proof.read_text())
    check(build['version']=='1.1.30' and build['bundle_version']=='1.0.321'
          and build['artifacts'].get(mpp.name)==summary['mpp'],'Wrong frozen build artifact')
    folder=summary_path.parent
    output=folder/'piko-compatibility-output-proof.json'
    check(not output.exists(),'Use one final extra proof per QA run')
    tools=summary['verification_tools']
    compiled=tools['compiled_build_audit_and_patch_classes']
    selected=[x for x in compiled if x['path'].endswith('/audit/Instagram137PikoContractAudit.class')]
    check(len(selected)==1,'Frozen Piko audit class missing')
    audit_dir=(WORK/selected[0]['path']).parent
    build_dir=audit_dir.parent
    pins=summary['pinned_inputs']
    def pin(name):
        row=pins[name];path=WORK/row['path'];check(identity(path)=={k:row[k] for k in ('bytes','sha256')},'Input changed: '+name);return path
    java=pin('java');sdk=pin('morphe_sdk');original=pin('original_apk');baseline=pin('piko_baseline')
    def freeze():
        check(identity(summary_path)==summary_id,'Summary changed')
        check(identity(driver_path)==driver_id,'Additional audit driver changed')
        check(identity(mpp)==summary['mpp'],'Final MPP changed')
        check(identity(build_proof)==summary['build_proof'],'Final build proof changed')
        source_ids={x.relative_to(source_root).as_posix():identity(x)['sha256']
                    for x in sorted(source_root.rglob('*')) if x.is_file()
                    and '__pycache__' not in x.parts and x.suffix in ('.java','.py','.dex')}
        check(source_ids==build['sources'],'Final frozen source inventory changed')
        for row in compiled:
            check(identity(WORK/row['path'])=={k:row[k] for k in ('bytes','sha256')},'Compiled tool changed: '+row['path'])
        for name in ('java','morphe_sdk','original_apk','piko_baseline'):pin(name)
    cases={x['case']:x for x in summary['cases']}
    check(set(cases)=={'hiro-only','piko64-hiro321','hiro321-piko64'},'Cases differ')
    results={}
    cp=os.pathsep.join(map(str,[audit_dir,build_dir/'patch',sdk]))
    for name in ('piko64-hiro321','hiro321-piko64'):
        freeze();apk=folder/name/'patched.apk';before=identity(apk)
        check(before==cases[name]['output'],'Final mixed APK differs')
        log=folder/name/'piko-compatibility-output-audit.txt';check(not log.exists(),'Prior audit log exists')
        command=[str(java),'-XX:-UsePerfData','-ea','-Xmx3g','-cp',cp,'Instagram137PikoContractAudit',str(original),str(baseline),str(apk)]
        with log.open('w') as f:run=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
        check(run.returncode==0,'Additional Piko audit failed: '+str(log))
        passes=[l for l in log.read_text().splitlines() if l.startswith('PASS Instagram137PikoContractAudit:')]
        check(len(passes)==1,'Exact Piko PASS result missing')
        check(identity(apk)==before,'Additional audit mutated output APK')
        freeze()
        results[name]={'result':'PASS','output':before,'log':identity(log),'summary':passes,'command':command,'all_inputs_and_output_unchanged':True}
        print('PASS',name,passes[0],flush=True)
    proof={'schema':'homeback137-piko-output-compatibility-v1','result':'PASS','physical_android_tested':False,
           'manager_summary':summary_id,'driver':{'path':str(driver_path.relative_to(WORK)),**driver_id},
           'mpp':summary['mpp'],'original_apk':identity(original),'piko_baseline':identity(baseline),
           'compiled_audit_and_patch_class_count':len(compiled),'compiled_classes_frozen_to_manager_summary':True,
           'cases':results,'all_inputs_and_outputs_unchanged':True,'source_build_proof_and_mpp_unchanged':True,'frozen_source_files':len(build['sources'])}
    write(output,proof);print('PASS FINAL',str(output),identity(output)['sha256'],flush=True)
if __name__=='__main__':main()
