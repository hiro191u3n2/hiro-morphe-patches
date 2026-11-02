#!/usr/bin/env python3
"""Build candidate MPPs; this command never edits feeds or publishes a release.
The existing complete DEX/ABI/loader/other-app preservation checks remain active.
Physical-device tests and application of these exact MPP bytes are separate gates.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re,subprocess,sys
HERE=Path(__file__).resolve().parent
GENERATED=HERE/'generated'
SOURCE=GENERATED/'src'
BASE=HERE.parent/'ulike19101-pipeline/src'
BASE_GPU_SHA='1707c79898fe2477b6b5fea45a57c84c22bd1773fa06616d5c281e1b7e4d6d18'
BASE_GPU_BYTES=171856
BASE_FP='6676e26c63c28e8fe0b4b4f0601d723bb65cab6f68604633fbf129faec45deea'
NR_SHA='77aecb1fafdb0b13b152225f70a9e01a7cbd303c1267aa386181246e456e9f10'
FLAGS=['-std=c11','-O3','-shared','-fPIC','-fvisibility=hidden','-fno-fast-math','-ffp-contract=off','-Wl,--no-undefined','-Wl,--build-id=sha1','-Wl,-z,max-page-size=16384','-Wl,-soname,libulike_gpu1960.so','-Wall','-Wextra','-Werror','-Wno-misleading-indentation']
def sha(raw):return hashlib.sha256(raw).hexdigest()
def need(value,message):
 if not value:raise AssertionError(message)
def load(name,path):
 spec=importlib.util.spec_from_file_location(name,path);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module

def native_inverse(source,ndk,native_out,work,baseline,nr_baseline=None):
 from native_identity1999 import generated_header,inspect
 source,ndk,native_out,work=map(Path,(source,ndk,native_out,work))
 need(sha(baseline)==BASE_GPU_SHA and len(baseline)==BASE_GPU_BYTES,'Exact published .101 GPU baseline required')
 need(isinstance(nr_baseline,bytes) and sha(nr_baseline)==NR_SHA and len(nr_baseline)==25984,'Exact inherited NR1955 required')
 manifest_path='native1960/compute_dependencies1990.json'
 need((source/manifest_path).read_bytes()==(BASE/manifest_path).read_bytes(),'Dependency classification must remain unchanged')
 manifest=json.loads((source/manifest_path).read_text());native_pins={};delta=[]
 for name in manifest['native_dependencies']:
  current=(source/name).read_bytes();original=(BASE/name).read_bytes();native_pins[name]=sha(current)
  if current!=original:delta.append(name)
 need(delta==['native1960/build_native1960.py'],'Only the reviewed fingerprint builder may change among native inputs: '+repr(delta))
 review=json.loads((GENERATED/'review/source-edits.json').read_text())
 changed=review['files']['native1960/build_native1960.py']
 need(changed['before_sha256']==sha((BASE/'native1960/build_native1960.py').read_bytes()) and changed['after_sha256']==native_pins['native1960/build_native1960.py'],'Native builder differs from guarded edit')
 builder=load('compute19102_native',source/'native1960/build_native1960.py')
 fingerprint=builder.compute_fingerprint1990(source/'native1960')[0]
 current=(native_out/'libulike_gpu1960.so').read_bytes();inspect(current,fingerprint)
 need(not work.exists(),'Fresh inverse work directory required');work.mkdir(parents=True)
 need(re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',(ndk/'source.properties').read_text(),re.M),'Pinned NDK required')
 header=(native_out/'shader_sources1960.h').read_text()
 need(header==generated_header(source,fingerprint),'Generated header must contain exact tracked shaders')
 old='#define GPU1960_SOURCE_SHA256 "'+fingerprint+'"';new='#define GPU1960_SOURCE_SHA256 "'+BASE_FP+'"'
 need(header.count(old)==1 and header.count(fingerprint)==1,'Exactly one complete fingerprint literal required')
 inverse=header.replace(old,new,1)
 need(inverse==generated_header(BASE,BASE_FP),'Inverse shaders differ from published .101 source')
 clang=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang'
 def compile_header(label,body):
  folder=work/label;folder.mkdir();(folder/'shader_sources1960.h').write_text(body);target=folder/'libulike_gpu1960.so'
  command=[str(x) for x in [clang,*FLAGS,'-I'+str(folder),source/'native1960/engine1960.c',source/'native1960/residual_blocks1961.c','-lEGL','-lGLESv3','-lm','-o',target]]
  p=subprocess.run(command,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
  (work/(label+'.log')).write_text(p.stdout);need(p.returncode==0,label+' build failed: '+p.stdout[-3000:]);return target.read_bytes()
 forward=compile_header('forward',header);need(forward==current,'Current native forward rebuild must match complete ELF')
 restored=compile_header('inverse',inverse);need(restored==baseline,'Only restoring identity must recover complete .101 native ELF')
 report={'schema':'ulike19102-native-fingerprint-inverse-build-v1','status':'passed','current_sha256':sha(current),'current_bytes':len(current),'baseline_release':'ulike-v1.9.101','baseline_sha256':BASE_GPU_SHA,'baseline_bytes':BASE_GPU_BYTES,'compute_fingerprint':fingerprint,'baseline_compute_fingerprint':BASE_FP,'same_ndk_forward_rebuild_verified':True,'forward_rebuild_sha256':sha(forward),'same_ndk_inverse_rebuild_verified':True,'inverse_rebuild_sha256':sha(restored),'generated_header_delta':['GPU1960_SOURCE_SHA256'],'generated_header_sha256':sha(header.encode()),'inverse_header_sha256':sha(inverse.encode()),'native_source_sha256':native_pins,'native_source_delta':delta,'native_shader_and_cpu_sources_identical_to_19101':True,'ndk_revision':'27.2.12479018','compile_flags':FLAGS,'link_libraries':['EGL','GLESv3','m'],'nr1955_byte_identical':True,'physical_android_tested':False}
 (work/'result.json').write_text(json.dumps(report,sort_keys=True,indent=2)+'\n');return report

def main():
 parser=argparse.ArgumentParser(description=__doc__)
 for name in ('input','tools','work','output','jdk','ndk'):parser.add_argument('--'+name,type=Path,required=True)
 args=parser.parse_args()
 for name,value in vars(args).items():setattr(args,name,value.resolve())
 need(SOURCE.is_dir(),'Run prepare.py first')
 # Human-readable package description, not an executable or proof exemption.
 target=SOURCE/'build19101.py';raw=target.read_text()
 old='ULike .101 bounded validation-history persistence, process identities, saved/current status labeling, and restored-record diagnostics. Pixel algorithms, qualification gates, image settings, and other apps preserved; physical Android speed not measured.'
 new='ULike .102 candidate: bounded resident upload budgets, production-equivalent controlled GPU probes, stable explicit diagnostic identity, and terminal failure accounting. Numerical image kernels and other apps preserved. Original-APK and physical-device validation pending.'
 need(raw.count(old)==1,'Expected metadata description');target.write_text(raw.replace(old,new))
 sys.path.insert(0,str(SOURCE));import build19101 as build
 build.VERSION='1.9.102';build.BUNDLE_VERSION='1.0.240';build.BASE_VERSION='1.9.101';build.BASE_BUNDLE_VERSION='1.0.239'
 build.SINGLE='ULike_HQ_Texture_Online_v1.9.102.mpp';build.BUNDLE='Hiro_Morphe_Patches_v1.0.240.mpp'
 build.BASE_SINGLE='ULike_HQ_Texture_Online_v1.9.101.mpp';build.BASE_BUNDLE='Hiro_Morphe_Patches_v1.0.239.mpp'
 build.BASE_PINS={build.BASE_SINGLE:{'bytes':1450415,'sha256':'d0ee9a36d08cbcfaf5213b7a13459f2878f2c061fa5c8d8e8de0d49100de9aae'},build.BASE_BUNDLE:{'bytes':18123390,'sha256':'accc1c5da74dde1e26f727d7b2c18855503e3bc54c4d0216042c878268cbfbd9'}}
 # Replace the version-specific .101 inverse verifier, not any build gates.
 build.native_identity19101.build_inverse=native_inverse
 baseline_manifest=json.loads((BASE.parent/'manifest.json').read_text())
 declaration={'schema':'ulike19101-build-declaration-v1','ulike_version':'1.9.102','runtime_version':'1.9.102','bundle_version':'1.0.240','timing_version_matches_release':True,'production_helper_roots':baseline_manifest['production_helper_roots'],'reviewed_source_sha256':build.source_pins(),'reviewed_publication_sha256':build.publication_pins()}
 args.declaration=GENERATED/'candidate-declaration.json';args.declaration.write_text(json.dumps(declaration,sort_keys=True,indent=2)+'\n')
 report=build.build(args)
 report['schema']='ulike19102-candidate-build-evidence-v1';report['release_published']=False
 (args.output/'build19102.json').write_text(json.dumps(report,sort_keys=True,indent=2)+'\n')
 (args.output/'CANDIDATE_NOT_RELEASED.txt').write_text('ULike 1.9.102 candidate / bundle 1.0.240 candidate.\nOriginal APKS application and physical Android validation are NOT completed.\nManager feeds are unchanged. Do not present this candidate as a verified public release.\n')
 print(json.dumps({'candidate_build':'passed','artifacts':report['artifacts'],'other_apps_preserved':report['other_apps_preserved'],'release_published':False},indent=2))
if __name__=='__main__':main()
