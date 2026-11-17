#!/usr/bin/env python3
from pathlib import Path
import argparse,json,subprocess
ROOT=Path(__file__).resolve().parent
NAME='NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）'
HIRO=['NewX: タイムラインのカテゴリ設定を再起動後も保持','NewX: ポストの日時を絶対時間・24時間表記にする','NewX: 戻る1回で終了・履歴削除','NewX: Pikoの設定・メニューを日本語化',NAME]
PIKO=json.loads((ROOT/'piko-selected.json').read_text())
def main():
 p=argparse.ArgumentParser();p.add_argument('--case',required=True,choices=['dev1','dev2','dev2-clone','standalone','missing-piko']);a=p.parse_args()
 version='3.12.0-dev.1'if a.case=='dev1'else'3.12.0-dev.2'
 standalone=a.case in ['standalone','missing-piko']
 hiro=[NAME]if standalone else HIRO
 piko=['NewX: Remove ads']if standalone else PIKO+(['NewX: Clone']if a.case=='dev2-clone'else[])
 bundle=ROOT/'dist'/('NewX_UI_Options_v1.1.0.mpp'if standalone else'Hiro_Morphe_Patches_v1.0.380.mpp')
 sources=[(bundle,hiro),(ROOT/'inputs'/('piko-'+version+'.mpp'),piko)]
 if a.case=='missing-piko':sources=sources[:1]
 if a.case=='dev1':sources.reverse()
 out=ROOT/'verification';out.mkdir(exist_ok=True)
 command=['java','-XX:-UsePerfData','-Xmx3g','-jar',str(ROOT/'tools/morphe.jar'),'patch',str(ROOT/'inputs/X_12.43.0-prod.01.apkm'),'--exclusive','--unsigned','--disable-purge','--bytecode-mode','FULL','-o',str(out/(a.case+'.apk')),'-r',str(out/(a.case+'-result.json')),'-t',str(out/(a.case+'-work'))]
 for source,enabled in sources:
  command+=['-p',str(source)]
  for name in enabled:command+=['-e',name]
 with(out/(a.case+'.log')).open('w')as log:r=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT)
 log=(out/(a.case+'.log')).read_text()
 if a.case=='missing-piko':
  assert r.returncode!=0 and 'Piko 3.12.0-dev.1/dev.2' in log
  print('PASS: missing Piko fails closed');return
 print(a.case+' EXIT '+str(r.returncode));print(log[-3000:])
 if r.returncode:raise SystemExit(r.returncode)
if __name__=='__main__':main()
