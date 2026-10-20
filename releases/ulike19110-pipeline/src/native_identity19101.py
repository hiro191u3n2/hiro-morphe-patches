#!/usr/bin/env python3
"""Prove the .107 regional interpolation delta over complete published .106.

Program selectors and native C are byte-identical. Only the reviewed Strong
regional helper and its single call replace the .106 interpolation expression.
The inverse removes that exact helper/call and must reproduce the complete .106
ELF using the same NDK. Unaffected programs retain exact preprocessed text;
changed Strong programs are explicitly inventoried, never called unchanged.
"""
from pathlib import Path
import copy, importlib.util, json, re, subprocess, tempfile
from native_identity1995 import need, sha, sections
from native_identity1999 import inspect, generated_header
ROOT=Path(__file__).resolve().parent
BASELINE_FILE='native_identity19101_baseline.json'
BASELINE_FILE_SHA256='7d49697f0cd2713c64cdc6deba32fd2867e6951b986b1be2b7639ae89664dfdf'
BASE_SHA256='6605d390ba0c4cb8eb038ce3c16a2fc544334a35d518f25d82ce221dce7867f7'
BASE_BYTES=178464
BASE_FINGERPRINT='5ab857f6118a9e0b146faf2b118a21a8b2ad17187483baa231fec3f91fc9fbf1'
NR_SHA256=NR_BASE_SHA256='77aecb1fafdb0b13b152225f70a9e01a7cbd303c1267aa386181246e456e9f10'
NR_BYTES=NR_BASE_BYTES=25984
NDK_REVISION='27.2.12479018'
MANIFEST_PATH='native1960/compute_dependencies1990.json'
DIAGNOSTIC_NAME='StrongTrace19106.java'
DIAGNOSTIC_REASON='Bounded post-save Strong intermediate trace; cannot grant quality or speed admission.'
MANIFEST_DELTA={}
BUILDER_PATH='native1960/build_native1960.py'
REVIEWED_BUILDER_SHA256='0320b7f96a7a9ea5c58c7e487e64c1d5811b7c292df7e6383bb9e5349b962c23'
ALLOWED_NATIVE_DELTA=('native1960/strong1960.comp',)
FROZEN_NATIVE_SOURCES=('native1960/engine1960.c','native1960/strong1960.comp')
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
    need(isinstance(raw,bytes) and len(raw)==NR_BYTES and sha(raw)==NR_SHA256,'NR1955 must equal complete published .106 payload')
    need('.text' in sections(raw),'Inherited NR1955 executable section absent')

def baseline(source):
    path=Path(source)/BASELINE_FILE
    need(path.is_file() and not path.is_symlink(),'Fixed native baseline file required')
    raw=path.read_bytes();need(sha(raw)==BASELINE_FILE_SHA256,'Published .106 native baseline pins changed')
    pins=strict_json(raw)
    need(pins.get('schema')=='ulike19107-native-source-baseline-v1' and pins.get('baseline_release')=='ulike-v1.9.106','Native baseline schema/release differs')
    need(pins['gpu']=={'sha256':BASE_SHA256,'bytes':BASE_BYTES,'compute_fingerprint':BASE_FINGERPRINT},'GPU baseline identity differs')
    need(pins['nr1955']=={'sha256':NR_SHA256,'bytes':NR_BYTES},'NR baseline identity differs')
    need(set(pins['restored_source_utf8'])==set(FROZEN_NATIVE_SOURCES),'Frozen native source inventory differs')
    need(set(pins['reviewed_native_delta_sha256'])==set(ALLOWED_NATIVE_DELTA),'Native delta inventory differs')
    for name,body in pins['restored_source_utf8'].items():need(sha(body.encode())==pins['native_source_sha256'][name],'Frozen native source SHA differs: '+name)
    return pins

def verify_native_sources(source=ROOT):
    source=Path(source);pins=baseline(source)
    original_raw=pins['compute_manifest_utf8'].encode();need(sha(original_raw)==pins['compute_manifest_sha256'],'Original dependency manifest hash differs')
    expected=strict_json(original_raw)
    path=source/MANIFEST_PATH;need(path.is_file() and not path.is_symlink(),'Native dependency manifest required')
    current_raw=path.read_bytes();current=strict_json(current_raw);need(current==expected,'Unreviewed compute dependency classification delta')
    entries=current['native_dependencies']
    need(len(entries)==len(set(entries))==len(pins['native_source_sha256'])==47 and set(entries)==set(pins['native_source_sha256']),'Native source inventory differs from .106')
    actual={};delta=[]
    for name,digest in pins['native_source_sha256'].items():
        relative=Path(name);path=source/name
        need(not relative.is_absolute() and '..' not in relative.parts and '\\' not in name,'Unsafe baseline native source path')
        need(path.is_file() and not path.is_symlink(),'Native source missing or symlink: '+name)
        actual[name]=sha(path.read_bytes())
        expected_digest=pins['reviewed_native_delta_sha256'].get(name,digest)
        need(actual[name]==expected_digest,'Unreviewed native/shader/CPU source delta: '+name)
        if actual[name]!=digest:delta.append(name)
    need(sorted(delta)==list(ALLOWED_NATIVE_DELTA),'Exact regional native delta required')
    from legacy_shader19106 import restore_regional19107
    restored=restore_regional19107((source/'native1960/strong1960.comp').read_text(),required=True)
    need(restored==pins['restored_source_utf8']['native1960/strong1960.comp'],'Regional inverse exceeds exact reviewed helper/call')
    need(actual[BUILDER_PATH]==REVIEWED_BUILDER_SHA256,'Inherited fingerprint builder changed')
    return dict(baseline_file_sha256=BASELINE_FILE_SHA256,native_source_sha256=actual,published_baseline_qa_sha256=pins['published_qa_sha256'],
        baseline_compute_manifest_sha256=pins['compute_manifest_sha256'],compute_manifest_sha256=sha(current_raw),
        reviewed_compute_manifest_delta=copy.deepcopy(MANIFEST_DELTA),native_delta_limited_to_regional_interpolation=True,
        reviewed_native_builder_sha256=REVIEWED_BUILDER_SHA256,native_source_delta=sorted(delta),added_jni_exports=[],
        nr1955_inherited=dict(sha256=NR_SHA256,bytes=NR_BYTES,byte_identical_to_19106=True))

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
    defines=['GX_LOCAL_SIZE='+str(local),'GX_STRONG_MODE='+str(mode),'GX_TILE_WIDTH='+str(8 if tiled else 0),'GX_IEEE_DIV73='+str(exact),'GX_POW2_EXACT78='+str(pow2),'GX_TRACE19106='+str(int(row[0]==60))]
    run=subprocess.run(['cc','-E','-P','-x','c',*['-D'+d for d in defines],'-'],input=text,text=True,capture_output=True)
    need(run.returncode==0,'Shader preprocessing failed: '+run.stderr)
    return re.sub(r'\s+',' ',run.stdout).strip()

_program_cache={}
def verify_legacy_programs(source=ROOT):
    source=Path(source);pins=baseline(source)
    # Verify the exact source contract before caching preprocessor observations.
    verify_native_sources(source)
    key=tuple(sha((source/name).read_bytes()) for name in (*FROZEN_NATIVE_SOURCES,BASELINE_FILE))
    if key in _program_cache:return copy.deepcopy(_program_cache[key])
    with tempfile.TemporaryDirectory(prefix='ulike107-native-proof-') as temp:
        rows_old=selector_rows(pins['restored_source_utf8']['native1960/engine1960.c'],Path(temp),'published106')
        rows_new=selector_rows((source/'native1960/engine1960.c').read_text(),Path(temp),'current107')
    need(len(rows_old)==len(rows_new)==61 and rows_new==rows_old,'Existing program selectors changed')
    need(rows_new[60]==[60,0,64,3,0,1,0],'Diagnostic program60 selector differs')
    names=['strong1960','single1960','analysis1960','geometry1960','analysis1961','residual1961','protection1961','compare1961','finish1961']
    from legacy_shader19106 import restore_regional19107
    unchanged={};changed={};inverse={}
    for row in rows_old:
        name='native1960/'+names[row[1]]+'.comp';current=(source/name).read_text();prior=pins['restored_source_utf8'].get(name,current)
        before,after=preprocess(prior,row),preprocess(current,row)
        authorized=(row[1]==0 and row[5]==1)
        if authorized:
            need(before!=after,'Reviewed Strong regional delta missing at program '+str(row[0]))
            restored=restore_regional19107(current,required=True)
            need(preprocess(restored,row)==before,'Inverse Strong program differs at '+str(row[0]))
            changed[str(row[0])]=dict(baseline_sha256=sha(before.encode()),current_sha256=sha(after.encode()))
            inverse[str(row[0])]=sha(before.encode())
        else:
            need(before==after,'Unreviewed shader changed at program '+str(row[0]))
            unchanged[str(row[0])]=sha(after.encode())
    need(bool(changed) and bool(unchanged) and len(unchanged)+len(changed)==61,'Program partition incomplete')
    result=dict(program_count_before=61,program_count_after=61,all_program_selectors_identical=True,
        unchanged_preprocessed_program_sha256=unchanged,unchanged_preprocessed_programs_identical=True,
        reviewed_regional_preprocessed_program_sha256=changed,
        reviewed_regional_inverse_program_sha256=inverse,
        reviewed_regional_inverse_programs_identical_to_19106=True,
        all_production_preprocessed_programs_identical=False,
        diagnostic_program_selector=rows_new[60])
    _program_cache[key]=copy.deepcopy(result)
    return result

def current_fingerprint(source=ROOT):
    source=Path(source);verify_native_sources(source)
    spec=importlib.util.spec_from_file_location('native_identity_compute19107',source/BUILDER_PATH)
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    return module.compute_fingerprint1990(source/'native1960')[0]

def inverse_header(source,pins):
    body=generated_header(source,BASE_FINGERPRINT)
    name='strong1960';current=(Path(source)/'native1960'/(name+'.comp')).read_text()
    def literal(text):return 'static const char '+name+'_source[]=\n'+''.join(json.dumps(line+'\n')+'\n' for line in text.splitlines())+';\n'
    from legacy_shader19106 import restore_regional19107
    restored=restore_regional19107(current,required=True)
    need(restored==pins['restored_source_utf8']['native1960/strong1960.comp'],'Inverse shader differs from frozen .106')
    old,new=literal(current),literal(restored)
    need(body.count(old)==1,'Generated Strong shader literal missing');return body.replace(old,new,1)

def verify(raw,fingerprint,report,source=ROOT,nr=None):
    inspect(raw,fingerprint);need(isinstance(report,dict),'Native inverse proof must be an object')
    need(report.get('schema')=='ulike19107-native-regional-inverse-build-v1' and report.get('status')=='passed','Native regional inverse proof absent')
    expected={'current_sha256':sha(raw),'current_bytes':len(raw),'compute_fingerprint':current_fingerprint(source),
        'baseline_compute_fingerprint':BASE_FINGERPRINT,'baseline_sha256':BASE_SHA256,'inverse_rebuild_sha256':BASE_SHA256,
        'baseline_bytes':BASE_BYTES,'same_ndk_inverse_rebuild_verified':True,'ndk_revision':NDK_REVISION,
        'same_ndk_forward_rebuild_verified':True,'forward_rebuild_sha256':sha(raw),
        'restored_native_sources':list(ALLOWED_NATIVE_DELTA),'compile_flags':list(FLAGS),'link_libraries':['EGL','GLESv3','m'],
        'generated_header_sha256':sha(generated_header(source,fingerprint).encode()),
        'inverse_header_sha256':sha(inverse_header(source,baseline(source)).encode()),**verify_native_sources(source),**verify_legacy_programs(source)}
    need(fingerprint==expected['compute_fingerprint'],'Expected native fingerprint differs from source')
    need(set(report)==set(expected)|{'schema','status'},'Unexpected native proof fields')
    for key,value in expected.items():need(exact_equal(report.get(key),value),'Native regional proof differs: '+key)
    if nr is not None:verify_nr(nr)
    return report

def build_inverse(source,ndk,native_out,work,baseline,nr_baseline=None):
    source,ndk,native_out,work=map(Path,(source,ndk,native_out,work))
    need(isinstance(baseline,bytes) and sha(baseline)==BASE_SHA256 and len(baseline)==BASE_BYTES,'Wrong .106 published native')
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
    need(forward==current,'Same-source/flags forward rebuild differs from packaged .107 ELF')
    restored=compile_source(work/'inverse',inverse,pins['restored_source_utf8']['native1960/engine1960.c'],'inverse')
    need(restored==baseline,'Rebuilt inverse native differs from complete published .106 ELF')
    report=dict(schema='ulike19107-native-regional-inverse-build-v1',status='passed',current_sha256=sha(current),current_bytes=len(current),
        baseline_sha256=BASE_SHA256,baseline_bytes=BASE_BYTES,inverse_rebuild_sha256=sha(restored),compute_fingerprint=fingerprint,
        baseline_compute_fingerprint=BASE_FINGERPRINT,same_ndk_forward_rebuild_verified=True,forward_rebuild_sha256=sha(forward),
        same_ndk_inverse_rebuild_verified=True,ndk_revision=NDK_REVISION,restored_native_sources=list(ALLOWED_NATIVE_DELTA),
        generated_header_sha256=sha(header.encode()),inverse_header_sha256=sha(inverse.encode()),compile_flags=list(FLAGS),link_libraries=['EGL','GLESv3','m'],**proof,**programs)
    verify(current,fingerprint,report,source,nr_baseline)
    (work/'result.json').write_text(json.dumps(report,sort_keys=True,indent=2)+'\n');return report
