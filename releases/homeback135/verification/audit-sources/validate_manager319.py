#!/usr/bin/env python3
"""Fresh Manager237 application and emitted-APK QA. Requires a frozen319 MPP.

No artifact build or application is performed by --preflight. All APKs remain
private scratch evidence; this runner does not claim physical Android execution.
"""
from pathlib import Path
import argparse, hashlib, json, os, re, subprocess, zipfile

ROOT = Path(__file__).resolve().parent
WORK = ROOT.parent
JAVA = WORK/'jdk134/jdk-21.0.8+9/bin/java'
JAVAC = JAVA.with_name('javac')
NAME = '戻る1回で終了・履歴削除'
CASES = ('hiro-only', 'piko64-hiro319', 'hiro319-piko64')
PINS = json.loads((ROOT/'prepared-inputs.json').read_text())


def check(value, why):
    if not value:
        raise RuntimeError(why)


def identity(path):
    with path.open('rb') as source:
        return {'bytes': path.stat().st_size,
                'sha256': hashlib.file_digest(source, 'sha256').hexdigest()}


def write_json(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2, sort_keys=True)+'\n')


def pinned(name):
    data = PINS[name]
    path = WORK/data['path']
    check(identity(path) == {k:data[k] for k in ('bytes','sha256')}, 'Pinned input changed: '+name)
    return path


def inventory(path):
    entries = {}
    with zipfile.ZipFile(path) as archive:
        check(len(archive.namelist()) == len(set(archive.namelist())), 'Duplicate ZIP entry')
        for entry in archive.infolist():
            name = entry.filename
            if (entry.is_dir() or name in ('AndroidManifest.xml','resources.arsc')
                    or re.fullmatch(r'classes(?:[0-9]+)?\.dex',name)
                    or re.fullmatch(r'META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))',name,re.I)):
                continue
            entries[name] = hashlib.sha256(archive.read(entry)).hexdigest()
    return entries


def run_logged(command, log):
    with log.open('w') as output:
        result = subprocess.run(list(map(str,command)),stdout=output,stderr=subprocess.STDOUT)
    check(result.returncode == 0, str(log)+' failed: '+log.read_text()[-2500:])
    return log.read_text()


def source_ids(root):
    return {p.relative_to(root).as_posix():identity(p)['sha256']
            for p in sorted(root.rglob('*')) if p.is_file()
            and '__pycache__' not in p.parts and p.suffix in ('.java','.py','.dex')}


def metadata(mpp, output):
    helper = pinned('selection_helper').parent
    host = pinned('host')
    text = run_logged([JAVA,'-XX:-UsePerfData','-Xmx512m','-cp',os.pathsep.join(map(str,[helper,host])),
                       'Manager237Selections',mpp,'com.instagram.android','452.1.0.53.64'],output)
    return json.loads(text)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--preflight',action='store_true')
    parser.add_argument('--mpp',type=Path)
    parser.add_argument('--mpp-sha256')
    parser.add_argument('--mpp-bytes',type=int)
    parser.add_argument('--build-dir',type=Path)
    parser.add_argument('--build-proof',type=Path)
    parser.add_argument('--source-root',type=Path,default=WORK/'repo135/releases/homeback135/src')
    parser.add_argument('--run-label',default='final')
    parser.add_argument('--case',choices=CASES,action='append')
    args=parser.parse_args()
    inputs={name: {'path':str(pinned(name).relative_to(WORK)),**identity(pinned(name))} for name in PINS}
    check(JAVA.is_file() and JAVAC.is_file(),'Pinned JDK21 missing')
    if args.preflight:
        print(json.dumps({'result':'READY','input_count':len(inputs),'artifact_application_started':False}))
        return
    for name in ('mpp','mpp_sha256','mpp_bytes','build_dir','build_proof'):
        check(getattr(args,name) is not None,'Frozen build argument required: '+name)
    check(re.fullmatch(r'[0-9a-f]{64}',args.mpp_sha256) is not None,'Full frozen MPP SHA required')
    args.mpp=args.mpp.resolve();args.build_dir=args.build_dir.resolve()
    args.build_proof=args.build_proof.resolve();args.source_root=args.source_root.resolve()
    expected={'bytes':args.mpp_bytes,'sha256':args.mpp_sha256}
    build=json.loads(args.build_proof.read_text())
    check(build['version']=='1.1.28' and build['bundle_version']=='1.0.319','Wrong build proof version')
    check(build['artifacts'].get(args.mpp.name)==expected,'Frozen MPP differs from build proof')
    build_identity=identity(args.build_proof)
    def frozen():
        check(identity(args.mpp)==expected,'Frozen MPP changed')
        check(identity(args.build_proof)==build_identity,'Build proof changed')
        check(source_ids(args.source_root)==build['sources'],'Frozen source inventory changed')
    frozen()
    folder=ROOT/args.run_label
    check(folder.parent==ROOT and not folder.exists(),'Use a fresh direct-child run label')
    folder.mkdir()
    resource_classes=folder/'resource-audit-classes';resource_classes.mkdir()
    sdk=pinned('morphe_sdk')
    run_logged([JAVAC,'-J-XX:-UsePerfData','-encoding','UTF-8','-cp',sdk,'-d',resource_classes,
                pinned('manifest_audit_source'),pinned('resources_audit_source')],folder/'resource-audits-compile.txt')
    cp=os.pathsep.join(map(str,[args.build_dir/'audit',args.build_dir/'patch',sdk]))
    rcp=os.pathsep.join(map(str,[resource_classes,sdk]))
    hiro=metadata(args.mpp,folder/'hiro-selections.json')
    piko=metadata(pinned('piko_mpp'),folder/'piko-selections.json')
    hrows=[r for r in hiro['patches'] if r['default'] and r['version_compatible']]
    prows=[r for r in piko['patches'] if r['default'] and r['version_compatible']]
    check([r['name'] for r in hrows]==[NAME],'Unexpected Hiro defaults')
    check(len(piko['patches'])==68 and len(prows)==64,'Unexpected Piko default selection')
    prior=json.loads((WORK/'QA134/piko64-baseline/proof.json').read_text())
    check(prior['success'] and prior['output']==identity(pinned('piko_baseline')),'Piko baseline identity')
    check([r['key'] for r in prows]==prior['selected_keys'],'Piko baseline selected keys differ')
    check({r['key']:r['option_defaults'] for r in prows}==prior['option_defaults'],'Piko baseline defaults differ')
    results=[]
    for case in args.case or CASES:
        frozen()
        current=folder/case;current.mkdir()
        sources=[(args.mpp,hrows)]
        if case=='piko64-hiro319':sources.insert(0,(pinned('piko_mpp'),prows))
        elif case=='hiro319-piko64':sources.append((pinned('piko_mpp'),prows))
        selected=[row['name'] for _,rows in sources for row in rows]
        count=1 if case=='hiro-only' else 65
        check(len(selected)==len(set(selected))==count,'Unexpected selected patch names')
        apk=pinned('original_apk');host=pinned('host');manager=pinned('manager')
        output=current/'patched.apk'
        request={'packageName':'com.instagram.android','inputFile':str(apk),'outputFile':str(output),
                 'mergedApkFile':str(current/'merged.apk'),'temporaryDir':str(current/'temp'),
                 'workspaceDir':str(current/'workspace'),
                 'bundles':[{'uid':i+1,'patchesFile':str(mpp),'patches':[r['key'] for r in rows],'options':{}}
                            for i,(mpp,rows) in enumerate(sources)],'stripUnusedNativeLibs':False,
                 'device':{'supportedAbis':['arm64-v8a'],'localeTags':['ja-JP'],'densityDpi':480}}
        write_json(current/'request.json',request)
        (current/'commands.jsonl').write_text(json.dumps({'type':'run','requestFile':str(current/'request.json')})+'\n')
        print('APPLY',case,flush=True)
        with (current/'process.log').open('w') as log:
            run=subprocess.run([str(JAVA),'-XX:-UsePerfData','-Xmx3g','-cp',os.pathsep.join(map(str,[host,manager])),
                'app.morphe.manager.patcher.host.HostMainKt',str(current/'commands.jsonl'),str(current/'events.jsonl')],
                stdout=log,stderr=subprocess.STDOUT)
        events=[json.loads(line) for line in (current/'events.jsonl').read_text().splitlines() if line.strip()]
        finished=[e for e in events if e.get('type')=='finished'];failures=[e for e in events if e.get('type')=='patch_failed']
        applied=[e['name'] for e in events if e.get('type')=='patch'];counts=[e['count'] for e in events if e.get('type')=='patches_done']
        success=(run.returncode==0 and len(finished)==1 and events[-1]==finished[0] and finished[0].get('failure') is None
                 and not failures and counts==[count] and len(applied)==count and set(applied)==set(selected)
                 and output.is_file() and zipfile.is_zipfile(output))
        application={'success':success,'process_exit_code':run.returncode,'physical_android_tested':False,
            'requested_source_bundle_order':[mpp.name for mpp,_ in sources],'selected_count':count,
            'selected_bundles':[{'artifact':mpp.name,**identity(mpp),'selected_keys':[r['key'] for r in rows],
                                'option_overrides':{},'option_defaults':{r['key']:r['option_defaults'] for r in rows}}
                               for mpp,rows in sources],'requested_patch_names':selected,'applied_patch_names':applied,
            'finished':finished,'patch_failures':failures,'patches_done_counts':counts,
            'request':identity(current/'request.json'),'events':identity(current/'events.jsonl'),
            'output':identity(output) if output.exists() else None}
        write_json(current/'application-proof.json',application)
        check(success,'Manager application failed; see '+str(current/'application-proof.json'))
        frozen();before=identity(output)
        base=apk if case=='hiro-only' else pinned('piko_baseline')
        manifest_args=[base,output] if case=='hiro-only' else ['--preserve-baseline',base,output]
        resource_args=[apk,pinned('hiro318_baseline'),output] if case=='hiro-only' else ['--preserve-baseline',base,output]
        synthetic=current/'native-home-synthetic.dex'
        check(synthetic.suffix=='.dex' and synthetic!=output and not synthetic.exists(),'Unique synthetic DEX path')
        audits=[('initial-profile','2g',cp,'Instagram135InitialProfileAudit',[apk,output]),
                ('native-home','2g',cp,'Instagram134NativeHomeAudit',[base,synthetic,output]),
                ('profile-link','2g',cp,'Instagram130ProfileLinkAudit',['output',base,output]),
                ('manifest','2g',rcp,'InstagramManifestAudit',manifest_args),
                ('resources','2g',rcp,'InstagramResourcesAudit',resource_args),
                ('whole-apk','3g',cp,'ActualInstagram452Audit',[base,output,args.mpp])]
        proofs={}
        for audit,heap,classpath,main,values in audits:
            print('AUDIT',case,audit,flush=True)
            command=[JAVA,'-XX:-UsePerfData','-ea','-Xmx'+heap,'-cp',classpath,main,*values]
            log=current/(audit+'-audit.txt');text=run_logged(command,log)
            passes=[line for line in text.splitlines() if line.startswith('PASS ')]
            check(passes,'No PASS result from '+main)
            check(identity(output)==before,'Audit mutated output APK')
            frozen()
            for name in PINS:pinned(name)
            proofs[audit]={'result':'PASS','summary':passes,'log':identity(log),'command':list(map(str,command))}
        old,new=inventory(base),inventory(output)
        changed=sorted(n for n in old.keys()|new.keys() if old.get(n)!=new.get(n))
        check(not changed,'Unexpected ZIP entry delta: '+repr(changed[:12]))
        result={'result':'PASS','case':case,'physical_android_tested':False,'input_baseline':identity(base),
                'output':before,'mpp':expected,'application':application,'audits':proofs,
                'unchanged_zip_entries':len(old),'native_libraries_byte_identical':True,
                'zip_entry_exclusions':['DEX','AndroidManifest.xml','resources.arsc','signature metadata'],
                'input_output_artifact_source_hashes_checked_after_each_audit':True}
        write_json(current/'preservation-proof.json',result);results.append(result)
        print('PASS',case,json.dumps(before),flush=True)
    frozen()
    summary={'schema':'homeback135-manager-final-v1','result':'PASS','physical_android_tested':False,
             'mpp':expected,'build_proof':build_identity,'frozen_source_files':len(build['sources']),
             'pinned_inputs':inputs,'cases':results,
             'limitations':['No physical Android execution.','Official Piko default64 selections only.',
                            'Manager source-bundle order reversed; actual patch scheduling is recorded, not assumed.']}
    write_json(folder/'summary.json',summary)
    print('PASS FINAL',str(folder/'summary.json'),identity(folder/'summary.json')['sha256'],flush=True)

if __name__=='__main__':main()
