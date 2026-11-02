#!/usr/bin/env python3
"""Execute the actual optional .119 native sources against immutable .118.

Desktop GLES and AArch64 Linux/QEMU establish bounded numerical and transaction
coverage. They do not grant an Android runtime certificate or device speed claim.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,shutil,subprocess,sys
import numpy as np

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[1]

def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);sys.modules[name]=m;spec.loader.exec_module(m);return m

def strong(source,work):
    work.mkdir(parents=True,exist_ok=True)
    fix=module('strong_fixtures119',source/'tests19119/arithmetic/strong_fixture19119.py')
    gl=fix.load_transport(source)
    baseline=(source/'native1960/strong1960.comp').read_text()
    candidate=(source/'native19119/strong19119.comp').read_text()
    seam=fix.OLD_CONDITION
    inserted=' || (defined(GX_QUOTIENT19119) && GX_QUOTIENT19119 == 1 && GX_STRONG_MODE == 3 && GX_TILE_WIDTH == 8)'
    assert candidate.count(seam+inserted)==2,'Reviewed quotient conditional changed'
    assert candidate.replace(seam+inserted,seam)==baseline,'Candidate differs outside two reviewed conditional extensions'
    engine=(source/'native19119/engine19119.c').read_text()
    assert 'if(id==48)strncat(define,"#define GX_QUOTIENT19119 1\\n#define GX_DIV_FAST19115 1\\n"' in engine,'Actual backend program48 does not select both changes'
    units={'baseline':fix.compile_unit(baseline),'candidate':fix.compile_unit(candidate,True).replace('#version 310 es\n','#version 310 es\n#define GX_QUOTIENT19119 1\n',1)}
    programs={name:gl.program(text.encode()) for name,text in units.items()}
    assert all(local==64 for _,local in programs.values())
    fixtures=fix.representative_cases(gl,40)+[fix.wide_case(3060,4080,1),fix.wide_case(4080,3060,0)]
    pixels=confidence=changed=nonzero=split_count=0
    for i,(label,u,arrays,first) in enumerate(fixtures):
        split=3 if i%7==0 else 1<<24
        old=gl.execute(*programs['baseline'],arrays,u,split,True)
        new=gl.execute(*programs['candidate'],arrays,u,split,True)
        fix.compare(old,new,'packaged candidate',label)
        pixels+=len(old[0]);confidence+=len(old[1]);split_count+=split==3
        image=arrays[0][(int(u[2])-first)*int(u[0]):(int(u[3])-first)*int(u[0])]
        changed+=int(np.count_nonzero(old[0]!=image));nonzero+=int(np.count_nonzero(old[1]))
    assert changed>1000 and nonzero>0,'No-op fixture coverage'
    good=units['candidate']
    mutations={
      'pixel_xor':('dst[at]=fullPixel(x,row,at,confidenceOut);','dst[at]=fullPixel(x,row,at,confidenceOut)^1u;'),
      'confidence_xor':('confidenceMap[atConfidence]=confidenceOut;','confidenceMap[atConfidence]=confidenceOut^1;'),
      'quotient_weight_zero':('precise float wy=ieeeDiv1973(base,(1.0+lumaRatioSquared19100));','precise float wy=0.0;'),
      'fast_divider_wrong_return':('#if GX_DIV_FAST19115\n    // Candidate only: one exact product;','#if GX_DIV_FAST19115\n    return 0x3f800000u;\n    // Candidate only: one exact product;')}
    negative={}
    for label,(before,after) in mutations.items():
        assert good.count(before)==1,'Mutation seam changed '+label
        bad=good.replace(before,after);program=gl.program(bad.encode())
        for fixture_label,u,arrays,_ in reversed(fixtures):
            old=gl.execute(*programs['baseline'],arrays,u,1<<24,True)
            actual=gl.execute(*program,arrays,u,1<<24,True)
            argb=int(np.count_nonzero(old[0]!=actual[0]));conf=int(np.count_nonzero(old[1]!=actual[1]))
            if argb or conf:
                negative[label]={'detected':True,'fixture':fixture_label,'argb_mismatches':argb,'confidence_mismatches':conf,'shader_sha256':hashlib.sha256(bad.encode()).hexdigest()};break
        assert label in negative,'Intended arithmetic fault escaped '+label
    result=dict(status='passed',assertions=pixels+confidence+len(negative)+3,cases=len(fixtures),pixels_compared=pixels,confidence_words_compared=confidence,
        nonzero_output_changes=changed,nonzero_confidence=nonzero,split_dispatches=split_count,actual_full_shader_gles=True,
        baseline_sha256=sha(source/'native1960/strong1960.comp'),candidate_sha256=sha(source/'native19119/strong19119.comp'),
        actual_program48_macros_verified=True,unchanged_shader_regions_verified=True,
        compiled_shader_sha256={k:hashlib.sha256(v.encode()).hexdigest() for k,v in units.items()},negative_controls=negative,
        immutable_bindings_checked=[0,2,3,4,5,6],renderer=gl.getstr(0x1f01).decode(),gles_version=gl.getstr(0x1f02).decode(),physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

def front(source,work,jdk,ndk):
    work.mkdir(parents=True,exist_ok=True);commands=[]
    def call(args,label,timeout=900):
        args=list(map(str,args));commands.append({'label':label,'argv':args});p=subprocess.run(args,text=True,capture_output=True,timeout=timeout)
        (work/(label+'.log')).write_text(p.stdout+p.stderr)
        assert p.returncode==0,label+' failed: '+(p.stdout+p.stderr)[-5000:]
        return p.stdout
    baseline=source/'native1955/single_noise1955.c';core=source/'native19119/front/front_core19119.c';wrapper=source/'native19119/front/front19119.c'
    builder=module('front119_builder_proof',source/'native19119/front/build_front19119.py')
    identity,identity_pins=builder.fingerprint();(work/'front_identity19119.h').write_text('#define FRONT119_SOURCE_SHA256 "'+identity+'"\n')
    cc=shutil.which('cc');arm=shutil.which('aarch64-linux-gnu-gcc');qemu=shutil.which('qemu-aarch64')
    assert cc and arm and qemu,'Actual host and AArch64 compiler/emulator required'
    common=['-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off','-fno-fast-math','-pthread','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work)]
    fixture=source/'tests19119/arithmetic/FrontKernel19100.c';target=source/'tests19119/arithmetic/FrontSkip119.c'
    source_text=core.read_text();good='if (budget != 0 && detail != 256) { zero_blend = 0; break; }'
    assert source_text.count(good)==1,'Candidate skip branch changed'
    mutant_dir=work/'mutant';mutant_dir.mkdir();shutil.copy2(wrapper,mutant_dir/wrapper.name)
    (mutant_dir/core.name).write_text(source_text.replace(good,'if (budget != 0 && detail != 256 && ax > width) { zero_blend = 0; break; }'))
    reports={};bins={}
    for arch,compiler,runner,flags in [('host',cc,[],[]),('aarch64',arm,[qemu],['-static'])]:
        by_kind={}
        for kind,kernel,is_candidate in [('baseline',baseline,False),('candidate',wrapper,True),('mutant',mutant_dir/wrapper.name,True)]:
            outputs={}
            for corpus,driver in [('broad',fixture),('targeted',target)]:
                if kind=='mutant' and corpus=='broad':continue
                label=arch+'-'+kind+'-'+corpus;binary=work/label;output=work/(label+'.bin')
                defs=['-DFRONT_KERNEL="'+str(kernel)+'"']
                if is_candidate:defs+=['-DFRONT_SKIP_TEST_COUNTER','-DFRONT_SKIP_CANDIDATE','-DJava_com_hiro_ulike_SingleNoise1955_processNative=Java_com_hiro_ulike_CpuFrontOptimized19119_processNative']
                call([compiler,*common,*flags,*defs,driver,'-lm','-o',binary],label+'-compile')
                result=json.loads(call([*runner,binary,output,*([0] if corpus=='broad' else [])],label+'-execute'))
                assert result['status']=='passed';outputs[corpus]={'result':result,'sha256':sha(output),'bytes':output.stat().st_size};bins[(arch,kind,corpus)]=output
            by_kind[kind]=outputs
        for corpus in ('broad','targeted'):
            old=by_kind['baseline'][corpus];new=by_kind['candidate'][corpus]
            assert old['sha256']==new['sha256'] and old['bytes']==new['bytes'],arch+' '+corpus+' output changed'
        b=by_kind['candidate']['broad']['result'];t=by_kind['candidate']['targeted']['result'];m=by_kind['mutant']['targeted']['result']
        assert b['pixel_cases']==320 and b['transform_cases']==8192 and b['model_cases']==32768
        assert t['cases']==640 and t['pixels']==3069312 and t['skipped_blocks']>0
        assert m['skipped_blocks']>t['skipped_blocks']
        assert by_kind['mutant']['targeted']['sha256']!=by_kind['baseline']['targeted']['sha256'],arch+' wrong skip escaped'
        reports[arch]=by_kind
    # Both architectures execute the exact native JNI entry from the library
    # wrapper, including array guards and transactional range commit.
    result=dict(status='passed',assertions=sum(row['candidate']['targeted']['result']['pixels']+row['candidate']['broad']['result'].get('assertions',1)+2 for row in reports.values()),
        actual_candidate_wrapper_executed=True,actual_native_jni_entry=True,actual_arm64_execution=True,
        host_jni_adapter=True,baseline_sha256=sha(baseline),candidate_core_sha256=sha(core),candidate_wrapper_sha256=sha(wrapper),implementation_sha256=identity,
        identity_source_sha256=identity_pins,architectures=reports,negative_controls_detected=2,
        physical_android_tested=False,device_speedup_verified=False,scope='Actual host and AArch64 Linux/QEMU JNI entry via deterministic JNI table; not Android runtime or hardware speed')
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

def test(source,work,jdk=None,ndk=None):
    source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['JAVA_HOME']).resolve();ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
    os.environ['PATH']=str(jdk/'bin')+os.pathsep+os.environ.get('PATH','');os.environ['EGL_PLATFORM']='surfaceless';os.environ['LIBGL_ALWAYS_SOFTWARE']='1'
    inputs=['native19119/strong19119.comp','native19119/engine19119.c','native19119/front/front19119.c','native19119/front/front_core19119.c','native19119/front/build_front19119.py','native1960/strong1960.comp','native1955/single_noise1955.c']
    pins={name:sha(source/name) for name in inputs}
    tests={'strong':strong(source,work/'strong'),'front':front(source,work/'front',jdk,ndk)}
    # The divider's arithmetic block is byte-identical to the independently
    # exercised RNE implementation, so that established rational proof also
    # checks the actual optional .119 denominator implementation.
    old=(source/'native1960/strong1960.comp').read_text();new=(source/'native19119/strong19119.comp').read_text()
    extract=lambda s:s[s.index('#ifndef GX_IEEE_DIV73'):s.index('/* GX16/GX27:')]
    assert extract(old)==extract(new),'Packaged exact divider differs from independent oracle target'
    division=module('division119_proof',source/'host_division19115.py').test(source,work/'division',jdk,ndk)
    tests['division']=division
    assert pins=={name:sha(source/name) for name in inputs},'Arithmetic source changed during test'
    result=dict(schema='ulike19119-noise-arithmetic-host-v1',status='passed',assertions=sum(row['assertions'] for row in tests.values())+1,
        tests=tests,positive_controls=54,negative_controls=7,source_sha256=pins,actual_packaged_candidate_sources=True,physical_android_tested=False,device_speedup_verified=False,fixture_classes_in_runtime=False)
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',type=Path,required=True);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path,required=True);p.add_argument('--ndk',type=Path,required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
