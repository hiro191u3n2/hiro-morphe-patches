"""Actual production pipeline differential and malformed-origin fallback regressions."""
from pathlib import Path
import hashlib,json,os,re,subprocess,sys

def _run(command,log):
    result=subprocess.run(list(map(str,command)),capture_output=True,text=True,timeout=180)
    Path(log).write_text(result.stdout+result.stderr)
    if result.returncode:raise RuntimeError(str(log)+'\n'+result.stdout[-3000:]+result.stderr[-9000:])
    found=re.search(r'^RESULT (\{[^\n]+\})$',result.stdout,re.M)
    return json.loads(found.group(1)) if found else None

def test(root,work,jdk=None,ndk=None):
    root=Path(root).resolve();work=Path(work).resolve()/'pipeline-audit1963';work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);sys.path.insert(0,str(root))
    from host_gpu1962 import all_sources
    reference=root.parents[1]/'ulike1962-gx24-gx29/src'
    if not (reference/'QualityPipeline1932.java').is_file():raise AssertionError('Independent published .62 source required')
    reports={};dumps={};pins={}
    for label,tree in [('published1962',reference),('candidate1963',root)]:
        classes=work/(label+'-classes');classes.mkdir(exist_ok=True)
        sources=[p for p in all_sources(tree) if p.name!='FastResize1933.java']+[tree/'FastResize1933.java']
        fixture=[p for p in sources if str(p).endswith('/android/graphics/Bitmap.java')]
        if len(fixture)!=1:raise AssertionError('Exactly one owned Bitmap fixture required')
        old='  check();bounds(out,offset,stride,x,y,w,h);reads++;'
        text=fixture[0].read_text()
        new='''  check();bounds(out,offset,stride,x,y,w,h);reads++;
  for(StackTraceElement call:new Throwable().getStackTrace())if(call.getClassName().equals("com.hiro.ulike.SpatialNoise1934")&&call.getMethodName().equals("probeCpu1961")){spatialProbeReads++;break;}'''
        if text.count(old)!=1:raise AssertionError('Bitmap read instrumentation anchor changed')
        text=text.replace(' public int reads,writes;',' public static int spatialProbeReads;\n public int reads,writes;').replace(old,new)
        instrumented=work/label/'android/graphics/Bitmap.java';instrumented.parent.mkdir(parents=True,exist_ok=True);instrumented.write_text(text)
        sources=[instrumented if p==fixture[0] else p for p in sources]
        sources+=[root/'tests1963/PipelineAudit1963Test.java']
        for p in sources:
            if p.is_relative_to(reference):pins[str(p.relative_to(reference))]=hashlib.sha256(p.read_bytes()).hexdigest()
        _run([jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-d',classes,*sources],work/(label+'-compile.log'))
        dumps[label]=work/(label+'.bin')
        reports[label]=_run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-Djava.library.path='+str(work/'unavailable'),'-cp',classes,'com.hiro.ulike.PipelineAudit1963Test',dumps[label]],work/(label+'-run.log'))
    if dumps['published1962'].read_bytes()!=dumps['candidate1963'].read_bytes():raise AssertionError('Production .62 face masks or normal pipeline pixels changed')
    before=reports['published1962']['spatial_probe_reads'];after=reports['candidate1963']['spatial_probe_reads']
    if before!=[3,2,3,3,3,3] or after!=[2,2,2,2,2,2]:raise AssertionError('Required exact probe consumer count differs: '+str((before,after)))
    classes=work/'candidate1963-classes'
    reports['invalid']=_run([jdk/'bin/java','-cp',classes,'com.hiro.ulike.PipelineAudit1963Test','invalid'],work/'invalid-run.log')
    reports['denied']=_run([jdk/'bin/java','-Djava.security.manager=allow','-cp',classes,'com.hiro.ulike.PipelineAudit1963Test','denied'],work/'denied-run.log')
    for name,sha in pins.items():
        if hashlib.sha256((reference/name).read_bytes()).hexdigest()!=sha:raise AssertionError('Published .62 source changed during audit')
    result={'status':'passed','assertions':sum(v['assertions'] for v in reports.values()),'normal_output_bytes_compared':dumps['candidate1963'].stat().st_size,'published1962_sha256':hashlib.sha256(dumps['published1962'].read_bytes()).hexdigest(),'face_cases':21,'pipeline_cases':6,'unused_spatial_probes_removed':sum(before)-sum(after),'joined_pre_moire_probe_retained':after[1]==before[1],'malformed_origin_rejected_before_commit':True,'optional_native_denial_preserves_cpu_fallback':True,'physical_android_tested':False,'reports':reports,'baseline_source_pins':pins}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[1]);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);a=p.parse_args();print(json.dumps(test(a.root,a.work,a.jdk),indent=2))
