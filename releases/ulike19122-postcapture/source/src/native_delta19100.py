#!/usr/bin/env python3
"""Bind the reviewed .100 native delta to exact current and published .99 ELFs.

Native C/shader changes are allowed only by path and reviewed SHA. Unchanged
native inputs, flags, classification and exports stay pinned. Current and frozen
baseline sources are built independently with the same pinned NDK; both GPU and
NR1955 complete payloads must match their corresponding exact binaries. Pixel
identity is a separate mandatory host/native regression gate, never inferred
from this structural proof.
"""
from pathlib import Path
import importlib.util,json,re,subprocess
from native_identity1995 import need,sha,sections
from native_identity1999 import inspect
ROOT=Path(__file__).resolve().parent
BASE_SHA256='e0076051bd88a258994a5bad93e181fc39515bb1d1a84ac3b4a8ee38186889da'
BASE_BYTES=164184
BASE_FINGERPRINT='5e992f15ae285ae7068c45dcdd24491d0a9065632955b9078aac675203ef821e'
NR_BASE_SHA256='37161e215476b9222e0763cfc74322fcd72b178d2bff931b93fbc42cfa86929c'
NR_BASE_BYTES=22544
BASELINE_FILE_SHA256='dbdbcd6e1a206348ca778a7f61143b1312db6748ffe227c63603acf72e02d4ca'
ADDED_EXPORTS=['Java_com_hiro_ulike_GpuNoise1960_programFailureNative19100']
ADDED_METHODS=['Lcom/hiro/ulike/GpuNoise1960;->programFailureNative19100(I)Ljava/lang/String;']
REFERENCE='tests19100/native/reference99'
# This set defines the reviewed optimization/diagnostics scope. The review file
# must then identify the exact nonempty subset and exact current source hashes.
ALLOWED_SOURCE_PATHS=frozenset(('native1955/single_noise1955.c','native1960/analysis1960.comp','native1960/strong1960.comp','native1960/engine1960.c'))
FLAGS=['-std=c11','-O3','-shared','-fPIC','-fvisibility=hidden','-fno-fast-math','-ffp-contract=off','-Wl,--no-undefined','-Wl,--build-id=sha1','-Wl,-z,max-page-size=16384','-Wl,-soname,libulike_gpu1960.so','-Wall','-Wextra','-Werror','-Wno-misleading-indentation']
NR_FLAGS=['-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off','-fno-fast-math','-fPIC','-fvisibility=hidden','-shared','-Wl,--no-undefined','-Wl,-z,max-page-size=16384']

def review(source=ROOT):
    value=json.loads((Path(source)/'native_delta19100_review.json').read_text())
    need(value.get('schema')=='ulike19100-native-review-v1','Native review schema')
    changed=value.get('reviewed_changed_source_sha256')
    need(isinstance(changed,dict) and changed and set(changed)<=ALLOWED_SOURCE_PATHS,'Explicit nonempty reviewed native delta required')
    need(all(re.fullmatch('[a-f0-9]{64}',v or '') for v in changed.values()),'Malformed reviewed source digest')
    for key,pattern in [('added_gpu_jni_exports',r'Java_com_hiro_ulike_[A-Za-z0-9_]+'),('added_runtime_jni_methods',r'Lcom/hiro/ulike/[A-Za-z0-9_$]+;->[A-Za-z0-9_]+\([^ ]*\)[^ ]+')]:
        items=value.get(key);need(isinstance(items,list) and items==sorted(set(items)) and all(re.fullmatch(pattern,x) for x in items),'Explicit reviewed JNI inventory required: '+key)
    need(value['added_gpu_jni_exports']==ADDED_EXPORTS and value['added_runtime_jni_methods']==ADDED_METHODS,'Added native method scope differs')
    return value

def verify_native_sources(source=ROOT):
    source=Path(source);baseline_bytes=(source/'native_delta19100_baseline.json').read_bytes()
    need(sha(baseline_bytes)==BASELINE_FILE_SHA256,'Frozen .99 source baseline inventory changed')
    pins=json.loads(baseline_bytes);approved=review(source)
    need(pins.get('schema')=='ulike19100-native-source-baseline-v1' and pins.get('baseline_release')=='ulike-v1.9.99','Native baseline schema/release')
    need(pins.get('gpu')==dict(sha256=BASE_SHA256,bytes=BASE_BYTES,fingerprint=BASE_FINGERPRINT),'Published GPU baseline pins changed')
    need(pins.get('nr')==dict(sha256=NR_BASE_SHA256,bytes=NR_BASE_BYTES),'Published NR baseline SHA changed')
    manifest=source/'native1960/compute_dependencies1990.json'
    need(sha(manifest.read_bytes())==pins['compute_manifest_sha256'],'Native dependency classification changed')
    entries=json.loads(manifest.read_text())['native_dependencies'];baseline=pins['native_source_sha256']
    need(set(entries)==set(baseline),'Native baseline source inventory differs')
    current={name:sha((source/name).read_bytes()) for name in entries}
    changed=sorted(name for name,digest in baseline.items() if current[name]!=digest)
    need(changed==sorted(approved['reviewed_changed_source_sha256']),'Native changed-path inventory differs from reviewed scope')
    for name,digest in baseline.items():
        need(sha((source/REFERENCE/name).read_bytes())==digest,'Frozen published .99 native input changed: '+name)
        if name in approved['reviewed_changed_source_sha256']:
            need(current[name]==approved['reviewed_changed_source_sha256'][name],'Current native source differs from review: '+name)
        else:need(current[name]==digest,'Unreviewed native input changed: '+name)
    return {'native_source_sha256':current,'baseline_native_source_sha256':baseline,
        'compute_manifest_sha256':pins['compute_manifest_sha256'],'changed_native_sources':changed,
        'reviewed_changed_source_sha256':approved['reviewed_changed_source_sha256'],
        'all_other_native_sources_identical_to_1999':True,
        'added_gpu_jni_exports':approved['added_gpu_jni_exports'],
        'added_runtime_jni_methods':approved['added_runtime_jni_methods']}

def current_fingerprint(source=ROOT):
    source=Path(source);path=source/'native1960/build_native1960.py'
    spec=importlib.util.spec_from_file_location('native_compute19100',path);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    return module.compute_fingerprint1990(source/'native1960')[0]

def generated_header(source,fingerprint):
    names=['strong1960','single1960','analysis1960','geometry1960','analysis1961','residual1961','protection1961','compare1961','finish1961']
    body='/* Generated verbatim from the tracked GX1960 compute sources. */\n'
    for name in names:
        data=(Path(source)/'native1960'/(name+'.comp')).read_bytes()
        body+='static const char '+name+'_source[]=\n'+''.join(json.dumps(line+'\n')+'\n' for line in data.decode().splitlines())+';\n'
    return body+'#define GPU1960_SOURCE_SHA256 "'+fingerprint+'"\n'

def verify(raw,fingerprint,report,source=ROOT,nr=None):
    source=Path(source);inspect(raw,fingerprint)
    need(report.get('schema')=='ulike19100-native-reviewed-source-delta-v1' and report.get('status')=='passed','Reviewed native source delta proof absent')
    need(report.get('current_sha256')==sha(raw) and report.get('current_bytes')==len(raw),'GPU native differs from rebuilt payload')
    need(report.get('compute_fingerprint')==fingerprint and report.get('baseline_compute_fingerprint')==BASE_FINGERPRINT,'Native fingerprint proof differs')
    need(report.get('baseline_sha256')==report.get('inverse_rebuild_sha256')==BASE_SHA256 and report.get('baseline_bytes')==BASE_BYTES,'Baseline GPU inverse proof differs')
    need(report.get('same_ndk_inverse_rebuild_verified') is True and report.get('same_ndk_forward_rebuild_verified') is True and report.get('ndk_revision')=='27.2.12479018','Pinned forward/inverse build proof absent')
    need(report.get('forward_rebuild_sha256')==sha(raw),'GPU forward build differs')
    need(report.get('compile_flags')==FLAGS and report.get('link_libraries')==['EGL','GLESv3','m'],'GPU build flags/libraries differ')
    expected=verify_native_sources(source)
    for key,value in expected.items():need(report.get(key)==value,'Native source proof differs: '+key)
    now=generated_header(source,fingerprint);old=generated_header(source/REFERENCE,BASE_FINGERPRINT)
    need(report.get('generated_header_sha256')==sha(now.encode()) and report.get('inverse_header_sha256')==sha(old.encode()),'Exact current/baseline generated shader header proof differs')
    need(isinstance(nr,bytes) and nr[:6]==b'\x7fELF\x02\x01' and nr[18:20]==b'\xb7\x00','Exact current AArch64 NR1955 required')
    proof=report.get('nr1955',{})
    need(proof.get('current_sha256')==proof.get('forward_rebuild_sha256')==sha(nr) and proof.get('current_bytes')==len(nr),'NR1955 forward/current proof differs')
    need(proof.get('baseline_sha256')==proof.get('inverse_rebuild_sha256')==NR_BASE_SHA256 and proof.get('baseline_bytes')==NR_BASE_BYTES,'NR1955 exact baseline inverse proof absent')
    need(proof.get('same_ndk_forward_rebuild_verified') is True and proof.get('same_ndk_inverse_rebuild_verified') is True,'NR1955 forward/inverse proof incomplete')
    need(proof.get('compile_flags')==NR_FLAGS and proof.get('link_libraries')==['m'] and proof.get('strip_flags')==['--strip-unneeded'],'NR1955 compiler/link/strip flags differ')
    need(report.get('pixel_equivalence_claimed_by_build') is False,'Structural rebuild cannot prove pixel equivalence')
    return report

def build_inverse(source,ndk,native_out,nr_out,work,baseline,nr_baseline):
    source,ndk,native_out,nr_out,work=map(Path,(source,ndk,native_out,nr_out,work))
    need(sha(baseline)==BASE_SHA256 and len(baseline)==BASE_BYTES,'Wrong .99 GPU native')
    need(sha(nr_baseline)==NR_BASE_SHA256 and len(nr_baseline)==NR_BASE_BYTES,'Wrong .99 NR1955 native')
    pins=verify_native_sources(source);fingerprint=current_fingerprint(source)
    current=(native_out/'libulike_gpu1960.so').read_bytes();nr=(nr_out/'libulike_nr1955.so').read_bytes();inspect(current,fingerprint)
    need(not work.exists(),'Fresh native proof build directory required');work.mkdir(parents=True)
    need(re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',(ndk/'source.properties').read_text(),re.M),'Pinned NDK r27c required')
    tool=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin';clang=tool/'aarch64-linux-android26-clang'
    header=(native_out/'shader_sources1960.h').read_text();need(header==generated_header(source,fingerprint),'GPU generated shader header differs')
    inverse_header=generated_header(source/REFERENCE,BASE_FINGERPRINT)
    def run(args,label):
        result=subprocess.run([str(x) for x in args],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        (work/(label+'.log')).write_text(result.stdout);need(result.returncode==0,label+' failed: '+result.stdout[-5000:])
    def compile_pair(tree,header,label):
        directory=work/label;directory.mkdir();(directory/'shader_sources1960.h').write_text(header)
        gpu=directory/'libulike_gpu1960.so';single=directory/'libulike_nr1955.so'
        run([clang,*FLAGS,'-I'+str(directory),tree/'native1960/engine1960.c',tree/'native1960/residual_blocks1961.c','-lEGL','-lGLESv3','-lm','-o',gpu],label+'-gpu')
        run([clang,*NR_FLAGS,tree/'native1955/single_noise1955.c','-lm','-o',single],label+'-nr1955')
        run([tool/'llvm-objcopy','--strip-unneeded',single],label+'-nr1955-strip')
        return gpu.read_bytes(),single.read_bytes()
    forward,nr_forward=compile_pair(source,header,'forward')
    need(forward==current and nr_forward==nr,'Current forward rebuild differs from packaged native payload')
    inverse,nr_inverse=compile_pair(source/REFERENCE,inverse_header,'inverse')
    need(inverse==baseline and nr_inverse==nr_baseline,'Frozen-source inverse rebuild differs from exact published .99 native payload')
    report={'schema':'ulike19100-native-reviewed-source-delta-v1','status':'passed',
        'current_sha256':sha(current),'current_bytes':len(current),'baseline_sha256':BASE_SHA256,'baseline_bytes':BASE_BYTES,
        'inverse_rebuild_sha256':sha(inverse),'forward_rebuild_sha256':sha(forward),
        'compute_fingerprint':fingerprint,'baseline_compute_fingerprint':BASE_FINGERPRINT,
        'same_ndk_inverse_rebuild_verified':True,'same_ndk_forward_rebuild_verified':True,'ndk_revision':'27.2.12479018',
        'generated_header_sha256':sha(header.encode()),'inverse_header_sha256':sha(inverse_header.encode()),
        'compile_flags':FLAGS,'link_libraries':['EGL','GLESv3','m'],'pixel_equivalence_claimed_by_build':False,
        'nr1955':{'current_sha256':sha(nr),'current_bytes':len(nr),'baseline_sha256':NR_BASE_SHA256,'baseline_bytes':len(nr_baseline),
          'forward_rebuild_sha256':sha(nr_forward),'inverse_rebuild_sha256':sha(nr_inverse),
          'same_ndk_forward_rebuild_verified':True,'same_ndk_inverse_rebuild_verified':True,
          'compile_flags':NR_FLAGS,'link_libraries':['m'],'strip_flags':['--strip-unneeded']},**pins}
    verify(current,fingerprint,report,source,nr)
    (work/'result.json').write_text(json.dumps(report,sort_keys=True,indent=2)+'\n');return report
