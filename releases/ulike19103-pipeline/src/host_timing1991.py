#!/usr/bin/env python3
"""Run retained timing contracts plus real-publication and interval-union regressions."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, re

def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    retained=module('timing91old',source/'host_timing1986.py')
    old=module('timing91base',source/'host_timing1982.py')
    prior=module('timing91prior',source/'host_timing1985.py')
    a84=module('timing91a84',source/'host_timing1984.py')
    jdk=Path(jdk);classes=work/'classes';classes.mkdir(exist_ok=True);paths=[]
    for name,body in old.STUBS.items():
        p=work/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True)
        p.write_text(retained.adapt_timing_peers1986(prior.adapt_timing_peers1985(a84.adapt_timing_peers1984(body))));paths.append(p)
    stub=work/'fixtures/com/hiro/ulike/GpuStartup1990.java'
    stub.write_text('''package com.hiro.ulike;
final class GpuStartup1990 {
 static Snapshot current=new Snapshot(); static void initialize(android.content.Context c){}
 static Snapshot snapshot(){return current;}
 static final class Snapshot { int state,preparedPrograms,failedPrograms;long startedNanos,readyNanos,elapsedNanos;boolean domainReady; }
}''');paths.append(stub)
    old_policy='強ノイズGPU方針: 画質・速度条件を満たす候補を採用。追加認定は保存後'
    new_policy='強ノイズGPU方針: 起動時の実出力検証済み標準経路を優先。追加候補は別途認定'
    original=source/'Timing1982Test.java';body=original.read_text()
    assert body.count(old_policy)==2 and '1.9.82' in body
    # .90 appends resident reason16; retain original invalid-input checks at new upper bound17.
    for old_call in ('resident1978(owner,4284,5712,4284,5712,0,true,0,1,16)', 'residentExit1982(valid,0,16)'):
        assert body.count(old_call)==1
        body=body.replace(old_call,old_call[:-3]+'17)')
    p=work/'fixtures/com/hiro/ulike/Timing1982Test.java';p.write_text(body.replace('1.9.82',re.search(r'VERSION = "([^"]+)"',(source/'ProcessingTiming1947.java').read_text()).group(1)).replace(old_policy,new_policy));paths.append(p)
    p=work/'fixtures/com/hiro/ulike/QueueTiming1983Test.java';p.write_text(prior.adapt_queue_upper_bound1985((source/'tests1983/qualification/QueueTiming1983Test.java').read_text()));paths.append(p)
    for p in ['tests1984/timing/QueueTiming1984Test.java','tests1985/timing/QueueTiming1985Test.java','tests1986/timing/ForecastTiming1986Test.java','tests1990/timing/SaveTiming1990Test.java','tests1991/timing/CaptureTimeline1991Test.java']:paths.append(source/p)
    prior.run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,source/'ProcessingTiming1947.java',*paths],work/'compile.log')
    cases={}
    for name in ('Timing1982Test','QueueTiming1983Test','QueueTiming1984Test','QueueTiming1985Test','ForecastTiming1986Test','SaveTiming1990Test','CaptureTimeline1991Test'):
        cases[name]=prior.parsed(prior.run([jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-cp',classes,'com.hiro.ulike.'+name],work/(name+'.log')))
    # Real capture owner and real timing recorder together: camera stubs only.
    metadata=module('capture91_metadata',source/'host_metadata1933.py')
    meta_paths=[]
    for name,body in metadata.STUBS.items():
        p=work/'metadata-fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);meta_paths.append(p)
    prior.run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',classes,'-d',classes,
               source/'ShotContext1932.java',*meta_paths,source/'tests1991/timing/CaptureBinding1991Test.java'],work/'capture-compile.log')
    cases['CaptureBinding1991Test']=prior.parsed(prior.run([jdk/'bin/java','-ea','-cp',classes,
        'com.hiro.ulike.CaptureBinding1991Test'],work/'CaptureBinding1991Test.log'))
    result=dict(status='passed',assertions=sum(x['assertions'] for x in cases.values()),cases=cases,physical_android_tested=False,device_speedup_verified=False,publication_success_observed_only=True,parallel_interval_union_verified=True,production_source_sha256=hashlib.sha256((source/'ProcessingTiming1947.java').read_bytes()).hexdigest())
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk',required=True);p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),ensure_ascii=False))
