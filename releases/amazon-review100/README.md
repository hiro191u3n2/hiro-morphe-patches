# Amazon 商品検索を評価件数順に表示 v1.0.0

Amazon ショッピング 32.19.0.100（1243250306、添付された Amazon v1.23.1 適用済みAPK）へ追加する Morphe MPP。パッチ名は「商品検索を評価件数の多い順に表示」。統合版は Hiro Morphe Patches 1.0.348。

添付APKには今回のパッチだけを選択する。単独版の利用を推奨。統合版では既存のAmazon用スポンサー非表示パッチなどを同時に選択しない。既存の改造は添付APK内のものを保持する。

検索画面で認識できたメインの商品一覧を、表示される評価件数の降順に並べる。追加読み込みを監視し、同数は現在の順を維持、件数不明は末尾とする。表示件数と読み込み済み範囲を画面に示す。件数は星のみの評価を含むことがあり、文章付きコメントだけの件数ではない。まだ読み込まれていない全検索結果の順位は保証しない。

検索用の2つのWebView loadUrlへURL正規化を追加し、onPageCommitVisible／onPageFinishedから画面処理を導入する。検索URLへの review-count-rank 指定は補助であり、サーバーが件数順を実施することは検証していない。ページ内容の取得・送信、アカウント情報の取得、バックグラウンド通信は追加しない。

既存の広告非表示・価格履歴のフックを維持する。対象4メソッドの完全なコード指紋とパッケージ／版を検査し、不一致や重ね掛けを中止する。元APK、個人情報、署名鍵は公開物に含めない。

## 検証

`src/build.py` は固定したJava21、Android SDK、旧Morphe SDK2種類でビルドし、URLと画面処理のホスト試験、単独版／統合版の検出、既存統合内容の保持を検証する。今回の公開用APK適用試験は、公式Desktop2.0.0内のPatcher2.2.0を使い、単独版と統合版の2通りを既定のSTRIP_FASTで実施した。追加命令以外の全74,125クラスと、マニフェスト・資源・ネイティブライブラリを保持し、両出力内容が一致することを確認した。Android実機および実際の検索画面の動作は未確認。

ビルドと互換性検査に使う旧ツールの内部Patcherは1.14.1-dev.3と2.0.1。旧デスクトップのFULL出力では14個のDEXを13個へ再分割した際に古いclasses14.dexが残る問題を検出した。旧版STRIP_SAFEでの単独版保持監査も通過したが、旧版4通りすべての適用完了とは表記しない。公開の適用証拠は `verification/QA_Amazon_Patcher22.json` に固定し、正確に一致するMPPの最新Patcher2.2.0の2出力を採用する。

`publication/manifest.json` にローカル検証済み成果物のハッシュを固定。公開CIで2回の再現ビルドが一致することを確認してから、GitHub ReleasesとMorphe Managerのmain/dev配布情報を更新する。固定配布ページは先に公開する。

再ビルド例：

```sh
python3 releases/amazon-review100/src/build.py --base BASELINE.mpp --tools TOOL_DIR --work NEW_WORK_DIR --dist NEW_DIST_DIR --java-home JDK21_DIR --apk SUPPLIED.apk
```

`src/runtime-tests` の固定依存関係を `npm ci --ignore-scripts` で導入する。詳細な対応範囲・導入手順は `publication/RELEASE_NOTES.txt`、検証結果は `verification/QA_Amazon_Review_Count_v1.0.0.json` を参照。
