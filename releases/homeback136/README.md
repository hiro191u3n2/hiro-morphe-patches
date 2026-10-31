# Instagram Back diagnostics — OneBack 1.1.29 / bundle 1.0.320

これは、実際の端末で戻る操作がどの判定を通るか確認するための**診断版**です。
同じブラウザリンクを初めて開いた時と、再度開いた時の動作差が直ったと確認した版ではありません。
ブラウザへ戻ったという結果だけでは、OneBack が処理したのか、Instagram 自身の画面遷移なのかは区別できません。

戻る処理の既存フックに到達すると、`HB320` で始まる 2 行の短いコードを Android の Toast で表示します。
同じ処理中に複数のフックを通った場合は 1 回の表示にまとめます。表示には application context を使い、
Activity が終了しても表示できる構成です。Android の表示制限などで Toast が見えない可能性は残ります。
同じコードを端末内のログにも `HiroBack320` タグで出します。

追加する診断コードは固定した区分と数値だけで、URL、アカウント名、プロフィール名、投稿内容、入力内容を含みません。
コードを自動送信する機能はありません。既存判定の観測結果を記録し、戻る操作の判断は baseline319 のまま維持します。
独立した host・ソース検証で、既存判断の保持と追加診断のプライバシー条件を確認しました。実機での表示は未確認です。

## 確認手順

利用するには Morphe Manager で Hiro ソースを更新し、クリーンな Instagram 452.1.0.53.64 APK に
「戻る1回で終了・履歴削除」を選択して再パッチし、出力 APK をインストールしてください。
ソースの更新だけではインストール済みアプリは変わりません。

1. 普段と同じブラウザリンクからプロフィールを開き、戻る操作を 1 回行います。
2. 表示された `HB320` の 2 行と、移動先が「ホーム」「ブラウザ」「同じ画面」のどれかを記録します。
3. ホームに移動してもう一度戻る必要があった場合は、その追加操作のコードも別に記録します。
4. 同じリンクをもう一度開き、同じ戻る操作で表示された 2 行と移動先を記録します。

「最初の表示」「ホームからの追加の戻る（あった場合）」「2 回目の表示」を区別してください。
コードが出なければ「表示なし」と記録してください。コードを見落とす場合は画面録画から読み取れますが、
共有に必要なのはコードと移動先だけです。リンク先やプロフィール内容を送る必要はありません。
診断コードが出たことや `F3` が出たことだけで、問題の解消や OS による終了完了を断定しません。

## コードの読み方

`-` は、その項目が今回の処理では観測されなかったことを表します。ビット値は足し合わせです。
コードは既存の判定結果を示し、アカウントやリンク先を特定する値ではありません。

| 項目 | 意味 |
| --- | --- |
| `H` | Activity の区分。`M`=Main、`U`=URL handler、`L`=URL launcher、`O`=その他。 |
| `R` | 状態リセット後の最新 A09 登録試行の結果。`0`=試行結果の記録なし、`1`=登録成功、`2`〜`9`=下表の段階。現在の entry 自体の結果とは限らず、後の試行が拒否されても古い有効 entry は保持されるため `R4 E1` もあり得る。新しい Intent / Activity 破棄でリセット。 |
| `E` | 登録済み外部プロフィールの entry があるか。`0`=なし、`1`=あり。 |
| `S` | 安全条件の最初の拒否段階 / 最後の拒否段階。最後が `0` なら、その後の安全条件を通過。最初の拒否は残る。 |
| `P` | 親 Fragment の区分。`0`=親なし、`1`=`00VS`、`2`=`02Wh`、`3`=`03CG`、`9`=その他。 |
| `C` | 表示コンテナ。`M`=MAIN、`B`=BASE、`O`=その他。 |
| `Q` | 選択された対象が登録済みプロフィールと同一か。`0`=不一致、`1`=一致。 |
| `A` | 観測元のビット。`1`=native Back scope、`2`=AndroidX dispatch、`4`=profile handler。 |
| `D` | dispatcher の結果。`0`=未記録、`1`=profile handler、`2`=汎用処理 / gesture 保護で消費、`3`=native に渡す。 |
| `N` | profile の native Back が消費したか。`0`=未消費、`1`=消費。 |
| `F` | profile 後処理。`0`=native 側へ戻す、`1`=native が消費、`2`=同一操作内の戻り保護で消費、`3`=終了処理の呼び出し、`4`=native handler の例外。 |
| `O` | 開始時対象との不一致のビット。`1`=native origin、`2`=prediction origin、`4`=key origin。 |
| `T` | 同一操作内の戻り保護が観測されたか。`0`=なし、`1`=あり。 |
| `W` | Back 転送中の状態が観測されたか。`0`=なし、`1`=あり。 |
| `B` | snapshot のビット。`1`=blocked、`2`=unknown、`4`=other content、`8`=current が native 未認識、`16`=current の状態不明、`32`=current なし。 |

| `R` 登録段階 | 判定 |
| --- | --- |
| `2` | スレッド・対象 Activity・profile 型の前提。 |
| `3` | Activity の終了状態、route の存在。 |
| `4` | 内部リンクの除外。 |
| `5` | original URL / on-create / entry module の整合性。 |
| `6` | 許可するプロフィール URL 形式。 |
| `7` | route URI と元 URL の整合性。 |
| `8` | Reel・別タブ・entry override・creation sheet の追加指定。 |
| `9` | 登録処理で例外を観測。 |

| `S` 拒否段階 | 判定 |
| --- | --- |
| `1` | Activity / profile の所有関係、入力状態。 |
| `2` | Fragment が added / visible / resumed か。 |
| `3` | profile View が存在するか。 |
| `4` | View が同じ window にあり、表示範囲が十分か。 |
| `5` | profile 内の子画面 / tab が空いているか。 |
| `6` | profile の popup がないか。 |
| `7` | 452 の親なし MAIN route の manager / container / 選択対象 / Activity layer の整合性。 |
| `8` | window snapshot に blocked / unknown / other content があるか。 |
| `9` | 452 の親あり route の snapshot / current / 選択対象の整合性。 |
| `10` | 旧経路の Activity 型。 |
| `11` | 旧経路の root profile / manager / Activity layer の整合性。 |
| `99` | 安全条件の確認中に例外を観測。 |

## 検証範囲

変更する既存 runtime は `InstagramExternalProfile`、`InstagramDispatcherBack`、`OneBackExit` の 3 クラスです。
既存 95 クラスを保持し、`InstagramBackTrace` とその `State` / Runnable の 3 定義を追加する計 101 クラスが検証対象です。
loader は owned 21 定義の構成と無関係な 282 定義を維持します。従来の native Instagram hook 12 個を使い、
診断用の追加 hook や native body の変更は加えません。

新しい `Back136InstagramDiagnosticTest` は 20 ケース・182 アサーションが成功しました。独立したソース AST の逆変換で、
既存 28 ファイルから診断呼び出し 33 個・値を変えない観測 wrapper 15 個を除いた元の実行判断が一致し、4 個の変更注入も拒否しました。
追加コードに URL・ユーザー名・内容を含めず、新しい native / reflective / lazy / current-screen 読み出しを加えないことも確認済みです。

全 36 suite（既存 35 と上記の診断テスト）が成功し、194 ソースファイルを固定した独立ビルド 2 回で、
集約 MPP・単独 MPP・ビルド QA JSON のバイト一致を確認しました。
また Manager 2.3.0-dev.7 で元 APK への単独適用と、公式 Piko 3.11.0-dev.3 の default 64 選択との併用を、
両ソース順で適用しました。単独は 1/1、併用は両方とも 65/65 の適用が成功し、すべて `failure=null` でした。
各出力の initial-profile（59）/ native-Home（379）/ profile-link（129）と manifest / resources / whole-APK の
6 監査が成功し、runtime 101 定義の一致と native hook 12 個の保持を確認しました。
両ソース順を指定しても実際の Manager 実行順は同じでした。混合出力 APK のバイト列は同一ではありませんが、
それぞれを同じ Piko-only baseline と個別に照合し、DEX・manifest・resource の保持検証が成功しました。
任意の追加選択を含むすべての組み合わせを検証したものではありません。

**実機未確認。** 診断表示や初回・2 回目の動作は端末での確認が必要です。
検証証拠は `verification/` に格納し、最終成果物と一緒に公開ゲートで照合します。元 APK、native disassembly、利用者の録画はソース配布物に含めません。
