# Berry Browser 1.4.8 / Hiro Morphe Patches 1.0.385

Yahoo!ニュースの記事から高速で戻る試験では、1.4.7のタブ撮影修正を適用しても一覧が白く抜けました。1.4.8では、Yahoo!ニュースのトップとカテゴリ一覧が履歴キャッシュから復帰したときに、その一覧を一度読み直します。スクロール位置を引き継ぎ、オフラインや入力中の一覧では再読み込みを見送ります。
Morphe Managerで「バックフォワードキャッシュの復帰処理を補正」を選び、元のBerry Browserに再適用してください。
Android 17・WebView 158のエミュレーターでは、通常8回と高速7回の計15往復で一覧を再表示できました。Galaxy実機では未確認です。

## 適用方法

1. Morphe ManagerでHiro Morphe Patchesを1.0.385へ更新します。Berry単独版をお使いの場合は1.4.8を選びます。
2. 元のBerry Browser 3.84.21のAPK一式を指定し、「バックフォワードキャッシュの復帰処理を補正」を選んで再適用してください。この項目の依存関係には1.4.7のタブ撮影修正を含む残り4項目が登録されています。
3. 過去のインストールと同じ署名キーを使って再構築し、端末にインストールします。

## 検証範囲

Android 17エミュレーターのBerry Browser 3.84.21、WebView 158.0.8127.2で履歴キャッシュを有効にして、Yahoo!ニュースの記事から一覧へ戻る操作を試験しました。通常8回と高速7回の計15往復で一覧の再表示を確認しました。同じ高速操作で従来の1.0.383と1.0.384は白抜けを再現しました。スクロール位置の引き継ぎは実装済みで、Galaxy実機での症状改善は未確認です。

3.84.21では統合版・Berry単独版それぞれ全5項目を適用し、再構築したAPKがバイト単位で一致しました。1.4.7との比較では復帰処理の15クラスに差があり、残り9,188クラスと2,180リソースは一致しました。3.83.74と3.84.12でも両配布版の全5項目を適用して再構築しました。


## ビルドと再検証

Java 21、Python 3、Node.js を使用します。リポジトリまたはソース資料を展開したルートから実行してください。ツールの入手元とハッシュは `releases/berry-resolver373/verification/tool-sources.json` にあります。`tools` に `ecj.jar` と `d8.jar` を、`previous` に公開済みの `Hiro_Morphe_Patches_v1.0.384.mpp` と `Berry_Browser_v1.4.7.mpp` を配置します。

```sh
python3 releases/berry-restore385/build_runtime.py --tools tools --output build/runtime
python3 releases/berry-restore385/build_mpp.py \
  --zip-helper releases/berry-resolver373/build/build.py --previous previous \
  --extension build/runtime/extensions/berry_bfcache_restore.mpe --output dist
```

ランタイムのビルドは既存のJVM・JavaScriptテストを含みます。復帰処理のブラウザー試験はPythonのPlaywrightとChromiumを使用します。

```sh
python3 releases/berry-restore385/verification/check_listing_browser.py \
  build/runtime/runtime_tests/generated/document_start.js --report browser-report.json
```
