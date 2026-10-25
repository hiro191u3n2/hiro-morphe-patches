#!/usr/bin/env python3
"""Exercise release/runtime version drift rejection; real DEX/APK checks run in build/apply."""
from pathlib import Path
import importlib.util,json,sys,os,subprocess,re,shutil,zipfile

def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 sys.path.insert(0,str(source))
 spec=importlib.util.spec_from_file_location('version_builder1997',source/'build19101.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder)
 assertions=0
 assert builder.VERSION=='1.9.118' and builder.BUNDLE_VERSION=='1.0.285';assertions+=1
 assert builder.runtime_source_version()=='1.9.118';assertions+=1
 fixture=work/'TimingVersion.java'
 good='public static final String VERSION = "1.9.118";'
 fixture.write_text(good);assert builder.runtime_source_version(fixture)=='1.9.118';assertions+=1
 for value in [*(good.replace('1.9.118','1.9.'+str(n)) for n in (92,*range(94,118))),good+'\n'+good,
               good.replace('public static final','static final'),good.replace('VERSION','OLD_VERSION')]:
  fixture.write_text(value)
  try:builder.runtime_source_version(fixture)
  except (AssertionError,RuntimeError):assertions+=1
  else:raise AssertionError('Stale/missing/ambiguous timing version was accepted')
 jdk=Path(jdk or os.environ.get('JAVA_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
 tools=Path(os.environ.get('ULIKE_TOOLS_DIR') or Path(os.environ.get('ULIKE_ANDROID_JAR',str(source.parent/'tools/android.jar'))).parent)
 classes=work/'classes';classes.mkdir(exist_ok=True)
 mpp=Path(os.environ.get('ULIKE_VERSION_TEST_MPP') or source.parent/'dist'/builder.SINGLE)
 with zipfile.ZipFile(mpp) as archive:
  name='app/hiro/ulike/patches/MethodContract.class'
  target=classes/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(archive.read(name))
 cp=str(tools/'morphe.jar')+os.pathsep+str(classes)
 def run(name,args):
  result=subprocess.run(list(map(str,args)),text=True,capture_output=True,timeout=60)
  (work/(name+'.log')).write_text(result.stdout+result.stderr)
  if result.returncode:raise AssertionError(name+' failed: '+result.stdout+result.stderr)
  return result.stdout
 run('compile',[jdk/'bin/javac','-encoding','UTF-8','-cp',cp,'-d',classes,source/'MergePayloads.java',source/'VerifyRuntimeVersion19101.java',source/'VerifyCaptureInvariant19104.java',source/'VerifyRegionalDisabled19115.java',source/'tests19103/version/RuntimeVersion19103Test.java',source/'tests19104/version/CaptureInvariant19104Test.java'])
 output=run('runtime-version-families',[jdk/'bin/java','-cp',str(classes)+os.pathsep+cp,'RuntimeVersion19103Test'])
 match=re.search(r'PASS diagnostic-version-family assertions=([0-9]+)',output)
 assert match and int(match.group(1))>=117
 assertions+=int(match.group(1))
 output=run('single-capture-invariant',[jdk/'bin/java','-cp',str(classes)+os.pathsep+cp,'CaptureInvariant19104Test'])
 match=re.search(r'PASS single-capture-invariant assertions=([0-9]+)',output)
 assert match and int(match.group(1))>=17
 assertions+=int(match.group(1))
 from regional_distribution19115 import verify,EXPECTED
 with zipfile.ZipFile(mpp) as archive:(work/'runtime.dex').write_bytes(archive.read('ulike/runtime.dex'))
 actual=json.loads(run('distributed-regional-disabled',[jdk/'bin/java','-cp',str(classes)+os.pathsep+cp,'VerifyRegionalDisabled19115',work/'runtime.dex']))
 verify(actual);assertions+=8
 for key,value in EXPECTED.items():
  for bad in (None,True,False,'',0):
   if type(bad) is type(value) and bad==value:continue
   try:verify({**actual,key:bad})
   except AssertionError:assertions+=1
   else:raise AssertionError('Malformed distributed DEX proof accepted: '+key)
 result={'status':'passed','distributed_regional_dex_absence_verified':True,'distributed_regional_dex':actual,'assertions':assertions,'physical_android_tested':False,'device_speedup_verified':False,
    'single_capture_dex_invariant_negative_controls':True,'nested_diagnostic_version_literals_verified':True,'legacy_timing_reader_exception_scoped':True,'runtime_source_version':'1.9.118','stale_92_94_95_96_97_98_99_versions_rejected':True,
    'scope':'Source version drift and single-shot DEX invariant rejection. Built DEX and actual applied APK are separately checked by VerifyRuntimeVersion19101 and VerifyCaptureInvariant19104.'}
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 import argparse
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work),indent=2))
