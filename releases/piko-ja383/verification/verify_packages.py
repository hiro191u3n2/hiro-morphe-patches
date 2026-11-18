#!/usr/bin/env python3
"""Reproduce the reported failure, then verify current combined and standalone bundles."""
import argparse, hashlib, json, subprocess
from pathlib import Path
RELEASE=Path(__file__).resolve().parents[1]
JA='NewX: Pikoの設定・メニューを日本語化'
UI='NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）'
HIRO=['NewX: タイムラインのカテゴリ設定を再起動後も保持','NewX: ポストの日時を絶対時間・24時間表記にする','NewX: 戻る1回で終了・履歴削除',JA,UI]
PIKO=json.loads((RELEASE/'piko-dev2-selected.json').read_text())
def main():
 p=argparse.ArgumentParser();p.add_argument('case',choices=['dev3','dev3-clone','dev2','baseline-dev2','standalone']);p.add_argument('--workspace',type=Path,required=True);a=p.parse_args();TASK=a.workspace.resolve()
 out=TASK/'qa';out.mkdir(exist_ok=True)
 version='3.12.0-dev.2' if a.case in ['dev2','baseline-dev2'] else '3.12.0-dev.3'
 hiro=HIRO;piko=PIKO+(['NewX: Clone'] if a.case=='dev3-clone' else [])
 sources=[(RELEASE/'dist/Hiro_Morphe_Patches_v1.0.383.mpp',hiro)]
 if a.case=='baseline-dev2':sources=[(TASK/'inputs/Hiro_Morphe_Patches_v1.0.382.mpp',hiro)]
 if a.case=='standalone':
  sources=[(RELEASE/'dist/Piko_NewX_Japanese_v1.0.1.mpp',[JA]),(RELEASE/'dist/NewX_UI_Options_v1.1.3.mpp',[UI])]
  piko=['NewX: Remove ads']
 sources.append((TASK/'upstream-piko'/('patches-'+version+'.mpp'),piko))
 if a.case in ['dev2','baseline-dev2']:sources.reverse()
 command=['java','-XX:-UsePerfData','-Xmx3g','-jar',str(TASK/'tools/morphe25.jar'),'patch',str(TASK/'inputs/X_12.43.0-prod.01.apkm'),'--exclusive','--unsigned','--disable-purge','--bytecode-mode','FULL','-o',str(out/(a.case+'.apk')),'-r',str(out/(a.case+'-result.json')),'-t',str(out/(a.case+'-work'))]
 for path,names in sources:
  command+=['-p',str(path)]
  for name in names:command+=['-e',name]
 with (out/(a.case+'.log')).open('w') as f:r=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
 log=(out/(a.case+'.log')).read_text()
 print(a.case+' exit '+str(r.returncode),flush=True);print(log[-5000:],flush=True)
 r.check_returncode()
 report=json.loads((out/(a.case+'-result.json')).read_text())
 assert report.get('success') is True and not report.get('failedPatches'), report
 assert len(report['appliedPatches'])==sum(len(n) for _,n in sources), report
 summary={'result':'PASS','case':a.case,'selected_patch_count':sum(len(n) for _,n in sources),'piko_version':version,'patcher_version':'2.5.0-dev.1','desktop_version':'2.3.0-dev.2','apk_sha256':hashlib.sha256((out/(a.case+'.apk')).read_bytes()).hexdigest(),'physical_device_tested':False,'emulator_tested':False}
 (out/(a.case+'-summary.json')).write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps(summary))
if __name__=='__main__':main()
