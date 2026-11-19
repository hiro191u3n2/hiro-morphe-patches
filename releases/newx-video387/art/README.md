# Android 17 リンク処理の試験

Extract.java は完成APKの2経路から、postId をキーとする Map.get から投稿詳細を返す命令までを読み出します。エラー分岐の飛び先を受け皿の null 戻り値に移し、コンストラクターは完成APKの FromId の実装に差し替えます。入力マップ、Kotlinの数値変換・リスト生成、列挙値は試験用です。

実行対象はリンク処理の抜粋です。Xの画面、ネットワーク、ログイン、動画デコーダーは起動しません。

```bash
mkdir -p work/probe/classes work/probe/dex work/probe/extract
javac -source 8 -target 8 -d work/probe/classes $(find src -name '*.java')
jar cf work/probe/stub.jar -C work/probe/classes .
java -cp "$R8_JAR" com.android.tools.r8.D8 --min-api 26 --output work/probe/dex work/probe/stub.jar
javac -cp "$MORPHE_JAR" -d work/probe/extract ../VerifyRoutes.java Extract.java
java -cp "work/probe/extract:$MORPHE_JAR" Extract "$PATCHED_APK" work/probe/dex/classes.dex work/probe/route.dex
adb push work/probe/route.dex /data/local/tmp/shared-video.dex
adb shell chmod 444 /data/local/tmp/shared-video.dex
adb shell dalvikvm -Xverify:all -cp /data/local/tmp/shared-video.dex hiro.Probe
```
