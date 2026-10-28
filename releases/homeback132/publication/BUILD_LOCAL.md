# OneBack 1.1.25 / bundle 1.0.312 のビルドと公開

基準は公開済み総合v1.0.311（19,409,377バイト、SHA-256 `7b8b16e9257b9be08e1144a0e72c4db611a3bf475fe23b6fc9e747c6f98c8f0d`）です。固定URLとSHA-256は`src/build.py`の`BASE_URL`/`BASE_SHA`です。OneBackのパッチ適用時ローダーを更新し、ランタイムの元バイトを保持します。旧リリースのディレクトリを変更しません。

## 入力とツール

Temurin JDK21.0.8+9を使用します。`publication/stage.py --root STAGED`は、固定SHA-256で検証するMorphe1.16.0 / 1.19.0-dev.19 / 1.19.0、Manager2.3.0-dev.7の公式APK、Android35 API、D8、基準MPPを取得します。ManagerAPKの`assets/jvm/patcher-host.jar.xz`を展開し、Patcher2.1.0-dev.4を含む同梱ホストの固定SHA-256とサイズも照合します。ツール名は`manager237.apk`と`manager237-host.jar`です。ローダーメタデータはこの同梱ホストを加えた4種類のSDKで検査します。原本APKや検証証拠は作成しません。基準MPPの保存名は`baseline311.mpp`です。ビルドのTimestampはUTC時計で確認した`2026-10-28T09:41:20`に固定しており、独立ビルド間で変更しません。

原本APKはcom.instagram.android、452.1.0.53.64、versionCode385811006、143237227バイト、SHA-256 `22f20062d16997e26009e73b0425974efef8ac69c76dd8912ee8a091213c39e2`です。

併用検証には、Piko 3.11.0-dev.3の[公式MPP](https://github.com/crimera/piko/releases/download/v3.11.0-dev.3/patches-3.11.0-dev.3.mpp)（8,529,170バイト、SHA-256 `a7658d042676d750ff68b782ff02e65d077c011d6a6f09031c05b39e80fcc7a3`）を`PIKO_MPP`として指定します。公開済みのManager APK・同梱ホスト・Piko・Hiro基準版は、検証ハーネスでも同じサイズとSHA-256を要求します。

## ビルドと原本適用

```bash
python3 releases/homeback132/src/build.py --base BASE --tools TOOLS --work PILOT_WORK --dist PILOT_DIST
```

既存のホスト回帰と`Back131InstagramProfileRouteTest`・`Back131InstagramProfileIdentityTest`を含め、必須テストが全て実行されます。基準版のOneBackローダー18クラス、ランタイム93クラスと固定ランタイムSHA-256を検査します。ローダーDEXではOneBackの名前空間だけを置き換えます。その他のDEX定義とZIP資産は保持します。共通ランタイムでは、93クラス全てを検査し、実装変更・新規クラス・既存クラス削除を拒否します。比較を通過した後は、基準版の拡張ファイルをバイト単位でそのまま使用します。ビルドと公開の両方で固定SHA-256と元バイトの一致を要求します。

4種類のSDKそれぞれで、生成済みの最終単体MPPに対して`Instagram132MutableContextAudit snapshot-members`も実行します。入力のスナップショット作成が編集用クラスのメソッド分類キャッシュを初期化せず、その後に追加したブリッジや補助メソッドが実際に出力DEXへ残ることを検査します。ビルドQAの`mutable_members`には4種類全ての成功記録が必要で、欠落・別SDK・別監査モード・失敗記録は公開ゲートで拒否します。

最終の単体・総合MPPをそれぞれ原本APKに適用します。`src/validate_apk.py`が実適用と監査を行い、`src/validation/real-apk.json`を作成します。スキーマは`homeback132-real-apk-v1`です。`publication/publish.py::validate_real_apk`が要求する、原本の識別情報、最終MPPのSHA-256、全工程成功、選択/適用パッチ、生成APKのSHA-256と各監査結果を実行結果から記録します。

- `Instagram130ProfileLinkAudit output ORIGINAL_APK PATCHED_APK`: 新しいネイティブ経路とその保存を検査します。
- `ActualInstagram452Audit`: 既存のネイティブBack処理と追加フックを除去して、原本との保存を検査します。
- `InstagramManifestAudit`: 元のAndroidManifestに対する保存を検査します。
- `InstagramResourcesAudit ORIGINAL_APK BASELINE_PATCHED_APK CURRENT_PATCHED_APK`: 原本・同じSDKで総合v1.0.311を適用した出力・新しい出力を比較します。

リソース表は57,543エントリのID・型・フラグ・設定・値と文字列/スタイルプールを検査し、基準版との意味上の完全一致を要求します。Morpheによる既存のエントリ名正規化は、文字列18,389件とdrawable114件だけを許容し、基準版と同じ名前であることも照合します。`resources.arsc`のバイト一致を主張せず、その他の変更対象外ZIP資産とネイティブライブラリはバイト単位で検査します。

監査の実際の引数は各ソースのusageを確認します。元入力だけの検査、以前のMPPの証拠、合成した成功値で代用しません。物理Android操作の成功を主張しません。原本/生成APKをソースツリーに置きません。

## 組み合わせの検証

`validate_apk.py`は`validate_manager.py`を呼び出し、実際のManager 2.3.0-dev.7同梱ホストの`HostMainKt`へリクエストを渡します。公式ホストを変更せず、同じAPKのフレームワークリソースを使用します。Managerの`PatchKeysKt.keyedFor`で取得した選択キーとオプション初期値を使い、次の4ケースをこの順序で実行します。

| ケース | 選択 | 必須結果 |
| --- | --- | --- |
| `old311-navigation` | PikoのCustomize navigation bar＋公開済みHiro 1.0.311のOneBack | navigationの適用後、OneBackが報告と同じ`003Q.A04`エラーで拒否される |
| `standalone-navigation` | PikoのCustomize navigation bar＋最終単体OneBack 1.1.25 | 選択した2パッチが全て完了する |
| `aggregate-default-baseline` | Pikoの対象版で有効な初期選択64個 | 選択した64パッチが全て完了する |
| `aggregate-default` | 同じPiko64個＋最終総合1.0.312のOneBack | 選択した65パッチが全て完了する |

Morpheホストはパッチ失敗時にもプロセス終了コード0を返すため、終了コードだけでは成功にしません。完了イベントの失敗情報、個別パッチの成功・失敗、`patches_done`の個数、生成APKを検査します。選択・適用名、Manager選択キー、オプション初期値と明示指定なしの事実、リクエスト・イベントのハッシュ、生成APKのサイズとハッシュを記録します。Pikoの利用可能な68個のメタデータと64個の初期選択は別々に検査します。利用者が選んだ68個の内容やオプションは復元しません。

`real-apk.json`の`manager_compatibility`は、この全4ケース、最終MPPの識別情報、公式Manager/Patcher/Pikoの出典と固定ハッシュ、現在の選択ヘルパーとハーネスのハッシュに結び付きます。`--only`による診断用の部分実行は`complete=false`となり、公開ゲートで拒否します。原本監査の`PASS Instagram132MutableContextAudit:`も必須です。

`manager_preservation`では、上記64個の出力APKを入力として、65個の出力APKとの差分を独立に監査します。両方のAPKハッシュを実行ケースと照合し、プロフィールのフックと全DEXの意味、既存のリソース・ネイティブライブラリを検査します。`InstagramManifestAudit --preserve-baseline PIKO64_APK PIKO65_APK`と`InstagramResourcesAudit --preserve-baseline PIKO64_APK PIKO65_APK`は、Pikoが変更したversionCode・リソース名・値もそのまま基準にします。原本向けの名前正規化例外を、この比較には許可しません。通常の単体・総合の原本適用と既存SDKの監査も全て維持します。

Piko併用出力の比較では、スタイルを持たないグローバル文字列プールに限り、全ての文字列・出現回数・エンコーディングフラグが一致する場合のインデックス入れ替えを認めます。その場合も、全リソースの名前・ID・設定・フラグ・参照解決後の値は完全一致を要求します。文字列の集合と各出現回数を維持したまま参照先の値を変える差分は、否定テストで拒否します。`resources.arsc`全体のバイト一致を主張するものではなく、原本APK向けの従来監査では文字列プールのバイト比較を維持します。

## 確定と独立ビルド

同じ例外処理を表す編集用クラスの正規化と、編集開始前の入力スナップショットの修正・回帰・文書を確定してから、2つの新しい作業ディレクトリでビルドします。OneBackランタイムは基準版の元バイトを保持します。ホスト/原本APK検証と実機操作を区別し、利用者の起動Intentや実機でのBack動作まで確認したとは扱いません。続いてその最終MPPの実適用記録を作成し、凍結します。`APK_WORK`も未作成の新しいディレクトリを指定します。

```bash
python3 releases/homeback132/src/build.py --base BASE --tools TOOLS --work WORK_A --dist DIST_A
python3 releases/homeback132/src/build.py --base BASE --tools TOOLS --work WORK_B --dist DIST_B
python3 releases/homeback132/src/validate_apk.py --apk ORIGINAL_APK --piko PIKO_MPP --dist DIST_A --base BASE --tools TOOLS --build-work WORK_A --work APK_WORK
python3 releases/homeback132/publication/freeze.py --first DIST_A --second DIST_B --base BASE --repo . --reviewed-main MAIN_SHA --reviewed-dev DEV_SHA
python3 releases/homeback132/publication/verify_pair.py DIST_A DIST_B --base BASE
python3 releases/homeback132/publication/selftest.py --dist DIST_A
python3 releases/homeback132/publication/package.py --dist DIST_A --base BASE
python3 releases/homeback132/publication/publish.py --dist DIST_A --base BASE --repo . --local-only
```

公開前にmain/devの完全なHEAD、同一の基準フィードを再確認します。`expected.json`は実際の2ビルドから生成します。ソース、証拠、文書、ワークフローのハッシュを含みます。変更した場合は必要な検証・凍結をやり直します。

## 自動公開

新しいリリースディレクトリと`.github/workflows/homeback132-publish.yml`を確認済みmainの直下へ1コミットで追加します。最初のコミットでフィードを変更しません。ワークフローの写しと実ファイルは一致が必要です。

CIが独立に2回ビルドし、ローカルで凍結したMPPとQAのバイトに一致した場合だけ公開します。同一run ID/source commitの証拠を確認後、リリース下書きの7資産、固定コミットのダウンロードを再取得して検証します。main/devフィードはlease付きの原子的pushで更新し、Instagramだけを更新対象にしたCHANGELOGを追加します。既存ULikeポリシーと他アプリの履歴は保持します。

途中失敗やHEAD/基準の進行を上書きせず、記録を確認します。タグ、配布ブランチ、同じバージョン資産が既に存在する場合は、無条件の上書きや再実行を行いません。成功後に公開記録を両ブランチへ保存します。
