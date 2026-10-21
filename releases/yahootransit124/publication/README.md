# v1.2.4 / bundle269 の公開

基準は公開済み総合版1.0.268です。build.pyはそのAndroid DEXと既存entryを保持し、同じDEXから変換したYahoo JVM29クラスを含む完成MPPを作成します。

```sh
python3 releases/yahootransit124/src/build.py --base BASE --tools TOOLS --work WORK --dist DIST
python3 releases/yahootransit124/publication/freeze.py --dist DIST
python3 releases/yahootransit124/publication/package.py --dist DIST
python3 releases/yahootransit124/publication/publish.py --dist DIST --base BASE --repo . --local-only
```

依存はsrc/tool-sources.jsonの公式URLから取得し、指定ハッシュを照合します。実Manager hostは公式Manager APK内の指定XZ entryから抽出します。fixtureはsrc/fixturesの公開テキストだけから作成し、生成APKを公開しません。

ManagerJarSelectionTestは実compiled PatchPipeline.selectedPatchesを呼びます。HostRuntimeRegressionTestは本番loaderとschedulerを通します。両方とも完成MPPを引数に渡し、classpathは各テストクラスと公式hostだけです。検証用Yahooコードやbridgeの追加は禁止です。

freeze.pyは最終ソースとローカル検証済みMPP/QAのハッシュをexpected.jsonへ記録します。CIは固定JDKとツールで2回ビルドし、ソースと生成物の一致を確認します。Manager選択・実行・再構築を含む全検証が通るまで公開しません。

公開タグyahootransit-v1.2.4、固定配布branch release/yahootransit124。main/devは期待HEADを確認してgit push --atomicで同時更新し、ULike_ACTIVE.jsonは保持します。原本APK、合成APK、実XML、端末ログは公開物に含めません。
