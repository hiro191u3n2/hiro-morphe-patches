# Hiro 1.0.390 / Piko NewX 日本語化 1.0.3

Piko 3.12.0-dev.5 でメディア選択画面にグリッド表示が追加され、2個の R8 コールバックの名前と引数が変化した。今回の修正では同じ画面クラスの静的コールバックから、選択件数とダウンロード操作に使う8個のラベル、リンクコピー結果のラベルをそれぞれ照合する。翻訳先は一意であることを要求し、検証した命令の位置とレジスタに翻訳を適用する。グリッド設定の日本語リソースは対応するピッカー実装がある場合に追加する。

## ビルド

入力の X APKM は再配布しない。元の翻訳データとライセンスは piko-ja378、ホスト検証は piko-ja388 を参照する。Java 21、piko-ja383/toolchain.json の ECJ・D8・コンパイル用 morphe.jar を使用する。以下で tools/morphe.jar は DEX 統合用の Morphe Desktop 2.3.0-dev.3、tools/morphe24.jar はコンパイル用の 2.2.0-dev.18。

```bash
r=releases/piko-ja390
unzip -p inputs/dev5.mpp extensions/newx.mpe > inputs/newx.mpe
unzip -p inputs/dev5.mpp extensions/shared.mpe > inputs/shared.mpe
python3 "$r/patch/build.py" --tools tools --patcher tools/morphe24.jar \
  --extension inputs/newx.mpe --extension inputs/shared.mpe --piko inputs/dev5.mpp --piko inputs/dev4.mpp
python3 "$r/build/integrate.py" --base inputs/hiro389.mpp --component "$r/patch/component.mpp" --tools tools --out dist
```

公開用の差分から、検証した統合版のバイト列を復元できる。

```bash
zstd -d --long=26 --patch-from=inputs/hiro389.mpp releases/piko-ja390/hiro390.zst -o Hiro_Morphe_Patches_v1.0.390.mpp
echo 'd143418e55b9d6f9ada4eb927306a37ab8f7e116348c801a29623225ad2da0a5  Hiro_Morphe_Patches_v1.0.390.mpp' | sha256sum -c -
```
