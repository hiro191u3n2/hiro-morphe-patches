#!/usr/bin/env python3
"""Execute the H6-H11 exact host oracles plus the published H1-H5 suites."""
from pathlib import Path
import importlib.util,json,os,subprocess,sys

def load(root,name):
    spec=importlib.util.spec_from_file_location('ulike1951_'+name,Path(root)/(name+'.py'))
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def run(cmd,env=None):
    p=subprocess.run([str(x) for x in cmd],capture_output=True,text=True,check=True,env=env,timeout=360)
    return p.stdout+p.stderr

def test(root,work,android):
    root,work=Path(root),Path(work)/'host-regression1951'
    work.mkdir(parents=True,exist_ok=True)
    original=load(root,'host_regression1950').test(root,work,android)
    suites=dict(original['suites'])
    moire=load(root,'host_moire1951').test(root,work)
    assert moire['host_C_to_original_Java_pixel_exact'] and moire['pixels_compared']>100000
    suites['h6_moire_exact']={**moire,'assertions':moire['pixels_compared']}
    h7=run(['bash',root/'tests/h7/run_host.sh',work/'h7'])
    assert 'PASS' in h7 and '122827264' in h7.replace(',',''),h7
    suites['h7_primary_denoise_exact']={'status':'passed','assertions':122827264,'baseline_c_jni_pixel_exact':True,'output_copy_race_guard':True}
    env=os.environ.copy();env.update(EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
    h8=run([sys.executable,root/'h8gpu/test_bilateral1950.py'],env)
    assert 'PASS: 640 exact integer cases' in h8,h8
    suites['h8_two_pass_gpu_exact']={'status':'passed','assertions':640,'software_egl_two_pass_pixel_exact':True,'gpu_execution_on_physical_android':False}
    env['ULIKE_H9_PREBUILT']=str(work/'host-regression1950'/'gpu-finish1950-host')
    env['ULIKE_H9_WORK']=str(work)
    h9=run([sys.executable,root/'tests/h9/verify_h9_h11.py'],env)
    for marker in ('GPU saved-vs-general pixel equality passed: 32 tile cases',
                   'Lossless packed-policy CPU finish: 39990 pixels',
                   'H10 overlap, buffer ownership, per-tile CPU fallback, pixel equality: 14310'):
        assert marker in h9, h9
    suites['h9_h11_gpu_exact']={'status':'passed','assertions':32+39990+14310,
                                 'policy_transfer_bytes_per_pixel_before':16,
                                 'policy_transfer_bytes_per_pixel_after':8,
                                 'software_egl_saved_output_pixel_exact':True,
                                 'overlap_and_cpu_fallback_pixel_exact':True}
    result={**original,'assertions':sum(v['assertions'] for v in suites.values()),'suites':suites,
            'h6_h11_selected':['H6','H7','H8','H9','H10','H11'],'status':'passed',
            'pixel_equivalence_to_baseline':True,'full_resolution_nv21_checked':'4080x3060',
            'gpu_execution_on_physical_android':False,'device_tested':False}
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+'\n')
    return result
