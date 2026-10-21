# Yahoo!乗換案内 v1.2.1 の公開

基準は公開済み総合版1.0.263です。CIは本番SDKと固定ツールで2回の独立したビルドを行い、MPPとQAが一致した場合にGitHub ReleaseとMorphe Managerのmain/devソースを更新します。

```sh
python3 releases/yahootransit121/src/build.py --base BASE --tools TOOLS --work WORK --dist DIST
python3 releases/yahootransit121/publication/freeze.py --dist DIST
python3 releases/yahootransit121/publication/package.py --dist DIST
python3 releases/yahootransit121/publication/publish.py --dist DIST --base BASE --repo . --local-only
```

ビルドの必須出力は`Hiro_Morphe_Patches_v1.0.264.mpp`と`QA_YahooTransit_v1.2.1.json`です。QAは`result=PASS`、`patch_version=1.2.1`、`bundle_version=1.0.264`、`baseline_version=1.0.263`、`bundle_sha256`、`physical_android_tested=false`を含みます。

`freeze.py`は最終ソースとローカル検証済みMPP・QAのSHA256を`publication/expected.json`へ記録します。CIはこのファイルを更新せず、ソースと2回のビルド結果の一致を確認します。ソースに変更がある場合は変更後のビルドを検証し直して記録します。

公開タグは`yahootransit-v1.2.1`、固定配布ブランチは`release/yahootransit121`です。main/devは期待するHEADを確認し、同一のgit push --atomicで進めます。既存のULike_ACTIVE.jsonは変更しません。入力APK・添付画像・端末ログは公開物に含めません。
