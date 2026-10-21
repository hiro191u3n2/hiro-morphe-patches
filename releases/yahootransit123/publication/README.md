# Yahoo!乗換案内 v1.2.3 の公開

基準は公開済み総合版1.0.267です。CIは本番SDKと固定ツールで2回の独立したビルドを行い、MPPとQAが一致した場合にGitHub ReleaseとMorphe Managerのmain/devソースを更新します。

```sh
python3 releases/yahootransit123/src/build.py --base BASE --tools TOOLS --work WORK --dist DIST
python3 releases/yahootransit123/publication/freeze.py --dist DIST
python3 releases/yahootransit123/publication/package.py --dist DIST
python3 releases/yahootransit123/publication/publish.py --dist DIST --base BASE --repo . --local-only
```

ビルドの必須出力は`Hiro_Morphe_Patches_v1.0.268.mpp`と`QA_YahooTransit_v1.2.3.json`です。QAは`result=PASS`、`patch_version=1.2.3`、`bundle_version=1.0.268`、`baseline_version=1.0.267`、`bundle_sha256`、`physical_android_tested=false`を含みます。

`freeze.py`は最終ソースとローカル検証済みMPP・QAのSHA256を`publication/expected.json`へ記録します。CIはこのファイルを更新せず、ソースと2回のビルド結果の一致を確認します。ソースに変更がある場合は変更後のビルドを検証し直して記録します。

公開タグは`yahootransit-v1.2.3`、固定配布ブランチは`release/yahootransit123`です。main/devは期待するHEADを確認し、同一のgit push --atomicで進めます。既存のULike_ACTIVE.jsonは変更しません。入力APK・添付画像・端末ログは公開物に含めません。

検証は登録済みexecuteの2 SDK実行、未改変の公式Data Binding 8.13.2での探索、出荷DEXに一致する過去8.4.1/8.5.0の実リソース4組の証跡を必須とします。実リソース検証はローカルで実施し、CIは証跡・helper・テスト・DEXの一致を検査します。元APKにはDEXがないため完全な原本適用や実機動作の確認ではありません。入力APKを公開またはCIにアップロードしません。
