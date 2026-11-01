#!/usr/bin/env python3
"""Freshly compile real quiet admission engines; mock only Android/GPU image peers.

Negative controls enable the old automatic path and must fail the same behavioral
checks. These are host ownership/persistence checks, not phone speed measurements.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, re, shutil, subprocess

ROOT=Path(__file__).resolve().parent

def load(name,path):
    spec=importlib.util.spec_from_file_location(name,path);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    java=str(Path(jdk)/'bin/java') if jdk else shutil.which('java')
    javac=str(Path(jdk)/'bin/javac') if jdk else shutil.which('javac')
    if not java or not javac:raise RuntimeError('JDK required')
    fixtures=load('quiet_queue_fixtures19120',source/'host_qualification1981.py').FIXTURES
    tracked=[source/n for n in ['GpuQualification1961.java','WholeRoute1952.java','WholeRoute1953.java','host_qualification1981.py','tests19120/QuietQueue19120Test.java','tests19120/QuietWhole19120Test.java','GpuStartup1990.java','GpuCertificates1990.java','CameraSession1965.java','host_startup1990.py','tests19120/QuietStartup19120Test.java','host_quiet_runtime19120.py']]
    pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    commands=[];results={}
    def run(command,label):
        command=list(map(str,command));commands.append({'label':label,'argv':command})
        p=subprocess.run(command,text=True,capture_output=True,timeout=60)
        (work/(label+'.log')).write_text(p.stdout+p.stderr);return p
    for suite in ['queue','whole']:
        for mutant in [False,True]:
            label=suite+('-automatic-enabled-control' if mutant else '-current')
            folder=work/label;fixture=folder/'sources';classes=folder/'classes'
            if folder.exists():shutil.rmtree(folder)
            fixture.mkdir(parents=True);classes.mkdir();files=[]
            def add(name,text):
                p=fixture/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);files.append(p)
            for name,content in fixtures.items():
                if name.endswith('QueueHost1967.java'):continue
                if name=='android/content/SharedPreferences.java':
                    content=content.replace('Editor remove(String key);void apply();','Editor remove(String key);Editor clear();void apply();')
                if name=='android/content/Context.java':
                    content=content.replace('public void apply(){synchronized(Memory.this)', 'public Editor clear(){synchronized(Memory.this){values.clear();}return this;}public void apply(){synchronized(Memory.this)')
                if name=='android/os/Build.java':
                    content=content.replace('public class Build {','public class Build {public static String[] SUPPORTED_ABIS={"arm64-v8a"};')
                if name.endswith('QueueFixtures1967.java'):
                    if suite=='whole':continue
                    content=content.replace('static long captureEpoch1953(){return epoch;}','static long captureEpoch1953(){return epoch;}static long captureId1989(long epoch){return epoch;}')
                add(name,content)
            if suite=='queue':
                raw=(source/'GpuQualification1961.java').read_text()
                if mutant:
                    raw,count=re.subn(r'(public static boolean automaticAllowed19120\(\)\s*\{\s*return\s+)false',r'\g<1>true',raw)
                    if count!=1:raise AssertionError('automatic-policy negative control missing')
                add('com/hiro/ulike/GpuQualification1961.java',raw)
                files.append(source/'tests19120/QuietQueue19120Test.java');main='com.hiro.ulike.QuietQueue19120Test'
            else:
                constants=(source/'PipelineDetail1988.java').read_text().split('    static ProcessingTiming1947.Trace foreground',1)[0]
                add('com/hiro/ulike/PipelineDetail1988.java',constants+'}\n')
                add('com/hiro/ulike/WholePeers19120.java','package com.hiro.ulike; final class GpuQualification1961 { public static boolean automaticAllowed19120(){return '+('true' if mutant else 'false')+';} } final class GpuFinish1953 {static String fingerprint(){return "fixture-driver";}}')
                files.extend([source/'WholeRoute1952.java',source/'WholeRoute1953.java',source/'tests19120/QuietWhole19120Test.java']);main='com.hiro.ulike.QuietWhole19120Test'
            compiled=run([javac,'--release','8','-encoding','UTF-8','-d',classes,*files],label+'-compile')
            if compiled.returncode:raise RuntimeError(label+' compilation failed\n'+compiled.stdout+compiled.stderr)
            executed=run([java,'-ea','-Xmx256m','-cp',classes,main],label+'-run')
            if mutant:
                expected='quiet startup must not admit' if suite=='queue' else 'unqualified photo must not be copied'
                if executed.returncode==0 or expected not in executed.stdout+executed.stderr:raise AssertionError(label+' did not fail its intended regression check\n'+executed.stdout+executed.stderr)
                results[label]={'status':'expected_failure','regression':expected}
            else:
                if executed.returncode:raise RuntimeError(label+' failed\n'+executed.stdout+executed.stderr)
                results[label]=json.loads(executed.stdout.strip().splitlines()[-1])
    startup=load('quiet_startup_fixtures19120',source/'host_startup1990.py').SOURCES
    for mutant in [False,True]:
        label='startup'+('-automatic-enabled-control' if mutant else '-current')
        folder=work/label;fixture=folder/'sources';classes=folder/'classes'
        if folder.exists():shutil.rmtree(folder)
        fixture.mkdir(parents=True);classes.mkdir();files=[]
        for name,content in startup.items():
            if name.endswith('/GpuQualification1961.java'):
                content=content.replace('static String environment=', 'static boolean automaticAllowed19120(){return '+('true' if mutant else 'false')+';}static String environment=')
            if name.endswith('/GpuStrongDefault1990.java'):
                content=content.replace('static final String SUITE=', 'static int restores,optional; static class Adaptive1994{static boolean saved;static boolean cached(){return saved;}}static boolean restoreCached19120(GpuQualification1961.Cancellation c){restores++;return passed;}static final String SUITE=')
                content=content.replace('static void request1994(){}','static void request1994(){optional++;}')
            if name.endswith('/GpuModelDefault1990.java'):
                content=content.replace('static final String SUITE=', 'static int restores,optional;static boolean restoreCached19120(GpuQualification1961.Cancellation c){restores++;return passed;}static final String SUITE=')
                content=content.replace('static void requestOptional1994(){}','static void requestOptional1994(){optional++;}')
            if name.endswith('/GpuFinishDefault1990.java'):
                content=content.replace('static final String SUITE=', 'static int restores,optional;static boolean restoreCached19120(){restores++;return passed;}static final String SUITE=')
                content=content.replace('static void requestWide1995(){}','static void requestWide1995(){optional++;}')
            p=fixture/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content);files.append(p)
        files.extend([source/n for n in ['GpuStartup1990.java','GpuCertificates1990.java','CameraSession1965.java','tests19120/QuietStartup19120Test.java']])
        compiled=run([javac,'--release','8','-encoding','UTF-8','-d',classes,*files],label+'-compile')
        if compiled.returncode:raise RuntimeError(label+' compilation failed\n'+compiled.stdout+compiled.stderr)
        for scenario in (['cold'] if mutant else ['cold','cached','cached_adaptive','rejected','unavailable','ui']):
            executed=run([java,'-ea','-Xmx256m','-cp',classes,'com.hiro.ulike.QuietStartup19120Test',scenario],label+'-'+scenario)
            if mutant:
                expected='startup performs zero corpus comparisons'
                if executed.returncode==0 or expected not in executed.stdout+executed.stderr:raise AssertionError('startup negative control failed for an unintended reason\n'+executed.stdout+executed.stderr)
                results[label]={'status':'expected_failure','regression':expected}
            else:
                if executed.returncode:raise RuntimeError(label+' '+scenario+' failed\n'+executed.stdout+executed.stderr)
                results[label+'-'+scenario]=json.loads(executed.stdout.strip().splitlines()[-1])
    if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('tracked source changed during tests')
    report={'status':'passed','assertions':sum(v.get('assertions',0) for v in results.values()),'negative_controls':3,'tests':results,'physical_android_tested':False,'device_speedup_verified':False,'actual_qualification_engine':True,'actual_whole_route_engine':True,'actual_startup_engine':True,'source_sha256':pins}
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),indent=2))
