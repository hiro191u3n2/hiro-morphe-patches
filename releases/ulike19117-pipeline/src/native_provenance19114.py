#!/usr/bin/env python3
"""Bind native and finish changes to complete rebuilt published .113 binaries.

This is source/build provenance, not numerical equivalence or device performance.
The independent current-kernel whole-output suites provide numerical evidence.
"""
from pathlib import Path
import copy, hashlib, importlib.util, json, re, shutil, subprocess, tempfile
from native_identity1995 import need, sha, sections
from native_identity1999 import generated_header, inspect
from native_identity19101 import exact_equal, FLAGS, NDK_REVISION

BASELINE_FILE='native_provenance19114_baseline.json'
BASELINE_SHA256='574edb14eb34135184550c4020d87489d4a2fd124efc88df9f1aef9d160453cd'
CHANGED=('native1955/single_noise1955.c','native1960/strong1960.comp')
JAVA_CHANGED=('GpuFinishPolicy1992.java',)
SCHEMA='ulike19114-native-source-and-rebuild-v1'
NR_FLAGS=['-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off',
          '-fno-fast-math','-fPIC','-fvisibility=hidden','-shared',
          '-Wl,--no-undefined','-Wl,-z,max-page-size=16384']

def baseline(source):
    raw=(Path(source)/BASELINE_FILE).read_bytes()
    need(sha(raw)==BASELINE_SHA256,'Pinned .113 native provenance manifest changed')
    value=json.loads(raw)
    need(value['schema']=='ulike19114-native-baseline113-v1' and value['baseline_release']=='ulike-v1.9.113','Wrong native baseline')
    return value

def restored_native(source,name):
    source=Path(source)
    need(name in CHANGED,'Only two reviewed native files can be restored')
    if name=='native1960/strong1960.comp':
        from strong_delta19114 import restore_shader19114
        raw=restore_shader19114((source/name).read_text(),required=True).encode()
    else:
        raw=(source/'tests19114/provenance/reference113'/name).read_bytes()
    need(sha(raw)==baseline(source)['native_sources'][name],'Restored .113 native source differs: '+name)
    return raw

def scope(source):
    source=Path(source);pins=baseline(source)
    manifest=(source/'native1960/compute_dependencies1990.json').read_bytes()
    need(sha(manifest)==pins['compute_dependencies_sha256'],'Native dependency classifications changed')
    actual={};changed=[]
    for name,digest in pins['native_sources'].items():
        path=source/name
        need(path.is_file() and not path.is_symlink(),'Missing native dependency '+name)
        actual[name]=sha(path.read_bytes())
        if actual[name]!=digest:
            need(name in CHANGED,'Unreviewed native source changed: '+name)
            restored_native(source,name);changed.append(name)
    need(tuple(sorted(changed))==CHANGED,'Both adopted native optimizations must be present')
    java_actual={};java_changed=[]
    for name,digest in pins['numerical_java_sources'].items():
        path=source/name
        need(path.is_file() and not path.is_symlink(),'Missing numerical Java dependency '+name)
        java_actual[name]=sha(path.read_bytes())
        if java_actual[name]!=digest:
            need(name in JAVA_CHANGED,'Unreviewed numerical Java source changed: '+name)
            reference=source/'tests19114/provenance/reference113'/name
            need(reference.is_file() and not reference.is_symlink() and sha(reference.read_bytes())==digest,'Frozen numerical Java source differs: '+name)
            java_changed.append(name)
    need(tuple(sorted(java_changed))==JAVA_CHANGED,'Reviewed finish optimization must be present')
    dependency=json.loads(manifest)
    need(set(dependency['native_dependencies'])==set(pins['native_sources']) and set(dependency['numerical_java'])==set(pins['numerical_java_sources']),'Compute dependency inventory differs')
    canonical=json.dumps({'schema':'ulike19102-compute-identity-v1','native_dependencies':sorted(dependency['native_dependencies']),'numerical_java':sorted(dependency['numerical_java'])},sort_keys=True,separators=(',',':')).encode()
    restored=hashlib.sha256(b'ulike19102-compute-fingerprint-v1\0'+canonical)
    for name in sorted([*dependency['native_dependencies'],*dependency['numerical_java']]):
        if name in CHANGED:
            raw=restored_native(source,name)
        elif name in JAVA_CHANGED:
            raw=(source/'tests19114/provenance/reference113'/name).read_bytes()
        else:
            raw=(source/name).read_bytes()
        restored.update(name.encode()+b'\0'+str(len(raw)).encode()+b'\0'+raw)
    need(restored.hexdigest()==pins['fingerprint'],'Reconstructed published .113 complete compute fingerprint differs')
    return dict(native_source_sha256=actual,changed_native_sources=sorted(changed),
                numerical_java_source_sha256=java_actual,changed_numerical_java_sources=sorted(java_changed),
                restored_baseline_compute_fingerprint=restored.hexdigest(),
                native_dependency_classification_unchanged=True,
                baseline_manifest_sha256=BASELINE_SHA256)

def current_fingerprint(source):
    source=Path(source);scope(source)
    spec=importlib.util.spec_from_file_location('provenance114_builder',source/'native1960/build_native1960.py')
    builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder)
    return builder.compute_fingerprint1990(source/'native1960')[0]

def baseline_header(source):
    source=Path(source);pins=baseline(source)
    header=generated_header(source,pins['fingerprint'])
    def literal(text):
        return 'static const char strong1960_source[]=\n'+''.join(json.dumps(line+'\n')+'\n' for line in text.splitlines())+';\n'
    current=literal((source/'native1960/strong1960.comp').read_text())
    need(header.count(current)==1,'Current generated Strong header literal missing')
    return header.replace(current,literal(restored_native(source,'native1960/strong1960.comp').decode()),1)

def expected(source,gpu,nr):
    source=Path(source);pins=baseline(source);fingerprint=current_fingerprint(source)
    need(type(gpu) is bytes and type(nr) is bytes,'Immutable native payload bytes required')
    inspect(gpu,fingerprint)
    need('.text' in sections(nr),'NR ELF executable section missing')
    need(sha(gpu)!=pins['gpu']['sha256'] and sha(nr)!=pins['nr']['sha256'],'Both native payloads must be freshly rebuilt')
    return dict(schema=SCHEMA,status='passed',baseline_release='ulike-v1.9.113',
        baseline_source_commit=pins['baseline_source_commit'],
        baseline_gpu=pins['gpu'],baseline_nr=pins['nr'],
        baseline_compute_fingerprint=pins['fingerprint'],compute_fingerprint=fingerprint,
        gpu_sha256=sha(gpu),gpu_bytes=len(gpu),nr_sha256=sha(nr),nr_bytes=len(nr),
        current_gpu_rebuild_sha256=sha(gpu),current_nr_rebuild_sha256=sha(nr),
        baseline_gpu_rebuild_sha256=pins['gpu']['sha256'],baseline_nr_rebuild_sha256=pins['nr']['sha256'],
        current_and_baseline_rebuilt_with_same_ndk=True,ndk_revision=NDK_REVISION,
        gpu_compile_flags=list(FLAGS),nr_compile_flags=list(NR_FLAGS),
        baseline_header_sha256=sha(baseline_header(source).encode()),
        current_header_sha256=sha(generated_header(source,fingerprint).encode()),
        numerical_equivalence_claimed_by_this_build_proof=False,
        physical_android_tested=False,**scope(source))

def verify(source,gpu,nr,report):
    value=expected(source,gpu,nr)
    need(exact_equal(report,value),'Native .114 provenance differs from exact payload/source graph')
    return report

def build(source,ndk,native_out,nr_out,work,baseline_gpu,baseline_nr):
    source,ndk,native_out,nr_out,work=map(Path,(source,ndk,native_out,nr_out,work))
    pins=baseline(source)
    for kind,raw in [('gpu',baseline_gpu),('nr',baseline_nr)]:
        need(type(raw) is bytes and len(raw)==pins[kind]['bytes'] and sha(raw)==pins[kind]['sha256'],'Wrong published .113 '+kind)
    need(not work.exists(),'Fresh .114 native provenance work directory required');work.mkdir(parents=True)
    need(re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',(ndk/'source.properties').read_text(),re.M),'Pinned NDK r27c required')
    tool=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin';clang=tool/'aarch64-linux-android26-clang'
    gpu=(native_out/'libulike_gpu1960.so').read_bytes();nr=(nr_out/'libulike_nr1955.so').read_bytes()
    def run(label,command):
        p=subprocess.run([str(x) for x in command],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        (work/(label+'.log')).write_text(p.stdout);need(p.returncode==0,'Native provenance compile failed: '+p.stdout[-4000:])
    for label,header,nr_source,want_gpu,want_nr in [
        ('baseline113',baseline_header(source),restored_native(source,CHANGED[0]),baseline_gpu,baseline_nr),
        ('current114',generated_header(source,current_fingerprint(source)),(source/CHANGED[0]).read_bytes(),gpu,nr)]:
        directory=work/label;directory.mkdir()
        (directory/'native1960').mkdir();(directory/'native1955').mkdir()
        (directory/'native1955/single_noise1955.c').write_bytes(nr_source)
        residual=directory/'native1960/residual_blocks1961.c';residual.write_bytes((source/'native1960/residual_blocks1961.c').read_bytes())
        (directory/'shader_sources1960.h').write_text(header)
        engine=directory/'engine1960.c';engine.write_bytes((source/'native1960/engine1960.c').read_bytes())
        target_gpu=directory/'libulike_gpu1960.so'
        run(label+'-gpu',[clang,*FLAGS,'-I'+str(directory),engine,residual,'-lEGL','-lGLESv3','-lm','-o',target_gpu])
        noise=directory/'single_noise1955.c';noise.write_bytes(nr_source);target_nr=directory/'libulike_nr1955.so'
        run(label+'-nr',[clang,*NR_FLAGS,noise,'-lm','-o',target_nr])
        run(label+'-nr-strip',[tool/'llvm-objcopy','--strip-unneeded',target_nr])
        need(target_gpu.read_bytes()==want_gpu,label+' complete GPU ELF rebuild differs')
        need(target_nr.read_bytes()==want_nr,label+' complete NR ELF rebuild differs')
    report=expected(source,gpu,nr);verify(source,gpu,nr,report)
    (work/'result.json').write_text(json.dumps(report,sort_keys=True,indent=2)+'\n');return report

def source_negative_controls(source):
    """Exercise complete-source scoping without writing the production tree."""
    source=Path(source);pins=baseline(source);scope(source)
    names=[BASELINE_FILE,'native1960/compute_dependencies1990.json',
           *pins['native_sources'],*pins['numerical_java_sources']]
    names += ['tests19114/provenance/reference113/'+name for name in (CHANGED[0],*JAVA_CHANGED)]
    with tempfile.TemporaryDirectory(prefix='ulike114-provenance-') as temporary:
        tree=Path(temporary)
        for name in names:
            path=tree/name;path.parent.mkdir(parents=True,exist_ok=True)
            shutil.copyfile(source/name,path)
        scope(tree)
        cases=[
            (BASELINE_FILE,(source/BASELINE_FILE).read_bytes()+b'\n'),
            ('native1944/kernels1935.c',(source/'native1944/kernels1935.c').read_bytes()+b'\n'),
            ('ColourCache1976.java',(source/'ColourCache1976.java').read_bytes()+b'\n'),
            (CHANGED[0],restored_native(source,CHANGED[0])),
            (CHANGED[1],restored_native(source,CHANGED[1])),
            (JAVA_CHANGED[0],(source/'tests19114/provenance/reference113'/JAVA_CHANGED[0]).read_bytes()),
            (CHANGED[1],(source/CHANGED[1]).read_bytes()+b'\n'),
            ('tests19114/provenance/reference113/'+CHANGED[0],restored_native(source,CHANGED[0])+b'\n'),
            ('tests19114/provenance/reference113/'+JAVA_CHANGED[0],(source/'tests19114/provenance/reference113'/JAVA_CHANGED[0]).read_bytes()+b'\n'),
        ]
        for name,mutated in cases:
            path=tree/name;original=path.read_bytes()
            need(mutated!=original,'Source mutation has no effect: '+name)
            path.write_bytes(mutated)
            try:
                try:scope(tree)
                except (AssertionError,RuntimeError,ValueError,TypeError,KeyError):pass
                else:raise AssertionError('Unreviewed .114 source accepted: '+name)
            finally:path.write_bytes(original)
        scope(tree)
    return len(cases)+2


def negative_controls(source,gpu,nr,report):
    verify(source,gpu,nr,report);count=1+source_negative_controls(source)
    def reject(body=report,shader=gpu,noise=nr):
        nonlocal count
        try:verify(source,shader,noise,body)
        except (AssertionError,RuntimeError,ValueError,TypeError,KeyError):count+=1
        else:raise AssertionError('Malformed .114 native provenance accepted')
    for key,value in report.items():
        row=copy.deepcopy(report);row.pop(key);reject(row)
        row=copy.deepcopy(report);row[key]=None;reject(row)
        if type(value) is bool:
            row=copy.deepcopy(report);row[key]=int(value);reject(row)
        if type(value) is int:
            row=copy.deepcopy(report);row[key]=float(value);reject(row)
    row=copy.deepcopy(report);row['unexpected']=True;reject(row)
    for label,raw in [('gpu',gpu),('nr',nr)]:
        offsets={0,4,18,len(raw)-1}
        for entry in sections(raw).values():
            start,size=entry[4:6]
            if entry[1]!=8 and size:offsets.update((start,start+size//2,start+size-1))
        for offset in sorted(offsets):
            changed=bytearray(raw);changed[offset]^=1
            reject(shader=bytes(changed) if label=='gpu' else gpu,noise=bytes(changed) if label=='nr' else nr)
        for changed in (raw[:-1],raw+b'\0',b'',bytearray(raw)):
            reject(shader=changed if label=='gpu' else gpu,noise=changed if label=='nr' else nr)
    return count
