# Piko NewX 日本語化 v1.0.0 / Hiro Morphe Patches v1.0.378

X `com.twitter.android` **12.43.0-prod.01** に追加される Piko NewX の設定・メニューを日本語化するパッチです。Piko **3.12.0-dev.1 / 3.12.0-dev.2** と Morphe Patcher **2.4.0** で検証しています。

## 使い方

1. Morphe Manager で **Hiro Morphe Patches のソースを v1.0.378 に更新**します。
2. **未改造の X 12.43.0-prod.01 の APKM 一式**を選択します。
3. Piko で普段使う **`NewX:` のパッチ**を選び、Hiro の **`NewX: Pikoの設定・メニューを日本語化`** を追加で選択します。日本語化は初期状態では選択されません。
4. Hiro のカテゴリ保持・絶対日時・戻る操作も使用する場合は、**旧 Hiro の3項目を解除し、`NewX:` が付く3項目を使用**します。
5. パッチを適用し、生成された APK をインストールします。X のアプリ言語、または端末の表示言語を日本語にしてください。言語を変更した後は X を完全に終了して起動し直します。

ソースの更新だけでは、インストール済みの X は変わりません。元の APKM からパッチを適用し直してください。

総合版 `Hiro_Morphe_Patches_v1.0.378.mpp` に日本語化を収録しています。単独版 `Piko_NewX_Japanese_v1.0.0.mpp` を使用する場合も Piko NewX が必要です。総合版と単独版で同じ日本語化パッチを重複選択しないでください。

配布ページ: https://hiro-morphe-patches.otoha10.chatgpt.site

更新ソース: https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/main/patches-bundle.json

## 翻訳範囲

| 対象 | 内容 |
|---|---|
| Piko NewX の設定 | 485 件の日本語文字列を追加 |
| メニュー・画面・通知 | 176 件の補助文字列を追加し、30 クラス内の確認済み 224 箇所から参照 |
| 主な画面 | トピック選択、非表示にした返信、メディア保存、翻訳、言語選択、オブジェクト表示、クラッシュログ操作 |
| 英語表示 | 既存の英語設定と補助文字列の英語を保持。日本語以外では元の言語リソース／英語へフォールバック |

内部で処理を分岐する識別子、API URL、ユーザーの投稿・入力内容、ログ本文、プロトコル名や単位は変更しません。`Image` / `Video` はメディア判定にも使われるため、この版では英語のままです。翻訳先の言語名など一部の表示は起動中に保持されるため、言語変更後は再起動が必要です。

**Morphe Manager に表示される Piko 側のパッチ一覧の名前・説明は対象外**です。日本語化の対象は、パッチ適用後の X 内に追加される Piko NewX の画面です。

## 検証範囲

公開 QA に、実 APKM への適用・APK 再構築、完成したリソースの日本語と英語、224 箇所の DEX 呼び出し、既存機能の保持に関する検証結果を収録します。対象ケースは Piko dev.1、dev.2、dev.2＋Clone、単独日本語化版＋NewX Remove ads の4通りです。

日本語化はクラス・メソッド・英語文字列・出現数を照合して適用します。対応していない Piko、Piko NewX がない構成、二重適用は検出して停止します。総合版への統合では v1.0.377 の既存プロバイダと実行拡張を保持します。

**Android 実機・エミュレーターでの起動や画面操作は未検証です。** APK の再構築と静的監査に成功したことを、実機での動作確認とは区別しています。確定した結果は `QA_Piko_NewX_Japanese_v1.0.0.json` と `QA_build378.json` を参照してください。

## ソースと再ビルド

ソース ZIP にはパッチ、翻訳、ビルド・監査コード、QA、ライセンスを収録します。X の APK/APKM、コンパイラ、ツール JAR、DEX は含みません。必要な入力とツールの SHA-256・取得元は `toolchain.json` を参照してください。

ソース ZIP を `piko-ja-port/` に展開し、その親に次を用意します。

| パス | 内容 |
|---|---|
| `tools/morphe.jar` | Morphe Desktop 2.2.0 / Patcher 2.4.0 |
| `tools/ecj.jar`, `tools/d8.jar`, `tools/classpath.jar` | `toolchain.json` に記載したコンパイラと統合用クラスパス |
| `inputs/X_12.43.0-prod.01.apkm` | 未改造の入力 APKM |
| `inputs/piko-3.12.0-dev.1.mpp`, `inputs/piko-3.12.0-dev.2.mpp` | 検証対象の Piko |
| `inputs/Hiro_Morphe_Patches_v1.0.377.mpp` | 総合版の統合元 |
| `verification/hiro-piko.apk` | v1.0.377 の NewX 3項目と Piko dev.1 の対象46項目を適用した、日本語化前の比較用 APK |

比較用 APK は、元 APKM に v1.0.377 の NewX 3項目と `piko-dev2-selected.json` の Piko 46項目を、Morphe CLI の `patch --exclusive --unsigned --bytecode-mode FULL` で適用して作成します。対象の選択名と CLI 呼び出し形式は `verification/verify_packages.py` にあります。比較用 APK の SHA-256 は公開 QA に記録しています。

Java 21、Python 3.12 を使用します。以下は展開先 `piko-ja-port/` から実行します。

```bash
python3 patch/build.py \
  --tools ../tools --patcher ../tools/morphe.jar \
  --apk ../verification/hiro-piko.apk \
  --piko ../inputs/piko-3.12.0-dev.2.mpp

python3 build/integrate.py \
  --base ../inputs/Hiro_Morphe_Patches_v1.0.377.mpp \
  --component patch/component.mpp \
  --tools ../tools --patcher ../tools/morphe.jar --out dist

python3 verification/verify_packages.py
python3 verification/run_completed_apk_audits.py --ecj ../tools/ecj.jar
python3 verification/audit_preservation.py dev1 --ecj ../tools/ecj.jar
```

`patch/build.py` は翻訳マップから Java と Android リソースを生成し、ホスト監査後に単独 MPP を作成します。`build/integrate.py` は v1.0.377 の既存 ZIP レコードを保ち、追加プロバイダを DEX に統合します。監査コードの AAPT2 などの実行条件は各スクリプトの引数を参照してください。公開用ソースの組み立ては `prepare_publication_sources.py` で行い、最終 QA が PASS の場合だけコピーと ZIP 作成を許可します。

## 出典・ライセンス

Piko: https://github.com/crimera/piko

参照したソースは v3.12.0-dev.2、commit `7acb08ae14de946e3b6b5a9837a860a7b2667caf` です。NewX の英語・日本語リソースと NewX/shared 拡張は dev.1 と dev.2 で同一であることを確認しています。共有 UI の一部は `app.crimera:piko-extension-library:0.10.1-dev.5` に由来します。

Piko 由来のリソース・文言と本追加パッチの配布には GPLv3 を適用します。`inventory/upstream-license/LICENSE` と `inventory/upstream-license/NOTICE` を保持してください。`NOTICE` は Piko の帰属表示を定めています。ビルド用にも同一内容の `upstream/LICENSE` と `upstream/NOTICE` を収録しています。
