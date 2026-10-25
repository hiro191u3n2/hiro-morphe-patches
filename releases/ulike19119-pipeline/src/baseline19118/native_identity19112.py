#!/usr/bin/env python3
"""Prove unchanged native algorithms with the reviewed Java-route identity update.

The .112 Java planning/cancellation changes alter the conservative compute
fingerprint embedded in the ELF. Replacing only that fingerprint with the exact
published .111 value must reproduce the complete published .111 ELF. Retain the
separate .107-to-.106 regional inverse proof as historical arithmetic evidence.
"""
from pathlib import Path
import copy, json, re, subprocess
import native_identity19101 as legacy
import native_dependency19111
from native_identity1999 import inspect, generated_header
from native_identity1995 import need, sha

BASE_SHA256='ae03a83aa09970ef2ce4d22370ff21a8730cfaff25bd51bb02a53ebb15a9c426'
BASE_BYTES=182464
BASE_FINGERPRINT='cdc1de7aaa41fd99ad767792beff37b072b1eba9b7dd4bb5666635d4ff5ed065'
SCHEMA='ulike19112-native-identity-only-v1'


def expected(raw,source):
    source=Path(source)
    fingerprint=legacy.current_fingerprint(source)
    inspect(raw,fingerprint)
    need(fingerprint!=BASE_FINGERPRINT,'Reviewed .112 Java route identity update absent')
    proof=legacy.verify_native_sources(source)
    return dict(schema=SCHEMA,status='passed',baseline_release='ulike-v1.9.111',
        baseline_sha256=BASE_SHA256,baseline_bytes=BASE_BYTES,
        baseline_compute_fingerprint=BASE_FINGERPRINT,
        current_sha256=sha(raw),current_bytes=len(raw),compute_fingerprint=fingerprint,
        native_algorithms_unchanged_from_19111=True,
        byte_identical_to_baseline=False,
        same_ndk_forward_rebuild_verified=True,forward_rebuild_sha256=sha(raw),
        same_ndk_identity_inverse_verified=True,identity_inverse_sha256=BASE_SHA256,
        identity_inverse_bytes=BASE_BYTES,
        ndk_revision=legacy.NDK_REVISION,compile_flags=list(legacy.FLAGS),link_libraries=['EGL','GLESv3','m'],
        generated_header_sha256=sha(generated_header(source,fingerprint).encode()),
        identity_inverse_header_sha256=sha(generated_header(source,BASE_FINGERPRINT).encode()),
        native_source_sha256=proof['native_source_sha256'],
        numerical_dependency_classification_unchanged=True,
        reviewed_diagnostic_exclusion19111=native_dependency19111.DELTA,
        diagnostic_manifest_inverse_sha256=proof['baseline_compute_manifest_sha256'],
        inherited_regional_inverse_preserved=True)


def verify(raw,report,source,nr=None):
    value=expected(raw,source)
    need(type(report) is dict and legacy.exact_equal(report,value),'Exact .112 identity-only native proof differs')
    need(sha(raw)!=BASE_SHA256,'Current fingerprint must not reuse the old published ELF')
    if nr is not None:legacy.verify_nr(nr)
    return report


def build(source,ndk,native_out,work,baseline,nr):
    source,ndk,native_out,work=map(Path,(source,ndk,native_out,work))
    need(type(baseline) is bytes and len(baseline)==BASE_BYTES and sha(baseline)==BASE_SHA256,'Wrong published .111 native payload')
    inspect(baseline,BASE_FINGERPRINT);legacy.verify_nr(nr)
    need(not work.exists(),'Fresh .112 native identity work directory required');work.mkdir(parents=True)
    need(re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',(ndk/'source.properties').read_text(),re.M),'Pinned NDK r27c required')
    current=(native_out/'libulike_gpu1960.so').read_bytes()
    fingerprint=legacy.current_fingerprint(source)
    need((native_out/'shader_sources1960.h').read_text()==generated_header(source,fingerprint),'Current shader header differs')
    clang=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang'
    def compile_header(label,header,engine):
        directory=work/label;directory.mkdir()
        (directory/'shader_sources1960.h').write_text(header)
        path=directory/'engine1960.c';path.write_text(engine)
        target=directory/'libulike_gpu1960.so'
        command=[str(v) for v in [clang,*legacy.FLAGS,'-I'+str(directory),path,source/'native1960/residual_blocks1961.c','-lEGL','-lGLESv3','-lm','-o',target]]
        result=subprocess.run(command,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        (work/(label+'.log')).write_text(result.stdout)
        need(result.returncode==0,'Native identity rebuild failed: '+result.stdout[-4000:])
        return target.read_bytes()
    inverse111=compile_header('identity-inverse111',generated_header(source,BASE_FINGERPRINT),(source/'native1960/engine1960.c').read_text())
    need(inverse111==baseline,'Identity-only inverse does not equal the complete published .111 ELF')
    # The frozen .106 hash is independently pinned in the unchanged legacy proof.
    # Reconstruct it to keep all retained legacy proof and corruption tests live.
    pins=legacy.baseline(source)
    inverse106=compile_header('historical-baseline106',legacy.inverse_header(source,pins),pins['restored_source_utf8']['native1960/engine1960.c'])
    need(len(inverse106)==legacy.BASE_BYTES and sha(inverse106)==legacy.BASE_SHA256,'Historical regional inverse differs from pinned .106 ELF')
    historical=legacy.build_inverse(source,ndk,native_out,work/'historical-regional-inverse',inverse106,nr)
    identity=expected(current,source);verify(current,identity,source,nr)
    result={'historical_regional_inverse':historical,'previous_release_identity':identity}
    (work/'result.json').write_text(json.dumps(result,sort_keys=True,indent=2)+'\n')
    return result


def negative_controls(source,raw,nr,report):
    verify(raw,report,source,nr);assertions=1
    source=Path(source)
    assertions+=native_dependency19111.negative_controls((source/legacy.MANIFEST_PATH).read_bytes(),
        legacy.baseline(source)['compute_manifest_utf8'].encode())
    def reject(candidate,body=raw,noise=nr):
        nonlocal assertions
        try:verify(body,candidate,source,noise)
        except (AssertionError,RuntimeError,ValueError):assertions+=1
        else:raise AssertionError('Malformed .112 identity-only proof accepted')
    for key in report:
        row=copy.deepcopy(report);row.pop(key);reject(row)
        row=copy.deepcopy(report);row[key]=None;reject(row)
        if type(report[key]) is bool:
            row=copy.deepcopy(report);row[key]=int(report[key]);reject(row)
        elif type(report[key]) is int:
            row=copy.deepcopy(report);row[key]=True;reject(row)
    row=copy.deepcopy(report);row['unexpected']=True;reject(row)
    reject(report,raw[:-1]);reject(report,noise=nr[:-1])
    return assertions
