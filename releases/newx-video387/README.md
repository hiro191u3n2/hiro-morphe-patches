# X 共有動画リンク修正

対象は X 12.43.0-prod.01（312430001、ARM64）の元APKMです。Hiro総合版 v1.0.387 または単独版 v1.0.0 の「NewX: 動画リンクで元の投稿を開く」を適用します。

LINEの共有カードとXで最初に表示される動画が異なる事例を確認しました。X本体には、status と statuses の /video/1 を動画フィードに割り当てる経路が2つあります。元の処理も投稿IDを読みますが、その後にフィードを開きます。パッチは双方でIDを読んだ直後にX自身の投稿詳細 FromId を返します。元の投稿内で動画をタップすると再生できます。フィードの配信内容やサーバーが別の動画を選んだ条件は未確認です。

## 再ビルドと確認

次のコマンドはSiteソースのルートで実行します。作業ディレクトリに tools/morphe.jar、tools/d8.jar、tools/patches-3.12.0-dev.3.mpp と X.apkm を用意します。ビルドツールの版とハッシュは build.py で検査します。Morphe Desktop は 2.3.0-dev.2、同梱の Patcher は 2.5.0-dev.1、R8 は 9.5.28 です。

```bash
python3 scripts/stage-berry386.py materialize
python3 scripts/newx-video387/build.py /path/to/work/tools /path/to/work/dist
for config in single combined clone; do
    python3 scripts/newx-video387/test_apkm.py /path/to/work "$config"
done
javac -cp /path/to/work/tools/morphe.jar -d /path/to/work/classes scripts/newx-video387/VerifyRoutes.java
for config in single combined clone; do
    java -cp /path/to/work/classes:/path/to/work/tools/morphe.jar VerifyRoutes "/path/to/work/$config.apk"
done
```

3種類の完成APKで、2経路のDEX命令と引数を検証しました。通常・クローンの併用試験はPiko 3.12.0-dev.3および既存Hiro設定を含みます。検証件数とハッシュは公開QAに記録しています。この検証の範囲はパッチ適用・APK再構築・投稿詳細へのIDの受け渡しまでです。

Android 17（API 37）のエミュレーターでは、完成APKから取り出した2経路と投稿詳細のコンストラクターを実行しました。有効な投稿ID、数値の上限超過、文字列、未指定の14ケースに成功しました。試験方法は art/README.md に記載しています。Xの画面操作・ログイン・動画再生は未確認です。
