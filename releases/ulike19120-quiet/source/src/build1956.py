#!/usr/bin/env python3
"""Reproducible H28-H33 incremental build on immutable published .55/.188."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, shutil, struct, zlib
import build1945 as common
ROOT=Path(__file__).resolve().parent
VERSION='1.9.56';BASE_VERSION='1.9.55';BUNDLE_VERSION='1.0.189';BASE_BUNDLE_VERSION='1.0.188'
SINGLE=f'ULike_HQ_Texture_Online_v{VERSION}.mpp';BASE_SINGLE=f'ULike_HQ_Texture_Online_v{BASE_VERSION}.mpp'
BUNDLE=f'Hiro_Morphe_Patches_v{BUNDLE_VERSION}.mpp';BASE_BUNDLE=f'Hiro_Morphe_Patches_v{BASE_BUNDLE_VERSION}.mpp'
BASE_SINGLE_SHA256='c458506effc27627458e33ed000a755a94767626f788f240f5aec3513dcb10ed';BASE_BUNDLE_SHA256='f766ead3054a2c8eed0d2f432ee6ed8e2fe001454ee2605c13407912f6581b3d'
BASE_SINGLE_BYTES=975747;BASE_BUNDLE_BYTES=17622747
SELECTED=['H28','H29','H30','H31','H32','H33']
PRODUCTION=['CorePixels1950','QualityShadow1932','WholeRoute1953','SaveQueue1935','AsyncSave1935','CodecDrain1945','SpeedWorkers1935','QualityPipeline1932','ProcessingTiming1947']
INSTALLER='app/hiro/ulike/patches/IntegrationPayload186.class';LOADER='app/hiro/ulike/patches/UlikeHqMaxPatch.class'
NATIVES=[('speed','native1944','build_native1944.py','native-build1944.json','ulike1935/runtime/libulike_speed1935.so'),('gpu','native1949','build_native1949.py','native-build1949.json','ulike1949/runtime/libulike_gpu1949.so'),('core','native1950','build_native1950.py','native-build1950.json','ulike1950/runtime/libulike_core1950.so'),('h8','h8gpu','build_h8gpu.py','h8gpu-build.json','ulike1951/runtime/libulike_h8gpu1951.so'),('nr','native1955','build_native1955.py','native1955-build.json','ulike1955/runtime/libulike_nr1955.so')]
CHANGED={'META-INF/MANIFEST.MF','classes.dex','ulike/runtime.dex',INSTALLER,LOADER}|{r[4] for r in NATIVES};ADDED=set()
TOOL_PINS=common.TOOL_PINS
require,sha,run,verify_jdk=common.require,common.sha,common.run,common.verify_jdk
archive,headers,manifest,own,write_zip=common.archive,common.headers,common.manifest,common.own,common.write_zip

def json_bytes(x):return (json.dumps(x,ensure_ascii=False,sort_keys=True,indent=2)+'\n').encode()
def source_pins():return {p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in sorted(ROOT.rglob('*')) if p.is_file() and '__pycache__' not in p.parts and p.suffix in ('.java','.py','.c','.h','.comp','.sh','.txt','.json','.dex','.tsv')}
def dex_integrity(raw,name):
 require(len(raw)>=112 and raw[:4]==b'dex\n' and struct.unpack_from('<I',raw,32)[0]==len(raw),'DEX header '+name)
 require(raw[12:32]==hashlib.sha1(raw[32:]).digest() and struct.unpack_from('<I',raw,8)[0]==zlib.adler32(raw[12:])&0xffffffff,'DEX integrity '+name)
def resource_delta(before,after,label):
 changed={n for n in before if before[n]!=after.get(n)};added=set(after)-set(before)
 require(set(before)==set(after) and changed==CHANGED and added==ADDED,'Unexpected '+label+' resource delta '+repr((sorted(changed),sorted(added))))
 return sorted(changed),sorted(added)
def load_module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod);return mod

def build(args):
 identity=verify_jdk();require(not args.work.exists(),'Fresh build directory required');args.work.mkdir(parents=True);args.output.mkdir(parents=True,exist_ok=True)
 pins={BASE_SINGLE:BASE_SINGLE_SHA256,BASE_BUNDLE:BASE_BUNDLE_SHA256,**TOOL_PINS}
 for name,digest in pins.items():require(sha(((args.tools if name.endswith('.jar') else args.input)/name).read_bytes())==digest,'Pinned input differs '+name)
 require((args.input/BASE_SINGLE).stat().st_size==BASE_SINGLE_BYTES and (args.input/BASE_BUNDLE).stat().st_size==BASE_BUNDLE_BYTES,'Input bytes differ')
 base=archive(args.input/BASE_SINGLE);bundle=archive(args.input/BASE_BUNDLE)
 require(headers(base['META-INF/MANIFEST.MF'])['Version']==BASE_VERSION and headers(bundle['META-INF/MANIFEST.MF'])['Version']==BASE_BUNDLE_VERSION,'Baseline versions')
 require({n:b for n,b in base.items() if own(n)}=={n:b for n,b in bundle.items() if own(n)},'Baseline ULike copies differ')
 require(json.loads((ROOT/'production1956.json').read_text())==PRODUCTION,'Reviewed roots')
 production=[ROOT/(n+'.java') for n in PRODUCTION];compiled_sources={p.name:sha(p.read_bytes()) for p in production};current_pins=source_pins()
 require(sha((ROOT/'ProcessingTiming1947.java').read_text().replace(VERSION,BASE_VERSION).encode())=='aa52c2bda429bfd86e7d71aae952384bf1ff25b09f17ce0b8cb2411640db4430','Timing algorithm differs beyond version')
 baseline=args.work/'baseline'
 for name,data in base.items():p=baseline/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
 bundle_dex=baseline/'bundle.dex';bundle_dex.write_bytes(bundle['classes.dex'])
 require(re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',(args.ndk/'source.properties').read_text(),re.M),'NDK r27c required')
 os.environ['ULIKE_NDK_HOME']=str(args.ndk);os.environ['ULIKE_MORPHE_JAR']=str(args.tools/'morphe.jar')
 ndk_bin=args.ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin';native_payloads={};native_reports={};native_sources={};tsv=[]
 for key,folder,builder,reportfile,entry in NATIVES:
  out=args.work/(key+'-native');run(['python3',ROOT/folder/builder,'--ndk',args.ndk,'--output',out],args.work/(key+'-build.log'))
  path=out/Path(entry).name;raw=path.read_bytes();require(raw[:6]==b'\x7fELF\x02\x01' and raw[18:20]==b'\xb7\x00','ELF AArch64 '+key)
  report=json.loads((out/reportfile).read_text());require(report['sha256']==sha(raw) and report['bytes']==len(raw) and report['ndk_revision']=='27.2.12479018' and report.get('physical_android_tested') is False,'Native build provenance '+key)
  sources={folder+'/'+n:sha((ROOT/folder/n).read_bytes()) for n in report['sources']};require({n.split('/',1)[1]:v for n,v in sources.items()}==report['sources'],'Native source pins '+key)
  elf=run([ndk_bin/'llvm-readelf','-h','-d','-Ws','--program-headers','--wide',path],args.work/(key+'-readelf.txt'))
  aligns=[int(line.split()[-1],0) for line in elf.splitlines() if line.strip().startswith('LOAD ')];needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',elf)
  require(aligns and min(aligns)>=16384 and set(needed)<={'libc.so','libm.so','libdl.so','liblog.so','libEGL.so','libGLESv2.so','libGLESv3.so','libandroid.so'},'Native alignment/dependencies '+key)
  native_payloads[entry]=raw;native_sources.update(sources);native_reports[key]={**report,'resource':entry,'load_segment_alignments':aligns,'needed_libraries':needed}
  tsv.append('\t'.join([entry,sha(base[entry]),str(len(base[entry])),sha(raw),str(len(raw))]))
 native_tsv=args.work/'native-rows1956.tsv';native_tsv.write_text('\n'.join(tsv)+'\n')
 classes=args.work/'classes';helpers=args.work/'helper-classes';dex=args.work/'helper-dex'
 for p in (classes,helpers,dex):p.mkdir()
 stubs={}
 for folder in ['front1936-stubs','renderer1938-stubs','capture-stubs','fast-stubs','metadata-stubs','quality-stubs','quality-dependencies','timing1947-stubs','core1950-stubs']:
  if (ROOT/folder).exists():
   for p in sorted((ROOT/folder).rglob('*.java')):stubs[str(p.relative_to(ROOT/folder))]=p
 retained=['AsyncSave1935','ProcessingTiming1947','QualityPipeline1932','ShotContext1932','TimedIo1947','QualityPixels1932','QualityShadow1932','SpeedWorkers1935','SaveQueue1935','FusionPixels1933','ReflectionCache1945','CodecDrain1945','NoiseCache1944','NativeSpeed1944','PolicyCache1945','Scheduling1944','GpuInteger1949','CorePixels1950','NativeMoire1951','GpuFinish1952','WholeRoute1952','PerformanceHints1952','GpuFinish1953','FinishPolicy1953','WholeRoute1953','SpatialNoise1934','SingleNoise1955']
 for n in retained:stubs['com/hiro/ulike/'+n+'.java']=ROOT/(n+'.java')
 for n in PRODUCTION:stubs.pop('com/hiro/ulike/'+n+'.java',None)
 run(['javac','-source','8','-target','8','-encoding','UTF-8','-bootclasspath',args.tools/'android.jar','-d',helpers,*stubs.values(),*production],args.work/'helper-javac.log')
 selected=sorted(p for p in (helpers/'com/hiro/ulike').glob('*.class') if any(p.name==n+'.class' or p.name.startswith(n+'$') for n in PRODUCTION))
 require(all(helpers/'com/hiro/ulike'/(n+'.class') in selected for n in PRODUCTION),'Missing helper root')
 run(['java','-cp',args.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',args.tools/'android.jar','--classpath',helpers,'--output',dex,*selected],args.work/'d8.log')
 cp=os.pathsep.join(map(str,[args.tools/'morphe.jar',baseline,classes]));transformers=['MergePayloads','Transform1956','VerifyHelperReferences'];transformer_sources=[ROOT/(n+'.java') for n in transformers+['PatchClass1956','PatchLoader1956']];transformer_pins={p.name:sha(p.read_bytes()) for p in transformer_sources}
 run(['javac','-encoding','UTF-8','-cp',cp,'-d',classes,*[ROOT/(n+'.java') for n in transformers]],args.work/'transform-javac.log')
 exports=['--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','--add-exports','java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED']
 run(['javac','-encoding','UTF-8',*exports,'-cp',cp,'-d',classes,ROOT/'PatchClass1956.java',ROOT/'PatchLoader1956.java'],args.work/'metadata-javac.log')
 for name in ['emitted','repeat']:run(['java','-Dulike.production='+','.join(PRODUCTION),'-cp',cp,'Transform1956',baseline,dex/'classes.dex',bundle_dex,args.work/name,args.work/(name+'-audit.tsv'),native_tsv],args.work/(name+'.log'))
 emitted=args.work/'emitted';require({p.name:p.read_bytes() for p in emitted.iterdir()}=={p.name:p.read_bytes() for p in (args.work/'repeat').iterdir()},'Nondeterministic DEX transform')
 run(['java',*exports,'-cp',cp,'PatchClass1956',baseline/LOADER,emitted/'UlikeHqMaxPatch.class'],args.work/'metadata.log')
 run(['java',*exports,'-cp',cp,'PatchLoader1956',baseline/INSTALLER,native_tsv,emitted/'IntegrationPayload186.class'],args.work/'native-installer-metadata.log')
 refs=run(['java','-cp',cp,'VerifyHelperReferences',baseline/'ulike/methods.dex',baseline/'ulike/runtime.dex',emitted/'methods.dex',emitted/'runtime.dex'],args.work/'helper-references.txt');require(refs.startswith('PASS helper references in '),'Helper linkage proof')
 final=dict(base);integrated=dict(bundle);payloads={**native_payloads,'ulike/runtime.dex':(emitted/'runtime.dex').read_bytes(),LOADER:(emitted/'UlikeHqMaxPatch.class').read_bytes(),INSTALLER:(emitted/'IntegrationPayload186.class').read_bytes()}
 for name,raw in payloads.items():final[name]=raw;integrated[name]=raw
 final['classes.dex']=(emitted/'loader.dex').read_bytes();integrated['classes.dex']=(emitted/'bundle-loader.dex').read_bytes()
 for archive_,version in [(final,VERSION),(integrated,BUNDLE_VERSION)]:
  fields=headers(archive_['META-INF/MANIFEST.MF']);fields['Version']=version;fields['Description']='H28-H33 exact optimization on single-image NR1-NR4; no capture fusion; physical device unverified.';archive_['META-INF/MANIFEST.MF']=manifest(fields)
 single_delta=resource_delta(base,final,'standalone');bundle_delta=resource_delta(bundle,integrated,'bundle')
 require({n:b for n,b in final.items() if own(n)}=={n:b for n,b in integrated.items() if own(n)},'Current ULike copies differ')
 require(all(integrated[n]==b for n,b in bundle.items() if not own(n) and n not in ('classes.dex','META-INF/MANIFEST.MF')),'Other app resource changed')
 unchanged_native={n:{'sha256':sha(b),'bytes':len(b)} for n,b in base.items() if (n.endswith('.so') or n=='ulike186/runtime/0000.bin') and n not in native_payloads};require(len(unchanged_native)==4 and all(final[n]==base[n] for n in unchanged_native),'Four unrelated native libraries retained')
 require(all(final[n]==base[n] for n in ['ulike/methods.dex','ulike/methods.tsv']),'Protected native methods changed')
 for raw,name in [(final['ulike/runtime.dex'],'runtime'),(final['classes.dex'],'single loader'),(integrated['classes.dex'],'bundle loader')]:dex_integrity(raw,name)
 write_zip(args.output/SINGLE,final);write_zip(args.output/BUNDLE,integrated)
 for name in [SINGLE,BUNDLE]:run(['java','-jar',args.tools/'morphe.jar','list-patches','--patches',args.output/name],args.work/(name+'.loader.log'))
 result=load_module('host1956',ROOT/'host_regression1956.py').test(ROOT,args.work,args.tools,args.input)
 require(result.get('status')=='passed' and result.get('assertions',0)>0,'Host checks must execute and pass')
 require(compiled_sources=={p.name:sha(p.read_bytes()) for p in production} and current_pins==source_pins(),'Source changed during build/test')
 inventory=json.loads((emitted/'optimization-inventory1956.json').read_text());artifacts={n:{'sha256':sha((args.output/n).read_bytes()),'bytes':(args.output/n).stat().st_size} for n in [SINGLE,BUNDLE]}
 qa={'schema':'ulike1956-optimization-v1','status':'MPP_HOST_VERIFIED_DEVICE_UNVERIFIED','ulike_version':VERSION,'bundle_version':BUNDLE_VERSION,'baseline_ulike_version':BASE_VERSION,'baseline_bundle_version':BASE_BUNDLE_VERSION,'selected_candidates':SELECTED,'approved_lineage':'ULike1.8.8','input_sha256':pins,'artifacts':artifacts,'jdk_identity':identity,'production_source_consistency_verified':True,'compiled_production_source_sha256':compiled_sources,'compiled_transformer_source_sha256':transformer_pins,'compiled_native_source_sha256':native_sources,'executed_host_source_sha256':current_pins,'native_builds':native_reports,'unchanged_native_payloads':unchanged_native,'host_quality_passed':True,'host_quality_result':result,'host_quality_result_sha256':sha(json.dumps(result,sort_keys=True).encode()),'capture_fusion_enabled':False,'capture_begin_image_false_return_verified':True,'capture_class_bytecode_identical':True,'nr1_nr4_preserved':True,'quality_algorithm_and_settings_preserved':True,'unrelated_runtime_classes_bytecode_identical':True,'non_ulike_loader_classes_unchanged':True,'non_ulike_resources_byte_identical':True,'standalone_and_bundle_ulike_resources_identical':True,'native_methods_byte_identical':True,'native_methods_tsv_byte_identical':True,'native_installer_total_rows':9,'native_installer_updated_rows':5,'native_installer_inverse_verified':True,'save_publication_hooks_inverse_verified':True,'resolution_and_save_format_preserved':True,'save_format_and_codec_configuration_preserved':True,'stage_timing_algorithm_preserved':True,'timing_version_only_change':True,'diagnostic_stage_names':['fusion','noise','correction','compression','save'],'original_apk_apply_tested':False,'original_split_merge_tested':False,'ci_android_apply_tested':False,'device_tested':False,'device_quality_verified':False,'device_save_speed_measured':False,'changed_standalone_entries':single_delta[0],'added_standalone_entries':single_delta[1],'changed_bundle_entries':bundle_delta[0],'added_bundle_entries':bundle_delta[1],**inventory,'published':False,'manager_feed_updated':False}
 (args.output/f'QA_ULike_v{VERSION}.json').write_bytes(json_bytes(qa));(args.output/'host-regression1956-result.json').write_bytes(json_bytes(result));(args.output/'native-builds1956.json').write_bytes(json_bytes(native_reports))
 for name in ['emitted-audit.tsv','helper-references.txt','emitted.log','metadata.log','native-installer-metadata.log','native-rows1956.tsv']:shutil.copyfile(args.work/name,args.output/name)
 shutil.copyfile(emitted/'optimization-inventory1956.json',args.output/'optimization-inventory1956.json')
 print(json.dumps({'status':qa['status'],'artifacts':artifacts,'host_assertions':result['assertions']},indent=2));return qa

if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for n in ['input','tools','work','output','ndk']:p.add_argument('--'+n,type=Path,required=True)
 p.add_argument('--jdk',type=Path);a=p.parse_args()
 if a.jdk:os.environ['ULIKE_JDK_HOME']=str(a.jdk.resolve());os.environ['PATH']=str(a.jdk.resolve()/'bin')+os.pathsep+os.environ.get('PATH','')
 for k,v in vars(a).items():
  if v is not None:setattr(a,k,v.resolve())
 build(a)
