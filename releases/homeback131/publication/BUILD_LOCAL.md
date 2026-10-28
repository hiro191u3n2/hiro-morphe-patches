# OneBack 1.1.24 / bundle 1.0.311 のビルドと公開

基準は公開済み総合v1.0.310（19,391,992バイト、SHA-256 `89313cab0ea94173b350900b59bb3eb0b36ef16b9d327a9d20dbbf227327b796`）です。固定URLとSHA-256は`src/build.py`の`BASE_URL`/`BASE_SHA`です。OneBackのローダーとランタイムだけを更新します。旧リリースのディレクトリを変更しません。

## 入力とツール

Temurin JDK21.0.8+9を使用します。`publication/stage.py --root STAGED`は、固定SHA-256で検証するMorphe1.16.0 / 1.19.0-dev.19 / 1.19.0、Android35 API、D8、基準MPPを取得します。原本APKや検証証拠は作成しません。基準MPPの保存名は`baseline310.mpp`です。ビルドのTimestampはUTC時計で確認した`2026-10-28T08:32:21`に固定しており、独立ビルド間で変更しません。

原本APKはcom.instagram.android、452.1.0.53.64、versionCode385811006、143237227バイト、SHA-256 `22f20062d16997e26009e73b0425974efef8ac69c76dd8912ee8a091213c39e2`です。

## ビルドと原本適用

```bash
python3 releases/homeback131/src/build.py --base BASE --tools TOOLS --work PILOT_WORK --dist PILOT_DIST
```

既存のホスト回帰と`Back131InstagramProfileRouteTest`・`Back131InstagramProfileIdentityTest`を含め、必須テストが全て実行されます。基準版のOneBackローダー18クラス、ランタイム93クラスと固定ランタイムSHA-256を検査します。ローダーDEXではOneBackの名前空間だけを置き換えます。その他のDEX定義とZIP資産は保持します。共通ランタイムでは、デバッグ行情報以外が一致するクラスを元の完全な定義へ戻し、非対象の実装変更を拒否します。`Bundle131Audit`が実装変更を認めるクラスは`InstagramExternalProfile`だけです。新規クラスの追加と既存クラスの削除は拒否します。

最終の単体・総合MPPをそれぞれ原本APKに適用します。`src/validate_apk.py`が実適用と監査を行い、`src/validation/real-apk.json`を作成します。スキーマは`homeback131-real-apk-v1`です。`publication/publish.py::validate_real_apk`が要求する、原本の識別情報、最終MPPのSHA-256、全工程成功、選択/適用パッチ、生成APKのSHA-256と各監査結果を実行結果から記録します。

- `Instagram130ProfileLinkAudit output ORIGINAL_APK PATCHED_APK`: 新しいネイティブ経路とその保存を検査します。
- `ActualInstagram452Audit`: 既存のネイティブBack処理と追加フックを除去して、原本との保存を検査します。
- `InstagramManifestAudit`: 元のAndroidManifestに対する保存を検査します。
- `InstagramResourcesAudit ORIGINAL_APK BASELINE_PATCHED_APK CURRENT_PATCHED_APK`: 原本・同じSDKで総合v1.0.310を適用した出力・新しい出力を比較します。

リソース表は57,543エントリのID・型・フラグ・設定・値と文字列/スタイルプールを検査し、基準版との意味上の完全一致を要求します。Morpheによる既存のエントリ名正規化は、文字列18,389件とdrawable114件だけを許容し、基準版と同じ名前であることも照合します。`resources.arsc`のバイト一致を主張せず、その他の変更対象外ZIP資産とネイティブライブラリはバイト単位で検査します。

監査の実際の引数は各ソースのusageを確認します。元入力だけの検査、以前のMPPの証拠、合成した成功値で代用しません。物理Android操作の成功を主張しません。原本/生成APKをソースツリーに置きません。

## 確定と独立ビルド

確認した2つのコード上の不具合（外部プロフィールURLの引き継ぎ認識と別プロフィールのBack結果）の修正・回帰・文書を確定してから、2つの新しい作業ディレクトリでビルドします。利用者の実際の起動Intentを確認したとは扱わず、ホスト/原本APK検証と実機操作を区別します。続いてその最終MPPの実適用記録を作成し、凍結します。`APK_WORK`も未作成の新しいディレクトリを指定します。

```bash
python3 releases/homeback131/src/build.py --base BASE --tools TOOLS --work WORK_A --dist DIST_A
python3 releases/homeback131/src/build.py --base BASE --tools TOOLS --work WORK_B --dist DIST_B
python3 releases/homeback131/src/validate_apk.py --apk ORIGINAL_APK --dist DIST_A --base BASE --tools TOOLS --build-work WORK_A --work APK_WORK
python3 releases/homeback131/publication/freeze.py --first DIST_A --second DIST_B --base BASE --repo . --reviewed-main MAIN_SHA --reviewed-dev DEV_SHA
python3 releases/homeback131/publication/verify_pair.py DIST_A DIST_B --base BASE
python3 releases/homeback131/publication/selftest.py --dist DIST_A
python3 releases/homeback131/publication/package.py --dist DIST_A --base BASE
python3 releases/homeback131/publication/publish.py --dist DIST_A --base BASE --repo . --local-only
```

公開前にmain/devの完全なHEAD、同一の基準フィードを再確認します。`expected.json`は実際の2ビルドから生成します。ソース、証拠、文書、ワークフローのハッシュを含みます。変更した場合は必要な検証・凍結をやり直します。

## 自動公開

新しいリリースディレクトリと`.github/workflows/homeback131-publish.yml`を確認済みmainの直下へ1コミットで追加します。最初のコミットでフィードを変更しません。ワークフローの写しと実ファイルは一致が必要です。

CIが独立に2回ビルドし、ローカルで凍結したMPPとQAのバイトに一致した場合だけ公開します。同一run ID/source commitの証拠を確認後、リリース下書きの7資産、固定コミットのダウンロードを再取得して検証します。main/devフィードはlease付きの原子的pushで更新し、Instagramだけを更新対象にしたCHANGELOGを追加します。既存ULikeポリシーと他アプリの履歴は保持します。

途中失敗やHEAD/基準の進行を上書きせず、記録を確認します。タグ、配布ブランチ、同じバージョン資産が既に存在する場合は、無条件の上書きや再実行を行いません。成功後に公開記録を両ブランチへ保存します。
