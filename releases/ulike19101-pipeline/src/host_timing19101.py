#!/usr/bin/env python3
"""Run current .101 timing with retained boundary tests and publication-latency regression.
Only release literals and the invalid upper bound after new queue reason24 are adapted."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, re

QUALIFICATION_1995='final class GpuQualification1961 {\n static int publications1995;static ProcessingTiming1947.Trace expectedUnlocked1995;\n static CaptureProbe1995 probe1995;\n static final class CaptureProbe1995 {final long id;final String kind,phase;final boolean alreadyCancelled;CaptureProbe1995(long i,String k,String p,boolean c){id=i;kind=k;phase=p;alreadyCancelled=c;}}\n static CaptureProbe1995 captureProbe1995(){return probe1995;}\n static boolean publicationSucceeded1995(){if(expectedUnlocked1995!=null&&Thread.holdsLock(expectedUnlocked1995))throw new AssertionError("queue called with trace monitor");publications1995++;return publications1995==1;}\n'
FAMILIES_1995='\nfinal class GpuStrongDefault1990 {static int requests;static boolean fail;static void request1994(){requests++;if(fail)throw new IllegalStateException("optional");}}\nfinal class GpuModelDefault1990 {static int requests;static boolean fail;static void requestOptional1994(){requests++;if(fail)throw new IllegalStateException("optional");}}\nfinal class GpuFinishDefault1990 {static int requests;static boolean fail;static String current="";static void requestWide1995(){requests++;if(fail)throw new IllegalStateException("optional");}static String summaryWide1995(){return current;}}\n'

def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    version=re.search(r'VERSION = "([^\"]+)"',(source/'ProcessingTiming1947.java').read_text()).group(1)
    assert version=="1.9.101", "Runtime diagnostic version must match this release target"
    retained=module('timing92old',source/'host_timing1986.py')
    old=module('timing92base',source/'host_timing1982.py')
    prior=module('timing92prior',source/'host_timing1985.py')
    a84=module('timing92a84',source/'host_timing1984.py')
    jdk=Path(jdk);classes=work/'classes';classes.mkdir(exist_ok=True);paths=[]
    for name,body in old.STUBS.items():
        p=work/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True)
        body=retained.adapt_timing_peers1986(prior.adapt_timing_peers1985(a84.adapt_timing_peers1984(body)))
        if 'final class GpuQualification1961 {' in body:
            body=body.replace('final class GpuQualification1961 {', QUALIFICATION_1995)
            body+=FAMILIES_1995
        p.write_text(body);paths.append(p)
    stub=work/'fixtures/com/hiro/ulike/GpuStartup1990.java'
    stub.write_text('''package com.hiro.ulike;
final class GpuStartup1990 {
 static Snapshot current=new Snapshot(); static void initialize(android.content.Context c){}
 static Snapshot snapshot(){return current;}
 static final class Snapshot { int state,preparedPrograms,failedPrograms;long startedNanos,readyNanos,elapsedNanos;boolean domainReady;String finishWideStatus1995=""; }
}''');paths.append(stub)
    old_policy='強ノイズGPU方針: 画質・速度条件を満たす候補を採用。追加認定は保存後'
    new_policy='強ノイズGPU方針: 起動時の実出力検証済み標準経路を優先。追加候補は別途認定'
    original=source/'Timing1982Test.java';body=original.read_text().replace('GPU枠内の待機・判定作業累計','強ノイズ撮影内ワーカーのGPU枠待機・判定累計')
    assert body.count(old_policy)==2 and '1.9.82' in body
    # .90 appends resident reason16; retain original invalid-input checks at new upper bound17.
    for old_call in ('resident1978(owner,4284,5712,4284,5712,0,true,0,1,16)', 'residentExit1982(valid,0,16)'):
        assert body.count(old_call)==1
        body=body.replace(old_call,old_call[:-3]+'17)')
    p=work/'fixtures/com/hiro/ulike/Timing1982Test.java';p.write_text(body.replace('1.9.82',re.search(r'VERSION = "([^"]+)"',(source/'ProcessingTiming1947.java').read_text()).group(1)).replace(old_policy,new_policy));paths.append(p)
    p=work/'fixtures/com/hiro/ulike/QueueTiming1983Test.java';p.write_text(prior.adapt_queue_upper_bound1985((source/'tests1983/qualification/QueueTiming1983Test.java').read_text()).replace('{22,0,0,4,1,80*M,47*M}', '{25,0,0,4,1,80*M,47*M}'));paths.append(p)
    for p in ['tests1984/timing/QueueTiming1984Test.java','tests1985/timing/QueueTiming1985Test.java','tests1986/timing/ForecastTiming1986Test.java','tests1990/timing/SaveTiming1990Test.java','tests1991/timing/CaptureTimeline1991Test.java','tests1992/timing/Timing1992Test.java','tests1993/timing/Timing1993Test.java','tests1994/timing/Timing1994Test.java','tests1995/timing/Timing1995Test.java','tests1996/timing/FrontTiming1996Test.java','tests1997/timing/MemoryTiming1997Test.java','tests1998/timing/RecentCapture1998Test.java']:
        path=source/p
        body=path.read_text()
        if path.name=='Timing1992Test.java':body=body.replace('1.9.92','1.9.101').replace('finishWork1992(t,10,1,1)','finishWork1992(t,11,1,1)')
        if path.name=='QueueTiming1985Test.java':body=body.replace('residentExit1983(other,0,6,22,0,0,0,0,0,0)', 'residentExit1983(other,0,6,25,0,0,0,0,0,0)')
        if path.name=='Timing1994Test.java':body=body.replace('3*24+23','3*25+23').replace('回（枠解放待ち・ロック再取得を含む）','回（撮影内ワーカーの枠解放待ち・ロック再取得を含む。背景検証の待機時間ではありません）')
        if path.name=='Timing1993Test.java':body=body.replace('11*23','11*25').replace('9*23+22','9*25+22').replace('1024,0,-1,23,0,0,1,0,1024','1024,0,-1,25,0,0,1,0,1024')
        if body!=path.read_text():
            path=work/'fixtures/com/hiro/ulike'/path.name;path.write_text(body)
        paths.append(path)
    prior.run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,source/'ProcessingTiming1947.java',*paths],work/'compile.log')
    cases={}
    for name in ('Timing1982Test','QueueTiming1983Test','QueueTiming1984Test','QueueTiming1985Test','ForecastTiming1986Test','SaveTiming1990Test','CaptureTimeline1991Test','Timing1992Test','Timing1993Test','Timing1994Test','Timing1995Test','FrontTiming1996Test','MemoryTiming1997Test','RecentCapture1998Test'):
        cases[name]=prior.parsed(prior.run([jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-cp',classes,'com.hiro.ulike.'+name],work/(name+'.log')))
    # Real capture owner and real timing recorder together: camera stubs only.
    metadata=module('capture92_metadata',source/'host_metadata1933.py')
    meta_paths=[]
    for name,body in metadata.STUBS.items():
        p=work/'metadata-fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);meta_paths.append(p)
    shutter=module('shutter94_fixture',source/'host_shutter1994.py')
    motion=work/'metadata-fixtures/android/view/MotionEvent.java';motion.parent.mkdir(parents=True,exist_ok=True);motion.write_text(shutter.MOTION_EVENT);meta_paths.append(motion)
    prior.run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',classes,'-d',classes,
               source/'ShotContext1932.java',source/'ShutterTiming1994.java',*meta_paths,source/'tests1991/timing/CaptureBinding1991Test.java',source/'tests1994/shutter/ShutterBinding1994Test.java',source/'tests1995/timing/MetadataBinding1995Test.java',source/'tests1999/capture/CaptureProvenance1999Test.java'],work/'capture-compile.log')
    cases['CaptureBinding1991Test']=prior.parsed(prior.run([jdk/'bin/java','-ea','-cp',classes,
        'com.hiro.ulike.CaptureBinding1991Test'],work/'CaptureBinding1991Test.log'))
    cases['ShutterBinding1994Test']=prior.parsed(prior.run([jdk/'bin/java','-ea','-cp',classes,
        'com.hiro.ulike.ShutterBinding1994Test'],work/'ShutterBinding1994Test.log'))
    cases['MetadataBinding1995Test']=prior.parsed(prior.run([jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.MetadataBinding1995Test'],work/'MetadataBinding1995Test.log'))
    cases['CaptureProvenance1999Test']=prior.parsed(prior.run([jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.CaptureProvenance1999Test'],work/'CaptureProvenance1999Test.log'))
    result=dict(status='passed',assertions=sum(x['assertions'] for x in cases.values()),cases=cases,physical_android_tested=False,device_speedup_verified=False,publication_success_observed_only=True,parallel_interval_union_verified=True,production_source_sha256=hashlib.sha256((source/'ProcessingTiming1947.java').read_bytes()).hexdigest())
    for key in ('recent_history_bounded16_verified','recent_history_success_only_verified','recent_history_exact_groups_verified','recent_history_lock_order_verified'):
        assert cases['RecentCapture1998Test'].get(key) is True,key
        result[key]=True
    for key in ('capture_bitmap_provenance_verified','ambiguous_metadata_trace_preserved','exact_input_lookup_verified'):
        assert cases['CaptureProvenance1999Test'].get(key) is True,key
        result[key]=True
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk',required=True);p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),ensure_ascii=False))
