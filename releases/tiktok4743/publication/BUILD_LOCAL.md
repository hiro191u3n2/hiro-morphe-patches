# TikTok 47.4.3 / 47.5.3 v4.1.0 の検証と公開

単体 v4.1.0 / OneBack v1.1.21 / 総合 v1.0.281。基準は固定コミットの総合 v1.0.280 です。署名確認済みの両原本に適用した MPP を、ローカルと CI の各2ビルドで再現して公開します。Android 実機の操作確認は含みません。

## 入力

Temurin JDK 21.0.8+9、`publish.py` の SHA-256 に一致する5つのツールを使用します。URL とアーカイブ内の抽出パスは `workflow.yml` にあります。基準 MPP と Metra 0.9.0-dev.1 MPP の URL / SHA-256 は `manifest-template.json` にあります。

元 APK の package / versionName / versionCode / bytes / SHA-256 / 署名証明書は `src/validation/apk-inputs.json` に固定しています。47.4.3 は2024704030・474921201 bytes、47.5.3 は2024705030・476733983 bytesです。署名検証も毎回行います。APK はリポジトリ・ソース ZIP・リリース資産へ入れません。

## 検証順序

1. 先行ビルドを行います。
2. 各原本へ、単体 MPP のネイティブ6項目＋OneBack、および総合 MPP の Hiro 製8項目＋上流 Settings を適用します。2原本×2構成の合計4ケースです。
3. 選択と適用パッチの完全一致・失敗0件・全工程成功を確認し、出力 APK の native / OneBack / privacy / クラス整合性 / Settings と日本語化の接続を監査します。
4. 版別証拠を結合し、4ケースが同じリリース MPP のハッシュに結び付いていることを検査します。
5. ソースと証拠を確定してから、空の作業場所2個で最終ビルドします。MPP が先行適用したバイトから変わる変更には、実 APK 検証のやり直しが必要です。

以下のプレースホルダーは実パスへ置き換えます。`BASE` は固定基準 MPP、`ORIGINAL_4743` / `ORIGINAL_4753` は両原本、`UPSTREAM` は Metra MPP、`TOOLS` は固定ツールのディレクトリです。`*_WORK` / `WORK_*` は未作成、`*_DIST` / `DIST_*` は別々の空ディレクトリにします。

```bash
python3 releases/tiktok4743/src/build.py --base BASE --tools TOOLS --work PILOT_WORK --dist PILOT_DIST
python3 releases/tiktok4743/src/validate_apk.py --apk ORIGINAL_4743 --version 47.4.3 --dist PILOT_DIST --upstream UPSTREAM --tools TOOLS --work REAL_4743_WORK
python3 releases/tiktok4743/src/validate_apk.py --apk ORIGINAL_4753 --version 47.5.3 --dist PILOT_DIST --upstream UPSTREAM --tools TOOLS --work REAL_4753_WORK
python3 releases/tiktok4743/src/merge_apk_proofs.py --dist PILOT_DIST
```

版別の2コマンドは、十分なメモリ・ディスクがあれば並行実行できます。各 Java 処理は `-Xmx2g` を使用します。47.4.3 の Settings 併用に `-f` は使用しません。47.5.3 の回帰検証では、上流のメタデータが47.4.3限定のため Settings 併用時だけ CLI の `-f` を使います。Hiro の実構造チェックは省略しません。

証拠は `src/validation/real-apk-4743.json`、`real-apk-4753.json`、`real-apk/4743/`、`real-apk/4753/` に保存し、結合時に `real-apk.json` を生成します。旧4.0.0の記録は `prior-4753-v4.0.0/` に分離しています。

追加の併用検証では、47.4.3へ上流136項目とHiro8項目を同時適用し、`-Xmx3g` で144項目すべての適用・再構築成功を確認しました。空SOの修正を含む同じ単体MPPのSHA-256に結び付けています。実行スクリプト、選択一覧、成功結果、空SO18パスと非空16ファイルの実出力監査、修正前の失敗履歴は [metra-all144/README.md](metra-all144/README.md) にあります。この結果は上記4ケースの詳細な出力監査を置き換えません。

```bash
python3 releases/tiktok4743/src/build.py --base BASE --tools TOOLS --work WORK_A --dist DIST_A
python3 releases/tiktok4743/src/build.py --base BASE --tools TOOLS --work WORK_B --dist DIST_B
python3 releases/tiktok4743/publication/freeze_local.py --first DIST_A --second DIST_B --repo . --reviewed-main REVIEWED_MAIN_SHA --reviewed-dev REVIEWED_DEV_SHA
python3 releases/tiktok4743/publication/verify_pair.py DIST_A DIST_B --repo .
python3 releases/tiktok4743/publication/selftest.py --dist DIST_A
python3 releases/tiktok4743/publication/package.py --dist DIST_A --repo .
python3 releases/tiktok4743/publication/publish.py --dist DIST_A --repo . --base BASE --upstream UPSTREAM --local-only
```

QA には全 src ファイルのハッシュが含まれます。証拠を追加した場合も最終2ビルドを更新します。`freeze_local.py` は両原本、4つの実 APK ケース、各 MPP ハッシュ、出力監査、2ローカルビルドの一致を確認し期待値を固定します。未実施の結果を成功フラグで代用しません。`verify_pair.py` は現行 GitHub Actions 実行内だけ CI 検証フラグを設定します。

`package.py` は今回の全 src/publication、README、LICENSE、NOTICE、日本語化の対応ソースと必要な既存ヘルパー、2種類の QA を収録します。日本語化の出所は固定コミット `efb5937ddea4fe29abdfe33027cfc0e5d397ae93` です。MPP2個、QA2個、ソース ZIP、説明、チェックサムの7資産を作ります。

## 公開

提出直前に main/dev の HEAD と基準 v1.0.280 のフィードを読み直します。ソースコミットは凍結した `reviewed_main_head` の直下、dev は `reviewed_dev_head` のままであることが必須です。`.github/workflows/tiktok4743-publish.yml` と `publication/workflow.yml` は同一にします。

GitHub Actions は2回の新規ビルド、凍結した実 APK 適用対象との照合、資産と公開参照状態の検査を行います。下書きへ7資産をアップロードし、再取得したバイトが一致してから公開します。固定コミットの MPP 配布 URL を確認し、main/dev のフィードを原子的に更新します。ULike の運用設定を保持し、Manager の TikTok のみが更新対象になることを検査して公開記録を残します。

過去の47.5.3向け Metra 全136項目の調査は12件失敗しているため公開成功の根拠にしません。47.4.3 の結果と混同せず履歴として残します。既存タグ・配布ブランチ、HEADや基準の変化、途中公開記録がある場合は停止し、状況を確認してから再開します。
