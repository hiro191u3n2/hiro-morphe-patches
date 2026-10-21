# Yahoo!乗換案内 JVM実行互換修正 v1.2.4 / Hiro v1.0.269

## 実行されなかった原因

利用者のログはManager 2.0.0-dev.15、Patcher 2.0.0-dev.22、Yahoo!乗換案内8.5.6、runtime=jvmでした。UI選択数は1ですが、patch=2ms、DEX mode NONEでリソースの再構築が行われていませんでした。

公式ManagerではUIの一覧をloadPatchesFromDexで作り、JVM側のHostMainはloadPatchesFromJarを使います。公開268はYahooをDEXにのみ収録していたため、前者で選べても後者には登録がありませんでした。PatchPipeline.selectedPatchesが空集合を許すため、広告処理を一度も呼ばずに成功終了できていました。

実Manager APKから抽出した未改変のpatcher-host.jarで、公開268の登録7件・Yahoo選択0件と、修正版269の登録8件・Yahoo選択1件を再現しています。既存7登録は保持されます。前回のテストは検証用JVMクラスを足したbundleを読んでいたため、配布形式の欠落を検出できませんでした。

## 変更

総合版1.0.268のclasses.dexを固定dex2jarで変換し、Yahoo namespaceの29個のJVM classを同梱します。Android DEXと既存34 JVM class、その他既存entryは完全に保持します。既存entryの変更はmanifestの版・説明だけです。広告非表示の実装自体はv1.2.3と同じで、内部のYahoo ads v1.2.3という診断メッセージも維持されます。

## 配布ファイルそのものの検証

1. 実Managerのcompiled PatchPipeline.selectedPatchesに、旧公開MPPと完成MPPをそのまま渡します。コードの取得元が候補MPP自身であること、system classpathにYahooの補助実装がないことも検査します。
2. 本番JAR loader・Patcher schedulerで、旧MPPは選択0件・変更なし、修正版は広告ResourcePatchとBytecode依存の2ノードを実行し成功結果1件を返すことを検査します。資源を再構築して再読込し、広告枠・通常操作・合成DEXの保持を確認します。
   実Managerがhostへ追加する公式APK内のframework資源15件も、同一bytesを資源専用JARとして与えます。実装コードは未改変の公式hostだけで、Yahooコードは完成MPP自身から読み込みます。
3. 完全な実行用のAPKは、公開された独自作成のXML4ファイルと空のJava2クラスから固定aapt2/D8で生成します。元アプリのAPK・XML・DEXは使いません。
4. 完成MPP内のJVMコードで、広告・NearMe・既存パッチ入口・公式Data Bindingの回帰検証も行います。追加のテスト用Yahoo JARやbundleは生成しません。
5. 実行登録はMorphe Desktop 1.16.0 / 1.19.0-dev.4 / 1.19.0-dev.19で検証します。最後の版はPatcher2.0.0-dev.22です。Manager実hostのPatcher classesもこの版と同一であることを調査時に確認しました。
6. CIの独立した2回のビルドがローカルで確認したMPP/QAと一致した場合だけ、Releaseとmain/devフィードを公開します。

## 証拠の範囲

今回の8.5.6は合成fixtureの宣言版です。利用者の実8.5.6 APKやAndroid実機での表示を検証した結果ではありません。これらは未取得・未確認です。

src/evidenceの8.4.1/8.5.0の実リソース検証は前回のローカル結果です。今回のAndroid DEXが完全同一であるため参考証拠として保持しますが、新しいManagerの選択経路の証明には使いません。その経路は今回の実Managerテストで別途検証します。過去の改造済みAPKにも原本DEXは含まれていません。

公式Data Bindingのテストは未改変の探索処理をホストView stub上で実行します。実アプリの生成Bindingクラス・Androidのinflation/描画の試験ではありません。

依存URLとSHA256はsrc/tool-sources.json、再ビルドと公開手順はpublication/README.md、全結果はQA_YahooTransit_v1.2.4.jsonを参照してください。
