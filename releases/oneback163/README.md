# 戻る1回で終了・履歴削除 v1.0.0

Hiro Morphe Patches v1.0.163へ追加する共通パッチです。Androidのナビゲーションの「戻る」で対象アプリの画面を終了し、そのアプリ自身のタスクを「最近使ったアプリ」から削除します。

| アプリ | 対応パッケージ |
| --- | --- |
| TikTok | `com.ss.android.ugc.trill`, `com.zhiliaoapp.musically` |
| Instagram | `com.instagram.android` |
| X / Twitter | `com.twitter.android` |
| Trip.com（torip） | `ctrip.english` |

## 適用

Morphe ManagerでHiro Morphe Patchesのソースをv1.0.163へ更新し、各アプリの元APK／APKSに「戻る1回で終了・履歴削除」を選択して再パッチしてください。初期選択はオンです。広告除去などの既存パッチも使う場合は、以前選択していたパッチと一緒に適用します。

ソースを更新しただけではインストール済みアプリは変わりません。アプリごとに再パッチ・再インストールが必要です。同じMorpheの署名鍵を使って更新してください。総合版と単体版は同じ共通パッチを含むため、どちらか一方を使用します。

このパッチにより、ナビゲーションの「戻る」は前の画面への移動ではなく終了操作になります。「履歴」はAndroidの「最近使ったアプリ」のタスク一覧を指します。

## 実装

`src/patch/OneBackPatch.java`がActivity、Dialog、Viewの継承関係を調べ、実際のコールバックとライフサイクルへランタイムを接続します。Activityとapplicationの`android:enableOnBackInvokedCallback`を有効にし、Android 13以降の`OnBackInvokedDispatcher`へ接続します。Android 16の戻る操作を想定した経路に加え、旧来のキー入力、ダイアログ、入力欄のIME前のキー入力にも対応しています。

`src/runtime/OneBackExit.java`がアプリ自身のタスクを列挙し、各タスクのパッケージを確認して`finishAndRemoveTask()`を呼びます。Activityの終了とタスク削除を組み合わせ、別アプリから起動された場合も別アプリのタスクは削除しません。登録したコールバックは画面のフォーカス・ライフサイクルに追随し、終了後に同じプロセスで再起動する場合も扱います。

元のキー処理は専用helperへそのまま移し、Back以外のキーは元の処理へ委譲します。画面を表示している間の通常の履歴設定や、アプリのSDK・権限・バージョンは保持します。

## 検証範囲

| 対象 | 実施した検証 |
| --- | --- |
| ランタイム | 実SDKに対してコンパイルした本体を使い、20ケース・137アサーションのホスト試験 |
| DEX変換 | 継承、レジスタ、既存メソッド保持、衝突時の拒否、Manifest再シリアライズとDEX再エンコードを含む12カテゴリ・2,382アサーション |
| 単体・総合MPP | 5パッケージそれぞれへ実Morphe CLIで適用する10回の合成APK試験・74,320アサーション。出力DEX、Manifest、リソースも検証 |
| 既存総合版 | v1.0.162のloader 246クラスを意味的に完全保持し、既存の58資産エントリをバイト単位で保持 |
| Xの入手済みAPK | 元241,847クラスを全件照合。DEX処理内容1,978,177アサーション、Manifest408アサーション、全18DEXの構造・チェックサム・クラス保持を検証 |
| Trip.comの元APK | 元85,229クラスを全件照合。DEX処理内容8,111,718アサーション、Manifest245アサーション、全10DEXの構造・チェックサム・クラス保持を検証 |
| Trip.comの既存パッチとの同時適用 | 両パッチの適用・再構築、元85,229クラス保持、追加19＋既存Trip用2クラス、全10DEX正常、Manifest245アサーションを確認 |

Xの入力は以前パッチ済みの`12.19.1-release.0`です。Trip.comの入力はユーザー提供の元split APK一式を結合したものです。TikTokとInstagramは元APKを入手できていないため、合成APKでの適用試験です。Android実機上の操作、履歴画面、Morphe Managerの更新バッジ表示は未確認です。適用成功や静的検証を実機動作確認と同一視しません。

詳細は`src/reviewed-qa.json`と`src/validation/`の記録を参照してください。配布物には元APK、再構築したアプリ本体、署名鍵を含めません。

Trip.comをデスクトップのMorphe FULLモードで並列再構築した試行では、一部DEXのヘッダーがゼロで出力される不具合を検出しました。これらの出力は不合格として除外しています。同じMPPを使いJavaへ`-XX:ActiveProcessorCount=1`を指定した最終出力は、単独適用・既存Tripパッチとの同時適用ともにすべてのDEXが正常で、元のクラスを欠落なく保持しています。単独適用については上記の全メソッド監査も完了しています。並列時の根本原因は未特定で、Android版Managerで同じ問題が起きるかは未確認です。診断の経緯と最終判定はQAに分けて記録しています。

## 再現ビルド

Java 21と、`src/build.py`内のSHA-256へ固定した`morphe.jar`、SDK 36の`android.jar`、`d8.jar`を使用します。ベースMPPはv1.0.162の固定コミットにあるファイルで、サイズとSHA-256を照合してから使用します。

```sh
python3 src/build.py \
  --base inputs/Hiro_Morphe_Patches_v1.0.162.mpp \
  --tools tools --work build --dist dist
```

ホスト試験、DEX自己検証、loaderの追加マージ、単体・総合MPPの対応パッケージ認識をビルド時に実行します。レビュー済みQAと公開manifestが存在する場合、ソースとQAの配布ZIPも作成します。既に検証したMPPを使って配布物を再作成する場合は`--package-only`を追加します。

合成APKの統合試験は次のように再現できます。

```sh
java -cp build/auditclasses:build/patchclasses:tools/morphe.jar \
  Fixtures create build/fixture-original.dex fixtures
python3 src/test-dex/run_synthetic.py \
  --java /path/to/jdk21/bin/java --morphe tools/morphe.jar \
  --build build --dist dist --fixtures fixtures \
  --out synthetic-runs --report synthetic-validation.json
```

実APKのManifest検証には独立した`src/validation/VerifyAppManifest.java`をコンパイルして使います。実APK自体は配布しないため、元入力を別途用意する必要があります。

```sh
javac -cp tools/morphe.jar -d validation-classes src/validation/VerifyAppManifest.java
java -cp validation-classes:tools/morphe.jar VerifyAppManifest original.apk patched.apk manifest-qa.json
python3 src/validation/verify_dex_inventory.py original.apk patched.apk dex-inventory.json
java -cp build/auditclasses:build/patchclasses:tools/morphe.jar \
  DexAudit audit-app original.apk patched.apk dex-qa.json
```

## 公開

GitHub Actionsが固定ベースから再ビルドし、レビュー済みMPPのSHA-256一致を条件として公開します。Releaseと固定コミットのダウンロードを検証してから、main/dev両方のMorphe Manager用フィードを同時に更新します。変更履歴の更新対象はTikTok、Instagram、X/Twitter、Trip.comです。

公開手順は`publication/oneback163-publish.yml`と`publication/publish163.py`、期待する配布物の値は`publication/manifest.json`にあります。既存の別アプリのパッチと更新履歴を保持します。
