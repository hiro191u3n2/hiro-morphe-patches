#!/usr/bin/env python3
"""Execute current-source NR preservation and all H28-H33 focused regressions."""
from pathlib import Path
import importlib.util,json,os,hashlib

def module(root,name):
 spec=importlib.util.spec_from_file_location(name,Path(root)/(name+'.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def test(root,work,tools,inputs):
 root,work,tools,inputs=map(Path,(root,work,tools,inputs));reports={}
 jdk=Path(os.environ['ULIKE_JDK_HOME']);ndk=Path(os.environ['ULIKE_NDK_HOME'])
 tasks=[('nr1_nr4_current_source_preserved',lambda:module(root,'host_nr1955').test(root,work,inputs,tools)),
 ('h28_h29_exact_cpu',lambda:module(root,'host_h28h29_1956').test(root,work,tools/'android.jar')),
 ('h30_exact_residual',lambda:module(root,'host_h30_1956').test(root,work,jdk=jdk,ndk=ndk)),
 ('h31_persistent_dispatch',lambda:module(root,'host_h31_1956').test(root,work)),
 ('h32_native_scratch',lambda:module(root,'host_h32_1956').test(root,work,jdk=jdk,ndk=ndk)),
 ('h33_encode_publication_overlap',lambda:module(root,'host_h33_1956').test(root,work)),
 ('native_scratch_memory_budget',lambda:module(root,'host_memory1956').run(work,inputs,tools))]
 for name,call in tasks:
  print('Executing '+name,flush=True);result=call()
  if not isinstance(result,dict) or not(result.get('status')=='passed' or result.get('passed') is True) or type(result.get('assertions')) is not int or result['assertions']<=0:raise AssertionError('Missing actual host assertions '+name)
  reports[name]=result
 result={'schema':'ulike1956-host-regression-v1','status':'passed','selected':['H28','H29','H30','H31','H32','H33'],'assertions':sum(r['assertions'] for r in reports.values()),'reports':reports,'physical_android_tested':False,'device_speedup_verified':False,'device_quality_improvement_verified':False,'arm_neon_required':os.environ.get('ULIKE_REQUIRE_ARM_NEON')=='1','nr1_nr4_current_source_checked':True}
 (work/'host-regression1956-result.json').write_text(json.dumps(result,indent=2,sort_keys=True)+'\n');return result
