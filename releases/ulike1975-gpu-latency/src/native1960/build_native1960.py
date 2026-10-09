#!/usr/bin/env python3
"""Build real GX compute kernels into the pinned arm64 Android GLES backend."""
import argparse,hashlib,json,pathlib,re,subprocess
NDK_REVISION='27.2.12479018'
NAMES=['strong1960','single1960','analysis1960','geometry1960','analysis1961','residual1961','protection1961','compare1961','finish1961']
JNI_NAMES=['nativeAbi','openNative','supportedNative','retainedNative','environmentNative','allocateNative','uploadIntsNative','uploadFloatsNative','dispatchNative','readIntsNative','closeNative','batchNative','executeNative','submitNative','readManyNative','trimNative','ticketReadyNative','timeoutNative','workgroupNative','readIntoNative','readManyIntoNative','capacityNative1971','failureCodeNative1971']
def run(args):
 p=subprocess.run([str(x) for x in args],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 if p.returncode:raise RuntimeError(p.stdout)
 return p.stdout
def shader_header(source,out):
 digest=hashlib.sha256((source/'engine1960.c').read_bytes())
 for dependency in [source/'build_native1960.py',source/'residual_blocks1961.c',source.parent/'native1955/single_noise1955.c']+sorted(source.parent.glob('*.java'))+sorted((source.parent/'quality-dependencies').rglob('*.java')):
  if dependency.exists():digest.update(dependency.read_bytes())
 result='/* Generated verbatim from the tracked GX1960 compute sources. */\n'
 for name in NAMES:
  data=(source/(name+'.comp')).read_bytes();digest.update(data)
  result+='static const char '+name+'_source[]=\n'+''.join(json.dumps(line+'\n')+'\n' for line in data.decode().splitlines())+';\n'
 result+='#define GPU1960_SOURCE_SHA256 "'+digest.hexdigest()+'"\n'
 target=out/'shader_sources1960.h';target.write_text(result);return target

def build(ndk,out):
 source=pathlib.Path(__file__).resolve().parent;ndk=pathlib.Path(ndk).resolve();out=pathlib.Path(out).resolve();out.mkdir(parents=True,exist_ok=True)
 if not re.search(r'^Pkg.Revision\s*=\s*'+re.escape(NDK_REVISION)+r'\s*$',(ndk/'source.properties').read_text(),re.M):raise RuntimeError('NDK revision must be '+NDK_REVISION)
 tool=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin';shader_header(source,out)
 target=out/'libulike_gpu1960.so'
 flags=['-std=c11','-O3','-shared','-fPIC','-fvisibility=hidden','-fno-fast-math','-ffp-contract=off','-Wl,--no-undefined','-Wl,--build-id=sha1','-Wl,-z,max-page-size=16384','-Wl,-soname,libulike_gpu1960.so','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-I'+str(out)]
 run([tool/'aarch64-linux-android26-clang',*flags,source/'engine1960.c',*sorted(source.glob('residual_blocks1961.c')),'-lEGL','-lGLESv3','-lm','-o',target])
 elf=run([tool/'llvm-readelf','-h','-d','-Ws','--program-headers','--wide',target])
 for name in JNI_NAMES:
  if 'Java_com_hiro_ulike_GpuNoise1960_'+name not in elf:raise RuntimeError('Missing JNI export '+name)
 if 'AArch64' not in elf:raise RuntimeError('Unexpected native target')
 needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',elf)
 if not {'libEGL.so','libGLESv3.so'}.issubset(needed) or any(x not in {'libEGL.so','libGLESv3.so','libc.so','libdl.so','libm.so'} for x in needed):raise RuntimeError('Unexpected dependencies '+str(needed))
 align=[int(line.split()[-1],0) for line in elf.splitlines() if line.strip().startswith('LOAD ')]
 if not align or any(x<16384 for x in align):raise RuntimeError('ELF LOAD alignment below 16KB')
 (out/'native1960-readelf.txt').write_text(elf)
 tracked=[source/'engine1960.c',source/'build_native1960.py']+[source/(x+'.comp') for x in NAMES]+sorted(source.glob('residual_blocks1961.c'))
 report={'schema':1,'library':target.name,'abi':'arm64-v8a','ndkRevision':NDK_REVISION,'jniAbi':19601,'shaderPrograms':NAMES,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'size':target.stat().st_size,'sources':{str(x.relative_to(source)):hashlib.sha256(x.read_bytes()).hexdigest() for x in tracked},'runtimePhysicalDeviceVerified':False,'qualityAdmission':'Full pixel CPU reference plus actual complete route timing required by caller','precision':'Strong fp32 strict; Single requires actual fp64 extension; unsupported precision is CPU fallback','maxResidentBytes':512*1024*1024,'unknownFenceQuarantinesContext':True,'sourceFloatReduction':False,'javaRouteFingerprintIncluded':True,'qualityDependencyFingerprintIncluded':True,'includedCpuSourceSha256':hashlib.sha256((source.parent/'native1955/single_noise1955.c').read_bytes()).hexdigest(),'jniBatch':True,'asyncBanks':2,'batchPoolBytes':128*1024*1024,'stagingPoolBytes':64*1024*1024,'workgroupVariants':[64,32,128]}
 report['strongModePrograms']=[]
 for choice,local in enumerate([64,32,128]):
  for mode in range(4):
   radius=7 if mode==1 else 3 if mode==2 else 5
   shared=(8+2*radius)*(local//8+2*radius)*16
   if mode!=2:
    offsets=12 if mode==1 else 4;diff_radius=7 if mode==1 else 5
    shared+=offsets*(8+diff_radius+1)*(local//8+diff_radius+1)*4
   report['strongModePrograms'].append({'programId':27+mode+choice*4,'mode':mode,'localSize':local,'cacheRadius':radius,'tileWidth':8,'tileHeight':local//8,'sharedBytes':shared})
 report['directByteBufferUpload']=True
 report['reusablePrivateMultiOutputReadback']=True
 report['strongExactEarlyRejection']='Original fp32 normalized final predicate applied to nonnegative ordered partial distance; all contributing patches retain every original observation'
 (out/'native1960-build.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--ndk',required=True);p.add_argument('--out',required=True);a=p.parse_args();print(json.dumps(build(a.ndk,a.out),indent=2))
