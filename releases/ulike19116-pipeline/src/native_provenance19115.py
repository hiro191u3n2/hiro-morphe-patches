#!/usr/bin/env python3
"""Bind the reviewed .115 GPU/CPU deltas to complete rebuilt published114 binaries.

Numerical equivalence and physical-device performance are separate evidence.
"""
from pathlib import Path
import copy, hashlib, importlib.util, json, re, subprocess, tempfile, shutil
from native_identity1995 import need, sha, sections
from native_identity1999 import generated_header, inspect
from native_identity19101 import exact_equal, FLAGS, NDK_REVISION
import source_inverse19115
BASELINE_FILE='native_provenance19115_baseline.json'
BASELINE_SHA256='f399a31d7575bda875b4b98008ddd8b80cd86afba4207bdbebb03e035882ac40'
REFERENCE='tests19115/provenance/reference114'
CHANGED=('native1955/single_noise1955.c','native1958/neon1959.h','native1960/engine1960.c','native1960/strong1960.comp')
JAVA_CHANGED=('GpuNoise1960.java','GpuQualification1961.java','GpuResident1976.java','GpuStrong1960.java','GpuStrongDefault1990.java','StrongNoise1958.java')
JAVA_ADDED=('GpuRegionalDefault19115.java',)
SCHEMA='ulike19115-native-source-and-rebuild-v1'
NR_FLAGS=['-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off','-fno-fast-math','-fPIC','-fvisibility=hidden','-shared','-Wl,--no-undefined','-Wl,-z,max-page-size=16384']
def baseline(source):
    source=Path(source);raw=(source/BASELINE_FILE).read_bytes()
    need(sha(raw)==BASELINE_SHA256,'Pinned114 complete native baseline differs')
    value=json.loads(raw);need(value['schema']=='ulike19115-native-baseline114-v1' and value['baseline_release']=='ulike-v1.9.114','Wrong native baseline')
    for name,h in value['reference_sources'].items():
        p=source/REFERENCE/name;need(p.is_file() and not p.is_symlink() and sha(p.read_bytes())==h,'Frozen114 dependency differs: '+name)
    return value

def restored_native(source,name):
    need(name in CHANGED,'Only reviewed native files may be restored')
    return source_inverse19115.restore(source,name)

def scope(source):
    source=Path(source);pins=baseline(source);old=json.loads((source/REFERENCE/'native1960/compute_dependencies1990.json').read_text())
    raw=(source/'native1960/compute_dependencies1990.json').read_bytes();current=json.loads(raw)
    restored=source_inverse19115.restore(source,'native1960/compute_dependencies1990.json',raw)
    need(sha(restored)==pins['compute_dependencies_sha256'],'Complete dependency manifest inverse differs')
    expected=copy.deepcopy(old);expected['numerical_java']=sorted(expected['numerical_java']+list(JAVA_ADDED))
    need(current==expected,'Only the reviewed new regional helper may enter compute dependencies')
    actual_native={};changed=[]
    for name,h in pins['native_sources'].items():
        data=(source/name).read_bytes();actual_native[name]=sha(data)
        if actual_native[name]!=h:
            need(name in CHANGED,'Unreviewed native dependency differs: '+name)
            need(sha(restored_native(source,name))==h,'Native inverse differs: '+name);changed.append(name)
    need(tuple(sorted(changed))==CHANGED,'All four reviewed GPU/CPU native deltas are required')
    actual_java={};java_changed=[]
    for name,h in pins['numerical_java_sources'].items():
        data=(source/name).read_bytes();actual_java[name]=sha(data)
        if actual_java[name]!=h:
            need(name in JAVA_CHANGED,'Unreviewed numerical Java differs: '+name)
            need(sha(source_inverse19115.restore(source,name,data))==h,'Java inverse differs: '+name);java_changed.append(name)
    need(tuple(sorted(java_changed))==JAVA_CHANGED,'Exact reviewed GPU routing/transport and CPU observation deltas required')
    for name in JAVA_ADDED:
        p=source/name;need(p.is_file() and not p.is_symlink(),'Missing regional helper');actual_java[name]=sha(p.read_bytes())
    return dict(native_source_sha256=actual_native,changed_native_sources=sorted(changed),numerical_java_source_sha256=actual_java,changed_numerical_java_sources=sorted(java_changed),added_numerical_java_sources=list(JAVA_ADDED),restored_baseline_compute_fingerprint=pins['fingerprint'],native_dependency_classification_unchanged=True,numerical_dependency_addition_reviewed=True,baseline_manifest_sha256=BASELINE_SHA256,complete_source_inverse_manifest_sha256=source_inverse19115.MANIFEST_SHA256,reviewed_116_overlay_sha256=source_inverse19115.OVERLAY116_SHA256)

def current_fingerprint(source):
    source=Path(source);scope(source)
    spec=importlib.util.spec_from_file_location('provenance115_builder',source/'native1960/build_native1960.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    return module.compute_fingerprint1990(source/'native1960')[0]

def baseline_header(source):
    source=Path(source);pins=baseline(source)
    return generated_header(source/REFERENCE,pins['fingerprint'])

def expected(source,gpu,nr):
    source=Path(source);pins=baseline(source);fp=current_fingerprint(source)
    need(type(gpu) is bytes and type(nr) is bytes,'Immutable native payload bytes required')
    inspect(gpu,fp);need('.text' in sections(nr),'NR executable section missing')
    need(sha(gpu)!=pins['gpu']['sha256'],'New GPU payload must be distinguished from baseline')
    need(sha(nr)!=pins['nr']['sha256'],'NR1955 reviewed code removal must be distinguished from baseline')
    return dict(schema=SCHEMA,status='passed',baseline_release='ulike-v1.9.114',baseline_source_commit=pins['baseline_source_commit'],baseline_gpu=pins['gpu'],baseline_nr=pins['nr'],baseline_compute_fingerprint=pins['fingerprint'],compute_fingerprint=fp,gpu_sha256=sha(gpu),gpu_bytes=len(gpu),nr_sha256=sha(nr),nr_bytes=len(nr),current_gpu_rebuild_sha256=sha(gpu),current_nr_rebuild_sha256=sha(nr),baseline_gpu_rebuild_sha256=pins['gpu']['sha256'],baseline_nr_rebuild_sha256=pins['nr']['sha256'],current_and_baseline_rebuilt_with_same_ndk=True,ndk_revision=NDK_REVISION,gpu_compile_flags=list(FLAGS),nr_compile_flags=list(NR_FLAGS),baseline_header_sha256=sha(baseline_header(source).encode()),current_header_sha256=sha(generated_header(source,fp).encode()),numerical_equivalence_claimed_by_this_build_proof=False,physical_android_tested=False,nr1955_byte_identical_to_published114=False,front_lag_exact_delta_verified=True,**scope(source))

def verify(source,gpu,nr,report):
    need(exact_equal(report,expected(source,gpu,nr)),'Native115 provenance differs from exact payload/source graph');return report

def build(source,ndk,native_out,nr_out,work,baseline_gpu,baseline_nr):
    source,ndk,native_out,nr_out,work=map(Path,(source,ndk,native_out,nr_out,work));pins=baseline(source)
    for kind,raw in [('gpu',baseline_gpu),('nr',baseline_nr)]:need(type(raw) is bytes and len(raw)==pins[kind]['bytes'] and sha(raw)==pins[kind]['sha256'],'Wrong published114 '+kind)
    need(not work.exists(),'Fresh native115 provenance directory required');work.mkdir(parents=True)
    need(re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',(ndk/'source.properties').read_text(),re.M),'Pinned NDKr27c required')
    tool=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin';clang=tool/'aarch64-linux-android26-clang'
    gpu=(native_out/'libulike_gpu1960.so').read_bytes();nr=(nr_out/'libulike_nr1955.so').read_bytes()
    def run(label,command):
        p=subprocess.run([str(x) for x in command],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(work/(label+'.log')).write_text(p.stdout);need(p.returncode==0,'Native provenance compile failed: '+p.stdout[-4000:])
    for label,tree,header,want_gpu,want_nr in [('baseline114',source/REFERENCE,baseline_header(source),baseline_gpu,baseline_nr),('current115',source,generated_header(source,current_fingerprint(source)),gpu,nr)]:
        d=work/label;d.mkdir();(d/'native1960').mkdir();(d/'native1955').mkdir()
        # residual_blocks includes ../native1955; create that complete dependency first.
        nr_source=(tree/'native1955/single_noise1955.c').read_bytes();(d/'native1955/single_noise1955.c').write_bytes(nr_source)
        residual=d/'native1960/residual_blocks1961.c';residual.write_bytes((tree/'native1960/residual_blocks1961.c').read_bytes())
        (d/'shader_sources1960.h').write_text(header);engine=d/'engine1960.c';engine.write_bytes((tree/'native1960/engine1960.c').read_bytes())
        target=d/'libulike_gpu1960.so';run(label+'-gpu',[clang,*FLAGS,'-I'+str(d),engine,residual,'-lEGL','-lGLESv3','-lm','-o',target]);need(target.read_bytes()==want_gpu,label+' complete GPU rebuild differs')
        noise=d/'single_noise1955.c';noise.write_bytes(nr_source);target=d/'libulike_nr1955.so';run(label+'-nr',[clang,*NR_FLAGS,noise,'-lm','-o',target]);run(label+'-nr-strip',[tool/'llvm-objcopy','--strip-unneeded',target]);need(target.read_bytes()==want_nr,label+' complete NR rebuild differs')
    report=expected(source,gpu,nr);verify(source,gpu,nr,report);(work/'result.json').write_text(json.dumps(report,sort_keys=True,indent=2)+'\n');return report

def source_negative_controls(source):
    source=Path(source);pins=baseline(source);scope(source)
    names={BASELINE_FILE,source_inverse19115.MANIFEST,source_inverse19115.OVERLAY116,'native1960/compute_dependencies1990.json',*pins['native_sources'],*pins['numerical_java_sources'],*JAVA_ADDED}
    names.update(REFERENCE+'/'+n for n in pins['reference_sources'])
    with tempfile.TemporaryDirectory(prefix='ulike115-provenance-') as td:
        tree=Path(td)
        for name in names:
            p=tree/name;p.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source/name,p)
        scope(tree)
        cases=list(dict.fromkeys([BASELINE_FILE,source_inverse19115.MANIFEST,source_inverse19115.OVERLAY116,'native1944/kernels1935.c','native1955/single_noise1955.c','ColourCache1976.java',*CHANGED,*JAVA_CHANGED,'native1960/compute_dependencies1990.json',REFERENCE+'/native1960/engine1960.c']))
        for name in cases:
            p=tree/name;old=p.read_bytes();p.write_bytes(old+b'\n')
            try:
                try:scope(tree)
                except (AssertionError,RuntimeError,ValueError,TypeError,KeyError):pass
                else:raise AssertionError('Unreviewed115 source accepted: '+name)
            finally:p.write_bytes(old)
    return len(cases)+2

def negative_controls(source,gpu,nr,report):
    verify(source,gpu,nr,report);count=1+source_negative_controls(source)
    def reject(body=report,shader=gpu,noise=nr):
        nonlocal count
        try:verify(source,shader,noise,body)
        except (AssertionError,RuntimeError,ValueError,TypeError,KeyError):count+=1
        else:raise AssertionError('Malformed115 native provenance accepted')
    for key,value in report.items():
        row=copy.deepcopy(report);row.pop(key);reject(row);row=copy.deepcopy(report);row[key]=None;reject(row)
        if type(value) is bool:row=copy.deepcopy(report);row[key]=int(value);reject(row)
        if type(value) is int:row=copy.deepcopy(report);row[key]=float(value);reject(row)
    row=copy.deepcopy(report);row['unexpected']=True;reject(row)
    for label,raw in [('gpu',gpu),('nr',nr)]:
        offsets={0,4,18,len(raw)-1}
        for entry in sections(raw).values():
            start,size=entry[4:6]
            if entry[1]!=8 and size:offsets.update((start,start+size//2,start+size-1))
        for offset in sorted(offsets):
            changed=bytearray(raw);changed[offset]^=1;reject(shader=bytes(changed) if label=='gpu' else gpu,noise=bytes(changed) if label=='nr' else nr)
        for changed in (raw[:-1],raw+b'\0',b'',bytearray(raw)):reject(shader=changed if label=='gpu' else gpu,noise=changed if label=='nr' else nr)
    return count
