#!/usr/bin/env python3
"""Host protocol tests, SDK36 compile, exact caller-owned stock/API/archive facts.
Does not run an Android device or publish vendor code/materials.
"""
import argparse,collections,hashlib,importlib.util,json,os,pathlib,subprocess,sys,tempfile,zipfile
ROOT=pathlib.Path(__file__).resolve().parent
WORK=ROOT.parents[2]
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def run(args):
 p=subprocess.run([str(a) for a in args],text=True,capture_output=True)
 if p.returncode:raise RuntimeError(p.stdout+"\n"+p.stderr)
 return p.stdout.strip()
APIS={
 'Lcom/ss/android/medialib/RecordInvoker;':{
  'setComposerMode':'(II)I','setComposerResourcePath':'(Ljava/lang/String;)I',
  'setComposerNodes':'([Ljava/lang/String;I)I','appendComposerNodes':'([Ljava/lang/String;I)I',
  'removeComposerNodes':'([Ljava/lang/String;I)I','reloadComposerNodes':'([Ljava/lang/String;I)I',
  'replaceComposerNodes':'([Ljava/lang/String;I[Ljava/lang/String;I)I',
  'updateComposerNode':'(Ljava/lang/String;Ljava/lang/String;F)I',
  'updateMultiComposerNodes':'(I[Ljava/lang/String;[Ljava/lang/String;[F)I',
  'setVEEffectParams':'(Lcom/ss/android/vesdk/VEEffectParams;)I'},
 'Lcom/ss/android/vesdk/VEEffectParams;':{'<init>':'()V'},
 'Lcom/ss/android/vesdk/VERecorder;':{
  'setComposerNodesWithTag':'([Ljava/lang/String;I[Ljava/lang/String;)I',
  'appendComposerNodesWithTag':'([Ljava/lang/String;I[Ljava/lang/String;)I',
  'reloadComposerNodesWithTag':'([Ljava/lang/String;I[Ljava/lang/String;)I',
  'replaceComposerNodesWithTag':'([Ljava/lang/String;I[Ljava/lang/String;I[Ljava/lang/String;)I'}
}
CONSTANTS={'EFFECT_TYPE_SET_COMPOSER_WITH_TAG':0,'EFFECT_TYPE_APPEND_COMPOSER_WITH_TAG':2,'EFFECT_TYPE_RELOAD_COMPOSER_WITH_TAG':1,'EFFECT_TYPE_REPLACE_COMPOSER_WITH_TAG':3}
FIELDS={'TYPE':'I','intValueOne':'I','intValueTwo':'I','stringArrayOne':'Ljava/util/ArrayList;','stringArrayTwo':'Ljava/util/ArrayList;','stringArrayThree':'Ljava/util/ArrayList;'}
FIELDS.update({'boolArrayValue':'Ljava/util/ArrayList;','boolValueOne':'Z','boolValueTwo':'Z','boolValueThree':'Z',
 'floatArrayValue':'Ljava/util/ArrayList;','floatValueOne':'F','floatValueTwo':'F','floatValueThree':'F',
 'intArrayValue':'Ljava/util/ArrayList;','intValueThree':'I','stringValueOne':'Ljava/lang/String;','stringValueTwo':'Ljava/lang/String;','stringValueThree':'Ljava/lang/String;'})
ARCHIVES={'natural':('ULike_Natural_blush_1790815043265.zip','5b50343dcedc7e9aadd218626e621ad683cd67372d225024f118b47f493546f0'),'purity':('ULike_Purity2_1790815028634.zip','cd5da20fefe1f9bd594e4e0d055d6320392fa54fb5df9d34b8f159c791c5b117')}
def stock(dex_root):
 from loguru import logger
 logger.disable('androguard')
 from androguard.core.dex import DEX
 spec=importlib.util.spec_from_file_location('face_contract',ROOT.parent/'android_face_backend/verify_stock_contract.py');mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
 methods={};fields={};constants={};pins={};routes={}
 for name in ['classes.dex','classes3.dex']:
  path=dex_root/name;assert sha(path)==mod.DEX_PINS[name];pins[name]=sha(path)
  for c in DEX(path.read_bytes()).get_classes():
   cname=c.get_name()
   if cname in APIS:
    for m in c.get_methods():
     if APIS[cname].get(m.get_name())==m.get_descriptor().replace(' ',''):
      assert m.get_access_flags()&1
      key=cname+'->'+m.get_name()+m.get_descriptor().replace(' ','')
      methods[key]=hashlib.sha256(m.get_code().get_raw()).hexdigest()
      if cname.endswith('/VERecorder;'):
       outputs=[i.get_output() for i in m.get_instructions()]
       assert any('setVEEffectParams' in x for x in outputs)
       required={'setComposerNodesWithTag':'SET','appendComposerNodesWithTag':'APPEND','reloadComposerNodesWithTag':'RELOAD','replaceComposerNodesWithTag':'REPLACE'}[m.get_name()]
       assert any('EFFECT_TYPE_'+required+'_COMPOSER_WITH_TAG' in x for x in outputs)
       for field in ['intValueOne','stringArrayOne','stringArrayTwo']+(['intValueTwo','stringArrayThree'] if required=='REPLACE' else []):assert any('->'+field+' ' in x or '->'+field+':' in x for x in outputs),(field,outputs)
       routes[m.get_name()]={'public_generic_params_route':True,'tag_arrays_distinct':True,'opcode_constant':CONSTANTS['EFFECT_TYPE_'+required+'_COMPOSER_WITH_TAG']}
   if cname=='Lcom/ss/android/vesdk/VEEffectParams;':
    for f in c.get_fields():
     if f.get_name() in FIELDS:
      assert f.get_descriptor()==FIELDS[f.get_name()] and f.get_access_flags()&1
      fields[f.get_name()]=f.get_descriptor()
     if f.get_name() in CONSTANTS:
      assert f.get_descriptor()=='I' and f.get_access_flags()&1 and f.get_access_flags()&8
      assert f.get_init_value().get_value()==CONSTANTS[f.get_name()]
      constants[f.get_name()]=f.get_init_value().get_value()
 assert len(methods)==sum(map(len,APIS.values())) and fields==FIELDS and constants==CONSTANTS and len(routes)==4
 return {'dex_sha256':pins,'public_method_code_sha256':methods,'public_fields':fields,'public_mutable_static_constants':constants,'tagged_routes':routes}
def archives(upload,temp):
 reports={};java=['package com.hiro.ulike.composer; import java.util.*; public final class ActualExports { public static void main(String[] a) { int count=0;']
 def lit(x):return json.dumps(x,ensure_ascii=True)
 for style,(name,pin) in ARCHIVES.items():
  p=upload/name;assert sha(p)==pin
  with zipfile.ZipFile(p) as z:m=json.loads(z.read('manifest.json'))
  events=m['api_events'];c=m['completeness']
  assert c['ordered_api_model_complete'] is False and m['composer_resource_path'] is None
  assert m['mode_one']==1 and m['mode_two']==0
  assert not any(e['operation']=='set' for e in events)
  reports[style]={'archive_sha256':pin,'manifest_sha256':hashlib.sha256(json.dumps(m,sort_keys=True,separators=(',',':')).encode()).hexdigest(),
   'events':len(events),'return_codes':dict(collections.Counter(e['return_code'] for e in events)),'operations':dict(collections.Counter(e['operation'] for e in events)),
   'single_count_update_events':sum(e['operation']=='update' and e['count']==1 for e in events),
   'legacy_requested_node_count':len(m['ordered_requested_nodes']), 'filtered_latest_update_count':len(m['parameter_updates_observed_since_node_reset']),
   'mode':[1,0],'composer_resource_path_observed':False,'ordered_api_model_complete':False,'native_queue_barrier_performed':False,
   'whole_init_baseline_observed':False,'replay_authorized':False}
  java+=['{ Map<String,Object> m=new LinkedHashMap<>();m.put("schema","ulike-style-materials-164");m.put("completeness",Collections.singletonMap("ordered_api_model_complete",false));m.put("mode_one",1);m.put("mode_two",0);List<Map<String,Object>> events=new ArrayList<>();']
  for e in events:
   java+=['{Map<String,Object> e=new LinkedHashMap<>();e.put("operation",'+lit(e['operation'])+');e.put("return_code",'+str(e['return_code'])+');e.put("count",'+str(e['count'])+');events.add(e);}']
  java+=['m.put("api_events",events);V164ReplayAudit audit=V164ReplayAudit.inspect(m);if(audit.events!='+str(len(events))+' || audit.failedEvents!='+str(sum(e['return_code']!=0 for e in events))+' || audit.hasSetEvent || audit.observedResourcePath || audit.replayAuthorized)throw new AssertionError();try{audit.requireReplayable();throw new AssertionError();}catch(IllegalStateException expected){}count++;}']
 java+=['System.out.println("PASS "+count+" actual export replay rejections");}}']
 fixture=temp/'ActualExports.java';fixture.write_text('\n'.join(java)+'\n');return reports,fixture
def main():
 ap=argparse.ArgumentParser();ap.add_argument('--dex-root',type=pathlib.Path,default=WORK/'models168/inputs/stock/base');ap.add_argument('--upload',type=pathlib.Path,default=WORK.parent/'upload');args=ap.parse_args()
 jdk=WORK/'models168/tools/jdk21/jdk-21.0.12.1+1/bin';sdk=WORK/'models168/tools/android.jar';r8=WORK/'models168/tools/r8-8.3.37.jar'
 production=sorted((ROOT/'src').rglob('*.java'));tests=sorted((ROOT/'test').rglob('*.java'))
 with tempfile.TemporaryDirectory(prefix='ulike-composer-qa-') as tmp:
  temp=pathlib.Path(tmp);host=temp/'host';host.mkdir();android=temp/'android';android.mkdir();dex=temp/'dex';dex.mkdir()
  evidence,fixture=archives(args.upload,temp)
  run([jdk/'javac','-d',host,*production,*tests,fixture])
  host_result=run([jdk/'java','-cp',host,'com.hiro.ulike.composer.ComposerReplayTest']);boundary_result=run([jdk/'java','-cp',host,'com.hiro.ulike.composer.NativeComposerBoundaryTest']);exports_result=run([jdk/'java','-cp',host,'com.hiro.ulike.composer.ActualExports'])
  run([jdk/'javac','-source','8','-target','8','-bootclasspath',sdk,'-d',android,*production])
  run([jdk/'java','-cp',r8,'com.android.tools.r8.D8','--min-api','26','--lib',sdk,'--output',dex,*sorted(android.rglob('*.class'))])
  report={'schema':'ulike-composer-replay-qa-1','host_checks':host_result,'native_boundary_checks':boundary_result,'actual_export_checks':exports_result,'sdk36_compile':True,'d8_min_api':26,'dex_bytes':(dex/'classes.dex').stat().st_size,
   'source_sha256':{str(p.relative_to(ROOT)):sha(p) for p in production+tests+[ROOT/'verify.py']},'tools_sha256':{'android_sdk36':sha(sdk),'r8':sha(r8)},
   'dependency_source_sha256':{'../android_face_backend/verify_stock_contract.py':sha(ROOT.parent/'android_face_backend/verify_stock_contract.py')},'actual_stock_contract':stock(args.dex_root),'actual_uploaded_exports':evidence,'live_hook_coverage_installed':False,'source_native_queue_barrier_implemented':False,'target_native_queue_barrier_implemented':False,'device_sdk_replay_tested':False,'full_style_replay_available':False,
   'scope':'Exact API transcript capture/guard/replay logic with ordinary SDK command adapter; only synthetic host Preconditions and barrier receipts are used in tests.'}
  (ROOT/'QA.json').write_text(json.dumps(report,indent=2)+'\n');print(host_result);print(boundary_result);print(exports_result);print('PASS SDK36 + D8; exact stock signatures and tag field routes')
if __name__=='__main__':main()
