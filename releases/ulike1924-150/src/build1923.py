#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess
from build1922 import archive,write_zip,headers,manifest,run,require,sha
from host_tests1920 import test as t20
from host_tests1921 import test as t21
from host_tests1922 import test as t22
from host_tests1923 import test as t23
ROOT=Path(__file__).resolve().parent
PINS={
 'ULike_HQ_Texture_Online_v1.9.22.mpp': '5fd0d17b8e5001d77f1bb8299610349fd0c3934318db76fb735dcb51fd2bade3',
 'Hiro_Morphe_Patches_v1.0.148.mpp':'4b995b563be1c024b1d1e438149835fee1b303f1301d87a822ef9da7fe0ffcff',
 'morphe.jar':'82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
 'android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
 'd8.jar':'305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5'}
def build(a):
 require(not a.work.exists(),'Fresh work directory required');a.work.mkdir(parents=True);a.output.mkdir(parents=True,exist_ok=True)
 for name,pin in PINS.items():require(sha(((a.tools if name.endswith('.jar') else a.input)/name).read_bytes())==pin,'Input SHA mismatch '+name)
 base=archive(a.input/'ULike_HQ_Texture_Online_v1.9.22.mpp');bundle=archive(a.input/'Hiro_Morphe_Patches_v1.0.148.mpp')
 for n in base:
  if n.startswith('ulike'):require(base[n]==bundle[n],'Bundle ULike baseline differs')
 baseline=a.work/'baseline'
 for name,raw in base.items():p=baseline/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(raw)
 (a.work/'bundle148.dex').write_bytes(bundle['classes.dex'])
 classes=a.work/'classes';classes.mkdir();helperclasses=a.work/'helper-classes';helperclasses.mkdir();helperdex=a.work/'helper-dex';helperdex.mkdir()
 run(['javac','--release','8','-encoding','UTF-8','-cp',a.tools/'android.jar','-d',helperclasses,*sorted((ROOT/'layout-stubs').rglob('*.java')),*sorted((ROOT/'noise-stubs').rglob('*.java')),ROOT/'PreviewLayout1922.java',ROOT/'ShadowDetail1923.java'],a.work/'helper-javac.log')
 selected=sorted((helperclasses/'com/hiro/ulike').glob('PreviewLayout1922*.class'))+[helperclasses/'com/hiro/ulike/ShadowDetail1923.class']
 run(['java','-cp',a.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',a.tools/'android.jar','--classpath',helperclasses,'--output',helperdex,*selected],a.work/'d8.log')
 cp=os.pathsep.join(map(str,[a.tools/'morphe.jar',baseline,classes]))
 run(['javac','-encoding','UTF-8','-cp',cp,'-d',classes,ROOT/'MergePayloads.java',ROOT/'Transform1923.java',ROOT/'Seed1923.java',ROOT/'VerifyHelperReferences.java'],a.work/'transform-javac.log')
 run(['javac','-encoding','UTF-8','--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','-cp',cp,'-d',classes,ROOT/'PatchClass1923.java'],a.work/'metadata-javac.log')
 if a.stock:
  run(['java','-cp',cp,'Seed1923',a.stock,ROOT/'stock-seed1923.dex'],a.work/'seed.log')
 require((ROOT/'stock-seed1923.dex').exists(),'Reviewed stock seed required')
 for n in ('emitted','repeat'):
  run(['java','-cp',cp,'Transform1923',baseline,helperdex/'classes.dex',a.work/'bundle148.dex',ROOT/'stock-seed1923.dex',a.work/n,a.work/(n+'-audit.tsv')],a.work/(n+'.log'))
 require({p.name:p.read_bytes() for p in (a.work/'emitted').iterdir()}=={p.name:p.read_bytes() for p in (a.work/'repeat').iterdir()},'DEX rebuild differs')
 emitted=a.work/'emitted'
 run(['java','--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','-cp',cp,'PatchClass1923',baseline/'app/hiro/ulike/patches/UlikeHqMaxPatch.class',emitted/'UlikeHqMaxPatch.class'],a.work/'metadata.log')
 run(['java','-cp',cp,'VerifyHelperReferences',baseline/'ulike/methods.dex',baseline/'ulike/runtime.dex',emitted/'methods.dex',emitted/'runtime.dex'],a.work/'references.log')
 shutil.copyfile(ROOT/'busy-schema.tsv',emitted/'busy-schema.tsv')
 t20(ROOT,a.work);exit_count=t21(ROOT,a.work);layout_count=t22(ROOT,a.work);shadow_count=t23(ROOT,a.work)
 final=dict(base);final.update({'classes.dex':(emitted/'loader.dex').read_bytes(),'ulike/runtime.dex':(emitted/'runtime.dex').read_bytes(),'ulike/methods.dex':(emitted/'methods.dex').read_bytes(),'ulike/methods.tsv':(emitted/'methods.tsv').read_bytes(),'app/hiro/ulike/patches/UlikeHqMaxPatch.class':(emitted/'UlikeHqMaxPatch.class').read_bytes()})
 mf=headers(base['META-INF/MANIFEST.MF']);mf.update(Version='1.9.23',Timestamp='2026-10-06T00:00:00',Description='Use live preview view geometry and label-safe recovery. Edge-aware shadow finishing after native or Java chroma. Device untested.');final['META-INF/MANIFEST.MF']=manifest(mf)
 own=lambda n:n.startswith(('ulike/','ulike181/','ulike182/','ulike186/','ulike191/','app/hiro/ulike/patches/'))
 unchanged={n:b for n,b in bundle.items() if not own(n) and n not in ('META-INF/MANIFEST.MF','classes.dex')}
 integrated={**unchanged,**{n:b for n,b in final.items() if own(n)},'classes.dex':(emitted/'bundle-loader.dex').read_bytes()}
 mf=headers(bundle['META-INF/MANIFEST.MF']);mf.update(Version='1.0.149',Timestamp='2026-10-06T00:00:00',Description='ULike1.9.23 preview geometry and shadow noise repair. Preserve all other apps from1.0.148. Device untested.');integrated['META-INF/MANIFEST.MF']=manifest(mf)
 require({n:b for n,b in integrated.items() if not own(n) and n not in ('META-INF/MANIFEST.MF','classes.dex')}==unchanged,'Other app resources changed')
 changes=sorted(n for n in final if final[n]!=base[n]);require(set(changes)=={'classes.dex','ulike/runtime.dex','ulike/methods.dex','ulike/methods.tsv','META-INF/MANIFEST.MF','app/hiro/ulike/patches/UlikeHqMaxPatch.class'},'Unexpected standalone changes')
 outputs={}
 for name,items in [('ULike_HQ_Texture_Online_v1.9.23.mpp',final),('Hiro_Morphe_Patches_v1.0.149.mpp',integrated)]:
  p=a.output/name;write_zip(p,items);again=a.work/(name+'.repeat');write_zip(again,items);require(p.read_bytes()==again.read_bytes(),'MPP nondeterministic');require(archive(p)==items,'ZIP mismatch')
  run(['java','-jar',a.tools/'morphe.jar','list-patches','--patches',p],a.work/(name+'.loader.log'));outputs[name]={'sha256':sha(p.read_bytes()),'bytes':p.stat().st_size}
 qa={'schema':'ulike1923-geometry-shadows-1','status':'BUILT_HOST_TESTED_DEVICE_UNVERIFIED_NOT_PUBLISHED','ulike_version':'1.9.23','bundle_version':'1.0.149','base_versions':['1.9.22','1.0.148'],'approved_lineage':'1.8.8','device_symptom_reproduction':False,'host_tests':{'total':71+exit_count+layout_count+shadow_count,'back_and_session':71,'exit':exit_count,'layout':layout_count,'shadow':shadow_count,'android_device_test':False},'other_app_resources_byte_identical':True,'preserved_face_detection':True,'preserved_panel_back':True,'preserved_capture_resolution_and_codec':True,'noise_off_respected':True,'shadows_off_respected':True,'source_photo_uploaded_to_repository':False,'rebuild_deterministic':True,'changed_standalone_entries':changes,'artifacts':outputs,'input_sha256':PINS,'stock_seed_sha256':sha((ROOT/'stock-seed1923.dex').read_bytes()),'published':False,'manager_feed_updated':False,'limitations':['No Galaxy device, logcat, or hardware camera test.','Code defects confirmed in DEX; exact screenshot trigger is not device-reproduced.','Shadow tests are synthetic/host raster tests, not a new phone capture. Strong denoising can suppress low-contrast texture in dark regions.','HDR/10bit paths not introduced. Existing approved SDR processing guards preserved.']}
 (a.output/'QA_ULike_v1.9.23.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
 for n in ['emitted-audit.tsv','host-tests1920.txt','host-tests1921.txt','host-tests1922.txt','host-tests1923.txt','references.log']:shutil.copyfile(a.work/n,a.output/n)
 print(json.dumps(qa,ensure_ascii=False,indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for n in ['input','tools','work','output']:p.add_argument('--'+n,type=Path,required=True)
 p.add_argument('--stock',type=Path)
 a=p.parse_args()
 for k,v in vars(a).items():
  if v is not None:setattr(a,k,v.resolve())
 build(a)
