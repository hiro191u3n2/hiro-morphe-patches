#!/usr/bin/env python3
"""Build narrowly scoped UI provider and runtime, excluding all compile/test stubs."""
from pathlib import Path
import hashlib,json,subprocess,zipfile,shutil
ROOT=Path(__file__).resolve().parent
TOOLS=ROOT/'tools'
CP=TOOLS/'morphe.jar'
WORK=ROOT/'build'
def run(args):
 r=subprocess.run(list(map(str,args)),text=True,capture_output=True)
 if r.returncode: print(r.stdout+r.stderr);raise RuntimeError(str(args[:4]))
 return r.stdout+r.stderr
def archive(path, entries):
 with zipfile.ZipFile(path,'w')as z:
  for n,b in sorted(entries.items()):
   i=zipfile.ZipInfo(n,(2026,11,17,0,0,0));i.create_system=3;i.external_attr=0o100644<<16
   z.writestr(i,b,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
def classfiles(path):return{p.relative_to(path).as_posix():p.read_bytes()for p in path.rglob('*.class')}
def main():
 for n in ['provider','runtime','provider-dex','runtime-dex']:
  if (WORK/n).exists():shutil.rmtree(WORK/n)
  (WORK/n).mkdir(parents=True,exist_ok=True)
 compiler=['java','-XX:-UsePerfData','-jar',TOOLS/'ecj.jar','-8','-nowarn','-encoding','UTF-8']
 print(run(compiler+['-cp',CP,'-d',WORK/'provider',ROOT/'src/patch/UiOptionsPatch.java',ROOT/'chat/ChatRecommendationsHook.java',ROOT/'bars/PinBarsPatch.java',ROOT/'views/ViewCountHook.java']))
 stubs=sorted((ROOT/'refresh/stubs/android').rglob('*.java'))+sorted((ROOT/'src/stubs').rglob('*.java'))
 print(run(compiler+['-d',WORK/'runtime',*stubs,ROOT/'src/runtime/UiOptions.java',ROOT/'refresh/RefreshRate.java',ROOT/'views-runtime/ViewCountFormat.java']))
 provider=classfiles(WORK/'provider');runtime=classfiles(WORK/'runtime')
 runtime_only={n:b for n,b in runtime.items() if n.startswith('app/hiro/newxui/')}
 stubs_only={n:b for n,b in runtime.items()if not n.startswith('app/hiro/newxui/')}
 expected_runtime={'app/hiro/newxui/UiOptions.class','app/hiro/newxui/RefreshRate.class','app/hiro/newxui/RefreshRate$Saved.class','app/hiro/newxui/ViewCountFormat.class','app/hiro/newxui/ViewCountFormat$FormatterState.class'}
 assert set(runtime_only)==expected_runtime and provider and all(n.startswith('app/hiro/newxui/patches/')for n in provider)
 archive(WORK/'provider.jar',provider);archive(WORK/'runtime.jar',runtime_only);archive(WORK/'stubs.jar',stubs_only)
 for kind in ['provider','runtime']:
  log=run(['java','-XX:-UsePerfData','-cp',TOOLS/'d8.jar','com.android.tools.r8.D8','--release','--min-api','26','--classpath',CP,'--classpath',WORK/'stubs.jar','--output',WORK/(kind+'-dex'),WORK/(kind+'.jar')]);(WORK/(kind+'-d8.log')).write_text(log)
 text={
 'title':('Hiro display settings','Hiroの表示設定'),
 'summary':('Chat recommendations, fixed navigation bars, 120 Hz preference and full view counts','チャットのおすすめ・上下バー固定・120Hz・表示回数を個別に切り替えます'),
 'chat_title':('Hide chat recommendations','チャットのおすすめを非表示'),
 'chat_summary':('Hide the recommendations heading, contact cards and suggested users. Restart X after changing this option.','おすすめ見出し・連絡先同期の案内・推薦ユーザーを非表示にします。変更後はXを再起動してください。'),
 'bars_title':('Keep top and bottom bars visible','スクロール中も上下のバーを固定'),
 'bars_summary':('Keep the home header and main navigation visible while scrolling. Restart X after changing this option.','スクロールによるホーム上部と下部ナビゲーションの自動非表示を止めます。変更後はXを再起動してください。'),
 'refresh_title':('Prefer 120 Hz','120Hzを優先して使用'),
 'refresh_summary':('Request a supported 120 Hz mode at the current resolution. Applies when returning to X. Power and thermal limits still apply. Turning this off restores the original window preference.','対応する120Hzモードを現在の解像度で要求します。Xへ戻ると反映。省電力・発熱などOSの制限は残ります。オフで元の表示設定へ戻します。'),
 'views_title':('Show full view counts','表示回数を省略せず表示'),
 'views_summary':('Show timeline view counts as full integers, for example 3,001 instead of 3K. Other counters are unchanged. Restart X after changing this option.','タイムラインの表示回数を「3K」ではなく「3,001」のように全桁表示します。いいねなど他の数値は変更しません。変更後はXを再起動してください。')}
 from xml.sax.saxutils import escape
 assets={}
 for idx,lang in [(0,'values'),(1,'values-ja')]:
  assets['newx-ui/'+lang+'.xml']=('\n'+''.join('    <string name="hiro_newx_ui_'+key+'" formatted="false">'+escape(pair[idx])+'</string>\n'for key,pair in text.items())).encode()
 mf='Manifest-Version: 1.0\r\nName: NewX UI Options\r\nVersion: 1.1.1\r\nTimestamp: 2026-11-17T09:30:00\r\nPatcher-Version: 2.4.0\r\nSource: remote\r\nAuthor: Hiro\r\nLicense: GPLv3\r\n\r\n'
 entries=dict(provider,**assets);entries.update({'META-INF/MANIFEST.MF':mf.encode(),'classes.dex':(WORK/'provider-dex/classes.dex').read_bytes(),'extensions/newx_ui_options.mpe':(WORK/'runtime-dex/classes.dex').read_bytes()})
 for name in ['LICENSE','NOTICE']:entries['newx-ui/'+name]=(ROOT/name).read_bytes()
 archive(ROOT/'component.mpp',entries)
 print(json.dumps({'component_bytes':(ROOT/'component.mpp').stat().st_size,'component_sha256':hashlib.sha256((ROOT/'component.mpp').read_bytes()).hexdigest(),'provider_classes':len(provider),'runtime_classes':len(runtime_only),'compile_stubs_packaged':False}))
if __name__=='__main__':main()
