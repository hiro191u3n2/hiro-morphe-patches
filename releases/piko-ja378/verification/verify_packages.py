#!/usr/bin/env python3
"""Apply delivered Japanese bundles with actual Patcher2.4 and supplied X APKM."""
from pathlib import Path
import argparse,json,subprocess
ROOT=Path(__file__).resolve().parents[1]
TASK=ROOT.parent
NAMES=['NewX: タイムラインのカテゴリ設定を再起動後も保持','NewX: ポストの日時を絶対時間・24時間表記にする','NewX: 戻る1回で終了・履歴削除']
JA='NewX: Pikoの設定・メニューを日本語化'
selected=json.loads((ROOT/'piko-dev2-selected.json').read_text())
cases=[('dev1','3.12.0-dev.1',False,False),('dev2','3.12.0-dev.2',False,False),('dev2-clone','3.12.0-dev.2',True,False),('standalone','3.12.0-dev.2',False,True)]
parser=argparse.ArgumentParser();parser.add_argument('--case');args=parser.parse_args()
if args.case:cases=[case for case in cases if case[0]==args.case]
for label,version,clone,standalone in cases:
 out=ROOT/'verification';out.mkdir(exist_ok=True)
 bundle=ROOT/'dist'/('Piko_NewX_Japanese_v1.0.0.mpp'if standalone else'Hiro_Morphe_Patches_v1.0.378.mpp')
 hiro_names=[JA]if standalone else NAMES+[JA]
 piko_names=(['NewX: Remove ads']if standalone else selected)+(['NewX: Clone']if clone else[])
 names=hiro_names+piko_names
 sources=[(bundle,hiro_names),(TASK/'inputs'/('piko-'+version+'.mpp'),piko_names)]
 if label=='dev1':sources.reverse()
 cmd=['java','-XX:-UsePerfData','-Xmx3g','-jar',str(TASK/'tools/morphe.jar'),'patch',str(TASK/'inputs/X_12.43.0-prod.01.apkm'),'--exclusive','--unsigned','--disable-purge','--bytecode-mode','FULL','-o',str(out/(label+'.apk')),'-r',str(out/(label+'-result.json')),'-t',str(out/(label+'-work'))]
 for source,enabled in sources:
  cmd+=['-p',str(source)]
  for name in enabled:cmd+=['-e',name]
 print('START '+label+' '+str(len(names))+' patches',flush=True)
 with (out/(label+'.log')).open('w')as f:r=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
 print('EXIT '+label+' '+str(r.returncode),flush=True)
 if r.returncode:
  print((out/(label+'.log')).read_text()[-12000:],flush=True);raise SystemExit(r.returncode)
 data=json.loads((out/(label+'-result.json')).read_text());print(json.dumps(data,ensure_ascii=False)[:500],flush=True)
