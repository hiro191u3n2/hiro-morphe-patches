#!/usr/bin/env python3
"""Build review-count preload/freeze MPP; preserve supplied app and bundle code."""
from pathlib import Path
import argparse, hashlib, json, os, re, subprocess, sys, zipfile

ROOT=Path(__file__).resolve().parent
BASELINE=json.loads((ROOT.parent/'publication/baseline.json').read_text())
VERSION,BASE_VERSION,BUNDLE_VERSION='1.0.5',BASELINE['version'],'1.0.353'
BASE_SHA=BASELINE['sha256']
SUPPLIED_APK_SHA='8c3bbe749856dce395508091a74841133bf044934d32cf8dd2ed47f11a217059'
TOOLS={
 'morphe.jar':'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
 'morphe-2.2.jar':'855dd2d6fa5ed997a5884a44f8807c9b5bc95e69b5ebd58dfd7e8b897c86e2d7',
 'android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
 'r8.jar':'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa'}
PREFIX='app/hiro/amazonreviews/patches/'
EXT='extensions/amazon_review_sort.mpe'
MF='META-INF/MANIFEST.MF'
PATCH_NAME='商品検索を評価件数の多い順に表示'
PACKAGE='com.amazon.mShop.android.shopping'
APP_VERSION='32.19.0.100'
SINGLE='Amazon_Review_Count_v1.0.5.mpp'
BUNDLE='Hiro_Morphe_Patches_v1.0.353.mpp'
QA_NAME='QA_Amazon_Review_Count_v1.0.5.json'

def require(ok,message):
    if not ok: raise RuntimeError(message)
def sha(path): return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def archive(path):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None and len(z.namelist())==len(set(z.namelist())),'Invalid ZIP')
        return {n:z.read(n) for n in z.namelist() if not n.endswith('/')}
def headers(raw):
    rows=[]
    for line in raw.replace(b'\r\n',b'\n').split(b'\n'):
        if line.startswith(b' '): rows[-1]+=line[1:]
        elif line: rows.append(line)
    return dict(row.decode().split(': ',1) for row in rows)
def manifest(fields):
    lines=[]
    for k,v in fields.items():
        require(isinstance(v,str) and not any(c in v for c in '\r\n\0'),'Invalid manifest field')
        raw,prefix=(k+': '+v).encode(),b''
        while len(prefix)+len(raw)>72:
            cut=72-len(prefix)
            while raw[cut]&192==128:cut-=1
            lines.append(prefix+raw[:cut]);raw,prefix=raw[cut:],b' '
        lines.append(prefix+raw)
    raw=b'\r\n'.join(lines)+b'\r\n\r\n'
    require(headers(raw)==fields,'Manifest differs');return raw
def write_zip(path,entries):
    with zipfile.ZipFile(path,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for n in sorted(entries,key=lambda x:(x!=MF,x)):
            info=zipfile.ZipInfo(n,(2026,11,5,0,0,0));info.external_attr=0o100644<<16;info.compress_type=zipfile.ZIP_DEFLATED
            z.writestr(info,entries[n],compress_type=zipfile.ZIP_DEFLATED,compresslevel=6)
def class_files(folder):return {p.relative_to(folder).as_posix():p.read_bytes() for p in sorted(folder.rglob('*.class'))}
def sources():return {str(p.relative_to(ROOT)):sha(p) for p in sorted(ROOT.rglob('*')) if p.is_file() and p.suffix in ('.java','.py','.js','.json') and not any(x in p.parts for x in ('node_modules','__pycache__'))}
def resources(path):
    with zipfile.ZipFile(path) as z:
        return {n:hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if not n.endswith('/') and not re.fullmatch(r'classes(?:[0-9]+)?\.dex',n) and not (n.startswith('META-INF/') and (n==MF or n.upper().endswith(('.SF','.RSA','.DSA','.EC'))))}
def entries(path):
    with zipfile.ZipFile(path) as z:return {n:hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if not n.endswith('/')}

def main():
    p=argparse.ArgumentParser()
    for n in ('base','tools','work','dist'):p.add_argument('--'+n,type=Path,required=True)
    p.add_argument('--apk',type=Path);p.add_argument('--java-home',type=Path)
    a=p.parse_args()
    for n in ('base','tools','work','dist','apk','java_home'):
        if getattr(a,n) is not None:setattr(a,n,getattr(a,n).resolve())
    java=a.java_home/'bin/java' if a.java_home else 'java'
    javac=a.java_home/'bin/javac' if a.java_home else 'javac'
    require(sha(a.base)==BASE_SHA,'Active bundle baseline differs')
    if a.apk:require(sha(a.apk)==SUPPLIED_APK_SHA,'Supplied Amazon APK differs')
    for n,h in TOOLS.items():require(sha(a.tools/n)==h,'Pinned tool differs '+n)
    require(not a.work.exists(),'Fresh work directory required')
    require(not a.dist.exists() or not any(a.dist.iterdir()),'Empty dist required')
    a.work.mkdir(parents=True);a.dist.mkdir(parents=True,exist_ok=True)
    def run(cmd,name,timeout=900):
        print('RUN '+name,flush=True)
        with (a.work/name).open('w') as out:r=subprocess.run(list(map(str,cmd)),stdout=out,stderr=subprocess.STDOUT,timeout=timeout)
        text=(a.work/name).read_text();require(r.returncode==0,'Failed '+name+'\n'+text[-12000:]);return text.strip()
    require(run([javac,'-version'],'javac.txt')=='javac 21.0.13','Pinned JDK21.0.13 required')
    source_hashes=sources()
    generator_tests=run([sys.executable,ROOT/'test/test_script_generator.py',javac,java],'script-generator-tests.txt')
    for n in ('runtime','runtime-generated','runtime-dex','patch','patch-dex','test','host','applications'):(a.work/n).mkdir()
    run([sys.executable,ROOT/'runtime/generate_script.py',a.work/'runtime-generated/ScriptPayload.java'],'script-generation.txt')
    run([javac,'--release','8','-encoding','UTF-8','-cp',a.tools/'android.jar','-d',a.work/'runtime',*sorted((ROOT/'runtime').glob('*.java')),*sorted((a.work/'runtime-generated').glob('*.java'))],'runtime-javac.log')
    run([javac,'--release','8','-encoding','UTF-8','-cp',a.tools/'morphe.jar','-d',a.work/'patch',*sorted((ROOT/'patch').glob('*.java'))],'patch-javac.log')
    auditcp=os.pathsep.join(map(str,(a.work/'test',a.work/'patch',a.tools/'morphe-2.2.jar')))
    run([javac,'--release','8','-encoding','UTF-8','-cp',auditcp,'-d',a.work/'test',*sorted((ROOT/'test').glob('*.java'))],'audit-javac.log')
    hostcp=os.pathsep.join(map(str,(a.work/'host',a.work/'runtime')))
    host_sources=sorted((ROOT/'runtime-tests/java').rglob('*.java'));require(host_sources,'Runtime host tests missing')
    run([javac,'--release','8','-encoding','UTF-8','-cp',hostcp,'-d',a.work/'host',*host_sources],'host-javac.log')
    host_java=run([java,'-ea','-cp',hostcp,'app.hiro.amazonreviews.ReviewCountRuntimeTest'],'host-java.txt')
    host_js=run(['node',ROOT/'runtime-tests/test_sort.js'],'host-js.txt')
    require('PASS' in host_java and 'PASS' in host_js,'Runtime tests incomplete')
    guards=[]
    for sdk in ('morphe-2.2.jar',):
        cp=os.pathsep.join(map(str,(a.work/'test',a.work/'patch',a.tools/sdk)))
        guards.append(run([java,'-Xmx3g','-ea','-cp',cp,'AmazonReviewAudit','guards',*([a.apk] if a.apk else [])],sdk+'.guards.txt'))
    for kind,api in (('runtime','23'),('patch','26')):
        write_zip(a.work/(kind+'.jar'),class_files(a.work/kind))
        run([java,'-cp',a.tools/'r8.jar','com.android.tools.r8.D8','--release','--min-api',api,'--lib',a.tools/'android.jar','--classpath',a.tools/('android.jar' if kind=='runtime' else 'morphe.jar'),'--output',a.work/(kind+'-dex'),a.work/(kind+'.jar')],kind+'-d8.log')
        require({p.name for p in (a.work/(kind+'-dex')).iterdir()}=={'classes.dex'},'Unexpected multidex')
    base=archive(a.base);require(headers(base[MF])['Version']==BASE_VERSION,'Baseline version differs')
    require(any(n.startswith(PREFIX) for n in base) and EXT in base,'Previous review patch missing from baseline')
    (a.work/'base.dex').write_bytes(base['classes.dex'])
    preservation=run([java,'-Xmx3g','-ea','-cp',auditcp,'AmazonReviewAudit','merge',a.work/'base.dex',a.work/'patch-dex/classes.dex',a.work/'merged.dex'],'loader-preservation.txt')
    additions={**class_files(a.work/'patch'),EXT:(a.work/'runtime-dex/classes.dex').read_bytes()}
    fields=headers(base[MF]);fields.update(Version=BUNDLE_VERSION,Timestamp='2026-11-05T00:00:00',Description='Amazon検索の起動・復旧とホームURL判定を改善。取得済みの評価件数順に固定。動画の順序不成立の原因は未確定。実機未確認。')
    retained={n:v for n,v in base.items() if not n.startswith(PREFIX) and n!=EXT}
    combined={**retained,**additions,'classes.dex':(a.work/'merged.dex').read_bytes(),MF:manifest(fields)}
    single_fields={**fields,'Name':'Amazon 商品検索を評価件数順に表示','Version':VERSION}
    single={**additions,'classes.dex':(a.work/'patch-dex/classes.dex').read_bytes(),MF:manifest(single_fields)}
    preserved={n:v for n,v in base.items() if n not in (MF,'classes.dex',EXT) and not n.startswith(PREFIX)}
    require(all(combined.get(n)==v for n,v in preserved.items()),'Existing resources changed')
    applications=[];all_entries={};original_resources=resources(a.apk) if a.apk else None
    for name,data in ((SINGLE,single),(BUNDLE,combined)):
        write_zip(a.dist/name,data);require(archive(a.dist/name)==data,'Archive differs')
        for sdk in ('morphe-2.2.jar',):
            listing=run([java,'-jar',a.tools/sdk,'list-patches','--patches',a.dist/name,'-f',PACKAGE,'-p','-v'],name+'.'+sdk+'.listing.txt')
            require(listing.count('Name: '+PATCH_NAME+'\n')==1,'Patch not discovered exactly once')
            if not a.apk:continue
            out=a.work/'applications'/(name+'-'+sdk);out.mkdir()
            apk,report=out/'patched.apk',out/'morphe.json'
            run([java,'-Xmx4g','-jar',a.tools/sdk,'patch',a.apk,'-p',a.dist/name,'--exclusive','-e',PATCH_NAME,'--unsigned','--bytecode-mode','STRIP_FAST','-o',apk,'-t',out/'tmp','-r',report],name+'.'+sdk+'.apply.log')
            r=json.loads(report.read_text())
            applied=[x['name'] if isinstance(x,dict) else x for x in r.get('appliedPatches',[])]
            steps=r.get('stepResults',r.get('patchingSteps',[]))
            require(apk.exists() and r.get('success',True) is True and not r.get('failedPatches') and applied==[PATCH_NAME] and r.get('packageName')==PACKAGE and r.get('packageVersion')==APP_VERSION and steps and all(x.get('success') is True for x in steps),'APK patch failed')
            audit=run([java,'-Xmx4g','-ea','-cp',auditcp,'AmazonReviewAudit','audit',a.apk,apk,a.work/'runtime-dex/classes.dex'],name+'.'+sdk+'.audit.txt')
            require('PASS' in audit,'APK audit incomplete');require(resources(apk)==original_resources,'APK resources/manifests/assets/native libraries changed')
            all_entries.setdefault(sdk,[]).append(entries(apk));applications.append({'bundle':name,'sdk':sdk,'sdk_version':'2.2.0','bytecode_mode':'STRIP_FAST','package':PACKAGE,'version':APP_VERSION,'result':'PASS','audit_summary':audit.splitlines()[-1],'preserved_original_resources':len(original_resources)})
    if a.apk:require(all(len(x)==2 and x[0]==x[1] for x in all_entries.values()),'Single/bundle APK content differs')
    require(sources()==source_hashes,'Sources changed during build')
    qa={'schema':'amazon-review105-qa-v1','patch_version':VERSION,'bundle_version':BUNDLE_VERSION,'baseline_version':BASE_VERSION,'baseline_sha256':BASE_SHA,'production_android_sdk_compile':True,'physical_android_tested':False,'runtime_extension':EXT,'apk_bytecode_mode':'STRIP_FAST','toolchain_sha256':TOOLS,'sources':source_hashes,'artifacts':{n:{'bytes':(a.dist/n).stat().st_size,'sha256':sha(a.dist/n)} for n in (SINGLE,BUNDLE)},'loader_preservation':preservation,'preserved_unrelated_entries':len(preserved),'host_tests':host_java+'\n'+host_js,'script_generation_tests':generator_tests,'guard_tests':'\n'.join(guards),'apk_application':applications,'supplied_apk_sha256':sha(a.apk) if a.apk else None,'supplied_apk_non_dex_resources_unchanged':bool(a.apk),'single_bundle_apk_entry_contents_equal':bool(a.apk)}
    (a.dist/QA_NAME).write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
    print('PASS BUILD '+json.dumps(qa['artifacts']),flush=True)
if __name__=='__main__':main()
