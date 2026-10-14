#!/usr/bin/env python3
"""Execute unchanged .60-.63 regressions and new GX30-GX36 actual host suites.

Every historical assertion and frozen oracle is retained. Compilation dependency
lists and the controlled certificate fixture include newly required production
fields without changing those assertions. The latest
published .63 pipeline oracle executes in a separate JVM using frozen source.
Host software GLES execution cannot substantiate Galaxy device speed claims.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re
import host_gpu1963 as inherited
ROOT=Path(__file__).resolve().parent
BASE_SHA='013e7e154f2a9912802ac6cfc702152bb34a1b759298b7f08e2acea5f6d50162'
FROZEN_SHA='e4edc11b87a3c27246634a4920feb8515a576d1162c539fd2c94defce05cb9e7'
GX_SUITES={
 'gx31_policy':'tests1964/policy_reuse1964.py',
 'gx32_gx33_shader':'tests1964/strong_tiles1964.py',
 'gx34_gx35_native':'tests1964/residual_transfer1964.py',
 'gx30_gx36_schedule':'tests1964/schedule1964_test.py',
 'gx35_direct_transfer':'tests1964/direct_transfer1964.py',
}
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def test(root,work,jdk=None,ndk=None):
 root,work=Path(root).resolve(),Path(work).resolve()
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
 # The old audit transforms run against their original .62 MPP, never the .63
 # package whose already-patched UI classes the new transform preserves.
 historical=Path(os.environ['ULIKE1963_BASELINE_MPP']).resolve()
 if historical.stat().st_size!=1153476 or sha(historical)!='e3db4401688863d56de30d6e6afdfc6dc8a941d42f7cc1cebec5700bd0f775cd':raise AssertionError('Historical audit UI input differs from exact published .62 MPP')
 morphe=Path(os.environ['ULIKE1963_MORPHE_JAR']).resolve()
 if sha(morphe)!='82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c':raise AssertionError('Historical audit UI toolchain differs from pinned Morphe')
 result=inherited.test(root,work,jdk,ndk)
 result['historical_ui_oracle']={'baseline_version':'1.9.62','baseline_mpp_sha256':sha(historical),'bytes':historical.stat().st_size,'morphe_sha256':sha(morphe)}
 reports=result['reports'];reference=root/'tests1964/published1963-reference'
 manifest=reference/'pins.json'
 if sha(manifest)!=FROZEN_SHA:raise AssertionError('Frozen .63 source oracle manifest changed')
 pins=json.loads(manifest.read_text())
 if pins['baseline_version']!='1.9.63' or pins['baseline_mpp_sha256']!=BASE_SHA:raise AssertionError('Wrong latest published .63 oracle')
 for name,expected in pins['files'].items():
  path=reference/name
  if path.stat().st_size!=expected['bytes'] or sha(path)!=expected['sha256']:raise AssertionError('Frozen .63 reference changed: '+name)
 classes=work/'published1963-pipeline-classes';classes.mkdir()
 sources=[reference/name for name in pins['pipeline_sources']]+[root/'tests1961/Pipeline1961Test.java',root/'tests1960/Native1960Test.java']
 inherited.inherited.run([jdk/'bin/javac','-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-d',classes,*sources],work/'published1963-compile.log')
 dump=work/'published1963-pipeline.bin'
 output=inherited.inherited.run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-cp',classes,'-Djava.library.path='+str(work/'unavailable'),'com.hiro.ulike.Pipeline1961Test',dump,'false'],work/'published1963-pipeline.log')
 found=re.search(r'^RESULT (\{[^\n]+\})$',output,re.M)
 if not found:raise AssertionError('Frozen .63 executed pipeline report absent')
 row=json.loads(found.group(1));candidate=work/'host-gpu1961/candidate1962_pipeline.bin'
 if dump.read_bytes()!=candidate.read_bytes():raise AssertionError('Saved pixels differ from immutable published .63')
 reports['published1963_pipeline']=row
 result['published1963_oracle']={'status':'passed','baseline_mpp_sha256':BASE_SHA,'source_manifest_sha256':FROZEN_SHA,'referenceNativeEnabled':False,'cases':row['cases'],'exact_output_sha256':sha(dump)}
 result['host_pixel_equivalence_to_published1963']=True
 for label,relative in GX_SUITES.items():
  path=root/relative
  if not path.is_file():raise AssertionError('Missing reviewed GX30-GX36 test suite: '+relative)
  spec=importlib.util.spec_from_file_location(label+'_1964_host',path);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
  report=module.test(root,work,jdk,ndk)
  if not isinstance(report,dict) or report.get('status')!='passed' or type(report.get('assertions')) is not int or report['assertions']<=0:raise AssertionError('Missing executed GX30-GX36 assertions: '+label)
  if report.get('physical_android_tested',report.get('physicalAndroidTested',False)) is not False:raise AssertionError('Host suite claims physical Android coverage: '+label)
  reports[label]=report
 for p in sorted((root/'tests1964').rglob('*')):
  if p.is_file() and '__pycache__' not in p.parts:result['sources'][p.relative_to(root).as_posix()]=sha(p)
 result['sources']['host_gpu1964.py']=sha(root/'host_gpu1964.py')
 result.update(schema='ulike-gx1964-executed-production-jni-v1',assertions=sum(row.get('assertions',0) for row in reports.values()),gx30_gx36_actual_host_coverage=True,physical_android_tested=False,device_speedup_verified=False,device_quality_improvement_verified=False)
 (work/'result1964.json').write_text(json.dumps(result,sort_keys=True,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--root',default=str(ROOT));p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.root,a.work,a.jdk,a.ndk),indent=2))
