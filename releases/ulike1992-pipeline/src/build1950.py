#!/usr/bin/env python3
"""Build reviewed integer GPU helpers on the pinned .49 baseline."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, shutil, subprocess
import build1945 as common

ROOT = Path(__file__).resolve().parent
VERSION = '1.9.50'
BASE_VERSION = '1.9.49'
SINGLE = 'ULike_HQ_Texture_Online_v1.9.50.mpp'
BASE_SINGLE = 'ULike_HQ_Texture_Online_v1.9.49.mpp'
BUNDLE_VERSION = '1.0.183'
BASE_BUNDLE_VERSION = '1.0.182'
BASE_SINGLE_SHA256 = '949ca9b8666dbe20b00b0cc876a45a1257de67e89558f5a8a946961a57ce236e'
BASE_BUNDLE_SHA256 = '474d7bf3290c090ce07c7c864585512be933e07d051f93c99f5eb6261762e3d8'
TOOL_PINS = common.TOOL_PINS
require, sha, run, verify_jdk = common.require, common.sha, common.run, common.verify_jdk
archive, headers, manifest, own, write_zip = common.archive, common.headers, common.manifest, common.own, common.write_zip
SELECTED = ['H1','H2','H3','H4','H5']
PRODUCTION = ['CorePixels1950','QualityShadow1932','GpuInteger1949','QualityPipeline1932','NativeSpeed1944']
GPU_ENTRY='ulike1949/runtime/libulike_gpu1949.so'
CORE_ENTRY='ulike1950/runtime/libulike_core1950.so'

def host_source_pins():
    return {p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in sorted(ROOT.rglob('*')) if p.is_file() and '__pycache__' not in p.parts and p.suffix in ('.java','.py','.txt','.c','.h','.comp')}

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
    require(sha(base['ulike186/runtime/0000.bin'])=='0ac181fac2afbd9260b8c59d922843deead72d3184e78ce97346d57a76b169cb','Correction write-before-read native binary pin required')
    baseline=args.work/'baseline'
    for name,data in base.items():
        p=baseline/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
    bundle_dex=baseline/'bundle.dex';bundle_dex.write_bytes(bundle['classes.dex'])
    names=json.loads((ROOT/'production1950.json').read_text())
    require(names==PRODUCTION,'Reviewed production roots differ')
    production=[ROOT/(n+'.java') for n in names]
    source_hashes={p.name:sha(p.read_bytes()) for p in production}
    timing_source=ROOT/'ProcessingTiming1947.java';timing_bytes=timing_source.read_bytes();timing_source_sha=sha(timing_bytes)
    require(timing_bytes.count(b'public static final String VERSION = \"1.9.50\";') == 1 and sha(timing_bytes.replace(b'\"1.9.50\"',b'\"1.9.47\"')) == 'ddcc76a0f900ad7fe4bee0a81f07db9dc917512b0118fe70c19ef67f393f5a79', 'Timer source must differ from pinned47 only in VERSION')
    require(args.ndk is not None and args.ndk.is_dir(), 'Pinned NDK r27c root required for integer GPU library build')
    os.environ['ULIKE_NDK_HOME']=str(args.ndk)
    os.environ['ULIKE_MORPHE_JAR']=str(args.tools/'morphe.jar')
    native_output=args.work/'gpu-native'
    run(['python3',ROOT/'native1949/build_native1949.py','--ndk',args.ndk,'--output',native_output],args.work/'native1949-build.log')
    gpu=native_output/'libulike_gpu1949.so';gpu_bytes=gpu.read_bytes();gpu_digest=sha(gpu_bytes)
    require(len(gpu_bytes)>=64 and gpu_bytes[:6]==b'\x7fELF\x02\x01' and gpu_bytes[18:20]==b'\xb7\x00','Integer GPU ELF64 AArch64 required')
    native_report=json.loads((native_output/'native-build1949.json').read_text())
    require(native_report.get('schema')=='ulike-native1949-v1' and native_report.get('sha256')==gpu_digest and native_report.get('bytes')==len(gpu_bytes) and native_report.get('physical_android_tested') is False,'GPU native build report differs')
    native_sources={name:sha((ROOT/'native1949'/name).read_bytes()) for name in native_report['sources']}
    require(native_sources==native_report['sources'],'GPU native sources changed after build')
    core_output=args.work/'core-native'
    run(['python3',ROOT/'native1950/build_native1950.py','--ndk',args.ndk,'--output',core_output],args.work/'native1950-build.log')
    core=core_output/'libulike_core1950.so';core_bytes=core.read_bytes();core_digest=sha(core_bytes)
    require(core_bytes[:6]==b'\x7fELF\x02\x01' and core_bytes[18:20]==b'\xb7\x00','Core ELF64 AArch64 required')
    core_report=json.loads((core_output/'native-build1950.json').read_text())
    require(core_report.get('schema')=='ulike-native1950-v1' and core_report.get('sha256')==core_digest and core_report.get('bytes')==len(core_bytes) and core_report.get('physical_android_tested') is False,'Core native build report differs')
    core_sources={name:sha((ROOT/'native1950'/name).read_bytes()) for name in core_report['sources']}
    require(core_sources==core_report['sources'],'Core native sources changed after build')
    classes=args.work/'classes';helpers=args.work/'helper-classes';dex=args.work/'helper-dex'
    for p in [classes,helpers,dex]:p.mkdir()
    stub_map={}
    for folder in ['front1936-stubs','renderer1938-stubs','capture-stubs','fast-stubs','metadata-stubs','quality-stubs','quality-dependencies','timing1947-stubs','core1950-stubs']:
        path=ROOT/folder
        if path.exists():
            for p in sorted(path.rglob('*.java')):stub_map[str(p.relative_to(path))]=p
    # Dependencies compile only; D8 receives exactly the three reviewed GPU production roots.
    # Every other runtime class remains byte-identical to the pinned .49 payload.
    for retained in ['AsyncSave1935','ProcessingTiming1947','QualityPipeline1932','ShotContext1932','TimedIo1947','QualityPixels1932','QualityShadow1932','SpeedWorkers1935','SaveQueue1935','FusionPixels1933','ReflectionCache1945','CodecDrain1945','NoiseCache1944','NativeSpeed1944','PolicyCache1945','Scheduling1944','GpuInteger1949']:
        stub_map['com/hiro/ulike/'+retained+'.java']=ROOT/(retained+'.java')
    for n in names:stub_map.pop('com/hiro/ulike/'+n+'.java',None)
    run(['javac','-source','8','-target','8','-encoding','UTF-8','-bootclasspath',args.tools/'android.jar','-d',helpers,*stub_map.values(),*production],args.work/'helper-javac.log')
    selected=sorted(p for p in (helpers/'com/hiro/ulike').glob('*.class') if any(p.name==n+'.class' or p.name.startswith(n+'$') for n in names))
    require(all(helpers/'com/hiro/ulike'/(n+'.class') in selected for n in names),'Missing compiled production root')
    run(['java','-cp',args.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',args.tools/'android.jar','--classpath',helpers,'--output',dex,*selected],args.work/'d8.log')
    cp=os.pathsep.join(map(str,[args.tools/'morphe.jar',baseline,classes]))
    transformer_names=['MergePayloads','Transform1950','Verify1950','VerifyHelperReferences']
    transformer_names+=['CoreHooks1950','CorrectionHooks1950']
    transformer_sources=[ROOT/(n+'.java') for n in transformer_names]+[ROOT/'PatchClass1950.java',ROOT/'PatchGpuLoader1950.java']
    transformer_hashes={p.name:sha(p.read_bytes()) for p in transformer_sources}
    run(['javac','-encoding','UTF-8','-cp',cp,'-d',classes,*[ROOT/(n+'.java') for n in transformer_names]],args.work/'transform-javac.log')
    run(['javac','-encoding','UTF-8','--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','--add-exports','java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED','-cp',cp,'-d',classes,ROOT/'PatchClass1950.java',ROOT/'PatchGpuLoader1950.java'],args.work/'metadata-javac.log')
    properties=['-Dulike.production='+','.join(names),'-Dulike.gpu.sha='+gpu_digest,'-Dulike.gpu.bytes='+str(len(gpu_bytes)),'-Dulike.gpu.oldsha='+sha(base[GPU_ENTRY]),'-Dulike.gpu.oldbytes='+str(len(base[GPU_ENTRY])),'-Dulike.core.sha='+core_digest,'-Dulike.core.bytes='+str(len(core_bytes))]
    for n in ['emitted','repeat']:
        run(['java',*properties,'-cp',cp,'Transform1950',baseline,dex/'classes.dex',bundle_dex,args.work/n,args.work/(n+'-audit.tsv')],args.work/(n+'.log'))
    emitted=args.work/'emitted'
    require({p.name:p.read_bytes() for p in emitted.iterdir()}=={p.name:p.read_bytes() for p in (args.work/'repeat').iterdir()},'DEX transform is not deterministic')
    run(['java','--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','-cp',cp,'PatchClass1950',baseline/'app/hiro/ulike/patches/UlikeHqMaxPatch.class',emitted/'UlikeHqMaxPatch.class'],args.work/'metadata.log')
    run(['java',*properties,'--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','--add-exports','java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED','-cp',cp,'PatchGpuLoader1950',baseline/'app/hiro/ulike/patches/IntegrationPayload186.class',gpu,core,emitted/'IntegrationPayload186.class'],args.work/'gpu-installer-metadata.log')
    verification=run(['java',*properties,'-cp',cp,'Verify1950',baseline/'ulike/runtime.dex',emitted/'runtime.dex',dex/'classes.dex'],args.work/'optimization-dex-verification.txt')
    references=run(['java','-cp',cp,'VerifyHelperReferences',baseline/'ulike/methods.dex',baseline/'ulike/runtime.dex',emitted/'methods.dex',emitted/'runtime.dex'],args.work/'helper-references.txt')
    require(references.startswith('PASS helper references in '),'Executed helper reference verification missing')
    spec=importlib.util.spec_from_file_location('optimization_tests',ROOT/'host_regression1950.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    host_sources=host_source_pins()
    result=module.test(ROOT,args.work,args.tools/'android.jar')
    require(host_sources==host_source_pins(),'Sources changed during executed host regression')
    require(result.get('status')=='passed' and result.get('assertions',0)>0,'Executed optimization and preservation tests must pass')
    require(set(result.get('suites',{})) >= {'baseline_camera_quality_save_timing','exact_temporal_fusion','capture_preprocessing_lifecycle','primary_denoise_native_exact','fused_gpu_finishing_exact','gpu_runtime_admission1950','exact_correction_copy_elision'} and result.get('pixel_equivalence_to_baseline') is True and result.get('full_resolution_nv21_checked') == '4080x3060','Executed H1-H5 pixel-exact and full-resolution preservation evidence required')
    require(result['suites']['primary_denoise_native_exact'].get('production_c_jni_executed') is True and result['suites']['fused_gpu_finishing_exact'].get('software_egl_shaders_executed') is True,'Actual native denoise and fused shader execution required')
    require(source_hashes=={p.name:sha(p.read_bytes()) for p in production},'Production sources changed during build')
    require(timing_source_sha==sha(timing_source.read_bytes()), 'Version-only timing source changed during build')
    require(transformer_hashes=={p.name:sha(p.read_bytes()) for p in transformer_sources},'Transformer sources changed during build')
    require(native_sources=={name:sha((ROOT/'native1949'/name).read_bytes()) for name in native_sources},'GPU native sources changed during build')
    require(core_sources=={name:sha((ROOT/'native1950'/name).read_bytes()) for name in core_sources},'Core native sources changed during build')
    inventory=json.loads((emitted/'optimization-inventory.json').read_text())
    final=dict(base)
    final.update({GPU_ENTRY:gpu_bytes,CORE_ENTRY:core_bytes,'app/hiro/ulike/patches/IntegrationPayload186.class':(emitted/'IntegrationPayload186.class').read_bytes(),'classes.dex':(emitted/'loader.dex').read_bytes(),'ulike/runtime.dex':(emitted/'runtime.dex').read_bytes(),'app/hiro/ulike/patches/UlikeHqMaxPatch.class':(emitted/'UlikeHqMaxPatch.class').read_bytes()})
    fields=headers(base['META-INF/MANIFEST.MF']);fields.update(Version=VERSION,Timestamp='2026-10-08T06:00:00',Description='H1-H5 exact native denoise and fused GPU noise finishing; batched transfers/shared buffers/reused validated admission. Exact baseline arithmetic, resolution, correction, compression and stage timing retained; physical Android speed unmeasured.')
    final['META-INF/MANIFEST.MF']=manifest(fields)
    integrated=dict(bundle);integrated.update({n:b for n,b in final.items() if own(n)});integrated['classes.dex']=(emitted/'bundle-loader.dex').read_bytes()
    fields=headers(bundle['META-INF/MANIFEST.MF']);fields.update(Version=BUNDLE_VERSION,Timestamp='2026-10-08T06:00:00',Description='ULike1.9.50 H1-H5 native denoise and fused GPU finishing; all other applications retained from1.0.182; exact CPU fallback; stage timing retained; device unverified.')
    integrated['META-INF/MANIFEST.MF']=manifest(fields)
    allowed={GPU_ENTRY,'classes.dex','ulike/runtime.dex','META-INF/MANIFEST.MF','app/hiro/ulike/patches/UlikeHqMaxPatch.class','app/hiro/ulike/patches/IntegrationPayload186.class'}
    changes=sorted(n for n in base if final[n]!=base[n])
    require(set(final)==set(base)|{CORE_ENTRY} and set(changes)<=allowed,'Unexpected archive resource delta')
    require(set(integrated)==set(bundle)|{CORE_ENTRY},'Integrated archive inventory differs')
    require(all(integrated[n]==b for n,b in bundle.items() if not own(n) and n not in ['classes.dex','META-INF/MANIFEST.MF']),'Other application bytes changed')
    native='ulike1935/runtime/libulike_speed1935.so';installer='app/hiro/ulike/patches/IntegrationPayload186.class'
    for n in [native,'ulike/methods.dex','ulike/methods.tsv']:require(final[n]==base[n],'Protected binary payload changed: '+n)
    artifacts={}
    for name,entries in [(SINGLE,final),('Hiro_Morphe_Patches_v'+BUNDLE_VERSION+'.mpp',integrated)]:
        p=args.output/name;write_zip(p,entries);require(archive(p)==entries,'Output archive roundtrip failed')
        run(['java','-jar',args.tools/'morphe.jar','list-patches','--patches',p],args.work/(name+'.loader.log'))
        artifacts[name]={'sha256':sha(p.read_bytes()),'bytes':p.stat().st_size}
    qa={'schema':'ulike1950-optimization-v1','status':'BUILT_HOST_VERIFIED_DEVICE_UNVERIFIED','ulike_version':VERSION,'bundle_version':BUNDLE_VERSION,'base_versions':[BASE_VERSION,BASE_BUNDLE_VERSION],'approved_lineage':'1.8.8','selected_candidates':SELECTED,'artifacts':artifacts,'input_sha256':pins,'jdk_identity':identity,
        'device_tested':False,'device_quality_verified':False,'original_apk_apply_tested':False,'ci_android_apply_tested':False,
        'non_ulike_resources_byte_identical':True,'non_ulike_loader_classes_unchanged':True,'standalone_and_bundle_ulike_resources_identical':True,
        'quality_algorithm_and_settings_preserved':True,'host_pixel_equivalence_passed':True,'stage_timing_payload_byte_identical':False,'stage_timing_algorithm_preserved':True,'timing_version_only_change':True,'device_save_speed_measured':False,'actual_sensor_capture_latency_measured':False,'resolution_and_save_format_preserved':True,'save_format_and_codec_configuration_preserved':True,'manual_capture_safety_checks_preserved':True,
        'native_library_byte_identical':True,'native_installer_byte_identical':False,'native_installer_existing_rows_preserved':False,'native_installer_legacy_cpu_rows_preserved':True,'native_installer_baseline_gpu_row_updated':True,'native_methods_byte_identical':True,'native_methods_tsv_byte_identical':True,
        'native_library_sha256':sha(final[native]),'native_library_bytes':len(final[native]),
        'host_quality_passed':True,'executed_host_source_sha256':host_sources,'host_quality_result':result,'host_quality_result_sha256':sha(json.dumps(result,sort_keys=True).encode()),
        'production_source_consistency_verified':True,'compiled_production_source_sha256':source_hashes,'timing_metadata_source_sha256':{'ProcessingTiming1947.java':timing_source_sha},'compiled_transformer_source_sha256':transformer_hashes,
        'gpu_processing_added':True,'gpu_policy':'H1-H5 exact finishing with transfer-inclusive speed admission and CPU fallback','gpu_shader_execution_on_host':True,'gpu_execution_on_physical_android':False,'gpu_native_library_sha256':gpu_digest,'gpu_native_library_bytes':len(gpu_bytes),'gpu_native_build':native_report,'compiled_gpu_native_source_sha256':native_sources,'diagnostic_report_updates_on_success':True,'legacy_report_not_displayed_as_current':True,'diagnostic_overlapping_wall_clock':True,'diagnostic_per_shot_owner':True,'diagnostic_stage_names':['fusion','noise','correction','compression','save'],
        'core_native_library_sha256':core_digest,'core_native_library_bytes':len(core_bytes),'core_native_build':core_report,'compiled_core_native_source_sha256':core_sources,
        'changed_standalone_entries':changes,'added_standalone_entries':[CORE_ENTRY],'changed_bundle_entries':sorted(n for n in bundle if integrated[n]!=bundle[n]),'added_bundle_entries':[CORE_ENTRY],
        'dex_verification':verification.strip(),'published':False,'manager_feed_updated':False,
        'limitations':['No Android/Galaxy camera execution.','Stage intervals may overlap and do not sum to shutter-to-save elapsed time.','Host equivalence and reduced work do not establish a device speedup.','Unmeasured or skipped stages are labeled explicitly.']}
    qa.update(inventory)
    (args.output/'QA_ULike_v1.9.50.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2,sort_keys=True)+'\n')
    (args.output/'native-build1950.json').write_text(json.dumps(core_report,ensure_ascii=False,indent=2,sort_keys=True)+'\n')
    (args.output/'native-build1949.json').write_text(json.dumps(native_report,ensure_ascii=False,indent=2,sort_keys=True)+'\n')
    shutil.copyfile(native_output/'native1949-readelf.txt',args.output/'native1949-readelf.txt')
    for n in ['emitted-audit.tsv','optimization-dex-verification.txt','helper-references.txt']:shutil.copyfile(args.work/n,args.output/n)
    shutil.copyfile(emitted/'optimization-inventory.json',args.output/'optimization-inventory.json')
    (args.output/'host-regression1950-result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2,sort_keys=True)+'\n')
    print(json.dumps({'status':qa['status'],'artifacts':artifacts,'host_quality_result':result},ensure_ascii=False,indent=2))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    for n in ['input','tools','work','output']:parser.add_argument('--'+n,type=Path,required=True)
    parser.add_argument('--jdk',type=Path);parser.add_argument('--ndk',type=Path,required=True);args=parser.parse_args()
    if args.jdk:os.environ['ULIKE_JDK_HOME']=str(args.jdk.resolve());os.environ['PATH']=str(args.jdk.resolve()/'bin')+os.pathsep+os.environ.get('PATH','')
    for k,v in vars(args).items():
        if v is not None:setattr(args,k,v.resolve())
    build(args)
