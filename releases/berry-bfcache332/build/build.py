#!/usr/bin/env python3
"""Reproducibly replace Berry structural providers in immutable Hiro bundle 331.

All source inputs remain separate from the published baseline. Existing queue,
contract classes, four unchanged runtime extensions and every unrelated JVM/DEX provider are
preserved; the BFCache runtime alone is replaced. This script proves packaging and provider loading, not device behavior.
"""
from pathlib import Path, PurePosixPath
import argparse, hashlib, json, os, subprocess, zipfile

BASE_SHA='9017b1a789afd37d78b2011050166a7d7c1101304ea2b4c5080f663d4ac3666a'
TOOLS={'classpath':'fe4bc92815bb14976150e4ec83e4985932b46d1d47edec75d37604481572e864',
       'ecj':'74c93172475fee0cd4e6eefb44233516dd40525642da35d009146d97ea598229',
       'd8':'38a9ffaaebbd17d1514e3ecbff5a85b989bb13c551b9e224c7409514eea84ff5'}
PREFIXES=('app/hiro/berry/','app/hiro/berryshare/','app/hiro/berryzoom/','app/hiro/berrybfcache/')
QUEUE='app/hiro/berryqueue/'
EXTENSIONS=('extensions/berry_foreground_tabs.mpe','extensions/berry_browser_visibility.mpe',
            'extensions/berry_onehand_zoom.mpe','extensions/berry_messagequeue.mpe','extensions/berry_bfcache_restore.mpe')
RUNTIME='extensions/berry_bfcache_restore.mpe'
MF='META-INF/MANIFEST.MF'
COMBINED='Hiro_Morphe_Patches_v1.0.332.mpp'
SINGLE='Berry_Browser_v1.4.1.mpp'

def require(value,message):
    if not value: raise RuntimeError(message)
def sha(raw): return hashlib.sha256(raw).hexdigest()
def file_row(path):
    raw=path.read_bytes();return {'bytes':len(raw),'sha256':sha(raw)}
def json_bytes(value): return (json.dumps(value,ensure_ascii=False,indent=2,sort_keys=True)+'\n').encode()
def archive(path):
    with zipfile.ZipFile(path) as z:
        require(len(z.namelist())==len(set(z.namelist())) and z.testzip() is None,'Invalid ZIP '+path.name)
        for name in z.namelist():
            p=PurePosixPath(name);require(not p.is_absolute() and '..' not in p.parts and '\\' not in name,'Unsafe ZIP entry')
        return {n:z.read(n) for n in z.namelist() if not n.endswith('/')}
def write_zip(path,entries):
    with zipfile.ZipFile(path,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for name,raw in sorted(entries.items(),key=lambda row:(row[0]!=MF,row[0])):
            info=zipfile.ZipInfo(name,(2026,11,2,0,0,0));info.create_system=3;info.external_attr=0o100644<<16
            z.writestr(info,raw,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
def headers(raw):
    rows=[]
    for line in raw.replace(b'\r\n',b'\n').split(b'\n'):
        if line.startswith(b' '):require(bool(rows),'Manifest continuation missing base');rows[-1]+=line[1:]
        elif line:rows.append(line)
    pairs=[r.decode().split(': ',1) for r in rows]
    require(all(len(r)==2 for r in pairs) and len(pairs)==len(dict(pairs)),'Invalid manifest')
    return dict(pairs)
def manifest(fields):
    output=bytearray()
    for key,value in fields.items():
        require(not any(c in value for c in '\r\n\0'),'Invalid manifest header')
        row=(key+': '+value).encode()
        while len(row)>70:
            end=70
            while row[end]&0xc0==0x80:end-=1
            output+=row[:end]+b'\r\n';row=b' '+row[end:]
        output+=row+b'\r\n'
    raw=bytes(output)+b'\r\n';require(headers(raw)==fields,'Manifest roundtrip failed');return raw
def contract(name): return name.rsplit('/',1)[-1] in ('MethodContract.class','OriginalContracts.class')
def replaceable(name):return name.endswith('.class') and name.startswith(PREFIXES) and not contract(name)
def berry_jvm(name):return name.endswith('.class') and name.startswith(PREFIXES+(QUEUE,))
def classes(folder):return {p.relative_to(folder).as_posix():p.read_bytes() for p in sorted(folder.rglob('*.class'))}

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for key in ('base','src','java','classpath','ecj','d8','runtime','out'):p.add_argument('--'+key,type=Path,required=True)
    a=p.parse_args()
    for key in vars(a):setattr(a,key,getattr(a,key).resolve())
    require(sha(a.base.read_bytes())==BASE_SHA,'Baseline hash differs')
    for name,pin in TOOLS.items():require(sha(getattr(a,name).read_bytes())==pin,'Tool hash differs: '+name)
    require(not a.out.exists() or not any(a.out.iterdir()),'Output directory must be empty')
    a.out.mkdir(parents=True,exist_ok=True);work=a.out/'work';work.mkdir()
    def run(args,name):
        result=subprocess.run(list(map(str,args)),capture_output=True,text=True)
        (work/name).write_text(result.stdout+result.stderr)
        require(result.returncode==0,name+' failed:\n'+(result.stdout+result.stderr)[-15000:])
        return result.stdout.strip()
    version=run([a.java,'-version'],'java.log')
    java_log=(work/'java.log').read_text();require('21.0.13' in java_log,'Use reviewed JRE/JDK21.0.13')
    base=archive(a.base);fields=headers(base[MF]);require(fields['Version']=='1.0.331','Wrong baseline version')
    require(all(n in base for n in EXTENSIONS),'Runtime extension missing')
    runtime=a.runtime.read_bytes()
    require(runtime[:4]==b'dex\n' and len(runtime)>112,'BFCache runtime must be a DEX extension')
    require(runtime!=base[RUNTIME],'BFCache runtime was not changed')
    classpath_jar=work/'baseline-jvm.jar'
    write_zip(classpath_jar,{n:b for n,b in base.items() if n.endswith('.class')})
    source_files=sorted(a.src.rglob('*.java'));require(source_files,'No provider sources')
    source_pins={path.relative_to(a.src).as_posix():file_row(path) for path in source_files}
    for rel in source_pins:
        require(rel.startswith(PREFIXES),'Source exceeds Berry namespaces: '+rel)
        require(rel.rsplit('/',1)[-1] not in ('MethodContract.java','OriginalContracts.java'),'Canonical contract sources must be retained')
    for name in ('classes','dex','tool-classes','compile-stub-src','compile-stub-classes'):(work/name).mkdir()
    build_dir=Path(__file__).resolve().parent
    run([a.java,'-jar',a.ecj,'-source','11','-target','11','-encoding','UTF-8','-cp',a.classpath,
         '-d',work/'tool-classes',build_dir/'MergeDex.java',build_dir/'Metadata.java',build_dir/'VerifyRuntime.java'],'compile-tools.log')
    tool_cp=str(work/'tool-classes')+os.pathsep+str(a.classpath)
    abi=json.loads(run([a.java,'-cp',tool_cp,'Metadata','--abi'],'app-target-abi.log'))
    runtime_audit=json.loads(run([a.java,'-cp',tool_cp,'VerifyRuntime',a.runtime],'runtime-audit.log'))
    require(runtime_audit['result']=='PASS_RUNTIME_DEX','Runtime DEX audit failed')
    require(abi['result']=='PASS','Baseline AppTarget ABI verification failed')
    # Compile-only declaration exposes the synthetic baseline 3-argument ABI.
    # This class is never input to D8 or included in either final MPP.
    stub=work/'compile-stub-src/app/morphe/patcher/patch/AppTarget.java'
    stub.parent.mkdir(parents=True,exist_ok=True)
    stub.write_text('package app.morphe.patcher.patch; public final class AppTarget { public AppTarget(String version, boolean experimental, Integer versionCode) {} }\n')
    run([a.java,'-jar',a.ecj,'-source','8','-target','8','-encoding','UTF-8','-d',work/'compile-stub-classes',stub],'compile-stub.log')
    stub_classes=classes(work/'compile-stub-classes')
    require(set(stub_classes)=={'app/morphe/patcher/patch/AppTarget.class'},'Unexpected compile-only stub output')
    stub_jar=work/'compile-only-app-target.jar';write_zip(stub_jar,stub_classes)
    cp=os.pathsep.join(map(str,(stub_jar,a.classpath,classpath_jar)))
    run([a.java,'-jar',a.ecj,'-source','8','-target','8','-g:source,lines','-encoding','UTF-8',
         '-cp',cp,'-d',work/'classes',*source_files],'compile-providers.log')
    fresh=classes(work/'classes');require(fresh and all(replaceable(n) for n in fresh),'Unexpected compiled namespace')
    require(all(n not in fresh for n in base if contract(n)),'Contract class replaced')
    fresh_jar=work/'fresh-jvm.jar';write_zip(fresh_jar,fresh)
    run([a.java,'-cp',a.d8,'com.android.tools.r8.D8','--release','--min-api','26','--classpath',a.classpath,
         '--classpath',classpath_jar,'--output',work/'dex',fresh_jar],'compile-dex.log')
    require([f.name for f in (work/'dex').glob('*.dex')]==['classes.dex'],'Unexpected DEX output')
    (work/'baseline.dex').write_bytes(base['classes.dex'])
    run([a.java,'-cp',tool_cp,'MergeDex',work/'baseline.dex',work/'dex/classes.dex',work/'combined.dex',
         work/'standalone.dex',work/'dex-preservation.json'],'merge.log')
    dex_qa=json.loads((work/'dex-preservation.json').read_text())
    require(dex_qa['result']=='PASS_DEX_DEFINITION_PRESERVATION','DEX preservation failed')
    retained={n:b for n,b in base.items() if not replaceable(n)}
    combined={**retained,**fresh,'classes.dex':(work/'combined.dex').read_bytes(),RUNTIME:runtime}
    fields.update(Version='1.0.332',Timestamp='2026-11-02T00:00:00',
                  Description='Berry Browser v1.4.1: repair BFCache restore completion stalls and recover from lost asynchronous callbacks. Structural all-version resolution retained. Physical device behavior is unverified.')
    combined[MF]=manifest(fields)
    single={n:b for n,b in combined.items() if berry_jvm(n) or n in EXTENSIONS}
    sf=dict(fields);sf.update(Name='Berry Browser',Version='1.4.1')
    single.update({MF:manifest(sf),'classes.dex':(work/'standalone.dex').read_bytes()})
    unchanged=[n for n in base if n not in (MF,'classes.dex',RUNTIME) and not replaceable(n)]
    require(all(combined[n]==base[n] for n in unchanged),'Unrelated ZIP entries changed')
    require(not any(n.startswith('app/morphe/') for n in combined),'Compile-only patcher stub leaked into combined MPP')
    require(not any(n.startswith('app/morphe/') for n in single),'Compile-only patcher stub leaked into standalone MPP')
    require(all(combined[n]==base[n] for n in EXTENSIONS if n!=RUNTIME),'Unrelated runtime extension bytes changed')
    require(combined[RUNTIME]==single[RUNTIME]==runtime,'New runtime not packaged identically')
    removed=sorted(set(base)-set(combined));added=sorted(set(combined)-set(base))
    require(all(replaceable(n) for n in removed+added),'Unexpected entry addition/removal')
    output_meta={}
    for name,entries in ((COMBINED,combined),(SINGLE,single)):
        write_zip(a.out/name,entries);require(archive(a.out/name)==entries,'MPP archive roundtrip failed')
        result=run([a.java,'-cp',tool_cp,'Metadata',a.out/name],name+'.metadata.log')
        output_meta[name]=json.loads(result)
    require(all(file_row(a.src/rel)==pin for rel,pin in source_pins.items()),'Provider source changed during build')
    require(a.runtime.read_bytes()==runtime,'BFCache runtime changed during build')
    report={'schema':'berry-bfcache332-build-qa-v1','result':'PASS','baseline_sha256':BASE_SHA,
            'bundle_version':'1.0.332','berry_version':'1.4.1','physical_device_tested':False,
            'artifacts':{name:file_row(a.out/name) for name in (COMBINED,SINGLE)},'source_files':source_pins,
            'tool_sha256':TOOLS,'jvm_provider_strategy':'Direct ECJ JVM classes; D8 synthetic classes remain only in DEX',
            'combined_entries':len(combined),'baseline_entries':len(base),'standalone_entries':len(single),
            'removed_jvm_entries':removed,'added_jvm_entries':added,
            'unchanged_entry_count':len(unchanged),'all_unrelated_jvm_entries_preserved':True,
            'four_unrelated_extensions_identical':True,'runtime_extensions':{n:sha(combined[n]) for n in EXTENSIONS},
            'previous_bfcache_runtime_sha256':sha(base[RUNTIME]),'runtime_dex_audit':runtime_audit,
            'dex_preservation':dex_qa,'metadata':output_meta,'baseline_app_target_abi':abi,
            'compile_only_stub_excluded_from_outputs':True}
    (a.out/'QA_build332.json').write_bytes(json_bytes(report))
    print(json.dumps({'result':'PASS_BUILD','artifacts':report['artifacts']},ensure_ascii=False),flush=True)
if __name__=='__main__':main()
