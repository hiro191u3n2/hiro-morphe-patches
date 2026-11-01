#!/usr/bin/env python3
"""Prove the .96 native delta is exactly the reviewed Strong shader + identity.

The C engine, compiler flags and other eight embedded programs are fixed to .95.
Replacing the one generated shader literal and route fingerprint with the frozen
.95 values and rebuilding with the same NDK must recover the exact published ELF.
No byte mask or arbitrary section exclusion can hide a native implementation change.
"""
from pathlib import Path
import importlib.util,json,re,subprocess
from native_identity1995 import need,sha,sections,current_fingerprint
ROOT=Path(__file__).resolve().parent
BASE_SHA256='def8a5bb9e1b137a135548718b90e36b71e29036aa262ebb9e5f20d6ebfe3bb3'
BASE_BYTES=163272
BASE_FINGERPRINT='029d8e0ba6fcaea69073a8fe28700002ca12242982966406151ad14ef11aa3e3'
SHADER='native1960/strong1960.comp'
REFERENCE='tests1996/shader/reference95/strong1960.comp'

def verify_native_sources(source=ROOT):
    source=Path(source);pins=json.loads((source/'native_delta1996_baseline.json').read_text())
    need(pins.get('schema')=='ulike1996-native-source-baseline-v1','Native source baseline schema')
    manifest=source/'native1960/compute_dependencies1990.json'
    need(sha(manifest.read_bytes())==pins['compute_manifest_sha256'],'Native dependency classification changed')
    entries=json.loads(manifest.read_text())['native_dependencies']
    need(set(entries)==set(pins['native_source_sha256']),'Native source inventory differs')
    need(pins.get('allowed_changed_native_source')==SHADER,'Exact reviewed shader path required')
    need(sha((source/REFERENCE).read_bytes())==pins['native_source_sha256'][SHADER],'Frozen .95 shader changed')
    current={name:sha((source/name).read_bytes()) for name in entries}
    changed=[name for name,digest in pins['native_source_sha256'].items() if current[name]!=digest]
    need(changed==[SHADER],'Native delta must contain only the reviewed Strong shader')
    need(re.fullmatch('[a-f0-9]{64}',pins.get('reviewed_current_shader_sha256','')) is not None,'Unreviewed current shader')
    need(current[SHADER]==pins['reviewed_current_shader_sha256'],'Current shader differs from reviewed source')
    return {'native_source_sha256':current,'baseline_native_source_sha256':pins['native_source_sha256'],
        'compute_manifest_sha256':pins['compute_manifest_sha256'],'changed_native_sources':[SHADER],
        'all_other_native_shader_and_cpu_sources_identical_to_1995':True,
        'frozen_baseline_shader_sha256':pins['native_source_sha256'][SHADER]}

def shader_literal(raw):
    return 'static const char strong1960_source[]=\n'+''.join(json.dumps(line+'\n')+'\n' for line in raw.decode().splitlines())+';\n'

def inspect(raw,fingerprint):
    need(isinstance(raw,bytes),'Native payload must be bytes')
    need(re.fullmatch('[a-f0-9]{64}',fingerprint or '') is not None,'Expected current fingerprint')
    table=sections(raw);need('.rodata' in table and '.text' in table,'Native sections missing')
    row=table['.rodata'];begin,size=row[4:6];data=raw[begin:begin+size]
    matches=list(re.finditer(rb'(?<![a-f0-9])[a-f0-9]{64}(?![a-f0-9])',data))
    need(len(matches)==1 and matches[0].group()==fingerprint.encode(),'Unique source-derived fingerprint required')
    pos=begin+matches[0].start();need(raw[pos-1:pos]==b'\0' and raw[pos+64:pos+65]==b'\0','Whole fingerprint literal required')
    need(raw.count(fingerprint.encode())==1,'Duplicated fingerprint')

def verify(raw,fingerprint,report,source=ROOT):
    inspect(raw,fingerprint)
    need(report.get('schema')=='ulike1996-native-reviewed-shader-delta-v1' and report.get('status')=='passed','Native delta proof absent')
    need(report.get('current_sha256')==sha(raw) and report.get('current_bytes')==len(raw),'Native differs from exact rebuilt current payload')
    need(report.get('compute_fingerprint')==fingerprint and report.get('baseline_compute_fingerprint')==BASE_FINGERPRINT,'Native fingerprint proof differs')
    need(report.get('baseline_sha256')==report.get('inverse_rebuild_sha256')==BASE_SHA256 and report.get('baseline_bytes')==BASE_BYTES,'Baseline inverse proof differs')
    need(report.get('same_ndk_inverse_rebuild_verified') is True and report.get('ndk_revision')=='27.2.12479018','Pinned native inverse build missing')
    need(report.get('generated_header_delta')==['strong1960_source','GPU1960_SOURCE_SHA256'],'Unreviewed generated header delta')
    expected=verify_native_sources(source)
    for k,v in expected.items():need(report.get(k)==v,'Native source proof differs: '+k)
    return report

def build_inverse(source,ndk,native_out,work,baseline):
    source,ndk,native_out,work=map(Path,(source,ndk,native_out,work))
    need(sha(baseline)==BASE_SHA256 and len(baseline)==BASE_BYTES,'Wrong .95 published native')
    pins=verify_native_sources(source);fingerprint=current_fingerprint(source)
    current=(native_out/'libulike_gpu1960.so').read_bytes();inspect(current,fingerprint)
    need(not work.exists(),'Fresh inverse native build directory required');work.mkdir(parents=True)
    header=(native_out/'shader_sources1960.h').read_text();current_literal=shader_literal((source/SHADER).read_bytes());reference_literal=shader_literal((source/REFERENCE).read_bytes())
    need(header.count(current_literal)==1 and header.count(fingerprint)==1,'Exact generated literal and identity required')
    inverse=header.replace(current_literal,reference_literal,1).replace(fingerprint,BASE_FINGERPRINT,1)
    (work/'shader_sources1960.h').write_text(inverse)
    need(re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',(ndk/'source.properties').read_text(),re.M),'Pinned NDK r27c required')
    flags=['-std=c11','-O3','-shared','-fPIC','-fvisibility=hidden','-fno-fast-math','-ffp-contract=off','-Wl,--no-undefined','-Wl,--build-id=sha1','-Wl,-z,max-page-size=16384','-Wl,-soname,libulike_gpu1960.so','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-I'+str(work)]
    target=work/'libulike_gpu1960.so';clang=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang'
    command=[str(x) for x in [clang,*flags,source/'native1960/engine1960.c',source/'native1960/residual_blocks1961.c','-lEGL','-lGLESv3','-lm','-o',target]]
    process=subprocess.run(command,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    (work/'inverse-build.log').write_text(process.stdout);need(process.returncode==0,'Inverse native build failed: '+process.stdout[-4000:])
    restored=target.read_bytes();need(restored==baseline,'Rebuilt inverse native differs from exact .95 ELF')
    report={'schema':'ulike1996-native-reviewed-shader-delta-v1','status':'passed','current_sha256':sha(current),'current_bytes':len(current),
        'baseline_sha256':BASE_SHA256,'baseline_bytes':BASE_BYTES,'inverse_rebuild_sha256':sha(restored),
        'compute_fingerprint':fingerprint,'baseline_compute_fingerprint':BASE_FINGERPRINT,
        'same_ndk_inverse_rebuild_verified':True,'ndk_revision':'27.2.12479018',
        'generated_header_delta':['strong1960_source','GPU1960_SOURCE_SHA256'],**pins}
    verify(current,fingerprint,report,source)
    (work/'result.json').write_text(json.dumps(report,sort_keys=True,indent=2)+'\n');return report
