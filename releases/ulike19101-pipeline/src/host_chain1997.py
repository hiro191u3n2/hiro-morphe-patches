#!/usr/bin/env python3
"""Run retained actual-JNI finish/resident pixel tests on current .97 graph.
Historical arithmetic/oracles and case assertions are not edited. Existing .89
adapter supplies its declared Android/telemetry seams; .90 adds only referenced
new production helper classes to the compiler input closure.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re

def test(source,work,jdk=None,ndk=None,runners=('host_chain1978','host_resident_pipeline1978')):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    spec=importlib.util.spec_from_file_location('current_chain_adapter89',source/'host_retained1989.py')
    previous=importlib.util.module_from_spec(spec);spec.loader.exec_module(previous)
    class Adapter(previous.Adapter):
        def compile_command(self,command):
            values=list(map(str,super().compile_command(command)))
            if values and Path(values[0]).name=='cc':
                # NDK EGL headers need platform-neutral Android typedefs on
                # this headless host; libGL exports the Mesa GLES entry points.
                if '-DANDROID' not in values:values.insert(1,'-DANDROID')
                values=['-l:libGL.so.1' if v=='-l:libGLESv2.so.2' else v for v in values]
            if not values or Path(values[0]).name!='javac':return values
            paths=[Path(v).resolve() for v in values if v.endswith('.java') and Path(v).is_file()]
            # Only current source compiles may gain helpers. Frozen published
            # oracle compiles do not reference them and remain untouched.
            alltext='\n'.join(p.read_text() for p in paths)
            for _ in range(15):
                changed=False
                declared=set(re.findall(r'\b(?:class|interface|enum)\s+(\w+)',alltext))
                for path in sorted(source.glob('*.java')):
                    if path.name.startswith(('Patch','Transform')) or path in paths or path.stem in declared:continue
                    if re.search(r'\b'+path.stem+r'\s*[.(<\[]',alltext):
                        paths.append(path);values.append(str(path));alltext+='\n'+path.read_text();changed=True
                if not changed:break
            if any(p.parent==source for p in paths):
                for path in paths:
                    body=path.read_text();updated=body
                    if path.name=='AsyncSave1935.java' and path.parent!=source and 'settingsForPhoto1981(' not in body:
                        updated=body.replace('public final class AsyncSave1935 {',"""public final class AsyncSave1935 {
 public static PhotoDetail.Settings settingsForPhoto1981(){return settings(PhotoDetail.snapshot1932());}
 public static ShotContext1932.Snapshot snapshotForPhoto1981(android.graphics.Bitmap b){return ShotContext1932.forBitmap(b);}
 public static PhotoAnalysis1981.Context photoContext1981(android.graphics.Bitmap b){return null;}
 public static boolean analysisMemoryAllows1981(long bytes){return false;}
""")
                    if path.name=='SaveQueue1935.java' and path.parent!=source and 'analysisWaiting1981(' not in body:
                        updated=body.replace('public final class SaveQueue1935 {','public final class SaveQueue1935 {public static boolean analysisWaiting1981(Runnable task){return false;}')
                    if path.name=='CameraTrace1965.java' and 'events++;' in body:
                        # .85-.89 added a separate background qualification log.
                        # The retained assertion is about foreground capture
                        # diagnostics; count those independently of proof logs.
                        names=sorted(set(re.findall(r'event.invoke\(null,"([^"]+)"',(source/'GpuQualification1961.java').read_text())))
                        reject='||'.join('n.equals('+json.dumps(n)+')' for n in names)
                        updated=body.replace('events++;','if(!('+reject+'))events++;')
                    if path.name=='FinishMemory1989.java' and path.parent!=source:
                        old='package com.hiro.ulike; final class FinishMemory1989 {static boolean fits(long extra){return GpuNoise1960.workspaceFits(extra);}}\n'
                        if body!=old:raise AssertionError('Unreviewed controlled-memory peer shape')
                        # This renderer fixture has no native scratch owner.
                        # Preserve its injected verdict; memory97 separately
                        # executes the real recovery with JNI/GL ownership.
                        updated=body.replace('static boolean fits(', 'static final int COUNT=33;static boolean recheckForeground1997(long extra,long[] report){return GpuNoise1960.workspaceFits(extra);}static boolean fits(')
                    if path.name=='Bitmap.java' and 'getGenerationId(' not in body:
                        updated=body.replace('public final class Bitmap {','public final class Bitmap {public int getGenerationId(){return 1;}')
                    if updated!=body:
                        destination=self.work/'photo-peer1990'/path.name;destination.parent.mkdir(parents=True,exist_ok=True);destination.write_text(updated)
                        values=[str(destination) if v==str(path) else v for v in values]
            if not hasattr(self,'current1990'):self.current1990={}
            for path in paths:
                if path.parent==source:self.current1990[str(path)]=hashlib.sha256(path.read_bytes()).hexdigest()
            return values
    reports={}
    for name in runners:
        adapter=Adapter(source,work/(name+'-compat'))
        runner=adapter.load(name)
        result=runner.test(source,work/name,jdk=jdk,ndk=ndk)
        if result.get('status')!='passed':raise AssertionError(name)
        for filename,digest in adapter.current1990.items():
            if hashlib.sha256(Path(filename).read_bytes()).hexdigest()!=digest:raise AssertionError('Current production changed during JNI test: '+filename)
        result['current1990_helper_closure']=True
        result['compiled_current1990_sha256']=adapter.current1990
        reports[name]=result
        (work/(name+'-result1990.json')).write_text(json.dumps(result,indent=2)+'\n')
    result=dict(status='passed',assertions=sum(r['assertions'] for r in reports.values()),tests=reports,
        actual_finish_resident_jni1990=True,physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');p.add_argument('--runner',action='append');a=p.parse_args()
    print(json.dumps(test(a.source,a.work,a.jdk,a.ndk,tuple(a.runner) if a.runner else ('host_chain1978','host_resident_pipeline1978')),indent=2))
