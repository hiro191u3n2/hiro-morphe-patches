#!/usr/bin/env python3
"""Check source-bound lifecycle inventory; no original code or APK is emitted."""
import argparse, collections, hashlib, json
from pathlib import Path

p=argparse.ArgumentParser();p.add_argument('--stock-apk',required=True,type=Path);p.add_argument('--references',required=True,type=Path);p.add_argument('--output',required=True,type=Path);a=p.parse_args()
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
assert digest(a.stock_apk)=='f51c64cc2ffedf2110f18816831e5c7cff5652980b42d6bb22a4a3f37316730d','wrong original APK'
rows=[line.split('\t') for line in a.references.read_text().splitlines()]
defs=[line.split('\t') for line in Path(str(a.references)+'.definitions').read_text().splitlines()]
restore=[line.split('\t') for line in Path(str(a.references)+'.restoration').read_text().splitlines()]
ri='Lcom/ss/android/medialib/RecordInvoker;';ve='Lcom/ss/android/vesdk/VERecorder;'
create=[r for r in rows if r[4]==ri+'->nativeCreate()J']
write=[r for r in rows if r[0]=='handle_write']
tear=[r for r in rows if r[4]==ri+'->nativeUninitBeautyPlay(J)I']
assert len(create)==2 and len(write)==3 and len(tear)==1
assert {r[1] for r in create}=={ri+'->initBeautyPlay(IILjava/lang/String;IILjava/lang/String;IZZZ)I',ri+'->initBeautyPlayOnlyPreview(Lcom/ss/android/medialib/qr/ScanSettings;)I'}
ctors={d[0] for d in defs if d[0].startswith(ve+'-><init>')}
delegating=[r for r in rows if r[1] in ctors and r[4].startswith(ve+'-><init>')]
assert len(ctors)==8 and len(delegating)==6
terminal=sorted(ctors-{r[1] for r in delegating});assert len(terminal)==2
factory=[r for r in restore if r[0].startswith('Lcom/ss/android/vesdk/TERecordFactory;->create(')]
assert len(factory)==1 and factory[0][2].startswith('Lcom/ss/android/vesdk/TECameraVideoRecorder;-><init>')
async_edges=[r for r in restore if r[2] in ('Li/f/l/n/q/t;->a(Ljava/lang/Runnable;)V','Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z','Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z')]
assert any(r[0]=='Lcom/bytedance/corecamera/camera/basic/PureCameraFragment$d;->e()V' for r in async_edges)
assert any(r[0]=='Li/o/a/b1/a/g/y$d;->e()V' for r in async_edges)
out={
 'schema':'ulike-stock-recorder-lifecycle-inventory-1',
 'scope':'Static original ULike v5.6.2(740) multidex references. Not dynamic/native/reflection coverage.',
 'stock_apk_sha256':digest(a.stock_apk),'dex_class_count':33134,
 'references':len(rows),'lifecycle_definition_count':len(defs),
 'reference_categories':dict(collections.Counter(r[0] for r in rows)),
 'native_create_calls':[dict(caller=r[1],code_unit_offset=int(r[2]),source_method_sha256=r[5]) for r in create],
 'native_uninit_calls':[dict(caller=r[1],code_unit_offset=int(r[2]),source_method_sha256=r[5]) for r in tear],
 'handle_writes':[dict(caller=r[1],code_unit_offset=int(r[2]),source_method_sha256=r[5]) for r in write],
 'constructor_count':8,'delegating_constructor_count':6,'terminal_constructors':terminal,
 'factory_constructs_only':'Lcom/ss/android/vesdk/TECameraVideoRecorder;',
 'other_backend_definition':'Lcom/ss/android/vesdk/TEPubRecorder; exists, but no direct constructor invocation from another original DEX method was observed',
 'native_init_restoration_is_not_synchronous':True,
 'asynchronous_restore_edges':[dict(caller=r[0],code_unit_offset=int(r[1]),target=r[2],source_method_sha256=r[3]) for r in async_edges if r[0].endswith('->e()V')],
 'actual_composer_restore_barrier_implemented':False,'partial_native_hook_methods':3,'partial_hooks_installed':False,'admission_enabled':False,'device_execution':False,
 'remaining_required_hooks':[
  'VERecorder two terminal constructors before backend allocation, successful completion and exceptional exit; prove six delegates preserve one registration',
  'VERecorder.onDestroy plus start/stop preview, init, pause/resume and surface entry families',
  'TECameraVideoRecorder direct/async worker lifecycle entries, releaseInteralRecorder, startRecordPreview, stopRecordPreview and surface callbacks, including permitted internal stop/restart delegation',
  'RecordInvoker direct startPlay/stopPlay/onPause/onDestroy/changeSurface and outstanding calls with previously captured native handle',
  'The asynchronous PureCameraFragment and y$d native-init/application tasks; actual composer reapplication completion plus render ordering is still unproved, and Handler enqueue returns are insufficient',
  'All-state composer transcript and independently observed restoration receipt for selected camera/style before releasing exclusive analysis lease'],
 'private_generated_inventory_sha256':{p.name:digest(p) for p in [a.references,Path(str(a.references)+'.definitions'),Path(str(a.references)+'.backends'),Path(str(a.references)+'.restoration')]}
}
a.output.write_text(json.dumps(out,indent=2,ensure_ascii=False)+'\n');print('PASS exact native lifetime + constructor + asynchronous restore inventory')
