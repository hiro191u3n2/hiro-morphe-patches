# TikTok 47.4.3 戻る修正 v4.2.0 の検証と公開

単体 v4.2.0 / OneBack v1.1.22 / 総合 v1.0.282。固定した総合 v1.0.281 を基準に、47.4.3 への適用、47.5.3 の強制選択拒否、上流との併用を検証します。Android 実機の操作確認は含みません。

## 入力

Temurin JDK 21.0.8+9、`publish.py` の SHA-256 に一致する5つのツールを使用します。URL と抽出パスは `workflow.yml`、基準 MPP と Metra 0.9.0-dev.1 MPP の固定 URL / SHA-256 は `manifest-template.json` にあります。

`src/validation/apk-inputs.json` は対応対象の47.4.3、`unsupported-apk-inputs.json` は拒否検査だけに使う47.5.3の原本を固定しています。package / versionName / versionCode / bytes / SHA-256 / 署名証明書を照合します。原本も生成 APK もリポジトリ・ソース ZIP・リリース資産へ入れません。

## 必須検証

| ケース | 入力と構成 | 確認する結果 |
| --- | --- | --- |
| `4743_standalone_native` | 47.4.3 ＋ 単体の native 6 ＋ OneBack | 7項目全成功、全工程成功、出力の native / OneBack / privacy / クラス監査 |
| `4743_aggregate_settings_ja` | 47.4.3 ＋ 総合の Hiro 8 ＋ 上流 Settings | 9項目全成功、上記監査と Settings / 日本語化の実接続 |
| `4753_standalone_rejected` | 47.5.3 ＋ 単体 Hiro 8 の強制選択 | 全項目を版検査で拒否、適用0件、出力 APK なし |
| `4753_aggregate_rejected` | 47.5.3 ＋ 総合 Hiro 8 の強制選択 | 同上 |
| 139項目併用 | 47.4.3 ＋ 上流全136 ＋ 総合の Hiro 3 | 139項目全成功、全工程成功、出力の OneBack と Settings / 日本語化監査 |

併用検査の Hiro 3項目は、ユーザーの画像で選択されていた「怪しい通信だけ遮断」「戻る1回で終了・履歴削除」「TikTok設定を日本語化」です。上流の120項目の内訳は不明のため、上流全136との検査であってユーザーの正確な構成の再現ではありません。3GBヒープ、既定のパッチオプション、互換性の強制無視なしを要求します。このケースの監査範囲は pipeline / OneBack / Settings / 日本語化で、全139機能の実機確認や詳細な privacy 全出力監査は主張しません。

47.5.3 の各構成は8項目を個別に `-f` で選択し、`--continue-on-error` を使用しない計16回の実行です。Morphe は失敗を無視して続行すると再構築へ進むため、各実行の非ゼロ終了・失敗報告・対象1件の版拒否・適用0件・出力なしを必須にします。結果は2つのMPPごとのケースにまとめ、8回ずつの `attempts` と原因を保持します。

さらに、`Back4743NativeBackTest` の表示ラベル非依存・本来の一時画面判定を尊重する戻る処理、`Back4753RejectedTest` の廃止ABI拒否、既存他アプリ回帰を要求します。3 SDK で metadata・匿名依存と本番 admission block を確認します。

## 実行順序

以下は実パスへ置き換えるプレースホルダーです。`BASE` は固定基準 MPP、`ORIGINAL_4743` / `ORIGINAL_4753` は原本、`UPSTREAM` は Metra MPP、`TOOLS` は固定ツールのディレクトリです。各 `*_WORK` は未作成、各 `*_DIST` は別の空ディレクトリにします。

```bash
python3 releases/tiktok4743backfix/src/build.py --base BASE --tools TOOLS --work PILOT_WORK --dist PILOT_DIST
python3 releases/tiktok4743backfix/src/validate_apk.py --apk ORIGINAL_4743 --version 47.4.3 --dist PILOT_DIST --upstream UPSTREAM --tools TOOLS --work REAL_4743_WORK
python3 releases/tiktok4743backfix/src/validate_rejection.py --apk ORIGINAL_4753 --dist PILOT_DIST --tools TOOLS --work REJECTION_WORK
python3 releases/tiktok4743backfix/src/validate_integration.py --apk ORIGINAL_4743 --dist PILOT_DIST --upstream UPSTREAM --tools TOOLS --work INTEGRATION_WORK
python3 releases/tiktok4743backfix/src/merge_apk_proofs.py --dist PILOT_DIST
```

署名検証と実行ログを保存し、各 runner は実結果から証拠を生成します。`merge_apk_proofs.py` は `real-apk-4743.json`、`rejection-apk-4753.json`、`integration-139.json` を読み込み、各 MPP の SHA-256 と必須の結果を検査して `real-apk.json` に結合します。47.5.3の拒否と47.4.3の成功は別 schema で保持します。前版の成功ファイルをコピーして代用しません。

ソースと実行証拠を確定してから、2つの空の作業場所で最終ビルドを行います。

```bash
python3 releases/tiktok4743backfix/src/build.py --base BASE --tools TOOLS --work WORK_A --dist DIST_A
python3 releases/tiktok4743backfix/src/build.py --base BASE --tools TOOLS --work WORK_B --dist DIST_B
python3 releases/tiktok4743backfix/publication/freeze_local.py --first DIST_A --second DIST_B --repo . --reviewed-main REVIEWED_MAIN_SHA --reviewed-dev REVIEWED_DEV_SHA
python3 releases/tiktok4743backfix/publication/verify_pair.py DIST_A DIST_B --repo .
python3 releases/tiktok4743backfix/publication/selftest.py --dist DIST_A
python3 releases/tiktok4743backfix/publication/package.py --dist DIST_A --repo .
python3 releases/tiktok4743backfix/publication/publish.py --dist DIST_A --repo . --base BASE --upstream UPSTREAM --local-only
```

MPP が実 APK に使ったバイトから変わる変更には、実 APK 検証のやり直しが必要です。QA は全 `src` ファイルのハッシュを含むため、証拠だけ追加した場合も最終2ビルドを更新します。`freeze_local.py` は実結果、出力監査、拒否の原因、併用構成、MPPハッシュと2ビルドの一致を確認して期待値を固定します。`verify_pair.py` は現在の GitHub Actions 内だけ CI 検証フラグを設定します。

`selftest.py` は本物の最終証拠のコピーを壊し、版の取り違え、実機確認の偽装、OneBack回帰の欠落、部分成功、47.5.3でのAPK出力、日本語化の拒否漏れ、古いMPPを使った検証、併用条件の変更などを公開ゲートが拒否することを検査します。合格用の架空の APK 証拠は作りません。

## ソースと資産

`package.py` は今回の全 `src` / `publication`、README、LICENSE、NOTICE、実結果の QA を収録します。日本語化の loader は今回の `src/japanese` が対応ソースです。変更しない2,181項目の辞書 runtime の元ソースと再生成手順は、固定コミット `efb5937ddea4fe29abdfe33027cfc0e5d397ae93` の `releases/tiktok-japanese100` と必要な既存ヘルパーを `inherited/japanese` に収録します。

MPP2個、QA2個、ソース ZIP、説明、チェックサムの7資産を作ります。旧リリースの notice は総合版で維持し、今回の notice は `META-INF/hiro-tiktok4743backfix/` に追加します。

## 公開

提出直前に main/dev の HEAD と基準 v1.0.281 のフィードを読み直します。ソースコミットは凍結した `reviewed_main_head` の直下、dev は `reviewed_dev_head` のままであることが必須です。`.github/workflows/tiktok4743backfix-publish.yml` と `publication/workflow.yml` を同一にします。

GitHub Actions は2回の新規ビルドを行い、凍結したローカルの MPP / QA と一致させます。証拠の欠落や変更を拒否した後、下書きへ7資産をアップロードし、再取得したバイトが一致してから公開します。固定コミットの MPP 配布 URL を確認し、main/dev のフィードを原子的に更新します。ULike の運用設定と無関係なファイルを保持し、Manager の TikTok だけが更新対象になることを検査して公開記録を保存します。

既存タグ・配布ブランチ、HEADや基準の変化、途中公開記録がある場合は、重複公開で上書きせず状況を確認してから再開します。
