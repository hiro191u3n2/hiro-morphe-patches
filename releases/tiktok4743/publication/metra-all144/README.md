# TikTok 47.4.3: Metra 136 + Hiro 8 の追加統合検証

修正後の Hiro MPP を使い、**明示した全 144 パッチの適用と APK 再構築が
`-Xmx3g` で完了しました**。SDK の適用結果は 144 件成功、失敗 0 件で、
`PATCHING` と `REBUILDING` はともに成功です。この補助検証の判定名は
`PASS_PIPELINE_ONLY` です。

これはユーザーが選択した 128 項目の再現ではありません。その項目名と
オプションは未取得です。Android 実機での実行・画面操作も行っていません。
全 144 項目を常用する設定として推奨するものではありません。

## 今回の確定した入力と出力

| 対象 | 識別情報 |
| --- | --- |
| 原本 | `com.zhiliaoapp.musically` / 47.4.3 / versionCode 2024704030 |
| 原本のサイズ | 474921201 bytes |
| 原本 SHA-256 | `af7cbe2b00d33ff1339730f6abb8ed7e387a83b27ff2b16748b8f41e6fc87e0b` |
| Metra | `patches-0.9.0-dev.1.mpp`、136 項目 |
| Metra SHA-256 | `45dc0c2244f8cf6973de18542d2d9a0cc25512f5b1de52438c685b82edb7c4cd` |
| Hiro | `TikTok_47.4.3_Hiro_v4.1.0.mpp`、8 項目 |
| Hiro サイズ | 768881 bytes |
| Hiro SHA-256 | `42da5b42478498bdd1b8905cbf256241220718ecae92ed7ed3543660ace2bea3` |
| SDK | Morphe Patcher 2.0.0、`morphe200.jar` |
| SDK SHA-256 | `deb5d931f3899b20a119637473d338c52585f0c561b5d8b2856b916fe90163b5` |
| 最大 Java ヒープ | `-Xmx3g` = 3072 MiB |
| 出力 APK サイズ | 457337031 bytes |
| 出力 APK SHA-256 | `914308cbc5c858d3ddf33043669385cc91d02489c038b366817bb27dcb27638d` |

出力 APK は署名していないローカル診断用ファイルです。この証拠一式に
APK、SDK、MPP、DEX またはコンパイル済みクラスは含めていません。
全試行でオプションはパッチの既定値を使い、互換性の強制指定はしていません。

## 判定方法

`current-3g/result.json` は SDK が出力した JSON の原文です。
`current-3g/audit-summary.json` はそれを明示した選択一覧と突き合わせた結果です。
`current-3g/apply.log`、コマンド、入力の由来、カタログ照合ログも原文で保存しています。

成功には、プロセスの終了値 0 と SDK のトップレベル `success: true` に加え、
次の条件をすべて要求しました。

- 正式な Morphe JVM ローダーで、Metra 136 件と Hiro 8 件の全名称および
  宣言された 47.4.3 対象互換性をそれぞれ照合する。
- 要求した 144 件がすべて `appliedPatches` にあり、失敗・欠落・余分な名称・
  重複・成功失敗の重なりがない。
- ログの `Applying 144 patches` と SDK の結果が一致する。
- `PATCHING` と `REBUILDING` を含め、報告された全ステップが成功する。
- 対象パッケージとバージョンが一致し、出力 APK が存在する。
- 実行後も原本 APK のサイズと SHA-256、Hiro MPP、Metra MPP、SDK の
  SHA-256 が実行前から変わっていない。

`--continue-on-error` は全選択の失敗を収集するために使っています。失敗を
成功として扱う設定ではありません。後述の旧 4g 試行では終了値 0 と
トップレベル `success: true` が返りましたが、この厳密判定では失敗です。

## 確認した衝突と修正後の結果

旧 MPP では、Metra の追加リソース削除機能が意図して空にした
`lib/arm64-v8a/libEffectCreatorJni.so` に対し、Hiro privacy の非ゼロサイズ
条件が失敗しました。修正後の検証では、事前に特定した 18 パスの空ファイルを
そのまま維持し、残るライブラリの検査と必要な書き換えを完了しています。

独立した `empty-native-audit.py` による実出力監査も成功しています。
`empty-native-audit.json` は次の事項を APK 内の実データで照合した結果です。

- 元の native ライブラリ 593 パスがすべて残り、余分なパスもない。
- 空になっているのは、上流の削除機能による既知の 18 パスだけ。
- `PRIVACY_NATIVE_PRESERVED_EMPTY` の 18 行と `preservedEmpty=18` が一致する。
- 非空の変更済みライブラリ 16 件すべてについて、ログ中の `afterBytes` と
  `afterSHA256` が実出力 ZIP と一致する。

この追加監査の対象は空 SO との共存およびログと実出力の整合です。
全 144 項目の機能や通信動作を実機で証明するものではありません。

## 旧試行は失敗・中断のまま保存

| 保存先 | Hiro MPP | ヒープ | 実結果 |
| --- | --- | --- | --- |
| `history/old-2g-oom/` | 修正前 | 2g | 136 件の Applied ログ後、OneBack の処理中に Java heap space。SDK 結果 JSON と出力 APK はなし |
| `history/old-4g-privacy-failure/` | 修正前 | 4g | 143 件適用、Hiro privacy 1 件失敗。`PATCHING=false`、`REBUILDING=true` |
| `history/old-3g-interrupted/` | 修正前 | 3g | 空 SO 衝突の原因確定後に中断。52 件の Applied ログ、終了値 130、結果 JSON と出力 APK はなし |
| `current-3g/` | 修正後 | 3g | 144 件適用、失敗 0 件、全ステップ成功 |

修正前 MPP の SHA-256 は
`a2e7152d3514bbbb67ec382bb66c3903726c6b37e092101db62b3beec21a3bb8`
です。元の証拠を上書きしていません。ヒープまたは MPP が異なる試行は
それぞれ別の条件として扱っています。

旧 2g 試行では SDK 結果 JSON が生成されなかったため、初期の
`audit-summary.json` にある `applied_count: 0` / `failed_count: 0` は
エンジンが報告した件数ではありません。同じ保存先の
`execution-failure.json` が、その欠測を `reported_*: null` として区別し、
実際の 136 件のログおよび例外スタックを記録しています。

3g は公式 Manager の 64-bit 内蔵 VM の上限 3072 MiB に合わせた条件です。
根拠は隣の `../manager-memory/audit-summary.json` に、固定コミットの
公式ソースとともに保存しています。同じヒープ上限を使うデスクトップ CLI
検証であり、Manager または実機の全動作を保証するものではありません。
旧 4g 試行を Manager での成功根拠に使っていません。

## 主検証との関係

主な公開判定に使う証拠は、別途実施した 47.4.3 と 47.5.3 の各 7 項目 / 9 項目
構成に対する実原本 APK 検証です。その native 出力、OneBack の実際の呼出し順、
日本語設定の接続、privacy コンテナや元の内容の保持を検査する監査を、
この全 144 項目の結果で置き換えていません。

全 144 の選択には versionCode の変更、ユーザー証明書の信頼、アプリ名の変更、
追加リソースの削除などの上流オプションも含まれます。それらの異なる出力を
受け入れるために主検証の監査を緩めていません。

記録上の `exploratory_only: true`、`valid_for_release: false`、
`physical_android_tested: false`、`user_128_selection_reproduced: false`
は意図した制限です。`valid_for_release: false` は、この補助結果単独では
公開判定にならないことを表します。

## 再実行

`run_all144.py`、`PatchSelectionCatalog.java`、3 つの選択 JSON は実行時の
ファイルをそのまま保存しています。ランナーのパス解決は検証時の workspace
配置を前提とするため、再実行時にはこれらを
`WORKSPACE/tiktok4743-work/metra-all144/` に配置します。元のファイル配置と
完全な argv は `current-3g/command.json`、その入力識別は
`current-3g/provenance.json` に記録しています。

```sh
python3 tiktok4743-work/metra-all144/run_all144.py \
  --execute --heap 3g \
  --artifact tiktok4743-work/dist-pilot-r2/TikTok_47.4.3_Hiro_v4.1.0.mpp \
  --work tiktok4743-work/metra-all144/fresh-reproduction
```

原本 APK、Metra MPP、SDK、JDK、Hiro MPP および隣接するビルド QA を、
記録された配置に用意してください。ランナーは原本・SDK・Metra の固定 SHA、
Hiro MPP とビルド QA の同一性を確認します。既存の作業ディレクトリは拒否します。
`--execute` を省くと Java を実行せず、コマンドを表示するだけです。

`evidence-manifest.json` は保存したテキストファイルのサイズと SHA-256 を
記録しています。実行に使った物と区別できるよう、過去のログ、結果、コマンドを
公開向けに書き換えていません。
