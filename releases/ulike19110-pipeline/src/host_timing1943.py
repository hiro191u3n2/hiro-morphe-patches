#!/usr/bin/env python3
"""Aggregate executed camera-adapter and real-pixel shutter-moment regressions."""
from pathlib import Path
import argparse,hashlib,importlib.util,json

def test(root,work):
    root=Path(root);work=Path(work);work.mkdir(parents=True,exist_ok=True)
    production={name:root/(name+'.java') for name in ('BurstCapture1933','FusionPixels1933')}
    if not all(path.is_file() for path in production.values()):
        raise RuntimeError('Timing verification requires the exact top-level compiled production sources')
    before={name:hashlib.sha256(path.read_bytes()).hexdigest() for name,path in production.items()}
    results={}
    for name,filename in (('capture_adapter','host_capture1943.py'),('shutter_pixels','host_shutter_moment1943.py')):
        spec=importlib.util.spec_from_file_location('timing1943_'+name,root/filename)
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        result=module.test(root,work)
        if not isinstance(result,dict) or result.get('status')!='passed' or type(result.get('assertions')) is not int or result['assertions']<=0:
            raise RuntimeError('Missing actual executed assertions for '+name)
        results[name]=result
    after={name:hashlib.sha256(path.read_bytes()).hexdigest() for name,path in production.items()}
    if before!=after:raise RuntimeError('Timing production source changed during host verification')
    result={'status':'passed','assertions':sum(item['assertions'] for item in results.values()),'suites':results,
            'production_source_sha256':after,'compiled_top_level_production_sources':True,
            'stationary_four_frame_denoise_preserved':True,'sensor_metadata_bound_to_first_capture':True,
            'scope':'production Java capture adapter with scripted Android objects and production pixel fusion; no device latency measurement',
            'device_tested':False,'tap_to_first_exposure_latency_measured':False}
    (work/'host-timing1943-result.json').write_text(json.dumps(result,indent=2,allow_nan=False)+'\n')
    return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--out',type=Path,required=True)
    args=parser.parse_args();print(json.dumps(test(Path(__file__).resolve().parent,args.out),allow_nan=False))
