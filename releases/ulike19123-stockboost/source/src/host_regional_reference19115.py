#!/usr/bin/env python3
"""Actual Strong/resident reference owners, callback ABI, and owned-copy replay.

Readiness and GPU transport are controlled; actual resident producer/queue and
published callback constructors are executed/compiled independently of them.
No timing here establishes physical Android save latency.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, subprocess, time
ROOT=Path(__file__).resolve().parent
BASELINE_SHA='c3aaef4eb23a532bc0af2377266a4951fe39779533fce1d0d9cfb615f2618635'
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value
STRONG_MUTATIONS=[
 ('speed_reference','return regionalFaster19115()?key+":regional115":key;','return key;','old speed proof cannot certify new baseline'),
 ('outside_shape','if(testedShape19115(width,height))return','if(true)return','outside tested shapes preserve old speed proofs'),
 ('pending_owner','if(regionalState19115("pending"))return;','if(false)return;','regional pending prevents adaptive coqueue'),
 ('current_commit_proof','if(u.length>23&&u[23]!=0&&(u[23]!=1||profile!=PROFILE||!regionalAccepts19115(u)))return false;','','stage-like regional marker must recheck live helper state'),
 ('actual_baseline','if(profile==PROFILE&&regionalFaster19115())return regionalBaseline19115(f,cancellation);','if(false)return regionalBaseline19115(f,cancellation);','active regional profile4 uses explicit measured bridge'),
 ('complete_api_clock','((Long)measured[1]).longValue()','1L','bridge uses returned complete API time'),
]
RESIDENT_MUTATIONS=[
 ('actual_profile','boolean regional=profile==4&&','boolean regional=','nonregional adaptive profile does not borrow regional identity'),
 ('source_geometry','deployedRoutes19115(source.getWidth(),source.getHeight())+":"','deployedRoutes19115(width,height)+":"','tested input geometry records actual regional baseline'),
 ('shape_profile','return ((Integer)call.invoke(null,Integer.valueOf(width),Integer.valueOf(height))).intValue();','return GpuStrongDefault1990.profile1994();','resident profile lookup uses tested source shape'),
 ('preflight_recheck','preflight.finishRows19112!=rowsFromKey19112(base)||\n                        !routesFromKey19102(base).equals(deployedRoutes19115(source.getWidth(),source.getHeight()))','preflight.finishRows19112!=rowsFromKey19112(base)','foreground result rejects changed regional route'),
]
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve()
    if work.exists():raise AssertionError('Fresh regional reference directory required')
    work.mkdir(parents=True);jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();started=time.monotonic();fixture=source/'tests19115/reference'
    tracked=[source/'GpuStrongDefault1990.java',source/'GpuResident1976.java',source/'host_regional_reference19115.py',*fixture.rglob('*.java'),source/'host_resident_memory19108.py',source/'tests19108/diagnostics/ResidentMemory19108Test.java']
    pins={str(p.relative_to(source)):sha(p) for p in tracked};baseline=fixture/'baseline114/GpuResident1976.java'
    if sha(baseline)!=BASELINE_SHA:raise AssertionError('Published114 resident baseline changed')
    commands=[]
    def run(argv,name,expected=None):
        argv=list(map(str,argv));commands.append({'name':name,'argv':argv});p=subprocess.run(argv,text=True,capture_output=True,timeout=180);output=p.stdout+p.stderr;(work/(name+'.log')).write_text(output)
        if expected:
            if not p.returncode or 'java.lang.AssertionError' not in output or expected not in output:raise AssertionError('Mutation rejected at wrong layer '+name+'\n'+output[-10000:])
        elif p.returncode:raise AssertionError(name+'\n'+output[-10000:])
        return output
    def compile_group(target,producer,name):
        target.mkdir(parents=True)
        files=[producer,fixture/(name+'Peers19115.java'),fixture/(name+'19115Test.java')]
        if name=='ResidentReference':files.append(fixture/'android/graphics/Bitmap.java')
        run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',target,*files],target.name+'-compile')
    def execute(target,name,label,args=(),expected=None):return run([jdk/'bin/java','-ea','-Xmx512m','-cp',target,'com.hiro.ulike.'+name+'19115Test',*args],label,expected)
    strong=work/'strong';compile_group(strong,source/'GpuStrongDefault1990.java','StrongReference');strong_result=json.loads(execute(strong,'StrongReference','strong-reference'))
    resident=work/'resident';compile_group(resident,source/'GpuResident1976.java','ResidentReference');resident_result=json.loads(execute(resident,'ResidentReference','resident-reference'))
    old=work/'baseline114';compile_group(old,baseline,'ResidentReference');old_result=json.loads(execute(old,'ResidentReference','baseline-keys',('baseline',)))
    if resident_result['unchanged_keys']!=old_result['unchanged_keys']:raise AssertionError('Unchanged resident configurations no longer retain exact published114 keys')
    def descriptor(classes,label):
        text=run([jdk/'bin/javap','-classpath',classes,'-s','-p','com.hiro.ulike.GpuResident1976$1'],label)
        match=re.search(r'com\.hiro\.ulike\.GpuResident1976\$1\([^\n]*\);\s*descriptor: ([^\n]+)',text)
        if not match:raise AssertionError('Anonymous callback constructor not observed')
        return match.group(1).strip()
    current_descriptor=descriptor(resident,'resident-current-abi');old_descriptor=descriptor(old,'resident-baseline-abi')
    if current_descriptor!=old_descriptor:raise AssertionError('Published resident anonymous constructor ABI changed')
    mutations=[]
    for group,filename,controls in [('StrongReference','GpuStrongDefault1990.java',STRONG_MUTATIONS),('ResidentReference','GpuResident1976.java',RESIDENT_MUTATIONS)]:
        original=(source/filename).read_text()
        for name,anchor,replacement,expected in controls:
            if original.count(anchor)!=1:raise AssertionError('Mutation anchor ambiguous '+name)
            folder=work/'mutations'/name;folder.mkdir(parents=True);producer=folder/filename;producer.write_text(original.replace(anchor,replacement,1));classes=folder/('classes-'+name)
            compile_group(classes,producer,group);execute(classes,group,'mutation-'+name,expected=expected);mutations.append({'name':name,'rejected':True,'expected_assertion':expected})
    # Re-run the actual resident producer/comparator/queue with its existing
    # byte-backed Bitmap fixture, then recycle the caller BEFORE queued work.
    memory=module('regional115_actual_queue',source/'host_resident_memory19108.py').test(source,work/'actual-queue',jdk,ndk)
    if memory.get('status')!='passed' or memory.get('assertions',0)<500:raise AssertionError('Actual queue proof incomplete')
    raw=(source/'tests19108/diagnostics/ResidentMemory19108Test.java').read_text();anchor='String key=latestKey();field(GpuQualification1961.class,"lastCapture")'
    if raw.count(anchor)!=1:raise AssertionError('Owned-copy replay insertion ambiguous')
    replay=raw.replace('ResidentMemory19108Test','ResidentOwnedCopy19115Test').replace(anchor,'String key=latestKey();source.recycle();check(source.isRecycled(),"caller released before queued execution");field(GpuQualification1961.class,"lastCapture")',1)
    owned=work/'ResidentOwnedCopy19115Test.java';owned.write_text(replay);classes=work/'actual-queue/classes'
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',classes,'-d',classes,owned],'owned-copy-compile')
    output=run([jdk/'bin/java','-ea','-Xmx128m','-cp',classes,'com.hiro.ulike.ResidentOwnedCopy19115Test'],'owned-copy-queue-replay')
    owned_result=json.loads(next(line[7:] for line in output.splitlines() if line.startswith('RESULT ')))
    if owned_result.get('status')!='passed' or owned_result['assertions']!=memory['assertions']+8:raise AssertionError('Eight owned-copy queue cases not proved')
    if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Reference proof source changed during execution')
    result={'status':'passed','assertions':strong_result['assertions']+resident_result['assertions']+memory['assertions']+owned_result['assertions']+len(mutations)+2,'strong':strong_result,'resident':resident_result,'actual_queue':memory,'caller_recycled_queue':owned_result,'published_callback_constructor':old_descriptor,'current_callback_constructor':current_descriptor,'anonymous_callback_abi_preserved':True,'published_unaffected_keys_preserved':True,'mutation_controls':mutations,'mutation_count':len(mutations),'source_sha256':pins,'owned_copy_generated_source_sha256':sha(owned),'host_elapsed_seconds':round(time.monotonic()-started,3),'physical_android_tested':False,'device_speedup_verified':False,'scope':'Actual reference/key/producer/queue logic with controlled GPU peers; Java class ABI comparison against frozen published114. This is not a shader or device-performance test.'}
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
