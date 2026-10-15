#!/usr/bin/env python3
"""Build reviewed M1/M2/M3/M4/M6/M7/M8/M10 helpers on the pinned .47 baseline."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, shutil, subprocess
import build1945 as common

ROOT = Path(__file__).resolve().parent
VERSION = '1.9.48'
BASE_VERSION = '1.9.47'
SINGLE = 'ULike_HQ_Texture_Online_v1.9.48.mpp'
BASE_SINGLE = 'ULike_HQ_Texture_Online_v1.9.47.mpp'
BUNDLE_VERSION = '1.0.181'
BASE_BUNDLE_VERSION = '1.0.180'
BASE_SINGLE_SHA256 = '47bddb21c9ff9c459afa2c560365c605cdb185a91f4e1b1c2bab6a98aa8420c9'
BASE_BUNDLE_SHA256 = '74d05e33baad75524f9b9c31312f5f2d0ee33bfb9377579abc77475c4a0ea520'
TOOL_PINS = common.TOOL_PINS
require, sha, run, verify_jdk = common.require, common.sha, common.run, common.verify_jdk
archive, headers, manifest, own, write_zip = common.archive, common.headers, common.manifest, common.own, common.write_zip
SELECTED = ['M1','M2','M4','M8','M3','M6','M7','M10']
PRODUCTION = ['BurstCapture1933','FusionPixels1933']

def bundle_name(qa):
    return 'Hiro_Morphe_Patches_v' + qa['bundle_version'] + '.mpp'

def config(path=None):
    return json.loads(Path(path or ROOT.parent/'manifest.json').read_text())

def build(args):
    identity = verify_jdk()
    require(not args.work.exists(), 'Fresh build work directory required')
    args.work.mkdir(parents=True); args.output.mkdir(parents=True,exist_ok=True)
    base_name='Hiro_Morphe_Patches_v'+BASE_BUNDLE_VERSION+'.mpp'
    pins={BASE_SINGLE:BASE_SINGLE_SHA256,base_name:BASE_BUNDLE_SHA256,**TOOL_PINS}
    for name,digest in pins.items():
        p=(args.tools if name.endswith('.jar') else args.input)/name
        require(sha(p.read_bytes())==digest,'Pinned input differs: '+name)
    base=archive(args.input/BASE_SINGLE); bundle=archive(args.input/base_name)
    require(headers(base['META-INF/MANIFEST.MF'])['Version']==BASE_VERSION,'Wrong standalone baseline')
    require(headers(bundle['META-INF/MANIFEST.MF'])['Version']==BASE_BUNDLE_VERSION,'Wrong integrated baseline')
    require({n:b for n,b in base.items() if own(n)}=={n:b for n,b in bundle.items() if own(n)},'Baseline ULike copies differ')
    baseline=args.work/'baseline'
    for name,data in base.items():
        p=baseline/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
    bundle_dex=baseline/'bundle.dex';bundle_dex.write_bytes(bundle['classes.dex'])
    names=json.loads((ROOT/'production1948.json').read_text())
    require(names==PRODUCTION,'Reviewed production roots differ')
    production=[ROOT/(n+'.java') for n in names]
    source_hashes={p.name:sha(p.read_bytes()) for p in production}
    timing_source=ROOT/'ProcessingTiming1947.java';timing_bytes=timing_source.read_bytes();timing_source_sha=sha(timing_bytes)
    require(timing_bytes.count(b'public static final String VERSION = \"1.9.48\";') == 1 and sha(timing_bytes.replace(b'\"1.9.48\"',b'\"1.9.47\"')) == 'ddcc76a0f900ad7fe4bee0a81f07db9dc917512b0118fe70c19ef67f393f5a79', 'Timer source must differ from pinned47 only in VERSION')
    classes=args.work/'classes';helpers=args.work/'helper-classes';dex=args.work/'helper-dex'
    for p in [classes,helpers,dex]:p.mkdir()
    stub_map={}
    for folder in ['front1936-stubs','renderer1938-stubs','capture-stubs','fast-stubs','metadata-stubs','quality-stubs','quality-dependencies','timing1947-stubs']:
        path=ROOT/folder
        if path.exists():
            for p in sorted(path.rglob('*.java')):stub_map[str(p.relative_to(path))]=p
    # Dependencies compile only; D8 receives exactly the two reviewed production roots.
    # Every other runtime class remains byte-identical to the pinned .47 payload.
    for retained in ['AsyncSave1935','ProcessingTiming1947','QualityPipeline1932','ShotContext1932','TimedIo1947','QualityPixels1932','QualityShadow1932','SpeedWorkers1935','SaveQueue1935','FusionPixels1933','ReflectionCache1945','CodecDrain1945','NoiseCache1944','NativeSpeed1944','PolicyCache1945','Scheduling1944']:
        stub_map['com/hiro/ulike/'+retained+'.java']=ROOT/(retained+'.java')
    for n in names:stub_map.pop('com/hiro/ulike/'+n+'.java',None)
    run(['javac','-source','8','-target','8','-encoding','UTF-8','-bootclasspath',args.tools/'android.jar','-d',helpers,*stub_map.values(),*production],args.work/'helper-javac.log')
    selected=sorted(p for p in (helpers/'com/hiro/ulike').glob('*.class') if any(p.name==n+'.class' or p.name.startswith(n+'$') for n in names))
    require(all(helpers/'com/hiro/ulike'/(n+'.class') in selected for n in names),'Missing compiled production root')
    run(['java','-cp',args.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',args.tools/'android.jar','--classpath',helpers,'--output',dex,*selected],args.work/'d8.log')
    cp=os.pathsep.join(map(str,[args.tools/'morphe.jar',baseline,classes]))
    transformer_names=['MergePayloads','Transform1948','Verify1948','VerifyHelperReferences']
    transformer_sources=[ROOT/(n+'.java') for n in transformer_names]+[ROOT/'PatchClass1948.java']
    transformer_hashes={p.name:sha(p.read_bytes()) for p in transformer_sources}
    run(['javac','-encoding','UTF-8','-cp',cp,'-d',classes,*[ROOT/(n+'.java') for n in transformer_names]],args.work/'transform-javac.log')
    run(['javac','-encoding','UTF-8','--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','-cp',cp,'-d',classes,ROOT/'PatchClass1948.java'],args.work/'metadata-javac.log')
    properties=['-Dulike.production='+','.join(names)]
    for n in ['emitted','repeat']:
        run(['java',*properties,'-cp',cp,'Transform1948',baseline,dex/'classes.dex',bundle_dex,args.work/n,args.work/(n+'-audit.tsv')],args.work/(n+'.log'))
    emitted=args.work/'emitted'
    require({p.name:p.read_bytes() for p in emitted.iterdir()}=={p.name:p.read_bytes() for p in (args.work/'repeat').iterdir()},'DEX transform is not deterministic')
    run(['java','--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','-cp',cp,'PatchClass1948',baseline/'app/hiro/ulike/patches/UlikeHqMaxPatch.class',emitted/'UlikeHqMaxPatch.class'],args.work/'metadata.log')
    verification=run(['java',*properties,'-cp',cp,'Verify1948',baseline/'ulike/runtime.dex',emitted/'runtime.dex',dex/'classes.dex'],args.work/'optimization-dex-verification.txt')
    references=run(['java','-cp',cp,'VerifyHelperReferences',baseline/'ulike/methods.dex',baseline/'ulike/runtime.dex',emitted/'methods.dex',emitted/'runtime.dex'],args.work/'helper-references.txt')
    require(references.startswith('PASS helper references in '),'Executed helper reference verification missing')
    spec=importlib.util.spec_from_file_location('optimization_tests',ROOT/'host_regression1948.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    result=module.test(ROOT,args.work,args.tools/'android.jar')
    require(result.get('status')=='passed' and result.get('assertions',0)>0,'Executed optimization and preservation tests must pass')
    require(set(result.get('suites',{})) >= {'baseline_camera_quality_save_timing','exact_temporal_fusion','capture_preprocessing_lifecycle'} and result.get('pixel_equivalence_to_baseline') is True and result.get('full_resolution_nv21_checked') == '4080x3060', 'Independent exact-output, full-resolution and capture preparation tests required')
    require(source_hashes=={p.name:sha(p.read_bytes()) for p in production},'Production sources changed during build')
    require(timing_source_sha==sha(timing_source.read_bytes()), 'Version-only timing source changed during build')
    require(transformer_hashes=={p.name:sha(p.read_bytes()) for p in transformer_sources},'Transformer sources changed during build')
    inventory=json.loads((emitted/'optimization-inventory.json').read_text())
    final=dict(base)
    final.update({'classes.dex':(emitted/'loader.dex').read_bytes(),'ulike/runtime.dex':(emitted/'runtime.dex').read_bytes(),'app/hiro/ulike/patches/UlikeHqMaxPatch.class':(emitted/'UlikeHqMaxPatch.class').read_bytes()})
    fields=headers(base['META-INF/MANIFEST.MF']);fields.update(Version=VERSION,Timestamp='2026-10-08T04:00:00',Description='M1/M2/M3/M4/M6/M7/M8/M10 exact-output fusion scheduling and reusable buffers. Existing stage timing, image quality, native payload and save settings retained; Android device unverified.')
    final['META-INF/MANIFEST.MF']=manifest(fields)
    integrated=dict(bundle);integrated.update({n:b for n,b in final.items() if own(n)});integrated['classes.dex']=(emitted/'bundle-loader.dex').read_bytes()
    fields=headers(bundle['META-INF/MANIFEST.MF']);fields.update(Version=BUNDLE_VERSION,Timestamp='2026-10-08T04:00:00',Description='ULike1.9.48 reviewed M optimizations; all other applications retained from1.0.180; stage timing retained; device unverified.')
    integrated['META-INF/MANIFEST.MF']=manifest(fields)
    allowed={'classes.dex','ulike/runtime.dex','META-INF/MANIFEST.MF','app/hiro/ulike/patches/UlikeHqMaxPatch.class'}
    changes=sorted(n for n in base if final[n]!=base[n])
    require(set(final)==set(base) and set(changes)<=allowed,'Unexpected archive resource delta')
    require(set(integrated)==set(bundle),'Integrated archive inventory differs')
    require(all(integrated[n]==b for n,b in bundle.items() if not own(n) and n not in ['classes.dex','META-INF/MANIFEST.MF']),'Other application bytes changed')
    native='ulike1935/runtime/libulike_speed1935.so';installer='app/hiro/ulike/patches/IntegrationPayload186.class'
    for n in [native,installer,'ulike/methods.dex','ulike/methods.tsv']:require(final[n]==base[n],'Protected binary payload changed: '+n)
    artifacts={}
    for name,entries in [(SINGLE,final),('Hiro_Morphe_Patches_v'+BUNDLE_VERSION+'.mpp',integrated)]:
        p=args.output/name;write_zip(p,entries);require(archive(p)==entries,'Output archive roundtrip failed')
        run(['java','-jar',args.tools/'morphe.jar','list-patches','--patches',p],args.work/(name+'.loader.log'))
        artifacts[name]={'sha256':sha(p.read_bytes()),'bytes':p.stat().st_size}
    qa={'schema':'ulike1948-optimization-v1','status':'BUILT_HOST_VERIFIED_DEVICE_UNVERIFIED','ulike_version':VERSION,'bundle_version':BUNDLE_VERSION,'base_versions':[BASE_VERSION,BASE_BUNDLE_VERSION],'approved_lineage':'1.8.8','selected_candidates':SELECTED,'artifacts':artifacts,'input_sha256':pins,'jdk_identity':identity,
        'device_tested':False,'device_quality_verified':False,'original_apk_apply_tested':False,'ci_android_apply_tested':False,
        'non_ulike_resources_byte_identical':True,'non_ulike_loader_classes_unchanged':True,'standalone_and_bundle_ulike_resources_identical':True,
        'quality_algorithm_and_settings_preserved':True,'host_pixel_equivalence_passed':True,'stage_timing_payload_byte_identical':False,'stage_timing_algorithm_preserved':True,'timing_version_only_change':True,'device_save_speed_measured':False,'actual_sensor_capture_latency_measured':False,'resolution_and_save_format_preserved':True,'save_format_and_codec_configuration_preserved':True,'manual_capture_safety_checks_preserved':True,
        'native_library_byte_identical':True,'native_installer_byte_identical':True,'native_methods_byte_identical':True,'native_methods_tsv_byte_identical':True,
        'native_library_sha256':sha(final[native]),'native_library_bytes':len(final[native]),
        'host_quality_passed':True,'host_quality_result':result,'host_quality_result_sha256':sha(json.dumps(result,sort_keys=True).encode()),
        'production_source_consistency_verified':True,'compiled_production_source_sha256':source_hashes,'timing_metadata_source_sha256':{'ProcessingTiming1947.java':timing_source_sha},'compiled_transformer_source_sha256':transformer_hashes,
        'gpu_processing_added':False,'diagnostic_report_updates_on_success':True,'legacy_report_not_displayed_as_current':True,'diagnostic_overlapping_wall_clock':True,'diagnostic_per_shot_owner':True,'diagnostic_stage_names':['fusion','noise','correction','compression','save'],
        'changed_standalone_entries':changes,'added_standalone_entries':[],'changed_bundle_entries':sorted(n for n in bundle if integrated[n]!=bundle[n]),'added_bundle_entries':[],
        'dex_verification':verification.strip(),'published':False,'manager_feed_updated':False,
        'limitations':['No Android/Galaxy camera execution.','Stage intervals may overlap and do not sum to shutter-to-save elapsed time.','Host equivalence and reduced work do not establish a device speedup.','Unmeasured or skipped stages are labeled explicitly.']}
    qa.update(inventory)
    (args.output/'QA_ULike_v1.9.48.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2,sort_keys=True)+'\n')
    for n in ['emitted-audit.tsv','optimization-dex-verification.txt','helper-references.txt']:shutil.copyfile(args.work/n,args.output/n)
    shutil.copyfile(emitted/'optimization-inventory.json',args.output/'optimization-inventory.json')
    (args.output/'host-regression1948-result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'status':qa['status'],'artifacts':artifacts,'host_quality_result':result},ensure_ascii=False,indent=2))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    for n in ['input','tools','work','output']:parser.add_argument('--'+n,type=Path,required=True)
    parser.add_argument('--jdk',type=Path);args=parser.parse_args()
    if args.jdk:os.environ['PATH']=str(args.jdk.resolve()/'bin')+os.pathsep+os.environ.get('PATH','')
    for k,v in vars(args).items():
        if v is not None:setattr(args,k,v.resolve())
    build(args)
