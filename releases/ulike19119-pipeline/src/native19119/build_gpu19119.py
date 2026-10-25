#!/usr/bin/env python3
"""Build the independent .119 engine; published .118 binaries are never rebuilt here."""
import argparse,hashlib,json,pathlib,re,subprocess
NDK_REVISION='27.2.12479018'
NAMES=['strong1960','single1960','analysis1960','geometry1960','analysis1961','residual1961','protection1961','compare1961','finish1961']
def run(args):
 p=subprocess.run([str(x) for x in args],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 if p.returncode:raise RuntimeError(p.stdout)
 return p.stdout
def fingerprint(root):
 manifest=json.loads((root/'native1960/compute_dependencies1990.json').read_text())
 names=set(manifest['native_dependencies']+manifest['numerical_java'])
 # Routing, proof admission and both new arithmetic backends bind this optional
 # identity. Diagnostics may conservatively invalidate this candidate only.
 names.update(p.name for p in root.glob('*.java') if not p.name.endswith('Test.java') and re.search(r'^package com\.hiro\.ulike;',p.read_text(),re.M))
 names.update(p.relative_to(root).as_posix() for p in (root/'native19119').iterdir() if p.is_file() and p.suffix in ('.c','.h','.comp','.py'))
 pins={};d=hashlib.sha256(b'ulike19119-independent-gpu-candidate-v1\0')
 for name in sorted(names):
  path=root/name
  if not path.is_file() or path.is_symlink():raise RuntimeError('Missing candidate dependency '+name)
  data=path.read_bytes();pins[name]=hashlib.sha256(data).hexdigest();d.update(name.encode()+b'\0'+str(len(data)).encode()+b'\0'+data)
 return d.hexdigest(),pins
def build(ndk,out):
 root=pathlib.Path(__file__).resolve().parent.parent;native=root/'native19119';ndk=pathlib.Path(ndk).resolve();out=pathlib.Path(out).resolve();out.mkdir(parents=True,exist_ok=True)
 if not re.search(r'^Pkg.Revision\s*=\s*'+re.escape(NDK_REVISION)+r'\s*$',(ndk/'source.properties').read_text(),re.M):raise RuntimeError('Pinned NDK r27c required')
 identity,pins=fingerprint(root);header='/* Independent .119 candidate sources and identity. */\n'
 for name in NAMES:
  path=native/'strong19119.comp' if name=='strong1960' else root/'native1960'/(name+'.comp')
  header+='static const char '+name+'_source[]=\n'+''.join(json.dumps(line+'\n')+'\n' for line in path.read_text().splitlines())+';\n'
 header+='#define GPU1960_SOURCE_SHA256 "'+identity+'"\n';(out/'shader_sources19119.h').write_text(header)
 tool=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin';target=out/'libulike_gpu19119.so'
 flags=['-std=c11','-O3','-shared','-fPIC','-fvisibility=hidden','-fno-fast-math','-ffp-contract=off','-Wl,--no-undefined','-Wl,--build-id=sha1','-Wl,-z,max-page-size=16384','-Wl,-soname,libulike_gpu19119.so','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-I'+str(out)]
 run([tool/'aarch64-linux-android26-clang',*flags,native/'engine19119.c','-lEGL','-lGLESv3','-lm','-o',target])
 elf=run([tool/'llvm-readelf','-h','-d','-Ws','--program-headers','--wide',target]);(out/'native19119-readelf.txt').write_text(elf)
 required={'Java_com_hiro_ulike_GpuNoiseOptimized19119_'+name for name in re.findall(r'private static native [\w\[\]]+ (\w+)\(', (root/'GpuNoiseOptimized19119.java').read_text())}
 exports=set(re.findall(r'\bJava_[A-Za-z0-9_]+',elf))
 if not required.issubset(exports):raise RuntimeError('Missing independent JNI exports '+repr(sorted(required-exports)))
 if any(not name.startswith('Java_com_hiro_ulike_GpuNoiseOptimized19119_raw') for name in exports):raise RuntimeError('Candidate shadows legacy JNI')
 needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',elf);align=[int(l.split()[-1],0) for l in elf.splitlines() if l.strip().startswith('LOAD ')]
 if 'AArch64' not in elf or not align or min(align)<16384 or not {'libEGL.so','libGLESv3.so'}.issubset(needed) or not set(needed)<={'libc.so','libm.so','libdl.so','libEGL.so','libGLESv3.so'}:raise RuntimeError('Invalid Android native contract')
 report={'schema':'ulike19119-independent-gpu-build-v1','library':target.name,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'bytes':target.stat().st_size,'computeFingerprint':identity,'computeDependencySha256':pins,'jniExports':sorted(exports),'ndkRevision':NDK_REVISION,'abi':'arm64-v8a','candidateProgram':48,'candidateDefines':{'GX_QUOTIENT19119':1,'GX_DIV_FAST19115':1},'physicalAndroidTested':False,'admission':'independent device ARGB+confidence corpus and full-transfer speed comparison against unchanged tile8; otherwise immutable legacy engine','legacyCertificateReuseForCandidate':False}
 (out/'native19119-build.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--ndk',required=True);p.add_argument('--out',required=True);a=p.parse_args();print(json.dumps(build(a.ndk,a.out),indent=2))
