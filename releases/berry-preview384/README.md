# Berry Browser 1.4.7 / Hiro Morphe Patches 1.0.384

Yahoo!ニュースで記事と一覧を往復したあと、画像や見出しの一部が白く抜けるという報告に対し、Berry側のタブプレビュー生成処理を修正しました。

従来のBerryは、ページを読み終えるたびにタブの画像を撮る処理を予約していました。従来の「表示中タブを優先」パッチは、その撮影を操作が止まるまで延期していました。その間に別ページへ移動すると、古い予約が現在表示しているWebViewをビットマップに描画できます。

今回の修正は読み込み完了時に予約する自動撮影だけを止めます。タブを離れるときのBerry標準のプレビュー更新と手動スクリーンショットは利用できます。ページの強制リロードは行いません。

## 適用方法

1. Morphe ManagerでHiro Morphe Patchesを1.0.384に更新します。Berry単独版をお使いの場合は1.4.7を選んでください。
2. 元のBerry Browser 3.84.21のAPK一式に「プレビュー作成を後回しにして表示中タブを優先」と普段使用する項目を適用してください。
3. 従来と同じ署名キーで生成したAPKを端末にインストールしてください。

## 検証

3.84.21の元APK一式に統合版・単独版それぞれ全5項目を適用し、APKを再構築しました。旧版と修正版を比較し、9,203クラスと44,156の既存メソッドを照合しました。元の呼び出し1か所の差し替えと、撮影対象を確認するメソッド1件の追加を確認しました。検証内容は QA_Berry_Browser_v1.4.7.json に記録しています。Galaxy実機で報告された白抜けの解消は未確認です。

3.83.74／3.84.12でも統合版・単独版それぞれ全5項目を適用・再構築し、同じバージョンでは両パッチから同一のDEXを得ました。

## ビルド

このリポジトリのルートから次のコマンドを実行します。Java 21、Python 3、v373で固定した3つのツールを使用します。ツールの配布URLとSHA-256は `releases/berry-resolver373/verification/tool-sources.json` にあります。`previous` ディレクトリには配布済みの `Hiro_Morphe_Patches_v1.0.383.mpp` と `Berry_Browser_v1.4.6.mpp` を配置してください。

```sh
python3 releases/berry-preview384/build_mpp.py \
  --src releases/berry-preview384/src/app/hiro/berry/patches/ForegroundTabsPatch.java \
  --previous previous --tools tools --output dist \
  --zip-helper releases/berry-resolver373/build/build.py
```

`verification/NavigationPreviewAudit.java` は3.84.21の前後APKを比較する検査です。同じリポジトリの `releases/berry-resolver373/qa/ZoomApplicationAudit.java` と `ForegroundApplicationAudit.java` とともにコンパイルします。`verification/reproduce_preview_race.py` では旧版の撮影予約とページ遷移の競合を決定的な順序で再現できます。

統合版でもBerry単独版でも、変更したZIP項目は版情報、ForegroundTabsPatch.class、プロバイダーのclasses.dexの3点です。タブ撮影を含む既存ランタイム拡張は元のバイト列と一致します。
