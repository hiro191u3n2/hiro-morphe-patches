#!/usr/bin/env python3
"""Prove the .97 GPU rebuild changes only source-identity inputs over exact .96.

The linker may move pooled strings and their instruction references when the
fingerprint literal changes. No ELF byte ranges are exempted. Rebuilding the
exact current sources and flags with only that generated literal restored must
recover the complete published .96 ELF byte for byte.
"""
from pathlib import Path
import importlib.util,json,re,subprocess
from native_identity1995 import need,sha,sections
ROOT=Path(__file__).resolve().parent
BASE_SHA256='a2a85739e867e3a72b30dc71a1557261c0c8553736ca0b19894e11044c46794e'
BASE_BYTES=164184
BASE_FINGERPRINT='cbb6aaf180b5f009eee5be81ea52b840ca8fbeabf3dcffeff2fc18649cc62f63'
FLAGS=['-std=c11','-O3','-shared','-fPIC','-fvisibility=hidden','-fno-fast-math','-ffp-contract=off','-Wl,--no-undefined','-Wl,--build-id=sha1','-Wl,-z,max-page-size=16384','-Wl,-soname,libulike_gpu1960.so','-Wall','-Wextra','-Werror','-Wno-misleading-indentation']

def inspect(raw,fingerprint):
    need(isinstance(raw,bytes),'Native payload must be bytes')
    need(re.fullmatch('[a-f0-9]{64}',fingerprint or '') is not None,'Invalid expected compute fingerprint')
    table=sections(raw);need('.rodata' in table and '.text' in table,'Native sections missing')
    row=table['.rodata'];begin,size=row[4:6];data=raw[begin:begin+size]
    matches=list(re.finditer(rb'(?<![a-f0-9])[a-f0-9]{64}(?![a-f0-9])',data))
    need(len(matches)==1 and matches[0].group()==fingerprint.encode(),'Unique source-derived fingerprint required')
    pos=begin+matches[0].start();need(raw[pos-1:pos]==b'\0' and raw[pos+64:pos+65]==b'\0','Whole fingerprint literal required')
    need(raw.count(fingerprint.encode())==1,'Duplicated fingerprint')

def generated_header(source,fingerprint):
    names=['strong1960','single1960','analysis1960','geometry1960','analysis1961','residual1961','protection1961','compare1961','finish1961']
    body='/* Generated verbatim from the tracked GX1960 compute sources. */\n'
    for name in names:
        data=(Path(source)/'native1960'/(name+'.comp')).read_bytes()
        body+='static const char '+name+'_source[]=\n'+''.join(json.dumps(line+'\n')+'\n' for line in data.decode().splitlines())+';\n'
    return body+'#define GPU1960_SOURCE_SHA256 "'+fingerprint+'"\n'

def verify(raw,fingerprint,report,source=ROOT):
    inspect(raw,fingerprint)
    need(report.get('schema')=='ulike1997-native-fingerprint-inverse-build-v1' and report.get('status')=='passed','Native inverse proof absent')
    need(report.get('current_sha256')==sha(raw) and report.get('current_bytes')==len(raw),'Native differs from exact rebuilt current payload')
    need(report.get('compute_fingerprint')==fingerprint and report.get('baseline_compute_fingerprint')==BASE_FINGERPRINT,'Native fingerprint proof differs')
    need(report.get('baseline_sha256')==report.get('inverse_rebuild_sha256')==BASE_SHA256 and report.get('baseline_bytes')==BASE_BYTES,'Baseline inverse proof differs')
    need(report.get('same_ndk_inverse_rebuild_verified') is True and report.get('ndk_revision')=='27.2.12479018','Pinned native inverse build missing')
    need(report.get('same_ndk_forward_rebuild_verified') is True and report.get('forward_rebuild_sha256')==sha(raw),'Pinned current native forward rebuild missing')
    need(report.get('generated_header_delta')==['GPU1960_SOURCE_SHA256'],'Unreviewed generated header delta')
    need(report.get('compile_flags')==FLAGS and report.get('link_libraries')==['EGL','GLESv3','m'],'Native inverse compiler/link flags differ')
    current=generated_header(source,fingerprint);inverse=generated_header(source,BASE_FINGERPRINT)
    need(report.get('generated_header_sha256')==sha(current.encode()) and report.get('inverse_header_sha256')==sha(inverse.encode()),'Generated shader header proof differs')
    for k,v in verify_native_sources(source).items():need(report.get(k)==v,'Native source proof differs: '+k)
    return report

def build_inverse(source,ndk,native_out,work,baseline):
    source,ndk,native_out,work=map(Path,(source,ndk,native_out,work))
    need(sha(baseline)==BASE_SHA256 and len(baseline)==BASE_BYTES,'Wrong .96 published native')
    pins=verify_native_sources(source);fingerprint=current_fingerprint(source)
    current=(native_out/'libulike_gpu1960.so').read_bytes();inspect(current,fingerprint)
    need(not work.exists(),'Fresh inverse native build directory required');work.mkdir(parents=True)
    header=(native_out/'shader_sources1960.h').read_text()
    need(header==generated_header(source,fingerprint),'Exact generated shader header required')
    old_literal='#define GPU1960_SOURCE_SHA256 "'+fingerprint+'"'
    new_literal='#define GPU1960_SOURCE_SHA256 "'+BASE_FINGERPRINT+'"'
    need(header.count(old_literal)==1 and header.count(fingerprint)==1,'Only one complete generated fingerprint literal may change')
    inverse=header.replace(old_literal,new_literal,1)
    need(inverse==generated_header(source,BASE_FINGERPRINT),'Shader header changed beyond fingerprint')
    (work/'shader_sources1960.h').write_text(inverse)
    need(re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',(ndk/'source.properties').read_text(),re.M),'Pinned NDK r27c required')
    clang=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang'
    def compile_header(directory,body,label):
        directory.mkdir(parents=True,exist_ok=True)
        (directory/'shader_sources1960.h').write_text(body)
        target=directory/'libulike_gpu1960.so'
        command=[str(x) for x in [clang,*FLAGS,'-I'+str(directory),source/'native1960/engine1960.c',source/'native1960/residual_blocks1961.c','-lEGL','-lGLESv3','-lm','-o',target]]
        process=subprocess.run(command,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        (work/(label+'-build.log')).write_text(process.stdout);need(process.returncode==0,label+' native build failed: '+process.stdout[-4000:])
        return target.read_bytes()
    forward=compile_header(work/'forward',header,'forward')
    need(forward==current,'Same-source/flags forward rebuild differs from packaged .97 ELF')
    restored=compile_header(work/'inverse',inverse,'inverse')
    need(restored==baseline,'Rebuilt inverse native differs from exact published .96 ELF')
    report={'schema':'ulike1997-native-fingerprint-inverse-build-v1','status':'passed','current_sha256':sha(current),'current_bytes':len(current),
        'baseline_sha256':BASE_SHA256,'baseline_bytes':BASE_BYTES,'inverse_rebuild_sha256':sha(restored),
        'compute_fingerprint':fingerprint,'baseline_compute_fingerprint':BASE_FINGERPRINT,
        'same_ndk_forward_rebuild_verified':True,'forward_rebuild_sha256':sha(forward),
        'same_ndk_inverse_rebuild_verified':True,'ndk_revision':'27.2.12479018','generated_header_delta':['GPU1960_SOURCE_SHA256'],
        'generated_header_sha256':sha(header.encode()),'inverse_header_sha256':sha(inverse.encode()),
        'compile_flags':FLAGS,'link_libraries':['EGL','GLESv3','m'],**pins}
    verify(current,fingerprint,report,source)
    (work/'result.json').write_text(json.dumps(report,sort_keys=True,indent=2)+'\n');return report

def current_fingerprint(source=ROOT):
    source=Path(source);path=source/'native1960/build_native1960.py'
    spec=importlib.util.spec_from_file_location('native_identity_compute1997',path)
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    return module.compute_fingerprint1990(source/'native1960')[0]

def verify_native_sources(source=ROOT):
    source=Path(source);pins=json.loads((source/'native_identity1997_baseline.json').read_text())
    need(pins.get('schema')=='ulike1997-native-source-baseline-v1','Native source baseline schema')
    manifest=source/'native1960/compute_dependencies1990.json'
    need(sha(manifest.read_bytes())==pins['compute_manifest_sha256'],'Native dependency classification changed')
    entries=json.loads(manifest.read_text())['native_dependencies']
    need(set(entries)==set(pins['native_source_sha256']),'Native baseline source inventory differs')
    for name,digest in pins['native_source_sha256'].items():
        need(sha((source/name).read_bytes())==digest,'Native/shader/CPU source changed from .96: '+name)
    return {'native_source_sha256':pins['native_source_sha256'],'compute_manifest_sha256':pins['compute_manifest_sha256'],
            'native_shader_and_cpu_sources_identical_to_1996':True}
