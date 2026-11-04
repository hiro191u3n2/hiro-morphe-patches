# Amazon 商品検索の評価件数順 v1.0.1

対象は添付された Amazon ショッピング 32.19.0.100（versionCode 1243250306、Amazon v1.23.1 適用済み）です。
単独版は `Amazon_Review_Count_v1.0.1.mpp`、統合版は `Hiro_Morphe_Patches_v1.0.349.mpp` です。

v1.0.0 で並べ替えと状態表示を確認できなかった報告に対応し、検索を含む Amazon ページで起動する処理、画面内の URL 更新、キャッシュからの再表示、商品カード・評価件数の検出を改善します。端末上の原因を直接確認したものではありません。

従来の読み込み・ページ完了の4箇所に加え、`MShopWebViewClient.doUpdateVisitedHistory` と `InteractionWebFragment.onFragmentPostShown` を検証付きでフックします。元の広告非表示や価格履歴を含む既存処理を保持し、6メソッドへ合計8命令を追加します。

検索画面では v1.0.1 の状態表示を出し、商品待機中、件数を判定できない場合、並べ替えできた場合を区別します。読み込まれた通常の商品を表示上の評価件数で並べ替えます。評価件数には星だけの評価が含まれ、文章付きレビューの件数とは一致しません。Amazon 側へ件数順も指定しますが、全検索結果を取得した厳密な順位は保証しません。実機での表示・操作は未確認です。

## 適用

元の添付 APK を選び、「商品検索を評価件数の多い順に表示」だけを適用してください。v1.0.0 を適用した APK への重ね掛けは受け付けません。単独版と統合版のどちらか一方を使用し、以前と同じ署名で作成したアプリを上書きインストールします。ソース更新だけではインストール済みアプリは変わりません。

## 再現ビルド

JDK 21.0.13、`publication/baseline.json` の固定された統合348、`src/build.py` の SHA-256 と一致する4ツールを使用します。`morphe.jar` は旧 Java パッチ API のコンパイル専用で、適用・検証には公式 Patcher 2.2.0 を含む `morphe-2.2.jar` を使います。

```sh
npm ci --ignore-scripts --no-audit --no-fund --prefix src/runtime-tests
python3 src/build.py --base baseline348.mpp --tools tools --work build-a --dist dist-a --apk supplied.apk
python3 src/build.py --base baseline348.mpp --tools tools --work build-b --dist dist-b
```

各ビルドは新しい作業ディレクトリと空の出力先を指定します。JDK を別途指定する場合は `--java-home /path/to/jdk-21.0.13` を追加します。配布 MPP の全バイトが2回のビルドで一致することを確認します。

`--apk` を指定したビルドは、単独版と統合版を Patcher 2.2.0 の `STRIP_FAST` で実際に適用します。生成した DEX のクラス一覧と変更箇所、既存メソッド、ランタイム、APK の全非DEX資源を照合します。Java の起動条件、URL、ガード、二重適用拒否と、DOM の旧版失敗再現・並べ替え・状態表示をホストテストで確認します。

統合348から置換するのは `app/hiro/amazonreviews/patches/` と `extensions/amazon_review_sort.mpe` だけです。他パッチのクラスと資源は保持します。APK 適用結果は `QA_Amazon_Review_Count_v1.0.1.json` に記録します。元 APK、実機の検索内容、アカウント情報を公開物に含めません。
