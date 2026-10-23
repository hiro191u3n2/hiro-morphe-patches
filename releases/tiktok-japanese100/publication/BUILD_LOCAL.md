# TikTok Japanese100 のローカルビルドと公開準備

日本語化 v1.0.0 / 総合 v1.0.279。基準は固定コミットの総合 v1.0.278、上流は公開済み Metra v0.9.0-dev.1 MPP です。URL と SHA-256 は `manifest-template.json`、ツールの取得先と SHA-256 は `workflow.yml` と `publish.py` に固定しています。Temurin JDK 21.0.8+9 を使用します。

以下の `BASE`、`UPSTREAM`、`TOOLS`、`WORK_A`、`WORK_B`、`DIST_A`、`DIST_B` はそれぞれ実際の絶対パスに置き換えます。各作業・出力ディレクトリは新規にし、A と B を分けます。リポジトリのルートで実行します。

```bash
python3 releases/tiktok-japanese100/src/build.py --base BASE --upstream UPSTREAM --tools TOOLS --legacy releases/oneback163/src --work WORK_A --dist DIST_A
python3 releases/tiktok-japanese100/src/build.py --base BASE --upstream UPSTREAM --tools TOOLS --legacy releases/oneback163/src --work WORK_B --dist DIST_B
python3 releases/tiktok-japanese100/publication/freeze_local.py --first DIST_A --second DIST_B
python3 releases/tiktok-japanese100/publication/verify_pair.py DIST_A DIST_B
python3 releases/tiktok-japanese100/publication/package.py --dist DIST_A --legacy releases/oneback163/src
python3 releases/tiktok-japanese100/publication/publish.py --dist DIST_A --repo . --base BASE --upstream UPSTREAM --local-only
```

`freeze_local.py` は別々の出力ディレクトリにある 2 つの MPP と QA の完全一致、全翻訳カタログ、ソースと検証結果を確認し、期待値を固定します。`verify_pair.py` はこの固定値と一致する場合だけ `manifest.json` を作ります。ローカルで実行しても CI 完了のフラグは立ちません。

`package.py` は LICENSE、NOTICE、README、全 src/publication テキストファイル（翻訳 CSV を含む）、使用する 2 つの legacy ヘルパー、QA を ZIP に収録します。APK、実行バイナリ、古いリリースの検証資料をソース ZIP に入れません。配布 MPP を含めて計 6 資産と SHA256SUMS を作ります。

提出直前に、main/dev の HEAD と v1.0.278 の配布フィードがレビュー時から変わっていないことを確認します。ソースコミットは `reviewed_main_head` の直下、dev は `reviewed_dev_head` のままである必要があります。`publication/workflow.yml` と `.github/workflows/tiktok-japanese100-publish.yml` を同一に保ちます。

main へのソース提出で workflow が起動し、ピン留めした入力から 2 回新規ビルドします。ローカルと CI の MPP・QA の完全一致、全資産・ソース ZIP の照合、公開前チェックに通ると、リリース作成、公開配布バイトの再取得、固定コミットの配布 URL、main/dev の原子的なフィード更新、公開記録の保存まで実行します。HEAD や基準版が変わった場合は新しい状態をレビューし、検査を弱めないでください。

公開済み Metra 拡張の実 DEX を合成 APK で検査します。TikTok 47.4.3 本体、Android 17 端末、123 パッチ同時実行は未確認です。
