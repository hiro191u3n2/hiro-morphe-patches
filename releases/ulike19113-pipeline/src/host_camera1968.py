#!/usr/bin/env python3
"""Execute fresh renderer and layout regressions using actual .68 production sources."""
from pathlib import Path
import importlib.util

def runner(name, source):
    spec=importlib.util.spec_from_file_location(name,source/(name+'.py'))
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    return module

def test(source,work,jdk=None):
    source,work=Path(source),Path(work)
    results={name:runner(name,source).test(source,work/name,jdk=jdk)
        for name in ('host_renderer1968','host_layout1968','host_front1968')}
    for name,result in results.items():
        if not (result.get('status')=='passed' and type(result.get('assertions')) is int and result['assertions']>0):
            raise RuntimeError('Missing fresh executed host regressions '+name)
        if result.get('physical_android_tested') is not False:
            raise RuntimeError('Physical Android must be explicitly untested '+name)
    return {'status':'passed','assertions':sum(r['assertions'] for r in results.values()),
        'physical_android_tested':False,'camera_preview_rebind_regressions_passed':True,
        'layout_diagnostics_regressions_passed':True,'front_preview_readiness_regressions_passed':True,'tests':results,
        'fresh_gpu_execution_in_this_release':False,
        'scope':'Actual renderer and layout Java source with modeled Android/SDK delivery; complete retained GPU/pixel/native/save code compared separately by DEX/archive preservation.'}
