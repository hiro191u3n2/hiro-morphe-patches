#!/usr/bin/env python3
"""Build deterministic BFCache runtime MPE; validate lost-callback state machine."""
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess,tempfile,zipfile
ROOT=Path(__file__).resolve().parent
ap=argparse.ArgumentParser()
ap.add_argument('--java',required=True)
ap.add_argument('--ecj',required=True)
ap.add_argument('--d8',required=True)
ap.add_argument('--output-dir',type=Path,default=ROOT)
a=ap.parse_args()
OUT=a.output_dir.resolve();OUT.mkdir(parents=True,exist_ok=True)
BUILD=Path(tempfile.mkdtemp(prefix='runtime_build_',dir=OUT))
STUBS=BUILD/'stubs';CLASSES=BUILD/'classes';DEX=BUILD/'dex';TEST=BUILD/'tests'
for p in (STUBS,CLASSES,DEX,TEST):p.mkdir()
def compile(sources,output,classpath=None):
 cmd=[a.java,'-jar',a.ecj,'-source','11','-target','11','-encoding','UTF-8','-d',str(output)]
 if classpath:cmd+=['-cp',str(classpath)]
 subprocess.run(cmd+list(map(str,sources)),check=True)
try:
 compile(sorted((ROOT/'runtime_stubs').rglob('*.java')),STUBS)
 compile(sorted((ROOT/'runtime_src').rglob('*.java')),CLASSES,STUBS)
 stub_jar=BUILD/'runtime_compile_only_stubs.jar'
 with zipfile.ZipFile(stub_jar,'w',zipfile.ZIP_DEFLATED) as z:
  for p in sorted(STUBS.rglob('*.class')):
   info=zipfile.ZipInfo(str(p.relative_to(STUBS)),(2026,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED
   z.writestr(info,p.read_bytes())
 runtime_classes=sorted(CLASSES.rglob('*.class'))
 subprocess.run([a.java,'-cp',a.d8,'com.android.tools.r8.D8','--release','--min-api','26','--classpath',str(stub_jar),'--output',str(DEX),*map(str,runtime_classes)],check=True)
 extension=OUT/'extensions/berry_bfcache_restore.mpe';extension.parent.mkdir(exist_ok=True)
 shutil.copyfile(DEX/'classes.dex',extension)
 compile(sorted((ROOT/'runtime_tests/src').rglob('*.java'))+sorted((ROOT/'runtime_src').rglob('*.java')),TEST)
 script_dir=OUT/'runtime_tests/generated';script_dir.mkdir(parents=True,exist_ok=True)
 jvm=subprocess.run([a.java,'-cp',str(TEST),'RestoreCoordinatorTest',str(script_dir)],check=True,capture_output=True,text=True)
 native=subprocess.run([a.java,'-cp',str(TEST),'NativeRestoreTrackerTest'],check=True,capture_output=True,text=True)
 native_integration=subprocess.run([a.java,'-cp',str(TEST),'RestoreNativeIntegrationTest'],check=True,capture_output=True,text=True)
 node=os.environ.get('CODEX_PRIMARY_RUNTIME_NODE') or shutil.which('node')
 if not node:raise RuntimeError('Node runtime unavailable')
 js=subprocess.run([node,str(ROOT/'runtime_tests/check_script.js'),str(script_dir)],check=True,capture_output=True,text=True)
 for n,result in [('jvm',jvm),('js',js),('native',native),('native_integration',native_integration)]:
  (OUT/f'runtime_tests/{n}_result.json').write_text(result.stdout)
 metadata={'result':'PASS','extension':'extensions/berry_bfcache_restore.mpe','extension_bytes':extension.stat().st_size,
  'extension_sha256':hashlib.sha256(extension.read_bytes()).hexdigest(),
  'runtime_classes':[str(p.relative_to(CLASSES)) for p in runtime_classes],
  'compile_only_android_stubs_packaged':False,
  'source_sha256':{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((ROOT/'runtime_src').rglob('*.java'))},
  'jvm_test':json.loads(jvm.stdout),'javascript_test':json.loads(js.stdout),
  'native_test':json.loads(native.stdout),'native_integration_test':json.loads(native_integration.stdout),
  'android_device_tested':False,'renderer_behavior_tested':False}
 (OUT/'RUNTIME_BUILD_QA.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n')
 published=OUT/'runtime_classes'
 if published.exists():shutil.rmtree(published)
 shutil.copytree(CLASSES,published)
 print(json.dumps(metadata,ensure_ascii=False,indent=2))
except subprocess.CalledProcessError as e:
 if e.stdout:print(e.stdout)
 if e.stderr:print(e.stderr)
 raise
finally:shutil.rmtree(BUILD)
