#!/usr/bin/env python3
"""Execute current Burst coordinator lifecycle tests and new observer wiring tests."""
from pathlib import Path
import argparse,importlib.util,json,re,subprocess

EXTRA='''
 public static int arrivals,results; public static Object lastOwner;public static android.media.Image lastImage;public static CaptureResult lastResult; public static long lastStart,lastEnd;
 public static Object cameraOwner19103(Object owner){return owner;}
 public static void acquired19103(Object owner,Object token,android.media.Image image,long started,long observed){if(image==null||token==null)return;arrivals++;lastOwner=owner;lastImage=image;lastStart=started;lastEnd=observed;}
 public static void resultObserved19103(Object owner,Object token,CaptureResult result,long observed){if(result==null||token==null)return;results++;lastOwner=owner;lastResult=result;}
'''
def run(source,work,jdk):
 source=Path(source).resolve();work=Path(work).resolve();jdk=Path(jdk).resolve();work.mkdir(parents=True,exist_ok=True);classes=work/'classes';classes.mkdir(exist_ok=True)
 spec=importlib.util.spec_from_file_location('arrival19103_capture_fixture',source/'host_capture_save1947.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
 timing=work/'fixtures/com/hiro/ulike/ProcessingTiming1947.java';timing.parent.mkdir(parents=True,exist_ok=True);timing.write_text(module.TIMING)
 files=[timing]
 for p in sorted((source/'tests/capture1948-fixtures').rglob('*.java')):
  if p.name=='ShotContext1932.java':
   adapted=work/'fixtures/com/hiro/ulike/ShotContext1932.java';adapted.write_text(p.read_text().replace('public final class ShotContext1932 {','public final class ShotContext1932 {'+EXTRA));files.append(adapted)
  else:files.append(p)
 def production(name):
  for p in (source/(name+'.java'),source/'quality-dependencies/com/hiro/ulike'/(name+'.java'),source/'capture-stubs/com/hiro/ulike'/(name+'.java')):
   if p.exists():return p
  raise RuntimeError('Missing production source '+name)
 files += [production(name) for name in ('BurstCapture1933','CapturePolicy1933','YuvPlanes1934','NativeSpeed1935','SpeedWorkers1935')]
 files += [source/'tests/Capture1948Test.java',source/'tests19103/capture/BurstArrivalHook19103Test.java']
 command=[str(jdk/'bin/javac'),'--release','8','-encoding','UTF-8','-d',str(classes),*map(str,files)]
 built=subprocess.run(command,capture_output=True,text=True);(work/'compile.log').write_text(built.stdout+built.stderr)
 if built.returncode:raise RuntimeError(built.stderr)
 evidence={}
 for name in ('Capture1948Test','BurstArrivalHook19103Test'):
  test=subprocess.run([str(jdk/'bin/java'),'-Xmx1g','-ea','-cp',str(classes),'com.hiro.ulike.'+name],capture_output=True,text=True,timeout=180);(work/(name+'.log')).write_text(test.stdout+test.stderr)
  if test.returncode:raise RuntimeError(test.stdout+test.stderr)
  if name=='Capture1948Test':
   match=re.fullmatch(r'CAPTURE_1948_PASS assertions=(\d+) scenarios=(\d+)',test.stdout.strip());assert match,test.stdout;evidence['retained_assertions']=int(match[1]);evidence['retained_scenarios']=int(match[2])
  else:evidence.update(json.loads(test.stdout.split('RESULT ',1)[1].strip()))
 evidence['new_assertions']=evidence['assertions'];evidence['assertions']+=evidence['retained_assertions'];evidence['physical_android_tested']=False;evidence['device_speedup_verified']=False;(work/'result.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n');return evidence
def test(source,work,jdk=None,ndk=None):
 return run(source,work,jdk)

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk',required=True);a=p.parse_args();print(json.dumps(run(a.source,a.work,a.jdk),ensure_ascii=False))
