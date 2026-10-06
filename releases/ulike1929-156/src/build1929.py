#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess
from build1922 import archive,write_zip,headers,manifest,run,require,sha
from host_tests1920 import test as t20
from host_tests1921 import test as t21
from host_tests1922 import test as t22
from host_tests1923 import test as t23
from host_pipeline1924 import test as tp
from host_gesture1925 import test as tg
from host_restart1926 import test as tr
from host_start1927 import test as ts
from host_facing1928 import test as tf
from host_audit1929 import test as ta
ROOT=Path(__file__).resolve().parent
PINS={
 'ULike_HQ_Texture_Online_v1.9.28.mpp':'dc3f52b63fe01476ea0a10a5389d8ad2bd3e2f122437cb0b2a2da03fe6917404',
 'Hiro_Morphe_Patches_v1.0.155.mpp':'33613c0177ad8b2909e5cb654d4a6e4d4966eb952eb79a3ddad6f7c05eddf344',
 'morphe.jar':'82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
 'android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
 'd8.jar':'305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5'}
def build(a):
 require(not a.work.exists(),'Fresh work directory required');a.work.mkdir(parents=True);a.output.mkdir(parents=True,exist_ok=True)
 for name,pin in PINS.items():require(sha(((a.tools if name.endswith('.jar') else a.input)/name).read_bytes())==pin,'Input SHA mismatch '+name)
 base=archive(a.input/'ULike_HQ_Texture_Online_v1.9.28.mpp');bundle=archive(a.input/'Hiro_Morphe_Patches_v1.0.155.mpp')
 for n in base:
  if n.startswith('ulike'):require(base[n]==bundle[n],'Bundle ULike baseline differs')
 baseline=a.work/'baseline'
 for name,raw in base.items():p=baseline/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(raw)
 (a.work/'bundle148.dex').write_bytes(bundle['classes.dex'])
 classes=a.work/'classes';classes.mkdir();helperclasses=a.work/'helper-classes';helperclasses.mkdir();helperdex=a.work/'helper-dex';helperdex.mkdir()
 run(['javac','--release','8','-encoding','UTF-8','-cp',a.tools/'android.jar','-d',helperclasses,*sorted((ROOT/'audit-stubs').rglob('*.java')),*[ROOT/(n+'.java') for n in ['ProviderLifecycle1929','PreviewInputs1929','SessionFallback1929']]],a.work/'helper-javac.log')
 selected=sorted(p for p in (helperclasses/'com/hiro/ulike').glob('*.class') if p.name.startswith(('ProviderLifecycle1929','PreviewInputs1929','SessionFallback1929')))
 run(['java','-cp',a.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',a.tools/'android.jar','--classpath',helperclasses,'--output',helperdex,*selected],a.work/'d8.log')
 cp=os.pathsep.join(map(str,[a.tools/'morphe.jar',baseline,classes]))
 run(['javac','-encoding','UTF-8','-cp',cp,'-d',classes,ROOT/'MergePayloads.java',ROOT/'Transform1926.java',ROOT/'Transform1927.java',ROOT/'Transform1928.java',ROOT/'Transform1929.java',ROOT/'VerifyAudit1929.java',ROOT/'VerifyRestart1926.java',ROOT/'VerifyStartCompat1929.java',ROOT/'VerifyHelperReferences.java'],a.work/'transform-javac.log')
 run(['javac','-encoding','UTF-8','--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','-cp',cp,'-d',classes,ROOT/'PatchClass1929.java'],a.work/'metadata-javac.log')
 for n in ('emitted','repeat'):
  run(['java','-cp',cp,'Transform1929',baseline,helperdex/'classes.dex',a.work/'bundle148.dex',ROOT/'stock-seed1929.dex',a.work/n,a.work/(n+'-audit.tsv')],a.work/(n+'.log'))
 require({p.name:p.read_bytes() for p in (a.work/'emitted').iterdir()}=={p.name:p.read_bytes() for p in (a.work/'repeat').iterdir()},'DEX rebuild differs')
 emitted=a.work/'emitted'
 run(['java','--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','-cp',cp,'PatchClass1929',baseline/'app/hiro/ulike/patches/UlikeHqMaxPatch.class',emitted/'UlikeHqMaxPatch.class'],a.work/'metadata.log')
 run(['java','-cp',cp,'VerifyHelperReferences',baseline/'ulike/methods.dex',baseline/'ulike/runtime.dex',emitted/'methods.dex',emitted/'runtime.dex'],a.work/'references.log')
 shutil.copyfile(ROOT/'busy-schema.tsv',emitted/'busy-schema.tsv')
 t20(ROOT,a.work);exit_count=t21(ROOT,a.work);layout_count=t22(ROOT,a.work);shadow_count=t23(ROOT,a.work);pipeline_count=tp(ROOT,a.work);gesture_count=tg(ROOT,a.work);restart_count=tr(ROOT,a.work);start_count=ts(ROOT,a.work);facing_count=tf(ROOT,a.work);audit_count=ta(ROOT,a.work)
 run(['java','-cp',cp,'VerifyRestart1926',emitted/'methods.dex',emitted/'runtime.dex'],a.work/'restart-contracts.txt')
 run(['java','-cp',cp,'VerifyStartCompat1929',ROOT/'stock-seed1927.dex',emitted/'methods.dex',emitted/'runtime.dex'],a.work/'start1927-contracts.txt')
 run(['java','-cp',cp,'VerifyAudit1929',baseline,ROOT/'stock-seed1929.dex',emitted/'methods.dex',emitted/'runtime.dex'],a.work/'audit1929-contracts.txt')
 final=dict(base);final.update({'classes.dex':(emitted/'loader.dex').read_bytes(),'ulike/runtime.dex':(emitted/'runtime.dex').read_bytes(),'ulike/methods.dex':(emitted/'methods.dex').read_bytes(),'ulike/methods.tsv':(emitted/'methods.tsv').read_bytes(),'app/hiro/ulike/patches/UlikeHqMaxPatch.class':(emitted/'UlikeHqMaxPatch.class').read_bytes()})
 mf=headers(base['META-INF/MANIFEST.MF']);mf.update(Version='1.9.29',Timestamp='2026-10-06T00:00:00',Description='Rear restart lifecycle audit: provider ownership, complete reuse identity, live preview inputs, ordered fallback and actual-frame watchdog. Image processing unchanged. Device untested.');final['META-INF/MANIFEST.MF']=manifest(mf)
 own=lambda n:n.startswith(('ulike/','ulike181/','ulike182/','ulike186/','ulike191/','app/hiro/ulike/patches/'))
 unchanged={n:b for n,b in bundle.items() if not own(n) and n not in ('META-INF/MANIFEST.MF','classes.dex')}
 integrated={**unchanged,**{n:b for n,b in final.items() if own(n)},'classes.dex':(emitted/'bundle-loader.dex').read_bytes()}
 mf=headers(bundle['META-INF/MANIFEST.MF']);mf.update(Version='1.0.156',Timestamp='2026-10-06T00:00:00',Description='ULike1.9.29 rear restart lifecycle audit. Preserve facing memory, image processing and other apps from1.0.155. Device untested.');integrated['META-INF/MANIFEST.MF']=manifest(mf)
 require({n:b for n,b in integrated.items() if not own(n) and n not in ('META-INF/MANIFEST.MF','classes.dex')}==unchanged,'Other app resources changed')
 changes=sorted(n for n in final if final[n]!=base[n]);require(set(changes)=={'classes.dex','ulike/runtime.dex','ulike/methods.dex','ulike/methods.tsv','META-INF/MANIFEST.MF','app/hiro/ulike/patches/UlikeHqMaxPatch.class'},'Unexpected standalone changes')
 outputs={}
 for name,items in [('ULike_HQ_Texture_Online_v1.9.29.mpp',final),('Hiro_Morphe_Patches_v1.0.156.mpp',integrated)]:
  p=a.output/name;write_zip(p,items);again=a.work/(name+'.repeat');write_zip(again,items);require(p.read_bytes()==again.read_bytes(),'MPP nondeterministic');require(archive(p)==items,'ZIP mismatch')
  run(['java','-jar',a.tools/'morphe.jar','list-patches','--patches',p],a.work/(name+'.loader.log'));outputs[name]={'sha256':sha(p.read_bytes()),'bytes':p.stat().st_size}
 qa={'schema':'ulike1929-rear-restart-lifecycle-audit-1','status':'BUILT_HOST_TESTED_DEVICE_UNVERIFIED_NOT_PUBLISHED','ulike_version':'1.9.29','bundle_version':'1.0.156','base_versions':['1.9.28','1.0.155'],'approved_lineage':'1.8.8','device_symptom_reproduction':False,
 'host_tests':{'total':71+exit_count+layout_count+shadow_count+pipeline_count+gesture_count+restart_count+start_count+facing_count+audit_count,'back_and_session':71,'exit':exit_count,'layout':layout_count,'shadow':shadow_count,'pipeline_contract':pipeline_count,'black_tap':gesture_count,'rear_restart':restart_count,'lost_preview_start_legacy_component':start_count,'facing_memory':facing_count,'shipped_1929_lifecycle_helpers':audit_count,'android_device_test':False},
 'other_app_resources_byte_identical':True,'preserved_face_detection':True,'preserved_panel_back':True,'preserved_capture_resolution_and_codec':True,'image_pipeline_byte_identical':True,'rear_facing_preference_preserved':True,'remember_last_camera':True,'manual_lens_selection_unchanged':True,
 'provider_teardown_snapshots_ownership':True,'provider_reuse_checks_live_target':True,'provider_cache_includes_surface_format_primary_and_frozen_size':True,'no_duplicate_foreground_epoch_reset':True,'released_rear_texture_renewed_via_native_path':True,'configuration_fallback_before_failure':True,'failed_first_request_not_reported_as_preview_success':True,'watchdog_uses_delivered_texture_frames':True,'external_surface_only_not_falsely_marked_missing':True,
 'new_retry_loops':0,'old_retry_and_busy_limits_preserved':True,'unrelated_runtime_unchanged':True,'source_photo_uploaded_to_repository':False,'rebuild_deterministic':True,'changed_standalone_entries':changes,'artifacts':outputs,'input_sha256':PINS,'published':False,'manager_feed_updated':False,
 'limitations':['No Galaxy device, logcat, ART, or hardware-camera execution.','The audit identifies concrete lifecycle defects and fixes them; it cannot establish these are all device-specific causes or that the supplied symptom is resolved.','The 283 lost-start legacy-component checks run its previous Java source; the substituted readiness helper is exercised by 1929 helper tests and its actual emitted DEX bridge/control flow are checked separately.','Frame delivery to the native listener is not proof of correct on-screen pixels. External Surface-only and non-texture modes retain metadata-based monitoring.','Existing native fallback can choose a supported normal-sensor/output configuration after failure; its peak resolution can differ. No still-image filter, codec or resolution policy is changed by this release.','Performance and repeated process restart timings are not measured on hardware. No original APK or user photos are published.']}
 (a.output/'QA_ULike_v1.9.29.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
 for n in ['emitted-audit.tsv','host-tests1920.txt','host-tests1921.txt','host-tests1922.txt','host-tests1923.txt','host-pipeline1924.txt','host-gesture1925.txt','host-restart1926.txt','host-start1927.txt','start1927-contracts.txt','restart-contracts.txt','references.log','host-facing1928.txt','audit1929-contracts.txt','host-audit1929.txt']:shutil.copyfile(a.work/n,a.output/n)
 print(json.dumps(qa,ensure_ascii=False,indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for n in ['input','tools','work','output']:p.add_argument('--'+n,type=Path,required=True)
 p.add_argument('--stock',type=Path)
 a=p.parse_args()
 for k,v in vars(a).items():
  if v is not None:setattr(a,k,v.resolve())
 build(a)
