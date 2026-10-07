#!/usr/bin/env python3
"""Run shipped numerical and capture-boundary policies; Android is not executed."""
from pathlib import Path
import importlib.util
import json
import os

SUITES = [('native_speed','host_native1935.py'),('fusion','host_fusion1933.py'),('q1','host_q1_1934.py'),
          ('capture','host_capture1933.py'),('metadata','host_metadata1933.py'),
          ('fast','host_fast1933.py'),('integration','host_integration1933.py'),
          ('yuv_geometry','host_yuv_geometry1934.py'),('spatial','host_spatial1934.py'),
          ('faces','host_faces1934.py'),('scheduler','host_scheduler1934.py'),
          ('s2','host_s2_1935.py'),('resize_speed','host_resize1935.py'),
          ('pipeline_speed','host_pipeline1935.py'),
          ('save_queue','host_save1935.py')]

def test(root, work, android):
    root,work,android=Path(root),Path(work),Path(android)
    results={}
    for name,filename in SUITES:
        path=root/filename
        if not path.is_file():raise RuntimeError('Missing host suite: '+filename)
        spec=importlib.util.spec_from_file_location('ulike1935_'+name,path)
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        result=module.test(root,work)
        if not isinstance(result,dict) or result.get('status')!='passed' or type(result.get('assertions')) is not int or result['assertions']<=0:
            raise RuntimeError('Host suite failed or missing executed assertions: '+filename)
        json.dumps(result,allow_nan=False)
        results[name]=result
        if name == 'native_speed':
            os.environ['ULIKE_NATIVE_TEST_DIR'] = str(work / 'host-native1935' / 'lib')
        (work/('host-speed-'+name+'-result.json')).write_text(json.dumps(result,ensure_ascii=False,indent=2,allow_nan=False)+'\n')
    return {'status':'passed','assertions':sum(r['assertions'] for r in results.values()),'suites':results,
            'device_tested':False,'performance_measured_on_device':False}
