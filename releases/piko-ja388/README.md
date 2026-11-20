# Piko NewX 日本語化 1.0.2（Hiro 1.0.388）

X 12.43.0-prod.01 の日本語化と表示設定が、選択された Piko のバージョン表記だけで停止する問題を修正した。両方とも、選択された NewX 項目から必要な4つの依存パッチをたどり、同一の Piko ソースが提供していることを確認する。各機能は従来どおり、対象クラスやメソッド、翻訳する元の文字列を適用時に照合する。

## ビルド

Java 21 を使用。コンパイラ・DEXツール・プロバイダのコンパイルに用いる Morphe JAR と元の表示設定 MPP は [1.0.383 の toolchain.json](../piko-ja383/toolchain.json) を参照。総合版の入力は配布済み Hiro 1.0.387。ハッシュと出力の内訳は [QA_build388.json](QA_build388.json) に記録した。

リポジトリのルートから実行する。下記のパスは使用する入力に合わせて指定する。`tools` には ecj.jar、d8.jar、classpath.jar を配置する。newx.mpe と shared.mpe は公式 Piko dev.4 の extensions ディレクトリから取り出す。

```bash
release=releases/piko-ja388
inputs=(--piko inputs/dev1.mpp --piko inputs/dev2.mpp --piko inputs/dev3.mpp --piko inputs/dev4.mpp)
python3 "$release/patch/build.py" --tools tools --patcher tools/morphe.jar \
  --extension inputs/newx.mpe --extension inputs/shared.mpe "${inputs[@]}"
python3 "$release/ui/build.py" --tools tools --patcher tools/morphe.jar \
  --base inputs/NewX_UI_Options_v1.1.2.mpp "${inputs[@]}"
python3 "$release/build/integrate.py" --base inputs/Hiro_Morphe_Patches_v1.0.387.mpp \
  --component "$release/patch/component.mpp" --ui-component "$release/ui/component.mpp" \
  --tools tools --patcher tools/morphe.jar --out dist
```

## 検証

[QA.json](QA.json) に元のエラーの再現、Morphe Desktop 2.3.0-dev.3（Patcher 2.5.0-dev.4）による実APKMへの適用、完成APKのリソースとDEXの検査結果を記録した。将来番号の試験には dev.4 のマニフェストの Version だけを書き換えた検証用コピーを使用した。実機での表示は未確認。
