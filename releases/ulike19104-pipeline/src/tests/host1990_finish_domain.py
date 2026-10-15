#!/usr/bin/env python3
"""Execute real startup finish corpus and admission against the compiled JNI graph.

Requires the current classes/native output of host1990_chain.py. Those retained
tests independently compare the very same finish graph with the frozen CPU
oracle; this test adds the real device-corpus lifecycle and sigma reuse path.
It neither replaces the GPU nor seeds positive certificates/timing records.
"""
from pathlib import Path
import argparse, json, os, re, subprocess

HARNESS=r'''
package com.hiro.ulike;
import android.content.Context;
import android.graphics.Bitmap;
public final class FinishDefault1990Test {
 static int assertions;
 static void check(boolean value,String label){assertions++;if(!value)throw new AssertionError(label);}
 static final GpuQualification1961.Cancellation ACTIVE=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
 static String key(Bitmap source,QualityPixels1932.Plan plan)throws Exception {
  java.lang.reflect.Method method=GpuChain1961.class.getDeclaredMethod("finishKey1976",Bitmap.class,int.class,int.class,int.class,QualityPixels1932.Plan.class,boolean.class);
  method.setAccessible(true);return (String)method.invoke(null,source,0,source.getWidth(),source.getHeight(),plan,false);
 }
 public static void main(String[] args)throws Exception {
  android.os.Build.FINGERPRINT="finish1990-real-jni-fixture";Context context=new Context();
  GpuQualification1961.initialize(context);GpuCertificates1990.initialize(context);
  check(!GpuFinishDefault1990.ready(),"no invented initial corpus evidence");
  boolean interrupted=false;try{GpuFinishDefault1990.qualify(new GpuQualification1961.Cancellation(){public boolean cancelled(){return true;}});}
  catch(java.util.concurrent.CancellationException expected){interrupted=true;}
  check(interrupted&&!GpuFinishDefault1990.ready()&&!GpuCertificates1990.rejected(GpuFinishDefault1990.SUITE),"cancel is not pass or mismatch");
  check(GpuFinishDefault1990.qualify(ACTIVE),"actual whole-graph corpus: "+GpuFinishDefault1990.summary());
  GpuCertificates1990.Evidence evidence=GpuCertificates1990.restore(GpuFinishDefault1990.SUITE);
  check(evidence!=null&&evidence.cases==28&&evidence.pixels>1000000&&evidence.cpuNanos>0&&evidence.gpuNanos>0,"real corpus evidence and timings");
  check(GpuFinishDefault1990.qualify(ACTIVE),"persisted finite evidence can be restored");
  GpuFinishDefault1990.Fixture f=GpuFinishDefault1990.fixture(0);Bitmap actual=null,expected=null;
  try {
   check(GpuFinishDefault1990.owns(f.source,0,f.source.getWidth(),f.source.getHeight(),f.plan),"production domain accepts ordinary policy");
   check(!GpuFinishDefault1990.owns(f.source,45,65,67,f.plan),"invalid rotation rejected");
   check(!GpuFinishDefault1990.owns(f.source,0,8193,67,f.plan),"dimension outside bounded domain rejected");
   QualityPixels1932.Plan custom=f.plan.withFaceRegions(new QualityPixels1932.RegionMask(){public int skinQ8(int x,int y){return 0;}public int detailQ8(int x,int y){return 0;}});
   check(!GpuFinishDefault1990.owns(f.source,0,65,67,custom),"unowned potentially stateful mask rejected");
   String original=key(f.source,f.plan);
   check(GpuQualification1961.restore(original)==null,"no fabricated photograph proof");
   for(float sigma:new float[]{.6012345f,3.812345f,17.4567f,31.9876f}){
    QualityPixels1932.Plan p=QualityPixels1932.plan(new QualityPixels1932.NoiseStats(sigma,sigma,128f,0f,64),800,16666666L,0,.35f,4,4,true,true,1f);
    String distinct=key(f.source,p);check(!distinct.equals(original),"historical sigma key still distinguishes input");
    expected=GpuChain1961.cpuFinish(f.source,0,65,67,p,false);
    actual=GpuFinishDefault1990.run(f.source,0,65,67,p,false,null,distinct);
    check(actual!=null&&GpuFinishDefault1990.difference(expected,actual)==null,"new sigma uses real CPU integer policy and exact GPU output");
    check(GpuQualification1961.restore(distinct)==null,"device admission does not fabricate per-photo records");
    expected.recycle();expected=null;actual.recycle();actual=null;
   }
   GpuQualification1961.rejectExact(original);
   check(GpuFinishDefault1990.run(f.source,0,65,67,f.plan,false,null,original)==null,"original exact mismatch remains authoritative");
   check(GpuFinishDefault1990.ready(),"photo rejection does not erase unrelated finite observations");
   GpuCertificates1990.reject(GpuFinishDefault1990.SUITE,"host-observed-test-revocation");
   check(!GpuFinishDefault1990.ready()&&!GpuFinishDefault1990.qualify(ACTIVE),"revoked corpus cannot be reused or overwritten");
  }finally{if(actual!=null)actual.recycle();if(expected!=null)expected.recycle();f.source.recycle();}
  check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"all real GPU resources and reservations retired");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"corpusCases\":"+evidence.cases+",\"exactPixels\":"+evidence.pixels+",\"corpusCpuNanos\":"+evidence.cpuNanos+",\"corpusGpuNanos\":"+evidence.gpuNanos+"}");
 }
}
'''

def test(source,work,classes,native,jdk):
    source,work,classes,native,jdk=map(lambda p:Path(p).resolve(),(source,work,classes,native,jdk))
    work.mkdir(parents=True,exist_ok=True)
    harness=work/'FinishDefault1990Test.java';harness.write_text(HARNESS)
    compiled=work/'classes';compiled.mkdir(exist_ok=True)
    def run(command,label,env=None):
        result=subprocess.run(list(map(str,command)),capture_output=True,text=True,timeout=240,env=env)
        output=result.stdout+result.stderr;(work/(label+'.log')).write_text(output)
        if result.returncode or 'WARNING in native method' in output:raise AssertionError(label+'\n'+output[-12000:])
        return output
    run([jdk/'bin/javac','--release','8','-cp',classes,'-d',compiled,harness],'compile')
    output=run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-Xcheck:jni','-Djava.library.path='+str(native),'-cp',str(compiled)+os.pathsep+str(classes),'com.hiro.ulike.FinishDefault1990Test'],
        'actual-jni',dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1'))
    match=re.search(r'^RESULT (\{[^\n]+\})$',output,re.M)
    if not match:raise AssertionError('Missing actual JNI result')
    result=json.loads(match.group(1));result.update(actual_finish_device_corpus=True,physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    p=argparse.ArgumentParser()
    for key in ('source','work','classes','native','jdk'):p.add_argument('--'+key,required=True)
    a=p.parse_args();print(json.dumps(test(a.source,a.work,a.classes,a.native,a.jdk),indent=2))
