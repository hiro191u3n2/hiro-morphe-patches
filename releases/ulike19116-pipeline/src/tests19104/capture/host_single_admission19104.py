#!/usr/bin/env python3
"""Compare exact production .103/.104 admission with the same high resolution fixture."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,subprocess

def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();jdk=Path(jdk).resolve();work.mkdir(parents=True,exist_ok=True)
 prior=source/'tests19104/capture/reference103/BurstCapture1933.java'
 if hashlib.sha256(prior.read_bytes()).hexdigest()!='6c5d4022b531d2af049c5a798b49832124d5522505fec98171809d3b799eb4b7':raise AssertionError('Exact frozen published .103 source required for reproducer')
 old=module('admission104_fixture',source/'host_capture_save1947.py')
 extra=module('admission104_observers',source/'tests19103/capture/host_burst_arrival19103.py').EXTRA
 extra=extra.replace('public static void q0Entry19104(Object owner,android.media.Image image,CaptureResult result){}','public static int q0Entries; public static void q0Entry19104(Object owner,android.media.Image image,CaptureResult result){q0Entries++;}')
 cases={}
 for version,production in (('103',prior),('104',source/'BurstCapture1933.java')):
  out=work/version;classes=out/'classes';classes.mkdir(parents=True,exist_ok=True);files=[]
  timing=out/'fixtures/com/hiro/ulike/ProcessingTiming1947.java';timing.parent.mkdir(parents=True,exist_ok=True);timing.write_text(old.TIMING);files.append(timing)
  for p in sorted((source/'tests/capture1948-fixtures').rglob('*.java')):
   code=p.read_text()
   if p.name=='ShotContext1932.java':code=code.replace('public final class ShotContext1932 {','public final class ShotContext1932 {'+extra)
   if p.name=='Image.java':code=code.replace('public int closes;','public int closes,planeReads;').replace('public Plane[] getPlanes(){','public Plane[] getPlanes(){planeReads++;')
   if p.name=='FusionPixels1933.java':code=code.replace('public static int prepareFrameCalls','public static int analysisAllocations;\n    public static int prepareFrameCalls').replace('public static final class Analysis {','public static final class Analysis {public Analysis(){analysisAllocations++;}')
   if code!=p.read_text():
    q=out/'fixtures'/p.relative_to(source/'tests/capture1948-fixtures');q.parent.mkdir(parents=True,exist_ok=True);q.write_text(code);files.append(q)
   else:files.append(p)
  files += [production,source/'YuvPlanes1934.java',source/'NativeSpeed1935.java',source/'SpeedWorkers1935.java',source/'capture-stubs/com/hiro/ulike/CapturePolicy1933.java',source/'tests/Capture1948Test.java',source/'tests19104/capture/SingleAdmission19104Test.java']
  built=subprocess.run([str(jdk/'bin/javac'),'--release','8','-encoding','UTF-8','-d',str(classes),*map(str,files)],text=True,capture_output=True)
  (out/'compile.log').write_text(built.stdout+built.stderr)
  if built.returncode:raise RuntimeError(built.stderr)
  run=subprocess.run([str(jdk/'bin/java'),'-ea','-Xmx1g','-cp',str(classes),'com.hiro.ulike.SingleAdmission19104Test',version],text=True,capture_output=True,timeout=60)
  (out/'test.log').write_text(run.stdout+run.stderr)
  if run.returncode:raise RuntimeError(run.stdout+run.stderr)
  cases[version]=json.loads(run.stdout.split('RESULT ',1)[1].strip())
 assert cases['103']['baseline_reproduced'] and cases['104']['production_single_image_policy_tested']
 result=dict(status='passed',assertions=sum(c['assertions'] for c in cases.values()),cases=cases,baseline103_supplementary_admission_reproduced=True,single104_no_extra_request_copy_or_analysis=True,single104_original_q0_delivery_preserved=True,physical_android_tested=False,device_speedup_verified=False)
 (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk',required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),ensure_ascii=False))
