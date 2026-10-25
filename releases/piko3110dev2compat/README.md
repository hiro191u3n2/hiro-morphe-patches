# Piko 3.11.0-dev.2.hiro.1 — Hide feed posts compatibility fix

Piko 3.11.0-dev.2 の「Hide feed posts」が Instagram 452.1.0.53.64 へ適用時に `Instruction not found in method implementation` で失敗する問題の互換修正版です。

元版は fingerprint の `InstructionMatch.instruction` を footer の外部分岐先へ渡していました。メソッドの mutable implementation が初めて生成されると、この命令は編集中の命令一覧に存在しません。分岐先を `footer.method.getInstruction(footer.instructionMatches[7].index)` で取得する処理へ変更します。

JVMクラスとAndroid用DEXの両方へ同じ修正を適用します。元MPPの変更は該当クラス、`classes.dex`、manifestの版番号の3エントリーだけです。PikoのNOTICEとGPL LICENSEも同梱します。他のパッチ機能や設定は変更しません。

## 使用方法

1. `Piko_3.11.0-dev.2.hiro.1.mpp` をMorphe Managerへ読み込みます。
2. Instagramへ適用するときは、元のPikoの代わりにこの互換版からパッチを選択します。両方のPikoを同時に適用しないでください。
3. Hiro Morphe Patchesの「戻る1回で終了・履歴削除」は併用できます。
4. 未改造のInstagram 452.1.0.53.64（arm64-v8a / versionCode 385811006）へ適用してください。

この互換版はhiroによる派生配布です。crimeraによる公式Piko更新ではありません。Hiro総合版の更新情報と公式Pikoの配布元は変更しません。

## 検証

- Morphe Desktop 1.19.1-dev.2 / Patcher 2.0.1-dev.2で、元APK＋元Pikoの「Hide feed posts」だけでも報告と同じエラーを再現しました。
- 修正版では同じ元APKへ適用し、APK生成に成功しました。
- 出力DEXの独立検査で、非表示時の分岐先が元のSystrace呼び出しであること、通常時のfooter呼び出しとsnapshot生成、モデル数の制御、追加読込の制御を確認しました。
- 同時適用の範囲と結果は `evidence/QA.json` を参照してください。ユーザーが選択した68項目の正確な内容・オプションは不明であり、同一選択の再現とは主張しません。
- Android実機でのインストール・画面動作は未確認です。

## 再現ビルド

JDK 21.0.8 と Python 3で次を実行します。

```sh
python3 build.py --java-home /absolute/path/to/jdk21
```

公式MPPと検証用CLIは `build.py` にURLとSHA-256を固定しています。Gradleの認証情報、Android SDK、未公開ファイルは不要です。`src/HideFeedPostsPatch.kt` と `source-fix.diff` は同等のKotlinソース修正です。`src/FixBundle.java` は配布済みバイナリの該当箇所のみを書き換える再現用ビルダーです。

上流の対応するソースは crimera/piko のcommit `6f0673fd7c19acb92f3d124b0b5e39c2edbfb4bd`（v3.11.0-dev.2）です。リリースには、そのソース、修正差分、ビルダー、検証資料を含めます。APK、端末情報、署名鍵、認証情報は配布しません。
