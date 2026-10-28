# TikTok Link Direct のビルドと公開

新しい追加パッチ v1.0.0 と総合 v1.0.307 を、公開済み総合 v1.0.306 に追加します。
既存パッチの JVM クラスと全リソースは同じバイトで保持し、DEX ローダーの既存クラス定義もデバッグ情報を含めて照合します。
過去の TikTok／OneBack 実装を再コンパイルして差し替えません。

## 入力

- Temurin JDK 21.0.8+9。`java` と `javac` の実行結果で版を検査します。
- `src/build.py` の `TOOLS` と一致する Morphe 1.16.0、Morphe 1.19.0、Android 35 API、D8。
- 同ファイルの `BASE_URL` / `BASE_SHA` と一致する公開済み総合 v1.0.306。
- 元の global TikTok 47.4.3。実入力の署名とハッシュは `publication/publish.py` の `INPUT` に固定しています。

`publication/stage.py --root STAGED` で公開済み基準 MPP とツールを取得できます。
この処理は元 APK を取得せず、リポジトリも更新しません。

## ビルドと実 APK 検証

```bash
python3 releases/tiktok-link-direct100/src/build.py --base BASE --tools TOOLS --work PILOT_WORK --dist PILOT_DIST
```

`src/patch/*.java` が実装です。ランタイムを追加する場合だけ `src/runtime/*.java` をコンパイルし、`extensions/hiro_tiktok_link_direct.mpe` にします。
パッチクラスの名前空間は `app.hiro.tiktoklink.patches` 専用です。
実際の Morphe JVM ローダーを2版で起動し、追加項目が1個、対象が global 47.4.3 のみ、既存メタデータが変わらないことを確認します。

次の runner は元 APK の署名・ハッシュを検証し、単体と総合から新規項目だけを選んで実際に適用・再構築します。出力の12クラス全メソッド、専用判定の実命令、3つの利用箇所の分岐を監査します。別版・別パッケージの強制選択拒否も本番アクションで検査します。

```bash
python3 releases/tiktok-link-direct100/src/validate_apk.py --apk ORIGINAL_APK --dist PILOT_DIST --tools TOOLS --work NEW_APK_WORK
```

原本の入手ページ: https://apkpure.net/tiktok-app/com.zhiliaoapp.musically/download/47.4.3 。配布元を信頼するだけでなく、固定SHA-256とTikTokの署名証明書を照合してください。

最終 MPP の両方を元 APK へ適用して、出力 APK の追加処理を監査してください。
実行結果は `src/validation/real-apk.json` へ保存します。公開処理が要求する schema は `tiktok-link-direct-real-apk-v1` です。
必要なフィールドは `publish.py` の `validate_real_apk` が定義しています。
入力の package / version / bytes / SHA-256 / 署名、実際のMPPのSHA-256、選択項目と成功項目の一致、全工程成功、出力 APK の SHA-256 と処理監査を要求します。
過去の証拠や合成入力を実原本への適用証拠として代用しません。Android 実機の表示を確認したとは記載しません。

実装と実行証拠を確定した後、別々の新しい作業ディレクトリで2回ビルドします。

```bash
python3 releases/tiktok-link-direct100/src/build.py --base BASE --tools TOOLS --work WORK_A --dist DIST_A
python3 releases/tiktok-link-direct100/src/build.py --base BASE --tools TOOLS --work WORK_B --dist DIST_B
python3 releases/tiktok-link-direct100/publication/freeze.py --first DIST_A --second DIST_B --base BASE --repo . --reviewed-main MAIN_SHA --reviewed-dev DEV_SHA
python3 releases/tiktok-link-direct100/publication/verify_pair.py DIST_A DIST_B --base BASE
python3 releases/tiktok-link-direct100/publication/selftest.py --dist DIST_A
python3 releases/tiktok-link-direct100/publication/package.py --dist DIST_A --base BASE
python3 releases/tiktok-link-direct100/publication/publish.py --dist DIST_A --base BASE --repo . --local-only
```

`MAIN_SHA` / `DEV_SHA` は公開直前に確認した main / dev の完全なコミット SHA です。
凍結後にコード、説明、証拠、ワークフローを変更したら、必要な検証と凍結をやり直します。

## 自動公開

ソース、凍結した `publication/expected.json`、対応するワークフローを、確認した main の直下に1コミットで追加します。
main/dev の配布フィードはまだ変更しません。

ワークフローは新規ビルド2回をローカルの凍結済みバイトと照合します。
公開処理はGitHub Actions内の同じ実行ID・コミットで検証した組だけ受け入れます。
リリースの下書きに7資産をアップロードし、再取得して照合します。固定コミットURLのMPPも取得して照合します。
その後リリースを公開し、main/dev のフィードとCHANGELOGをlease付き原子的pushで更新します。
既存のバージョン・タグ・配布ブランチ、HEADの進行、資産不一致は停止理由です。失敗後は記録を確認してから再開します。

更新対象はTikTokです。元APKと生成APKは公開資産にもソースZIPにも入れません。
