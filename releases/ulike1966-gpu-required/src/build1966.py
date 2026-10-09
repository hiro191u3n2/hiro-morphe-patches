#!/usr/bin/env python3
"""Build mandatory GPU noise/protection/correction on exact .65/.198 packages.

GPU math failures abort processing instead of CPU or untreated-output fallback.
All eleven inherited native payloads, camera classes and unrelated apps remain.
Host tests are evidence of code behavior, not physical Android speed or quality.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, shutil, struct, zlib
import build1945 as common
ROOT=Path(__file__).resolve().parent
VERSION='1.9.66';BASE_VERSION='1.9.65';BUNDLE_VERSION='1.0.199';BASE_BUNDLE_VERSION='1.0.198'
SINGLE='ULike_HQ_Texture_Online_v1.9.66.mpp';BASE_SINGLE='ULike_HQ_Texture_Online_v1.9.65.mpp'
BUNDLE='Hiro_Morphe_Patches_v1.0.199.mpp';BASE_BUNDLE='Hiro_Morphe_Patches_v1.0.198.mpp'
BASE_SINGLE_SHA256='4b4ea78a8f3ee6d959d6f89585296061bc87454db67debeae9b1541a60a6ecfa'
BASE_BUNDLE_SHA256='b7853baeb38b435ff68cfb0fd249f903401fc354d473e98790b52a459c176c81'
BASE_SINGLE_BYTES=1180426;BASE_BUNDLE_BYTES=17827397
ORACLE_SINGLE='ULike_HQ_Texture_Online_v1.9.64.mpp'
ORACLE_SINGLE_SHA256='cf6621ed1ec55c5b115516a04f897e380c785c56d9772ab48043b32c568ae48b'
ORACLE_SINGLE_BYTES=1163379
ORACLE_SINGLE_URL='https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/9ade3adfec36a8b1058f26c59d6e7f61b5428e6b/downloads/'+ORACLE_SINGLE
PIXEL_ORACLE={'ulike_version':'1.9.64','filename':ORACLE_SINGLE,'sha256':ORACLE_SINGLE_SHA256,'bytes':ORACLE_SINGLE_BYTES,'url':ORACLE_SINGLE_URL}
SELECTED=['GPU_REQUIRED_NOISE','GPU_REQUIRED_PROTECTION','GPU_REQUIRED_CORRECTION','GPU_REQUIRED_RECOVERY','GPU_REQUIRED_DIAGNOSTICS']
GX_STATUS={name:'mandatory_gpu_or_explicit_failure' for name in SELECTED}
PRODUCTION=json.loads((ROOT/'production1966.json').read_text())
GPU_ENTRY='ulike1960/runtime/libulike_gpu1960.so'
INSTALLER='app/hiro/ulike/patches/IntegrationPayload186.class';LOADER='app/hiro/ulike/patches/UlikeHqMaxPatch.class'
LICENSE_ENTRY='ulike1965/THIRD_PARTY_LICENSES.txt'
CHANGED={'META-INF/MANIFEST.MF','classes.dex','ulike/runtime.dex',INSTALLER,LOADER,GPU_ENTRY};ADDED={LICENSE_ENTRY}
JNI_NAMES=['nativeAbi','openNative','allocateNative','uploadIntsNative','uploadFloatsNative','dispatchNative','readIntsNative','closeNative','environmentNative','retainedNative','supportedNative','batchNative','executeNative','submitNative','readManyNative','trimNative','ticketReadyNative','timeoutNative','workgroupNative','readIntoNative']
NEW_JNI=['Lcom/hiro/ulike/GpuNoise1960;->recoverNative1965()Z','Lcom/hiro/ulike/GpuNoise1960;->diagnosticsNative1965()[J','Lcom/hiro/ulike/GpuNoise1960;->failureNative1965()Ljava/lang/String;','Lcom/hiro/ulike/GpuNoise1960;->fp64ProbeNative1965()Z']
NEW_JNI += ['Lcom/hiro/ulike/GpuNoise1960;->soft64ProbeNative1965()Z']
JNI_NAMES += ['recoverNative1965','diagnosticsNative1965','failureNative1965','fp64ProbeNative1965','soft64ProbeNative1965']
TOOL_PINS=common.TOOL_PINS
NATIVE_TOOL_PINS={'clang': 'a871130d810536f7bb924c8aeaff57c66de27bed9b13d0ccafba25fdcc8bd02d', 'ld.lld': 'c4a7473d3b8a99a32335616838af2aa85c6333091abeab423be2a905ff5f288c', 'llvm-readelf': 'a8bd1b1d548f30cf582a025bf42d63ce225a861e8ad35006a722c27e0a4b3a8c'}
require,sha,run,verify_jdk=common.require,common.sha,common.run,common.verify_jdk
archive,headers,manifest,own,write_zip=common.archive,common.headers,common.manifest,common.own,common.write_zip

def production_path(name):
 path=ROOT/(name+'.java')
 if path.is_file():return path
 path=ROOT/'quality-dependencies/com/hiro/ulike'/(name+'.java')
 require(path.is_file(),'Missing production source '+name);return path

def source_preserved_roots():
 pins=json.loads((ROOT/'tests1965/published1965-source-pins.json').read_text())
 return sorted(name for name in PRODUCTION if production_path(name).relative_to(ROOT).as_posix() in pins and sha(production_path(name).read_bytes())==pins[production_path(name).relative_to(ROOT).as_posix()]['sha256'])
def json_bytes(data):return (json.dumps(data,ensure_ascii=False,sort_keys=True,indent=2)+'\n').encode()
def source_pins():
 return {p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in sorted(ROOT.rglob('*')) if p.is_file() and '__pycache__' not in p.parts and p.suffix in ('.java','.py','.c','.h','.comp','.glsl','.sh','.txt','.json','.dex','.tsv')}
def dex_integrity(raw,name):
 require(len(raw)>=112 and raw[:4]==b'dex\n' and struct.unpack_from('<I',raw,32)[0]==len(raw),'DEX header invalid: '+name)
 require(raw[12:32]==hashlib.sha1(raw[32:]).digest() and struct.unpack_from('<I',raw,8)[0]==zlib.adler32(raw[12:])&0xffffffff,'DEX integrity invalid: '+name)
def resource_delta(before,after,label):
 require(set(before)<=set(after),'Removed '+label+' resource');changed={n for n in before if before[n]!=after[n]};added=set(after)-set(before)
 require(changed==CHANGED and added==ADDED,'Unexpected '+label+' resource delta '+repr((sorted(changed),sorted(added))));return sorted(changed),sorted(added)
def load_module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod);return mod

def build(args):
 identity=verify_jdk();declaration=json.loads((ROOT.parent/'manifest.json').read_text());require(declaration.get('reviewed_source_sha256')==source_pins(),'Reviewed source graph changed before build');require(declaration.get('reviewed_publication_sha256')=={n:sha((ROOT.parent/'publication'/n).read_bytes()) for n in ('publish1966.py','ulike1966-gpu-required-publish.yml','README.md')},'Reviewed publication graph changed before build');require(not args.work.exists(),'Fresh build directory required');args.work.mkdir(parents=True);args.output.mkdir(parents=True,exist_ok=True)
 pins={BASE_SINGLE:BASE_SINGLE_SHA256,BASE_BUNDLE:BASE_BUNDLE_SHA256,**TOOL_PINS}
 for name,digest in pins.items():
  path=(args.tools if name.endswith('.jar') else args.input)/name;require(sha(path.read_bytes())==digest,'Pinned input differs: '+name)
 require((args.input/BASE_SINGLE).stat().st_size==BASE_SINGLE_BYTES and (args.input/BASE_BUNDLE).stat().st_size==BASE_BUNDLE_BYTES,'Pinned input byte sizes differ')
 require((args.input/ORACLE_SINGLE).stat().st_size==ORACLE_SINGLE_BYTES and sha((args.input/ORACLE_SINGLE).read_bytes())==ORACLE_SINGLE_SHA256,'Pinned frozen .64 numerical oracle differs')
 base=archive(args.input/BASE_SINGLE);bundle=archive(args.input/BASE_BUNDLE)
 oracle=archive(args.input/ORACLE_SINGLE)
 require(headers(base['META-INF/MANIFEST.MF'])['Version']==BASE_VERSION and headers(bundle['META-INF/MANIFEST.MF'])['Version']==BASE_BUNDLE_VERSION,'Wrong baseline version')
 require({n:b for n,b in base.items() if own(n)}=={n:b for n,b in bundle.items() if own(n)},'Standalone and integrated baselines differ')
 require(json.loads((ROOT/'production1966.json').read_text())==PRODUCTION,'Reviewed production roots differ')
 production=[production_path(n) for n in PRODUCTION];compiled_sources={p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in production}
 baseline=args.work/'baseline'
 for name,data in base.items():
  path=baseline/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
 bundle_dex=baseline/'bundle.dex';bundle_dex.write_bytes(bundle['classes.dex'])
 require(re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',(args.ndk/'source.properties').read_text(),re.M),'Pinned NDK r27c required')
 ndk_bin=args.ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin'
 require(all(sha((ndk_bin/name).read_bytes())==digest for name,digest in NATIVE_TOOL_PINS.items()),'Official r27c compiler/linker/readelf executable hash differs')
 native_compiler_identity=run([ndk_bin/'clang','--version'],args.work/'native-compiler-version.txt')
 require('Android (12470979' in native_compiler_identity and 'clang version 18.0.3' in native_compiler_identity,'Official r27c compiler identity differs')
 native_output=args.work/'gpu-native'
 run(['python3',ROOT/'native1960/build_native1960.py','--ndk',args.ndk,'--out',native_output],args.work/'native1960-build.log')
 native=native_output/'libulike_gpu1960.so';native_bytes=native.read_bytes()
 require(native_bytes[:6]==b'\x7fELF\x02\x01' and native_bytes[18:20]==b'\xb7\x00','ELF64 AArch64 GPU payload required')
 ndk_bin=args.ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin'
 elf=run([ndk_bin/'llvm-readelf','-h','-d','-Ws','--program-headers','--wide',native],args.work/'gpu-native-readelf.txt')
 exports=['Java_com_hiro_ulike_GpuNoise1960_'+n for n in JNI_NAMES]+['Java_com_hiro_ulike_SingleResidual1961_prepareNative','Java_com_hiro_ulike_SingleResidual1961_prepareDirectNative']
 require('AArch64' in elf and all(n in elf for n in exports),'GPU native JNI linkage differs')
 alignments=[int(line.split()[-1],0) for line in elf.splitlines() if line.strip().startswith('LOAD ')];needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',elf)
 require(alignments and all(a>=16384 for a in alignments) and set(needed)<= {'libc.so','libm.so','libdl.so','libEGL.so','libGLESv3.so'},'GPU native page alignment or dependency differs')
 native_sources={p.relative_to(ROOT/'native1960').as_posix():sha(p.read_bytes()) for p in sorted((ROOT/'native1960').rglob('*')) if p.is_file() and p.suffix in ('.c','.h','.comp','.glsl','.py')}
 native_report={'library':native.name,'sha256':sha(native_bytes),'bytes':len(native_bytes),'ndk_revision':'27.2.12479018','native_abi':19601,'abi':'arm64-v8a','min_sdk':26,'physical_android_tested':False,'sources':native_sources,'toolchain_sha256':NATIVE_TOOL_PINS,'compiler_identity':native_compiler_identity,'jni_exports':exports,'load_segment_alignments':alignments,'needed_libraries':needed}
 current_pins=source_pins();preserved_roots=source_preserved_roots();require(declaration.get('source_preserved_helper_roots')==preserved_roots and declaration.get('intentionally_modified_helper_roots')==sorted(set(PRODUCTION)-set(preserved_roots)),'Explicit reviewed modified-source roots differ')
 classes=args.work/'classes';helpers=args.work/'helper-classes';dex=args.work/'helper-dex'
 for p in (classes,helpers,dex):p.mkdir()
 stub_map={}
 for folder in ['layout1937-stubs','geometry1937-compile','front1936-stubs','renderer1938-stubs','capture-stubs','fast-stubs','metadata-stubs','quality-stubs','quality-dependencies','timing1947-stubs','core1950-stubs']:
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
 cp=os.pathsep.join(map(str,[args.tools/'morphe.jar',baseline,classes]));transformers=['MergePayloads','Transform1966','VerifyHelperReferences']
 transformer_sources=[ROOT/(n+'.java') for n in transformers]+[ROOT/'PatchClass1966.java',ROOT/'PatchLoader1966.java'];transformer_pins={p.name:sha(p.read_bytes()) for p in transformer_sources}
 run(['javac','-encoding','UTF-8','-cp',cp,'-d',classes,*[ROOT/(n+'.java') for n in transformers]],args.work/'transform-javac.log')
 exports_args=['--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','--add-exports','java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED']
 run(['javac','-encoding','UTF-8',*exports_args,'-cp',cp,'-d',classes,ROOT/'PatchClass1966.java',ROOT/'PatchLoader1966.java'],args.work/'metadata-javac.log')
 properties=['-Dulike.newjni='+','.join(NEW_JNI),'-Dulike.production='+','.join(PRODUCTION),'-Dulike.sourcepreserved='+','.join(preserved_roots),'-Dulike.gpu1960.sha='+sha(native_bytes),'-Dulike.gpu1960.bytes='+str(len(native_bytes))]
 for name in ['emitted','repeat']:run(['java',*properties,'-cp',cp,'Transform1966',baseline,dex/'classes.dex',bundle_dex,args.work/name,args.work/(name+'-audit.tsv')],args.work/(name+'.log'))
 emitted=args.work/'emitted';require({p.name:p.read_bytes() for p in emitted.iterdir()}=={p.name:p.read_bytes() for p in (args.work/'repeat').iterdir()},'DEX transform is not deterministic')
 run(['java',*exports_args,'-cp',cp,'PatchClass1966',baseline/LOADER,emitted/'UlikeHqMaxPatch.class'],args.work/'metadata.log')
 run(['java',*exports_args,'-cp',cp,'PatchLoader1966',baseline/INSTALLER,native,emitted/'IntegrationPayload186.class'],args.work/'native-installer-metadata.log')
 refs=run(['java','-cp',cp,'VerifyHelperReferences',baseline/'ulike/methods.dex',baseline/'ulike/runtime.dex',emitted/'methods.dex',emitted/'runtime.dex'],args.work/'helper-references.txt');require(refs.startswith('PASS helper references in '),'Changed helper linkage proof required')
 verification=(args.work/'emitted.log').read_text();require('begin_image_false_return_verified=true' in verification and 'serialized_dex_verified=true' in verification,'Capture/serialized preservation proof required')
 final=dict(base);integrated=dict(bundle)
 payloads={'ulike/runtime.dex':(emitted/'runtime.dex').read_bytes(),LOADER:(emitted/'UlikeHqMaxPatch.class').read_bytes(),INSTALLER:(emitted/'IntegrationPayload186.class').read_bytes(),GPU_ENTRY:native_bytes,LICENSE_ENTRY:(ROOT/'THIRD_PARTY_LICENSES.txt').read_bytes()}
 for name,data in payloads.items():final[name]=data;integrated[name]=data
 final['classes.dex']=(emitted/'loader.dex').read_bytes();integrated['classes.dex']=(emitted/'bundle-loader.dex').read_bytes()
 fields=headers(base['META-INF/MANIFEST.MF']);fields['Version']=VERSION;fields['Description']='Noise, protection and correction require GPU; bounded recovery then explicit failure; physical Android unverified.';final['META-INF/MANIFEST.MF']=manifest(fields)
 fields=headers(bundle['META-INF/MANIFEST.MF']);fields['Version']=BUNDLE_VERSION;fields['Description']='ULike mandatory GPU noise/correction and bounded recovery; unrelated apps preserved; physical Android unverified.';integrated['META-INF/MANIFEST.MF']=manifest(fields)
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
 os.environ['ULIKE_ANDROID_JAR']=str(args.tools/'android.jar');os.environ['ULIKE_MORPHE_JAR']=str(args.tools/'morphe.jar');os.environ['ULIKE1963_MORPHE_JAR']=str(args.tools/'morphe.jar');os.environ['ULIKE_NDK_HOME']=str(args.ndk)
 os.environ['ULIKE1965_BASELINE_MPP']=str(args.input/ORACLE_SINGLE)
 host_oracle=args.work/'host-oracle1965';(host_oracle/'tools').mkdir(parents=True)
 (host_oracle/'runtime1964.dex').write_bytes(oracle['ulike/runtime.dex']);shutil.copyfile(args.tools/'morphe.jar',host_oracle/'tools/morphe.jar')
 os.environ['ULIKE_TOOLCHAIN1965']=str(host_oracle)
 host=load_module('host_gpu1966_build',ROOT/'host_gpu1966.py');result=host.test(ROOT,args.work/'gpu-host',jdk=args.jdk,ndk=args.ndk)
 require(isinstance(result,dict) and result.get('status')=='passed' and type(result.get('assertions')) is int and result['assertions']>0,'Fresh GPU host test result required')
 require(result.get('gpu_shader_execution_on_host') is True and result.get('strict_gpu_required_routes_verified') is True,'Actual host GPU and strict route behavior evidence required')
 require(result.get('reports',{}).get('gpu_chroma1965',{}).get('published_runtime_dex_sha256')==sha(oracle['ulike/runtime.dex']),'Chroma oracle is not exact pinned published .64 runtime')
 require(result.get('production_gles_required_math_functional') is True,'Original production GLES required math must execute successfully; desktop FP64 adaptation or unsupported-only behavior is not a publishable GPU migration')
 require(compiled_sources=={p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in production} and current_pins==source_pins(),'Source changed during compilation/test')
 inventory=json.loads((emitted/'gpu-inventory1966.json').read_text());artifacts={n:{'sha256':sha((args.output/n).read_bytes()),'bytes':(args.output/n).stat().st_size} for n in (SINGLE,BUNDLE)}
 qa={'schema':'ulike1966-gpu-required-v1','status':'MANDATORY_GPU_HOST_VERIFIED_ORIGINAL_APKS_AND_DEVICE_UNVERIFIED',
 'ulike_version':VERSION,'bundle_version':BUNDLE_VERSION,'baseline_ulike_version':BASE_VERSION,'baseline_bundle_version':BASE_BUNDLE_VERSION,
 'selected_candidates':SELECTED,'gx_implementation_status':GX_STATUS,'input_sha256':pins,'artifacts':artifacts,'jdk_identity':identity,'approved_lineage':'ULike1.8.8',
 'host_quality_passed':True,'host_quality_result':result,'host_quality_result_sha256':sha(json.dumps(result,sort_keys=True).encode()),
 'host_pixel_equivalence_to_baseline':result.get('host_pixel_equivalence_to_baseline',False),'baseline_runtime_dex_sha256':sha(base['ulike/runtime.dex']),
 'pixel_oracle_input':PIXEL_ORACLE,'pixel_oracle_runtime_dex_sha256':sha(oracle['ulike/runtime.dex']),
 'gpu_required_noise':True,'gpu_required_protection':True,'gpu_required_correction':True,'cpu_image_processing_fallback_enabled':False,
 'untreated_success_on_gpu_failure_enabled':False,'bounded_gpu_recovery':True,'gpu_recovery_max_attempts':3,'android_software_driver_refused':True,
 'camera_baseline_classes_bytecode_identical':True,'camera_timing_hooks_preserved':True,'gpu_diagnostics_in_camera_zip':True,
 'capture_fusion_enabled':False,'capture_begin_image_false_return_verified':True,'capture_class_bytecode_identical':True,
 'production_source_consistency_verified':True,'compiled_production_source_sha256':compiled_sources,'compiled_transformer_source_sha256':transformer_pins,
 'executed_host_source_sha256':current_pins,'compiled_gpu_native_source_sha256':native_sources,'gpu_native_library_sha256':sha(native_bytes),'gpu_native_library_bytes':len(native_bytes),
 'gpu_native_build':native_report,'gpu_native_toolchain_sha256':NATIVE_TOOL_PINS,'gpu_native_jni_exports':exports,'gpu_native_load_segment_alignments':alignments,'gpu_native_needed_libraries':needed,
 'inherited_native_payloads':inherited_native,'inherited_native_libraries_byte_identical':True,'native_methods_byte_identical':True,'native_methods_tsv_byte_identical':True,
 'native_installer_existing_rows_preserved':True,'native_installer_existing_row_count_preserved':True,'native_installer_appended_rows':0,'native_installer_updated_rows':1,
 'native_installer_preserved_rows':11,'native_installer_total_rows':12,'native_installer_inverse_verified':True,'unrelated_runtime_classes_bytecode_identical':True,
 'unchanged_source_helper_roots_bytecode_identical':True,'source_preserved_helper_roots':preserved_roots,'intentionally_modified_helper_roots':sorted(set(PRODUCTION)-set(preserved_roots)),
 'non_ulike_loader_classes_unchanged':True,'non_ulike_resources_byte_identical':True,'standalone_and_bundle_ulike_resources_identical':True,
 'resolution_and_save_format_preserved':True,'save_format_and_codec_configuration_preserved':True,'diagnostic_stage_names':['fusion','noise','correction','compression','save'],
 'gpu_processing_added':True,'gpu_shader_execution_on_host':True,'gpu_execution_on_physical_android':False,'all_processing_on_gpu':False,
 'original_apk_apply_tested':False,'original_split_merge_tested':False,'ci_android_apply_tested':False,'device_tested':False,'device_quality_verified':False,'device_save_speed_measured':False,
 'changed_standalone_entries':single_delta[0],'added_standalone_entries':single_delta[1],'changed_bundle_entries':bundle_delta[0],'added_bundle_entries':bundle_delta[1],
 **inventory,'published':False,'manager_feed_updated':False}
 (args.output/'QA_ULike_v1.9.66.json').write_bytes(json_bytes(qa));(args.output/'host-regression1966-result.json').write_bytes(json_bytes(result));(args.output/'native-build1966.json').write_bytes(json_bytes(native_report))
 for name in ['emitted-audit.tsv','helper-references.txt','emitted.log','metadata.log','native-installer-metadata.log','gpu-native-readelf.txt']:shutil.copyfile(args.work/name,args.output/name)
 shutil.copyfile(emitted/'gpu-inventory1966.json',args.output/'gpu-inventory1966.json');print(json.dumps({'status':qa['status'],'artifacts':artifacts,'host_assertions':result['assertions']},ensure_ascii=False,indent=2));return qa

if __name__=='__main__':
 parser=argparse.ArgumentParser(description=__doc__)
 for name in ['input','tools','work','output']:parser.add_argument('--'+name,type=Path,required=True)
 parser.add_argument('--jdk',type=Path);parser.add_argument('--ndk',type=Path,required=True);parser.add_argument('--prepare-only',action='store_true',help='Compile provisional packages only; no QA or publishable evidence');args=parser.parse_args()
 if args.jdk:os.environ['ULIKE_JDK_HOME']=str(args.jdk.resolve());os.environ['PATH']=str(args.jdk.resolve()/'bin')+os.pathsep+os.environ.get('PATH','')
 for key,value in vars(args).items():
  if isinstance(value,Path):setattr(args,key,value.resolve())
 build(args)
