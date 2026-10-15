#!/usr/bin/env python3
"""Build single-image NR1-NR4 incrementally from byte-pinned published .54/.187.

All existing native libraries, camera/save classes and other app resources stay
pinned. Only three reviewed Java helper roots, beginImage admission, the ULike
version metadata and one appended optional ARM64 library may change.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, shutil, struct, subprocess, zlib
import build1945 as common

ROOT=Path(__file__).resolve().parent
VERSION='1.9.55';BASE_VERSION='1.9.54';BUNDLE_VERSION='1.0.188';BASE_BUNDLE_VERSION='1.0.187'
SINGLE='ULike_HQ_Texture_Online_v1.9.55.mpp';BASE_SINGLE='ULike_HQ_Texture_Online_v1.9.54.mpp'
BUNDLE='Hiro_Morphe_Patches_v1.0.188.mpp';BASE_BUNDLE='Hiro_Morphe_Patches_v1.0.187.mpp'
BASE_SINGLE_SHA256='268cdfe20db63d869bf8e12cbf4101523f12b5c1ff4a2f1d6d227e02105ae6f5'
BASE_BUNDLE_SHA256='377b97d5b152485d784385090bd7430f0ff235a24650851ba9e7a1aff6516abb'
BASE_SINGLE_BYTES=957482;BASE_BUNDLE_BYTES=17604460
SELECTED=['NR1','NR2','NR3','NR4']
PRODUCTION=['QualityPipeline1932','SingleNoise1955','ProcessingTiming1947']
NR_ENTRY='ulike1955/runtime/libulike_nr1955.so'
INSTALLER='app/hiro/ulike/patches/IntegrationPayload186.class'
LOADER='app/hiro/ulike/patches/UlikeHqMaxPatch.class'
CHANGED={'META-INF/MANIFEST.MF','classes.dex','ulike/runtime.dex',INSTALLER,LOADER}
ADDED={NR_ENTRY}
TOOL_PINS=common.TOOL_PINS
require,sha,run,verify_jdk=common.require,common.sha,common.run,common.verify_jdk
archive,headers,manifest,own,write_zip=common.archive,common.headers,common.manifest,common.own,common.write_zip


def bundle_name(qa):return 'Hiro_Morphe_Patches_v'+qa['bundle_version']+'.mpp'
def json_bytes(data):return (json.dumps(data,ensure_ascii=False,sort_keys=True,indent=2)+'\n').encode()
def source_pins():
 return {p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in sorted(ROOT.rglob('*')) if p.is_file() and '__pycache__' not in p.parts and p.suffix in ('.java','.py','.c','.h','.comp','.sh','.txt')}
def dex_integrity(raw,name):
 require(len(raw)>=112 and raw[:4]==b'dex\n' and struct.unpack_from('<I',raw,32)[0]==len(raw),'DEX header invalid: '+name)
 require(raw[12:32]==hashlib.sha1(raw[32:]).digest() and struct.unpack_from('<I',raw,8)[0]==zlib.adler32(raw[12:])&0xffffffff,'DEX integrity invalid: '+name)
def resource_delta(before,after,label):
 require(set(before)<=set(after),'Removed '+label+' resource')
 changed={n for n in before if before[n]!=after[n]};added=set(after)-set(before)
 require(changed==CHANGED and added==ADDED,'Unexpected '+label+' resource delta '+repr((sorted(changed),sorted(added))))
 return sorted(changed),sorted(added)
def load_module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod);return mod


def build(args):
 identity=verify_jdk();require(not args.work.exists(),'Fresh build directory required')
 args.work.mkdir(parents=True);args.output.mkdir(parents=True,exist_ok=True)
 pins={BASE_SINGLE:BASE_SINGLE_SHA256,BASE_BUNDLE:BASE_BUNDLE_SHA256,**TOOL_PINS}
 for name,digest in pins.items():
  path=(args.tools if name.endswith('.jar') else args.input)/name
  require(sha(path.read_bytes())==digest,'Pinned input differs: '+name)
 require((args.input/BASE_SINGLE).stat().st_size==BASE_SINGLE_BYTES and (args.input/BASE_BUNDLE).stat().st_size==BASE_BUNDLE_BYTES,'Pinned input byte sizes differ')
 base=archive(args.input/BASE_SINGLE);bundle=archive(args.input/BASE_BUNDLE)
 require(headers(base['META-INF/MANIFEST.MF'])['Version']==BASE_VERSION and headers(bundle['META-INF/MANIFEST.MF'])['Version']==BASE_BUNDLE_VERSION,'Wrong baseline version')
 require({n:b for n,b in base.items() if own(n)}=={n:b for n,b in bundle.items() if own(n)},'Standalone and integrated baselines differ')
 production_path=ROOT/'production1955.json'
 if production_path.exists():require(json.loads(production_path.read_text())==PRODUCTION,'Reviewed production roots differ')
 production=[ROOT/(n+'.java') for n in PRODUCTION]
 compiled_sources={p.name:sha(p.read_bytes()) for p in production}
 current_pins=source_pins()
 require(sha((ROOT/'ProcessingTiming1947.java').read_text().replace(VERSION,BASE_VERSION).encode())=='78ad9d09ea444aa65e0240ca585275ea94b6b3f8efe759a01701b7d085995d4a','Timing algorithm differs beyond version')
 baseline=args.work/'baseline'
 for name,data in base.items():
  path=baseline/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
 bundle_dex=baseline/'bundle.dex';bundle_dex.write_bytes(bundle['classes.dex'])
 require(args.ndk is not None and re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',(args.ndk/'source.properties').read_text(),re.M),'Pinned NDK r27c required')
 native_output=args.work/'nr-native'
 native_builder=ROOT/'native1955/build_native1955.py'
 require(native_builder.is_file(),'Reviewed single-image native build source required')
 run(['python3',native_builder,'--ndk',args.ndk,'--output',native_output],args.work/'native1955-build.log')
 native=native_output/'libulike_nr1955.so';native_bytes=native.read_bytes()
 require(native_bytes[:6]==b'\x7fELF\x02\x01' and native_bytes[18:20]==b'\xb7\x00','ELF64 AArch64 NR payload required')
 ndk_bin=args.ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin'
 nr_elf=run([ndk_bin/'llvm-readelf','-h','-d','-Ws','--program-headers','--wide',native],args.work/'nr-native-readelf.txt')
 nr_exports=['Java_com_hiro_ulike_SingleNoise1955_nativeAbi','Java_com_hiro_ulike_SingleNoise1955_processNative']
 require('AArch64' in nr_elf and all(n in nr_elf for n in nr_exports),'NR native JNI linkage differs')
 alignments=[int(line.split()[-1],0) for line in nr_elf.splitlines() if line.strip().startswith('LOAD ')]
 needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',nr_elf)
 require(alignments and all(a>=16384 for a in alignments) and set(needed)<= {'libc.so','libm.so','libdl.so'},'NR native page alignment or dependency differs')
 reports=sorted(native_output.glob('*build*.json'));require(len(reports)==1,'Exactly one NR native build report required')
 native_report=json.loads(reports[0].read_text())
 require(native_report.get('sha256')==sha(native_bytes) and native_report.get('bytes')==len(native_bytes) and native_report.get('ndk_revision')=='27.2.12479018' and native_report.get('physical_android_tested') is False,'NR native build provenance differs')
 native_sources={n:sha((ROOT/'native1955'/n).read_bytes()) for n in native_report['sources']}
 require(native_sources==native_report['sources'],'NR native sources differ from build')
 classes=args.work/'classes';helpers=args.work/'helper-classes';dex=args.work/'helper-dex'
 for p in (classes,helpers,dex):p.mkdir()
 stub_map={}
 for folder in ['front1936-stubs','renderer1938-stubs','capture-stubs','fast-stubs','metadata-stubs','quality-stubs','quality-dependencies','timing1947-stubs','core1950-stubs']:
  path=ROOT/folder
  if path.exists():
   for p in sorted(path.rglob('*.java')):stub_map[str(p.relative_to(path))]=p
 retained=['AsyncSave1935','ProcessingTiming1947','QualityPipeline1932','ShotContext1932','TimedIo1947','QualityPixels1932','QualityShadow1932','SpeedWorkers1935','SaveQueue1935','FusionPixels1933','ReflectionCache1945','CodecDrain1945','NoiseCache1944','NativeSpeed1944','PolicyCache1945','Scheduling1944','GpuInteger1949','CorePixels1950','NativeMoire1951','GpuFinish1952','WholeRoute1952','PerformanceHints1952','GpuFinish1953','FinishPolicy1953','WholeRoute1953','SpatialNoise1934']
 for name in retained:stub_map['com/hiro/ulike/'+name+'.java']=ROOT/(name+'.java')
 for name in PRODUCTION:stub_map.pop('com/hiro/ulike/'+name+'.java',None)
 run(['javac','-source','8','-target','8','-encoding','UTF-8','-bootclasspath',args.tools/'android.jar','-d',helpers,*stub_map.values(),*production],args.work/'helper-javac.log')
 selected=sorted(p for p in (helpers/'com/hiro/ulike').glob('*.class') if any(p.name==n+'.class' or p.name.startswith(n+'$') for n in PRODUCTION))
 require(all(helpers/'com/hiro/ulike'/(n+'.class') in selected for n in PRODUCTION),'Missing production helper root')
 run(['java','-cp',args.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',args.tools/'android.jar','--classpath',helpers,'--output',dex,*selected],args.work/'d8.log')
 cp=os.pathsep.join(map(str,[args.tools/'morphe.jar',baseline,classes]))
 transformers=['MergePayloads','Transform1955','VerifyHelperReferences']
 transformer_sources=[ROOT/(n+'.java') for n in transformers]+[ROOT/'PatchClass1955.java',ROOT/'PatchLoader1955.java']
 transformer_pins={p.name:sha(p.read_bytes()) for p in transformer_sources}
 run(['javac','-encoding','UTF-8','-cp',cp,'-d',classes,*[ROOT/(n+'.java') for n in transformers]],args.work/'transform-javac.log')
 exports=['--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','--add-exports','java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED']
 run(['javac','-encoding','UTF-8',*exports,'-cp',cp,'-d',classes,ROOT/'PatchClass1955.java',ROOT/'PatchLoader1955.java'],args.work/'metadata-javac.log')
 properties=['-Dulike.production='+','.join(PRODUCTION),'-Dulike.nr1955.sha='+sha(native_bytes),'-Dulike.nr1955.bytes='+str(len(native_bytes))]
 for name in ['emitted','repeat']:
  run(['java',*properties,'-cp',cp,'Transform1955',baseline,dex/'classes.dex',bundle_dex,args.work/name,args.work/(name+'-audit.tsv')],args.work/(name+'.log'))
 emitted=args.work/'emitted'
 require({p.name:p.read_bytes() for p in emitted.iterdir()}=={p.name:p.read_bytes() for p in (args.work/'repeat').iterdir()},'DEX transform is not deterministic')
 run(['java',*exports,'-cp',cp,'PatchClass1955',baseline/LOADER,emitted/'UlikeHqMaxPatch.class'],args.work/'metadata.log')
 run(['java',*exports,'-cp',cp,'PatchLoader1955',baseline/INSTALLER,native,emitted/'IntegrationPayload186.class'],args.work/'native-installer-metadata.log')
 refs=run(['java','-cp',cp,'VerifyHelperReferences',baseline/'ulike/methods.dex',baseline/'ulike/runtime.dex',emitted/'methods.dex',emitted/'runtime.dex'],args.work/'helper-references.txt')
 require(refs.startswith('PASS helper references in '),'Changed helper linkage proof required')
 verification=(args.work/'emitted.log').read_text();require('begin_image_false_return_verified=true' in verification and 'serialized_dex_verified=true' in verification,'Single-image capture/serialized preservation proof required')
 final=dict(base);integrated=dict(bundle)
 payloads={'ulike/runtime.dex':(emitted/'runtime.dex').read_bytes(),LOADER:(emitted/'UlikeHqMaxPatch.class').read_bytes(),INSTALLER:(emitted/'IntegrationPayload186.class').read_bytes(),NR_ENTRY:native_bytes}
 for name,data in payloads.items():final[name]=data;integrated[name]=data
 final['classes.dex']=(emitted/'loader.dex').read_bytes();integrated['classes.dex']=(emitted/'bundle-loader.dex').read_bytes()
 fields=headers(base['META-INF/MANIFEST.MF']);fields['Version']=VERSION;final['META-INF/MANIFEST.MF']=manifest(fields)
 fields=headers(bundle['META-INF/MANIFEST.MF']);fields['Version']=BUNDLE_VERSION;integrated['META-INF/MANIFEST.MF']=manifest(fields)
 single_delta=resource_delta(base,final,'standalone');bundle_delta=resource_delta(bundle,integrated,'bundle')
 require({n:b for n,b in final.items() if own(n)}=={n:b for n,b in integrated.items() if own(n)},'Current standalone and bundle ULike copies differ')
 require(all(integrated[n]==data for n,data in bundle.items() if not own(n) and n not in ('classes.dex','META-INF/MANIFEST.MF')),'Other app resource changed')
 inherited_native={n:{'sha256':sha(b),'bytes':len(b)} for n,b in base.items() if n.endswith('.so') or n=='ulike186/runtime/0000.bin'}
 require(len(inherited_native)==8 and all(final[n]==base[n] for n in inherited_native),'All eight inherited native libraries must be byte preserved')
 require(all(final[n]==base[n] for n in ['ulike/methods.dex','ulike/methods.tsv']),'Legacy native method payloads differ')
 for n in ['ulike/runtime.dex','classes.dex']:dex_integrity(final[n],n)
 dex_integrity(integrated['classes.dex'],'bundle classes.dex')
 write_zip(args.output/SINGLE,final);write_zip(args.output/BUNDLE,integrated)
 os.environ['ULIKE_MORPHE_JAR']=str(args.tools/'morphe.jar');os.environ['ULIKE_NDK_HOME']=str(args.ndk)
 host_path=ROOT/'host_nr1955.py';require(host_path.is_file(),'Fresh single-image NR host test driver required')
 host=load_module('host_nr1955_build',host_path)
 result=host.run(args.work/'nr-host',args.input,args.tools)
 require(isinstance(result,dict) and result.get('status')=='passed' and type(result.get('assertions')) is int and result['assertions']>0,'Fresh NR host test result required')
 require(compiled_sources=={p.name:sha(p.read_bytes()) for p in production} and current_pins==source_pins(),'Reviewed source changed during build/test')
 inventory=json.loads((emitted/'nr-inventory1955.json').read_text())
 artifacts={n:{'sha256':sha((args.output/n).read_bytes()),'bytes':(args.output/n).stat().st_size} for n in (SINGLE,BUNDLE)}
 qa={
  'schema':'ulike1955-single-noise-v1','status':'MPP_INCREMENTAL_SINGLE_NR_HOST_VERIFIED_ORIGINAL_APKS_AND_DEVICE_UNVERIFIED',
  'ulike_version':VERSION,'bundle_version':BUNDLE_VERSION,'baseline_ulike_version':BASE_VERSION,'baseline_bundle_version':BASE_BUNDLE_VERSION,
  'selected_candidates':SELECTED,'input_sha256':pins,'artifacts':artifacts,'jdk_identity':identity,
  'approved_lineage':'ULike1.8.8','host_quality_passed':True,'host_quality_result':result,'host_quality_result_sha256':sha(json.dumps(result,sort_keys=True).encode()),
  'host_pixel_equivalence_to_baseline':False,'intentional_quality_algorithm_change':True,'capture_fusion_enabled':False,
  'capture_begin_image_false_return_verified':True,'capture_begin_image_original_method_sha256':'e59cd85b6db190a1796df64442939696a8c18fd8ad220022eb1cdb9509f9ba06',
  'capture_other_methods_and_fields_preserved':True,'capture_admission_does_not_read_or_transfer_input':True,
  'production_source_consistency_verified':True,'compiled_production_source_sha256':compiled_sources,'compiled_transformer_source_sha256':transformer_pins,'executed_host_source_sha256':current_pins,
  'compiled_nr_native_source_sha256':native_sources,'nr_native_library_sha256':sha(native_bytes),'nr_native_library_bytes':len(native_bytes),'nr_native_build':native_report,'nr_native_jni_exports':nr_exports,'nr_native_load_segment_alignments':alignments,'nr_native_needed_libraries':needed,
  'inherited_native_payloads':inherited_native,'inherited_native_libraries_byte_identical':True,'native_methods_byte_identical':True,'native_methods_tsv_byte_identical':True,
  'native_installer_existing_rows_preserved':True,'native_installer_preserved_rows':8,'native_installer_total_rows':9,'native_installer_inverse_verified':True,
  'unrelated_runtime_classes_bytecode_identical':True,'non_ulike_loader_classes_unchanged':True,'non_ulike_resources_byte_identical':True,
  'standalone_and_bundle_ulike_resources_identical':True,'resolution_and_save_format_preserved':True,'save_format_and_codec_configuration_preserved':True,
  'stage_timing_algorithm_preserved':True,'timing_version_only_change':True,'diagnostic_stage_names':['fusion','noise','correction','compression','save'],
  'original_apk_apply_tested':False,'original_split_merge_tested':False,'ci_android_apply_tested':False,'device_tested':False,'device_quality_verified':False,'device_save_speed_measured':False,
  'changed_standalone_entries':single_delta[0],'added_standalone_entries':single_delta[1],'changed_bundle_entries':bundle_delta[0],'added_bundle_entries':bundle_delta[1],
  **inventory,'published':False,'manager_feed_updated':False,
 }
 (args.output/'QA_ULike_v1.9.55.json').write_bytes(json_bytes(qa))
 (args.output/'host-regression1955-result.json').write_bytes(json_bytes(result))
 (args.output/'native-build1955.json').write_bytes(json_bytes(native_report))
 for name in ['emitted-audit.tsv','helper-references.txt','emitted.log','metadata.log','native-installer-metadata.log','nr-native-readelf.txt']:shutil.copyfile(args.work/name,args.output/name)
 shutil.copyfile(emitted/'nr-inventory1955.json',args.output/'nr-inventory1955.json')
 print(json.dumps({'status':qa['status'],'artifacts':artifacts,'host_assertions':result['assertions']},ensure_ascii=False,indent=2))
 return qa

if __name__=='__main__':
 parser=argparse.ArgumentParser(description=__doc__)
 for name in ['input','tools','work','output']:parser.add_argument('--'+name,type=Path,required=True)
 parser.add_argument('--jdk',type=Path);parser.add_argument('--ndk',type=Path,required=True);args=parser.parse_args()
 if args.jdk:os.environ['ULIKE_JDK_HOME']=str(args.jdk.resolve());os.environ['PATH']=str(args.jdk.resolve()/'bin')+os.pathsep+os.environ.get('PATH','')
 for key,value in vars(args).items():
  if value is not None:setattr(args,key,value.resolve())
 build(args)
