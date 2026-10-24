#!/usr/bin/env python3
"""Rebuild reviewed ULike runtime families over exact published .114 and bundle .282.

This script emits structural/build evidence only. Publication also requires the fresh
host contract and application evidence checked by validate19101.py.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, shutil, struct, zlib
import build1945 as common
import build19100 as previous
import regional_distribution19115
import native_identity19101
import native_provenance19115
import native_smooth_provenance19115
ROOT=Path(__file__).resolve().parent
VERSION, BUNDLE_VERSION='1.9.116','1.0.283'
BASE_VERSION, BASE_BUNDLE_VERSION='1.9.114','1.0.282'
SINGLE='ULike_HQ_Texture_Online_v1.9.116.mpp'
BUNDLE='Hiro_Morphe_Patches_v1.0.283.mpp'
BASE_SINGLE='ULike_HQ_Texture_Online_v1.9.114.mpp'
BASE_BUNDLE='Hiro_Morphe_Patches_v1.0.282.mpp'
BASE_PINS={BASE_SINGLE:{'bytes':1513570,'sha256':'b3f688b09ddeb3e2756cb68669589186fa348f7eab098255cafe80cf0328e8d3'},BASE_BUNDLE:{'bytes':19050673,'sha256':'b30217a6daa8dff127732f4e7f71b06a5e38d0ab2e021601f913c7f350cc5864'}}
BASE_REFS={BASE_SINGLE:'ac0b19e3827e0e168f8b85c9bd5a8777a2a16207',BASE_BUNDLE:'c214d69c1b3971724a7e12c001cfe67de77cff6b'}
GPU_ENTRY='ulike1960/runtime/libulike_gpu1960.so'
NR_ENTRY='ulike1955/runtime/libulike_nr1955.so'
SMOOTH_ENTRY='ulike1958/runtime/libulike_smooth1958.so'
INSTALLER='app/hiro/ulike/patches/IntegrationPayload186.class'
LOADER='app/hiro/ulike/patches/UlikeHqMaxPatch.class'
CHANGED={'META-INF/MANIFEST.MF','classes.dex','ulike/runtime.dex',LOADER,INSTALLER,GPU_ENTRY,NR_ENTRY,SMOOTH_ENTRY}
TOOL_PINS=common.TOOL_PINS
require,sha,run,archive,headers,manifest,own,write_zip=common.require,common.sha,common.run,common.archive,common.headers,common.manifest,common.own,common.write_zip

def json_bytes(x):return (json.dumps(x,ensure_ascii=False,sort_keys=True,indent=2)+'\n').encode()
def module(name,path):
 s=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m

def source_pins():
 return {p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in sorted(ROOT.rglob('*')) if p.is_file() and not p.is_symlink() and '__pycache__' not in p.parts and p.suffix in ('.java','.py','.c','.h','.comp','.glsl','.sh','.txt','.json','.dex','.tsv','.md')}
def publication_pins():
 return {name:sha((ROOT.parent/'publication'/name).read_bytes()) for name in ('publish19101.py','ulike19101-pipeline-publish.yml','README.md')}

def production_path(name):
 require(re.fullmatch(r'[A-Za-z][A-Za-z0-9_]*',name) is not None,'Invalid root '+name)
 p=ROOT/(name+'.java')
 if not p.is_file():p=ROOT/'quality-dependencies/com/hiro/ulike'/(name+'.java')
 require(p.is_file(),'Missing root '+name);return p

def compile_inputs(production):
 stubs=previous.compile_inputs([])
 # Pick current declarations for all production peers; only reviewed roots enter D8.
 for p in ROOT.glob('*.java'):
  if not p.name.endswith('Test.java') and re.search(r'^package com\.hiro\.ulike;',p.read_text(),re.M):stubs['com/hiro/ulike/'+p.name]=p
 for name in production:stubs.pop('com/hiro/ulike/'+name+'.java',None)
 return stubs

def dex_integrity(raw,name):
 require(len(raw)>=112 and raw[:4]==b'dex\n' and struct.unpack_from('<I',raw,32)[0]==len(raw),'Bad dex '+name)
 require(raw[12:32]==hashlib.sha1(raw[32:]).digest() and struct.unpack_from('<I',raw,8)[0]==zlib.adler32(raw[12:])&0xffffffff,'Dex integrity '+name)

def runtime_source_version(path=None):
 p=Path(path) if path is not None else ROOT/'ProcessingTiming1947.java'
 values=re.findall(r'public\s+static\s+final\s+String\s+VERSION\s*=\s*"([^"\r\n]+)"\s*;',p.read_text())
 require(values==[VERSION],'Timing source VERSION differs from MPP release: '+repr(values));return values[0]

def build(args):
 os.environ['PATH']=str(args.jdk/'bin')+os.pathsep+os.environ.get('PATH','')
 identity=common.verify_jdk();runtime_version=runtime_source_version()
 declaration=json.loads(args.declaration.read_text())
 require(declaration.get('schema')=='ulike19101-build-declaration-v1','Wrong declaration schema')
 require(declaration.get('ulike_version')==declaration.get('runtime_version')==runtime_version==VERSION and declaration.get('bundle_version')==BUNDLE_VERSION and declaration.get('timing_version_matches_release') is True,'Declared and timing runtime versions differ')
 pins=source_pins();pubpins=publication_pins();require(declaration.get('reviewed_source_sha256')==pins and declaration.get('reviewed_publication_sha256')==pubpins,'Reviewed source/publication changed')
 production=declaration['production_helper_roots'];require(production and production==sorted(set(production)),'Unique sorted production roots required');require('GpuRegionalDefault19115' not in production,'Experimental helper must not be distributed')
 require(not args.work.exists(),'Fresh build directory required');args.work.mkdir(parents=True);args.output.mkdir(parents=True,exist_ok=True)
 for name,pin in BASE_PINS.items():
  p=args.input/name;require(p.stat().st_size==pin['bytes'] and sha(p.read_bytes())==pin['sha256'],'Wrong baseline '+name)
 for name,digest in TOOL_PINS.items():require(sha((args.tools/name).read_bytes())==digest,'Wrong tool '+name)
 base,bundle=archive(args.input/BASE_SINGLE),archive(args.input/BASE_BUNDLE)
 require(headers(base['META-INF/MANIFEST.MF'])['Version']==BASE_VERSION and headers(bundle['META-INF/MANIFEST.MF'])['Version']==BASE_BUNDLE_VERSION,'Baseline versions differ')
 require({k:v for k,v in base.items() if own(k)}=={k:v for k,v in bundle.items() if own(k)},'Baseline ULike resources differ')
 baseline=args.work/'baseline'
 for name,data in base.items():p=baseline/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
 require(re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',(args.ndk/'source.properties').read_text(),re.M),'Pinned NDK r27c required')
 native_out=args.work/'native1960';run(['python3',ROOT/'native1960/build_native1960.py','--ndk',args.ndk,'--out',native_out],args.work/'native-build.log')
 native=native_out/'libulike_gpu1960.so';payload=native.read_bytes();require(payload[:6]==b'\x7fELF\x02\x01' and payload[18:20]==b'\xb7\x00','ELF64 AArch64 required')
 ndkbin=args.ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin'
 elf=run([ndkbin/'llvm-readelf','-h','-d','-Ws','--program-headers','--wide',native],args.work/'native-readelf.txt')
 prior_elf=run([ndkbin/'llvm-readelf','-Ws','--wide',baseline/GPU_ENTRY],args.work/'native-baseline-readelf.txt')
 exports=set(re.findall(r'\bJava_[A-Za-z0-9_]+',elf));prior_exports=set(re.findall(r'\bJava_[A-Za-z0-9_]+',prior_elf));added_exports={'Java_com_hiro_ulike_GpuNoise1960_regionalReadyNative19115'};require(exports==prior_exports|added_exports,'JNI exports differ from exact inherited and reviewed added set')
 align=[int(l.split()[-1],0) for l in elf.splitlines() if l.strip().startswith('LOAD ')];needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',elf)
 require(align and all(n>=16384 for n in align) and set(needed)<={'libc.so','libm.so','libdl.so','libEGL.so','libGLESv3.so'},'Native alignment/dependencies differ')
 nr_out=args.work/'native1955';run(['python3',ROOT/'native1955/build_native1955.py','--ndk',args.ndk,'--output',nr_out],args.work/'nr-build.log')
 nr_native=nr_out/'libulike_nr1955.so';nr_payload=nr_native.read_bytes();require(nr_payload!=base[NR_ENTRY],'Reviewed CPU NR native change must differ from published114')
 nr_elf=run([ndkbin/'llvm-readelf','-h','-d','-Ws','--program-headers','--wide',nr_native],args.work/'nr-readelf.txt')
 nr_prior_elf=run([ndkbin/'llvm-readelf','-Ws','--wide',baseline/NR_ENTRY],args.work/'nr-baseline-readelf.txt')
 nr_exports=set(re.findall(r'\bJava_[A-Za-z0-9_]+',nr_elf));nr_prior_exports=set(re.findall(r'\bJava_[A-Za-z0-9_]+',nr_prior_elf));require(nr_exports==nr_prior_exports,'NR1955 JNI export inventory changed')
 nr_align=[int(l.split()[-1],0) for l in nr_elf.splitlines() if l.strip().startswith('LOAD ')];nr_needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',nr_elf)
 require(nr_align and all(n>=16384 for n in nr_align) and set(nr_needed)<={'libc.so','libm.so','libdl.so'},'NR1955 alignment/dependencies differ')
 smooth_out=args.work/'native1958';run(['python3',ROOT/'native1958/build_native1958.py','--ndk',args.ndk,'--output',smooth_out],args.work/'smooth-build.log')
 smooth_native=smooth_out/'libulike_smooth1958.so';smooth_payload=smooth_native.read_bytes();require(smooth_payload!=base[SMOOTH_ENTRY],'Reviewed Strong CPU native must differ from published114')
 smooth_elf=run([ndkbin/'llvm-readelf','-h','-d','-Ws','--program-headers','--wide',smooth_native],args.work/'smooth-readelf.txt')
 smooth_prior_elf=run([ndkbin/'llvm-readelf','-Ws','--wide',baseline/SMOOTH_ENTRY],args.work/'smooth-baseline-readelf.txt')
 smooth_exports=set(re.findall(r'\bJava_[A-Za-z0-9_]+',smooth_elf));smooth_prior_exports=set(re.findall(r'\bJava_[A-Za-z0-9_]+',smooth_prior_elf));require(smooth_exports==smooth_prior_exports,'Strong CPU JNI export inventory changed')
 smooth_align=[int(l.split()[-1],0) for l in smooth_elf.splitlines() if l.strip().startswith('LOAD ')];smooth_needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',smooth_elf)
 require(smooth_align and all(n>=16384 for n in smooth_align) and set(smooth_needed)<={'libc.so','libm.so','libdl.so'},'Strong CPU alignment/dependencies differ')
 bundle_dex=baseline/'bundle.dex';bundle_dex.write_bytes(bundle['classes.dex'])
 classes,helpers,dex=(args.work/n for n in ('classes','helper-classes','helper-dex'))
 for p in (classes,helpers,dex):p.mkdir()
 sources=[production_path(n) for n in production];stubmap=compile_inputs(production)
 run(['javac','-source','8','-target','8','-encoding','UTF-8','-bootclasspath',args.tools/'android.jar','-d',helpers,*stubmap.values(),*sources],args.work/'javac.log')
 selected=sorted(p for p in (helpers/'com/hiro/ulike').glob('*.class') if any(p.name==n+'.class' or p.name.startswith(n+'$') for n in production))
 require(all(helpers/'com/hiro/ulike'/(n+'.class') in selected for n in production),'Compiled production root missing')
 run(['java','-cp',args.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',args.tools/'android.jar','--classpath',helpers,'--output',dex,*selected],args.work/'d8.log')
 cp=os.pathsep.join(map(str,(args.tools/'morphe.jar',baseline,classes)))
 trans=['MergePayloads','Transform19101','ShutterStock1994','VerifyRuntimeVersion19101','VerifyCaptureInvariant19104','VerifyStrongDiagnostic19105','VerifyStrongTrace19106','VerifyRegionalDisabled19115','TimingCameraHooks1980','VerifyHelperReferences','VerifyAllHelperReferences1980']
 run(['javac','-encoding','UTF-8','-cp',cp,'-d',classes,*[ROOT/(n+'.java') for n in trans]],args.work/'transform-javac.log')
 asm=['--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','--add-exports','java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED']
 run(['javac','-encoding','UTF-8',*asm,'-cp',cp,'-d',classes,ROOT/'PatchClass19101.java',ROOT/'PatchLoader19101.java'],args.work/'metadata-javac.log')
 props=['-Dulike.shutter.seed='+str(ROOT/'stock-seed1994.dex'),'-Dulike.version='+VERSION,'-Dulike.production='+','.join(production),'-Dulike.gpu1960.sha='+sha(payload),'-Dulike.gpu1960.bytes='+str(len(payload)),'-Dulike.nr1955.sha='+sha(nr_payload),'-Dulike.nr1955.bytes='+str(len(nr_payload)),'-Dulike.smooth1958.sha='+sha(smooth_payload),'-Dulike.smooth1958.bytes='+str(len(smooth_payload))]
 for n in ('emitted','repeat'):run(['java',*props,'-cp',cp,'Transform19101',baseline,dex/'classes.dex',bundle_dex,args.work/n,args.work/(n+'-audit.tsv')],args.work/(n+'.log'))
 emitted=args.work/'emitted';regional_disabled=regional_distribution19115.verify(json.loads(run(['java','-cp',cp,'VerifyRegionalDisabled19115',emitted/'runtime.dex'],args.work/'regional-disabled-dex.json')));require({p.name:p.read_bytes() for p in emitted.iterdir()}=={p.name:p.read_bytes() for p in (args.work/'repeat').iterdir()},'Non-deterministic DEX transformation')
 strong_diagnostic=run(['java','-cp',cp,'VerifyStrongDiagnostic19105',emitted/'runtime.dex'],args.work/'strong-diagnostic-dex.txt').strip()
 require(strong_diagnostic=='PASS emitted-strong-diagnostic foreground_cpu_guard replay_strong_guard target_null_guard strict_gate_unchanged qualification_writes_absent resident_timing_guard negative_controls=23 cpu_timing_negative_controls=11 fair_plan_negative_controls=6','Strong diagnostic DEX guard or actual negative controls failed')
 strong_trace=run(['java','-cp',cp,'VerifyStrongTrace19106',emitted/'runtime.dex'],args.work/'strong-trace-dex.txt').strip()
 require(strong_trace=='PASS emitted-strong-trace post_save_guard fixed_reproducer_guard strict_gate_unchanged qualification_writes_absent eligibility_cases=958 negative_controls=7','Strong intermediate trace DEX guard or actual negative controls failed')
 run(['java',*asm,'-cp',cp,'PatchClass19101',baseline/LOADER,emitted/'UlikeHqMaxPatch.class'],args.work/'metadata.log')
 run(['java',*asm,'-cp',cp,'PatchLoader19101',baseline/INSTALLER,native,nr_native,smooth_native,emitted/'IntegrationPayload186.class'],args.work/'installer.log')
 refs=run(['java','-cp',cp,'VerifyAllHelperReferences1980',baseline/'ulike/methods.dex',baseline/'ulike/runtime.dex',emitted/'methods.dex',emitted/'runtime.dex'],args.work/'all-helper-references.txt');require(refs.startswith('PASS helper references'),'Unresolved runtime references')
 final,integrated=dict(base),dict(bundle)
 updates={'ulike/methods.dex':(emitted/'methods.dex').read_bytes(),'ulike/methods.tsv':(emitted/'methods.tsv').read_bytes(),'ulike/runtime.dex':(emitted/'runtime.dex').read_bytes(),LOADER:(emitted/'UlikeHqMaxPatch.class').read_bytes(),INSTALLER:(emitted/'IntegrationPayload186.class').read_bytes(),GPU_ENTRY:payload,NR_ENTRY:nr_payload,SMOOTH_ENTRY:smooth_payload}
 final.update(updates);integrated.update(updates)
 final['classes.dex']=(emitted/'loader.dex').read_bytes();integrated['classes.dex']=(emitted/'bundle-loader.dex').read_bytes()
 for data,version in ((final,VERSION),(integrated,BUNDLE_VERSION)):
  f=headers(data['META-INF/MANIFEST.MF']);f.update(Version=version,Description='ULike .116 candidate: compare GPU chain speed only with a matching Strong execution plan; preserve the 5 percent speed gate, exact image output, quality settings and existing GPU routes. Physical save latency under 2000ms remains unverified.');data['META-INF/MANIFEST.MF']=manifest(f)
 deltas={}
 for kind,old,new in (('standalone',base,final),('bundle',bundle,integrated)):
  delta={k for k in old if old[k]!=new.get(k)};require(set(old)==set(new) and delta==CHANGED,'Unexpected '+kind+' delta '+repr(delta));deltas[kind]=sorted(delta)
 require({k:v for k,v in final.items() if own(k)}=={k:v for k,v in integrated.items() if own(k)},'ULike standalone/bundle mismatch')
 require(all(integrated[k]==v for k,v in bundle.items() if not own(k) and k not in ('classes.dex','META-INF/MANIFEST.MF')),'Other apps changed')
 inherited={k:sha(v) for k,v in base.items() if (k.endswith('.so') or k=='ulike186/runtime/0000.bin') and k not in (GPU_ENTRY,NR_ENTRY,SMOOTH_ENTRY)}
 require(len(inherited)==9 and all(final[k]==base[k] for k in inherited),'Nine inherited native payloads changed')
 stock_hook=json.loads((emitted/'stock-shutter19101.json').read_text())
 require(stock_hook.get('added_method_count')==0 and stock_hook.get('existing_methods_preserved') is True and stock_hook.get('byte_identical_to_baseline') is True,'Existing stock shutter method must remain byte-identical')
 native_proof=native_provenance19115.build(ROOT,args.ndk,native_out,nr_out,args.work/'native-provenance19115',base[GPU_ENTRY],base[NR_ENTRY])
 smooth_proof=native_smooth_provenance19115.build(ROOT,args.ndk,smooth_out,args.work/'smooth-provenance19115',base[SMOOTH_ENTRY])
 require(all(final[k]==base[k] for k in ('ulike/methods.dex','ulike/methods.tsv')),'Stock methods bytes changed')
 for kind,new in (('standalone',final),('bundle',integrated)):
  for name in ('classes.dex','ulike/runtime.dex','ulike/methods.dex'):dex_integrity(new[name],kind+'/'+name)
 write_zip(args.output/SINGLE,final);write_zip(args.output/BUNDLE,integrated)
 for name in (SINGLE,BUNDLE):run(['java','-jar',args.tools/'morphe.jar','list-patches','--patches',args.output/name],args.work/(name+'.loader.log'))
 require(source_pins()==pins and publication_pins()==pubpins,'Source/publication changed during build')
 artifacts={n:{'sha256':sha((args.output/n).read_bytes()),'bytes':(args.output/n).stat().st_size} for n in (SINGLE,BUNDLE)}
 if 'reviewed_mpp_artifacts' in declaration:require(declaration['reviewed_mpp_artifacts']==artifacts,'Rebuilt MPP differs from locally applied artifact')
 report={'schema':'ulike19101-build-evidence-v1','status':'BUILT_STRUCTURALLY_VERIFIED_HOST_AND_DEVICE_PENDING','ulike_version':VERSION,'bundle_version':BUNDLE_VERSION,'runtime_version':runtime_version,'timing_version_matches_release':True,'baseline':BASE_PINS,'artifacts':artifacts,'jdk_identity':identity,'tool_sha256':TOOL_PINS,'source_sha256':pins,'publication_sha256':pubpins,'production_helper_roots':production,'compiled_production_sha256':{p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in sources},'resource_delta':deltas,'inherited_native_sha256':inherited,'gpu_native':{'sha256':sha(payload),'bytes':len(payload),'build':'rebuilt_from_current_sources','byte_identical_to_baseline':payload==base[GPU_ENTRY],'optimization_provenance':native_proof,'ndk_revision':'27.2.12479018','jni_exports':sorted(exports),'added_jni_exports':sorted(added_exports),'inherited_jni_exports_preserved':True,'segment_alignment':align,'dependencies':needed},'nr_native':{'sha256':sha(nr_payload),'bytes':len(nr_payload),'build':'rebuilt_from_current_sources','byte_identical_to_baseline':nr_payload==base[NR_ENTRY],'jni_exports':sorted(nr_exports),'inherited_jni_exports_preserved':True,'ndk_revision':'27.2.12479018','segment_alignment':nr_align,'dependencies':nr_needed},'smooth_native':{'sha256':sha(smooth_payload),'bytes':len(smooth_payload),'build':'rebuilt_from_current_sources','byte_identical_to_baseline':False,'optimization_provenance':smooth_proof,'jni_exports':sorted(smooth_exports),'inherited_jni_exports_preserved':True,'ndk_revision':'27.2.12479018','segment_alignment':smooth_align,'dependencies':smooth_needed},'stock_shutter_hook':stock_hook,'structural_inventory':json.loads((emitted/'inventory19101.json').read_text()),'distributed_regional_dex':regional_disabled,'experimental_gpu_helper_packaged':False,'experimental_gpu_auto_adoption':False,'runtime_helper_references_verified':True,'deterministic_dex_verified':True,'fixed_strong_diagnostic_dex_verified':True,'strong_trace_dex_verified':True,'strong_trace_eligibility_cases':958,'strong_trace_negative_controls':7,'strong_diagnostic_negative_controls':23,'fair_plan_dex_negative_controls':6,'cpu_timing_dex_negative_controls':11,'other_apps_preserved':True,'host_tests_passed':False,'original_apk_apply_tested':False,'device_tested':False,'device_speed_measured':False,'device_quality_verified':False}
 (args.output/'build19101.json').write_bytes(json_bytes(report));print(json.dumps({'status':report['status'],'artifacts':artifacts},indent=2));return report

if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for n in ('input','tools','work','output','jdk','ndk','declaration'):p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args()
 for n,v in vars(a).items():setattr(a,n,v.resolve())
 build(a)
