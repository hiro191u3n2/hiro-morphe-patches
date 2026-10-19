#!/usr/bin/env python3
"""Run shipped numerical and capture-boundary policies; Android is not executed."""
from pathlib import Path
import importlib.util
import json
import os

SUITES = [('front_startup','host_front1936.py'), ('renderer_startup','host_renderer1938.py'), ('startup_layout','host_layout1937.py'), ('layout_lifecycle','host_layout_lifecycle1937.py'), ('layout_geometry','host_geometry1937.py')]

def test(root, work, android):
    root,work,android=Path(root),Path(work),Path(android)
    results={}
    for name,filename in SUITES:
        path=root/filename
        if not path.is_file():raise RuntimeError('Missing host suite: '+filename)
        spec=importlib.util.spec_from_file_location('ulike1938_'+name,path)
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        result=module.test(root,work,android)
        if not isinstance(result,dict) or result.get('status')!='passed' or type(result.get('assertions')) is not int or result['assertions']<=0:
            raise RuntimeError('Host suite failed or missing executed assertions: '+filename)
        json.dumps(result,allow_nan=False)
        results[name]=result
        (work/('host-regression-'+name+'-result.json')).write_text(json.dumps(result,ensure_ascii=False,indent=2,allow_nan=False)+'\n')
    return {'status':'passed','assertions':sum(r['assertions'] for r in results.values()),'suites':results,
            'device_tested':False,'performance_measured_on_device':False}
