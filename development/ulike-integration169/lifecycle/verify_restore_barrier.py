#!/usr/bin/env python3
"""Bounded exact-original-binary findings. Never treats readiness as a barrier."""
import hashlib,json,pathlib,sys
from loguru import logger
logger.disable('androguard')
from androguard.core.dex import DEX
ROOT=pathlib.Path(__file__).resolve().parent
WORK=ROOT.parents[1]
PIN='47a96995b8a1989df8abe9d659e9bf92a9388986e0fdf777a7ebfa82dfcf2f37'
def sha(b):return hashlib.sha256(b).hexdigest()
def main():
 dex=WORK/'models168/inputs/stock/base/classes3.dex'
 # Stock class-file SHA is also bound in the independently checked core contract.
 core=json.loads((WORK/'hdr_rebuild167/core/android_composer_replay/QA.json').read_text())
 expected=core['actual_stock_contract']['dex_sha256']['classes3.dex']
 raw=dex.read_bytes();assert sha(raw)==expected
 methods=[m for c in DEX(raw).get_classes() if c.get_name()=='Lcom/ss/android/medialib/RecordInvoker;' for m in c.get_methods() if m.get_name()=='onNativeCallback_Init' and m.get_descriptor().replace(' ','')=='(I)V']
 assert len(methods)==1;m=methods[0];assert sha(m.get_code().get_raw())==PIN
 ins={};offset=0
 for i in m.get_instructions():ins[offset]=i;offset+=i.get_length()//2
 anchors={2:('const/4','v1, 1'),3:('if-gez','v5, +017h'),25:('goto','+17h'),62:('iput-boolean','v1, v4, Lcom/ss/android/medialib/RecordInvoker;->mIsRenderReady Z'),64:('return-void','')}
 for off,(name,output) in anchors.items():assert ins[off].get_name()==name and ins[off].get_output()==output
 assert [off for off,i in ins.items() if i.get_name().startswith('return')]==[64]
 writers=[]
 for off,i in ins.items():
  if i.get_name().startswith(('const','move','new-instance','iget','sget')) and i.get_output().split(',')[0]=='v1':writers.append(off)
 assert writers==[2]
 # Negative status takes the fallthrough and common goto: no status rewrite,
 # then the same final ready=true store after optional listener invocations.
 assert 25+ins[25].get_ref_off()==48
 assert [off for off,i in ins.items() if '->onNativeInitCallBack(I)V' in i.get_output()]==[52,59]
 inventory=json.loads((ROOT/'INVENTORY.json').read_text())
 assert inventory['native_init_restoration_is_not_synchronous'] and len(inventory['asynchronous_restore_edges'])==3
 report={'schema':'ulike-restoration-barrier-findings-171-v1','verifier_sha256':sha(pathlib.Path(__file__).read_bytes()),
 'classes3_dex_sha256':sha(raw),'method':'Lcom/ss/android/medialib/RecordInvoker;->onNativeCallback_Init(I)V',
 'method_raw_code_sha256':PIN,'independently_checked_anchors':len(anchors),'normal_return_code_unit_offsets':[64],
 'status_negative_path_joins_common_listener_dispatch_at':48,'ready_true_store_before_only_normal_return':62,
 'constant_true_register_defined_at':2,'normal_return_sets_ready_true_for_negative_status':True,
 'meaning':'mIsRenderReady or native-init listener presence cannot prove successful initialization or native composer/render restoration.',
 'async_restore_inventory_sha256':sha((ROOT/'INVENTORY.json').read_bytes()),'app_async_restore_edges':inventory['asynchronous_restore_edges'],
 'barrier_implemented':False,'device_execution':False,'scope':'Pinned static normal-return behavior only; original listener exceptions and all native queues are not modeled.'}
 (ROOT/'RESTORATION_BARRIER_FINDINGS.json').write_text(json.dumps(report,indent=2)+'\n')
 print('PASS pinned native-init flag finding: negative status may return ready=true; not a restoration receipt')
if __name__=='__main__':main()
