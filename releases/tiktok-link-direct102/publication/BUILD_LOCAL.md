# TikTok Link Direct v1.0.2 のビルドと公開

共有リンクパッチをv1.0.1からv1.0.2へ、総合をv1.0.309からv1.0.310へ更新します。
既存の共有リンクローダー5クラスだけを、厳密な名前・SHA-256照合後に置き換え、専用ランタイム拡張を追加します。非対象の283個のDEXローダー定義はデバッグ情報を含めて照合し、非対象のJVMクラス、ランタイム、リソースと旧ライセンス通知を同じバイトで保持します。

## ネイティブ処理の変更範囲

| 対象 | 変更 |
| --- | --- |
| `LX/0ICN;->invoke()` | v1.0.0の共有動画小窓専用の適格性判定を維持。元の実験値や動画処理は保持。 |
| `LX/1SxQ;->LJ(Activity, Uri)` | 短縮リンク解決前のMain画面先行起動を、元からある早期終了へ送る。実際のリンク解決と失敗時の処理は保持。 |
| `LX/1SyD;->LIZLLL(...)` / `LJII(...)` | 通知・type40・コメントの既存ガード後に、同じ共有リンク条件を追加。対象URIでは元のDetailActivity用Intentと`feed_detail`分類を選ぶ。 |
| `LX/1SyC;` / `LX/1SyE;` / `LX/1SyG;` の動画用Intent生成 | 共有リンク条件を通った一時Intentの権限を、元の動画IDが設定された最終Intentへ移す。 |
| `LX/1SxW;->LIZJ(...)` | 専用ランタイムが発行した共有リンクIntentの場合だけ、元の画面遷移アニメーション引数を0/0へ変更する。 |
| `DetailSafRootFragment`の戻る・ライフサイクル | 元のキー・コメント等の判定後、子画面の通常遷移直前に終了判定を追加。元の`onResume`完了後に表示状態を結び付け、破棄時に解除する。 |

共有リンク条件は、通常の単一動画URIと、URI自体または入れ子の`params_url`にある`utm_campaign=client_share`です。動画URIは`aweme/detail/動画ID`に加え、ホストが`detail` / `item` / `musical`、パスが空または`/`、トップレベルの`id`が先頭に0を付けない正の1～20桁の数字である既存形式も対象です。後者は元の動画用Intent生成処理がクエリの`id`を読む形式で、追加の変更対象メソッドはありません。

`group_invite_id`、`jump_to_search_detail`、`is_from_drama_tab_deeplink`、`forbid_insert_feed`、`tab_index`がURI自体または`params_url`で非空なら対象から外します。共有リンク判定の補助メソッドは`LX/1SyD;`内に保持します。元のURL、動画ID、認証、データ取得、再生処理も保持します。

v1.0.2の追加ランタイムは`extensions/hiro_tiktok_link_direct.mpe`、名前空間は`app.hiro.tiktoklink.runtime`に限定します。共有リンク動画の表示状態とネイティブの戻る判定を確認し、終了対象と判定できた場合にTikTok自身の画面・タスクとプロセスを終了します。既存OneBackを再実装せず、共有リンクの遷移と戻る終了の範囲を担当します。状態や戻る判定が確認できない場合は元の処理へ戻します。

基準v1.0.309の`extensions/oneback_exit.mpe`は166028バイト、SHA-256は`43a0d988f1f11e0ccfd91a5a0562f386ea4a932d8d1419d5cf401b46b8865965`です。総合v1.0.310でもこのバイト列を保持します。

URL解決と動画データの取得にかかる時間は通信状況などに依存します。コードと生成APKの検証は、Android実機の表示順や再生開始時間を測定するものではありません。即時再生や待ち時間ゼロは保証しません。

## 入力

- Temurin JDK21.0.8+9。実際のjava/javacの版を確認します。
- `src/build.py`の`TOOLS`に一致するMorphe1.16.0、Morphe1.19.0、Android35 API、D8。
- 同ファイルの`BASE_URL`/`BASE_SHA`に一致する総合v1.0.309。
- 署名済み原本global TikTok47.4.3。固定識別情報は`publication/publish.py`の`INPUT`です。

`publication/stage.py --root STAGED`は公式ツールと公開済み基準MPPを取得します。元APKと新しい検証証拠は取得・作成しません。

## ビルド

```bash
python3 releases/tiktok-link-direct102/src/build.py --base BASE --tools TOOLS --work PILOT_WORK --dist PILOT_DIST
```

`src/patch/*.java`と`src/runtime/`以下のJavaソースを別々にコンパイルします。実ランタイムは固定したAndroid35 API jarだけを外部クラスパスとして使用し、他のOneBackソースクラスに依存しません。
`RuntimeExtensionAudit`が出力DEXの名前空間、実際のJVMコンパイルとのクラス集合一致、既存の全ランタイム・ローダーとの衝突不存在を検査します。
ホストテストは実際にコンパイル済みのランタイムクラスを使用します。Androidスタブとテストは別ディレクトリへコンパイルし、製品クラスを置き換える同名クラスがないことを検査します。
旧ローダーのJVMピンは`OWNED_BASE_JVM`に固定し、DEX側も既知の5クラスだけを取り除いて新実装に差し替えます。
同じパッチ名・対象バージョン・既定オンを維持し、非対象パッチのメタデータを変更しないことを2版の実Morpheローダーで確認します。

## 元APKへの適用と出力検査

元APKに対して次の4回の実適用を行い、`src/validation/real-apk.json`へ実結果を保存します。

| ケース | 入力MPP | 選択する項目 |
| --- | --- | --- |
| `baseline_oneback` | 公開済み総合v1.0.309 | OneBackのみ。併用監査用の基準出力。 |
| `standalone_link` | 単体v1.0.2 | 共有リンク項目のみ。 |
| `aggregate_link` | 総合v1.0.310 | 共有リンク項目のみ。 |
| `aggregate_link_oneback` | 総合v1.0.310 | 共有リンク項目と既存OneBackの両方。 |

```bash
python3 releases/tiktok-link-direct102/src/validate_apk.py --apk ORIGINAL_APK --dist PILOT_DIST --base BASE --tools TOOLS --work REAL_APK_WORK
```

新しいschemaは`tiktok-link-direct-real-apk-v3`です。公開処理は前版のv1/v2証拠を拒否します。
入力の識別情報・署名、正確なパッチ集合の選択と適用、全工程成功、出力APKのハッシュ、実ランタイムのホストテストと、次の3つの出力監査が必要です。

- `direct_link_audit`: `PASS LinkBranchAudit output:`で始まる小窓省略の回帰監査。
- `fast_route_audit`: `PASS LinkFastRouteAudit output:`で始まる、実際の出力DEXに対する先行起動省略、両選択メソッドと共有条件、元のネイティブ処理の保持の監査。
- `back_exit_audit`: `PASS LinkBackAudit output:`で始まる、共有リンク動画からの戻る終了と元のネイティブ処理の保持の監査。

3つの新出力をすべて監査します。併用ケースだけは、各監査に基準OneBackのみを適用した実出力も渡します。元APKは引き続き署名と入力契約の基準です。既存OneBackの変更を新しい共有リンクパッチの変更と混同しないよう、併用出力と基準出力のランタイム・ネイティブ処理を比較します。

元入力だけの検査や過去のMPP検査を、新出力への監査として代用しません。MPPを変更した場合は、その新しいバイトで実APK検査を再実行します。Android実機の操作確認を主張しません。

## 独立ビルドと凍結

実装と実行証拠を確定した後、2つの新規作業場所でビルドします。

```bash
python3 releases/tiktok-link-direct102/src/build.py --base BASE --tools TOOLS --work WORK_A --dist DIST_A
python3 releases/tiktok-link-direct102/src/build.py --base BASE --tools TOOLS --work WORK_B --dist DIST_B
python3 releases/tiktok-link-direct102/publication/freeze.py --first DIST_A --second DIST_B --base BASE --repo . --reviewed-main MAIN_SHA --reviewed-dev DEV_SHA
python3 releases/tiktok-link-direct102/publication/verify_pair.py DIST_A DIST_B --base BASE
python3 releases/tiktok-link-direct102/publication/selftest.py --dist DIST_A
python3 releases/tiktok-link-direct102/publication/package.py --dist DIST_A --base BASE
python3 releases/tiktok-link-direct102/publication/publish.py --dist DIST_A --base BASE --repo . --local-only
```

凍結用`expected.json`は実際の2ビルドと原本適用証拠から生成します。前版の期待値や成功記録をコピーしません。
変更後は必要な検証と凍結をやり直します。対応ソース、証拠、説明文、ワークフローもハッシュに含めます。

## 公開

main/devの現在の完全なHEADと基準フィードを再確認します。ソース・期待値・新ワークフローを、確認したmainの直下に1コミットで追加します。フィードはまだ編集しません。
GitHub Actionsは独立した2ビルドを実行し、凍結したMPPとQAの正確なバイトを再現します。
同じ実行ID・ソースコミットでの検証完了後、リリース下書きの7資産を再取得して照合し、固定コミットURLのMPPも照合します。
main/devフィードはlease付き原子的pushで更新し、TikTokだけを更新対象とするCHANGELOGを追加します。
タグ・配布ブランチの既存利用、基準/HEADの進行、資産不一致は停止理由です。途中失敗を上書きせず記録を確認してください。

原本APKと生成APKは公開資産、ソースZIP、リポジトリへ含めません。
