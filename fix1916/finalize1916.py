#!/usr/bin/env python3
"""Assemble reproducible trial artifacts only after current build and APK gates pass."""
import argparse,hashlib,json,subprocess,zipfile
from pathlib import Path
R=Path(__file__).resolve().parents[1]
def sha(b):return hashlib.sha256(b).hexdigest()
def load(p):return json.loads(p.read_text())
def js(x):return (json.dumps(x,ensure_ascii=False,indent=2)+'\n').encode()
def main():
 ap=argparse.ArgumentParser()
 for k in ('results','host','base'):ap.add_argument('--'+k,type=Path,required=True)
 a=ap.parse_args();dist=a.results/'dist';source=load(R/'fix1916/source-input.json');graph=load(R/'fix1916/local-graph-qa.json');apk=load(R/'fix1916/apk-qa.json')
 for n,h in source['pins'].items():assert sha((R/n).read_bytes())==h,n
 assert source['source_only_zip_sha256']=='fa68c07c8bc20c99d1f58f70d17850a6028f760b94f0e0dbefebb5d6e84c894b'
 files=list((R/'implementation191/native_precision/native').glob('*'))+[R/'fix1916'/n for n in ('private_audit1916.py','graph_audit1916.cpp','fixture1916.h','graph-pins.json','inventory1916.py','assets1916.py','prepare_graphs1916.py')]
 pins={str(p.relative_to(R)):sha(p.read_bytes()) for p in files if p.is_file()}
 assert sha(json.dumps(pins,sort_keys=True,separators=(',',':')).encode())==graph['native_and_harness_source_map_sha256']
 assert graph['input_source_only_zip_sha256']==source['source_only_zip_sha256']
 assert graph['result']=='PASS_CONTROLLED_ORIGINAL_GLSL_REPLAY' and graph['failures']==0
 assert (graph['controlled_pairs'],graph['controlled_chain_cases'],graph['checked_chain_intermediates'],graph['large_nonzero_cases'])==(154,33,252,6)
 assert (graph['legacy_skin_positives'],graph['legacy_skin_rejections'],graph['legacy_eye_positives'],graph['legacy_eye_rejections'])==(45,32,17,22)
 host=load(a.host/'HOST_RESULT1916.json');assert host['status']=='PASS_HOST_ONLY'
 required={'host','cache198','blit199','upload1910','phase1911','state1912','sampler1913','mask1914','skin1915','graph1916','renderer','diagnostic-compile','diagnostic','runtime','runtime-independent'}
 assert set(host['tests'])==required
 for name,row in host['tests'].items():assert row['exit_code']==0 and sha((a.host/(name+'.log')).read_bytes())==row['sha256']
 build=load(dist/'BUILD1916.json');local=load(a.results/'LOCAL_BUILD1916.json')
 assert build['result']=='PASS_SOURCE_BUILT_ARM64_AND_SCOPED_MPP' and local['result']=='LOCAL_TRIAL_MPP_BUILD_ONLY'
 assert build['version']=='1.9.16' and build['bundle_version']=='1.0.140'
 assert build['other_application_payloads_unchanged'] and build['photographic_input_encoder_models_and_sdk_queue_unchanged']
 assert (a.results/'libhiro_precision191.so').read_bytes()==(a.results/'independent.so').read_bytes()
 assert (a.results/'runtime/dex/classes.dex').read_bytes()==(a.results/'runtime-independent/dex/classes.dex').read_bytes()
 assert apk['schema']=='ulike1916-original-apks-apply-1' and apk['result']=='PASS_BOTH_MPP_VARIANTS_ORIGINAL_APKS_APPLICATION'
 assert apk['original_apks_sha256']=='73c6d3a3008b9975645f63238f07dc9c1960ad982c70f141ee4b5dfe60f7a293'
 assert apk['original_apk_apply_tested'] and not apk['android_device_tested']
 for n,h in apk['source_sha256'].items():assert sha((R/n).read_bytes())==h,n
 assert len(build['artifacts'])==2 and {r['kind'] for r in apk['variants']}=={'standalone','integrated'}
 for row in build['artifacts']:
  raw=(dist/row['file']).read_bytes();assert sha(raw)==row['sha256'] and len(raw)==row['bytes']
  assert raw==(a.results/'dist-independent'/row['file']).read_bytes()
  assert apk['artifacts'][row['file']]=={'sha256':row['sha256'],'bytes':row['bytes']}
  assert row['loader_inverse_semantics_verified'] and row['runtime_source_merge_and_non_target_preservation_verified'] and row['emitted_installer_executed']
 for v in apk['variants']:
  assert v['result']=='PASS_ORIGINAL_APKS_APPLICATION' and v['native_sha256']==build['native_sha256']
  d=v['dex'];assert d['result']=='PASS' and d['diagnostic_marker']=='SDK診断 v1.9.16'
  assert d['full_helper_linkage_checked'] and d['strict_still_frame_zero_status_preserved']
  assert d['illegal_private_member_accesses']==0 and d['illegal_package_or_protected_member_accesses']==0
  assert v['arm64_only_verified'] and v['resource_and_original_vendor_native_contracts_verified']
 with zipfile.ZipFile(a.base/'ULike_HQ_Texture_Online_v1.9.15.mpp') as z:old=z.read('ulike191/runtime/0000.bin')
 oldso=a.results/'baseline-precision.so';oldso.write_bytes(old)
 def symbols(path):
  lines=subprocess.run(['readelf','--dyn-syms','--wide',str(path)],capture_output=True,text=True,check=True).stdout.splitlines();out={}
  for line in lines:
   c=line.split()
   if len(c)>=8 and c[4] in ('GLOBAL','WEAK') and c[6] not in ('UND','ABS'):out[c[7]]=[c[3],c[4],c[5]]
  return out
 before=symbols(oldso);after=symbols(a.results/'libhiro_precision191.so');assert before and before==after
 qa={'schema':'ulike1916-source-build-release-1','version':'1.9.16','bundle_version':'1.0.140','result':'PASS_EXACT_SOURCE_HOST_GPU_INDEPENDENT_BUILD_AND_ORIGINAL_APK_APPLICATION','trial':True,'status':'READY_FOR_VERIFIED_TRIAL_PUBLICATION','latest_source_input':source,'native_and_harness_source_sha256':pins,'host_regressions':host,'controlled_original_shader_tests':graph,'build':build,'artifacts':build['artifacts'],'original_apk_application':apk,'android_arm64_native_built':True,'mpp_built':True,'native_independent_rebuild_byte_identical':True,'java_independent_rebuild_byte_identical':True,'mpp_independent_rebuild_byte_identical':True,'native_exported_abi_preserved':True,'native_exports':after,'full_original_apk_apply_tested':True,'android_device_tested':False,'physical_original_sdk_execution_verified':False,'capture_success_proven':False,'no_8bit_photo_fallback_added':True,'limits':graph['limits']+['Publication completion is separately recorded by publication-graph1916.json; build evidence is not a device capture success.']}
 qa_bytes=js(qa);(dist/'QA_ULike_v1.9.16.json').write_bytes(qa_bytes)
 # Include only already released authored files, exact new authored sources and sanitized evidence.
 with zipfile.ZipFile(a.base/'ULike_v1.9.15_sources_and_QA.zip') as z:
  names={n for n in z.namelist() if not n.endswith('/') and n!='SOURCE_ARCHIVE_MANIFEST.json'}
  factory=z.read('implementation191/integration/deltas/factory-delta.dex')
  assert len(factory)==884 and sha(factory)=='50180394aa4f1f06297e471d56fdf7d4957c4685f2cc59497be57e3913fefbbf'
 names.update(source['pins']);names.update('fix1916/'+n for n in ('source-input.json','local-graph-qa.json','apk-qa.json','finalize1916.py','publish1916.py','RELEASE_NOTES.txt','SUMMARY.txt'))
 exported={n:(R/n).read_bytes() for n in names}
 exported['fix1916/evidence/QA_ULike_v1.9.16.json']=qa_bytes
 exported['fix1916/evidence/BUILD1916.json']=js(build)
 for folder,label in ((a.results/'logs','build'),(a.host,'host')):
  for p in folder.iterdir():
   if p.is_file() and p.suffix in ('.log','.json'):exported['fix1916/evidence/'+label+'/'+p.name]=p.read_bytes()
 for n,b in exported.items():
  p=Path(n);assert not p.is_absolute() and '..' not in p.parts and p.suffix not in ('.apk','.apks','.so','.jar','.mpp','.png','.jpg','.frag','.vert','.glsl')
  if p.suffix=='.dex':assert n=='implementation191/integration/deltas/factory-delta.dex' and b==factory
 manifest={'schema':'ulike1916-source-manifest-1','files':{n:{'bytes':len(b),'sha256':sha(b)} for n,b in sorted(exported.items())}}
 exported['SOURCE_ARCHIVE_MANIFEST.json']=js(manifest)
 archive=dist/'ULike_v1.9.16_sources_and_QA.zip'
 with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
  for n,b in sorted(exported.items()):
   i=zipfile.ZipInfo(n,(2026,10,5,0,0,0));i.compress_type=zipfile.ZIP_DEFLATED;i.external_attr=0o100644<<16;z.writestr(i,b,compresslevel=9)
 with zipfile.ZipFile(archive) as z:assert z.testzip() is None and all(z.read(n)==b for n,b in exported.items())
 print('PASS_LATEST_SOURCE_AND_BOTH_APPLIED_MPPS_RELEASE_GATE',flush=True)
if __name__=='__main__':main()
