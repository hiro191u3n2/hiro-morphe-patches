#!/usr/bin/env python3
"""Execute per-shot diagnostics and the affected quality/capture/save contracts."""
from pathlib import Path
import importlib.util, json, re

def load(root, name):
    spec=importlib.util.spec_from_file_location('ulike1947_'+name, root/(name+'.py'))
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    return module

def count(value):
    if isinstance(value,dict):
        if type(value.get('assertions')) is int:return value['assertions']
        return sum(count(v) for v in value.values())
    return 0

def test(root, work, android):
    root,work,android=Path(root),Path(work)/'host-regression1947',Path(android)
    work.mkdir(parents=True,exist_ok=True)
    results={}
    timing=load(root,'host_timing1947').test(root,work)
    match=re.search(r'PASS (\d+) checks:',timing.get('result',''))
    if not timing.get('ok') or not match:raise RuntimeError('Timer assertions missing')
    timing.update(status='passed',assertions=int(match.group(1)))
    results['timing']=timing
    for name in ['host_quality1947','host_pipeline1942','host_scheduling1944','host_capture_save1947','host_io1947']:
        module=load(root,name)
        value=module.test(root,work) if name in ['host_capture_save1947','host_io1947'] else module.test(root,work,android)
        if value.get('status')!='passed' or count(value)<=0:raise RuntimeError('Executed passing assertions missing: '+name)
        value['assertions']=count(value)
        results[name]=value
    result={'status':'passed','assertions':sum(count(v) for v in results.values()),'suites':results,
            'device_tested':False,'performance_measured_on_device':False,
            'native_payload_scope':'Native library and entry point payload remain byte-identical to the pinned, previously verified public baseline.'}
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+'\n')
    return result
