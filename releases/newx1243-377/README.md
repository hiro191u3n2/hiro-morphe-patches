# NewX 12.43.0 Hiro patch port

X `com.twitter.android` `12.43.0-prod.01` (`312430001`, arm64) 用の3項目です。

| パッチ | 変更範囲 |
|---|---|
| NewX: タイムラインのカテゴリ設定を再起動後も保持 | X自身が保存するカテゴリ除外設定の起動時消去と期限切れ消去を無効化。手動リセット・適用・地域設定を維持。Pikoのカテゴリ選択はPiko自身の永続保存を使用。 |
| NewX: ポストの日時を絶対時間・24時間表記にする | 投稿の日時を端末のタイムゾーンで `yyyy/MM/dd HH:mm` に統一。投稿専用の描画・Compose再描画経路を複製し、DMなどの共通処理を維持。 |
| NewX: 戻る1回で終了・履歴削除 | ホーム・検索・通知・DMの最上位から戻るとホームを選択して `finishAndRemoveTask()`。詳細画面・ダイアログ・キーボード・対象外タブはアプリ本来の処理を維持。 |

## 導入

- 元の未改造APKM一式へ適用する。
- 総合版 `Hiro_Morphe_Patches_v1.0.377.mpp` と単独版 `NewX_Hiro_Addons_v1.0.0.mpp` はどちらか一方を使用する。
- **旧Hiroの3項目は選択解除し、`NewX:` の付いた3項目を選択する。** 総合版では旧アプリ用の項目も保持している。特に旧「戻る1回で終了・履歴削除」は古い幅広い互換情報が残るため、自動選択に任せず解除する。
- Pikoを併用するときも `NewX:` の項目を使用する。確認済みはPiko `3.12.0-dev.1`。
- Morphe Patcher `2.4.0` 以上を要求する。適用後に生成されたAPKをインストールする。

この変更の成果物は手動取り込み用。既存の自動更新フィード・本番サイトには変更を加えていない。

## 検証

提供された実APKMへ、安定版Morphe Patcher `2.4.0`（Desktop `2.2.0`）で以下を適用し、APKの再構築に成功。

1. Hiro単独の3項目。
2. Piko NewXの既定46項目とHiro3項目（計49）。
3. 上記にPiko NewX Cloneを追加（計50）。

完成APKを再読込し、追加した描画メソッド・再描画クラス・呼び出し先が存在すること、追加ランタイムが参照するネイティブメソッドとフィールドが解決できること、静的/インスタンス呼び出しとアクセス権が合うことを検証。元の共通日時メソッドと再描画クラスは一致。

カテゴリ処理のホスト監査308項目、戻る処理41項目、日時のタイムゾーン/DST/異常値/並列処理の試験を実施。MPP統合では従来の433プロバイダクラスと既存の全実行拡張・その他ZIPエントリを保持。

Android実機・エミュレーターでの起動、画面表示、戻る操作は未検証。

## 入力とビルド

元APKMは同梱しない。展開した `base.apk` のSHA-256:

`fe6e5214cb0220998e2106c5cd565a15fd1f7828fa8a4da13f99106502fd49b9`

統合元Hiro `1.0.376` のSHA-256:

`864112facfa290aa6e48a3e9a7f117af175e2c25861764a66e716af224674916`

Java 21、ECJ、D8、Morphe Desktopのコンパイル用JARが必要。ツールの取得元とSHAは `toolchain.json` を参照。`DEX_DIR` は元base.apkから展開したclasses*.dexのディレクトリ。

```bash
python3 category/build.py --tools "$TOOLS" --patcher "$PATCHER" --dex-dir "$DEX_DIR"
python3 time/build.py --tool-dir "$TOOLS" --patcher "$PATCHER" --dex-dir "$DEX_DIR"
python3 back/build.py --tools "$TOOLS" --dex-dir "$DEX_DIR"
python3 build/integrate.py --base "$BASE376" --components . --tools "$TOOLS" --cli "$PATCHER" --out "$OUTPUT"
```

コンパイル用スタブは配布するランタイムに含めない。`build/verify_packages.py` は記載の作業フォルダ構成で実APKM適用を再実行する。`build/ReviewNewXApk.java` は完成APKの参照と日時描画経路を監査する。
