# Berry Browser v1.4.6 / Hiro Morphe Patches v1.0.373

Berry Browser 3.84.21で「ダブルタップから指を離さず片手ズーム」が候補0で失敗する問題を修正しました。ユーザー提供の未改造APK一式で旧エラーを再現し、修正版の適用・再構築を確認しています。

## 使い方

1. Morphe ManagerでHiro Morphe Patchesを1.0.373へ更新します。
2. 元の未改造Berry Browser 3.84.21のAPK／APKSへ、必要なパッチを再適用します。
3. 作成されたAPKを、従来と同じ署名キーでインストールします。

単独のBerryソースを利用する場合は1.4.6です。統合版と単独版を重複選択しないでください。ソース更新だけではインストール済みアプリに反映されません。

## 変更点

- 片手ズームの検出を、使用しないタブ制御処理の検出から分離。
- 3.84.21でコントローラーからタブ本体へ移動した再開・停止・保存・復元処理に対応。
- タブから具体型のコントローラーを参照する新構造に対応。分岐・例外を含む所有関係を検証。
- 復元の補助処理も、新しいタブ→コントローラー→WebViewの参照を使用。
- 保存処理などの直前に入れる補助処理を、元の分岐が飛び越さないよう修正。
- BFCache側の直接のクライアント設定、インライン化されたリスナー登録、直接のWebView getterに対応。

実行拡張5ファイルは旧版とバイト一致。無関係なパッチ、共有先切替、MessageQueue、設定、描画診断を削除した状態を保持します。適用不能な機能を黙って無効化して成功扱いにはしていません。

## 検証

Patcher 2.4.0-dev.7（Manager 2.6.0-dev.5内のPatcherクラスと一致）で以下を確認しました。

- 実3.84.21の元APKS一式への統合版・単独版の全5項目適用と再構築。
- 実3.84.21の片手ズーム単独適用と再構築。
- 実3.84.12への統合版5項目適用と再構築。
- 両版の検出、所有関係、異なる受信オブジェクト・重複・分岐による参照置換の拒否。
- 実際の出力APKのズーム挿入先、既存命令・分岐・例外領域、復元時の引数、保存処理の分岐先。
- BFCache復帰処理の実出力DEXで、WebView同一性確認・getter命令・参照先・レジスタ・分岐・引数幅を検証（3.84.21: 3,840項目、3.84.12: 3,816項目）。
- 設定XMLの正常解析、既存設定保持、ズーム設定1件の追加。
- MPP内の対象外DEX定義・ZIP項目・実行拡張の保持。

Galaxy実機でのインストール・ズーム操作・BFCache画面動作は未確認です。APK原本や署名キーは配布しません。

## 開発・再検証

`verification/build.json`に最終ソース・ツール・成果物のSHA-256、`verification/apk-checks.json`に実APKの検証範囲を記録しています。入力はユーザー提供3.84.21と、署名検証済み3.84.12です。`verification/tool-sources.json`からツールを取得し、ハッシュを確認してください。

```sh
python3 build/build.py --base /path/to/baseline372.mpp --src src --tools /path/to/tools --out /path/to/dist
python3 qa/run_release_checks.py --inputs /path/to/inputs --tools /path/to/tools --dist /path/to/dist --out /path/to/checks
```

実出力DEXの追加監査は、同梱の`qa/VerifyPatchedHooks.java`（旧版の共通ヘルパー）と`qa/NativeApplicationAudit.java`をPatcherのクラスパスでコンパイルし、`NativeApplicationAudit patched.apk platform`（3.84.21）または`interface`（3.84.12）で再実行できます。旧ヘルパーのmain処理・旧版との比較処理は使用しません。

既存配布ページ: https://hiro-morphe-patches.otoha10.chatgpt.site
更新ソース: https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/main/patches-bundle.json
