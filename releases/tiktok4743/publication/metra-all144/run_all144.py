#!/usr/bin/env python3
"""Explicit all-144 supplementary collision probe; never a replacement for release gates.
Default invocation previews the command only. The parent schedules --execute once
one Java pipeline slot becomes free. This script starts at most one Java process
at a time, uses an isolated Morphe data root and never publishes an output APK.
"""
from pathlib import Path
import argparse, collections, hashlib, json, os, re, shlex, shutil, subprocess, sys
from datetime import datetime, timezone

HERE=Path(__file__).resolve().parent
BASE=HERE.parents[1]
APK=BASE/'tiktok4743-work/apk/TikTok_47.4.3_original.apk'
UPSTREAM=BASE/'tiktok-ja-work/upstream/patches-0.9.0-dev.1.mpp'
SDK=BASE/'tiktok-work/tools/morphe200.jar'
JAVA=BASE/'tiktok-work/jdk/bin/java'
JAVAC=BASE/'tiktok-work/jdk/bin/javac'
INPUT_SHA='af7cbe2b00d33ff1339730f6abb8ed7e387a83b27ff2b16748b8f41e6fc87e0b'
UPSTREAM_SHA='45dc0c2244f8cf6973de18542d2d9a0cc25512f5b1de52438c685b82edb7c4cd'
SDK_SHA='deb5d931f3899b20a119637473d338c52585f0c561b5d8b2856b916fe90163b5'
PACKAGE='com.zhiliaoapp.musically'
VERSION='47.4.3'
ARTIFACT='TikTok_47.4.3_Hiro_v4.1.0.mpp'
QA='QA_TikTok_47.4.3_v4.1.0.json'

def sha(path):
    h=hashlib.sha256()
    with path.open('rb') as stream:
        while block:=stream.read(1024*1024):h.update(block)
    return h.hexdigest()

def write(path,value):path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n')
def need(value,why):
    if not value:raise RuntimeError(why)

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--execute',action='store_true',help='Run after the parent schedules the free Java slot')
    parser.add_argument('--heap',choices=('2g','3g','4g'),default='2g',help='Maximum heap for the patch process; keep prior runs separate')
    parser.add_argument('--artifact',type=Path,default=BASE/'tiktok4743-work/dist-pilot'/ARTIFACT)
    parser.add_argument('--work',type=Path,default=HERE/'run')
    args=parser.parse_args();artifact=args.artifact.resolve();work=args.work.resolve()
    upstream=json.loads((HERE/'upstream-selected.json').read_text())
    hiro=json.loads((HERE/'hiro-selected.json').read_text())
    selected=upstream+hiro
    need(len(upstream)==136 and len(hiro)==8 and len(set(selected))==144,'Explicit selection changed')
    sdk=work/'tools/morphe.jar';output=work/'patched.apk';result_file=work/'result.json'
    command=[str(JAVA),'-Xmx'+args.heap,'-jar',str(sdk),'patch','--unsigned','--exclusive',
             '--continue-on-error','--bytecode-mode=FULL','-o',str(output),'-t',str(work/'temp'),'-r',str(result_file)]
    for bundle,names in [(UPSTREAM,upstream),(artifact,hiro)]:
        command+=['-p',str(bundle)]
        for name in names:command+=['-e',name]
    command.append(str(APK))
    if not args.execute:
        print('PREPARED ONLY: '+shlex.join(command))
        print('selected=144; Metra=136; Hiro=8; heap='+args.heap+'; force=false; continue_on_error=true; no patch execution')
        return 0
    need(not work.exists(),'Use a fresh isolated --work directory')
    need(sha(APK)==INPUT_SHA and APK.stat().st_size==474921201,'Wrong signed original 47.4.3 APK')
    need(sha(UPSTREAM)==UPSTREAM_SHA,'Upstream 0.9.0-dev.1 MPP changed')
    need(sha(SDK)==SDK_SHA,'Patcher 2.0 SDK changed')
    need(artifact.name==ARTIFACT,'Wrong Hiro artifact name')
    build=json.loads((artifact.parent/QA).read_text())
    artifact_sha=sha(artifact)
    need(build.get('schema')=='tiktok4743-build-v1','Unexpected build QA schema')
    need(build.get('artifacts',{}).get(ARTIFACT,{}).get('sha256')==artifact_sha,'Hiro bytes differ from build QA')
    work.mkdir(parents=True);sdk.parent.mkdir();shutil.copyfile(SDK,sdk)
    write(work/'command.json',command);(work/'command.sh').write_text(shlex.join(command)+'\n')
    write(work/'selected-patches.json',selected)
    provenance={'schema':'tiktok4743-all144-collision-input-v1','started_at':datetime.now(timezone.utc).isoformat(),
        'input':{'package':PACKAGE,'version':VERSION,'version_code':2024704030,'sha256':INPUT_SHA,'bytes':474921201},
        'upstream':{'source':'icysymmetra/tiktok-patches-for-morphe','version':'0.9.0-dev.1','sha256':UPSTREAM_SHA,'selected':upstream},
        'hiro':{'artifact':ARTIFACT,'sha256':artifact_sha,'selected':hiro},'sdk_sha256':SDK_SHA,
        'default_options_used':True,'force_compatibility':False,'continue_on_error':True,'max_heap':args.heap,
        'physical_android_tested':False,'user_128_selection_reproduced':False,'exploratory_only':True,'valid_for_release':False}
    write(work/'provenance.json',provenance)
    def run(label,cmd,timeout=3600):
        print('RUN '+label,flush=True)
        with (work/(label+'.log')).open('w') as stream:
            try:return subprocess.run(list(map(str,cmd)),stdout=stream,stderr=subprocess.STDOUT,timeout=timeout).returncode
            except subprocess.TimeoutExpired:
                stream.write('\nTIMED_OUT\n');return 124
    classes=work/'catalog-classes';classes.mkdir()
    need(run('catalog-compile',[JAVAC,'-encoding','UTF-8','-cp',sdk,'-d',classes,HERE/'PatchSelectionCatalog.java'],300)==0,'Catalog compilation failed')
    cp=os.pathsep.join(map(str,[classes,sdk]))
    for label,bundle,names in [('upstream',UPSTREAM,upstream),('hiro',artifact,hiro)]:
        names_path=work/(label+'-catalog-names.txt')
        need(run(label+'-catalog',[JAVA,'-Xmx512m','-cp',cp,'PatchSelectionCatalog',PACKAGE,VERSION,len(names),bundle,names_path],300)==0,'Catalog discovery failed: '+label)
        need(sorted(names_path.read_text().splitlines())==sorted(names),'Explicit selection differs from official loader catalog: '+label)
    code=run('apply',command)
    try:result=json.loads(result_file.read_text())
    except (OSError,ValueError):result={}
    applied=result.get('appliedPatches',[]);failed=result.get('failedPatches',[]);steps=result.get('stepResults',[])
    need(isinstance(applied,list) and all(isinstance(n,str) for n in applied),'Unexpected appliedPatches shape')
    need(isinstance(failed,list) and all(isinstance(row,dict) and isinstance(row.get('name'),str) for row in failed),'Unexpected failedPatches shape')
    failed_names=[row['name'] for row in failed]
    missing=sorted(set(selected)-set(applied)-set(failed_names));unexpected=sorted((set(applied)|set(failed_names))-set(selected))
    duplicate_applied=sorted(name for name,n in collections.Counter(applied).items() if n>1)
    duplicate_failed=sorted(name for name,n in collections.Counter(failed_names).items() if n>1)
    overlap=sorted(set(applied)&set(failed_names))
    log_text=(work/'apply.log').read_text()
    logged_counts=[int(n) for n in re.findall(r'Applying (\d+) patches',log_text)]
    logged_applied=re.findall(r'^INFO: Applied: (.+)$',log_text,re.MULTILINE)
    fatal_errors=[line for line in log_text.splitlines() if 'OutOfMemoryError' in line or 'SIGBUS' in line or 'fatal error has been detected' in line]
    steps_pass=bool(steps) and all(isinstance(x,dict) and x.get('success') is True for x in steps) and {'PATCHING','REBUILDING'}<={x.get('step') for x in steps if isinstance(x,dict)}
    unchanged=(sha(artifact)==artifact_sha and sha(UPSTREAM)==UPSTREAM_SHA and sha(SDK)==SDK_SHA
        and APK.stat().st_size==474921201 and sha(APK)==INPUT_SHA)
    passed=(code==0 and result.get('success') is True and not failed and len(applied)==144 and set(applied)==set(selected)
        and not duplicate_applied and not duplicate_failed and not overlap and not missing and not unexpected
        and steps_pass and logged_counts==[144] and result.get('packageName')==PACKAGE and result.get('packageVersion')==VERSION
        and output.is_file() and unchanged)
    classified=[]
    for row in failed:
        detail=row.get('error',row.get('reason','No failure detail supplied'))
        # The observed SDK shape uses a string; preserve any JSON-valued detail
        # without letting a non-string hide the failure during report formatting.
        detail_text=detail if isinstance(detail,str) else json.dumps(detail,ensure_ascii=False,sort_keys=True)
        classified.append({'name':row['name'],'source':'Hiro' if row['name'] in hiro else 'Metra' if row['name'] in upstream else 'unexpected',
                           'error':detail,'error_text':detail_text})
    summary={'schema':'tiktok4743-all144-collision-result-v1','outcome':'PASS_PIPELINE_ONLY' if passed else 'FAIL',
        'pipeline_all_selected_success':passed,'process_exit_code':code,'reported_top_level_success':result.get('success'),
        'max_heap':args.heap,'result_json_present':result_file.is_file(),'logged_applied_count':len(logged_applied),
        'logged_applied_patches':logged_applied,'fatal_errors':fatal_errors,
        'input_sha256':INPUT_SHA,'upstream_sha256':UPSTREAM_SHA,'hiro_artifact_sha256':artifact_sha,'inputs_unchanged':unchanged,
        'requested_count':144,'requested_metra_count':136,'requested_hiro_count':8,'logged_apply_counts':logged_counts,
        'applied_count':len(applied),'failed_count':len(failed),'applied_patches':applied,'failed_patches':classified,
        'missing_requested_patches':missing,'unexpected_result_patches':unexpected,'duplicate_applied_patches':duplicate_applied,
        'duplicate_failed_patches':duplicate_failed,'applied_failed_overlap':overlap,'step_results':steps,'steps_all_success':steps_pass,
        'output_apk_sha256':sha(output) if output.is_file() else None,'output_apk_bytes':output.stat().st_size if output.is_file() else None,
        'force_compatibility':False,'continue_on_error':True,'default_options_used':True,'exploratory_only':True,'valid_for_release':False,
        'physical_android_tested':False,'user_128_selection_reproduced':False,
        'functional_compatibility':'Not established by patch application or rebuild. Primary emitted-hook/OneBack/privacy proofs use the separately validated release configurations.',
        'note':'The explicit 144-name configuration includes every Metra entry, including optional and destructive resource choices; it is an additional collision probe, not a recommended selection or the unknown user 128-name configuration.'}
    write(work/'audit-summary.json',summary)
    (work/'failed-patches.txt').write_text('\n\n'.join('['+row['source']+'] '+row['name']+'\n'+row['error_text'] for row in classified)+'\n')
    print(json.dumps({k:summary[k] for k in ['outcome','requested_count','applied_count','failed_count','steps_all_success','physical_android_tested','user_128_selection_reproduced']},ensure_ascii=False),flush=True)
    return 0 if passed else 1

if __name__=='__main__':
    try:sys.exit(main())
    except Exception as exc:
        print('COLLISION_PROBE_ABORTED: '+str(exc),file=sys.stderr);sys.exit(2)
