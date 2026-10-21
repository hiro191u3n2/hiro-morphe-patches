# v1.2.5 / bundle270 の公開

基準は公開済み総合版1.0.269です。固定URLとSHA256は `publish.py` に記録しています。build.pyは `NearMeLayout` のみを変更し、最終DEXから対応するJVM helperを作成します。MPPのentry一覧を変えず、変更entryをmanifest・classes.dex・NearMeLayout.classの3件に限定します。その他121 entriesと62 JVMクラスを保持し、DEXの他クラスはLoaderAuditで定義を比較します。

## ローカル保存リソースの証拠を更新する場合

helper・テスト・MPPが変わったときは、古い4 runsの証拠をそのまま使わず、次の順序で再計測します。ここに示す `BASE`・`TOOLS`・`WORK_*`・`DIST_*`・`REPORT_*`・`PRIVATE_ARCHIVE_*` は実際のローカルパスへ置き換えます。各work/distは新しい空の場所を指定します。

```sh
python3 releases/yahootransit125/src/build.py --prepare-local-evidence \
  --base BASE --tools TOOLS --work WORK_PREPARE --dist DIST_PREPARE
```

この開発用オプションは、通常と同じ最終MPPを生成し、そのMPPでcore・Binding・登録・Manager選択・schedulerの試験を行います。最後の非公開リソース証拠の照合とQA発行の直前で `PREPARED ... No QA file emitted; ...` と表示して終了します。QAを発行しないため、この準備成果物からfreeze・package・publishへ進むことはできません。CIではこのオプションを指定しません。

準備された `DIST_PREPARE/Hiro_Morphe_Patches_v1.0.270.mpp` を、同じビルドで作られた `WORK_PREPARE/core-tests` の `RealResourceRoundtripTest` へ直接渡します。例えば8.4.1とSDK1の組合せは次の4引数です。

```sh
java -ea -cp "WORK_PREPARE/core-tests:TOOLS/morphe.jar" \
  app.hiro.yahootransit.patches.RealResourceRoundtripTest \
  DIST_PREPARE/Hiro_Morphe_Patches_v1.0.270.mpp \
  PRIVATE_ARCHIVE_841.apk WORK_RESOURCE_841_SDK1 REPORT_841_SDK1.json
```

同じ最終MPPで次の4組すべてを、新しいworkとreport出力先を使って実行します。classpathは上記テストクラスと各SDKだけです。MPPは引数で指定し、Yahoo helperや変換用JARをclasspathへ加えません。

| 保存リソースの版 | SDKファイル | 実行結果の例 |
|---|---|---|
| 8.4.1 | `TOOLS/morphe.jar` | `REPORT_841_SDK1.json` |
| 8.5.0 | `TOOLS/morphe.jar` | `REPORT_850_SDK1.json` |
| 8.4.1 | `TOOLS/morphe2.jar` | `REPORT_841_SDK2.json` |
| 8.5.0 | `TOOLS/morphe2.jar` | `REPORT_850_SDK2.json` |

4 runsがすべてPASSしたら、`src/evidence/real-resource-validation.json` の `runs` を今回の4つのreportへ置き換え、`helper_source_sha256` と `test_source_sha256` を最終ソースのSHA256へ更新します。各reportは実入力APK・SDK・出荷MPP・出荷DEXのSHAを記録し、`candidateLoadedDirectly=true`、`testOnlyBridge=false`、`evidenceIdentity=shippedMppSha256` を含む必要があります。APK本体・デコードXML・生成APKはこの証拠JSONへ格納しません。

証拠を保存した後、準備段階とは別のfresh work/distで、次節の通常ビルドを最初から実行します。通常ビルドは今回のソース、4 runsの版とSDKの全組合せ、入力APKの固定SHA、生成したMPPとDEXのSHA、既存機能保持・繰返し適用・再構築結果を照合してからQAを発行します。ソース変更でMPPが変わった場合はprepareと4 runsからやり直します。

## 通常の検証と公開準備

```sh
python3 releases/yahootransit125/src/build.py --base BASE --tools TOOLS --work WORK --dist DIST
python3 releases/yahootransit125/publication/freeze.py --dist DIST
python3 releases/yahootransit125/publication/package.py --dist DIST
python3 releases/yahootransit125/publication/publish.py --dist DIST --base BASE --repo . --local-only
```

`freeze.py` は最終検証を終えたMPP/QAとソースのハッシュを `expected.json` に記録します。QAの版とMPPのSHAが一致しなければfreeze/packageは中止します。期待値を仮作成して公開を進めてはいけません。

依存は `src/tool-sources.json` の公式URLから取得し、指定ハッシュを照合します。公式Manager APKからhostのXZ entryとframework資源を取得します。framework JARは15個のAPK資源だけを固定順序・固定日時のZIP_STORED形式で作り、完成SHAも照合します。

`ManagerJarSelectionTest` は実compiled `PatchPipeline.selectedPatches` を呼び、完成MPPを引数で渡します。classpathはテストと公式hostのみです。`HostRuntimeRegressionTest` は公式framework資源だけを加えて本番loader・scheduler・再構築を通し、基準269と候補270を比較します。検証用Yahooコード、別helper実装、追加bridgeの注入は禁止です。

完全実行用fixtureは `src/fixtures` の独自作成テキストから生成します。新中央ボタンの判定・非表示・通常操作保持・対象不明時の失敗は `ReverseSearchButtonTest` と合成fixtureで確認します。保存済み8.4.1/8.5.0の4回のリソース試験も、最終MPPを直接読み込んだ新しい証拠を必要とします。この4回は中央ボタンが存在しない旧レイアウトでの既存機能保持試験です。

CIは固定JDKとツールで独立して2回ビルドし、MPP/QAのバイト一致とローカル検証済みの生成物との一致を確認します。source identity、Manager選択・scheduler、中央ボタン・広告・Binding、DEXと他JVMクラスの保持、公開パッケージの内容を照合するまで公開しません。

公開タグは `yahootransit-v1.2.5`、固定配布branchは `release/yahootransit125` です。Release assetsと不変URLから実bytesを取得して照合し、main/devは期待HEADを確認して `git push --atomic` で同時更新します。`ULike_ACTIVE.json` と他アプリの内容を保持します。

原本8.5.6 APKと新機能の実機表示は未検証です。元APK・実XML・画像・端末ログ・合成APKは公開物に含めません。
