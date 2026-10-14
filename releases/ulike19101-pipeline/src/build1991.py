#!/usr/bin/env python3
"""Rebuild reviewed ULike runtime families and the GPU payload over exact published .90.

This script emits structural/build evidence only. Publication also requires the fresh
host contract and application evidence checked by validate1991.py.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, shutil, struct, zlib
import build1945 as common
import build1990 as previous
ROOT=Path(__file__).resolve().parent
VERSION, BUNDLE_VERSION='1.9.91','1.0.224'
BASE_VERSION, BASE_BUNDLE_VERSION='1.9.90','1.0.223'
SINGLE='ULike_HQ_Texture_Online_v1.9.91.mpp'
BUNDLE='Hiro_Morphe_Patches_v1.0.224.mpp'
BASE_SINGLE='ULike_HQ_Texture_Online_v1.9.90.mpp'
BASE_BUNDLE='Hiro_Morphe_Patches_v1.0.223.mpp'
BASE_PINS={BASE_SINGLE: {'sha256':'6eeea95699571fc4a97bf77205925153e1b84d9f6a63c5835f275cdd1859a1d7','bytes':1374122}, BASE_BUNDLE:{'sha256':'95527a84fd12c1d8fca823318459494ea965e80337f1a2420bdf9f3815834761','bytes':18021131}}
GPU_ENTRY='ulike1960/runtime/libulike_gpu1960.so'
INSTALLER='app/hiro/ulike/patches/IntegrationPayload186.class'
LOADER='app/hiro/ulike/patches/UlikeHqMaxPatch.class'
CHANGED={'META-INF/MANIFEST.MF','classes.dex','ulike/runtime.dex',LOADER,INSTALLER,GPU_ENTRY}
TOOL_PINS=common.TOOL_PINS
require,sha,run,archive,headers,manifest,own,write_zip=common.require,common.sha,common.run,common.archive,common.headers,common.manifest,common.own,common.write_zip

def json_bytes(x):return (json.dumps(x,ensure_ascii=False,sort_keys=True,indent=2)+'\n').encode()
def module(name,path):
 s=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m

def source_pins():
 return {p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in sorted(ROOT.rglob('*')) if p.is_file() and not p.is_symlink() and '__pycache__' not in p.parts and p.suffix in ('.java','.py','.c','.h','.comp','.glsl','.sh','.txt','.json','.dex','.tsv','.md')}
def publication_pins():
 return {name:sha((ROOT.parent/'publication'/name).read_bytes()) for name in ('publish1991.py','ulike1991-pipeline-publish.yml','README.md')}

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

def build(args):
 os.environ['PATH']=str(args.jdk/'bin')+os.pathsep+os.environ.get('PATH','')
 identity=common.verify_jdk()
 declaration=json.loads(args.declaration.read_text())
 require(declaration.get('schema')=='ulike1991-build-declaration-v1','Wrong declaration schema')
 pins=source_pins();pubpins=publication_pins();require(declaration.get('reviewed_source_sha256')==pins and declaration.get('reviewed_publication_sha256')==pubpins,'Reviewed source/publication changed')
 production=declaration['production_helper_roots'];require(production and production==sorted(set(production)),'Unique sorted production roots required')
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
 exports=set(re.findall(r'\bJava_[A-Za-z0-9_]+',elf));prior_exports=set(re.findall(r'\bJava_[A-Za-z0-9_]+',prior_elf));added_exports={'Java_com_hiro_ulike_GpuNoise1960_releaseSlotsNative1991'};require(exports==prior_exports|added_exports and not prior_exports&added_exports,'JNI exports differ from exact inherited set plus reviewed slot disposal')
 align=[int(l.split()[-1],0) for l in elf.splitlines() if l.strip().startswith('LOAD ')];needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',elf)
 require(align and all(n>=16384 for n in align) and set(needed)<={'libc.so','libm.so','libdl.so','libEGL.so','libGLESv3.so'},'Native alignment/dependencies differ')
 bundle_dex=baseline/'bundle.dex';bundle_dex.write_bytes(bundle['classes.dex'])
 classes,helpers,dex=(args.work/n for n in ('classes','helper-classes','helper-dex'))
 for p in (classes,helpers,dex):p.mkdir()
 sources=[production_path(n) for n in production];stubmap=compile_inputs(production)
 run(['javac','-source','8','-target','8','-encoding','UTF-8','-bootclasspath',args.tools/'android.jar','-d',helpers,*stubmap.values(),*sources],args.work/'javac.log')
 selected=sorted(p for p in (helpers/'com/hiro/ulike').glob('*.class') if any(p.name==n+'.class' or p.name.startswith(n+'$') for n in production))
 require(all(helpers/'com/hiro/ulike'/(n+'.class') in selected for n in production),'Compiled production root missing')
 run(['java','-cp',args.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',args.tools/'android.jar','--classpath',helpers,'--output',dex,*selected],args.work/'d8.log')
 cp=os.pathsep.join(map(str,(args.tools/'morphe.jar',baseline,classes)))
 trans=['MergePayloads','Transform1991','TimingCameraHooks1980','VerifyHelperReferences','VerifyAllHelperReferences1980']
 run(['javac','-encoding','UTF-8','-cp',cp,'-d',classes,*[ROOT/(n+'.java') for n in trans]],args.work/'transform-javac.log')
 asm=['--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','--add-exports','java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED']
 run(['javac','-encoding','UTF-8',*asm,'-cp',cp,'-d',classes,ROOT/'PatchClass1991.java',ROOT/'PatchLoader1991.java'],args.work/'metadata-javac.log')
 props=['-Dulike.production='+','.join(production),'-Dulike.gpu1960.sha='+sha(payload),'-Dulike.gpu1960.bytes='+str(len(payload))]
 for n in ('emitted','repeat'):run(['java',*props,'-cp',cp,'Transform1991',baseline,dex/'classes.dex',bundle_dex,args.work/n,args.work/(n+'-audit.tsv')],args.work/(n+'.log'))
 emitted=args.work/'emitted';require({p.name:p.read_bytes() for p in emitted.iterdir()}=={p.name:p.read_bytes() for p in (args.work/'repeat').iterdir()},'Non-deterministic DEX transformation')
 run(['java',*asm,'-cp',cp,'PatchClass1991',baseline/LOADER,emitted/'UlikeHqMaxPatch.class'],args.work/'metadata.log')
 run(['java',*asm,'-cp',cp,'PatchLoader1991',baseline/INSTALLER,native,emitted/'IntegrationPayload186.class'],args.work/'installer.log')
 refs=run(['java','-cp',cp,'VerifyAllHelperReferences1980',baseline/'ulike/methods.dex',baseline/'ulike/runtime.dex',emitted/'methods.dex',emitted/'runtime.dex'],args.work/'all-helper-references.txt');require(refs.startswith('PASS helper references'),'Unresolved runtime references')
 final,integrated=dict(base),dict(bundle)
 updates={'ulike/runtime.dex':(emitted/'runtime.dex').read_bytes(),LOADER:(emitted/'UlikeHqMaxPatch.class').read_bytes(),INSTALLER:(emitted/'IntegrationPayload186.class').read_bytes(),GPU_ENTRY:payload}
 final.update(updates);integrated.update(updates)
 final['classes.dex']=(emitted/'loader.dex').read_bytes();integrated['classes.dex']=(emitted/'bundle-loader.dex').read_bytes()
 for data,version in ((final,VERSION),(integrated,BUNDLE_VERSION)):
  f=headers(data['META-INF/MANIFEST.MF']);f.update(Version=version,Description='ULike .91 bounded full-output validation, GPU stage readiness, pipeline transport and capture-request-to-save diagnostics. Physical Android quality and speed not measured.');data['META-INF/MANIFEST.MF']=manifest(f)
 deltas={}
 for kind,old,new in (('standalone',base,final),('bundle',bundle,integrated)):
  delta={k for k in old if old[k]!=new.get(k)};require(set(old)==set(new) and delta==CHANGED,'Unexpected '+kind+' delta '+repr(delta));deltas[kind]=sorted(delta)
 require({k:v for k,v in final.items() if own(k)}=={k:v for k,v in integrated.items() if own(k)},'ULike standalone/bundle mismatch')
 require(all(integrated[k]==v for k,v in bundle.items() if not own(k) and k not in ('classes.dex','META-INF/MANIFEST.MF')),'Other apps changed')
 inherited={k:sha(v) for k,v in base.items() if (k.endswith('.so') or k=='ulike186/runtime/0000.bin') and k!=GPU_ENTRY}
 require(len(inherited)==11 and all(final[k]==base[k] for k in inherited),'Eleven other native payloads changed')
 require(all(final[k]==base[k] for k in ('ulike/methods.dex','ulike/methods.tsv')),'Stock patch method contract changed')
 for kind,new in (('standalone',final),('bundle',integrated)):
  for name in ('classes.dex','ulike/runtime.dex'):dex_integrity(new[name],kind+'/'+name)
 write_zip(args.output/SINGLE,final);write_zip(args.output/BUNDLE,integrated)
 for name in (SINGLE,BUNDLE):run(['java','-jar',args.tools/'morphe.jar','list-patches','--patches',args.output/name],args.work/(name+'.loader.log'))
 require(source_pins()==pins and publication_pins()==pubpins,'Source/publication changed during build')
 artifacts={n:{'sha256':sha((args.output/n).read_bytes()),'bytes':(args.output/n).stat().st_size} for n in (SINGLE,BUNDLE)}
 if 'reviewed_mpp_artifacts' in declaration:require(declaration['reviewed_mpp_artifacts']==artifacts,'Rebuilt MPP differs from locally applied artifact')
 report={'schema':'ulike1991-build-evidence-v1','status':'BUILT_STRUCTURALLY_VERIFIED_HOST_AND_DEVICE_PENDING','ulike_version':VERSION,'bundle_version':BUNDLE_VERSION,'baseline':BASE_PINS,'artifacts':artifacts,'jdk_identity':identity,'tool_sha256':TOOL_PINS,'source_sha256':pins,'publication_sha256':pubpins,'production_helper_roots':production,'compiled_production_sha256':{p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in sources},'resource_delta':deltas,'inherited_native_sha256':inherited,'gpu_native':{'sha256':sha(payload),'bytes':len(payload),'build':'rebuilt_from_current_sources','ndk_revision':'27.2.12479018','jni_exports':sorted(exports),'added_jni_exports':sorted(added_exports),'inherited_jni_exports_preserved':True,'segment_alignment':align,'dependencies':needed},'structural_inventory':json.loads((emitted/'inventory1991.json').read_text()),'runtime_helper_references_verified':True,'deterministic_dex_verified':True,'other_apps_preserved':True,'host_tests_passed':False,'original_apk_apply_tested':False,'device_tested':False,'device_speed_measured':False,'device_quality_verified':False}
 (args.output/'build1991.json').write_bytes(json_bytes(report));print(json.dumps({'status':report['status'],'artifacts':artifacts},indent=2));return report

if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for n in ('input','tools','work','output','jdk','ndk','declaration'):p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args()
 for n,v in vars(a).items():setattr(a,n,v.resolve())
 build(a)
