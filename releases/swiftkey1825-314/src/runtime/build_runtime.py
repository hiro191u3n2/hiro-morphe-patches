#!/usr/bin/env python3
"""Rebuild and verify a minimal dictionary-capacity overlay against published 1.8.24.

The final runtime keeps 91 types; only three method bodies and two integer
constants change. Rebuilds never modify downloaded dictionaries or input MPPs.
"""
from __future__ import annotations
import argparse, hashlib, json, os, shutil, subprocess, zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
EXT='extensions/swiftkey_japanese.mpe'
BASE_PIN='494565fcceffcdbcb6df0e5885cf537bd5e7b8ffa6d53f4b78f603b3ef38ca55'
APK_PIN='e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7'
PINS={'android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b','apktool.jar':'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423','r8.jar':'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa','morphe.jar':'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58'}
METHODS=sorted(['Lhiro/swiftkey/WordCandidates$1;->run()V','Lhiro/swiftkey/WordCandidates$Index;->fromBytes([B)Lhiro/swiftkey/WordCandidates$Index;','Lhiro/swiftkey/WordCandidates$Index;->words(I)[Ljava/lang/String;'])
FIELDS=sorted(['Lhiro/swiftkey/WordCandidates;->MAX_ASSET_BYTES:I','Lhiro/swiftkey/WordCandidates;->MAX_ROWS:I'])
def sha(b):return hashlib.sha256(b).hexdigest()
def require(v,m):
    if not v:raise RuntimeError(m)
def run(args,log):
    p=subprocess.run([str(v) for v in args],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    log.parent.mkdir(parents=True,exist_ok=True);log.write_text(p.stdout)
    require(p.returncode==0,'Command failed: '+str(log)+'\n'+p.stdout[-4000:]);return p.stdout
def javac(args,log):return run(['java','com.sun.tools.javac.Main',*args],log)
def json_write(path,data):path.write_text(json.dumps(data,ensure_ascii=False,indent=2,sort_keys=True)+'\n')
def smali(path):return {p.relative_to(path).as_posix():p.read_bytes() for p in sorted(path.rglob('*.smali'))}
def zip_add(z,n,b):
    i=zipfile.ZipInfo(n,(2026,10,29,0,0,0));i.compress_type=zipfile.ZIP_DEFLATED;i.external_attr=0o100644<<16;z.writestr(i,b)
def compile_mode(mode,tools,work,dump):
    d=work/mode;c=d/'classes';c.mkdir(parents=True)
    src=ROOT/('baseline' if mode=='baseline' else 'src')/'hiro/swiftkey/WordCandidates.java'
    javac(['--release','8','-encoding','UTF-8','-cp',tools/'android.jar','-d',c,*sorted((ROOT/'stubs').rglob('*.java')),src],d/'javac.log')
    with zipfile.ZipFile(d/'word.jar','w') as z,zipfile.ZipFile(d/'stubs.jar','w') as stubs:
        for p in sorted(c.rglob('*.class')):
            name=p.relative_to(c).as_posix();zip_add(z if name.startswith('hiro/swiftkey/WordCandidates') else stubs,name,p.read_bytes())
    (d/'dex').mkdir();run(['java','-cp',tools/'r8.jar','com.android.tools.r8.D8','--debug','--min-api','26','--lib',tools/'android.jar','--classpath',d/'stubs.jar','--output',d/'dex',d/'word.jar'],d/'d8.log')
    run(dump+[d/'dex/classes.dex',d/'smali'],d/'dump.log')
    # Host doubles override only platform IO; WordCandidates bytecode is exactly the compiled production source.
    javac(['--release','8','-encoding','UTF-8','-cp',str(c)+os.pathsep+str(tools/'android.jar'),'-d',c,*sorted((ROOT/'tests/host_stubs').rglob('*.java')),ROOT/'tests/DictionaryExpansionTest.java'],d/'host-javac.log')
    return d
def build(baseline_mpp,dictionary,tools,work,output,reference_apk=None):
    for name,pin in PINS.items():require(sha((tools/name).read_bytes())==pin,'Tool pin differs: '+name)
    require(not output.is_relative_to(work),'Output cannot be inside the reset work directory')
    if work.exists():shutil.rmtree(work)
    work.mkdir(parents=True);output.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(baseline_mpp) as z:old=z.read(EXT);words=z.read('swiftkey/words_ja.tsv')
    require(sha(old)==BASE_PIN,'Baseline must be published SwiftKey1.8.24 runtime')
    (work/'baseline.mpe').write_bytes(old);(work/'baseline.tsv').write_bytes(words)
    tc=work/'tool_classes';tc.mkdir()
    javac(['-cp',tools/'apktool.jar','-d',tc,ROOT/'tools_java/Dump1814.java'],work/'javac-dump.log')
    javac(['-cp',tools/'morphe.jar','-d',tc,ROOT/'tools_java/MergeDictionaryExpansion.java'],work/'javac-merge.log')
    dump=['java','-cp',str(tc)+os.pathsep+str(tools/'apktool.jar'),'Dump1814']
    run(dump+[work/'baseline.mpe',work/'before'],work/'dump-before.log')
    before=smali(work/'before');require(len(before)==91,'Runtime classes differ')
    bd=compile_mode('baseline',tools,work,dump);fd=compile_mode('fixed',tools,work,dump)
    rebuilt=smali(bd/'smali');fixed=smali(fd/'smali')
    names={'hiro/swiftkey/WordCandidates.smali','hiro/swiftkey/WordCandidates$Index.smali','hiro/swiftkey/WordCandidates$1.smali'}
    require(rebuilt.keys()==names and fixed.keys()==names,'Word class inventory differs')
    require(all(rebuilt[n]==before[n] for n in names),'Baseline Java does not exactly reconstruct published executable Word classes')
    oldsrc=(ROOT/'baseline/hiro/swiftkey/WordCandidates.java').read_text()
    expect=oldsrc.replace('MAX_ASSET_BYTES = 48 * 1024 * 1024','MAX_ASSET_BYTES = 192 * 1024 * 1024').replace('MAX_ROWS = 1000000','MAX_ROWS = 3000000').replace('offset <= end && result.size() < MAX_WORDS_PER_READING','offset <= end')
    expect=expect.replace('// Class/API incompatibility leaves the native candidate list intact.','// Class/API incompatibility leaves the native candidate list intact.\n                } catch (OutOfMemoryError ignored) {\n                    // This optional large dictionary must not terminate the native keyboard.')
    require(expect==(ROOT/'src/hiro/swiftkey/WordCandidates.java').read_text(),'Production source delta exceeds reviewed bounds/parser changes')
    java=os.environ.get('MORPHE_JAVA','java')
    run([java,'-cp',str(tc)+os.pathsep+str(tools/'morphe.jar'),'MergeDictionaryExpansion',work/'baseline.mpe',fd/'dex/classes.dex',output/'runtime.mpe',output/'merge-report.json'],work/'merge.log')
    merge=json.loads((output/'merge-report.json').read_text());require(merge['changed_methods']==METHODS and merge['changed_fields']==FIELDS,'Unexpected runtime deltas')
    run(dump+[output/'runtime.mpe',work/'after'],work/'dump-after.log');after=smali(work/'after')
    require(before.keys()==after.keys() and all(before[n]==after[n] for n in before if n not in names),'Unrelated runtime class changed')
    reports={}
    for mode,dic in [('baseline',work/'baseline.tsv'),('fixed',dictionary)]:
        stdout=run(['java','-Xmx512m','-cp',str(work/mode/'classes')+os.pathsep+str(tools/'android.jar'),'hiro.swiftkey.DictionaryExpansionTest',dic,work/'baseline.tsv',mode],work/(mode+'-host.log'))
        r=json.loads(stdout.strip().splitlines()[-1]);r.pop('elapsed_ms',None)
        require(r['status']=='PASSED' and r['missing_candidates']==0 and r['unexpected_candidates']==0 and r['duplicate_pairs']==0,'Coverage failed');reports[mode]=r
    runtime_sha=sha((output/'runtime.mpe').read_bytes())
    abi_source=ROOT/'tests/AuditDictionaryAbi.java';abi=None
    if reference_apk:
        require(sha(reference_apk.read_bytes())==APK_PIN,'Reference APK pin differs')
        cp=str(tools/'morphe.jar')+os.pathsep+str(tools/'r8.jar')
        javac(['-cp',cp,'-d',tc,abi_source],work/'javac-abi.log')
        run([java,'-Xmx1024m','-cp',str(tc)+os.pathsep+cp,'AuditDictionaryAbi',reference_apk,output/'runtime.mpe',tools/'android.jar',output/'native-abi.json'],work/'abi.log')
        abi=json.loads((output/'native-abi.json').read_text());abi['verifier_source_sha256']=sha(abi_source.read_bytes());json_write(output/'native-abi.json',abi)
    else:
        abi=json.loads((ROOT/'tests/native-abi.json').read_text())
    require(abi['status']=='PASSED' and abi['extension_sha256']==runtime_sha and abi['apk_sha256']==APK_PIN and abi['android_jar_sha256']==PINS['android.jar'] and abi['verifier_source_sha256']==sha(abi_source.read_bytes()),'Native ABI evidence does not match built runtime')
    require(not abi['unresolved'] and not abi['inaccessible'] and not abi['incompatible_opcodes'],'ABI error')
    json_write(output/'native-abi.json',abi)
    report={'status':'PASSED','baseline_extension_sha256':BASE_PIN,'runtime_sha256':runtime_sha,'baseline_java_matches_published_dex':True,'production_source_delta_reviewed':True,'merge':merge,'word_coverage':{'baseline':reports['baseline'],'fixed':reports['fixed'],'all_exact_entries_reachable':True},'host_tests_passed':True,'native_abi_verified':True,'abi_report_sha256':sha((output/'native-abi.json').read_bytes()),'abi_replayed':bool(reference_apk),'device_tested':False,'original_apk_apply_tested':False,'capacity':{'max_asset_bytes':192*1024*1024,'max_rows':3000000,'exact_row_candidate_cap':None,'prefix_suggestions':12,'prefix_rows':64,'prefix_ranks':32},'source_sha256':{p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in sorted(ROOT.rglob('*')) if p.is_file() and p.suffix in ('.java','.py') and '__pycache__' not in p.parts}}
    json_write(output/'runtime-report.json',report);print(json.dumps(report,ensure_ascii=False));return report
def main():
    p=argparse.ArgumentParser();p.add_argument('--baseline-mpp',type=Path,required=True);p.add_argument('--dictionary',type=Path,required=True);p.add_argument('--tools',type=Path,required=True);p.add_argument('--work',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--reference-apk',type=Path);a=p.parse_args();build(a.baseline_mpp.resolve(),a.dictionary.resolve(),a.tools.resolve(),a.work.resolve(),a.output.resolve(),a.reference_apk.resolve() if a.reference_apk else None)
if __name__=='__main__':main()
