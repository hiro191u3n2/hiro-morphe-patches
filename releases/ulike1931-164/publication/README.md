# ULike 1.9.31 / Hiro Morphe Patches 1.0.164 公開手順

`publish1931.py` はULike 1.9.30の公開処理を基にしています。ネットワーク・配布ブランチ作成部分には総合1.0.163の自己完結した実装を使用し、古い `rollback142.py` への実行時依存を取り除いています。

## 固定基準

| 基準 | SHA-256 | bytes |
| --- | --- | ---: |
| `ULike_HQ_Texture_Online_v1.9.30.mpp` | `8b7de29e47362d8e2291b5c2d506dd85688279443269ee8d854513d4cae5d723` | 706227 |
| `Hiro_Morphe_Patches_v1.0.163.mpp` | `d8fa1b78392803316a73a65c791fc5dd5e56e50a45884dbaf05097190230f1b4` | 17353320 |

ULikeの現行系統は1.8.8から継承した1.9.30です。1.9.17の破棄履歴と既存の他アプリを保持します。総合の基準には1.0.163を使います。

## 完成後に確定する値

`manifest.template.json` は意図的に未完成です。null値、空の変更リスト、未確認フラグを残したままでは公開できません。完成したビルド・実APK適用結果・生成DEXの保持検証に合わせて値を確定し、`releases/ulike1931-164/manifest.json` に保存します。

- 単体MPP、総合MPP、QA、ソースとQAのZIP、リリースノート、SHA256SUMSの6ファイルのサイズとSHA-256。
- 単体・総合それぞれの実際の変更エントリ。追加・削除は想定しません。変更可能な範囲はmanifest、ULike loader、runtime、native method payloadとその索引です。
- 実際に変えたruntime/nativeメソッドと追加helperクラスの完全な一覧。
- native payload・索引が基準と同じかどうかの実測値。既存1.9.30の「同一」フラグをそのまま引き継ぎません。
- カメラ寿命の呼出経路には前面再試行取消を追加するため、ACTIVEの `camera_lifecycle_byte_identical` と `all_existing_runtime_unchanged` はfalseへ更新します。保持したprovider寿命管理と黒帯操作はそれぞれ別のQA・ACTIVEキーで表します。
- 配布ZIPに必要な実装・ビルド・検証ソースのパス。QAの `source_sha256` とZIP内の実バイトが一致する必要があります。
- 実APK検証の実施結果、画質処理・背面プレビュー再試行・他アプリの保持、前面プレビュー再試行・解放済みtexture更新・deferred surface準備待ちの検証結果。

公開スクリプトは未確認の実機成功を受け付けません。`device_tested` と `device_fix_confirmed` は今回の検証範囲ではfalseです。

## ローカル検証

リポジトリのルートで、完成した配布物と固定基準MPPを指定します。

```sh
python3 releases/ulike1931-164/publication/publish1931.py \
  --dist dist \
  --repo . \
  --expected releases/ulike1931-164/manifest.json \
  --baseline input/Hiro_Morphe_Patches_v1.0.163.mpp \
  --standalone-baseline input/ULike_HQ_Texture_Online_v1.9.30.mpp \
  --local-only
```

`--local-only` はネットワークとGitを使用しません。全6資産、QA契約、基準との差分、ULike単体と総合の同一性、他アプリの保持、ソースZIPとQAのハッシュを検証します。

`--preflight-only` では、同じ検証に加えてmain/devの現在のhead・feed・ULike ACTIVE policy、Release/tag/配布ブランチの有無を読み取ります。リモートへは書き込みません。

## GitHub Actions

`ulike1931-164-publish.yml` を `.github/workflows/ulike1931-164-publish.yml` に同一バイトで置くと、mainへの当該workflowの変更で実行します。ソース・検証・確定manifestを先にそろえます。

workflowはJava21と固定Morphe/SDK/D8を使用し、`src/build1931.py`、`src/finalize1931.py`、`evidence/validation.json` を実行・参照します。再ビルドMPPがローカルの検証済みMPPと同じであることを公開条件にします。ツール取得元は既存の `ulike1925-baseline-and-tools` artifact（run 37412171927）です。

公開処理は以下を順に行います。

1. main/devの基準がともに1.0.163であることと、現在のULikeが1.9.30であることを確認します。
2. draft Releaseへ6資産をアップロードし、ダウンロードしたSHAを照合してから公開します。
3. 専用配布ブランチへMPPを置き、コミットSHAを含む不変のURLから実バイナリを確認します。
4. main/dev両方へ、各旧headを親とするコミットを作ります。`git push --atomic` と旧headのleaseを組み合わせ、途中の別更新や片方だけの反映を防ぎます。
5. `patches-bundle.json`・`CHANGELOG.md`・`ULike_ACTIVE.json` を両ブランチで確認し、Manager配布URLのMPPを再照合します。
6. 公開receiptを両ブランチとReleaseへ保存します。

Manager向け履歴にはULikeのみのscopeを追加します。1.9.30を収録した総合1.0.159と直前総合1.0.163のいずれからも再パッチ対象となり、総合1.0.164で再適用した後は今回変更だけで古いと判定されません。旧履歴と他アプリの説明を保持します。

Release/tag/配布ブランチが存在する場合、または途中でheadが進んだ場合は自動で上書きせず停止します。receiptと公開済み資産を確認してから再開方法を決めます。ここでの停止はユーザーへの公開許可の再質問を要求するものではありません。

元APKS、生成APK、署名鍵、ユーザーの添付画像を配布物へ含めません。
