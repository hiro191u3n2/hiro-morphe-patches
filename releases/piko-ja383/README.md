# Piko dev.3 対応修正 / Hiro Morphe Patches v1.0.383

X `com.twitter.android` **12.43.0-prod.01** と Piko **3.12.0-dev.3** の組み合わせで、日本語化と表示設定のソース確認が「未検証のPikoソースです」で停止する問題への修正です。

## 変更

- 「NewX: Pikoの設定・メニューを日本語化」の対応一覧へ Piko 3.12.0-dev.3 を追加。単独版は `Piko_NewX_Japanese_v1.0.1.mpp`。
- 「NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）」にも同じ対応を追加。単独版は `NewX_UI_Options_v1.1.3.mpp`。
- 未対応ソースで停止した場合は、選択されたソース名・バージョンと対応バージョンを表示。
- dev.1 / dev.2 の対応、未改造 X 12.43.0-prod.01 の確認、単一 Piko ソースの確認、対象クラス・メソッド・文字列数の検証を保持。

翻訳内容は v1.0.378 と同じ485件の設定文字列と176件の補助文字列です。224箇所の翻訳対象、表示設定の4機能、設定値、スクロール処理は変更しません。表示設定の実行用拡張とリソースは v1.1.2（Hiro v1.0.382）のバイト列をそのまま使用します。

## 使い方

Hiro Morphe Patches のソースを v1.0.383 に更新し、未改造の X 12.43.0-prod.01 APKM へ、普段使う Piko NewX 項目と Hiro の項目を再適用してください。ソースの更新だけではインストール済み X は変わりません。

日本語化と表示設定は初期状態では未選択です。単独版を使用する場合、総合版と同じ項目を二重に選択しないでください。

## 検証の記録

`patch/QA.json` は日本語化プロバイダの新規ホスト検証です。公式 Piko dev.1 / dev.2 / dev.3 の実際の依存関係を読み込み、対応判定、未対応バージョン、別ソース名、欠落したソース情報、混在ソースの拒否を確認します。dev.3 の公式 NewX/shared 拡張で224箇所の翻訳と DEX 再生成を検証し、対象外の550メソッドが同一であることも確認しています。

`ui/QA.json` は表示設定の対応判定とバイト保存の新規検証です。公式3バージョンの選択と不正なソースの拒否を確認し、実行用拡張を再コンパイルしていないこと、v1.1.2 の実行用拡張・リソースが同一であることを記録します。

APK 全体の適用・再構築は、dev.3通常構成（51項目）、dev.3＋Clone（52項目）、dev.2通常構成（51項目）、日本語化・表示設定の単独MPP併用（3項目）の4構成で成功しました。完成APKの日本語・英語リソース1322件と、翻訳呼び出し224箇所を各構成で監査しています。dev.2では今回生成したAPKがv1.0.382の完成APKと全バイト一致しました。確定記録は `verification/final-validation.json` と各ケースのレポートです。

全APK検証は公式Desktop 2.3.0-dev.2内蔵のPatcher **2.5.0-dev.1**で実施しました。ユーザー環境のPatcher2.5.0-dev.2とManager2.7.0-dev.4での操作は未確認です。**Android実機・エミュレーターでの起動、インストール、画面操作は未検証**です。

## プロバイダの再ビルド

Java 21、Python 3、ECJ、D8、および Morphe Patcher 2.4 API を含む Desktop JAR が必要です。使用ツールと入力の SHA-256 は `toolchain.json` と各 QA に記録します。公式 Piko MPP の `extensions/newx.mpe` と `extensions/shared.mpe` を検証入力として抽出してください。実 APK はこのホスト検証に含めません。

```bash
python3 patch/build.py --tools /path/to/tools --patcher /path/to/morphe24.jar \
  --extension /path/to/newx-dev3.mpe --extension /path/to/shared-dev3.mpe \
  --piko /path/to/patches-3.12.0-dev.1.mpp \
  --piko /path/to/patches-3.12.0-dev.2.mpp \
  --piko /path/to/patches-3.12.0-dev.3.mpp

python3 ui/build.py --base /path/to/NewX_UI_Options_v1.1.2.mpp \
  --tools /path/to/tools --patcher /path/to/morphe24.jar \
  --piko /path/to/patches-3.12.0-dev.1.mpp \
  --piko /path/to/patches-3.12.0-dev.2.mpp \
  --piko /path/to/patches-3.12.0-dev.3.mpp
```

日本語化は `patch/component.mpp`、表示設定は `ui/component.mpp` に出力します。日本語化のホスト検証には、拡張入力の代わりに `--apk /path/to/already-piko-patched.apk` も使用できます。`build/integrate.py` が総合版へ統合します。

```bash
python3 build/integrate.py --base /path/to/Hiro_Morphe_Patches_v1.0.382.mpp \
  --component patch/component.mpp --ui-component ui/component.mpp \
  --tools /path/to/tools --patcher /path/to/morphe24.jar --out dist
```

APK検証用ワークスペースには `tools/morphe25.jar`、`tools/ecj.jar`、`inputs/X_12.43.0-prod.01.apkm`、`inputs/Hiro_Morphe_Patches_v1.0.382.mpp`、`upstream-piko/patches-3.12.0-dev.{1,2,3}.mpp` を配置します。`verification/verify_packages.py <case> --workspace /path/to/workspace` で各構成を生成し、`verification/run_release_apk_audits.py --qa /path/to/workspace/qa --tools /path/to/workspace/tools <case>` で完成APKを監査できます。APKやツール本体はソースZIPに含みません。

## 出典とライセンス

公式 Piko: https://github.com/crimera/piko/releases/tag/v3.12.0-dev.3

dev.3 の NewX/shared 拡張と設定リソースは dev.1 / dev.2 と同じであることを確認しています。翻訳の初期実装と調査資料は `../piko-ja378/` に保持されています。Piko 由来のリソース・文言と追加パッチは GPLv3。`upstream/LICENSE` と `upstream/NOTICE` を保持してください。
