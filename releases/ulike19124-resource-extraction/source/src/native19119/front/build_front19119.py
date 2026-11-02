#!/usr/bin/env python3
"""Build the separately identified, optional exact protected-block NR backend."""
import argparse, hashlib, json, re, subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parent
SRC=ROOT.parent.parent
FLAGS=['-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off','-fno-fast-math',
       '-fPIC','-fvisibility=hidden','-shared','-Wl,--no-undefined','-Wl,-z,max-page-size=16384',
       '-Wl,-soname,libulike_front19119.so']

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def fingerprint():
    names=['native19119/front/front19119.c','native19119/front/front_core19119.c',
           'native19119/front/build_front19119.py','CpuFrontOptimized19119.java','CpuSingle1978.java',
           'SingleNoise1955.java','ColourCache1976.java','GpuQualification1961.java']
    pins={n:sha(SRC/n) for n in names}
    raw=json.dumps({'schema':'front19119-candidate-v1','sources':pins,'flags':FLAGS},sort_keys=True,separators=(',',':')).encode()
    return hashlib.sha256(raw).hexdigest(),pins

def build(ndk,output):
    ndk=Path(ndk).resolve();output=Path(output).resolve();output.mkdir(parents=True,exist_ok=True)
    if not re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',(ndk/'source.properties').read_text(),re.M):raise RuntimeError('Pinned NDK r27c required')
    identity,pins=fingerprint();(output/'front_identity19119.h').write_text('#define FRONT119_SOURCE_SHA256 "'+identity+'"\n')
    bin=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin';library=output/'libulike_front19119.so'
    subprocess.run([str(bin/'aarch64-linux-android26-clang'),*FLAGS,'-I'+str(output),str(ROOT/'front19119.c'),'-lm','-o',str(library)],check=True)
    subprocess.run([str(bin/'llvm-objcopy'),'--strip-unneeded',str(library)],check=True)
    elf=subprocess.check_output([str(bin/'llvm-readelf'),'-h','-d','-Ws','--program-headers','--wide',str(library)],text=True)
    exports=set(re.findall(r'\bJava_[A-Za-z0-9_]+',elf))
    expected={'Java_com_hiro_ulike_CpuFrontOptimized19119_'+n for n in ('nativeAbi','nativeFingerprint','processNative')}
    if exports!=expected:raise RuntimeError('Candidate JNI exports differ: '+repr(exports))
    align=[int(l.split()[-1],0) for l in elf.splitlines() if l.strip().startswith('LOAD ')]
    needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',elf)
    if 'AArch64' not in elf or not align or min(align)<16384 or not set(needed)<={'libc.so','libm.so','libdl.so'}:raise RuntimeError('Candidate ELF contract')
    result={'schema':'front19119-native-build-v1','library':library.name,'sha256':sha(library),'bytes':library.stat().st_size,
      'implementation_sha256':identity,'source_sha256':pins,'flags':FLAGS,'jni_exports':sorted(exports),'segment_alignment':align,
      'dependencies':needed,'baseline_modified':False,'device_tested':False,'admission':'independent exact output twice and candidate worst <= baseline best minus 5 percent'}
    (output/'front19119-build.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result));return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--ndk',required=True);p.add_argument('--output',required=True);a=p.parse_args();build(a.ndk,a.output)
