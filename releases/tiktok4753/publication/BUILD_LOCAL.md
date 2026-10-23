# TikTok 47.5.3 v4.0.0 の検証と公開

単体 v4.0.0 / 戻る処理 v1.1.20 / 総合 v1.0.280。基準は固定コミットの総合 v1.0.279 です。署名確認済みの元 APK と実際の出力 APK の検証を先に行い、その検証済み MPP と同じバイトを CI で再現してから公開します。実機上の操作確認は含みません。

## 入力

Temurin JDK 21.0.8+9 と、`publish.py` の SHA-256 に一致する5つのビルドツールを使用します。固定 URL とアーカイブ内の抽出パスは `workflow.yml` にあります。基準 MPP と上流 Settings 用 MPP の URL / SHA-256 は `manifest-template.json` にあります。

元 APK は `com.zhiliaoapp.musically` / 47.5.3 / 2024705030、476,733,983 bytes、SHA-256 `6670ca3f93bfea379658cad3c27f05d3a380b23a0d82569cce684b7453e8ff3e` です。署名の検証と署名証明書の照合も必要です。APK をソース、リポジトリ、公開 ZIP、リリース資産に含めません。

## 実APK検証と最終ビルドの順序

1. 一時ディレクトリで先行ビルドを行います。
2. その単体 MPP のネイティブ機能6個と戻る処理、および総合 MPP の Hiro 製8個と上流 Settings をそれぞれ元 APK に適用します。CLI の終了コードやトップレベルの `success` だけを成功判定に使いません。選択・適用パッチの集合、失敗0件、全工程成功を検査します。
3. 出力 APK のネイティブ処理、戻る処理、プライバシー処理、クラスの整合性、総合版の設定画面と日本語化を監査します。`src/validation/real-apk.json` に `standalone_native` と `aggregate_settings_ja` の2件を記録し、検証した MPP 自体のハッシュを保存します。
4. 必要な検証記録とソースをすべて確定した後、新しい作業ディレクトリ2個で最終ビルドします。QA は全 src ファイルのハッシュを含むため、記録を追加するたびに最終ビルドをやり直します。検証済み MPP のハッシュが変わる変更なら実APKへの適用検証もやり直します。

以下のプレースホルダーを実際のパスに置き換え、リポジトリのルートで実行します。`BASE` は基準 MPP、`ORIGINAL` は元 APK、`UPSTREAM` は上流 MPP、`TOOLS` は固定ツールのディレクトリです。作業用の `PILOT_WORK`、`REAL_WORK`、`WORK_A`、`WORK_B` には未作成のディレクトリを指定し、出力先の `PILOT_DIST`、`DIST_A`、`DIST_B` はそれぞれ別の空ディレクトリにします。

まず先行ビルドと実 APK 検証を行います。`validate_apk.py` が2ケースを適用・再構築し、出力を監査して `src/validation/real-apk.json` と `src/validation/real-apk/` に証拠を保存します。

```bash
python3 releases/tiktok4753/src/build.py --base BASE --tools TOOLS --work PILOT_WORK --dist PILOT_DIST
python3 releases/tiktok4753/src/validate_apk.py --apk ORIGINAL --dist PILOT_DIST --upstream UPSTREAM --tools TOOLS --work REAL_WORK
```

両ケースの監査が成功し、記録とソースが確定した後に最終ビルドを2回行います。

```bash
python3 releases/tiktok4753/src/build.py --base BASE --tools TOOLS --work WORK_A --dist DIST_A
python3 releases/tiktok4753/src/build.py --base BASE --tools TOOLS --work WORK_B --dist DIST_B
python3 releases/tiktok4753/publication/freeze_local.py --first DIST_A --second DIST_B --repo .
python3 releases/tiktok4753/publication/verify_pair.py DIST_A DIST_B --repo .
python3 releases/tiktok4753/publication/selftest.py --dist DIST_A
python3 releases/tiktok4753/publication/package.py --dist DIST_A --repo .
python3 releases/tiktok4753/publication/publish.py --dist DIST_A --repo . --base BASE --upstream UPSTREAM --local-only
```

`freeze_local.py` は元 APK の署名・サイズ・ハッシュ、2つの実APKケース、各 MPP のハッシュ、出力監査、ローカル2ビルドを確認して期待値を固定します。未実行の検証結果をテンプレートで成功にすることはありません。`verify_pair.py` は同じバイトを要求し、現在の GitHub Actions 実行内でだけ CI 検証済みフラグを立てます。

`package.py` は今回の全 src/publication、README、LICENSE、NOTICE、変更していない日本語化の対応ソースと必要な既存ビルドヘルパー、2種類の QA を収録します。日本語化の出所は固定コミット `efb5937ddea4fe29abdfe33027cfc0e5d397ae93` です。バイナリをソース ZIP に入れず、MPP2個、QA2個、ソース ZIP、説明、チェックサムの7資産を作ります。

## 公開

提出直前に main/dev の HEAD と基準 v1.0.279 のフィードを読み直し、`freeze_local.py --reviewed-main SHA --reviewed-dev SHA` でレビュー済み状態を記録します。ソースコミットは `reviewed_main_head` の直下、dev は `reviewed_dev_head` のままである必要があります。2つの workflow ファイルは同一に保ちます。

main のソース提出から GitHub Actions が起動し、固定入力を使う2回の新規ビルド、凍結済みの実APK検証対象 MPP / QA との照合、ソースと資産の検査、公開前の参照状態検査を行います。成功後は下書きリリースへ7資産をアップロードし、再取得してバイトを照合してから公開します。固定コミットの MPP 配布 URL を確認した後、main/dev のフィードを原子的に更新し、ULike の運用設定と TikTok のみの更新範囲を検査して公開記録を保存します。

上流 Metra 全136パッチの探索的実行は12件失敗しているため、公開成功の根拠には使いません。失敗一覧を説明とソースに残し、今回それらを修正したとは記載しません。タグ・配布ブランチが既存、基準や HEAD が変化、途中公開の記録が存在する場合は停止し、現状を確認してから処理を続けます。
