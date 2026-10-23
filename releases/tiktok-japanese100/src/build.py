#!/usr/bin/env python3
"""Deterministic additive MPP build and real published-extension integration gates.
The APK containers are synthetic; neither a real TikTok APK nor device execution
is claimed. Exact upstream extension DEX, full Morphe pipelines and bytecode
preservation are checked instead of imitating the upstream translation registry.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re,subprocess,sys
ROOT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT))
import generate,make_host
VERSION='1.0.0';BASE_VERSION='1.0.278';BUNDLE_VERSION='1.0.279'
BASE_SHA='8811761b21adf9bf80859e4598a975e82e1a3fed9f16cf74b9a8b0a7ac95c419'
UPSTREAM_SHA='45dc0c2244f8cf6973de18542d2d9a0cc25512f5b1de52438c685b82edb7c4cd'
UPSTREAM_COMMIT='b582c984f50aeacbc812761e082354efa3fc0c28'
EXT_SHA='0ffc719121dd4e5cc5579a22e9b76208544b47c2c990582c898dff16549f2f21'
TOOLS={'morphe200.jar':'deb5d931f3899b20a119637473d338c52585f0c561b5d8b2856b916fe90163b5','morphe.jar':'82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c','morphe22.jar':'fe4bc92815bb14976150e4ec83e4985932b46d1d47edec75d37604481572e864','android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b','d8.jar':'305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5'}
SINGLE='TikTok_Settings_Japanese_v1.0.0.mpp';BUNDLE='Hiro_Morphe_Patches_v1.0.279.mpp'
QA='QA_TikTok_Settings_Japanese_v1.0.0.json';MF='META-INF/MANIFEST.MF'
PREFIX='app/hiro/tiktokjapanese/patches/';EXT='extensions/hiro_tiktok_japanese.mpe'
NAME='TikTok設定を日本語化';PACKAGES=['com.zhiliaoapp.musically','com.ss.android.ugc.trill']
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def require(ok,why):
    if not ok:raise RuntimeError(why)
def source_ids():
    return {str(p.relative_to(ROOT)):sha(p) for p in sorted(ROOT.rglob('*')) if p.is_file() and '__pycache__' not in p.parts}
def main():
    parser=argparse.ArgumentParser()
    for name in ('base','upstream','tools','legacy','work','dist'):parser.add_argument('--'+name,type=Path,required=True)
    a=parser.parse_args()
    for name in vars(a):setattr(a,name,getattr(a,name).resolve())
    require(sha(a.base)==BASE_SHA,'Reviewed baseline278 differs')
    require(sha(a.upstream)==UPSTREAM_SHA,'Published upstream MPP differs')
    for name,digest in TOOLS.items():require(sha(a.tools/name)==digest,'Pinned tool differs: '+name)
    require(not a.work.exists(),'Use a fresh work directory');a.work.mkdir(parents=True)
    require(not a.dist.exists() or not any(a.dist.iterdir()),'Use an empty dist directory');a.dist.mkdir(parents=True,exist_ok=True)
    spec=importlib.util.spec_from_file_location('base_util',a.legacy/'build.py');u=importlib.util.module_from_spec(spec);spec.loader.exec_module(u)
    def run(cmd,log,success=True):
        print('RUN '+log,flush=True)
        with (a.work/log).open('w') as out:r=subprocess.run(list(map(str,cmd)),stdout=out,stderr=subprocess.STDOUT,timeout=300)
        text=(a.work/log).read_text()
        if success:require(r.returncode==0,'Failed '+log+'\n'+text[-7000:])
        return text
    require('Temurin-21.0.8+9' in run(['java','--version'],'java.txt'),'Exact JDK required')
    require(run(['javac','-version'],'javac.txt').strip()=='javac 21.0.8','Exact javac required')
    sources=source_ids()
    for name in ('runtime','patch','audit','host','host-source','runtime-dex','patch-dex','fixtures-empty','fixtures-metra','applications'):(a.work/name).mkdir()
    catalog=generate.generate(a.work/'generated');rows,_=generate.validate()
    run(['javac','--release','8','-encoding','UTF-8','-cp',a.tools/'android.jar','-d',a.work/'runtime',a.work/'generated/JapaneseTranslations.java'],'runtime-javac.log')
    run(['javac','--release','8','-encoding','UTF-8','-cp',a.tools/'morphe.jar','-d',a.work/'patch',*sorted((ROOT/'patch').glob('*.java'))],'patch-javac.log')
    make_host.create(a.work/'host-source',rows)
    run(['javac','--release','8','-encoding','UTF-8','-cp',a.work/'runtime','-d',a.work/'host',ROOT/'upstream/L10n.java',*sorted((a.work/'host-source').rglob('*.java'))],'host-javac.log')
    host=run(['java','-ea','-cp',os.pathsep.join(map(str,[a.work/'host',a.work/'runtime'])),'app.morphe.extension.tiktok.settings.LocalizationHostTest',a.work/'host-source/catalog.tsv'],'host.txt').strip()
    require(host.startswith('PASS localization host: 2181'),'Host localization failed')
    auditcp=os.pathsep.join(map(str,[a.work/'audit',a.work/'patch',a.tools/'morphe.jar']))
    run(['javac','--release','8','-encoding','UTF-8','-cp',auditcp,'-d',a.work/'audit',a.legacy/'test-dex/Fixtures.java',*sorted((ROOT/'test').glob('*.java'))],'audit-javac.log')
    baseline=u.archive(a.base);upstream=u.archive(a.upstream)
    require(u.headers(baseline[MF])['Version']==BASE_VERSION,'Baseline manifest version differs')
    require(u.headers(upstream[MF])['Version']=='0.9.0-dev.1','Upstream version differs')
    require(hashlib.sha256(upstream['extensions/tiktok.mpe']).hexdigest()==EXT_SHA,'Upstream real DEX differs')
    (a.work/'upstream.dex').write_bytes(upstream['extensions/tiktok.mpe'])
    (a.work/'shared.dex').write_bytes(upstream['extensions/shared.mpe'])
    registry=run(['java','-ea','-cp',auditcp,'LocalizationAudit','selftest',a.work/'upstream.dex'],'registry.txt').strip()
    for kind,minapi in [('runtime','26'),('patch','26')]:
        u.write_zip(a.work/(kind+'.jar'),u.class_files(a.work/kind))
        run(['java','-cp',a.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api',minapi,'--lib',a.tools/'android.jar','--classpath',a.tools/('android.jar' if kind=='runtime' else 'morphe.jar'),'--output',a.work/(kind+'-dex'),a.work/(kind+'.jar')],kind+'-d8.log')
        require({p.name for p in (a.work/(kind+'-dex')).iterdir()}=={'classes.dex'},'Unexpected runtime/patch multidex')
    (a.work/'base.dex').write_bytes(baseline['classes.dex'])
    preservation=run(['java','-ea','-cp',auditcp,'LocalizationAudit','merge',a.work/'base.dex',a.work/'patch-dex/classes.dex',a.work/'merged.dex'],'merge.txt').strip()
    additions={**u.class_files(a.work/'patch'),EXT:(a.work/'runtime-dex/classes.dex').read_bytes()}
    for name in ('LICENSE','NOTICE'):additions['META-INF/hiro-tiktok-japanese/'+name]=(ROOT.parent/name).read_bytes()
    additions['META-INF/hiro-tiktok-japanese/ORIGIN.txt']=(
        'Hiro TikTok Settings Japanese 1.0.0\nIndependent Japanese companion compatible with Morphe.\n'
        'Translation catalog derived from icysymmetra/tiktok-patches-for-morphe / Hushfeed.\n'
        'Upstream: https://github.com/icysymmetra/tiktok-patches-for-morphe/tree/'+UPSTREAM_COMMIT+'\n'
        'Corresponding source: https://github.com/hiro191u3n2/hiro-morphe-patches/tree/tiktok-japanese-v1.0.0/releases/tiktok-japanese100\n'
        'GPL-3.0-only with inherited Section 7 notices; see included LICENSE and NOTICE.\n').encode()
    require(not (set(additions)&set(baseline)),'New patch resource path collides with baseline')
    fields=u.headers(baseline[MF]);fields.update(Version=BUNDLE_VERSION,Timestamp='2026-10-24T00:00:00',Description='Add opt-in Japanese settings companion for Metra TikTok 0.9.0-dev.1; preserve all existing patches.')
    combined={**baseline,**additions,MF:u.manifest(fields),'classes.dex':(a.work/'merged.dex').read_bytes()}
    ownfields=dict(fields);ownfields.update(Name='Hiro TikTok Settings Japanese',Version=VERSION,Description='Japanese translations for Metra TikTok settings. Select alongside upstream Settings. Japanese locale required.',Source='https://github.com/hiro191u3n2/hiro-morphe-patches',Author='Hiro contributors',License='GPL-3.0-only with inherited Section 7 notices',**{'Patcher-Version':'2.0.0'})
    single={**additions,MF:u.manifest(ownfields),'classes.dex':(a.work/'patch-dex/classes.dex').read_bytes()}
    retained={k:v for k,v in baseline.items() if k not in (MF,'classes.dex')}
    require(all(combined.get(k)==v for k,v in retained.items()),'Existing bundle resource changed')
    metadata=[]
    for name,data in ((SINGLE,single),(BUNDLE,combined)):
        u.write_zip(a.dist/name,data);require(u.archive(a.dist/name)==data,'Written MPP differs')
        for sdk in ('morphe.jar','morphe22.jar','morphe200.jar'):
            listing=run(['java','-jar',a.tools/sdk,'list-patches','--patches',a.dist/name,'-f',PACKAGES[0],'-p','-v'],name+'.'+sdk+'.listing.txt')
            require(listing.count('Name: '+NAME+'\n')==1,'Patch discovery failed')
            log=run(['java','-ea','-cp',os.pathsep.join(map(str,[a.work/'audit',a.tools/sdk])),'MetadataAudit',a.dist/name],name+'.'+sdk+'.metadata.txt')
            require(log.startswith('PASS metadata'),'Official loader audit failed')
            metadata.append({'artifact':name,'sdk':sdk,'result':'PASS'})
    run(['java','-ea','-cp',auditcp,'LocalizationAudit','empty',a.work/'empty.dex'],'empty.txt')
    run(['java','-ea','-cp',auditcp,'LocalizationAudit','merge-fixture',a.work/'shared.dex',a.work/'upstream.dex',a.work/'metra.dex'],'metra-merge.txt')
    run(['java','-ea','-cp',auditcp,'LocalizationAudit','merge',a.work/'empty.dex',a.work/'metra.dex',a.work/'existing.dex'],'existing-merge.txt')
    for dex,out in [('empty.dex','fixtures-empty'),('existing.dex','fixtures-metra')]:
        run(['java','-ea','-cp',auditcp,'Fixtures','create',a.work/dex,a.work/out],out+'.txt')
    importer={k:v for k,v in u.class_files(a.work/'audit').items() if k.startswith('fixture/')}
    importer.update({MF:u.manifest({'Manifest-Version':'1.0','Name':'Fixture extension import','Version':'1.0.0'}),'extensions/published.mpe':(a.work/'metra.dex').read_bytes()})
    u.write_zip(a.work/'import.mpp',importer)
    cases=[];order_seen=set();sample=None
    def apply(name,package,mode,apk,patches,selected,success=True):
        out=a.work/'applications'/name/package/mode;out.mkdir(parents=True)
        cmd=['java','-Xmx1g','-jar',a.tools/'morphe200.jar','patch',apk]
        # CLI enable flags belong to the immediately preceding -p bundle group.
        require(len(patches)==len(selected),'One explicit selection per fixture bundle')
        for patch,selection in zip(patches,selected):cmd+=['-p',patch,'-e',selection]
        cmd+=['--exclusive']
        cmd+=['--unsigned','--bytecode-mode','FULL','-o',out/'patched.apk','-t',out/'tmp','-r',out/'report.json']
        log=run(cmd,name+'.'+package+'.'+mode+'.apply.log',success=success)
        report=json.loads((out/'report.json').read_text()) if (out/'report.json').exists() else {}
        if success:
            steps=report.get('patchingSteps',report.get('stepResults',[]))
            require((out/'patched.apk').exists() and not report.get('failedPatches') and steps and all(s.get('success') is True for s in steps),'Full patch pipeline failed '+mode)
            require('TikTok Japanese: 2181 catalog entries;' in log,'Finalizer did not connect dictionary')
        return out,log,report
    for name in (SINGLE,BUNDLE):
        for package in PACKAGES:
            for mode in ('preexisting','import-first','japanese-first'):
                patches=[a.dist/name];selected=[NAME]
                if mode=='preexisting':apk=a.work/'fixtures-metra'/(package+'.apk')
                else:
                    apk=a.work/'fixtures-empty'/(package+'.apk')
                    if mode=='import-first':patches.insert(0,a.work/'import.mpp');selected.insert(0,'Fixture Metra extension')
                    else:patches.append(a.work/'import.mpp');selected.append('ZZZ Fixture Metra extension')
                out,log,_=apply(name,package,mode,apk,patches,selected)
                proof=run(['java','-ea','-cp',auditcp,'LocalizationAudit','audit',a.work/'existing.dex',out/'patched.apk'],name+'.'+package+'.'+mode+'.dex.txt')
                require(proof.startswith('PASS actual Metra DEX'),'DEX preservation failed')
                for value in re.findall(r'IMPORT_ORDER_JAPANESE_PRESENT=(true|false)',log):order_seen.add(value=='true')
                cases.append({'artifact':name,'package':package,'mode':mode,'result':'PASS'})
                if name==SINGLE and package==PACKAGES[0] and mode=='preexisting':sample=out/'patched.apk'
    require(order_seen=={False,True},'Both extension execution orders must actually run: '+str(order_seen))
    out,log,_=apply(SINGLE,PACKAGES[0],'repeat',sample,[a.dist/SINGLE],[NAME])
    require('already connected' in log,'Repeat application inserted a new wrapper')
    run(['java','-ea','-cp',auditcp,'LocalizationAudit','audit',a.work/'existing.dex',out/'patched.apk'],'repeat.dex.txt')
    out,log,report=apply(SINGLE,PACKAGES[0],'missing',a.work/'fixtures-empty'/(PACKAGES[0]+'.apk'),[a.dist/SINGLE],[NAME],success=False)
    require('Metraの多言語設定が見つかりません' in log and not (out/'patched.apk').exists(),'Missing upstream extension did not fail clearly')
    require(source_ids()==sources,'Build sources changed during verification')
    qa={'schema':'tiktok-japanese100-qa-v1','patch_version':VERSION,'bundle_version':BUNDLE_VERSION,'baseline_version':BASE_VERSION,'baseline_sha256':BASE_SHA,'upstream_sha256':UPSTREAM_SHA,'upstream_version':'0.9.0-dev.1','upstream_commit':UPSTREAM_COMMIT,'upstream_extension_sha256':EXT_SHA,'physical_android_tested':False,'original_apk_apply_tested':False,'catalog':catalog,'host':host,'registry_selftest':registry,'loader_preservation':preservation,'preserved_unrelated_entries':len(retained),'artifacts':{n:{'bytes':(a.dist/n).stat().st_size,'sha256':sha(a.dist/n)} for n in (SINGLE,BUNDLE)},'sources':sources,'toolchain_sha256':TOOLS,'runtime_sha256':sha(a.work/'runtime-dex/classes.dex'),'fixture_cases':cases,'negative_missing_extension':True,'repeat_apply':True,'both_source_execution_orders':True,'metadata':metadata,'default_selected':False,'production_android_sdk_compile':True,'limitations':['Synthetic APK containers with the exact published Metra extension DEX; original TikTok 47.4.3 APK and physical Android device not tested.','Upstream release-note prose, license text, SIM preset country names and raw diagnostic values are not translated.']}
    (a.dist/QA).write_bytes(u.json_bytes(qa));print('PASS BUILD '+json.dumps(qa['artifacts']),flush=True)
if __name__=='__main__':main()
