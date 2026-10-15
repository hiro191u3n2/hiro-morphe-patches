#!/usr/bin/env python3
"""Prove the .106 diagnostic-only GPU delta over complete published .105.

All original programs 0..59 preserve their preprocessed shader text and actual
native program selection. Program60 is a separate post-save trace. Both a full
same-source forward build and an inverse build with the two frozen source files
and fingerprint restored must reproduce the exact current and published ELFs.
NR1955 remains byte-identical. No native byte ranges are exempted.
"""
from pathlib import Path
import copy, importlib.util, json, re, subprocess, tempfile
from native_identity1995 import need, sha, sections
from native_identity1999 import inspect, generated_header
ROOT=Path(__file__).resolve().parent
BASELINE_FILE='native_identity19101_baseline.json'
BASELINE_FILE_SHA256='a2534bdcbc78a6db0f45e67eeecfe5f1012b7d67446007a13ec72d49e5297629'
BASE_SHA256='8555954ed13f4fac0dc7c8c8dc0ef1e424e393eb45861bf99f7f5c9815508823'
BASE_BYTES=171856
BASE_FINGERPRINT='92146ac5ce92b0d55f0cd3f2ae584c8698a6db5643e88921031de8e09aa30665'
NR_SHA256=NR_BASE_SHA256='77aecb1fafdb0b13b152225f70a9e01a7cbd303c1267aa386181246e456e9f10'
NR_BYTES=NR_BASE_BYTES=25984
NDK_REVISION='27.2.12479018'
MANIFEST_PATH='native1960/compute_dependencies1990.json'
DIAGNOSTIC_NAME='StrongTrace19106.java'
DIAGNOSTIC_REASON='Bounded post-save Strong intermediate trace; cannot grant quality or speed admission.'
MANIFEST_DELTA={'excluded_runtime_java':{DIAGNOSTIC_NAME:DIAGNOSTIC_REASON}}
BUILDER_PATH='native1960/build_native1960.py'
REVIEWED_BUILDER_SHA256='0320b7f96a7a9ea5c58c7e487e64c1d5811b7c292df7e6383bb9e5349b962c23'
ALLOWED_NATIVE_DELTA=('native1960/engine1960.c','native1960/strong1960.comp')
FLAGS=['-std=c11','-O3','-shared','-fPIC','-fvisibility=hidden','-fno-fast-math','-ffp-contract=off','-Wl,--no-undefined','-Wl,--build-id=sha1','-Wl,-z,max-page-size=16384','-Wl,-soname,libulike_gpu1960.so','-Wall','-Wextra','-Werror','-Wno-misleading-indentation']

def strict_json(raw):
    """Reject duplicate JSON keys instead of silently replacing classifications."""
    def pairs(items):
        result = {}
        for key, value in items:
            need(key not in result, 'Duplicate native proof JSON key: ' + key)
            result[key] = value
        return result
    return json.loads(raw, object_pairs_hook=pairs)


def exact_equal(value, expected):
    """Proof booleans and byte counts cannot be replaced by numeric lookalikes."""
    if type(value) is not type(expected):
        return False
    if isinstance(expected, dict):
        return value.keys() == expected.keys() and all(exact_equal(value[key], item)
                                                       for key, item in expected.items())
    if isinstance(expected, list):
        return len(value) == len(expected) and all(exact_equal(left, right)
                                                   for left, right in zip(value, expected))
    return value == expected



def verify_nr(raw):
    need(isinstance(raw,bytes) and len(raw)==NR_BYTES and sha(raw)==NR_SHA256,'NR1955 must equal complete published .105 payload')
    need('.text' in sections(raw),'Inherited NR1955 executable section absent')

def baseline(source):
    path=Path(source)/BASELINE_FILE
    need(path.is_file() and not path.is_symlink(),'Fixed native baseline file required')
    raw=path.read_bytes();need(sha(raw)==BASELINE_FILE_SHA256,'Published .105 native baseline pins changed')
    pins=strict_json(raw)
    need(pins.get('schema')=='ulike19106-native-source-baseline-v1' and pins.get('baseline_release')=='ulike-v1.9.105','Native baseline schema/release differs')
    need(pins['gpu']=={'sha256':BASE_SHA256,'bytes':BASE_BYTES,'compute_fingerprint':BASE_FINGERPRINT},'GPU baseline identity differs')
    need(pins['nr1955']=={'sha256':NR_SHA256,'bytes':NR_BYTES},'NR baseline identity differs')
    need(set(pins['restored_source_utf8'])==set(pins['reviewed_native_delta_sha256'])==set(ALLOWED_NATIVE_DELTA),'Native delta inventory differs')
    for name,body in pins['restored_source_utf8'].items():need(sha(body.encode())==pins['native_source_sha256'][name],'Frozen native source SHA differs: '+name)
    return pins

def verify_native_sources(source=ROOT):
    source=Path(source);pins=baseline(source)
    original_raw=pins['compute_manifest_utf8'].encode();need(sha(original_raw)==pins['compute_manifest_sha256'],'Original dependency manifest hash differs')
    expected=strict_json(original_raw)
    need(DIAGNOSTIC_NAME not in expected['excluded_runtime_java'],'Diagnostic helper already classified in baseline')
    expected['excluded_runtime_java'][DIAGNOSTIC_NAME]=DIAGNOSTIC_REASON
    path=source/MANIFEST_PATH;need(path.is_file() and not path.is_symlink(),'Native dependency manifest required')
    current_raw=path.read_bytes();current=strict_json(current_raw);need(current==expected,'Unreviewed compute dependency classification delta')
    entries=current['native_dependencies']
    need(len(entries)==len(set(entries))==len(pins['native_source_sha256'])==47 and set(entries)==set(pins['native_source_sha256']),'Native source inventory differs from .105')
    actual={};delta=[]
    for name,digest in pins['native_source_sha256'].items():
        relative=Path(name);path=source/name
        need(not relative.is_absolute() and '..' not in relative.parts and '\\' not in name,'Unsafe baseline native source path')
        need(path.is_file() and not path.is_symlink(),'Native source missing or symlink: '+name)
        actual[name]=sha(path.read_bytes())
        expected_digest=pins['reviewed_native_delta_sha256'].get(name,digest)
        need(actual[name]==expected_digest,'Unreviewed native/shader/CPU source delta: '+name)
        if actual[name]!=digest:delta.append(name)
    need(sorted(delta)==list(ALLOWED_NATIVE_DELTA),'Exact diagnostic native delta required')
    need(actual[BUILDER_PATH]==REVIEWED_BUILDER_SHA256,'Inherited fingerprint builder changed')
    return dict(baseline_file_sha256=BASELINE_FILE_SHA256,native_source_sha256=actual,published_baseline_qa_sha256=pins['published_qa_sha256'],
        baseline_compute_manifest_sha256=pins['compute_manifest_sha256'],compute_manifest_sha256=sha(current_raw),
        reviewed_compute_manifest_delta=copy.deepcopy(MANIFEST_DELTA),native_delta_limited_to_diagnostic_trace=True,
        reviewed_native_builder_sha256=REVIEWED_BUILDER_SHA256,native_source_delta=sorted(delta),added_jni_exports=[],
        nr1955_inherited=dict(sha256=NR_SHA256,bytes=NR_BYTES,byte_identical_to_19105=True))

def selector_rows(engine,directory,label):
    """Execute selector declarations extracted verbatim from the native source."""
    constants='\n'.join(re.findall(r'^#define (?:BASE_PROGRAMS|VARIANTS|STRONG_MODES|STRONG_PROGRAM_BASE|EXACT_STRONG_PROGRAM_BASE|EXACT_TILED_STRONG_PROGRAM_BASE|POW2_STRONG_PROGRAM_BASE|POW2_TILED_STRONG_PROGRAM_BASE|EXACT_PREP_PROGRAM_BASE|TRACE_STRONG_PROGRAM19106|PROGRAMS)\b.*$',engine,re.M))
    program=engine[engine.index('static int program(int id){'):]
    start=program.index('\n',program.index('if(!shader){'))+1
    stop=program.index(' if(base==0)')
    declarations=program[start:stop]
    source='#include <stdio.h>\n'+constants+'\nint main(void){for(int id=0;id<PROGRAMS;id++){\n'+declarations+'\nprintf("%d %d %d %d %d %d %d\\n",id,base,requested,mode,tiled,exact,pow2);}\nreturn 0;}\n'
    path=directory/(label+'.c');path.write_text(source);exe=directory/label
    result=subprocess.run(['cc','-std=c11','-O0',str(path),'-o',str(exe)],text=True,capture_output=True)
    need(result.returncode==0,'Native program selector compile failed: '+result.stderr)
    output=subprocess.check_output([str(exe)],text=True)
    return [list(map(int,line.split())) for line in output.splitlines()]

def preprocess(shader,row):
    _,base,local,mode,tiled,exact,pow2=row
    text=re.sub(r'^\s*#(?:version|extension)\b[^\n]*','',shader,flags=re.M)
    defines=['GX_LOCAL_SIZE='+str(local),'GX_STRONG_MODE='+str(mode),'GX_TILE_WIDTH='+str(8 if tiled else 0),'GX_IEEE_DIV73='+str(exact),'GX_POW2_EXACT78='+str(pow2),'GX_TRACE19106=0']
    run=subprocess.run(['cc','-E','-P','-x','c',*['-D'+d for d in defines],'-'],input=text,text=True,capture_output=True)
    need(run.returncode==0,'Shader preprocessing failed: '+run.stderr)
    return re.sub(r'\s+',' ',run.stdout).strip()

_program_cache={}
def verify_legacy_programs(source=ROOT):
    source=Path(source);pins=baseline(source)
    key=tuple(sha((source/name).read_bytes()) for name in ('native1960/engine1960.c','native1960/strong1960.comp',BASELINE_FILE))
    if key in _program_cache:return copy.deepcopy(_program_cache[key])
    with tempfile.TemporaryDirectory(prefix='ulike106-native-proof-') as temp:
        rows_old=selector_rows(pins['restored_source_utf8']['native1960/engine1960.c'],Path(temp),'published105')
        rows_new=selector_rows((source/'native1960/engine1960.c').read_text(),Path(temp),'current106')
    need(len(rows_old)==60 and len(rows_new)==61 and rows_new[:60]==rows_old,'Existing program selectors changed')
    need(rows_new[60]==[60,0,64,3,0,1,0],'Diagnostic program60 selector differs')
    names=['strong1960','single1960','analysis1960','geometry1960','analysis1961','residual1961','protection1961','compare1961','finish1961']
    results={}
    for row in rows_old:
        name='native1960/'+names[row[1]]+'.comp';current=(source/name).read_text();prior=pins['restored_source_utf8'].get(name,current)
        before,after=preprocess(prior,row),preprocess(current,row)
        need(before==after,'Production shader changed at program '+str(row[0]))
        results[str(row[0])]=sha(after.encode())
    result=dict(program_count_before=60,program_count_after=61,production_program_selectors_identical=True,
        production_preprocessed_program_sha256=results,production_preprocessed_programs_identical=True,
        diagnostic_program_selector=rows_new[60])
    _program_cache[key]=copy.deepcopy(result)
    return result

def current_fingerprint(source=ROOT):
    source=Path(source);verify_native_sources(source)
    spec=importlib.util.spec_from_file_location('native_identity_compute19106',source/BUILDER_PATH)
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    return module.compute_fingerprint1990(source/'native1960')[0]

def inverse_header(source,pins):
    body=generated_header(source,BASE_FINGERPRINT)
    name='strong1960';current=(Path(source)/'native1960'/(name+'.comp')).read_text()
    def literal(text):return 'static const char '+name+'_source[]=\n'+''.join(json.dumps(line+'\n')+'\n' for line in text.splitlines())+';\n'
    old,new=literal(current),literal(pins['restored_source_utf8']['native1960/strong1960.comp'])
    need(body.count(old)==1,'Generated Strong shader literal missing');return body.replace(old,new,1)

def verify(raw,fingerprint,report,source=ROOT,nr=None):
    inspect(raw,fingerprint);need(isinstance(report,dict),'Native inverse proof must be an object')
    need(report.get('schema')=='ulike19106-native-diagnostic-inverse-build-v1' and report.get('status')=='passed','Native diagnostic inverse proof absent')
    expected={'current_sha256':sha(raw),'current_bytes':len(raw),'compute_fingerprint':current_fingerprint(source),
        'baseline_compute_fingerprint':BASE_FINGERPRINT,'baseline_sha256':BASE_SHA256,'inverse_rebuild_sha256':BASE_SHA256,
        'baseline_bytes':BASE_BYTES,'same_ndk_inverse_rebuild_verified':True,'ndk_revision':NDK_REVISION,
        'same_ndk_forward_rebuild_verified':True,'forward_rebuild_sha256':sha(raw),
        'restored_native_sources':list(ALLOWED_NATIVE_DELTA),'compile_flags':list(FLAGS),'link_libraries':['EGL','GLESv3','m'],
        'generated_header_sha256':sha(generated_header(source,fingerprint).encode()),
        'inverse_header_sha256':sha(inverse_header(source,baseline(source)).encode()),**verify_native_sources(source),**verify_legacy_programs(source)}
    need(fingerprint==expected['compute_fingerprint'],'Expected native fingerprint differs from source')
    need(set(report)==set(expected)|{'schema','status'},'Unexpected native proof fields')
    for key,value in expected.items():need(exact_equal(report.get(key),value),'Native diagnostic proof differs: '+key)
    if nr is not None:verify_nr(nr)
    return report

def build_inverse(source,ndk,native_out,work,baseline,nr_baseline=None):
    source,ndk,native_out,work=map(Path,(source,ndk,native_out,work))
    need(isinstance(baseline,bytes) and sha(baseline)==BASE_SHA256 and len(baseline)==BASE_BYTES,'Wrong .105 published native')
    if nr_baseline is not None:verify_nr(nr_baseline)
    proof=verify_native_sources(source);programs=verify_legacy_programs(source);fingerprint=current_fingerprint(source)
    current=(native_out/'libulike_gpu1960.so').read_bytes();inspect(current,fingerprint)
    need(not work.exists(),'Fresh inverse native build directory required');work.mkdir(parents=True)
    header=(native_out/'shader_sources1960.h').read_text();need(header==generated_header(source,fingerprint),'Exact generated shader header required')
    pins=strict_json((source/BASELINE_FILE).read_bytes());inverse=inverse_header(source,pins)
    need(re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',(ndk/'source.properties').read_text(),re.M),'Pinned NDK r27c required')
    clang=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang'
    def compile_source(directory,body,engine,label):
        directory.mkdir(parents=True);(directory/'shader_sources1960.h').write_text(body)
        engine_path=directory/'engine1960.c';engine_path.write_text(engine);target=directory/'libulike_gpu1960.so'
        command=[str(v) for v in [clang,*FLAGS,'-I'+str(directory),engine_path,source/'native1960/residual_blocks1961.c','-lEGL','-lGLESv3','-lm','-o',target]]
        result=subprocess.run(command,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        (work/(label+'-build.log')).write_text(result.stdout);need(result.returncode==0,label+' native build failed: '+result.stdout[-4000:]);return target.read_bytes()
    forward=compile_source(work/'forward',header,(source/'native1960/engine1960.c').read_text(),'forward')
    need(forward==current,'Same-source/flags forward rebuild differs from packaged .106 ELF')
    restored=compile_source(work/'inverse',inverse,pins['restored_source_utf8']['native1960/engine1960.c'],'inverse')
    need(restored==baseline,'Rebuilt inverse native differs from complete published .105 ELF')
    report=dict(schema='ulike19106-native-diagnostic-inverse-build-v1',status='passed',current_sha256=sha(current),current_bytes=len(current),
        baseline_sha256=BASE_SHA256,baseline_bytes=BASE_BYTES,inverse_rebuild_sha256=sha(restored),compute_fingerprint=fingerprint,
        baseline_compute_fingerprint=BASE_FINGERPRINT,same_ndk_forward_rebuild_verified=True,forward_rebuild_sha256=sha(forward),
        same_ndk_inverse_rebuild_verified=True,ndk_revision=NDK_REVISION,restored_native_sources=list(ALLOWED_NATIVE_DELTA),
        generated_header_sha256=sha(header.encode()),inverse_header_sha256=sha(inverse.encode()),compile_flags=list(FLAGS),link_libraries=['EGL','GLESv3','m'],**proof,**programs)
    verify(current,fingerprint,report,source,nr_baseline)
    (work/'result.json').write_text(json.dumps(report,sort_keys=True,indent=2)+'\n');return report
