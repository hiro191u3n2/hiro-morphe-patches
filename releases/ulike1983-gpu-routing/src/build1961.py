#!/usr/bin/env python3
"""Incremental GX11-GX23 build from the exact published .60/.193 packages.

GPU support is conditional on result-equivalence/capability checks. All eleven
inherited native payloads, camera classes, encoding configuration and unrelated
application resources are preserved. No physical-device speed/quality claim.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, shutil, struct, zlib
import build1945 as common
ROOT=Path(__file__).resolve().parent
VERSION='1.9.61';BASE_VERSION='1.9.60';BUNDLE_VERSION='1.0.194';BASE_BUNDLE_VERSION='1.0.193'
SINGLE='ULike_HQ_Texture_Online_v1.9.61.mpp';BASE_SINGLE='ULike_HQ_Texture_Online_v1.9.60.mpp'
BUNDLE='Hiro_Morphe_Patches_v1.0.194.mpp';BASE_BUNDLE='Hiro_Morphe_Patches_v1.0.193.mpp'
BASE_SINGLE_SHA256='85bff5ab27fea4ceb02f6ad78810dbf36b6ff8c8c321cad5575239d5e24e352d'
BASE_BUNDLE_SHA256='354e0ed5bdcd686ec2ed7a9ce45ab3b703a04a6bd6327d0434570f8a6ddcbc0c'
BASE_SINGLE_BYTES=1080755;BASE_BUNDLE_BYTES=17727736
SELECTED=['GX'+str(i) for i in range(11,24)]
GX_STATUS={'GX11': 'conditional_gpu_spatial_and_strong_analysis', 'GX12': 'conditional_gpu_finish_integer_budgets_and_smooth_masks', 'GX13': 'partial_gpu_single_residual_reconstruction_fp64_cpu_retained', 'GX14': 'partial_gpu_face_pixel_skin_range_integer_protection_face_detector_double_geometry_retained', 'GX15': 'conditional_gpu_exact_halo_tiled_large_resize', 'GX16': 'conditional_gpu_nlm_shared_pixels_luma_chroma_cache', 'GX17': 'conditional_gpu_exact_integer_comparison', 'GX18': 'conditional_batch_jni_owner_dispatch_and_one_read_fence', 'GX19': 'conditional_strong_two_bank_overlap', 'GX20': 'partial_resident_pyramid_evidence_preparation_and_connected_supported_stages', 'GX21': 'conditional_bounded_gpu_and_upload_staging_reuse', 'GX22': 'conditional_exact_workgroup_and_tile_variants', 'GX23': 'conditional_background_two_exact_five_percent_persistent_device_driver_app_source_qualification'}
PRODUCTION=['QualityPipeline1932', 'SingleNoise1955', 'StrongNoise1958', 'GpuNoise1960', 'GpuPolicy1960', 'GpuStrong1960', 'GpuSingle1960', 'GpuGeometry1960', 'FastResize1933', 'ProcessingTiming1947', 'GpuAnalysis1961', 'SpatialNoise1934', 'GpuProtection1961', 'FinishPolicy1953', 'FaceRegions1934Pixels', 'SingleResidual1961', 'GpuResidual1961', 'GpuQualification1961', 'GpuChain1961']
GPU_ENTRY='ulike1960/runtime/libulike_gpu1960.so'
INSTALLER='app/hiro/ulike/patches/IntegrationPayload186.class';LOADER='app/hiro/ulike/patches/UlikeHqMaxPatch.class'
CHANGED={'META-INF/MANIFEST.MF','classes.dex','ulike/runtime.dex',INSTALLER,LOADER,GPU_ENTRY};ADDED=set()
JNI_NAMES=['nativeAbi','openNative','allocateNative','uploadIntsNative','uploadFloatsNative','dispatchNative','readIntsNative','closeNative','environmentNative','retainedNative','supportedNative','batchNative','executeNative','submitNative','readManyNative','trimNative','ticketReadyNative','timeoutNative','workgroupNative']
NEW_JNI=['Lcom/hiro/ulike/GpuNoise1960;->'+n+d for n,d in [('batchNative','(J[I[I[J[I[Ljava/lang/Object;[[I[[I[[F[I)Z'),('executeNative','(J[I[I[J[I[Ljava/lang/Object;[[I[[I[[F[I[I[I)[[I'),('submitNative','(JI[I[I[J[I[Ljava/lang/Object;[[I[[I[[F[I)J'),('readManyNative','(JJ[I[I)[[I'),('trimNative','()Z'),('ticketReadyNative','(JJ)I'),('timeoutNative','(J)Z'),('workgroupNative','(I)I')]]+['Lcom/hiro/ulike/SingleResidual1961;->prepareNative([IIIIIIIIIZIIIII[F)[F']
TOOL_PINS=common.TOOL_PINS
require,sha,run,verify_jdk=common.require,common.sha,common.run,common.verify_jdk
archive,headers,manifest,own,write_zip=common.archive,common.headers,common.manifest,common.own,common.write_zip

def production_path(name):
 path=ROOT/(name+'.java')
 if path.is_file():return path
 path=ROOT/'quality-dependencies/com/hiro/ulike'/(name+'.java')
 require(path.is_file(),'Missing production source '+name);return path

def json_bytes(data):return (json.dumps(data,ensure_ascii=False,sort_keys=True,indent=2)+'\n').encode()
def source_pins():
 return {p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in sorted(ROOT.rglob('*')) if p.is_file() and '__pycache__' not in p.parts and p.suffix in ('.java','.py','.c','.h','.comp','.sh','.txt','.json','.dex','.tsv')}
def dex_integrity(raw,name):
 require(len(raw)>=112 and raw[:4]==b'dex\n' and struct.unpack_from('<I',raw,32)[0]==len(raw),'DEX header invalid: '+name)
 require(raw[12:32]==hashlib.sha1(raw[32:]).digest() and struct.unpack_from('<I',raw,8)[0]==zlib.adler32(raw[12:])&0xffffffff,'DEX integrity invalid: '+name)
def resource_delta(before,after,label):
 require(set(before)<=set(after),'Removed '+label+' resource');changed={n for n in before if before[n]!=after[n]};added=set(after)-set(before)
 require(changed==CHANGED and added==ADDED,'Unexpected '+label+' resource delta '+repr((sorted(changed),sorted(added))));return sorted(changed),sorted(added)
def load_module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod);return mod

def build(args):
 identity=verify_jdk();declaration=json.loads((ROOT.parent/'manifest.json').read_text());require(declaration.get('reviewed_source_sha256')==source_pins(),'Reviewed source graph changed before build');require(declaration.get('reviewed_publication_sha256')=={n:sha((ROOT.parent/'publication'/n).read_bytes()) for n in ('publish1961.py','ulike1961-gx11-gx23-publish.yml','README.md')},'Reviewed publication graph changed before build');require(not args.work.exists(),'Fresh build directory required');args.work.mkdir(parents=True);args.output.mkdir(parents=True,exist_ok=True)
 pins={BASE_SINGLE:BASE_SINGLE_SHA256,BASE_BUNDLE:BASE_BUNDLE_SHA256,**TOOL_PINS}
 for name,digest in pins.items():
  path=(args.tools if name.endswith('.jar') else args.input)/name;require(sha(path.read_bytes())==digest,'Pinned input differs: '+name)
 require((args.input/BASE_SINGLE).stat().st_size==BASE_SINGLE_BYTES and (args.input/BASE_BUNDLE).stat().st_size==BASE_BUNDLE_BYTES,'Pinned input byte sizes differ')
 base=archive(args.input/BASE_SINGLE);bundle=archive(args.input/BASE_BUNDLE)
 require(headers(base['META-INF/MANIFEST.MF'])['Version']==BASE_VERSION and headers(bundle['META-INF/MANIFEST.MF'])['Version']==BASE_BUNDLE_VERSION,'Wrong baseline version')
 require({n:b for n,b in base.items() if own(n)}=={n:b for n,b in bundle.items() if own(n)},'Standalone and integrated baselines differ')
 require(json.loads((ROOT/'production1961.json').read_text())==PRODUCTION,'Reviewed production roots differ')
 production=[production_path(n) for n in PRODUCTION];compiled_sources={p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in production}
 baseline=args.work/'baseline'
 for name,data in base.items():
  path=baseline/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
 bundle_dex=baseline/'bundle.dex';bundle_dex.write_bytes(bundle['classes.dex'])
 require(re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',(args.ndk/'source.properties').read_text(),re.M),'Pinned NDK r27c required')
 native_output=args.work/'gpu-native'
 run(['python3',ROOT/'native1960/build_native1960.py','--ndk',args.ndk,'--out',native_output],args.work/'native1960-build.log')
 native=native_output/'libulike_gpu1960.so';native_bytes=native.read_bytes()
 require(native_bytes[:6]==b'\x7fELF\x02\x01' and native_bytes[18:20]==b'\xb7\x00','ELF64 AArch64 GPU payload required')
 ndk_bin=args.ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin'
 elf=run([ndk_bin/'llvm-readelf','-h','-d','-Ws','--program-headers','--wide',native],args.work/'gpu-native-readelf.txt')
 exports=['Java_com_hiro_ulike_GpuNoise1960_'+n for n in JNI_NAMES]+['Java_com_hiro_ulike_SingleResidual1961_prepareNative']
 require('AArch64' in elf and all(n in elf for n in exports),'GPU native JNI linkage differs')
 alignments=[int(line.split()[-1],0) for line in elf.splitlines() if line.strip().startswith('LOAD ')];needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',elf)
 require(alignments and all(a>=16384 for a in alignments) and set(needed)<= {'libc.so','libm.so','libdl.so','libEGL.so','libGLESv3.so'},'GPU native page alignment or dependency differs')
 native_sources={p.relative_to(ROOT/'native1960').as_posix():sha(p.read_bytes()) for p in sorted((ROOT/'native1960').rglob('*')) if p.is_file() and p.suffix in ('.c','.h','.comp','.py')}
 native_report={'library':native.name,'sha256':sha(native_bytes),'bytes':len(native_bytes),'ndk_revision':'27.2.12479018','native_abi':19601,'abi':'arm64-v8a','min_sdk':26,'physical_android_tested':False,'sources':native_sources,'jni_exports':exports,'load_segment_alignments':alignments,'needed_libraries':needed}
 current_pins=source_pins()
 classes=args.work/'classes';helpers=args.work/'helper-classes';dex=args.work/'helper-dex'
 for p in (classes,helpers,dex):p.mkdir()
 stub_map={}
 for folder in ['front1936-stubs','renderer1938-stubs','capture-stubs','fast-stubs','metadata-stubs','quality-stubs','quality-dependencies','timing1947-stubs','core1950-stubs']:
  path=ROOT/folder
  if path.exists():
   for p in sorted(path.rglob('*.java')):stub_map[str(p.relative_to(path))]=p
 retained=['StrongNoise1957','AsyncSave1935','ProcessingTiming1947','QualityPipeline1932','ShotContext1932','TimedIo1947','QualityPixels1932','QualityShadow1932','SpeedWorkers1935','SaveQueue1935','FusionPixels1933','ReflectionCache1945','CodecDrain1945','NoiseCache1944','NativeSpeed1944','PolicyCache1945','Scheduling1944','GpuInteger1949','CorePixels1950','NativeMoire1951','GpuFinish1952','WholeRoute1952','PerformanceHints1952','GpuFinish1953','FinishPolicy1953','WholeRoute1953','SpatialNoise1934','SingleNoise1955','StrongNoise1958']
 for name in retained:stub_map['com/hiro/ulike/'+name+'.java']=ROOT/(name+'.java')
 for name in PRODUCTION:stub_map.pop('com/hiro/ulike/'+name+'.java',None)
 run(['javac','-source','8','-target','8','-encoding','UTF-8','-bootclasspath',args.tools/'android.jar','-d',helpers,*stub_map.values(),*production],args.work/'helper-javac.log')
 selected=sorted(p for p in (helpers/'com/hiro/ulike').glob('*.class') if any(p.name==n+'.class' or p.name.startswith(n+'$') for n in PRODUCTION))
 require(all(helpers/'com/hiro/ulike'/(n+'.class') in selected for n in PRODUCTION),'Missing production helper root')
 run(['java','-cp',args.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',args.tools/'android.jar','--classpath',helpers,'--output',dex,*selected],args.work/'d8.log')
 cp=os.pathsep.join(map(str,[args.tools/'morphe.jar',baseline,classes]));transformers=['MergePayloads','Transform1961','VerifyHelperReferences']
 transformer_sources=[ROOT/(n+'.java') for n in transformers]+[ROOT/'PatchClass1961.java',ROOT/'PatchLoader1961.java'];transformer_pins={p.name:sha(p.read_bytes()) for p in transformer_sources}
 run(['javac','-encoding','UTF-8','-cp',cp,'-d',classes,*[ROOT/(n+'.java') for n in transformers]],args.work/'transform-javac.log')
 exports_args=['--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','--add-exports','java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED']
 run(['javac','-encoding','UTF-8',*exports_args,'-cp',cp,'-d',classes,ROOT/'PatchClass1961.java',ROOT/'PatchLoader1961.java'],args.work/'metadata-javac.log')
 properties=['-Dulike.newjni='+','.join(NEW_JNI),'-Dulike.production='+','.join(PRODUCTION),'-Dulike.gpu1960.sha='+sha(native_bytes),'-Dulike.gpu1960.bytes='+str(len(native_bytes))]
 for name in ['emitted','repeat']:run(['java',*properties,'-cp',cp,'Transform1961',baseline,dex/'classes.dex',bundle_dex,args.work/name,args.work/(name+'-audit.tsv')],args.work/(name+'.log'))
 emitted=args.work/'emitted';require({p.name:p.read_bytes() for p in emitted.iterdir()}=={p.name:p.read_bytes() for p in (args.work/'repeat').iterdir()},'DEX transform is not deterministic')
 run(['java',*exports_args,'-cp',cp,'PatchClass1961',baseline/LOADER,emitted/'UlikeHqMaxPatch.class'],args.work/'metadata.log')
 run(['java',*exports_args,'-cp',cp,'PatchLoader1961',baseline/INSTALLER,native,emitted/'IntegrationPayload186.class'],args.work/'native-installer-metadata.log')
 refs=run(['java','-cp',cp,'VerifyHelperReferences',baseline/'ulike/methods.dex',baseline/'ulike/runtime.dex',emitted/'methods.dex',emitted/'runtime.dex'],args.work/'helper-references.txt');require(refs.startswith('PASS helper references in '),'Changed helper linkage proof required')
 verification=(args.work/'emitted.log').read_text();require('begin_image_false_return_verified=true' in verification and 'serialized_dex_verified=true' in verification,'Capture/serialized preservation proof required')
 final=dict(base);integrated=dict(bundle)
 payloads={'ulike/runtime.dex':(emitted/'runtime.dex').read_bytes(),LOADER:(emitted/'UlikeHqMaxPatch.class').read_bytes(),INSTALLER:(emitted/'IntegrationPayload186.class').read_bytes(),GPU_ENTRY:native_bytes}
 for name,data in payloads.items():final[name]=data;integrated[name]=data
 final['classes.dex']=(emitted/'loader.dex').read_bytes();integrated['classes.dex']=(emitted/'bundle-loader.dex').read_bytes()
 fields=headers(base['META-INF/MANIFEST.MF']);fields['Version']=VERSION;fields['Description']='GX11-GX23 conditional GPU routes; exact-output guards and CPU fallback; physical device unverified.';final['META-INF/MANIFEST.MF']=manifest(fields)
 fields=headers(bundle['META-INF/MANIFEST.MF']);fields['Version']=BUNDLE_VERSION;fields['Description']='ULike GX11-GX23 conditional GPU routes; other app payloads retained; physical device unverified.';integrated['META-INF/MANIFEST.MF']=manifest(fields)
 single_delta=resource_delta(base,final,'standalone');bundle_delta=resource_delta(bundle,integrated,'bundle')
 require({n:b for n,b in final.items() if own(n)}=={n:b for n,b in integrated.items() if own(n)},'Current standalone and bundle ULike copies differ')
 require(all(integrated[n]==data for n,data in bundle.items() if not own(n) and n not in ('classes.dex','META-INF/MANIFEST.MF')),'Other app resource changed')
 inherited_native={n:{'sha256':sha(b),'bytes':len(b)} for n,b in base.items() if (n.endswith('.so') or n=='ulike186/runtime/0000.bin') and n!=GPU_ENTRY}
 require(len(inherited_native)==11 and all(final[n]==base[n] for n in inherited_native),'All eleven inherited native libraries must be byte preserved')
 require(all(final[n]==base[n] for n in ['ulike/methods.dex','ulike/methods.tsv']),'Legacy native method payloads differ')
 for n in ['ulike/runtime.dex','classes.dex']:dex_integrity(final[n],n)
 dex_integrity(integrated['classes.dex'],'bundle classes.dex');write_zip(args.output/SINGLE,final);write_zip(args.output/BUNDLE,integrated)
 for name in [SINGLE,BUNDLE]:run(['java','-jar',args.tools/'morphe.jar','list-patches','--patches',args.output/name],args.work/(name+'.loader.log'))
 if args.prepare_only:
  print(json.dumps({'status':'PREPARE_ONLY_NOT_FOR_PUBLICATION','host_tests_run':False,'work':str(args.work),'output':str(args.output)},indent=2));return None
 os.environ['ULIKE_MORPHE_JAR']=str(args.tools/'morphe.jar');os.environ['ULIKE_NDK_HOME']=str(args.ndk)
 host=load_module('host_gpu1961_build',ROOT/'host_gpu1961.py');result=host.test(ROOT,args.work/'gpu-host',jdk=args.jdk,ndk=args.ndk)
 require(isinstance(result,dict) and result.get('status')=='passed' and type(result.get('assertions')) is int and result['assertions']>0,'Fresh GPU host test result required')
 require(result.get('gpu_shader_execution_on_host') is True and result.get('host_pixel_equivalence_to_baseline') is True,'Actual host GPU and exact baseline-output evidence required')
 require(compiled_sources=={p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in production} and current_pins==source_pins(),'Source changed during compilation/test')
 inventory=json.loads((emitted/'gpu-inventory1961.json').read_text());artifacts={n:{'sha256':sha((args.output/n).read_bytes()),'bytes':(args.output/n).stat().st_size} for n in (SINGLE,BUNDLE)}
 qa={'schema':'ulike1961-gx11-gx23-v1','status':'MPP_INCREMENTAL_GX_GPU_HOST_VERIFIED_ORIGINAL_APKS_AND_DEVICE_UNVERIFIED','ulike_version':VERSION,'bundle_version':BUNDLE_VERSION,'baseline_ulike_version':BASE_VERSION,'baseline_bundle_version':BASE_BUNDLE_VERSION,'selected_candidates':SELECTED,'gx_implementation_status':GX_STATUS,'input_sha256':pins,'artifacts':artifacts,'jdk_identity':identity,'approved_lineage':'ULike1.8.8','host_quality_passed':True,'host_quality_result':result,'host_quality_result_sha256':sha(json.dumps(result,sort_keys=True).encode()),'host_pixel_equivalence_to_baseline':True,'intentional_quality_algorithm_change':False,'capture_fusion_enabled':False,'capture_begin_image_false_return_verified':True,'capture_class_bytecode_identical':True,'production_source_consistency_verified':True,'compiled_production_source_sha256':compiled_sources,'compiled_transformer_source_sha256':transformer_pins,'executed_host_source_sha256':current_pins,'compiled_gpu_native_source_sha256':native_sources,'gpu_native_library_sha256':sha(native_bytes),'gpu_native_library_bytes':len(native_bytes),'gpu_native_build':native_report,'gpu_native_jni_exports':exports,'gpu_native_load_segment_alignments':alignments,'gpu_native_needed_libraries':needed,'inherited_native_payloads':inherited_native,'inherited_native_libraries_byte_identical':True,'native_methods_byte_identical':True,'native_methods_tsv_byte_identical':True,'native_installer_existing_rows_preserved':True,'native_installer_existing_row_count_preserved':True,'native_installer_appended_rows':0,'native_installer_updated_rows':1,'native_installer_preserved_rows':11,'native_installer_total_rows':12,'native_installer_inverse_verified':True,'unrelated_runtime_classes_bytecode_identical':True,'non_ulike_loader_classes_unchanged':True,'non_ulike_resources_byte_identical':True,'standalone_and_bundle_ulike_resources_identical':True,'resolution_and_save_format_preserved':True,'save_publication_hooks_preserved':True,'save_format_and_codec_configuration_preserved':True,'stage_timing_algorithm_preserved':True,'timing_version_only_change':False,'timing_new_idle_gpu_qualification_hooks':True,'timing_stage_core_source_inverse_verified':True,'diagnostic_stage_names':['fusion','noise','correction','compression','save'],'gpu_processing_added':True,'gpu_shader_execution_on_host':True,'gpu_execution_on_physical_android':False,'all_processing_on_gpu':False,'gx9_beauty_interop_supported':False,'gx10_encoder_interop_supported':False,'original_apk_apply_tested':False,'original_split_merge_tested':False,'ci_android_apply_tested':False,'device_tested':False,'device_quality_verified':False,'device_save_speed_measured':False,'changed_standalone_entries':single_delta[0],'added_standalone_entries':single_delta[1],'changed_bundle_entries':bundle_delta[0],'added_bundle_entries':bundle_delta[1],**inventory,'published':False,'manager_feed_updated':False}
 (args.output/'QA_ULike_v1.9.61.json').write_bytes(json_bytes(qa));(args.output/'host-regression1961-result.json').write_bytes(json_bytes(result));(args.output/'native-build1961.json').write_bytes(json_bytes(native_report))
 for name in ['emitted-audit.tsv','helper-references.txt','emitted.log','metadata.log','native-installer-metadata.log','gpu-native-readelf.txt']:shutil.copyfile(args.work/name,args.output/name)
 shutil.copyfile(emitted/'gpu-inventory1961.json',args.output/'gpu-inventory1961.json');print(json.dumps({'status':qa['status'],'artifacts':artifacts,'host_assertions':result['assertions']},ensure_ascii=False,indent=2));return qa

if __name__=='__main__':
 parser=argparse.ArgumentParser(description=__doc__)
 for name in ['input','tools','work','output']:parser.add_argument('--'+name,type=Path,required=True)
 parser.add_argument('--jdk',type=Path);parser.add_argument('--ndk',type=Path,required=True);parser.add_argument('--prepare-only',action='store_true',help='Compile provisional packages only; no QA or publishable evidence');args=parser.parse_args()
 if args.jdk:os.environ['ULIKE_JDK_HOME']=str(args.jdk.resolve());os.environ['PATH']=str(args.jdk.resolve()/'bin')+os.pathsep+os.environ.get('PATH','')
 for key,value in vars(args).items():
  if isinstance(value,Path):setattr(args,key,value.resolve())
 build(args)
